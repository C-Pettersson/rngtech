#!/usr/bin/env python3
"""Generate the Crusher block models, part models, item models, and owned textures.

The design is a double-roll crusher. Feed drops from a hopper on top into the nip between two
toothed rolls, whose end faces show through the open front of the crushing chamber; the crushed
material falls down a stepped chute into an output bin at the front. Behind the chamber sits the
drive: a gearbox with the cable port on its back, a motor on its west side, and a cradle for the
Battery Cell on its east side.

The rolls are the installed Crush Head: a part model in the head's material, drawn twice (and on
the batching Tungstensteel and Exotic chassis, twice more as smaller secondary rolls) and turned
by CrusherRenderer, whose constants match the geometry here. With no head installed only the bare
shafts show. Every metal chassis shares one housing and differs by its shell, trim, and the hopper
rim in the metal at the top of its recipe; Iron, Copper, and Bronze stand on the smooth stone of
their recipe. The Wooden Crusher is a Stage 0 plank frame in its recipe's materials. Run from the
repository root:

    python tools/blockmodels/crusher.py
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
TEXTURES = ASSETS / "textures/block/crusher"
OLD_TEXTURES = ASSETS / "textures/block/crusher_chassis"
TEX = "rngtech:block/crusher/"
FACES = ("north", "south", "east", "west", "up", "down")

# Primary rolls: axes along z, end faces toward the open front (north, z = 0).
ROLL_Y = 8.5
ROLL_XS = (5.2, 10.8)
ROLL_TIP = 2.7
ROLL_Z0, ROLL_Z1 = 3.0, 9.5
# Secondary rolls under the primary pair on the batching chassis.
SECONDARY_Y = 4.3
SECONDARY_XS = (6.4, 9.6)
SECONDARY_TIP = 1.4
# Canonical roll part model: an eight-toothed star of two squares around (8, 8), z 4 to 12, tip radius 4.
PART_TIP = 4.0
PART_Z0, PART_Z1 = 4.0, 12.0
HUB_HALF, HUB_DEPTH = 0.9, 0.5
# Crushing chamber between the cheek plates, and the hopper above it.
CHAMBER_X0, CHAMBER_X1 = 2.5, 13.5
CHAMBER_Z0, CHAMBER_Z1 = 0.5, 9.5
HEADER_Y0, HEADER_Y1 = 11.25, 12.5
HOPPER_INNER = ((3.5, 2.0), (12.5, 8.5))
HOPPER_FLOOR = 12.75
FLOOR_Y = 2.5
# Drive: gearbox with the cable port, motor on the west, Battery Cell cradle on the east.
GEARBOX = ((3.5, 2.0, 10.0), (12.5, 12.5, 15.0))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
METER = ((12.5, 6.0, 11.0), (12.75, 10.5, 14.0))
METER_SEGMENTS = 4
MOTOR_X, MOTOR_Y = 2.0, 5.25
CELL_X, CELL_Y, CELL_Z = 14.0, 4.25, 12.75

METAL_CHASSIS = ("iron", "copper", "bronze", "steel", "aluminum", "titanium", "tungstensteel", "exotic")
HEAVY = ("tungstensteel", "exotic")
STONE_FOOTED = ("iron", "copper", "bronze")
# The hopper rim is the metal at the top middle of each chassis recipe.
RIM = {
    "iron": "copper", "copper": "copper", "bronze": "bronze", "steel": "steel",
    "aluminum": "aluminum", "titanium": "arclite", "tungstensteel": "nullite", "exotic": "naquadah",
}
# Rolls shown on each item model, which has no renderer: the Crush Head of the chassis stage.
ITEM_HEADS = {"wooden": "flint", **{chassis: chassis for chassis in METAL_CHASSIS}}
HEADS = ("flint", "iron", "copper", "bronze", "steel", "aluminum", "titanium", "tungstensteel", "exotic", "unique")


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
            entry["uv"] = [r(v) for v in uv[face]]
        element["faces"][face] = entry
    if extra:
        element.update(extra)
    return element


def default_uvs(start, end):
    """Minecraft's position-derived UVs, so a moved or scaled element can keep its canonical mapping."""
    (x1, y1, z1), (x2, y2, z2) = start, end
    return {
        "up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }


# ---------------------------------------------------------------- rolls

def roll_elements(cx, cy, tip, z0, z1):
    """A toothed roll around (cx, cy) from z0 to z1: two squares, one turned 45 degrees, and a hub cap.

    Every element keeps the UVs of the canonical part, so a baked roll matches the one the renderer draws.
    """
    k = tip / PART_TIP
    length = (z1 - z0) / (PART_Z1 - PART_Z0)
    half = PART_TIP / math.sqrt(2)

    def place(point):
        x, y, z = point
        return (cx + (x - 8) * k, cy + (y - 8) * k, z0 + (z - PART_Z0) * length)

    elements = []
    canonical = [
        ((8 - half, 8 - half, PART_Z0), (8 + half, 8 + half, PART_Z1), None),
        ((8 - half, 8 - half, PART_Z0 + 0.05), (8 + half, 8 + half, PART_Z1 - 0.05), 45),
    ]
    for start, end, angle in canonical:
        uvs = default_uvs(start, end)
        uvs["up"] = uvs["down"] = uvs["up"]
        textures = {"north": "roll_end", "south": "roll_end", "up": "roll_v", "down": "roll_v", "*": "roll"}
        extra = None
        if angle:
            extra = {"rotation": {"origin": [r(cx), r(cy), 8], "axis": "z", "angle": angle}}
        elements.append(box(place(start), place(end), textures, uv=uvs, extra=extra))
    hub = ((8 - HUB_HALF, 8 - HUB_HALF, PART_Z0 - HUB_DEPTH), (8 + HUB_HALF, 8 + HUB_HALF, PART_Z0))
    elements.append(box(place(hub[0]), place(hub[1]), "hub", faces=("north", "east", "west", "up", "down"),
                        uv={face: [0, 0, 16, 16] if face == "north" else [0, 0, 16, 4] for face in FACES}))
    return elements


def item_roll_elements(heavy):
    elements = []
    for x in ROLL_XS:
        elements += roll_elements(x, ROLL_Y, ROLL_TIP, ROLL_Z0, ROLL_Z1)
    if heavy:
        for x in SECONDARY_XS:
            elements += roll_elements(x, SECONDARY_Y, SECONDARY_TIP, ROLL_Z0, ROLL_Z1)
    return elements


def shaft_elements(heavy):
    """Bare shafts the rolls slide onto; hidden inside the rolls once a Crush Head is installed."""
    centres = [(x, ROLL_Y, 0.5) for x in ROLL_XS]
    if heavy:
        centres += [(x, SECONDARY_Y, 0.4) for x in SECONDARY_XS]
    return [box((x - h, y - h, ROLL_Z0 + 0.5), (x + h, y + h, ROLL_Z1), "shaft", faces=("north", "east", "west", "up", "down"))
            for x, y, h in centres]


# ---------------------------------------------------------------- metal housing

def ring(outer, inner, y0, y1, texture):
    """A rectangular ring between two (x0, z0, x1, z1) outlines."""
    (ox0, oz0, ox1, oz1), (ix0, iz0, ix1, iz1) = outer, inner
    return [
        box((ox0, y0, oz0), (ix0, y1, oz1), texture),
        box((ix1, y0, oz0), (ox1, y1, oz1), texture),
        box((ix0, y0, oz0), (ix1, y1, iz0), texture, faces=("north", "south", "up", "down")),
        box((ix0, y0, iz1), (ix1, y1, oz1), texture, faces=("north", "south", "up", "down")),
    ]


def inset(outline, amount):
    x0, z0, x1, z1 = outline
    return (x0 + amount, z0 + amount, x1 - amount, z1 - amount)


def hopper_elements(wall, rim, floor):
    """A feed hopper that steps out from a narrow throat on the header to a flared rim, so it reads as a funnel."""
    (ix0, iz0), (ix1, iz1) = HOPPER_INNER
    t = 0.5
    throat = (ix0 - t, iz0 - t, ix1 + t, iz1 + t)
    middle = (throat[0] - 0.75, throat[1] - 0.5, throat[2] + 0.75, throat[3] + 0.5)
    top = (middle[0] - 0.75, middle[1] - 0.5, middle[2] + 0.75, middle[3] + 0.5)
    elements = ring(throat, inset(throat, t), HEADER_Y1, 13.25, wall)
    elements += ring(middle, inset(throat, t), 13.25, 13.5, wall)
    elements += ring(middle, inset(middle, t), 13.5, 14.25, wall)
    elements += ring(top, inset(middle, t), 14.25, 14.5, rim)
    elements += ring(top, inset(top, t), 14.5, 16.0, rim)
    elements.append(box((ix0, HEADER_Y1, iz0), (ix1, HOPPER_FLOOR, iz1), floor, faces=("up",), uv={"up": [0, 0, 16, 16]}))
    return elements


def chamber_elements(heavy, cheek, liner, chute, floor):
    elements = []
    # Cheek plates, lined on the inside, from the floor to the header.
    for x0, x1, inner in ((1.0, CHAMBER_X0, "east"), (CHAMBER_X1, 15.0, "west")):
        elements.append(box((x0, 2.0, CHAMBER_Z0), (x1, HEADER_Y1, CHAMBER_Z1), {inner: liner, "*": cheek}))
    # Back wall the shafts run into.
    elements.append(box((CHAMBER_X0, FLOOR_Y, CHAMBER_Z1 - 0.5), (CHAMBER_X1, HEADER_Y1, CHAMBER_Z1), liner, faces=("north",)))
    # Floor, the stepped chute down to the bin, and the bin lip.
    elements.append(box((CHAMBER_X0, 2.0, CHAMBER_Z0), (CHAMBER_X1, FLOOR_Y, CHAMBER_Z1), {"up": floor}, faces=("up",)))
    if not heavy:
        elements.append(box((CHAMBER_X0, FLOOR_Y, 7.0), (CHAMBER_X1, 4.5, CHAMBER_Z1 - 0.5), chute, faces=("north", "up")))
        elements.append(box((CHAMBER_X0, FLOOR_Y, 4.75), (CHAMBER_X1, 3.5, 7.0), chute, faces=("north", "up")))
    return elements


def metal_housing(heavy):
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    elements += chamber_elements(heavy, "shell", "liner", "chute", "chute")
    if heavy:
        # Reinforcing ribs on the cheeks of the batching chassis.
        for x0, x1 in ((0.5, 1.0), (15.0, 15.5)):
            for z0 in (1.5, 6.5):
                elements.append(box((x0, 2.0, z0), (x1, HEADER_Y1, z0 + 1.25), "trim", faces=("north", "south", "east", "west", "up")))
    elements.append(box((CHAMBER_X0, FLOOR_Y, CHAMBER_Z0), (CHAMBER_X1, 4.0, CHAMBER_Z0 + 0.5), "trim"))
    # Header beam across the top of the chamber mouth, carrying the status lamp.
    elements.append(box((CHAMBER_X0, HEADER_Y0, 0.25), (CHAMBER_X1, HEADER_Y1, 3.0), {"north": "header", "*": "trim"},
                        faces=("north", "up", "down"), uv={"north": [0, 0, 11, 1.25]}))
    elements.append(box((11.0, 11.5, 0.0), (12.75, 12.25, 0.25), "lamp", faces=("north", "east", "west", "up", "down"),
                        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}))
    elements += hopper_elements("shell", "rim", "throat")
    elements += shaft_elements(heavy)

    # Gearbox with a teal band that stops short of the port face, the port, and the charge meter.
    (gx0, gy0, gz0), (gx1, gy1, gz1) = GEARBOX
    elements.append(box((gx0, gy0, gz0), (gx1, gy1, gz1), {"up": "gear_top", "*": "casing"}, faces=("south", "east", "west", "up")))
    elements.append(box((gx0 - 0.25, gy1 - 1.5, gz0), (gx1 + 0.25, gy1 - 1.0, gz1), "band", faces=("east", "west", "up", "down")))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}))
    elements.append(box(*METER, {"east": "meter", "*": "trim"}, faces=("north", "south", "east", "up", "down"), uv={"east": [0, 0, 3, 4.5]}))

    # Motor on the west: a ribbed drum along z on feet, a coupling flange into the gearbox, and a terminal box.
    elements.append(box((0.5, 2.0, 11.0), (3.5, 3.5, 14.75), "trim", faces=("north", "south", "east", "west", "up")))
    for half_x, half_y, inset_z in ((1.75, 1.0, 0.0), (1.0, 1.75, 0.05)):
        elements.append(box((MOTOR_X - half_x, MOTOR_Y - half_y, 10.5 + inset_z), (MOTOR_X + half_x, MOTOR_Y + half_y, 15.5 - inset_z),
                            {"north": "motor_end", "south": "motor_end", "*": "motor"}, uv={"north": [4, 4, 12, 12], "south": [4, 4, 12, 12]}))
    elements.append(box((MOTOR_X - 0.75, MOTOR_Y - 0.75, 10.0), (MOTOR_X + 0.75, MOTOR_Y + 0.75, 10.5), "shaft"))
    elements.append(box((3.75 - 0.5, 3.75, 11.25), (gx0, 6.75, 13.75), "trim", faces=("north", "south", "west", "up", "down")))
    elements.append(box((1.25, MOTOR_Y + 1.75, 12.0), (2.75, MOTOR_Y + 2.75, 13.75), "terminal",
                        faces=("north", "south", "east", "west", "up")))

    # Battery Cell cradle on the east: a base and two saddles the cell lies across.
    elements.append(box((12.75, 2.0, 10.25), (15.25, 2.5, 15.25), "trim", faces=("north", "south", "east", "west", "up")))
    for z0 in (10.75, 14.0):
        elements.append(box((12.75, 2.5, z0), (13.25, 3.5, z0 + 0.75), "trim", faces=("north", "south", "east", "west", "up")))
        elements.append(box((14.75, 2.5, z0), (15.25, 3.5, z0 + 0.75), "trim", faces=("north", "south", "east", "west", "up")))
    return elements


# ---------------------------------------------------------------- wooden housing

WOOD_POSTS = ((0.5, 0.5), (14.0, 0.5), (0.5, 8.0), (14.0, 8.0))


def wooden_housing():
    """Stage 0: planks, sticks, and flint. A plank frame on stick posts around the rolls, a plank hopper, and a
    plank crate behind with the copper-ringed cable socket on its back."""
    elements = []
    for x, z in WOOD_POSTS:
        elements.append(box((x, 0.0, z), (x + 1.5, HEADER_Y1, z + 1.5), {"down": "post_end", "up": "post_end", "*": "post"}))
    for x0, x1, inner in ((0.75, CHAMBER_X0, "east"), (CHAMBER_X1, 15.25, "west")):
        elements.append(box((x0, FLOOR_Y, 2.0), (x1, HEADER_Y0, 8.0), {inner: "dark", "*": "boards_v"}))
    # Plank floor with a back board sloping toward the bin lip.
    elements.append(box((0.75, 1.0, 0.75), (15.25, FLOOR_Y, 9.25), {"up": "boards", "*": "boards_edge"}))
    elements.append(box((CHAMBER_X0, FLOOR_Y, 6.5), (CHAMBER_X1, 4.0, CHAMBER_Z1 - 0.5), {"*": "boards"}, faces=("north", "up")))
    elements.append(box((CHAMBER_X0, FLOOR_Y, CHAMBER_Z0), (CHAMBER_X1, 3.75, CHAMBER_Z0 + 0.5), "boards_edge"))
    elements.append(box((CHAMBER_X0, FLOOR_Y, CHAMBER_Z1 - 0.5), (CHAMBER_X1, HEADER_Y1, CHAMBER_Z1), "dark", faces=("north",)))
    # Header plank and stick braces across the back of the frame.
    elements.append(box((2.0, HEADER_Y0, 0.25), (14.0, HEADER_Y1, 2.5), "boards_edge", faces=("north", "south", "up", "down")))
    elements.append(box((2.0, HEADER_Y0, 7.5), (14.0, HEADER_Y1, 9.5), "boards_edge", faces=("north", "south", "up", "down")))
    elements += hopper_elements("boards_v", "boards_edge", "throat")
    elements += shaft_elements(False)
    # Plank crate behind the frame, framed in dark oak, with the cable socket on its back.
    (gx0, gy0, gz0), (gx1, gy1, gz1) = GEARBOX
    elements.append(box((gx0, 0.25, gz0 + 0.25), (gx1, gy1, gz1 - 0.25), {"up": "boards", "down": "boards", "*": "boards_v"},
                        faces=("south", "east", "west", "up", "down")))
    for x, z in ((gx0 - 0.5, gz0), (gx1 - 1.0, gz0), (gx0 - 0.5, gz1 - 1.5), (gx1 - 1.0, gz1 - 1.5)):
        elements.append(box((x, 0.0, z), (x + 1.5, gy1 + 0.25, z + 1.5), {"down": "post_end", "up": "post_end", "*": "post"}))
    elements.append(box((PORT[0][0], PORT[0][1], gz1 - 0.25), PORT[1], {"south": "port", "*": "post"},
                        faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}))
    # Plank saddle for the Battery Cell on the east side.
    elements.append(box((13.0, 0.0, 10.25), (15.25, 2.5, 15.25), {"up": "boards", "*": "boards_edge"}, faces=("north", "south", "east", "up")))
    for z0 in (10.75, 14.0):
        elements.append(box((13.0, 2.5, z0), (15.25, 3.25, z0 + 0.75), "post", faces=("north", "south", "east", "west", "up")))
    return elements


# ---------------------------------------------------------------- models

SHARED_TEXTURES = {
    "casing": TEX + "casing",
    "liner": TEX + "liner",
    "chute": TEX + "chute",
    "throat": TEX + "throat",
    "header": TEX + "header",
    "shaft": TEX + "shaft",
    "gear_top": TEX + "gear_top",
    "band": TEX + "band",
    "port": TEX + "port",
    "meter": TEX + "meter",
    "motor": TEX + "motor",
    "motor_end": TEX + "motor_end",
    "terminal": TEX + "terminal",
    "lamp": TEX + "lamp_off",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
}
WOOD_KEYS = ("post", "post_end", "boards", "boards_v", "boards_edge", "dark", "port")


def wood_textures():
    textures = {key: TEX + f"wood/{key}" for key in WOOD_KEYS}
    textures.update({"particle": TEX + "wood/boards", "throat": TEX + "wood/throat", "shaft": TEX + "shaft"})
    return textures


def chassis_textures(chassis):
    textures = {
        "particle": TEX + f"shell/{chassis}",
        "shell": TEX + f"shell/{chassis}",
        "trim": TEX + f"trim/{chassis}",
        "rim": TEX + f"rim/{RIM[chassis]}",
    }
    if chassis in STONE_FOOTED:
        textures.update({"plinth_top": TEX + "stone_top", "plinth_side": TEX + "stone_side"})
    return textures


def head_textures(head):
    return {
        "roll": TEX + f"roll/{head}",
        "roll_v": TEX + f"roll/{head}_v",
        "roll_end": TEX + f"roll/{head}_end",
        "hub": TEX + f"roll/{head}_hub",
    }


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    for name, heavy in (("crusher", False), ("crusher_heavy", True)):
        housing = metal_housing(heavy)
        write_json(MODELS / f"block/{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": SHARED_TEXTURES,
            "elements": housing,
        })
        write_json(MODELS / f"item/{name}_template.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": SHARED_TEXTURES,
            "elements": item_roll_elements(heavy) + housing,
        })
    for chassis in METAL_CHASSIS:
        name = f"{chassis}_crusher_chassis"
        parent = "crusher_heavy" if chassis in HEAVY else "crusher"
        write_json(MODELS / f"block/{name}.json", {"parent": f"rngtech:block/{parent}", "textures": chassis_textures(chassis)})
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": TEX + "lamp_on"}})
        write_json(MODELS / f"item/{name}.json", {
            "parent": f"rngtech:item/{parent}_template",
            "textures": {**chassis_textures(chassis), **head_textures(ITEM_HEADS[chassis])},
        })

    wooden = wooden_housing()
    write_json(MODELS / "block/wooden_crusher_chassis.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": wood_textures(),
        "elements": wooden,
    })
    write_json(MODELS / "block/wooden_crusher_chassis_active.json", {"parent": "rngtech:block/wooden_crusher_chassis"})
    write_json(MODELS / "item/wooden_crusher_chassis.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**wood_textures(), **head_textures(ITEM_HEADS["wooden"])},
        "elements": item_roll_elements(False) + wooden,
    })

    parts = MODELS / "block/crusher"
    canonical = roll_elements(8.0, 8.0, PART_TIP, PART_Z0, PART_Z1)
    write_json(parts / "roll.json", {"textures": {"particle": "#roll"}, "elements": canonical})
    for head in HEADS:
        write_json(parts / f"roll_{head}.json", {"parent": "rngtech:block/crusher/roll", "textures": head_textures(head)})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"crusher:{name}")


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


def blotches(image, name, color, count, radius, strength):
    """Soft patches, such as rust or tarnish, blended into the base."""
    rand = rng(name)
    pixels = image.load()
    size = image.size[0]
    for _ in range(count):
        cx, cy = rand.uniform(0, size), rand.uniform(0, size)
        rad = rand.uniform(radius * 0.5, radius)
        for y in range(size):
            for x in range(size):
                dx = min(abs(x - cx), size - abs(x - cx))
                dy = min(abs(y - cy), size - abs(y - cy))
                t = 1.0 - math.hypot(dx, dy) / rad
                if t > 0 and rand.random() < 0.85:
                    pixels[x, y] = (*mix(pixels[x, y], color, min(1.0, t * strength)), 255)
    return image


def flecks(image, name, color, count, spread=8):
    rand = rng(name)
    pixels = image.load()
    size = image.size[0]
    for _ in range(count):
        x, y = rand.randrange(size), rand.randrange(size)
        pixels[x, y] = (*shade(color, rand.uniform(-spread, spread)), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
LINER = (52, 54, 58)

# Shell and trim per metal chassis: the same housing in each chassis metal.
CHASSIS_COLORS = {
    "iron": ((118, 118, 114), (168, 168, 162)),
    "copper": ((142, 82, 48), (214, 134, 82)),
    "bronze": ((160, 116, 60), (204, 166, 92)),
    "steel": ((72, 82, 94), (150, 158, 168)),
    "aluminum": ((168, 178, 190), (212, 220, 230)),
    "titanium": ((106, 118, 138), (158, 174, 196)),
    "tungstensteel": ((56, 60, 70), (100, 108, 122)),
    "exotic": ((50, 42, 62), (76, 66, 92)),
}
RIM_COLORS = {
    "copper": (214, 128, 72),
    "bronze": (204, 160, 84),
    "steel": (150, 158, 168),
    "aluminum": (206, 214, 224),
    "arclite": (110, 200, 228),
    "nullite": (70, 66, 92),
    "naquadah": (66, 54, 86),
}
# Crush Head materials, after the Crush Head item palettes.
HEAD_COLORS = {
    "iron": (190, 190, 184),
    "copper": (212, 118, 60),
    "bronze": (196, 140, 62),
    "steel": (126, 132, 134),
    "aluminum": (196, 210, 222),
    "titanium": (140, 160, 188),
    "tungstensteel": (84, 92, 104),
    "exotic": (78, 62, 92),
    "unique": (150, 104, 62),
}
EXOTIC_GLOW = (170, 96, 222)


def write_chassis_textures() -> None:
    for chassis, (shell_color, trim_color) in CHASSIS_COLORS.items():
        if chassis == "iron":
            shell = panel_seams(noisy(64, shell_color, "iron_shell", amount=8), step=32, depth=24)
            blotches(shell, "iron_rust", (120, 84, 60), 4, 10, 0.45)
        elif chassis == "copper":
            shell = noisy(64, shell_color, "copper_shell", amount=7)
            flecks(shell, "copper_dimples", shade(shell_color, 24), 80, 6)
            panel_seams(shell, step=32, depth=28)
            blotches(shell, "copper_tarnish", (92, 56, 36), 5, 11, 0.5)
        elif chassis == "bronze":
            shell = panel_seams(noisy(64, shell_color, "bronze_shell", amount=5, streak=10), step=16, rivets=False, depth=24)
        elif chassis == "steel":
            shell = panel_seams(noisy(64, shell_color, "steel_shell", amount=4, streak=4), step=16, depth=18)
        elif chassis == "aluminum":
            shell = panel_seams(noisy(64, shell_color, "aluminum_shell", amount=3, streak=8), step=32, rivets=False, depth=20)
        elif chassis == "titanium":
            shell = panel_seams(noisy(64, shell_color, "titanium_shell", amount=4, streak=6), step=16, depth=20)
        elif chassis == "tungstensteel":
            shell = panel_seams(noisy(64, shell_color, "tungstensteel_shell", amount=5), step=16, depth=16)
            pixels = shell.load()
            for y in range(64):
                for x in range(64):
                    if (x + y) % 16 == 8 and x % 16 not in (0, 1) and y % 16 not in (0, 1):
                        pixels[x, y] = (*shade(pixels[x, y], -10), 255)
        else:
            shell = panel_seams(noisy(64, shell_color, "exotic_shell", amount=5), step=16, rivets=False, depth=14)
            pixels = shell.load()
            for y in range(64):
                for x in range(64):
                    if x % 16 == 8 and y % 16 in range(3, 13):
                        pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.45), 255)
            flecks(shell, "exotic_glints", EXOTIC_GLOW, 24, 20)
        save(shell, f"shell/{chassis}")
        trim = noisy(64, trim_color, f"{chassis}_trim", amount=6, streak=5)
        if chassis == "iron":
            blotches(trim, "iron_trim_rust", (128, 90, 64), 3, 8, 0.4)
        save(trim, f"trim/{chassis}")

    for metal, color in RIM_COLORS.items():
        rim = noisy(64, color, f"rim_{metal}", amount=6, streak=6)
        pixels = rim.load()
        for y in range(64):
            for x in range(64):
                if y % 8 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -22), 255)
                elif y % 8 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 16), 255)
        if metal == "naquadah":
            for x in range(64):
                for y in (3, 4):
                    pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.7), 255)
        elif metal == "arclite":
            flecks(rim, "rim_arclite_sparks", (200, 244, 255), 30, 10)
        save(rim, f"rim/{metal}")


def roll_surface(head, color, vertical):
    """Roll faces with tooth ridges running along the roll axis."""
    if head == "flint":
        base = noisy(64, (150, 104, 58), "roll_flint_wood", amount=6)
        pixels = base.load()
        rand = rng("roll_flint_studs")
        for row in range(0, 64, 8):
            for col in range(2, 64, 6):
                tone = rand.choice(((48, 48, 54), (70, 70, 76), (36, 36, 42)))
                for dx in range(3):
                    for dy in range(3):
                        if rand.random() < 0.85:
                            x, y = col + dx, row + 2 + dy
                            if x < 64:
                                pixels[x, y] = (*shade(tone, rand.uniform(-6, 6)), 255)
                pixels[min(63, col), row + 2] = (120, 122, 130, 255)
    else:
        base = noisy(64, color, f"roll_{head}", amount=6, streak=4)
        pixels = base.load()
        for y in range(64):
            for x in range(64):
                phase = y % 8
                if phase in (0, 1):
                    pixels[x, y] = (*shade(pixels[x, y], -34), 255)
                elif phase == 2:
                    pixels[x, y] = (*shade(pixels[x, y], -14), 255)
                elif phase == 4:
                    pixels[x, y] = (*shade(pixels[x, y], 26), 255)
        if head == "exotic":
            for y in range(64):
                if y % 8 == 5:
                    for x in range(64):
                        pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.6), 255)
        elif head == "unique":
            blotches(base, "roll_unique_wear", (110, 70, 40), 6, 10, 0.6)
            flecks(base, "roll_unique_flecks", (232, 150, 70), 40, 12)
    if vertical:
        base = base.transpose(Image.TRANSPOSE)
    return base


def roll_end(head, color):
    """The end face: radial shading out to the tooth tips, a worn rim, and keyed bolts around the hub."""
    image = Image.new("RGBA", (64, 64))
    pixels = image.load()
    rand = rng(f"roll_end_{head}")
    base = (150, 104, 58) if head == "flint" else color
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 32) / 22.6
            value = shade(base, 14 - 30 * d + rand.uniform(-5, 5))
            if 0.42 < d < 0.48:
                value = shade(value, -24)
            pixels[x, y] = (*value, 255)
    if head == "flint":
        for angle in range(0, 360, 45):
            px = 32 + math.cos(math.radians(angle + 22.5)) * 15
            py = 32 + math.sin(math.radians(angle + 22.5)) * 15
            for dx in range(-2, 2):
                for dy in range(-2, 2):
                    pixels[int(px + dx), int(py + dy)] = (*shade((52, 52, 58), rand.uniform(-8, 8)), 255)
    elif head == "exotic":
        for y in range(64):
            for x in range(64):
                d = math.hypot(x + 0.5 - 32, y + 0.5 - 32) / 22.6
                if 0.62 < d < 0.68:
                    pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.75), 255)
    elif head == "unique":
        flecks(image, "roll_end_unique", (232, 150, 70), 30, 12)
    for angle in range(0, 360, 90):
        px = int(32 + math.cos(math.radians(angle + 45)) * 9)
        py = int(32 + math.sin(math.radians(angle + 45)) * 9)
        pixels[px, py] = (*shade(base, 46), 255)
        pixels[px + 1, py + 1] = (*shade(base, -40), 255)
    return image


def hub(head, color):
    """Hub cap, mapped whole on its face and as a strip on its sides: a bolt head with a hex socket."""
    base = (96, 100, 108) if head == "flint" else shade(color, -12)
    image = noisy(64, base, f"hub_{head}", amount=4)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(base, -36), 255), width=4)
    draw.regular_polygon((32, 32, 14), 6, fill=(*shade(base, -46), 255))
    draw.regular_polygon((32, 32, 7), 6, fill=(*shade(base, -70), 255))
    return image


def write_roll_textures() -> None:
    for head in HEADS:
        color = HEAD_COLORS.get(head, (150, 104, 58))
        save(roll_surface(head, color, False), f"roll/{head}")
        save(roll_surface(head, color, True), f"roll/{head}_v")
        save(roll_end(head, color), f"roll/{head}_end")
        save(hub(head, color), f"roll/{head}_hub")


def write_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    tread = noisy(64, SLATE_DARK, "plinth_top", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, "plinth_top")

    side = noisy(64, SLATE_DARK, "plinth_side", amount=4)
    pixels = side.load()
    for x in range(64):
        pixels[x, 56] = (*shade(SLATE, 26), 255)
        pixels[x, 63] = (*shade(SLATE_DARK, -10), 255)
        for y in range(59, 62):
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)
    save(side, "plinth_side")

    # Smooth stone footing for the Iron, Copper, and Bronze chassis, whose recipes stand on smooth stone.
    stone = noisy(64, (158, 158, 158), "stone_top", amount=5)
    pixels = stone.load()
    for i in range(64):
        for edge in (0, 63):
            pixels[i, edge] = (*shade((120, 120, 120), 0), 255)
            pixels[edge, i] = (*shade((120, 120, 120), 0), 255)
    save(stone, "stone_top")
    stone_side = noisy(64, (150, 150, 150), "stone_side", amount=5)
    pixels = stone_side.load()
    for x in range(64):
        pixels[x, 56] = (190, 190, 190, 255)
        pixels[x, 63] = (112, 112, 112, 255)
    save(stone_side, "stone_side")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    # Chamber liner: worn wear plates bolted inside the cheeks and across the back wall.
    liner = panel_seams(noisy(64, LINER, "liner", amount=7), step=16, depth=18)
    blotches(liner, "liner_scuffs", (84, 84, 86), 6, 8, 0.5)
    save(liner, "liner")

    chute = noisy(64, (70, 72, 76), "chute", amount=6, streak=6)
    pixels = chute.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -26), 255)
    flecks(chute, "chute_grit", (112, 108, 100), 60, 10)
    save(chute, "chute")

    # Hopper floor, mapped whole: a dark feed slot over the nip between two scuffed plates.
    throat = noisy(64, (58, 60, 64), "throat", amount=5)
    draw = ImageDraw.Draw(throat)
    draw.rectangle([0, 0, 63, 63], outline=(40, 42, 46, 255), width=2)
    draw.rectangle([24, 4, 39, 59], fill=(14, 14, 16, 255))
    draw.line([(23, 4), (23, 59)], fill=(96, 98, 104, 255))
    draw.line([(40, 4), (40, 59)], fill=(96, 98, 104, 255))
    save(throat, "throat")

    # Header face, mapped as an 11:1.25 strip: a riveted beam.
    header = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    header.paste(noisy(64, (88, 94, 102), "header", amount=4).crop((0, 0, 64, 5)), (0, 0))
    hp = header.load()
    for x in range(44):
        hp[x, 0] = (120, 128, 138, 255)
        hp[x, 4] = (50, 54, 60, 255)
    for x in range(3, 44, 8):
        hp[x, 2] = (160, 168, 178, 255)
        hp[x + 1, 3] = (44, 48, 54, 255)
    save(header, "header")

    save(noisy(64, (128, 136, 146), "shaft", amount=4, streak=4), "shaft")

    gear_top = noisy(64, (40, 47, 57), "gear_top", amount=4)
    draw = ImageDraw.Draw(gear_top)
    for cx, cy, rad in ((22, 30, 14), (44, 30, 14)):
        draw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], outline=(*SLATE_DARK, 255), width=2)
        draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(*shade(STEEL, 10), 255))
    for x in range(6, 64, 12):
        draw.rectangle([x, 56, x + 1, 57], fill=(150, 158, 168, 255))
    save(panel_seams(gear_top, step=32, rivets=False, depth=14), "gear_top")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the port plate.
    port = noisy(64, SLATE, "port", amount=4)
    draw = ImageDraw.Draw(port)
    draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
    draw.rectangle([6, 6, 57, 57], outline=(*TEAL_DARK, 255), width=3)
    draw.rectangle([8, 8, 55, 55], outline=(*shade(TEAL, 10), 255), width=1)
    for x, y in ((3, 3), (58, 3), (3, 58), (58, 58)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
    draw.rectangle([20, 20, 43, 43], fill=(14, 18, 24, 255), outline=(98, 108, 120, 255))
    for x, y in ((24, 24), (37, 24), (24, 37), (37, 37)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(184, 115, 51, 255))
    save(port, "port")

    # Charge meter, mapped as a 3:4.5 panel: steel rails around four dark LED slots.
    meter = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = meter.load()
    for y in range(18):
        for x in range(12):
            edge = x in (0, 11) or y in (0, 17)
            pixels[x, y] = (*shade(STEEL, 14 if edge else -12), 255) if edge or x in (1, 10) or y in (1, 16) else (18, 22, 28, 255)
    for i in range(METER_SEGMENTS):
        y0, y1 = meter_segment(i)
        for y in range(round((METER[1][1] - y1) * 4), round((METER[1][1] - y0) * 4)):
            for x in range(3, 9):
                pixels[x, y] = (36, 44, 40, 255)
    save(meter, "meter")

    # Motor drum: cooling ribs along the drum, and an end bell with a vent ring.
    motor = noisy(64, (56, 64, 76), "motor", amount=4)
    pixels = motor.load()
    for y in range(64):
        for x in range(64):
            if x % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
            elif x % 4 == 2:
                pixels[x, y] = (*shade(pixels[x, y], 16), 255)
    save(motor, "motor")
    motor_end = noisy(64, (64, 72, 84), "motor_end", amount=4)
    draw = ImageDraw.Draw(motor_end)
    draw.ellipse([18, 18, 45, 45], outline=(*SLATE_DARK, 255), width=2)
    for angle in range(0, 360, 30):
        x = 32 + math.cos(math.radians(angle)) * 10
        y = 32 + math.sin(math.radians(angle)) * 10
        draw.point((x, y), fill=(20, 22, 26, 255))
    save(motor_end, "motor_end")

    terminal = noisy(64, (150, 86, 46), "terminal", amount=6)
    flecks(terminal, "terminal_glints", (220, 150, 96), 40, 8)
    save(terminal, "terminal")

    save(noisy(64, (40, 46, 40), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (140, 240, 122), "lamp_on", amount=6), "lamp_on")

    # LED lens, tinted by CrusherRenderer: a bright face with a darker rim and a glint.
    led = Image.new("RGBA", (16, 16), (232, 232, 232, 255))
    draw = ImageDraw.Draw(led)
    draw.rectangle([0, 0, 15, 15], outline=(176, 176, 176, 255), width=2)
    draw.rectangle([3, 3, 6, 4], fill=(255, 255, 255, 255))
    save(led, "fx/led")

    write_chassis_textures()
    write_roll_textures()
    write_wood_textures()


# The Stage 0 wood palette, shared with the Tool Bench: oak boards, a dark oak frame, and copper nails.
OAK = (164, 114, 64)
OAK_DARK = (108, 70, 38)
FRAME = (86, 54, 30)
SEAM = (52, 32, 18)
COPPER = (196, 116, 56)
COPPER_DARK = (120, 64, 30)


def boards(name, color, width, vertical=False, nails=True):
    """Planks `width` texels wide with dark seams, per-board tone, wavy grain, and copper nails at the board ends."""
    image = noisy(64, color, name, amount=6)
    pixels = image.load()
    rand = rng(name + "_boards")
    tones = [rand.uniform(-14, 12) for _ in range(64 // width + 1)]
    for y in range(64):
        for x in range(64):
            across, along = (x, y) if vertical else (y, x)
            board = across // width
            grain = math.sin(along * 0.35 + board * 2.1 + math.sin(across * 1.3) * 1.2) * 5
            value = shade(pixels[x, y], tones[board] + grain)
            if across % width == 0:
                value = SEAM
            elif across % width == 1:
                value = shade(value, 12)
            pixels[x, y] = (*value, 255)
    if nails:
        for board in range(64 // width):
            for along in (3, 60):
                across = board * width + width // 2
                x, y = (across, along) if vertical else (along, across)
                pixels[x, y] = (*COPPER, 255)
                pixels[x + 1, y + 1] = (*COPPER_DARK, 255)
    return image


def write_wood_textures() -> None:
    save(boards("wood_boards", OAK, 16), "wood/boards")
    save(boards("wood_boards_v", OAK, 16, vertical=True), "wood/boards_v")
    save(boards("wood_boards_edge", OAK_DARK, 8, nails=False), "wood/boards_edge")
    save(noisy(64, (46, 32, 20), "wood_dark", amount=5), "wood/dark")

    post = noisy(64, FRAME, "wood_post", amount=5)
    pixels = post.load()
    for y in range(64):
        for x in range(64):
            grain = math.sin(x * 0.9 + math.sin(y * 0.15) * 2.0) * 7
            pixels[x, y] = (*shade(pixels[x, y], grain), 255)
    save(post, "wood/post")

    post_end = noisy(64, OAK_DARK, "wood_post_end", amount=5)
    pixels = post_end.load()
    for y in range(64):
        for x in range(64):
            if math.hypot((x % 12) - 5.5, (y % 12) - 5.5) % 2.5 < 0.7:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(post_end, "wood/post_end")

    throat = boards("wood_throat", OAK_DARK, 16, nails=False)
    draw = ImageDraw.Draw(throat)
    draw.rectangle([24, 4, 39, 59], fill=(22, 14, 8, 255))
    save(throat, "wood/throat")

    # Cable port: a dark oak plate with a copper ring around a socket, as on the Tool Bench.
    port = noisy(64, FRAME, "wood_port", amount=5)
    draw = ImageDraw.Draw(port)
    draw.rectangle([0, 0, 63, 63], outline=(*SEAM, 255), width=2)
    draw.ellipse([10, 10, 53, 53], outline=(*COPPER_DARK, 255), width=4)
    draw.ellipse([12, 12, 51, 51], outline=(*COPPER, 255), width=2)
    draw.ellipse([22, 22, 41, 41], fill=(28, 18, 12, 255))
    for x, y in ((4, 4), (58, 4), (4, 58), (58, 58)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(*COPPER, 255))
    save(port, "wood/port")


def meter_segment(index):
    """Bottom and top of one charge-meter LED, in model pixels."""
    y0 = METER[0][1] + 0.5 + index * 1.0
    return y0, y0 + 0.75


def remove_old_textures() -> None:
    """The cube faces this design replaces."""
    if OLD_TEXTURES.is_dir():
        for path in sorted(OLD_TEXTURES.rglob("*"), reverse=True):
            path.unlink() if path.is_file() else path.rmdir()
        OLD_TEXTURES.rmdir()


def main() -> None:
    write_models()
    write_textures()
    remove_old_textures()


if __name__ == "__main__":
    main()
