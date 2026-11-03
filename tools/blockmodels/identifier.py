#!/usr/bin/env python3
"""Generate the Identifier blockstate, block models, Job Chip lamp part, item model, and owned textures.

The Identifier is an Exotic analysis bench on the slate plinth. An analysis cabinet in the Exotic shell stands at the
back, with the feed hatch on its naquadah-rimmed roof and the cable port on its back. In front of it lies the scanning
bed, a dark glass slab etched with a teal grid and a target ring, where the Gear being read rests. An inspection lens
hangs over the bed on an arm from the cabinet; it lights pale teal while the Identifier works. The Identifying Fluid
stands in a framed sight vial on the front-east corner and feeds the lens through a short pipe; identified items
collect in a tray on the front-west corner. A readout on the cabinet front, the status lamp, and the Job Chip lamp
(job_lamp.py) sit above the bed. Run from the repository root:

    python tools/blockmodels/identifier.py
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
TEXTURES = ASSETS / "textures/block/identifier"
TEX = "rngtech:block/identifier/"
CRUSHER = "rngtech:block/crusher/"
FURNACE = "rngtech:block/furnace/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")
GLOW = {"neoforge_data": {"block_light": 15, "sky_light": 15}}

# ---------------------------------------------------------------- geometry
# The front faces north (z = 0); facing the front, east (+x) is on the left.
CABINET = ((2.0, 2.0, 8.0), (14.0, 13.0, 15.0))
BAND_Y = (11.0, 11.5)
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))
# Feed hatch on the cabinet roof, open on top.
HATCH = ((5.0, 13.0, 9.0), (11.0, 14.5, 14.0))
HATCH_WALL = 0.5
# Scanning bed in front of the cabinet.
BED = ((4.0, 2.0, 2.0), (12.0, 5.0, 8.0))
# Inspection lens over the bed, hung from an arm on the cabinet front.
# The lens head is a rounded drum of two crossed slabs, ringed at its foot by a band that lights with the lens.
ARM = ((7.4, 10.25, 6.5), (8.6, 11.25, 8.0))
LENS_X, LENS_Z = 8.0, 4.75
LENS_R, LENS_FLAT = 2.25, 1.4
LENS_Y = (8.75, 10.5)
LENS_RING_Y = (8.75, 9.25)
LENS_FACE = ((LENS_X - 1.75, 8.65, LENS_Z - 1.75), (LENS_X + 1.75, 8.75, LENS_Z + 1.75))
# Sight vial of Identifying Fluid on the front-east corner, and its feed pipe into the lens housing.
VIAL = ((12.0, 1.0), (15.0, 4.0))
VIAL_Y = (2.0, 12.0)
VIAL_CAP = 1.0
VIAL_RAIL = 0.5
FLUID_Y1 = 9.5
# The feed leaves the vial's top cap and drops into the lens housing.
PIPE = ((8.75, 11.0, 3.0), (12.25, 11.75, 3.75))
PIPE_DROP = ((8.75, 10.5, 3.0), (9.5, 11.0, 3.75))
# Output tray on the front-west corner.
TRAY = ((0.75, 2.0, 1.5), (3.5, 3.75, 6.5))
TRAY_WALL = 0.5
# Readout, status lamp, and Job Chip lamp on the cabinet front.
READOUT = ((2.75, 6.0, 7.75), (6.25, 9.25, 8.0))
STATUS_LAMP = ((9.5, 12.0, 7.5), (10.75, 12.75, 8.0))
JOB_LAMP = ((5.25, 12.0, 7.5), (6.5, 12.75, 8.0))
LAMP_FACES = ("north", "east", "west", "up", "down")


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, uv=None, extra=None):
    """One model element. `textures` is a texture key or a {face: key} map with an optional "*" default."""
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end], "faces": {}}
    for face in faces:
        key = textures if isinstance(textures, str) else textures.get(face, textures.get("*"))
        if key is None:
            continue
        entry = {"texture": f"#{key}"}
        if uv and face in uv:
            entry["uv"] = [r(v) for v in uv[face]]
        element["faces"][face] = entry
    if extra:
        element.update(extra)
    return element


def cabinet_elements():
    (x0, y0, z0), (x1, y1, z1) = CABINET
    elements = [
        box(CABINET[0], CABINET[1], "shell", faces=SIDES + ("up",)),
        box((x0 - 0.1, BAND_Y[0], z0 - 0.1), (x1 + 0.1, BAND_Y[1], z1 + 0.1), "band", faces=SIDES),
        box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"),
            uv={"south": [0, 0, 16, 16]}),
        box(*READOUT, "readout", faces=("north",), uv={"north": [0, 0, 16, 16]}),
        box(*STATUS_LAMP, "lamp", faces=LAMP_FACES, extra=GLOW),
        job_lamp.element(*JOB_LAMP, LAMP_FACES),
    ]
    return elements


def hatch_elements(item: bool):
    (hx0, hy0, hz0), (hx1, hy1, hz1) = HATCH
    w = HATCH_WALL
    walls = {"up": "trim", "*": "rim"}
    elements = [
        box((hx0, hy0, hz0), (hx1, hy1, hz0 + w), {"south": "liner", **walls}),
        box((hx0, hy0, hz1 - w), (hx1, hy1, hz1), {"north": "liner", **walls}),
        box((hx0, hy0, hz0 + w), (hx0 + w, hy1, hz1 - w), {"east": "liner", **walls}, faces=("east", "west", "up")),
        box((hx1 - w, hy0, hz0 + w), (hx1, hy1, hz1 - w), {"west": "liner", **walls}, faces=("east", "west", "up")),
    ]
    if not item:
        elements.append(box((hx0 + w, hy0, hz0 + w), (hx1 - w, hy0 + 0.05, hz1 - w), "throat", faces=("up",),
                            uv={"up": [0, 0, 16, 16]}))
    return elements


def bed_elements():
    (bx0, by0, bz0), (bx1, by1, bz1) = BED
    return [
        box(BED[0], BED[1], {"up": "bed", "*": "shell"}, faces=("north", "east", "west", "up"),
            uv={"up": [0, 0, 16, 16]}),
        # A trim lip along the bed's front edge.
        box((bx0 - 0.25, by1 - 0.75, bz0 - 0.25), (bx1 + 0.25, by1 - 0.25, bz0 + 0.25), "trim",
            faces=("north", "east", "west", "up", "down")),
    ]


def drum(y0, y1, textures, grow=0.0, faces=FACES, extra=None):
    """The lens head's rounded drum: two crossed slabs, axis-aligned so it also renders as a wiki icon."""
    radius, flat = LENS_R + grow, LENS_FLAT + grow
    return [
        box((LENS_X - radius, y0, LENS_Z - flat), (LENS_X + radius, y1, LENS_Z + flat), textures, faces, extra=extra),
        box((LENS_X - flat, y0, LENS_Z - radius), (LENS_X + flat, y1, LENS_Z + radius), textures, faces, extra=extra),
    ]


def lens_elements(active: bool):
    lens = "lens_on" if active else "lens_off"
    ring = "ring_on" if active else "ring_off"
    glow = GLOW if active else None
    elements = [box(*ARM, "trim", faces=("east", "west", "up", "down"))]
    elements += drum(LENS_RING_Y[1], LENS_Y[1], {"up": "emitter_top", "down": "emitter_under", "*": "emitter"})
    elements += drum(*LENS_RING_Y, ring, grow=0.1, extra=glow)
    elements.append(box(*LENS_FACE, lens, faces=("down",), uv={"down": [0, 0, 16, 16]}, extra=glow))
    return elements


def vial_elements(item: bool):
    (x0, z0), (x1, z1) = VIAL
    y0, y1 = VIAL_Y
    cap, rail = VIAL_CAP, VIAL_RAIL
    elements = [
        box((x0, y0, z0), (x1, y0 + cap, z1), {"up": "cap_top", "*": "cap"}, faces=SIDES + ("up",),
            uv={"up": [0, 0, 16, 16]}),
        box((x0, y1 - cap, z0), (x1, y1, z1), {"up": "cap_top", "down": "cap_top", "*": "cap"},
            uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
    ]
    for x in (x0, x1 - rail):
        for z in (z0, z1 - rail):
            elements.append(box((x, y0 + cap, z), (x + rail, y1 - cap, z + rail), "rail", faces=SIDES))
    # The fluid: a glowing column, standing clear of the glass. The placed block leaves it to IdentifierRenderer,
    # which draws the tank's real level.
    if item:
        inset = rail
        elements.append(box((x0 + inset, y0 + cap, z0 + inset), (x1 - inset, FLUID_Y1, z1 - inset),
                            {"up": "fluid_top", "*": "fluid"}, faces=SIDES + ("up",), extra=GLOW))
    else:
        glass = (x0 + 0.05, y0 + cap, z0 + 0.05), (x1 - 0.05, y1 - cap, z1 - 0.05)
        elements.append(box(*glass, "glass", faces=SIDES, uv={face: [0, 0, 16, 16] for face in SIDES}))
    elements.append(box(*PIPE, "pipe", faces=("north", "south", "up", "west")))
    elements.append(box(*PIPE_DROP, "pipe", faces=("north", "south", "east", "west")))
    return elements


def tray_elements(item: bool):
    (tx0, ty0, tz0), (tx1, ty1, tz1) = TRAY
    w = TRAY_WALL
    elements = [
        box((tx0, ty0, tz0), (tx1, ty1, tz0 + w), {"south": "liner", "*": "trim"}),
        box((tx0, ty0, tz1 - w), (tx1, ty1, tz1), {"north": "liner", "*": "trim"}),
        box((tx0, ty0, tz0 + w), (tx0 + w, ty1, tz1 - w), {"east": "liner", "*": "trim"}, faces=("east", "west", "up")),
        box((tx1 - w, ty0, tz0 + w), (tx1, ty1, tz1 - w), {"west": "liner", "*": "trim"}, faces=("east", "west", "up")),
    ]
    if not item:
        elements.append(box((tx0 + w, ty0, tz0 + w), (tx1 - w, ty0 + 0.5, tz1 - w), "liner", faces=("up",)))
    return elements


def elements(active: bool, item: bool = False):
    out = [box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})]
    out += cabinet_elements()
    out += hatch_elements(item)
    out += bed_elements()
    out += lens_elements(active)
    out += vial_elements(item)
    out += tray_elements(item)
    return out


TEXTURE_MAP = {
    "particle": CRUSHER + "shell/exotic",
    "shell": CRUSHER + "shell/exotic",
    "trim": CRUSHER + "trim/exotic",
    "rim": CRUSHER + "rim/naquadah",
    "casing": CRUSHER + "casing",
    "band": CRUSHER + "band",
    "port": CRUSHER + "port",
    "liner": CRUSHER + "liner",
    "throat": CRUSHER + "throat",
    "plinth_top": CRUSHER + "plinth_top",
    "plinth_side": CRUSHER + "plinth_side",
    "lamp": CRUSHER + "lamp_off",
    "glass": FURNACE + "glass",
    "pipe": FURNACE + "pipe",
    "bed": TEX + "bed",
    "emitter": TEX + "emitter",
    "emitter_under": TEX + "emitter_under",
    "emitter_top": TEX + "emitter_top",
    "lens_off": TEX + "lens_off",
    "lens_on": TEX + "lens_on",
    "ring_off": TEX + "ring_off",
    "ring_on": TEX + "ring_on",
    "fluid": TEX + "fluid",
    "fluid_top": TEX + "fluid_top",
    "cap": TEX + "cap",
    "cap_top": TEX + "cap_top",
    "rail": TEX + "rail",
    "readout": TEX + "readout",
    **job_lamp.TEXTURE,
}


def textures(active: bool):
    shown = dict(TEXTURE_MAP)
    for key in (("lens_off", "ring_off") if active else ("lens_on", "ring_on")):
        shown.pop(key)
    if active:
        shown["lamp"] = CRUSHER + "lamp_on"
    return shown


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    for active, name in ((False, "identifier"), (True, "identifier_active")):
        write_json(MODELS / f"block/{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": textures(active),
            "elements": elements(active),
        })
    job_lamp.write_part(MODELS / "block/identifier/job_lamp.json", *JOB_LAMP, LAMP_FACES)
    job_lamp.write_blockstate("identifier", "rngtech:block/identifier", "rngtech:block/identifier_active",
                              "rngtech:block/identifier/job_lamp")
    # The item shows the lit lens and leaves out the glass and the hatch and tray floors, which a wiki icon
    # without a depth buffer would draw over the parts in front of them.
    item_textures = textures(True)
    item_textures["lamp"] = CRUSHER + "lamp_off"
    write_json(MODELS / "item/identifier.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": item_textures,
        "elements": elements(True, item=True),
    })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"identifier:{name}")


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


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


# The Exotic chassis palette (crusher.py) and the Identifying Fluid's tint.
EXOTIC_SHELL = (50, 42, 62)
EXOTIC_TRIM = (76, 66, 92)
EXOTIC_GLOW = (170, 96, 222)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
FLUID = (0xB8, 0xF2, 0xE6)
GLASS_DARK = (20, 18, 26)


def bed_texture():
    """The scanning bed: dark glass in a trim frame, a faint teal grid, and a target ring at its centre."""
    image = noisy(64, GLASS_DARK, "bed", amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if min(x, y, 63 - x, 63 - y) < 3:
                pixels[x, y] = (*shade(EXOTIC_TRIM, -6 if x > y else 6), 255)
            elif (x - 3) % 8 == 0 or (y - 3) % 8 == 0:
                pixels[x, y] = (*mix(pixels[x, y], TEAL_DARK, 0.6), 255)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 32)
            if 17.0 < d < 18.5:
                pixels[x, y] = (*mix(pixels[x, y], TEAL, 0.75), 255)
            elif 6.0 < d < 7.0:
                pixels[x, y] = (*mix(pixels[x, y], TEAL, 0.5), 255)
    for cx, cy in ((10, 10), (53, 10), (10, 53), (53, 53)):
        for i in range(-2, 3):
            pixels[cx + i, cy] = (*mix(pixels[cx + i, cy], TEAL, 0.6), 255)
            pixels[cx, cy + i] = (*mix(pixels[cx, cy + i], TEAL, 0.6), 255)
    return image


def emitter_texture():
    """The lens housing's flanks: Exotic trim with vent slots and a purple seam."""
    image = noisy(64, EXOTIC_TRIM, "emitter", amount=5, streak=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if y in (0, 63):
                pixels[x, y] = (*shade(EXOTIC_TRIM, -26), 255)
            elif x % 8 in (3, 4) and 18 < y < 46:
                pixels[x, y] = (*shade(EXOTIC_SHELL, -18), 255)
            elif y == 8:
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.45), 255)
    return image


def emitter_under_texture():
    image = noisy(64, shade(EXOTIC_TRIM, -12), "emitter_under", amount=4)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(EXOTIC_TRIM, -34), 255), width=2)
    for x, y in ((5, 5), (57, 5), (5, 57), (57, 57)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(*shade(EXOTIC_TRIM, 34), 255))
    return image


def emitter_top_texture():
    """The lens head's cap: Exotic trim with a bolt circle and a purple seam ring."""
    image = noisy(64, EXOTIC_TRIM, "emitter_top", amount=4)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 18)
            if 9.0 < d < 10.5:
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.5), 255)
            elif d < 4.0:
                pixels[x, y] = (*shade(EXOTIC_SHELL, -10), 255)
    return image


def ring_texture(lit: bool):
    """The band round the lens head's foot, mapped by position: an even glow, brightest along its middle."""
    color = FLUID if lit else (30, 62, 70)
    image = noisy(64, color, "ring_on" if lit else "ring_off", amount=3 if lit else 4)
    pixels = image.load()
    for y in range(64):
        lift = 10 if y % 2 == 0 else -4
        for x in range(64):
            pixels[x, y] = (*shade(pixels[x, y], lift if lit else lift / 2), 255)
    return image


def lens_texture(lit: bool):
    """The inspection lens seen from below: a bezel ring round a glass disc, pale teal and bright when lit."""
    image = Image.new("RGBA", (64, 64))
    pixels = image.load()
    rand = rng("lens_on" if lit else "lens_off")
    core = FLUID if lit else (28, 52, 60)
    edge = mix(FLUID, TEAL, 0.5) if lit else (18, 32, 40)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 32) / 32
            if d > 1.0:
                continue
            if d > 0.86:
                value = shade(EXOTIC_TRIM, -20 + rand.uniform(-4, 4))
            elif d > 0.78:
                value = shade(EXOTIC_TRIM, 18)
            else:
                value = mix(core, edge, d / 0.78)
                value = shade(value, rand.uniform(-3, 3) + (24 if lit and d < 0.25 else 0))
                # A highlight arc toward the top left.
                if 0.5 < d < 0.6 and x < 30 and y < 30:
                    value = shade(value, 40 if not lit else 20)
            pixels[x, y] = (*value, 255)
    return image


def fluid_texture(top: bool):
    """The Identifying Fluid: pale teal with soft ripple bands and a few rising bubbles."""
    name = "fluid_top" if top else "fluid"
    image = noisy(64, FLUID, name, amount=5)
    pixels = image.load()
    rand = rng(name + "_bubbles")
    for y in range(64):
        for x in range(64):
            wave = math.sin((y if not top else x + y) * 0.4 + math.sin(x * 0.3) * 1.2)
            pixels[x, y] = (*shade(pixels[x, y], wave * 8 - (0 if top else 4)), 255)
    for _ in range(14):
        x, y = rand.randrange(2, 61), rand.randrange(2, 61)
        size = rand.choice((1, 2))
        for dx in range(size):
            for dy in range(size):
                pixels[x + dx, y + dy] = (*shade(FLUID, 30), 255)
        pixels[x + size, y + size] = (*shade(FLUID, -26), 255)
    if not top:
        # The meniscus line along the top edge of the column.
        for x in range(64):
            pixels[x, 0] = (*shade(FLUID, 34), 255)
            pixels[x, 1] = (*shade(FLUID, 18), 255)
    return image


def cap_texture():
    image = noisy(64, shade(EXOTIC_TRIM, -10), "cap", amount=4)
    pixels = image.load()
    for x in range(64):
        pixels[x, 0] = (*shade(EXOTIC_TRIM, 26), 255)
        pixels[x, 63] = (*shade(EXOTIC_SHELL, -20), 255)
        for y in (30, 31):
            pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.4), 255)
    return image


def cap_top_texture():
    image = noisy(64, EXOTIC_TRIM, "cap_top", amount=4)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(EXOTIC_TRIM, -24), 255), width=4)
    draw.ellipse([20, 20, 43, 43], fill=(*shade(EXOTIC_SHELL, -8), 255), outline=(*shade(EXOTIC_TRIM, 40), 255),
                 width=2)
    draw.ellipse([28, 28, 35, 35], fill=(*mix(FLUID, TEAL, 0.3), 255))
    return image


def rail_texture():
    """Vial rail: Exotic trim with gauge ticks every eighth."""
    image = noisy(64, shade(EXOTIC_TRIM, -4), "rail", amount=4)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if y % 8 == 0:
                pixels[x, y] = (*mix(EXOTIC_TRIM, (220, 220, 236), 0.5), 255)
            elif x % 16 in (0, 15):
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
    return image


def readout_texture():
    """A dark readout showing affix lines being resolved in teal, one line still a purple unknown."""
    image = noisy(64, (12, 14, 20), "readout", amount=2)
    pixels = image.load()
    rand = rng("readout_lines")
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(EXOTIC_TRIM, -10), 255), width=3)
    for row in range(5):
        y = 9 + row * 10
        color = EXOTIC_GLOW if row == 3 else TEAL
        x = 8
        end = 8 + rand.randint(26, 46)
        while x < end:
            width = rand.randint(2, 6)
            for dx in range(width):
                for dy in range(3):
                    if x + dx < 56:
                        pixels[x + dx, y + dy] = (*shade(color, rand.uniform(-26, 6)), 255)
            x += width + 2
    for y in range(3, 61, 2):
        for x in range(3, 61):
            pixels[x, y] = (*shade(pixels[x, y], -4), 255)
    return image


def write_textures() -> None:
    save(bed_texture(), "bed")
    save(emitter_texture(), "emitter")
    save(emitter_under_texture(), "emitter_under")
    save(emitter_top_texture(), "emitter_top")
    save(ring_texture(False), "ring_off")
    save(ring_texture(True), "ring_on")
    save(lens_texture(False), "lens_off")
    save(lens_texture(True), "lens_on")
    save(fluid_texture(False), "fluid")
    save(fluid_texture(True), "fluid_top")
    save(cap_texture(), "cap")
    save(cap_top_texture(), "cap_top")
    save(rail_texture(), "rail")
    save(readout_texture(), "readout")


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
