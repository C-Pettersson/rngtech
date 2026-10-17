---
navigation:
  title: "Nitrogen atmosphere"
  parent: heat.md
  position: 2
  icon: rngtech:steel_heat_core
---

# Nitrogen atmosphere

The electric [Furnace](rngtech:furnace.md), the [Alloy Furnace](rngtech:alloy-furnace.md), and the [Metal Press](rngtech:metal-press.md) each have a Nitrogen tank that holds 4,000 mB. Pipe in **Nitrogen Bucket** Nitrogen from any side, for example from a [Cavitation Generator](rngtech:cavitation-generator.md) running the Nitrogen rotor and nozzle. The gauge sits at the right edge of the Process tab.

- **With Blast Draft**, Nitrogen in the tank gives 15% increased Overheat Tolerance, so the overheat temperature sits further above the safe maximum. For a 1,000 °C tin smelt with a 1,150 °C safe maximum, the line moves from 1,150 °C to 1,323 °C. That is more room before wobble spikes build failure strain, and a recipe that cannot fail lets the slider go further, so it crafts faster.
- **Cost.** A lane uses 1 mB of Nitrogen per tick, but only while it works hotter than its overheat temperature would be without Nitrogen. A lane at or below that line uses none, and a 4-lane Lead Furnace running hot uses up to 4 mB a tick.
- **Without Blast Draft** the Nitrogen does nothing and stays in the tank.
- When the tank runs dry, the overheat temperature drops back at once. A smelt that can fail keeps its setpoint and starts building strain, so keep the tank fed or lower the slider. A recipe that cannot fail stops and cools to its old limit first, the same as lowering the slider.

The gauge's rim lights up while a lane is using Nitrogen, and its hover shows the amount, whether the atmosphere is active, and the Nitrogen used per tick. Every mB here is Nitrogen your [Ammonia](rngtech:power-chains/stage-6-ammonia.md) chain does not get.
