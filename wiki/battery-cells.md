---
wiki:
  category: Guides
  icon: rngtech:iron_battery_cell
  ids:
    - rngtech:potato_battery_cell
    - rngtech:unique_potato_battery_cell
    - rngtech:iron_battery_cell
    - rngtech:copper_battery_cell
    - rngtech:lead_battery_cell
    - rngtech:invar_battery_cell
    - rngtech:sparksteel_battery_cell
    - rngtech:arclite_battery_cell
    - rngtech:nullite_battery_cell
    - rngtech:aethergold_battery_cell
    - rngtech:exotic_battery_cell
---

# Battery Cells

**Battery Cells** are rechargeable [Gear](gear.md) that store FE. You install them in a machine's Gear tab to give it a power reserve, or fill a [Battery Chassis](battery-chassis.md) with them to build an energy bank.

Each cell is its own item with its own [rarity and affixes](rarity-and-affixes.md), so cells do not stack. A cell keeps its stored FE when you take it out, and its durability bar shows how full it is. Cells can be charged by anything that charges FE items.

## Cell tiers

Each tier stores more and moves FE faster. Values are before affixes.

| Cell | Stage | Capacity | Input | Output | Idle loss | Identity |
|---|---:|---:|---:|---:|---:|---|
| {{ item('rngtech:potato_battery_cell') }} | 0 | 1,000 FE | 8 FE/t | 4 FE/t | 12%/min | Spud Cell |
| {{ item('rngtech:iron_battery_cell') }} | 1 | 10,000 FE | 128 FE/t | 64 FE/t | 0 | Iron Cell |
| {{ item('rngtech:copper_battery_cell') }} | 2 | 12,000 FE | 256 FE/t | 128 FE/t | 0.01%/min | Conductive Cell |
| {{ item('rngtech:lead_battery_cell') }} | 3 | 32,000 FE | 256 FE/t | 128 FE/t | 0 | Dense Cell |
| {{ item('rngtech:invar_battery_cell') }} | 4 | 50,000 FE | 512 FE/t | 256 FE/t | 0 | Insulated Cell |
| {{ item('rngtech:sparksteel_battery_cell') }} | 5 | 100,000 FE | 1,024 FE/t | 512 FE/t | 0.01%/min | Balanced Cell |
| {{ item('rngtech:arclite_battery_cell') }} | 6 | 400,000 FE | 2,048 FE/t | 1,024 FE/t | 0.02%/min | Signal Cell |
| {{ item('rngtech:nullite_battery_cell') }} | 7 | 1,600,000 FE | 4,096 FE/t | 2,048 FE/t | 0.02%/min | Phase Cell |
| {{ item('rngtech:aethergold_battery_cell') }} | 7 | 1,200,000 FE | 6,144 FE/t | 3,072 FE/t | 0 | Aetherburst Cell |
| {{ item('rngtech:exotic_battery_cell') }} | 8 | 10,000,000 FE | 8,192 FE/t | 4,096 FE/t | 0 | Harmonic Cell |

- **Input** is how fast the cell can be charged, and **Output** is how fast it can give power back. Cells always charge twice as fast as they discharge.
- **Idle loss** is slow leakage. It only drains cells sitting in a Battery Chassis.
- The Aethergold Cell is a sidegrade to Nullite: less capacity, but faster transfer and no leakage.
- The Potato Cell is a novelty starter with heavy leakage.
- **Identity** is the cell's fixed material trait. Hold Left Shift over a cell to see it in the tooltip. It is a name only and changes nothing beyond the cell's base values.

### Voltaic Potato Battery Cell

The {{ item('rngtech:unique_potato_battery_cell') }} is a rare find in village chests. It cannot be crafted. It always has Unique rarity, cannot be refined, and stores 10,000 FE with 128 FE/t input, 64 FE/t output, and 6%/min idle loss. Its identity is Voltaic Spud.

## Affixes

Cells roll affixes like any other Gear. Capacity affixes come in two kinds: flat (+FE) and percentage (increased capacity). Flat bonuses apply first, so a Copper Cell with +4,000 FE and 25% increased capacity holds (12,000 + 4,000) × 1.25 = 20,000 FE. Other affixes improve transfer rates or reduce leakage. Cells can also roll Efficiency, and the Left Shift tooltip shows an Efficiency value, but it has no effect on how a cell charges or discharges. You can refine standard cells in the [Affix Forge](affix-forge.md).

## Cells in machines

Most powered machines have one Battery Cell slot on their Gear tab. FE you feed the machine fills its small internal buffer first, then charges the cell. While working, the machine spends its buffer first, then draws from the cell.

The cell's rates still apply inside a machine. It accepts at most its Input rate each tick and hands over at most its Output rate each tick. When a machine is running on stored power alone, a low-tier cell can limit how fast it works.

Solid Fuel Burners are the exception: they write the FE they generate straight into the cell, and the cell's rates do not limit their output.

### Running without a cell

The cell slot is optional on most processors, but leaving it empty has a cost. The machine keeps only its small internal buffer, and:

| Machine | Without a cell |
|---|---|
| [Crusher](crusher.md) | Output Amount drops to 75%. Crusher Battery Link affixes, and weakly the Scuffed Crush Head affix, win some of that back. The Cell Bypass keystone in [Machine Mastery](machine-mastery.md) removes the penalty. |
| Electric [Furnace](furnace.md) | 25% less Processing Speed. |
| [Alloy Furnace](alloy-furnace.md) | 15% less Processing Speed and 10% reduced Stability. |
| [Metal Press](metal-press.md) | 15% less Processing Speed and 20% reduced Stability. |
| [Resonance Calibrator](resonance-calibrator.md) | 15% less Processing Speed and 10% reduced Calibration Quality. |
| [Melter](melter.md) | 20% less Processing Speed. |
| [Component Assembler](component-assembler.md) | 15% less Processing Speed. |

Your modpack can change the Crusher and Furnace penalties.

### Which cells fit where

Most slots take any cell. These machines limit the stage:

| Machine | Cell slot |
|---|---|
| [Solid Fuel Burner](solid-fuel-burner.md) | Required. Up to the burner's stage: Stage 1 on the Crude burner, up to Stage 4 on the Steel burner. |
| [Bio Generator](bio-generator.md) | Optional, Stage 0–3. |
| [Coal Gasifier](coal-gasifier.md), [Syngas Combustor](syngas-combustor.md) | Optional, Stage 5 or higher. |
| [Steam Methane Reformer](steam-methane-reformer.md) | Optional, Stage 6 or higher. |
| [Ammonia Fuel Cell](ammonia-fuel-cell.md) | Optional, Stage 6 or higher. |
| [Compressor Tank](compressor-tank.md) (Steel and higher) | Required, up to the tank's stage. |

Modular tools and the {{ item('rngtech:miners_companion') }} also run on an installed Battery Cell. The [Tool Bench](tool-bench.md) installs cells in modular tools and charges both; the Miner's Companion takes its cell in its own Gear tab.

## Cells in a Battery Chassis

A [Battery Chassis](battery-chassis.md) holds several cells and turns them into one energy bank:

- Its capacity is the total of its cells, raised by the chassis's capacity affixes. That bonus only applies while the cells stay in that chassis.
- Its charge and discharge speed is the **combined** Input and Output of the cells that hold power. One Iron Cell gives a 64 FE/t bank no matter how big the chassis is. Four give 256 FE/t. That rate is per tick and shared by every side and connector of the chassis.
- Cells leak their idle loss here, and some chassis add, reduce, or seal leakage.

Fill a chassis with enough cells to reach the transfer rate you need, not just the capacity.

## Making cells

### Early cells

Potato, Iron, Copper, and Lead cells have crafting-table recipes that use dry Electrolyte. They roll their traits when crafted and start empty.

{{ crafting('rngtech:potato_battery_cell', 'rngtech:iron_battery_cell', 'rngtech:copper_battery_cell', 'rngtech:lead_battery_cell') }}

### Assembled cells

Every standard cell from Iron to Exotic can be made in the [Component Assembler](component-assembler.md) in two steps:

1. Assemble a {{ item('rngtech:battery_cell') }} from a Cell Shell, a Conductive Plate, and Electrolyte Solution. Primed Cell Cores stack and hold no power.
2. Assemble the final cell from one Primed Cell Core, the material's plate or ingot, and Redstone. Sparksteel and higher cells also need a Stabilization Catalyst.

The finished cell starts empty and rolls its traits when assembly completes. The [Component Assembler](component-assembler.md) page lists every assembly recipe.

{{ processing("battery_assembly", output="rngtech:sparksteel_battery_cell") }}

Unwanted standard cells can be broken down in a [Component Recycler](component-recycler.md).

## See also

- [Battery Chassis](battery-chassis.md)
- [Component Assembler](component-assembler.md)
- [Gear](gear.md)
- [Stages](stages.md)

{{ navbox() }}
