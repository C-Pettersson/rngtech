---
wiki:
  category: Guides
  icon: rngtech:job_chip
  ids:
    - rngtech:dispatch_terminal
    - rngtech:job_chip
    - rngtech:completed_job_chip
    - rngtech:requisition
---

# Work Orders

{{ infobox(
    variants=["rngtech:dispatch_terminal"],
    fields={
        "Type": "Job vendor",
        "Power": "None",
        "Listings": "6 jobs",
        "Refresh": "{{ item('rngtech:requisition') }}",
    },
) }}

**Work Orders** are repeatable jobs for your processing machines. A **Dispatch Terminal** lists six jobs, each on a **Job Chip**. Put a chip in the right kind of machine and run the machine until the job is done. Then redeem the chip for the reward it shows.

The terminal rolls each job when it lists it, so a chip shows everything before you take it:

- The machine it is for
- The job and its tier
- The chassis stage it needs
- Its conditions
- Its reward

Work Orders give a machine goals after its Mastery is maxed. They give Stage 7 and 8 chassis jobs that lower stages cannot take. They also give goals to machines without [Mastery](machine-mastery.md), such as the [Component Assembler](component-assembler.md).

## Obtaining

### Crafting

{{ crafting("rngtech:dispatch_terminal", "rngtech:requisition") }}

Each recipe needs a Calibrated Logic Component from the [Resonance Calibrator](resonance-calibrator.md):

- The Dispatch Terminal needs a component with 80 or more stability. Only the Stage 4 Calibrator's resonant recipe reaches that.
- The Requisition needs a component with 65 or more stability. One craft makes two Requisitions.

Job Chips and Completed Job Chips have no recipe. Chips come from a Dispatch Terminal, and some quests give them out.

### Breaking

Mine a Dispatch Terminal with a pickaxe. It keeps its job list on the dropped item, empty lines included. Placing it again does not refill the empty lines.

## Usage

### The Dispatch Terminal

A new terminal fills its six lines the first time you open it. Each line shows the machine, chassis stage, job, and reward. Hover over a line to see the chip's full tooltip.

- **Download** moves the job into your inventory as a Job Chip. It is free, and the line stays empty.
- **Refresh** uses one {{ item('rngtech:requisition') }} from your inventory. It replaces all six lines with new jobs, including jobs you did not download.

One terminal lists jobs of every tier for every machine. The six lamps on its face show how many jobs are left.

### Tiers

A job's tier, I to V, sets the job's size, the chassis stage it needs, and the reward's size. Some machines stop below a tier's stage. These machines need only their top stage.

| Tier | Chassis stage | Job size | Rush time |
|---|---:|---|---:|
| I | 4 | About 15 minutes on a good Stage 4 build | 7:30 |
| II | 5 | About 20 minutes on a good Stage 5 build | 10:00 |
| III | 6 | About 25 minutes on a good Stage 6 build | 12:30 |
| IV | 7 | About 30 minutes on a good Stage 7 build | 15:00 |
| V | 8 | About 30 minutes on a good Stage 8 build | 15:00 |

| Machine | Top stage |
|---|---:|
| [Metal Press](metal-press.md) | 4 |
| [Forestry Companion](forestry-cart-station.md) | 4 |
| [Alloy Furnace](alloy-furnace.md) | 6 |
| [Melter](melter.md) | 6 |
| [Resonance Calibrator](resonance-calibrator.md) | 7 |

The [Component Assembler](component-assembler.md) has no chassis stages, so its chips need none.

### Jobs

A job has one to three parts, and it is done when every part is done. The terminal rolls each count from a range for the tier. Two jobs of the same tier are similar in size but rarely identical. A count near the top of its range pays a little more.

| Kind | Example |
|---|---|
| Named items | Crush 96 Raw Tin and 64 Raw Lead |
| Any work of a stage | Crush 256 items needing Processing Level 6+ |
| Any work | Smelt 768 items |
| A machine setting | Smelt 192 items with the heat slider at 50%+ |
| A kind of output | Press 96 plates and 48 gears |
| A streak | Calibrate 12 components in a row at 80+ stability |
| A stat goal | Reach a Processing Speed of 3.1 on a Crusher |

Only work the machine finishes while the chip is active counts. A batch counts every item it finishes.

- **Stage jobs:** Hold Shift on the chip, or on its row in the Orders tab, to see the items that count. Furnace and Metal Press chips list materials instead.
- **Streaks:** A streak counts work in a row. A failure resets it to 0, and so does work below the streak's setting. The chip remembers its best streak.
- **Stat goals:** A stat goal asks the machine to reach a value of one of its stats. The value includes traits, [Gear](gear.md), Mastery, and the chip's conditions. The machine must finish a job while the stat is at or above the target. Installing the right Gear is not enough on its own: the machine has to run. The Orders tab shows the value at the last finished job against the target.

Jobs by machine:

| Machine | Jobs |
|---|---|
| [Crusher](crusher.md) | Any items. Items needing a Processing Level. Named items. Stat goals for Processing Speed, Super Output Chance, and Output Amount. |
| [Furnace](furnace.md) | Any items. Items of a stage. A heat slider setting. Named items. Stat goals for Heat Transfer, Temperature Stability, Processing Speed, and Super Output Chance. |
| [Alloy Furnace](alloy-furnace.md) | Any alloys. Recipes of a stage. A heat slider setting. Named alloys. |
| [Metal Press](metal-press.md) | Any presses. Metal of a stage. A heat slider setting. Named outputs. Plates, gears, casings, or connectors of any metal. Streaks without a failure. |
| [Melter](melter.md) | Any fluids. A heat slider setting. Named fluids. Counts are in mB. |
| [Resonance Calibrator](resonance-calibrator.md) | Any calibrations. Recipes of a stage. A stability. Named components. A calibration family, such as Logic or Thermal. Streaks at a stability. |
| [Forestry Companion](forestry-cart-station.md) | Logs. Crops. Logs cut with a cutting head of a stage. |
| [Component Assembler](component-assembler.md) | Any assemblies. Battery Cells of a stage. Named outputs. |
| [Component Recycler](component-recycler.md) | Any parts. Parts of a stage. |

These do not take chips: generators, the [Battery Chassis](battery-chassis.md), the [Miner's Companion](miners-companion.md), and the Crude Component Recycler.

### Drive bays and the Orders tab

Each machine in the table has an **Orders** tab with three **drive bays**. To load a chip, put it in a bay. While the tab is open, you can also Shift-click a chip in your inventory to put it in the next empty bay.

A bay refuses:

- A chip for a different kind of machine
- A chip that needs a higher chassis stage
- A chip that needs a higher Mastery level
- A blank chip
- A Rush chip with no time left

A machine works on one chip at a time, the **active** chip. If no chip is active, the first chip you load becomes active. A small blue lamp on the machine lights while it has an active chip. Click a chip's row to select it, then use these buttons:

- **Activate:** Makes the selected chip active. The previously active chip pauses and keeps its progress.
- **Pause:** Stops the active chip. It keeps its progress.
- **Rush:** Makes the selected chip active with a time limit. See [Rush](#rush).
- **Abandon:** Shift-click to destroy the chip. You get nothing back.

A chip you take out of its bay keeps its progress, which its tooltip shows. When you load it again, here or in another machine of the same kind, it carries on from there. Taking out the active chip leaves the machine with no active chip.

When the active job is done, its chip turns into a {{ item('rngtech:completed_job_chip') }} in the same bay. It is no longer active, and no other chip becomes active on its own. Take it out and use it to get its reward.

A broken machine keeps its chips and loads them again when you place it. The Forestry Companion keeps its chips on the cart item.

With Jade installed, look at a machine to see its active job, its progress, and any Rush time left. Hold Shift to see how many chips it holds and how many are done.

### Conditions

A chip has a [rarity](rarity-and-affixes.md#rarity), like Gear:

| Rarity | Chance | Conditions |
|---|---:|---:|
| Normal | 50% | 0 |
| Magic | 35% | 1 or 2 |
| Rare | 15% | 3 or 4 |

Conditions are affixes. A prefix lowers one of the machine's stats, and a suffix adds a rule to the job. Each condition adds Bounty, which makes the reward bigger. Rule conditions also add Prestige, which unlocks rarer rewards.

Conditions apply only while their chip is active. A stat breakdown lists each one's cost, such as "Work Order: Starved Feed".

| Condition | Effect while active | Reward | Machines |
|---|---|---|---|
| Starved Feed | 15%–30% less Processing Speed | +15% to +30% Bounty | All |
| Power Surcharge | 25%–50% increased Energy Usage | +10% to +25% Bounty | All except the Forestry Companion |
| Short Crew | 1 less Batch Size | +30% Bounty | Crusher, Furnace, Alloy Furnace, Metal Press, Melter, Resonance Calibrator |
| Cold Start | 10%–20% reduced Temperature Stability | +15% to +25% Bounty | Furnace, Alloy Furnace, Metal Press |
| Unsteady Hands | 10%–20% reduced Stability | +15% to +25% Bounty | Metal Press, Resonance Calibrator |
| of Hand Feeding | Instant Process does not occur | +20% Bounty | Crusher |
| of Brittle Lining | A heat failure resets the job's progress to 0 | +35% Bounty, +1 Prestige | Furnace, Alloy Furnace, Metal Press |
| of the Jammed Line | A jam resets the job's progress to 0 | +30% Bounty, +1 Prestige | Crusher |
| of Fine Tolerance | A calibration below the job's stability resets its progress to 0 | +35% Bounty, +1 Prestige | Resonance Calibrator stability jobs |
| of Deep Ore | Only work one level above the job's minimum counts | +25% Bounty, +1 Prestige | Stage jobs |

A higher-tier condition costs more and pays more. A chip only rolls conditions its job can use.

### Refining and corrupting chips

You can refine a Job Chip that has a job. It has its own [Refinement Potential](rarity-and-affixes.md#refinement-potential). In the [Affix Forge](affix-forge.md), you can:

- Add a condition for a bigger reward.
- Remove a condition to make the job easier.
- Upgrade or reroll its conditions.
- Raise its rarity.

The chip's Bounty and Prestige follow its conditions, and its reward grows or shrinks to match. You cannot refine a chip in a drive bay, so take it out first.

A {{ item('rngtech:volatile_catalyst') }} corrupts a chip, with the usual [corruption](rarity-and-affixes.md#corruption) odds. Every outcome makes the chip Corrupted, and you cannot refine a Corrupted chip again.

| Outcome | Effect on a chip |
|---|---|
| Untouched | Nothing else changes. |
| Blessed | One of three: the reward goes up by 50%, the chip gains 1 Prestige, or the chip gains a condition that adds reward at no cost. |
| Reforged | Rerolls the conditions at the chip's rarity. The reward changes to match. |
| Warped | Rerolls each condition's value within its tier. The reward stays the same. |
| Blighted | Adds a condition that costs the machine but adds no reward. |

The [Phase Array](phase-array.md) does not accept Job Chips.

### Rush

Rush is an optional time limit for a bigger reward. To use it:

1. Click the chip's row to select it.
2. Hover over **Rush** to see the time limit.
3. Click **Rush**.

The chip becomes active with half its tier's target time on the clock (see [Tiers](#tiers)). Its reward goes up by 50%, rounded up.

The clock never stops. It keeps running while the chip is paused, out of its bay, in your inventory, or in an unloaded chunk. If time runs out, you lose the chip and get nothing. A finished chip cannot expire. You cannot Rush a chip twice.

### Rewards

Each job pays one kind of reward, shown on the chip, so you choose jobs by what they pay. The tier sets the base amount. Count ranges, conditions, Rush, and corruption add Bounty, which raises the amount.

| Reward | Tiers |
|---|---|
| {{ item('rngtech:chaos_crystal', 'Chaos Crystals') }} | All |
| {{ item('rngtech:expansion_crystal', 'Expansion Crystals') }} | All |
| {{ item('rngtech:null_crystal', 'Null Crystals') }} | All |
| {{ item('rngtech:affix_modifier', 'Affix Upgraders') }} | All |
| {{ item('rngtech:mastery_refund', 'Mastery Refunds') }} | All |
| {{ item('rngtech:requisition', 'Requisitions') }} | All |
| A {{ item('rngtech:glyph') }} of the job's tier | All |
| {{ item('rngtech:volatile_catalyst', 'Volatile Catalysts') }}, {{ item('rngtech:affix_upgrade', 'Greater Affix Upgraders') }}, or an {{ item('rngtech:ascension_matrix') }} | III and higher |
| An unidentified [Unique](uniques.md) | Prestige 2 or more |

Rewards never include FE, ores, ingots, or other materials. Tier V jobs are the only source of Tier V Glyphs for the [Phase Array](phase-array.md).

## Screen

- **Dispatch Terminal:** Six job lines, each with its machine, chassis stage, job, reward, and a Download button. The Refresh button and your Requisition count sit below them.
- **Orders tab:** Three drive bays, with your inventory below. Each bay's row shows the chip's tier, job, progress per part, best streak, and Rush time. Activate, Pause, Rush, and Abandon act on the selected row.

## Data values

{{ data_values() }}

## See also

- [Machine Mastery](machine-mastery.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- [Affix Forge](affix-forge.md)
- [Phase Array](phase-array.md)

{{ navbox() }}
