#!/usr/bin/env python3
"""Generate RNGTech material item textures from the planned materials list.

The generator intentionally uses only the Python standard library so it can run
in the checked-in development environment without installing image tooling.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_ROOT = ROOT / "src/main/resources/assets/rngtech/textures/item"
BLOCK_TEXTURE_ROOT = ROOT / "src/main/resources/assets/rngtech/textures/block"
MATERIAL_ROOT = TEXTURE_ROOT / "materials"
ORE_BLOCK_ROOT = BLOCK_TEXTURE_ROOT / "ores"
DOC_ASSET_ROOT = ROOT / "docs/assets"
CATALOG_PATH = ROOT / "src/main/resources/data/rngtech/materials/catalog.json"
ORE_CATALOG_PATH = ROOT / "src/main/resources/data/rngtech/materials/ore_catalog.json"


MATERIALS = {
    "iron": {"base": "#b7b7ad", "accent": "#f2f2e8", "ore": "#8d8177", "stage": 1},
    "copper": {"base": "#d77a3c", "accent": "#ffc38a", "ore": "#7f6a5d", "stage": 2},
    "bronze": {"base": "#b97935", "accent": "#efb36a", "ore": "#7c6956", "stage": 3},
    "tin": {"base": "#c7d1d0", "accent": "#f2ffff", "ore": "#748387", "stage": 3},
    "zinc": {"base": "#a4adba", "accent": "#dce8f0", "ore": "#687681", "stage": 3},
    "gold": {"base": "#f0c331", "accent": "#fff27a", "ore": "#89714d", "stage": 3},
    "steel": {"base": "#747b7d", "accent": "#cbd0cf", "ore": "#565b5d", "stage": 4},
    "invar": {"base": "#8d988a", "accent": "#d6dfcc", "ore": "#5d675e", "stage": 4},
    "nickel": {"base": "#b9c7b0", "accent": "#eff8df", "ore": "#69756a", "stage": 4},
    "lead": {"base": "#505a71", "accent": "#8792aa", "ore": "#434a5b", "stage": 4},
    "silver": {"base": "#d6dde0", "accent": "#ffffff", "ore": "#68747b", "stage": 4},
    "aluminum": {"base": "#bfcdda", "accent": "#f5ffff", "ore": "#677584", "stage": 5},
    "sparksteel": {"base": "#4d87c7", "accent": "#b8e6ff", "ore": "#465a73", "stage": 5, "glow": "#6bbcff"},
    "osmium": {"base": "#7f9fb2", "accent": "#c4e8f6", "ore": "#4d6573", "stage": 5},
    "titanium": {"base": "#8ea0b7", "accent": "#d7e9ff", "ore": "#566579", "stage": 6},
    "arclite": {"base": "#edf4f8", "accent": "#ffc3e2", "ore": "#788494", "stage": 6, "glow": "#89c8ff", "speck": "#86c9ff"},
    "tungsten": {"base": "#62666d", "accent": "#adb3bc", "ore": "#4d4e54", "stage": 7},
    "tungstensteel": {"base": "#4a525d", "accent": "#95a6b9", "ore": "#3b424a", "stage": 7},
    "nullite": {"base": "#21162f", "accent": "#7b5dad", "ore": "#332840", "stage": 7, "glow": "#a05cff"},
    "aethergold": {"base": "#d7a73b", "accent": "#fff0a4", "ore": "#776447", "stage": 7, "glow": "#ffd463"},
    "platinum": {"base": "#d8e6eb", "accent": "#ffffff", "ore": "#697a82", "stage": 7},
    "netherite": {"base": "#4c4149", "accent": "#8b7f84", "ore": "#3f353a", "stage": 8},
    "naquadah": {"base": "#2d8c74", "accent": "#6dffcf", "ore": "#35544d", "stage": 8, "glow": "#46ffb6"},
}

NON_STAGE_MATERIALS = {
    "stone": {"base": "#7c7c7c", "accent": "#b7b7b7", "kind": "chunk"},
    "clay": {"base": "#8d97aa", "accent": "#c7d3e2", "kind": "chunk"},
    "glass": {"base": "#93d5e1", "accent": "#e7ffff", "kind": "lens"},
    "coal": {"base": "#25282b", "accent": "#62676c", "kind": "chunk"},
    "charcoal": {"base": "#2f2b28", "accent": "#73685e", "kind": "chunk"},
    "wood": {"base": "#8b572b", "accent": "#d19a55", "kind": "rod"},
    "plant_reagent": {"base": "#4e9f43", "accent": "#bdf07d", "kind": "reagent"},
    "redstone": {"base": "#c31d28", "accent": "#ff605d", "kind": "dust"},
    "lapis_lazuli": {"base": "#2757bd", "accent": "#79a9ff", "kind": "gem"},
    "amethyst": {"base": "#9d54d9", "accent": "#e7bcff", "kind": "crystal"},
    "quartz": {"base": "#ddd3bf", "accent": "#ffffff", "kind": "crystal"},
    "diamond": {"base": "#65d7db", "accent": "#d9ffff", "kind": "gem"},
    "emerald": {"base": "#2bbf69", "accent": "#9affbf", "kind": "gem"},
    "ender_pearl": {"base": "#4fb58b", "accent": "#d9fff2", "kind": "pearl"},
    "eye_of_ender": {"base": "#7ccf7d", "accent": "#f1d45a", "kind": "eye"},
    "glowstone": {"base": "#e0a943", "accent": "#fff2a2", "kind": "dust"},
    "obsidian": {"base": "#21182f", "accent": "#6c4b95", "kind": "chunk"},
    "blaze_rod": {"base": "#e27728", "accent": "#ffdc5e", "kind": "rod"},
    "blaze_powder": {"base": "#dc7128", "accent": "#ffe14c", "kind": "dust"},
    "slime": {"base": "#69c853", "accent": "#c9ff9b", "kind": "pearl"},
    "honey": {"base": "#d8931e", "accent": "#ffd866", "kind": "droplet"},
    "resin": {"base": "#b76828", "accent": "#ffb765", "kind": "droplet"},
    "rubber": {"base": "#252628", "accent": "#6c7072", "kind": "sheet"},
    "organic_reagent": {"base": "#5b8f3e", "accent": "#dfef8b", "kind": "reagent"},
    "lava_bucket": {"base": "#dc5d23", "accent": "#ffe35d", "kind": "bucket"},
    "leather": {"base": "#8c5132", "accent": "#d4915d", "kind": "sheet"},
}

METAL_FORMS = [
    "ingot",
    "dust",
    "nugget",
    "storage_block",
    "ore",
    "raw",
    "crushed",
    "plate",
    "gear",
    "rod",
    "coil",
    "casing",
]

GENERIC_FORMS = [
    "machine_frame",
    "advanced_machine_frame",
    "exotic_machine_frame",
    "casing",
    "mechanism",
    "heat_core",
    "crush_head",
    "energy_coil",
    "battery_cell",
    "control_board",
    "basic_electric_circuit",
    "advanced_electric_circuit",
    "elite_electric_circuit",
    "ultimate_electric_circuit",
    "insulator",
    "lens",
    "matrix",
    "catalyst",
    "scrap",
    "fragment",
    "spent_catalyst",
    "abrasive_compound",
    "output_bin",
    "refractory_casing",
    "ignition_catalyst",
    "heat_shield",
    "fuel_chamber",
    "connector",
    "cell_shell",
    "electrolyte",
    "conductive_plate",
    "stabilization_catalyst",
    "bus_bar",
    "balancing_board",
    "shielding",
    "redstone_control",
    "risk_catalyst",
    "recycling_byproduct",
]


def color(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def mix(a: tuple[int, int, int, int], b: tuple[int, int, int, int], amount: float) -> tuple[int, int, int, int]:
    amount = max(0.0, min(1.0, amount))
    return tuple(round(a[i] * (1 - amount) + b[i] * amount) for i in range(4))


def shade(c: tuple[int, int, int, int], amount: float) -> tuple[int, int, int, int]:
    target = (255, 255, 255, c[3]) if amount >= 0 else (0, 0, 0, c[3])
    return mix(c, target, abs(amount))


def palette(spec: dict[str, object]) -> dict[str, tuple[int, int, int, int]]:
    base = color(str(spec["base"]))
    accent = color(str(spec["accent"]))
    dark = shade(base, -0.55)
    shadow = shade(base, -0.32)
    mid = base
    light = mix(base, accent, 0.55)
    glow = color(str(spec.get("glow", spec["accent"])))
    ore = color(str(spec.get("ore", "#666666")))
    return {
        "dark": dark,
        "shadow": shadow,
        "mid": mid,
        "light": light,
        "accent": accent,
        "glow": glow,
        "ore": ore,
        "stone": color("#6c6a66"),
        "stone_dark": color("#383735"),
        "empty": (0, 0, 0, 0),
    }


class Canvas:
    def __init__(self, width: int = 16, height: int = 16) -> None:
        self.width = width
        self.height = height
        self.pixels = [(0, 0, 0, 0)] * (width * height)

    def copy(self) -> "Canvas":
        other = Canvas(self.width, self.height)
        other.pixels = list(self.pixels)
        return other

    def set(self, x: int, y: int, c: tuple[int, int, int, int]) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y * self.width + x] = c

    def get(self, x: int, y: int) -> tuple[int, int, int, int]:
        if 0 <= x < self.width and 0 <= y < self.height:
            return self.pixels[y * self.width + x]
        return (0, 0, 0, 0)

    def rect(self, x0: int, y0: int, x1: int, y1: int, c: tuple[int, int, int, int]) -> None:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def line(self, x0: int, y0: int, x1: int, y1: int, c: tuple[int, int, int, int]) -> None:
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def polygon(self, points: list[tuple[int, int]], c: tuple[int, int, int, int]) -> None:
        min_y = max(0, min(y for _, y in points))
        max_y = min(self.height - 1, max(y for _, y in points))
        for y in range(min_y, max_y + 1):
            xs: list[float] = []
            for i, (x1, y1) in enumerate(points):
                x2, y2 = points[(i + 1) % len(points)]
                if y1 == y2:
                    continue
                if min(y1, y2) <= y < max(y1, y2):
                    xs.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
            xs.sort()
            for start, end in zip(xs[::2], xs[1::2]):
                for x in range(math.ceil(start), math.floor(end) + 1):
                    self.set(x, y, c)

    def circle(self, cx: int, cy: int, radius: int, c: tuple[int, int, int, int]) -> None:
        r2 = radius * radius
        for y in range(cy - radius, cy + radius + 1):
            for x in range(cx - radius, cx + radius + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r2:
                    self.set(x, y, c)


def hashed(name: str, x: int, y: int) -> int:
    seed = 2166136261
    for ch in f"{name}:{x}:{y}":
        seed ^= ord(ch)
        seed = (seed * 16777619) & 0xFFFFFFFF
    return seed


def draw_sparkles(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    for x, y in [(3, 4), (11, 5), (5, 11), (13, 10)]:
        if hashed(name, x, y) % 3 == 0:
            c.set(x, y, p["accent"])


INGOT_TEMPLATE = [
    "                ",
    "                ",
    "          33    ",
    "       333664   ",
    "    3336777764  ",
    " 33367777777764 ",
    "3877777777778874",
    "3687777778887461",
    "3668778887544561",
    "3666887544446661",
    "356675444466511 ",
    " 356754455111   ",
    "  35652111      ",
    "   3311         ",
    "                ",
    "                ",
]

INGOT_LUMINANCE = {
    "1": 53,
    "2": 88,
    "3": 94,
    "4": 114,
    "5": 130,
    "6": 168,
    "7": 216,
    "8": 255,
}


def tint_ingot_pixel(p: dict[str, tuple[int, int, int, int]], luminance: int) -> tuple[int, int, int, int]:
    gray = (luminance, luminance, luminance, 255)
    if luminance <= 94:
        return mix(gray, p["dark"], 0.60)
    if luminance <= 130:
        return mix(gray, p["shadow"], 0.52)
    if luminance <= 168:
        return mix(gray, p["mid"], 0.58)
    if luminance <= 216:
        return mix(gray, p["light"], 0.38)
    return mix(gray, p["light"], 0.14)


def draw_ingot(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    for y, row in enumerate(INGOT_TEMPLATE):
        for x, level in enumerate(row):
            luminance = INGOT_LUMINANCE.get(level)
            if luminance is not None:
                c.set(x, y, tint_ingot_pixel(p, luminance))


def draw_dust(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(2, 12), (5, 6), (8, 3), (12, 7), (14, 12)], p["dark"])
    c.polygon([(3, 12), (5, 8), (8, 4), (11, 8), (13, 12)], p["mid"])
    c.polygon([(5, 11), (7, 6), (10, 9), (12, 12)], p["light"])
    for y in range(4, 13):
        for x in range(2, 15):
            if c.get(x, y)[3] and hashed(name, x, y) % 5 == 0:
                c.set(x, y, p["accent"] if hashed(name, y, x) % 2 else p["shadow"])
    c.line(3, 12, 14, 12, p["dark"])


def draw_nugget(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(5, 6), (8, 4), (11, 5), (12, 8), (10, 11), (6, 11), (4, 9)], p["dark"])
    c.polygon([(6, 7), (8, 5), (10, 6), (11, 8), (9, 10), (6, 10), (5, 8)], p["mid"])
    c.set(7, 6, p["accent"])
    c.set(8, 6, p["light"])
    c.set(10, 8, p["shadow"])


def draw_storage_block(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.rect(3, 3, 12, 12, p["dark"])
    c.rect(4, 4, 11, 11, p["mid"])
    c.rect(4, 4, 11, 5, p["light"])
    c.rect(4, 4, 5, 11, p["light"])
    c.rect(10, 6, 11, 11, p["shadow"])
    c.rect(6, 10, 11, 11, p["shadow"])
    c.line(6, 6, 9, 6, p["accent"])
    c.line(6, 8, 9, 8, p["accent"])


def draw_casing_body(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(4, 3), (11, 3), (14, 6), (7, 7)], p["dark"])
    c.polygon([(3, 6), (7, 7), (7, 13), (3, 11)], p["dark"])
    c.polygon([(7, 7), (14, 6), (14, 12), (7, 13)], p["dark"])
    c.polygon([(5, 4), (11, 4), (13, 6), (7, 6)], p["light"])
    c.polygon([(4, 7), (7, 8), (7, 12), (4, 11)], p["mid"])
    c.polygon([(8, 8), (13, 7), (13, 11), (8, 12)], p["shadow"])
    for x, y in [(5, 8), (6, 11), (10, 8), (12, 10)]:
        c.set(x, y, p["accent"] if hashed(name, x, y) % 2 else p["dark"])


def draw_ore(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.rect(2, 2, 13, 13, p["stone_dark"])
    c.rect(3, 3, 12, 12, p["stone"])
    for x, y in [(3, 3), (12, 3), (3, 12), (12, 12), (6, 5), (10, 10)]:
        c.set(x, y, p["stone_dark"] if hashed(name, x, y) % 2 else shade(p["stone"], 0.12))
    c.polygon([(5, 4), (7, 4), (8, 6), (6, 7), (5, 6)], p["mid"])
    c.polygon([(10, 5), (12, 6), (11, 8), (9, 8), (9, 6)], p["light"])
    c.polygon([(4, 10), (6, 9), (7, 11), (6, 12), (4, 12)], p["accent"])
    c.polygon([(9, 11), (11, 10), (12, 12), (10, 13)], p["mid"])


def draw_raw(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    chunks = [
        ([(3, 9), (5, 5), (8, 4), (10, 7), (8, 12), (4, 12)], p["dark"], p["mid"]),
        ([(9, 4), (13, 5), (14, 9), (11, 11), (8, 8)], p["shadow"], p["light"]),
        ([(6, 11), (10, 9), (13, 12), (11, 14), (7, 14)], p["dark"], p["shadow"]),
    ]
    for outer, border, fill in chunks:
        c.polygon(outer, border)
        inner = [(max(0, min(15, x)), max(0, min(15, y + 1))) for x, y in outer]
        c.polygon(inner, fill)
    for x, y in [(5, 6), (9, 5), (11, 6), (8, 10), (10, 12)]:
        c.set(x, y, p["accent"] if hashed(name, x, y) % 2 else p["light"])
    for x, y in [(4, 12), (12, 10), (7, 14)]:
        c.set(x, y, p["dark"])


HOST_STONE_PALETTES = {
    "stone": {
        "base": color("#7f7f7f"),
        "dark": color("#666666"),
        "shadow": color("#707070"),
        "light": color("#949494"),
        "chip": color("#5c5c5c"),
    },
    "deepslate": {
        "base": color("#50545c"),
        "dark": color("#393d44"),
        "shadow": color("#444850"),
        "light": color("#676b74"),
        "chip": color("#30343a"),
    },
    "nether": {
        "base": color("#6d272d"),
        "dark": color("#4f1b21"),
        "shadow": color("#5d2026"),
        "light": color("#8a3438"),
        "chip": color("#3f151b"),
    },
    "end": {
        "base": color("#d7d1a3"),
        "dark": color("#aaa27a"),
        "shadow": color("#c4bd91"),
        "light": color("#ebe5b7"),
        "chip": color("#8d8662"),
    },
}


ORE_LAYER_VARIANTS = [
    {"name": "pockets", "target": 38, "jitter": 8, "warp": 0.9, "style": "pockets"},
    {"name": "clusters", "target": 42, "jitter": 8, "warp": 0.8, "style": "clusters"},
    {"name": "flecks", "target": 32, "jitter": 8, "warp": 0.7, "style": "flecks"},
    {"name": "broken_lodes", "target": 36, "jitter": 8, "warp": 0.8, "style": "broken_lodes"},
    {"name": "rich_pockets", "target": 46, "jitter": 8, "warp": 0.9, "style": "rich_pockets"},
]
ORE_HOTSPOT_EDGE_MARGIN = 1


def ore_hotspot_inset(x: int, y: int) -> bool:
    return (
        ORE_HOTSPOT_EDGE_MARGIN <= x < 16 - ORE_HOTSPOT_EDGE_MARGIN
        and ORE_HOTSPOT_EDGE_MARGIN <= y < 16 - ORE_HOTSPOT_EDGE_MARGIN
    )


def host_stone_color(host: str, name: str, x: int, y: int) -> tuple[int, int, int, int]:
    p = HOST_STONE_PALETTES[host]
    noise = hashed(f"{name}:{host}:host", x, y) % 13
    if noise in {0, 1}:
        return p["dark"]
    if noise in {2, 3}:
        return p["shadow"]
    if noise in {4, 5}:
        return p["light"]
    if noise == 6:
        return p["chip"]
    return p["base"]


def draw_host_stone(c: Canvas, host: str, name: str) -> None:
    for y in range(16):
        for x in range(16):
            c.set(x, y, host_stone_color(host, name, x, y))
    cracks = {
        "stone": [((1, 4), (5, 4)), ((9, 1), (12, 3)), ((3, 13), (7, 12)), ((11, 10), (15, 11))],
        "deepslate": [((0, 5), (5, 6)), ((7, 2), (12, 1)), ((3, 11), (9, 13)), ((12, 8), (15, 7))],
        "nether": [((2, 3), (6, 5)), ((10, 2), (14, 4)), ((1, 11), (5, 10)), ((9, 13), (14, 12))],
        "end": [((1, 5), (6, 4)), ((9, 3), (14, 2)), ((3, 12), (8, 13)), ((10, 10), (15, 9))],
    }[host]
    chip = HOST_STONE_PALETTES[host]["chip"]
    light = HOST_STONE_PALETTES[host]["light"]
    for index, ((x0, y0), (x1, y1)) in enumerate(cracks):
        c.line(x0, y0, x1, y1, chip)
        if index % 2 == 0:
            c.set(max(0, x0 - 1), y0, light)


def lerp(a: float, b: float, amount: float) -> float:
    return a * (1.0 - amount) + b * amount


def smoothstep(amount: float) -> float:
    return amount * amount * (3.0 - 2.0 * amount)


def hash_float(key: str, x: int, y: int) -> float:
    return (hashed(key, x, y) & 0xFFFF) / 65535.0


def value_noise(key: str, x: float, y: float, scale: float) -> float:
    nx = x / scale
    ny = y / scale
    x0 = math.floor(nx)
    y0 = math.floor(ny)
    tx = smoothstep(nx - x0)
    ty = smoothstep(ny - y0)
    v00 = hash_float(key, x0, y0)
    v10 = hash_float(key, x0 + 1, y0)
    v01 = hash_float(key, x0, y0 + 1)
    v11 = hash_float(key, x0 + 1, y0 + 1)
    return lerp(lerp(v00, v10, tx), lerp(v01, v11, tx), ty)


def fbm_noise(key: str, x: float, y: float, base_scale: float, octaves: int = 4) -> float:
    total = 0.0
    amplitude = 1.0
    amplitude_total = 0.0
    scale = base_scale
    for octave in range(octaves):
        total += value_noise(f"{key}:octave_{octave}", x, y, max(1.35, scale)) * amplitude
        amplitude_total += amplitude
        amplitude *= 0.5
        scale *= 0.5
    return total / amplitude_total


def ore_pocket_noise(name: str, host: str, style: str, x: float, y: float) -> float:
    key = f"{name}:{host}:ore_pocket:{style}"
    blob_count = {
        "flecks": 8,
        "clusters": 6,
        "rich_pockets": 7,
        "broken_lodes": 7,
    }.get(style, 5)
    score = 0.0
    for index in range(blob_count):
        major = 2.0 + hash_float(f"{key}:major", index, 0) * 3.0
        minor = 1.45 + hash_float(f"{key}:minor", index, 0) * 1.9
        if style == "flecks":
            major *= 0.62
            minor *= 0.62
        elif style == "clusters":
            major *= 0.92
            minor *= 0.92
        elif style == "broken_lodes":
            major *= 1.08
            minor *= 0.72
        elif style == "rich_pockets":
            major *= 1.12
            minor *= 1.04

        center_margin = max(2.25, min(6.5, max(major, minor) * 0.55 + ORE_HOTSPOT_EDGE_MARGIN))
        center_span = max(0.0, 15.0 - center_margin * 2.0)
        cx = center_margin + hash_float(f"{key}:cx", index, 0) * center_span
        cy = center_margin + hash_float(f"{key}:cy", index, 0) * center_span

        angle = hash_float(f"{key}:angle", index, 0) * math.pi * 2.0
        cos_angle = math.cos(angle)
        sin_angle = math.sin(angle)
        dx = x - cx
        dy = y - cy
        rx = dx * cos_angle + dy * sin_angle
        ry = -dx * sin_angle + dy * cos_angle
        distance = (rx / major) ** 2 + (ry / minor) ** 2
        core = max(0.0, 1.0 - distance)
        halo = max(0.0, 1.0 - distance * 0.58) * 0.32
        score = max(score, smoothstep(core) + halo)
    return min(1.0, score)


def ore_layer_variant(name: str, host: str) -> dict[str, object]:
    return ORE_LAYER_VARIANTS[hashed(f"{name}:{host}:ore_layer_variant", 0, 0) % len(ORE_LAYER_VARIANTS)]


def ore_layer_value(name: str, host: str, variant: dict[str, object], x: int, y: int) -> float:
    warp = float(variant["warp"])
    wx = (fbm_noise(f"{name}:{host}:ore_warp_x", x, y, 8.0, 3) - 0.5) * warp
    wy = (fbm_noise(f"{name}:{host}:ore_warp_y", x, y, 8.0, 3) - 0.5) * warp
    nx = x + wx
    ny = y + wy

    style = str(variant["style"])
    pocket = ore_pocket_noise(name, host, style, nx, ny)
    cluster = fbm_noise(f"{name}:{host}:ore_cluster", nx, ny, 5.2, 3)
    grain = hash_float(f"{name}:{host}:ore_grain", x, y)

    if style == "flecks":
        return pocket * 0.74 + grain * 0.18 + cluster * 0.08
    if style == "broken_lodes":
        return pocket * 0.78 + grain * 0.12 + cluster * 0.10
    if style == "rich_pockets":
        return pocket * 0.82 + cluster * 0.12 + grain * 0.06
    return pocket * 0.80 + cluster * 0.14 + grain * 0.06


def ore_direction_length(mask: set[tuple[int, int]], x: int, y: int, dx: int, dy: int) -> int:
    length = 0
    x += dx
    y += dy
    while (x, y) in mask:
        length += 1
        x += dx
        y += dy
    return length


def break_thin_ore_runs(name: str, host: str, mask: set[tuple[int, int]]) -> set[tuple[int, int]]:
    cleaned = set(mask)
    for x, y in sorted(mask):
        left = ore_direction_length(mask, x, y, -1, 0)
        right = ore_direction_length(mask, x, y, 1, 0)
        up = ore_direction_length(mask, x, y, 0, -1)
        down = ore_direction_length(mask, x, y, 0, 1)
        horizontal_run = left + right + 1
        vertical_run = up + down + 1
        thin_vertical = vertical_run >= 6 and horizontal_run <= 2 and up >= 2 and down >= 2
        thin_horizontal = horizontal_run >= 6 and vertical_run <= 2 and left >= 2 and right >= 2
        if (thin_vertical or thin_horizontal) and hashed(f"{name}:{host}:ore_run_break", x, y) % 4 == 0:
            cleaned.discard((x, y))
    return cleaned


def ore_layer_mask(name: str, host: str) -> set[tuple[int, int]]:
    variant = ore_layer_variant(name, host)
    jitter = int(variant["jitter"])
    target = (
        int(variant["target"])
        + hashed(f"{name}:{host}:ore_layer_density", 0, 0) % jitter
        - jitter // 2
    )
    scored = [
        (ore_layer_value(name, host, variant, x, y), x, y)
        for y in range(ORE_HOTSPOT_EDGE_MARGIN, 16 - ORE_HOTSPOT_EDGE_MARGIN)
        for x in range(ORE_HOTSPOT_EDGE_MARGIN, 16 - ORE_HOTSPOT_EDGE_MARGIN)
    ]
    target = min(target, len(scored))
    scored.sort(reverse=True)
    mask = {(x, y) for _, x, y in scored[:target]}
    for _, x, y in scored[target:target + 20]:
        neighbours = sum(
            (x + dx, y + dy) in mask
            for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0))
        )
        if neighbours >= 3:
            mask.add((x, y))
    return break_thin_ore_runs(name, host, mask)


def ore_neighbour_count(mask: set[tuple[int, int]], x: int, y: int) -> int:
    return sum(
        (x + dx, y + dy) in mask
        for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0))
    )


def draw_metal_layer(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str, host: str) -> None:
    mask = ore_layer_mask(name, host)
    shadow = mix(p["dark"], HOST_STONE_PALETTES[host]["chip"], 0.25)
    for x, y in sorted(mask):
        neighbours = ore_neighbour_count(mask, x, y)
        surface = fbm_noise(f"{name}:{host}:ore_surface", x * 1.4, y * 1.4, 4.5, 3)
        if neighbours <= 1:
            px = p["dark"]
        elif neighbours == 2:
            px = p["shadow"]
        elif surface > 0.82:
            px = p["accent"]
        elif surface > 0.65:
            px = p["light"]
        elif surface < 0.22:
            px = p["shadow"]
        else:
            px = p["mid"]
        c.set(x, y, px)

    for x, y in sorted(mask):
        if (x - 1, y - 1) not in mask and hashed(f"{name}:{host}:ore_glint", x, y) % 5 == 0:
            c.set(x, y, p["accent"])
        for dx, dy in ((0, 1), (1, 0)):
            sx = x + dx
            sy = y + dy
            if ore_hotspot_inset(sx, sy) and (sx, sy) not in mask:
                c.set(sx, sy, mix(c.get(sx, sy), shadow, 0.32))


def draw_ore_block(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str, host: str) -> None:
    draw_host_stone(c, host, name)
    draw_metal_layer(c, p, name, host)


def draw_crushed(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    for x, y, r, col in [
        (4, 11, 2, p["stone"]),
        (7, 8, 3, p["mid"]),
        (11, 10, 3, p["shadow"]),
        (10, 6, 2, p["light"]),
        (5, 6, 2, p["stone_dark"]),
    ]:
        c.circle(x, y, r, p["dark"])
        c.circle(x, y, max(1, r - 1), col)
        c.set(x - 1, y - 1, p["accent"])
    for x, y in [(3, 13), (13, 13), (7, 12), (12, 5)]:
        c.set(x, y, p["light"] if hashed(name, x, y) % 2 else p["stone"])


def draw_plate(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(3, 6), (11, 4), (14, 8), (6, 12)], p["dark"])
    c.polygon([(4, 7), (11, 5), (13, 8), (6, 11)], p["mid"])
    c.line(5, 7, 11, 6, p["light"])
    for x, y in [(5, 8), (11, 8), (7, 10)]:
        c.set(x, y, p["accent"])


def draw_rod(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.line(3, 11, 12, 4, p["dark"])
    c.line(4, 12, 13, 5, p["dark"])
    c.line(4, 11, 12, 5, p["mid"])
    c.line(5, 10, 11, 5, p["light"])
    c.set(12, 4, p["accent"])


def draw_gear(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    for x, y in [(7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8), (4, 4), (11, 4), (4, 11), (11, 11)]:
        c.rect(x, y, x + 1, y + 1, p["dark"])
    for y in range(3, 13):
        for x in range(3, 13):
            d2 = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if 11 <= d2 <= 30:
                c.set(x, y, p["dark"])
            if 13 <= d2 <= 24:
                c.set(x, y, p["mid"])
            if 15 <= d2 <= 18 and y < 8:
                c.set(x, y, p["light"])


def draw_coil(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.rect(3, 6, 13, 10, p["dark"])
    c.rect(2, 5, 4, 11, p["shadow"])
    c.rect(12, 5, 14, 11, p["shadow"])
    for x in range(5, 12, 2):
        c.line(x, 5, x + 1, 11, p["light"])
        c.line(x + 1, 5, x + 2, 11, p["mid"])
    c.set(3, 8, p["accent"])
    c.set(13, 8, p["accent"])


def draw_casing(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    draw_casing_body(c, p, name)
    c.rect(6, 8, 11, 11, p["dark"])
    c.rect(7, 9, 10, 10, p["shadow"])
    c.set(7, 9, p["accent"])
    c.set(10, 10, p["light"])


def draw_advanced_machine_frame(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    draw_casing_body(c, p, name)
    c.polygon([(5, 5), (11, 4), (13, 7), (8, 8)], p["dark"])
    c.polygon([(5, 8), (8, 9), (8, 12), (5, 11)], p["dark"])
    c.polygon([(9, 9), (13, 8), (13, 11), (9, 12)], p["dark"])
    c.rect(7, 7, 10, 10, p["shadow"])
    c.rect(8, 8, 9, 9, p["glow"])
    c.set(7, 6, p["light"])
    c.set(10, 6, p["light"])
    c.set(6, 10, p["light"])


def draw_mechanism(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    draw_plate(c, p, name)
    draw_gear(c, p, name)
    c.rect(6, 6, 9, 9, p["dark"])


def draw_heat_core(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.rect(4, 3, 11, 13, p["dark"])
    c.rect(5, 4, 10, 12, p["shadow"])
    c.rect(6, 5, 9, 11, p["glow"])
    c.rect(7, 6, 8, 10, p["accent"])
    c.line(5, 4, 10, 4, p["light"])


def draw_crush_head(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(3, 4), (12, 4), (13, 9), (11, 12), (5, 12), (2, 9)], p["dark"])
    c.polygon([(4, 5), (11, 5), (12, 9), (10, 10), (5, 10), (3, 8)], p["mid"])
    for x in [4, 7, 10]:
        c.polygon([(x, 10), (x + 1, 10), (x, 13)], p["dark"])
    c.line(5, 5, 11, 6, p["light"])


def draw_battery_cell(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.rect(3, 6, 12, 10, p["dark"])
    c.rect(2, 7, 3, 9, p["dark"])
    c.rect(13, 7, 14, 9, p["dark"])
    c.rect(4, 7, 11, 9, p["shadow"])
    c.rect(5, 7, 9, 9, p["glow"])
    c.line(4, 6, 11, 6, p["light"])


def draw_board(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    board = color("#2f7b46")
    c.polygon([(3, 6), (11, 4), (14, 9), (6, 13)], p["dark"])
    c.polygon([(4, 7), (11, 5), (13, 9), (6, 12)], board)
    c.rect(7, 7, 10, 10, color("#24272b"))
    for x, y in [(5, 8), (11, 8), (6, 11), (12, 10)]:
        c.set(x, y, p["accent"])
    c.line(5, 8, 7, 8, p["light"])
    c.line(10, 9, 12, 10, p["light"])


def draw_electric_circuit(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str, tier: int) -> None:
    draw_board(c, p, name)
    c.rect(6, 6, 10, 10, p["dark"])
    c.rect(7, 7, 9, 9, p["shadow"])
    c.set(8, 8, p["glow"])
    for i in range(tier):
        c.set(4 + i * 2, 6, p["accent"])
        c.set(5 + i * 2, 11, p["light"])
    if tier >= 3:
        c.line(5, 10, 11, 6, p["glow"])
    if tier >= 4:
        c.rect(7, 7, 9, 9, p["glow"])
        c.set(8, 8, color("#ffffff"))


def draw_lens(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.circle(8, 8, 6, p["dark"])
    c.circle(8, 8, 5, mix(p["light"], color("#bfffff", 190), 0.5))
    c.circle(7, 7, 3, mix(p["accent"], color("#ffffff", 210), 0.45))
    c.set(5, 5, color("#ffffff"))
    c.set(6, 5, color("#ffffff", 210))


def draw_catalyst(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.circle(8, 8, 4, p["dark"])
    c.circle(8, 8, 3, p["glow"])
    for x, y in [(8, 2), (8, 14), (2, 8), (14, 8), (4, 4), (12, 4), (4, 12), (12, 12)]:
        c.circle(x, y, 1, p["accent"])
        c.line(8, 8, x, y, p["shadow"])


def draw_scrap(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    for pts, col in [
        ([(3, 10), (6, 7), (9, 9), (7, 13)], p["shadow"]),
        ([(8, 5), (12, 6), (11, 10), (7, 9)], p["mid"]),
        ([(5, 4), (8, 5), (6, 8), (3, 7)], p["light"]),
        ([(10, 11), (14, 10), (13, 13), (9, 13)], p["dark"]),
    ]:
        c.polygon(pts, p["dark"])
        inner = [(x, y + 1 if y < 12 else y) for x, y in pts]
        c.polygon(inner, col)


def draw_fragment(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(4, 11), (6, 5), (10, 3), (13, 8), (10, 13)], p["dark"])
    c.polygon([(5, 10), (7, 6), (10, 4), (12, 8), (9, 12)], p["mid"])
    c.line(7, 6, 10, 4, p["accent"])
    c.line(6, 10, 9, 12, p["shadow"])


def draw_bucket(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    metal = palette(MATERIALS["iron"])
    c.rect(4, 5, 11, 12, metal["dark"])
    c.rect(5, 6, 10, 11, metal["mid"])
    c.rect(6, 7, 9, 10, p["glow"])
    c.line(4, 5, 8, 2, metal["light"])
    c.line(8, 2, 12, 5, metal["light"])


def draw_sheet(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.polygon([(4, 4), (12, 6), (10, 13), (3, 11)], p["dark"])
    c.polygon([(5, 5), (11, 7), (9, 12), (4, 10)], p["mid"])
    c.line(5, 6, 10, 8, p["light"])
    c.line(4, 10, 9, 12, p["shadow"])


def draw_reagent(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    draw_dust(c, p, name)
    for x, y in [(5, 5), (10, 6), (7, 3), (11, 10)]:
        c.set(x, y, p["accent"])


def draw_pearl(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    c.circle(8, 8, 5, p["dark"])
    c.circle(8, 8, 4, p["mid"])
    c.circle(7, 6, 2, p["accent"])
    c.set(5, 5, color("#ffffff", 230))


def draw_eye(c: Canvas, p: dict[str, tuple[int, int, int, int]], name: str) -> None:
    draw_pearl(c, p, name)
    c.circle(8, 8, 2, color("#263b2f"))
    c.set(8, 8, p["accent"])


def render_metal(form: str, material: str, spec: dict[str, object]) -> Canvas:
    p = palette(spec)
    c = Canvas()
    draw = {
        "ingot": draw_ingot,
        "dust": draw_dust,
        "nugget": draw_nugget,
        "storage_block": draw_storage_block,
        "ore": draw_ore,
        "raw": draw_raw,
        "crushed": draw_crushed,
        "plate": draw_plate,
        "gear": draw_gear,
        "rod": draw_rod,
        "coil": draw_coil,
        "casing": draw_casing,
    }[form]
    draw(c, p, material)
    if material == "arclite":
        speck = color(str(spec.get("speck", spec["glow"])))
        for x, y, col in [
            (4, 5, p["accent"]),
            (11, 6, speck),
            (6, 10, speck),
            (13, 11, p["accent"]),
        ]:
            if c.get(x, y)[3]:
                c.set(x, y, col)
    return c


def render_generic(form: str) -> Canvas:
    palettes = {
        "machine_frame": MATERIALS["steel"],
        "advanced_machine_frame": MATERIALS["aluminum"],
        "exotic_machine_frame": MATERIALS["tungstensteel"],
        "casing": MATERIALS["steel"],
        "mechanism": MATERIALS["bronze"],
        "heat_core": MATERIALS["arclite"],
        "crush_head": MATERIALS["iron"],
        "energy_coil": MATERIALS["copper"],
        "battery_cell": MATERIALS["sparksteel"],
        "control_board": NON_STAGE_MATERIALS["redstone"],
        "basic_electric_circuit": MATERIALS["copper"],
        "advanced_electric_circuit": MATERIALS["gold"],
        "elite_electric_circuit": NON_STAGE_MATERIALS["diamond"],
        "ultimate_electric_circuit": MATERIALS["netherite"],
        "insulator": NON_STAGE_MATERIALS["rubber"],
        "lens": NON_STAGE_MATERIALS["quartz"],
        "matrix": MATERIALS["naquadah"],
        "catalyst": NON_STAGE_MATERIALS["amethyst"],
        "scrap": MATERIALS["steel"],
        "fragment": MATERIALS["osmium"],
        "spent_catalyst": MATERIALS["netherite"],
        "abrasive_compound": NON_STAGE_MATERIALS["obsidian"],
        "output_bin": MATERIALS["aluminum"],
        "refractory_casing": NON_STAGE_MATERIALS["obsidian"],
        "ignition_catalyst": NON_STAGE_MATERIALS["blaze_powder"],
        "heat_shield": MATERIALS["lead"],
        "fuel_chamber": MATERIALS["steel"],
        "connector": MATERIALS["copper"],
        "cell_shell": MATERIALS["aluminum"],
        "electrolyte": NON_STAGE_MATERIALS["honey"],
        "conductive_plate": MATERIALS["sparksteel"],
        "stabilization_catalyst": NON_STAGE_MATERIALS["amethyst"],
        "bus_bar": MATERIALS["copper"],
        "balancing_board": NON_STAGE_MATERIALS["quartz"],
        "shielding": MATERIALS["lead"],
        "redstone_control": NON_STAGE_MATERIALS["redstone"],
        "risk_catalyst": MATERIALS["nullite"],
        "recycling_byproduct": MATERIALS["steel"],
    }
    spec = palettes[form]
    p = palette(spec)
    c = Canvas()
    if form == "advanced_machine_frame":
        draw_advanced_machine_frame(c, p, form)
    elif form in {
        "machine_frame",
        "exotic_machine_frame",
        "casing",
        "refractory_casing",
        "fuel_chamber",
        "output_bin",
    }:
        draw_casing(c, p, form)
    elif form == "mechanism":
        draw_mechanism(c, p, form)
    elif form == "heat_core":
        draw_heat_core(c, p, form)
    elif form == "crush_head":
        draw_crush_head(c, p, form)
    elif form == "energy_coil":
        draw_coil(c, p, form)
        c.set(8, 8, p["glow"])
    elif form == "battery_cell":
        draw_battery_cell(c, p, form)
    elif form in {"control_board", "balancing_board", "redstone_control"}:
        draw_board(c, p, form)
    elif form in {
        "basic_electric_circuit",
        "advanced_electric_circuit",
        "elite_electric_circuit",
        "ultimate_electric_circuit",
    }:
        tier = {
            "basic_electric_circuit": 1,
            "advanced_electric_circuit": 2,
            "elite_electric_circuit": 3,
            "ultimate_electric_circuit": 4,
        }[form]
        draw_electric_circuit(c, p, form, tier)
    elif form in {"insulator", "shielding"}:
        draw_coil(c, p, form)
    elif form == "lens":
        draw_lens(c, p, form)
    elif form == "matrix":
        draw_catalyst(c, p, form)
        c.rect(6, 6, 9, 9, p["dark"])
        c.rect(7, 7, 8, 8, p["glow"])
    elif form in {"catalyst", "ignition_catalyst", "stabilization_catalyst", "risk_catalyst"}:
        draw_catalyst(c, p, form)
    elif form in {"scrap", "recycling_byproduct"}:
        draw_scrap(c, p, form)
    elif form in {"fragment", "spent_catalyst"}:
        draw_fragment(c, p, form)
    elif form == "abrasive_compound":
        draw_dust(c, p, form)
    elif form == "connector":
        draw_rod(c, p, form)
        c.rect(2, 10, 4, 12, p["dark"])
        c.rect(11, 3, 13, 5, p["dark"])
    elif form == "bus_bar":
        c.rect(2, 7, 13, 9, p["dark"])
        c.rect(3, 6, 12, 8, p["mid"])
        c.line(4, 6, 11, 6, p["light"])
        c.rect(3, 10, 5, 12, p["shadow"])
        c.rect(10, 3, 12, 5, p["shadow"])
    elif form in {"cell_shell", "conductive_plate"}:
        draw_plate(c, p, form)
    elif form == "electrolyte":
        draw_pearl(c, p, form)
    elif form == "heat_shield":
        draw_plate(c, p, form)
        c.rect(6, 6, 10, 10, p["dark"])
    else:
        draw_nugget(c, p, form)
    return c


def render_non_stage(name: str, spec: dict[str, object]) -> Canvas:
    p = palette(spec)
    c = Canvas()
    kind = spec["kind"]
    if kind == "dust":
        draw_dust(c, p, name)
    elif kind == "gem":
        draw_fragment(c, p, name)
    elif kind == "crystal":
        draw_fragment(c, p, name)
        c.set(8, 4, p["accent"])
    elif kind == "pearl":
        draw_pearl(c, p, name)
    elif kind == "eye":
        draw_eye(c, p, name)
    elif kind == "rod":
        draw_rod(c, p, name)
    elif kind == "droplet":
        draw_pearl(c, p, name)
        c.set(8, 3, p["dark"])
    elif kind == "sheet":
        draw_sheet(c, p, name)
    elif kind == "reagent":
        draw_reagent(c, p, name)
    elif kind == "lens":
        draw_lens(c, p, name)
    elif kind == "bucket":
        draw_bucket(c, p, name)
    else:
        draw_crushed(c, p, name)
    return c


def png_bytes(canvas: Canvas) -> bytes:
    rows = []
    for y in range(canvas.height):
        row = bytearray([0])
        for x in range(canvas.width):
            row.extend(canvas.get(x, y))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", canvas.width, canvas.height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def write_png(path: Path, canvas: Canvas) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png_bytes(canvas))


def scaled(canvas: Canvas, scale: int) -> Canvas:
    out = Canvas(canvas.width * scale, canvas.height * scale)
    for y in range(canvas.height):
        for x in range(canvas.width):
            px = canvas.get(x, y)
            for yy in range(scale):
                for xx in range(scale):
                    out.set(x * scale + xx, y * scale + yy, px)
    return out


def paste(dst: Canvas, src: Canvas, ox: int, oy: int) -> None:
    for y in range(src.height):
        for x in range(src.width):
            px = src.get(x, y)
            if px[3]:
                dst.set(ox + x, oy + y, px)


def checker(width: int, height: int, tile: int = 8) -> Canvas:
    out = Canvas(width, height)
    a = color("#24272b")
    b = color("#30343a")
    for y in range(height):
        for x in range(width):
            out.set(x, y, a if ((x // tile) + (y // tile)) % 2 == 0 else b)
    return out


def write_preview(path: Path, icons: list[Canvas]) -> None:
    scale = 4
    cell = 20 * scale
    columns = 12
    rows = math.ceil(len(icons) / columns)
    preview = checker(columns * cell, rows * cell, 16)
    for index, icon in enumerate(icons):
        x = (index % columns) * cell + 2 * scale
        y = (index // columns) * cell + 2 * scale
        paste(preview, scaled(icon, scale), x, y)
    write_png(path, preview)


def load_material_catalog() -> dict:
    with CATALOG_PATH.open(encoding="utf-8") as handle:
        return json.load(handle)


def load_vanilla_form_mappings(catalog: dict) -> dict[str, set[str]]:
    return {
        material: set(forms)
        for material, forms in catalog.get("vanilla_form_mappings", {}).items()
    }


def form_excluded(material: str, form: str, catalog: dict) -> bool:
    for family in catalog["materials"]:
        if family["id"] == material:
            return form in family.get("excluded_forms", [])
    return False


def excluded_material_forms(catalog: dict) -> dict[str, list[str]]:
    return {
        family["id"]: list(family.get("excluded_forms", []))
        for family in catalog["materials"]
        if family.get("excluded_forms")
    }


def load_ore_catalog() -> dict:
    with ORE_CATALOG_PATH.open(encoding="utf-8") as handle:
        return json.load(handle)


def write_ore_block_textures(generated: dict[str, list[str]]) -> list[str]:
    ore_catalog = load_ore_catalog()
    hosts: list[str] = []
    active_textures: set[Path] = set()
    for ore in ore_catalog["ores"]:
        material = ore["material"]
        spec = MATERIALS[material]
        p = palette(spec)
        for host in ore.get("hosts", ["stone", "deepslate"]):
            if host not in hosts:
                hosts.append(host)
            icon = Canvas()
            draw_ore_block(icon, p, material, host)
            texture = ORE_BLOCK_ROOT / host / f"{material}.png"
            write_png(texture, icon)
            active_textures.add(texture)
            generated.setdefault("ore_blocks", []).append(texture.relative_to(ROOT).as_posix())
    for host_dir in ORE_BLOCK_ROOT.iterdir() if ORE_BLOCK_ROOT.exists() else []:
        if not host_dir.is_dir():
            continue
        for texture in host_dir.glob("*.png"):
            if texture.stem in MATERIALS and texture not in active_textures:
                texture.unlink()
    return hosts


def generate(reference_atlas: Path | None) -> dict[str, list[str]]:
    generated: dict[str, list[str]] = {"metal_forms": [], "generic_forms": [], "non_stage": [], "registered_items": []}
    preview_icons: list[Canvas] = []
    material_catalog = load_material_catalog()
    vanilla_form_mappings = load_vanilla_form_mappings(material_catalog)

    for material, spec in MATERIALS.items():
        for form in METAL_FORMS:
            texture = MATERIAL_ROOT / form / f"{material}.png"
            if form in vanilla_form_mappings.get(material, set()) or form_excluded(material, form, material_catalog):
                if texture.exists():
                    texture.unlink()
                continue
            icon = render_metal(form, material, spec)
            write_png(texture, icon)
            generated["metal_forms"].append(texture.relative_to(ROOT).as_posix())
            if form in {"ingot", "dust", "storage_block", "crushed"}:
                preview_icons.append(icon)

    for form in GENERIC_FORMS:
        icon = render_generic(form)
        texture = MATERIAL_ROOT / "forms" / f"{form}.png"
        write_png(texture, icon)
        generated["generic_forms"].append(texture.relative_to(ROOT).as_posix())
        preview_icons.append(icon)

    for material, spec in NON_STAGE_MATERIALS.items():
        icon = render_non_stage(material, spec)
        texture = MATERIAL_ROOT / "non_stage" / f"{material}.png"
        write_png(texture, icon)
        generated["non_stage"].append(texture.relative_to(ROOT).as_posix())
        preview_icons.append(icon)

    registered = {
        "crushed_iron": render_metal("crushed", "iron", MATERIALS["iron"]),
        "crushed_gold": render_metal("crushed", "gold", MATERIALS["gold"]),
        "crushed_copper": render_metal("crushed", "copper", MATERIALS["copper"]),
        "iron_crush_head": render_generic("crush_head"),
    }
    for name, icon in registered.items():
        texture = TEXTURE_ROOT / f"{name}.png"
        write_png(texture, icon)
        generated["registered_items"].append(texture.relative_to(ROOT).as_posix())

    ore_block_hosts = write_ore_block_textures(generated)

    DOC_ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    write_preview(DOC_ASSET_ROOT / "material-textures-preview.png", preview_icons)
    generated["preview"] = ["docs/assets/material-textures-preview.png"]

    target = DOC_ASSET_ROOT / "materials-imagegen-reference-atlas.png"
    if reference_atlas and reference_atlas.exists():
        shutil.copy2(reference_atlas, target)
    if target.exists():
        generated["imagegen_reference"] = [target.relative_to(ROOT).as_posix()]

    manifest = {
        "source": "docs/reference/materials.md",
        "generated_by": "tools/generate_material_textures.py",
        "material_count": len(MATERIALS),
        "metal_forms": METAL_FORMS,
        "vanilla_mapped_forms": {
            material: sorted(forms)
            for material, forms in vanilla_form_mappings.items()
        },
        "generic_forms": GENERIC_FORMS,
        "non_stage_materials": sorted(NON_STAGE_MATERIALS),
        "excluded_material_forms": excluded_material_forms(material_catalog),
        "ore_block_hosts": ore_block_hosts,
        "outputs": generated,
    }
    manifest_path = DOC_ASSET_ROOT / "material-textures-manifest.json"
    with manifest_path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(json.dumps(manifest, indent=2) + "\n")
    generated["manifest"] = [manifest_path.relative_to(ROOT).as_posix()]
    return generated


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reference-atlas", type=Path, help="Optional imagegen reference atlas to copy into docs/assets.")
    args = parser.parse_args()
    generated = generate(args.reference_atlas)
    count = sum(len(v) for v in generated.values())
    print(f"Generated {count} material texture artifacts.")


if __name__ == "__main__":
    main()
