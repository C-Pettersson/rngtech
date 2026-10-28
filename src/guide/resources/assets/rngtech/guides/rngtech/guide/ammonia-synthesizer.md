---
navigation:
  title: "Ammonia Synthesizer"
  parent: fluids-and-gases.md
  icon: rngtech:ammonia_synthesizer
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:ammonia_synthesizer
  - rngtech:ammonia_catalyst_bed
  - rngtech:urea
---

# Ammonia Synthesizer

<BlockImage id="rngtech:ammonia_synthesizer" scale="6" />

<Row>
  <ItemImage id="rngtech:ammonia_synthesizer" scale="2" />
  <ItemImage id="rngtech:ammonia_catalyst_bed" scale="2" />
</Row>

- **Type:** Gas processor
- **Stage:** 6
- **Power:** FE consumer
- **Gear:** <ItemLink id="rngtech:ammonia_catalyst_bed" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Ammonia Synthesizer** combines Nitrogen and Hydrogen into Ammonia using FE. It is the first half of the ammonia power chain: the Ammonia it makes fuels the [Fuel Cell](rngtech:ammonia-fuel-cell.md), a strong late-game generator. It can also combine Ammonia with Carbon Exhaust into <ItemLink id="rngtech:urea" />, a fertilizer.

It is built as an ammonia loop: a compressor pushes the mixed gases into a thick-walled converter that holds the catalyst, and the hot gas passes through a frosted condenser into a glass Ammonia receiver.

Each Synthesizer you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and you can refine the placed machine.

## Usage

### Using Urea

- **By hand or from a dispenser**, Urea works like bone meal on any plant.
- In the [Forestry Cart Station](rngtech:forestry-cart-station/mastery-and-ascendancies.md), the Forestry Companion and its station take Urea wherever they take bone meal. Each Urea counts as 4 bone meal.
- In the [Algae Photobioreactor](rngtech:algae-photobioreactor.md#urea), each Urea grows the next 4 Algae Biomass at twice the speed.
- In a crafting grid, one Urea turns four Composted Biomass into four [Rich Biomass](rngtech:bio-generator/fuel-chains.md).

- [Getting the gases](rngtech:ammonia-synthesizer/getting-the-gases.md)

### Gear

The Gear tab has one required **Catalyst Bed** slot. The bed's stage must be high enough for the recipe; the default recipe needs a Stage 6 bed, which is what the Ammonia Catalyst Bed is.

An unrolled Catalyst Bed only unlocks recipes. Its rolled Processing Speed, Energy Usage, and Instant Process affixes apply to the Synthesizer. An older bed can still show Efficiency, Stability, or Fluid Transfer. Those affixes do nothing.

- [Automation](rngtech:ammonia-synthesizer/automation.md)

## Screen

The Ammonia Synthesizer screen has four tabs:

- **Process**: Nitrogen, Hydrogen, Ammonia, and Carbon Exhaust tanks with purge buttons, the Urea slot, progress, FE, and status.
- **Gear**: the Catalyst Bed slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Synthesizer's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:ammonia-synthesizer/crafting.md)

### Breaking

Mine the Synthesizer with a pickaxe. It drops itself with its rolled traits, plus its Catalyst Bed.
