---
navigation:
  title: "Syngas Combustor"
  parent: power-generation.md
  icon: rngtech:syngas_combustor
categories:
  - "Power generation"
item_ids:
  - rngtech:syngas_combustor
---

# Syngas Combustor

<BlockImage id="rngtech:syngas_combustor" scale="6" />

- **Type:** Generator
- **Stages:** 5
- **Power:** FE generator
- **Gear:** <ItemLink id="rngtech:sparksteel_battery_cell" />, <ItemLink id="rngtech:steel_servo" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Syngas Combustor** is the Stage 5 gas-power generator. It burns Syngas from the [Coal Gasifier](rngtech:coal-gasifier.md) for 240 FE/t before rolls, and turns the leftovers into Carbon Exhaust that you can feed to an [Algae Photobioreactor](rngtech:algae-photobioreactor.md). It also burns Carbon Monoxide as a weak way to get rid of it. It is built like a small gas turbine: fuel gas feeds a combustion can, the hot gas turns a turbine, and the turbine's shaft drives an alternator at the back. See [Power Chains](rngtech:power-chains/stage-5-syngas.md) for the whole chain around it.

Like every RNGTech machine, each Syngas Combustor you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Generating power](rngtech:syngas-combustor/generating-power.md)
- [Mixture](rngtech:syngas-combustor/mixture.md)
- [Gear](rngtech:syngas-combustor/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Output FE, for example into a [Universal Cable](rngtech:universal-cable.md). |
| Any | Fills the gas tank with Syngas or Carbon Monoxide, and drains Carbon Exhaust. |

Gear slots are not reachable by automation. The back of the alternator has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Screen

The Syngas Combustor screen has four tabs:

- **Process**: the gas and Carbon Exhaust tanks with their purge buttons, FE, cycle progress, and the mixture gauge. Hover the gauge for what the setting does.
- **Gear**: Battery Cell and Servo slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed combustor's traits with a catalyst.

A **Purge Bucket** used on the combustor voids up to 1,000 mB from one of its tanks.

## Obtaining

### Crafting

<RecipesFor id="rngtech:syngas_combustor" />

- Requires the Aluminum material to be enabled.

### Breaking

Mine a placed Syngas Combustor with a pickaxe. It drops itself and keeps its rolled traits.
