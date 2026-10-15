---
navigation:
  title: "Crusher"
  parent: processing.md
  icon: rngtech:iron_crusher_chassis
categories:
  - "Processing"
item_ids:
  - rngtech:wooden_crusher_chassis
  - rngtech:iron_crusher_chassis
  - rngtech:copper_crusher_chassis
  - rngtech:bronze_crusher_chassis
  - rngtech:steel_crusher_chassis
  - rngtech:aluminum_crusher_chassis
  - rngtech:titanium_crusher_chassis
  - rngtech:tungstensteel_crusher_chassis
  - rngtech:exotic_crusher_chassis
  - rngtech:jam_debris
---

# Crusher

<BlockImage id="rngtech:iron_crusher_chassis" scale="6" />

<Row>
  <ItemImage id="rngtech:wooden_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:iron_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:copper_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:bronze_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:steel_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:aluminum_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:titanium_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:tungstensteel_crusher_chassis" scale="2" />
  <ItemImage id="rngtech:exotic_crusher_chassis" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 0–8
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:iron_crush_head" />, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Crusher** breaks ores and raw metals into crushed material, and crushed material into dust. Crushing an ore yields more metal than smelting it directly, and dust smelts faster and cheaper in the [Furnace](rngtech:furnace.md). It also turns coal into Coal Dust and crops into biomass inputs for the [Bio Generator](rngtech:bio-generator.md) and [Wooden Composter](rngtech:wooden-composter.md).

Every Crusher runs on FE and needs a Crush Head installed. Like every RNGTech machine, each Crusher you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Crushing](rngtech:crusher/crushing.md)
- [Hardness](rngtech:crusher/hardness.md)
- [Output amount](rngtech:crusher/output-amount.md)
- [Stages](rngtech:crusher/stages.md)

### Gear

The Gear tab has two slots:

- one **Crush Head** slot. A head is required. Its stage cannot be higher than the chassis stage, so a Wooden Crusher only takes a Flint Crush Head.
- one **Battery Cell** slot. Without a cell, the Crusher keeps only a tiny internal buffer and loses a quarter of its bonus output.

Crush Heads roll their own affixes, and their stats add to the Crusher's. See [Gear](rngtech:gear.md) for how Gear works.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input slot. Accepts only items with a Crusher recipe; some rolled affixes also reject items that would not fit the output. |
| Bottom | Extracts finished output. |
| Sides | Inserts Battery Cells. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

### Mastery and Ascendancies

Each completed crushing job earns Crusher Mastery XP, and you spend the points on the shared [Machine Mastery](rngtech:machine-mastery.md) tree. Reversible ingot-to-dust recipes earn no XP. Some Crusher keystones change how Gear works, for example removing the Battery Cell slot or allowing a Crush Head one stage higher than the chassis. Crushers choose between the **Rockbreaker** and **Assayer** [ascendancies](rngtech:machine-mastery.md#ascendancies).

## Obtaining

- [Crafting](rngtech:crusher/crafting.md)

### Breaking

Mine a placed Crusher with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## More

- [Screen](rngtech:crusher/screen.md)
