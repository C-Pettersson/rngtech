---
navigation:
  title: "Burst"
  parent: battery-chassis.md
  position: 2
  icon: rngtech:iron_battery_chassis
---

# Burst

The Copper and Gold chassis, and every chassis from Sparksteel up, can burst. A burst lets the cells give out FE faster than their rated output for a short time:

- When the chassis already gives its full rated output in a tick, it can give up to the rated output times its Burst Transfer. Gold has 2.5x and Aethergold has 4x.
- Each tick that gives more than the rated output uses one tick of the burst window. The window holds Burst Duration ticks.
- The window refills by one tick for each tick without a burst.
- Stability below 1.0 wastes part of the burst. At 0.80 Stability, a Gold chassis loses 20% of the extra FE. At 1.0 Stability or more, a burst wastes nothing.
- A burst does not make FE or capacity. It only empties the cells faster.
- Two chassis placed directly next to each other do not burst while they even out their charge. Banks that even out over a [Universal Cable](rngtech:universal-cable.md) can burst.

Hover the Burst row on the Process tab to see the burst output and the ticks left in the window. An attached connector still caps the burst.
