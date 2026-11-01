---
navigation:
  title: "Stages"
  parent: crusher.md
  position: 3
  icon: rngtech:iron_crusher_chassis
---

# Stages

| Stage | Crusher | Notes |
|---:|---|---|
| 0 | <ItemLink id="rngtech:wooden_crusher_chassis" /> | Primitive body: weak buffer, slow, and uses more energy. |
| 1 | <ItemLink id="rngtech:iron_crusher_chassis" /> | Baseline powered Crusher. |
| 2 | <ItemLink id="rngtech:copper_crusher_chassis" /> | Better energy use, smaller buffer. |
| 3 | <ItemLink id="rngtech:gold_crusher_chassis" /> | Copper numbers with one built-in [jewel socket](rngtech:jewels.md#body-sockets). |
| 3 | <ItemLink id="rngtech:bronze_crusher_chassis" /> | More output, higher energy use. |
| 4 | <ItemLink id="rngtech:steel_crusher_chassis" /> | Durable and efficient but slower. Has Output Guard. |
| 5 | <ItemLink id="rngtech:aluminum_crusher_chassis" /> | Fast and efficient, smaller buffer. |
| 6 | <ItemLink id="rngtech:titanium_crusher_chassis" /> | High throughput and stronger output. Has Output Guard. |
| 7 | <ItemLink id="rngtech:tungstensteel_crusher_chassis" /> | Heavy output body. Crushes up to 4 items per cycle. Has Output Guard. |
| 8 | <ItemLink id="rngtech:exotic_crusher_chassis" /> | Endgame body. Crushes up to 9 items per cycle. Has Output Guard and a far larger internal buffer than any other chassis. |

Output Guard keeps progress for a while when the output slot is full instead of resetting it. Batching chassis process several items from the same input stack in one cycle; each extra item makes the cycle a little longer. A batch takes as many items as the input stack and the free output space allow, up to the batch size, and keeps that count until the cycle ends.
