# Machine Mastery

Status: Prototype

Machine Mastery is progression owned by each machine chassis. Machines share one passive graph and enter it at different starting positions. The [implementation matrix](../reference/current-implementation.md) records the current adapters; additional machines and family ascendancies are deferred.

## Shared tree

The catalog contains 750 nodes: six starts, 360 attribute travel nodes, 240 ordinary nodes, 120 notables, and 24 keystones. Its 809 links form an asymmetric attribute-road network with 90 optional reward pockets. Distinct pockets are separated by travel steps; each notable requires three reward allocations from its road gate, and keystones continue those investments at four. Reward rings and open arcs cannot shortcut between roads. Every paid allocation costs one point and must connect to the machine's start through allocated nodes. Foreign starts cannot be allocated or used as shortcuts. Nodes have no individual level or chassis-stage gates.

![Shared machine passive tree](../assets/machine-mega-passive-tree.svg)

| Starting archetype | Initial attributes | Initial machine adapter |
| --- | --- | --- |
| Control | 20 Control | Deferred |
| Control / Drive | 10 Control, 10 Drive | Forestry Companion |
| Drive | 20 Drive | Crusher |
| Drive / Reserve | 10 Drive, 10 Reserve | Furnace |
| Reserve | 20 Reserve | Deferred |
| Reserve / Control | 10 Reserve, 10 Control | Deferred |

The three unused starts are present in the graph. They do not yet have machine adapters. Gear and cables do not gain their own Mastery progression.

All nodes remain visible and selectable regardless of machine family. Applicability is evaluated for each effect. A Crusher can allocate maximum heat and gain no heat benefit. An electric Furnace gains no fuel-duration benefit. A node with an irrelevant benefit still applies any relevant penalty; tooltips label inactive effects. Relevance highlighting never hides routes.

## Attributes

Control, Drive, and Reserve are point totals. Travel nodes grant flat points; dedicated clusters increase individual attributes or all three attributes. Attribute increases use the same modifier rules as other stats.

Inherent conversions use final, nonnegative attribute totals:

| Attribute | Shared conversion | Family conversion |
| --- | --- | --- |
| Control | 0.1% increased Stability and 0.02% reduced Energy Usage per point | Furnace: 0.05% increased Temperature Stability per point |
| Drive | 0.15% increased Processing Speed per point | Forestry: +1 Tree Fell Limit per complete 20 points |
| Reserve | Crusher and Furnace: 0.25% increased Energy Capacity per point | Furnace: 0.1% increased Heat Isolation per point; Forestry: +1 managed-cell maximum per complete 4 points |

Conversions only have a gameplay effect where the receiving stat is used. For example, the primitive fuel Furnace has no FE buffer to benefit from Reserve's energy capacity conversion. Gear and chassis remain the main sources of processing capability.

Explicit attribute scaling is separate. A node can grant an authored percentage per attribute point in addition to inherent conversions. **Attributes grant no inherent bonuses** removes only the conversions in the table: totals, attribute modifiers, and explicit scaling remain. Attribute Transfiguration grants 100% more of each attribute with that restriction. Reserve Actuation uses the same restriction and grants 0.5% increased Processing Speed per Reserve.

Hover the machine's starting node to see current attribute totals. Tool Control retains its separate durability mechanic; these machine conversions do not change tools.

## Modifier keywords and hard constraints

The canonical operation definitions are in [Affix Generation](affix-generation.md#modifier-operations).

Two 50% reduced modifiers add to 100% reduced. Two 50% less modifiers leave 25%. A 100% less modifier leaves zero even with increased modifiers. “More” and “less” are written as percentages in tooltips and stored as factors in the catalog: `1.5` means 50% more; `0.75` means 25% less.

Fixed values and hard ceilings resolve after ordinary modifiers, including Gear. A fixed maximum temperature of 800 sets it to 800; a ceiling of 800 only prevents exceeding 800. The lower value wins when absolute maximum values conflict. A recipe hardness ceiling rejects harder Crusher recipes regardless of installed head or processing level; ordinary under-level recipes retain their existing penalty behavior when no hard ceiling forbids them.

## XP and chassis ownership

Level 1 starts with no points; level 80 grants 79 points. Levels 1–30 retain their previous cumulative XP thresholds. Each cumulative threshold after level 30 is the preceding threshold multiplied by 1.10 and rounded. This is a long-term progression curve, still subject to gameplay balance testing.

Successful work grants `base machine_xp × (1 + floor(band² / 20))`, followed by the band falloff below. Dense Crusher batches award XP per completed job. Failed work, idle placement, output waits, and recipes authored with zero machine XP grant none.

| Machine level relative to work band | XP awarded |
| --- | ---: |
| At or below band | 100% |
| One above band | 50% |
| Two above band | 25% |
| Three or more above band | 0% |

Quarter-XP remainders persist. Positive-XP Crusher recipes derive bands from hardness; Furnace recipes derive them from target heat. Their old 1–25 bands map to 1–78 with `min(78, 1 + floor((oldBand − 1) × 77 / 24))`. An explicitly authored `machine_xp_band` uses the new level scale directly. Forestry uses `min(78, 12 + floor(sqrt(Tree Fell Limit)) × 8)` for completed planting, leaf cleanup, and harvesting actions. Stronger work capability raises its training ceiling. Work in band 78 can eventually reach level 80.

Progression survives supported drops, pick-block, and replacement through `rngtech:machine_progression`. It does not transfer to independently crafted higher-stage chassis. Existing saves preserve XP and earned levels while refunding old machine-specific allocations. New saves store versioned stable node IDs and ordered target builds instead of two node masks.

## Allocating, refunding, and copying

Open Mastery to browse the tree. Drag to pan and scroll to zoom at the cursor; Shift-scroll pans sideways and Ctrl-scroll pans vertically. Toolbar controls expand the view, fit the tree, or return home. Find searches names, IDs, and stat effects, shows the match count, and rings every match so it stays visible when zoomed out; Enter cycles through matches. The relevance control highlights useful nodes. Hover nodes for exact effects, constraints, and refund instructions.

Hovering an unallocated node previews the shortest route from your allocations and its point cost. Left-click allocates the whole route in one server action when you can afford it and installed Gear allows every node on it; otherwise nothing is allocated and the tooltip explains why. Allocation happens on release, so a drag that starts on a node only pans. Right-click an allocated node to refund it. Each removed node costs one **Mastery Refund**, crafted eight at a time from paper, redstone, and a copper ingot. Refunds must leave every remaining allocation connected and keep installed Gear legal. Shift-click Clear pays the same per-node price for the whole tree. Creative players do not consume refunds.

Copy stores an ordered build code in the clipboard. Paste requires an empty tree with the same start. It allocates what the destination can afford, then follows the remaining order as that chassis earns points. Purple outlines show target nodes. Pause/Resume controls following. Gear conflicts pause before the illegal allocation; resolve the conflict and resume. Manual refunds pause following. Copying a machine already following a target copies the complete target.

The Configurator has a dedicated Mastery mode. Sneak-use in air toggles between connector and Mastery modes. In Mastery mode, sneak-use a supported machine to copy and use normally to paste. Shift-click Copy in the Mastery screen stores the build on an inventory Configurator and selects Mastery mode. Entity companions use the same workflow.

## Authoring and verification

The checked-in runtime catalog is `src/main/resources/data/rngtech/mastery/machine_tree.json`. `tools/moddex/author-mega-tree.mjs` deterministically authors its rewards and node names; `layout-mega-tree.mjs` authors connected attribute roads with optional reward arcs, horseshoes, and rings, without crossings. ModDex reads this catalog directly; its JSON draft and Java layout exports remain authoring views rather than runtime catalog writers. Follow [Passive Tree Design Rules](../reference/passive-tree-design-rules.md) when changing geometry or routes.

Run `node tools/moddex/check-mega-tree.mjs --report` to refresh the [audit report](../reference/machine-mega-tree-audit.json). It includes paths from all starts and representative 79-point builds combining local investment with distant keystones. `masteryCheck`, included in `quickCheck` and `ciCheck`, checks modifier math, migration, build ordering, connectivity, point budgets, attribute suppression, and hard constraints. `npm run moddex:check` checks catalog geometry and export consistency.

The implementation has automated domain and graph coverage. In-game interaction, multiplayer synchronization, and late-game balance still need focused playtesting.
