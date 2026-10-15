---
navigation:
  title: "Ingredient leftovers"
  parent: automation.md
  position: 1
  icon: rngtech:basic_item_connector
---

# Ingredient leftovers

Some items leave another item behind when a recipe uses them, such as the Glass Bottle from a <ItemLink id="minecraft:honey_bottle" /> in the [Melter](rngtech:melter.md)'s Lubricant. No default Alloy Furnace or Component Assembler recipe takes one, but a modpack can add such recipes. In the Melter, Alloy Furnace, and Component Assembler, a leftover goes back into its emptied input slot only if automation would put it there itself. On the Melter, a leftover that can hold the product goes into an empty output container slot instead. Any other leftover goes into the inventory directly below, such as a hopper or chest, or drops on the ground if there is none, so it never blocks the input slots.
