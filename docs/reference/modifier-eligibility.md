# Modifier Eligibility

This table tracks which categories can receive modifiers and which current profiles expose named affix candidates.

Eligibility is represented by static code profiles. A profile has target capability flags, named modifier definitions, and optional rollable behavior definitions. Modifier definitions use `affix_id`, `mod_group`, slot, family roll weight, tier ranges, optional tier weights, and target effects; the `CAN_ROLL` flag separates rollable affixes from authored fixed modifiers.

Modifier family weights, tier selection, and value ranges are documented in [Affix Generation](../systems/affix-generation.md). The grouped modifier inventory starts at [Modifiers](../modifiers/index.md).

## Capability Flags

| Flag | Meaning |
|---|---|
| `HAS_FE_STORAGE` | Stores FE or modifies an FE buffer. |
| `HAS_FE_INPUT` | Receives FE or modifies receive rate. |
| `HAS_FE_OUTPUT` | Outputs FE or modifies extraction rate. |
| `HAS_FE_GENERATION` | Generates FE. |
| `HAS_FUEL_SLOT` | Consumes burnable fuel or owns fuel inventory. |
| `HAS_INPUT_SLOT` | Consumes item inputs. |
| `HAS_OUTPUT_SLOT` | Produces item outputs. |
| `HAS_PROCESSING` | Runs recipe or work-cycle processing. |
| `HAS_HEAT` | Has heat-transfer, insulation, or temperature behavior. |
| `HAS_BATTERY_CELLS` | Stores or coordinates battery-cell stacks. |
| `HAS_BURST_TRANSFER` | Supports burst transfer stats. |
| `HAS_IDLE_LOSS` | Supports passive FE loss or loss reduction. |
| `HAS_GLOBAL_MODIFIER_EFFECTS` | Applies chassis effects to contained components. |
| `HAS_FLUID_STORAGE` | Stores fluids in an internal tank. |
| `HAS_FLUID_INPUT` | Accepts fluid input or fluid-handler container draining. |
| `HAS_FLUID_OUTPUT` | Outputs fluids through tanks, containers, or fluid capabilities. |
| `HAS_STABILITY` | Has stability as active behavior/stat. |
| `HAS_GEAR_STATS` | Is a component whose modifiers contribute through a gear slot. |
| `HAS_CALIBRATION` | Has calibration quality, precision, or RP outcome behavior. |
| `HAS_CATALYST_CONTROL` | Has catalyst preservation or catalyst-efficiency behavior. |
| `HAS_DURABILITY` | Has item-stack durability or wear behavior that can roll flat and percent durability affixes. |
| `HAS_FIELD_TOOL` | Applies to modular field tool heads, rods, and assembled tool stacks. |
| `HAS_SOLAR_ARRAY` | Has Solar Array Controller-only panel selection, low-light, and overflow behavior. |
| `HAS_SOLAR_PANEL` | Has direct daylight Solar Panel generation behavior. |
| `HAS_CRUSHER_SPECIALS` | Has Crusher-only prefix stats for output-guard grace, no-cell retention, high-hardness mitigation, top input filtering, Batch Size additions, and salvage chance. Used by Crusher blocks and Crush Head special prefixes. |
| `HAS_MINERS_COMPANION` | Has portable Miner's Companion filter capacity behavior. |

## Modifier Definition Flags

| Flag | Meaning |
|---|---|
| `CAN_ROLL` | The modifier definition can be selected by crafting rolls, Affix Injector, Ascension Matrix, and Debug Reroller after profile, slot, group, and operation-specific RP filters. Legal families are selected by weight, not as a uniform pool. |

Definitions without `CAN_ROLL` are authored fixed modifiers. Current machine, chassis, component, and Battery Cell numeric identity lives in authored base stat profiles before modifiers are applied. Component and Battery Cell profiles are resolved through `ComponentBaseStatCatalog`; machine and chassis profiles are resolved through `MachineBaseStatCatalog`. `MachineImplicitCatalog` keeps fixed behavior identity and nonnumeric item identity. Random rolling and refinement do not select or remove fixed behaviors or base stat profiles.

## Modifier Groups

Rollable affixes are mutually exclusive by `mod_group`. Initial rolls, Affix Forge additions, targeted adds, transmutation, Chaos Crystal rerolls, Debug Reroller, and Expansion Crystal slot filling all avoid adding a rolled affix from a group already present on the target. After those legality checks, candidate families are selected by roll weight. The current `processing_speed` group includes the generic processing-speed affix, processing-specific speed names such as Crushing or Smelting, the Overclocked hybrid, and the Bulk Speed behavior.

Profiles are validated at startup against their declared capability flags. They are not padded with duplicate named variants, so narrow profiles may expose fewer than three legal prefix or suffix families.

## Behavior Flags

Behavior flags are stored separately from numeric modifiers. Most are fixed identity traits, while `BULK_SPEED` is a code-backed rollable behavior on eligible processing-machine profiles. Balance Mode is a Battery Chassis prefix affix that enables `CHARGE_BALANCER` behavior. Rollable behavior definitions participate in the same weighted family selection as numeric affixes.

| Behavior | Current implementation |
|---|---|
| `FUEL_GOVERNOR` | Copper, Bronze, and Steel Fuel Boxes pause fuel burn while the installed Battery Cell is full. |
| `QUICK_FEED` | Copper solid fuel burner and Copper Heat/Fuel Box identity. Fuel Governor still controls full-buffer pause when a governor-capable Fuel Box is installed. |
| `FUEL_RESERVE` | Alloy solid fuel burner and Bronze Fuel Box keep the final accepted fuel item untouched. |
| `BLOCK_FEED` | Steel Fuel Box enables compact block fuels when fuel tier tags allow them. |
| `ALLOY_BLEND` | Bronze Furnace gets conditional speed only for inputs tagged `rngtech:alloy_blend_smeltables`. |
| `CELL_LEAKAGE_DAMPING` | Steel Battery Chassis subtracts a flat installed-cell idle-loss amount and clamps at zero; Aethergold Battery Chassis seals installed-cell idle loss. |
| `CHARGE_BALANCER` | Sparksteel+ Battery Chassis variants, including Aethergold, have fixed balancing identity, and the Balance Mode Battery Chassis prefix can enable the same behavior. Balanced chassis fill and drain eligible installed cells evenly instead of using one slot first. |
| `BURST_RELEASE` | Burst-capable Battery Chassis variants may output above steady transfer while burst ticks remain. |
| `DENSE_PARALLEL` | Retired. Batching needs no behavior; stored traits that name it still load and do nothing. |
| `BULK_SPEED` | Rollable on current processing-machine block profiles at family weight `25`. Each completed process adds `1%` processing speed, up to `100%`; powered machines draw higher FE/t as processing time falls. It uses the `processing_speed` modifier group. |
| `OUTPUT_GUARD` | Processing identities that carry it avoid work when output cannot be accepted. The Crusher Frame prefix enables the same behavior and adds a finite blocked-output progress grace window. |
| `THERMAL_BUFFER` | Steel+ Heat Cores and high-control thermal identities expose heat stability behavior hooks. |
| `POWER_GRACE` | Rollable on powered heat-control profiles and still present on Titanium+ Servo identity. It halves Furnace, Alloy Furnace, and Metal Press sensitive power-drop strain. |
| `RESIDUE_SCREEN` | Recovery Filters enable Potential Reactor residue output. |
| `SIDE_FLUID_OUTPUT` | Fluid Pumps enable Melter output container filling and side fluid extraction. Fluid Pumps also enable Corrosion Cell side Electrolyte Solution filling through the pump slot. |
| `AUTO_PURGE` | Nullite Servo fixed identity lets Servo-equipped fluid-output machines void produced fluid that cannot fit in a matching output tank. It is not a rolled modifier. |

The default public data tags Bronze Blend and Steel Blend with `rngtech:alloy_blend_smeltables`. Packs can add their own mixed-metal precursor items to the same tag.

## Capability Modifier Matrix

The code-backed source for this matrix is `ModifierEligibilityProfiles.definitionsForCapability(...)`. Profiles select from these capability-backed definitions; a capability can expose more candidates than a specific target currently chooses to roll.

| Capability Flag | Attached modifier definitions |
|---|---|
| `HAS_FE_STORAGE` | Flat machine `ADD ENERGY_CAPACITY_FLAT` prefix, flat Battery Cell `ADD ENERGY_CAPACITY_FLAT` prefix, percent `ENERGY_CAPACITY` prefix. |
| `HAS_FE_INPUT` | FE input and recipe energy behavior. Profile-specific rolls are authoritative: ordinary processors use `ENERGY_USAGE` but no longer roll user-facing `ENERGY_TRANSFER`; storage and continuous FE systems may still roll transfer. |
| `HAS_FE_OUTPUT` | FE output behavior. Storage-like and legacy generator profiles may roll `ENERGY_TRANSFER`; solid fuel-burning generators now expose supply and connector limits instead. |
| `HAS_FE_GENERATION` | Flat `ADD ENERGY_GENERATION` prefix, percent `ENERGY_GENERATION` suffix, and `EFFICIENCY` prefix. |
| `HAS_FUEL_SLOT` | `INPUT_SLOTS` fuel-slot suffix, `FUEL_EFFICIENCY` prefix, `FUEL_DURATION` suffix, `EFFICIENCY` prefix. |
| `HAS_INPUT_SLOT` | `INPUT_SLOTS` suffix. Current concrete uses include fuel-slot count on solid fuel-burning content and Lead Furnace's authored multi-lane input count. |
| `HAS_OUTPUT_SLOT` | `OUTPUT_AMOUNT` and `SUPER_OUTPUT_CHANCE` suffix candidates when the profile produces stackable item outputs. Fluid-output utility profiles may also use `OUTPUT_AMOUNT` without enabling Super Output. |
| `HAS_PROCESSING` | `PROCESSING_SPEED`, `EFFICIENCY`, and `INSTANT_PROCESS_CHANCE` suffix candidates, plus processing behavior eligibility such as `BULK_SPEED` when the profile opts in. Generator-style processing profiles can opt into speed without instant or super-output outcomes. |
| `HAS_HEAT` | `HEAT_TRANSFER` prefix, percentage and flat `MAX_TEMPERATURE` prefixes, `HEAT_ISOLATION` prefix, reduced `WARMUP_TIME` prefix, reduced `COOLING_RATE` prefix, `TEMPERATURE_STABILITY` suffix, and `OVERHEAT_TOLERANCE` suffix. |
| `HAS_BATTERY_CELLS` | Battery Chassis Additional Battery Slots prefix and Balance Mode prefix. |
| `HAS_BURST_TRANSFER` | `BURST_TRANSFER` suffix, `BURST_DURATION` suffix. |
| `HAS_IDLE_LOSS` | `IDLE_LOSS` suffix. |
| `HAS_GLOBAL_MODIFIER_EFFECTS` | `GLOBAL_MODIFIER_STRENGTH` suffix. |
| `HAS_FLUID_STORAGE` | None currently. |
| `HAS_FLUID_INPUT` | `FLUID_TRANSFER` suffix. |
| `HAS_FLUID_OUTPUT` | `FLUID_TRANSFER` suffix. |
| `HAS_STABILITY` | `STABILITY` suffix. |
| `HAS_CALIBRATION` | `CALIBRATION_QUALITY` prefix, `CALIBRATION_PRECISION` prefix, and `REFINEMENT_POTENTIAL_BONUS` suffix. |
| `HAS_CATALYST_CONTROL` | `CATALYST_EFFICIENCY` suffix. |
| `HAS_DURABILITY` | Flat `ADD DURABILITY` prefix and percent `INCREASED_PERCENT DURABILITY` prefix. |
| `HAS_GEAR_STATS` | Marker for source-local component profiles. Numeric component identity comes from `ComponentBaseStatCatalog`; profile rows below list the rolled affixes that can scale or adjust that base. This capability also anchors Bio Chamber ingredient-power affixes. |
| `HAS_FIELD_TOOL` | Self Repair, mining speed, attack speed, FE usage, FE transfer, stability, control, Battery Support, Luck, Treefeller limit, Pick Head-only Vein Miner limit/FE, and Ore Burst speed candidates. Mining level remains authored Tool Head identity plus stored head upgrades such as Diamond Tip. Durability lives under `HAS_DURABILITY`. |
| `HAS_SOLAR_PANEL` | Peak Solar suffix for zenith-window panel generation. |
| `HAS_CRUSHER_SPECIALS` | Crusher Frame, Crusher Throughput, Crusher Battery Link, Crusher Feed Control, Crusher Compression, and Crusher Salvage prefix families. Crush Head profiles use Crusher-special stats through Jagged, Scuffed, and Dust-Grooved prefix families. |
| `HAS_MINERS_COMPANION` | Miner's Companion filter slot prefix and energy usage suffix. |

## Current Code Profiles

The rollable modifiers column lists active stat families and special named affixes. Profiles do not add filler stats or duplicate named variants that the host ignores. `Energy generation` means both the flat prefix and percent suffix unless a row explicitly says otherwise.

| Profile | Capability Flags | Rollable modifiers |
|---|---|---|
| Crusher block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_CRUSHER_SPECIALS` | Crusher Frame, Crusher Kinetics, Crusher Jaws, Crusher Ore Handling, Crusher Battery Link, Crusher Feed Control, Crusher Compression, Crusher Vibration, Crusher Throughput, Crusher Salvage, energy usage, output amount, processing speed, Crushing, Instant Process, Super Output, Overclocked, and Bulk Speed behavior. |
| Furnace block | `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_FUEL_SLOT`, `HAS_HEAT` | Heat transfer, max temperature, heat isolation, reduced warmup time, reduced cooling rate, efficiency, fuel efficiency, input slots, processing speed, temperature stability, overheat tolerance, Smelting, Instant Process, and Super Output. |
| Electric Furnace block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_HEAT` | Flat energy capacity, increased energy capacity, energy usage, heat transfer, max temperature, heat isolation, reduced warmup time, reduced cooling rate, input slots, processing speed, temperature stability, overheat tolerance, Smelting, Instant Process, Super Output, Overclocked, Bulk Speed behavior, and Power Grace behavior. |
| Alloy Furnace block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy usage, heat transfer, max temperature, heat isolation, reduced warmup time, reduced cooling rate, processing speed, stability, temperature stability, overheat tolerance, Alloying, Instant Process, Super Output, Overclocked, Bulk Speed behavior, and Power Grace behavior. Active input slots come from Alloy Crucible base profiles, not rolled machine affixes. |
| Battery Cell | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_FE_OUTPUT`, `HAS_IDLE_LOSS` | Battery flat energy capacity, increased energy capacity, energy transfer, efficiency, idle loss. |
| Unique Battery Cell | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_FE_OUTPUT`, `HAS_IDLE_LOSS` | None. Unique identity is authored through its base profile, fixed item identity, and `UNIQUE` rarity. |
| Battery Chassis | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_FE_OUTPUT`, `HAS_BATTERY_CELLS`, `HAS_BURST_TRANSFER`, `HAS_IDLE_LOSS`, `HAS_GLOBAL_MODIFIER_EFFECTS`, `HAS_STABILITY` | Increased energy capacity, Charged Storage, Balance Mode, Additional Battery Slots, efficiency, energy transfer, burst transfer, burst duration, stability, idle loss, global modifier strength. |
| Miner's Companion | `HAS_MINERS_COMPANION` | Miner's Companion filter-slot prefixes and energy usage. Active filter slots start at `3` and are capped at ten by the item GUI. |
| Forestry Companion | `HAS_FE_INPUT`, `HAS_PROCESSING` | Processing speed and energy usage. Processing speed adjusts the placed cart's work interval, and energy usage scales movement, scan, plant, and cut FE costs. |
| Component Assembler block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT` | Uses compatibility profile id `BATTERY_ASSEMBLER`. Flat energy capacity, increased energy capacity, energy transfer, energy usage, processing speed, fluid transfer, stability, Assembly, Instant Process, Super Output, Overclocked, and Bulk Speed behavior. |
| Solid Fuel Burner block | `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_FUEL_SLOT`, `HAS_HEAT`, `HAS_STABILITY` | Efficiency, fuel efficiency, fuel duration, fuel slots, energy generation, stability. |
| Bio Generator block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_FUEL_SLOT`, `HAS_INPUT_SLOT` | Flat energy capacity, increased energy capacity, efficiency, fuel efficiency, energy generation, and energy transfer. |
| Solar Panel block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_SOLAR_PANEL` | Flat energy capacity, increased energy capacity, efficiency, energy generation, energy transfer, and Peak Solar. |
| Solar Array Controller block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_BATTERY_CELLS`, `HAS_STABILITY`, `HAS_SOLAR_ARRAY` | Flat energy capacity, increased energy capacity, efficiency, energy generation, stability, Array Expansion range, Moonlit Conversion, Cloud Piercer, Lunar Inverter, Clear-Sky Amplifier, Panel Synchronizer, and Panel Arbitration. Energy transfer is fixed by the installed Energy Connector when present. |
| Potential Reactor block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy generation, energy transfer, efficiency, processing speed, stability, Reacting, and Bulk Speed behavior. Generator-style profile does not roll Instant Process or Super Output. |
| Corrosion Cell block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy generation, energy transfer, fluid transfer, efficiency, processing speed, stability, Corroding, and Bulk Speed behavior. Generator-style profile does not roll Instant Process or Super Output. |
| Cavitation Generator block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT`, `HAS_OUTPUT_SLOT` | Flat energy capacity, increased energy capacity, energy generation, energy transfer, efficiency, processing speed, stability, temperature stability, output amount, fluid transfer, and Cavitating. Generator-style profile does not roll Instant Process or Super Output. |
| Ammonia Synthesizer block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT` | Flat energy capacity, increased energy capacity, energy transfer, energy usage, processing speed, efficiency, stability, fluid transfer, Synthesizing, Overclocked, and Bulk Speed behavior. Fluid-output processor profile does not roll Super Output. |
| Ammonia Fuel Cell block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT` | Flat energy capacity, increased energy capacity, energy generation, energy transfer, processing speed, efficiency, stability, fluid transfer, and Ammonia. Generator-style profile does not roll Instant Process or Super Output. |
| Vacuum Collapse Generator block | `HAS_FE_STORAGE`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy generation, energy transfer, efficiency, processing speed, stability, and Collapsing. Generator-style profile does not roll Instant Process or Super Output. |
| Powered Component Recycler block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy transfer, energy usage, processing speed, stability, Recycling, Instant Process, Overclocked, and Bulk Speed behavior. Super Output is excluded because recycling returns the source craft's ingredients. Crude Recycler is manual and does not roll this profile. |
| Metal Press block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY` | Flat energy capacity, increased energy capacity, energy transfer, energy usage, heat transfer, max temperature, reduced warmup time, reduced cooling rate, processing speed, stability, temperature stability, overheat tolerance, Pressing, Instant Process, Super Output, Overclocked, Bulk Speed behavior, and Power Grace behavior. |
| Melter block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT` | Flat energy capacity, increased energy capacity, energy transfer, energy usage, heat transfer, max temperature, processing speed, fluid transfer, Melting, Instant Process, Overclocked, and Bulk Speed behavior. Fluid-output profile does not roll Super Output. |
| Fluid Tank block | `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT` | Applies to Iron, Copper, and Bronze Fluid Tanks. These plain tanks have authored fluid capacity and no rollable compression or FE affixes. |
| Compressor Tank block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_FLUID_STORAGE`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT`, `HAS_PROCESSING`, `HAS_STABILITY` | Applies to Steel+ Compressor Tanks. Flat energy capacity, increased energy capacity, energy transfer, energy usage, processing speed, fluid transfer, and stability. `FLUID_CAPACITY` and `COMPRESSION_RATIO` are authored/display stats, not rollable affixes. This continuous storage profile does not roll Instant Process, Super Output, Overclocked, or Bulk Speed in v1. |
| Resonance Calibrator block | `HAS_FE_STORAGE`, `HAS_FE_INPUT`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_CALIBRATION`, `HAS_CATALYST_CONTROL` | Flat energy capacity, increased energy capacity, energy transfer, energy usage, processing speed, stability, calibration quality, calibration precision, catalyst efficiency, RP bonus, Calibrating, Instant Process, Super Output, Overclocked, and Bulk Speed behavior. |
| Crush Head | `HAS_GEAR_STATS`, `HAS_PROCESSING`, `HAS_OUTPUT_SLOT`, `HAS_CRUSHER_SPECIALS` | Pulverizing, Jagged, Kinetic, Scuffed, and Dust-Grooved prefixes; processing speed, output amount, Crushing, Instant Process, and Super Output. Processing level is a base-profile hard gate. |
| Heat Core | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_FUEL_SLOT`, `HAS_HEAT` | Fuel efficiency, heat isolation, heat transfer, max temperature, reduced warmup time, reduced cooling rate, temperature stability, overheat tolerance, energy generation, and Power Grace behavior. Fuel tier remains a hard identity gate. |
| Alloy Crucible | `HAS_GEAR_STATS`, `HAS_INPUT_SLOT`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY` | Processing speed, heat transfer, heat isolation, reduced warmup time, reduced cooling rate, temperature stability, overheat tolerance, stability, Alloying, Instant Process, Super Output, and Power Grace behavior. Input slot count is authored in `ComponentBaseStatCatalog` and remains a base-profile hard gate. |
| Servo | `HAS_GEAR_STATS`, `HAS_FE_INPUT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY`, `HAS_FLUID_OUTPUT` | Processing speed, energy usage, heat transfer, heat isolation, reduced warmup time, reduced cooling rate, stability, temperature stability, overheat tolerance, fluid transfer, Serving, Instant Process, Overclocked, and Power Grace behavior. Nullite Servo carries fixed Auto Purge identity. Servos do not roll generic output amount. |
| Fluid Pump | `HAS_GEAR_STATS`, `HAS_FLUID_INPUT`, `HAS_FLUID_OUTPUT` | Fluid transfer. Side fluid output remains fixed behavior identity; Corrosion Cell side filling uses the same pump transfer stat. |
| Cathode | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY` | Flat and percent energy generation, efficiency, processing speed, stability, and Electroplating. Generation rolls stay local to the Cathode. |
| Solar Array Extender | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_SOLAR_ARRAY` | Energy generation, array range, Moonlit Conversion, Cloud Piercer, and Clear-Sky Amplifier. Sparksteel and Aethergold variants have fixed day/night implicit identity. |
| Resonance Coil | `HAS_GEAR_STATS`, `HAS_FE_INPUT`, `HAS_PROCESSING`, `HAS_CALIBRATION` | Processing speed, energy transfer, energy usage, calibration quality, Calibrating, Instant Process, and Overclocked. Processing level is a base-profile hard gate. |
| Control Board | `HAS_GEAR_STATS`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_CALIBRATION` | Calibration precision, stability, and RP bonus. |
| Stabilizer Matrix | `HAS_GEAR_STATS`, `HAS_FE_INPUT`, `HAS_STABILITY`, `HAS_CALIBRATION`, `HAS_CATALYST_CONTROL` | Calibration quality, stability, catalyst efficiency, and energy usage. |
| Fuel Box | `HAS_GEAR_STATS`, `HAS_FUEL_SLOT`, `HAS_INPUT_SLOT`, `HAS_STABILITY` | Fuel efficiency, fuel duration, and stability. Fuel slot count and supported fuel forms remain base-profile identity. |
| Bio Chamber | `HAS_GEAR_STATS`, `HAS_FUEL_SLOT`, `HAS_FE_GENERATION` | Generic Bio Generator power and fuel-duration suffixes; fuel efficiency; crop-family prefixes for potato, carrot, bread, sapling, and seed fuels; and prepared-fuel suffixes for plant biomass, Organic Reagent, Composted Biomass, Algae Biomass / Dense Algae Biomass, and Rich Biomass. Fuel efficiency also acts as Bio Conversion when installed in the Algae Photobioreactor. |
| Reactor Chamber | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY` | Energy generation, processing speed, stability, and Reacting. Processing level is a base-profile hard gate. |
| Recovery Filter | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_PROCESSING` | Recovery efficiency, processing speed, and Screening. |
| Containment Lining | `HAS_GEAR_STATS`, `HAS_STABILITY` | Stability. |
| Ammonia Catalyst Bed | `HAS_GEAR_STATS`, `HAS_FE_INPUT`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_FLUID_OUTPUT` | Energy usage, processing speed, efficiency, stability, fluid transfer, Catalyzing, Instant Process, and Overclocked. |
| Fuel Cell Membrane | `HAS_GEAR_STATS`, `HAS_FE_OUTPUT`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_FLUID_INPUT` | Energy generation, energy transfer, processing speed, efficiency, stability, fluid transfer, and Membrane. |
| Cavitation Rotor | `HAS_GEAR_STATS`, `HAS_DURABILITY`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY`, `HAS_OUTPUT_SLOT` | Flat durability, increased durability, energy generation, processing speed, output amount, stability, and Cavitating. Processing level is a base-profile hard gate. Rotor wear is stored on the rotor stack. |
| Void Chamber | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY` | Energy generation, processing speed, stability, and Containing. Processing level is a base-profile hard gate. |
| Collapse Nozzle | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_FE_OUTPUT`, `HAS_PROCESSING`, `HAS_HEAT`, `HAS_STABILITY`, `HAS_FLUID_OUTPUT`, `HAS_OUTPUT_SLOT` | Shared ordinary nozzles for Cavitation and Vacuum Collapse plus Cavitation-only Nitrogen Separation Nozzle. Energy generation, energy transfer, temperature stability, output amount, fluid transfer, stability, and Focusing. |
| Dimensional Stabilizer | `HAS_GEAR_STATS`, `HAS_FE_GENERATION`, `HAS_PROCESSING`, `HAS_STABILITY` | Efficiency, energy generation, processing speed, stability, and Stabilizing. |
| Disassembly Head | `HAS_GEAR_STATS`, `HAS_OUTPUT_SLOT`, `HAS_PROCESSING`, `HAS_STABILITY` | Processing speed, stability, Disassembling, and Instant Process. Super Output is excluded like the Component Recycler profile. Processing level is a base-profile hard gate. |
| Tool Head | `HAS_DURABILITY`, `HAS_FIELD_TOOL` | Flat durability, increased durability, Self Repair, mining speed, attack speed, stability, Luck, Treefeller limit on the general Tool Head profile, Pick Head-only Vein Miner, and Ore Burst speed. Mining level is a Tool Head base-profile hard gate, with stored head upgrades such as Diamond Tip allowed outside normal affixes. |
| Tool Rod | `HAS_DURABILITY`, `HAS_FIELD_TOOL` | Flat durability, increased durability, Self Repair, mining speed, attack speed, FE usage, FE transfer, stability, control, and Battery Support. Battery Support gates Battery Cell compatibility only. |
| Modular Tool | `HAS_DURABILITY`, `HAS_FIELD_TOOL` | Reserved profile. Assembled modular tools currently carry no own rolled affixes; effective stats come from installed Tool Head and Tool Rod stacks. |

Modifier lens tags are inferred from each modifier definition's stat/effect family. Power, Speed, Yield, Stability, and Control lenses cover the focused families. Kinetic covers speed and motion families such as processing speed, instant process, batch size, mining speed, attack speed, and Ore Burst speed. Efficiency remains the legacy broad-family lens for existing item ids. Lens tags bias Affix Forge add/upgrade selection weights and do not change modifier eligibility, tier eligibility, or slot limits. The same `lensTags` data is exposed in Moddex as tag chips and tag filters for the selected profile's modifier pool.

## Unique Rules

`UNIQUE` rarity is authored-only. Random rarity rolls do not create Unique items, and Unique targets cannot be refined, ascended, affix-injected, or debug-rerolled.

Unique targets may use authored base profiles and fixed behaviors, but they do not have to. The current `rngtech:unique_potato_battery_cell` uses a Voltaic Spud base profile and `UNIQUE` rarity, rejects refinement, and does not roll removable affixes.

## Category Matrix

| Category | Implicit | Prefix | Suffix | Enchant | Notes |
|---|---|---|---|---|---|
| Machine block | Prototype | Prototype | Prototype | Planned | Applies to machine stacks and placed machines such as the Crusher, Furnace, Alloy Furnace, Battery Chassis, Component Assembler, concrete solid fuel-burning generator chassis, Bio Generator, Solar Panels, Solar Array Controller, Potential Reactor, Corrosion Cell, Cavitation Generator, Ammonia Synthesizer, Ammonia Fuel Cell, Vacuum Collapse Generator, Component Recycler, Crude and Steel Metal Press blocks, Melter, Fluid Tanks, and Compressor Tanks. |
| Machine chassis item | No | No | No | No | Registered chassis subtype items do not currently carry RPG traits or refinement. |
| Machine part | Prototype | Prototype | Prototype | Planned | Applies to parts such as `CRUSH_HEAD`, `HEAT_CORE`, `ALLOY_CRUCIBLE`, `BIO_CHAMBER`, `SERVO`, `FLUID_PUMP`, `CATHODE`, `SOLAR_ARRAY_EXTENDER`, `FUEL_BOX`, `REACTOR_CHAMBER`, `RECOVERY_FILTER`, `CONTAINMENT_LINING`, `AMMONIA_CATALYST_BED`, `FUEL_CELL_MEMBRANE`, `CAVITATION_ROTOR`, `VOID_CHAMBER`, `COLLAPSE_NOZZLE`, `DIMENSIONAL_STABILIZER`, and `DISASSEMBLY_HEAD`; installed parts contribute their source-local base profile plus stored affixes through placed machine Gear tabs. |
| Battery cell item | Prototype | Prototype | Prototype | Planned | Applies to [Battery Cells](../content/battery-cells.md) used by [Battery Chassis](../content/battery-chassis.md). Flat capacity and increased capacity are both prefixes. Capacity, transfer, efficiency, idle loss, and cell-level traits live on cells. |
| Portable utility item | Prototype | Prototype | Prototype | Planned | Applies to the [Miner's Companion](../content/miners-companion.md) and [Forestry Companion](../content/tree-farm-automation.md). These are item-stack refinement targets; the Miner's Companion has filter-slot prefixes and energy usage, while the Forestry Companion has processing speed and energy usage for placed cart work. The Magnet and Mining Lamp Gear items are fixed upgrades and are not separate affix targets. |
| Modular field tool item | Prototype | Prototype | Prototype | Planned | Applies to Tool Heads and Tool Rods. Assembled [Modular Field Tools](../content/modular-field-tools.md) are blank carriers, are not refinement targets, and derive effective stats from installed part base profiles plus stored part affixes. |
| Energy storage block | Prototype | Prototype | Prototype | Planned | Applies to the implemented [Battery Chassis](../content/battery-chassis.md). Chassis modifiers affect chassis behavior; assigned fixed behaviors include leakage damping, charge balancing, and burst release. |
| Universal cable / Universal connector / Energy, Fluid, and Item connectors | Out of scope | Out of scope | Out of scope | Out of scope | Universal Cables, Universal Connectors, and connector modules use fixed transfer, shipment, wait, and jam stats. Installed modules, channels, Attach As, modes, and disabled cable links are placement/configuration state, not modifier targets. |
| Regular item | Deferred | Deferred | Deferred | Deferred | General item modifiers are not part of the initial scope. |
| World block | Out of scope | Out of scope | Out of scope | Out of scope | RNGTech focuses on machines, not arbitrary world blocks. |
| Item storage network | Out of scope | Out of scope | Out of scope | Out of scope | Explicitly outside project scope. |

## Modifier Matrix

Rows without links are either current named affix families summarized in this matrix or candidate design names. The Status column is authoritative for whether a row is implemented, planned, deferred, or out of scope.

| Modifier | Machine Block | Machine Part | Regular Item | World Block | Notes |
|---|---|---|---|---|---|
| [Auto Balance](../modifiers/auto-balance.md) | Planned | Planned | Deferred | Out of scope | Evenly redistributes compatible inputs across active input slots on bulk-processing machines. |
| [Balance Mode](../modifiers/balance-mode.md) | Implemented | Out of scope | Deferred | Out of scope | Battery Chassis prefix that enables even fill and unload behavior across installed Battery Cells. |
| Auto Eject | Planned | Planned | Deferred | Out of scope | Pushes finished outputs into an adjacent inventory when possible. Machine-local only; it does not create item-network behavior. |
| Batch Start | Planned | Planned | Deferred | Out of scope | Waits until enough inputs and energy or fuel exist to complete a batch, then starts processing cleanly. |
| [Bulk Speed](../modifiers/bulk-speed.md) | Implemented | Out of scope | Deferred | Out of scope | Ramps processing speed by `1%` per completed process, up to `100%`, on eligible processing-machine blocks. |
| Catalyst Saver | Deferred | Deferred | Deferred | Out of scope | Future catalyst-style recipes only. Prevents a catalyst slot from consuming its final catalyst item when possible. |
| Energy category modifiers | Prototype | Prototype | Prototype for battery cells | Out of scope | Covers energy usage, capacity, generation, transfer, and efficiency modifiers. Does not apply to Universal Cables or connector modules. On Solar Array Controllers, Energy Connectors are fixed Gear that set output transfer rather than modifier targets. On battery cells, flat capacity and increased capacity are prefixes. On battery chassis blocks, capacity prefixes scale installed cells while they remain in the chassis. |
| Fluid category modifiers | Prototype | Prototype | Deferred | Out of scope | Covers fluid transfer and fluid-backed processing behavior. Component Assembler uses fluid transfer for Electrolyte Solution and Lubricant input, Corrosion Cell uses installed Fluid Pump transfer for Electrolyte Solution input, Melter uses Fluid Pump parts for output extraction/container filling, Cavitation Generator uses it for nitrogen output draining, and Steel+ Compressor Tanks use fluid transfer for compression/decompression rate while capacity and ratio remain authored. |
| [Fuel Governor](../modifiers/fuel-governor.md) | Prototype | Prototype | Deferred | Out of scope | Implemented as fixed Copper, Bronze, and Steel Fuel Box behavior for solid fuel-burning generators; rolled named modifiers remain planned. |
| Fuel Reserve | Prototype | Prototype | Deferred | Out of scope | Implemented as fixed identity behavior for Alloy solid fuel burner and Bronze Fuel Box; rolled named modifiers remain planned. |
| Graceful Progress | Planned | Planned | Deferred | Out of scope | Preserves recipe progress for a short grace period when input briefly runs out before resetting progress. |
| Input Filter | Prototype | Planned | Deferred | Out of scope | Crusher Feed Control prefixes narrow top automation insertion by valid recipe, output acceptance, output-bonus-bank compatibility, and dense-batch output acceptance. Broader input-filter modifiers remain planned. |
| Input Pairing | Planned | Planned | Deferred | Out of scope | For bulk machines, processes matching items from paired slots together only when both sides are ready. |
| Instant Process | Prototype | Prototype | Deferred | Out of scope | Rollable chance suffix for processing profiles. Powered machines require full adjusted craft FE up front; otherwise normal processing continues. |
| Jam Bypass | Planned | Planned | Deferred | Out of scope | Sends compatible outputs to another active output slot if the preferred slot is blocked. |
| Output Sorter | Planned | Planned | Deferred | Out of scope | Sends primary outputs and bonus or byproduct outputs to different active output slots. |
| Overflow Guard | Prototype | Planned | Deferred | Out of scope | Implemented as fixed `OUTPUT_GUARD` identity on selected processing chassis and as the Crusher Frame prefix family for Crusher machines. |
| Overclocked | Prototype | Prototype | Deferred | Out of scope | Rollable compound suffix for powered processing profiles. It increases processing speed and also increases energy usage in the shared `processing_speed` group. |
| [Packer](../modifiers/packer.md) | Planned | Planned | Deferred | Out of scope | Combines compatible stackable items across active output slots. |
| Priority Feed | Planned | Planned | Deferred | Out of scope | Chooses which input slot to process first using a mode such as fullest stack, leftmost slot, or rarest input. |
| Recipe Lock | Planned | Planned | Deferred | Out of scope | Locks the machine to the last valid recipe and rejects unrelated inputs until cleared. |
| Remainder Keeper | Planned | Planned | Deferred | Out of scope | Keeps crafting or container remainders inside a protected slot instead of dropping them when possible. |
| Round Robin Output | Planned | Planned | Deferred | Out of scope | Spreads outputs across active output slots instead of filling one slot first. Complements [Packer](../modifiers/packer.md). |
| Smart Fuel | Planned | Planned | Deferred | Out of scope | Chooses the smallest valid fuel stack that can complete the next job, reducing fuel waste. |
| Super Output | Prototype | Prototype | Deferred | Out of scope | Rollable chance suffix for item-output processors. It adds one extra copy of the base stackable output and does not multiply deterministic `OUTPUT_AMOUNT`. |
