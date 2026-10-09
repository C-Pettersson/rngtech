# Progression

Player guide: [Refinement Potential](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#refinement-potential), [Stages](https://c-pettersson.github.io/rngtech/stages/)

RNGTech progression is based on machines, machine parts, rarity, modifiers, and Refinement Potential.

## Crafting and Upgrades

RNGTech crafting treats machines like action RPG characters. The machine chassis defines the character, and components act like gear that can roll, refine, upgrade, fracture, or hit rare breakthrough outcomes.

The canonical crafting and upgrade rules are documented in [Crafting and Upgrades](crafting.md).

## Rarity

Machine rarity tiers are defined in [Rarity](../reference/rarity.md).

## Modifier Layers

Machines and machine parts can have modifiers in these slots:

- Implicit
- Prefix
- Suffix
- Enchant

The slot definitions are documented in [Affix Generation](affix-generation.md).

Implicit modifiers are fixed authored modifiers or behavior flags from item, part, cell, or machine identity. Machine and chassis numeric identity is usually an authored base stat profile before affixes. Implicits use their own pool and do not count as rolled affixes.

## Refinement Potential

Refinement Potential tracks how much crafting or rerolling budget a modifier-bearing stack or placed machine still has.

Status: Prototype

Canonical stat: [REFINEMENT_POTENTIAL](../reference/machine-stats.md#refinement-potential)

Starting modifiers and Refinement Potential are separate rolls; RP ranges come from component stage, not starting rarity (see [Affix Generation](affix-generation.md)). Invalid refinement actions consume no catalyst and no RP. Successful refinement consumes the catalyst and the operation's RP cost.

Upgrading a component preserves its remaining Refinement Potential and does not add a fresh budget. A good component can move into later stages, but heavy early investment still matters.

## Refinement Operations

Status: Prototype

| Operation | Item | Behavior |
|---|---|---|
| Add modifier | `rngtech:affix_injector` | Adds one legal prefix or suffix modifier from the full eligible pool. Normal targets become Magic. Magic targets can only fill their missing affix type. Rare targets can repeat until legal affix slots are full. Consumes `1-8` RP. |
| Upgrade random modifier | `rngtech:affix_modifier` | Player-facing Affix Upgrader. Upgrades one weighted random eligible rolled modifier. Consumes `2-6` RP. |
| Greater upgrade | `rngtech:affix_upgrade` | Player-facing Greater Affix Upgrader. Requires an installed Affix Resonance Matrix, then upgrades one weighted random eligible rolled modifier or tunes a capped retunable modifier. Consumes `6-10` RP. |
| Catalyze ascension | `rngtech:ascension_catalyst` | Requires Magic rarity and at least `5` Refinement Potential, consumes `4` RP, upgrades to Rare, then adds one legal Rare affix. |
| Ascend rarity | `rngtech:ascension_matrix` | Requires Magic rarity, consumes all remaining Refinement Potential, upgrades to Rare, adds weighted `1-4` rolled modifiers capped by open Rare affix slots, and upgrades or retunes one pre-existing rolled affix. |
| Remove modifier | `rngtech:nullifier_coil` | Removes one random rolled modifier. Fixed identity implicits and authored base stat profiles are never removed. |
| Full reroll | `rngtech:chaos_crystal` | Rerolls all current rolled prefix and suffix modifiers from legal pools while preserving rarity and remaining non-affix state. |
| Fill open slots | `rngtech:expansion_crystal` | Attempts to fill all currently legal open prefix/suffix slots, rolling tier-weighted RP costs per added modifier. |
| Random crystal removal | `rngtech:null_crystal` | Removes one random rolled modifier. This is the crystal-family equivalent of Nullifier Coil behavior. |

The base Affix Forge supports Affix Injector, Affix Upgrader, and Nullifier Coil. Modifier lenses require an installed `rngtech:affix_lens_array`; modifier crystals require an installed `rngtech:affix_modifier_socket`; Greater Affix Upgrader, ascension, Chaos, Expansion, and Null Crystal operations require an installed `rngtech:affix_resonance_matrix`.

Affix add and upgrade operations roll a hidden tier-weighted cost inside the selected operation's range. Affix Injector costs `1-8` RP, Affix Upgrader costs `2-6` RP, Greater Affix Upgrader costs `6-10` RP, and Expansion Crystal slot filling still costs `1-18` RP per added affix. Higher-tier additions and upgrades require more available RP before they can be selected, capped by the operation range. Legal add/upgrade operations have no separate success percentage; when the rolled cost exceeds remaining RP, the operation consumes all remaining RP and leaves the target at `0` RP.

Nullifier Coil and random remove/reroll operations consume weighted random Refinement Potential on success:

| Cost | Weight |
|---:|---:|
| 1 | 50% |
| 2 | 30% |
| 3 | 15% |
| 4 | 5% |

Modifier lenses are focus items, not standalone catalysts. Power, Speed, Yield, Stability, Control, Kinetic, and Efficiency lenses can be inserted in the Affix Forge focus slot and apply a `3x` weight multiplier to eligible definitions with matching lens tags. They work with Affix Injector, Affix Upgrader, and Greater Affix Upgrader. If no eligible candidate matches the inserted lens, the operation is rejected and consumes nothing. On success, both the catalyst and lens are consumed. Tool Bench lens refinement is out of scope for the current focus-slot pass.

Optional modifier crystals can be inserted in the Affix Forge focus slot after the Modifier Socket is installed. They work only with their listed compatible operation:

| Modifier item | Behavior |
|---|---|
| `rngtech:conservation_crystal` | `25%` chance to preserve the main catalyst on successful add or upgrade. |
| `rngtech:frugality_crystal` | Requires `rngtech:affix_modifier`; `25%` chance to reduce the successful random-upgrade RP cost to `1`. |
| `rngtech:transmutation_crystal` | Requires `rngtech:affix_upgrade`; replaces the upgraded affix with another legal affix in the same prefix or suffix slot. |
| `rngtech:resonance_crystal` | Requires `rngtech:affix_upgrade`; preserves the upgraded affix's roll quality within the next tier range. |
| `rngtech:destabilization_crystal` | Requires `rngtech:affix_upgrade`; rerolls the target's other rolled affixes after the chosen affix upgrades. |
| `rngtech:stabilization_crystal` | Prevents corruption when a refinement operation would corrupt the target; current corruption state storage remains deferred. |

## Entry Points

Entry points are the [Affix Forge](../content/affix-forge.md), the Exotic Affix Forge, the [Tool Bench](../content/modular-field-tools.md) Refine tab (including parts inside assembled tools), and machine screen Refinement tabs. There is no placed-block right-click refinement path.

## Shared Implementation

All refinement entry points must use the shared refinement library in `com.rngtech.rpg.refinement` so new stations, catalysts, recipes, and machine UIs keep the same legality checks and result behavior.
