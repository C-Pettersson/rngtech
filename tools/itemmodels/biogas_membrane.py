#!/usr/bin/env python3
"""Generate the Biogas Membrane item texture.

A hollow-fibre membrane module lying on its side: a steel pressure tube with flanged end caps, a window onto the
fibre bundle, and a port stub on each end of the top, one for the raw gas and one for the Methane. Shading is flat, as
on vanilla items. Run from the repository root:

    python tools/itemmodels/biogas_membrane.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/rngtech/textures/item/biogas_membrane.png"

# One character per pixel: O outline, L light steel, S steel, D dark steel, F fibre, f dark fibre, P port, . clear.
MASK = (
    "................",
    "................",
    "...OOO....OOO...",
    "...OPO....OPO...",
    ".OOOPOOOOOOPOOO.",
    ".OLLLLLLLLLLLLO.",
    ".ODLSOOOOOOSLDO.",
    ".ODLSOFfFfFOLDO.",
    ".ODLSOfFfFfOLDO.",
    ".ODLSOFfFfFOLDO.",
    ".ODLSOOOOOOSLDO.",
    ".ODDDDDDDDDDDDO.",
    ".OOOOOOOOOOOOOO.",
    "................",
    "................",
    "................",
)

COLORS = {
    "O": (38, 42, 48, 255),
    "L": (196, 204, 212, 255),
    "S": (150, 160, 170, 255),
    "D": (98, 106, 116, 255),
    "F": (202, 206, 120, 255),
    "f": (150, 158, 78, 255),
    "P": (176, 118, 72, 255),
    ".": (0, 0, 0, 0),
}


def main() -> None:
    image = Image.new("RGBA", (16, 16))
    for y, row in enumerate(MASK):
        if len(row) != 16:
            raise SystemExit(f"row {y} is {len(row)} pixels wide")
        for x, key in enumerate(row):
            image.putpixel((x, y), COLORS[key])
    image.save(OUT)
    print(f"wrote {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
