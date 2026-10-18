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

The **Component Assembler** is a powered workbench that combines items with a fluid. It makes <ItemLink id="rngtech:battery_cell" />s and the material Battery Cells built from them, Corrosion Cell Anodes, late-game Servos and frame parts that need Lubricant, and **Void Catalyst**.

It started life as the Battery Assembler, which is why its resource id is still `battery_assembler`. Each Component Assembler you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Assembling](rngtech:component-assembler/assembling.md)
- [Battery Cells](rngtech:component-assembler/battery-cells.md)
- [Other components](rngtech:component-assembler/other-components.md)

### Gear

The Gear tab has one optional **Battery Cell** slot. FE you supply fills the machine's internal buffer first, then charges the cell. While running, it spends the buffer first and then draws from the cell. Without a cell, it runs on its internal buffer alone.

- [Automation](rngtech:component-assembler/automation.md)

## Screen

The Component Assembler screen has four tabs:

- **Process**: four item inputs, the fluid slot and tank with its purge button, the output rack, progress, and energy. Hover the progress bar for time remaining and expected items per minute.
- **Gear**: the Battery Cell slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Assembler's traits with a catalyst.

## Obtaining

### Crafting

<RecipesFor id="rngtech:battery_assembler" />

- Requires the Iron material to be enabled.
- Requires the Copper material to be enabled.

### Breaking

Mine a placed Component Assembler with a pickaxe. It drops itself and keeps its rolled traits.
