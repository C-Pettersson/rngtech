---
navigation:
  title: "Rotor speed and throttle"
  parent: cavitation-generator.md
  position: 0
  icon: rngtech:cavitation_generator
---

# Rotor speed and throttle

The rotor has to spin up before it makes power. While a cycle runs, it speeds up toward the throttle setting, taking about 3.5 seconds to reach rated speed (2,100 RPM). Below 900 RPM the Water does not cavitate: the generator makes no FE and the cycle does not advance, so spinning up costs time but no Water. When the generator stops, whether for lack of Water, a redstone signal, or a missing part, the rotor coasts down slowly, and it has to spin up again from wherever it slowed to. A steady Water supply keeps it at speed. Faster Gear (a higher Processing Speed) also spins the rotor up faster.

Set the throttle on the rotor speed gauge in the Process tab. Click a point on the gauge to jump there, scroll over it to move 1%, or hold Shift while scrolling to move 5%. The throttle runs from 40% to 100% in steps of 1% (30 RPM), and new generators start at the rated 70%. The table shows every tenth step.

| Throttle | Rotor speed | FE/t | Rotor wear | Heat strain | Water |
|---|---|---|---|---|---|
| 40% | 1,200 RPM | x0.25 | x0.19 | x0.01 | x0.57 |
| 50% | 1,500 RPM | x0.50 | x0.36 | x0.07 | x0.71 |
| 60% | 1,800 RPM | x0.75 | x0.63 | x0.29 | x0.86 |
| 70% (rated) | 2,100 RPM | x1.00 | x1.00 | x1.00 | x1.00 |
| 80% | 2,400 RPM | x1.12 | x1.49 | x2.91 | x1.14 |
| 90% | 2,700 RPM | x1.23 | x2.13 | x7.47 | x1.29 |
| 100% | 3,000 RPM | x1.35 | x2.92 | x17.35 | x1.43 |

The rotor pushes Water through the chamber in proportion to its speed, so each 1,000 mB cycle finishes faster at a higher throttle and slower at a lower one. At rated speed a cycle takes 12 seconds, which is 5 buckets per minute. Rated speed gets the most FE out of each bucket; every other throttle gets a little less, and low throttles much less.

Below the rated speed, each rotor lasts longer and makes more FE over its life, but makes less FE each tick and less FE per bucket, while using less Water per minute. Above it, the generator supercavitates: more FE per tick, but the rotor wears out much faster and heat climbs steeply. A generator cools 1 heat strain per tick, plus half its Heat Core's stage (a Titanium Heat Core cools 4 per tick). When the selected throttle makes more heat than that, strain builds until the strain penalty eats the extra FE. Your Heat Core and Temperature Stability therefore set the highest throttle you can hold: without a Heat Core a Steel setup holds 80%, a Steel Heat Core lets it hold 90%, and full throttle needs an Exotic Heat Core or a Titanium one backed by extra Temperature Stability. Faster rotors and Servos run shorter cycles, so they heat up faster too.

Hovering over the gauge shows the current speed, how fast the rotor is wearing right now (including resonance), the FE and heat multipliers for the selected throttle, and its heat strain per tick against your cooling, in red when the generator cannot hold that throttle. The wear column in the table is the off-resonance rate.
