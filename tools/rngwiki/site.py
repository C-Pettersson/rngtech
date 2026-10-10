"""Wiki templates: expands {{ name(...) }} calls in wiki pages into HTML."""

from __future__ import annotations

import ast
import html
import io
import logging
import posixpath
import re
from dataclasses import dataclass, field
from pathlib import Path
from urllib.parse import quote

from mkdocs.structure.files import Files
from mkdocs.structure.pages import Page
from mkdocs.utils import get_relative_url
from mkdocs.utils.meta import get_data

from . import icons
from .resources import MOD_ID, Resources, split_id
from .vanilla import find_client_jar

log = logging.getLogger("mkdocs.hooks.rngwiki")

ICON_DIR = "assets/icons"
CATEGORIES = [
    "Guides",
    "Processing",
    "Power generation",
    "Energy storage",
    "Fluids and gases",
    "Refinement",
    "Logistics",
    "Tools",
]

_CRAFTING_TYPES = {
    "minecraft:crafting_shaped",
    "rngtech:trait_shaped",
    "rngtech:calibrated_shaped",
    "minecraft:crafting_shapeless",
    "rngtech:tool_damage_shapeless",
}
_IGNORED_KEYS = {"type", "group", "category", "neoforge:conditions", "input_count", "catalyst_count", "show_notification"}
_OUTPUT_KEYS = {"result", "results", "output", "outputs", "residue", "failure_output", "saturated_output"}
_LEADING_COLUMNS = ["processing_ticks", "energy"]
_COLUMN_LABELS = {
    "processing_ticks": "Time",
    "energy": "Energy",
    "machine_xp": "Mastery XP",
    "experience": "Player XP",
    "bonus_output": "Bonus output",
    "roll_result_traits": "Rolls traits",
}
_TEMPLATE_START = re.compile(r"\{\{\s*([a-z_]+)\s*\(")
_CODE = re.compile(r"(^```.*?^```|`[^`\n]+`)", re.S | re.M)


def _esc(value) -> str:
    return html.escape(str(value), quote=True)


def _format_number(value) -> str:
    if isinstance(value, bool):
        return "Yes" if value else "No"
    if isinstance(value, int):
        return f"{value:,}"
    if isinstance(value, float):
        return f"{value:,.2f}".rstrip("0").rstrip(".")
    return str(value)


def _compact_amount(amount: int) -> str:
    if amount >= 1000:
        return f"{amount / 1000:g}k"
    return str(amount)


def _label(key: str) -> str:
    if key in _COLUMN_LABELS:
        return _COLUMN_LABELS[key]
    text = key.replace("minimum_", "min. ").replace("maximum_", "max. ").replace("required_", "")
    text = text.replace("_", " ")
    return text[:1].upper() + text[1:]


def _format_cell(key: str, value) -> str:
    if key == "processing_ticks" and isinstance(value, (int, float)):
        return f"{value / 20:g} s"
    if key == "energy" and isinstance(value, (int, float)):
        return f"{_format_number(value)} FE"
    if isinstance(value, dict) and set(value) == {"min", "max"}:
        return f"{_format_number(value['min'])}–{_format_number(value['max'])}"
    if isinstance(value, str):
        text = value.replace("_", " ")
        return text[:1].upper() + text[1:]
    return _format_number(value)


@dataclass
class Stack:
    """One displayable slot: alternatives cycle when an ingredient is a tag."""

    options: list[str]
    count: int = 1
    fluid: bool = False
    label: str | None = None
    note: str | None = None
    tag: str | None = None


@dataclass
class WikiPage:
    file: object
    title: str
    category: str
    icon: str | None
    ids: list[str] = field(default_factory=list)


class WikiSite:
    def __init__(self, config):
        repo = Path(config.config_file_path).resolve().parent
        jar = find_client_jar()
        if jar is None:
            log.info("rngwiki: no Minecraft client jar found; vanilla icons use placeholders. "
                     "Run `python tools/rngwiki/vanilla.py --fetch` to download it.")
        self.res = Resources([repo / "src/main/resources", repo / "src/generated/resources"], jar)
        self.pages: dict[str, WikiPage] = {}
        self.id_pages: dict[str, WikiPage] = {}
        self.files: Files | None = None
        self.page: Page | None = None
        self.used_icons: set[str] = set()
        self.table_counter = 0

    # Page registry

    def scan(self, files: Files) -> None:
        self.files = files
        for file in files.documentation_pages():
            source = Path(file.abs_src_path).read_text(encoding="utf-8")
            body, meta = get_data(source)
            wiki = meta.get("wiki") or {}
            heading = re.search(r"^#\s+(.+)$", body, re.M)
            title = meta.get("title") or (heading.group(1).strip() if heading else file.name)
            page = WikiPage(file, title, wiki.get("category", ""), wiki.get("icon"), list(wiki.get("ids", [])))
            self.pages[file.src_uri] = page
            for resource_id in page.ids:
                owner = self.id_pages.setdefault(resource_id, page)
                if owner is not page:
                    log.warning("%s: %s is already listed by %s", file.src_uri, resource_id, owner.file.src_uri)

    # Rendering entry point

    def render_page(self, markdown: str, page: Page) -> str:
        self.page = page
        self.table_counter = 0
        stash: list[str] = []

        def hide(match):
            stash.append(match.group(0))
            return f"\x00{len(stash) - 1}\x00"

        text = _CODE.sub(hide, markdown)
        text = self._expand(text)
        return re.sub("\x00(\\d+)\x00", lambda m: stash[int(m.group(1))], text)

    def _expand(self, text: str) -> str:
        out: list[str] = []
        pos = 0
        for match in _TEMPLATE_START.finditer(text):
            if match.start() < pos:
                continue
            call = self._parse_call(text, match)
            if call is None:
                continue
            end, name, args, kwargs = call
            handler = getattr(self, f"t_{name}", None)
            out.append(text[pos:match.start()])
            if handler is None:
                log.warning("%s: unknown wiki template '%s'", self.page.file.src_uri, name)
                out.append(text[match.start():end])
            else:
                try:
                    out.append(handler(*args, **kwargs))
                except Exception as error:  # surface template mistakes as build warnings
                    log.warning("%s: template %s failed: %s", self.page.file.src_uri, name, error)
                    out.append(f'<span class="rw-error">{_esc(name)}: {_esc(error)}</span>')
            pos = end
        out.append(text[pos:])
        return "".join(out)

    def _parse_call(self, text: str, match: re.Match):
        for close in re.finditer(r"\)\s*\}\}", text[match.end():]):
            inner = text[match.start() + 2:match.end() + close.start() + 1].strip()
            try:
                node = ast.parse(inner, mode="eval").body
            except SyntaxError:
                continue
            if not isinstance(node, ast.Call):
                continue
            try:
                args = [ast.literal_eval(a) for a in node.args]
                kwargs = {k.arg: ast.literal_eval(k.value) for k in node.keywords}
            except ValueError:
                continue
            return match.end() + close.end(), match.group(1), args, kwargs
        log.warning("%s: could not parse wiki template at '%s'", self.page.file.src_uri, text[match.start():match.start() + 60])
        return None

    def _inline(self, text: str) -> str:
        """Expand templates plus Markdown links and bold, for text placed inside raw HTML."""

        def link(match: re.Match) -> str:
            label, target = match.group(1), match.group(2)
            if re.match(r"^[a-z]+:", target):
                return f'<a href="{_esc(target)}">{label}</a>'
            path, _, anchor = target.partition("#")
            src = posixpath.normpath(posixpath.join(posixpath.dirname(self.page.file.src_uri), path))
            href = self.doc_href(src) + (f"#{anchor}" if anchor else "")
            return f'<a href="{_esc(href)}">{label}</a>'

        text = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", link, text)
        text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
        return self._expand(text)

    # URL helpers

    def icon_url(self, resource_id: str) -> str:
        self.used_icons.add(resource_id)
        namespace, path = split_id(resource_id)
        return get_relative_url(f"{ICON_DIR}/{namespace}/{path}.png", self.page.url)

    def page_href(self, resource_id: str) -> str | None:
        target = self.id_pages.get(resource_id)
        if target is not None:
            if target.file.src_uri == self.page.file.src_uri:
                return None
            return get_relative_url(target.file.url, self.page.url)
        namespace, _ = split_id(resource_id)
        if namespace == "minecraft":
            return "https://minecraft.wiki/w/" + quote(self.res.name(resource_id).replace(" ", "_"))
        return None

    def doc_href(self, src_uri: str) -> str:
        file = self.files.get_file_from_path(src_uri) if self.files else None
        if file is None:
            log.warning("%s: linked doc '%s' does not exist", self.page.file.src_uri, src_uri)
            return "#"
        return get_relative_url(file.url, self.page.url)

    # Ingredient and result parsing

    def _fluid_icon(self, fluid_id: str) -> str:
        namespace, path = split_id(fluid_id)
        bucket = f"{namespace}:{path}_bucket"
        return bucket if self.res.has_item(bucket) else fluid_id

    def _is_fluid(self, resource_id: str) -> bool:
        namespace, path = split_id(resource_id)
        return f"fluid.{namespace}.{path}" in self.res.lang or resource_id in {"minecraft:water", "minecraft:lava"}

    def ingredient(self, data, label: str | None = None) -> Stack | None:
        if data is None:
            return None
        if isinstance(data, list):
            options = [o for d in data if (s := self.ingredient(d)) for o in s.options]
            return Stack(options, label=label)
        if data.get("type") == "neoforge:compound":
            children = self.ingredient(data.get("children", data.get("ingredients", [])), label)
            return Stack(list(dict.fromkeys(children.options)), label=label) if children else None
        if data.get("type") == "neoforge:components":
            items = data["items"] if isinstance(data["items"], list) else [data["items"]]
            return self.ingredient([{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in items], label)
        if "ingredient" in data:
            stack = self.ingredient(data["ingredient"], label)
            if stack is None:
                return None
            if "amount" in data:
                stack.count, stack.fluid = data["amount"], True
            elif "count" in data:
                stack.count = data["count"]
            if "calibration" in data:
                c = data["calibration"]
                stack.note = f"{c.get('family', 'any').title()} calibration, Stage {c.get('min_stage', 0)}+, stability {c.get('min_stability', 0)}+"
            return stack
        if "fluid" in data:
            return Stack([data["fluid"]], count=data.get("amount", 1000), fluid=True, label=label)
        if "item" in data:
            return Stack([data["item"]], count=data.get("count", 1), label=label)
        if "tag" in data:
            return Stack(list(self.res.tag_items(data["tag"])), count=data.get("count", 1), label=label, tag=data["tag"])
        return None

    def result(self, data, label: str | None = None) -> Stack | None:
        if not isinstance(data, dict):
            return None
        if "stack" in data:
            stack = self.result(data["stack"], label)
            if stack is None:
                return None
            notes = []
            if data.get("requires_filter"):
                notes.append("needs a Recovery Filter")
            if "stage" in data:
                notes.append(f"Stage {data['stage']}")
            for key in ("stability", "refinement_potential"):
                if key in data:
                    notes.append(f"{_label(key).lower()} {_format_cell(key, data[key])}")
            stack.note = ", ".join(notes) or None
            return stack
        if "id" not in data:
            return None
        fluid = "amount" in data or self._is_fluid(data["id"])
        count = data.get("amount", 1000) if fluid else data.get("count", 1)
        return Stack([data["id"]], count=count, fluid=fluid, label=label)

    def split_recipe(self, recipe: dict) -> tuple[list[Stack], list[Stack], dict]:
        inputs: list[Stack] = []
        outputs: list[Stack] = []
        scalars: dict = {}
        for key, value in recipe.items():
            if key in _IGNORED_KEYS:
                continue
            is_output = isinstance(value, (dict, list)) and (key in _OUTPUT_KEYS or key.endswith("_output"))
            role = None if key in {"ingredient", "ingredients", "result", "results", "outputs"} else _label(key)
            if is_output:
                values = value if isinstance(value, list) else [value]
                outputs.extend(s for v in values if (s := self.result(v, role)))
            elif isinstance(value, dict) and ({"item", "tag", "ingredient", "fluid"} & set(value)):
                stack = self.ingredient(value, role)
                if stack:
                    if key == "ingredient" and "input_count" in recipe:
                        stack.count = recipe["input_count"]
                    if key == "catalyst" and "catalyst_count" in recipe:
                        stack.count = recipe["catalyst_count"]
                    inputs.append(stack)
            elif isinstance(value, list) and value and isinstance(value[0], dict):
                if key.endswith("ingredients"):
                    inputs.extend(s for v in value if (s := self.ingredient(v, role)))
                elif stack := self.ingredient(value, role):
                    inputs.append(stack)
            elif not isinstance(value, (dict, list)):
                scalars[key] = value
        return inputs, outputs, scalars

    # Slot rendering

    def slot(self, stack: Stack | None, large: bool = False) -> str:
        classes = "rw-slot" + (" rw-slot-large" if large else "")
        if stack is None:
            return f'<span class="{classes}"></span>'
        options = stack.options or []
        if not options:
            title = f"#{stack.tag}" if stack.tag else "Unknown"
            return f'<span class="{classes}" title="{_esc(title)}"><span class="rw-missing">?</span></span>'
        if len(options) > 1:
            classes += " rw-cycle"
        parts = []
        for index, resource_id in enumerate(options):
            name = self.res.name(resource_id)
            title = name
            if stack.fluid:
                title += f" ({stack.count:,} mB)"
            if stack.tag:
                title += f" (any #{stack.tag})"
            if stack.label:
                title += f" — {stack.label}"
            if stack.note:
                title += f" — {stack.note}"
            icon_id = self._fluid_icon(resource_id) if stack.fluid else resource_id
            img = f'<img src="{self.icon_url(icon_id)}" alt="{_esc(name)}" loading="lazy">'
            href = self.page_href(resource_id)
            on = " rw-on" if index == 0 else ""
            if href:
                parts.append(f'<a class="rw-alt{on}" href="{_esc(href)}" title="{_esc(title)}">{img}</a>')
            else:
                parts.append(f'<span class="rw-alt{on}" title="{_esc(title)}">{img}</span>')
        if stack.fluid:
            parts.append(f'<span class="rw-count">{_compact_amount(stack.count)}</span>')
        elif stack.count > 1:
            parts.append(f'<span class="rw-count">{stack.count}</span>')
        if stack.note or (stack.label and not stack.fluid):
            parts.append('<span class="rw-flag"></span>')
        return f'<span class="{classes}">{"".join(parts)}</span>'

    def stack_name(self, stack: Stack) -> str:
        if not stack.options:
            return f"#{stack.tag}"
        if stack.tag and len(stack.options) > 1:
            return "Any " + stack.tag.rpartition("/")[2].rpartition(":")[2].replace("_", " ").title()
        name = self.res.name(stack.options[0])
        if stack.fluid:
            name += f" ({stack.count:,} mB)"
        return name

    # Templates: inline

    def t_item(self, resource_id: str, text: str | None = None, link: bool = True) -> str:
        name = text or self.res.name(resource_id)
        img = f'<img class="rw-sprite" src="{self.icon_url(resource_id)}" alt="">'
        href = self.page_href(resource_id) if link else None
        if href:
            return f'<a class="rw-item" href="{_esc(href)}">{img}{_esc(name)}</a>'
        return f'<span class="rw-item">{img}{_esc(name)}</span>'

    def t_slot(self, resource_id: str, count: int = 1) -> str:
        return self.slot(Stack([resource_id], count=count))

    def t_tag(self, tag: str, cycle: bool = False) -> str:
        items = self.res.tag_items(tag)
        if not items:
            return f'<p class="rw-empty">No items in <code>#{_esc(tag)}</code>.</p>'
        if cycle:
            return self.slot(Stack(list(items), tag=tag))
        return f'<span class="rw-tag-items">{"".join(self.slot(Stack([i])) for i in items)}</span>'

    # Templates: infobox

    def t_infobox(self, title: str | None = None, variants=None, fields: dict | None = None, caption: str | None = None) -> str:
        current = self.pages.get(self.page.file.src_uri)
        title = title or (current.title if current else self.page.title)
        if variants is None:
            variants = [[self.res.name(i), i] for i in (current.ids if current else [])]
        variants = [[self.res.name(v), v] if isinstance(v, str) else list(v) for v in variants]
        images, tabs = [], []
        for index, (label, resource_id) in enumerate(variants):
            on = " rw-on" if index == 0 else ""
            images.append(
                f'<img class="rw-variant-image{on}" data-index="{index}" src="{self.icon_url(resource_id)}" '
                f'alt="{_esc(self.res.name(resource_id))}">'
            )
            tabs.append(f'<button type="button" class="rw-variant-tab{on}" data-index="{index}" '
                        f'title="{_esc(self.res.name(resource_id))}">{_esc(label)}</button>')
        rows = []
        for key, value in (fields or {}).items():
            rows.append(f"<tr><th>{_esc(key)}</th><td>{self._inline(str(value))}</td></tr>")
        tool = self._tool_row([v[1] for v in variants])
        if tool:
            rows.append(tool)
        parts = [f'<aside class="rw-infobox"><div class="rw-infobox-title">{_esc(title)}</div>',
                 f'<div class="rw-infobox-image">{"".join(images)}</div>']
        if len(tabs) > 1:
            parts.append(f'<div class="rw-variant-tabs">{"".join(tabs)}</div>')
        if caption:
            parts.append(f'<div class="rw-infobox-caption">{self._inline(caption)}</div>')
        parts.append(f'<table>{"".join(rows)}</table></aside>')
        return "".join(parts)

    def _tool_row(self, ids: list[str]) -> str | None:
        blocks = [i for i in ids if self.res.is_block(i)]
        if not blocks:
            return None
        for tool in ("pickaxe", "axe", "shovel", "hoe"):
            mineable = self.res.block_tag_values(f"minecraft:mineable/{tool}")
            if all(b in mineable for b in blocks):
                tier = "wooden"
                for level in ("stone", "iron", "diamond"):
                    if all(b in self.res.block_tag_values(f"minecraft:needs_{level}_tool") for b in blocks):
                        tier = level
                return f"<tr><th>Tool</th><td>{self.t_item(f'minecraft:{tier}_{tool}', tool.title())}</td></tr>"
        return "<tr><th>Tool</th><td>Any tool</td></tr>"

    # Templates: recipes

    def _crafting_recipes(self, result_ids: list[str]) -> list[tuple[str, dict]]:
        found = []
        for resource_id in result_ids:
            for recipe_id, recipe in self.res.recipes.items():
                result = recipe.get("result", {})
                # Component variants, such as dyed Cable, are described in page text instead of the item's table.
                if recipe.get("type") in _CRAFTING_TYPES and result.get("id") == resource_id and "components" not in result:
                    found.append((recipe_id, recipe))
        return found

    def crafting_grid(self, recipe: dict) -> str:
        cells: list[Stack | None] = [None] * 9
        if "pattern" in recipe:
            pattern = recipe["pattern"]
            row_offset = (3 - len(pattern)) // 2 if len(pattern) < 3 else 0
            for r, row in enumerate(pattern):
                for c, symbol in enumerate(row):
                    if symbol != " ":
                        cells[(r + row_offset) * 3 + c] = self.ingredient(recipe["key"][symbol])
            shapeless = False
        else:
            for index, data in enumerate(recipe.get("ingredients", [])[:9]):
                cells[index] = self.ingredient(data)
            shapeless = True
        output = self.result(recipe["result"])
        grid = "".join(self.slot(cell) for cell in cells)
        marker = '<span class="rw-shapeless" title="Shapeless: ingredients can go in any slot">⇄</span>' if shapeless else ""
        return (f'<span class="rw-crafting"><span class="rw-crafting-grid">{grid}</span>'
                f'<span class="rw-arrow">{marker}</span>{self.slot(output, large=True)}</span>')

    def recipe_notes(self, recipe: dict) -> list[str]:
        notes = []
        if recipe.get("type") == "rngtech:tool_damage_shapeless":
            tool = self.ingredient(recipe.get("tool"))
            if tool and tool.options:
                notes.append(f"The {self.t_item(tool.options[0])} loses {recipe.get('tool_damage', 1)} durability instead of being used up.")
        for symbol, data in recipe.get("key", {}).items():
            if isinstance(data, dict) and "calibration" in data:
                stack = self.ingredient(data)
                notes.append(f"{self.t_item(stack.options[0])} must be a {stack.note}.")
        for condition in recipe.get("neoforge:conditions", []):
            if condition.get("type") == "rngtech:material_enabled":
                notes.append(f"Requires the {condition['material'].replace('_', ' ').title()} material to be enabled.")
            elif condition.get("type") == "neoforge:mod_loaded":
                notes.append(f"Only available when the <code>{_esc(condition['modid'])}</code> mod is installed.")
        return notes

    def t_crafting(self, *ids: str, notes: bool = True) -> str:
        current = self.pages.get(self.page.file.src_uri)
        result_ids = list(ids) or (current.ids if current else [])
        recipes = self._crafting_recipes(result_ids)
        if not recipes:
            return '<p class="rw-empty">No crafting recipe.</p>'
        rows = []
        any_notes = False
        for _, recipe in recipes:
            inputs = [self.ingredient(recipe["key"][s]) for row in recipe.get("pattern", []) for s in row if s != " "]
            inputs += [self.ingredient(d) for d in recipe.get("ingredients", [])]
            names = list(dict.fromkeys(self.stack_name(s) for s in inputs if s))
            note_list = self.recipe_notes(recipe) if notes else []
            any_notes |= bool(note_list)
            rows.append((names, self.crafting_grid(recipe), note_list, self.res.name(recipe["result"]["id"])))
        head = "<tr><th>Ingredients</th><th>Crafting recipe</th>" + ("<th>Notes</th>" if any_notes else "") + "</tr>"
        several = len({result for *_, result in rows}) > 1
        body = []
        for names, grid, note_list, result in rows:
            heading = f"<strong>{_esc(result)}</strong><br>" if several else ""
            cells = f"<td>{heading}{' +<br>'.join(_esc(n) for n in names)}</td><td>{grid}</td>"
            if any_notes:
                cells += f"<td>{'<br>'.join(note_list)}</td>"
            body.append(f"<tr>{cells}</tr>")
        return f'<div class="rw-recipes"><table><thead>{head}</thead><tbody>{"".join(body)}</tbody></table></div>'

    def _matches(self, stacks: list[Stack], resource_id: str | None) -> bool:
        return resource_id is None or any(resource_id in s.options for s in stacks)

    def t_processing(self, type: str, input: str | None = None, output: str | None = None,
                     columns: list[str] | None = None, hide: list[str] | None = None, limit: int | None = None) -> str:
        recipe_type = type if ":" in type else f"{MOD_ID}:{type}"
        rows = []
        for recipe_id, recipe in self.res.recipes.items():
            if recipe.get("type") != recipe_type:
                continue
            inputs, outputs, scalars = self.split_recipe(recipe)
            if self._matches(inputs, input) and self._matches(outputs, output):
                rows.append((recipe_id, inputs, outputs, scalars))
        if not rows:
            return '<p class="rw-empty">No recipes.</p>'
        if limit:
            rows = rows[:limit]
        keys: list[str] = []
        for _, _, _, scalars in rows:
            keys.extend(k for k in scalars if k not in keys)
        keys.sort(key=lambda k: _LEADING_COLUMNS.index(k) if k in _LEADING_COLUMNS else len(_LEADING_COLUMNS))
        if columns:
            keys = [k for k in columns if k in keys]
        keys = [k for k in keys if k not in (hide or [])]
        self.table_counter += 1
        table_id = f"rw-table-{self.table_counter}"
        head = "<tr><th>Input</th><th></th><th>Output</th>" + "".join(f"<th>{_esc(_label(k))}</th>" for k in keys) + "</tr>"
        body = []
        for _, inputs, outputs, scalars in rows:
            search = " ".join(self.res.name(o) for s in inputs + outputs for o in s.options[:1]).lower()
            cells = (f'<td class="rw-slots">{"".join(self.slot(s) for s in inputs)}</td><td class="rw-arrow-cell">→</td>'
                     f'<td class="rw-slots">{"".join(self.slot(s) for s in outputs) or "—"}</td>')
            cells += "".join(f"<td>{_esc(_format_cell(k, scalars[k])) if k in scalars else ''}</td>" for k in keys)
            body.append(f'<tr data-search="{_esc(search)}">{cells}</tr>')
        filter_box = ""
        if len(rows) > 12:
            filter_box = (f'<input class="rw-filter" type="search" data-table="{table_id}" '
                          f'placeholder="Filter {len(rows)} recipes by item name" aria-label="Filter recipes">')
        return (f'<div class="rw-recipes">{filter_box}<table id="{table_id}">'
                f'<thead>{head}</thead><tbody>{"".join(body)}</tbody></table></div>')

    def t_crafting_usage(self, *ids: str) -> str:
        current = self.pages.get(self.page.file.src_uri)
        targets = set(ids or (current.ids if current else []))
        rows = []
        for _, recipe in self.res.recipes.items():
            if recipe.get("type") not in _CRAFTING_TYPES:
                continue
            stacks = [self.ingredient(v) for v in recipe.get("key", {}).values()]
            stacks += [self.ingredient(v) for v in recipe.get("ingredients", [])]
            if any(s and targets & set(s.options) for s in stacks):
                rows.append(f"<tr><td>{self.t_item(recipe['result']['id'])}</td><td>{self.crafting_grid(recipe)}</td></tr>")
        if not rows:
            return '<p class="rw-empty">Not used in any crafting recipe.</p>'
        return (f'<div class="rw-recipes"><table><thead><tr><th>Result</th><th>Crafting recipe</th></tr></thead>'
                f'<tbody>{"".join(rows)}</tbody></table></div>')

    # Templates: reference tables and navigation

    def t_data_values(self, *ids: str) -> str:
        current = self.pages.get(self.page.file.src_uri)
        rows = []
        for resource_id in ids or (current.ids if current else []):
            kind = "Block" if self.res.is_block(resource_id) else "Item"
            key = self.res.translation_key(resource_id) or ""
            rows.append(f"<tr><td>{self.t_item(resource_id, link=False)}</td><td><code>{_esc(resource_id)}</code></td>"
                        f"<td>{kind}</td><td><code>{_esc(key)}</code></td></tr>")
        return ('<div class="rw-data"><table><thead><tr><th>Name</th><th>Resource location</th><th>Form</th>'
                f'<th>Translation key</th></tr></thead><tbody>{"".join(rows)}</tbody></table></div>')

    def _grouped_pages(self) -> list[tuple[str, list[WikiPage]]]:
        groups = []
        for category in CATEGORIES + sorted({p.category for p in self.pages.values() if p.category} - set(CATEGORIES)):
            members = sorted((p for p in self.pages.values() if p.category == category), key=lambda p: p.title)
            if members:
                groups.append((category, members))
        return groups

    def _page_link(self, target: WikiPage, icon_class: str = "rw-sprite") -> str:
        img = f'<img class="{icon_class}" src="{self.icon_url(target.icon)}" alt="">' if target.icon else ""
        if target.file.src_uri == self.page.file.src_uri:
            return f'<strong class="rw-item">{img}{_esc(target.title)}</strong>'
        href = get_relative_url(target.file.url, self.page.url)
        return f'<a class="rw-item" href="{_esc(href)}">{img}{_esc(target.title)}</a>'

    def t_navbox(self) -> str:
        rows = "".join(
            f'<tr><th>{_esc(category)}</th><td>{" · ".join(self._page_link(p) for p in members)}</td></tr>'
            for category, members in self._grouped_pages()
        )
        home = get_relative_url(self.files.get_file_from_path("index.md").url, self.page.url)
        return (f'<details class="rw-navbox" open><summary><a href="{home}">RNGTech wiki</a></summary>'
                f"<table>{rows}</table></details>")

    def t_gallery(self) -> str:
        sections = []
        for category, members in self._grouped_pages():
            cards = "".join(
                f'<a class="rw-card" href="{_esc(get_relative_url(p.file.url, self.page.url))}">'
                f'<img src="{self.icon_url(p.icon)}" alt="" loading="lazy"><span>{_esc(p.title)}</span></a>'
                for p in members if p.icon
            )
            sections.append(f'<h2 id="{_esc(category.lower().replace(" ", "-"))}">{_esc(category)}</h2>'
                            f'<div class="rw-gallery">{cards}</div>')
        return "\n\n".join(sections)

    # Icons

    def write_icons(self, site_dir: str) -> None:
        root = Path(site_dir) / ICON_DIR
        for resource_id in sorted(self.used_icons):
            namespace, path = split_id(resource_id)
            target = root / namespace / f"{path}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(_icon_png(self.res, resource_id))


_ICON_CACHE: dict[str, bytes] = {}


def _icon_png(resources: Resources, resource_id: str) -> bytes:
    if resource_id not in _ICON_CACHE:
        image = icons.render(resources, resource_id) or icons.placeholder()
        buffer = io.BytesIO()
        image.save(buffer, "PNG", optimize=True)
        _ICON_CACHE[resource_id] = buffer.getvalue()
    return _ICON_CACHE[resource_id]
