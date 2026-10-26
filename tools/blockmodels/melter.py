#!/usr/bin/env python3
"""Generate the Melter blockstate, block model, part models, item model, and owned textures.

The Melter is a sealed crucible on the slate plinth. A rounded titanium vessel stands in the middle
with a bolted porthole in its front: behind it, a crucible on the refractory floor holds the melt in
progress. A heat bar under the porthole reads the vessel's heat against the Heat Core's limit. The
Heat Core is two banks of fins on the vessel's flanks, between two mounting rails. On the bolted lid,
a feed hopper holds the ingredients on the Crush Head, a pair of small toothed rolls in the head's
material, and an installed Servo is the motor beside the hopper that drives them. The input and output
tanks are glass sight columns on the front corners. The Fluid Pump stands in the back-east corner and
feeds the output column through a pipe along the plinth; the Battery Cell lies in a cradle in the
back-west corner, with its charge meter on the drive cabinet that carries the cable port on its back.
The status lamp and the Job Chip lamp (job_lamp.py) sit on the front of the flange.

The vessel and its fixtures are a static block model. The rolls, fins, pump, and motor are part models
in the installed part's material; they, the fluids, the melt, the ingredients, the heat glow, and the
meters are drawn by MelterRenderer, whose constants match the geometry here. Run from the repository
root:

    python tools/blockmodels/melter.py
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
TEXTURES = ASSETS / "textures/block/melter"
OLD_FACE_FOLDERS = ("front", "side", "top", "bottom")
TEX = "rngtech:block/melter/"
CRUSHER = "rngtech:block/crusher/"
FURNACE = "rngtech:block/furnace/"
# Heat Cores and Servos are the same items in other machines, so they keep those machines' textures.
CORE_TEX = "rngtech:block/solid_fuel_burner/heat_core/"
SERVO_TEX = "rngtech:block/compressor_tank/servo/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# Vessel: two crossed slabs, the narrow one deeper, read as a rounded drum. The front faces north.
VESSEL_X = 8.0
NARROW = ((4.5, 2.0), (11.5, 12.5))
WIDE = ((3.0, 3.75), (13.0, 10.75))
VESSEL_Y0, BELLY_Y0, BELLY_Y1, VESSEL_Y1 = 2.0, 4.5, 9.0, 11.0
# Melt chamber behind the porthole, open at the front.
CHAMBER = ((5.5, BELLY_Y0, NARROW[0][1]), (10.5, BELLY_Y1, 8.5))
# Crucible on the chamber floor: walls around the pool, which rises from the floor to the brim.
CRUCIBLE = ((6.0, BELLY_Y0, 3.0), (10.0, 6.5, 7.75))
CRUCIBLE_WALL = 0.5
POOL_Y0 = BELLY_Y0 + 0.25
# Porthole bezel on the vessel front, and the heat bar under it.
BEZEL_Z = 1.5
GLASS_Z = 1.75
HEAT_BAR = ((5.5, 2.5), (10.5, 3.5))
HEAT_BAR_Z = 1.75
# Flange, domed lid, and top plate.
FLANGE_Y1 = 11.75
LID_Y1 = 12.75
# Feed hopper on the lid, open on top; the Crush Head is two rolls along x inside it.
HOPPER = ((5.0, LID_Y1, 4.25), (11.0, 15.25, 10.25))
HOPPER_WALL = 0.5
ROLL_Y = 13.75
ROLL_ZS = (6.0, 8.5)
ROLL_X0, ROLL_X1 = 5.5, 10.5
ROLL_TIP = 1.15
PART_CENTRE = 8.0
# Servo motor east of the hopper, on the axis of the front roll.
SERVO = ((11.0, 12.75, 5.0), (13.5, 14.75, 7.0))
# Heat Core fins on both flanks of the vessel, between the mounting rails.
FIN_ZS = (4.75, 6.0, 7.25, 8.5, 9.75)
FIN_WIDTH = 0.5
FIN_Y0, FIN_Y1 = 4.5, 9.75
FIN_SPANS = ((1.25, WIDE[0][0]), (WIDE[1][0], 14.75))
RAIL_YS = ((4.0, 4.5), (9.75, 10.25))
# Sight columns on the front corners: input on the west, output on the east. The fluid fills the inside.
COLUMN_IN = ((0.75, 0.5), (3.75, 3.5))
COLUMN_OUT = ((12.25, 0.5), (15.25, 3.5))
COLUMN_Y0, COLUMN_Y1 = 2.0, 12.0
CAP = 1.0
RAIL = 0.5
# Fluid Pump in the back-east corner, feeding the output column along the plinth.
PUMP_X, PUMP_Z = 13.875, 13.625
PUMP_R = 1.75
PUMP_Y0, PUMP_Y1 = 2.5, 5.5
PIPE_X = (13.25, 14.25)
PIPE_Y = (2.0, 3.0)
# Drive cabinet behind the vessel, with the cable port on its back and the charge meter on its west face.
CABINET = ((4.0, 2.0, NARROW[1][1]), (12.0, 11.0, 15.0))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
METER = ((3.75, 5.0, 12.0), (4.0, 9.5, 15.0))
# Battery Cell cradle in the back-west corner: the cell lies along z across two saddles.
CRADLE = ((0.75, 2.0, 11.5), (3.5, 2.5, 15.25))
SADDLE_ZS = (12.0, 14.25)
# Job Chip lamp on the flange, across the vessel's centre line from the status lamp.
JOB_LAMP = ((5.5, VESSEL_Y1 + 0.1, NARROW[0][1] - 0.75), (6.75, FLANGE_Y1 - 0.1, NARROW[0][1] - 0.5))
JOB_LAMP_FACES = ("north", "east", "west", "up", "down")

HEAT_CORES = ("iron", "copper", "bronze", "steel", "aluminum", "sparksteel", "titanium", "exotic", "unique")
CRUSH_HEADS = ("flint", "iron", "copper", "bronze", "steel", "aluminum", "titanium", "tungstensteel", "exotic", "unique")
PUMPS = ("osmium", "titanium", "tungstensteel", "exotic", "unique")
SERVOS = ("steel", "titanium", "tungstensteel", "nullite", "exotic", "unique")


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


def stack(cx, cz, radius, flat, y0, y1, textures, faces=FACES):
    """A rounded vertical stack: two crossed slabs, axis-aligned so it also renders as a wiki icon."""
    return [
        box((cx - radius, y0, cz - flat), (cx + radius, y1, cz + flat), textures, faces),
        box((cx - flat, y0, cz - radius), (cx + flat, y1, cz + radius), textures, faces),
    ]


def crossed(narrow, wide, y0, y1, textures, grow=0.0, faces=FACES):
    """The vessel's two crossed slabs between y0 and y1, grown outward by `grow`."""
    (nx0, nz0), (nx1, nz1) = narrow
    (wx0, wz0), (wx1, wz1) = wide
    return [
        box((nx0 - grow, y0, nz0 - grow), (nx1 + grow, y1, nz1 + grow), textures, faces),
        box((wx0 - grow, y0, wz0 - grow), (wx1 + grow, y1, wz1 + grow), textures, faces),
    ]


def lamp(start, end):
    return box(start, end, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}})


# ---------------------------------------------------------------- parts

def roll_elements():
    """One Crush Head roll along x, built around (y, z) = (8, 8): two squares, one turned 45 degrees, and hub caps."""
    c = PART_CENTRE
    half = ROLL_TIP / math.sqrt(2)
    textures = {"east": "roll_end", "west": "roll_end", "*": "roll"}
    elements = [
        box((ROLL_X0, c - half, c - half), (ROLL_X1, c + half, c + half), textures),
        box((ROLL_X0 + 0.05, c - half, c - half), (ROLL_X1 - 0.05, c + half, c + half), textures,
            extra={"rotation": {"origin": [c, c, c], "axis": "x", "angle": 45}}),
    ]
    for x0, x1, face in ((ROLL_X0 - 0.25, ROLL_X0, "west"), (ROLL_X1, ROLL_X1 + 0.25, "east")):
        elements.append(box((x0, c - 0.4, c - 0.4), (x1, c + 0.4, c + 0.4), "hub",
                            faces=(face, "north", "south", "up", "down"), uv={face: [0, 0, 16, 16]}))
    return elements


def fin_elements():
    """The Heat Core: a bank of fins on each flank of the vessel, between the mounting rails."""
    elements = []
    for x0, x1 in FIN_SPANS:
        for z in FIN_ZS:
            elements.append(box((x0, FIN_Y0, z), (x1, FIN_Y1, z + FIN_WIDTH), "core"))
    return elements


def pump_elements():
    """The Fluid Pump: a round volute on a foot, a motor can on top, and an outlet flange toward the pipe."""
    elements = stack(PUMP_X, PUMP_Z, PUMP_R, PUMP_R * 0.62, PUMP_Y0, PUMP_Y1, {"up": "pump_end", "down": "pump_end", "*": "pump"},
                     faces=SIDES + ("up",))
    elements += stack(PUMP_X, PUMP_Z, 1.1, 0.7, PUMP_Y1, PUMP_Y1 + 2.0, {"up": "pump_end", "*": "pump_motor"}, faces=SIDES + ("up",))
    elements.append(box((PIPE_X[0] - 0.25, PIPE_Y[0] - 0.25 + 0.5, PUMP_Z - PUMP_R - 0.5), (PIPE_X[1] + 0.25, PIPE_Y[1] + 0.75, PUMP_Z - PUMP_R + 0.25),
                        "pump_flange", faces=SIDES + ("up",)))
    return elements


def servo_elements():
    """The Servo: a motor can beside the hopper, coupled to the front roll's shaft, with an accent band."""
    (x0, y0, z0), (x1, y1, z1) = SERVO
    return [
        box((x0 + 0.5, y0, z0), (x1, y1, z1), {"west": "accent", "*": "servo"}, faces=("north", "south", "east", "up", "down")),
        box((x0 + 1.25, y0 - 0.1, z0 - 0.1), (x0 + 1.75, y1 + 0.1, z1 + 0.1), "accent", faces=("north", "south", "up", "down")),
        box((x0, ROLL_Y - 0.4, ROLL_ZS[0] - 0.4), (x0 + 0.5, ROLL_Y + 0.4, ROLL_ZS[0] + 0.4), "accent", faces=("north", "south", "up", "down")),
    ]


# ---------------------------------------------------------------- housing

def vessel_elements():
    (nx0, nz0), (nx1, nz1) = NARROW
    (wx0, wz0), (wx1, wz1) = WIDE
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CHAMBER
    shell = {"*": "shell"}
    elements = []
    # Lower band, solid; its top is the chamber floor.
    elements += crossed(NARROW, WIDE, VESSEL_Y0, BELLY_Y0, {"up": "refractory", "*": "shell"}, faces=SIDES + ("up",))
    # Belly around the chamber: west, east, and back of the narrow slab, and the wide slab's flanks.
    elements.append(box((nx0, BELLY_Y0, nz0), (cx0, BELLY_Y1, nz1), {"east": "refractory", **shell}, faces=SIDES))
    elements.append(box((cx1, BELLY_Y0, nz0), (nx1, BELLY_Y1, nz1), {"west": "refractory", **shell}, faces=SIDES))
    elements.append(box((cx0, BELLY_Y0, cz1), (cx1, BELLY_Y1, nz1), {"north": "refractory", **shell}, faces=("north", "south")))
    elements.append(box((wx0, BELLY_Y0, wz0), (nx0, BELLY_Y1, wz1), shell, faces=("north", "south", "west")))
    elements.append(box((nx1, BELLY_Y0, wz0), (wx1, BELLY_Y1, wz1), shell, faces=("north", "south", "east")))
    # Upper band, solid; its underside is the chamber ceiling.
    elements += crossed(NARROW, WIDE, BELLY_Y1, VESSEL_Y1, {"down": "refractory", "*": "shell"}, faces=SIDES + ("down",))
    # Teal band around the upper belly, under the flange.
    elements += crossed(NARROW, WIDE, VESSEL_Y1 - 1.0, VESSEL_Y1 - 0.5, "band", grow=0.1, faces=SIDES)
    # Bolted flange, domed lid, and the top plate the hopper stands on.
    elements += crossed(NARROW, WIDE, VESSEL_Y1, FLANGE_Y1, {"up": "flange_top", "*": "flange"}, grow=0.5)
    elements += crossed(((5.0, 3.0), (11.0, 11.5)), ((4.0, 4.5), (12.0, 10.0)), FLANGE_Y1, LID_Y1, "lid",
                        faces=SIDES + ("up",))

    # Crucible: a floor and four walls around the pool.
    (rx0, ry0, rz0), (rx1, ry1, rz1) = CRUCIBLE
    w = CRUCIBLE_WALL
    elements.append(box((rx0, ry0, rz0), (rx1, POOL_Y0, rz1), "crucible_floor", faces=("up",)))
    elements.append(box((rx0, ry0, rz0), (rx1, ry1, rz0 + w), {"up": "crucible_rim", "*": "crucible"}, faces=("north", "south", "east", "west", "up")))
    elements.append(box((rx0, ry0, rz1 - w), (rx1, ry1, rz1), {"up": "crucible_rim", "*": "crucible"}, faces=("north", "east", "west", "up")))
    elements.append(box((rx0, ry0, rz0 + w), (rx0 + w, ry1, rz1 - w), {"up": "crucible_rim", "*": "crucible"}, faces=("east", "west", "up")))
    elements.append(box((rx1 - w, ry0, rz0 + w), (rx1, ry1, rz1 - w), {"up": "crucible_rim", "*": "crucible"}, faces=("east", "west", "up")))
    # Pour lip on the front wall.
    elements.append(box((7.5, ry1 - 0.25, rz0 - 0.5), (8.5, ry1, rz0), "crucible", faces=("north", "east", "west", "up", "down")))

    # Porthole bezel with bolts and sealed glass over the chamber mouth, and the heat bar under it.
    trim = ("north", "east", "west", "up", "down")
    elements.append(box((cx0 - 0.75, cy1, BEZEL_Z), (cx1 + 0.75, cy1 + 0.75, nz0), "bezel", faces=trim))
    elements.append(box((cx0 - 0.75, cy0 - 0.5, BEZEL_Z), (cx1 + 0.75, cy0, nz0), "bezel", faces=trim))
    elements.append(box((cx0 - 0.75, cy0, BEZEL_Z), (cx0, cy1, nz0), "bezel", faces=("north", "east", "west")))
    elements.append(box((cx1, cy0, BEZEL_Z), (cx1 + 0.75, cy1, nz0), "bezel", faces=("north", "east", "west")))
    elements.append(box((cx0, cy0, GLASS_Z - 0.05), (cx1, cy1, GLASS_Z + 0.05), "glass", faces=("north", "south"),
                        uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}))
    (bx0, by0), (bx1, by1) = HEAT_BAR
    elements.append(box((bx0 - 0.25, by0 - 0.25, HEAT_BAR_Z), (bx1 + 0.25, by1 + 0.25, nz0), {"north": "heat_bar", "*": "trim"},
                        faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 16, 2]}))

    # Heat Core mounting rails on both flanks; the fins sit between them.
    for x0, x1 in FIN_SPANS:
        for y0, y1 in RAIL_YS:
            elements.append(box((x0, y0, FIN_ZS[0] - 0.25), (x1, y1, FIN_ZS[-1] + FIN_WIDTH + 0.25), "trim",
                                faces=SIDES + ("up", "down")))
    # Status lamp and Job Chip lamp on the flange.
    elements.append(lamp((9.25, VESSEL_Y1 + 0.1, NARROW[0][1] - 0.75), (10.5, FLANGE_Y1 - 0.1, NARROW[0][1] - 0.5)))
    elements.append(job_lamp.element(*JOB_LAMP, JOB_LAMP_FACES))
    return elements


def hopper_elements():
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HOPPER
    w = HOPPER_WALL
    walls = {"*": "hopper"}
    elements = [
        box((hx0, hy0, hz0), (hx1, hy1, hz0 + w), {"south": "liner", **walls}),
        box((hx0, hy0, hz1 - w), (hx1, hy1, hz1), {"north": "liner", **walls}),
        box((hx0, hy0, hz0 + w), (hx0 + w, hy1, hz1 - w), {"east": "liner", **walls}, faces=("east", "west", "up")),
        box((hx1 - w, hy0, hz0 + w), (hx1, hy1, hz1 - w), {"west": "liner", **walls}, faces=("east", "west", "up")),
        box((hx0 + w, hy0, hz0 + w), (hx1 - w, hy0 + 0.05, hz1 - w), "throat", faces=("up",), uv={"up": [0, 0, 16, 16]}),
        # Teal band round the hopper mouth.
        box((hx0 - 0.1, hy1 - 0.75, hz0 - 0.1), (hx1 + 0.1, hy1 - 0.25, hz1 + 0.1), "band", faces=SIDES),
    ]
    # Bare roll shafts through the hopper walls, hidden inside the rolls once a Crush Head is installed.
    for z in ROLL_ZS:
        elements.append(box((hx0 - 0.25, ROLL_Y - 0.3, z - 0.3), (hx1 + 0.25, ROLL_Y + 0.3, z + 0.3), "shaft",
                            faces=("east", "west", "north", "south", "up", "down")))
    return elements


def column_elements(column):
    """A sight column: caps in the chassis trim, four corner rails, and a gauge strip on the front rail."""
    (x0, z0), (x1, z1) = column
    y0, y1 = COLUMN_Y0, COLUMN_Y1
    elements = [
        box((x0, y0, z0), (x1, y0 + CAP, z1), {"up": "cap_top", "*": "cap"}, faces=SIDES + ("up",), uv={"up": [0, 0, 16, 16]}),
        box((x0, y1 - CAP, z0), (x1, y1, z1), {"up": "cap_top", "down": "cap_top", "*": "cap"}, uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
    ]
    for x in (x0, x1 - RAIL):
        for z in (z0, z1 - RAIL):
            elements.append(box((x, y0 + CAP, z), (x + RAIL, y1 - CAP, z + RAIL), "rail", faces=SIDES))
    return elements


def pipes():
    (ix0, iz0), (ix1, iz1) = COLUMN_IN
    (ox0, oz0), (ox1, oz1) = COLUMN_OUT
    elements = [
        # Input column into the vessel, under the flange.
        box((ix1, 9.5, 1.75), (NARROW[0][0], 10.25, 2.5), "pipe", faces=("north", "south", "up", "down")),
        # Output line: from the pump's flange forward along the plinth to the foot of the output column.
        box((PIPE_X[0], PIPE_Y[0], oz1), (PIPE_X[1], PIPE_Y[1], PUMP_Z - PUMP_R - 0.5), "pipe", faces=("east", "west", "up")),
        # Bare pump pad, under the pump.
        box((PUMP_X - 1.75, 2.0, PUMP_Z - 1.6), (PUMP_X + 1.5, 2.5, PUMP_Z + 1.6), {"up": "socket", "*": "trim"}, faces=SIDES + ("up",)),
    ]
    return elements


def cabinet_elements():
    (gx0, gy0, gz0), (gx1, gy1, gz1) = CABINET
    elements = [
        box((gx0, gy0, gz0), (gx1, gy1, gz1), "casing", faces=("south", "east", "west", "up")),
        box((gx0 - 0.1, gy1 - 1.5, gz0), (gx1 + 0.1, gy1 - 1.0, gz1 + 0.1), "band", faces=("south", "east", "west")),
        box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}),
        box(*METER, {"west": "meter", "*": "trim"}, faces=("north", "south", "west", "up", "down"), uv={"west": [0, 0, 3, 4.5]}),
    ]
    # Battery Cell cradle: a base and two saddles the cell lies across.
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CRADLE
    elements.append(box((cx0, cy0, cz0), (cx1, cy1, cz1), "trim", faces=SIDES + ("up",)))
    for z0 in SADDLE_ZS:
        elements.append(box((cx0, cy1, z0), (cx0 + 0.5, cy1 + 1.0, z0 + 0.75), "trim", faces=SIDES + ("up",)))
        elements.append(box((cx1 - 0.5, cy1, z0), (cx1, cy1 + 1.0, z0 + 0.75), "trim", faces=SIDES + ("up",)))
    return elements


def housing():
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    elements += vessel_elements()
    elements += hopper_elements()
    elements += column_elements(COLUMN_IN)
    elements += column_elements(COLUMN_OUT)
    elements += pipes()
    elements += cabinet_elements()
    return elements


# ---------------------------------------------------------------- models

HOUSING_TEXTURES = {
    "particle": TEX + "vessel",
    "shell": TEX + "vessel",
    "trim": CRUSHER + "trim/titanium",
    "casing": CRUSHER + "casing",
    "liner": CRUSHER + "liner",
    "shaft": CRUSHER + "shaft",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "meter": CRUSHER + "meter",
    "throat": CRUSHER + "throat",
    "lamp": CRUSHER + "lamp_off",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "refractory": FURNACE + "refractory",
    "heat_bar": FURNACE + "heat_bar",
    "pipe": FURNACE + "pipe",
    "glass": FURNACE + "glass",
    "socket": FURNACE + "socket",
    "lid": TEX + "lid",
    "flange": TEX + "flange",
    "flange_top": TEX + "flange_top",
    "bezel": TEX + "bezel",
    "hopper": TEX + "hopper",
    "rail": TEX + "rail",
    "cap": TEX + "cap",
    "cap_top": TEX + "cap_top",
    "crucible": TEX + "crucible",
    "crucible_rim": TEX + "crucible_rim",
    "crucible_floor": TEX + "crucible_floor",
    **job_lamp.TEXTURE,
}


def roll_textures(head):
    return {"roll": CRUSHER + f"roll/{head}", "roll_end": CRUSHER + f"roll/{head}_end", "hub": CRUSHER + f"roll/{head}_hub"}


def pump_textures(pump):
    return {"pump": TEX + f"pump/{pump}", "pump_end": TEX + f"pump/{pump}_end", "pump_motor": TEX + "pump/motor",
            "pump_flange": TEX + f"pump/{pump}_end"}


def servo_textures(servo):
    return {"servo": SERVO_TEX + servo, "accent": SERVO_TEX + f"{servo}_accent"}


def default_uv(face, start, end):
    (x1, y1, z1), (x2, y2, z2) = start, end
    return [r(v) for v in {
        "up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]]


def moved(elements, dy, dz):
    """Elements moved in y and z, keeping their position-derived UVs, so a roll sits where the renderer draws it."""
    out = []
    for element in elements:
        copy = json.loads(json.dumps(element))
        start, end = copy["from"], copy["to"]
        for face, entry in copy["faces"].items():
            entry.setdefault("uv", default_uv(face, start, end))
        copy["from"] = [start[0], r(start[1] + dy), r(start[2] + dz)]
        copy["to"] = [end[0], r(end[1] + dy), r(end[2] + dz)]
        if "rotation" in copy:
            origin = copy["rotation"]["origin"]
            copy["rotation"]["origin"] = [origin[0], r(origin[1] + dy), r(origin[2] + dz)]
        out.append(copy)
    return out


def item_rolls():
    rolls = []
    for z in ROLL_ZS:
        rolls += moved(roll_elements(), ROLL_Y - PART_CENTRE, z - PART_CENTRE)
    return rolls


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    elements = housing()
    write_json(MODELS / "block/melter.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": elements,
    })
    write_json(MODELS / "block/melter_active.json", {"parent": "rngtech:block/melter", "textures": {"lamp": CRUSHER + "lamp_on"}})
    job_lamp.write_part(MODELS / "block/melter/job_lamp.json", *JOB_LAMP, JOB_LAMP_FACES)
    job_lamp.write_blockstate("melter", "rngtech:block/melter", "rngtech:block/melter_active", "rngtech:block/melter/job_lamp")
    # The item shows the Gear its recipe builds in: Titanium rolls and fins, and an Osmium pump.
    write_json(MODELS / "item/melter.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, **roll_textures("titanium"), **pump_textures("osmium"), "core": CORE_TEX + "titanium"},
        "elements": fin_elements() + pump_elements() + item_rolls() + elements,
    })

    parts = MODELS / "block/melter"
    write_json(parts / "roll.json", {"textures": {"particle": "#roll"}, "elements": roll_elements()})
    for head in CRUSH_HEADS:
        write_json(parts / f"roll_{head}.json", {"parent": "rngtech:block/melter/roll", "textures": roll_textures(head)})
    write_json(parts / "fins.json", {"textures": {"particle": "#core"}, "elements": fin_elements()})
    for core in HEAT_CORES:
        write_json(parts / f"fins_{core}.json", {"parent": "rngtech:block/melter/fins", "textures": {"core": CORE_TEX + core}})
    write_json(parts / "pump.json", {"textures": {"particle": "#pump"}, "elements": pump_elements()})
    for pump in PUMPS:
        write_json(parts / f"pump_{pump}.json", {"parent": "rngtech:block/melter/pump", "textures": pump_textures(pump)})
    write_json(parts / "servo.json", {"textures": {"particle": "#servo"}, "elements": servo_elements()})
    for servo in SERVOS:
        write_json(parts / f"servo_{servo}.json", {"parent": "rngtech:block/melter/servo", "textures": servo_textures(servo)})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"melter:{name}")


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


TITANIUM = (150, 166, 188)
DARK = (44, 48, 56)
# Fluid Pump bodies, after the Fluid Pump item palettes.
PUMP_COLORS = {
    "osmium": (128, 150, 176),
    "titanium": (156, 182, 214),
    "tungstensteel": (96, 104, 116),
    "exotic": (150, 90, 200),
    "unique": (175, 96, 37),
}


def bolts(image, color, spacing, inset, light, dark):
    """Round bolt heads along every edge, spaced `spacing` texels apart."""
    pixels = image.load()
    size = image.size[0]
    spots = [(x, inset) for x in range(inset, size, spacing)] + [(x, size - 1 - inset) for x in range(inset, size, spacing)]
    for x, y in spots:
        for dx, dy, value in ((0, 0, light), (1, 0, color), (0, 1, color), (1, 1, dark)):
            if 0 <= x + dx < size and 0 <= y + dy < size:
                pixels[x + dx, y + dy] = (*value, 255)


def write_textures() -> None:
    # Vessel wall: brushed titanium with welded hoop seams every four model pixels, read as a round pressure vessel.
    vessel = noisy(64, shade(TITANIUM, -8), "vessel", amount=4, streak=3)
    pixels = vessel.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(TITANIUM, -58), 255)
            elif y % 16 == 1:
                pixels[x, y] = (*shade(TITANIUM, 22), 255)
            elif y % 16 == 15:
                pixels[x, y] = (*shade(pixels[x, y], -14), 255)
    save(vessel, "vessel")

    # Domed lid: brushed titanium with faint radial seams.
    lid = noisy(64, shade(TITANIUM, -18), "lid", amount=4, streak=3)
    pixels = lid.load()
    for y in range(64):
        for x in range(64):
            if x % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -20), 255)
    save(lid, "lid")

    # Flange ring: a darker titanium band with bolt heads along it.
    flange = noisy(64, shade(TITANIUM, -36), "flange", amount=4)
    pixels = flange.load()
    for x in range(64):
        pixels[x, 0] = (*shade(TITANIUM, 10), 255)
        pixels[x, 63] = (*shade(TITANIUM, -70), 255)
    for x in range(2, 64, 6):
        for y in (1, 2):
            pixels[x, y] = (*shade(TITANIUM, 30 if y == 1 else -10), 255)
            pixels[x + 1, y] = (*shade(TITANIUM, -10 if y == 1 else -50), 255)
    save(flange, "flange")
    flange_top = noisy(64, shade(TITANIUM, -36), "flange_top", amount=4)
    draw = ImageDraw.Draw(flange_top)
    for x in range(3, 64, 8):
        for y in (3, 59):
            draw.rectangle([x, y, x + 1, y + 1], fill=(*shade(TITANIUM, 30), 255))
    save(flange_top, "flange_top")

    # Porthole bezel: a heavy dark ring with bright bolt heads.
    bezel = noisy(64, (70, 76, 86), "bezel", amount=4)
    bolts(bezel, (120, 128, 140), 8, 1, (170, 178, 190), (40, 44, 50))
    save(bezel, "bezel")

    # Hopper walls: titanium plate with a riveted top edge.
    hopper = noisy(64, shade(TITANIUM, -24), "hopper", amount=4, streak=2)
    pixels = hopper.load()
    for x in range(64):
        pixels[x, 0] = (*shade(TITANIUM, 6), 255)
        pixels[x, 1] = (*shade(TITANIUM, -50), 255)
    save(hopper, "hopper")

    # Sight-column rail: steel with gauge ticks every eighth.
    rail = noisy(64, (74, 80, 90), "rail", amount=4)
    pixels = rail.load()
    for y in range(64):
        for x in range(64):
            if y % 8 == 0:
                pixels[x, y] = (150, 158, 170, 255)
            elif x % 16 in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
    save(rail, "rail")
    cap_side = noisy(64, (58, 63, 72), "cap", amount=4)
    pixels = cap_side.load()
    for x in range(64):
        pixels[x, 0] = (96, 104, 116, 255)
        pixels[x, 63] = (30, 33, 38, 255)
    save(cap_side, "cap")
    cap = noisy(64, (66, 72, 82), "cap_top", amount=4)
    draw = ImageDraw.Draw(cap)
    draw.rectangle([0, 0, 63, 63], outline=(70, 76, 86, 255), width=4)
    draw.ellipse([22, 22, 41, 41], fill=(56, 60, 68, 255), outline=(150, 158, 170, 255), width=2)
    save(cap, "cap_top")

    # Crucible: dark graphite clay, a rim, and a scorched floor.
    crucible = noisy(64, (66, 62, 60), "crucible", amount=6)
    pixels = crucible.load()
    for x in range(64):
        for y in range(64):
            if y % 16 in (0, 1):
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(crucible, "crucible")
    rim = noisy(64, (96, 90, 86), "crucible_rim", amount=6)
    save(rim, "crucible_rim")
    floor = noisy(64, (36, 32, 30), "crucible_floor", amount=5)
    rand = rng("crucible_floor_scorch")
    pixels = floor.load()
    for _ in range(80):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*mix(pixels[x, y], (120, 70, 40), 0.5), 255)
    save(floor, "crucible_floor")

    # Fluid Pumps: a ribbed volute in the pump's metal, its end cap, and a dark motor can.
    for pump, color in PUMP_COLORS.items():
        body = noisy(64, color, f"pump_{pump}", amount=6, streak=3)
        pixels = body.load()
        for y in range(64):
            for x in range(64):
                if y % 8 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -30), 255)
                elif y % 8 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 16), 255)
                if pump == "exotic" and x % 8 == 4:
                    pixels[x, y] = (*mix(pixels[x, y], (170, 96, 222), 0.5), 255)
        save(body, f"pump/{pump}")
        end = noisy(64, shade(color, -12), f"pump_{pump}_end", amount=5)
        draw = ImageDraw.Draw(end)
        draw.ellipse([10, 10, 53, 53], outline=(*shade(color, -50), 255), width=3)
        draw.ellipse([26, 26, 37, 37], fill=(*shade(color, 30), 255))
        save(end, f"pump/{pump}_end")
    motor = noisy(64, (58, 62, 70), "pump_motor", amount=4)
    pixels = motor.load()
    for y in range(64):
        for x in range(64):
            if x % 6 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
    save(motor, "pump/motor")


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
