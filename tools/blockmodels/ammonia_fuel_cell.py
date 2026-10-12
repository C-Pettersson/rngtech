#!/usr/bin/env python3
"""Generate the Ammonia Fuel Cell block model, item model, and owned textures.

The design follows a clamped fuel cell stack: graphite bipolar plates between copper current collectors and two
steel end plates, compressed by four tie rods. The stack runs across the front so its cells read as stripes. Copper
bus bars carry the current back to a power-conditioning cabinet whose rear wall holds the cable port. A framed
glass Ammonia tank feeds the left end plate; the right end plate exhausts into a framed glass residue trap with a
vent stack. The housing is a static block model; the membranes between the plates, the Ammonia level, and the
residue in the trap are drawn by AmmoniaFuelCellRenderer, whose constants match the geometry here. Run from the
repository root:

    python tools/blockmodels/ammonia_fuel_cell.py
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
TEXTURES = ASSETS / "textures/block/ammonia_fuel_cell"
TEX = "rngtech:block/ammonia_fuel_cell/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# The front faces north (z = 0); the stack axis runs along x.
END_Y0, END_Y1 = 2.0, 13.0
END_Z0, END_Z1 = 0.5, 10.5
END_PLATES = ((2.0, 3.25), (12.75, 14.0))
COLLECTORS = ((3.25, 3.75), (12.25, 12.75))
COLLECTOR_Y0, COLLECTOR_Y1 = 3.25, 12.0
COLLECTOR_Z0, COLLECTOR_Z1 = 1.5, 9.5
# Bipolar plates and the membrane gaps between them: twelve cells, gap - plate - gap ... - gap.
STACK_X0 = 3.75
GAP = 0.25
PLATE = 0.5
PLATES = 11
PLATE_Y0, PLATE_Y1 = 3.5, 11.75
PLATE_Z0, PLATE_Z1 = 1.75, 9.25
# Membranes stand a little proud of the plates at the top, front, and back, like gaskets.
MEMBRANE_PROUD = 0.125
# Tie rods: centres in (y, z), running along x through both end plates.
RODS = ((2.75, 1.0), (2.75, 10.0), (12.25, 1.0), (12.25, 10.0))
ROD_X0, ROD_X1 = 1.0, 15.0
# Ammonia tank on the back left, residue trap on the back right; both are framed glass.
TANK = ((0.75, 3.0, 11.5), (3.5, 12.5, 15.5))
TRAP = ((12.5, 2.75, 11.5), (15.25, 8.25, 15.5))
POST = 0.5
# Power-conditioning cabinet, with the cable port on its back, sized to the Universal Connector ring (4 to 12).
CABINET = ((3.875, 2.0, 11.0), (12.125, 14.5, 15.5))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
VENT = ((14.25, 9.0, 14.25), (15.0, 15.0, 15.0))


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


def gaps():
    """The x span of each membrane gap, left to right."""
    return [(STACK_X0 + i * (GAP + PLATE), STACK_X0 + i * (GAP + PLATE) + GAP) for i in range(PLATES + 1)]


def plates():
    return [(x1, x1 + PLATE) for _, x1 in gaps()[:PLATES]]


FULL = [0, 0, 16, 16]


def framed_glass(bounds, base_y0, lid_y1):
    """A glass box between four corner posts, on a steel collar and under a steel lid. The glass carries level ticks."""
    (x0, y0, z0), (x1, y1, z1) = bounds
    elements = [
        box((x0 - 0.25, base_y0, z0 - 0.25), (x1 + 0.25, y0, z1 + 0.25), "flange"),
        box((x0 + 0.1, y0, z0 + 0.1), (x1 - 0.1, y1, z1 - 0.1), "glass", faces=SIDES, uv={face: FULL for face in SIDES}),
        box((x0 - 0.25, y1, z0 - 0.25), (x1 + 0.25, lid_y1, z1 + 0.25), "cap"),
    ]
    for px in (x0, x1 - POST):
        for pz in (z0, z1 - POST):
            elements.append(box((px, y0, pz), (px + POST, y1, pz + POST), "flange", faces=SIDES))
    return elements


def housing_elements():
    elements = []
    # Plinth with a teal hazard band and a tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))
    # Insulating bed under the cells.
    elements.append(box((STACK_X0, 2.0, 3.0), (16 - STACK_X0, PLATE_Y0, 7.5), "casing"))
    # End plates, current collectors, and the bipolar plates between them.
    for x0, x1 in END_PLATES:
        elements.append(box((x0, END_Y0, END_Z0), (x1, END_Y1, END_Z1), "end_plate"))
    for x0, x1 in COLLECTORS:
        elements.append(box((x0, COLLECTOR_Y0, COLLECTOR_Z0), (x1, COLLECTOR_Y1, COLLECTOR_Z1), "copper"))
    for x0, x1 in plates():
        elements.append(box((x0, PLATE_Y0, PLATE_Z0), (x1, PLATE_Y1, PLATE_Z1), "plate"))
    # Tie rods with a nut outside each end plate.
    for y, z in RODS:
        elements.append(box((ROD_X0, y - 0.25, z - 0.25), (ROD_X1, y + 0.25, z + 0.25), "rod"))
        for x0, x1 in ((END_PLATES[0][0] - 0.75, END_PLATES[0][0]), (END_PLATES[1][1], END_PLATES[1][1] + 0.75)):
            elements.append(box((x0, y - 0.5, z - 0.5), (x1, y + 0.5, z + 0.5), "bolt"))
    # Stiffening ribs across the outer face of each end plate.
    for x0, x1 in ((END_PLATES[0][0] - 0.5, END_PLATES[0][0]), (END_PLATES[1][1], END_PLATES[1][1] + 0.5)):
        for y0 in (4.5, 9.75):
            elements.append(box((x0, y0, 1.75), (x1, y0 + 0.75, 9.25), "end_plate", faces=FACES[:5]))
    # Stack voltmeter on the outer face of the left end plate, between the ribs.
    elements.append(box((1.5, 6.25, 4.25), (2.0, 8.75, 6.75), {"west": "gauge", "*": "casing"}, uv={"west": FULL}))
    # Copper bus bars: a tab off each collector, across, and back into the cabinet.
    for (cx0, cx1), (bx0, bx1) in zip(COLLECTORS, ((4.5, 5.5), (10.5, 11.5))):
        elements.append(box((cx0, COLLECTOR_Y1, 4.5), (cx1, 13.25, 5.5), "copper", faces=("north", "south", "east", "west")))
        elements.append(box((min(cx0, bx0), 13.25, 4.5), (max(cx1, bx1), 13.75, 5.5), "copper"))
        elements.append(box((bx0, 13.25, 5.5), (bx1, 13.75, CABINET[0][2]), "copper", faces=("east", "west", "up", "down")))
    # Power-conditioning cabinet with a teal band, heat-sink fins, a status lamp, and the cable port.
    elements.append(box(*CABINET, {"north": "cabinet", "south": "cabinet", "*": "casing"}))
    elements.append(box((3.75, 12.5, 10.75), (12.25, 13.0, 15.75), "band", faces=SIDES))
    for z in (11.75, 12.75, 13.75, 14.75):
        elements.append(box((4.5, CABINET[1][1], z - 0.25), (11.5, CABINET[1][1] + 1.0, z + 0.25), "flange", faces=("north", "south", "east", "west", "up")))
    elements.append(box(
        (7.25, 13.25, 10.5), (8.75, 14.25, CABINET[0][2]), "lamp",
        faces=("north", "east", "west", "up", "down"),
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))
    # Ammonia tank, with a fill fitting on its lid, and the feed line into the left end plate.
    (tx0, _, tz0), (tx1, ty1, tz1) = TANK
    elements += framed_glass(TANK, END_Y0, ty1 + 0.75)
    elements.append(box((1.75, ty1 + 0.75, 13.0), (2.75, ty1 + 1.5, 14.0), "pipe", faces=SIDES + ("up",)))
    elements.append(box((2.0, END_Y0, END_Z1), (3.0, 3.0, tz0 - 0.25), "pipe", faces=("east", "west", "up")))
    elements.append(box((1.875, 3.0, 10.625), (3.125, 3.5, 11.125), "bolt", faces=SIDES + ("up",)))
    # Exhaust from the right end plate down into the residue trap, which vents through a stack.
    (_, _, rz0), (_, ry1, _) = TRAP
    lid = ry1 + 0.75
    elements += framed_glass(TRAP, END_Y0, lid)
    elements.append(box((13.0, 10.0, END_Z1), (14.0, 11.0, 12.5), "pipe", faces=("east", "west", "up", "down")))
    elements.append(box((13.0, lid, 12.5), (14.0, 11.0, 13.5), "pipe", faces=("south", "east", "west", "up")))
    elements.append(box(VENT[0], VENT[1], "pipe", faces=SIDES))
    (vx0, _, vz0), (vx1, vy1, vz1) = VENT
    elements.append(box((vx0 - 0.25, vy1, vz0 - 0.25), (vx1 + 0.25, vy1 + 0.5, vz1 + 0.25), "cap"))
    return elements


def membrane_elements():
    """Static membranes for the item model, which has no renderer to draw them."""
    uv = {"north": [0, 0, 1, 16], "south": [0, 0, 1, 16], "up": [0, 0, 1, 16]}
    p = MEMBRANE_PROUD
    return [box((x0, PLATE_Y0, PLATE_Z0 - p), (x1, PLATE_Y1 + p, PLATE_Z1 + p), "membrane", faces=("north", "south", "up"), uv=uv)
            for x0, x1 in gaps()]


HOUSING_TEXTURES = {
    "particle": TEX + "end_plate",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "end_plate": TEX + "end_plate",
    "plate": TEX + "plate",
    "copper": TEX + "copper",
    "rod": TEX + "rod",
    "bolt": TEX + "bolt",
    "flange": TEX + "flange",
    "cap": TEX + "cap",
    "band": TEX + "band",
    "glass": TEX + "glass",
    "pipe": TEX + "pipe",
    "cabinet": TEX + "cabinet",
    "gauge": TEX + "gauge",
    "port": TEX + "port",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/ammonia_fuel_cell.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/ammonia_fuel_cell_active.json", {
        "parent": "rngtech:block/ammonia_fuel_cell",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/ammonia_fuel_cell.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, "membrane": TEX + "membrane"},
        "elements": membrane_elements() + housing,
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"ammonia_fuel_cell:{name}")


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


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
GRAPHITE = (40, 43, 48)
COPPER = (184, 115, 51)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
SCALE = (222, 226, 210, 255)
# Fuel Cell Membrane item palette: white gasket, green catalyst.
GASKET = (234, 244, 239)
CATALYST = (104, 185, 145)
CATALYST_LIGHT = (181, 229, 166)


def write_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    tread = noisy(64, SLATE_DARK, "plinth_top", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, "plinth_top")

    # Plinth sides: the hazard band sits on the bottom rows, which the 2 px plinth always maps.
    side = noisy(64, SLATE_DARK, "plinth_side", amount=4)
    pixels = side.load()
    for x in range(64):
        pixels[x, 56] = (*shade(SLATE, 26), 255)
        pixels[x, 63] = (*shade(SLATE_DARK, -10), 255)
        for y in range(59, 62):
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)
    save(side, "plinth_side")

    # End plates: heavy brushed steel with seams.
    save(panel_seams(noisy(64, STEEL_BLUE, "end_plate", amount=5, streak=5)), "end_plate")

    # Bipolar plates: graphite, streaked along the rows so thin edges stay even wherever they land.
    plate = noisy(64, GRAPHITE, "plate", amount=4, streak=5)
    pixels = plate.load()
    for y in range(64):
        if y % 4 == 0:
            for x in range(64):
                pixels[x, y] = (*shade(pixels[x, y], -10), 255)
    save(plate, "plate")

    save(noisy(64, COPPER, "copper", amount=7, streak=6), "copper")
    save(noisy(64, (150, 158, 168), "rod", amount=5, streak=8), "rod")
    save(noisy(64, (150, 158, 168), "bolt", amount=8), "bolt")
    save(noisy(64, STEEL, "flange", amount=5), "flange")

    # Lids and collars: dark steel with a bolt circle on every 16-texel tile.
    cap = noisy(64, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for angle in range(0, 360, 60):
                x = px + 8 + round(math.cos(math.radians(angle)) * 5)
                y = py + 8 + round(math.sin(math.radians(angle)) * 5)
                pixels[x % 64, y % 64] = (*shade((150, 158, 168), 10), 255)
    save(cap, "cap")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    # Glass panes map the whole texture, so every pane shows the same glints and a level scale up its left edge.
    glass = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    draw.line([(6, 6), (11, 1)], fill=GLINT)
    draw.line([(7, 9), (9, 7)], fill=GLINT)
    for y in range(1, 16, 2):
        glass.putpixel((0, y), SCALE)
        if y % 4 == 1:
            glass.putpixel((1, y), SCALE)
    save(glass, "glass")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Cabinet: seamed casing with louvred vents.
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

    # Stack voltmeter: a cream dial with a red band at the top of the scale and the needle in the green.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=SCALE)
    for angle in range(-210, 31, 30):
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        color = (196, 52, 40, 255) if angle >= 0 else (40, 46, 56, 255)
        gauge.putpixel((int(round(x)), int(round(y))), color)
    draw.arc([4, 4, 11, 11], -150, -60, fill=(90, 170, 70, 255))
    draw.line([(7.5, 7.5), (10.0, 4.0)], fill=(196, 52, 40, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    gauge.putpixel((7, 12), (40, 46, 56, 255))
    save(gauge, "gauge")

    # Cable port: a teal-ringed plate with a socket and four contacts, mapped whole onto the port plate.
    port = noisy(64, SLATE, "port", amount=4)
    draw = ImageDraw.Draw(port)
    draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
    draw.rectangle([6, 6, 57, 57], outline=(*TEAL_DARK, 255), width=3)
    draw.rectangle([8, 8, 55, 55], outline=(*shade(TEAL, 10), 255), width=1)
    for x, y in ((3, 3), (58, 3), (3, 58), (58, 58)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
    draw.rectangle([20, 20, 43, 43], fill=(14, 18, 24, 255), outline=(98, 108, 120, 255))
    for x, y in ((24, 24), (37, 24), (24, 37), (37, 37)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(*COPPER, 255))
    save(port, "port")

    save(noisy(64, (18, 52, 34), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (128, 240, 120), "lamp_on", amount=6), "lamp_on")

    # Membrane edge, mapped whole along its length: white gasket at both ends, green catalyst between.
    membrane = Image.new("RGBA", (16, 16))
    pixels = membrane.load()
    rand = rng("membrane")
    for y in range(16):
        for x in range(16):
            if y < 2 or y > 13:
                color = shade(GASKET, rand.uniform(-6, 4))
            elif y in (2, 13):
                color = shade(CATALYST_LIGHT, rand.uniform(-6, 6))
            else:
                color = shade(CATALYST, rand.uniform(-10, 10))
            pixels[x, y] = (*color, 255)
    save(membrane, "membrane")

    # Renderer texture: Corrosion Residue settled in the trap.
    residue = noisy(16, (70, 92, 40), "residue", amount=12)
    pixels = residue.load()
    rand = rng("residue_flecks")
    for _ in range(20):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (*shade((118, 146, 62), rand.uniform(-12, 12)), 255)
    save(residue, "fx/residue")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
