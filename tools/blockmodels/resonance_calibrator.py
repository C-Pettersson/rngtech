#!/usr/bin/env python3
"""Generate the Resonance Calibrator blockstates, block models, part models, item models, and owned textures.

A Resonance Calibrator is a tuning bench on the slate plinth. At the front, the Resonance Coil stands upright as a wound
ring on a saddle, and the input waits on a pin in the middle of the ring, lit by an emitter lens on the cabinet behind
it. Bulk chassis carry one smaller ring per lane. The cabinet at the back holds the cable port and, in a bay in its west
wall, the Control Board. On top of it a monitor shows the active Calibration Pattern behind glass and a scope with the
waveform and the stability range, and on its roof the status lamp and the Job Chip lamp (job_lamp.py). Facing the front,
the Battery Cell stands in a cradle on the left and the Stabilizer Matrix floats between two prongs on the right.
Finished components collect in a tray at the front. The Nullite chassis, the precision branch, adds a vernier ring with
alignment ticks around the coil.

The housings are static block models. The installed parts are standalone part models; they, the items, the field
glow, the beam, the scope, and the meters are drawn by ResonanceCalibratorRenderer, whose constants match the geometry
here. Run from the repository root:

    python tools/blockmodels/resonance_calibrator.py
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

import job_lamp

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block"
TEX = "rngtech:block/resonance_calibrator/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))
STAGGER = 0.03

# ---------------------------------------------------------------- geometry
# The front faces north (z = 0); facing the front, east (+x) is on the left.
# Back cabinet with the cable port; the Control Board bay is cut into its west wall.
CABINET = ((1.0, 2.0, 10.0), (15.0, 12.0, 15.0))
WALL_X = 2.0
BAY = ((10.75, 3.0), (14.75, 10.25))
RAIL = 0.5
# Control Board standing in the bay, its face to the west.
BOARD = ((1.25, 3.5, 11.0), (1.75, 9.75, 14.5))
# Cable port on the back, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Monitor on the cabinet: a bezel framing the pattern window and the scope screen.
MONITOR = ((2.0, 12.0, 10.0), (14.0, 15.75, 14.5))
BEZEL_Z = 9.5
PATTERN_WINDOW = ((2.75, 12.75), (5.75, 15.0))
SCREEN = ((6.25, 12.75), (13.25, 15.0))
BACKING_Z = 9.98
GLASS_Z = 9.6
# Job Chip lamp on the monitor's roof, across from the status lamp.
JOB_LAMP = ((2.5, MONITOR[1][1], 11.0), (4.0, 16.0, 12.5))
JOB_LAMP_FACES = SIDES + ("up",)

# Resonance Coil: an octagonal ring on the z axis, wound around its bars, between two teal former lips inside the ring.
RING_X, RING_Y = 8.0, 7.5
RING_OUTER, RING_INNER = 4.0, 3.0
RING_Z = (2.5, 4.5)
FORMER = 0.2
FORMER_LIP = 0.25
# Lane layouts: ring centres across x, the ring centre height, the ring scale, the size of the item on the pin, and the
# emitter lenses across x. The outer Tungstensteel lenses sit inward, clear of the cradle and stand, and aim across.
LANES = {
    1: ((8.0,), 7.5, 1.0, 3.2, (8.0,)),
    2: ((4.75, 11.25), 7.0, 0.6875, 2.2, (4.75, 11.25)),
    3: ((3.0, 8.0, 13.0), 6.75, 0.55, 1.75, (5.0, 8.0, 11.0)),
}
PIN_Z = 3.5
EMITTER_Z = (9.0, 10.0)

# Battery Cell cradle on the east side; the cell stands centred at (CELL_X, CELL_Z).
CRADLE = ((12.5, 2.0, 5.5), (15.5, 10.0, 10.0))
CELL_X, CELL_Z = 14.0, 7.75
CELL_FLOOR = 3.0
# Charge meter on the cradle's front post, facing north.
METER = ((12.75, 3.0, 5.0), (15.25, 9.5, 5.5))
METER_SEGMENTS = 8

# Stabilizer Matrix stand on the west side; the crystal floats centred at MATRIX between two prongs.
STAND = ((0.5, 2.0, 5.5), (3.5, 10.75, 10.0))
MATRIX = (2.0, 6.625, 7.75)
MATRIX_LAYERS = ((0.4, 1.2), (0.95, 0.9), (1.5, 0.6), (2.0, 0.35), (2.35, 0.15))

# Output tray at the front.
TRAY = ((5.0, 2.0, 0.5), (11.0, 2.4, 2.45))
TRAY_LIP = 0.35

# Nullite vernier ring with alignment ticks, in front of the coil.
VERNIER = (4.3, 4.65)
VERNIER_Z = (1.95, 2.2)

TIERS = ("iron", "copper", "steel", "titanium", "lead", "tungstensteel", "nullite")
TIER_LANES = {"lead": 2, "tungstensteel": 3}
# The Resonance Coil and Control Board shown on each chassis item model, which has no renderer.
ITEM_GEAR = {
    "iron": "iron", "copper": "copper", "steel": "steel", "titanium": "titanium",
    "lead": "steel", "tungstensteel": "tungstensteel", "nullite": "nullite",
}
GEAR = ("iron", "copper", "steel", "titanium", "tungstensteel", "nullite")


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, uv=None, rotation=None, face_rotation=None, tint=False, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    if rotation:
        element["rotation"] = rotation
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is None:
            continue
        entry = {"texture": f"#{key}"}
        if uv and face in uv:
            entry["uv"] = uv[face]
        if face_rotation and face in face_rotation:
            entry["rotation"] = face_rotation[face]
        if tint:
            entry["tintindex"] = 0
        element["faces"][face] = entry
    if extra:
        element.update(extra)
    return element


def plinth():
    return box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})


def lamp(start, end):
    return box(start, end, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}})


def ring_z(cx, cy, outer, inner, z0, z1, texture, wound=False, tint=False):
    """
    A flat octagonal ring on the z axis: four straight bars plus four diagonal bars. A `wound` ring turns the texture
    on its upright bars so the windings always run across the bar.
    """
    half = outer * TAN
    depth = outer - inner
    across = {face: 90 for face in SIDES} if wound else None

    def bar(start, end, upright, rotation=None):
        return box(start, end, texture, rotation=rotation, face_rotation=across if upright else None, tint=tint)

    bars = [
        bar((cx - half, cy + inner, z0), (cx + half, cy + outer, z1), False),
        bar((cx - half, cy - outer, z0), (cx + half, cy - inner, z1), False),
        bar((cx + inner, cy - half, z0), (cx + outer, cy + half, z1), True),
        bar((cx - outer, cy - half, z0), (cx - inner, cy + half, z1), True),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = cx + sx * mid, cy + sy * mid
            upright = sx * sy < 0
            if upright:
                start, end = (x - depth / 2, y - half, z0 + STAGGER), (x + depth / 2, y + half, z1 - STAGGER)
            else:
                start, end = (x - half, y - depth / 2, z0 + STAGGER), (x + half, y + depth / 2, z1 - STAGGER)
            rotation = {"origin": [r(x), r(y), r((z0 + z1) / 2)], "axis": "z", "angle": -45}
            bars.append(bar(start, end, upright, rotation))
    return bars


def coil_elements(cx=RING_X, cy=RING_Y, scale=1.0, glow=False):
    """The Resonance Coil: a wound ring between two teal former lips inside the ring, one at each face. The glow
    version is a tinted shell over the windings for the renderer's field glow."""
    outer, inner = RING_OUTER * scale, RING_INNER * scale
    z0, z1 = RING_Z
    if glow:
        return ring_z(cx, cy, outer + 0.02, inner - 0.02, z0 - 0.02, z1 + 0.02, "glow", wound=True, tint=True)
    elements = ring_z(cx, cy, outer, inner, z0, z1, "coil", wound=True)
    for l0, l1 in ((z0 - FORMER_LIP, z0 + 0.1), (z1 - 0.1, z1 + FORMER_LIP)):
        elements += ring_z(cx, cy, inner + FORMER * scale, inner - 0.1, l0, l1, "accent")
    return elements


def lane_elements(lanes):
    """Per lane: the saddle the ring rests on, the pin and cup that hold the input, and the emitter lens behind."""
    xs, cy, scale, size, lenses = LANES[lanes]
    elements = []
    bottom = cy - RING_OUTER * scale
    cup_top = cy - size / 2 - 0.05
    half = max(1.0, RING_OUTER * scale * 0.45)
    lens = max(1.0, 1.5 * scale)
    z0, z1 = RING_Z
    for cx in xs:
        elements.append(box((cx - half, 2.0, z0), (cx + half, bottom, z1), {"up": "saddle", "*": "casing"}, faces=SIDES + ("up",)))
        elements.append(box((cx - 0.15, bottom, PIN_Z - 0.15), (cx + 0.15, cup_top - 0.25, PIN_Z + 0.15), "pipe", faces=SIDES))
        elements.append(box((cx - 0.6, cup_top - 0.25, PIN_Z - 0.5), (cx + 0.6, cup_top, PIN_Z + 0.5), "pipe", faces=SIDES + ("up", "down")))
    e0, e1 = EMITTER_Z
    for lx in lenses:
        elements.append(box((lx - lens, cy - lens, e0), (lx + lens, cy + lens, e1), {"north": "emitter", "*": "trim"},
                            faces=("north", "east", "west", "up", "down"), uv={"north": [0, 0, 16, 16]}))
    return elements


def cabinet_elements():
    (x0, y0, z0), (x1, y1, z1) = CABINET
    (bz0, by0), (bz1, by1) = BAY
    shell = "shell"
    side = ("west", "north", "south")
    elements = [
        # Main body; its west face is the bay's backplane, hidden elsewhere behind the west wall.
        box((WALL_X, y0, z0), (x1, y1, z1), {"west": "backplane", "up": "crown", "*": shell}, faces=("north", "south", "east", "west", "up")),
        # West wall around the Control Board bay.
        box((x0, y0, z0), (WALL_X, by0, z1), shell, faces=side + ("up",)),
        box((x0, by1, z0), (WALL_X, y1, z1), shell, faces=side + ("up", "down")),
        box((x0, by0, z0), (WALL_X, by1, bz0), shell, faces=side),
        box((x0, by0, bz1), (WALL_X, by1, z1), shell, faces=side),
        # Guide rails top and bottom of the bay.
        box((x0, by0, bz0), (WALL_X, by0 + RAIL, bz1), "trim", faces=("west", "up")),
        box((x0, by1 - RAIL, bz0), (WALL_X, by1, bz1), "trim", faces=("west", "down")),
        # Teal band round the front and sides.
        box((x0 - 0.25, 10.75, z0 - 0.25), (x1 + 0.25, 11.25, z1), "band", faces=("north", "east", "west", "down")),
        box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"), uv={"south": [0, 0, 16, 16]}),
    ]
    return elements


def monitor_elements():
    (mx0, my0, mz0), (mx1, my1, mz1) = MONITOR
    (px0, py0), (px1, py1) = PATTERN_WINDOW
    (sx0, sy0), (sx1, sy1) = SCREEN
    elements = [box((mx0, my0, mz0), (mx1, my1, mz1), {"up": "vent", "*": "shell"}, faces=("south", "east", "west", "up"))]
    frame = ("north", "east", "west", "up", "down")
    z0, z1 = BEZEL_Z, mz0
    elements += [
        box((mx0, my0, z0), (px0, my1, z1), "trim", faces=frame),
        box((sx1, my0, z0), (mx1, my1, z1), "trim", faces=frame),
        box((px0, py1, z0), (sx1, my1, z1), "trim", faces=frame),
        box((px0, my0, z0), (sx1, py0, z1), "trim", faces=frame),
        box((px1, py0, z0), (sx0, py1, z1), "trim", faces=frame),
        box((px0, py0, BACKING_Z), (px1, py1, mz0), {"north": "pattern_slot"}, faces=("north",), uv={"north": [0, 0, 16, 12]}),
        box((sx0, sy0, BACKING_Z), (sx1, sy1, mz0), {"north": "screen"}, faces=("north",)),
        box((px0, py0, GLASS_Z - 0.05), (px1, py1, GLASS_Z), "glass", faces=("north", "south"),
            uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}),
        lamp((12.0, my1, 11.0), (13.5, 16.0, 12.5)),
        job_lamp.element(*JOB_LAMP, JOB_LAMP_FACES),
    ]
    return elements


def cradle_elements():
    """An open Battery Cell cradle against the cabinet, with the charge meter on its front post."""
    (x0, y0, z0), (x1, y1, z1) = CRADLE
    px0, px1 = x0 + 0.25, x1 - 0.25
    return [
        box((x0, y0, z0), (x1, y0 + 0.75, z1), {"up": "socket", "*": "casing"}, faces=("north", "east", "west", "up")),
        box((px0, y0, z1 - 1.0), (px1, y1, z1), "casing", faces=("north", "east", "west", "up")),
        box((px0, y1 - 0.75, z0 + 0.5), (px1, y1, z1 - 1.0), "trim", faces=("north", "east", "west", "up", "down")),
        box((px0, y0, z0), (px1, y1, z0 + 0.5), "casing", faces=SIDES + ("up",)),
        box(METER[0], METER[1], {"north": "meter", "*": "trim"}, faces=("north", "east", "west", "up", "down"),
            uv={"north": [0, 0, 6, 16]}),
        box((CELL_X - 1.25, y0 + 0.75, CELL_Z - 1.25), (CELL_X + 1.25, CELL_FLOOR, CELL_Z + 1.25), "pipe", faces=SIDES + ("up",)),
    ]


def stand_elements():
    """The Stabilizer Matrix stand: a base, a back post, a top arm, and two prongs the crystal floats between."""
    (x0, y0, z0), (x1, y1, z1) = STAND
    mx, my, mz = MATRIX
    top = my + MATRIX_LAYERS[-1][0]
    bottom = my - MATRIX_LAYERS[-1][0]
    return [
        box((x0, y0, z0), (x1, 3.25, z1 - 0.5), {"up": "socket", "*": "casing"}, faces=("north", "east", "west", "up")),
        box((x0 + 0.25, y0, z1 - 1.0), (x1 - 0.25, y1, z1), "casing", faces=("north", "east", "west", "up")),
        box((x0 + 0.25, y1 - 0.75, mz - 1.5), (x1 - 0.25, y1, z1 - 1.0), "trim", faces=("north", "east", "west", "up", "down")),
        box((mx - 0.5, 3.25, mz - 0.5), (mx + 0.5, bottom - 0.5, mz + 0.5), "collar", faces=SIDES + ("up",)),
        box((mx - 0.5, top + 0.5, mz - 0.5), (mx + 0.5, y1 - 0.75, mz + 0.5), "collar", faces=SIDES + ("down",)),
    ]


def tray_elements():
    (x0, y0, z0), (x1, y1, z1) = TRAY
    lip = TRAY_LIP
    top = y1 + 0.45
    return [
        box((x0, y0, z0), (x1, y1, z1), {"up": "tray", "*": "casing"}, faces=("north", "east", "west", "up")),
        box((x0, y1, z0), (x1, top, z0 + lip), "trim", faces=("north", "south", "east", "west", "up")),
        box((x0, y1, z0 + lip), (x0 + lip, top, z1), "trim", faces=("east", "west", "up", "south")),
        box((x1 - lip, y1, z0 + lip), (x1, top, z1), "trim", faces=("east", "west", "up", "south")),
    ]


def vernier_elements():
    """The Nullite precision vernier: a fine scale ring in front of the coil with four alignment ticks and a strut."""
    inner, outer = VERNIER
    z0, z1 = VERNIER_Z
    elements = ring_z(RING_X, RING_Y, outer, inner, z0, z1, "vernier")
    tick = 0.45
    for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        x, y = RING_X + dx * (outer + tick / 2), RING_Y + dy * (outer + tick / 2)
        w, h = (0.15, tick / 2) if dx == 0 else (tick / 2, 0.15)
        elements.append(box((x - w, y - h, z0), (x + w, y + h, z1), "accent"))
    elements.append(box((RING_X - 0.15, 2.0, z0), (RING_X + 0.15, RING_Y - outer, z1), "trim"))
    return elements


def housing_elements(lanes, nullite=False):
    elements = [plinth()]
    elements += cabinet_elements()
    elements += monitor_elements()
    elements += lane_elements(lanes)
    elements += cradle_elements()
    elements += stand_elements()
    elements += tray_elements()
    if nullite:
        elements += vernier_elements()
    return elements


def item_coils(lanes):
    xs, cy, scale, _, _ = LANES[lanes]
    elements = []
    for cx in xs:
        elements += coil_elements(cx, cy, scale)
    return elements


def board_elements():
    """The Control Board standing in its bay: a thin card, its face to the west, mapped whole onto that face."""
    (x0, y0, z0), (x1, y1, z1) = BOARD
    width, height = z1 - z0, y1 - y0
    return [box((x0, y0, z0), (x1, y1, z1), "board", faces=SIDES + ("up",),
                uv={"west": [0, 0, width, height], "east": [0, 0, width, height],
                    "north": [0, 0, x1 - x0, height], "south": [0, 0, x1 - x0, height], "up": [0, 0, width, x1 - x0]})]


def matrix_elements(glow=False):
    """The Stabilizer Matrix: an octahedral crystal in stepped layers turned 45 degrees, with a teal core at its
    waist. The glow version is a tinted shell over the core for the renderer."""
    mx, my, mz = MATRIX
    rotation = {"origin": [mx, my, mz], "axis": "y", "angle": 45}
    if glow:
        _, half = MATRIX_LAYERS[0]
        top = MATRIX_LAYERS[0][0]
        return [box((mx - half - 0.02, my - top - 0.02, mz - half - 0.02), (mx + half + 0.02, my + top + 0.02, mz + half + 0.02),
                    "glow", rotation=rotation, tint=True)]
    elements = []
    previous = 0.0
    for index, (reach, half) in enumerate(MATRIX_LAYERS):
        texture = "core" if index == 0 else "matrix"
        if index == 0:
            elements.append(box((mx - half, my - reach, mz - half), (mx + half, my + reach, mz + half), texture, rotation=rotation))
        else:
            for sign in (1, -1):
                y0, y1 = sorted((my + sign * previous, my + sign * reach))
                faces = SIDES + (("up",) if sign > 0 else ("down",))
                elements.append(box((mx - half, y0, mz - half), (mx + half, y1, mz + half), texture, faces=faces, rotation=rotation))
        previous = reach
    return elements


# ---------------------------------------------------------------- models

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
    "collar": TEX + "collar",
    "backplane": TEX + "backplane",
    "vent": TEX + "vent",
    "pattern_slot": TEX + "pattern_slot",
    "screen": TEX + "screen",
    "glass": TEX + "glass",
    "saddle": TEX + "saddle",
    "emitter": TEX + "emitter",
    "tray": TEX + "tray",
    "accent": TEX + "accent",
    "vernier": TEX + "vernier",
    **job_lamp.TEXTURE,
}


def tier_textures(tier):
    return {"particle": TEX + f"shell/{tier}", "shell": TEX + f"shell/{tier}", "trim": TEX + f"trim/{tier}",
            "crown": TEX + f"crown/{tier}"}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def template(tier):
    if tier == "nullite":
        return "nullite"
    return {1: "single", 2: "dual", 3: "triple"}[TIER_LANES.get(tier, 1)]


def write_models() -> None:
    templates = {"single": (1, False), "dual": (2, False), "triple": (3, False), "nullite": (1, True)}
    for name, (lanes, nullite) in templates.items():
        write_json(MODELS / f"block/resonance_calibrator/{name}.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": SHARED, "elements": housing_elements(lanes, nullite),
        })
        write_json(MODELS / f"item/resonance_calibrator_{name}_template.json", {
            "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": SHARED, "elements": item_coils(lanes) + board_elements() + housing_elements(lanes, nullite),
        })
    for tier in TIERS:
        name = f"{tier}_resonance_calibrator_chassis"
        job_lamp.write_blockstate(name, f"rngtech:block/{name}", f"rngtech:block/{name}_active",
                                  "rngtech:block/resonance_calibrator/job_lamp")
        write_json(MODELS / f"block/{name}.json", {
            "parent": f"rngtech:block/resonance_calibrator/{template(tier)}", "textures": tier_textures(tier),
        })
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": TEX + "lamp_on"}})
        gear = ITEM_GEAR[tier]
        write_json(MODELS / f"item/{name}.json", {
            "parent": f"rngtech:item/resonance_calibrator_{template(tier)}_template",
            "textures": {**tier_textures(tier), "coil": TEX + f"coil/{gear}", "board": TEX + f"board/{gear}"},
        })

    parts = MODELS / "block/resonance_calibrator"
    job_lamp.write_part(parts / "job_lamp.json", *JOB_LAMP, JOB_LAMP_FACES)
    write_json(parts / "coil.json", {"textures": {"particle": "#coil", "accent": TEX + "accent"}, "elements": coil_elements()})
    write_json(parts / "coil_glow.json", {"textures": {"particle": "#glow", "glow": TEX + "fx/field"}, "elements": coil_elements(glow=True)})
    write_json(parts / "board.json", {"textures": {"particle": "#board"}, "elements": board_elements()})
    write_json(parts / "matrix.json", {"textures": {"particle": "#matrix", "core": TEX + "matrix_core"}, "elements": matrix_elements()})
    write_json(parts / "matrix_glow.json", {"textures": {"particle": "#glow", "glow": TEX + "fx/core"}, "elements": matrix_elements(glow=True)})
    for material in GEAR:
        write_json(parts / f"coil_{material}.json", {"parent": "rngtech:block/resonance_calibrator/coil", "textures": {"coil": TEX + f"coil/{material}"}})
        write_json(parts / f"board_{material}.json", {"parent": "rngtech:block/resonance_calibrator/board", "textures": {"board": TEX + f"board/{material}"}})
        write_json(parts / f"matrix_{material}.json", {"parent": "rngtech:block/resonance_calibrator/matrix", "textures": {"matrix": TEX + f"matrix/{material}"}})
    write_json(parts / "board_unique.json", {"parent": "rngtech:block/resonance_calibrator/board", "textures": {"board": TEX + "board/unique"}})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"resonance_calibrator:{name}")


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


def save(image, name):
    path = TEXTURES / "resonance_calibrator" / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
TEAL_BRIGHT = (120, 236, 236)
PHOSPHOR = (10, 20, 18)

# Shell and trim colours for each chassis metal.
TIER_COLORS = {
    "iron": ((132, 132, 128), (176, 176, 170)),
    "copper": ((136, 78, 46), (216, 138, 86)),
    "steel": ((72, 82, 94), (150, 158, 168)),
    "titanium": ((112, 120, 136), (172, 180, 194)),
    "lead": ((78, 84, 102), (122, 128, 148)),
    "tungstensteel": ((52, 54, 64), (106, 110, 124)),
    "nullite": ((26, 50, 52), (58, 146, 136)),
}

# Coil windings, board frame, and crystal colours for each Gear metal, matching the item sprites.
GEAR_COLORS = {
    "iron": (204, 204, 194),
    "copper": (222, 132, 72),
    "steel": (150, 156, 162),
    "titanium": (178, 192, 216),
    "tungstensteel": (94, 98, 112),
    "nullite": (38, 150, 140),
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
        elif tier == "nullite":
            # Nullite: dark field-grown plate with faint teal grain lines, the precision branch's own look.
            shell = panel_seams(noisy(64, shell_color, name, amount=3, streak=2), step=32, depth=14)
            pixels = shell.load()
            rand = rng("nullite_grain")
            for _ in range(7):
                y = rand.randrange(64)
                for x in range(64):
                    if rand.random() < 0.7:
                        pixels[x, y] = (*mix(pixels[x, y], (60, 170, 156), 0.35), 255)
        else:
            step = {"steel": 16, "titanium": 16, "tungstensteel": 16}[tier]
            shell = panel_seams(noisy(64, shell_color, name, amount=4, streak=4), step=step, depth=18)
            if tier == "titanium":
                blotches(shell, "titanium_heat_tint", (120, 110, 150), 3, 12, 0.35)
        save(shell, f"shell/{tier}")
        save(noisy(64, trim_color, f"{tier}_trim", amount=6, streak=6), f"trim/{tier}")
        crown = noisy(64, shade(shell_color, -8), f"{tier}_crown", amount=4)
        pixels = crown.load()
        for y in range(64):
            for x in range(64):
                if y % 8 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -16), 255)
        save(crown, f"crown/{tier}")


def write_gear_textures() -> None:
    for material, color in GEAR_COLORS.items():
        # Coil windings: one turn of wire per model pixel, run across u; the ring turns the texture on upright bars.
        coil = noisy(64, color, f"coil_{material}", amount=5)
        pixels = coil.load()
        for y in range(64):
            for x in range(64):
                if x % 4 == 0:
                    pixels[x, y] = (*mix(color, (24, 22, 26), 0.75), 255)
                elif x % 4 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 30), 255)
                elif x % 4 == 3:
                    pixels[x, y] = (*shade(pixels[x, y], -16), 255)
        save(coil, f"coil/{material}")

        # Crystal facets: diagonal bands of light and shade over the metal-tinted crystal.
        crystal = noisy(64, color, f"matrix_{material}", amount=4)
        pixels = crystal.load()
        for y in range(64):
            for x in range(64):
                band = (x + y) % 16
                if band < 2:
                    pixels[x, y] = (*shade(pixels[x, y], 40), 255)
                elif band > 11:
                    pixels[x, y] = (*shade(pixels[x, y], -22), 255)
        save(crystal, f"matrix/{material}")

        save(board_texture(color, f"board_{material}"), f"board/{material}")
    # The Ancient Echo Control Board: a sculk-dark card with echo-cyan traces.
    save(board_texture((20, 44, 52), "board_unique", pcb=(12, 26, 34), trace=(60, 200, 210), chip=(8, 12, 16)), "board/unique")

    core = noisy(64, TEAL, "matrix_core", amount=5)
    pixels = core.load()
    for y in range(64):
        for x in range(64):
            if y % 16 in (7, 8):
                pixels[x, y] = (*TEAL_BRIGHT, 255)
    save(core, "matrix_core")

    accent = noisy(64, TEAL, "accent", amount=6)
    save(accent, "accent")


def board_texture(frame, name, pcb=(34, 104, 62), trace=(86, 170, 98), chip=(30, 32, 36)):
    """The Control Board, mapped whole onto a 3.5 x 6.25 face (14 x 25 texels): a frame in the Gear metal round a
    circuit board with traces, two chips, a row of LED pads along the top, and a gold edge connector at the bottom.
    Its narrow edges read the frame column on the left."""
    image = noisy(64, frame, name, amount=5)
    draw = ImageDraw.Draw(image)
    draw.rectangle([2, 2, 11, 22], fill=(*pcb, 255))
    rand = rng(name + "_traces")
    for _ in range(9):
        x = rand.randrange(3, 11)
        y0, y1 = sorted((rand.randrange(4, 21), rand.randrange(4, 21)))
        draw.line([(x, y0), (x, y1)], fill=(*trace, 255))
        x1 = rand.randrange(3, 11)
        draw.line([(min(x, x1), y1), (max(x, x1), y1)], fill=(*trace, 255))
    for (cx0, cy0, cx1, cy1) in ((4, 8, 9, 12), (5, 15, 9, 18)):
        draw.rectangle([cx0, cy0, cx1, cy1], fill=(*chip, 255))
        for x in range(cx0, cx1 + 1, 2):
            draw.point((x, cy0 - 1), fill=(190, 190, 196, 255))
            draw.point((x, cy1 + 1), fill=(190, 190, 196, 255))
    for x in (3, 6, 9):
        draw.rectangle([x, 3, x + 1, 4], fill=(16, 22, 18, 255))
    for x in range(3, 11, 2):
        draw.rectangle([x, 22, x, 24], fill=(214, 172, 70, 255))
    return image


def write_housing_textures() -> None:
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
    save(noisy(64, (70, 78, 88), "collar", amount=4), "collar")

    socket = noisy(64, (52, 58, 66), "socket", amount=4)
    pixels = socket.load()
    for y in range(64):
        for x in range(64):
            if (x % 16 in (2, 13)) and y % 6 == 3:
                pixels[x, y] = (*shade((150, 158, 168), 0), 255)
                pixels[x, (y + 1) % 64] = (*shade(SLATE_DARK, -6), 255)
    save(socket, "socket")

    # Bay backplane: dark panel with bus traces and a card socket the board's edge connector plugs into.
    plane = noisy(64, (28, 32, 38), "backplane", amount=3)
    pixels = plane.load()
    for y in range(64):
        for x in range(64):
            if y % 6 == 0:
                pixels[x, y] = (*shade((60, 120, 90), -10), 255)
    draw = ImageDraw.Draw(plane)
    draw.rectangle([43, 47, 59, 50], fill=(12, 14, 18, 255), outline=(150, 158, 168, 255))
    save(plane, "backplane")

    vent = noisy(64, (54, 60, 68), "vent", amount=4)
    pixels = vent.load()
    for y in range(64):
        for x in range(4, 60):
            if y % 6 in (0, 1):
                pixels[x, y] = (*shade(SLATE_DARK, -12), 255)
            elif y % 6 == 2:
                pixels[x, y] = (*shade(STEEL, 20), 255)
    save(vent, "vent")

    # Pattern window backing, mapped as a 16:12 plate: a dark card well with guide notches at the sides.
    slot = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(slot)
    draw.rectangle([0, 0, 63, 47], fill=(20, 24, 30, 255))
    draw.rectangle([0, 0, 63, 47], outline=(*shade(STEEL, -20), 255), width=2)
    for y in (10, 36):
        draw.rectangle([2, y, 5, y + 2], fill=(*TEAL_DARK, 255))
        draw.rectangle([58, y, 61, y + 2], fill=(*TEAL_DARK, 255))
    save(slot, "pattern_slot")

    # Scope screen: dark phosphor glass with a faint graticule every model pixel. The renderer draws the trace on it.
    screen = noisy(64, PHOSPHOR, "screen", amount=2)
    pixels = screen.load()
    for y in range(64):
        for x in range(64):
            if x % 8 == 0 or y % 8 == 0:
                pixels[x, y] = (*shade(PHOSPHOR, 14), 255)
            elif x % 4 == 0 and y % 4 == 0:
                pixels[x, y] = (*shade(PHOSPHOR, 10), 255)
    save(screen, "screen")

    # Window glass, mapped whole onto the pane: clear, with glints in opposite corners.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for offset in (0, 5):
        draw.line([(54 - offset, 6), (58, 10 + offset)], fill=(214, 236, 244, 200), width=2)
    draw.line([(4, 52), (10, 58)], fill=(214, 236, 244, 200), width=1)
    save(glass, "glass")

    saddle = noisy(64, (60, 66, 76), "saddle", amount=4)
    pixels = saddle.load()
    for y in range(64):
        for x in range(64):
            if x % 32 in (14, 15, 16, 17):
                pixels[x, y] = (*shade(SLATE_DARK, -10), 255)
            elif x % 32 in (13, 18):
                pixels[x, y] = (*shade(STEEL, 16), 255)
    save(saddle, "saddle")

    # Emitter lens, mapped whole onto its face: a steel housing round a teal lens with a crosshair.
    lens = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(lens)
    draw.rectangle([0, 0, 63, 63], fill=(*shade(STEEL, -20), 255))
    draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=3)
    draw.ellipse([8, 8, 55, 55], fill=(*SLATE_DARK, 255))
    draw.ellipse([14, 14, 49, 49], fill=(*TEAL_DARK, 255))
    draw.ellipse([20, 20, 43, 43], fill=(*TEAL, 255))
    draw.ellipse([24, 22, 31, 29], fill=(*TEAL_BRIGHT, 255))
    draw.line([(31, 10), (31, 53)], fill=(*SLATE_DARK, 255), width=2)
    draw.line([(10, 31), (53, 31)], fill=(*SLATE_DARK, 255), width=2)
    for x, y in ((4, 4), (57, 4), (4, 57), (57, 57)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
    save(lens, "emitter")

    tray = noisy(64, (34, 38, 44), "tray", amount=4)
    pixels = tray.load()
    for y in range(64):
        for x in range(64):
            if (x // 4 + y // 4) % 2 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 8), 255)
    save(tray, "tray")

    # Vernier scale: dark ring with a fine teal tick every model pixel and a longer one every four.
    vernier = noisy(64, (22, 30, 32), "vernier", amount=3)
    pixels = vernier.load()
    for y in range(64):
        for x in range(64):
            if x % 4 == 0 and (y % 64 < 20 or x % 16 == 0):
                pixels[x, y] = (*TEAL, 255)
    save(vernier, "vernier")

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

    save(noisy(64, (18, 44, 48), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (110, 236, 236), "lamp_on", amount=6), "lamp_on")


def write_fx_textures() -> None:
    """Renderer textures: the field glow over the windings, the matrix core glow, and the emitter beam."""
    # Field glow: the windings as a light mask, bright on the wire and dim in the gaps; tinted by the renderer.
    field = Image.new("RGBA", (64, 64))
    pixels = field.load()
    for y in range(64):
        for x in range(64):
            value = (90, 255, 210, 160)[x % 4]
            pixels[x, y] = (value, value, value, 255)
    save(field, "fx/field")

    core = Image.new("RGBA", (16, 16))
    pixels = core.load()
    for y in range(16):
        for x in range(16):
            value = clamp(255 * (0.45 + 0.55 * (1.0 - abs(y + 0.5 - 8) / 8)))
            pixels[x, y] = (value, value, value, 255)
    save(core, "fx/core")

    # Beam: bright along its middle, fading to the edges across u.
    beam = Image.new("RGBA", (16, 16))
    pixels = beam.load()
    for y in range(16):
        for x in range(16):
            value = clamp(255 * max(0.0, 1.0 - abs(x + 0.5 - 8) / 8) ** 1.5)
            pixels[x, y] = (value, value, value, 255)
    save(beam, "fx/beam")


def meter_segment(index):
    """Bottom and top of one charge-meter LED, in model pixels."""
    y0 = METER[0][1] + 0.5 + index * 0.6875
    return y0, y0 + 0.5


def main() -> None:
    write_models()
    write_housing_textures()
    write_tier_textures()
    write_gear_textures()
    write_fx_textures()


if __name__ == "__main__":
    main()
