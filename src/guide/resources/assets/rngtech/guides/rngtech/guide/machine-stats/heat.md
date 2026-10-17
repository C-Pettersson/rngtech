---
navigation:
  title: "Heat"
  parent: machine-stats.md
  position: 4
  icon: rngtech:iron_heat_core
---

# Heat

Heat machines must warm up to a recipe's temperature before they start, and can fail if they run too hot, too cold, or lose power. See [Heat](rngtech:heat.md) for how this plays out.

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Max Temperature | Highest temperature the machine can reach. Recipes above it are locked. Mostly comes from the installed Heat Core. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md), [Melter](rngtech:melter.md). | Higher |
| Heat Transfer | How hard the heater pushes heat in: faster warmup and recovery, and higher FE/t while heating. On the [Cavitation Generator](rngtech:cavitation-generator.md) it sheds heat strain. | Heat machines, Cavitation Generator. | Higher |
| Thermal Conductivity | How fast heat reaches the input, and leaks through the walls: faster crafting and higher FE/t, about the same FE per craft. | Heat machines. | Higher |
| Thermal Mass | How much heat the chamber stores: smaller load dips and less wobble, but slower warmup and recovery. | Heat machines. | Depends |
| Cooling Rate | How fast a heat machine sheds heat when it is too hot for its next recipe or slider setting. | Heat machines. | Higher |
| Heat Insulation | How well the walls hold heat in: less heat lost, so cheaper crafts. On a [Solid Fuel Burner](rngtech:solid-fuel-burner.md), below 1.0 some burn time leaks away as waste heat. | Heat machines, Solid Fuel Burner. | Higher |
| Temperature Stability | Precision of temperature control. Some recipes need a minimum; on Stage 4 and higher recipes, low values build failure strain instead. On the Cavitation Generator it reduces heat strain. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md), [Cavitation Generator](rngtech:cavitation-generator.md). | Higher |
| Overheat Tolerance | Widens each recipe's safe maximum before overheating builds failure strain. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md). | Higher |
