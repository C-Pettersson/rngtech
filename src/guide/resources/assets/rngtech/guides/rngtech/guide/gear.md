---
navigation:
  title: "Gear"
  parent: guides.md
  icon: rngtech:iron_heat_core
categories:
  - "Guides"
item_ids:
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

Every Gear part is its own item with its own [rarity and affixes](rngtech:rarity-and-affixes.md), so two Iron Heat Cores can perform differently.

## Part families

- [Crush Heads](rngtech:gear/crush-heads.md)
- [Heat Cores](rngtech:gear/heat-cores.md)
- [Alloy Crucibles](rngtech:gear/alloy-crucibles.md)

### Fuel Boxes

The Fuel Box sets a burner's fuel slots, which fuel forms it burns, and fuel behaviors such as the Fuel Governor.

| Fuel Box | Stage | Fuel slots | Fuel forms |
|---|---:|---:|---|
| <ItemLink id="rngtech:iron_fuel_box" /> | 1 | 1 | Items |
| <ItemLink id="rngtech:copper_fuel_box" /> | 2 | 1 | Items |
| <ItemLink id="rngtech:bronze_fuel_box" /> | 3 | 2 | Items |
| <ItemLink id="rngtech:steel_fuel_box" /> | 4 | 2 | Items and compact fuel blocks |

**Used in:** [Solid Fuel Burner](rngtech:solid-fuel-burner.md) (required, up to the burner's stage). That page explains each box's behaviors.

### Bio Chamber

The <ItemLink id="rngtech:bio_chamber" /> adds power, fuel duration, and fuel efficiency to bio fuels. Its affixes can favor particular crops or prepared biomass fuels.

**Used in:** [Bio Generator](rngtech:bio-generator.md) (required) and [Algae Photobioreactor](rngtech:algae-photobioreactor.md) (required), where it scales algae output.

- [Potential Reactor parts](rngtech:gear/potential-reactor-parts.md)
- [Disassembly Heads](rngtech:gear/disassembly-heads.md)
- [Servos](rngtech:gear/servos.md)
- [Fluid Pumps](rngtech:gear/fluid-pumps.md)

### Cathodes

A Cathode sets the highest-stage Anode a [Corrosion Cell](rngtech:corrosion-cell.md) can burn, and rolls generation, efficiency, speed, and stability affixes for the cell.

| Cathode | Stage |
|---|---:|
| <ItemLink id="rngtech:aluminum_cathode" /> | 5 |
| <ItemLink id="rngtech:titanium_cathode" /> | 6 |
| <ItemLink id="rngtech:tungstensteel_cathode" /> | 7 |
| <ItemLink id="rngtech:naquadah_cathode" /> | 8 |

**Used in:** [Corrosion Cell](rngtech:corrosion-cell.md) (optional, needed to burn Anodes).

- [Resonance Calibrator parts](rngtech:gear/resonance-calibrator-parts.md)
- [Cavitation Rotors](rngtech:gear/cavitation-rotors.md)
- [Collapse Nozzles](rngtech:gear/collapse-nozzles.md)
- [Void Chambers and Dimensional Stabilizers](rngtech:gear/void-chambers-and-dimensional-stabilizers.md)
- [Catalyst Beds and Fuel Cell Membrane](rngtech:gear/catalyst-beds-and-fuel-cell-membrane.md)

### Battery Cells

Battery Cells store FE inside a machine. Most powered machines have an optional cell slot, and running without one usually costs speed or output. See [Battery Cells](rngtech:battery-cells.md).

- [Energy Connectors](rngtech:gear/energy-connectors.md)

### Other Gear-tab items

A few machines store tools in their Gear tab rather than rolled parts:

- [Metal Press](rngtech:metal-press.md): five Mold slots, with one active mold.
- [Resonance Calibrator](rngtech:resonance-calibrator.md): six Calibration Pattern slots, with one active pattern.
- [Affix Forge](rngtech:affix-forge.md): three upgrade slots.
- [Tool Bench](rngtech:tool-bench.md): a <ItemLink id="rngtech:tiny_anvil" /> slot.
- [Solar Panel](rngtech:solar-panel.md) array controllers: a Solar Array Extender slot.

## More

- [The Gear tab](rngtech:gear/the-gear-tab.md)
- [Stage caps](rngtech:gear/stage-caps.md)
- [Rolls and refinement](rngtech:gear/rolls-and-refinement.md)
