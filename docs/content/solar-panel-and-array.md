# Solar Panel and Solar Array

Status: Prototype

Player guide: [Solar Panel](https://c-pettersson.github.io/rngtech/solar-panel/)

Solar power is a passive FE generator path that depends on daylight, sky access, and weather instead of fuel.

## Current Runtime Surface

| Block | Resource id | Stage | Clear day output | Weather output | Internal buffer |
|---|---|---:|---:|---:|---:|
| Crude Solar Panel | `rngtech:crude_solar_panel` | 1 | `4 FE/t` | `1 FE/t` | `256 FE` |
| Copper Solar Panel | `rngtech:copper_solar_panel` | 2 | `8 FE/t` | `2 FE/t` | `512 FE` |
| Gold Solar Panel | `rngtech:gold_solar_panel` | 3 | `12 FE/t` | `3 FE/t` | `512 FE` |
| Sparksteel Solar Panel | `rngtech:sparksteel_solar_panel` | 5 | `20 FE/t` | `7 FE/t` | `1024 FE` |
| Solar Array Controller | `rngtech:solar_array_controller` | 4 | Aggregates panels | Aggregates panels | `512 FE` plus optional Battery Cell |

Panels are registered blocks with block entities and FE extraction capability. They do not have item slots, fuel slots, processing recipes, or a placed-machine UI.

Upgraded Solar Panel recipes use the non-rollable `rngtech:solar_panel_frame` intermediate instead of consuming earlier Solar Panel stacks, so a well-rolled lower panel remains useful in standalone arrays.

The Solar Array Controller has Process, Gear, Stats, and Refinement tabs. Its Gear tab holds one Battery Cell, one Energy Connector, and one Solar Array Extender.

## Generation Rules

Solar generation is server-authoritative.

- The dimension must be enabled by common config; only the Overworld is enabled by default.
- The position above the panel must see sky, and generation requires daytime. Weather uses the weather output value.
- Peak Solar multiplies that panel's clear or weather output before standalone storage/export or controller aggregation.

Standalone panels store generated FE in their small internal buffer and have no machine-side export cap: the attached receiver or [Universal Connector tier](basic-wire.md#energy-transfer-limits) decides the rate. They do not accept external FE.

## Array Controller

The controller scans a bounded horizontal area every 20 ticks:

| Range rule | Value |
|---|---:|
| Base range | `1` |
| Maximum effective range | `2` |
| Scan height | Same Y level as the controller |

Range is a square radius that excludes the controller's own position. Panels must connect to the controller through adjacent Solar Panels inside that square.

Array Expansion increases controller range rather than panel count: controller rolls can add up to `+2` and an installed Solar Array Extender adds `+1`, but the effective range is capped at `2` (at most `24` panels).

If two controllers cover the same panel, each receives half of that panel's generation for that tick. A panel owned by a controller has its standalone generation/export suppressed briefly so it does not double-generate. The controller applies its own generation modifiers, flat generation, and efficiency once to the summed array output, so a flat roll adds its FE/t once rather than once per panel.

A full range of same-stage panels applies a `20%` Set bonus. The range overlay is a persistent client-side toggle.

## Solar Array Extenders

| Component | Resource id | Implicit identity |
|---|---|---|
| Sparksteel Solar Array Extender | `rngtech:sparksteel_solar_array_extender` | Clear-day bonus |
| Aethergold Solar Array Extender | `rngtech:aethergold_solar_array_extender` | Nighttime bonus |

Both extenders add `+1` array range and no generation multiplier. Stored rolled affixes on the component also contribute while it is installed.

## Solar Panel Modifiers

Solar Panel modifiers include normal generator stats plus the panel-specific Peak Solar suffix:

| Modifier | Slot | Effect |
|---|---|---|
| Peak Solar | Suffix | Increases that panel's power generation by `50-100%` during the zenith window, from Minecraft time `4000` through `8000` each day. |

## Solar Controller Modifiers

Solar Array Controller modifiers include normal generator stats plus controller-specific affixes:

| Modifier | Slot | Mod group | Stat | Tier 1-4 roll ranges |
|---|---|---|---|---|
| Array Expansion | Suffix | `solar_array_size` | `SOLAR_PANEL_LIMIT` | `+1`, `+1`, `+2`, `+2` (effective range capped at `2`) |
| Moonlit Conversion | Suffix | `solar_low_light` | `MOONLIGHT_CONVERSION` | `10-20`, `20-30`, `30-40`, `50-55` % of clear-day output at night |
| Lunar Inverter | Suffix | `solar_low_light` | `LUNAR_INVERSION` | `30-40`, `41-55`, `56-70`, `71-85` % at night; clear-day output `x0.85` |
| Cloud Piercer | Suffix | `solar_weather` | `WEATHER_RECOVERY` | `10-15`, `16-25`, `26-40`, `45-60` % of the weather gap; Stability adds `(STABILITY - 1) x 0.25` (max `0.5`), total capped at `0.85` |
| Clear-Sky Amplifier | Suffix | `solar_clear_sky` | `CLEAR_SKY_AMPLIFICATION` | `5-10`, `11-20`, `21-35`, `40-50` % |
| Panel Synchronizer | Prefix | `solar_panel_sync` | `SOLAR_PANEL_SYNCHRONIZATION` | `5-10`, `11-20`, `21-35`, `40-50` %, when more than one active panel shares one material |
| Panel Arbitration | Prefix | `solar_panel_selection` | `SOLAR_PANEL_ARBITRATION` | Fixed `1`; sorts claimed panels by output before distance |

Moonlit Conversion and Lunar Inverter share a mod group, so they cannot roll together. Every controller special uses roll weight `60`.

Without an installed Energy Connector the controller exports at its base `128 FE/t`; with one, the connector tier is the cap. The cap is per tick and shared between the controller's own push to adjacent receivers and any connector pulls from its sides. The controller Stats tab reports this effective Energy Transfer value.

## Automation

| Block | Energy capability | Item capability |
|---|---|---|
| Solar Panel | FE extraction only | None |
| Solar Array Controller | FE extraction only | Battery Cell, Energy Connector, and Solar Array Extender slots are UI-managed Gear |


## Traits And Base Stats

Solar numeric identity is authored in `MachineBaseStatCatalog`:

- Solar Panel base profiles define `ENERGY_GENERATION`, `ENERGY_CAPACITY`, and `EFFICIENCY`. The base profile still carries a legacy material `ENERGY_TRANSFER` value, but it no longer limits panel export and is not shown in the item summary.
- Solar Array Controller base profile defines `ENERGY_GENERATION`, `ENERGY_CAPACITY`, fallback `ENERGY_TRANSFER`, `EFFICIENCY`, `STABILITY`, one Battery Cell slot, one Energy Connector slot, one Solar Array Extender slot, and solar array control stats.

Solar modifiers use dedicated profiles:

| Target | Rollable stats |
|---|---|
| Solar Panel | Flat `ENERGY_CAPACITY_FLAT`, increased `ENERGY_CAPACITY`, `EFFICIENCY`, `ENERGY_GENERATION`, and Peak Solar |
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
