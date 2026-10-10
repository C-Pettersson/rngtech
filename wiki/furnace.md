---
wiki:
  category: Processing
  icon: rngtech:furnace
  ids:
    - rngtech:furnace
    - rngtech:iron_furnace_chassis
    - rngtech:copper_furnace_chassis
    - rngtech:bronze_furnace_chassis
    - rngtech:steel_furnace_chassis
    - rngtech:lead_furnace_chassis
    - rngtech:aluminum_furnace_chassis
    - rngtech:titanium_furnace_chassis
    - rngtech:tungstensteel_furnace_chassis
    - rngtech:exotic_furnace_chassis
---

# Furnace

{{ infobox(
    variants=[
        ["0", "rngtech:furnace"],
        ["Iron", "rngtech:iron_furnace_chassis"],
        ["Copper", "rngtech:copper_furnace_chassis"],
        ["Bronze", "rngtech:bronze_furnace_chassis"],
        ["Steel", "rngtech:steel_furnace_chassis"],
        ["Lead", "rngtech:lead_furnace_chassis"],
        ["Aluminum", "rngtech:aluminum_furnace_chassis"],
        ["Titanium", "rngtech:titanium_furnace_chassis"],
        ["Tungstensteel", "rngtech:tungstensteel_furnace_chassis"],
        ["Exotic", "rngtech:exotic_furnace_chassis"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "0–8",
        "Power": "Solid fuel (Stage 0), FE (Stages 1–8)",
        "Gear": "{{ item('rngtech:iron_heat_core', 'Heat Core') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "Yes",
    },
) }}

The **Furnace** is RNGTech's heat-aware smelter. Each recipe needs the machine to warm up to a target temperature first, so the Furnace's stage and installed Heat Core decide which ores and alloys it can smelt. Stage 0 burns ordinary furnace fuel. Stages 1–8 are electric chassis that run on FE.

Like every RNGTech machine, each Furnace you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

Each stage has its own recipe. You do not need the previous stage's Furnace to craft the next one.

{{ crafting() }}

### Breaking

Mine a placed Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress.

## Usage

### Smelting

Put items in the top input slot. The Furnace heats up at a rate set by its Heat Transfer and Warmup Time stats, and starts smelting only once the lane reaches the recipe's target temperature. While the lane is blocked, out of input, or out of power, it cools down.

Heat decides your progress. Unmodified electric chassis reach about:

| Heat Core | Max temperature | Unlocks |
|---|---:|---|
| Stage 0 Furnace (no core) | 800 | Iron and Copper |
| {{ item('rngtech:iron_heat_core') }} | 800 | Iron and Copper |
| {{ item('rngtech:copper_heat_core') }} | 1,000 | Bronze ingredients and other early metals |
| {{ item('rngtech:bronze_heat_core') }} | 1,200 | Mid-tier metals |
| {{ item('rngtech:steel_heat_core') }} | 1,400 | Stage 4 metals |
| {{ item('rngtech:sparksteel_heat_core') }} | 1,600 | High-heat metals |
| {{ item('rngtech:titanium_heat_core') }} | 1,800 | Late-game metals |
| {{ item('rngtech:exotic_heat_core') }} | 2,200 | Every metal recipe |
| {{ item('rngtech:aluminum_heat_core') }} | 850 | Low-heat Stage 5 sidegrade that heats up much faster |

Vanilla ores and raw metals go through the same heat gates as RNGTech metals, so you cannot skip heat progression by smelting them directly.

Crushed material smelts straight into ingots, which is handy for simple hopper lines, but it takes three times as long and costs 50% more FE than smelting dust. Crush it into dust first for faster, cheaper smelting.

There is no Steel Dust smelting recipe. Steel comes from Steel Blend, which you make in the [Alloy Furnace](alloy-furnace.md) and then smelt here.

Stage 4 and higher recipes also need temperature stability. On recipes that have a failure output, poor stability, overheating, or a power drop mid-cycle builds up failure strain. When strain fills, the input becomes a recoverable failure item such as a {{ item('rngtech:malformed_ingot') }}. Smelt it again to get two nuggets back.

### Stages

| Stage | Furnace | Notes |
|---:|---|---|
| 0 | {{ item('rngtech:furnace') }} | Burns standard furnace fuel. Slow, and reaches 800 heat. |
| 1 | {{ item('rngtech:iron_furnace_chassis') }} | First electric Furnace. |
| 2 | {{ item('rngtech:copper_furnace_chassis') }} | Better early heat throughput and energy use. |
| 3 | {{ item('rngtech:bronze_furnace_chassis') }} | Smelts Bronze Blend and Steel Blend faster. |
| 4 | {{ item('rngtech:steel_furnace_chassis') }} | More efficient midgame chassis. |
| 4 | {{ item('rngtech:lead_furnace_chassis') }} | Four smelting lanes and four Heat Core slots, at half the base speed. |
| 5 | {{ item('rngtech:aluminum_furnace_chassis') }} | Fast, efficient body with quick heat transfer. |
| 6 | {{ item('rngtech:titanium_furnace_chassis') }} | High-heat advanced chassis. |
| 7 | {{ item('rngtech:tungstensteel_furnace_chassis') }} | Heavy late-game heat chassis. |
| 8 | {{ item('rngtech:exotic_furnace_chassis') }} | Optional endgame chassis. |

Higher-stage metal recipes cost more FE: Stage 5–8 recipes cost 2×, 3×, 4× and 6× the early baseline. Recipes needing 1,750 heat or more cost another 2× on electric Furnaces.

### Gear

Electric Furnaces have a Gear tab with:

- one **Heat Core** slot (four on the Lead Furnace, one per lane). The core's stage cannot exceed the chassis stage. On the Lead Furnace each lane heats with its own core, and the four cores do not combine. The Stats tab shows the strongest core, the one with the highest maximum temperature.
- one **Battery Cell** slot. Without a cell, the Furnace keeps only a small internal buffer and smelts more slowly. If the Furnace has enough FE stored but the cell cannot hand it over fast enough for the current work, the status reads **Cell Rate Limited** instead of No Power. A cell with a higher Output rate fixes it.

The Stage 0 Furnace burns fuel from a slot on its Process tab and does not need Gear to run.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input lane(s). Accepts only items with a Furnace recipe. |
| Bottom | Extracts finished output. |
| Sides | Fuel on Stage 0, Battery Cells on electric stages. |
| Any | Electric stages accept FE, for example from a [Universal Cable](universal-cable.md). |

### Mastery and Ascendancies

Smelting ores, raw metals, crushed ores, and alloy blends earns Furnace Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. Reversible dust-to-ingot smelts, food, and decorative blocks earn no XP. Furnaces choose between the **Crucible Keeper** and **Bloomer** [ascendancies](machine-mastery.md#ascendancies).

### Furnace recipes

The Furnace uses its own recipe type, `rngtech:furnace`, which covers vanilla smelting as well as RNGTech metals, alloy blends, and malformed-ingot recovery.

{{ processing("furnace", hide=["experience"]) }}

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

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

## Screen

The Furnace screen has five tabs:

- **Process**: input and output lanes, live heat, failure strain, and energy or fuel. Hover for exact temperatures.
- **Gear**: Heat Core and Battery Cell slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Furnace's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Data values

{{ data_values() }}

## See also

- {{ item('rngtech:bronze_alloy_furnace_chassis', 'Alloy Furnace') }}
- [Machine Stats](machine-stats.md)

{{ navbox() }}
