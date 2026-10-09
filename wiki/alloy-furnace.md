---
wiki:
  category: Processing
  icon: rngtech:bronze_alloy_furnace_chassis
  ids:
    - rngtech:bronze_alloy_furnace_chassis
    - rngtech:steel_alloy_furnace_chassis
    - rngtech:titanium_alloy_furnace_chassis
---

# Alloy Furnace

{{ infobox(
    variants=[
        ["Bronze", "rngtech:bronze_alloy_furnace_chassis"],
        ["Steel", "rngtech:steel_alloy_furnace_chassis"],
        ["Titanium", "rngtech:titanium_alloy_furnace_chassis"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "3, 4, 6",
        "Power": "FE",
        "Gear": "{{ item('rngtech:bronze_heat_core', 'Heat Core') }}, {{ item('rngtech:bronze_alloy_crucible', 'Alloy Crucible') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }}",
        "Mastery": "Yes",
    },
) }}

The **Alloy Furnace** mixes exact amounts of metals and additives into alloys. Early recipes make alloy blends such as {{ item('rngtech:bronze_blend') }} and {{ item('rngtech:steel_blend') }}, which you then smelt in a [Furnace](furnace.md). Stronger setups unlock direct-ingot recipes that skip the blend step, and late alloys such as Tungstensteel are only made this way.

The Alloy Furnace runs on FE and needs a Heat Core and an Alloy Crucible installed. Like every RNGTech machine, each Alloy Furnace you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

Each chassis has its own recipe. You do not need a Furnace or an earlier Alloy Furnace to craft one.

{{ crafting() }}

### Breaking

Mine a placed Alloy Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## Usage

### Alloying

Put the recipe's ingredients in the input slots. Each recipe needs exact counts, but the slot order does not matter. The Alloy Furnace then heats up and starts mixing only once it reaches the recipe's target temperature. While it is blocked, out of input, or out of power, it cools down.

Each recipe also has a minimum stage. Both the chassis and the installed Alloy Crucible must reach that stage, so late alloys need a Titanium setup.

Your first Bronze does not need an Alloy Furnace: Bronze Blend has a shapeless crafting recipe from Copper Dust, Tin Dust, and Coal Dust. Once you have a Bronze Alloy Furnace, it makes more Bronze Blend from the same dusts plus Charcoal, and it can already make a slow Steel Blend bootstrap recipe.

Stage 4 and higher recipes with a failure output build up failure strain when heat or stability is off, or when power drops mid-craft on power-sensitive recipes. When strain fills, the craft fails and outputs a recoverable {{ item('rngtech:malformed_ingot') }} instead of the alloy. Smelt it in a [Furnace](furnace.md) to get two nuggets back.

### Stages

| Stage | Alloy Furnace | Notes |
|---:|---|---|
| 3 | {{ item('rngtech:bronze_alloy_furnace_chassis') }} | First Alloy Furnace. Makes Bronze and Steel blends. |
| 4 | {{ item('rngtech:steel_alloy_furnace_chassis') }} | Midgame chassis with Output Guard. |
| 6 | {{ item('rngtech:titanium_alloy_furnace_chassis') }} | Advanced chassis with Output Guard. Runs the late direct-ingot alloys. |

Output Guard keeps progress for a while when the output slot is full instead of resetting it.

### Gear

The Gear tab has four slots. No Gear can be a higher stage than the chassis.

- **Heat Core** (required): sets how hot the Alloy Furnace can get.
- **Alloy Crucible** (required): sets how many input slots are active and adds mixing control.
- **Battery Cell** (optional): adds portable FE storage.
- **Servo** (optional): adds control stats.

| Alloy Crucible | Stage | Input slots |
|---|---:|---:|
| {{ item('rngtech:bronze_alloy_crucible') }} | 3 | 3 |
| {{ item('rngtech:steel_alloy_crucible') }} | 4 | 3 |
| {{ item('rngtech:titanium_alloy_crucible') }} | 6 | 4 |

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the active input slots. |
| Bottom | Extracts finished output. |
| Sides | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

Gear slots cannot be filled by automation.

### Mastery and Ascendancies

Completed alloy crafts earn Alloy Furnace Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree, starting from the same point as the Furnace. Failures, stalled heat, blocked output, and power-starved ticks earn nothing. Alloy Furnaces choose between the **Metallurgist** and **Blendwright** [ascendancies](machine-mastery.md#ascendancies).

### Alloy Furnace recipes

The Alloy Furnace uses its own recipe type, `rngtech:alloy_furnace`. Mode tells you whether a recipe makes a blend or a finished ingot. The desiccant recipe makes Supercharged Silica Gel Beads for the [Silica Gel Dehumidifier](silica-gel-dehumidifier.md).

{{ processing("alloy_furnace", columns=["mode", "processing_ticks", "energy", "minimum_component_stage", "minimum_temperature", "required_temperature_stability", "machine_xp"]) }}

## Screen

The Alloy Furnace screen has five tabs:

- **Process**: input and output slots, progress, energy, live heat, and failure strain on recipes that can fail. Status squares show the recipe state and whether the heat target is met. Hover for exact values.
- **Gear**: Heat Core, Alloy Crucible, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Alloy Furnace's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Data values

{{ data_values() }}

## See also

- [Machine Stats](machine-stats.md)
- [Furnace](furnace.md)
- [Crusher](crusher.md): makes the dusts used in blend recipes.

{{ navbox() }}
