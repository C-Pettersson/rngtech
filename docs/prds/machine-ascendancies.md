# PRD: Machine Ascendancies

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Machine Mastery](../systems/machine-mastery.md) for current behavior.

PRD status: Accepted

Implementation status: Planned

Release: 2.0

Last updated: 2026-09-29

## Summary

Machine ascendancies are family-specific specializations for one chassis, modeled on Path of Exile ascendancy classes. Every [Machine Mastery](../systems/machine-mastery.md) family, including the Forestry Companion, starts with two ascendancies. A chassis chooses one when it uses its first Ascendancy Seal, then spends ascendancy points in a small tree for that ascendancy.

Ascendancies use their own points and items. Using a Seal on a chassis grants 2 points immediately; crafting the Seal is the trial. There are three Seal tiers, for 6 points in total. Tiers follow material stages and lean toward late game; the last tier is aspirational. Ascendancy state stays with the chassis, like the rest of Mastery.

Ascendancies are encouraged to introduce new stats and behaviors. The system is built so new ascendancies can be added later with data, plus only the code their new mechanics need.

Ascendancy nodes may add yield, but no ascendancy, alone or combined with other effects, may create a looping or infinite recipe. This is a hard requirement with an automated audit.

## References

- [Machine Mastery](../systems/machine-mastery.md)
- [Shared Machine Mega Passive Tree PRD](machine-mega-passive-tree.md)
- [Passive Tree Design Rules](../reference/passive-tree-design-rules.md)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Stats](../reference/machine-stats.md)
- [Component Stages](../reference/component-stages.md)
- [Stage Progression and Ore Duplication](../systems/stage-progression-and-ore-duplication.md)
- [Resonance Calibrator](../content/resonance-calibrator.md)
- [Potential Reactor](../content/potential-reactor.md)
- [Component Recycler](../content/component-recycler.md)
- [Modular Field Tools](../content/modular-field-tools.md)
- [Crusher](../content/crusher.md), [Furnace](../content/furnace.md), [Alloy Furnace](../content/alloy-furnace.md), [Metal Press](../content/metal-press.md), [Melter](../content/melter.md), [Tree Farm Automation](../content/tree-farm-automation.md)

Key existing code:

- `src/main/java/com/rngtech/rpg/progression/MachineProgressionState.java`
- `src/main/java/com/rngtech/rpg/progression/MegaPassiveTree.java`
- `src/main/java/com/rngtech/rpg/progression/MegaPassiveNode.java`
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryFamily.java`
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryHost.java`
- `src/main/java/com/rngtech/rpg/progression/MasteryOperations.java`
- `src/main/java/com/rngtech/rpg/progression/MasteryBuildCode.java`
- `src/main/java/com/rngtech/rpg/MachineStat.java`
- `src/main/java/com/rngtech/client/screen/MasteryScreenSupport.java`
- `src/main/java/com/rngtech/content/blockentity/ProcessingChance.java`
- `src/main/java/com/rngtech/content/entity/ForestryCartEntity.java`
- `src/main/resources/data/rngtech/mastery/machine_tree.json`

## Goals

- At least two ascendancies for every Mastery family:
    - Crusher, Furnace, Alloy Furnace, Metal Press;
    - Resonance Calibrator, Melter, Forestry Companion.
- Ascendancy effects are machine-dependent: signature mechanics that the shared tree cannot provide, not generic stat totals.
- Ascendancies introduce new stats and behaviors where that makes them more defining.
- New ascendancies can be added later with a catalog file, language keys, and icons, plus only the code their new mechanics need.
- A separate 6-point budget earned through three Seal tiers weighted toward late game.
- Crafting a Seal is the trial. Each Seal recipe needs several machine families and calibration quality gates.
- Pack makers can switch off Seal recipes and award Seals another way, such as loot or quests.
- Ascendancy effects use the same stat pipeline, Gear legality checks, and tooltips as the shared tree.
- No looping or infinite recipes, enforced in code and in CI.

## Non-Goals

- No change to the shared tree's nodes, links, geometry, catalog version, or 120-point allocation cap.
- Ascendancy points do not come from the 21 reserved Mastery allocations. Those belong to the separate Tome of XP (see [Related: Tome of XP](#related-tome-of-xp)).
- No changes to Gear slot counts, processing lane counts, or automation sides.
- No ascendancies for Gear, cables, or families without a Mastery adapter (generators and storage) until they get one.
- No protection against a machine running out of power. A node may make a machine too expensive to run; that softlock is acceptable.
- No work-based trials. Using a Seal grants its points directly.
- No datapack override of the ascendancy catalog. It is a classpath resource like the shared tree.
- No portable ascendancy. It does not follow the player to a newly crafted chassis.

## Vocabulary

| Term | Meaning |
|---|---|
| Ascendancy | A family-specific specialization chosen once per chassis. |
| Ascendancy point | A point spent only in the chassis's ascendancy tree. |
| Ascendancy Seal | The tiered item that grants ascendancy points when used on a chassis. Crafting it is the trial. Do not name ascendancy items "Ascension …"; the Ascension Catalyst and Ascension Matrix already name rarity promotion. |
| Ascendancy stat | A stat an ascendancy introduces, such as Hardness Tolerance. It is a normal `MachineStat` and can later be used by other systems. |
| Root | The free start node of an ascendancy tree. It holds the signature mechanic. |
| Small node | A 1-point node with one stat. Every notable sits behind one. |
| Notable | A 1-point node with a new rule or a large step in the ascendancy's stat, reached through its small node. |
| Deep notable (★) | A notable behind another notable that defines the build. Reaching it costs 4 points including the parent pair. |

## Player Flow

1. Build a chassis of Stage 4 or higher. For the Forestry Companion, install a Stage 4+ cutting tool instead.
2. Craft an Ascendancy Seal I. Its multi-machine recipe is the trial.
3. With the Seal in inventory, open the Mastery tab's Ascendancy panel and press Ascend. The first time, a dialog previews every ascendancy for the family before the player commits.
4. The Seal is consumed, the chosen root is allocated for free, and the chassis gains 2 ascendancy points.
5. Seals II and III are used the same way and grant 2 more points each.

## Points, Tiers, and Gates

### Tiers

| Tier | Seal materials | Calibrated part in the recipe | Reward |
|---|---|---|---|
| I | Stage 5 (Aluminum, Sparksteel) | Stage 5 part at `70+` stability, or two at `55+` plus a Stabilizer Matrix | Choose an ascendancy, +2 points |
| II | Stage 7 (Tungstensteel, Nullite, Aethergold) | Stage 7 part or Calibrated Diamond Crystal at `85+` stability | +2 points |
| III (aspirational) | Stage 8 (Exotic) | Highest-stage calibrated part at `95+` stability | +2 points |

- Each tier is earned once per chassis, in order.
- Seal items are `rngtech:ascendancy_seal_1`, `rngtech:ascendancy_seal_2`, and `rngtech:ascendancy_seal_3`.
- Crafting a Seal is the trial. Seal recipes are `rngtech:calibrated_shaped` and combine several machine families:
    - Metal Press plates of the tier material.
    - A calibrated part from the Resonance Calibrator.
    - A Primed Seal Core from the Component Assembler, using Electrolyte Solution for Tier I and Lubricant for Tiers II–III.
    - The two calibrated-part routes in Tier I let players trade stability for a Stabilizer Matrix.
- Seals and Seal Cores have no Component Recycler or Potential Reactor recipes and carry no RPG traits.
- **Pack-maker control:**
    - Seals are plain items with no crafted-only data, so a Seal from a loot table, quest reward, or `/give` works exactly like a crafted one.
    - Seal recipes carry a config-backed condition, `rngtech:ascendancy_seal_recipes_enabled`, following the existing `rngtech:material_enabled` pattern. It defaults to enabled.
    - Pack makers can also remove or replace the recipes with a datapack, KubeJS, or CraftTweaker.

### Entry gate

Using Seal I requires an entry stage of 4 or higher. The host supplies it through `MachineMasteryHost.ascendancyEntryStage()`: the chassis stage for block machines, and the installed Axe or Treefeller head stage for the Forestry Companion, which has no chassis stage. The gate is checked only when Seal I is used; removing the tool later does not remove the ascendancy.

Later tiers have no entry gate. The Seal's material stage carries the tier, because a chassis gate could never be met by several families:

- Metal Press stops at Stage 4.
- Alloy Furnace stops at Stage 6.
- Resonance Calibrator stops at Stage 7.
- The Melter has one body.

| Family | Qualifies with |
|---|---|
| Crusher | Steel, Aluminum, Titanium, Tungstensteel, or Exotic chassis |
| Furnace | Steel, Lead, Aluminum, Titanium, Tungstensteel, or Exotic chassis |
| Alloy Furnace | Steel or Titanium chassis |
| Metal Press | Steel Metal Press |
| Resonance Calibrator | Steel, Lead, Titanium, Tungstensteel, or Nullite chassis |
| Melter | Melter |
| Forestry Companion | An installed Axe or Treefeller with a Stage 4+ head |

### Using a Seal

- **Ascend:** the Ascendancy panel's Ascend action consumes the next-tier Seal from the player's inventory, the same way Mastery Refunds are consumed. There is no Seal slot on the machine.
- **Rejected uses:** the server rejects the action without consuming anything when:
    - the Seal is not the chassis's next tier;
    - Seal I is used below entry stage 4;
    - the chassis already has all three tiers.
- **Creative:** creative players need the Seal in inventory but do not consume it, matching refunds.
- **No other gates:** Seals have no Mastery level gate and no work requirement.

## Tree Shape

Every ascendancy follows these rules, based on PoE's 12–16 node trees:

- 12–16 nodes.
- Exactly one root, which is free and cannot be refunded.
- Every notable sits behind exactly one small node.
- At least one deep notable, reached through its own small node behind another notable.
- With 6 points, a chassis can reach at least three notables.

The launch ascendancies use 13 nodes:

- the root;
- four small-node → notable pairs;
- two deep notables.

With 6 points a chassis takes three notables, or one deep notable and one other notable. Tier I (2 points) allows the root plus one notable.

Power bands:

- A small node is one stat, preferably the ascendancy's own stat, slightly stronger than a shared-tree notable (for example, 8% increased instead of 6%).
- A notable adds a new rule or a large step in the ascendancy's stat.
- A deep notable defines the build. A tradeoff is optional; a node with a tradeoff can be more powerful than one without.

All numbers in this PRD are placeholders until balance testing.

Ascendancy nodes use the shared node effect schema: effects, tagged effects, behaviors, fixed values, ceilings, attribute scaling, and passive stats. Each ascendancy has a fixed hand-authored layout; the shared tree's layout tooling does not apply.

## New Stats and Behaviors

Ascendancies are encouraged to introduce new stats and behaviors. Rules:

- A new stat is a `MachineStat` with a declaration in `data/rngtech/mastery/declarations.json`:
    - the families that support it;
    - its yield classification for [Loop Prevention](#loop-prevention): `none`, `output`, `input`, or `energy`.
- Its Stats tab display is added with the stat itself.
- A new behavior is declared in the same file by id, with its owning families and yield classification. Its tooltip uses a language key named after the id.
- The Stats tab shows an ascendancy stat only when an allocated node grants it, following the capability-driven rule in [Machine Guidelines](../reference/machine-guidelines.md).
- Once declared, a stat can later be used by affixes, Gear, or the shared tree, but a yield stat needs loop-audit coverage before it appears anywhere else.
- Reusable mechanics are built once and shared:
    - output ledgers (`LEDGER_RATE`);
    - Super Output cadence;
    - parallel jobs outside the Crusher;
    - streak counters.

Launch stats:

| Stat | Meaning | Used by | Yield |
|---|---|---|---|
| `HARDNESS_TOLERANCE` | Missing hardness levels that add no time, FE, or jam risk | Rockbreaker | No |
| `JAM_CHANCE` | Modifier to under-level jam chance | Rockbreaker | No |
| `JAM_RECOVERY` | Faster jam clearing | Rockbreaker | No |
| `UNDER_LEVEL_EFFICIENCY` | Reduces the per-level time and FE penalty beyond the tolerance | Rockbreaker | No |
| `BANK_MEMORY` | Number of inputs whose bonus bank is remembered | Assayer | Yes |
| `AT_LEVEL_OUTPUT` | Increased Output Amount on recipes exactly at the Crush Head's level | Assayer | Yes |
| `SUPER_OUTPUT_CADENCE` | Every Nth eligible cycle is a guaranteed Super Output | Assayer, Metallurgist | Yes |
| `OVERDRIVE_SPEED` | Processing Speed per 10 °C above the recipe target | Crucible Keeper | No |
| `OVERDRIVE_CAP` | Maximum Overdrive speed | Crucible Keeper | No |
| `OVERDRIVE_MARGIN` | °C that Overdrive keeps below a recipe's safe maximum | Crucible Keeper | No |
| `STRAIN_RECOVERY` | Failure strain drained per tick inside the safe band | Crucible Keeper | No |
| `LEDGER_RATE` | Share of a base output banked per eligible cycle, paid out as whole items | Bloomer, Blendwright, Drop Forge, Timber Baron | Yes |
| `FLUX_RATE` | Share of one unit of the largest input banked per craft; a full unit is skipped later | Metallurgist | Yes |
| `BLEND_SPEED` | Processing Speed on blend routes | Blendwright | No |
| `BLEND_HEAT_REDUCTION` | °C less needed on blend routes | Blendwright | No |
| `MOLD_SWAP_TIME` | Ticks to swap to another stored mold | Die Keeper | No |
| `HEAT_WINDOW` | Width of the safe heat window | Drop Forge | No |
| `STREAK_FLOOR` | Stability floor per consecutive same-family calibration | Harmonist | No |
| `STREAK_CAP` | Maximum streak floor | Harmonist | No |
| `COIL_REACH` | Extra recipe stage reach for the installed Resonance Coil | Mass Tuner | No |
| `FLUID_YIELD` | Extra fluid on eligible melts | Pressure Vessel | Yes |
| `OVERLEVEL_SPEED` | Processing Speed per Crush Head level above the recipe's requirement | Twin Crucible | No |
| `CART_SPEED` | Cart movement speed | Timber Baron | No |
| `GROWTH_PULSE` | Strength of the cart's FE-paid sapling growth pulse | Grove Warden | Yes |

Existing stats extended to new families:

- `PARALLEL_JOBS`: Metal Press, Resonance Calibrator, and Melter (today it is Crusher-only).
- `FLUID_CAPACITY`: Melter.

Stability changes must not raise Refinement Potential outcomes.

## Extensibility

Ascendancies are content. Adding one later needs data, plus only the code for any new stats or behaviors it introduces.

- **Catalog:**
    - One JSON file per ascendancy under `src/main/resources/data/rngtech/mastery/ascendancies/`.
    - Files are listed in `ascendancies/index.json`, because classpath resources cannot be listed by directory.
    - Index order is display order within a family.
- **No fixed count:** a family may have any number of ascendancies. The choose dialog, switching, build codes, and checks work for any count. Launch gives every family two.
- **Adding an ascendancy:**
    - a catalog file and an index entry;
    - language keys under `rngtech.mastery.ascendancy.<id>`;
    - icons under `textures/gui/mastery/ascendancy/<id>/`;
    - code only for new stats or behaviors.
- **Behavior registry:** ascendancy behaviors are declared in `declarations.json` instead of being added to the string switch in `MachineMasteryFamily.supportsBehavior`; the switch falls back to the declarations. Hosts query them the same way they query shared-tree behaviors today.
- **Entry stage hook:** `MachineMasteryHost.ascendancyEntryStage()`. Every current host implements it, and future families (such as generator or storage adapters) implement the same hook.
- **UI:** the Ascendancy panel and choose dialog live in `MasteryScreenSupport`, which every Mastery screen already uses, including the Forestry cart.
- **Stable ids:** ascendancy and node ids are permanent.
    - Adding nodes never moves or renames existing ones.
    - Retiring a node drops it on load and returns its point.
    - Retiring an ascendancy clears the choice, keeps earned tiers, and allows a free re-choice.
- **Validation:** checks run over every catalog entry, so a new ascendancy is validated automatically:
    - tree shape rules;
    - stat and behavior references, and family support for each;
    - language keys and icons;
    - loop-audit coverage for yield stats and behaviors.
- **Fixtures:** test-only ascendancies under `src/masteryTest/resources/data/rngtech/mastery/ascendancies/fixtures/` exercise loading, choosing, saving, build codes, and retirement.
    - Three Crusher fixtures and one Furnace fixture load only on the domain-check classpath.
    - A fixture declaration file adds a stat and a behavior for the Crusher, which proves an ascendancy can be added without touching family code.

## Refunds and Switching

Following PoE, where one ascendancy point costs five regular refund points:

- Refunding an ascendancy node costs `5` Mastery Refunds. The remaining nodes must stay connected to the root and keep installed Gear legal.
- Switching ascendancy requires every non-root ascendancy node to be refunded, then consumes a Seal I. The choose dialog lists every ascendancy for the family. Earned tiers and points are kept.
- Clearing the shared tree does not touch the ascendancy.
- Creative players pay no refunds, as with the shared tree.

## Storage, Saves, and Build Codes

- `MachineProgressionState` gains optional fields, so existing saves load unchanged:
    - `ascendancy`: an id, empty when none is chosen.
    - `ascendancy_nodes`: allocated non-root nodes.
    - `seal_tiers`: `0`–`3`.
- The stream codec carries the same fields.
- **Validation on load:**
    - Unknown node ids are dropped and their points returned.
    - Ascendancy nodes may not exceed `2 × seal_tiers` and must connect to the root; otherwise they are refunded.
    - If the chosen ascendancy no longer exists or belongs to another family, the choice clears, earned tiers remain, and the chassis may choose again without a Seal.
- `forFamily()` currently resets state only when the start node differs. It must also check the ascendancy's family, because the Furnace and Alloy Furnace (and the Metal Press and Resonance Calibrator) share a start.
- The shared tree version stays `2`. The ascendancy catalog has its own version.
- Ascendancy state survives drops, pick-block, and the Forestry cart's item form through `rngtech:machine_progression`. Like the rest of Mastery, it does not transfer to a newly crafted chassis.
- `MegaPassiveTree.applyStats`, `has`, and `passive` include the root and allocated ascendancy nodes, so every family adapter gets ascendancy effects without new call sites. The node parser in `MegaPassiveTree` is extracted so both catalogs use it.
- **Build codes:**
    - Build codes gain optional `ascendancy` and `ascendancy_nodes` fields.
    - Pasting applies ascendancy nodes only when the destination has the same family and ascendancy, and they are allocated within the destination's earned points.
    - Otherwise only the shared-tree part pastes.

## Loop Prevention

Hard requirement: no recipe cycle may return more of an item than it consumed, and no cycle may return its own inputs while producing net FE. This covers every combination of ascendancies, keystones, and affixes, and every stat or behavior declared as yield.

### Yield effects

A yield effect is anything that:

- adds output: bonus banks, ledgers, Output Amount, Super Output, salvage, guaranteed procs;
- recovers output from failures;
- refunds, skips, or reduces inputs, catalysts, or stabilizers;
- adds fluid;
- turns FE into items, such as a growth pulse;
- raises Refinement Potential or any other value that can be turned into FE.

Speed, FE, heat, stability, and failure-strain changes are not yield. Stability changes must not raise Refinement Potential outcomes.

### Eligibility

- Every yield effect goes through one shared eligibility check, the same one that ordinary Output Amount, Super Output, and salvage use.
- Crusher and Furnace recipes keep their `bonus_output` field and defaults. Today 31 recipes opt out:
    - copper, iron, and tin ingot/dust conversions;
    - blend smelting;
    - malformed-ingot nugget recovery.
- Alloy Furnace, Metal Press, Melter, and calibration recipes gain the same field.
    - For these types it defaults to not eligible, so a forgotten recipe fails safe.
    - Their existing Super Output moves behind the same check.
    - Authors mark current one-way routes eligible in the same change, so behavior only changes on routes that could loop.
- Recipe-count reductions are not allowed. No node lowers a recipe's authored input counts.
- No saving effect may reduce an input to zero. Catalyst Efficiency stays below `100%`, and multi-lane savings still consume at least one catalyst per cycle.
- A failure-recovery effect returns at most what the success route would produce from the same inputs.

### Loop audit

`tools/moddex/check-recipe-loops.mjs` runs from `npm run moddex:check`, so CI enforces it.

- It builds one graph from every recipe type:
    - crafting: shaped, shapeless, `trait_shaped`, `calibrated_shaped`;
    - machine recipes, the Component Recycler, and the Potential Reactor;
    - generators, fuels, and fluid recipes.
- Items, fluids, and FE are nodes. Tags resolve through the tag files, and every `material_enabled` condition is treated as enabled.
- Each input-to-output pair becomes an edge with its count ratio. Co-inputs are ignored, which overestimates gains and fails safe.
- Eligible edges take the strongest stacked yield from affixes, keystones, and ascendancies, read from the declared yield stats and behaviors. Chance outputs use their maximum stacked chance as expected value.
- Non-recipe conversions declared as yield become edges too. Grove Warden's growth pulse is an FE → log edge at its strongest stacked strength, combined with the strongest log yield from any ascendancy, because two carts can split growing and harvesting.
- It fails on any cycle with an item gain above `1`, or any cycle that returns its inputs with net FE.
- Reviewed false positives go in an allowlist, each with a written reason.
- A mutation fixture that marks Crusher ingot-to-dust eligible must fail the audit.
- It runs in report mode until the known cycles below are fixed or allowlisted, then becomes a CI gate. No yield node ships before the gate is on.

### Known cycles

- **Calibrate, then recycle (exists on `main` today, independent of ascendancies):**
    - An Iron Plate, a lapis catalyst, and `1,200 FE` make a Calibrated Structural Component.
    - Calibrated components stack, so the Calibrator's Super Output can add a copy.
    - The Component Recycler returns one Iron Plate per component, and the recycler can also roll Super Output.
    - Net: more Iron Plates than consumed, paid in lapis and FE. It is fixed separately; the audit must catch it.
- **Refinement Potential to FE:**
    - Calibrated outputs roll Refinement Potential.
    - The Potential Reactor pays FE for that Refinement Potential and returns a stripped copy.
    - The Component Recycler turns the stripped copy back into the raw input.
    - The audit measures this cycle. For that reason, no ascendancy node raises Refinement Potential, and catalyst-saving nodes are limited as described above.
- **FE to logs:**
    - Grove Warden turns FE into tree growth, and logs burn in Solid Fuel Burners.
    - A growth pulse must cost more FE than the burn value of the wood it produces at the strongest stacked rates, including Timber Baron's ledger on another cart.

## Launch Ascendancies

Cost tags describe the node once its ascendancy's stats and behaviors exist:

- **S**: stats only.
- **H**: a hook in an existing code path.
- **N**: a new mechanic or saved state.

Each ascendancy lists the stats it introduces. Every yield effect applies only to eligible recipes.

### Crusher

**Rockbreaker** pushes recipes past the Crush Head's hardness. Introduces `HARDNESS_TOLERANCE`, `JAM_CHANCE`, `JAM_RECOVERY`, and `UNDER_LEVEL_EFFICIENCY`.

| Node | Effect | Cost |
|---|---|---|
| **Breaker's Stance** (root) | +1 Hardness Tolerance: the first missing hardness level adds no time, FE, or jam risk. Bonuses stay suppressed while under level. | N |
| +10% Jam Recovery → **Jam Breaker** | 50% less Jam Chance; +40% Jam Recovery. | S → S |
| 3 High-Hardness Mitigation → **Fault Lines** | Under-level cycles keep positive Output Amount. Super Output and salvage stay off. | S → H |
| +10% Under-Level Efficiency → **Pressure Stacking** | +40% Under-Level Efficiency: each missing level beyond the tolerance adds `+0.5x` time and FE instead of `+1.0x`. | S → S |
| 5% reduced Energy Usage → **Shatter Point** | The FE surcharge on level 7+ recipes drops from `2.0x` to `1.5x`. | S → H |
| ★ **Bedrock Bite** (after Pressure Stacking) | +1 Hardness Tolerance. | S |
| ★ **Rubble Reclaimer** (after Fault Lines) | Under-level cycles can roll salvage at half chance. | H |

**Assayer** keeps every fraction of bonus output. Introduces `BANK_MEMORY`, `AT_LEVEL_OUTPUT`, and `SUPER_OUTPUT_CADENCE`.

| Node | Effect | Cost |
|---|---|---|
| **Assay Ledger** (root) | +4 Bank Memory: the bonus bank remembers up to 4 inputs instead of resetting when the input changes. | N |
| +2 Bank Memory → **Wide Ledger** | +6 Bank Memory; remembered banks survive breaking and pick-block. | S → N |
| 4% increased Output Amount → **Compound Yield** | A bank payout can also trigger Super Output. | S → H |
| +1% Super Output chance → **Tailings Recovery** | +3% salvage; salvage that does not fit the output is banked instead of lost. | S → H |
| +5% At-Level Output → **Matched Hardness** | +15% At-Level Output. | S → S |
| ★ **Mother Lode** (after Compound Yield) | Super Output Cadence 16: every 16th eligible cycle on the same input is a guaranteed Super Output. | S |
| ★ **Refiner's Oath** (after Wide Ledger) | Dense Parallel is off; 5% more Output Amount per Parallel Job, up to 50%. | H |

Refiner's Oath counts Parallel Jobs from every source: chassis, Crusher Throughput prefixes, and Dense Batching. A one-job chassis gets 5%, Tungstensteel 20%, Exotic 45%, and 10 or more jobs reach the 50% cap.

### Furnace

**Crucible Keeper** runs lanes hotter for speed. Introduces `OVERDRIVE_SPEED`, `OVERDRIVE_CAP`, `OVERDRIVE_MARGIN`, and `STRAIN_RECOVERY`.

| Node | Effect | Cost |
|---|---|---|
| **Overdrive Lanes** (root) | Lanes heat past the recipe target toward the Heat Core's maximum, with 1% Overdrive Speed per 10 °C over target and a 30% Overdrive Cap. The extra warmup costs FE, and a lane can enter a recipe's overheat band. | N |
| +5% Overdrive Cap → **Superheat** | +15% Overdrive Cap. | S → S |
| 10% increased Heat Isolation → **Hold the Fire** | A lane does not cool while its input slot holds a valid input. | S → H |
| +10 °C Overdrive Margin → **Safe Margin** | +15 °C Overdrive Margin; Overdrive never enters a recipe's overheat band. | S → H |
| 4% increased Max Temperature → **Shared Hearth** | Lead Furnace: every lane uses the hottest installed Heat Core's maximum at 90%. | S → H |
| ★ **Crucible Heart** (after Superheat) | Recipes whose target is at most half the lane's temperature finish two inputs per cycle at twice the FE. | N |
| ★ **Strain Bleed** (after Safe Margin) | +10 Strain Recovery: failure strain drains 10 per tick while a lane is inside its safe band. | S |

Today lanes stop warming at the recipe's required temperature (`FurnaceBlockEntity.warmLane`), so Overdrive Lanes changes that warm target.

**Bloomer** turns ore smelting into steady extra ingots. Introduces the shared `LEDGER_RATE` output ledger.

| Node | Effect | Cost |
|---|---|---|
| **Bloom Ledger** (root) | Eligible ore, raw, and crushed smelts feed an output ledger at 1/9 Ledger Rate, about one nugget's worth per smelt, shown on a bonus bar. 50% less Super Output chance. | N |
| 10% increased Ledger Rate → **Rich Blooms** | 100% increased Ledger Rate. | S → S |
| 8% increased Temperature Stability → **Fluxed Blend** | Blend smelts need 100 °C less. | S → H |
| 8% increased Stability → **Slag Reclaim** | Malformed-ingot recovery takes half the time and half the FE. Its output is unchanged, because the recovery recipes opt out of bonus output. | S → H |
| 10% increased Ledger Rate → **Crusher Line** | Crushed inputs feed the ledger twice. | S → H |
| ★ **Patient Bloom** (after Rich Blooms) | 50% more Ledger Rate; 30% less Processing Speed. | S |
| ★ **Clean Bloom** (after Slag Reclaim) | Ore smelts never produce failure outputs; failures pause the lane as at Stages 0–3. | H |

Crushed items currently come only from ore and raw inputs. The strongest Bloomer build (Rich Blooms, Patient Bloom, Crusher Line) banks about 0.73 of an ingot per crushed smelt. Check it against the ore duplication budget together with the Crusher's Output Amount.

### Alloy Furnace

**Metallurgist** spends fewer inputs per alloy. Introduces `FLUX_RATE` and reuses `SUPER_OUTPUT_CADENCE`.

| Node | Effect | Cost |
|---|---|---|
| **Flux Ledger** (root) | 10% Flux Rate: each eligible alloy craft banks 10% of one unit of its largest input, and a full unit is skipped on a later craft. | N |
| +3% Flux Rate → **Reactive Flux** | Flux Rate doubled on direct-ingot routes. | S → H |
| 8% increased Temperature Stability → **Recipe Lock** | Top automation accepts only the current recipe's inputs, in recipe ratio. | S → H |
| 8% increased Stability → **Dross Skimming** | Failure outputs also refund one unit of the recipe's largest input. | S → H |
| 5% reduced Energy Usage → **Heat Economy** | Warmup costs 30% less FE. | S → H |
| ★ **Transmuter's Rate** (after Reactive Flux) | +12% Flux Rate; 25% less Processing Speed. | S |
| ★ **Unbroken Pour** (after Recipe Lock) | Super Output Cadence 32; the count also resets on a failure. | S |

**Blendwright** makes blend routes the safe, fast path. Introduces `BLEND_SPEED` and `BLEND_HEAT_REDUCTION`, and reuses `LEDGER_RATE`.

| Node | Effect | Cost |
|---|---|---|
| **Blend Reversal** (root) | A failed direct-ingot craft returns the blend its inputs would make through the blend route, never more. +25% Blend Speed; direct-ingot routes are 15% slower. | H |
| +5% Blend Speed → **Cold Mixing** | +100 °C Blend Heat Reduction. | S → S |
| 6% increased Processing Speed → **Blend Ledger** | Eligible blend routes feed an output ledger at 25% Ledger Rate. | S → S |
| 8% increased Temperature Stability → **Tempered Crucible** | 40% more Temperature Stability on blend routes. | S → H |
| 8% increased Stability → **Steady Supply** | Power-drop failure strain halved. | S → H |
| ★ **Continuous Pour** (after Cold Mixing) | Back-to-back blend crafts skip re-warmup. | H |
| ★ **Master Blend** (after Blend Ledger) | 100% more Ledger Rate; direct-ingot routes are disabled on this chassis. | S |

### Metal Press

**Die Keeper** focuses on precision and automation. Introduces `MOLD_SWAP_TIME`.

| Node | Effect | Cost |
|---|---|---|
| **Mold Rack** (root) | Selects the stored mold that matches the input automatically, with a 40-tick Mold Swap Time. | N |
| −5 ticks Mold Swap Time → **Quick Change** | −25 ticks Mold Swap Time; swaps keep heat. | S → H |
| 8% increased Temperature Stability → **Tolerance Map** | 40% less heat-related failure strain. | S → H |
| 8% increased Stability → **Circuit Discipline** | Circuit recipes gain 20% increased Stability and use 15% less FE. | S → H |
| 10% increased Overheat Tolerance → **Servo Sync** | Servo power-drop grace doubled. | S → H |
| ★ **Master Die** (after Tolerance Map) | No failure outputs while heat stays in the window and power holds; 20% less Processing Speed. | H |
| ★ **Production Die** (after Quick Change) | Gears and casings 40% faster; circuits 25% slower. | H |

**Drop Forge** focuses on throughput. Introduces `HEAT_WINDOW`, extends `PARALLEL_JOBS` to the Metal Press, and reuses `LEDGER_RATE`.

| Node | Effect | Cost |
|---|---|---|
| **Drop Hammer** (root) | +3 Parallel Jobs: plate, gear, and casing recipes press up to 4 inputs per cycle. 25% narrower Heat Window. Circuits are unaffected. | N |
| 8% increased Processing Speed → **Anvil Mass** | +2 Parallel Jobs. | S → S |
| +5% Heat Window → **Split Failure** | A failed batch fails one job instead of all of them. | S → H |
| 5% reduced Energy Usage → **Shock Absorbers** | Power-drop grace doubled while batching. | S → H |
| 8% increased Heat Transfer → **Hot Stamping** | Plates press 20% faster above their target temperature. | S → H |
| ★ **Forge Line** (after Anvil Mass) | +4 Parallel Jobs; circuit recipes are disabled on this press. | H |
| ★ **Batch Ledger** (after Split Failure) | Eligible jobs feed an output ledger at 5% Ledger Rate. | S |

### Resonance Calibrator

**Harmonist** raises calibration quality through focus. Introduces `STREAK_FLOOR` and `STREAK_CAP`.

| Node | Effect | Cost |
|---|---|---|
| **Resonant Streak** (root) | +1 Streak Floor: each consecutive calibration of the same family raises the stability floor by 1, up to a +10 Streak Cap. Changing pattern resets the streak. | N |
| +2 Streak Cap → **Sustained Tone** | +8 Streak Cap. | S → S |
| 5% reduced Energy Usage → **Second Pass** | Outputs under 40 stability are re-run once, paying FE and a catalyst again but not the input. | S → H |
| 8% increased Stability → **Clear Signal** | At the full streak, the stability ceiling rises by 5. | S → H |
| 8% increased Processing Speed → **Perfect Pitch** | 20% increased Calibration Precision. | S → S |
| ★ **Master Harmonic** (after Sustained Tone) | At the full streak, every 8th calibration comes out at exactly 100 stability; 30% less Processing Speed. | N |
| ★ **Pattern Memory** (after Second Pass) | The streak survives one pattern swap and breaking the machine. | N |

**Mass Tuner** calibrates in bulk. Introduces `COIL_REACH` and extends `PARALLEL_JOBS` to the Resonance Calibrator.

| Node | Effect | Cost |
|---|---|---|
| **Shared Field** (root) | One catalyst per cycle however many lanes run; stability ceiling −10. | H |
| 8% increased Processing Speed → **Stabilizer Economy** | Recipe stabilizers are consumed every other cycle. | S → H |
| 8% increased Calibration Precision → **Lane Sync** | 15% more Processing Speed when all lanes run the same pattern. | S → H |
| 5% reduced Energy Usage → **Catalytic Surplus** | 25% increased Catalyst Efficiency; the total stays below 100%. | S → S |
| 8% increased Stability → **Wide Tolerance** | +5 stability floor on multi-lane chassis. | S → H |
| ★ **Resonance Array** (after Lane Sync) | +1 Parallel Job per lane: each lane runs two jobs from one input stack; stability ceiling another −15. | N |
| ★ **Overreach** (after Wide Tolerance) | +1 Coil Reach; 20% less Calibration Precision. | S |

Mass Tuner's catalyst savings sit inside the Refinement Potential to FE cycle. Their final numbers depend on the loop audit passing.

### Melter

**Pressure Vessel** trades speed for more fluid. Introduces `FLUID_YIELD` and extends `FLUID_CAPACITY` to the Melter.

| Node | Effect | Cost |
|---|---|---|
| **Pressurized Tanks** (root) | +200% Fluid Capacity; +10% Fluid Yield on eligible melts; 20% less Processing Speed. | H |
| 10% increased Fluid Transfer → **Deep Intake** | +100% Fluid Capacity. | S → S |
| +3% Fluid Yield → **Methane Trap** | +25% Fluid Yield on methane from algae. | S → S |
| +3% Fluid Yield → **Brine Loop** | +25% Fluid Yield on Electrolyte Solution. | S → S |
| 10% increased Energy Capacity → **Sealed Lines** | A full output tank pauses work and keeps progress. | S → H |
| ★ **Autoclave** (after Brine Loop) | +15% Fluid Yield; 40% less Processing Speed. | S |
| ★ **Overpressure** (after Deep Intake) | Melts run 30% faster while the output tank is over half full. | H |

**Twin Crucible** runs parallel melts. Introduces `OVERLEVEL_SPEED` and extends `PARALLEL_JOBS` to the Melter.

| Node | Effect | Cost |
|---|---|---|
| **Second Crucible** (root) | +1 Parallel Job: runs two melts in parallel; 15% more Energy Usage. | N |
| 8% increased Processing Speed → **Crush Feed** | +10% Overlevel Speed per Crush Head level above the recipe's required level. | S → S |
| 8% increased Heat Transfer → **Flash Point** | 40% increased Heat Transfer. | S → S |
| 5% reduced Energy Usage → **Shared Heat** | Parallel melts after the first use 20% less FE. | S → H |
| 4% increased Max Temperature → **Lava Tap** | The lava recipe runs 50% faster. | S → H |
| ★ **Triple Crucible** (after Shared Heat) | +1 Parallel Job. | S |
| ★ **Fused Crucibles** (after Flash Point) | Parallel melting is off; 30% more Processing Speed per Parallel Job given up. | H |

Melter fluids feed generators through Electrolyte Solution, Methane, and lava, so the loop audit's FE pass covers every Melter yield node.

### Forestry Companion

The Forestry Companion's entry gate is its installed cutting tool head, and its Ascendancy panel is the shared panel in the cart's Mastery screen.

**Timber Baron** turns the cart into a log hauler. Introduces `CART_SPEED` and reuses `LEDGER_RATE`.

| Node | Effect | Cost |
|---|---|---|
| **Log Ledger** (root) | Harvested logs feed an output ledger at 10% Ledger Rate, paid into cargo; leaf cleanup 25% slower. | N |
| +2 Tree Fell Limit → **Sawyer's Eye** | +5 Tree Fell Limit. | S → S |
| 5% reduced Energy Usage → **Clean Fell** | Treefeller batches cost FE only for the logs actually cut, not the full Tree Fell Limit. | S → H |
| +10% Cart Speed → **Dock Sprint** | +50% Cart Speed while seeking transfer. | S → H |
| +3% Ledger Rate → **Heartwood** | Logs cut in one Treefeller batch feed the ledger twice. | S → H |
| ★ **Clearcut Charter** (after Sawyer's Eye) | +15% Ledger Rate; the cart stops planting. | H |
| ★ **Rolling Harvest** (after Dock Sprint) | The cart cuts and plants without stopping at each cell. | N |

**Grove Warden** grows the forest faster and denser. Introduces `GROWTH_PULSE` and reuses the managed-cell passive stat.

| Node | Effect | Cost |
|---|---|---|
| **Growth Pulse** (root) | As the cart passes a managed sapling, it spends FE on a growth pulse of bone-meal strength (Growth Pulse 1). Movement costs 25% more FE. | N |
| +2 managed cells → **Nursery** | +8 managed cells. | S → S |
| 10% increased Growth Pulse → **Rich Soil** | 50% increased Growth Pulse. | S → S |
| 5% reduced Energy Usage → **Seed Library** | Each managed cell remembers its tree species and replants the same species. | S → N |
| 5% increased Processing Speed → **Canopy Care** | Shears wear half as fast. | S → H |
| ★ **Ancient Grove** (after Nursery) | Plants 2×2 giant species (dark oak, jungle, spruce) when four matching saplings are in cargo. | N |
| ★ **Verdant Surge** (after Rich Soil) | Growth pulses reach every managed sapling within 3 blocks of the rail, not only the next cell. | N |

## UI

Follow [Machine Guidelines](../reference/machine-guidelines.md): graphical state with hover detail and minimal visible text.

- **Mastery tab:**
    - An Ascendancy button beside the machine's start node, as PoE shows the ascendancy tab near the class start.
    - The panel shows the chosen tree, earned and spent points, and an Ascend action. Hovering Ascend names the next Seal tier and why the action is unavailable, if it is.
    - Both live in `MasteryScreenSupport`, so every family, including the Forestry cart, gets them without per-screen work.
- **Choose dialog:** every ascendancy for the family side by side (paged when there are more than fit), read-only trees, root effects, and a confirm step.
- **Stats tab:** ascendancy stats appear only when an allocated node grants them.
- **Bonus Summary drawer:** an Ascendancy section, combined the same way as other effects.
- **Jade (sneak):** ascendancy name and points.
- **JEI:** Seal recipes plus an information entry explaining Seal use.

## Delivery Phases

1. **PRD and state.** This document and [its state page](machine-ascendancies-state.md).
2. **Framework, no content:**
    - catalog index loader and shared node parser;
    - stat declarations, behavior registry, and `ascendancyEntryStage()`;
    - state fields, codecs, and load validation;
    - `forFamily()` family check;
    - effect pipeline integration and build codes;
    - generic catalog validation and the fixture ascendancies;
    - domain checks.
3. **Loop safety:**
    - eligibility field on the four new recipe types, with Super Output routed through the shared check;
    - loop audit in report mode, reading declared yield stats and behaviors;
    - fix or allowlist the known cycles, then turn the CI gate on.
4. **Seals:** items, recipes, the `rngtech:ascendancy_seal_recipes_enabled` condition and its config key, the entry gate, and server actions for Ascend, choose, allocate, refund, and switch.
5. **UI:** Ascendancy panel and choose dialog in `MasteryScreenSupport`, Stats tab visibility, Bonus Summary section, Jade line, and JEI.
6. **Crusher and Furnace content**, including their new stats and shared mechanics, then an in-game playtest.
7. **Alloy Furnace, Metal Press, Resonance Calibrator, Melter, and Forestry Companion content.**
8. **Documentation:**
    - canonical Machine Mastery section and family pages;
    - [Machine Stats](../reference/machine-stats.md) entries for new stats;
    - [Current Implementation Matrix](../reference/current-implementation.md) and getting-started;
    - FTB Quests chapter and ModDex ascendancy view.

## Test Plan

Domain checks (`masteryCheck`):

- Codec and stream codec round trips with and without the new fields; existing saves load unchanged.
- Tier order, 2 points per tier, a 6-point maximum, a free root, notables behind their small nodes, and deep notables behind their parent notables.
- Refund price, connectivity after refunds, a non-refundable root, and switching that requires an empty tree and a Seal I while keeping tiers.
- Family mismatch across shared starts clears the ascendancy but keeps tiers.
- Ascendancy effects apply through `applyStats`, behaviors apply only to their owning families, and Gear legality is enforced.
- Build codes paste ascendancy nodes only onto the same family and ascendancy.
- Refiner's Oath: 5% per Parallel Job with a 50% cap.
- Seal use: next-tier order, the entry stage gate for Seal I (chassis stage, and tool head stage for the Forestry Companion), rejection without consuming the Seal, and creative use without consumption.
- The fixture ascendancies load, can be chosen, save by id, paste through build codes, and are cleared cleanly when retired. A retired node returns its point.
- New stats combine correctly with existing modifiers.
- Every ascendancy follows the tree shape rules, and its stats and behaviors are declared and supported by its family. Mutated fixtures are rejected with the matching violation.
- Language keys and icons exist for every shipped ascendancy and node.

ModDex checks (`npm run moddex:check`):

- The loop audit passes. The mutation fixture fails it.
- Every yield stat and behavior has loop-audit coverage.
- Every Seal recipe carries the `rngtech:ascendancy_seal_recipes_enabled` condition.

In-game checks:

- Choosing an ascendancy, Ascend with each Seal tier, a Seal from a loot table or quest, refunds, and switching.
- With the Seal recipe config off, Seal recipes are absent from crafting and JEI, and a given Seal still works.
- Ascendancy stats appear on the Stats tab only when granted.
- The Forestry cart's Ascendancy panel and entry gate.
- Drops and pick-block, the Forestry cart item form, multiplayer sync, Jade, and JEI.

## Acceptance Criteria

- Every shipped family has at least two ascendancies that follow the tree shape rules. For 2.0 that means at least the Crusher and Furnace.
- Tiers I–III grant 2 points each, for 6 in total, separate from the shared tree's 120 allocations.
- Using a Seal grants its 2 points immediately. Seal I requires entry stage 4. Seal recipes can be disabled by config or datapack without affecting Seals from other sources.
- The fixture ascendancies work with only their catalog files and declarations, which proves new ascendancies need no family code.
- Existing progression loads unchanged, and ascendancy state survives drops and pick-block.
- The loop audit is a CI gate and passes, with every allowlist entry justified.
- No ascendancy node lowers authored recipe input counts, raises Refinement Potential, or bypasses a `bonus_output` opt-out.
- `./gradlew ciCheck`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` pass.

## Release Scope

Ascendancies are part of the 2.0 release.

- **2.0 must include:**
    - phases 1–6: framework, loop safety, Seals, UI, and the Crusher and Furnace ascendancies;
    - the phase 8 documentation for everything that ships.
- **In 2.0 if ready before the tag:** phase 7 families (Alloy Furnace, Metal Press, Resonance Calibrator, Melter, Forestry Companion). Otherwise their ascendancies stay Planned.
- **Release gate:** the full in-game checklist in [Releasing](../releasing.md) runs once for 2.0 after every 2.0 feature lands.

## Decisions

Resolved 2026-09-29:

- Tier I Seals use Stage 5 materials.
- There are no work-based trials. Using a Seal grants its points, and crafting the Seal is the trial.
- Ascendancy state stays with the chassis.
- Ascendancies ship in 2.0.
- The ascendancy catalog is not datapack-overridable. Pack makers can instead disable Seal recipes and award Seals another way.
- The Forestry Companion gets ascendancies. Its entry gate uses the installed cutting tool's head stage.
- Deep notables must define the build. Tradeoffs are optional and can pay for more power.
- The system must support adding ascendancies later.
- Ascendancies may introduce new stats and behaviors, and are encouraged to.

## Related: Tome of XP

The 21 Mastery allocations above the 99 level points are reserved for a separate restricted item, the Tome of XP. Pack makers wire it into recipes, loot, or quests; RNGTech ships no default source. It is not part of this PRD.

If both features change the save format in the same branch:

- `MachineProgressionState` currently clears every allocation when the count exceeds `level - 1`. That check and `totalPoints()` must also count Tome points, or a Tome-funded build is erased on load.
