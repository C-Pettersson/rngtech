# Documentation Conventions

## Status Labels

Use these labels when documenting features or content.

| Label | Meaning |
|---|---|
| Implemented | Present in code and expected to work. |
| Prototype | Present but subject to redesign. |
| Planned | Intended design, not implemented yet. |
| Deferred | Intentionally postponed. |
| Out of scope | Not planned for RNGTech unless the scope changes. |

## Canonical Definitions

Define a concept once, then link to it everywhere else.

Good:

```md
Crusher modifiers use [Machine Stats](../reference/machine-stats.md).
```

Avoid:

```md
Processing speed means...
```

unless the page being edited is the canonical definition of processing speed.

Current implementation status belongs in [Current Implementation Matrix](reference/current-implementation.md). Start each content page with one status label so readers arriving through a direct link can distinguish working features from proposals. Label planned subsections when a page mixes current behavior and future design. Tie verification statements to the revision and configuration that were tested.

## Modifier Pages

Each modifier should have its own page when it becomes a concrete design or implementation target.

Modifier pages should document:

- Slot eligibility.
- Item, block, machine, and part eligibility.
- Stat effects.
- Incompatibilities.
- Current implementation status.

Modifier roll mechanics, slots, operations, and tier ranges live in [Affix Generation](systems/affix-generation.md). The grouped modifier inventory lives in [Modifiers Overview](modifiers/index.md).

Use the authoring templates in `docs/page-templates/` when adding new content or modifier pages. These templates are intentionally not included in the rendered documentation navigation.

## Example-Only Concepts

Do not add example concepts as canonical docs until they become real RNGTech design. If an example is useful in a template, mark it clearly as an example and avoid listing it in reference tables.
