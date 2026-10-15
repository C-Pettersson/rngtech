---
navigation:
  title: "Pressing"
  parent: metal-press.md
  position: 0
  icon: rngtech:metal_press
---

# Pressing

Install a Heat Core and at least one Mold, select the Mold you want in the Gear tab, then put the input in the input slot and supply FE. The press warms up using the recipe's normal FE draw and starts pressing only once its heat reaches the recipe's target temperature. If it runs out of FE while warming up, it pauses and cools. Adding more of the same input keeps the current press going, so a hopper or Item Connector can feed it; swapping the input for a different item restarts it.

Each recipe has a heat window and a temperature stability requirement. The two presses handle a bad window differently:

- The **Crude Metal Press** never fails. If its heat is too low or too high, its stability is too low, or it runs out of power, it pauses and leaves the input alone. A press that is too hot cools down before continuing.
- The **Metal Press** builds up failure strain instead. Low heat, low stability, a weak Servo, or a power drop after pressing starts all add strain. Like the Crude press, it cools down to the recipe's overheat temperature before pressing when a hotter recipe left it above that. When strain fills, the input is used up and the press outputs a failure item: a **Malformed Ingot** for plates and casings, or a **Broken Circuit** for circuits. Smelt Malformed Ingots in a [Furnace](rngtech:furnace.md) to get two nuggets back, or keep Stage 5 and higher ones for the [Volatile Catalyst](rngtech:affix-forge/crafting-the-consumables.md). The [Potential Reactor](rngtech:potential-reactor.md) and [Component Recycler](rngtech:component-recycler.md) can recover scrap from Broken Circuits.
