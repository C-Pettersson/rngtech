---
wiki:
  category: Guides
  icon: rngtech:wooden_crusher_chassis
---

# Getting Started

RNGTech is a technology mod that treats machines like RPG characters. A machine's body, the **chassis**, sets its base stats and limits. The **Gear** you install inside it does the real work. Machines, Gear parts, tool parts, and Battery Cells each roll their own rarity and affixes, so two Crushers of the same stage can play very differently.

Progress runs through nine [stages](stages.md), from flint and wood at Stage 0 to exotic materials at Stage 8. This page walks you through them in order. For exact ingredients, temperatures, and amounts, use the recipe tables on each machine page or JEI in game, because modpacks can change them.

## Installing

| Requirement | Version |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.228 or newer for 1.21.1 |
| Java | 21 |

Download RNGTech from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/rngtech) or [GitHub Releases](https://github.com/c-pettersson/rngtech/releases) and put the JAR in your instance's `mods` folder. For multiplayer, install it on both the server and every client.

These mods are optional. RNGTech works without them and adds extra features when they are present:

| Mod | What RNGTech adds |
|---|---|
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | Recipe categories for every RNGTech machine, a Gear lookup that shows which parts fit each machine, and ore generation pages with depth and hardness. |
| [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) | Machine overlays when you look at a placed RNGTech block. |
| [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) | A bridge connector that links AE2 networks through [Universal Cable](universal-cable.md). |
| [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) | A bridge connector that links Refined Storage networks through Universal Cable. |

Back up your worlds before updating, and read the release notes for the version you install.

## How RNGTech works

Every machine is built in layers, and each layer is its own item with its own roll. When a machine feels weak, the cause is usually its Gear rather than its chassis.

- **Chassis.** The placed machine block. It sets base stats and caps how advanced its Gear can be. A Gear part's stage can be equal to or lower than the chassis stage, never higher, so mixing old and new parts is normal. See [Stages](stages.md#chassis-stage-and-gear).
- **Gear.** Parts you install in a machine's Gear tab. Crush Heads set how hard an ore a [Crusher](crusher.md) handles, Heat Cores set how hot a [Furnace](furnace.md) or [Metal Press](metal-press.md) gets, and Molds pick what a press makes. See [Gear](gear.md).
- **Battery Cells.** Gear that stores FE and sets how fast a machine takes it in. Most machines run slower or produce less without one. See [Battery Cells](battery-cells.md).
- **Rarity and affixes.** Each rolled item is Normal, Magic, or Rare, and Magic and Rare items carry random affixes that change their stats. See [Rarity and Affixes](rarity-and-affixes.md).
- **Refinement Potential.** Each rolled item also has a budget you spend to add, upgrade, reroll, or remove affixes. Higher-stage items roll a bigger budget. See [Refinement Potential](rarity-and-affixes.md#refinement-potential).
- **Machine Mastery.** Placed machines earn XP from their work and spend points in a passive tree. See [Machine Mastery](machine-mastery.md).

### Identifying new items

Crafted machines, Gear parts, tool parts, and Battery Cells come out **unidentified**. The crafting preview keeps the roll hidden on purpose. To reveal one:

- Put a single unidentified item alone in a crafting grid and take the result. Repeat for each item in a stack.
- Or place an unidentified machine block. Only the placed block is identified; the rest of the stack stays unidentified.

## Start: tools

You gather your own materials, so the first job is a set of tools. RNGTech tools are **Modular Field Tools** built from a separate head and rod, and each part keeps its own roll. See [Tool Bench](tool-bench.md) for the full details.

1. Craft a {{ item('rngtech:tool_bench') }}. It needs no iron.
2. Craft a {{ item('rngtech:flint_pick_head') }} and a {{ item('rngtech:wooden_tool_rod') }}, and assemble a {{ item('rngtech:modular_pick') }} in the bench's Build tab.
3. Add Flint Shovel and Axe heads when you want them. You can swap a head onto an existing rod instead of building a new tool.
4. Install a {{ item('rngtech:tiny_anvil') }} in the bench's Gear tab. It is never used up and unlocks the heavy tools: Hammers break a 3×3 area, Diggers clear 3×3 of dirt or sand, and Treefellers fell whole trees.
5. Craft a {{ item('rngtech:forming_hammer') }} and hammer iron and copper ingots into plates. Plates lead to Iron and Copper heads, which mine faster than flint.
6. Set up a {{ item('rngtech:crude_recycler') }} with a {{ item('rngtech:hand_crank') }} on top. It lets you win back materials from spare parts and bad rolls by hand. See [Component Recycler](component-recycler.md#crude-recycler-and-hand-crank).

### Ore hardness

RNGTech ores have a **hardness** from 1 to 8, separate from vanilla mining tiers. A Modular Pick or Hammer can only mine ores up to its head's hardness. Flint heads reach hardness 1, which covers Iron and Copper ore, and better heads reach further. See [Ore hardness](tool-bench.md#ore-hardness) on the Tool Bench page for every head's reach.

## Stages 1–2: first power and ore doubling

Your first factory goal is to get more than one ingot per ore. Early machines pass power to whatever they touch, so you can build this without cables.

1. Craft a {{ item('rngtech:machine_frame') }} and an {{ item('rngtech:iron_battery_cell') }}. Battery Cells go in a machine's Gear tab.
2. Build a {{ item('rngtech:crude_solid_fuel_burner') }} and install an {{ item('rngtech:iron_heat_core') }}, an {{ item('rngtech:iron_fuel_box') }}, and a Battery Cell. It burns coal, charcoal, and wood and pushes FE into neighbouring machines. See [Solid Fuel Burner](solid-fuel-burner.md).
3. Build an {{ item('rngtech:iron_crusher_chassis') }} with an {{ item('rngtech:iron_crush_head') }} and a Battery Cell, and place it next to the burner. See [Crusher](crusher.md).
4. Crush raw metal before you smelt it. One raw metal gives 2 crushed, and a Silk Touch ore block gives 3. Crush the crushed material again into dust for faster, cheaper smelting. See [Stages](stages.md#from-ore-to-ingot) for the whole chain.
5. Build the {{ item('rngtech:furnace') }}. It burns ordinary furnace fuel and reaches the heat that crushed ore and dust need. Electric Furnaces come later, once you can make circuits. See [Furnace](furnace.md).
6. Pick a [Battery Chassis](battery-chassis.md) to store power. The {{ item('rngtech:iron_battery_chassis') }} is simple and never leaks. The {{ item('rngtech:copper_battery_chassis') }} holds two cells and moves FE faster, but leaks a little while idle.

Farms can also make power. The [Bio Generator](bio-generator.md) burns crops, seeds, and saplings, and the [Wooden Composter](wooden-composter.md) turns plant scraps into {{ item('rngtech:composted_biomass') }}, a much denser fuel. The Stage 0 wooden machines and the {{ item('rngtech:potato_battery_cell') }} are fun novelties, but they are not a base for a factory.

## Stages 3–4: the steel factory

Here recipes start to care about temperature, machines need steady FE, and from Stage 4 a machine can fail a job when its heat, stability, or power is off. Your goal is a park of Steel machines you keep for a long time.

1. **Make Bronze.** Crush copper and tin into dust, combine three Copper Dust, one Tin Dust, and one Coal Dust into {{ item('rngtech:bronze_blend') }} in a crafting grid, and smelt the blend in the Furnace. Hammer Bronze Ingots into plates with the Forming Hammer.
2. **Get Nether Quartz.** Circuit blanks need it, so plan a trip to the Nether.
3. **Build the {{ item('rngtech:crude_metal_press') }}.** Install a {{ item('rngtech:bronze_heat_core') }}, a {{ item('rngtech:circuit_mold') }}, and a Battery Cell, then press a {{ item('rngtech:basic_circuit_blank') }} into a {{ item('rngtech:basic_electric_circuit') }}. The Crude press never ruins an input: if conditions are wrong, it waits. See [Metal Press](metal-press.md).
4. **Learn heat.** A job waits until the machine reaches the recipe's minimum temperature. Heat Cores raise the temperature. Heat stability keeps it steady, and Stage 4 and higher recipes need a minimum stability too.
5. **Build an electric Furnace.** The {{ item('rngtech:iron_furnace_chassis') }} is the plain baseline. Copper heats faster but draws more power, and Bronze runs hotter and is faster on alloy blends.
6. **Build a [Resonance Calibrator](resonance-calibrator.md).** The {{ item('rngtech:iron_resonance_calibrator_chassis') }} turns ordinary plates, coils, and circuits into calibrated components. Most Stage 3 and higher machines need calibrated components in their recipes, so this is the gate into the midgame.
7. **Make Steel.** Build a {{ item('rngtech:bronze_alloy_furnace_chassis') }} with a Bronze Heat Core. It makes {{ item('rngtech:steel_blend') }} to smelt in the Furnace, and direct Steel Ingots once it is hot enough. See [Alloy Furnace](alloy-furnace.md).
8. **Build the Steel {{ item('rngtech:metal_press') }}** with a {{ item('rngtech:steel_servo') }}. It is faster and handles demanding plates, casings, and circuits, but it can fail a job and leave a recoverable failure item. Keep the Crude press for safe, slow work.
9. **Upgrade to Steel.** Press a {{ item('rngtech:reinforced_machine_frame') }}, then build the {{ item('rngtech:steel_crusher_chassis') }}, {{ item('rngtech:steel_furnace_chassis') }}, and {{ item('rngtech:steel_battery_chassis') }}. A {{ item('rngtech:steel_crush_head') }} handles hardness 4 recipes, and a Steel Pick Head with a Diamond Tip mines Aluminum and Osmium ore.

Two recovery machines become worth building now. The [Potential Reactor](potential-reactor.md) turns unwanted rolled items into FE and strips them, and the [Component Recycler](component-recycler.md) returns crafting materials from the stripped item. Run old Gear through the reactor first, then recycle what comes out.

Your power options also widen: the [Corrosion Cell](corrosion-cell.md) burns metal plates in electrolyte, [Solar Panels](solar-panel.md) need circuits and open sky, and the [Algae Photobioreactor](algae-photobioreactor.md) grows Bio Generator fuel from water, carbon gas, and light. A [Wooden Dehumidifier](wooden-dehumidifier.md) supplies water without pumps.

## Stages 5–6: fluids and chemistry

Stages 5 and 6 are about fluids and gases. Gases behave like fluids: they use the same tanks, buckets, and cable connectors. Machine sets now come in Aluminum and Titanium, and older Gear still fits.

1. Craft an {{ item('rngtech:advanced_machine_frame') }}. Stage 5 and 6 machine bodies need it.
2. Gasify Coal Dust and water into Syngas in the [Coal Gasifier](coal-gasifier.md), and burn it in the [Syngas Combustor](syngas-combustor.md). It is the step up from burning coal. Its Carbon Exhaust feeds algae.
3. Build the [Cavitation Generator](cavitation-generator.md), which spins a rotor through water for FE.
4. Build the [Silica Gel Dehumidifier](silica-gel-dehumidifier.md) for steady water that works underground.
5. Build the [Melter](melter.md) and make **Lubricant**, which later Servos and frame parts need. It also makes methane from algae for gas chemistry.
6. Use the [Component Assembler](component-assembler.md) to make Battery Cells and other parts in bulk.
7. Reform methane with water in the [Steam Methane Reformer](steam-methane-reformer.md) for Hydrogen. Fit the Cavitation Generator with nitrogen Gear to split water into Nitrogen.
8. Combine Nitrogen and Hydrogen in the [Ammonia Synthesizer](ammonia-synthesizer.md), and burn the Ammonia in the [Ammonia Fuel Cell](ammonia-fuel-cell.md).

Steel and Titanium [Resonance Calibrator](resonance-calibrator.md) sets calibrate the components the next stages ask for.

## Stages 7–8: late game

Stages 7 and 8 are permanent infrastructure for high FE/t lines. Late recipes can ask for calibrated components with high stability, so watch the stability and heat on your Gear.

1. Press an {{ item('rngtech:elite_electric_circuit') }}.
2. Build a {{ item('rngtech:titanium_alloy_furnace_chassis') }} with a {{ item('rngtech:titanium_heat_core') }} and a {{ item('rngtech:titanium_alloy_crucible') }}, and alloy Tungstensteel, Nullite, and Aethergold.
3. Craft an {{ item('rngtech:exotic_machine_frame') }} and upgrade your Crusher and Furnace to Tungstensteel. Leave room for power and logistics to grow around them.
4. Build {{ item('rngtech:nullite_battery_cell') }}s and a {{ item('rngtech:nullite_battery_chassis') }}. {{ item('rngtech:aethergold_battery_cell') }}s have the highest burst output and never leak.
5. Press an {{ item('rngtech:ultimate_electric_circuit') }}.
6. Build the [Vacuum Collapse Generator](vacuum-collapse-generator.md), the Stage 7 generator. It collapses {{ item('rngtech:void_catalyst') }} into large batches of FE.
7. Build the [Exotic Affix Forge](exotic-affix-forge.md), where you choose exactly which affix to change.

Exotic machine sets with Exotic Battery Cells are the top stage. Naquadah, the Stage 8 ore, needs an Exotic head to mine.

## Power basics

RNGTech machines run on **FE**. Generators push FE into neighbouring blocks, and every powered machine has a small internal buffer plus a Battery Cell slot.

- **Install a Battery Cell.** Most machines run slower or produce less on their internal buffer alone.
- **Store power.** A [Battery Chassis](battery-chassis.md) holds several cells and smooths out gaps between generation and use.
- **Watch the FE per tick.** Each recipe costs a fixed amount of FE. A faster machine finishes sooner but draws more FE per tick, so speed alone does not save power.
- **Stage 5–8 materials cost more.** Recipes for those materials cost 2×, 3×, 4×, and 6× the early FE.

Typical generators by stage, before rolls and Gear bonuses:

| Stage | Generator | Output |
|---:|---|---|
| 1–4 | [Solid Fuel Burner](solid-fuel-burner.md) | 24, 40, 64, or 96 FE/t, depending on the Heat Core |
| 1–5 | [Solar Panel](solar-panel.md) | 4 to 20 FE/t per panel on a clear day |
| 2 | [Bio Generator](bio-generator.md) | About 8 FE/t |
| 4+ | [Corrosion Cell](corrosion-cell.md) | Depends on the plate or Anode |
| 5 | [Syngas Combustor](syngas-combustor.md) | 240 FE/t |
| 6 | [Ammonia Fuel Cell](ammonia-fuel-cell.md) | 600 FE/t |
| 7 | [Vacuum Collapse Generator](vacuum-collapse-generator.md) | About 1,000 FE/t |

The [Potential Reactor](potential-reactor.md) and [Cavitation Generator](cavitation-generator.md) also generate FE. See each page for details.

## Logistics basics

Early machines share power by touching. Once you are past the first Furnace, build [Universal Cable](universal-cable.md). One {{ item('rngtech:cable') }} carries FE, fluids, and items. A {{ item('rngtech:universal_connector') }} on the cable next to each machine decides what moves, which way, and how fast, through the connector modules you install in it.

- The {{ item('rngtech:crude_energy_connector') }} is a simple crafting recipe. Basic and higher Energy, Fluid, and Item Connectors are pressed in a Metal Press with a {{ item('rngtech:connector_mold') }}.
- Each module has a channel from 0 to 15, and connectors only exchange with others on the same channel.
- A {{ item('rngtech:wrench') }} turns individual cable links on and off and shows a connector's settings. A {{ item('rngtech:configurator') }} copies one connector's settings onto others.
- [Fluid and Compressor Tanks](compressor-tank.md) store fluids and gases.

Automation reaches a machine's process slots, not its installed Gear. Battery Cells are the common exception: many machines accept them through a side. Each machine page lists its sides under Automation.

## Refinement

The [Affix Forge](affix-forge.md) changes the affixes on loose machine items, Gear parts, and Battery Cells. Placed machines refine from their own Refinement tab, and tool parts refine in the [Tool Bench](tool-bench.md#refining-tool-parts). Every operation spends Refinement Potential.

- The base forge adds an affix with an {{ item('rngtech:affix_injector') }}, upgrades a random affix with an {{ item('rngtech:affix_modifier') }}, and removes a random affix with a {{ item('rngtech:nullifier_coil') }}.
- Forge upgrades unlock lenses that steer which affix you get, crystals that change an operation's cost or side effects, and catalysts that raise a Magic item to Rare.

Spend the budget deliberately. A Magic part with two good affixes is a strong candidate to raise to Rare. See [Rarity and Affixes](rarity-and-affixes.md) for how rolls and costs work.

## Mastery and Ascendancies

Every placed machine earns its own Mastery XP from successful work and spends points in the passive tree on its Mastery tab. Mastery stays with the machine when you break and move it, so level the machines you plan to keep.

A machine with a Stage 4 or higher chassis can spend an {{ item('rngtech:ascendancy_seal_1') }} from your inventory to choose one of its family's ascendancies. Seals are crafted from Stage 5 materials, and Seals II and III from Stage 7 and 8 materials. See [Ascendancies](machine-mastery.md#ascendancies).

## When a machine underperforms

- **Check its Gear first.** A missing Battery Cell, a Crush Head below the recipe's hardness, or a Heat Core that cannot reach the recipe's temperature explains most slow or stalled machines.
- **Check the recipe.** Recipe tables list minimum temperature, temperature stability, and hardness. Hover the status squares on the Process tab for what is missing.
- **Check power.** From Stage 4, a power drop in the middle of a job can cause a failure. Servos and Battery Cells help.
- **Check connectors.** When power or fluid does not move, check each connector's mode and channel.
- **Check the roll.** Compare your Gear's affixes with what the recipe needs, and refine or recycle the part instead of rebuilding the machine.

## See also

- [Stages](stages.md)
- [Gear](gear.md)
- [Machine Stats](machine-stats.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- [Machine Mastery](machine-mastery.md)

{{ navbox() }}
