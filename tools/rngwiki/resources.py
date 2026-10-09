"""Read-only index over RNGTech resources and the vanilla client jar."""

from __future__ import annotations

import io
import json
import zipfile
from functools import cached_property, lru_cache
from pathlib import Path

from PIL import Image

MOD_ID = "rngtech"

# Common-tag fallbacks for tags only NeoForge defines, e.g. c:ingots/iron -> minecraft:iron_ingot.
_COMMON_FORMS = {
    "ingots": "{m}_ingot",
    "nuggets": "{m}_nugget",
    "dusts": "{m}_dust",
    "gems": "{m}",
    "ores": "{m}_ore",
    "raw_materials": "raw_{m}",
    "storage_blocks": "{m}_block",
    "plates": "{m}_plate",
    "rods": "{m}_rod",
    "gears": "{m}_gear",
}


class _DirSource:
    def __init__(self, root: Path):
        self.root = root

    def read(self, path: str) -> bytes | None:
        file = self.root / path
        return file.read_bytes() if file.is_file() else None

    def list(self, prefix: str) -> list[str]:
        base = self.root / prefix
        if not base.is_dir():
            return []
        return [p.relative_to(self.root).as_posix() for p in base.rglob("*") if p.is_file()]


class _ZipSource:
    def __init__(self, path: Path):
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())

    def read(self, path: str) -> bytes | None:
        return self.zip.read(path) if path in self.names else None

    def list(self, prefix: str) -> list[str]:
        return [n for n in self.names if n.startswith(prefix) and not n.endswith("/")]


def split_id(resource_id: str) -> tuple[str, str]:
    namespace, _, path = resource_id.rpartition(":")
    return (namespace or "minecraft"), path


class Resources:
    def __init__(self, mod_roots: list[Path], vanilla_jar: Path | None):
        self.mod_sources = [_DirSource(root) for root in mod_roots if root.is_dir()]
        self.vanilla = _ZipSource(vanilla_jar) if vanilla_jar else None
        self.sources = self.mod_sources + ([self.vanilla] if self.vanilla else [])

    def read(self, path: str) -> bytes | None:
        for source in self.sources:
            data = source.read(path)
            if data is not None:
                return data
        return None

    @lru_cache(maxsize=None)
    def json(self, path: str) -> dict | None:
        data = self.read(path)
        return json.loads(data) if data is not None else None

    # Names

    @cached_property
    def lang(self) -> dict[str, str]:
        merged: dict[str, str] = {}
        for source in reversed(self.sources):
            for namespace in ("minecraft", MOD_ID):
                data = source.read(f"assets/{namespace}/lang/en_us.json")
                if data:
                    merged.update(json.loads(data))
        return merged

    def name(self, resource_id: str) -> str:
        namespace, path = split_id(resource_id)
        for kind in ("block", "item", "fluid"):
            key = f"{kind}.{namespace}.{path}"
            if key in self.lang:
                return self.lang[key]
        return path.replace("_", " ").replace("/", " ").title()

    def translation_key(self, resource_id: str) -> str | None:
        namespace, path = split_id(resource_id)
        for kind in ("block", "item", "fluid"):
            key = f"{kind}.{namespace}.{path}"
            if key in self.lang:
                return key
        return None

    def is_block(self, resource_id: str) -> bool:
        namespace, path = split_id(resource_id)
        return self.json(f"assets/{namespace}/blockstates/{path}.json") is not None

    def has_item(self, resource_id: str) -> bool:
        namespace, path = split_id(resource_id)
        return self.json(f"assets/{namespace}/models/item/{path}.json") is not None

    # Models and textures

    def model(self, model_id: str) -> dict | None:
        namespace, path = split_id(model_id)
        if path.startswith("builtin/"):
            return None
        return self.json(f"assets/{namespace}/models/{path}.json")

    @lru_cache(maxsize=None)
    def texture(self, texture_id: str) -> Image.Image | None:
        namespace, path = split_id(texture_id)
        data = self.read(f"assets/{namespace}/textures/{path}.png")
        if data is None:
            return None
        image = Image.open(io.BytesIO(data)).convert("RGBA")
        if image.height > image.width and image.height % image.width == 0:
            image = image.crop((0, 0, image.width, image.width))
        return image

    # Tags

    @cached_property
    def _raw_item_tags(self) -> dict[str, list]:
        tags: dict[str, list] = {}
        for source in reversed(self.sources):
            for path in source.list("data/"):
                parts = path.split("/")
                if len(parts) < 5 or parts[2] != "tags" or parts[3] != "item" or not path.endswith(".json"):
                    continue
                tag_id = f"{parts[1]}:{'/'.join(parts[4:])[:-5]}"
                data = json.loads(source.read(path))
                values = [v["id"] if isinstance(v, dict) else v for v in data.get("values", [])]
                if data.get("replace"):
                    tags[tag_id] = values
                else:
                    tags.setdefault(tag_id, []).extend(values)
        return tags

    @lru_cache(maxsize=None)
    def tag_items(self, tag_id: str) -> tuple[str, ...]:
        items: list[str] = []
        seen: set[str] = set()

        def visit(tag: str, depth: int) -> None:
            for value in self._raw_item_tags.get(tag, []):
                if value.startswith("#"):
                    if depth < 8:
                        visit(value[1:], depth + 1)
                elif value not in seen and self.has_item(value):
                    seen.add(value)
                    items.append(value)

        visit(tag_id, 0)
        if not items:
            items.extend(self._guess_common_tag(tag_id))
        return tuple(items)

    def _guess_common_tag(self, tag_id: str) -> list[str]:
        namespace, path = split_id(tag_id)
        form, _, material = path.partition("/")
        pattern = _COMMON_FORMS.get(form)
        if namespace != "c" or not pattern or not material or "/" in material:
            return []
        candidate = pattern.format(m=material)
        return [f"{ns}:{candidate}" for ns in ("minecraft", MOD_ID) if self.has_item(f"{ns}:{candidate}")]

    def block_tag_values(self, tag_id: str) -> set[str]:
        namespace, path = split_id(tag_id)
        values: set[str] = set()
        for source in self.sources:
            data = source.read(f"data/{namespace}/tags/block/{path}.json")
            if data:
                values.update(v["id"] if isinstance(v, dict) else v for v in json.loads(data).get("values", []))
        return values

    # Recipes

    @cached_property
    def recipes(self) -> dict[str, dict]:
        recipes: dict[str, dict] = {}
        for source in reversed(self.mod_sources):
            for path in source.list(f"data/{MOD_ID}/recipe/"):
                if path.endswith(".json"):
                    recipe_id = f"{MOD_ID}:{path[len(f'data/{MOD_ID}/recipe/'):-5]}"
                    recipes[recipe_id] = json.loads(source.read(path))
        return dict(sorted(recipes.items()))
