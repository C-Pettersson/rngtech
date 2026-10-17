---
navigation:
  title: "Generating power"
  parent: ammonia-fuel-cell.md
  position: 0
  icon: rngtech:ammonia_fuel_cell
---

# Generating power

Install a Fuel Cell Membrane in the Gear tab, then pipe Ammonia into the 8,000 mB tank. Each cycle burns 1,000 mB of Ammonia and generates 216,000 FE over 18 seconds, which is 600 FE/t before membrane and machine rolls. After the synthesis cost, the chain nets about 510 FE/t.

You can read the cell from the outside. Facing the front:

- An installed membrane shows as green-and-white lines between the dark plates of the stack. With no membrane, the slots between the plates are empty.
- The glass tank at the back right shows the Ammonia level.
- Corrosion Residue collects in the glass trap at the back left, and vapour rises from the vent stack above it while the cell runs.
- The lamp on the cabinet lights while the cell runs.

The cell pushes FE out of its top and sides. It does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. It stops when its internal storage and Battery Cell are full or when the residue slot is full, so keep both drained. The Process tab has a purge button for the Ammonia tank. Purging resets the cycle in progress.
