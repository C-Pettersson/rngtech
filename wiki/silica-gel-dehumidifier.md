---
wiki:
  category: Fluids and gases
  icon: rngtech:silica_gel_dehumidifier
  ids:
    - rngtech:silica_gel_dehumidifier
    - rngtech:silica_gel_column_casing
    - rngtech:silica_gel_beads
    - rngtech:saturated_silica_gel_beads
    - rngtech:supercharged_silica_gel_beads
    - rngtech:saturated_supercharged_silica_gel_beads
---

# Silica Gel Dehumidifier

{{ infobox(
    variants=[
        ["Controller", "rngtech:silica_gel_dehumidifier"],
        ["Casing", "rngtech:silica_gel_column_casing"],
    ],
    fields={
        "Type": "Multiblock water source",
        "Stage": "5",
        "Power": "None",
        "Gear": "None",
        "Mastery": "No",
    },
) }}

The **Silica Gel Dehumidifier** is a Stage 5 water source that runs on desiccant beads instead of weather. Each dry bead soaks up a fixed amount of water and comes out saturated. You then dry the saturated beads in a [Furnace](furnace.md) and feed them back in.

It needs no power and ignores sky access, biome, rain, and dimension, so it works anywhere, including underground and in the Nether. Your real cost is the Furnace energy spent recharging beads. Like the [Wooden Dehumidifier](wooden-dehumidifier.md), it has no rarity, affixes, Refinement, or Mastery.

## Obtaining

### Crafting

One casing recipe makes two casings, which is exactly one column.

{{ crafting() }}

### Supercharged beads

{{ item('rngtech:supercharged_silica_gel_beads') }} hold more than three times as much water as normal beads. Make them in an [Alloy Furnace](alloy-furnace.md) with Stage 6 parts at 1,450 heat:

{{ processing("alloy_furnace", output="rngtech:supercharged_silica_gel_beads", columns=["processing_ticks", "energy", "minimum_temperature", "minimum_component_stage"]) }}

### Breaking

Mine the controller or a casing with a pickaxe. Each block drops itself, and the controller also drops the beads in its lanes. Breaking a casing only pauses the machine; the beads and stored water stay in the controller.

## Usage

### Building the structure

The only valid shape is a three-block column:

1. Place the {{ item('rngtech:silica_gel_dehumidifier') }} controller.
2. Stack two {{ item('rngtech:silica_gel_column_casing') }} blocks directly on top of it.

If either casing is missing, all three lanes pause. Nothing is lost, and work resumes as soon as the column is complete again.

### Drying the air

The controller has three independent lanes. Put dry beads in any lane's input slot. Each lane turns one dry bead into one saturated bead and adds water to a shared 10,000 mB tank:

| Dry bead | Water per bead | Time | All three lanes |
|---|---:|---:|---:|
| {{ item('rngtech:silica_gel_beads') }} | 945 mB | 1,134 ticks | 3 buckets/min |
| {{ item('rngtech:supercharged_silica_gel_beads') }} | 3,150 mB | 1,134 ticks | 10 buckets/min |

A lane stops if its saturated output slot is full or the water tank cannot fit the next batch. The bead and the water change hands only when the batch finishes. At full speed, one stack of dry beads lasts about 20 minutes.

The absorption recipes use the `rngtech:desiccant_absorption` recipe type.

{{ processing("desiccant_absorption", hide=["minimum_stage"]) }}

### Recharging beads

Smelt saturated beads in an RNGTech [Furnace](furnace.md) to dry them out again. Recharging gives back exactly the bead you put in, never a bonus bead.

{{ processing("furnace", input="rngtech:saturated_silica_gel_beads", hide=["experience", "bonus_output"]) }}

{{ processing("furnace", input="rngtech:saturated_supercharged_silica_gel_beads", hide=["experience", "bonus_output"]) }}

### Getting water out

Pipes can drain water from the controller's four horizontal sides, or from the horizontal sides of either casing. The machine has no bucket slot, so use pipes or a [Universal Cable](universal-cable.md).

To throw water away, use the purge button beside the tank, or right-click the controller with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts dry beads into the lane input slots. |
| Bottom | Extracts saturated beads from the lane output slots. |
| Sides | Drain water. |
| Casing sides | Drain water on behalf of the controller while the column is complete. |

The casings sit on the controller's top face once the column is built, so you usually load dry beads by hand. The machine never accepts fluid, and has no FE connection. Casings do not handle items.

## Screen

The Silica Gel Dehumidifier has a single screen with three input and output lane pairs, the shared water tank, and its purge button. Hover the gauges for lane progress, water amount, structure state, and status.

## Data values

{{ data_values() }}

## See also

- [Wooden Dehumidifier](wooden-dehumidifier.md): the early weather-driven water source.
- [Furnace](furnace.md)
- [Alloy Furnace](alloy-furnace.md)

{{ navbox() }}
