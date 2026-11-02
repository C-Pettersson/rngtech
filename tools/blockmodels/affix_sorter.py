#!/usr/bin/env python3
"""Generate the Affix Sorter blockstate, block models, item model, owned textures, and its Gear and card sprites.

The Affix Sorter is an Exotic sorting gate on the slate plinth. Identified items drop through the naquadah-rimmed
hopper on the roof into the Exotic housing and come out onto a reading deck at the front, where a reader bridge reads
their affixes; its reader strip lights pale teal while the sorter works. Two chutes at the front corners carry the
items away: the Pass chute on the west, its mouth collared in dull green enamel, and the Reject chute on the east,
collared in dull red. A card slot on the east flank takes the Rule Card, the status lamp sits on the bridge, and the
cable port is on the back. Run from the repository root:

    python tools/blockmodels/affix_sorter.py

It also writes the Exotic Rule Core part sprite and the Rule Card sprite with their item models.
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block/affix_sorter"
ITEM_TEXTURES = ASSETS / "textures/item"
TEX = "rngtech:block/affix_sorter/"
CRUSHER = "rngtech:block/crusher/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
GLOW = {"neoforge_data": {"block_light": 15, "sky_light": 15}}
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}

# ---------------------------------------------------------------- geometry
# The front faces north (z = 0); facing the front, east (+x) is on the left.
HOUSING = ((2.0, 2.0, 4.0), (14.0, 12.0, 15.0))
BAND_Y = (10.0, 10.5)
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Feed hopper on the roof, open on top, with the naquadah rim round its mouth.
HOPPER = ((5.0, 12.0, 6.0), (11.0, 14.5, 12.0))
HOPPER_WALL = 0.5
# Reading deck along the front, and the reader bridge across it.
DECK = ((4.5, 2.0, 1.0), (11.5, 4.5, 4.0))
POSTS = (((4.5, 4.5, 1.5), (5.5, 9.0, 3.5)), ((10.5, 4.5, 1.5), (11.5, 9.0, 3.5)))
BRIDGE = ((4.25, 9.0, 1.0), (11.75, 10.5, 4.0))
READER = ((5.5, 8.9, 1.75), (10.5, 9.0, 3.25))
# The reader's window on the bridge front, which lights with the strip below, and the status lamp beside it.
WINDOW = ((6.25, 9.4, 0.9), (11.0, 10.1, 1.0))
STATUS_LAMP = ((4.75, 9.4, 0.75), (5.75, 10.1, 1.0))
# Chutes at the front corners: Pass on the west (low x), Reject on the east.
CHUTES = {"pass": (0.75, 4.0), "reject": (12.0, 15.25)}
CHUTE_Y = (2.0, 5.5)
CHUTE_Z = (0.75, 4.0)
CHUTE_WALL = 0.5
COLLAR_DEPTH = 0.5
# Rule Card slot on the east flank.
CARD_SLOT = ((14.0, 5.5, 7.0), (14.25, 7.0, 12.0))


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, uv=None, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map with an optional "*" default."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is None:
            continue
        entry = {"texture": f"#{key}"}
        if uv and face in uv:
            entry["uv"] = [r(v) for v in uv[face]]
        element["faces"][face] = entry
    if extra:
        element.update(extra)
    return element


def housing_elements():
    (x0, y0, z0), (x1, y1, z1) = HOUSING
    return [
        box(HOUSING[0], HOUSING[1], "shell", faces=SIDES + ("up",)),
        box((x0 - 0.1, BAND_Y[0], z0 - 0.1), (x1 + 0.1, BAND_Y[1], z1 + 0.1), "band", faces=SIDES),
        box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"),
            uv={"south": [0, 0, 16, 16]}),
        box(*CARD_SLOT, "card_slot", faces=("east", "north", "south", "up", "down"), uv={"east": [0, 0, 16, 16]}),
    ]


def hopper_elements(item: bool):
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HOPPER
    w = HOPPER_WALL
    walls = {"*": "trim"}
    elements = [
        box((hx0, hy0, hz0), (hx1, hy1, hz0 + w), {"south": "liner", **walls}),
        box((hx0, hy0, hz1 - w), (hx1, hy1, hz1), {"north": "liner", **walls}),
        box((hx0, hy0, hz0 + w), (hx0 + w, hy1, hz1 - w), {"east": "liner", **walls}, faces=("east", "west", "up")),
        box((hx1 - w, hy0, hz0 + w), (hx1, hy1, hz1 - w), {"west": "liner", **walls}, faces=("east", "west", "up")),
        box((hx0 - 0.1, hy1 - 1.0, hz0 - 0.1), (hx1 + 0.1, hy1 - 0.25, hz1 + 0.1), "rim", faces=SIDES),
    ]
    if not item:
        elements.append(box((hx0 + w, hy0, hz0 + w), (hx1 - w, hy0 + 0.05, hz1 - w), "throat", faces=("up",),
                            uv={"up": [0, 0, 16, 16]}))
    return elements


def reader_elements(active: bool):
    elements = [
        box(*DECK, {"up": "deck", "*": "shell"}, faces=("north", "east", "west", "up"), uv={"up": [0, 0, 16, 16]}),
    ]
    for post in POSTS:
        elements.append(box(*post, "trim", faces=SIDES))
    reader = "reader_on" if active else "reader_off"
    glow = GLOW if active else None
    elements.append(box(*BRIDGE, "trim"))
    elements.append(box(*READER, reader, faces=("down",), uv={"down": [0, 0, 16, 16]}, extra=glow))
    elements.append(box(*WINDOW, reader, faces=("north", "east", "west", "up", "down"),
                        uv={"north": [0, 6, 16, 10]}, extra=glow))
    elements.append(box(*STATUS_LAMP, "lamp", faces=("north", "east", "west", "up", "down"), extra=GLOW))
    return elements


def chute_elements(name: str, item: bool):
    """A square chute out of the housing's front corner, open at the front, with an enamel collar round its mouth."""
    x0, x1 = CHUTES[name]
    y0, y1 = CHUTE_Y
    z0, z1 = CHUTE_Z
    w = CHUTE_WALL
    collar = f"collar_{name}"
    elements = [
        box((x0, y0, z0), (x1, y0 + w, z1), {"up": "liner", "*": "trim"}, faces=("north", "east", "west", "up")),
        box((x0, y1 - w, z0), (x1, y1, z1), {"down": "liner", "*": "trim"}, faces=("north", "east", "west", "up", "down")),
        box((x0, y0 + w, z0), (x0 + w, y1 - w, z1), {"east": "liner", "*": "trim"}, faces=("north", "east", "west")),
        box((x1 - w, y0 + w, z0), (x1, y1 - w, z1), {"west": "liner", "*": "trim"}, faces=("north", "east", "west")),
    ]
    # The collar: a ring of enamel round the mouth, leaving the opening clear.
    g, d = 0.1, COLLAR_DEPTH
    ring = ("north", "east", "west", "up", "down")
    elements += [
        box((x0 - g, y1 - w, z0 - g), (x1 + g, y1 + g, z0 + d), collar, faces=ring),
        box((x0 - g, y0 - 0.05, z0 - g), (x1 + g, y0 + w, z0 + d), collar, faces=ring),
        box((x0 - g, y0 + w, z0 - g), (x0 + w, y1 - w, z0 + d), collar, faces=ring),
        box((x1 - w, y0 + w, z0 - g), (x1 + g, y1 - w, z0 + d), collar, faces=ring),
    ]
    if not item:
        elements.append(box((x0 + w, y0 + w, z1 - 0.05), (x1 - w, y1 - w, z1), "throat", faces=("north",),
                            uv={"north": [0, 0, 16, 16]}))
    return elements


def elements(active: bool, item: bool = False):
    out = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    out += housing_elements()
    out += hopper_elements(item)
    out += reader_elements(active)
    out += chute_elements("pass", item)
    out += chute_elements("reject", item)
    return out


TEXTURE_MAP = {
    "particle": CRUSHER + "shell/exotic",
    "shell": CRUSHER + "shell/exotic",
    "trim": CRUSHER + "trim/exotic",
    "rim": CRUSHER + "rim/naquadah",
    "casing": CRUSHER + "casing",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "liner": CRUSHER + "liner",
    "throat": CRUSHER + "throat",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "lamp": CRUSHER + "lamp_off",
    "deck": TEX + "deck",
    "reader_off": TEX + "reader_off",
    "reader_on": TEX + "reader_on",
    "collar_pass": TEX + "collar_pass",
    "collar_reject": TEX + "collar_reject",
    "card_slot": TEX + "card_slot",
}


def textures(active: bool):
    shown = dict(TEXTURE_MAP)
    shown.pop("reader_off" if active else "reader_on")
    if active:
        shown["lamp"] = CRUSHER + "lamp_on"
    return shown


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    for active, name in ((False, "affix_sorter"), (True, "affix_sorter_active")):
        write_json(MODELS / f"block/{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": textures(active),
            "elements": elements(active),
        })
    # The item shows the lit reader and leaves out the hopper floor and the chutes' back walls, which a wiki icon
    # without a depth buffer would draw over the parts in front of them.
    item_textures = textures(True)
    item_textures["lamp"] = CRUSHER + "lamp_off"
    write_json(MODELS / "item/affix_sorter.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": item_textures,
        "elements": elements(True, item=True),
    })
    write_json(MODELS / "item/exotic_rule_core.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "rngtech:item/parts/rule_core/exotic"},
    })
    write_json(MODELS / "item/rule_card.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "rngtech:item/rule_card"},
    })


def write_blockstate() -> None:
    variants = {}
    for active, model in (("false", "rngtech:block/affix_sorter"), ("true", "rngtech:block/affix_sorter_active")):
        for facing, angle in ROTATIONS.items():
            apply = {"model": model}
            if angle:
                apply["y"] = angle
            variants[f"active={active},facing={facing}"] = apply
    write_json(ASSETS / "blockstates/affix_sorter.json", {"variants": variants})


# ---------------------------------------------------------------- block textures

def rng(name: str) -> random.Random:
    return random.Random(f"affix_sorter:{name}")


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noisy(size, color, name, amount=6, streak=0):
    rand = rng(name)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    rows = [rand.uniform(-streak, streak) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            pixels[x, y] = (*shade(color, rand.uniform(-amount, amount) + rows[y]), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


# The Exotic chassis palette (crusher.py), the RNGTech teal, and the reader's pale teal after the Identifying Fluid.
EXOTIC_SHELL = (50, 42, 62)
EXOTIC_TRIM = (76, 66, 92)
EXOTIC_GLOW = (170, 96, 222)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
READ_GLOW = (0xB8, 0xF2, 0xE6)
# Dull enamels, so the chutes read apart without looking like signal lights.
PASS_ENAMEL = (58, 98, 74)
REJECT_ENAMEL = (110, 56, 56)


def deck_texture():
    """The reading deck: a dark slat belt between trim rails, with a teal reading line across its middle."""
    image = noisy(64, (28, 24, 34), "deck", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if x < 4 or x > 59:
                pixels[x, y] = (*shade(EXOTIC_TRIM, 8 if x in (0, 60) else -6), 255)
            elif y % 6 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -10), 255)
            elif y % 6 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    for x in range(6, 58):
        for y in (31, 32):
            pixels[x, y] = (*mix(pixels[x, y], TEAL_DARK, 0.7), 255)
    return image


def reader_texture(lit: bool):
    """The reader strip under the bridge: a glass bar in a bezel, pale teal and bright when lit."""
    core = READ_GLOW if lit else (26, 46, 54)
    image = noisy(64, core, "reader_on" if lit else "reader_off", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if min(x, y, 63 - x, 63 - y) < 4:
                pixels[x, y] = (*shade(EXOTIC_TRIM, -14), 255)
            elif lit and 26 < y < 37:
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
            elif not lit and y == 10:
                pixels[x, y] = (*shade(pixels[x, y], 24), 255)
    return image


def collar_texture(name, enamel):
    """A chute collar mapped by position: dull enamel with a darker lip line, worn at the edges."""
    image = noisy(64, enamel, f"collar_{name}", amount=5, streak=2)
    pixels = image.load()
    rand = rng(f"collar_{name}_wear")
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(enamel, -30), 255)
            elif rand.random() < 0.05:
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_TRIM, 0.6), 255)
    return image


def card_slot_texture():
    """The Rule Card slot: a dark slit in a trim plate, with a small teal contact strip beside it."""
    image = noisy(64, shade(EXOTIC_TRIM, -6), "card_slot", amount=4)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(EXOTIC_TRIM, -28), 255), width=2)
    draw.rectangle([8, 26, 55, 37], fill=(10, 8, 14, 255), outline=(*shade(EXOTIC_TRIM, 28), 255))
    draw.rectangle([10, 30, 53, 33], fill=(4, 4, 6, 255))
    for x in range(10, 54, 4):
        draw.point((x, 44), fill=(*TEAL, 255))
        draw.point((x + 1, 44), fill=(*TEAL_DARK, 255))
    return image


def write_textures() -> None:
    save(deck_texture(), "deck")
    save(reader_texture(False), "reader_off")
    save(reader_texture(True), "reader_on")
    save(collar_texture("pass", PASS_ENAMEL), "collar_pass")
    save(collar_texture("reject", REJECT_ENAMEL), "collar_reject")
    save(card_slot_texture(), "card_slot")


# ---------------------------------------------------------------- item sprites
# 16x16 sprites in the Exotic part palette (textures/item/parts/*/exotic.png).

PART_OUTLINE = (0x22, 0x1D, 0x21)
PART_BODY = (0x4C, 0x41, 0x49)
PART_ACCENT = (0x8B, 0x53, 0xAD)
PART_BRIGHT = (0xBF, 0x62, 0xFF)
PART_CYAN = (0x52, 0xA6, 0xD2)
PART_CYAN_BRIGHT = (0xB8, 0xF2, 0xE6)


def sprite(rows, palette):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), "sprites are 16x16"
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), (*palette[key], 255))
    return image


def rule_core_sprite():
    """A dense logic core: a square chip in the Exotic housing with pins on every side and a crystal lattice die."""
    rows = [
        "................",
        "....o.o..o.o....",
        "....p.p..p.p....",
        "..oooooooooooo..",
        "..obbbbbbbbbbo..",
        "oporbaaaaaabroqo",
        "...obacBcaCabo..",
        "..pobaBcCcBabo..",
        "..pobacCLcCabop.",
        "...obaCcBcLabo..",
        "oporbaaaaaabroqo",
        "..obbbbbbbbbbo..",
        "..oooooooooooo..",
        "....p.p..p.p....",
        "....o.o..o.o....",
        "................",
    ]
    palette = {
        "o": PART_OUTLINE,
        "b": PART_BODY,
        "r": shade(PART_BODY, 22),
        "a": PART_ACCENT,
        "p": shade(PART_BODY, 30),
        "q": shade(PART_BODY, 30),
        "c": (0x2A, 0x24, 0x36),
        "B": PART_BRIGHT,
        "C": PART_CYAN,
        "L": PART_CYAN_BRIGHT,
    }
    return sprite(rows, palette)


def rule_card_sprite():
    """A small programmable card: a dark Exotic card with a teal label strip, rule lines, a purple chip, and gold contacts."""
    rows = [
        "................",
        "................",
        "...oooooooooo...",
        "..obbbbbbbbbbo..",
        "..obttttttttbo..",
        "..obbbbbbbbbbo..",
        "..obllllllbbbo..",
        "..obbbbbbbbbbo..",
        "..oblllllbbbbo..",
        "..obbbbbbbbbbo..",
        "..obllllbbapbo..",
        "..obbbbbbbppbo..",
        "..obgbgbgbbbbo..",
        "..oddddddddddo..",
        "...oooooooooo...",
        "................",
    ]
    palette = {
        "o": PART_OUTLINE,
        "b": PART_BODY,
        "d": shade(PART_BODY, -14),
        "t": (0x2F, 0xB3, 0xC2),
        "l": shade(PART_BODY, 26),
        "a": PART_ACCENT,
        "p": PART_BRIGHT,
        "g": (0xD8, 0xB0, 0x4C),
    }
    return sprite(rows, palette)


def write_item_textures() -> None:
    path = ITEM_TEXTURES / "parts/rule_core/exotic.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    rule_core_sprite().save(path)
    rule_card_sprite().save(ITEM_TEXTURES / "rule_card.png")


def main() -> None:
    write_models()
    write_blockstate()
    write_textures()
    write_item_textures()


if __name__ == "__main__":
    main()
