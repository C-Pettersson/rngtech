#!/usr/bin/env python3
"""Generate the Battery Chassis block models, part models, item models, owned textures, and Battery Cell sprites.

Every metal chassis shares one generic open cell rack: a slate plinth, four corner posts that carry
the charge meter, and a lid with a status lamp on each side. The chassis metal shows only in the
wiring: the bus rails under the lid, the cell straps, and the edges of the cable port plates. The
cells stand in a grid on the plinth floor, laid out for the chassis's active cell slots.

The Wooden chassis is a Stage 0 exception: an open plank crate with a copper bus bar across the
top and a redstone lamp that lights while FE flows, in the materials of its recipe.

The housings are static block models. The cells are standalone part models in a canonical 6x6
footprint, 10 pixels tall; BatteryChassisRenderer scales them into the layout, draws the sockets,
straps, charge windows, meter, lamps, a dyed band, and the port plates on wired sides. Its constants and its
layout match this file. Item models bake a full set of matching cells. Run from the repository root:

    python tools/blockmodels/battery_chassis.py
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
BLOCKSTATES = ASSETS / "blockstates"
TEXTURES = ASSETS / "textures/block/battery_chassis"
CELL_SPRITES = ASSETS / "textures/item/battery_cells"
TEX = "rngtech:block/battery_chassis/"
MODEL = "rngtech:block/battery_chassis"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# The metal rack: plinth, corner posts, bus rails under a banded lid.
PLINTH_TOP = 2.0
POST = 2.0
RAIL_Y0, RAIL_Y1 = 12.75, 14.0
RAIL_IN, RAIL_OUT = 1.5, 0.5
BAND_Y = 14.5
LID_TOP = 16.0
# Charge meter: eight LED segments on the outward faces of each corner post.
LED_SEGMENTS = 8
LED_Y0 = 3.1875
LED_PITCH = 1.25
LED_HEIGHT = 0.875
LED_HALF = 0.5
# Status lamp lens in the middle of each lid side.
LAMP_X0, LAMP_X1 = 6.5, 9.5
LAMP_Y0, LAMP_Y1 = 14.875, 15.625
# Cell floor and sockets.
SOCKET_TOP = 2.25
RACK_INTERIOR = (2.0, 14.0, 2.0, 14.0)
STRAP_Y0, STRAP_Y1 = 12.25, 12.75
STRAP_HALF = 0.3
# Plate on a side with an FE neighbour, sized to the Universal Connector ring. The lid and plinth have a hatch
# recessed by the plate's depth, so the same plate sits flush on top and underneath.
PORT = ((4.0, 4.0, 0.0), (12.0, 12.0, 1.0))
HATCH = (4.0, 12.0)
PLATE_DEPTH = 1.0
# A contact wall closes a whole side, flush with the posts, in front of the bus rail.
CONTACT_DEPTH = 0.5

# The Wooden crate.
CRATE_FLOOR = 1.0
CRATE_WALL = 1.0
CRATE_TOP = 8.0
CRATE_INTERIOR = (2.0, 14.0, 2.0, 14.0)
CRATE_CELL_SCALE = 0.85
CRATE_BAR = ((0.0, 9.5, 7.25), (16.0, 10.5, 8.75))
# Redstone lamp on the front rim; BatteryChassisRenderer lights it while FE flows in or out.
CRATE_LAMP = ((10.5, 8.0, 0.25), (12.5, 9.25, 1.75))

# Canonical cell: a rounded can of two crossed slabs, centred on (8, 8), 10 pixels tall.
CELL_R, CELL_F = 3.0, 2.0
CELL_WIDTH = 2 * CELL_R
CELL_HEIGHT = 10.0
CELL_MAX_DIAMETER = 6.0
CELL_PITCH_SHARE = 0.78
WINDOW_HALF = 0.75
WINDOW_Y0, WINDOW_Y1 = 2.5, 7.0

# Chassis metal for the wiring, from the chassis material.
CHASSIS = {
    "wooden": "copper",
    "iron": "iron",
    "copper": "copper",
    "gold": "gold",
    "steel": "steel",
    "sparksteel": "sparksteel",
    "arclite": "arclite",
    "nullite": "nullite",
    "aethergold": "aethergold",
    "exotic": "naquadah",
}
# Base cell slots and the cell the item model shows, matching each chassis stage.
CHASSIS_SLOTS = {
    "wooden": 1, "iron": 1, "copper": 2, "gold": 3, "steel": 4, "sparksteel": 5,
    "arclite": 6, "nullite": 8, "aethergold": 6, "exotic": 10,
}
ITEM_CELLS = {
    "wooden": "potato", "iron": "iron", "copper": "copper", "gold": "lead", "steel": "invar",
    "sparksteel": "sparksteel", "arclite": "arclite", "nullite": "nullite", "aethergold": "aethergold", "exotic": "exotic",
}
# Wiring metal: base and highlight.
METALS = {
    "iron": ((183, 183, 173), (242, 242, 232)),
    "copper": ((206, 116, 58), (255, 190, 136)),
    "gold": ((234, 186, 48), (255, 240, 122)),
    "steel": ((116, 123, 125), (203, 208, 207)),
    "sparksteel": ((77, 135, 199), (184, 230, 255)),
    "arclite": ((222, 232, 238), (255, 195, 226)),
    "nullite": ((70, 46, 104), (150, 112, 210)),
    "aethergold": ((215, 167, 59), (255, 240, 164)),
    "naquadah": ((45, 140, 116), (109, 255, 207)),
}
# Cells: body, cap, accent rings, and charge glow. The keys are the BatteryCellMaterial names.
CELLS = {
    "iron": {"body": (176, 176, 168), "cap": (74, 76, 84), "accent": (232, 232, 222), "glow": (96, 176, 255)},
    "copper": {"body": (200, 112, 56), "cap": (92, 52, 34), "accent": (255, 186, 128), "glow": (255, 170, 84)},
    "lead": {"body": (84, 94, 118), "cap": (42, 46, 58), "accent": (135, 146, 170), "glow": (150, 186, 255)},
    "invar": {"body": (146, 156, 140), "cap": (76, 84, 78), "accent": (214, 223, 204), "glow": (118, 236, 222)},
    "sparksteel": {"body": (72, 128, 192), "cap": (44, 56, 76), "accent": (184, 230, 255), "glow": (120, 200, 255)},
    "arclite": {"body": (228, 236, 242), "cap": (112, 122, 140), "accent": (255, 176, 216), "glow": (137, 200, 255)},
    "nullite": {"body": (40, 28, 58), "cap": (82, 62, 118), "accent": (140, 104, 196), "glow": (172, 104, 255)},
    "aethergold": {"body": (212, 164, 58), "cap": (104, 78, 30), "accent": (255, 240, 164), "glow": (255, 214, 100)},
    "exotic": {"body": (42, 134, 112), "cap": (28, 56, 50), "accent": (109, 255, 207), "glow": (70, 255, 182)},
}
POTATOES = {"potato": (190, 152, 86), "unique_potato": (150, 166, 78)}
COINS = "bastion_coin_stack"
ANODE = "shipwreck_salt_zinc"

SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)

# The Stage 0 wood palette, shared with the Tool Bench.
OAK = (164, 114, 64)
OAK_DARK = (108, 70, 38)
FRAME = (86, 54, 30)
SEAM = (52, 32, 18)
COPPER = (196, 116, 56)
COPPER_DARK = (120, 64, 30)


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, uv=None, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is None:
            continue
        entry = {"texture": f"#{key}"}
        if uv and face in uv:
            entry["uv"] = uv[face]
        element["faces"][face] = entry
    if extra:
        element.update(extra)
    return element


def crossed(cx, cz, radius, flat, y0, y1, textures, faces=FACES):
    """A rounded vertical can: two crossed slabs, axis-aligned so it also renders as a wiki icon."""
    return [
        box((cx - radius, y0, cz - flat), (cx + radius, y1, cz + flat), textures, faces),
        box((cx - flat, y0, cz - radius), (cx + flat, y1, cz + radius), textures, faces),
    ]


def default_uv(face, start, end):
    """The UV Minecraft gives a face without an explicit one, so a moved element keeps its texture."""
    (x1, y1, z1), (x2, y2, z2) = start, end
    return {
        "up": [x1, z1, x2, z2],
        "down": [x1, 16 - z2, x2, 16 - z1],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


# ---------------------------------------------------------------- layout


def cell_layout(slots, interior=RACK_INTERIOR):
    """Centre x, centre z, and diameter of each cell slot. BatteryChassisRenderer.layout matches this."""
    if slots <= 0:
        return []
    cols = math.ceil(math.sqrt(slots))
    rows = math.ceil(slots / cols)
    x0, x1, z0, z1 = interior
    pitch = min((x1 - x0) / cols, (z1 - z0) / rows)
    diameter = min(CELL_MAX_DIAMETER, pitch * CELL_PITCH_SHARE)
    mid_x, mid_z = (x0 + x1) / 2, (z0 + z1) / 2
    cells = []
    for slot in range(slots):
        row, col = divmod(slot, cols)
        in_row = min(cols, slots - row * cols)
        cells.append((mid_x + ((in_row - 1) / 2 - col) * pitch, mid_z + (row - (rows - 1) / 2) * pitch, diameter))
    return cells


def cell_top(key):
    """Height of a cell's highest point in the canonical model."""
    if key in POTATOES:
        return 7.5
    if key == COINS:
        return 9.0
    if key == ANODE:
        return 8.0
    return CELL_HEIGHT


# ---------------------------------------------------------------- cells


def cell_elements(key):
    """A cell in its canonical 6x6 footprint, 10 pixels tall."""
    if key in POTATOES:
        return potato_elements()
    if key == COINS:
        return coin_elements()
    if key == ANODE:
        return anode_elements()
    return [
        *crossed(8, 8, CELL_R, CELL_F, 0.0, 1.0, "cap", faces=("north", "south", "east", "west", "down")),
        *crossed(8, 8, CELL_R, CELL_F, 1.0, 8.5, "body", faces=SIDES),
        *crossed(8, 8, CELL_R, CELL_F, 8.5, 9.5, {"up": "top", "*": "cap"}, faces=("north", "south", "east", "west", "up")),
        box((7.0, 9.5, 7.0), (9.0, 10.0, 9.0), "terminal", faces=("north", "south", "east", "west", "up")),
    ]


def potato_elements():
    """A potato cell: a lumpy potato pierced by a copper and a zinc electrode."""
    return [
        box((5.0, 0.0, 6.0), (11.0, 4.0, 10.0), "skin"),
        box((6.0, 0.0, 5.0), (10.0, 4.0, 11.0), "skin"),
        box((5.75, 3.5, 6.25), (10.25, 5.0, 9.75), "skin", faces=("north", "south", "east", "west", "up")),
        box((6.25, 4.25, 7.5), (6.75, 7.5, 8.0), "electrode_copper", faces=("north", "south", "east", "west", "up")),
        box((9.25, 4.25, 7.5), (9.75, 7.5, 8.0), "electrode_zinc", faces=("north", "south", "east", "west", "up")),
        box((6.0, 7.0, 7.25), (7.0, 7.5, 8.25), "electrode_copper", faces=("north", "south", "east", "west", "up", "down")),
        box((9.0, 7.0, 7.25), (10.0, 7.5, 8.25), "electrode_zinc", faces=("north", "south", "east", "west", "up", "down")),
    ]


def coin_elements():
    """The Bastion Coin Stack: gold coins stacked a little out of true."""
    elements = []
    offsets = (0.0, 0.25, -0.2, 0.15, -0.3, 0.2, -0.1, 0.3, 0.0, -0.2, 0.1, -0.25)
    for i, dx in enumerate(offsets):
        y0 = i * 0.75
        top = {"up": "coin_face", "*": "coin_edge"} if i == len(offsets) - 1 else "coin_edge"
        elements += crossed(8 + dx, 8 - dx * 0.6, 2.75, 1.75, y0, y0 + 0.75, top,
                            faces=("north", "south", "east", "west", "up") if i == len(offsets) - 1 else SIDES)
    return elements


def anode_elements():
    """The Shipwreck Salt-Zinc Anode: a crusted zinc slab with a copper strap bolted across the top."""
    return [
        box((5.5, 0.0, 6.5), (10.5, 7.0, 9.5), {"up": "salt", "*": "zinc"}),
        box((7.25, 7.0, 6.0), (8.75, 8.0, 10.0), "electrode_copper", faces=("north", "south", "east", "west", "up")),
    ]


def cell_texture_keys(key):
    if key in POTATOES:
        return {
            "skin": TEX + f"cell/{key}",
            "electrode_copper": TEX + "cell/electrode_copper",
            "electrode_zinc": TEX + "cell/electrode_zinc",
        }
    if key == COINS:
        return {"coin_edge": TEX + "cell/coin_edge", "coin_face": TEX + "cell/coin_face"}
    if key == ANODE:
        return {
            "zinc": TEX + "cell/salt_zinc",
            "salt": TEX + "cell/salt_crust",
            "electrode_copper": TEX + "cell/electrode_copper",
        }
    return {
        "body": TEX + f"cell/{key}_body",
        "cap": TEX + f"cell/{key}_cap",
        "top": TEX + f"cell/{key}_top",
        "terminal": TEX + "cell/terminal",
    }


def has_window(key):
    return key in CELLS


def placed(elements, cx, cz, diameter, base, height_scale, prefix):
    """Canonical cell elements moved into a slot, with explicit UVs so the texture maps as on the part model."""
    scale = diameter / CELL_WIDTH
    moved = []
    for element in elements:
        start, end = element["from"], element["to"]

        def at(point):
            return [cx + (point[0] - 8) * scale, base + point[1] * height_scale, cz + (point[2] - 8) * scale]

        faces = {}
        for face, entry in element["faces"].items():
            faces[face] = {"texture": "#" + prefix + entry["texture"][1:], "uv": entry.get("uv", default_uv(face, start, end))}
        moved.append({"from": [r(v) for v in at(start)], "to": [r(v) for v in at(end)], "faces": faces})
    return moved


def window_glow(key, cx, cz, diameter, base, height_scale, fill, prefix):
    """Lit charge windows on the four faces of a cell, as the renderer draws them, for item models."""
    scale = diameter / CELL_WIDTH
    half = WINDOW_HALF * scale
    reach = CELL_R * scale + 0.02
    y0 = base + WINDOW_Y0 * height_scale
    y1 = y0 + (WINDOW_Y1 - WINDOW_Y0) * height_scale * fill
    glow = f"{prefix}glow"
    return [
        box((cx - half, y0, cz - reach), (cx + half, y1, cz - reach), glow, faces=("north",)),
        box((cx - half, y0, cz + reach), (cx + half, y1, cz + reach), glow, faces=("south",)),
        box((cx - reach, y0, cz - half), (cx - reach, y1, cz + half), glow, faces=("west",)),
        box((cx + reach, y0, cz - half), (cx + reach, y1, cz + half), glow, faces=("east",)),
    ]


def baked_cells(slots, key, interior, base, height_scale, strap):
    """Elements and textures for a full chassis of one cell kind, for the item model."""
    prefix = f"{key}_"
    elements = []
    textures = {prefix + name: path for name, path in cell_texture_keys(key).items()}
    if has_window(key):
        textures[prefix + "glow"] = TEX + f"cell/{key}_glow"
    layout = cell_layout(slots, interior)
    fills = (0.85, 0.6, 0.95, 0.7, 0.8, 0.5, 0.9, 0.65, 0.75, 1.0, 0.55, 0.85, 0.7, 0.9)
    for i, (cx, cz, diameter) in enumerate(layout):
        elements += placed(cell_elements(key), cx, cz, diameter, base, height_scale, prefix)
        if has_window(key):
            elements += window_glow(key, cx, cz, diameter, base, height_scale, fills[i % len(fills)], prefix)
        if strap:
            half = diameter / 2 + 0.25
            elements.append(box((cx - half, PLINTH_TOP, cz - half), (cx + half, SOCKET_TOP, cz + half), "socket",
                                faces=("north", "south", "east", "west", "up")))
    if strap:
        for z in sorted({round(cz, 4) for _, cz, _ in layout}):
            elements.append(box((RAIL_OUT, STRAP_Y0, z - STRAP_HALF), (16 - RAIL_OUT, STRAP_Y1, z + STRAP_HALF), "wire",
                                faces=("north", "south", "up", "down")))
    return elements, textures


# ---------------------------------------------------------------- rack


def framed(y0, y1, textures, hatch_y0, hatch_y1, hatch_textures, inner_faces):
    """A full-footprint slab around a centre hatch, and the hatch filling part of its height."""
    a, b = HATCH
    return [
        box((0, y0, 0), (16, y1, a), {**textures, "south": "casing"}, faces=("north", "east", "west", "up", "down", "south")),
        box((0, y0, b), (16, y1, 16), {**textures, "north": "casing"}, faces=("south", "east", "west", "up", "down", "north")),
        box((0, y0, a), (a, y1, b), {**textures, "east": "casing"}, faces=("west", "up", "down", "east")),
        box((b, y0, a), (16, y1, b), {**textures, "west": "casing"}, faces=("east", "up", "down", "west")),
        box((a, hatch_y0, a), (b, hatch_y1, b), hatch_textures, faces=inner_faces),
    ]


def rack_elements():
    elements = []
    # Plinth with a tread floor between the posts and a hatch underneath for a bottom plate.
    elements += framed(0, PLINTH_TOP, {"up": "floor", "down": "base_bottom", "*": "plinth_side"},
                       PLATE_DEPTH, PLINTH_TOP, {"up": "floor", "down": "base_bottom"}, ("up", "down"))

    # Corner posts; their outward faces carry the charge meter.
    for x0 in (0.0, 16 - POST):
        for z0 in (0.0, 16 - POST):
            outward = {"north" if z0 == 0 else "south": "post", "west" if x0 == 0 else "east": "post", "*": "casing"}
            elements.append(box((x0, PLINTH_TOP, z0), (x0 + POST, BAND_Y, z0 + POST), outward, faces=SIDES))

    # Bus rails in the chassis metal, set in from the faces under the lid.
    lo, hi = POST, 16 - POST
    elements.append(box((lo, RAIL_Y0, RAIL_OUT), (hi, RAIL_Y1, RAIL_IN), "wire", faces=("north", "south", "down")))
    elements.append(box((lo, RAIL_Y0, 16 - RAIL_IN), (hi, RAIL_Y1, 16 - RAIL_OUT), "wire", faces=("north", "south", "down")))
    elements.append(box((RAIL_OUT, RAIL_Y0, lo), (RAIL_IN, RAIL_Y1, hi), "wire", faces=("east", "west", "down")))
    elements.append(box((16 - RAIL_IN, RAIL_Y0, lo), (16 - RAIL_OUT, RAIL_Y1, hi), "wire", faces=("east", "west", "down")))

    # Lid over a teal band, with a hatch on top for a top plate.
    elements.append(box((0, RAIL_Y1, 0), (16, BAND_Y, 16), {"down": "casing", "*": "band"}, faces=("north", "south", "east", "west", "down")))
    lid = framed(BAND_Y, LID_TOP, {"up": "lid_top", "*": "lid_side"}, BAND_Y, LID_TOP - PLATE_DEPTH, {"up": "lid_top"}, ("up",))
    for element in lid:
        element["faces"].pop("down", None)
    elements += lid
    return elements


def plate_elements(face, backed):
    """A plate on the north face; the renderer turns it to each side. A side plate is backed by a block reaching into
    the rack; a top or bottom plate sits in its hatch. A side contact plate is a solid wall filling the whole opening
    between the posts, flush with them."""
    if face == "contact" and backed:
        return [box((POST, PLINTH_TOP, 0.0), (16 - POST, BAND_Y - 0.5, CONTACT_DEPTH), {"north": "contact", "south": "casing"},
                    faces=("north", "south"), uv={"north": [POST, 16 - BAND_Y + 0.5, 16 - POST, 16 - PLINTH_TOP]})]
    (x0, y0, z0), (x1, y1, z1) = PORT
    face_uv = [0, 0, 16, 16] if face == "port" else [POST, POST, 16 - POST, 16 - POST]
    elements = [box((x0, y0, z0), (x1, y1, z1), {"north": face, "*": "wire"},
                    faces=FACES if backed else ("north", "east", "west", "up", "down"), uv={"north": face_uv})]
    if backed:
        elements.append(box((x0 + 1.0, y0 + 1.0, z1), (x1 - 1.0, y1 - 1.0, z1 + 1.0), "casing", faces=("south", "east", "west", "up", "down")))
    return elements


def crate_elements():
    elements = [box((0, 0, 0), (16, CRATE_FLOOR, 16), {"up": "crate_floor", "down": "crate_floor", "*": "crate_side"})]
    top = CRATE_TOP
    elements.append(box((0, CRATE_FLOOR, 0), (16, top, CRATE_WALL), {"up": "crate_rim", "*": "crate_side"}, faces=("north", "south", "east", "west", "up")))
    elements.append(box((0, CRATE_FLOOR, 16 - CRATE_WALL), (16, top, 16), {"up": "crate_rim", "*": "crate_side"}, faces=("north", "south", "east", "west", "up")))
    elements.append(box((0, CRATE_FLOOR, CRATE_WALL), (CRATE_WALL, top, 16 - CRATE_WALL), {"up": "crate_rim", "*": "crate_side"}, faces=("east", "west", "up")))
    elements.append(box((16 - CRATE_WALL, CRATE_FLOOR, CRATE_WALL), (16, top, 16 - CRATE_WALL), {"up": "crate_rim", "*": "crate_side"}, faces=("east", "west", "up")))
    # Copper bus bar on two oak blocks across the middle.
    (bx0, by0, bz0), (bx1, by1, bz1) = CRATE_BAR
    for x0 in (0.0, 16 - 1.5):
        elements.append(box((x0, top, bz0 - 0.75), (x0 + 1.5, by0, bz1 + 0.75), "crate_rim", faces=("north", "south", "east", "west")))
    elements.append(box((bx0, by0, bz0), (bx1, by1, bz1), "wire"))
    # Redstone lamp and a copper terminal on the front rim.
    (lx0, ly0, lz0), (lx1, ly1, lz1) = CRATE_LAMP
    elements.append(box((lx0, ly0, lz0), (lx1, ly1, lz1), "lamp"))
    elements.append(box((3.5, top, 0.5), (4.5, top + 1.0, 1.5), "wire", faces=("north", "south", "east", "west", "up")))
    return elements


# ---------------------------------------------------------------- models


SHARED_TEXTURES = {
    "casing": TEX + "casing",
    "plinth_side": TEX + "plinth_side",
    "floor": TEX + "floor",
    "base_bottom": TEX + "base_bottom",
    "post": TEX + "post",
    "band": TEX + "band",
    "lid_side": TEX + "lid_side",
    "lid_top": TEX + "lid_top",
    "socket": TEX + "socket",
}
CRATE_TEXTURES = {
    "crate_side": TEX + "crate/side",
    "crate_rim": TEX + "crate/rim",
    "crate_floor": TEX + "crate/floor",
    "lamp": TEX + "crate/lamp_off",
    "wire": TEX + "crate/bar",
}


def lit_meter(segments):
    """Lit LED segments on every post, for the item model."""
    elements = []
    for x0 in (0.0, 16 - POST):
        for z0 in (0.0, 16 - POST):
            cx, cz = x0 + POST / 2, z0 + POST / 2
            fz = -0.02 if z0 == 0 else 16.02
            fx = -0.02 if x0 == 0 else 16.02
            for i in range(segments):
                y0 = LED_Y0 + i * LED_PITCH
                led = "led_low" if i < 2 else "led_on"
                elements.append(box((cx - LED_HALF, y0, fz), (cx + LED_HALF, y0 + LED_HEIGHT, fz),
                                    led, faces=("north" if z0 == 0 else "south",)))
                elements.append(box((fx, y0, cz - LED_HALF), (fx, y0 + LED_HEIGHT, cz + LED_HALF),
                                    led, faces=("west" if x0 == 0 else "east",)))
    return elements


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_blockstate(name, active_model):
    variants = {}
    for active in ("false", "true"):
        model = active_model if active == "true" else f"rngtech:block/{name}"
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            entry = {"model": model}
            if rotation:
                entry["y"] = rotation
            variants[f"active={active},facing={facing}"] = entry
    write_json(BLOCKSTATES / f"{name}.json", {"variants": variants})


def write_models() -> None:
    write_json(MODELS / "block/battery_chassis.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": SHARED_TEXTURES,
        "elements": rack_elements(),
    })
    for chassis, metal in CHASSIS.items():
        name = f"{chassis}_battery_chassis"
        slots = CHASSIS_SLOTS[chassis]
        key = ITEM_CELLS[chassis]
        if chassis == "wooden":
            write_json(MODELS / f"block/{name}.json", {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:cutout",
                "textures": {"particle": TEX + "crate/side", **CRATE_TEXTURES},
                "elements": crate_elements(),
            })
            write_blockstate(name, f"rngtech:block/{name}")
            cells, cell_textures = baked_cells(slots, key, CRATE_INTERIOR, CRATE_FLOOR, CRATE_CELL_SCALE, strap=False)
            write_json(MODELS / f"item/{name}.json", {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:cutout",
                "textures": {"particle": TEX + "crate/side", **CRATE_TEXTURES, **cell_textures},
                "elements": cells + crate_elements(),
            })
            continue
        textures = {"particle": TEX + f"wire/{metal}", "wire": TEX + f"wire/{metal}"}
        write_json(MODELS / f"block/{name}.json", {"parent": MODEL, "textures": textures})
        write_blockstate(name, f"rngtech:block/{name}")
        cells, cell_textures = baked_cells(slots, key, RACK_INTERIOR, SOCKET_TOP, 1.0, strap=True)
        write_json(MODELS / f"item/{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {**SHARED_TEXTURES, **textures, **cell_textures,
                         "led_on": TEX + "fx/led_on", "led_low": TEX + "fx/led_low"},
            "elements": cells + lit_meter(6) + rack_elements(),
        })

    parts = MODELS / "block/battery_chassis"
    # Connector ports face a cable or Universal Connector; smooth contact plates face any other FE neighbour.
    for plate in ("port", "contact"):
        for backed, suffix in ((True, ""), (False, "_flat")):
            write_json(parts / f"{plate}{suffix}.json", {
                "textures": {"particle": "#wire", "casing": TEX + "casing", plate: TEX + plate},
                "elements": plate_elements(plate, backed),
            })
            for metal in METALS:
                write_json(parts / f"{plate}{suffix}_{metal}.json", {
                    "parent": f"{MODEL}/{plate}{suffix}",
                    "textures": {"wire": TEX + f"wire/{metal}"},
                })
    for key in [*CELLS, *POTATOES, COINS, ANODE]:
        textures = cell_texture_keys(key)
        write_json(parts / f"cell_{key}.json", {
            "textures": {"particle": next(iter(textures.values())), **textures},
            "elements": cell_elements(key),
        })


# ---------------------------------------------------------------- textures


def rng(name: str) -> random.Random:
    return random.Random(f"battery_chassis:{name}")


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


def panel_seams(image, step=16, rivets=True, depth=22):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            if x % step == 0 or y % step == 0:
                pixels[x, y] = (*shade(pixels[x, y], -depth), 255)
            elif x % step == 1 or y % step == 1:
                pixels[x, y] = (*shade(pixels[x, y], depth // 2), 255)
    if rivets:
        for py in range(0, size, step):
            for px in range(0, size, step):
                for dx, dy in ((3, 3), (step - 4, 3), (3, step - 4), (step - 4, step - 4)):
                    x, y = px + dx, py + dy
                    pixels[x, y] = (*shade(pixels[x, y], 34), 255)
                    pixels[x + 1, y + 1] = (*shade(pixels[x + 1, y + 1], -24), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def px(value):
    """Model pixels to texels on a 64x64 texture."""
    return int(round(value * 4))


def port_ring(draw, x0, y0, x1, y1):
    """A teal-ringed socket with four copper contacts, inside the texel box."""
    w = x1 - x0
    draw.rectangle([x0, y0, x1, y1], outline=(*SLATE_DARK, 255), width=2)
    draw.rectangle([x0 + w * 6 // 64, y0 + w * 6 // 64, x1 - w * 6 // 64, y1 - w * 6 // 64], outline=(*TEAL_DARK, 255), width=max(1, w * 3 // 64))
    draw.rectangle([x0 + w * 8 // 64, y0 + w * 8 // 64, x1 - w * 8 // 64, y1 - w * 8 // 64], outline=(*shade(TEAL, 10), 255), width=1)
    c0, c1 = x0 + w * 20 // 64, x1 - w * 20 // 64
    draw.rectangle([c0, y0 + w * 20 // 64, c1, y1 - w * 20 // 64], fill=(14, 18, 24, 255), outline=(*STEEL, 255))
    s = max(1, w * 3 // 64)
    for cx, cy in ((c0 + 4 * s // 3, y0 + w * 24 // 64), (c1 - 2 * s, y0 + w * 24 // 64),
                   (c0 + 4 * s // 3, y1 - w * 24 // 64 - s), (c1 - 2 * s, y1 - w * 24 // 64 - s)):
        draw.rectangle([cx, cy, cx + s - 1, cy + s - 1], fill=(184, 115, 51, 255))


def write_rack_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    side = noisy(64, SLATE_DARK, "plinth_side", amount=4)
    pixels = side.load()
    for x in range(64):
        pixels[x, 56] = (*shade(SLATE, 26), 255)
        pixels[x, 63] = (*shade(SLATE_DARK, -10), 255)
        for y in range(59, 62):
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)
    save(side, "plinth_side")

    # Floor between the posts: dark tread plate.
    tread = noisy(64, SLATE_DARK, "floor", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, "floor")

    # Underside and lid top: a full face of casing with the cable port in the middle (4 to 12).
    # Underside and lid top: plain panels whose seams frame the centre hatch (4 to 12).
    for name in ("base_bottom", "lid_top"):
        face = panel_seams(noisy(64, SLATE, name, amount=4), step=16, rivets=False, depth=16)
        draw = ImageDraw.Draw(face)
        draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
        if name == "lid_top":
            for x, y in ((3, 3), (59, 3), (3, 59), (59, 59)):
                draw.rectangle([x, y, x + 1, y + 1], fill=(150, 158, 168, 255))
        save(face, name)

    # Corner posts: slate with a recessed LED slot down each outward face (u 0.5-1.5 and 14.5-15.5, y 3-13).
    post = noisy(64, SLATE, "post", amount=4, streak=3)
    pixels = post.load()
    for y in range(64):
        for x in range(64):
            u = x % 64
            if u in (0, 7, 56, 63):
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif u in (1, 57):
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
    for i in range(LED_SEGMENTS):
        y0 = LED_Y0 + i * LED_PITCH
        top, bottom = px(16 - y0 - LED_HEIGHT), px(16 - y0)
        for x0 in (px(0.5), px(14.5)):
            for y in range(top - 1, bottom + 1):
                for x in range(x0 - 1, x0 + px(1.0) + 1):
                    pixels[x, y] = (*SLATE_DARK, 255)
            for y in range(top, bottom):
                for x in range(x0, x0 + px(1.0)):
                    pixels[x, y] = (24, 32, 30, 255)
    save(post, "post")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 2 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 6), 255)
    save(band, "band")
    # Grey copy the renderer tints with a dyed chassis's colour; the brightest texel turns white.
    top = max(g for _, g, _, _ in band.getdata())
    dyed = Image.new("RGBA", band.size)
    dyed.putdata([(v, v, v, a) for _, g, _, a in band.getdata() for v in [min(255, round(g * 255 / top))]])
    save(dyed, "band_dyed")

    # Lid sides, six texels tall: seams and a dark lamp lens in the middle (x 6.5 to 9.5).
    lid = noisy(64, SLATE, "lid_side", amount=4)
    draw = ImageDraw.Draw(lid)
    pixels = lid.load()
    for x in range(64):
        pixels[x, 0] = (*shade(SLATE, 22), 255)
        pixels[x, 5] = (*shade(SLATE_DARK, -6), 255)
    draw.rectangle([px(LAMP_X0) - 1, px(16 - LAMP_Y1) - 1, px(LAMP_X1), px(16 - LAMP_Y0)], fill=(*SLATE_DARK, 255))
    draw.rectangle([px(LAMP_X0), px(16 - LAMP_Y1), px(LAMP_X1) - 1, px(16 - LAMP_Y0) - 1], fill=(30, 34, 32, 255))
    for x in (4, 59):
        draw.rectangle([x, 2, x + 1, 3], fill=(150, 158, 168, 255))
    save(lid, "lid_side")

    socket = noisy(64, (40, 44, 50), "socket", amount=4)
    draw = ImageDraw.Draw(socket)
    for y in range(0, 64, 8):
        draw.line([(0, y), (63, y)], fill=(*SLATE_DARK, 255))
    save(socket, "socket")

    # Contact wall: a smooth bevelled panel with four screws, drawn between the posts (2 to 14) at 4 texels per
    # pixel, for a machine or another chassis against this side. Top and bottom plates show the same panel.
    contact = noisy(64, shade(SLATE, 8), "contact", amount=2)
    draw = ImageDraw.Draw(contact)
    lo, hi = px(POST), px(16 - POST) - 1
    draw.rectangle([lo, lo, hi, hi], outline=(*SLATE_DARK, 255), width=2)
    draw.line([(lo + 2, lo + 2), (hi - 2, lo + 2)], fill=(*shade(SLATE, 30), 255), width=2)
    draw.line([(lo + 2, lo + 2), (lo + 2, hi - 2)], fill=(*shade(SLATE, 30), 255), width=2)
    draw.line([(lo + 3, hi - 2), (hi - 2, hi - 2)], fill=(*shade(SLATE_DARK, -6), 255), width=2)
    draw.line([(hi - 2, lo + 3), (hi - 2, hi - 2)], fill=(*shade(SLATE_DARK, -6), 255), width=2)
    for x, y in ((lo + 6, lo + 6), (hi - 8, lo + 6), (lo + 6, hi - 8), (hi - 8, hi - 8)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
        draw.point((x + 2, y + 2), fill=(*SLATE_DARK, 255))
    save(contact, "contact")

    port = noisy(64, SLATE, "port", amount=4)
    port_ring(ImageDraw.Draw(port), 0, 0, 63, 63)
    for x, y in ((3, 3), (58, 3), (3, 58), (58, 58)):
        ImageDraw.Draw(port).rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
    save(port, "port")

    # Wiring metal: brushed along the run, with a bright top edge every 8 texels.
    for metal, (base, light) in METALS.items():
        wire = noisy(64, base, f"wire_{metal}", amount=4, streak=9)
        pixels = wire.load()
        for y in range(64):
            for x in range(64):
                if y % 8 == 0:
                    pixels[x, y] = (*mix(pixels[x, y], light, 0.55), 255)
                elif y % 8 == 7:
                    pixels[x, y] = (*shade(pixels[x, y], -30), 255)
        save(wire, f"wire/{metal}")

    # Renderer and item-model LEDs: a white lens the renderer tints, and baked green and amber ones.
    for name, color in (("led", (236, 236, 236)), ("led_on", (140, 240, 122)), ("led_low", (255, 176, 64))):
        led = Image.new("RGBA", (16, 16), (*color, 255))
        draw = ImageDraw.Draw(led)
        draw.rectangle([0, 0, 15, 15], outline=(*shade(color, -40), 255))
        draw.rectangle([1, 1, 14, 14], outline=(*shade(color, -16), 255))
        draw.line([(3, 3), (12, 3)], fill=(*shade(color, 20), 255))
        save(led, f"fx/{name}")


def write_cell_textures() -> None:
    """Cell cans mapped by element position: the round shading follows the distance from the can's axis."""
    for key, palette in CELLS.items():
        body = noisy(64, palette["body"], f"{key}_body", amount=4)
        pixels = body.load()
        for y in range(64):
            for x in range(64):
                u = (x + 0.5) / 4
                roundness = 1.0 - min(1.0, abs(u - 8) / 3.0)
                value = shade(pixels[x, y], -34 + 46 * roundness ** 0.6)
                pixels[x, y] = (*value, 255)
        draw = ImageDraw.Draw(body)
        # Accent rings near both caps.
        for y in (px(16 - 7.9), px(16 - 2.0)):
            for x in range(64):
                for dy in (0, 1):
                    u = (x + 0.5) / 4
                    roundness = 1.0 - min(1.0, abs(u - 8) / 3.0)
                    pixels[x, y + dy] = (*shade(palette["accent"], -30 + 30 * roundness), 255)
        # Charge window: a framed dark glass strip; BatteryChassisRenderer fills it from the bottom.
        wx0, wx1 = px(8 - WINDOW_HALF), px(8 + WINDOW_HALF)
        wy0, wy1 = px(16 - WINDOW_Y1), px(16 - WINDOW_Y0)
        draw.rectangle([wx0 - 1, wy0 - 1, wx1, wy1], fill=(*shade(palette["cap"], -10), 255))
        glass = mix((16, 20, 26), palette["glow"], 0.16)
        draw.rectangle([wx0, wy0, wx1 - 1, wy1 - 1], fill=(*glass, 255))
        draw.line([(wx0, wy0), (wx0, wy1 - 1)], fill=(*mix(glass, (255, 255, 255), 0.18), 255))
        save(body, f"cell/{key}_body")

        cap = noisy(64, palette["cap"], f"{key}_cap", amount=4)
        pixels = cap.load()
        for y in range(64):
            for x in range(64):
                u = (x + 0.5) / 4
                roundness = 1.0 - min(1.0, abs(u - 8) / 3.0)
                ridge = 10 if x % 3 == 0 else 0
                pixels[x, y] = (*shade(pixels[x, y], -24 + 34 * roundness ** 0.6 + ridge), 255)
        save(cap, f"cell/{key}_cap")

        top = noisy(64, palette["cap"], f"{key}_top", amount=4)
        draw = ImageDraw.Draw(top)
        draw.ellipse([px(5.25), px(5.25), px(10.75) - 1, px(10.75) - 1], outline=(*palette["accent"], 255), width=2)
        draw.ellipse([px(6.5), px(6.5), px(9.5) - 1, px(9.5) - 1], fill=(*shade(palette["cap"], -20), 255))
        save(top, f"cell/{key}_top")

        save(Image.new("RGBA", (16, 16), (*palette["glow"], 255)), f"cell/{key}_glow")

    terminal = noisy(64, (196, 200, 206), "terminal", amount=6, streak=4)
    save(terminal, "cell/terminal")

    # Potatoes: lumpy skin with eyes; the Unique one is the poisonous green.
    for key, skin in POTATOES.items():
        image = noisy(64, skin, key, amount=9)
        pixels = image.load()
        rand = rng(key + "_eyes")
        for _ in range(18):
            x, y = rand.randrange(62), rand.randrange(62)
            pixels[x, y] = (*shade(skin, -48), 255)
            pixels[x + 1, y] = (*shade(skin, -30), 255)
            pixels[x, y + 1] = (*shade(skin, 18), 255)
        for _ in range(140):
            x, y = rand.randrange(64), rand.randrange(64)
            pixels[x, y] = (*shade(pixels[x, y], rand.choice((-16, 12))), 255)
        save(image, f"cell/{key}")
    save(noisy(64, (210, 120, 60), "electrode_copper", amount=6, streak=6), "cell/electrode_copper")
    save(noisy(64, (170, 176, 182), "electrode_zinc", amount=6, streak=6), "cell/electrode_zinc")

    # Bastion coins: milled gold edges and a stamped face.
    edge = noisy(64, (226, 176, 52), "coin_edge", amount=6)
    pixels = edge.load()
    for y in range(64):
        for x in range(64):
            if x % 2 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -26), 255)
            if y % 3 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -40), 255)
    save(edge, "cell/coin_edge")
    face = noisy(64, (236, 188, 60), "coin_face", amount=6)
    draw = ImageDraw.Draw(face)
    draw.ellipse([px(5.5), px(5.5), px(10.5) - 1, px(10.5) - 1], outline=(150, 104, 24, 255), width=2)
    draw.rectangle([px(7.25), px(7.0), px(8.75) - 1, px(9.0) - 1], fill=(184, 132, 34, 255))
    save(face, "cell/coin_face")

    # Shipwreck anode: pitted zinc under a white salt crust that thickens toward the top.
    zinc = noisy(64, (150, 160, 168), "salt_zinc", amount=8, streak=4)
    pixels = zinc.load()
    rand = rng("salt_zinc_crust")
    for y in range(64):
        for x in range(64):
            crust = max(0.0, 0.55 - y / 64) + rand.uniform(-0.25, 0.25)
            if crust > 0.3:
                pixels[x, y] = (*mix(pixels[x, y], (232, 230, 220), min(1.0, crust)), 255)
            elif rand.random() < 0.04:
                pixels[x, y] = (*shade(pixels[x, y], -36), 255)
    save(zinc, "cell/salt_zinc")
    salt = noisy(64, (226, 224, 214), "salt_crust", amount=10)
    pixels = salt.load()
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], rand.choice((-30, 14))), 255)
    save(salt, "cell/salt_crust")


def boards(name, color, width, frame=True):
    """Horizontal planks `width` texels tall with dark seams, per-board tone, and grain; framed at the corners."""
    image = noisy(64, color, name, amount=6)
    pixels = image.load()
    rand = rng(name + "_boards")
    tones = [rand.uniform(-14, 12) for _ in range(64 // width + 1)]
    for y in range(64):
        for x in range(64):
            board = y // width
            grain = math.sin(x * 0.35 + board * 2.1 + math.sin(y * 1.3) * 1.2) * 5
            value = shade(pixels[x, y], tones[board] + grain)
            if y % width == 0:
                value = SEAM
            elif y % width == 1:
                value = shade(value, 12)
            if frame and (x < 4 or x >= 60):
                value = shade(FRAME, rand.uniform(-6, 6) + (8 if x in (1, 61) else 0))
            pixels[x, y] = (*value, 255)
    if frame:
        for y in range(2, 58, width):
            for x in (1, 61):
                pixels[x, y + 3] = (*COPPER, 255)
                pixels[x + 1, y + 4] = (*COPPER_DARK, 255)
    return image


def write_crate_textures() -> None:
    save(boards("crate_side", OAK, 12), "crate/side")
    save(boards("crate_floor", OAK_DARK, 16, frame=False), "crate/floor")
    rim = noisy(64, OAK_DARK, "crate_rim", amount=5)
    pixels = rim.load()
    for y in range(64):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], math.sin(x * 0.4 + math.sin(y * 0.2)) * 6), 255)
    save(rim, "crate/rim")
    # Copper bus bar: hammered, with the Stage 0 copper nail colours.
    bar = noisy(64, COPPER, "crate_bar", amount=7, streak=6)
    pixels = bar.load()
    rand = rng("crate_bar_dents")
    for _ in range(80):
        x, y = rand.randrange(63), rand.randrange(63)
        pixels[x, y] = (*shade(COPPER, 28), 255)
        pixels[x + 1, y + 1] = (*COPPER_DARK, 255)
    save(bar, "crate/bar")
    save(noisy(64, (86, 20, 16), "lamp_off", amount=4), "crate/lamp_off")
    lamp = noisy(64, (255, 70, 46), "lamp_on", amount=8)
    pixels = lamp.load()
    for y in range(64):
        for x in range(64):
            glow = 1.0 - min(1.0, math.hypot(x - 31.5, y - 31.5) / 40)
            pixels[x, y] = (*mix(pixels[x, y], (255, 200, 150), glow * 0.45), 255)
    save(lamp, "crate/lamp_on")


def write_cell_sprites() -> None:
    """16x16 Battery Cell item sprites: the same can, caps, rings, and lit window as the placed cell."""
    for key, palette in CELLS.items():
        image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        pixels = image.load()
        outline = shade(palette["cap"], -34)
        rand = rng(key + "_sprite")

        def put(x, y, color):
            pixels[x, y] = (*color, 255)

        def row(y, color, x0=4, x1=11, round_off=0):
            for x in range(x0, x1 + 1):
                centre = abs(x - 7.5) / 3.5
                put(x, y, shade(color, 22 - 40 * centre ** 1.5 + rand.uniform(-4, 4) + round_off))

        # Terminal.
        for x in (7, 8):
            put(x, 0, shade((196, 200, 206), 10 if x == 7 else -14))
            put(x, 1, shade((196, 200, 206), -6))
        # Top cap, body with accent rings, bottom cap.
        for y in (2, 3):
            row(y, palette["cap"])
        for y in range(4, 13):
            row(y, palette["body"])
        row(4, palette["accent"])
        row(12, palette["accent"])
        for y in (13, 14):
            row(y, palette["cap"])
        # Outline.
        for y in range(2, 15):
            put(3, y, outline)
            put(12, y, outline)
        for x in range(4, 12):
            put(x, 15, outline)
        for x in (4, 5, 6, 9, 10, 11):
            put(x, 1, outline)
        put(6, 0, outline)
        put(9, 0, outline)
        # Lit charge window.
        frame = shade(palette["cap"], -10)
        for y in range(5, 12):
            put(6, y, frame)
            put(9, y, frame)
        for x in (7, 8):
            put(x, 5, frame)
            put(x, 11, frame)
        glass = mix((16, 20, 26), palette["glow"], 0.16)
        for y in range(6, 11):
            lit = y >= 7
            put(7, y, shade(palette["glow"], 26) if lit else glass)
            put(8, y, palette["glow"] if lit else glass)
        CELL_SPRITES.mkdir(parents=True, exist_ok=True)
        image.save(CELL_SPRITES / f"{key}_battery_cell.png")


def write_textures() -> None:
    write_rack_textures()
    write_cell_textures()
    write_crate_textures()
    write_cell_sprites()


def main() -> None:
    write_models()
    for stale in TEXTURES.rglob("*.png"):
        stale.unlink()
    write_textures()


if __name__ == "__main__":
    main()
