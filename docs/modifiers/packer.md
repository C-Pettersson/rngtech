# Packer

Status: Planned

## Summary

Packer combines compatible stackable items across a machine's active output slots.

This modifier only has an effect when the machine has more than one active output slot and those slots contain compatible stackable items.

## Applies To

| Category | Applies? | Notes |
|---|---|---|
| Machine block | Planned | Eligible for machines with multiple active output slots. |
| Machine part | Planned | Eligible for output-handling parts if that part can affect machine inventory behavior. |
| Regular item | Deferred | Not part of the initial modifier scope. |
| World block | Out of scope | RNGTech does not apply modifiers to arbitrary world blocks. |

## Slot Eligibility

| Slot | Applies? | Notes |
|---|---|---|
| Implicit | Planned | Suitable for machine or part identities built around compact output handling. |
| Prefix | Planned | Suitable as a rolled convenience modifier. |
| Suffix | Planned | Suitable as a rolled convenience modifier. |
| Enchant | Planned | Suitable as a special automation-focused effect. |

## Effects

| Effect Type | Target | Operation | Tier / Range | Notes |
|---|---|---|---|---|
| Behavioral | Active output slots | Enabled when present | _N/A_ | Compacts output inventory contents instead of changing a numeric machine stat. |

When compatible stackable items exist in multiple active output slots, the machine should merge them into as few slots as possible while respecting item stack limits and slot limits.

Example: if four active output slots contain `12`, `20`, `4`, and `8` of the same stackable item, Packer should combine them into one stack of `44` if the stack limit allows it.

If the total exceeds one stack, earlier active output slots receive full stacks first. For example, `80` items with a stack limit of `64` becomes `64` and `16`.

## Rules

- Only pack output slots that are currently active for the machine.
- Only combine stacks that are compatible by item and components.
- Do not move items into input, fuel, machine-part, or inactive slots.
- Do not exceed the slot limit or the item stack limit.
- Do not change recipe results or output amounts.
- Do not pack unstackable items.

## Incompatible With

- TBD

## Related Concepts

- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
