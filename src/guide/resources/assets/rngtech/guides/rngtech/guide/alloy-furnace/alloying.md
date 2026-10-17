---
navigation:
  title: "Alloying"
  parent: alloy-furnace.md
  position: 0
  icon: rngtech:bronze_alloy_furnace_chassis
---

# Alloying

Put the recipe's ingredients in the input slots. Each recipe needs a set count of each ingredient, but the slot order does not matter. Extra units are fine and feed the next crafts, but every item in the input slots must belong to the recipe: one stray item stops the machine. The Alloy Furnace then heats up and starts mixing only once it reaches the recipe's target temperature. While it is out of input or out of power, it cools down. A full output slot pauses it with its heat and progress kept. Swapping an input for a different item mid-craft restarts the craft.

Each recipe also has a minimum stage. Both the chassis and the installed Alloy Crucible must reach that stage, so late alloys need a Titanium setup. The machine's max heat must also reach the recipe's target temperature. On recipes that cannot fail, its Temperature Stability must meet the recipe's requirement, or the craft does not start. See [Heat](rngtech:heat.md) for how it heats, what the FE pays for, and the overheat slider.

Your first Bronze does not need an Alloy Furnace. 3 Copper Dust, 1 Tin Dust, and 1 Coal Dust make 2 Bronze Blend in a crafting grid, in any arrangement. Once you have a Bronze Alloy Furnace, it makes more Bronze Blend from the same dusts plus Charcoal, and it can already make a slow Steel Blend bootstrap recipe.

Stage 4 and higher recipes with a failure output build up failure strain when heat or stability is off, or when power drops mid-craft on power-sensitive recipes. When strain fills, the craft fails and outputs a recoverable **Malformed Ingot** instead of the alloy. Smelt it in a [Furnace](rngtech:furnace.md) to get two nuggets back, or keep it for the [Volatile Catalyst](rngtech:affix-forge/crafting-the-consumables.md). Of the default recipes, only Nullite and Tungstensteel can fail.
