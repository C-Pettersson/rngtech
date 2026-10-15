---
navigation:
  title: "Hardness"
  parent: crusher.md
  position: 1
  icon: rngtech:iron_crusher_chassis
---

# Hardness

Every recipe has a hardness level, and every Crush Head has a hardness level from 1 to 8. The Crusher still runs a recipe that is harder than its head, but for each missing level:

- the craft takes one extra recipe-length of time and costs one extra recipe's worth of FE,
- the craft has a 5% chance to jam when it starts, which pauses the Crusher for a short time without using the input. The chance is rolled once per craft, so waiting for power does not raise it,
- bonus output, Super Output, and salvage are switched off.

A Crush Head with **Jam Chance per Cycle**, such as the <ItemLink id="rngtech:mineshaft_worn_pick_jaw" />, can also jam on recipes at or below its hardness. Such a jam lasts as long as a one-level jam.

Recipes at hardness 7 or higher also cost twice the FE. Material recipes for Stage 5–8 metals cost 2×, 3×, 4×, and 6× the early FE baseline.

## Jam Debris

A jam on a recipe of hardness 5 or higher drops one <ItemLink id="rngtech:jam_debris" /> into the output slot, if the slot is empty or already holds Jam Debris. The jam still pauses the Crusher and keeps the input. Jam Debris is an ingredient of the [Volatile Catalyst](rngtech:affix-forge/crafting-the-consumables.md), so to collect it, crush ore harder than your Crush Head on purpose and keep the output slot clear. Jam Chance and Jam Recovery change how fast it arrives.

| Crush Head | Stage | Hardness |
|---|---:|---:|
| <ItemLink id="rngtech:flint_crush_head" /> | 0 | 1 |
| <ItemLink id="rngtech:iron_crush_head" /> | 1 | 2 |
| <ItemLink id="rngtech:copper_crush_head" /> | 2 | 2 |
| <ItemLink id="rngtech:bronze_crush_head" /> | 3 | 3 |
| <ItemLink id="rngtech:steel_crush_head" /> | 4 | 4 |
| <ItemLink id="rngtech:aluminum_crush_head" /> | 5 | 5 |
| <ItemLink id="rngtech:titanium_crush_head" /> | 6 | 6 |
| <ItemLink id="rngtech:tungstensteel_crush_head" /> | 7 | 7 |
| <ItemLink id="rngtech:exotic_crush_head" /> | 8 | 8 |
