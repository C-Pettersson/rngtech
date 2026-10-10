# Machine Stats

Player guide: [Machine Stats](https://c-pettersson.github.io/rngtech/machine-stats/)

Machine stats are the canonical values modified by RNGTech progression systems.

Effective stats are built from machine-family defaults plus an authored machine or chassis base stat profile. Installable components and Battery Cells also have authored base profiles: a component's stored rolled affixes are evaluated against that component first, then the effective component contribution is merged into the host machine. Host rolled affixes, refinement changes, and runtime effects then scale or add to the assembled machine. Numeric rolled affixes carry a tier and value range, but base stat profiles do not have modifier tiers and are not removable modifiers.

Slot counts, processing level, batch size, buffer size, and upgrade limit roll as whole numbers. Multiplier-style stats roll as decimal values.

Modifier operations (`ADD`, `INCREASED_PERCENT`, `DECREASED_PERCENT`, `MORE`, `LESS`) and the final stat formula are defined in [Affix Generation](../systems/affix-generation.md#modifier-operations).

| Stat | Status | Meaning |
|---|---|---|
| `INPUT_SLOTS` | Prototype | Number of input slots available to a machine. For Furnace, this controls active processing lanes; Lead Furnace currently uses `4`. |
| `OUTPUT_SLOTS` | Planned | Number of output slots available to a machine. |
| `ADDON_SLOTS` | Planned | Number of addon or machine-part slots available. |
| `OUTPUT_AMOUNT` | Prototype | Deterministic amount produced by processing. Machines that support fractional deterministic item output keep bonus progress in a bank and pay extra items when the bank reaches a whole output. On the Crusher it is one soft-capped yield bucket paid for in cycle time; see [Crusher Yield](#crusher-yield). Cavitation Generator uses it directly as a multiplier for nitrogen mB produced per recipe cycle. |
| `SUPER_OUTPUT_CHANCE` | Prototype | Percentage chance for item-output processing to add one extra copy of the base recipe output when that output is stackable. This is separate from `OUTPUT_AMOUNT` and does not multiply deterministic output bonuses. |
| `OUTPUT_GUARD_GRACE` | Prototype | Crusher-only blocked-output progress grace in ticks. Crusher Frame prefixes add this value and enable Output Guard behavior. |
| `NO_BATTERY_OUTPUT_RETENTION` | Prototype | Crusher-only percentage of the missing-Battery-Cell output penalty that is retained instead of lost. |
| `HIGH_HARDNESS_ENERGY_MITIGATION` | Prototype | Crusher-only percentage reduction to the surcharge above `1.0x` from the high-hardness energy multiplier. It does not reduce the base recipe energy cost. |
| `CRUSHER_INPUT_FILTER` | Prototype | Crusher-only top automation filter tier. Top automation always requires a valid recipe; higher tiers add output acceptance, output-bonus-bank compatibility, and dense-batch output acceptance. It does not reject under-hardness Crusher recipes. |
| `CRUSHER_SALVAGE_CHANCE` | Prototype | Crusher-only percentage chance to add one extra copy of the base output item after deterministic output amount and Super Output have been evaluated. |
| `BLOCK_FILTER_SLOTS` | Prototype | Portable Miner's Companion filter capacity. The base profile starts at `3`; Miner's Companion filter-slot prefixes can raise it to the ten-slot GUI cap. |
| `MAGIC_FIND` | Deferred | Reserved for later special outcome systems; not shown as a core machine stat. |
| `PROCESSING_SPEED` | Prototype | Processing rate. Recipe machines use it for cycle time; the Forestry Companion uses it for placed cart work intervals. |
| `INSTANT_PROCESS_CHANCE` | Prototype | Percentage chance for processing to complete instantly at process start. Powered machines must have enough FE to pay the full adjusted craft cost up front; otherwise the craft proceeds normally. |
| `PROCESSING_LEVEL` | Prototype | Recipe tier or material hardness pressure. On Crushers, this is the Crush Head hardness level compared to `required_processing_level` on crusher recipes; lower-than-required heads still process but take longer, cost more total FE, and can jam. On Potential Reactors, this is the Reactor Chamber level checked by `minimum_material_stage` on reactor recipes. Component rolls and normal refinement do not raise hard recipe gates where a machine family uses one. |
| `ENERGY_USAGE` | Prototype | Multiplier for FE consumed by powered processing recipes or placed Forestry Companion cart actions. Lower is better. Reductions divide rather than subtract: `base * more and less factors * (1 + increased / 100) / (1 + reduced / 100)`, so one large reduction roll halves usage instead of reaching the floor. |
| `ENERGY_CAPACITY` | Prototype | Percent or multiplicative stored energy capacity scaling. Processing machines use the final capacity value for small internal buffers, while meaningful portable storage should come from inserted [Battery Cells](https://c-pettersson.github.io/rngtech/battery-cells/). Battery Chassis capacity prefixes scale installed-cell capacity while the cells are inside the chassis. |
| `ENERGY_CAPACITY_FLAT` | Prototype | Flat FE added to stored energy capacity before `ENERGY_CAPACITY` percent and more multipliers. Machine buffer and Battery Cell flat-capacity prefixes roll this stat, then the accumulator folds it into the final `ENERGY_CAPACITY` value used by machines. |
| `ENERGY_GENERATION` | Prototype | Energy generated by a machine. Solid fuel-burning generators and Solar Panels use this as FE/t. Solar Array Controllers use percent and component multiplier contributions over selected panel output, while rollable flat prefixes add FE/t after panel output exists. Bio Generators use this as FE/t power, with burn length handled separately by fuel value and `FUEL_DURATION`. Potential Reactors, Syngas Combustors, and other recipe-total generators use percent and component multiplier contributions on total generated FE; rollable flat prefixes add FE/t across the active burn or processing duration instead of multiplying the full recipe energy total. Generator profiles can roll both flat additive prefixes and percent suffixes for this stat. Modifiers rolled on an installed part are local: they scale only that part's own generation multiplier and flat FE/t, once. A part's generation multiplier scales the machine's base or recipe generation, not flat FE/t from that part or any other part. The machine's own modifiers, Mastery passives, and modifiers explicitly labeled Global scale the machine's whole output, flat FE/t included. On generators where the stat is a multiplier (recipe-total generators, the Bio Generator, and the Solar Array Controller), the Stats tab row shows the final FE/t for the active or next recipe, the same value as the Process tab. Its hover breaks that into base FE/t, the generation percent, flat FE/t from rolls, and a combined factor for speed, efficiency, stability, and other machine effects. The Solar Array Controller has no fixed base, so its hover omits the base and combined lines. Solid Fuel Burners show their Heat Core FE/t directly. |
| `PEAK_SOLAR_GENERATION` | Prototype | Solar Panel-only multiplier that applies during the zenith window, from Minecraft time `4000` through `8000` each day. The Peak Solar suffix raises this multiplier by `50-100%`, affecting standalone panels and panels aggregated by Solar Array Controllers. |
| `ENERGY_TRANSFER` | Prototype | Storage and connector transfer rate, not a general powered-machine intake cap. Processing machines accept external FE up to free buffer space and the source/connector limit; their user-facing power number is recipe demand. Generators have no machine-side export cap and do not roll or show this stat; the attached Universal Connector's Energy Connector tier limits export. For Battery Cells this is the cell's base input rate before cell rolls, and output scales from the material output rate by the same source-local transfer multiplier. For Battery Chassis blocks, this is a displayed chassis transfer rating; current actual input and output are governed by inserted cell rates, free cell space, stored FE, and connector/source/sink limits. Generator-installed Battery Cells are capacity buffers only and do not cap generator FE/t or export. Every remaining cap is a per-tick budget shared by the machine's own push and connector pulls. |
| `FLUID_CAPACITY` | Prototype | Authored/display total ordinary-fluid capacity. Plain Fluid Tanks report loose capacity; Steel+ Compressor Tanks report loose capacity plus compressed effective capacity. The Melter's tanks start at `4,000 mB`, and its Pressure Vessel ascendancy raises them. The Forestry Companion's sprinkler water tank starts at `8,000 mB`. The shared tree's Reservoirs pockets raise both, and Fluid Pumps can roll a flat (Brimming) or percent (Depth) Fluid Capacity affix that applies to the machine they are installed in. The Corrosion Cell's electrolyte tank starts at `4,000 mB` and reads it too. It is not rollable in v1. |
| `COMPRESSION_RATIO` | Prototype | Authored/display physical compression ratio for Steel+ Compressor Tanks. The compressed store keeps decompressed-equivalent mB, while the UI displays physical volume as `equivalent / ratio`. It is not rollable in v1. |
| `FLUID_TRANSFER` | Prototype | Fluid movement rate in mB/t. Component Assembler uses this for Electrolyte Solution and Lubricant input. Corrosion Cell uses this on installed Fluid Pumps to cap side Electrolyte Solution filling. The Melter uses this on installed Fluid Pumps to cap side fluid extraction and output container filling. Cavitation Generator uses it to cap nitrogen output draining. Steel+ Compressor Tanks use it as the base compression/decompression rate before Servo count and Servo quality multipliers. |
| `CALIBRATION_QUALITY` | Prototype | Resonance Calibrator quality control. Higher values raise stability floor or ceiling for calibrated component outputs. |
| `CALIBRATION_PRECISION` | Prototype | Resonance Calibrator variance control. Higher values narrow the stability spread for calibrated outputs. |
| `CATALYST_EFFICIENCY` | Prototype | Resonance Calibrator catalyst preservation and stabilizer efficiency. Higher values can preserve catalysts during calibration. |
| `REFINEMENT_POTENTIAL_BONUS` | Prototype | Flat bonus to Refinement Potential on calibrated outputs when the recipe can produce RP. |
| `BURST_TRANSFER` | Prototype | Surge identity value for burst-oriented energy storage. Current Battery Chassis transfer remains cell-led; detailed battery chassis behavior is documented in [Battery Chassis](https://c-pettersson.github.io/rngtech/battery-chassis/). |
| `BURST_DURATION` | Prototype | Number of ticks a chassis can sustain future burst-risk behavior. |
| `EFFICIENCY` | Prototype | Non-recipe efficiency. For generators and Potential Reactors, it increases recovered or generated FE. For fuel machines, it can extend fuel duration or output. For battery storage, it represents charge or discharge loss behavior. It does not reduce powered processing recipe FE cost. |
| `IDLE_LOSS` | Prototype | Stored energy lost while the block is idle. Lower is better. |
| `BATCH_SIZE` | Prototype | The most items a machine takes from one input stack and works together in one cycle. Base `1`, clamped to `1-16`. A cycle locks its batch when it starts: the largest batch the input, fluid, and output room allow. Every item in the batch pays its own FE and grants its own XP. Tungstensteel and Exotic Crusher chassis author `4` and `9`; Lead and Tungstensteel Resonance Calibrator chassis author `2` and `3`; Crusher Throughput prefixes, Dense Batching, and the Drop Forge, Mass Tuner, and Twin Crucible ascendancies add more. Metal Press circuit recipes never batch. Refiner’s Oath and Fused Crucibles turn batching off for a payoff that scales with Batch Size. Stored and authored `PARALLEL_JOBS` values load as `BATCH_SIZE`. Furnace lanes are separate and come from `INPUT_SLOTS`. |
| `BATCH_OVERHEAD` | Prototype | Extra cycle time for each item past the first in a batch, as a percentage of the single-item time. Base `25`, never below `5`. A batch of `n` takes `1 + overhead × (n − 1)` times as long, so throughput approaches `1 / overhead`. |
| `BUFFER_SIZE` | Planned | Internal buffer capacity for inputs, outputs, fluids, heat, or energy. |
| `STABILITY` | Prototype | Reliability stat used for failures, quality variance, overclocking risk, battery chassis overload risk, or tool cost avoidance. Modular Field Tools use Stability above `1.0` as a per-block chance to skip both durability and FE cost; `1.10` is a `10%` skip chance. Potential Reactors currently use stability as a deterministic recovered-FE multiplier until jam or contamination behavior exists. Metal Press uses stability to reduce forming failure. |
| `UPGRADE_LIMIT` | Planned | Maximum number or total weight of upgrades the machine can accept. |
| `BATTERY_SLOTS` | Prototype | Number of battery cell slots available in a battery chassis. Material identity sets the base count, and Battery Chassis slot prefixes can activate additional cell slots. |
| `GLOBAL_MODIFIER_STRENGTH` | Prototype | Scalar for global chassis modifier effects applied to inserted cells. Current code exposes the stat for chassis traits and UI; broad cell-global behavior remains future work. |
| `MAX_TEMPERATURE` | Prototype | Highest effective operating temperature. Furnace, Alloy Furnace, Metal Press, and Melter compare this against recipe temperature gates. Electric Furnace and Alloy Furnace chassis gain additional effective temperature from installed Heat Cores. Metal Press requires an installed Heat Core, then combines the forming bed baseline with the Heat Core contribution. |
| `HEAT_TRANSFER` | Prototype | How quickly the machine applies or receives heat. It affects Furnace, Alloy Furnace, and Metal Press warmup speed and still contributes to heat-processing time scaling. |
| `HEAT_ISOLATION` | Prototype | How well the machine retains heat. Furnace, Alloy Furnace, and Metal Press cooling use it to slow heat loss while blocked, out of fuel, or out of FE. |
| `TEMPERATURE_STABILITY` | Prototype | Precision of temperature control. Normal Furnace and Alloy Furnace recipes can require a minimum value. Failure-bearing Stage 4+ heat recipes use low stability as failure strain instead of a hard block after work starts. Metal Press also compares it against recipe requirements while accumulating forming failure risk when it is too low. |
| `FUEL_EFFICIENCY` | Prototype | Fuel-specific efficiency for burnable or heat-producing recipes. Bio Generators apply it to total fuel FE. |
| `POTATO_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/potato_fuels`. |
| `CARROT_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/carrot_fuels`. |
| `BREAD_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/bread_fuels`. |
| `SAPLING_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/sapling_biomass`. |
| `SEED_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/seed_biomass`. |
| `PLANT_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/plant_biomass` that are not matched by a more specific plant family. |
| `ORGANIC_REAGENT_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/organic_reagents`. |
| `COMPOSTED_BIOMASS_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/composted_biomass`. |
| `ALGAE_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/algae_biomass` or `rngtech:bio_generator/dense_algae_biomass`. |
| `RICH_BIOMASS_POWER` | Prototype | Ingredient-aware Bio Generator multiplier for fuels tagged `rngtech:bio_generator/rich_biomass`. |
| `FUEL_DURATION` | Prototype | Generator burn-time multiplier for Bio Generators and solid fuel-burning generators. It increases the number of burn ticks without increasing FE/t. |
| `WARMUP_TIME` | Prototype | Time needed before the machine reaches target temperature. Lower is better. Furnace, Alloy Furnace, and Metal Press warmup rate is `max(1, round(10 * HEAT_TRANSFER / max(0.25, WARMUP_TIME)))` C/t. |
| `COOLING_RATE` | Prototype | How quickly stored heat is lost. Lower is better for retained heat. Furnace, Alloy Furnace, and Metal Press cooling is `max(1, round(2 * COOLING_RATE / max(0.25, HEAT_ISOLATION)))` C/t. |
| `OVERHEAT_TOLERANCE` | Prototype | How far the machine can exceed safe heat before penalties apply. Furnace, Alloy Furnace, and Metal Press use it to widen a recipe's safe maximum temperature before overheat failure risk accumulates. |
| `REFINEMENT_POTENTIAL` | Prototype | Remaining crafting or rerolling budget. |
| `MINING_SPEED` | Prototype | Modular Field Tool block-breaking speed. Head and rod base profiles and stored part affixes combine directly; assembled tools carry no own affixes. |
| `ATTACK_SPEED` | Prototype | Modular Field Tool main-hand swing cadence in attacks per second. Tool Head family provides the base value, Tool Rods add a material swing adjustment, and stored part affixes can add flat attack speed. |
| `MINING_LEVEL` | Prototype | Modular Field Tool harvest tier. It is a hard gate from Tool Head identity plus stored head upgrades such as Diamond Tip; normal part affixes do not raise it. |
| `DURABILITY` | Prototype | Modular Field Tool max durability and Cavitation Rotor wear limit. Area and tree actions consume one durability per block actually broken; Cavitation recipes add rotor wear to the installed rotor stack. |
| `SELF_REPAIR` | Prototype | Modular Field Tool passive repair amount restored every `7.5` seconds while an assembled tool is idle in the player's hotbar or offhand. Active mining or tool-use pauses the repair. Head and rod values add together on the assembled tool. |
| `FE_USAGE` | Prototype | FE consumed per successful modular-tool block break before durability is applied. Lower is better. Tools can fall back to durability-only work when FE protection is unavailable. |
| `FE_TRANSFER` | Prototype | FE transfer ceiling for an installed modular-tool Battery Cell and compatible inventory Battery Cell transfer. |
| `BATTERY_SUPPORT` | Prototype | Highest Battery Cell stage a modular-tool rod can accept. It gates installed cells and inventory charge sources only. |
| `AREA_WIDTH` | Prototype | Hammer area width. Current authored heads default to `3`; normal first-pass profiles do not broadly expand it. |
| `AREA_HEIGHT` | Prototype | Hammer area height. Current authored heads default to `3`; normal first-pass profiles do not broadly expand it. |
| `TREE_FELL_LIMIT` | Prototype | Maximum connected log blocks a Treefeller action can break, and the Forestry Companion's Treefeller batch size. It does not limit which trees the Forestry Companion can harvest. Durability and FE are still charged per block for direct tool use; Forestry Companion Treefeller batches pay the configured batch FE. |
| `VEIN_MINE_LIMIT` | Prototype | Maximum connected same-type ore blocks a Vein Miner Pick action can break, including the targeted block. Pick Heads can roll this as part of the rare Vein Miner prefix. |
| `VEIN_MINE_FE_USAGE` | Prototype | Extra FE required for each connected ore block broken by Vein Miner after the targeted block. Lower values are better. Normal per-block durability and FE costs still apply. |
| `ORE_BURST_SPEED` | Prototype | Ore-only mining speed bonus for Ore Burst-capable Pick and Hammer tools. |
| `ORE_BURST_DURATION` | Reserved | Reserved for future timed burst windows; the current implementation applies the speed check directly to valid ore blocks. |
| `ORE_BURST_FE_USAGE` | Prototype | Extra FE charged per successfully broken ore block while Ore Burst applies. |
| `ORE_BURST_COOLDOWN` | Reserved | Reserved for future burst pacing. |
| `LUCK` | Prototype | Tool-local extra drop chance for eligible modular-tool block drops. It does not affect machines, recipes, chests, mobs, or arbitrary loot. |
| `CONTROL` | Prototype | Machine precision attribute; conversions and explicit scaling are defined in [Machine Mastery](../systems/machine-mastery.md#attributes). Modular Tool Rod Control retains its separate deterministic FE-assisted durability protection above 1.0, after Stability, capped at 100%. |
| `DRIVE` | Prototype | Machine actuation attribute; see [Mastery conversions](../systems/machine-mastery.md#attributes). |
| `RESERVE` | Prototype | Machine capacity attribute; see [Mastery conversions](../systems/machine-mastery.md#attributes). |

## Ascendancy Stats

[Ascendancies](../systems/machine-mastery.md#ascendancies) introduce these stats. Each reaches only the families listed, and appears on a machine's Stats tab only when granted. A [Unique](../prds/uniques.md) can also grant some of them: as an ascendancy hook that applies only with that ascendancy, or as a signature such as the Crying Crucible's Blend Speed. Percent values are percentage points added to the stat. Yield stats are covered by the recipe loop audit.

| Stat | Status | Meaning |
|---|---|---|
| `HARDNESS_TOLERANCE` | Prototype | Crusher. Missing Crush Head levels that add no time, FE, or jam risk. The machine still counts as under level, so bonus output stays off. |
| `JAM_CHANCE` | Prototype | Crusher. Multiplier on the configured under-level jam chance and on `CYCLE_JAM_CHANCE`; every Crusher starts at `1`. Lower is better. |
| `JAM_RECOVERY` | Prototype | Crusher. Percent faster jam clearing. |
| `UNDER_LEVEL_EFFICIENCY` | Prototype | Crusher. Percent reduction to the time and FE each missing hardness level adds, up to `90%`. |
| `BANK_MEMORY` | Prototype | Crusher. Number of inputs whose Output Amount bonus banks are remembered when the input changes. Without it, a Crusher keeps one bank. Yield stat. |
| `AT_LEVEL_OUTPUT` | Prototype | Crusher. Percent Output Amount on recipes exactly at the Crush Head's hardness. Yield stat. |
| `SUPER_OUTPUT_CADENCE` | Prototype | Crusher and Alloy Furnace. Guarantees Super Output every this many eligible cycles of the same input; `0` is off. Mother Lode fixes it at `16` and Unbroken Pour at `32`. Yield stat. |
| `OVERDRIVE_SPEED` | Prototype | Furnace. Percent Processing Speed for each `10 °C` a lane runs above the recipe target. |
| `OVERDRIVE_CAP` | Prototype | Furnace. Most Processing Speed Overdrive can add, in percent. |
| `OVERDRIVE_MARGIN` | Prototype | Furnace. How far below the recipe's safe maximum, in °C, Overdrive stops heating. |
| `STRAIN_RECOVERY` | Prototype | Furnace. Extra failure strain drained per tick while a lane is inside its safe band. |
| `LEDGER_RATE` | Prototype | Furnace, Alloy Furnace, Metal Press, and Forestry Companion. Percent of the base output banked per eligible cycle in an output ledger, paid out as whole items when the output has room. It drives the Bloom Ledger, the blend ledger, the Batch Ledger, and the Log Ledger. Yield stat. |
| `FLUX_RATE` | Prototype | Alloy Furnace. Percent of one unit of the largest input banked per eligible alloy craft; a full unit is skipped on a later craft. Only an ingredient that needs two or more units is saved, so no input reaches zero. Yield stat. |
| `BLEND_SPEED` | Prototype | Alloy Furnace. Percent Processing Speed on blend recipes. |
| `BLEND_HEAT_REDUCTION` | Prototype | Alloy Furnace. Degrees Celsius removed from the minimum, target, and safe maximum of blend recipes. |
| `MOLD_SWAP_TIME` | Prototype | Metal Press. Ticks the Mold Rack takes to switch to an installed mold that fits the input. Lower is better. |
| `HEAT_WINDOW` | Prototype | Metal Press. Percent change to the distance from each recipe's target to its window edges; negative values narrow the window. |
| `STREAK_FLOOR` | Prototype | Resonance Calibrator. Stability floor gained per consecutive calibration of one family on one pattern. |
| `STREAK_CAP` | Prototype | Resonance Calibrator. Largest stability floor bonus the streak can reach. |
| `COIL_REACH` | Prototype | Resonance Calibrator. Stages above the installed Resonance Coil's stage that a recipe may require. |
| `FLUID_YIELD` | Prototype | Melter. Percent more fluid on eligible melts, rounded down per melt. Yield stat. |
| `OVERLEVEL_SPEED` | Prototype | Melter. Percent Processing Speed for each Crush Head level above a recipe's requirement. |
| `CART_SPEED` | Prototype | Forestry Companion. Multiplier on powered and station-seeking cart speed, capped at `0.34` blocks per tick. Starts at `1`, which moves a powered cart `0.055` blocks per tick. The shared tree's Rail Pace pocket and Driven Wheels, which scales it from Drive, raise it. |
| `GROWTH_PULSE` | Prototype | Forestry Companion. Bone meal applications per growth pulse; the fraction is a chance for one more. Each pulse spends one bone meal from the cart's fertilizer store. Yield stat. |
| `WORK_RANGE` | Prototype | Forestry Companion. Rows the cart works on each side of the rail, read as whole rows from `1` to `7`. Starts at `1`. Scan FE scales with it, and Verdant Surge and Rolling Harvest reach one row further. Granted by Grove Warden's Nursery and the shared tree's Far Rows and Outer Rows. |
| `IDLE_CART_SPEED` | Prototype | Forestry Companion. Percent increased Cart Speed after `80` ticks (4 seconds) without a plant, cut, or crop harvest. Stacks with other increased Cart Speed. Granted by the shared tree's Open Track pocket. |

## Unique Stats

Only [Uniques](../prds/uniques.md) grant these. A machine's Stats tab lists them while it has them.

| Stat | Status | Meaning |
|---|---|---|
| `CYCLE_JAM_CHANCE` | Prototype | Crusher. Percent jam chance on every cycle, at, above, or under level, added to the under-level chance before `JAM_CHANCE` scales it. A jam it causes on an at-level recipe lasts as long as a one-level jam. Lower is better. |
| `ESCAPEMENT_SPEED` | Prototype | Metal Press, Alloy Furnace, and Melter with the `ESCAPEMENT` behavior. Percent faster first cycle after the machine idles or its recipe or mold changes. FE per craft is unchanged. |

## Behavior Flags

Behavior flags enable runtime paths such as burst output, charge balancing, fuel reserve, side fluid output, alloy-only speed, or sustained-processing ramps.

Most current behavior flags are fixed identity traits. Uniques add `ESCAPEMENT` (first-cycle speed), `REFLUX` (half Sprinkler water), and `ECHO_STREAK` (the calibration streak survives one off-family calibration). They are shown separately from rolled numeric modifiers and are re-derived from the concrete item or block so refinement cannot remove them. [Bulk Speed](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#bulk-speed) is code-backed rollable behavior on eligible processing-machine profiles and applies an implicit `PROCESSING_SPEED` bonus based on completed processes. [Balance Mode](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#balance-mode) is a Battery Chassis prefix affix that enables `CHARGE_BALANCER` without changing a numeric stat.

## Base Machine Range

| Stat | Base Range | Notes |
|---|---:|---|
| `INPUT_SLOTS` | `1-2` | Recipe shape and automation flexibility. For Fuel Boxes, fuel slot count is component base identity and normal component affixes do not raise it. |
| `OUTPUT_SLOTS` | `1-2` | Allows recipes with multiple outputs. |
| `ADDON_SLOTS` | `0-2` | Controls customization capacity. |
| `PROCESSING_SPEED` | `0.8-1.2` | Multiplier against recipe time. |
| `PROCESSING_LEVEL` | `1` | Generic recipe gating starts at level 1. Crushers override their machine base to `0`; installed Crush Heads provide the active hardness level. |
| `ENERGY_USAGE` | `0.9-1.1` | Multiplier against energy cost. Lower is better. |
| `BLOCK_FILTER_SLOTS` | `3` | Miner's Companion-only active ghost filter slots before filter-slot prefixes. |
| `ENERGY_CAPACITY` | `0.8-1.2` | Percent/multiplier scaling against configured storage. |
| `ENERGY_TRANSFER` | `1` | Neutral transfer-rate multiplier for energy storage items. |
| `EFFICIENCY` | `0.9-1.1` | Non-recipe efficiency for machine families that explicitly use it. Higher is better where applicable. |
| `BATCH_SIZE` | `1` | Chassis, prefixes, Mastery, and ascendancies raise it. |
| `BATCH_OVERHEAD` | `25` | Percentage of the single-item cycle time added per extra batched item. |
| `BUFFER_SIZE` | `1` | Neutral base multiplier for future buffers. |
| `STABILITY` | `0.9-1.1` | Neutral for most generic machines until failure or quality systems exist. Modular Field Tools use values above `1.0` as free durability and FE cost avoidance before Control can spend FE for durability protection. |
| `UPGRADE_LIMIT` | `1-3` | Upgrade capacity budget. |
| `CONTROL` | `0+` | Core precision and regulation points. Machine families translate these into relevant effective stats. |
| `DRIVE` | `0+` | Core motor, force, and actuation points. Machine families translate these into work-rate or workload effects. |
| `RESERVE` | `0+` | Core buffer, storage, and endurance points. Machine families translate these into capacity or interruption-tolerance effects. |

## Crusher-Specific Range

Crusher-specific prefix stats default to `0` and become active only through rolled Crusher prefix families.

| Stat | Range | Notes |
|---|---:|---|
| `OUTPUT_GUARD_GRACE` | `100-800 ticks` | Crusher Frame tiers preserve output-blocked progress for `100`, `200`, `400`, or `800` ticks. |
| `NO_BATTERY_OUTPUT_RETENTION` | `25-100%` | Crusher Battery Link tiers retain `25%`, `50%`, `75%`, or `100%` of the no-cell lost multiplier. |
| `HIGH_HARDNESS_ENERGY_MITIGATION` | `15-60%` | Crusher Compression tiers reduce only the high-hardness surcharge by `15%`, `30%`, `45%`, or `60%`. |
| `CRUSHER_INPUT_FILTER` | `2-4` | Crusher Feed Control tiers add filter `+2`, `+3`, or `+4`: output acceptance, then bonus-bank compatibility, then dense-batch output acceptance. Filter `1` matches the default recipe check, so no roll grants it. |
| `CRUSHER_SALVAGE_CHANCE` | `1-5%` | Crusher Salvage uses the shared chance table: `1-2%`, `2-3%`, `3-4%`, `4-5%`. |

## Crusher Yield

Crusher `OUTPUT_AMOUNT` works differently from the general formula:

- One bucket. Every Crusher yield source adds percent to the increased bucket: Crush Head base yield and its rolls (the head merges them as increased percent, not as a local multiplier), Crusher affixes, Mastery keystones and nodes, ascendancy nodes, and At-Level Output on recipes at the head's hardness.
- Soft cap. With `B` the summed increased percent as a fraction, the paid bonus is `B * K / (B + K)` with `K = 1.0`, so it never reaches `+100%`. Output Amount is then the chassis base times `(1 + paid bonus)` times any less penalty, such as the no-Battery-Cell multiplier. The Stats tab, its hover, and recipe previews show this effective value.
- Speed trade. Crusher Jaws and Crush Head Pulverizing pair their Output Amount with reduced `PROCESSING_SPEED` on the same affix. The penalty is an ordinary speed effect, so it slows every recipe. The Output Amount suffix has no penalty and much lower rolls.
- Once per ore. Only ore and raw-metal crushing is bonus-eligible; the crushed-to-dust step sets `bonus_output: false`.
- `SUPER_OUTPUT_CHANCE` stays a separate chance stat with a Crusher ceiling of `25%`.

## Furnace Range

Furnaces use authored base processing stats and heat stats. Stage 0 uses fuel stats and has an authored `800` maximum temperature for early Iron and Copper smelting; electric Furnace chassis additionally use FE storage, energy usage, and optional Heat Core Gear. External FE intake is limited by free buffer space and the source/connector, while the UI reports per-lane recipe demand. `rngtech:furnace` recipes can require target heat and temperature stability, and Stage 4+ recipes with `failure_output` can convert low stability, underheat, overheat, or sensitive post-start FE drops into failure strain. Heat Core base profiles and percentage or flat Heat Core affixes add high-heat reach; Furnace and Electric Furnace chassis can also roll heat-control affixes. Lead Furnace is the current multi-lane case: it has four active input/output lane pairs, four lane-local Heat Core Gear slots, per-lane FE/t tooltips, and a `0.5x` base processing-speed value. Bronze Furnace carries `ALLOY_BLEND`, which adds conditional speed only for inputs tagged `rngtech:alloy_blend_smeltables`; default data tags Bronze Blend and Steel Blend.

| Stat | Base Range | Notes |
|---|---:|---|
| `HEAT_TRANSFER` | `0.8-1.2` | Faster warmup and heat application. |
| `HEAT_ISOLATION` | `0.8-1.2` | Heat retention while cooling. |
| `MAX_TEMPERATURE` | Stage 0 `800`; electric chassis `600` baseline plus Heat Core and affix contribution | Recipe temperature gate. |
| `TEMPERATURE_STABILITY` | `0.85-1.18x` from Heat Cores before affixes | Precision recipes prefer higher values. |
| `FUEL_EFFICIENCY` | `0.9-1.1` | Fuel cost multiplier support. |
| `WARMUP_TIME` | `0.9-1.2` | Lower means reaching heat faster. |
| `COOLING_RATE` | `0.8-1.2` | Lower means retaining heat longer. |
| `OVERHEAT_TOLERANCE` | `1.0-1.25` | Safety margin above intended heat. |

## Alloy Furnace Range

Status: Prototype

Alloy Furnaces use authored chassis base stats plus required Heat Core and Alloy Crucible component base profiles. The chassis supplies FE storage, processing speed, energy usage, heat transfer, and stability. External FE intake is limited by free buffer space and the source/connector, not by a machine-side intake stat. The Heat Core supplies usable `MAX_TEMPERATURE`; the Alloy Crucible supplies active `INPUT_SLOTS` and mixing control stats. Live heat must reach recipe `target_temperature` before the cycle starts, and Stage 4+ failure-bearing recipes use the shared failure-strain model.

| Stat | Design Target | Notes |
|---|---:|---|
| `INPUT_SLOTS` | `2-4` | Authored by installed Alloy Crucible material and treated as a hard base-profile identity. |
| `ENERGY_CAPACITY` | `900-2200 FE` internal buffer | Supplemented by an optional installed Battery Cell. |
| `PROCESSING_SPEED` | `1.0-1.22x` chassis before Gear | Alloy Crucibles, Servos, and Bulk Speed can raise the effective value. |
| `ENERGY_USAGE` | `0.88-1.08x` | Scales total recipe FE cost. |
| `HEAT_TRANSFER` | `0.95-1.22x` | Reduces alloy processing time together with processing speed. |
| `MAX_TEMPERATURE` | Heat Core-authored | Must meet the recipe's `target_temperature`, defaulting to `minimum_temperature`. |
| `WARMUP_TIME` | `1.0x` neutral before affixes | Lower reaches target heat faster. |
| `COOLING_RATE` | `1.0x` neutral before affixes | Lower retains heat longer. |
| `TEMPERATURE_STABILITY` | `0.9-1.18x` from crucibles and Gear | Must meet recipe `required_temperature_stability` unless the active Stage 4+ recipe carries a failure output. |
| `OVERHEAT_TOLERANCE` | `1.0x+` from Servo or affixes | Widens the safe heat band before overheat strain. |
| `STABILITY` | `0.95-1.18x` chassis before Gear | General mixing reliability and modifier target. |

## Bio Generator Range

Status: Prototype

Bio Generators use FE output stats, fuel multipliers, and optional Bio Chamber ingredient-power prefixes. They have no machine-side export cap; the attached receiver or Universal Connector tier decides the export rate.

| Stat | Design Target | Notes |
|---|---:|---|
| `ENERGY_CAPACITY` | `400 FE` baseline | Internal working buffer before an installed Battery Cell is counted. |
| `ENERGY_GENERATION` | `1.0x` baseline | Multiplies FE/t power for each fuel item. |
| `FUEL_EFFICIENCY` | `1.0x` baseline | Multiplies effective fuel value before burn duration is calculated. |
| `FUEL_DURATION` | `1.0x` baseline | Multiplies fuel burn ticks without increasing FE/t. |
| `EFFICIENCY` | `1.0x` baseline | General Bio Generator fuel multiplier. |
| `POTATO_POWER`, `CARROT_POWER`, `BREAD_POWER`, `SAPLING_POWER`, `SEED_POWER` | `1.0x` baseline | Bio Chamber prefix multipliers that apply only to matching fuel tags. |

## Algae Photobioreactor Range

Status: Prototype

The Algae Photobioreactor is a non-FE organic support processor. Its Bio Chamber Gear slot reuses the chamber's `FUEL_EFFICIENCY` contribution as Bio Conversion for deterministic algae output scaling.

| Stat | Design Target | Notes |
|---|---:|---|
| `FUEL_EFFICIENCY` | `1.0x` baseline | Displayed as Bio Conversion on the Algae Photobioreactor Stats tab. Installed Bio Chamber values scale algae item output and bank fractional bonus progress between cycles. |

## Solar Generation Range

Status: Prototype

Solar Panels use authored material base stats for clear-day FE/t, weather FE/t, and internal buffer. The Solar Array Controller has its own authored controller base profile and multiplies the selected panels' environment-adjusted output. `SOLAR_PANEL_LIMIT` is currently the controller array range stat: base `1`, capped at effective range `6`, with additional range coming from controller rolls and installed Solar Array Extenders.

| Stat | Design Target | Notes |
|---|---:|---|
| `ENERGY_CAPACITY` | `256-1024 FE` panel buffers; `512 FE` controller buffer | Controller capacity can be supplemented by its optional Battery Cell Gear slot. |
| `ENERGY_GENERATION` | `4-24 FE/t` clear day panels; `1.0x` controller baseline | Panel profiles store clear-day output. Weather output is material-authored and lower than clear output. |
| `PEAK_SOLAR_GENERATION` | `1.0x` baseline; `1.5-2.0x` with Peak Solar | Applies only during the zenith window, from Minecraft time `4000` through `8000` each day. |
| `EFFICIENCY` | `1.0x` baseline | Multiplies generated FE after daylight and weather checks. |
| `STABILITY` | `1.0x` controller baseline | Controller-only stat that recovers part of the weather penalty when rolled above baseline. |

## Battery Chassis Range

Status: Prototype

Battery chassis blocks use energy-related stats differently from normal processing machines. Inserted cells provide the underlying stored FE, while chassis capacity prefixes scale those installed cells inside the chassis.

| Stat | Design Target | Notes |
|---|---:|---|
| `BATTERY_SLOTS` | `1-14` | Material identity provides `1-10` slots. The Additional Battery Slots prefix can add up to `+4` active slots. |
| `ENERGY_CAPACITY` | Installed-cell capacity multiplier | Increased capacity prefixes scale installed-cell capacity. Charged Storage is an untiered prefix that grants `100% more` capacity after the chassis has held energy continuously for `10` minutes. |
| `ENERGY_TRANSFER` | Material-defined | Displayed chassis transfer rating. Effective input is governed by inserted cell input rates and free cell space; effective output is capped by inserted cell output rates and stored FE. Cell rates are per-tick budgets shared by every side and connector. |
| `BURST_TRANSFER` | `1.0-4.0x ENERGY_TRANSFER` | Surge identity value retained for later risk tuning. Current transfer remains capped by inserted cell output rates. |
| `BURST_DURATION` | `0-100 ticks` | How long future burst-risk behavior can be sustained. `0` means no burst window. Runtime burst identity requires the `BURST_RELEASE` behavior. |
| `EFFICIENCY` | `0.9-1.1` | Charge or discharge efficiency. Higher means less loss. |
| `IDLE_LOSS` | `0-0.05%/minute` | Passive leakage. Lower is better. Steel Battery Chassis applies flat `CELL_LEAKAGE_DAMPING` to installed cell loss before chassis loss is added; Aethergold seals installed-cell leakage entirely. |
| `STABILITY` | `0.8-1.2` | Overload safety, burst reliability, and high-transfer risk. |
| `GLOBAL_MODIFIER_STRENGTH` | `0.7-1.3` | Per-cell strength of chassis-wide effects. Lower-slot chassis can be stronger per cell. |
| `BUFFER_SIZE` | Small or `1` | Optional internal bookkeeping buffer. Should not replace inserted cell capacity. |

## Component Assembler Range

Status: Prototype

Component Assembler uses normal powered-processing stats plus fluid-input transfer. Its public recipe type id remains `rngtech:battery_assembly` for compatibility. Its internal FE buffer is intentionally small; an optional Battery Cell Gear slot adds portable storage for longer assembly batches.

| Stat | Design Target | Notes |
|---|---:|---|
| `INPUT_SLOTS` | `4` authored slots | Recipe shape is fixed in the block entity. The current profile exposes input-slot modifiers for consistency, but the active Process tab owns four item inputs. |
| `OUTPUT_SLOTS` | `4` authored rack slots | The rack prevents unstackable rolled final cells from blocking after one craft. |
| `ENERGY_CAPACITY` | `1,600 FE` internal buffer | Supplemented by the optional installed Battery Cell. Side FE input fills the internal buffer first, then charges the installed Battery Cell when possible, with no machine-side intake cap. |
| `FLUID_TRANSFER` | `250 mB/t` input baseline | Caps side Electrolyte Solution or Lubricant filling. Dry `rngtech:electrolyte` dissolves at `125 mB` per item. |
| `PROCESSING_SPEED` | `1.0x` baseline | Scales `rngtech:battery_assembly` processing time. |
| `ENERGY_USAGE` | `1.0x` baseline | Scales recipe-authored FE cost. |
| `STABILITY` | `1.0x` baseline | Rollable assembly reliability surface for future recipe-risk behavior. |

## Fluid and Compressor Tank Range

Status: Prototype

Fluid and Compressor Tanks use authored material values for loose capacity, compressed physical volume, compression ratio, base fluid-transfer rate, and FE per bucket. Iron, Copper, and Bronze are plain Fluid Tanks with loose storage only, no FE capability, and no active Gear tab. Steel and later tanks need one valid Battery Cell, at least one valid Servo, and FE to compress or decompress fluid.

| Stat | Design Target | Notes |
|---|---:|---|
| `FLUID_CAPACITY` | `16,000-9,408,000 mB` | Total displayed ordinary-fluid capacity. This combines loose capacity and compressed effective capacity. It is authored and not a rollable affix in v1. |
| `COMPRESSION_RATIO` | `10x-48x` for Steel+ | Iron through Bronze are plain Fluid Tanks and do not show compression stats. Steel+ range from `10x` to `48x`. This is authored and not a rollable affix in v1. |
| `FLUID_TRANSFER` | `0-1000 mB/t` before Servo scaling | Steel+ tanks multiply this by Servo count and the average installed Servo processing-speed contribution. |
| `ENERGY_CAPACITY` | `2400-32000 FE` internal buffer for Steel+ | The installed Battery Cell adds extra stored FE, but the block exposes input only and cannot act as a battery. External FE goes into the internal buffer first, then into the installed Battery Cell if possible, with no machine-side intake cap. Iron through Bronze have no FE buffer or FE input. |
| `ENERGY_USAGE` | `1.0x` baseline | Scales compression and decompression FE per bucket. Decompression costs `25%` of compression. |
| `PROCESSING_SPEED` | `1.0x` baseline | Scales compression/decompression rate. Servo quality is averaged before it contributes. |
| `STABILITY` | `1.0x` baseline | Rollable profile surface for future pressure-risk behavior; v1 does not leak or void fluid when power is missing. |

## Resonance Calibrator Range

Status: Prototype

Resonance Calibrators use normal processing and FE stats plus calibration-specific stats. Chassis identity defines lane count and baseline personality; installed Gear defines the usable calibration envelope.

| Stat | Design Target | Notes |
|---|---:|---|
| `PROCESSING_SPEED` | Material-defined | Copper and Nullite favor speed; Lead and Tungstensteel trade single-cycle speed for dense output. |
| `STABILITY` | `0.9-1.2` | Raises or lowers calibrated output stability floor. |
| `CALIBRATION_QUALITY` | `0.9-1.4` | Raises output stability floor and ceiling. Resonance Coils and Stabilizer Matrices are the main Gear sources. |
| `CALIBRATION_PRECISION` | `0.9-1.4` | Narrows random stability spread. Control Boards and Nullite chassis are the main sources. |
| `CATALYST_EFFICIENCY` | `1.0+` | Gives a capped chance to preserve catalysts. Stabilizer Matrices are the main source. |
| `REFINEMENT_POTENTIAL_BONUS` | `0+` | Adds flat RP to calibration outputs. Titanium/Nullite chassis and Control Boards are the main sources. |
| `BATCH_SIZE` | `1-3` | Lead and Tungstensteel chassis consume multiple inputs/catalysts per completed cycle and output a same-state batch. Each calibration pays its own FE. |

## Refinement Potential

`REFINEMENT_POTENTIAL` tracks remaining crafting or rerolling budget for machine block stacks, machine parts, and placed machines.

Current refinement operations are documented in [Progression](https://c-pettersson.github.io/rngtech/rarity-and-affixes/).

## Source

Implemented stats are tracked in `com.rngtech.rpg.MachineStat`. Planned rows may not exist in code yet.
