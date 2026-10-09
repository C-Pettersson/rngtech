# Exotic Affix Forge

Status: Prototype

Player guide: [Exotic Affix Forge](https://c-pettersson.github.io/rngtech/exotic-affix-forge/)

Resource id: `rngtech:exotic_affix_forge`

The Exotic Affix Forge is the Stage 8 powered, recipe-driven refinement station for item stacks. It consumes `rngtech:exotic_affix_catalyst` and FE, and writes per-stack craft history that makes repeat work more expensive. It does not target placed machines in v1.

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Exotic Affix Forge block and item | `rngtech:exotic_affix_forge` |
| Exotic Affix Catalyst item | `rngtech:exotic_affix_catalyst` |
| Recipe type | `rngtech:exotic_affix_forge` |
| Target history data component | `rngtech:exotic_affix_forge_history` |

The placed machine uses `ExoticAffixForgeBlockEntity`, `ExoticAffixForgeMenu`, and `ExoticAffixForgeScreen`. It has active Process, Gear, and Stats tabs, keeps the standard inactive Refine tab position for machine-screen consistency, exposes FE input through the block energy capability, and exposes no item automation capability. Refinement results move into a dedicated output slot instead of replacing the target stack in place.

The forge itself is not rollable, not refinable, and has no station-side RPG traits.

## Processing

The Process tab owns the target slot (current `RefinementTargets`), the catalyst slot, the output slot, the optional rolled-affix or empty-slot selection panel, the six operation rows, and the Apply/Retry button with energy, power-failure, progress, cost, and status previews.

Contract:

- No auto-start. The selected operation begins only when Apply is clicked, and operation and selection controls lock until the craft succeeds or fails.
- Success consumes the target and catalyst, spends Refinement Potential, writes target history, and places the refined stack in the output slot.
- FE is drawn per tick while active. When a tick's FE cannot be drawn, the power-failure meter fills; a full meter fails the craft, resets progress, plays the failure cue, and switches the button to Retry. Failure keeps the target, catalyst, Refinement Potential, and history; FE already spent stays spent. Changing the operation or selection clears the failed state.
- Invalid targets, Unique targets, stripped targets, missing selections, insufficient RP, missing catalyst, missing recipe data, full output, and illegal operations do not start or commit an operation.

## Operations

| Action | Default catalyst | Default FE | Ticks | RP cost | Selection |
|---|---:|---:|---:|---:|---|
| `refine_all` | 2 | 6,000,000 | 3,600 | 2 | No |
| `upgrade_random_modifier` | 3 | 10,000,000 | 4,800 | 0, plus shared `2-6` RP random-upgrade cost | No |
| `refine_selected_modifier` | 3 | 12,000,000 | 4,800 | 4 | Rolled affix |
| `remove_selected_modifier` | 3 | 10,000,000 | 4,000 | 4 | Removable rolled affix |
| `add_modifier` | 4 | 14,000,000 | 5,200 | 0, plus shared `1-8` RP add cost | Empty prefix or suffix slot |
| `reforge` | 4 | 16,000,000 | 6,000 | 0 | No |

`reforge` rerolls rarity, Refinement Potential, and rollable affixes as if the item was newly generated, while preserving the item identity, `rngtech:exotic_affix_forge_history`, and non-trait stack data such as stored Battery Cell energy. Chargeable targets clamp stored energy if the resulting capacity changes.

`refine_all` and `refine_selected_modifier` reroll values inside each affected affix's current tier. `upgrade_random_modifier` and `add_modifier` use the shared refinement legality and operation-specific RP cost rules. `remove_selected_modifier` cannot remove fixed identity or implicit behavior.

## Scaling

Each successful operation increments `totalUses` and that action's count in `rngtech:exotic_affix_forge_history`.

Recipe costs scale from the inserted target's stored history:

```text
scale = 1 + totalUses * total_use_energy_multiplier + actionUses * action_use_energy_multiplier
effectiveEnergy = energy * scale * exoticAffixForge.energyMultiplier
effectiveCatalystCount = min(max_catalyst_count, ceil(catalyst_count * scale))
effectiveTicks = ceil(processing_ticks * exoticAffixForge.timeMultiplier)
```

The default `max_catalyst_count` is `16` for all six built-in action recipes. Datapacks can replace or add `rngtech:exotic_affix_forge` recipes, and config can globally scale FE and processing time.

## Recipe Schema

`rngtech:exotic_affix_forge` recipes support:

- `action`: one of `reforge`, `refine_all`, `upgrade_random_modifier`, `refine_selected_modifier`, `add_modifier`, or `remove_selected_modifier`.
- `target`: optional ingredient filter for the target slot.
- `catalyst`: catalyst ingredient.
- `catalyst_count` and `max_catalyst_count`.
- `energy` and `processing_ticks`.
- `refinement_potential_cost`.
- `total_use_energy_multiplier` and `action_use_energy_multiplier`.

## Gear

The Gear tab exposes one optional Battery Cell slot as backing storage. The forge has a `2,000,000 FE` internal buffer and no machine-side FE intake cap; external intake is limited by free buffer or installed-cell space plus the source or attached Universal Connector.

## JEI

When JEI is installed, `rngtech:exotic_affix_forge` recipes appear under Exotic Affix Forging. The category shows the action, target filter when present, catalyst, base costs, and the per-item history scaling rule. The Process tab registers a clickable JEI recipe area.

## Related

- [Affix Forge](affix-forge.md)
- [Progression](../systems/progression.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
