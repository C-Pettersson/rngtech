#!/usr/bin/env python3
"""Generate the Ammonia Synthesizer block model, item model, and owned textures.

The design compresses a Haber-Bosch ammonia loop into one block. Framed sight glasses hold the Nitrogen and Hydrogen
feeds at the back west; their risers meet at a mixing tee and a suction header that runs forward into a vertical
reciprocating compressor. The compressor's crankcase carries a flywheel on its front and an open crosshead guide under
the cylinder. The compressed syngas enters a thick-walled converter vessel, the hero element, with heavy bolted flanges,
a teal band, and an armoured sight port onto its three catalyst beds. The converter outlet runs back to a frosted
condenser on top of the drive cabinet, whose rear wall holds the cable port, and drains into a framed glass Ammonia
receiver that matches the Ammonia Fuel Cell's tank. A high-pressure gauge and the status lamp stand at the front east.

The housing is a static block model; the catalyst beds and their glow, the flywheel, the crosshead and piston rod, the
three fluid levels, and the gauge needle are drawn by AmmoniaSynthesizerRenderer, whose constants match the geometry
here. Run from the repository root:

    python tools/blockmodels/ammonia_synthesizer.py
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
TEXTURES = ASSETS / "textures/block/ammonia_synthesizer"
TEX = "rngtech:block/ammonia_synthesizer/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))

# The front faces north (z = 0). West (x = 0) is on the viewer's right.
# Converter vessel: a vertical octagon around (CONV_X, CONV_Z).
CONV_X, CONV_Z, CONV_R = 9.25, 5.25, 3.25
CONV_Y0, CONV_Y1 = 2.0, 12.5
FLANGE_R = 3.75
# Armoured sight port on the converter's front flat, one window per catalyst bed, top bed first in the gas flow.
PORT_X0, PORT_X1 = 8.25, 10.25
WINDOWS = ((5.0, 6.5), (7.0, 8.5), (9.0, 10.5))
BAR = 0.5
# Compressor: crankcase on the west, crankshaft along z, flywheel on the front, cylinder standing on a crosshead guide.
CRANKCASE = ((0.75, 2.0, 2.0), (5.25, 7.0, 10.5))
FLY_X, FLY_Y, FLY_R = 3.0, 4.75, 2.25
FLY_Z0, FLY_Z1 = 0.75, 1.5
CYL_X, CYL_Z, CYL_R = 3.0, 6.25, 2.0
CYL_Y0, CYL_Y1 = 9.75, 12.75
GUIDE_Y0, GUIDE_Y1 = 7.0, 9.25
CROSSHEAD_Y0, CROSSHEAD_H = 7.25, 0.75
# Feed sight glasses, Nitrogen in front of Hydrogen, rails painted on the glass texture.
FEED_X0, FEED_X1 = 1.0, 3.0
NITROGEN_Z = (11.0, 13.0)
HYDROGEN_Z = (13.5, 15.5)
SIGHT_Y0, SIGHT_Y1 = 3.5, 11.0
HEADER_Y0, HEADER_Y1 = 14.0, 14.75
# Drive cabinet at the back, with the condenser on top and the cable port sized to the Universal Connector ring.
CABINET = ((3.875, 2.0, 11.0), (12.125, 12.5, 15.5))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
CONDENSER_Y0, CONDENSER_Y1 = 13.0, 13.75
CONDENSER_X0, CONDENSER_X1 = 4.5, 11.5
FRONT_HEADER = (11.25, 12.0)
BACK_HEADER = (14.5, 15.25)
TUBES = (5.0, 6.25, 7.5, 8.75, 10.0, 11.0)
# Ammonia receiver at the back east, the same framed tank as the Fuel Cell's.
RECEIVER = ((12.5, 3.0, 11.5), (15.25, 10.5, 15.5))
POST = 0.5
# Instrument stand at the front east: high-pressure gauge and status lamp.
DIAL = ((13.0, 8.0, 1.0), (15.5, 10.5, 2.75))


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


def vertical_octagon(cx, cz, radius, y0, y1, side, end=None, ends=("up", "down")):
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
        elements.append(box((cx - radius, y0, cz - radius), (cx + radius, y1, cz + radius), end, faces=ends,
                            uv={face: FULL for face in ends}))
    return elements


def bolt_ring(cx, cz, radius, y0, y1, size=0.6, count=8, offset=22.5):
    elements = []
    for i in range(count):
        angle = math.radians(offset + i * 360.0 / count)
        bx, bz = cx + math.cos(angle) * radius, cz + math.sin(angle) * radius
        elements.append(box((bx - size / 2, y0, bz - size / 2), (bx + size / 2, y1, bz + size / 2), "bolt",
                            faces=("north", "south", "east", "west", "up")))
    return elements


# Feed sight glasses map a framed strip of the texture onto each 2 px face, so the rails sit on the glass edges.
SIGHT_UV = {face: [0, 0, 4, 16] for face in SIDES}


def feed_glass(z0, z1):
    return [
        box((FEED_X0 - 0.25, 2.0, z0 - 0.25), (FEED_X1 + 0.25, SIGHT_Y0, z1 + 0.25), "flange"),
        box((FEED_X0, SIGHT_Y0, z0), (FEED_X1, SIGHT_Y1, z1), "sight_glass", faces=SIDES, uv=SIGHT_UV),
        box((FEED_X0 - 0.25, SIGHT_Y1, z0 - 0.25), (FEED_X1 + 0.25, SIGHT_Y1 + 0.75, z1 + 0.25), "cap"),
    ]


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


def converter_elements():
    elements = []
    cx, cz = CONV_X, CONV_Z
    # Shell, the bottom flange on the plinth, and the teal band.
    elements += vertical_octagon(cx, cz, CONV_R, CONV_Y0, CONV_Y1, "vessel")
    elements += vertical_octagon(cx, cz, FLANGE_R, 2.0, 2.75, "flange", end="flange_end", ends=("up",))
    elements += vertical_octagon(cx, cz, CONV_R + 0.25, 11.25, 12.0, "band", end="band_end", ends=("up", "down"))
    # Body and head flanges, clamped by a ring of studs, under a domed head with the outlet nozzle.
    elements += vertical_octagon(cx, cz, FLANGE_R, CONV_Y1 - 0.25, CONV_Y1 + 0.5, "flange", end="flange_end", ends=("down",))
    elements += vertical_octagon(cx, cz, FLANGE_R, CONV_Y1 + 0.5, CONV_Y1 + 1.25, "flange", end="flange_end", ends=("up",))
    elements += bolt_ring(cx, cz, 3.15, CONV_Y1 + 1.25, CONV_Y1 + 1.75)
    elements += vertical_octagon(cx, cz, 2.5, CONV_Y1 + 1.25, CONV_Y1 + 2.0, "vessel", end="head", ends=("up",))
    elements += vertical_octagon(cx, cz, 1.5, CONV_Y1 + 2.0, CONV_Y1 + 2.5, "flange", end="cap", ends=("up",))
    # Outlet line: up from the nozzle, back over the cabinet, and down into the condenser's front header.
    elements.append(box((cx - 0.5, CONV_Y1 + 2.5, cz - 0.5), (cx + 0.5, 15.75, cz + 0.5), "pipe", faces=SIDES))
    elements.append(box((cx - 0.5, 15.0, cz - 0.5), (cx + 0.5, 16.0, FRONT_HEADER[1]), "pipe"))
    elements.append(box((cx - 0.5, CONDENSER_Y1, FRONT_HEADER[0]), (cx + 0.5, 15.0, FRONT_HEADER[1]), "pipe", faces=SIDES))

    # Armoured sight port: a dark liner behind the windows, glass, and a heavy frame with corner bolts.
    front = cz - CONV_R
    top = WINDOWS[-1][1] + BAR
    bottom = WINDOWS[0][0] - BAR
    elements.append(box((PORT_X0, bottom, front - 0.05), (PORT_X1, top, front), "liner", faces=("north",), uv={"north": FULL}))
    for y0, y1 in WINDOWS:
        elements.append(box((PORT_X0, y0, front - 0.4), (PORT_X1, y1, front - 0.4), "glass", faces=("north",)))
    # The frame runs back into the shell so no gap shows where it overhangs the diagonal flats.
    frame_z = (front - 0.65, front + 0.5)
    for x0, x1 in ((PORT_X0 - 0.75, PORT_X0), (PORT_X1, PORT_X1 + 0.75)):
        elements.append(box((x0, bottom, frame_z[0]), (x1, top, frame_z[1]), "armour"))
    bars = [(WINDOWS[0][0] - BAR, WINDOWS[0][0])] + [(a[1], b[0]) for a, b in zip(WINDOWS, WINDOWS[1:])] + [(WINDOWS[-1][1], top)]
    for y0, y1 in bars:
        elements.append(box((PORT_X0, y0, frame_z[0]), (PORT_X1, y1, frame_z[1]), "armour"))
    for bx in (PORT_X0 - 0.375, PORT_X1 + 0.375):
        for by in (bottom + 0.3, top - 0.3):
            elements.append(box((bx - 0.2, by - 0.2, frame_z[0] - 0.15), (bx + 0.2, by + 0.2, frame_z[0]), "bolt",
                                faces=("north", "east", "west", "up", "down")))
    return elements


def compressor_elements():
    elements = []
    (kx0, ky0, kz0), (kx1, ky1, kz1) = CRANKCASE
    elements.append(box((kx0, ky0, kz0), (kx1, ky1, kz1), {"north": "crankcase_front", "*": "crankcase"},
                        uv={"north": FULL}))
    # Inspection cover on the west side, and the crankshaft bearing boss behind the flywheel.
    elements.append(box((kx0 - 0.25, 3.0, 3.5), (kx0, 6.0, 9.0), "cover", faces=("north", "south", "west", "up", "down")))
    elements.append(box((FLY_X - 0.75, FLY_Y - 0.75, FLY_Z1), (FLY_X + 0.75, FLY_Y + 0.75, kz0), "flange"))
    # Open crosshead guide: four posts between the crankcase and the cylinder's gland flange.
    for px in (CYL_X - 1.5, CYL_X + 1.0):
        for pz in (CYL_Z - 1.5, CYL_Z + 1.0):
            elements.append(box((px, GUIDE_Y0, pz), (px + 0.5, GUIDE_Y1, pz + 0.5), "rod", faces=SIDES))
    elements.append(box((CYL_X - 1.75, GUIDE_Y0, CYL_Z - 1.75), (CYL_X + 1.75, GUIDE_Y0 + 0.25, CYL_Z + 1.75), "flange"))
    # Cylinder: a gland flange, the finned barrel, and the head with two valve covers.
    elements += vertical_octagon(CYL_X, CYL_Z, CYL_R + 0.25, GUIDE_Y1, CYL_Y0, "flange", end="flange_end", ends=("down",))
    elements += vertical_octagon(CYL_X, CYL_Z, CYL_R, CYL_Y0, CYL_Y1, "cylinder")
    elements += vertical_octagon(CYL_X, CYL_Z, CYL_R + 0.25, CYL_Y1, CYL_Y1 + 0.75, "flange", end="cylinder_head", ends=("up",))
    for vz in (CYL_Z - 1.0, CYL_Z + 0.25):
        elements.append(box((CYL_X + 0.25, CYL_Y1 + 0.75, vz), (CYL_X + 1.25, CYL_Y1 + 1.25, vz + 0.75), "bolt",
                            faces=("north", "south", "east", "west", "up")))
    # Discharge: a check valve body from the cylinder into the converter's west flat.
    elements.append(box((CYL_X + CYL_R - 0.1, 10.25, CYL_Z - 0.75), (CONV_X - CONV_R + 0.1, 11.5, CYL_Z + 0.5), "pipe",
                        faces=("north", "south", "up", "down")))
    elements.append(box((5.25, 9.75, CYL_Z - 1.0), (5.85, 12.0, CYL_Z + 0.75), "flange"))
    return elements


def feed_elements():
    elements = []
    elements += feed_glass(*NITROGEN_Z)
    elements += feed_glass(*HYDROGEN_Z)
    mid = (FEED_X0 + FEED_X1) / 2
    lid = SIGHT_Y1 + 0.75
    # Risers from both lids into the suction header, joined at the mixing tee over the Nitrogen glass.
    for z0, z1 in (NITROGEN_Z, HYDROGEN_Z):
        zc = (z0 + z1) / 2
        elements.append(box((mid - 0.4, lid, zc - 0.4), (mid + 0.4, HEADER_Y0, zc + 0.4), "pipe", faces=SIDES))
    hz = sum(HYDROGEN_Z) / 2
    elements.append(box((mid - 0.5, HEADER_Y0, CYL_Z - 0.5), (mid + 0.5, HEADER_Y1, hz + 0.5), "pipe"))
    nz = sum(NITROGEN_Z) / 2
    elements.append(box((mid - 0.75, HEADER_Y0 - 0.25, nz - 0.75), (mid + 0.75, HEADER_Y1 + 0.25, nz + 0.75), "tee"))
    elements.append(box((mid - 0.4, CYL_Y1 + 0.75, CYL_Z - 0.4), (mid + 0.4, HEADER_Y0, CYL_Z + 0.4), "pipe", faces=SIDES))
    return elements


def cabinet_elements():
    elements = []
    elements.append(box(*CABINET, {"north": "cabinet", "south": "cabinet", "*": "casing"}))
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CABINET
    elements.append(box((cx0 - 0.125, 10.75, cz0 - 0.125), (cx1 + 0.125, 11.25, cz1 + 0.125), "band", faces=SIDES))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))
    # Condenser: frosted tubes between a front and a back header, on feet above the cabinet top.
    for z0, z1 in (FRONT_HEADER, BACK_HEADER):
        elements.append(box((CONDENSER_X0, CONDENSER_Y0, z0), (CONDENSER_X1, CONDENSER_Y1, z1), "frost"))
        for fx in (CONDENSER_X0 + 0.25, CONDENSER_X1 - 0.75):
            elements.append(box((fx, cy1, z0 + 0.125), (fx + 0.5, CONDENSER_Y0, z1 - 0.125), "flange", faces=SIDES))
    for tx in TUBES:
        elements.append(box((tx - 0.25, CONDENSER_Y0 + 0.125, FRONT_HEADER[1]), (tx + 0.25, CONDENSER_Y1 - 0.125, BACK_HEADER[0]),
                            "frost", faces=("east", "west", "up", "down")))
    # Drain from the back header into the receiver lid.
    (rx0, _, _), (rx1, ry1, _) = RECEIVER
    lid = ry1 + 0.75
    dx = (rx0 + rx1) / 2
    elements.append(box((CONDENSER_X1, CONDENSER_Y0, BACK_HEADER[0]), (dx + 0.5, CONDENSER_Y1, BACK_HEADER[1]), "frost",
                        faces=("north", "south", "east", "up", "down")))
    elements.append(box((dx - 0.5, lid, BACK_HEADER[0]), (dx + 0.5, CONDENSER_Y0, BACK_HEADER[1]), "frost", faces=SIDES))
    elements += framed_glass(RECEIVER, 2.0, lid)
    return elements


def instrument_elements():
    elements = []
    (dx0, dy0, dz0), (dx1, dy1, dz1) = DIAL
    elements.append(box((13.5, 2.0, 1.5), (15.0, dy0, 3.0), "casing"))
    elements.append(box((dx0, dy0, dz0), (dx1, dy1, dz1), {"north": "gauge", "*": "casing"}, uv={"north": FULL}))
    # Impulse line from the gauge back to the converter shell.
    elements.append(box((13.875, 9.0, dz1), (14.375, 9.5, 4.5), "pipe", faces=("east", "west", "up", "down")))
    elements.append(box((CONV_X + CONV_R - 0.5, 9.0, 4.0), (14.375, 9.5, 4.5), "pipe", faces=("north", "south", "up", "down")))
    elements.append(box(
        (13.75, 5.75, 1.0), (14.75, 6.75, 1.5), "lamp",
        faces=("north", "east", "west", "up", "down"),
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def housing_elements():
    elements = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    elements += converter_elements()
    elements += compressor_elements()
    elements += feed_elements()
    elements += cabinet_elements()
    elements += instrument_elements()
    return elements


def flywheel_segment(angle, x0, y0, z0, x1, y1, z1):
    """A box around the flywheel centre, turned by a multiple of 45 degrees about the shaft."""
    if angle in (0, 180):
        return box((x0, y0, z0), (x1, y1, z1), "flywheel")
    if angle in (90, 270):
        # A quarter turn swaps the box's x and y extents about the centre.
        cx, cy = FLY_X, FLY_Y
        return box((cx - (y1 - cy), cy + (x0 - cx), z0), (cx - (y0 - cy), cy + (x1 - cx), z1), "flywheel")
    origin = [r(FLY_X), r(FLY_Y), r((z0 + z1) / 2)]
    return box((x0, y0, z0), (x1, y1, z1), "flywheel", rotation={"origin": origin, "axis": "z", "angle": 45 if angle in (45, 225) else -45})


def item_extra_elements():
    """Static flywheel, crosshead, and piston rod for the item model, which has no renderer. No catalyst is installed."""
    elements = []
    half = FLY_R * TAN
    rim = 0.6
    for angle in (0, 45, 90, 135):
        for sign in (1, -1):
            y0, y1 = (FLY_Y + FLY_R - rim, FLY_Y + FLY_R) if sign > 0 else (FLY_Y - FLY_R, FLY_Y - FLY_R + rim)
            elements.append(flywheel_segment(angle if sign > 0 else (angle + 180) % 360,
                                             FLY_X - half, y0, FLY_Z0, FLY_X + half, y1, FLY_Z1))
    spoke = 0.25
    elements.append(box((FLY_X - spoke, FLY_Y - FLY_R + rim, FLY_Z0 + 0.15), (FLY_X + spoke, FLY_Y + FLY_R - rim, FLY_Z1 - 0.15), "flywheel"))
    elements.append(box((FLY_X - FLY_R + rim, FLY_Y - spoke, FLY_Z0 + 0.15), (FLY_X + FLY_R - rim, FLY_Y + spoke, FLY_Z1 - 0.15), "flywheel"))
    elements.append(box((FLY_X - 0.5, FLY_Y - 0.5, FLY_Z0 - 0.25), (FLY_X + 0.5, FLY_Y + 0.5, FLY_Z1 + 0.1), "bolt"))
    elements.append(box((CYL_X - 0.9, CROSSHEAD_Y0, CYL_Z - 0.9), (CYL_X + 0.9, CROSSHEAD_Y0 + CROSSHEAD_H, CYL_Z + 0.9), "crosshead"))
    elements.append(box((CYL_X - 0.25, CROSSHEAD_Y0 + CROSSHEAD_H, CYL_Z - 0.25), (CYL_X + 0.25, GUIDE_Y1, CYL_Z + 0.25), "rod", faces=SIDES))
    return elements


HOUSING_TEXTURES = {
    "particle": TEX + "vessel",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "vessel": TEX + "vessel",
    "head": TEX + "head",
    "armour": TEX + "armour",
    "liner": TEX + "liner",
    "glass": TEX + "glass",
    "sight_glass": TEX + "sight_glass",
    "band": TEX + "band",
    "band_end": TEX + "band_end",
    "flange": TEX + "flange",
    "flange_end": TEX + "flange_end",
    "cap": TEX + "cap",
    "bolt": TEX + "bolt",
    "rod": TEX + "rod",
    "crankcase": TEX + "crankcase",
    "crankcase_front": TEX + "crankcase_front",
    "cover": TEX + "cover",
    "cylinder": TEX + "cylinder",
    "cylinder_head": TEX + "cylinder_head",
    "pipe": TEX + "pipe",
    "tee": TEX + "tee",
    "frost": TEX + "frost",
    "cabinet": TEX + "cabinet",
    "port": TEX + "port",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/ammonia_synthesizer.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/ammonia_synthesizer_active.json", {
        "parent": "rngtech:block/ammonia_synthesizer",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "item/ammonia_synthesizer.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, "flywheel": TEX + "flywheel", "crosshead": TEX + "crosshead"},
        "elements": housing + item_extra_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"ammonia_synthesizer:{name}")


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
STEEL_BLUE = (82, 96, 112)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
SCALE = (222, 226, 210, 255)
BOLT = (150, 158, 168)
COPPER = (184, 115, 51)
FROST = (176, 196, 210)


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

    # Converter shell: heavy forged steel, slightly heat-tinted toward the catalyst beds, with forged-ring seams.
    vessel = Image.new("RGBA", (64, 64))
    pixels = vessel.load()
    rand = rng("vessel")
    rows = [rand.uniform(-4, 4) for _ in range(64)]
    for y in range(64):
        heat = max(0.0, 1.0 - abs(y - 30) / 26.0) * 0.35
        base = mix((90, 106, 124), (116, 100, 92), heat)
        for x in range(64):
            pixels[x, y] = (*shade(base, rand.uniform(-5, 5) + rows[y]), 255)
    for y in range(64):
        for x in range(64):
            if y % 12 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -26), 255)
            elif y % 12 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 16), 255)
    save(vessel, "vessel")

    # Domed head: rings shading toward a bright crown, cut to the octagon.
    head = Image.new("RGBA", (32, 32))
    pixels = head.load()
    rand = rng("head")
    for y in range(32):
        for x in range(32):
            distance = math.hypot(x - 15.5, y - 15.5) / 16.0
            base = mix((126, 138, 152), STEEL_BLUE, min(1.0, distance))
            pixels[x, y] = (*shade(base, rand.uniform(-4, 4)), 255)
    save(octagon_mask(head), "head")

    # Sight-port armour: dark heavy steel with a bevel highlight.
    armour = noisy(64, (74, 82, 94), "armour", amount=4)
    pixels = armour.load()
    for y in range(64):
        for x in range(64):
            if x % 3 == 0 and y % 3 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -14), 255)
    save(armour, "armour")
    # Empty catalyst basket: a dark liner behind a wire mesh, so an empty bed still reads as a basket.
    liner = noisy(16, (22, 22, 24), "liner", amount=3)
    pixels = liner.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                pixels[x, y] = (*shade((70, 74, 80), rng("mesh").uniform(-6, 6)), 255)
    save(liner, "liner")

    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    for offset in (0, 32):
        draw.line([(offset + 18, 14), (offset + 26, 6)], fill=GLINT)
        draw.line([(offset + 20, 19), (offset + 23, 16)], fill=GLINT)
    for offset in (0, 32):
        draw.line([(offset + 4, 46), (offset + 12, 38)], fill=GLINT)
    save(glass, "glass")

    # Feed sight glass: clear between steel rails, with level ticks on one rail and a glint. SIGHT_UV maps columns 0-15.
    sight = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = sight.load()
    for y in range(64):
        for x in (0, 1, 14, 15):
            edge = x in (0, 15)
            pixels[x, y] = (*shade(STEEL, 14 if edge else -12), 255)
        if y % 8 == 4:
            pixels[2, y] = SCALE
            pixels[3, y] = SCALE
        elif y % 16 == 12:
            pixels[2, y] = SCALE
    for y in range(6, 58):
        if (y // 6) % 3 != 2:
            pixels[10, y] = GLINT
    save(sight, "sight_glass")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")
    save(octagon_mask(noisy(32, TEAL, "band_end", amount=6), inner=0.9), "band_end")

    save(noisy(64, STEEL, "flange", amount=5), "flange")
    flange_end = noisy(32, STEEL, "flange_end", amount=5)
    pixels = flange_end.load()
    for y in range(32):
        for x in range(32):
            u, v = abs(x - 15.5), abs(y - 15.5)
            if max(u, v) > 14.5 or u + v > 21.5:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
    save(octagon_mask(flange_end), "flange_end")

    # Lids: dark steel with a bolt circle on every 16-texel tile, as on the Fuel Cell.
    cap = noisy(64, (64, 72, 84), "cap", amount=4)
    pixels = cap.load()
    for py in range(0, 64, 16):
        for px in range(0, 64, 16):
            for angle in range(0, 360, 60):
                x = px + 8 + round(math.cos(math.radians(angle)) * 5)
                y = py + 8 + round(math.sin(math.radians(angle)) * 5)
                pixels[x % 64, y % 64] = (*shade(BOLT, 10), 255)
    save(cap, "cap")

    save(noisy(64, BOLT, "bolt", amount=8), "bolt")
    save(noisy(64, (150, 158, 168), "rod", amount=5, streak=8), "rod")

    # Crankcase: dark cast iron with seams; its front carries the bearing housing behind the flywheel.
    save(panel_seams(noisy(64, (52, 58, 66), "crankcase", amount=5), rivets=False), "crankcase")
    front = noisy(64, (52, 58, 66), "crankcase_front", amount=5)
    draw = ImageDraw.Draw(front)
    draw.rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
    for x, y in ((4, 4), (57, 4), (4, 57), (57, 57)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(*BOLT, 255))
    save(front, "crankcase_front")
    cover = noisy(64, (64, 72, 82), "cover", amount=4)
    pixels = cover.load()
    for y in range(0, 64, 6):
        for x in range(0, 64, 6):
            pixels[x, y] = (*shade(BOLT, 6), 255)
    save(cover, "cover")

    # Compressor barrel: steel with horizontal cooling fins.
    cylinder = noisy(64, (88, 98, 110), "cylinder", amount=4)
    pixels = cylinder.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(SLATE_DARK, -4), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 22), 255)
    save(cylinder, "cylinder")
    cylinder_head = noisy(32, STEEL, "cylinder_head", amount=5)
    pixels = cylinder_head.load()
    for angle in range(0, 360, 45):
        x = 16 + round(math.cos(math.radians(angle + 22.5)) * 12)
        y = 16 + round(math.sin(math.radians(angle + 22.5)) * 12)
        pixels[x, y] = (*shade(BOLT, 20), 255)
        pixels[x - 1, y] = (*shade(BOLT, -30), 255)
    save(octagon_mask(cylinder_head), "cylinder_head")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")
    tee = noisy(64, (110, 120, 132), "tee", amount=5)
    pixels = tee.load()
    for y in range(64):
        for x in range(64):
            if x % 6 == 0 or y % 6 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(tee, "tee")

    # Condenser: frosted steel with rime flecks and pale streaks.
    frost = noisy(64, FROST, "frost", amount=6, streak=4)
    pixels = frost.load()
    rand = rng("frost_flecks")
    for _ in range(420):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((236, 244, 250), rand.uniform(-8, 4)), 255)
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((136, 156, 172), rand.uniform(-6, 6)), 255)
    save(frost, "frost")

    # Cabinet: seamed casing with louvred vents, as on the Fuel Cell.
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
        draw.rectangle([x, y, x + 2, y + 2], fill=(*COPPER, 255))
    save(port, "port")

    # High-pressure gauge: a cream dial whose scale runs green to a red line. The renderer draws the needle.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=SCALE)
    for step in range(9):
        angle = 135 + step * 33.75
        color = (70, 150, 80) if step < 5 else (200, 160, 50) if step < 7 else (196, 52, 40)
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    draw.arc([4, 4, 11, 11], 15, 45, fill=(196, 52, 40, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    gauge.putpixel((8, 8), (30, 30, 30, 255))
    save(gauge, "gauge")

    save(noisy(64, (18, 52, 34), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (128, 240, 120), "lamp_on", amount=6), "lamp_on")

    # Flywheel: machined steel, so it stands out against the cast crankcase. The renderer reuses it and the crosshead.
    save(noisy(64, (150, 158, 168), "flywheel", amount=6, streak=4), "flywheel")
    crosshead = noisy(64, (132, 140, 150), "crosshead", amount=5, streak=4)
    pixels = crosshead.load()
    for y in range(64):
        for x in range(64):
            if y % 16 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -30), 255)
    save(crosshead, "crosshead")

    # Renderer textures. The catalyst is promoted iron granules, one 16-texel-tall strip per bed, and the glow mask
    # brightens the same granules so the beds warm from within.
    catalyst = Image.new("RGBA", (16, 16))
    glow = Image.new("RGBA", (16, 16))
    cat_pixels = catalyst.load()
    glow_pixels = glow.load()
    rand = rng("catalyst")
    for y in range(16):
        for x in range(16):
            granule = (x // 2 + (y // 2) * 3 + rand.randrange(3)) % 4
            edge = x % 2 == 0 and y % 2 == 0
            base = ((48, 42, 40), (70, 56, 48), (96, 66, 46), (58, 54, 56))[granule]
            cat_pixels[x, y] = (*shade(base, -14 if edge else rand.uniform(-6, 6)), 255)
            strength = (0.25 if edge else 0.7 + 0.3 * (granule / 3.0)) * rand.uniform(0.8, 1.0)
            glow_pixels[x, y] = (clamp(255 * strength), clamp(255 * strength), clamp(255 * strength), 255)
    save(catalyst, "fx/catalyst")
    save(glow, "fx/glow")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
