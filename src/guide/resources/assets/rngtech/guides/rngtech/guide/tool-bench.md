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
  - rngtech:modular_pick
  - rngtech:modular_hammer
  - rngtech:modular_shovel
  - rngtech:modular_digger
  - rngtech:modular_axe
  - rngtech:modular_treefeller
---

# Tool Bench

<BlockImage id="rngtech:tool_bench" scale="6" />

- **Type:** Tool station
- **Power:** None (FE optional for charging tools)
- **Gear:** <ItemLink id="rngtech:tiny_anvil" />, <ItemLink id="rngtech:basic_energy_connector" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Tool Bench** is where you build and maintain RNGTech's **Modular Field Tools**: picks, hammers, shovels, diggers, axes, and treefellers made from a separate Tool Head and Tool Rod. The bench assembles tools, swaps their parts, repairs them, takes them apart, and refines the installed parts.

It needs no iron to craft, so it is one of the first things you build. Modular tools are also how you mine the harder RNGTech ores; vanilla pickaxes only reach the first two ore-hardness levels by default.

The placed bench faces you, with a bed of gravel between its legs. A tool in its Tool slot lies on the worktop, or the loose head and rod when there is no tool. An installed Tiny Anvil sits on the front corner of the worktop, and an installed Energy Connector adds a back panel with a cable port.

## Usage

- [Building a tool](rngtech:tool-bench/building-a-tool.md)
- [Ore hardness](rngtech:tool-bench/ore-hardness.md)
- [Tool behavior](rngtech:tool-bench/tool-behavior.md)

### Changing parts

Put a finished tool in the Tool slot, put a new head, rod, or Battery Cell in the matching Build slot, and press **Apply**. The old part returns to your inventory with its rolls intact. Press **Remove Battery Cell** to take out just the cell, or **Disassemble** to break the tool back into its head, rod, and cell.

- [Repairing](rngtech:tool-bench/repairing.md)
- [Diamond Tip](rngtech:tool-bench/diamond-tip.md)
- [Gear](rngtech:tool-bench/gear.md)

### Refining tool parts

The Refine tab applies an <ItemLink id="rngtech:affix_injector" />, <ItemLink id="rngtech:ascension_catalyst" />, or <ItemLink id="rngtech:ascension_matrix" /> to the Tool Head or Tool Rod inside the tool in the Tool slot. With no tool inserted, it works on a loose head or rod instead. Other refinement operations need the [Affix Forge](rngtech:affix-forge.md), where loose heads and rods are normal targets.

### Automation

The Tool Bench has no item automation. With an Energy Connector installed, it accepts FE from any side, for example from a [Universal Cable](rngtech:universal-cable.md). The cable port appears on the back of the bench once a connector is installed.

## Obtaining

### Crafting

<RecipesFor id="rngtech:tool_bench" />

### Breaking

Mine a placed Tool Bench with a pickaxe. It drops itself.

## More

- [Screen](rngtech:tool-bench/screen.md)
