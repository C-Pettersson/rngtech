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

## Usage

- [Recycling](rngtech:component-recycler/recycling.md)
- [Crude Recycler and Hand Crank](rngtech:component-recycler/crude-recycler-and-hand-crank.md)

### Stages

| Stage | Recycler | Notes |
|---:|---|---|
| 2 (manual) | <ItemLink id="rngtech:crude_recycler" /> | Hand-cranked, no FE, no Gear, no traits. |
| 1 | <ItemLink id="rngtech:iron_component_recycler" /> | First powered Recycler. |
| 2 | <ItemLink id="rngtech:copper_component_recycler" /> | |
| 3 | <ItemLink id="rngtech:bronze_component_recycler" /> | |
| 4 | <ItemLink id="rngtech:steel_component_recycler" /> | |
| 5 | <ItemLink id="rngtech:aluminum_component_recycler" /> | |
| 6 | <ItemLink id="rngtech:titanium_component_recycler" /> | |
| 7 | <ItemLink id="rngtech:tungstensteel_component_recycler" /> | |
| 8 | <ItemLink id="rngtech:exotic_component_recycler" /> | |

Each recipe has a minimum processing level. To run it, both the Recycler's stage and the installed Disassembly Head's processing level must reach that level.

- [Gear](rngtech:component-recycler/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts items to recycle. |
| Bottom | Extracts from the three output slots. |
| Sides | Powered Recyclers accept FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear slots are not reachable by automation. The Crude Recycler has no FE input, and its top is taken by the Hand Crank.

## Screen

Powered Recyclers have four tabs:

- **Process**: the input, three outputs, progress, and energy. With JEI installed, click the progress bar to browse recycling recipes.
- **Gear**: Disassembly Head, Recovery Filter, and Battery Cell slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Recycler's traits with a catalyst.

The Crude Recycler has only its Process screen.

## Obtaining

- [Crafting](rngtech:component-recycler/crafting.md)

### Breaking

Mine a placed Component Recycler with a pickaxe. It drops itself, and any items inside drop as well. The Hand Crank breaks with any tool.
