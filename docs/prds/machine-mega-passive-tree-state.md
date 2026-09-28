# Machine Mega Passive Tree Implementation State

Source: [accepted design](machine-mega-passive-tree.md).

Status: **Done** — implementation and travel-road redesign completed 2026-09-27. Gameplay remains a Prototype pending focused playtesting.

## Scope checklist

- [x] Create and switch to feature/machine-mega-passive-tree from clean main.
- [x] Shared stable-ID graph: 750 nodes, six starts, asymmetric layout and validation.
- [x] Versioned progression, level 80, XP bands and legacy allocation refund migration.
- [x] Three attributes, explicit scaling, absolute constraints and family behaviors.
- [x] Crusher, Furnace and Forestry Companion integration.
- [x] Mastery UI, refunds, ordered copy/paste and automatic allocation.
- [x] Configurator build mode and refund consumable.
- [x] Replace direct cluster links with connected attribute roads and optional reward arcs/rings.
- [x] Verify reward investment costs and update design rules to v6.
- [x] ModDex audit, canonical documentation and representative 79-point builds.
- [x] Verification, diff review, and staging on the feature branch.

## Progress log

- Created the requested branch and migrated from two masks to versioned stable node IDs. Existing XP and earned levels survive; old allocations refund.
- Added three attributes and six starts. Increased/reduced share a bucket, more/less multiply, and absolute constraints resolve last. Irrelevant effects stay selectable and are labelled per effect.
- Integrated the initial three families, Gear-safe refunds, target builds, automatic allocation, and Configurator interaction before machine menu opening.
- Replaced the concentric draft with an asymmetric graph, then rebuilt its topology in response to the reference image: 360 attribute nodes now form connected roads, with 60 reward arcs, 15 open horseshoes, and 15 rings.
- Every notable requires three reward allocations from its road gate; keystones continue those branches at four. Distinct gates have intervening travel nodes. Reward branches cannot shortcut between roads.
- Preserved all 750 stable IDs and authored rewards, including modifier operations. Only grouping, placement, and connections changed in this redesign.
- Updated the design rules, catalog audit, ModDex source export, and generated overview.

## Verification

- Gradle quickCheck and ciCheck — passed after the topology change; 435 domain checks including migration, network codec, point budgets, connectivity, target following, attribute suppression, mixed applicability, and modifier/constraint math. Spotless checks passed.
- npm run moddex:check — passed; 55 profiles and 781 recipes. Shared catalog: 750 nodes, 809 links, 272 authoring groups, zero overlaps, links through nodes, or crossings.
- Graph audit verifies travel-only connectivity, single-gate reward components, minimum two-edge gate separation, and reward investment costs. Mutation cases reject a notable shortcut, a cross-pocket bypass, and a travel route that requires a starter to reconnect.
- Keystone shortest paths from all six starts range from 15 to 42 points. Representative builds spend exactly 79 points.
- ModDex fitted Visual Audit passes with zero proper crossings and zero long chords.
- npm run repo:check and mkdocs build --strict — passed after the travel-road redesign.

## Handoff and limits

- Implementation files are staged on feature/machine-mega-passive-tree. No commit, push, merge, or deployment performed.
- Suggested commit: feat: add shared machine mega passive tree.
- Additional machine adapters and family ascendancies are intentionally deferred. Legacy graph definitions remain as reference/audit fixtures.
- In-game controls, multiplayer UI synchronization, refund inventory consumption, and late-game XP/balance need focused playtesting. Automated checks do not replace those checks.
- Runtime catalog: src/main/resources/data/rngtech/mastery/machine_tree.json. Author rewards with tools/moddex/author-mega-tree.mjs and roads/reward placement with layout-mega-tree.mjs. Regenerate the audit report and overview after changes.
