---
navigation:
  title: "Logistics basics"
  parent: getting-started.md
  position: 7
  icon: rngtech:wooden_crusher_chassis
---

# Logistics basics

Early machines share power by touching. Once you are past the first Furnace, build [Universal Cable](rngtech:universal-cable.md). One <ItemLink id="rngtech:cable" /> carries FE, fluids, and items. A <ItemLink id="rngtech:universal_connector" /> on the cable next to each machine decides what moves, which way, and how fast, through the connector modules you install in it.

- The <ItemLink id="rngtech:crude_energy_connector" /> and <ItemLink id="rngtech:crude_item_connector" /> are simple crafting recipes. Basic and higher Energy, Fluid, and Item Connectors are pressed in a Metal Press with a **Connector Mold**.
- Each module has a channel from 0 to 15, and connectors only exchange with others on the same channel.
- An Energy Connector's tier caps the FE it moves each tick, in both directions. Generators and most machines do not limit their own FE, so the connector tier is usually what limits a power line.
- A <ItemLink id="rngtech:wrench" /> turns individual cable links on and off and shows a connector's settings. A <ItemLink id="rngtech:configurator" /> copies one connector's settings onto others.
- [Fluid and Compressor Tanks](rngtech:compressor-tank.md) store fluids and gases.

Automation reaches a machine's process slots, not its installed Gear. Battery Cells are the common exception: many machines accept them through a side. Each machine page lists its sides under Automation. Factory chains are the intended way to autocraft; [Automation](rngtech:automation.md) explains how machines with several ingredients take them without jamming.
