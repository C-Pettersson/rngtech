---
navigation:
  title: "Automation"
  parent: compressor-tank.md
  position: 3
  icon: rngtech:steel_compressor_tank
---

# Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert fluid containers. |
| Bottom | Extracts filled containers. |
| Any | Fills and drains ordinary fluid, loose fluid first. Compressor Tanks also accept FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Compressor Tanks only take FE in. They never give FE out, so they cannot act as batteries. Gear and Refinement slots are filled by hand.

To throw fluid away, use the purge buttons on the Process tab, or right-click the tank with a **Purge Bucket** to void up to 1,000 mB. A normal right-click empties loose fluid first; sneaking empties compressed fluid first, which needs no FE.
