---
navigation:
  title: "Exotic Affix Forge"
  parent: refinement.md
  icon: rngtech:exotic_affix_forge
categories:
  - "Refinement"
item_ids:
  - rngtech:exotic_affix_forge
  - rngtech:exotic_affix_catalyst
---

# Exotic Affix Forge

<BlockImage id="rngtech:exotic_affix_forge" scale="6" />

- **Type:** Refinement station
- **Stages:** 8
- **Power:** FE consumer
- **Gear:** <ItemLink id="rngtech:exotic_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Exotic Affix Forge** is the endgame refinement station. Where the [Affix Forge](rngtech:affix-forge.md) rolls dice for you, the Exotic Forge lets you choose: pick an affix to refine or remove, pick an empty slot to fill, or reforge the whole item. Each operation costs millions of FE and several <ItemLink id="rngtech:exotic_affix_catalyst" />, and every operation on the same item makes the next one more expensive.

The Exotic Forge refines items only. Placed machines still use their own Refinement tab.

## Usage

- [Running an operation](rngtech:exotic-affix-forge/running-an-operation.md)

### Power failure

The forge draws FE every tick while it works. Each tick it cannot draw enough fills the power-failure meter a little, and each powered tick drains it again. After 100 unpowered ticks (5 seconds) the meter is full: the operation fails with an audible cue, progress resets, and the button changes to **Retry**.

A failure keeps your item, catalysts, and RP. Only the FE already spent is lost. Make sure your supply, plus the internal buffer and Battery Cell, can carry the whole operation.

- [Operations](rngtech:exotic-affix-forge/operations.md)
- [Rising costs](rngtech:exotic-affix-forge/rising-costs.md)

### Gear

The Gear tab has one optional **Battery Cell** slot. Any Battery Cell fits. The forge also has a 2,000,000 FE internal buffer and no input limit of its own: it takes FE as fast as its buffer and cell have room and your supply or connector tier allows. A large cell lets a long operation keep running when your supply cannot match the draw.

### Automation

| Side | Behavior |
|---|---|
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

The forge has no item automation.

## Screen

The Exotic Affix Forge screen has three working tabs:

- **Process**: target, catalyst, and output slots, the affix and empty-slot selection panel, operation buttons, Apply/Retry, and energy, power-failure, progress, and cost readouts. With JEI installed, click the recipe area to open the Exotic Affix Forging category.
- **Gear**: the Battery Cell slot.
- **Stats**: the forge's current stats.

## Obtaining

- [Crafting](rngtech:exotic-affix-forge/crafting.md)

### Breaking

Mine a placed Exotic Affix Forge with a pickaxe. It drops itself and everything in its slots, including the Battery Cell.
