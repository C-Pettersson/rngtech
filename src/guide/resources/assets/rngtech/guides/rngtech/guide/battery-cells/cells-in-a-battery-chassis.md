---
navigation:
  title: "Cells in a Battery Chassis"
  parent: battery-cells.md
  position: 4
  icon: rngtech:iron_battery_cell
---

# Cells in a Battery Chassis

A [Battery Chassis](rngtech:battery-chassis.md) holds several cells and turns them into one energy bank:

- Its capacity is the total of its cells, raised by the chassis's capacity affixes. That bonus only applies while the cells stay in that chassis.
- Its charge and discharge speed is the **combined** Input and Output of the cells that hold power. One Iron Cell gives a 64 FE/t bank no matter how big the chassis is. Four give 256 FE/t. That rate is per tick and shared by every side and connector of the chassis.
- Cells leak their idle loss here, and some chassis add, reduce, or seal leakage.

Fill a chassis with enough cells to reach the transfer rate you need, not just the capacity.
