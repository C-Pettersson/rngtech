---
navigation:
  title: "Metal Press"
  parent: processing.md
  icon: rngtech:metal_press
categories:
  - "Processing"
item_ids:
  - rngtech:crude_metal_press
  - rngtech:metal_press
  - rngtech:insulator
  - rngtech:basic_circuit_blank
  - rngtech:advanced_circuit_blank
  - rngtech:elite_circuit_blank
  - rngtech:ultimate_circuit_blank
  - rngtech:basic_electric_circuit
  - rngtech:advanced_electric_circuit
  - rngtech:elite_electric_circuit
  - rngtech:ultimate_electric_circuit
---

# Metal Press

<BlockImage id="rngtech:metal_press" scale="6" />

<Row>
  <ItemImage id="rngtech:crude_metal_press" scale="2" />
  <ItemImage id="rngtech:metal_press" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 3 (Crude), 4 (Steel)
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:bronze_heat_core" />, <ItemLink id="rngtech:steel_servo" />, **Mold**, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Metal Press** forms ingots into plates, gears, and casings, presses circuit blanks into Electric Circuits, and presses coils, casings, and gears into Universal Cable connectors. The installed Mold decides what it makes. There are two presses: the **Crude Metal Press** is the early bootstrap press, and the Stage 4 **Metal Press** (the Steel press) is faster and more controlled but can fail.

Both presses run on FE and heat up before pressing. Like every RNGTech machine, each press you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Pressing](rngtech:metal-press/pressing.md)
- [Molds](rngtech:metal-press/molds.md)
- [Circuits](rngtech:metal-press/circuits.md)

### Stages

| Stage | Press | Notes |
|---:|---|---|
| 3 | <ItemLink id="rngtech:crude_metal_press" /> | Bootstrap press. Servo optional. Pauses instead of failing. |
| 4 | <ItemLink id="rngtech:metal_press" /> | Servo required. Uses failure strain and failure outputs. |

- [Gear](rngtech:metal-press/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe input. Rejects items no Mold can press. |
| Bottom | Extracts finished output. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md), and Nitrogen for the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). With Bright Anneal or Hydrogen Quench, also accepts Hydrogen for [annealing](rngtech:metal-press/annealing.md). |

Gear slots, Molds, and the Refinement slot cannot be filled by automation.

- [Mastery and Ascendancies](rngtech:metal-press/mastery-and-ascendancies.md)
- [Annealing](rngtech:metal-press/annealing.md)
- [Metal Press recipes](rngtech:metal-press/metal-press-recipes.md)

## Screen

Both presses share a screen with five tabs:

- **Process**: input and output slots, progress, energy, live heat, a failure-risk bar, and the atmosphere gauge at the right edge, for Nitrogen or Hydrogen. Status squares show the recipe state and the current risk. Hover for exact values.
- **Gear**: Heat Core, Servo, Battery Cell, and Mold slots, plus the active Mold selector.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed press's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Obtaining

- [Crafting](rngtech:metal-press/crafting.md)

### Breaking

Mine a placed press with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear and stored Molds, drop next to it.
