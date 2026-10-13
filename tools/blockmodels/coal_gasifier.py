#!/usr/bin/env python3
"""Generate the Coal Gasifier block model, item model, and owned textures.

The design follows a fixed-bed coal gasifier. A tall refractory-lined pressure vessel stands on a quench chamber in the
middle of the block. Coal Dust drops from a feed hopper on top through a rotary valve into the vessel, and a view port
on the vessel front shows the glowing bed. The installed Heat Core is the igniter, a finned element in a socket on the
vessel's side, and the installed Servo drives the rotary valve. Raw Syngas leaves the vessel shoulder through a line
with a pressure gauge into the Syngas sight glass; quench water comes from the other sight glass into the quench
chamber, whose ash lock discharges Gasification Residue into a bin at the front. The control cabinet behind the vessel
carries the cable port and the Battery Cell cradle.

The housing is a static block model; the reaction glow, the coal in the hopper, the rotary valve hub, the Servo, the
Heat Core, both tank levels, the residue in the bin, the gauge needle, and the Battery Cell are drawn by
CoalGasifierRenderer, whose constants match the geometry here. Run from the repository root:

    python tools/blockmodels/coal_gasifier.py
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
TEXTURES = ASSETS / "textures/block/coal_gasifier"
TEX = "rngtech:block/coal_gasifier/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))

# The front faces north (z = 0). West (x = 0) is on the viewer's right.
PLINTH_TOP = 1.5
# Reactor vessel: a vertical octagon on a quench chamber, both around (VESSEL_X, VESSEL_Z).
VESSEL_X, VESSEL_Z, VESSEL_R = 8.0, 8.5, 3.5
VESSEL_Y0, VESSEL_Y1 = 4.75, 11.25
QUENCH_R = 3.0
# View port on the vessel's front face.
PORT_X0, PORT_X1, PORT_Y0, PORT_Y1 = 7.0, 9.0, 7.5, 9.5
# Rotary feed valve between vessel and hopper; its hub turns about an east-west axis.
VALVE_Y, VALVE_Z = 12.875, 8.5
HUB_X0, HUB_X1 = 5.55, 6.5
# Feed hopper on top, open to the sky: the Coal Dust level rises from its floor.
HOPPER = ((5.0, 14.0, 5.5), (11.0, 16.0, 11.5))
HOPPER_FLOOR = 14.5
WALL = 0.5
# Residue bin at the front, under the ash lock discharge chute.
BIN = ((5.0, PLINTH_TOP, 0.75), (11.0, 3.75, 4.5))
BIN_FLOOR = 2.0
# Sight glasses: quench water at the front west, Syngas at the front east.
WATER_X, SYNGAS_X, SIGHT_Z0, SIGHT_Z1 = 0.75, 13.25, 1.5, 4.0
SIGHT_Y0, SIGHT_Y1 = 3.5, 10.5
# Heat Core socket on the vessel's west face; the renderer draws the installed core in front of it.
SOCKET = ((3.5, 6.75, 7.25), (4.6, 10.0, 9.75))
# Servo on a bracket west of the valve, Battery Cell cradle on the cabinet's east side.
SERVO = ((3.0, 11.85, 7.5), (5.5, 13.9, 9.5))
CELL = ((12.75, 2.0, 12.5), (15.25, 7.0, 15.0))
CABINET = ((3.5, PLINTH_TOP, 12.25), (12.5, 12.5, 15.0))
# Cable port on the control cabinet, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Pressure gauge on the Syngas line above the Syngas sight glass.
DIAL = ((13.0, 11.6, 1.25), (15.5, 14.1, 2.25))
PIPE = 0.75


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, rotation=None, uv=None, extra=None):
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
    if rotation:
        element["rotation"] = rotation
    if extra:
        element.update(extra)
    return element


FULL = [0, 0, 16, 16]


def vertical_octagon(radius, y0, y1, side, end=None, cx=VESSEL_X, cz=VESSEL_Z):
    """An octagonal prism around a vertical axis, one face per side so no faces overlap.

    Two straight slabs give the four flat sides and two slabs turned 45 degrees the diagonals. The ends are one square
    whose texture is cut to the octagon.
    """
    half = radius * TAN
    origin = [r(cx), r((y0 + y1) / 2), r(cz)]
    narrow = ((cx - half, y0, cz - radius), (cx + half, y1, cz + radius))
    elements = [
        box((cx - radius, y0, cz - half), (cx + radius, y1, cz + half), side, faces=("east", "west")),
        box(*narrow, side, faces=("north", "south")),
        box(*narrow, side, faces=("north", "south"), rotation={"origin": origin, "axis": "y", "angle": 45}),
        box(*narrow, side, faces=("north", "south"), rotation={"origin": origin, "axis": "y", "angle": -45}),
    ]
    if end:
        elements.append(box((cx - radius, y0, cz - radius), (cx + radius, y1, cz + radius), end, faces=("up", "down"),
                            uv={"up": FULL, "down": FULL}))
    return elements


# Sight-glass faces map a framed strip of the texture: the narrow (2 px) north and south faces use columns 0-15, the
# wider (2.5 px) east and west faces use columns 32-51, matching the Syngas Combustor's sight glasses.
SIGHT_UV = {
    "north": [0, 0, 4, 16],
    "south": [0, 0, 4, 16],
    "east": [8, 0, 13, 16],
    "west": [8, 0, 13, 16],
}


def sight_glass(x0):
    x1 = x0 + 2.0
    return [
        box((x0 - 0.25, PLINTH_TOP, SIGHT_Z0 - 0.25), (x1 + 0.25, SIGHT_Y0, SIGHT_Z1 + 0.25), "pipe"),
        box((x0 - 0.25, SIGHT_Y1, SIGHT_Z0 - 0.25), (x1 + 0.25, SIGHT_Y1 + 1.0, SIGHT_Z1 + 0.25), "pipe"),
        box((x0, SIGHT_Y0, SIGHT_Z0), (x1, SIGHT_Y1, SIGHT_Z1), "sight_glass", faces=SIDES, uv=SIGHT_UV),
    ]


def pipe(start, end, faces=FACES):
    return box(start, end, "pipe", faces=faces)


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, PLINTH_TOP, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))

    # Quench chamber under the vessel, then the refractory-lined vessel with two teal bands, a bolted top flange, and a
    # domed head.
    elements += vertical_octagon(QUENCH_R, PLINTH_TOP, VESSEL_Y0, "quench")
    elements += vertical_octagon(QUENCH_R + 0.25, 3.75, 4.25, "pipe", end="ring_end")
    elements += vertical_octagon(VESSEL_R, VESSEL_Y0, VESSEL_Y1, "vessel", end="vessel_end")
    elements += vertical_octagon(VESSEL_R + 0.25, 5.75, 6.5, "band", end="band_end")
    elements += vertical_octagon(VESSEL_R + 0.25, 10.25, 10.9, "band", end="band_end")
    elements += vertical_octagon(VESSEL_R + 0.25, VESSEL_Y1, 11.75, "flange", end="flange_end")
    for angle in (45, 135, 225, 315):
        bx = VESSEL_X + math.cos(math.radians(angle)) * 3.3
        bz = VESSEL_Z + math.sin(math.radians(angle)) * 3.3
        elements.append(box((bx - 0.4, 11.75, bz - 0.4), (bx + 0.4, 12.25, bz + 0.4), "bolt"))
    elements += vertical_octagon(2.75, 11.75, 12.5, "flange", end="head")

    # View port: a dark liner behind the glass in a steel frame. The bed glow sits between liner and glass.
    front = VESSEL_Z - VESSEL_R
    elements.append(box((PORT_X0, PORT_Y0, front - 0.05), (PORT_X1, PORT_Y1, front), "liner", faces=("north",)))
    elements.append(box((PORT_X0, PORT_Y0, front - 0.3), (PORT_X1, PORT_Y1, front - 0.3), "glass", faces=("north",)))
    frame_z = (front - 0.5, front)
    for x0, x1 in ((PORT_X0 - 0.5, PORT_X0), (PORT_X1, PORT_X1 + 0.5)):
        elements.append(box((x0, PORT_Y0 - 0.5, frame_z[0]), (x1, PORT_Y1 + 0.5, frame_z[1]), "flange"))
    for y0, y1 in ((PORT_Y0 - 0.5, PORT_Y0), (PORT_Y1, PORT_Y1 + 0.5)):
        elements.append(box((PORT_X0, y0, frame_z[0]), (PORT_X1, y1, frame_z[1]), "flange"))

    # Rotary feed valve and the hopper neck above it.
    elements.append(box((6.5, 12.25, 7.25), (9.5, 13.5, 9.75), {"west": "valve_end", "*": "valve"}, uv={"west": FULL}))
    elements.append(box((6.5, 13.5, 7.0), (9.5, 14.0, 10.0), "hopper"))

    # Feed hopper: an open bin whose floor shows a grate while empty.
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HOPPER
    elements.append(box((hx0, hy0, hz0), (hx1, HOPPER_FLOOR, hz1), {"up": "grate", "*": "hopper"}))
    for start, end in (
            ((hx0, HOPPER_FLOOR, hz0), (hx1, hy1, hz0 + WALL)),
            ((hx0, HOPPER_FLOOR, hz1 - WALL), (hx1, hy1, hz1)),
            ((hx0, HOPPER_FLOOR, hz0 + WALL), (hx0 + WALL, hy1, hz1 - WALL)),
            ((hx1 - WALL, HOPPER_FLOOR, hz0 + WALL), (hx1, hy1, hz1 - WALL)),
    ):
        elements.append(box(start, end, {"up": "flange", "*": "hopper"}))

    # Servo bracket west of the valve, with a socket plate the installed Servo sits on and a strut to the core socket.
    (vx0, vy0, vz0), (vx1, vy1, vz1) = SERVO
    elements.append(box((vx0 - 0.25, vy0 - 0.5, vz0 - 0.25), (vx1 + 0.25, vy0, vz1 + 0.25), {"up": "socket", "*": "casing"}))
    elements.append(box((3.5, 10.25, 8.25), (4.0, vy0 - 0.5, 8.75), "casing"))

    # Heat Core socket on the vessel's west face, fed by a power conduit from the cabinet.
    elements.append(box(*SOCKET, {"west": "socket", "*": "casing"}))
    elements.append(box((3.6, 8.5, SOCKET[1][2]), (4.3, 9.25, CABINET[0][2]), "conduit"))

    # Raw Syngas line: up from the vessel shoulder, east, forward, and down into the Syngas sight glass.
    gx = SYNGAS_X + 1.0 - PIPE / 2
    gz = (SIGHT_Z0 + SIGHT_Z1) / 2 - PIPE / 2
    elements.append(pipe((10.6, 11.75, 8.125), (11.35, 13.0, 8.875)))
    elements.append(pipe((11.35, 12.25, 8.125), (gx + PIPE, 13.0, 8.875), faces=("north", "south", "up", "down", "east")))
    elements.append(pipe((gx, 12.25, gz), (gx + PIPE, 13.0, 8.125), faces=("north", "east", "west", "up", "down")))
    elements.append(pipe((gx, SIGHT_Y1 + 1.0, gz), (gx + PIPE, 12.25, gz + PIPE), faces=SIDES))

    # Pressure gauge on the Syngas line.
    (dx0, dy0, dz0), (dx1, dy1, dz1) = DIAL
    elements.append(box((dx0, dy0, dz0), (dx1, dy1, dz1), {"north": "gauge", "*": "casing"}, uv={"north": FULL}))
    elements.append(box((14.0, 12.3, dz1), (14.5, 12.9, gz + 0.1), "flange"))

    # Quench water sight glass and the line into the quench chamber.
    elements += sight_glass(WATER_X)
    elements += sight_glass(SYNGAS_X)
    elements.append(pipe((1.375, 2.25, SIGHT_Z1 + 0.25), (2.125, 3.0, 7.375), faces=("east", "west", "up", "down", "south")))
    elements.append(pipe((2.125, 2.25, 6.625), (5.5, 3.0, 7.375), faces=("north", "south", "up", "down")))

    # Ash lock: a gate valve on the quench chamber front and a chute discharging into the residue bin.
    elements.append(box((6.75, 3.5, 4.75), (9.25, VESSEL_Y0, 5.6), {"north": "valve_end", "*": "valve"}, uv={"north": FULL}))
    elements.append(box((7.25, 3.85, 2.75), (8.75, 4.5, 4.75), {"down": "soot", "*": "pipe"}))

    # Residue bin: a floor, four walls, and a handle on the front.
    (bx0, by0, bz0), (bx1, by1, bz1) = BIN
    elements.append(box((bx0, by0, bz0), (bx1, BIN_FLOOR, bz1), {"up": "soot", "*": "bin"}))
    for start, end in (
            ((bx0, BIN_FLOOR, bz0), (bx1, by1, bz0 + WALL)),
            ((bx0, BIN_FLOOR, bz1 - WALL), (bx1, by1, bz1)),
            ((bx0, BIN_FLOOR, bz0 + WALL), (bx0 + WALL, by1, bz1 - WALL)),
            ((bx1 - WALL, BIN_FLOOR, bz0 + WALL), (bx1, by1, bz1 - WALL)),
    ):
        elements.append(box(start, end, {"up": "flange", "*": "bin"}))
    elements.append(box((6.5, 2.75, 0.4), (9.5, 3.25, 0.75), "flange"))

    # Control cabinet with the cable port.
    elements.append(box(*CABINET, {"up": "casing", "down": "casing", "*": "cabinet"}))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))

    # Battery Cell cradle: a base, a back plate against the cabinet, and a clamp bar with the cell terminal.
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CELL
    elements.append(box((cx0 - 0.25, PLINTH_TOP, cz0 - 0.25), (cx1 + 0.25, cy0, cz1 + 0.25), "flange"))
    elements.append(box((CABINET[1][0], PLINTH_TOP, cz0 - 0.25), (cx0, cy1 + 1.0, cz1 + 0.25), {"east": "socket", "*": "casing"}))
    elements.append(box((cx0, cy1 + 0.25, 13.4), (cx1 + 0.25, cy1 + 1.0, 14.1), "bus"))

    # Status lamp on the plinth front.
    elements.append(box(
        (3.25, PLINTH_TOP, 0.5), (4.75, PLINTH_TOP + 1.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def item_extra_elements():
    """Static valve hub for the item model, which has no renderer. A fresh item has no Gear installed."""
    h = 0.75
    return [
        box((HUB_X0, VALVE_Y - h, VALVE_Z - h), (HUB_X1, VALVE_Y + h, VALVE_Z + h), "coupling"),
        box((HUB_X0 - 0.1, VALVE_Y - 0.3, VALVE_Z - 0.3), (HUB_X1 - 0.1, VALVE_Y + 0.3, VALVE_Z + 0.3), "bolt",
            rotation={"origin": [r((HUB_X0 + HUB_X1) / 2), VALVE_Y, VALVE_Z], "axis": "x", "angle": 45}),
    ]


HOUSING_TEXTURES = {
    "particle": TEX + "vessel",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "quench": TEX + "quench",
    "ring_end": TEX + "ring_end",
    "vessel": TEX + "vessel",
    "vessel_end": TEX + "vessel_end",
    "band": TEX + "band",
    "band_end": TEX + "band_end",
    "flange": TEX + "flange",
    "flange_end": TEX + "flange_end",
    "head": TEX + "head",
    "bolt": TEX + "bolt",
    "liner": TEX + "liner",
    "glass": TEX + "glass",
    "valve": TEX + "valve",
    "valve_end": TEX + "valve_end",
    "hopper": TEX + "hopper",
    "grate": TEX + "grate",
    "socket": TEX + "socket",
    "conduit": TEX + "conduit",
    "pipe": TEX + "pipe",
    "gauge": TEX + "gauge",
    "sight_glass": TEX + "sight_glass",
    "soot": TEX + "soot",
    "bin": TEX + "bin",
    "cabinet": TEX + "cabinet",
    "port": TEX + "port",
    "bus": TEX + "bus",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/coal_gasifier.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/coal_gasifier_active.json", {
        "parent": "rngtech:block/coal_gasifier",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/coal_gasifier.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            **HOUSING_TEXTURES,
            "coupling": TEX + "coupling",
        },
        "elements": housing + item_extra_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"coal_gasifier:{name}")


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


def panel_seams(image, step=16, rivets=True):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            if x % step == 0 or y % step == 0:
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
            elif x % step == 1 or y % step == 1:
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    if rivets:
        for py in range(0, size, step):
            for px in range(0, size, step):
                for dx, dy in ((3, 3), (step - 4, 3), (3, step - 4), (step - 4, step - 4)):
                    x, y = px + dx, py + dy
                    pixels[x, y] = (*shade(pixels[x, y], 34), 255)
                    pixels[x + 1, y + 1] = (*shade(pixels[x + 1, y + 1], -24), 255)
    return image


def speckle(image, name, count, color, spread=12):
    rand = rng(name)
    pixels = image.load()
    size = image.size[0]
    for _ in range(count):
        x, y = rand.randrange(size), rand.randrange(size)
        pixels[x, y] = (*shade(color, rand.uniform(-spread, spread)), 255)
    return image


def octagon_mask(image, inner=0.0):
    """Cut a square texture to the regular octagon inscribed in it, optionally leaving a hole of `inner` apothem."""
    size = image.size[0]
    pixels = image.load()
    limit = math.sqrt(2.0)
    for y in range(size):
        for x in range(size):
            u = (x + 0.5) / size * 2.0 - 1.0
            v = (y + 0.5) / size * 2.0 - 1.0
            outside = abs(u) + abs(v) > limit or abs(u) > 1.0 or abs(v) > 1.0
            if inner > 0.0:
                outside |= max(abs(u), abs(v)) < inner and abs(u) + abs(v) < inner * limit
            if outside:
                pixels[x, y] = (0, 0, 0, 0)
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
GLINT = (206, 240, 250, 255)
BOLT = (150, 158, 168)
SOOT = (22, 20, 20)


def write_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    tread = noisy(64, SLATE_DARK, "plinth_top", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, "plinth_top")

    # Plinth side: the 1.5 px plinth maps rows 58-63, so the band sits in those rows only.
    side = noisy(64, SLATE_DARK, "plinth_side", amount=4)
    pixels = side.load()
    for x in range(64):
        pixels[x, 58] = (*shade(SLATE, 26), 255)
        pixels[x, 63] = (*shade(SLATE_DARK, -10), 255)
        for y in range(60, 62):
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)
    save(side, "plinth_side")

    # Vessel shell: heavy plate courses with staggered vertical welds, warmer where the bed burns behind the lining.
    vessel = Image.new("RGBA", (64, 64))
    pixels = vessel.load()
    rand = rng("vessel")
    for y in range(64):
        heat = max(0.0, 1.0 - abs(y - 30) / 14.0)
        base = mix((84, 90, 102), (108, 92, 80), heat * 0.6)
        for x in range(64):
            pixels[x, y] = (*shade(base, rand.uniform(-5, 5)), 255)
    for y in range(64):
        course = y // 12
        for x in range(64):
            if y % 12 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
            elif y % 12 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
            elif (x + course * 8) % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(vessel, "vessel")
    save(octagon_mask(noisy(32, (70, 76, 88), "vessel_end", amount=4)), "vessel_end")

    # Quench chamber: dark water-jacketed steel with vertical ribs.
    quench = noisy(64, (56, 64, 76), "quench", amount=4)
    pixels = quench.load()
    for y in range(64):
        for x in range(64):
            if x % 8 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif x % 8 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    save(quench, "quench")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")
    # Octagonal end faces, mapped whole onto their square, so each ring reads as a ring.
    save(octagon_mask(noisy(32, TEAL, "band_end", amount=6), inner=0.9), "band_end")
    save(octagon_mask(noisy(32, (124, 134, 146), "ring_end", amount=4), inner=0.88), "ring_end")
    save(octagon_mask(noisy(32, STEEL, "flange_end", amount=5), inner=0.7), "flange_end")

    save(noisy(64, STEEL, "flange", amount=5), "flange")
    save(noisy(64, BOLT, "bolt", amount=8), "bolt")

    # Domed head: darker steel with a bolt circle, cut to the octagon.
    head = noisy(32, (72, 80, 92), "head", amount=4)
    pixels = head.load()
    for angle in range(0, 360, 30):
        x = 16 + round(math.cos(math.radians(angle)) * 12)
        y = 16 + round(math.sin(math.radians(angle)) * 12)
        pixels[x, y] = (*shade(BOLT, 10), 255)
        pixels[x - 1, y] = (*shade(BOLT, -30), 255)
    save(octagon_mask(head), "head")

    # View-port liner: refractory brick behind the glass.
    liner = noisy(64, (40, 30, 26), "liner", amount=5)
    pixels = liner.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0 or (x + (y // 4) * 4) % 8 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -14), 255)
    save(liner, "liner")

    # Glass: transparent (cutout) with sparse diagonal glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for offset in (0, 32):
        draw.line([(offset + 20, 4), (offset + 26, -2)], fill=GLINT)
        draw.line([(offset + 22, 9), (offset + 25, 6)], fill=GLINT)
    draw.line([(30, 34), (36, 28)], fill=GLINT)
    draw.line([(31, 38), (34, 35)], fill=GLINT)
    save(glass, "glass")

    # Sight glass: clear between steel edge rails, with level ticks on one rail and a glint, so an empty or
    # half-full tank still reads as a glass tube. See SIGHT_UV for the two strips.
    sight = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = sight.load()
    for left, right in ((0, 15), (32, 51)):
        for y in range(64):
            for x in (left, left + 1, right - 1, right):
                edge = x in (left, right)
                pixels[x, y] = (*shade(STEEL, 14 if edge else -12), 255)
            if y % 8 == 4:
                pixels[left + 2, y] = (222, 226, 210, 255)
                pixels[left + 3, y] = (222, 226, 210, 255)
            elif y % 16 == 12:
                pixels[left + 2, y] = (222, 226, 210, 255)
        glint = left + (right - left) * 2 // 3
        for y in range(6, 58):
            if (y // 6) % 3 != 2:
                pixels[glint, y] = GLINT
    save(sight, "sight_glass")

    # Rotary valve: cast body with bolted flange ribs, and an end cover around the hub bearing.
    valve = noisy(64, (82, 90, 100), "valve", amount=5)
    pixels = valve.load()
    for y in range(64):
        for x in range(64):
            if x % 12 in (0, 1):
                pixels[x, y] = (*shade(pixels[x, y], -20 if x % 12 == 0 else 14), 255)
    save(valve, "valve")
    valve_end = noisy(16, (82, 90, 100), "valve_end", amount=5)
    draw = ImageDraw.Draw(valve_end)
    draw.rectangle([0, 0, 15, 15], outline=(*shade(SLATE_DARK, 4), 255))
    draw.ellipse([3, 3, 12, 12], fill=(*SLATE_DARK, 255))
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(*BOLT, 255))
    save(valve_end, "valve_end")

    # Hopper: worn steel sheet in welded courses.
    hopper = Image.new("RGBA", (64, 64))
    pixels = hopper.load()
    rand = rng("hopper")
    for y in range(64):
        for x in range(64):
            pixels[x, y] = (*shade((92, 98, 106), rand.uniform(-6, 6)), 255)
    for _ in range(40):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], -26), 255)
    for y in range(64):
        for x in range(64):
            if y % 8 == 7:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif x % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -12), 255)
    save(hopper, "hopper")
    # Hopper floor: a grate over the valve throat, seen while the hopper is empty.
    grate = noisy(64, (64, 70, 78), "grate", amount=4)
    pixels = grate.load()
    for y in range(64):
        for x in range(64):
            if x % 4 != 0 and y % 4 != 0:
                pixels[x, y] = (*shade(SOOT, (x * 7 + y * 3) % 5), 255)
    save(grate, "grate")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Power conduit: dark ribbed flex.
    conduit = noisy(64, (38, 42, 50), "conduit", amount=3)
    pixels = conduit.load()
    for y in range(64):
        for x in range(64):
            if x % 3 == 0 and y % 3 == 0 or (x + y) % 3 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -10), 255)
    save(conduit, "conduit")

    # Gear socket plate: dark recess with four mounting bolts per tile.
    socket = noisy(64, (26, 30, 37), "socket", amount=3)
    pixels = socket.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((2, 2), (12, 2), (2, 12), (12, 12)):
                pixels[px + dx, py + dy] = (*BOLT, 255)
                pixels[px + dx + 1, py + dy + 1] = (*shade(SLATE_DARK, -10), 255)
    save(socket, "socket")
    save(noisy(64, (184, 115, 51), "bus", amount=7, streak=4), "bus")

    # Soot: the bin floor and the chute mouth.
    save(noisy(64, SOOT, "soot", amount=4), "soot")

    # Residue bin: scuffed, ash-streaked dark steel.
    bin_ = noisy(64, (60, 62, 66), "bin", amount=5)
    speckle(bin_, "bin_ash", 160, (120, 116, 108), spread=10)
    save(bin_, "bin")

    # Control cabinet: slate panels with louvred vents.
    cabinet = noisy(64, SLATE, "cabinet", amount=4)
    pixels = cabinet.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (4, 5, 6, 7, 8, 9, 10, 11) and y % 16 in (3, 5, 7, 9, 11):
                pixels[x, y] = (*shade(SLATE_DARK, -10), 255)
            elif x % 16 in (4, 5, 6, 7, 8, 9, 10, 11) and y % 16 in (4, 6, 8, 10, 12):
                pixels[x, y] = (*shade(SLATE, 16), 255)
    panel_seams(cabinet, step=16, rivets=False)
    save(cabinet, "cabinet")

    # Coupling hub on the rotary valve.
    coupling = noisy(64, STEEL, "coupling", amount=5)
    pixels = coupling.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((4, 4), (11, 4), (4, 11), (11, 11)):
                pixels[px + dx, py + dy] = (*shade(BOLT, 20), 255)
    save(coupling, "coupling")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the port plate.
    port = noisy(64, SLATE, "port", amount=4)
    draw = ImageDraw.Draw(port)
    draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
    draw.rectangle([6, 6, 57, 57], outline=(*TEAL_DARK, 255), width=3)
    draw.rectangle([8, 8, 55, 55], outline=(*shade(TEAL, 10), 255), width=1)
    for x, y in ((3, 3), (58, 3), (3, 58), (58, 58)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(*BOLT, 255))
    draw.rectangle([20, 20, 43, 43], fill=(14, 18, 24, 255), outline=(*STEEL, 255))
    for x, y in ((24, 24), (37, 24), (24, 37), (37, 37)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(184, 115, 51, 255))
    save(port, "port")

    # Pressure gauge face: a scale from green through amber to a red line. The renderer draws the needle.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    for step in range(9):
        angle = 135 + step * 33.75
        color = (70, 150, 80) if step < 5 else (200, 160, 50) if step < 7 else (196, 52, 40)
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    gauge.putpixel((8, 8), (30, 30, 30, 255))
    save(gauge, "gauge")

    save(noisy(64, (52, 30, 14), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (255, 168, 70), "lamp_on", amount=6), "lamp_on")

    # Renderer textures. The bed glow is additive: black stays dark, embers glow orange to yellow.
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    rand = rng("glow")
    for y in range(16):
        for x in range(16):
            bed = y / 15.0
            ember = 0.5 + 0.5 * math.sin(x * 1.7 + y * 0.9) * math.sin(x * 0.6 - y * 1.3)
            heat = max(0.0, min(1.0, 0.25 + 0.55 * bed + 0.35 * ember + rand.uniform(-0.1, 0.1)))
            pixels[x, y] = (clamp(255 * min(1.0, heat * 1.3)), clamp(190 * heat ** 1.5), clamp(70 * heat ** 3), 255)
    save(glow, "fx/glow")

    # Coal Dust heap and residue heap, mapped by position like the housing.
    coal = noisy(64, (26, 26, 28), "coal", amount=5)
    speckle(coal, "coal_glints", 180, (70, 72, 80), spread=10)
    save(coal, "fx/coal")
    residue = noisy(64, (118, 112, 102), "residue", amount=10)
    speckle(residue, "residue_slag", 140, (62, 56, 54), spread=8)
    speckle(residue, "residue_clinker", 60, (150, 120, 92), spread=10)
    save(residue, "fx/residue")

    # Heat Core: greyscale finned element tinted by material.
    core = noisy(16, (215, 215, 215), "core", amount=6, streak=4)
    pixels = core.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -45), 255)
    save(core, "fx/core")

    servo = noisy(16, (210, 210, 210), "servo", amount=6)
    pixels = servo.load()
    for y in range(16):
        for x in range(16):
            if y in (3, 12):
                pixels[x, y] = (*shade(pixels[x, y], -70), 255)
            elif x in (0, 15) or y in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -40), 255)
    save(servo, "fx/servo")

    cell = noisy(16, (220, 220, 220), "cell", amount=5)
    pixels = cell.load()
    for y in range(16):
        for x in range(16):
            if y in (0, 1, 14, 15):
                pixels[x, y] = (*shade(pixels[x, y], -60), 255)
            elif x in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -30), 255)
    save(cell, "fx/cell")
    charge = noisy(16, (150, 236, 120), "charge", amount=8)
    pixels = charge.load()
    for y in range(0, 16, 4):
        for x in range(16):
            pixels[x, y] = (*shade(pixels[x, y], -90), 255)
    save(charge, "fx/charge")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
