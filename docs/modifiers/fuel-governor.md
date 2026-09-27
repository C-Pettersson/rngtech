# Fuel Governor

Status: Prototype

## Summary

Fuel Governor prevents burnable fuel from being wasted while a fuel-burning energy generator has no internal energy storage space.

When present, the machine pauses fuel use while its internal energy buffer is full.

Current code note: Copper, Bronze, and Steel Fuel Boxes provide fixed Fuel Governor behavior for the concrete [Solid Fuel Burning](../content/solid-fuel-burner.md) generator chassis. Rolled named Fuel Governor modifiers are still planned.

## Applies To

| Category | Applies? | Notes |
|---|---|---|
| Machine block | Prototype | Active through concrete solid fuel-burning generator chassis. |
| Machine part | Prototype | Active as fixed behavior on Copper, Bronze, and Steel Fuel Boxes. |
| Regular item | Deferred | Not part of the initial modifier scope. |
| World block | Out of scope | RNGTech does not apply modifiers to arbitrary world blocks. |

## Slot Eligibility

| Slot | Applies? | Notes |
|---|---|---|
| Implicit | Prototype | Implemented as fixed Fuel Box behavior; generator identity modifiers remain planned. |
| Prefix | Deferred | Fuel Governor is a behavioral control modifier, not a capacity-style prefix. |
| Suffix | Planned | Suitable as a future rolled fuel-control modifier. |
| Enchant | Planned | Suitable as a special automation-focused effect. |

## Effects

| Effect Type | Target | Operation | Tier / Range | Notes |
|---|---|---|---|---|
| Behavioral | Fuel burn clock | Enabled when present | _N/A_ | Pauses fuel consumption while the internal energy buffer is full. |

While the modifier is active and the machine's internal energy buffer is full:

- Do not start burning a new fuel item.
- Do not decrement active burn time.
- Do not apply heat-isolation burn-time leakage.
- Continue exporting stored energy normally.

When internal energy space becomes available again, the machine resumes burning from the remaining burn time or starts the next fuel item according to its normal fuel rules.

## Rules

- Only pause fuel when the machine cannot store more generated energy.
- Do not block normal energy export.
- Do not reduce fuel cost, fuel duration, or energy generation while the machine is able to store generated energy.
- Do not affect machines that consume energy instead of producing it from burnable fuel.

## Incompatible With

- TBD

## Related Concepts

- [Solid Fuel Burning](../content/solid-fuel-burner.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
