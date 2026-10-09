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

The **Corrosion Cell** is a Stage 4 generator that makes FE by eating metal. Each cycle it dissolves one plate, Anode, or piece of Scrap in electrolyte, produces a batch of FE, and leaves behind {{ item('rngtech:corrosion_residue') }}. Early on it burns plain Iron and Copper Plates. From Stage 5 you feed it Anodes, which need a matching Cathode installed.

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

The cell then generates the recipe's FE over the cycle and drops one Corrosion Residue into the output slot. It pulls the next fuel only when the residue slot has room and its internal buffer or installed Battery Cell can still take FE. Clear the residue and draw FE away, or the cell sits idle. A redstone signal pauses it.

The electrolyte tank holds 4,000 mB, and a Fluid Pump's Fluid Capacity rolls make it bigger. To empty it, use the purge button on the Process tab or right-click the cell with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Fuel tiers

Plain plates burn on their own. From Stage 5, the main fuel is an **Anode**: two plates of one metal assembled with Electrolyte Solution in the [Component Assembler](component-assembler.md). An Anode burns for twice as long as a plate and produces far more FE, but it only burns when the installed Cathode is at least the Anode's stage. A higher-tier plate still burns without a Cathode, at roughly 40% of its Anode's FE/t.

| Fuel | Stage | FE/t (base) | Cathode needed |
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
- **Fluid Pump**: lets pipes fill the electrolyte tank from the sides. Without one, the cell runs on dry Electrolyte items. The pump's Fluid Transfer sets the fill rate.
- **Cathode**: sets the highest Anode stage the cell can burn. Its rolled Energy Generation, Efficiency, Processing Speed, and Stability affixes apply to the cell, and each stage above Aluminum adds a little base Efficiency.

None of the Gear is required to burn plates, Scrap, and dry Electrolyte.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts plates, Anodes, and Scrap. |
| Bottom | Extracts Corrosion Residue. |
| Sides | Insert plates, Anodes, Scrap, or Electrolyte. Fill Electrolyte Solution when a Fluid Pump is installed. Output FE, for example into a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation.

### Corrosion Cell recipes

The Corrosion Cell uses its own recipe type, `rngtech:corrosion_cell`. Every recipe takes one Electrolyte (or 125 mB of Electrolyte Solution) and returns one Corrosion Residue.

{{ processing("corrosion_cell", hide=["minimum_material_stage"]) }}

## Screen

The Corrosion Cell screen has four tabs:

- **Process**: the plate and electrolyte slots, the electrolyte tank with its purge button, the residue slot, FE, and cycle progress. Click the recipe line to open its JEI category.
- **Gear**: Battery Cell, Fluid Pump, and Cathode slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed cell's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Component Assembler](component-assembler.md), which builds Anodes.
- [Machine Stats](machine-stats.md)

{{ navbox() }}
