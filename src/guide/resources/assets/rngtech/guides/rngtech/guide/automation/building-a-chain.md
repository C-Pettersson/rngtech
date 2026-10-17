---
navigation:
  title: "Building a chain"
  parent: automation.md
  position: 2
  icon: rngtech:basic_item_connector
---

# Building a chain

- Give each recipe its own machine. When an item fits several recipes, the next matching ingredient to arrive picks the recipe. Use Item Connector filter rows or an <ItemLink id="rngtech:advanced_item_filter" /> to keep other recipes' items out.
- Pull outputs. A full output slot or tank pauses the machine.
- Size connectors to the machine. An Item Connector's tier sets how many items it moves and how often; see [Universal Cable](rngtech:universal-cable.md#items).
- Pipe fluids with Fluid Connectors. They fill input tanks directly.

Example: a [Metal Press](rngtech:metal-press.md) with a **Plate Mold** presses ingots into plates. A cable carries the plates to a Component Assembler, and a Melter pipes in Electrolyte Solution. The Assembler makes Anodes for a [Corrosion Cell](rngtech:corrosion-cell.md).
