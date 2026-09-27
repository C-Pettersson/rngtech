# Modifier Name

Status: Planned

Use this template when adding a concrete modifier page. Replace this text with the modifier's canonical definition.

## Summary

Describe what the modifier does in one or two sentences.

## Definition

| Field | Value |
|---|---|
| Slot | `PREFIX`, `SUFFIX`, `IMPLICIT`, or `ENCHANT` |
| Stat or behavior | TBD |
| Operation | `ADD`, `INCREASED_PERCENT`, `DECREASED_PERCENT`, `MORE`, `LESS`, or behavioral |
| Tier / range | TBD |
| `CAN_ROLL` | Yes / No |

Use `CAN_ROLL = No` for authored fixed modifiers. Fixed modifiers can still display, serialize, and apply, but random rolls and refinement do not select them.

## Capability Eligibility

List every capability flag that can attach this modifier definition. Keep this aligned with [Modifier Eligibility](../reference/modifier-eligibility.md#capability-modifier-matrix).

| Capability Flag | Applies? | Notes |
|---|---|---|
| `HAS_FE_STORAGE` | TBD |  |
| `HAS_FE_INPUT` | TBD |  |
| `HAS_FE_OUTPUT` | TBD |  |
| `HAS_FE_GENERATION` | TBD |  |
| `HAS_FUEL_SLOT` | TBD |  |
| `HAS_INPUT_SLOT` | TBD |  |
| `HAS_OUTPUT_SLOT` | TBD |  |
| `HAS_PROCESSING` | TBD |  |
| `HAS_HEAT` | TBD |  |
| `HAS_BATTERY_CELLS` | TBD |  |
| `HAS_BURST_TRANSFER` | TBD |  |
| `HAS_IDLE_LOSS` | TBD |  |
| `HAS_GLOBAL_MODIFIER_EFFECTS` | TBD |  |
| `HAS_STABILITY` | TBD |  |
| `HAS_GEAR_STATS` | TBD |  |

## Target Profiles

List concrete profiles that can currently roll or carry this modifier.

| Profile | Rollable? | Notes |
|---|---|---|
| `CRUSHER` | TBD |  |
| `FURNACE` | TBD |  |
| `BATTERY` | TBD |  |
| `BATTERY_CELL` | TBD |  |
| `BATTERY_CHASSIS` | TBD |  |
| `SOLID_FUEL_BURNER` | TBD |  |
| `CRUSH_HEAD` | TBD |  |
| `HEAT_CORE` | TBD |  |
| `FUEL_BOX` | TBD |  |

## Slot Eligibility

| Slot | Applies? | Notes |
|---|---|---|
| Implicit | TBD |  |
| Prefix | TBD |  |
| Suffix | TBD |  |
| Enchant | TBD |  |

## Effects

| Effect Type | Target | Operation | Tier / Range | Notes |
|---|---|---|---|---|
| Numeric | TBD | TBD | TBD | Include tier ranges and the rolled value behavior. |
| Behavioral | TBD | Enabled when present | _N/A_ | Use this shape for boolean-style behavior modifiers. |

## Unique Behavior

State whether Unique items can carry this as a fixed authored modifier. Unique targets do not roll or refine modifiers.

## Incompatible With

- TBD

## Related Concepts

- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
