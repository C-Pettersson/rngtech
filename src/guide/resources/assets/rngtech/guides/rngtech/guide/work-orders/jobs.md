---
navigation:
  title: "Jobs"
  parent: work-orders.md
  position: 1
  icon: rngtech:job_chip
---

# Jobs

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
- **Stat goals:** A stat goal asks the machine to reach a value of one of its stats. The value includes traits, [Gear](rngtech:gear.md), Mastery, and the chip's conditions. The machine must finish a job while the stat is at or above the target. Installing the right Gear is not enough on its own: the machine has to run. The Orders tab shows the value at the last finished job against the target.

Jobs by machine:

| Machine | Jobs |
|---|---|
| [Crusher](rngtech:crusher.md) | Any items. Items needing a Processing Level. Named items. Stat goals for Processing Speed, Super Output Chance, and Output Amount. |
| [Furnace](rngtech:furnace.md) | Any items. Items of a stage. A heat slider setting. Named items. Stat goals for Heat Transfer, Temperature Stability, Processing Speed, and Super Output Chance. |
| [Alloy Furnace](rngtech:alloy-furnace.md) | Any alloys. Recipes of a stage. A heat slider setting. Named alloys. |
| [Metal Press](rngtech:metal-press.md) | Any presses. Metal of a stage. A heat slider setting. Named outputs. Plates, gears, casings, or connectors of any metal. Streaks without a failure. |
| [Melter](rngtech:melter.md) | Any fluids. A heat slider setting. Named fluids. Counts are in mB. |
| [Resonance Calibrator](rngtech:resonance-calibrator.md) | Any calibrations. Recipes of a stage. A stability. Named components. A calibration family, such as Logic or Thermal. Streaks at a stability. |
| [Forestry Companion](rngtech:forestry-cart-station.md) | Logs. Crops. Logs cut with a cutting head of a stage. |
| [Component Assembler](rngtech:component-assembler.md) | Any assemblies. Battery Cells of a stage. Named outputs. |
| [Component Recycler](rngtech:component-recycler.md) | Any parts. Parts of a stage. |

These do not take chips: generators, the [Battery Chassis](rngtech:battery-chassis.md), the [Miner's Companion](rngtech:miners-companion.md), and the Crude Component Recycler.
