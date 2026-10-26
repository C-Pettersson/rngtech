"""The Job Chip lamp shared by every machine that loads Work Orders.

Each of those machines carries a small blue lamp beside its status lamp. Its base model has the lamp unlit; a lit part
model of the same lamp, drawn a hair larger so it covers the unlit faces, is added by the blockstate's `work_order`
case while a Job Chip is active. The blue keeps it apart from the green, amber, and cyan status lamps. Machine
generators import this module; run it on its own to write the two lamp textures:

    python tools/blockmodels/job_lamp.py
"""

from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/rngtech"
TEXTURES = ASSETS / "textures/block/work_order"
TEX = "rngtech:block/work_order/"
KEY = "job_lamp"
TEXTURE = {KEY: TEX + "lamp_off"}
LIT_GROWTH = 0.02
ROTATIONS = {"north": 0, "east": 90, "south": 180, "west": 270}
OFF = (24, 30, 46)
ON = (70, 124, 250)


def r(value: float) -> float:
    return round(value, 4)


def _box(start, end, texture, faces, extra=None):
    element = {"from": [r(v) for v in start], "to": [r(v) for v in end],
               "faces": {face: {"texture": f"#{texture}"} for face in faces}}
    if extra:
        element.update(extra)
    return element


def element(start, end, faces):
    """The unlit lamp for a machine's base model, which needs `TEXTURE` among its textures."""
    return _box(start, end, KEY, faces)


def write_part(path: Path, start, end, faces) -> None:
    """The lit lamp part model at `path`, the blockstate adds while a Job Chip is active."""
    grown_start = [v - LIT_GROWTH for v in start]
    grown_end = [v + LIT_GROWTH for v in end]
    _write_json(path, {
        "render_type": "minecraft:cutout",
        "textures": {"particle": TEX + "lamp_on", "lit": TEX + "lamp_on"},
        "elements": [_box(grown_start, grown_end, "lit", faces,
                          extra={"neoforge_data": {"block_light": 15, "sky_light": 15}})],
    })


def blockstate(model: str, active_model: str, lamp_model: str) -> dict:
    """A multipart blockstate: the base or running model by `active`, plus the lit lamp while `work_order` is true."""
    parts = []
    for active, shown in (("false", model), ("true", active_model)):
        for facing, angle in ROTATIONS.items():
            parts.append({"when": {"active": active, "facing": facing}, "apply": _apply(shown, angle)})
    for facing, angle in ROTATIONS.items():
        parts.append({"when": {"facing": facing, "work_order": "true"}, "apply": _apply(lamp_model, angle)})
    return {"multipart": parts}


def write_blockstate(block_id: str, model: str, active_model: str, lamp_model: str) -> None:
    _write_json(ASSETS / f"blockstates/{block_id}.json", blockstate(model, active_model, lamp_model))


def _apply(model: str, angle: int) -> dict:
    return {"model": model, "y": angle} if angle else {"model": model}


def _write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def _lamp_texture(color, name, amount, glow):
    rand = random.Random(f"job_lamp:{name}")
    image = Image.new("RGBA", (64, 64))
    pixels = image.load()
    for y in range(64):
        for x in range(64):
            # A brighter core toward the middle reads as a lens rather than a flat panel.
            edge = min(x, y, 63 - x, 63 - y) / 32
            lift = glow * edge + rand.uniform(-amount, amount)
            pixels[x, y] = (*(max(0, min(255, round(c + lift))) for c in color), 255)
    return image


def write_textures() -> None:
    TEXTURES.mkdir(parents=True, exist_ok=True)
    _lamp_texture(OFF, "off", 3, 10).save(TEXTURES / "lamp_off.png")
    _lamp_texture(ON, "on", 6, 50).save(TEXTURES / "lamp_on.png")


if __name__ == "__main__":
    write_textures()
