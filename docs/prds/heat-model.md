# PRD: Heat Model

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the machine content pages for current behavior.

PRD status: Draft

Implementation status: Planned

Release: 2.0

Last updated: 2026-10-06

## Summary

Heated machines have seven heat stats, but almost all of them are thresholds or one-time costs:

- **Max Temperature** only has to reach the recipe target. Heat above that does nothing, except through Crucible Keeper's Overdrive.
- **Heat Transfer** has no downside. It speeds up warmup and also divides processing time, while FE per craft stays the same.
- **Warmup Time, Cooling Rate, and Heat Isolation** only matter while a machine is cold or idle. A busy machine sits at its target, so they do nothing while it runs.
- **Temperature Stability** is a pass/fail gate for normal recipes and a strain source for Stage 4+ failure recipes.
- The **Melter** has no live temperature at all. Its heat is its Max Temperature.

While a furnace is running, its speed comes down to Processing Speed × Heat Transfer.

This PRD replaces that with a shared heat engine for the Furnace, Alloy Furnace, Metal Press, and Melter:

- Recipes declare the **heat units (HU)** they need instead of FE.
- Each lane has a live temperature that heat flows into and out of.
- FE is fully emergent: machines pay for the heat they put in, including what leaks away. A slow, well-insulated kiln is cheap per craft; a hot, fast blast furnace costs more.
- Every heat stat has an upside and a cost.
- A per-machine **Heat Mode** (Eco, Balanced, Hot) lets players choose how hard a machine runs.

Quest progression is a hard constraint. The new model may make steps slower or faster, but it must never block one. The [Progression safety](#progression-safety) rules below are requirements, and an automated check enforces them.

The work lands in four slices. Each slice passes every check on its own.

1. Shared heat engine, recipe heat units, stats, and Heat Mode on all four machines, plus the progression check.
2. Heat Core and chassis profiles, affix eligibility, and stored-affix migration.
3. Passive tree and ascendancy rework.
4. UI, Jade, documentation, and language.

Progress, decisions, and verification are tracked on [Heat Model State](heat-model-state.md).

## References

- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Stats](../reference/machine-stats.md)
- [Machine GUI Design](../reference/machine-gui-design.md)
- [Batch Processing PRD](batch-processing.md)
- [Furnace](../content/furnace.md), [Alloy Furnace](../content/alloy-furnace.md), [Metal Press](../content/metal-press.md), [Melter](../content/melter.md)
- [Component Stages](../reference/component-stages.md)

Key existing code:

- `src/main/java/com/rngtech/content/blockentity/HeatControl.java`
- `src/main/java/com/rngtech/rpg/MachineStatAccumulator.java` (`adjustedHeatProcessingTicks`)
- `src/main/java/com/rngtech/content/blockentity/FurnaceBlockEntity.java` (`warmLane`, `coolLane`, `payHeatTick`, `processingTicks`, `overdrive`, `updateFailureStrain`)
- `AlloyFurnaceBlockEntity.java`, `MetalPressBlockEntity.java`, `MelterBlockEntity.java` (`effectiveHeat`)
- `src/main/java/com/rngtech/content/energy/HeatCoreMaterial.java` and `ComponentBaseStatCatalog.heatCore`
- `src/main/java/com/rngtech/content/machine/FurnaceChassisMaterial.java`
- `tools/moddex/check-recipe-loops.mjs`

## Goals

- Every heat stat matters while a machine is running, and each one has a cost.
- Gear, Heat Mode, and passive choices produce distinct machines: a kiln that is slow but cheap per craft, a blast furnace that is fast but costs more FE, or a precise build that runs right at the edge.
- One heat engine for all four heated machines, with one set of stat meanings.
- Players choose how hard each machine runs.
- No quest, recipe, or stage gate becomes harder to reach than it is today.

## Non-goals

- Changing recipe temperatures or stability requirements.
- Generators. The Solid Fuel Burner and Cavitation Generator keep their current heat behavior and stats.
- Heat moving between neighboring blocks.
- New fuels.

## Stats

| Stat | Status | Meaning | Upside | Cost |
|---|---|---|---|---|
| `MAX_TEMPERATURE` | Kept | How hot the heater can drive the chamber | Unlocks hotter recipes. Heats faster when far below its maximum | Hotter chambers wobble more, so they need Stability |
| `HEAT_TRANSFER` | Meaning changes | How fast the heater pushes heat in | Fast warmup, fast recovery after load dips | Higher FE/t peaks, so Battery Cell rate and energy transfer matter |
| `THERMAL_CONDUCTIVITY` | New | How freely heat moves from the chamber into the input, and through the walls | Faster processing | More heat leaks through the walls, so FE/t rises. FE per craft stays about the same, because the craft also finishes sooner |
| `HEAT_ISOLATION` | Kept | How well the walls hold heat in | Less heat lost, so lower FE per craft: the kiln stat | Traps heat: the lane cools slowly when it must drop to a cooler recipe's safe band |
| `THERMAL_MASS` | Renamed from `WARMUP_TIME` | How much heat the chamber stores | Smaller load dips, smoother temperature, holds heat while idle | Slow first warmup and slow recovery |
| `TEMPERATURE_STABILITY` | Meaning changes | How precisely the controller holds temperature | Smaller wobble, so Balanced and Hot modes run closer to the safe maximum | Uses up a stat roll that could have been raw power |
| `OVERHEAT_TOLERANCE` | Kept | Extra room above the recipe's safe maximum before strain starts | Safer Hot mode and Overdrive | — |

- On heated machines, `COOLING_RATE` is retired; idle cooling comes from heat loss and Thermal Mass. Generators keep reading `COOLING_RATE` and `HEAT_ISOLATION` as they do today.
- `HEAT_TRANSFER` no longer divides processing time. Conductivity and temperature above the target now set processing speed.
- `ENERGY_USAGE` on heated machines becomes FE per heat unit, so it scales every HU the heater supplies.
- All base values are 1.0 except temperatures.

## Recipe heat

Heated recipes declare `heat`, the heat units (HU) the input must absorb, instead of `energy`.

- `processing_ticks` stays. It is the reference time: how long the recipe takes at its target temperature with base Processing Speed and Thermal Conductivity. Authors keep control of pacing separately from heat cost. Today's FE per tick varies about 12× across recipes in the same heat band, so deriving time from heat alone would change many recipe times.
- A one-time data migration converts `energy` to `heat` at a fixed FE-per-HU rate. Stage 5–8 FE multipliers and the ≥ 1750 °C 2× rule are folded into the authored `heat` values, so the runtime rule goes away.
- The recipe codec still reads a legacy `energy` field and converts it, so third-party datapacks keep loading.
- The same `heat` value covers electric and fuel furnaces. Stage 0 gets a real heat cost per recipe for the first time.
- `check-recipe-loops.mjs` reads `heat` × the FE-per-HU rate. Every craft pays at least its recipe heat, so that is a safe lower bound on FE.

## Heat engine

The engine runs every tick, for each lane. One shared class serves all four machines.

### Temperature

```text
setpoint     = from Heat Mode (see below), never above Max Temperature
holding heat = Thermal Conductivity × k_loss × ((T − ambient) / 1000)^p / Heat Isolation
approach     = Heat Transfer × k_heat × (0.5 + 0.5 × (Max Temperature − T) / (Max Temperature − ambient))
heater power = holding heat + approach, limited by available FE or fuel
T change     = (heater power − holding heat − heat absorbed by the input) / Thermal Mass
```

- The heater always pays for holding heat first, so heat loss costs FE but never lowers the temperature the lane can reach.
- Heat loss grows faster than linearly with temperature (`p` > 1, as radiant loss does in real furnaces). Running hotter is faster, but the extra loss costs more FE per craft than the speed saves. This is what makes Hot mode and blast builds expensive.
- The approach term is at least half its full value, even at Max Temperature. A lane can always reach a setpoint equal to its Max Temperature.
- A hotter heater (more Max Temperature headroom) approaches a low setpoint faster.
- While idle, the heater is off and the lane cools by `holding heat / Thermal Mass`. This replaces Cooling Rate and Heat Isolation. Hold the Fire keeps its rule.

### Load dip

When a cycle starts, the cold input absorbs heat:

```text
dip = k_dip × (target − ambient) × batch / Thermal Mass
```

- The dip never takes the lane below the recipe minimum. A lane that dips stays at or above the minimum and recovers through `approach`.
- A bigger [batch](batch-processing.md) means a bigger dip. That is the Furnace's physical cost of batching. Thermal Mass and Heat Transfer offset it.

### Processing speed

Progress is heat absorbed by the input:

```text
heat factor = 0                                              below the minimum
            = 0.5 + 0.5 × (T − minimum) / (target − minimum) from the minimum to the target
            = 1 + 2 × (T − target) / target                  above the target
absorbed HU = (recipe heat / processing_ticks) × Processing Speed × Thermal Conductivity × heat factor
progress    = absorbed HU so far / recipe heat
```

- A cycle still starts only once the lane first reaches the target. After that, a lane running between the minimum and the target is slower instead of stopped.
- Above the target, running hotter is faster. At the default safe maximum (target × 1.15), the heat factor is 1.3.
- When the minimum equals the target, which is the default, the middle band is empty.

### Energy

FE is fully emergent: machines pay for the heat they put in.

```text
FE per tick  = heater power × FE per HU × Energy Usage
FE per craft = (recipe heat + holding heat during the cycle + share of warmup and dips) × FE per HU × Energy Usage
```

- Every craft pays its recipe heat. Gear, mode, and passives change only what's paid on top.
- Calibration targets, compared with base gear in Balanced mode:

| Build | Setup | Speed | FE per craft |
|---|---|---|---|
| Base | Base gear, Balanced | 1× | 1×, of which about 40% is holding heat |
| Kiln | Eco mode, high Heat Isolation and Thermal Mass | about 0.6–0.7× | about 0.7× |
| Blast | Hot mode, high Thermal Conductivity and Heat Transfer | about 1.6× | about 1.5× |

- If FE intake is short, the heater throttles. The lane heats slower or drops toward the minimum, and progress slows. It never deadlocks while any FE arrives. Cell Rate Limited stays a status, not a stop.

### Stage 0 fuel furnace

- Burning fuel is the heater. Each burn tick supplies a fixed number of HU, scaled by Fuel Efficiency.
- Burn ticks are spent on HU actually delivered, so a hot, leaky furnace burns fuel faster and a kiln-style furnace stretches each coal.
- Coal must still smelt Iron and Copper. See the progression rules.

### Melter

The Melter gains a live temperature with the same engine. Its recipes have only a minimum temperature, which is used as the target. Its single crucible is one lane.

## Heat Mode

Every heated machine gets a three-way Heat Mode, stored on the machine and kept when it's broken and placed again. Lanes on a Lead Furnace share the mode.

| Mode | Setpoint | Feel |
|---|---|---|
| Eco | The recipe target | Lowest FE, base speed |
| Balanced (default) | Target + half the safe band | Moderate speed and FE |
| Hot | The safe maximum | Fastest, most FE |

```text
wobble margin = how far the temperature wobbles, from Temperature Stability and Thermal Mass
setpoint      = clamp(mode setpoint, target, safe maximum − wobble margin), never above Max Temperature
```

- Poor Stability pulls the Balanced and Hot setpoints down toward the target, so the controller itself never wobbles into the overheat band. Stability becomes speed.
- Machines placed before the change, and new machines, start in Balanced.
- Heat Mode is set in the UI. Automation does not change it.

## Progression safety

The bar: a player who follows the FTB quest pack in order, using the machines and parts the quests ask for at Normal rarity, with no affix rolls, no refinement, and no Mastery points, can complete every quest. Grinding machine XP on cheaper recipes is an optional way to go faster or overcome a hard step, never a requirement. Steps may become slower or faster.

These rules are requirements. Slice 1 adds a `masteryCheck` simulation, `HeatProgressionChecks`, that enforces them.

1. **No new gates.** A recipe starts if, and only if, it starts today: Max Temperature reaches the target, and any required Temperature Stability is met. These thresholds and every Heat Core's Max Temperature bonus do not change.
2. **Always reachable.** With any gear and mode, a lane reaches every setpoint up to its Max Temperature while FE or fuel arrives. Heat loss costs FE but never caps temperature. Recipes whose target equals the gear's Max Temperature therefore stay reachable.
3. **No power deadlocks.** Short FE slows heating and processing and never stops them for good. At base gear, holding heat at the highest target a stage can run uses at most half the chassis's base energy transfer.
4. **No new failure strain at base gear.** In Eco and Balanced, base gear on its own stage's failure-bearing recipes builds no more strain than today. Load dips stop at the minimum, so they never add underheat strain. Hot mode and Overdrive may add risk.
5. **Quest-order run.** For every quest that needs a heated recipe, the gear the quest line expects at that point (unrolled, no Mastery) completes the recipe in Balanced mode at base energy transfer, within 0.5×–2× of today's time and FE or fuel. The bounds only catch calibration mistakes; tedium inside them is acceptable.
    - **Roll-expected steps** are the exception. On these steps the quest line accepts that players roll or refine gear, or use Mastery. For each one, the check uses the smallest Max Temperature or Stability bonus that starts the recipe today, and requires that the same bonus still completes it.
    - Roll-expected steps today: the Steel Blend smelt and Methane from Biomass.
6. **Stage 0 bootstrap.** Coal in the Stage 0 furnace reaches 800 °C and smelts Iron and Copper.

`HeatProgressionChecks` simulates each case tick by tick with the shared heat engine. A calibration change that breaks a rule fails `quickCheck`. Quest files are local pack data (`config/ftbquests/`, not in git), so the check stores its own copy of the quest-path cases.

### Quest path audit

Audited 2026-10-06 against the quest chapters and recipe data. Unrolled heat ceilings: Furnace 600 + Heat Core bonus (Primitive 800); Alloy Furnace 300 + Heat Core; Metal Press 800 + Heat Core; Melter 0 + Heat Core, with Heat Cores capped at stage 6.

| Quest step | Recipe | Needs | Expected gear gives | Margin |
|---|---|---|---|---|
| First Alloy (bronze ingot) | `furnace/alloy_blends/bronze_blend` | 700 °C | Primitive furnace, 800 | +100 |
| Steel ingot | `alloy_furnace/steel_blend_bootstrap_from_dust_and_fuels`, then `furnace/alloy_blends/steel_blend` | 900 °C / 0.9, then 1100 °C | Bronze Alloy Furnace + Bronze core 900 / 0.95; Copper furnace + Copper core 1000 | 0, then −100: roll-expected |
| Pressed Circuits | `metal_press/basic_electric_circuit` | 1300 °C / 0.95 | Crude press + Bronze core 1400 / 0.96 | Stability +0.01 |
| Improved connection, Connector Modules | `metal_press/*_connector` | 900 °C / 1.0 | Steel press + Bronze core 1400 / 1.075 | +0.075 |
| Advanced Electric Circuit | `metal_press/advanced_electric_circuit` | 1300 °C / 1.05 | Steel press + Bronze core 1400 / 1.075 | Stability +0.025 |
| Melter and Lubricant | `melter/lubricant` | 1200 °C | Titanium core 1200 | 0 |
| Methane from Biomass | `melter/methane_from_algae` | 1400 °C | Titanium core 1200, the Melter's highest | −200: roll-expected |
| Elite Electric Circuit | `metal_press/elite_electric_circuit` | 1600 °C / 1.10 | Steel core 1600 / 1.22 | 0 |
| Ultimate Electric Circuit | `metal_press/ultimate_electric_circuit`, `metal_press/naquadah_gear` | 2000 °C / 1.15 and 1.05 | Titanium core 2000 | 0 |
| Nullite ingot | `alloy_furnace/nullite_from_ingots` | 1500 °C / 1.18 | Titanium Alloy Furnace + Titanium core 1500 / 1.36 | 0 |
| Tungstensteel ingot | `alloy_furnace/tungstensteel_from_ingots` | 1500 °C / 1.16 | same | 0 |
| Aethergold ingot | `alloy_furnace/aethergold_from_ingots` | 1400 °C / 1.12 | same | +100 |

- Zero-margin rows are fine under rules 1 and 2, and they become test cases.
- Two steps need more than unrolled gear today: the Steel Blend smelt and Methane from Biomass. Affix rolls, refinement, or Mastery get through. These are accepted as roll-expected steps (rule 5) and stay as they are. The new model must not make them harder.
- The `invar_blend` quest asks for an item with no recipe. It is not a heat issue, but it blocks the main line at Stage 5.
- Quest descriptions quote heat numbers. They need a review when this lands, though thresholds do not change.
- `docs/reference/component-stages.md` lists heat bands that differ from the code, including a Tungstensteel Heat Core that does not exist.

## Gear and chassis profiles

- Heat Cores keep their Max Temperature bonus. Everything else is authored again, by identity. For example:
    - Aluminum: high Conductivity, low Thermal Mass, the blast core;
    - Steel: high Thermal Mass and Heat Isolation, the kiln core;
    - Sparksteel and Titanium: high Heat Transfer;
    - Exotic: strong across the board.
- Every core is calibrated so the quest-order run (progression rule 5) passes.
- Chassis base profiles (`FurnaceChassisMaterial`, Alloy Furnace, Metal Press, and Melter chassis) get the same pass. Lead Furnace keeps its 0.5× Processing Speed identity.

## Affix migration

Stored affixes keep their physical meaning:

| Old stat on a heated-machine item | New stat |
|---|---|
| Warmup Time | Thermal Mass, same value |
| Heat Isolation | Unchanged |
| Cooling Rate increased or reduced by x% | Heat Isolation reduced or increased by x% |

- The stat codec accepts `warmup_time` as `THERMAL_MASS`.
- Cooling Rate rolls are converted when a heated-machine item or Heat Core is read. On generators they stay as they are.
- Converted rolls can change from a pure upside to a trade-off. Players can reroll them through refinement as usual.
- Affix eligibility for heated machines swaps Cooling Rate for Thermal Conductivity.

## Passive tree and ascendancies

| Node | Change |
|---|---|
| Overdrive Lanes (Crucible Keeper) | Hot mode is the new baseline for running above the target. Overdrive lets the setpoint go past the safe maximum, up to the overheat temperature (safe maximum × Overheat Tolerance) minus the wobble margin, and its Overdrive Cap and Speed keep raising the heat factor there. Overdrive Margin and Safe Overdrive keep their meaning. |
| Crucible Heart (Crucible Keeper) | +1 [Batch Size](batch-processing.md) while the lane runs at twice the recipe target or hotter. This replaces its own two-inputs rule. |
| Shared Hearth (Crucible Keeper) | Lanes share heat: each load dip is split across all active lanes. |
| Hold the Fire (Crucible Keeper) | Unchanged: a lane with smeltable input never cools. |
| Kiln Discipline | Its Warmup Time cost becomes more Thermal Mass. |
| Regulated Heat, Low Heat Specialist, Flash Annealing | Their Max Temperature caps also cap the setpoint. |
| Quench Protocol | Its Heat Transfer cost stays. Quench's strain halving keeps its rule. |
| Hot Stamping (Metal Press) | Unchanged; Hot mode makes it easier to trigger. |
| Heat Window (Drop Forge and others) | Applies to the safe band, so it also widens or narrows the Balanced and Hot setpoints. |
| Shared-tree Heat Transfer, Warmup, Cooling, and Isolation nodes | Mapped to the new stats as in the affix migration table. |

## UI

Follows [Machine GUI Design](../reference/machine-gui-design.md).

- The Process tab gets a Heat Mode button: a flame icon with three states. Hover text names the mode and its setpoint.
- The heat bar marks the minimum, target, setpoint, safe maximum, and overheat temperature.
- Hover text shows:
    - current temperature and setpoint;
    - the heat factor;
    - recipe heat in HU;
    - FE/t split into input heat and holding heat;
    - predicted FE per craft.
- The Stats tab shows the new stats with one-line explanations of their upside and cost.
- Jade shows the mode, temperature, and setpoint.

## Migration

- Lanes keep their current temperature on load.
- Machines start in Balanced.
- `warmup_time` decodes as `THERMAL_MASS`. Cooling Rate rolls on heated-machine items convert as described above.
- Heated recipes migrate from `energy` to `heat`; legacy `energy` still loads.
- Mastery allocations stay. No node IDs change.

## Slices

1. **Heat engine.** Shared engine, recipe `heat` with data migration and legacy `energy` support, `THERMAL_CONDUCTIVITY`, `THERMAL_MASS`, Heat Mode, emergent FE, and fuel heat. Move the Furnace, Alloy Furnace, Metal Press, and Melter onto it with today's gear values mapped across. Add `HeatProgressionChecks` with the quest path cases.
2. **Profiles and affixes.** Author Heat Core and chassis profiles, the affix eligibility swap, and stored-affix conversion.
3. **Passive tree.** The node changes above.
4. **Presentation.** Heat Mode button, heat bar markers, hover text, Jade, language, and the canonical docs: Machine Stats, Machine Guidelines, the four machine pages, Component Stages, and Current Implementation.

## Verification

- `HeatProgressionChecks` enforces every [progression safety](#progression-safety) rule.
- `masteryCheck` also covers:
    - the engine's formulas;
    - the reachability floor;
    - load dip clamping at the minimum;
    - Heat Mode setpoints with poor and good Stability;
    - stored-affix conversion.
- `quickCheck` after each slice; `ciCheck`, `npm run moddex:check` (including the recipe loop audit), `npm run repo:check`, and `mkdocs build --strict` before handoff.
- In-game checks:
    - Stage 0 coal smelting;
    - one quest step per stage chapter;
    - a Lead Furnace running four lanes in each mode;
    - a Stage 4+ failure recipe in Hot mode;
    - Overdrive past the safe maximum;
    - a world saved before the change loads with its machines and affixes intact.

## Open questions

- **Constants:** `k_loss`, `k_heat`, `k_dip`, `p`, and the FE-per-HU rate are set during slice 1 calibration, against progression rule 5 and the build targets.
- **Heat Mode on automated lines:** whether a redstone signal should be able to switch the mode. The current proposal is UI only.
- **Order with batching:** this PRD and [Batch Processing](batch-processing.md) both touch Furnace lanes. The current plan lands batching first.
