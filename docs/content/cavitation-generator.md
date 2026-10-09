# Cavitation Generator

Status: Prototype

Player guide: [Cavitation Generator](https://c-pettersson.github.io/rngtech/cavitation-generator/)

The Cavitation Generator (`rngtech:cavitation_generator`) is a Stage 5+ fluid-fed FE generator with local heat strain and rotor-stack durability wear. Stage 6 nitrogen Gear adds an internal Nitrogen output tank; the Stage 7 Aethergold rotor is a burst sidegrade. FE beyond its `4,000 FE` buffer and Battery Cell is vented rather than pausing the machine. Cavitation does not add a pressure network; pressure remains a recipe display and balance field.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:cavitation_generator` | `CavitationGeneratorBlockEntity` | `CavitationGeneratorMenu` / `CavitationGeneratorScreen` | Side fluid input/output, side filled-container input, side FE extraction, bottom worn-rotor extraction | Item stack and placed machine |

The default recipe type is `rngtech:cavitation`. Recipes declare fluid input, generated FE, processing ticks, minimum rotor stage, pressure rating, heat strain, and rotor wear. They may also declare required rotor/nozzle ingredients and an optional fluid output. Gear-specific recipes win over generic fluid-only recipes when both match.

Side FE export is capped by the installed Energy Connector tier, or by the rotor/nozzle `ENERGY_TRANSFER` fallback when no connector is installed. The Aethergold rotor does not raise that fallback, so its burst needs a Sparksteel or better Energy Connector. Gear slots are not exposed to automation.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Cavitation Rotor | Yes | Steel, Titanium, Tungstensteel, Aethergold, and Nullite Cavitation Rotors plus `rngtech:nitrogen_extraction_rotor` | Sets `PROCESSING_LEVEL`, generation scaling, cycle speed, fallback FE export, wear resistance, rotor durability, and nitrogen `OUTPUT_AMOUNT` identity |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles plus `rngtech:nitrogen_separation_nozzle` | Tunes FE output, fallback transfer, heat strain, nitrogen `OUTPUT_AMOUNT`, and nitrogen output transfer. Nitrogen Separation Nozzle is Cavitation-only. |
| Heat Core | No | Existing Heat Core items | Adds heat-related stat contribution |
| Battery Cell | No | Existing Battery Cell items | Adds portable FE capacity |
| Servo | No | `rngtech:titanium_servo`, `rngtech:tungstensteel_servo`, `rngtech:nullite_servo`, or `rngtech:exotic_servo` | Improves processing speed, rotor wear control, heat strain control, and nitrogen output handling |
| Energy Connector | No | Existing Energy Connector items | Sets the side FE export cap when installed; without one, export uses the rotor/nozzle `ENERGY_TRANSFER` fallback |

`OUTPUT_AMOUNT` controls produced Nitrogen mB per recipe cycle; Servo `PROCESSING_SPEED`, `STABILITY`, `TEMPERATURE_STABILITY`, and `FLUID_TRANSFER` affect cycle time, rotor wear, heat strain, and output-tank draining respectively. Battery Cells add storage but do not raise the side export cap. Tungstensteel and Exotic Servos favor faster cycles; the Nullite Servo halves base processing speed in exchange for voiding excess Nitrogen when the output tank already contains Nitrogen.

Purging the input tank, by button or `rngtech:purge_bucket`, clears the active recipe state before draining.

Rotor wear is stored on the installed rotor stack. A worn rotor is ejected as `rngtech:pitted_cavitation_rotor` when stack wear reaches the rotor's effective `DURABILITY`. Pitted rotors recycle in a Stage 5+ Component Recycler into `rngtech:recycling_byproduct` only, because all worn rotors collapse into the same generic waste item. Cavitation Rotors can roll both flat `ADD DURABILITY` and percent `INCREASED_PERCENT DURABILITY` prefixes. `rngtech:aethergold_cavitation_rotor` is intentionally short-lived: its high cycle speed and FE output come with much higher wear per completed recipe.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
