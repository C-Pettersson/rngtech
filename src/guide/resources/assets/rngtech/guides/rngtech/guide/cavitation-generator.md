---
navigation:
  title: "Cavitation Generator"
  parent: power-generation.md
  icon: rngtech:cavitation_generator
categories:
  - "Power generation"
item_ids:
  - rngtech:cavitation_generator
---

# Cavitation Generator

<BlockImage id="rngtech:cavitation_generator" scale="6" />

- **Type:** Generator
- **Stages:** 5
- **Power:** FE generator
- **Gear:** <ItemLink id="rngtech:steel_cavitation_rotor" />, <ItemLink id="rngtech:steel_collapse_nozzle" />, <ItemLink id="rngtech:titanium_servo" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Cavitation Generator** is a Stage 5 generator that spins a rotor through Water to make FE. You set the rotor's throttle: run it at rated speed, ease off to make the rotor last, or push it into supercavitation for more FE at the cost of wear and heat. With the Stage 6 nitrogen Gear installed, it also splits Water into Nitrogen for the [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md). See [Power Chains](rngtech:power-chains/stage-6-ammonia.md) for the ammonia chain, and [Water supply](rngtech:power-chains/water-supply.md) for keeping it fed.

The installed rotor spins behind the front porthole at its real speed, and the chamber glows blue while it cavitates. The side sight glasses show the Water and Nitrogen levels, and the rear fins glow orange as heat strain builds.

Like every RNGTech machine, each Cavitation Generator you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

### Generating power

Install a Cavitation Rotor and a Collapse Nozzle in the Gear tab, then fill the input tank with Water. The tank holds 4,000 mB and takes fluid from pipes or from filled buckets and containers. Each cycle uses 1,000 mB of Water and generates FE while it runs; how long a cycle lasts depends on the rotor's speed (see below). The rotor's stage must meet the recipe's minimum; the starter Water recipe needs a Stage 5 rotor.

- [Rotor speed and throttle](rngtech:cavitation-generator/rotor-speed-and-throttle.md)
- [Rotor resonance](rngtech:cavitation-generator/rotor-resonance.md)
- [Nitrogen](rngtech:cavitation-generator/nitrogen.md)

### Burst rotor

The <ItemLink id="rngtech:aethergold_cavitation_rotor" /> is a Stage 7 sidegrade. It runs cycles very fast and raises FE sharply, but wears out in a short time. Attach the generator through a Universal Connector with a Sparksteel or better Energy Connector, or most of the burst is vented.

- [Gear](rngtech:cavitation-generator/gear.md)
- [Crafting rotors](rngtech:cavitation-generator/crafting-rotors.md)

### Automation

| Side | Behavior |
|---|---|
| Bottom | Extracts Pitted Cavitation Rotors. |
| Other sides | Insert filled fluid containers. |
| Any side | Fills the Water tank and drains Nitrogen through pipes. |
| Sides | Output FE, for example into a [Universal Cable](rngtech:universal-cable.md). |

Gear slots are not reachable by automation.

## Obtaining

- [Crafting](rngtech:cavitation-generator/crafting.md)

### Breaking

Mine a placed Cavitation Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## More

- [Screen](rngtech:cavitation-generator/screen.md)
