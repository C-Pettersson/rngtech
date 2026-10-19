---
navigation:
  title: "Potential Reactor"
  parent: power-generation.md
  icon: rngtech:potential_reactor
categories:
  - "Power generation"
item_ids:
  - rngtech:potential_reactor
---

# Potential Reactor

<BlockImage id="rngtech:potential_reactor" scale="6" />

- **Type:** Generator
- **Power:** Burns salvage and retired gear, generates FE
- **Gear:** <ItemLink id="rngtech:iron_reactor_chamber" />, <ItemLink id="rngtech:iron_recovery_filter" />, <ItemLink id="rngtech:iron_containment_lining" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Potential Reactor** turns salvage and old gear into FE. Feed it recycling byproducts, or any rolled machine, part, or Battery Cell you no longer need, and it pays out power for the item's rarity, affixes, and remaining Refinement Potential. The stripped item that comes out goes on to the [Component Recycler](rngtech:component-recycler.md) for its materials. See [Power Chains](rngtech:power-chains.md#potential-reactor) for its place in your power supply.

It is a containment vessel with a viewport on the front. Fuel drops in through the hopper on the lid, burns as a glowing core between two electrodes, and its residue falls through the filter cage on the side into a glass bin.

Each Potential Reactor you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md). It does not have Machine Mastery.

## Usage

- [Generating power](rngtech:potential-reactor/generating-power.md)
- [Recycling gear](rngtech:potential-reactor/recycling-gear.md)
- [Gear](rngtech:potential-reactor/gear.md)
- [Reading the reactor](rngtech:potential-reactor/reading-the-reactor.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts salvage fuel or gear to recycle. |
| Bottom | Extracts residue and stripped items. |
| Sides | Export FE to adjacent receivers. Do not accept FE. |

Gear slots are installed by hand. The back of the reactor has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Screen

The Potential Reactor screen has four tabs:

- **Process**: input and residue slots, the FE buffer, processing progress, and status. Hover for exact FE, fuel value, generation rate, and last tick's FE output.
- **Gear**: Reactor Chamber, Recovery Filter, and Containment Lining slots, plus the chamber's stage.
- **Stats**: the reactor's current stats, including traits and Gear.
- **Refinement**: refine the placed reactor's traits with a catalyst.

## Obtaining

### Crafting

<RecipesFor id="rngtech:potential_reactor" />

### Breaking

Mine it with a pickaxe to get it back. The dropped reactor keeps its rarity, affixes, and memory of recent gear burns. Items in its slots and installed Gear drop as separate items.
