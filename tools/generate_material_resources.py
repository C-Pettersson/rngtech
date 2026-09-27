#!/usr/bin/env python3
"""Generate material models, tags, mapped recipe references, and coverage docs."""

from __future__ import annotations

import json
from collections import OrderedDict, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "src/main/resources/data/rngtech/materials/catalog.json"
ORE_CATALOG_PATH = ROOT / "src/main/resources/data/rngtech/materials/ore_catalog.json"
MODEL_ROOT = ROOT / "src/main/resources/assets/rngtech/models/item"
BLOCK_MODEL_ROOT = ROOT / "src/main/resources/assets/rngtech/models/block"
BLOCKSTATE_ROOT = ROOT / "src/main/resources/assets/rngtech/blockstates"
TEXTURE_ROOT = ROOT / "src/main/resources/assets/rngtech/textures/item"
LANG_PATH = ROOT / "src/main/resources/assets/rngtech/lang/en_us.json"
DATA_ROOT = ROOT / "src/main/resources/data"
COVERAGE_PATH = ROOT / "docs/reference/material-catalog-coverage.md"

COMMON_TAG_FORMS = {
    "ingot": "ingots",
    "dust": "dusts",
    "nugget": "nuggets",
    "storage_block": "storage_blocks",
    "ore": "ores",
    "raw": "raw_materials",
}

NUGGET_CONVERSION_MATERIALS = {"copper", "naquadah", "netherite"}
VANILLA_ORE_HARDNESS = {
    "minecraft:iron_ore": 1,
    "minecraft:deepslate_iron_ore": 1,
    "minecraft:copper_ore": 1,
    "minecraft:deepslate_copper_ore": 1,
    "minecraft:gold_ore": 2,
    "minecraft:deepslate_gold_ore": 2,
}
VANILLA_ORE_BURST_VALUES = [
    "minecraft:coal_ore",
    "minecraft:deepslate_coal_ore",
    "minecraft:copper_ore",
    "minecraft:deepslate_copper_ore",
    "minecraft:iron_ore",
    "minecraft:deepslate_iron_ore",
    "minecraft:gold_ore",
    "minecraft:deepslate_gold_ore",
    "minecraft:redstone_ore",
    "minecraft:deepslate_redstone_ore",
    "minecraft:lapis_ore",
    "minecraft:deepslate_lapis_ore",
    "minecraft:diamond_ore",
    "minecraft:deepslate_diamond_ore",
    "minecraft:emerald_ore",
    "minecraft:deepslate_emerald_ore",
    "minecraft:nether_gold_ore",
    "minecraft:nether_quartz_ore",
]
SOURCE_ORE_MATERIALS = {
    "tin",
    "zinc",
    "nickel",
    "lead",
    "silver",
    "aluminum",
    "osmium",
    "titanium",
    "tungsten",
    "platinum",
    "naquadah",
}
PICKAXE_MINING_TAG = DATA_ROOT / "minecraft/tags/block/mineable/pickaxe.json"
DEFAULT_ORE_HOSTS = ["stone", "deepslate"]
KNOWN_ORE_HOSTS = ["stone", "deepslate", "nether", "end"]
CIRCUIT_FORMS = {
    "basic_electric_circuit",
    "advanced_electric_circuit",
    "elite_electric_circuit",
    "ultimate_electric_circuit",
}

GENERIC_TEXTURE_ALIASES = {
    "basic_circuit_blank": "basic_electric_circuit",
    "advanced_circuit_blank": "advanced_electric_circuit",
    "elite_circuit_blank": "elite_electric_circuit",
    "ultimate_circuit_blank": "ultimate_electric_circuit",
    "broken_circuit": "scrap",
    "reinforced_machine_frame": "machine_frame",
}


def load_catalog() -> dict:
    with CATALOG_PATH.open() as handle:
        return json.load(handle, object_pairs_hook=OrderedDict)


def load_ore_catalog() -> dict:
    with ORE_CATALOG_PATH.open() as handle:
        return json.load(handle, object_pairs_hook=OrderedDict)


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(data, indent=2) + "\n")


def remove_file(path: Path) -> None:
    if path.exists():
        path.unlink()


def form_excluded(material: dict, form_id: str) -> bool:
    return form_id in material.get("excluded_forms", [])


def excluded_materials(catalog: dict, form_id: str) -> set[str]:
    return {
        material["id"]
        for material in catalog["materials"]
        if form_excluded(material, form_id)
    }


def item_id(form: dict, material: dict) -> str:
    return form["item_pattern"].replace("{material}", material["id"])


def display_name(form: dict, material: dict) -> str:
    return form["display_pattern"].replace("{material}", material["display"])


def vanilla_form_items(catalog: dict, material_id: str, form_id: str) -> list[str]:
    return list(catalog.get("vanilla_form_mappings", {}).get(material_id, {}).get(form_id, []))


def has_vanilla_form_mapping(catalog: dict, material_id: str, form_id: str) -> bool:
    return bool(vanilla_form_items(catalog, material_id, form_id))


def primary_material_item_id(catalog: dict, form: dict, material: dict) -> str:
    mapped = vanilla_form_items(catalog, material["id"], form["id"])
    if mapped:
        return mapped[0]
    return f"rngtech:{item_id(form, material)}"


def generic_form_texture(form_id: str) -> str:
    texture_id = GENERIC_TEXTURE_ALIASES.get(form_id, form_id)
    return f"rngtech:item/materials/forms/{texture_id}"


def material_items(catalog: dict) -> list[dict]:
    items: list[dict] = []
    for material in catalog["materials"]:
        for form in catalog["metal_forms"]:
            if form_excluded(material, form["id"]):
                continue
            if has_vanilla_form_mapping(catalog, material["id"], form["id"]):
                continue
            items.append({
                "item_id": item_id(form, material),
                "display": display_name(form, material),
                "texture": f"rngtech:item/materials/{form['id']}/{material['id']}",
                "material": material["id"],
                "form": form["id"],
                "kind": "metal_form",
                "stage": material["stage"],
            })
    for form in catalog["generic_forms"]:
        items.append({
            "item_id": form["id"],
            "display": form["display"],
            "texture": generic_form_texture(form["id"]),
            "material": None,
            "form": form["id"],
            "kind": "generic_form",
            "stage": None,
        })
    for material in catalog["non_stage_materials"]:
        if material["handling"] == "register":
            items.append({
                "item_id": material["id"],
                "display": material["display"],
                "texture": f"rngtech:item/materials/non_stage/{material['id']}",
                "material": None,
                "form": "non_stage",
                "kind": "non_stage",
                "stage": None,
            })
    return items


def texture_path(texture: str) -> Path:
    namespace, path = texture.split(":", 1)
    if namespace != "rngtech":
        raise ValueError(f"Unexpected texture namespace: {texture}")
    if not path.startswith("item/"):
        raise ValueError(f"Unexpected texture path: {texture}")
    return TEXTURE_ROOT / (path.removeprefix("item/") + ".png")


def validate_textures(items: list[dict]) -> None:
    missing = [str(texture_path(item["texture"])) for item in items if not texture_path(item["texture"]).is_file()]
    if missing:
        formatted = "\n".join(missing)
        raise SystemExit(f"Missing material textures:\n{formatted}")


def ore_block_item_ids(ore_catalog: dict) -> set[str]:
    ids: set[str] = set()
    for ore in ore_catalog["ores"]:
        for host in ore_hosts(ore):
            ids.add(ore_block_id(ore, host))
    return ids


def generate_models(items: list[dict], ore_catalog: dict) -> None:
    ore_items = ore_block_item_ids(ore_catalog)
    for item in items:
        if item["item_id"] in ore_items:
            continue
        write_json(MODEL_ROOT / f"{item['item_id']}.json", {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": item["texture"],
            },
        })


def remove_vanilla_mapped_models(catalog: dict) -> None:
    forms_by_id = {form["id"]: form for form in catalog["metal_forms"]}
    materials_by_id = {material["id"]: material for material in catalog["materials"]}
    for material_id, form_mappings in catalog.get("vanilla_form_mappings", {}).items():
        material = materials_by_id[material_id]
        for form_id in form_mappings:
            remove_file(MODEL_ROOT / f"{item_id(forms_by_id[form_id], material)}.json")


def generate_lang(items: list[dict]) -> None:
    with LANG_PATH.open() as handle:
        lang = json.load(handle, object_pairs_hook=OrderedDict)
    changed = False
    for item in items:
        key = f"item.rngtech.{item['item_id']}"
        if key not in lang:
            lang[key] = item["display"]
            changed = True
    if "rngtech.tooltip.material.disabled" not in lang:
        lang["rngtech.tooltip.material.disabled"] = "Disabled by pack config"
        changed = True
    tooltip_entries = {
        "rngtech.tooltip.ore.material": "Material: %s",
        "rngtech.tooltip.ore.hardness": "Ore Hardness: %s",
        "rngtech.tooltip.ore.drops": "Drops: Raw %s",
        "rngtech.tooltip.modular_tool.ore_hardness_reach": "Ore Hardness Reach: %s",
    }
    for key, value in tooltip_entries.items():
        if key not in lang:
            lang[key] = value
            changed = True
    if changed:
        write_json(LANG_PATH, lang)


def remove_lang_entries(keys: list[str]) -> None:
    with LANG_PATH.open(encoding="utf-8") as handle:
        lang = json.load(handle, object_pairs_hook=OrderedDict)
    changed = False
    for key in keys:
        if key in lang:
            del lang[key]
            changed = True
    if changed:
        write_json(LANG_PATH, lang)


def cleanup_excluded_form_resources(catalog: dict) -> None:
    forms_by_id = {form["id"]: form for form in catalog["metal_forms"]}
    lang_keys: list[str] = []
    for material in catalog["materials"]:
        material_id = material["id"]
        for form_id in material.get("excluded_forms", []):
            if form_id not in forms_by_id:
                continue
            excluded_item_id = item_id(forms_by_id[form_id], material)
            remove_file(MODEL_ROOT / f"{excluded_item_id}.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/materials/{material_id}_{form_id}.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/metal_press/{material_id}_{form_id}.json")
            lang_keys.append(f"item.rngtech.{excluded_item_id}")
        if form_excluded(material, "ore"):
            remove_file(DATA_ROOT / f"c/tags/item/ores/{material_id}.json")
            remove_file(DATA_ROOT / f"c/tags/block/ores/{material_id}.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_ore.json")
            remove_file(DATA_ROOT / f"rngtech/worldgen/configured_feature/ore_{material_id}.json")
            remove_file(DATA_ROOT / f"rngtech/worldgen/placed_feature/ore_{material_id}.json")
            remove_file(DATA_ROOT / f"rngtech/neoforge/biome_modifier/ore_{material_id}.json")
            lang_keys.append(f"item.rngtech.{item_id(forms_by_id['ore'], material)}")
            for host in KNOWN_ORE_HOSTS:
                block_id = ore_block_id_for_material(material_id, host)
                remove_file(BLOCKSTATE_ROOT / f"{block_id}.json")
                remove_file(BLOCK_MODEL_ROOT / f"{block_id}.json")
                remove_file(MODEL_ROOT / f"{block_id}.json")
                remove_file(DATA_ROOT / f"rngtech/loot_table/blocks/{block_id}.json")
                remove_file(DATA_ROOT / f"rngtech/recipe/crusher/{block_id}.json")
                lang_keys.append(f"block.rngtech.{block_id}")
        if form_excluded(material, "raw"):
            raw_id = item_id(forms_by_id["raw"], material)
            remove_file(MODEL_ROOT / f"{raw_id}.json")
            remove_file(DATA_ROOT / f"c/tags/item/raw_materials/{material_id}.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_raw.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/crusher/raw_{material_id}.json")
            lang_keys.append(f"item.rngtech.{raw_id}")
    remove_lang_entries(lang_keys)


def tag(path: str, values: list[str], replace: bool = False) -> None:
    write_json(DATA_ROOT / path, {
        "replace": replace,
        "values": values,
    })


def append_tag_values(path: Path, values: list[str]) -> None:
    existing: dict = {"replace": False, "values": []}
    if path.exists():
        with path.open(encoding="utf-8") as handle:
            existing = json.load(handle, object_pairs_hook=OrderedDict)
    merged: list = []
    seen: set[str] = set()
    for value in existing.get("values", []):
        key = json.dumps(value, sort_keys=True) if isinstance(value, dict) else str(value)
        if key not in seen:
            merged.append(value)
            seen.add(key)
    for value in values:
        if value not in seen:
            merged.append(value)
            seen.add(value)
    write_json(path, {"replace": existing.get("replace", False), "values": merged})


def replace_managed_tag_values(path: Path, values: list[str], managed_values: set[str]) -> None:
    existing: dict = {"replace": False, "values": []}
    if path.exists():
        with path.open(encoding="utf-8") as handle:
            existing = json.load(handle, object_pairs_hook=OrderedDict)
    merged: list = []
    seen: set[str] = set()
    for value in existing.get("values", []):
        if value in managed_values:
            continue
        key = json.dumps(value, sort_keys=True) if isinstance(value, dict) else str(value)
        if key not in seen:
            merged.append(value)
            seen.add(key)
    for value in values:
        if value not in seen:
            merged.append(value)
            seen.add(value)
    write_json(path, {"replace": existing.get("replace", False), "values": merged})


def managed_ore_block_values(catalog: dict) -> set[str]:
    return {
        f"rngtech:{ore_block_id_for_material(material['id'], host)}"
        for material in catalog["materials"]
        for host in KNOWN_ORE_HOSTS
    }


def ore_block_id(ore: dict, host: str) -> str:
    material = ore["material"]
    if host == "stone":
        return f"{material}_ore"
    if host == "deepslate":
        return f"deepslate_{material}_ore"
    if host == "nether":
        return f"nether_{material}_ore"
    if host == "end":
        return f"end_{material}_ore"
    raise ValueError(f"Unknown ore host: {host}")


def ore_block_id_for_material(material_id: str, host: str) -> str:
    return ore_block_id({"material": material_id}, host)


def ore_hosts(ore: dict) -> list[str]:
    return list(ore.get("hosts", DEFAULT_ORE_HOSTS))


def ore_generation_hosts(ore: dict) -> list[str]:
    return list(ore.get("generation_hosts", ore_hosts(ore)))


def ore_item_values(ore_catalog: dict, material_id: str) -> list[str]:
    for ore in ore_catalog["ores"]:
        if ore["material"] == material_id:
            return [f"rngtech:{ore_block_id(ore, host)}" for host in ore_hosts(ore)]
    return []


def generate_common_tags(catalog: dict, ore_catalog: dict) -> None:
    forms_by_id = {form["id"]: form for form in catalog["metal_forms"]}
    ore_materials = {ore["material"] for ore in ore_catalog["ores"]}
    item_ore_tags: list[str] = []
    block_ore_tags: list[str] = []
    raw_material_tags: list[str] = []
    for material in catalog["materials"]:
        material_id = material["id"]
        for form_id, tag_form in COMMON_TAG_FORMS.items():
            if form_excluded(material, form_id):
                remove_file(DATA_ROOT / f"c/tags/item/{tag_form}/{material_id}.json")
                if form_id == "ore":
                    remove_file(DATA_ROOT / f"c/tags/block/ores/{material_id}.json")
                continue
            form = forms_by_id[form_id]
            values = vanilla_form_items(catalog, material_id, form_id)
            if form_id == "ore" and material_id in ore_materials:
                values = ore_item_values(ore_catalog, material_id)
            elif not values:
                values = [f"rngtech:{item_id(form, material)}"]
            tag(f"c/tags/item/{tag_form}/{material_id}.json", values)
            if form_id == "ore" and values:
                item_ore_tags.append(f"#c:ores/{material_id}")
                if material_id in ore_materials:
                    tag(f"c/tags/block/ores/{material_id}.json", values)
                    block_ore_tags.append(f"#c:ores/{material_id}")
            elif form_id == "raw" and values:
                raw_material_tags.append(f"#c:raw_materials/{material_id}")
    tag("c/tags/item/ores.json", item_ore_tags)
    tag("c/tags/block/ores.json", block_ore_tags)
    tag("c/tags/item/raw_materials.json", raw_material_tags)


def generate_rngtech_tags(catalog: dict, ore_catalog: dict, items: list[dict]) -> None:
    by_stage: dict[int, list[str]] = defaultdict(list)
    by_form: dict[str, list[str]] = defaultdict(list)
    by_family: dict[str, list[str]] = defaultdict(list)
    ore_materials = {ore["material"] for ore in ore_catalog["ores"]}

    registered_metal_items = {
        (item["material"], item["form"]): f"rngtech:{item['item_id']}"
        for item in items
        if item["kind"] == "metal_form"
    }
    forms_by_id = {form["id"]: form for form in catalog["metal_forms"]}
    for material in catalog["materials"]:
        material_id = material["id"]
        for form_id in forms_by_id:
            if form_excluded(material, form_id):
                continue
            if form_id == "ore" and material_id in ore_materials:
                values = ore_item_values(ore_catalog, material_id)
            else:
                values = vanilla_form_items(catalog, material_id, form_id)
            if not values:
                value = registered_metal_items.get((material_id, form_id))
                values = [value] if value else []
            for value in values:
                by_form[form_id].append(value)
                by_family[material_id].append(value)
                by_stage[material["stage"]].append(value)

    for item in items:
        if item["kind"] == "metal_form":
            continue
        value = f"rngtech:{item['item_id']}"
        by_form[item["form"]].append(value)
    machine_frame_tiers = {
        "base": ["rngtech:machine_frame"],
        "reinforced": ["rngtech:reinforced_machine_frame"],
        "advanced": ["rngtech:advanced_machine_frame"],
        "exotic": ["rngtech:exotic_machine_frame"],
    }
    for values in machine_frame_tiers.values():
        for value in values:
            if value not in by_form["machine_frame"]:
                by_form["machine_frame"].append(value)
    for stage, values in sorted(by_stage.items()):
        tag(f"rngtech/tags/item/materials/stage_{stage}.json", values)
    for form, values in sorted(by_form.items()):
        tag(f"rngtech/tags/item/materials/form/{form}.json", values)
    for family, values in sorted(by_family.items()):
        tag(f"rngtech/tags/item/materials/family/{family}.json", values)
    for group in catalog.get("material_groups", []):
        group_values: list[str] = []
        for family in group["families"]:
            if family not in by_family:
                raise SystemExit(f"Unknown material family in group {group['id']}: {family}")
            group_values.extend(by_family[family])
        tag(f"rngtech/tags/item/materials/family/{group['id']}.json", group_values)
    for tier, values in machine_frame_tiers.items():
        tag(f"rngtech/tags/item/machine_frames/{tier}.json", values)
    tag("rngtech/tags/item/machine_frames/all.json", [
        "#rngtech:machine_frames/base",
        "#rngtech:machine_frames/reinforced",
        "#rngtech:machine_frames/advanced",
        "#rngtech:machine_frames/exotic",
    ])


def generate_circuit_tags(catalog: dict) -> None:
    generic_form_ids = {form["id"] for form in catalog["generic_forms"]}
    if not CIRCUIT_FORMS.issubset(generic_form_ids):
        return
    tag("c/tags/item/circuits.json", [
        "#c:circuits/basic",
        "#c:circuits/advanced",
        "#c:circuits/elite",
        "#c:circuits/ultimate",
    ])
    for tier in ("basic", "advanced", "elite", "ultimate"):
        tag(f"c/tags/item/circuits/{tier}.json", [f"rngtech:{tier}_electric_circuit"])


def generate_recipes(catalog: dict, ore_catalog: dict) -> None:
    forms = {form["id"]: form for form in catalog["metal_forms"]}
    for material in catalog["materials"]:
        material_id = material["id"]
        if form_excluded(material, "raw"):
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_raw.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/crusher/raw_{material_id}.json")
        if form_excluded(material, "ore"):
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_ore.json")
        if material_id not in NUGGET_CONVERSION_MATERIALS:
            remove_file(DATA_ROOT / f"rngtech/recipe/materials/{material_id}_nugget_from_ingot.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/materials/{material_id}_ingot_from_nuggets.json")
        else:
            rewrite_recipe_result(
                DATA_ROOT / f"rngtech/recipe/materials/{material_id}_ingot_from_nuggets.json",
                f"rngtech:{item_id(forms['ingot'], material)}",
                primary_material_item_id(catalog, forms["ingot"], material),
            )

        mapped_ingot = vanilla_form_items(catalog, material_id, "ingot")
        if has_vanilla_form_mapping(catalog, material_id, "raw"):
            raw_tag = f"c:raw_materials/{material_id}"
            rewrite_recipe_ingredient_to_tag(DATA_ROOT / f"rngtech/recipe/crusher/raw_{material_id}.json", raw_tag)
            rewrite_recipe_ingredient_to_tag(
                DATA_ROOT / f"rngtech/recipe/furnace/vanilla/{material_id}_ingot_from_smelting_raw_{material_id}.json",
                raw_tag,
            )
        if mapped_ingot:
            rewrite_recipe_result(
                DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_dust.json",
                f"rngtech:{item_id(forms['ingot'], material)}",
                mapped_ingot[0],
            )
            rewrite_recipe_result(
                DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_crushed.json",
                f"rngtech:{item_id(forms['ingot'], material)}",
                mapped_ingot[0],
            )
        if form_excluded(material, "ore"):
            continue
        if has_vanilla_form_mapping(catalog, material_id, "ore"):
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_ore.json")
        elif material_id in {ore["material"] for ore in ore_catalog["ores"]}:
            if material_id in SOURCE_ORE_MATERIALS:
                generate_ore_furnace_recipe(catalog, material)
            else:
                remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_ore.json")
                remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_raw.json")

    generate_raw_material_recipes(catalog, ore_catalog)
    generate_crusher_ore_recipes(catalog, ore_catalog)


def rewrite_recipe_ingredient_to_tag(path: Path, tag_id: str) -> None:
    if not path.exists():
        return
    with path.open(encoding="utf-8") as handle:
        recipe = json.load(handle, object_pairs_hook=OrderedDict)
    recipe["ingredient"] = OrderedDict([("tag", tag_id)])
    write_json(path, recipe)


def generate_ore_furnace_recipe(catalog: dict, material: dict) -> None:
    forms = {form["id"]: form for form in catalog["metal_forms"]}
    material_id = material["id"]
    path = DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_ore.json"
    if path.exists():
        with path.open(encoding="utf-8") as handle:
            recipe = json.load(handle, object_pairs_hook=OrderedDict)
        recipe["ingredient"] = OrderedDict([("tag", f"c:ores/{material_id}")])
        recipe["result"] = OrderedDict([("id", primary_material_item_id(catalog, forms["ingot"], material))])
        write_json(path, recipe)
        return
    write_json(path, OrderedDict([
        ("neoforge:conditions", [
            OrderedDict([
                ("type", "rngtech:material_enabled"),
                ("material", material_id),
            ]),
        ]),
        ("type", "rngtech:furnace"),
        ("group", "rngtech_metals"),
        ("ingredient", OrderedDict([("tag", f"c:ores/{material_id}")])),
        ("result", OrderedDict([("id", primary_material_item_id(catalog, forms["ingot"], material))])),
        ("processing_ticks", 300),
        ("energy", 7200),
        ("experience", 0.7),
        ("minimum_temperature", 1000),
    ]))


def generate_crusher_ore_recipes(catalog: dict, ore_catalog: dict) -> None:
    forms = {form["id"]: form for form in catalog["metal_forms"]}
    for ore in ore_catalog["ores"]:
        material_id = ore["material"]
        if material_id not in SOURCE_ORE_MATERIALS:
            continue
        material = next(material for material in catalog["materials"] if material["id"] == material_id)
        crushed_id = primary_material_item_id(catalog, forms["crushed"], material)
        for host in ore_hosts(ore):
            block_id = ore_block_id(ore, host)
            ticks = 200 if host == "deepslate" else 180 if host in {"nether", "end"} else 160
            energy = 9600 if host == "deepslate" else 8640 if host in {"nether", "end"} else 7680
            write_json(DATA_ROOT / f"rngtech/recipe/crusher/{block_id}.json", OrderedDict([
                ("neoforge:conditions", [
                    OrderedDict([
                        ("type", "rngtech:material_enabled"),
                        ("material", material_id),
                    ]),
                ]),
                ("type", "rngtech:crusher"),
                ("ingredient", OrderedDict([("item", f"rngtech:{block_id}")])),
                ("result", OrderedDict([
                    ("count", 3),
                    ("id", crushed_id),
                ])),
                ("processing_ticks", ticks),
                ("energy", energy),
                ("required_processing_level", max(1, min(8, ore["hardness_level"]))),
            ]))


def generate_raw_material_recipes(catalog: dict, ore_catalog: dict) -> None:
    forms = {form["id"]: form for form in catalog["metal_forms"]}
    materials = {material["id"]: material for material in catalog["materials"]}
    ores_by_material = {ore["material"]: ore for ore in ore_catalog["ores"]}
    for material_id, ore in ores_by_material.items():
        if material_id not in SOURCE_ORE_MATERIALS:
            remove_file(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_raw.json")
            remove_file(DATA_ROOT / f"rngtech/recipe/crusher/raw_{material_id}.json")
            continue
        material = materials[material_id]
        raw_tag = f"c:raw_materials/{material_id}"
        write_json(DATA_ROOT / f"rngtech/recipe/furnace/metals/{material_id}_from_raw.json", OrderedDict([
            ("neoforge:conditions", [
                OrderedDict([
                    ("type", "rngtech:material_enabled"),
                    ("material", material_id),
                ]),
            ]),
            ("type", "rngtech:furnace"),
            ("group", "rngtech_metals"),
            ("ingredient", OrderedDict([("tag", raw_tag)])),
            ("result", OrderedDict([("id", primary_material_item_id(catalog, forms["ingot"], material))])),
            ("processing_ticks", 200),
            ("energy", 4800),
            ("experience", 0.7),
            ("minimum_temperature", 1000),
        ]))
        write_json(DATA_ROOT / f"rngtech/recipe/crusher/raw_{material_id}.json", OrderedDict([
            ("neoforge:conditions", [
                OrderedDict([
                    ("type", "rngtech:material_enabled"),
                    ("material", material_id),
                ]),
            ]),
            ("type", "rngtech:crusher"),
            ("ingredient", OrderedDict([("tag", raw_tag)])),
            ("result", OrderedDict([
                ("count", 2),
                ("id", primary_material_item_id(catalog, forms["crushed"], material)),
            ])),
            ("processing_ticks", 120),
            ("energy", 5760),
            ("required_processing_level", 1),
        ]))


def rewrite_recipe_result(path: Path, old_id: str, new_id: str) -> None:
    if not path.exists():
        return
    old_text = f'"id": "{old_id}"'
    new_text = f'"id": "{new_id}"'
    content = path.read_text(encoding="utf-8")
    updated = content.replace(old_text, new_text)
    if updated != content:
        path.write_text(updated, encoding="utf-8", newline="\n")


def generate_ore_resources(catalog: dict, ore_catalog: dict) -> None:
    materials = {material["id"]: material for material in catalog["materials"]}
    generated_values: list[str] = []
    disabled_values: list[str] = []
    rngtech_ore_values: list[str] = []
    hardness_values: dict[int, list[str]] = defaultdict(list)
    pickaxe_values: list[str] = []

    for ore in ore_catalog["ores"]:
        material = materials[ore["material"]]
        generation_hosts = set(ore_generation_hosts(ore))
        for host in ore_hosts(ore):
            block_id = ore_block_id(ore, host)
            block_value = f"rngtech:{block_id}"
            rngtech_ore_values.append(block_value)
            pickaxe_values.append(block_value)
            hardness_values[ore["hardness_level"]].append(block_value)
            if ore["generated_by_default"] and ore["veins_per_chunk"] > 0 and host in generation_hosts:
                generated_values.append(block_value)
            else:
                disabled_values.append(block_value)
            generate_ore_block_assets(ore, material, host)
            generate_ore_loot(block_id, f"rngtech:raw_{ore['material']}")

    for vanilla_ore, hardness in VANILLA_ORE_HARDNESS.items():
        hardness_values[hardness].append(vanilla_ore)

    tag("rngtech/tags/block/ores.json", rngtech_ore_values)
    tag("rngtech/tags/item/ores.json", rngtech_ore_values)
    tag("rngtech/tags/block/generated_ores.json", generated_values)
    tag("rngtech/tags/item/generated_ores.json", generated_values)
    tag("rngtech/tags/block/worldgen_disabled_ores.json", disabled_values)
    tag("rngtech/tags/item/worldgen_disabled_ores.json", disabled_values)
    tag("rngtech/tags/block/ore_burst_targets.json", [{"id": "#c:ores", "required": False}, *VANILLA_ORE_BURST_VALUES])
    for hardness, values in sorted(hardness_values.items()):
        tag(f"rngtech/tags/block/ore_hardness/level_{hardness}.json", values)
    replace_managed_tag_values(PICKAXE_MINING_TAG, pickaxe_values, managed_ore_block_values(catalog))
    generate_ore_worldgen(ore_catalog)


def generate_ore_block_assets(ore: dict, material: dict, host: str) -> None:
    block_id = ore_block_id(ore, host)
    texture = f"rngtech:block/ores/{host}/{ore['material']}"
    write_json(BLOCKSTATE_ROOT / f"{block_id}.json", {
        "variants": {
            "": {
                "model": f"rngtech:block/{block_id}",
            },
        },
    })
    write_json(BLOCK_MODEL_ROOT / f"{block_id}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {
            "all": texture,
        },
    })
    write_json(MODEL_ROOT / f"{block_id}.json", {
        "parent": f"rngtech:block/{block_id}",
    })
    add_lang_entry(f"block.rngtech.{block_id}", ore_display_name(material, host))


def add_lang_entry(key: str, value: str) -> None:
    with LANG_PATH.open(encoding="utf-8") as handle:
        lang = json.load(handle, object_pairs_hook=OrderedDict)
    if key not in lang:
        lang[key] = value
        write_json(LANG_PATH, lang)


def ore_display_name(material: dict, host: str) -> str:
    if host == "deepslate":
        return f"Deepslate {material['display']} Ore"
    if host == "nether":
        return f"Nether {material['display']} Ore"
    if host == "end":
        return f"End Stone {material['display']} Ore"
    return f"{material['display']} Ore"


def generate_ore_loot(block_id: str, raw_item_id: str) -> None:
    write_json(DATA_ROOT / f"rngtech/loot_table/blocks/{block_id}.json", OrderedDict([
        ("type", "minecraft:block"),
        ("pools", [
            OrderedDict([
                ("bonus_rolls", 0.0),
                ("entries", [
                    OrderedDict([
                        ("type", "minecraft:alternatives"),
                        ("children", [
                            OrderedDict([
                                ("type", "minecraft:item"),
                                ("conditions", [
                                    OrderedDict([
                                        ("condition", "minecraft:match_tool"),
                                        ("predicate", OrderedDict([
                                            ("predicates", OrderedDict([
                                                ("minecraft:enchantments", [
                                                    OrderedDict([
                                                        ("enchantments", "minecraft:silk_touch"),
                                                        ("levels", OrderedDict([("min", 1)])),
                                                    ]),
                                                ]),
                                            ])),
                                        ])),
                                    ]),
                                ]),
                                ("name", f"rngtech:{block_id}"),
                            ]),
                            OrderedDict([
                                ("type", "minecraft:item"),
                                ("functions", [
                                    OrderedDict([
                                        ("enchantment", "minecraft:fortune"),
                                        ("formula", "minecraft:ore_drops"),
                                        ("function", "minecraft:apply_bonus"),
                                    ]),
                                    OrderedDict([("function", "minecraft:explosion_decay")]),
                                ]),
                                ("name", raw_item_id),
                            ]),
                        ]),
                    ]),
                ]),
                ("rolls", 1.0),
            ]),
        ]),
        ("random_sequence", f"rngtech:blocks/{block_id}"),
    ]))


def generate_ore_worldgen(ore_catalog: dict) -> None:
    for ore in ore_catalog["ores"]:
        if ore["veins_per_chunk"] <= 0:
            continue
        material_id = ore["material"]
        targets = [ore_worldgen_target(ore, host) for host in ore_generation_hosts(ore)]
        write_json(DATA_ROOT / f"rngtech/worldgen/configured_feature/ore_{material_id}.json", OrderedDict([
            ("type", "minecraft:ore"),
            ("config", OrderedDict([
                ("discard_chance_on_air_exposure", ore["discard_chance_on_air_exposure"]),
                ("size", ore["vein_size"]),
                ("targets", targets),
            ])),
        ]))
        write_json(DATA_ROOT / f"rngtech/worldgen/placed_feature/ore_{material_id}.json", OrderedDict([
            ("feature", f"rngtech:ore_{material_id}"),
            ("placement", [
                OrderedDict([
                    ("type", "minecraft:count"),
                    ("count", ore["veins_per_chunk"]),
                ]),
                OrderedDict([("type", "minecraft:in_square")]),
                OrderedDict([
                    ("type", "minecraft:height_range"),
                    ("height", OrderedDict([
                        ("type", "minecraft:uniform"),
                        ("min_inclusive", OrderedDict([("absolute", ore["min_y"])])),
                        ("max_inclusive", OrderedDict([("absolute", ore["max_y"])])),
                    ])),
                ]),
                OrderedDict([("type", "minecraft:biome")]),
            ]),
        ]))
        write_json(DATA_ROOT / f"rngtech/neoforge/biome_modifier/ore_{material_id}.json", OrderedDict([
            ("neoforge:conditions", [
                OrderedDict([
                    ("type", "rngtech:ore_worldgen_enabled"),
                    ("material", material_id),
                ]),
            ]),
            ("type", "neoforge:add_features"),
            ("biomes", ore.get("biomes", "#minecraft:is_overworld")),
            ("features", f"rngtech:ore_{material_id}"),
            ("step", "underground_ores"),
        ]))


def ore_worldgen_target(ore: dict, host: str) -> OrderedDict:
    block_state = f"rngtech:{ore_block_id(ore, host)}"
    if host == "stone":
        return ore_target("minecraft:stone_ore_replaceables", block_state)
    if host == "deepslate":
        return ore_target("minecraft:deepslate_ore_replaceables", block_state)
    if host == "nether":
        return OrderedDict([
            ("target", OrderedDict([
                ("predicate_type", "minecraft:block_match"),
                ("block", "minecraft:netherrack"),
            ])),
            ("state", OrderedDict([("Name", block_state)])),
        ])
    if host == "end":
        return OrderedDict([
            ("target", OrderedDict([
                ("predicate_type", "minecraft:block_match"),
                ("block", "minecraft:end_stone"),
            ])),
            ("state", OrderedDict([("Name", block_state)])),
        ])
    raise ValueError(f"Unknown ore host: {host}")


def ore_target(replaceables_tag: str, block_state: str) -> OrderedDict:
    return OrderedDict([
        ("target", OrderedDict([
            ("predicate_type", "minecraft:tag_match"),
            ("tag", replaceables_tag),
        ])),
        ("state", OrderedDict([("Name", block_state)])),
    ])


def generate_coverage(catalog: dict, ore_catalog: dict, items: list[dict]) -> None:
    registered_non_stage = [material for material in catalog["non_stage_materials"] if material["handling"] == "register"]
    mapped_non_stage = [material for material in catalog["non_stage_materials"] if material["handling"] == "map"]
    registered_metal_items = [item for item in items if item["kind"] == "metal_form"]
    vanilla_mapped_forms = [
        (material, form, values)
        for material, forms in catalog.get("vanilla_form_mappings", {}).items()
        for form, values in forms.items()
    ]
    lines = [
        "# Material Catalog Coverage",
        "",
        "Status: Implemented",
        "",
        "Generated from `src/main/resources/data/rngtech/materials/catalog.json` and `src/main/resources/data/rngtech/materials/ore_catalog.json`.",
        "",
        "## Staged Material Families",
        "",
        "| Material | Stage | Default | Reason |",
        "|---|---:|---|---|",
    ]
    for material in catalog["materials"]:
        default = "Enabled" if material["default_enabled"] else "Disabled"
        lines.append(f"| `{material['id']}` | {material['stage']} | {default} | {material['default_reason']} |")
    material_groups = catalog.get("material_groups", [])
    if material_groups:
        lines.extend([
            "",
            "## Material Family Groups",
            "",
            "| Group | Families | Reason |",
            "|---|---|---|",
        ])
        for group in material_groups:
            families = ", ".join(f"`{family}`" for family in group["families"])
            lines.append(f"| `rngtech:materials/family/{group['id']}` | {families} | {group['reason']} |")
    lines.extend([
        "",
        "## Registered Item Counts",
        "",
        f"- Registered metal form items: {len(registered_metal_items)}",
        f"- Vanilla-mapped metal forms: {len(vanilla_mapped_forms)}",
        f"- Generic form items: {len(catalog['generic_forms'])}",
        f"- Registered non-stage items: {len(registered_non_stage)}",
        f"- Registered RNGTech ore blocks: {sum(len(ore_hosts(ore)) for ore in ore_catalog['ores'])}",
        f"- Total generated material item models: {len(items)}",
        "",
        "## Ore Block Catalog",
        "",
        "| Material | Hardness | Default worldgen | Biomes | Variants |",
        "|---|---:|---|---|---|",
    ])
    for ore in ore_catalog["ores"]:
        default = "Enabled" if ore["generated_by_default"] and ore["veins_per_chunk"] > 0 else "Disabled"
        biomes = ore.get("biomes", "#minecraft:is_overworld")
        variants = ", ".join(f"`rngtech:{ore_block_id(ore, host)}`" for host in ore_hosts(ore))
        lines.append(f"| `{ore['material']}` | {ore['hardness_level']} | {default} | `{biomes}` | {variants} |")
    lines.extend([
        "",
        "Default ore generation can be disabled globally with `worldgen.ores.enabled`, per family with `worldgen.ores.<material>.enabled`, or by overriding the stable `data/rngtech/neoforge/biome_modifier/ore_<material>.json` file.",
        "",
        "## Vanilla-Mapped Metal Forms",
        "",
        "| Material | Form | Uses |",
        "|---|---|---|",
    ])
    for material, form, values in vanilla_mapped_forms:
        uses = ", ".join(f"`{value}`" for value in values)
        lines.append(f"| `{material}` | `{form}` | {uses} |")
    lines.extend([
        "",
        "## Non-Stage Material Handling",
        "",
        "| Material | Handling | Maps to | Reason |",
        "|---|---|---|---|",
    ])
    for material in catalog["non_stage_materials"]:
        maps_to = material.get("maps_to", "")
        lines.append(f"| `{material['id']}` | {material['handling']} | `{maps_to}` | {material['reason']} |")
    lines.extend([
        "",
        "Mapped non-stage materials use vanilla or external ingredients in recipes instead of registering RNGTech item ids.",
        "",
        f"Registered non-stage materials: {', '.join(f'`{m['id']}`' for m in registered_non_stage)}.",
        f"Mapped non-stage materials: {len(mapped_non_stage)}.",
        "",
    ])
    with COVERAGE_PATH.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(lines))


def main() -> None:
    catalog = load_catalog()
    ore_catalog = load_ore_catalog()
    items = material_items(catalog)
    validate_textures(items)
    remove_vanilla_mapped_models(catalog)
    cleanup_excluded_form_resources(catalog)
    generate_models(items, ore_catalog)
    generate_lang(items)
    generate_common_tags(catalog, ore_catalog)
    generate_rngtech_tags(catalog, ore_catalog, items)
    generate_circuit_tags(catalog)
    generate_recipes(catalog, ore_catalog)
    generate_ore_resources(catalog, ore_catalog)
    generate_coverage(catalog, ore_catalog, items)
    print(f"Generated {len(items)} material item models and supporting resources.")


if __name__ == "__main__":
    main()
