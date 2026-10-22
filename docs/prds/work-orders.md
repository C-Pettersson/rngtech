# PRD: Work Orders

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Machine Mastery](https://c-pettersson.github.io/rngtech/machine-mastery/) for current behavior.

PRD status: Draft

Implementation status: Planned

Release: Not scheduled

Last updated: 2026-10-21

## Summary

Work Orders are a repeatable endgame loop for Mastery machines, modeled on Path of Exile's maps. A Work Order is an item that sets one Mastery machine a job, such as "Crush 256 ores that need Processing Level 6". Rolled conditions make the job harder, and each condition raises the reward. When the machine finishes the job, it drops a Fulfilled Work Order, which the player opens for loot. That loot includes blank orders of the same or a higher tier, so the loop feeds itself.

Today a machine's Mastery has no goal after its third Seal and level 100. Stage 8 chassis have little to do that a Stage 7 chassis can't, and all six [challenge loot tables](../reference/current-implementation.md#challenge-loot-tables) ship empty. Work Orders give each of these a job:

- A maxed machine gets repeatable goals, and the machine's build decides how hard a job it can take.
- The highest tiers need the top chassis stage of each family.
- The reward table is a seventh challenge table, with default loot like the Glyph entries the [Phase Array](phase-array.md) adds to the other six.

Orders reuse the existing systems. Conditions are affixes, so the Affix Forge, Nullifier Coil, Chaos Crystal, and Volatile Catalyst all work on an order. Condition effects join the Mastery stat pipeline as one more effect source, beside tree and ascendancy nodes. Rewards roll through `ChallengeLoot`.

## References

- [Machine Mastery](../systems/machine-mastery.md), especially [Ascendancies: Seals and tiers](../systems/machine-mastery.md#seals-and-tiers)
- [Machine Ascendancies PRD: Loop Prevention](machine-ascendancies.md#loop-prevention)
- [Unique Items PRD: Challenge loot](uniques.md#challenge-loot)
- [Corruption PRD](corruption.md)
- [Heat Model PRD](heat-model.md)
- [Batch Processing PRD](batch-processing.md)
- [Affix Generation](../systems/affix-generation.md)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Dormant Machines](https://c-pettersson.github.io/rngtech/dormant-machines/), for Soul Transfer

Key existing code:

- `src/main/java/com/rngtech/rpg/progression/MachineProgressionState.java` (record, `CODEC`, `STREAM_CODEC`)
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryHost.java` (`grantMasteryXp`, `masteryLevelGained`, `applyMasteryState`)
- `src/main/java/com/rngtech/rpg/progression/MasteryOperations.java` (`ascendancy`, `AscendancyPayment`)
- `src/main/java/com/rngtech/rpg/progression/MasteryEffectSource.java`
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryFamily.java` (`topStage`)
- `src/main/java/com/rngtech/rpg/progression/SoulTransfer.java` (`Charge`)
- `src/main/java/com/rngtech/content/loot/ChallengeLoot.java`, `ChallengeContext.java`
- `src/main/java/com/rngtech/client/screen/AscendancyPanel.java`
- `src/main/java/com/rngtech/rpg/refinement/RefinementTargets.java`, `RefinementEngine.java`
- `src/main/java/com/rngtech/content/blockentity/HeatControl.java` (overheat slider)

## Goals

- A repeatable goal for Mastery machines after level 100 and Seal III, in all seven Mastery families.
- Every cost is a condition the player can read on the order before accepting it. No hidden penalty applies.
- Conditions are affixes, so the existing refinement and corruption tools shape an order with no new currency.
- Higher tiers ask for higher chassis stages, up to each family's top stage, so Stage 7 and 8 bodies have an endgame role.
- Rewards are refinement currency, Glyphs for the Phase Array, Uniques, and further orders. They are never FE and never yield, so orders cannot create a loop.
- Blank orders are gated by RNGTech's own challenges. Higher tiers come only from fulfilled orders.
- Pack makers can disable the recipe, replace the objective, condition, and reward data by datapack, and hand out orders through quests.

## Non-Goals

- No Dispatch Desk or other block that offers orders. Orders are crafted or dropped only.
- No Work Orders on generators, Battery Chassis, the Component Recycler, or the Miner's Companion. They have no Mastery family.
- No condition that raises yield, Refinement Potential, FE generation, or Gear stage acceptance.
- No Mastery XP bonus or tree points from orders. The 21 reserved Mastery points stay with the Tome of XP.
- No Seals or Seal Cores in the default rewards. Seals stay the precision trial that the ascendancy design asks for.
- No raw materials, ingots, or ores in the default rewards. They would undercut the processing chain and the ore stages.
- No more than one active order per machine.
- No HUD icon or overlay. Status cues on the machine are diegetic or appear in Jade and the machine screen.

## Vocabulary

| Term | Meaning |
|---|---|
| Work Order | `rngtech:work_order`, the item that holds an unstarted order: tier, rarity, conditions, and a seed. |
| Tier | I to V. It sets the job size, the chassis stage the machine needs, and the reward pool. |
| Objective | The job, such as a count of completed work that matches a filter. The seed and the machine's family choose it. |
| Condition | A rolled prefix or suffix on the order. It applies a cost to the machine while the order runs, and adds Bounty or Prestige. |
| Bounty | A percent, 100% at base. It sets how many times the reward table rolls. |
| Prestige | A whole number, 0 at base. It unlocks better reward pools. |
| Active order | An accepted order, stored in the machine's `rngtech:machine_progression`. |
| Rush | An opt-in deadline the player can add when accepting an order. It never rolls on its own. |
| Fulfilled Work Order | `rngtech:fulfilled_work_order`, the item a finished order drops. Using it rolls the reward table. |

## Player Flow

1. The player crafts a Tier I Work Order, or gets one from a fulfilled order or a quest.
2. They may shape it in the Affix Forge or a Refinement tab: add a condition for more Bounty, remove one to make the job safer, reroll it, or corrupt it.
3. On a Mastery machine's Mastery tab, they open the Work Order panel. With the order in their inventory, the panel previews the exact objective this machine would get, every condition and what it costs, the Bounty and Prestige, and the Rush option.
4. They click **Accept**. The order is consumed from their inventory, the same way a Seal is used from the Ascendancy panel. The order is now active on the machine.
5. While the order is active, its conditions apply, and the machine counts matching work toward the objective. The Mastery tab, the Bonus Summary drawer, and Jade show progress.
6. When the objective is complete, the machine drops a Fulfilled Work Order on top of itself, or into the cart's cargo for a Forestry Companion. The conditions stop applying.
7. The player uses the Fulfilled Work Order to roll its rewards.

The player can **Abandon** an active order from the Work Order panel at any time. The order is lost, and nothing is returned.

## Tiers

| Tier | Machine stage required | Mastery level required | Draft objective size |
|---|---:|---:|---|
| I | 4 | 40 | About 15 minutes of steady work on a good Stage 4 build |
| II | 5 | 55 | About 20 minutes on a good Stage 5 build |
| III | 6 | 70 | About 25 minutes on a good Stage 6 build |
| IV | 7 | 85 | About 30 minutes on a good Stage 7 build |
| V | 8 | 100 | About 30 minutes on a good Stage 8 build |

The stage requirement is the lower of the tier's stage and the family's `topStage()`. Per-tier chassis stage gates cannot work on their own, because the Metal Press tops out at Stage 4, the Alloy Furnace at 6, the Resonance Calibrator at 7, and the Melter has one body. The Mastery level requirement carries the ladder for those families. Objective sizes are tuned to time on a reference build, not to a fixed count, and the tuning uses the processing balance simulation.

## Objectives

Each objective counts work the machine already reports. Progress counts only work this machine finishes while the order is active. A batch counts each item it completes.

Objectives come from `data/rngtech/work_orders/objectives/*.json`. Each entry names a family, an objective type, a count per tier, and optional filters. Draft entries:

| Family | Objective type | Example |
|---|---|---|
| Crusher | Completed crushes with a minimum recipe Processing Level | Crush 256 ores that need Processing Level 6 or more |
| Furnace | Completed cycles with the overheat slider at or above a value | 192 cycles with the overheat slider at 50% or more |
| Alloy Furnace | Completed alloy cycles with a minimum component stage | 96 Stage 6+ alloy cycles |
| Metal Press | A streak of presses with no Malformed Ingot | 200 presses in a row without a failure |
| Melter | Fluid output in mB from a minimum material stage | Melt 48,000 mB of Stage 6+ metal |
| Resonance Calibrator | A streak of calibrations at or above a stability value | 12 calibrations in a row at 80+ stability |
| Forestry Companion | Trees harvested with a minimum cutting tool stage | Harvest 300 trees with a Stage 6+ head |

Rules:

- The objective is a function of the order's seed, its tier, and the machine's family. The panel can preview it exactly before the player accepts, and accepting on another machine of the same family gives the same objective.
- A streak objective resets to 0 on the event that breaks it, such as a Malformed Ingot. The panel shows the best streak so far.
- An objective must be reachable on every chassis that meets the tier's stage requirement. A domain check runs each objective's filters against the family's recipes.
- Fill and count filters read existing recipe data: Processing Level, recipe stage, component stage, material stage, and calibration stability. New objective types need a reporting hook in each machine that uses them.

## Conditions

Conditions are affixes on the order, drawn from a `work_order` eligibility profile. Prefixes are **pressure** conditions on the machine's stats. Suffixes are **rule** conditions on the job. Each condition adds Bounty, Prestige, or both. Draft pool:

| Slot | Condition | Effect while active | Reward | Families |
|---|---|---|---|---|
| Prefix | Starved Feed | 15%–30% less Processing Speed | +15%–30% Bounty | All |
| Prefix | Power Surcharge | 25%–50% increased Energy Usage | +10%–25% Bounty | All except Forestry Companion |
| Prefix | Short Crew | −1 Batch Size, where the machine has 2 or more | +30% Bounty | Machines that read Batch Size |
| Prefix | Unsteady Hands | 10–20 less Stability | +15%–25% Bounty | Machines that read Stability |
| Prefix | Thin Margins | The overheat safe band is 25%–50% narrower | +20%–35% Bounty | Furnace, Alloy Furnace, Metal Press, Melter |
| Prefix | Cold Start | 10–20 less Temperature Stability | +15%–25% Bounty | Furnace, Alloy Furnace, Metal Press, Melter |
| Suffix | of Hand Feeding | Instant Process does not trigger | +20% Bounty | Machines that roll Instant Process |
| Suffix | of Brittle Lining | A heat failure or Malformed Ingot resets progress to 0 | +35% Bounty, +1 Prestige | Furnace, Alloy Furnace, Metal Press |
| Suffix | of Fine Tolerance | A calibration below the objective's stability resets progress to 0 | +35% Bounty, +1 Prestige | Resonance Calibrator |
| Suffix | of Deep Ore | Only work on recipes one stage above the objective's filter counts | +25% Bounty, +1 Prestige | All |
| Suffix | of the Jammed Line | A Crusher jam resets progress to 0 | +30% Bounty, +1 Prestige | Crusher |

Rules:

- Rarity sets the condition count: Normal 0, Magic 1–2, Rare 3–4. Normal orders still pay a base reward.
- Condition tiers follow normal affix tiers. A higher tier is a harsher cost and a larger reward.
- A condition rolls on an order only if every family can read it, or the order's tooltip names the families it applies to. A condition the accepting machine cannot read does nothing and adds nothing, and the preview shows it greyed out with no reward. The panel warns before accepting such an order.
- Every pressure condition is a cost. The pool loader rejects an entry that raises any stat, so a condition can never add yield, FE, Refinement Potential, Gear stage acceptance, or speed.
- Condition effects are one more `MasteryEffectSource`. They apply after Gear, tree, and ascendancy effects, and the Bonus Summary drawer lists them in their own Work Order section.

### Rush

Rush is the one condition the player adds at acceptance. It never rolls on an order, and refinement cannot add or remove it.

- The Accept dialog has a Rush option. It shows the deadline and the extra reward before the player commits.
- Draft: the deadline is 1.5 times the tier's target time, in world game time. Rush adds +50% Bounty and +1 Prestige.
- The deadline counts world game time, so it keeps running while the machine's chunk is unloaded, while the machine is broken, and while the order is inside a Soul Transfer charge.
- When the deadline passes, the order expires. It is lost, the conditions stop, and nothing drops.

## Refinement and Corruption

An unaccepted Work Order is a refinement target like a machine part:

- **Affix Injector** adds a condition, **Affix Upgrader** raises one condition's tier, **Nullifier Coil** and **Null Crystal** remove one, **Chaos Crystal** rerolls them, and the ascension items promote rarity. Each spends the order's Refinement Potential as usual.
- The **Volatile Catalyst** corrupts an order. Work Orders get their own corruption pools:
    - Blessed: +1 reward roll, +1 Prestige, or a condition that adds reward with no cost.
    - Blighted: one extra condition that adds no reward.
- The [Phase Array](phase-array.md) rejects Work Orders. Its costs are sized for permanent Gear, not a consumable.
- An active order is not a target. It lives in the machine's progression state, not in an item.

## Rewards

A Fulfilled Work Order stores its tier, Bounty, Prestige, family, and the machine's ascendancy. Using it rolls `rngtech:challenges/work_order`:

- The table rolls once per full 100% Bounty, plus one more roll with a chance equal to the remainder. 155% Bounty rolls once, then once more with a 55% chance.
- `ChallengeContext` gains `tier`, `bounty`, and `prestige`. New loot conditions `rngtech:order_tier` and `rngtech:order_prestige` read them with an optional inclusive `min` and `max`, like the existing number conditions. The existing `rngtech:machine_family` and `rngtech:ascendancy` conditions work here too.

Draft default table:

| Pool | Condition | Entries |
|---|---|---|
| Next order | Always | One blank Work Order: 60% same tier, 25% one tier higher (Tier V stays V), 15% nothing |
| Currency | Always | Chaos Crystal, Expansion Crystal, Null Crystal, Affix Upgrader, Mastery Refunds |
| Late currency | Tier III or more | Volatile Catalyst, Greater Affix Upgrader, Ascension Matrix |
| Glyphs | Always | One [Glyph](phase-array.md#glyphs-and-work-orders) of the order's tier, plus one per Bounty roll at Prestige 1 or higher |
| Uniques | Prestige 2 or more | One unidentified Unique through `rngtech:unidentified_unique`, weighted toward the order's family |

The next-order pool rolls once per order, not once per Bounty roll, so Bounty scales currency and not the order supply. That keeps sustain a matter of finishing orders, not of stacking conditions.

## Sources

Tier I Work Orders have one default source: a `rngtech:calibrated_shaped` recipe under a `rngtech:work_order_recipes_enabled` condition and the `workOrders.recipesEnabled` config key, following the Seal and Volatile Catalyst precedent. Draft recipe:

| Ingredient | Count | Challenge |
|---|---:|---|
| Calibrated Logic Component, Stage 4+, 60+ stability | 1 | A good Stage 4 calibration |
| Paper | 4 | None. Filler, so an order reads as a document. |
| Steel Plate | 2 | The Stage 4 material anchor |
| Basic Electric Circuit | 2 | The Stage 4 circuit chain |

The recipe makes two orders. Tiers II to V have no recipe and come only from fulfilled orders or pack rewards.

Pack makers can disable the recipe and award orders through quests, their own recipes, or the other challenge tables. A plain `rngtech:work_order` stack from a quest or `/give` rolls its rarity, conditions, and seed the first time a player holds it, as an unidentified Unique does. A command `/rngtech work_order give <players> <tier>` gives one the same way.

## Storage and Saves

- `MachineProgressionState` gains one optional field, `work_order`, a record of `{ tier, rarity, conditions, objective, progress, best_streak, rush_deadline, bounty, prestige }`. Its presence means an order is active. The persistent codec uses `optionalFieldOf("work_order")`, so existing saves load unchanged. The stream codec gains the field.
- Every `new MachineProgressionState(...)` site and every `with...` method must keep the field. A domain check round-trips a state with an active order through each one.
- Because the order lives in the progression state, it travels wherever the state goes:
    - **Soul Transfer** absorbs and releases it with the rest of the Mastery. A released order resumes on the new machine. If the new machine is below the tier's stage requirement, the order stays stored but paused, its conditions do not apply, and the panel says why.
    - **Breaking a machine** keeps it on drop for every family whose drop copies `rngtech:machine_progression`.
    - **Dormant Machine** souls never carry an order.
    - **`forFamily`**, which resets the tree for another family, drops the order.
- The Work Order item stores its traits like other refinement targets, plus `rngtech:work_order_tier` and `rngtech:work_order_seed` data components.

## Loop Prevention

Conditions only add costs, and the pool loader rejects any entry that raises a stat. Rewards are items with no FE value: Work Orders and Fulfilled Work Orders have no Component Recycler or Potential Reactor recipe, and the default reward table has no material, fuel, or ingot entries. The recipe loop audit needs no new allowlist entries. A ModDex check confirms that no recycling or reactor recipe accepts either order item.

## UI

- **Work Order panel:** a clipboard button on the Mastery tab, beside the Ascendancy crest, opens it. With no active order, it lists Work Orders in the player's inventory and previews the selected one for this machine: objective, conditions with their costs and rewards, greyed-out conditions the machine cannot read, Bounty, Prestige, tier requirements, and the Rush option. With an active order, it shows progress, the best streak, the Rush countdown, and Abandon.
- **Tooltip:** tier, rarity, conditions with their rewards, Bounty, Prestige, and "Objective depends on the machine. Preview it on the Mastery tab."
- **Fulfilled Work Order tooltip:** tier, family, Bounty, Prestige, and the number of reward rolls.
- **Bonus Summary drawer:** a Work Order section that lists each active condition's effect.
- **Jade:** the active order's objective and progress, and the Rush countdown.
- **Machine model:** where a shaped model has room, a small order sheet clipped to the body while an order is active. Machines without room show a status light only. This follows the diegetic-cue rule; there is no floating icon.
- **JEI:** an information page for Work Orders and Fulfilled Work Orders that explains tiers and the reward pools.

## Delivery Phases

1. **PRD and state.** This document and [its state page](work-orders-state.md).
2. **Storage:** the `work_order` field, codecs, Soul Transfer carry, drop carry, the `forFamily` rule, and round-trip checks.
3. **Core loop for two families:** the Work Order item, objective catalog and loader, the Accept and Abandon actions in `MasteryOperations`, progress hooks for the Crusher and Furnace, the Fulfilled Work Order, the reward table with the new context fields and conditions, and the Tier I recipe.
4. **Conditions:** the `work_order` eligibility profile and pool, conditions as a `MasteryEffectSource`, the reset-on-event rule conditions, and Rush.
5. **The other five families:** objectives and progress hooks for the Alloy Furnace, Metal Press, Melter, Resonance Calibrator, and Forestry Companion.
6. **Refinement and corruption:** order corruption pools, Phase Array rejection, and first-hold rolling for plain stacks.
7. **UI:** the Work Order panel, tooltips, the drawer section, Jade, JEI, and model cues.
8. **Documentation:** a wiki page, Machine Mastery and Rarity updates, the implementation matrix, the GuideME guide, a Machine Design Notes entry, and an FTB Quests chapter.

## Test Plan

Domain checks (`masteryCheck`):

- `MachineProgressionState` round trips with and without an active order, and existing saves load unchanged.
- Every `with...` method keeps the order, and `forFamily` drops it.
- A Soul Transfer charge keeps the order through absorb and release, and an order on an under-stage machine stays paused.
- The objective for a given seed, tier, and family is stable across runs.
- Every objective's filters match at least one recipe on every chassis that meets the tier's stage requirement.
- The condition pool loader rejects any entry that raises a stat.
- A reset-on-event condition resets progress, and the best streak stays.
- Bounty rolls: 100% rolls once, 155% rolls once plus a 55% chance, over a seeded sample.
- The next-order pool rolls once per order regardless of Bounty.
- Rush expires an order at its deadline and drops nothing.

ModDex checks (`npm run moddex:check`):

- The Tier I recipe carries its config condition.
- No recycling or reactor recipe accepts a Work Order or a Fulfilled Work Order.
- The reward table has no material, ingot, ore, or fuel entries.
- The loop audit passes with no new allowlist entries.

GameTests (`runGameTestServer`):

- Accept an order on a Crusher, run matching recipes, and check that the Fulfilled Work Order drops at the target count.
- A heat failure resets an order with of Brittle Lining.

In-game checks:

- Accept, run, finish, and open an order on each of the seven families.
- Abandon an order and check that nothing returns.
- Move an active order with Soul Transfer, and break and replace a machine with an active order.
- Refine and corrupt an order, then check that the preview and the active effects match the tooltip.
- With the recipe config off, the recipe is absent, and an order from `/give` still works.
- Check the target times against the tier table on a reference build of each family.

## Acceptance Criteria

- All seven Mastery families accept, run, and finish Work Orders of every tier they can reach.
- Every condition's cost and reward show on the order and in the preview before accepting.
- No condition or reward adds yield, FE, Refinement Potential, or Gear stage acceptance.
- Active orders survive Soul Transfer and machine drops, and existing saves load unchanged.
- Tiers II to V come only from fulfilled orders or pack rewards by default.
- `./gradlew ciCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `mkdocs build --strict`, and `mkdocs build --strict -f mkdocs.wiki.yml` pass.

## Decisions

Resolved 2026-10-18:

- An order is accepted from the machine's Mastery tab and consumed from the player's inventory, the same way a Seal is used from the Ascendancy panel. No new Gear slot is added.
- Orders are crafted or dropped only for now. There is no Dispatch Desk.
- An active order travels with the Mastery state, including through Soul Transfer.
- Rush is an opt-in deadline the player adds at acceptance. It never rolls on its own.

## Open Questions

- Release target: a later 2.0 alpha, or the first feature after 2.0.
- Whether the tier requirement should use Mastery level, chassis stage, or both, as drafted.
- Whether Normal orders should exist, or every order should roll at least Magic.
- Whether conditions a machine cannot read should be allowed to roll at all, or the pool should be filtered per family at acceptance.
- Whether the Uniques pool should weight toward the order's family, as drafted, or roll from the whole catalog.
- Whether a Rush deadline should pause while the order is inside a Soul Transfer charge.
