# Machine Chassis

Status: Prototype


Resource ids:

- `rngtech:furnace_machine_chassis`
- Furnace chassis blocks: `rngtech:iron_furnace_chassis`, `rngtech:copper_furnace_chassis`, `rngtech:bronze_furnace_chassis`, `rngtech:steel_furnace_chassis`, `rngtech:lead_furnace_chassis`, `rngtech:aluminum_furnace_chassis`, `rngtech:titanium_furnace_chassis`, `rngtech:tungstensteel_furnace_chassis`, and `rngtech:exotic_furnace_chassis`
- Crusher chassis blocks: `rngtech:wooden_crusher_chassis`, `rngtech:iron_crusher_chassis`, `rngtech:copper_crusher_chassis`, `rngtech:bronze_crusher_chassis`, `rngtech:steel_crusher_chassis`, `rngtech:aluminum_crusher_chassis`, `rngtech:titanium_crusher_chassis`, `rngtech:tungstensteel_crusher_chassis`, and `rngtech:exotic_crusher_chassis`
- Component Recycler chassis blocks: `rngtech:iron_component_recycler`, `rngtech:copper_component_recycler`, `rngtech:bronze_component_recycler`, `rngtech:steel_component_recycler`, `rngtech:aluminum_component_recycler`, `rngtech:titanium_component_recycler`, `rngtech:tungstensteel_component_recycler`, and `rngtech:exotic_component_recycler`
- `rngtech:washer_machine_chassis`
- `rngtech:solid_fuel_burner_machine_chassis`

## Summary

Machine chassis define the intended machine subtype or category for machine assembly. Some are item-only ingredients; some are placed machine bodies.

The current prototype registers item-only subtype ingredients for Furnace, Washer, and the solid fuel-burning generator category. Crusher, electric Furnace, Component Recycler, and Battery Chassis families use placed chassis blocks: the player places a chassis block, then installs Gear before or during processing.

## Behavior

Current runtime surface:

- Java item types: `MachineChassisItem` for item-only chassis ingredients, `CrusherChassisBlockItem` for placed Crusher chassis, `FurnaceChassisBlockItem` for placed Furnace chassis, and `ComponentRecyclerBlockItem` for placed Component Recycler chassis.
- Subtype enum: `MachineChassisType`.
- Implemented subtypes: `FURNACE`, `CRUSHER`, `WASHER`, and `SOLID_FUEL_BURNER`.
- Tooltip: item-only chassis show subtype; Crusher and Furnace chassis blocks show material and component stage.
- Creative tab: placed chassis and the Furnace, Washer, and solid-fuel subtype ingredients appear in the RNGTech creative tab. The Washer ingredient does not provide a working Washer machine.
- Recipes: the item-only subtype ingredients and placed chassis blocks have crafting recipes. A craftable Washer chassis ingredient does not imply a working Washer machine. Crusher chassis recipes are standalone and draw their base body cost from the Machine Frame ladder or direct early-game materials instead of requiring another Crusher chassis.
- Capabilities: item-only chassis have none. Placed Crusher, electric Furnace, and powered Component Recycler chassis expose block FE input and sided process-item automation.
- RPG traits: no for item-only chassis ingredients; yes for placed Crusher, Furnace, and Component Recycler chassis blocks.
- Refinement target: no for item-only chassis ingredients; yes for placed Crusher, Furnace, and Component Recycler chassis blocks and placed machines.

Crusher chassis blocks cover staged bodies from Wooden through Exotic. They roll Crusher machine traits when crafted, preserve those traits when placed, and apply material-specific base stats and fixed behavior while placed. Crusher chassis recipes do not consume earlier Crusher chassis, which keeps older rolls available for experimentation and side-by-side comparison.

The shared frame ladder is recipe-only. `rngtech:machine_frame` covers Stage 0-3 chassis, early support bodies, and the Steel Metal Press body. `rngtech:reinforced_machine_frame` gates most other Stage 4 Steel and Lead machine bodies. One Reinforced Machine Frame is built around the base Machine Frame with four Steel Plates, two Steel Casings, and two Basic Electric Circuits, adding `12` Steel Ingots and six Steel press operations before the surrounding chassis recipe. `rngtech:advanced_machine_frame` is used by Stage 5-6 chassis, and `rngtech:exotic_machine_frame` is used by Stage 7-8 chassis. Frame tier does not add machine stats, traits, or refinement behavior by itself.

Furnace stages use `rngtech:furnace` as the Stage 0 non-electric furnace. Electric Furnace chassis cover Stage 1-8 progression from Iron through Exotic, with Lead as a Stage 4 sidegrade. They roll electric Furnace machine traits when crafted, preserve those traits when placed, receive FE, and use Gear-tab Battery Cell and Heat Core slots for portable buffer capacity and heat tuning. Lead additionally exposes four processing lanes and four Heat Core Gear slots with slower per-lane base speed.

The Washer chassis item exists as a subtype target, but there is no registered Washer block, block entity, menu, screen, recipe type, or processing behavior yet.

The solid fuel-burning chassis item remains a subtype ingredient and category marker. The placed generator content is registered as four concrete chassis blocks: `rngtech:crude_solid_fuel_burner`, `rngtech:copper_solid_fuel_burner`, `rngtech:alloy_solid_fuel_burner`, and `rngtech:steel_solid_fuel_burner`. There is still no registered `rngtech:solid_fuel_burner` block, item, or block entity type.

## Gear Tabs

Machines built from chassis, and the Battery Chassis block, expose or are intended to expose a Gear tab where components are installed.

Current placed-machine Gear tab status:

| Machine | Implemented component slots | Notes |
|---|---|---|
| Crusher chassis | Crush Head, Battery Cell | The crush-head slot and Battery Cell slot are shown in the Gear tab. A valid Crush Head is required for processing, must not exceed the chassis stage, and provides recipe hardness level. |
| Furnace | Component on Stage 0; Battery Cell and Heat Core on electric stages; Lead has four Heat Core slots | Stage 0 burns fuel from Process. Electric chassis expose a Gear-tab Battery Cell slot, accept FE, and accept Heat Cores up to the chassis stage. Furnace recipes warm to target heat before processing and can require maximum temperature, temperature stability, failure output, and power sensitivity. Lead processes four paired input/output lanes; other electric chassis expose one Heat Core slot. |
| Solid fuel-burning generators | Heat Core, Battery Cell, Fuel Box | Four placed chassis variants use `MachineType.SOLID_FUEL_BURNER`; the public category id `rngtech:solid_fuel_burner` remains unregistered. |
| Battery Chassis | Material-defined Battery Cell slots | Implemented as material-specific blocks such as `rngtech:iron_battery_chassis`; future non-cell component slots are still planned. |
| Component Recycler | Disassembly Head, Battery Cell, Recovery Filter | Disassembly Head is required and gates component recycling recipes. Recovery Filter unlocks filtered secondary outputs. |

Gear slots are saved with the block entity and dropped when the block inventory is dropped. Standard machine-part Gear slots are not exposed through sided item automation. The Crusher Battery Cell slot is shown on the Gear tab but remains side-insertable for Battery Cells. Battery Chassis cell slots are exposed through the chassis item handler.

Installed machine parts contribute their identity traits plus stored per-stack rolled affixes to the placed machine's effective stats. Required processing parts also gate whether the machine can run. This has implemented gameplay effects for the Crusher through staged Crush Heads from Flint through Exotic, for solid fuel-burning generators through Heat Cores and Fuel Boxes, and for electric Furnace and Metal Press heat loops through installed Heat Cores that raise heat-transfer, heat-retention, warmup, cooling, maximum-temperature, temperature-stability, and overheat behavior. Electric Furnace chassis use Battery Cells for buffer capacity; Stage 0 does not yet have a registered Furnace-specific processing part.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Implicit | Placed Crusher, Furnace, Battery Chassis, solid fuel-burning generator, Potential Reactor, Component Recycler, Metal Press, and Melter blocks resolve machine base stats through `MachineBaseStatCatalog` and fixed behavior identity through `MachineImplicitCatalog`. Item-only chassis ingredients do not. |
| Prefix | Placed Crusher and Furnace chassis blocks roll and refine as machine blocks. Item-only chassis ingredients do not. |
| Suffix | Placed Crusher and Furnace chassis blocks roll and refine as machine blocks. Item-only chassis ingredients do not. |
| Enchant | Enchant-style chassis behavior is not implemented. |

Modifier-bearing behavior belongs to [Machine Parts](machine-parts.md) and placed machine traits for now.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
- [Battery Chassis](battery-chassis.md)
- [Solid Fuel Burning](solid-fuel-burner.md)
- [Crafting and Upgrades](../systems/crafting.md)
