#!/usr/bin/env python3
"""Generate the Component Recycler blockstates, block models, part models, item models, and owned textures.

A powered Recycler is an open teardown bay. The item being recycled lies on a turntable on a grated
floor, under the installed Disassembly Head: a four-prong claw on a spindle that hangs from the bay
roof, with the spindle motor on top. Returns fall through the grate into three trays at the bottom of
the front, one per output slot. Behind the bay stands a drive cabinet with the cable port on its back,
a Battery Cell cradle and charge meter on its east side, and a cage on its west side that holds the
Recovery Filter cartridge. The roof header carries the status lamp and, across from it, the Job Chip
lamp (job_lamp.py); the Crude Recycler takes no Job Chips and has neither.

The claw and the cartridge are part models in the installed part's material, drawn by
ComponentRecyclerRenderer, whose constants match the geometry here; with no head installed only the
bare spindle shows. Every powered chassis shares one housing in its chassis metal, reusing the
Crusher's shell, trim, plinth, port, meter, and lamp textures. The Crude Recycler follows its recipe
instead: a plank crate on wooden slabs around a cobblestone quern, whose flint-studded runner stone
the Hand Crank on its lid turns. Run from the repository root:

    python tools/blockmodels/component_recycler.py
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

import job_lamp

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block/component_recycler"
OLD_FACE_FOLDERS = ("front", "side", "top", "bottom")
TEX = "rngtech:block/component_recycler/"
CRUSHER = "rngtech:block/crusher/"
FACES = ("north", "south", "east", "west", "up", "down")

# Teardown bay between the cheeks, its grated floor, and the roof the spindle hangs from.
BAY_X0, BAY_X1 = 2.5, 13.5
BAY_Z0, BAY_Z1 = 0.5, 9.5
GRATE_Y0, GRATE_Y1 = 5.5, 6.25
ROOF_Y0, ROOF_Y1 = 12.5, 13.5
# Turntable on the grate, and the claw's spindle above it. The claw part is built around (8, 8) in x and z.
TABLE_X, TABLE_Z = 8.0, 5.0
TABLE_R, TABLE_Y1 = 2.5, 7.0
PART_CENTRE = 8.0
# Return trays under the grate: one per output slot, west to east.
TRAYS = ((BAY_X0, 5.75), (6.25, 9.75), (10.25, BAY_X1))
TRAY_Y0, TRAY_Z1 = 2.25, 9.0
# Drive cabinet with the cable port, the charge meter and Battery Cell cradle on the east, and the
# Recovery Filter cage on the west.
CABINET = ((3.5, 2.0, 10.0), (12.5, ROOF_Y1, 15.0))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
METER = ((12.5, 6.0, 11.0), (12.75, 10.5, 14.0))
CAGE = ((0.75, 2.0, 10.5), (3.25, 10.0, 14.5))
# Crude Recycler: a cobblestone quern bed in a plank crate, and the runner stone turned by the Hand Crank.
QUERN_BED = ((3.0, 2.0, 4.0), (13.0, 5.0, 13.0))
RUNNER_Y0, RUNNER_Y1, RUNNER_R = 5.0, 7.0, 3.5
LID_Y0 = 14.5
# Job Chip lamp on the roof header, across from the status lamp.
JOB_LAMP = ((3.25, 12.75, 0.0), (5.0, 13.25, 0.25))
JOB_LAMP_FACES = ("north", "east", "west", "up", "down")

POWERED = ("iron", "copper", "bronze", "steel", "aluminum", "titanium", "tungstensteel", "exotic")
HEADS = POWERED
FILTERS = ("iron", "copper", "bronze", "steel")


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


def octagon(cx, cz, radius, y0, y1, textures, faces=FACES):
    """Two square slabs around (cx, cz), one turned 45 degrees, read as a round disc or drum."""
    half = radius / math.sqrt(2) * 1.08
    turned = {"rotation": {"origin": [r(cx), r((y0 + y1) / 2), r(cz)], "axis": "y", "angle": 45}}
    return [
        box((cx - half, y0, cz - half), (cx + half, y1, cz + half), textures, faces),
        box((cx - half + 0.05, y0 + 0.02, cz - half + 0.05), (cx + half - 0.05, y1 - 0.02, cz + half - 0.05), textures, faces,
            extra=turned),
    ]


# ---------------------------------------------------------------- parts

def claw_elements():
    """The Disassembly Head: a chuck collar on the spindle, a body, four prongs, and a centre driver bit."""
    c = PART_CENTRE
    elements = [
        box((c - 1.0, 11.5, c - 1.0), (c + 1.0, ROOF_Y0, c + 1.0), "body", faces=("north", "south", "east", "west")),
        box((c - 2.5, 10.0, c - 2.5), (c + 2.5, 11.5, c + 2.5), {"up": "body_top", "down": "body_top", "*": "body"},
            uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
        box((c - 0.5, 7.75, c - 0.5), (c + 0.5, 10.0, c + 0.5), "bit", faces=("north", "south", "east", "west", "down")),
    ]
    prong = {"*": "prong"}
    # West, east, north, and south prongs, each with a tip turned in toward the item.
    for (x0, z0, x1, z1), (tx0, tz0, tx1, tz1) in (
            ((c - 2.5, c - 0.6, c - 1.6, c + 0.6), (c - 1.9, c - 0.6, c - 1.1, c + 0.6)),
            ((c + 1.6, c - 0.6, c + 2.5, c + 0.6), (c + 1.1, c - 0.6, c + 1.9, c + 0.6)),
            ((c - 0.6, c - 2.5, c + 0.6, c - 1.6), (c - 0.6, c - 1.9, c + 0.6, c - 1.1)),
            ((c - 0.6, c + 1.6, c + 0.6, c + 2.5), (c - 0.6, c + 1.1, c + 0.6, c + 1.9)),
    ):
        elements.append(box((x0, 8.0, z0), (x1, 10.0, z1), prong, faces=("north", "south", "east", "west")))
        elements.append(box((tx0, 7.25, tz0), (tx1, 8.05, tz1), prong, faces=("north", "south", "east", "west", "down")))
    return elements


def filter_elements():
    """The Recovery Filter: a pleated cartridge standing in the west cage, with collars in the filter's metal."""
    (x0, y0, z0), (x1, y1, z1) = CAGE
    body = ((x0 + 0.65, y0 + 0.5, z0 + 0.65), (x1 - 0.65, y1 - 0.5, z1 - 0.65))
    elements = [box(*body, {"up": "cap", "down": "cap", "*": "pleat"})]
    for cy in (y0 + 1.0, y1 - 1.5):
        elements.append(box((x0 + 0.5, cy, z0 + 0.5), (x1 - 0.5, cy + 0.5, z1 - 0.5), "collar"))
    return elements


def runner_elements():
    """The Crude Recycler's runner stone: a flint-studded disc on the crank's square drive shaft."""
    elements = octagon(8.0, 8.0, RUNNER_R, RUNNER_Y0, RUNNER_Y1, {"up": "runner_top", "down": "runner_top", "*": "runner"})
    elements.append(box((7.4, RUNNER_Y1, 7.4), (8.6, LID_Y0, 8.6), "drive", faces=("north", "south", "east", "west")))
    return elements


# ---------------------------------------------------------------- powered housing

def powered_housing():
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    # Cheeks, lined inside, from the plinth to the roof.
    for x0, x1, inner in ((1.5, BAY_X0, "east"), (BAY_X1, 14.5, "west")):
        elements.append(box((x0, 2.0, BAY_Z0), (x1, ROOF_Y0, CABINET[0][2]), {inner: "liner", "*": "shell"}))
    # Ribs on the cheeks, in the chassis trim.
    for x0, x1 in ((1.0, 1.5), (14.5, 15.0)):
        elements.append(box((x0, 2.0, 1.5), (x1, ROOF_Y0, 2.75), "trim", faces=("north", "south", "east", "west")))

    # Return trays: a dark floor and back, trim dividers, and a lip along the front.
    elements.append(box((BAY_X0, 2.0, BAY_Z0 + 0.5), (BAY_X1, TRAY_Y0, TRAY_Z1), "tray", faces=("up",)))
    elements.append(box((BAY_X0, 2.0, TRAY_Z1), (BAY_X1, GRATE_Y0, TRAY_Z1 + 0.5), "tray", faces=("north",)))
    for x0 in (TRAYS[0][1], TRAYS[1][1]):
        elements.append(box((x0, 2.0, BAY_Z0 + 0.5), (x0 + 0.5, GRATE_Y0, TRAY_Z1), "trim", faces=("north", "east", "west")))
    elements.append(box((BAY_X0, 2.0, BAY_Z0), (BAY_X1, 2.75, BAY_Z0 + 0.5), "trim", faces=("north", "south", "up")))

    # Grated floor, the turntable on it, and the back wall of the bay.
    elements.append(box((BAY_X0, GRATE_Y0, BAY_Z0), (BAY_X1, GRATE_Y1, BAY_Z1), {"up": "grate", "down": "tray", "*": "trim"},
                        faces=("north", "up", "down"), uv={"up": [0, 0, 16, 16]}))
    elements += octagon(TABLE_X, TABLE_Z, TABLE_R, GRATE_Y1, TABLE_Y1, {"up": "table", "*": "shaft"})
    elements.append(box((BAY_X0, GRATE_Y1, BAY_Z1), (BAY_X1, ROOF_Y0, CABINET[0][2]), "liner", faces=("north",)))

    # Roof with a riveted header, the status and Job Chip lamps, the bare spindle, and the spindle motor on top.
    elements.append(box((1.0, ROOF_Y0, 0.25), (15.0, ROOF_Y1, CABINET[0][2]),
                        {"north": "header", "down": "liner", "up": "shell", "*": "shell"},
                        faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 14, 1]}))
    elements.append(box((11.0, 12.75, 0.0), (12.75, 13.25, 0.25), "lamp", faces=("north", "east", "west", "up", "down"),
                        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}))
    elements.append(job_lamp.element(*JOB_LAMP, JOB_LAMP_FACES))
    elements.append(box((TABLE_X - 0.6, 11.0, TABLE_Z - 0.6), (TABLE_X + 0.6, ROOF_Y0, TABLE_Z + 0.6), "shaft",
                        faces=("north", "south", "east", "west", "down")))
    elements.append(box((TABLE_X - 2.75, ROOF_Y1, TABLE_Z - 2.75), (TABLE_X + 2.75, 14.0, TABLE_Z + 2.75), "trim",
                        faces=("north", "south", "east", "west", "up")))
    elements += octagon(TABLE_X, TABLE_Z, 2.25, 14.0, 15.75, {"up": "motor_end", "*": "motor"},
                        faces=("north", "south", "east", "west", "up"))
    elements.append(box((TABLE_X - 1.9, 14.75, TABLE_Z - 1.9), (TABLE_X + 1.9, 15.25, TABLE_Z + 1.9), "band",
                        faces=("north", "south", "east", "west")))

    # Cabinet with a teal band along both sides, the port, and the charge meter.
    (gx0, gy0, gz0), (gx1, gy1, gz1) = CABINET
    elements.append(box((gx0, gy0, gz0), (gx1, gy1, gz1), "casing", faces=("south", "east", "west", "up")))
    for x0, x1 in ((gx0 - 0.25, gx0), (gx1, gx1 + 0.25)):
        elements.append(box((x0, gy1 - 1.5, gz0), (x1, gy1 - 1.0, gz1), "band", faces=("south", "east", "west", "up", "down")))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}))
    elements.append(box(*METER, {"east": "meter", "*": "trim"}, faces=("north", "south", "east", "up", "down"), uv={"east": [0, 0, 3, 4.5]}))

    # Battery Cell cradle on the east: a base and two saddles the cell lies across.
    elements.append(box((12.75, 2.0, 10.25), (15.25, 2.5, 15.25), "trim", faces=("north", "south", "east", "west", "up")))
    for z0 in (10.75, 14.0):
        elements.append(box((12.75, 2.5, z0), (13.25, 3.5, z0 + 0.75), "trim", faces=("north", "south", "east", "west", "up")))
        elements.append(box((14.75, 2.5, z0), (15.25, 3.5, z0 + 0.75), "trim", faces=("north", "south", "east", "west", "up")))

    # Recovery Filter cage on the west: a base, a lid with a feed pipe into the cabinet, and four corner posts.
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CAGE
    elements.append(box((cx0, cy0, cz0), (cx1, cy0 + 0.5, cz1), "trim", faces=("north", "south", "east", "west", "up")))
    elements.append(box((cx0, cy1 - 0.5, cz0), (cx1, cy1, cz1), "trim"))
    elements.append(box((cx1, cy1 - 1.5, 12.0), (gx0, cy1 - 0.5, 13.0), "shaft", faces=("north", "south", "up", "down")))
    for x in (cx0, cx1 - 0.5):
        for z in (cz0, cz1 - 0.5):
            elements.append(box((x, cy0 + 0.5, z), (x + 0.5, cy1 - 0.5, z + 0.5), "trim", faces=("north", "south", "east", "west")))
    return elements


# ---------------------------------------------------------------- crude housing

def crude_housing():
    """The Crude Recycler: wooden slabs, a plank crate with dark oak corner posts and a window in front, and a
    cobblestone quern bed inside. The crate lid carries the Hand Crank, whose shaft turns the runner stone."""
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "boards", "down": "boards", "*": "boards_edge"})]
    for x, z in ((0.5, 0.5), (14.0, 0.5), (0.5, 14.0), (14.0, 14.0)):
        elements.append(box((x, 2.0, z), (x + 1.5, LID_Y0, z + 1.5), {"down": "post_end", "up": "post_end", "*": "post"},
                            faces=("north", "south", "east", "west")))
    # Side and back walls of vertical boards, dark inside.
    elements.append(box((1.0, 2.0, 2.0), (2.0, LID_Y0, 14.0), {"east": "dark", "*": "boards_v"}, faces=("east", "west")))
    elements.append(box((14.0, 2.0, 2.0), (15.0, LID_Y0, 14.0), {"west": "dark", "*": "boards_v"}, faces=("east", "west")))
    elements.append(box((2.0, 2.0, 14.0), (14.0, LID_Y0, 15.0), {"north": "dark", "*": "boards_v"}, faces=("north", "south")))
    # Front: a low rail, a header board over the window, and the quern bed inside.
    elements.append(box((2.0, 2.0, 1.0), (14.0, 3.0, 2.0), "boards_edge", faces=("north", "south", "up")))
    elements.append(box((2.0, 11.5, 1.0), (14.0, LID_Y0, 2.0), {"north": "boards", "*": "boards_edge"}, faces=("north", "south", "down")))
    elements.append(box((2.0, 2.0, 2.0), (14.0, 2.25, 14.0), "dark", faces=("up",)))
    elements.append(box(*QUERN_BED, {"up": "cobble_top", "*": "cobble"}, faces=("north", "east", "west", "up")))
    # Plank lid with a copper-ringed bearing plate the Hand Crank stands on.
    elements.append(box((0.5, LID_Y0, 0.5), (15.5, 16.0, 15.5), {"up": "lid", "*": "boards_edge"}, uv={"up": [0, 0, 16, 16]}))
    return elements


# ---------------------------------------------------------------- models

POWERED_TEXTURES = {
    "casing": CRUSHER + "casing",
    "liner": CRUSHER + "liner",
    "shaft": CRUSHER + "shaft",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "meter": CRUSHER + "meter",
    "motor": CRUSHER + "motor",
    "motor_end": CRUSHER + "motor_end",
    "lamp": CRUSHER + "lamp_off",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "header": TEX + "header",
    "grate": TEX + "grate",
    "tray": TEX + "tray",
    "table": TEX + "table",
    **job_lamp.TEXTURE,
}
CRUDE_TEXTURES = {
    "particle": CRUSHER + "wood/boards",
    "boards": CRUSHER + "wood/boards",
    "boards_v": CRUSHER + "wood/boards_v",
    "boards_edge": CRUSHER + "wood/boards_edge",
    "dark": CRUSHER + "wood/dark",
    "post": CRUSHER + "wood/post",
    "post_end": CRUSHER + "wood/post_end",
    "cobble": TEX + "crude/cobble",
    "cobble_top": TEX + "crude/cobble_top",
    "lid": TEX + "crude/lid",
}
RUNNER_TEXTURES = {
    "runner": CRUSHER + "roll/flint",
    "runner_top": CRUSHER + "roll/flint_end",
    "drive": CRUSHER + "wood/post",
}


def chassis_textures(chassis):
    return {
        "particle": CRUSHER + f"shell/{chassis}",
        "shell": CRUSHER + f"shell/{chassis}",
        "trim": CRUSHER + f"trim/{chassis}",
    }


def head_textures(head):
    return {key: TEX + f"head/{head}_{key}" for key in ("body", "body_top", "prong", "bit")}


def filter_textures(metal):
    return {"pleat": TEX + "filter/pleat", "cap": TEX + "filter/cap", "collar": TEX + f"filter/{metal}_collar"}


def offset(elements, dx, dz):
    """Elements moved in x and z, keeping their position-derived UVs, so a part sits where the renderer draws it."""
    moved = []
    for element in elements:
        copy = json.loads(json.dumps(element))
        start, end = copy["from"], copy["to"]
        for face, entry in copy["faces"].items():
            if "uv" not in entry:
                entry["uv"] = default_uv(face, start, end)
        copy["from"] = [r(start[0] + dx), start[1], r(start[2] + dz)]
        copy["to"] = [r(end[0] + dx), end[1], r(end[2] + dz)]
        if "rotation" in copy:
            origin = copy["rotation"]["origin"]
            copy["rotation"]["origin"] = [r(origin[0] + dx), origin[1], r(origin[2] + dz)]
        moved.append(copy)
    return moved


def default_uv(face, start, end):
    (x1, y1, z1), (x2, y2, z2) = start, end
    return [r(v) for v in {
        "up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]]


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = powered_housing()
    write_json(MODELS / "block/component_recycler.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": POWERED_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "item/component_recycler_template.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": POWERED_TEXTURES,
        "elements": offset(claw_elements(), TABLE_X - PART_CENTRE, TABLE_Z - PART_CENTRE) + housing,
    })
    job_lamp.write_part(MODELS / "block/component_recycler/job_lamp.json", *JOB_LAMP, JOB_LAMP_FACES)
    for chassis in POWERED:
        name = f"{chassis}_component_recycler"
        job_lamp.write_blockstate(name, f"rngtech:block/{name}", f"rngtech:block/{name}_active",
                                  "rngtech:block/component_recycler/job_lamp")
        write_json(MODELS / f"block/{name}.json", {"parent": "rngtech:block/component_recycler", "textures": chassis_textures(chassis)})
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": CRUSHER + "lamp_on"}})
        write_json(MODELS / f"item/{name}.json", {
            "parent": "rngtech:item/component_recycler_template",
            "textures": {**chassis_textures(chassis), **head_textures(chassis)},
        })

    crude = crude_housing()
    write_json(MODELS / "block/crude_recycler.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": CRUDE_TEXTURES,
        "elements": crude,
    })
    write_json(MODELS / "block/crude_recycler_active.json", {"parent": "rngtech:block/crude_recycler"})
    write_json(MODELS / "item/crude_recycler.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**CRUDE_TEXTURES, **RUNNER_TEXTURES},
        "elements": runner_elements() + crude,
    })

    parts = MODELS / "block/component_recycler"
    write_json(parts / "claw.json", {"textures": {"particle": "#body"}, "elements": claw_elements()})
    for head in HEADS:
        write_json(parts / f"claw_{head}.json", {"parent": "rngtech:block/component_recycler/claw", "textures": head_textures(head)})
    write_json(parts / "filter.json", {"textures": {"particle": "#collar"}, "elements": filter_elements()})
    for metal in FILTERS:
        write_json(parts / f"filter_{metal}.json", {"parent": "rngtech:block/component_recycler/filter", "textures": filter_textures(metal)})
    write_json(parts / "runner.json", {"textures": {"particle": "#runner", **RUNNER_TEXTURES}, "elements": runner_elements()})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"component_recycler:{name}")


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


# Disassembly Head materials, after the Disassembly Head item palettes.
HEAD_COLORS = {
    "iron": (204, 204, 196),
    "copper": (212, 118, 60),
    "bronze": (196, 140, 62),
    "steel": (150, 156, 160),
    "aluminum": (200, 212, 224),
    "titanium": (156, 182, 214),
    "tungstensteel": (96, 104, 116),
    "exotic": (150, 90, 200),
}
# Recovery Filter collars, as on the Potential Reactor cartridge.
FILTER_COLORS = {
    "iron": (201, 201, 194),
    "copper": (217, 138, 90),
    "bronze": (199, 154, 82),
    "steel": (141, 152, 164),
}
EXOTIC_GLOW = (170, 96, 222)


def write_head_textures() -> None:
    for head, color in HEAD_COLORS.items():
        body_color = shade(mix(color, (60, 66, 76), 0.55), 0)
        body = noisy(64, body_color, f"head_{head}_body", amount=4)
        pixels = body.load()
        for y in range(64):
            for x in range(64):
                if y % 16 in (0, 1):
                    pixels[x, y] = (*shade(pixels[x, y], -22), 255)
                elif y % 16 == 2:
                    pixels[x, y] = (*shade(pixels[x, y], 14), 255)
        save(body, f"head/{head}_body")

        top = noisy(64, body_color, f"head_{head}_top", amount=4)
        draw = ImageDraw.Draw(top)
        draw.rectangle([0, 0, 63, 63], outline=(*shade(body_color, -30), 255), width=3)
        for cx, cy in ((10, 10), (53, 10), (10, 53), (53, 53)):
            draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(*shade(body_color, 30), 255))
        draw.regular_polygon((32, 32, 9), 6, fill=(*shade(body_color, -40), 255))
        save(top, f"head/{head}_body_top")

        # Prongs: the head's material, brighter toward the tips, with a worn grip edge.
        prong = noisy(64, color, f"head_{head}_prong", amount=6, streak=3)
        pixels = prong.load()
        for y in range(64):
            for x in range(64):
                value = shade(pixels[x, y], (y - 32) * 0.35)
                if x % 4 == 0:
                    value = shade(value, -24)
                pixels[x, y] = (*value, 255)
        if head == "exotic":
            for y in range(64):
                for x in range(64):
                    if x % 4 == 2:
                        pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.5), 255)
        save(prong, f"head/{head}_prong")

        bit = noisy(64, shade(mix(color, (40, 42, 48), 0.6), 0), f"head_{head}_bit", amount=4)
        pixels = bit.load()
        for y in range(64):
            for x in range(64):
                if (x + y) % 8 < 2:
                    pixels[x, y] = (*shade(pixels[x, y], 28), 255)
        save(bit, f"head/{head}_bit")


def write_filter_textures() -> None:
    # Pleated paper of the cartridge, with vertical folds.
    pleat = noisy(64, (204, 196, 170), "filter_pleat", amount=5)
    pixels = pleat.load()
    for y in range(64):
        for x in range(64):
            phase = x % 6
            if phase == 0:
                pixels[x, y] = (*shade(pixels[x, y], -42), 255)
            elif phase in (1, 2):
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    flecks(pleat, "filter_grime", (120, 108, 84), 60, 10)
    save(pleat, "filter/pleat")
    cap = noisy(64, (90, 96, 104), "filter_cap", amount=4)
    draw = ImageDraw.Draw(cap)
    draw.ellipse([14, 14, 49, 49], outline=(60, 64, 70, 255), width=3)
    draw.ellipse([26, 26, 37, 37], fill=(28, 30, 34, 255))
    save(cap, "filter/cap")
    for metal, color in FILTER_COLORS.items():
        collar = noisy(64, color, f"filter_{metal}_collar", amount=6, streak=4)
        pixels = collar.load()
        for x in range(64):
            for y in range(64):
                if y % 8 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -26), 255)
                elif y % 8 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 18), 255)
        save(collar, f"filter/{metal}_collar")


def write_textures() -> None:
    # Grate, mapped whole on the bay floor: bars over the dark trays, with a rim.
    grate = Image.new("RGBA", (64, 64), (20, 22, 26, 255))
    pixels = grate.load()
    rand = rng("grate")
    for y in range(64):
        for x in range(64):
            if x % 8 in (0, 1, 2) or y < 3 or y > 60 or x > 60:
                base = (96, 102, 112) if x % 8 != 2 else (70, 74, 82)
                pixels[x, y] = (*shade(base, rand.uniform(-6, 6)), 255)
            elif y % 16 == 8:
                pixels[x, y] = (62, 66, 74, 255)
    save(grate, "grate")

    tray = noisy(64, (34, 37, 42), "tray", amount=4)
    flecks(tray, "tray_grit", (70, 70, 72), 50, 8)
    save(tray, "tray")

    # Turntable top: a ribbed steel disc with an index notch.
    table = noisy(64, (110, 118, 128), "table", amount=4)
    draw = ImageDraw.Draw(table)
    for radius in (28, 20, 12):
        draw.ellipse([32 - radius, 32 - radius, 32 + radius, 32 + radius], outline=(76, 82, 90, 255), width=2)
    draw.rectangle([30, 2, 33, 10], fill=(47, 179, 194, 255))
    draw.ellipse([28, 28, 35, 35], fill=(56, 60, 68, 255))
    save(table, "table")

    # Header face, mapped as a 14:1 strip: a riveted beam.
    header = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    header.paste(noisy(64, (88, 94, 102), "header", amount=4).crop((0, 0, 64, 4)), (0, 0))
    hp = header.load()
    for x in range(56):
        hp[x, 0] = (120, 128, 138, 255)
        hp[x, 3] = (50, 54, 60, 255)
    for x in range(3, 56, 8):
        hp[x, 1] = (160, 168, 178, 255)
        hp[x + 1, 2] = (44, 48, 54, 255)
    save(header, "header")

    write_head_textures()
    write_filter_textures()
    write_crude_textures()


def write_crude_textures() -> None:
    # Cobblestone of the quern bed: rounded stones in dark mortar.
    rand = rng("cobble")
    cobble = Image.new("RGBA", (64, 64), (70, 70, 70, 255))
    pixels = cobble.load()
    seeds = [(rand.uniform(0, 64), rand.uniform(0, 64), rand.uniform(-18, 18)) for _ in range(26)]
    for y in range(64):
        for x in range(64):
            best, second, tone = 1e9, 1e9, 0.0
            for sx, sy, t in seeds:
                dx = min(abs(x - sx), 64 - abs(x - sx))
                dy = min(abs(y - sy), 64 - abs(y - sy))
                d = math.hypot(dx, dy)
                if d < best:
                    best, second, tone = d, best, t
                elif d < second:
                    second = d
            if second - best < 1.6:
                value = (62, 62, 64)
            else:
                value = shade((128, 128, 126), tone + rand.uniform(-6, 6) - best * 0.6)
            pixels[x, y] = (*value, 255)
    save(cobble, "crude/cobble")
    top = cobble.copy()
    draw = ImageDraw.Draw(top)
    draw.ellipse([8, 8, 55, 55], outline=(84, 82, 78, 255), width=2)
    save(top, "crude/cobble_top")

    # Lid: oak boards with a copper-ringed bearing plate in the middle, mapped whole.
    lid = noisy(64, (108, 70, 38), "crude_lid", amount=6)
    pixels = lid.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (52, 32, 18, 255)
            elif y % 16 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    draw = ImageDraw.Draw(lid)
    draw.rectangle([16, 16, 47, 47], fill=(86, 54, 30, 255), outline=(52, 32, 18, 255), width=2)
    draw.ellipse([22, 22, 41, 41], outline=(196, 116, 56, 255), width=2)
    draw.rectangle([29, 29, 34, 34], fill=(28, 18, 12, 255))
    for x, y in ((18, 18), (44, 18), (18, 44), (44, 44)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(196, 116, 56, 255))
    save(lid, "crude/lid")


def remove_old_textures() -> None:
    """The cube faces this design replaces."""
    for folder in OLD_FACE_FOLDERS:
        path = TEXTURES / folder
        if path.is_dir():
            for file in sorted(path.rglob("*"), reverse=True):
                file.unlink() if file.is_file() else file.rmdir()
            path.rmdir()


def main() -> None:
    write_models()
    write_textures()
    remove_old_textures()


if __name__ == "__main__":
    main()
