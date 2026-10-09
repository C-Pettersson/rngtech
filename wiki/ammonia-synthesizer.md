---
wiki:
  category: Fluids and gases
  icon: rngtech:ammonia_synthesizer
  ids:
    - rngtech:ammonia_synthesizer
    - rngtech:ammonia_catalyst_bed
---

# Ammonia Synthesizer

{{ infobox(
    variants=[
        ["Synthesizer", "rngtech:ammonia_synthesizer"],
        ["Catalyst Bed", "rngtech:ammonia_catalyst_bed"],
    ],
    fields={
        "Type": "Gas processor",
        "Stage": "6",
        "Power": "FE consumer",
        "Gear": "{{ item('rngtech:ammonia_catalyst_bed', 'Ammonia Catalyst Bed') }}",
        "Mastery": "No",
    },
) }}

The **Ammonia Synthesizer** combines Nitrogen and Hydrogen into Ammonia using FE. It is the first half of the ammonia power chain: the Ammonia it makes fuels the [Ammonia Fuel Cell](ammonia-fuel-cell.md), a strong late-game generator.

Each Synthesizer you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and you can refine the placed machine.

## Obtaining

### Crafting

The Synthesizer recipe uses up one {{ item('rngtech:ammonia_catalyst_bed') }}, so craft two: one for the recipe and one for the Gear slot.

{{ crafting() }}

### Breaking

Mine the Synthesizer with a pickaxe. It drops itself with its rolled traits, plus its Catalyst Bed.

## Usage

### Making Ammonia

1. Install an Ammonia Catalyst Bed in the Gear tab. The Synthesizer does nothing without one.
2. Pipe Nitrogen and Hydrogen into the machine. Each has its own 8,000 mB tank.
3. Supply FE from any side except the bottom.

Each cycle turns 500 mB of Nitrogen and 1,500 mB of Hydrogen into 1,000 mB of Ammonia, using 12,000 FE over 400 ticks. The Ammonia collects in an 8,000 mB output tank; pipe it out to your Fuel Cells. The machine stops when the output tank is full.

Synthesis uses the `rngtech:ammonia_synthesis` recipe type.

{{ processing("ammonia_synthesis") }}

### Getting the gases

- **Nitrogen**: separate it from water in a Stage 6 [Cavitation Generator](cavitation-generator.md) fitted with a Nitrogen Extraction Rotor and a Nitrogen Separation Nozzle.
- **Hydrogen**: from the [Steam Methane Reformer](steam-methane-reformer.md).

### Gear

The Gear tab has one required **Catalyst Bed** slot. The bed's stage must be high enough for the recipe; the default recipe needs a Stage 6 bed, which is what the Ammonia Catalyst Bed is.

An unrolled Catalyst Bed only unlocks recipes. Its rolled Processing Speed and Energy Usage modifiers speed up the Synthesizer or cut its FE cost. Other rolls on the bed do nothing here. See [Ammonia Fuel Cell](ammonia-fuel-cell.md) for details.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts an Ammonia Catalyst Bed into the Gear slot. Accepts FE. |
| Sides | Accept FE. |
| Any | Fills Nitrogen and Hydrogen, and drains Ammonia. |

There are no process item slots to automate. To throw gas away, use the purge buttons beside each tank, or right-click the machine with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB. A normal right-click empties Nitrogen or Hydrogen first, and sneaking empties the Ammonia tank first. Purging Nitrogen or Hydrogen resets the cycle in progress.

## Screen

The Ammonia Synthesizer screen has four tabs:

- **Process**: Nitrogen, Hydrogen, and Ammonia tanks with purge buttons, progress, FE, and status.
- **Gear**: the Catalyst Bed slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Synthesizer's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Ammonia Fuel Cell](ammonia-fuel-cell.md)
- [Gear](gear.md)

{{ navbox() }}
