#!/usr/bin/env python3
"""Generate the Steam Methane Reformer block model, item model, and owned textures.

The design follows a fired tubular reformer. A refractory-lined firebox stands over a
burner bay; through its front window a row of catalyst tubes runs from the burner floor
up to the inlet header on the roof. The installed Heat Core sits in the burner bay and
feeds the burners. A steam drum lies across the convection cabinet behind the firebox,
with a level glass and a pressure gauge on its front, and the flue stack rises at the
back corner. Feed gas enters through a framed sight glass and a control valve whose
actuator is the installed Servo; the Hydrogen and Carbon Monoxide sight glasses stand
along the west side on the outlet collector. The Battery Cell sits in a cradle at the
front east corner, and the cabinet's back carries the cable port.

The housing is a static block model; the tube glow and catalyst packing, the burner
flames, the firebox glow, the Heat Core, the four tank levels, the gauge needle, the
Servo, and the Battery Cell are drawn by SteamMethaneReformerRenderer, whose constants
match the geometry here. Run from the repository root:

    python tools/blockmodels/steam_methane_reformer.py
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
TEXTURES = ASSETS / "textures/block/steam_methane_reformer"
TEX = "rngtech:block/steam_methane_reformer/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))

# The front faces north (z = 0). West (x = 0) is on the viewer's right.
# Firebox shell over the burner bay, and the hollow behind its window back to the core wall.
FIREBOX_X0, FIREBOX_X1 = 3.5, 13.5
FIREBOX_Y0, FIREBOX_Y1 = 4.75, 12.5
FIREBOX_Z0, FIREBOX_Z1 = 1.0, 9.0
OPENING_X0, OPENING_X1 = 4.5, 12.5
OPENING_Y0, OPENING_Y1 = 5.25, 12.0
OPENING_Z = 5.0
# Window glass and its frame on the firebox front.
GLASS_X0, GLASS_X1 = 4.75, 12.25
GLASS_Y0, GLASS_Y1 = 5.5, 11.75
GLASS_Z = 0.75
# Catalyst tubes: square cages between collars at the floor and the roof.
TUBE_XS = (5.5, 7.5, 9.5, 11.5)
TUBE_Z, TUBE_HALF = 3.0, 0.75
TUBE_Y0, TUBE_Y1 = 5.25, 12.0
COLLAR = 0.5
# Burner nozzles on the firebox floor, in front of the gaps between tubes.
BURNER_XS = (6.5, 8.5, 10.5)
# Heat Core socket: the burner bay under the firebox.
BAY_Y1 = FIREBOX_Y0
HEAT_CORE = ((5.5, 2.0, 1.5), (11.5, 4.5, 4.5))
# Inlet header along the roof over the tubes.
HEADER = ((4.25, 12.5, 2.25), (12.75, 13.5, 3.75))
# Convection cabinet behind the firebox; its back carries the cable port.
CABINET = ((3.5, 2.0, 9.0), (13.5, 12.0, 15.0))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Steam drum lying along x on the cabinet.
DRUM_Y, DRUM_Z, DRUM_R = 14.0, 11.75, 1.875
DRUM_X0, DRUM_X1 = 4.25, 11.75
# Flue stack at the back east corner.
STACK = ((13.5, 2.0, 11.75), (15.75, 15.25, 14.0))
# Sight glasses: feed gas, Hydrogen, and Carbon Monoxide along the west side, water on the drum front.
SIGHT_X = 0.75
SIGHT_Y0, SIGHT_Y1 = 3.0, 11.0
FEED_Z = (1.25, 3.75)
HYDROGEN_Z = (5.25, 7.75)
MONOXIDE_Z = (9.5, 12.0)
WATER = ((8.25, 13.25, 8.25), (10.25, 15.25, 9.75))
# Feed control valve on the feed glass, with the Servo actuator on its yoke.
VALVE = ((1.0, 12.0, 1.75), (3.0, 13.375, 3.75))
SERVO = ((0.875, 13.625, 1.625), (3.125, 15.875, 3.875))
# Battery Cell cradle at the front east corner.
CELL = ((13.75, 2.5, 1.5), (15.75, 7.5, 4.0))
# Pressure gauge on the drum front.
DIAL = ((4.75, 12.75, 9.25), (7.25, 15.25, 9.875))
LAMP = ((14.0, 2.0, 0.25), (15.5, 2.75, 1.25))


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


def drum_octagon(radius, x0, x1, side, end=None):
    """An octagonal prism around the drum axis (along x), one face per side so no faces overlap.

    Two straight slabs give the four flat sides and two slabs turned 45 degrees the diagonals. The ends are one square
    whose texture is cut to the octagon.
    """
    half = radius * TAN
    origin = [r((x0 + x1) / 2), r(DRUM_Y), r(DRUM_Z)]
    narrow = ((x0, DRUM_Y - half, DRUM_Z - radius), (x1, DRUM_Y + half, DRUM_Z + radius))
    elements = [
        box((x0, DRUM_Y - radius, DRUM_Z - half), (x1, DRUM_Y + radius, DRUM_Z + half), side, faces=("up", "down")),
        box(*narrow, side, faces=("north", "south")),
        box(*narrow, side, faces=("north", "south"), rotation={"origin": origin, "axis": "x", "angle": 45}),
        box(*narrow, side, faces=("north", "south"), rotation={"origin": origin, "axis": "x", "angle": -45}),
    ]
    if end:
        elements.append(box((x0, DRUM_Y - radius, DRUM_Z - radius), (x1, DRUM_Y + radius, DRUM_Z + radius), end,
                            faces=("east", "west"), uv={"east": FULL, "west": FULL}))
    return elements


def sight_uv(height):
    """Sight-glass faces map a framed strip of the texture: the 2 px faces use columns 0-15, the others columns 32-51."""
    v = r(min(16.0, height * 2.0))
    return {
        "north": [0, 0, 4, v],
        "south": [0, 0, 4, v],
        "east": [8, 0, 13, v],
        "west": [8, 0, 13, v],
    }


def sight_glass(x0, z0, z1, y0=SIGHT_Y0, y1=SIGHT_Y1, cap_below=2.0, cap_above=12.0):
    """A framed sight glass along z between pipe caps, 2 px wide in x."""
    x1 = x0 + 2.0
    return [
        box((x0 - 0.25, cap_below, z0 - 0.25), (x1 + 0.25, y0, z1 + 0.25), "pipe"),
        box((x0 - 0.25, y1, z0 - 0.25), (x1 + 0.25, cap_above, z1 + 0.25), "pipe"),
        box((x0, y0, z0), (x1, y1, z1), "sight_glass", faces=SIDES, uv=sight_uv(y1 - y0)),
    ]


def tube_uv():
    width = r(TUBE_HALF * 2)
    height = r(TUBE_Y1 - TUBE_Y0 - 2 * COLLAR)
    return {face: [0, 0, width, height] for face in SIDES}


def firebox_elements():
    elements = []
    # Core wall behind the opening; its north face is the lit back of the firebox.
    elements.append(box((FIREBOX_X0, FIREBOX_Y0, OPENING_Z), (FIREBOX_X1, FIREBOX_Y1, FIREBOX_Z1), {"north": "liner", "*": "shell"}))
    # Side walls, floor, and roof around the opening, lined on the inside.
    elements.append(box((FIREBOX_X0, FIREBOX_Y0, FIREBOX_Z0), (OPENING_X0, FIREBOX_Y1, OPENING_Z), {"east": "liner", "*": "shell"}))
    elements.append(box((OPENING_X1, FIREBOX_Y0, FIREBOX_Z0), (FIREBOX_X1, FIREBOX_Y1, OPENING_Z), {"west": "liner", "*": "shell"}))
    elements.append(box((OPENING_X0, FIREBOX_Y0, FIREBOX_Z0), (OPENING_X1, OPENING_Y0, OPENING_Z), {"up": "liner", "*": "shell"},
                        faces=("north", "up", "down")))
    elements.append(box((OPENING_X0, OPENING_Y1, FIREBOX_Z0), (OPENING_X1, FIREBOX_Y1, OPENING_Z), {"down": "liner", "*": "shell"},
                        faces=("north", "up", "down")))

    # Window: glass in a bolted steel frame.
    elements.append(box((GLASS_X0, GLASS_Y0, GLASS_Z), (GLASS_X1, GLASS_Y1, GLASS_Z), "glass", faces=("north",)))
    frame_z = (FIREBOX_Z0 - 0.5, FIREBOX_Z0)
    for x0, x1 in ((GLASS_X0 - 0.75, GLASS_X0), (GLASS_X1, GLASS_X1 + 0.75)):
        elements.append(box((x0, FIREBOX_Y0, frame_z[0]), (x1, FIREBOX_Y1, frame_z[1]), "flange"))
    for y0, y1 in ((FIREBOX_Y0, GLASS_Y0), (GLASS_Y1, FIREBOX_Y1)):
        elements.append(box((GLASS_X0, y0, frame_z[0]), (GLASS_X1, y1, frame_z[1]), "flange"))
    for x in (GLASS_X0 - 0.375, GLASS_X1 + 0.375):
        for y in (FIREBOX_Y0 + 0.375, FIREBOX_Y1 - 0.375):
            elements.append(box((x - 0.25, y - 0.25, frame_z[0] - 0.2), (x + 0.25, y + 0.25, frame_z[0]), "bolt"))

    # Catalyst tubes: cages between collars. The renderer packs them with catalyst and makes them glow.
    for x in TUBE_XS:
        x0, x1 = x - TUBE_HALF, x + TUBE_HALF
        z0, z1 = TUBE_Z - TUBE_HALF, TUBE_Z + TUBE_HALF
        elements.append(box((x0, TUBE_Y0 + COLLAR, z0), (x1, TUBE_Y1 - COLLAR, z1), "tube", faces=SIDES, uv=tube_uv()))
        for y0, y1 in ((TUBE_Y0, TUBE_Y0 + COLLAR), (TUBE_Y1 - COLLAR, TUBE_Y1)):
            elements.append(box((x0 - 0.1, y0, z0 - 0.1), (x1 + 0.1, y1, z1 + 0.1), "flange", faces=("north", "east", "west", "up", "down")))
    # Burner nozzles on the floor.
    for x in BURNER_XS:
        elements.append(box((x - 0.4, OPENING_Y0, 1.6), (x + 0.4, OPENING_Y0 + 0.4, 2.4), "bolt"))

    # Buckstays: steel channels holding the shell on both sides.
    for z in (4.5, 8.25):
        elements.append(box((FIREBOX_X0 - 0.25, 2.0, z), (FIREBOX_X0, 10.75, z + 0.5), "flange", faces=("north", "south", "west", "up")))
        elements.append(box((FIREBOX_X1, 2.0, z), (FIREBOX_X1 + 0.25, FIREBOX_Y1, z + 0.5), "flange", faces=("north", "south", "east", "up")))

    # Burner bay: posts at the sides and a socket plate at the back; the renderer draws the installed Heat Core in it.
    elements.append(box((FIREBOX_X0, 2.0, FIREBOX_Z0), (OPENING_X0, BAY_Y1, OPENING_Z), "shell"))
    elements.append(box((OPENING_X1, 2.0, FIREBOX_Z0), (FIREBOX_X1, BAY_Y1, OPENING_Z), "shell"))
    elements.append(box((FIREBOX_X0, 2.0, OPENING_Z), (FIREBOX_X1, BAY_Y1, FIREBOX_Z1), {"north": "socket", "*": "shell"}))

    # Inlet header on the roof, with a collar over each tube.
    elements.append(box(*HEADER, "pipe"))
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HEADER
    for x in TUBE_XS:
        elements.append(box((x - 0.5, hy0, hz0 - 0.25), (x + 0.5, hy1 + 0.25, hz1 + 0.25), "flange", faces=("north", "south", "east", "west", "up")))
    return elements


def drum_elements():
    elements = []
    # Saddles on the cabinet roof, the drum shell, and its two heads.
    for x0 in (5.25, 9.75):
        elements.append(box((x0, 12.0, DRUM_Z - 1.5), (x0 + 1.0, 12.75, DRUM_Z + 1.5), "flange"))
    elements += drum_octagon(DRUM_R, DRUM_X0, DRUM_X1, "drum")
    elements += drum_octagon(DRUM_R - 0.25, DRUM_X0 - 0.5, DRUM_X0, "drum", end="drum_end")
    elements += drum_octagon(DRUM_R - 0.25, DRUM_X1, DRUM_X1 + 0.5, "drum", end="drum_end")

    # Water level glass on the drum front.
    (wx0, wy0, wz0), (wx1, wy1, wz1) = WATER
    elements.append(box((wx0 - 0.25, FIREBOX_Y1, wz0 - 0.25), (wx1 + 0.25, wy0, wz1 + 0.25), "pipe"))
    elements.append(box((wx0 - 0.25, wy1, wz0 - 0.25), (wx1 + 0.25, 16.0, wz1 + 0.25), "pipe"))
    elements.append(box((wx0, wy0, wz0), (wx1, wy1, wz1), "sight_glass", faces=SIDES, uv=sight_uv(wy1 - wy0)))
    for y in (wy0 - 0.5, wy1 + 0.25):
        elements.append(box((wx0 + 0.5, y - 0.25, wz1 + 0.25), (wx1 - 0.5, y + 0.25, DRUM_Z - DRUM_R + 0.25), "pipe"))

    # Pressure gauge on the drum front.
    (dx0, dy0, dz0), (dx1, dy1, dz1) = DIAL
    elements.append(box((dx0, dy0, dz0), (dx1, dy1, dz1), {"north": "gauge", "*": "casing"}, uv={"north": FULL}))

    # Steam line from the drum down to the inlet header.
    hz1 = HEADER[1][2]
    elements.append(box((11.25, FIREBOX_Y1, hz1), (12.0, FIREBOX_Y1 + 0.75, DRUM_Z - DRUM_R + 0.25), "pipe", faces=("east", "west", "up", "south")))
    return elements


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))
    elements += firebox_elements()

    # Convection cabinet with a teal band and louvres, and the cable port on its back.
    elements.append(box(*CABINET, {"up": "casing", "*": "cabinet"}, faces=("south", "east", "west", "up")))
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CABINET
    elements.append(box((cx0 - 0.25, 9.0, cz0), (cx1 + 0.25, 9.75, cz1 + 0.25), "band", faces=("south", "east", "west")))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))
    elements += drum_elements()

    # Flue stack on a base flange, with a collar where the breeching joins and a lip around a sooty mouth.
    (sx0, sy0, sz0), (sx1, sy1, sz1) = STACK
    elements.append(box((sx0, sy0, sz0), (sx1, sy1, sz1), {"up": "soot", "*": "stack"}, faces=("north", "south", "east", "west", "up")))
    elements.append(box((sx0 - 0.25, 2.0, sz0 - 0.25), (16.0, 3.0, sz1 + 0.25), "flange", faces=("north", "south", "east", "up")))
    elements.append(box((cx1, 8.5, sz0 + 0.25), (sx0, 10.25, sz1 - 0.25), "flange", faces=("north", "south", "up", "down")))
    lip = 0.25
    for start, end in (
            ((sx0 - lip, sy1 - 0.75, sz0 - lip), (sx1 + lip, 16.0, sz0)),
            ((sx0 - lip, sy1 - 0.75, sz1), (sx1 + lip, 16.0, sz1 + lip)),
            ((sx0 - lip, sy1 - 0.75, sz0), (sx0, 16.0, sz1)),
            ((sx1, sy1 - 0.75, sz0), (sx1 + lip, 16.0, sz1)),
    ):
        elements.append(box(start, end, "flange"))

    # Sight glasses along the west side: feed gas at the front, then Hydrogen and Carbon Monoxide on the outlet collector.
    for z0, z1 in (FEED_Z, HYDROGEN_Z, MONOXIDE_Z):
        elements += sight_glass(SIGHT_X, z0, z1)
    elements.append(box((SIGHT_X + 2.0, 11.0, HYDROGEN_Z[0] - 0.25), (FIREBOX_X0, 11.75, MONOXIDE_Z[1] + 0.25), "pipe",
                        faces=("north", "south", "west", "up", "down")))

    # Feed control valve on the feed glass, a yoke plate for the Servo, and the line into the inlet header.
    (vx0, vy0, vz0), (vx1, vy1, vz1) = VALVE
    elements.append(box((vx0, vy0, vz0), (vx1, vy1, vz1), "flange"))
    (sx0, sy0, sz0), (sx1, sy1, sz1) = SERVO
    elements.append(box((sx0 - 0.125, vy1, sz0 - 0.125), (sx1 + 0.125, sy0, sz1 + 0.125), {"up": "socket", "*": "casing"}))
    hx0 = HEADER[0][0]
    elements.append(box((vx1, 12.6, 2.6), (hx0, 13.4, 3.4), "pipe", faces=("north", "south", "up", "down")))

    # Battery Cell cradle: a base, a back plate against the firebox, and a clamp bar with the cell terminal.
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CELL
    elements.append(box((FIREBOX_X1, 2.0, cz0 - 0.25), (16.0, cy0, cz1 + 0.25), "flange"))
    elements.append(box((FIREBOX_X1, cy0, cz0 - 0.25), (cx0, cy1 + 0.75, cz1 + 0.25), {"east": "socket", "*": "casing"}))
    elements.append(box((cx0, cy1 + 0.25, 2.4), (cx1 + 0.25, cy1 + 0.75, 3.1), "bus"))

    # Status lamp on the plinth.
    elements.append(box(*LAMP, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}))
    return elements


HOUSING_TEXTURES = {
    "particle": TEX + "shell",
    "casing": TEX + "casing",
    "shell": TEX + "shell",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "liner": TEX + "liner",
    "glass": TEX + "glass",
    "tube": TEX + "tube",
    "flange": TEX + "flange",
    "bolt": TEX + "bolt",
    "socket": TEX + "socket",
    "pipe": TEX + "pipe",
    "cabinet": TEX + "cabinet",
    "band": TEX + "band",
    "port": TEX + "port",
    "drum": TEX + "drum",
    "drum_end": TEX + "drum_end",
    "sight_glass": TEX + "sight_glass",
    "gauge": TEX + "gauge",
    "stack": TEX + "stack",
    "soot": TEX + "soot",
    "bus": TEX + "bus",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/steam_methane_reformer.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/steam_methane_reformer_active.json", {
        "parent": "rngtech:block/steam_methane_reformer",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    # The item model has no renderer and a fresh item has no Gear, so the housing alone is the whole machine.
    write_json(MODELS / "item/steam_methane_reformer.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"steam_methane_reformer:{name}")


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
ALLOY = (132, 118, 112)


def write_housing_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    # Firebox shell: slate plate on a 4 px grid of welded stiffeners, a little darker than the casing.
    shell = noisy(64, shade(SLATE, -4), "shell", amount=4)
    pixels = shell.load()
    for y in range(64):
        for x in range(64):
            if x % 16 == 0 or y % 32 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -6), 255)
            elif x % 16 == 1 or y % 32 == 1:
                pixels[x, y] = (*shade(SLATE, 14), 255)
    for y in range(4, 64, 8):
        for x in range(0, 64, 16):
            pixels[x + 3, y] = (*shade(BOLT, -20), 255)
            pixels[x + 12, y] = (*shade(BOLT, -20), 255)
    save(shell, "shell")

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

    # Refractory lining: firebrick in staggered courses, sooted toward the roof.
    liner = Image.new("RGBA", (64, 64))
    pixels = liner.load()
    rand = rng("liner")
    for y in range(64):
        course = y // 4
        soot = (63 - y) / 63.0
        for x in range(64):
            offset = 4 if course % 2 else 0
            brick = (x + offset) // 8
            tone = rng(f"liner:{course}:{brick}").uniform(-10, 10)
            base = mix((150, 94, 64), (70, 48, 40), soot * 0.6)
            color = shade(base, tone + rand.uniform(-5, 5))
            if y % 4 == 3 or (x + offset) % 8 == 7:
                color = shade(base, -42)
            pixels[x, y] = (*color, 255)
    save(liner, "liner")

    # Window glass: transparent (cutout) with sparse diagonal glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for ox, oy in ((0, 0), (32, 32)):
        draw.line([(ox + 23, oy + 28), (ox + 28, oy + 23)], fill=GLINT)
    save(glass, "glass")

    # Catalyst tube: heat-stained alloy with a slot down the middle of each face between rings every 2 px, so the
    # packing shows through. Faces map columns 0-5.
    tube = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = tube.load()
    rand = rng("tube")
    for y in range(64):
        for x in range(6):
            slot = x in (2, 3) and y % 8 not in (0, 1)
            if not slot:
                tone = (14, 6, -4, -4, -10, -18)[x] + (8 if y % 8 == 0 else 0)
                pixels[x, y] = (*shade(ALLOY, tone + rand.uniform(-4, 4)), 255)
    save(tube, "tube")

    save(noisy(64, STEEL, "flange", amount=5), "flange")
    save(noisy(64, BOLT, "bolt", amount=8), "bolt")

    # Gear socket plate: dark recess with four mounting bolts per tile.
    socket = noisy(64, (26, 30, 37), "socket", amount=3)
    pixels = socket.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((2, 2), (12, 2), (2, 12), (12, 12)):
                pixels[px + dx, py + dy] = (*BOLT, 255)
                pixels[px + dx + 1, py + dy + 1] = (*shade(SLATE_DARK, -10), 255)
    save(socket, "socket")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Convection cabinet: dark louvred panels.
    cabinet = noisy(64, (40, 47, 57), "cabinet", amount=4)
    pixels = cabinet.load()
    for y in range(64):
        for x in range(64):
            if x % 32 in (0, 1):
                pixels[x, y] = (*shade(SLATE_DARK, -8 if x % 32 == 0 else 6), 255)
            elif y % 6 == 0:
                pixels[x, y] = (*shade(SLATE, 22), 255)
            elif y % 6 in (1, 2):
                pixels[x, y] = (*shade(SLATE_DARK, -14), 255)
    save(cabinet, "cabinet")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the cabinet back.
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

    # Steam drum: light steel with girth welds every 8 px, and dished heads with a manway.
    drum = noisy(64, (112, 120, 132), "drum", amount=4, streak=3)
    pixels = drum.load()
    for y in range(64):
        for x in range(64):
            if x % 32 == 6:
                pixels[x, y] = (*shade(STEEL, -26), 255)
            elif x % 32 == 7:
                pixels[x, y] = (*shade(STEEL, 22), 255)
    save(drum, "drum")
    head = noisy(32, (118, 126, 138), "drum_end", amount=4)
    pixels = head.load()
    for y in range(32):
        for x in range(32):
            distance = math.hypot(x - 15.5, y - 15.5)
            pixels[x, y] = (*shade(pixels[x, y], 18 - distance * 1.6), 255)
    draw = ImageDraw.Draw(head)
    draw.ellipse([10, 10, 21, 21], outline=(*shade(STEEL, -30), 255))
    draw.ellipse([11, 11, 20, 20], fill=(*shade(STEEL, 8), 255))
    for x, y in ((15, 9), (15, 22), (9, 15), (22, 15)):
        draw.point((x, y), fill=(*BOLT, 255))
    save(octagon_mask(head), "drum_end")

    # Sight glass: clear between steel edge rails, with level ticks on one rail and a glint, so an empty or
    # half-full tank still reads as a glass tube. See sight_uv for the two strips.
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

    # Flue stack: heat-blued steel, darker toward the mouth, and a sooty mouth.
    stack = Image.new("RGBA", (64, 64))
    pixels = stack.load()
    rand = rng("stack")
    for y in range(64):
        base = mix((70, 66, 80), (104, 104, 116), y / 63.0)
        for x in range(64):
            pixels[x, y] = (*shade(base, rand.uniform(-5, 5)), 255)
    for y in range(0, 64, 16):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], -20), 255)
    save(stack, "stack")
    soot = noisy(64, (18, 16, 16), "soot", amount=4)
    draw = ImageDraw.Draw(soot)
    for offset in (0, 32):
        for ring in range(3):
            draw.rectangle([offset + 2 + ring * 4, 2 + ring * 4, offset + 29 - ring * 4, 29 - ring * 4],
                           outline=(*shade((18, 16, 16), 6 - ring * 3), 255))
    save(soot, "soot")

    # Bus bar from the Battery Cell terminal: copper.
    save(noisy(64, (184, 115, 51), "bus", amount=7, streak=4), "bus")

    save(noisy(64, (52, 30, 14), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (255, 168, 70), "lamp_on", amount=6), "lamp_on")


def write_renderer_textures() -> None:
    """Greyscale glow textures tinted in code, and Gear bodies tinted by material."""
    flame = Image.new("RGBA", (16, 16))
    pixels = flame.load()
    rand = rng("flame")
    for y in range(16):
        for x in range(16):
            core = max(0.0, 1.0 - abs(x - 7.5) / (2.0 + 6.0 * y / 15.0))
            strength = max(0.0, min(1.0, core * (0.35 + 0.65 * y / 15.0) * 1.2 + rand.uniform(-0.08, 0.08)))
            value = clamp(255 * strength)
            pixels[x, y] = (value, value, value, 255)
    save(flame, "fx/flame")

    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot((x + 0.5 - 8) / 8, (y + 0.5 - 11) / 11))
            value = clamp(255 * (1.0 - distance) ** 1.2)
            pixels[x, y] = (value, value, value, 255)
    save(glow, "fx/glow")

    # Tube glow: hottest a third of the way up, the rails a little brighter than the packing between them.
    tube_glow = Image.new("RGBA", (16, 16))
    pixels = tube_glow.load()
    rand = rng("tube_glow")
    for y in range(16):
        height = 1.0 - (y + 0.5) / 16
        along = 0.55 + 0.45 * math.exp(-((height - 0.35) / 0.35) ** 2)
        for x in range(16):
            across = 1.0 if x in (0, 1, 14, 15) else 0.8
            value = clamp(255 * along * across + rand.uniform(-10, 10))
            pixels[x, y] = (value, value, value, 255)
    save(tube_glow, "fx/tube_glow")

    # Catalyst packing: pale ceramic pellets with dark gaps, seen through the tube cage.
    catalyst = noisy(16, (54, 50, 48), "catalyst", amount=3)
    pixels = catalyst.load()
    rand = rng("catalyst_pellets")
    for row in range(4):
        for col in range(4):
            cx = col * 4 + (2 if row % 2 else 0) + rand.randint(0, 1)
            cy = row * 4 + rand.randint(0, 1)
            tone = rand.uniform(-14, 10)
            for dx, dy, lift in ((0, 0, 18), (1, 0, 4), (0, 1, 0), (1, 1, -16), (2, 1, -24)):
                x, y = (cx + dx) % 16, (cy + dy) % 16
                pixels[x, y] = (*shade((196, 190, 178), tone + lift), 255)
    save(catalyst, "fx/catalyst")

    # Heat Core fins: brushed light metal, tinted by material.
    core = noisy(16, (226, 226, 226), "heat_core", amount=6, streak=6)
    pixels = core.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -50), 255)
    save(core, "fx/heat_core")

    servo = noisy(16, (210, 210, 210), "servo", amount=6)
    pixels = servo.load()
    for y in range(16):
        for x in range(16):
            if y in (3, 12):
                pixels[x, y] = (*shade(pixels[x, y], -70), 255)
            elif x in (0, 15) or y in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -40), 255)
    save(servo, "fx/servo")
    horn = Image.new("RGBA", (16, 16), (*BOLT, 255))
    draw = ImageDraw.Draw(horn)
    draw.rectangle([0, 0, 15, 15], outline=(*shade(BOLT, -40), 255))
    draw.rectangle([6, 6, 9, 9], fill=(*SLATE_DARK, 255))
    save(horn, "fx/horn")

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
    write_housing_textures()
    write_renderer_textures()


if __name__ == "__main__":
    main()
