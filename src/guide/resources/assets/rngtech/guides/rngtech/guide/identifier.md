---
navigation:
  title: "Identifier"
  parent: refinement.md
  icon: rngtech:identifier
categories:
  - "Refinement"
item_ids:
  - rngtech:identifier
  - rngtech:identifying_fluid_bucket
---

# Identifier

<BlockImage id="rngtech:identifier" scale="6" />

- **Type:** Processing machine
- **Stages:** 8
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:exotic_fluid_pump" />, <ItemLink id="rngtech:exotic_servo" />, <ItemLink id="rngtech:exotic_battery_cell" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Identifier** identifies unidentified items without a player. It uses FE and <ItemLink id="rngtech:identifying_fluid_bucket" /> for each item. It is the only way that automation can identify an item. Use it with an [Affix Sorter](rngtech:affix-sorter.md) to keep the items you want from an automated crafting line.

An item identified here rolls in the same way as an item you identify by hand. Annealed items still roll their Refinement Potential twice. A Unique still gets the values that it got when it dropped. To identify an item by hand, see [Rarity and Affixes](rngtech:rarity-and-affixes/identification.md).

## Usage

- [Identifying Fluid](rngtech:identifier/identifying-fluid.md)
- [Identifying items](rngtech:identifier/identifying-items.md)

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Fluid Pump | Yes | Any <ItemLink id="rngtech:osmium_fluid_pump" />. The pump lets fluid in through every face and sets the fill rate. |
| Servo | Yes | Any Servo. The Servo sets the speed. |
| Battery Cell | No | Any Battery Cell. Without one, the Identifier runs 20% slower. |

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert unidentified items. Accept FE. Accept Identifying Fluid when a Fluid Pump is installed. |
| Bottom | Extract identified items. Accept FE. |

Cables and hoppers cannot reach the Gear slots.

## Screen

The Identifier has six tabs:

- **Process**: the input and output slots, the fluid gauge with its purge button and bucket slot, the energy bar, and progress.
- **Gear**: the Battery Cell, Fluid Pump, and Servo slots.
- **Stats**: Processing Speed, Energy Use, Fluid Use, Energy Capacity, and Fluid Transfer.
- **Refinement**: refine the placed Identifier with a catalyst.
- **Mastery**: the shared passive tree and the Identifier's ascendancies.
- **Orders**: three drive bays for [Job Chips](rngtech:work-orders.md). Identifier jobs count identified items.

## Obtaining

### Crafting

The Identifier comes out Unidentified and rolls as a Stage 8 machine.

<RecipesFor id="rngtech:identifier" />

- Requires the Naquadah material to be enabled.

### Breaking

Mine the Identifier with a pickaxe. It keeps its traits, Mastery, and Job Chips. The items in its slots drop. The fluid in its tank is lost.
