# Passive Tree Design Rules v4

Status: Implemented

Design standard for passive-tree authoring. See [Design Proposals](../design-proposals.md) for broader framework requirements.

Scope: RNGTech machine passive trees built with typed node enums, `PassiveTreeDefinition`, and `PassiveTreeGroup`

## Design target

Use Path of Exile as a reference for spatial grammar: a central start, compact constellations, short paths between nearby clusters, loops that offer route choices, and keystones around the perimeter. Keep RNGTech names, icons, mechanics, and artwork distinct.

A finished tree should read as one field of connected constellations at fit-to-screen scale. Themes may form regions, but each region needs links to its neighbors. Long isolated rails and map-spanning chords fail review.

The fitted render decides whether a layout passes. Graph counts help find problems; they cannot overrule a poor render.

## Why v3 failed

The v3 rules rewarded edge density. Its 30% node-cycle target and 35% to 55% cross-group link target encouraged authors to add long links until the graph passed. The resulting Forestry Companion tree had 144 edges and a node-cycle rank of 45. It still rendered as tangled lanes spread across a large canvas.

Version 4 lowers the density targets and adds a ModDex visual gate. The gate loads coordinates and links from the Java source, fits the full graph into a gameplay-style view, and marks crossings and long chords in red.

## Runtime constraints

Keep these constraints unless a tree class records an exception:

- Preserve released enum order. Append new enum constants after existing constants.
- Keep the tree within the 128-node save cap imposed by two `long` masks.
- Target about 100 nodes for a full machine tree.
- Use one `STARTER` node. Allocate it without spending a point.
- Give the starter three exits.
- Give each node a placement and a `PassiveTreeNodeSpec`.
- Make each node reachable from the starter.
- Limit `TRAVEL` grants to the tree's core passive attributes.
- Keep keystones at least seven allocated nodes from the starter.

Use these degree caps:

| Kind | Degree cap |
| --- | ---: |
| `STARTER` | exactly 3 |
| `TRAVEL` | 4 |
| `NODE` | 3 |
| `NOTABLE` | 2 |
| `KEYSTONE` | exactly 1 |

The geometry validator must report:

- zero overlapping node bounds;
- zero links through unrelated node bounds; and
- zero exact duplicate links.

## Fitted-view acceptance gate

Load the Java tree in ModDex and inspect **Visual Audit** before handoff. Use the fitted view, since the editor canvas can hide a weak composition behind scroll position and zoom.

For a 90 to 110 node tree, apply these gates:

| Metric | Pass condition |
| --- | ---: |
| Proper link crossings | `<= max(3, round(edgeCount * 0.02))` |
| Starter position | within 30% to 70% of both occupied axes |
| Endpoint coverage | at least 5 of 8 angular sectors |
| Largest empty endpoint arc | `<= 120 degrees` |
| Long internal chords | `<= max(3, round(edgeCount * 0.06))` |
| Internal edge P90 | `<= max(240 px, medianLength * 2.75)` |

The chord calculation excludes the final link into a one-degree keystone. That link represents a deliberate endpoint commitment. The calculation includes links to ordinary nodes and notables.

Use red crossing markers as repair locations. Move a cluster, rotate orbit occupancy, or remove a redundant link. Keep a crossing only if each local alternative creates a node overlap, a link through a node, or more crossings elsewhere.

## Graph health ranges

Graph metrics should support the composition without turning the tree into a mesh.

For a 90 to 110 node tree:

| Metric | Review range |
| --- | ---: |
| Node cycle rank | 15% to 30% of node count |
| Bridge edges | `<= 20%` after excluding final keystone links |
| Longest degree-2 corridor | `<= 5` nodes |
| Articulation nodes | investigate above 25% of node count |

For a 25 to 35 group tree:

| Metric | Review range |
| --- | ---: |
| Group cycle rank | `>= max(8, ceil(groupCount * 0.25))` |
| Group articulation count | `<= ceil(groupCount * 0.35)` |
| Group bridge count | `<= ceil(groupEdges * 0.20)` after excluding endpoint groups |
| Cross-group edge ratio | 25% to 40% of all edges |
| Longest low-degree group corridor | at most 3 non-endpoint groups |

A result outside a review range needs a written exception and a clean fitted render. The runtime validator still enforces reachability, node kinds, degree caps, and save limits.

## Composition rules

### Build a compact skeleton

Place the starter inside the occupied tree body. Arrange three to five regions around it. Each region should contain several clusters, and adjacent regions should share short links near their boundary.

Keep most group centers close enough that a cross-group edge has a similar length to an internal travel edge. Reserve extra distance for a keystone approach or a clear boundary between regions.

Avoid rows, columns, and long monotonic runs. A route that moves in one direction through four group centers reads as a rail even when small loops decorate it.

### Use local constellation templates

Useful templates include:

- a crescent with one notable at its edge;
- two short approaches that meet at a notable;
- a triangle linking two neighboring clusters;
- a side pocket with a nearby rejoin;
- a short keystone approach with a choice near the final link; and
- a starter rosette whose exits reconnect within four nodes.

Avoid inline notable chains, six-node theme pipes, decorative wheels with one usable route, and several endpoints hanging from one side.

### Mix reward packages

Split each broad goal across at least three regions. Processing speed, energy efficiency, output handling, and reserve support each need more than one route package.

Give a strong package a nearby competing choice. A speed cluster can border reserve support or control. A cargo cluster can border energy recovery or canopy safety. These links give the player reasons to cross a regional boundary.

### Treat keystones as perimeter commitments

A keystone may use a bridge as its final link. Give its approach cluster a branch, loop, or cross-group choice within two graph steps of that link. Place endpoint groups around the occupied perimeter instead of collecting them into a comb.

## Authoring workflow

1. Sketch a group graph with the starter, regional clusters, and endpoint groups.
2. Place group centers around the starter and assign orbit templates.
3. Add short internal links and links between neighboring groups.
4. Assign themes and rewards after the skeleton reads as one tree.
5. Run the runtime and geometry validators.
6. Load the Java source in ModDex and repair each fitted-view failure.
7. Record graph metrics and intentional exceptions in the handoff.

Do not add links to chase cycle rank after the fitted composition works. Add a link when it creates a useful route choice and can stay local in the render.

## Forestry Companion reference result

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
