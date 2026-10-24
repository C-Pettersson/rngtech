#!/usr/bin/env python3
"""Generate the Affix Forge block model, upgrade part models, item model, and owned textures.

The Affix Forge is a refinement altar on the slate plinth. In the middle, a slate pedestal carries an iron anvil plate
inlaid with a lapis ring, and the item being refined stands on a cup at its centre. The catalyst lies in a copper dish
at the front, fed into the pedestal by a redstone trace. Behind the pedestal, a back wall carries a lapis sigil and,
on its crown, the mount for the Lens Array: a boom that reaches over the target and holds a copper loupe with a glass
lens and two satellite lenses aimed down at the item. The Modifier Socket is a copper claw cup on the stand to the
right of the pedestal, and the Resonance Matrix is a violet crystal held between two collars in the tower to its left.
Without an upgrade, only its bare mount shows.

The housing is a static block model, and the three upgrades are blockstate parts. The target, the catalyst, the focus,
the matrix glow, the lapis ring, and the loupe beam are drawn by AffixForgeRenderer, whose constants match the geometry
here. Run from the repository root:

    python tools/blockmodels/affix_forge.py
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
TEXTURES = ASSETS / "textures/block/affix_forge"
TEX = "rngtech:block/affix_forge/"
OLD_TEXTURES = ("bottom", "core", "side", "top", "lens_array", "modifier_socket", "resonance_matrix")
OLD_PARTS = ("lens_array", "modifier_socket", "resonance_matrix")
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")

# ---------------------------------------------------------------- geometry
# The front faces north (z = 0); facing the front, east (+x) is on the left.
# Pedestal: a base slab, a column with a teal band, and the iron anvil plate on top.
BASE = ((3.0, 2.0, 3.0), (13.0, 3.5, 12.0))
COLUMN = ((4.5, 3.5, 4.5), (11.5, 6.5, 10.5))
PLATE = ((3.5, 6.5, 3.0), (12.5, 7.5, 12.0))
# The target stands on a cup at the plate's centre.
TARGET_X, TARGET_Z = 8.0, 7.5
CUP = ((7.25, 7.5, 6.75), (8.75, 7.8, 8.25))
TARGET_SIZE = 4.0
# Lapis ring inlaid in the plate, centred on the target, in model pixels.
RING_OUTER, RING_INNER = 3.75, 3.0

# Catalyst dish at the front, and the redstone trace from it to the column.
DISH = ((5.0, 2.0, 0.5), (11.0, 2.75, 3.0))
DISH_LIP = 0.4
DISH_RIM = 3.25
TRACE_X = (7.75, 8.25)

# Back wall with the lapis sigil and a teal band; the Lens Array mount sits on its crown.
WALL = ((3.5, 2.0, 13.0), (12.5, 12.0, 15.5))
SIGIL = ((4.5, 4.5), (11.5, 11.5))
SIGIL_Z = 12.9
MOUNT = ((6.5, 12.0, 12.75), (9.5, 13.0, 15.25))
# Lens Array: a riser and boom from the mount to a hanger that holds the loupe, a flat octagonal ring with a glass
# lens, level over the target; a satellite lens on each side is tilted toward it.
BOOM_Y = (15.0, 15.75)
LOUPE_Y = (13.0, 13.75)
LOUPE_OUTER, LOUPE_INNER = 2.25, 1.5
GLASS_Y = (13.2, 13.55)
SATELLITE_X = 3.4
SATELLITE_OUTER, SATELLITE_INNER = 0.85, 0.45
# The focus lens clips to the loupe's front.
LENS_CLIP = (8.0, 13.6, 5.0)

# Modifier Socket stand on the west (right, facing the front): a base, a post, and the bare mount plate.
SOCKET_X, SOCKET_Z = 2.0, 8.5
SOCKET_BASE = ((0.5, 2.0, 6.0), (3.5, 3.5, 11.0))
SOCKET_POST = ((1.25, 3.5, 7.75), (2.75, 8.5, 9.25))
SOCKET_MOUNT = ((0.75, 8.5, 7.25), (3.25, 9.0, 9.75))
SOCKET_CUP_Y = (9.0, 9.5)
SOCKET_CLAW_TOP = 10.75
SOCKET_CRYSTAL_Y = 10.5

# Resonance Matrix tower on the east (left, facing the front): a base, a back post, a top arm, and two collars.
MATRIX = (14.0, 8.75, 8.5)
MATRIX_LAYERS = ((0.45, 1.25), (1.05, 0.95), (1.6, 0.65), (2.1, 0.38), (2.45, 0.18))
TOWER_BASE = ((12.5, 2.0, 6.0), (15.5, 3.5, 11.0))
TOWER_POST = ((13.0, 3.5, 10.25), (15.0, 14.5, 12.25))
TOWER_ARM = ((13.25, 13.75, 7.75), (14.75, 14.5, 10.25))


def r(value: float) -> float:
    return round(value, 4)


def box(start, end, textures, faces=FACES, uv=None, rotation=None, tint=False):
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
        if tint:
            entry["tintindex"] = 0
        element["faces"][face] = entry
    return element


def plinth():
    return box((0, 0, 0), (16, 2, 16), {"up": "plinth_top", "down": "casing", "*": "plinth_side"})


def pedestal_elements():
    (px0, py0, pz0), (px1, py1, pz1) = PLATE
    (cx0, cy0, cz0), (cx1, cy1, cz1) = COLUMN
    return [
        box(*BASE, "casing", faces=SIDES + ("up",)),
        box(*COLUMN, "casing", faces=SIDES),
        box((cx0 - 0.25, 5.0, cz0 - 0.25), (cx1 + 0.25, 5.5, cz1 + 0.25), "band", faces=SIDES + ("up", "down")),
        box(*PLATE, {"up": "plate", "*": "anvil"}, uv={"up": [0, 0, px1 - px0, pz1 - pz0]}),
        box(*CUP, "pipe", faces=SIDES + ("up",)),
    ]


def dish_elements():
    (x0, y0, z0), (x1, y1, z1) = DISH
    lip = DISH_LIP
    top = DISH_RIM
    t0, t1 = TRACE_X
    (_, by0, bz0), (_, by1, _) = BASE
    return [
        box((x0, y0, z0), (x1, y1, z1), {"up": "dish", "*": "copper"}, faces=("north", "east", "west", "up")),
        box((x0, y1, z0), (x1, top, z0 + lip), "copper", faces=("north", "south", "east", "west", "up")),
        box((x0, y1, z1 - lip), (x1, top, z1), "copper", faces=("north", "south", "east", "west", "up")),
        box((x0, y1, z0 + lip), (x0 + lip, top, z1 - lip), "copper", faces=("east", "west", "up")),
        box((x1 - lip, y1, z0 + lip), (x1, top, z1 - lip), "copper", faces=("east", "west", "up")),
        # Redstone trace from the dish up the base front, into the pedestal under the plate.
        box((t0, by0 + 0.75, bz0 - 0.05), (t1, by1, bz0), "redstone", faces=("north",)),
    ]


def wall_elements():
    (x0, y0, z0), (x1, y1, z1) = WALL
    (sx0, sy0), (sx1, sy1) = SIGIL
    return [
        box((x0, y0, z0), (x1, y1, z1), {"up": "crown", "*": "casing"}, faces=SIDES + ("up",)),
        box((sx0, sy0, SIGIL_Z), (sx1, sy1, z0), {"north": "sigil"}, faces=("north",), uv={"north": [0, 0, sx1 - sx0, sy1 - sy0]}),
        box((sx0 - 0.25, sy0 - 0.25, SIGIL_Z - 0.1), (sx1 + 0.25, sy0, z0), "trim", faces=("north", "up", "east", "west")),
        box((sx0 - 0.25, sy1, SIGIL_Z - 0.1), (sx1 + 0.25, sy1 + 0.25, z0), "trim", faces=("north", "down", "up", "east", "west")),
        box((sx0 - 0.25, sy0, SIGIL_Z - 0.1), (sx0, sy1, z0), "trim", faces=("north", "east", "west")),
        box((sx1, sy0, SIGIL_Z - 0.1), (sx1 + 0.25, sy1, z0), "trim", faces=("north", "east", "west")),
        box((x0 - 0.25, 11.0, z0 - 0.25), (x1 + 0.25, 11.5, z1 + 0.25), "band", faces=SIDES + ("down",)),
        box(*MOUNT, {"up": "socket", "*": "trim"}, faces=SIDES + ("up",)),
    ]


def socket_stand_elements():
    return [
        box(*SOCKET_BASE, {"up": "socket", "*": "casing"}, faces=SIDES + ("up",)),
        box(*SOCKET_POST, "pipe", faces=SIDES),
        box(*SOCKET_MOUNT, {"up": "socket", "*": "trim"}, faces=SIDES + ("up", "down")),
    ]


def tower_elements():
    mx, my, mz = MATRIX
    top = my + MATRIX_LAYERS[-1][0]
    bottom = my - MATRIX_LAYERS[-1][0]
    (_, _, _), (_, base_top, _) = TOWER_BASE
    (_, arm_bottom, _), _ = TOWER_ARM
    return [
        box(*TOWER_BASE, {"up": "socket", "*": "casing"}, faces=SIDES + ("up",)),
        box(*TOWER_POST, {"up": "crown", "*": "casing"}, faces=SIDES + ("up",)),
        box(*TOWER_ARM, "trim", faces=("north", "east", "west", "up", "down")),
        box((TOWER_POST[0][0] - 0.25, 11.0, TOWER_POST[0][2] - 0.25), (TOWER_POST[1][0] + 0.25, 11.5, TOWER_POST[1][2] + 0.25),
            "band", faces=SIDES + ("down", "up")),
        box((mx - 0.5, base_top, mz - 0.5), (mx + 0.5, bottom - 0.5, mz + 0.5), "collar", faces=SIDES + ("up",)),
        box((mx - 0.5, top + 0.5, mz - 0.5), (mx + 0.5, arm_bottom, mz + 0.5), "collar", faces=SIDES + ("down",)),
    ]


def housing_elements():
    elements = [plinth()]
    elements += pedestal_elements()
    elements += dish_elements()
    elements += wall_elements()
    elements += socket_stand_elements()
    elements += tower_elements()
    return elements


def ring_y(cx, cz, outer, inner, y0, y1, texture):
    """A flat octagonal ring on the y axis: four straight bars plus four diagonal bars turned 45 degrees."""
    half = outer * math.tan(math.radians(22.5))
    depth = outer - inner
    bars = [
        box((cx - half, y0, cz + inner), (cx + half, y1, cz + outer), texture),
        box((cx - half, y0, cz - outer), (cx + half, y1, cz - inner), texture),
        box((cx + inner, y0, cz - half), (cx + outer, y1, cz + half), texture),
        box((cx - outer, y0, cz - half), (cx - inner, y1, cz + half), texture),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sz in (-1, 1):
            x, z = cx + sx * mid, cz + sz * mid
            rotation = {"origin": [r(x), r((y0 + y1) / 2), r(z)], "axis": "y", "angle": 45}
            bars.append(box((x - half, y0 + 0.02, z - depth / 2), (x + half, y1 - 0.02, z + depth / 2), texture, rotation=rotation))
    return bars


def lens_array_elements():
    """The Lens Array: a riser and boom from the wall mount to a hanger over the back of the loupe, a flat copper ring
    with a glass lens level over the target, and a smaller satellite lens on each side tilted toward the target."""
    (_, _, mz0), (_, my1, mz1) = MOUNT
    b0, b1 = BOOM_Y
    l0, l1 = LOUPE_Y
    g0, g1 = GLASS_Y
    back = TARGET_Z + LOUPE_OUTER
    elements = [
        box((7.5, my1, mz0 + 0.75), (8.5, b1, mz0 + 1.75), "copper", faces=SIDES),
        box((7.5, b0, back - 1.0), (8.5, b1, mz0 + 1.75), "copper"),
        box((7.6, l1, back - 0.9), (8.4, b0, back - 0.1), "copper", faces=SIDES),
        box((TARGET_X - LOUPE_INNER, g0, TARGET_Z - LOUPE_INNER), (TARGET_X + LOUPE_INNER, g1, TARGET_Z + LOUPE_INNER),
            {"up": "lens", "down": "lens", "*": "glass_lens"}, uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}),
    ]
    elements += ring_y(TARGET_X, TARGET_Z, LOUPE_OUTER, LOUPE_INNER, l0, l1, "copper")
    for sign in (-1, 1):
        x = TARGET_X + sign * SATELLITE_X
        inner = TARGET_X + sign * LOUPE_OUTER
        elements.append(box((min(inner, x - sign * SATELLITE_OUTER), l0 + 0.2, TARGET_Z - 0.2),
                            (max(inner, x - sign * SATELLITE_OUTER), l1 - 0.2, TARGET_Z + 0.2), "copper", faces=SIDES + ("up", "down")))
        rotation = {"origin": [r(x), r((l0 + l1) / 2), TARGET_Z], "axis": "z", "angle": 22.5 * sign}
        so, si = SATELLITE_OUTER, SATELLITE_INNER
        elements.append(box((x - so, l0, TARGET_Z - so), (x + so, l1, TARGET_Z + so),
                            {"up": "lens", "down": "lens", "*": "copper"}, uv={"up": [0, 0, 16, 16], "down": [0, 0, 16, 16]},
                            rotation=rotation))
    return elements


def modifier_socket_elements():
    """The Modifier Socket: a copper cup on the stand's mount with four claws, turned 45 degrees as on the item."""
    y0, y1 = SOCKET_CUP_Y
    rotation = {"origin": [SOCKET_X, y0, SOCKET_Z], "axis": "y", "angle": 45}
    half = 1.0
    elements = [box((SOCKET_X - half, y0, SOCKET_Z - half), (SOCKET_X + half, y1, SOCKET_Z + half),
                    {"up": "socket_well", "*": "copper"}, rotation=rotation, uv={"up": [0, 0, 16, 16]})]
    claw = 0.2
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        x, z = SOCKET_X + dx * (half + 0.05), SOCKET_Z + dz * (half + 0.05)
        wx, wz = (claw, 0.35) if dx else (0.35, claw)
        elements.append(box((x - wx, y1, z - wz), (x + wx, SOCKET_CLAW_TOP, z + wz), "copper", rotation=rotation))
    return elements


def matrix_elements(glow=False):
    """The Resonance Matrix: an octahedral violet crystal in stepped layers turned 45 degrees, with a bright core at
    its waist. The glow version is a tinted shell over the whole crystal for the renderer."""
    mx, my, mz = MATRIX
    rotation = {"origin": [mx, my, mz], "axis": "y", "angle": 45}
    if glow:
        elements = []
        previous = 0.0
        for reach, half in MATRIX_LAYERS:
            h = half + 0.03
            elements.append(box((mx - h, my - reach - 0.03, mz - h), (mx + h, my + reach + 0.03, mz + h),
                                "glow", rotation=rotation, tint=True))
            previous = reach
        return elements[:2]
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

HOUSING = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "plinth_top": TEX + "plinth_top",
    "plinth_side": TEX + "plinth_side",
    "band": TEX + "band",
    "crown": TEX + "crown",
    "trim": TEX + "trim",
    "anvil": TEX + "anvil",
    "plate": TEX + "plate",
    "pipe": TEX + "pipe",
    "collar": TEX + "collar",
    "socket": TEX + "socket",
    "copper": TEX + "copper",
    "dish": TEX + "dish",
    "redstone": TEX + "redstone",
    "sigil": TEX + "sigil",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    write_json(MODELS / "block/affix_forge.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": HOUSING, "elements": housing_elements(),
    })
    write_json(MODELS / "item/affix_forge.json", {"parent": "rngtech:block/affix_forge"})
    parts = MODELS / "block/affix_forge"
    write_json(parts / "lens_array.json", {
        "render_type": "minecraft:cutout",
        "textures": {"particle": TEX + "copper", "copper": TEX + "copper", "lens": TEX + "lens", "glass_lens": TEX + "glass_lens"},
        "elements": lens_array_elements(),
    })
    write_json(parts / "modifier_socket.json", {
        "render_type": "minecraft:cutout",
        "textures": {"particle": TEX + "copper", "copper": TEX + "copper", "socket_well": TEX + "socket_well"},
        "elements": modifier_socket_elements(),
    })
    write_json(parts / "resonance_matrix.json", {
        "render_type": "minecraft:cutout",
        "textures": {"particle": TEX + "matrix", "matrix": TEX + "matrix", "core": TEX + "matrix_core"},
        "elements": matrix_elements(),
    })
    write_json(parts / "matrix_glow.json", {
        "textures": {"particle": "#glow", "glow": TEX + "fx/core"}, "elements": matrix_elements(glow=True),
    })
    for name in OLD_PARTS:
        (MODELS / f"block/affix_forge_{name}.json").unlink(missing_ok=True)


def write_blockstate() -> None:
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    parts = [("affix_forge", None)] + [(f"affix_forge/{name}", f"has_{name}") for name in OLD_PARTS]
    multipart = []
    for model, flag in parts:
        for facing, angle in rotations.items():
            when = {"facing": facing}
            if flag:
                when[flag] = "true"
            apply = {"model": f"rngtech:block/{model}"}
            if angle:
                apply["y"] = angle
            multipart.append({"when": when, "apply": apply})
    write_json(ASSETS / "blockstates/affix_forge.json", {"multipart": multipart})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"affix_forge:{name}")


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
    """Soft patches, such as tarnish, blended into the base."""
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
IRON = (150, 150, 146)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
COPPER = (198, 112, 64)
COPPER_DARK = (118, 62, 36)
LAPIS = (38, 76, 178)
LAPIS_BRIGHT = (96, 140, 236)
REDSTONE = (170, 18, 12)
GLASS = (196, 236, 240)
VIOLET = (132, 72, 206)
VIOLET_DARK = (44, 22, 72)
VIOLET_BRIGHT = (214, 176, 255)


def ring_mask(size, cx, cy, outer, inner):
    """Pixels whose centres lie in the annulus, as (x, y) pairs."""
    return [(x, y) for y in range(size) for x in range(size)
            if inner <= math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= outer]


def write_housing_textures() -> None:
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")

    crown = noisy(64, shade(SLATE, -8), "crown", amount=4)
    pixels = crown.load()
    for y in range(64):
        for x in range(64):
            if y % 8 == 0:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(crown, "crown")

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

    # Iron, after the iron ingots in the recipe: brushed trim and the anvil plate's flanks.
    save(noisy(64, (176, 176, 170), "trim", amount=6, streak=6), "trim")
    anvil = noisy(64, IRON, "anvil", amount=5, streak=5)
    pixels = anvil.load()
    for y in range(64):
        for x in range(64):
            if y % 4 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
            elif y % 4 == 3:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(anvil, "anvil")

    # Anvil plate top, mapped as a 9 x 9 face (36 x 36 texels): hammered iron with the lapis ring round the target.
    plate = noisy(64, shade(IRON, -26), "plate", amount=7)
    blotches(plate, "plate_hammer", shade(IRON, -30), 10, 4, 0.45)
    pixels = plate.load()
    size = 36
    for x in range(size):
        for y in (0, size - 1):
            pixels[x, y] = (*shade(IRON, 24), 255)
            pixels[y, x] = (*shade(IRON, 24), 255)
    centre = size / 2
    scale = size / 9
    for x, y in ring_mask(size, centre, centre, RING_OUTER * scale, RING_INNER * scale):
        pixels[x, y] = (*shade(LAPIS, rng("ring").uniform(-10, 10)), 255)
    for x, y in ring_mask(size, centre, centre, RING_OUTER * scale + 1, RING_OUTER * scale):
        pixels[x, y] = (*shade(IRON, -44), 255)
    for x, y in ring_mask(size, centre, centre, RING_INNER * scale, RING_INNER * scale - 1):
        pixels[x, y] = (*shade(IRON, -44), 255)
    # Four lapis glyph notches across the ring.
    for angle in range(0, 360, 45):
        a = math.radians(angle)
        for step in range(3):
            radius = (RING_INNER + 0.25) * scale + step
            x, y = int(centre + math.cos(a) * radius), int(centre + math.sin(a) * radius)
            pixels[x, y] = (*LAPIS_BRIGHT, 255)
    save(plate, "plate")

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

    # Copper, after the copper nuggets in the recipe and the Modifier Socket and Lens Array sprites.
    copper = noisy(64, COPPER, "copper", amount=7)
    pixels = copper.load()
    rand = rng("copper_dimples")
    for _ in range(90):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (*shade(pixels[x, y], 24), 255)
        pixels[(x + 1) % 64, (y + 1) % 64] = (*shade(pixels[(x + 1) % 64, (y + 1) % 64], -18), 255)
    blotches(copper, "copper_tarnish", COPPER_DARK, 5, 11, 0.45)
    save(copper, "copper")

    # Dish floor: dark beaten copper with concentric hammer rings.
    dish = noisy(64, shade(COPPER_DARK, -10), "dish", amount=5)
    pixels = dish.load()
    for y in range(64):
        for x in range(64):
            if int(math.hypot(x - 32, (y - 6) * 2.4)) % 6 == 0:
                pixels[x, y] = (*shade(pixels[x, y], 18), 255)
    save(dish, "dish")

    redstone = noisy(64, REDSTONE, "redstone", amount=14)
    pixels = redstone.load()
    rand = rng("redstone_sparks")
    for _ in range(60):
        x, y = rand.randrange(64), rand.randrange(64)
        pixels[x, y] = (255, 70, 50, 255)
    save(redstone, "redstone")

    # Lapis sigil, mapped as a 7 x 7 face (28 x 28 texels): a dark panel with a lapis ring and four spokes.
    sigil = noisy(64, (24, 28, 38), "sigil", amount=3)
    pixels = sigil.load()
    size = 28
    centre = size / 2
    for x, y in ring_mask(size, centre, centre, 11, 9):
        pixels[x, y] = (*shade(LAPIS, rng("sigil_ring").uniform(-8, 8)), 255)
    for x, y in ring_mask(size, centre, centre, 5, 3.5):
        pixels[x, y] = (*shade(LAPIS, 10), 255)
    for i in range(4, 10):
        for x, y in ((centre + i, centre), (centre - i - 1, centre), (centre, centre + i), (centre, centre - i - 1)):
            pixels[int(x), int(y)] = (*LAPIS_BRIGHT, 255)
    for x, y in ((13, 13), (14, 13), (13, 14), (14, 14)):
        pixels[x, y] = (*LAPIS_BRIGHT, 255)
    save(sigil, "sigil")


def write_part_textures() -> None:
    # Loupe lens, mapped whole onto its face: a copper rim round pale glass with a glint.
    lens = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(lens)
    draw.rectangle([0, 0, 63, 63], fill=(*COPPER_DARK, 255))
    draw.ellipse([4, 4, 59, 59], fill=(*shade(COPPER, -20), 255))
    draw.ellipse([9, 9, 54, 54], fill=(*shade(GLASS, -40), 255))
    draw.ellipse([13, 13, 50, 50], fill=(*GLASS, 255))
    draw.ellipse([18, 16, 30, 26], fill=(250, 255, 255, 255))
    save(lens, "lens")

    # Glass dome under the loupe: pale cyan with white glints, as on the Lens Array sprite.
    dome = noisy(64, GLASS, "glass_lens", amount=6)
    pixels = dome.load()
    for y in range(64):
        for x in range(64):
            if (x + y) % 16 in (0, 1):
                pixels[x, y] = (250, 255, 255, 255)
    save(dome, "glass_lens")

    # Socket well, mapped whole onto the cup's top: a copper ring round a dark well, as on the Modifier Socket sprite.
    well = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(well)
    draw.rectangle([0, 0, 63, 63], fill=(*COPPER, 255))
    draw.rectangle([0, 0, 63, 63], outline=(*COPPER_DARK, 255), width=4)
    draw.rectangle([18, 18, 45, 45], fill=(*COPPER_DARK, 255))
    draw.rectangle([22, 22, 41, 41], fill=(24, 14, 10, 255))
    save(well, "socket_well")

    # Matrix crystal facets: diagonal bands of light and shade over violet, after the Resonance Matrix sprite.
    crystal = noisy(64, VIOLET, "matrix", amount=5)
    pixels = crystal.load()
    for y in range(64):
        for x in range(64):
            band = (x + y) % 16
            if band < 2:
                pixels[x, y] = (*shade(pixels[x, y], 44), 255)
            elif band > 11:
                pixels[x, y] = (*mix(pixels[x, y], VIOLET_DARK, 0.55), 255)
    save(crystal, "matrix")

    core = noisy(64, shade(VIOLET, 30), "matrix_core", amount=5)
    pixels = core.load()
    for y in range(64):
        for x in range(64):
            if y % 16 in (7, 8):
                pixels[x, y] = (*VIOLET_BRIGHT, 255)
    save(core, "matrix_core")


def write_fx_textures() -> None:
    """Renderer textures: the matrix glow and the lapis ring flare, drawn additively. The loupe beam reuses the
    Resonance Calibrator's emitter beam."""
    core = Image.new("RGBA", (16, 16))
    pixels = core.load()
    for y in range(16):
        for x in range(16):
            value = clamp(255 * (0.4 + 0.6 * (1.0 - abs(y + 0.5 - 8) / 8)))
            pixels[x, y] = (value, value, value, 255)
    save(core, "fx/core")

    # Ring flare: the inlay's annulus, bright in the middle of the band and fading to its edges, on black.
    ring = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    pixels = ring.load()
    outer, inner = RING_OUTER / RING_OUTER * 30, RING_INNER / RING_OUTER * 30
    middle, half = (outer + inner) / 2, (outer - inner) / 2 + 3
    for y in range(64):
        for x in range(64):
            d = math.hypot(x + 0.5 - 32, y + 0.5 - 32)
            t = max(0.0, 1.0 - abs(d - middle) / half)
            value = clamp(255 * t ** 1.4)
            pixels[x, y] = (value, value, value, 255)
    save(ring, "fx/ring")


def remove_old_textures() -> None:
    (ASSETS / "textures/block/affix_forge.png").unlink(missing_ok=True)
    for name in OLD_TEXTURES:
        (ASSETS / f"textures/block/affix_forge_{name}.png").unlink(missing_ok=True)


def main() -> None:
    write_models()
    write_blockstate()
    write_housing_textures()
    write_part_textures()
    write_fx_textures()
    remove_old_textures()


if __name__ == "__main__":
    main()
