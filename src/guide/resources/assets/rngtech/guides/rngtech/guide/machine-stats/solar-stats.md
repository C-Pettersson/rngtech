---
navigation:
  title: "Solar stats"
  parent: machine-stats.md
  position: 6
  icon: rngtech:iron_heat_core
---

# Solar stats

| Stat | What it does | Used by |
|---|---|---|
| Peak Solar Generation | Boosts a panel's output during the midday window (time 4000 to 8000). The Peak Solar suffix raises it by 50–100%. | Solar Panels |
| Array Range | How far the Array Controller reaches to collect panels, up to 2. | Solar Array Controller |
| Moonlight Conversion | Turns part of the array's solar output into night output. | Solar Array Controller |
| Lunar Inversion | Stronger night output, at a penalty on clear days. | Solar Array Controller |
| Weather Recovery | Restores part of the output lost to rain and storms. | Solar Array Controller |
| Clear Sky Amplification | Bonus output in clear daylight. | Solar Array Controller |
| Panel Synchronization | Bonus output when every panel in the array is the same material. | Solar Array Controller |
| Panel Arbitration | Picks the highest-output panels when the array mixes materials. | Solar Array Controller |

All of these are better higher. See [Solar Panel](rngtech:solar-panel.md).
