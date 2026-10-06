# Corruption Implementation State

Source: [draft design](corruption.md).

Status: **Planned** for the 2.0 release. The PRD was accepted on 2026-10-05. No implementation work has started.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Resolve the PRD's open questions and accept it.
- [ ] Create a feature branch from clean `main`.
- [ ] Storage: the `corruption` field, the `CORRUPTION` modifier slot, stat aggregation, constructors and normalizers, and codec round-trip checks.
- [ ] Operation: `CORRUPT`, outcome tables, pools and their validation, rejection order, station gating, the Stabilization Crystal change, and Exotic Affix Forge rejection.
- [ ] Item and sources: the Volatile Catalyst, `max_stability` on calibration requirements, the Malformed Ingot stage ingredient, Jam Debris and the Crusher jam drop, the Stage 5 catalyst recipe, and its config condition.
- [ ] UI: tooltips, the Affix Forge outcome preview, Refinement tab hover text, Jade, JEI, and optional filter matching.
- [ ] Documentation: Progression, Affix Forge, Exotic Affix Forge, Rarity, the implementation matrix, and quests.
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

Recorded 2026-10-06:

- The catalyst is gated by RNGTech challenges only: a Stage 5+ Calibrated Kinetic Component inside a 20–35 stability band, two Stage 5+ Malformed Ingots from press or Alloy Furnace failures, and two Jam Debris from under-level Crusher jams. It has no vanilla loot and no vanilla-only ingredient such as Echo Shards.

## Findings

Recorded 2026-10-04:

- `rngtech:stabilization_crystal` is a `RefinementModifierItem(CORRUPTION_WARD)`. It is accepted only with add and upgrade operations, is never consumed, and changes no outcome. Its one real use is as an ingredient in `void_catalyst_from_stabilization_crystal.json`.
- `MachineTraits` has no version field or migration. Backward compatibility relies on `optionalFieldOf`, which a `corruption` field can follow. The stream codec has four fields, and NeoForge composites allow six.
- `normalizedStored` strips every non-affix modifier, so a `CORRUPTION` slot must be exempted explicitly.
- Placed-machine Refinement tabs accept any `RefinementConsumableItem` with no upgrade gating, so a new operation reaches about 25 machine menus with no menu changes.
- The Exotic Affix Forge uses its own action enum and recipes, not `RefinementOperation`, so it needs a separate Corrupted check.

## Progress log

- 2026-10-04: drafted the PRD and this state page.
- 2026-10-05: accepted the PRD. Unified the outcome table for Uniques, made corruption implicits fixed-value, and kept the stage-gate entries in the Blessed pools.
- 2026-10-06: replaced the Echo Shard recipe and vanilla loot with the challenge recipe.

## Verification

- 2026-10-04: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-05: documentation only, after the acceptance edits. `npm run repo:check` and `mkdocs build --strict` passed again.
- 2026-10-06: documentation only, after the source changes. `npm run repo:check` and `mkdocs build --strict` passed.

## Handoff and limits

- Outcome weights and pool values are draft targets.
- The 20–35 stability band is a draft. Check in playtest that a Stage 5 calibrator can be mistuned into it reliably, and that a well-built one overshoots it.
- Malformed Ingots carry a free-form material string. The stage ingredient needs a material-to-stage lookup from the material catalog.
