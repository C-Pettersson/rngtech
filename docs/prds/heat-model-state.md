# Heat Model Implementation State

Source: [Heat Model PRD](heat-model.md).

Status: **Prototype** for the 2.0 release. All four slices are implemented together with batching slices 3 and 4 and pass the automated checks. The in-game playtest decides whether the direction stays.

## Scope checklist

- [x] Survey current heat math on the Furnace, Alloy Furnace, Metal Press, and Melter.
- [x] Draft the PRD and this state page.
- [x] Quest path audit.
- [ ] Resolve the PRD's open questions. Constants have first values; the redstone slider question stays open.
- [x] Slice 1: shared heat engine, new stats, overheat slider, emergent FE, `HeatProgressionChecks`.
- [x] Slice 2: Heat Core and chassis profiles, affix eligibility. Stored affixes are not rewritten; see the decisions below.
- [x] Slice 3: passive tree and ascendancy changes.
- [x] Slice 4: UI, Jade, language, and canonical docs.
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

Recorded 2026-10-15, during implementation:

- First constants: wall loss `3 HU/t` at 1,000 °C over ambient with exponent 3, heater approach `30 HU/t` (later `10 HU/t`), `2 HU/°C` heat capacity, load dip `5%` of the target's rise per batched item, active cooling `8 °C/t`, wobble `0.5` of the target per unit of slider push and up to `0.25` from a Stability shortfall, an 80-tick wobble period, `1 FE` per HU, and `7 HU` per Stage 0 burn tick. Wobble and strain constants are placeholders for the playtest.
- The heater pays wall loss and input absorption before raising the chamber, so loss never caps the reachable temperature; the reachability rule becomes a power rule (holding heat at base gear stays under half the Battery Cell transfer).
- Legacy recipe `energy` converts at `0.6 HU` per FE, doubled for Furnace recipes at `1750+` heat, which absorbs the retired high-heat surcharge and its config keys. All shipped heated recipes now author `heat`.
- Cooling Rate is converted where it is read, not in stored items: on a heated machine every reduction counts as an increase of the same size. Old items still say "reduced", which the playtest should judge. `WARMUP_TIME` is renamed `THERMAL_MASS` and keeps the `warmup_time` id, so stored affixes, Uniques, and data decode unchanged.
- Heat Transfer no longer shortens crafts. Thermal Conductivity carries crafting speed and raises wall loss with it, so FE per craft stays about the same while FE/t rises.
- Shared Hearth splits each load dip across every Lead Furnace lane that is smelting. Crucible Heart is +1 Batch Size while a lane runs at twice the recipe target. Heat Economy pays 30% less heat before the cycle starts; Continuous Pour skips the load dip.
- The Melter has no separate target: its recipe minimum is the target, with a safe maximum of 1.15× the minimum.
- Playtest 2026-10-15: a recipe with no temperature requirement (Silica Gel recharge) flickered between Too Hot and Ready, because 20 °C was above its 0 °C envelope. Ungated recipes now work at any heat at base speed, ignore the slider, and use a lane's leftover heat first.
- Playtest 2026-10-15: overheating Iron Dust at 100% never failed, since only Stage 4+ smelts had failure outputs. Every metal smelt now has its malformed ingot as a failure output. Below a Stage 4 chassis only overheating builds strain, so the failure is always self-inflicted. To keep maxing the slider from being the default, it stops at the safe maximum until the Blast Draft keystone (no downside, 10 points from the Drive/Reserve start) or Overdrive unlocks it; recipes that cannot fail then stop at their overheat temperature. The slider spans whatever band the machine can reach.
- Playtest 2026-10-15: heating was too fast (3–5 s for unrolled gear). The heater approach drops from `30` to `10 HU/t`, so unrolled gear climbs about 100 °C a second: an Iron Furnace reaches 800 °C in about 10 s and the fastest cores take 2–4 s. `HeatProgressionChecks` locks this in.
- With the slower heater, a power-limited lane at its minimum went stop-and-go: the input drained the chamber below the minimum and the heater needed ticks to recover. Short of heat, the input now takes only what the walls leave and works that much slower, and heat stored above the setpoint counts as available. Quest steps are now timed as eight crafts from cold, so one warmup counts once.
- Cool Headed, on Blast Draft's road node, cools a lane to the recipe target before each new input. Silica Gel Bead recharging runs at 120 °C and burns to Ash when overheated; Ash is compostable.
- Playtest 2026-10-15: overheated Tin dust never failed. Each new cycle cleared the failure strain, and at a high heat factor a cycle ended long before strain could reach the threshold. Strain now carries across finished cycles on the Furnace, Alloy Furnace, and Metal Press, draining only through recovery in the safe band, and temperature strain is scaled by the heat factor so it counts per unit of work. GameTests cover an overheated Tin dust lane with and without Blast Draft.
- `HeatProgressionChecks` runs the 18 quest steps tick by tick on Normal, unrolled gear and holds time and FE per craft within 0.5–2× of the old model. Its per-step table prints with `-Drngtech.heatReport`.

## Progress log

- 2026-10-06: Surveyed current heat math and wrote the PRD and this state page.
- 2026-10-06: Audited heated-recipe quest steps. Found two steps blocked today (Steel Blend smelt, Methane) and an `invar_blend` quest item with no recipe.
- 2026-10-14: Replaced Heat Mode with the overheat slider, added wobble and active cooling, and moved the progress start to the recipe minimum.
- 2026-10-15, all slices:
    - `HeatEngine` and `HeatLane` run the Furnace lanes, Alloy Furnace, Metal Press, and Melter. Lanes save their temperature, work, and fuel debt; older Furnace saves load from their old temperatures.
    - 247 recipe JSONs and the material generator moved from `energy` to `heat`. JEI shows HU.
    - `THERMAL_CONDUCTIVITY` and `THERMAL_MASS` are on Heat Cores, Alloy Crucibles, and Servos, roll as affixes, and show on the Stats tabs.
    - The overheat slider sits on each Process tab. The heat bar marks the target, the setpoint, and the wobble band, and its hover splits FE into heating and wall loss. Jade shows the setpoint.
    - The heat tree: Heat Flow and Startup nodes reduce Thermal Mass, Thermal Inertia gives Heat Isolation and Cooling Rate, and Kiln Discipline and Thermal Reservoir trade Thermal Mass. Node IDs and positions are unchanged.
    - New wiki Heat guide; machine pages, Machine Stats, and the reference docs describe the model.
    - Not done: unit checks for wobble, load-dip clamping, and active cooling beyond what the quest path exercises.
