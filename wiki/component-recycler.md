---
wiki:
  category: Processing
  icon: rngtech:iron_component_recycler
  ids:
    - rngtech:crude_recycler
    - rngtech:hand_crank
    - rngtech:iron_component_recycler
    - rngtech:copper_component_recycler
    - rngtech:bronze_component_recycler
    - rngtech:steel_component_recycler
    - rngtech:aluminum_component_recycler
    - rngtech:titanium_component_recycler
    - rngtech:tungstensteel_component_recycler
    - rngtech:exotic_component_recycler
---

# Component Recycler

{{ infobox(
    variants=[
        ["Crude", "rngtech:crude_recycler"],
        ["Iron", "rngtech:iron_component_recycler"],
        ["Copper", "rngtech:copper_component_recycler"],
        ["Bronze", "rngtech:bronze_component_recycler"],
        ["Steel", "rngtech:steel_component_recycler"],
        ["Aluminum", "rngtech:aluminum_component_recycler"],
        ["Titanium", "rngtech:titanium_component_recycler"],
        ["Tungstensteel", "rngtech:tungstensteel_component_recycler"],
        ["Exotic", "rngtech:exotic_component_recycler"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "Crude (manual), 1–8",
        "Power": "Hand Crank (Crude), FE (Stages 1–8)",
        "Gear": "{{ item('rngtech:iron_disassembly_head', 'Disassembly Head') }}, {{ item('rngtech:iron_recovery_filter', 'Recovery Filter') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Component Recycler** takes RNGTech machines, machine parts, modular tool parts, calibrated components, and Battery Cells apart and gives back some of the materials used to make them. It is how you recover a failed calibration, an outgrown machine, or a Gear part you no longer want.

Returns are always physical parts only. A recycled item's rarity, affixes, and Refinement Potential are lost, so strip valuable items in a [Potential Reactor](potential-reactor.md) first if you want energy for them.

## Obtaining

### Crafting

Each stage has its own recipe. You do not need the previous stage's Recycler to craft the next one. The Crude Recycler also needs a {{ item('rngtech:hand_crank') }} placed on top of it.

{{ crafting() }}

### Breaking

Mine a placed Component Recycler with a pickaxe. It drops itself, and any items inside drop as well. The Hand Crank breaks with any tool.

## Usage

### Recycling

Put the item to recycle in the input slot. When it finishes, its returns go to the three output slots. Each recyclable item has its own fixed return list; the Recycler never reverses arbitrary crafting recipes, and no return is ever larger than what the original craft used. Common returns:

| Item recycled | Guaranteed return | With a Recovery Filter |
|---|---|---|
| Tool Head | Flint or one material gear | Scrap or one material plate |
| Tool Rod | Sticks or one material rod | A button or one material plate |
| Machine chassis | One frame or chassis ingredient plus one material or functional ingredient | Another ingredient, such as a circuit, coil, or bus bar |
| Machine part | One gear, coil, core, plate, casing, or other functional part | Another ingredient |
| Calibrated component | Its main raw ingredient, such as the Iron Gear from a {{ item('rngtech:calibrated_kinetic_component') }} | Nothing extra |
| Battery Cell | The cell core or shell plus one material ingredient | One plate or stabilizing ingredient |

Calibrated components return their raw ingredient, so a low-stability roll from the [Resonance Calibrator](resonance-calibrator.md) is not wasted: recycle it and try again. Patterns, catalysts, and stabilizers used in the calibration are not returned. Machines and parts that were crafted from calibrated components return the uncalibrated ingredient instead.

### Crude Recycler and Hand Crank

The {{ item('rngtech:crude_recycler') }} is the no-power starter version. Place a {{ item('rngtech:hand_crank') }} directly on top of it, put an item in its input slot, close the screen, then right-click the crank repeatedly until the job finishes. Each turn plays a grinding sound, and a separate sound plays when the item is done.

The Crude Recycler only accepts early items: flint, iron, and copper tool heads and rods, early machine blocks, Stage 1 iron components, calibrated components, and early Crush and Disassembly Heads. It handles recipes up to Stage 2 and never gives Recovery Filter returns. Each turn adds 4 ticks of progress. A simple job needs 80 ticks (20 turns), and longer recipes need twice their listed time.

### Stages

| Stage | Recycler | Notes |
|---:|---|---|
| 2 (manual) | {{ item('rngtech:crude_recycler') }} | Hand-cranked, no FE, no Gear, no traits. |
| 1 | {{ item('rngtech:iron_component_recycler') }} | First powered Recycler. |
| 2 | {{ item('rngtech:copper_component_recycler') }} | |
| 3 | {{ item('rngtech:bronze_component_recycler') }} | |
| 4 | {{ item('rngtech:steel_component_recycler') }} | |
| 5 | {{ item('rngtech:aluminum_component_recycler') }} | |
| 6 | {{ item('rngtech:titanium_component_recycler') }} | |
| 7 | {{ item('rngtech:tungstensteel_component_recycler') }} | |
| 8 | {{ item('rngtech:exotic_component_recycler') }} | |

Each recipe has a minimum processing level. To run it, both the Recycler's stage and the installed Disassembly Head's processing level must reach that level.

### Gear

Powered Recyclers have a Gear tab with:

- one **Disassembly Head** slot (required). The head sets the processing level, speed, and stability. Heads range from {{ item('rngtech:iron_disassembly_head') }} to {{ item('rngtech:exotic_disassembly_head') }}, and the head's stage cannot exceed the Recycler's.
- one **Recovery Filter** slot (optional). A filter unlocks the extra returns marked "needs a Recovery Filter" in the table below. Filters run from {{ item('rngtech:iron_recovery_filter') }} to {{ item('rngtech:steel_recovery_filter') }} and are shared with the [Potential Reactor](potential-reactor.md).
- one **Battery Cell** slot (optional) for extra FE storage.

Recyclers never roll or use Super Output, so recycling can never duplicate materials.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts items to recycle. |
| Bottom | Extracts from the three output slots. |
| Sides | Powered Recyclers accept FE, for example from a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation. The Crude Recycler has no FE input, and its top is taken by the Hand Crank.

### Recycling recipes

The Component Recycler uses the `rngtech:component_recycling` recipe type. Use the filter box to find an item. The minimum processing level is listed per recipe; outputs marked with a yellow dot need a Recovery Filter.

{{ processing("component_recycling", columns=["processing_ticks", "energy", "minimum_processing_level"]) }}

## Screen

Powered Recyclers have four tabs:

- **Process**: the input, three outputs, progress, and energy. With JEI installed, click the progress bar to browse recycling recipes.
- **Gear**: Disassembly Head, Recovery Filter, and Battery Cell slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Recycler's traits with a catalyst.

The Crude Recycler has only its Process screen.

## Data values

{{ data_values() }}

## See also

- [Potential Reactor](potential-reactor.md)
- [Resonance Calibrator](resonance-calibrator.md)
- [Gear](gear.md)

{{ navbox() }}
