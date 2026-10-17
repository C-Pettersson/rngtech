---
navigation:
  title: "Collapse depth"
  parent: vacuum-collapse-generator.md
  position: 2
  icon: rngtech:vacuum_collapse_generator
---

# Collapse depth

The depth gauge under the status icons sets how deep each collapse goes, from 50% to 150%. Click or scroll on it (hold Shift to step by 5). A collapse keeps the depth it started with, so a change applies from the next catalyst.

Deeper collapses draw more FE from each catalyst, but they take longer, peak harder, and multiply the recipe's instability. Shallow collapses spend catalysts faster for a flatter, gentler curve. Depth hardly changes the average FE/t, so the choice is catalysts against how hard a burst your network can take.

| Depth | FE per catalyst | Cycle | Instability | Start | Peak |
|---|---:|---:|---:|---:|---:|
| 50% (shallow) | ×0.66 | ×0.71 | ×0.25 | 0.7× average | 1.3× average |
| 75% | ×0.84 | ×0.87 | ×0.56 | 0.55× average | 1.65× average |
| 100% (rated, default) | ×1.00 | ×1.00 | ×1.00 | 0.4× average | 2× average |
| 125% | ×1.14 | ×1.12 | ×1.56 | 0.25× average | 2.35× average |
| 150% (deep) | ×1.28 | ×1.22 | ×2.25 | 0.1× average | 2.7× average |

Better stability takes the edge off deep collapses, since stability divides the recipe's instability after depth multiplies it. Hover the gauge to see the effects of the current setting.
