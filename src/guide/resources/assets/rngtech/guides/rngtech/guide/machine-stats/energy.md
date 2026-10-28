---
navigation:
  title: "Energy"
  parent: machine-stats.md
  position: 3
  icon: rngtech:iron_heat_core
---

# Energy

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Energy Capacity | How much FE the machine can store in its own buffer. Processing machines keep only a small buffer; a [Battery Cell](rngtech:battery-cells.md) gives real storage. On a [Battery Chassis](rngtech:battery-chassis.md) it scales the installed cells while they stay inside. | Every FE machine, Battery Chassis, and Battery Cells. | Higher |
| Flat Energy Capacity | Fixed FE added to the buffer before percentage bonuses. | Machines and Battery Cells that roll a flat capacity prefix. | Higher |
| Energy Transfer | Rate FE can move through storage: a Battery Cell's input rate, and on a Battery Chassis a raise to each installed cell's rates. Generators and processing machines have no FE limit of their own; the receiver or [connector tier](rngtech:universal-cable/energy.md) sets the rate. | Battery Cells, Battery Chassis. | Higher |
| Efficiency | More FE from the same fuel or recipe on generators, and less loss when a Battery Chassis charges and discharges. It does **not** cut the FE cost of processing recipes. | Generators, Battery Chassis. | Higher |
| Idle Loss | Stored FE lost while sitting idle. | Battery Cells, Battery Chassis. | Lower |
| Battery Slots | Active Battery Cell slots. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
| Burst Transfer | Output during a [burst](rngtech:battery-chassis/burst.md), as a multiple of the installed cells' rated output. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
| Burst Duration | Ticks of burst the window holds. It refills one tick for each tick without a burst. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
| Global Modifier Strength | How strongly the chassis's Capacity and Energy Transfer affixes act on each installed cell. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
