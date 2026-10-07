# Machine Parts

Status: Prototype


Machine parts are item components that can affect a machine independently from the machine block itself.

Machine parts use authored base stat profiles from `ComponentBaseStatCatalog`, fixed behavior identity from `MachineImplicitCatalog`, and stored per-stack rolled affixes. Component stage and base stats are separate: stage controls compatibility and recipe gates, while the base profile supplies the part's numeric contribution before its own rolled affixes are applied. Refinement changes only the stored rolled affixes and remaining Refinement Potential, so two copies of the same part item can diverge after Affix Forge use while keeping the same material identity.

Machine parts are installed through placed machine Gear tabs. The Crusher exposes a crush-head slot, Stage 0 Furnace exposes a saved Gear-tab component slot for future compatible parts, electric Furnace chassis expose Heat Core and Battery Cell slots, Alloy Furnace chassis expose Heat Core, Alloy Crucible, Battery Cell, and Servo slots, solid fuel-burning generators expose Heat Core and Fuel Box slots, the Bio Generator exposes a Bio Chamber slot, the Potential Reactor exposes Reactor Chamber, Recovery Filter, and Containment Lining slots, the Corrosion Cell exposes a Fluid Pump slot, Gas Chemistry machines expose optional Servo slots, the Cavitation Generator exposes Cavitation Rotor, Collapse Nozzle, Heat Core, Battery Cell, and optional Stage 6+ Servo slots, the Ammonia Synthesizer exposes a Catalyst Bed slot, the Ammonia Fuel Cell exposes Fuel Cell Membrane and Battery Cell slots, the Vacuum Collapse Generator exposes Void Chamber, Collapse Nozzle, and Dimensional Stabilizer slots, powered Component Recyclers expose Disassembly Head and Recovery Filter slots, Metal Press blocks expose Heat Core, Servo, five internal Mold slots with a Gear-tab active mold selector, and Battery Cell slots, Resonance Calibrator chassis expose Resonance Coil, Control Board, Stabilizer Matrix, Battery Cell, and internal Calibration Pattern slots, the Melter exposes Heat Core, Crush Head, Battery Cell, Servo, and Fluid Pump slots, and Steel+ Compressor Tanks expose Battery Cell and Servo slots. The Crude Recycler and Iron, Copper, and Bronze Fluid Tanks have no active Gear tab.

The Miner's Companion is the current item-stack exception to placed Gear tabs. Its item GUI stores one Crush Head, one Battery Cell, one optional Recovery Filter, one optional Miner's Companion Magnet, and one optional Mining Lamp on the Miner's Companion stack. The Crush Head supplies recovery processing-level gates, the Battery Cell pays utility FE, the Recovery Filter unlocks trace recovery rolls, and the Magnet and Mining Lamp are fixed utility upgrades rather than RPG-affix parts.

Machine parts above primitive stage are intended to be long-term investments. Stage 0 parts such as the Flint Crush Head are disposable starter tooling. Current staged Crush Head, high Heat Core, non-nullite Servo, and Fluid Pump recipes craft each stage from plain material or frame inputs rather than consuming the previous rolled part stack, so existing rolled or refined parts can remain in service. Nullite Servo is the intentional exception and upgrades from Titanium Servo. Future upgrade flows can still raise a component through later material stages, but upgrades do not grant fresh [Refinement Potential](../systems/progression.md#refinement-potential). Component stage controls natural modifier tier weighting and RP expectations, but ordinary affix tiers are not hard-capped by stage. The full upgrade model is documented in [Crafting and Upgrades](../systems/crafting.md), and the material ladder is documented in [Component Stages](../reference/component-stages.md).

## Part Types

| Part Type | Notes |
|---|---|
| `CRUSH_HEAD` | Used by crusher-related progression. Staged heads provide processing level and can roll Pulverizing, Jagged, Kinetic, Scuffed, and Dust-Grooved prefix families. |
| `HEAT_CORE` | Used by solid fuel-burning generators to set FE/t, fuel efficiency, heat isolation, and fuel tier access. Electric Furnace, Alloy Furnace, and Metal Press machines also use Heat Cores for heat transfer, maximum temperature, warmup, cooling, temperature stability, overheat tolerance, and optional `POWER_GRACE`. |
| `ALLOY_CRUCIBLE` | Required by Alloy Furnaces to set active input-slot count and mixing control stats, including rollable heat-control affixes where they affect alloy processing. |
| `BIO_CHAMBER` | Used by Bio Generators to add generic power and fuel-duration suffixes, fuel efficiency, and ingredient-aware crop and prepared-biomass power modifiers. |
| `SERVO` | Used by Metal Press progression for precision control, press speed, heat stability, overheat tolerance, and power-drop tolerance. Servos can also be installed in Gas Chemistry machines and the Melter for optional speed/control, in Steel+ Compressor Tanks to enable compression, and in the Cavitation Generator at Stage 6+ for nitrogen utility control. |
| `FLUID_PUMP` | Used by Melter progression to enable output container filling and side fluid extraction, and by Corrosion Cell progression to enable direct Electrolyte Solution filling. |
| `FUEL_BOX` | Used by solid fuel-burning generators to set fuel slots, fuel forms, burn duration, stability, and behavior such as Quick Feed, Fuel Reserve, Fuel Governor, and compact block fuel support. |
| `REACTOR_CHAMBER` | Used by Potential Reactors to set processing level, salvage generation, and stability. |
| `RECOVERY_FILTER` | Used by Potential Reactors to enable residue recovery and improve salvage efficiency or speed. Optional Miner's Companion Gear also uses Recovery Filter stage and efficiency for trace recovery recipes. |
| `CONTAINMENT_LINING` | Used by Potential Reactors to improve stability. |
| `AMMONIA_CATALYST_BED` | Used by Ammonia Synthesizers to gate synthesis recipes and tune energy usage, speed, efficiency, stability, and fluid transfer. |
| `FUEL_CELL_MEMBRANE` | Used by Ammonia Fuel Cells to gate ammonia power recipes and tune generation, transfer, speed, efficiency, stability, and fluid transfer. |
| `CAVITATION_ROTOR` | Used by Cavitation Generators to set rotor stage, generation scaling, wear resistance, durability, and nitrogen output identity. |
| `VOID_CHAMBER` | Used by Vacuum Collapse Generators to set recipe stage reach, generation, and stability. |
| `COLLAPSE_NOZZLE` | Used by Cavitation Generators and Vacuum Collapse Generators to focus output, stability behavior, and fluid-output handling. Ordinary Collapse Nozzles are shared by both machines; Nitrogen Separation Nozzle is Cavitation-only. |
| `DIMENSIONAL_STABILIZER` | Used by Vacuum Collapse Generators to improve stability, efficiency, and residue handling. |
| `DISASSEMBLY_HEAD` | Used by Component Recyclers to set physical recycling processing level, speed, and stability. |

## Implemented Parts

See [Current Implementation](../reference/current-implementation.md#items-and-components) for registered part families and their current roles. Numeric base profiles come from `ComponentBaseStatCatalog` and the material enums under `com.rngtech.content`; fixed behavior comes from `MachineImplicitCatalog`.

Crafted parts receive per-stack rolls. Constructor fallback rarity and potential are not guarantees for a crafted item. Use the in-game tooltip or ModDex for the effective profile, and [Affix Generation](../systems/affix-generation.md) for rolling rules.

| Part family | Machine reference |
|---|---|
| Crush Heads | [Crusher](crusher.md), [Melter](melter.md), and [Miner's Companion](miners-companion.md) |
| Heat Cores and Fuel Boxes | [Solid Fuel Burning](solid-fuel-burner.md); Heat Cores also support compatible processors |
| Alloy Crucibles | [Alloy Furnace](alloy-furnace.md) |
| Bio Chamber | [Bio Generator](bio-generator.md) and [Algae Photobioreactor](algae-photobioreactor.md) |
| Reactor Chambers, Recovery Filters, and Containment Linings | [Potential Reactor](potential-reactor.md); Recovery Filters also support compatible recyclers and utility items |
| Disassembly Heads | [Component Recycler](component-recycler.md) |
| Servos and Fluid Pumps | [Metal Press](metal-press.md), [Melter](melter.md), and compatible fluid/chemistry machines |
| Resonance Coils, Control Boards, and Stabilizer Matrices | [Resonance Calibrator](resonance-calibrator.md) |
| Cavitation Rotors and Collapse Nozzles | [Cavitation Generator](cavitation-generator.md) |
| Catalyst Beds and Fuel Cell Membranes | [Ammonia Power Chain](ammonia-power-chain.md) |
| Void Chambers and Dimensional Stabilizers | [Vacuum Collapse Generator](vacuum-collapse-generator.md) |

A Crusher requires a compatible Crush Head. Its `PROCESSING_LEVEL` supplies hardness; recipes above that level incur penalties rather than a hard rejection. Other machines can use processing level as a hard recipe gate, so check the host machine's rules.

## Implementation Contract

Current runtime surface:

- Implemented item ids: staged Crush Heads from `rngtech:flint_crush_head` through `rngtech:exotic_crush_head`, eight Heat Cores from Iron through Exotic including the Aluminum low-heat sidegrade, three Alloy Crucibles: Bronze, Steel, and Titanium, four Fuel Boxes, `rngtech:bio_chamber`, four Reactor Chambers, four Recovery Filters, four Containment Linings, Ammonia Catalyst Bed, Fuel Cell Membrane, Steel through Nullite Cavitation Rotors plus `rngtech:nitrogen_extraction_rotor`, `rngtech:aethergold_cavitation_rotor`, shared Steel through Exotic Collapse Nozzles plus Cavitation-only `rngtech:nitrogen_separation_nozzle`, Tungstensteel / Nullite / Exotic Void Chambers and Dimensional Stabilizers, eight Disassembly Heads from Iron through Exotic, Stage 4 / 6 / 7 / 8 Servos, and Stage 5 / 6 / 7 / 8 Fluid Pumps.
- Java item type: `MachinePartItem`; staged Crush Heads use the `CrushHeadItem` subtype.
- Part types: `CRUSH_HEAD`, `HEAT_CORE`, `ALLOY_CRUCIBLE`, `BIO_CHAMBER`, `SERVO`, `FLUID_PUMP`, `FUEL_BOX`, `REACTOR_CHAMBER`, `RECOVERY_FILTER`, `CONTAINMENT_LINING`, `AMMONIA_CATALYST_BED`, `FUEL_CELL_MEMBRANE`, `CAVITATION_ROTOR`, `VOID_CHAMBER`, `COLLAPSE_NOZZLE`, `DIMENSIONAL_STABILIZER`, and `DISASSEMBLY_HEAD`.
- Compatible machine types: `CRUSHER` for `CRUSH_HEAD`; `ALLOY_FURNACE` for Alloy Crucibles; `SOLID_FUEL_BURNER` for Heat Cores and Fuel Boxes; `BIO_GENERATOR` for Bio Chamber; `POTENTIAL_REACTOR` for Reactor Chambers, Recovery Filters, and Containment Linings; `AMMONIA_SYNTHESIZER` for Ammonia Catalyst Bed; `AMMONIA_FUEL_CELL` for Fuel Cell Membrane; `CAVITATION_GENERATOR` for Cavitation Rotors and Cavitation-compatible Collapse Nozzles; `VACUUM_COLLAPSE_GENERATOR` for Void Chambers, ordinary Collapse Nozzles, and Dimensional Stabilizers; `COMPONENT_RECYCLER` for Disassembly Heads; `METAL_PRESS` for Servo; `MELTER` for Fluid Pumps. Electric Furnace chassis, Alloy Furnace chassis, Gas Chemistry machines, Cavitation Generators, the Metal Press, the Melter, Corrosion Cell, and Steel+ Compressor Tanks have explicit exceptions that accept staged Heat Cores, Servos, Battery Cells, Fluid Pumps, or other staged Gear up to their stage requirements. Gas Chemistry machines accept Stage 4+ Servos. Cavitation Generators accept Titanium+ Servos only; Steel Servo is rejected. Vacuum Collapse rejects the Nitrogen Separation Nozzle. Component Recyclers also accept Potential Reactor Recovery Filters as optional recovery Gear. Alloy Furnaces also accept Servos as optional control Gear. The Melter also accepts Crush Heads and Servos as Gear components for its own recipe gates and speed behavior. Corrosion Cell also accepts Fluid Pumps as optional fluid-input Gear. Steel+ Compressor Tanks also accept Servos as compression Gear.
- Miner's Companion item GUI accepts Crush Heads, Battery Cells, optional Recovery Filters, the fixed Miner's Companion Magnet, and the fixed Mining Lamp as item-stored Gear instead of placed-machine Gear.
- Default modifier sets: authored component base stat profiles from `ComponentBaseStatCatalog` plus fixed behavior flags from each part identity. All current machine parts roll stored per-stack affixes when crafted; base stats and fixed behaviors are re-derived from the item and are not stored as removable affixes.
- Stack size: `1`.
- Capabilities: none.
- Refinement target: yes, through the Affix Forge.
- Modifier scope: rolled modifiers on an installed part are local. They scale only that part's own stats, and the result then joins the machine. Part tooltips list them under Local Modifiers. Rolls that apply to the whole machine, currently the Fluid Pump's Fluid Capacity, are listed under Global Modifiers.
- Installed behavior: a valid Crush Head in the Crusher Gear-tab crush-head slot is required for Crusher processing, must be no higher than the placed Crusher chassis stage, contributes its effective source-local component profile to the Crusher's stats, and sets the active hardness level through `PROCESSING_LEVEL`. Heat Cores installed in electric Furnace chassis and Metal Press machines contribute their effective component profile, including heat transfer, heat retention, warmup, cooling, maximum temperature, temperature stability, and overheat behavior. Alloy Furnace chassis require a Heat Core and Alloy Crucible; the Heat Core provides the active heat gate, while the Alloy Crucible supplies input slots and mixing-control stats. Cavitation Rotors, Collapse Nozzles, Ammonia parts, and Vacuum Collapse parts contribute their source-local profiles to their matching generator families; Cavitation Rotor wear is stored on the rotor stack and uses the rotor's effective `DURABILITY`. Fully worn Cavitation Rotors eject the non-part `rngtech:pitted_cavitation_rotor`, which has a Stage 5 Component Recycler salvage recipe instead of preserving source rotor material. Vacuum Collapse still gates recipes by Void Chamber stage, and lower-stage Collapse Nozzles add local instability pressure instead of blocking the recipe. Gas Chemistry Servos add speed/control contribution to gas processing or combustion cycles. Cavitation nitrogen output uses rotor/nozzle `OUTPUT_AMOUNT` as its main identity, while Titanium+ Servos can add cycle speed, stability, temperature stability, and fluid-transfer handling according to their authored profiles. In the Melter, Heat Core temperature, Crush Head `PROCESSING_LEVEL`, and a Fluid Pump are required to run recipes; Servos adjust speed/control, and Fluid Pumps enable output fluid movement. In the Corrosion Cell, Fluid Pumps enable side Electrolyte Solution filling and contribute `FLUID_TRANSFER` to the fill rate. Nullite Servo Auto Purge lets Servo-equipped fluid-output machines keep processing when produced fluid overflows a matching output tank, voiding only the excess produced fluid. In Steel+ Compressor Tanks, at least one Servo plus a Battery Cell and FE are required to move fluid between loose and compressed storage.

Implementation checks:

- The Crusher should accept `CRUSH_HEAD` parts in the crush-head slot only when the head stage is compatible with the placed chassis, and reject other items.
- The Crusher should not process, consume FE, or keep partial progress without a valid Crush Head installed.
- Crusher recipes above the installed head's `PROCESSING_LEVEL` should incur the under-hardness time, energy, and jam penalties described on the Crusher page.
- The default Flint Crush Head should show level `1` hardness and reduced processing speed.
- The default Iron Crush Head should show level `2` hardness and increased processing speed.
- Higher staged Crush Heads should expose their component stage, hardness level, authored base stats, and rolled modifiers in tooltips.
- Refining one Iron Crush Head stack should not change another unrefined copy.
- A refined crush head installed in a Crusher should affect the Crusher's computed stats.
- Furnace Gear tabs should reject the current Iron Crush Head because it is compatible with `MachineType.CRUSHER`, not `MachineType.FURNACE`.
- Electric Furnace chassis should accept Heat Cores up to the chassis stage and reject higher-stage Heat Cores. Lead Furnace should expose up to four Heat Core slots.
- Alloy Furnace chassis should require a Heat Core and compatible Alloy Crucible, reject crucibles above the chassis stage, and expose active input slots from the crucible base profile.
- Solid fuel-burning generator Gear tabs should reject parts above the chassis max part stage and reject unrelated material forms.
- Copper Fuel Box should provide Fuel Governor and Quick Feed, Bronze Fuel Box should provide Fuel Governor and preserve the final accepted fuel item through Fuel Reserve, and Steel Fuel Box should provide Fuel Governor plus compact block fuel support.
- Bio Generator Gear tabs should accept one Bio Chamber, reject unrelated parts, and apply ingredient-aware Bio Chamber modifiers only to matching Bio Generator fuel tags.
- Potential Reactor Gear tabs should require a Reactor Chamber, accept optional Recovery Filters and Containment Linings, and reject unrelated material forms.
- Component Recycler Gear tabs should require a Disassembly Head, accept optional Battery Cells and Recovery Filters, and reject stripped gear stacks.
- Crude Metal Press Gear tabs should require a valid Heat Core and selected active Mold before processing; Servo and Battery Cell slots are optional. Steel Metal Press Gear tabs should require a valid Heat Core, valid Servo, and selected active Mold before processing, and should reject Servo installation in unrelated machines.
- Resonance Calibrator Gear tabs should store all six reusable Calibration Patterns, select one active Pattern for recipes, and keep Process automation limited to input, catalyst, and optional recipe stabilizer slots.
- Stage 6+ Servos should provide Power Grace behavior to reduce strain from short power drops.
- Melter Gear tabs should require a valid Heat Core, Crush Head, and Fluid Pump before processing. With a Fluid Pump that has Side Fluid Output behavior installed, the Melter should expose output container filling and side fluid extraction. Corrosion Cell Gear tabs should accept Fluid Pumps as optional fluid-input equipment for side Electrolyte Solution filling.

## Modifier Eligibility

Machine parts have authored base stat profiles, fixed behavior identity, and their own prefix, suffix, or future enchant-style modifiers.

Base stats come from the part identity and are evaluated before the part's stored rolled affixes. Prefix and suffix modifiers are rolled affixes and can be changed by refinement when the part is a legal target.

Use [Modifier Eligibility](../reference/modifier-eligibility.md) to track which modifiers can apply to machine parts.

## Refinement

Machine parts can be refined in the [Affix Forge](affix-forge.md). A part installed in a machine contributes its effective component profile plus stored rolled affixes to that machine's effective stats.

The chassis and Gear-tab surface is documented in [Machine Chassis](machine-chassis.md).

## Upgrades

Part upgrade recipes raise a component into a higher material stage. Upgrade outcomes can fail, fracture a modifier, add strain, complete cleanly, or rarely hit a breakthrough that raises the part beyond the recipe target stage or raises a family rating.

Bad upgrade outcomes should not delete the component under normal rules. Hardcore packs may make those outcomes harsher through recipe or config rules.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Chassis](machine-chassis.md)
- [Affix Forge](affix-forge.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
