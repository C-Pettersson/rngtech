---
navigation:
  title: "Melter"
  parent: processing.md
  icon: rngtech:melter
categories:
  - "Processing"
item_ids:
  - rngtech:melter
---

# Melter

<BlockImage id="rngtech:melter" scale="6" />

- **Type:** Processing machine
- **Stages:** 6
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:titanium_heat_core" />, <ItemLink id="rngtech:titanium_crush_head" />, <ItemLink id="rngtech:osmium_fluid_pump" />, <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:steel_servo" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Melter** is a Stage 6 machine that turns two items and a fluid, usually water, into a new fluid. Its main job is making Lubricant, which later Servo and frame recipes need. It also makes Electrolyte Solution in bulk for the [Component Assembler](rngtech:component-assembler.md), methane from algae for gas chemistry, lava, and Ender Slurry for the [Vacuum Collapse Generator](rngtech:vacuum-collapse-generator.md)'s Void Catalyst.

The Melter runs on FE and needs a Heat Core, a Crush Head, and a Fluid Pump installed. Like every RNGTech machine, each Melter you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Melting](rngtech:melter/melting.md)
- [Gear](rngtech:melter/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe items. Rejects items no Melter recipe uses, and never lets one ingredient fill the slot the other one needs; see [Automation](rngtech:automation.md). |
| Bottom | Extracts filled output containers. Ingredient leftovers are pushed into the inventory below; see [Automation](rngtech:automation/ingredient-leftovers.md). |
| Sides | Insert fluid containers and fill the input tank. With a Fluid Pump installed, sides also drain the output tank. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear slots and the Refinement slot cannot be filled by automation.

- [Mastery and Ascendancies](rngtech:melter/mastery-and-ascendancies.md)

## Screen

The Melter screen has five tabs:

- **Process**: item inputs, fluid container slots, the input and output tanks with purge buttons, progress, energy, and heat. Status squares show the recipe state and fluid transfer. Hover for exact values. The progress tooltip also shows time remaining and expected mB per minute with your Gear and Mastery applied.
- **Gear**: Heat Core, Crush Head, Fluid Pump, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Melter's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Obtaining

- [Crafting](rngtech:melter/crafting.md)

### Breaking

Mine a placed Melter with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.
