---
navigation:
  title: "Storing power"
  parent: battery-chassis.md
  position: 0
  icon: rngtech:iron_battery_chassis
---

# Storing power

Put Battery Cells in the chassis, then connect it to generators and machines directly or with a [Universal Cable](rngtech:universal-cable.md). The chassis accepts FE from any side and pushes stored FE out to adjacent blocks that accept it. It does not push FE back into a side that fed it on the same tick.

- **Capacity** is the total of the installed cells, multiplied by the chassis's capacity affixes. That bonus only applies while the cells stay in this chassis.
- **Input and output speed** come from the installed cells. Their combined charge and discharge rates are the limit, so a big chassis full of weak cells is still slow. Energy Transfer affixes on the chassis raise the rate of each cell while it stays in this chassis. These rates are per tick and shared by every side and connector, so adding sides or connectors does not raise them. An attached Universal Cable connector can cap it lower; while it runs at its cap and the cells could move more, the readout shows **Attached connector limits input energy** or **Attached connector limits output energy**.
- **Idle loss** slowly drains stored FE. Cells leak on their own, and some chassis add or remove leakage.
- **Efficiency** below 1.0 wastes FE: the chassis stores less than it accepts and draws more from its cells than it delivers. The Wooden, Copper, Gold, and Arclite chassis start below 1.0, and Efficiency affixes can raise it. Efficiency above 1.0 gives no bonus.
- **Global Modifier Strength** sets how strongly the chassis's Capacity and Energy Transfer affixes act on each cell. It starts at 1.5 on the Wooden chassis and falls to 0.7 on the Exotic chassis. For example, 50% increased Energy Transfer on an Iron chassis, which has 1.3 strength, raises each cell's rates by 65%. Charged Storage always doubles capacity.

Two Battery Chassis placed directly next to each other slowly even out their fill levels. Banks on the same cable network with connectors on Both also even out their charge and then stop, instead of draining each other, so daisy-chained banks pass charge along. See [storage balancing](rngtech:universal-cable/energy.md).
