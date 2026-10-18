---
navigation:
  title: "How a lane heats"
  parent: heat.md
  position: 0
  icon: rngtech:steel_heat_core
---

# How a lane heats

- **Warming up.** A cold lane heats toward its setpoint. Heat Transfer sets how hard the heater pushes, and Thermal Mass sets how much heat the chamber has to take before its temperature moves. Unrolled gear climbs about 100 °C a second, so an Iron Furnace takes about 10 seconds to reach 800 °C; the fastest Heat Cores still take a few seconds.
- **Starting.** A cycle starts once the lane reaches the recipe's minimum temperature. Most recipes have no separate minimum, so they start at the target. A cold batch takes some heat from the chamber when it goes in. The lane never drops below the minimum because of it, and a bigger batch takes a bigger dip.
- **Working.** Below the target the input takes heat more slowly; above it, faster. The heat bar's hover shows this as the **heat factor**: ×1 at the target, ×1.3 at the default safe maximum. Thermal Conductivity and Processing Speed multiply it.
- **Short of power.** When FE or fuel cannot keep up, the input takes only the heat left after the walls, so crafting slows down and the status reads **Underpowered**. The lane keeps its temperature. Hot recipes need a lot of heat, so a weak supply crawls on them.
- **Output full.** A lane whose output has no room, for example after a failed batch, holds its setpoint and pays the wall loss, so it resumes at once when you clear the output.
- **Idle.** With nothing to work on, the heater turns off and the walls slowly leak the heat away. Heat Insulation and Thermal Mass keep it longer.
- **Too hot.** A lane hotter than its next recipe allows, after a hotter recipe or when you lower the slider, stops and cools first. The status reads **Too Hot**. Cooling Rate makes that wait shorter. With the **Cool Headed** keystone, every new input waits until the lane has cooled to the recipe target, so a cycle never starts on leftover or pushed heat.

Recipes with no temperature requirement, such as food, work at any heat at their normal speed, and a lane still hot from an earlier recipe pays for them with that leftover heat.

Heat loss grows quickly with temperature, so running hotter is faster but costs more FE per craft. Every craft pays at least its recipe's heat; JEI and the recipe tables list it in heat units (HU). Hotter recipes need more heat for the same input, as real metal does: a 1,450 °C smelt takes about 1.6 times the heat of an 800 °C one, and a 2,000 °C smelt about 2.1 times. On an electric machine each HU costs 1 FE before Energy Usage.
