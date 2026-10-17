#!/usr/bin/env python3
"""Generate the Fluid and Compressor Tank block models, part models, item models, and owned textures.

The design follows the recipe: glass panes in a frame of the tank's metal, built on a tank frame. Every tank is a
glass tank on a slate plinth, with corner posts, rails, and a hoop in its tier metal, so the loose fluid shows through
the glass. The plain Fluid Tanks (Iron, Copper, Bronze) have a bolted manway lid and a drain spigot on the front.

The Compressor Tanks (Steel and later) are built around their compressed store: a round pressure vessel with a domed
head, banded with more reinforcing hoops the higher its compression ratio. An armoured sight window on its front shows
the compressed fill and a pressure gauge above it reads it. The loose fluid shows in a glass feed tube in each corner,
piped into the vessel's skirt. Each Servo is a pump on top of a feed tube, joined by a header ring around the vessel
head, and the Battery Cell lies in a cradle behind the vessel.

Tanks meet pipes from any side, so each side gets a connector port only where a cable or Universal Connector faces it;
the lid and the plinth have a hatch recessed by the port's depth so the same plate sits flush on top and underneath.

The housing is a static block model. The fluid, the compressed fill, the gauge needle, the installed Servo pumps and
their strokes, the Battery Cell, and the ports are drawn by CompressorTankRenderer, whose constants match the geometry here. Run from the
repository root:

    python tools/blockmodels/compressor_tank.py
"""

from __future__ import annotations

import json
import math
import random
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures/block/compressor_tank"
TEX = "rngtech:block/compressor_tank/"
PARTS = "rngtech:block/compressor_tank/"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

PLINTH_TOP = 2.0
# Centre hatch in the plinth and lid, sized to the Universal Connector ring, recessed by the port plate's depth.
HATCH = (4.0, 12.0)
PLATE_DEPTH = 1.0
HATCH_TOP = 16.0 - PLATE_DEPTH
# Corner posts and the glass panes between them.
POST = (1.0, 2.5)
GLASS_OUT, GLASS_IN = 1.5, 1.75
RAIL_OUT = 1.25
RAIL = 0.75
# The Fluid Tank's lid runs to the top of the block.
FLUID_TANK_GLASS_TOP = 14.0
LID = (0.5, 15.5)
BAND_OUT = 0.25
# Compressor Tank: a pressure vessel in the middle, three crossed slabs so it reads round, on a skirt, under a
# shoulder and a domed head that ends in a manway flange at the hatch height.
VESSEL_SLABS = (((3.0, 5.5), (13.0, 10.5)), ((5.5, 3.0), (10.5, 13.0)), ((4.25, 4.25), (11.75, 11.75)))
SKIRT_Y = (PLINTH_TOP, 2.75)
VESSEL_Y = (2.75, 11.5)
SHOULDER_Y = (11.5, 13.0)
HEAD_Y = (13.0, 14.25)
FLANGE = (5.5, 10.5)
HOOP_OUT = 0.25
HOOP_HEIGHT = 0.5
# Reinforcing hoops by tier: more hoops for a higher compression ratio (10x at Steel to 48x at Exotic).
HOOPS = {"steel": 1, "aluminum": 2, "titanium": 2, "tungstensteel": 3, "exotic": 4}
# Loose fluid shows in a glass feed tube in each corner, between a foot cap and a head cap.
TUBE = (0.75, 3.25)
TUBE_GLASS_Y = (2.75, 12.75)
TUBE_TOP = 13.5
TUBE_RAIL = 0.5
# Armoured sight window on the vessel's front for the compressed fill, and the pressure gauge above it.
WINDOW = ((6.75, 3.5), (9.25, 10.5))
WINDOW_Z = 2.6
VESSEL_FRONT = 3.0
RAIL_Z = 2.2
GAUGE = ((6.5, 11.0, 2.5), (9.5, 14.0, 4.0))
LAMP = ((10.0, 11.25, 3.75), (11.0, 12.25, 4.75))
# A pump on each corner, on top of its feed tube; the header ring joins them around the vessel head.
PUMP_CENTRES = ((2.0, 2.0), (14.0, 2.0), (2.0, 14.0), (14.0, 14.0))
RING = (3.0, 13.0)
RING_Y = (13.0, 13.75)
# Battery Cell cradle behind the vessel.
CRADLE_Z = (13.25, 15.5)

FLUID_TANKS = ("iron", "copper", "bronze")
COMPRESSOR_TANKS = ("steel", "aluminum", "titanium", "tungstensteel", "exotic")
# Servos baked into each Compressor Tank's item model: the best Servo its stage takes.
ITEM_SERVOS = {"steel": "steel", "aluminum": "steel", "titanium": "titanium", "tungstensteel": "tungstensteel", "exotic": "exotic"}

# Tank metals: base colour per tier. Exotic is the teal-green the shaped generators use.
METALS = {
    "iron": (126, 128, 128),
    "copper": (184, 104, 60),
    "bronze": (150, 104, 50),
    "steel": (104, 114, 124),
    "aluminum": (186, 198, 214),
    "titanium": (142, 160, 183),
    "tungstensteel": (74, 82, 93),
    "exotic": (84, 108, 104),
}
# Servo materials as body and accent colours, plus Unique Servos. The accent marks each pump's band and crosshead,
# so Servos of similar grey metals still read apart from above.
SERVOS = {
    "steel": ((116, 123, 125), (214, 220, 224)),
    "titanium": ((126, 150, 186), (170, 208, 250)),
    "tungstensteel": ((60, 68, 80), (124, 174, 255)),
    "nullite": ((52, 37, 76), (160, 92, 255)),
    "exotic": ((60, 96, 90), (70, 255, 182)),
    "unique": ((175, 96, 37), (255, 190, 110)),
}


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


def framed(lo, hi, y0, y1, textures, hatch_y0, hatch_y1, hatch_textures, hatch_faces, inner="casing"):
    """A slab from lo to hi on both axes around the centre hatch, and the hatch filling part of its height."""
    a, b = HATCH
    return [
        box((lo, y0, lo), (hi, y1, a), {**textures, "south": inner}),
        box((lo, y0, b), (hi, y1, hi), {**textures, "north": inner}),
        box((lo, y0, a), (a, y1, b), {**textures, "east": inner}, faces=("west", "east", "up", "down")),
        box((b, y0, a), (hi, y1, b), {**textures, "west": inner}, faces=("east", "west", "up", "down")),
        box((a, hatch_y0, a), (b, hatch_y1, b), hatch_textures, faces=hatch_faces),
    ]


def side_box(side, a0, a1, y0, y1, d0, d1, textures, faces):
    """A box along one side: a0..a1 along the side, d0..d1 in from that side's face."""
    if side == "north":
        return box((a0, y0, d0), (a1, y1, d1), textures, faces)
    if side == "south":
        return box((a0, y0, 16.0 - d1), (a1, y1, 16.0 - d0), textures, faces)
    if side == "west":
        return box((d0, y0, a0), (d1, y1, a1), textures, faces)
    return box((16.0 - d1, y0, a0), (16.0 - d0, y1, a1), textures, faces)


def outward(side):
    return side


def inward(side):
    return {"north": "south", "south": "north", "east": "west", "west": "east"}[side]


def tank_walls(glass_top):
    """Plinth, corner posts, glass panes, rails, and the hoop: everything below the lid."""
    elements = framed(0.0, 16.0, 0.0, PLINTH_TOP, {"up": "plinth_top", "down": "base_bottom", "*": "plinth_side"},
                      PLATE_DEPTH, PLINTH_TOP, {"up": "plinth_top", "down": "base_bottom"}, ("up", "down"))
    p0, p1 = POST
    for x0 in (p0, 16.0 - p1):
        for z0 in (p0, 16.0 - p1):
            elements.append(box((x0, PLINTH_TOP, z0), (x0 + p1 - p0, glass_top, z0 + p1 - p0), "trim", faces=SIDES))
    hoop = (PLINTH_TOP + glass_top) / 2.0
    rails = ((PLINTH_TOP, PLINTH_TOP + RAIL), (glass_top - RAIL, glass_top), (hoop - RAIL / 2.0, hoop + RAIL / 2.0))
    for side in SIDES:
        a0, a1 = POST[1], 16.0 - POST[1]
        elements.append(side_box(side, a0, a1, PLINTH_TOP, glass_top, GLASS_OUT, GLASS_IN, "glass", (outward(side), inward(side))))
        for y0, y1 in rails:
            elements.append(side_box(side, a0, a1, y0, y1, RAIL_OUT, GLASS_IN, "trim", (outward(side), inward(side), "up", "down")))
    return elements


def fluid_tank_elements():
    elements = tank_walls(FLUID_TANK_GLASS_TOP)
    lo, hi = LID
    lid = framed(lo, hi, FLUID_TANK_GLASS_TOP, 16.0, {"up": "cap", "down": "shell", "*": "shell"},
                 FLUID_TANK_GLASS_TOP, HATCH_TOP, {"up": "cap", "down": "shell"}, ("up", "down"), inner="shell")
    elements += lid
    y = FLUID_TANK_GLASS_TOP + 0.5
    elements.append(box((BAND_OUT, y, BAND_OUT), (16.0 - BAND_OUT, y + 0.5, 16.0 - BAND_OUT), "band", faces=SIDES))
    # Drain spigot on the front rail, with its valve handle.
    elements.append(box((7.0, PLINTH_TOP, 0.25), (9.0, PLINTH_TOP + 1.25, RAIL_OUT), "trim", faces=("north", "east", "west", "up")))
    elements.append(box((6.25, PLINTH_TOP + 1.25, 0.5), (9.75, PLINTH_TOP + 1.5, 1.0), "trim"))
    return elements


def slabs(inset, y0, y1, textures, faces=SIDES, grow=0.0):
    """The vessel's three slabs at one height, shrunk by inset or grown outward by grow."""
    d = grow - inset
    return [box((x0 - d, y0, z0 - d), (x1 + d, y1, z1 + d), textures, faces) for (x0, z0), (x1, z1) in VESSEL_SLABS]


def compressor_tank_elements(hoops):
    elements = framed(0.0, 16.0, 0.0, PLINTH_TOP, {"up": "plinth_top", "down": "base_bottom", "*": "plinth_side"},
                      PLATE_DEPTH, PLINTH_TOP, {"up": "plinth_top", "down": "base_bottom"}, ("up", "down"))

    # Pressure vessel: skirt, shell with its reinforcing hoops, shoulder, domed head, and the manway flange.
    elements += slabs(0.0, *SKIRT_Y, "trim", SIDES + ("up",), grow=HOOP_OUT)
    elements += slabs(0.0, *VESSEL_Y, "shell")
    pitch = (VESSEL_Y[1] - VESSEL_Y[0]) / (hoops + 1)
    for i in range(1, hoops + 1):
        y = VESSEL_Y[0] + pitch * i
        elements += slabs(0.0, y - HOOP_HEIGHT / 2, y + HOOP_HEIGHT / 2, "trim", SIDES + ("up", "down"), grow=HOOP_OUT)
    elements += slabs(0.0, VESSEL_Y[1] - 0.25, VESSEL_Y[1], "trim", SIDES + ("up",), grow=HOOP_OUT)
    elements += slabs(1.0, *SHOULDER_Y, "shell", SIDES + ("up",))
    elements += slabs(2.0, *HEAD_Y, "shell", SIDES + ("up",))
    f0, f1 = FLANGE
    elements.append(box((f0, HEAD_Y[1], f0), (f1, HATCH_TOP, f1), {"up": "manifold", "*": "trim"}, SIDES + ("up",)))

    # Sight window for the compressed fill: dark gauge glass in a frame of heavy rails.
    (wx0, wy0), (wx1, wy1) = WINDOW
    elements.append(box((wx0, wy0, WINDOW_Z), (wx1, wy1, VESSEL_FRONT), {"north": "window"}, faces=("north",)))
    elements.append(box((wx0 - 0.5, wy0, RAIL_Z), (wx0, wy1, VESSEL_FRONT), "trim", faces=("north", "east", "west")))
    elements.append(box((wx1, wy0, RAIL_Z), (wx1 + 0.5, wy1, VESSEL_FRONT), "trim", faces=("north", "east", "west")))
    elements.append(box((wx0 - 0.5, wy0 - 0.5, RAIL_Z), (wx1 + 0.5, wy0, VESSEL_FRONT), "trim",
                        faces=("north", "east", "west", "up", "down")))
    elements.append(box((wx0 - 0.5, wy1, RAIL_Z), (wx1 + 0.5, wy1 + 0.5, VESSEL_FRONT), "trim",
                        faces=("north", "east", "west", "up", "down")))
    # Pressure gauge on the shoulder, and the status lamp beside it.
    elements.append(box(*GAUGE, {"north": "gauge", "*": "trim"}, faces=("north", "east", "west", "up", "down"),
                        uv={"north": [0, 0, 16, 16]}))
    elements.append(box(*LAMP, "lamp", extra={"neoforge_data": {"block_light": 15, "sky_light": 15}}))

    # Wired-glass feed tubes in the corners: a foot and head cap, a rail up each edge, collars around the glass, and a
    # feed pipe into the skirt.
    t0, t1 = TUBE
    g0, g1 = TUBE_GLASS_Y
    for x0 in (t0, 16.0 - t1):
        for z0 in (t0, 16.0 - t1):
            x1, z1 = x0 + t1 - t0, z0 + t1 - t0
            elements.append(box((x0 + 0.25, g0, z0 + 0.25), (x1 - 0.25, g1, z1 - 0.25), "tube_glass", faces=SIDES))
            elements.append(box((x0, PLINTH_TOP, z0), (x1, g0, z1), "trim", faces=SIDES + ("up",)))
            elements.append(box((x0, g1, z0), (x1, TUBE_TOP, z1), {"up": "socket", "*": "trim"}, faces=SIDES + ("up", "down")))
            for rx in (x0, x1 - TUBE_RAIL):
                for rz in (z0, z1 - TUBE_RAIL):
                    elements.append(box((rx, g0, rz), (rx + TUBE_RAIL, g1, rz + TUBE_RAIL), "trim", faces=SIDES))
            for i in (1, 2):
                y = g0 + (g1 - g0) * i / 3
                elements.append(box((x0, y - 0.25, z0), (x1, y + 0.25, z1), "trim", faces=SIDES + ("up", "down")))
            px0, px1 = (x1, x1 + 1.0) if x0 < 8 else (x0 - 1.0, x0)
            pz0 = z1 - 1.0 if z0 < 8 else z0 + 0.25
            elements.append(box((px0, 3.0, pz0), (px1, 3.75, pz0 + 0.75), "pipe", faces=("north", "south", "up", "down")))

    # Header ring around the vessel head, joining the four pumps.
    r0, r1 = RING
    y0, y1 = RING_Y
    elements.append(box((r0, y0, r0), (r1, y1, r0 + 0.75), "pipe", faces=("north", "south", "up", "down")))
    elements.append(box((r0, y0, r1 - 0.75), (r1, y1, r1), "pipe", faces=("north", "south", "up", "down")))
    elements.append(box((r0, y0, r0 + 0.75), (r0 + 0.75, y1, r1 - 0.75), "pipe", faces=("east", "west", "up", "down")))
    elements.append(box((r1 - 0.75, y0, r0 + 0.75), (r1, y1, r1 - 0.75), "pipe", faces=("east", "west", "up", "down")))

    # Battery Cell cradle on the plinth behind the vessel.
    cz0, cz1 = CRADLE_Z
    for x0 in (6.25, 9.0):
        elements.append(box((x0, PLINTH_TOP, cz0), (x0 + 0.75, PLINTH_TOP + 0.75, cz1), "socket"))
    return elements


PUMP_HEAD = 1.5


def pump_elements():
    """One Servo pump, centred on (8, 8) and standing on y 0: a ribbed barrel in the Servo's metal with a band in its
    accent colour, under a cylinder head."""
    return [
        box((6.75, 0.0, 7.25), (9.25, PUMP_HEAD - 0.25, 8.75), "servo", faces=SIDES),
        box((7.25, 0.0, 6.75), (8.75, PUMP_HEAD - 0.25, 9.25), "servo", faces=SIDES),
        box((6.6, 0.45, 7.1), (9.4, 0.8, 8.9), "accent", faces=SIDES + ("up", "down")),
        box((7.1, 0.45, 6.6), (8.9, 0.8, 9.4), "accent", faces=SIDES + ("up", "down")),
        box((7.0, PUMP_HEAD - 0.25, 7.0), (9.0, PUMP_HEAD, 9.0), "servo", faces=SIDES + ("up",)),
    ]


def crosshead_elements(lift):
    """The piston rod and crosshead at one point of the stroke, for the item model; the renderer moves them."""
    y0 = PUMP_HEAD + 0.1 + lift
    return [
        box((7.8, PUMP_HEAD, 7.8), (8.2, y0, 8.2), "pipe", faces=SIDES),
        box((7.1, y0, 7.1), (8.9, y0 + 0.35, 8.9), "accent", faces=SIDES + ("up", "down")),
    ]


def port_elements(backed):
    """A connector plate on the north face; the renderer turns it to each side. A side plate has a nozzle behind it
    reaching the glass or the vessel; a top or bottom plate sits in its hatch."""
    elements = [box((4.0, 4.0, 0.0), (12.0, 12.0, PLATE_DEPTH), {"north": "port", "*": "trim"}, uv={"north": [0, 0, 16, 16]})]
    if backed:
        elements.append(box((5.5, 5.5, PLATE_DEPTH), (10.5, 10.5, VESSEL_FRONT), "trim", faces=("east", "west", "up", "down")))
    return elements


SHARED_TEXTURES = {
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "base_bottom": TEX + "base_bottom",
    "glass": TEX + "glass",
    "band": TEX + "band",
}
COMPRESSOR_TEXTURES = {
    **SHARED_TEXTURES,
    "window": TEX + "window",
    "tube_glass": TEX + "tube_glass",
    "gauge": TEX + "gauge",
    "socket": TEX + "socket",
    "pipe": TEX + "pipe",
    "manifold": TEX + "manifold",
    "lamp": TEX + "lamp_off",
}


def tier_textures(metal):
    return {
        "particle": TEX + f"shell/{metal}",
        "shell": TEX + f"shell/{metal}",
        "trim": TEX + f"trim/{metal}",
        "cap": TEX + f"cap/{metal}",
    }


def translated(elements, dx, dy, dz):
    moved = []
    for element in elements:
        copy = json.loads(json.dumps(element))
        copy["from"] = [r(copy["from"][0] + dx), r(copy["from"][1] + dy), r(copy["from"][2] + dz)]
        copy["to"] = [r(copy["to"][0] + dx), r(copy["to"][1] + dy), r(copy["to"][2] + dz)]
        moved.append(copy)
    return moved


def item_pump_elements():
    elements = []
    for slot, (cx, cz) in enumerate(PUMP_CENTRES):
        parts = pump_elements() + crosshead_elements(0.25 * (slot % 2))
        elements += translated(parts, cx - 8.0, TUBE_TOP, cz - 8.0)
    return elements


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    parts = MODELS / "block/compressor_tank"
    write_json(parts / "fluid_tank.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": SHARED_TEXTURES,
        "elements": fluid_tank_elements(),
    })
    for hoops in sorted(set(HOOPS.values())):
        housing = compressor_tank_elements(hoops)
        write_json(parts / f"compressor_tank_{hoops}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": COMPRESSOR_TEXTURES,
            "elements": housing,
        })
        write_json(MODELS / f"item/compressor_tank_template_{hoops}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": COMPRESSOR_TEXTURES,
            "elements": item_pump_elements() + housing,
        })
    for metal in FLUID_TANKS + COMPRESSOR_TANKS:
        name = f"{metal}_compressor_tank"
        compressor = metal in COMPRESSOR_TANKS
        housing_model = PARTS + (f"compressor_tank_{HOOPS[metal]}" if compressor else "fluid_tank")
        write_json(MODELS / f"block/{name}.json", {"parent": housing_model, "textures": tier_textures(metal)})
        active = {"parent": f"rngtech:block/{name}"}
        if compressor:
            active["textures"] = {"lamp": TEX + "lamp_on"}
        write_json(MODELS / f"block/{name}_active.json", active)
        if compressor:
            write_json(MODELS / f"item/{name}.json", {
                "parent": f"rngtech:item/compressor_tank_template_{HOOPS[metal]}",
                "textures": {
                    **tier_textures(metal),
                    "servo": TEX + f"servo/{ITEM_SERVOS[metal]}",
                    "accent": TEX + f"servo/{ITEM_SERVOS[metal]}_accent",
                },
            })
        else:
            write_json(MODELS / f"item/{name}.json", {"parent": f"rngtech:block/{name}"})

    write_json(parts / "servo.json", {"textures": {"particle": "#servo"}, "elements": pump_elements()})
    for material in SERVOS:
        write_json(parts / f"servo_{material}.json", {
            "parent": PARTS + "servo",
            "textures": {"servo": TEX + f"servo/{material}", "accent": TEX + f"servo/{material}_accent"},
        })
    write_json(parts / "port.json", {"textures": {"particle": "#trim", "port": TEX + "port"}, "elements": port_elements(True)})
    write_json(parts / "port_flat.json", {"textures": {"particle": "#trim", "port": TEX + "port"}, "elements": port_elements(False)})
    for metal in METALS:
        for part in ("port", "port_flat"):
            write_json(parts / f"{part}_{metal}.json", {"parent": PARTS + part, "textures": {"trim": TEX + f"trim/{metal}"}})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"compressor_tank:{name}")


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
    """Soft patches, such as rust, tarnish, or heat tint, blended into the base."""
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
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
EXOTIC_GLOW = (70, 255, 182)


def shell_texture(metal, color):
    """The tank metal: the same riveted plate in each tier's own finish."""
    if metal == "iron":
        image = panel_seams(noisy(64, color, "iron_shell", amount=8), step=32, depth=26)
        blotches(image, "iron_rust", (122, 76, 44), 5, 10, 0.55)
    elif metal == "copper":
        image = noisy(64, color, "copper_shell", amount=7)
        pixels = image.load()
        rand = rng("copper_dimples")
        for _ in range(90):
            x, y = rand.randrange(64), rand.randrange(64)
            pixels[x, y] = (*shade(pixels[x, y], 26), 255)
            pixels[(x + 1) % 64, (y + 1) % 64] = (*shade(pixels[(x + 1) % 64, (y + 1) % 64], -18), 255)
        panel_seams(image, step=32, depth=30)
        blotches(image, "copper_patina", (74, 132, 112), 4, 9, 0.45)
    elif metal == "bronze":
        image = panel_seams(noisy(64, color, "bronze_shell", amount=6, streak=3), step=32, depth=28)
        blotches(image, "bronze_sheen", shade(color, 34), 4, 10, 0.4)
    elif metal == "steel":
        image = panel_seams(noisy(64, color, "steel_shell", amount=4, streak=4), step=16, depth=18)
    elif metal == "aluminum":
        image = panel_seams(noisy(64, color, "aluminum_shell", amount=3, streak=9), step=32, rivets=False, depth=16)
    elif metal == "titanium":
        image = panel_seams(noisy(64, color, "titanium_shell", amount=4, streak=5), step=16, rivets=False, depth=18)
        blotches(image, "titanium_heat", (128, 112, 168), 4, 12, 0.35)
    elif metal == "tungstensteel":
        image = panel_seams(noisy(64, color, "tungstensteel_shell", amount=4, streak=3), step=32, depth=30)
        pixels = image.load()
        for py in range(0, 64, 32):
            for px in range(0, 64, 32):
                for dx, dy in ((8, 8), (24, 8), (8, 24), (24, 24)):
                    for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                        pixels[px + dx + ox, py + dy + oy] = (*shade(color, 44 if (ox, oy) == (0, 0) else 18), 255)
    else:
        image = panel_seams(noisy(64, color, "exotic_shell", amount=5, streak=4), step=16, rivets=False, depth=26)
        pixels = image.load()
        for y in range(64):
            for x in range(64):
                if x % 16 == 0 or y % 16 == 0:
                    pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.3), 255)
    return image


def trim_texture(metal, color):
    image = noisy(64, shade(color, 14), f"{metal}_trim", amount=6, streak=2)
    if metal == "exotic":
        pixels = image.load()
        for y in range(0, 64, 8):
            for x in range(64):
                pixels[x, y] = (*mix(pixels[x, y], EXOTIC_GLOW, 0.15), 255)
    return image


def cap_texture(metal, color, shell):
    """The lid top: the tier plate with a bolted manway in the middle (texels 16 to 48, the hatch)."""
    image = shell.copy()
    draw = ImageDraw.Draw(image)
    draw.ellipse([18, 18, 45, 45], fill=(*shade(color, -26), 255))
    draw.ellipse([20, 20, 43, 43], fill=(*shade(color, 10), 255))
    draw.ellipse([25, 25, 38, 38], fill=(*shade(color, -8), 255), outline=(*shade(color, -34), 255))
    for i in range(8):
        angle = math.radians(i * 45 + 22.5)
        x, y = 31.5 + math.cos(angle) * 9.5, 31.5 + math.sin(angle) * 9.5
        draw.rectangle([round(x) - 1, round(y) - 1, round(x), round(y)], fill=(*shade(color, 48), 255))
    draw.rectangle([30, 20, 33, 23], fill=(*shade(color, -40), 255))
    return image


def port_ring(draw, x0, y0, x1, y1):
    """A teal-ringed socket with four copper contacts, inside the texel box."""
    w = x1 - x0
    draw.rectangle([x0, y0, x1, y1], outline=(*SLATE_DARK, 255), width=2)
    draw.rectangle([x0 + w * 6 // 64, y0 + w * 6 // 64, x1 - w * 6 // 64, y1 - w * 6 // 64], outline=(*TEAL_DARK, 255), width=max(1, w * 3 // 64))
    draw.rectangle([x0 + w * 8 // 64, y0 + w * 8 // 64, x1 - w * 8 // 64, y1 - w * 8 // 64], outline=(*shade(TEAL, 10), 255), width=1)
    c0, c1 = x0 + w * 20 // 64, x1 - w * 20 // 64
    draw.rectangle([c0, y0 + w * 20 // 64, c1, y1 - w * 20 // 64], fill=(14, 18, 24, 255), outline=(*STEEL, 255))
    s = max(1, w * 3 // 64)
    for cx, cy in ((c0 + 4 * s // 3, y0 + w * 24 // 64), (c1 - 2 * s, y0 + w * 24 // 64),
                   (c0 + 4 * s // 3, y1 - w * 24 // 64 - s), (c1 - 2 * s, y1 - w * 24 // 64 - s)):
        draw.rectangle([cx, cy, cx + s - 1, cy + s - 1], fill=(184, 115, 51, 255))


def write_tier_textures() -> None:
    for metal, color in METALS.items():
        shell = shell_texture(metal, color)
        save(shell, f"shell/{metal}")
        save(trim_texture(metal, color), f"trim/{metal}")
        save(cap_texture(metal, color, shell), f"cap/{metal}")


def write_servo_textures() -> None:
    """Ribbed pump barrels in each Servo material, and a bright accent for each one's band and crosshead."""
    for material, (color, accent) in SERVOS.items():
        image = noisy(64, color, f"servo_{material}", amount=5)
        pixels = image.load()
        for y in range(64):
            for x in range(64):
                if x % 4 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -30), 255)
                elif x % 4 == 1:
                    pixels[x, y] = (*shade(pixels[x, y], 20), 255)
        if material == "unique":
            rand = rng("servo_unique_flecks")
            for _ in range(60):
                x, y = rand.randrange(64), rand.randrange(64)
                pixels[x, y] = (*shade((232, 170, 96), rand.uniform(-10, 10)), 255)
        save(image, f"servo/{material}")
        band = noisy(64, accent, f"servo_{material}_accent", amount=6)
        pixels = band.load()
        for y in range(64):
            for x in range(64):
                if y % 4 == 3:
                    pixels[x, y] = (*shade(pixels[x, y], -36), 255)
        save(band, f"servo/{material}_accent")


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

    bottom = panel_seams(noisy(64, SLATE, "base_bottom", amount=4), step=16, rivets=False, depth=16)
    ImageDraw.Draw(bottom).rectangle([0, 0, 63, 63], outline=(*SLATE_DARK, 255), width=2)
    save(bottom, "base_bottom")

    band = noisy(64, TEAL, "band", amount=6)
    pixels = band.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(TEAL_DARK, 0), 255)
            elif y % 4 == 1:
                pixels[x, y] = (*shade(pixels[x, y], 30), 255)
    save(band, "band")

    # Glass: clear, with sparse diagonal glints so the pane reads without hiding the fluid behind it.
    glass = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = glass.load()
    for start, length in ((6, 9), (11, 4), (38, 7), (43, 3)):
        for i in range(length):
            x, y = start + i, 44 - i - (start % 7)
            if 0 <= x < 64 and 0 <= y < 64:
                pixels[x, y] = (214, 236, 242, 255)
    for x in range(64):
        if x % 16 in (5, 6):
            pixels[x, 2] = (190, 218, 226, 255)
    save(glass, "glass")

    # Wired glass for the Compressor Tank's feed tubes: a diamond mesh of steel wire, clear between the wires.
    wired = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = wired.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                pixels[x, y] = (150, 162, 172, 255)
    for x in range(64):
        if x % 8 in (2, 3):
            pixels[x, (x * 3) % 64] = (214, 236, 242, 255)
    save(wired, "tube_glass")

    # Gauge glass behind the compressed fill: dark, with graduations up the left edge (u 29 to 35, v 20 to 50).
    window = noisy(64, (16, 22, 28), "window", amount=3)
    pixels = window.load()
    for y in range(20, 51):
        if (50 - y) % 4 == 0:
            for x in range(29, 31 + (1 if (50 - y) % 8 == 0 else 0)):
                pixels[x, y] = (120, 140, 150, 255)
    save(window, "window")

    socket = noisy(64, (52, 58, 66), "socket", amount=4)
    pixels = socket.load()
    for y in range(64):
        for x in range(64):
            if (x % 10 in (1, 8)) and y % 10 in (1, 8):
                pixels[x, y] = (150, 158, 168, 255)
    save(socket, "socket")

    # Header pipework: plain brushed steel. Teal stays on the deliberate bands.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Manifold drum top, mapped by position (the hatch, texels 16 to 48): a bolted flange around a domed cap.
    manifold = noisy(64, SLATE, "manifold", amount=4)
    draw = ImageDraw.Draw(manifold)
    draw.rectangle([16, 16, 47, 47], outline=(*SLATE_DARK, 255), width=2)
    draw.ellipse([20, 20, 43, 43], fill=(*shade(SLATE, 16), 255), outline=(*SLATE_DARK, 255))
    draw.ellipse([25, 25, 38, 38], fill=(*shade(STEEL, 10), 255), outline=(*shade(STEEL, -20), 255))
    draw.ellipse([29, 29, 34, 34], fill=(*shade(STEEL, 34), 255))
    for i in range(8):
        angle = math.radians(i * 45)
        x, y = 31.5 + math.cos(angle) * 9.5, 31.5 + math.sin(angle) * 9.5
        draw.rectangle([round(x) - 1, round(y) - 1, round(x), round(y)], fill=(150, 158, 168, 255))
    save(manifold, "manifold")

    port = noisy(64, SLATE, "port", amount=4)
    draw = ImageDraw.Draw(port)
    port_ring(draw, 0, 0, 63, 63)
    for x, y in ((2, 2), (59, 2), (2, 59), (59, 59)):
        draw.rectangle([x, y, x + 2, y + 2], fill=(150, 158, 168, 255))
    save(port, "port")

    # Pressure gauge, mapped whole onto the dial: a white face with a scale from green through amber to a red line.
    # The renderer draws the needle about the centre, sweeping clockwise from the lower left (225 degrees) over 270.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(222, 226, 210, 255))
    for step in range(10):
        fraction = step / 9
        angle = math.radians(225 - 270 * fraction)
        x, y = 7.5 + math.cos(angle) * 5, 7.5 - math.sin(angle) * 5
        color = (70, 160, 80) if fraction < 0.6 else (226, 160, 50) if fraction < 0.85 else (200, 50, 36)
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    save(gauge, "gauge")

    save(noisy(64, (52, 30, 12), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (255, 176, 64), "lamp_on", amount=6), "lamp_on")

    write_tier_textures()
    write_servo_textures()


def remove_cube_textures() -> None:
    """The cube textures this model replaces."""
    for face in ("bottom", "front", "side", "top"):
        shutil.rmtree(TEXTURES / face, ignore_errors=True)


def main() -> None:
    remove_cube_textures()
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
