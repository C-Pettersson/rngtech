---
navigation:
  title: "Wooden Composter"
  parent: processing.md
  icon: rngtech:wooden_composter
categories:
  - "Processing"
item_ids:
  - rngtech:wooden_composter
---

# Wooden Composter

<BlockImage id="rngtech:wooden_composter" scale="6" />

- **Type:** Passive processing machine
- **Stages:** 1
- **Power:** None
- **Gear:** None
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Wooden Composter** slowly turns low-value plant matter into **Composted Biomass**, a dense early fuel for the [Bio Generator](rngtech:bio-generator.md). It needs no power, so you can fill it with seeds, saplings, leaves, and crop scraps and leave it to work. Adding water makes it twice as fast.

Unlike most RNGTech machines, the Wooden Composter has no rarity, affixes, Gear, or Mastery.

## Usage

- [Composting](rngtech:wooden-composter/composting.md)

### Inputs

The Composter accepts everything in the inputs tag below, plus any item that the vanilla Composter accepts.

<ItemLink id="minecraft:dandelion" />, <ItemLink id="minecraft:poppy" />, <ItemLink id="minecraft:blue_orchid" /> and 104 more

These prepared inputs count as 2 compost units each:

**Compost Feedstock**

- [Crushing seeds first](rngtech:wooden-composter/crushing-seeds-first.md)

### Using the output

Burn **Composted Biomass** in the [Bio Generator](rngtech:bio-generator.md) for 1,600 FE each before modifiers, or craft it with Bone Meal, Organic Reagent, or Gunpowder into **Rich Biomass**, a denser fuel. Composted Biomass is not a furnace fuel and does not burn in the [Solid Fuel Burner](rngtech:solid-fuel-burner.md).

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert compostable items and water containers, and fill the water tank. |
| Bottom | Extracts Composted Biomass and empty containers. |

The Wooden Composter has no FE input or output.

## Screen

The Wooden Composter has one working tab, **Process**. It shows the 27 input slots, the output slot, the water tank with its container slots and purge button, progress, and status squares for the bulk and variety bonuses. Hover for exact values.

## Obtaining

### Crafting

<RecipesFor id="rngtech:wooden_composter" />

### Breaking

Mine a placed Wooden Composter with a pickaxe. It drops itself, and the items inside it drop next to it.
