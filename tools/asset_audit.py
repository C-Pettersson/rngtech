#!/usr/bin/env python3
"""Audit RNGTech asset identity and texture reuse.

The audit intentionally uses only the Python standard library so it can run in
offline development and CI environments without image dependencies.
"""

from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import struct
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/resources/assets/rngtech"
MODEL_ROOT = ASSET_ROOT / "models"
BLOCKSTATE_ROOT = ASSET_ROOT / "blockstates"
TEXTURE_ROOT = ASSET_ROOT / "textures"
DEFAULT_ALLOWLIST = ROOT / "tools/asset_audit_allowlist.json"

IN_SCOPE_MACHINE_BLOCKS = [
    "affix_forge",
    "debug_reroller",
    "crusher",
    "*_crusher_chassis",
    "furnace",
    "*_furnace_chassis",
    "*_alloy_furnace_chassis",
    "*_solid_fuel_burner",
    "*_battery_chassis",
    "*_resonance_calibrator_chassis",
    "*_component_recycler",
    "crude_metal_press",
    "metal_press",
    "melter",
    "bio_generator",
    "potential_reactor",
]


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def walk_values(value: Any):
    if isinstance(value, dict):
        for child in value.values():
            yield from walk_values(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_values(child)
    else:
        yield value


def collect_texture_refs(model: Any) -> list[str]:
    refs: list[str] = []
    textures = model.get("textures") if isinstance(model, dict) else None
    if isinstance(textures, dict):
        for value in textures.values():
            if isinstance(value, str) and value.startswith("rngtech:"):
                refs.append(value)
    return refs


def texture_path(ref: str) -> Path | None:
    if ref.startswith("rngtech:block/"):
        return TEXTURE_ROOT / "block" / f"{ref.removeprefix('rngtech:block/')}.png"
    if ref.startswith("rngtech:item/"):
        return TEXTURE_ROOT / "item" / f"{ref.removeprefix('rngtech:item/')}.png"
    return None


def block_model_path(ref: str) -> Path | None:
    if not ref.startswith("rngtech:block/"):
        return None
    return MODEL_ROOT / "block" / f"{ref.removeprefix('rngtech:block/')}.json"


def item_model_path(ref: str) -> Path | None:
    if not ref.startswith("rngtech:item/"):
        return None
    return MODEL_ROOT / "item" / f"{ref.removeprefix('rngtech:item/')}.json"


def collect_models_from_blockstate(blockstate: Any) -> set[str]:
    models: set[str] = set()
    for value in walk_values(blockstate):
        if isinstance(value, str) and value.startswith("rngtech:block/"):
            models.add(value)
    return models


def in_scope_machine(block_id: str) -> bool:
    return any(fnmatch.fnmatch(block_id, pattern) for pattern in IN_SCOPE_MACHINE_BLOCKS)


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as handle:
        header = handle.read(24)
    if len(header) < 24 or not header.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError(f"{rel(path)} is not a PNG")
    return struct.unpack(">II", header[16:24])


def resolve_item_texture_stack(item_id: str, item_models: dict[str, Any], seen: set[str] | None = None) -> tuple[tuple[str, str], ...]:
    seen = seen or set()
    if item_id in seen:
        return tuple()
    seen.add(item_id)
    model = item_models.get(item_id)
    if not isinstance(model, dict):
        return tuple()
    layers: dict[str, str] = {}
    parent = model.get("parent")
    if isinstance(parent, str) and parent.startswith("rngtech:item/"):
        layers.update(resolve_item_texture_stack(parent.removeprefix("rngtech:item/"), item_models, seen))
    textures = model.get("textures")
    if isinstance(textures, dict):
        for key, value in textures.items():
            if key.startswith("layer") and isinstance(value, str):
                layers[key] = value
    return tuple(sorted(layers.items()))


def finding(check: str, key: str, subjects: list[str], detail: str) -> dict[str, Any]:
    return {
        "check": check,
        "key": key,
        "subjects": sorted(subjects),
        "detail": detail,
    }


def load_allowlist(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    data = load_json(path)
    entries = data.get("entries", [])
    if not isinstance(entries, list):
        raise ValueError(f"{rel(path)} entries must be a list")
    for entry in entries:
        if not entry.get("reason"):
            raise ValueError(f"{rel(path)} allowlist entry is missing a reason: {entry}")
    return entries


def matches_any(value: str, patterns: list[str]) -> bool:
    return any(fnmatch.fnmatch(value, pattern) for pattern in patterns)


def is_allowed(item: dict[str, Any], entries: list[dict[str, Any]]) -> str | None:
    for entry in entries:
        if entry.get("check") != item["check"]:
            continue
        key_pattern = entry.get("key")
        if key_pattern and not fnmatch.fnmatch(item["key"], key_pattern):
            continue
        subject_patterns = entry.get("subject_globs")
        if subject_patterns and not all(matches_any(subject, subject_patterns) for subject in item["subjects"]):
            continue
        return str(entry["reason"])
    return None


def audit() -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []

    model_files = sorted(MODEL_ROOT.rglob("*.json"))
    model_json = {rel(path): load_json(path) for path in model_files}
    blockstate_files = sorted(BLOCKSTATE_ROOT.glob("*.json"))
    blockstates = {path.stem: load_json(path) for path in blockstate_files}

    model_texture_refs: dict[str, list[str]] = {}
    all_texture_refs: set[str] = set()
    for path_key, data in model_json.items():
        refs = collect_texture_refs(data)
        model_texture_refs[path_key] = refs
        all_texture_refs.update(refs)
        for ref in refs:
            path = texture_path(ref)
            if path is not None and not path.exists():
                findings.append(finding(
                    "missing_texture_reference",
                    ref,
                    [path_key],
                    f"Model references missing texture {ref}.",
                ))

    item_models = {
        path.stem: model_json[rel(path)]
        for path in sorted((MODEL_ROOT / "item").glob("*.json"))
    }
    item_texture_stacks: dict[str, tuple[tuple[str, str], ...]] = {}
    for item_id in item_models:
        stack = resolve_item_texture_stack(item_id, item_models)
        if stack:
            item_texture_stacks[item_id] = stack
    texture_stack_groups: dict[tuple[tuple[str, str], ...], list[str]] = defaultdict(list)
    for item_id, stack in item_texture_stacks.items():
        texture_stack_groups[stack].append(item_id)
    for stack, item_ids in sorted(texture_stack_groups.items()):
        if len(item_ids) > 1:
            layer0 = dict(stack).get("layer0", "<no layer0>")
            findings.append(finding(
                "shared_item_layer0",
                layer0,
                item_ids,
                f"{len(item_ids)} item models resolve to the same item texture stack.",
            ))

    blockstate_model_groups: dict[str, list[str]] = defaultdict(list)
    block_to_models: dict[str, set[str]] = {}
    for block_id, data in blockstates.items():
        models = collect_models_from_blockstate(data)
        block_to_models[block_id] = models
        for model in models:
            blockstate_model_groups[model].append(block_id)
    for model, block_ids in sorted(blockstate_model_groups.items()):
        distinct = sorted(set(block_ids))
        if len(distinct) > 1:
            findings.append(finding(
                "shared_blockstate_model",
                model,
                distinct,
                f"{len(distinct)} blockstates reference the same block model.",
            ))

    block_texture_groups: dict[str, set[str]] = defaultdict(set)
    for block_id, models in block_to_models.items():
        if not in_scope_machine(block_id):
            continue
        for model_ref in models:
            path = block_model_path(model_ref)
            if path is None or not path.exists():
                continue
            refs = model_texture_refs.get(rel(path), collect_texture_refs(load_json(path)))
            for ref in refs:
                if ref.startswith("rngtech:block/"):
                    block_texture_groups[ref].add(block_id)
    for ref, block_ids in sorted(block_texture_groups.items()):
        if len(block_ids) > 1:
            findings.append(finding(
                "shared_machine_block_texture",
                ref,
                sorted(block_ids),
                f"{len(block_ids)} in-scope machine blocks reference the same block texture.",
            ))

    for block_id, models in block_to_models.items():
        if not in_scope_machine(block_id):
            continue
        for model_ref in models:
            path = block_model_path(model_ref)
            if path is None or not path.exists():
                continue
            refs = model_texture_refs.get(rel(path), collect_texture_refs(load_json(path)))
            for ref in refs:
                path = texture_path(ref)
                if path is None or not path.exists() or path.suffix != ".png":
                    continue
                width, height = png_size(path)
                if width < 64 or height < 64:
                    findings.append(finding(
                        "low_resolution_machine_texture",
                        ref,
                        [block_id, rel(path)],
                        f"In-scope machine texture is {width}x{height}; expected 64x64 frames or larger.",
                    ))

    pngs = sorted(TEXTURE_ROOT.rglob("*.png"))
    digest_groups: dict[str, list[str]] = defaultdict(list)
    for path in pngs:
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        digest_groups[digest].append(rel(path))
    for digest, paths in sorted(digest_groups.items()):
        if len(paths) > 1:
            findings.append(finding(
                "duplicate_png",
                digest,
                paths,
                f"{len(paths)} PNG files have byte-identical content.",
            ))

    referenced_paths = {
        rel(path)
        for ref in all_texture_refs
        for path in [texture_path(ref)]
        if path is not None
    }
    for path in pngs:
        path_key = rel(path)
        if path_key not in referenced_paths:
            findings.append(finding(
                "unused_png",
                path_key,
                [path_key],
                "PNG is not referenced by any model JSON texture slot.",
            ))

    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allowlist", type=Path, default=DEFAULT_ALLOWLIST)
    parser.add_argument("--json", action="store_true", help="Print the full finding payload as JSON.")
    args = parser.parse_args()

    entries = load_allowlist(args.allowlist)
    findings = audit()
    unallowed: list[dict[str, Any]] = []
    allowed_count = 0
    for item in findings:
        reason = is_allowed(item, entries)
        if reason is None:
            unallowed.append(item)
        else:
            allowed_count += 1

    if args.json:
        print(json.dumps({"allowed": allowed_count, "unallowed": unallowed}, indent=2))
    else:
        print(f"Asset audit: {len(findings)} finding(s), {allowed_count} allowlisted, {len(unallowed)} unallowlisted.")
        for item in unallowed:
            subjects = ", ".join(item["subjects"][:8])
            if len(item["subjects"]) > 8:
                subjects += f", ... (+{len(item['subjects']) - 8} more)"
            print(f"- [{item['check']}] {item['key']}: {item['detail']} Subjects: {subjects}")

    return 1 if unallowed else 0


if __name__ == "__main__":
    sys.exit(main())
