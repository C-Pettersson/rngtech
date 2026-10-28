---
navigation:
  title: "Building a network"
  parent: universal-cable.md
  position: 0
  icon: rngtech:cable
---

# Building a network

1. Place Cables. Touching Cables link up on their own, unless they are dyed different colors (see [Dyed cables](rngtech:universal-cable/dyed-cables.md)).
2. Use a Universal Connector on the cable face beside a machine. You can also use it on the machine itself when a Cable is next to it. The connector mounts on that face of the cable.
3. Right-click the connector with an empty hand to open it, and install modules: an Energy Connector for FE, Fluid Connectors for fluids, Item Connectors for items.
4. Set each module's channel and mode. Only connectors on the same channel exchange anything.

One Cable can hold a connector on each of its six faces.

Hold a Cable or a Universal Connector and aim at a block to see a transparent preview. The preview shows where a right-click puts the part. It also shows the cable links and the connector face that the part gets. If no preview shows, a right-click does not place the part there. On a machine, the preview shows only while you sneak. Without sneak, a right-click opens the machine screen.

A network reaches only as far as the loaded world. Cables and machines in unloaded chunks drop out of the network and move nothing, and they join back in by themselves once their chunk loads again. Cables never keep chunks loaded.

Energy Connectors are modules: they go in a Universal Connector's Energy tab, or in the Gear slot of a [Tool Bench](rngtech:tool-bench.md) or [Forestry Cart Station](rngtech:forestry-cart-station.md). Right-clicking a Cable with one does not place it on the cable; if you aim at a connector, that connector's screen opens instead. Direct Energy Connectors on a cable face from older worlds keep working, and you can still configure and mine them, but you cannot place new ones.

## Standalone plates

A Universal Connector placed on its own, with no Cable, works as a standalone plate. If you use a Universal Connector on a Cable face that has no block behind it, the connector mounts on that face of the Cable. Sneak to place a plate in the space beyond that face instead. Use another Universal Connector on a different face of the same space to add a second plate there. Plates join a network only once they sit on a Cable: use a Cable on the plate's space, or on the machine face behind it, and the space turns into a Cable. Every plate in it keeps its modules and settings.
