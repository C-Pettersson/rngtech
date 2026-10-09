# Component Assembler

Status: Prototype

Player guide: [Component Assembler](https://c-pettersson.github.io/rngtech/component-assembler/)

Resource id: `rngtech:battery_assembler`

Recipe type id: `rngtech:battery_assembly`

## Summary

The Component Assembler is a powered fluid-backed assembly machine for Primed Cell Cores, material Battery Cells, Corrosion Cell Anodes, and lubricant-backed endgame components. Its public ids keep the older Battery Assembler names for save and datapack compatibility.

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Component Assembler block | `rngtech:battery_assembler` |
| Recipe type | `rngtech:battery_assembly` |
| Electrolyte Solution fluid | `rngtech:electrolyte_solution`, `rngtech:flowing_electrolyte_solution` |
| Lubricant fluid | `rngtech:lubricant`, `rngtech:flowing_lubricant` |
| Electrolyte Solution bucket | `rngtech:electrolyte_solution_bucket` |
| Lubricant bucket | `rngtech:lubricant_bucket` |
| Stackable Primed Cell Core | `rngtech:battery_cell` |
| Lubricated Frame Coupling | `rngtech:lubricated_frame_coupling` |
| Corrosion Cell Anodes | `rngtech:aluminum_anode`, `rngtech:titanium_anode`, `rngtech:tungstensteel_anode`, `rngtech:naquadah_anode` |

The placed machine uses `BatteryAssemblerBlockEntity`, `BatteryAssemblerMenu`, and `BatteryAssemblerScreen`. It supports Process, Gear, Stats, and Refinement tabs.

## Processing

The Process tab has four item input slots, one fluid container or dry-electrolyte reagent slot, an internal input-fluid tank, and a four-slot output rack.

The core Primed Cell Core recipe is:

```text
rngtech:cell_shell + rngtech:conductive_plate + 250 mB rngtech:electrolyte_solution -> rngtech:battery_cell
```

`rngtech:battery_cell` has no FE capability, carries no RPG traits, and is not a refinement target. Final material cells such as `rngtech:iron_battery_cell` roll Battery Cell traits once when assembly completes.

Anode recipes cost `4,800 FE` each. See [Corrosion Cell](corrosion-cell.md).

Recipes may declare counted item inputs, so one Process input slot can supply several of the same item.

## Fluids

The input tank accepts fluids tagged as `rngtech:electrolytes` or `rngtech:lubricants`. Dry `rngtech:electrolyte` items dissolve at `125 mB` of Electrolyte Solution each and never create lubricant.

The dry electrolyte path keeps early battery production available before the Melter is reachable. The Melter provides later bulk sources for both Electrolyte Solution and Lubricant.

Purging the input tank by button or `rngtech:purge_bucket` resets active assembly progress so changing or clearing the fluid cannot leave an old recipe snapshot active.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert recipe item inputs. |
| Bottom | Extract finished outputs from the output rack. |
| Sides | Insert dry electrolyte or filled fluid containers, extract drained/non-input containers from the fluid slot, fill valid assembly fluids through the fluid capability, and receive FE. |

Gear slots and the Refinement catalyst slot are manual UI equipment. The Gear tab exposes one optional Battery Cell slot; received FE fills the internal buffer before the cell, and processing spends the buffer before drawing from the cell.

## Modifier Eligibility

Component Assembler machine stacks and placed machines use the compatibility profile id `BATTERY_ASSEMBLER`. The profile can roll FE storage/input, item input/output, processing, stability, and fluid-input transfer modifiers. It can also roll the Assembly work-speed suffix, Overclocked, Instant Process, Super Output for stackable outputs, and Bulk Speed behavior.

## Related Pages

- [Battery Cells](battery-cells.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
