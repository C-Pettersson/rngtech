# Passive Tree Design Rules v7

Status: Implemented

Design standard for passive-tree authoring. See [Design Proposals](../design-proposals.md) for broader framework requirements.

Scope: the shared [Machine Mastery catalog](../systems/machine-mastery.md) and retained legacy typed-tree references. The shared catalog supersedes the per-machine enum/mask format.

## Design target

Use Path of Exile as a reference for spatial grammar: distinct layers of attribute roads, junctions, and star hubs, with varied reward constellations branching into the spaces between roads. Keep RNGTech names, icons, mechanics, and artwork distinct.

The shared tree reads from the center outward:

1. A central junction splits three ways to the core ring. Each branch serves two neighboring starts through spokes of equal step count, so every start pays the same number of points to reach the center.
2. Six starts sit on a start ring outside the core, so the middle of the tree is travel rather than starting territory.
3. Each start owns a home region between two boundary spokes. Every home region has its own road silhouette: lattice, trident, loop, bridge, fan, or star hub.
4. A polygonal middle ring separates the home regions from the outer field.
5. The outer field has star hubs and bowed climbs up to a curved perimeter road.
6. Outer constellations sit on and beyond the perimeter.

Keystones are spread through every layer, from the core to beyond the perimeter, so a build can commit by pathing inward, sideways, or outward. Reaching the outer ring must not be the default route to every keystone.

Travel must consume allocations. Moving between layers should mean following attribute nodes along a spoke, loop, hub, or climb, then deciding whether to leave the road and invest in a reward branch. A sequence of directly connected reward wheels fails this design even when its geometry is tidy.

Layers are structure, not repetition. Vary road silhouettes, junction radii, hub placement, reward shapes, and open space between regions. Avoid identical clusters in concentric bands, mirrored sectors, and identical keystone pairs.

The fitted render decides whether the composition works. Counts cannot overrule a poor render.

## Why the rules changed

Version 3 rewarded density and produced tangled links. Version 4 added a fitted-view gate. Version 5 removed the shared tree's symmetry but still placed its travel nodes inside repeated reward loops. Version 6 separated the travel network from optional reward investments, but clustered all six starts in the middle of a random road web.

Version 7 moves the starts outward to leave room for core pathing, builds the road network in distinct layers with varied travel between them, adds larger reward constellations, and spreads keystones through every layer, directly on roads or at the end of reward arms, away from the starts. The catalog grows to 1300 nodes, and machines reach level 100.

## Runtime and topology constraints

- Preserve stable string IDs. Array order is only a synchronized catalog index, never save identity. A layout that renames nodes must bump the catalog version so saved allocations refund.
- Target about 1300 shared nodes, with approximately half granting travel attributes. The runtime guard allows up to 4096.
- Levels grant 99 points by level 100. Allocation storage and target builds hold 120 points for future non-level sources.
- Use six starters, each with three exits: one inward to the core ring and two into its home region. A machine receives only its family's start for free. Foreign starts cannot be traversed.
- Keep every start within 30%–70% of the occupied bounds on both axes.
- Make every non-starter reachable from each start without crossing another starter.
- Limit travel grants to Control, Drive, and Reserve. Hybrid starts do not add attributes. Hybrid regions alternate attributes by road, not by node, and boundary spokes carry the attribute both neighbors share.
- Connect all travel nodes using travel nodes alone. Removing reward nodes and starters must leave one connected road network.
- Attach each reward component to one road gate. Multiple open arms may share a gate; reward branches must not form shortcuts between roads.
- Separate distinct reward gates by at least two travel edges, so even neighboring investments have intervening travel.
- Require at least three reward allocations from a road gate to a notable. Ordinary bonuses precede the notable on each approach.
- Keystones may attach directly to a travel node or end an invested reward arm. Every keystone must be at least 10 and at most 99 allocations from every start, so no start region begins beside a commitment. Keep keystones at least 280 units apart.
- Give travel dead ends a reward destination. Do not add empty spurs to meet a node-count target.

| Kind | Degree cap |
| --- | ---: |
| STARTER | exactly 3 |
| TRAVEL | 4 |
| NODE | 3 |
| NOTABLE | 2 |
| KEYSTONE | exactly 1 |

Released legacy masks and earlier catalog versions are refunded during migration, preserving XP and levels. They do not constrain the shared graph. Nodes have no individual level or chassis-stage gates.

## Composition and investment

### Lay out roads first

Place the layers before any reward. Connect the core ring to each start's inward exit and to the boundary spokes, and split the center three ways with spokes of equal step count. Give each home region a distinct silhouette with lateral links to its boundary spokes. Join spoke ends and region arrivals with the polygonal middle ring. Climb from the middle ring to the perimeter through star hubs and bowed roads at irregular angles.

A junction that needs more than four roads becomes a roundabout: a small ring of degree-limited travel nodes, each carrying at most one road.

Road spacing and reward spacing serve different purposes. Roads need room to read across the map; rewards sit closer together in constellations. Long roads are allowed when they contain visible, paid travel steps. A single map-spanning edge that bypasses that investment is not a road.

### Branch rewards into the open spaces

Fill the widest open spaces first, then tighter gaps. Use a mixture of:

- open arcs, forks, diamonds, and pearl chains with notables along or at the end of the approach;
- open horseshoes and single-entry rings with notables on the far side;
- wheels with a central notable;
- three-by-three lattices entered from two corners, with notables across the far row;
- crowns: two climbing arms that meet in a scalloped row of three notables;
- hexagram stars with notables on three tips.

A constellation may reconnect its own arms locally. It must not connect to a second road and become the cheapest path between specialties. Do not connect tips merely to increase cycle rank.

Vary orientation, mirroring, radius, and spacing. Large showpiece shapes belong in large cells; compact shapes fill the gaps. Leave clear space between reward constellations and the roads that carry the player across the tree.

### Make choices compete

Repeat broad goals across several regions: processing speed, energy efficiency, output handling, attribute scaling, and reserve support should offer more than one route package. Home regions carry their start's themes; outer constellations blend a region with its nearest neighbor, so nearby road gates present competing investments. Balance themes across the whole tree rather than per region.

Keep the distinction between increased/reduced, more/less, flat additions, and absolute constraints. Changing a path must not silently change the meaning of its rewards.

### Place keystones as commitments

Distribute keystones across the layers: a few in the core for any start that paths inward, some in each home region's outer half, more in the outer field and on outer star hubs, and only a few on the perimeter. Stagger their angles between layers so commitments do not line up along spokes. General keystones belong in the core. A keystone on the center junction is equally far from every start; when distances tie, it faces the Crusher's Drive start. Family keystones sit near the start whose family they suit. A keystone may sit directly on a road, but it must stay at least 10 allocations from every start. All starting archetypes must be able to reach each keystone within the level-100 budget. Check representative builds that combine a nearby commitment with a distant specialty.

## Validation and fitted-view gate

Run the catalog audit and load the regenerated source in ModDex before handoff. Inspect the fitted Visual Audit as well as a zoomed section showing road steps and reward approaches.

The shared graph must have zero overlapping node bounds, links through unrelated nodes, exact duplicate links, degree violations, and proper link crossings. Validate travel-only connectivity, one-gate reward components, gate separation, notable investment, keystone start distances, and start positions from the actual links and coordinates; group labels are not proof.

Use the median cross-group link length for the fitted long-chord threshold: max(240 px, median * 2.75). Keep the long-link allowance at max(3, round(edgeCount * 0.06)) and P90 below the threshold. Subdivided roads pass because the travel is paid in visible steps. The final link to a degree-one keystone is excluded from this chord calculation.

Endpoints should occupy at least five of eight angular sectors, with no empty arc larger than 120 degrees. Do not add a seventh synthetic starter when importing the catalog.

The current catalog has 1300 nodes and 1445 links: 6 starts, 634 travel nodes, 446 ordinary nodes, 184 notables, and 30 keystones. Its cycle rank is 146. Reward constellations use 120 road gates: 17 wheels, 4 stars, 4 crowns, 3 lattices, 6 rings, 11 horseshoes, 21 chains, 12 forks, 10 diamonds, 2 arcs, and 18 keystone arms. Twelve more keystones sit directly on roads, outer star hubs, or the center junction. By radius, 4 keystones are in the core, 9 in home regions, 9 in the outer field, 4 on the perimeter road, and 4 beyond it. Notables cost three to seven reward points from their gates. Keystones are 10 to 46 points from each start; Silent Operation, on the center junction, is exactly 10 from every start. The [generated audit](machine-mega-tree-audit.json) records the exact geometry, distances, and representative 99-point builds.

The legacy small-tree review ranges remain useful for the historical reference only: node cycle rank 15–30%, bridge edges up to 20% excluding keystone endpoints, degree-two corridors up to five nodes, and articulation review above 25%. For its 25–35 groups, review cross-group ratios of 25–40%, group cycle rank at least max(8, ceil(groupCount * 0.25)), group articulation up to 35%, group bridges up to 20%, and low-degree group corridors up to three groups. These are not shared-tree targets.

## Authoring workflow

1. Place the hub, core ring, starts, boundary spokes, and home-region silhouettes.
2. Add the middle ring, outer star hubs, climbs, and perimeter road, with paid intermediate steps.
3. Place keystones, then fill open spaces with reward constellations, largest cells first.
4. Assign themes, notable names, special notables, and keystones without altering modifier keyword semantics.
5. Regenerate the runtime catalog, ModDex source export, audit report, and overview.
6. Check investment costs, keystone distances, runtime reachability, geometry, and the fitted render.

`tools/moddex/layout-mega-tree.mjs` owns steps 1–3, and `author-mega-tree.mjs` owns step 4. Do not add links to chase cycle rank or reduce bridge counts after the composition works.

## Historical Forestry Companion reference result

This retained 100-node definition documents the v4 reference, not the graph currently used in gameplay. The shared catalog, [audit report](machine-mega-tree-audit.json), and [overview](../assets/machine-mega-passive-tree.svg) are the current source for all three adapters.

The Forestry Companion layout follows the [concept image](passive-tree-concept.png): a central start, an irregular field of connected constellations, larger notable landmarks, and five keystones around the perimeter. All 100 node ids, enum ordinals, effects, and level requirements are preserved. Routes have been re-authored, so the paths to future allocations change while saved allocations keep their identities.

Small local loops and open crescents connect adjacent regions. Forestry uses display diameters of 12 for travel nodes, 16 for ordinary nodes, 48 for notables, 60 for keystones, and 40 for the starter. These sizes are specific to Forestry and are included in geometry checks and the ModDex export. The shared in-game view uses circular frames, faint background rings, and a full-tree fit when expanded; collapsing restores the previous view.

Seventy-six connections follow their group's orbit. These curves connect adjacent nodes on the same orbit with a sweep of at most 120 degrees. Other connections stay straight. The in-game renderer, ModDex, and geometry validators use the same sampled paths. Moving an entire imported group keeps its curves; changing individual positions removes curves whose original orbit geometry no longer matches.

| Metric | Previous fitted candidate | Current layout |
| --- | ---: | ---: |
| Nodes | 100 | 100 |
| Groups | 29 | 29 |
| Undirected edges | 121 | 122 |
| Node cycle rank | 22 | 23 |
| Group edges | 37 | 39 |
| Group cycle rank | 9 | 11 |
| Same-group links | 82 | 81 |
| Cross-group links | 39 | 41 |
| Proper crossings | 3 | 0 |
| Long internal chords | 3 | 0 |
| Internal edge P90 / median | 1.97x | 1.39x |
| Endpoint angular sectors | 6 of 8 | 6 of 8 |
| Largest empty endpoint arc | 114 degrees | 89 degrees |

The tree contains one starter, 48 travel nodes, 32 ordinary nodes, 14 notables, and five keystones. Geometry checks report no node overlaps, links through nodes, duplicate links, or degree-cap violations. The starter sits at 47% of both occupied axes. Keystone distances are 11 for Coasting Clutch, 10 for Manual Throttle, 11 for Serrated Leaf Protocol, 10 for Magnet Mode, and 7 for Seedling Magnet.

The graph has 18 articulation nodes and 15 bridge edges excluding final keystone links. Its longest degree-two corridor has five nodes. The group graph has nine articulation groups, one bridge excluding endpoint groups, and no multi-group low-degree corridor. These values fall within the review ranges above.

Run `node tools/moddex/check-passive-tree.mjs` to regenerate and validate the actual Java export. `npm run moddex:check` also runs this audit, including the same fitted-view metrics as ModDex. Compilation alone does not execute the Java definition's static validation.

The fitted ModDex render below uses the implemented Java coordinates, display sizes, and connections. The game uses its existing mastery icon textures in the node frames.

![Forestry Companion fitted passive tree](../assets/forestry-passive-tree-layout.png)

## Handoff checklist

Include these results with a new or remediated tree:

- node and kind counts;
- group and edge counts;
- degree-cap violations;
- keystone shortest-path distances;
- node and group cycle rank;
- bridge and articulation counts;
- same-group and cross-group link counts;
- node overlap and link-through-node counts;
- fitted-view crossings, long chords, starter position, and endpoint spread; and
- a screenshot of the ModDex fitted audit.

Reject a tree when ModDex reports **FAIL**, when a runtime validator fails, or when the fitted render reads as separate thematic rails despite passing the numeric gates.
