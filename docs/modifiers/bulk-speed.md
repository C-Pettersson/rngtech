# Bulk Speed

Status: Implemented

`BULK_SPEED` is a rollable behavioral modifier for processing machines. It rewards uninterrupted repeated processing by increasing effective `PROCESSING_SPEED` after each completed process.

## Effect

Each completed process adds `1%` processing speed, up to `100%`.

The bonus is applied as an implicit increased-percent `PROCESSING_SPEED` modifier while the behavior is present. Powered processing machines preserve recipe FE-per-craft behavior, so the shorter processing time raises visible FE/t as the ramp grows.

The ramp resets when the active process chain is interrupted by invalid input, missing required Gear, changed process ingredients, or other machine-specific cycle resets. A completed process keeps the ramp and improves the next process.

## Eligibility

Bulk Speed can roll on current machine block profiles that already support processing speed:

| Profile | Eligible |
|---|---|
| Crusher block | Yes |
| Furnace block | No |
| Electric Furnace block | Yes |
| Potential Reactor block | Yes |
| Component Recycler block | Yes |
| Metal Press block | Yes |
| Melter block | Yes |
| Resonance Calibrator block | Yes |

Stage 0 solid-fuel Furnace, solid fuel-burning generators, Battery Cells, Battery Chassis, and machine parts do not currently roll Bulk Speed.

## Related Pages

- [Affix Generation](../systems/affix-generation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Machine Stats](../reference/machine-stats.md)
