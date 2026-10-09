# Machine Mastery

Status: Prototype

Player guide: [Machine Mastery](https://c-pettersson.github.io/rngtech/machine-mastery/)

Machine Mastery is progression owned by each machine chassis. Machines share one passive graph and enter it at different starting positions. The [implementation matrix](../reference/current-implementation.md) records the current adapters; generator and storage adapters are deferred. Each family also has [ascendancies](#ascendancies): small specialization trees unlocked with Ascendancy Seals.

## Shared tree

The catalog contains 1346 nodes: six starts, 634 attribute travel nodes, 480 ordinary nodes, 195 notables, and 31 keystones, joined by 1499 links. It is built in layers. Inside the six starts, a central junction splits three ways to a core ring, so every machine can path through the middle and reaches the center for the same number of points. Each start opens into a home region with its own road shape between two boundary spokes. A middle ring leads through outer star hubs and climbs to the perimeter road.

Optional reward constellations branch from single road gates: arcs, forks, diamonds, pearl chains, horseshoes, rings, wheels, lattices, crowns, and hexagram stars. Distinct constellations are separated by travel steps. Each notable requires at least three reward allocations from its road gate, and constellations cannot shortcut between roads. Keystones are spread through every layer, either directly on a road or at the end of a reward arm, and always at least 10 points from every start. Silent Operation sits on the center junction, 10 points from every start. Attribute Transfiguration, Singular Drive, and Reserve Actuation sit around the core, where any start can path inward to them. Family keystones sit in the home regions, outer field, and perimeter near their family's start. Output constellations appear on both sides of the tree: Material Memory and Fine Screens mirror the Drive region's Recovery wheels on the Control and Reserve / Control side, with Single Pass on the road between them.

Every paid allocation costs one point and must connect to the machine's start through allocated nodes. Foreign starts cannot be allocated or used as shortcuts. Nodes have no individual level or chassis-stage gates.

![Shared machine passive tree](../assets/machine-mega-passive-tree.svg)

| Starting archetype | Emblem | Initial attributes | Machine adapters |
| --- | --- | --- | --- |
| Control | Tuning fork | 20 Control | Metal Press, Resonance Calibrator |
| Control / Drive | Steering wheel | 10 Control, 10 Drive | Forestry Companion |
| Drive | Drive cog | 20 Drive | Crusher |
| Drive / Reserve | Hearth flame | 10 Drive, 10 Reserve | Furnace, Alloy Furnace |
| Reserve | Leyden jar | 20 Reserve | Deferred |
| Reserve / Control | Valve tap | 10 Reserve, 10 Control | Melter |

Each start draws its emblem in its attributes' colors: teal for Control, gold for Drive, and red for Reserve. Mixed starts combine both colors. The Reserve start is present in the graph but has no machine adapter yet. Machines that share a start keep their own progression, but build codes paste between them. Gear and cables do not gain their own Mastery progression.

All nodes remain visible and selectable regardless of machine family. Applicability is evaluated for each effect. A Crusher can allocate maximum heat and gain no heat benefit. An electric Furnace gains no fuel-duration benefit. A node with an irrelevant benefit still applies any relevant penalty; tooltips label inactive effects. The reverse does not happen: a keystone payoff that depends on a family-specific cost is [tagged](#tagged-payoffs), so a machine that escapes the cost does not get the payoff. Relevance highlighting never hides paths.

## Attributes

Control, Drive, and Reserve are point totals. Travel nodes grant flat points; dedicated clusters increase individual attributes or all three attributes. Attribute increases use the same modifier rules as other stats.

Inherent conversions use final, nonnegative attribute totals:

| Attribute | Shared conversion | Family conversion |
| --- | --- | --- |
| Control | 0.1% increased Stability and 0.02% reduced Energy Usage per point | Furnace, Alloy Furnace, and Metal Press: 0.05% increased Temperature Stability per point; Resonance Calibrator: 0.05% increased Calibration Precision per point |
| Drive | 0.15% increased Processing Speed per point | Forestry: +1 Tree Fell Limit per complete 20 points |
| Reserve | Every machine except Forestry: 0.25% increased Energy Capacity per point | Furnace and Alloy Furnace: 0.1% increased Heat Isolation per point; Melter: 0.1% increased Fluid Transfer per point; Forestry: +1 managed-cell maximum per complete 4 points |

Conversions only have a gameplay effect where the receiving stat is used. For example, the primitive fuel Furnace has no FE buffer to benefit from Reserve's energy capacity conversion. Gear and chassis remain the main sources of processing capability.

Explicit attribute scaling is separate. A node can grant an authored percentage per attribute point in addition to inherent conversions. **Attributes grant no inherent bonuses** removes only the conversions in the table: totals, attribute modifiers, and explicit scaling remain. Attribute Transfiguration grants 100% more of each attribute with that restriction. Reserve Actuation uses the same restriction and grants 0.5% increased Processing Speed per Reserve.

Steady State grants 50% more Stability with 15% less Processing Speed. Singular Drive grants 50% more Drive with 50% less Control and Reserve. Lean Grid applies 30% less Energy Usage for machines with an energy buffer and 50% less Energy Capacity. Single Pass disables bonus output and grants 30% more Processing Speed for machines with bonus output. Family keystones are described on each machine page.

Tool Control retains its separate durability mechanic; these machine conversions do not change tools.

## Tagged payoffs

A tagged effect applies only to machines in its group, and only where the machine uses the stat. Tooltips show the group and its members, and mark the effect inactive on other machines.

| Tag | Machines | Used by |
| --- | --- | --- |
| Heated machines | Furnace, Alloy Furnace, Metal Press | Low Heat Specialist and Flash Annealing speed |
| Crushers | Crusher | Soft Material Specialist speed, Cell Bypass Energy Capacity |
| Machines with bonus output | Crusher, Furnace, Alloy Furnace, Metal Press, Resonance Calibrator | Single Pass speed |
| Machines with an energy buffer | Every family except the Forestry Companion | Lean Grid Energy Usage reduction |

Group membership follows each machine's real stat surface. Heated machines are those whose maximum temperature accepts absolute limits; the Melter is excluded because every Melter recipe needs more heat than those limits allow. Machines with bonus output use Output Amount or Super Output. Only the Crusher reads Output Amount; the other members produce bonus output through Super Output.

Single Pass disables bonus output: Output Amount cannot exceed the base output, and Super Output and Crusher salvage chances drop to zero. It never raises a penalized Output Amount, such as a Crusher running without a Battery Cell.

Untagged penalties still apply everywhere. Flash Annealing's 50% more Energy Usage and Soft Material Specialist's 50% more Energy Usage reach every machine that allocates them, as the Closed Loop Recuperator's speed penalty does.

## Modifier keywords and hard constraints

The canonical operation definitions are in [Affix Generation](affix-generation.md#modifier-operations).

Two 50% reduced modifiers add to 100% reduced. Two 50% less modifiers leave 25%. A 100% less modifier leaves zero even with increased modifiers. “More” and “less” are written as percentages in tooltips and stored as factors in the catalog: `1.5` means 50% more; `0.75` means 25% less.

Fixed values and hard ceilings resolve after ordinary modifiers, including Gear. A fixed maximum temperature of 800 sets it to 800; a ceiling of 800 only prevents exceeding 800. The lower value wins when absolute maximum values conflict. Keystone heat limits are ceilings, so Low Heat Specialist (800) and Flash Annealing (600) never raise a weaker heat source to their limit. A recipe hardness ceiling rejects harder Crusher recipes regardless of installed head or processing level; ordinary under-level recipes retain their existing penalty behavior when no hard ceiling forbids them.

## XP and chassis ownership

Level 1 starts with no points; each level grants one point, so level 100 grants 99 points. Allocation storage, target builds, and build codes hold up to 120 points; the remaining 21 are reserved for a future non-level source. Levels 1–30 retain their previous cumulative XP thresholds. Each cumulative threshold after level 30 is the preceding threshold multiplied by 1.10 and rounded. This is a long-term progression curve, still subject to gameplay balance testing.

Successful work grants `base machine_xp × (1 + floor(band² / 20))`, followed by the band falloff below. Dense Crusher batches and multi-lane Resonance Calibrators award XP per completed job. Failed work, idle placement, output waits, and recipes authored with zero machine XP grant none.

| Machine level relative to work band | XP awarded |
| --- | ---: |
| At or below band | 100% |
| One above band | 50% |
| Two above band | 25% |
| Three or more above band | 0% |

Quarter-XP remainders persist. Positive-XP Crusher recipes derive bands from hardness, and Melter recipes from required processing level on the same scale. Furnace, Alloy Furnace, and Metal Press recipes derive them from target heat, and calibration recipes from the required calibrator stage. Their old 1–25 bands map to 1–98 with `min(98, 1 + floor((oldBand − 1) × 97 / 24))`. An explicitly authored `machine_xp_band` uses the new level scale directly. Forestry uses `min(98, 12 + floor(sqrt(Tree Fell Limit)) × 10)` for completed planting, leaf cleanup, and harvesting actions. Stronger work capability raises its training ceiling. Work in band 98 can eventually reach level 100.

Progression survives supported drops, pick-block, and replacement through `rngtech:machine_progression`. It does not transfer to independently crafted higher-stage chassis. Existing saves preserve XP and earned levels while refunding old machine-specific allocations. New saves store versioned stable node IDs and ordered target builds instead of two node masks. Catalog version 2 replaced the version 1 layout, so version 1 allocations and target builds are refunded and version 1 build codes are rejected.

## Allocating, refunding, and copying

The player guide covers the Mastery screen workflow, controls, Bonus Summary, copying, and the Configurator. Implementation rules it leaves out:

- Find matches and relevance highlights use a thick icy white-blue ring. Under simulated protanopia, deuteranopia, and tritanopia it stayed the most distinct from every Mastery accent; red blends into the orange heat accents under red-green color blindness.
- A path allocation is one server action, so a partly affordable or partly Gear-illegal path allocates nothing.
- A Gear conflict pauses build following before the illegal allocation. `MasteryBuildCode.copy` encodes the target build when one exists, otherwise the allocated nodes.
- Shift-click Copy sends `copy_configurator`, which writes the build to the first Configurator in the player inventory and sets `MASTERY_CONFIGURATOR_MODE`. Entity hosts such as the Forestry Companion route Configurator use through `ConfiguratorItem.useMastery`.

## Ascendancies

Status: Prototype. Ascendancies still need an in-game playtest, and their node values are subject to balance testing.

An ascendancy is a family-specific specialization for one machine, modeled on Path of Exile ascendancy classes. Every Mastery family has two; the Forestry Companion has three. A machine chooses one when it uses its first Ascendancy Seal, then spends ascendancy points in that ascendancy's small tree. Ascendancy points are separate from the shared tree's points and allocations.

| Family | Ascendancies |
| --- | --- |
| [Crusher](../content/crusher.md#ascendancies) | Rockbreaker, Assayer |
| [Furnace](../content/furnace.md#ascendancies) | Crucible Keeper, Bloomer |
| [Alloy Furnace](../content/alloy-furnace.md#ascendancies) | Metallurgist, Blendwright |
| [Metal Press](../content/metal-press.md#ascendancies) | Die Keeper, Drop Forge |
| [Resonance Calibrator](../content/resonance-calibrator.md#ascendancies) | Harmonist, Mass Tuner |
| [Melter](../content/melter.md#ascendancies) | Pressure Vessel, Twin Crucible |
| [Forestry Companion](../content/tree-farm-automation.md#ascendancies) | Timber Baron, Grove Warden, Field Hand |

### Seals and tiers

Each Ascendancy Seal grants 2 ascendancy points to one machine, for at most 6 across three ordered tiers. Seal I requires entry stage 4 or higher: the chassis stage for block machines, 3 for the Crude Metal Press, 4 for the Metal Press, and 6 for the Melter. The Forestry Companion has no chassis stage and always meets the gate. `AscendStatus` checks the gate only for tier 1.

Seals carry no data, so Seals from loot tables, quests, or commands work the same as crafted ones. Pack makers can turn Seal recipes off with the `ascendancy.sealRecipesEnabled` common config key or the `rngtech:ascendancy_seal_recipes_enabled` recipe condition. Crafting the Seal is the only trial; there is no work requirement.

### Choosing, allocating, and switching

The player guide covers the Ascendancy panel, crest, Ascend, refunds, Switch, Creative Seal use, and free re-choice after a retired ascendancy. Implementation notes:

- `AscendancyPanel.available()` shows the crest when the family has catalog ascendancies or the state has earned tiers.
- A tier without a valid catalog ascendancy reports `AscendStatus.FREE_CHOICE`, and `choose_ascendancy` then picks without consuming a Seal.

Ascendancy effects use the same modifier rules and stat pipeline as the shared tree. Granted ascendancy stats appear on the Stats tab only when they apply, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines each one. Jade ascendancy lines and the JEI Seal information entry are described on the player guide.

Ascendancy state lives in `rngtech:machine_progression`, so it survives drops, pick-block, and the Forestry Companion's item form. Like the rest of Mastery, it does not transfer to a newly crafted chassis. Build codes carry the ascendancy and its allocation order. Paste allocates ascendancy nodes only onto the same ascendancy with none allocated, as far as its earned points reach.

### Yield and loops

No mix of recipes may return every consumed input while gaining items or producing net FE, including through ascendancies. Every yield effect honors a recipe's `bonus_output` opt-out, no node lowers authored recipe input counts, and no node raises Refinement Potential. The recipe loop audit in `npm run moddex:check` is a CI gate, and every declared yield stat or behavior must be covered by one of its bounds. See the [ascendancies PRD](../prds/machine-ascendancies.md#loop-prevention) for the audit's model.

## Authoring and verification

The checked-in runtime catalog is `src/main/resources/data/rngtech/mastery/machine_tree.json`. `tools/moddex/layout-mega-tree.mjs` deterministically lays out the road layers, keystone placements, and reward constellations without crossings; `author-mega-tree.mjs` assigns attributes, themes, notable names, special notables, and keystones, then writes the catalog and language entries. Later additions go in its `EXTRAS` list, which the layout places after the original fill so existing node IDs, positions, and links stay stable. ModDex reads this catalog directly; its JSON draft and Java layout exports remain authoring views rather than runtime catalog writers. Follow [Passive Tree Design Rules](../reference/passive-tree-design-rules.md) when changing geometry or connections.

Run `node tools/moddex/check-mega-tree.mjs --report` to refresh the [audit report](../reference/machine-mega-tree-audit.json). It includes paths from all starts and representative 99-point builds combining local investment with distant keystones. `masteryCheck`, included in `quickCheck` and `ciCheck`, checks modifier math, migration, build ordering, connectivity, point budgets, attribute suppression, hard constraints, tagged payoffs, the bonus summary, and a keystone audit that fails when a payoff reaches a machine that escapes the keystone's cost. `npm run moddex:check` checks catalog geometry and export consistency.

Ascendancies are data-driven. `data/rngtech/mastery/ascendancies/index.json` lists one file per ascendancy in display order, and each file holds its family and nodes. New stats and behaviors are declared, with the families they reach and their yield kind, in `data/rngtech/mastery/declarations.json`. The catalog validates tree shape and family support on load and is not datapack-overridable. `masteryCheck` validates every shipped ascendancy, runs test-only fixture ascendancies, and checks language keys and icons. `node tools/moddex/export-ascendancy-data.mjs --write-docs` regenerates the node tables on each family page, and `npm run moddex:check` fails when they are stale or a declared stat has no [Machine Stats](../reference/machine-stats.md#ascendancy-stats) row.

The implementation has automated domain and graph coverage. In-game interaction, multiplayer synchronization, and late-game balance still need focused playtesting.
