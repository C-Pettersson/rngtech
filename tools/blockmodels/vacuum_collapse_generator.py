#!/usr/bin/env python3
"""Generate the Vacuum Collapse Generator block models, item model, and owned textures.

The design is a void chamber held between two Dimensional Stabilizer pylons. The installed Void Chamber is the glass
vessel itself, capped and railed in its material; the Collapse Nozzle hangs from the catalyst feed line and injects
through the chamber's top cap; a rear control cabinet carries the cable port and the catalyst hopper. The housing is a
static block model. The installed parts (one model per material), the collapsing core, the pulsing cap rims, and the
stabilizer lens glow are drawn by VacuumCollapseGeneratorRenderer, whose constants match the geometry here.
Run from the repository root:

    python tools/blockmodels/vacuum_collapse_generator.py
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
TEXTURES = ASSETS / "textures/block/vacuum_collapse_generator"
TEX = "rngtech:block/vacuum_collapse_generator/"
PART_MODEL = "rngtech:block/vacuum_collapse_generator/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
TAN = math.tan(math.radians(22.5))

# The chamber axis; the front faces north (z = 0).
CX, CZ = 8.0, 6.5
SOCKET_Y0, SOCKET_Y1 = 2.0, 3.0
CAP_HALF = 4.0
BOTTOM_CAP = (3.0, 4.5)
TOP_CAP = (11.0, 12.5)
GLASS_R = 3.5
RAIL = 0.75
# Stabilizer pylons, west side; the east pylon mirrors them about x = 8.
MAST = ((1.0, 2.5, CZ - 1.0), (2.5, 11.5, CZ + 1.0))
HEAD = ((2.0, 6.25, CZ - 1.25), (3.75, 8.75, CZ + 1.25))
FINIAL = ((0.75, 11.5, CZ - 1.25), (2.75, 12.5, CZ + 1.25))
PAD = ((0.5, 2.0, CZ - 1.5), (3.0, 2.5, CZ + 1.5))
# Collapse Nozzle, hanging from the feed line into the top cap.
NOZZLE_COLLAR = ((CX - 1.5, 13.75, CZ - 1.5), (CX + 1.5, 14.5, CZ + 1.5))
NOZZLE_BODY = ((CX - 1.0, 13.0, CZ - 1.0), (CX + 1.0, 13.75, CZ + 1.0))
NOZZLE_TIP = ((CX - 0.5, 12.5, CZ - 0.5), (CX + 0.5, 13.0, CZ + 0.5))
CABINET = ((2.0, 2.0, 11.5), (14.0, 12.0, 15.5))
HOPPER = ((5.0, 12.0, 12.0), (11.0, 16.0, 15.5))
# Cable port on the back of the cabinet, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))

CHAMBER_MATERIALS = ("tungstensteel", "nullite", "exotic")
NOZZLE_MATERIALS = ("steel", "titanium", "tungstensteel", "nullite", "exotic")
# Body colour and glow colour per material, matching the part item textures. The renderer's glow colours match these.
MATERIALS = {
    "steel": ((116, 123, 125), (196, 204, 208)),
    "titanium": ((142, 160, 183), (182, 200, 223)),
    "tungstensteel": ((74, 82, 93), (124, 174, 255)),
    "nullite": ((52, 37, 76), (160, 92, 255)),
    "exotic": ((45, 140, 116), (70, 255, 182)),
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


def mirror(start, end):
    """The same box reflected about x = 8."""
    return (16.0 - end[0], start[1], start[2]), (16.0 - start[0], end[1], end[2])


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


FULL = [0, 0, 16, 16]


def housing_elements():
    elements = []
    # Plinth with a teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))
    # Chamber socket with a teal band; an empty socket means no Void Chamber is installed.
    elements.append(box((CX - 4.5, SOCKET_Y0, CZ - 4.5), (CX + 4.5, SOCKET_Y1, CZ + 4.5), "flange"))
    elements.append(box((CX - 4.75, 2.25, CZ - 4.75), (CX + 4.75, 2.75, CZ + 4.75), "band", faces=SIDES))
    # Stabilizer pads on the plinth.
    for start, end in (PAD, mirror(*PAD)):
        elements.append(box(start, end, "flange"))
    # Control cabinet with a teal band, the catalyst hopper on top, and the cable port on its back.
    elements.append(box(*CABINET, {"north": "cabinet", "south": "cabinet", "*": "casing"}))
    (kx0, _, kz0), (kx1, _, kz1) = CABINET
    elements.append(box((kx0 - 0.25, 9.5, kz0 - 0.25), (kx1 + 0.25, 10.25, kz1 + 0.25), "band", faces=SIDES))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, uv={"south": FULL}))
    elements.append(box(*HOPPER, {"up": "hopper_top", "*": "hopper"}, uv={"up": FULL}))
    # Feed line from the hopper forward to the nozzle mount over the chamber.
    elements.append(box((CX - 1.0, 14.75, CZ - 1.0), (CX + 1.0, 15.75, HOPPER[0][2]), "pipe",
                        faces=("north", "east", "west", "up", "down")))
    elements.append(box((CX - 1.75, 14.5, CZ - 1.75), (CX + 1.75, 14.75, CZ + 1.75), "flange"))
    # Instability dial and status lamp on the plinth front.
    elements.append(box((0.75, 2.0, 0.25), (3.25, 4.5, 1.25), {"north": "gauge", "*": "casing"}, uv={"north": FULL}))
    elements.append(box(
        (12.75, 2.0, 0.5), (14.25, 3.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def chamber_elements():
    """The Void Chamber: a glass vessel between two caps, framed by corner rails, all in the chamber's material."""
    elements = []
    y0, y1 = BOTTOM_CAP[1], TOP_CAP[0]
    for cy0, cy1 in (BOTTOM_CAP, TOP_CAP):
        elements.append(box((CX - CAP_HALF, cy0, CZ - CAP_HALF), (CX + CAP_HALF, cy1, CZ + CAP_HALF), "part"))
    elements += glass_column(CX, CZ, GLASS_R, y0, y1, "glass")
    for sx in (-1, 1):
        for sz in (-1, 1):
            x0 = CX + sx * CAP_HALF if sx < 0 else CX + CAP_HALF - RAIL
            z0 = CZ + sz * CAP_HALF if sz < 0 else CZ + CAP_HALF - RAIL
            elements.append(box((x0, y0, z0), (x0 + RAIL, y1, z0 + RAIL), "part", faces=SIDES))
    return elements


def stabilizer_elements():
    """Two Dimensional Stabilizer pylons; each diamond emitter head turns its lens on the chamber."""
    elements = []
    for flip in (False, True):
        def place(part):
            return mirror(*part) if flip else part
        elements.append(box(*place(MAST), "part"))
        elements.append(box(*place(FINIAL), "part"))
        (hx0, hy0, hz0), (hx1, hy1, hz1) = place(HEAD)
        inner = "west" if flip else "east"
        origin = [r((hx0 + hx1) / 2), r((hy0 + hy1) / 2), r((hz0 + hz1) / 2)]
        elements.append(box(
            (hx0, hy0, hz0), (hx1, hy1, hz1), {inner: "lens", "*": "part"},
            rotation={"origin": origin, "axis": "x", "angle": 45}, uv={inner: FULL},
        ))
    return elements


def nozzle_elements():
    return [
        box(*NOZZLE_COLLAR, "part"),
        box(*NOZZLE_BODY, "part"),
        box(*NOZZLE_TIP, "tip", faces=SIDES),
    ]


def item_core_elements():
    """A dark resting core for the item model, which has no renderer to draw the collapse."""
    half = 1.6
    y = (BOTTOM_CAP[1] + TOP_CAP[0]) / 2
    return [box((CX - half, y - half, CZ - half), (CX + half, y + half, CZ + half), "core")]


HOUSING_TEXTURES = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "band": TEX + "band",
    "flange": TEX + "flange",
    "pipe": TEX + "pipe",
    "cabinet": TEX + "cabinet",
    "hopper": TEX + "hopper",
    "hopper_top": TEX + "hopper_top",
    "port": TEX + "port",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/vacuum_collapse_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": HOUSING_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "block/vacuum_collapse_generator_active.json", {
        "parent": "rngtech:block/vacuum_collapse_generator",
        "textures": {"lamp": TEX + "lamp_on"},
    })

    parts = {"chamber": chamber_elements(), "stabilizer": stabilizer_elements(), "nozzle": nozzle_elements()}
    for name, elements in parts.items():
        write_json(MODELS / f"block/vacuum_collapse_generator/{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"particle": "#part", "glass": TEX + "glass"},
            "elements": elements,
        })
    for material in CHAMBER_MATERIALS:
        for name in ("chamber", "stabilizer"):
            write_json(MODELS / f"block/vacuum_collapse_generator/{name}_{material}.json", {
                "parent": PART_MODEL + name,
                "textures": {"part": TEX + f"part/{material}", "lens": TEX + f"lens/{material}"},
            })
    for material in NOZZLE_MATERIALS:
        write_json(MODELS / f"block/vacuum_collapse_generator/nozzle_{material}.json", {
            "parent": PART_MODEL + "nozzle",
            "textures": {"part": TEX + f"part/{material}", "tip": TEX + f"tip/{material}"},
        })

    # The item shows a Tungstensteel set around a resting core.
    write_json(MODELS / "item/vacuum_collapse_generator.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            **HOUSING_TEXTURES,
            "glass": TEX + "glass",
            "part": TEX + "part/tungstensteel",
            "lens": TEX + "lens/tungstensteel",
            "tip": TEX + "tip/tungstensteel",
            "core": TEX + "core",
        },
        "elements": item_core_elements() + housing + chamber_elements() + stabilizer_elements() + nozzle_elements(),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"vacuum_collapse_generator:{name}")


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
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
VOID = (8, 6, 14)


def write_material_textures() -> None:
    for material, (body, glow) in MATERIALS.items():
        # Part body: brushed material with a seam every 16 texels (4 px), so caps, rails, and masts read as plating.
        part = noisy(64, body, f"part_{material}", amount=5, streak=3)
        pixels = part.load()
        for y in range(64):
            for x in range(64):
                if y % 16 == 0 or x % 16 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -18), 255)
                elif y % 16 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 14), 255)
        save(part, f"part/{material}")

        # Nozzle tip: the glow colour, brighter at the orifice.
        tip = Image.new("RGBA", (16, 16))
        pixels = tip.load()
        for y in range(16):
            for x in range(16):
                edge = min(x, y, 15 - x, 15 - y)
                pixels[x, y] = (*mix(shade(body, -10), glow, min(1.0, 0.35 + edge / 6)), 255)
        save(tip, f"tip/{material}")

        if material not in CHAMBER_MATERIALS:
            continue
        # Stabilizer lens: a framed emitter in the glow colour, mapped whole onto the head's inner face.
        lens = Image.new("RGBA", (16, 16), (*shade(body, -14), 255))
        draw = ImageDraw.Draw(lens)
        draw.rectangle([1, 1, 14, 14], outline=(*shade(body, 24), 255))
        draw.rectangle([4, 4, 11, 11], fill=(*VOID, 255))
        draw.rectangle([5, 5, 10, 10], fill=(*mix(VOID, glow, 0.55), 255))
        draw.rectangle([6, 6, 9, 9], fill=(*glow, 255))
        lens.putpixel((7, 6), (255, 255, 255, 255))
        save(lens, f"lens/{material}")


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
    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Control cabinet: seamed casing with louvred vents; the cable port is its only fitting.
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

    # Catalyst hopper: dark steel sides and a barred throat on top.
    save(panel_seams(noisy(64, (64, 72, 84), "hopper", amount=4), step=32, rivets=False), "hopper")
    throat = Image.new("RGBA", (16, 16), (*shade(STEEL, -6), 255))
    draw = ImageDraw.Draw(throat)
    draw.rectangle([0, 0, 15, 15], outline=(*shade(STEEL, 16), 255))
    draw.rectangle([2, 2, 13, 13], fill=(*VOID, 255), outline=(*SLATE_DARK, 255))
    for x in (5, 8, 11):
        draw.line([(x, 3), (x, 12)], fill=(*shade(STEEL, -18), 255))
    save(throat, "hopper_top")

    # Glass: transparent (cutout) with sparse vertical glints.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = glass.load()
    for x in range(64):
        if x % 16 in (3, 4):
            for y in range(64):
                if (y // 5) % 4 != 3:
                    pixels[x, y] = GLINT
    save(glass, "glass")

    # Instability dial: calm green to violet over the scale, needle low.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    scale = [(-225, (70, 170, 110)), (-180, (80, 160, 150)), (-135, (90, 140, 190)), (-90, (110, 110, 200)),
             (-45, (140, 90, 210)), (0, (160, 70, 200)), (45, (180, 50, 150))]
    for angle, color in scale:
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    draw.line([(7.5, 7.5), (4.5, 9.5)], fill=(196, 52, 40, 255))
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

    save(noisy(64, (34, 24, 52), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (196, 150, 255), "lamp_on", amount=6), "lamp_on")

    # Item-model resting core: near-black with violet flecks.
    core = noisy(64, (20, 14, 32), "core", amount=4)
    pixels = core.load()
    rand = rng("core_flecks")
    for _ in range(36):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*mix((20, 14, 32), (150, 110, 230), rand.uniform(0.3, 0.7)), 255)
    save(core, "core")

    # Renderer textures: the core (tinted by the chamber's glow and darkened as the collapse runs), the cap rim glow, and the
    # stabilizer lens glow.
    core_fill = noisy(16, (236, 236, 236), "core_fill", amount=12)
    save(core_fill, "fx/core")
    rim = Image.new("RGBA", (16, 16))
    pixels = rim.load()
    for y in range(16):
        for x in range(16):
            strength = 1.0 - abs(y + 0.5 - 8) / 8
            pixels[x, y] = (clamp(255 * strength), clamp(255 * strength), clamp(255 * strength), 255)
    save(rim, "fx/rim")
    lens = Image.new("RGBA", (16, 16))
    pixels = lens.load()
    for y in range(16):
        for x in range(16):
            strength = (1.0 - min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)) ** 1.2
            pixels[x, y] = (clamp(255 * strength), clamp(255 * strength), clamp(255 * strength), 255)
    save(lens, "fx/lens")


def main() -> None:
    write_models()
    write_textures()
    write_material_textures()


if __name__ == "__main__":
    main()
