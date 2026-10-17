# Dormant Machines

Status: Planned

Planned resource ids: structure sets under `rngtech:dormant/`, a derelict Forestry Companion entity variant

## Summary

The Dormant Machine block, its souls, waking, absorbing, the failed-absorb setting, and the reset and restore commands ship as a Prototype. The [player wiki](https://c-pettersson.github.io/rngtech/dormant-machines/) describes them, the [implementation matrix](../reference/current-implementation.md) lists the code surface, and [Machine Design Notes](../reference/machine-guidelines.md#dormant-machines) hold the design rules.

This page keeps what is still planned: generating the six sites in the world, and the derelict Forestry Companion at the Abandoned Cart Siding. Until then, pack makers place Dormant Machines with `/setblock` or their own structures.

## World Generation

Each site is a small RNGTech structure with one Dormant Machine whose `Site` tag names its soul table.

| Site | Where | Structure idea |
|---|---|---|
| Collapsed Workshop | Overworld surface: plains, forest, taiga, savanna | A roofless stone-and-timber shed with a fallen chimney and scattered scrap. |
| Abandoned Cart Siding | A spur off a mineshaft rail line | A short dead-end rail with buffers and a derelict Forestry Companion on it. |
| Buried Calibration Vault | Overworld below Y 0 | A sealed deepslate chamber with cracked calibration benches. |
| Flooded Foundry | Ocean floor near ruins | A half-buried brick foundry with a broken crucible and an air pocket at the machine. |
| Nether Smeltworks | Nether Wastes, Basalt Deltas | A blackstone smelter platform over lava with collapsed walkways. |
| End Relay | End outer islands | A small purpur-and-obsidian relay pylon on an island edge. |

Spacing should make each site an occasional find, not a regular stop: the Collapsed Workshop roughly as common as a ruined portal, the End Relay rarer than an End city.

## Derelict Forestry Companion

The Abandoned Cart Siding holds a derelict Forestry Companion entity instead of a block. It follows the same rules as the block: one soul, a wake charge filled from FE and nine Battery Cell slots on its screen, absorb only when awake, no owner, and the same failed-absorb setting. Once spent, it breaks into Scrap like a husk.

## Implementation Contract

Planned runtime surface:

- Structures, structure sets, and template pools under `data/rngtech/worldgen/`, with templates under `data/rngtech/structure/`. These are the mod's first generated structures.
- Each template's Dormant Machine carries its `Site` tag. The machine registers itself in `DormantSiteIndex` the first time its chunk loads, as placed machines already do.
- Config: `dormantMachines.enabled` (default on) gates the structure sets through a load condition, like `OreWorldgenEnabledCondition`.
- Forestry: a derelict variant of the Forestry Companion entity with the soul, wake charge, and cell slots of `DormantMachineBlockEntity`, registered in `DormantSiteIndex` by its position when spawned.

Implementation checks:

- GameTests: each site template places one Dormant Machine with a valid `Site`; the derelict cart wakes, absorbs, and spends like the block.
- An audit that each site's top level stays within the training reach of its release stage, which [Machine Design Notes](../reference/machine-guidelines.md#dormant-machines) currently state without a check.

## Open Questions

- Whether the End Relay or Nether Smeltworks should carry a Seal I tier with an ascendancy already chosen. The default keeps Seals as the only ascendancy source.
- Whether waking should draw hostile mobs, such as a wave once the charge passes half. That would turn the expedition into a fight, but it is an automatic cost the player cannot shape with Gear or Mastery.
- Whether the Soul Transfer tooltip should warn about the pack's failed-absorb setting. The setting is common config, which is not synced to clients, so today only the machine's screen shows the warning.

## Related Pages

- [Dormant Machines](https://c-pettersson.github.io/rngtech/dormant-machines/)
- [Machine Mastery](../systems/machine-mastery.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
