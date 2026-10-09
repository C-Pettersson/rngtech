---
wiki:
  category: Fluids and gases
  icon: rngtech:wooden_dehumidifier
  ids:
    - rngtech:wooden_dehumidifier
    - rngtech:wooden_dehumidifier_frame
    - rngtech:waterproofed_planks
---

# Wooden Dehumidifier

{{ infobox(
    variants=[
        ["Controller", "rngtech:wooden_dehumidifier"],
        ["Frame", "rngtech:wooden_dehumidifier_frame"],
    ],
    fields={
        "Type": "Multiblock water source",
        "Stage": "2–3",
        "Power": "None",
        "Gear": "None",
        "Mastery": "No",
    },
) }}

The **Wooden Dehumidifier** is a low-tech multiblock that pulls water out of the air. You build it from one controller and a shell of frames, and it fills itself with water while it can see the sky. It needs no power, so it is an early water source for the algae and gas chains.

It can also turn {{ item('rngtech:coal_dust') }} or {{ item('rngtech:gasification_residue') }} plus stored water into Carbon Exhaust, the starter carbon input for the [Algae Photobioreactor](algae-photobioreactor.md).

Unlike most RNGTech machines, the Wooden Dehumidifier has no rarity, affixes, Refinement, or Mastery.

## Obtaining

### Crafting

Craft {{ item('rngtech:waterproofed_planks') }} first, then use them for the controller and the frames. One frame recipe makes four frames.

{{ crafting() }}

### Breaking

Break the controller or a frame with any tool, or by hand. Each block drops itself. Breaking the controller also drops the items in its slots. Removing frames shrinks the structure, and the controller updates its frame count.

## Usage

### Building the structure

The structure is a controller plus at least four frames:

1. Place the {{ item('rngtech:wooden_dehumidifier') }} controller.
2. Place {{ item('rngtech:wooden_dehumidifier_frame') }} blocks so that at least one touches a face of the controller, and the rest touch each other face to face. Diagonal contact does not count.
3. Keep every frame within 4 blocks of the controller on each axis. Frames further out are ignored.

Any shape works, such as a ring or barrel around the controller. Once the controller finds four or more connected frames, they all switch to their formed look and the machine starts working. With fewer than four, it stops and shows an invalid-structure status.

More frames make a bigger and faster machine, up to 25 frames:

| Frames | Water tank | Converted-output tank | Base water rate |
|---:|---:|---:|---:|
| 4 | 4,000 mB | 4,000 mB | 240 mB/min |
| 25 | 25,000 mB | 25,000 mB | 1,500 mB/min |

Each frame adds 1,000 mB to each tank and 60 mB/min to the base rate. Frames beyond 25 still count as part of the structure but add nothing.

### Collecting water

The Dehumidifier makes water only while the controller or at least one formed frame can see the sky. If the whole structure is covered, it makes none.

The biome sets the humidity:

| Humidity | Rate | Example biomes |
|---|---:|---|
| Wet | 1.5× | Swamps, jungles, rivers, oceans, Lush Caves |
| Normal | 1× | Plains, forests, taigas, meadows, beaches |
| Dry | 0.35× | Deserts, badlands, savannas, snowy biomes |
| Blocked | 0× | The Nether, the End, and The Void |

Rain adds another 0.5× while the structure can see the sky. The final rate never goes above 1,500 mB/min. Biomes not in any list count as Normal.

### Making Carbon Exhaust

Put Coal Dust or Gasification Residue in the conversion slot. Each batch uses one item and 1,000 mB of stored water and makes 1,000 mB of Carbon Exhaust over 6,000 ticks (5 minutes). The item and water are taken only when the batch finishes. Conversion pauses if the structure breaks, the water runs short, or the converted-output tank is full.

The conversion recipes use the `rngtech:wooden_dehumidifier_conversion` recipe type.

{{ processing("wooden_dehumidifier_conversion") }}

### Getting fluid out

Click the selected-output square on the screen to choose which tank to empty: Water or Carbon Exhaust. Buckets and other containers in the empty-container slot are filled from the selected tank, and pipes drain the selected tank too.

To throw fluid away, use the purge buttons beside each tank, or right-click the controller with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB. A normal right-click empties water first, and sneaking empties the converted output first. Purging the water tank resets any conversion in progress.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts conversion items and empty containers. |
| Bottom | Extracts filled containers. Drains the selected output fluid. |
| Sides | Drain the selected output fluid. |
| Formed frames | Accept conversion items and empty containers, and drain the selected output fluid, on behalf of the controller. |

The Dehumidifier never accepts fluid from pipes, and has no FE connection. Unformed frames expose nothing.

## Screen

The Wooden Dehumidifier has a single screen with the conversion slot, the container slots, both tanks with purge buttons, and the selected-output square. Hover the gauges and icons for frame count, humidity, sky access, rain bonus, production rate, and status.

## Data values

{{ data_values() }}

## See also

- [Silica Gel Dehumidifier](silica-gel-dehumidifier.md): the Stage 5 water source that does not depend on weather or biome.
- [Algae Photobioreactor](algae-photobioreactor.md)
- [Coal Gasifier](coal-gasifier.md): makes Gasification Residue.

{{ navbox() }}
