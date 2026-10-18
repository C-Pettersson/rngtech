#!/usr/bin/env python3
"""Generate the Metal Press block models, part models, item models, and owned textures.

Both presses share one layout around a heated die bed. The selected Mold is the lower die on the bed, the input lies on
it, and a ram brings the upper die down onto it; pressed parts collect in a tray at the front of the bed. The Heat Core
is a finned radiator on the west side of the bed, the Battery Cell stands in a cradle on the east side with the
atmosphere tank (Nitrogen, or Hydrogen for annealing) behind it, and a back cabinet carries the cable port and a rack
for the stored Molds. A heat bar on the front of the bed reads like the Furnace's.

The Steel Metal Press is a straight-side servo press on the slate plinth: two steel housings, a crown with a teal band,
the installed Servo on the crown driving the ram through a rod, a heated platen under the die, and a failure-strain
meter next to the heat bar. The Crude Metal Press follows its recipe: a firebrick bed from its Furnace chassis wound
with copper heating coils, an iron anvil plate, two bronze columns, a bronze crosshead, and a screw spindle with a
fly bar that turns as the iron ram comes down. Its Servo is optional and shows on a bracket behind the crosshead only
when installed.

The housings are static block models. The installed parts, the ram, the screw, the items, the heat glow, and the
meters are drawn by MetalPressRenderer, whose constants match the geometry here. Run from the repository root:

    python tools/blockmodels/metal_press.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

import furnace
from furnace import (
    CYLINDER, CYLINDER_FLAT, CYLINDER_R, CYLINDER_WINDOW, CYLINDER_Y, CRADLE, SIDES,
    blotches, box, cradle_elements, cylinder_elements, heat_bar, lamp, noisy, panel_seams, plinth,
    port_elements, shade, stack, write_json,
)

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
OLD_TEXTURES = ASSETS / "textures/block/metal_press"
TEX = "rngtech:block/metal_press/"
FURNACE_TEX = "rngtech:block/furnace/"
CORE_TEX = "rngtech:block/solid_fuel_burner/heat_core/"
SERVO_TEX = "rngtech:block/compressor_tank/servo/"

# ---------------------------------------------------------------- shared geometry
# The press works around (WORK_X, WORK_Z); the front faces north (z = 0).
WORK_X, WORK_Z = 7.25, 5.75
BED = ((2.5, 2.0, 0.75), (12.5, 5.0, 10.5))
BED_TOP = BED[1][1]
# The selected Mold: a die block on the bed; the input lies on its top.
DIE = ((4.75, 5.5, 3.25), (9.75, 7.0, 8.25))
DIE_TOP = DIE[1][1]
# Output tray at the front of the bed; pressed parts lie on its floor.
TRAY = ((4.5, BED_TOP, 0.75), (10.0, 5.75, 2.75))
TRAY_FLOOR = 5.25
# Heat bar on the front of the bed, and the Steel press's failure-strain meter beside it.
HEAT_BAR_STEEL = ((3.25, 2.75), (8.75, 3.75))
HEAT_BAR_CRUDE = ((3.25, 2.75), (11.5, 3.75))
STRAIN_BAR = ((9.5, 2.75), (11.5, 3.75))
BAR_Z = 0.5
# Heat Core radiator on the west side of the bed: a base plate and horizontal fins.
RADIATOR_PLATE = ((2.0, 2.75, 2.25), (2.5, 8.0, 9.25))
RADIATOR_FINS = 6
RADIATOR_FIN_X = (0.25, 2.0)
RADIATOR_FIN_Z = (2.5, 9.0)
# Ram travel: the bottom of the ram body sits at RAM_UP when raised; parts and the item model draw it there.
RAM_UP = 10.0
UPPER_DIE_DEPTH = 0.75
# Back cabinet with the cable port, and the Mold rack on its top: five slots, cavities facing the back.
CABINET = ((2.5, 2.0, 11.0), (12.5, 12.5, 15.0))
RACK = ((2.75, CABINET[1][1], 11.25), (12.25, 13.0, 14.75))
RACK_SLOTS = 5
RACK_DIVIDER = 0.2
RACK_TOP = 14.25
# Status lamp on top of the cell cradle, as on the Furnace.
LAMP = ((13.25, CRADLE[1][1], 6.25), (14.75, CRADLE[1][1] + 1.0, 7.75))

# ---------------------------------------------------------------- Steel press geometry
HOUSINGS = (((2.5, BED_TOP, 1.0), (3.75, 12.5, 10.0)), ((10.75, BED_TOP, 1.0), (12.0, 12.5, 10.0)))
CROWN = ((2.0, 12.5, 0.75), (12.5, 14.5, 10.5))
PLATEN = ((3.75, BED_TOP, 3.0), (10.75, 5.5, 9.5))
RAM = ((3.875, 0.0, 2.25), (10.625, 1.75, 9.25))
ROD = ((6.75, 1.75, 5.25), (7.75, 4.5, 6.25))
# The Servo lies on the crown along x; on the Crude press it sits on a bracket behind the crosshead.
SERVO_Y = CROWN[1][1]
SERVO_X = (4.5, 10.0)

# ---------------------------------------------------------------- Crude press geometry
COLUMN_XS = (3.5, 11.0)
COLUMN_R, COLUMN_FLAT = 0.9, 0.6
CROSSHEAD = ((2.25, 12.5, 4.0), (12.25, 14.0, 7.5))
NUT = ((6.25, CROSSHEAD[1][1], 4.75), (8.25, 14.5, 6.75))
SERVO_BRACKET = ((4.5, 12.75, CROSSHEAD[1][2]), (10.0, 13.75, 10.25))
CRUDE_SERVO_OFFSET = (0.0, SERVO_BRACKET[1][1] - SERVO_Y, 3.125)
ANVIL = ((4.5, BED_TOP, 3.0), (10.0, 5.5, 8.5))
COIL_Y = (4.0, 4.75)
SLIDE = ((4.5, 0.0, 3.75), (10.0, 1.5, 7.75))
SPINDLE_Y = (9.5, 15.5)
SPINDLE_HALF = 0.6
FLY_BAR_Y = (15.5, 16.0)
FLY_BAR_HALF = 4.75
WEIGHT_HALF = 0.5

MOLDS = ("plate", "gear", "casing", "circuit", "connector")
HEAT_CORES = ("iron", "copper", "bronze", "steel", "aluminum", "sparksteel", "titanium", "exotic", "unique")
SERVOS = ("steel", "titanium", "tungstensteel", "nullite", "exotic", "unique")
CAVITY_TEXELS = 20


def ring(outer, inner, y0, y1, texture):
    """A rectangular ring between two (x0, z0, x1, z1) outlines."""
    (ox0, oz0, ox1, oz1), (ix0, iz0, ix1, iz1) = outer, inner
    return [
        box((ox0, y0, oz0), (ix0, y1, oz1), texture),
        box((ix1, y0, oz0), (ox1, y1, oz1), texture),
        box((ix0, y0, oz0), (ix1, y1, iz0), texture, faces=("north", "south", "up", "down")),
        box((ix0, y0, iz1), (ix1, y1, oz1), texture, faces=("north", "south", "up", "down")),
    ]


# ---------------------------------------------------------------- shared parts of both housings

def tray_elements():
    (x0, y0, z0), (x1, y1, z1) = TRAY
    lip = ("north", "south", "east", "west", "up")
    return [
        box((x0, y0, z0 + 0.25), (x1, TRAY_FLOOR, z1), {"up": "tray", "*": "trim"}, faces=("east", "west", "up")),
        box((x0, y0, z0), (x1, y1, z0 + 0.25), "trim", faces=lip),
        box((x0, TRAY_FLOOR, z0 + 0.25), (x0 + 0.25, y1, z1), "trim", faces=("east", "west", "up", "south")),
        box((x1 - 0.25, TRAY_FLOOR, z0 + 0.25), (x1, y1, z1), "trim", faces=("east", "west", "up", "south")),
    ]


def radiator_socket():
    (px0, py0, pz0), (_, py1, pz1) = RADIATOR_PLATE
    return box((px0 + 0.25, py0 - 0.25, pz0 - 0.25), (px0 + 0.5, py1 + 0.25, pz1 + 0.25), "socket",
               faces=("north", "south", "west", "up", "down"))


def cabinet_elements():
    (kx0, ky0, kz0), (kx1, ky1, kz1) = CABINET
    elements = [box((kx0, ky0, kz0), (kx1, ky1, kz1), "casing", faces=("north", "west", "south", "up"))]
    elements.append(box((kx0 - 0.25, ky1 - 1.25, kz0 - 0.25), (kx1, ky1 - 0.75, kz1), "band", faces=("north", "west", "down")))
    elements += port_elements()
    # Mold rack: a tray with dividers; MetalPressRenderer stands the stored Molds in its slots.
    (rx0, ry0, rz0), (rx1, ry1, rz1) = RACK
    elements.append(box((rx0, ry0, rz0), (rx1, ry1, rz1), {"up": "rack", "*": "casing"}, faces=SIDES + ("up",)))
    elements.append(box((rx0, ry1, rz0), (rx1, ry1 + 0.5, rz0 + 0.25), "casing", faces=SIDES + ("up",)))
    elements.append(box((rx0, ry1, rz1 - 0.25), (rx1, RACK_TOP, rz1), "casing", faces=SIDES + ("up",)))
    pitch = (rx1 - rx0 - RACK_DIVIDER) / RACK_SLOTS
    for i in range(RACK_SLOTS + 1):
        x = rx0 + pitch * i
        elements.append(box((x, ry1, rz0 + 0.25), (x + RACK_DIVIDER, ry1 + 0.75, rz1 - 0.25), "pipe", faces=("east", "west", "up")))
    return elements


def east_side_elements():
    """The Furnace's cell cradle, lamp, and atmosphere cylinder, fed into the bed and the cabinet."""
    elements = cradle_elements(feed_to=CYLINDER[1] - CYLINDER_R)
    elements.append(lamp(*LAMP))
    elements += cylinder_elements(CYLINDER, CYLINDER_R, CYLINDER_FLAT, CYLINDER_Y, CYLINDER_WINDOW, "east")
    top = CYLINDER_Y[1] + 0.75
    elements.append(box((CABINET[1][0] - 0.25, top + 0.25, CYLINDER[1] - 0.25), (CYLINDER[0] - 0.5, top + 0.75, CYLINDER[1] + 0.25),
                        "pipe", faces=("north", "south", "up", "down")))
    return elements


# ---------------------------------------------------------------- Steel press

def steel_elements():
    (bx0, by0, bz0), (bx1, by1, bz1) = BED
    elements = [plinth()]
    elements.append(box((bx0, by0, bz0), (bx1, by1, bz1), {"*": "shell", "up": "crown_top"}))
    elements.append(heat_bar(HEAT_BAR_STEEL, BAR_Z, back=bz0))
    elements.append(heat_bar(STRAIN_BAR, BAR_Z, back=bz0))
    elements.append(box(PLATEN[0], PLATEN[1], {"up": "platen", "*": "trim"}, faces=SIDES + ("up",)))
    for (x0, y0, z0), (x1, y1, z1) in HOUSINGS:
        inner = "east" if x0 < WORK_X else "west"
        elements.append(box((x0, y0, z0), (x1, y1, z1), {inner: "liner", "*": "shell"}, faces=SIDES))
        # Guide gibs on the front edge of each housing, along the ram's travel.
        gx0, gx1 = (x1, x1 + 0.125) if inner == "east" else (x0 - 0.125, x0)
        elements.append(box((gx0, 6.0, z0 + 0.25), (gx1, 12.5, z0 + 1.0), "pipe", faces=("north", "south", inner)))
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CROWN
    elements.append(box((cx0, cy0, cz0), (cx1, cy1, cz1), {"up": "crown_top", "*": "shell"}))
    elements.append(box((cx0 - 0.25, cy0 + 0.75, cz0 - 0.25), (cx1 + 0.25, cy0 + 1.25, cz1), "band",
                        faces=("north", "east", "west", "down")))
    elements += tray_elements()
    elements.append(radiator_socket())
    elements += cabinet_elements()
    elements += east_side_elements()
    return elements


def steel_ram_elements(rod=True):
    """The Steel press's ram at its raised position: the ram body, the upper die under it, and the rod to the Servo."""
    (x0, y0, z0), (x1, y1, z1) = RAM
    (dx0, _, dz0), (dx1, _, dz1) = DIE
    up = RAM_UP
    elements = [
        box((x0, up + y0, z0), (x1, up + y1, z1), {"*": "ram", "up": "ram_top"}),
        box((dx0, up - UPPER_DIE_DEPTH, dz0), (dx1, up, dz1), {"down": "upper_die", "*": "die"}, faces=SIDES + ("down",),
            uv={"down": [0, 0, 5, 5]}),
    ]
    if rod:
        (rx0, ry0, rz0), (rx1, ry1, rz1) = ROD
        elements.append(box((rx0, up + ry0, rz0), (rx1, up + ry1, rz1), "pipe", faces=SIDES))
    return elements


def servo_elements():
    """The Servo: a motor lying on the crown along x, with end bells, accent rings, and a terminal box."""
    y0 = SERVO_Y
    x0, x1 = SERVO_X
    z = WORK_Z
    elements = [
        box((x0, y0, z - 1.0), (x1, y0 + 1.5, z + 1.0), "servo"),
        box((x0, y0 + 0.25, z - 1.25), (x1, y0 + 1.25, z + 1.25), "servo"),
        box((x0 - 0.25, y0 + 0.25, z - 0.75), (x0, y0 + 1.25, z + 0.75), "accent", faces=SIDES + ("up", "down")),
        box((x1, y0 + 0.25, z - 0.75), (x1 + 0.25, y0 + 1.25, z + 0.75), "accent", faces=SIDES + ("up", "down")),
    ]
    for rx in (x0 + 0.75, x1 - 1.25):
        elements.append(box((rx, y0, z - 1.375), (rx + 0.5, y0 + 1.5, z + 1.375), "accent", faces=SIDES + ("up",)))
    elements.append(box((WORK_X - 0.75, y0, z + 1.25), (WORK_X + 0.75, y0 + 1.0, z + 2.0), "accent",
                        faces=("south", "east", "west", "up")))
    return elements


def heat_core_elements(texture="core"):
    """The Heat Core: a base plate on the bed's west side and a stack of horizontal fins."""
    elements = [box(RADIATOR_PLATE[0], RADIATOR_PLATE[1], texture, faces=("north", "south", "west", "up", "down"))]
    (_, py0, _), (_, py1, _) = RADIATOR_PLATE
    pitch = (py1 - py0 - 0.5) / (RADIATOR_FINS - 1)
    for i in range(RADIATOR_FINS):
        y = py0 + pitch * i
        elements.append(box((RADIATOR_FIN_X[0], y, RADIATOR_FIN_Z[0]), (RADIATOR_FIN_X[1], y + 0.5, RADIATOR_FIN_Z[1]), texture,
                            faces=("north", "south", "west", "up", "down")))
    return elements


def die_elements():
    """A Mold on the bed: a tool-steel block with the Mold's cavity on its top, mapped from a 20-texel square."""
    return [box(DIE[0], DIE[1], {"up": "cavity", "*": "die"}, faces=SIDES + ("up",), uv={"up": [0, 0, 5, 5]})]


# ---------------------------------------------------------------- Crude press

def crude_elements():
    (bx0, by0, bz0), (bx1, by1, bz1) = BED
    elements = [plinth()]
    # Firebrick bed from the press's Furnace chassis, wound with a copper heating coil under its top.
    elements.append(box((bx0, by0, bz0), (bx1, by1, bz1), {"*": "brick", "up": "brick"}))
    c0, c1 = COIL_Y
    elements.append(box((bx0 - 0.25, c0, bz0 - 0.25), (bx1 + 0.25, c1, bz1 + 0.25), "coil", faces=SIDES))
    elements.append(heat_bar(HEAT_BAR_CRUDE, BAR_Z, back=bz0))
    elements.append(box(ANVIL[0], ANVIL[1], {"up": "anvil", "*": "anvil_side"}, faces=SIDES + ("up",)))
    # Bronze columns on cast feet, and the crosshead with the screw nut.
    for x in COLUMN_XS:
        elements += stack(x, WORK_Z, COLUMN_R + 0.4, COLUMN_FLAT + 0.4, BED_TOP, BED_TOP + 0.5, "trim", faces=SIDES + ("up",))
        elements += stack(x, WORK_Z, COLUMN_R, COLUMN_FLAT, BED_TOP + 0.5, CROSSHEAD[0][1], "column", faces=SIDES)
    (hx0, hy0, hz0), (hx1, hy1, hz1) = CROSSHEAD
    elements.append(box((hx0, hy0, hz0), (hx1, hy1, hz1), "shell"))
    elements.append(box(NUT[0], NUT[1], "trim", faces=SIDES + ("up",)))
    elements.append(box(SERVO_BRACKET[0], SERVO_BRACKET[1], {"up": "socket", "*": "trim"}, faces=("south", "east", "west", "up", "down")))
    elements += tray_elements()
    elements.append(radiator_socket())
    elements += cabinet_elements()
    elements += east_side_elements()
    return elements


def crude_ram_elements():
    """The Crude press's iron slide at its raised position: guide sleeves round the columns and the upper die under it."""
    (x0, y0, z0), (x1, y1, z1) = SLIDE
    (dx0, _, dz0), (dx1, _, dz1) = DIE
    up = RAM_UP
    elements = [
        box((x0, up + y0, z0), (x1, up + y1, z1), {"*": "ram", "up": "ram_top"}),
        box((dx0, up - UPPER_DIE_DEPTH, dz0), (dx1, up, dz1), {"down": "upper_die", "*": "die"}, faces=SIDES + ("down",),
            uv={"down": [0, 0, 5, 5]}),
    ]
    for x in COLUMN_XS:
        elements.append(box((x - COLUMN_R - 0.25, up + 0.25, WORK_Z - COLUMN_FLAT - 0.4), (x + COLUMN_R + 0.25, up + 1.25, WORK_Z + COLUMN_FLAT + 0.4),
                            "ram", faces=SIDES + ("up", "down")))
    return elements


def screw_elements(segments=None):
    """The screw spindle and fly bar, turned by MetalPressRenderer about (WORK_X, WORK_Z).

    `segments` limits the spindle to these (y0, y1) spans, so the item model leaves out the parts inside the crosshead.
    """
    h = SPINDLE_HALF
    elements = []
    for y0, y1 in segments or (SPINDLE_Y,):
        elements.append(box((WORK_X - h, y0, WORK_Z - h), (WORK_X + h, y1, WORK_Z + h), "screw", faces=SIDES))
        elements.append(box((WORK_X - h, y0, WORK_Z - h), (WORK_X + h, y1, WORK_Z + h), "screw", faces=SIDES,
                            extra={"rotation": {"origin": [WORK_X, 8, WORK_Z], "axis": "y", "angle": 45}}))
    f0, f1 = FLY_BAR_Y
    elements.append(box((WORK_X - 0.75, f0 - 0.5, WORK_Z - 0.75), (WORK_X + 0.75, f1, WORK_Z + 0.75), "trim", faces=SIDES + ("up",)))
    elements.append(box((WORK_X - FLY_BAR_HALF, f0, WORK_Z - 0.25), (WORK_X + FLY_BAR_HALF, f1 - 0.1, WORK_Z + 0.25), "bar",
                        faces=SIDES + ("up", "down")))
    for side in (-1, 1):
        cx = WORK_X + side * (FLY_BAR_HALF + WEIGHT_HALF)
        elements.append(box((cx - WEIGHT_HALF, f0 - 0.25, WORK_Z - 0.75), (cx + WEIGHT_HALF, f1, WORK_Z + 0.75), "weight"))
    return elements


# ---------------------------------------------------------------- models

SHARED_TEXTURES = {
    "casing": FURNACE_TEX + "casing",
    "plinth_top": FURNACE_TEX + "plinth_top",
    "plinth_side": FURNACE_TEX + "plinth_side",
    "band": FURNACE_TEX + "band",
    "port": FURNACE_TEX + "port",
    "socket": FURNACE_TEX + "socket",
    "pipe": FURNACE_TEX + "pipe",
    "meter": FURNACE_TEX + "meter",
    "lamp": FURNACE_TEX + "lamp_off",
    "cylinder": FURNACE_TEX + "cylinder",
    "nitrogen_band": FURNACE_TEX + "nitrogen_band",
    "cylinder_window": FURNACE_TEX + "cylinder_window",
    "collar": FURNACE_TEX + "collar",
    "heat_bar": FURNACE_TEX + "heat_bar",
    "tray": TEX + "tray",
    "rack": TEX + "rack",
}
STEEL_TEXTURES = {
    **SHARED_TEXTURES,
    "particle": TEX + "shell/steel",
    "shell": TEX + "shell/steel",
    "trim": TEX + "trim/steel",
    "crown_top": TEX + "crown_top",
    "liner": TEX + "liner",
    "platen": TEX + "platen",
}
CRUDE_TEXTURES = {
    **SHARED_TEXTURES,
    "particle": TEX + "shell/bronze",
    "shell": TEX + "shell/bronze",
    "trim": TEX + "trim/bronze",
    "column": TEX + "column",
    "brick": FURNACE_TEX + "refractory",
    "coil": "rngtech:block/alloy_furnace/coil",
    "anvil": TEX + "anvil",
    "anvil_side": TEX + "anvil_side",
}
RAM_TEXTURES = {
    "ram": TEX + "ram",
    "ram_top": TEX + "ram_top",
    "die": TEX + "die",
    "upper_die": TEX + "upper_die",
    "pipe": FURNACE_TEX + "pipe",
}
CRUDE_RAM_TEXTURES = {**RAM_TEXTURES, "ram": TEX + "slide", "ram_top": TEX + "slide"}
SCREW_TEXTURES = {"screw": TEX + "screw", "trim": TEX + "trim/bronze", "bar": TEX + "bar", "weight": TEX + "weight"}


def model(textures, elements, parent="minecraft:block/block"):
    return {"parent": parent, "render_type": "minecraft:cutout", "textures": textures, "elements": elements}


def write_models() -> None:
    write_json(MODELS / "block/metal_press.json", model(STEEL_TEXTURES, steel_elements()))
    write_json(MODELS / "block/metal_press_active.json", {"parent": "rngtech:block/metal_press", "textures": {"lamp": FURNACE_TEX + "lamp_on"}})
    write_json(MODELS / "block/crude_metal_press.json", model(CRUDE_TEXTURES, crude_elements()))
    write_json(MODELS / "block/crude_metal_press_active.json",
               {"parent": "rngtech:block/crude_metal_press", "textures": {"lamp": FURNACE_TEX + "lamp_on"}})

    # Item models carry the Gear of the press's stage, which only the renderer draws in the world: a plate die, the
    # raised ram, the Heat Core, and on the Steel press its Servo. They leave out what hides inside the crown or
    # crosshead, which the wiki icon renderer would otherwise paint over the outside.
    die = {"die": TEX + "die", "cavity": TEX + "cavity/plate"}
    write_json(MODELS / "item/metal_press.json", model(
        {**STEEL_TEXTURES, **RAM_TEXTURES, **die, "core": CORE_TEX + "steel", "servo": SERVO_TEX + "steel",
         "accent": SERVO_TEX + "steel_accent"},
        die_elements() + steel_ram_elements(rod=False) + heat_core_elements() + servo_elements() + steel_elements(),
    ))
    gap = (RAM_UP + SLIDE[1][1], CROSSHEAD[0][1])
    write_json(MODELS / "item/crude_metal_press.json", model(
        {**CRUDE_TEXTURES, **CRUDE_RAM_TEXTURES, **SCREW_TEXTURES, **die, "core": CORE_TEX + "bronze",
         "shell": TEX + "shell/bronze", "trim": TEX + "trim/bronze"},
        die_elements() + crude_ram_elements() + heat_core_elements()
        + screw_elements(segments=(gap, (NUT[1][1], SPINDLE_Y[1]))) + crude_elements(),
    ))

    parts = MODELS / "block/metal_press"
    write_json(parts / "ram.json", {"textures": {**RAM_TEXTURES, "particle": "#ram"}, "elements": steel_ram_elements()})
    write_json(parts / "crude_ram.json", {"textures": {**CRUDE_RAM_TEXTURES, "particle": "#ram"}, "elements": crude_ram_elements()})
    write_json(parts / "screw.json", {"textures": {**SCREW_TEXTURES, "particle": "#screw"}, "elements": screw_elements()})
    write_json(parts / "die.json", {"textures": {"particle": "#die", "die": TEX + "die"}, "elements": die_elements()})
    for mold in MOLDS:
        write_json(parts / f"die_{mold}.json", {"parent": "rngtech:block/metal_press/die", "textures": {"cavity": TEX + f"cavity/{mold}"}})
    write_json(parts / "heat_core.json", {"textures": {"particle": "#core"}, "elements": heat_core_elements()})
    for material in HEAT_CORES:
        write_json(parts / f"heat_core_{material}.json", {"parent": "rngtech:block/metal_press/heat_core", "textures": {"core": CORE_TEX + material}})
    write_json(parts / "servo.json", {"textures": {"particle": "#servo"}, "elements": servo_elements()})
    for material in SERVOS:
        write_json(parts / f"servo_{material}.json", {
            "parent": "rngtech:block/metal_press/servo",
            "textures": {"servo": SERVO_TEX + material, "accent": SERVO_TEX + f"{material}_accent"},
        })


# ---------------------------------------------------------------- textures

STEEL = (72, 82, 94)
STEEL_TRIM = (150, 158, 168)
BRONZE = (156, 118, 68)
BRONZE_TRIM = (196, 162, 96)
TOOL_STEEL = (112, 118, 126)
IRON = (150, 150, 146)
RECESS = (22, 24, 28)


def save(image, name):
    furnace.save(image, name, root="metal_press")


def write_shell_textures() -> None:
    save(panel_seams(noisy(64, STEEL, "press_steel_shell", amount=4, streak=4), step=16, depth=18), "shell/steel")
    save(noisy(64, STEEL_TRIM, "press_steel_trim", amount=6, streak=6), "trim/steel")
    crown = noisy(64, shade(STEEL, -6), "press_crown_top", amount=4)
    pixels = crown.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 16), 255)
    save(panel_seams(crown, step=32, depth=16), "crown_top")
    liner = panel_seams(noisy(64, (58, 62, 68), "press_liner", amount=6), step=16, rivets=False, depth=16)
    blotches(liner, "press_liner_wear", (92, 96, 100), 4, 8, 0.4)
    save(liner, "liner")

    # The Crude press's bronze, from its recipe's Bronze Plates: cast columns and a riveted crosshead.
    shell = panel_seams(noisy(64, BRONZE, "press_bronze_shell", amount=7, streak=2), step=32, depth=26)
    blotches(shell, "press_bronze_tarnish", (96, 82, 52), 5, 10, 0.45)
    blotches(shell, "press_bronze_shine", (214, 176, 112), 3, 7, 0.35)
    save(shell, "shell/bronze")
    trim = noisy(64, BRONZE_TRIM, "press_bronze_trim", amount=7, streak=2)
    blotches(trim, "press_bronze_trim_tarnish", (130, 104, 62), 4, 8, 0.4)
    save(trim, "trim/bronze")
    column = noisy(64, shade(BRONZE, 8), "press_column", amount=5)
    pixels = column.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (0, 1):
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif x % 16 == 7:
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
    save(column, "column")


def write_work_textures() -> None:
    # Heated platen: tool steel, heat-tinted toward its middle. MetalPressRenderer draws the glow over it.
    platen = noisy(64, (70, 72, 78), "press_platen", amount=5)
    blotches(platen, "press_platen_heat", (110, 86, 92), 4, 14, 0.4)
    for i in range(0, 64, 16):
        for j in range(64):
            platen.putpixel((j, i), (*shade(platen.getpixel((j, i)), -18), 255))
    save(platen, "platen")

    # The Crude press's anvil plate, from its recipe's iron: hammered, with a darker worn face.
    anvil = noisy(64, IRON, "press_anvil", amount=9)
    blotches(anvil, "press_anvil_wear", (104, 104, 102), 6, 10, 0.5)
    save(anvil, "anvil")
    side = noisy(64, shade(IRON, -24), "press_anvil_side", amount=6, streak=4)
    save(side, "anvil_side")

    save(panel_seams(noisy(64, (84, 92, 104), "press_ram", amount=4, streak=5), step=32, depth=14), "ram")
    ram_top = noisy(64, (76, 84, 96), "press_ram_top", amount=4)
    save(panel_seams(ram_top, step=32, depth=14), "ram_top")
    slide = noisy(64, (128, 128, 124), "press_slide", amount=8)
    blotches(slide, "press_slide_rust", (122, 88, 64), 3, 8, 0.35)
    save(slide, "slide")

    # Die steel: ground tool steel with fine machining lines.
    die = noisy(64, TOOL_STEEL, "press_die", amount=4)
    pixels = die.load()
    for y in range(0, 64, 3):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], 8), 255)
    save(die, "die")

    # The upper die's punch face, mapped from a 20-texel square like the cavities: a flat, polished face.
    upper = noisy(64, shade(TOOL_STEEL, 14), "press_upper_die", amount=3)
    draw = ImageDraw.Draw(upper)
    draw.rectangle([0, 0, CAVITY_TEXELS - 1, CAVITY_TEXELS - 1], outline=(*shade(TOOL_STEEL, -20), 255), width=1)
    save(upper, "upper_die")

    tray = noisy(64, (52, 56, 62), "press_tray", amount=5)
    for x in range(0, 64, 8):
        for y in range(64):
            tray.putpixel((x, y), (*shade(tray.getpixel((x, y)), -14), 255))
    save(tray, "tray")
    rack = noisy(64, (46, 50, 58), "press_rack", amount=4)
    save(rack, "rack")

    # Screw spindle: steep thread ridges; fly bar and weights in forged iron.
    screw = noisy(64, (150, 154, 160), "press_screw", amount=4)
    pixels = screw.load()
    for y in range(64):
        for x in range(64):
            phase = (y + x // 2) % 6
            if phase in (0, 1):
                pixels[x, y] = (*shade(pixels[x, y], -40), 255)
            elif phase == 3:
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(screw, "screw")
    save(noisy(64, (92, 92, 96), "press_bar", amount=6, streak=6), "bar")
    weight = noisy(64, (70, 70, 74), "press_weight", amount=8)
    blotches(weight, "press_weight_scale", (96, 92, 88), 4, 9, 0.4)
    save(weight, "weight")


def cavity(name, draw_cavity):
    """A Mold's top face: the die steel with its engraved cavity, in the top-left 20-texel square."""
    image = noisy(64, TOOL_STEEL, f"press_cavity_{name}", amount=4)
    draw = ImageDraw.Draw(image)
    n = CAVITY_TEXELS - 1
    draw.rectangle([0, 0, n, n], outline=(*shade(TOOL_STEEL, -22), 255), width=1)
    draw.line([(1, 1), (n - 1, 1)], fill=(*shade(TOOL_STEEL, 24), 255))
    draw.line([(1, 1), (1, n - 1)], fill=(*shade(TOOL_STEEL, 24), 255))
    draw_cavity(draw)
    save(image, f"cavity/{name}")


def recess(draw, box_, fill=RECESS):
    """A sunk rectangle, lit on its lower and right inner walls."""
    x0, y0, x1, y1 = box_
    draw.rectangle([x0, y0, x1, y1], fill=(*fill, 255))
    draw.line([(x0, y1), (x1, y1)], fill=(*shade(TOOL_STEEL, 30), 255))
    draw.line([(x1, y0), (x1, y1)], fill=(*shade(TOOL_STEEL, 30), 255))


def write_cavity_textures() -> None:
    cavity("plate", lambda d: recess(d, (4, 5, 15, 14)))

    def gear(d):
        teeth = []
        for i in range(16):
            angle = math.radians(i * 22.5)
            radius = 7.5 if i % 2 == 0 else 5.5
            teeth.append((9.5 + math.cos(angle) * radius, 9.5 + math.sin(angle) * radius))
        d.polygon(teeth, fill=(*RECESS, 255))
        d.ellipse([7, 7, 12, 12], fill=(*TOOL_STEEL, 255))
        d.ellipse([8, 8, 11, 11], fill=(*shade(RECESS, 6), 255))

    cavity("gear", gear)

    def casing(d):
        recess(d, (3, 3, 16, 16))
        d.rectangle([6, 6, 13, 13], fill=(*shade(TOOL_STEEL, 6), 255))
        d.line([(6, 6), (13, 6)], fill=(*shade(TOOL_STEEL, -20), 255))

    cavity("casing", casing)

    def circuit(d):
        recess(d, (3, 4, 16, 15), fill=(28, 34, 30))
        trace = (*shade(TOOL_STEEL, 10), 255)
        for y in (6, 9, 12):
            d.line([(5, y), (14, y)], fill=trace)
        d.line([(9, 6), (9, 12)], fill=trace)
        for x, y in ((5, 6), (14, 9), (5, 12), (14, 12)):
            d.rectangle([x - 1, y - 1, x, y], fill=(184, 115, 51, 255))

    cavity("circuit", circuit)

    def connector(d):
        d.ellipse([3, 3, 16, 16], fill=(*RECESS, 255))
        d.ellipse([6, 6, 13, 13], fill=(*TOOL_STEEL, 255))
        d.ellipse([8, 8, 11, 11], fill=(*RECESS, 255))
        for x, y in ((9, 3), (9, 15), (3, 9), (15, 9)):
            d.rectangle([x, y, x + 1, y + 1], fill=(*shade(TOOL_STEEL, 10), 255))

    cavity("connector", connector)


def remove_old_textures() -> None:
    """The cube faces this design replaces."""
    for face in ("front", "side", "top", "bottom"):
        folder = OLD_TEXTURES / face
        if folder.is_dir():
            for path in sorted(folder.rglob("*"), reverse=True):
                path.unlink() if path.is_file() else path.rmdir()
            folder.rmdir()


def main() -> None:
    remove_old_textures()
    write_models()
    write_shell_textures()
    write_work_textures()
    write_cavity_textures()


if __name__ == "__main__":
    main()
