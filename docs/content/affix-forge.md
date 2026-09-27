# Affix Forge

Status: Prototype


Resource id: `rngtech:affix_forge`

The Affix Forge applies refinement consumables to modifier-bearing RNGTech item stacks.

For late-game powered, recipe-driven stack refinement, see the [Exotic Affix Forge](exotic-affix-forge.md). The normal Affix Forge is the early catalyst station. It starts with basic add/remove operations and gains later operations from reusable installed upgrades.

It accepts:

- Machine block items.
- Machine parts with per-stack trait data.
- Battery Cell items.

The Affix Forge has a target slot, a catalyst slot, an optional focus slot, three upgrade slots, a current-affix panel, an outcome preview panel, and a Reforge button. It mutates the target stack when the operation succeeds.

Placed machines are refined from their machine screen Refinement tab instead of through world right-click behavior.

## Implementation Contract

Current runtime surface:

- Block and item id: `rngtech:affix_forge`.
- Block entity: `AffixForgeBlockEntity`.
- Menu and screen: `AffixForgeMenu` and `AffixForgeScreen`.
- Slots: target slot, catalyst slot, focus slot, Affix Lens Array slot, Affix Modifier Socket slot, and Affix Resonance Matrix slot.
- Target slot accepts current `RefinementTargets`: machine block items, machine parts, and Battery Cell items.
- Catalyst slot accepts unlocked `RefinementConsumableItem` stacks.
- Focus slot accepts either modifier lenses while an Affix Lens Array is installed or modifier crystals while an Affix Modifier Socket is installed. Lenses and crystals cannot be combined in one operation.
- Upgrade slots are reusable Gear-style items saved with the block and dropped with the forge inventory.
- Installed upgrades update placed block visuals through blockstate properties.
- The block currently exposes no registered NeoForge item or energy capability. Interaction is through the menu.
- The modifier panel lists current rolled affixes for inspection only. Add and upgrade operations no longer use forge selection.
- The left-side `RP` readout shows the target's remaining Refinement Potential, with the full value available on hover.
- The outcome panel previews what the inserted refinement consumable and optional focus item will do before Reforge is clicked.
- The Reforge button mutates the target stack on successful refinement and consumes the catalyst unless a modifier effect preserves it.
- When JEI is installed, refinement consumables appear under an Affix Forging category. This is a dynamic operation view rather than a registered recipe type because the forge mutates the inserted target stack in place.

Implementation checks:

- Shift-clicking refinement consumables should prefer the catalyst slot when their required upgrade is installed.
- Shift-clicking focus items should prefer the focus slot only when their matching upgrade is installed.
- Shift-clicking forge upgrades should prefer their matching upgrade slots.
- Shift-clicking valid targets should prefer the target slot.
- Invalid targets, invalid catalysts, and failed refinement attempts must not consume items or Refinement Potential.
- Successful operations must use `com.rngtech.rpg.refinement` rather than station-specific refinement logic.

## Forge Upgrades

| Upgrade | Resource id | Unlocks |
|---|---|---|
| Affix Lens Array | `rngtech:affix_lens_array` | Modifier lens focus items. |
| Affix Modifier Socket | `rngtech:affix_modifier_socket` | Modifier crystal insertion and effects. |
| Affix Resonance Matrix | `rngtech:affix_resonance_matrix` | Greater Affix Upgrader, Ascension Catalyst, Ascension Matrix, Chaos Crystal, Expansion Crystal, and Null Crystal operations. |

The base forge still supports Affix Injector, Affix Upgrader, and Nullifier Coil. Existing early-access 3-slot forge inventories migrate to the full upgrade set on load so existing worlds keep the old functionality.

## Refinement Consumables

| Item | Resource id | Behavior |
|---|---|---|
| Affix Injector | `rngtech:affix_injector` | Adds one legal prefix or suffix modifier using weighted family and tier selection. Normal targets become Magic. Magic targets can only fill the missing affix type. Rare targets can repeat until legal affix slots are full. Consumes `1-8` RP. |
| Affix Upgrader | `rngtech:affix_modifier` | Upgrades one weighted random eligible rolled affix. Consumes `2-6` RP. |
| Greater Affix Upgrader | `rngtech:affix_upgrade` | Requires an installed Affix Resonance Matrix. Upgrades one weighted random eligible rolled affix, or tunes a capped retunable affix. Consumes `6-10` RP. |
| Ascension Catalyst | `rngtech:ascension_catalyst` | Requires a Magic target with at least `5` [Refinement Potential](../systems/progression.md#refinement-potential), consumes `4` RP, upgrades the target to Rare, and adds one legal affix using weighted family and tier selection. |
| Ascension Matrix | `rngtech:ascension_matrix` | Consumes all remaining [Refinement Potential](../systems/progression.md#refinement-potential), upgrades Magic targets to Rare, adds weighted `1-4` modifiers capped by open legal slots and affordable tiers, and upgrades or retunes one pre-existing rolled affix. |
| Nullifier Coil | `rngtech:nullifier_coil` | Removes one random non-implicit rolled modifier. |
| Chaos Crystal | `rngtech:chaos_crystal` | Rerolls all current rolled affixes from the target's legal pools using weighted replacement families and tiers. |
| Expansion Crystal | `rngtech:expansion_crystal` | Attempts to fill every legal open prefix/suffix slot using weighted family and tier selection, capped by affordable tiers for each added modifier. |
| Null Crystal | `rngtech:null_crystal` | Removes one random non-implicit rolled modifier. |

Adding or upgrading affixes succeeds when the operation is legal and the target has at least the operation's minimum RP cost. Add, fill, reroll, and broad ascension paths pick a legal family by roll weight, then pick a legal tier by global tier weight, family tier weight, and component-stage weighting. Rarity controls open affix slots, not tier eligibility. Tier 7 is limited to Stage 8 Exotic targets and can appear from add, fill, and full-reroll paths, but not from upgrade or ascension actions. The final tier-aware cost roll is capped by the target's remaining RP, so stronger affixes usually consume more budget and can drain all remaining RP, but there is no separate success percentage. Affix Injector costs `1-8` RP, Affix Upgrader costs `2-6` RP, Greater Affix Upgrader costs `6-10` RP, and Expansion Crystal slot filling still costs `1-18` RP per added affix.

Remove and reroll operations still consume weighted random Refinement Potential on success:

| Cost | Weight |
|---:|---:|
| 1 | 50% |
| 2 | 30% |
| 3 | 15% |
| 4 | 5% |

## Modifier Lenses

Modifier lenses are focus items inserted in the focus slot after an Affix Lens Array is installed. They work with Affix Injector, Affix Upgrader, and Greater Affix Upgrader only. A matching lens tag applies a `3x` definition weight multiplier when choosing the new or upgraded affix. Lenses do not affect tier weights, RP costs, rarity rules, slot limits, or modifier conflicts.

Lens bias is soft: non-matching candidates remain in the pool as long as at least one eligible matching candidate exists. If a lens is inserted and no eligible candidate matches its tags, the operation is blocked and consumes nothing. On a successful lens-assisted operation, both the catalyst and the lens are consumed.

Lens item tooltips show the bias family and explain that the lens is not a hard filter. Holding Shift shows the global affix names currently tagged for that lens. The Affix Forge outcome preview stays target-aware: when a lens is inserted, it reports how many candidates on the current target match the lens and how many non-matching candidates can still roll, with the matching current candidates available on hover.

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

Affix Forge add and upgrade operations no longer require a selected prefix, suffix, or existing affix. Locked catalyst operations are rejected before any item or Refinement Potential is consumed.

## Refinement Modifiers

Modifier crystals are inserted in the Affix Forge focus slot after an Affix Modifier Socket is installed. They are optional and only work with their listed compatible operations.

| Item | Resource id | Behavior |
|---|---|---|
| Conservation Crystal | `rngtech:conservation_crystal` | On a successful add or upgrade, has a `25%` chance to preserve the main catalyst. The Conservation Crystal itself is consumed. |
| Frugality Crystal | `rngtech:frugality_crystal` | Requires Affix Upgrader as the main catalyst. On a successful random upgrade, has a `25%` chance to reduce the RP cost to `1`. The Frugality Crystal itself is consumed. |
| Transmutation Crystal | `rngtech:transmutation_crystal` | Requires Greater Affix Upgrader. Replaces the upgraded affix with another weighted legal affix in the same prefix or suffix slot at the target upgrade tier. |
| Resonance Crystal | `rngtech:resonance_crystal` | Requires Greater Affix Upgrader. Preserves the upgraded affix's roll quality within the next tier range. |
| Destabilization Crystal | `rngtech:destabilization_crystal` | Requires Greater Affix Upgrader. Rerolls the target's other rolled affixes after the chosen affix upgrades. |
| Stabilization Crystal | `rngtech:stabilization_crystal` | Prevents corruption when a refinement operation would corrupt the target. The current Affix Forge slice has no corruption outcome storage yet, so this is accepted as a forward-compatible ward and is not consumed by current add/upgrade operations. |

Invalid actions consume no item and no Refinement Potential.

Fixed identity behavior plus machine, component, and Battery Cell base stat profiles are not rolled affixes. They come from the item identity and are not added or removed by Affix Forge consumables.

## Machine Refinement Tab

Crusher, Furnace, Battery Chassis, and future compatible machines share the same Refinement tab content. The tab shows the placed machine's rarity, remaining Refinement Potential, rolled affixes, one catalyst slot, and an Apply button.

Each machine passes its own machine type into the refinement rules so legal affix pools and future machine-specific restrictions stay machine-aware.

## Shared Implementation Rule

All refinement entry points must use the shared refinement library in `com.rngtech.rpg.refinement`.

Current entry points:

- Affix Forge item-stack refinement.
- Machine screen Refinement tabs for placed machines.

## Related

- [Exotic Affix Forge](exotic-affix-forge.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
- [Progression](../systems/progression.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
- [Rarity](../reference/rarity.md)
