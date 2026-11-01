---
navigation:
  title: "Automation"
  parent: resonance-calibrator.md
  position: 5
  icon: rngtech:iron_resonance_calibrator_chassis
---

# Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert into the input, catalyst, and stabilizer slots. Each slot accepts only items that a recipe for the selected pattern and the current input uses there. |
| Bottom | Extracts finished components. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear and pattern slots are not reachable by automation.

With JEI installed, recipes are listed under Resonance Calibration. Recipe transfer puts the pattern into the selected pattern slot if it is empty or already holds that pattern, otherwise into a slot holding it or the first empty one.
