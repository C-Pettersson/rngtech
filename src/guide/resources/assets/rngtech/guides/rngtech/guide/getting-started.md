---
navigation:
  title: "Getting Started"
  parent: guides.md
  icon: rngtech:wooden_crusher_chassis
categories:
  - "Guides"
---

# Getting Started

RNGTech is a technology mod that treats machines like RPG characters. A machine's body, the **chassis**, sets its base stats and limits. The **Gear** you install inside it does the real work. Machines, Gear parts, tool parts, and Battery Cells each roll their own rarity and affixes, so two Crushers of the same stage can play very differently.

Progress runs through nine [stages](rngtech:stages.md), from flint and wood at Stage 0 to exotic materials at Stage 8. This page walks you through them in order. For exact ingredients, temperatures, and amounts, use the recipe tables on each machine page or JEI in game, because modpacks can change them.

## How RNGTech works

Every machine is built in layers, and each layer is its own item with its own roll. When a machine feels weak, the cause is usually its Gear rather than its chassis.

- **Chassis.** The placed machine block. It sets base stats and caps how advanced its Gear can be. A Gear part's stage can be equal to or lower than the chassis stage, never higher, so mixing old and new parts is normal. See [Stages](rngtech:stages.md#chassis-stage-and-gear).
- **Gear.** Parts you install in a machine's Gear tab. Crush Heads set how hard an ore a [Crusher](rngtech:crusher.md) handles, Heat Cores set how hot a [Furnace](rngtech:furnace.md) or [Metal Press](rngtech:metal-press.md) gets, and Molds pick what a press makes. See [Gear](rngtech:gear.md).
- **Battery Cells.** Gear that stores FE and sets how fast a machine takes it in. Most machines run slower or produce less without one. See [Battery Cells](rngtech:battery-cells.md).
- **Rarity and affixes.** Each rolled item is Normal, Magic, or Rare, and Magic and Rare items carry random affixes that change their stats. See [Rarity and Affixes](rngtech:rarity-and-affixes.md).
- **Refinement Potential.** Each rolled item also has a budget you spend to add, upgrade, reroll, or remove affixes. Higher-stage items roll a bigger budget. See [Refinement Potential](rngtech:rarity-and-affixes/refinement-potential.md).
- **Machine Mastery.** Placed machines earn XP from their work and spend points in a passive tree. See [Machine Mastery](rngtech:machine-mastery.md).

### Identifying new items

Crafted machines, Gear parts, tool parts, and Battery Cells come out **unidentified**. The crafting preview keeps the roll hidden on purpose. To reveal one:

- Put a single unidentified item alone in a crafting grid and take the result. Repeat for each item in a stack.
- Or place an unidentified machine block. Only the placed block is identified; the rest of the stack stays unidentified.

## Start: tools

You gather your own materials, so the first job is a set of tools. RNGTech tools are **Modular Field Tools** built from a separate head and rod, and each part keeps its own roll. See [Modular Tools](rngtech:modular-tools.md) for the full details and [Tool Bench](rngtech:tool-bench.md) for building them.

1. Craft a <ItemLink id="rngtech:tool_bench" />. It needs no iron.
2. Craft a <ItemLink id="rngtech:flint_pick_head" /> and a <ItemLink id="rngtech:wooden_tool_rod" />, and assemble a <ItemLink id="rngtech:modular_pick" /> in the bench's Build tab.
3. Add Flint Shovel and Axe heads when you want them. You can swap a head onto an existing rod instead of building a new tool.
4. Install a <ItemLink id="rngtech:tiny_anvil" /> in the bench's Gear tab. It is never used up and unlocks the heavy tools: Hammers break a 3×3 area, Diggers clear 3×3 of dirt or sand, and Treefellers fell whole trees.
5. Craft a **Forming Hammer** and hammer iron and copper ingots into plates. Plates lead to Iron and Copper heads, which mine faster than flint.
6. Set up a <ItemLink id="rngtech:crude_recycler" /> with a <ItemLink id="rngtech:hand_crank" /> on top. It lets you win back materials from spare parts and bad rolls by hand. See [Component Recycler](rngtech:component-recycler/crude-recycler-and-hand-crank.md).

### Ore hardness

RNGTech ores have a **hardness** from 1 to 8, separate from vanilla mining tiers. A Modular Pick or Hammer can only mine ores up to its head's hardness. Flint heads reach hardness 1, which covers Iron and Copper ore, and better heads reach further. See [Ore hardness](rngtech:modular-tools.md#ore-hardness) on the Modular Tools page for every head's reach.

## More

- [Installing](rngtech:getting-started/installing.md)
- [Stages 1–2: first power and ore doubling](rngtech:getting-started/stages-1-2-first-power-and-ore-doubling.md)
- [Stages 3–4: the steel factory](rngtech:getting-started/stages-3-4-the-steel-factory.md)
- [Stages 5–6: fluids and chemistry](rngtech:getting-started/stages-5-6-fluids-and-chemistry.md)
- [Stages 7–8: late game](rngtech:getting-started/stages-7-8-late-game.md)
- [Power basics](rngtech:getting-started/power-basics.md)
- [Logistics basics](rngtech:getting-started/logistics-basics.md)
- [Refinement](rngtech:getting-started/refinement.md)
- [Mastery and Ascendancies](rngtech:getting-started/mastery-and-ascendancies.md)
- [When a machine underperforms](rngtech:getting-started/when-a-machine-underperforms.md)
