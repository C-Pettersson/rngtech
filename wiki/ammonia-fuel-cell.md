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

The **Ammonia Fuel Cell** is the Stage 6 generator at the end of the ammonia chain. It burns Ammonia from the [Ammonia Synthesizer](ammonia-synthesizer.md) through a Fuel Cell Membrane and makes 600 FE/t before rolls, leaving a little {{ item('rngtech:corrosion_residue') }} behind each cycle.

Like every RNGTech machine, each Ammonia Fuel Cell you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Ammonia Fuel Cell with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### The ammonia chain

The fuel cell is the last step of the chain:

1. A [Cavitation Generator](cavitation-generator.md) with nitrogen Gear splits Water into Nitrogen.
2. A [Steam Methane Reformer](steam-methane-reformer.md) makes Hydrogen.
3. The [Ammonia Synthesizer](ammonia-synthesizer.md) combines them into Ammonia, using FE.
4. The Ammonia Fuel Cell burns the Ammonia for more FE than the synthesis cost.

### Generating power

Install a Fuel Cell Membrane in the Gear tab, then pipe Ammonia into the 8,000 mB tank. Each cycle burns 1,000 mB of Ammonia and generates 216,000 FE over 18 seconds, which is 600 FE/t before membrane and machine rolls. After the synthesis cost, the chain nets about 510 FE/t.

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

Gear slots are not reachable by automation.

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
