---
navigation:
  title: "Alloy Furnace"
  parent: processing.md
  icon: rngtech:bronze_alloy_furnace_chassis
categories:
  - "Processing"
item_ids:
  - rngtech:bronze_alloy_furnace_chassis
  - rngtech:steel_alloy_furnace_chassis
  - rngtech:titanium_alloy_furnace_chassis
---

# Alloy Furnace

<BlockImage id="rngtech:bronze_alloy_furnace_chassis" scale="6" />

<Row>
  <ItemImage id="rngtech:bronze_alloy_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:steel_alloy_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:titanium_alloy_furnace_chassis" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 3, 4, 6
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:bronze_heat_core" />, <ItemLink id="rngtech:bronze_alloy_crucible" />, <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:steel_servo" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Alloy Furnace** mixes exact amounts of metals and additives into alloys. Early recipes make alloy blends such as **Bronze Blend** and **Steel Blend**, which you then smelt in a [Furnace](rngtech:furnace.md). Stronger setups unlock direct-ingot recipes that skip the blend step, and late alloys such as Tungstensteel are only made this way.

The Alloy Furnace runs on FE and needs a Heat Core and an Alloy Crucible installed. Like every RNGTech machine, each Alloy Furnace you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Alloying](rngtech:alloy-furnace/alloying.md)
- [Stages](rngtech:alloy-furnace/stages.md)

### Gear

The Gear tab has four slots. No Gear can be a higher stage than the chassis.

- **Heat Core** (required): sets how hot the Alloy Furnace can get.
- **Alloy Crucible** (required): sets how many input slots are active and adds mixing control.
- **Battery Cell** (optional): adds portable FE storage.
- **Servo** (optional): adds control stats.

| Alloy Crucible | Stage | Input slots |
|---|---:|---:|
| <ItemLink id="rngtech:bronze_alloy_crucible" /> | 3 | 3 |
| <ItemLink id="rngtech:steel_alloy_crucible" /> | 4 | 3 |
| <ItemLink id="rngtech:titanium_alloy_crucible" /> | 6 | 4 |

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the active input slots. Accepts only Alloy Furnace recipe ingredients, and never lets one ingredient fill a slot another ingredient needs; see [Automation](rngtech:automation.md). |
| Bottom | Extracts finished output. Ingredient leftovers are pushed into the inventory below; see [Automation](rngtech:automation/ingredient-leftovers.md). |
| Sides | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |
| Any | Accepts Nitrogen for the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). |

Gear slots cannot be filled by automation.

- [Mastery and Ascendancies](rngtech:alloy-furnace/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:alloy-furnace/crafting.md)

### Breaking

Mine a placed Alloy Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## More

- [Screen](rngtech:alloy-furnace/screen.md)
