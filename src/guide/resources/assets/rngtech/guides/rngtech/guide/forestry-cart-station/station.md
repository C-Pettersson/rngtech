---
navigation:
  title: "Station"
  parent: forestry-cart-station.md
  position: 2
  icon: rngtech:forestry_cart
---

# Station

When the cart stops beside a station, the station:

- unloads the cart's output into its six output slots.
- loads saplings from its supply into the cart. A Seed Library or Seed Ledger cart first gets the species its empty cells are waiting for, and the station makes room by moving supply nobody is waiting for into its plantables buffer. While the station still stocks a species the cells want, it tops up only species the cart already carries.
- installs spare shears into an empty cart shears slot, and a spare Axe or Treefeller when the cart's tool is missing or broken. The broken tool goes to the station's output slots, so no tool is swapped while those are full.
- charges the cart's Battery Cell, at up to its Energy Connector tier or its own Battery Cell's output rate, whichever is higher. The cart waits at the dock until its cell is full.
- holds a cart with empty sapling cargo while it loads saplings, then lets it go once the station has had nothing more to load for 20 ticks.

The station's hold toggle keeps any docked cart parked until you release it. The station does not need to be linked to a cart; any cart that passes it is served.
