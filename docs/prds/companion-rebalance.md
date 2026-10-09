# PRD: Companion Rebalance

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Tree Farm Automation](https://c-pettersson.github.io/rngtech/forestry-cart-station/) for current behavior.

PRD status: Accepted

Implementation status: Prototype

Release: 2.0

Last updated: 2026-10-03

## Summary

The Forestry Companion works only the two blocks beside each rail block, so its managed-cell cap never binds. Grove Warden's managed-cell nodes therefore do nothing on ordinary farms, and Ancient Grove sits behind three of them. The cart is also faster than intended, and the shared tree offers few Forestry choices.

This PRD covers:

- a **Work Range** stat for how many rows the cart works on each side of the rail;
- a slower base cart, which builds earn back through new Forestry clusters, Drive-scaling notables, and an idle-speed bonus;
- tool-aware break timing with block-breaking animations;
- a third Forestry ascendancy, **Field Hand**, which farms crops instead of trees and uses a sprinkler fed from a water tank;
- a renamed and reworked **Companion Station**.

The work lands in four slices. Each slice passes every check on its own.

1. Work Range, Grove Warden rework, shared-tree clusters, speed rebalance.
2. Tool-aware breaking with crack animations.
3. Field Hand ascendancy, crops, sprinkler, water, and tilling.
4. Companion Station overhaul.

Progress, decisions, and verification are tracked on [Companion Rebalance State](companion-rebalance-state.md).

## References

- [Tree Farm Automation](https://c-pettersson.github.io/rngtech/forestry-cart-station/) and its [requirements](tree-farm-automation.md)
- [Machine Ascendancies PRD](machine-ascendancies.md)
- [Shared Machine Mega Passive Tree PRD](machine-mega-passive-tree.md)
- [Passive Tree Design Rules](../reference/passive-tree-design-rules.md)
- [Machine Stats](../reference/machine-stats.md)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine GUI Design](../reference/machine-gui-design.md)

Key existing code:

- `src/main/java/com/rngtech/content/entity/ForestryCartEntity.java`
- `src/main/java/com/rngtech/content/blockentity/ForestryCartStationBlockEntity.java`
- `src/main/java/com/rngtech/client/screen/ForestryCartScreen.java` and `ForestryCartStationScreen.java`
- `src/main/resources/data/rngtech/mastery/ascendancies/`
- `tools/moddex/author-mega-tree.mjs`

## Goals

- Grove Warden's Nursery and managed-cell nodes matter on ordinary farms. Work Range makes the cell cap a real constraint, so range and cells trade off: a wide, short farm or a long, thin one.
- A fresh cart is slower in movement and work. Specialization, the shared tree, and tools earn back today's speed and slightly exceed it.
- Tools and block hardness decide break time, and breaking is visible.
- Forestry has three ascendancies:
    - Timber Baron, the logger;
    - Grove Warden, the generalist whose edge is growth pulses;
    - Field Hand, the farmer.
- The station supplies everything a cart consumes, and its GUI follows the [Machine GUI Design](../reference/machine-gui-design.md) rules.
- No item or FE loops. This is a hard requirement, and the loop audit and its Forestry world source stay green.

## Non-goals

- Stem crops (melon, pumpkin), sweet berries, cocoa, nether wart, sugar cane, and bamboo.
- Modded crops beyond what a datapack adds to `#rngtech:cart_crops`.
- Renaming registry IDs. Only display names change.
- A shared item-storage system.

## Work Range

`WORK_RANGE` is a Forestry stat. The cart works rows at distances 1 to Work Range from the rail on each side, so each rail block has 2 × Work Range cells.

- The base value is 1, which keeps today's behavior. The cart reads it as a whole number, clamped to 1–7.
- It is not a yield. More cells mean more free growth, not more output per input.
- Scan FE scales with rows: `SCAN_FE × Work Range`.
- Rows beyond 1 that cannot be planted are skipped silently. Only row 1 reports Invalid Soil or Planting Blocked, which avoids status spam over water, paths, and fences.
- Reach rules follow Work Range:
    - Verdant Surge pulses every managed cell within Work Range + 1 of the rail;
    - Rolling Harvest reaches Work Range + 1.
- Ancient Grove plots extend outward from the rail on any row; their far squares may sit one row beyond Work Range.
- Pruning: when the cart scans a rail block, it drops managed cells in that rail block's cross-section that lie beyond the current Work Range and hold no sapling, crop, or log. A reduced range or a moved route then stops wasting the cap.

Managed-cell cap math:

| Range | Cells per rail block | 96 cells fill | 128 cells fill |
|---|---|---|---|
| 1 | 2 | 48 rail blocks | 64 |
| 3 | 6 | 16 | about 21 |
| 5 | 10 | about 10 | about 13 |

Sources:

- Grove Warden's Nursery: +2.
- Two shared-tree notables, **Far Rows** and **Outer Rows**, +1 each. They sit in separate, distant clusters so getting both costs extra points.
- Field Hand: +1 from its root, plus +1 each from Wide Furrows and Open Fields.

## Speed rebalance

Base rail speeds drop by roughly a third. Unpowered speed, coasting speed, and the 0.34 cap are unchanged.

| Speed | Before | After |
|---|---|---|
| Powered | 0.08 | 0.055 |
| Seeking a station | 0.12 | 0.08 |
| Manual Throttle, powered | 0.14 | 0.095 |
| Manual Throttle, seeking | 0.18 | 0.12 |

Sources that earn it back:

- **Shared-tree clusters** in the Forestry region, added as extra pockets so existing node IDs stay stable:
    - Rail Pace: Cart Speed;
    - Open Track: Idle Cart Speed;
    - Far Rows and Outer Rows: Tree Fell Limit, with a Work Range notable.
- **Driven Wheels**, a Rail Pace notable: increased Cart Speed per point of Drive. Drive affects speed only through notables like this one, not through an inherent conversion.
- **Timber Baron**: Greased Axles and Loose Brakes grant 15% Cart Speed each.
- **`IDLE_CART_SPEED`**: increased Cart Speed while the cart has done no work for 4 seconds (80 ticks). Work means a successful plant, cut, Treefeller batch, or crop harvest. Pulses and path clearing do not count. It stacks with Dock Sprint.

A committed shared-tree build reaches about today's speed. Adding Timber Baron takes it to about 1.1–1.2×.

## Tool-aware breaking

The cart breaks a block over time, with the vanilla crack animation, instead of deleting it and then waiting out a flat interval.

- Break ticks are `max(6, ceil(hardness × 30 / toolSpeed × 2.25 / Processing Speed))`.
    - Tool speed is the tool's destroy speed against the block, plus Efficiency (level² + 1).
    - An iron axe takes about 27 ticks per log, a flint axe about 45, and an exotic axe about 14.
- A Treefeller batch cracks every log at once and takes `single × (1 + 0.5 × log2(N))` ticks. That is cheaper per log than an axe but still scales with tree size.
- Planting uses a tool-independent 30 ticks / Processing Speed. Leaves keep the shear cadence.
- The cart stays put while breaking. Rolling Harvest's reach rule still applies.
- Cracks clear when a break is abandoned: the snapshot is dropped, the block changes, power runs out, the tree leaves reach, or the cart is removed. Break progress is not saved, so a break restarts after reload.

## Field Hand

Field Hand is a Forestry ascendancy that farms crops instead of trees.

Its root, **Field Rotation**, grants:

- `CROP_TENDING`: plant, harvest, and replant crops from `#rngtech:cart_crops` on farmland;
- `NO_TREE_WORK`: no sapling planting and no log or leaf work;
- +1 Work Range.

Supported crops are items in `#rngtech:cart_crops`. The default list is wheat seeds, carrots, potatoes, and beetroot seeds. Each must be a `BlockItem` whose block is a `CropBlock`.

- **Supply.** A Field Hand cart's supply slots take crop items only. Other carts' supply slots take saplings only. A cart whose mode changes moves items it can no longer use to output cargo.
- **Planting** picks the first supply stack whose block can survive at the cell, so crops need farmland. Seed Library memory applies to crop cells.
- **Harvest.** A managed cell holding a fully grown crop is broken by the owner's fake player, and the drops go to cargo: seeds to supply, everything else to output. Each harvest gives 2 machine XP. Immature crops are skipped.
- **Tools.** A Field Hand cart needs no cutting tool. A Forestry Companion always meets the Seal I entry gate, so it can ascend without any Gear installed.

Notables:

| Notable | Effect |
|---|---|
| Wide Furrows | +1 Work Range |
| Open Fields (deep) | +1 Work Range, +32 managed cells |
| Sprinkler | Tills and hydrates. Needs a Fluid Pump installed and water in the cart's tank. Tills dirt or grass under empty crop cells within Work Range into farmland, and holds a NeoForge farmland water ticket on each watered block for 60 seconds, so it stays moist as if water were beside it. Spends 50 mB per block, refreshed once half the time has passed. |
| Irrigation (deep) | The sprinkler reaches Work Range + 1, uses half the water, and keeps farmland watered 50% longer. |
| Reap and Sow | Replants in the same action as the harvest. |
| Rolling Reap (deep) | Harvests mature crops without stopping, using Rolling Harvest's roll rules. |
| Seed Ledger | Seed Library for crop cells. |

**Water.** The cart has a Fluid Pump Gear slot and an 8,000 mB water tank. The station fills it from its own water tank, limited per tick by the cart pump's transfer rate.

Hydration matches vanilla water-adjacency growth and never adds growth ticks, so FE and water never become crops.

## Companion Station

The Forestry Companion Station is renamed **Companion Station** in display text. Registry IDs stay `forestry_cart_station`.

- **Plantables buffer.** The sapling slot becomes a 3×3 plantables buffer on a new Supply tab and takes saplings or cart crops. A docked Seed Library cart reports which species its empty cells wait for; the station loads those first, moving supply nobody waits for back into the buffer to make room.
- **Per-cell reserves (cart).** With Seed Library or Seed Ledger, each managed cell keeps its own replant, set aside from its harvest, so a common species filling the two supply slots cannot starve a rarer one.
- **Bone meal slot.** A dedicated slot fills the fertilizer store. The plantables slot no longer takes bone meal.
- **Tool slot.** It holds a spare Axe or Treefeller. When a docked tree-working cart has no tool, or a broken one, the station installs the spare and moves the broken tool to its output. Nothing is swapped if the output is full.
- **Water tank.** A 16,000 mB, water-only tank is filled through a Fluid Connector and loads Field Hand carts.
- **Hoppers** sort plantables, bone meal, tools, and shears by item type.
- **GUI**, following [Machine GUI Design](../reference/machine-gui-design.md):
    - grouped slots with short labels above each group, and no overlaps;
    - hover hints and faded ghost items on empty slots;
    - Gear slot hints;
    - status and action tooltips for every state;
    - water, energy, and fertilizer gauges;
    - Stats rows for the docked cart.
- **Cart screen.** It gains the pump slot, a water gauge for Field Hand carts, Gear slot hints, and Work Range and idle-speed indicators.

## Loop safety

- `WORK_RANGE`, `IDLE_CART_SPEED`, `NO_TREE_WORK`, `SPRINKLER`, `IRRIGATION`, `REAP_AND_SOW`, and `ROLLING_REAP` are not yields.
- `CROP_TENDING` is declared with yield `output` and covered by the Forestry world source in `tools/moddex/recipe-loop-bounds.json`.
- That world source lists `#rngtech:cart_crops`, `minecraft:wheat`, and `minecraft:beetroot` as items no recipe may make.
- Bio Generator FE from crops counts as free world growth, like logs.

## Acceptance criteria

- With Nursery, a cart works three rows per side, and the cell cap binds on a farm of about 16 rail blocks at range 3.
- Existing carts with no new nodes keep range 1 and do no crop work. They move and break slower by design.
- Field Hand plants, harvests, and replants the four vanilla crops on farmland, tills and hydrates with a sprinkler, and never works trees.
- Breaking shows crack animations for an axe and for Treefeller batches, and abandoned breaks clear their cracks.
- The station's GUI has no overlapping text, every slot has a hover hint, and it supplies bone meal, tools, shears, plantables, and water.
- `./gradlew ciCheck`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` pass.
