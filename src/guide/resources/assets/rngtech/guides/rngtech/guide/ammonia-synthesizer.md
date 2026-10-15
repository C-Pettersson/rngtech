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

The **Ammonia Synthesizer** combines Nitrogen and Hydrogen into Ammonia using FE. It is the first half of the ammonia power chain: the Ammonia it makes fuels the [Ammonia Fuel Cell](rngtech:ammonia-fuel-cell.md), a strong late-game generator.

It is built as an ammonia loop: a compressor pushes the mixed gases into a thick-walled converter that holds the catalyst, and the hot gas passes through a frosted condenser into a glass Ammonia receiver.

Each Synthesizer you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and you can refine the placed machine.

## Usage

### Getting the gases

- **Nitrogen**: separate it from water in a Stage 6 [Cavitation Generator](rngtech:cavitation-generator.md) fitted with a Nitrogen Extraction Rotor and a Nitrogen Separation Nozzle.
- **Hydrogen**: from the [Steam Methane Reformer](rngtech:steam-methane-reformer.md).

### Gear

The Gear tab has one required **Catalyst Bed** slot. The bed's stage must be high enough for the recipe; the default recipe needs a Stage 6 bed, which is what the Ammonia Catalyst Bed is.

An unrolled Catalyst Bed only unlocks recipes. Its rolled Processing Speed and Energy Usage modifiers speed up the Synthesizer or cut its FE cost. Other rolls on the bed do nothing here. See [Ammonia Fuel Cell](rngtech:ammonia-fuel-cell.md) for details.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts an Ammonia Catalyst Bed into the Gear slot. Accepts FE. |
| Sides | Accept FE. |
| Any | Fills Nitrogen and Hydrogen, and drains Ammonia. |

There are no process item slots to automate. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one. To throw gas away, use the purge buttons beside each tank, or right-click the machine with a **Purge Bucket** to void up to 1,000 mB. A normal right-click empties Nitrogen or Hydrogen first, and sneaking empties the Ammonia tank first. Purging Nitrogen or Hydrogen resets the cycle in progress.

## Screen

The Ammonia Synthesizer screen has four tabs:

- **Process**: Nitrogen, Hydrogen, and Ammonia tanks with purge buttons, progress, FE, and status.
- **Gear**: the Catalyst Bed slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Synthesizer's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:ammonia-synthesizer/crafting.md)

### Breaking

Mine the Synthesizer with a pickaxe. It drops itself with its rolled traits, plus its Catalyst Bed.
