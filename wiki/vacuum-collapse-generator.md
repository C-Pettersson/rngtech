---
wiki:
  category: Power generation
  icon: rngtech:vacuum_collapse_generator
  ids:
    - rngtech:vacuum_collapse_generator
---

# Vacuum Collapse Generator

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "7",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:tungstensteel_void_chamber', 'Void Chamber') }}, {{ item('rngtech:tungstensteel_collapse_nozzle', 'Collapse Nozzle') }}, {{ item('rngtech:tungstensteel_dimensional_stabilizer', 'Dimensional Stabilizer') }}",
        "Mastery": "No",
    },
) }}

The **Vacuum Collapse Generator** is RNGTech's Stage 7 primary generator. It collapses one {{ item('rngtech:void_catalyst') }} at a time into a huge batch of FE, about 1,000 FE/t before Gear, and can leave {{ item('rngtech:collapse_residue') }} behind. Its Gear decides how much it makes and how stable each collapse is.

The generator is built around its Gear. The Void Chamber is the glass vessel in the middle, a Dimensional Stabilizer pylon stands on each side, and the Collapse Nozzle hangs over the chamber from the catalyst hopper's feed line. Each part shows in its own material, so you can tell the tier at a glance, and an empty Gear slot leaves an empty mount.

Like every RNGTech machine, each Vacuum Collapse Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

The recipe itself uses a full set of Tungstensteel Gear, so you will need to craft a second set to run it.

{{ crafting() }}

### Breaking

Mine a placed Vacuum Collapse Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Void Chamber, a Collapse Nozzle, and a Dimensional Stabilizer in the Gear tab, then put Void Catalyst in the input slot. Each catalyst starts one collapse, which generates its FE over the cycle and then drops any residue into the output slot.

A collapse starts only when:

- the Void Chamber's stage meets the recipe's minimum (Stage 7 for Void Catalyst),
- the machine's stability is at least the recipe's minimum (1.0 for Void Catalyst), and
- the residue slot has room.

During a collapse, a glowing core inside the chamber shrinks and darkens to black as the cycle runs, and the chamber's cap rims pulse faster toward the end. The stabilizer lenses glow and draw motes into the core.

Once a collapse starts it cannot pause for full storage. FE that the 20,000 FE internal buffer cannot hold is lost, so connect enough cables and storage to take each catalyst's output. The generator does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it, even in the middle of a collapse, which is the way to hold it while storage catches up.

### The collapse curve

A collapse does not make its FE evenly. Output starts low and climbs to a peak at the very end of the cycle. At rated depth it starts at 0.4 times the collapse's average FE/t and ends at twice the average. Every collapse still makes all of its FE, but your connector and storage have to take the peak, or the part they cannot take is lost.

The Process tab draws the curve above the progress bar. The lit part is what the running collapse has already made, and the faint line marks its average. Hover it to see the starting and peak FE/t. While a collapse runs, the generation icon shows the FE/t at that moment.

### Collapse depth

The depth gauge under the status icons sets how deep each collapse goes, from 50% to 150%. Click or scroll on it (hold Shift to step by 5). A collapse keeps the depth it started with, so a change applies from the next catalyst.

Deeper collapses draw more FE from each catalyst, but they take longer, peak harder, and multiply the recipe's instability. Shallow collapses spend catalysts faster for a flatter, gentler curve. Depth hardly changes the average FE/t, so the choice is catalysts against how hard a burst your network can take.

| Depth | FE per catalyst | Cycle | Instability | Start | Peak |
|---|---:|---:|---:|---:|---:|
| 50% (shallow) | ×0.66 | ×0.71 | ×0.25 | 0.7× average | 1.3× average |
| 75% | ×0.84 | ×0.87 | ×0.56 | 0.55× average | 1.65× average |
| 100% (rated, default) | ×1.00 | ×1.00 | ×1.00 | 0.4× average | 2× average |
| 125% | ×1.14 | ×1.12 | ×1.56 | 0.25× average | 2.35× average |
| 150% (deep) | ×1.28 | ×1.22 | ×2.25 | 0.1× average | 2.7× average |

Better stability takes the edge off deep collapses, since stability divides the recipe's instability after depth multiplies it. Hover the gauge to see the effects of the current setting.

### Instability

Every recipe adds instability, collapse depth multiplies it, and higher stability reduces it. Instability makes each collapse slower and yield less FE, and every full point of it adds an extra Collapse Residue. If the installed Collapse Nozzle's stage is lower than the recipe's minimum chamber stage, each missing stage adds a full point of instability. An unstable collapse makes the chamber's rim light flicker instead of pulsing smoothly.

### Gear

| Slot | Accepts | Role |
|---|---|---|
| Void Chamber | Tungstensteel, Nullite, or Exotic Void Chambers | Required. Sets the stage, generation, and stability. |
| Collapse Nozzle | Steel through Exotic Collapse Nozzles. The Nitrogen Separation Nozzle is rejected. | Required. Tunes generation, speed, and stability. |
| Dimensional Stabilizer | Tungstensteel, Nullite, or Exotic Dimensional Stabilizers | Raises stability and efficiency. Void Catalyst only leaves residue while one is installed. |

Chamber and nozzle generation multiply each other. Unmodified Tungstensteel Gear reaches about 1,057 FE/t, and unmodified Exotic Gear about 3,000 FE/t. Rolled and refined parts push beyond that. See [Stages](stages.md) for the Stage 7 power band.

The Collapse Nozzle is shared with the [Cavitation Generator](cavitation-generator.md).

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts catalysts. |
| Bottom | Extracts Collapse Residue. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation. The back of the machine has a port plate sized for a Universal Connector, the tidiest place to take FE from it.

### Void Catalyst

{{ crafting('rngtech:void_catalyst') }}

### Vacuum Collapse recipes

The Vacuum Collapse Generator uses its own recipe type, `rngtech:vacuum_collapse`.

{{ processing("vacuum_collapse", hide=["residue_requires_stabilizer"]) }}

## Screen

The Vacuum Collapse Generator screen has four tabs:

- **Process**: the catalyst and residue slots, FE, the collapse curve, cycle progress, instability, and the depth gauge. The generation icon shows the FE/t generated right now, and the output icon shows the FE exported last tick. Hover the curve and the gauge for details. Click the recipe line to open its JEI category.
- **Gear**: Void Chamber, Collapse Nozzle, and Dimensional Stabilizer slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Gear](gear.md)
- [Potential Reactor](potential-reactor.md), whose Containment Linings are separate from the Dimensional Stabilizer.

{{ navbox() }}
