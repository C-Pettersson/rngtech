---
wiki:
  category: Tools
  icon: rngtech:tool_bench
  ids:
    - rngtech:tool_bench
    - rngtech:tiny_anvil
    - rngtech:diamond_tip
---

# Tool Bench

{{ infobox(
    variants=[
        ["Bench", "rngtech:tool_bench"],
    ],
    fields={
        "Type": "Tool station",
        "Power": "None (FE optional for charging tools)",
        "Gear": "{{ item('rngtech:tiny_anvil') }}, {{ item('rngtech:basic_energy_connector', 'Energy Connector') }}",
        "Mastery": "No",
    },
) }}

The **Tool Bench** is where you build and maintain RNGTech's [Modular Tools](modular-tools.md): picks, hammers, shovels, diggers, axes, and treefellers made from a separate Tool Head and Tool Rod. The bench assembles tools, swaps their parts, repairs them, takes them apart, and refines the installed parts.

It needs no iron to craft, so it is one of the first things you build.

The placed bench faces you, with a bed of gravel between its legs. A tool in its Tool slot lies on the worktop, or the loose head and rod when there is no tool. An installed Tiny Anvil sits on the front corner of the worktop, and an installed Energy Connector adds a back panel with a cable port.

## Obtaining

### Crafting

{{ crafting("rngtech:tool_bench") }}

### Breaking

Mine a placed Tool Bench with a pickaxe. It drops itself.

## Usage

### Building a tool

To build a tool, leave the Tool slot empty, put a [Tool Head](modular-tools.md#parts), a Tool Rod, and optionally a Battery Cell in the Build tab, and press **Apply**. The head's type picks the tool; see [Tool types](modular-tools.md#tool-types). A Hammer, Digger, or Treefeller also needs a Tiny Anvil in the [Gear](#gear) tab.

### Changing parts

Put a finished tool in the Tool slot, put a new head, rod, or Battery Cell in the matching Build slot, and press **Apply**. The old part returns to your inventory with its rolls intact. Press **Remove Battery Cell** to take out just the cell, or **Disassemble** to break the tool back into its head, rod, and cell.

### Repairing

A tool at 0 durability stays in your inventory but stops working until you repair it. Put the head's material in the repair slot (the matching ingot, or flint for Flint heads) and press **Apply**. Each material restores up to a quarter of the tool's durability. You can also repair in a vanilla anvil with the same material. Combining two tools in a crafting grid does not work, because it would destroy their parts.

### Diamond Tip

A {{ item('rngtech:diamond_tip') }} in the repair slot upgrades a Steel Pick Head or Steel Hammer Head, loose or installed, from ore-hardness 3 to 4. This lets you mine Aluminum and Osmium ores before you can craft an Aluminum head. The tip is used up.

{{ crafting("rngtech:diamond_tip") }}

### Gear

The Gear tab has:

- one **Tiny Anvil** slot. A {{ item('rngtech:tiny_anvil') }} is needed to build a Hammer, Digger, or Treefeller, or to swap one of their heads. It is never used up.
- one **Energy Connector** slot. With a connector installed, the bench accepts FE and charges the Battery Cell of the tool in its Tool slot, up to the connector's rate or the tool's FE transfer rate, whichever is lower. It can also charge a {{ item('rngtech:miners_companion') }} placed in the Tool slot.

{{ crafting("rngtech:tiny_anvil") }}

### Refining tool parts

The Refine tab applies an {{ item('rngtech:affix_injector') }}, {{ item('rngtech:ascension_catalyst') }}, or {{ item('rngtech:ascension_matrix') }} to the Tool Head or Tool Rod inside the tool in the Tool slot. With no tool inserted, it works on a loose head or rod instead. Other refinement operations need the [Affix Forge](affix-forge.md), where loose heads and rods are normal targets.

### Automation

The Tool Bench has no item automation. With an Energy Connector installed, it accepts FE from any side, for example from a [Universal Cable](universal-cable.md). The cable port appears on the back of the bench once a connector is installed.

## Screen

The Tool Bench screen has four tabs:

- **Build**: Tool, head, rod, Battery Cell, and repair slots, with the Apply, Remove Battery Cell, and Disassemble buttons.
- **Gear**: the Tiny Anvil and Energy Connector slots.
- **Stats**: the inserted tool's effective stats. Hover a stat for an explanation.
- **Refine**: Tool, Head, Rod, and catalyst slots for refining tool parts.

Hover an empty slot to see what it takes.

## Data values

{{ data_values() }}

## See also

- [Modular Tools](modular-tools.md), for tool types, ore hardness, and tool behavior.
- [Affix Forge](affix-forge.md)
- [Forestry Cart Station](forestry-cart-station.md), whose cart uses a modular Axe or Treefeller.

{{ navbox() }}
