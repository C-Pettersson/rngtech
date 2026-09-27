# Rarity

Rarity controls the rolled affix slot count for a machine or machine part. Modifier tier eligibility is controlled separately by global tier weights, modifier-family tier tables, component-stage weighting, and the Stage 8 Exotic tier 7 gate.

| Rarity | Status | Notes |
|---|---|---|
| `NORMAL` | Prototype | Baseline rarity. |
| `MAGIC` | Prototype | Intermediate rarity with one prefix and one suffix modifier. |
| `RARE` | Prototype | Higher rarity with up to three prefixes and three suffixes. Generated names use the first prefix and first suffix. |
| `UNIQUE` | Prototype | Special fixed or heavily constrained drop/find-only behavior. Current code-backed example: `rngtech:unique_potato_battery_cell`, which can appear in village chest loot. |

Fixed identity behavior and authored base stat profiles are separate from rarity affix limits. They can exist on any rarity and do not count as Magic or Rare prefix/suffix modifiers. Machine, chassis, component, and Battery Cell base stat profiles are applied before affixes and do not consume affix slots.

## Refinement Rules

| Rarity | Refinement behavior |
|---|---|
| `NORMAL` | Affix Injector can add the first rolled affix and promote the target to Magic. |
| `MAGIC` | Affix Injector can fill the missing prefix or suffix type. Ascension Catalyst can promote the target to Rare with one added affix and a fixed RP cost. Ascension Matrix can promote the target to Rare with additional affixes, one existing-affix upgrade or retune, and full RP consumption. |
| `RARE` | Affix Injector can continue adding legal rolled affixes until three prefixes and three suffixes are full. |
| `UNIQUE` | Refinement is rejected by the current shared refinement rules. |

## Source

Implemented enum: `com.rngtech.rpg.Rarity`
