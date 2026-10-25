---
navigation:
  title: "Machine Mastery"
  parent: guides.md
  icon: rngtech:ascendancy_seal_1
categories:
  - "Guides"
item_ids:
  - rngtech:mastery_refund
  - rngtech:ascendancy_seal_1
  - rngtech:ascendancy_seal_2
  - rngtech:ascendancy_seal_3
  - rngtech:primed_seal_core
  - rngtech:lubricated_seal_core
  - rngtech:soul_transfer
---

# Machine Mastery

**Machine Mastery** lets a machine get better the more work it does. Each machine earns its own XP, levels up, and spends the points it earns on a large shared passive tree. Later, Ascendancy Seals unlock a small specialization tree just for that machine's family.

Mastery belongs to the individual machine, not to you. Two Crushers side by side level separately, and each one keeps its progress when you break it and place it again.

## Earning XP

A machine earns XP each time it successfully finishes a job that awards Mastery XP. Recipe tables on the machine pages show a **Mastery XP** column where a recipe gives XP.

| Family | What earns XP |
|---|---|
| Crusher | Finished crushing jobs. A dense batch awards XP for each job in it. |
| Furnace | Smelting ores, raw metals, crushed ores, and alloy blends. Dust-to-ingot smelts, food, and decorative blocks give none. |
| Alloy Furnace | Finished alloy recipes. |
| Metal Press | Finished pressing jobs. |
| Resonance Calibrator | Finished calibrations, counted per lane on multi-lane chassis. |
| Melter | Finished melts that land in the output tank. |
| Forestry Companion | Planting, leaf cleanup, and harvesting. |

Failed jobs, idle time, waiting on a full output, and recipes with no XP give nothing.

- [Harder work, more XP](rngtech:machine-mastery/harder-work-more-xp.md)

## The passive tree

All seven families share one passive tree of 1,368 nodes. Each family enters at its own **start**, shown in the table above. Your machine can only grow outward from its own start, and it cannot pass through another family's start.

The tree has three **attributes**: **Control** (teal), **Drive** (gold), and **Reserve** (red). Each start gives 20 points of its attribute, split 10 and 10 on mixed starts. Travel nodes along the tree's roads add more, and machines and Gear can roll [attribute suffixes](rngtech:rarity-and-affixes/attribute-suffixes.md). Attributes give bonuses on their own:

| Attribute | Every machine | Family bonus |
|---|---|---|
| Control | 0.1% increased Stability and 0.02% reduced Energy Usage per point | Furnace, Alloy Furnace, and Metal Press: 0.05% increased Temperature Stability per point. Resonance Calibrator: 0.05% increased Calibration Precision per point. |
| Drive | 0.15% increased Processing Speed per point | Forestry Companion: +1 Tree Fell Limit per 20 points. |
| Reserve | 0.25% increased Energy Capacity per point (not the Forestry Companion) | Furnace and Alloy Furnace: 0.1% increased Heat Isolation per point. Melter: 0.1% increased Fluid Transfer per point. Forestry Companion: +1 managed-cell maximum per 4 points. |

A bonus only matters where the machine uses that stat. For example, the Stage 0 fuel Furnace has no FE buffer, so Reserve's Energy Capacity does nothing for it.

Hover your start node to see your current attribute totals.

- [Attribute scaling](rngtech:machine-mastery/attribute-scaling.md)
- [Node types](rngtech:machine-mastery/node-types.md)

## Allocating and refunding

Open the **Mastery** tab to browse the tree.

- **Moving around**: drag to pan and scroll to zoom. Shift-scroll pans sideways and Ctrl-scroll pans up and down. The button at the top right expands the view to fill the screen. The **Menu** button left of the search box opens the tree's actions, including **Home**, which centers on your machine's start, and **Fit**, which shows the whole tree.
- **Find**: click the search box or press Ctrl+F, then type part of a node's name, stat, or ID. The box shows how many nodes match, and Enter jumps to the next match. Typing `keystone`, `notable`, or `travel` highlights every node of that type.

Spending and refunding points:

- **Allocate**: hover any unallocated node to preview the shortest path to it and its cost, then click to buy the whole path in one go. The path is bought only if you can afford all of it and your Gear allows every node on it; otherwise nothing is bought. The purchase happens when you release the click, so a drag that starts on a node only pans. Every node costs one point and must connect back to your start.
- **Gear limits**: some nodes restrict which Gear you can install. You cannot take a node your installed Gear breaks; the tooltip says why.
- **Refund**: right-click an allocated node to remove it. Each removed node costs one <ItemLink id="rngtech:mastery_refund" />. A refund must leave the rest of your tree connected and your Gear legal. Shift-click **Clear tree** in the **Menu** to refund the whole tree at the same price per node. After an update that changes the tree, each machine's build is kept wherever it still fits, and its next Clear is free. While it is available, the **Menu** button and its **Clear tree** entry glow green, and the Bonus Summary and start node say so.

In the expanded view, the tab on the right edge opens the **Bonus Summary**: your attribute totals, keystones, and every allocated effect combined the way the machine actually adds them up. It has sections for your Ascendancy, bonuses that come from attributes, attribute scaling, limits, special behaviors, and effects this machine does not use. Hover a line to see the stat, its current contribution, and which nodes it comes from.

- [Mastery Refunds](rngtech:machine-mastery/mastery-refunds.md)

## Copying builds and keeping progress

- [Build codes](rngtech:machine-mastery/build-codes.md)

### Breaking a machine

Breaking a Mastery machine with the right tool drops it with its XP, level, tree, and ascendancy intact. Pick-block in Creative keeps them too, and the Forestry Companion keeps its progress in item form.

Progress belongs to that one machine. Crafting a higher-stage chassis starts a fresh machine at level 1. To carry your progress over, use a Soul Transfer.

- [Soul Transfer](rngtech:machine-mastery/soul-transfer.md)

## Ascendancies

An **ascendancy** is a small specialization tree for one machine family. Each family has two to choose from, and the Forestry Companion has three. You unlock one with **Ascendancy Seals**. Ascendancy points are separate from your passive tree points.

- [Ascendancy Seals](rngtech:machine-mastery/ascendancy-seals.md)
- [Choosing and spending points](rngtech:machine-mastery/choosing-and-spending-points.md)
- [Ascendancies by family](rngtech:machine-mastery/ascendancies-by-family.md)

## More

- [Which machines have Mastery](rngtech:machine-mastery/which-machines-have-mastery.md)
- [Levels and points](rngtech:machine-mastery/levels-and-points.md)
