# Auto Balance

Status: Planned

## Summary

Auto Balance evenly redistributes compatible input stacks across a machine's active input slots.

This modifier only has an effect when the machine has more than one input slot and can process from those slots as a bulk-processing machine.

## Applies To

| Category | Applies? | Notes |
|---|---|---|
| Machine block | Planned | Eligible for machines with multiple active input slots. |
| Machine part | Planned | Eligible for input-handling parts if that part can affect machine inventory behavior. |
| Regular item | Deferred | Not part of the initial modifier scope. |
| World block | Out of scope | RNGTech does not apply modifiers to arbitrary world blocks. |

## Slot Eligibility

| Slot | Applies? | Notes |
|---|---|---|
| Implicit | Planned | Suitable for machine or part identities built around bulk processing. |
| Prefix | Planned | Suitable as a rolled convenience modifier. |
| Suffix | Planned | Suitable as a rolled convenience modifier. |
| Enchant | Planned | Suitable as a special automation-focused effect. |

## Effects

| Effect Type | Target | Operation | Tier / Range | Notes |
|---|---|---|---|---|
| Behavioral | Active input slots | Enabled when present | _N/A_ | Rebalances inventory contents instead of changing a numeric machine stat. |

When a compatible item stack is inserted into one active input slot, the machine should count matching items across all active input slots and redistribute them as evenly as stack limits allow.

Example: inserting `40` matching items into the first input slot of a machine with `4` active input slots should leave `10` items in each slot.

If the total does not divide evenly, earlier active input slots receive the remainder. For example, `41` matching items across `4` active input slots becomes `11`, `10`, `10`, and `10`.

## Rules

- Only balance input slots that are currently active for the machine.
- Only balance stacks that are compatible by item and components.
- Do not move items into fuel, output, machine-part, or inactive slots.
- Do not exceed the slot limit or the item stack limit.
- Do not mix different input types into the same balancing pass.

## Incompatible With

- TBD

## Related Concepts

- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
