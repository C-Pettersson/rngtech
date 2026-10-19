---
navigation:
  title: "Automation"
  parent: melter.md
  position: 2
  icon: rngtech:melter
---

# Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe items. Rejects items no Melter recipe uses, and never lets one ingredient fill the slot the other one needs; see [Automation](rngtech:automation.md). |
| Bottom | Extracts filled output containers. Ingredient leftovers are pushed into the inventory below; see [Automation](rngtech:automation/ingredient-leftovers.md). |
| Sides | Insert fluid containers and fill the input tank. With a Fluid Pump installed, sides also drain the output tank. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear slots and the Refinement slot cannot be filled by automation.
