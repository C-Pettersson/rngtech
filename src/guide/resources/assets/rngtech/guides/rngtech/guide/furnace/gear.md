---
navigation:
  title: "Gear"
  parent: furnace.md
  position: 3
  icon: rngtech:furnace
---

# Gear

Electric Furnaces have a Gear tab with:

- one **Heat Core** slot. The core's stage cannot exceed the chassis stage. On the Lead Furnace one core heats all four lanes. A Lead Furnace built before lanes shared their core keeps its first core; the others go into an inventory beside it, or drop once a player is nearby.
- one **Battery Cell** slot. Without a cell, the Furnace keeps only a small internal buffer and smelts more slowly. If the Furnace has enough FE stored but the cell cannot hand it over fast enough for the current work, the status reads **Cell Rate Limited** instead of No Power. A cell with a higher Output rate fixes it.

The Stage 0 Furnace burns fuel from a slot on its Process tab and does not need Gear to run. Its Efficiency, Fuel Efficiency, and Fuel Duration stats all make each fuel item burn longer, so one item smelts more.

The Stage 0 Furnace has one optional Gear slot for the <ItemLink id="rngtech:kiln_core" />. The Kiln Core fits no other machine. It turns the kiln into a slow electric Furnace, so you can keep your first Furnace when you move to FE:

- The kiln takes FE on any side and stops burning fuel. It keeps a small internal FE buffer and has no Battery Cell slot. Take out the core to burn fuel again.
- Its Max Temperature goes from 800 to 1,000, so it can smelt Gold and Tin.
- 1,000 is a hard cap. Affixes, Mastery, and refinement cannot lift the kiln above it, so Steel Blend and the Stage 4 metals stay out of reach. A Mastery cap that is lower, such as Low Heat Specialist, still applies.
- The kiln smelts 25% more slowly than it does on fuel, and it was already slow. Each smelt costs more FE than in an electric Furnace, and the kiln has no overheat slider or Nitrogen tank.

<RecipesFor id="rngtech:kiln_core" />

- Requires the Copper material to be enabled.
