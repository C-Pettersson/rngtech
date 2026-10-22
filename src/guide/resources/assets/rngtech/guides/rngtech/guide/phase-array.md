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

The **Phase Array** is the endgame refinement bench. Where the [Affix Forge](rngtech:affix-forge.md) rolls dice for you, the Array lets you choose: raise this affix, remove that one, add the affix you want, swap one for another, or move an affix across from a donor item. It works on any refinable item of any stage, and it spends only a little [Refinement Potential](rngtech:rarity-and-affixes.md#refinement-potential), so an item is done when you decide it is.

Every operation is a **pulse**. The line charges FE, you fire it, and the pulse crosses the Coils one by one. Each Coil holds it or lets it slip. Reach far enough and the operation happens; slip early and nothing changes. Three things make you stop before your RP runs out:

- **FE** that grows with how good the item already is.
- **Strain**, one point per pulse, which raises the chance of a Fracture.
- **Glyphs**, which the deeper operations spend and which no recipe makes.

### In one minute

1. Place the **Cradle**, three to nine **Coils** in a straight row from one side of it, and the **Emitter** at the far end. The line lights up when it is complete.
2. Open the Cradle and put a **Glyph** in the seat of its tier. A Tier I Glyph opens the first three positions.
3. Put the item in the Cradle, pick an operation, and read its odds.
4. **Charge**, then **Fire**. Watch the Coils engage. Take the item out of the output.

## Usage

- [Building the line](rngtech:phase-array/building-the-line.md)
- [Opening positions](rngtech:phase-array/opening-positions.md)
- [Running a pulse](rngtech:phase-array/running-a-pulse.md)
- [Outcomes](rngtech:phase-array/outcomes.md)
- [Operations](rngtech:phase-array/operations.md)
- [Costs and Strain](rngtech:phase-array/costs-and-strain.md)

### Gear

The Cradle has nine Battery Cell sockets. Any Battery Cell fits. While the line charges, each cell drains into the charge at its own output rate, so a full set carries a pulse to a line with no cable. Drained cells can be pulled out of the Cradle's bottom face.

- [Line stats](rngtech:phase-array/line-stats.md)

### Automation

| Side | Behavior |
|---|---|
| Any side of the Cradle, the Emitter, or a Coil | Accepts FE while the line charges, for example from a [Universal Cable](rngtech:universal-cable.md). |
| Bottom of the Cradle | Hands out Battery Cells once they are drained, for example to a hopper. Nothing goes in this way. |

Nothing else moves by automation: the target, the Glyph, the donor, and the output are placed and taken by hand, and every pulse is a click.

## Obtaining

- [Crafting](rngtech:phase-array/crafting.md)
- [Glyphs](rngtech:phase-array/glyphs.md)

### Breaking

Mine any part with a pickaxe. The Cradle drops itself and everything in its slots, including the Battery Cells. The seated Glyphs drop with the Cradle; a Coil carries nothing but its own affixes.

## More

- [Screen](rngtech:phase-array/screen.md)
- [Tips](rngtech:phase-array/tips.md)
