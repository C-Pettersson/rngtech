#!/usr/bin/env python3
"""Generate the Bio Generator block models and owned textures.

The Bio Generator is a small biogas plant: a digester tank under a membrane gas
holder dome, a feed hopper, a gas engine with a flywheel, and an exhaust stack.
The static housing is a block model; the dome, flywheel, culture canister,
slurry, gauge needle, and hopper contents are drawn by BioGeneratorRenderer,
whose constants match the geometry here. Run from the repository root:

    python tools/blockmodels/bio_generator.py
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
TEXTURES = ASSETS / "textures/block/bio_generator"
TEX = "rngtech:block/bio_generator/"

# The front faces north (z = 0). West (x = 0) is on the viewer's right.
TANK_X, TANK_Z = 9.5, 9.5
TANK_R = 5.5
TANK_TOP = 10.5
DOME_BASE = 11.0
# Dome tiers: (radius, y0, y1), stacked from the rim up.
DOME_TIERS = ((5.4, 11.0, 12.0), (4.6, 12.0, 12.9), (3.6, 12.9, 13.6), (2.3, 13.6, 14.1))
FLYWHEEL_X, FLYWHEEL_Y = 3.375, 4.75
FLYWHEEL_R = 2.25
STACK_FROM, STACK_TO = (1.5, 9.5, 1.5), (3.0, 15.0, 3.0)
TAN = math.tan(math.radians(22.5))
# Step between overlapping coplanar faces, in model pixels: invisible, but enough depth to stop z-fighting.
STAGGER = 0.03
# Automation port: a grid-connection cabinet on the back face. Its door is the seat for a Universal Connector's 8 px
# square ring (4..12 on the face), with a recessed socket in the middle.
CABINET_X0, CABINET_X1 = 2.5, 13.5
CABINET_Y0, CABINET_Y1 = 2.0, 13.0
# The cabinet body runs back into the digester up to the tank top; above it, columns step back around the gas
# holder dome so the cabinet wraps the tank with no gap. Each is (x0, x1, back z), clear of the dome at full swell.
CABINET_BACK = 11.5
CABINET_UPPER_COLUMNS = (
    (2.5, 4.0, 11.5),
    (4.0, 5.5, 13.2),
    (5.5, 7.0, 14.7),
    (7.0, 12.0, 15.0),
    (12.0, 13.0, 14.7),
    (13.0, 13.5, 13.7),
)
SOCKET_LOW, SOCKET_HIGH = 6.0, 10.0
SOCKET_RECESS = 0.5
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


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


def octagon_y(cx, cz, radius, y0, y1, textures, faces=FACES):
    """
    A regular octagon prism standing on the y axis: two crossed slabs plus four 45-degree corner squares. The pieces
    overlap, so their caps step in by STAGGER: coplanar overlapping faces with different UVs would z-fight.
    """
    half = radius * TAN
    elements = [
        box((cx - radius, y0, cz - half), (cx + radius, y1, cz + half), textures, faces),
        box((cx - half, y0 + STAGGER, cz - radius), (cx + half, y1 - STAGGER, cz + radius), textures, faces),
    ]
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = cx + sx * half, cz + sz * half
            elements.append(box(
                (x - half, y0 + 2 * STAGGER, z - half), (x + half, y1 - 2 * STAGGER, z + half), textures, faces,
                rotation={"origin": [r(x), r((y0 + y1) / 2), r(z)], "axis": "y", "angle": 45},
            ))
    return elements


def ring_z(cx, cy, outer, inner, z0, z1, textures, marked=None):
    """A flat octagonal ring on the z axis: four straight bars plus four diagonal bars. `marked` retextures one bar."""
    half = outer * TAN
    depth = outer - inner
    bars = [
        box((cx - half, cy + inner, z0), (cx + half, cy + outer, z1), marked or textures),
        box((cx - half, cy - outer, z0), (cx + half, cy - inner, z1), textures),
        box((cx + inner, cy - half, z0), (cx + outer, cy + half, z1), textures),
        box((cx - outer, cy - half, z0), (cx - inner, cy + half, z1), textures),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = cx + sx * mid, cy + sy * mid
            if sx * sy > 0:
                start, end = (x - half, y - depth / 2, z0), (x + half, y + depth / 2, z1)
            else:
                start, end = (x - depth / 2, y - half, z0), (x + depth / 2, y + half, z1)
            # Diagonal bars sit a hair inside the straight bars' faces so the overlapping corners do not z-fight.
            start, end = (start[0], start[1], z0 + STAGGER), (end[0], end[1], z1 - STAGGER)
            bars.append(box(start, end, textures, rotation={"origin": [r(x), r(y), r((z0 + z1) / 2)], "axis": "z", "angle": -45}))
    return bars


def cabinet_port():
    """
    The automation port: the plant's grid-connection cabinet, built into the back of the digester. Its body runs
    back into the tank wall, and above the tank top it steps back around the gas holder dome, so it reads as one mass
    with the tank from every angle. The tank's teal hoop carries on round its sides, and a stepped steel hood runs
    back to the dome. The door is flush with the block boundary and is the seat for a Universal Connector's ring;
    the socket with its copper pins sits recessed in the middle of the door.
    """
    x0, x1, y0, y1 = CABINET_X0, CABINET_X1, CABINET_Y0, CABINET_Y1
    lo, hi = SOCKET_LOW, SOCKET_HIGH
    door = 16.0 - SOCKET_RECESS
    edge = ("east", "west", "up", "down")
    elements = [
        # Body below the tank top; only its south face, seen through the socket, carries the door texture.
        box((x0, y0, CABINET_BACK), (x1, TANK_TOP, door), {"south": "cabinet", "*": "cabinet_side"}),
        # Door frame around the socket, flush with the block boundary.
        box((x0, y0, door), (x1, lo, 16.0), {"south": "cabinet", "*": "cabinet_side"}, faces=("south",) + edge),
        box((x0, hi, door), (x1, y1, 16.0), {"south": "cabinet", "*": "cabinet_side"}, faces=("south",) + edge),
        box((x0, lo, door), (lo, hi, 16.0), {"south": "cabinet", "*": "cabinet_side"}, faces=("south", "east", "west")),
        box((hi, lo, door), (x1, hi, 16.0), {"south": "cabinet", "*": "cabinet_side"}, faces=("south", "east", "west")),
    ]
    for index, (cx0, cx1, back) in enumerate(CABINET_UPPER_COLUMNS):
        elements.append(box((cx0, TANK_TOP, back), (cx1, y1, door), "cabinet_side", faces=("north", "east", "west", "up")))
        hood_x0 = cx0 - 0.25 if index == 0 else cx0
        hood_x1 = cx1 + 0.25 if index == len(CABINET_UPPER_COLUMNS) - 1 else cx1
        elements.append(box((hood_x0, y1, back), (hood_x1, y1 + 0.5, 16.0), "rim"))
    # The tank's teal hoop continues round both sides of the cabinet.
    for nub_x0, nub_x1 in ((x0 - 0.25, x0), (x1, x1 + 0.25)):
        elements.append(box((nub_x0, 9.0, CABINET_BACK), (nub_x1, 9.75, 16.0), "band"))
    return elements


def walls(x0, z0, x1, z1, y0, y1, thickness, textures):
    """Four walls of an open-topped box."""
    return [
        box((x0, y0, z0), (x1, y1, z0 + thickness), textures),
        box((x0, y0, z1 - thickness), (x1, y1, z1), textures),
        box((x0, y0, z0 + thickness), (x0 + thickness, y1, z1 - thickness), textures),
        box((x1 - thickness, y0, z0 + thickness), (x1, y1, z1 - thickness), textures),
    ]


def housing_elements():
    elements = []
    # Plinth with a teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))

    # Digester tank: footing ring, wall, teal hoop, and the rim the gas holder seals onto.
    elements += octagon_y(TANK_X, TANK_Z, TANK_R + 0.35, 2.0, 2.75, "rim", faces=SIDES + ("up",))
    elements += octagon_y(TANK_X, TANK_Z, TANK_R, 2.75, TANK_TOP, "tank", faces=SIDES)
    elements += octagon_y(TANK_X, TANK_Z, TANK_R + 0.25, 9.0, 9.75, "band", faces=SIDES + ("up", "down"))
    elements += octagon_y(TANK_X, TANK_Z, TANK_R + 0.35, TANK_TOP, DOME_BASE, "rim")

    # Sight glass on the tank front: frame, dark interior, and the glass the slurry shows through.
    front = TANK_Z - TANK_R
    elements.append(box((7.5, 3.0, front - 0.5), (8.0, 8.5, front), "rim"))
    elements.append(box((11.0, 3.0, front - 0.5), (11.5, 8.5, front), "rim"))
    elements.append(box((8.0, 3.0, front - 0.5), (11.0, 3.5, front), "rim"))
    elements.append(box((8.0, 8.0, front - 0.5), (11.0, 8.5, front), "rim"))
    elements.append(box((8.0, 3.5, front - 0.05), (11.0, 8.0, front), "interior", faces=("north",)))
    elements.append(box((8.0, 3.5, front - 0.45), (11.0, 8.0, front - 0.45), "glass", faces=("north", "south")))

    # Saddles for the culture canister on the plinth below the sight glass; the renderer adds the canister itself.
    for x in (8.5, 10.0):
        elements.append(box((x, 2.0, 0.6), (x + 0.5, 2.4, 2.2), "rim"))

    # Feed hopper: screw housing, two-tier funnel, and the chute into the tank.
    elements.append(box((12.0, 2.0, 0.75), (15.5, 6.5, 4.25), {"up": "hopper_inner", "*": "hopper"}))
    elements.append(box((11.0, 5.5, 2.0), (12.0, 6.5, 4.75), "pipe"))
    elements.append(box((12.25, 6.5, 1.0), (15.25, 7.75, 4.0), "hopper"))
    elements.append(box((12.75, 7.75, 1.5), (14.75, 7.9, 3.5), "hopper_inner", faces=("up",)))
    elements += walls(12.25, 1.0, 15.25, 4.0, 7.75, 8.75, 0.5, "hopper")
    elements += walls(11.75, 0.5, 15.75, 4.5, 8.75, 9.5, 0.5, "hopper_rim")

    # Gas engine: block, cylinder head with the gas-train gauge, terminal box, and the gas line from the tank.
    elements.append(box((0.75, 2.0, 0.75), (6.0, 7.5, 5.5), {"north": "engine_front", "*": "engine"}))
    elements.append(box((1.25, 7.5, 1.25), (5.5, 9.5, 5.0), {"up": "head_top", "*": "head"}))
    elements.append(box((2.375, 7.5, 0.95), (4.375, 9.5, 1.25), {"north": "gauge", "*": "casing"}, uv={"north": [0, 0, 16, 16]}))
    elements.append(box((0.25, 3.0, 1.5), (0.75, 6.0, 4.5), {"west": "bus", "*": "casing"}))
    elements.append(box((3.0, 9.5, 3.25), (7.6, 10.25, 4.0), "pipe"))
    elements.append(box((5.25, 10.25, 3.0), (6.0, 10.75, 4.25), "valve"))

    # Exhaust stack rising straight off the cylinder head, clear of the west port, with a teal band.
    elements.append(box(STACK_FROM, STACK_TO, {"up": "interior", "*": "stack"}))
    elements.append(box((STACK_FROM[0] - 0.25, 12.5, STACK_FROM[2] - 0.25), (STACK_TO[0] + 0.25, 13.0, STACK_TO[2] + 0.25), "band"))

    # Automation works on every face; the one visual port, the grid-connection cabinet on the back, marks the spot.
    # A copper conduit runs from the engine's terminal box along the plinth into the cabinet, so it plainly carries power.
    elements += cabinet_port()
    elements.append(box((0.3, 2.6, 3.9), (0.9, 3.0, 4.5), "conduit"))
    elements.append(box((0.3, 2.0, 3.9), (0.9, 2.6, 15.2), "conduit"))
    elements.append(box((0.9, 2.0, 14.6), (CABINET_X0, 2.6, 15.2), "conduit"))

    # Status lamp on the front of the hopper's screw housing.
    elements.append(box(
        (13.0, 4.25, 0.5), (14.5, 5.25, 0.75), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def dome_elements():
    elements = []
    for radius, y0, y1 in DOME_TIERS:
        elements += octagon_y(TANK_X, TANK_Z, radius, y0, y1, "membrane", faces=SIDES + ("up",))
    top = DOME_TIERS[-1][2]
    elements.append(box((TANK_X - 0.5, top, TANK_Z - 0.5), (TANK_X + 0.5, top + 0.6, TANK_Z + 0.5), "valve"))
    return elements


def flywheel_elements():
    """An octagonal rim with one teal balance weight, four spokes, and a hub, centred on the crankshaft."""
    elements = ring_z(FLYWHEEL_X, FLYWHEEL_Y, FLYWHEEL_R, FLYWHEEL_R - 0.6, 0.1, 0.65, "flywheel", marked="weight")
    spoke = FLYWHEEL_R - 0.4
    elements.append(box((FLYWHEEL_X - spoke, FLYWHEEL_Y - 0.25, 0.2), (FLYWHEEL_X + spoke, FLYWHEEL_Y + 0.25, 0.55), "flywheel"))
    elements.append(box((FLYWHEEL_X - 0.25, FLYWHEEL_Y - spoke, 0.2), (FLYWHEEL_X + 0.25, FLYWHEEL_Y + spoke, 0.55), "flywheel"))
    elements.append(box((FLYWHEEL_X - 0.6, FLYWHEEL_Y - 0.6, 0.0), (FLYWHEEL_X + 0.6, FLYWHEEL_Y + 0.6, 0.75), "hub"))
    return elements


def culture_elements():
    """The installed Bio Chamber: a glass culture canister lying in its saddles in front of the sight glass."""
    return [
        box((8.0, 2.1, 0.8), (11.0, 3.3, 2.0), "culture", faces=("north", "south", "up")),
        box((7.5, 2.0, 0.7), (8.0, 3.45, 2.1), "valve"),
        box((11.0, 2.0, 0.7), (11.5, 3.45, 2.1), "valve"),
    ]


HOUSING_TEXTURES = {
    "particle": TEX + "tank",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "tank": TEX + "tank",
    "band": TEX + "band",
    "rim": TEX + "rim",
    "interior": TEX + "interior",
    "glass": TEX + "glass",
    "hopper": TEX + "hopper",
    "hopper_rim": TEX + "hopper_rim",
    "hopper_inner": TEX + "hopper_inner",
    "pipe": TEX + "pipe",
    "valve": TEX + "valve",
    "engine": TEX + "engine",
    "engine_front": TEX + "engine_front",
    "head": TEX + "head",
    "head_top": TEX + "head_top",
    "gauge": TEX + "gauge",
    "bus": TEX + "bus",
    "cabinet": TEX + "cabinet",
    "cabinet_side": TEX + "cabinet_side",
    "conduit": TEX + "conduit",
    "stack": TEX + "stack",
    "lamp": TEX + "lamp_off",
}
PART_TEXTURES = {
    "membrane": TEX + "membrane",
    "flywheel": TEX + "flywheel",
    "weight": TEX + "flywheel_weight",
    "hub": TEX + "hub",
    "culture": TEX + "culture",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/bio_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/bio_generator_active.json", {
        "parent": "rngtech:block/bio_generator",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "block/bio_generator/dome.json", {
        "textures": {"particle": "#membrane", "membrane": PART_TEXTURES["membrane"], "valve": TEX + "valve"},
        "elements": dome_elements(),
    })
    write_json(MODELS / "block/bio_generator/flywheel.json", {
        "textures": {
            "particle": "#flywheel",
            "flywheel": PART_TEXTURES["flywheel"],
            "weight": PART_TEXTURES["weight"],
            "hub": PART_TEXTURES["hub"],
        },
        "elements": flywheel_elements(),
    })
    write_json(MODELS / "block/bio_generator/culture.json", {
        "render_type": "minecraft:cutout",
        "textures": {"particle": "#culture", "culture": PART_TEXTURES["culture"], "valve": TEX + "valve"},
        "elements": culture_elements(),
    })
    write_json(MODELS / "item/bio_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, **PART_TEXTURES},
        "elements": housing + dome_elements() + flywheel_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"bio_generator:{name}")


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noisy(size, color, name, amount=6, streak=0, vertical=False):
    rand = rng(name)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    lines = [rand.uniform(-streak, streak) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            pixels[x, y] = (*shade(color, rand.uniform(-amount, amount) + lines[x if vertical else y]), 255)
    return image


def panel_seams(image, step=16, rivets=True, horizontal=True, vertical=True):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            if (vertical and x % step == 0) or (horizontal and y % step == 0):
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
            elif (vertical and x % step == 1) or (horizontal and y % step == 1):
                pixels[x, y] = (*shade(pixels[x, y], 12), 255)
    if rivets:
        for py in range(0, size, step):
            for px in range(0, size, step):
                for dx, dy in ((3, 3), (step - 4, 3), (3, step - 4), (step - 4, step - 4)):
                    x, y = px + dx, py + dy
                    pixels[x, y] = (*shade(pixels[x, y], 34), 255)
                    pixels[x + 1, y + 1] = (*shade(pixels[x + 1, y + 1], -24), 255)
    return image


def stripes(image, period, light, dark, axis="y"):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            coordinate = y if axis == "y" else x
            if coordinate % period == 0:
                pixels[x, y] = (*shade(pixels[x, y], dark), 255)
            elif coordinate % period == 1:
                pixels[x, y] = (*shade(pixels[x, y], light), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
STEEL_BLUE = (82, 96, 112)
TANK = (60, 72, 80)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
COPPER = (184, 115, 51)
MEMBRANE = (108, 126, 88)
MEMBRANE_STRAP = (72, 86, 60)
SLURRY = (96, 82, 44)
CULTURE = (92, 168, 76)


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

    # Digester wall: tall vertical plates with rivet lines, darkening toward the base.
    tank = noisy(64, TANK, "tank", amount=4, streak=4, vertical=True)
    pixels = tank.load()
    for y in range(64):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], -y * 0.18), 255)
            if x % 12 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -20), 255)
            elif x % 12 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 10), 255)
            if x % 12 == 3 and y % 6 == 2:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(tank, "tank")

    save(stripes(noisy(64, TEAL, "band", amount=6), 4, 30, -60), "band")
    save(panel_seams(noisy(64, (104, 114, 126), "rim", amount=5), step=8, rivets=False, vertical=False), "rim")
    save(noisy(64, (16, 20, 18), "interior", amount=3), "interior")

    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    draw.line([(23, 22), (23, 46)], fill=(206, 240, 250, 255))
    draw.line([(26, 22), (26, 30)], fill=(206, 240, 250, 255))
    save(glass, "glass")

    hopper = noisy(64, STEEL_BLUE, "hopper", amount=5, streak=4)
    pixels = hopper.load()
    for y in range(64):
        for x in range(64):
            if y % 16 in (12, 13):
                pixels[x, y] = (*shade(TEAL, -14), 255)
    save(hopper, "hopper")
    save(stripes(noisy(64, (128, 138, 150), "hopper_rim", amount=6), 3, 14, -16), "hopper_rim")

    inner = noisy(64, (52, 44, 30), "hopper_inner", amount=8)
    pixels = inner.load()
    rand = rng("hopper_inner_bits")
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((96, 120, 54), rand.uniform(-20, 20)), 255)
    save(inner, "hopper_inner")

    pipe = noisy(64, (140, 150, 162), "pipe", amount=6)
    pixels = pipe.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (6, 7):
                pixels[x, y] = (*shade((200, 176, 64), -20), 255)
    save(pipe, "pipe")

    save(noisy(64, TEAL, "valve", amount=10), "valve")

    engine = stripes(noisy(64, (50, 58, 70), "engine", amount=4), 4, 14, -18)
    save(engine, "engine")
    engine_front = panel_seams(noisy(64, (50, 58, 70), "engine_front", amount=4), step=32, rivets=True)
    save(engine_front, "engine_front")
    head = noisy(64, TEAL_DARK, "head", amount=5)
    pixels = head.load()
    for y in range(64):
        for x in range(64):
            if y % 8 in (0, 1):
                pixels[x, y] = (*shade(TEAL, -20), 255)
    save(head, "head")
    head_top = noisy(64, TEAL_DARK, "head_top", amount=5)
    draw = ImageDraw.Draw(head_top)
    for bx in (8, 24, 40, 56):
        draw.rectangle([bx - 2, 30, bx + 1, 33], fill=(*shade(SLATE, 30), 255))
    save(head_top, "head_top")

    # Gas-train gauge face; the renderer draws the needle. The amber arc is below the engine's start pressure.
    gauge = Image.new("RGBA", (64, 64), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([2, 2, 61, 61], fill=(120, 128, 138, 255))
    draw.ellipse([6, 6, 57, 57], fill=(222, 226, 210, 255))
    draw.arc([12, 12, 51, 51], start=135, end=135 + 270 * 0.25, fill=(214, 164, 58, 255), width=4)
    draw.arc([12, 12, 51, 51], start=135 + 270 * 0.25, end=405, fill=(96, 168, 92, 255), width=4)
    for step in range(11):
        angle = math.radians(135 + 27 * step)
        inner_r, outer_r = (17, 24) if step % 5 == 0 else (20, 24)
        draw.line([
            (31.5 + math.cos(angle) * inner_r, 31.5 + math.sin(angle) * inner_r),
            (31.5 + math.cos(angle) * outer_r, 31.5 + math.sin(angle) * outer_r),
        ], fill=(40, 40, 40, 255), width=2 if step % 5 == 0 else 1)
    draw.ellipse([28, 28, 35, 35], fill=(40, 40, 40, 255))
    save(gauge, "gauge")

    bus = noisy(64, (34, 40, 48), "bus", amount=4)
    pixels = bus.load()
    for top in (16, 30, 44):
        for y in range(top, top + 5):
            for x in range(14, 50):
                pixels[x, y] = (*shade(COPPER, 26 if y == top else (-26 if y == top + 4 else 0)), 255)
    save(bus, "bus")

    write_port_texture()

    # Exhaust stack: bare steel that tempers to straw, bronze, and blue toward the hot top.
    stack = noisy(64, (120, 126, 134), "stack", amount=5, streak=5, vertical=True)
    pixels = stack.load()
    temper = [(120, 126, 134), (168, 150, 104), (150, 102, 70), (86, 92, 132)]
    for y in range(64):
        heat = max(0.0, (24 - y) / 24.0)
        band = min(len(temper) - 2, int(heat * (len(temper) - 1)))
        color = mix(temper[band], temper[band + 1], heat * (len(temper) - 1) - band)
        for x in range(64):
            pixels[x, y] = (*mix(pixels[x, y], color, heat * 0.85), 255)
    save(stack, "stack")

    save(noisy(64, (18, 52, 58), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (110, 236, 255), "lamp_on", amount=6), "lamp_on")

    # Gas holder membrane: olive fabric with tension straps running up the dome.
    membrane = noisy(64, MEMBRANE, "membrane", amount=4)
    pixels = membrane.load()
    for y in range(64):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], (32 - y) * 0.25), 255)
            if x % 16 in (7, 8):
                pixels[x, y] = (*shade(MEMBRANE_STRAP, -4 if x % 16 == 8 else 4), 255)
            elif y % 21 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -10), 255)
    save(membrane, "membrane")

    flywheel = noisy(64, (66, 72, 80), "flywheel", amount=6)
    save(stripes(flywheel, 6, 18, -10, axis="x"), "flywheel")
    save(noisy(64, TEAL, "flywheel_weight", amount=8), "flywheel_weight")
    hub = noisy(64, (150, 158, 168), "hub", amount=6)
    save(hub, "hub")

    # Culture canister glass: green culture with rising bubbles and a bright glint.
    culture = noisy(64, CULTURE, "culture", amount=10)
    pixels = culture.load()
    rand = rng("culture_bubbles")
    for _ in range(40):
        x, y = rand.randrange(2, 62), rand.randrange(2, 62)
        pixels[x, y] = (*shade(CULTURE, 70), 255)
    for y in range(64):
        pixels[10, y] = (*shade(pixels[10, y], 60), 255)
        pixels[11, y] = (*shade(pixels[11, y], 30), 255)
        pixels[0, y] = pixels[63, y] = (*shade(CULTURE, -50), 255)
    save(culture, "culture")

    slurry = noisy(64, SLURRY, "fx_slurry", amount=10)
    pixels = slurry.load()
    rand = rng("fx_slurry_flecks")
    for _ in range(160):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((122, 132, 58), rand.uniform(-16, 16)), 255)
    save(slurry, "fx/slurry")

    white = Image.new("RGBA", (16, 16), (255, 255, 255, 255))
    ImageDraw.Draw(white).rectangle([0, 0, 15, 15], outline=(235, 240, 230, 255))
    save(white, "fx/solid")


def write_port_texture() -> None:
    """
    The grid-connection cabinet, mapped by position on the back face: a slate door with a seam and hinges, louvres
    under the hood, a teal hazard kick strip like the plinth's, a teal seat ring, and the dark socket with copper pins.
    """
    texel = lambda px: int(round(px * 4))
    u = lambda x: texel(x)
    v = lambda y: texel(16 - y)
    cabinet = noisy(64, SLATE, "cabinet", amount=4)
    draw = ImageDraw.Draw(cabinet)
    pixels = cabinet.load()
    # Door seam, a bevel inside it, and two hinges on the right as seen from behind.
    draw.rectangle([u(3.0), v(12.5), u(13.0) - 1, v(3.25) - 1], outline=(*shade(SLATE_DARK, -10), 255))
    draw.rectangle([u(3.0) + 1, v(12.5) + 1, u(13.0) - 2, v(3.25) - 2], outline=(*shade(SLATE, 16), 255))
    for hinge in (v(11.5), v(5.0)):
        draw.rectangle([u(12.5), hinge, u(13.0), hinge + 4], fill=(*shade(STEEL, 10), 255))
    # Louvres under the hood.
    for y in range(v(12.25), v(11.0)):
        if y % 2 == 0:
            for x in range(u(4.0), u(12.0)):
                pixels[x, y] = (*shade(SLATE_DARK, -6), 255)
    # Teal hazard kick strip along the bottom, echoing the plinth band.
    for y in range(v(2.75), v(2.0)):
        for x in range(64):
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)
    # Teal ring marking the seat, and the socket with four copper pins.
    draw.rectangle([u(4.5), v(11.5), u(11.5) - 1, v(4.5) - 1], outline=(*TEAL, 255), width=2)
    draw.rectangle([u(SOCKET_LOW), v(SOCKET_HIGH), u(SOCKET_HIGH) - 1, v(SOCKET_LOW) - 1], fill=(13, 16, 20, 255), outline=(6, 8, 10, 255))
    centre = u(8.0)
    for dx in (-4, 2):
        for dy in (-4, 2):
            draw.rectangle([centre + dx, centre + dy, centre + dx + 1, centre + dy + 1], fill=(*COPPER, 255))
            cabinet.putpixel((centre + dx, centre + dy), (*shade(COPPER, 40), 255))
    save(cabinet, "cabinet")

    # Cabinet sides, top, and back: plain slate panels with seams, never the door.
    save(panel_seams(noisy(64, SLATE, "cabinet_side", amount=4), step=16, rivets=True, horizontal=False), "cabinet_side")

    conduit = noisy(64, COPPER, "conduit", amount=8)
    pixels = conduit.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (0, 1):
                pixels[x, y] = (*shade(SLATE, 10), 255)
            elif y % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 26), 255)
    save(conduit, "conduit")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
