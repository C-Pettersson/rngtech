---
navigation:
  title: "Phase Array line"
  parent: machine-stats.md
  position: 13
  icon: rngtech:iron_heat_core
---

# Phase Array line

The [Phase Array](rngtech:phase-array.md) reads these from its Emitter and Coils. None of them touch a processing machine.

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Charge Capacity | FE the line holds for one pulse, summed over the Emitter and every Coil. | Phase Emitter, Phase Coil. | Higher |
| Charge Rate | FE/t the line takes from supply and from the Cradle's cells, summed over its parts. | Phase Emitter, Phase Coil. | Higher |
| Hold Chance | The chance this Coil holds the pulse. Base 92%. | Phase Coil. | Higher |
| Second Hold | The chance a slip in this Coil is rolled again. | Phase Coil. | Higher |
| Overhold Chance | The chance a surplus hold in this Coil triggers Overhold. Base 8%, plus the Emitter's. | Phase Emitter, Phase Coil. | Higher |
| Partial Window | How many Coils short of the operation's count still count as Partial. Base 2. | Phase Emitter. | Higher |
| Strain Tolerance | Strain points that add no Fracture chance, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| FE Thrift | Percent off every pulse's FE, summed over the line, up to 50%. | Phase Emitter, Phase Coil. | Higher |
| FE Recovery | Percent of a slipped pulse's FE returned to the charge, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| Glyph Salvage | The chance the Glyph returns on a Miss, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| Strain Mercy | The chance a pulse adds no Strain, summed over the line. | Phase Emitter, Phase Coil. | Higher |
