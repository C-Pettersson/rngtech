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
- `bio_take_off_methane_per_fe`: Methane the Bio Generator take-off makes per FE of digested gas (the game uses `0.05`).

Nothing in a scenario changes the game.

### Output

The task writes `build/energy-balance/energy-balance.json`. For each scenario and behavior it contains:

- `setups`: one row per chassis and part combination, with the unrolled baseline and distributions for running FE/t, 24-hour average FE/t, FE per consumed input, FE/t net of the fuel chain, and generator-specific metrics such as Aethergold output kept per connector tier.
- `pools`: setups grouped by generator and highest component stage, with the affixes most over-represented in the top 5% of crafts.
- `ladder`: the best unrolled sustained setup per stage, and how often a craft of it beats the next stage's best unrolled setup.

Each scenario also has `gearFuel`: FE per item when crafted gear is burned in a Potential Reactor.

`bio_generator_take_off` setups are one rolled Bio Generator exporting Methane instead of burning it (Energy Generation only digests faster there, so Methane per item depends on the fuel alone), by separation (bare pump or a membrane at stability 62 or 100), water-gas shift, Bio Chamber, and retention. FE/t is what the Reformer, Ammonia Synthesizer, and Fuel Cell make from that Methane at base stats, and net also pays those machines. Metrics give Methane per tick and per Rich Biomass, Bio Generators per Fuel Cell, Rich Biomass per minute per Fuel Cell, and the net as a multiple of burning the same gas in place. These setups feed a generator rather than being one, so they stay out of the `ladder`.

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
- `trees`: shared Mastery tree allocations, by level, in a valid allocation order. `processingTreeSearch` writes them; see [Tree search](#tree-search).
- `treeSearch`: what each tree maximizes. Each entry under `objectives` names the machine `family`, the archetype whose affixes and ascendancy score it, and its `metrics`: `output` (items per input), `speed` (inputs per tick), `efficiency` (inputs per FE), and `batch` (Crusher Batch Size).

Every listed affix is a perfect crafted Magic roll: the highest tier refinement upgrades reach (`6`, or the family's top tier), at its maximum value. The task rejects affixes that a profile cannot roll or that share a mod group.

### Tree search

```sh
./gradlew processingTreeSearch
```

The task rewrites `trees` in `processing-setups.json` for every tree under `treeSearch.objectives`. Run it after a passive tree change; `masteryCheck` fails while a stored allocation is no longer a valid build.

A build's score is the sum, over every stage and each of the tree's metrics, of the log ratio to the same machine with no tree. The Crusher is measured crushing the stage's ore, the Furnace smelting its crushed ore, with the ascendancy nodes the stage's Seals unlock. The search is greedy and deterministic: each step allocates the shortest path to whichever node gains the most score per point, ties going to the shorter path and then the lower node index. It stops when no path gains or the level's points run out, so a tree can leave points unspent. Nodes that cap recipe temperature or hardness, Single Pass, and Cell Bypass are never allocated, since they would block recipes the model runs.

### Output

The task writes `build/processing-balance/processing-balance.json`. For each stage, archetype, and level it reports:

- `crusher` and `furnace`: the effective stats and the Mastery build that applied.
- `crushOre`, `crushDust`, `smeltDust`, `smeltCrushed`, `smeltOre`: one row per machine step with output per input, FE per input and per output, ticks per input, running FE/t, batch or lane count, and whether the stage's power supply limits it.
- `routes`: ore to ingot through crushed and dust, through crushed only, or smelted directly. Each route has ingots per ore, FE per ingot, Crusher and Furnace ticks per ingot (raw and power-supplied), and Furnaces needed per Crusher.

### Keeping it accurate

Stats assemble through `MachineBaseStatCatalog`, `ComponentBaseStatCatalog`, and `MegaPassiveTree`, and RNGTech config defaults load headless. Ticks, FE, Output Amount, Super Output, batching, and Overdrive mirror `CrusherBlockEntity` and `FurnaceBlockEntity` in `src/masteryTest/java/com/rngtech/rpg/ProcessingBalanceSimulation.java`, so update it when those methods change. The model counts steady running only: Furnace warm-up, jams, and output-blocked waits are left out.

## Phase Array model

`./gradlew phaseArraySim` models the [Phase Array PRD](../../docs/prds/phase-array.md) before any of it exists in code: the hold sequence, Strain and Fracture, FE by Quality, and the Refinement Potential ceiling. Fresh affix tiers and RP roll through `MachineTraitRoller`, so the starting items are what the game crafts; every other rule is the PRD's, read from `phase-array-scenarios.json`.

```sh
./gradlew phaseArraySim
```

### Scenarios

`prd` is the PRD's number set. Every other scenario overrides part of it to preview a change; `first_draft` keeps the numbers the PRD started with. Keys: `hold_chance`, `second_hold`, `overhold_per_surplus`, `partial_window`, `fracture_per_strain`, `strain_tolerance`, `quality_base`, `quality_divisor`, `stage_factors`, `reference_supply_fe_per_tick`, and `operations` (`base_fe`, `coils`, `rp`, `glyph` per operation). `items` lists the fresh items to start from, and `line_sizes` the Coil counts to run.

### Behaviors

Every player reads the preview, so none fires once the Fracture chance reaches 25% (`strain` in `stoppedBy`).

- `resonate_until_rp`: Resonate the lowest affix until every affix is Tier 6 or RP runs out.
- `resonate_with_budget`: as above, but stop when one pulse would take more than ten minutes at the reference supply.
- `resonate_and_temper`: as `resonate_until_rp`, Tempering whenever the Fracture chance reaches 10%, with Glyphs assumed on hand.
- `exalt_all`: start from an all-Tier-6 item with fresh RP and Exalt each affix to Tier 7 until RP runs out.
- `reforge_fish`: Reforge until the item is Rare with Quality 12 or more, paying each Reforge at the item's current Quality.

An item whose affixes were all fractured away stops as `dead`.

### Output

The task writes `build/phase-array/phase-array.json`. For each scenario:

- `resonateTimeByQuality`: Resonate FE and minutes at the reference supply, by item stage and Quality.
- `ladder`: the closed-form outcome odds (Success, Overhold, Partial, Miss, Fracture) per operation, line size, and Strain. This is what the Cradle preview would show.
- `behaviors`: per behavior, item, and line size, distributions of pulses, FE, minutes, final Quality, Fractures, Strain, RP left, and Glyphs, plus how often the goal was reached and what stopped the player.

### Keeping it accurate

Nothing in the game implements the Array yet. When it does, each rule in `src/masteryTest/java/com/rngtech/rpg/PhaseArraySimulation.java` must name the method it mirrors, as the other simulations do.
