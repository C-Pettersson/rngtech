# Work Orders Implementation State

Source: [draft design](work-orders.md).

Status: **Planned**. The PRD is a draft with open questions and no release target. No code exists.

## Scope checklist

- [x] Draft the PRD and this state page.
- [ ] Resolve the PRD's open questions and accept it.
- [ ] Create a feature branch from clean `main`.
- [ ] Storage: the `work_order` field on `MachineProgressionState`, codecs, Soul Transfer carry, drop carry, the `forFamily` rule, and round-trip checks.
- [ ] Core loop for the Crusher and Furnace: the Work Order item, objective catalog, Accept and Abandon actions, progress hooks, the Fulfilled Work Order, the reward table and its conditions, and the Tier I recipe.
- [ ] Conditions: the `work_order` eligibility profile and pool, conditions as a `MasteryEffectSource`, reset-on-event conditions, and Rush.
- [ ] The other five families: Alloy Furnace, Metal Press, Melter, Resonance Calibrator, and Forestry Companion.
- [ ] Refinement and corruption: order corruption pools, Phase Array rejection, and first-hold rolling for plain stacks.
- [ ] UI: the Work Order panel, tooltips, the Bonus Summary section, Jade, JEI, and model cues.
- [ ] Documentation: wiki page, Machine Mastery and Rarity updates, the implementation matrix, the GuideME guide, Machine Design Notes, and an FTB Quests chapter.
- [ ] In-game playtest of every family and tier.

## Decisions

Recorded 2026-10-18 while drafting:

- An order is accepted from the machine's Mastery tab and consumed from the player's inventory, the same way a Seal is used from the Ascendancy panel. No new Gear slot is added.
- Orders are crafted or dropped only. There is no Dispatch Desk for now.
- An active order is part of the Mastery state, so it travels through Soul Transfer.
- Rush is an opt-in deadline added at acceptance. It never rolls on its own.

## Findings

Recorded 2026-10-18 from the code on `main` at `fad45920`:

- Seals are not used by right-clicking a machine. The Ascendancy panel sends an `ascend` action, and `MasteryOperations.ascendancy` takes the Seal from the player's inventory through `AscendancyPayment`. Creative players need the Seal but do not use it up. Accept can follow the same payment pattern.
- `MachineProgressionState` already has optional fields with defaults (`ascendancy`, `ascendancy_nodes`, `seal_tiers`), so a `work_order` field can follow `optionalFieldOf` with no migration.
- `SoulTransfer.Charge` stores the whole `MachineProgressionState`, so an order stored there travels with no Soul Transfer change. A charge's `releaseStage` already handles stage limits on release; the paused-order rule is separate.
- `MasteryEffectSource` is the shared interface for tree and ascendancy nodes. It already carries effects, tagged effects, behaviors, fixed values, and ceilings, which covers both pressure and rule conditions.
- `ChallengeContext` holds family, machine stage, recipe stage, stability, instability, Mastery level, and ascendancy. Adding tier, Bounty, and Prestige means new fields there and two new loot conditions.
- `MachineMasteryHost.grantMasteryXp` runs for completed work, so it marks most places that need a progress hook. Streak objectives also need the failure events: the Metal Press failure output, the heat-failure challenge roll, the Crusher jam, and the calibration result.
- The parallel-work stat is `BATCH_SIZE`, shown as Batch Size. `parallel_jobs` is a legacy id alias.
- `MachineMasteryFamily.topStage()` gives each family's highest stage, which the tier stage rule needs.

## Progress log

- 2026-10-18: drafted the PRD and this state page.
- 2026-10-21: the reward table now pays Glyphs for the [Phase Array](phase-array.md), and the Array replaces the Exotic Affix Forge as the station that rejects orders.

## Verification

- 2026-10-18: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.

## Handoff and limits

- Tier stages, Mastery levels, target times, condition values, rewards, and the Tier I recipe are draft numbers. Tune them with the processing balance simulation before coding phase 3.
- The objective filters assume each family can report the listed values at the moment work completes. Check each hook before committing to an objective type.
- Shaped models exist for some families only, so the order sheet cue is per model, with a status light as the fallback.
