---
navigation:
  title: "Vacuum Collapse Generator"
  parent: power-generation.md
  icon: rngtech:vacuum_collapse_generator
categories:
  - "Power generation"
item_ids:
  - rngtech:vacuum_collapse_generator
---

# Vacuum Collapse Generator

<BlockImage id="rngtech:vacuum_collapse_generator" scale="6" />

- **Type:** Generator
- **Stages:** 7
- **Power:** FE generator
- **Gear:** <ItemLink id="rngtech:tungstensteel_void_chamber" />, <ItemLink id="rngtech:tungstensteel_collapse_nozzle" />, <ItemLink id="rngtech:tungstensteel_dimensional_stabilizer" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Vacuum Collapse Generator** is RNGTech's Stage 7 primary generator. It collapses one **Void Catalyst** at a time into a huge batch of FE, about 1,000 FE/t before Gear, and can leave **Collapse Residue** behind. Its Gear decides how much it makes and how stable each collapse is. See [Power Chains](rngtech:power-chains/stage-7-8-void-catalyst.md) for the chain that makes its catalyst.

The generator is built around its Gear. The Void Chamber is the glass vessel in the middle, a Dimensional Stabilizer pylon stands on each side, and the Collapse Nozzle hangs over the chamber from the catalyst hopper's feed line. Each part shows in its own material, so you can tell the tier at a glance, and an empty Gear slot leaves an empty mount.

Like every RNGTech machine, each Vacuum Collapse Generator you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Generating power](rngtech:vacuum-collapse-generator/generating-power.md)
- [The collapse curve](rngtech:vacuum-collapse-generator/the-collapse-curve.md)
- [Collapse depth](rngtech:vacuum-collapse-generator/collapse-depth.md)

### Instability

Every recipe adds instability, collapse depth multiplies it, and higher stability reduces it. Instability makes each collapse slower and yield less FE, and every full point of it adds an extra Collapse Residue. If the installed Collapse Nozzle's stage is lower than the recipe's minimum chamber stage, each missing stage adds a full point of instability. An unstable collapse makes the chamber's rim light flicker instead of pulsing smoothly.

- [Gear](rngtech:vacuum-collapse-generator/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts catalysts. |
| Bottom | Extracts Collapse Residue. |
| Sides | Output FE, for example into a [Universal Cable](rngtech:universal-cable.md). |

Gear slots are not reachable by automation. The back of the machine has a port plate sized for a Universal Connector, the tidiest place to take FE from it.

- [Void Catalyst](rngtech:vacuum-collapse-generator/void-catalyst.md)

## Obtaining

### Crafting

The recipe itself uses a full set of Tungstensteel Gear, so you will need to craft a second set to run it.

<RecipesFor id="rngtech:vacuum_collapse_generator" />

### Breaking

Mine a placed Vacuum Collapse Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## More

- [Screen](rngtech:vacuum-collapse-generator/screen.md)
