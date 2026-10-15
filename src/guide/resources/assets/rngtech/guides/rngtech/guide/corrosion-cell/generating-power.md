---
navigation:
  title: "Generating power"
  parent: corrosion-cell.md
  position: 0
  icon: rngtech:corrosion_cell
---

# Generating power

Each cycle needs two things:

- **Fuel**: one plate, Anode, or **Scrap** in the plate slot.
- **Electrolyte**: one **Electrolyte** in the electrolyte slot, or 125 mB of Electrolyte Solution from the internal tank. The dry item is used first when both are present.

The cell then generates the fuel's FE over the cycle and drops one Corrosion Residue into the output slot. It pulls the next fuel only when the residue slot has room and its internal buffer or installed Battery Cell can still take FE. If the buffer fills mid-cycle, the fuel stops dissolving until FE is drawn away, so no FE is lost. Clear the residue and draw FE away, or the cell sits idle. The cell does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it, and it resumes the same fuel when the signal ends.

The electrolyte tank holds 4,000 mB, and a Fluid Pump's Fluid Capacity rolls make it bigger. To empty it, use the purge button on the Process tab or right-click the cell with a **Purge Bucket**. Each purge voids up to 1,000 mB.
