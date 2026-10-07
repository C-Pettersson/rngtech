# Unique Items Implementation State

Source: [draft design](uniques.md).

Status: **Planned** for the 2.0 release. The PRD was accepted on 2026-10-05. No implementation work has started.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Redesign the [Unique Item Ideas](../reference/unique-item-ideas.md) catalog against current stats, Gear slots, and ascendancies.
- [x] Resolve the PRD's open questions and accept it.
- [ ] Create a feature branch from clean `main`.
- [ ] Framework: catalog loader and validation with ranges, the stat-reader table, `ModifierSlot.UNIQUE` roll storage and clamping, the Unique identification branch, `UniquePartItem`, catalog-driven registration, the `rngtech:uniques` tag, catalog lookups in identity and base-stat catalogs, generalized Unique checks, the Potato cell migration, and domain checks.
- [ ] Sources: the `rngtech:unique_loot_enabled` condition, config keys, the `rngtech:unidentified_unique` loot function, per-Unique loot tables and modifiers, the six empty challenge loot tables with their event hooks and context conditions, and `/rngtech unique give`.
- [ ] Launch content: eight Uniques, the `ESCAPEMENT`, `REFLUX`, and `ECHO_STREAK` behaviors, textures, and language keys.
- [ ] UI: unidentified and identified tooltips, the Shift roll view, dimmed unread stats, JEI pages, and foil.
- [ ] Documentation: Rarity, Machine Parts, Battery Cells, Machine Stats, the implementation matrix, the ideas page, and quests.
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

## Findings

Recorded 2026-10-04 while redesigning the ideas page:

- Every Unique check outside `Rarity` is Battery-Cell-only: `RefinementTargets`, `CraftedTraitOutputs`, the JEI info list and forge-category filters, and `BatteryCellItem` foil and tooltip handling.
- `MachineImplicitCatalog` identities carry no modifiers, and `IMPLICIT` modifiers are not applied by `ComponentBaseStatCatalog.effectiveStats`. Unique stats must therefore live in base-stat profiles.
- Several old candidate drawbacks were display-only on their hosts. `STABILITY` is not read by the Solid Fuel Burner, Battery Chassis, or Component Recycler. `OUTPUT_AMOUNT` is read only by the Crusher and Cavitation Generator. This led to the load-time stat-reader check.
- Servos start at Stage 4 and Fluid Pumps at Stage 5, and the Metal Press has no pump slot. Upgrade outcomes (failed and fractured parts), Fuel Box blaze handling, lubricant handling, and calibration heat do not exist.
- The only loot modifier is the Potato cell's `neoforge:add_table`, and it has no config gate.

## Progress log

- 2026-10-04: drafted the PRD, this state page, and the redesigned ideas page.
- 2026-10-05: accepted the PRD with ranged rolls and no per-machine limit. Added roll storage, identification, clamping, and roll-quality UI, and gave every launch Unique draft ranges.
- 2026-10-06: added pack configuration: per-Unique config keys, six challenge loot tables fired by machine events, context loot conditions, and quest and command support.

## Verification

- 2026-10-04: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-05: documentation only, after the acceptance edits. `npm run repo:check` and `mkdocs build --strict` passed again.
- 2026-10-06: documentation only, after the source changes. `npm run repo:check` and `mkdocs build --strict` passed.

## Handoff and limits

- Launch ranges are draft targets against existing profiles and need tuning in playtest.
- The stat-reader table does not exist yet. The Findings above come from a manual survey of block-entity reads.
