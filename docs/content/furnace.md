# Furnace

Status: Prototype


Resource ids:

- Stage 0: `rngtech:furnace`
- Electric stages: `rngtech:iron_furnace_chassis`, `rngtech:copper_furnace_chassis`, `rngtech:bronze_furnace_chassis`, `rngtech:steel_furnace_chassis`, `rngtech:lead_furnace_chassis`, `rngtech:aluminum_furnace_chassis`, `rngtech:titanium_furnace_chassis`, `rngtech:tungstensteel_furnace_chassis`, and `rngtech:exotic_furnace_chassis`

## Summary

Furnaces are heat-aware item smelting processors with RNGTech machine traits and shared placed-machine refinement.

Stage 0 is the non-electric furnace. It burns standard furnace fuels in the Process tab, runs on a slow `0.533x` processing-speed profile, and reaches the early `800` heat band for Iron and Copper smelting. Stages 1-8 are electric furnace chassis blocks. They accept FE through a block energy capability, use a small internal working buffer, and can install a Battery Cell plus staged Heat Core in the Gear tab. Bronze carries `ALLOY_BLEND`, a behavior that speeds inputs tagged `rngtech:alloy_blend_smeltables`; default data tags Bronze Blend and Steel Blend for that path. Lead is a Stage 4 electric sidegrade built for bulk smelting: it has four active processing lanes and four Heat Core Gear slots, but carries a base `0.5x` processing-speed profile. Stage 5-8 metal-catalog Furnace recipes author higher FE costs, and electric recipes at or above the configured high-heat threshold cost extra FE. Furnace Mastery is machine-owned chassis progression; progression smelts grant XP, and passive nodes trade heat reach, throughput, control, and efficiency.

## Implementation Contract

Current runtime surface:

- Block entity: `FurnaceBlockEntity`.
- Menu and screen: `FurnaceMenu` and `FurnaceScreen`.
- Recipe source: custom `rngtech:furnace` recipe type.
- Slots: up to four paired input/output processing lanes, up to four component Gear slots, one shared Refinement-tab catalyst slot, and a mode-specific fuel or Battery Cell slot.
- Stage 0 fuel slot: shown on Process, accepts standard smelting fuel, and uses `EFFICIENCY` plus `FUEL_EFFICIENCY` for burn duration.
- Electric Battery Cell slot: shown on Gear, accepts `BatteryCellItem` stacks, and contributes stored FE through the item energy capability.
- Item capability automation: top inserts active input lanes, bottom extracts active output lanes, and side insertion targets the mode-specific fuel or Battery Cell slot.
- Energy capability: electric chassis receive FE. Stage 0 exposes no useful FE behavior.
- Gear tab accepts optional `MachinePartItem` stacks for the matching furnace machine type on Stage 0, but the Stage 0 furnace does not require a component to process. Electric Furnace chassis accept staged `HEAT_CORE` parts up to the chassis stage; Lead Furnace exposes four Heat Core slots and other electric chassis expose one.
- Refinement applies to the placed furnace's machine traits.
- Mastery applies to the placed furnace's `rngtech:machine_progression` component. Breaking, pick-blocking, and replacing the chassis preserve Mastery state when the block item keeps its copied components.

Implementation checks:

- Hopper or item-pipe insertion from the top should insert smeltable inputs.
- Stage 0 side insertion should insert valid furnace fuels.
- Electric stage side insertion should insert Battery Cells, and block energy insertion should fill the internal buffer or installed cell.
- Lead Furnace top insertion should expose all four active input lanes, and bottom extraction should expose all four active output lanes.
- Bottom extraction should extract completed output from active output lanes.
- `rngtech:furnace` recipes should process with RNGTech timing, temperature, and stability gates.
- Placed-machine refinement should persist after closing and reopening the screen.
- Mastery unlocks should require one unspent passive point, and at least one linked unlocked node. Unlocking a node resets active cycles and failure strain but preserves inventory and current lane heat.

## Stage Behavior

| Stage | Block | Mode | Notes |
|---:|---|---|---|
| 0 | `rngtech:furnace` | Solid fuel | Primitive non-electric furnace with slow solid-fuel throughput. |
| 1 | `rngtech:iron_furnace_chassis` | Electric | First FE-powered furnace chassis. |
| 2 | `rngtech:copper_furnace_chassis` | Electric | Better early heat throughput and energy usage. |
| 3 | `rngtech:bronze_furnace_chassis` | Electric | Alloy-smelting chassis with conditional speed for tagged Bronze and Steel blend inputs. |
| 4 | `rngtech:steel_furnace_chassis` | Electric | More efficient midgame chassis. |
| 4 | `rngtech:lead_furnace_chassis` | Electric | Four processing lanes and four Heat Core slots, with `50%` less base processing speed. |
| 5 | `rngtech:aluminum_furnace_chassis` | Electric | Faster controlled chassis. |
| 6 | `rngtech:titanium_furnace_chassis` | Electric | High-heat advanced chassis. |
| 7 | `rngtech:tungstensteel_furnace_chassis` | Electric | Heavy late-game heat chassis. |
| 8 | `rngtech:exotic_furnace_chassis` | Electric | Optional endgame chassis. |

Electric furnace chassis preserve base FE per craft as speed and heat transfer increase. Faster chassis consume more FE per active tick unless reduced `ENERGY_USAGE` offsets the cost. Stage 5-8 metal-catalog recipes use authored FE ramps of `2.0x`, `3.0x`, `4.0x`, and `6.0x`. Recipes with `minimum_temperature >= 1750` also use the default `2.0x` high-heat FE multiplier before `ENERGY_USAGE` is applied. Electric chassis without an installed Battery Cell keep only their small internal buffer and apply a processing-speed penalty.

## Implemented Furnace Stats

Stage 0 uses:

- `HEAT_TRANSFER`
- `HEAT_ISOLATION`
- `MAX_TEMPERATURE`
- `WARMUP_TIME`
- `COOLING_RATE`
- `TEMPERATURE_STABILITY`
- `OVERHEAT_TOLERANCE`
- `INPUT_SLOTS`
- `FUEL_EFFICIENCY`
- `PROCESSING_SPEED`
- `EFFICIENCY`
- `REFINEMENT_POTENTIAL`

Electric stages use:

- `HEAT_TRANSFER`
- `HEAT_ISOLATION`
- `MAX_TEMPERATURE`
- `WARMUP_TIME`
- `COOLING_RATE`
- `TEMPERATURE_STABILITY`
- `OVERHEAT_TOLERANCE`
- `INPUT_SLOTS`
- `PROCESSING_SPEED`
- `ENERGY_USAGE`
- `ENERGY_CAPACITY`
- `REFINEMENT_POTENTIAL`

The broader furnace stat range is documented in [Machine Stats](../reference/machine-stats.md#furnace-range). Warmup, cooling, overheat tolerance, and temperature stability are active heat-loop behavior.

## Recipe Use

All Furnace stages process `rngtech:furnace` recipes under `data/<namespace>/recipe/`. RNGTech ships mirrored `minecraft:smelting` coverage under `data/rngtech/recipe/furnace/vanilla/`, material-catalog metal recipes under `data/rngtech/recipe/furnace/metals/`, alloy blend smelting recipes under `data/rngtech/recipe/furnace/alloy_blends/`, and malformed-ingot recovery recipes under `data/rngtech/recipe/furnace/malformed/`. Most stages expose one input slot and one output slot. Lead Furnace exposes four paired input/output lanes and can progress up to four recipes at once, consuming FE per active lane.

Furnace recipes currently support:

- `ingredient`
- optional `malformed_material`
- `result`
- `processing_ticks`
- `energy`
- optional `experience`
- optional `minimum_temperature`
- optional `required_temperature_stability`
- optional `target_temperature`
- optional `safe_maximum_temperature`
- optional `failure_output`
- optional `failure_material`
- optional `power_sensitive`
- optional `bonus_output`
- optional `machine_xp`
- optional `machine_xp_band`

With JEI installed, Furnace recipes are exposed with processing ticks, scaled FE cost, experience, minimum heat, temperature-stability gates, positive machine XP awards, and bonus-output opt-outs. Recipes at or above the configured high-heat threshold show the multiplied FE cost. Recipes with default zero gates are shown as having no heat or stability gate.

Example:

```json
{
  "type": "rngtech:furnace",
  "group": "rngtech_metals",
  "ingredient": {
    "item": "rngtech:iron_dust"
  },
  "result": {
    "id": "minecraft:iron_ingot"
  },
  "processing_ticks": 200,
  "energy": 4800,
  "experience": 0.7,
  "minimum_temperature": 800,
  "target_temperature": 800,
  "safe_maximum_temperature": 1100,
  "required_temperature_stability": 0.0
}
```

Metal-catalog ore, raw, non-alloy dust, and non-alloy crushed recipes use stage-scaled `minimum_temperature`, and Stage 4+ metals require temperature stability. Stage 5-8 material-form recipes author `2.0x`, `3.0x`, `4.0x`, and `6.0x` FE costs compared to early-stage baselines. Mirrored vanilla iron, copper, gold, and ancient-debris smelting recipes use the matching heat gates so vanilla ore and raw-metal inputs cannot bypass RNGTech metal heat progression. Disabled material families remain excluded from their furnace recipes through the `rngtech:material_enabled` condition. Crushed inputs smelt directly for early hopper automation, but cost more FE and take substantially longer than dust inputs. Malformed-ingot recovery recipes use `malformed_material` to match the stack's stored `rngtech:material` component and return two matching nuggets over `400` ticks for `1,200 FE`; these recipes disable output bonuses and are modeled as failure/recovery outputs, not ordinary progression sources.

Furnace `machine_xp` is granted once per successful normal output-producing lane completion after recipe-band falloff. Lead Furnace can therefore earn XP from each active lane. Failure outputs, blocked cycles, invalid inputs, malformed recovery, reversible dust-to-ingot loops, food, decorative blocks, and ordinary utility smelts grant no machine XP unless a datapack explicitly authors it. If `machine_xp_band` is omitted, positive-XP recipes derive their band from `target_temperature`, defaulting to `minimum_temperature`: `<=800 -> 1`, `<=1000 -> 5`, `<=1200 -> 9`, `<=1400 -> 13`, `<=1600 -> 17`, `<=1800 -> 21`, and `>1800 -> 25`. Recipe XP grants full value at or below band, `50%` one level over, `25%` two levels over, and zero above that. Fractional XP is banked on the machine progression state in quarter-XP units.

Dust-to-ingot Furnace recipes that pair with Crusher ingot-to-dust recipes set `bonus_output: false`. They still smelt normally, but Furnace Super Output cannot turn the reversible iron, copper, or tin conversion chain into an ingot source.

Steel is intended to come through the Alloy Furnace route. Stage 3 Bronze Alloy Furnace gear can bootstrap Steel Blend from Iron Dust, Coal, and Charcoal flux, then Furnace recipes smelt Steel Blend into Steel Ingots. Higher-heat Alloy Furnace recipes can output Steel Blend more efficiently or output Steel Ingots directly. No default Furnace Steel Dust smelting path ships in the public data.

Current stat behavior:

- Furnace Mastery passive stats participate in the same stat stack as chassis base stats, machine traits, installed Heat Cores, Battery Cell penalties, and Bulk Speed.
- `PROCESSING_SPEED` and `HEAT_TRANSFER` reduce smelting time.
- Super Output can add one extra copy of eligible item outputs; recipes with `bonus_output: false` ignore it.
- Processing does not start until live heat reaches `target_temperature`, defaulting to `minimum_temperature`.
- Warmup is `max(1, round(10 * HEAT_TRANSFER / max(0.25, WARMUP_TIME)))` C/t. Electric warmup consumes the recipe's normal FE/t; Stage 0 warmup consumes fuel burn ticks.
- Cooling is `max(1, round(2 * COOLING_RATE / max(0.25, HEAT_ISOLATION)))` C/t while the lane is blocked, out of input, or power/fuel-starved.
- `MAX_TEMPERATURE` must meet a recipe's `target_temperature`.
- `TEMPERATURE_STABILITY` remains a hard gate for normal recipes. Stage 4+ failure-bearing recipes instead turn poor stability, dips below `minimum_temperature`, overheat above the safe band, and sensitive post-start power drops into failure strain.
- Failure strain fills to `6000`; when a Stage 4+ cycle with `failure_output` crosses that threshold, it consumes the input into the recoverable failure stack. `power_sensitive: true` adds `1400` strain per empty-FE tick after processing has started, or `700` when `POWER_GRACE` is present.
- `MAX_TEMPERATURE`, `HEAT_TRANSFER`, `HEAT_ISOLATION`, `WARMUP_TIME`, `COOLING_RATE`, `TEMPERATURE_STABILITY`, and `OVERHEAT_TOLERANCE` can come from chassis affixes, installed Heat Core base profiles, and installed Heat Core affixes. Heat Cores author their heat bonus, heat transfer, and heat stability directly, then stored affixes scale or adjust those values while installed.
- Bronze Furnace applies its identity speed bonus only when the active input is tagged `rngtech:alloy_blend_smeltables`. Default data tags Bronze Blend and Steel Blend; ordinary smelting does not receive that bonus.
- Stage 0 uses `EFFICIENCY` and `FUEL_EFFICIENCY` to affect fuel duration, and its `0.533x PROCESSING_SPEED` base keeps solid-fuel smelting intentionally slow before traits.
- Electric stages use recipe `energy` as the base FE per craft. `PROCESSING_SPEED` and `HEAT_TRANSFER` raise FE/t as they shorten the process; `ENERGY_USAGE` can change total FE per craft. External FE intake is limited by free buffer or installed-cell space plus the source or attached Universal Connector, while the screen reports FE/t demand per lane and total active demand for Lead Furnace.
- If the combined visible FE store has enough energy but the installed Battery Cell cannot discharge enough FE/t for the current lane demand, the status reports Cell Rate Limited instead of No Power.
- Stage 5-8 metal-catalog recipes author higher base FE costs, and electric recipes with `minimum_temperature >= 1750` cost another `2.0x` FE by default before `ENERGY_USAGE`. This means Tungsten, Tungstensteel, Platinum, Aethergold, Nullite, Naquadah, Netherite, and future Stage 7+ heat-band recipes stack the stage-authored cost with the high-heat rule. Stage 0 has no FE cost path and is unaffected.
- Stage 0 has an authored `800` `MAX_TEMPERATURE` so standard fuel can process the first Iron and Copper recipes. Electric furnace chassis start at a `600` baseline before Heat Core and affix contribution. With unmodified Heat Cores, the effective electric ladder is Iron `800`, Copper `1000`, Bronze `1200`, Steel `1400`, Sparksteel `1600`, and Titanium `1800`. Aluminum is a Stage 5 speed sidegrade at `850` heat with `2.20x` Heat Core transfer; flat `MAX_TEMPERATURE` rolls can push it over early heat thresholds, but its base identity stays low-heat and fast. Iron-tier heat covers Iron and Copper smelting; Copper Heat Cores then open the `1000` band for Bronze ingredients and similar early metals. Higher-heat recipes require better components, `MAX_TEMPERATURE` affixes, or refinement.
- Lead Furnace has base `INPUT_SLOTS` and `OUTPUT_SLOTS` values of `4` and a base `0.5x PROCESSING_SPEED` profile. Its Heat Core slots are lane-local for processing, so slot 1 controls lane 1, slot 2 controls lane 2, and so on. The Stats tab reports the strongest installed Heat Core as the displayed heat profile; the four cores do not add into one shared maximum-temperature ceiling.

## Mastery

Furnaces enter the [shared Machine Mastery tree](../systems/machine-mastery.md) at Drive / Reserve. Heat, throughput, efficiency, attributes, and behavior choices share routes with other machine families. Fuel-only effects are marked inactive on electric chassis, and FE effects are marked inactive on the primitive fuel chassis.

Low Heat Specialist caps maximum temperature at 800 and grants 100% more Processing Speed. Flash Annealing caps it at 600, grants 100% more Processing Speed, and applies 50% more Energy Usage. Both speed bonuses are [tagged](../systems/machine-mastery.md#tagged-payoffs) for heated machines, so they reach the Furnace, Alloy Furnace, and Metal Press but not machines their heat caps cannot limit. A cap never raises a weaker heat source. Regulated Heat caps maximum temperature at 1000 with 80% more Temperature Stability. Kiln Discipline grants 60% more Temperature Stability with 40% more Warmup Time. Gear cannot override these absolute limits; the lower limit wins if several apply.

Quench Protocol halves temperature-derived failure strain, leaves power-drop strain unchanged, and applies 25% less Heat Transfer. Closed Loop Recuperator returns 5% of adjusted electric recipe FE after successful output, with 15% less Processing Speed. Its recovery is inactive on the primitive Furnace, while its speed penalty still applies.

Nodes do not change Furnace slot counts or Heat Core compatibility. Allocation changes reset active cycles and failure strain while preserving inventory and current lane heat. Shared progression, attributes, refunds, copying, and migration are defined on the Mastery page.

## Ascendancies

Status: Prototype

Furnace machines choose between Crucible Keeper and Bloomer when they use their first Ascendancy Seal. [Machine Mastery](../systems/machine-mastery.md#ascendancies) defines Seals, points, refunds, and switching, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines the new stats. The tables below are generated from the ascendancy catalog.

- Crucible Keeper’s Overdrive Lanes heat a working lane past the recipe target toward its maximum temperature. Each step costs another heat tick of FE or fuel. With any Overdrive Margin, it stops that far below the recipe’s safe maximum, and Safe Margin keeps an overdriving lane’s stability wobble out of the overheat band.
- Hold the Fire keeps a lane’s heat whenever its input slot holds anything the Furnace can smelt. Shared Hearth gives every Lead Furnace lane at least 90% of the hottest lane’s maximum.
- Crucible Heart pays a second craft’s FE, or its burn time on a fuel furnace.
- The Bloom Ledger feeds from bonus-eligible smelts of inputs in `rngtech:bloom_ledger_inputs` (ores, raw ores, and crushed materials) and pays whole items when the output has room. The Process tab draws each lane’s ledger along the bottom of its progress bar.
- Fluxed Blend lowers blend smelts’ minimum, target, and safe maximum by 100 °C. Slag Reclaim speeds up and cheapens malformed-ingot recovery without changing its output.

<!-- ascendancy-trees:start -->

### Crucible Keeper

| Node | Type | After | Effect |
|---|---|---|---|
| **Overdrive Lanes** | Root | — | +1% Overdrive Speed; +30% Overdrive Cap. |
| Hotter Lanes | Small | Overdrive Lanes | +5% Overdrive Cap. |
| **Superheat** | Notable | Hotter Lanes | +15% Overdrive Cap. |
| Deep Draft | Small | Superheat | 10% increased Heat Transfer. |
| **Crucible Heart** | Deep notable | Deep Draft | Recipes whose target is at most half the lane’s temperature finish two inputs per cycle at twice the FE. |
| Banked Coals | Small | Overdrive Lanes | 10% increased Heat Insulation. |
| **Hold the Fire** | Notable | Banked Coals | A lane does not cool while its input slot holds a smeltable input. |
| Watchful Gauge | Small | Overdrive Lanes | +10 °C Overdrive Margin. |
| **Safe Margin** | Notable | Watchful Gauge | +15 °C Overdrive Margin. Overdrive never enters a recipe’s overheat band. |
| Steady Hands | Small | Safe Margin | +2 Strain Recovery. |
| **Strain Bleed** | Deep notable | Steady Hands | +10 Strain Recovery. |
| Stoked Hearth | Small | Overdrive Lanes | 4% increased Max Temperature. |
| **Shared Hearth** | Notable | Stoked Hearth | Every lane uses the hottest installed Heat Core’s maximum at 90%. |

### Bloomer

| Node | Type | After | Effect |
|---|---|---|---|
| **Bloom Ledger** | Root | — | +11.11% Ledger Rate; 50% less Super Output. |
| Rich Ore | Small | Bloom Ledger | 10% increased Ledger Rate. |
| **Rich Blooms** | Notable | Rich Ore | 100% increased Ledger Rate. |
| Slow Growth | Small | Rich Blooms | 10% increased Ledger Rate. |
| **Patient Bloom** | Deep notable | Slow Growth | 50% more Ledger Rate; 30% less Processing Speed. |
| Flux Bed | Small | Bloom Ledger | 8% increased Temperature Stability. |
| **Fluxed Blend** | Notable | Flux Bed | Blend smelts need 100 °C less. |
| Skimmed Slag | Small | Bloom Ledger | 8% increased Stability. |
| **Slag Reclaim** | Notable | Skimmed Slag | Malformed-ingot recovery takes half the time and half the FE. |
| Clean Pour | Small | Slag Reclaim | 8% increased Temperature Stability. |
| **Clean Bloom** | Deep notable | Clean Pour | Ore, raw, and crushed smelts never produce failure outputs. Low stability pauses the lane instead. |
| Fed Line | Small | Bloom Ledger | 10% increased Ledger Rate. |
| **Crusher Line** | Notable | Fed Line | Crushed inputs feed the Bloom Ledger twice. |

<!-- ascendancy-trees:end -->

## Screen Tabs

The furnace screen has Processing, Gear, Stats, Refinement, and Mastery tabs. The Process tab adapts to the placed stage: Stage 0 shows a fuel slot and burn bar, electric single-lane stages show an energy bar and move the Battery Cell to Gear, and Lead shows four compact lane progress bars. The Process tab also shows live heat and, for failure-bearing recipes, failure strain; hover text exposes current heat, minimum, target, safe maximum, overheat limit, and power-drop strain status. The Refinement tab targets the placed furnace itself, accepts one refinement catalyst, and applies the shared refinement rules. The Mastery tab shows machine XP, level, unspent passive points, and the shared passive tree.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine implicit | Furnace chassis material provides base heat, processing, electric behavior, and fixed behavior flags such as `ALLOY_BLEND`, `DENSE_PARALLEL`, `OUTPUT_GUARD`, and `THERMAL_BUFFER`. |
| Machine prefix | Stage 0 uses the `FURNACE` profile; electric stages use the `ELECTRIC_FURNACE` profile. Heat-capable machine prefixes can include `HEAT_TRANSFER`, `MAX_TEMPERATURE`, `HEAT_ISOLATION`, reduced `WARMUP_TIME`, and reduced `COOLING_RATE`. |
| Machine suffix | Stage-specific profiles keep fuel-only and FE-only stats separate. Heat-control suffixes include `TEMPERATURE_STABILITY` and `OVERHEAT_TOLERANCE`. |
| Machine behavior | Electric Furnace can roll `POWER_GRACE`, which halves sensitive power-drop strain. |
| Machine part modifiers | Electric Furnace chassis apply installed Heat Core base profiles plus stored Heat Core affixes, including heat transfer, retention, warmup, cooling, maximum temperature, temperature stability, and overheat tolerance; Lead exposes four slots, while other electric stages expose one. Stage 0 keeps a saved component slot for future Furnace-specific parts. |

See [Modifier Eligibility](../reference/modifier-eligibility.md#current-code-profiles).

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Parts](machine-parts.md)
- [Alloy Furnace](alloy-furnace.md)
- [Machine Stats](../reference/machine-stats.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
