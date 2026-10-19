---
navigation:
  title: "Bio Generator"
  parent: power-generation.md
  icon: rngtech:bio_generator
categories:
  - "Power generation"
item_ids:
  - rngtech:bio_generator
  - rngtech:biogas_membrane
---

# Bio Generator

<BlockImage id="rngtech:bio_generator" scale="6" />

- **Type:** Generator
- **Power:** Digests crops and biomass into biogas, burns it for FE or exports it as Methane
- **Gear:** <ItemLink id="rngtech:bio_chamber" />, <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:osmium_fluid_pump" />, <ItemLink id="rngtech:biogas_membrane" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Bio Generator** is an early generator built like a small farm biogas plant. A feed hopper drops food and plant matter into a digester tank, where it rots into biogas. The gas collects under a membrane dome, and a small gas engine burns it for FE. Potatoes, carrots, bread, saplings, and seeds all work straight from the farm, and prepared biomass from the [Wooden Composter](rngtech:wooden-composter.md) and [Algae Photobioreactor](rngtech:algae-photobioreactor.md) lasts much longer. See [Power Chains](rngtech:power-chains.md#bio-generator) for how it fits beside the main generators. Later, a Fluid Pump turns the same generator into a Methane source for the Stage 6 chain; see [Methane take-off](rngtech:bio-generator/methane-take-off.md).

Each Bio Generator you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md). It does not have Machine Mastery.

## Usage

- [Generating power](rngtech:bio-generator/generating-power.md)
- [Retention time](rngtech:bio-generator/retention-time.md)
- [Fuel](rngtech:bio-generator/fuel.md)
- [Fuel chains](rngtech:bio-generator/fuel-chains.md)
- [Methane take-off](rngtech:bio-generator/methane-take-off.md)
- [Gear](rngtech:bio-generator/gear.md)
- [Automation](rngtech:bio-generator/automation.md)

## Screen

The Bio Generator screen has four tabs:

- **Process**: fuel slot, the retention dial, digestion progress, the gas holder gauge with its engine start mark, stored FE, the Methane and Carbon Exhaust tanks, and status icons. Hover for exact values.
- **Gear**: Battery Cell, Bio Chamber, and two Gas Line slots.
- **Stats**: the generator's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

## Obtaining

### Crafting

<RecipesFor id="rngtech:bio_generator" />

### Breaking

Mine it with a pickaxe to get it back. Fuel and installed Gear drop as separate items. Methane and Carbon Exhaust in its tanks are lost.
