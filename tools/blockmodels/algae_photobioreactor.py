#!/usr/bin/env python3
"""Generate the Algae Photobioreactor block model, item model, and owned textures.

The design follows an airlift bubble-column photobioreactor: three glass culture
columns stand on a gas sparger manifold and vent into a degassing header, which
overflows into a rear settling tank where the harvest collects. A rotameter on the
front shows the aeration setting. The housing is a static block model; the culture,
bubbles, tank levels, settled harvest, rotameter float, and light-sensor glow are
drawn by AlgaePhotobioreactorRenderer, whose constants match the geometry here.
Run from the repository root:

    python tools/blockmodels/algae_photobioreactor.py
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
TEXTURES = ASSETS / "textures/block/algae_photobioreactor"
TEX = "rngtech:block/algae_photobioreactor/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# Culture columns: centres along x, all at COLUMN_Z; the front faces north (z = 0).
COLUMN_XS = (4.0, 8.0, 12.0)
COLUMN_Z = 5.0
COLUMN_R = 1.75
GLASS_Y0, GLASS_Y1 = 4.0, 13.5
TAN = math.tan(math.radians(22.5))
# Sight glasses: Water on the west, carbon gas on the east.
SIGHT_XS = (0.75, 13.25)
SIGHT_Z0, SIGHT_Z1 = 12.75, 15.25
SIGHT_Y0, SIGHT_Y1 = 3.5, 11.5
# Settling tank behind the columns, above the pump cabinet.
SETTLER = ((4.0, 8.0, 10.5), (12.0, 14.0, 15.0))
POST = 0.75
# Rotameter on the front right.
ROTAMETER = ((13.5, 3.0, 0.25), (15.0, 9.0, 1.75))
# Cable port on the rear of the pump cabinet, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
VENT = ((2.0, 15.75, 4.5), (3.0, 16.0, 5.5))


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


def glass_column(cx, cz, radius, y0, y1, texture):
    """An octagonal glass tube around a vertical axis, one face per side so no faces overlap.

    Two straight slabs give the four flat sides; two slabs turned 45 degrees give the four diagonal sides.
    """
    half = radius * TAN
    origin = [r(cx), r((y0 + y1) / 2), r(cz)]
    narrow = ((cx - half, y0, cz - radius), (cx + half, y1, cz + radius))
    return [
        box((cx - radius, y0, cz - half), (cx + radius, y1, cz + half), texture, faces=("east", "west")),
        box(*narrow, texture, faces=("north", "south")),
        box(*narrow, texture, faces=("north", "south"), rotation={"origin": origin, "axis": "y", "angle": 45}),
        box(*narrow, texture, faces=("north", "south"), rotation={"origin": origin, "axis": "y", "angle": -45}),
    ]


# Sight-glass faces map a framed strip of the texture at 8 texels per pixel: the narrow (2 px) north and south
# faces use columns 0-15, the wider (2.5 px) east and west faces use columns 32-51.
SIGHT_UV = {
    "north": [0, 0, 4, 16],
    "south": [0, 0, 4, 16],
    "east": [8, 0, 13, 16],
    "west": [8, 0, 13, 16],
}


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))
    # Gas sparger manifold under the columns, with a teal band.
    elements.append(box((1.5, 2.0, 2.5), (14.5, 3.5, 7.5), "manifold"))
    elements.append(box((1.25, 2.5, 2.25), (14.75, 3.0, 7.75), "band", faces=SIDES))
    for cx in COLUMN_XS:
        # Bottom flange, glass tube, mid clamp, top cap.
        elements.append(box((cx - 1.8, 3.5, COLUMN_Z - 1.8), (cx + 1.8, GLASS_Y0, COLUMN_Z + 1.8), "flange"))
        elements += glass_column(cx, COLUMN_Z, COLUMN_R, GLASS_Y0, GLASS_Y1, "glass")
        elements.append(box((cx - 1.9, 8.75, COLUMN_Z - 1.9), (cx + 1.9, 9.25, COLUMN_Z + 1.9), "band", faces=SIDES))
        elements.append(box((cx - 1.9, GLASS_Y1, COLUMN_Z - 1.9), (cx + 1.9, 14.5, COLUMN_Z + 1.9), "cap"))
    # Support rail the clamps strap to, behind the columns.
    elements.append(box((1.5, 8.5, 6.9), (14.5, 9.5, 7.6), "band"))
    for x0 in (1.5, 13.75):
        elements.append(box((x0, 3.5, 6.9), (x0 + 0.75, 8.5, 7.6), "casing"))
    # Degassing header across the column tops, with an oxygen vent.
    elements.append(box((1.75, 14.5, 4.0), (14.25, 15.75, 6.0), "pipe"))
    elements.append(box(*VENT, "bolt"))
    # Overflow line from the header to the settling tank.
    elements.append(box((7.25, 14.75, 6.0), (8.75, 15.5, 11.0), "pipe", faces=("east", "west", "up", "down")))
    elements.append(box((7.25, 14.0, 10.0), (8.75, 15.5, 11.5), "pipe", faces=("north", "east", "west", "up")))
    # Pump and control cabinet, with the cable port on its back.
    elements.append(box((3.0, 2.0, 10.0), (13.0, 8.0, 15.5), {"south": "cabinet", "north": "cabinet", "*": "casing"}))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": [0, 0, 16, 16]}))
    # Settling tank: glass walls in a steel frame, with a lid and a light sensor.
    (sx0, sy0, sz0), (sx1, sy1, sz1) = SETTLER
    elements.append(box((sx0 + 0.25, sy0, sz0 + 0.25), (sx1 - 0.25, sy1, sz1 - 0.25), "glass_tank", faces=SIDES))
    for px in (sx0, sx1 - POST):
        for pz in (sz0, sz1 - POST):
            elements.append(box((px, sy0, pz), (px + POST, sy1, pz + POST), "flange"))
    elements.append(box((sx0, sy1, sz0), (sx1, sy1 + 0.75, sz1), "cap"))
    elements.append(box((7.0, sy1 + 0.75, 12.0), (9.0, sy1 + 1.25, 14.0), "sensor"))
    # Sight glasses with collars, and the feed lines forward into the manifold.
    for x0 in SIGHT_XS:
        x1 = x0 + 2.0
        elements.append(box((x0 - 0.25, 2.0, SIGHT_Z0 - 0.25), (x1 + 0.25, SIGHT_Y0, SIGHT_Z1 + 0.25), "pipe"))
        elements.append(box((x0 - 0.25, SIGHT_Y1, SIGHT_Z0 - 0.25), (x1 + 0.25, 13.0, SIGHT_Z1 + 0.25), "pipe"))
        elements.append(box((x0, SIGHT_Y0, SIGHT_Z0), (x1, SIGHT_Y1, SIGHT_Z1), "sight_glass", faces=SIDES, uv=SIGHT_UV))
        elements.append(box((x0 + 0.5, 2.0, 7.5), (x1 - 0.5, 3.0, SIGHT_Z0 - 0.25), "pipe", faces=("east", "west", "up")))
    # Rotameter: a glass tube between two fittings on the gas feed.
    (rx0, ry0, rz0), (rx1, ry1, rz1) = ROTAMETER
    elements.append(box((rx0 - 0.25, 2.0, rz0 - 0.1), (rx1 + 0.25, ry0, rz1 + 0.1), "flange"))
    full = {face: [0, 0, 16, 16] for face in SIDES}
    elements.append(box((rx0, ry0, rz0), (rx1, ry1, rz1), "rotameter", faces=SIDES, uv=full))
    elements.append(box((rx0 - 0.25, ry1, rz0 - 0.1), (rx1 + 0.25, ry1 + 1.0, rz1 + 0.1), "flange"))
    elements.append(box((rx0 + 0.35, 2.25, rz1 + 0.1), (rx0 + 0.95, 2.85, 2.5), "pipe", faces=("east", "west", "up")))
    # Dial and status lamp on the plinth front.
    elements.append(box((0.75, 2.0, 0.25), (3.25, 4.5, 1.25), {"north": "gauge", "*": "casing"}, uv={"north": [0, 0, 16, 16]}))
    elements.append(box(
        (11.75, 2.0, 0.5), (13.25, 3.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def item_culture_elements():
    """Static culture for the item model, which has no renderer to draw it."""
    inner = COLUMN_R * 0.82
    return [box((cx - inner, GLASS_Y0, COLUMN_Z - inner), (cx + inner, GLASS_Y1 - 0.5, COLUMN_Z + inner), "culture")
            for cx in COLUMN_XS]


HOUSING_TEXTURES = {
    "particle": TEX + "manifold",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "manifold": TEX + "manifold",
    "band": TEX + "band",
    "flange": TEX + "flange",
    "cap": TEX + "cap",
    "bolt": TEX + "bolt",
    "glass": TEX + "glass",
    "sight_glass": TEX + "sight_glass",
    "glass_tank": TEX + "glass_tank",
    "pipe": TEX + "pipe",
    "cabinet": TEX + "cabinet",
    "sensor": TEX + "sensor",
    "gauge": TEX + "gauge",
    "rotameter": TEX + "rotameter",
    "port": TEX + "port",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/algae_photobioreactor.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/algae_photobioreactor_active.json", {
        "parent": "rngtech:block/algae_photobioreactor",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/algae_photobioreactor.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, "culture": TEX + "culture"},
        "elements": item_culture_elements() + housing,
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"algae_photobioreactor:{name}")


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


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL_BLUE = (82, 96, 112)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
CULTURE_PALE = (216, 236, 142)
CULTURE_DENSE = (72, 184, 62)


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

    # Sparger manifold: brushed steel-blue with a row of nozzle holes along the top.
    manifold = noisy(64, STEEL_BLUE, "manifold", amount=5, streak=6)
    pixels = manifold.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -20), 255)
    for y in range(8, 64, 16):
        for x in range(2, 64, 6):
            pixels[x, y] = (18, 24, 32, 255)
            pixels[x + 1, y] = (*shade(STEEL_BLUE, 30), 255)
    save(manifold, "manifold")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    save(noisy(64, STEEL, "flange", amount=5), "flange")
    save(noisy(64, (150, 158, 168), "bolt", amount=8), "bolt")

    # Column caps: dark steel with a bolt circle on every 16-texel tile.
    cap = noisy(64, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for angle in range(0, 360, 60):
                x = px + 8 + round(math.cos(math.radians(angle)) * 5)
                y = py + 8 + round(math.sin(math.radians(angle)) * 5)
                pixels[x % 64, y % 64] = (*shade((150, 158, 168), 10), 255)
    save(cap, "cap")

    # Glass: transparent (cutout) with sparse vertical glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = glass.load()
    for x in range(64):
        if x % 16 in (3, 4):
            for y in range(64):
                if (y // 5) % 4 != 3:
                    pixels[x, y] = GLINT
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
    tank = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(tank)
    for offset in (0, 32):
        draw.line([(offset + 20, 12), (offset + 28, 4)], fill=GLINT)
        draw.line([(offset + 21, 17), (offset + 24, 14)], fill=GLINT)
    save(tank, "glass_tank")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Pump cabinet: seamed casing with louvred vents; the cable port is its only fitting.
    cabinet = panel_seams(noisy(64, (40, 47, 57), "cabinet", amount=4), rivets=False)
    pixels = cabinet.load()
    for y in range(20, 44):
        if y % 3 == 0:
            for x in range(12, 52):
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
        elif y % 3 == 1:
            for x in range(12, 52):
                pixels[x, y] = (*shade(SLATE, 18), 255)
    save(cabinet, "cabinet")

    # Light sensor: a white diffuser dome.
    sensor = noisy(64, (214, 222, 226), "sensor", amount=5)
    save(sensor, "sensor")

    # pH dial: green-to-amber scale, needle near neutral.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    scale = [(-225, (196, 160, 52)), (-180, (170, 190, 60)), (-135, (90, 170, 70)), (-90, (60, 150, 90)),
             (-45, (60, 130, 150)), (0, (70, 90, 170)), (45, (110, 70, 160))]
    for angle, color in scale:
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    draw.line([(7.5, 7.5), (8.5, 3.5)], fill=(196, 52, 40, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    save(gauge, "gauge")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the junction plate.
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

    # Rotameter tube: clear glass between steel rails, with a flow scale on one rail.
    rotameter = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = rotameter.load()
    for y in range(16):
        for x in (0, 1, 14, 15):
            pixels[x, y] = (*shade(STEEL, 12 if x in (0, 14) else -10), 255)
        if y % 3 == 1:
            pixels[2, y] = (222, 226, 210, 255)
    for y in (5, 11):
        pixels[9, y] = GLINT
        pixels[9, y + 1] = GLINT
    save(rotameter, "rotameter")

    save(noisy(64, (18, 52, 34), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (128, 240, 120), "lamp_on", amount=6), "lamp_on")

    # Item-model culture, between thin and dense.
    culture = noisy(64, mix(CULTURE_PALE, CULTURE_DENSE, 0.65), "culture", amount=7, streak=4)
    pixels = culture.load()
    rand = rng("culture_bubbles")
    for _ in range(40):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], 55), 255)
    save(culture, "culture")

    # Renderer textures: culture fill (tinted by density), bubbles, settled harvest, sensor glow.
    culture_fill = noisy(16, (236, 236, 236), "culture_fill", amount=10, streak=4)
    save(culture_fill, "fx/culture")
    bubble = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bubble)
    draw.ellipse([2, 2, 13, 13], fill=(220, 245, 230, 150), outline=(250, 255, 252, 230))
    draw.point([(5, 5), (6, 5), (5, 6)], fill=(255, 255, 255, 255))
    save(bubble, "fx/bubble")
    sludge = noisy(16, (52, 86, 34), "sludge", amount=12)
    pixels = sludge.load()
    rand = rng("sludge_flecks")
    for _ in range(18):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (*shade((88, 130, 52), rand.uniform(-10, 10)), 255)
    save(sludge, "fx/sludge")
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)
            strength = (1.0 - distance) ** 1.2
            pixels[x, y] = (clamp(255 * strength), clamp(236 * strength), clamp(170 * strength), 255)
    save(glow, "fx/glow")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
