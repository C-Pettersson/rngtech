#!/usr/bin/env python3
"""Generate the Solar Panel and Solar Array Controller block models, blockstates, and owned textures.

Panels are low single-axis trackers on a compact post. The deck fills the 16x16 footprint on a
north-south torque tube and follows the sun (or, in an array that converts moonlight, the moon) while generating;
an idle panel stows flat. Each frame lip is half a bar wide, so flat neighbours share one frame bar and a
north-south row stays one continuous deck and tube while tracking. Every panel blockstate bakes only the mount
(*_mount); SolarPanelRenderer turns the deck (*_deck). The full stepped models (flat, *_east, *_west) are the static
fallback when animation is off in the client config, and the item model. Only the deck spans the footprint: the post
stands on a foot plate sized to the Universal Connector ring, whose underside is the cable port, so a panel on the
ground, on a roof, or on top of a cable column shows one flat surface.

The controller is the array head: an inverter cabinet that rises above the decks, with the cable port under its base, a teal band at
deck height, a framed battery bay and status lamp on the front, heat-sink fins on the back, a top display that
SolarArrayControllerRenderer lights as a map of the array, and a bracket on which the renderer mounts the installed
Solar Array Extender. Both renderers' constants match the geometry here.
Run from the repository root:

    python tools/blockmodels/solar.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
BLOCKSTATES = ASSETS / "blockstates"
PANEL_TEXTURES = ASSETS / "textures/block/solar_panel"
CONTROLLER_TEXTURES = ASSETS / "textures/block/solar_array_controller"
PANEL_TEX = "rngtech:block/solar_panel/"
CONTROLLER_TEX = "rngtech:block/solar_array_controller/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
MATERIALS = ("crude", "copper", "gold", "sparksteel")
EMISSIVE = {"neoforge_data": {"block_light": 15, "sky_light": 15}}

# The deck turns on a north-south torque tube about TRACK_PIVOT, toward the sun (or, for an array that converts
# moonlight, the moon) while generating, and stows flat otherwise; the renderer draws it at up to TRACK_MAX degrees.
# Flat, the frame lips at DECK_Y1 are the walking surface and match the collision shape in SolarPanelBlock.
TRACK_PIVOT = [8.0, 4.0, 8.0]
TRACK_MAX = 25.0
# Baked steps for the static fallback: stowed, or 22.5 degrees east or west (model rotations step by 22.5).
TILTS = {"flat": 0.0, "east": -22.5, "west": 22.5}
DECK_Y1 = TRACK_PIVOT[1] + 2.25
LIP = 0.5
# Purlins under the deck run east-west; rail clamps hold the deck to them on the west and east edges.
PURLIN_ZS = (2.5, 12.0)
PURLIN_W = 1.5
# The post stands on a foot plate exactly the size of the Universal Connector ring (4 to 12), whose underside is the
# cable port, so a cable below plugs straight into it. Anchor bolts hold the plate at its corners.
FOOT = ((4.0, 0.0, 4.0), (12.0, 0.75, 12.0))
FOOT_TOP = FOOT[1][1]
BOLT = 0.75

# Controller. The front faces north (z = 0). A wide base sits among the panel decks; the narrower head above it
# carries everything a player reads.
BASE = ((1.0, 0.0, 1.0), (15.0, 7.0, 15.0))
BAND_Y = DECK_Y1
HEAD = ((2.0, 7.0, 2.0), (14.0, 11.25, 14.0))
CAP = ((1.5, 11.25, 1.5), (14.5, 12.0, 14.5))
# Array map display: a square bezel so every facing keeps the map on the same footprint.
DISPLAY = ((3.5, 12.0, 2.5), (12.5, 12.5, 11.5))
DISPLAY_MARGIN = 0.75
DISPLAY_CELLS = 5
DISPLAY_LED = 1.2
# Extender bracket behind the display; the renderer hinges the extender card on its front top edge.
SOCKET = ((4.0, 12.0, 12.0), (12.0, 12.75, 14.25))
# Battery bay on the front: a frame of four bars around a glass pane, with the cell drawn behind the glass.
BAY = ((8.5, 7.5, 1.25), (13.0, 11.0, 2.0))
BAY_BAR = 0.5
BAY_GLASS_Z = 1.35
STATUS_LAMP = ((3.0, 8.75, 1.5), (4.5, 10.25, 2.0))


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


FULL_UV = [0, 0, 16, 16]


# ---------------------------------------------------------------- panel geometry

UP_AND_SIDES = ("north", "south", "east", "west", "up")


def deck_elements():
    """The tracking deck: glass, lips, clamps, purlins, and torque tube turn as one about TRACK_PIVOT."""
    up_and_sides = UP_AND_SIDES
    py = TRACK_PIVOT[1]
    y0, glass, y1, clamp_top = py + 1.25, py + 1.9, DECK_Y1, DECK_Y1 + 0.2
    deck = [
        # PV deck: glass cells on top, frame metal on the edges, backsheet underneath.
        box((0, y0, 0), (16, glass, 16), {"up": "cells", "down": "backsheet", "*": "frame"}),
        # Raised frame lips; their tops map the frame border of the cells texture.
        box((0, glass, 0), (16, y1, LIP), {"up": "cells", "*": "frame"}, faces=up_and_sides),
        box((0, glass, 16 - LIP), (16, y1, 16), {"up": "cells", "*": "frame"}, faces=up_and_sides),
        box((0, glass, LIP), (LIP, y1, 16 - LIP), {"up": "cells", "*": "frame"}, faces=up_and_sides),
        box((16 - LIP, glass, LIP), (16, y1, 16 - LIP), {"up": "cells", "*": "frame"}, faces=up_and_sides),
    ]
    for z0 in PURLIN_ZS:
        for x0 in (0.0, 16 - 0.75):
            deck.append(box((x0, y1, z0), (x0 + 0.75, clamp_top, z0 + PURLIN_W), "clamp", faces=up_and_sides))
        deck.append(box((0.5, y0 - 0.5, z0), (15.5, y0, z0 + PURLIN_W), "rail", faces=("north", "south", "east", "west", "down")))
    # Torque tube along the whole block, so a north-south row shares one continuous tube.
    deck.append(box((7.25, py - 0.75, 0), (8.75, py + 0.75, 16), "tube"))
    return deck


def foot_elements():
    """The foot plate with the cable port underneath, and an anchor bolt in each corner."""
    (fx0, _, fz0), (fx1, fy1, fz1) = FOOT
    elements = [box(*FOOT, {"up": "foot", "down": "port", "*": "foot_side"}, uv={"up": [fx0, fz0, fx1, fz1], "down": [fx0, fz0, fx1, fz1]})]
    for x0 in (fx0 + 0.5, fx1 - 0.5 - BOLT):
        for z0 in (fz0 + 0.5, fz1 - 0.5 - BOLT):
            elements.append(box((x0, fy1, z0), (x0 + BOLT, fy1 + 0.4, z0 + BOLT), "clamp", faces=UP_AND_SIDES))
    return elements


def mount_elements():
    """The static mount: foot plate with the cable port, post, bearing, slew drive, and braces, all set from the pivot."""
    py = TRACK_PIVOT[1]
    elements = foot_elements() + [
        box((7.0, FOOT_TOP, 7.0), (9.0, py - 1.5, 9.0), "post", faces=SIDES),
        box((6.75, FOOT_TOP, 6.75), (9.25, FOOT_TOP + 0.5, 9.25), "band", faces=("north", "south", "east", "west", "up")),
        # Bearing saddle and cheek plates under the tube, and the slew drive on the post.
        box((6.75, py - 1.5, 7.0), (9.25, py - 0.75, 9.0), "flange"),
        box((6.4, py - 1.5, 7.25), (6.8, py + 0.5, 8.75), "flange"),
        box((9.2, py - 1.5, 7.25), (9.6, py + 0.5, 8.75), "flange"),
        box((9.6, py - 3.0, 7.25), (11.0, py - 1.75, 8.75), "junction"),
    ]
    half = 3.0 * 2 ** 0.5 / 2
    for cz, angle in ((5.5, 45), (10.5, -45)):
        cy = py - 2.25
        elements.append(box(
            (7.625, cy - 0.375, cz - half), (8.375, cy + 0.375, cz + half), "post",
            rotation={"origin": [8, cy, cz], "axis": "x", "angle": angle},
        ))
    return elements


def panel_elements(tilt="flat"):
    deck = deck_elements()
    if TILTS[tilt]:
        for element in deck:
            element["rotation"] = {"origin": TRACK_PIVOT, "axis": "z", "angle": TILTS[tilt]}
    return deck + mount_elements()


def panel_textures(material):
    return {
        "particle": PANEL_TEX + f"cells_{material}",
        "cells": PANEL_TEX + f"cells_{material}",
        "frame": PANEL_TEX + f"frame_{material}",
        "backsheet": PANEL_TEX + "backsheet",
        "rail": PANEL_TEX + "rail",
        "junction": PANEL_TEX + "junction",
        "post": PANEL_TEX + "post",
        "band": PANEL_TEX + "band",
        "flange": PANEL_TEX + "flange",
        "clamp": PANEL_TEX + "clamp",
        "tube": PANEL_TEX + "post",
        "foot": PANEL_TEX + "foot",
        "foot_side": PANEL_TEX + "foot_side",
        "port": PANEL_TEX + "port",
    }


# The panel's tracker blockstate: stowed, or tracking a body in the east or west half of the sky.
TRACKERS = ("stowed", "east", "west")


def panel_blockstate(material):
    """Every state bakes the mount; the renderer draws the deck. Facing is ignored so rows line up however the panels
    were placed."""
    variants = {}
    for active in ("false", "true"):
        for facing, _ in FACINGS:
            for tracker in TRACKERS:
                variants[f"active={active},facing={facing},tracker={tracker}"] = {"model": f"rngtech:block/{material}_solar_panel_mount"}
    return {"variants": variants}


# ---------------------------------------------------------------- controller geometry

def controller_elements():
    (bx0, _, bz0), (bx1, by1, bz1) = BASE
    (hx0, _, hz0), (hx1, _, hz1) = HEAD
    elements = [box(
        (bx0, 0.0, bz0), (bx1, by1, bz1), {"up": "ledge", "down": "port", "*": "casing"},
        uv={"down": [bx0, bz0, bx1, bz1]},
    )]
    # Teal band just above the panel decks, where the array head rises out of the field.
    elements.append(box((bx0 - 0.25, BAND_Y, bz0 - 0.25), (bx1 + 0.25, by1, bz1 + 0.25), "band", faces=SIDES))
    elements.append(box(*HEAD, {"north": "front", "south": "back", "east": "side", "west": "side"}, faces=SIDES))
    elements.append(box(*CAP, {"up": "cap", "*": "flange"}))
    elements.append(box(*DISPLAY, {"up": "display", "*": "flange"}, uv={"up": FULL_UV}))
    elements.append(box(*SOCKET, {"up": "socket", "*": "flange"}, faces=("north", "south", "east", "west", "up")))
    # Battery bay: four bars and a glass pane; the cell itself is drawn by the renderer.
    (ax0, ay0, az0), (ax1, ay1, az1) = BAY
    bar = BAY_BAR
    elements.append(box((ax0, ay1 - bar, az0), (ax1, ay1, az1), "flange", faces=("north", "east", "west", "up", "down")))
    elements.append(box((ax0, ay0, az0), (ax1, ay0 + bar, az1), "flange", faces=("north", "east", "west", "up", "down")))
    elements.append(box((ax0, ay0 + bar, az0), (ax0 + bar, ay1 - bar, az1), "flange", faces=("north", "east", "west")))
    elements.append(box((ax1 - bar, ay0 + bar, az0), (ax1, ay1 - bar, az1), "flange", faces=("north", "east", "west")))
    elements.append(box(
        (ax0 + bar, ay0 + bar, BAY_GLASS_Z), (ax1 - bar, ay1 - bar, BAY_GLASS_Z), "glass",
        faces=("north", "south"), uv={"north": FULL_UV, "south": FULL_UV},
    ))
    elements.append(box(*STATUS_LAMP, "lamp", faces=("north", "east", "west", "up", "down"), extra=EMISSIVE))
    # Heat-sink fins on the back of the head.
    for i in range(5):
        x0 = 3.5 + i * 2.0
        elements.append(box((x0, 7.5, hz1), (x0 + 0.75, 10.75, hz1 + 0.75), "fin", faces=("south", "east", "west", "up", "down")))
    return elements


CONTROLLER_TEXTURES_MAP = {
    "particle": CONTROLLER_TEX + "casing",
    "casing": CONTROLLER_TEX + "casing",
    "ledge": CONTROLLER_TEX + "ledge",
    "front": CONTROLLER_TEX + "front",
    "side": CONTROLLER_TEX + "side",
    "back": CONTROLLER_TEX + "back",
    "band": CONTROLLER_TEX + "band",
    "cap": CONTROLLER_TEX + "cap",
    "display": CONTROLLER_TEX + "display",
    "socket": CONTROLLER_TEX + "socket",
    "flange": CONTROLLER_TEX + "flange",
    "glass": CONTROLLER_TEX + "glass",
    "fin": CONTROLLER_TEX + "fin",
    "port": PANEL_TEX + "port",
    "lamp": CONTROLLER_TEX + "lamp_off",
}


def controller_blockstate():
    variants = {}
    for active in ("false", "true"):
        for facing, angle in FACINGS:
            entry = {"model": "rngtech:block/solar_array_controller" + ("_active" if active == "true" else "")}
            if angle:
                entry["y"] = angle
            variants[f"active={active},facing={facing}"] = entry
    return {"variants": variants}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    for material in MATERIALS:
        name = f"{material}_solar_panel"
        for tilt in TILTS:
            suffix = "" if tilt == "flat" else f"_{tilt}"
            write_json(MODELS / f"block/{name}{suffix}.json", {
                "parent": "minecraft:block/block",
                "textures": panel_textures(material),
                "elements": panel_elements(tilt),
            })
        write_json(MODELS / f"block/{name}_mount.json", {
            "parent": "minecraft:block/block",
            "textures": panel_textures(material),
            "elements": mount_elements(),
        })
        write_json(MODELS / f"block/{name}_deck.json", {
            "parent": "minecraft:block/block",
            "textures": panel_textures(material),
            "elements": deck_elements(),
        })
        write_json(MODELS / f"item/{name}.json", {"parent": f"rngtech:block/{name}_east"})
        write_json(BLOCKSTATES / f"{name}.json", panel_blockstate(material))

    write_json(MODELS / "block/solar_array_controller.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": CONTROLLER_TEXTURES_MAP,
        "elements": controller_elements(),
    })
    write_json(MODELS / "block/solar_array_controller_active.json", {
        "parent": "rngtech:block/solar_array_controller",
        "textures": {"lamp": CONTROLLER_TEX + "lamp_on"},
    })
    # The item has no renderer, so its display shows a static lit array.
    write_json(MODELS / "item/solar_array_controller.json", {
        "parent": "rngtech:block/solar_array_controller",
        "textures": {"display": CONTROLLER_TEX + "display_item"},
    })
    write_json(BLOCKSTATES / "solar_array_controller.json", controller_blockstate())


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"solar:{name}")


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


def louvres(image, x0, x1, y0, y1):
    pixels = image.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            if (y - y0) % 3 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
            elif (y - y0) % 3 == 1:
                pixels[x, y] = (*shade(SLATE, 18), 255)
    return image


def save(image, directory: Path, name: str):
    path = directory / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
PIPE = (124, 134, 146)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
MINT = (90, 242, 190)
AMBER = (255, 190, 70)

# Frame metal, frame shadow, and the cell layout for each tier.
TIERS = {
    "crude": {"frame": (128, 134, 138), "shadow": (70, 75, 78), "cells": 2, "gap": 2},
    "copper": {"frame": (190, 108, 60), "shadow": (112, 60, 32), "cells": 3, "gap": 2},
    "gold": {"frame": (226, 184, 66), "shadow": (138, 102, 28), "cells": 4, "gap": 1},
    "sparksteel": {"frame": (170, 164, 104), "shadow": (86, 82, 50), "cells": 6, "gap": 1, "backsheet": (10, 12, 18)},
}
BACKSHEET = (196, 202, 206)


def cell_spans(count, gap, start=2, end=62):
    """Cell rectangles along one axis inside the frame, separated by `gap` texels of backsheet."""
    spans = []
    usable = end - start
    for i in range(count):
        a = start + round(i * usable / count) + (gap if i else 1)
        b = start + round((i + 1) * usable / count) - (0 if i < count - 1 else 1)
        spans.append((a, b))
    return spans


def deck_top(material):
    """64 texels over 16 pixels. The border two texels are the frame lips' tops: half a bar per panel, so two panels
    side by side share one continuous bar. The lips cast a one-texel shadow onto the glass, and a faint diagonal
    sheen crosses the glass."""
    tier = TIERS[material]
    rand = rng(f"cells_{material}")
    image = Image.new("RGBA", (64, 64), (*tier.get("backsheet", BACKSHEET), 255))
    pixels = image.load()
    spans = cell_spans(tier["cells"], tier["gap"])
    for (x0, x1) in spans:
        for (y0, y1) in spans:
            if material == "crude":
                draw_poly_cell(pixels, rand, x0, x1, y0, y1, (78, 90, 108), flakes=8, busbars=2, bar=(176, 182, 188))
            elif material == "copper":
                draw_poly_cell(pixels, rand, x0, x1, y0, y1, (42, 76, 142), flakes=14, busbars=2, bar=(186, 192, 198))
            elif material == "gold":
                draw_mono_cell(pixels, rand, x0, x1, y0, y1)
            else:
                draw_back_contact_cell(pixels, rand, x0, x1, y0, y1)
    for y in range(2, 62):
        for x in range(2, 62):
            if 66 <= x + y <= 80:
                pixels[x, y] = (*shade(pixels[x, y], 9), 255)
    for i in range(2, 62):
        for x, y in ((i, 2), (2, i), (i, 61), (61, i)):
            pixels[x, y] = (*shade(pixels[x, y], -26), 255)
    for i in range(64):
        for edge in (0, 63):
            pixels[i, edge] = (*shade(tier["frame"], 16 + rand.uniform(-5, 5)), 255)
            pixels[edge, i] = (*shade(tier["frame"], 16 + rand.uniform(-5, 5)), 255)
        for edge in (1, 62):
            pixels[i, edge] = (*shade(tier["frame"], rand.uniform(-4, 4)), 255)
            pixels[edge, i] = (*shade(tier["frame"], rand.uniform(-4, 4)), 255)
    return image


def draw_poly_cell(pixels, rand, x0, x1, y0, y1, base, flakes, busbars, bar):
    """Polycrystalline cell: mottled crystal flakes with silver bus bars."""
    seeds = [(rand.uniform(x0, x1), rand.uniform(y0, y1), rand.uniform(-flakes, flakes)) for _ in range(9)]
    for y in range(y0, y1):
        for x in range(x0, x1):
            nearest = min(seeds, key=lambda s: (s[0] - x) ** 2 + (s[1] - y) ** 2)
            finger = 7 if (y - y0) % 3 == 1 else 0
            pixels[x, y] = (*shade(base, nearest[2] + finger + rand.uniform(-3, 3)), 255)
    width = x1 - x0
    for k in range(busbars):
        bx = x0 + round((k + 1) * width / (busbars + 1))
        for y in range(y0, y1):
            pixels[bx, y] = (*shade(bar, rand.uniform(-6, 6)), 255)


def draw_mono_cell(pixels, rand, x0, x1, y0, y1):
    """Monocrystalline cell: deep navy with clipped corners and two fine bus bars."""
    base = (24, 34, 70)
    for y in range(y0, y1):
        for x in range(x0, x1):
            corner = min(x - x0, x1 - 1 - x) + min(y - y0, y1 - 1 - y)
            if corner < 2:
                continue
            sheen = (y - y0) / max(1, y1 - y0) * 10
            finger = 8 if (y - y0) % 3 == 1 else 0
            pixels[x, y] = (*shade(base, sheen + finger + rand.uniform(-3, 3)), 255)
    for k in (1, 2):
        bx = x0 + round(k * (x1 - x0) / 3)
        for y in range(y0 + 1, y1 - 1):
            pixels[bx, y] = (*shade((168, 174, 182), rand.uniform(-6, 6)), 255)


def draw_back_contact_cell(pixels, rand, x0, x1, y0, y1):
    """Back-contact cell: near black with a faint violet-to-teal sheen and no bus bars on the face."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((x - x0) + (y - y0)) / max(1, (x1 - x0) + (y1 - y0))
            base = mix((20, 18, 38), (14, 34, 44), t)
            pixels[x, y] = (*shade(base, rand.uniform(-2, 2)), 255)
    glint_y = y0 + (y1 - y0) // 3
    if x1 - x0 > 4:
        pixels[x0 + 2, glint_y] = (*shade((60, 70, 110), 0), 255)


def frame_side(material):
    tier = TIERS[material]
    return noisy(64, tier["frame"], f"frame_{material}", amount=5, streak=4)


def write_panel_textures() -> None:
    for material in MATERIALS:
        save(deck_top(material), PANEL_TEXTURES, f"cells_{material}")
        save(frame_side(material), PANEL_TEXTURES, f"frame_{material}")

    backsheet = noisy(64, SLATE_DARK, "backsheet", amount=3)
    pixels = backsheet.load()
    for y in range(64):
        for x in range(64):
            if x % 16 == 0 or y % 16 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
    save(backsheet, PANEL_TEXTURES, "backsheet")
    save(noisy(64, STEEL, "rail", amount=5, streak=5), PANEL_TEXTURES, "rail")
    save(noisy(64, PIPE, "post", amount=4, streak=5), PANEL_TEXTURES, "post")
    save(noisy(64, STEEL, "flange", amount=5), PANEL_TEXTURES, "flange")
    save(noisy(64, (172, 180, 190), "clamp", amount=6, streak=3), PANEL_TEXTURES, "clamp")
    junction = panel_seams(noisy(64, SLATE, "junction", amount=4), rivets=False)
    save(junction, PANEL_TEXTURES, "junction")
    save(band_texture("panel_band"), PANEL_TEXTURES, "band")
    save(foot_top(), PANEL_TEXTURES, "foot")
    save(foot_side(), PANEL_TEXTURES, "foot_side")
    save(port(), PANEL_TEXTURES, "port")


def band_texture(name):
    band = noisy(64, TEAL, name, amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*TEAL_DARK, 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    return band


def foot_top():
    """Foot plate: dark slate tread, drawn in the middle of the texture where the plate's 4 to 12 face maps it, with a
    bevel at the plate's edge."""
    foot = noisy(64, SLATE_DARK, "foot", amount=4)
    pixels = foot.load()
    for y in range(16, 48):
        for x in range(16, 48):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
            if x in (16, 47) or y in (16, 47):
                pixels[x, y] = (*shade(SLATE, 16), 255)
    return foot


def foot_side():
    side = noisy(64, SLATE_DARK, "foot_side", amount=4)
    pixels = side.load()
    for x in range(64):
        pixels[x, 61] = (*shade(SLATE, 20), 255)
        pixels[x, 63] = (*shade(SLATE_DARK, -10), 255)
    return side


def port():
    """The cable port for a block's underside: the Universal Connector ring at 4 to 12, in the middle of the texture,
    on plain slate for a face that is wider than the ring. The ring stays dim, since a raised field shows every
    panel's port from below."""
    image = noisy(64, SLATE_DARK, "pad_bottom", amount=3)
    draw = ImageDraw.Draw(image)
    draw.rectangle([16, 16, 47, 47], fill=(*SLATE, 255), outline=(*SLATE_DARK, 255))
    draw.rectangle([18, 18, 45, 45], outline=(*shade(SLATE_DARK, 8), 255))
    draw.rectangle([19, 19, 44, 44], outline=(*TEAL_DARK, 255))
    for x, y in ((17, 17), (45, 17), (17, 45), (45, 45)):
        draw.point([(x, y)], fill=(150, 158, 168, 255))
    draw.rectangle([26, 26, 37, 37], fill=(14, 18, 24, 255), outline=(98, 108, 120, 255))
    for x, y in ((28, 28), (34, 28), (28, 34), (34, 34)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(184, 115, 51, 255))
    return image


def lamp_texture(color, name):
    lamp = noisy(16, color, name, amount=6)
    pixels = lamp.load()
    for y in range(4, 8):
        for x in range(4, 8):
            pixels[x, y] = (*shade(color, 50), 255)
    return lamp


def display_layout():
    """LED rectangles on the display face, in texels of the 64x64 display texture (row-major, north row first)."""
    (x0, _, z0), (x1, _, _) = DISPLAY
    size = x1 - x0
    scale = 64 / size
    pitch = (size - 2 * DISPLAY_MARGIN) / DISPLAY_CELLS
    cells = []
    for row in range(DISPLAY_CELLS):
        for column in range(DISPLAY_CELLS):
            u0 = (DISPLAY_MARGIN + column * pitch + (pitch - DISPLAY_LED) / 2) * scale
            v0 = (DISPLAY_MARGIN + row * pitch + (pitch - DISPLAY_LED) / 2) * scale
            cells.append((u0, v0, u0 + DISPLAY_LED * scale, v0 + DISPLAY_LED * scale))
    return cells


def display_texture(lit):
    """Dark glass with a socket for each map cell; `lit` maps a cell index to a colour for the item model."""
    image = noisy(64, (16, 22, 28), "display", amount=2)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*STEEL, 255), width=2)
    draw.rectangle([2, 2, 61, 61], outline=(10, 14, 18, 255), width=2)
    for index, (u0, v0, u1, v1) in enumerate(display_layout()):
        draw.rectangle([round(u0) - 1, round(v0) - 1, round(u1), round(v1)], fill=(6, 8, 10, 255), outline=(40, 50, 62, 255))
        color = lit.get(index)
        if color:
            draw.rectangle([round(u0), round(v0), round(u1) - 1, round(v1) - 1], fill=(*color, 255))
            draw.point([(round(u0) + 1, round(v0) + 1)], fill=(*shade(color, 40), 255))
    draw.line([(48, 6), (57, 15)], fill=(54, 66, 78, 255))
    return image


def write_controller_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), CONTROLLER_TEXTURES, "casing")
    tread = noisy(64, SLATE_DARK, "ledge", amount=4)
    pixels = tread.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 in (0, 1) and (x - y) % 16 in range(0, 6):
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(tread, CONTROLLER_TEXTURES, "ledge")
    # Head front: louvres between the status lamp and the battery bay (x 5 to 8, y 7.75 to 10.75).
    front = louvres(panel_seams(noisy(64, SLATE, "front")), 32, 44, 21, 33)
    save(front, CONTROLLER_TEXTURES, "front")
    side = louvres(panel_seams(noisy(64, SLATE, "side"), rivets=False), 12, 52, 21, 33)
    save(side, CONTROLLER_TEXTURES, "side")
    save(panel_seams(noisy(64, (40, 47, 57), "back", amount=4), rivets=False), CONTROLLER_TEXTURES, "back")
    save(band_texture("controller_band"), CONTROLLER_TEXTURES, "band")

    cap = noisy(64, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for dx, dy in ((2, 2), (13, 2), (2, 13), (13, 13)):
                pixels[px + dx, py + dy] = (*shade((150, 158, 168), 10), 255)
    save(cap, CONTROLLER_TEXTURES, "cap")

    save(display_texture({}), CONTROLLER_TEXTURES, "display")
    lit = {12: MINT}
    for index in (6, 7, 8, 11, 13, 16, 17, 18):
        lit[index] = AMBER
    save(display_texture(lit), CONTROLLER_TEXTURES, "display_item")

    socket = noisy(64, (64, 72, 84), "socket", amount=4)
    draw = ImageDraw.Draw(socket)
    draw.rectangle([4, 8, 59, 30], fill=(12, 16, 20, 255), outline=(98, 108, 120, 255))
    for x in range(10, 56, 8):
        draw.rectangle([x, 12, x + 2, 14], fill=(184, 115, 51, 255))
    save(socket, CONTROLLER_TEXTURES, "socket")

    save(noisy(64, STEEL, "controller_flange", amount=5), CONTROLLER_TEXTURES, "flange")
    save(noisy(64, (150, 158, 168), "fin", amount=6, streak=4), CONTROLLER_TEXTURES, "fin")

    glass = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    draw.line([(11, 2), (13, 4)], fill=GLINT)
    save(glass, CONTROLLER_TEXTURES, "glass")

    save(noisy(16, (40, 30, 14), "controller_lamp_off", amount=3), CONTROLLER_TEXTURES, "lamp_off")
    save(lamp_texture(AMBER, "controller_lamp_on"), CONTROLLER_TEXTURES, "lamp_on")

    # Renderer textures: a map LED and one extender card per Solar Array Extender material.
    led = Image.new("RGBA", (16, 16), (236, 236, 236, 255))
    draw = ImageDraw.Draw(led)
    draw.rectangle([0, 0, 15, 15], outline=(200, 200, 200, 255), width=2)
    draw.rectangle([3, 3, 6, 6], fill=(255, 255, 255, 255))
    save(led, CONTROLLER_TEXTURES, "fx/led")
    save(extender_card((214, 172, 62), (120, 90, 26), (36, 58, 116), (226, 190, 80), "aethergold"),
         CONTROLLER_TEXTURES, "fx/extender_aethergold")
    save(extender_card((170, 164, 104), (86, 82, 50), (96, 160, 214), (150, 204, 236), "sparksteel"),
         CONTROLLER_TEXTURES, "fx/extender_sparksteel")


def extender_card(frame, shadow, cell, accent, name):
    """The extender's reference cell: a framed 3x2 module, coloured like its item."""
    rand = rng(f"extender_{name}")
    image = Image.new("RGBA", (16, 16), (*frame, 255))
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                pixels[x, y] = (*shade(frame, rand.uniform(-6, 6)), 255)
            elif x in (1, 14) or y in (1, 14):
                pixels[x, y] = (*shade(shadow, 0), 255)
            else:
                checker = ((x - 2) // 4 + (y - 2) // 6) % 2 == 0
                pixels[x, y] = (*shade(cell if checker else accent, rand.uniform(-6, 6)), 255)
    for x in (6, 10):
        for y in range(2, 14):
            pixels[x, y] = (*shade(shadow, 20), 255)
    for x in range(2, 14):
        pixels[x, 8] = (*shade(shadow, 20), 255)
    return image


def main() -> None:
    write_models()
    write_panel_textures()
    write_controller_textures()


if __name__ == "__main__":
    main()
