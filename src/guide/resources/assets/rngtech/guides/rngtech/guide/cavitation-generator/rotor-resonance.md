---
navigation:
  title: "Rotor resonance"
  parent: cavitation-generator.md
  position: 1
  icon: rngtech:cavitation_generator
---

# Rotor resonance

Every Cavitation Rotor has its own resonance speed, somewhere between 55% and 90% throttle. Run it at exactly that throttle and the rotor wears about a third as fast. FE and heat do not change, so you keep everything else the throttle gives you.

The resonance is not shown on the rotor. Find it by tuning:

- Watch **Rotor wear now** in the gauge tooltip while you scroll 1% at a time. It drops as you approach the resonance and turns aqua close to it.
- Each step closer helps. Three steps off still saves a quarter of the wear, one step off saves most of it, and only the exact throttle gives the full bonus.
- In the world, the chamber glow flickers hard off resonance and settles into a steady glow on it.

A rotor's resonance never changes, so once you have found it you can leave the generator running. The rotor rolls its resonance when crafted. A more precise [Cavitation Rotor Hub](rngtech:cavitation-generator/crafting-rotors.md) makes a higher resonance more likely, which puts the rotor's low-wear speed at more FE/t, though a resonance above 70% also brings supercavitation heat with it. Rotors from creative or loot roll a resonance when first installed.

Two things build up as it runs:

- **Heat strain** builds while the rotor cavitates and cools off over time. The hotter the machine, the less FE each cycle makes, losing up to half at the strain cap of 1,000. At the cap it waits to cool before starting another cycle. Hover the strain bar to see the FE lost now and at the cap, and your cooling per tick.
- **Rotor wear** is stored on the rotor itself, so pulling it out and putting it back does not reset it. At rated speed off resonance, a Steel rotor lasts about an hour, Titanium 71 minutes, Tungstensteel 86 minutes, and Nullite 100 minutes; tuned to resonance, each lasts about three times as long. The Aethergold burst rotor lasts minutes. When wear reaches the rotor's durability, the rotor is removed and a **Pitted Cavitation Rotor** drops into the output slot. Keep that slot empty, or the generator stops. Pitted rotors can be salvaged in a Stage 5 [Component Recycler](rngtech:component-recycler.md) for byproduct only.

The generator never pauses because its storage is full. FE that its 4,000 FE buffer and Battery Cell cannot hold is lost, so connect it to cables and storage that keep up. It does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it.
