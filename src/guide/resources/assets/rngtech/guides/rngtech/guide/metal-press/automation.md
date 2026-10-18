---
navigation:
  title: "Automation"
  parent: metal-press.md
  position: 4
  icon: rngtech:metal_press
---

# Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe input. Rejects items no Mold can press. |
| Bottom | Extracts finished output. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md), and Nitrogen for the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). With Bright Anneal or Hydrogen Quench, also accepts Hydrogen for [annealing](rngtech:metal-press/annealing.md). |

Gear slots, Molds, and the Refinement slot cannot be filled by automation.
