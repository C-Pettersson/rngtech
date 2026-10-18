---
wiki:
  category: Processing
  icon: rngtech:metal_press
  ids:
    - rngtech:crude_metal_press
    - rngtech:metal_press
    - rngtech:insulator
    - rngtech:basic_circuit_blank
    - rngtech:advanced_circuit_blank
    - rngtech:elite_circuit_blank
    - rngtech:ultimate_circuit_blank
    - rngtech:basic_electric_circuit
    - rngtech:advanced_electric_circuit
    - rngtech:elite_electric_circuit
    - rngtech:ultimate_electric_circuit
---

# Metal Press

{{ infobox(
    variants=[
        ["Crude", "rngtech:crude_metal_press"],
        ["Steel", "rngtech:metal_press"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "3 (Crude), 4 (Steel)",
        "Power": "FE",
        "Gear": "{{ item('rngtech:bronze_heat_core', 'Heat Core') }}, {{ item('rngtech:steel_servo', 'Servo') }}, {{ item('rngtech:plate_mold', 'Mold') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "Yes",
    },
) }}

The **Metal Press** forms ingots into plates, gears, and casings, presses circuit blanks into Electric Circuits, and presses coils, casings, and gears into Universal Cable connectors. The installed Mold decides what it makes. There are two presses: the **Crude Metal Press** is the early bootstrap press, and the Stage 4 **Metal Press** (the Steel press) is faster and more controlled but can fail.

Both presses run on FE and heat up before pressing. Like every RNGTech machine, each press you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

You can read a press from the outside. The selected Mold sits on the bed as the die with the input lying on it, and the other stored Molds stand in the rack on the back cabinet. While the press works, the ram comes down as the cycle runs and strikes when it finishes, and pressed parts collect in the tray at the front of the bed. The bed and the die glow from dull red to yellow-white with the heat. The bar on the front of the bed reads like the [heat bar](heat.md#the-overheat-slider) on the Process tab: it fills with the heat against the Heat Core's Max Temperature, a white mark shows the recipe's target and a yellow mark the setpoint, and the fill turns red while the press overheats. Facing the front, the Heat Core is the finned radiator on the right and the Battery Cell stands in the cradle on the left with its charge meter. The gas cylinder behind the cradle is the [atmosphere tank](heat.md#nitrogen-atmosphere): its window shows how full it is, pale blue for Nitrogen and near white for Hydrogen, and its valve lights up while the press uses Nitrogen. The cable port is on the back.

The Steel press is a servo press: the Servo lies on the crown and drives the ram, and the meter next to the heat bar fills from amber to red with failure strain and blinks when a failure is near. The Crude press is a bronze fly press: copper coils heat its firebrick bed, and the screw and its fly bar turn as the ram comes down. A Servo installed in a Crude press sits behind the crosshead.

## Obtaining

### Crafting

Each press has its own recipe. The Steel press is built on a regular Machine Frame and does not use up a Crude Metal Press.

{{ crafting("rngtech:crude_metal_press", "rngtech:metal_press") }}

### Breaking

Mine a placed press with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear and stored Molds, drop next to it.

## Usage

### Pressing

Install a Heat Core and at least one Mold, select the Mold you want in the Gear tab, then put the input in the input slot and supply FE. The press heats up and starts pressing once its heat reaches the recipe's temperature; without FE its heater stops and it cools. See [Heat](heat.md) for how it heats, what the FE pays for, and the overheat slider. Adding more of the same input keeps the current press going, so a hopper or Item Connector can feed it; swapping the input for a different item restarts it.

Each recipe has a heat window and a temperature stability requirement. The two presses handle a bad window differently:

- The **Crude Metal Press** never fails. If its heat is too low or too high, its stability is too low, or it runs out of power, it pauses and leaves the input alone. A press that is too hot cools down before continuing.
- The **Metal Press** builds up failure strain instead. Low heat, low stability, a weak Servo, or a power drop after pressing starts all add strain. Like the Crude press, it cools down to the recipe's overheat temperature before pressing when a hotter recipe left it above that. When strain fills, the input is used up and the press outputs a failure item: a {{ item('rngtech:malformed_ingot') }} for plates and casings, or a {{ item('rngtech:broken_circuit') }} for circuits. Smelt Malformed Ingots in a [Furnace](furnace.md) to get two nuggets back, or keep Stage 5 and higher ones for the [Volatile Catalyst](affix-forge.md#crafting-the-consumables). The [Potential Reactor](potential-reactor.md) and [Component Recycler](component-recycler.md) can recover scrap from Broken Circuits.

### Molds

| Mold | Makes | Input |
|---|---|---|
| {{ item('rngtech:plate_mold') }} | Material plates | 1 ingot |
| {{ item('rngtech:gear_mold') }} | Material gears | 3 ingots |
| {{ item('rngtech:casing_mold') }} | Material casings, for the metals that use them | 4 ingots |
| {{ item('rngtech:circuit_mold') }} | Electric Circuits | 1 circuit blank |
| {{ item('rngtech:connector_mold') }} | Energy, Fluid, and Item Connectors for the [Universal Cable](universal-cable.md) | 1 coil, casing, or gear; 8 Nullite Coils for the Exotic Energy Connector |

The press stores up to five Molds, so you can keep all of them installed and switch between them in the Gear tab. Selecting another Mold, or swapping the Heat Core, Servo, or a Mold, restarts the current press.

### Circuits

Electric Circuits are a main reason to build a press. Every tier works the same way: craft four circuit blanks at a crafting table, then press each blank into a circuit with the {{ item('rngtech:circuit_mold') }}. Each tier's blank needs the circuit from the tier below, so you climb the chain in order:

1. **Insulators.** Craft {{ item('rngtech:insulator') }} from glass, quartz, clay, and smooth stone. Every blank uses them.
2. **Basic.** A {{ item('rngtech:basic_circuit_blank') }} needs Insulators, a Bronze Plate, a Copper Coil, redstone, and quartz. A Bronze Heat Core is enough to press it into a {{ item('rngtech:basic_electric_circuit') }}.
3. **Calibrate.** Run a Basic Electric Circuit through a [Resonance Calibrator](resonance-calibrator.md) to get a {{ item('rngtech:calibrated_logic_component') }}. Its stability roll decides which higher blanks it can go into.
4. **Advanced.** An {{ item('rngtech:advanced_circuit_blank') }} needs a Basic circuit, Gold Coils, a Steel Plate, Insulators, and a Calibrated Logic Component with **55** or more stability. A well-equipped Crude Metal Press can press it.
5. **Elite.** An {{ item('rngtech:elite_circuit_blank') }} needs an Advanced circuit, Titanium Plates, a Titanium Control Board, a Titanium Resonance Coil, an Arclite Coil, Insulators, and a Calibrated Logic Component with **80** or more stability.
6. **Ultimate.** An {{ item('rngtech:ultimate_circuit_blank') }} needs an Elite circuit, Tungstensteel Plates, Naquadah Coils, a Nullite Resonance Coil, Insulators, and a Stage 6 {{ item('rngtech:calibrated_diamond_crystal') }} with **85** or more stability.

Elite and Ultimate circuits expect the Steel press or a strongly upgraded Crude press. The Notes column below shows the stage and stability each calibrated ingredient needs. The Circuits table under [Metal Press recipes](#metal-press-recipes) lists the heat, stability, and FE for pressing each blank.

{{ crafting("rngtech:insulator", "rngtech:basic_circuit_blank", "rngtech:advanced_circuit_blank", "rngtech:elite_circuit_blank", "rngtech:ultimate_circuit_blank") }}

{{ processing("calibration", output="rngtech:calibrated_logic_component", columns=["processing_ticks", "energy", "minimum_stage"]) }}

### Stages

| Stage | Press | Notes |
|---:|---|---|
| 3 | {{ item('rngtech:crude_metal_press') }} | Bootstrap press. Servo optional. Pauses instead of failing. |
| 4 | {{ item('rngtech:metal_press') }} | Servo required. Uses failure strain and failure outputs. |

### Gear

The Gear tab has:

- one **Heat Core** slot (required). Iron through Titanium Heat Cores fit; the core sets the forming temperature and heat stability.
- one **Servo** slot. Optional on the Crude press, required on the Steel press. The Servo adds speed, stability, overheat tolerance, and protection against power drops.
- five **Mold** slots with a selector for the active Mold. An active Mold is required.
- one **Battery Cell** slot (optional). Without a cell, the press keeps only a small working buffer and runs slower and less stably. Incoming FE fills the press's own buffer first, then charges the cell, and pressing draws from the buffer before the cell.

The Stability stat only matters on the Steel press, where low Stability adds failure strain. The Crude press ignores it.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe input. Rejects items no Mold can press. |
| Bottom | Extracts finished output. |
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md), and Nitrogen for the [Nitrogen atmosphere](heat.md#nitrogen-atmosphere). With Bright Anneal or Hydrogen Quench, also accepts Hydrogen for [annealing](#annealing). |

Gear slots, Molds, and the Refinement slot cannot be filled by automation.

### Mastery and Ascendancies

Each successful plate, gear, casing, or circuit earns press Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. Connector recipes, failures, and paused cycles earn nothing. Presses choose between the **Die Keeper** and **Drop Forge** [ascendancies](machine-mastery.md#ascendancies): Die Keeper swaps Molds automatically, and Drop Forge presses several items per cycle.

Two keystones cap your heat in exchange for speed or savings. Each cap only stops Gear from going higher; it never raises a weaker Heat Core. **Flash Annealing** caps Max Temperature at 1,000 heat, enough for Iron, Copper, Bronze, Gold, Tin, and Zinc parts and several connectors. It gives 100% more Processing Speed with 50% more Energy Usage, so the press works twice as fast and each item costs 1.5 times the FE. **Low Heat Specialist** caps Max Temperature at 800 heat, which allows only Iron plates, gears, and casings, and its savings only work on the Furnace. On a press it is only a cap.

**Quench Protocol** halves the temperature failure strain the press takes, for 25% less Heat Transfer.

Die Keeper's Mold Rack picks the first stored Mold, in slot order, that has a recipe for the input. The press cools during the swap unless you have Quick Change. A Drop Forge batch is set when the cycle starts and is limited by the input stack and the free output space. Each pressed item in a batch pays its own FE, earns its own XP, and rolls Super Output on its own.

### Annealing

Two ascendancy notables let the press anneal what it makes under Hydrogen: Die Keeper's **Bright Anneal**, after Purge Line, and Drop Forge's **Hydrogen Quench**, after Quench Line. With either one, the press's [atmosphere tank](heat.md#nitrogen-atmosphere) also takes {{ item('rngtech:hydrogen_bucket') }} Hydrogen. The tank holds one gas at a time, so it takes Hydrogen only while it holds no Nitrogen; pump the tank out with a [Universal Cable](universal-cable.md) to switch gases. Without either notable it refuses Hydrogen.

- **Bright Anneal**: Basic Electric Circuits pressed while the tank holds Hydrogen come out Annealed.
- **Hydrogen Quench**: plates pressed while the tank holds Hydrogen come out Annealed.

Each Annealed item costs 250 mB of Hydrogen. Other outputs press normally and use none. A Drop Forge batch presses only as many plates as the tank can pay for, so a batch of 6 plates needs 1,500 mB. Hover the gauge on an annealing press to see what it anneals and the cost.

Annealed items show an aqua **Annealed** line in their tooltip. They do not stack with plain copies, and every recipe accepts them like plain ones. What annealing gives you:

- An Annealed Basic Electric Circuit calibrates with [lucky stability](resonance-calibrator.md#annealed-circuits) in the Resonance Calibrator.
- Gear crafted with only Annealed plates rolls [lucky Refinement Potential](rarity-and-affixes.md#annealed-items).

### Metal Press recipes

Both presses use the `rngtech:metal_press` recipe type. Each table lists the recipes for one Mold; the failure item in the output column is what the Steel press makes when a craft fails.

#### Plates

{{ processing("metal_press", input="rngtech:plate_mold", hide=["failure_material"]) }}

#### Gears

{{ processing("metal_press", input="rngtech:gear_mold", hide=["failure_material"]) }}

#### Casings

{{ processing("metal_press", input="rngtech:casing_mold", hide=["failure_material"]) }}

#### Circuits

{{ processing("metal_press", input="rngtech:circuit_mold", hide=["failure_material"]) }}

#### Connectors

{{ processing("metal_press", input="rngtech:connector_mold", hide=["failure_material"]) }}

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

<!-- ascendancy-trees:start -->

### Die Keeper

| Node | Type | After | Effect |
|---|---|---|---|
| **Mold Rack** | Root | — | +40 ticks Mold Swap Time. Switches to the stored mold that fits the input after the Mold Swap Time. |
| Swap Drill | Small | Mold Rack | −5 ticks Mold Swap Time. |
| **Quick Change** | Notable | Swap Drill | −25 ticks Mold Swap Time. The press keeps its heat while it swaps molds. |
| Rack Drill | Small | Quick Change | −5 ticks Mold Swap Time. |
| **Production Die** | Deep notable | Rack Drill | Gears and casings press 40% faster; circuits 25% slower. |
| Steady Bed | Small | Mold Rack | 8% increased Temperature Stability. |
| **Tolerance Map** | Notable | Steady Bed | 40% less heat-related failure strain. |
| True Bed | Small | Tolerance Map | 8% increased Temperature Stability. |
| **Master Die** | Deep notable | True Bed | 20% less Processing Speed. No failures while heat stays in the window and power holds. |
| Purge Line | Small | Mold Rack | 5% reduced Energy Use. |
| **Bright Anneal** | Notable | Purge Line | The atmosphere tank takes Hydrogen, and Basic Electric Circuits pressed with it come out Annealed for 250 mB each. An Annealed circuit calibrates with lucky stability. |
| Fine Servos | Small | Mold Rack | 8% increased Stability. |
| **Circuit Discipline** | Notable | Fine Servos | Circuit recipes gain 20% increased Stability and use 15% less FE. |
| Heat Margin | Small | Mold Rack | 10% increased Overheat Tolerance. |
| **Servo Sync** | Notable | Heat Margin | Power-drop grace is doubled, halving power-drop strain again. |

### Drop Forge

| Node | Type | After | Effect |
|---|---|---|---|
| **Drop Hammer** | Root | — | +4 Batch Size; −15% Heat Window. Plate, gear, and casing recipes batch; circuits never do. |
| Heavy Blows | Small | Drop Hammer | 8% increased Processing Speed. |
| **Anvil Mass** | Notable | Heavy Blows | +2 Batch Size. |
| Fast Line | Small | Anvil Mass | 8% increased Processing Speed. |
| **Forge Line** | Deep notable | Fast Line | +4 Batch Size. Circuit recipes are disabled. |
| Wide Bed | Small | Drop Hammer | +5% Heat Window. |
| **Split Failure** | Notable | Wide Bed | A failed batch ruins one job’s input instead of all of them; the rest waits for the next cycle. |
| Broad Bed | Small | Split Failure | +5% Heat Window. |
| **Batch Ledger** | Deep notable | Broad Bed | +5% Ledger Rate. |
| Quench Line | Small | Drop Hammer | 8% increased Temperature Stability. |
| **Hydrogen Quench** | Notable | Quench Line | The atmosphere tank takes Hydrogen, and plates pressed with it come out Annealed for 250 mB each. Gear crafted only from Annealed plates rolls lucky Refinement Potential. |
| Lean Strikes | Small | Drop Hammer | 5% reduced Energy Use. |
| **Shock Absorbers** | Notable | Lean Strikes | Power-drop grace is doubled while batching. |
| Hot Platen | Small | Drop Hammer | 8% increased Heat Transfer. |
| **Hot Stamping** | Notable | Hot Platen | Plates press 20% faster above their target temperature. |

<!-- ascendancy-trees:end -->

## Screen

Both presses share a screen with five tabs:

- **Process**: input and output slots, progress, energy, live heat, a failure-risk bar, and the atmosphere gauge at the right edge, for Nitrogen or Hydrogen. Status squares show the recipe state and the current risk. Hover for exact values. The progress tooltip also shows time remaining and expected items per minute with your Gear and Mastery applied (chance bonuses such as Super Output are not counted).
- **Gear**: Heat Core, Servo, Battery Cell, and Mold slots, plus the active Mold selector.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed press's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Data values

{{ data_values() }}

## See also

- [Gear](gear.md)
- [Resonance Calibrator](resonance-calibrator.md)
- [Furnace](furnace.md)

{{ navbox() }}
