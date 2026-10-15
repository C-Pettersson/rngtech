---
navigation:
  title: "Stages"
  parent: guides.md
  icon: rngtech:steel_ingot
categories:
  - "Guides"
item_ids:
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

- **What Gear fits.** A chassis only accepts Gear up to its own stage. See [Chassis stage and Gear](rngtech:stages.md#chassis-stage-and-gear).
- **What a rolled item is worth.** Higher-stage items roll a larger Refinement Potential budget and better affix tiers. See [Stage and rolls](rngtech:stages/stage-and-rolls.md).
- **What a recipe costs.** Higher-stage metals need hotter machines, harder Crush Heads, and more FE.

Stage is not the same as affix **tier**. Tier describes how strong one affix is; stage describes the material. See [Rarity and Affixes](rngtech:rarity-and-affixes.md) for tiers.

## Materials by stage

Each stage has natural metals you mine and alloys you mix. Ingots set the stage: plates, gears, rods, coils, and casings are recipe forms of the same metal and do not change its stage.

| Stage | Mined metals | Alloys |
|---:|---|---|
| 1 | <ItemLink id="minecraft:iron_ingot" /> | — |
| 2 | <ItemLink id="minecraft:copper_ingot" /> | — |
| 3 | <ItemLink id="rngtech:tin_ingot" />, <ItemLink id="rngtech:zinc_ingot" />, <ItemLink id="minecraft:gold_ingot" /> | <ItemLink id="rngtech:bronze_ingot" /> |
| 4 | <ItemLink id="rngtech:nickel_ingot" />, <ItemLink id="rngtech:lead_ingot" />, <ItemLink id="rngtech:silver_ingot" /> | <ItemLink id="rngtech:steel_ingot" />, <ItemLink id="rngtech:invar_ingot" /> |
| 5 | <ItemLink id="rngtech:aluminum_ingot" />, <ItemLink id="rngtech:osmium_ingot" /> | <ItemLink id="rngtech:sparksteel_ingot" /> |
| 6 | <ItemLink id="rngtech:titanium_ingot" /> | <ItemLink id="rngtech:arclite_ingot" /> |
| 7 | <ItemLink id="rngtech:tungsten_ingot" />, <ItemLink id="rngtech:platinum_ingot" /> | <ItemLink id="rngtech:tungstensteel_ingot" />, <ItemLink id="rngtech:nullite_ingot" />, <ItemLink id="rngtech:aethergold_ingot" /> |
| 8 | <ItemLink id="rngtech:naquadah_ingot" />, <ItemLink id="minecraft:netherite_ingot" /> | — |

Naquadah is the default Exotic material, and modpacks can substitute their own.

- [Ores](rngtech:stages/ores.md)
- [Alloys](rngtech:stages/alloys.md)

## From ore to ingot

Ore moves up a processing chain. Each step costs a machine and some FE, and pays you back with more ingots per ore.

| Route | Ingots per raw metal | Machines |
|---|---:|---|
| Smelt raw metal | 1 | [Furnace](rngtech:furnace.md) |
| Crush raw metal, smelt the crushed material | 2 | [Crusher](rngtech:crusher.md), Furnace |
| Crush raw metal, crush again into dust, smelt the dust | 2, faster and cheaper | Crusher twice, Furnace |

1. **Mine** the ore for raw metal. Silk Touch gives the ore block instead.
2. **Crush** it. One raw metal gives 2 crushed material, and one ore block gives 3. Deepslate ore blocks take a little longer to crush.
3. **Crush again** (optional). Each crushed item gives 1 dust in a quick second pass.
4. **Smelt** the crushed material or dust in the Furnace for 1 ingot each.

Smelting crushed material directly is slow, which makes it an easy hopper chain. Crushing it into dust first is the better route: for Iron and Copper, the dust route takes 260 ticks and 6,000 FE per ingot across both machines, against 600 ticks and 7,200 FE for crushed material.

Each metal also has its own pace. Soft, low-melting Lead and Tin process in half the time, while hard metals take much longer: Titanium takes three times as long to crush and smelt, and Tungsten smelts four times slower. FE per craft stays the same, so slow metals just tie up your machines longer. The recipe tables on the [Crusher](rngtech:crusher.md) and [Furnace](rngtech:furnace.md) pages list each time.

The Crusher's [Output Amount](rngtech:crusher/output-amount.md) stat adds bonus crushed material on the first crush, on top of these numbers. Alloys skip this chain: they have no ore, raw, crushed, or dust form.

- [Smelting heat](rngtech:stages/smelting-heat.md)

## Chassis stage and Gear

A machine's chassis stage is the highest stage of Gear it accepts. A Stage 4 Steel Crusher takes a Flint, Iron, Copper, Bronze, or Steel Crush Head, but not an Aluminum one. Lower-stage Gear always fits, so a well-rolled early part can stay in service for a long time.

This cap applies to a machine's main processing Gear:

- Crush Heads in the [Crusher](rngtech:crusher.md).
- Heat Cores in the [Furnace](rngtech:furnace.md), [Alloy Furnace](rngtech:alloy-furnace.md), and [Solid Fuel Burner](rngtech:solid-fuel-burner.md).
- Alloy Crucibles and Servos in the Alloy Furnace.
- Fuel Boxes in the Solid Fuel Burner.
- Disassembly Heads and Recovery Filters in the [Component Recycler](rngtech:component-recycler.md).
- Battery Cells and Servos in [Compressor Tanks](rngtech:compressor-tank.md).

Battery Cells fit most machines at any stage. A few machines limit them, for example the Solid Fuel Burner takes cells up to its own stage, and some gas machines need a minimum cell or Heat Core stage instead. Each machine's Gear section lists its limits, and the [Gear](rngtech:gear.md) page explains the slots.

Two other systems read the chassis stage:

- Some [Machine Mastery](rngtech:machine-mastery.md) keystones loosen the cap, such as a Crusher keystone that allows a Crush Head one stage above the chassis.
- A machine needs a Stage 4 or higher chassis before it can choose an [Ascendancy](rngtech:machine-mastery.md#ascendancies).

Each chassis has its own recipe. You never need the previous stage's machine to build the next one.

- [Machine frames](rngtech:stages/machine-frames.md)

## More

- [The stage ladder](rngtech:stages/the-stage-ladder.md)
- [Stage and rolls](rngtech:stages/stage-and-rolls.md)
- [Failures from Stage 4](rngtech:stages/failures-from-stage-4.md)
