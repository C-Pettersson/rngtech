---
navigation:
  title: "Generating power"
  parent: potential-reactor.md
  position: 0
  icon: rngtech:potential_reactor
---

# Generating power

Install a Reactor Chamber on the Gear tab, then put fuel in the input slot. The reactor processes one item at a time, adds the FE to its internal buffer, and pushes it out to adjacent blocks on its four sides, for example a [Universal Cable](rngtech:universal-cable.md). The reactor does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. When the buffer is full, the reactor pauses so expensive fuel is never burned for nothing.

A redstone signal pauses the reactor.

It accepts two kinds of fuel:

- **Salvage fuel**: recycling byproducts listed in the recipe table below. Each has a fixed FE value and needs a minimum Reactor Chamber stage.
- **Retired gear**: any machine, machine part, or Battery Cell that still has something to pay for: rarity above Normal, at least one rolled prefix or suffix, or unspent Refinement Potential. Unique items are refused, and so are unidentified or already stripped items.
