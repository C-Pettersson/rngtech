#!/usr/bin/env python3
"""Generate the Affix Sorter blockstate, block models, item model, owned textures, and its Gear and card sprites.

The Affix Sorter is a squat Exotic sorting block on the slate plinth: a thick housing between a trim skirt and a heavy
lid with a teal band, and a square port on every face, since every face routes items. Each side port is a chunky
collar round a cable socket; the roof port's collar rings the reader, a glass floor that lights pale teal while the
sorter works, and the item being read lies on it. The bottom port is an inlay under the plinth. The status lamp sits
beside the front port. AffixSorterRenderer enamels each collar in the route its face is set to, green for Pass and
red for Reject, leaving the bare trim collar on input faces; it also draws the item on the reader. The ring positions
here match that renderer. Run from the repository root:

    python tools/blockmodels/affix_sorter.py

It also writes the Exotic Rule Core part sprite and the Rule Card sprite with their item models.
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image

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
PLINTH = ((0.0, 0.0, 0.0), (16.0, 2.0, 16.0))
SKIRT = ((1.0, 2.0, 1.0), (15.0, 3.0, 15.0))
BODY = ((1.5, 3.0, 1.5), (14.5, 12.5, 14.5))
LID = ((1.0, 12.5, 1.0), (15.0, 13.5, 15.0))
LID_BAND = ((0.9, 12.75, 0.9), (15.1, 13.25, 15.1))
STATUS_LAMP = ((12.75, 10.75, 1.25), (14.0, 12.0, 1.5))
# Every face has a port: a square collar ring round its mouth. A ring is (u0, v0, u1, v1) across the face and (d0, d1)
# inward from the face's outer plane; see oriented(). The side collars stand proud of the body, the roof collar stands
# on the lid round the reader, and the bottom inlay lies just under the plinth.
SIDE_RING = (3.5, 3.25, 12.5, 12.25)
SIDE_DEPTH = (0.5, 1.5)
TOP_RING = (3.5, 3.5, 12.5, 12.5)
TOP_DEPTH = (1.0, 2.5)
BOTTOM_RING = TOP_RING
BOTTOM_DEPTH = (-0.1, 0.0)
RING_WIDTH = 2.0
MOUTH_DEPTH = 0.1
# Relative faces for the north-facing model, as AffixSorterBlockEntity.RelativeFace maps them.
PORTS = {
    "front": ("north", SIDE_RING, SIDE_DEPTH),
    "back": ("south", SIDE_RING, SIDE_DEPTH),
    "left": ("east", SIDE_RING, SIDE_DEPTH),
    "right": ("west", SIDE_RING, SIDE_DEPTH),
    "top": ("up", TOP_RING, TOP_DEPTH),
    "bottom": ("down", BOTTOM_RING, BOTTOM_DEPTH),
}
# The default routes: Pass on the left, Reject on the right. The item model shows them enamelled.
ITEM_ROUTES = {"left": "pass", "right": "reject"}


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


def oriented(face, u0, v0, u1, v1, d0, d1):
    """A box given across a face (u, v) and inward from its outer plane (d), as model from/to corners."""
    if face == "north":
        a, b = (u0, v0, d0), (u1, v1, d1)
    elif face == "south":
        a, b = (u0, v0, 16 - d1), (u1, v1, 16 - d0)
    elif face == "east":
        a, b = (16 - d1, v0, u0), (16 - d0, v1, u1)
    elif face == "west":
        a, b = (d0, v0, u0), (d1, v1, u1)
    elif face == "up":
        a, b = (u0, 16 - d1, v0), (u1, 16 - d0, v1)
    else:
        a, b = (u0, d0, v0), (u1, d1, v1)
    return a, b


def ring_bars(ring, width):
    u0, v0, u1, v1 = ring
    return [
        (u0, v0, u1, v0 + width),
        (u0, v1 - width, u1, v1),
        (u0, v0 + width, u0 + width, v1 - width),
        (u1 - width, v0 + width, u1, v1 - width),
    ]


def port_elements(name, active: bool, item: bool):
    face, ring, (d0, d1) = PORTS[name]
    collar = f"enamel_{ITEM_ROUTES[name]}" if item and name in ITEM_ROUTES else "trim"
    out = []
    if face != "down":
        for bar in ring_bars(ring, RING_WIDTH):
            out.append(box(*oriented(face, *bar, d0, d1), collar))
    else:
        # The inlay under the plinth shows only from below.
        for bar in ring_bars(ring, RING_WIDTH):
            out.append(box(*oriented(face, *bar, d0, d1), collar, faces=("down",)))
    u0, v0, u1, v1 = ring
    w = RING_WIDTH
    mouth = (u0 + w, v0 + w, u1 - w, v1 - w)
    if face == "up":
        reader = "reader_on" if active else "reader_off"
        out.append(box(*oriented(face, *mouth, d1 - MOUTH_DEPTH, d1), reader, faces=("up",),
                       uv={"up": [0, 0, 16, 16]}, extra=GLOW if active else None))
    elif face == "down":
        out.append(box(*oriented(face, *mouth, d0, d1), "port", faces=("down",), uv={"down": [0, 0, 16, 16]}))
    else:
        out.append(box(*oriented(face, *mouth, d1 - MOUTH_DEPTH, d1), "port", faces=(face,),
                       uv={face: [0, 0, 16, 16]}))
    return out


def housing_elements(item: bool):
    (bx0, by0, bz0), (bx1, by1, bz1) = BODY
    out = [
        box(*PLINTH, {"up": "plinth_top", "down": "casing", "*": "plinth_side"}),
        box(*SKIRT, "trim", faces=SIDES + ("up",)),
    ]
    if item:
        # The body reaches the lid top and the lid keeps only its sides: a wiki icon has no depth buffer, and a lid
        # top drawn after the far side of the roof collar would cover it.
        out.append(box((bx0, by0, bz0), (bx1, LID[1][1], bz1), {"up": "trim", "*": "shell"}, faces=SIDES + ("up",)))
        out.append(box(*LID, "trim", faces=SIDES))
    else:
        out.append(box(*BODY, "shell", faces=SIDES))
        out.append(box(*LID, "trim"))
    out.append(box(*LID_BAND, "band", faces=SIDES))
    out.append(box(*STATUS_LAMP, "lamp", faces=("north", "east", "west", "up", "down"), extra=GLOW))
    return out


def elements(active: bool, item: bool = False):
    out = housing_elements(item)
    for name in PORTS:
        out += port_elements(name, active, item)
    return out


TEXTURE_MAP = {
    "particle": CRUSHER + "shell/exotic",
    "shell": CRUSHER + "shell/exotic",
    "trim": CRUSHER + "trim/exotic",
    "casing": CRUSHER + "casing",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "lamp": CRUSHER + "lamp_off",
    "reader_off": TEX + "reader_off",
    "reader_on": TEX + "reader_on",
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
    # The item shows the lit reader and the default routes enamelled on their collars.
    item_textures = textures(True)
    item_textures["lamp"] = CRUSHER + "lamp_off"
    for route in sorted(set(ITEM_ROUTES.values())):
        item_textures[f"enamel_{route}"] = TEX + f"enamel_{route}"
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


def multiply(a, b):
    return tuple(clamp(a[i] * b[i] / 255) for i in range(3))


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


# The Exotic chassis palette (crusher.py), and the reader's pale teal after the Identifying Fluid.
EXOTIC_TRIM = (76, 66, 92)
READ_GLOW = (0xB8, 0xF2, 0xE6)
# The route colours of the Process tab; the renderer tints the pale enamel with them.
PASS = (0x4E, 0x8A, 0x4A)
REJECT = (0xA6, 0x47, 0x3E)
ENAMEL = (236, 236, 232)


def reader_texture(lit: bool):
    """The reader in the roof port: a glass floor in a bezel with a cross-hair, pale teal and bright when lit."""
    core = READ_GLOW if lit else (26, 46, 54)
    image = noisy(64, core, "reader_on" if lit else "reader_off", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            edge = min(x, y, 63 - x, 63 - y)
            if edge < 4:
                pixels[x, y] = (*shade(EXOTIC_TRIM, -14), 255)
            elif x in (31, 32) or y in (31, 32):
                pixels[x, y] = (*shade(pixels[x, y], 22 if lit else 14), 255)
    return image


def enamel_texture():
    """Pale, slightly worn enamel with a darker lip every 16 texels, so collar edges read at any size."""
    image = noisy(64, ENAMEL, "enamel", amount=4, streak=2)
    pixels = image.load()
    rand = rng("enamel_wear")
    for y in range(64):
        for x in range(64):
            if x % 16 in (0, 15) or y % 16 in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -34), 255)
            elif rand.random() < 0.04:
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
    return image


def tinted(image, color):
    out = image.copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            pixels[x, y] = (*multiply(pixels[x, y], color), 255)
    return out


def write_textures() -> None:
    save(reader_texture(False), "reader_off")
    save(reader_texture(True), "reader_on")
    enamel = enamel_texture()
    save(enamel, "enamel")
    save(tinted(enamel, PASS), "enamel_pass")
    save(tinted(enamel, REJECT), "enamel_reject")


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
