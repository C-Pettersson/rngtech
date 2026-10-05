# PRD: Tree Farm Automation

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Tree Farm Automation](../content/tree-farm-automation.md) for current behavior.


PRD status: Draft

Implementation status: Prototype

Last updated: 2026-06-21

## Summary

Add grounded tree-farm automation for RNGTech. The system should automate planting, waiting, harvesting, and replanting trees through visible world interaction rather than a single block that internally converts saplings into logs.

The first implementation target is a Rail Forestry Cart: a powered cart that runs on vanilla rails, works beside the track, carries onboard FE and cargo charged or loaded by a Forestry Cart Station, plants saplings from onboard cargo, cuts managed trees with an installed modular Axe or Treefeller, and unloads drops to the station when docked. Later tree-farm variants can add a Big Saw Gantry for high-throughput row forestry and expensive Forestry Drones for irregular terrain and precision harvesting.

The design goal is industrial automation that still feels like Minecraft: rails, tools, gear, block placement, real trees, real drops, limited reach, and failure states the player can see and fix.

## References

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Visual Design](../reference/machine-visual-design.md)
- [Machine GUI Design](../reference/machine-gui-design.md)
- [Component Stages](../reference/component-stages.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Battery Cells](../content/battery-cells.md)
- [Universal Cable](../content/basic-wire.md)
- [Modular Field Tools](../content/modular-field-tools.md)

Key existing code to reuse:

- `src/main/java/com/rngtech/content/tool/ToolHeadFamily.java`
- `src/main/java/com/rngtech/content/tool/ToolBaseStatCatalog.java`
- `src/main/java/com/rngtech/content/item/ModularToolItem.java`
- `src/main/java/com/rngtech/content/item/BatteryCellItem.java`
- `src/main/java/com/rngtech/content/blockentity/BaseMachineBlockEntity.java`
- `src/main/java/com/rngtech/content/registry/ModCapabilities.java`

## Problem

RNGTech needs automated wood production, but a compact block that produces logs from FE or saplings would conflict with the mod's hard-tech identity. Tree farming should require visible infrastructure, installed gear, power planning, and a layout that interacts with the world.

The player constraints are:

- It should not feel magic.
- It should be real-world based where possible.
- It should interact with real Minecraft blocks.
- It should reuse existing gear systems.
- It may use modular tools.

Existing RNGTech systems already support Battery Cells, modular Axes, modular Treefellers, machine tabs, machine stats, refinement, and standard NeoForge capabilities. The tree-farm system should use those systems instead of creating a separate automation economy.

## Goals

- Add a tree-farm automation family that acts on real world saplings, logs, soil, rails, and item drops.
- Make the first implementation the Rail Forestry Cart because rails naturally constrain area, reach, pathing, and throughput.
- Require player-built infrastructure: station, rails, planting lanes, sapling input, tool gear, and power.
- Reuse modular Axes and Treefellers as the cart's cutting tool.
- Reuse Battery Cells as the cart power buffer.
- Use vanilla rails for the first pass.
- Plant saplings into actual valid world positions.
- Wait for normal tree growth by default.
- Harvest only trees the system planted or explicitly adopted as managed planting cells.
- Clear connected snapshot leaves before cutting logs.
- Break logs one at a time or in bounded connected-log batches, with FE and tool-wear costs per log.
- Send saplings from leaf drops to cart sapling cargo first, with overflow to output cargo.
- Stop safely when output is full, power is missing, the tool is broken, the rail route is blocked, or the target tree exceeds the current tool/stage limits.
- Keep scanning and harvesting performance bounded.
- Preserve room for later Big Saw Gantry and Forestry Drone variants without requiring them in the first implementation.

## Non-Goals

- Do not implement mecha units or general-purpose worker entities.
- Do not implement drones in the first pass.
- Do not implement the Big Saw Gantry in the first pass.
- Do not add a single-block tree generator.
- Do not generate logs without world logs existing.
- Do not force-grow trees for free.
- Do not harvest arbitrary nearby log structures.
- Do not add broad item-transfer networks.
- Do not add fake crop or plant farming to this PRD.
- Do not add hard dependencies on optional tree or farming mods.
- Do not implement fertilizer, pruning, sap collection, charcoal drying, or tree breeding in the first pass.
- Do not solve every modded tree shape in the first pass.

## Product Model

Tree farm automation has three planned actuator styles.

| Variant | Implementation target | Role |
|---|---|---|
| Rail Forestry Cart | First pass | Early physical automation tied to vanilla rails |
| Big Saw Gantry | Later | Mid-game industrial row forestry with high throughput |
| Forestry Drones | Later | Expensive flexible forestry for irregular terrain and large trees |

Only the Rail Forestry Cart is in scope for the initial implementation.

## Rail Forestry Cart Overview

The Rail Forestry Cart system has two core pieces.

| Content | Type | Role |
|---|---|---|
| Forestry Cart Station | Placed machine block | Physical transfer dock for sapling loading, output unloading, FE charging, connector ports, station output, and optional cart holding |
| Forestry Cart | Entity and item | Owns Battery Cell and tool Gear, cargo, managed cells, owner FakePlayer profile, FSM state, snapshots, rail movement, planting, and harvesting |

The cart is the machine. The station is a service dock.

The cart should not be a general minecart replacement. It is a specialized forestry machine that works independently on rails and uses stations only when physical transfer, recharge, or holding is needed.

## Player Flow

1. Craft a Forestry Cart Station.
2. Place the station beside a rail.
3. Build a rail route through or around an orchard area.
4. Place a Forestry Cart on the rail.
5. Right-click the cart to install a valid Battery Cell and modular Axe or Treefeller.
6. Insert saplings into the station Process inventory so the station can load the cart while docked.
7. Close the cart UI so the cart can resume its FSM.
8. Install station connector ports and supply FE to the station through a Universal Cable or adjacent energy source.
9. The station charges any physically docked cart and loads saplings into its onboard cargo.
10. The cart follows rails, plants saplings from onboard cargo beside the route, waits for grown trees, harvests managed trees into onboard output cargo, unloads at station output when physically docked, and repeats.

The farm layout is physical. Track placement, planting-lane spacing, obstructions, station position, sapling supply, output space, power, and tool quality all affect throughput.

## Work Area

The first implementation should not use an abstract GUI radius. The rail line defines the work area.

Rules:

- The cart only works while on valid rails.
- Each rail segment exposes left and right planting lanes.
- The default planting position is offset from the rail so the cart can pass without colliding with trunks.
- The cart scans a small bounded set of positions near its current rail segment each tick.
- The cart records managed planting cells.
- A managed planting cell is the root position for one tree.
- A planting cell can be empty, planted, grown, blocked, invalid soil, or exhausted.

Initial constants can be conservative:

| Parameter | Initial target |
|---|---:|
| Planting lane offsets | 1 block from rail |
| Scan samples per tick | 1 to 4 positions |
| Default max connected logs per tree | From installed tool's `TREE_FELL_LIMIT`, or a small cap for Axe |
| Default max tree height | Tool or station stage derived |
| Default route memory | Bounded by station stage or config |

## Managed Planting Cells

The system must avoid harvesting player-built structures.

Managed cell rules:

- When the cart plants a sapling, the cart records the planting position and sapling item.
- The cart may harvest trees rooted at track-adjacent log bases.
- A manually placed sapling may be adopted only if it is in a valid planting lane and the station is configured to adopt unmanaged saplings.
- Existing log bases directly beside the track can be snapshotted for harvesting.
- Logs outside the connected tree rooted at the current adjacent log base are ignored.
- If the managed cell becomes invalid, the cart marks it blocked instead of expanding harvest behavior.

This is the main safety boundary between a forestry machine and arbitrary world-block modifiers.

## Planting Behavior

The cart plants saplings into the world.

Requirements:

- Consume a sapling item from the cart's onboard sapling cargo, which the station fills while the cart is docked.
- Obey normal placement rules.
- Require valid soil.
- Require adequate clearance.
- Do not replace solid blocks.
- Do not plant outside the rail-defined planting lanes.
- Do not plant if the cart is in a transfer-seeking, blocked, managed, held, or off-rail state.
- Persist the planted position as a managed cell.

The first pass supports vanilla saplings. Optional mod support should be data-driven later.

## Growth Behavior

Default behavior is passive waiting.

The cart does not create grown trees. It records planted saplings and periodically checks whether normal Minecraft growth has produced logs at the managed cell.

Deferred fertilizer behavior:

- A future Fertilizer Injector may consume bonemeal or fertilizer items.
- It must apply those items to real sapling blocks.
- It should inherit vanilla failure/chance behavior where possible.
- It must not force growth without consuming a real item.

## Tree Detection

For each current track-adjacent cell, the cart or station can detect a candidate tree.

Detection rules:

- If the root block is still a sapling, the cell is waiting.
- If the adjacent root position is a log base, it becomes a harvest candidate.
- Connected logs and leaves are discovered with a bounded breadth-first search.
- Vanilla `BlockTags.LOGS` is the first-pass log source.
- Vanilla `BlockTags.LEAVES` is the first-pass leaf source.
- The connected tree search must cap log count, leaf count, and vertical range.
- The search must not run over a large area in one tick.

Large, branching, or nonstandard trees may exceed the current tool/station limit. In that case the cart should stop or mark the cell as "tree too large" instead of trying to harvest indefinitely.

## Harvest Behavior

The cart clears leaves and cuts logs physically.

Harvest loop:

1. Stop movement beside a track-adjacent log base.
2. Build or resume the persisted bounded snapshot for that log base.
3. If snapshot leaves remain, target a leaf before any log.
4. Confirm cargo space before breaking the next block or Treefeller log batch.
5. Confirm the installed tool can harvest logs, or that leaf cleanup is allowed.
6. Confirm enough FE is available for the action or full Treefeller log batch.
7. Remove one leaf, break one Axe log, or break the preflighted Treefeller log batch.
8. With shears installed, produce normal leaf drops through the vanilla loot path; without shears, remove leaves without drops.
9. Insert sapling drops into cart sapling cargo first, then overflow to output cargo.
10. Insert other drops into the cart's onboard output cargo.
11. Unload cart output cargo into station output when docked.
12. Apply FE cost, modular tool wear for logs, and amortized cart shears wear for collected leaves; Treefeller batches pay per log.
13. Continue next work interval until the snapshot is complete or blocked, then check for another adjacent log base before planting.

The first pass uses shears as the leaf-loot collection gate. Vanilla-compatible shears remain the starter option, while staged RNGTech Pruning Shears provide the durable Forestry Cart path. The cart rolls normal leaf drops so saplings can feed replanting, then applies deterministic cart shears wear after successful collected leaves. Without usable shears, leaves are still removed before logs but do not produce drops.

## Tool Integration

The cart uses its own installed modular tool.

Valid tools:

- Modular Axe: valid but slow; cuts one log at a time.
- Modular Treefeller: preferred; after leaf cleanup, cuts the current snapshot logs in one action up to `TREE_FELL_LIMIT`.

Tool stats should matter:

| Existing stat or behavior | Forestry use |
|---|---|
| `MINING_SPEED` | Work interval or cut time |
| `FE_USAGE` | Added FE per log |
| `DURABILITY` | Tool lifetime |
| `TREE_FELL_LIMIT` | Maximum connected logs in a Treefeller snapshot batch |
| `STABILITY` | Chance to avoid durability cost, matching existing tool behavior where practical |
| `CONTROL` | Durability protection when FE is available, matching existing tool behavior where practical |
| Battery support on the tool | Relevant only if the implementation uses an installed tool Battery Cell directly |

The station should not consume or replace the tool when it breaks. A broken tool remains installed and blocks work until repaired or replaced.

Superseded by the [Companion Rebalance](companion-rebalance.md): the renamed Companion Station has a spare-tool slot and installs the spare when a docked tree-working cart's tool is missing or broken, moving the broken tool to its outputs.

## Energy Model

The Forestry Cart Station is a physical dock that charges any docked Forestry Cart's installed Battery Cell.

Rules:

- The station has a small internal FE buffer plus one optional station Battery Cell slot.
- Station FE input requires an installed Energy Connector and is capped by its tier.
- The cart owns the Battery Cell Gear slot.
- External FE input fills the station buffer, charges the optional station Battery Cell, and charges any physically docked cart Battery Cell when possible.
- Route movement, scanning, planting, and cutting draw FE from the cart Battery Cell.
- If the cart runs out of FE, planting and harvesting pause.
- If the cart runs out of FE, rail movement continues at reduced unpowered speed so the cart can return to the dock.

Energy costs:

| Action | Cost model |
|---|---|
| Movement | Small FE/t while moving |
| Scanning | Small FE per sample or per scan interval |
| Planting | FE per placed sapling |
| Cutting | FE per log, modified by installed tool and machine stats |
| Docking/unloading | Free or negligible |

No Battery Cell behavior should be harsh. Recommended first-pass rule: the station cannot deploy or operate a cart without a valid Battery Cell.

## Inventory And Automation

Suggested station slots:

| Slot | Tab | Automation |
|---|---|---|
| Sapling input | Station | Top insertion |
| Shears input | Station | Top insertion |
| Output slots | Station | Bottom extraction |
| Cart Battery Cell | Cart | Manual only through cart right-click UI |
| Modular Axe/Treefeller | Cart | Manual through cart right-click UI; the station installs its spare when the docked cart's tool is missing or broken |
| Cart shears | Cart | Manual through cart right-click UI; station can fill an empty slot while docked |
| Cart cargo | Cart | Manual only through cart right-click UI |
| Station Battery Cell | Gear | Manual only |
| Station connector ports | Gear | Manual only |
| Refinement catalyst | Refinement | Manual only |

The cart should have a small onboard inventory split between sapling cargo and output cargo. It should not expose broad item automation. The station is the automation surface: top insertion fills the station sapling staging slot and shears staging slot, dock transfer loads saplings and replacement shears into any physically docked cart when matching cart storage is available, dock transfer unloads harvested cargo into station output, and bottom extraction removes station output.

## Station UI

Tabs should follow existing machine conventions.

Station tab:

- Sapling input.
- Shears input.
- Output slots.
- Internal station energy bar.
- Work status icon.
- Hold-at-station control.

Cart management UI:

- Cart Battery Cell slot.
- Modular tool slot.
- Shears slot.
- Onboard sapling cargo.
- Onboard output cargo.
- Cart FE indicator.
- Work status icon.
- Current action icon.
- Scan debug toggle.
- Stats tab.

Opening this UI pauses cart movement and forestry work until the final viewer closes.

Gear tab:

- Optional station Battery Cell slot.
- Energy, Item, and Fluid Connector slots.

Stats tab:

- Route length or known rail segments.
- Managed planting cells.
- Active planting lanes.
- Max tree height.
- Max connected logs.
- FE per log.
- Movement FE/t.
- Scan rate.
- Tool condition.

Refinement tab:

- Present only when the station has a proper `MachineType` profile and is a valid placed-machine refinement target.

Visible text should stay minimal. Exact status and numeric detail should be in hover text.

## Status And Failure States

The station and cart should expose clear states.

Required states:

- Ready
- Missing cart
- Missing rail
- Missing Battery Cell
- Missing tool
- Invalid tool
- Broken tool
- No saplings
- Output full
- No power
- Route blocked
- Cart off rail
- Cart unloaded
- Tree too large
- Invalid soil
- Planting blocked
- Harvest blocked

Failure should pause work before destructive action. For example, output-full should stop before breaking a log.

## Entity And Station Architecture

The implementation needs new entity infrastructure because RNGTech currently has no entity registry.

New or changed registry classes:

- Add `ModEntityTypes`.
- Register it from `RNGTech`.
- Register the Forestry Cart renderer from `RNGTechClient`.
- Register station block, item, block entity, menu, capabilities, recipes, models, loot, and language.

New gameplay classes:

- `ForestryCartStationBlock`
- `ForestryCartStationBlockEntity`
- `ForestryCartStationMenu`
- `ForestryCartStationScreen`
- `ForestryCartEntity`
- `ForestryCartItem`
- `ForestryCartRenderer`

The cart stores:

- Owner UUID/name for FakePlayer attribution.
- Route direction.
- FSM/work state.
- Small onboard inventory for station-loaded saplings and harvested output cargo.
- Cart Gear inventory for Battery Cell, modular tool, and shears.
- Managed planting cells.
- Persisted active harvest snapshot.
- Cooldown, status/action, transfer-seeking reason, and scan-debug state.

The station stores:

- Inventory.
- Internal energy.
- Optional hold-at-dock flag.
- Status code.
- Machine traits and refinement inventory when enabled.

The station must not store a cart UUID or owning station/cart relationship. It services any Forestry Cart physically present on its dock rail.

## Block Breaking And Protection

The implementation should use a protection-aware block breaking path.

Requirements:

- Validate the target block is harvestable by the installed tool.
- Confirm the cart owner is allowed to break the block where possible.
- Generate drops through the block's normal loot path with the installed tool stack.
- Avoid bypassing NeoForge events that protection or claim mods may depend on.
- Remove the block only after output capacity and energy/tool checks pass.

If a first internal slice cannot complete protection integration, that slice must not be presented as survival-ready. The PRD remains incomplete until the block break path is safe.

## Balance Targets

The rail cart should be useful but not free.

Early identity:

- Cheap compared to gantry or drones.
- Requires rail infrastructure.
- Slow enough that layout matters.
- Strongly benefits from Treefeller tools.
- Stops often for power, output, and route issues.

Mid-game identity:

- Better tools and cells make the same rail layout more comfortable.
- Refinement can improve speed, FE use, scan rate, or tool wear.
- Still lower peak throughput than a future Big Saw Gantry on perfect rows.

High-stage identity:

- Can support longer routes, more managed cells, and larger trees.
- Still worse than future drones on irregular terrain.

## Optional Mod Support

First pass supports vanilla tree behavior through tags.

Later optional mod support should be data-driven:

- Sapling block.
- Sapling item.
- Valid soil or substrate.
- Log tags or explicit log blocks.
- Leaf tags or explicit leaf blocks.
- Trunk pattern hints.
- Maximum safe harvest volume.
- 2x2 sapling rules where needed.

No optional tree mod should be a required dependency.

## Later Variants

### Big Saw Gantry

Deferred mid-game bulk forestry.

Core idea:

- Two pillars or a frame hold a large saw span.
- The saw moves along rows.
- Best for straight, planned plantations.
- Higher throughput than rail cart.
- Poorer flexibility than drones.

### Forestry Drones

Deferred expensive forestry.

Core idea:

- Drone Dock attached to an Arbor or Forestry Station.
- Survey drones scan.
- Pruner drones clean leaves or branches.
- Logger drones cut top-down.
- Heavy logger or swarm docks support high-stage forestry.

Drones should be expensive precision tools, not a replacement for rail and gantry bulk throughput.

## Implementation Slices

1. Add PRD and implementation state file.
2. Add `ModEntityTypes` and a bare Forestry Cart entity that can spawn on rails.
3. Add Forestry Cart item and basic client renderer.
4. Add Forestry Cart Station block, block entity, inventory, FE storage, and capabilities.
5. Add station menu/screen with Station, Cart, Gear, Stats, and optional Refinement tabs.
6. Persist cart owner UUID/name and implement the independent cart FSM.
7. Implement rail movement, docking, and default dead-end reversal.
8. Implement planting lanes and managed planting cell storage.
9. Implement vanilla sapling planting.
10. Implement managed tree detection.
11. Implement safe log harvest queue and real drop insertion.
12. Apply FE costs, Battery Cell behavior, modular tool validation, and tool wear.
13. Add recipes, loot tables, models, language, and docs.
14. Update Current Implementation Matrix only after code-backed behavior exists.
15. Run verification.

## Acceptance Criteria

- Forestry Cart Station is registered, craftable, placeable, and has a working screen.
- Forestry Cart is registered, placeable on rails, rendered, saved, and recoverable.
- Station does not deploy, claim, or link to carts.
- Cart follows vanilla rails and recognizes any physical station dock for transfer.
- Cart plants vanilla saplings beside rails only when placement is valid.
- Cart records managed planting cells and active snapshots.
- Cart harvests track-adjacent managed cells and adjacent log-base snapshots only.
- Cart does not harvest arbitrary log structures away from the rail-defined work lanes.
- Cart clears leaves before cutting logs and uses bounded, protection-aware block removal.
- Treefeller-equipped carts batch-cut snapshot logs after leaf cleanup, capped by `TREE_FELL_LIMIT`; Axe-equipped carts continue cutting one log per interval.
- Drops go into cart sapling cargo or output cargo before station unloading, or work pauses before breaking.
- FE and tool durability are charged per successful log or collected leaf action; Treefeller batch FE and wear are applied per log, and cart shears wear is amortized by shears type.
- Missing power, full output, invalid tool, broken tool, blocked route, invalid soil, and too-large trees produce non-destructive pause states.
- Cart right-click management pauses movement/work while open and resumes after the final viewer closes.
- Cart and station Gear slots reject invalid items.
- Sided item and energy automation match documented station behavior.
- Docs clearly mark the feature as `Planned` until implemented.
- `.\gradlew.bat quickCheck` passes after code/resources are added.
- `mkdocs build` passes after docs are updated.

## Open Questions

- Resolved for first pass: players place carts directly on rails; carts do not bind to stations.
- Should the station require a Battery Cell to operate, or allow tiny internal-buffer operation with severe penalties?
- Should manual sapling adoption be enabled by default, disabled by default, or require a specific tool action?
- Should rail route bounds be limited by distance from station, number of rail segments scanned, or component stage?
- Resolved for first pass: block breaking uses the cart owner FakePlayer profile, falling back to the shared server FakePlayer for ownerless legacy carts.
- Resolved for first pass: the cart carries saplings and harvested drops locally, and the station loads/unloads that cargo only while the cart is docked.
- Should 2x2 trees be deferred entirely, or supported through managed multi-cell sapling groups in the first pass?
