# Potential Reactor

Resource id: `rngtech:potential_reactor`

Status: Prototype

## Summary

The Potential Reactor is the current code-backed Energy Recycler compatibility path. It consumes explicit RNGTech recycling inputs and converts them into FE, with optional residue recovery when a Recovery Filter is installed. It can also consume non-Unique machine, part, and Battery Cell stacks that still carry recyclable RPG data.

When the input carries recyclable rarity, rolled prefixes or suffixes, or Refinement Potential, the Potential Reactor pays FE for that RPG value and outputs a `rngtech:recycling_stripped` copy of the original item. The stripped copy keeps enough item identity for [Component Recycler](component-recycler.md) recipes, but cannot be placed, refined, charged as a Battery Cell, or installed as Gear.

This means the current survival chain is:

```text
Chest -> Potential Reactor -> Component Recycler -> chest or storage
```

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

The Potential Reactor screen follows the shared low-text machine UI rule. The Process tab should present the salvage input slot, residue output slot, internal FE bar, processing bar, and compact status/generation/output icons. Exact FE, progress ticks, recipe fuel value, current status, generation rate, and output rate belong in hover details rather than permanent text rows.

The Gear tab should present the Reactor Chamber, Recovery Filter, and Containment Lining as equipment slots, plus a compact processing-level meter for the installed chamber. Persistent numeric stat rows belong in the Stats tab, and placed-machine trait/refinement details belong in the Refinement tab.

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

These factors apply before machine stat multipliers. The output is always a one-count stripped copy of the input item. This stripped output is the intended feedstock for the Component Recycler.

Unique stacks are not accepted by this generic RPG-value path.

## Fuel Recipes

Potential Reactor fuel is explicit recipe data. A recipe defines:

- `ingredient`
- `energy`
- `processing_ticks`
- optional `residue`
- optional `minimum_material_stage`

With JEI installed, Potential Reactor fuel recipes are exposed with input, residue output when present, base FE, processing ticks, and minimum material stage. In the machine screen, clicking the Process tab recipe line opens the Potential Reactor recipe category.

Current starter recipes:

| Input | Base FE | Processing ticks | Minimum stage | Residue with Recovery Filter |
|---|---:|---:|---:|---|
| `rngtech:fragment` | `4800` | `120` | `2` | `rngtech:scrap` |
| `rngtech:recycling_byproduct` | `7200` | `140` | `2` | `rngtech:scrap` |
| `rngtech:spent_catalyst` | `12000` | `160` | `3` | `rngtech:scrap` |

The Potential Reactor also has a built-in recovery recipe for `rngtech:malformed_ingot`, the generic Metal Press failure output. This recipe reads the stack's `rngtech:material` component. With a Recovery Filter installed, known material failures return two matching nuggets; malformed ingots without a known material component return two generic `rngtech:scrap`.

Residue is only produced when a valid Recovery Filter is installed. Without a filter, the reactor can still process valid fuel, but byproducts are lost. The starter datapack keeps `rngtech:scrap` as residue-only material so recovered residue cannot be fed back into the reactor for more FE.

## Power Model

The current reactor uses a deterministic first-pass model:

```text
effective FE = recipe energy * ENERGY_GENERATION * EFFICIENCY * STABILITY
```

For RPG-bearing targets, "recipe energy" is replaced by the computed RPG value. For explicit fuel recipes, the recipe `energy` field is still used.

`PROCESSING_SPEED` shortens the processing time. Higher `ENERGY_GENERATION` therefore raises the visible FE/t and total recovered FE. `ENERGY_TRANSFER` controls side extraction from the internal FE buffer. `ENERGY_CAPACITY` controls the internal buffer size.

The reactor pauses while its internal buffer is full, so expensive salvage fuel is not consumed just to waste generated FE.

## Gear

The Potential Reactor exposes three Gear-tab slots:

| Gear Slot | Required? | Runtime role |
|---|---|---|
| Reactor Chamber | Yes | Provides `PROCESSING_LEVEL`, `ENERGY_GENERATION`, and chamber stability. Recipe `minimum_material_stage` checks this installed level. |
| Recovery Filter | No | Enables residue output and can improve `EFFICIENCY` and `PROCESSING_SPEED`. |
| Containment Lining | No | Improves `STABILITY`, which currently modifies recovered FE deterministically. |

Reactor Chamber stage is the current material gate. The starter salvage recipes require at least stage 2, and stronger chambers unlock higher-stage inputs.

## Automation

The Potential Reactor uses standard NeoForge capabilities:

| Side | Behavior |
|---|---|
| Top | Insert valid Potential Reactor fuel recipes or RPG-bearing energy-recycling targets. |
| Bottom | Extract residue or stripped target output. |
| Sides | Extract FE. |

Gear slots and Refinement catalyst slots are not exposed through normal sided automation.

The block also supports simple redstone control. A powered Potential Reactor pauses processing and will not start or advance salvage fuel while the signal is present.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine prefix | Can roll energy capacity and efficiency modifiers. |
| Machine affixes | Can roll flat prefix `ENERGY_GENERATION`; suffix percent `ENERGY_GENERATION`, `ENERGY_TRANSFER`, `PROCESSING_SPEED`, and `STABILITY`. |
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
