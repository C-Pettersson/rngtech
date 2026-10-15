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
| Energy Transfer | Rate FE can move through storage: a Battery Cell's input rate and a Battery Chassis's transfer rating. Generators and processing machines have no FE limit of their own; the receiver or [connector tier](rngtech:universal-cable/energy.md) sets the rate. | Battery Cells, Battery Chassis. | Higher |
| Efficiency | More FE from the same fuel or recipe on generators, and less loss when a Battery Chassis or Cell charges and discharges. It does **not** cut the FE cost of processing recipes. | Generators, Battery Chassis, Battery Cells. | Higher |
| Idle Loss | Stored FE lost while sitting idle. | Battery Cells, Battery Chassis. | Lower |
| Battery Slots | Active Battery Cell slots. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
| Burst Transfer | Burst output rating, as a multiple of the transfer rating, on chassis with a burst identity. Output still cannot exceed what the installed cells deliver. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
| Burst Duration | Ticks a burst can last before it has to recover. | [Battery Chassis](rngtech:battery-chassis.md). | Higher |
