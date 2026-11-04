---
navigation:
  title: "Furnace"
  parent: processing.md
  icon: rngtech:furnace
categories:
  - "Processing"
item_ids:
  - rngtech:furnace
  - rngtech:iron_furnace_chassis
  - rngtech:copper_furnace_chassis
  - rngtech:gold_furnace_chassis
  - rngtech:bronze_furnace_chassis
  - rngtech:steel_furnace_chassis
  - rngtech:lead_furnace_chassis
  - rngtech:aluminum_furnace_chassis
  - rngtech:titanium_furnace_chassis
  - rngtech:tungstensteel_furnace_chassis
  - rngtech:exotic_furnace_chassis
---

# Furnace

<BlockImage id="rngtech:furnace" scale="6" />

<Row>
  <ItemImage id="rngtech:furnace" scale="2" />
  <ItemImage id="rngtech:iron_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:copper_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:gold_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:bronze_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:steel_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:lead_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:aluminum_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:titanium_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:tungstensteel_furnace_chassis" scale="2" />
  <ItemImage id="rngtech:exotic_furnace_chassis" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 0–8
- **Power:** Solid fuel (Stage 0), FE (Stages 1–8)
- **Gear:** <ItemLink id="rngtech:iron_heat_core" />, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Furnace** is RNGTech's heat-aware smelter. Each recipe needs the machine to warm up to a target temperature first, so the Furnace's stage and installed Heat Core decide which ores and alloys it can smelt. Stage 0 burns ordinary furnace fuel. Stages 1–8 are electric chassis that run on FE.

Like every RNGTech machine, each Furnace you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

You can read a Furnace from the outside. The Stage 0 Furnace is a smooth-stone kiln: fuel burns in the fire mouth at the bottom, the item being smelted sits in the oven above it, and smoke rises from the copper-banded chimney while fuel burns. An electric Furnace shows the item it is smelting on the hearth behind its viewport door. As the lane heats up, the chamber and its heating elements glow from dull red through orange to yellow-white. The bar under the door reads like the [heat bar](rngtech:heat/the-overheat-slider.md) on the Process tab: it fills with the lane's heat against the Heat Core's Max Temperature, a white mark shows the recipe's target and a yellow mark the setpoint when the slider pushes past it, and the fill turns red while the lane overheats. A lane pushed past its target flickers with its wobble. With extra lanes, the items stand side by side on the hearth and the bar reads the hottest lane. The Heat Core sits on the crown as a row of fins in the core's material. Facing the front, the Battery Cell stands in the cradle on the left with its charge meter, the [Nitrogen tank](rngtech:heat/nitrogen-atmosphere.md) is the gas cylinder behind it, and the cable port is on the back. The window on the cylinder shows how full the tank is, and its valve lights up while a lane uses Nitrogen. The Lead Furnace has four small chambers, one per lane: under each column of doors, the upper half of the bar reads the top chamber and the lower half the bottom one. Each chassis has its own shell metal.

## Usage

- [Smelting](rngtech:furnace/smelting.md)
- [Stages](rngtech:furnace/stages.md)
- [Batching](rngtech:furnace/batching.md)
- [Gear](rngtech:furnace/gear.md)
- [Automation](rngtech:furnace/automation.md)
- [Mastery and Ascendancies](rngtech:furnace/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:furnace/crafting.md)

### Breaking

Mine a placed Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress.

## More

- [Screen](rngtech:furnace/screen.md)
