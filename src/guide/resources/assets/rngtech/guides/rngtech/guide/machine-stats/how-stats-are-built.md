---
navigation:
  title: "How stats are built"
  parent: machine-stats.md
  position: 0
  icon: rngtech:iron_heat_core
---

# How stats are built

A machine's final stats come from several layers:

1. **Base stats** from the machine's material and stage.
2. **Installed [Gear](rngtech:gear.md)**: each part's own base stats plus its rolled affixes.
3. The machine's own **[affixes](rngtech:rarity-and-affixes.md)**.
4. **[Machine Mastery](rngtech:machine-mastery.md)** passives and [ascendancies](rngtech:machine-mastery.md#ascendancies).

Open a machine's **Stats** tab to see the result. Hover a row to see what the stat does. Stats that only exist on some machines appear only where they apply.

Hold **Shift** while hovering a row to see the math behind it. The breakdown lists the base value, each flat addition, the increased and more modifiers, and any fixed value or cap. Every line names its source: an affix, an installed part, a Mastery node, an ascendancy, or a penalty such as a missing Battery Cell. Each installed part counts as one line. Its own affixes are already included in that line, so hover the part itself to see them.

Affixes change stats in four ways. **Increased** and **reduced** percentages add together, while **more** and **less** multiply separately on top. Flat additions, such as "+2,000 FE", come first. Reduced Energy Use divides the cost instead, so it never reaches zero. See [Rarity and Affixes](rngtech:rarity-and-affixes/reading-the-tooltip.md).

In the tables below, **Better** says which direction you want.
