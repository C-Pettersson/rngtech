---
wiki:
  category: Tools
  icon: rngtech:modular_pick
  ids:
    - rngtech:modular_pick
    - rngtech:modular_hammer
    - rngtech:modular_shovel
    - rngtech:modular_digger
    - rngtech:modular_axe
    - rngtech:modular_treefeller
    - rngtech:flint_pick_head
    - rngtech:iron_pick_head
    - rngtech:copper_pick_head
    - rngtech:bronze_pick_head
    - rngtech:steel_pick_head
    - rngtech:aluminum_pick_head
    - rngtech:titanium_pick_head
    - rngtech:tungstensteel_pick_head
    - rngtech:nullite_pick_head
    - rngtech:exotic_pick_head
    - rngtech:flint_hammer_head
    - rngtech:iron_hammer_head
    - rngtech:copper_hammer_head
    - rngtech:bronze_hammer_head
    - rngtech:steel_hammer_head
    - rngtech:aluminum_hammer_head
    - rngtech:titanium_hammer_head
    - rngtech:tungstensteel_hammer_head
    - rngtech:nullite_hammer_head
    - rngtech:exotic_hammer_head
    - rngtech:flint_shovel_head
    - rngtech:iron_shovel_head
    - rngtech:copper_shovel_head
    - rngtech:bronze_shovel_head
    - rngtech:steel_shovel_head
    - rngtech:aluminum_shovel_head
    - rngtech:titanium_shovel_head
    - rngtech:tungstensteel_shovel_head
    - rngtech:nullite_shovel_head
    - rngtech:exotic_shovel_head
    - rngtech:flint_digger_head
    - rngtech:iron_digger_head
    - rngtech:copper_digger_head
    - rngtech:bronze_digger_head
    - rngtech:steel_digger_head
    - rngtech:aluminum_digger_head
    - rngtech:titanium_digger_head
    - rngtech:tungstensteel_digger_head
    - rngtech:nullite_digger_head
    - rngtech:exotic_digger_head
    - rngtech:flint_axe_head
    - rngtech:iron_axe_head
    - rngtech:copper_axe_head
    - rngtech:bronze_axe_head
    - rngtech:steel_axe_head
    - rngtech:aluminum_axe_head
    - rngtech:titanium_axe_head
    - rngtech:tungstensteel_axe_head
    - rngtech:nullite_axe_head
    - rngtech:exotic_axe_head
    - rngtech:flint_treefeller_head
    - rngtech:iron_treefeller_head
    - rngtech:copper_treefeller_head
    - rngtech:bronze_treefeller_head
    - rngtech:steel_treefeller_head
    - rngtech:aluminum_treefeller_head
    - rngtech:titanium_treefeller_head
    - rngtech:tungstensteel_treefeller_head
    - rngtech:nullite_treefeller_head
    - rngtech:exotic_treefeller_head
    - rngtech:wooden_tool_rod
    - rngtech:iron_tool_rod
    - rngtech:copper_conduit_tool_rod
    - rngtech:bronze_tool_rod
    - rngtech:steel_reinforced_tool_rod
    - rngtech:aluminum_tool_rod
    - rngtech:sparksteel_routed_tool_rod
    - rngtech:titanium_stabilized_tool_rod
    - rngtech:tungstensteel_heavy_tool_rod
    - rngtech:nullite_phase_tool_rod
    - rngtech:exotic_harmonic_tool_rod
---

# Modular Tools

{{ infobox(
    variants=[
        ["Pick", "rngtech:modular_pick"],
        ["Hammer", "rngtech:modular_hammer"],
        ["Shovel", "rngtech:modular_shovel"],
        ["Digger", "rngtech:modular_digger"],
        ["Axe", "rngtech:modular_axe"],
        ["Treefeller", "rngtech:modular_treefeller"],
    ],
    fields={
        "Type": "Mining and chopping tool",
        "Built at": "[Tool Bench](tool-bench.md)",
        "Power": "Optional Battery Cell",
    },
) }}

**Modular Field Tools** are RNGTech's picks, hammers, shovels, diggers, axes, and treefellers. Each one is built at the [Tool Bench](tool-bench.md) from a separate **Tool Head** and **Tool Rod**, and each part keeps its own rarity and affixes. They mine RNGTech ores early, and other pickaxes also mine them by their tier.

## Parts

A Modular Field Tool is made from:

- a **Tool Head**, which decides the tool type and material, such as a {{ item('rngtech:flint_pick_head') }}.
- a **Tool Rod**, which sets durability, speed adjustments, FE transfer, and which Battery Cells the tool supports, such as a {{ item('rngtech:wooden_tool_rod') }}.
- an optional **Battery Cell**, which pays the FE for tool features that use it.

Heads and rods are crafted at a crafting table, and each one rolls its own rarity and affixes. They roll 2 more Refinement Potential than other parts of the same stage, and land higher affix tiers a little more often. The assembled tool has no affixes of its own; it uses whatever its installed parts have.

{{ crafting("rngtech:flint_pick_head", "rngtech:wooden_tool_rod") }}

See [Building a tool](tool-bench.md#building-a-tool) for putting the parts together.

## Tool types

The head's type picks the tool:

| Head family | Tool | Breaks |
|---|---|---|
| Pick | {{ item('rngtech:modular_pick') }} | One pickaxe block. |
| Hammer | {{ item('rngtech:modular_hammer') }} | A 3×3 area of pickaxe blocks. Needs a Tiny Anvil to build. |
| Shovel | {{ item('rngtech:modular_shovel') }} | One shovel block. |
| Digger | {{ item('rngtech:modular_digger') }} | A 3×3 area of shovel blocks, such as dirt, gravel, and sand. Needs a Tiny Anvil to build. |
| Axe | {{ item('rngtech:modular_axe') }} | One axe block. |
| Treefeller | {{ item('rngtech:modular_treefeller') }} | A whole tree of connected logs, up to its limit. Needs a Tiny Anvil to build. |

## Ore hardness

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

Hardness applies to RNGTech's ores and to vanilla Iron, Copper, and Gold ore. Other mods' ores have no hardness, unless your modpack turns it on for them.

Other pickaxes also mine RNGTech ore, by their tier:

| Pickaxe | Ore hardness |
|---|---:|
| Stone | 1 |
| Iron | 3 |
| Diamond | 6 |
| Netherite Pickaxe | 8 |

Other mods' pickaxes and drills follow the same rule by their tier. Only the vanilla Netherite Pickaxe counts as netherite tier, so other mods' best pickaxes reach hardness 6. Modpacks can change these rules and can weaken or disable vanilla tools. See [Ore progression](modpack-creators.md#ore-progression).

## Tool behavior

- **Durability and FE** are spent per block actually broken, so a Hammer or Digger that hits fewer blocks costs less.
- **Stability** above 1.0 gives each broken block a chance to cost no durability and no FE. 1.10 Stability is a 10% chance.
- **Control** above 1.0 moves part of each durability point onto the installed Battery Cell as FE. 1.60 Control pays 60% of the wear in FE, up to 100% at 2.0. The full FE price per durability point is 16 FE for a Pick or Axe, 12 FE for a Shovel, 32 FE for a Hammer, and 28 FE for a Digger or Treefeller, scaled down by the share Control covers. If the cell cannot pay, the tool takes full durability wear for that block. Stability is checked first.
- **Self Repair** heads and rods restore 3 to 8 durability, depending on the affix tier, every 7.5 seconds while the tool sits idle in your hotbar or offhand. A head and rod that both have it add together. Mining pauses the repair.
- **Battery Cells** in your inventory recharge the tool's installed cell, first inventory slot first, up to the tool's FE transfer rate. Only cells within the rod's Battery Support count, the same limit that decides which cells you can install.
- **Vein Miner**, a rare Pick Head affix, breaks connected ore of the same type, up to the Vein Mine Limit. Each block after the first costs extra FE; when the cell cannot pay it, the rest of the vein stays. Sneak while mining to break only the block you aim at.
- **Ore Burst** speeds up mining on ore for Picks and Hammers. It needs the Ore Burst suffix on a part and an Ore Burst material: a Copper, Nullite, or Exotic head, or a {{ item('rngtech:copper_conduit_tool_rod') }}, {{ item('rngtech:sparksteel_routed_tool_rod') }}, {{ item('rngtech:nullite_phase_tool_rod') }}, or {{ item('rngtech:exotic_harmonic_tool_rod') }}. Each ore it breaks costs extra FE, and without enough FE the ore mines at normal speed.
- **Luck** can add extra drops from ores and logs broken by the tool. Each point, up to 5, gives every drop a 15% chance of one extra copy. It does nothing with Silk Touch, and nothing for machines, chests, or mobs.

## Data values

{{ data_values() }}

## See also

- [Tool Bench](tool-bench.md), which builds, changes, repairs, and refines modular tools.
- [Battery Cells](battery-cells.md)
- [Forestry Cart Station](forestry-cart-station.md), whose cart uses a modular Axe or Treefeller.

{{ navbox() }}
