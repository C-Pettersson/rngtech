---
navigation:
  title: "Water-gas shift"
  parent: steam-methane-reformer.md
  position: 1
  icon: rngtech:steam_methane_reformer
---

# Water-gas shift

The shift uses 500 mB of Carbon Monoxide and 500 mB of water, and makes 500 mB of Hydrogen and 125 mB of Carbon Exhaust. It needs the same Heat Core and Catalyst Bed as a reform.

The gas tank and the second output tank each hold one fluid at a time. The second output tank holds Carbon Monoxide after a reform and Carbon Exhaust after a shift, so a Reformer cannot start a shift while that tank still holds Carbon Monoxide; drain or purge it first. In practice, run the shift in its own Reformer and pipe the first Reformer's Carbon Monoxide into it.

Shifting the Carbon Monoxide from one Methane reform adds 500 mB of Hydrogen, about a third more. The cost is in the algae: the [Algae Photobioreactor](rngtech:algae-photobioreactor.md) grows one Algae Biomass in 600 ticks on Carbon Monoxide but needs 1,200 ticks on Carbon Exhaust, so a shift loop needs about twice the Photobioreactors. See [the carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md).
