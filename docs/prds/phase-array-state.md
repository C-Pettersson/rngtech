# Phase Array Implementation State

Source: [draft design](phase-array.md).

Status: **Prototype**. The PRD was accepted on 2026-10-21 and every delivery phase merged to `main` in #205. The Exotic Affix Forge is replaced. Everything still needs an in-game playtest.

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Resolve the decisions and accept the PRD. The open questions stay open for the simulation and formation phases.
- [x] Create a feature branch from clean `main`.
- [x] Simulation: `./gradlew phaseArraySim`, `tools/balance/phase-array-scenarios.json`, and the PRD's numbers set from it.
- [x] Parts and formation: Emitter, Coil, Glyph, their profiles, the line pattern, formation, and the Cradle rename.
- [x] Operations with instant outcomes: the `rngtech:phase_array` recipe type, the Cradle screen, charge, and every operation through the refinement library.
- [x] The hold sequence: timed holds, sound, lights, and the stored outcome.
- [x] Glyph sources: default challenge table entries, Dormant Machines, the `/rngtech glyph give` command, and the plain-stack rule.
- [x] Removal of the Exotic Affix Forge operations, history, recipe type, menu, screen, JEI category, and config keys (phase 4), then its catalyst item, wiki page, guide pages, and quest text (phase 7).
- [x] Balance-world setup `rngbalance:phase_array`.
- [x] Documentation: wiki page, Rarity and Affixes, Machine Stats, Battery Cells, Modpack Creators, the implementation matrix, the GuideME guide, Machine Design Notes, and quests.
- [ ] In-game playtest of every operation and outcome.

## Decisions

Recorded 2026-10-21 while drafting:

- Default Glyph sources are the existing challenge loot tables and Dormant Machines. Work Orders add the main supply later, and this PRD does not wait for them. (Superseded 2026-10-26: the challenge tables were removed, so Dormant Machines are the only default source until Work Orders.)
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

- 2026-10-26: the machine-challenge Glyph sources are gone. Machines roll no loot on their own events, because the Glyph dropped on top of the machine or blocked its output. Dormant Machines are the only default source until Work Orders land. The wiki, the JEI Glyph page, and the pack-maker page say so.
- 2026-10-22: playtest retune. Resonate costs 2 RP instead of 1, and Imprint needs a Tier IV Glyph instead of Tier II, in the recipes, the `prd` scenario, and the PRD. `phaseArraySim` reran: a fresh Rare Stage 8 item now stops on RP for 91% of runs after 18 pulses at Quality 22, where it stopped on Strain after 25 pulses at Quality 25, so the PRD's model table and its reading of the limits are rewritten. The GameTest that loads Resonate checks 2 RP.
- 2026-10-22: Glyph seats. The Cradle has five Glyph seats, one per tier; a Glyph of that tier or higher opens every position of its tier, and taking it out closes them. Coils carry no tier of their own any more: `rngtech:coil_tier`, the lock-in click, and the Coil item's tier tooltip are gone, and a Coil's `tier` block state is a display the Cradle sets to the tier its position runs at. Shift-clicking a Glyph fills its own seat first, then the spend slot. The balance-world setup seats a Glyph of every tier, the GameTests seat Glyphs, and the wiki, guide, quests, and PRD describe seats.
- 2026-10-22: pack-maker levers. Hold Chance, Overhold Chance, the Partial window, Fracture per Strain, the Quality curve, the stage factors, the parts' charge store and rate, and the Glyph floors are `phaseArray.*` keys in `rngtech-common.toml`, read through `PhaseArrayRules` with the PRD's numbers as defaults when no config is loaded, so the domain checks and the simulation keep their values. The operations' costs were already recipes and RP already had `calibration.refinementPotentialBonus`; the Modpack Creators page lists the keys.
- 2026-10-21: the Alt tier view on an item names each affix's top tier ("Tier 4 of 4"), because some affixes, such as the flat Max Temperature one, stop below Tier 7 and read as raisable when they are finished.
- 2026-10-21: a Tier 7 affix selected for Resonate, and a Tier 6 one, read only as an illegal operation. The affix list now dims each affix the chosen operation cannot change and says why on hover (at the top tier, Tier 6 needs Exalt, Exalt wants Tier 6, Exotic only, fixed, untiered), and the status names the affix and its tier with the same reason; Jade gets the affix and tier.
- 2026-10-21: the result line ran under the donor, Glyph, and output slots. It now stops short of them with a short form, and its hover, like the chamber's, carries the full outcome text.
- 2026-10-21: every empty Cradle slot and cell socket explains what it takes on hover, and the blocked statuses name their numbers: the Glyph tier the operation wants, the Coils it needs against the usable count, and the RP it spends against the item's. The same numbers reach Jade.
- 2026-10-21: a full Magic item showed Imprint and Graft with nothing to select, because rarity sets the slot count and nothing but Reforge changes it. The Cradle now reports a dedicated no-empty-slot status, the list carries a red hint row that says why and points at Reforge, and the wiki says the same under Operations.
- 2026-10-21: Graft feedback and the guide. A Graft whose donor affix the target cannot take now reports a dedicated status, and the donor list dims each such affix with its reason: wrong slot type, not in the target's profile, or a group the target already carries; the legality rule itself is unchanged. The wiki page is rewritten as a manual, with a one-minute start, a positions table that names the operations each one opens, a worked cost table, a screen walkthrough, and tips; the in-game guide now keeps the operations table and opens with an interactive scene of a five-Coil line through a new guide-only prefix hook in `tools/rngwiki/guideme.py`.
- 2026-10-21: lock-in. Clicking a blank position in the Cradle's screen takes the Glyph that position needs from the player's inventory, the exact tier first and else the lowest tier above it, so the player never picks a tier; the right-click on a Coil and the Glyph-slot click are gone. A slip past the operation's Coil count shows amber, not red, during and after a pulse, because it did no harm. The test plan's gaps are closed: domain checks for the RP and Graft donor rules and the Fracture curve, GameTests for Attune keeping tiers, a broken Coil keeping the charge, Strain round-tripping, and a retuned recipe parsing, and moddex rules that no recipe names a Glyph and the part recipes sit behind Nullite. Removing an operation with an empty recipe is vanilla datapack behaviour and is not tested.
- 2026-10-21: audit items 7 and 8. The target item hangs in the Cradle's bore, turning slowly, through a block entity renderer fed by an update tag that carries the target. The Cradle's bottom face hands out drained Battery Cells and takes nothing in; the PRD's Non-Goal now says it rules out automating the pulse, not emptying the sockets, and the wiki's automation table has the row.
- 2026-10-21: audit items 5 and 6. Jade names the first missing block when no line forms and shows the chosen operation in every state, through a new free note line on `MachineInfoSnapshot`. JEI has information pages for the Emitter, the Coil, and the Glyph.
- 2026-10-21: audit against the PRD closed four gaps. Strain shows on item tooltips with the Fracture chance it adds. Primed reads a twelfth line stat, `PRIMED_CHARGE_RATE`, that the Cradle pays only while a Battery Cell is socketed, as the PRD's pool table says; the PRD's count of eleven stats is now twelve. Coil nodes show Second Hold, Overhold Chance, and affixes, and the Emitter node the line's totals and its affixes, with part traits synced to the client for that. The charge hover gives the time to full at the rate FE arrived last tick.
- 2026-10-21: art and interaction pass after the first in-game look. The three parts are shaped models from `tools/blockmodels/phase_array.py`: caged cubes with lit strips that follow the line's state, a glyph disc on each side of a Coil, a core in the Emitter that fills by quarters through a new `charge` block state, and a mirror-lined bore on the Cradle that faces the Coils through a new `facing` state the line sets on formation. The beam, charge sparks, and outcome burst are particles. Placement follows the neighbouring line axis instead of the look direction. A Glyph right-clicked on a placed Coil inscribes it, and the Cradle's screen names each outcome's effect. Two earlier passes, a receiver well with rising crystals and Needler-style needles on the Emitter, were rendered by a block entity renderer and rejected as looking detached from the block.
- 2026-10-21: drafted the PRD and this state page, and updated the Work Orders PRD to match. Accepted the same day.
- 2026-10-21: phase 9. The Machine Stats page lists the eleven line stats, Rarity and Affixes defines Strain and Quality, Battery Cells mentions the Cradle's sockets, and Modpack Creators explains `rngtech:phase_array` recipes and Glyph handling. The guide is regenerated.
- 2026-10-21: phases 7 and 8. The Exotic Affix Catalyst item, recipe, model, texture, and lang key are gone, and the Stage 7-8 quests describe the Phase Array. `wiki/exotic-affix-forge.md` is replaced by `wiki/phase-array.md`, with every link repointed and the guide regenerated. Inscribing has its in-game surface: a Glyph in the Cradle's Glyph slot plus a click on a Coil's node. `rngbalance:phase_array` spawns a nine-Coil line with every position inscribed, a Debug Battery on the Cradle, charged cells in its sockets, and Debug Chests of Glyphs and parts.
- 2026-10-21: phase 6. The six challenge tables carry their first default loot: conditioned Glyph pools per the PRD's table, with `heat_failure` empty. A woken Dormant Machine drops a Glyph by site through `GlyphSources`. `/rngtech glyph give <players> <tier>` exists. A plain `rngtech:glyph` stack already read as Tier I. `check-uniques.mjs` now requires every default challenge pool to drop only Glyphs behind a condition.
- 2026-10-21: phase 5. `PhasePulseSequence` turns a rolled pulse into frames: Coils engage from the Emitter every 8 ticks with a chime that climbs the line, the slipped Coil flashes for 12 ticks with a slip sound, lit Coils unwind back toward the Emitter every 5 ticks, a full hold stays lit 16 ticks, and a Fracture shows 20 ticks with no Coil engaging. The Cradle rolls the outcome at Fire, locks its slots, lights the Coils and the Emitter through their `active` block state (new lit models), and applies the outcome when the frames end. The screen mirrors the frames. A pulse in flight survives a save: it applies on the first tick after load without replaying.
- 2026-10-21: phase 4. `rngtech:phase_array` is a recipe type with one recipe per operation (`data/rngtech/recipe/phase_array/`), and the nine defaults carry the PRD's numbers. `PhaseCradleBlockEntity` replaces the Exotic Affix Forge's logic behind the same block id: it charges a pulse from supply and nine Battery Cell sockets at the line's Charge Rate, prices it with `PhaseArrayRules.pulseCost` and the line's Thrift, previews the odds with `PhasePulse.odds`, and fires through new `phase*` methods on `RefinementEngine` that spend no RP themselves. Strain lives in `rngtech:strain`. The old recipe type, action enum, history component, menu, screen, JEI category, and config multipliers are gone. `PhaseCradleScreen` is one panel in the Dormant Machine's style. The hold sequence is still instant; phase 5 adds the timed locks.
- 2026-10-21: phase 3. `rngtech:phase_emitter` and `rngtech:phase_coil` are trait-bearing Stage 8 machine blocks with twelve-affix profiles under a new `HAS_PHASE_ARRAY` capability, eleven new `MachineStat` entries, base stats from `PhaseArrayRules`, loot tables that keep traits and the Coil's `rngtech:coil_tier`, calibrated shaped recipes behind the Nullite condition, and a `phase_array` corruption pool. `rngtech:glyph` carries `rngtech:glyph_tier` and has no recipe. The Cradle (the renamed `rngtech:exotic_affix_forge`) scans for the line like the Dehumidifier, lights members, and counts usable Coils. `PhaseArrayChecks` and `PhaseArrayGameTests` cover pools, base stats, position floors, formation, unforming, and inscribing. Inscribing has no in-game surface until the Fire tab lands.
- 2026-10-21: added `phaseArraySim` and set the PRD's numbers from it: Resonate 1 RP, Fracture 1% per Strain, Quality divisor 3.5, Exalt 4 MFE. The first draft's 2 RP and 2% made RP the only limit a Rare item met and burned five Glyphs of Temper per item. Revised the same day: the form is a straight line instead of a ring, every stage is accepted, operations spend a little RP, operations are datapack recipes, Glyphs get default sources so the Array stands alone, part pools hold six affixes per slot type, the screen follows the Dormant Machine, and a balance-world setup is in scope.

## Verification

- 2026-10-21: art pass. `./gradlew ciCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `python tools/rngwiki/guideme.py --check`, and both `mkdocs build --strict` runs passed.
- 2026-10-21: documentation only. `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-21: `./gradlew spotlessApply phaseArraySim` ran clean, and `./gradlew quickCheck` passed with the new `masteryTest` class. `mkdocs build --strict` passed.
- 2026-10-21: phase 3. `./gradlew quickCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` passed.
- 2026-10-21: phase 4. `./gradlew quickCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, and both `mkdocs build --strict` runs passed.
- 2026-10-21: phases 7 to 9. `./gradlew quickCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `python tools/rngwiki/guideme.py --check`, and both `mkdocs build --strict` runs passed.
- 2026-10-21: phase 6. `./gradlew quickCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `python tools/rngwiki/guideme.py`, and both `mkdocs build --strict` runs passed.
- 2026-10-21: phase 5. `./gradlew quickCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, and both `mkdocs build --strict` runs passed.

## Handoff and limits

- The PRD's numbers are the `prd` scenario of `tools/balance/phase-array-scenarios.json`. The model has no Second Hold, Strain Tolerance, or part affixes in play; they are zero at base and only exist once parts roll.
- The sequence uses vanilla sounds (amethyst chime, beacon deactivate, respawn anchor charge, glass break) and vanilla particles. Custom sounds are open.
- The pulse crosses every usable Coil, so a long line running a short operation watches all its positions; slips past the operation's count still succeed and a full hold rolls Overhold. This is the PRD's ladder, and it was questioned in playtest.
- Formation differs from the first PRD text: it is automatic, not a right-click, so it matches the Dehumidifier. The PRD now says so.
- The model's players are simple: they always Resonate the lowest affix, and they stop at 25% Fracture. A player who Reforges a worn item, or Grafts between bodies, is not modelled.
- The line is drafted horizontal only. A vertical line is an open question.
- Tier V Glyphs come from Work Orders only. No interim source is added here, by decision on 2026-10-21.
- The Affix Forge 3D models branch's Exotic Affix Forge model should wait for the form decision.
