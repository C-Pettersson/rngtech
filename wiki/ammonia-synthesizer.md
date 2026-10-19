---
wiki:
  category: Fluids and gases
  icon: rngtech:ammonia_synthesizer
  ids:
    - rngtech:ammonia_synthesizer
    - rngtech:ammonia_catalyst_bed
    - rngtech:urea
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

The **Ammonia Synthesizer** combines Nitrogen and Hydrogen into Ammonia using FE. It is the first half of the ammonia power chain: the Ammonia it makes fuels the [Fuel Cell](ammonia-fuel-cell.md), a strong late-game generator. It can also combine Ammonia with Carbon Exhaust into {{ item('rngtech:urea') }}, a fertilizer.

It is built as an ammonia loop: a compressor pushes the mixed gases into a thick-walled converter that holds the catalyst, and the hot gas passes through a frosted condenser into a glass Ammonia receiver.

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

You can read the Synthesizer from the outside. Facing the front:

- The windows on the converter show the installed Catalyst Bed as three packed layers, which glow while the Synthesizer runs. With no bed, they show an empty mesh.
- The compressor flywheel on the right turns while it runs, and the pressure gauge on the left rises with it.
- The two glasses at the back right show the Nitrogen and Hydrogen levels, and the receiver at the back left the Ammonia level.
- Frost drifts off the condenser on top while it runs, and the lamp at the front lights.

Synthesis uses the `rngtech:ammonia_synthesis` recipe type.

{{ processing("ammonia_synthesis") }}

### Making Urea

Pipe Carbon Exhaust into the Synthesizer's 8,000 mB Carbon Exhaust tank. Each Urea cycle takes 250 mB of Ammonia from the Synthesizer's own Ammonia tank and 250 mB of Carbon Exhaust, and makes one Urea in the Urea slot on the Process tab, using 3,000 FE over 120 ticks. It needs the same Stage 6 Catalyst Bed as Ammonia.

Urea comes first. Whenever the Ammonia tank and the Carbon Exhaust tank both hold enough and the Urea slot has room, the Synthesizer makes Urea; otherwise it makes Ammonia. Switching between the two restarts the cycle in progress. A full Urea slot does not stop the machine: it goes back to making Ammonia.

You can also pipe Ammonia into the Synthesizer. A Synthesizer with no Nitrogen or Hydrogen then works as a dedicated Urea maker, fed from another Synthesizer's Ammonia.

Urea is not free. The 250 mB of Ammonia in each Urea would make 54,000 FE in a Fuel Cell, so make Urea only as fast as you use it.

Urea synthesis uses the `rngtech:urea_synthesis` recipe type.

{{ processing("urea_synthesis") }}

### Using Urea

- **By hand or from a dispenser**, Urea works like bone meal on any plant.
- In the [Forestry Cart Station](forestry-cart-station.md#mastery-and-ascendancies), the Forestry Companion and its station take Urea wherever they take bone meal. Each Urea counts as 4 bone meal.
- In the [Algae Photobioreactor](algae-photobioreactor.md#urea), each Urea grows the next 4 Algae Biomass at twice the speed.
- In a crafting grid, one Urea turns four Composted Biomass into four [Rich Biomass](bio-generator.md#fuel-chains).

### Getting the gases

- **Nitrogen**: separate it from water in a Stage 6 [Cavitation Generator](cavitation-generator.md) fitted with a Nitrogen Extraction Rotor and a Nitrogen Separation Nozzle.
- **Hydrogen**: from the [Steam Methane Reformer](steam-methane-reformer.md).
- **Carbon Exhaust**, for Urea: from a [Syngas Combustor](syngas-combustor.md), a [Wooden Dehumidifier](wooden-dehumidifier.md), a Steam Methane Reformer running the [water-gas shift](steam-methane-reformer.md#water-gas-shift), or a [Bio Generator](bio-generator.md#methane-take-off) with a Biogas Membrane.

### Gear

The Gear tab has one required **Catalyst Bed** slot. The bed's stage must be high enough for the recipe; the default recipe needs a Stage 6 bed, which is what the Ammonia Catalyst Bed is.

An unrolled Catalyst Bed only unlocks recipes. Its rolled Processing Speed and Energy Usage modifiers speed up the Synthesizer or cut its FE cost. Other rolls on the bed do nothing here.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts an Ammonia Catalyst Bed into the Gear slot. Accepts FE. |
| Bottom | Extracts Urea. |
| Sides | Accept FE. Extract Urea. |
| Any | Fills Nitrogen, Hydrogen, Carbon Exhaust, and Ammonia, and drains Ammonia. |

Nothing can insert into the Urea slot; you and automation can only take from it. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one. To throw gas away, use the purge buttons beside each tank, or right-click the machine with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB. A normal right-click empties an input tank first, and sneaking empties the Ammonia tank first. Purging Nitrogen, Hydrogen, or Carbon Exhaust resets the cycle in progress.

## Screen

The Ammonia Synthesizer screen has four tabs:

- **Process**: Nitrogen, Hydrogen, Ammonia, and Carbon Exhaust tanks with purge buttons, the Urea slot, progress, FE, and status.
- **Gear**: the Catalyst Bed slot.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Synthesizer's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Fuel Cell](ammonia-fuel-cell.md)
- [Gear](gear.md)

{{ navbox() }}
