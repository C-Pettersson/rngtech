# PRD: Corruption

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Progression](https://c-pettersson.github.io/rngtech/rarity-and-affixes/) for current behavior.

PRD status: Accepted

Implementation status: Planned

Release: 2.0

Last updated: 2026-10-06

## Summary

Corruption is a one-way gamble on a finished item, modeled on Path of Exile's Vaal Orb. Using a Volatile Catalyst on a refinement target gives one weighted outcome: no change, a corruption implicit beyond normal affix limits, a full reroll, or a negative implicit. Corruption implicits have one fixed value each, with no tiers or ranges. Afterwards the item is **Corrupted** and can never be refined again.

Corruption is also the only way to change a [Unique](uniques.md). That pairing is why both features ship together.

Today the Stabilization Crystal is a focus item whose description promises to prevent corruption. No operation corrupts and no item can store corruption, so the crystal does nothing at runtime. This PRD gives it a real job.

## References

- [Progression: Refinement Operations](https://c-pettersson.github.io/rngtech/rarity-and-affixes/)
- [Affix Generation](../systems/affix-generation.md)
- [Affix Forge](https://c-pettersson.github.io/rngtech/affix-forge/), [Exotic Affix Forge](https://c-pettersson.github.io/rngtech/exotic-affix-forge/)
- [Unique Items PRD](uniques.md)
- [Machine Ascendancies PRD: Loop Prevention](machine-ascendancies.md#loop-prevention)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Stats](../reference/machine-stats.md)

Key existing code:

- `src/main/java/com/rngtech/rpg/MachineTraits.java` (record, `CODEC`, `STREAM_CODEC`, `withIdentity`, `normalizedStored`)
- `src/main/java/com/rngtech/rpg/ModifierSlot.java`
- `src/main/java/com/rngtech/rpg/refinement/RefinementEngine.java`, `RefinementOperation.java`, `RefinementAction.java`, `RefinementModifier.java` (`CORRUPTION_WARD`)
- `src/main/java/com/rngtech/rpg/refinement/RefinementTargets.java`
- `src/main/java/com/rngtech/content/blockentity/AffixForgeBlockEntity.java` (`lockedMessage`)
- `src/main/java/com/rngtech/content/menu/RefinementMenuSupport.java` (`applyToMachine`, `applyToTargetStack`)
- `src/main/java/com/rngtech/content/blockentity/ExoticAffixForgeBlockEntity.java`
- `src/main/java/com/rngtech/content/blockentity/ToolBenchBlockEntity.java`
- `src/main/java/com/rngtech/content/registry/ModItems.java` (`STABILIZATION_CRYSTAL`)

## Goals

- A Volatile Catalyst with four weighted outcomes, for ordinary targets and Uniques alike.
- A stored, synced Corrupted state on item stacks and placed machines. Every refinement entry point rejects a Corrupted target.
- Corruption implicits: one per item, drawn by weight from per-host pools, for effects that normal affixes cannot roll. Each has one fixed value, with no tiers or ranges.
- The Stabilization Crystal removes the negative outcome when it is in the focus slot.
- Corruption implicits never add yield or Refinement Potential in 2.0.
- The catalyst is gated by RNGTech's own challenges, not by vanilla items or exploration. Its recipe mirrors the Seals: Seals reward precision, and the catalyst rewards controlled instability.
- Pack makers can disable the catalyst recipe and award catalysts another way, such as quests or [challenge loot](uniques.md#challenge-loot).

## Non-Goals

- No corruption chance on existing operations. Only the Volatile Catalyst corrupts.
- No item destruction. The worst outcome is a negative implicit, consistent with [Crafting and Upgrades](../systems/crafting.md#strain), where rough results cost a long-term price without deleting invested components.
- No way to remove corruption.
- No corruption of modular tool heads or rods in 2.0. The Tool Bench Refine tab stays limited to its current operations.
- No corruption of chassis items, material catalog items, unidentified stacks, or `rngtech:recycling_stripped` stacks.
- No phased modifiers, fractures, or instability state. Those stay deferred.
- No vanilla loot source and no vanilla-only ingredient, such as Echo Shards, for the catalyst.

## Vocabulary

| Term | Meaning |
|---|---|
| Volatile Catalyst | `rngtech:volatile_catalyst`, the refinement consumable that corrupts a target. "Vaal" is Path of Exile's name and is not used. |
| Corrupted | The stored state after any catalyst outcome. A Corrupted target rejects every refinement operation. |
| Corruption implicit | One stored modifier or behavior, granted by the Blessed or Blighted outcome, outside the prefix and suffix limits. |
| Blessed | The outcome that adds a positive corruption implicit. |
| Blighted | The outcome that adds a negative corruption implicit. |
| Reforged | The outcome that rerolls every rolled affix, like a Chaos Crystal, while keeping rarity. On a Unique it rerolls every ranged stat line instead. |
| Untouched | The outcome that only marks the target Corrupted. |

## Player Flow

1. The player has a finished part, cell, or placed machine, or a Unique.
2. They put a Volatile Catalyst in the Affix Forge with an installed Affix Resonance Matrix, or in a placed machine's Refinement tab. They may add a Stabilization Crystal to the Affix Forge focus slot.
3. The forge shows the outcome table for that target.
4. Applying the catalyst consumes it, rolls one outcome, and marks the target Corrupted.
5. The tooltip shows a red Corrupted line and any corruption implicit. Every refinement station now rejects the item.

## Outcomes

| Outcome | Weight | With Stabilization Crystal |
|---|---:|---|
| Untouched | 25 | Gains the Blighted weight |
| Blessed | 25 | Unchanged |
| Reforged | 25 | Unchanged |
| Blighted | 25 | Removed |

- **Reforged** rerolls every rolled prefix and suffix from legal pools, keeps rarity and Refinement Potential, and leaves fixed identity and base profiles alone. On a Normal target with no affixes, Reforged becomes Untouched.
- **Reforged on a Unique** rerolls every ranged stat line inside its catalog range, as identification does. Fixed lines and behaviors stay. On a Unique with no ranged lines, Reforged becomes Untouched.
- Refinement Potential stays stored but is hidden everywhere once the target is Corrupted, because it can no longer be spent.
- Weights are datapack-tunable through one `data/rngtech/corruption/outcomes.json`, so hardcore packs can make corruption harsher.

## Corruption Implicits

Each host type has a Blessed pool and a Blighted pool in `data/rngtech/corruption/pools/<host>.json`. One entry is chosen by weight. Entries use the existing modifier operations and `MachineBehavior` ids. Each entry has one fixed value: a corruption implicit has no tiers, no range, and no roll, and no operation can upgrade it. Draft pools:

| Host | Blessed examples | Blighted examples |
|---|---|---|
| Heat Core | +10% Maximum Temperature beyond the tier cap; `THERMAL_BUFFER` on sub-Steel cores; `POWER_GRACE` | 25% increased Energy Usage; 20% less Temperature Stability |
| Crush Head | +1 Processing Level (stage acceptance unchanged); +Jam Recovery | +Jam Chance; 15% less Processing Speed |
| Servo | `POWER_GRACE` below Stage 6; `AUTO_PURGE`; +Overheat Tolerance | 15% less Processing Speed; 20% less Stability |
| Alloy Crucible | +Blend Speed; +Temperature Stability | 20% less Stability |
| Fluid Pump | 50% increased host Fluid Capacity | 30% less Fluid Transfer |
| Battery Cell | `CELL_LEAKAGE_DAMPING`; 20% increased Energy Transfer | Doubled Idle Loss |
| Control Board, Coil, Stabilizer Matrix | +1 Coil Reach; +Streak Floor; +Catalyst Efficiency | 20% less Processing Speed; 10% less Calibration Precision |
| Placed processing machine | +1 Batch Size where the machine reads it; +10% Processing Speed | 25% increased Energy Usage |
| Placed generator | +10% Energy Generation | 20% less Efficiency |
| Battery Chassis | +1 active Battery Slot | 15% increased Idle Loss |

Pool rules, checked at load:

- Every entry's stat or behavior is read by the host, using the same stat-reader table as the [Unique catalog](uniques.md#unique-catalog).
- No yield stats and no Refinement Potential in 2.0 pools.
- No entry raises Gear stage acceptance, lowers authored recipe inputs, or bypasses a `bonus_output` opt-out.
- Entries may grant ascendancy stats. They have no effect without the ascendancy, and the tooltip names it.

+1 Processing Level and +1 Batch Size stay in the Blessed pools by decision. They are the iconic "+1 level" corruptions, and their pool weights, not their values, control how often they appear.

## Uniques

- A Unique can be corrupted once, with the same outcome weights as other targets.
- Reforged rerolls its ranged lines, so a poorly rolled Unique has a 25% chance of a new roll, with the same chance of a Blighted implicit.
- A Unique stores its rolls and its corruption, and nothing else. `withIdentity` must keep both while forcing `UNIQUE` and 0 RP.
- A Corrupted Unique stays unrecyclable.

## Storage and Saves

- `MachineTraits` gains one optional field, `corruption`, a record of `{ outcome, modifiers, behaviors }`. Its presence means Corrupted. The persistent codec uses `optionalFieldOf("corruption")`, so existing saves load unchanged. The stream codec grows to five fields.
- `ModifierSlot` gains `CORRUPTION`. Stat aggregation applies `CORRUPTION` modifiers after affixes for item stacks (`ComponentBaseStatCatalog.effectiveStats`) and for placed machines.
- `withIdentity`, `normalizedStored`, `isEmpty`, `RefinementEngine.withModifiers`, and every `new MachineTraits(...)` site must keep `corruption`. A domain check builds a Corrupted stack and round-trips it through each one.
- Placed machines store traits as block-entity components, so the field drops with the block and returns on placement.

## Refinement Integration

- `RefinementOperation.CORRUPT` and `RefinementAction.CORRUPT`, with an engine branch. `rngtech:volatile_catalyst` is a `RefinementConsumableItem(CORRUPT)`.
- `RefinementEngine.apply` checks in this order:
    1. a Corrupted target is rejected for every operation;
    2. a Unique target is rejected for every operation except `CORRUPT`;
    3. the existing lens and modifier checks.
- Corrupt costs no Refinement Potential.
- **Affix Forge:** `CORRUPT` requires an installed Affix Resonance Matrix. The Stabilization Crystal (`CORRUPTION_WARD`) becomes valid with `CORRUPT`, removes Blighted, and is consumed on use. It stops being valid with the add and upgrade operations, where it never did anything.
- **Placed-machine Refinement tabs:** they accept every `RefinementConsumableItem`, so `CORRUPT` works there with no focus slot and no crystal.
- **Exotic Affix Forge:** rejects Corrupted targets with a new `STATUS_CORRUPTED`.
- **Tool Bench:** unchanged allowlist, so `CORRUPT` is rejected.
- **Debug Reroller:** rejects Corrupted targets.

## Sources

The Volatile Catalyst has one default source: a `rngtech:calibrated_shaped` recipe under a `rngtech:volatile_catalyst_recipes_enabled` condition and config key, following the Seal recipe precedent. It has no loot source.

Crafting it is the challenge. Each ingredient asks the player to make a machine go wrong in a controlled way:

| Ingredient | Count | Family | Challenge |
|---|---:|---|---|
| Detuned Calibrated Kinetic Component | 1 | Resonance Calibrator | A Stage 5+ calibration that lands inside a narrow **20–35 stability** band. A well-built calibrator overshoots, so the player has to mistune it on purpose: a weaker Control Board, no Stabilizer Matrix, or no Battery Cell. |
| Malformed Ingot of a Stage 5+ material | 2 | Metal Press or Alloy Furnace | A failed cycle on a Stage 5+ recipe, from heat, stability, or power strain. The Steel Press and the Alloy Furnace's Tungstensteel and Nullite recipes already produce it. |
| Jam Debris | 2 | Crusher | New. A Crusher jam on a recipe that needs Processing Level 5 or more drops one Jam Debris into the output. Jams only happen when crushing under level, so the player has to crush above their head's hardness on purpose. |
| Sparksteel Plate | 4 | Material | The Stage 5 material anchor, as in Seal I. |

The recipe makes one catalyst. This places corruption at Stage 5, alongside Seal I.

Supporting changes:

- `CalibrationRequirement` gains an optional `max_stability` field, defaulting to 100. A Detuned part is any calibrated component that passes that ceiling; there is no separate item.
- A new `rngtech:malformed_ingot` ingredient type matches the stored `rngtech:material` component against a minimum material stage.
- `rngtech:jam_debris` is a plain item with no RPG traits. A jam that drops debris still costs its normal time and FE, and the input is not consumed. Rockbreaker's jam reduction and the Mineshaft Worn Pick-Jaw's extra jam chance both change how fast debris arrives. That trade-off is intended.
- The Volatile Catalyst and Jam Debris have no Component Recycler or Potential Reactor recipes.

Pack makers can disable the recipe and award catalysts through quests, their own recipes, or the [challenge loot hooks](uniques.md#challenge-loot) defined in the Uniques PRD.

## Loop Prevention

2.0 pools contain no yield stats and no Refinement Potential, and the pool loader rejects them. Jam Debris is a new Crusher output. The loop audit counts it once per jam, like a failure output, and nothing turns it back into its inputs. Recycling returns are per recipe and ignore traits, so corruption cannot change them. A future yield entry follows the same rule as a yield Unique: loop-audit coverage, a bound that fits, and a domain check that pins its worst case.

## UI

- **Tooltip:** a red Corrupted line under the rarity header, then the corruption implicit in a red section labeled Blessed or Blighted. The Refinement Potential line is hidden, as are the RP readouts in the Affix Forge, Exotic Affix Forge, Refinement tabs, and Jade.
- **Affix Forge:** with a Volatile Catalyst inserted, the preview shows the outcome table for the current target and the effect of an inserted Stabilization Crystal. The existing candidate preview shows the possible implicits from the target's pools.
- **Placed-machine Refinement tab:** the same outcome table in hover text.
- **Jade (sneak):** a Corrupted line on placed machines.
- **JEI:** an information page for the Volatile Catalyst with the outcome tables, and one for Jam Debris explaining the jam challenge. The calibrated crafting view shows the stability band, not only its floor.
- **Advanced Item Filter:** optional Corrupted and Uncorrupted matching, in phase 5 if time allows.

## Delivery Phases

1. **PRD and state.** This document and [its state page](corruption-state.md).
2. **Storage:** the `corruption` field, the `CORRUPTION` slot, stat aggregation, every constructor and normalizer, and codec round-trip checks.
3. **Operation:** `CORRUPT`, the outcome tables and pools with their loaders and validation, the rejection order, and station gating, including the Stabilization Crystal change and Exotic Affix Forge rejection.
4. **Item and sources:** the Volatile Catalyst, `max_stability` on calibration requirements, the Malformed Ingot stage ingredient, Jam Debris and the Crusher jam drop, the catalyst recipe, and its condition.
5. **UI:** tooltips, the Affix Forge outcome preview, Refinement tab hover text, Jade, JEI, and the optional filter matching.
6. **Documentation:**
    - Progression, Affix Forge, Exotic Affix Forge, and Rarity;
    - the [Current Implementation Matrix](../reference/current-implementation.md), removing the deferred-corruption notes;
    - an FTB Quests entry.

## Test Plan

Domain checks (`masteryCheck`):

- Codec and stream codec round trips with and without `corruption`. Existing saves load unchanged.
- `withIdentity`, `normalizedStored`, and `withModifiers` keep `corruption`, including on Uniques.
- Outcome weights, with and without a Stabilization Crystal, match the table over a seeded sample, for every rarity and for Uniques.
- Every corruption implicit equals its pool entry's fixed value.
- Every refinement entry point rejects a Corrupted target and consumes nothing. Every entry point except `CORRUPT` still rejects Uniques.
- Corrupted targets report no Refinement Potential to any tooltip, screen, or Jade line.
- Reforged keeps rarity, Refinement Potential, fixed identity, and base profiles. On an empty Normal target it becomes Untouched. On a Unique it rerolls only ranged lines, inside their ranges.
- Pool validation rejects an unread stat, a yield stat, Refinement Potential, and stage-acceptance changes.
- `max_stability` accepts parts inside the band and rejects parts above it. Old recipes without the field behave as before.
- The Malformed Ingot ingredient accepts Stage 5+ materials and rejects lower stages and ingots with no material.
- Jam Debris drops only from jams on recipes that need Processing Level 5 or more, and a jam still consumes no input.
- `CORRUPTION` modifiers apply on stacks and on placed machines, after affixes.

ModDex checks (`npm run moddex:check`):

- The catalyst recipe carries the config condition.
- No vanilla loot table or loot modifier outputs the catalyst.
- The loop audit counts Jam Debris, and neither the catalyst nor Jam Debris has a recycling or reactor recipe.
- The loop audit still passes with no new allowlist entries.

In-game checks:

- Corrupt a Rare part, a Unique, a Battery Cell, and a placed machine through each station. Check all four outcomes.
- The Stabilization Crystal removes Blighted and is consumed.
- Corrupted items are rejected by every station and the Debug Reroller.
- A corrupted placed machine keeps its state through breaking and replacing it, and in multiplayer.
- Make each ingredient: a detuned calibration inside the band, Stage 5+ press and Alloy Furnace failures, and under-level Crusher jams. Check that JEI explains each challenge.
- With the recipe config off, the catalyst recipe is absent from crafting and JEI, and a catalyst from `/give` or a quest still works.

## Acceptance Criteria

- The Volatile Catalyst corrupts every listed target type with the outcome tables above.
- Corrupted is stored, synced, shown, and blocks all refinement.
- The Stabilization Crystal has a working, documented effect.
- No 2.0 pool entry adds yield, Refinement Potential, or stage acceptance.
- Existing saves load unchanged.
- `./gradlew ciCheck`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` pass.

## Release Scope

- **2.0 must include:** phases 1–4 and 6, plus tooltips and the Affix Forge preview from phase 5.
- **In 2.0 if ready before the tag:** Jade, JEI, and filter matching.
- **Dependency:** corrupting Uniques needs the [Unique catalog](uniques.md#unique-catalog). Corruption of ordinary targets can land first.
- **Release gate:** the full in-game checklist in [Releasing](../releasing.md) runs once for 2.0 after every 2.0 feature lands.

## Decisions

Resolved 2026-10-05:

- No outcome destroys or downgrades an item's rarity. The worst result is Blighted.
- Corruption works on placed machines through their Refinement tabs.
- The Stabilization Crystal removes Blighted instead of preventing corruption, and is consumed.
- Corrupt costs no Refinement Potential.
- Corruption pools carry no yield or Refinement Potential in 2.0.

- The catalyst is named Volatile Catalyst.
- Outcomes are weighted. A corruption implicit has one fixed value, with no tiers or ranges.
- +1 Processing Level and +1 Batch Size are in the Blessed pools.
- Uniques use the same four outcomes. Reforged rerolls their ranged lines.
- A Corrupted item hides its Refinement Potential.

Resolved 2026-10-06:

- The catalyst is gated by RNGTech challenges only: a detuned calibration plus failure byproducts from two machine families. It has no vanilla loot and no vanilla-only ingredient.
- The recipe sits at Stage 5, alongside Seal I.
