#!/usr/bin/env python3
"""Generate the Potential Reactor block model, item model, and owned textures.

The design is a containment reactor: a heavy vessel with a framed viewport on the front, a feed hopper on the lid
(fuel goes in from the top), and two electrodes inside that hold the reaction between them. The installed Gear sits
where it works: the Containment Lining is a sleeve inside the vessel, the Reactor Chamber is the electrode pair, and the
Recovery Filter is a cartridge in an open cage on the residue path, which drops into a framed residue bin above the
bottom face. A sight glass on the west side shows the FE buffer, a front dial shows stability, and the power cabinet
on the back carries the cable port. The housing is a static block model; the lining, electrodes, core glow, filter
cartridge, residue, charge column, and dial needle are drawn by PotentialReactorRenderer, whose constants match the
geometry here. Run from the repository root:

    python tools/blockmodels/potential_reactor.py
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
TEXTURES = ASSETS / "textures/block/potential_reactor"
TEX = "rngtech:block/potential_reactor/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# Containment vessel; the front faces north (z = 0). Walls are one pixel thick.
VESSEL = ((3.0, 2.0, 2.0), (13.0, 13.0, 12.0))
# Viewport opening in the front wall, and the core centre between the electrodes.
VIEW = ((5.0, 4.5), (11.0, 10.5))
CORE = (8.0, 7.5, 7.0)
# Recovery Filter cage on the east side, above the residue bin.
FILTER = ((14.1, 7.0, 3.1), (15.4, 10.5, 5.9))
BIN = ((13.75, 2.0, 7.0), (15.75, 5.25, 11.25))
BIN_BASE = 0.5
POST = 0.5
# Charge sight glass on the west side.
SIGHT = ((0.5, 3.5, 5.0), (2.5, 11.5, 7.5))
# Stability dial and status lamp on the plinth front.
DIAL = ((0.75, 2.0, 0.25), (3.25, 4.5, 1.25))
LAMP = ((12.75, 2.0, 0.5), (14.25, 3.0, 1.5))
# Cable port on the back of the power cabinet, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))

FACE_PLANES = {"north": (2, 0.0), "south": (2, 16.0), "west": (0, 0.0), "east": (0, 16.0), "down": (1, 0.0), "up": (1, 16.0)}


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, rotation=None, uv=None, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map; faces on the block boundary cull."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is None:
            continue
        entry = {"texture": f"#{key}"}
        if uv and face in uv:
            entry["uv"] = uv[face]
        axis, plane = FACE_PLANES[face]
        if rotation is None and (end if plane else start)[axis] == plane:
            entry["cullface"] = face
        element["faces"][face] = entry
    if rotation:
        element["rotation"] = rotation
    if extra:
        element.update(extra)
    return element


# Sight-glass faces map a framed strip of the texture at 8 texels per pixel: the narrow (2 px) north and south faces use
# columns 0-15, the wider (2.5 px) east and west faces use columns 32-51.
SIGHT_UV = {
    "north": [0, 0, 4, 16],
    "south": [0, 0, 4, 16],
    "east": [8, 0, 13, 16],
    "west": [8, 0, 13, 16],
}
FULL = [0, 0, 16, 16]


def vessel_elements():
    (x0, y0, z0), (x1, y1, z1) = VESSEL
    (vx0, vy0), (vx1, vy1) = VIEW
    shell = {"*": "vessel"}
    elements = [
        # Side and back walls show the scorched interior on their inner faces.
        box((x0, y0, z0 + 1), (x0 + 1, y1, z1 - 1), {**shell, "east": "interior"}),
        box((x1 - 1, y0, z0 + 1), (x1, y1, z1 - 1), {**shell, "west": "interior"}),
        box((x0, y0, z1 - 1), (x1, y1, z1), {**shell, "north": "interior"}),
        box((x0 + 1, y0, z0 + 1), (x1 - 1, y0 + 1, z1 - 1), "interior", faces=("up",)),
        box((x0 + 1, y1 - 1, z0 + 1), (x1 - 1, y1, z1 - 1), "interior", faces=("down",)),
    ]
    # Front wall around the viewport.
    front = {**shell, "south": "interior"}
    elements += [
        box((x0, y0, z0), (vx0, y1, z0 + 1), front),
        box((vx1, y0, z0), (x1, y1, z0 + 1), front),
        box((vx0, y0, z0), (vx1, vy0, z0 + 1), front),
        box((vx0, vy1, z0), (vx1, y1, z0 + 1), front),
    ]
    # Viewport bezel, its four bolts, and the glass.
    bz0, bz1 = z0 - 0.75, z0
    elements += [
        box((vx0 - 1, vy0 - 1, bz0), (vx1 + 1, vy0, bz1), "frame"),
        box((vx0 - 1, vy1, bz0), (vx1 + 1, vy1 + 1, bz1), "frame"),
        box((vx0 - 1, vy0, bz0), (vx0, vy1, bz1), "frame"),
        box((vx1, vy0, bz0), (vx1 + 1, vy1, bz1), "frame"),
    ]
    for bx in (vx0 - 0.75, vx1 + 0.25):
        for by in (vy0 - 0.75, vy1 + 0.25):
            elements.append(box((bx, by, bz0 - 0.25), (bx + 0.5, by + 0.5, bz0), "bolt"))
    elements.append(box((vx0, vy0, z0 + 0.4), (vx1, vy1, z0 + 0.6), "viewport", faces=("north", "south"),
                        uv={"north": FULL, "south": FULL}))
    # Corner ribs, proud of the teal containment bands.
    for rx in (x0 - 0.5, x1 - 0.5):
        for rz in (z0 - 0.5, z1 - 0.5):
            elements.append(box((rx, y0, rz), (rx + 1, y1, rz + 1), "rib"))
    for by in (2.5, 11.75):
        elements.append(box((x0 - 0.25, by, z0 - 0.25), (x1 + 0.25, by + 0.75, z1 + 0.25), "band", faces=SIDES))
    # Lid with the feed hopper (fuel goes in from the top) and a relief valve.
    elements.append(box((x0 - 0.5, y1, z0 - 0.5), (x1 + 0.5, y1 + 1, z1 + 0.5), "cap"))
    elements.append(box((6.75, 14.0, 5.75), (9.25, 14.75, 8.25), "hopper", faces=SIDES))
    elements.append(box((5.75, 14.75, 4.75), (10.25, 15.75, 9.25), {"up": "hopper_top", "*": "hopper"},
                        uv={"up": FULL}))
    elements.append(box((3.5, 14.0, 9.75), (5.0, 15.25, 11.25), "pipe", faces=SIDES))
    elements.append(box((3.25, 15.25, 9.5), (5.25, 15.75, 11.5), "flange"))
    return elements


def residue_path_elements():
    """The outlet, the Recovery Filter cage, and the residue bin, on the east side."""
    (fx0, fy0, fz0), (fx1, fy1, fz1) = FILTER
    elements = [
        # Outlet from the vessel into the top of the cage.
        box((13.0, 10.75, 4.0), (13.6, 11.5, 5.0), "pipe", faces=("north", "south", "up", "down")),
        # Caps and four corner posts; the cartridge between them is drawn by the renderer.
        box((fx0 - POST, fy1, fz0 - POST), (fx1 + POST, fy1 + 1.0, fz1 + POST), "flange"),
        box((fx0 - POST, fy0 - 1.0, fz0 - POST), (fx1 + POST, fy0, fz1 + POST), "flange"),
    ]
    for px in (fx0 - POST, fx1):
        for pz in (fz0 - POST, fz1):
            elements.append(box((px, fy0, pz), (px + POST, fy1, pz + POST), "rib"))
    # Chute from the bottom cap back into the bin lid.
    elements.append(box((14.4, 6.1, fz1 + POST), (15.1, 6.8, 8.5), "pipe", faces=("east", "west", "up", "down")))
    elements.append(box((14.4, 5.75, 7.8), (15.1, 6.1, 8.5), "pipe", faces=("north", "south", "east", "west")))
    # Residue bin: glass walls between a base, corner posts, and a lid.
    (bx0, by0, bz0), (bx1, by1, bz1) = BIN
    elements.append(box((bx0 - 0.1, by0, bz0 - 0.1), (bx1 + 0.1, by0 + BIN_BASE, bz1 + 0.1), "flange"))
    elements.append(box((bx0 + 0.1, by0 + BIN_BASE, bz0 + 0.1), (bx1 - 0.1, by1, bz1 - 0.1), "bin_glass", faces=SIDES))
    for px in (bx0, bx1 - POST):
        for pz in (bz0, bz1 - POST):
            elements.append(box((px, by0 + BIN_BASE, pz), (px + POST, by1, pz + POST), "flange"))
    elements.append(box((bx0 - 0.1, by1, bz0 - 0.1), (bx1 + 0.1, by1 + 0.5, bz1 + 0.1), "cap"))
    return elements


def instrument_elements():
    elements = []
    # Charge sight glass with collars, wired back to the power cabinet.
    (sx0, sy0, sz0), (sx1, sy1, sz1) = SIGHT
    elements.append(box((sx0 - 0.25, 2.0, sz0 - 0.25), (sx1 + 0.25, sy0, sz1 + 0.25), "pipe"))
    elements.append(box((sx0 - 0.25, sy1, sz0 - 0.25), (sx1 + 0.25, 13.0, sz1 + 0.25), "pipe"))
    elements.append(box((sx0, sy0, sz0), (sx1, sy1, sz1), "sight_glass", faces=SIDES, uv=SIGHT_UV))
    elements.append(box((1.0, 11.75, sz1 + 0.25), (2.0, 12.5, 13.25), "copper", faces=("east", "west", "up", "down")))
    elements.append(box((2.0, 11.75, 12.25), (3.5, 12.5, 13.25), "copper", faces=("north", "south", "up", "down")))
    # Stability dial and status lamp on the plinth front.
    elements.append(box(*DIAL, {"north": "gauge", "*": "casing"}, uv={"north": FULL}))
    elements.append(box(*LAMP, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}))
    return elements


def housing_elements():
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    elements += vessel_elements()
    # Power cabinet behind the vessel, with the cable port on its back and copper busbars over the top.
    elements.append(box((3.5, 2.0, 12.0), (12.5, 12.5, 15.0), {"east": "cabinet", "west": "cabinet", "*": "casing"}))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))
    for bx in (5.0, 10.0):
        elements.append(box((bx, 12.5, 12.0), (bx + 1.0, 13.25, 14.25), "copper", faces=("east", "west", "up", "south")))
    elements += residue_path_elements()
    elements += instrument_elements()
    return elements


def item_elements():
    """Static Gear and core for the item model, which has no renderer to draw them."""
    cx, cy, cz = CORE
    (fx0, fy0, fz0), (fx1, fy1, fz1) = FILTER
    return [
        box((cx - 1.5, 3.0, cz - 1.5), (cx + 1.5, 4.0, cz + 1.5), "electrode"),
        box((cx - 0.75, 4.0, cz - 0.75), (cx + 0.75, 5.0, cz + 0.75), "electrode"),
        box((cx - 1.25, 5.0, cz - 1.25), (cx + 1.25, 5.5, cz + 1.25), "electrode"),
        box((cx - 1.25, 9.5, cz - 1.25), (cx + 1.25, 10.0, cz + 1.25), "electrode"),
        box((cx - 0.75, 10.0, cz - 0.75), (cx + 0.75, 11.0, cz + 0.75), "electrode"),
        box((cx - 1.5, 11.0, cz - 1.5), (cx + 1.5, 12.0, cz + 1.5), "electrode"),
        box((cx - 0.9, cy - 0.9, cz - 0.9), (cx + 0.9, cy + 0.9, cz + 0.9), "core",
            extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}),
        box((fx0, fy0, fz0), (fx1, fy1, fz1), "cartridge"),
    ]


HOUSING_TEXTURES = {
    "particle": TEX + "vessel",
    "casing": TEX + "casing",
    "vessel": TEX + "vessel",
    "interior": TEX + "interior",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "band": TEX + "band",
    "rib": TEX + "rib",
    "frame": TEX + "frame",
    "bolt": TEX + "bolt",
    "viewport": TEX + "viewport",
    "cap": TEX + "cap",
    "hopper": TEX + "hopper",
    "hopper_top": TEX + "hopper_top",
    "flange": TEX + "flange",
    "pipe": TEX + "pipe",
    "copper": TEX + "copper",
    "cabinet": TEX + "cabinet",
    "port": TEX + "port",
    "bin_glass": TEX + "bin_glass",
    "sight_glass": TEX + "sight_glass",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/potential_reactor.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/potential_reactor_active.json", {
        "parent": "rngtech:block/potential_reactor",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/potential_reactor.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            **HOUSING_TEXTURES,
            "electrode": TEX + "fx/electrode",
            "cartridge": TEX + "fx/cartridge",
            "core": TEX + "core_item",
        },
        "elements": item_elements() + housing,
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"potential_reactor:{name}")


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
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
SCALE = (222, 226, 210, 255)
POTENTIAL = (176, 112, 255)


def write_housing_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    # Vessel: heavier plate, horizontal courses with rivet rows instead of a square grid.
    vessel = noisy(64, (42, 49, 60), "vessel", amount=5)
    pixels = vessel.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
            elif y % 16 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
            elif y % 16 == 3 and x % 8 == 4:
                pixels[x, y] = (*shade(pixels[x, y], 36), 255)
                pixels[x, (y + 1) % 64] = (*shade(pixels[x, (y + 1) % 64], -20), 255)
    save(vessel, "vessel")

    # Interior: scorched plate, darker towards the bottom of each course.
    interior = noisy(64, (34, 30, 32), "interior", amount=7)
    pixels = interior.load()
    rand = rng("soot")
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], rand.choice((-14, 18))), 255)
    for y in range(0, 64, 16):
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(interior, "interior")

    tread = noisy(64, SLATE_DARK, "plinth_top", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, "plinth_top")

    # Plinth side: the hazard band sits in the bottom rows, which only the 2 px plinth maps.
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

    save(noisy(64, (88, 98, 110), "rib", amount=5, streak=3), "rib")
    save(noisy(64, (112, 120, 132), "frame", amount=5), "frame")
    save(noisy(64, STEEL, "flange", amount=5), "flange")
    save(noisy(64, (150, 158, 168), "bolt", amount=8), "bolt")
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")
    save(noisy(64, (92, 102, 114), "hopper", amount=5, streak=4), "hopper")

    copper = noisy(64, (184, 115, 51), "copper", amount=8, streak=4)
    save(copper, "copper")

    # Lid: dark steel with a bolt circle on every 16-texel tile.
    cap = noisy(64, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for angle in range(0, 360, 60):
                x = px + 8 + round(math.cos(math.radians(angle)) * 5)
                y = py + 8 + round(math.sin(math.radians(angle)) * 5)
                pixels[x % 64, y % 64] = (*shade((150, 158, 168), 10), 255)
    save(cap, "cap")

    # Hopper mouth: a dark throat behind a bar grate, mapped whole onto the top of the hopper.
    mouth = noisy(64, (92, 102, 114), "hopper_top", amount=4)
    draw = ImageDraw.Draw(mouth)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(STEEL, 20), 255), width=2)
    draw.rectangle([8, 8, 55, 55], fill=(12, 14, 18, 255), outline=(*SLATE_DARK, 255), width=2)
    for x in range(14, 52, 8):
        draw.rectangle([x, 10, x + 2, 53], fill=(*STEEL, 255))
    save(mouth, "hopper_top")

    # Viewport: clear glass with two diagonal glints, mapped whole onto the pane.
    viewport = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(viewport)
    draw.line([(8, 22), (22, 8)], fill=GLINT, width=2)
    draw.line([(12, 30), (30, 12)], fill=GLINT, width=1)
    draw.line([(42, 58), (58, 42)], fill=GLINT, width=1)
    save(viewport, "viewport")

    # Power cabinet: seamed casing with louvred vents across every column, so any face position shows them.
    cabinet = panel_seams(noisy(64, (40, 47, 57), "cabinet", amount=4), rivets=False)
    pixels = cabinet.load()
    for y in range(20, 44):
        for x in range(64):
            if x % 16 in (0, 1):
                continue
            if y % 3 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
            elif y % 3 == 1:
                pixels[x, y] = (*shade(SLATE, 18), 255)
    save(cabinet, "cabinet")

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

    # Residue bin glass: clear with a glint on each face; the corner posts frame it.
    bin_glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bin_glass)
    for offset in (0, 16, 32, 48):
        draw.line([(offset + 6, 40), (offset + 12, 34)], fill=GLINT)
    save(bin_glass, "bin_glass")

    # Charge sight glass: clear between steel edge rails, with level ticks on one rail and a glint. See SIGHT_UV.
    sight = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = sight.load()
    for left, right in ((0, 15), (32, 51)):
        for y in range(64):
            for x in (left, left + 1, right - 1, right):
                edge = x in (left, right)
                pixels[x, y] = (*shade(STEEL, 14 if edge else -12), 255)
            if y % 8 == 4:
                pixels[left + 2, y] = SCALE
                pixels[left + 3, y] = SCALE
            elif y % 16 == 12:
                pixels[left + 2, y] = SCALE
        glint = left + (right - left) * 2 // 3
        for y in range(6, 58):
            if (y // 6) % 3 != 2:
                pixels[glint, y] = GLINT
    save(sight, "sight_glass")

    # Stability dial: a red-to-green arc over the lower half of the sweep; the renderer draws the needle.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    zones = [(-150, (196, 52, 40)), (-120, (214, 120, 40)), (-90, (200, 180, 60)), (-60, (120, 170, 70)),
             (-30, (60, 150, 80))]
    for angle, color in zones:
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    gauge.putpixel((8, 7), (30, 30, 30, 255))
    save(gauge, "gauge")

    save(noisy(64, (18, 52, 34), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (128, 240, 120), "lamp_on", amount=6), "lamp_on")

    # Item-model core: the violet glow of a salvage-fuel run.
    core = Image.new("RGBA", (16, 16))
    pixels = core.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 9)
            pixels[x, y] = (*shade(POTENTIAL, 70 * (1.0 - distance) - 20), 255)
    save(core, "core_item")


def write_renderer_textures() -> None:
    # Gear parts are light greyscale, tinted by the installed material in the renderer.
    lining = noisy(64, (214, 214, 210), "lining", amount=6)
    pixels = lining.load()
    for y in range(64):
        for x in range(64):
            brick = (x + (8 if (y // 8) % 2 else 0)) % 16
            if y % 8 == 0 or brick == 0:
                pixels[x, y] = (*shade(pixels[x, y], -46), 255)
            elif y % 8 == 1 or brick == 1:
                pixels[x, y] = (*shade(pixels[x, y], 16), 255)
    save(lining, "fx/lining")

    electrode = noisy(16, (220, 220, 216), "electrode", amount=6, streak=3)
    pixels = electrode.load()
    for y in range(0, 16, 4):
        for x in range(16):
            pixels[x, y] = (*shade(pixels[x, y], -40), 255)
    save(electrode, "fx/electrode")

    cartridge = noisy(16, (226, 224, 216), "cartridge", amount=5)
    pixels = cartridge.load()
    for y in range(16):
        for x in range(16):
            if x % 3 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -48), 255)
            elif x % 3 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
        if y in (0, 15):
            for x in range(16):
                pixels[x, y] = (*shade((226, 224, 216), -64), 255)
    save(cartridge, "fx/cartridge")

    scrap = noisy(16, (96, 90, 84), "scrap", amount=14)
    pixels = scrap.load()
    rand = rng("scrap_flecks")
    for _ in range(26):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (*shade(rand.choice(((150, 154, 160), (150, 92, 54), (70, 64, 60))), rand.uniform(-8, 8)), 255)
    save(scrap, "fx/scrap")

    charge = noisy(16, (210, 230, 205), "charge", amount=8, streak=6)
    save(charge, "fx/charge")

    # Core glow: a soft radial falloff, drawn additively and tinted by the run.
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)
            strength = (1.0 - distance) ** 1.4
            pixels[x, y] = (clamp(255 * strength), clamp(255 * strength), clamp(255 * strength), 255)
    save(glow, "fx/glow")


def main() -> None:
    write_models()
    write_housing_textures()
    write_renderer_textures()


if __name__ == "__main__":
    main()
