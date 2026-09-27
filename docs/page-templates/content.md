# Content Name

Status: Planned

Resource id: `rngtech:example`

## Summary

Describe the item, block, machine, component, or part in one or two paragraphs.

## Behavior

Document gameplay behavior that is specific to this content.

Keep shared definitions linked instead of repeated. For example, link to [Machine Stats](../reference/machine-stats.md) for stat semantics and [Modifier Eligibility](../reference/modifier-eligibility.md) for modifier profile rules.

## Implementation Contract

Current runtime surface:

- Block, item, or resource id: `rngtech:example`.
- Java owner type: `ExampleClass`.
- Menu and screen: TBD.
- Slots: TBD.
- Capabilities: TBD.
- Trait/refinement behavior: TBD.

Implementation checks:

- TBD

## Modifier Eligibility

State the concrete modifier eligibility profile used by this content. If the content has no modifiers, say `Available modifiers: none.`

| Modifier Source | Eligible? | Notes |
|---|---|---|
| Implicit | TBD | Fixed authored identity modifiers from the concrete item, block, or part flavor. |
| Prefix | TBD | List exact stat/modifier definitions, or `None`. |
| Suffix | TBD | List exact stat/modifier definitions, or `None`. |
| Enchant | TBD | Planned, deferred, or out of scope. |

Profile reference: `TBD_PROFILE`. See [Modifier Eligibility](../reference/modifier-eligibility.md#current-code-profiles).

## Base Stats

Status: Planned

List code-backed base stats only. Use a table for blocks, machines, and component families.

| Stat or value | Base | Notes |
|---|---:|---|
| TBD | TBD | TBD |

## Related Pages

- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
