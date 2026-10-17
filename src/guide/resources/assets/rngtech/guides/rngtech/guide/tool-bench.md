---
navigation:
  title: "Tool Bench"
  parent: tools.md
  icon: rngtech:tool_bench
categories:
  - "Tools"
item_ids:
  - rngtech:tool_bench
  - rngtech:tiny_anvil
  - rngtech:diamond_tip
---

# Tool Bench

<BlockImage id="rngtech:tool_bench" scale="6" />

- **Type:** Tool station
- **Power:** None (FE optional for charging tools)
- **Gear:** <ItemLink id="rngtech:tiny_anvil" />, <ItemLink id="rngtech:basic_energy_connector" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Tool Bench** is where you build and maintain RNGTech's [Modular Tools](rngtech:modular-tools.md): picks, hammers, shovels, diggers, axes, and treefellers made from a separate Tool Head and Tool Rod. The bench assembles tools, swaps their parts, repairs them, takes them apart, and refines the installed parts.

It needs no iron to craft, so it is one of the first things you build.

## Usage

### Building a tool

To build a tool, leave the Tool slot empty, put a [Tool Head](rngtech:modular-tools/parts.md), a Tool Rod, and optionally a Battery Cell in the Build tab, and press **Apply**. The head's type picks the tool; see [Tool types](rngtech:modular-tools.md#tool-types). A Hammer, Digger, or Treefeller also needs a Tiny Anvil in the [Gear](rngtech:tool-bench/gear.md) tab.

### Changing parts

Put a finished tool in the Tool slot, put a new head, rod, or Battery Cell in the matching Build slot, and press **Apply**. The old part returns to your inventory with its rolls intact. Press **Remove Battery Cell** to take out just the cell, or **Disassemble** to break the tool back into its head, rod, and cell.

- [Repairing](rngtech:tool-bench/repairing.md)
- [Diamond Tip](rngtech:tool-bench/diamond-tip.md)
- [Gear](rngtech:tool-bench/gear.md)

### Refining tool parts

The Refine tab applies an <ItemLink id="rngtech:affix_injector" />, <ItemLink id="rngtech:ascension_catalyst" />, or <ItemLink id="rngtech:ascension_matrix" /> to the Tool Head or Tool Rod inside the tool in the Tool slot. With no tool inserted, it works on a loose head or rod instead. Other refinement operations need the [Affix Forge](rngtech:affix-forge.md), where loose heads and rods are normal targets.

### Automation

The Tool Bench has no item automation. With an Energy Connector installed, it accepts FE from any side, for example from a [Universal Cable](rngtech:universal-cable.md).

## Screen

The Tool Bench screen has four tabs:

- **Build**: Tool, head, rod, Battery Cell, and repair slots, with the Apply, Remove Battery Cell, and Disassemble buttons.
- **Gear**: the Tiny Anvil and Energy Connector slots.
- **Stats**: the inserted tool's effective stats. Hover a stat for an explanation.
- **Refine**: Tool, Head, Rod, and catalyst slots for refining tool parts.

## Obtaining

- [Crafting](rngtech:tool-bench/crafting.md)

### Breaking

Mine a placed Tool Bench with a pickaxe. It drops itself.
