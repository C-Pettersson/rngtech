#!/usr/bin/env python3
"""Generate the Component Assembler blockstate, block model, part models, item model, and owned textures.

The Component Assembler (resource id battery_assembler) is an assembly bed on the slate plinth. Its
four input slots are four jig pockets along the front of the bed, and the part being assembled stands
on a copper-contact nest behind them. A gantry on four cell-shell posts, after the Cell Shells in its
recipe, carries a bridge, a carriage, and a quill with a gripper and dosing nozzle. While it works,
the head picks from each filled pocket in turn and sets down on the nest, then doses the assembly
fluid. Behind the bed, a cabinet holds the four-slot output rack as four cubbies, the assembly fluid in
a glass reservoir lying on its roof, the Battery Cell in a cradle beside the reservoir with its charge
meter on the east face, and the cable port on its back. The status lamp and the Job Chip lamp
(job_lamp.py) sit on the cabinet front above the cubbies.

The bed, gantry frame, cabinet, and reservoir frame are a static block model. The bridge, carriage,
and quill are part models moved by BatteryAssemblerRenderer, which also draws the items, the fluid,
the dosing stream, the cell, and the meter; its constants match the geometry here. Run from the
repository root:

    python tools/blockmodels/battery_assembler.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

import job_lamp

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block/battery_assembler"
OLD_TEXTURES = ("front.png", "front_active.png", "bottom.png")
TEX = "rngtech:block/battery_assembler/"
CRUSHER = "rngtech:block/crusher/"
FURNACE = "rngtech:block/furnace/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# Assembly bed across the front; the front faces north.
BED = ((1.0, 2.0, 0.75), (15.0, 5.0, 10.5))
BED_TOP = BED[1][1]
# Four jig pockets along the front, one per input slot, and the nest behind them. The renderer fills them east to
# west, so the first slot is on the left seen from the front.
POCKET_XS = (3.9, 6.6, 9.4, 12.1)
POCKET_HALF = 1.1
POCKET_Z0, POCKET_Z1 = 2.0, 4.5
POCKET_DEPTH = 0.25
# The nest is a shallow copper basin: the delivered items lie on its floor, and the poured fluid stays under its lip.
NEST = ((6.25, BED_TOP, 6.0), (9.75, 5.75, 9.0))
NEST_FLOOR = 5.25
NEST_LIP = 0.25
# Gantry posts on the bed corners, the side rails along z on top of them, and the bridge travel.
POSTS = ((1.0, 1.0), (13.5, 1.0), (1.0, 8.5), (13.5, 8.5))
POST_SIZE = 1.5
POST_Y1 = 12.5
RAIL_Y1 = 13.5
# Bridge part: spans x between the rails, built around z = 8; the carriage rides it, built around x = 8.
BRIDGE = ((2.5, 12.5, 7.5), (13.5, 13.25, 8.5))
CARRIAGE = ((7.0, 11.75, 7.0), (9.0, 13.75, 9.0))
# Quill: a rod hanging from the carriage with the gripper and nozzle at its tip, built around (8, 8).
# The rod reaches up into the carriage, so it stays inside it from the idle lift down to the deepest dip.
QUILL_Y0, QUILL_Y1 = 7.75, 13.5
HEAD = ((7.25, 7.0, 7.25), (8.75, 7.75, 8.75))
# Cabinet behind the bed with the output cubbies on its front, filled east to west like the pockets.
CABINET = ((1.0, 2.0, 10.5), (15.0, 12.0, 15.0))
CUBBIES = ((1.75, 4.5), (5.0, 7.75), (8.25, 11.0), (11.5, 14.25))
CUBBY_Y0, CUBBY_Y1 = 6.0, 9.5
CUBBY_DEPTH = 2.0
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
METER = ((15.0, 6.0, 11.5), (15.25, 10.5, 14.5))
# Reservoir lying along x on the cabinet roof: copper end caps and four rails; the fluid fills the inside.
TANK = ((1.75, 12.0, 11.0), (11.75, 15.5, 14.5))
TANK_CAP = 0.75
TANK_RAIL = 0.5
# Battery Cell cradle on the roof beside the reservoir; the cell lies along z.
CRADLE = ((12.25, 12.0, 11.0), (14.75, 12.5, 14.75))
SADDLE_ZS = (11.5, 13.75)
# Job Chip lamp on the cabinet front, across from the status lamp and clear of the gantry post.
JOB_LAMP = ((2.75, CUBBY_Y1 + 0.5, CABINET[0][2] - 0.25), (4.25, CUBBY_Y1 + 1.25, CABINET[0][2]))
JOB_LAMP_FACES = ("north", "east", "west", "up", "down")


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


def lamp(start, end):
    return box(start, end, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}})


# ---------------------------------------------------------------- parts

def bridge_elements():
    """The bridge beam between the side rails, with a teal band, and trucks over each rail."""
    (x0, y0, z0), (x1, y1, z1) = BRIDGE
    return [
        box((x0, y0, z0), (x1, y1, z1), {"*": "beam"}, faces=("north", "south", "up", "down")),
        box((x0 + 0.5, y1 - 0.4, z0 - 0.05), (x1 - 0.5, y1 - 0.15, z1 + 0.05), "band", faces=("north", "south")),
        box((1.0, y1, z0 - 0.25), (2.5, y1 + 0.5, z1 + 0.25), "truck", faces=SIDES + ("up",)),
        box((13.5, y1, z0 - 0.25), (15.0, y1 + 0.5, z1 + 0.25), "truck", faces=SIDES + ("up",)),
    ]


def carriage_elements():
    """The carriage riding the bridge, with the quill's bearing collar closed round the rod under it."""
    (x0, y0, z0), (x1, y1, z1) = CARRIAGE
    return [
        box((x0, BRIDGE[1][1], z0), (x1, y1, z1), {"up": "carriage_top", "*": "carriage"}, uv={"up": [0, 0, 16, 16]}),
        box((x0, y0, z0), (x1, BRIDGE[0][1], z0 + 0.5), "carriage", faces=SIDES + ("down",)),
        box((x0, y0, z1 - 0.5), (x1, BRIDGE[0][1], z1), "carriage", faces=SIDES + ("down",)),
        box((x0, y0, z0 + 0.5), (x0 + 0.5, BRIDGE[0][1], z1 - 0.5), "carriage", faces=("east", "west", "down")),
        box((x1 - 0.5, y0, z0 + 0.5), (x1, BRIDGE[0][1], z1 - 0.5), "carriage", faces=("east", "west", "down")),
    ]


def quill_elements():
    """The quill: a rod, a gripper block with two copper fingers, and the dosing nozzle under it."""
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HEAD
    return [
        box((7.75, QUILL_Y0, 7.75), (8.25, QUILL_Y1, 8.25), "quill", faces=SIDES),
        box((hx0, hy0, hz0), (hx1, hy1, hz1), {"down": "head_bottom", "*": "head"}, uv={"down": [0, 0, 16, 16]}),
        box((hx0 - 0.25, hy0 - 0.75, 7.75), (hx0 + 0.25, hy1 - 0.25, 8.25), "finger", faces=SIDES + ("down",)),
        box((hx1 - 0.25, hy0 - 0.75, 7.75), (hx1 + 0.25, hy1 - 0.25, 8.25), "finger", faces=SIDES + ("down",)),
        box((7.8, hy0 - 0.5, 7.8), (8.2, hy0, 8.2), "nozzle", faces=SIDES + ("down",)),
    ]


# ---------------------------------------------------------------- housing

def bed_elements():
    (bx0, by0, bz0), (bx1, by1, bz1) = BED
    elements = [box((bx0, by0, bz0), (bx1, by1 - POCKET_DEPTH, bz1), {"up": "bed", "*": "shell"}, faces=SIDES + ("up",))]
    # The top skin around the pockets, so each pocket is a shallow recess.
    top0 = by1 - POCKET_DEPTH
    xs = [bx0]
    for x in POCKET_XS:
        xs += [x - POCKET_HALF, x + POCKET_HALF]
    xs.append(bx1)
    for i in range(0, len(xs), 2):
        elements.append(box((xs[i], top0, POCKET_Z0), (xs[i + 1], by1, POCKET_Z1), "bed", faces=("up", "east", "west")))
    elements.append(box((bx0, top0, bz0), (bx1, by1, POCKET_Z0), "bed", faces=("up", "north", "south", "east", "west")))
    elements.append(box((bx0, top0, POCKET_Z1), (bx1, by1, bz1), "bed", faces=("up", "north", "east", "west")))
    for x in POCKET_XS:
        elements.append(box((x - POCKET_HALF, top0, POCKET_Z0), (x + POCKET_HALF, top0 + 0.01, POCKET_Z1), "pocket", faces=("up",),
                            uv={"up": [0, 0, 16, 16]}))
    # Copper trim along the front edge of the bed, with two conductive plates on the front face.
    elements.append(box((bx0, by1 - 0.75, bz0 - 0.25), (bx1, by1, bz0), "trim", faces=("north", "east", "west", "up", "down")))
    for x0, x1 in ((2.5, 6.5), (9.5, 13.5)):
        elements.append(box((x0, 2.75, bz0 - 0.25), (x1, 3.75, bz0), {"north": "plate", "*": "trim"},
                            faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 16, 4]}))
    # The nest: a basin floor with copper contacts, walled by a copper lip.
    (nx0, ny0, nz0), (nx1, ny1, nz1) = NEST
    w = NEST_LIP
    elements.append(box((nx0, ny0, nz0), (nx1, NEST_FLOOR, nz1), {"up": "nest", "*": "trim"}, faces=SIDES + ("up",),
                        uv={"up": [0, 0, 16, 16]}))
    elements.append(box((nx0, NEST_FLOOR, nz0), (nx1, ny1, nz0 + w), "trim", faces=SIDES + ("up",)))
    elements.append(box((nx0, NEST_FLOOR, nz1 - w), (nx1, ny1, nz1), "trim", faces=SIDES + ("up",)))
    elements.append(box((nx0, NEST_FLOOR, nz0 + w), (nx0 + w, ny1, nz1 - w), "trim", faces=("east", "west", "up")))
    elements.append(box((nx1 - w, NEST_FLOOR, nz0 + w), (nx1, ny1, nz1 - w), "trim", faces=("east", "west", "up")))
    return elements


def gantry_elements():
    elements = []
    for x, z in POSTS:
        # A cell-shell post: a canister with end rims, standing on the bed.
        elements.append(box((x, BED_TOP, z), (x + POST_SIZE, POST_Y1, z + POST_SIZE), {"*": "post", "up": "post_end"}, faces=SIDES))
        for y0 in (BED_TOP, POST_Y1 - 0.75):
            elements.append(box((x - 0.1, y0, z - 0.1), (x + POST_SIZE + 0.1, y0 + 0.75, z + POST_SIZE + 0.1), "trim",
                                faces=SIDES + ("up", "down")))
    # Side rails along z on the posts; the bridge rides them.
    for x in (1.0, 13.5):
        elements.append(box((x, POST_Y1, 1.0), (x + POST_SIZE, RAIL_Y1, 10.0), {"up": "rail_top", "*": "rail"},
                            uv={"up": [0, 0, 3, 16]}))
    return elements


def cabinet_elements():
    (gx0, gy0, gz0), (gx1, gy1, gz1) = CABINET
    elements = [
        # Cabinet shell, split around the four cubbies on its front.
        box((gx0, gy0, gz0), (gx1, CUBBY_Y0, gz1), {"up": "casing", "*": "casing"}, faces=("north", "south", "east", "west")),
        box((gx0, CUBBY_Y1, gz0), (gx1, gy1, gz1), "casing", faces=("north", "south", "east", "west", "up", "down")),
        box((gx0, CUBBY_Y0, gz0 + CUBBY_DEPTH), (gx1, CUBBY_Y1, gz1), "casing", faces=("north", "south", "east", "west")),
    ]
    edges = [gx0] + [x for pair in CUBBIES for x in pair] + [gx1]
    for i in range(0, len(edges), 2):
        if edges[i + 1] > edges[i]:
            elements.append(box((edges[i], CUBBY_Y0, gz0), (edges[i + 1], CUBBY_Y1, gz0 + CUBBY_DEPTH), "casing",
                                faces=("north", "east", "west")))
    for x0, x1 in CUBBIES:
        elements.append(box((x0, CUBBY_Y0 - 0.01, gz0), (x1, CUBBY_Y0, gz0 + CUBBY_DEPTH), "cubby", faces=("up",)))
        elements.append(box((x0, CUBBY_Y0, gz0 + CUBBY_DEPTH), (x1, CUBBY_Y1, gz0 + CUBBY_DEPTH + 0.01), "cubby", faces=("north",)))
        # Copper lip along the front of each cubby.
        elements.append(box((x0, CUBBY_Y0, gz0 - 0.25), (x1, CUBBY_Y0 + 0.5, gz0 + 0.25), "trim", faces=("north", "up", "east", "west")))
    # Teal band and copper trim round the cabinet top, the port, the meter, and the lamps.
    elements.append(box((gx0 - 0.1, gy1 - 1.25, gz0 - 0.1), (gx1 + 0.1, gy1 - 0.75, gz1 + 0.1), "band", faces=SIDES))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}))
    elements.append(box(*METER, {"east": "meter", "*": "trim"}, faces=("north", "south", "east", "up", "down"), uv={"east": [0, 0, 3, 4.5]}))
    elements.append(lamp((12.5, CUBBY_Y1 + 0.5, gz0 - 0.25), (14.0, CUBBY_Y1 + 1.25, gz0)))
    elements.append(job_lamp.element(*JOB_LAMP, JOB_LAMP_FACES))
    return elements


def tank_elements():
    """The reservoir: copper end caps, four rails, and glass between them, with a feed pipe down into the cabinet."""
    (tx0, ty0, tz0), (tx1, ty1, tz1) = TANK
    elements = [
        box((tx0, ty0, tz0), (tx0 + TANK_CAP, ty1, tz1), {"west": "tank_end", "*": "trim"}, uv={"west": [0, 0, 16, 16]}),
        box((tx1 - TANK_CAP, ty0, tz0), (tx1, ty1, tz1), {"east": "tank_end", "*": "trim"}, uv={"east": [0, 0, 16, 16]}),
    ]
    for y in (ty0, ty1 - TANK_RAIL):
        for z in (tz0, tz1 - TANK_RAIL):
            elements.append(box((tx0 + TANK_CAP, y, z), (tx1 - TANK_CAP, y + TANK_RAIL, z + TANK_RAIL), "tank_rail",
                                faces=SIDES + ("up", "down")))
    inner = ((tx0 + TANK_CAP, ty0 + 0.05, tz0 + 0.05), (tx1 - TANK_CAP, ty1 - 0.05, tz1 - 0.05))
    elements.append(box(*inner, "glass", faces=("north", "south", "up"),
                        uv={"north": [0, 0, 16, 7], "south": [0, 0, 16, 7], "up": [0, 0, 16, 7]}))
    # Saddles under the tank.
    for x in (tx0 + 2.0, tx1 - 2.75):
        elements.append(box((x, CABINET[1][1], tz0 + 0.5), (x + 0.75, ty0, tz1 - 0.5), "trim", faces=SIDES))
    return elements


def cradle_elements():
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CRADLE
    elements = [box((cx0, cy0, cz0), (cx1, cy1, cz1), "trim", faces=SIDES + ("up",))]
    for z0 in SADDLE_ZS:
        elements.append(box((cx0, cy1, z0), (cx0 + 0.5, cy1 + 1.0, z0 + 0.75), "trim", faces=SIDES + ("up",)))
        elements.append(box((cx1 - 0.5, cy1, z0), (cx1, cy1 + 1.0, z0 + 0.75), "trim", faces=SIDES + ("up",)))
    return elements


def housing():
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    elements += bed_elements()
    elements += gantry_elements()
    elements += cabinet_elements()
    elements += tank_elements()
    elements += cradle_elements()
    return elements


# ---------------------------------------------------------------- models

HOUSING_TEXTURES = {
    "particle": CRUSHER + "shell/iron",
    "shell": CRUSHER + "shell/iron",
    "trim": CRUSHER + "trim/copper",
    "casing": CRUSHER + "casing",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "meter": CRUSHER + "meter",
    "lamp": CRUSHER + "lamp_off",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "glass": FURNACE + "glass",
    "bed": TEX + "bed",
    "pocket": TEX + "pocket",
    "nest": TEX + "nest",
    "plate": TEX + "plate",
    "post": TEX + "post",
    "post_end": TEX + "post_end",
    "rail": TEX + "rail",
    "rail_top": TEX + "rail_top",
    "cubby": TEX + "cubby",
    "tank_end": TEX + "tank_end",
    "tank_rail": TEX + "rail",
    **job_lamp.TEXTURE,
}
PART_TEXTURES = {
    "beam": TEX + "beam",
    "band": CRUSHER + "band",
    "truck": CRUSHER + "trim/copper",
    "carriage": CRUSHER + "casing",
    "carriage_top": TEX + "carriage_top",
    "quill": CRUSHER + "shaft",
    "head": TEX + "head",
    "head_bottom": TEX + "head_bottom",
    "finger": CRUSHER + "trim/copper",
    "nozzle": FURNACE + "pipe",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    elements = housing()
    write_json(MODELS / "block/battery_assembler.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": elements,
    })
    write_json(MODELS / "block/battery_assembler_active.json", {
        "parent": "rngtech:block/battery_assembler", "textures": {"lamp": CRUSHER + "lamp_on"}})
    job_lamp.write_part(MODELS / "block/battery_assembler/job_lamp.json", *JOB_LAMP, JOB_LAMP_FACES)
    job_lamp.write_blockstate("battery_assembler", "rngtech:block/battery_assembler", "rngtech:block/battery_assembler_active",
                              "rngtech:block/battery_assembler/job_lamp")
    # The item shows the head parked over the nest.
    write_json(MODELS / "item/battery_assembler.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, **PART_TEXTURES},
        "elements": bridge_elements() + carriage_elements() + quill_elements() + elements,
    })
    parts = MODELS / "block/battery_assembler"
    for name, build in (("bridge", bridge_elements), ("carriage", carriage_elements), ("quill", quill_elements)):
        write_json(parts / f"{name}.json", {"textures": {"particle": "#" + ("beam" if name == "bridge" else "carriage" if name == "carriage" else "head"),
                                                         **PART_TEXTURES}, "elements": build()})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"battery_assembler:{name}")


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


IRON = (176, 176, 168)
COPPER = (196, 116, 56)
COPPER_DARK = (120, 64, 30)
REDSTONE = (196, 28, 20)
REDSTONE_DARK = (110, 14, 10)
BED_STEEL = (70, 76, 86)


def write_textures() -> None:
    # Bed top: a dark jig plate with a scribed grid and redstone traces running to the nest.
    bed = noisy(64, BED_STEEL, "bed", amount=4)
    pixels = bed.load()
    for y in range(64):
        for x in range(64):
            if x % 8 == 0 or y % 8 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -12), 255)
    for x in range(64):
        for y in (22, 23):
            pixels[x, y] = (*(REDSTONE if y == 22 else REDSTONE_DARK), 255)
    for y in range(22, 64):
        for x in (30, 31):
            pixels[x, y] = (*(REDSTONE if x == 30 else REDSTONE_DARK), 255)
    save(bed, "bed")

    # Jig pocket floor: a darker recess with a copper locating ring.
    pocket = noisy(64, (40, 44, 52), "pocket", amount=4)
    draw = ImageDraw.Draw(pocket)
    draw.rectangle([0, 0, 63, 63], outline=(26, 28, 34, 255), width=4)
    draw.ellipse([16, 16, 47, 47], outline=(*COPPER, 255), width=3)
    save(pocket, "pocket")

    # Nest top: copper contacts on a dark insulator, mapped whole.
    nest = noisy(64, (52, 56, 64), "nest", amount=4)
    draw = ImageDraw.Draw(nest)
    draw.rectangle([0, 0, 63, 63], outline=(*COPPER_DARK, 255), width=3)
    for x in (12, 26, 40):
        draw.rectangle([x, 14, x + 9, 49], fill=(*COPPER, 255), outline=(*COPPER_DARK, 255))
    save(nest, "nest")

    # Conductive plate on the bed front, mapped as a 4:1 strip.
    plate = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    strip = noisy(64, (92, 132, 196), "plate", amount=6).crop((0, 0, 64, 16))
    plate.paste(strip, (0, 0))
    pixels = plate.load()
    for x in range(64):
        pixels[x, 0] = (150, 186, 236, 255)
        pixels[x, 15] = (46, 70, 112, 255)
    for x in range(4, 64, 12):
        pixels[x, 7] = (210, 226, 246, 255)
        pixels[x, 8] = (46, 70, 112, 255)
    save(plate, "plate")

    # Cell-shell post: an iron canister with rolled seams and a vent slot.
    post = noisy(64, IRON, "post", amount=5, streak=2)
    pixels = post.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -36), 255)
            elif y % 32 in (0, 1):
                pixels[x, y] = (*shade(pixels[x, y], -28), 255)
            elif 6 <= x % 16 <= 9 and 10 <= y % 32 <= 21:
                pixels[x, y] = (40, 42, 46, 255)
    save(post, "post")
    post_end = noisy(64, shade(IRON, -20), "post_end", amount=4)
    draw = ImageDraw.Draw(post_end)
    draw.ellipse([8, 8, 55, 55], outline=(*shade(IRON, -60), 255), width=3)
    save(post_end, "post_end")

    # Gantry rails and the bridge beam: dark steel with a polished running strip.
    rail = noisy(64, (88, 94, 104), "rail", amount=4)
    pixels = rail.load()
    for x in range(64):
        pixels[x, 0] = (130, 138, 150, 255)
        pixels[x, 63] = (44, 48, 54, 255)
    save(rail, "rail")
    rail_top = noisy(64, (88, 94, 104), "rail_top", amount=4)
    pixels = rail_top.load()
    for y in range(64):
        for x in (5, 6):
            pixels[x, y] = (180, 188, 198, 255)
    save(rail_top, "rail_top")
    beam = noisy(64, (96, 102, 112), "beam", amount=4, streak=2)
    pixels = beam.load()
    for x in range(64):
        for y in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -26), 255)
    save(beam, "beam")

    # Carriage top and the gripper head.
    top = noisy(64, (50, 56, 66), "carriage_top", amount=4)
    draw = ImageDraw.Draw(top)
    draw.rectangle([0, 0, 63, 63], outline=(30, 34, 40, 255), width=4)
    draw.ellipse([22, 22, 41, 41], fill=(120, 128, 140, 255), outline=(30, 34, 40, 255), width=2)
    save(top, "carriage_top")
    head = noisy(64, (110, 118, 130), "head", amount=4)
    pixels = head.load()
    for x in range(64):
        for y in range(64):
            if y < 8:
                pixels[x, y] = (47, 179, 194, 255) if y < 6 else (22, 92, 104, 255)
    save(head, "head")
    bottom = noisy(64, (70, 76, 86), "head_bottom", amount=4)
    draw = ImageDraw.Draw(bottom)
    draw.ellipse([24, 24, 39, 39], fill=(24, 26, 30, 255))
    save(bottom, "head_bottom")

    # Output cubby lining.
    cubby = noisy(64, (34, 38, 44), "cubby", amount=4)
    save(cubby, "cubby")

    # Reservoir end cap: a copper disc with a filler plug.
    end = noisy(64, COPPER, "tank_end", amount=6)
    draw = ImageDraw.Draw(end)
    draw.rectangle([0, 0, 63, 63], outline=(*COPPER_DARK, 255), width=4)
    draw.ellipse([20, 20, 43, 43], fill=(*shade(COPPER, 24), 255), outline=(*COPPER_DARK, 255), width=3)
    save(end, "tank_end")


def remove_old_textures() -> None:
    """The cube faces this design replaces."""
    for name in OLD_TEXTURES:
        path = TEXTURES / name
        if path.is_file():
            path.unlink()


def main() -> None:
    write_models()
    write_textures()
    remove_old_textures()


if __name__ == "__main__":
    main()
