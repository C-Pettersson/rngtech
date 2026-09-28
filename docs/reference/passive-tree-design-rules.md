# Passive Tree Design Rules v6

Status: Implemented

Design standard for passive-tree authoring. See [Design Proposals](../design-proposals.md) for broader framework requirements.

Scope: the shared [Machine Mastery catalog](../systems/machine-mastery.md) and retained legacy typed-tree references. The shared catalog supersedes the per-machine enum/mask format.

## Design target

Use Path of Exile as a reference for spatial grammar: a connected network of attribute roads, junctions and broad route loops, with compact reward rings and open arcs branching into the spaces between roads. Keep RNGTech names, icons, mechanics, and artwork distinct.

Travel must consume allocations. Moving between specialties should mean following attribute nodes, then deciding whether to leave the road and invest in a reward branch. Notables belong at the far ends of those investments. A sequence of directly connected reward wheels fails this design even when its geometry is tidy.

Keep the overall field asymmetric. Avoid concentric cluster bands, mirrored sectors, evenly repeated spokes, and identical keystone pairs. Vary road lengths, junction spacing, reward silhouettes, open space, and endpoint distribution. Six starts may occupy an inner region without imposing symmetry on the surrounding graph.

The fitted render decides whether the composition works. Counts cannot overrule a poor render.

## Why the rules changed

Version 3 rewarded density and produced tangled links. Version 4 added a fitted-view gate. Version 5 removed the shared tree's symmetry but still placed its travel nodes inside repeated reward loops, leaving almost no travel between clusters.

Version 6 separates the travel network from optional reward investments. Long roads, open space, degree-two corridors, and bridged reward branches are intentional. The old requirements for similar internal/external link lengths, low bridge counts, and short corridors apply only to the retained small-tree reference.

## Runtime and topology constraints

- Preserve stable string IDs. Array order is only a synchronized catalog index, never save identity.
- Target 600–900 shared nodes, with approximately half granting travel attributes. The current catalog contains 750; the runtime guard allows up to 4096.
- Use six starters, each with three exits. A machine receives only its family's start for free. Foreign starts cannot be traversed.
- Make every non-starter reachable from each start without crossing another starter.
- Limit travel grants to Control, Drive, and Reserve. Hybrid starts do not add attributes.
- Connect all travel nodes using travel nodes alone. Removing reward nodes and starters must leave one connected road network.
- Attach each reward component to one road gate. Multiple open arms may share a gate; reward branches must not form shortcuts between roads.
- Separate distinct reward gates by at least two travel edges, so even neighboring investments have intervening travel.
- Require at least three reward allocations from a road gate to a notable. Ordinary bonuses precede the notable on each approach.
- Require at least four reward allocations from a road gate to a keystone, and between seven and 79 total allocations from every start.
- Give travel dead ends a reward destination. Do not add empty spurs to meet a node-count target.

| Kind | Degree cap |
| --- | ---: |
| STARTER | exactly 3 |
| TRAVEL | 4 |
| NODE | 3 |
| NOTABLE | 2 |
| KEYSTONE | exactly 1 |

Released legacy masks are refunded during migration, preserving XP and levels. They do not constrain the shared graph. Nodes have no individual level or chassis-stage gates.

## Composition and investment

### Lay out roads first

Sketch an irregular network around the starting region. Connect nearby junctions through several attribute allocations. Give the network broad loops and alternate regional approaches without making every junction a four-way intersection.

Road spacing and reward spacing serve different purposes. Roads need room to read across the map; rewards sit closer together in small constellations. Long roads are allowed when they contain visible, paid travel steps. A single map-spanning edge that bypasses that investment is not a road.

### Branch rewards into the open spaces

Use a mixture of:

- open arcs with two ordinary nodes leading to an endpoint notable;
- open horseshoes with two competing three-point arms;
- rings entered from one road gate, with ordinary nodes on both approaches and notables on the far side;
- keystone continuations after an invested reward arm.

A ring may reconnect its reward arms locally. It must not connect to a second road and become the cheapest path between specialties. Open arcs and single-entry rings are useful even though their entrance is an articulation point. Do not connect their tips merely to increase cycle rank.

Vary orientation, radius, density, and spacing. Avoid identical wheels at every junction. Leave clear space between the compact reward branches and the routes that carry the player across the tree.

### Make choices compete

Repeat broad goals across several regions: processing speed, energy efficiency, output handling, attribute scaling, and reserve support should offer more than one route package. Nearby road gates should present competing investments.

Keep the distinction between increased/reduced, more/less, flat additions, and absolute constraints. Changing a path must not silently change the meaning of its rewards.

### Place keystones as commitments

Distribute keystones around the perimeter, following reward investment rather than hanging directly off travel junctions. All starting archetypes must be able to reach each keystone within the level-80 budget. Check representative builds that combine a nearby commitment with a distant specialty.

## Validation and fitted-view gate

Run the catalog audit and load the regenerated source in ModDex before handoff. Inspect the fitted Visual Audit as well as a zoomed section showing road steps and reward approaches.

The shared graph must have zero overlapping node bounds, links through unrelated nodes, exact duplicate links, degree violations, and proper link crossings. Validate travel-only connectivity, one-gate reward components, gate separation, notable investment, and keystone budgets from the actual links; group labels are not proof.

Use the median cross-group link length for the fitted long-chord threshold: max(240 px, median * 2.75). Keep the long-link allowance at max(3, round(edgeCount * 0.06)) and P90 below the threshold. Subdivided roads pass because the travel is paid in visible steps. The final link to a degree-one keystone is excluded from this chord calculation.

All six starts must remain within the occupied center region (30% to 70% on both axes). Endpoints should occupy at least five of eight angular sectors, with no empty arc larger than 120 degrees. Do not add a seventh synthetic starter when importing the catalog.

The current catalog has 750 nodes and 809 links, including 360 travel nodes and 90 reward pockets: 60 arcs, 15 open horseshoes, and 15 rings. Its cycle rank is 60. Every notable costs three reward points from its gate; every keystone costs four. The [generated audit](machine-mega-tree-audit.json) records the exact geometry, distances, and representative 79-point builds.

The legacy small-tree review ranges remain useful for the historical reference only: node cycle rank 15–30%, bridge edges up to 20% excluding keystone endpoints, degree-two corridors up to five nodes, and articulation review above 25%. For its 25–35 groups, review cross-group ratios of 25–40%, group cycle rank at least max(8, ceil(groupCount * 0.25)), group articulation up to 35%, group bridges up to 20%, and low-degree group corridors up to three groups. These are not shared-tree targets.

## Authoring workflow

1. Place the starts and irregular road junctions.
2. Connect the travel network, including paid intermediate steps and alternate routes.
3. Place optional reward arcs and rings beside the roads.
4. Put notables at the ends of their approaches and keystones beyond committed reward paths.
5. Author themes and rewards without altering modifier keyword semantics.
6. Regenerate the runtime catalog, ModDex source export, audit report, and overview.
7. Check investment costs, runtime reachability, geometry, and the fitted render.

Do not add links to chase cycle rank or reduce bridge counts after the composition works.

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
