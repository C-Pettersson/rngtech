---
wiki:
  category: Refinement
  icon: rngtech:identifier
  ids:
    - rngtech:identifier
    - rngtech:identifying_fluid_bucket
---

# Identifier

{{ infobox(
    variants=[["Identifier", "rngtech:identifier"]],
    fields={
        "Type": "Processing machine",
        "Stages": "8",
        "Power": "FE",
        "Gear": "{{ item('rngtech:exotic_fluid_pump', 'Fluid Pump') }}, {{ item('rngtech:exotic_servo', 'Servo') }}, {{ item('rngtech:exotic_battery_cell', 'Battery Cell') }}",
        "Mastery": "Yes",
    },
) }}

The **Identifier** identifies unidentified items without a player. It uses FE and {{ item('rngtech:identifying_fluid_bucket', 'Identifying Fluid') }} for each item. It is the only way that automation can identify an item. Use it with an [Affix Sorter](affix-sorter.md) to keep the items you want from an automated crafting line.

An item identified here rolls in the same way as an item you identify by hand. Annealed items still roll their Refinement Potential twice. A Unique still gets the values that it got when it dropped. To identify an item by hand, see [Rarity and Affixes](rarity-and-affixes.md#identification).

## Obtaining

### Crafting

The Identifier comes out Unidentified and rolls as a Stage 8 machine.

{{ crafting() }}

### Breaking

Mine the Identifier with a pickaxe. It keeps its traits, Mastery, and Job Chips. The items in its slots drop. The fluid in its tank is lost.

## Usage

### Identifying Fluid

The Identifier uses Identifying Fluid. Make it in a [Melter](melter.md) from Naquadah Dust, a Calibrated Diamond Crystal, and Ender Slurry. The recipe needs an Exotic Heat Core.

{{ processing("melter", output="rngtech:identifying_fluid") }}

To fill the tank, pipe the fluid in, or put a bucket of Identifying Fluid in the bucket slot. The tank holds 8,000 mB before rolls.

### Identifying items

1. Put unidentified items in the input slot. The Identifier refuses items that are already identified.
2. Supply FE and Identifying Fluid.
3. Each item takes 5 seconds at base speed.
4. The identified item goes to the output slot.

Each item costs FE and fluid by its stage. A Stage 8 item costs 16 kFE and 100 mB. A lower-stage item costs less. Hover the fluid gauge and the energy icon to see the cost of the next item.

The Identifier stops when the output slot is full, when the tank has too little fluid, or when it has too little FE. It never voids an item.

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Fluid Pump | Yes | Any {{ item('rngtech:osmium_fluid_pump', 'Fluid Pump') }}. The pump lets fluid in through every face and sets the fill rate. |
| Servo | Yes | Any Servo. The Servo sets the speed. |
| Battery Cell | No | Any Battery Cell. Without one, the Identifier runs 20% slower. |

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert unidentified items. Accept FE. Accept Identifying Fluid when a Fluid Pump is installed. |
| Bottom | Extract identified items. Accept FE. |

Cables and hoppers cannot reach the Gear slots.

## Screen

The Identifier has six tabs:

- **Process**: the input and output slots, the fluid gauge with its purge button and bucket slot, the energy bar, and progress.
- **Gear**: the Battery Cell, Fluid Pump, and Servo slots.
- **Stats**: Processing Speed, Energy Use, Fluid Use, Energy Capacity, and Fluid Transfer.
- **Refinement**: refine the placed Identifier with a catalyst.
- **Mastery**: the shared passive tree and the Identifier's ascendancies.
- **Orders**: three drive bays for [Job Chips](work-orders.md). Identifier jobs count identified items.

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

<!-- ascendancy-trees:start -->

### Appraiser

| Node | Type | After | Effect |
|---|---|---|---|
| **Quick Glance** | Root | — | 40% increased Processing Speed; 25% increased Fluid Use. |
| Steady Feed | Small | Quick Glance | 10% increased Processing Speed. |
| **Rapid Assay** | Notable | Steady Feed | 25% increased Processing Speed. |
| Overclocked Feed | Small | Rapid Assay | 10% increased Processing Speed. |
| **Snap Judgement** | Deep notable | Overclocked Feed | 50% increased Processing Speed; 30% increased Energy Use. |
| Spark Reserve | Small | Quick Glance | 10% increased Energy Capacity. |
| **Flash Read** | Notable | Spark Reserve | +4% Instant Process. |
| Flash Bank | Small | Flash Read | +1% Instant Process. |
| **Second Look** | Deep notable | Flash Bank | +6% Instant Process. |
| Wide Intake | Small | Quick Glance | 10% increased Fluid Transfer. |
| **Buffered Vial** | Notable | Wide Intake | 100% increased Fluid Capacity. |
| Lean Draw | Small | Quick Glance | 5% reduced Energy Use. |
| **Tuned Lens** | Notable | Lean Draw | 15% reduced Energy Use. |

### Reagent Warden

| Node | Type | After | Effect |
|---|---|---|---|
| **Measured Dose** | Root | — | 30% reduced Fluid Use; 20% less Processing Speed. |
| Fine Dropper | Small | Measured Dose | 5% reduced Fluid Use. |
| **Thin Film** | Notable | Fine Dropper | 15% reduced Fluid Use. |
| Careful Pour | Small | Thin Film | 5% reduced Fluid Use. |
| **Last Drop** | Deep notable | Careful Pour | 25% reduced Fluid Use; 15% less Processing Speed. |
| Cool Coils | Small | Measured Dose | 5% reduced Energy Use. |
| **Cold Reading** | Notable | Cool Coils | 20% reduced Energy Use. |
| Idle Coils | Small | Cold Reading | 5% reduced Energy Use. |
| **Trickle Charge** | Deep notable | Idle Coils | 25% reduced Energy Use; 10% less Processing Speed. |
| Deep Well | Small | Measured Dose | 25% increased Fluid Capacity. |
| **Reservoir** | Notable | Deep Well | 150% increased Fluid Capacity. |
| Open Valve | Small | Measured Dose | 10% increased Fluid Transfer. |
| **Steady Line** | Notable | Open Valve | 40% increased Fluid Transfer; 25% increased Energy Capacity. |

<!-- ascendancy-trees:end -->

## Data values

{{ data_values() }}

## See also

- [Affix Sorter](affix-sorter.md)
- [Rarity and Affixes](rarity-and-affixes.md#identification)
- [Melter](melter.md)

{{ navbox() }}
