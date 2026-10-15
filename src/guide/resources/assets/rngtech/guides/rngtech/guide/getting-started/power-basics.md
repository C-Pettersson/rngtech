---
navigation:
  title: "Power basics"
  parent: getting-started.md
  position: 5
  icon: rngtech:wooden_crusher_chassis
---

# Power basics

RNGTech machines run on **FE**. Generators push FE into neighbouring blocks, and every powered machine has a small internal buffer plus a Battery Cell slot.

- **Install a Battery Cell.** Most machines run slower or produce less on their internal buffer alone.
- **Store power.** A [Battery Chassis](rngtech:battery-chassis.md) holds several cells and smooths out gaps between generation and use.
- **Watch the FE per tick.** Each recipe costs a fixed amount of FE. A faster machine finishes sooner but draws more FE per tick, so speed alone does not save power.
- **Stage 5–8 materials cost more.** Recipes for those materials cost 2×, 3×, 4×, and 6× the early FE.

Typical generators by stage, before rolls and Gear bonuses:

| Stage | Generator | Output |
|---:|---|---|
| 1–4 | [Solid Fuel Burner](rngtech:solid-fuel-burner.md) | 24, 40, 64, or 96 FE/t, depending on the Heat Core |
| 1–5 | [Solar Panel](rngtech:solar-panel.md) | 4 to 20 FE/t per panel on a clear day |
| 2 | [Bio Generator](rngtech:bio-generator.md) | About 8 FE/t |
| 4+ | [Corrosion Cell](rngtech:corrosion-cell.md) | Depends on the plate or Anode |
| 5 | [Syngas Combustor](rngtech:syngas-combustor.md) | 240 FE/t |
| 6 | [Ammonia Fuel Cell](rngtech:ammonia-fuel-cell.md) | 600 FE/t |
| 7 | [Vacuum Collapse Generator](rngtech:vacuum-collapse-generator.md) | About 1,000 FE/t |

The [Potential Reactor](rngtech:potential-reactor.md) and [Cavitation Generator](rngtech:cavitation-generator.md) also generate FE. See each page for details, and [Power Chains](rngtech:power-chains.md) for how each generator connects to the machines that fuel it.
