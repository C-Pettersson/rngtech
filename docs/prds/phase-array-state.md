# Phase Array Implementation State

Source: [draft design](phase-array.md).

Status: **Planned**. The PRD was accepted on 2026-10-21 with no release target. No code exists. The Exotic Affix Forge it replaces is a shipped prototype.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Resolve the decisions and accept the PRD. The open questions stay open for the simulation and formation phases.
- [ ] Create a feature branch from clean `main`.
- [x] Simulation: `./gradlew phaseArraySim`, `tools/balance/phase-array-scenarios.json`, and the PRD's numbers set from it.
- [ ] Parts and formation: Emitter, Coil, Glyph, their profiles, the line pattern, formation, and the Cradle rename.
- [ ] Operations with instant outcomes: the `rngtech:phase_array` recipe type, the Fire tab, charge, and every operation through the refinement library.
- [ ] The hold sequence: timed holds, sound, lights, and the stored outcome.
- [ ] Glyph sources: default challenge table entries, Dormant Machines, the `/rngtech glyph give` command, and the plain-stack rule.
- [ ] Removal of the Exotic Affix Forge operations, history, cooling, cells, Coils, and forge affixes.
- [ ] Balance-world setup `rngbalance:phase_array`.
- [ ] Documentation: wiki page, Rarity and Affixes, Modpack Creators, the implementation matrix, the GuideME guide, Machine Design Notes, and quests.
- [ ] In-game playtest of every operation and outcome.

## Decisions

Recorded 2026-10-21 while drafting:

- Default Glyph sources are the existing challenge loot tables and Dormant Machines. Work Orders add the main supply later, and this PRD does not wait for them.
- Tier V Glyphs have no source until Work Orders. No interim source is added.
- Every stage is accepted, and operations spend a little RP, so RP stays the ceiling.
- Operations are `rngtech:phase_array` recipes.
- The Cradle screen follows the Dormant Machine's graphical panel, not the processing-machine tabs.

Recorded 2026-10-21 when the PRD was accepted:

- The Array replaces the Exotic Affix Forge.
- The Cradle keeps the `rngtech:exotic_affix_forge` id with a new display name.
- The form is the straight line.
- Reforge keeps Strain.

## Findings

Recorded 2026-10-21 from the code on `main` at `c7757d96` and the open branches:

- Endgame generators make about 1 to 3 kFE/t each, and the Exotic Affix Forge's operations cost 6 to 16 MFE, so an operation is a 30 to 90 minute wait on one generator. That gap drove the cooling, spool, cell, and Coil work on `feature/exotic-affix-forge-fe-t-e77e75`, which is uncommitted and not merged.
- `ExoticAffixForgeHistory` stores the per-item operation counts the Array drops. Strain takes its place on the item.
- `WoodenDehumidifierBlock` is the only shaped multiblock, a hollow 3x3x3 frame formed by right-click. The line can follow its formation pattern.
- The Volatile Catalyst outcome table in `AffixForgeScreen` is the closest existing UI to the pulse preview.
- The Exotic Affix Forge already has a recipe type, `rngtech:exotic_affix_forge`, with one recipe per operation. The `rngtech:phase_array` type follows that shape.
- All six `rngtech:challenge` loot tables ship empty, with number conditions for stage, stability, instability, and Mastery level already in place. Glyph entries need no new condition.
- The Dormant Machine screen is one graphical panel with cell sockets, a filling conduit, and a chamber whose marks light as it wakes. The Cradle screen follows it.
- `tools/balance-world/build.py` builds setups from saved NBT and sided handlers, one function per setup. A Phase Array setup needs the formed line's NBT, so it comes after formation works.
- The Work Orders PRD lives on `feature/endgame-ideas-cb3bae` at `040c3b28` and is not merged. Its reward table and rejection rule are updated here to name Glyphs and the Array.

## Progress log

- 2026-10-21: drafted the PRD and this state page, and updated the Work Orders PRD to match. Accepted the same day.
- 2026-10-21: added `phaseArraySim` and set the PRD's numbers from it: Resonate 1 RP, Fracture 1% per Strain, Quality divisor 3.5, Exalt 4 MFE. The first draft's 2 RP and 2% made RP the only limit a Rare item met and burned five Glyphs of Temper per item. Revised the same day: the form is a straight line instead of a ring, every stage is accepted, operations spend a little RP, operations are datapack recipes, Glyphs get default sources so the Array stands alone, part pools hold six affixes per slot type, the screen follows the Dormant Machine, and a balance-world setup is in scope.

## Verification

- 2026-10-21: documentation only. `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-21: `./gradlew spotlessApply phaseArraySim` ran clean, and `./gradlew quickCheck` passed with the new `masteryTest` class. `mkdocs build --strict` passed.

## Handoff and limits

- The PRD's numbers are the `prd` scenario of `tools/balance/phase-array-scenarios.json`. The model has no Second Hold, Strain Tolerance, or part affixes in play; they are zero at base and only exist once parts roll.
- The model's players are simple: they always Resonate the lowest affix, and they stop at 25% Fracture. A player who Reforges a worn item, or Grafts between bodies, is not modelled.
- The line is drafted horizontal only. A vertical line is an open question.
- Tier V Glyphs come from Work Orders only. No interim source is added here, by decision on 2026-10-21.
- The Affix Forge 3D models branch's Exotic Affix Forge model should wait for the form decision.
