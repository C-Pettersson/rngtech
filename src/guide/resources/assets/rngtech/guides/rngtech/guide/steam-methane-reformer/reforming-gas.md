---
navigation:
  title: "Reforming gas"
  parent: steam-methane-reformer.md
  position: 0
  icon: rngtech:steam_methane_reformer
---

# Reforming gas

1. Install a Stage 6 or higher Heat Core and a Reforming Catalyst Bed in the Gear tab. The Reformer needs both to run.
2. Fill the water tank and the gas tank with Methane or Syngas. Make Methane from algae biomass in the [Melter](rngtech:melter.md).
3. Supply FE.

Each run uses 1,000 mB of gas and 500 mB of water. Methane makes 1,500 mB of Hydrogen and Syngas only 500 mB, so treat Syngas as a stopgap until your Methane supply is running. Both feeds also make 500 mB of Carbon Monoxide. See the [recipe table](rngtech:steam-methane-reformer.md) for time and FE costs.

All four tanks hold 8,000 mB. The Reformer stops when either output tank is full, so drain both.

You can read the Reformer from the outside. Facing the front:

- The window shows four reformer tubes. An installed Catalyst Bed shows as pellets packed inside them, and the tubes glow red-orange while the Reformer runs.
- The installed Heat Core sits in the burner bay under the firebox, in its material's colour, and blue burner flames light under the tubes while it runs.
- The three glasses on the right show the feed gas, Hydrogen, and Carbon Monoxide levels. The glass on the steam drum shows the water level, and the gauge beside it rises while the Reformer runs.
- The Servo works the feed valve above the feed glass, and the Battery Cell sits at the front left with a charge window.
- Smoke rises from the flue stack at the back while it runs, and the lamp at the front lights.

The screen's status line tells you what is missing: Heat Core, catalyst, fluid, power, or output space. You can empty any tank with the purge buttons on the Process tab, or by right-clicking the Reformer with a **Purge Bucket**. Each purge voids up to 1,000 mB.
