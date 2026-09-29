# Machine Ascendancies Implementation State

Source: [draft design](machine-ascendancies.md).

Status: **Planned** for the 2.0 release — PRD accepted 2026-09-29 on `feature/machine-ascendancies`. No code yet.

## Scope checklist

- [x] Create `feature/machine-ascendancies` from clean `main`.
- [x] Draft the PRD and this state page.
- [x] Resolve the PRD's open questions.
- [ ] Framework: catalog index loader, shared node parser, stat declarations, behavior registry, `ascendancyEntryStage()`, state fields, load validation, `forFamily()` family check, effect pipeline, build codes, generic catalog validation, fixture ascendancy, domain checks.
- [ ] Loop safety: eligibility field on Alloy Furnace, Metal Press, Melter, and calibration recipes; loop audit in report mode reading declared yield stats and behaviors; fix or allowlist known cycles; turn on the CI gate.
- [ ] Seals: items, recipes, `rngtech:ascendancy_seal_recipes_enabled` condition and config key, entry gate, server actions.
- [ ] UI: Ascendancy panel and choose dialog in `MasteryScreenSupport`, Stats tab visibility, Bonus Summary section, Jade line, JEI.
- [ ] Crusher and Furnace content with their new stats and shared mechanics, then an in-game playtest.
- [ ] Alloy Furnace, Metal Press, Resonance Calibrator, Melter, and Forestry Companion content.
- [ ] Canonical documentation, Machine Stats entries, implementation matrix, getting-started, quests, and ModDex view.

## Decisions

Recorded 2026-09-29:

- Ascendancies depend entirely on the machine family. Each family starts with two.
- Ascendancies use their own points and items.
- Each Seal grants 2 points. There are three tiers, for 6 points in total.
- Tiers follow material stages and lean toward late game. The last tier is aspirational.
- The 21 reserved Mastery allocations belong to the Tome of XP, a restricted pack-maker item. They are not ascendancy points.
- Ascendancies follow the Path of Exile model:
    - choose on the first completion;
    - notables sit behind small nodes;
    - refunding one point costs five refunds;
    - switching requires an empty ascendancy tree.
- No looping or infinite recipes, including FE loops. This is a hard requirement.
- A machine may be priced out of running by power usage; that softlock is acceptable.
- Refiner's Oath grants 5% more Output Amount per Parallel Job, capped at 50%.
- Tier I Seals use Stage 5 materials.
- No work-based trials. Using a Seal grants its points; crafting the Seal is the trial.
- Ascendancy state stays with the chassis.
- Ascendancies are part of the 2.0 release.
- The ascendancy catalog is not datapack-overridable. Pack makers can disable Seal recipes and award Seals through loot or quests.
- The Forestry Companion gets ascendancies. Its entry gate is the installed Axe or Treefeller head stage.
- Deep notables must define the build. Tradeoffs are optional and can pay for more power.
- The system must support adding ascendancies later: data-driven catalog with no fixed count per family, a behavior registry, declared stats, and a test fixture ascendancy.
- Ascendancies may introduce new stats and behaviors, and are encouraged to. The launch set introduces 24 stats and extends two existing ones.

## Findings

- Only Crusher and Furnace recipes have a `bonus_output` opt-out. Today 31 recipes opt out: copper, iron, and tin ingot/dust conversions, blend smelting, and malformed-ingot nugget recovery. Slag Reclaim was changed to a speed and FE effect so it does not bypass the recovery opt-out.
- Blend routes are one-way. Crushed items come only from ore and raw inputs.
- An item cycle already exists on `main`:
    - Calibrated components stack, so Resonance Calibrator Super Output can duplicate them.
    - The Component Recycler returns one Iron Plate per Calibrated Structural Component, and the recycler can also roll Super Output.
    - This was confirmed by reading the code and recipes, not in game.
- The Potential Reactor pays FE for Refinement Potential and returns a stripped copy that the Component Recycler can reduce to the raw input. That cycle needs measuring.
- Furnace lanes stop warming at the recipe's required temperature (`FurnaceBlockEntity.warmLane`). Crucible Keeper therefore needs a new warm target.
- Every current Melter recipe is processing level 6, and no Furnace recipe targets 2200 °C or more. This shaped the dropped work-trial design and no longer affects Seals.
- `MachinePassiveClass` and its empty `ascendancies` list are used only by legacy tree definitions. `MachineMasteryFamily` is the live family source.
- Every Mastery screen, including `ForestryCartScreen`, uses `MasteryScreenSupport`, so one Ascendancy panel covers all families.
- The Forestry cart has no chassis stage, but its installed modular tool's head has a material stage (`ToolHeadMaterial.stage()`), reachable through `ForestryCartEntity.toolStack()`.
- Grove Warden's growth pulse turns FE into logs, and logs burn as fuel. The loop audit models it as an FE → log edge.

## Verification

- 2026-09-29 PRD revisions: `npm run repo:check` passed. `mkdocs build --strict` was not run because MkDocs is not installed locally; CI runs it.

## Handoff and limits

- The PRD is accepted. Its node numbers are placeholders until balance testing.
- The existing calibrate-then-recycle cycle is independent of ascendancies. It needs its own fix before the loop audit can become a CI gate.
