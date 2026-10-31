---
navigation:
  title: "Wrench"
  parent: universal-cable.md
  position: 5
  icon: rngtech:cable
---

# Wrench

The <ItemLink id="rngtech:wrench" /> manages links and shows connector settings.

- Right-click the cable arm that leads to another Cable to switch that link off. To switch it back on, right-click the side of the cable core facing the other Cable. Disabled links stay off after reloads, and only the Wrench turns them back on. Cables dyed different colors cannot be linked.
- Right-click a connector on a cable face to open its screen.
- Hold the Wrench while looking at a Universal Connector, standalone or on a cable, to see a hologram of its settings. Direct Energy Connectors left on a cable face from older worlds show no hologram.
- The hologram has Summary, Energy, Item, and Fluid pages. The Energy page shows the first Energy row that has a connector. The Summary page also shows the bridge, such as `Bridge: AE2, channel 3`. Shift + right-click cycles the pages, and so does the **Cycle Wrench View** key. That key has no default binding; set it under Controls.
- The Energy, Item, and Fluid pages add a red line under a connector or module that cannot move anything: no other connector on its channel, nothing on the channel that can receive from or supply its mode (for example two inputs on one channel), or a module with no Attach side. Working connectors show no warning.
