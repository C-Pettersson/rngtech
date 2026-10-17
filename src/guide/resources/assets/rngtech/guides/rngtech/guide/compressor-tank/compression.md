---
navigation:
  title: "Compression"
  parent: compressor-tank.md
  position: 0
  icon: rngtech:steel_compressor_tank
---

# Compression

On a Compressor Tank, fluid first goes into the **loose** tank. With a valid Battery Cell, at least one Servo, and FE, the tank moves loose fluid into the **compressed** store. Compressed fluid still counts as the same amount of ordinary fluid. The Process tab also shows the smaller physical volume it takes up, so a Steel tank holding 100 buckets compressed shows about 10 buckets.

When you drain the tank, it empties loose fluid first. Draining compressed fluid needs the same Gear and FE, and costs a quarter of the FE it took to compress it. It runs at the same rate limit as compression. Without power, compressed fluid simply stays put: it never leaks, vanishes, or gets voided.

Idle compressed storage costs no FE.

FE you feed the tank fills its small internal buffer first, then charges the Battery Cell. Compression spends the buffer first, then draws from the cell. The tank sets no intake limit of its own, so only the cell, your cable, and the source limit charging.

Compression speed is the base rate times the Servo bonus, raised further by speed and fluid transfer affixes.
