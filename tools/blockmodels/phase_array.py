"""
Generates the Phase Array models and textures: the Phase Coil, the Phase Emitter, and the Phase Cradle.

All three parts are caged cubes so a line reads as one assembly: a steel frame of twelve edge bars round slate panels,
with lit strips set along the inside of every visible face that brighten as the line forms and go white while a pulse
runs. A Coil carries a recessed glyph disc on each side of the line that shows the inscribed tier. The Emitter is the
same cage round a glowing core, seen through square openings in its sides and top, that fills as the charge rises; its
lens faces down the line. The Cradle is the same cage in brass with a mirror-lined bore opening toward the Coils to
catch the pulse. The beam between parts is a particle effect, never geometry.

Models follow docs/reference/machine-visual-design.md: 64x64 textures mapped by element position (four texels per
model pixel), slate casing, steel pipework, and one lit colour per part: teal Coils, blue Emitter, amber Cradle. Run
from the repository root:

    python tools/blockmodels/phase_array.py
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
BLOCKSTATES = ASSETS / "blockstates"
TEXTURES = ASSETS / "textures/block/phase_array"
TEX = "rngtech:block/phase_array/"

TAN = math.tan(math.radians(22.5))
STAGGER = 0.03
FACES = ("north", "south", "east", "west", "up", "down")
TIERS = 5
CHARGE_LEVELS = 4
NUMERALS = {1: "I", 2: "II", 3: "III", 4: "IV", 5: "V"}

# The line runs along z in model space: the Emitter's lens and the Cradle's bore face north.
FRAME = 1.5
STRIP = 0.75
STRIP_PROUD = 0.5
OPENING = (4.0, 12.0)
CORE = (3.5, 12.5)
GLOW = {"shade": False}

# Each face's normal axis, its outward sign, and the two axes that span it.
FACE_AXES = {
    "east": ("x", 1, ("y", "z")),
    "west": ("x", -1, ("y", "z")),
    "up": ("y", 1, ("x", "z")),
    "down": ("y", -1, ("x", "z")),
    "south": ("z", 1, ("x", "y")),
    "north": ("z", -1, ("x", "y")),
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


def axis_box(ranges, textures, **kw):
    """A box given as {axis: (lo, hi)}."""
    return box((ranges["x"][0], ranges["y"][0], ranges["z"][0]), (ranges["x"][1], ranges["y"][1], ranges["z"][1]), textures, **kw)


def ring_z(cx, cy, outer, inner, z0, z1, textures):
    """A flat octagonal ring on the z axis: four straight bars plus four diagonal bars."""
    half = outer * TAN
    depth = outer - inner
    bars = [
        box((cx - half, cy + inner, z0), (cx + half, cy + outer, z1), textures),
        box((cx - half, cy - outer, z0), (cx + half, cy - inner, z1), textures),
        box((cx + inner, cy - half, z0), (cx + outer, cy + half, z1), textures),
        box((cx - outer, cy - half, z0), (cx - inner, cy + half, z1), textures),
    ]
    mid = (outer + inner) / 2 / math.sqrt(2)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = cx + sx * mid, cy + sy * mid
            if sx * sy > 0:
                start, end = (x - half, y - depth / 2, z0), (x + half, y + depth / 2, z1)
            else:
                start, end = (x - depth / 2, y - half, z0), (x + depth / 2, y + half, z1)
            start, end = (start[0], start[1], z0 + STAGGER), (end[0], end[1], z1 - STAGGER)
            bars.append(box(start, end, textures, rotation={"origin": [r(x), r(y), r((z0 + z1) / 2)], "axis": "z", "angle": -45}))
    return bars


# ---- The cage every part shares -------------------------------------------------------------------------------------


def frame_elements(key="frame"):
    """Twelve edge bars round the block."""
    elements = []
    lo, hi = (0.0, FRAME), (16.0 - FRAME, 16.0)
    full = (0.0, 16.0)
    for a in (lo, hi):
        for b in (lo, hi):
            elements.append(axis_box({"x": full, "y": a, "z": b}, key))
            elements.append(axis_box({"x": a, "y": full, "z": b}, key))
            elements.append(axis_box({"x": a, "y": b, "z": full}, key))
    return elements


def depth_range(face, inner, outer):
    """A range along a face's normal, measured inward from the block surface: `outer` is nearest the surface."""
    _, sign, _ = FACE_AXES[face]
    if sign > 0:
        return (16.0 - inner, 16.0 - outer)
    return (outer, inner)


def panel(face, textures, lo=1.0, hi=15.0):
    """A solid one-pixel panel just inside the frame on one face."""
    axis, _, (a, b) = FACE_AXES[face]
    return axis_box({axis: depth_range(face, 2.0, 1.0), a: (lo, hi), b: (lo, hi)}, textures)


def ring_panel(face, key, opening=OPENING, interior="interior"):
    """A one-pixel panel with a square opening; the opening's walls show the interior texture."""
    axis, _, (a, b) = FACE_AXES[face]
    depth = depth_range(face, 2.0, 1.0)
    textures = {face: key, "*": interior}
    o0, o1 = opening
    return [
        axis_box({axis: depth, a: (1.0, o0), b: (1.0, 15.0)}, textures),
        axis_box({axis: depth, a: (o1, 15.0), b: (1.0, 15.0)}, textures),
        axis_box({axis: depth, a: (o0, o1), b: (1.0, o0)}, textures),
        axis_box({axis: depth, a: (o0, o1), b: (o1, 15.0)}, textures),
    ]


def recess_floor(face, key, opening=OPENING, depth=2.0, thickness=0.5):
    """The back wall of a recess behind a ring panel's opening."""
    axis, _, (a, b) = FACE_AXES[face]
    return axis_box({axis: depth_range(face, depth + thickness, depth), a: opening, b: opening}, {face: key, "*": "interior"}, extra=GLOW)


def strip_elements(face, key="strip"):
    """Four lit strips along the inside of the frame on one face, proud of the panel."""
    axis, _, (a, b) = FACE_AXES[face]
    depth = depth_range(face, 1.0, 1.0 - STRIP_PROUD)
    m = FRAME
    inner = (m + STRIP, 16.0 - m - STRIP)
    full = (m, 16.0 - m)
    return [
        axis_box({axis: depth, a: (m, m + STRIP), b: full}, key, extra=GLOW),
        axis_box({axis: depth, a: (16.0 - m - STRIP, 16.0 - m), b: full}, key, extra=GLOW),
        axis_box({axis: depth, a: inner, b: (m, m + STRIP)}, key, extra=GLOW),
        axis_box({axis: depth, a: inner, b: (16.0 - m - STRIP, 16.0 - m)}, key, extra=GLOW),
    ]


def port_face(face):
    """The cable port: a flush door plate on the face centre, sized to the Universal Connector ring."""
    return panel(face, {face: "port", "*": "casing"})


# ---- The Coil --------------------------------------------------------------------------------------------------------


def coil_elements():
    """The cage round slate panels, a recess on each side of the line for the glyph disc, and a pane on the crown."""
    elements = frame_elements()
    elements += ring_panel("east", "casing")
    elements += ring_panel("west", "casing")
    elements.append(panel("up", {"up": "casing", "*": "interior"}))
    elements.append(panel("down", {"down": "casing", "*": "interior"}))
    elements.append(panel("north", {"north": "aperture", "*": "interior"}))
    elements.append(panel("south", {"south": "aperture", "*": "interior"}))
    elements.append(box((5.0, 15.0, 5.0), (11.0, 15.5, 11.0), "pane", extra=GLOW))
    for face in ("east", "west", "up"):
        elements += strip_elements(face)
    return elements


def plate_elements():
    """The glyph discs at the back of both side recesses; one model per tier swaps the texture."""
    return [recess_floor("east", "plate"), recess_floor("west", "plate")]


# ---- The Emitter ---------------------------------------------------------------------------------------------------------


def emitter_elements():
    """The cage round a glowing core seen through openings in the sides and crown; the lens faces the line."""
    elements = frame_elements()
    for face in ("east", "west", "up"):
        elements += ring_panel(face, "casing")
    elements.append(panel("down", {"down": "casing", "*": "interior"}))
    elements.append(panel("north", {"north": "casing", "*": "interior"}))
    elements.append(port_face("south"))
    elements.append(box((CORE[0], CORE[0], CORE[0]), (CORE[1], CORE[1], CORE[1]), "core", extra=GLOW))
    elements.append(box((OPENING[0], OPENING[0], 0.6), (OPENING[1], OPENING[1], 1.0), {"north": "lens", "*": "frame"}, extra=GLOW))
    for face in ("east", "west", "up"):
        elements += strip_elements(face)
    return elements


# ---- The Cradle ----------------------------------------------------------------------------------------------------------


BORE_BACK = 10.5


def cradle_elements():
    """
    The brass cage with a mirror-lined bore opening toward the Coils, the item's seat at its back, and a brass collar
    round the mouth; amber halos sit in the side recesses and an amber pane under a brass cap on the crown.
    """
    elements = frame_elements("frame")
    elements += ring_panel("north", "casing")
    for face in ("east", "west"):
        elements += ring_panel(face, "casing")
        elements.append(recess_floor(face, "halo"))
    elements.append(panel("up", {"up": "casing", "*": "interior"}))
    elements.append(panel("down", {"down": "casing", "*": "interior"}))
    elements.append(port_face("south"))
    # The bore: four mirror walls from the mouth to the seat.
    o0, o1 = OPENING
    c0, c1 = CORE
    elements.append(box((c0, c0, 2.0), (o0, c1, BORE_BACK), {"east": "mirror", "*": "interior"}))
    elements.append(box((o1, c0, 2.0), (c1, c1, BORE_BACK), {"west": "mirror", "*": "interior"}))
    elements.append(box((o0, c0, 2.0), (o1, o0, BORE_BACK), {"up": "mirror", "*": "interior"}))
    elements.append(box((o0, o1, 2.0), (o1, c1, BORE_BACK), {"down": "mirror", "*": "interior"}))
    elements.append(box((o0, o0, BORE_BACK), (o1, o1, BORE_BACK + 0.5), {"north": "seat", "*": "interior"}, extra=GLOW))
    elements += ring_z(8.0, 8.0, 5.6, 4.4, 0.25, 1.0, "frame")
    # The crown: a brass cap with the amber pane.
    elements.append(box((5.0, 15.0, 5.0), (11.0, 15.75, 11.0), "frame"))
    elements.append(box((5.5, 15.75, 5.5), (10.5, 16.0, 10.5), "pane", extra=GLOW))
    for face in ("east", "west", "up", "north"):
        elements += strip_elements(face)
    return elements


# ---- Models and blockstates ---------------------------------------------------------------------------------------------

COIL_TEXTURES = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "interior": TEX + "interior",
    "frame": TEX + "frame_steel",
    "aperture": TEX + "aperture",
    "pane": TEX + "pane_off",
    "strip": TEX + "strip_off",
}
EMITTER_TEXTURES = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "interior": TEX + "interior",
    "frame": TEX + "frame_steel",
    "core": TEX + "core_0",
    "lens": TEX + "lens_off",
    "strip": TEX + "estrip_off",
    "port": TEX + "port",
}
CRADLE_TEXTURES = {
    "particle": TEX + "casing",
    "casing": TEX + "casing",
    "interior": TEX + "interior",
    "frame": TEX + "frame_brass",
    "mirror": TEX + "mirror",
    "seat": TEX + "seat",
    "halo": TEX + "halo",
    "pane": TEX + "pane_amber",
    "strip": TEX + "strip_amber",
    "port": TEX + "port",
}


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def emitter_model(formed: bool, charge: int) -> str:
    name = "phase_emitter"
    if formed:
        name += "_formed"
    if charge:
        name += f"_c{charge}"
    return name


def write_models() -> None:
    write_json(MODELS / "block/phase_coil.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": COIL_TEXTURES,
        "elements": coil_elements(),
    })
    write_json(MODELS / "block/phase_coil_formed.json", {
        "parent": "rngtech:block/phase_coil",
        "textures": {"pane": TEX + "pane_formed", "strip": TEX + "strip_formed"},
    })
    write_json(MODELS / "block/phase_coil_active.json", {
        "parent": "rngtech:block/phase_coil",
        "textures": {"pane": TEX + "pane_active", "strip": TEX + "strip_active"},
    })
    for tier in range(TIERS + 1):
        write_json(MODELS / f"block/phase_coil_plate_{tier}.json", {
            "render_type": "minecraft:cutout",
            "textures": {"particle": TEX + "casing", "plate": TEX + f"plate_{tier}", "interior": TEX + "interior"},
            "elements": plate_elements(),
        })
    write_json(MODELS / "block/phase_emitter.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": EMITTER_TEXTURES,
        "elements": emitter_elements(),
    })
    for formed in (False, True):
        for charge in range(CHARGE_LEVELS + 1):
            if not formed and charge == 0:
                continue
            textures = {"core": TEX + f"core_{charge}"}
            if formed:
                textures.update({"lens": TEX + "lens_formed", "strip": TEX + "estrip_formed"})
            write_json(MODELS / f"block/{emitter_model(formed, charge)}.json", {"parent": "rngtech:block/phase_emitter", "textures": textures})
    write_json(MODELS / "block/phase_emitter_active.json", {
        "parent": "rngtech:block/phase_emitter",
        "textures": {"core": TEX + "core_active", "lens": TEX + "lens_active", "strip": TEX + "estrip_active"},
    })
    write_json(MODELS / "block/exotic_affix_forge.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": CRADLE_TEXTURES,
        "elements": cradle_elements(),
    })
    for name in ("phase_coil", "phase_emitter", "exotic_affix_forge"):
        write_json(MODELS / f"item/{name}.json", {"parent": f"rngtech:block/{name}"})


FACINGS = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def apply(model, y):
    entry = {"model": model}
    if y:
        entry["y"] = y
    return entry


def write_blockstates() -> None:
    parts = []
    for active in ("false", "true"):
        for formed in ("false", "true"):
            model = "rngtech:block/phase_coil" + ("_active" if active == "true" else "_formed" if formed == "true" else "")
            for facing, y in FACINGS:
                parts.append({"when": {"active": active, "formed": formed, "facing": facing}, "apply": apply(model, y)})
    for tier in range(TIERS + 1):
        for facing, y in FACINGS:
            parts.append({"when": {"tier": str(tier), "facing": facing}, "apply": apply(f"rngtech:block/phase_coil_plate_{tier}", y)})
    write_json(BLOCKSTATES / "phase_coil.json", {"multipart": parts})
    variants = {}
    for active in ("false", "true"):
        for formed in ("false", "true"):
            for charge in range(CHARGE_LEVELS + 1):
                model = "rngtech:block/" + ("phase_emitter_active" if active == "true" else emitter_model(formed == "true", charge))
                for facing, y in FACINGS:
                    variants[f"active={active},charge={charge},facing={facing},formed={formed}"] = apply(model, y)
    write_json(BLOCKSTATES / "phase_emitter.json", {"variants": variants})
    write_json(BLOCKSTATES / "exotic_affix_forge.json", {
        "variants": {f"facing={facing}": apply("rngtech:block/exotic_affix_forge", y) for facing, y in FACINGS},
    })


# ---- Textures ----------------------------------------------------------------------------------------------------------


def rng(name: str) -> random.Random:
    return random.Random(f"phase_array:{name}")


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def shade(color, amount):
    return tuple(clamp(c + amount) for c in color[:3])


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noisy(size, color, name, amount=6, streak=0, vertical=False):
    rand = rng(name)
    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    lines = [rand.uniform(-streak, streak) for _ in range(size)]
    for y in range(size):
        for x in range(size):
            pixels[x, y] = (*shade(color, rand.uniform(-amount, amount) + lines[x if vertical else y]), 255)
    return image


def panel_seams(image, step=16, rivets=True, horizontal=True, vertical=True):
    pixels = image.load()
    size = image.size[0]
    for y in range(size):
        for x in range(size):
            if (vertical and x % step == 0) or (horizontal and y % step == 0):
                pixels[x, y] = (*shade(pixels[x, y], -22), 255)
            elif (vertical and x % step == 1) or (horizontal and y % step == 1):
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
    TEXTURES.mkdir(parents=True, exist_ok=True)
    image.save(TEXTURES / f"{name}.png")


SLATE = (46, 54, 66)
SLATE_DARK = (30, 35, 43)
STEEL = (98, 108, 120)
STEEL_LIGHT = (128, 138, 150)
BRASS = (172, 132, 70)
BRASS_LIGHT = (214, 176, 104)
TEAL = (47, 179, 194)
TEAL_DARK = (22, 92, 104)
TEAL_BRIGHT = (142, 236, 246)
BLUE = (64, 150, 255)
BLUE_DARK = (26, 62, 120)
BLUE_BRIGHT = (168, 214, 255)
AMBER = (236, 168, 56)
AMBER_DARK = (120, 78, 24)
AMBER_BRIGHT = (255, 224, 150)
WHITE_HOT = (236, 253, 255)
COPPER = (184, 115, 51)
GLASS = (18, 24, 28)


def frame(color, name):
    """An edge bar: brushed metal with a bevel highlight on one edge and a rivet pair every sixteen texels."""
    image = noisy(64, color, name, amount=5, streak=3, vertical=True)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            if x < 2 or y < 2:
                pixels[x, y] = (*shade(pixels[x, y], 28), 255)
            elif x > 61 or y > 61:
                pixels[x, y] = (*shade(pixels[x, y], -30), 255)
            if x % 16 == 7 and y % 16 == 7:
                for dx in range(3):
                    for dy in range(3):
                        pixels[x + dx, y + dy] = (*shade(color, -36), 255)
                pixels[x, y] = (*shade(color, 40), 255)
    save(image, name)


def strip(color, glow, name):
    """A lit strip: a glowing bar in short segments with a bright centre line."""
    image = noisy(64, shade(color, -40), name, amount=3)
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            segment = (x % 16) not in (0, 1) and (y % 16) not in (0, 1)
            if segment:
                pixels[x, y] = (*color, 255)
            if segment and 28 <= (y % 64) < 36 and 28 <= (x % 64) < 36:
                pixels[x, y] = (*glow, 255)
    save(image, name)


def pane(color, glow, name):
    """The crown pane: a square window lit from a bright centre out to a dim rim."""
    image = noisy(64, SLATE_DARK, name, amount=3)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 63, 63], outline=(*STEEL, 255), width=3)
    for step in range(9):
        inset = 4 + step * 3
        draw.rectangle([inset, inset, 63 - inset, 63 - inset], fill=(*mix(shade(color, -50), glow, step / 8), 255))
    save(image, name)


def core(color, glow, name, fill=1.0):
    """The Emitter's core face: a lit square that fills from the bottom up as the charge rises."""
    image = noisy(64, shade(color, -70), name, amount=4)
    pixels = image.load()
    top = int(round(64 * (1.0 - fill)))
    for y in range(64):
        for x in range(64):
            if y >= top:
                t = 1.0 - abs(x - 31.5) / 32
                pixels[x, y] = (*mix(color, glow, t * t), 255)
            if (x % 8 == 0 or y % 8 == 0) and y >= top:
                pixels[x, y] = (*shade(pixels[x, y], -24), 255)
    save(image, name)


def lens(color, glow, name):
    """The Emitter's aperture: concentric rings around a lit core."""
    image = noisy(64, SLATE_DARK, name, amount=3)
    draw = ImageDraw.Draw(image)
    draw.ellipse([2, 2, 61, 61], fill=(*STEEL, 255))
    draw.ellipse([8, 8, 55, 55], fill=(*BLUE_DARK, 255))
    draw.ellipse([16, 16, 47, 47], fill=(*color, 255))
    draw.ellipse([25, 25, 38, 38], fill=(*glow, 255))
    for p in ((6, 31), (57, 31), (31, 6), (31, 57)):
        draw.rectangle([p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1], fill=(*COPPER, 255))
    save(image, name)


def numeral(draw, tier, color):
    """A Roman numeral drawn inside the disc, strokes six texels wide."""
    glyph = NUMERALS[tier]
    width = {"I": 6, "II": 18, "III": 30, "IV": 26, "V": 14}[glyph]
    x = 32 - width // 2
    top, bottom = 18, 46
    for ch in glyph:
        if ch == "I":
            draw.rectangle([x, top, x + 5, bottom], fill=color)
            draw.rectangle([x - 2, top, x + 7, top + 3], fill=color)
            draw.rectangle([x - 2, bottom - 3, x + 7, bottom], fill=color)
            x += 12
        else:
            for dx in range(6):
                draw.line([(x + dx, top), (x + 7 + dx, bottom)], fill=color)
                draw.line([(x + 14 - dx, top), (x + 7 - dx, bottom)], fill=color)
            x += 20


def disc(draw, rim, studs):
    """The socket ring round a disc: a steel rim with eight studs."""
    draw.ellipse([4, 4, 59, 59], outline=(*rim, 255), width=4)
    for angle in range(0, 360, 45):
        x = 31.5 + 25 * math.cos(math.radians(angle))
        y = 31.5 + 25 * math.sin(math.radians(angle))
        draw.rectangle([x - 1.5, y - 1.5, x + 1.5, y + 1.5], fill=(*studs, 255))


def plate(tier):
    image = noisy(64, SLATE_DARK, f"plate_{tier}", amount=3)
    draw = ImageDraw.Draw(image)
    disc(draw, STEEL, STEEL_LIGHT)
    if tier == 0:
        draw.ellipse([10, 10, 53, 53], fill=(*shade(SLATE_DARK, -12), 255))
        draw.ellipse([14, 14, 49, 49], outline=(*shade(SLATE_DARK, 10), 255))
        save(image, "plate_0")
        return
    draw.ellipse([10, 10, 53, 53], fill=(*TEAL_DARK, 255))
    draw.ellipse([12, 12, 51, 51], outline=(*TEAL, 255), width=2)
    numeral(draw, tier, (*TEAL_BRIGHT, 255))
    save(image, f"plate_{tier}")


def write_textures() -> None:
    for old in TEXTURES.glob("*.png"):
        old.unlink()
    save(panel_seams(noisy(64, SLATE, "casing")), "casing")
    save(noisy(64, SLATE_DARK, "interior", amount=3), "interior")
    frame(STEEL, "frame_steel")
    frame(BRASS, "frame_brass")

    strip(mix(TEAL_DARK, SLATE_DARK, 0.5), TEAL_DARK, "strip_off")
    strip(TEAL, TEAL_BRIGHT, "strip_formed")
    strip(TEAL_BRIGHT, WHITE_HOT, "strip_active")
    strip(mix(BLUE_DARK, SLATE_DARK, 0.5), BLUE_DARK, "estrip_off")
    strip(BLUE, BLUE_BRIGHT, "estrip_formed")
    strip(BLUE_BRIGHT, WHITE_HOT, "estrip_active")
    strip(AMBER, AMBER_BRIGHT, "strip_amber")

    pane(TEAL_DARK, TEAL, "pane_off")
    pane(TEAL, TEAL_BRIGHT, "pane_formed")
    pane(TEAL_BRIGHT, WHITE_HOT, "pane_active")
    pane(AMBER, AMBER_BRIGHT, "pane_amber")

    core(BLUE_DARK, BLUE, "core_0", fill=0.0)
    for level in range(1, CHARGE_LEVELS + 1):
        core(BLUE, BLUE_BRIGHT, f"core_{level}", fill=level / CHARGE_LEVELS)
    core(BLUE_BRIGHT, WHITE_HOT, "core_active")
    lens(BLUE_DARK, BLUE, "lens_off")
    lens(BLUE, BLUE_BRIGHT, "lens_formed")
    lens(BLUE_BRIGHT, WHITE_HOT, "lens_active")

    aperture = noisy(64, SLATE_DARK, "aperture", amount=3)
    draw = ImageDraw.Draw(aperture)
    disc(draw, STEEL, STEEL_LIGHT)
    draw.ellipse([12, 12, 51, 51], fill=(*GLASS, 255))
    draw.ellipse([20, 20, 43, 43], outline=(*TEAL_DARK, 255), width=2)
    save(aperture, "aperture")

    port = panel_seams(noisy(64, SLATE_DARK, "port", amount=4), step=32, rivets=True)
    draw = ImageDraw.Draw(port)
    draw.ellipse([14, 14, 49, 49], outline=(*STEEL_LIGHT, 255), width=3)
    draw.ellipse([22, 22, 41, 41], fill=(*shade(SLATE_DARK, -16), 255))
    for p in ((27, 27), (36, 27), (27, 36), (36, 36)):
        draw.rectangle([p[0], p[1], p[0] + 1, p[1] + 1], fill=(*COPPER, 255))
    save(port, "port")

    halo = noisy(64, SLATE_DARK, "halo", amount=3)
    draw = ImageDraw.Draw(halo)
    disc(draw, BRASS, BRASS_LIGHT)
    draw.ellipse([12, 12, 51, 51], fill=(*AMBER_DARK, 255))
    draw.ellipse([16, 16, 47, 47], outline=(*AMBER, 255), width=3)
    draw.ellipse([26, 26, 37, 37], fill=(*AMBER_BRIGHT, 255))
    for angle in range(0, 360, 90):
        x = 31.5 + 12 * math.cos(math.radians(angle))
        y = 31.5 + 12 * math.sin(math.radians(angle))
        draw.line([(31.5, 31.5), (x, y)], fill=(*AMBER, 255), width=2)
    save(halo, "halo")

    # Mirror plate: polished steel-blue with diagonal highlight streaks and an amber cast, so the bore reads as a reflector.
    mirror = noisy(64, (120, 140, 160), "mirror", amount=3)
    pixels = mirror.load()
    for y in range(64):
        for x in range(64):
            diagonal = (x + y) % 24
            if diagonal < 3:
                pixels[x, y] = (*mix((246, 232, 206), (120, 140, 160), diagonal / 3), 255)
            elif 12 <= diagonal < 15:
                pixels[x, y] = (*shade((96, 118, 140), -8), 255)
            if x % 32 in (0, 31) or y % 32 in (0, 31):
                pixels[x, y] = (*shade((70, 84, 100), -10), 255)
    save(mirror, "mirror")

    seat = noisy(64, GLASS, "seat", amount=3)
    draw = ImageDraw.Draw(seat)
    draw.rectangle([6, 6, 57, 57], outline=(*AMBER_DARK, 255), width=2)
    draw.ellipse([16, 16, 47, 47], outline=(*AMBER, 255), width=2)
    draw.ellipse([26, 26, 37, 37], fill=(*AMBER_BRIGHT, 255))
    save(seat, "seat")

    for tier in range(TIERS + 1):
        plate(tier)


if __name__ == "__main__":
    write_textures()
    write_models()
    write_blockstates()
    print("Phase Array models and textures written")
