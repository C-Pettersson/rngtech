---
navigation:
  title: "Coal Gasifier"
  parent: fluids-and-gases.md
  icon: rngtech:coal_gasifier
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:coal_gasifier
---

# Coal Gasifier

<BlockImage id="rngtech:coal_gasifier" scale="6" />

- **Type:** Gas chemistry machine
- **Stages:** 5
- **Power:** FE consumer
- **Gear:** <ItemLink id="rngtech:aluminum_heat_core" />, <ItemLink id="rngtech:sparksteel_battery_cell" />, <ItemLink id="rngtech:steel_servo" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Coal Gasifier** turns Coal Dust and water into **Syngas**, the Stage 5 step up from burning coal directly. Burn the Syngas in a [Syngas Combustor](rngtech:syngas-combustor.md) for power, or reform it in a [Steam Methane Reformer](rngtech:steam-methane-reformer.md). Each run also leaves a **Gasification Residue** behind.

It is built as a gasifier tower: a feed hopper on top drops Coal Dust through a rotary valve into a tall reactor vessel, and ash falls out of the bottom into a residue bin.

Like other RNGTech machines, each Coal Gasifier you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Gasifying coal](rngtech:coal-gasifier/gasifying-coal.md)

### Where the products go

- **Syngas** fuels the [Syngas Combustor](rngtech:syngas-combustor.md), RNGTech's Stage 5 gas generator. The [Steam Methane Reformer](rngtech:steam-methane-reformer.md) can also reform it into Hydrogen, at a lower yield than Methane.
- **Gasification Residue** can be turned into Carbon Exhaust in a [Wooden Dehumidifier](rngtech:wooden-dehumidifier.md), which feeds algae in the [Algae Photobioreactor](rngtech:algae-photobioreactor.md).

- [Gear](rngtech:coal-gasifier/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts Coal Dust into the input slot. Accepts FE. |
| Bottom | Extracts Gasification Residue. Does not accept FE. |
| Sides | Accept FE. |
| Any | Fill water and drain Syngas through any face. |

Gear slots are manual only. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Screen

The Coal Gasifier screen has four tabs:

- **Process**: the Coal Dust and residue slots, water and Syngas tanks with purge buttons, progress, energy, and status. Hover for exact amounts.
- **Gear**: Heat Core, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Gasifier's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:coal-gasifier/crafting.md)

### Breaking

Mine a placed Coal Gasifier with a pickaxe. It drops itself with its rolled traits, plus any Coal Dust, residue, and Gear inside it. Fluid in its tanks is lost.
