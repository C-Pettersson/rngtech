---
navigation:
  title: "Calibrating"
  parent: resonance-calibrator.md
  position: 0
  icon: rngtech:iron_resonance_calibrator_chassis
---

# Calibrating

1. Install a Resonance Coil and a Control Board on the Gear tab.
2. Put the reusable Calibration Patterns in the Gear tab's pattern slots and select the one for the family you want.
3. On the Process tab, put in the raw input, its catalyst, and the recipe stabilizer if the recipe uses one.
4. Supply FE.

Each job uses the input and the catalyst. The stabilizer is used up when a recipe has one. Patterns are never used up. The output's stability is rolled within the recipe's range; your Control Board, Stabilizer Matrix, and the chassis improve the roll.

A recipe runs only if both the chassis stage and the installed Resonance Coil's stage reach the recipe's required stage. When a recipe lists no required stage, it needs the stage of its output.

If a component rolls too low for the recipe you need, put it in a [Component Recycler](rngtech:component-recycler.md) to get its raw input back and try again. You lose the catalyst, the stabilizer, and the Refinement Potential. Calibrated Conductive Components always recycle into a Copper Coil, even ones made from a Sparksteel Coil.

Only the Sparksteel Coil and Calibrated Diamond Crystal recipes can give Super Output. Every other recipe makes exactly one component per job, so calibrating and recycling never gains you items.
