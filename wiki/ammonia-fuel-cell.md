---
wiki:
  category: Power generation
  icon: rngtech:ammonia_fuel_cell
  ids:
    - rngtech:ammonia_fuel_cell
---

# Fuel Cell

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "6",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:fuel_cell_membrane') }}, {{ item('rngtech:arclite_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Fuel Cell** is the Stage 6 generator at the end of the ammonia chain. It burns Ammonia from the [Ammonia Synthesizer](ammonia-synthesizer.md) through a Fuel Cell Membrane and makes 600 FE/t before rolls, leaving a little {{ item('rngtech:corrosion_residue') }} behind each cycle. It can also burn Hydrogen straight from the [Steam Methane Reformer](steam-methane-reformer.md), for much less FE. See [Power Chains](power-chains.md#ammonia) for the machines that feed it.

It is built as a fuel cell stack: twelve cells clamped between two steel end plates by tie rods, with copper bus bars carrying the current back to a power cabinet.

Like every RNGTech machine, each Fuel Cell you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Fuel Cell with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Fuel Cell Membrane in the Gear tab, then pipe Ammonia or Hydrogen into the 8,000 mB fuel tank. The tank holds one fuel at a time, so empty it before you switch. The status line reads "Needs Ammonia or Hydrogen" while the tank is empty.

- **Ammonia**: each cycle burns 1,000 mB and generates 216,000 FE over 18 seconds, which is 600 FE/t before membrane and machine rolls, and leaves a little Corrosion Residue. After the synthesis cost, the chain nets about 510 FE/t.
- **Hydrogen**: each cycle burns 1,000 mB and generates 60,000 FE over 15 seconds, which is 200 FE/t before rolls. It leaves no residue.

You can read the cell from the outside. Facing the front:

- An installed membrane shows as green-and-white lines between the dark plates of the stack. With no membrane, the slots between the plates are empty.
- The glass tank at the back right shows the fuel level, in the colour of the fuel it holds.
- Corrosion Residue collects in the glass trap at the back left, and vapour rises from the vent stack above it while the cell runs.
- The lamp on the cabinet lights while the cell runs.

The cell pushes FE out of its top and sides. It does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. It stops when its internal storage and Battery Cell are full or when the residue slot is full, so keep both drained. The Process tab has a purge button for the fuel tank. Purging resets the cycle in progress.

### Ammonia or Hydrogen

Hydrogen is worth more as Ammonia. Turned into Ammonia in the [Ammonia Synthesizer](ammonia-synthesizer.md) and burned here, each mB of Hydrogen makes about 136 FE after the synthesis cost. Burned directly, it makes 60 FE. Burning Hydrogen skips the Cavitation Generator and the Synthesizer, so it suits a small build or a Reformer with Hydrogen to spare. Once your Ammonia chain runs, send the Hydrogen there instead.

### Gear

- **Fuel Cell Membrane** (required): gates recipes by stage. The membrane has neutral base stats, so an unrolled one only unlocks the cell. Its rolled Energy Generation, Processing Speed, and Efficiency affixes apply to the fuel cell. An older membrane can still show Stability or Fluid Transfer. Those affixes do nothing.
- **Battery Cell** (optional): adds FE storage. Only Stage 6 and higher cells fit.

### Automation

| Side | Behavior |
|---|---|
| Top | Outputs FE. |
| Bottom | Extracts Corrosion Residue. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |
| Any | Fills the fuel tank with Ammonia or Hydrogen through pipes. |

Gear slots are not reachable by automation. The back of the cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

### Fuel Cell Power recipes

The Fuel Cell uses its own recipe type, `rngtech:ammonia_power_cycle`, shown in JEI as Fuel Cell Power.

{{ processing("ammonia_power_cycle") }}

## Screen

The Fuel Cell screen has four tabs:

- **Process**: the fuel tank with its purge button, the residue slot, FE, and cycle progress. Click the recipe line to open its JEI category.
- **Gear**: Fuel Cell Membrane and Battery Cell slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed fuel cell's traits with a catalyst.

A {{ item('rngtech:purge_bucket') }} used on the fuel cell voids up to 1,000 mB of fuel.

## Data values

{{ data_values() }}

## See also

- [Ammonia Synthesizer](ammonia-synthesizer.md)
- [Steam Methane Reformer](steam-methane-reformer.md), for Hydrogen.
- [Stages](stages.md), for the Stage 6 power band.

{{ navbox() }}
