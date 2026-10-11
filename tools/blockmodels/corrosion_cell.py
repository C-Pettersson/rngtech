#!/usr/bin/env python3
"""Generate the Corrosion Cell block models and owned textures.

The cell is an aluminium-air style bath: a glass tank of electrolyte with the fuel plate hanging from a copper bus bar,
the air cathode as the tank's back wall over an air plenum, and a blower on top feeding it. Automation works on every
face; the one visible connector port sits on the back, centred where a Universal Connector meets it. The housing is a
static block model; the fuel, Cathode, electrolyte, sludge, bubbles, reservoir level, ammeter needle, and blower fan are
drawn by CorrosionCellRenderer, whose constants match the geometry here. Run from the repository root:

    python tools/blockmodels/corrosion_cell.py
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
TEXTURES = ASSETS / "textures/block/corrosion_cell"
TEX = "rngtech:block/corrosion_cell/"
FACES = ("north", "south", "east", "west", "up", "down")
TAN = math.tan(math.radians(22.5))

# Blower fan on top of the plenum housing, spinning about a vertical axis through (FAN_X, FAN_Z).
FAN_X, FAN_Z = 8.0, 13.0
SHROUD_OUTER, SHROUD_INNER = 3.0, 2.3
FAN_RADIUS = 2.1
# Ammeter dial on the front, needle pivot at (DIAL_X, DIAL_Y).
DIAL_X, DIAL_Y = 1.75, 3.25

# Connector port: the Universal Connector's contact ring is 8x8 (4-12) centred on the face; the port adds a 1 px border.
PORT_MIN, PORT_MAX = 3.0, 13.0
SOCKET_MIN, SOCKET_MAX = 5.0, 11.0
FRAME_DEPTH = 1.0
SOCKET_DEPTH = 0.5
PIN_POSITIONS = ((6.25, 6.25), (9.25, 6.25), (6.25, 9.25), (9.25, 9.25))
PIN_SIZE = 0.5


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, rotation=None, uv=None, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map with an optional "*" default."""
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


def on_face(face, u0, v0, u1, v1, d0, d1):
    """Corners of a box given in face coordinates: (u, v) across the face, d inward from the block boundary."""
    if face == "north":
        return (u0, v0, d0), (u1, v1, d1)
    if face == "south":
        return (u0, v0, 16 - d1), (u1, v1, 16 - d0)
    if face == "west":
        return (d0, v0, u0), (d1, v1, u1)
    if face == "east":
        return (16 - d1, v0, u0), (16 - d0, v1, u1)
    if face == "down":
        return (u0, d0, v0), (u1, d1, v1)
    if face == "up":
        return (u0, 16 - d1, v0), (u1, 16 - d0, v1)
    raise ValueError(face)


def port(face, backing):
    """
    A connector port flush with `face`: a dark slate frame with a teal ring, a recessed socket with copper pins, and a
    backing block that reaches the machine body `backing` px in, so the connector always plugs into something solid.
    """
    elements = []
    lo, hi, slo, shi = PORT_MIN, PORT_MAX, SOCKET_MIN, SOCKET_MAX
    for u0, v0, u1, v1 in ((lo, lo, hi, slo), (lo, shi, hi, hi), (lo, slo, slo, shi), (shi, slo, hi, shi)):
        elements.append(box(*on_face(face, u0, v0, u1, v1, 0, FRAME_DEPTH), "port"))
    socket_back = min(backing, SOCKET_DEPTH + FRAME_DEPTH)
    elements.append(box(*on_face(face, slo, slo, shi, shi, SOCKET_DEPTH, socket_back), "port"))
    for u, v in PIN_POSITIONS:
        elements.append(box(*on_face(face, u, v, u + PIN_SIZE, v + PIN_SIZE, SOCKET_DEPTH - 0.3, SOCKET_DEPTH), "port",
                            uv={f: [0, 0, 1, 1] for f in FACES}))
    if backing > FRAME_DEPTH:
        elements.append(box(*on_face(face, lo, lo, hi, hi, FRAME_DEPTH, backing), "casing"))
    return elements


def octagon_ring_flat(cx, cz, outer, inner, y0, y1, textures):
    """A flat octagonal ring lying in the horizontal plane around a vertical axis through (cx, cz)."""
    half = outer * TAN
    depth = outer - inner
    elements = [
        box((cx - half, y0, cz + inner), (cx + half, y1, cz + outer), textures),
        box((cx - half, y0, cz - outer), (cx + half, y1, cz - inner), textures),
        box((cx + inner, y0, cz - half), (cx + outer, y1, cz + half), textures),
        box((cx - outer, y0, cz - half), (cx - inner, y1, cz + half), textures),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = cx + sx * mid, cz + sz * mid
            if sx * sz > 0:
                start, end = (x - half, y0, z - depth / 2), (x + half, y1, z + depth / 2)
            else:
                start, end = (x - depth / 2, y0, z - half), (x + depth / 2, y1, z + half)
            elements.append(box(start, end, textures, rotation={"origin": [r(x), r((y0 + y1) / 2), r(z)], "axis": "y", "angle": 45}))
    return elements


def housing_elements():
    elements = []
    # Plinth with the family's teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))

    # Electrolyte bath: sludge-trap tray, corner posts, teal rim, glass on three sides, air plenum behind.
    elements.append(box((2.5, 2, 2), (13.5, 3, 10), "tray"))
    for x0 in (2.5, 12.5):
        for z0 in (2.0, 9.0):
            elements.append(box((x0, 3, z0), (x0 + 1, 13, z0 + 1), "frame"))
    elements.append(box((2.5, 13, 2), (13.5, 14, 3), "band"))
    elements.append(box((2.5, 13, 9), (13.5, 14, 10), "band"))
    elements.append(box((2.5, 13, 3), (3.5, 14, 9), "band"))
    elements.append(box((12.5, 13, 3), (13.5, 14, 9), "band"))
    elements.append(box((3.5, 3, 2.5), (12.5, 13, 2.5), "glass", faces=("north", "south")))
    elements.append(box((3, 3, 3), (3, 13, 9), "glass", faces=("east", "west")))
    elements.append(box((13, 3, 3), (13, 13, 9), "glass", faces=("east", "west")))
    elements.append(box((3.5, 3, 9), (12.5, 13, 10), {"north": "plenum", "*": "casing"}))

    # Air plenum housing behind the bath, carrying the back port, with the blower on top.
    elements.append(box((3.5, 2, 10), (12.5, 13, 14), "motor"))
    elements.append(box((3.5, 13, 10), (12.5, 15, 16), {"up": "intake", "*": "motor"}))
    elements += octagon_ring_flat(FAN_X, FAN_Z, SHROUD_OUTER, SHROUD_INNER, 15.0, 16.0, "shroud")
    elements.append(box((FAN_X - 0.25, 15.75, FAN_Z - SHROUD_INNER), (FAN_X + 0.25, 16.0, FAN_Z + SHROUD_INNER), "pipe"))
    elements.append(box((FAN_X - SHROUD_INNER, 15.75, FAN_Z - 0.25), (FAN_X + SHROUD_INNER, 16.0, FAN_Z + 0.25), "pipe"))

    # Anode bus: copper bar on teal-banded insulators, the clamp the fuel hangs from, and the strap to the terminal box.
    for x0 in (2.5, 12.5):
        elements.append(box((x0, 14, 4.5), (x0 + 1, 15, 6.5), "insulator"))
    elements.append(box((2.75, 15, 5), (15, 15.75, 6), "bus"))
    elements.append(box((7, 13.5, 4.5), (9, 15, 6.5), "clamp"))
    elements.append(box((14.25, 7, 5), (15, 15, 6), "bus"))
    elements.append(box((13.75, 2, 3), (15.75, 7, 8), "terminal"))

    # Electrolyte reservoir on the west side, with its feed pipe over the rim into the bath.
    elements.append(box((0.25, 2, 5.5), (2.75, 3.5, 9), "pipe"))
    elements.append(box((0.25, 12, 5.5), (2.75, 13.5, 9), "pipe"))
    elements.append(box((0.5, 3.5, 5.75), (2.5, 12, 8.75), "glass_tube", faces=("north", "south", "east", "west")))
    for x0 in (0.4, 2.35):
        for z0 in (5.65, 8.6):
            elements.append(box((x0, 3.5, z0), (x0 + 0.25, 12, z0 + 0.25), "pipe", faces=("north", "south", "east", "west")))
    elements.append(box((1, 13.5, 6.75), (2, 14.5, 7.75), "pipe", faces=("north", "south", "east", "west")))
    elements.append(box((1, 14.5, 6.75), (4.75, 15.25, 7.75), "pipe"))
    elements.append(box((4, 12.5, 6.75), (4.75, 14.5, 7.75), "pipe", faces=("north", "south", "east", "west", "down")))

    # Hydrogen vent stack beside the blower, piped from the bath headspace.
    elements.append(box((2.25, 13.25, 9.75), (3.25, 14.25, 11), "pipe", faces=("east", "west", "up", "down", "north")))
    elements.append(box((1.75, 2, 11), (3.25, 15.25, 12.5), "pipe"))
    elements.append(box((1.5, 15.25, 10.75), (3.5, 15.75, 12.75), "vent_cap"))

    # Ammeter and status lamp on the front.
    elements.append(box((0.5, 2, 0.5), (3, 4.5, 1.75), {"north": "gauge", "*": "casing"}, uv={"north": [0, 0, 16, 16]}))
    elements.append(box(
        (13.25, 2, 0.75), (15.25, 3, 1.75), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))

    # The one visible connector port, on the back; automation itself works on every face.
    elements += port("south", 2.0)
    return elements


def item_parts():
    """A fresh aluminium-looking anode and Cathode, so the item shows a loaded cell."""
    return [
        box((3.75, 3.25, 8.2), (12.25, 12.75, 8.7), "cathode"),
        box((5, 4, 4.75), (11, 12, 6.25), "anode"),
        box((7.25, 12, 5.1), (8.75, 13.5, 5.9), "anode"),
    ]


def fan_elements():
    """Four pitched blades and a hub, lying flat around the vertical fan axis."""
    elements = []
    y0, y1 = 15.15, 15.45
    yc = (y0 + y1) / 2
    width = 0.7
    blades = [
        ((FAN_X + 0.6, y0, FAN_Z - width), (FAN_X + FAN_RADIUS, y1, FAN_Z + width), "x", 22.5),
        ((FAN_X - FAN_RADIUS, y0, FAN_Z - width), (FAN_X - 0.6, y1, FAN_Z + width), "x", -22.5),
        ((FAN_X - width, y0, FAN_Z + 0.6), (FAN_X + width, y1, FAN_Z + FAN_RADIUS), "z", -22.5),
        ((FAN_X - width, y0, FAN_Z - FAN_RADIUS), (FAN_X + width, y1, FAN_Z - 0.6), "z", 22.5),
    ]
    for start, end, axis, angle in blades:
        origin = [r((start[0] + end[0]) / 2), r(yc), r((start[2] + end[2]) / 2)]
        elements.append(box(start, end, "blade", rotation={"origin": origin, "axis": axis, "angle": angle}))
    hub = 0.7
    elements.append(box((FAN_X - hub, 15.0, FAN_Z - hub), (FAN_X + hub, 15.6, FAN_Z + hub), "hub"))
    elements.append(box((FAN_X - hub, 15.0, FAN_Z - hub), (FAN_X + hub, 15.6, FAN_Z + hub), "hub",
                        rotation={"origin": [FAN_X, 15.3, FAN_Z], "axis": "y", "angle": 45}))
    return elements


HOUSING_TEXTURES = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "tray": TEX + "tray",
    "frame": TEX + "frame",
    "band": TEX + "band",
    "glass": TEX + "glass",
    "glass_tube": TEX + "glass_tube",
    "plenum": TEX + "plenum",
    "insulator": TEX + "insulator",
    "bus": TEX + "bus",
    "clamp": TEX + "clamp",
    "terminal": TEX + "terminal",
    "pipe": TEX + "pipe",
    "motor": TEX + "motor",
    "intake": TEX + "intake",
    "shroud": TEX + "shroud",
    "vent_cap": TEX + "vent_cap",
    "gauge": TEX + "gauge",
    "port": TEX + "port",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/corrosion_cell.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/corrosion_cell_active.json", {
        "parent": "rngtech:block/corrosion_cell",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "block/corrosion_cell/fan.json", {
        "textures": {"particle": "#blade", "blade": TEX + "fan_blade", "hub": TEX + "fan_hub"},
        "elements": fan_elements(),
    })
    write_json(MODELS / "item/corrosion_cell.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            **HOUSING_TEXTURES,
            "anode": TEX + "anode",
            "cathode": TEX + "cathode",
            "blade": TEX + "fan_blade",
            "hub": TEX + "fan_hub",
        },
        "elements": housing + item_parts() + fan_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"corrosion_cell:{name}")


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


def rings(image, center, spacing, amount, start=4):
    pixels = image.load()
    size = image.size[0]
    cx, cy = center
    for y in range(size):
        for x in range(size):
            distance = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if distance > start and int(distance) % spacing == 0:
                pixels[x, y] = (*shade(pixels[x, y], amount), 255)
    return image


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL_BLUE = (82, 96, 112)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
COPPER = (184, 115, 51)
# Top-face UV of the fan axis: u = x, v = z, at 4 px per unit.
FAN_PX = (FAN_X * 4, FAN_Z * 4)


def hazard_rows(image, rows):
    pixels = image.load()
    for x in range(image.size[0]):
        for y in rows:
            stripe = ((x + y) // 3) % 3 == 0
            pixels[x, y] = (*shade(TEAL_DARK if stripe else SLATE_DARK, 8 if stripe else 0), 255)


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
    hazard_rows(side, range(59, 62))
    save(side, "plinth_side")

    # The tray's side faces cover model y 2-3, texture rows 52-56: a hazard stripe marks the sludge trap.
    tray = noisy(64, SLATE_DARK, "tray", amount=4)
    hazard_rows(tray, range(53, 56))
    save(tray, "tray")

    frame = noisy(64, (40, 47, 58), "frame", amount=4)
    pixels = frame.load()
    for y in range(64):
        for x in range(64):
            if x % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 16), 255)
            elif x % 4 == 3:
                pixels[x, y] = (*shade(pixels[x, y], -12), 255)
    save(frame, "frame")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    # Bath glass: short glints in the upper corners of each pane, the rest see-through (cutout).
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for x0, y0 in ((17, 15), (40, 15)):
        draw.line([(x0, y0 + 8), (x0 + 8, y0)], fill=(206, 240, 250, 255))
        draw.line([(x0 + 1, y0 + 12), (x0 + 4, y0 + 9)], fill=(206, 240, 250, 255))
    save(glass, "glass")
    tube = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = tube.load()
    for x in (6, 26, 33, 59):
        for y in range(18, 46):
            if y % 7 != 0:
                pixels[x, y] = (206, 240, 250, 255)
    save(tube, "glass_tube")

    # Air plenum behind the Cathode: dark perforated plate, seen through the Cathode mesh.
    plenum = noisy(64, (16, 22, 30), "plenum", amount=3)
    pixels = plenum.load()
    for y in range(64):
        for x in range(64):
            if x % 4 == 1 and y % 4 == 1:
                pixels[x, y] = (*shade(TEAL_DARK, -20), 255)
    save(plenum, "plenum")

    # Fuel and Cathode are greyscale; the renderer tints them by material.
    anode = noisy(64, (206, 206, 206), "anode", amount=10, streak=4)
    pixels = anode.load()
    rand = rng("anode_pits")
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], -48), 255)
        if x + 1 < 64:
            pixels[x + 1, y] = (*shade(pixels[x + 1, y], -24), 255)
    save(anode, "anode")

    cathode = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = cathode.load()
    rand = rng("cathode")
    left, right, top, bottom = 15, 48, 13, 50
    for y in range(64):
        for x in range(64):
            edge = x in (left, left + 1, right - 1, right) or y in (top, top + 1, bottom - 1, bottom)
            wire = (x - left) % 4 in (0, 1) or (y - top) % 4 in (0, 1)
            if edge or wire:
                value = 210 + rand.uniform(-12, 12) + (14 if edge else 0)
                pixels[x, y] = (clamp(value), clamp(value), clamp(value), 255)
    save(cathode, "cathode")

    save(noisy(64, (150, 158, 168), "clamp", amount=8), "clamp")

    bus = noisy(64, COPPER, "bus", amount=7)
    pixels = bus.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 26), 255)
            elif y % 4 == 3:
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
    save(bus, "bus")

    pipe = noisy(64, (140, 150, 162), "pipe", amount=6)
    pixels = pipe.load()
    for y in range(64):
        for x in range(64):
            if x % 16 in (6, 7):
                pixels[x, y] = (*shade(TEAL, -10), 255)
    save(pipe, "pipe")

    motor = noisy(64, (52, 60, 72), "motor", amount=4)
    pixels = motor.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
    save(motor, "motor")

    insulator = noisy(64, (218, 212, 196), "insulator", amount=5)
    pixels = insulator.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 10), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
    save(insulator, "insulator")

    terminal = panel_seams(noisy(64, (34, 40, 48), "terminal", amount=4), rivets=False)
    draw = ImageDraw.Draw(terminal)
    for top in (10, 30):
        draw.rectangle([18, top, 45, top + 6], fill=(*shade(COPPER, -10), 255), outline=(*shade(COPPER, 30), 255))
        draw.rectangle([28, top + 2, 35, top + 4], fill=(*TEAL, 255))
    save(terminal, "terminal")

    intake = noisy(64, (14, 20, 30), "intake", amount=3)
    rings(intake, FAN_PX, 5, 10)
    save(intake, "intake")
    save(rings(noisy(64, (98, 108, 120), "shroud", amount=5), FAN_PX, 3, -10), "shroud")

    blade = noisy(64, (170, 178, 188), "fan_blade", amount=7)
    pixels = blade.load()
    for y in range(64):
        for x in range(64):
            angle = math.atan2(y + 0.5 - FAN_PX[1], x + 0.5 - FAN_PX[0])
            if int(math.degrees(angle) * 2) % 9 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
    save(blade, "fan_blade")
    hub = noisy(64, (58, 64, 72), "fan_hub", amount=5)
    ImageDraw.Draw(hub).ellipse([FAN_PX[0] - 2.5, FAN_PX[1] - 2.5, FAN_PX[0] + 2.5, FAN_PX[1] + 2.5], fill=(*TEAL, 255))
    save(hub, "fan_hub")

    cap = noisy(64, (60, 68, 80), "vent_cap", amount=5)
    pixels = cap.load()
    for y in range(64):
        for x in range(64):
            if y % 8 in (0, 1):
                pixels[x, y] = (*shade(TEAL_DARK, 6), 255)
    save(cap, "vent_cap")

    # Ammeter face: a 120 degree scale over the top, with the rated end marked teal; the needle is drawn live.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    for step in range(5):
        angle = math.radians(-150 + step * 30)
        x, y = 7.5 + math.cos(angle) * 4.6, 7.5 + math.sin(angle) * 4.6
        gauge.putpixel((int(round(x)), int(round(y))), (40, 40, 40, 255))
    end = math.radians(-30)
    gauge.putpixel((int(round(7.5 + math.cos(end) * 4.6)), int(round(7.5 + math.sin(end) * 4.6))), (*TEAL_DARK, 255))
    gauge.putpixel((int(round(7.5 + math.cos(end) * 3.6)), int(round(7.5 + math.sin(end) * 3.6))), (*TEAL, 255))
    gauge.putpixel((7, 8), (30, 30, 30, 255))
    gauge.putpixel((8, 8), (30, 30, 30, 255))
    save(gauge, "gauge")

    # Connector port, mapped by position: frame over texels 12-52 (model 3-13), teal ring just inside its outer edge,
    # socket over 20-44 (model 5-11), and a copper patch in the corner for the pins.
    port = noisy(64, (24, 29, 36), "port", amount=3)
    draw = ImageDraw.Draw(port)
    draw.rectangle([12, 12, 51, 51], fill=(*SLATE_DARK, 255))
    draw.rectangle([12, 12, 51, 51], outline=(*shade(SLATE, 22), 255))
    draw.rectangle([14, 14, 49, 49], outline=(*TEAL, 255))
    draw.rectangle([15, 15, 48, 48], outline=(*TEAL_DARK, 255))
    draw.rectangle([20, 20, 43, 43], fill=(12, 15, 20, 255), outline=(6, 8, 11, 255))
    pixels = port.load()
    rand = rng("port_socket")
    for y in range(21, 43):
        for x in range(21, 43):
            pixels[x, y] = (*shade(pixels[x, y], rand.uniform(-3, 3)), 255)
    for x in range(4):
        for y in range(4):
            pixels[x, y] = (*shade(COPPER, 30 if x + y < 3 else -10), 255)
    save(port, "port")

    save(noisy(64, (18, 52, 58), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (110, 236, 255), "lamp_on", amount=6), "lamp_on")

    # Settled hydroxide sludge: pale, lumpy, and opaque.
    sludge = noisy(64, (178, 176, 164), "sludge", amount=14)
    pixels = sludge.load()
    rand = rng("sludge_lumps")
    for _ in range(140):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], rand.choice((-30, 24))), 255)
    save(sludge, "sludge")

    bubble = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bubble)
    draw.ellipse([2, 2, 13, 13], fill=(220, 245, 250, 70), outline=(240, 252, 255, 220))
    draw.point([(5, 5), (6, 5), (5, 6)], fill=(255, 255, 255, 255))
    save(bubble, "fx/bubble")
    save(Image.new("RGBA", (16, 16), (255, 255, 255, 255)), "fx/needle")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
