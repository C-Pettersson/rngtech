---
navigation:
  title: "Automation"
  parent: silica-gel-dehumidifier.md
  position: 1
  icon: rngtech:silica_gel_dehumidifier
---

# Automation

| Face | Behavior |
|---|---|
| Any controller face | Inserts dry beads into the lanes and extracts saturated beads. |
| Controller sides | Drain water. |
| Any casing face | Inserts and extracts beads for the controller while the column is complete. |
| Casing sides | Drain water for the controller while the column is complete. |

Each inserted bead goes to the lane with the fewest beads that can take it. A hopper on top of the column or a Furnace chain therefore keeps all three lanes busy. Automation cannot pull dry beads out of a lane. The machine never accepts fluid and has no FE connection.
