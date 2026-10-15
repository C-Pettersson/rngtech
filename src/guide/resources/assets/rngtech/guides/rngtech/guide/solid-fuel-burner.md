---
navigation:
  title: "Solid Fuel Burner"
  parent: power-generation.md
  icon: rngtech:crude_solid_fuel_burner
categories:
  - "Power generation"
item_ids:
  - rngtech:crude_solid_fuel_burner
  - rngtech:copper_solid_fuel_burner
  - rngtech:alloy_solid_fuel_burner
  - rngtech:steel_solid_fuel_burner
---

# Solid Fuel Burner

<BlockImage id="rngtech:crude_solid_fuel_burner" scale="6" />

<Row>
  <ItemImage id="rngtech:crude_solid_fuel_burner" scale="2" />
  <ItemImage id="rngtech:copper_solid_fuel_burner" scale="2" />
  <ItemImage id="rngtech:alloy_solid_fuel_burner" scale="2" />
  <ItemImage id="rngtech:steel_solid_fuel_burner" scale="2" />
</Row>

- **Type:** Generator
- **Stages:** 1–4
- **Power:** Burns solid fuel, generates FE
- **Gear:** <ItemLink id="rngtech:iron_heat_core" />, <ItemLink id="rngtech:iron_fuel_box" />, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Solid Fuel Burner** is RNGTech's first real generator. It burns coal, charcoal, wood, and other furnace fuels and turns them into FE. The chassis decides which Gear you can install, and the Gear decides how fast it generates, which fuels it accepts, and how carefully it spends them. See [Power Chains](rngtech:power-chains/stage-1-4-coal.md) for where it fits and what replaces it.

Each burner you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md). Burners do not have Machine Mastery.

You can read a burner from the outside. Facing its front, the hopper on the right is the installed Fuel Box, split into two bins when it has two fuel slots, and shows the fuel waiting in each. The current fuel item burns on the grate behind the front grille. The Heat Core sits on top as a row of fins in the core's material. The flames stand taller at a higher FE/t, from a stronger Heat Core or a higher stoke, and the draught slots in the ash-pit door below the grille open and close with the stoke. A burner paused by its Fuel Governor keeps a dull, banked fire with no flames. The meter on the left side shows how full the Battery Cell is, and smoke rises from the chimney while fuel burns. Each chassis has its own casing: cast iron for Crude, copper for Copper, bronze for Alloy, and blued steel for Steel.

## Usage

- [Generating power](rngtech:solid-fuel-burner/generating-power.md)
- [Stoke](rngtech:solid-fuel-burner/stoke.md)
- [Fuel](rngtech:solid-fuel-burner/fuel.md)
- [Stages](rngtech:solid-fuel-burner/stages.md)
- [Gear](rngtech:solid-fuel-burner/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Any | Inserts fuel into the Fuel Box fuel slots. |
| Any | Exports FE to adjacent receivers. Does not accept FE. |

Heat Cores, Fuel Boxes, and Battery Cells are installed by hand. Automation cannot reach the Gear slots. The back of the burner has a port plate sized for a Universal Connector, the tidiest place to attach one.

## Screen

The burner screen has four tabs:

- **Process**: fuel slots, burn progress, the stoke gauge, stored FE, and FE/t. Hover for exact values.
- **Gear**: Heat Core, Fuel Box, and Battery Cell slots.
- **Stats**: the burner's current stats, including traits and installed Gear.
- **Refinement**: refine the placed burner's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:solid-fuel-burner/crafting.md)

### Breaking

Mine a placed burner with a pickaxe. It drops itself and keeps its rolled traits. Fuel and installed Gear drop as separate items.
