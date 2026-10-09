# Melter

Status: Prototype

Player guide: [Melter](https://c-pettersson.github.io/rngtech/melter/)

Resource id: `rngtech:melter`

## Summary

The Melter is a Stage 6 electric machine that turns two items plus water into a fluid: Lubricant, bulk Electrolyte Solution, methane, or lava.

Required Gear is a Heat Core (melt temperature; the `1200` default gate makes Stage 6 Titanium heat the first matching path), a Crush Head (base-profile `PROCESSING_LEVEL`, hard-gated at the recipe level), and a Fluid Pump (output filling and side extraction rate). Fluid Capacity affixes on the pump, a flat Brimming prefix and a percent Depth suffix, size both Melter tanks. Battery Cell and Servo are optional.

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

The placed machine uses `MelterBlockEntity`, `MelterMenu`, and `MelterScreen`. It supports Process, Gear, Stats, Refinement, and Mastery tabs.

The Melter block recipe uses `rngtech:calibrated_shaped`. Its body keeps Titanium Plates, a Titanium Heat Core, a Titanium Crush Head, and an Advanced Machine Frame, then gates the conductive slot on `rngtech:calibrated_conductive_component` with family `conductive`, Stage `5+`, and `70+` stability. The default Stage 5 conductive source calibrates `rngtech:sparksteel_coil` with the Conductive Calibration Pattern, Matrix catalyst, and Stabilization Catalyst stabilizer.

The two item inputs match orderlessly. Every default recipe consumes `1000 mB minecraft:water`, produces `1000 mB` of fluid, and requires processing level `6`. Base values before traits:

| Recipe | Item inputs | Output | Heat | Ticks | FE | `machine_xp` |
|---|---|---|---:|---:|---:|---:|
| `lubricant` | `#rngtech:melter/lubricant_bases` + `#rngtech:melter/lubricant_additives` | `rngtech:lubricant` | `1200` | `240` | `4800` | `5` |
| `electrolyte_solution` | `minecraft:sugar` + `minecraft:redstone` | `rngtech:electrolyte_solution` | `1200` | `220` | `3600` | `4` |
| `methane_from_algae` | `rngtech:algae_biomass` + `rngtech:organic_reagent` | `rngtech:methane` | `1400` | `320` | `8000` | `6` |
| `lava` | `minecraft:stone` + `minecraft:blaze_powder` | `minecraft:lava` | `1200` | `360` | `9600` | none |

Electrolyte Solution here is a bulk supply path for the [Component Assembler](battery-assembler.md), not the first required electrolyte source. Balance intent: Dense Algae Biomass stays a Bio Generator fuel, so no Melter recipe accepts it.

With JEI installed, Melter recipes are exposed with primary and secondary item inputs, fluid input, fluid output, FE cost, processing ticks, minimum heat, required processing level, and machine XP when authored. Melter recipe transfer is intentionally omitted until fluid-container transfer can represent the tank requirement.

## Fluids

The Melter has an input tank and an output tank. The input tank fills through the block fluid capability or from NeoForge fluid-handler items. The output tank accepts fluids tagged `rngtech:melter_outputs`. An installed Nullite Servo voids produced fluid that does not fit instead of blocking the next craft. Without a Fluid Pump the Melter does not process; with one, the side fluid capability exposes output-only extraction.

Purging the input tank, through the Process tab buttons or `rngtech:purge_bucket`, resets active Melter progress so a wrong partial fill cannot strand the recipe state. Fluid automation uses standard NeoForge fluid capabilities.

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

## Mastery

The Melter enters the [shared Machine Mastery tree](../systems/machine-mastery.md) at the Reserve / Control start, whose home region covers insulation, regulation, thermal inertia, fluid handling, and catalysis. Base attributes are `10` Control, `0` Drive, and `10` Reserve. Besides the shared attribute conversions, each Reserve point grants `0.1%` increased Fluid Transfer. Effects the Melter cannot use, such as Stability, bonus output chance, and fuel duration, stay selectable but are marked inactive. Increased Maximum Temperature applies, but the maximum-temperature caps from Low Heat Specialist, Flash Annealing, and Regulated Heat do not, because those values sit below the `1200` minimum of every default recipe. The Melter is not a heated machine for [tagged payoffs](../systems/machine-mastery.md#tagged-payoffs), so it also does not gain their speed bonuses; untagged penalties such as Flash Annealing's Energy Usage still apply. It has no Super Output, so Single Pass does nothing on a Melter; ascendancy Fluid Yield is unaffected.

Recipes may set `machine_xp` and an optional `machine_xp_band`; the band defaults from `required_processing_level` on the same scale as Crusher recipes, so the default level `6` recipes train in band `81`. XP is granted only after a completed melt puts fluid into the output tank. Missing Gear, low heat or processing level, a full output tank, invalid recipes, and power-starved ticks grant none; a Nullite Servo melt whose whole output is voided also grants none. Allocation changes reset active melt progress and Bulk Speed while keeping inventory, tanks, and Gear. Breaking and pick-blocking the Melter preserve `rngtech:machine_progression`. Default XP values are in the recipe table above.

## Ascendancies

Status: Prototype

Melter machines choose between Pressure Vessel and Twin Crucible when they use their first Ascendancy Seal. [Machine Mastery](../systems/machine-mastery.md#ascendancies) defines Seals, points, refunds, and switching, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines the new stats. The tables below are generated from the ascendancy catalog.

- The Melter’s tanks start at 4,000 mB, and Pressurized Tanks raise them. Melter recipes carry `bonus_output`, and only eligible melts gain Fluid Yield, rounded down per melt.
- Sealed Lines stops excess output from being voided, even with an auto-purge servo, so a full tank pauses work and keeps progress.
- Twin Crucible’s Second Crucible and Triple Crucible raise [Batch Size](../reference/machine-stats.md), so one cycle melts several sets, each needing its own inputs, fluid, and tank room. Each extra melt adds Batch Overhead to the cycle time. Shared Heat makes each melt after the first 20% cheaper, and Fused Crucibles turns batching off for 20% more Processing Speed per point of Batch Size.
- Electrolyte Solution, Methane, and lava feed generators, so the loop audit bounds Melter yield at 1.6 times the recipe fluid.

<!-- ascendancy-trees:start -->

### Pressure Vessel

| Node | Type | After | Effect |
|---|---|---|---|
| **Pressurized Tanks** | Root | — | 200% increased Fluid Capacity; +10% Fluid Yield; 20% less Processing Speed. |
| Wide Valves | Small | Pressurized Tanks | 10% increased Fluid Transfer. |
| **Deep Intake** | Notable | Wide Valves | 100% increased Fluid Capacity. |
| Relief Valve | Small | Deep Intake | 10% increased Fluid Transfer. |
| **Overpressure** | Deep notable | Relief Valve | Melts run 30% faster while the output tank is over half full. |
| Tight Seals | Small | Pressurized Tanks | +3% Fluid Yield. |
| **Methane Trap** | Notable | Tight Seals | +25% Fluid Yield on methane from algae. |
| Closed Circuit | Small | Pressurized Tanks | +3% Fluid Yield. |
| **Brine Loop** | Notable | Closed Circuit | +25% Fluid Yield on Electrolyte Solution. |
| Slow Boil | Small | Brine Loop | +3% Fluid Yield. |
| **Autoclave** | Deep notable | Slow Boil | +15% Fluid Yield; 40% less Processing Speed. |
| Spare Charge | Small | Pressurized Tanks | 10% increased Energy Capacity. |
| **Sealed Lines** | Notable | Spare Charge | A full output tank pauses work and keeps progress, even with an auto-purge servo. |

### Twin Crucible

| Node | Type | After | Effect |
|---|---|---|---|
| **Second Crucible** | Root | — | +2 Batch Size; 15% more Energy Use. Each batched melt needs its own inputs, fluid, and tank room. |
| Steady Feed | Small | Second Crucible | 8% increased Processing Speed. |
| **Crush Feed** | Notable | Steady Feed | +10% Overlevel Speed. |
| Hot Walls | Small | Second Crucible | 8% increased Heat Transfer. |
| **Flash Point** | Notable | Hot Walls | 40% increased Heat Transfer. |
| Fused Walls | Small | Flash Point | 8% increased Heat Transfer. |
| **Fused Crucibles** | Deep notable | Fused Walls | Batching is off; 20% more Processing Speed per point of Batch Size. |
| Lean Burn | Small | Second Crucible | 5% reduced Energy Use. |
| **Shared Heat** | Notable | Lean Burn | Batched melts after the first use 20% less FE. |
| Banked Burn | Small | Shared Heat | 5% reduced Energy Use. |
| **Triple Crucible** | Deep notable | Banked Burn | +1 Batch Size. |
| Deep Heat | Small | Second Crucible | 4% increased Max Temperature. |
| **Lava Tap** | Notable | Deep Heat | The lava recipe runs 50% faster. |

<!-- ascendancy-trees:end -->

## Related Pages

- [Machine Mastery](../systems/machine-mastery.md)
- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
