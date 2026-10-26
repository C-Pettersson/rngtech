---
navigation:
  title: "Component Assembler"
  parent: processing.md
  icon: rngtech:battery_assembler
categories:
  - "Processing"
item_ids:
  - rngtech:battery_assembler
  - rngtech:battery_cell
  - rngtech:lubricated_frame_coupling
---

# Component Assembler

<BlockImage id="rngtech:battery_assembler" scale="6" />

- **Type:** Processing machine
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:iron_battery_cell" /> (optional)
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Component Assembler** is a powered workbench that combines items with a fluid. It makes <ItemLink id="rngtech:battery_cell" />s and the material Battery Cells built from them, Corrosion Cell Anodes, late-game Servos and frame parts that need Lubricant, the <ItemLink id="rngtech:biogas_membrane" />, and **Void Catalyst**.

It started life as the Battery Assembler, which is why its resource id is still `battery_assembler`. Each Component Assembler you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

You can read an Assembler from the outside. Each input slot is a pocket along the front of the bed, and its item lies in it, in slot order from left to right. While the Assembler works, the head on the gantry picks up the item from each filled pocket in turn and carries it to the nest behind them. A pocket stays filled if its slot holds more than the job takes. When the recipe takes fluid, the head ends the job by pouring it over the items on the nest. The puddle soaks in and leaves the finished part. The output rack is the row of four cubbies in the back cabinet, and finished items stand in them. The assembly fluid shows in the glass reservoir on the roof, and the Battery Cell lies beside it, with a charge meter on the left side of the cabinet. FE and fluid still enter on any side, but the cable port is drawn on the back.

## Usage

- [Assembling](rngtech:component-assembler/assembling.md)
- [Battery Cells](rngtech:component-assembler/battery-cells.md)
- [Other components](rngtech:component-assembler/other-components.md)

### Gear

The Gear tab has one optional **Battery Cell** slot. FE you supply fills the machine's internal buffer first, then charges the cell. While running, it spends the buffer first and then draws from the cell. Without a cell, it runs on its internal buffer alone.

- [Automation](rngtech:component-assembler/automation.md)

## Obtaining

### Crafting

<RecipesFor id="rngtech:battery_assembler" />

- Requires the Iron material to be enabled.
- Requires the Copper material to be enabled.

### Breaking

Mine a placed Component Assembler with a pickaxe. It drops itself and keeps its rolled traits.

## More

- [Screen](rngtech:component-assembler/screen.md)
