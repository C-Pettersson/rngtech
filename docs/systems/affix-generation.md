# Affix Generation

Player guide: [Rarity and Affixes](https://c-pettersson.github.io/rngtech/rarity-and-affixes/)

Affix generation describes how modifier-bearing item stacks and placed machines receive authored base profiles, implicit modifiers, prefix and suffix affixes, and refinement changes.

Use [Modifiers](../modifiers/index.md) for the modifier inventory and [Modifier Eligibility](../reference/modifier-eligibility.md) for capability and profile eligibility.

## Modifier Slots

| Slot | Meaning |
|---|---|
| Implicit | Fixed authored modifiers or behaviors built into an item, block, machine, or part identity. Implicit modifiers use their own pool and are not prefix or suffix affixes. Machine numeric identity is usually an authored base stat profile instead of an implicit modifier. |
| Prefix | Rolled modifier before the item name. |
| Suffix | Rolled modifier after the item name. |
| Enchant | Special modifier layer used for enchant-style effects. |

Prefix and suffix modifiers each provide a named affix word. Magic machines use one prefix and one suffix in their generated name. Rare machines may have multiple prefix or suffix modifiers; their generated name uses the first prefix and first suffix, while the remaining affix words remain visible in tooltips.

When an item stack is created, its identity may add an authored base stat profile, fixed implicit modifiers, or fixed behavior flags, then its rarity roll adds actual prefix and suffix affixes from the legal affix pools. Refinement consumables operate on rolled affixes and do not remove authored base profiles or fixed identity implicits.

Current generation rolls rarity and Refinement Potential independently. Rarity controls starting affix count, while Refinement Potential comes from component stage. Starting modifiers do not spend or reduce that rolled potential.

Current prototype rarity weights are stage-sensitive. Stage `0` targets roll `35%` Normal, `50%` Magic, and `15%` Rare. Stage `1-4` targets roll `70%` Magic and `30%` Rare with no natural Normal outcome, making early crafted gear reliably show affixes while it is still likely to be replaced later. Stage `5+` targets roll `50%` Normal, `40%` Magic, and `10%` Rare. Unique remains authored-only.

After rarity and slot-count rules choose a slot, generation uses two weighted steps. First, it filters legal affix families by target profile, slot, `CAN_ROLL`, target stats, and `mod_group` conflicts, then chooses one family by roll weight. Second, it chooses a tier for that family by combining the global tier weight, the family's tier weight, and component-stage weighting. Rarity controls affix count only; it does not control tier eligibility. Ordinary families default to family weight `100` and equal tier base weights unless code overrides them.

Modular Field Tool Heads and Tool Rods use the same stage-sensitive rarity and tier weights, but their field-tool tuning gives the component-stage overflow multipliers `+5` percentage points. Their Refinement Potential range is the normal component-stage range plus `2`.

Current Refinement Potential ranges are:

| Component stage | RP range |
|---:|---:|
| 0 | `1-3` |
| 1-2 | `6-10` |
| 3-4 | `8-14` |
| 5-6 | `14-22` |
| 7-8 | `22-32` |

The Crude and Steel Metal Press are the current exception to physical-stage weighting. Their bodies remain Stage 3 and Stage 4 progression content, but modifier generation, rerolls, and refinement use Stage 8 weighting and the Stage 8 Refinement Potential range because Metal Press bodies are not part of a staged chassis upgrade ladder that preserves investment.

Natural starting modifier tiers are weighted by component stage. A lower-stage target can rarely roll above its natural tier, but stage still controls the normal result and Refinement Potential budget:

| Component stage | Natural modifier tier target |
|---:|---:|
| 0 | 1 |
| 1-2 | 2 |
| 3-6 | 3 |
| 7-8 | 4 |

Stage bands are soft penalties, not hard gates. When a random tier is above the natural tier, its effective tier weight is multiplied by the overflow multiplier:

| Overflow above natural tier | Tier weight multiplier |
|---:|---:|
| +1 tier | `50%` |
| +2 tiers | `15%` |
| +3 or more tiers | `5%` |

Stages `3-4` treat the first overflow tier as unpenalized, so Tier 4 appears often enough to be noticed without being guaranteed. With current Magic/Rare slot counts, this puts a Tier 4-or-better result on roughly one in four Stage 3-4 items. The field-tool tuning adds `+5` percentage points to the other overflow multipliers. Random rolls can reach tier 6 on non-Exotic targets and tier 7 on Stage 8 Exotic targets when the selected modifier family defines that tier. Tier 7 can appear from initial generation, Affix Forge add or fill operations, and full rerolls on Exotic targets; upgrades never create tier 7. Specialized modifier families may define fewer legal tiers than the random roll cap.

Rollable affixes store an `affix_id`, display key, slot, modifier group, family roll weight, tier range, optional tier weights, and one or more stat effects. Concrete modifier families have one legal slot. The modifier group is the compatibility family for duplicate prevention: initial rolls, Affix Injector additions, transmutation, rerolls, and Expansion Crystal slot fills cannot add a rolled affix whose `mod_group` already exists on the target. This lets related processing-speed outcomes such as generic processing speed, Crushing, Bulk Speed, and Overclocked all compete in the same `processing_speed` group even when their displayed names and effects differ.

Initial v1 family weights are static Java metadata:

| Family | Roll weight |
|---|---:|
| Ordinary rollable affix | `100` |
| Processing-specific speed names such as Crushing or Smelting | `75` |
| Instant Process and Super Output | `35` |
| Overclocked | `25` |
| Bulk Speed behavior | `25` |
| Battery Chassis Balance Mode | `45` |
| Battery Chassis Charged Storage | `35` |
| Battery Chassis Additional Battery Slots | `60` |
| Solar special utility families | `60` |
| Crusher-specific prefix families | `65` |
| Crush Head prefix families | `65` |
| Miner's Companion filter slots | `60` |

Old saved modifiers that only have stat, operation, value, and slot still decode. They receive a default group derived from operation and stat so legacy stat-only data remains valid and still participates in duplicate checks.

Current slot rule of thumb:

| Modifier family | Slot |
|---|---|
| Energy capacity | Prefix |
| Crusher-specific output guard, throughput, no-cell retention, input filtering, high-hardness mitigation, and salvage | Prefix on Crusher machines only |
| Crush Head Pulverizing, Jagged, Kinetic, Scuffed, and Dust-Grooved families | Prefix on Crush Head parts only |
| Miner's Companion filter slots | Prefix on Miner's Companion items only |
| Heat transfer | Prefix |
| Efficiency and fuel efficiency | Prefix |
| Processing speed | Suffix |
| Energy generation and energy usage | Suffix |
| Input, output, and addon slots | Suffix |
| Output amount | Suffix |
| Heat insulation | Implicit when authored onto a concrete item or part identity; not randomly rolled |
| Instant Process and Super Output chance | Suffix |
| Conditional behavior flags | Enchant, suffix, or fixed identity behavior depending on the behavior |

Rare machines can have at most three prefixes and three suffixes.

Implicit authored modifiers and behavior flags do not count against Magic or Rare prefix/suffix limits.

Profiles are validated against their declared capability flags. They are not padded with duplicate named variants, so a narrow profile may expose fewer than three legal prefixes or suffixes.

## Modifier Operations

Modifier operations are implemented in code.

| Operation | Meaning |
|---|---|
| `ADD` | Adds a fixed value to the authored base stat. |
| `INCREASED_PERCENT` | Adds percentage scaling after fixed additions. Multiple percentage modifiers stack additively with each other. |
| `DECREASED_PERCENT` | Subtracts percentage scaling from the same additive percentage layer as `INCREASED_PERCENT`. |
| `MORE` | Applies separate multiplicative scaling after fixed additions and increased-percent scaling. Multiple more modifiers multiply with each other. |
| `LESS` | Applies separate multiplicative scaling below `1.0` in the same multiplier layer as `MORE`. |

Final stat calculation:

```text
(authored base + fixed additions) * max(0, 1 + (increased percent - reduced percent) / 100) * each more factor * each less factor
```

Two stats bend the increased layer:

- `ENERGY_USAGE` divides by its reductions instead of subtracting them, so one large roll cannot empty the bucket and hit the usage floor: `(base + additions) * more and less factors * (1 + increased / 100) / (1 + reduced / 100)`.
- Crusher `OUTPUT_AMOUNT` bends its increased bucket with a soft cap; see [Crusher Yield](../reference/machine-stats.md#crusher-yield).

Status: Prototype

Player-facing wording uses **increased**, **reduced**, **more**, and **less** explicitly. The internal `DECREASED_PERCENT` operation is displayed as reduced. Mastery fixed values and ceilings resolve after ordinary modifiers; see [hard constraints](machine-mastery.md#modifier-keywords-and-hard-constraints).

## Modifier Tiers

Numeric modifiers have a tier, a value range, and one rolled value inside that range. Compound affixes have one rolled value per effect, using that effect's own tier table. A few named affixes are untiered; they roll a fixed value and do not show modifier-tier details.

The rolled value is what machine stat math uses. The tier and range remain attached to the modifier so tooltips, rerolling, and later crafting systems can explain where that value came from.

Each modifier family can define its own tier table and per-tier roll weights. If a family has no custom tier weights, each legal tier starts at weight `100`. Random tier selection uses:

```text
global tier weight * family tier weight * component-stage weight
```

Current global base tier weights before component-stage weighting are:

| Tier | Base weight |
|---:|---:|
| 1 | `450` |
| 2 | `270` |
| 3 | `150` |
| 4 | `80` |
| 5 | `35` |
| 6 | `12` |
| 7 | `3` |

The current percent-scaling table is:

| Tier | Range |
|---:|---:|
| 1 | `2-5%` increased or reduced |
| 2 | `4-10%` increased or reduced |
| 3 | `11-30%` increased or reduced |
| 4 | `40-55%` increased or reduced |
| 5 | `60-75%` increased or reduced |
| 6 | `80-100%` increased or reduced |
| 7 | `110-140%` increased or reduced |

Example: an energy-generation affix at tier 2 rolls one value from `4-10%`, such as `7% increased ENERGY_GENERATION`.

## Code-Backed Modifier Ranges

The code-backed source for modifier value ranges is `ModifierEligibilityProfiles`. Each `ModifierDefinition` owns its affix id, modifier group, slot, `CAN_ROLL` flag, family roll weight, optional tier weights, target effects, and tier ranges.

Most current rollable stat effects use the shared percent tier table above. Crusher yield affixes use smaller tables, because they share one soft-capped bucket. Crusher Jaws and Crush Head Pulverizing are compound prefixes: Output Amount on the yield table (`YIELD_TIER_RANGES`) plus reduced Processing Speed on a wider penalty table (`YIELD_SPEED_PENALTY_RANGES`). The Crusher and Crush Head Output Amount suffix uses the penalty-free clean table (`CLEAN_YIELD_TIER_RANGES`) in its own `clean_output_amount` group:

| Modifier definition | Slot | Operation | Tier table |
|---|---|---|---|
| `ENERGY_CAPACITY` | Prefix | `INCREASED_PERCENT` | Shared percent table |
| `EFFICIENCY` | Prefix | `INCREASED_PERCENT` | Shared percent table |
| `FUEL_EFFICIENCY` | Prefix | `INCREASED_PERCENT` | Shared percent table |
| `HEAT_TRANSFER` | Prefix | `INCREASED_PERCENT` | Shared percent table |
| `PROCESSING_SPEED` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `ENERGY_USAGE` | Suffix | `DECREASED_PERCENT` | Shared percent table |
| `OUTPUT_AMOUNT` | Suffix | `INCREASED_PERCENT` | Shared percent table; clean yield table on the Crusher and Crush Head |
| `INPUT_SLOTS` / fuel slots | Suffix | `ADD` | Flat slot table |
| Battery Chassis Charged Storage | Prefix | `MORE ENERGY_CAPACITY` | Untiered fixed `2.0x` multiplier after `10` minutes with stored energy |
| Battery Chassis Balance Mode | Prefix | Behavioral `CHARGE_BALANCER` | Single-tier charge-balancing behavior; fills and drains installed cells evenly |
| Battery Chassis Additional Battery Slots | Prefix | `ADD BATTERY_SLOTS` | Battery Chassis slot table |
| `ENERGY_GENERATION` flat | Prefix | `ADD` | Flat generation table |
| `ENERGY_GENERATION` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `ENERGY_TRANSFER` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `STABILITY` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `BURST_TRANSFER` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `BURST_DURATION` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `IDLE_LOSS` | Suffix | `DECREASED_PERCENT` | Shared percent table |
| `GLOBAL_MODIFIER_STRENGTH` | Suffix | `INCREASED_PERCENT` | Shared percent table |
| `DURABILITY` flat | Prefix | `ADD` | Durability table |
| Tool Self Repair | Prefix | `ADD SELF_REPAIR` | Tool self-repair table |
| Tool Attack Speed | Suffix | `ADD ATTACK_SPEED` | Tool attack-speed table |
| Crusher Frame | Prefix | `ADD OUTPUT_GUARD_GRACE` and enables `OUTPUT_GUARD` | `+100`, `+200`, `+400`, `+800` blocked-output grace ticks |
| Crusher Kinetics | Prefix | `ADD ENERGY_CAPACITY_FLAT` | Flat machine buffer table |
| Crusher Jaws | Prefix | `INCREASED_PERCENT OUTPUT_AMOUNT` plus `DECREASED_PERCENT PROCESSING_SPEED` | Yield table and speed-penalty table |
| Crusher Ore Handling | Prefix | `ADD SUPER_OUTPUT_CHANCE` | Chance table |
| Crusher Battery Link | Prefix | `ADD NO_BATTERY_OUTPUT_RETENTION` | `25%`, `50%`, `75%`, `100%` lost-multiplier retention |
| Crusher Feed Control | Prefix | `ADD CRUSHER_INPUT_FILTER` | Filter tiers `1-4` |
| Crusher Compression | Prefix | `ADD HIGH_HARDNESS_ENERGY_MITIGATION` | `15%`, `30%`, `45%`, `60%` surcharge mitigation |
| Crusher Vibration | Prefix | `DECREASED_PERCENT ENERGY_USAGE` | Shared percent table |
| Crusher Throughput | Prefix | `ADD BATCH_SIZE` | `+1`, `+2`, `+3`, `+5` items |
| Crusher Salvage | Prefix | `ADD CRUSHER_SALVAGE_CHANCE` | Chance table |
| Crush Head Pulverizing | Prefix | `INCREASED_PERCENT OUTPUT_AMOUNT` plus `DECREASED_PERCENT PROCESSING_SPEED` | Yield table and speed-penalty table |
| Crush Head Jagged | Prefix | `ADD CRUSHER_SALVAGE_CHANCE` | Chance table |
| Crush Head Kinetic | Prefix | `INCREASED_PERCENT PROCESSING_SPEED` | Shared percent table |
| Crush Head Scuffed | Prefix | `ADD NO_BATTERY_OUTPUT_RETENTION` | Untiered `+6%` lost-multiplier retention |
| Crush Head Dust Groove | Prefix | `ADD CRUSHER_INPUT_FILTER` | Untiered filter tier `1` |
| Miner's Companion filter slots | Prefix | `ADD BLOCK_FILTER_SLOTS` | `+1`, `+1`, `+2`, `+3`, `+4`, `+5`, `+7`, capped at ten active filters |

Flat energy-generation prefixes use a whole-number addition table:

| Tier | Added generation |
|---:|---:|
| 1 | `+5-10` |
| 2 | `+10-20` |
| 3 | `+25-40` |
| 4 | `+50-80` |
| 5 | `+100-150` |
| 6 | `+180-260` |
| 7 | `+320-480` |

Input-slot and fuel-slot affixes are flat additions instead of percentage scaling:

| Tier | Added slots |
|---:|---:|
| 1 | `+1` |
| 2 | `+1` |
| 3 | `+1` |
| 4 | `+2` |
| 5 | `+2` |
| 6 | `+2` |
| 7 | `+3` |

Battery Chassis slot prefixes use their own flat addition table:

| Tier | Added battery slots |
|---:|---:|
| 1 | `+1` |
| 2 | `+1` |
| 3 | `+2` |
| 4 | `+2` |
| 5 | `+2` |
| 6 | `+3` |
| 7 | `+4` |

Durability-capable profiles can roll either the shared percent prefix or a separate flat prefix. Current targets are Tool Heads, Tool Rods, and Cavitation Rotors:

| Tier | Added durability |
|---:|---:|
| 1 | `+32-64` |
| 2 | `+65-128` |
| 3 | `+129-256` |
| 4 | `+257-512` |
| 5 | `+513-768` |
| 6 | `+769-1024` |
| 7 | `+1025-1536` |

Tool Self Repair restores tool durability every `7.5` seconds while an assembled modular tool is idle in the player's hotbar or offhand. Active mining or tool-use pauses the repair. Tier 2 matches the `4` durability per interval baseline; tiers above it exceed that rate:

| Tier | Durability restored per interval |
|---:|---:|
| 1 | `3` |
| 2 | `4` |
| 3 | `6` |
| 4 | `8` |

Tool Attack Speed adds main-hand swing cadence to modular field tools. Tool Head family sets the base attack speed, Tool Rod material identity adds a swing adjustment, and this suffix adds a flat value:

| Tier | Added attack speed |
|---:|---:|
| 1 | `+0.05-0.10` |
| 2 | `+0.10-0.15` |
| 3 | `+0.15-0.25` |
| 4 | `+0.25-0.35` |
| 5 | `+0.35-0.45` |
| 6 | `+0.45-0.60` |
| 7 | `+0.65-0.80` |

Vein Miner is a rare Pick Head-only prefix. It rolls as one compound affix with `ADD VEIN_MINE_LIMIT` and `ADD VEIN_MINE_FE_USAGE`. The limit is the total connected same-type ore blocks including the targeted block, and the FE value is the extra surcharge for each connected block after the targeted block:

| Tier | Total blocks | Extra FE per extra block |
|---:|---:|---:|
| 1 | `3` | `24-32` |
| 2 | `5` | `20-24` |
| 3 | `8` | `16-20` |
| 4 | `12` | `12-16` |
| 5 | `18` | `8-12` |
| 6 | `26` | `6-8` |
| 7 | `40` | `4-6` |

Processing profiles also expose named processing-speed affixes whose display name is specific to the work being done, such as Crushing, Smelting, Reacting, Recycling, Pressing, Melting, Alloying, Calibrating, Serving, Screening, and Disassembling. These use the `PROCESSING_SPEED` stat, live in the shared `processing_speed` modifier group, and currently roll fixed values by tier:

| Tier | Processing-specific speed |
|---:|---:|
| 1 | `4%` |
| 2 | `6%` |
| 3 | `8%` |
| 4 | `10%` |
| 5 | `13%` |
| 6 | `16%` |
| 7 | `20%` |

Powered processing profiles can also roll `Overclocked`, a compound suffix in the `processing_speed` group:

| Effect | Tier 1 | Tier 2 | Tier 3 | Tier 4 | Tier 5 | Tier 6 | Tier 7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| `INCREASED_PERCENT PROCESSING_SPEED` | `8-10%` | `12-15%` | `18-22%` | `25-30%` | `35-40%` | `45-55%` | `65-80%` |
| `INCREASED_PERCENT ENERGY_USAGE` | `4-6%` | `7-10%` | `12-15%` | `16-20%` | `21-26%` | `28-35%` | `40-50%` |

Processing chance suffixes use additive percentage-point ranges:

| Modifier | Effect | Tier table |
|---|---|---|
| Instant Process | `ADD INSTANT_PROCESS_CHANCE` | `1-2%`, `2-3%`, `3-4%`, `4-5%`, `5-7%`, `7-9%`, `10-12%` |
| Super Output | `ADD SUPER_OUTPUT_CHANCE` | `1-2%`, `2-3%`, `3-4%`, `4-5%`, `5-7%`, `7-9%`, `10-12%` |

Crusher Salvage uses the same chance table but remains a separate Crusher-only prefix outcome. Deterministic `OUTPUT_AMOUNT`, Super Output, and Crusher Salvage are evaluated independently; Salvage can add one extra base-output item only when the output slot can accept it.

Crusher Feed Control tiers are discrete automation filters for top-side insertion: tier 1 requires a valid Crusher recipe, tier 2 also requires the current output slot to accept one candidate output, tier 3 also rejects candidates that would conflict with a banked fractional-output bonus from another input item, and tier 4 also checks output space for the dense batch that the input could join. Feed Control does not reject under-hardness recipes; those still run with the Crusher's time, FE, and jam-risk penalty rules.

## Solar Panel Modifiers

Solar Panel-only affixes use panel-specific stats. Peak Solar applies only during the zenith window, from Minecraft time `4000` through `8000` each day.

| Modifier | Slot | Operation | Tier table |
|---|---|---|---|
| Peak Solar | Suffix | `INCREASED_PERCENT PEAK_SOLAR_GENERATION` | `50-60%`, `61-75%`, `76-90%`, `91-100%` zenith output bonus |

## Solar Array Controller Modifiers

Solar Array Controller-only affixes use controller-specific stats instead of generic processing or fuel stats.

| Modifier | Slot | Operation | Tier table |
|---|---|---|---|
| Array Expansion | Suffix | `ADD SOLAR_PANEL_LIMIT` | `+1`, `+1`, `+2`, `+2` array range |
| Moonlit Conversion | Suffix | `ADD MOONLIGHT_CONVERSION` | `10-20%`, `20-30%`, `30-40%`, `50-55%` night output |
| Cloud Piercer | Suffix | `ADD WEATHER_RECOVERY` | `10-15%`, `16-25%`, `26-40%`, `45-60%` weather recovery |
| Lunar Inverter | Suffix | `ADD LUNAR_INVERSION` | `30-40%`, `41-55%`, `56-70%`, `71-85%` night output with a clear-day penalty |
| Clear-Sky Amplifier | Suffix | `ADD CLEAR_SKY_AMPLIFICATION` | `5-10%`, `11-20%`, `21-35%`, `40-50%` clear-day bonus |
| Panel Synchronizer | Prefix | `ADD SOLAR_PANEL_SYNCHRONIZATION` | `5-10%`, `11-20%`, `21-35%`, `40-50%` same-panel-material bonus |
| Panel Arbitration | Prefix | `ADD SOLAR_PANEL_ARBITRATION` | Enables highest-output panel selection |

Flat energy capacity uses its own `ADD ENERGY_CAPACITY_FLAT` tables. These flat values are added before `ENERGY_CAPACITY` percent and more multipliers. Ordinary machine buffer capacity keeps a conservative machine table:

| Tier | Range |
|---:|---:|
| 1 | `1,000-3,000 FE` |
| 2 | `3,000-8,000 FE` |
| 3 | `9,000-20,000 FE` |
| 4 | `30,000-50,000 FE` |
| 5 | `75,000-100,000 FE` |
| 6 | `150,000-250,000 FE` |
| 7 | `350,000-500,000 FE` |

Battery Cells use a separate high-capacity item table:

| Tier | Range |
|---:|---:|
| 1 | `1,000-3,000 FE` |
| 2 | `4,000-12,000 FE` |
| 3 | `16,000-50,000 FE` |
| 4 | `75,000-100,000 FE` |
| 5 | `150,000-250,000 FE` |
| 6 | `350,000-600,000 FE` |
| 7 | `800,000-1,000,000 FE` |

The cell item table is intentionally larger at high tiers so rare rolls can matter against GTNH-style cell capacities. It applies to [Battery Cells](https://c-pettersson.github.io/rngtech/battery-cells/), not to Battery Chassis capacity prefixes. Battery Chassis capacity prefixes scale installed cells only while those cells remain inside the chassis.

Fixed authored modifiers do not use tier ranges unless a specific item stores one. Current fixed component and Battery Cell identity modifiers are `CAN_ROLL = No`, stored as implicit modifiers, and use exact values from the material or identity. Current machine and chassis numeric identity uses authored base stat profiles before modifiers are applied. Implicit modifiers are authored identity modifiers, not random affixes.

`OUTPUT_AMOUNT` remains deterministic: fractional output is banked by machines that support deterministic output scaling. `SUPER_OUTPUT_CHANCE` is a separate chance outcome and only adds one extra copy of the recipe's base stackable item output. It does not multiply deterministic `OUTPUT_AMOUNT` bonuses.

`INSTANT_PROCESS_CHANCE` is checked when eligible processing starts. Powered machines only complete instantly when they can pay the full adjusted craft FE up front; otherwise the process continues through normal progress. Fuel-driven furnace processing spends normal fuel work when the instant outcome succeeds.

Status: Prototype

## Eligibility

Not every modifier can apply to every category of content.

Use [Modifier Eligibility](../reference/modifier-eligibility.md) as the cross-reference table for capability and profile eligibility, and use one page per concrete modifier once modifier names are finalized. The full inventory starts at [Modifiers](../modifiers/index.md).

## Behavioral Modifiers

Some planned modifiers affect machine behavior directly instead of changing a numeric stat.

Behavioral modifiers are presence/absence effects by default. If the modifier is present, the behavior is enabled; if it is absent, the behavior is disabled. They do not use a numeric tier range unless a specific behavioral modifier later needs scaled behavior.

The current code-backed rollable behaviors are [Bulk Speed](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#bulk-speed) and [Balance Mode](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#balance-mode). Bulk Speed competes with suffix rolls on eligible processing-machine profiles and is stored as a behavior flag in `MachineTraits`. Balance Mode is a Battery Chassis prefix affix that enables `CHARGE_BALANCER` behavior and occupies a normal prefix slot.

Behavioral modifiers should still have a dedicated modifier page that documents their trigger, eligibility, and interaction rules.
