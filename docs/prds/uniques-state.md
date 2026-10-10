# Unique Items Implementation State

Source: [draft design](uniques.md).

Status: **Prototype** for the 2.0 release. The PRD was accepted on 2026-10-05, and phases 2–6 landed on `feature/uniques-implementation-88f3c4` on 2026-10-10: the framework, sources, all eight launch Uniques, the UI, and documentation. They still need an in-game playtest.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Redesign the [Unique Item Ideas](../reference/unique-item-ideas.md) catalog against current stats, Gear slots, and ascendancies.
- [x] Resolve the PRD's open questions and accept it.
- [x] Create a feature branch from clean `main`.
- [x] Framework: catalog loader and validation with ranges, the stat-reader table, `ModifierSlot.UNIQUE` roll storage and clamping, the Unique identification branch, `UniquePartItem`, catalog-driven registration, the `rngtech:uniques` tag, catalog lookups in identity and base-stat catalogs, generalized Unique checks, the Potato cell migration, and domain checks.
- [x] Sources: the `rngtech:unique_loot_enabled` condition, config keys, the `rngtech:unidentified_unique` loot function, per-Unique loot tables and modifiers, the six empty challenge loot tables with their event hooks and context conditions, and `/rngtech unique give`.
- [x] Launch content: eight Uniques, the `ESCAPEMENT`, `REFLUX`, and `ECHO_STREAK` behaviors, textures, and language keys.
- [x] UI: unidentified and identified tooltips, the Shift roll view, dimmed unread stats, JEI pages, and foil.
- [x] Documentation: Rarity, Machine Parts, Battery Cells, Machine Stats, the implementation matrix, the ideas page, and quests.
- [ ] In-game playtest of every launch Unique.

## Decisions

Recorded 2026-10-05 when the PRD was accepted:

- Ranged stat lines that roll per copy, so copies can be well or poorly rolled. Integer stats roll whole numbers, for example +1 to +3 Batch Size.
- No limit on Uniques per machine.
- Uniques drop unidentified and roll on identification.

Recorded 2026-10-06:

- Vanilla sources are the default. Pack makers can switch each Unique to RNGTech challenges (challenge loot tables), quest rewards, or their own sources through config and datapacks.
- No yield on any Unique in 2.0.
- The catalog is a classpath resource, and loot stays datapack data.
- No Unique tool parts in 2.0.
- Uniques stay unrecyclable.

Recorded 2026-10-10 during implementation (also in the [PRD decisions](uniques.md#decisions)):

- Catalog lines carry a `role` (`identity`, `signature`, `hook`, `drawback`).
- New stats `CYCLE_JAM_CHANCE` and `ESCAPEMENT_SPEED` carry the Pick-Jaw's every-cycle jam and Escapement's first-cycle speed.
- Hook lines are gated on the host accumulator's recorded ascendancy.
- Unit fixes: Heat Window in percent, Blend Heat Reduction in °C, Overdrive Margin scaled to +(20–40) °C.
- The ominous vault source rolls once per vault, because loot modifiers cannot reach the nested rare pool.

## Findings

Recorded 2026-10-04 while redesigning the ideas page:

- Every Unique check outside `Rarity` is Battery-Cell-only: `RefinementTargets`, `CraftedTraitOutputs`, the JEI info list and forge-category filters, and `BatteryCellItem` foil and tooltip handling.
- `MachineImplicitCatalog` identities carry no modifiers, and `IMPLICIT` modifiers are not applied by `ComponentBaseStatCatalog.effectiveStats`. Unique stats must therefore live in base-stat profiles.
- Several old candidate drawbacks were display-only on their hosts. `STABILITY` is not read by the Solid Fuel Burner, Battery Chassis, or Component Recycler. `OUTPUT_AMOUNT` is read only by the Crusher and Cavitation Generator. This led to the load-time stat-reader check.
- Servos start at Stage 4 and Fluid Pumps at Stage 5, and the Metal Press has no pump slot. Upgrade outcomes (failed and fractured parts), Fuel Box blaze handling, lubricant handling, and calibration heat do not exist.
- The only loot modifier is the Potato cell's `neoforge:add_table`, and it has no config gate.

Recorded 2026-10-10 during implementation:

- Machines recognized Gear by concrete item class (`CrushHeadItem`, `SolidFuelBurnerPartItem`, and so on) and read stages from materials. `GearParts` now checks part type and stage, so a `UniquePartItem` fits wherever a normal part of its type and slot stage fits.
- The Crusher merged its Crush Head before applying the passive tree. It now applies the tree first, so hook lines see the chosen ascendancy; stat values do not depend on that order.
- Ascendancy stats such as Heat Window apply whether or not the ascendancy is chosen, so hook gating has to happen at merge time rather than rely on the stat.
- Battery Chassis stats did not include anything from installed cells. Non-cell stats on a cell profile, such as the Bastion's Burst lines, now merge into the chassis.
- The Stats tab listed only ascendancy-granted stats; it now also lists non-hook Unique stats a machine has.
- A Mastery level gain had no hook. `MachineMasteryHost.masteryLevelGained` now fires after XP raises the level.
- The `rngtech:challenge` loot table type needs a `LootContextParamSets` registry entry, which is private; it is added by reflection when loot types register.

## Progress log

- 2026-10-04: drafted the PRD, this state page, and the redesigned ideas page.
- 2026-10-05: accepted the PRD with ranged rolls and no per-machine limit. Added roll storage, identification, clamping, and roll-quality UI, and gave every launch Unique draft ranges.
- 2026-10-06: added pack configuration: per-Unique config keys, six challenge loot tables fired by machine events, context loot conditions, and quest and command support.
- 2026-10-10: implemented phases 2–6. The catalog (`data/rngtech/uniques/`), `UniqueCatalog` validation, `UniqueStatReaders`, `UniqueStatLine` rolls, and `ModifierSlot.UNIQUE` storage; `UniquePartItem` registration from the catalog and the Bastion `BatteryCellMaterial`; `GearParts` acceptance in every host; loot condition, function, config keys, default tables, and loot modifiers; challenge tables, context, conditions, and hooks in the Crusher, Furnace, Alloy Furnace, Metal Press, Resonance Calibrator, Vacuum Collapse Generator, Forestry Companion, and every Mastery host; `/rngtech unique give`; the three behaviors and two stats; textures; tooltips, host dimming, Stats tab rows, JEI pages, and Machine Gear guide entries; the wiki Uniques page and related pages; and two quests.

- 2026-10-10, later: balance review after the first in-game look. Fixed two Uniques whose Temperature Stability drawback hard-blocked their own recipes (Fortress Heater Element, Crying Crucible), dropped the Fortress to Sparksteel-class heat and added Heat Core Maximum Temperature to the load-time recipe-gate check, replaced the Pick-Jaw's base-ore-destroying Output Amount drawback with increased Energy Usage, and buffed the Escapement, Witch-Bottle Pump, Echo Board, and Igloo Thermostat. Unique names and tooltips now use Path of Exile's unique colour.

- 2026-10-10, later: pack Uniques. `config/rngtech/uniques/*.json` loads after the built-in catalog with the same validation, skipping bad files with a logged error; ids register under `rngtech` and clashes with RNGTech items are skipped; catalog files gained an optional format `version`. Added the pack-maker guide `extras/uniques/README.md`, covering sources, challenge tables, and new Uniques. A game-test server boot confirmed a pack file registers with its own config key and a clashing id is skipped without a crash.

## Verification

- 2026-10-04: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-05: documentation only, after the acceptance edits. `npm run repo:check` and `mkdocs build --strict` passed again.
- 2026-10-06: documentation only, after the source changes. `npm run repo:check` and `mkdocs build --strict` passed.
- 2026-10-10: `./gradlew quickCheck` passed, including the new `UniqueChecks` (catalog load, mutated fixtures, roll ranges and seeds, merge, hook gating, storage, clamping, refinement rejection, Potato migration, slot stages, Escapement, Echo Streak). `npm run moddex:check` passed with the new Unique source check and the loop audit at 0 allowlisted loops. `npm run repo:check`, `mkdocs build --strict`, and `mkdocs build --strict -f mkdocs.wiki.yml` passed. A temporary game test, run once on the game-test server and then removed, confirmed that every default Unique table loads and drops its Unique unidentified at about its drafted rate, identification rolls in range, both config keys stop default loot, the `rngtech:challenge` type registers, and the six challenge tables load empty. No client playtest was run.

## Handoff and limits

- Launch ranges, drop chances, and the Bastion cell's numbers are draft targets and need tuning in playtest.
- The in-game checklist in the PRD's test plan is still open: tooltips, host dimming, Stats tab rows, JEI pages, each Unique in each host, the witch drop, the command, and multiplayer sync.
- The stat-reader table is hand-maintained. Nothing yet checks it against `GearSlotCatalog` automatically, because that catalog needs registries.
- [Corruption](corruption.md) landed in #61: a Volatile Catalyst is the one operation a Unique accepts, its Reforged outcome rerolls the ranged lines, and Unique parts draw implicits from their part's pools.
- Escapement state is not saved, so a reload primes one fast cycle.
