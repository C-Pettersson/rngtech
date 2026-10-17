---
wiki:
  category: Power generation
  icon: rngtech:ammonia_fuel_cell
  ids:
    - rngtech:ammonia_fuel_cell
---

# Ammonia Fuel Cell

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "6",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:fuel_cell_membrane') }}, {{ item('rngtech:arclite_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Ammonia Fuel Cell** is the Stage 6 generator at the end of the ammonia chain. It burns Ammonia from the [Ammonia Synthesizer](ammonia-synthesizer.md) through a Fuel Cell Membrane and makes 600 FE/t before rolls, leaving a little {{ item('rngtech:corrosion_residue') }} behind each cycle. See [Power Chains](power-chains.md#ammonia) for the machines that feed it.

It is built as a fuel cell stack: twelve cells clamped between two steel end plates by tie rods, with copper bus bars carrying the current back to a power cabinet.

Like every RNGTech machine, each Ammonia Fuel Cell you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Ammonia Fuel Cell with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Fuel Cell Membrane in the Gear tab, then pipe Ammonia into the 8,000 mB tank. Each cycle burns 1,000 mB of Ammonia and generates 216,000 FE over 18 seconds, which is 600 FE/t before membrane and machine rolls. After the synthesis cost, the chain nets about 510 FE/t.

You can read the cell from the outside. Facing the front:

- An installed membrane shows as green-and-white lines between the dark plates of the stack. With no membrane, the slots between the plates are empty.
- The glass tank at the back right shows the Ammonia level.
- Corrosion Residue collects in the glass trap at the back left, and vapour rises from the vent stack above it while the cell runs.
- The lamp on the cabinet lights while the cell runs.

The cell pushes FE out of its top and sides. It does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. It stops when its internal storage and Battery Cell are full or when the residue slot is full, so keep both drained. The Process tab has a purge button for the Ammonia tank. Purging resets the cycle in progress.

### Gear

- **Fuel Cell Membrane** (required): gates recipes by stage. The membrane has neutral base stats, so an unrolled one only unlocks the cell. Its rolled Energy Generation, Processing Speed, and Efficiency affixes apply to the fuel cell. Other rolls, such as Stability or Fluid Transfer, do nothing here.
- **Battery Cell** (optional): adds FE storage. Only Stage 6 and higher cells fit.

### Automation

| Side | Behavior |
|---|---|
| Top | Outputs FE. |
| Bottom | Extracts Corrosion Residue. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |
| Any | Fills the Ammonia tank through pipes. |

Gear slots are not reachable by automation. The back of the cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

### Ammonia Power Cycle recipes

The Ammonia Fuel Cell uses its own recipe type, `rngtech:ammonia_power_cycle`.

{{ processing("ammonia_power_cycle") }}

## Screen

The Ammonia Fuel Cell screen has four tabs:

- **Process**: the Ammonia tank with its purge button, the residue slot, FE, and cycle progress. Click the recipe line to open its JEI category.
- **Gear**: Fuel Cell Membrane and Battery Cell slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed fuel cell's traits with a catalyst.

A {{ item('rngtech:purge_bucket') }} used on the fuel cell voids up to 1,000 mB of Ammonia.

## Data values

{{ data_values() }}

## See also

- [Ammonia Synthesizer](ammonia-synthesizer.md)
- [Stages](stages.md), for the Stage 6 power band.

{{ navbox() }}
