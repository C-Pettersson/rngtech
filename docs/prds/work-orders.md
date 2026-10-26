# PRD: Work Orders

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the [Work Orders](https://c-pettersson.github.io/rngtech/work-orders/) wiki page for current behavior.

PRD status: Draft

Implementation status: Prototype

Release: Not scheduled

Last updated: 2026-10-26

## Summary

Work Orders are a repeatable loop of jobs for processing machines, modeled on quests from an MMORPG vendor and on Path of Exile's maps. Each order is a **Job Chip**: a data chip that holds one job. A **Dispatch Terminal** lists six randomly rolled jobs. A job is rolled when the terminal lists it, so the chip is readable before the player takes it: it shows its machine family, its job, its tier, its conditions, and the exact reward it pays, such as "Crusher: crush 96 Raw Tin and 64 Raw Lead. Pays 4 Chaos Crystals". Pack makers can add specific jobs by datapack, and a config switch turns the random ones off so a pack offers only its own. The player downloads the chips they want, refreshes the listing with a crafted **Requisition**, and inserts chips into the **drive bays** on each machine's **Orders** tab.

A machine has three drive bays but works on one chip at a time. Switching the active chip keeps every chip's progress. When the machine finishes the active chip's job, the chip stays in its bay as a Completed Job Chip until the player takes it out and redeems it for the listed reward. Some jobs pay Requisitions, so the loop can feed itself.

Today a machine's Mastery has no goal after its third Seal and level 100, Stage 8 chassis have little to do that a Stage 7 chassis can't, and machines without Mastery have no goals at all. Work Orders give each of these a job:

- Every processing machine gets repeatable goals, including machines with no Mastery family, such as the Component Assembler.
- Higher tiers need higher chassis stages, up to each family's top stage.
- A job can also ask for a Mastery level, but the default jobs don't.
- The reward table is a seventh challenge table, with default loot like the Glyph entries the [Phase Array](phase-array.md) adds to the other six.

Orders reuse the existing systems. Conditions are affixes, so the Affix Forge, Nullifier Coil, Chaos Crystal, and Volatile Catalyst all work on a chip. Condition effects join the machine's stat pipeline as one more effect source. Rewards roll through `ChallengeLoot`.

## References

- [Machine Mastery](../systems/machine-mastery.md)
- [Machine Ascendancies PRD: Loop Prevention](machine-ascendancies.md#loop-prevention)
- [Unique Items PRD: Challenge loot](uniques.md#challenge-loot)
- [Corruption PRD](corruption.md)
- [Heat Model PRD](heat-model.md)
- [Batch Processing PRD](batch-processing.md)
- [Affix Generation](../systems/affix-generation.md)
- [Machine Guidelines](../reference/machine-guidelines.md)

Key existing code:

- `src/main/java/com/rngtech/content/blockentity/BaseMachineBlockEntity.java` (machine components, stats cache)
- `src/main/java/com/rngtech/rpg/progression/MasteryEffectSource.java`
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryFamily.java` (`topStage`)
- `src/main/java/com/rngtech/content/loot/ChallengeLoot.java`, `ChallengeContext.java`
- `src/main/java/com/rngtech/rpg/refinement/RefinementTargets.java`, `RefinementEngine.java`
- `src/main/java/com/rngtech/content/blockentity/HeatControl.java` (overheat slider)

## Goals

- A repeatable goal for every processing machine, with or without a Mastery family.
- The player chooses jobs from a vendor, as from a quest giver, and can refresh the listing with a crafted currency.
- Jobs vary enough to be worth choosing between: specific items, any work of a stage, large broad counts, and stat goals that reward building the machine, with counts and targets rolled within a range.
- Each job pays one kind of reward, so choosing a job means choosing what to earn.
- Orders are random, but each one is rolled when listed, so the chip shows everything before the player takes it: family, job, tier, requirements, conditions, and reward. No hidden penalty applies.
- Conditions are affixes, so the existing refinement and corruption tools shape an order with no new currency.
- Higher tiers ask for higher chassis stages, up to each family's top stage, so Stage 7 and 8 bodies have an endgame role.
- Rewards are refinement currency, Glyphs for the Phase Array, Uniques, and Requisitions. They are never FE and never yield, so orders cannot create a loop.
- The Dispatch Terminal and Requisitions are gated by RNGTech's own challenges.
- Pack makers can write their own orders by datapack, hand them out through quests and commands, replace the objective, condition, and reward data, and turn the random orders off by config.

## Non-Goals

- No Work Orders on generators, Battery Chassis, or the Miner's Companion. Their work is FE or mining, not processing.
- No condition that raises yield, Refinement Potential, FE generation, or Gear stage acceptance.
- No Mastery XP bonus or tree points from orders. The 21 reserved Mastery points stay with the Tome of XP.
- No Seals or Seal Cores in the default rewards. Seals stay the precision trial that the ascendancy design asks for.
- No raw materials, ingots, or ores in the default rewards. They would undercut the processing chain and the ore stages.
- No more than one active order per machine.
- No automation into or out of drive bays. Chips are handled by hand.
- No HUD icon or overlay. Status cues on the machine are diegetic or appear in Jade and the machine screen.

## Vocabulary

| Term | Meaning |
|---|---|
| Work Order | The system and one job in it. |
| Job Chip | `rngtech:job_chip`, the item that holds one Work Order, rolled when listed and readable before it is taken: family, job, tier, requirements, rarity, conditions, and reward. |
| Completed Job Chip | `rngtech:completed_job_chip`, what a chip becomes in its bay when the machine finishes its job. Redeeming it pays the chip's listed reward. |
| Dispatch Terminal | `rngtech:dispatch_terminal`, the vendor block. Its screen lists six jobs that the player downloads as chips. |
| Requisition | `rngtech:requisition`, the crafted currency that refreshes a terminal's listing. |
| Orders tab | A tab on every eligible machine's screen, beside Mastery, with the machine's three drive bays. |
| Drive bay | A chip slot on a machine. Inserting a chip loads its job; pulling it out unloads it with its progress. |
| Family | The machine kind a job is for, such as Crusher or Component Assembler. For Mastery machines it is the Mastery family. |
| Tier | I to V. It sets the job size, the default chassis stage requirement, and the reward size. |
| Objective | The job: one or more parts, each a count of completed work that matches a filter, or a stat goal. |
| Stat goal | An objective met when the machine's effective stat reaches a target, such as a Furnace reaching 2,000 Max Temperature. |
| Requirements | What the machine needs to load the chip: a chassis stage, and optionally a Mastery level. |
| Condition | A rolled prefix or suffix on the chip. It applies a cost to the machine while the job is active, and adds Bounty or Prestige. |
| Bounty | A percent, 100% at base. It sets how big the reward is when the terminal rolls the job. Players see the result as the chip's reward, not the percentage. |
| Prestige | A whole number, 0 at base. It unlocks better reward kinds. |
| Active chip | The one inserted chip the machine counts work toward and whose conditions apply. |
| Rush | An opt-in deadline the player can add when activating a chip. It never rolls on its own. |
| Random job | A job the terminal rolls from the objective and condition pools. On by default; `workOrders.randomOrdersEnabled` turns it off. |
| Job template | A datapack entry that defines a specific job, for pack makers and quests. |

## Player Flow

1. The player places a Dispatch Terminal. Its screen lists six jobs, each with its family, job, tier, requirements, conditions, and reward.
2. The player downloads the jobs they want. Each becomes a Job Chip in their inventory, and its line on the terminal stays empty.
3. A Requisition refreshes the terminal: every listed job is replaced by a new roll, and empty lines fill again.
4. The player may shape a chip in the Affix Forge or a Refinement tab: add a condition for more reward, remove one to make the job safer, reroll it, or corrupt it.
5. On a machine of the chip's family, the player opens the Orders tab and inserts the chip into a drive bay. The bay accepts it only if the machine meets its requirements: chassis stage, and Mastery level if the chip has one.
6. The first chip inserted into a machine with none active becomes active. The player can activate another bay's chip, or pause the active one, and can add Rush when activating.
7. While a chip is active, its conditions apply, and the machine counts matching work toward its job. The Orders tab, the Bonus Summary drawer, and Jade show progress.
8. Switching to another chip pauses the current one. Its progress and best streak stay, and its conditions stop applying.
9. When the active chip's job is complete, it becomes a Completed Job Chip in the same bay and is no longer active. It keeps the bay until the player takes it out, cannot be made active again, and no other chip becomes active on its own.
10. The player redeems the Completed Job Chip for its listed reward.

Pulling a chip out of its bay unloads it, progress kept; pulling the active chip leaves the machine with none active. The player can **Abandon** a chip from its bay, which destroys it and returns nothing.

## Eligible Machines

Work Orders work on every processing machine: a machine that turns inputs into outputs. That includes the seven Mastery families and the machines without Mastery, such as the Component Assembler and the Component Recycler. Generators, Battery Chassis, the Miner's Companion, and storage blocks are not eligible.

A machine without a Mastery family has a family for orders only. Its tier stage requirement uses that machine's own highest stage.

## Dispatch Terminal

The **Dispatch Terminal** is the vendor:

- One block with six job listings. A new terminal fills its listing the first time it is opened.
- Each listing is a full job, rolled from the pools: a tier, then an objective by weight and its family, its counts, a rarity, conditions, and its reward. Every tier is equally likely for now, so one terminal lists Tier I to V jobs side by side, and the machine's chassis stage decides which chips it can load.
- Downloading a job is free and moves its chip into the player's inventory. An empty line stays empty until a Requisition refreshes the terminal.
- A Requisition replaces all six listings, including any the player has not downloaded.
- A broken terminal keeps its listing, empty lines included, on the dropped item, so placing it again never refills it for free.
- The terminal's recipe uses the Stage 4 calibration gate: a Calibrated Logic Component with 80+ stability, which only the Stage 4 Calibrator's resonant recipe reaches, plus Glass Panes, Steel Plates, and Basic Electric Circuits, under the `workOrders.recipesEnabled` config condition.
- Pack makers can add job templates to the terminal's pools. With `workOrders.randomOrdersEnabled` off, the terminal lists only templates, and a terminal with no pooled templates lists nothing.

## Requisitions

- `rngtech:requisition` is crafted. Its recipe is gated by an RNGTech challenge, following the Seal and Volatile Catalyst precedent, and sits under a `rngtech:work_order_recipes_enabled` condition and the `workOrders.recipesEnabled` config key.
- Requisitions are one of the reward kinds a job can pay, so some jobs fund more refreshes. No job pays them by default.
- Draft recipe: a Calibrated Logic Component (65+ stability), Paper, Steel Plates, and Basic Electric Circuits. One recipe makes two.
- Requisitions have no tier. The terminal rolls the tiers.

## Tiers

| Tier | Default chassis stage | Draft objective size |
|---|---:|---|
| I | 4 | About 15 minutes of steady work on a good Stage 4 build |
| II | 5 | About 20 minutes on a good Stage 5 build |
| III | 6 | About 25 minutes on a good Stage 6 build |
| IV | 7 | About 30 minutes on a good Stage 7 build |
| V | 8 | About 30 minutes on a good Stage 8 build |

The stage requirement is the lower of the tier's stage and the family's top stage, because the Metal Press tops out at Stage 4, the Alloy Furnace at 6, and the Melter has one body. Default orders have no Mastery level requirement. An order template can add one, and a condition can add one. Objective sizes are tuned to time on a reference build, and the tuning uses the processing balance simulation.

## Objectives

Objectives come from `data/rngtech/work_orders/objectives/*.json`. Each entry names a family, a kind, a weight, and a goal for each tier. The terminal rolls a job's objective, and every random part of it, when it lists the job, so the chip shows its exact job before the player takes it.

The mix should feel balanced, with no one kind dominating. Draft kinds, tuned by entry weight rather than fixed rules:

- **Specific items:** one to three named items, each with its own count, such as "Smelt 96 Tin Dust and 64 Copper Dust". An entry lists options, each a recipe id, an input tag, or an output item or tag, with a weight and a stage. The terminal picks the parts when it rolls the job, never the same option twice in one job. Each part counts separately, and the job is done when every part is.
- **Groups of work:** one or two kinds of output, counted across every recipe of that kind, such as "Press 96 plates and 48 gears" on the Metal Press (by mold: plates, gears, casings, connectors) or "Calibrate 24 Logic components" on the Resonance Calibrator (by calibration family). A group entry uses `"filter": "group"` with options naming the groups its family reports.
- **Any work of a stage:** a count of any completed work whose recipe is at or above a stage, such as "Crush 256 items of Stage 6 or higher".
- **Broad counts:** a large count of any completed work, with no stage or item filter, such as "Smelt 768 items".
- **Work with a machine filter:** a count of work done a particular way, such as "Smelt 192 items with the heat slider at 50% or more" or "Crush 256 ores that need Processing Level 6 or more".
- **Stat goals:** reach a value of one of the machine's effective stats, such as "Reach 2,000 Max Temperature" on a Furnace, or "Reach 40% Bonus Output Chance" on a Crusher. They reward building and refining the machine rather than running it longer.

### Counts and targets

Each tier's goal is a range, not one number: a count range for counted parts, and a target range for stat goals. The terminal rolls a value in the range and rounds it to the entry's step, such as a multiple of 8 items or 50 °C, so jobs read cleanly and rarely stack. Draft: ranges stay within about 20% either side of their middle, so jobs of one tier stay comparable. A single number still works and means a fixed goal. Broad counts use larger ranges than specific items, because any work counts toward them.

The range is split into three equal bands: low, middle, and high. The band sets a Bounty bonus, which makes the reward bigger. Every value inside one band pays the same. Draft bonuses: low +0%, middle +10%, high +20% Bounty. A fixed goal has no bands and no bonus. A job with several parts uses the band of its largest part.

### Examples

| Family | Kind | Example |
|---|---|---|
| Crusher | Specific items | Crush 96 Raw Tin and 64 Raw Lead |
| Crusher | Any work of a stage | Crush 256 items of Stage 6 or higher |
| Crusher | Broad count | Crush 640 items |
| Crusher | Machine filter | Crush 256 ores that need Processing Level 6 or more |
| Crusher | Stat goal | Reach 40% Bonus Output Chance |
| Furnace | Specific items | Smelt 180 Naquadah Dust |
| Furnace | Any work of a stage | Smelt 192 items of Stage 5 or higher |
| Furnace | Broad count | Smelt 768 items |
| Furnace | Machine filter | Smelt 192 items with the heat slider at 50% or more |
| Furnace | Stat goal | Reach 2,000 Max Temperature |
| Alloy Furnace | Any work of a stage | 96 Stage 6+ alloy cycles |
| Metal Press | Machine filter | 200 presses in a row without a failure |
| Melter | Any work of a stage | Melt 48,000 mB of Stage 6+ metal |
| Resonance Calibrator | Machine filter | 12 calibrations in a row at 80+ stability |
| Forestry Companion | Machine filter | Harvest 300 trees with a Stage 6+ head |
| Component Assembler | Any work of a stage | Assemble 64 Stage 5+ components |
| Component Recycler | Any work of a stage | Recycle 96 Stage 4+ parts |

### Rules

- Progress counts only work this machine finishes while the chip is active. A batch counts each item it completes.
- A streak objective resets to 0 on the event that breaks it, such as a Malformed Ingot. The Orders tab shows the best streak so far. A streak part uses `"filter": "streak"` with an optional `min` on the work's setting (such as a calibration's stability): qualifying work of its verb counts, and other work of its verb or a machine failure breaks it. It only rolls for families whose machines report what breaks it (the Metal Press and the Resonance Calibrator so far).
- A specific-item part is rolled only from recipes that are loaded, so an option whose material a pack disabled never rolls. An option's stage must be at or below the job's stage requirement.
- A stat goal reads the machine's effective stat with every source applied: traits, Gear, Mastery, and the chip's own conditions. It is met at the first job the machine completes while the stat is at or above the target, so the build has to run, not just be installed for a moment. Until then the Orders tab shows the current value against the target.
- A stat goal entry uses `"filter": "stat"`, names its `stat`, and gives each tier a `target` in the stat's own units (such as `3.1` Processing Speed or `12` Super Output Chance), fixed or as a `min`/`max`/`step` range. Targets are kept in hundredths. A template part can be a stat goal the same way. The Orders tab and the chip show the machine's value at its last completed job against the target, written the way the Stats tab writes that stat.
- A stat goal asks for a stat; it never grants one. Only stats players can raise through Gear, refinement, or Mastery can be goals, listed per family in the entry.
- An objective must be reachable on every chassis that meets the job's stage requirement. A domain check runs each counted part's filters against the family's recipes, and each stat goal's top target against a reference build of that stage from the processing balance simulation.
- New objective kinds and filters need a reporting hook in each machine that uses them.

## Order Templates

Pack makers write fixed jobs in `data/<namespace>/work_orders/templates/*.json`. A template sets any of these and leaves the rest to roll: family, tier, objective (an objective id, or inline parts or a stat goal with fixed values or ranges), requirements (chassis stage, Mastery level), rarity, conditions, Bounty, Prestige, a display name, a fixed reward, and a reward table that replaces `rngtech:challenges/work_order` for this job.

- `/rngtech work_order give <players> template <template> [tier]` gives a template's chip; the tier only applies when the template leaves it unset. `/rngtech work_order give <players> random [tier]` rolls one the way the terminal does, of the given tier or a random one; with random jobs off, it rolls from the pooled templates instead.
- Quests hand out template chips through the give command or an item reward with the chip's components.
- A template can join the Dispatch Terminal's pools with a weight, or stay quest-only.
- Template fields left unset still roll, so a template can fix only the family and objective and keep random conditions. A template that sets every field is fully specific.

The shipped format, with every key, is on the wiki's [Modpack Creators](https://c-pettersson.github.io/rngtech/modpack-creators/#job-templates) page.

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
| Prefix | Veteran Crew | Needs Mastery level 60–90 to load | +20%–40% Bounty, +1 Prestige | Mastery families |
| Suffix | of Hand Feeding | Instant Process does not trigger | +20% Bounty | Machines that roll Instant Process |
| Suffix | of Brittle Lining | A heat failure or Malformed Ingot resets progress to 0 | +35% Bounty, +1 Prestige | Furnace, Alloy Furnace, Metal Press |
| Suffix | of Fine Tolerance | A calibration below the objective's stability resets progress to 0 | +35% Bounty, +1 Prestige | Resonance Calibrator |
| Suffix | of Deep Ore | Only work on recipes one stage above the objective's filter counts | +25% Bounty, +1 Prestige | All |
| Suffix | of the Jammed Line | A Crusher jam resets progress to 0 | +30% Bounty, +1 Prestige | Crusher |

Rules:

- Phase 6 ships the conditions the Crusher and Furnace can read: Starved Feed, Power Surcharge, Short Crew, Cold Start (Furnace), of Hand Feeding (Crusher), of Brittle Lining (Furnace), of the Jammed Line (Crusher), and of Deep Ore (stage jobs). Thin Margins needs a safe-band reader, Veteran Crew is a load requirement rather than a stat cost, and Unsteady Hands and of Fine Tolerance have no Crusher or Furnace reader; they wait for later machines or phases. Hand Feeding is a suffix in the shipped pool, since it changes a rule rather than a stat's size.
- Listed jobs roll Normal, Magic, or Rare at 50/35/15 (draft). Normal chips exist: half the listings are plain jobs, and conditions are a choice the player takes for more Bounty.
- Rarity sets the condition count: Normal 0, Magic 1–2, Rare 3–4. Normal orders still pay a base reward. Rarity changes only the conditions, never the kind of job: any rarity can be a work objective, an item objective, or a stat goal.
- Condition tiers follow normal affix tiers. A higher tier is a harsher cost and a larger reward.
- Because an order knows its family, only conditions that family can read roll on it.
- Every pressure condition is a cost. The pool loader rejects an entry that raises any stat, so a condition can never add yield, FE, Refinement Potential, Gear stage acceptance, or speed.
- Condition effects are one more effect source on the machine. They apply after Gear, tree, and ascendancy effects, only while the order is active, and the Bonus Summary drawer lists them in their own Work Order section. Machines without Mastery read them through the same source.

### Rush

Rush is the one condition the player adds when activating an order. It never rolls on an order, and refinement cannot add or remove it.

- The Orders tab shows the deadline before the player commits.
- Draft: the deadline is half the tier's target time (7:30 at Tier I up to 15:00 at Tier V), in world game time. Rush adds 50% to the listed reward's amount. The first draft, 1.5 times the target time, was no pressure in playtest, because the draft job sizes finish well inside the target time.
- The deadline keeps running while the chip is inactive or out of its bay, while the machine's chunk is unloaded, and while the machine is broken.
- When the deadline passes, the chip expires. It is lost, the conditions stop, and nothing is ejected.

## Refinement and Corruption

A Job Chip is a refinement target like a machine part:

- **Affix Injector** adds a condition, **Affix Upgrader** raises one condition's tier, **Nullifier Coil** and **Null Crystal** remove one, **Chaos Crystal** rerolls them, and the ascension items promote rarity. Each spends the chip's Refinement Potential as usual.
- The **Volatile Catalyst** corrupts a chip. Job Chips get their own corruption pools:
    - Blessed: a larger reward, +1 Prestige, or a condition that adds reward with no cost.
    - Blighted: one extra condition that adds no reward.
- The [Phase Array](phase-array.md) rejects Job Chips. Its costs are sized for permanent Gear, not a consumable.
- A chip in a drive bay is not a target. Pulled out, it is a target again, with its progress kept.

## Rewards

Every job pays **one kind of reward**, listed on the chip, so the player chooses jobs by what they pay instead of getting a little of everything. When the terminal rolls a job, it also rolls the reward once from the template's reward table, or `rngtech:challenges/work_order` by default, and stores it on the chip. The reward rides with the chip into the machine and onto the Completed Job Chip, and redeeming that pays exactly the listed reward. A chip without a listed reward, such as one a pack hands out without rolling it, rolls the table when it is redeemed.

- The table holds one pool of reward kinds, each with a weight, a base amount for each tier, and optional conditions. One entry is picked.
- Bounty scales the amount: the base amount times Bounty, rounded down, with the rest as a chance of one more. 100% pays the base amount; 150% pays half again. Count bands, conditions, Rush, and corruption raise Bounty, so they make the one reward bigger rather than adding kinds.
- Prestige unlocks rarer kinds through entry conditions.
- `ChallengeContext` carries the job's tier, Bounty, and Prestige. The loot conditions `rngtech:order_tier` and `rngtech:order_prestige` read them with an optional inclusive `min` and `max`. The loot function `rngtech:set_order_tier` sets a Glyph's tier to the job's tier plus an optional `offset`. The existing `rngtech:machine_family` and `rngtech:ascendancy` conditions work here too.

Draft default reward kinds:

| Kind | Condition | Base amount |
|---|---|---|
| Chaos Crystals | Any tier | 2 to 6 by tier |
| Expansion Crystals | Any tier | 1 to 4 by tier |
| Null Crystals | Any tier | 1 to 3 by tier |
| Affix Upgraders | Any tier | 1 to 3 by tier |
| Mastery Refunds | Any tier | 2 to 8 by tier |
| Requisitions | Any tier | 1 to 3 by tier |
| A [Glyph](phase-array.md#glyphs-and-work-orders) of the job's tier | Any tier | 1 |
| Volatile Catalysts, Greater Affix Upgraders, or an Ascension Matrix | Tier III or more | 1 to 2 |
| An unidentified Unique through `rngtech:unidentified_unique` | Prestige 2 or more | 1 |

## Storage and Saves

- An eligible machine stores its chips in a `rngtech:work_orders` component on the block entity: up to three, each with its full job data plus `progress`, `best_streak`, and `rush_deadline`, and the index of the active one. The persistent codec is lenient, so a chip that no longer decodes is dropped without touching the machine's other data.
- The drive bays are slots over that component: inserting a chip loads it, and pulling one out returns a chip that carries its progress in `rngtech:work_order_progress`.
- Breaking a machine keeps its chips on the dropped item, the same way the machine keeps its traits.
- Chips stay with the machine. They do not move with Mastery through a Soul Transfer.
- A Job Chip stores its traits like other refinement targets, plus `rngtech:work_order` data: family, tier, objective, requirements, Bounty, Prestige, the listed reward, and an optional template id.

## Loop Prevention

Conditions only add costs, and the pool loader rejects any entry that raises a stat. Rewards are items with no FE value: Job Chips, Completed Job Chips, and Requisitions have no Component Recycler or Potential Reactor recipe, and the default reward table has no material, fuel, or ingot entries. The recipe loop audit needs no new allowlist entries. A ModDex check confirms that no recycling or reactor recipe accepts any of the three items.

## UI

- **Dispatch Terminal screen:** a terminal display with no player inventory. Six job lines show each chip's machine, tier, job, requirements, and reward, with a Download button that moves the chip into the inventory; hovering a line shows the chip's full tooltip. Refresh and the Requisition count sit below the display.
- **Orders tab:** its own tab on every eligible machine, beside Mastery. It shows three drive bays as real chip slots with the player's inventory below, the active chip's job, per-part progress, best streak, and Rush countdown, and Activate, Pause, and Abandon controls. Bays refuse a chip for another family or one the machine does not meet.
- **Tooltip:** family, tier, job with each part, requirements, rarity, conditions, and the reward it pays.
- **Completed Job Chip tooltip:** tier, family, and the reward it pays.
- **Bonus Summary drawer:** a Work Order section that lists each active condition's effect.
- **Jade:** the active chip's job and progress, and the Rush countdown.
- **Machine model:** where a shaped model has room, a drive bay panel shows one lit chip per inserted chip, the active one brighter. Machines without room show a status light only. This follows the diegetic-cue rule; there is no floating icon.
- **JEI:** an information page for Job Chips, the Dispatch Terminal, and Requisitions that explains tiers and the reward kinds.

## Delivery Phases

1. **PRD and state.** This document and [its state page](work-orders-state.md).
2. **Order data and storage:** the readable job data, the `rngtech:work_orders` machine component with three slots and an active index, drop carry, and round-trip checks.
3. **Core loop for the Crusher and Furnace:** loading, activating, switching, unloading, and abandoning on an Orders tab; progress hooks; the Completed Job Chip; the reward table with its conditions and functions.
4. **Dispatch Terminal and Requisitions:** the vendor block, job rolling with count ranges, one-kind rewards, Download and Refresh, the Requisition recipe, and the give commands.
    - **Job Chips and drive bays:** the chip and completed chip items, the Dispatch Terminal model and screen, and three drive bays on the Crusher and Furnace Orders tab.
    - **Job mix for the Crusher and Furnace:** specific-item jobs with one to three parts, any-work-of-a-stage jobs, and broad counts.
5. **Order templates:** the template loader, the terminal pools, per-template reward tables, quest-friendly give commands, and the `workOrders.randomOrdersEnabled` config key.
6. **Conditions:** the `work_order` eligibility profile and pool, conditions as an effect source on any machine, reset-on-event rule conditions, and Rush.
7. **More jobs and machines:** stat goals for the Crusher and Furnace, then objectives and progress hooks for the other Mastery families and the machines without Mastery, starting with the Component Assembler.
8. **Refinement and corruption:** order corruption pools and Phase Array rejection.
9. **UI polish:** the Bonus Summary section, Jade, JEI, and the drive bay panels on machine models.
10. **Documentation:** a wiki page, Machine Mastery and Rarity updates, the implementation matrix, the GuideME guide, a Machine Design Notes entry, and an FTB Quests chapter.

## Test Plan

Domain checks (`masteryCheck`):

- The machine's order component round trips with zero to three orders, and machines saved before Work Orders load unchanged.
- Switching the active order keeps every order's progress and best streak.
- Pulling a chip out of its bay returns a chip that loads again with its progress.
- The terminal's listing for a given seed is stable, and rolled conditions only come from the job family's readable pool.
- Every counted part's filters match at least one loaded recipe on every chassis that meets the job's stage requirement, and every stat goal's top target is reachable on a reference build of that stage.
- A specific-item job has one to three parts, never the same option twice, and is done only when every part is.
- A rolled count or target stays inside its tier's range and lands on the entry's step, and its band's Bounty bonus matches the band the value falls in.
- A specific-item part never rolls an option whose recipe is not loaded.
- A stat goal is met at the first completed job with the stat at or above its target, and not while the machine is idle.
- The condition pool loader rejects any entry that raises a stat.
- A reset-on-event condition resets progress, and the best streak stays.
- A rolled reward is one kind, and Bounty scales its amount: 100% pays the base amount, 150% half again, over a seeded sample.
- Rush expires an order at its deadline and drops nothing.
- A template loads with any subset of its fields and rolls the rest.
- With random jobs off, the terminal's listing and `give random` draw only from pooled templates.

ModDex checks (`npm run moddex:check`):

- The Requisition and Dispatch Terminal recipes carry their config condition.
- No recycling or reactor recipe accepts a Job Chip, a Completed Job Chip, or a Requisition.
- The reward table is one pool, so every job pays one kind.
- The reward table has no material, ingot, ore, or fuel entries.
- The loop audit passes with no new allowlist entries.

GameTests (`runGameTestServer`):

- Insert a chip into a Crusher's bay, run matching recipes, and check that the chip becomes a Completed Job Chip in its bay at the target count, with nothing dropped.
- Switch between two chips and check that each keeps its progress.
- Break a machine with chips in its bays and place it again.
- A bay refuses a chip for another family or one the machine does not meet.
- A heat failure resets an order with of Brittle Lining.

In-game checks:

- Download, refresh, insert, run, finish, and redeem chips on each eligible machine.
- Switch, pull out, and abandon chips.
- Refine and corrupt a chip, then check that the Orders tab and the active effects match the tooltip.
- With the recipe config off, the recipes are absent, and a template order from a command still works.
- With random jobs off, the terminal lists only pooled templates.
- Check the target times against the tier table on a reference build of each family.

## Acceptance Criteria

- Every eligible machine loads, runs, switches, and finishes Work Orders of every tier it can reach.
- Every chip's family, job, requirements, conditions, and reward show before the player takes it.
- No condition or reward adds yield, FE, Refinement Potential, or Gear stage acceptance.
- Chips in drive bays survive machine drops, and existing saves load unchanged.
- Pack makers can define, give, and pool their own orders without code, and can turn the random orders off by config.
- `./gradlew ciCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `mkdocs build --strict`, and `mkdocs build --strict -f mkdocs.wiki.yml` pass.

## Decisions

Resolved 2026-10-18:

- Rush is an opt-in deadline the player adds when activating an order. It never rolls on its own.

Resolved 2026-10-25, after playtesting the first loop and the Orders tab. These replace the 2026-10-18 decisions that orders are accepted from the Mastery tab, that there is no vendor block, and that orders travel with Mastery:

- A Dispatch Board block is the quest vendor. Players take the orders they want and reroll the offer with a crafted Requisition.
- Orders are random, but rolled when offered, so the item shows its family and job before the player takes it. Pack makers add specific orders by datapack, and `workOrders.randomOrdersEnabled` turns the random ones off.
- Work Orders get their own Orders tab on the machine, not a button under Mastery.
- Work Orders work on every processing machine, including machines without Mastery such as the Component Assembler.
- Orders need no Mastery level by default. Templates and conditions can add one.
- A machine holds up to three loaded orders with one active. Switching keeps progress.
- Loaded orders are stored on the machine, not in Mastery, and do not move through Soul Transfer.
- Pack makers can define custom orders as datapack templates.
- Jobs come in three kinds: work objectives, item objectives that name one item or recipe, and stat goals such as reaching a Max Temperature. Every tier's count or target rolls within a range.
- Rarity sets only how many conditions an order rolls. It does not decide the kind of job.
- One Dispatch Board block offers every tier, equally weighted for now. Requisitions have no tier.
- A taken order's slot stays empty until a Requisition rerolls the board.
- The Dispatch Board's recipe uses the Stage 4 calibration gate.
- Each range has three bands with their own Bounty bonus. Within a band, every value pays the same.
- Every order lists the exact items it pays, rolled with the order; Bounty is how big that roll is, not a number the player reads.

Resolved 2026-10-25, after playtesting the Dispatch Board. These replace the paper orders, the notice board, and the many-pool reward:

- Orders are Job Chips, a fit for a tech mod. The Dispatch Terminal replaces the Dispatch Board, and machines take chips in three real drive bays on the Orders tab, with no automation.
- Each job pays one kind of reward, scaled by tier and Bounty. Requisitions are one possible kind, not paid by every job.
- The job mix is balanced across specific items (one to three), any work of a stage, broad counts, machine filters, and stat goals. The examples are tuning targets, not hard rules.

## Open Questions

- Release target: a later 2.0 alpha, or the first feature after 2.0.
- Whether a machine should have three drive bays, as drafted, or more.
- Whether the Unique reward should weight toward the job's family or roll from the whole catalog.
