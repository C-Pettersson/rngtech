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

Break the controller or a frame with any tool, or by hand. Each block drops itself. Breaking the controller also drops the items in its slots. Removing any frame stops the machine until you put it back.

## Usage

### Building the structure

The structure is a hollow 3×3×3 cube of 25 {{ item('rngtech:wooden_dehumidifier_frame') }} blocks, with the {{ item('rngtech:wooden_dehumidifier') }} controller in the middle of one side:

1. Build the cube from frames, leaving the middle block of one side out and the centre of the cube empty.
2. Place the controller in the gap in that side, facing out.

Once the cube is complete, every frame switches to its formed look and the machine starts working. A missing frame, a block in the centre, or frames in any other shape leave the structure invalid, and the screen shows how many of the 25 frames it found. Frames outside the cube do nothing.

The complete structure has a 25,000 mB water tank, a 25,000 mB converted-output tank, and a base water rate of 1,500 mB/min.

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
