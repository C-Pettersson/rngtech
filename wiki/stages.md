---
wiki:
  category: Guides
  icon: rngtech:steel_ingot
  ids:
    - rngtech:machine_frame
    - rngtech:reinforced_machine_frame
    - rngtech:advanced_machine_frame
    - rngtech:exotic_machine_frame
    - rngtech:tin_ingot
    - rngtech:zinc_ingot
    - rngtech:bronze_ingot
    - rngtech:nickel_ingot
    - rngtech:lead_ingot
    - rngtech:silver_ingot
    - rngtech:steel_ingot
    - rngtech:invar_ingot
    - rngtech:aluminum_ingot
    - rngtech:osmium_ingot
    - rngtech:sparksteel_ingot
    - rngtech:titanium_ingot
    - rngtech:arclite_ingot
    - rngtech:tungsten_ingot
    - rngtech:platinum_ingot
    - rngtech:tungstensteel_ingot
    - rngtech:nullite_ingot
    - rngtech:aethergold_ingot
    - rngtech:naquadah_ingot
    - rngtech:tin_ore
    - rngtech:deepslate_tin_ore
    - rngtech:zinc_ore
    - rngtech:deepslate_zinc_ore
    - rngtech:nickel_ore
    - rngtech:deepslate_nickel_ore
    - rngtech:lead_ore
    - rngtech:deepslate_lead_ore
    - rngtech:silver_ore
    - rngtech:deepslate_silver_ore
    - rngtech:aluminum_ore
    - rngtech:deepslate_aluminum_ore
    - rngtech:osmium_ore
    - rngtech:deepslate_osmium_ore
    - rngtech:titanium_ore
    - rngtech:deepslate_titanium_ore
    - rngtech:tungsten_ore
    - rngtech:deepslate_tungsten_ore
    - rngtech:platinum_ore
    - rngtech:deepslate_platinum_ore
    - rngtech:naquadah_ore
    - rngtech:deepslate_naquadah_ore
    - rngtech:raw_tin
    - rngtech:raw_zinc
    - rngtech:raw_nickel
    - rngtech:raw_lead
    - rngtech:raw_silver
    - rngtech:raw_aluminum
    - rngtech:raw_osmium
    - rngtech:raw_titanium
    - rngtech:raw_tungsten
    - rngtech:raw_platinum
    - rngtech:raw_naquadah
    - rngtech:crushed_iron
    - rngtech:crushed_copper
    - rngtech:crushed_gold
    - rngtech:crushed_tin
    - rngtech:crushed_zinc
    - rngtech:crushed_nickel
    - rngtech:crushed_lead
    - rngtech:crushed_silver
    - rngtech:crushed_aluminum
    - rngtech:crushed_osmium
    - rngtech:crushed_titanium
    - rngtech:crushed_tungsten
    - rngtech:crushed_platinum
    - rngtech:crushed_naquadah
    - rngtech:iron_dust
    - rngtech:copper_dust
    - rngtech:gold_dust
    - rngtech:tin_dust
    - rngtech:zinc_dust
    - rngtech:nickel_dust
    - rngtech:lead_dust
    - rngtech:silver_dust
    - rngtech:aluminum_dust
    - rngtech:osmium_dust
    - rngtech:titanium_dust
    - rngtech:tungsten_dust
    - rngtech:platinum_dust
    - rngtech:naquadah_dust
---

# Stages

A **stage** is RNGTech's material tier. Every machine chassis, Gear part, tool part, and Battery Cell has a stage from 0 to 8, set by the metal it is made from. Stage 0 is wood and flint, Stage 1 is Iron, and Stage 8 is Exotic, the top of the ladder.

Stages decide three things:

- **What Gear fits.** A chassis only accepts Gear up to its own stage. See [Chassis stage and Gear](#chassis-stage-and-gear).
- **What a rolled item is worth.** Higher-stage items roll a larger Refinement Potential budget and better affix tiers. See [Stage and rolls](#stage-and-rolls).
- **What a recipe costs.** Higher-stage metals need hotter machines, harder Crush Heads, and more FE.

Stage is not the same as affix **tier**. Tier describes how strong one affix is; stage describes the material. See [Rarity and Affixes](rarity-and-affixes.md) for tiers.

## The stage ladder

| Stage | Name | Main materials | What opens up |
|---:|---|---|---|
| 0 | Primitive | Wood, flint, stone | [Tool Bench](tool-bench.md) and flint tools, the Wooden Crusher, the fuel-burning [Furnace](furnace.md), Potato Battery Cells. |
| 1 | Iron | Iron | The first powered machines: [Solid Fuel Burner](solid-fuel-burner.md), Iron [Crusher](crusher.md), Iron [Battery Chassis](battery-chassis.md), Iron [Resonance Calibrator](resonance-calibrator.md), [Solar Panels](solar-panel.md). |
| 2 | Copper | Copper | Faster energy transfer and heat. Copper machine sets, the [Wooden Dehumidifier](wooden-dehumidifier.md). |
| 3 | Bronze | Bronze, tin, zinc, gold | The first alloy. Basic Electric Circuits, the Crude [Metal Press](metal-press.md), the Bronze [Alloy Furnace](alloy-furnace.md). |
| 4 | Steel | Steel, invar, nickel, lead, silver | Stable midgame machines, the Steel Metal Press, the Lead Furnace, the [Corrosion Cell](corrosion-cell.md). The first recoverable processing failures. |
| 5 | Aluminum | Aluminum, sparksteel, osmium | Gas power: [Coal Gasifier](coal-gasifier.md), [Syngas Combustor](syngas-combustor.md), [Cavitation Generator](cavitation-generator.md), [Silica Gel Dehumidifier](silica-gel-dehumidifier.md). |
| 6 | Titanium | Titanium, arclite | Chemistry: [Melter](melter.md), [Steam Methane Reformer](steam-methane-reformer.md), [Ammonia Synthesizer](ammonia-synthesizer.md), [Ammonia Fuel Cell](ammonia-fuel-cell.md), the Titanium Alloy Furnace. |
| 7 | Tungstensteel | Tungstensteel, nullite, aethergold, tungsten, platinum | Late-game machine sets and the [Vacuum Collapse Generator](vacuum-collapse-generator.md). |
| 8 | Exotic | Naquadah, netherite | The top machine sets, Exotic Battery Cells, the [Exotic Affix Forge](exotic-affix-forge.md). |

Modpacks can disable materials, change recipes, and swap the Exotic material, so check the recipe tables in your pack.

## Materials by stage

Each stage has natural metals you mine and alloys you mix. Ingots set the stage: plates, gears, rods, coils, and casings are recipe forms of the same metal and do not change its stage.

| Stage | Mined metals | Alloys |
|---:|---|---|
| 1 | {{ item('minecraft:iron_ingot') }} | — |
| 2 | {{ item('minecraft:copper_ingot') }} | — |
| 3 | {{ item('rngtech:tin_ingot') }}, {{ item('rngtech:zinc_ingot') }}, {{ item('minecraft:gold_ingot') }} | {{ item('rngtech:bronze_ingot') }} |
| 4 | {{ item('rngtech:nickel_ingot') }}, {{ item('rngtech:lead_ingot') }}, {{ item('rngtech:silver_ingot') }} | {{ item('rngtech:steel_ingot') }}, {{ item('rngtech:invar_ingot') }} |
| 5 | {{ item('rngtech:aluminum_ingot') }}, {{ item('rngtech:osmium_ingot') }} | {{ item('rngtech:sparksteel_ingot') }} |
| 6 | {{ item('rngtech:titanium_ingot') }} | {{ item('rngtech:arclite_ingot') }} |
| 7 | {{ item('rngtech:tungsten_ingot') }}, {{ item('rngtech:platinum_ingot') }} | {{ item('rngtech:tungstensteel_ingot') }}, {{ item('rngtech:nullite_ingot') }}, {{ item('rngtech:aethergold_ingot') }} |
| 8 | {{ item('rngtech:naquadah_ingot') }}, {{ item('minecraft:netherite_ingot') }} | — |

Naquadah is the default Exotic material, and modpacks can substitute their own.

### Ores

RNGTech adds eleven ores to the Overworld, each in a stone and a deepslate version. Iron, Copper, and Gold use the vanilla ores. Each ore has a **hardness** that your pick or hammer head must reach; see [Ore hardness](tool-bench.md#ore-hardness) for which heads reach which level.

| Ore | Stage | Hardness | Height (Y) | Notes |
|---|---:|---:|---|---|
| {{ item('minecraft:iron_ore') }} | 1 | 1 | Vanilla | |
| {{ item('minecraft:copper_ore') }} | 2 | 1 | Vanilla | |
| {{ item('rngtech:tin_ore') }} | 3 | 2 | −16 to 96 | Common. |
| {{ item('rngtech:zinc_ore') }} | 3 | 2 | −16 to 80 | Common. |
| {{ item('minecraft:gold_ore') }} | 3 | 2 | Vanilla | |
| {{ item('rngtech:nickel_ore') }} | 4 | 3 | −32 to 48 | |
| {{ item('rngtech:lead_ore') }} | 4 | 3 | −32 to 40 | |
| {{ item('rngtech:silver_ore') }} | 4 | 3 | −48 to 32 | |
| {{ item('rngtech:aluminum_ore') }} | 5 | 4 | −32 to 64 | |
| {{ item('rngtech:osmium_ore') }} | 5 | 4 | −48 to 24 | |
| {{ item('rngtech:titanium_ore') }} | 6 | 5 | −64 to 16 | Sometimes missing where a vein touches air. |
| {{ item('rngtech:tungsten_ore') }} | 7 | 6 | −64 to 0 | Rare. Often missing where a vein touches air. |
| {{ item('rngtech:platinum_ore') }} | 7 | 6 | −48 to 16 | Rare. Often missing where a vein touches air. |
| {{ item('rngtech:naquadah_ore') }} | 8 | 8 | −64 to −32 | Very rare, small veins. Often missing where a vein touches air. |

The deeper ores are often cut out of cave walls, so dig into solid rock instead of only searching caves. With the JEI mod installed, each ore has an ore generation page showing its depth, vein size, and hardness for your pack's settings.

Mining an RNGTech ore drops its raw metal, such as {{ item('rngtech:raw_tin') }}. Fortune increases the drop like it does on vanilla ores, and Silk Touch drops the ore block itself.

### Alloys

Alloys have no ore. You make them in the [Alloy Furnace](alloy-furnace.md), and the Alloy Furnace chassis and its Crucible must both reach the recipe's stage.

| Alloy | Stage | Made from | How |
|---|---:|---|---|
| {{ item('rngtech:bronze_ingot') }} | 3 | Copper and tin | Craft {{ item('rngtech:bronze_blend') }} from Copper, Tin, and Coal Dust and smelt it in a [Furnace](furnace.md), or alloy Copper and Tin Ingots with Charcoal. |
| {{ item('rngtech:steel_ingot') }} | 4 | Iron and carbon | Make {{ item('rngtech:steel_blend') }} in the Alloy Furnace and smelt it, or alloy Iron Ingots with Coal, or with Coal Dust and Charcoal. |
| {{ item('rngtech:invar_ingot') }} | 4 | Iron and nickel | Alloy Furnace. |
| {{ item('rngtech:sparksteel_ingot') }} | 5 | Gold, silver, and redstone | Alloy Furnace, Steel stage or higher. |
| {{ item('rngtech:arclite_ingot') }} | 6 | Copper, silver, and redstone | Alloy Furnace, Steel stage or higher. |
| {{ item('rngtech:tungstensteel_ingot') }} | 7 | Tungsten, steel, and Coal Dust | Titanium Alloy Furnace. |
| {{ item('rngtech:nullite_ingot') }} | 7 | Lead, platinum, and Ender Pearls | Titanium Alloy Furnace. |
| {{ item('rngtech:aethergold_ingot') }} | 7 | Tin, silver, and Glowstone Dust | Titanium Alloy Furnace. |

See the [Alloy Furnace](alloy-furnace.md#alloy-furnace-recipes) recipe table for exact amounts and temperatures.

## From ore to ingot

Ore moves up a processing chain. Each step costs a machine and some FE, and pays you back with more ingots per ore.

| Route | Ingots per raw metal | Machines |
|---|---:|---|
| Smelt raw metal | 1 | [Furnace](furnace.md) |
| Crush raw metal, smelt the crushed material | 2 | [Crusher](crusher.md), Furnace |
| Crush raw metal, crush again into dust, smelt the dust | 2, faster and cheaper | Crusher twice, Furnace |

1. **Mine** the ore for raw metal. Silk Touch gives the ore block instead.
2. **Crush** it. One raw metal gives 2 crushed material, and one ore block gives 3. Deepslate ore blocks take a little longer to crush.
3. **Crush again** (optional). Each crushed item gives 1 dust in a quick second pass.
4. **Smelt** the crushed material or dust in the Furnace for 1 ingot each.

Smelting crushed material directly is slow, which makes it an easy hopper chain. Crushing it into dust first is the better route: for Stage 1–4 metals, the dust route takes 260 ticks and 6,000 FE per ingot across both machines, against 600 ticks and 7,200 FE for crushed material.

The Crusher's [Output Amount](crusher.md#output-amount) stat adds bonus crushed material on top of these numbers. Alloys skip this chain: they have no ore, raw, crushed, or dust form.

### Smelting heat

Raw metal is the easy route: it smelts into one ingot at only 1,000 heat (800 for raw iron and copper). Ore blocks, crushed material, and dust need the metal's full smelting heat, so your Furnace's Heat Core decides which metals you can process this way.

| Metal | Minimum heat | Needs stability |
|---|---:|---|
| Iron, Copper | 800 | No |
| Tin, Zinc, Gold | 1,000 | No |
| Nickel, Lead, Silver | 1,100 | Yes |
| Aluminum, Osmium | 1,300 | Yes |
| Titanium | 1,450 | Yes |
| Tungsten, Platinum | 1,750 | Yes |
| Naquadah | 2,000 | Yes |

See the [Furnace](furnace.md#smelting) for the heat each Heat Core reaches.

Higher-stage metals also cost more FE. Smelting and crushing Stage 5, 6, 7, and 8 metals cost 2×, 3×, 4×, and 6× the Stage 1–4 FE, and their crushing recipes need harder [Crush Heads](crusher.md#hardness).

## Chassis stage and Gear

A machine's chassis stage is the highest stage of Gear it accepts. A Stage 4 Steel Crusher takes a Flint, Iron, Copper, Bronze, or Steel Crush Head, but not an Aluminum one. Lower-stage Gear always fits, so a well-rolled early part can stay in service for a long time.

This cap applies to a machine's main processing Gear:

- Crush Heads in the [Crusher](crusher.md).
- Heat Cores in the [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), and [Solid Fuel Burner](solid-fuel-burner.md).
- Alloy Crucibles and Servos in the Alloy Furnace.
- Fuel Boxes in the Solid Fuel Burner.
- Disassembly Heads and Recovery Filters in the [Component Recycler](component-recycler.md).
- Battery Cells and Servos in [Compressor Tanks](compressor-tank.md).

Battery Cells fit most machines at any stage. A few machines limit them, for example the Solid Fuel Burner takes cells up to its own stage, and some gas machines need a minimum cell or Heat Core stage instead. Each machine's Gear section lists its limits, and the [Gear](gear.md) page explains the slots.

Two other systems read the chassis stage:

- Some [Machine Mastery](machine-mastery.md) keystones loosen the cap, such as a Crusher keystone that allows a Crush Head one stage above the chassis.
- A machine needs a Stage 4 or higher chassis before it can choose an [Ascendancy](machine-mastery.md#ascendancies).

Each chassis has its own recipe. You never need the previous stage's machine to build the next one.

### Machine frames

Most staged machine bodies are built around a frame for their stage band:

| Frame | Used by |
|---|---|
| {{ item('rngtech:machine_frame') }} | Stage 0–3 bodies, and the Steel Metal Press. |
| {{ item('rngtech:reinforced_machine_frame') }} | Most Stage 4 Steel and Lead bodies. |
| {{ item('rngtech:advanced_machine_frame') }} | Stage 5–6 bodies. |
| {{ item('rngtech:exotic_machine_frame') }} | Stage 7–8 bodies. |

Each frame is crafted from the one before it. The Exotic frame also needs a {{ item('rngtech:lubricated_frame_coupling') }}, assembled from Tungstensteel Casing and Lubricant in the [Component Assembler](component-assembler.md).

{{ crafting("rngtech:machine_frame", "rngtech:reinforced_machine_frame", "rngtech:advanced_machine_frame", "rngtech:exotic_machine_frame") }}

## Stage and rolls

When a machine, Gear part, or Battery Cell rolls its traits, its stage sets the size of its [Refinement Potential](rarity-and-affixes.md#refinement-potential) budget:

| Stage | Refinement Potential |
|---:|---:|
| 0 | 1–3 |
| 1–2 | 6–10 |
| 3–4 | 8–14 |
| 5–6 | 14–22 |
| 7–8 | 22–32 |

Tool Heads and Tool Rods roll slightly more. Stage also shifts which affix tiers an item tends to roll: higher stages make high tiers more likely, and the top affix tier only appears on Stage 8 items.

## Failures from Stage 4

Stages 0–3 are forgiving. When conditions are wrong, early machines wait, slow down, or produce less, and they keep your input.

From Stage 4, some recipes can fail. Heat outside the safe window, poor temperature stability, or a power drop in the middle of a job builds up failure strain, and when it fills, the input becomes a recoverable failure item such as a {{ item('rngtech:malformed_ingot') }} or {{ item('rngtech:broken_circuit') }}. Smelt a Malformed Ingot in a [Furnace](furnace.md) to get two nuggets back. Better Heat Cores, Servos, and Battery Cells make the same work safer. See the [Metal Press](metal-press.md#pressing) and [Furnace](furnace.md#smelting) for how each machine handles it.

## See also

- [Getting Started](getting-started.md)
- [Gear](gear.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
