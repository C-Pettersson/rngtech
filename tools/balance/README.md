# Balance Tools

## Energy generation simulation

`./gradlew energyBalanceSim` crafts every legal generator setup many times with rolled machine and part traits, then runs each generator's FE formula. Use it to check per-stage generation ranges against the power budget in [Component Stages](../../docs/reference/component-stages.md), and to preview balance changes before editing the game.

```sh
./gradlew energyBalanceSim
./gradlew energyBalanceSim -PbalanceTrials=5000 -PbalanceBehaviorTrials=2000
```

A full run at the defaults (1,000 fresh crafts and 500 crafts per other behavior, per setup and scenario) takes about 15 minutes.

### Player behaviors

- `fresh`: craft each machine and part once and install what rolled.
- `best_of_5`: craft five of each machine and part, keeping the copy that adds the most output on its own.
- `refined`: craft once, then spend all Refinement Potential through `RefinementEngine`: add affixes, catalyse Magic to Rare, then upgrade, each with the first usable lens (Power, Efficiency, Kinetic).

### Scenarios

`energy-scenarios.json` holds what-if scenarios. `current` must stay empty: it is the game as coded. Add another scenario to preview a change before editing the game. Supported keys:

- `solid_fuel_fe_per_burn_tick`: FE per effective burn tick for Solid Fuel Burner fuel.
- `recipe_energy`: recipe energy overrides keyed `<recipe folder>/<recipe id>`, e.g. `gas_combustion/syngas_power`.
- `solar`: `extender_range_bonus`, `extender_generation`, `controller_flat_once`, `max_range`, `panel_clear_generation`.
- `corrosion_recipes`: a full replacement Corrosion Cell recipe list with chain costs.
- `reactor_fatigue`, `reactor_gear_stage_scale`, `reactor_mastery`: Potential Reactor gear-fuel rules.
- `cavitation_overflow`, `vacuum_collapse_overflow`: `vent` (the game's behavior) or `stall`.

Nothing in a scenario changes the game.

### Output

The task writes `build/energy-balance/energy-balance.json`. For each scenario and behavior it contains:

- `setups`: one row per chassis and part combination, with the unrolled baseline and distributions for running FE/t, 24-hour average FE/t, FE per consumed input, FE/t net of the fuel chain, and generator-specific metrics such as Aethergold output kept per connector tier.
- `pools`: setups grouped by generator and highest component stage, with the affixes most over-represented in the top 5% of crafts.
- `ladder`: the best unrolled sustained setup per stage, and how often a craft of it beats the next stage's best unrolled setup.

Each scenario also has `gearFuel`: FE per item when crafted gear is burned in a Potential Reactor.

### Keeping it accurate

Rolls use `MachineTraitRoller`, part stats merge through `ComponentBaseStatCatalog`, and refinement uses `RefinementEngine`. Each generator's final FE formula is mirrored in `src/masteryTest/java/com/rngtech/rpg/EnergyBalanceSimulation.java` and names the block entity method it follows, so update it when that method changes. Recipe energy and ticks are read from the recipe JSON.

`energy-chain-costs.json` lists the FE that base-stat machines spend producing each generator input, plus which inputs can be obtained and from which stage. Recheck it when a fuel chain recipe changes.

## Ore line model

`./gradlew processingBalanceSim` models the ore line, Crusher then Furnace, for each stage, Mastery level, and build archetype. It is deterministic: Output Amount, Super Output, salvage, and Instant Process are expected values per input, not rolls.

```sh
./gradlew processingBalanceSim
```

### Setups

`processing-setups.json` defines the model:

- `levels`: the Mastery levels to model. Each level `L` spends `L - 1` points.
- `stages`: one row per stage. Each row names the stage's metal, the stage-matched Crusher chassis, Crush Head, Battery Cell, Furnace chassis and Heat Core, the Universal Connector, the stage's primary generator FE/t for comparison, and the Ascendancy Seal tiers assumed at that stage. `furnaceVariants` adds sidegrade Furnaces, such as the Lead Furnace or an Aluminum Heat Core with a flat Max Temperature roll.
- `archetypes`: one build per archetype. Each machine lists its chassis and part affixes (`prefix:<id>` or `suffix:<id>`), a `tree` key, and optionally an ascendancy with nodes in allocation order. Seal tiers decide how many ascendancy nodes apply.
- `trees`: shared Mastery tree allocations, by level, in a valid allocation order. They were chosen by a search over `machine_tree.json` that maximizes the archetype's stat, with recipe-blocking keystones (temperature and hardness caps, Single Pass, Cell Bypass) excluded.

Every listed affix is a perfect crafted Magic roll: the highest tier refinement upgrades reach (`6`, or the family's top tier), at its maximum value. The task rejects affixes that a profile cannot roll or that share a mod group.

### Output

The task writes `build/processing-balance/processing-balance.json`. For each stage, archetype, and level it reports:

- `crusher` and `furnace`: the effective stats and the Mastery build that applied.
- `crushOre`, `crushDust`, `smeltDust`, `smeltCrushed`, `smeltOre`: one row per machine step with output per input, FE per input and per output, ticks per input, running FE/t, batch or lane count, and whether the stage's power supply limits it.
- `routes`: ore to ingot through crushed and dust, through crushed only, or smelted directly. Each route has ingots per ore, FE per ingot, Crusher and Furnace ticks per ingot (raw and power-supplied), and Furnaces needed per Crusher.

### Keeping it accurate

Stats assemble through `MachineBaseStatCatalog`, `ComponentBaseStatCatalog`, and `MegaPassiveTree`, and RNGTech config defaults load headless. Crusher yield (the soft-capped Output Amount bucket and its time cost) goes through the shared `CrusherYield` helper, and the run fails if yield alone ever raises items per tick above the chassis base. Ticks, FE, Output Amount, Super Output, batching, and Overdrive mirror `CrusherBlockEntity` and `FurnaceBlockEntity` in `src/masteryTest/java/com/rngtech/rpg/ProcessingBalanceSimulation.java`, so update it when those methods change. The model counts steady running only: Furnace warm-up, jams, and output-blocked waits are left out.
