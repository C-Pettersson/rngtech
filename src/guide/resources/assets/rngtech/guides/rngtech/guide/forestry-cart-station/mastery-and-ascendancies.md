---
navigation:
  title: "Mastery and Ascendancies"
  parent: forestry-cart-station.md
  position: 5
  icon: rngtech:forestry_cart
---

# Mastery and Ascendancies

Successful planting, leaf cleanup, and log cutting earn Mastery XP for the cart. The Forestry Companion starts from the Control / Drive start of the shared [Machine Mastery](rngtech:machine-mastery.md) tree, with nodes for cart speed, work range, managed cells, and behaviors such as Magnet Mode, which picks up dropped items within 3 blocks into output cargo. With Seedling Magnet too, picked-up saplings go to sapling cargo first.

Its first Ascendancy Seal chooses one of three paths:

- **Timber Baron**: a logger that banks extra logs and harvests while it keeps rolling.
- **Grove Warden**: spends bone meal to grow managed saplings faster, and can plant 2×2 giant trees.
- **Field Hand**: a farmer that plants and harvests wheat, carrots, potatoes, and beetroot on farmland instead of trees, and can water its own fields.

A few rules the node tables below do not spell out:

- **Rolling Harvest** and **Rolling Reap**: the cart halts only when a tree or ripe crop it is working, or still has to work, would leave its reach at the next rail block. Cells it rolled past stay on its work list while in reach. Empty cells never hold it up. After planting, it moves at most one rail block per planting interval, so faster Processing Speed means a faster stride. With nothing to plant it rolls at full speed. Each sapling still costs its usual FE.
- **Growth Pulse**: growth costs bone meal, not FE alone. A Grove Warden cart stores up to 256 bone meal, loaded through its sapling slots or from a docked station. The station keeps its own 256 store, filled through its bone meal slot or its sapling slot. <ItemLink id="rngtech:urea" /> from the [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md) goes anywhere bone meal goes and adds 4 to the store; a store takes only as many whole Urea as fit. Each pulse still spends 1 from the store. Each managed sapling takes at most one pulse per 100 ticks. If a ready sapling still does not grow, for lack of room or because its species only grows as a 2×2, that cell waits 1,200 ticks before the next pulse.
- **Seed Library** and **Seed Ledger**: a cell remembers whatever grows in it when the cart scans it, including plants you placed yourself, and the cart adopts those plants while its cell cap allows. When the cart harvests a cell, it sets aside one matching sapling or crop item from that cell's drops (four for an Ancient Grove plot) as that cell's replant. Planting uses that reserve before supply cargo, so a common species cannot crowd out a rare one. The Stats tab shows reserves as Reserved. They return to cargo when a cell is forgotten or changes species, move to output if the cart loses the node, and drop if the cart is broken.
- **Ancient Grove** needs four matching dark oak, jungle, or spruce saplings in cargo. It plants a clear 2×2 plot away from the rail on any row, and the far squares may sit one row past Work Range. It costs four plantings of FE and counts as one managed cell.
- **Field Hand** supply slots take only crop items. Seeds, carrots, and potatoes from a harvest go back to supply; wheat and beetroot go to output. Each crop harvest costs 20 FE and gives 2 machine XP. If the cart leaves Field Hand, crop items left in supply move to output.
- **Sprinkler**: the cart's water tank holds 8,000 mB, raised by Fluid Capacity. A docked station fills it as fast as the installed Fluid Pump's transfer rate allows. Sprinkled farmland counts as next to water for 60 seconds (90 with Irrigation), and the cart refreshes it once half that time has passed. Watering works like a nearby water source and adds no extra growth.

See [Forestry Companion ascendancies](rngtech:machine-mastery.md#ascendancies) for the node tables.
