---
navigation:
  title: "Gasifying coal"
  parent: coal-gasifier.md
  position: 0
  icon: rngtech:coal_gasifier
---

# Gasifying coal

1. Install a Stage 5 or higher Heat Core in the Gear tab. The Gasifier will not run without one.
2. Put Coal Dust in the input slot. Grind coal in a [Crusher](rngtech:crusher.md) to get it.
3. Fill the water tank, for example by pumping water in with a Fluid Connector on a [Universal Cable](rngtech:universal-cable.md).
4. Supply FE.

Each run uses one Coal Dust, 500 mB of water, and 6,000 FE over 12 seconds, and makes 1,000 mB of Syngas plus one Gasification Residue. The water and Syngas tanks hold 8,000 mB each. The Gasifier stops when the Syngas tank or the residue slot is full, so pipe both away.

You can read the Gasifier from the outside. Facing the front:

- The hopper on top shows how much Coal Dust is loaded, and the rotary valve under it turns while the Gasifier runs.
- The view port on the vessel glows while it runs, and the gauge beside the Syngas line rises with it.
- The glass on the right shows the water level and the glass on the left the Syngas level.
- Residue piles up in the bin under the vessel, and steam rises from it while the Gasifier runs.
- The installed Heat Core sits in a socket on the right side of the vessel, the Servo beside the valve, and the Battery Cell on the back cabinet, each in its material's colour.
- The lamp at the front lights while it runs.

The screen's status line tells you what is missing: Heat Core, input, fluid, power, or output space. You can empty the water or Syngas tank with the purge buttons on the Process tab, or by right-clicking the Gasifier with a **Purge Bucket**. Each purge voids up to 1,000 mB.
