---
navigation:
  title: "Screen"
  parent: phase-array.md
  position: 7
  icon: rngtech:exotic_affix_forge
---

# Screen

The Cradle has one screen. Its item area at the top right reads like a furnace: the item goes in, the Glyph is the fuel, and the result comes out at the end of the arrow.

- **The line** runs across the top from the Emitter at the left, one node per position, into the chamber. A lit node is a usable position. A dim node with an amber edge is closed, and its hover names the seat it waits on. After a pulse, the held nodes turn green and the slipped one turns red or amber. See [Reading a slip](rngtech:phase-array/outcomes.md#reading-a-slip).
- **The chamber** at the end of the line holds the target item. Marks around it show the item's Strain.
- **The Glyph slot** sits under the chamber, where a furnace keeps its fuel. **The donor slot** sits beside it. The donor slot is dim unless the operation is Graft.
- **The conduit** is the arrow from the chamber to the output. It fills with the charge, and its head lights when the charge is full. Hover it for the FE held and needed, the rate FE arrives, and the time to full.
- **The button** under the conduit reads Charge, Stop, Fire, or Pulse, whichever the line is ready for.
- **The output slot** is the large slot at the end of the arrow. The item goes there when the pulse ends.
- **The readout** under the line names the last outcome. With an item in the chamber, it also shows the item's Strain and RP.
- **The operation marks** run in a row above the list. A dim mark cannot run, and its hover tells you why: the missing position, Glyph, selection, or RP.
- **The list** shows what the chosen operation can act on: the item's affixes, its empty slots, the affixes it can take, or the donor's affixes.
- **The Glyph seats** run down the right side under the output slot, numbered I to V. A numeral lights while its seat holds a Glyph.
- **The cell sockets** sit beside your inventory, nine of them. Each ring dims as its cell empties.
- **Three lights** at the top right show blocked, waiting for input, and live.

Hover anything for its exact values, and an empty slot for what goes in it. In the world, Jade on the Cradle names the chosen operation and, with no line, the first block it lacks. With JEI installed, the operations are listed under the Phase Array category.
