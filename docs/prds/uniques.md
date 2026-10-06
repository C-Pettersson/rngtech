# PRD: Unique Items

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Rarity](../reference/rarity.md) for current behavior.

PRD status: Accepted

Implementation status: Planned

Release: 2.0

Last updated: 2026-10-06

## Summary

Unique items are authored, find-only Gear with ranged stats, one signature mechanic, and one visible drawback, modeled on Path of Exile uniques. Every copy of a Unique has the same stat lines, but each copy rolls its own values inside the catalog ranges, so players can find a well-rolled or poorly rolled copy. For example, a Unique might roll +1 to +3 Batch Size.

A Unique is a sidegrade or a build-around piece, not a higher stage. It comes from one exact place in the world, and the best Uniques make a Mastery build or an ascendancy work differently.

`Rarity.UNIQUE` and one Unique item already exist: the Voltaic Potato Battery Cell found in village chests. Every Unique check outside the rarity enum is specific to Battery Cells, so this PRD adds a shared Unique catalog, moves the Potato cell onto it, and ships eight launch Uniques that cover every Mastery family.

Uniques cannot be refined. [Corruption](corruption.md) is the only way to change one, and its Reforged outcome is the only way to reroll a Unique's values.

## References

- [Rarity](../reference/rarity.md)
- [Unique Item Ideas](../reference/unique-item-ideas.md): the candidate catalog this PRD promotes from
- [Corruption PRD](corruption.md)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Stats](../reference/machine-stats.md)
- [Machine Parts](../content/machine-parts.md), [Battery Cells](../content/battery-cells.md)
- [Machine Mastery](../systems/machine-mastery.md) and the [Machine Ascendancies PRD](machine-ascendancies.md)
- [Crafting and Upgrades](../systems/crafting.md#unique-components)

Key existing code:

- `src/main/java/com/rngtech/rpg/Rarity.java`
- `src/main/java/com/rngtech/rpg/MachineTraits.java` (`withIdentity` forces Unique rarity and 0 RP; `normalizedStored` keeps only affix slots)
- `src/main/java/com/rngtech/rpg/ModifierSlot.java`
- `src/main/java/com/rngtech/rpg/MachineImplicitCatalog.java` (`identity(Item)`, `batteryCell()`)
- `src/main/java/com/rngtech/rpg/ComponentBaseStatCatalog.java` (`profile(Item)`, `effectiveStats`)
- `src/main/java/com/rngtech/rpg/ModifierEligibilityProfiles.java` (`UNIQUE_BATTERY_CELL`)
- `src/main/java/com/rngtech/content/item/CraftedTraitOutputs.java` (`isUnidentified`, `applyRolledTraits`)
- `src/main/java/com/rngtech/content/recipe/UnidentifiedTraitIdentifyRecipe.java`
- `src/main/java/com/rngtech/content/item/MachinePartItem.java`, `BatteryCellItem.java`
- `src/main/java/com/rngtech/content/energy/BatteryCellMaterial.java` (`UNIQUE_POTATO`)
- `src/main/java/com/rngtech/content/gear/GearSlotCatalog.java`
- `src/main/java/com/rngtech/content/recycling/RecyclingData.java`
- `src/main/java/com/rngtech/compat/jei/RNGTechJeiPlugin.java` (`uniqueItemStacks()`)
- `src/main/resources/data/rngtech/loot_modifiers/add_unique_potato_battery_cell_to_village_chests.json`

## Goals

- A shared Unique catalog, so a new Unique needs a catalog entry, a loot table, language keys, and a texture, plus code only for a new signature mechanic.
- Ranged stat lines that roll once per copy. Integer stats, such as Batch Size or Processing Level, roll whole numbers.
- Eight launch Uniques. Together they reach every Mastery family (Crusher, Furnace, Alloy Furnace, Metal Press, Melter, Resonance Calibrator, Forestry Companion) and the Battery Chassis.
- Every stat on a Unique, including its drawback, is read by at least one host it fits. No display-only drawbacks.
- Uniques can grant ascendancy stats, which matter only on a chassis with that ascendancy. This is the PoE build-around pattern.
- World sources only: structure chests, archaeology, entity drops, and trial vaults. No crafting recipe makes a Unique.
- Vanilla sources are the default. Pack makers can switch any Unique, or all of them, to RNGTech challenges, quest rewards, or their own sources, using config and datapacks without code.
- Uniques never add yield in 2.0, so they cannot open a recipe loop.

## Non-Goals

- No refinement, Affix Forge, Exotic Affix Forge, Tool Bench refinement, or Debug Reroller on Uniques. [Corruption](corruption.md) is the only change allowed.
- No dedicated reroll item for Unique values, like PoE's Divine Orb. Corruption's Reforged outcome is the only reroll.
- No limit on how many Uniques one machine can hold.
- No Unique machines or chassis. Uniques are installable Gear and Battery Cells.
- No Unique modular tool heads or rods in 2.0. They use `ToolBaseStatCatalog`, a separate pipeline; see the [backlog](../reference/unique-item-ideas.md#backlog).
- No recycling. Uniques stay rejected by the Potential Reactor and Component Recycler, as today.
- No datapack override of Unique stats. The catalog is a classpath resource, like the ascendancy catalog. Loot tables and loot modifiers stay datapack data.
- No new structures, bosses, or dimensions.

## Vocabulary

| Term | Meaning |
|---|---|
| Unique | A find-only item with `Rarity.UNIQUE`, catalog-defined stat lines, and 0 Refinement Potential. |
| Unique catalog | The classpath data that defines every Unique's host, slot stage, stat lines, behaviors, and source. |
| Stat line | One catalog stat on a Unique. It is either fixed or a range. |
| Roll | The value one copy rolled for a ranged stat line. Rolls are stored on the stack. |
| Roll quality | Where a roll sits in its range, from 0% (worst) to 100% (best). For a drawback line, the smallest penalty counts as the best roll. |
| Slot stage | The component stage a Unique counts as for Gear stage gates, such as Crush Head ≤ chassis stage or the Metal Press Heat Core cap of 6. |
| Signature | The one mechanic that makes a Unique worth building around. It is either a stat no normal part of that type provides, or a new `MachineBehavior`. |
| Drawback | A visible stat penalty or behavior restriction. It must be read by the Unique's hosts. |
| Ascendancy hook | An ascendancy stat on a Unique, such as Overdrive Margin. The tooltip names the ascendancy it needs. |

## Player Flow

1. The player finds an unidentified Unique in its source, such as a Nether fortress chest.
2. The unidentified tooltip shows the Unique's name in light purple, its stat lines with their ranges, its signature, its drawback, and its source. JEI shows the same.
3. The player identifies it by crafting it alone, the same way crafted parts are identified today. Every ranged line rolls.
4. The identified tooltip shows the rolled values. Holding Shift shows each line's range and roll quality.
5. The player installs it in a compatible Gear slot. The normal slot rules apply, including stage gates and Mastery Gear legality.
6. Later the player may corrupt it (see [Corruption](corruption.md)).

## Unique Catalog

Each Unique is one JSON file under `src/main/resources/data/rngtech/uniques/`, loaded at startup like the ascendancy catalog.

```json
{
    "id": "rngtech:fortress_heater_element",
    "host": "heat_core",
    "slot_stage": 4,
    "base_profile": "rngtech:titanium_heat_core",
    "stats": {
        "rngtech:warmup_time": { "operation": "more", "min": -0.8, "max": -0.6 },
        "rngtech:energy_usage": { "operation": "increased", "min": 0.6, "max": 0.4 },
        "rngtech:temperature_stability": { "operation": "more", "min": -0.5, "max": -0.3 },
        "rngtech:fuel_efficiency": { "operation": "more", "value": -0.5 },
        "rngtech:overdrive_margin": { "operation": "flat", "min": 60, "max": 120 }
    },
    "behaviors": [],
    "source": "rngtech:unique_source.nether_fortress"
}
```

- `host` names a `MachinePartType`, or `battery_cell`.
- `base_profile` starts from an existing part's base stats. `stats` then adjusts them with the same operations that Mastery uses. A Unique may also list every stat with no base profile.
- A stat line has either `value` (fixed) or `min` and `max` (ranged). `min` is the worst roll and `max` the best, so a drawback range can run downward, as Energy Usage does above. Rolls are uniform across the range.
- Integer stats roll whole numbers inclusive of both ends. For example, `"rngtech:parallel_jobs": { "operation": "flat", "min": 1, "max": 3 }` rolls 1, 2, or 3.
- `behaviors` are `MachineBehavior` ids and never roll. New behaviors are declared in code, as ascendancy behaviors are.
- `source` is a language key for the tooltip and JEI line. The loot table is separate data.

Load-time validation:

- `host` is a known part type, and `slot_stage` lies in that type's stage range.
- Every stat and behavior is known, and is read by at least one host machine that accepts the part type. A hand-maintained table of which machines read which stats backs this check and is checked against `GearSlotCatalog`.
- Integer stats use whole-number bounds. A drawback line is still a penalty at its best roll, and a signature line is still a benefit at its worst roll.
- At its best roll, a recipe-gating stat such as Processing Level or input-slot count reaches at most one stage past the slot stage.
- The catalog contains no yield stat (Output Amount, Super Output Chance, Crusher Salvage Chance, Fluid Yield, Ledger Rate, or any stat or behavior declared with a `yield` other than `none`). The one exception is a penalty, such as less Output Amount. A later Unique may add yield only after it gets loop-audit coverage (see [Loop Prevention](#loop-prevention)).
- Every Unique has a language name, a description key, a texture, and at least one loot table that drops it.

## Items, Identity, and Rolls

- Unique parts use one `UniquePartItem` class, a `MachinePartItem` subclass that carries its catalog id. Unique Battery Cells stay `BatteryCellMaterial` entries, because a cell's output rate is a material field rather than a stat.
- `MachineImplicitCatalog.identity()` and `ComponentBaseStatCatalog.profile()` check the Unique catalog first. The base profile and fixed lines form the Unique's base stats.
- **Roll storage:** rolls are stored as modifiers in a new `ModifierSlot.UNIQUE` inside `rngtech:machine_traits`. `normalizedStored` keeps that slot when the identity is Unique and drops it otherwise. `withIdentity` keeps it while forcing `UNIQUE` and 0 RP. `effectiveStats` applies `UNIQUE` modifiers after the base profile, both on stacks and in installed hosts.
- **Identification:** loot tables apply a `rngtech:unidentified_unique` loot function that sets the existing seeded `rngtech:unidentified_trait_roll` component. `CraftedTraitOutputs.applyRolledTraits` gains a Unique branch that rolls every ranged line from the catalog instead of rolling affixes. A Unique stack with no traits and no pending roll, such as one from `/give` or the creative tab, also counts as unidentified.
- **Catalog changes:** when a range changes in a later version, stored rolls are clamped into the new range when read. A newly added ranged line on an old copy uses its range midpoint. A removed line's stored roll is ignored.
- Uniques stack to 1, render with foil, and use vanilla `Rarity.EPIC` for a light-purple name that matches the light-purple rarity header in the tooltip.
- These checks are Battery-Cell-only today and move to one shared `UniqueItems.isUnique(ItemStack)` helper backed by the `rngtech:uniques` item tag:
    - `RefinementTargets` eligibility and the synthesized traits for component-less stacks;
    - `CraftedTraitOutputs`;
    - JEI's `uniqueItemStacks()` and the Affix Forge and Exotic Affix Forge category filters;
    - `BatteryCellItem` foil and tooltip handling, generalized to `UniquePartItem`.
- One eligibility profile per host type, with capabilities and no rollable affixes, as `UNIQUE_BATTERY_CELL` does now.

## Sources and Loot

Uniques come from vanilla places by default. Pack makers can move them to RNGTech's own challenges or to quests.

### Default vanilla sources

- Each Unique gets its own loot table under `data/rngtech/loot_table/uniques/`, added to vanilla tables through `neoforge:add_table` loot modifiers, as the Potato cell is today. Every Unique loot entry applies `rngtech:unidentified_unique`.
- A new loot item condition, `rngtech:unique_loot_enabled`, reads the common config keys `uniques.loot.enabled` and `uniques.loot.<id>.enabled`. Every default Unique loot table carries it, and the Potato cell's table is migrated onto it. The keys gate only these default tables.
- Draft chances, tuned in playtest:
    - common structures (village, ruined portal, mineshaft): 3–5% per chest;
    - uncommon structures (igloo basement, bastion treasure, Nether fortress): 8–10% per chest;
    - rare structures (ancient city): 4% per chest;
    - trial vault rewards: in the ominous vault's rare pool only;
    - entity drops: 1% on a player kill, +0.5% per Looting level.

### Pack configuration

A pack maker can use any of these options, alone or together:

1. **Switch off defaults:** set `uniques.loot.enabled=false`, or `uniques.loot.<id>.enabled=false` for one Unique.
2. **Challenge loot:** add the Unique to one of RNGTech's [challenge loot tables](#challenge-loot) with a datapack, for example as a reward for a Stage 6+ heat failure or a Mastery level.
3. **Quests and commands:** give the plain item, for example as an FTB Quests item reward. A Unique with no traits counts as unidentified, so each player rolls their own copy on identification. `/rngtech unique give <player> <id>` gives an unidentified copy for testing and command-based quests.
4. **Own loot:** replace or extend any loot table or loot modifier with ordinary datapacks.

The same options work for the [Volatile Catalyst](corruption.md#sources), whose recipe can be switched off by config.

### Challenge loot

RNGTech rolls a loot table when certain machine events happen. Every table ships empty, so default play is unchanged, and pack makers fill them by datapack. A table that is still empty is skipped without rolling.

| Table | Rolled when | Context available to conditions |
|---|---|---|
| `rngtech:challenges/calibration` | A calibration completes | family, chassis stage, recipe stage, result stability |
| `rngtech:challenges/crusher_jam` | A Crusher jams | chassis stage, recipe Processing Level, head Processing Level |
| `rngtech:challenges/heat_failure` | A Furnace, Alloy Furnace, or Metal Press cycle fails | family, chassis stage, recipe stage, failure cause |
| `rngtech:challenges/vacuum_collapse` | A Vacuum Collapse cycle completes | chassis stage, instability |
| `rngtech:challenges/forestry_harvest` | A Forestry cart harvests a tree or crop | cutting tool stage, ascendancy |
| `rngtech:challenges/mastery_level` | A machine gains a Mastery level | family, new level, ascendancy |

- Results go into the machine's output slots when they fit, or drop on top of the machine. Forestry results go into the cart's inventory. Mastery level results drop on top of the machine.
- New loot item conditions read the context: `rngtech:machine_family`, `rngtech:machine_stage`, `rngtech:recipe_stage`, `rngtech:calibration_stability`, `rngtech:instability`, `rngtech:mastery_level`, and `rngtech:ascendancy`. Each takes an optional `min` and `max`, or an id.
- The tables can hold anything, not only Uniques: catalysts, Seals, or pack items. Pack makers own the loop safety of what they add. RNGTech's shipped tables stay empty.

Example datapack entry that moves the Fortress Heater Element from Nether fortress chests to Stage 4+ heat failures, after the pack sets `uniques.loot.fortress_heater_element.enabled=false`:

```json
{
    "type": "rngtech:challenge",
    "pools": [
        {
            "rolls": 1,
            "conditions": [
                { "condition": "rngtech:machine_stage", "min": 4 },
                { "condition": "minecraft:random_chance", "chance": 0.02 }
            ],
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "rngtech:fortress_heater_element",
                    "functions": [{ "function": "rngtech:unidentified_unique" }]
                }
            ]
        }
    ]
}
```

## Launch Uniques

Ranges are draft targets, and fixed values are expressed against existing profiles so they stay meaningful while base stats are tuned. Ranges are written worst to best. Each row lists hosts whose runtime reads the Unique's stats, based on the current survey of which machines read each stat.

| Unique | Host | Slot stage | Source | Families |
|---|---|---:|---|---|
| Fortress Heater Element | Heat Core | 4 | Nether fortress chest | Furnace, Alloy Furnace, Metal Press, Melter |
| Igloo Basement Thermostat | Heat Core | 2 | Igloo basement chest | Furnace, Metal Press |
| Mineshaft Worn Pick-Jaw | Crush Head | 2 | Mineshaft minecart chest | Crusher, Melter |
| Crying Crucible | Alloy Crucible | 3 | Ruined portal chest | Alloy Furnace |
| Trial Vault Escapement | Servo | 6 | Ominous trial vault | Metal Press, Alloy Furnace, Melter |
| Witch-Bottle Reflux Pump | Fluid Pump | 5 | Witch drop | Melter, Forestry Companion |
| Ancient Echo Control Board | Control Board | 6 | Ancient city chest | Resonance Calibrator |
| Bastion Coin-Stack Capacitor | Battery Cell | 4 | Bastion treasure chest | Battery Chassis |

The Voltaic Potato Battery Cell moves onto the catalog with fixed lines equal to its current stats, so existing cells are unchanged. It keeps its village source.

### Fortress Heater Element

`rngtech:fortress_heater_element`, Unique Heat Core, slot stage 4.

- **Identity:** fixed Titanium-class Maximum Temperature in a Stage 4 slot, so a Steel-stage Furnace, Alloy Furnace, Metal Press, or Melter meets late heat gates early.
- **Signature:** (60–80)% less Warmup Time.
- **Ascendancy hook:** +(60–120) °C Overdrive Margin (Crucible Keeper).
- **Drawback:** (60–40)% increased Energy Usage, (50–30)% less Temperature Stability, and a fixed 50% less Fuel Efficiency in a Solid Fuel Burner.
- **Why:** it pays for heat reach with FE and failure strain on Stage 4+ failure-bearing recipes. It stays a sidegrade because PROCESSING_LEVEL and the other gates are unchanged.

### Igloo Basement Thermostat

`rngtech:igloo_basement_thermostat`, Unique Heat Core, slot stage 2.

- **Identity:** precision heat for early and mid recipes.
- **Signature:** (40–80)% more Temperature Stability and (40–80)% more Overheat Tolerance, beyond any normal core, and the `POWER_GRACE` behavior that normal parts only reach on Stage 6 Servos.
- **Ascendancy hook:** +(5–15) °C Heat Window (Drop Forge).
- **Drawback:** a fixed Maximum Temperature cap at the Bronze profile, and (50–30)% less Heat Transfer, which also slows the Melter.
- **Why:** a cold-running core for presses and furnaces that punish instability. It is useless for hot alloys.

### Mineshaft Worn Pick-Jaw

`rngtech:mineshaft_worn_pick_jaw`, Unique Crush Head, slot stage 2.

- **Identity:** a Stage 2 head that crushes one hardness level above its stage.
- **Signature:** a fixed +1 Processing Level over its slot stage, and +(1–3) Batch Size. Chassis acceptance still uses slot stage 2, so it fits early Crushers and does not change stage-support keystones.
- **Ascendancy hook:** +(10–30)% Jam Recovery (Rockbreaker).
- **Drawback:** +(8–3)% Jam Chance on every cycle, not only under-level cycles, and (25–15)% less Output Amount on the Crusher.
- **Why:** an early exploration find for players who push wide, under-level crushing, and a natural Rockbreaker piece. Batch Size adds throughput, not yield, and the Output Amount line only lowers yield.

### Crying Crucible

`rngtech:crying_crucible`, Unique Alloy Crucible, slot stage 3.

- **Identity:** a Bronze-class crucible tuned for blend recipes. It has the same fixed input-slot count as the Bronze crucible, so it does not skip the Steel crucible.
- **Signature:** +(15–30)% Blend Speed and (10–20)% Blend Heat Reduction, which normally come only from Blendwright.
- **Ascendancy hook:** the signature itself stacks with Blendwright.
- **Drawback:** (40–20)% less Stability and (40–20)% less Temperature Stability, which raise failure strain on failure-bearing recipes.
- **Why:** it opens a blend-recipe Alloy Furnace before the ascendancy, and rewards Blendwright players who accept more failures.

### Trial Vault Escapement

`rngtech:trial_vault_escapement`, Unique Servo, slot stage 6.

- **Identity:** a stop-start servo for small batches.
- **Signature:** new behavior `ESCAPEMENT`. The first cycle after the machine idles, or after its recipe or mold changes, is (30–60)% faster. FE per craft is unchanged, so speed never makes a craft cheaper.
- **Ascendancy hook:** −(10–25) ticks Mold Swap Time (Die Keeper).
- **Drawback:** (25–15)% less Processing Speed on every cycle after the first, and no `POWER_GRACE`, which every normal Stage 6+ Servo provides.
- **Why:** it suits a Die Keeper press that swaps molds often, and hurts a press that runs one recipe all day.

### Witch-Bottle Reflux Pump

`rngtech:witch_bottle_reflux_pump`, Unique Fluid Pump, slot stage 5.

- **Identity:** a pump that holds rather than moves.
- **Signature:** (75–125)% increased host Fluid Capacity (Melter tanks, Forestry cart tank). New behavior `REFLUX`: the Field Hand Sprinkler spends half as much water, stacking with Irrigation.
- **Ascendancy hook:** the Sprinkler half of the signature (Field Hand).
- **Drawback:** (70–50)% less Fluid Transfer, which slows Melter container filling, side extraction, and station refills.
- **Why:** a large buffer for slow or manual fluid handling, and a Field Hand piece.

### Ancient Echo Control Board

`rngtech:ancient_echo_control_board`, Unique Control Board, slot stage 6.

- **Identity:** fixed Nullite-class Calibration Precision in a Stage 6 slot.
- **Signature:** new behavior `ECHO_STREAK`. The calibration streak survives one calibration of another family, as Pattern Memory lets it survive one change.
- **Ascendancy hook:** +(1–3) Streak Cap (Harmonist).
- **Drawback:** (40–20)% less Processing Speed, and no Refinement Potential Bonus, which every normal board provides.
- **Why:** it is a Harmonist build-around, and it does not touch the RP economy.

### Bastion Coin-Stack Capacitor

`rngtech:bastion_coin_stack_capacitor`, Unique Battery Cell, slot stage 4.

- **Identity:** a burst cell for Battery Chassis with `BURST_RELEASE`.
- **Signature:** (75–125)% increased host Burst Transfer and (75–125)% increased Burst Duration while installed in a burst-capable chassis.
- **Drawback:** (150–100)% increased Idle Loss, and a fixed steady output rate at half the Steel cell's.
- **Why:** it rewards a Copper, Gold, or later burst chassis and does little elsewhere.

## New Stats and Behaviors

| Name | Kind | Used by | Yield |
|---|---|---|---|
| `ESCAPEMENT` | Behavior | Processing machines that read `PROCESSING_SPEED` and host Servos | None |
| `REFLUX` | Behavior | Forestry cart Sprinkler | None |
| `ECHO_STREAK` | Behavior | Resonance Calibrator streak | None |

Every other Unique stat already exists. A Unique may grant an ascendancy stat. Without the ascendancy it has no effect, and the tooltip says which ascendancy it needs.

## Extensibility

A new Unique with no new mechanic needs only:

- a catalog file;
- a loot table and loot modifier entry;
- language keys (name, description, source);
- an item texture and model;
- an entry in the `rngtech:uniques` tag.

Item registration iterates the catalog, so no Java registry edit is needed. A new signature mechanic adds a `MachineBehavior` and its host code, plus an entry in the stat-reader table.

## Interaction Rules

- **Gear legality:** Uniques follow the normal slot rules. Mastery nodes that require Gear, such as Precision Jaw Mount's exact-stage head, compare against slot stage.
- **No count limit:** a machine can hold a Unique in every Gear slot that accepts one.
- **Corruption:** a Unique can be corrupted once. Reforged rerolls every ranged line. The [Corruption PRD](corruption.md#uniques) defines the outcomes.
- **Recycling:** unchanged. `RecyclingData` returns no RPG value for Uniques, so the reactor and recycler reject them. Unique fragments are deferred.
- **Item filters:** the existing rarity filter already matches `UNIQUE`.
- **Drops and pick-block:** installed Uniques save and drop with the host, like any Gear, and keep their rolls.

## Loop Prevention

Launch Uniques carry no yield stats, apart from the Pick-Jaw's Output Amount penalty, so they cannot change any recipe-loop bound. The catalog validation rejects yield gains. Today the loop audit's coverage check reads only `mastery/declarations.json`. A future yield Unique must therefore:

- extend that coverage check to read the Unique catalog;
- fit inside each affected recipe type's `maxOutputMultiplier` at its best roll, or raise that bound with an updated `reason`;
- add a domain check that pins its worst case.

## UI

- **Tooltip:**
    - unidentified: a light-purple name, the Unique header, each stat line with its range, the signature, the drawback, any ascendancy hook with the ascendancy name, and the source line;
    - identified: the same sections with rolled values instead of ranges;
    - Shift: each line's range and roll quality, plus the overall roll quality as the average of the ranged lines.
- **Gear tab:** no layout change. When a stat is not read by the current host, it is dimmed, using the same table as the load check. An installed Unique gets the foil highlight.
- **JEI:** one information page per Unique with its ranges, source, and hosts, replacing the Potato-only page.
- **Jade:** no change. Gear summaries already list installed parts.

## Delivery Phases

1. **PRD and state.** This document and [its state page](uniques-state.md).
2. **Framework:**
    - Unique catalog loader and validation, including ranges and the stat-reader table;
    - `UniquePartItem`, catalog-driven registration, and the `rngtech:uniques` tag;
    - catalog lookups in `MachineImplicitCatalog` and `ComponentBaseStatCatalog`;
    - `ModifierSlot.UNIQUE`, roll storage, clamping, and aggregation;
    - the Unique identification branch;
    - generalize every Battery-Cell-only Unique check;
    - migrate the Potato cell;
    - domain checks.
3. **Sources:**
    - the `rngtech:unique_loot_enabled` condition and config keys, the `rngtech:unidentified_unique` loot function, per-Unique loot tables and modifiers, and the Potato cell on the new condition;
    - the `rngtech:challenge` loot context, the six empty challenge tables and their event hooks, and the context conditions;
    - `/rngtech unique give`.
4. **Launch content:** the eight Uniques, the three new behaviors, textures, and language keys.
5. **UI:** unidentified and identified tooltips, the Shift roll view, dimmed unread stats, JEI pages, and foil.
6. **Documentation:**
    - Rarity, Machine Parts, Battery Cells, and Machine Stats;
    - [Current Implementation Matrix](../reference/current-implementation.md);
    - the [Unique Item Ideas](../reference/unique-item-ideas.md) page, marking promoted entries;
    - an FTB Quests entry for finding and identifying a first Unique.

## Test Plan

Domain checks (`masteryCheck`):

- The catalog loads, and every mutated fixture fails with the matching violation:
    - unknown host or slot stage out of range;
    - unknown stat, or a stat read by none of its hosts;
    - a yield gain;
    - a non-integer bound on an integer stat;
    - a drawback that becomes a benefit at its best roll;
    - a recipe gate past slot stage + 1;
    - missing language or texture.
- Over a seeded sample, every roll lies inside its range, integer rolls hit both ends, and rolls are stable for a given seed.
- Identification rolls every ranged line. Unidentified stacks with and without a pending roll both identify.
- Effective stats equal the base profile plus fixed lines plus rolls, on both the item stack and the installed host.
- `withIdentity` and `normalizedStored` keep `UNIQUE` modifiers on Uniques and drop them on other items.
- Stored rolls clamp into a changed range, and a missing line uses its midpoint.
- Every refinement entry point rejects a Unique and consumes nothing. Debug Reroller rejects Uniques.
- Slot stage drives stage gates, including Crusher acceptance and the Metal Press and Melter Heat Core caps.
- `ESCAPEMENT` speeds only the first cycle after idle or a change, and leaves FE per craft unchanged.
- `ECHO_STREAK` keeps the streak through exactly one off-family calibration.
- The Potato cell's stats, name, and old save data are unchanged after the migration.

ModDex checks (`npm run moddex:check`):

- Every Unique has a default loot table. Every default Unique loot table carries `rngtech:unique_loot_enabled` and applies `rngtech:unidentified_unique`.
- Every shipped challenge table is empty.
- No recipe outputs a Unique.
- The loop audit still passes with no new allowlist entries.

In-game checks:

- Find each Unique from its source, or with `/loot`, then identify it. Install it in each listed host, and check that incompatible slots reject it.
- Unidentified and identified tooltips, the Shift roll view, dimmed unread stats, JEI pages, and foil.
- With `uniques.loot.enabled=false`, no Unique appears in default loot. With one per-Unique key off, only that Unique disappears.
- A test datapack fills each challenge table. Its items arrive when the event happens and its conditions hold, and they never arrive otherwise.
- A Unique given by an FTB Quests reward and by `/rngtech unique give` identifies and rolls like a looted one.
- Ascendancy hooks change behavior only with their ascendancy chosen.
- Uniques keep their rolls through breaking and replacing the host, and in multiplayer.

## Acceptance Criteria

- Eight launch Uniques plus the migrated Potato cell, all defined by catalog files.
- Ranged lines roll per copy on identification and are stored, synced, and shown with their roll quality.
- No Battery-Cell-only Unique check remains.
- Every stat on a Unique is read by at least one compatible host, enforced at load.
- Uniques cannot be crafted, refined, rerolled, or recycled. Only corruption changes them.
- Default Unique loot is config-gated per Unique, the six challenge tables fire with their context, and a quest-given Unique rolls on identification.
- Existing saves, including Potato cells, load unchanged.
- `./gradlew ciCheck`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` pass.

## Release Scope

- **2.0 must include:** phases 1–3, including every challenge table, and 5–6, plus at least five launch Uniques that together reach the Crusher, Furnace, Metal Press, Resonance Calibrator, and Battery Chassis.
- **In 2.0 if ready before the tag:** the remaining launch Uniques. Otherwise they stay Planned on the ideas page.
- **Release gate:** the full in-game checklist in [Releasing](../releasing.md) runs once for 2.0 after every 2.0 feature lands. Add a "find, identify, install, and corrupt a Unique" line to it.

## Decisions

Resolved 2026-10-05:

- Unique stat lines are ranged and roll per copy, so copies can be well or poorly rolled. Integer stats roll whole numbers, such as +1 to +3 Batch Size.
- There is no limit on how many Uniques a machine holds.
- Uniques drop unidentified and roll when identified, reusing the existing identification flow.
- Uniques never add yield in 2.0.
- The Unique catalog is a classpath resource. Loot is datapack data.
- Unique tool heads and rods wait for a later release.
- Uniques stay unrecyclable.

Resolved 2026-10-06:

- Vanilla sources are the default for Uniques. Pack makers can switch each Unique to RNGTech challenges through challenge loot tables, to quest rewards, or to their own sources, using config and datapacks without code.
- The Volatile Catalyst has no vanilla source. See [Corruption: Sources](corruption.md#sources).
