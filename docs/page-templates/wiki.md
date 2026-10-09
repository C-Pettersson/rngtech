---
wiki:
  category: Processing
  icon: rngtech:example
  ids:
    - rngtech:example
---

# Example Machine

{{ infobox(
    fields={
        "Type": "Processing machine",
        "Stages": "1",
        "Power": "FE",
        "Gear": "{{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "Yes",
    },
) }}

The **Example Machine** does one thing, explained for a player in two or three sentences. Say what it is for and where it sits in progression.

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine it with a pickaxe. Say what it keeps when broken.

## Usage

Explain how to run it: inputs, power or fuel, what gates recipes, and what goes wrong.

### Stages

Staged machines only: a table using `item()` links, one row per stage.

### Gear

List the Gear slots, what each accepts, and which are required.

### Automation

| Side | Behavior |
|---|---|
| Top | |
| Bottom | |
| Sides | |

### Example recipes

{{ processing("example") }}

## Screen

List the tabs and what each shows.

## Data values

{{ data_values() }}

## See also

- {{ item('rngtech:furnace') }} (related machines and guides, linked as `furnace.md`, `gear.md`, and so on)

{{ navbox() }}
