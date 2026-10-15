---
navigation:
  title: "Building a tool"
  parent: tool-bench.md
  position: 0
  icon: rngtech:tool_bench
---

# Building a tool

A Modular Field Tool is made from:

- a **Tool Head**, which decides the tool type and material, such as a **Flint Pick Head**.
- a **Tool Rod**, which sets durability, speed adjustments, FE transfer, and which Battery Cells the tool supports, such as a **Wooden Tool Rod**.
- an optional **Battery Cell**, which pays the FE for tool features that use it.

Heads and rods are crafted at a crafting table, and each one rolls its own rarity and affixes. They roll 2 more Refinement Potential than other parts of the same stage, and land higher affix tiers a little more often. The assembled tool has no affixes of its own; it uses whatever its installed parts have.

<RecipesFor id="rngtech:flint_pick_head" />

<RecipesFor id="rngtech:wooden_tool_rod" />

To build a tool, leave the Tool slot empty, put a head, a rod, and optionally a Battery Cell in the Build tab, and press **Apply**. The head's type picks the tool:

| Head family | Tool | Breaks |
|---|---|---|
| Pick | <ItemLink id="rngtech:modular_pick" /> | One pickaxe block. |
| Hammer | <ItemLink id="rngtech:modular_hammer" /> | A 3×3 area of pickaxe blocks. Needs a Tiny Anvil. |
| Shovel | <ItemLink id="rngtech:modular_shovel" /> | One shovel block. |
| Digger | <ItemLink id="rngtech:modular_digger" /> | A 3×3 area of shovel blocks, such as dirt, gravel, and sand. Needs a Tiny Anvil. |
| Axe | <ItemLink id="rngtech:modular_axe" /> | One axe block. |
| Treefeller | <ItemLink id="rngtech:modular_treefeller" /> | A whole tree of connected logs, up to its limit. Needs a Tiny Anvil. |
