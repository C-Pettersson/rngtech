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

You can read an Alloy Furnace from the outside. The installed Alloy Crucible sits in a refractory well wrapped in copper induction coils. While the furnace has a recipe, a pool fills the crucible as the charge melts and drops back when the alloy pours, glowing with the crucible's heat. Each input slot has a feed hopper on the back cabinet that shows the item in it, and hoppers past the furnace's input slots are closed with a lid. An installed Servo is a stirrer over the crucible that turns while the furnace works. Facing the front, the Heat Core is the finned radiator on the left and the Battery Cell stands in the cradle on the right with its charge meter. The gas cylinder at the front left corner is the [Nitrogen tank](rngtech:heat/nitrogen-atmosphere.md): its window shows how full it is, and its valve lights up while the crucible uses Nitrogen. The bar on the front console reads like the [heat bar](rngtech:heat/the-overheat-slider.md) on the Process tab: it fills with the crucible's heat against the Heat Core's Max Temperature, a white mark shows the recipe's target and a yellow mark the setpoint, and the fill turns red while the crucible overheats. A crucible pushed past its target flickers with its wobble. The cable port is on the back.

## Usage

- [Alloying](rngtech:alloy-furnace/alloying.md)
- [Stages](rngtech:alloy-furnace/stages.md)
- [Gear](rngtech:alloy-furnace/gear.md)
- [Automation](rngtech:alloy-furnace/automation.md)
- [Mastery and Ascendancies](rngtech:alloy-furnace/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:alloy-furnace/crafting.md)

### Breaking

Mine a placed Alloy Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## More

- [Screen](rngtech:alloy-furnace/screen.md)
