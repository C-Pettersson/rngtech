"""
Writes the GameTest structure `gametest_line`: a 12 by 4 by 5 room with a stone floor at y 0 and air above, long
enough for a Phase Array line of nine Coils along x. Run from the repository root:

    python tools/gametest/line_template.py
"""

from __future__ import annotations

import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / "src/main/resources/data/rngtech/structure/gametest_line.nbt"
SIZE = (12, 4, 5)
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def string(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def named(tag: int, name: str, payload: bytes) -> bytes:
    return bytes([tag]) + string(name) + payload


def int_payload(value: int) -> bytes:
    return struct.pack(">i", value)


def int_list(values) -> bytes:
    return bytes([TAG_INT]) + struct.pack(">i", len(values)) + b"".join(int_payload(v) for v in values)


def compound(entries) -> bytes:
    return b"".join(entries) + bytes([TAG_END])


def compound_list(items) -> bytes:
    if not items:
        return bytes([TAG_END]) + struct.pack(">i", 0)
    return bytes([TAG_COMPOUND]) + struct.pack(">i", len(items)) + b"".join(items)


def main() -> None:
    blocks = []
    for x in range(SIZE[0]):
        for y in range(SIZE[1]):
            for z in range(SIZE[2]):
                blocks.append(compound([named(TAG_LIST, "pos", int_list([x, y, z])), named(TAG_INT, "state", int_payload(1 if y == 0 else 0))]))
    palette = [
        compound([named(TAG_STRING, "Name", string("minecraft:air"))]),
        compound([named(TAG_STRING, "Name", string("minecraft:stone"))]),
    ]
    root = named(TAG_COMPOUND, "", compound([
        named(TAG_LIST, "size", int_list(SIZE)),
        named(TAG_LIST, "entities", compound_list([])),
        named(TAG_LIST, "blocks", compound_list(blocks)),
        named(TAG_LIST, "palette", compound_list(palette)),
        named(TAG_INT, "DataVersion", int_payload(DATA_VERSION)),
    ]))
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(TARGET, "wb") as out:
        out.write(root)
    print(f"wrote {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
