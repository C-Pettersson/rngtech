# RNGTech Moddex

Static modifier-pool, rolling simulator, recipe bill-of-material explorer, component-stage preview, and GUI layout editor for RNGTech.

Run:

```sh
npm run moddex:serve
```

Then open <http://127.0.0.1:4177/>.

Direct app routes such as <http://127.0.0.1:4177/passive-trees> load the matching Moddex tab.

The web UI reads `tools/moddex/generated/modifier-data.json`. Regenerate it without starting the server:

```sh
npm run moddex:export
```

The GUI editor also reads `tools/moddex/generated/gui-layouts.json`, which is extracted from existing Java screen classes:

```sh
npm run moddex:gui-export
```

Export typed Java passive trees without starting the server:

```sh
npm run moddex:passive-tree-export
```

Run the static server smoke check:

```sh
npm run moddex:check
```

The smoke check also fails when the RBOM exporter finds overlapping crafting recipes with different outputs, including collisions against built-in vanilla iron tool recipe sentinels.

The smoke check runs the recipe loop audit. Run it directly, with `--report` to list every loop found:

```sh
node tools/moddex/check-recipe-loops.mjs --report
```

The audit reads every recipe type, the vanilla tags and crafting it chains into, and `tools/moddex/recipe-loop-bounds.json`. The bounds file sets:

- the free items and free input keys;
- the worst-case bonus multiplier for each recipe type;
- the Potential Reactor stripping payout and the recyclable items it cannot strip;
- an allowlist of reviewed loops, each with a reason.

A linear program finds the loop with the largest gain. Each loop found is shrunk to a minimal recipe set, reported with a likely fix, and then set aside so the search can surface the next one. Mutation self-tests re-enable bonus output on known loops and fail when the audit misses them. Every stat or behavior declared with a yield in `data/rngtech/mastery/declarations.json` must be covered by a bound.

Check Java passive-tree geometry and graph constraints directly:

```sh
node tools/moddex/check-passive-tree.mjs
```

This regenerates the Java export and checks node identities, reachability, node degree limits, keystone distances, node bounds, links through nodes, duplicate links, and the editor's fitted visual gate. It also reports cycles, bridges, articulation points, and corridor lengths. The smoke check includes the same validation.

The exporter derives modifier pools, tier ranges, profile capabilities, rollable behaviors, and refinement-operation metadata from the Java source under `src/main/java/com/rngtech/rpg`. It also normalizes item-producing recipes and item tags under `src/main/resources/data` for the RBOM and Stages tabs.

The GUI exporter derives screen dimensions, tab positions, slot frames, labels, meters, status bars, and tooltip rectangles from Java screen classes under `src/main/java/com/rngtech/client/screen`. The Moddex GUI editor can load those layouts, let graphical elements be moved or added, and emit an `rngtech.moddex.gui.v1` JSON implementation brief. It does not update Java, resources, or generated game data.

The Passive Trees tab authors machine passive-tree layouts. It supports reusable `PassiveTreeLayouts` shapes, signed node identities, saved shape templates, named drafts, group and node dragging, node insertion and deletion, link editing, and Java or `rngtech.moddex.passive-tree.v2` JSON output. Every draft includes a starter node that is allocated without a point cost and grants nothing.

When the server starts, `export-passive-tree-data.mjs` reads typed `*PassiveTreeLayout`, `*PassiveTreeDefinition`, and passive-node Java sources into `tools/moddex/generated/passive-trees.json`. Select a Java source and press **Load Java Tree** to inspect the same node coordinates, kinds, and authored links that the game uses. The fitted **Visual Audit** reports proper crossings, long internal chords, edge-length spread, starter position, and endpoint angular coverage. It marks failed crossings and chords in red.

The JSON brief includes draft metadata, signed identities in `identities: [{ role, label, negative }]`, hybrid flags, authored connections, and a datapack candidate path for future runtime loading. The game still uses Java layout classes and code-backed passive node definitions. A negative identity records authoring intent such as a penalty or tradeoff; it does not select a game stat operation.

RBOM includes pseudo targets for code-backed assembly and machine-process surfaces that are not represented as item-producing JSON recipes, so those paths can be expanded like normal recipes.

The Stages tab groups registered stage-bearing content by component stage and category. Its issue markers use a recursive reachability check, so same-stage component inputs are only flagged when the whole recipe chain cannot be bootstrapped from lower-stage components, craftable direct material-form inputs, allowed base material forms, or external inputs. Known alloy material tags are resolved through their item-producing recipes instead of being treated as free base materials, which lets Moddex catch bootstrap loops such as a stage machine requiring the same-stage alloy needed to build it.
