#!/usr/bin/env python3
"""Generate the Cavitation Generator block model, rotor models, and owned textures.

The housing is a static block model; the rotor, chamber glow, heat glow, and tank
levels are drawn by CavitationGeneratorRenderer, whose constants match the
geometry here. Run from the repository root:

    python tools/blockmodels/cavitation_generator.py
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
TEXTURES = ASSETS / "textures/block/cavitation_generator"
TEX = "rngtech:block/cavitation_generator/"

# Chamber axis runs front to back through (CX, CY); the front faces north (z = 0).
CX, CY = 8.0, 9.0
CHAMBER_R = 5.5
COLLAR_R = 6.0
FLANGE_OUTER_R = 6.5
FLANGE_INNER_R = 4.5
TAN = math.tan(math.radians(22.5))
FACES = ("north", "south", "east", "west", "up", "down")

ROTORS = {
    "steel": (138, 147, 156),
    "titanium": (184, 196, 207),
    "nitrogen_extraction": (168, 224, 240),
    "tungstensteel": (75, 85, 102),
    "aethergold": (226, 179, 60),
    "nullite": (78, 54, 112),
}


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


def octagon_prism(radius, z0, z1, textures, faces=FACES):
    """A regular octagon around the chamber axis: two crossed slabs plus four 45-degree corner squares."""
    half = radius * TAN
    elements = [
        box((CX - radius, CY - half, z0), (CX + radius, CY + half, z1), textures, faces),
        box((CX - half, CY - radius, z0), (CX + half, CY + radius, z1), textures, faces),
    ]
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx, cy = CX + sx * half, CY + sy * half
            elements.append(box(
                (cx - half, cy - half, z0), (cx + half, cy + half, z1), textures, faces,
                rotation={"origin": [r(cx), r(cy), r((z0 + z1) / 2)], "axis": "z", "angle": 45},
            ))
    return elements


def octagon_ring(outer, inner, z0, z1, textures):
    """A flat octagonal ring: four straight bars plus four diagonal bars."""
    half = outer * TAN
    depth = outer - inner
    elements = [
        box((CX - half, CY + inner, z0), (CX + half, CY + outer, z1), textures),
        box((CX - half, CY - outer, z0), (CX + half, CY - inner, z1), textures),
        box((CX + inner, CY - half, z0), (CX + outer, CY + half, z1), textures),
        box((CX - outer, CY - half, z0), (CX - inner, CY + half, z1), textures),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx, cy = CX + sx * mid, CY + sy * mid
            # The bar's long side lies along the chamfer; pick its axis so +45 degrees aligns it.
            if sx * sy > 0:
                start, end = (cx - half, cy - depth / 2, z0), (cx + half, cy + depth / 2, z1)
            else:
                start, end = (cx - depth / 2, cy - half, z0), (cx + depth / 2, cy + half, z1)
            elements.append(box(start, end, textures, rotation={"origin": [r(cx), r(cy), r((z0 + z1) / 2)], "axis": "z", "angle": -45}))
    return elements


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))
    # Cradles under the chamber.
    for z0, z1 in ((3.5, 5.0), (9.0, 10.5)):
        elements.append(box((3.5, 2, z0), (12.5, 5, z1), "casing"))
    # Pressure chamber; its front face is the cavity back wall behind the rotor.
    elements += octagon_prism(CHAMBER_R, 3.0, 11.0, {"north": "cavity", "*": "chamber"})
    # Teal pressure collar.
    elements += octagon_prism(COLLAR_R, 6.5, 7.5, "band", faces=("north", "south", "east", "west", "up", "down"))
    # Front flange ring around the porthole.
    elements += octagon_ring(FLANGE_OUTER_R, FLANGE_INNER_R, 0.5, 3.0, "flange")
    # Flange bolts.
    for angle in range(0, 360, 45):
        bx = CX + math.cos(math.radians(angle)) * 5.5
        by = CY + math.sin(math.radians(angle)) * 5.5
        elements.append(box((bx - 0.5, by - 0.5, 0.0), (bx + 0.5, by + 0.5, 0.5), "bolt", faces=("north", "east", "west", "up", "down")))
    # Porthole glass, flush inside the flange.
    elements.append(box(
        (CX - FLANGE_INNER_R, CY - FLANGE_INNER_R, 1.0), (CX + FLANGE_INNER_R, CY + FLANGE_INNER_R, 1.0),
        "glass", faces=("north", "south"),
    ))
    # Rear motor with heat-sink fins.
    elements.append(box((3.5, 3.0, 11.0), (12.5, 12.0, 15.0), "motor"))
    for x in FIN_XS:
        elements.append(box((x, 12.0, 11.5), (x + 0.5, 15.0, 14.75), "fins"))
    # Rear power bus.
    elements.append(box((2.0, 2.0, 15.0), (14.0, 13.0, 16.0), {"south": "bus", "*": "casing"}))
    # Sight glasses: Water on the west, output on the east.
    for x0 in (0.5, 13.5):
        x1 = x0 + 2.0
        elements.append(box((x0 - 0.25, 2.0, 8.0), (x1 + 0.25, 3.5, 11.0), "pipe"))
        elements.append(box((x0 - 0.25, 12.0, 8.0), (x1 + 0.25, 13.5, 11.0), "pipe"))
        elements.append(box((x0, 3.5, 8.25), (x1, 12.0, 10.75), "glass_tube", faces=("north", "south", "east", "west")))
        inner_x0, inner_x1 = (x1, 4.5) if x0 < 8 else (11.5, x0)
        elements.append(box((inner_x0, 12.25, 9.0), (inner_x1, 13.25, 10.0), "pipe", faces=("north", "south", "up", "down")))
    # Pressure dial and status lamp on the plinth front.
    elements.append(box((0.75, 2.0, 0.25), (3.25, 4.5, 1.25), {"north": "gauge", "*": "casing"}, uv={"north": [0, 0, 16, 16]}))
    elements.append(box(
        (11.75, 2.0, 0.5), (13.75, 3.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


FIN_XS = [4.0 + i * 1.25 for i in range(7)]


def rotor_elements():
    """Four pitched main blades, four flat splitter vanes, and an octagonal hub, centered on the chamber axis."""
    elements = []
    z0, z1 = 2.0, 2.3
    zc = (z0 + z1) / 2
    width = 1.1
    blades = [
        ((CX + 1.0, CY - width, z0), (CX + 4.3, CY + width, z1), "x", 22.5),
        ((CX - 4.3, CY - width, z0), (CX - 1.0, CY + width, z1), "x", -22.5),
        ((CX - width, CY + 1.0, z0), (CX + width, CY + 4.3, z1), "y", 22.5),
        ((CX - width, CY - 4.3, z0), (CX + width, CY - 1.0, z1), "y", -22.5),
    ]
    for start, end, axis, angle in blades:
        origin = [r((start[0] + end[0]) / 2), r((start[1] + end[1]) / 2), r(zc)]
        elements.append(box(start, end, "blade", rotation={"origin": origin, "axis": axis, "angle": angle}))
    for angle in (45, -45):
        elements.append(box((CX - 3.4, CY - 0.4, 1.85), (CX + 3.4, CY + 0.4, 2.45), "blade",
                            rotation={"origin": [CX, CY, 2.15], "axis": "z", "angle": angle}))
    hub = 1.2
    elements.append(box((CX - hub, CY - hub, 1.5), (CX + hub, CY + hub, 2.8), "hub"))
    elements.append(box((CX - hub, CY - hub, 1.5), (CX + hub, CY + hub, 2.8), "hub",
                        rotation={"origin": [CX, CY, 2.15], "axis": "z", "angle": 45}))
    elements.append(box((CX - 0.6, CY - 0.6, 1.2), (CX + 0.6, CY + 0.6, 1.5), "hub"))
    return elements


HOUSING_TEXTURES = {
    "particle": TEX + "chamber",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "chamber": TEX + "chamber",
    "cavity": TEX + "cavity",
    "band": TEX + "band",
    "flange": TEX + "flange",
    "bolt": TEX + "bolt",
    "glass": TEX + "glass",
    "glass_tube": TEX + "glass_tube",
    "motor": TEX + "motor",
    "fins": TEX + "fins",
    "bus": TEX + "bus",
    "pipe": TEX + "pipe",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/cavitation_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/cavitation_generator_active.json", {
        "parent": "rngtech:block/cavitation_generator",
        "textures": {"lamp": TEX + "lamp_on"},
    })
    write_json(MODELS / "block/cavitation_generator/rotor.json", {
        "textures": {"particle": "#blade"},
        "elements": rotor_elements(),
    })
    for material in ROTORS:
        write_json(MODELS / f"block/cavitation_generator/rotor_{material}.json", {
            "parent": "rngtech:block/cavitation_generator/rotor",
            "textures": {"blade": TEX + f"rotor/{material}", "hub": TEX + "rotor/hub"},
        })
    write_json(MODELS / "item/cavitation_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**HOUSING_TEXTURES, "blade": TEX + "rotor/steel", "hub": TEX + "rotor/hub"},
        "elements": housing + rotor_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"cavitation:{name}")


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
# Front-face UV of the chamber axis: u = 16 - x, v = 16 - y, at 4 px per unit.
AXIS_PX = ((16 - CX) * 4, (16 - CY) * 4)


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

    save(noisy(64, STEEL_BLUE, "chamber", amount=5, streak=7), "chamber")

    cavity = noisy(64, (14, 20, 30), "cavity", amount=3)
    rings(cavity, AXIS_PX, 5, 10)
    draw = ImageDraw.Draw(cavity)
    for angle in range(0, 360, 45):
        dx, dy = math.cos(math.radians(angle + 22.5)), math.sin(math.radians(angle + 22.5))
        draw.line([(AXIS_PX[0] + dx * 6, AXIS_PX[1] + dy * 6), (AXIS_PX[0] + dx * 18, AXIS_PX[1] + dy * 18)], fill=(30, 46, 62, 255))
    save(cavity, "cavity")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    save(rings(noisy(64, (98, 108, 120), "flange", amount=5), AXIS_PX, 3, -10), "flange")
    save(noisy(64, (150, 158, 168), "bolt", amount=8), "bolt")

    # Porthole glass: two short glints in the upper corner, the rest see-through (cutout).
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glass)
    draw.line([(17, 26), (26, 17)], fill=(206, 240, 250, 255))
    draw.line([(18, 31), (21, 28)], fill=(206, 240, 250, 255))
    save(glass, "glass")
    # Sight-glass tubes: one vertical glint per side face.
    tube = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = tube.load()
    for x in (23, 35, 44, 53):
        for y in range(18, 46, 1):
            if y % 7 != 0:
                pixels[x, y] = (206, 240, 250, 255)
    save(tube, "glass_tube")

    motor = noisy(64, (52, 60, 72), "motor", amount=4)
    pixels = motor.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -18), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 14), 255)
    save(motor, "motor")

    fins = noisy(64, (64, 72, 84), "fins", amount=4)
    pixels = fins.load()
    for y in range(64):
        for x in range(64):
            if y % 3 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
    save(fins, "fins")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    bus = panel_seams(noisy(64, (34, 40, 48), "bus", amount=4), rivets=False)
    pixels = bus.load()
    for top in (18, 44):
        for y in range(top, top + 5):
            for x in range(10, 54):
                pixels[x, y] = (*shade(COPPER, 26 if y == top else (-26 if y == top + 4 else 0)), 255)
    draw = ImageDraw.Draw(bus)
    draw.rectangle([26, 27, 37, 38], fill=(*TEAL_DARK, 255), outline=(*shade(TEAL, 20), 255))
    draw.rectangle([29, 30, 34, 35], fill=(12, 16, 22, 255))
    save(bus, "bus")

    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    for angle in range(-225, 46, 45):
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (40, 40, 40, 255))
    draw.line([(7.5, 7.5), (10.5, 4.5)], fill=(196, 52, 40, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    save(gauge, "gauge")

    lamp_off = noisy(64, (18, 52, 58), "lamp_off", amount=3)
    save(lamp_off, "lamp_off")
    lamp_on = noisy(64, (110, 236, 255), "lamp_on", amount=6)
    save(lamp_on, "lamp_on")

    for material, color in ROTORS.items():
        blade = noisy(64, color, f"rotor_{material}", amount=7)
        pixels = blade.load()
        for y in range(64):
            for x in range(64):
                angle = math.atan2(y + 0.5 - AXIS_PX[1], x + 0.5 - AXIS_PX[0])
                if int(math.degrees(angle) * 2) % 9 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], 18), 255)
        save(blade, f"rotor/{material}")
    hub = noisy(64, (58, 64, 72), "rotor_hub", amount=5)
    ImageDraw.Draw(hub).ellipse([AXIS_PX[0] - 2.5, AXIS_PX[1] - 2.5, AXIS_PX[0] + 2.5, AXIS_PX[1] + 2.5], fill=(*TEAL, 255))
    save(hub, "rotor/hub")

    glow = Image.new("RGBA", (64, 64))
    pixels = glow.load()
    for y in range(64):
        for x in range(64):
            distance = min(1.0, math.hypot(x + 0.5 - 32, y + 0.5 - 32) / 32)
            strength = (1.0 - distance) ** 1.6
            pixels[x, y] = (clamp(140 * strength), clamp(225 * strength), clamp(255 * strength), 255)
    save(glow, "fx/glow")

    heat = Image.new("RGBA", (16, 16))
    pixels = heat.load()
    for y in range(16):
        strength = 1.0 - y / 15 * 0.75
        for x in range(16):
            pixels[x, y] = (clamp(255 * strength), clamp(110 * strength), clamp(36 * strength), 255)
    save(heat, "fx/heat")


def write_hub_item() -> None:
    """The Cavitation Rotor Hub: an octagonal steel hub with a teal calibrated bore and four blade sockets."""
    write_json(MODELS / "item/cavitation_rotor_hub.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "rngtech:item/cavitation_rotor_hub"},
    })
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    octagon = [(5, 1), (10, 1), (14, 5), (14, 10), (10, 14), (5, 14), (1, 10), (1, 5)]
    draw.polygon(octagon, fill=(118, 128, 140, 255), outline=(40, 46, 56, 255))
    draw.line([(5, 2), (10, 2)], fill=(168, 178, 188, 255))
    draw.line([(2, 5), (2, 10)], fill=(150, 160, 170, 255))
    for x, y in ((7, 2), (7, 12), (2, 7), (12, 7)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(46, 54, 66, 255))
    draw.ellipse([5, 5, 10, 10], fill=(*TEAL_DARK, 255), outline=(30, 35, 43, 255))
    draw.rectangle([7, 7, 8, 8], fill=(*TEAL, 255))
    icon.putpixel((7, 7), (190, 240, 248, 255))
    path = ASSETS / "textures/item/cavitation_rotor_hub.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    icon.save(path)


def main() -> None:
    write_models()
    write_textures()
    write_hub_item()


if __name__ == "__main__":
    main()
