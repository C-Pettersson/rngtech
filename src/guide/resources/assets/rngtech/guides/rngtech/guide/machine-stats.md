---
navigation:
  title: "Machine Stats"
  parent: guides.md
  icon: rngtech:iron_heat_core
categories:
  - "Guides"
---

# Machine Stats

Every RNGTech machine, part, Battery Cell, and tool works from a set of **stats**. This page is a glossary of them: what each one does, which machines use it, and whether you want it higher or lower.

## Processing

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Processing Speed | How fast a recipe or work cycle runs, shown as how much faster than the recipe's listed time: +50% faster finishes in two thirds of it. Better chassis start above the listed time; Shift-hover shows the chassis base multiplied by the bonuses. | Every recipe machine, including generators that run recipes, and the Forestry Companion's work interval. | Higher |
| Energy Use | Multiplier on the FE each craft costs. Increases multiply it; reductions add together and then divide it, so 100% reduced halves the cost. | Powered processing machines, such as the [Crusher](rngtech:crusher.md), [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Melter](rngtech:melter.md), [Metal Press](rngtech:metal-press.md), [Component Assembler](rngtech:component-assembler.md), [Component Recycler](rngtech:component-recycler.md), [Resonance Calibrator](rngtech:resonance-calibrator.md), and Steel and higher [Compressor Tanks](rngtech:compressor-tank.md). | Lower |
| Processing Level | The hardness or tier an installed part lets the machine handle. Shown as **Hardness** on the Crusher. Set by Crush Heads, Reactor Chambers, and Disassembly Heads; affixes never raise it. | [Crusher](rngtech:crusher.md), [Melter](rngtech:melter.md), [Potential Reactor](rngtech:potential-reactor.md), [Component Recycler](rngtech:component-recycler.md), and others with a level gate. | Higher |
| Batch Size | How many items from one input stack the machine works in a single cycle. Each item still pays its own FE and earns its own XP. Starts at 1, up to 16. | [Crusher](rngtech:crusher.md), [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Melter](rngtech:melter.md), [Metal Press](rngtech:metal-press.md), [Resonance Calibrator](rngtech:resonance-calibrator.md). | Higher |
| Batch Overhead | Extra cycle time for each item past the first in a batch, as a percentage of one item's time. Starts at 25%, never below 5%. The path to the Dense Batching keystone lowers it. | The same machines as Batch Size. | Lower |
| Input Slots | Active input lanes or slots. Sets the Furnace's smelting lanes (four on the Lead Furnace), the Alloy Furnace's inputs (from its Alloy Crucible), and a Solid Fuel Burner's fuel slots (from its Fuel Box). | [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), [Solid Fuel Burner](rngtech:solid-fuel-burner.md). | Higher |
| Bonus Output | Extra output per recipe. Fractions are banked and paid out as whole items. On the Crusher, increases have diminishing returns; see [Output amount](rngtech:crusher/output-amount.md). On the Cavitation Generator it multiplies the nitrogen made per cycle. | [Crusher](rngtech:crusher.md), [Cavitation Generator](rngtech:cavitation-generator.md). | Higher |
| Super Output | Chance to add one extra copy of the recipe's base output. Separate from Bonus Output. | Item-processing machines. | Higher |
| Instant Process | Chance for a process to finish the moment it starts. Powered machines need enough FE for the whole craft up front. | Item-processing machines. | Higher |
| Stability | General reliability. Its effect depends on the machine: see [Stability](rngtech:machine-stats/stability.md) below. | Many machines and tools. | Higher |

- [Stability](rngtech:machine-stats/stability.md)
- [Crusher stats](rngtech:machine-stats/crusher-stats.md)

## Generation and fuel

| Stat | What it does | Used by | Better |
|---|---|---|---|
| Energy Generation | FE the machine produces. On Solar Panels and Solid Fuel Burners it is FE/t directly; on recipe generators and the Bio Generator it multiplies the recipe or fuel's output, and flat rolls add FE/t on top. The Stats tab shows the final FE/t; hover it for the breakdown. If your modpack changes RNGTech's energy settings, the hover shows them as **Pack setting**. | Every generator. | Higher |
| Fuel Duration | Makes each fuel item burn longer. Generators keep the same FE/t; the Stage 0 Furnace smelts more per item. | [Furnace](rngtech:furnace.md) (Stage 0), [Bio Generator](rngtech:bio-generator.md), [Solid Fuel Burner](rngtech:solid-fuel-burner.md). | Higher |
| Fuel Efficiency | More value from each fuel item. Longer burns on the Stage 0 Furnace and Solid Fuel Burner, more total FE on the Bio Generator. Shown as **Bio Conversion** on the Algae Photobioreactor, where it scales algae output. | [Furnace](rngtech:furnace.md) (Stage 0), [Solid Fuel Burner](rngtech:solid-fuel-burner.md), [Bio Generator](rngtech:bio-generator.md), [Algae Photobioreactor](rngtech:algae-photobioreactor.md). | Higher |

- [Bio Generator fuel power](rngtech:machine-stats/bio-generator-fuel-power.md)
- [Solar stats](rngtech:machine-stats/solar-stats.md)

## More

- [How stats are built](rngtech:machine-stats/how-stats-are-built.md)
- [Energy](rngtech:machine-stats/energy.md)
- [Heat](rngtech:machine-stats/heat.md)
- [Fluids](rngtech:machine-stats/fluids.md)
- [Calibration and refinement](rngtech:machine-stats/calibration-and-refinement.md)
- [Mastery attributes](rngtech:machine-stats/mastery-attributes.md)
- [Tools and companions](rngtech:machine-stats/tools-and-companions.md)
- [Ascendancy stats](rngtech:machine-stats/ascendancy-stats.md)
- [Unique stats](rngtech:machine-stats/unique-stats.md)
- [Phase Array line](rngtech:machine-stats/phase-array-line.md)
