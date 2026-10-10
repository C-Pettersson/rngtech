---
wiki:
  category: Processing
  icon: rngtech:iron_resonance_calibrator_chassis
  ids:
    - rngtech:iron_resonance_calibrator_chassis
    - rngtech:copper_resonance_calibrator_chassis
    - rngtech:steel_resonance_calibrator_chassis
    - rngtech:titanium_resonance_calibrator_chassis
    - rngtech:lead_resonance_calibrator_chassis
    - rngtech:tungstensteel_resonance_calibrator_chassis
    - rngtech:nullite_resonance_calibrator_chassis
---

# Resonance Calibrator

{{ infobox(
    variants=[
        ["Iron", "rngtech:iron_resonance_calibrator_chassis"],
        ["Copper", "rngtech:copper_resonance_calibrator_chassis"],
        ["Steel", "rngtech:steel_resonance_calibrator_chassis"],
        ["Lead", "rngtech:lead_resonance_calibrator_chassis"],
        ["Titanium", "rngtech:titanium_resonance_calibrator_chassis"],
        ["Tungstensteel", "rngtech:tungstensteel_resonance_calibrator_chassis"],
        ["Nullite", "rngtech:nullite_resonance_calibrator_chassis"],
    ],
    fields={
        "Type": "Processing machine",
        "Stages": "1, 2, 4, 6, 7",
        "Power": "FE",
        "Gear": "{{ item('rngtech:iron_resonance_coil', 'Resonance Coil') }}, {{ item('rngtech:iron_control_board', 'Control Board') }}, {{ item('rngtech:iron_stabilizer_matrix', 'Stabilizer Matrix') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:structural_calibration_pattern', 'Calibration Patterns') }}",
        "Mastery": "Yes",
    },
) }}

The **Resonance Calibrator** turns ordinary plates, gears, coils, casings, and circuits into calibrated components. Stage 3 and higher RNGTech machines need calibrated components in their crafting recipes, so the Calibrator is the gate between early and mid-game progression. Any machine, including ones from other mods, can make the raw parts, but only the Calibrator can calibrate them.

Each calibrated component records a family, a stage, a stability score from 0 to 100, and sometimes Refinement Potential. Recipes that use it can demand a minimum of each. Like every RNGTech machine, each Calibrator rolls its own [rarity and affixes](rarity-and-affixes.md) and levels up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

Each chassis has its own recipe, and none of them use up an older Calibrator, so a well-rolled early chassis stays useful. Steel and higher chassis need calibrated components of a given family, stage, and stability, shown in the Notes column.

{{ crafting() }}

### Breaking

Mine a placed Resonance Calibrator with a pickaxe. It drops itself and keeps its rolled traits, Mastery progress, and ascendancy.

## Usage

### Calibrating

1. Install a Resonance Coil and a Control Board on the Gear tab.
2. Put the reusable Calibration Patterns in the Gear tab's pattern slots and select the one for the family you want.
3. On the Process tab, put in the raw input, its catalyst, and the recipe stabilizer if the recipe uses one.
4. Supply FE.

Each job uses the input and the catalyst. The stabilizer is used up when a recipe has one. Patterns are never used up. The output's stability is rolled within the recipe's range; your Control Board, Stabilizer Matrix, and the chassis improve the roll.

A recipe runs only if both the chassis stage and the installed Resonance Coil's stage reach the recipe's required stage. When a recipe lists no required stage, it needs the stage of its output.

If a component rolls too low for the recipe you need, put it in a [Component Recycler](component-recycler.md) to get its raw input back and try again. You lose the catalyst, the stabilizer, and the Refinement Potential. Calibrated Conductive Components always recycle into a Copper Coil, even ones made from a Sparksteel Coil.

Only the Sparksteel Coil and Calibrated Diamond Crystal recipes can give Super Output. Every other recipe makes exactly one component per job, so calibrating and recycling never gains you items.

### Calibration families

There are six patterns, one per family: {{ item('rngtech:structural_calibration_pattern') }}, {{ item('rngtech:kinetic_calibration_pattern') }}, {{ item('rngtech:thermal_calibration_pattern') }}, {{ item('rngtech:conductive_calibration_pattern') }}, {{ item('rngtech:storage_calibration_pattern') }}, and {{ item('rngtech:logic_calibration_pattern') }}. The Logic family also makes the {{ item('rngtech:calibrated_diamond_crystal') }}, a Stage 6 crystal needed for the Stage 7 chassis and late Stabilizer Matrices.

The Stage 5 Kinetic recipe from a Sparksteel Gear rolls a low, wide stability range on purpose. The [Volatile Catalyst](affix-forge.md#crafting-the-consumables) needs one between 20% and 35%, which a well-built Calibrator overshoots.

Several families have a "resonant" recipe that adds a Matrix and a Stabilization Catalyst. It needs Stage 4 reach and gives a higher and tighter stability range, for recipes that demand well-calibrated parts.

### Stages

| Stage | Chassis | Lanes | Notes |
|---:|---|---:|---|
| 1 | {{ item('rngtech:iron_resonance_calibrator_chassis') }} | 1 | First Calibrator. Reaches the Stage 2 parts needed for Steel. |
| 2 | {{ item('rngtech:copper_resonance_calibrator_chassis') }} | 1 | Optional faster sidegrade with weaker stability. |
| 4 | {{ item('rngtech:steel_resonance_calibrator_chassis') }} | 1 | Slower but more stable. |
| 4 | {{ item('rngtech:lead_resonance_calibrator_chassis') }} | 2 | Two-lane bulk sidegrade with lower precision. |
| 6 | {{ item('rngtech:titanium_resonance_calibrator_chassis') }} | 1 | Precision chassis with better speed, stability, and Refinement Potential. |
| 7 | {{ item('rngtech:tungstensteel_resonance_calibrator_chassis') }} | 3 | Three-lane late-game bulk branch. |
| 7 | {{ item('rngtech:nullite_resonance_calibrator_chassis') }} | 1 | Late-game precision branch with the best quality and Refinement Potential. |

A multi-lane chassis calibrates that many copies of the input per cycle, each paying its own FE. A cycle runs only as many copies as the input, catalyst, and stabilizer stacks allow. All copies from one cycle share the same stability and Refinement Potential roll.

### Gear

The Gear tab has:

- one **Resonance Coil** slot (required). The coil's stage sets which recipes you can reach and improves quality.
- one **Control Board** slot (required). Improves precision, stability, and Refinement Potential outcomes.
- one **Stabilizer Matrix** slot (optional). Raises the stability floor and can save catalysts.
- one **Battery Cell** slot (optional). Without a cell, the Calibrator keeps only a small internal buffer and runs slower with lower quality.
- six **Calibration Pattern** slots, one per family, with one selected as active.

Coils, Control Boards, and Stabilizer Matrices come in Iron, Copper, Steel, Titanium, Tungstensteel, and Nullite.

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert into the input, catalyst, and stabilizer slots. Each slot accepts only items a calibration recipe uses there. |
| Bottom | Extracts finished components. |
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

Gear and pattern slots are not reachable by automation.

With JEI installed, recipes are listed under Resonance Calibration. Recipe transfer puts the pattern into the selected pattern slot if it is empty or already holds that pattern, otherwise into a slot holding it or the first empty one.

### Mastery and Ascendancies

Each completed calibration earns Mastery XP; higher-stage recipes earn more. Multi-lane chassis earn XP for every job in a cycle. Calibrators start from the Control start of the shared [Machine Mastery](machine-mastery.md) tree, which they share with the [Metal Press](metal-press.md). They choose between the **Harmonist**, which rewards long runs of the same family with a rising stability floor, and the **Mass Tuner**, which makes multi-lane chassis cheaper to run. See [Resonance Calibrator ascendancies](machine-mastery.md#ascendancies).

Extra components from Super Output earn no XP. Changing your Mastery allocation restarts the current calibration, but keeps your items and patterns.

### Calibration recipes

The Resonance Calibrator uses the `rngtech:calibration` recipe type. Hover an output for its stage, stability range, and Refinement Potential range. A blank Min. stage means the recipe needs its output's stage.

{{ processing("calibration", columns=["processing_ticks", "energy", "minimum_stage", "machine_xp"]) }}

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

<!-- ascendancy-trees:start -->

### Harmonist

| Node | Type | After | Effect |
|---|---|---|---|
| **Resonant Streak** | Root | — | +1 Streak Floor; +10 Streak Cap. |
| Held Note | Small | Resonant Streak | +2 Streak Cap. |
| **Sustained Tone** | Notable | Held Note | +8 Streak Cap. |
| Long Note | Small | Sustained Tone | +2 Streak Cap. |
| **Master Harmonic** | Deep notable | Long Note | 30% less Processing Speed. At the full streak, every 8th calibration comes out at exactly 100 stability. |
| Quiet Coil | Small | Resonant Streak | 5% reduced Energy Use. |
| **Second Pass** | Notable | Quiet Coil | A calibration under 40 stability is re-run once, paying FE and a catalyst again but not the input. |
| Soft Reset | Small | Second Pass | 5% reduced Energy Use. |
| **Pattern Memory** | Deep notable | Soft Reset | The streak survives one pattern or family change, and breaking the machine. |
| Clean Line | Small | Resonant Streak | 8% increased Stability. |
| **Clear Signal** | Notable | Clean Line | At the full streak, the stability ceiling rises by 5. |
| Quick Ear | Small | Resonant Streak | 8% increased Processing Speed. |
| **Perfect Pitch** | Notable | Quick Ear | 20% increased Calibration Precision. |

### Mass Tuner

| Node | Type | After | Effect |
|---|---|---|---|
| **Shared Field** | Root | — | One catalyst per cycle however many lanes run; the stability ceiling drops by 10. |
| Brisk Field | Small | Shared Field | 8% increased Processing Speed. |
| **Stabilizer Economy** | Notable | Brisk Field | Recipe stabilizers are consumed every other cycle. |
| Tight Field | Small | Shared Field | 8% increased Calibration Precision. |
| **Lane Sync** | Notable | Tight Field | 15% more Processing Speed while more than one lane runs. |
| Focused Array | Small | Lane Sync | 8% increased Calibration Precision. |
| **Resonance Array** | Deep notable | Focused Array | 100% increased Batch Size. The stability ceiling drops by another 15. |
| Lean Field | Small | Shared Field | 5% reduced Energy Use. |
| **Catalytic Surplus** | Notable | Lean Field | 25% increased Catalyst Efficiency. |
| Broad Field | Small | Shared Field | 8% increased Stability. |
| **Wide Tolerance** | Notable | Broad Field | +5 stability floor on a multi-lane chassis. |
| Steady Reach | Small | Wide Tolerance | 8% increased Stability. |
| **Overreach** | Deep notable | Steady Reach | +1 Coil Reach; 20% less Calibration Precision. |

<!-- ascendancy-trees:end -->

## Screen

The Resonance Calibrator screen has five tabs:

- **Process**: input, catalyst, stabilizer, and output slots, progress, and energy. With the Harmonist's Resonant Streak, the progress tooltip shows your current stability-floor bonus.
- **Gear**: Resonance Coil, Control Board, Stabilizer Matrix, Battery Cell, and pattern slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Calibrator's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Data values

{{ data_values() }}

## See also

- [Component Recycler](component-recycler.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
