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

The **Metal Press** forms ingots into plates, gears, and casings, presses circuit blanks into Electric Circuits, and presses coils into Energy Connectors. The installed Mold decides what it makes. There are two presses: the **Crude Metal Press** is the early bootstrap press, and the Stage 4 **Metal Press** (the Steel press) is faster and more controlled but can fail.

Both presses run on FE and heat up before pressing. Like every RNGTech machine, each press you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

Each press has its own recipe. The Steel press is built on a regular Machine Frame and does not use up a Crude Metal Press.

{{ crafting("rngtech:crude_metal_press", "rngtech:metal_press") }}

### Breaking

Mine a placed press with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear and stored Molds, drop next to it.

## Usage

### Pressing

Install a Heat Core and at least one Mold, select the Mold you want in the Gear tab, then put the input in the input slot and supply FE. The press warms up using the recipe's normal FE draw and starts pressing only once its heat reaches the recipe's target temperature. If it runs out of FE while warming up, it pauses and cools.

Each recipe has a heat window and a temperature stability requirement. The two presses handle a bad window differently:

- The **Crude Metal Press** never fails. If its heat is too low or too high, its stability is too low, or it runs out of power, it pauses and leaves the input alone. A press that is too hot cools down before continuing.
- The **Metal Press** builds up failure strain instead. Low or high heat, low stability, a weak Servo, or a power drop after pressing starts all add strain. When strain fills, the input is used up and the press outputs a failure item: a {{ item('rngtech:malformed_ingot') }} for plates and casings, or a {{ item('rngtech:broken_circuit') }} for circuits. Smelt Malformed Ingots in a [Furnace](furnace.md) to get two nuggets back. The [Potential Reactor](potential-reactor.md) and [Component Recycler](component-recycler.md) can recover scrap from Broken Circuits.

### Molds

| Mold | Makes | Input |
|---|---|---|
| {{ item('rngtech:plate_mold') }} | Material plates | 1 ingot |
| {{ item('rngtech:gear_mold') }} | Material gears | 3 ingots |
| {{ item('rngtech:casing_mold') }} | Material casings, for the metals that use them | 4 ingots |
| {{ item('rngtech:circuit_mold') }} | Electric Circuits | 1 circuit blank |
| {{ item('rngtech:connector_mold') }} | Energy Connectors for the [Universal Cable](universal-cable.md) | Staged coils |

The press stores up to five Molds, so you can keep all of them installed and switch between them in the Gear tab.

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
- one **Battery Cell** slot (optional). Without a cell, the press keeps only a small working buffer and runs slower and less stably.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe input. |
| Bottom | Extracts finished output. |
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

Gear slots, Molds, and the Refinement slot cannot be filled by automation.

### Mastery and Ascendancies

Each successful plate, gear, casing, or circuit earns press Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. Connector recipes, failures, and paused cycles earn nothing. Presses choose between the **Die Keeper** and **Drop Forge** [ascendancies](machine-mastery.md#ascendancies): Die Keeper swaps Molds automatically, and Drop Forge presses several items per cycle.

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

## Screen

Both presses share a screen with five tabs:

- **Process**: input and output slots, progress, energy, live heat, and a failure-risk bar. Status squares show the recipe state and the current risk. Hover for exact values.
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
