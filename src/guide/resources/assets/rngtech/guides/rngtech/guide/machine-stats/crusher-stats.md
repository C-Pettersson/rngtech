---
navigation:
  title: "Crusher stats"
  parent: machine-stats.md
  position: 2
  icon: rngtech:iron_heat_core
---

# Crusher stats

These come from Crusher prefixes and start at 0.

| Stat | What it does | Better |
|---|---|---|
| Output Guard Grace | Ticks the Crusher keeps its progress while the output is blocked. | Higher |
| No-Cell Output Retention | How much of the output penalty for running without a Battery Cell you keep. | Higher |
| Hardness Energy Mitigation | Cuts the extra FE that high-hardness recipes cost. It does not reduce the recipe's base cost. | Higher |
| Crusher Input Filter | How strictly the top side filters automated input. The top side always rejects items that cannot be crushed; from tier 2 it also checks output space, then bonus-bank compatibility, then room for a full batch. Tier 1 adds nothing beyond that default. It never rejects recipes above the Crush Head's hardness. | Higher |
| Crusher Salvage | Chance to recover one extra base output item, on top of Bonus Output and Super Output. | Higher |
