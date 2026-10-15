---
navigation:
  title: "Silica Gel Dehumidifier"
  parent: fluids-and-gases.md
  icon: rngtech:silica_gel_dehumidifier
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:silica_gel_dehumidifier
  - rngtech:silica_gel_column_casing
  - rngtech:silica_gel_beads
  - rngtech:saturated_silica_gel_beads
  - rngtech:supercharged_silica_gel_beads
  - rngtech:saturated_supercharged_silica_gel_beads
---

# Silica Gel Dehumidifier

<BlockImage id="rngtech:silica_gel_dehumidifier" scale="6" />

<Row>
  <ItemImage id="rngtech:silica_gel_dehumidifier" scale="2" />
  <ItemImage id="rngtech:silica_gel_column_casing" scale="2" />
</Row>

- **Type:** Multiblock water source
- **Stage:** 5
- **Power:** None
- **Gear:** None
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Silica Gel Dehumidifier** is a Stage 5 water source that runs on desiccant beads instead of weather. Each dry bead soaks up a fixed amount of water and comes out saturated. You then dry the saturated beads in a [Furnace](rngtech:furnace.md) and feed them back in.

It needs no power and ignores sky access, biome, rain, and dimension, so it works anywhere, including underground and in the Nether. Your real cost is the Furnace energy spent recharging beads. Like the [Wooden Dehumidifier](rngtech:wooden-dehumidifier.md), it has no rarity, affixes, Refinement, or Mastery.

## Usage

### Building the structure

The only valid shape is a three-block column:

1. Place the <ItemLink id="rngtech:silica_gel_dehumidifier" /> controller.
2. Stack two <ItemLink id="rngtech:silica_gel_column_casing" /> blocks directly on top of it.

If either casing is missing, all three lanes pause. Nothing is lost, and work resumes as soon as the column is complete again.

### Recharging beads

Smelt saturated beads in an RNGTech [Furnace](rngtech:furnace.md) to dry them out again. Recharging gives back exactly the bead you put in, never a bonus bead.

| Input | Output | Time | Energy |
|---|---|---|---|
| <ItemLink id="rngtech:saturated_silica_gel_beads" /> | <ItemLink id="rngtech:silica_gel_beads" /> | 60 s | 28,800 FE |

| Input | Output | Time | Energy |
|---|---|---|---|
| <ItemLink id="rngtech:saturated_supercharged_silica_gel_beads" /> | <ItemLink id="rngtech:supercharged_silica_gel_beads" /> | 120 s | 57,600 FE |

### Getting water out

Pipes can drain water from the controller's four horizontal sides, or from the horizontal sides of either casing. The machine has no bucket slot, so use pipes or a [Universal Cable](rngtech:universal-cable.md).

To throw water away, use the purge button beside the tank, or right-click the controller with a **Purge Bucket** to void up to 1,000 mB.

- [Automation](rngtech:silica-gel-dehumidifier/automation.md)

## Screen

The Silica Gel Dehumidifier has a single screen with three input and output lane pairs, the shared water tank, and its purge button. Hover the gauges for lane progress, water amount, structure state, and status.

## Obtaining

- [Crafting](rngtech:silica-gel-dehumidifier/crafting.md)

### Supercharged beads

<ItemLink id="rngtech:supercharged_silica_gel_beads" /> hold more than three times as much water as normal beads. Make them in an [Alloy Furnace](rngtech:alloy-furnace.md) with Stage 6 parts at 1,450 heat:

| Input | Output | Time | Energy | Min. temperature | Min. component stage |
|---|---|---|---|---|---|
| 4× <ItemLink id="rngtech:silica_gel_beads" /><br /><ItemLink id="rngtech:titanium_dust" /><br /><ItemLink id="minecraft:redstone" /> | <ItemLink id="rngtech:supercharged_silica_gel_beads" /> | 12 s | 6,000 FE | 1,450 | 6 |

### Breaking

Mine the controller or a casing with a pickaxe. Each block drops itself, and the controller also drops the beads in its lanes. Breaking a casing only pauses the machine; the beads and stored water stay in the controller.
