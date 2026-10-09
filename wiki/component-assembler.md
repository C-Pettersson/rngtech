---
wiki:
  category: Processing
  icon: rngtech:battery_assembler
  ids:
    - rngtech:battery_assembler
    - rngtech:battery_cell
    - rngtech:lubricated_frame_coupling
---

# Component Assembler

{{ infobox(
    variants=[
        ["Assembler", "rngtech:battery_assembler"],
    ],
    fields={
        "Type": "Processing machine",
        "Power": "FE",
        "Gear": "{{ item('rngtech:iron_battery_cell', 'Battery Cell') }} (optional)",
        "Mastery": "No",
    },
) }}

The **Component Assembler** is a powered workbench that combines items with a fluid. It makes {{ item('rngtech:battery_cell') }}s and the material Battery Cells built from them, Corrosion Cell Anodes, and late-game Servos and frame parts that need Lubricant.

It started life as the Battery Assembler, which is why its resource id is still `battery_assembler`. Each Component Assembler you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting("rngtech:battery_assembler") }}

### Breaking

Mine a placed Component Assembler with a pickaxe. It drops itself and keeps its rolled traits.

## Usage

### Assembling

Put the item ingredients in the four input slots on the Process tab and fill the assembly tank with the fluid the recipe needs. The machine then runs on FE and places the result in its four-slot output rack. Ingredients go in slot order: the recipe's first ingredient in the first slot, the second in the next, and so on. Leave unused input slots empty. Some recipes take more than one of an item from a single input slot, such as four Redstone for a Servo.

The tank holds 4,000 mB of one assembly fluid at a time:

- **Electrolyte Solution** for Primed Cell Cores, Anodes, and the Primed Seal Core.
- **Lubricant** for Servos, the Lubricated Seal Core, and the {{ item('rngtech:lubricated_frame_coupling') }}.

You can fill the tank three ways:

- Pipe the fluid in from any side.
- Put a filled bucket or other fluid container in the fluid slot.
- Put dry {{ item('rngtech:electrolyte') }} in the fluid slot. Each one dissolves into 125 mB of Electrolyte Solution, so you can make batteries before you reach the [Melter](melter.md). Dry electrolyte cannot make Lubricant.

To switch fluids, press the purge button on the Process tab or right-click the machine with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB and resets the current job.

### Battery Cells

Battery Cells are made in two steps:

1. Assemble a {{ item('rngtech:battery_cell') }} from a {{ item('rngtech:cell_shell') }}, a {{ item('rngtech:conductive_plate') }}, and 250 mB of Electrolyte Solution. Primed Cell Cores stack, hold no FE, and have no traits.
2. Assemble a final cell from one Primed Cell Core, the material's plate or ingot, and Redstone. Sparksteel and higher cells also need a {{ item('rngtech:stabilization_catalyst') }}.

The finished cell starts empty and rolls its own traits when assembly completes. See [Battery Cells](battery-cells.md) for each material's capacity and stage. Potato, Iron, Copper, and Lead cells also have crafting-table recipes that use dry electrolyte, so you can make your first cells without the Assembler.

### Other components

- **Anodes**: two Aluminum, Titanium, Tungstensteel, or Naquadah plates and 250 mB of Electrolyte Solution make one Anode, the fuel for a [Corrosion Cell](corrosion-cell.md).
- **Servos**: Titanium and higher Servos are assembled with Lubricant.
- **Lubricated Frame Coupling**: a Tungstensteel Casing and 1,000 mB of Lubricant, used to craft the Exotic Machine Frame.
- **Seal Cores**: the Primed and Lubricated Seal Cores used to craft Ascendancy Seals. See [Machine Mastery](machine-mastery.md#ascendancies).

### Gear

The Gear tab has one optional **Battery Cell** slot. FE you supply fills the machine's internal buffer first, then charges the cell. While running, it spends the buffer first and then draws from the cell. Without a cell, it runs on its internal buffer alone.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the four item input slots. |
| Bottom | Extracts finished items from the output rack. |
| Sides | Insert dry electrolyte or filled fluid containers into the fluid slot, and extract empty containers. |
| Any | Accepts FE and assembly fluids, for example from a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation.

### Component assembly recipes

The Component Assembler uses the `rngtech:battery_assembly` recipe type.

{{ processing("battery_assembly") }}

## Screen

The Component Assembler screen has four tabs:

- **Process**: four item inputs, the fluid slot and tank with its purge button, the output rack, progress, and energy.
- **Gear**: the Battery Cell slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Assembler's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Battery Cells](battery-cells.md)
- [Melter](melter.md), for bulk Electrolyte Solution and Lubricant.
- [Corrosion Cell](corrosion-cell.md)
- [Battery Chassis](battery-chassis.md)

{{ navbox() }}
