# Companion Rebalance Implementation State

Source: [Companion Rebalance PRD](companion-rebalance.md).

Status: **Prototype** for the 2.0 release. All four slices are in and pass the automated checks; they still need an in-game playtest. Work happens on `feature/companion-rebalance`.

## Scope checklist

- [x] Sync with `main` after PR #18 (Rolling Harvest fix).
- [x] Draft the PRD and this state page.
- [x] Slice 1: Work Range, Grove Warden rework, shared-tree Forestry clusters, speed rebalance, idle speed.
- [x] Slice 2: tool-aware breaking with crack animations.
- [x] Slice 3: Field Hand ascendancy, crops, sprinkler, water, and tilling.
- [x] Slice 4: Companion Station overhaul and cart screen additions.
- [ ] In-game playtest.

## Decisions

Recorded 2026-10-03:

- Nursery grants +2 Work Range. Rows are contiguous; whether dense sapling grids still grow needs an in-game check.
- The shared tree grants Work Range through two +1 notables in separate, distant extra pockets, not one +2 notable. Extra pockets leave existing node IDs stable, which a claim on an existing notable slot would not.
- The cart is slower in both movement and work cadence. Specialization, the shared tree, and tools earn back today's speed and slightly exceed it.
- Drive scales speed only through notables (Driven Wheels), not through an inherent conversion.
- Field Rotation becomes the root of a third Forestry ascendancy, Field Hand, instead of a Grove Warden notable:
    - Timber Baron is the logger.
    - Grove Warden is the generalist, with growth pulses as its edge.
    - Field Hand farms crops and does no tree work. Its sprinkler needs a Fluid Pump and water, and it also tills.
- Because Field Hand never handles saplings, crops and saplings never share a cart's supply slots. The pasted PRD's two-slot deadlock rule is therefore unnecessary.
- The station is renamed Companion Station in display text only; registry IDs stay.
- The station's tool slot installs a spare when the docked cart's tool is missing or broken. This reverses the earlier rule that the station never replaces tools.
- The Forestry Companion no longer needs a Stage 4+ Axe or Treefeller to use Seal I; it always meets the entry gate, and crafting the Seal is the requirement. The tool gate blocked carts meant for Field Hand.

## Progress log

- 2026-10-03: Synced with `main` after PRs #18 and #19, and wrote the PRD and this state page.
- 2026-10-03, slice 1:
    - `WORK_RANGE` and `IDLE_CART_SPEED` are declared Forestry stats. The cart works rows 1–7 per side through `ForestryCartRules`, scan FE scales with rows, outer rows skip planting silently, Ancient Grove plots work on any row, and Verdant Surge and Rolling Harvest reach Work Range + 1.
    - Managed cells beyond the current Work Range that hold no sapling, crop, or log are pruned as the cart scans each rail block.
    - Grove Warden: Spare Plots and Old Rows grant 16 managed cells each, and Nursery grants +2 Work Range. Timber Baron's two Cart Speed nodes grant 15% each.
    - Base speeds dropped: powered 0.055, seeking 0.08, Manual Throttle 0.095 and 0.12. Idle Cart Speed applies after 80 ticks without work and is saved with the cart.
    - The shared tree gained four extra Forestry pockets (21 nodes) without moving or renaming any existing node. Extras can now replace their pocket's notables.
    - Balance pass: the shortest Forestry route to Rail Pace, Open Track, Far Rows, and Outer Rows spends 50 points and reaches 48 Drive. Driven Wheels at 0.5% per Drive then gives +62% Cart Speed in total, about 1.11× the old powered speed; Timber Baron's two nodes take it to about 1.32×. Open Track adds up to 73% more while idle.
- 2026-10-03, slice 2:
    - Logs crack over time with `destroyBlockProgress` and block hit sounds before the existing cut runs. Break time follows hardness, the tool's destroy speed against the block plus Efficiency, and Processing Speed, with a 2x scale over vanilla and a 6-tick floor. A cut is followed by a 4-tick settle.
    - Treefeller batches crack every log at once for the slowest log's time times `1 + 0.35 × log2(N)`.
    - Planting has its own 30-tick interval divided by Processing Speed. The Stats tab's action interval now shows the installed tool's log break time.
    - Cracks clear on a dropped snapshot, a changed target, an unaffordable cut, and cart removal. Break progress is not saved.
- 2026-10-03, slice 3:
    - Field Hand ships as the third Forestry ascendancy (15 nodes, placeholder icons for the root and seven notables), with six new behaviors: `CROP_TENDING` (yield output, covered by the Forestry world source), `NO_TREE_WORK`, `SPRINKLER`, `IRRIGATION`, `REAP_AND_SOW`, and `ROLLING_REAP`.
    - `rngtech:cart_crops` lists wheat seeds, carrots, potatoes, and beetroot seeds. A Field Hand cart's supply slots take only those; other carts take only saplings, and a cart moves supply items it can no longer plant to output.
    - Planting picks the first supply stack that can survive at the cell, so farmland gets crops. Mature managed crops are harvested through the owner fake player.
    - The cart gained a Fluid Pump Gear slot (save migration through the existing gear-size expansion) and an 8,000 mB water tank. The station's fluid endpoint is now a 16,000 mB water tank that refills docked carts at the pump's transfer rate.
    - The cart screen shows the pump slot to the left of the Gear row and a water gauge beside the energy gauge for Field Hand carts.
- 2026-10-03, slice 4:
    - The station is named Companion Station in display text; registry IDs are unchanged.
    - New process slots are appended after the shears slot, so older saves keep their items: a bone meal slot that drains into the 256 store, and a spare-tool slot. The plantables slot no longer takes bone meal; bone meal an older station left there still drains into the store.
    - A docked tree-working cart whose tool is missing or broken gets the spare tool, and the broken tool goes to the station outputs only when they have room.
    - Hoppers route plantables, bone meal, shears, and tools by type through four virtual input slots.
    - The Station tab groups Supply (plantables, bone meal), Cart (tool, shears), and Output under short centered labels, with water and FE gauges, the bone meal bar, and the status, action, and hold controls moved below the Supply group. Empty supply slots show faded example items and hover hints. The Gear tab spaces its heading from the slot labels and labels the energy port FE. Gear hints now appear on the station's Gear tab and the cart's Gear row.
    - The Stats tab lists the water store and, for a docked cart, its energy, Work Range, and tool condition or water.
    - The cart screen gained a Work Range readout and an Idle Cart Speed light beside the status icons, and supply-slot hints.
- 2026-10-03, playtest fixes:
    - Seed Ledger replanted a cell's earlier crop after harvesting a different one, because a cell learned its crop only when the cart planted it. Each scan now records what is growing in a cell, and the cart adopts saplings or crops it can tend that a player planted, so the memory follows what was there.
    - Sprinkled farmland holds a NeoForge farmland water ticket for 60 seconds, 90 with Irrigation, and the cart refreshes it once half has passed. Before this, vanilla dried it within a few random ticks.
    - Forestry carts always meet the Seal I entry gate; worktrees share the main checkout's `run/` folder; the cart's Work Range and idle lights moved off the Output label.
    - Limited supply slots let a common species crowd out rarer remembered ones. Seed Library carts now keep a per-cell reserve set aside from each cell's own harvest, and the station gained a Supply tab with a 3×3 plantables buffer that loads what waiting cells need first. A cart tab was not needed: reserves show as a Stats row and in the supply-slot hint.
    - Fluid Capacity: the cart's water tank reads it (base 8,000 mB). Two Reservoirs pockets joined the shared tree, one by the Forestry start and one by the Melter's, and Fluid Pumps can roll a flat Brimming prefix (250–9,000 mB by tier) and a percent Depth suffix. Those affixes apply to the host machine's tanks; the cart also refills at the pump's rolled transfer rate. The Corrosion Cell's electrolyte tank also reads Fluid Capacity now (base 4,000 mB), so the affixes work there too. Field Hand's Hose Fittings and Wide Nozzles now grant 25% increased Fluid Capacity each instead of 5% reduced Energy Usage.
    - Rolling Harvest no longer halts for empty cells. With the slower 30-tick planting, a rolling cart fell behind and stopped to plant; it now halts only for trees, or ripe crops under Rolling Reap, and plants empty cells on the move or on its next pass. It plants every empty cell in reach in one action and, for one plant interval after planting, moves at most one rail block per interval, so rolling planting stays even; Processing Speed raises that stride. The station also tops up any plantable once it has none of the species Seed Library cells are waiting for.

## Verification

- 2026-10-03 slice 1: `gradlew spotlessApply quickCheck` passed with 2327 mastery checks, 23 tree-scan checks, and 535 new Work Range geometry checks. `npm run moddex:check` passed: the mega tree audit with 1336 nodes and no overlaps or crossings, the loop audit with no loop, and the ascendancy docs with 27 declared stats. Not yet run in game.
- 2026-10-03 slice 2: `gradlew spotlessApply quickCheck` passed with 558 Forestry cart rule checks, including break timing. Crack animations need an in-game check.
- 2026-10-03 slice 3: `gradlew spotlessApply quickCheck` passed with 2427 mastery checks, including Field Hand's launch content. `npm run moddex:check` passed: 15 ascendancies on 7 family pages, and the loop audit covers `CROP_TENDING` with no loop. Field Hand needs an in-game check.
- 2026-10-03 slice 4 and handoff: `gradlew spotlessApply quickCheck` and `gradlew ciCheck` passed (2427 mastery checks, 23 tree-scan checks, 558 cart rule checks). `npm run moddex:check` and `npm run repo:check` passed. `runGameTestServer` completed mod loading with the new tag, ascendancy, menus, and block entity data, then exited because no game tests are registered. `mkdocs build --strict` was not run because MkDocs is not installed locally; CI runs it. Not yet run in game.

## Handoff and limits

- Node numbers, speed constants, the 2x break scale, and the batch growth factor are first-pass values, pending balance testing.
- In-game checklist:
    - Work Range 1, 3, and 5 farms with the debug overlay; the cell cap binding at about 16 rail blocks at range 3; pruning after shortening the range or moving a route.
    - Dense-row tree growth with contiguous rows, and Ancient Grove on outer rows.
    - New base speeds, Driven Wheels, and the Idle Cart Speed light and bonus.
    - Crack animations for axe and Treefeller breaks, cracks clearing on abort, and the hit sounds.
    - Field Hand: no tree work and no tool needed; crops planting, harvesting, and replanting; Sprinkler tilling and wetting with a pump and station water; Irrigation; Reap and Sow; Rolling Reap; Seed Ledger; switching away moving crops to output.
    - Companion Station: Supply tab, demand-first plantables loading, bone meal slot, spare-tool swap with a broken tool, hopper routing, slot hints and ghost items, no overlapping labels at default and large GUI scales, and the new Stats rows.
    - Seed Library reserves: a mixed farm keeps replanting rare species, reserves survive save and reload, and they return or drop correctly on pruning, respec, and cart breakage.
    - Save and reload, including a station and a cart saved before this change.
