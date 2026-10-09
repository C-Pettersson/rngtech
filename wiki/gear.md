---
wiki:
  category: Guides
  icon: rngtech:iron_heat_core
  ids:
    - rngtech:flint_crush_head
    - rngtech:iron_crush_head
    - rngtech:copper_crush_head
    - rngtech:bronze_crush_head
    - rngtech:steel_crush_head
    - rngtech:aluminum_crush_head
    - rngtech:titanium_crush_head
    - rngtech:tungstensteel_crush_head
    - rngtech:exotic_crush_head
    - rngtech:iron_heat_core
    - rngtech:copper_heat_core
    - rngtech:bronze_heat_core
    - rngtech:steel_heat_core
    - rngtech:aluminum_heat_core
    - rngtech:sparksteel_heat_core
    - rngtech:titanium_heat_core
    - rngtech:exotic_heat_core
    - rngtech:bronze_alloy_crucible
    - rngtech:steel_alloy_crucible
    - rngtech:titanium_alloy_crucible
    - rngtech:iron_fuel_box
    - rngtech:copper_fuel_box
    - rngtech:bronze_fuel_box
    - rngtech:steel_fuel_box
    - rngtech:bio_chamber
    - rngtech:iron_reactor_chamber
    - rngtech:copper_reactor_chamber
    - rngtech:bronze_reactor_chamber
    - rngtech:steel_reactor_chamber
    - rngtech:iron_recovery_filter
    - rngtech:copper_recovery_filter
    - rngtech:bronze_recovery_filter
    - rngtech:steel_recovery_filter
    - rngtech:iron_containment_lining
    - rngtech:copper_containment_lining
    - rngtech:bronze_containment_lining
    - rngtech:steel_containment_lining
    - rngtech:iron_disassembly_head
    - rngtech:copper_disassembly_head
    - rngtech:bronze_disassembly_head
    - rngtech:steel_disassembly_head
    - rngtech:aluminum_disassembly_head
    - rngtech:titanium_disassembly_head
    - rngtech:tungstensteel_disassembly_head
    - rngtech:exotic_disassembly_head
    - rngtech:steel_servo
    - rngtech:titanium_servo
    - rngtech:tungstensteel_servo
    - rngtech:nullite_servo
    - rngtech:exotic_servo
    - rngtech:osmium_fluid_pump
    - rngtech:titanium_fluid_pump
    - rngtech:tungstensteel_fluid_pump
    - rngtech:exotic_fluid_pump
    - rngtech:aluminum_cathode
    - rngtech:titanium_cathode
    - rngtech:tungstensteel_cathode
    - rngtech:naquadah_cathode
    - rngtech:iron_resonance_coil
    - rngtech:copper_resonance_coil
    - rngtech:steel_resonance_coil
    - rngtech:titanium_resonance_coil
    - rngtech:tungstensteel_resonance_coil
    - rngtech:nullite_resonance_coil
    - rngtech:iron_control_board
    - rngtech:copper_control_board
    - rngtech:steel_control_board
    - rngtech:titanium_control_board
    - rngtech:tungstensteel_control_board
    - rngtech:nullite_control_board
    - rngtech:iron_stabilizer_matrix
    - rngtech:copper_stabilizer_matrix
    - rngtech:steel_stabilizer_matrix
    - rngtech:titanium_stabilizer_matrix
    - rngtech:tungstensteel_stabilizer_matrix
    - rngtech:nullite_stabilizer_matrix
    - rngtech:steel_cavitation_rotor
    - rngtech:titanium_cavitation_rotor
    - rngtech:nitrogen_extraction_rotor
    - rngtech:tungstensteel_cavitation_rotor
    - rngtech:aethergold_cavitation_rotor
    - rngtech:nullite_cavitation_rotor
    - rngtech:steel_collapse_nozzle
    - rngtech:titanium_collapse_nozzle
    - rngtech:nitrogen_separation_nozzle
    - rngtech:tungstensteel_collapse_nozzle
    - rngtech:nullite_collapse_nozzle
    - rngtech:exotic_collapse_nozzle
    - rngtech:tungstensteel_void_chamber
    - rngtech:nullite_void_chamber
    - rngtech:exotic_void_chamber
    - rngtech:tungstensteel_dimensional_stabilizer
    - rngtech:nullite_dimensional_stabilizer
    - rngtech:exotic_dimensional_stabilizer
    - rngtech:fuel_cell_membrane
---

# Gear

**Gear** is the set of parts you install inside a machine: Heat Cores, Crush Heads, Servos, Battery Cells, and more. The machine block decides what kind of machine it is; its Gear decides much of what it can actually do, such as how hot it gets, how hard an ore it can crush, or how much FE it stores.

Every Gear part is its own item with its own [rarity and affixes](rarity-and-affixes.md), so two Iron Heat Cores can perform differently.

## The Gear tab

Machines that take Gear have a **Gear** tab in their screen. Each slot accepts one kind of part.

- **Required** slots must be filled before the machine runs. A Crusher with no Crush Head does nothing.
- **Optional** slots add stats or unlock extra behavior.
- Hover an empty slot to see what it accepts. Hold Shift for example items.
- With JEI installed, the **Machine Gear** category lists every machine's slots, and looking up a part's uses shows which machines take it.

You install Gear by hand. Pipes, hoppers, and connectors cannot reach Gear slots, with one exception: Crushers and electric Furnaces accept Battery Cells from their sides.

## Stage caps

Gear parts have a [stage](stages.md), just like machines. Most slots only accept parts **up to the machine's own stage**: a Bronze (Stage 3) Crusher takes a Bronze Crush Head or lower, not a Steel one. The tooltip shows each part's stage.

A few slots work differently:

- Some machines need a **minimum** stage instead, such as the Coal Gasifier's Stage 5+ Heat Core.
- The Metal Press and Melter take Heat Cores only up to Stage 6.
- Some slots take any stage. These are noted in the tables below.
- On a Crusher, the Component Mount nodes in [Machine Mastery](machine-mastery.md) can raise the Crush Head limit by one stage per node.

## Rolls and refinement

Each crafted part rolls its own rarity and affixes, and you can refine it in the [Affix Forge](affix-forge.md). A part's affixes are **local**: they scale that part's own stats, and the result is then added to the machine. Holding Shift on a tooltip marks these rolls "(Local)".

Higher-stage parts are crafted from fresh materials, not from the previous part. You can keep a well-rolled part in service and craft the next stage separately. The {{ item('rngtech:nullite_servo') }} is the one part that upgrades from an older part, the {{ item('rngtech:titanium_servo') }}.

Many parts you no longer need can be broken down for materials in a [Component Recycler](component-recycler.md).

## Part families

### Crush Heads

A Crush Head sets the **hardness level** your machine can crush, from 1 to 8. The [Crusher](crusher.md) can still crush ores above its head's level, but slower, at higher FE cost, and with a chance to jam.

| Crush Head | Stage | Hardness |
|---|---:|---:|
| {{ item('rngtech:flint_crush_head') }} | 0 | 1 |
| {{ item('rngtech:iron_crush_head') }} | 1 | 2 |
| {{ item('rngtech:copper_crush_head') }} | 2 | 2 |
| {{ item('rngtech:bronze_crush_head') }} | 3 | 3 |
| {{ item('rngtech:steel_crush_head') }} | 4 | 4 |
| {{ item('rngtech:aluminum_crush_head') }} | 5 | 5 |
| {{ item('rngtech:titanium_crush_head') }} | 6 | 6 |
| {{ item('rngtech:tungstensteel_crush_head') }} | 7 | 7 |
| {{ item('rngtech:exotic_crush_head') }} | 8 | 8 |

The Flint Crush Head is cheap starter tooling with slower processing.

**Used in:** [Crusher](crusher.md) (required, up to the chassis stage), [Melter](melter.md) (required, any stage, sets the processing level for melts), and the Miner's Companion (required).

{{ crafting('rngtech:iron_crush_head') }}

### Heat Cores

Heat Cores set heat. In heated processors they decide maximum temperature, how fast the machine warms up, and how steady the heat is. In solid fuel burners they set FE/t output and the highest fuel tier you can burn.

| Heat Core | Stage | Notes |
|---|---:|---|
| {{ item('rngtech:iron_heat_core') }} | 1 | |
| {{ item('rngtech:copper_heat_core') }} | 2 | |
| {{ item('rngtech:bronze_heat_core') }} | 3 | |
| {{ item('rngtech:steel_heat_core') }} | 4 | |
| {{ item('rngtech:aluminum_heat_core') }} | 5 | Fast but low-heat sidegrade. |
| {{ item('rngtech:sparksteel_heat_core') }} | 5 | |
| {{ item('rngtech:titanium_heat_core') }} | 6 | |
| {{ item('rngtech:exotic_heat_core') }} | 8 | Highest heat. |

**Used in:**

| Machine | Slot |
|---|---|
| Electric [Furnace](furnace.md) | Required, up to the chassis stage. The Lead Furnace has four, one per lane. |
| [Alloy Furnace](alloy-furnace.md) | Required, up to the chassis stage. |
| [Solid Fuel Burner](solid-fuel-burner.md) | Required, up to the burner's stage. |
| [Metal Press](metal-press.md) | Required, up to Stage 6. |
| [Melter](melter.md) | Required, up to Stage 6. |
| [Coal Gasifier](coal-gasifier.md) | Required, Stage 5 or higher. |
| [Steam Methane Reformer](steam-methane-reformer.md) | Required, Stage 6 or higher. |
| [Cavitation Generator](cavitation-generator.md) | Optional, any stage. |

The [Furnace](furnace.md) page lists the temperature each core reaches, and the [Solid Fuel Burner](solid-fuel-burner.md) page lists each core's FE/t and fuel tier.

{{ crafting('rngtech:iron_heat_core') }}

### Alloy Crucibles

The crucible sets how many input slots an [Alloy Furnace](alloy-furnace.md) has and adds mixing stability and speed.

| Alloy Crucible | Stage | Input slots |
|---|---:|---:|
| {{ item('rngtech:bronze_alloy_crucible') }} | 3 | 3 |
| {{ item('rngtech:steel_alloy_crucible') }} | 4 | 3 |
| {{ item('rngtech:titanium_alloy_crucible') }} | 6 | 4 |

**Used in:** [Alloy Furnace](alloy-furnace.md) (required, up to the chassis stage).

{{ crafting('rngtech:bronze_alloy_crucible') }}

### Fuel Boxes

The Fuel Box sets a burner's fuel slots, which fuel forms it burns, and fuel behaviors such as the Fuel Governor.

| Fuel Box | Stage | Fuel slots | Fuel forms |
|---|---:|---:|---|
| {{ item('rngtech:iron_fuel_box') }} | 1 | 1 | Items |
| {{ item('rngtech:copper_fuel_box') }} | 2 | 1 | Items |
| {{ item('rngtech:bronze_fuel_box') }} | 3 | 2 | Items |
| {{ item('rngtech:steel_fuel_box') }} | 4 | 2 | Items and compact fuel blocks |

**Used in:** [Solid Fuel Burner](solid-fuel-burner.md) (required, up to the burner's stage). That page explains each box's behaviors.

### Bio Chamber

The {{ item('rngtech:bio_chamber') }} adds power, fuel duration, and fuel efficiency to bio fuels. Its affixes can favor particular crops or prepared biomass fuels.

**Used in:** [Bio Generator](bio-generator.md) (required) and [Algae Photobioreactor](algae-photobioreactor.md) (required), where it scales algae output.

### Potential Reactor parts

The [Potential Reactor](potential-reactor.md) uses three part types:

- **Reactor Chamber** (required): its stage decides which inputs the reactor can process, and it sets salvage generation and stability.
- **Recovery Filter** (optional): enables residue recovery and improves salvage.
- **Containment Lining** (optional): improves stability.

| Stage | Reactor Chamber | Recovery Filter | Containment Lining |
|---:|---|---|---|
| 1 | {{ item('rngtech:iron_reactor_chamber') }} | {{ item('rngtech:iron_recovery_filter') }} | {{ item('rngtech:iron_containment_lining') }} |
| 2 | {{ item('rngtech:copper_reactor_chamber') }} | {{ item('rngtech:copper_recovery_filter') }} | {{ item('rngtech:copper_containment_lining') }} |
| 3 | {{ item('rngtech:bronze_reactor_chamber') }} | {{ item('rngtech:bronze_recovery_filter') }} | {{ item('rngtech:bronze_containment_lining') }} |
| 4 | {{ item('rngtech:steel_reactor_chamber') }} | {{ item('rngtech:steel_recovery_filter') }} | {{ item('rngtech:steel_containment_lining') }} |

Recovery Filters also fit the [Component Recycler](component-recycler.md) (optional, up to the chassis stage), where they unlock filter-only outputs, and the Miner's Companion, where they unlock rare trace recovery.

### Disassembly Heads

A Disassembly Head sets the processing level, speed, and stability of a powered [Component Recycler](component-recycler.md). Recycling recipes need a head of at least their level.

| Disassembly Head | Stage |
|---|---:|
| {{ item('rngtech:iron_disassembly_head') }} | 1 |
| {{ item('rngtech:copper_disassembly_head') }} | 2 |
| {{ item('rngtech:bronze_disassembly_head') }} | 3 |
| {{ item('rngtech:steel_disassembly_head') }} | 4 |
| {{ item('rngtech:aluminum_disassembly_head') }} | 5 |
| {{ item('rngtech:titanium_disassembly_head') }} | 6 |
| {{ item('rngtech:tungstensteel_disassembly_head') }} | 7 |
| {{ item('rngtech:exotic_disassembly_head') }} | 8 |

**Used in:** [Component Recycler](component-recycler.md) (required, up to the chassis stage).

### Servos

Servos add processing speed, stability, temperature stability, and overheat tolerance. Stage 6 and higher Servos also soften the strain from short power drops.

| Servo | Stage | Notes |
|---|---:|---|
| {{ item('rngtech:steel_servo') }} | 4 | Needs no Lubricant to craft. |
| {{ item('rngtech:titanium_servo') }} | 6 | |
| {{ item('rngtech:tungstensteel_servo') }} | 7 | |
| {{ item('rngtech:nullite_servo') }} | 7 | Sidegrade: half processing speed, but **Auto Purge** lets fluid machines void overflowing output fluid instead of stopping. |
| {{ item('rngtech:exotic_servo') }} | 8 | |

Titanium and higher Servos need Lubricant and can also be made in the [Component Assembler](component-assembler.md).

**Used in:**

| Machine | Slot |
|---|---|
| [Metal Press](metal-press.md) | Required on the Metal Press, optional on the Crude Metal Press. Any stage. |
| [Alloy Furnace](alloy-furnace.md) | Optional, up to the chassis stage. |
| [Melter](melter.md) | Optional, any stage. |
| [Coal Gasifier](coal-gasifier.md), [Syngas Combustor](syngas-combustor.md), [Steam Methane Reformer](steam-methane-reformer.md) | Optional, Stage 4 or higher. |
| [Compressor Tank](compressor-tank.md) (Steel and higher) | Four slots, at least one required, up to the tank's stage. |
| [Cavitation Generator](cavitation-generator.md) | Optional, Stage 6 or higher. |

{{ crafting('rngtech:steel_servo') }}

### Fluid Pumps

Fluid Pumps move fluid out of, or into, a machine. Their transfer rate before affixes:

| Fluid Pump | Stage | Transfer |
|---|---:|---:|
| {{ item('rngtech:osmium_fluid_pump') }} | 5 | 125 mB/t |
| {{ item('rngtech:titanium_fluid_pump') }} | 6 | 250 mB/t |
| {{ item('rngtech:tungstensteel_fluid_pump') }} | 7 | 500 mB/t |
| {{ item('rngtech:exotic_fluid_pump') }} | 8 | 1,000 mB/t |

**Used in:** [Melter](melter.md) (required, lets it fill containers and pipe out fluid), [Corrosion Cell](corrosion-cell.md) (optional, lets it fill with Electrolyte Solution from the side), and the Forestry Companion cart (optional, used by the Field Hand sprinkler; see [Machine Mastery](machine-mastery.md#ascendancies)). Any stage fits.

{{ crafting('rngtech:osmium_fluid_pump') }}

### Cathodes

A Cathode sets the highest-stage Anode a [Corrosion Cell](corrosion-cell.md) can burn, and rolls generation, efficiency, speed, and stability affixes for the cell.

| Cathode | Stage |
|---|---:|
| {{ item('rngtech:aluminum_cathode') }} | 5 |
| {{ item('rngtech:titanium_cathode') }} | 6 |
| {{ item('rngtech:tungstensteel_cathode') }} | 7 |
| {{ item('rngtech:naquadah_cathode') }} | 8 |

**Used in:** [Corrosion Cell](corrosion-cell.md) (optional, needed to burn Anodes).

### Resonance Calibrator parts

The [Resonance Calibrator](resonance-calibrator.md) takes three part types, all from the same six materials:

- **Resonance Coil** (required): its stage sets which calibration recipes the machine can reach, and it adds calibration quality.
- **Control Board** (required): improves precision, stability, and Refinement Potential outcomes.
- **Stabilizer Matrix** (optional): raises the stability floor and improves catalyst efficiency.

| Stage | Resonance Coil | Control Board | Stabilizer Matrix |
|---:|---|---|---|
| 1 | {{ item('rngtech:iron_resonance_coil') }} | {{ item('rngtech:iron_control_board') }} | {{ item('rngtech:iron_stabilizer_matrix') }} |
| 2 | {{ item('rngtech:copper_resonance_coil') }} | {{ item('rngtech:copper_control_board') }} | {{ item('rngtech:copper_stabilizer_matrix') }} |
| 4 | {{ item('rngtech:steel_resonance_coil') }} | {{ item('rngtech:steel_control_board') }} | {{ item('rngtech:steel_stabilizer_matrix') }} |
| 6 | {{ item('rngtech:titanium_resonance_coil') }} | {{ item('rngtech:titanium_control_board') }} | {{ item('rngtech:titanium_stabilizer_matrix') }} |
| 7 | {{ item('rngtech:tungstensteel_resonance_coil') }} | {{ item('rngtech:tungstensteel_control_board') }} | {{ item('rngtech:tungstensteel_stabilizer_matrix') }} |
| 7 | {{ item('rngtech:nullite_resonance_coil') }} | {{ item('rngtech:nullite_control_board') }} | {{ item('rngtech:nullite_stabilizer_matrix') }} |

**Used in:** [Resonance Calibrator](resonance-calibrator.md), any stage.

{{ crafting('rngtech:iron_resonance_coil') }}

### Cavitation Rotors

The rotor drives a [Cavitation Generator](cavitation-generator.md). Its stage decides which recipes the generator can run, and it wears down as it works. A fully worn rotor is ejected as a Pitted Cavitation Rotor, which you can salvage in a Component Recycler.

| Cavitation Rotor | Stage | Notes |
|---|---:|---|
| {{ item('rngtech:steel_cavitation_rotor') }} | 5 | |
| {{ item('rngtech:titanium_cavitation_rotor') }} | 6 | |
| {{ item('rngtech:nitrogen_extraction_rotor') }} | 6 | Low power. With a Nitrogen Separation Nozzle, makes Nitrogen from water. |
| {{ item('rngtech:tungstensteel_cavitation_rotor') }} | 7 | |
| {{ item('rngtech:aethergold_cavitation_rotor') }} | 7 | Burst rotor: much more generation and speed, but wears out quickly. |
| {{ item('rngtech:nullite_cavitation_rotor') }} | 8 | |

**Used in:** [Cavitation Generator](cavitation-generator.md) (required).

{{ crafting('rngtech:steel_cavitation_rotor') }}

### Collapse Nozzles

Collapse Nozzles focus output and stability in the two collapse generators.

| Collapse Nozzle | Stage | Notes |
|---|---:|---|
| {{ item('rngtech:steel_collapse_nozzle') }} | 5 | |
| {{ item('rngtech:titanium_collapse_nozzle') }} | 6 | |
| {{ item('rngtech:nitrogen_separation_nozzle') }} | 6 | Cavitation Generator only. Needed for Nitrogen. |
| {{ item('rngtech:tungstensteel_collapse_nozzle') }} | 7 | |
| {{ item('rngtech:nullite_collapse_nozzle') }} | 8 | |
| {{ item('rngtech:exotic_collapse_nozzle') }} | 8 | |

**Used in:** [Cavitation Generator](cavitation-generator.md) (required) and [Vacuum Collapse Generator](vacuum-collapse-generator.md) (required). In the Vacuum Collapse Generator a nozzle below the recipe's stage still works but adds instability.

### Void Chambers and Dimensional Stabilizers

The [Vacuum Collapse Generator](vacuum-collapse-generator.md) needs both. The **Void Chamber**'s stage decides which recipes it can run and adds generation and stability. The **Dimensional Stabilizer** improves stability, efficiency, and residue handling.

| Stage | Void Chamber | Dimensional Stabilizer |
|---:|---|---|
| 7 | {{ item('rngtech:tungstensteel_void_chamber') }} | {{ item('rngtech:tungstensteel_dimensional_stabilizer') }} |
| 8 | {{ item('rngtech:nullite_void_chamber') }} | {{ item('rngtech:nullite_dimensional_stabilizer') }} |
| 8 | {{ item('rngtech:exotic_void_chamber') }} | {{ item('rngtech:exotic_dimensional_stabilizer') }} |

**Used in:** [Vacuum Collapse Generator](vacuum-collapse-generator.md) (both required).

### Catalyst Beds and Fuel Cell Membrane

These single-item parts gate the gas chemistry machines' recipes and tune their speed, efficiency, and stability.

| Part | Used in |
|---|---|
| {{ item('rngtech:ammonia_catalyst_bed') }} | [Ammonia Synthesizer](ammonia-synthesizer.md) (required) |
| {{ item('rngtech:reforming_catalyst_bed') }} | [Steam Methane Reformer](steam-methane-reformer.md) (required) |
| {{ item('rngtech:fuel_cell_membrane') }} | [Ammonia Fuel Cell](ammonia-fuel-cell.md) (required) |

{{ crafting('rngtech:fuel_cell_membrane') }}

### Battery Cells

Battery Cells store FE inside a machine. Most powered machines have an optional cell slot, and running without one usually costs speed or output. See [Battery Cells](battery-cells.md).

### Energy Connectors

Some machines take an Energy Connector module in a Gear slot to set how fast they export or accept FE: the [Cavitation Generator](cavitation-generator.md), [Vacuum Collapse Generator](vacuum-collapse-generator.md), Solar Array Controller ([Solar Panel](solar-panel.md)), [Tool Bench](tool-bench.md), and [Forestry Cart Station](forestry-cart-station.md). Any tier fits, from the {{ item('rngtech:crude_energy_connector') }} up to the {{ item('rngtech:exotic_energy_connector') }}. See [Universal Cable](universal-cable.md) for the tiers.

### Other Gear-tab items

A few machines store tools in their Gear tab rather than rolled parts:

- [Metal Press](metal-press.md): five Mold slots, with one active mold.
- [Resonance Calibrator](resonance-calibrator.md): six Calibration Pattern slots, with one active pattern.
- [Affix Forge](affix-forge.md): three upgrade slots.
- [Tool Bench](tool-bench.md): a {{ item('rngtech:tiny_anvil') }} slot.
- [Solar Panel](solar-panel.md) array controllers: a Solar Array Extender slot.

## See also

- [Battery Cells](battery-cells.md)
- [Stages](stages.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
