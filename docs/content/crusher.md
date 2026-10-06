# Crusher

Status: Prototype


The crusher is the first implemented RNGTech processing machine family.

Current placed Crusher chassis ids:

- `rngtech:wooden_crusher_chassis`
- `rngtech:iron_crusher_chassis`
- `rngtech:copper_crusher_chassis`
- `rngtech:bronze_crusher_chassis`
- `rngtech:steel_crusher_chassis`
- `rngtech:aluminum_crusher_chassis`
- `rngtech:titanium_crusher_chassis`
- `rngtech:tungstensteel_crusher_chassis`
- `rngtech:exotic_crusher_chassis`

The standalone legacy `rngtech:crusher` block and item are not registered. `rngtech:crusher` remains the Crusher recipe type id.

## Visual Design

Crusher placed block art follows [Machine Visual Design](../reference/machine-visual-design.md). Staged chassis use owned `textures/block/crusher_chassis/<face>/<id>` face textures with jaw-and-roller fronts, side throughput panels, top feed hatches, and bottom output hatches. All Crusher block faces use `64x64` frames, and active states keep the motion cue localized to the front working face.

## Behavior

The crusher processes ore-related inputs into crushed outputs. Non-alloy crushed materials can run through a second Crusher pass to produce dust for the faster and cheaper smelting chain.

It exposes NeoForge item and energy capabilities so hoppers and other modded automation can interact with it through standard APIs.

The crusher screen has Processing, Gear, Stats, Refinement, and Mastery tabs. The Processing tab shows the input, output, energy bar, processing bar, banked output-bonus bar, a recipe-state status square, and a Battery Cell status square; hover text reports exact progress, per-job FE/t, total batch FE/t when applicable, projected FE per craft, under-hardness time and FE penalty, jam chance, output-bonus payout preview, status, and no-cell output penalties. The Gear tab exposes the crush-head component slot and Battery Cell slot. The Stats tab exposes Crusher-specific prefix stats such as output-guard grace, no-cell retention, high-hardness mitigation, input filtering, and salvage chance. The Refinement tab targets the placed crusher itself, accepts one refinement catalyst, and applies the shared refinement rules. The Mastery tab shows the placed Crusher chassis' machine-owned XP, level, unspent passive points, and a fixed passive tree inside a panning viewport. The tree starts from a green lower-left node, expands through layered clusters for energy capacity, energy use, bonus output, processing speed, and high-hardness stability, lets some paths cross between regions, and puts keystones on the outer layer as chase choices with tradeoffs.

## Implementation Contract

Current runtime surface:

- Block and item ids: staged Crusher chassis blocks from Wooden through Exotic. The standalone legacy `rngtech:crusher` block and item are no longer registered.
- Block entity: `CrusherBlockEntity`.
- Menu and screen: `CrusherMenu` and `CrusherScreen`.
- Slots: input, output, one Gear-tab Battery Cell slot, one Gear-tab crush-head slot, and one shared Refinement-tab catalyst slot.
- Machine progression: Crusher processing recipes can grant machine XP through `machine_xp`; XP, level, spent points, and unlocked Mastery nodes are stored on `rngtech:machine_progression`.
- Item capability automation: top inserts input, sides insert Battery Cells, bottom extracts output. Crusher Feed Control prefixes improve the top input handler's recipe, output-space, output-bonus-bank, and dense-batch output filtering.
- Energy capability: accepts FE into the small internal buffer and the installed Battery Cell. External intake is limited by free storage space and the source or attached Universal Connector, not by a Crusher-side transfer stat.
- Battery Cell slot runtime behavior: the crusher has only a small internal working buffer without an installed Battery Cell. The installed cell provides the meaningful FE storage and keeps its stored energy on the item stack.
- No-cell production penalty: when no Battery Cell is installed, effective `OUTPUT_AMOUNT` is multiplied by `crusher.noBatteryCellOutputMultiplier`; Crusher Battery Link prefixes retain part or all of the lost multiplier.
- Gear tab accepts staged `CRUSH_HEAD` machine parts in the crush-head slot. A valid Crush Head is required for processing, and its stage may not exceed the placed chassis stage. The installed part's authored base profile plus stored rolled affixes contribute to effective crusher stats and define the Crusher's active hardness level.
- Crusher chassis materials provide authored base stats for internal buffer scale, energy usage, processing speed, output amount, and fixed behavior flags such as Output Guard. Crusher Frame prefixes can also enable Output Guard on rolled machines, and Crusher Throughput prefixes add Batch Size.
- Refinement applies to the placed crusher's machine traits, not to the installed part.
- Mastery applies to the placed crusher chassis itself, not to a player. Breaking and replacing the chassis keeps Mastery state when the chassis item survives and its loot path copies `rngtech:machine_progression`.

Current stat hooks:

- `ENERGY_CAPACITY` scales configured internal FE storage, while `ENERGY_CAPACITY_FLAT` adds FE before that scaling.
- `ENERGY_USAGE` affects total FE consumed per craft.
- `PROCESSING_SPEED` affects processing time. Faster processing raises FE draw per tick so speed does not make the recipe cheaper by itself.
- `PROCESSING_LEVEL` is the active Crush Head hardness level. Crusher recipes with higher `required_processing_level` still run, but each missing level adds configured processing time, total FE cost, and jam risk, and disables positive output bonuses and chance procs.
- `OUTPUT_AMOUNT` affects produced stack count. Fractional output is banked deterministically toward the next bonus item and resets when the input item changes. Under-level Crusher recipes cap positive output amount at base output, while still allowing output penalties such as the missing-Battery-Cell penalty.
- [`BATCH_SIZE`](../reference/machine-stats.md) lets a Crusher complete several recipe jobs from one input stack in one cycle. Tungstensteel batches up to `4` and Exotic up to `9`. Each extra item adds [`BATCH_OVERHEAD`](../reference/machine-stats.md) to the cycle time.
- `OUTPUT_GUARD_GRACE` preserves in-progress work for a fixed number of output-blocked ticks before normal progress reset.
- `NO_BATTERY_OUTPUT_RETENTION` reduces the missing-Battery-Cell output penalty by retaining part of the lost output multiplier.
- `HIGH_HARDNESS_ENERGY_MITIGATION` reduces only the configured high-hardness Crusher energy surcharge before `ENERGY_USAGE`.
- `CRUSHER_INPUT_FILTER` improves top-side automation filtering by tier.
- `CRUSHER_SALVAGE_CHANCE` is a separate chance to add one extra copy of the base output item after deterministic `OUTPUT_AMOUNT` and Super Output are evaluated. Under-level Crusher recipes do not roll it.
- `REFINEMENT_POTENTIAL` is shown for refinement state.

Current Mastery hooks:

- Crushers enter the [shared Machine Mastery tree](../systems/machine-mastery.md) at Drive. Successful jobs grant band-scaled XP per completed job, up to level 100 and 99 passive points.
- Paths, refunds, copy/paste, automatic allocation, attributes, and save migration follow the shared rules. Current Gear must remain legal for every allocation and refund.
- Cell Bypass grants 100% increased Energy Capacity, 10% more Energy Usage, and +100 percentage points of no-cell output retention. It blocks the Battery Cell slot and cannot be allocated with a cell installed.
- Precision Jaw Mount grants 10% increased Output Amount and +2 percentage points of salvage chance. An installed Crush Head must match the chassis stage.
- Dense Batching adds two Batch Size and applies 25% less Processing Speed and 25% more Energy Usage.
- Soft Material Specialist grants 200% more Processing Speed and 50% more Energy Usage, but forbids recipes above hardness 2 even with a stronger head. Its speed is [tagged](../systems/machine-mastery.md#tagged-payoffs) for Crushers, as is Cell Bypass's Energy Capacity, because only a Crusher pays their hardness and Battery Cell costs.
- Single Pass disables bonus output: Output Amount cannot exceed the base, and Super Output and salvage chances drop to zero. In exchange it grants 30% more Processing Speed. It never raises the no-cell Output Amount penalty.
- Heavy Yield grants 15% more Output Amount and 50% more Energy Usage.
- Component Mount, Reinforced Mount, Stage Adapter, and Universal Bracket each support one higher Crush Head stage; Precision Jaw Mount still requires an exact match. Silent Operation mutes the machine's client-side loop and cues.


Implementation checks:

- Hopper or item-pipe insertion from the top should insert processable inputs.
- Side insertion should only insert Battery Cells.
- Bottom extraction should extract completed output.
- A Crusher without a valid Crush Head should not process, consume FE, or keep partial progress.
- A Crusher should reject Crush Heads above the placed chassis stage plus allocated Component Stage Support.
- A Crusher with Precision Jaw Mount should reject Crush Heads whose stage does not match the chassis stage.
- A Crusher with Cell Bypass should reject Battery Cell insertion and should not count an already-present Battery Cell as installed.
- A Crusher should reject Mastery unlock requests that lack points, connected paths, or compatible current Gear.
- A Crusher should process recipes whose `required_processing_level` is higher than its effective `PROCESSING_LEVEL`, but at the configured under-hardness time, total-FE, and jam-risk penalty, unless a Mastery hard ceiling forbids the recipe.
- An under-level Crusher cycle should not roll Instant Process, Super Output, or Crusher Salvage, and should not apply positive `OUTPUT_AMOUNT` above the authored base output.
- A charged Battery Cell in the Gear tab should contribute to processing work after the crusher's small internal buffer is used.
- Removing the Battery Cell should reduce the effective output amount shown in the Stats tab and used by the output-bonus bar.
- A Flint Crush Head should process under-level raw ore recipes slowly with jam risk instead of rejecting them.
- An Iron or higher Crush Head should require a matching or higher-stage Crusher chassis.
- A crusher with `1.25x` output amount should advance the output-bonus bar after completed crafts by the banked fractional output contribution, preview the next payout in hover text, and grant an extra output when the bank reaches a full item.
- Batching Crushers lock their job count at the start of a cycle, consume FE for that job count every tick, and only start a batch when the output slot can accept the whole deterministic batch output.
- Crusher Frame rolls should pause progress during output blockage for their grace window and then reset when the grace expires.
- Crusher Feed Control rolls should reject top automation insertion according to their tier while still allowing manual slot use.
- Crusher Salvage rolls should remain separate from deterministic `OUTPUT_AMOUNT` and Super Output, and should only apply when the output slot can accept the extra item.
- Placed-machine refinement should persist after closing and reopening the screen.
- Machine progression should persist after closing/reopening the screen, world save/load, break/place, pick-block, and inventory movement of the chassis item.

## Recipe Use

Crusher processing uses `rngtech:crusher` recipe JSON files under `data/<namespace>/recipe/`.

Crusher recipes currently support:

- One item ingredient.
- An optional `input_count` for recipes that consume more than one item per craft.
- An optional malformed-ingot material selector for datapack recovery recipes.
- One output item stack.
- A per-recipe processing time in ticks.
- A per-recipe FE cost.
- A per-recipe required processing level. This is the recipe's optimal crush hardness target; under-level processing is allowed but slower, more expensive, bonus-suppressed, and can jam.
- A per-recipe `bonus_output` flag. Recipes with `bonus_output: false` always produce their authored base stack and ignore `OUTPUT_AMOUNT`, Super Output, and Crusher salvage.
- A per-recipe `machine_xp` value. It is granted to the placed Crusher chassis once per successfully completed job, after recipe-band falloff. Shipped reversible `bonus_output: false` recipes grant `0` machine XP.
- An optional per-recipe `machine_xp_band` value. If omitted, recipes with positive `machine_xp` map `required_processing_level` onto the Crusher Mastery curve through the [shared work-band mapping](../systems/machine-mastery.md#xp-and-chassis-ownership). Use an explicit band when a recipe's training value should follow the processed material rather than the mechanical hardness gate.

With JEI installed, Crusher recipes are exposed with their input, input count, output, processing ticks, scaled FE cost, base output count, required processing level, and bonus-output opt-outs. Recipes at or above the configured high-hardness threshold show the multiplied FE cost.

Machine stats still control effective output amount for eligible recipes, processing speed, and energy usage.

Crusher chassis recipes are standalone by stage. Crafting a Copper or later Crusher Chassis does not consume an earlier Crusher Chassis, so players can keep older rolls and compare chassis side by side.

Higher-stage chassis still use stage-appropriate materials and calibrated kinetic components as progression gates.

Crush Head recipes are also standalone by stage. They use current-stage plates as the primary jaw body, previous-stage plates or primitive flint as backing, a current-stage gear through Titanium, and calibrated kinetic components for Tungstensteel and Exotic instead of consuming the previous Crush Head stack.

Crusher under-hardness rules apply only inside the Crusher recipe system. World ore harvesting still uses the Modular Pick and Hammer ore-hardness gate, and tools that do not meet a block's ore hardness do not harvest that block.

The Gear-tab Battery Cell slot accepts material-specific Battery Cell items. When a valid crusher recipe is waiting and the crusher needs FE, work draws spend internal buffer FE first and then draw the remaining tick cost from the installed cell without consuming the cell item. FE received through the block energy capability fills the internal buffer first, then charges the installed Battery Cell.

The modular energy storage model is the [Battery Chassis](battery-chassis.md) system.

## Base Stats

Code-backed crusher base values:

| Stat or value | Base | Notes |
|---|---:|---|
| Internal FE storage | `200 FE` | Config key: `crusher.internalEnergyCapacity`; flat additions apply through `ENERGY_CAPACITY_FLAT`, then the total is scaled by `ENERGY_CAPACITY`. This is only the built-in working buffer before an installed Battery Cell is counted. |
| Battery Cell storage | Varies by cell | The installed cell contributes its own stored FE, capacity, input rate, output rate, efficiency, and modifiers. |
| No-cell output multiplier | `0.75x` | Config key: `crusher.noBatteryCellOutputMultiplier`; applied to `OUTPUT_AMOUNT` while no Battery Cell is installed. |
| Block FE intake | Free storage plus source or connector limit | The Crusher no longer has a meaningful machine-side external input cap. Universal Connector tier is the intended visible wiring limit. |
| Processing energy cost | Recipe-defined FE per craft | Authored by each `rngtech:crusher` recipe's `energy` field. Stage 5-8 material-form recipes use higher authored base costs, then the high-hardness rule applies when applicable, then `ENERGY_USAGE` adjusts the result. |
| High-hardness threshold | `7` processing level | Config key: `crusher.highHardnessEnergyThreshold`; recipes at or above this required level use the high-hardness multiplier. |
| High-hardness FE multiplier | `2.0x` | Config key: `crusher.highHardnessEnergyMultiplier`; applied before `ENERGY_USAGE`. |
| Under-hardness penalty | `+1.0x` time and FE per missing level | Config key: `crusher.underLevelPenaltyMultiplierPerLevel`; extends processing ticks and total FE by the same multiplier so FE/t does not fall. Also suppresses positive `OUTPUT_AMOUNT`, Instant Process, Super Output, and Crusher Salvage. |
| Under-hardness jam chance | `5%` per missing level | Config key: `crusher.underLevelJamChancePerLevel`; chance is rolled once when an under-level cycle starts. |
| Under-hardness jam duration | `40 ticks` per missing level | Config key: `crusher.underLevelJamTicksPerLevel`; a jam pauses work and clears automatically after the cooldown. |
| Output Guard grace | `0 ticks` | Added by Crusher Frame prefixes: `100`, `200`, `400`, or `800` blocked ticks. |
| No-cell output retention | `0%` | Added by Crusher Battery Link prefixes: `25%`, `50%`, `75%`, or `100%` of the lost no-cell multiplier. |
| High-hardness mitigation | `0%` | Added by Crusher Compression prefixes: `15%`, `30%`, `45%`, or `60%` of the surcharge above `1.0x`. |
| Crusher input filter | `0` | Added by Crusher Feed Control prefixes; tiers filter top automation by recipe, output acceptance, output-bonus-bank compatibility, and dense-batch output acceptance. |
| Crusher salvage chance | `0%` | Added by Crusher Salvage prefixes using the shared `1-2%`, `2-3%`, `3-4%`, `4-5%` chance table. |
| Input slots | `1` | Config key: `crusher.baseInputSlots`; current UI exposes one processing input. |
| Output slots | `1` | Fixed current output slot. |
| Processing speed | `1.0x` | Adjusts recipe processing ticks. |
| Processing level | `0` machine base, `1-8` from staged Crush Heads | `PROCESSING_LEVEL` sets the optimal recipe hardness target. The Crusher machine contributes none by itself; installed Crush Heads provide the active value. |
| Output amount | `1.0x` | Multiplies recipe output count and banks fractional extra output toward the next bonus item. |
| Batch size | `1`, `4`, or `9` | Stages 0-6 batch one item. Tungstensteel batches up to four. Exotic batches up to nine. |
| Gear slots | `2` | One required Crush Head slot and one Battery Cell slot in the Gear tab. |

## Tiered Crusher Prefixes

Crusher-specific prefixes use tier-aware name keys, so the rolled tier changes the displayed word while the family id remains stable for refinement.

| Family | Tier names | Effect | Lens |
|---|---|---|---|
| `crusher_frame` | Braced / Girded / Trussed / Monolithic | Enables Output Guard and preserves output-blocked progress for `100 / 200 / 400 / 800` ticks. | None |
| `crusher_kinetics` | Weighted / Flywheeled / Counterweighted / Inertial | Adds internal FE capacity from the shared flat machine-capacity table. | Power |
| `crusher_jaws` | Lined / Hardened / Faceted / Diamond-Jawed | Prefix-side `OUTPUT_AMOUNT` from the shared percent table; conflicts with other output-amount affixes. | Yield |
| `crusher_ore_handling` | Sorted / Screened / Segmented / Stratified | Adds `SUPER_OUTPUT_CHANCE` from the shared chance table; conflicts with Super Output. | Yield |
| `crusher_battery_link` | Tapped / Cell-Fed / Bus-Linked / Grid-Coupled | Retains `25% / 50% / 75% / 100%` of the output multiplier lost when no Battery Cell is installed. | Efficiency |
| `crusher_feed_control` | Guided / Metered / Indexed / Synchronized | Improves top automation input filtering by tier. | None |
| `crusher_compression` | Clamped / Pressured / Force-Bound / Hypercompressed | Reduces only the high-hardness energy surcharge by `15% / 30% / 45% / 60%`. | Efficiency |
| `crusher_vibration` | Dampened / Isolated / Stabilized / Anchored | Prefix-side reduced `ENERGY_USAGE` from the shared percent table; conflicts with other energy-usage affixes. | Efficiency |
| `crusher_throughput` | Belted / Chain-Driven / Shaft-Driven / Turbine-Coupled | Adds `+1 / +2 / +3 / +5` `BATCH_SIZE`. | Kinetic |
| `crusher_salvage` | Picking / Sifting / Winnowing / Reclaiming | Adds `CRUSHER_SALVAGE_CHANCE`; on success, adds one extra base-output item if the output slot can accept it. | Yield |

## Crush Head Prefixes

Crush Head prefixes are stored on the installed part and contribute through the Gear slot. The main stat families use tier-aware name keys, so the rolled tier changes the displayed word while the family id remains stable for refinement. Scuffed and Dust-Grooved are low-impact filler prefixes that share the same family roll weight as the stronger head prefixes.

| Family | Display names | Effect | Lens |
|---|---|---|---|
| `crush_head_pulverizing` | Coarse / Grinding / Pulverizing / Micronizing | Prefix-side `OUTPUT_AMOUNT` from the shared percent table; conflicts with other output-amount affixes on the same head. | Yield |
| `crush_head_jagged` | Nicked / Jagged / Serrated / Rending | Adds `CRUSHER_SALVAGE_CHANCE` from the shared chance table. | Yield |
| `crush_head_kinetic` | Quickened / Kinetic / Momentum-Driven / Impulse-Forged | Prefix-side `PROCESSING_SPEED` from the shared percent table; conflicts with other processing-speed affixes on the same head. | Kinetic |
| `crush_head_scuffed` | Scuffed | Untiered `+6% NO_BATTERY_OUTPUT_RETENTION`; only softens the missing-Battery-Cell output penalty. | Control / Efficiency |
| `crush_head_dust_groove` | Dust-Grooved | Untiered `+1 CRUSHER_INPUT_FILTER`; rejects non-recipe items from top automation. | Control |

## Chassis Stages

Crusher chassis are placed machine blocks. They roll Crusher machine traits when crafted and keep those traits when placed, refined, broken, and picked up. Each chassis recipe is independent of the other Crusher chassis, so one material branch does not consume or overwrite another machine.

| Stage | Chassis | Identity |
|---:|---|---|
| 0 | Wooden | Primitive body, weak buffer, poor speed, and higher energy usage. |
| 1 | Iron | Baseline powered Crusher body. |
| 2 | Copper | Better energy usage, lighter buffer. |
| 3 | Bronze | Better output amount with higher energy pressure. |
| 4 | Steel | Durable and efficient, but slower. |
| 5 | Aluminum | Fast and efficient, lighter buffer. |
| 6 | Titanium | High throughput with stronger output. |
| 7 | Tungstensteel | Heavy output-focused body with slower handling, Output Guard, and a batch of up to `4`. |
| 8 | Exotic | Endgame body with Output Guard, a full-batch Naquadah working buffer, and a batch of up to `9`. |

## Balance Position

The Crusher baseline is intended to feel hard-tech: expensive, staged, and dependent on cells and power infrastructure. Modifiers can move a good machine toward a lighter pack feel, but the unmodified machine should not be cheap throughput.

Base FE per craft:

```text
base total FE = recipe energy * high-hardness multiplier when required_processing_level >= threshold
hardness deficit = max(0, required_processing_level - active PROCESSING_LEVEL)
under-level penalty multiplier = 1 + hardness deficit * under-level penalty
effective FE per craft = base total FE * ENERGY_USAGE * under-level penalty multiplier
adjusted processing ticks = speed-adjusted recipe ticks * (1 + BATCH_OVERHEAD * (active jobs - 1)) * under-level penalty multiplier
effective FE/t = effective FE per craft / adjusted processing ticks
batch FE/t = effective FE/t * active jobs
```

Stage 5-8 material-form Crusher recipes use authored FE ramps before machine modifiers: Stage 5 costs `2.0x`, Stage 6 costs `3.0x`, Stage 7 costs `4.0x`, and Stage 8 costs `6.0x` compared to the early-stage base. This applies to raw or ore inputs producing crushed material and to non-alloy crushed material producing dust.

Separately, recipes with `required_processing_level >= 7` cost `2.0x` FE before `ENERGY_USAGE` is applied. The current shipped Crusher recipes affected by that rule are the Naquadah raw and ore block recipes at hardness `8`, so those recipes stack the Stage 8 authored recipe cost with the high-hardness surcharge.

Under-hardness work multiplies total FE per craft by the same factor as processing time, so FE/t stays comparable to optimal-hardness work while the craft becomes slower and more expensive overall. At the default `+1.0x` penalty per missing level, an Iron Crush Head processing a Titanium raw or ore block recipe (`2 / 5`) takes `4.0x` the normal processing ticks, costs `4.0x` FE, and has a `15%` jam chance at cycle start. A jam consumes no input, resets the in-progress cycle, pauses the Crusher for the configured cooldown, and then clears automatically.

Under-hardness also disables positive production bonuses and chance procs. The recipe still produces its authored base output, but positive `OUTPUT_AMOUNT`, Instant Process, Super Output, and Crusher Salvage do not apply until the installed Crush Head meets the recipe's required processing level. Output penalties such as the missing-Battery-Cell penalty can still reduce output.

At base stats with an installed Crush Head and Battery Cell, before high-hardness and under-hardness surcharges:

| Recipe input | Hardness | Minimum head | Ticks | Base output | Optimal FE per craft | Optimal FE per output |
|---|---:|---|---:|---:|---:|---:|
| Raw material item | Matches source ore, `2-8` | Matching ore head | `120` | `2` crushed | `5,760 FE * stage ramp` | `2,880 FE * stage ramp` |
| Stone ore block | `2` | Iron | `160` | `3` crushed | `7,680 FE * stage ramp` | `2,560 FE * stage ramp` |
| Deepslate ore block | `2` | Iron | `200` | `3` crushed | `9,600 FE * stage ramp` | `3,200 FE * stage ramp` |
| Non-alloy crushed material | `1` | Flint | `60` | `1` dust | `1,200 FE * stage ramp` | `1,200 FE * stage ramp` |
| Iron Ingot to Iron Dust | `1` | Flint | `30` | `1` dust | `600 FE` | `600 FE` |
| Coal Dust from coal | `0` | Flint | `60` | `1` dust | `600 FE` | `600 FE` |
| Organic Reagent from crop or non-seed plant biomass | `0` | Flint | `120` | `1` reagent | `600 FE` | `600 FE` |
| Compost Feedstock from seeds | `0` | Flint | `16` | `1` feedstock | `80 FE` | `80 FE` |

Ingot-to-dust Crusher recipes set `bonus_output: false` because their dust outputs can smelt into ingots. They remain useful for producing dust but cannot use Crusher output bonuses as a reversible duplication loop.

Without an installed Battery Cell, the Crusher can still run from direct FE input through its tiny internal buffer, but production is penalized:

| Recipe input | Normal base output | No-cell average output |
|---|---:|---:|
| Raw material item | `2` crushed | `1.5` crushed |
| Stone ore block | `3` crushed | `2.25` crushed |
| Deepslate ore block | `3` crushed | `2.25` crushed |

`PROCESSING_SPEED` improves throughput, not energy efficiency. A faster crusher completes the craft in fewer ticks and draws more FE each tick to preserve the recipe's effective total cost. `ENERGY_USAGE` and `OUTPUT_AMOUNT` are the main ways modifiers make the machine feel lighter.

## Ascendancies

Status: Prototype

Crusher machines choose between Rockbreaker and Assayer when they use their first Ascendancy Seal. [Machine Mastery](../systems/machine-mastery.md#ascendancies) defines Seals, points, refunds, and switching, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines the new stats. The tables below are generated from the ascendancy catalog.

- Rockbreaker makes under-level crushing viable. Hardness Tolerance forgives missing Crush Head levels for time, FE, and jam risk, but the Crusher still counts as under level, so bonus output stays off.
- Assayer turns the Output Amount bank into a per-input ledger. Bank Memory keeps one bank per remembered input, and Wide Ledger saves the banks in `rngtech:output_banks` so they survive drops and pick-block.
- Compound Yield rolls Super Output a second time when a craft’s bank pays out and copies the items the bank paid. Tailings Recovery banks one whole item in the current input’s bank. Mother Lode counts eligible cycles per remembered input.
- Refiner’s Oath turns batching off for 5% more Output Amount per point of Batch Size, up to 50%.

<!-- ascendancy-trees:start -->

### Rockbreaker

| Node | Type | After | Effect |
|---|---|---|---|
| **Breaker’s Stance** | Root | — | +1 Hardness Tolerance. |
| Quick Release | Small | Breaker’s Stance | +10% Jam Recovery. |
| **Jam Breaker** | Notable | Quick Release | 50% less Jam Chance; +40% Jam Recovery. |
| Tempered Jaws | Small | Breaker’s Stance | +3% Hardness Energy Mitigation. |
| **Fault Lines** | Notable | Tempered Jaws | Under-level cycles keep positive Output Amount. Super Output and salvage stay off. |
| Salvage Grit | Small | Fault Lines | +2% Crusher Salvage. |
| **Rubble Reclaimer** | Deep notable | Salvage Grit | Under-level cycles can roll salvage at half chance. |
| Steady Pressure | Small | Breaker’s Stance | +10% Under-Level Efficiency. |
| **Pressure Stacking** | Notable | Steady Pressure | +40% Under-Level Efficiency. |
| Deep Pressure | Small | Pressure Stacking | +10% Under-Level Efficiency. |
| **Bedrock Bite** | Deep notable | Deep Pressure | +1 Hardness Tolerance. |
| Lean Crushing | Small | Breaker’s Stance | 5% reduced Energy Use. |
| **Shatter Point** | Notable | Lean Crushing | The extra FE cost of high-hardness recipes is halved. |

### Assayer

| Node | Type | After | Effect |
|---|---|---|---|
| **Assay Ledger** | Root | — | +4 Bank Memory. |
| Ledger Pages | Small | Assay Ledger | +2 Bank Memory. |
| **Wide Ledger** | Notable | Ledger Pages | +6 Bank Memory. Remembered bonus banks survive breaking and pick-block. |
| Sworn Yield | Small | Wide Ledger | 4% increased Bonus Output. |
| **Refiner’s Oath** | Deep notable | Sworn Yield | Batching is off. 5% more Output Amount per point of Batch Size, up to 50%. |
| Rich Assay | Small | Assay Ledger | 4% increased Bonus Output. |
| **Compound Yield** | Notable | Rich Assay | A bonus bank payout can also trigger Super Output. |
| Vein Sense | Small | Compound Yield | +1% Super Output. |
| **Mother Lode** | Deep notable | Vein Sense | Super Output Cadence fixed at 16 cycles. |
| Lucky Strike | Small | Assay Ledger | +1% Super Output. |
| **Tailings Recovery** | Notable | Lucky Strike | +3% Crusher Salvage. Salvage that does not fit the output is banked instead of lost. |
| True Measure | Small | Assay Ledger | +5% At-Level Output. |
| **Matched Hardness** | Notable | True Measure | +15% At-Level Output. |

<!-- ascendancy-trees:end -->

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine implicit | Crusher chassis material identity defines base stats through `MachineBaseStatCatalog` and behavior flags through `MachineImplicitCatalog`; Tungstensteel and Exotic author base `BATCH_SIZE` `4` and `9`, and Steel, Titanium, Tungstensteel, and Exotic carry `OUTPUT_GUARD`. |
| Machine prefix | Crusher-specific Frame, Kinetics, Jaws, Ore Handling, Battery Link, Feed Control, Compression, Vibration, Throughput, and Salvage families. |
| Machine suffix | `PROCESSING_SPEED`, `ENERGY_USAGE`, `OUTPUT_AMOUNT`, Instant Process, Super Output, Overclocked, and Bulk Speed. New Crusher rolls no longer present `ENERGY_TRANSFER` as a useful machine stat. |
| Machine enchant | See [Modifier Eligibility](../reference/modifier-eligibility.md). |
| Machine part modifiers | Crush Heads have authored base `PROCESSING_LEVEL`, speed, and sometimes output amount. They can roll Pulverizing, Jagged, Kinetic, Scuffed, and Dust-Grooved prefixes plus `PROCESSING_SPEED`, `OUTPUT_AMOUNT`, Crushing, Instant Process, and Super Output suffixes; those rolls scale the Crush Head contribution before the host Crusher's own rolls apply. |

Crusher uses the `CRUSHER` modifier eligibility profile. The installed crush head uses the `CRUSH_HEAD` profile. See [Modifier Eligibility](../reference/modifier-eligibility.md#current-code-profiles).

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Battery Chassis](battery-chassis.md)
- [Battery Cells](battery-cells.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Visual Design](../reference/machine-visual-design.md)
- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
