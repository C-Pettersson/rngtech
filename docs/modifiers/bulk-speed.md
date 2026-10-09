# Bulk Speed

Status: Implemented

Player guide: [Bulk Speed](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#bulk-speed)

`BULK_SPEED` is a rollable behavioral modifier in the `processing_speed` group for processing machines.

## Effect

Each completed process adds `1%` processing speed, up to `100%`. The bonus is applied as an implicit `INCREASED_PERCENT PROCESSING_SPEED` modifier while the behavior is present. Powered machines keep recipe FE per craft, so FE/t rises with the ramp.

The ramp resets on invalid input, missing required Gear, changed process ingredients, or other machine-specific cycle resets.

## Eligibility

Bulk Speed can roll on current machine block profiles that already support processing speed:

| Profile | Eligible |
|---|---|
| Crusher block | Yes |
| Furnace block | No |
| Electric Furnace block | Yes |
| Alloy Furnace block | Yes |
| Component Assembler block | Yes |
| Potential Reactor block | Yes |
| Corrosion Cell block | Yes |
| Component Recycler block | Yes |
| Metal Press block | Yes |
| Melter block | Yes |
| Resonance Calibrator block | Yes |

Stage 0 solid-fuel Furnace, solid fuel-burning generators, Battery Cells, Battery Chassis, and machine parts do not currently roll Bulk Speed. The Cavitation Generator, Vacuum Collapse Generator, Syngas Combustor, and Ammonia Fuel Cell do not roll it either: they never applied it, so it was a dead roll. Among generators only the Potential Reactor and Corrosion Cell use it, where it can double FE/t at its cap.

The Coal Gasifier, Steam Methane Reformer, and Ammonia Synthesizer still roll Bulk Speed but do not apply it yet.

## Related Pages

- [Affix Generation](../systems/affix-generation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Machine Stats](../reference/machine-stats.md)
