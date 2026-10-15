---
navigation:
  title: "Any order"
  parent: automation.md
  position: 0
  icon: rngtech:basic_item_connector
---

# Any order

The [Melter](rngtech:melter.md) and [Alloy Furnace](rngtech:alloy-furnace.md) ignore slot order. A recipe that needs two of an item takes both from one slot. An item gets a second slot only if every recipe it could still make keeps an empty slot for each missing ingredient.

- A Melter fed only <ItemLink id="minecraft:sugar" /> fills one slot and keeps the other for the <ItemLink id="minecraft:redstone" /> in Electrolyte Solution.
- A Melter fed only <ItemLink id="minecraft:ender_pearl" /> keeps one slot for **Collapse Residue**. Ender Slurry from pearls alone takes both pearls from that one slot.
- A three-slot Alloy Furnace fed only <ItemLink id="minecraft:copper_ingot" /> keeps two slots for Bronze's tin and charcoal.
- Two Lubricant bases cannot fill both Melter slots, because the additive needs one.

An ingredient stacks in its own slot up to a full stack. To keep ingredients within one craft of each other, take the Alloy Furnace's **Recipe Lock** in the [Metallurgist](rngtech:alloy-furnace.md) ascendancy.
