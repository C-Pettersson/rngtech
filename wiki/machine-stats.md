---
wiki:
  category: Guides
  icon: rngtech:iron_heat_core
---

# Machine Stats

Every RNGTech machine, part, Battery Cell, and tool works from a set of **stats**. This page is a glossary of them: what each one does, which machines use it, and whether you want it higher or lower.

## How stats are built

A machine's final stats come from several layers:

1. **Base stats** from the machine's material and stage.
2. **Installed [Gear](gear.md)**: each part's own base stats plus its rolled affixes.
3. The machine's own **[affixes](rarity-and-affixes.md)**.
4. **[Machine Mastery](machine-mastery.md)** passives and [ascendancies](machine-mastery.md#ascendancies).

Open a machine's **Stats** tab to see the result. Hover a row to see what the stat does. Stats that only exist on some machines appear only where they apply.

The **vs base** column shows how far each stat sits from its base: a step for whole-number stats like Input Slots, otherwise a percent change. Green means the change helps and red means it costs, so a higher Energy Use shows red. Hold **Shift** while hovering a row to see the math behind it. The breakdown lists the base value, each flat addition, the increased and more modifiers, and any fixed value or cap. Every line names its source: an affix, an installed part, a Mastery node, an ascendancy, or a penalty such as a missing Battery Cell. Each installed part counts as one line. Its own affixes are already included in that line, so hover the part itself to see them.

Affixes change stats in four ways. **Increased** and **reduced** percentages add together, while **more** and **less** multiply separately on top. Flat additions, such as "+2,000 FE", come first. Reduced Energy Use divides the cost instead, so it never reaches zero. See [Rarity and Affixes](rarity-and-affixes.md#reading-the-tooltip).

In the tables below, **Better** says which direction you want.

## Processing

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Processing Speed | How fast a recipe or work cycle runs, shown as how much faster than the recipe's listed time: +50% faster finishes in two thirds of it. Better chassis start above the listed time; Shift-hover shows the chassis base multiplied by the bonuses. | Every recipe machine, including generators that run recipes, and the Forestry Companion's work interval. | Higher |
| Energy Use | Multiplier on the FE each craft costs. Increases multiply it; reductions add together and then divide it, so 100% reduced halves the cost. | Powered processing machines, such as the [Crusher](crusher.md), [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Melter](melter.md), [Metal Press](metal-press.md), [Component Assembler](component-assembler.md), [Component Recycler](component-recycler.md), [Resonance Calibrator](resonance-calibrator.md), and Steel and higher [Compressor Tanks](compressor-tank.md). | Lower |
| Processing Level | The hardness or tier an installed part lets the machine handle. Shown as **Hardness** on the Crusher. Set by Crush Heads, Reactor Chambers, and Disassembly Heads; affixes never raise it. | [Crusher](crusher.md), [Melter](melter.md), [Potential Reactor](potential-reactor.md), [Component Recycler](component-recycler.md), and others with a level gate. | Higher |
| Batch Size | How many items from one input stack the machine works in a single cycle. Each item still pays its own FE and earns its own XP. Starts at 1, up to 16. | [Crusher](crusher.md), [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Melter](melter.md), [Metal Press](metal-press.md), [Resonance Calibrator](resonance-calibrator.md). | Higher |
| Batch Overhead | Extra cycle time for each item past the first in a batch, as a percentage of one item's time. Starts at 25%, never below 5%. The path to the Dense Batching keystone lowers it. | The same machines as Batch Size. | Lower |
| Input Slots | Active input lanes or slots. Sets the Furnace's smelting lanes (four on the Lead Furnace), the Alloy Furnace's inputs (from its Alloy Crucible), and a Solid Fuel Burner's fuel slots (from its Fuel Box). | [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Solid Fuel Burner](solid-fuel-burner.md). | Higher |
| Bonus Output | Extra output per recipe. Fractions are banked and paid out as whole items. On the Crusher, increases have diminishing returns; see [Output amount](crusher.md#output-amount). On the Cavitation Generator it multiplies the nitrogen made per cycle. | [Crusher](crusher.md), [Cavitation Generator](cavitation-generator.md). | Higher |
| Super Output | Chance to add one extra copy of the recipe's base output. Separate from Bonus Output. | Item-processing machines. | Higher |
| Instant Process | Chance for a process to finish the moment it starts. Powered machines need enough FE for the whole craft up front. | Item-processing machines. | Higher |
| Stability | General reliability. Its effect depends on the machine: see [Stability](#stability) below. | Many machines and tools. | Higher |

### Stability

Stability means something different on each machine:

| Machine | Effect of higher Stability |
|---|---|
| [Potential Reactor](potential-reactor.md), [Corrosion Cell](corrosion-cell.md) | More FE recovered per cycle. |
| [Cavitation Generator](cavitation-generator.md) | Less wear on the installed rotor. |
| [Vacuum Collapse Generator](vacuum-collapse-generator.md) | Meets recipe Stability minimums, and less instability slows each cycle less. |
| [Metal Press](metal-press.md) | Less forming failure. |
| [Resonance Calibrator](resonance-calibrator.md) | Higher stability floor on calibrated outputs. |
| [Solar Panel](solar-panel.md) Array Controller | Recovers part of the weather penalty. |
| [Modular tools](modular-tools.md) | Above 1.0, a per-block chance to skip both the durability and FE cost. 1.10 is a 10% chance. |
| [Battery Chassis](battery-chassis.md) | Below 1.0, part of each [burst](battery-chassis.md#burst) is lost. |

### Crusher stats

These come from Crusher prefixes and start at 0.

| Stat | What it does | Better |
|---|---|---|
| Output Guard Grace | Ticks the Crusher keeps its progress while the output is blocked. | Higher |
| No-Cell Output Retention | How much of the output penalty for running without a Battery Cell you keep. | Higher |
| Hardness Energy Mitigation | Cuts the extra FE that high-hardness recipes cost. It does not reduce the recipe's base cost. | Higher |
| Crusher Input Filter | How strictly the top side filters automated input. The top side always rejects items that cannot be crushed; from tier 2 it also checks output space, then bonus-bank compatibility, then room for a full batch. Tier 1 adds nothing beyond that default. It never rejects recipes above the Crush Head's hardness. | Higher |
| Crusher Salvage | Chance to recover one extra base output item, on top of Bonus Output and Super Output. | Higher |

## Energy

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Energy Capacity | How much FE the machine can store in its own buffer. Processing machines keep only a small buffer; a [Battery Cell](battery-cells.md) gives real storage. On a [Battery Chassis](battery-chassis.md) it scales the installed cells while they stay inside. | Every FE machine, Battery Chassis, and Battery Cells. | Higher |
| Flat Energy Capacity | Fixed FE added to the buffer before percentage bonuses. | Machines and Battery Cells that roll a flat capacity prefix. | Higher |
| Energy Transfer | Rate FE can move through storage: a Battery Cell's input rate, and on a Battery Chassis a raise to each installed cell's rates. Generators and processing machines have no FE limit of their own; the receiver or [connector tier](universal-cable.md#energy) sets the rate. | Battery Cells, Battery Chassis. | Higher |
| Efficiency | More FE from the same fuel or recipe on generators, and less loss when a Battery Chassis charges and discharges. It does **not** cut the FE cost of processing recipes. | Generators, Battery Chassis. | Higher |
| Idle Loss | Stored FE lost while sitting idle. | Battery Cells, Battery Chassis. | Lower |
| Battery Slots | Active Battery Cell slots. | [Battery Chassis](battery-chassis.md). | Higher |
| Burst Transfer | Output during a [burst](battery-chassis.md#burst), as a multiple of the installed cells' rated output. | [Battery Chassis](battery-chassis.md). | Higher |
| Burst Duration | Ticks of burst the window holds. It refills one tick for each tick without a burst. | [Battery Chassis](battery-chassis.md). | Higher |
| Global Modifier Strength | How strongly the chassis's Capacity and Energy Transfer affixes act on each installed cell. | [Battery Chassis](battery-chassis.md). | Higher |

## Heat

Heat machines must warm up to a recipe's temperature before they start, and can fail if they run too hot, too cold, or lose power. See [Heat](heat.md) for how this plays out.

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Max Temperature | Highest temperature the machine can reach. Recipes above it are locked. Mostly comes from the installed Heat Core. | [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md), [Melter](melter.md). | Higher |
| Heat Transfer | How hard the heater pushes heat in: faster warmup and recovery, and higher FE/t while heating. On the [Cavitation Generator](cavitation-generator.md) it sheds heat strain. | Heat machines, Cavitation Generator. | Higher |
| Thermal Conductivity | How fast heat reaches the input, and leaks through the walls: faster crafting and higher FE/t, about the same FE per craft. | Heat machines. | Higher |
| Thermal Mass | How much heat the chamber stores: smaller load dips and less wobble, but slower warmup and recovery. | Heat machines. | Depends |
| Cooling Rate | How fast a heat machine sheds heat when it is too hot for its next recipe or slider setting. | Heat machines. | Higher |
| Heat Insulation | How well the walls hold heat in: less heat lost, so cheaper crafts. On a [Solid Fuel Burner](solid-fuel-burner.md), below 1.0 some burn time leaks away as waste heat. | Heat machines, Solid Fuel Burner. | Higher |
| Temperature Stability | Precision of temperature control. Some recipes need a minimum; on Stage 4 and higher recipes, low values build failure strain instead. On the Cavitation Generator it reduces heat strain. | [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md), [Cavitation Generator](cavitation-generator.md). | Higher |
| Overheat Tolerance | Widens each recipe's safe maximum before overheating builds failure strain. | [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md). | Higher |

## Generation and fuel

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Energy Generation | FE the machine produces. On Solar Panels and Solid Fuel Burners it is FE/t directly; on recipe generators and the Bio Generator it multiplies the recipe or fuel's output, and flat rolls add FE/t on top. The Stats tab shows the final FE/t; hover it for the breakdown. If your modpack changes RNGTech's energy settings, the hover shows them as **Pack setting**. | Every generator. | Higher |
| Fuel Duration | Makes each fuel item burn longer. Generators keep the same FE/t; the Stage 0 Furnace smelts more per item. | [Furnace](furnace.md) (Stage 0), [Bio Generator](bio-generator.md), [Solid Fuel Burner](solid-fuel-burner.md). | Higher |
| Fuel Efficiency | More value from each fuel item. Longer burns on the Stage 0 Furnace and Solid Fuel Burner, more total FE on the Bio Generator. Shown as **Bio Conversion** on the Algae Photobioreactor, where it scales algae output. | [Furnace](furnace.md) (Stage 0), [Solid Fuel Burner](solid-fuel-burner.md), [Bio Generator](bio-generator.md), [Algae Photobioreactor](algae-photobioreactor.md). | Higher |

### Bio Generator fuel power

Each of these multiplies the FE a [Bio Generator](bio-generator.md) gets from one fuel family. They come from Bio Chamber affixes and only apply to matching fuel. Higher is better.

| Stat | Fuel family |
|---|---|
| Potato Power | Potatoes |
| Carrot Power | Carrots |
| Bread Power | Bread |
| Sapling Power | Saplings |
| Seed Power | Seeds |
| Plant Power | Other plant biomass |
| Organic Reagent Power | Organic Reagent |
| Composted Biomass Power | Composted Biomass |
| Algae Power | Algae Biomass and Dense Algae Biomass |
| Rich Biomass Power | Rich Biomass |

### Solar stats

| Stat | What it does | Used by |
|---|---|---|
| Peak Solar Generation | Boosts a panel's output during the midday window (time 4000 to 8000). The Peak Solar suffix raises it by 50–100%. | Solar Panels |
| Array Range | How far the Array Controller reaches to collect panels, up to 2. | Solar Array Controller |
| Moonlight Conversion | Turns part of the array's solar output into night output. | Solar Array Controller |
| Lunar Inversion | Stronger night output, at a penalty on clear days. | Solar Array Controller |
| Weather Recovery | Restores part of the output lost to rain and storms. | Solar Array Controller |
| Clear Sky Amplification | Bonus output in clear daylight. | Solar Array Controller |
| Panel Synchronization | Bonus output when every panel in the array is the same material. | Solar Array Controller |
| Panel Arbitration | Picks the highest-output panels when the array mixes materials. | Solar Array Controller |

All of these are better higher. See [Solar Panel](solar-panel.md).

## Fluids

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Fluid Capacity | Size of the machine's fluid tank. Fluid Pumps can roll it for the machine they sit in. | [Melter](melter.md), [Corrosion Cell](corrosion-cell.md), [Compressor Tanks](compressor-tank.md), the Forestry Companion's water tank. | Higher |
| Fluid Transfer | How fast fluid moves in or out, in mB/t. Usually set by an installed Fluid Pump; on Compressor Tanks it is the compression rate before Servos. | [Component Assembler](component-assembler.md), [Corrosion Cell](corrosion-cell.md), [Melter](melter.md), [Cavitation Generator](cavitation-generator.md), [Compressor Tanks](compressor-tank.md). | Higher |
| Compression Ratio | How much fluid a Steel or higher Compressor Tank packs into its compressed store. Set by the tank's material. | [Compressor Tanks](compressor-tank.md). | Higher |

## Calibration and refinement

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Refinement Potential | The item's remaining budget for refinement. See [Refinement Potential](rarity-and-affixes.md#refinement-potential). | Everything that rolls traits. | Higher |
| Calibration Quality | Raises the stability floor and ceiling of calibrated outputs. | [Resonance Calibrator](resonance-calibrator.md) | Higher |
| Calibration Precision | Narrows the stability range of calibrated outputs. | [Resonance Calibrator](resonance-calibrator.md) | Higher |
| Catalyst Efficiency | Above 1.0, a chance to keep the catalyst after a calibration, up to 45%. | [Resonance Calibrator](resonance-calibrator.md) | Higher |
| Output RP Bonus | Adds Refinement Potential to calibrated outputs. | [Resonance Calibrator](resonance-calibrator.md) | Higher |

## Mastery attributes

**Control**, **Drive**, and **Reserve** are Machine Mastery attributes: precision, actuation, and capacity. Points come from the Mastery tree and from [attribute suffixes](rarity-and-affixes.md#attribute-suffixes) on the machine and its Gear. Each machine family turns its points into its own stats, such as speed or buffer size. See [Machine Mastery](machine-mastery.md). On a Tool Rod, Control also spends FE to protect durability.

## Tools and companions

| Stat | What it does | Better |
|---|---|---|
| Mining Speed | Block-breaking speed of a modular tool. | Higher |
| Mining Level | Harvest tier. Set by the Tool Head and upgrades such as the Diamond Tip; affixes never raise it. | Higher |
| Attack Speed | Main-hand swings per second. | Higher |
| Durability | Max durability of a modular tool, and the wear limit of a Cavitation Rotor. | Higher |
| Self Repair | Durability restored every 7.5 seconds while the tool sits idle in your hotbar or offhand. | Higher |
| FE Use | FE spent per block broken. | Lower |
| FE Transfer | How fast FE moves into and out of the tool's installed Battery Cell. | Higher |
| Battery Support | Highest Battery Cell stage a Tool Rod accepts. | Higher |
| Area Width, Area Height | Size of a Hammer's mining area. | Higher |
| Tree Fell Limit | Most connected logs a Treefeller breaks at once. Also the Forestry Companion's Treefeller batch size. | Higher |
| Vein Mine Limit | Most connected ore blocks a Vein Miner Pick breaks, including the first. | Higher |
| Vein Mine FE | Extra FE for each block Vein Miner breaks after the first. | Lower |
| Ore Burst Speed | Extra mining speed on ore for Ore Burst Picks and Hammers. | Higher |
| Ore Burst FE Use | Extra FE for each ore block broken with Ore Burst. | Lower |
| Luck | Chance for extra drops from blocks the tool breaks. It affects nothing else. | Higher |
| Block Filter Slots | Active block filters on the {{ item('rngtech:miners_companion') }}. Starts at 3, up to 10. | Higher |

## Ascendancy stats

These come from [ascendancies](machine-mastery.md#ascendancies), the shared Mastery tree, and some [Uniques](uniques.md). They appear on the Stats tab only once a machine has them. Percentages are added as percentage points.

| Stat | Machine | What it does | Better |
|---|---|---|---|
| Hardness Tolerance | Crusher | Missing Crush Head levels that add no time, FE, or jam risk. Bonus output stays off while under level. | Higher |
| Jam Chance | Crusher | Multiplier on the jam chance, including Jam Chance per Cycle. Starts at 1. | Lower |
| Jam Recovery | Crusher | Clears jams faster. | Higher |
| Under-Level Efficiency | Crusher | Cuts the time and FE each missing hardness level adds, up to 90%. | Higher |
| Bank Memory | Crusher | How many inputs keep their Bonus Output bank when you switch inputs. Without it, one. | Higher |
| At-Level Output | Crusher | Bonus Output on recipes exactly at the Crush Head's hardness. | Higher |
| Super Output Cadence | Crusher, Alloy Furnace | Guarantees Super Output every this many eligible cycles of the same input. 0 is off. | Lower, above 0 |
| Overdrive Speed | Furnace | Processing Speed for every 10 °C a lane runs above the recipe target. | Higher |
| Overdrive Cap | Furnace | Most Processing Speed Overdrive can add. | Higher |
| Overdrive Margin | Furnace | How far below the recipe's safe maximum Overdrive stops heating. A bigger margin is safer but leaves less room for Overdrive. | — |
| Strain Recovery | Furnace | Extra failure strain drained each tick while a lane is in its safe band. | Higher |
| Ledger Rate | Furnace, Alloy Furnace, Metal Press, Forestry Companion | Share of the base output banked each eligible cycle and paid out as whole items. | Higher |
| Flux Rate | Alloy Furnace | Share of one unit of the largest input banked per craft; a full unit is skipped on a later craft. | Higher |
| Blend Speed | Alloy Furnace | Processing Speed on blend recipes. | Higher |
| Blend Heat Reduction | Alloy Furnace | Degrees removed from the heat that blend recipes need. | Higher |
| Mold Swap Time | Metal Press | Ticks the Mold Rack takes to switch to a stored mold that fits. | Lower |
| Heat Window | Metal Press | Widens each recipe's safe heat window; negative values narrow it. | Higher |
| Streak Floor | Resonance Calibrator | Stability floor gained per consecutive calibration of one family on one pattern. | Higher |
| Streak Cap | Resonance Calibrator | Largest floor bonus the streak can reach. | Higher |
| Coil Reach | Resonance Calibrator | Stages above the Resonance Coil's own that a recipe may need. | Higher |
| Fluid Yield | Melter | More fluid from eligible melts. | Higher |
| Overlevel Speed | Melter | Processing Speed for each Crush Head level above a recipe's need. | Higher |
| Cart Speed | Forestry Companion | Cart travel speed. Starts at 1, capped at 0.34 blocks per tick. | Higher |
| Idle Cart Speed | Forestry Companion | Extra Cart Speed after 4 seconds without planting or harvesting. | Higher |
| Growth Pulse | Forestry Companion | Bone meal applications per growth pulse. Each pulse uses one bone meal. | Higher |
| Work Range | Forestry Companion | Rows the cart works on each side of the rail, 1 to 7. Wider rows cost more scan FE. | Higher |

## Unique stats

Only [Uniques](uniques.md) give these. They appear on the Stats tab while a Unique that gives them is installed.

| Stat | Machine | What it does | Better |
|---|---|---|---|
| Jam Chance per Cycle | Crusher | Chance to jam when any craft starts, under-level or not. Adds to the under-level jam chance. | Lower |
| Escapement Speed | Metal Press, Alloy Furnace, Melter | Speeds the first cycle after the machine idles or its recipe or mold changes. FE per craft is unchanged. | Higher |

## Phase Array line

The [Phase Array](phase-array.md) reads these from its Emitter and Coils. None of them touch a processing machine.

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Charge Capacity | FE the line holds for one pulse, summed over the Emitter and every Coil. | Phase Emitter, Phase Coil. | Higher |
| Charge Rate | FE/t the line takes from supply and from the Cradle's cells, summed over its parts. | Phase Emitter, Phase Coil. | Higher |
| Hold Chance | The chance this Coil holds the pulse. Base 92%. | Phase Coil. | Higher |
| Second Hold | The chance a slip in this Coil is rolled again. | Phase Coil. | Higher |
| Overhold Chance | The chance a surplus hold in this Coil triggers Overhold. Base 8%, plus the Emitter's. | Phase Emitter, Phase Coil. | Higher |
| Partial Window | How many Coils short of the operation's count still count as Partial. Base 2. | Phase Emitter. | Higher |
| Strain Tolerance | Strain points that add no Fracture chance, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| FE Thrift | Percent off every pulse's FE, summed over the line, up to 50%. | Phase Emitter, Phase Coil. | Higher |
| FE Recovery | Percent of a slipped pulse's FE returned to the charge, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| Glyph Salvage | The chance the Glyph returns on a Miss, summed over the line. | Phase Emitter, Phase Coil. | Higher |
| Strain Mercy | The chance a pulse adds no Strain, summed over the line. | Phase Emitter, Phase Coil. | Higher |

## See also

- [Rarity and Affixes](rarity-and-affixes.md): where rolled stats come from.
- [Gear](gear.md): the parts that add stats to a machine.
- [Machine Mastery](machine-mastery.md): passives, attributes, and ascendancies.

{{ navbox() }}
