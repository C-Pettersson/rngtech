# Solar Panel and Solar Array

Status: Prototype


Solar power is a passive FE generator path. It depends on daylight, sky access, weather, and storage planning instead of fuel.

## Current Runtime Surface

| Block | Resource id | Stage | Clear day output | Weather output | Internal buffer |
|---|---|---:|---:|---:|---:|
| Crude Solar Panel | `rngtech:crude_solar_panel` | 1 | `4 FE/t` | `1 FE/t` | `256 FE` |
| Copper Solar Panel | `rngtech:copper_solar_panel` | 2 | `8 FE/t` | `2 FE/t` | `512 FE` |
| Gold Solar Panel | `rngtech:gold_solar_panel` | 3 | `12 FE/t` | `3 FE/t` | `512 FE` |
| Sparksteel Solar Panel | `rngtech:sparksteel_solar_panel` | 5 | `24 FE/t` | `8 FE/t` | `1024 FE` |
| Solar Array Controller | `rngtech:solar_array_controller` | 4 | Aggregates panels | Aggregates panels | `512 FE` plus optional Battery Cell |

Panels are registered blocks with block entities and FE extraction capability. They do not have item slots, fuel slots, processing recipes, or a placed-machine UI.

Upgraded Solar Panel recipes use the non-rollable `rngtech:solar_panel_frame` intermediate instead of consuming earlier Solar Panel stacks, so a well-rolled lower panel remains useful in standalone arrays.

The Solar Array Controller has a compact screen with Process, Gear, Stats, and Refinement tabs. Its Gear tab accepts one Battery Cell for output smoothing and extra storage, one Energy Connector that sets maximum FE/t export, and one Solar Array Extender component that expands range and tunes generation behavior.

## Generation Rules

Solar generation is server-authoritative.

- The dimension must be enabled by common config. The Overworld is enabled by default; Nether, End, and other dimensions are disabled by default.
- The panel position above the block must see sky. Transparent blocks that preserve sky visibility can allow generation.
- Generation normally requires daytime.
- Rain, snow from weather, or thunder uses the weather output value.
- The Peak Solar suffix is a panel-local modifier. During the zenith window, from Minecraft time `4000` through `8000` each day, it multiplies that panel's clear or weather output before standalone storage/export or controller aggregation.
- Night, blocked sky, or disabled dimensions produce `0 FE/t`.

Standalone panels store generated FE in their small internal buffer and export through standard NeoForge energy capability. They do not accept external FE.

## Array Controller

The controller scans a bounded horizontal area every 20 ticks:

| Range rule | Value |
|---|---:|
| Base range | `1` |
| Maximum effective range | `6` |
| Scan height | Same Y level as the controller |

Range is a square radius around the controller. A range `1` controller can use the eight surrounding positions, range `2` can use a `5x5` field around the controller, and so on. The controller's own position is excluded. Panels must be connected to the controller through adjacent Solar Panels inside that square; disconnected panel islands inside the range are ignored.

The Array Expansion modifier now increases controller range instead of panel count. The unmodified controller has range `1`; controller rolls can add up to `+2`; an installed Solar Array Extender can add up to `+4`; the final effective range is capped at `6`.

Overlapping controllers share panel generation. If two controllers both cover the same panel, each receives half of that panel's controller-adjusted generation for that tick.

While a controller owns a panel for aggregation, the panel's standalone generation/export path is suppressed briefly so the same panel does not double-generate. The controller computes each selected panel's environment-adjusted generation, applies solar controller modifiers and generation/efficiency traits, stores FE internally or in the installed Battery Cell, then exports through its block energy capability.

If every panel position inside the current range is filled by Solar Panels of the same stage, the controller applies a `20%` Set bonus. The Process tab has a single icon button that toggles a persistent client-side ghost range overlay for the placed controller. The overlay remains visible after the UI closes while the controller is loaded and the toggle is enabled. The Process tab also exposes tooltips for current FE/t, stored FE, connector-controlled max output, active panel count, blocked panel count, and daylight/weather state.

## Solar Array Extenders

| Component | Resource id | Implicit identity |
|---|---|---|
| Sparksteel Solar Array Extender | `rngtech:sparksteel_solar_array_extender` | Clear-day bonus |
| Aethergold Solar Array Extender | `rngtech:aethergold_solar_array_extender` | Nighttime bonus |

Both extenders provide the same base array range and generation contribution. Their implicit identity decides whether the component leans toward daytime or nighttime output. Stored rolled affixes on the component also contribute while it is installed.

## Solar Panel Modifiers

Solar Panel modifiers include normal generator stats plus the panel-specific Peak Solar suffix:

| Modifier | Slot | Effect |
|---|---|---|
| Peak Solar | Suffix | Increases that panel's power generation by `50-100%` during the zenith window, from Minecraft time `4000` through `8000` each day. |

## Solar Controller Modifiers

Solar Array Controller modifiers include normal generator stats plus controller-specific affixes:

| Modifier | Slot | Effect |
|---|---|---|
| Array Expansion | Suffix | Adds controller range, up to `+2` from controller rolls. |
| Moonlit Conversion | Suffix | Generates at `10-55%` of clear-day output at night, depending on tier and roll. |
| Cloud Piercer | Suffix | Recovers more of the gap between weather output and clear-day output. |
| Lunar Inverter | Suffix | Strong night conversion with a clear-day output penalty. Conflicts with Moonlit Conversion. |
| Clear-Sky Amplifier | Suffix | Boosts clear-day output only. |
| Panel Synchronizer | Prefix | Boosts generation when all controlled panels are the same material. |
| Panel Arbitration | Prefix | Selects highest-output panels before nearest panels. |

Energy Connector transfer uses the existing connector tiers: Basic `128 FE/t`, Copper `512 FE/t`, Gold `2,048 FE/t`, Sparksteel `8,192 FE/t`, Arclite `32,768 FE/t`, and Debug `50,000,000 FE/t`. Without an installed connector, the controller uses its base `128 FE/t` output rate. The controller Stats tab reports this effective connector-controlled Energy Transfer value.

## Automation

| Block | Energy capability | Item capability |
|---|---|---|
| Solar Panel | FE extraction only | None |
| Solar Array Controller | FE extraction only | Battery Cell, Energy Connector, and Solar Array Extender slots are UI-managed Gear |

The controller is the preferred automation surface. It exports to adjacent energy receivers and does not accept external FE.

## Traits And Base Stats

Solar numeric identity is authored in `MachineBaseStatCatalog`:

- Solar Panel base profiles define `ENERGY_GENERATION`, `ENERGY_CAPACITY`, `ENERGY_TRANSFER`, and `EFFICIENCY`.
- Solar Array Controller base profile defines `ENERGY_GENERATION`, `ENERGY_CAPACITY`, fallback `ENERGY_TRANSFER`, `EFFICIENCY`, `STABILITY`, one Battery Cell slot, one Energy Connector slot, one Solar Array Extender slot, and solar array control stats.

Solar modifiers use dedicated profiles:

| Target | Rollable stats |
|---|---|
| Solar Panel | Flat `ENERGY_CAPACITY_FLAT`, increased `ENERGY_CAPACITY`, `EFFICIENCY`, `ENERGY_GENERATION`, `ENERGY_TRANSFER`, and Peak Solar |
| Solar Array Controller | Flat `ENERGY_CAPACITY_FLAT`, increased `ENERGY_CAPACITY`, `EFFICIENCY`, `ENERGY_GENERATION`, `STABILITY`, plus Array Expansion, Moonlit Conversion, Cloud Piercer, Lunar Inverter, Clear-Sky Amplifier, Panel Synchronizer, and Panel Arbitration |
| Solar Array Extender | `ENERGY_GENERATION`, array range, Moonlit Conversion, Weather Recovery, and Clear-Sky Amplification |

Solar targets do not roll fuel, heat, processing, or output-amount modifiers. Fixed identity is nonnumeric and stays separate from rolled affixes.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Battery Cells](battery-cells.md)
- [Battery Chassis](battery-chassis.md)
- [Universal Cable](basic-wire.md)
