---
wiki:
  category: Processing
  icon: rngtech:furnace
  ids:
    - rngtech:furnace
    - rngtech:iron_furnace_chassis
    - rngtech:copper_furnace_chassis
    - rngtech:gold_furnace_chassis
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
        ["Gold", "rngtech:gold_furnace_chassis"],
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

You can read a Furnace from the outside. The Stage 0 Furnace is a smooth-stone kiln: fuel burns in the fire mouth at the bottom, the item being smelted sits in the oven above it, and smoke rises from the copper-banded chimney while fuel burns. An electric Furnace shows the item it is smelting on the hearth behind its viewport door. As the lane heats up, the chamber and its heating elements glow from dull red through orange to yellow-white. The bar under the door reads like the [heat bar](heat.md#the-overheat-slider) on the Process tab: it fills with the lane's heat against the Heat Core's Max Temperature, a white mark shows the recipe's target and a yellow mark the setpoint when the slider pushes past it, and the fill turns red while the lane overheats. A lane pushed past its target flickers with its wobble. With extra lanes, the items stand side by side on the hearth and the bar reads the hottest lane. The Heat Core sits on the crown as a row of fins in the core's material. Facing the front, the Battery Cell stands in the cradle on the left with its charge meter, the [Nitrogen tank](heat.md#nitrogen-atmosphere) is the gas cylinder behind it, and the cable port is on the back. The window on the cylinder shows how full the tank is, and its valve lights up while a lane uses Nitrogen. The Lead Furnace has four small chambers, one per lane: under each column of doors, the upper half of the bar reads the top chamber and the lower half the bottom one. Each chassis has its own shell metal.

## Obtaining

### Crafting

Each stage has its own recipe. You do not need the previous stage's Furnace to craft the next one.

{{ crafting() }}

### Breaking

Mine a placed Furnace with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress.

## Usage

### Smelting

Put items in the top input slot. Each lane heats up and starts smelting once it reaches the recipe's temperature, and leaks heat while it has nothing to smelt. The Furnace pays FE, or fuel, for the heat it puts in, including what its walls lose, so a hotter lane costs more per smelt. See [Heat](heat.md) for how lanes heat and the overheat slider on the Process tab.

Each metal smelts at its own pace: low-melting Lead and Tin in half the time of Iron, high-melting Osmium, Titanium, and Tungsten in two and a half to four times as long. The FE per smelt does not change.

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

Stage 4 and higher metal recipes also need temperature stability. A Stage 0–3 Furnace will not start one without enough stability. A Stage 4 or higher Furnace starts it anyway, but poor stability or heat above the recipe's safe maximum builds up failure strain. A lane left above the recipe's overheat temperature by a hotter recipe cools down to it before smelting, so switching to a cooler metal costs time instead of ingots. Higher Overheat Tolerance shortens that wait. When strain fills, the input becomes a {{ item('rngtech:malformed_ingot') }} of that metal. Smelt it again to get two nuggets back. On any Furnace, a metal smelt pushed past its overheat temperature with the [overheat slider](heat.md#the-overheat-slider) builds strain the same way. Ancient Debris never fails.

### Stages

| Stage | Furnace | Notes |
|---:|---|---|
| 0 | {{ item('rngtech:furnace') }} | Burns standard furnace fuel. Slow, and reaches 800 heat. |
| 1 | {{ item('rngtech:iron_furnace_chassis') }} | First electric Furnace. |
| 2 | {{ item('rngtech:copper_furnace_chassis') }} | Better early heat throughput and energy use. |
| 3 | {{ item('rngtech:gold_furnace_chassis') }} | Copper numbers with one built-in [jewel socket](jewels.md#body-sockets). |
| 3 | {{ item('rngtech:bronze_furnace_chassis') }} | Smelts Bronze Blend and Steel Blend faster. |
| 4 | {{ item('rngtech:steel_furnace_chassis') }} | More efficient midgame chassis. |
| 4 | {{ item('rngtech:lead_furnace_chassis') }} | Four smelting lanes sharing one Heat Core, at half the base speed. |
| 5 | {{ item('rngtech:aluminum_furnace_chassis') }} | Fast, efficient body with quick heat transfer. |
| 6 | {{ item('rngtech:titanium_furnace_chassis') }} | High-heat advanced chassis. |
| 7 | {{ item('rngtech:tungstensteel_furnace_chassis') }} | Heavy late-game heat chassis. |
| 8 | {{ item('rngtech:exotic_furnace_chassis') }} | Optional endgame chassis. |

Higher-stage metal recipes need more heat: Stage 5–8 recipes take 2×, 3×, 4× and 6× the early baseline, and recipes needing 1,750 heat or more twice that again.

### Batching

With a Batch Size above 1, each lane smelts several items from its input stack in one cycle. A batch takes as many items as the stack and the lane's free output space allow, up to the Batch Size, and keeps that count until the cycle ends. Each extra item makes the cycle longer by the Batch Overhead, 25% of one item's time to start with. Every item still absorbs its own heat, paid in FE or burn time, and earns its own XP. A bigger batch takes a bigger heat dip from the lane when it goes in. If a batch fails, every item in it becomes a {{ item('rngtech:malformed_ingot') }}.

### Gear

Electric Furnaces have a Gear tab with:

- one **Heat Core** slot. The core's stage cannot exceed the chassis stage. On the Lead Furnace one core heats all four lanes. A Lead Furnace built before lanes shared their core keeps its first core; the others go into an inventory beside it, or drop once a player is nearby.
- one **Battery Cell** slot. Without a cell, the Furnace keeps only a small internal buffer and smelts more slowly. If the Furnace has enough FE stored but the cell cannot hand it over fast enough for the current work, the status reads **Cell Rate Limited** instead of No Power. A cell with a higher Output rate fixes it.

The Stage 0 Furnace burns fuel from a slot on its Process tab and does not need Gear to run. Its Efficiency, Fuel Efficiency, and Fuel Duration stats all make each fuel item burn longer, so one item smelts more.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input lane(s). Accepts only items with a Furnace recipe. |
| Bottom | Extracts finished output. |
| Sides | Fuel on Stage 0, Battery Cells on electric stages. |
| Any | Electric stages accept FE, for example from a [Universal Cable](universal-cable.md), and Nitrogen for the [Nitrogen atmosphere](heat.md#nitrogen-atmosphere). |

Automation fills the input lanes in order: lane 1 takes a full stack before lane 2 gets anything, so a slow feed into a Lead Furnace keeps one lane busy while the others sit idle. The **Even Feed** keystone on the [Machine Mastery](machine-mastery.md) tree changes that. With it, each item that arrives through automation goes to the emptiest lane that can hold it, so a hopper trickle or a full stack from a cable spreads across every lane. It does not change which items the Furnace accepts, and a lane never holds two kinds of item. Items you place by hand still go where you put them. Even Feed sits right on a road, 15 points from the Furnace start, and has no downside beyond the points it costs.

### Mastery and Ascendancies

Smelting ores, raw metals, crushed ores, and alloy blends earns Furnace Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. Reversible dust-to-ingot smelts, food, and decorative blocks earn no XP. Furnaces choose between the **Crucible Keeper** and **Bloomer** [ascendancies](machine-mastery.md#ascendancies). Crucible Keeper's Overdrive speeds a lane up only past the recipe's safe maximum, so it needs the **Blast Draft** keystone to let the [overheat slider](heat.md#the-overheat-slider) go that far.

Two keystones cap your heat in exchange for speed or savings. Each cap only stops Gear from going higher; it never raises a weaker Heat Core. Compare the caps with the heat column in the [recipe table](#furnace-recipes).

- **Flash Annealing** caps Max Temperature at 1,000 heat. It gives 100% more Processing Speed, with 50% more Energy Usage and 50% less Fuel Efficiency. An electric Furnace smelts twice as fast, and each smelt costs 1.5 times the FE. A Stage 0 Furnace smelts twice as fast for the same fuel per smelt.
- **Low Heat Specialist** caps Max Temperature at 800 heat, enough for Iron and Copper. It gives 50% less Energy Usage and 100% more Fuel Efficiency, so each smelt costs half the FE or fuel at the same speed.

Other Furnace keystones:

- **Quench Protocol** halves temperature failure strain, for 25% less Heat Transfer.
- **Closed Loop Recuperator** returns 25% of each electric smelt's FE to the internal buffer, for 15% less Processing Speed.
- **Thermal Reservoir** gives 40% more Heat Insulation and 50% more Thermal Mass, so the Furnace keeps its heat between jobs and through load dips, but warms up more slowly from cold.
- **High Flux** gives 100% more Heat Transfer for 25% more Energy Usage and 40% less Fuel Duration.

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
| **Crucible Heart** | Deep notable | Deep Draft | +1 Batch Size while a lane runs at twice the recipe target or hotter. |
| Banked Coals | Small | Overdrive Lanes | 10% increased Heat Insulation. |
| **Hold the Fire** | Notable | Banked Coals | A lane does not cool while its input slot holds a smeltable input. |
| Watchful Gauge | Small | Overdrive Lanes | +10 °C Overdrive Margin. |
| **Safe Margin** | Notable | Watchful Gauge | +15 °C Overdrive Margin. The overheat slider stops at the recipe’s safe maximum, and wobble never spikes past the overheat temperature. |
| Steady Hands | Small | Safe Margin | +2 Strain Recovery. |
| **Strain Bleed** | Deep notable | Steady Hands | +10 Strain Recovery. |
| Stoked Hearth | Small | Overdrive Lanes | 4% increased Max Temperature. |
| **Shared Hearth** | Notable | Stoked Hearth | Each load dip is split across every lane that is smelting. |

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
| Skimmed Slag | Small | Bloom Ledger | 8% increased Temperature Stability. |
| **Slag Reclaim** | Notable | Skimmed Slag | Malformed-ingot recovery takes half the time and half the FE. |
| Clean Pour | Small | Slag Reclaim | 8% increased Temperature Stability. |
| **Clean Bloom** | Deep notable | Clean Pour | Ore, raw, and crushed smelts never produce failure outputs. Low stability pauses the lane instead. |
| Fed Line | Small | Bloom Ledger | 10% increased Ledger Rate. |
| **Crusher Line** | Notable | Fed Line | Crushed inputs feed the Bloom Ledger twice. |

<!-- ascendancy-trees:end -->

## Screen

The Furnace screen has six tabs:

- **Process**: input and output lanes, live heat, failure strain, and energy or fuel. Electric stages add the Nitrogen gauge at the right edge. Hover for exact temperatures. Hover a lane's progress bar for its time remaining and expected items per minute with your Gear and Mastery applied (chance bonuses such as Super Output are not counted).
- **Gear**: Heat Core and Battery Cell slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Furnace's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.
- **Orders**: three drive bays for [Job Chips](work-orders.md), with each chip's job, progress, and the Activate, Pause, Rush, and Abandon buttons.

## Data values

{{ data_values() }}

## See also

- {{ item('rngtech:bronze_alloy_furnace_chassis', 'Alloy Furnace') }}
- [Machine Stats](machine-stats.md)

{{ navbox() }}
