---
navigation:
  title: "The collapse curve"
  parent: vacuum-collapse-generator.md
  position: 1
  icon: rngtech:vacuum_collapse_generator
---

# The collapse curve

A collapse does not make its FE evenly. Output starts low and climbs to a peak at the very end of the cycle. At rated depth it starts at 0.4 times the collapse's average FE/t and ends at twice the average. Every collapse still makes all of its FE, but your connector and storage have to take the peak, or the part they cannot take is lost.

The Process tab draws the curve above the progress bar. The lit part is what the running collapse has already made, and the faint line marks its average. Hover it to see the starting and peak FE/t. While a collapse runs, the generation icon shows the FE/t at that moment.
