#!/usr/bin/env python3
"""Generate the Syngas Combustor block model, item model, and owned textures.

The design follows a small gas-turbine generator set. A framed sight glass holds the
fuel gas, which feeds an intake manifold ring around a vertical combustion can. The
can's hot gas turns a turbine whose shaft drives an alternator through an open
coupling; the alternator's terminal box on the back is the cable port. Exhaust leaves
through a stack on top, and a bleed line fills the Carbon Exhaust sight glass. The
installed Servo is the fuel governor on the turbine, and the Battery Cell sits in a
cradle beside the alternator.

The housing is a static block model; the flame behind the view ports, the stack
glow, the shaft and coupling, both tank levels, the tachometer needle, the Servo, and
the Battery Cell are drawn by SyngasCombustorRenderer, whose constants match the
geometry here. Run from the repository root:

    python tools/blockmodels/syngas_combustor.py
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
TEXTURES = ASSETS / "textures/block/syngas_combustor"
TEX = "rngtech:block/syngas_combustor/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))

# The front faces north (z = 0). West (x = 0) is on the viewer's right.
# Combustion can: a vertical octagon around (CAN_X, CAN_Z).
CAN_X, CAN_Z, CAN_R = 8.0, 4.5, 3.25
CAN_Y0, CAN_Y1 = 2.0, 12.0
# View ports on the can's front face, two windows one above the other.
PORT_X0, PORT_X1 = 7.0, 9.0
WINDOWS = ((5.5, 7.25), (7.75, 9.5))
# Shaft axis, front to back, shared by the turbine, coupling, and alternator.
AXIS_X, AXIS_Y = 8.0, 8.0
TURBINE_R, TURBINE_Z0, TURBINE_Z1 = 2.75, 7.5, 10.5
ALTERNATOR_R, ALTERNATOR_Z0, ALTERNATOR_Z1 = 4.0, 12.0, 15.0
# Exhaust stack on top of the turbine.
STACK = ((6.5, 10.5, 7.75), (9.5, 15.25, 10.5))
# Sight glasses: fuel gas at the front west, Carbon Exhaust at the east beside the stack.
FUEL_X, FUEL_Z0, FUEL_Z1 = 0.75, 1.5, 4.0
EXHAUST_X, EXHAUST_Z0, EXHAUST_Z1 = 13.25, 8.0, 10.5
SIGHT_Y0, SIGHT_Y1 = 3.5, 11.5
# Servo mount on the turbine's west side, Battery Cell cradle on the alternator's east side.
SERVO = ((2.25, 5.5, 8.0), (4.75, 8.5, 10.0))
CELL = ((12.75, 2.5, 12.0), (15.25, 7.5, 14.5))
# Cable port on the alternator's terminal box, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
DIAL = ((12.75, 2.0, 0.25), (15.25, 4.5, 1.25))


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


def vertical_octagon(cx, cz, radius, y0, y1, side, end=None):
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


def axial_octagon(radius, z0, z1, side, end=None):
    """An octagonal prism around the shaft axis, built like vertical_octagon."""
    half = radius * TAN
    origin = [r(AXIS_X), r(AXIS_Y), r((z0 + z1) / 2)]
    narrow = ((AXIS_X - half, AXIS_Y - radius, z0), (AXIS_X + half, AXIS_Y + radius, z1))
    elements = [
        box((AXIS_X - radius, AXIS_Y - half, z0), (AXIS_X + radius, AXIS_Y + half, z1), side, faces=("east", "west")),
        box(*narrow, side, faces=("up", "down")),
        box(*narrow, side, faces=("up", "down"), rotation={"origin": origin, "axis": "z", "angle": 45}),
        box(*narrow, side, faces=("up", "down"), rotation={"origin": origin, "axis": "z", "angle": -45}),
    ]
    if end:
        elements.append(box((AXIS_X - radius, AXIS_Y - radius, z0), (AXIS_X + radius, AXIS_Y + radius, z1), end,
                            faces=("north", "south"), uv={"north": FULL, "south": FULL}))
    return elements


# Sight-glass faces map a framed strip of the texture: the narrow (2 px) north and south faces use columns 0-15, the
# wider (2.5 px) east and west faces use columns 32-51, matching the Photobioreactor's sight glasses.
SIGHT_UV = {
    "north": [0, 0, 4, 16],
    "south": [0, 0, 4, 16],
    "east": [8, 0, 13, 16],
    "west": [8, 0, 13, 16],
}


def sight_glass(x0, z0, z1):
    x1 = x0 + 2.0
    return [
        box((x0 - 0.25, 2.0, z0 - 0.25), (x1 + 0.25, SIGHT_Y0, z1 + 0.25), "pipe"),
        box((x0 - 0.25, SIGHT_Y1, z0 - 0.25), (x1 + 0.25, 13.0, z1 + 0.25), "pipe"),
        box((x0, SIGHT_Y0, z0), (x1, SIGHT_Y1, z1), "sight_glass", faces=SIDES, uv=SIGHT_UV),
    ]


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))

    # Combustion can with a teal cooling-jacket band, the intake manifold ring, and a bolted top cap with an igniter.
    elements += vertical_octagon(CAN_X, CAN_Z, CAN_R, CAN_Y0, CAN_Y1, "chamber")
    elements += vertical_octagon(CAN_X, CAN_Z, CAN_R + 0.25, 3.25, 4.0, "band", end="band_end")
    elements += vertical_octagon(CAN_X, CAN_Z, CAN_R + 0.5, 10.75, 11.75, "pipe", end="manifold_end")
    for angle in (45, 135, 225, 315):
        bx = CAN_X + math.cos(math.radians(angle)) * (CAN_R + 0.6)
        bz = CAN_Z + math.sin(math.radians(angle)) * (CAN_R + 0.6)
        elements.append(box((bx - 0.4, 11.75, bz - 0.4), (bx + 0.4, 12.35, bz + 0.4), "bolt"))
    elements += vertical_octagon(CAN_X, CAN_Z, CAN_R + 0.25, CAN_Y1, 12.75, "flange", end="cap")
    elements.append(box((CAN_X - 0.5, 12.75, CAN_Z - 0.5), (CAN_X + 0.5, 13.5, CAN_Z + 0.5), "bolt"))

    # View ports: a dark liner behind two glass windows in a steel frame. The flame glow sits between liner and glass.
    front = CAN_Z - CAN_R
    elements.append(box((PORT_X0, WINDOWS[0][0], front - 0.05), (PORT_X1, WINDOWS[-1][1], front), "liner", faces=("north",)))
    for y0, y1 in WINDOWS:
        elements.append(box((PORT_X0, y0, front - 0.3), (PORT_X1, y1, front - 0.3), "glass", faces=("north",)))
    frame_z = (front - 0.5, front)
    for x0, x1 in ((PORT_X0 - 0.5, PORT_X0), (PORT_X1, PORT_X1 + 0.5)):
        elements.append(box((x0, WINDOWS[0][0] - 0.5, frame_z[0]), (x1, WINDOWS[-1][1] + 0.5, frame_z[1]), "flange"))
    for y0, y1 in ((WINDOWS[0][0] - 0.5, WINDOWS[0][0]), (WINDOWS[0][1], WINDOWS[1][0]), (WINDOWS[1][1], WINDOWS[1][1] + 0.5)):
        elements.append(box((PORT_X0, y0, frame_z[0]), (PORT_X1, y1, frame_z[1]), "flange"))

    # Turbine on the shaft axis, flanged at both ends and standing on a foot.
    elements += axial_octagon(TURBINE_R, TURBINE_Z0, TURBINE_Z1, "turbine")
    elements += axial_octagon(TURBINE_R + 0.35, TURBINE_Z0, TURBINE_Z0 + 0.5, "flange", end="flange_end")
    elements += axial_octagon(TURBINE_R + 0.35, TURBINE_Z1 - 0.5, TURBINE_Z1, "flange", end="turbine_end")
    elements.append(box((5.75, 2.0, 8.0), (10.25, 5.5, 10.0), "casing"))

    # Exhaust stack: a sooty mouth inside a lip.
    (sx0, sy0, sz0), (sx1, sy1, sz1) = STACK
    elements.append(box((sx0, sy0, sz0), (sx1, sy1, sz1), {"up": "soot", "*": "stack"}, faces=("north", "south", "east", "west", "up")))
    lip = 0.5
    for start, end in (
            ((sx0 - lip, sy1 - 0.75, sz0 - lip), (sx1 + lip, 16.0, sz0)),
            ((sx0 - lip, sy1 - 0.75, sz1), (sx1 + lip, 16.0, sz1 + lip)),
            ((sx0 - lip, sy1 - 0.75, sz0), (sx0, 16.0, sz1)),
            ((sx1, sy1 - 0.75, sz0), (sx1 + lip, 16.0, sz1)),
    ):
        elements.append(box(start, end, "flange"))

    # Alternator with a teal stator band, cooling ribs, and the terminal box carrying the cable port.
    elements += axial_octagon(ALTERNATOR_R, ALTERNATOR_Z0, ALTERNATOR_Z1, "alternator", end="endbell")
    elements += axial_octagon(ALTERNATOR_R + 0.25, 13.25, 13.75, "band", end="band_end")
    elements.append(box((4.5, 2.0, 12.25), (11.5, 4.5, 14.75), "casing"))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))

    # Fuel sight glass and the intake line into the manifold ring.
    elements += sight_glass(FUEL_X, FUEL_Z0, FUEL_Z1)
    elements.append(box((FUEL_X + 2.25, 11.0, 2.75), (CAN_X - CAN_R, 11.75, 3.5), "pipe", faces=("north", "south", "up", "down")))

    # Carbon Exhaust sight glass, fed by a bleed line from the stack.
    elements += sight_glass(EXHAUST_X, EXHAUST_Z0, EXHAUST_Z1)
    elements.append(box((sx1, 12.0, 8.75), (EXHAUST_X - 0.25, 12.75, 9.75), "pipe", faces=("north", "south", "up", "down")))

    # Servo mount: a bracket bolted to the turbine with a socket plate; the renderer draws the installed Servo on it.
    (vx0, vy0, vz0), (vx1, vy1, vz1) = SERVO
    elements.append(box((vx1, vy0 - 0.5, vz0 - 0.25), (AXIS_X - TURBINE_R + 0.1, vy1 + 0.5, vz1 + 0.25), {"west": "socket", "*": "casing"}))
    elements.append(box((vx0 - 0.25, vy0 - 0.75, vz0 - 0.25), (vx1, vy0, vz1 + 0.25), "casing"))
    elements.append(box((vx0 - 0.25, 2.0, vz0 + 0.5), (vx0 + 0.75, vy0 - 0.75, vz1 - 0.5), "casing"))

    # Battery Cell cradle: a base, a back plate against the alternator, and a clamp bar with the cell terminal.
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CELL
    elements.append(box((cx0 - 0.25, 2.0, cz0 - 0.25), (cx1 + 0.25, cy0, cz1 + 0.25), "flange"))
    elements.append(box((12.0, 2.0, cz0 - 0.25), (cx0, cy1 + 1.0, cz1 + 0.25), {"east": "socket", "*": "casing"}))
    elements.append(box((cx0, cy1 + 0.25, 12.9), (cx1 + 0.25, cy1 + 1.0, 13.6), "bus"))

    # Tachometer and status lamp on the plinth front.
    (dx0, dy0, dz0), (dx1, dy1, dz1) = DIAL
    elements.append(box((dx0, dy0, dz0), (dx1, dy1, dz1), {"north": "gauge", "*": "casing"}, uv={"north": FULL}))
    elements.append(box(
        (3.25, 2.0, 0.5), (4.75, 3.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def item_extra_elements():
    """Static shaft and coupling for the item model, which has no renderer. A fresh item has no Gear installed."""
    elements = []
    elements.append(box((AXIS_X - 0.6, AXIS_Y - 0.6, TURBINE_Z1), (AXIS_X + 0.6, AXIS_Y + 0.6, ALTERNATOR_Z0), "flange"))
    elements.append(box((AXIS_X - 1.9, AXIS_Y - 1.9, 10.9), (AXIS_X + 1.9, AXIS_Y + 1.9, 11.6), "coupling"))
    return elements


HOUSING_TEXTURES = {
    "particle": TEX + "chamber",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "chamber": TEX + "chamber",
    "liner": TEX + "liner",
    "band": TEX + "band",
    "band_end": TEX + "band_end",
    "manifold_end": TEX + "manifold_end",
    "flange": TEX + "flange",
    "flange_end": TEX + "flange_end",
    "cap": TEX + "cap",
    "bolt": TEX + "bolt",
    "glass": TEX + "glass",
    "sight_glass": TEX + "sight_glass",
    "turbine": TEX + "turbine",
    "turbine_end": TEX + "turbine_end",
    "stack": TEX + "stack",
    "soot": TEX + "soot",
    "alternator": TEX + "alternator",
    "endbell": TEX + "endbell",
    "pipe": TEX + "pipe",
    "socket": TEX + "socket",
    "bus": TEX + "bus",
    "port": TEX + "port",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/syngas_combustor.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/syngas_combustor_active.json", {
        "parent": "rngtech:block/syngas_combustor",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/syngas_combustor.json", {
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
    return random.Random(f"syngas_combustor:{name}")


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

    # Combustion can: heat-stained steel, bronze near the burner and blued toward the top, with weld seams.
    chamber = Image.new("RGBA", (64, 64))
    pixels = chamber.load()
    rand = rng("chamber")
    for y in range(64):
        heat = y / 63.0
        base = mix((86, 92, 112), (112, 98, 84), heat)
        for x in range(64):
            pixels[x, y] = (*shade(base, rand.uniform(-5, 5)), 255)
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
            elif y % 16 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
    save(chamber, "chamber")

    # View-port liner: a dark, sooty firebox wall behind the glass.
    save(noisy(64, (24, 20, 20), "liner", amount=5), "liner")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")
    # Octagonal end faces, mapped whole onto their square, so each band reads as a ring.
    save(octagon_mask(noisy(32, TEAL, "band_end", amount=6), inner=0.9), "band_end")
    save(octagon_mask(noisy(32, (124, 134, 146), "manifold_end", amount=4), inner=0.85), "manifold_end")
    save(octagon_mask(noisy(32, STEEL, "flange_end", amount=5), inner=0.8), "flange_end")

    flange = noisy(64, STEEL, "flange", amount=5)
    save(flange, "flange")
    save(noisy(64, BOLT, "bolt", amount=8), "bolt")

    # Can cap: dark steel with a bolt circle, cut to the octagon.
    cap = noisy(32, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for angle in range(0, 360, 30):
        x = 16 + round(math.cos(math.radians(angle)) * 11)
        y = 16 + round(math.sin(math.radians(angle)) * 11)
        pixels[x, y] = (*shade(BOLT, 10), 255)
        pixels[x - 1, y] = (*shade(BOLT, -30), 255)
    save(octagon_mask(cap), "cap")

    # Glass: transparent (cutout) with sparse diagonal glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for offset in (0, 32):
        draw.line([(offset + 18, 14), (offset + 26, 6)], fill=GLINT)
        draw.line([(offset + 20, 19), (offset + 23, 16)], fill=GLINT)
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

    # Turbine casing: brushed steel with stage rings around the axis.
    turbine = noisy(64, (88, 98, 110), "turbine", amount=4, streak=4)
    pixels = turbine.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (5, 11):
                pixels[x, y] = (*shade(pixels[x, y], -20), 255)
            elif x % 16 in (6, 12):
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
    save(turbine, "turbine")
    # Turbine outlet end: a flanged ring around the shaft bearing.
    turbine_end = noisy(32, STEEL, "turbine_end", amount=5)
    draw = ImageDraw.Draw(turbine_end)
    draw.ellipse([10, 10, 21, 21], fill=(*shade(SLATE_DARK, -6), 255))
    draw.ellipse([13, 13, 18, 18], fill=(*BOLT, 255))
    save(octagon_mask(turbine_end), "turbine_end")

    # Exhaust stack: heat-blued steel, darker toward the mouth, and a sooty mouth.
    stack = Image.new("RGBA", (64, 64))
    pixels = stack.load()
    rand = rng("stack")
    for y in range(64):
        base = mix((70, 66, 80), (104, 104, 116), y / 63.0)
        for x in range(64):
            pixels[x, y] = (*shade(base, rand.uniform(-5, 5)), 255)
    save(stack, "stack")
    soot = noisy(64, (18, 16, 16), "soot", amount=4)
    draw = ImageDraw.Draw(soot)
    for offset in (0, 32):
        for ring in range(3):
            draw.rectangle([offset + 2 + ring * 4, 2 + ring * 4, offset + 29 - ring * 4, 29 - ring * 4],
                           outline=(*shade((18, 16, 16), 6 - ring * 3), 255))
    save(soot, "soot")

    # Alternator: dark frame with horizontal cooling ribs, and an end bell with vent slots around the bearing.
    alternator = noisy(64, (40, 47, 57), "alternator", amount=4)
    pixels = alternator.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(SLATE, 18), 255)
    save(alternator, "alternator")
    endbell = noisy(32, SLATE, "endbell", amount=4)
    pixels = endbell.load()
    for y in range(32):
        for x in range(32):
            angle = math.degrees(math.atan2(y - 15.5, x - 15.5)) % 45
            distance = math.hypot(x - 15.5, y - 15.5)
            if 7 < distance < 12 and 10 < angle < 35:
                pixels[x, y] = (*shade(SLATE_DARK, -12), 255)
    draw = ImageDraw.Draw(endbell)
    draw.ellipse([11, 11, 20, 20], fill=(*STEEL, 255))
    draw.ellipse([14, 14, 17, 17], fill=(*SLATE_DARK, 255))
    save(octagon_mask(endbell), "endbell")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Gear socket plate: dark recess with four mounting bolts per tile.
    socket = noisy(64, (26, 30, 37), "socket", amount=3)
    pixels = socket.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((2, 2), (12, 2), (2, 12), (12, 12)):
                pixels[px + dx, py + dy] = (*BOLT, 255)
                pixels[px + dx + 1, py + dy + 1] = (*shade(SLATE_DARK, -10), 255)
    save(socket, "socket")
    # Bus bar from the Battery Cell terminal: copper.
    save(noisy(64, (184, 115, 51), "bus", amount=7, streak=4), "bus")

    # Shaft coupling for the item model: steel with a bolt circle.
    coupling = noisy(64, STEEL, "coupling", amount=5)
    pixels = coupling.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((4, 4), (11, 4), (4, 11), (11, 11)):
                pixels[px + dx, py + dy] = (*shade(BOLT, 20), 255)
    save(coupling, "coupling")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the terminal box.
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

    # Tachometer face: a scale from green through amber to a red line. The renderer draws the needle.
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

    # Renderer textures. The flame is greyscale and tinted by fuel; Servo and Battery Cell bodies are tinted by material.
    flame = Image.new("RGBA", (16, 16))
    pixels = flame.load()
    rand = rng("flame")
    for y in range(16):
        for x in range(16):
            core = 1.0 - min(1.0, abs(x - 7.5) / 8.0)
            rise = y / 15.0
            strength = max(0.0, min(1.0, 0.35 + 0.65 * core * (0.4 + 0.6 * rise) + rand.uniform(-0.12, 0.12)))
            pixels[x, y] = (clamp(255 * strength), clamp(255 * strength), clamp(255 * strength), 255)
    save(flame, "fx/flame")
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)
            strength = (1.0 - distance) ** 1.1
            pixels[x, y] = (clamp(255 * strength), clamp(150 * strength), clamp(60 * strength), 255)
    save(glow, "fx/glow")

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
    write_textures()


if __name__ == "__main__":
    main()
