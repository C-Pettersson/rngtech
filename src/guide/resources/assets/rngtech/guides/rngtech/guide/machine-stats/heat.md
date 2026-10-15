---
navigation:
  title: "Heat"
  parent: machine-stats.md
  position: 4
  icon: rngtech:iron_heat_core
---

# Heat

Heat machines must warm up to a recipe's target temperature before they start, and can fail if they run too hot, too cold, or lose power. See [Furnace](rngtech:furnace/smelting.md) for how this plays out.

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Max Temperature | Highest temperature the machine can reach. Recipes above it are locked. Mostly comes from the installed Heat Core. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md), [Melter](rngtech:melter.md). | Higher |
| Heat Transfer | How fast the machine heats up. Also shortens heat-processing time. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md). | Higher |
| Warmup Time | Time to reach target heat. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md). | Lower |
| Cooling Rate | How fast heat drains while the machine is blocked, idle, unpowered, or too hot for its next recipe. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md). | Lower |
| Heat Insulation | How well heat is kept. Slows cooling on heat machines. On a [Solid Fuel Burner](rngtech:solid-fuel-burner.md), below 1.0 some burn time leaks away as waste heat. | Heat machines, Solid Fuel Burner. | Higher |
| Temperature Stability | Precision of temperature control. Some recipes need a minimum; on Stage 4 and higher recipes, low values build failure strain instead. On the Cavitation Generator it reduces heat strain. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md), [Cavitation Generator](rngtech:cavitation-generator.md). | Higher |
| Overheat Tolerance | Widens each recipe's safe maximum before overheating builds failure strain. | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Metal Press](rngtech:metal-press.md). | Higher |
