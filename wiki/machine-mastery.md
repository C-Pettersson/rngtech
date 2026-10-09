---
wiki:
  category: Guides
  icon: rngtech:ascendancy_seal_1
  ids:
    - rngtech:mastery_refund
    - rngtech:ascendancy_seal_1
    - rngtech:ascendancy_seal_2
    - rngtech:ascendancy_seal_3
    - rngtech:primed_seal_core
    - rngtech:lubricated_seal_core
---

# Machine Mastery

**Machine Mastery** lets a machine get better the more work it does. Each machine earns its own XP, levels up, and spends the points it earns on a large shared passive tree. Later, Ascendancy Seals unlock a small specialization tree just for that machine's family.

Mastery belongs to the individual machine, not to you. Two Crushers side by side level separately, and each one keeps its progress when you break it and place it again.

## Which machines have Mastery

Seven machine families have a **Mastery** tab:

| Family | Machines | Starting point on the tree |
|---|---|---|
| Crusher | Every [Crusher](crusher.md) chassis | Drive |
| Furnace | Every [Furnace](furnace.md), including the Stage 0 fuel Furnace | Drive / Reserve |
| Alloy Furnace | Every [Alloy Furnace](alloy-furnace.md) chassis | Drive / Reserve |
| Metal Press | Crude Metal Press and Metal Press ([Metal Press](metal-press.md)) | Control |
| Resonance Calibrator | Every [Resonance Calibrator](resonance-calibrator.md) chassis | Control |
| Melter | [Melter](melter.md) | Reserve / Control |
| Forestry Companion | The Forestry Companion cart ([Forestry Cart Station](forestry-cart-station.md)) | Control / Drive |

Other machines, Gear, and cables do not level up.

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

### Harder work, more XP

Every XP-awarding job has a **work level** from 1 to 98. It comes from how demanding the job is: ore hardness for the Crusher, required processing level for the Melter, target heat for the Furnace, Alloy Furnace, and Metal Press, and required calibrator stage for the Resonance Calibrator. For the Forestry Companion it rises with its Tree Fell Limit.

Higher work levels pay much more XP per job. Once your machine's level passes the work level, the XP falls off:

| Machine level compared with work level | XP awarded |
|---|---:|
| At or below | 100% |
| 1 level above | 50% |
| 2 levels above | 25% |
| 3 or more above | 0% |

To keep levelling, feed the machine harder work. Fractions of XP are saved, so small amounts are never lost.

## Levels and points

Machines start at level 1 and can reach **level 100**. Each level after the first gives **one point**, so a level 100 machine has **99 points**.

The XP curve gets steep. These are the total XP needed to reach some levels:

| Level | Total XP |
|---:|---:|
| 2 | 100 |
| 10 | 5,600 |
| 20 | 184,000 |
| 30 | 5,334,600 |
| 50 | about 35.9 million |
| 100 | about 4.2 billion |

After level 30, each level needs 10% more total XP than the one before.

## The passive tree

All seven families share one passive tree of 1,346 nodes. Each family enters at its own **start**, shown in the table above. Your machine can only grow outward from its own start, and it cannot pass through another family's start.

The tree has three **attributes**: **Control** (teal), **Drive** (gold), and **Reserve** (red). Each start gives 20 points of its attribute, split 10 and 10 on mixed starts. Travel nodes along the tree's roads add more. Attributes give bonuses on their own:

| Attribute | Every machine | Family bonus |
|---|---|---|
| Control | 0.1% increased Stability and 0.02% reduced Energy Usage per point | Furnace, Alloy Furnace, and Metal Press: 0.05% increased Temperature Stability per point. Resonance Calibrator: 0.05% increased Calibration Precision per point. |
| Drive | 0.15% increased Processing Speed per point | Forestry Companion: +1 Tree Fell Limit per 20 points. |
| Reserve | 0.25% increased Energy Capacity per point (not the Forestry Companion) | Furnace and Alloy Furnace: 0.1% increased Heat Isolation per point. Melter: 0.1% increased Fluid Transfer per point. Forestry Companion: +1 managed-cell maximum per 4 points. |

A bonus only matters where the machine uses that stat. For example, the Stage 0 fuel Furnace has no FE buffer, so Reserve's Energy Capacity does nothing for it.

Hover your start node to see your current attribute totals.

### Node types

- **Travel nodes** sit along the roads and grant attribute points.
- **Ordinary nodes** and **notables** branch off the roads into reward clusters. Notables are the stronger nodes at the end or center of a cluster, at least three points in from the road.
- **Keystones** are big trade-offs, at least 10 points from every start. For example, **Steady State** gives 50% more Stability but 15% less Processing Speed, and **Single Pass** turns off bonus output for 30% more Processing Speed.

Every node is visible to every family. A node can have no effect on your machine: a Crusher can take a heat node and gain nothing from it. Any penalty on that node still applies, and the tooltip marks which effects are inactive. Some keystone payoffs only work on the machines that actually pay their cost; the tooltip lists which machines those are. Use the relevance control to highlight nodes that matter for your machine.

Stat terms like "increased" and "more" are explained in [Rarity and Affixes](rarity-and-affixes.md), and each stat is defined in [Machine Stats](machine-stats.md).

## Allocating and refunding

Open the **Mastery** tab to browse the tree.

- **Moving around**: drag to pan and scroll to zoom. Shift-scroll pans sideways and Ctrl-scroll pans up and down. The button at the top right expands the view to fill the screen. The toolbar has **Home**, which centers on your machine's start, and **Fit**, which shows the whole tree.
- **Find**: click the search box or press Ctrl+F, then type part of a node's name, stat, or ID. The box shows how many nodes match, and Enter jumps to the next match. Typing `keystone`, `notable`, or `travel` highlights every node of that type.

Spending and refunding points:

- **Allocate**: hover any unallocated node to preview the shortest path to it and its cost, then click to buy the whole path in one go. The path is bought only if you can afford all of it and your Gear allows every node on it; otherwise nothing is bought. The purchase happens when you release the click, so a drag that starts on a node only pans. Every node costs one point and must connect back to your start.
- **Gear limits**: some nodes restrict which Gear you can install. You cannot take a node your installed Gear breaks; the tooltip says why.
- **Refund**: right-click an allocated node to remove it. Each removed node costs one {{ item('rngtech:mastery_refund') }}. A refund must leave the rest of your tree connected and your Gear legal. Shift-click **Clear** to refund the whole tree at the same price per node.

In the expanded view, the tab on the right edge opens the **Bonus Summary**: your attribute totals, keystones, and every allocated effect combined the way the machine actually adds them up. It has sections for your Ascendancy, bonuses that come from attributes, attribute scaling, limits, special behaviors, and effects this machine does not use. Hover a line to see the stat, its current contribution, and which nodes it comes from.

### Mastery Refunds

{{ crafting('rngtech:mastery_refund') }}

One craft makes eight. Creative players refund for free.

## Copying builds and keeping progress

### Build codes

**Copy** puts your allocation order on the clipboard as a build code. If the machine is following a pasted build, Copy takes the whole target build, not just the part allocated so far. **Paste** it into another machine with an empty tree and the same start. That machine buys what it can afford now, then keeps following the build as it earns points. Purple outlines show the target nodes. Use **Pause** and **Resume** to control following. Following pauses if a node would conflict with installed Gear or if you refund manually.

Machines that share a start can share build codes: the Furnace with the Alloy Furnace, and the Metal Press with the Resonance Calibrator.

The {{ item('rngtech:configurator') }} has a Mastery mode for copying without the clipboard. Sneak-use it in the air to switch modes. In Mastery mode, sneak-use a machine to copy its build and use it normally on another machine to paste. This works on the Forestry Companion cart too. You can also shift-click **Copy** on the Mastery tab: the build is saved to a Configurator in your inventory, which switches to Mastery mode.

### Breaking a machine

Breaking a Mastery machine with the right tool drops it with its XP, level, tree, and ascendancy intact. Pick-block in Creative keeps them too, and the Forestry Companion keeps its progress in item form.

Progress belongs to that one machine. Crafting a higher-stage chassis starts a fresh machine at level 1.

## Ascendancies

An **ascendancy** is a small specialization tree for one machine family. Each family has two to choose from, and the Forestry Companion has three. You unlock one with **Ascendancy Seals**. Ascendancy points are separate from your passive tree points.

### Ascendancy Seals

Each Seal grants **2 ascendancy points** to one machine. Seals are used in order, one per tier, for at most **6 points**:

| Seal | Needs |
|---|---|
| {{ item('rngtech:ascendancy_seal_1') }} | A machine of Stage 4 or higher. Choosing your ascendancy happens here. |
| {{ item('rngtech:ascendancy_seal_2') }} | Seal I already used on this machine. |
| {{ item('rngtech:ascendancy_seal_3') }} | Seal II already used on this machine. |

The Stage 4 requirement means a Steel or better Crusher, Furnace, Alloy Furnace, or Resonance Calibrator, or the Metal Press. The Crude Metal Press (Stage 3) cannot ascend. The Melter counts as Stage 6, and the Forestry Companion always qualifies. Only Seal I checks the stage; Seals II and III do not.

Seals are plain items. A Seal from loot or a quest works the same as a crafted one. Your modpack may turn Seal recipes off. In Creative mode you still need the Seal in your inventory, but it is not used up.

With JEI installed, each Seal has an information page. With Jade installed, looking at a machine while sneaking shows its ascendancy and points.

#### Crafting Seals

Seals are crafted at a crafting table. Seal I needs a calibrated conductive component from the [Resonance Calibrator](resonance-calibrator.md); the Notes column shows the stage and stability it must have.

{{ crafting('rngtech:ascendancy_seal_1', 'rngtech:ascendancy_seal_2', 'rngtech:ascendancy_seal_3') }}

The Seal Cores at the center of each recipe come from the [Component Assembler](component-assembler.md): the {{ item('rngtech:primed_seal_core') }} uses Electrolyte Solution and the {{ item('rngtech:lubricated_seal_core') }} uses Lubricant.

{{ processing("battery_assembly", output="rngtech:primed_seal_core") }}

{{ processing("battery_assembly", output="rngtech:lubricated_seal_core") }}

### Choosing and spending points

On the Mastery tab, the crest beside your start node opens the **Ascendancy panel**. The crest appears on every family that has ascendancies and on any machine that has already earned a tier. Its pips show which Seal tiers the machine has earned, and it glows when a Seal can be used or points are waiting.

1. Press **Ascend** with the next Seal in your inventory. Seal I opens a dialog that previews every ascendancy for the family.
2. Pick one. Its root node is allocated for free.
3. Click nodes to spend your points. Each small node leads to one notable, so 6 points reach at most three notables. A **deep notable** sits one small node beyond a notable.

To refund an ascendancy node, right-click it. It costs **five** Mastery Refunds per node, and you refund from the tips inward. The root cannot be refunded.

**Switch** changes to the family's other ascendancy. It needs an empty tree apart from the root and uses up one Seal I. You keep your earned tiers and points.

If a machine's ascendancy is no longer in the game, for example after a mod update, the machine keeps its earned tiers and can choose a new ascendancy for free, without a Seal.

Ascendancy bonuses show on the machine's Stats tab and in the Bonus Summary, and build codes carry the chosen ascendancy.

### Ascendancies by family

| Family | Ascendancy | Summary |
|---|---|---|
| [Crusher](crusher.md) | Rockbreaker | Makes crushing above your Crush Head's level practical by forgiving missing head levels for time, FE, and jam risk. |
| | Assayer | Keeps a separate bonus-output bank for each input you crush, and can keep those banks when the Crusher is broken. |
| [Furnace](furnace.md) | Crucible Keeper | Heats working lanes past the recipe target for extra speed, at extra FE or fuel, and can hold lane heat while input waits. |
| | Bloomer | Banks part of each ore, raw, or crushed smelt and pays it out as whole extra items, in exchange for 50% less Super Output. |
| [Alloy Furnace](alloy-furnace.md) | Metallurgist | Builds up Flux that occasionally saves one unit of an alloy's largest ingredient. |
| | Blendwright | Speeds up blend recipes, and a failed direct-ingot craft returns a blend; direct-ingot recipes run 15% slower. |
| [Metal Press](metal-press.md) | Die Keeper | Stores several molds and switches to the one that fits the input automatically. |
| | Drop Forge | Presses several plate, gear, or casing jobs as one batch, with a narrower safe heat window. |
| [Resonance Calibrator](resonance-calibrator.md) | Harmonist | Each consecutive calibration of the same family on one pattern raises the stability floor. |
| | Mass Tuner | Spends one catalyst per cycle however many lanes run, at a lower stability ceiling. |
| [Melter](melter.md) | Pressure Vessel | Much bigger tanks and more fluid per melt, at 20% less speed. |
| | Twin Crucible | Melts several sets per cycle, at 15% more energy use. |
| [Forestry Companion](forestry-cart-station.md) | Timber Baron | The logger: banks part of each harvested log and pays it out as extra logs. |
| | Grove Warden | The grower: spends bone meal and FE on growth pulses that push managed saplings along. |
| | Field Hand | The farmer: plants, harvests, and replants crops on farmland instead of trees. Its Sprinkler node waters the rows using a Fluid Pump. |

No ascendancy lets a loop of recipes create items or FE from nothing, and none lowers the inputs a recipe asks for.

## See also

- [Machine Stats](machine-stats.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- [Stages](stages.md)
- [Gear](gear.md)

{{ navbox() }}
