#!/usr/bin/env python3
"""Generate the Furnace and Alloy Furnace block models, part models, item models, and owned textures.

The Stage 0 Furnace is a smooth-stone kiln from its recipe: a fire mouth below a smelting oven,
and a copper-banded chimney. Every electric Furnace is an insulated muffle kiln on the slate
plinth: a viewport door shows the hearth and the heating elements inside, the Heat Core sits on
the crown as a stack of fins, the Battery Cell stands in a side cradle, the Nitrogen atmosphere
tank is a gas cylinder behind it, and a heat bar under the door reads the chamber temperature
against the core's limit. The Lead Furnace has four small
chambers behind four doors that share one crown core. Chassis differ by shell and trim metal.

The Alloy Furnace is a crucible furnace: the installed Crucible sits in a refractory well wrapped
in induction bands, fed from four hoppers on the back cabinet, with the Heat Core as a side
radiator, the Battery Cell in a cradle on the other side, the Nitrogen tank as a gas cylinder at
the front corner, and the Servo as a stirrer over the pot.

The housings are static block models. The installed parts are standalone part models; they, the
items, the heat glow, the fire, the molten pool, and the meters are drawn by FurnaceRenderer and
AlloyFurnaceRenderer, whose constants match the geometry here. Run from the repository root:

    python tools/blockmodels/furnace.py
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
TEXTURES = ASSETS / "textures/block"
TEX = "rngtech:block/furnace/"
ALLOY_TEX = "rngtech:block/alloy_furnace/"
# Heat Cores are the same item in a Solid Fuel Burner, so they keep the Burner's fin textures.
CORE_TEX = "rngtech:block/solid_fuel_burner/heat_core/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# ---------------------------------------------------------------- electric Furnace geometry
# Kiln body; the front faces north (z = 0) and the Battery Cell cradle stands on the east side.
BODY = ((1.0, 2.0, 1.0), (12.5, 13.0, 15.0))
# Single muffle chamber, open at the front behind the door.
CHAMBER = ((3.0, 4.5, 1.0), (10.5, 10.5, 10.0))
# Viewport inside the door frame.
VIEWPORT = ((3.5, 5.0), (10.0, 10.0))
# Heating element rods along each side wall, at these heights (bottom edge), 0.5 thick.
ROD_YS = (5.75, 7.5, 9.25)
ROD_Z = (1.75, 9.5)
ROD_SIZE = 0.5
# Setter plate on the chamber floor; items rest on its top.
SETTER = ((3.75, 4.5, 2.0), (9.75, 5.0, 9.0))
# Lead Furnace: four chambers, lanes 0-1 on the top row, 2-3 below, left to right.
QUAD_COLUMNS = ((2.25, 6.25), (7.25, 11.25))
QUAD_ROWS = ((8.25, 11.25), (4.25, 7.25))
QUAD_DEPTH = 9.0
QUAD_INSET = 0.5
# Heat bar under the door: a recessed strip read left to right.
HEAT_BAR = ((2.5, 2.5), (11.0, 3.5))
HEAT_BAR_Z = 0.5
# Crown: Heat Core socket and fins, and a vent behind them.
CROWN_Y = BODY[1][1]
SOCKET = ((2.0, CROWN_Y, 2.0), (11.5, CROWN_Y + 0.5, 10.0))
FIN_COUNT = 8
FIN_X0, FIN_X1 = 2.5, 11.0
FIN_WIDTH = 0.5
FIN_Y0, FIN_Y1 = CROWN_Y + 1.0, CROWN_Y + 3.0
FIN_Z0, FIN_Z1 = 2.75, 9.25
# Battery Cell cradle on the east side; the cell stands centred at (CELL_X, CELL_Z).
CRADLE = ((12.5, 2.0, 1.5), (15.5, 10.0, 9.0))
CELL_X, CELL_Z = 14.0, 5.25
CELL_FLOOR = 2.75
# Charge meter on the cradle's front post, facing north.
METER = ((12.75, 3.0, 1.0), (15.25, 9.5, 1.5))
# Cable port on the back, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Nitrogen atmosphere cylinder behind the cradle: a rounded bottle with a fill window on its east face.
CYLINDER = (14.0, 12.25)
CYLINDER_R, CYLINDER_FLAT = 1.5, 1.0
CYLINDER_Y = (2.0, 11.5)
CYLINDER_WINDOW = (3.5, 10.5)

FURNACE_TIERS = ("iron", "copper", "bronze", "steel", "lead", "aluminum", "titanium", "tungstensteel", "exotic")
# The Heat Core shown on each chassis item model, which has no renderer.
ITEM_CORES = {
    "iron": "iron", "copper": "copper", "bronze": "bronze", "steel": "steel", "lead": "steel",
    "aluminum": "aluminum", "titanium": "titanium", "tungstensteel": "titanium", "exotic": "exotic",
}
HEAT_CORES = ("iron", "copper", "bronze", "steel", "aluminum", "sparksteel", "titanium", "exotic", "unique")

# ---------------------------------------------------------------- Stage 0 Furnace geometry
KILN = ((1.0, 1.0, 1.0), (15.0, 12.0, 15.0))
FIRE_MOUTH = ((5.0, 1.0, 1.0), (11.0, 4.5, 9.0))
OVEN = ((4.0, 6.0, 1.0), (12.0, 10.0, 10.0))
KILN_CHIMNEY = (8.0, 12.0)
KILN_CHIMNEY_R, KILN_CHIMNEY_FLAT = 2.0, 1.25


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


def stack(cx, cz, radius, flat, y0, y1, textures, faces=FACES):
    """A rounded vertical stack: two crossed slabs, axis-aligned so it also renders as a wiki icon."""
    return [
        box((cx - radius, y0, cz - flat), (cx + radius, y1, cz + flat), textures, faces),
        box((cx - flat, y0, cz - radius), (cx + flat, y1, cz + radius), textures, faces),
    ]


def plinth():
    return box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})


def lamp(start, end):
    return box(start, end, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}})


def chamber_shell(chamber, body_z1, front_z):
    """Refractory-lined walls around one open-fronted chamber, as faces seen from inside."""
    (x0, y0, z0), (x1, y1, z1) = chamber
    return [
        box((x0, y0, z1), (x1, y1, z1 + 0.01), {"north": "refractory"}, faces=("north",)),
        box((x0, y0 - 0.01, front_z), (x1, y0, z1), {"up": "hearth"}, faces=("up",)),
        box((x0, y1, front_z), (x1, y1 + 0.01, z1), {"down": "refractory"}, faces=("down",)),
        box((x0 - 0.01, y0, front_z), (x0, y1, z1), {"east": "refractory"}, faces=("east",)),
        box((x1, y0, front_z), (x1 + 0.01, y1, z1), {"west": "refractory"}, faces=("west",)),
    ]


def solid_with_holes(outer, holes, texture_map):
    """A box with rectangular front-open holes, split into slabs so no face crosses a hole."""
    (bx0, by0, bz0), (bx1, by1, bz1) = outer
    xs = sorted({bx0, bx1, *[h[0][0] for h in holes], *[h[1][0] for h in holes]})
    ys = sorted({by0, by1, *[h[0][1] for h in holes], *[h[1][1] for h in holes]})
    elements = []
    for i in range(len(xs) - 1):
        for j in range(len(ys) - 1):
            cx0, cx1, cy0, cy1 = xs[i], xs[i + 1], ys[j], ys[j + 1]
            z_front = bz0
            for (hx0, hy0, _), (hx1, hy1, hz1) in holes:
                if hx0 <= cx0 and cx1 <= hx1 and hy0 <= cy0 and cy1 <= hy1:
                    z_front = hz1
            faces = []
            if cx0 == bx0:
                faces.append("west")
            if cx1 == bx1:
                faces.append("east")
            if cy0 == by0:
                faces.append("down")
            if cy1 == by1:
                faces.append("up")
            faces += ["north", "south"]
            elements.append(box((cx0, cy0, z_front), (cx1, cy1, bz1), texture_map, faces=faces))
    return elements


def door(viewport, frame, z0=0.25, z1=1.0, glass_z=0.6, hinge_side="west"):
    """A door frame around a viewport, with framed glass, two hinges, and a handle."""
    (vx0, vy0), (vx1, vy1) = viewport
    fx0, fy0, fx1, fy1 = vx0 - frame, vy0 - frame, vx1 + frame, vy1 + frame
    trim = ("north", "east", "west", "up", "down")
    elements = [
        box((fx0, vy1, z0), (fx1, fy1, z1), "trim", faces=trim),
        box((fx0, fy0, z0), (fx1, vy0, z1), "trim", faces=trim),
        box((fx0, vy0, z0), (vx0, vy1, z1), "trim", faces=("north", "east", "west")),
        box((vx1, vy0, z0), (fx1, vy1, z1), "trim", faces=("north", "east", "west")),
        box((vx0, vy0, glass_z - 0.05), (vx1, vy1, glass_z + 0.05), "glass", faces=("north", "south"),
            uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}),
    ]
    hx0, hx1 = (fx0 - 0.25, fx0 + 0.25) if hinge_side == "west" else (fx1 - 0.25, fx1 + 0.25)
    span = fy1 - fy0
    for y in (fy0 + span * 0.2, fy1 - span * 0.2 - 0.75):
        elements.append(box((hx0, y, z0 - 0.25), (hx1, y + 0.75, z1), "pipe", faces=("north", "east", "west", "up", "down")))
    handle_x = fx1 - frame / 2 if hinge_side == "west" else fx0 + frame / 2
    elements.append(box((handle_x - 0.2, fy0 + span * 0.35, z0 - 0.5), (handle_x + 0.2, fy1 - span * 0.35, z0),
                        "pipe", faces=("north", "east", "west", "up", "down")))
    return elements


def heat_bar(bar, z, back=BODY[0][2]):
    (bx0, by0), (bx1, by1) = bar
    return box((bx0 - 0.25, by0 - 0.25, z), (bx1 + 0.25, by1 + 0.25, back), {"north": "heat_bar", "*": "trim"},
               faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 16, 2]})


def cradle_elements(cradle=CRADLE, cell=(CELL_X, CELL_Z), meter=METER, inner="west", feed_to=BODY[1][2] - 2.0):
    """An open Battery Cell cradle against the machine's `inner` side, with the charge meter on its front post."""
    (x0, y0, z0), (x1, y1, z1) = cradle
    outer = "east" if inner == "west" else "west"
    px0, px1 = (x0 + 0.25, x1) if inner == "west" else (x0, x1 - 0.25)
    cell_x, cell_z = cell
    feed_x0, feed_x1 = (x0, x0 + 1.0) if inner == "west" else (x1 - 1.0, x1)
    elements = [
        box((x0, y0, z0), (x1, y0 + 0.75, z1), {"up": "socket", "*": "casing"}, faces=("north", "south", outer, "up")),
        # Rear post and top strap; the front post carries the charge meter.
        box((px0, y0, z1 - 1.0), (px1, y1, z1), "casing", faces=SIDES + ("up",)),
        box((px0, y1 - 0.75, z0 + 0.5), (px1, y1, z1 - 1.0), "trim", faces=("north", outer, "up", "down")),
        box((px0, y0, z0), (px1, y1, z0 + 0.5), "casing", faces=SIDES + ("up",)),
        box(meter[0], meter[1], {"north": "meter", "*": "trim"}, faces=("north", "east", "west", "up", "down"),
            uv={"north": [0, 0, 6, 16]}),
        # Contact plate under the cell, wired into the machine behind the cradle.
        box((cell_x - 1.25, y0 + 0.75, cell_z - 1.25), (cell_x + 1.25, CELL_FLOOR, cell_z + 1.25), "pipe",
            faces=SIDES + ("up",)),
        box((feed_x0, 4.0, z1), (feed_x1, 5.0, feed_to), "pipe", faces=("north", outer, "up", "down")),
    ]
    return elements


def cylinder_elements(centre, radius, flat, ys, window, window_face):
    """The Nitrogen atmosphere tank: a gas bottle with a nitrogen band, a fill window, a valve collar, and a handwheel.
    The renderer draws the fill in the window and lights the collar while a lane uses Nitrogen."""
    cx, cz = centre
    y0, y1 = ys
    elements = stack(cx, cz, radius + 0.25, flat + 0.25, y0, y0 + 0.5, "trim", faces=SIDES + ("up",))
    elements += stack(cx, cz, radius, flat, y0 + 0.5, y1, {"*": "cylinder", "up": "cylinder"}, faces=SIDES + ("up",))
    elements += stack(cx, cz, radius + 0.05, flat + 0.05, y1 - 2.0, y1 - 1.25, "nitrogen_band", faces=SIDES)
    elements += stack(cx, cz, radius * 0.6, flat * 0.6, y1, y1 + 0.75, "cylinder", faces=SIDES + ("up",))
    elements += stack(cx, cz, 0.6, 0.6, y1 + 0.75, y1 + 1.5, "collar", faces=SIDES + ("up",))
    elements.append(box((cx - 0.75, y1 + 1.5, cz - 0.15), (cx + 0.75, y1 + 1.8, cz + 0.15), "pipe", faces=SIDES + ("up",)))
    elements.append(box((cx - 0.15, y1 + 1.5, cz - 0.75), (cx + 0.15, y1 + 1.8, cz + 0.75), "pipe", faces=SIDES + ("up",)))
    w0, w1 = window
    if window_face == "east":
        x = cx + radius
        elements.append(box((x, w0 - 0.25, cz - 0.6), (x + 0.1, w1 + 0.25, cz + 0.6), {"east": "cylinder_window", "*": "trim"},
                            faces=("east", "north", "south", "up", "down"), uv={"east": [0, 0, 4, 16]}))
    else:
        z = cz - radius
        elements.append(box((cx - 0.6, w0 - 0.25, z - 0.1), (cx + 0.6, w1 + 0.25, z), {"north": "cylinder_window", "*": "trim"},
                            faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 4, 16]}))
    return elements


def crown_elements():
    (bx0, _, bz0), (bx1, by1, bz1) = BODY
    elements = [
        box(SOCKET[0], SOCKET[1], "socket", faces=("north", "south", "east", "west", "up")),
        # Vent hood behind the core, and the teal band below the crown.
        box((2.5, by1, 10.75), (11.0, by1 + 1.5, 14.25), {"up": "vent", "*": "trim"}, faces=("north", "south", "east", "west", "up")),
        box((bx0 - 0.25, by1 - 1.25, bz0 - 0.25), (bx1 + 0.25, by1 - 0.75, bz1), "band", faces=("north", "east", "west", "down")),
    ]
    return elements


def port_elements():
    return [box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]})]


def electric_elements(rods=True):
    (bx0, by0, bz0), (bx1, by1, bz1) = BODY
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CHAMBER
    shell = {"*": "shell", "up": "crown"}
    elements = [plinth()]
    elements += solid_with_holes(BODY, [CHAMBER], shell)
    elements += chamber_shell(CHAMBER, bz1, cz0)
    for y in ROD_YS if rods else ():
        for x0, inward in ((cx0, "east"), (cx1 - ROD_SIZE, "west")):
            elements.append(box((x0, y, ROD_Z[0]), (x0 + ROD_SIZE, y + ROD_SIZE, ROD_Z[1]), "element",
                                faces=("north", inward, "up", "down")))
    elements.append(box(SETTER[0], SETTER[1], "setter", faces=("north", "east", "west", "up")))
    elements += door(VIEWPORT, 1.0)
    elements.append(heat_bar(HEAT_BAR, HEAT_BAR_Z))
    elements += crown_elements()
    elements += cradle_elements(feed_to=CYLINDER[1] - CYLINDER_R)
    elements += port_elements()
    elements.append(lamp((13.25, CRADLE[1][1], 6.25), (14.75, CRADLE[1][1] + 1.0, 7.75)))
    elements += furnace_cylinder()
    return elements


def furnace_cylinder():
    elements = cylinder_elements(CYLINDER, CYLINDER_R, CYLINDER_FLAT, CYLINDER_Y, CYLINDER_WINDOW, "east")
    # Gas line from the valve into the crown.
    top = CYLINDER_Y[1] + 0.75
    elements.append(box((BODY[1][0] - 0.5, top + 0.25, CYLINDER[1] - 0.25), (CYLINDER[0] - 0.5, top + 0.75, CYLINDER[1] + 0.25), "pipe",
                        faces=("north", "south", "up", "down")))
    return elements


def quad_chambers():
    chambers = []
    for (y0, y1) in QUAD_ROWS:
        for (x0, x1) in QUAD_COLUMNS:
            chambers.append(((x0, y0, BODY[0][2]), (x1, y1, QUAD_DEPTH)))
    return chambers


def quad_elements(rods=True):
    (bx0, by0, bz0), (bx1, by1, bz1) = BODY
    shell = {"*": "shell", "up": "crown"}
    chambers = quad_chambers()
    elements = [plinth()]
    elements += solid_with_holes(BODY, chambers, shell)
    for chamber in chambers:
        (x0, y0, z0), (x1, y1, z1) = chamber
        elements += chamber_shell(chamber, bz1, z0)
        for rod_x, inward in ((x0, "east"), (x1 - ROD_SIZE, "west")) if rods else ():
            elements.append(box((rod_x, y1 - 1.0, 1.75), (rod_x + ROD_SIZE, y1 - 0.5, z1 - 0.5), "element",
                                faces=("north", inward, "up", "down")))
        elements += door(((x0 + QUAD_INSET, y0 + QUAD_INSET), (x1 - QUAD_INSET, y1 - QUAD_INSET)), 0.5,
                         z0=0.4, z1=1.0, glass_z=0.7)
    for i, (x0, x1) in enumerate(QUAD_COLUMNS):
        elements.append(heat_bar(((x0 + 0.25, 2.5), (x1 - 0.25, 3.5)), HEAT_BAR_Z))
    elements += crown_elements()
    elements += cradle_elements(feed_to=CYLINDER[1] - CYLINDER_R)
    elements += port_elements()
    elements.append(lamp((13.25, CRADLE[1][1], 6.25), (14.75, CRADLE[1][1] + 1.0, 7.75)))
    elements += furnace_cylinder()
    return elements


def fin_elements(texture="core"):
    """The Heat Core: a base plate and a row of cooling fins on the crown."""
    elements = [box((FIN_X0, SOCKET[1][1], FIN_Z0 - 0.25), (FIN_X1, FIN_Y0, FIN_Z1 + 0.25), texture,
                    faces=("north", "south", "east", "west", "up"))]
    pitch = (FIN_X1 - FIN_X0 - FIN_WIDTH) / (FIN_COUNT - 1)
    for i in range(FIN_COUNT):
        x = FIN_X0 + pitch * i
        elements.append(box((x, FIN_Y0, FIN_Z0), (x + FIN_WIDTH, FIN_Y1, FIN_Z1), texture,
                            faces=("north", "south", "east", "west", "up")))
    return elements


def kiln_elements():
    """The Stage 0 Furnace: a smooth-stone kiln with a fire mouth, an oven, and a copper-banded chimney."""
    (kx0, ky0, kz0), (kx1, ky1, kz1) = KILN
    stone = {"*": "stone", "up": "stone_top"}
    elements = [box((0.5, 0, 0.5), (15.5, 1, 15.5), {"up": "slab_top", "down": "slab_top", "*": "slab_side"})]
    elements += solid_with_holes(KILN, [FIRE_MOUTH, OVEN], stone)
    (fx0, fy0, fz0), (fx1, fy1, fz1) = FIRE_MOUTH
    (ox0, oy0, oz0), (ox1, oy1, oz1) = OVEN
    elements += [
        box((fx0, fy0, fz1), (fx1, fy1, fz1 + 0.01), {"north": "soot"}, faces=("north",)),
        box((fx0, fy0 - 0.01, fz0), (fx1, fy0, fz1), {"up": "ash"}, faces=("up",)),
        box((fx0, fy1, fz0), (fx1, fy1 + 0.01, fz1), {"down": "soot"}, faces=("down",)),
        box((fx0 - 0.01, fy0, fz0), (fx0, fy1, fz1), {"east": "soot"}, faces=("east",)),
        box((fx1, fy0, fz0), (fx1 + 0.01, fy1, fz1), {"west": "soot"}, faces=("west",)),
    ]
    elements += [
        box((ox0, oy0, oz1), (ox1, oy1, oz1 + 0.01), {"north": "kiln_brick"}, faces=("north",)),
        box((ox0, oy0 - 0.01, oz0), (ox1, oy0, oz1), {"up": "oven_floor"}, faces=("up",)),
        box((ox0, oy1, oz0), (ox1, oy1 + 0.01, oz1), {"down": "soot"}, faces=("down",)),
        box((ox0 - 0.01, oy0, oz0), (ox0, oy1, oz1), {"east": "kiln_brick"}, faces=("east",)),
        box((ox1, oy0, oz0), (ox1 + 0.01, oy1, oz1), {"west": "kiln_brick"}, faces=("west",)),
    ]
    # Dark stone lintels over both openings, and a sill under the oven.
    elements.append(box((fx0 - 0.75, fy1, 0.5), (fx1 + 0.75, fy1 + 1.0, kz0), "lintel", faces=("north", "east", "west", "up", "down")))
    elements.append(box((ox0 - 0.75, oy1, 0.5), (ox1 + 0.75, oy1 + 1.0, kz0), "lintel", faces=("north", "east", "west", "up", "down")))
    elements.append(box((ox0 - 0.5, oy0 - 0.5, 0.5), (ox1 + 0.5, oy0, kz0), "slab_side", faces=("north", "east", "west", "up", "down")))
    # Copper band and soot-lipped chimney on the crown, toward the back.
    cx, cz = KILN_CHIMNEY
    elements += stack(cx, cz, KILN_CHIMNEY_R + 0.5, KILN_CHIMNEY_FLAT + 0.5, ky1, ky1 + 0.75, "stone",
                      faces=("north", "south", "east", "west", "up"))
    elements += stack(cx, cz, KILN_CHIMNEY_R, KILN_CHIMNEY_FLAT, ky1 + 0.75, 15.5, "stone", faces=SIDES)
    elements += stack(cx, cz, KILN_CHIMNEY_R + 0.25, KILN_CHIMNEY_FLAT + 0.25, 13.5, 14.25, "copper",
                      faces=("north", "south", "east", "west", "up", "down"))
    elements += stack(cx, cz, KILN_CHIMNEY_R + 0.25, KILN_CHIMNEY_FLAT + 0.25, 15.5, 16.0, {"up": "chimney_mouth", "*": "copper"},
                      faces=("north", "south", "east", "west", "up", "down"))
    # A copper draught plate on the fire-mouth side.
    elements.append(box((kx1, 2.0, 4.0), (kx1 + 0.5, 4.5, 8.0), {"east": "draught", "*": "copper"},
                        faces=("north", "south", "east", "up", "down"), uv={"east": [0, 0, 16, 10]}))
    return elements


def kiln_item_elements():
    (fx0, fy0, fz0), (fx1, _, fz1) = FIRE_MOUTH
    return [box((fx0 + 0.5, fy0, fz0 + 1.0), (fx1 - 0.5, fy0 + 1.0, fz1 - 1.0), "coal", faces=("north", "up"))]


# ---------------------------------------------------------------- Alloy Furnace geometry
# Crucible well centred at (WELL_X, WELL_Z): an octagonal refractory jacket around a square well.
WELL_X, WELL_Z = 8.0, 6.5
JACKET_TOP = 10.5
WELL = ((5.0, 4.0, 3.5), (11.0, JACKET_TOP, 9.5))
JACKET_WALLS = (
    ((5.0, 2.0, 2.0), (11.0, JACKET_TOP, 3.5)),
    ((5.0, 2.0, 9.5), (11.0, JACKET_TOP, 11.0)),
    ((3.5, 2.0, 3.5), (5.0, JACKET_TOP, 9.5)),
    ((11.0, 2.0, 3.5), (12.5, JACKET_TOP, 9.5)),
    ((4.25, 2.0, 2.75), (5.0, JACKET_TOP, 3.5)),
    ((11.0, 2.0, 2.75), (11.75, JACKET_TOP, 3.5)),
    ((4.25, 2.0, 9.5), (5.0, JACKET_TOP, 10.25)),
    ((11.0, 2.0, 9.5), (11.75, JACKET_TOP, 10.25)),
)
# The east wall carries the radiator socket, so the coils skip it.
COILED_WALLS = (0, 1, 2, 4, 5, 6, 7)
COIL_YS = (3.5, 5.5, 7.5)
COIL_HEIGHT = 1.0
# Crucible pot inside the well; the molten pool fills the inner pot between POOL_Y0 and POOL_Y1.
POT = ((5.25, 4.0, 3.75), (10.75, 10.5, 9.25))
POT_WALL = 0.5
POOL_Y0, POOL_Y1 = 5.0, 10.0
# Heat Core radiator on the east wall of the jacket: a base plate and horizontal fins.
RADIATOR_PLATE = ((12.75, 3.0, 3.5), (13.25, 10.0, 9.5))
RADIATOR_FINS = 7
RADIATOR_FIN = (13.25, 15.5)
RADIATOR_FIN_Z = (3.75, 9.25)
# Battery Cell cradle on the west side.
ALLOY_CRADLE = ((0.5, 2.0, 2.25), (3.5, 10.0, 9.25))
ALLOY_CELL_X, ALLOY_CELL_Z = 2.0, 5.5
ALLOY_METER = ((0.75, 3.0, 1.75), (3.25, 9.5, 2.25))
# Heat bar on a console in front of the jacket.
ALLOY_HEAT_BAR = ((5.25, 2.5), (10.75, 3.5))
ALLOY_HEAT_BAR_Z = 0.75
# Back cabinet with the cable port, and four feed hoppers on its top.
CABINET = ((1.0, 2.0, 11.25), (15.0, 12.0, 15.0))
HOPPERS = ((1.5, 4.0), (4.25, 6.75), (9.25, 11.75), (12.0, 14.5))
HOPPER_Z = (11.75, 14.75)
HOPPER_Y = (12.0, 14.0)
HOPPER_WALL = 0.375
HOPPER_FLOOR = 12.5
# Servo stirrer: a column and arm from the cabinet carry the motor over the crucible.
MOTOR = ((6.5, 12.0, 5.0), (9.5, 14.25, 8.0))
# Nitrogen cylinder at the front east corner, with its fill window facing north.
ALLOY_CYLINDER = (14.25, 1.75)
ALLOY_CYLINDER_R, ALLOY_CYLINDER_FLAT = 1.25, 0.75
ALLOY_CYLINDER_Y = (2.0, 9.25)
ALLOY_CYLINDER_WINDOW = (3.25, 8.25)
STIRRER_SHAFT = 0.5
STIRRER_Y = (5.25, 12.0)
ALLOY_TIERS = ("bronze", "steel", "titanium")
CRUCIBLES = {
    "bronze": (176, 116, 52),
    "steel": (112, 120, 124),
    "titanium": (150, 164, 180),
    "unique": (72, 36, 107),
}
SERVOS = ("steel", "titanium", "tungstensteel", "nullite", "exotic", "unique")
SERVO_TEX = "rngtech:block/compressor_tank/servo/"


def alloy_elements():
    shell = {"*": "shell", "up": "jacket_top"}
    elements = [plinth()]
    for (x0, y0, z0), (x1, y1, z1) in JACKET_WALLS:
        elements.append(box((x0, y0, z0), (x1, y1, z1), shell))
    (wx0, wy0, wz0), (wx1, wy1, wz1) = WELL
    elements.append(box((wx0, 2.0, wz0), (wx1, wy0, wz1), {"up": "refractory"}, faces=("up",)))
    elements += [
        box((wx0, wy0, wz1 - 0.01), (wx1, wy1, wz1), {"north": "refractory"}, faces=("north",)),
        box((wx0, wy0, wz0), (wx1, wy1, wz0 + 0.01), {"south": "refractory"}, faces=("south",)),
        box((wx0, wy0, wz0), (wx0 + 0.01, wy1, wz1), {"east": "refractory"}, faces=("east",)),
        box((wx1 - 0.01, wy0, wz0), (wx1, wy1, wz1), {"west": "refractory"}, faces=("west",)),
    ]
    # Copper induction coils wrapped around every jacket face but the east one, which carries the radiator socket.
    for y in COIL_YS:
        for index in COILED_WALLS:
            (x0, _, z0), (x1, _, z1) = JACKET_WALLS[index]
            elements.append(box((x0 - 0.25, y, z0 - 0.25), (x1 + 0.25, y + COIL_HEIGHT, z1 + 0.25), "coil"))
    elements.append(box((12.5, 2.75, 3.5), (12.75, 10.25, 9.5), "socket", faces=("north", "south", "east", "up", "down")))
    # Console with the heat bar in front of the jacket.
    (hx0, hy0), (hx1, hy1) = ALLOY_HEAT_BAR
    elements.append(box((hx0 - 0.5, 2.0, ALLOY_HEAT_BAR_Z + 0.25), (hx1 + 0.5, hy0 - 0.25, 1.75), "casing",
                        faces=("north", "east", "west", "up")))
    elements.append(heat_bar(ALLOY_HEAT_BAR, ALLOY_HEAT_BAR_Z, back=1.75))
    elements += cradle_elements(ALLOY_CRADLE, (ALLOY_CELL_X, ALLOY_CELL_Z), ALLOY_METER, inner="east", feed_to=CABINET[0][2])
    elements += cylinder_elements(ALLOY_CYLINDER, ALLOY_CYLINDER_R, ALLOY_CYLINDER_FLAT, ALLOY_CYLINDER_Y, ALLOY_CYLINDER_WINDOW, "north")
    elements.append(box((11.75, 8.75, 2.25), (ALLOY_CYLINDER[0] - 0.75, 9.25, 2.75), "pipe", faces=("north", "south", "up", "down")))
    # Back cabinet, teal band, port, and lamp.
    (kx0, ky0, kz0), (kx1, ky1, kz1) = CABINET
    elements.append(box((kx0, ky0, kz0), (kx1, ky1, kz1), {"up": "socket", "*": "casing"}, faces=SIDES + ("up",)))
    elements.append(box((kx0 - 0.25, ky1 - 1.25, kz0 - 0.25), (kx1 + 0.25, ky1 - 0.75, kz1), "band", faces=("north", "east", "west", "down")))
    elements += port_elements()
    elements.append(lamp((13.0, 9.75, kz0 - 0.5), (14.5, 10.75, kz0)))
    # Feed hoppers, each with a chute into the collector trough that runs along the cabinet front.
    for x0, x1 in HOPPERS:
        z0, z1 = HOPPER_Z
        y0, y1 = HOPPER_Y
        elements += [
            box((x0, y0, z0), (x0 + HOPPER_WALL, y1, z1), "hopper"),
            box((x1 - HOPPER_WALL, y0, z0), (x1, y1, z1), "hopper"),
            box((x0 + HOPPER_WALL, y0, z0), (x1 - HOPPER_WALL, y1, z0 + HOPPER_WALL), "hopper"),
            box((x0 + HOPPER_WALL, y0, z1 - HOPPER_WALL), (x1 - HOPPER_WALL, y1, z1), "hopper"),
            box((x0 + HOPPER_WALL, y0, z0 + HOPPER_WALL), (x1 - HOPPER_WALL, HOPPER_FLOOR, z1 - HOPPER_WALL), "hopper", faces=("up",)),
            box(((x0 + x1) / 2 - 0.5, 11.75, kz0 - 0.25), ((x0 + x1) / 2 + 0.5, 12.25, z0), "pipe", faces=("north", "east", "west", "up")),
        ]
    elements.append(box((1.5, 11.25, 10.25), (14.5, 12.0, kz0), {"up": "trough", "*": "trim"}, faces=("north", "east", "west", "up", "down")))
    elements.append(box((7.25, 10.75, 9.0), (8.75, 11.25, 10.25), {"up": "trough", "*": "trim"}, faces=("north", "east", "west", "up", "down")))
    return elements


def crucible_elements(texture="crucible"):
    """The Crucible: an open pot in the well, with a lip over the jacket and a pour spout at the front."""
    (x0, y0, z0), (x1, y1, z1) = POT
    w = POT_WALL
    return [
        box((x0, y0, z0), (x1, y0 + w, z1), texture, faces=("up",)),
        box((x0, y0, z0), (x0 + w, y1, z1), texture, faces=("east",)),
        box((x1 - w, y0, z0), (x1, y1, z1), texture, faces=("west",)),
        box((x0 + w, y0, z0), (x1 - w, y1, z0 + w), texture, faces=("south",)),
        box((x0 + w, y0, z1 - w), (x1 - w, y1, z1), texture, faces=("north",)),
        box((x0 - 0.5, y1, z0 - 0.5), (x1 + 0.5, y1 + 0.75, z0 + w), texture),
        box((x0 - 0.5, y1, z1 - w), (x1 + 0.5, y1 + 0.75, z1 + 0.5), texture),
        box((x0 - 0.5, y1, z0 + w), (x0 + w, y1 + 0.75, z1 - w), texture),
        box((x1 - w, y1, z0 + w), (x1 + 0.5, y1 + 0.75, z1 - w), texture),
        box((WELL_X - 0.75, y1, z0 - 1.25), (WELL_X + 0.75, y1 + 0.5, z0 - 0.5), texture, faces=("north", "east", "west", "up", "down")),
    ]


def radiator_elements(texture="core"):
    """The Heat Core on an Alloy Furnace: a base plate on the jacket and a stack of horizontal fins."""
    elements = [box(RADIATOR_PLATE[0], RADIATOR_PLATE[1], texture, faces=("north", "south", "east", "up", "down"))]
    (_, py0, _), (_, py1, _) = RADIATOR_PLATE
    pitch = (py1 - py0 - 0.5) / (RADIATOR_FINS - 1)
    for i in range(RADIATOR_FINS):
        y = py0 + pitch * i
        elements.append(box((RADIATOR_FIN[0], y, RADIATOR_FIN_Z[0]), (RADIATOR_FIN[1], y + 0.5, RADIATOR_FIN_Z[1]), texture,
                            faces=("north", "south", "east", "up", "down")))
    return elements


def servo_elements():
    """The Servo: a column and arm from the cabinet carrying the stirrer motor over the crucible."""
    (mx0, my0, mz0), (mx1, my1, mz1) = MOTOR
    return [
        box((7.25, 12.0, 12.0), (8.75, 15.25, 13.5), "servo"),
        box((7.25, 14.25, mz1), (8.75, 15.25, 12.0), "servo", faces=("east", "west", "up", "down")),
        box((mx0, my0, mz0), (mx1, my1, mz1), "servo"),
        box((mx0 - 0.125, my0 + 1.0, mz0 - 0.125), (mx1 + 0.125, my0 + 1.5, mz1 + 0.125), "accent", faces=SIDES),
        box((7.0, my1, 5.5), (9.0, 15.25, 7.5), "servo", faces=SIDES + ("up",)),
    ]


def stirrer_elements():
    """The stirrer shaft and paddles, drawn turning by AlloyFurnaceRenderer about (WELL_X, WELL_Z)."""
    half = STIRRER_SHAFT / 2
    y0, y1 = STIRRER_Y
    return [
        box((WELL_X - half, y0, WELL_Z - half), (WELL_X + half, y1, WELL_Z + half), "accent", faces=SIDES + ("down",)),
        box((WELL_X - 1.75, y0, WELL_Z - 0.25), (WELL_X + 1.75, y0 + 1.0, WELL_Z + 0.25), "accent"),
        box((WELL_X - 0.25, y0, WELL_Z - 1.75), (WELL_X + 0.25, y0 + 1.0, WELL_Z + 1.75), "accent"),
    ]


SHARED = {
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "band": TEX + "band",
    "port": TEX + "port",
    "socket": TEX + "socket",
    "pipe": TEX + "pipe",
    "meter": TEX + "meter",
    "lamp": TEX + "lamp_off",
    "cylinder": TEX + "cylinder",
    "nitrogen_band": TEX + "nitrogen_band",
    "cylinder_window": TEX + "cylinder_window",
    "collar": TEX + "collar",
}

FURNACE_TEXTURES = {
    **SHARED,
    "refractory": TEX + "refractory",
    "hearth": TEX + "hearth",
    "setter": TEX + "setter",
    "element": TEX + "element",
    "glass": TEX + "glass",
    "heat_bar": TEX + "heat_bar",
    "vent": TEX + "vent",
}

KILN_TEXTURES = {
    "particle": TEX + "kiln/stone",
    "stone": TEX + "kiln/stone",
    "stone_top": TEX + "kiln/stone_top",
    "slab_top": TEX + "kiln/slab_top",
    "slab_side": TEX + "kiln/slab_side",
    "soot": TEX + "kiln/soot",
    "ash": TEX + "kiln/ash",
    "kiln_brick": TEX + "kiln/brick",
    "oven_floor": TEX + "kiln/oven_floor",
    "lintel": TEX + "kiln/lintel",
    "copper": TEX + "kiln/copper",
    "chimney_mouth": TEX + "kiln/chimney_mouth",
    "draught": TEX + "kiln/draught",
}


def tier_textures(tier):
    return {"particle": TEX + f"shell/{tier}", "shell": TEX + f"shell/{tier}", "trim": TEX + f"trim/{tier}",
            "crown": TEX + f"crown/{tier}"}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_furnace_models() -> None:
    write_json(MODELS / "block/furnace/electric.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": FURNACE_TEXTURES, "elements": electric_elements(),
    })
    write_json(MODELS / "block/furnace/quad.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": FURNACE_TEXTURES, "elements": quad_elements(),
    })
    # Item models leave out the heating rods: they barely show at item size, and the wiki icon renderer would draw them
    # over the chamber walls.
    for template, elements in (("electric", electric_elements(rods=False)), ("quad", quad_elements(rods=False))):
        write_json(MODELS / f"item/furnace_{template}_template.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": FURNACE_TEXTURES, "elements": fin_elements() + elements,
        })
    for tier in FURNACE_TIERS:
        name = f"{tier}_furnace_chassis"
        template = "quad" if tier == "lead" else "electric"
        write_json(MODELS / f"block/{name}.json", {"parent": f"rngtech:block/furnace/{template}", "textures": tier_textures(tier)})
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": TEX + "lamp_on"}})
        write_json(MODELS / f"item/{name}.json", {
            "parent": f"rngtech:item/furnace_{template}_template",
            "textures": {**tier_textures(tier), "core": CORE_TEX + ITEM_CORES[tier]},
        })

    write_json(MODELS / "block/furnace.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": KILN_TEXTURES, "elements": kiln_elements(),
    })
    write_json(MODELS / "block/furnace_active.json", {"parent": "rngtech:block/furnace"})
    write_json(MODELS / "item/furnace.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {**KILN_TEXTURES, "coal": TEX + "kiln/coal"},
        "elements": kiln_item_elements() + kiln_elements(),
    })

    parts = MODELS / "block/furnace"
    write_json(parts / "heat_core.json", {"textures": {"particle": "#core"}, "elements": fin_elements()})
    for material in HEAT_CORES:
        write_json(parts / f"heat_core_{material}.json", {"parent": "rngtech:block/furnace/heat_core", "textures": {"core": CORE_TEX + material}})


ALLOY_TEXTURES = {
    **SHARED,
    "refractory": TEX + "refractory",
    "heat_bar": TEX + "heat_bar",
    "jacket_top": ALLOY_TEX + "jacket_top",
    "coil": ALLOY_TEX + "coil",
    "hopper": ALLOY_TEX + "hopper",
    "trough": ALLOY_TEX + "trough",
}


def write_alloy_models() -> None:
    write_json(MODELS / "block/alloy_furnace/housing.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": ALLOY_TEXTURES, "elements": alloy_elements(),
    })
    write_json(MODELS / "item/alloy_furnace_template.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": ALLOY_TEXTURES, "elements": crucible_elements() + radiator_elements() + alloy_elements(),
    })
    for tier in ALLOY_TIERS:
        name = f"{tier}_alloy_furnace_chassis"
        write_json(MODELS / f"block/{name}.json", {"parent": "rngtech:block/alloy_furnace/housing", "textures": tier_textures(tier)})
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": TEX + "lamp_on"}})
        write_json(MODELS / f"item/{name}.json", {
            "parent": "rngtech:item/alloy_furnace_template",
            "textures": {**tier_textures(tier), "core": CORE_TEX + tier, "crucible": ALLOY_TEX + f"crucible/{tier}"},
        })

    parts = MODELS / "block/alloy_furnace"
    write_json(parts / "heat_core.json", {"textures": {"particle": "#core"}, "elements": radiator_elements()})
    write_json(parts / "crucible.json", {"textures": {"particle": "#crucible"}, "elements": crucible_elements()})
    write_json(parts / "servo.json", {"textures": {"particle": "#servo"}, "elements": servo_elements()})
    write_json(parts / "stirrer.json", {"textures": {"particle": "#accent"}, "elements": stirrer_elements()})
    for material in HEAT_CORES:
        write_json(parts / f"heat_core_{material}.json", {"parent": "rngtech:block/alloy_furnace/heat_core", "textures": {"core": CORE_TEX + material}})
    for material in CRUCIBLES:
        write_json(parts / f"crucible_{material}.json", {
            "parent": "rngtech:block/alloy_furnace/crucible", "textures": {"crucible": ALLOY_TEX + f"crucible/{material}"},
        })
    for material in SERVOS:
        textures = {"servo": SERVO_TEX + material, "accent": SERVO_TEX + f"{material}_accent"}
        write_json(parts / f"servo_{material}.json", {"parent": "rngtech:block/alloy_furnace/servo", "textures": textures})
        write_json(parts / f"stirrer_{material}.json", {"parent": "rngtech:block/alloy_furnace/stirrer", "textures": textures})


def write_alloy_textures() -> None:
    # Jacket top: refractory ring, scorched toward the well.
    top = noisy(64, (150, 132, 108), "jacket_top", amount=7)
    blotches(top, "jacket_scorch", (84, 66, 54), 6, 12, 0.45)
    save(top, "jacket_top", root="alloy_furnace")

    # Induction coil: copper windings, one turn every 4 texels, on an insulating backing.
    coil = noisy(64, (196, 112, 64), "coil", amount=6)
    pixels = coil.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade((60, 40, 30), 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 34), 255)
    save(coil, "coil", root="alloy_furnace")

    hopper = noisy(64, (104, 112, 122), "hopper", amount=5, streak=3)
    pixels = hopper.load()
    for y in range(64):
        for x in range(64):
            if x % 16 == 0 or y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
    save(hopper, "hopper", root="alloy_furnace")
    trough = noisy(64, (70, 66, 64), "trough", amount=7)
    blotches(trough, "trough_spill", (150, 120, 80), 4, 6, 0.4)
    save(trough, "trough", root="alloy_furnace")

    # Crucibles: cast metal, heat-scaled toward the rim. The Unique Crying Crucible is crying obsidian.
    for material, color in CRUCIBLES.items():
        if material == "unique":
            pot = noisy(64, (24, 6, 46), "crucible_unique", amount=6)
            blotches(pot, "crucible_unique_veins", (126, 70, 174), 6, 9, 0.7)
            pixels = pot.load()
            rand = rng("crucible_unique_tears")
            for _ in range(40):
                x, y = rand.randrange(64), rand.randrange(64)
                pixels[x, y] = (190, 110, 255, 255)
        else:
            pot = noisy(64, color, f"crucible_{material}", amount=6, streak=4)
            pixels = pot.load()
            for y in range(64):
                for x in range(64):
                    if y % 16 in (0, 15):
                        pixels[x, y] = (*shade(pixels[x, y], -24 if y % 16 == 0 else 14), 255)
            blotches(pot, f"crucible_{material}_scale", mix(color, (50, 40, 36), 0.5), 4, 10, 0.4)
        save(pot, f"crucible/{material}", root="alloy_furnace")


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"furnace:{name}")


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


def courses(image, height, length, mortar, name, offset_rows=True, darken=0.0):
    """Masonry courses: `height` and `length` texels per block, with mortar joints."""
    pixels = image.load()
    size = image.size[0]
    rand = rng(name)
    tones = {}
    for y in range(size):
        row = y // height
        offset = length // 2 if offset_rows and row % 2 else 0
        for x in range(size):
            block = (row, (x + offset) // length)
            if block not in tones:
                tones[block] = rand.uniform(-8, 8)
            if y % height == 0 or (x + offset) % length == 0:
                pixels[x, y] = (*mortar, 255)
            else:
                base = shade(pixels[x, y], tones[block])
                if darken:
                    base = mix(base, (30, 26, 24), darken * (1.0 - (y % height) / height))
                pixels[x, y] = (*base, 255)
    return image


def save(image, name, root="furnace"):
    path = TEXTURES / root / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
# Nitrogen, as on the Process-tab gauge.
NITROGEN = (139, 164, 201)

# Shell, trim, and crown colours for each chassis metal.
TIER_COLORS = {
    "iron": ((132, 132, 128), (176, 176, 170)),
    "copper": ((136, 78, 46), (216, 138, 86)),
    "bronze": ((166, 122, 64), (206, 170, 94)),
    "steel": ((72, 82, 94), (150, 158, 168)),
    "lead": ((78, 84, 102), (122, 128, 148)),
    "aluminum": ((168, 176, 188), (214, 222, 230)),
    "titanium": ((112, 120, 136), (172, 180, 194)),
    "tungstensteel": ((52, 54, 64), (106, 110, 124)),
    "exotic": ((34, 64, 60), (52, 170, 132)),
}


def write_tier_textures() -> None:
    for tier, (shell_color, trim_color) in TIER_COLORS.items():
        name = f"{tier}_shell"
        if tier == "iron":
            shell = panel_seams(noisy(64, shell_color, name, amount=7, streak=3), step=32, depth=24)
            blotches(shell, "iron_rust", (120, 74, 46), 4, 9, 0.45)
        elif tier == "copper":
            shell = noisy(64, shell_color, name, amount=7)
            pixels = shell.load()
            rand = rng("copper_dimples")
            for _ in range(90):
                x, y = rand.randrange(64), rand.randrange(64)
                pixels[x, y] = (*shade(pixels[x, y], 26), 255)
                pixels[(x + 1) % 64, (y + 1) % 64] = (*shade(pixels[(x + 1) % 64, (y + 1) % 64], -18), 255)
            panel_seams(shell, step=32, depth=30)
            blotches(shell, "copper_tarnish", (96, 52, 32), 5, 11, 0.55)
        elif tier == "bronze":
            shell = panel_seams(noisy(64, shell_color, name, amount=5, streak=10), step=16, rivets=False, depth=24)
        elif tier == "lead":
            # Soft lead sheet: no rivets, lapped seams, and shallow dents.
            shell = noisy(64, shell_color, name, amount=5, streak=2)
            pixels = shell.load()
            for y in range(64):
                for x in range(64):
                    if y % 21 == 0:
                        pixels[x, y] = (*shade(pixels[x, y], -20), 255)
                    elif y % 21 == 1:
                        pixels[x, y] = (*shade(pixels[x, y], 14), 255)
            blotches(shell, "lead_dents", shade(shell_color, -14), 6, 7, 0.5)
            blotches(shell, "lead_bloom", (150, 154, 162), 3, 10, 0.25)
        elif tier == "exotic":
            shell = panel_seams(noisy(64, shell_color, name, amount=4, streak=3), step=16, depth=16)
            pixels = shell.load()
            rand = rng("exotic_veins")
            for _ in range(5):
                x, y = rand.uniform(0, 64), rand.uniform(0, 64)
                for _ in range(30):
                    x, y = (x + rand.uniform(-1.2, 1.6)) % 64, (y + rand.uniform(-1.2, 1.6)) % 64
                    pixels[int(x), int(y)] = (*shade((64, 196, 156), rand.uniform(-16, 8)), 255)
        else:
            step = {"steel": 16, "aluminum": 32, "titanium": 16, "tungstensteel": 16}[tier]
            shell = panel_seams(noisy(64, shell_color, name, amount=4, streak=4), step=step, depth=18)
            if tier == "titanium":
                blotches(shell, "titanium_heat_tint", (120, 110, 150), 3, 12, 0.35)
        save(shell, f"shell/{tier}")
        save(noisy(64, trim_color, f"{tier}_trim", amount=6, streak=6), f"trim/{tier}")

        # Crown: the shell metal, heat-darkened toward the middle.
        crown = noisy(64, shade(shell_color, -10), f"{tier}_crown", amount=5)
        blotches(crown, f"{tier}_crown_heat", mix(shell_color, (60, 40, 30), 0.4), 4, 14, 0.4)
        save(crown, f"crown/{tier}")


def write_furnace_textures() -> None:
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

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    socket = noisy(64, (52, 58, 66), "socket", amount=4)
    pixels = socket.load()
    for y in range(64):
        for x in range(64):
            if (x % 16 in (2, 13)) and y % 6 == 3:
                pixels[x, y] = (*shade((150, 158, 168), 0), 255)
                pixels[x, (y + 1) % 64] = (*shade(SLATE_DARK, -6), 255)
    save(socket, "socket")

    # Refractory lining: pale firebrick courses; the setter and hearth are plain refractory slabs.
    lining = noisy(64, (196, 174, 140), "refractory", amount=8)
    courses(lining, 8, 16, (150, 132, 108), "refractory_courses")
    save(lining, "refractory")
    hearth = noisy(64, (170, 150, 120), "hearth", amount=8)
    blotches(hearth, "hearth_scorch", (110, 88, 70), 5, 10, 0.5)
    save(hearth, "hearth")
    setter = noisy(64, (214, 200, 176), "setter", amount=5)
    panel_seams(setter, step=32, rivets=False, depth=18)
    save(setter, "setter")

    # Heating element: dark coiled resistance wire. FurnaceRenderer draws the glow over it.
    element = noisy(64, (70, 62, 58), "element", amount=6)
    pixels = element.load()
    for y in range(64):
        for x in range(64):
            if (x + y // 2) % 4 == 0:
                pixels[x, y] = (*shade((40, 34, 32), 0), 255)
            elif (x + y // 2) % 4 == 1:
                pixels[x, y] = (*shade((118, 104, 96), 0), 255)
    save(element, "element")

    # Viewport glass, mapped whole onto each pane: clear, with two glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for offset in (0, 4):
        draw.line([(8 + offset, 4), (4, 8 + offset)], fill=(214, 236, 244, 255), width=2)
    draw.line([(58, 44), (44, 58)], fill=(214, 236, 244, 255), width=1)
    save(glass, "glass")

    # Heat bar, mapped as a 16:2 strip: a dark recessed track. FurnaceRenderer draws the fill and target mark.
    bar = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bar)
    draw.rectangle([0, 0, 63, 7], fill=(*shade(STEEL, 10), 255))
    draw.rectangle([1, 1, 62, 6], fill=(16, 18, 22, 255))
    for x in range(8, 63, 8):
        draw.point((x, 1), fill=(*shade(STEEL, 30), 255))
    save(bar, "heat_bar")

    vent = noisy(64, (54, 60, 68), "vent", amount=4)
    pixels = vent.load()
    for y in range(64):
        for x in range(4, 60):
            if y % 6 in (0, 1):
                pixels[x, y] = (*shade(SLATE_DARK, -12), 255)
            elif y % 6 == 2:
                pixels[x, y] = (*shade(STEEL, 20), 255)
    save(vent, "vent")

    # Charge meter, mapped as a 6:16 plate (columns 0-23): steel rails around eight dark LED slots.
    meter = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = meter.load()
    for y in range(64):
        for x in range(24):
            if x in (0, 1, 22, 23) or y in (0, 1, 62, 63):
                pixels[x, y] = (*shade(STEEL, 14 if x in (0, 23) or y in (0, 63) else -12), 255)
            else:
                pixels[x, y] = (18, 22, 28, 255)
    height = METER[1][1] - METER[0][1]
    for i in range(METER_SEGMENTS):
        bottom, top = meter_segment(i)
        for y in range(round((METER[1][1] - top) / height * 64), round((METER[1][1] - bottom) / height * 64)):
            for x in range(6, 18):
                pixels[x, y] = (36, 44, 40, 255)
    save(meter, "meter")

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

    save(noisy(64, (52, 30, 12), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (255, 176, 64), "lamp_on", amount=6), "lamp_on")

    # Nitrogen cylinder: dark painted steel with a pale nitrogen band, the colour of the Process-tab gauge.
    save(noisy(64, (58, 62, 70), "cylinder", amount=4, streak=6), "cylinder")
    band = noisy(64, NITROGEN, "nitrogen_band", amount=5)
    pixels = band.load()
    for x in range(64):
        pixels[x, 0] = (*shade(NITROGEN, -40), 255)
        pixels[x, 63] = (*shade(NITROGEN, -40), 255)
    save(band, "nitrogen_band")
    # Fill window, mapped as a 4:16 strip (columns 0-15): steel rails around a dark track.
    window = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(window)
    draw.rectangle([0, 0, 15, 63], fill=(*shade(STEEL, 10), 255))
    draw.rectangle([3, 3, 12, 60], fill=(16, 18, 24, 255))
    save(window, "cylinder_window")
    collar = noisy(64, (70, 78, 88), "collar", amount=4)
    save(collar, "collar")

    write_tier_textures()
    write_kiln_textures()
    write_fx_textures()


def write_kiln_textures() -> None:
    """Stage 0 kiln: smooth stone from its recipe, sooted inside, with copper fittings."""
    stone = noisy(64, (160, 160, 160), "kiln_stone", amount=5)
    courses(stone, 16, 32, (118, 118, 118), "kiln_stone_courses")
    save(stone, "kiln/stone")
    top = noisy(64, (166, 166, 166), "kiln_stone_top", amount=5)
    pixels = top.load()
    for y in range(64):
        for x in range(64):
            if x % 32 == 0 or y % 32 == 0:
                pixels[x, y] = (122, 122, 122, 255)
    save(top, "kiln/stone_top")

    slab_top = noisy(64, (172, 172, 172), "kiln_slab_top", amount=4)
    ImageDraw.Draw(slab_top).rectangle([0, 0, 63, 63], outline=(128, 128, 128, 255), width=1)
    save(slab_top, "kiln/slab_top")
    slab_side = noisy(64, (156, 156, 156), "kiln_slab_side", amount=4)
    pixels = slab_side.load()
    for x in range(64):
        for y in (0, 31, 32, 63):
            pixels[x, y] = (120, 120, 120, 255)
    save(slab_side, "kiln/slab_side")

    soot = noisy(64, (46, 42, 40), "kiln_soot", amount=6)
    courses(soot, 16, 32, (30, 28, 27), "kiln_soot_courses")
    save(soot, "kiln/soot")
    ash = noisy(64, (110, 106, 100), "kiln_ash", amount=14)
    pixels = ash.load()
    rand = rng("kiln_cinders")
    for _ in range(70):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((44, 40, 38), rand.uniform(-6, 6)), 255)
    save(ash, "kiln/ash")
    brick = noisy(64, (132, 116, 100), "kiln_brick", amount=7)
    courses(brick, 8, 16, (84, 74, 66), "kiln_brick_courses", darken=0.35)
    save(brick, "kiln/brick")
    floor = noisy(64, (128, 120, 110), "kiln_oven_floor", amount=6)
    blotches(floor, "kiln_floor_scorch", (70, 60, 52), 4, 12, 0.5)
    save(floor, "kiln/oven_floor")
    lintel = noisy(64, (88, 88, 90), "kiln_lintel", amount=12)
    courses(lintel, 16, 21, (58, 58, 60), "kiln_lintel_courses", offset_rows=False)
    save(lintel, "kiln/lintel")
    copper = noisy(64, (196, 112, 64), "kiln_copper", amount=8, streak=6)
    blotches(copper, "kiln_copper_patina", (92, 150, 120), 3, 8, 0.4)
    save(copper, "kiln/copper")
    save(noisy(64, (20, 18, 18), "kiln_chimney_mouth", amount=4), "kiln/chimney_mouth")

    # Draught plate, mapped as a 16:10 plate: a copper slide over three air holes.
    draught = noisy(64, (178, 100, 58), "kiln_draught", amount=6)
    draw = ImageDraw.Draw(draught)
    draw.rectangle([0, 0, 63, 39], outline=(120, 64, 36, 255), width=2)
    for x0 in (12, 28, 44):
        draw.ellipse([x0, 14, x0 + 8, 24], fill=(18, 14, 12, 255))
    save(draught, "kiln/draught")

    coal = noisy(64, (34, 33, 34), "kiln_coal", amount=8)
    pixels = coal.load()
    rand = rng("kiln_coal_glints")
    for _ in range(50):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((92, 92, 98), rand.uniform(-8, 8)), 255)
    save(coal, "kiln/coal")


def write_fx_textures() -> None:
    """Renderer textures: wall glow, element glow, molten metal, and cooled crust. The fire and LED reuse the Solid Fuel
    Burner's textures."""
    # Wall glow: a soft hotspot that never fades to black, so the corners of a hot chamber stay lit.
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)
            value = clamp(255 * (0.35 + 0.65 * (1.0 - distance) ** 1.2))
            pixels[x, y] = (value, value, value, 255)
    save(glow, "fx/glow")

    # Element glow: a bright coil core fading to the edges; tinted by temperature.
    coil = Image.new("RGBA", (16, 16))
    pixels = coil.load()
    for y in range(16):
        for x in range(16):
            across = 1.0 - abs(y + 0.5 - 8) / 8
            turn = 0.7 + 0.3 * math.cos((x + y * 0.5) * math.pi / 2)
            value = clamp(255 * across ** 0.8 * turn)
            pixels[x, y] = (value, value, value, 255)
    save(coil, "fx/coil")

    # Molten metal: a bright, slowly varying surface in greys; the renderer tints it by temperature.
    molten = Image.new("RGBA", (32, 32))
    pixels = molten.load()
    rand = rng("molten")
    blobs = [(rand.uniform(0, 32), rand.uniform(0, 32), rand.uniform(4, 9)) for _ in range(9)]
    for y in range(32):
        for x in range(32):
            value = 0.75
            for bx, by, rad in blobs:
                d = math.hypot(min(abs(x - bx), 32 - abs(x - bx)), min(abs(y - by), 32 - abs(y - by))) / rad
                if d < 1:
                    value += 0.12 * (1 - d) ** 2
            value += rand.uniform(-0.04, 0.04)
            v = clamp(255 * min(1.0, value))
            pixels[x, y] = (v, v, v, 255)
    save(molten, "fx/molten")

    # Cold crust on a pool that has cooled: dark slag with a few cracks.
    crust = noisy(16, (66, 60, 58), "crust", amount=10)
    pixels = crust.load()
    rand = rng("crust_cracks")
    for _ in range(12):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (*shade((30, 26, 26), rand.uniform(-6, 6)), 255)
    save(crust, "fx/crust")


METER_SEGMENTS = 8


def meter_segment(index):
    """Bottom and top of one charge-meter LED, in model pixels."""
    y0 = METER[0][1] + 0.5 + index * 0.6875
    return y0, y0 + 0.5


def main() -> None:
    write_furnace_models()
    write_alloy_models()
    write_furnace_textures()
    write_alloy_textures()


if __name__ == "__main__":
    main()
