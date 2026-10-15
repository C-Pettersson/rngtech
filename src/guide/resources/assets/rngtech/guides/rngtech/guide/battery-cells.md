---
navigation:
  title: "Battery Cells"
  parent: guides.md
  icon: rngtech:iron_battery_cell
categories:
  - "Guides"
item_ids:
  - rngtech:potato_battery_cell
  - rngtech:iron_battery_cell
  - rngtech:copper_battery_cell
  - rngtech:lead_battery_cell
  - rngtech:invar_battery_cell
  - rngtech:sparksteel_battery_cell
  - rngtech:arclite_battery_cell
  - rngtech:nullite_battery_cell
  - rngtech:aethergold_battery_cell
  - rngtech:exotic_battery_cell
---

# Battery Cells

**Battery Cells** are rechargeable [Gear](rngtech:gear.md) that store FE. You install them in a machine's Gear tab to give it a power reserve, or fill a [Battery Chassis](rngtech:battery-chassis.md) with them to build an energy bank.

Each cell is its own item with its own [rarity and affixes](rngtech:rarity-and-affixes.md), so cells do not stack. A cell keeps its stored FE when you take it out, and its durability bar shows how full it is. Cells can be charged by anything that charges FE items.

## Cell tiers

Each tier stores more and moves FE faster. Values are before affixes.

| Cell | Stage | Capacity | Input | Output | Idle loss | Identity |
|---|---:|---:|---:|---:|---:|---|
| <ItemLink id="rngtech:potato_battery_cell" /> | 0 | 1,000 FE | 8 FE/t | 4 FE/t | 12%/min | Spud Cell |
| <ItemLink id="rngtech:iron_battery_cell" /> | 1 | 10,000 FE | 128 FE/t | 64 FE/t | 0 | Iron Cell |
| <ItemLink id="rngtech:copper_battery_cell" /> | 2 | 12,000 FE | 256 FE/t | 128 FE/t | 0.01%/min | Conductive Cell |
| <ItemLink id="rngtech:lead_battery_cell" /> | 3 | 32,000 FE | 256 FE/t | 128 FE/t | 0 | Dense Cell |
| <ItemLink id="rngtech:invar_battery_cell" /> | 4 | 50,000 FE | 512 FE/t | 256 FE/t | 0 | Insulated Cell |
| <ItemLink id="rngtech:sparksteel_battery_cell" /> | 5 | 100,000 FE | 1,024 FE/t | 512 FE/t | 0.01%/min | Balanced Cell |
| <ItemLink id="rngtech:arclite_battery_cell" /> | 6 | 400,000 FE | 2,048 FE/t | 1,024 FE/t | 0.02%/min | Signal Cell |
| <ItemLink id="rngtech:nullite_battery_cell" /> | 7 | 1,600,000 FE | 4,096 FE/t | 2,048 FE/t | 0.02%/min | Phase Cell |
| <ItemLink id="rngtech:aethergold_battery_cell" /> | 7 | 1,200,000 FE | 6,144 FE/t | 3,072 FE/t | 0 | Aetherburst Cell |
| <ItemLink id="rngtech:exotic_battery_cell" /> | 8 | 10,000,000 FE | 8,192 FE/t | 4,096 FE/t | 0 | Harmonic Cell |

- **Input** is how fast the cell can be charged, and **Output** is how fast it can give power back. Cells always charge twice as fast as they discharge.
- **Idle loss** is slow leakage. It only drains cells sitting in a Battery Chassis.
- The Aethergold Cell is a sidegrade to Nullite: less capacity, but faster transfer and no leakage.
- The Potato Cell is a novelty starter with heavy leakage.
- **Identity** is the cell's fixed material trait. Hold Left Shift over a cell to see it in the tooltip. It is a name only and changes nothing beyond the cell's base values.

- [Unique cells](rngtech:battery-cells/unique-cells.md)

## Cells in machines

Most powered machines have one Battery Cell slot on their Gear tab. FE you feed the machine fills its small internal buffer first, then charges the cell. While working, the machine spends its buffer first, then draws from the cell.

The cell's rates still apply inside a machine. It accepts at most its Input rate each tick and hands over at most its Output rate each tick. When a machine is running on stored power alone, a low-tier cell can limit how fast it works.

Solid Fuel Burners are the exception: they write the FE they generate straight into the cell, and the cell's rates do not limit their output.

- [Running without a cell](rngtech:battery-cells/running-without-a-cell.md)
- [Which cells fit where](rngtech:battery-cells/which-cells-fit-where.md)

## Making cells

- [Early cells](rngtech:battery-cells/early-cells.md)
- [Assembled cells](rngtech:battery-cells/assembled-cells.md)

## More

- [Affixes](rngtech:battery-cells/affixes.md)
- [Cells in a Battery Chassis](rngtech:battery-cells/cells-in-a-battery-chassis.md)
