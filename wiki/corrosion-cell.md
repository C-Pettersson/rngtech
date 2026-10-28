---
wiki:
  category: Power generation
  icon: rngtech:corrosion_cell
  ids:
    - rngtech:corrosion_cell
---

# Corrosion Cell

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "4",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:osmium_fluid_pump', 'Fluid Pump') }}, {{ item('rngtech:aluminum_cathode', 'Cathode') }}",
        "Mastery": "No",
    },
) }}

The **Corrosion Cell** is a Stage 4 generator that makes FE by eating metal, the way an aluminium-air battery does. Each cycle it dissolves one plate, Anode, or piece of Scrap in electrolyte, produces a batch of FE, and leaves behind {{ item('rngtech:corrosion_residue') }}. Early on it burns plain Iron and Copper Plates. From Stage 5 you feed it Anodes, which need a matching Cathode installed. You choose how hard to run it: full rated load for the most FE/t, or a lighter load for more FE out of every fuel. See [Power Chains](power-chains.md#corrosion) for the machines that make its fuel.

You can watch it work. The fuel hangs from the copper bus bar in a glass electrolyte bath and dissolves from the bottom up. Hydrogen bubbles stream off it, more of them at a higher load, and a faint mist rises from the vent stack at the back. The electrolyte turns cloudy as it fills with dissolved metal and clears with each fresh dose. The installed Cathode lines the back of the bath in its metal's colour, and the blower on top that feeds it air spins with the current. The ammeter on the front shows the current, and the lamp in the other front corner lights while the cell works. Residue settles in the sludge trap at the bottom of the bath as the residue slot fills, and the sight glass on the side shows the electrolyte tank.

Like every RNGTech machine, each Corrosion Cell you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Corrosion Cell with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Each cycle needs two things:

- **Fuel**: one plate, Anode, or {{ item('rngtech:scrap') }} in the plate slot.
- **Electrolyte**: one {{ item('rngtech:electrolyte') }} in the electrolyte slot, or 125 mB of Electrolyte Solution from the internal tank. The dry item is used first when both are present.

The cell then generates the fuel's FE over the cycle and drops one Corrosion Residue into the output slot. It pulls the next fuel only when the residue slot has room and its internal buffer or installed Battery Cell can still take FE. If the buffer fills mid-cycle, the fuel stops dissolving until FE is drawn away, so no FE is lost. Clear the residue and draw FE away, or the cell sits idle. The cell does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it, and it resumes the same fuel when the signal ends.

The electrolyte tank holds 4,000 mB. Fluid Capacity rolls on the cell or its Fluid Pump make it bigger. To empty it, use the purge button on the Process tab or right-click the cell with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Load

The load is the share of its rated current the cell draws. Each fuel holds a fixed amount of charge, so the load sets how fast the fuel dissolves. A lighter load also runs the cell at a higher voltage, so each fuel is worth more FE. Rated load (100%) makes the most FE/t and gives exactly the recipe's FE and cycle time. New cells start there.

Lower loads trade FE/t for FE per fuel. Two cells at 50% together make 1.125 times the FE/t of one cell at rated load, and they get 12.5% more FE out of every fuel. That pays off when fuel, such as Anodes, runs short before cells do. Each fuel still uses one Electrolyte and leaves one Corrosion Residue, so a lighter load also gets more FE from each Electrolyte.

| Load | FE/t | FE per fuel | Cycle length |
|---|---|---|---|
| 40% | x0.46 | x1.15 | x2.50 |
| 50% | x0.56 | x1.125 | x2.00 |
| 60% | x0.66 | x1.10 | x1.67 |
| 70% | x0.75 | x1.075 | x1.43 |
| 80% | x0.84 | x1.05 | x1.25 |
| 90% | x0.92 | x1.025 | x1.11 |
| 100% (rated) | x1.00 | x1.00 | x1.00 |

Set the load on the gauge below the cycle bar in the Process tab. Click a point on it, or scroll over it to move 5%. The load runs from 40% to 100%; the table shows every 10%. The gauge draws the trade: the teal profile is the FE/t at each load, lit up to the current the cell draws right now, and the amber line is the FE per fuel. The white pointer marks your setting. Hover the gauge for the exact numbers and the cell voltage. A load change applies to the rest of the fuel already in the bath.

### Fuel tiers

Plain plates burn on their own. From Stage 5, the main fuel is an **Anode**: two plates of one metal assembled with 250 mB of Electrolyte Solution and 4,800 FE in the [Component Assembler](component-assembler.md). An Anode burns for twice as long as a plate and produces far more FE, but it only burns when the installed Cathode is at least the Anode's stage. A higher-tier plate still burns without a Cathode, at roughly 40% of its Anode's FE/t.

| Fuel | Stage | FE/t (base, rated load) | Cathode needed |
|---|---:|---:|---|
| {{ item('rngtech:scrap') }} | 4 | 30 | None |
| {{ item('rngtech:iron_plate') }} | 4 | 80 | None |
| {{ item('rngtech:copper_plate') }} | 4 | 100 | None |
| {{ item('rngtech:aluminum_anode') }} | 5 | 200 | {{ item('rngtech:aluminum_cathode') }} or better |
| {{ item('rngtech:titanium_anode') }} | 6 | 480 | {{ item('rngtech:titanium_cathode') }} or better |
| {{ item('rngtech:tungstensteel_anode') }} | 7 | 900 | {{ item('rngtech:tungstensteel_cathode') }} or better |
| {{ item('rngtech:naquadah_anode') }} | 8 | 1,800 | {{ item('rngtech:naquadah_cathode') }} |

Values are before machine and Cathode rolls. Every Anode tier stays below that stage's main generator once you count its assembly cost.

### Gear

The Gear tab has three optional slots:

- **Battery Cell**: adds FE storage. It accepts cells up to Stage 5.
- **Fluid Pump**: lets pipes fill the electrolyte tank from any face. Without one, the cell runs on dry Electrolyte items. The pump's Fluid Transfer sets the fill rate.
- **Cathode**: sets the highest Anode stage the cell can burn. Its rolled Energy Generation, Efficiency, Processing Speed, and Stability affixes apply to the cell, and each stage above Aluminum adds 2% base Efficiency.

None of the Gear is required to burn plates, Scrap, and dry Electrolyte.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts plates, Anodes, and Scrap. |
| Bottom | Extracts Corrosion Residue. |
| Sides | Insert plates, Anodes, Scrap, or Electrolyte. |
| Every face | Outputs FE, for example into a [Universal Cable](universal-cable.md). Fills Electrolyte Solution when a Fluid Pump is installed. |

A connector works on any face. The port on the back, a dark plate with a teal ring, marks the tidy spot for one, clear of the tank window and the blower.

Gear slots are not reachable by automation.

### Corrosion Cell recipes

The Corrosion Cell uses its own recipe type, `rngtech:corrosion_cell`. Every recipe takes one Electrolyte (or 125 mB of Electrolyte Solution) and returns one Corrosion Residue.

{{ processing("corrosion_cell", hide=["minimum_material_stage"]) }}

## Screen

The Corrosion Cell screen has four tabs:

- **Process**: the plate and electrolyte slots, the electrolyte tank with its purge button, the residue slot, FE, cycle progress, and the load gauge. Click the recipe line to open its JEI category.
- **Gear**: Battery Cell, Fluid Pump, and Cathode slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed cell's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Component Assembler](component-assembler.md), which builds Anodes.
- [Machine Stats](machine-stats.md)

{{ navbox() }}
