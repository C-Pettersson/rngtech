---
wiki:
  category: Tools
  icon: rngtech:tool_bench
  ids:
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

The **Tool Bench** is where you build and maintain RNGTech's **Modular Field Tools**: picks, hammers, shovels, diggers, axes, and treefellers made from a separate Tool Head and Tool Rod. The bench assembles tools, swaps their parts, repairs them, takes them apart, and refines the installed parts.

It needs no iron to craft, so it is one of the first things you build. Modular tools are also how you mine the harder RNGTech ores; vanilla pickaxes only reach the first two ore-hardness levels by default.

## Obtaining

### Crafting

{{ crafting("rngtech:tool_bench") }}

### Breaking

Mine a placed Tool Bench with a pickaxe. It drops itself.

## Usage

### Building a tool

A Modular Field Tool is made from:

- a **Tool Head**, which decides the tool type and material, such as a {{ item('rngtech:flint_pick_head') }}.
- a **Tool Rod**, which sets durability, speed adjustments, FE transfer, and which Battery Cells the tool supports, such as a {{ item('rngtech:wooden_tool_rod') }}.
- an optional **Battery Cell**, which pays the FE for tool features that use it.

Heads and rods are crafted at a crafting table, and each one rolls its own rarity and affixes. They roll 2 more Refinement Potential than other parts of the same stage, and land higher affix tiers a little more often. The assembled tool has no affixes of its own; it uses whatever its installed parts have.

{{ crafting("rngtech:flint_pick_head", "rngtech:wooden_tool_rod") }}

To build a tool, leave the Tool slot empty, put a head, a rod, and optionally a Battery Cell in the Build tab, and press **Apply**. The head's type picks the tool:

| Head family | Tool | Breaks |
|---|---|---|
| Pick | {{ item('rngtech:modular_pick') }} | One pickaxe block. |
| Hammer | {{ item('rngtech:modular_hammer') }} | A 3×3 area of pickaxe blocks. Needs a Tiny Anvil. |
| Shovel | {{ item('rngtech:modular_shovel') }} | One shovel block. |
| Digger | {{ item('rngtech:modular_digger') }} | A 3×3 area of shovel blocks, such as dirt, gravel, and sand. Needs a Tiny Anvil. |
| Axe | {{ item('rngtech:modular_axe') }} | One axe block. |
| Treefeller | {{ item('rngtech:modular_treefeller') }} | A whole tree of connected logs, up to its limit. Needs a Tiny Anvil. |

### Ore hardness

RNGTech ores have a **hardness** from 1 to 8, separate from vanilla mining tiers. A Modular Pick or Hammer mines ores up to its head's hardness, and a head reaches every hardness below its own. Ore that is too hard drops nothing. A Hammer skips any block in its area that is too hard for it, without spending durability or FE on it.

| Head material | Stage | Ore hardness | Opens up |
|---|---:|---:|---|
| Flint | 0 | 1 | Iron, Copper |
| Iron | 1 | 1 | |
| Copper | 2 | 2 | Tin, Zinc, Gold |
| Bronze | 3 | 2 | |
| Steel | 4 | 3 | Nickel, Lead, Silver |
| Steel with a {{ item('rngtech:diamond_tip') }} | 4 | 4 | Aluminum, Osmium |
| Aluminum | 5 | 5 | Titanium |
| Titanium | 6 | 6 | Tungsten, Platinum |
| Tungstensteel | 7 | 7 | None by default |
| Nullite | 7 | 7 | None by default |
| Exotic | 8 | 8 | Naquadah |

By default, a vanilla stone pickaxe reaches hardness 1 and an iron or better pickaxe reaches hardness 2. Beyond that you need Modular tools. Modpacks can change both rules.

### Tool behavior

- **Durability and FE** are spent per block actually broken, so a Hammer or Digger that hits fewer blocks costs less.
- **Stability** above 1.0 gives each broken block a chance to cost no durability and no FE. 1.10 Stability is a 10% chance.
- **Control** above 1.0 moves part of each durability point onto the installed Battery Cell as FE. 1.60 Control pays 60% of the wear in FE, up to 100% at 2.0. The full FE price per durability point is 16 FE for a Pick or Axe, 12 FE for a Shovel, 32 FE for a Hammer, and 28 FE for a Digger or Treefeller, scaled down by the share Control covers. If the cell cannot pay, the tool takes full durability wear for that block. Stability is checked first.
- **Self Repair** heads and rods restore 3 to 8 durability, depending on the affix tier, every 7.5 seconds while the tool sits idle in your hotbar or offhand. A head and rod that both have it add together. Mining pauses the repair.
- **Battery Cells** in your inventory recharge the tool's installed cell, first inventory slot first, up to the tool's FE transfer rate. Only cells within the rod's Battery Support count, the same limit that decides which cells you can install.
- **Vein Miner**, a rare Pick Head affix, breaks connected ore of the same type, up to the Vein Mine Limit. Each block after the first costs extra FE; when the cell cannot pay it, the rest of the vein stays. Sneak while mining to break only the block you aim at.
- **Ore Burst** speeds up mining on ore for Picks and Hammers. It needs the Ore Burst suffix on a part and an Ore Burst material: a Copper, Nullite, or Exotic head, or a {{ item('rngtech:copper_conduit_tool_rod') }}, {{ item('rngtech:sparksteel_routed_tool_rod') }}, {{ item('rngtech:nullite_phase_tool_rod') }}, or {{ item('rngtech:exotic_harmonic_tool_rod') }}. Each ore it breaks costs extra FE, and without enough FE the ore mines at normal speed.
- **Luck** can add extra drops from ores and logs broken by the tool. Each point, up to 5, gives every drop a 15% chance of one extra copy. It does nothing with Silk Touch, and nothing for machines, chests, or mobs.

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

The Tool Bench has no item automation. With an Energy Connector installed, it accepts FE from any side, for example from a [Universal Cable](universal-cable.md).

## Screen

The Tool Bench screen has four tabs:

- **Build**: Tool, head, rod, Battery Cell, and repair slots, with the Apply, Remove Battery Cell, and Disassemble buttons.
- **Gear**: the Tiny Anvil and Energy Connector slots.
- **Stats**: the inserted tool's effective stats. Hover a stat for an explanation.
- **Refine**: Tool, Head, Rod, and catalyst slots for refining tool parts.

## Data values

{{ data_values() }}

## See also

- [Affix Forge](affix-forge.md)
- [Forestry Cart Station](forestry-cart-station.md), whose cart uses a modular Axe or Treefeller.

{{ navbox() }}
