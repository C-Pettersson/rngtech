#!/usr/bin/env python3
"""Generate the Affix Sorter blockstate, block models, item model, owned textures, and its Gear and card sprites.

The Affix Sorter is an Exotic sorting cabinet on the slate plinth, with the feed hatch on its naquadah-rimmed roof, the
cable port on its back, and a Rule Card slot on its east flank. Its front carries the working parts. Up top is the
reader bay: a framed window between two emitter posts, whose teal slits light while the sorter works, in front of a
dark reader plate. The bay stands on an outlet deck whose face holds the two outlet mouths, Pass on the left (east) and Reject on the right
(west) as the default faces route them, each with its route lamp on the deck top behind it. Between the mouths a diverter flap hangs from
a hinge over an etched fork. The status lamp sits beside the bay.

AffixSorterRenderer draws the moving parts and keeps its constants in step with the geometry here: the item being read
in the bay with the scan sheet sweeping over it, the flap swinging toward the route an item takes, and that route's
lamp flashing. The item model carries the flap at rest. Run from the repository root:

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
HOUSING = ((2.0, 2.0, 7.0), (14.0, 13.0, 15.0))
BAND_Y = (11.25, 11.75)
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
CARD_SLOT = ((14.0, 6.0, 8.5), (14.25, 7.5, 13.5))
STATUS_LAMP = ((12.75, 12.0, 6.75), (13.75, 12.75, 7.0))
# Feed hatch on the roof, open on top.
HATCH = ((5.0, 13.0, 8.5), (11.0, 14.5, 13.5))
HATCH_WALL = 0.5
# The outlet deck across the front, with an outlet mouth at each end of its face: Pass on the left (east), Reject on
# the right (west). Each route's lamp sits on the deck top behind its mouth.
DECK = ((2.0, 2.0, 2.0), (14.0, 6.5, 7.0))
MOUTHS = {"pass": (9.5, 13.25), "reject": (2.75, 6.5)}
MOUTH_Y = (2.5, 5.75)
COLLAR = 0.5
COLLAR_Z = 1.25
ROUTE_LAMP = (6.5, 7.1, 2.3, 3.3)
ROUTE_LAMP_WIDTH = 1.25
# Reader bay standing on the deck against the cabinet: a sill and a hooded header between two emitter posts, the
# reader plate at the back. The item being read hangs in the bay.
BAY_X = (3.5, 12.5)
BAY_Y = (6.5, 12.5)
BAY_Z = (4.5, 7.0)
SILL = 0.75
HEADER = 0.75
HOOD_Z = 4.25
POST = 1.25
READER = ((4.75, 7.25, 6.9), (11.25, 11.5, 7.0))
SLIT_Y = (7.75, 11.0)
SLIT_Z = (5.0, 6.0)
# The diverter between the mouths: a hinge boss over an etched fork, and the flap hanging from it. The renderer
# swings the flap about its hinge pin at (8, FLAP_PIVOT_Y); the item model shows it hanging straight down.
FORK = ((6.75, 2.25, 1.95), (9.25, 6.25, 2.0))
HINGE = ((7.4, 5.5, 0.3), (8.6, 6.5, 2.0))
FLAP_PIVOT_Y = 6.0
FLAP = ((7.5, 2.75, 0.4), (8.5, 6.0, 0.8))
FLAP_UV = [0, 0, 4, 13]
LAMP_FACES = ("north", "east", "west", "up", "down")


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
        box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}),
        box(HOUSING[0], HOUSING[1], "shell", faces=SIDES + ("up",)),
        box((x0 - 0.1, BAND_Y[0], z0 - 0.1), (x1 + 0.1, BAND_Y[1], z1 + 0.1), "band", faces=SIDES),
        box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"),
            uv={"south": [0, 0, 16, 16]}),
        box(*CARD_SLOT, "card_slot", faces=("east", "north", "south", "up", "down"), uv={"east": [0, 0, 16, 16]}),
        box(*STATUS_LAMP, "lamp", faces=LAMP_FACES, extra=GLOW),
    ]


def hatch_elements(item: bool):
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HATCH
    w = HATCH_WALL
    walls = {"up": "trim", "*": "rim"}
    elements = [
        box((hx0, hy0, hz0), (hx1, hy1, hz0 + w), {"south": "liner", **walls}),
        box((hx0, hy0, hz1 - w), (hx1, hy1, hz1), {"north": "liner", **walls}),
        box((hx0, hy0, hz0 + w), (hx0 + w, hy1, hz1 - w), {"east": "liner", **walls}, faces=("east", "west", "up")),
        box((hx1 - w, hy0, hz0 + w), (hx1, hy1, hz1 - w), {"west": "liner", **walls}, faces=("east", "west", "up")),
    ]
    if not item:
        elements.append(box((hx0 + w, hy0, hz0 + w), (hx1 - w, hy0 + 0.05, hz1 - w), "throat", faces=("up",),
                            uv={"up": [0, 0, 16, 16]}))
    return elements


def bay_elements(active: bool):
    (bx0, bx1), (by0, by1), (bz0, bz1) = BAY_X, BAY_Y, BAY_Z
    scan = "scan_on" if active else "scan_off"
    glow = GLOW if active else None
    reader = "reader_on" if active else "reader_off"
    return [
        box((bx0, by0, bz0), (bx1, by0 + SILL, bz1), "trim", faces=("north", "east", "west", "up")),
        box((bx0 - 0.25, by1 - HEADER, HOOD_Z), (bx1 + 0.25, by1, bz1), "trim"),
        box((bx0, by0 + SILL, bz0), (bx0 + POST, by1 - HEADER, bz1), "emitter", faces=("north", "east", "west")),
        box((bx1 - POST, by0 + SILL, bz0), (bx1, by1 - HEADER, bz1), "emitter", faces=("north", "east", "west")),
        # The emitters' slits on the posts' inner faces, which light while the sorter works.
        box((bx0 + POST, SLIT_Y[0], SLIT_Z[0]), (bx0 + POST + 0.1, SLIT_Y[1], SLIT_Z[1]), scan, faces=("east",),
            extra=glow),
        box((bx1 - POST - 0.1, SLIT_Y[0], SLIT_Z[0]), (bx1 - POST, SLIT_Y[1], SLIT_Z[1]), scan, faces=("west",),
            extra=glow),
        box(*READER, reader, faces=("north",), uv={"north": [0, 0, 16, 16]}, extra=glow),
    ]


def mouth_elements(name: str):
    """An outlet mouth in the deck face: a trim collar standing proud round a dark chute, its route lamp behind."""
    x0, x1 = MOUTHS[name]
    y0, y1 = MOUTH_Y
    w, z0, z1 = COLLAR, COLLAR_Z, DECK[0][2]
    ring = ("north", "east", "west", "up", "down")
    cx = (x0 + x1) / 2
    ly0, ly1, lz0, lz1 = ROUTE_LAMP
    half = ROUTE_LAMP_WIDTH / 2
    return [
        box((x0, y1 - w, z0), (x1, y1, z1), "trim", faces=ring),
        box((x0, y0, z0), (x1, y0 + w, z1), "trim", faces=ring),
        box((x0, y0 + w, z0), (x0 + w, y1 - w, z1), "trim", faces=("north", "east", "west")),
        box((x1 - w, y0 + w, z0), (x1, y1 - w, z1), "trim", faces=("north", "east", "west")),
        box((x0 + w, y0 + w, z1 - 0.05), (x1 - w, y1 - w, z1), "mouth", faces=("north",),
            uv={"north": [0, 0, 16, 16]}),
        box((cx - half, ly0, lz0), (cx + half, ly1, lz1), f"lamp_{name}", faces=LAMP_FACES),
    ]


def deck_elements(item: bool):
    elements = [box(*DECK, {"up": "trim", "*": "shell"}, faces=("north", "east", "west", "up"))]
    elements += mouth_elements("pass")
    elements += mouth_elements("reject")
    elements.append(box(*FORK, "fork", faces=("north",), uv={"north": [0, 0, 16, 16]}))
    elements.append(box(*HINGE, "trim", faces=("north", "east", "west", "up", "down")))
    if item:
        elements.append(box(*FLAP, "flap", faces=("north", "east", "west", "down"),
                            uv={"north": FLAP_UV, "east": [0, 0, 2, 13], "west": [0, 0, 2, 13], "down": [0, 0, 4, 2]}))
    return elements


def elements(active: bool, item: bool = False):
    out = housing_elements()
    out += hatch_elements(item)
    out += bay_elements(active)
    out += deck_elements(item)
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
    "card_slot": TEX + "card_slot",
    "emitter": TEX + "emitter",
    "scan_off": TEX + "scan_off",
    "scan_on": TEX + "scan_on",
    "reader_off": TEX + "reader_off",
    "reader_on": TEX + "reader_on",
    "mouth": TEX + "mouth",
    "fork": TEX + "fork",
    "lamp_pass": TEX + "lamp_pass",
    "lamp_reject": TEX + "lamp_reject",
}


def textures(active: bool):
    shown = dict(TEXTURE_MAP)
    for key in (("scan_off", "reader_off") if active else ("scan_on", "reader_on")):
        shown.pop(key)
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
    # The item shows the lit reader and the flap at rest, and leaves out the hatch floor, which a wiki icon without a
    # depth buffer would draw over the parts in front of it.
    item_textures = textures(True)
    item_textures["lamp"] = CRUSHER + "lamp_off"
    item_textures["flap"] = TEX + "flap"
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


# The Exotic chassis palette (crusher.py), the reader's teal after the Identifying Fluid, and the route colours of the
# Process tab.
EXOTIC_SHELL = (50, 42, 62)
EXOTIC_TRIM = (76, 66, 92)
EXOTIC_GLOW = (170, 96, 222)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
READ_GLOW = (0xB8, 0xF2, 0xE6)
GLASS_DARK = (20, 18, 26)
PASS = (0x4E, 0x8A, 0x4A)
REJECT = (0xA6, 0x47, 0x3E)


def reader_texture(lit: bool):
    """The reader plate behind the bay: dark glass etched with a teal grid and a reticle, brighter when lit."""
    line = TEAL if lit else TEAL_DARK
    image = noisy(64, GLASS_DARK if not lit else (18, 34, 40), "reader_on" if lit else "reader_off", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if min(x, y, 63 - x, 63 - y) < 3:
                pixels[x, y] = (*shade(EXOTIC_TRIM, -14), 255)
            elif (x - 3) % 10 == 0 or (y - 3) % 10 == 0:
                pixels[x, y] = (*mix(pixels[x, y], line, 0.45), 255)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 32)
            if 13.0 < d < 14.5 or (d < 20 and (x in (31, 32) or y in (31, 32)) and d > 5):
                pixels[x, y] = (*mix(pixels[x, y], READ_GLOW if lit else line, 0.8), 255)
    return image


def emitter_texture():
    """The emitter posts: Exotic trim with ribbed vents and a purple seam."""
    image = noisy(64, EXOTIC_TRIM, "emitter", amount=5, streak=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if x in (0, 63) or y in (0, 63):
                pixels[x, y] = (*shade(EXOTIC_TRIM, -26), 255)
            elif y % 8 in (3, 4) and 10 < x < 54:
                pixels[x, y] = (*shade(EXOTIC_SHELL, -18), 255)
            elif x == 8:
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.45), 255)
    return image


def scan_texture(lit: bool):
    """An emitter slit: dark teal glass, or a pale teal glow brightest along its middle."""
    color = READ_GLOW if lit else (26, 52, 60)
    image = noisy(64, color, "scan_on" if lit else "scan_off", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            edge = min(x, 63 - x) / 32
            pixels[x, y] = (*shade(pixels[x, y], (edge * 24 - 12) if lit else -edge * 6), 255)
    return image


def mouth_texture():
    """An outlet chute seen from the front: dark, shadowed under the lip, with a worn floor sloping out."""
    image = noisy(64, (24, 21, 30), "mouth", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if y < 14:
                pixels[x, y] = (*shade(pixels[x, y], -10 + y * 0.5), 255)
            elif y > 44:
                floor = shade(EXOTIC_SHELL, -6 + (y - 44) * 0.6)
                pixels[x, y] = (*shade(floor, -4 if (x + y) % 9 == 0 else 0), 255)
    return image


def fork_texture():
    """The plate behind the flap: dark Exotic steel etched with the forked chute it guards."""
    image = noisy(64, shade(EXOTIC_SHELL, -10), "fork", amount=3)
    draw = ImageDraw.Draw(image)
    groove = (*shade(EXOTIC_SHELL, -26), 255)
    edge = (*shade(EXOTIC_TRIM, 18), 255)
    draw.line([(32, 2), (32, 18)], fill=groove, width=7)
    draw.line([(32, 18), (6, 62)], fill=groove, width=7)
    draw.line([(32, 18), (58, 62)], fill=groove, width=7)
    draw.line([(36, 2), (36, 16)], fill=edge, width=1)
    draw.line([(36, 20), (61, 62)], fill=edge, width=1)
    draw.line([(28, 20), (3, 62)], fill=edge, width=1)
    for x, y in ((4, 4), (58, 4)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(*shade(EXOTIC_TRIM, 30), 255))
    return image


def flap_texture():
    """The diverter flap, drawn in 16-texel columns (four model pixels each): trim plate, purple seam, bright tip."""
    image = noisy(64, shade(EXOTIC_TRIM, 6), "flap", amount=4)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            column = x % 16
            if column in (0, 15) or y >= 58:
                pixels[x, y] = (*shade(EXOTIC_TRIM, -24 if y < 58 else 30), 255)
            elif column in (7, 8) and 6 < y < 54:
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.55), 255)
            elif y < 5:
                pixels[x, y] = (*shade(EXOTIC_TRIM, 22), 255)
    return image


def lamp_texture(color, name):
    """A route lamp at rest: dim coloured glass with a highlight in its upper corner."""
    base = mix((14, 12, 18), color, 0.55)
    image = noisy(64, base, name, amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if min(x, y, 63 - x, 63 - y) < 4:
                pixels[x, y] = (*shade(base, -22), 255)
            elif 8 < x < 22 and 8 < y < 18:
                pixels[x, y] = (*shade(base, 30), 255)
    return image


def card_slot_texture():
    """The Rule Card slot on the east flank: a dark slit in a trim bezel, with a teal ready light."""
    image = noisy(64, EXOTIC_TRIM, "card_slot", amount=4)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(EXOTIC_TRIM, -26), 255), width=3)
    draw.rectangle([8, 24, 55, 39], fill=(*GLASS_DARK, 255), outline=(*shade(EXOTIC_TRIM, 30), 255), width=2)
    draw.rectangle([12, 30, 51, 33], fill=(*shade(GLASS_DARK, -10), 255))
    draw.rectangle([56, 8, 59, 11], fill=(*TEAL, 255))
    return image


def write_textures() -> None:
    save(reader_texture(False), "reader_off")
    save(reader_texture(True), "reader_on")
    save(emitter_texture(), "emitter")
    save(scan_texture(False), "scan_off")
    save(scan_texture(True), "scan_on")
    save(mouth_texture(), "mouth")
    save(fork_texture(), "fork")
    save(flap_texture(), "flap")
    save(lamp_texture(PASS, "lamp_pass"), "lamp_pass")
    save(lamp_texture(REJECT, "lamp_reject"), "lamp_reject")
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
