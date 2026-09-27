# Component Assembler

Status: Prototype


Resource id: `rngtech:battery_assembler`

Recipe type id: `rngtech:battery_assembly`

## Summary

The Component Assembler is a powered fluid-backed assembly machine. Its public ids still use the older Battery Assembler names for save and datapack compatibility, but the player-facing machine is now the Component Assembler.

It has three current jobs:

- Produce stackable Primed Cell Cores from shells, conductive plates, and `rngtech:electrolyte_solution`.
- Assemble final material Battery Cells from Primed Cell Cores and material-specific battery ingredients.
- Assemble lubricant-backed endgame components from ordinary item ingredients and stored `rngtech:lubricant`.

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

The placed machine uses `BatteryAssemblerBlockEntity`, `BatteryAssemblerMenu`, and `BatteryAssemblerScreen`. It supports Process, Gear, Stats, and Refinement tabs.

## Processing

The Process tab has four item input slots, one fluid container or dry-electrolyte reagent slot, an internal input-fluid tank, and a four-slot output rack.

The core Primed Cell Core recipe is:

```text
rngtech:cell_shell + rngtech:conductive_plate + 250 mB rngtech:electrolyte_solution -> rngtech:battery_cell
```

`rngtech:battery_cell` is displayed as Primed Cell Core. It is a stackable inert intermediate, has no FE capability, carries no RPG traits, and is not a refinement target.

Final cell assembly recipes consume one Primed Cell Core plus material ingredients and redstone. Sparksteel and higher also consume `rngtech:stabilization_catalyst`. Final outputs use material Battery Cell ids such as `rngtech:iron_battery_cell`, stack to one, start at zero FE, and roll Battery Cell traits once when assembly completes. Potato, Iron, Copper, and Lead cells also keep direct dry-electrolyte crafting recipes for players who have not built a Component Assembler.

Lubricant-backed recipes consume stored `rngtech:lubricant` instead of a Lubricant Bucket. The current defaults assemble Stage 6+ Servos and `rngtech:lubricated_frame_coupling`, which feeds the Exotic Machine Frame crafting recipe.

Recipes may declare counted item inputs. For example, a Servo recipe can consume multiple redstone or ingots from one Process input slot while still using the same four-slot machine layout.

## Fluids

The Component Assembler accepts assembly fluids in three ways:

- Side fluid insertion through the block fluid capability.
- Compatible filled fluid containers in the Process fluid slot.
- Dry `rngtech:electrolyte` items in the Process fluid slot, dissolved at `125 mB` of Electrolyte Solution each.

The input tank accepts fluids tagged as `rngtech:electrolytes` or `rngtech:lubricants`. Dry electrolyte only creates Electrolyte Solution; it does not create lubricant.

The dry electrolyte path keeps early battery production available before the Melter is reachable. The Melter provides later bulk sources for both Electrolyte Solution and Lubricant.

The Process tab has a purge button for the assembly-fluid tank. The craftable `rngtech:purge_bucket` can also right-click the placed machine to void up to `1000 mB` of stored assembly fluid. Purging this input tank resets active assembly progress so changing or clearing the fluid cannot leave an old recipe snapshot active.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert recipe item inputs. |
| Bottom | Extract finished outputs from the output rack. |
| Sides | Insert dry electrolyte or filled fluid containers, extract drained/non-input containers from the fluid slot, fill valid assembly fluids through the fluid capability, and receive FE. |

Gear slots and the Refinement catalyst slot are manual UI equipment. The Gear tab exposes one optional Battery Cell slot. FE received through the block energy capability fills the internal buffer first, then charges the installed Battery Cell when possible; processing draws spend internal buffer FE first and then draw any remaining tick cost from the installed cell.

## Modifier Eligibility

Component Assembler machine stacks and placed machines use the compatibility profile id `BATTERY_ASSEMBLER`. The profile can roll FE storage/input, item input/output, processing, stability, and fluid-input transfer modifiers. It can also roll the Assembly work-speed suffix, Overclocked, Instant Process, Super Output for stackable outputs, and Bulk Speed behavior.

## Related Pages

- [Battery Cells](battery-cells.md)
- [Machine Parts](machine-parts.md)
- [Melter](melter.md)
- [Battery Chassis](battery-chassis.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
