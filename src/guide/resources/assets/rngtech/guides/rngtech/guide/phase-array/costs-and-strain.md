---
navigation:
  title: "Costs and Strain"
  parent: phase-array.md
  position: 5
  icon: rngtech:exotic_affix_forge
---

# Costs and Strain

A pulse's FE is the operation's base, times a factor for the item's stage, times `1.5` for every `3.5` points of the item's **Quality**, the sum of its affix tiers. The line's FE Thrift takes a percentage off.

| Item stage | Stage factor |
|---:|---:|
| 0 to 2 | 0.1 |
| 3 to 4 | 0.25 |
| 5 to 6 | 0.5 |
| 7 | 1 |
| 8 | 1.5 |

One Resonate on a Stage 8 item, from a 20 kFE/t supply:

| Item | Quality | FE | Charge time |
|---|---:|---:|---|
| Fresh Rare Exotic part | 12 | 12 MFE | 30 seconds |
| All affixes Tier 4 | 24 | 48 MFE | 2 minutes |
| Six Tier 6 affixes | 36 | 194 MFE | 8 minutes |
| Six Tier 7 affixes | 42 | 389 MFE | 16 minutes |

A Stage 5 item pays a third of that at the same Quality, and a Stage 2 item a fifteenth. The last pulses on an item cost thirty times the first.

Every pulse adds one **Strain** to the item, on every outcome, unless the line's Strain Mercy rolls. Strain shows on the item's tooltip with the Fracture chance it adds, and as marks around the chamber. Strain up to the line's Strain Tolerance adds no Fracture chance; every point past it adds 1%. Only Temper removes Strain, and Excise's Overhold takes one point off. A Reforge keeps it, so a reforged item is a fresh roll on a worn body.
