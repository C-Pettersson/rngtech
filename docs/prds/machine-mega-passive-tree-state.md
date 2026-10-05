# Machine Mega Passive Tree Implementation State

Source: [accepted design](machine-mega-passive-tree.md).

Status: **Done** — implementation and travel-road redesign completed 2026-09-27; layered 1300-node redesign and level-100 cap completed 2026-09-28. Gameplay remains a Prototype pending focused playtesting.

## Scope checklist

- [x] Create and switch to feature/machine-mega-passive-tree from clean main.
- [x] Shared stable-ID graph: six starts, layout and validation (750 nodes in version 1, 1300 in version 2).
- [x] Versioned progression, level 100 (80 before 2026-09-28), XP bands and legacy allocation refund migration.
- [x] Three attributes, explicit scaling, absolute constraints and family behaviors.
- [x] Crusher, Furnace and Forestry Companion integration.
- [x] Mastery UI, refunds, ordered copy/paste and automatic allocation.
- [x] Configurator build mode and refund consumable.
- [x] Replace direct cluster links with connected attribute roads and optional reward arcs/rings.
- [x] Verify reward investment costs and update design rules to v6.
- [x] ModDex audit, canonical documentation and representative full-budget builds.
- [x] Layered redesign: outward starts, core pathing, distinct home regions, middle ring, outer field, and design rules v7.
- [x] Larger reward constellations, and keystones spread through every layer, directly on roads or on reward arms, at least 10 points from every start.
- [x] Level 100 with 99 level points and 120-point allocation storage.
- [x] Verification, diff review, and staging on the feature branch.
- [x] Second adapter wave: Alloy Furnace, Metal Press, Melter, and Resonance Calibrator.
- [x] Tagged keystone payoffs, Single Pass, left-side output constellations, Bonus Summary drawer, and color-blind-safe highlight.

## Progress log

- Created the requested branch and migrated from two masks to versioned stable node IDs. Existing XP and earned levels survive; old allocations refund.
- Added three attributes and six starts. Increased/reduced share a bucket, more/less multiply, and absolute constraints resolve last. Irrelevant effects stay selectable and are labelled per effect.
- Integrated the initial three families, Gear-safe refunds, target builds, automatic allocation, and Configurator interaction before machine menu opening.
- Replaced the concentric draft with an asymmetric graph, then rebuilt its topology in response to the reference image: 360 attribute nodes now form connected roads, with 60 reward arcs, 15 open horseshoes, and 15 rings.
- Every notable requires three reward allocations from its road gate; keystones continue those branches at four. Distinct gates have intervening travel nodes. Reward branches cannot shortcut between roads.
- Preserved all 750 stable IDs and authored rewards, including modifier operations. Only grouping, placement, and connections changed in this redesign.
- Updated the design rules, catalog audit, ModDex source export, and generated overview.
- Reworked the Mastery screen for the version 1 catalog: batched, viewport-culled geometry replaces per-pixel fills; icons draw once per texture; allocation, search, and relevance state is cached. Hovering previews the shortest path, clicking allocates it atomically, allocation happens on release, scrolling zooms at the cursor, and right-click refunds now reach the tree.
- Rebuilt the catalog as version 2 in layers: a central hub and core ring, six starts moved outward, distinct home-region road silhouettes between boundary spokes, a polygonal middle ring, outer star hubs, and a curved perimeter road. The catalog grew to 1300 nodes with 627 travel nodes.
- Added wheels, lattices, crowns, hexagram stars, and pearl chains alongside arcs, forks, diamonds, horseshoes, and rings. Notables have unique names and secondary effects; themes are balanced across the tree. Six new keystones bring the total to 30. Keystones moved off the perimeter into every layer: 4 in the core, 9 in home regions, 9 in the outer field, and 8 on or beyond the perimeter road. Twelve attach directly to roads, outer star hubs, or the center junction.
- Rebuilt the center as a three-way junction with equal-length spokes. Silent Operation sits on it, 10 points from every start (previously 13–20), facing the Crusher start.
- Mastery Find now highlights every keystone, notable, or travel node when the query is exactly that type word.
- Raised the level cap to 100 (99 points from levels) with 120-point allocation storage reserved for a future non-level source. XP bands now reach 98. Version 1 allocations, targets, and build codes refund or are rejected.
- Added the second adapter wave on 2026-09-28. The Alloy Furnace shares the Furnace's Drive / Reserve start; the Metal Press and Resonance Calibrator take the Control start; the Melter takes the Reserve / Control start. Each has a Mastery tab, per-family stat applicability and conversions, recipe `machine_xp` with derived bands, JEI XP lines, and pick-block progression. Fixed and capped maximum temperature no longer applies to the Melter, whose recipes all need more heat than those keystones allow.
- Tagged payoffs on 2026-09-28. Low Heat Specialist doubled the speed of every machine its heat limit could not reach, including the Melter after its heat exemption. Effects can now carry a machine tag (heated machines, Crushers, machines with bonus output, machines with an energy buffer), and keystones tag the payoff that belongs with a family-specific cost. Low Heat Specialist and Flash Annealing now use temperature ceilings instead of fixed values, so they no longer raise weak heat sources. Soft Material Specialist speed and Cell Bypass capacity are Crusher-only; Lean Grid's energy reduction needs an energy buffer. Output Amount and Parallel Jobs are now marked Crusher-only, matching the code: the Furnace never read them. A `masteryCheck` audit fails when a keystone's payoff reaches a family that escapes its costs or limits.
- Appended after the original fill without moving or renaming any node: Material Memory and Fine Screens, two Recovery wheels on the left side, and the Single Pass keystone between them (13–36 points from the starts). Single Pass disables bonus output and grants 30% more Processing Speed for machines with bonus output. The catalog now has 1315 nodes and 1464 links.
- 2026-10-03, [Companion Rebalance](companion-rebalance.md): appended four Forestry Companion pockets after the fill, again without moving or renaming any node. Rail Pace (Cart Speed, with the Driven Wheels notable scaling Cart Speed from Drive), Open Track (Idle Cart Speed), and two separate Work Range pockets, Far Rows and Outer Rows, 23 allocations apart. An extra may now replace its pocket's notables. The catalog now has 1336 nodes and 1489 links.
- 2026-10-03: appended two Reservoirs pockets (Fluid Capacity) after the fill: Holding Tanks and Cistern Walls 21 points from the Forestry start, and Deep Tanks and Bulk Reservoir 11 points from the Melter's. The Forestry Companion's sprinkler tank now reads Fluid Capacity. The catalog now has 1346 nodes and 1499 links.
- The expanded view has a Bonus Summary drawer that combines allocated effects as the stat pipeline does, with sections for keystones, conversions, scaling, limits, special behavior, and inactive effects; hovering explains each line and lists its source nodes. Search and relevance highlights use a thicker icy white-blue ring, chosen by simulating protanopia, deuteranopia, and tritanopia against every Mastery accent (red scored among the worst).
- Each of the six starts has its own emblem, colored by its attributes (teal Control, gold Drive, red Reserve): tuning fork (Control), steering wheel (Control / Drive), drive cog (Drive), hearth flame (Drive / Reserve), Leyden jar (Reserve), and valve tap (Reserve / Control). Previously every start drew the Control stat icon. `masteryCheck` now requires every start, like every keystone, to use its own icon.

## Verification

- Gradle quickCheck and ciCheck — passed after the topology change; 435 domain checks including migration, network codec, point budgets, connectivity, target following, attribute suppression, mixed applicability, and modifier/constraint math. Spotless checks passed.
- npm run moddex:check — passed; 55 profiles and 781 recipes. Shared catalog: 750 nodes, 809 links, 272 authoring groups, zero overlaps, links through nodes, or crossings.
- Graph audit verifies travel-only connectivity, single-gate reward components, minimum two-edge gate separation, and reward investment costs. Mutation cases reject a notable shortcut, a cross-pocket bypass, and a travel path that requires a starter to reconnect.
- Keystone shortest paths from all six starts range from 15 to 42 points. Representative builds spend exactly 79 points.
- ModDex fitted Visual Audit passes with zero proper crossings and zero long chords.
- npm run repo:check and mkdocs build --strict — passed after the travel-road redesign.
- Mastery screen rework: Gradle ciCheck passed with 446 domain checks, including shortest paths and atomic rejection of unaffordable, disconnected, or Gear-blocked paths. npm run repo:check, npm run moddex:check, and mkdocs build --strict passed. The rendering and input changes still need an in-game playtest.
- Layered redesign (catalog version 2): Gradle quickCheck and ciCheck passed with 543 domain checks, including the level-100 budget, 120-point target builds, band rescaling, and version 1 refunds. npm run moddex:check and npm run repo:check passed. Catalog audit: 1300 nodes, 1445 links, zero overlaps, links through nodes, or crossings; every start sits within 33%–69% of the bounds; keystones are 10–46 points from every start, and Silent Operation is exactly 10 from each; representative builds spend exactly 99 points. ModDex fitted Visual Audit passes: zero crossings and long chords, P90/median 1.33x, endpoints in seven of eight sectors with a 76° maximum gap. mkdocs build --strict was not run because MkDocs is not installed locally.
- Second adapter wave: Gradle spotlessApply and quickCheck passed with 560 domain checks, including start membership, shared-start allocation retention, per-family conversions, applicability, and Melter absolute-heat exemption. npm run moddex:check and npm run repo:check passed. mkdocs build --strict was not run because MkDocs is not installed locally. In-game checks of the four new Mastery tabs are still needed.
- Tagged payoffs, Single Pass, left-side output, the Bonus Summary drawer, and the highlight: Gradle quickCheck and ciCheck passed with 674 domain checks; npm run moddex:check and npm run repo:check passed. The mega-tree audit passes with 1315 nodes, zero overlaps, links through nodes, or crossings, unchanged start positions, and keystones 10–46 points from every start. mkdocs build --strict was not run because MkDocs is not installed locally. In-game checks of the drawer, tooltips, and highlight are still needed.

## Handoff and limits

- Work is committed on feature/machine-mega-passive-tree and merges into main as part of the planned 2.0 release. The full in-game release checklist in docs/releasing.md runs once for 2.0 after the remaining 2.0 features land.
- Start emblems and the getting-started rewrite (2026-09-29): Gradle quickCheck and ciCheck passed with 753 domain checks; npm run moddex:check, npm run repo:check, and mkdocs build --strict passed.
- Reserve-start generator and storage adapters and family ascendancies are intentionally deferred. Legacy graph definitions remain as reference/audit fixtures.
- In-game controls, multiplayer UI synchronization, refund inventory consumption, and late-game XP/balance need focused playtesting. Automated checks do not replace those checks.
- Runtime catalog: src/main/resources/data/rngtech/mastery/machine_tree.json. Author rewards with tools/moddex/author-mega-tree.mjs and roads/reward placement with layout-mega-tree.mjs. Regenerate the audit report and overview after changes.
