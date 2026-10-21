---
navigation:
  title: "Line stats"
  parent: phase-array.md
  position: 6
  icon: rngtech:exotic_affix_forge
---

# Line stats

The Emitter and Coils roll their own affixes, and the line reads them. Hover a Coil's node for its Hold Chance, Second Hold, Overhold Chance, and affixes, and the Emitter's node for the line's totals and its affixes.

| Stat | From | What it does |
|---|---|---|
| Charge Capacity, Charge Rate | Every part, summed | How much FE the line holds and how fast it takes it |
| Primed Charge Rate | Emitter | Extra Charge Rate, paid only while a Battery Cell sits in the Cradle's sockets |
| Hold Chance, Second Hold | Each Coil | Whether that Coil holds the pulse, and whether a slip there is rolled again |
| Overhold Chance | Each Coil, plus the Emitter | The chance a surplus hold triggers Overhold |
| Partial Window | The Emitter | How many Coils short of `R` still count as Partial |
| Strain Tolerance | Every part, summed | Strain that adds no Fracture chance |
| FE Thrift, FE Recovery | Every part, summed | A percentage off every pulse, and a percentage of a slipped pulse's FE returned to the charge |
| Glyph Salvage, Strain Mercy | Every part, summed | The chance a Glyph returns on a Miss, and the chance a pulse adds no Strain |

A Coil or the Emitter is itself a target: pull it out, refine it in the Cradle, and put it back. Pulling position 7 out drops the line to six Coils, so refine it in a second line or a friend's.
