---
navigation:
  title: "Fuel Cell"
  parent: power-generation.md
  icon: rngtech:ammonia_fuel_cell
categories:
  - "Power generation"
item_ids:
  - rngtech:ammonia_fuel_cell
---

# Fuel Cell

<BlockImage id="rngtech:ammonia_fuel_cell" scale="6" />

- **Type:** Generator
- **Stages:** 6
- **Power:** FE generator
- **Gear:** <ItemLink id="rngtech:fuel_cell_membrane" />, <ItemLink id="rngtech:arclite_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Fuel Cell** is the Stage 6 generator at the end of the ammonia chain. It burns Ammonia from the [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md) through a Fuel Cell Membrane and makes 600 FE/t before rolls, leaving a little **Corrosion Residue** behind each cycle. It can also burn Hydrogen straight from the [Steam Methane Reformer](rngtech:steam-methane-reformer.md), for much less FE. See [Power Chains](rngtech:power-chains/stage-6-ammonia.md) for the machines that feed it.

It is built as a fuel cell stack: twelve cells clamped between two steel end plates by tie rods, with copper bus bars carrying the current back to a power cabinet.

Like every RNGTech machine, each Fuel Cell you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Generating power](rngtech:ammonia-fuel-cell/generating-power.md)
- [Ammonia or Hydrogen](rngtech:ammonia-fuel-cell/ammonia-or-hydrogen.md)

### Gear

- **Fuel Cell Membrane** (required): gates recipes by stage. The membrane has neutral base stats, so an unrolled one only unlocks the cell. Its rolled Energy Generation, Processing Speed, and Efficiency affixes apply to the fuel cell. An older membrane can still show Stability or Fluid Transfer. Those affixes do nothing.
- **Battery Cell** (optional): adds FE storage. Only Stage 6 and higher cells fit.

### Automation

| Side | Behavior |
|---|---|
| Top | Outputs FE. |
| Bottom | Extracts Corrosion Residue. |
| Sides | Output FE, for example into a [Universal Cable](rngtech:universal-cable.md). |
| Any | Fills the fuel tank with Ammonia or Hydrogen through pipes. |

Gear slots are not reachable by automation. The back of the cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Obtaining

### Crafting

<RecipesFor id="rngtech:ammonia_fuel_cell" />

### Breaking

Mine a placed Fuel Cell with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## More

- [Screen](rngtech:ammonia-fuel-cell/screen.md)
