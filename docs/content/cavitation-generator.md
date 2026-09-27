# Cavitation Generator

Status: Prototype

The Cavitation Generator is a Stage 5+ fluid-fed FE generator registered as `rngtech:cavitation_generator`. It consumes recipe-authored fluids from an input tank, generates FE over a timed cycle, tracks local heat strain, and wears the installed Cavitation Rotor's own item-stack durability. With the Stage 6 nitrogen rotor and nozzle pair installed, it also separates Water into an internal Nitrogen output tank. The Stage 7 Aethergold Cavitation Rotor is a burst sidegrade that runs cycles very quickly, raises FE generation sharply, and burns through rotor durability in a short span. High-generation setups should install an Energy Connector to raise FE export beyond the rotor/nozzle fallback cap. Cavitation does not add a pressure network; pressure remains a recipe display and balance field.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:cavitation_generator` | `CavitationGeneratorBlockEntity` | `CavitationGeneratorMenu` / `CavitationGeneratorScreen` | Side fluid input/output, side filled-container input, side FE extraction, bottom worn-rotor extraction | Item stack and placed machine |

The default recipe type is `rngtech:cavitation`. Recipes declare fluid input, generated FE, processing ticks, minimum rotor stage, pressure rating, heat strain, and rotor wear. They may also declare required rotor/nozzle ingredients and an optional fluid output. Gear-specific recipes win over generic fluid-only recipes when both match. With JEI installed, clicking the Process tab recipe line opens the Cavitation recipe category.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Cavitation Rotor | Yes | Steel, Titanium, Tungstensteel, Aethergold, and Nullite Cavitation Rotors plus `rngtech:nitrogen_extraction_rotor` | Sets `PROCESSING_LEVEL`, generation scaling, cycle speed, fallback FE export, wear resistance, rotor durability, and nitrogen `OUTPUT_AMOUNT` identity |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles plus `rngtech:nitrogen_separation_nozzle` | Tunes FE output, fallback transfer, heat strain, nitrogen `OUTPUT_AMOUNT`, and nitrogen output transfer. Nitrogen Separation Nozzle is Cavitation-only. |
| Heat Core | No | Existing Heat Core items | Adds heat-related stat contribution |
| Battery Cell | No | Existing Battery Cell items | Adds portable FE capacity |
| Servo | No | `rngtech:titanium_servo`, `rngtech:tungstensteel_servo`, `rngtech:nullite_servo`, or `rngtech:exotic_servo` | Improves processing speed, rotor wear control, heat strain control, and nitrogen output handling |
| Energy Connector | No | Existing Energy Connector items | Sets the side FE export cap when installed; without one, export uses the rotor/nozzle `ENERGY_TRANSFER` fallback |

The process inventory has one fluid-container slot, one damaged rotor output slot, and an internal Nitrogen output tank. The Process tab shows compact meters for input fluid, nitrogen output, FE, cycle progress, heat strain, and rotor wear. `OUTPUT_AMOUNT` controls produced Nitrogen mB per recipe cycle; Servo `PROCESSING_SPEED`, `STABILITY`, `TEMPERATURE_STABILITY`, and `FLUID_TRANSFER` affect cycle time, rotor wear, heat strain, and output-tank draining respectively. Battery Cells add storage but do not raise the side export cap. Nullite Servo halves base processing speed but lets excess produced Nitrogen void when the output tank already contains Nitrogen.

The Process tab has separate purge buttons for the input tank and Nitrogen output tank. The craftable `rngtech:purge_bucket` can also right-click the placed generator to void up to `1000 mB`; normal use prefers the input tank, while sneak-use prefers the Nitrogen output tank. Purging the input tank clears the active recipe state before draining.

Rotor wear is stored on the installed rotor stack, so removing and reinstalling the rotor does not reset durability. A worn rotor is ejected as `rngtech:pitted_cavitation_rotor` when stack wear reaches the rotor's effective `DURABILITY` value. Pitted rotors can be disassembled in a Stage 5+ Component Recycler into `rngtech:recycling_byproduct`; they do not recover source material because all worn rotors collapse into the same generic waste item. Cavitation Rotors can roll both flat `ADD DURABILITY` and percent `INCREASED_PERCENT DURABILITY` prefixes. `rngtech:aethergold_cavitation_rotor` is intentionally short-lived: its high cycle speed and FE output come with much higher wear per completed recipe.

## Step-By-Step Guide

1. Craft a Steel Cavitation Rotor and Steel Collapse Nozzle.
2. Craft the Cavitation Generator, place it, and install both parts in the Gear tab.
3. Optionally install a Heat Core for heat-stat contribution, a Battery Cell for portable FE capacity, and an Energy Connector for higher FE export.
4. Fill the input tank with Water for the starter `rngtech:cavitation` recipe. Side fluid automation and filled containers both work.
5. Extract FE from the sides.
6. Watch heat strain and rotor wear in the UI. A fully worn rotor is removed from Gear and the bottom output receives `rngtech:pitted_cavitation_rotor`.
7. For Nitrogen, craft and install `rngtech:nitrogen_extraction_rotor` and `rngtech:nitrogen_separation_nozzle`, then run Water through the same input tank.
8. For burst FE, craft and install `rngtech:aethergold_cavitation_rotor`; it produces a short, high-output jolt before wearing out quickly.
9. Optionally install a Titanium+ Servo for Servo stat contributions and nitrogen output handling. Tungstensteel and Exotic favor faster cycles; Nullite trades speed for Auto Purge. Steel Servo is rejected.
10. Drain Nitrogen from the side fluid capability. The machine pauses with a Nitrogen Full status when the output tank cannot accept the next recipe output, unless a Nullite Servo is installed and the tank already contains Nitrogen.
11. Let strain cool before pushing continuous operation too hard.

## Automation

- Sides fill the input tank and drain the output tank through the fluid capability.
- Sides insert filled fluid containers.
- Sides extract FE, capped by the installed Energy Connector tier or by the rotor/nozzle fallback transfer when no connector is installed.
- Bottom extracts the pitted rotor output.
- Gear slots are manual UI equipment.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
