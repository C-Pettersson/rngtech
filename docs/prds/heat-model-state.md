# Heat Model Implementation State

Source: [Heat Model PRD](heat-model.md).

Status: **Planned** for the 2.0 release. The PRD is a draft; no code has changed.

## Scope checklist

- [x] Survey current heat math on the Furnace, Alloy Furnace, Metal Press, and Melter.
- [x] Draft the PRD and this state page.
- [x] Quest path audit.
- [ ] Resolve the PRD's open questions.
- [ ] Slice 1: shared heat engine, new stats, Heat Mode, emergent FE, `HeatProgressionChecks`.
- [ ] Slice 2: Heat Core and chassis profiles, affix eligibility, stored-affix conversion.
- [ ] Slice 3: passive tree and ascendancy changes.
- [ ] Slice 4: UI, Jade, language, and canonical docs.
- [ ] In-game playtest.

## Decisions

Recorded 2026-10-06:

- Heat work is split from [Batch Processing](batch-processing.md) into this PRD.
- FE is fully emergent: machines pay for the heat they put in, including losses. A recipe's own heat is the floor on FE per craft.
- Scope covers every heated machine: Furnace, Alloy Furnace, Metal Press, and Melter. Generators keep their current heat stats.
- ~~Every heated machine gets a player-set Heat Mode: Eco, Balanced, or Hot.~~ Replaced on 2026-10-14 by the overheat slider.
- Recipes declare heat units (`heat`) instead of FE. `processing_ticks` stays as the reference pacing.
- Heat Isolation stays on heated machines as the efficiency stat. ~~Cooling Rate is retired there.~~ Reversed on 2026-10-14: Cooling Rate stays as active cooling.
- The progression bar is a quest-order run with Normal-rarity, unrolled gear and no Mastery points. Steps may get slower or faster but must never be blocked. Grinding XP on cheaper recipes is optional, never required.
- Some rolling is acceptable. The Steel Blend smelt (1100 °C) and Methane from Biomass (1400 °C) stay as roll-expected steps; the new model must not make them harder.

Recorded 2026-10-14:

- An overheat slider replaces Heat Mode. It runs from the recipe target (0%, the default) to the machine's Max Temperature (100%). Going past the safe maximum is open to everyone, not only Overdrive.
- Temperature Stability sets a visible wobble risk instead of silently clamping the setpoint. The wobble grows with how far the slider pushes past the target and shrinks with Stability and Thermal Mass. Dips below the minimum pause progress; spikes above the overheat temperature build strain on recipes that can fail.
- Cooling Rate stays on heated machines as active cooling. It applies only while a lane is above the heat it may work at (a cooler recipe or a lowered slider), separate from Heat Isolation's passive loss.
- Progress starts once the lane reaches the recipe minimum, slower until the target and faster above it.
- Every Stage 4+ Furnace metal smelt now has a failure output, and a Furnace lane or Metal Press left above a cooler recipe's overheat temperature cools before working. Both land ahead of the engine; active cooling generalizes the second rule.

## Progress log

- 2026-10-06: Surveyed current heat math and wrote the PRD and this state page.
- 2026-10-06: Audited heated-recipe quest steps. Found two steps blocked today (Steel Blend smelt, Methane) and an `invar_blend` quest item with no recipe.
- 2026-10-14: Replaced Heat Mode with the overheat slider, added wobble and active cooling, and moved the progress start to the recipe minimum.
