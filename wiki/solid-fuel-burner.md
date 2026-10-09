---
wiki:
  category: Power generation
  icon: rngtech:crude_solid_fuel_burner
  ids:
    - rngtech:crude_solid_fuel_burner
    - rngtech:copper_solid_fuel_burner
    - rngtech:alloy_solid_fuel_burner
    - rngtech:steel_solid_fuel_burner
---

# Solid Fuel Burner

{{ infobox(
    variants=[
        ["Crude", "rngtech:crude_solid_fuel_burner"],
        ["Copper", "rngtech:copper_solid_fuel_burner"],
        ["Alloy", "rngtech:alloy_solid_fuel_burner"],
        ["Steel", "rngtech:steel_solid_fuel_burner"],
    ],
    fields={
        "Type": "Generator",
        "Stages": "1–4",
        "Power": "Burns solid fuel, generates FE",
        "Gear": "{{ item('rngtech:iron_heat_core', 'Heat Core') }}, {{ item('rngtech:iron_fuel_box', 'Fuel Box') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Solid Fuel Burner** is RNGTech's first real generator. It burns coal, charcoal, wood, and other furnace fuels and turns them into FE. The chassis decides which Gear you can install, and the Gear decides how fast it generates, which fuels it accepts, and how carefully it spends them.

Each burner you craft rolls its own [rarity and affixes](rarity-and-affixes.md). Burners do not have Machine Mastery.

## Obtaining

### Crafting

Each stage has its own recipe. You do not need the previous stage's burner to craft the next one.

{{ crafting() }}

### Breaking

Mine a placed burner with a pickaxe. It drops itself and keeps its rolled traits. Fuel and installed Gear drop as separate items.

## Usage

### Generating power

A burner needs all three Gear parts before it burns anything: a **Heat Core**, a **Fuel Box**, and a **Battery Cell**. Install them on the Gear tab, then put fuel in the fuel slot on the Process tab.

The burner writes generated FE straight into the installed Battery Cell and pushes it out to adjacent blocks that accept FE, such as a [Universal Cable](universal-cable.md) or a [Battery Chassis](battery-chassis.md). The burner does not limit its own output, so the receiver or cable tier decides how much FE moves each tick. It never accepts FE from outside.

Each fuel item holds a fixed amount of energy: 10 FE per tick of its furnace burn time. One Coal holds 16,000 FE before fuel stats. A stronger Heat Core does not get more FE from that Coal. It spends the same Coal faster, at a higher FE/t. Fuel efficiency and fuel duration stats are what make each item worth more. For example, a Steel Heat Core in a Steel Fuel Box gets about 20,000 FE from one Coal and burns through it in about 10 seconds.

Logs are accepted but burn for only a quarter of their furnace burn time, so turn them into planks first.

### Fuel

A fuel has to pass two checks before the burner accepts it:

- **Fuel tier.** The Heat Core sets the highest tier you can burn.
- **Fuel form.** The Fuel Box decides whether you can burn item fuels, compact block fuels, or both.

| Tier | Fuels | Requires |
|---:|---|---|
| 1 | {{ slot('minecraft:coal') }}{{ slot('minecraft:charcoal') }}{{ slot('minecraft:stick') }}{{ slot('minecraft:oak_planks') }}{{ slot('minecraft:oak_log') }} and every other log and plank | Any Heat Core |
| 2 | {{ tag("rngtech:solid_fuel/tier_2") }} | Copper Heat Core or better, and a Steel Fuel Box because these are block fuels |
| 3 | {{ tag("rngtech:solid_fuel/tier_3") }} | Bronze Heat Core or better |

Any other vanilla item that works in a furnace and leaves nothing behind (so not a Lava Bucket) also counts as a Tier 1 item fuel. Tier 4 has no fuels by default. Modpacks can add fuels to any tier, and a pack that adds charcoal blocks gets them as Tier 2 block fuels automatically.

Item fuels:

{{ tag("rngtech:solid_fuel/item_fuels") }}

Compact block fuels, which only a Steel Fuel Box accepts:

{{ tag("rngtech:solid_fuel/block_fuels") }}

With JEI installed, click the fuel line on the Process tab to open the Solid Fuel Burning category, which groups fuels by tier and form.

### Stages

| Stage | Burner | Notes |
|---:|---|---|
| 1 | {{ item('rngtech:crude_solid_fuel_burner') }} | Accepts Stage 1 Gear. Lowest stability. |
| 2 | {{ item('rngtech:copper_solid_fuel_burner') }} | Accepts Gear up to Stage 2. |
| 3 | {{ item('rngtech:alloy_solid_fuel_burner') }} | Accepts Gear up to Stage 3. Has Fuel Reserve. |
| 4 | {{ item('rngtech:steel_solid_fuel_burner') }} | Accepts Gear up to Stage 4. Highest stability. |

Every burner also accepts lower-stage Gear, so you can mix parts to trade speed against fuel efficiency. For more FE/t than a Steel burner gives, move on to the Stage 5 [Syngas Combustor](syngas-combustor.md).

### Gear

The Gear tab has three required slots:

- **Heat Core**: sets base FE/t and the highest fuel tier. The core's stage cannot exceed the chassis stage.
- **Fuel Box**: sets the number of fuel slots, which fuel forms are accepted, and fuel behaviors.
- **Battery Cell**: stores the generated FE. Stage 0–4 cells fit, up to the chassis stage. In a burner, the cell only adds storage and does not cap output.

| Heat Core | Base output | Fuel efficiency | Heat isolation | Max fuel tier |
|---|---:|---:|---:|---:|
| {{ item('rngtech:iron_heat_core') }} | 24 FE/t | 0.85x | 0.85x | 1 |
| {{ item('rngtech:copper_heat_core') }} | 40 FE/t | 0.80x | 0.75x | 2 |
| {{ item('rngtech:bronze_heat_core') }} | 64 FE/t | 1.00x | 1.00x | 3 |
| {{ item('rngtech:steel_heat_core') }} | 96 FE/t | 1.15x | 1.20x | 4 |

Heat isolation below 1.00x leaks heat: part of each fuel item's burn time is lost while it burns, so Iron and Copper Heat Cores waste some fuel. At 1.00x or above, nothing leaks.

| Fuel Box | Fuel slots | Fuel efficiency | Forms | Behaviors |
|---|---:|---:|---|---|
| {{ item('rngtech:iron_fuel_box') }} | 1 | 0.95x | Item fuels | None |
| {{ item('rngtech:copper_fuel_box') }} | 1 | 0.90x | Item fuels | Fuel Governor, Quick Feed |
| {{ item('rngtech:bronze_fuel_box') }} | 2 | 1.00x | Item fuels | Fuel Governor, Fuel Reserve |
| {{ item('rngtech:steel_fuel_box') }} | 2 | 1.10x | Item and block fuels | Fuel Governor, Block Feed |

The Heat Core's and Fuel Box's fuel efficiency multiply together.

What the behaviors do:

- **Fuel Governor**: when the Battery Cell is full, the burner pauses instead of burning fuel for nothing. The current fuel item stops burning and loses no burn time to heat leakage, the burner keeps exporting FE, and it picks up where it left off once the cell has room. An Iron Fuel Box has no governor, so it keeps burning and wastes fuel while the cell is full. See [Fuel Governor](rarity-and-affixes.md#fuel-governor).
- **Quick Feed**: an identity trait on the Copper burner, Copper Heat Core, and Copper Fuel Box. It does not change how the burner runs.
- **Fuel Reserve**: the burner leaves the last fuel item in its slot untouched, so an automated feed line never runs completely dry. Comes from the Alloy burner and Bronze Fuel Box.
- **Block Feed**: lets the burner accept compact block fuels such as Blocks of Coal.

### Automation

| Side | Behavior |
|---|---|
| Any | Inserts fuel into the Fuel Box fuel slots. |
| Any | Exports FE to adjacent receivers. Does not accept FE. |

Heat Cores, Fuel Boxes, and Battery Cells are installed by hand. Automation cannot reach the Gear slots.

## Screen

The burner screen has four tabs:

- **Process**: fuel slots, burn progress, stored FE, and FE/t. Hover for exact values.
- **Gear**: Heat Core, Fuel Box, and Battery Cell slots.
- **Stats**: the burner's current stats, including traits and installed Gear.
- **Refinement**: refine the placed burner's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Battery Cells](battery-cells.md)
- [Bio Generator](bio-generator.md), the crop-based alternative.
- [Machine Stats](machine-stats.md)

{{ navbox() }}
