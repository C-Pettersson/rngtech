#!/usr/bin/env python3
"""Render assembled Modular Field Tools outside the game.

Composites every Tool Head over every Tool Rod the way ModularToolItemRenderer does: the rod and the head are
each translated and scaled into the 16x16 item frame by the PartTransform constants read straight from the renderer
source, so the preview cannot drift from the game. Writes a contact sheet with one row per head
material, one column per family, and a strip of each family on every rod.

With --check, also fails when a part transform is not a whole-pixel shift at scale 1, or when the rod or a head
does not sit where tools/itemmodels/tool_heads.py designed it.

Run from the repository root:

    python tools/itemmodels/modular_tool_preview.py [--out build/modular-tools/preview.png] [--check]
"""

from __future__ import annotations

import argparse
import io
import json
import re
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets"
RENDERER = ROOT / "src/main/java/com/rngtech/client/renderer/ModularToolItemRenderer.java"
DEFAULT_OUT = ROOT / "build/modular-tools/preview.png"

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(ROOT / "tools/rngwiki"))
import tool_heads  # noqa: E402

ROD_MODELS = (
    "wooden_tool_rod",
    "iron_tool_rod",
    "bronze_tool_rod",
    "aluminum_tool_rod",
    "copper_conduit_tool_rod",
    "steel_reinforced_tool_rod",
    "sparksteel_routed_tool_rod",
    "titanium_stabilized_tool_rod",
    "tungstensteel_heavy_tool_rod",
    "nullite_phase_tool_rod",
    "exotic_harmonic_tool_rod",
)
SCALE = 6
GAP = 4
BACKGROUND = (139, 139, 139, 255)


def read_transforms() -> dict[str, tuple[float, float, float]]:
    """PartTransforms by part: "rod" and one per head family."""
    source = RENDERER.read_text(encoding="utf-8")
    pattern = re.compile(
        r"PartTransform (\w+?)(?:_HEAD)?_TRANSFORM = new PartTransform\("
        r"\s*(-?[\d.]+)F,\s*(-?[\d.]+)F,\s*(-?[\d.]+)F\s*\)"
    )
    transforms = {name.lower(): (float(x), float(y), float(s)) for name, x, y, s in pattern.findall(source)}
    missing = [part for part in ("rod", *tool_heads.FAMILIES) if part not in transforms]
    if missing:
        raise SystemExit(f"ModularToolItemRenderer has no PartTransform for: {', '.join(missing)}")
    return transforms


def vanilla_texture(path: str) -> Image.Image:
    import vanilla

    jar = vanilla.find_client_jar()
    if jar is None:
        raise SystemExit("Minecraft client jar not found; run python tools/rngwiki/vanilla.py --fetch")
    with zipfile.ZipFile(jar) as archive:
        return Image.open(io.BytesIO(archive.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def texture(reference: str) -> Image.Image:
    namespace, path = reference.split(":", 1) if ":" in reference else ("minecraft", reference)
    if namespace == "minecraft":
        return vanilla_texture(path)
    return Image.open(ASSETS / namespace / "textures" / f"{path}.png").convert("RGBA")


def model_texture(model: str) -> Image.Image:
    data = json.loads((ASSETS / "rngtech/models/item" / f"{model}.json").read_text(encoding="utf-8"))
    return texture(data["textures"]["layer0"])


def place(part: Image.Image, transform: tuple[float, float, float]) -> Image.Image:
    """Map a part texture into the tool's 16x16 frame like the renderer: scale about the centre, then shift."""
    x, y, scale = transform
    frame = Image.new("RGBA", (16 * SCALE, 16 * SCALE))
    size = round(16 * SCALE * scale)
    scaled = part.resize((size, size), Image.NEAREST)
    offset = (16 * SCALE - size) / 2
    frame.alpha_composite(scaled, (round(offset + x * 16 * SCALE), round(offset - y * 16 * SCALE)))
    return frame


def assemble(rod: Image.Image, head: Image.Image, transforms: dict, family: str) -> Image.Image:
    tool = place(rod, transforms["rod"])
    tool.alpha_composite(place(head, transforms[family]))
    return tool


def contact_sheet(transforms: dict[str, tuple[float, float, float]], materials: tuple[str, ...]) -> Image.Image:
    cell = 16 * SCALE + GAP
    rods = [model_texture(model) for model in ROD_MODELS]
    families = tool_heads.FAMILIES
    width = cell * max(2 * len(families), len(rods)) + GAP
    height = cell * (len(materials) + len(families)) + 3 * GAP
    sheet = Image.new("RGBA", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(sheet)
    default_rod = model_texture("iron_tool_rod")
    for row, material in enumerate(materials):
        for column, family in enumerate(families):
            head = texture(f"rngtech:item/tool_heads/{material}_{family}_head")
            x, y = GAP + column * 2 * cell, GAP + row * cell
            sheet.alpha_composite(head.resize((16 * SCALE, 16 * SCALE), Image.NEAREST), (x, y))
            sheet.alpha_composite(assemble(default_rod, head, transforms, family), (x + cell, y))
    top = GAP + len(materials) * cell + 2 * GAP
    draw.line([(0, top - GAP), (width, top - GAP)], fill=(90, 90, 90, 255), width=2)
    for row, family in enumerate(families):
        head = texture(f"rngtech:item/tool_heads/iron_{family}_head")
        for column, rod in enumerate(rods):
            sheet.alpha_composite(assemble(rod, head, transforms, family), (GAP + column * cell, top + row * cell))
    return sheet


def check(transforms: dict[str, tuple[float, float, float]]) -> list[str]:
    errors = []
    expected = {"rod": tool_heads.ROD_SHIFT} | {family: tool_heads.render_offset(family) for family in tool_heads.FAMILIES}
    for part, (dx, dy) in expected.items():
        x, y, scale = transforms[part]
        if scale != 1.0:
            errors.append(f"{part}: scale {scale} blurs pixels; use 1.0")
        if abs(x * 16 - dx) > 0.01 or abs(-y * 16 - dy) > 0.01:
            errors.append(
                f"{part}: PartTransform({x}F, {y}F) shifts by ({x * 16:+.2f}, {-y * 16:+.2f}) px,"
                f" tool_heads.py expects PartTransform({dx / 16:g}F, {-dy / 16:g}F, 1.0F)"
            )
    return errors


def main() -> None:
    global SCALE
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--check", action="store_true", help="fail when a head is off its designed position")
    parser.add_argument("--material", action="append", choices=tool_heads.MATERIAL_ORDER, help="only these heads")
    parser.add_argument("--scale", type=int, default=SCALE, help="screen pixels per texture pixel")
    args = parser.parse_args()
    SCALE = args.scale
    transforms = read_transforms()
    args.out.parent.mkdir(parents=True, exist_ok=True)
    contact_sheet(transforms, tuple(args.material or tool_heads.MATERIAL_ORDER)).save(args.out)
    print(f"Wrote {args.out.relative_to(ROOT) if args.out.is_relative_to(ROOT) else args.out}")
    if args.check:
        errors = check(transforms)
        for error in errors:
            print(f"error: {error}", file=sys.stderr)
        if errors:
            raise SystemExit(1)


if __name__ == "__main__":
    main()
