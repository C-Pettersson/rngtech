#!/usr/bin/env python3
"""Generate the Tool Bench block models, item model, and owned textures.

The Tool Bench is a Stage 0 block built from oak slabs, a crafting table, and gravel, so it is a
plain wooden table in the Stage 0 wood language: a thick oak top with a crafting grid in its
boards, four dark oak legs, and a bed of gravel between the legs for the recipe's bottom row. The
detail is in what lies on it: ToolBenchRenderer lays the tool, or the loose head and rod, on the
top. Installed Gear shows through multipart blockstate parts: a Tiny Anvil on the front-west
corner, and an Energy Connector as a plank back panel with the cable port, a copper-ringed socket.
Run from the repository root:

    python tools/blockmodels/tool_bench.py
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
TEXTURES = ASSETS / "textures/block/tool_bench"
TEX = "rngtech:block/tool_bench/"
FACES = ("north", "south", "east", "west", "up", "down")

# Worktop: the front faces north (z = 0). ToolBenchRenderer lays items on its top.
WORKTOP = ((0.0, 12.0, 0.0), (16.0, 16.0, 16.0))
# Legs at the corners, three pixels square.
LEG_SIZE = 3.0
LEG_POSITIONS = ((0.5, 0.5), (12.5, 0.5), (0.5, 12.5), (12.5, 12.5))
# Gravel bed low between the legs, for the recipe's bottom row of gravel: a board tray filled to its rim.
GRAVEL_BED = ((1.0, 1.5, 1.0), (15.0, 3.5, 15.0))
# Back panel between the back legs; the cable port plate sits on its centre, sized to the Universal
# Connector ring (4 to 12 on the face).
BACK_PANEL = ((3.5, 2.0, 14.0), (12.5, 12.0, 15.0))
PORT = ((4.0, 4.0, 15.0), (12.0, 12.0, 16.0))


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


def housing_elements():
    elements = [box(*WORKTOP, {"up": "worktop", "down": "underside", "*": "worktop_side"})]
    for x, z in LEG_POSITIONS:
        elements.append(box((x, 0, z), (x + LEG_SIZE, WORKTOP[0][1], z + LEG_SIZE), {"down": "leg_end", "*": "leg"},
                            faces=("north", "south", "east", "west", "down")))
    elements.append(box(*GRAVEL_BED, {"up": "gravel", "down": "underside", "*": "worktop_side"}))
    return elements


def port_elements():
    """Shown only with an Energy Connector installed: the back panel and the cable port plate on it."""
    return [
        box(*BACK_PANEL, "back", faces=("north", "south")),
        box(*PORT, {"south": "port", "*": "leg"}, faces=("south", "east", "west", "down"), uv={"south": [0, 0, 16, 16]}),
    ]


# Tiny Anvil, in anvil units around the centre of its face, from the floor up: a stepped foot,
# a waist, the body with a polished face, a tapered horn toward +x, and a heel toward -x. The item
# draws it large and centred; the bench draws it small on the front-west corner of the worktop,
# its horn pointing at the tool.
ANVIL_TEX = TEX + "anvil_"
ANVIL_SHAPE = (
    ((-3.0, 0.0, -2.0), (3.0, 1.0, 2.0), {"up": "iron", "*": "iron"}),
    ((-2.25, 1.0, -1.5), (2.25, 1.75, 1.5), {"*": "iron"}),
    ((-1.25, 1.75, -1.0), (1.25, 3.0, 1.0), {"*": "iron"}),
    ((-3.5, 3.0, -1.5), (3.5, 4.5, 1.5), {"up": "face", "*": "iron"}),
    ((3.5, 3.5, -1.0), (5.0, 4.5, 1.0), {"up": "horn", "*": "iron"}),
    ((5.0, 3.75, -0.5), (6.0, 4.375, 0.5), {"up": "horn", "*": "iron"}),
    ((-4.5, 3.5, -1.25), (-3.5, 4.5, 1.25), {"up": "horn", "*": "iron"}),
)
ANVIL_ITEM = (1.6, (8.0 - 1.6 * 0.75, 2.0, 8.0))
ANVIL_BENCH = (0.55, (3.75, WORKTOP[1][1], 3.0))


def anvil_elements(scale, origin):
    """The Tiny Anvil scaled by `scale` model pixels per anvil unit, with its face centre's foot at `origin`."""
    ox, oy, oz = origin
    elements = []
    for start, end, faces in ANVIL_SHAPE:
        lo = (ox + start[0] * scale, oy + start[1] * scale, oz + start[2] * scale)
        hi = (ox + end[0] * scale, oy + end[1] * scale, oz + end[2] * scale)
        elements.append(box(lo, hi, faces, uv=face_uvs(lo, hi, full_up=faces.get("up") == "face")))
    return elements


def face_uvs(lo, hi, full_up=False):
    """Explicit UVs sized to each face. Without them, faces above y 16 would sample outside their sprite."""
    dx, dy, dz = (r(hi[i] - lo[i]) for i in range(3))
    uvs = {
        "north": [0, 0, dx, dy], "south": [0, 0, dx, dy],
        "east": [0, 0, dz, dy], "west": [0, 0, dz, dy],
        "up": [0, 0, dx, dz], "down": [0, 0, dx, dz],
    }
    if full_up:
        uvs["up"] = [0, 0, 16, 16]
    return uvs


ANVIL_TEXTURES = {"particle": ANVIL_TEX + "iron", "iron": ANVIL_TEX + "iron", "face": ANVIL_TEX + "face", "horn": ANVIL_TEX + "horn"}


TEXTURE_KEYS = ("worktop", "worktop_side", "underside", "leg", "leg_end", "gravel", "back", "port")


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def write_models() -> None:
    textures = {key: TEX + key for key in TEXTURE_KEYS}
    textures["particle"] = TEX + "worktop"
    write_json(MODELS / "block/tool_bench.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": textures,
        "elements": housing_elements(),
    })
    stale = MODELS / "block/tool_bench_with_anvil.json"
    if stale.exists():
        stale.unlink()
    parts = MODELS / "block/tool_bench"
    write_json(parts / "tiny_anvil.json", {
        "render_type": "minecraft:cutout",
        "textures": ANVIL_TEXTURES,
        "elements": anvil_elements(*ANVIL_BENCH),
    })
    write_json(MODELS / "item/tiny_anvil.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": ANVIL_TEXTURES,
        "elements": anvil_elements(*ANVIL_ITEM),
    })
    write_json(parts / "energy_port.json", {
        "render_type": "minecraft:cutout",
        "textures": {"particle": TEX + "back", "back": TEX + "back", "port": TEX + "port", "leg": TEX + "leg"},
        "elements": port_elements(),
    })
    write_json(MODELS / "item/tool_bench.json", {"parent": "rngtech:block/tool_bench"})

    multipart = []
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for model, condition in (
                ("rngtech:block/tool_bench", {}),
                ("rngtech:block/tool_bench/tiny_anvil", {"has_tiny_anvil": "true"}),
                ("rngtech:block/tool_bench/energy_port", {"has_energy_connector": "true"}),
        ):
            apply = {"model": model}
            if rotation:
                apply["y"] = rotation
            multipart.append({"when": {"facing": facing, **condition}, "apply": apply})
    write_json(ASSETS / "blockstates/tool_bench.json", {"multipart": multipart})


# ---------------------------------------------------------------- textures

def rng(name: str) -> random.Random:
    return random.Random(f"tool_bench:{name}")


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


def save(image, name):
    path = TEXTURES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


# The Stage 0 wood palette: warm oak boards, a dark oak frame, and copper nail heads.
OAK = (164, 114, 64)
OAK_DARK = (108, 70, 38)
FRAME = (86, 54, 30)
SEAM = (52, 32, 18)
COPPER = (196, 116, 56)
COPPER_DARK = (120, 64, 30)
IRON = (58, 60, 66)
POLISHED = (156, 162, 170)


def boards(name, color, width, vertical=False, nails=True):
    """Planks `width` texels wide with dark seams, per-board tone, wavy grain, and copper nails at the board ends."""
    image = noisy(64, color, name, amount=6)
    pixels = image.load()
    rand = rng(name + "_boards")
    tones = [rand.uniform(-14, 12) for _ in range(64 // width + 1)]
    for y in range(64):
        for x in range(64):
            across, along = (x, y) if vertical else (y, x)
            board = across // width
            grain = math.sin(along * 0.35 + board * 2.1 + math.sin(across * 1.3) * 1.2) * 5
            value = shade(pixels[x, y], tones[board] + grain)
            if across % width == 0:
                value = SEAM
            elif across % width == 1:
                value = shade(value, 12)
            pixels[x, y] = (*value, 255)
    if nails:
        for board in range(64 // width):
            for along in (3, 60):
                across = board * width + width // 2
                x, y = (across, along) if vertical else (along, across)
                pixels[x, y] = (*COPPER, 255)
                pixels[x + 1, y + 1] = (*COPPER_DARK, 255)
    return image


def write_textures() -> None:
    # Top: oak slab boards with a crafting-table grid in the middle, where the tool lies.
    top = boards("worktop", OAK, 16)
    draw = ImageDraw.Draw(top)
    draw.rectangle([12, 12, 51, 51], outline=(*SEAM, 255), width=2)
    draw.rectangle([14, 14, 49, 49], outline=(*shade(OAK, 18), 255))
    for i in (25, 38):
        draw.line([(i, 14), (i, 49)], fill=(*OAK_DARK, 255), width=2)
        draw.line([(14, i), (49, i)], fill=(*OAK_DARK, 255), width=2)
    save(top, "worktop")

    # Edges of the top: two stacked slabs, each two pixels thick.
    save(boards("worktop_side", OAK_DARK, 8, nails=False), "worktop_side")
    save(boards("underside", FRAME, 16, nails=False), "underside")

    leg = noisy(64, FRAME, "leg", amount=5)
    pixels = leg.load()
    for y in range(64):
        for x in range(64):
            grain = math.sin(x * 0.9 + math.sin(y * 0.15) * 2.0) * 7
            pixels[x, y] = (*shade(pixels[x, y], grain), 255)
    save(leg, "leg")

    leg_end = noisy(64, OAK_DARK, "leg_end", amount=5)
    pixels = leg_end.load()
    for y in range(64):
        for x in range(64):
            if math.hypot((x % 12) - 5.5, (y % 12) - 5.5) % 2.5 < 0.7:
                pixels[x, y] = (*shade(pixels[x, y], -16), 255)
    save(leg_end, "leg_end")

    save(boards("back", (142, 98, 54), 16, vertical=True), "back")

    # Gravel: grey stones in a few tones, with the odd dark flint among them.
    gravel = noisy(64, (128, 122, 118), "gravel", amount=10)
    pixels = gravel.load()
    rand = rng("gravel_stones")
    for _ in range(140):
        x, y = rand.randrange(63), rand.randrange(63)
        tone = rand.choice(((96, 90, 86), (160, 154, 148), (112, 104, 98)))
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if rand.random() < 0.8:
                pixels[x + dx, y + dy] = (*shade(tone, rand.uniform(-6, 6)), 255)
    for _ in range(10):
        x, y = rand.randrange(63), rand.randrange(63)
        pixels[x, y] = (40, 38, 42, 255)
        pixels[x + 1, y] = (58, 56, 62, 255)
    save(gravel, "gravel")

    write_anvil_textures()

    # Cable port: a dark oak plate with a copper ring around a socket, mapped whole onto the port plate.
    port = noisy(64, FRAME, "port", amount=5)
    draw = ImageDraw.Draw(port)
    draw.rectangle([0, 0, 63, 63], outline=(*SEAM, 255), width=2)
    draw.ellipse([10, 10, 53, 53], outline=(*COPPER_DARK, 255), width=4)
    draw.ellipse([12, 12, 51, 51], outline=(*COPPER, 255), width=2)
    draw.ellipse([22, 22, 41, 41], fill=(28, 18, 12, 255))
    for x, y in ((4, 4), (58, 4), (4, 58), (58, 58)):
        draw.rectangle([x, y, x + 1, y + 1], fill=(*COPPER, 255))
    save(port, "port")



def write_anvil_textures() -> None:
    """Wrought iron with hammer marks, a polished face, and a polished horn."""
    iron = noisy(64, IRON, "anvil_iron", amount=5)
    pixels = iron.load()
    rand = rng("anvil_hammer_marks")
    for _ in range(60):
        x, y = rand.randrange(63), rand.randrange(63)
        pixels[x, y] = (*shade(IRON, 16), 255)
        pixels[x + 1, y + 1] = (*shade(IRON, -14), 255)
    save(iron, "anvil_iron")

    face = noisy(64, POLISHED, "anvil_face", amount=4, streak=5)
    draw = ImageDraw.Draw(face)
    draw.rectangle([0, 0, 63, 63], outline=(*shade(IRON, 10), 255), width=3)
    draw.line([(3, 3), (60, 3)], fill=(*shade(POLISHED, 26), 255))
    for x in range(26, 58, 7):
        draw.line([(x, 12), (x + 4, 12)], fill=(*shade(POLISHED, -22), 255))
    save(face, "anvil_face")

    horn = noisy(64, shade(POLISHED, -18), "anvil_horn", amount=5, streak=8)
    save(horn, "anvil_horn")


def main() -> None:
    write_models()
    for stale in TEXTURES.glob("*.png"):
        stale.unlink()
    write_textures()


if __name__ == "__main__":
    main()
