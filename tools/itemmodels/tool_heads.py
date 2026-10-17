#!/usr/bin/env python3
"""Generate the Tool Head item textures for every head material and family.

Each family has its own silhouette, drawn as an ASCII mask in the assembled tool's 16x16 frame around the tip of the
Tool Rod, so the head lands on the rod exactly: a classic double-pointed Pick, a sledge Hammer with flared faces, a
rounded Shovel spade, a broad Digger scoop, a single-bit Axe, and a double-bit Treefeller. Every head except the Axe is
mirror-symmetric about the handle. Shading is flat, as on vanilla tools: a one-pixel outline, a bright line inside the
top and left edges, and a darker line inside the bottom and right.

The loose head item is the same drawing shifted by whole pixels to the middle of its icon; ModularToolItemRenderer
shifts it back with the PartTransform for its family. The script rewrites those PartTransform constants in the
renderer, and modular_tool_preview.py --check verifies them. It also writes the assembled Iron-head-on-Wooden-rod
texture each modular tool model uses as its particle and wiki icon.

Run from the repository root:

    python tools/itemmodels/tool_heads.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src/main/resources/assets/rngtech/textures/item"
OUTPUT = TEXTURES / "tool_heads"
ASSEMBLED = TEXTURES / "modular_tool"
DEFAULT_ROD = TEXTURES / "materials/non_stage/wood.png"
RENDERER = ROOT / "src/main/java/com/rngtech/client/renderer/ModularToolItemRenderer.java"

sys.path.insert(0, str(ROOT / "tools"))
from generate_material_textures import MATERIALS, color, mix, shade  # noqa: E402

FAMILIES = ("pick", "hammer", "shovel", "digger", "axe", "treefeller")
MATERIAL_ORDER = (
    "flint",
    "iron",
    "copper",
    "bronze",
    "steel",
    "aluminum",
    "titanium",
    "tungstensteel",
    "nullite",
    "exotic",
)
MATERIAL_SPECS = {
    "flint": {"base": "#55545b", "accent": "#b4b3bd", "binding": "#9a7046"},
    "exotic": MATERIALS["naquadah"],
    **{name: MATERIALS[name] for name in MATERIAL_ORDER if name in MATERIALS},
}

# Rod tip in the assembled tool frame. Tool Rod textures end at (12, 4); the renderer's ROD_TRANSFORM moves the rod
# one pixel down and left, leaving room for the head above it.
ROD_SHIFT = (-1, 1)
TIP = (12 + ROD_SHIFT[0], 4 + ROD_SHIFT[1])

MASKS = {
    "pick": (
        "................",
        "................",
        ".....########...",
        "...###########..",
        "..e############.",
        ".........######.",
        "...........####.",
        "...........####.",
        "............###.",
        "............###.",
        "............###.",
        "............###.",
        "............##..",
        "............##..",
        "............e...",
        "................",
    ),
    "hammer": (
        "................",
        ".........f......",
        "........ffg.....",
        ".......ffg#.....",
        "......ffg###....",
        ".......g#####...",
        ".........#####g.",
        "..........###gff",
        "...........#gff.",
        "...........gff..",
        "............f...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    "shovel": (
        "................",
        "................",
        "..........##eee.",
        ".........####ee.",
        "........####s#e.",
        "........###s###.",
        ".........#s####.",
        "........cc####..",
        "........cc.##...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    "digger": (
        "................",
        ".........re.....",
        "........rree....",
        "........r##ee...",
        ".......rr###ee..",
        ".......r#####ee.",
        "........######ee",
        "........c#####rr",
        ".......ccc##rrr.",
        "........c..rr...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    "axe": (
        ".......ee##.....",
        "......ee###.....",
        ".....ee#####....",
        "....ee#######...",
        "...ee#########..",
        "..ee#########cc.",
        ".ee#########cc..",
        ".........##cc...",
        "..........cc....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    "treefeller": (
        "................",
        "........ee#.....",
        ".......ee##.....",
        "......ee###c....",
        ".....ee###ccc...",
        "....ee###ccccc..",
        "........ccccc###",
        ".........ccc###e",
        "..........c###ee",
        "...........##ee.",
        "...........#ee..",
        "...........ee...",
        "...........e....",
        "................",
        "................",
        "................",
    ),
}

PARTS = {"#": "body", "e": "edge", "f": "face", "g": "band", "c": "collar", "r": "rim", "s": "spine"}


def parts(family: str) -> dict[tuple[int, int], str]:
    """Pixels of a head in the assembled tool frame, keyed by position, valued by part name."""
    return {
        (x, y): PARTS[cell]
        for y, row in enumerate(MASKS[family])
        for x, cell in enumerate(row)
        if cell != "."
    }


def tones(spec: dict[str, str]) -> dict[str, tuple[int, int, int, int]]:
    base = color(spec["base"])
    accent = color(spec["accent"])
    return {
        "outline": shade(base, -0.82),
        "rim": shade(base, -0.6),
        "dark": shade(base, -0.25),
        "mid": base,
        "light": mix(base, accent, 0.5),
        "accent": accent,
        "glow": color(spec.get("glow", spec["accent"])),
        "binding": color(spec.get("binding", spec["base"])),
    }


def exposure(x: int, y: int, drawn: dict[tuple[int, int], str], depth: int) -> tuple[bool, bool]:
    top_left = (x, y - depth) not in drawn or (x - depth, y) not in drawn
    bottom_right = (x, y + depth) not in drawn or (x + depth, y) not in drawn
    return top_left, bottom_right


def pixel(part: str, x: int, y: int, drawn: dict[tuple[int, int], str], t: dict, spec: dict) -> tuple:
    """Flat item-sprite shading, as on vanilla tools: a full one-pixel outline, darkest on the bottom and right,
    a bright line just inside the top and left edges, and a darker line just inside the bottom and right."""
    top_left, bottom_right = exposure(x, y, drawn, 1)
    if bottom_right:
        return t["outline"]
    if top_left:
        return t["rim"]
    inner_top_left, inner_bottom_right = exposure(x, y, drawn, 2)
    if part == "collar":
        if "binding" in spec:
            return shade(t["binding"], 0.2) if (x + y) % 2 else t["binding"]
        return t["dark"]
    if part == "band":
        return t["glow"] if "glow" in spec else t["dark"]
    if part == "rim":
        return t["dark"]
    if inner_top_left or part in {"edge", "face", "spine"}:
        return t["accent"] if part in {"edge", "face"} or inner_top_left and inner_bottom_right else t["light"]
    if inner_bottom_right:
        return t["dark"]
    return t["mid"]


def render(family: str, material: str) -> Image.Image:
    spec = MATERIAL_SPECS[material]
    t = tones(spec)
    drawn = parts(family)
    image = Image.new("RGBA", (16, 16))
    for (x, y), part in drawn.items():
        image.putpixel((x, y), pixel(part, x, y, drawn, t, spec))
    return image


def render_offset(family: str) -> tuple[int, int]:
    """Whole-pixel shift from the centred loose icon back onto the rod tip, in texture pixels (y down)."""
    drawn = parts(family)
    xs = [x for x, _ in drawn]
    ys = [y for _, y in drawn]
    centre_x = (16 - (max(xs) - min(xs) + 1)) // 2 - min(xs)
    centre_y = (16 - (max(ys) - min(ys) + 1)) // 2 - min(ys)
    return -centre_x, -centre_y


def loose_icon(family: str, material: str) -> Image.Image:
    dx, dy = render_offset(family)
    icon = Image.new("RGBA", (16, 16))
    icon.paste(render(family, material), (-dx, -dy))
    return icon


def assembled_icon(family: str) -> Image.Image:
    """An Iron head on a Wooden Tool Rod, as an unassembled tool renders; the modular tool models' particle."""
    icon = Image.new("RGBA", (16, 16))
    icon.paste(Image.open(DEFAULT_ROD).convert("RGBA"), ROD_SHIFT)
    icon.alpha_composite(render(family, "iron"))
    return icon


def java_float(pixels: int) -> str:
    return f"{pixels / 16}F"


def write_renderer_transforms() -> None:
    shifts = {"ROD": ROD_SHIFT} | {f"{family.upper()}_HEAD": render_offset(family) for family in FAMILIES}
    source = RENDERER.read_text(encoding="utf-8")
    for name, (dx, dy) in shifts.items():
        replacement = f"{name}_TRANSFORM = new PartTransform({java_float(dx)}, {java_float(-dy)}, 1.0F)"
        source, count = re.subn(rf"{name}_TRANSFORM = new PartTransform\([^)]*\)", replacement, source)
        if count != 1:
            raise SystemExit(f"{RENDERER.name}: expected one {name}_TRANSFORM, found {count}")
    RENDERER.write_text(source, encoding="utf-8", newline="\n")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for material in MATERIAL_ORDER:
        for family in FAMILIES:
            loose_icon(family, material).save(OUTPUT / f"{material}_{family}_head.png")
    ASSEMBLED.mkdir(parents=True, exist_ok=True)
    for family in FAMILIES:
        assembled_icon(family).save(ASSEMBLED / f"{family}.png")
    write_renderer_transforms()
    print(f"Wrote {len(MATERIAL_ORDER) * len(FAMILIES)} tool head textures, {len(FAMILIES)} assembled tool textures,"
          f" and the {RENDERER.stem} transforms")


if __name__ == "__main__":
    main()
