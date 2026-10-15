---
navigation:
  title: "Generating power"
  parent: solid-fuel-burner.md
  position: 0
  icon: rngtech:crude_solid_fuel_burner
---

# Generating power

A burner needs all three Gear parts before it burns anything: a **Heat Core**, a **Fuel Box**, and a **Battery Cell**. Install them on the Gear tab, then put fuel in the fuel slot on the Process tab.

The burner writes generated FE straight into the installed Battery Cell and pushes it out to adjacent blocks that accept FE, such as a [Universal Cable](rngtech:universal-cable.md) or a [Battery Chassis](rngtech:battery-chassis.md). The burner does not limit its own output, so the receiver or cable tier decides how much FE moves each tick. It never accepts FE from outside.

Each fuel item holds a fixed amount of energy: 10 FE per tick of its furnace burn time. One Coal holds 16,000 FE before fuel stats. A stronger Heat Core does not get more FE from that Coal. It spends the same Coal faster, at a higher FE/t. Fuel efficiency and fuel duration stats are what make each item worth more. For example, a Steel Heat Core in a Steel Fuel Box gets about 20,000 FE from one Coal and burns through it in about 10 seconds.

Logs are accepted but burn for only a quarter of their furnace burn time, so turn them into planks first.
