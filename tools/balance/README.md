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

`energy-scenarios.json` holds what-if scenarios. `current` must stay empty: it is the game as coded. Other scenarios override the simulator's mirrored generator formulas (recipe energy, solid fuel value, solar array rules, Corrosion Cell recipes, Potential Reactor gear-fuel scaling and fatigue, Aethergold overflow). Nothing in a scenario changes the game.

### Output

The task writes `build/energy-balance/energy-balance.json`. For each scenario and behavior it contains:

- `setups`: one row per chassis and part combination, with the unrolled baseline and distributions for running FE/t, 24-hour average FE/t, FE per consumed input, FE/t net of the fuel chain, and generator-specific metrics such as Aethergold output kept per connector tier.
- `pools`: setups grouped by generator and highest component stage, with the affixes most over-represented in the top 5% of crafts.
- `ladder`: the best unrolled sustained setup per stage, and how often a craft of it beats the next stage's best unrolled setup.

Each scenario also has `gearFuel`: FE per item when crafted gear is burned in a Potential Reactor.

### Keeping it accurate

Rolls use `MachineTraitRoller`, part stats merge through `ComponentBaseStatCatalog`, and refinement uses `RefinementEngine`. Each generator's final FE formula is mirrored in `src/masteryTest/java/com/rngtech/rpg/EnergyBalanceSimulation.java` and names the block entity method it follows, so update it when that method changes. Recipe energy and ticks are read from the recipe JSON.

`energy-chain-costs.json` lists the FE that base-stat machines spend producing each generator input, plus which inputs can be obtained and from which stage. Recheck it when a fuel chain recipe changes.
