# Machine Ascendancies Implementation State

Source: [draft design](machine-ascendancies.md).

Status: **Prototype** for the 2.0 release — PRD accepted 2026-09-29 on `feature/machine-ascendancies`. The framework, loop audit, Seals, and UI are in; no ascendancy content ships yet.

## Scope checklist

- [x] Create `feature/machine-ascendancies` from clean `main`.
- [x] Draft the PRD and this state page.
- [x] Resolve the PRD's open questions.
- [x] Framework: catalog index loader, shared node parser, stat declarations, behavior registry, `ascendancyEntryStage()`, state fields, load validation, `forFamily()` family check, effect pipeline, build codes, generic catalog validation, fixture ascendancies, domain checks.
- [x] Loop safety: `bonus_output` on Alloy Furnace and Metal Press recipes (calibration and recycling from PR #14; the Melter's arrives with its first yield effect), a shared `BonusOutputRecipe` check, the recipe loop audit with mutation self-tests, opt-outs for every loop found, and the CI gate on.
- [x] Seals: items, recipes, `rngtech:ascendancy_seal_recipes_enabled` condition and config key, entry gate, server actions.
- [x] UI: Ascendancy panel and choose dialog in `MasteryScreenSupport`, client sync, Bonus Summary section, Jade line, JEI.
- [ ] Crusher and Furnace content with their new stats, Stats tab rows shown only when granted, and shared mechanics, then an in-game playtest.
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

- At PRD time only Crusher and Furnace recipes had a `bonus_output` opt-out, on 31 recipes: copper, iron, and tin ingot/dust conversions, blend smelting, and malformed-ingot nugget recovery. Slag Reclaim was changed to a speed and FE effect so it does not bypass the recovery opt-out.
- Blend routes are one-way. Crushed items come only from ore and raw inputs.
- The calibrate-then-recycle item cycle found during planning was fixed in PR #14.
- Calibrated components are not refinement targets, so the Potential Reactor pays nothing for them and the suspected Refinement Potential to FE cycle does not exist. The audit models the general case: stripping any recyclable machine or part.
- Recycler Super Output was the main loop source. Recycling returns an item's own ingredients, so wherever the returns cover what the item cost, Super Output duplicated them.
- Furnace lanes stop warming at the recipe's required temperature (`FurnaceBlockEntity.warmLane`). Crucible Keeper therefore needs a new warm target.
- Every current Melter recipe is processing level 6, and no Furnace recipe targets 2200 °C or more. This shaped the dropped work-trial design and no longer affects Seals.
- `MachinePassiveClass` and its empty `ascendancies` list are used only by legacy tree definitions. `MachineMasteryFamily` is the live family source.
- Every Mastery screen, including `ForestryCartScreen`, uses `MasteryScreenSupport`, so one Ascendancy panel covers all families.
- The Forestry cart has no chassis stage, but its installed modular tool's head has a material stage (`ToolHeadMaterial.stage()`), reachable through `ForestryCartEntity.toolStack()`.
- Grove Warden's growth pulse turns FE into logs, and logs burn as fuel. The loop audit models it as an FE → log edge.

## Progress log

- 2026-09-29, Phase 2 framework (no content):
    - `AscendancyCatalog` loads `data/rngtech/mastery/ascendancies/index.json` (empty until content lands) and validates every entry's shape and family support on load.
    - `MasteryNodeJson` is the shared node parser for both catalogs. `MasteryEffectSource` lets `MegaPassiveTree.applyStats`, `has`, `passive`, and `acceptsHardness` include the chosen ascendancy's root and allocated nodes, filtered to the owning family.
    - `MasteryDeclarations` reads `data/rngtech/mastery/declarations.json` (empty for now). `MachineMasteryFamily.supports` and `supportsBehavior` fall back to it, so new stats and behaviors need no switch changes.
    - `MachineProgressionState` gained `ascendancy`, `ascendancy_nodes`, and `seal_tiers`. Load validation drops retired nodes, prunes disconnected ones, and refunds over-budget allocations. `forFamily()` drops a foreign ascendancy and keeps earned tiers.
    - Domain operations: `withSealTier`, `withAscendancyChoice`, `withSwitchedAscendancy`, `withAscendancyNode`, and `withoutAscendancyNode`.
    - Every host implements `ascendancyEntryStage()`:
        - chassis stage for the Crusher, Furnace, Alloy Furnace, and Resonance Calibrator;
        - 3 or 4 for the Crude or Steel Metal Press, and 6 for the Melter;
        - the installed tool head stage for the Forestry cart.
    - Build codes carry the ascendancy and its order. Paste allocates onto a matching, empty ascendancy as far as points and Gear allow. Unknown ascendancies or invalid orders are dropped, so older codes still paste.
    - Test-only fixtures live in `src/masteryTest/resources/data/rngtech/mastery/ascendancies/fixtures/`: three Crusher ascendancies, one Furnace ascendancy, and a fixture declaration of `LUCK` and `FIXTURE_ECHO` for the Crusher.
- 2026-09-29, Phase 3 loop safety:
    - Merged `main` with PR #14, which fixed the calibrate-then-recycle loop and added a calibration-only loop check.
    - Replaced that check with a general recipe loop audit, `tools/moddex/check-recipe-loops.mjs`, run by `npm run moddex:check`. It solves a linear program over every recipe type, vanilla tags and wood/chest/furnace crafting, bucket conversions, and Potential Reactor stripping.
    - An earlier per-edge version ignored co-inputs. It reported more than 1,600 false cycles, such as redstone turning into 16 cables, so it was dropped in favor of whole-recipe semantics.
    - Every loop the audit found ran through bonus output on one of 21 recipes: 19 Component Recycling recipes and 2 Furnace silica gel recharge recipes. They now set `bonus_output: false`. It found no base-recipe or FE loop.
    - Calibration catalysts and stabilizers are free inputs, so the calibrate-then-recycle pattern stays a loop even though it consumes lapis.
    - Alloy Furnace and Metal Press recipes gained `bonus_output`, and JEI marks opted-out recipes. Every machine now applies Super Output and salvage through `ProcessingChance` with the recipe, instead of per-machine checks.
- 2026-09-30, Phase 4 Seals:
    - Registered `rngtech:ascendancy_seal_1` to `_3` (`AscendancySealItem`, stack of 16, uncommon, rare, and epic) and the `rngtech:primed_seal_core` and `rngtech:lubricated_seal_core` intermediates. All have placeholder textures and creative tab entries.
    - Six recipes, all gated by `rngtech:ascendancy_seal_recipes_enabled` and material conditions: two Component Assembler Seal Core recipes, two Seal I routes, and one each for Seals II and III.
    - The `ascendancy.sealRecipesEnabled` common config key backs the condition and defaults to on.
    - Server actions in `MasteryOperations`: `ascend`, `choose_ascendancy`, `switch_ascendancy`, `allocate_ascendancy`, and `refund_ascendancy`.
    - Validation happens before payment, so a rejected action costs nothing. A Seal is taken from the player's inventory; creative players must carry it but keep it. A refund costs five Mastery Refunds (`AscendancyCatalog.REFUNDS_PER_NODE`).
    - `tools/moddex/check-ascendancy-seals.mjs` fails `moddex:check` when a Seal or Seal Core recipe lacks the condition.
- 2026-09-30, Phase 5 UI:
    - Mastery menus sync the chosen ascendancy (catalog index), its nodes in allocation order, Seal tiers, and the entry stage through the shared `MasteryMenuSupport` fields. The client snapshot now carries the ascendancy, so the Mastery screen's Copy button includes it.
    - `AscendancyPanel` lives in `MasteryScreenSupport`, so all seven Mastery screens get it, including the Forestry cart:
        - A crest beside the start node opens it. The crest shows earned tiers as pips and is highlighted when a Seal can be used or points are unspent.
        - The tree view shows the chosen ascendancy with points and tier pips. Nodes allocate on click and refund on right-click; deep notables are diamonds.
        - Its Ascend button shows the next Seal, and its Switch button opens the choose dialog.
        - The choose dialog previews every ascendancy for the family in paged columns with read-only trees and root effects, then confirms the selection. It serves Seal I, a free choice after retirement, and switching.
        - Hover text explains every button, including why Ascend is unavailable.
    - `AscendStatus` computes the Ascend state in the server's order; domain checks confirm the two agree.
    - `AscendancyTreeLayout` fits an authored grid into any box. The catalog now rejects two nodes on one grid position, so nodes never overlap.
    - Node tooltips for both trees share one effect-line helper.
    - `MasteryBonusSummary` combines ascendancy effects with shared-tree effects and names ascendancy nodes as sources. The drawer adds an Ascendancy section with the chosen ascendancy and its allocated notables.
    - Jade shows the ascendancy and its points while sneaking. JEI has an information entry for the three Seals.
    - Deferred to Phase 6: Stats tab rows for ascendancy stats, because no declared stat exists yet.
    - The panel is hidden until a family has ascendancies, so it first appears in normal play with Phase 6 content.

## Verification

- 2026-09-29 PRD revisions: `npm run repo:check` passed. `mkdocs build --strict` was not run because MkDocs is not installed locally; CI runs it.
- 2026-09-29 Phase 2: `gradlew spotlessApply` and `quickCheck` passed with 837 domain checks (753 on the previous commit). `npm run moddex:check` and `npm run repo:check` passed. No in-game check: Phase 2 adds no player-facing surface.
- 2026-09-29 Phase 3: `gradlew spotlessApply` and `quickCheck` passed with 837 domain checks. `npm run moddex:check` passed, including the recipe loop audit (982 recipes, no item or FE loop, no allowlist entries) and both mutation self-tests. `npm run repo:check` passed. No in-game check yet: the gameplay changes are Super Output no longer applying to 21 opted-out recipes, and JEI marking them.
- 2026-09-30 Phase 4: `gradlew spotlessApply` and `quickCheck` passed with 853 domain checks, including the ascendancy action checks with a fake payment. `npm run moddex:check` passed: the loop audit covers 988 recipes, and the new Seal recipe check covers 6. `npm run repo:check` passed. `runGameTestServer` completed mod loading with the new items, config key, and condition registered, then exited before datapack loading because no game tests are registered. Recipe parsing and Seal use still need an in-game check.
- 2026-09-30 Phase 5: `gradlew spotlessApply` and `quickCheck` passed with 1058 domain checks, including menu sync, Ascend status agreement with the server, the Bonus Summary, and panel layout for every catalog entry. `npm run moddex:check` and `npm run repo:check` passed. Not yet run in game: the panel has no shipped ascendancy to show until Phase 6, so rendering, clicks, Jade, and JEI need a client check then.

## Handoff and limits

- The PRD is accepted. Its node numbers are placeholders until balance testing.
- The loop audit is a CI gate. New recipes that loop fail `npm run moddex:check` with a minimal loop and a likely fix.
