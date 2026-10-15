---
navigation:
  title: "Stoke"
  parent: solid-fuel-burner.md
  position: 1
  icon: rngtech:crude_solid_fuel_burner
---

# Stoke

Stoke sets how hard the fire is driven. Set it from 25% to 100% by clicking or scrolling on the stoke gauge on the Process tab (hold Shift to step by 5). New burners start at 100%, which runs at the Heat Core's full FE/t. Stoke can never make a burner faster than that.

Turning the stoke down lowers FE/t in proportion, but the slower burn gets more FE from each fuel item:

| Stoke | FE/t | FE per fuel item |
|---|---:|---:|
| 100% (rated, default) | ×1.00 | ×1.00 |
| 75% | ×0.75 | ×1.07 |
| 50% | ×0.50 | ×1.13 |
| 25% | ×0.25 | ×1.20 |

Run at full stoke when you need the power, and turn it down to stretch your fuel when demand is low or your fuel supply is slow. Changing the stoke while an item burns is fair both ways: the part of the item already burned keeps its value, and the rest is worth what the new stoke gives. Hover the gauge to see the FE/t and the FE per fuel item at the current setting.
