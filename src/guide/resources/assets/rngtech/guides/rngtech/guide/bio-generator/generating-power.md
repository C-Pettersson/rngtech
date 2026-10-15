---
navigation:
  title: "Generating power"
  parent: bio-generator.md
  position: 0
  icon: rngtech:bio_generator
---

# Generating power

Put fuel in the fuel slot on the Process tab. Power comes in three steps, and you can watch each one on the block:

1. **Digester.** The digester takes one item at a time from the hopper and turns it into biogas at about 8 FE/t before stats. A fuel's FE value decides how long it digests: a Composted Biomass worth 1,600 FE takes 200 ticks (10 seconds). The sight glass on the tank shows the current item's slurry level falling, with bubbles while gas forms.
2. **Gas holder.** Biogas collects under the dome on top of the tank, which swells as it fills. It holds 1,600 FE of gas.
3. **Engine.** The gas engine starts once the holder has 400 FE of gas, then runs at the digester's pace until the holder is empty. Its flywheel spins and the stack puffs exhaust while it runs, and the lamp on the hopper lights. The gauge on the engine shows the holder's pressure, with the amber band below the start pressure.

A freshly fed generator therefore waits a few seconds before the first FE while the gas pressure builds. The last of the gas still burns when the hopper runs dry, so no fuel is lost.

Generated FE goes into a small internal buffer, plus any installed Battery Cell, and the generator pushes it out to adjacent blocks on every side, for example a [Universal Cable](rngtech:universal-cable.md). The generator does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. When the buffer is full, the engine idles and the digester keeps filling the gas holder. Once the holder is full too, the digester waits. Nothing is wasted. The generator never accepts FE from outside.

Golden Apples, Enchanted Golden Apples, and Golden Carrots are refused.
