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

You can read a Crusher from the outside. The installed Crush Head is the pair of toothed rolls in the open front, in the head's material; with no head you see only the bare shafts. The rolls turn toward each other while the Crusher works, faster when a craft is shorter, and grit of the input falls below them. Items in the input slot wait in the hopper on top and shake while they are crushed, and finished items collect in the bin at the bottom of the front. A jam stops the rolls with a shudder, wedges the input between them, puffs smoke, and blinks the lamp above the opening red. Facing the front, the Battery Cell lies in the cradle on the left with a charge meter beside it, and the motor sits on the right. Cables connect on any side, but the cable port is drawn on the back.

Each chassis has its own casing, and the rim of its hopper is the metal at the top of its recipe. The Iron, Copper, and Bronze Crushers stand on smooth stone. The Tungstensteel and Exotic Crushers, which crush several items per cycle, have a second, smaller pair of rolls under the first. The <ItemLink id="rngtech:wooden_crusher_chassis" /> is a plank frame on wooden posts instead, with no lamp or meter; a Flint Crush Head shows as wooden rolls studded with flint.

## Usage

- [Crushing](rngtech:crusher/crushing.md)
- [Hardness](rngtech:crusher/hardness.md)
- [Output amount](rngtech:crusher/output-amount.md)
- [Stages](rngtech:crusher/stages.md)
- [Gear](rngtech:crusher/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input slot. Accepts only items with a Crusher recipe; some rolled affixes also reject items that would not fit the output. |
| Bottom | Extracts finished output. |
| Sides | Inserts Battery Cells. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

- [Mastery and Ascendancies](rngtech:crusher/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:crusher/crafting.md)

### Breaking

Mine a placed Crusher with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## More

- [Screen](rngtech:crusher/screen.md)
