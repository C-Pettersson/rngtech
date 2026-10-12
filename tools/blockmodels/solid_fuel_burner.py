#!/usr/bin/env python3
"""Generate the Solid Fuel Burner block models, part models, item models, and owned textures.

The design is a small firebox generator: fuel drops from a side hopper (the Fuel Box) onto a
grate inside a firebrick-lined firebox behind a front grille. The firebox crown carries the
Heat Core as a stack of cooling fins, which feed a thermoelectric converter cabinet behind
the firebox; the flue gas leaves through a duct and chimney on top of the cabinet. All four
chassis share one shape and differ by their shell and trim material.

The housing is a static block model. The installed Heat Core fins and Fuel Box hopper are
standalone part models; they, the fuel levels, the fire, and the charge
meter are drawn by SolidFuelBurnerRenderer, whose constants match the geometry here. Run from
the repository root:

    python tools/blockmodels/solid_fuel_burner.py
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
TEXTURES = ASSETS / "textures/block/solid_fuel_burner"
TEX = "rngtech:block/solid_fuel_burner/"
MODEL = "rngtech:block/solid_fuel_burner"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# Firebox: shell from x 3 to 13 and z 1 to 10; the front faces north (z = 0).
FIREBOX = ((3.0, 2.0, 1.0), (13.0, 11.0, 10.0))
# Firebrick-lined cavity inside the walls, open at the front behind the door frame.
CAVITY = ((4.5, 4.5, 1.0), (11.5, 10.0, 8.5))
# Door opening inside the frame, and the grate the fuel bed lies on.
OPENING = ((4.75, 5.25), (11.25, 9.25))
GRATE_Y0, GRATE_Y1 = 5.0, 5.5
GRATE_Z0, GRATE_Z1 = 1.25, 8.25
GRATE_XS = [4.75 + i for i in range(7)]
# Heat Core fins on the firebox crown.
FIN_COUNT = 8
FIN_X0, FIN_X1 = 3.75, 12.25
FIN_WIDTH = 0.5
FIN_Y0, FIN_Y1 = 12.0, 15.0
FIN_Z0, FIN_Z1 = 2.0, 8.0
# Fuel Box hopper on the west side; WALL is the sheet thickness.
HOPPER = ((0.5, 5.0, 2.0), (3.0, 12.5, 8.5))
WALL = 0.375
HOPPER_FLOOR = 5.5
DIVIDER = 0.375
# Thermoelectric converter cabinet behind the firebox, with a charge meter on its east side.
CABINET = ((2.5, 2.0, 10.0), (13.5, 12.0, 15.0))
METER = ((13.5, 3.0, 11.5), (14.0, 11.0, 13.5))
# Chimney on the cabinet: a rounded stack around (CHIMNEY_X, CHIMNEY_Z).
CHIMNEY_X, CHIMNEY_Z = 11.0, 12.5
CHIMNEY_R, CHIMNEY_FLAT = 1.75, 1.0
# Cable port on the back of the cabinet, sized to the Universal Connector ring (4 to 12 on the face).
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))

CHASSIS = ("crude", "copper", "alloy", "steel")
# Gear shown on the item model, which has no renderer: the parts that match each chassis stage.
ITEM_PARTS = {"crude": ("iron", "iron"), "copper": ("copper", "copper"), "alloy": ("bronze", "bronze"), "steel": ("steel", "steel")}
# Heat Core materials (the item palettes), plus Unique cores.
HEAT_CORES = {
    "iron": (176, 176, 168),
    "copper": (208, 118, 60),
    "bronze": (182, 122, 56),
    "steel": (118, 126, 130),
    "aluminum": (186, 198, 214),
    "sparksteel": (224, 192, 96),
    "titanium": (150, 164, 180),
    "exotic": (52, 170, 132),
    "unique": (175, 96, 37),
}
FUEL_BOXES = {
    "iron": (170, 170, 162),
    "copper": (200, 112, 56),
    "bronze": (176, 116, 52),
    "steel": (112, 120, 124),
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


def stack(cx, cz, radius, flat, y0, y1, textures, faces=FACES):
    """A rounded vertical stack: two crossed slabs, axis-aligned so it also renders as a wiki icon."""
    return [
        box((cx - radius, y0, cz - flat), (cx + radius, y1, cz + flat), textures, faces),
        box((cx - flat, y0, cz - radius), (cx + flat, y1, cz + radius), textures, faces),
    ]


def housing_elements():
    elements = []
    # Plinth with teal hazard band and tread top.
    elements.append(box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"}))

    # Firebox: ash pit, side walls, back wall, and crown around a firebrick cavity.
    (fx0, fy0, fz0), (fx1, fy1, fz1) = FIREBOX
    (cx0, cy0, cz0), (cx1, cy1, cz1) = CAVITY
    elements.append(box((fx0, fy0, fz0), (fx1, cy0, fz1), {"up": "ash", "*": "shell"}, faces=("north", "east", "west", "up")))
    elements.append(box((fx0, cy0, fz0), (cx0, cy1, fz1), {"east": "firebrick", "*": "shell"}, faces=("north", "east", "west")))
    elements.append(box((cx1, cy0, fz0), (fx1, cy1, fz1), {"west": "firebrick", "*": "shell"}, faces=("north", "east", "west")))
    elements.append(box((cx0, cy0, cz1), (cx1, cy1, fz1), {"north": "firebrick"}, faces=("north",)))
    elements.append(box((fx0, cy1, fz0), (fx1, fy1, fz1), {"down": "firebrick", "up": "crown", "*": "shell"}))

    # Grate bars on two cross rails.
    for x in GRATE_XS:
        elements.append(box((x, GRATE_Y0, GRATE_Z0), (x + 0.5, GRATE_Y1, GRATE_Z1), "grate"))
    for z in (2.0, 6.75):
        elements.append(box((cx0, 4.5, z), (cx1, GRATE_Y0, z + 0.75), "grate", faces=("north", "south", "up")))

    # Door frame around the opening, a grille across it, and the ash-pit door with its draught vents below.
    (ox0, oy0), (ox1, oy1) = OPENING
    elements.append(box((3.25, oy1, 0.25), (12.75, 10.75, 1.0), "trim", faces=("north", "east", "west", "up", "down")))
    elements.append(box((3.25, 4.0, 0.25), (12.75, oy0, 1.0), "trim", faces=("north", "east", "west", "up", "down")))
    elements.append(box((3.25, oy0, 0.25), (ox0, oy1, 1.0), "trim", faces=("north", "east", "west")))
    elements.append(box((ox1, oy0, 0.25), (12.75, oy1, 1.0), "trim", faces=("north", "east", "west")))
    pitch = (ox1 - ox0) / 5
    for i in range(1, 5):
        x = ox0 + pitch * i
        elements.append(box((x - 0.25, oy0, 0.5), (x + 0.25, oy1, 0.9), "grille", faces=("north", "south", "east", "west")))
    elements.append(box((ox0, 7.0, 0.3), (ox1, 7.5, 0.6), "grille", faces=("north", "south", "up", "down")))
    elements.append(box((4.5, 2.25, 0.5), (11.5, 4.0, 1.0), {"north": "ash_door", "*": "trim"},
                        faces=("north", "east", "west", "up"), uv={"north": [0, 0, 16, 4]}))

    # Primary-air damper on the east wall.
    elements.append(box((fx1, 5.0, 3.0), (fx1 + 0.5, 8.5, 7.0), {"east": "damper", "*": "trim"},
                        faces=("north", "south", "east", "up", "down"), uv={"east": [0, 0, 16, 14]}))

    # Heat Core socket on the crown; the fins themselves are a part model.
    elements.append(box((3.5, fy1, 1.5), (12.5, 11.5, 8.5), "socket", faces=("north", "south", "east", "west", "up")))

    # Fuel Box mounting bracket under the hopper.
    elements.append(box((1.0, 2.0, 3.0), (2.5, HOPPER[0][1], 7.5), "casing", faces=("north", "south", "west", "up")))

    # Converter cabinet with the cable port on its back and the charge meter on its east side.
    (kx0, ky0, kz0), (kx1, ky1, kz1) = CABINET
    elements.append(box((kx0, ky0, kz0), (kx1, ky1, kz1), {"west": "cabinet", "*": "casing"},
                        faces=("north", "south", "east", "west", "up"), uv={"west": [0, 0, 8, 16]}))
    # Teal band around the cabinet top, stopping at the back so it never crosses the port face.
    elements.append(box((kx0 - 0.25, ky1 - 1.0, kz0 - 0.25), (kx1 + 0.25, ky1 - 0.5, kz1), "band",
                        faces=("north", "east", "west", "down")))
    elements.append(box(*PORT, {"south": "port", "*": "casing"}, faces=("south", "east", "west", "up", "down"),
                        uv={"south": [0, 0, 16, 16]}))
    elements.append(box(*METER, {"east": "meter", "*": "trim"}, faces=("north", "south", "east", "up", "down"),
                        uv={"east": [0, 0, 4, 16]}))

    # Conduits from the fin base into the converter, and the flue duct from the crown into the chimney.
    for x in (4.5, 6.25):
        elements.append(box((x, 11.5, 8.25), (x + 0.75, 12.25, kz0), "pipe", faces=("east", "west", "up")))
    elements.append(box((9.75, fy1, 8.5), (12.25, 13.0, CHIMNEY_Z - CHIMNEY_R), "pipe", faces=("north", "east", "west", "up")))

    # Chimney: base flange, stack, teal collar, and a lip around the sooty mouth.
    top = ky1
    elements += stack(CHIMNEY_X, CHIMNEY_Z, CHIMNEY_R + 0.5, CHIMNEY_FLAT + 0.5, top, top + 0.5, "trim",
                      faces=("north", "south", "east", "west", "up"))
    elements += stack(CHIMNEY_X, CHIMNEY_Z, CHIMNEY_R, CHIMNEY_FLAT, top + 0.5, 15.5, "shell", faces=SIDES)
    elements += stack(CHIMNEY_X, CHIMNEY_Z, CHIMNEY_R + 0.25, CHIMNEY_FLAT + 0.25, 14.0, 14.5, "band",
                      faces=("north", "south", "east", "west", "up", "down"))
    elements += stack(CHIMNEY_X, CHIMNEY_Z, CHIMNEY_R + 0.25, CHIMNEY_FLAT + 0.25, 15.5, 16.0, {"up": "soot", "*": "trim"},
                      faces=("north", "south", "east", "west", "up", "down"))

    # Temperature dial and status lamp on the plinth front.
    elements.append(box((0.5, 2.0, 0.25), (3.0, 4.5, 1.25), {"north": "gauge", "*": "casing"}, uv={"north": [0, 0, 16, 16]}))
    elements.append(box(
        (13.25, 2.0, 0.5), (14.75, 3.0, 1.5), "lamp",
        extra={"neoforge_data": {"block_light": 15, "sky_light": 15}},
    ))
    return elements


def fin_elements(texture="core"):
    """The Heat Core: a base plate and a row of cooling fins on the firebox crown."""
    elements = [box((FIN_X0, 11.5, FIN_Z0 - 0.25), (FIN_X1, FIN_Y0, FIN_Z1 + 0.25), texture, faces=("north", "south", "east", "west", "up"))]
    pitch = (FIN_X1 - FIN_X0 - FIN_WIDTH) / (FIN_COUNT - 1)
    for i in range(FIN_COUNT):
        x = FIN_X0 + pitch * i
        elements.append(box((x, FIN_Y0, FIN_Z0), (x + FIN_WIDTH, FIN_Y1, FIN_Z1), texture, faces=("north", "south", "east", "west", "up")))
    return elements


def hopper_elements(texture="box"):
    """The Fuel Box: an open-topped sheet hopper beside the firebox."""
    (x0, y0, z0), (x1, y1, z1) = HOPPER
    return [
        box((x0, y0, z0), (x0 + WALL, y1, z1), texture),
        box((x1 - WALL, y0, z0), (x1, y1, z1), texture),
        box((x0 + WALL, y0, z0), (x1 - WALL, y1, z0 + WALL), texture),
        box((x0 + WALL, y0, z1 - WALL), (x1 - WALL, y1, z1), texture),
        box((x0 + WALL, y0, z0 + WALL), (x1 - WALL, HOPPER_FLOOR, z1 - WALL), texture, faces=("up", "down")),
    ]


def divider_elements(texture="box"):
    """The partition of a two-slot Fuel Box."""
    (x0, _, z0), (x1, y1, z1) = HOPPER
    mid = (z0 + z1) / 2
    return [box((x0 + WALL, HOPPER_FLOOR, mid - DIVIDER / 2), (x1 - WALL, y1 - 0.25, mid + DIVIDER / 2), texture,
                faces=("north", "south", "up"))]


def item_fuel_elements():
    """Static coal for the item model: a full hopper and an unlit charge on the grate."""
    (x0, _, z0), (x1, y1, z1) = HOPPER
    (ox0, _), (ox1, _) = OPENING
    return [
        box((x0 + WALL, HOPPER_FLOOR, z0 + WALL), (x1 - WALL, y1 - 1.0, z1 - WALL), "coal", faces=("up",)),
        box((ox0, GRATE_Y1, GRATE_Z0 + 0.25), (ox1, GRATE_Y1 + 1.0, GRATE_Z1 - 0.25), "coal", faces=("north", "up")),
    ]


SHARED_TEXTURES = {
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "firebrick": TEX + "firebrick",
    "ash": TEX + "ash",
    "crown": TEX + "crown",
    "grate": TEX + "grate",
    "grille": TEX + "grille",
    "ash_door": TEX + "ash_door",
    "damper": TEX + "damper",
    "socket": TEX + "socket",
    "cabinet": TEX + "cabinet",
    "band": TEX + "band",
    "port": TEX + "port",
    "meter": TEX + "meter",
    "pipe": TEX + "pipe",
    "soot": TEX + "soot",
    "gauge": TEX + "gauge",
    "lamp": TEX + "lamp_off",
}


def chassis_textures(chassis):
    return {
        "particle": TEX + f"shell/{chassis}",
        "shell": TEX + f"shell/{chassis}",
        "trim": TEX + f"trim/{chassis}",
    }


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    housing = housing_elements()
    write_json(MODELS / "block/solid_fuel_burner.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": SHARED_TEXTURES,
        "elements": housing,
    })
    write_json(MODELS / "item/solid_fuel_burner_template.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {**SHARED_TEXTURES, "coal": TEX + "coal"},
        "elements": item_fuel_elements() + fin_elements() + hopper_elements() + housing,
    })
    for chassis in CHASSIS:
        name = f"{chassis}_solid_fuel_burner"
        write_json(MODELS / f"block/{name}.json", {"parent": MODEL, "textures": chassis_textures(chassis)})
        write_json(MODELS / f"block/{name}_active.json", {"parent": f"rngtech:block/{name}", "textures": {"lamp": TEX + "lamp_on"}})
        core, fuel_box = ITEM_PARTS[chassis]
        write_json(MODELS / f"item/{name}.json", {
            "parent": "rngtech:item/solid_fuel_burner_template",
            "textures": {**chassis_textures(chassis), "core": TEX + f"heat_core/{core}", "box": TEX + f"fuel_box/{fuel_box}"},
        })

    parts = MODELS / "block/solid_fuel_burner"
    write_json(parts / "heat_core.json", {"textures": {"particle": "#core"}, "elements": fin_elements()})
    write_json(parts / "fuel_box.json", {"textures": {"particle": "#box"}, "elements": hopper_elements()})
    write_json(parts / "fuel_box_divider.json", {"textures": {"particle": "#box"}, "elements": divider_elements()})
    for material in HEAT_CORES:
        write_json(parts / f"heat_core_{material}.json", {
            "parent": f"{MODEL}/heat_core",
            "textures": {"core": TEX + f"heat_core/{material}"},
        })
    for material in FUEL_BOXES:
        for part in ("fuel_box", "fuel_box_divider"):
            write_json(parts / f"{part}_{material}.json", {
                "parent": f"{MODEL}/{part}",
                "textures": {"box": TEX + f"fuel_box/{material}"},
            })


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"solid_fuel_burner:{name}")


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
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
GLINT = (206, 240, 250, 255)
DIAL_FACE = (222, 226, 210)


def write_chassis_textures() -> None:
    """Shell and trim for each chassis: the same design in cast iron, copper, bronze alloy, and blued steel."""
    # Crude: blackened cast iron, coarse rivets, and rust.
    shell = panel_seams(noisy(64, (62, 58, 54), "crude_shell", amount=9), step=32, depth=26)
    blotches(shell, "crude_rust", (122, 66, 34), 7, 12, 0.7)
    save(shell, "shell/crude")
    trim = noisy(64, (84, 78, 72), "crude_trim", amount=10)
    blotches(trim, "crude_trim_rust", (110, 62, 36), 5, 9, 0.6)
    save(trim, "trim/crude")

    # Copper: riveted sheet with hammered dimples and darker tarnish.
    shell = noisy(64, (136, 78, 46), "copper_shell", amount=7)
    pixels = shell.load()
    rand = rng("copper_dimples")
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], 26), 255)
        pixels[(x + 1) % 64, (y + 1) % 64] = (*shade(pixels[(x + 1) % 64, (y + 1) % 64], -18), 255)
    panel_seams(shell, step=32, depth=30)
    blotches(shell, "copper_tarnish", (96, 52, 32), 5, 11, 0.55)
    save(shell, "shell/copper")
    save(noisy(64, (216, 138, 86), "copper_trim", amount=8, streak=6), "trim/copper")

    # Alloy: brushed bronze with fine seams.
    shell = noisy(64, (166, 122, 64), "alloy_shell", amount=5, streak=10)
    panel_seams(shell, step=16, rivets=False, depth=24)
    save(shell, "shell/alloy")
    save(noisy(64, (206, 170, 94), "alloy_trim", amount=6, streak=8), "trim/alloy")

    # Steel: blued steel panels with small rivets.
    shell = noisy(64, (72, 82, 94), "steel_shell", amount=4, streak=4)
    panel_seams(shell, step=16, depth=18)
    save(shell, "shell/steel")
    save(noisy(64, (150, 158, 168), "steel_trim", amount=5, streak=5), "trim/steel")


def write_part_textures() -> None:
    # Heat Core fins: brushed metal of the core's material, darker at the edges of each fin pitch.
    for material, color in HEAT_CORES.items():
        fins = noisy(64, color, f"core_{material}", amount=5, streak=8)
        pixels = fins.load()
        for y in range(64):
            for x in range(64):
                if y % 16 == 15:
                    pixels[x, y] = (*shade(pixels[x, y], -26), 255)
                elif y % 16 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], 18), 255)
        if material == "unique":
            rand = rng("core_unique_flecks")
            for _ in range(60):
                x, y = rand.randrange(64), rand.randrange(64)
                pixels[x, y] = (*shade((232, 170, 96), rand.uniform(-10, 10)), 255)
        save(fins, f"heat_core/{material}")

    # Fuel Box hopper: plain sheet with a riveted rim every 16 texels.
    for material, color in FUEL_BOXES.items():
        sheet = noisy(64, color, f"box_{material}", amount=6, streak=3)
        pixels = sheet.load()
        for y in range(64):
            for x in range(64):
                if x % 16 == 0 or y % 16 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], -24), 255)
                elif (x % 16 == 2 or y % 16 == 2) and (x + y) % 4 == 0:
                    pixels[x, y] = (*shade(pixels[x, y], 30), 255)
        save(sheet, f"fuel_box/{material}")


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

    # Firebrick: refractory brick courses, sooted toward the top of each 16-texel tile.
    bricks = noisy(64, (140, 72, 48), "firebrick", amount=10)
    pixels = bricks.load()
    for y in range(64):
        for x in range(64):
            row = y // 8
            offset = 8 if row % 2 else 0
            if y % 8 == 0 or (x + offset) % 16 == 0:
                pixels[x, y] = (*shade((74, 62, 56), 0), 255)
            else:
                pixels[x, y] = (*mix(pixels[x, y], (40, 26, 22), 0.35 * (1.0 - (y % 16) / 16)), 255)
    save(bricks, "firebrick")

    ash = noisy(64, (118, 114, 108), "ash", amount=14)
    pixels = ash.load()
    rand = rng("ash_cinders")
    for _ in range(70):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((44, 40, 38), rand.uniform(-6, 6)), 255)
    save(ash, "ash")

    # Firebox crown: heat-darkened plate.
    crown = noisy(64, (58, 56, 56), "crown", amount=5)
    blotches(crown, "crown_heat", (78, 60, 52), 4, 14, 0.5)
    save(crown, "crown")

    save(noisy(64, (50, 48, 47), "grate", amount=6), "grate")
    save(noisy(64, (74, 78, 84), "grille", amount=5, streak=4), "grille")
    save(noisy(64, (24, 22, 22), "soot", amount=4), "soot")

    # Heat Core socket: dark plate with a bolt row along each 16-texel tile edge.
    socket = noisy(64, (52, 58, 66), "socket", amount=4)
    pixels = socket.load()
    for y in range(64):
        for x in range(64):
            if (x % 16 in (2, 13)) and y % 6 == 3:
                pixels[x, y] = (*shade((150, 158, 168), 0), 255)
                pixels[x, (y + 1) % 64] = (*shade(SLATE_DARK, -6), 255)
    save(socket, "socket")

    # Pipework: plain brushed steel. Teal stays on the deliberate bands, never on runs of pipe.
    save(noisy(64, (124, 134, 146), "pipe", amount=4, streak=5), "pipe")

    # Ash-pit door, mapped as a 4:1 strip: three draught slots. SolidFuelBurnerRenderer slides shutters over them by stoke.
    door = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    door.paste(noisy(64, (70, 74, 80), "ash_door", amount=5).crop((0, 0, 64, 16)), (0, 0))
    draw = ImageDraw.Draw(door)
    draw.rectangle([0, 0, 63, 15], outline=(*SLATE_DARK, 255))
    for x0 in (10, 26, 42):
        draw.rectangle([x0, 5, x0 + 11, 10], fill=(16, 14, 14, 255))
        draw.line([(x0, 11), (x0 + 11, 11)], fill=(*shade(STEEL, 30), 255))
    draw.rectangle([56, 6, 59, 9], fill=(150, 158, 168, 255))
    save(door, "ash_door")

    # Primary-air damper, mapped as a 16:14 plate: angled louvres.
    damper = noisy(64, (66, 70, 76), "damper", amount=4)
    pixels = damper.load()
    for y in range(4, 52):
        for x in range(6, 58):
            if y % 6 in (0, 1):
                pixels[x, y] = (*shade(SLATE_DARK, -10), 255)
            elif y % 6 == 2:
                pixels[x, y] = (*shade(STEEL, 26), 255)
    save(damper, "damper")

    # Converter cabinet side, mapped as an 8:16 panel: seams and a block of cooling louvres.
    cabinet = panel_seams(noisy(64, (40, 47, 57), "cabinet", amount=4), step=32, rivets=False)
    pixels = cabinet.load()
    for y in range(14, 50):
        if y % 3 == 0:
            for x in range(6, 26):
                pixels[x, y] = (*shade(SLATE_DARK, -8), 255)
        elif y % 3 == 1:
            for x in range(6, 26):
                pixels[x, y] = (*shade(SLATE, 18), 255)
    save(cabinet, "cabinet")

    # Charge meter, mapped as a 2:8 strip (columns 0-15): steel rails around eight dark LED slots.
    meter = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pixels = meter.load()
    for y in range(64):
        for x in range(16):
            if x in (0, 1, 14, 15) or y in (0, 1, 62, 63):
                edge = x in (0, 15) or y in (0, 63)
                pixels[x, y] = (*shade(STEEL, 14 if edge else -12), 255)
            else:
                pixels[x, y] = (18, 22, 28, 255)
    for i in range(METER_SEGMENTS):
        bottom, top = meter_segment(i)
        for y in range(round((METER[1][1] - top) * 8), round((METER[1][1] - bottom) * 8)):
            for x in range(4, 12):
                pixels[x, y] = (36, 44, 40, 255)
    save(meter, "meter")

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
        draw.rectangle([x, y, x + 2, y + 2], fill=(184, 115, 51, 255))
    save(port, "port")

    # Temperature dial: grey to amber to red scale, needle in the working band.
    gauge = Image.new("RGBA", (16, 16), (*SLATE_DARK, 255))
    draw = ImageDraw.Draw(gauge)
    draw.ellipse([1, 1, 14, 14], fill=(120, 128, 138, 255))
    draw.ellipse([2, 2, 13, 13], fill=(*DIAL_FACE, 255))
    scale = [(-225, (150, 150, 150)), (-180, (170, 166, 150)), (-135, (210, 180, 80)), (-90, (226, 150, 50)),
             (-45, (220, 110, 40)), (0, (200, 60, 36)), (45, (170, 30, 30))]
    for angle, color in scale:
        x, y = 7.5 + math.cos(math.radians(angle)) * 5, 7.5 + math.sin(math.radians(angle)) * 5
        gauge.putpixel((int(round(x)), int(round(y))), (*color, 255))
    draw.line([(7.5, 7.5), (10.5, 4.5)], fill=(30, 30, 30, 255))
    gauge.putpixel((7, 7), (30, 30, 30, 255))
    save(gauge, "gauge")

    save(noisy(64, (52, 30, 12), "lamp_off", amount=3), "lamp_off")
    save(noisy(64, (255, 176, 64), "lamp_on", amount=6), "lamp_on")

    # Item-model coal: black lumps with a few glints.
    coal = noisy(64, (34, 33, 34), "coal", amount=8)
    pixels = coal.load()
    rand = rng("coal_glints")
    for _ in range(50):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade((92, 92, 98), rand.uniform(-8, 8)), 255)
    save(coal, "coal")

    write_chassis_textures()
    write_part_textures()
    write_fx_textures()


def write_fx_textures() -> None:
    """Renderer textures: tintable fuel, embers, flame tongue, firelight, and LED."""
    # Fuel lumps in light neutral greys; the renderer tints them by the fuel item.
    fuel = Image.new("RGBA", (16, 16))
    pixels = fuel.load()
    rand = rng("fuel_lumps")
    centres = [(rand.uniform(0, 16), rand.uniform(0, 16), rand.uniform(2.0, 3.6)) for _ in range(14)]
    for y in range(16):
        for x in range(16):
            best = min(((math.hypot(x + 0.5 - cx, y + 0.5 - cy) / rad), rad) for cx, cy, rad in centres)
            light = 250 - min(1.0, best[0]) * 90 + rand.uniform(-10, 10)
            pixels[x, y] = (clamp(light), clamp(light), clamp(light), 255)
    save(fuel, "fx/fuel")

    # Embers: glowing coals with dark cracks.
    embers = noisy(16, (240, 120, 40), "embers", amount=24)
    pixels = embers.load()
    rand = rng("ember_cracks")
    for _ in range(30):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (*shade((120, 40, 16), rand.uniform(-10, 10)), 255)
    for _ in range(16):
        x, y = rand.randrange(16), rand.randrange(16)
        pixels[x, y] = (255, 226, 130, 255)
    save(embers, "fx/embers")

    # Flame tongue for additive blending: black is transparent, the core is bright yellow.
    flame = Image.new("RGBA", (32, 32))
    pixels = flame.load()
    for y in range(32):
        for x in range(32):
            height = 1.0 - (y + 0.5) / 32
            half = 0.5 * (1.0 - height) ** 0.7 * (0.35 + 0.65 * math.sin(math.pi * min(1.0, (1.0 - height) * 1.15)))
            dx = abs(x + 0.5 - 16) / 16
            if half <= 0 or dx >= half:
                pixels[x, y] = (0, 0, 0, 255)
                continue
            core = 1.0 - dx / half
            heat = core * (0.55 + 0.45 * (1.0 - height))
            pixels[x, y] = (clamp(255 * min(1.0, heat * 1.6)), clamp(210 * heat ** 1.4), clamp(90 * heat ** 3), 255)
    save(flame, "fx/flame")

    # Firelight on the cavity walls: a soft orange radial gradient.
    glow = Image.new("RGBA", (16, 16))
    pixels = glow.load()
    for y in range(16):
        for x in range(16):
            distance = min(1.0, math.hypot(x + 0.5 - 8, y + 0.5 - 8) / 8)
            strength = (1.0 - distance) ** 1.4
            pixels[x, y] = (clamp(255 * strength), clamp(120 * strength), clamp(30 * strength), 255)
    save(glow, "fx/glow")

    led = Image.new("RGBA", (16, 16), (236, 236, 236, 255))
    draw = ImageDraw.Draw(led)
    draw.rectangle([0, 0, 15, 15], outline=(196, 196, 196, 255))
    draw.line([(2, 2), (13, 2)], fill=(255, 255, 255, 255))
    save(led, "fx/led")


METER_SEGMENTS = 8


def meter_segment(index):
    """Bottom and top of one charge-meter LED, in model pixels."""
    y0 = METER[0][1] + 0.5 + index * 0.875
    return y0, y0 + 0.625


def main() -> None:
    write_models()
    write_textures()


if __name__ == "__main__":
    main()
