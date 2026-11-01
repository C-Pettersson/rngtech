---
wiki:
  category: Processing
  icon: rngtech:iron_crusher_chassis
  ids:
    - rngtech:wooden_crusher_chassis
    - rngtech:iron_crusher_chassis
    - rngtech:copper_crusher_chassis
    - rngtech:gold_crusher_chassis
    - rngtech:bronze_crusher_chassis
    - rngtech:steel_crusher_chassis
    - rngtech:aluminum_crusher_chassis
    - rngtech:titanium_crusher_chassis
    - rngtech:tungstensteel_crusher_chassis
    - rngtech:exotic_crusher_chassis
    - rngtech:jam_debris
---

# Crusher

{{ infobox(
    variants=[
        ["Wooden", "rngtech:wooden_crusher_chassis"],
        ["Iron", "rngtech:iron_crusher_chassis"],
        ["Copper", "rngtech:copper_crusher_chassis"],
        ["Gold", "rngtech:gold_crusher_chassis"],
        ["Bronze", "rngtech:bronze_crusher_chassis"],
        ["Steel", "rngtech:steel_crusher_chassis"],
        ["Aluminum", "rngtech:aluminum_crusher_chassis"],
        ["Titanium", "rngtech:titanium_crusher_chassis"],
        ["Tungstensteel", "rngtech:tungstensteel_crusher_chassis"],
        ["Exotic", "rngtech:exotic_crusher_chassis"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "0–8",
        "Power": "FE",
        "Gear": "{{ item('rngtech:iron_crush_head', 'Crush Head') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "Yes",
    },
) }}

The **Crusher** breaks ores and raw metals into crushed material, and crushed material into dust. Crushing an ore yields more metal than smelting it directly, and dust smelts faster and cheaper in the [Furnace](furnace.md). It also turns coal into Coal Dust and crops into biomass inputs for the [Bio Generator](bio-generator.md) and [Wooden Composter](wooden-composter.md).

Every Crusher runs on FE and needs a Crush Head installed. Like every RNGTech machine, each Crusher you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

You can read a Crusher from the outside. The installed Crush Head is the pair of toothed rolls in the open front, in the head's material; with no head you see only the bare shafts. The rolls turn toward each other while the Crusher works, faster when a craft is shorter, and grit of the input falls below them. Items in the input slot wait in the hopper on top and shake while they are crushed, and finished items collect in the bin at the bottom of the front. A jam stops the rolls with a shudder, wedges the input between them, puffs smoke, and blinks the lamp above the opening red. Facing the front, the Battery Cell lies in the cradle on the left with a charge meter beside it, and the motor sits on the right. Cables connect on any side, but the cable port is drawn on the back.

Each chassis has its own casing, and the rim of its hopper is the metal at the top of its recipe. The Iron, Copper, and Bronze Crushers stand on smooth stone. The Tungstensteel and Exotic Crushers, which crush several items per cycle, have a second, smaller pair of rolls under the first. The {{ item('rngtech:wooden_crusher_chassis') }} is a plank frame on wooden posts instead, with no lamp or meter; a Flint Crush Head shows as wooden rolls studded with flint.

## Obtaining

### Crafting

Each stage has its own recipe. You do not need the previous stage's Crusher to craft the next one, so you can keep a good older roll running beside a new chassis.

{{ crafting() }}

### Breaking

Mine a placed Crusher with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## Usage

### Crushing

Put items in the input slot and supply FE. The Crusher only runs with a Crush Head in its Gear tab. Each recipe uses a fixed amount of FE per craft; a faster Crusher finishes sooner but draws more FE per tick, so speed alone does not make crushing cheaper. Adding more of the same item keeps the current craft going, but swapping the input or the Crush Head for a different one mid-craft restarts it.

Typical recipes:

- A raw metal item gives 2 crushed material.
- An ore block gives 3 crushed material.
- Crushed material (except alloys) gives 1 dust in a second pass. This pass never gets bonus output, so Output Amount counts once per ore.
- Harder metals take longer to crush: Lead and Tin crush twice as fast as Iron, Titanium and Tungsten take three times as long, and Naquadah four times.
- Ingots crush back into dust, but these recipes never get bonus output.
- Coal gives {{ item('rngtech:coal_dust') }}, crops and plants give {{ item('rngtech:organic_reagent') }}, and seeds give {{ item('rngtech:compost_feedstock') }}.

### Hardness

Every recipe has a hardness level, and every Crush Head has a hardness level from 1 to 8. The Crusher still runs a recipe that is harder than its head, but for each missing level:

- the craft takes one extra recipe-length of time and costs one extra recipe's worth of FE,
- the craft has a 5% chance to jam when it starts, which pauses the Crusher for a short time without using the input. The chance is rolled once per craft, so waiting for power does not raise it,
- bonus output, Super Output, and salvage are switched off.

A Crush Head with **Jam Chance per Cycle**, such as the {{ item('rngtech:mineshaft_worn_pick_jaw') }}, can also jam on recipes at or below its hardness. Such a jam lasts as long as a one-level jam.

Recipes at hardness 7 or higher also cost twice the FE. Material recipes for Stage 5–8 metals cost 2×, 3×, 4×, and 6× the early FE baseline.

#### Jam Debris

A jam on a recipe of hardness 5 or higher drops one {{ item('rngtech:jam_debris') }} into the output slot, if the slot is empty or already holds Jam Debris. The jam still pauses the Crusher and keeps the input. Jam Debris is an ingredient of the [Volatile Catalyst](affix-forge.md#crafting-the-consumables), so to collect it, crush ore harder than your Crush Head on purpose and keep the output slot clear. Jam Chance and Jam Recovery change how fast it arrives.

| Crush Head | Stage | Hardness |
|---|---:|---:|
| {{ item('rngtech:flint_crush_head') }} | 0 | 1 |
| {{ item('rngtech:iron_crush_head') }} | 1 | 2 |
| {{ item('rngtech:copper_crush_head') }} | 2 | 2 |
| {{ item('rngtech:bronze_crush_head') }} | 3 | 3 |
| {{ item('rngtech:steel_crush_head') }} | 4 | 4 |
| {{ item('rngtech:aluminum_crush_head') }} | 5 | 5 |
| {{ item('rngtech:titanium_crush_head') }} | 6 | 6 |
| {{ item('rngtech:tungstensteel_crush_head') }} | 7 | 7 |
| {{ item('rngtech:exotic_crush_head') }} | 8 | 8 |

### Output amount

Output Amount sets how much extra crushed material an ore block or raw metal gives. Only that first crush counts: crushing crushed material into dust, or ingots back into dust, never gets bonus output.

- The chassis sets a base multiplier. Everything else adds to one **increased** total: the Crush Head's own yield, its rolled affixes, Crusher affixes, Mastery nodes, ascendancy nodes, and At-Level Output on recipes at the head's hardness.
- That total stops at **Maximum Bonus Output**, 75% to begin with. Bonuses adding up to +100% pay +75%, and the Stats tab shows how much is over the cap.
- Maximum Bonus Output can be raised, up to 90%. The **Overflow Gate** notable in [Machine Mastery](machine-mastery.md) adds +2%, and the jewel line **of Headroom** adds +1-2% per jewel.
- **Big yield affixes cost speed.** Crusher Jaws and Crush Head Pulverizing give the most Bonus Output, but each also rolls reduced Processing Speed, which slows every recipe. The Output Amount suffix is much smaller and has no penalty.
- Output Amount never drops below 1×, so a Crusher always makes at least the recipe's base output.
- Without a Battery Cell, the Crusher keeps only 75% of its bonus above 1×. A 1.4× Crusher drops to 1.3×, and a 1× Crusher loses nothing.

Output Amount above 1× is banked: each craft adds its fractional share to a bonus bar, and you get an extra item when the bar fills. The bar resets when you change the input item. Hover the bar to preview the next payout.

Super Output is a separate chance for one extra copy of the base output. On a Crusher it stops at 25%.

### Stages

| Stage | Crusher | Notes |
|---:|---|---|
| 0 | {{ item('rngtech:wooden_crusher_chassis') }} | Primitive body: weak buffer, slow, and uses more energy. |
| 1 | {{ item('rngtech:iron_crusher_chassis') }} | Baseline powered Crusher. |
| 2 | {{ item('rngtech:copper_crusher_chassis') }} | Better energy use, smaller buffer. |
| 3 | {{ item('rngtech:gold_crusher_chassis') }} | Copper numbers with one built-in [jewel socket](jewels.md#body-sockets). |
| 3 | {{ item('rngtech:bronze_crusher_chassis') }} | More output, higher energy use. |
| 4 | {{ item('rngtech:steel_crusher_chassis') }} | Durable and efficient but slower. Has Output Guard. |
| 5 | {{ item('rngtech:aluminum_crusher_chassis') }} | Fast and efficient, smaller buffer. |
| 6 | {{ item('rngtech:titanium_crusher_chassis') }} | High throughput and stronger output. Has Output Guard. |
| 7 | {{ item('rngtech:tungstensteel_crusher_chassis') }} | Heavy output body. Crushes up to 4 items per cycle. Has Output Guard. |
| 8 | {{ item('rngtech:exotic_crusher_chassis') }} | Endgame body. Crushes up to 9 items per cycle. Has Output Guard and a far larger internal buffer than any other chassis. |

Output Guard keeps progress for a while when the output slot is full instead of resetting it. Batching chassis process several items from the same input stack in one cycle; each extra item makes the cycle a little longer. A batch takes as many items as the input stack and the free output space allow, up to the batch size, and keeps that count until the cycle ends.

### Gear

The Gear tab has two slots:

- one **Crush Head** slot. A head is required. Its stage cannot be higher than the chassis stage, so a Wooden Crusher only takes a Flint Crush Head.
- one **Battery Cell** slot. Without a cell, the Crusher keeps only a tiny internal buffer and loses a quarter of its bonus output.

Crush Heads roll their own affixes, and their stats add to the Crusher's. See [Gear](gear.md) for how Gear works.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input slot. Accepts only items with a Crusher recipe; some rolled affixes also reject items that would not fit the output. |
| Bottom | Extracts finished output. |
| Sides | Inserts Battery Cells. |
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

### Mastery and Ascendancies

Each completed crushing job earns Crusher Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. Reversible ingot-to-dust recipes earn no XP. Some Crusher keystones change how Gear works, for example removing the Battery Cell slot or allowing a Crush Head one stage higher than the chassis. Crushers choose between the **Rockbreaker** and **Assayer** [ascendancies](machine-mastery.md#ascendancies).

### Crusher recipes

The Crusher uses its own recipe type, `rngtech:crusher`. The Processing level column is the recipe's hardness.

{{ processing("crusher") }}

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

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
| **Refiner’s Oath** | Deep notable | Sworn Yield | 20% increased Bonus Output. Batching is off. |
| Rich Assay | Small | Assay Ledger | 4% increased Bonus Output. |
| **Compound Yield** | Notable | Rich Assay | A bonus bank payout can also trigger Super Output. |
| Vein Sense | Small | Compound Yield | +1% Super Output. |
| **Mother Lode** | Deep notable | Vein Sense | Super Output Cadence fixed at 16 cycles. |
| Lucky Strike | Small | Assay Ledger | +1% Super Output. |
| **Tailings Recovery** | Notable | Lucky Strike | +3% Crusher Salvage. Salvage that does not fit the output is banked instead of lost. |
| True Measure | Small | Assay Ledger | +5% At-Level Output. |
| **Matched Hardness** | Notable | True Measure | +15% At-Level Output. |

<!-- ascendancy-trees:end -->

## Screen

The Crusher screen has six tabs:

- **Process**: input, output, energy, progress, the bonus output bar, and status squares for the recipe and Battery Cell. Hover the progress bar for time remaining, expected items per minute with your Gear and Mastery applied (chance bonuses such as Super Output are not counted), FE per tick, FE per craft, hardness penalties, and jam chance.
- **Gear**: Crush Head and Battery Cell slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery. Hold Shift over Bonus Output to see what your yield bonuses add up to, your Maximum Bonus Output, and how much is over the cap.
- **Refinement**: refine the placed Crusher's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.
- **Orders**: three drive bays for [Job Chips](work-orders.md), with each chip's job, progress, and the Activate, Pause, Rush, and Abandon buttons.

## Data values

{{ data_values() }}

## See also

- [Machine Stats](machine-stats.md)
- [Furnace](furnace.md)
- [Battery Cells](battery-cells.md)

{{ navbox() }}
