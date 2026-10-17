---
navigation:
  title: "Modular Tools"
  parent: tools.md
  icon: rngtech:modular_pick
categories:
  - "Tools"
item_ids:
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

<ItemImage id="rngtech:modular_pick" scale="4" />

<Row>
  <ItemImage id="rngtech:modular_pick" scale="2" />
  <ItemImage id="rngtech:modular_hammer" scale="2" />
  <ItemImage id="rngtech:modular_shovel" scale="2" />
  <ItemImage id="rngtech:modular_digger" scale="2" />
  <ItemImage id="rngtech:modular_axe" scale="2" />
  <ItemImage id="rngtech:modular_treefeller" scale="2" />
</Row>

- **Type:** Mining and chopping tool
- **Built at:** [Tool Bench](rngtech:tool-bench.md)
- **Power:** Optional Battery Cell

**Modular Field Tools** are RNGTech's picks, hammers, shovels, diggers, axes, and treefellers. Each one is built at the [Tool Bench](rngtech:tool-bench.md) from a separate **Tool Head** and **Tool Rod**, and each part keeps its own rarity and affixes. They are also how you mine the harder RNGTech ores; vanilla pickaxes only reach the first two ore-hardness levels by default.

## Tool types

The head's type picks the tool:

| Head family | Tool | Breaks |
|---|---|---|
| Pick | <ItemLink id="rngtech:modular_pick" /> | One pickaxe block. |
| Hammer | <ItemLink id="rngtech:modular_hammer" /> | A 3×3 area of pickaxe blocks. Needs a Tiny Anvil to build. |
| Shovel | <ItemLink id="rngtech:modular_shovel" /> | One shovel block. |
| Digger | <ItemLink id="rngtech:modular_digger" /> | A 3×3 area of shovel blocks, such as dirt, gravel, and sand. Needs a Tiny Anvil to build. |
| Axe | <ItemLink id="rngtech:modular_axe" /> | One axe block. |
| Treefeller | <ItemLink id="rngtech:modular_treefeller" /> | A whole tree of connected logs, up to its limit. Needs a Tiny Anvil to build. |

## Ore hardness

RNGTech ores have a **hardness** from 1 to 8, separate from vanilla mining tiers. A Modular Pick or Hammer mines ores up to its head's hardness, and a head reaches every hardness below its own. Ore that is too hard drops nothing. A Hammer skips any block in its area that is too hard for it, without spending durability or FE on it.

| Head material | Stage | Ore hardness | Opens up |
|---|---:|---:|---|
| Flint | 0 | 1 | Iron, Copper |
| Iron | 1 | 1 | |
| Copper | 2 | 2 | Tin, Zinc, Gold |
| Bronze | 3 | 2 | |
| Steel | 4 | 3 | Nickel, Lead, Silver |
| Steel with a <ItemLink id="rngtech:diamond_tip" /> | 4 | 4 | Aluminum, Osmium |
| Aluminum | 5 | 5 | Titanium |
| Titanium | 6 | 6 | Tungsten, Platinum |
| Tungstensteel | 7 | 7 | None by default |
| Nullite | 7 | 7 | None by default |
| Exotic | 8 | 8 | Naquadah |

By default, a vanilla stone pickaxe reaches hardness 1 and an iron or better pickaxe reaches hardness 2. Beyond that you need Modular tools. Modpacks can change both rules.

## More

- [Parts](rngtech:modular-tools/parts.md)
- [Tool behavior](rngtech:modular-tools/tool-behavior.md)
