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

## Usage

- [Smelting](rngtech:furnace/smelting.md)
- [Stages](rngtech:furnace/stages.md)

### Batching

With a Batch Size above 1, each lane smelts several items from its input stack in one cycle. A batch takes as many items as the stack and the lane's free output space allow, up to the Batch Size, and keeps that count until the cycle ends. Each extra item makes the cycle longer by the Batch Overhead, 25% of one item's time to start with. Every item still absorbs its own heat, paid in FE or burn time, and earns its own XP. A bigger batch takes a bigger heat dip from the lane when it goes in. If a batch fails, every item in it becomes a **Malformed Ingot**.

- [Gear](rngtech:furnace/gear.md)
- [Automation](rngtech:furnace/automation.md)
- [Mastery and Ascendancies](rngtech:furnace/mastery-and-ascendancies.md)

## Screen

The Furnace screen has five tabs:

- **Process**: input and output lanes, live heat, failure strain, and energy or fuel. Electric stages add the Nitrogen gauge at the right edge. Hover for exact temperatures.
- **Gear**: Heat Core and Battery Cell slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Furnace's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Obtaining

- [Crafting](rngtech:furnace/crafting.md)

### Breaking

Mine a placed Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress.
