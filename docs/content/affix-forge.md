# Affix Forge

Status: Prototype

Player guide: [Affix Forge](https://c-pettersson.github.io/rngtech/affix-forge/)

Resource id: `rngtech:affix_forge`

The Affix Forge is the early, unpowered station that applies refinement consumables to modifier-bearing item stacks. The [Exotic Affix Forge](exotic-affix-forge.md) is the late-game powered counterpart.

## Implementation Contract

Current runtime surface:

- Block and item id: `rngtech:affix_forge`.
- Block entity: `AffixForgeBlockEntity`.
- Menu and screen: `AffixForgeMenu` and `AffixForgeScreen`.
- Slots: target slot, catalyst slot, focus slot, Affix Lens Array slot, Affix Modifier Socket slot, and Affix Resonance Matrix slot.
- Target slot accepts current `RefinementTargets`: machine block items, machine parts, and Battery Cell items.
- Catalyst slot accepts any `RefinementConsumableItem` stack. A catalyst whose upgrade is missing stays in the slot, the outcome panel names the missing upgrade, and Reforge refuses it without consuming anything.
- Focus slot accepts modifier lenses and modifier crystals. A lens needs an installed Affix Lens Array and a crystal needs an installed Affix Modifier Socket; otherwise the outcome panel names the missing upgrade and Reforge refuses it. Lenses and crystals cannot be combined in one operation.
- Upgrade slots are reusable Gear-style items saved with the block and dropped with the forge inventory.
- Installed upgrades update placed block visuals through blockstate properties.
- The block currently exposes no registered NeoForge item or energy capability. Interaction is through the menu.
- The modifier panel lists current rolled affixes for inspection only. Add and upgrade operations do not use forge selection.
- The Reforge button mutates the target stack on successful refinement and consumes the catalyst unless a modifier effect preserves it.
- When JEI is installed, refinement consumables appear under an Affix Forging category. This is a dynamic operation view rather than a registered recipe type because the forge mutates the inserted target stack in place.
- Existing early-access 3-slot forge inventories migrate to the full upgrade set on load so existing worlds keep the old functionality.

Implementation checks:

- Shift-clicking refinement consumables should prefer the catalyst slot.
- Shift-clicking modifier lenses and crystals should prefer the focus slot.
- Shift-clicking forge upgrades should prefer their matching upgrade slots.
- Shift-clicking valid targets should prefer the target slot.
- Invalid targets, invalid catalysts, and failed refinement attempts must not consume items or Refinement Potential.
- Successful operations must use `com.rngtech.rpg.refinement` rather than station-specific refinement logic.

## Forge Upgrades

| Upgrade | Resource id |
|---|---|
| Affix Lens Array | `rngtech:affix_lens_array` |
| Affix Modifier Socket | `rngtech:affix_modifier_socket` |
| Affix Resonance Matrix | `rngtech:affix_resonance_matrix` |

## Refinement Consumables

| Item | Resource id | RP cost | Requires |
|---|---|---|---|
| Affix Injector | `rngtech:affix_injector` | `1-8` | — |
| Affix Upgrader | `rngtech:affix_modifier` | `2-6` | — |
| Nullifier Coil | `rngtech:nullifier_coil` | Remove table | — |
| Greater Affix Upgrader | `rngtech:affix_upgrade` | `6-10` | Resonance Matrix |
| Ascension Catalyst | `rngtech:ascension_catalyst` | `4`, target needs at least `5` | Resonance Matrix |
| Ascension Matrix | `rngtech:ascension_matrix` | All remaining | Resonance Matrix |
| Chaos Crystal | `rngtech:chaos_crystal` | Remove table | Resonance Matrix |
| Expansion Crystal | `rngtech:expansion_crystal` | `1-18` per added affix | Resonance Matrix |
| Null Crystal | `rngtech:null_crystal` | Remove table | Resonance Matrix |

RP is [Refinement Potential](../systems/progression.md#refinement-potential).

Selection rules:

- Add, fill, reroll, and broad ascension paths pick a legal family by roll weight, then a legal tier by global tier weight, family tier weight, and component-stage weighting. Chaos Crystal rerolls replacement families and tiers the same way.
- Rarity controls open affix slots, not tier eligibility.
- Tier 7 is limited to Stage 8 Exotic targets and can appear from add, fill, and full-reroll paths, but not from upgrade or ascension actions.
- The final tier-aware cost roll is capped by the target's remaining RP, so stronger affixes usually consume more budget and can drain all remaining RP. There is no separate success percentage.
- Expansion Crystal and Ascension Matrix additions are capped by open legal slots and affordable tiers. Ascension Matrix adds weighted `1-4` modifiers and upgrades one pre-existing rolled affix up to the normal upgrade tier cap, or retunes it if it is already capped.
- Greater Affix Upgrader tunes a capped retunable affix instead of upgrading it.

Remove and reroll operations consume weighted random Refinement Potential on success:

| Cost | Weight |
|---:|---:|
| 1 | 50% |
| 2 | 30% |
| 3 | 15% |
| 4 | 5% |

Fixed identity behavior plus machine, component, and Battery Cell base stat profiles are not rolled affixes and are never added or removed by Affix Forge consumables.

## Modifier Lenses

A matching lens tag applies a `3x` definition weight multiplier when choosing the new or upgraded affix. Lenses do not affect tier weights, RP costs, rarity rules, slot limits, or modifier conflicts. Non-matching candidates stay in the pool while at least one eligible matching candidate exists; with none, the operation is blocked. The outcome preview reports matching and non-matching candidate counts for the current target.

| Lens | Resource id | Bias tag |
|---|---|---|
| Power Modifier Lens | `rngtech:power_modifier_lens` | `POWER` |
| Speed Modifier Lens | `rngtech:speed_modifier_lens` | `SPEED` |
| Yield Modifier Lens | `rngtech:yield_modifier_lens` | `YIELD` |
| Stability Modifier Lens | `rngtech:stability_modifier_lens` | `STABILITY` |
| Control Modifier Lens | `rngtech:control_modifier_lens` | `CONTROL` |
| Kinetic Modifier Lens | `rngtech:kinetic_modifier_lens` | `KINETIC` speed and motion family |
| Efficiency Modifier Lens | `rngtech:efficiency_modifier_lens` | `EFFICIENCY` legacy family |

The Moddex Pool table exposes the same lens tags as tag chips and can filter the current profile's pool by tag.

## Refinement Modifiers

| Item | Resource id |
|---|---|
| Conservation Crystal | `rngtech:conservation_crystal` |
| Frugality Crystal | `rngtech:frugality_crystal` |
| Transmutation Crystal | `rngtech:transmutation_crystal` |
| Resonance Crystal | `rngtech:resonance_crystal` |
| Destabilization Crystal | `rngtech:destabilization_crystal` |
| Stabilization Crystal | `rngtech:stabilization_crystal` |

Transmutation Crystal picks the replacement as another weighted legal affix in the same prefix or suffix slot at the target upgrade tier.

Stabilization Crystal is meant to prevent corruption when a refinement operation would corrupt the target. The Affix Forge has no corruption outcome storage yet, so the crystal is accepted as a forward-compatible ward and is not consumed.

## Shared Implementation Rule

All refinement entry points must use the shared refinement library in `com.rngtech.rpg.refinement`.

Current entry points:

- Affix Forge item-stack refinement.
- Machine screen Refinement tabs for placed machines. Crusher, Furnace, Battery Chassis, and future compatible machines share the same tab content, and each machine passes its own machine type into the refinement rules so legal affix pools stay machine-aware.

## Related

- [Exotic Affix Forge](exotic-affix-forge.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
- [Progression](../systems/progression.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
- [Rarity](../reference/rarity.md)
