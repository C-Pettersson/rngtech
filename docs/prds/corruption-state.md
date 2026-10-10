# Corruption Implementation State

Source: [draft design](corruption.md).

Status: **Prototype** for the 2.0 release. The PRD was accepted on 2026-10-05. Phases 2 to 6 are in on `feature/corruption-prd-implementation-d6f800`, including the optional Jade, JEI, and filter work. Merged with the Unique catalog: Uniques accept the catalyst, Reforged rerolls their ranged lines, and Unique parts draw implicits from their part's pools. Everything still needs an in-game playtest.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Resolve the PRD's open questions and accept it.
- [x] Create a feature branch from clean `main`.
- [x] Storage: the `corruption` field, the `CORRUPTION` modifier slot, stat aggregation, constructors and normalizers, and codec round-trip checks.
- [x] Operation: `CORRUPT`, outcome tables, pools and their validation, rejection order, station gating, the Stabilization Crystal change, and Exotic Affix Forge rejection.
- [x] Item and sources: the Volatile Catalyst, `max_stability` on calibration requirements, the Malformed Ingot stage ingredient, Jam Debris and the Crusher jam drop, the Stage 5 catalyst recipe, and its config condition.
- [x] UI: tooltips, the Affix Forge outcome preview, Refinement tab hover text, Jade, JEI, and optional filter matching.
- [x] Documentation: Progression, Affix Forge, Exotic Affix Forge, Rarity, the implementation matrix, and quests.
- [x] Reforged on a Unique rerolls its ranged lines inside their catalog ranges.
- [ ] In-game playtest of every outcome on every station.

## Decisions

Recorded 2026-10-05 when the PRD was accepted:

- No outcome destroys an item or lowers its rarity.
- Corruption works on placed machines.
- The Stabilization Crystal removes Blighted and is consumed.
- Corrupt costs no Refinement Potential.
- No yield or Refinement Potential in 2.0 pools.
- The name Volatile Catalyst is kept.
- Outcomes are weighted. Corruption implicits have one fixed value, with no tiers or ranges.
- +1 Processing Level and +1 Batch Size are in the Blessed pools.
- Uniques use the same four outcomes. Reforged rerolls their ranged lines.
- A Corrupted item hides its Refinement Potential (decided 2026-10-05, after acceptance).

Recorded 2026-10-10:

- A fifth outcome, Warped, scales each affix by its own -15% to +15% past its tier range. All five outcomes weigh 20, and the range is set in `outcomes.json`.
- Blessed pools were widened with more entries per host than Blighted pools.

Recorded 2026-10-11:

- The Stabilization Crystal's vanilla recipe (obsidian, quartz, gold, diamond) was too cheap for an item that deletes Blighted. It is now a `rngtech:calibrated_shaped` recipe: a Stage 5 Calibrated Conductive Component with 85+ stability, two Stabilization Catalysts, two Aluminum Casings, and four Sparksteel Plates. That puts it at Stage 5 beside the catalyst, gated by precision where the catalyst is gated by detuning. 85+ is a draft; the Sparksteel Coil calibration rolls 60-95 before Gear bonuses.

Recorded 2026-10-06:

- The catalyst is gated by RNGTech challenges only: a Stage 5+ Calibrated Kinetic Component inside a 20–35 stability band, two Stage 5+ Malformed Ingots from press or Alloy Furnace failures, and two Jam Debris from under-level Crusher jams. It has no vanilla loot and no vanilla-only ingredient such as Echo Shards.

## Findings

Recorded 2026-10-04:

- `rngtech:stabilization_crystal` is a `RefinementModifierItem(CORRUPTION_WARD)`. It is accepted only with add and upgrade operations, is never consumed, and changes no outcome. Its one real use is as an ingredient in `void_catalyst_from_stabilization_crystal.json`.
- `MachineTraits` has no version field or migration. Backward compatibility relies on `optionalFieldOf`, which a `corruption` field can follow. The stream codec has four fields, and NeoForge composites allow six.
- `normalizedStored` strips every non-affix modifier, so a `CORRUPTION` slot must be exempted explicitly.
- Placed-machine Refinement tabs accept any `RefinementConsumableItem` with no upgrade gating, so a new operation reaches about 25 machine menus with no menu changes.
- The Exotic Affix Forge uses its own action enum and recipes, not `RefinementOperation`, so it needs a separate Corrupted check.

Recorded 2026-10-10 during implementation:

- Pools are keyed by eligibility profile id. Each pool file lists its `hosts`, so one file covers a family such as every placed generator, and `pool_index.json` lists the default files for the classpath load. The server reloads `data/*/corruption/` from datapacks and syncs the raw files to clients; invalid data keeps the previous catalog.
- The stat-reader table lives in `CorruptionHostReaders`: a host reads every stat and behavior its own affix pool can roll, plus listed extras its machine reads from host stats, such as Processing Level, Jam Chance, and Jam Recovery on Crush Heads, Blend Speed on Alloy Crucibles, calibration stats on calibration Gear, and Batch Size on the Melter, Metal Press, and Resonance Calibrator. The Uniques catalog can reuse it.
- The reader rule changed three draft entries: `THERMAL_BUFFER` is not read by any host, so Heat Cores got Temperature Stability instead; `CELL_LEAKAGE_DAMPING` is read from the Battery Chassis, not cells, so Battery Cells got 30% less Idle Loss; Control Board, Coil, and Stabilizer Matrix Blighted entries rely on the calibration extras. +Streak Floor was dropped: it does nothing without the Harmonist's Streak Cap, so no 2.0 entry depends on an ascendancy and no tooltip has to name one. Parts the PRD table did not list use shared Energy Generation or Stability pools. The Fluid Tank, tools, and companions have no pool, so they cannot be corrupted.
- Part implicits on stats the part carries merge like affixes, after them, and may move the hard-gate Processing Level. Implicits on stats the part does not carry apply to the host machine directly.
- Machine and chassis items are not item targets: the Affix Forge refuses the catalyst on them, and players corrupt the placed machine through its Refinement tab instead. The engine itself refuses hosts without a pool.
- No Stage 5 kinetic calibration existed, so `calibration_kinetic_sparksteel_gear` makes one with a 20-60 stability roll. It recycles into an Iron Gear like the other kinetic components.
- Jam Debris drops into the Crusher output slot only when the slot is empty or already holds debris. The loop audit counts one free debris per jam on a level 5+ recipe.
- The Advanced Item Filter's Identity button gained Corrupt and Intact modes.
- After the Uniques merge, `unique_<part>` profiles fall back to their part's pools, `RefinementEngine.apply` takes the target's `UniqueDefinition` so Reforged can reroll its lines, Warped leaves Unique lines alone, and the Unique tooltip shows the Corrupted line and implicit. Corruption keeps its own `CorruptionHostReaders` rather than the Unique stat-reader table, because pools are keyed by eligibility profile, not by host machine.
- Warped skips any affix with a yield or Refinement Potential effect, so it cannot push yield past the loop-audit bounds. One factor applies to every effect of an affix, so drawbacks scale with benefits. Reductions stop short of 100%. Results round like a normal roll, to whole percents for percent, more, and less affixes and to the original's decimal places for flat ones, so Warped rarely changes a +1 Batch Size or slot affix. The Corrupted tooltip line now names the outcome, so Warped and Reforged items are recognizable.
- Blessed pools now list 2 to 7 entries per host, with subset pools for Stability, Efficiency, Processing Speed, Fuel, and Energy Capacity where the host's affixes roll them.

## Progress log

- 2026-10-04: drafted the PRD and this state page.
- 2026-10-05: accepted the PRD. Unified the outcome table for Uniques, made corruption implicits fixed-value, and kept the stage-gate entries in the Blessed pools.
- 2026-10-06: replaced the Echo Shard recipe and vanilla loot with the challenge recipe.
- 2026-10-10: implemented storage, the `CORRUPT` operation with its catalog and pools, station gating, the Volatile Catalyst and Jam Debris items and recipe, `max_stability`, the Malformed Ingot ingredient, tooltips, the Affix Forge and Refinement tab previews, Jade, JEI, filter modes, domain and ModDex checks, wiki pages, the implementation matrix, design notes, and a quest.

## Verification

- 2026-10-04: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-05: documentation only, after the acceptance edits. `npm run repo:check` and `mkdocs build --strict` passed again.
- 2026-10-06: documentation only, after the source changes. `npm run repo:check` and `mkdocs build --strict` passed.
- 2026-10-10: `./gradlew ciCheck` (including `masteryCheck` with the new `CorruptionChecks`), `npm run moddex:check` (including the new Volatile Catalyst check and the loop audit with no allowlist entries), `npm run repo:check`, `mkdocs build --strict`, and `mkdocs build --strict -f mkdocs.wiki.yml` passed. No in-game check has been run.

## Handoff and limits

- Outcome weights and pool values are draft targets. Check in playtest that the +1 Processing Level and +1 Batch Size weights feel rare enough.
- The Stabilization Crystal's 85+ stability gate is a draft. Check in playtest that a well-built Stage 5 calibrator reaches it often enough.
- The 20–35 stability band is a draft. Check in playtest that a Stage 5 calibrator can be mistuned into it reliably, and that a well-built one overshoots it.
- Malformed Ingots carry a free-form material string. The stage ingredient looks it up with `MaterialCatalog.materialStage`, so an ingot with an unknown or missing material never counts.
- The in-game checklist in the PRD's Test Plan has not been run. Jam Debris, the debris-into-output rule, and a jam keeping its input are covered only by code review and the pure `JamDebris` check.
