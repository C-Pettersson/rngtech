---
navigation:
  title: "Output amount"
  parent: crusher.md
  position: 2
  icon: rngtech:iron_crusher_chassis
---

# Output amount

Output Amount sets how much extra crushed material an ore block or raw metal gives. Only that first crush counts: crushing crushed material into dust, or ingots back into dust, never gets bonus output.

- The chassis sets a base multiplier. Everything else adds to one **increased** total: the Crush Head's own yield, its rolled affixes, Crusher affixes, Mastery nodes, ascendancy nodes, and At-Level Output on recipes at the head's hardness.
- That total has diminishing returns. Bonuses adding up to +50% give about +33% more output, +100% gives +50%, and the result never reaches +100%.
- **Big yield affixes cost speed.** Crusher Jaws and Crush Head Pulverizing give the most Bonus Output, but each also rolls reduced Processing Speed, which slows every recipe. The Output Amount suffix is much smaller and has no penalty.
- Output Amount never drops below 1×, so a Crusher always makes at least the recipe's base output.
- Without a Battery Cell, the Crusher keeps only 75% of its bonus above 1×. A 1.4× Crusher drops to 1.3×, and a 1× Crusher loses nothing.

Output Amount above 1× is banked: each craft adds its fractional share to a bonus bar, and you get an extra item when the bar fills. The bar resets when you change the input item. Hover the bar to preview the next payout.

Super Output is a separate chance for one extra copy of the base output. On a Crusher it stops at 25%.
