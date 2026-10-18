---
navigation:
  title: "Component Recycler"
  parent: processing.md
  icon: rngtech:iron_component_recycler
categories:
  - "Processing"
item_ids:
  - rngtech:crude_recycler
  - rngtech:hand_crank
  - rngtech:iron_component_recycler
  - rngtech:copper_component_recycler
  - rngtech:bronze_component_recycler
  - rngtech:steel_component_recycler
  - rngtech:aluminum_component_recycler
  - rngtech:titanium_component_recycler
  - rngtech:tungstensteel_component_recycler
  - rngtech:exotic_component_recycler
---

# Component Recycler

<BlockImage id="rngtech:iron_component_recycler" scale="6" />

<Row>
  <ItemImage id="rngtech:crude_recycler" scale="2" />
  <ItemImage id="rngtech:iron_component_recycler" scale="2" />
  <ItemImage id="rngtech:copper_component_recycler" scale="2" />
  <ItemImage id="rngtech:bronze_component_recycler" scale="2" />
  <ItemImage id="rngtech:steel_component_recycler" scale="2" />
  <ItemImage id="rngtech:aluminum_component_recycler" scale="2" />
  <ItemImage id="rngtech:titanium_component_recycler" scale="2" />
  <ItemImage id="rngtech:tungstensteel_component_recycler" scale="2" />
  <ItemImage id="rngtech:exotic_component_recycler" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** Crude (manual), 1–8
- **Power:** Hand Crank (Crude), FE (Stages 1–8)
- **Gear:** <ItemLink id="rngtech:iron_disassembly_head" />, <ItemLink id="rngtech:iron_recovery_filter" />, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Component Recycler** takes RNGTech machines, machine parts, modular tool parts, calibrated components, and Battery Cells apart and gives back some of the materials used to make them. It is how you recover a failed calibration, an outgrown machine, or a Gear part you no longer want.

Returns are always physical parts only. A recycled item's rarity, affixes, and Refinement Potential are lost, so strip valuable items in a [Potential Reactor](rngtech:potential-reactor.md) first if you want energy for them.

You can read a powered Recycler from the outside. The installed Disassembly Head hangs in the open front as a claw in the head's material; with no head you see only the bare spindle. While the Recycler works, the claw turns, faster when a craft is shorter, and reaches down onto the item on the turntable, throwing off sparks and bits of the item. Each output slot has its own tray under the grated floor, and its returns stand in it. Facing the front, the Battery Cell lies in the cradle on the left of the back cabinet with a charge meter beside it, and an installed Recovery Filter stands as a cartridge in the cage on the right. Each chassis has its own casing. FE still enters on any of the four sides, but the cable port is drawn on the back.

The <ItemLink id="rngtech:crude_recycler" /> is a plank crate instead. Through its front window you can see a flint-studded stone on a cobblestone bed, with the item to recycle lying on it and its returns standing in front. The stone turns while you turn the Hand Crank on the lid.

## Usage

- [Recycling](rngtech:component-recycler/recycling.md)
- [Crude Recycler and Hand Crank](rngtech:component-recycler/crude-recycler-and-hand-crank.md)
- [Stages](rngtech:component-recycler/stages.md)
- [Gear](rngtech:component-recycler/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts items to recycle. |
| Bottom | Extracts from the three output slots. |
| Sides | Powered Recyclers accept FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear slots are not reachable by automation. The Crude Recycler has no FE input, and its top is taken by the Hand Crank.

## Obtaining

- [Crafting](rngtech:component-recycler/crafting.md)

### Breaking

Mine a placed Component Recycler with a pickaxe. It drops itself, and any items inside drop as well. The Hand Crank breaks with any tool.

## More

- [Screen](rngtech:component-recycler/screen.md)
