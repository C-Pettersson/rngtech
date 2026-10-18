---
navigation:
  title: "Automation"
  parent: alloy-furnace.md
  position: 3
  icon: rngtech:bronze_alloy_furnace_chassis
---

# Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the active input slots. Accepts only Alloy Furnace recipe ingredients, and never lets one ingredient fill a slot another ingredient needs; see [Automation](rngtech:automation.md). |
| Bottom | Extracts finished output. Ingredient leftovers are pushed into the inventory below; see [Automation](rngtech:automation/ingredient-leftovers.md). |
| Sides | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |
| Any | Accepts Nitrogen for the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). |

Gear slots cannot be filled by automation.
