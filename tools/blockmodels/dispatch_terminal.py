#!/usr/bin/env python3
"""Generate the Dispatch Terminal block model, listing lamp parts, item model, and owned textures.

The Dispatch Terminal is a job console after the steel plates and circuits in its recipe: a slate cabinet with a teal
band, a desk deck with a keyboard, and a monitor whose screen shows the job listing in teal text. A row of six status
lamps along the desk's front edge lights one lamp for each job still listed, so the terminal shows at a glance how many
jobs are left; the lit lamps are blockstate parts keyed on the `listings` property. Run from the repository root:

    python tools/blockmodels/dispatch_terminal.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block/dispatch_terminal"
TEX = "rngtech:block/dispatch_terminal/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
LISTINGS = 6

# ---------------------------------------------------------------- geometry
# The front faces north (z = 0); facing the front, east (+x) is on the left.
CABINET = ((1.0, 0.0, 3.0), (15.0, 7.0, 15.0))
BAND = ((0.9, 5.5, 2.9), (15.1, 6.0, 15.1))
DECK = ((1.0, 7.0, 1.0), (15.0, 8.0, 9.0))
KEYBOARD = ((3.0, 8.0, 2.5), (13.0, 8.5, 7.0))
MONITOR = ((2.0, 8.0, 9.0), (14.0, 15.0, 13.0))
SCREEN = ((3.0, 9.0, 8.75), (13.0, 14.0, 9.0))
STAND = ((6.0, 8.0, 13.0), (10.0, 12.0, 14.5))
CHIP_SLOT = ((5.0, 3.5, 2.75), (11.0, 4.25, 3.0))
# Six lamps along the desk's front edge, left to right as seen from the front, so x runs from high to low.
LAMP_X = tuple(12.5 - index * 1.8 for index in range(LISTINGS))
LAMP_Y = (7.25, 7.75)
LAMP_Z = (0.8, 1.0)
LAMP_W = 0.9


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES):
    """One model element. `textures` is a texture key or a {face: key} map with an optional "*" default."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is not None:
            element["faces"][face] = {"texture": f"#{key}"}
    return element


def lamp(index: int, texture: str):
    x = LAMP_X[index]
    return box((x - LAMP_W / 2, LAMP_Y[0], LAMP_Z[0]), (x + LAMP_W / 2, LAMP_Y[1], LAMP_Z[1]), texture,
               faces=("north", "up", "down", "east", "west"))


def housing_elements():
    return [
        box(*CABINET, "casing"),
        box(*BAND, "band", faces=SIDES),
        box(*DECK, {"up": "deck", "*": "casing"}),
        box(*KEYBOARD, {"up": "keyboard", "*": "casing"}, faces=("north", "up", "east", "west")),
        box(*MONITOR, {"north": "casing", "*": "casing"}),
        box(*SCREEN, "screen", faces=("north",)),
        box(*STAND, "casing", faces=SIDES),
        box(*CHIP_SLOT, "slot", faces=("north",)),
    ] + [lamp(index, "lamp_off") for index in range(LISTINGS)]


HOUSING = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "band": TEX + "band",
    "deck": TEX + "deck",
    "keyboard": TEX + "keyboard",
    "screen": TEX + "screen",
    "slot": TEX + "slot",
    "lamp_off": TEX + "lamp_off",
}
LAMP_TEXTURES = {"particle": TEX + "lamp_on", "lamp_on": TEX + "lamp_on"}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    write_json(MODELS / "block/dispatch_terminal.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": HOUSING, "elements": housing_elements(),
    })
    for index in range(LISTINGS):
        write_json(MODELS / f"block/dispatch_terminal/lamp_{index + 1}.json", {
            "render_type": "minecraft:cutout", "textures": LAMP_TEXTURES, "elements": [lamp(index, "lamp_on")],
        })
    elements = housing_elements()[:-LISTINGS] + [lamp(index, "lamp_on") for index in range(LISTINGS)]
    write_json(MODELS / "item/dispatch_terminal.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": HOUSING | LAMP_TEXTURES | {"particle": TEX + "casing"}, "elements": elements,
    })


def write_blockstate() -> None:
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    multipart = []
    for facing, angle in rotations.items():
        apply = {"model": "rngtech:block/dispatch_terminal"}
        if angle:
            apply["y"] = angle
        multipart.append({"when": {"facing": facing}, "apply": apply})
    for index in range(1, LISTINGS + 1):
        shown = "|".join(str(count) for count in range(index, LISTINGS + 1))
        for facing, angle in rotations.items():
            apply = {"model": f"rngtech:block/dispatch_terminal/lamp_{index}"}
            if angle:
                apply["y"] = angle
            multipart.append({"when": {"facing": facing, "listings": shown}, "apply": apply})
    write_json(ASSETS / "blockstates/dispatch_terminal.json", {"multipart": multipart})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"dispatch_terminal:{name}")


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def noisy(size, color, name, amount=6):
    rand = rng(name)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            pixels[x, y] = (*shade(color, rand.uniform(-amount, amount)), 255)
    return image


def panel_seams(image, step=16, depth=22):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            if x % step == 0 or y % step == 0:
                pixels[x, y] = (*shade(pixels[x, y], -depth), 255)
            elif x % step == 1 or y % step == 1:
                pixels[x, y] = (*shade(pixels[x, y], depth // 2), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
SCREEN_DARK = (12, 20, 24)
KEY = (64, 72, 84)


def write_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")
    save(noisy(64, SLATE_DARK, "deck", amount=4), "deck")

    band = noisy(64, TEAL, "band", amount=5)
    pixels = band.load()
    for x in range(64):
        for y in (0, 63):
            pixels[x, y] = (*TEAL_DARK, 255)
    save(band, "band")

    keyboard = noisy(64, SLATE_DARK, "keyboard", amount=3)
    pixels = keyboard.load()
    for y in range(64):
        for x in range(64):
            if x % 6 in (1, 2, 3, 4) and y % 6 in (1, 2, 3, 4):
                pixels[x, y] = (*shade(KEY, 8 if y % 6 == 1 else 0), 255)
    save(keyboard, "keyboard")

    screen = noisy(64, SCREEN_DARK, "screen", amount=2)
    pixels = screen.load()
    rand = rng("screen-text")
    for row in range(6):
        y = 6 + row * 9
        pixels_on = rand.randint(28, 52)
        x = 6
        while x < 6 + pixels_on:
            width = rand.randint(2, 5)
            for dx in range(width):
                for dy in range(2):
                    if x + dx < 58:
                        pixels[x + dx, y + dy] = (*shade(TEAL, rand.uniform(-24, 8)), 255)
            x += width + 2
        if row % 2 == 0:
            for dx in range(4):
                for dy in range(2):
                    pixels[54 + dx, y + dy] = (*shade(TEAL, 30), 255)
    for y in range(0, 64, 2):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], -4), 255)
    save(screen, "screen")

    slot = noisy(64, (8, 10, 12), "slot", amount=2)
    save(slot, "slot")
    save(noisy(64, (34, 40, 44), "lamp_off", amount=3), "lamp_off")
    lamp = noisy(64, (96, 232, 168), "lamp_on", amount=6)
    pixels = lamp.load()
    for y in range(0, 20):
        for x in range(0, 20):
            pixels[x, y] = (*shade((96, 232, 168), 30), 255)
    save(lamp, "lamp_on")


def main() -> None:
    write_models()
    write_blockstate()
    write_textures()


if __name__ == "__main__":
    main()
