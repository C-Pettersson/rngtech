---
navigation:
  title: "Wooden Dehumidifier"
  parent: fluids-and-gases.md
  icon: rngtech:wooden_dehumidifier
categories:
  - "Fluids and gases"
item_ids:
  - rngtech:wooden_dehumidifier
  - rngtech:wooden_dehumidifier_frame
  - rngtech:waterproofed_planks
---

# Wooden Dehumidifier

<BlockImage id="rngtech:wooden_dehumidifier" scale="6" />

<Row>
  <ItemImage id="rngtech:wooden_dehumidifier" scale="2" />
  <ItemImage id="rngtech:wooden_dehumidifier_frame" scale="2" />
</Row>

- **Type:** Multiblock water source
- **Stage:** 2–3
- **Power:** None
- **Gear:** None
- **Mastery:** No
- **Tool:** Any tool

The **Wooden Dehumidifier** is a low-tech multiblock that pulls water out of the air. You build it from one controller and a shell of frames, and it fills itself with water while it can see the sky. It needs no power, so it is an early water source for the algae and gas chains.

It can also turn **Coal Dust** or **Gasification Residue** plus stored water into Carbon Exhaust, the starter carbon input for the [Algae Photobioreactor](rngtech:algae-photobioreactor.md).

Unlike most RNGTech machines, the Wooden Dehumidifier has no rarity, affixes, Refinement, or Mastery.

## Usage

- [Building the structure](rngtech:wooden-dehumidifier/building-the-structure.md)
- [Collecting water](rngtech:wooden-dehumidifier/collecting-water.md)

### Getting fluid out

Click the selected-output square on the screen to choose which tank to empty: Water or Carbon Exhaust. Buckets and other containers in the empty-container slot are filled from the selected tank, and pipes drain the selected tank too.

To throw fluid away, use the purge buttons beside each tank, or right-click the controller with a **Purge Bucket** to void up to 1,000 mB. A normal right-click empties water first, and sneaking empties the converted output first. Purging the water tank resets any conversion in progress.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts conversion items and empty containers. |
| Bottom | Extracts filled containers. Drains the selected output fluid. |
| Sides | Drain the selected output fluid. |
| Formed frames | Accept conversion items and empty containers, and drain the selected output fluid, on behalf of the controller. |

The Dehumidifier never accepts fluid from pipes, and has no FE connection. Unformed frames expose nothing.

## Screen

The Wooden Dehumidifier has a single screen with the conversion slot, the container slots, both tanks with purge buttons, and the selected-output square. Hover the gauges and icons for frame count, humidity, sky access, rain bonus, production rate, and status.

## Obtaining

- [Crafting](rngtech:wooden-dehumidifier/crafting.md)

### Breaking

Break the controller or a frame with any tool, or by hand. Each block drops itself. Breaking the controller also drops the items in its slots. Removing any frame stops the machine until you put it back.
