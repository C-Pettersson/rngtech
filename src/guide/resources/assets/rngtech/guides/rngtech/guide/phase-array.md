---
navigation:
  title: "Phase Array"
  parent: refinement.md
  icon: rngtech:exotic_affix_forge
categories:
  - "Refinement"
item_ids:
  - rngtech:exotic_affix_forge
  - rngtech:phase_emitter
  - rngtech:phase_coil
  - rngtech:glyph
---

# Phase Array

<BlockImage id="rngtech:exotic_affix_forge" scale="6" />

<Row>
  <ItemImage id="rngtech:exotic_affix_forge" scale="2" />
  <ItemImage id="rngtech:phase_emitter" scale="2" />
  <ItemImage id="rngtech:phase_coil" scale="2" />
</Row>

- **Type:** Refinement multiblock
- **Stages:** 8
- **Power:** FE consumer
- **Gear:** 9 × <ItemLink id="rngtech:exotic_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Phase Array** is the endgame refinement bench. Where the [Affix Forge](rngtech:affix-forge.md) rolls dice for you, the Array lets you choose: raise this affix, remove that one, add the affix you want, swap one for another, or move an affix from a donor item. Every operation is a **pulse**: the line charges, you fire it, and the Coils hold the pulse or let it slip. A pulse costs FE that grows with how good the item already is, adds **Strain** to the item, and the deeper operations spend a **Glyph**. The Array spends only a little [Refinement Potential](rngtech:rarity-and-affixes.md#refinement-potential), so an item is usually done when you decide it is, not when a counter runs out.

The Array works on any refinable item of any stage: machine items, machine parts, Battery Cells, Tool Heads, Tool Rods, companions, and its own Emitter and Coils. Unique and [Corrupted](rngtech:rarity-and-affixes.md#corruption) items are refused.

## Usage

- [Building the line](rngtech:phase-array/building-the-line.md)
- [Running a pulse](rngtech:phase-array/running-a-pulse.md)
- [Outcomes](rngtech:phase-array/outcomes.md)
- [Costs](rngtech:phase-array/costs.md)

### Gear

The Cradle has nine Battery Cell sockets. Any Battery Cell fits. While the line charges, each cell drains into the charge at its own output rate, so a full set carries a pulse to a line with no cable.

- [Line stats](rngtech:phase-array/line-stats.md)

### Automation

| Side | Behavior |
|---|---|
| Any side of the Cradle, the Emitter, or a Coil | Accepts FE while the line charges, for example from a [Universal Cable](rngtech:universal-cable.md). |
| Bottom of the Cradle | Hands out Battery Cells once they are drained, for example to a hopper. Nothing goes in this way. |

Nothing else moves by automation: the target, the Glyph, the donor, and the output are placed and taken by hand, and every pulse is a click. The target shows in the Cradle's bore while it sits in the chamber.

## Screen

The Cradle has one screen. The line is drawn across the top as it stands, with the Emitter at the left and the target's chamber at the right; the conduit under it fills with the charge. The cell sockets sit on the left, the operation marks and the affix list in the middle, and the one button under the list reads Charge, Stop, Fire, or Pulse. Three lights at the top left show blocked, waiting for input, and live. Hover anything for its exact values.

With JEI installed, the operations are listed under the Phase Array category.

## Obtaining

- [Crafting](rngtech:phase-array/crafting.md)
- [Glyphs](rngtech:phase-array/glyphs.md)

### Breaking

Mine any part with a pickaxe. The Cradle drops itself and everything in its slots, including the Battery Cells. A Coil keeps the Glyph inscribed into it.
