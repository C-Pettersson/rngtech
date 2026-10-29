---
navigation:
  title: "Steam Methane Reformer"
  parent: fluids-and-gases.md
  icon: rngtech:steam_methane_reformer
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:steam_methane_reformer
  - rngtech:reforming_catalyst_bed
---

# Steam Methane Reformer

<BlockImage id="rngtech:steam_methane_reformer" scale="6" />

- **Type:** Gas chemistry machine
- **Stages:** 6
- **Power:** FE consumer
- **Gear:** <ItemLink id="rngtech:titanium_heat_core" />, <ItemLink id="rngtech:reforming_catalyst_bed" />, <ItemLink id="rngtech:arclite_battery_cell" />, <ItemLink id="rngtech:steel_servo" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Steam Methane Reformer** reacts a gas with water to make **Hydrogen** and **Carbon Monoxide**. It is the Stage 6 Hydrogen source for the [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md). Methane is its main feed. Syngas from the [Coal Gasifier](rngtech:coal-gasifier.md) also works, at a lower Hydrogen yield. Fed Carbon Monoxide instead, it runs the water-gas shift, which turns the Carbon Monoxide into more Hydrogen and a little Carbon Exhaust.

It is built as a fired tubular reformer: burners under a firebox heat a row of catalyst-filled tubes, and a steam drum on top feeds them water.

Like other RNGTech machines, each Steam Methane Reformer you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Reforming gas](rngtech:steam-methane-reformer/reforming-gas.md)
- [Water-gas shift](rngtech:steam-methane-reformer/water-gas-shift.md)
- [Where the products go](rngtech:steam-methane-reformer/where-the-products-go.md)
- [Gear](rngtech:steam-methane-reformer/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Accepts FE. |
| Bottom | Does not accept FE. |
| Sides | Accept FE. |
| Any | Fill water and Methane, Syngas, or Carbon Monoxide, and drain Hydrogen and the second output, through any face. |

The Reformer has no item automation. Gear slots are manual only. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Screen

The Steam Methane Reformer screen has four tabs:

- **Process**: water, gas, Hydrogen, and second output tanks with purge buttons, progress, energy, and status. Hover for exact amounts.
- **Gear**: Heat Core, Battery Cell, Servo, and Catalyst Bed slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Reformer's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:steam-methane-reformer/crafting.md)

### Breaking

Mine a placed Steam Methane Reformer with a pickaxe. It drops itself with its rolled traits, plus any installed Gear. Fluid in its tanks is lost.
