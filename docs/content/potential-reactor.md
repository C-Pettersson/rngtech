# Potential Reactor

Resource id: `rngtech:potential_reactor`

Status: Prototype

Player guide: [Potential Reactor](https://c-pettersson.github.io/rngtech/potential-reactor/)

## Summary

The Potential Reactor is the current code-backed Energy Recycler compatibility path. It converts explicit salvage fuel recipes and RPG-bearing machine, part, and Battery Cell stacks into FE, and outputs a `rngtech:recycling_stripped` copy of RPG targets as [Component Recycler](component-recycler.md) feedstock.

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Potential Reactor block | `rngtech:potential_reactor` |
| Recipe type | `rngtech:potential_reactor` |
| Stripped marker | `rngtech:recycling_stripped` |
| Reactor Chambers | `rngtech:iron_reactor_chamber`, `rngtech:copper_reactor_chamber`, `rngtech:bronze_reactor_chamber`, `rngtech:steel_reactor_chamber` |
| Recovery Filters | `rngtech:iron_recovery_filter`, `rngtech:copper_recovery_filter`, `rngtech:bronze_recovery_filter`, `rngtech:steel_recovery_filter` |
| Containment Linings | `rngtech:iron_containment_lining`, `rngtech:copper_containment_lining`, `rngtech:bronze_containment_lining`, `rngtech:steel_containment_lining` |

The placed machine uses `PotentialReactorBlockEntity`, `PotentialReactorMenu`, and `PotentialReactorScreen`. It supports Process, Gear, Stats, and Refinement tabs.

## Screen Contract

The Potential Reactor screen follows the shared low-text machine UI rule in [Machine Guidelines](../reference/machine-guidelines.md): exact FE, progress ticks, recipe fuel value, status, generation rate, and last-tick FE output belong in hover details rather than permanent text rows.

## RPG Energy Recycling

The Potential Reactor accepts a stack that is already a valid refinement target when the stack is not Unique, is not unidentified, is not already stripped, and has at least one recyclable value:

- rarity above Normal
- rolled prefix or suffix
- remaining Refinement Potential

Stored `rngtech:machine_traits` are used when present. Machine parts that define authored Refinement Potential through their part identity also count, so a plain Steel Resonance Coil can still pay out its part RP. Authored base stats and fixed identity behavior do not pay FE by themselves. For example, a plain Steel Heat Core is still physically valuable, but its base component stats do not create modifier energy beyond the part's own RP.

The payout starts from rarity value, affix tier value, and Refinement Potential value, then scales with what the item was, so the reactor recoups retired gear instead of paying for cheap crafts:

| Factor | Scale |
|---|---|
| Item stage | `0.25 + 0.25 x stage`: Stage 0 junk keeps a quarter, Stage 4 gear `1.25x`, Stage 8 gear `2.25x` |
| Machine Mastery | `+1%` per Mastery level above 1 |
| Ascendancy Seals | `+200%` per Seal tier used on the machine, so a fully ascended machine is worth `7x` |

Repeats lose value. The reactor remembers the item ids of its last 15 gear burns; each earlier burn of the same id multiplies the next one's FE by `0.75`, down to `10%`. Rotating 16 or more different items avoids the penalty. Explicit fuel recipes such as Broken Circuits are not affected.

These factors apply before machine stat multipliers. The output is always a one-count stripped copy of the input item.

## Fuel Recipes

Potential Reactor fuel is explicit recipe data. A recipe defines:

- `ingredient`
- `energy`
- `processing_ticks`
- optional `residue`
- optional `minimum_material_stage`

Example:

```json
{
  "type": "rngtech:potential_reactor",
  "ingredient": {
    "item": "rngtech:fragment"
  },
  "energy": 4800,
  "processing_ticks": 120,
  "residue": {
    "count": 1,
    "id": "rngtech:scrap"
  },
  "minimum_material_stage": 2
}
```

Starter recipes live in `data/rngtech/recipe/potential_reactor/`. JEI exposes them as a Potential Reactor recipe category.

The built-in `rngtech:malformed_ingot` recovery recipe reads the stack's `rngtech:material` component to choose matching nuggets, falling back to generic `rngtech:scrap`.

Residue is only produced when a Recovery Filter is installed. The starter datapack keeps `rngtech:scrap` as residue-only material so recovered residue cannot be fed back into the reactor for more FE.

## Power Model

The current reactor uses a deterministic first-pass model:

```text
effective FE = recipe energy * ENERGY_GENERATION * EFFICIENCY * STABILITY
```

For RPG-bearing targets, "recipe energy" is replaced by the computed RPG value. For explicit fuel recipes, the recipe `energy` field is still used.

`PROCESSING_SPEED` shortens the processing time. Higher `ENERGY_GENERATION` therefore raises the visible FE/t and total recovered FE. The reactor has no machine-side export cap: sides export whatever is stored, and the attached receiver or [Universal Connector tier](basic-wire.md#energy-transfer-limits) decides the rate. `ENERGY_CAPACITY` controls the internal buffer size.

## Gear

The Potential Reactor exposes three Gear-tab slots:

| Gear Slot | Required? | Runtime role |
|---|---|---|
| Reactor Chamber | Yes | Provides `PROCESSING_LEVEL`, `ENERGY_GENERATION`, and chamber stability. Recipe `minimum_material_stage` checks this installed level. |
| Recovery Filter | No | Enables residue output and can improve `EFFICIENCY` and `PROCESSING_SPEED`. |
| Containment Lining | No | Improves `STABILITY`, which currently modifies recovered FE deterministically. |

Reactor Chamber stage is the current material gate. Starter salvage recipes require stage `1` to `3`, and stronger chambers unlock higher-stage inputs.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert valid Potential Reactor fuel recipes or RPG-bearing energy-recycling targets. |
| Bottom | Extract residue or stripped target output. |
| Sides | Extract FE. |

Gear slots and Refinement catalyst slots are not exposed through sided automation. A redstone signal pauses processing.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine prefix | Can roll energy capacity and efficiency modifiers. |
| Machine affixes | Can roll flat prefix `ENERGY_GENERATION`; suffix percent `ENERGY_GENERATION`, `PROCESSING_SPEED`, and `STABILITY`. |
| Reactor Chamber base profile | Provides authored `PROCESSING_LEVEL`, `ENERGY_GENERATION`, and `STABILITY`; `PROCESSING_LEVEL` remains a hard gate. |
| Reactor Chamber rolled modifiers | Can roll flat prefix and percent suffix `ENERGY_GENERATION`, plus `PROCESSING_SPEED` and `STABILITY`. |
| Recovery Filter rolled modifiers | Can roll `EFFICIENCY` and `PROCESSING_SPEED`. |
| Containment Lining rolled modifiers | Can roll `STABILITY`. |
| Machine enchant | Reserved for special salvage behavior such as preserving part materials or extracting affix residue. |

Potential Reactor machine stacks and placed machines use the `POTENTIAL_REACTOR` modifier eligibility profile. Reactor Chambers, Recovery Filters, and Containment Linings use their own part profiles.

## Compatibility Direction

The long-term PRD language calls this role "Energy Recycler." The current implementation keeps the existing `rngtech:potential_reactor` block and parts as the compatibility bridge instead of introducing a second FE-generating recycler id in the same pass.

Future work may rename or migrate this surface to staged Energy Recycler chassis. Until there is a save-safe migration plan, `rngtech:potential_reactor` remains registered and documented here.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Crafting and Upgrades](../systems/crafting.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
