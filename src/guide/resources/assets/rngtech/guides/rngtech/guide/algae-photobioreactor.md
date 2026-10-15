---
navigation:
  title: "Algae Photobioreactor"
  parent: fluids-and-gases.md
  icon: rngtech:algae_photobioreactor
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:algae_photobioreactor
---

# Algae Photobioreactor

<BlockImage id="rngtech:algae_photobioreactor" scale="6" />

- **Type:** Support processor
- **Stage:** 3–4
- **Power:** None
- **Gear:** <ItemLink id="rngtech:bio_chamber" /> (optional)
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Algae Photobioreactor** grows **Algae Biomass** from water, a carbon-bearing gas, and light. It uses no power. Three glass columns hold a living algae culture, with carbon gas bubbling up through them; the harvest settles in a tank at the back. Algae Biomass is fuel for the [Bio Generator](rngtech:bio-generator.md) and a feedstock for methane in the [Melter](rngtech:melter.md).

It is a Stage 3–4 support machine that turns early gas byproducts into fuel. It has no rarity, affixes, or Refinement of its own; an installed Bio Chamber is what improves it.

## Usage

- [Growing algae](rngtech:algae-photobioreactor/growing-algae.md)
- [The culture](rngtech:algae-photobioreactor/the-culture.md)

### Getting carbon gas

- **Carbon Exhaust**: make it in a [Wooden Dehumidifier](rngtech:wooden-dehumidifier.md) from Coal Dust or Gasification Residue, or burn Syngas in a [Syngas Combustor](rngtech:syngas-combustor.md). To bootstrap, craft a Carbon Exhaust Bucket from a Water Bucket and one **Coal Dust**. You get Coal Dust by crushing coal in a [Crusher](rngtech:crusher.md).
- **Syngas**: from the [Coal Gasifier](rngtech:coal-gasifier.md).
- **Carbon Monoxide**: a byproduct of the [Steam Methane Reformer](rngtech:steam-methane-reformer.md).

### Gear

The Gear tab has one optional **Bio Chamber** slot. The chamber's fuel-efficiency rolls become *Bio Conversion*, which raises how much algae each cycle produces. Partial bonus algae carries over between cycles, so even a small bonus pays off over time. Without a chamber, each cycle makes exactly one Algae Biomass.

- [Using Algae Biomass](rngtech:algae-photobioreactor/using-algae-biomass.md)
- [Automation](rngtech:algae-photobioreactor/automation.md)

## Screen

The Algae Photobioreactor screen has three tabs:

- **Process**: water and carbon tanks with purge buttons, the container slots, the output slot, progress, the light level, the culture density, and the aeration gauge. Hover for amounts, the light requirement, and what the aeration setting does.
- **Gear**: the Bio Chamber slot.
- **Stats**: the machine's current stats.

## Obtaining

### Crafting

The recipe uses up a <ItemLink id="rngtech:bio_chamber" />. You need a second Bio Chamber if you want one in the Gear slot.

<RecipesFor id="rngtech:algae_photobioreactor" />

### Breaking

Mine the Photobioreactor with a pickaxe. It drops itself plus the items in its slots.
