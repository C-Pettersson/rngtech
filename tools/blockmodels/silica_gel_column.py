#!/usr/bin/env python3
"""Generate the working Silica Gel column casing art, models, and blockstate.

While the controller works, its two casings swap to animated side textures: a band of wet, blue
beads with a bright front climbs through the bead bed from the bottom of the lower casing to the
top of the upper one, like moisture moving through indicating silica gel. Both strips share a frame
count and frame time, so the band crosses the seam between the casings in step. The idle textures
are the source; this script only derives the working strips from them.
Run from the repository root:

    python tools/blockmodels/silica_gel_column.py
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
TEXTURES = ASSETS / "textures/block/silica_gel_dehumidifier"
TEX = "rngtech:block/silica_gel_dehumidifier/"

SIZE = 64
BED_X = range(16, 48)
# Bead bed rows in each texture, and where that texture's lowest bed row sits along the column.
LOWER_ROWS = range(0, 58)
UPPER_ROWS = range(10, 64)
LOWER_BASE = 0
UPPER_BASE = len(LOWER_ROWS)
COLUMN = len(LOWER_ROWS) + len(UPPER_ROWS)

FRAMES = 28
STEP = COLUMN // FRAMES
FRAME_TIME = 6
FRONT = 3
TAIL = 30
WET = (96, 160, 196)
WET_MAX = 0.55
GLOW = (236, 250, 244)
GLOW_MIX = 0.35


def lerp(a: tuple[int, ...], b: tuple[int, ...], t: float) -> tuple[int, ...]:
    return tuple(round(x + (y - x) * t) for x, y in zip(a[:3], b[:3])) + (a[3],)


def is_bead(pixel: tuple[int, ...]) -> bool:
    r, g, b, _ = pixel
    return 0.3 * r + 0.59 * g + 0.11 * b > 130


def working_strip(source: Image.Image, rows: range, base: int) -> Image.Image:
    strip = Image.new("RGBA", (SIZE, SIZE * FRAMES))
    for frame in range(FRAMES):
        front = frame * STEP
        image = source.copy()
        for y in rows:
            along = base + (rows.stop - 1 - y)
            below = (front - along) % COLUMN
            for x in BED_X:
                pixel = image.getpixel((x, y))
                if not is_bead(pixel):
                    continue
                if below < FRONT:
                    image.putpixel((x, y), lerp(pixel, GLOW, GLOW_MIX))
                elif below < FRONT + TAIL:
                    image.putpixel((x, y), lerp(pixel, WET, WET_MAX * (1 - (below - FRONT) / TAIL)))
        strip.paste(image, (0, frame * SIZE))
    return strip


def write_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def casing_model(name: str, side: str, up: str, down: str) -> None:
    textures = {"down": TEX + down, "up": TEX + up, "particle": TEX + side}
    textures.update({face: TEX + side for face in ("north", "south", "east", "west")})
    write_json(ASSETS / f"models/block/{name}.json", {"parent": "minecraft:block/cube", "textures": textures})


def main() -> None:
    for part, rows, base in (("lower", LOWER_ROWS, LOWER_BASE), ("upper", UPPER_ROWS, UPPER_BASE)):
        source = Image.open(TEXTURES / f"silica_gel_column_casing_side_{part}.png").convert("RGBA")
        name = f"silica_gel_column_casing_side_{part}_active"
        working_strip(source, rows, base).save(TEXTURES / f"{name}.png")
        write_json(TEXTURES / f"{name}.png.mcmeta", {"animation": {"frametime": FRAME_TIME, "interpolate": True}})
    casing_model("silica_gel_column_casing_lower_active", "silica_gel_column_casing_side_lower_active",
                 "silica_gel_column_casing_join", "silica_gel_column_casing_join")
    casing_model("silica_gel_column_casing_upper_active", "silica_gel_column_casing_side_upper_active",
                 "silica_gel_column_casing_top", "silica_gel_column_casing_join")
    variants = {}
    for part in ("lower", "single", "upper"):
        for active in (False, True):
            model = "silica_gel_column_casing" if part == "single" else f"silica_gel_column_casing_{part}"
            if active and part != "single":
                model += "_active"
            variants[f"active={str(active).lower()},part={part}"] = {"model": f"rngtech:block/{model}"}
    write_json(ASSETS / "blockstates/silica_gel_column_casing.json", {"variants": variants})


if __name__ == "__main__":
    main()
