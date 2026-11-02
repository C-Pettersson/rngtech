#!/usr/bin/env python3
"""Generate the jewel item textures.

Three cut jewels in a gold bezel, one per Mastery attribute colour (teal Control, gold Drive, red Reserve), and the
uncut Jewel Blank: the same bezel around a grey stone. Shading is flat, as on vanilla items. Run from the repository
root:

    python tools/itemmodels/jewels.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/rngtech/textures/item"

# One character per pixel: O outline, B bezel, b dark bezel, H stone highlight, S stone, D dark stone, . clear.
MASK = (
    "................",
    "......OOOO......",
    "....OOBBBBOO....",
    "...OBBHHSSBBO...",
    "...OBHHSSSDBO...",
    "..OBHHSSSSDDBO..",
    "..OBHSSSSSDDBO..",
    "..OBSSSSSDDDBO..",
    "..OBSSSSDDDDBO..",
    "..ObSSDDDDDDbO..",
    "...ObDDDDDDbO...",
    "...ObbDDDDbbO...",
    "....OObbbbOO....",
    "......OOOO......",
    "................",
    "................",
)

BEZEL = {"O": (46, 32, 10, 255), "B": (232, 190, 74, 255), "b": (168, 128, 36, 255)}

STONES = {
    "control_jewel": {"H": (196, 246, 240, 255), "S": (64, 190, 178, 255), "D": (22, 110, 104, 255)},
    "drive_jewel": {"H": (255, 244, 190, 255), "S": (244, 196, 64, 255), "D": (160, 110, 20, 255)},
    "reserve_jewel": {"H": (255, 190, 184, 255), "S": (214, 64, 56, 255), "D": (120, 22, 22, 255)},
    "jewel_blank": {"H": (214, 214, 206, 255), "S": (150, 150, 144, 255), "D": (92, 92, 88, 255)},
}


def render(colors: dict[str, tuple[int, int, int, int]]) -> Image.Image:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(MASK):
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), colors[key])
    return image


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, stone in STONES.items():
        render({**BEZEL, **stone}).save(OUT / f"{name}.png")
        print(f"wrote {name}.png")
    render_socket()


# The Mastery tree's Jewel Socket icon: an empty gold setting, drawn at the tree's 16 px icon size.
SOCKET_MASK = (
    "................",
    ".....OOOOOO.....",
    "...OOBBBBBBOO...",
    "..OBBb....bBBO..",
    "..OBb......bBO..",
    ".OBb........bBO.",
    ".OB..........BO.",
    ".OB..........BO.",
    ".OB..........BO.",
    ".OB..........BO.",
    ".ObB........BbO.",
    "..ObB......BbO..",
    "..ObbB....BbbO..",
    "...OObbbbbbOO...",
    ".....OOOOOO.....",
    "................",
)
SOCKET_OUT = ROOT / "src/main/resources/assets/rngtech/textures/gui/mastery/jewel_socket.png"


def render_socket() -> None:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(SOCKET_MASK):
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), BEZEL[key])
    image.save(SOCKET_OUT)
    print("wrote gui/mastery/jewel_socket.png")


if __name__ == "__main__":
    render_socket()
