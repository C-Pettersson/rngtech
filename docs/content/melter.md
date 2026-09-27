# Melter

Status: Prototype


Resource id: `rngtech:melter`

## Summary

The Melter is a Stage 6 electric machine for fluid-backed processing. The default recipe family combines a lubricant base, a plant or organic additive, and water into `rngtech:lubricant`; a high-heat stone recipe can also produce vanilla lava. It also provides the bulk upgrade path for `rngtech:electrolyte_solution` after Sparksteel-era conductive calibration is available.

The Melter requires three Gear-tab components before it can process:

| Gear Slot | Required? | Runtime role |
|---|---|---|
| Heat Core | Yes | Provides the effective melt temperature from its component base profile. The default lubricant recipes require `1200` temperature, so Stage 6 Titanium heat is the first matching default path. |
| Crush Head | Yes | Provides base-profile `PROCESSING_LEVEL`; the default lubricant recipes require level `6`. |
| Battery Cell | No | Adds portable FE storage. Without a cell, the Melter keeps only its small working buffer and runs more slowly. |
| Servo | No | Adds processing-speed and control contribution from the installed Servo stack. |
| Fluid Pump | Yes | Required before processing. Enables output container filling and side fluid extraction, with transfer from the pump's base profile plus stored pump affixes. |

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Melter block | `rngtech:melter` |
| Recipe type | `rngtech:melter` |
| Lubricant fluid | `rngtech:lubricant`, `rngtech:flowing_lubricant` |
| Lubricant bucket | `rngtech:lubricant_bucket` |
| Electrolyte Solution fluid | `rngtech:electrolyte_solution`, `rngtech:flowing_electrolyte_solution` |
| Electrolyte Solution bucket | `rngtech:electrolyte_solution_bucket` |
| Lava output | `minecraft:lava` |
| Fluid Pumps | `rngtech:osmium_fluid_pump`, `rngtech:titanium_fluid_pump`, `rngtech:tungstensteel_fluid_pump`, `rngtech:exotic_fluid_pump` |

The placed machine uses `MelterBlockEntity`, `MelterMenu`, and `MelterScreen`. It supports Process, Gear, Stats, and Refinement tabs.

The Melter block recipe uses `rngtech:calibrated_shaped`. Its body keeps Titanium Plates, a Titanium Heat Core, a Titanium Crush Head, and an Advanced Machine Frame, then gates the conductive slot on `rngtech:calibrated_conductive_component` with family `conductive`, Stage `5+`, and `70+` stability. The default Stage 5 conductive source calibrates `rngtech:sparksteel_coil` with the Conductive Calibration Pattern, Matrix catalyst, and Stabilization Catalyst stabilizer.

The two item inputs match orderlessly. The default lubricant recipe consumes one item tagged `rngtech:melter/lubricant_bases`, one item tagged `rngtech:melter/lubricant_additives`, and `1000 mB minecraft:water`. The base tag currently includes `rngtech:resin`, `rngtech:rubber`, `minecraft:slime_ball`, and `minecraft:honey_bottle`; the additive tag includes `rngtech:plant_reagent` and `rngtech:organic_reagent`. It produces `1000 mB rngtech:lubricant`, requires an installed Fluid Pump, `1200` temperature, and processing level `6`, and takes `240` ticks and `4800 FE` before traits adjust timing and cost.

The default lava recipe consumes `minecraft:stone`, `minecraft:blaze_powder`, and `1000 mB minecraft:water`. It produces `1000 mB minecraft:lava`, requires `1200` temperature and processing level `6`, and takes `360` ticks and `9600 FE` before traits adjust timing and cost.

The default Electrolyte Solution recipe consumes `minecraft:sugar`, `minecraft:redstone`, and `1000 mB minecraft:water`. It produces `1000 mB rngtech:electrolyte_solution`, requires `1200` temperature and processing level `6`, and takes `220` ticks and `3600 FE` before traits adjust timing and cost. This is a bulk supply path for the [Component Assembler](battery-assembler.md), not the first required electrolyte source.

The default methane recipe consumes `rngtech:algae_biomass`, `rngtech:organic_reagent`, and `1000 mB minecraft:water`. It produces `1000 mB rngtech:methane`, requires `1400` temperature and processing level `6`, and takes `320` ticks and `8000 FE` before traits adjust timing and cost. Dense Algae Biomass is reserved for Bio Generator fuel instead of methane chemistry.

With JEI installed, Melter recipes are exposed with primary and secondary item inputs, fluid input, fluid output, FE cost, processing ticks, minimum heat, and required processing level. Melter recipe transfer is intentionally omitted until fluid-container transfer can represent the tank requirement.

## Fluids

The Melter has an input tank and an output tank. Water can be inserted through the block fluid capability or drained from buckets and compatible NeoForge fluid-handler items into the input tank. The output tank accepts fluids tagged `rngtech:melter_outputs`, currently lubricant, Electrolyte Solution, methane, and lava. If a Nullite Servo is installed, produced fluid that does not fit in a matching output tank is voided instead of blocking the next craft.

Without a Fluid Pump, the Melter will not process. With a Fluid Pump installed, the Melter can process into its internal output tank, side fluid capability exposes output-only extraction, and the Process tab can fill compatible output containers.

The Process tab has precise purge buttons for the input and output tanks. The craftable `rngtech:purge_bucket` can also right-click the placed Melter to void up to `1000 mB`; normal use prefers the input tank, while sneak-use prefers the output tank. Purging the input tank resets active Melter progress so a wrong partial fill cannot strand the recipe state.

No RNGTech fluid pipes are implemented. Automation uses standard NeoForge fluid capabilities.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert item recipe inputs. |
| Bottom | Extract output containers after they are filled. |
| Sides | Insert fluid containers, fill the input tank through the block fluid capability, and receive FE. With a Fluid Pump installed, sides also expose output-only fluid extraction. |

Gear slots and the Refinement catalyst slot are not exposed through sided item automation. FE received through the block energy capability fills the internal buffer first, then charges an installed Battery Cell when possible. Processing draws spend internal buffer FE first and then draw any remaining tick cost from the installed cell.

## Modifier Eligibility

Melter machine stacks and placed machines use the `MELTER` modifier eligibility profile. The profile can roll FE storage/input, item input, processing, heat, and fluid transfer modifiers that match the machine's implemented behavior.

Fluid Pumps are `FLUID_PUMP` machine parts for `MachineType.MELTER`. The Osmium Fluid Pump is the first pump tier at Stage 5 and gives the Melter its required fluid-output Gear. Each pump's authored base `FLUID_TRANSFER` value gates external extraction and output container filling rate.

## Related Pages

- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
