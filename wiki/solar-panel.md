---
wiki:
  category: Power generation
  icon: rngtech:crude_solar_panel
  ids:
    - rngtech:crude_solar_panel
    - rngtech:copper_solar_panel
    - rngtech:gold_solar_panel
    - rngtech:sparksteel_solar_panel
    - rngtech:solar_array_controller
    - rngtech:sparksteel_solar_array_extender
    - rngtech:aethergold_solar_array_extender
---

# Solar Panel

{{ infobox(
    variants=[
        ["Crude", "rngtech:crude_solar_panel"],
        ["Copper", "rngtech:copper_solar_panel"],
        ["Gold", "rngtech:gold_solar_panel"],
        ["Sparksteel", "rngtech:sparksteel_solar_panel"],
        ["Controller", "rngtech:solar_array_controller"],
    ],
    fields={
        "Type": "Passive generator",
        "Stages": "1–3, 5 (panels); 4 (controller)",
        "Power": "Sunlight, generates FE",
        "Gear": "Controller only: {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:basic_energy_connector', 'Energy Connector') }}, {{ item('rngtech:sparksteel_solar_array_extender', 'Solar Array Extender') }}",
        "Mastery": "No",
    },
) }}

**Solar Panels** generate FE from daylight with no fuel at all. Place one under open sky and it charges whatever is next to it. Once you have a few, a **Solar Array Controller** gathers a connected field of panels into one output with a shared buffer and its own bonuses.

Each panel and controller you craft rolls its own [rarity and affixes](rarity-and-affixes.md). Solar blocks do not have Machine Mastery.

## Obtaining

### Crafting

Upgraded panels use a {{ item('rngtech:solar_panel_frame') }} instead of the previous panel, so a well-rolled lower panel stays useful rather than being crafted away.

{{ crafting() }}

{{ crafting("rngtech:solar_panel_frame") }}

### Breaking

Mine panels and the controller with a pickaxe. They drop themselves and keep their rolled traits. A broken controller also drops its installed Gear.

## Usage

### Generating power

A panel generates FE only when all of these are true:

- It is daytime.
- The block above the panel can see the sky. Transparent blocks that keep sky visibility are fine.
- You are in the Overworld. Other dimensions are disabled by default in the common config.

Rain, snow, and thunderstorms drop a panel to its lower weather output. At night or under a roof it makes nothing.

A standalone panel stores FE in a small internal buffer and pushes it out to adjacent blocks that accept FE, such as a [Universal Cable](universal-cable.md) or a [Battery Chassis](battery-chassis.md). A panel does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. Panels never accept FE from outside.

### Stages

| Stage | Block | Clear day | Weather | Buffer |
|---:|---|---:|---:|---:|
| 1 | {{ item('rngtech:crude_solar_panel') }} | 4 FE/t | 1 FE/t | 256 FE |
| 2 | {{ item('rngtech:copper_solar_panel') }} | 8 FE/t | 2 FE/t | 512 FE |
| 3 | {{ item('rngtech:gold_solar_panel') }} | 12 FE/t | 3 FE/t | 512 FE |
| 4 | {{ item('rngtech:solar_array_controller') }} | Sum of its panels | Sum of its panels | 512 FE plus Battery Cell |
| 5 | {{ item('rngtech:sparksteel_solar_panel') }} | 20 FE/t | 7 FE/t | 1,024 FE |

Panels can roll the **Peak Solar** suffix, which boosts that panel's output by 50–100% during the midday window (Minecraft time 4000 to 8000).

### Solar Array Controller

Place the controller on the same level as your panels. Every 20 ticks it scans a square around itself and takes over every panel that connects back to it through adjacent panels. Panels inside the square that are not connected to the rest are ignored.

| Range | Area | Max panels |
|---:|---|---:|
| 1 | 3×3 around the controller | 8 |
| 2 | 5×5 around the controller | 24 |

The controller starts at range 1. Range rolls on the controller and an installed Solar Array Extender raise it, up to a maximum of 2.

While a controller owns a panel, that panel stops exporting on its own. The controller adds up its panels' output, applies its own stats once to the total, stores the FE, and exports it to adjacent blocks. A few more rules:

- **Set bonus.** If every panel spot in range holds a panel of the same stage, the array gets +20% output.
- **Overlap.** A panel inside two controllers' ranges splits its output between them.
- **Output cap.** The installed Energy Connector sets the controller's maximum FE/t export. Without one, it exports up to 128 FE/t. The cap is per tick and shared by all sides.

The Process tab has a button that toggles an in-world outline of the controller's range. The outline stays visible after you close the screen.

#### Controller affixes

Besides the usual generator stats, a controller can roll these array affixes. Their values show on the Stats tab under the stat name.

| Affix | Stat | Effect |
|---|---|---|
| Array Expansion | Array Range | +1 or +2 range, still capped at 2. |
| Moonlit Conversion | Moonlight Conversion | The array keeps generating at night, at a share of its clear-day output. |
| Lunar Inverter | Lunar Inversion | Stronger night generation, but clear-day output drops by 15%. Cannot roll alongside Moonlit Conversion. |
| Cloud Piercer | Weather Recovery | Recovers part of the gap between weather output and clear-day output. Higher Stability also helps. |
| Clear-Sky Amplifier | Clear Sky Amplification | Boosts clear-day output only. |
| Panel Synchronizer | Panel Synchronization | Boosts output when every active panel is the same material. |
| Panel Arbitration | Panel Arbitration | Collects the highest-output panels first instead of the nearest. |

### Gear

Panels have no Gear and no screen. The controller's Gear tab has three optional slots:

- **Battery Cell**: extra storage and smoother output.
- **Energy Connector**: sets the maximum FE/t the controller exports. The caps per tier are listed under [Universal Cable energy](universal-cable.md#energy).
- **Solar Array Extender**: adds +1 range.

| Extender | Leans toward |
|---|---|
| {{ item('rngtech:sparksteel_solar_array_extender') }} | Clear-day output |
| {{ item('rngtech:aethergold_solar_array_extender') }} | Night-time output |

Extenders also roll their own affixes, which apply while installed.

### Automation

| Block | Behavior |
|---|---|
| Solar Panel | Exports FE on every side. Does not accept FE or items. |
| Solar Array Controller | Exports FE on every side. Does not accept FE. Gear is installed by hand. |

## Screen

Panels have no screen. The Solar Array Controller screen has four tabs:

- **Process**: stored FE, current FE/t, active and blocked panel counts, daylight and weather state, and the range-outline button. Hover for exact values.
- **Gear**: Battery Cell, Energy Connector, and Solar Array Extender slots.
- **Stats**: the controller's current stats, including traits and Gear.
- **Refinement**: refine the placed controller's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Battery Chassis](battery-chassis.md), for storing daytime power for the night.
- [Machine Stats](machine-stats.md)

{{ navbox() }}
