---
navigation:
  title: "Reading the reactor"
  parent: potential-reactor.md
  position: 3
  icon: rngtech:potential_reactor
---

# Reading the reactor

The installed Gear is visible on the block, in its material's colour:

- **Reactor Chamber**: the two electrodes inside the vessel. With no chamber the vessel is empty.
- **Containment Lining**: a sleeve lining the inside of the vessel, behind the electrodes.
- **Recovery Filter**: the cartridge in the open cage on the side, on the residue path.

Through the viewport, the core shows what the reactor is doing:

- **Colour**: violet on salvage fuel. Recycled gear burns in its rarity colour: white for Normal, aqua for Magic, and gold for Rare.
- **Size and brightness**: follow the FE per tick of the current item, so high-value gear burns larger and brighter.
- **Steadiness**: follows stability. Below 1.20 stability the core flickers, wanders, and arcs unevenly, and the relief valve on the lid vents smoke more often. Raise stability with a better Reactor Chamber or Containment Lining.
- **Banked**: when the FE buffer or the residue slot is full, the item stays loaded and the core dims to a low glow until there is room.

The dial on the front shows stability, from red (low) to green (high). The sight glass on the side shows the FE buffer, and the glass bin fills as the residue slot fills. A stripped item fills the bin on its own, since it does not stack: take it out to start the next one.

Recovered residue is **Scrap**, which cannot be fed back in for more FE. The reactor also accepts **Malformed Ingot** from failed smelts and presses. It needs a Stage 4 (Steel) Reactor Chamber and pays 3,600 FE before stats. With a Recovery Filter, a malformed ingot of a known metal returns two matching nuggets, and an unknown one returns two Scrap.
