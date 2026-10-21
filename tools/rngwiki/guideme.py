"""Exports the player wiki as the GuideME in-game guide.

Renders the same wiki templates as the MkDocs hook, but into GuideME Markdown shaped like the AE2 guide: short
pages with a block render, reference tables left to JEI and the machine screens, long sections moved to child
pages, and one index page per wiki category. The guide is generated, committed under src/guide/resources, and
shipped in the mod jar; it does nothing unless the optional GuideME mod is installed. See
docs/reference/wiki-authoring.md.

    python tools/rngwiki/guideme.py              # regenerate src/guide/resources after a wiki change
    python tools/rngwiki/guideme.py --check      # fail if the committed guide is out of date (CI)
"""

from __future__ import annotations

import argparse
import filecmp
import json
import re
import shutil
import sys
import tempfile
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from mkdocs.utils.meta import get_data  # noqa: E402

from rngwiki.resources import MOD_ID, Resources, split_id  # noqa: E402
from rngwiki import chains  # noqa: E402
from rngwiki.site import _CRAFTING_TYPES, CATEGORIES, Stack, WikiPage, WikiSite, _format_cell, _label, flow_label  # noqa: E402
from rngwiki.vanilla import find_client_jar  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
WIKI_URL = "https://c-pettersson.github.io/rngtech/"
GUIDE_ROOT = REPO / "src/guide/resources"
GUIDE_ID = f"{MOD_ID}:guide"
GUIDE_DEFINITION = {
    "item_settings": {
        "display_name": {"text": "RNGTech Guide"},
        "tooltip_lines": [{"text": "Machines, Gear, and recipes", "color": "gray"}],
    }
}
# The wiki's front page is replaced by the guide index; pack-maker settings do not belong in a player's book.
EXCLUDED_PAGES = {"index", "modpack-creators"}

# A section heavier than this moves to its own child page. Weight is visible text, with each recipe box counted
# as a block of text, so a run of crafting grids splits off like a long table does.
SPLIT_WEIGHT = 1000
PAGE_BUDGET = 2500
MIN_SPLIT_WEIGHT = 400
_RECIPE_WEIGHT = 400
_ITEM_LINK_WEIGHT = 14

# Reference sections the game already shows: full recipe lists (JEI), ascendancy trees (the Mastery tab),
# resource ids, and the wiki's own cross-links.
_DROP = "\x01drop\x01"
_DROP_TITLES = {"See also", "Data values"}
# Wiki sections that describe the web page itself, keyed by (page, heading id), with the guide's own version.
_GUIDE_SECTIONS = {
    ("power-chains.md", "reading"): """\
Each chain shows its machines from the first step down to the last. The table under it lists every flow: the machine
that makes the resource, how much one run makes or uses, the rate at base speed, and the machine that takes it.
Click a linked name to open that machine's page.

- One number, such as **1,000 mB**, means both machines make and use the same amount.
- Two numbers, such as **500 → 100 mB**, mean the first machine makes 500 mB per run and the second uses 100 mB per
  run. Compare the rates to see how many of each machine you need.
- *You supply* marks something you bring yourself, such as mined coal. *Leaves the chain* marks a product that goes on
  to another chain.
- ↺ marks a flow that loops back to an earlier machine.

The rates are the recipes' base times, before machine stats, Gear, Mastery, and settings such as mixture, aeration,
or throttle.

Most gas machines also use water. To keep the chains readable, water appears only under [Water supply](#water).""",
}
# AE2 pages describe the block first and end with its recipe.
_LAST_TITLES = ["Obtaining"]

# Guide-only markup put in front of a wiki section's text, keyed like _GUIDE_SECTIONS: scenes the site cannot draw.
_GUIDE_PREFIXES = {
    ("phase-array.md", "building"): """<GameScene zoom="2" interactive={true}>
  <Block id="rngtech:exotic_affix_forge" x="0" y="0" z="0" p:facing="east" />
  <Block id="rngtech:phase_coil" x="1" y="0" z="0" p:facing="east" p:formed="true" p:tier="2" />
  <Block id="rngtech:phase_coil" x="2" y="0" z="0" p:facing="east" p:formed="true" p:tier="2" />
  <Block id="rngtech:phase_coil" x="3" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_coil" x="4" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_coil" x="5" y="0" z="0" p:facing="east" p:formed="true" p:tier="1" />
  <Block id="rngtech:phase_emitter" x="6" y="0" z="0" p:facing="west" p:formed="true" />
</GameScene>

*A five-Coil line: the Cradle at the left, the Emitter at the right, positions counted from the Emitter.*
""",
}

_LINK = re.compile(r"\[([^\]]+)\]\(([^)\s]+)\)")
_HTML_COMMENT = re.compile(r"<!--.*?-->\n?", re.S)
_CODE = re.compile(r"<code>(.*?)</code>")
_HEADING = re.compile(r"^(#{1,6})\s+(.+?)(?:\s*\{#([\w-]+)\})?\s*$")


class _Source:
    def __init__(self, src_uri: str):
        self.src_uri = src_uri


class _PageRef:
    def __init__(self, page: WikiPage):
        self.file = page.file
        self.title = page.title


def _wiki_slug(title: str) -> str:
    """MkDocs' default heading id, which wiki links use as anchors."""
    text = unicodedata.normalize("NFKD", title)
    text = re.sub(r"[^\w\s-]", "", text).strip().lower()
    return re.sub(r"[-\s]+", "-", text)


def _guide_anchor(title: str) -> str:
    """GuideME's heading anchor."""
    return re.sub(r"\s+", "-", title.strip().lower())


def _file_slug(title: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", unicodedata.normalize("NFKD", title).lower()).strip("-")


def _weight(text: str) -> int:
    text = re.sub(r"<Recipes?For\b[^>]*/>", "x" * _RECIPE_WEIGHT, text)
    text = re.sub(r"<ItemLink\b[^>]*/>", "x" * _ITEM_LINK_WEIGHT, text)
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"(?m)^\|[-|: ]+\|$", "", text)
    return len(re.sub(r"\s+", " ", text))


@dataclass
class Section:
    level: int
    title: str
    wiki_id: str | None = None  # an explicit {#id} on the wiki heading, which wiki links use instead of the slug
    lead: list[str] = field(default_factory=list)
    children: list[Section] = field(default_factory=list)
    stub: str | None = None  # child page id, once the section moved there

    def text(self) -> str:
        if self.stub:
            return self.title
        return "\n".join(self.lead) + "".join(c.text() for c in self.children)

    def walk(self):
        yield self
        for child in self.children:
            yield from child.walk()


def _parse(markdown: str) -> Section:
    root = Section(1, "")
    stack = [root]
    for line in markdown.splitlines():
        match = _HEADING.match(line)
        if match and len(match.group(1)) > 1:
            section = Section(len(match.group(1)), match.group(2), match.group(3))
            while stack[-1].level >= section.level:
                stack.pop()
            stack[-1].children.append(section)
            stack.append(section)
        else:
            stack[-1].lead.append(line)
    return root


def _render(section: Section, shift: int = 0, with_heading: bool = True) -> str:
    out = []
    if with_heading and section.title:
        out.append(f"{'#' * (section.level - shift)} {section.title}\n")
    lead = "\n".join(section.lead).strip("\n")
    if lead:
        out.append(lead + "\n")
    stubs: list[str] = []
    # A page's own top-level sections that moved out entirely are listed once at the end, so their links do not
    # read as part of the section before them.
    deferred: list[str] = []
    for child in section.children:
        if child.stub:
            (deferred if section.level == 1 else stubs).append(f"- [{child.title}]({child.stub})")
            continue
        if stubs:
            out.append("\n".join(stubs) + "\n")
            stubs = []
        out.append(_render(child, shift))
    if stubs:
        out.append("\n".join(stubs) + "\n")
    if deferred:
        out.append("## More\n\n" + "\n".join(deferred) + "\n")
    return "\n".join(out)


@dataclass
class GuidePage:
    path: str  # relative to the guide root, e.g. crusher/hardness.md
    front: dict
    body: str

    @property
    def page_id(self) -> str:
        return f"{MOD_ID}:{self.path}"


def _front_matter(front: dict) -> str:
    lines = ["---", "navigation:"]
    for key, value in front["navigation"].items():
        lines.append(f'  {key}: "{value}"' if key == "title" else f"  {key}: {value}")
    for key in ("categories", "item_ids"):
        if front.get(key):
            lines.append(f"{key}:")
            lines.extend(f'  - "{v}"' if key == "categories" else f"  - {v}" for v in front[key])
    return "\n".join(lines + ["---", ""]) + "\n"


class GuideExporter(WikiSite):
    def __init__(self, wiki_dir: Path, names: list[str]):
        self.res = Resources([REPO / "src/main/resources", REPO / "src/generated/resources"], find_client_jar())
        self.pages: dict[str, WikiPage] = {}
        self.id_pages: dict[str, WikiPage] = {}
        self.files = None
        self.page = None
        self.used_icons: set[str] = set()
        self.table_counter = 0
        self.sources: dict[str, str] = {}
        # (wiki page, wiki anchor) -> (guide page id, GuideME anchor or None)
        self.anchors: dict[tuple[str, str], tuple[str, str | None]] = {}
        for name in names:
            src_uri = f"{name}.md"
            source = (wiki_dir / src_uri).read_text(encoding="utf-8")
            body, meta = get_data(source)
            wiki = meta.get("wiki") or {}
            heading = re.search(r"^#\s+(.+)$", body, re.M)
            title = meta.get("title") or (heading.group(1).strip() if heading else name)
            page = WikiPage(_Source(src_uri), title, wiki.get("category", ""), wiki.get("icon"), list(wiki.get("ids", [])))
            self.pages[src_uri] = page
            self.sources[src_uri] = body
            for resource_id in page.ids:
                self.id_pages.setdefault(resource_id, page)

    # Pages

    def export(self, root: Path) -> list[Path]:
        """Writes the guide definition and every page as a resource tree under root, replacing what was there."""
        guide_pages = self._category_pages()
        for src_uri, page in self.pages.items():
            guide_pages.extend(self._machine_pages(src_uri, page))
        guide_pages.insert(0, self._index())
        if root.exists():
            shutil.rmtree(root)
        namespace, path = split_id(GUIDE_ID)
        definition = root / f"assets/{namespace}/guideme_guides/{path}.json"
        definition.parent.mkdir(parents=True)
        definition.write_text(json.dumps(GUIDE_DEFINITION, indent=4) + "\n", encoding="utf-8", newline="\n")
        pages_dir = root / pages_path()
        written = [definition]
        for guide_page in guide_pages:
            current = guide_page.path
            body = _LINK.sub(lambda m: self._link(m, current), guide_page.body)
            target = pages_dir / guide_page.path
            target.parent.mkdir(parents=True, exist_ok=True)
            text = _front_matter(guide_page.front) + body.strip() + "\n"
            text = re.sub(r"[ \t]+\n", "\n", re.sub(r"\n{3,}", "\n\n", text))
            target.write_text(text, encoding="utf-8", newline="\n")
            written.append(target)
        return written

    def _categories(self) -> list[str]:
        used = {p.category for p in self.pages.values() if p.category}
        return [c for c in CATEGORIES if c in used] + sorted(used - set(CATEGORIES))

    def _index(self) -> GuidePage:
        body = (
            "# RNGTech Guide\n\n"
            "RNGTech treats machines like RPG characters: every machine, Gear part, and Battery Cell rolls its own "
            "rarity and affixes. Start with Getting Started, then look up a machine when you build it.\n\n"
            "Hover an item and hold the guide key to open its page. Recipe lists are in JEI.\n\n"
            "<SubPages />\n\n"
            f"The full wiki is online at [{WIKI_URL}]({WIKI_URL}).\n"
        )
        return GuidePage("index.md", {"navigation": {"title": "RNGTech", "icon": "rngtech:iron_crusher_chassis"}}, body)

    def _category_pages(self) -> list[GuidePage]:
        pages = []
        for position, category in enumerate(self._categories()):
            icon = next((p.icon for p in self.pages.values() if p.category == category and p.icon), None)
            navigation = {"title": category, "parent": "index.md", "position": position}
            if icon:
                navigation["icon"] = icon
            body = f"# {category}\n\n<CategoryIndex category=\"{category}\" />\n"
            pages.append(GuidePage(f"{_file_slug(category)}.md", {"navigation": navigation}, body))
        return pages

    def _machine_pages(self, src_uri: str, page: WikiPage) -> list[GuidePage]:
        self.page = _PageRef(page)
        self.table_counter = 0
        text = self.sources[src_uri].replace("<!-- ascendancy-trees:start -->", _DROP)
        text = _HTML_COMMENT.sub("", text)
        text = _CODE.sub(r"`\1`", self.render_page(text, self.page))
        root = _parse(text)
        self._drop(root)
        for section in root.walk():
            replacement = _GUIDE_SECTIONS.get((src_uri, section.wiki_id))
            if replacement:
                section.lead, section.children = ["", replacement], []
            prefix = _GUIDE_PREFIXES.get((src_uri, section.wiki_id))
            if prefix:
                section.lead = ["", prefix] + section.lead
        root.children.sort(key=lambda s: _LAST_TITLES.index(s.title) + 1 if s.title in _LAST_TITLES else 0)

        name = src_uri[:-3]
        main = GuidePage(src_uri, {"navigation": {"title": page.title}}, "")
        navigation = main.front["navigation"]
        navigation["parent"] = f"{_file_slug(page.category)}.md" if page.category else "index.md"
        if page.icon:
            navigation["icon"] = page.icon
        if page.category:
            main.front["categories"] = [page.category]
        main.front["item_ids"] = page.ids

        for section in root.walk():
            if section.title:
                self.anchors[(src_uri, section.wiki_id or _wiki_slug(section.title))] = (f"{MOD_ID}:{src_uri}", _guide_anchor(section.title))
        children = self._split(root, src_uri, page)
        main.body = _render(root)
        return [main] + children

    def _split(self, holder: Section, holder_path: str, page: WikiPage) -> list[GuidePage]:
        """Moves holder's heavy sections to child pages, then does the same inside each child page."""
        children = []
        for position, candidate in enumerate(self._split_candidates(holder)):
            child_path = f"{holder_path[:-3]}/{_file_slug(candidate.title)}.md"
            candidate.stub = f"{MOD_ID}:{child_path}"
            for section in candidate.walk():
                anchor = None if section is candidate else _guide_anchor(section.title)
                self.anchors[(page.file.src_uri, section.wiki_id or _wiki_slug(section.title))] = (candidate.stub, anchor)
            grandchildren = self._split(candidate, child_path, page)
            navigation = {"title": candidate.title, "parent": holder_path, "position": position}
            if page.icon:
                navigation["icon"] = page.icon
            body = f"# {candidate.title}\n\n" + _render(candidate, candidate.level - 1, with_heading=False)
            children += [GuidePage(child_path, {"navigation": navigation}, body)] + grandchildren
        return children

    def _drop(self, section: Section) -> None:
        section.children = [
            c for c in section.children
            if c.title not in _DROP_TITLES and not any(_DROP in line for line in c.lead)
        ]
        for child in section.children:
            self._drop(child)
        section.lead = [line for line in section.lead if _DROP not in line]

    def _split_candidates(self, holder: Section) -> list[Section]:
        pool = [section for top in holder.children for section in (top.children or [top])]
        weights = {id(s): _weight(s.text()) for s in pool}
        chosen = {id(s) for s in pool if weights[id(s)] > SPLIT_WEIGHT}
        remaining = _weight(holder.text()) - sum(weights[i] for i in chosen)
        # A page made of many mid-sized sections still reads as a wall; split the heaviest until it fits.
        for section in sorted(pool, key=lambda s: -weights[id(s)]):
            if remaining <= PAGE_BUDGET or weights[id(section)] < MIN_SPLIT_WEIGHT:
                break
            if id(section) not in chosen:
                chosen.add(id(section))
                remaining -= weights[id(section)]
        return [s for s in pool if id(s) in chosen]

    def _link(self, match: re.Match, current: str) -> str:
        label, target = match.group(1), match.group(2)
        if re.match(r"^https?:", target) or target.startswith(f"{MOD_ID}:"):
            return match.group(0)
        path, _, anchor = target.partition("#")
        if not path:
            # A same-page anchor; child pages come from their parent's wiki page.
            path = current.split("/")[0].removesuffix(".md") + ".md"
        if path not in self.pages:
            # Pages that are not in the guide stay plain text; the online wiki has them.
            return label
        page_id, guide_anchor = self.anchors.get((path, anchor), (f"{MOD_ID}:{path}", None)) if anchor else (f"{MOD_ID}:{path}", None)
        if page_id == f"{MOD_ID}:{current}" and not guide_anchor:
            return label
        return f"[{label}]({page_id}{'#' + guide_anchor if guide_anchor else ''})"

    # Items

    def _known_item(self, resource_id: str) -> bool:
        return self.res.has_item(resource_id) or self.res.is_block(resource_id)

    def _linkable(self, resource_id: str) -> bool:
        namespace, _ = split_id(resource_id)
        # GuideME reports an error for an ItemLink to one of the guide's own items without a page.
        return self._known_item(resource_id) and (namespace != MOD_ID or resource_id in self.id_pages)

    def item_text(self, resource_id: str, text: str | None = None) -> str:
        if self._linkable(resource_id):
            return f'<ItemLink id="{resource_id}" />'
        return f"**{text or self.res.name(resource_id)}**"

    def stack_text(self, stack: Stack) -> str:
        if not stack.options:
            text = f"`#{stack.tag}`"
        elif stack.fluid:
            text = f"{self.res.name(stack.options[0])} ({stack.count:,} mB)"
        elif stack.tag and len(stack.options) > 1:
            text = f"{self.stack_name(stack)} (`#{stack.tag}`)"
        else:
            text = self.item_text(stack.options[0])
        if stack.count > 1 and not stack.fluid:
            text = f"{stack.count}× {text}"
        extras = [e for e in (stack.label, stack.note) if e]
        if extras:
            text += f" *({', '.join(extras)})*"
        return text

    # Templates

    def t_item(self, resource_id: str, text: str | None = None, link: bool = True) -> str:
        return self.item_text(resource_id, text) if link else f"**{text or self.res.name(resource_id)}**"

    def t_slot(self, resource_id: str, count: int = 1) -> str:
        return self.stack_text(Stack([resource_id], count=count))

    def t_tag(self, tag: str, cycle: bool = False) -> str:
        items = [i for i in self.res.tag_items(tag) if self._known_item(i)]
        if not items:
            return f"`#{tag}`"
        shown = ", ".join(self.item_text(i) for i in items[:3])
        return shown + (f" and {len(items) - 3} more" if len(items) > 3 else "")

    def t_infobox(self, title: str | None = None, variants=None, fields: dict | None = None, caption: str | None = None) -> str:
        current = self.pages.get(self.page.file.src_uri)
        if variants is None:
            variants = current.ids if current else []
        ids = [v if isinstance(v, str) else v[1] for v in variants]
        ids = [i for i in ids if self._known_item(i)]
        if not ids:
            return ""
        hero = current.icon if current and current.icon in ids else ids[0]
        lines = [f'<BlockImage id="{hero}" scale="6" />' if self.res.is_block(hero) else f'<ItemImage id="{hero}" scale="4" />']
        if len(ids) > 1:
            lines += ["", "<Row>", *(f'  <ItemImage id="{i}" scale="2" />' for i in ids), "</Row>"]
        lines.append("")
        for key, value in (fields or {}).items():
            lines.append(f"- **{key}:** {self._expand(str(value))}")
        tool = self.mining_tool(ids)
        if tool is not None:
            lines.append(f"- **Tool:** {self.item_text(tool[0], tool[1]) if tool else 'Any tool'}")
        if caption:
            lines += ["", self._expand(caption)]
        return "\n".join(lines) + "\n"

    def t_crafting(self, *ids: str, notes: bool = True) -> str:
        current = self.pages.get(self.page.file.src_uri)
        result_ids = list(ids) or (current.ids if current else [])
        recipes = self._crafting_recipes(result_ids)
        if not recipes:
            return "No crafting recipe."
        blocks = [f'<RecipesFor id="{i}" />' for i in dict.fromkeys(r["result"]["id"] for _, r in recipes)]
        note_list = list(dict.fromkeys(n for _, r in recipes for n in self.recipe_notes(r))) if notes else []
        return "\n\n".join(blocks) + ("\n\n" + "\n".join(f"- {n}" for n in note_list) if note_list else "")

    def t_crafting_usage(self, *ids: str) -> str:
        current = self.pages.get(self.page.file.src_uri)
        targets = set(ids or (current.ids if current else []))
        results = []
        for _, recipe in self.res.recipes.items():
            if recipe.get("type") not in _CRAFTING_TYPES:
                continue
            stacks = [self.ingredient(v) for v in recipe.get("key", {}).values()]
            stacks += [self.ingredient(v) for v in recipe.get("ingredients", [])]
            if any(s and targets & set(s.options) for s in stacks):
                results.append(recipe["result"]["id"])
        if not results:
            return "Not used in any crafting recipe."
        return "\n".join(f"- {self.item_text(r)}" for r in dict.fromkeys(results))

    def t_processing(self, type: str, input: str | None = None, output: str | None = None,
                     columns: list[str] | None = None, hide: list[str] | None = None, limit: int | None = None,
                     always: bool = False) -> str:
        if input is None and output is None and not always:
            return _DROP  # a machine's full recipe list is JEI's job in game
        rows, keys = self.processing_rows(type, input, output, columns, hide, limit)
        if not rows:
            return "No recipes."
        # A list of operations with no item in or out, such as the Phase Array's, shows only its numbers.
        with_items = any(inputs or outputs for _, inputs, outputs, _ in rows)
        header = (["Input", "Output"] if with_items else []) + [_label(k) for k in keys]
        lines = ["| " + " | ".join(header) + " |", "|" + "---|" * len(header)]
        for _, inputs, outputs, scalars in rows:
            cells = [
                "<br />".join(self.stack_text(s) for s in inputs) or "—",
                "<br />".join(self.stack_text(s) for s in outputs) or "—",
            ] if with_items else []
            cells += [_format_cell(k, scalars[k]) if k in scalars else "" for k in keys]
            lines.append("| " + " | ".join(c.replace("|", "\\|") for c in cells) + " |")
        return "\n".join(lines)

    def t_data_values(self, *ids: str) -> str:
        return _DROP

    def t_chain(self, *steps, edges: list | None = None, caption: str | None = None) -> str:
        chain = self.chain_model(steps, edges)
        layout = chains.layout(chain)
        layers: dict[int, list[chains.Node]] = {}
        for node in sorted(chain.nodes, key=lambda n: layout.cells[n.key]):
            layers.setdefault(layout.cells[node.key][0], []).append(node)
        # GuideME has no arrows to draw, so the diagram is the layers top to bottom and the table names each flow.
        rows = []
        for _, nodes in sorted(layers.items()):
            cells = "\n".join(self._chain_node(n) for n in nodes)
            rows.append(f'<Row gap="16" alignItems="end">\n{cells}\n</Row>')
        lines = ['<Column alignItems="center" fullWidth={true}>', "\n\n↓\n\n".join(rows), "</Column>", ""]
        lines += ["| From | Flow | To |", "|---|---|---|"]
        for edge in chain.edges:
            amount, rate = flow_label(edge)
            flow = " · ".join(t for t in (self.res.name(edge.resource), amount, rate) if t)
            source = "*you supply*" if edge.source.external else self._chain_link(edge.source)
            target = "*leaves the chain*" if edge.target.external else self._chain_link(edge.target)
            if edge.loop:
                target = f"↺ back to {target}"
            lines.append(f"| {source} | {flow} | {target} |")
        if caption:
            lines += ["", self._expand(caption)]
        return "\n".join(lines) + "\n"

    def _chain_link(self, node: chains.Node) -> str:
        machine = chains.machine_id(node.key)
        return self.item_text(machine, self._chain_name(machine))

    def _chain_node(self, node: chains.Node) -> str:
        if node.external:
            icon = self._fluid_icon(node.resource) if self._is_fluid(node.resource) else node.resource
            label = self.res.name(node.resource)
        else:
            icon = chains.machine_id(node.key)
            label = self._chain_name(icon)
        image = f'  <ItemImage id="{icon}" scale="2" />\n' if self._known_item(icon) else ""
        return f'<Column alignItems="center" gap="2">\n{image}  {label}\n</Column>'

    def t_navbox(self) -> str:
        return ""

    def t_gallery(self) -> str:
        return "<SubPages />"


def pages_path() -> str:
    """Where GuideME reads the guide's pages, relative to a resource root."""
    namespace, path = split_id(GUIDE_ID)
    return f"assets/{namespace}/guides/{namespace}/{path}"


def _differences(expected: Path, actual: Path) -> list[str]:
    def files(root: Path) -> set[str]:
        return {p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()} if root.exists() else set()

    wanted, present = files(expected), files(actual)
    stale = sorted(wanted ^ present)
    stale += sorted(f for f in wanted & present if not filecmp.cmp(expected / f, actual / f, shallow=False))
    return stale


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="fail if the committed guide is out of date")
    args = parser.parse_args()
    # Vanilla names and tags come from the client jar, so the output would differ without it.
    if find_client_jar() is None:
        sys.exit("guideme: no Minecraft client jar found; run `python tools/rngwiki/vanilla.py --fetch` first.")
    wiki_dir = REPO / "wiki"
    names = sorted(p.stem for p in wiki_dir.glob("*.md") if p.stem not in EXCLUDED_PAGES)
    exporter = GuideExporter(wiki_dir, names)
    shown_root = GUIDE_ROOT.relative_to(REPO).as_posix()
    if not args.check:
        written = exporter.export(GUIDE_ROOT)
        print(f"Wrote {len(written) - 1} guide pages to {shown_root}")
        return
    with tempfile.TemporaryDirectory() as temp:
        exporter.export(Path(temp))
        stale = _differences(Path(temp), GUIDE_ROOT)
    if stale:
        print("The in-game guide is out of date. Run `python tools/rngwiki/guideme.py` and commit the result:")
        print("\n".join(f"  {shown_root}/{f}" for f in stale[:20]))
        if len(stale) > 20:
            print(f"  ... and {len(stale) - 20} more")
        sys.exit(1)
    print("The in-game guide is up to date.")


if __name__ == "__main__":
    main()
