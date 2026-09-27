# PRD: Passive Tree Framework

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and [Tree Farm Automation](../content/tree-farm-automation.md) for current behavior.


PRD status: Draft

Implementation status: Planned

Last updated: 2026-06-12

## Summary

Extract the Crusher's passive-tree domain logic into a small reusable internal framework so future machine mastery trees — and upcoming Crusher nodes such as Mute Machine Sound and Component Stage Support +1/+2 — plug in without duplicating unlock validation, stat application, or special-behavior queries.

The framework is domain-only: a `PassiveNode` interface, a progression view abstraction, and a generic `PassiveTree` utility. The Crusher remains the first and only concrete implementation. Crusher-specific policies (battery-slot blocking, matching Crush Head stage, Dense Parallel enablement, gear-conflict checks) stay in Crusher classes. No generic mastery screen is built until a second machine needs one.

## References

- [Progression System](../systems/progression.md)
- [Machine Stats](../reference/machine-stats.md)
- [Current Implementation Matrix](../reference/current-implementation.md)

Key code:

- `src/main/java/com/rngtech/rpg/progression/MachineProgressionState.java`
- `src/main/java/com/rngtech/rpg/progression/CrusherPassiveNode.java`
- `src/main/java/com/rngtech/rpg/progression/CrusherPassiveTree.java`
- `src/main/java/com/rngtech/rpg/progression/CrusherPassiveTreeLayout.java`
- `src/main/java/com/rngtech/rpg/progression/PassiveTreeLayouts.java`
- `src/main/java/com/rngtech/content/blockentity/CrusherBlockEntity.java`
- `src/main/java/com/rngtech/content/menu/CrusherMenu.java`
- `src/main/java/com/rngtech/client/screen/CrusherScreen.java`

## Problem

Crusher passive progression currently mixes node metadata, layout, links, stat effects, special behavior queries, unlock validation, and UI behavior inside `CrusherPassiveNode` and its consumers. Adding a second machine tree would force copy-paste of all of it.

Two concrete defects motivate the refactor beyond reuse:

- Unlock validation is duplicated with a latent semantic divergence: server-side `CrusherBlockEntity.unlockPassiveNode` treats a parentless node as locked, while client-side `CrusherMenu.hasUnlockedPassiveConnection` treats empty `parents()` as connected. Every current node has links, so the divergence is dormant — but it must be unified before more validation logic accretes.
- Special behaviors are hardcoded identity checks (`this == CELL_BYPASS`) with duplicated gear-conflict logic in both `CrusherBlockEntity.gearAllowsPassive` and `CrusherScreen.masteryNodeGearAllowsUnlock`. Upcoming nodes need extensible boolean flags (Mute Sound) and additive integer stats (Component Stage Support) instead of more identity checks.

Save compatibility is a hard constraint: node identity is `ordinal()` mapped to a bit in two 64-bit masks (`unlockedNodeMask`, `unlockedNodeMaskHigh`), 90 of 128 slots used. Existing Crusher enum constant order and mask semantics must not change.

## Goals

- A reusable `PassiveNode` interface/model: level requirement, kind, position, links, stat effects, special behavior flags, additive passive stats, icon key, translation key.
- Shared utilities for: unlock validation against progression state, parent/link checks, applying node effects into `MachineStatAccumulator`, querying special flags, and summing additive passive stats.
- `CrusherPassiveNode` implements the shared model in place — same enum, same constant order, bit-for-bit identical masks.
- Server and menu validate unlocks through one shared predicate via a progression view abstraction.
- Startup-time assertion of the index/mask invariant so accidental reordering is a hard failure instead of silent save corruption.
- Mute Machine Sound and Component Stage Support +1/+2 become append-a-node changes after this refactor.

## Non-Goals

- No generic mastery screen or generic menu; `CrusherScreen` only retypes private helpers where zero-risk.
- No change to `MachineProgressionState` codecs, fields, XP curve, or bit math.
- No move of the Crusher's effect-content machinery (`Identity`/`Special`/magnitude tables) into the framework; deferred until a second tree wants it.
- No data-driven/datapack tree definition; trees remain code-defined enums.
- No new passive nodes in this PRD — only the framework that makes them cheap.

## Framework Model

All new types live in `com.rngtech.rpg.progression`.

### `PassiveNodeKind`

Top-level enum moved verbatim from `CrusherPassiveNode.NodeKind` (STARTER/TRAVEL/NODE/NOTABLE/KEYSTONE with render sizes). The ~6 `CrusherPassiveNode.NodeKind` references in `CrusherScreen` are updated directly; no deprecated alias is kept.

### `PassiveProgressionView`

Interface: `boolean hasNode(int index)`, `int level()`, `int unspentPoints()`.

- `MachineProgressionState` implements it (all three methods already exist; only `implements` is added).
- `CrusherMenu` gets a private adapter over its synced `ContainerData` masks/level/points, so client-side validation runs the same shared logic as the server.

### `PassiveNodeFlag`

Enum for cross-machine boolean behaviors, seeded with `MUTE_MACHINE_SOUND` (unused until that node lands). Crusher specials (cell bypass, jaw mount, dense batching) deliberately do not move here — they remain named Crusher policies.

### `PassiveStatType`

Enum for additive integer passive stats, seeded with `COMPONENT_STAGE_SUPPORT`. These gate item insertion rather than flowing through `MachineStatAccumulator`.

### `PassiveNode`

Interface with:

- Abstract: `index()`, `requiredLevel()`, `kind()`, `x()`, `y()`, `alwaysAllocated()`, `grantsNothing()`, `parents()` (`List<? extends PassiveNode>`), `effects()`, `modifier()`, `masteryIconKey()`, `translationKey()`, `flags()` (`Set<PassiveNodeFlag>`), `passiveStat(PassiveStatType)`.
- Default methods moved verbatim from `CrusherPassiveNode`, parameterized on `index()`: `mask()`, `maskBank()`, `buttonId()` (100 + index), `size()` (from kind), `isUnlocked(PassiveProgressionView)`, `parentUnlocked(PassiveProgressionView)`.

Canonical parent semantics (resolves the menu/server divergence): a node with empty `parents()` is unlockable only if `alwaysAllocated()`. This matches current server behavior; the menu's lenient branch is dead code today. Documented in the interface javadoc.

### `PassiveTree<N extends PassiveNode>`

Final class wrapping `List<N> nodes`. Constructor validates: at most 128 nodes and `nodes.get(i).index() == i` — the save-format invariant, asserted in one place at class-load. Methods:

- `applyStats(MachineStatAccumulator, PassiveProgressionView)` — moved from `CrusherPassiveTree.applyStats`, same iteration and `grantsNothing` skip.
- `canUnlock(N, PassiveProgressionView)` — the full shared predicate: not already unlocked, not always-allocated, level requirement met, unspent points available, parent unlocked. Gear checks are not here; they are a machine policy hook the caller ANDs in.
- `hasUnlockedFlag(PassiveNodeFlag, PassiveProgressionView)` — any unlocked node carries the flag.
- `statTotal(PassiveStatType, PassiveProgressionView)` — sum over unlocked nodes.
- `byButtonId(int)`, `nodes()`, `contentMaxX()`/`contentMaxY()` (pan-bound scans currently inlined in `CrusherScreen`).

## Crusher Adaptation (behavior-preserving)

- `CrusherPassiveNode implements PassiveNode`: add `index()` returning `ordinal()`, `translationKey()` returning `"rngtech.mastery.node." + serializedName`, `flags()` returning `Set.of()`, `passiveStat(...)` returning 0. Existing accessors already match the interface; `kind()` retypes to `PassiveNodeKind`. Untouched: constant order, constructors, `createLinks()`, `createEffects()`/`Identity`/`Special`/magnitude tables, and `blocksBatteryCell()`/`requiresMatchingCrushHeadStage()`/`enablesDenseParallel()`. Existing `isUnlocked(MachineProgressionState)` overloads delegate to the interface defaults so call sites do not churn. Static `byButtonId` delegates to the tree.
- `CrusherPassiveTree`: keeps its exact public static API (so `CrusherBlockEntity` call sites are untouched), now backed by `public static final PassiveTree<CrusherPassiveNode> TREE = new PassiveTree<>(List.of(CrusherPassiveNode.values()))`. The three special-query statics remain here as named Crusher policies.
- `CrusherBlockEntity.unlockPassiveNode`: becomes `CrusherPassiveTree.TREE.canUnlock(node, progression) && gearAllowsPassive(node)` then `withUnlockedNode(node.index())`. `gearAllowsPassive` stays as-is.
- `CrusherMenu`: `hasPassiveNode`/`canUnlockPassiveNode`/`hasUnlockedPassiveConnection` delegate to `node.isUnlocked(view)` / `TREE.canUnlock(node, view)` / `node.parentUnlocked(view)` using the synced-data adapter. This removes the duplication and silently adopts the canonical empty-parents semantic (no observable change today).
- `CrusherScreen`: minimal. Retype private helpers that only use interface methods (`drawMasteryNode`, `drawMasteryLink`, `masteryNodeCenterX/Y`, `masteryNodeFill`, hover hit-test, pan-bound math via `TREE.contentMaxX()/Y()`) to `PassiveNode`. Tooltip gains one generic loop emitting lines for `flags()` alongside the existing Crusher-specific lines. `masteryNodeGearAllowsUnlock` and the `EnumMap` icon cache stay Crusher-typed. Iteration stays over `CrusherPassiveNode.values()`.

## Save And Mask Compatibility

- Enum constants: order and names untouched; new nodes are only ever appended. The `PassiveTree` constructor check plus the existing 128-node static guard turn accidental reordering into a hard startup failure.
- `MachineProgressionState` codec, stream codec, field semantics, and `withUnlockedNode` bit math: completely untouched.
- `mask()`/`maskBank()` move to interface defaults with identical expressions on `index()` ≡ `ordinal()` — bit-for-bit identical.
- `applyStats` ordering and the `grantsNothing` skip are preserved, and `createEffects` is untouched, so accumulated stats for any existing save are identical by construction.

## Future Nodes After This Refactor

- **Mute Machine Sound**: append a `MUTE_MACHINE_SOUND` constant (likely `grantsNothing = true`) overriding `flags()` to `Set.of(PassiveNodeFlag.MUTE_MACHINE_SOUND)`; add lang key and icon. Hook: `MachineSoundClient` (client-side, has the block entity) checks `CrusherPassiveTree.TREE.hasUnlockedFlag(MUTE_MACHINE_SOUND, be.machineProgression())` before starting loop/start/stop sounds — one framework query, zero new plumbing.
- **Component Stage Support +1/+2**: append two nodes overriding `passiveStat(COMPONENT_STAGE_SUPPORT)` to 1 each (stacking to +2). Hook: `CrusherBlockEntity.isAllowedCrushHead` becomes `head.material().stage() <= chassisMaterial().stage() + TREE.statTotal(COMPONENT_STAGE_SUPPORT, machineProgression())`. Explicit decision: `PRECISION_JAW_MOUNT`'s matching-stage requirement keeps comparing against the bare chassis stage (a +1-stage head is "not matching"); changing that later is one line at the same call site.
- **A second machine's tree**: new node enum implementing `PassiveNode`, its own `PassiveTree` instance, and its own layout class; validation, stat application, and queries come free. Only then consider sharing the effect-magnitude tables and a generic screen.

## Test Plan

There is no Java test source set, so verification is layered:

1. Compile gate: `./gradlew quickCheck` after each stage. The refactor is staged so each stage compiles: framework types first, then Crusher adaptation, then menu/screen retyping.
2. Startup self-checks: the `PassiveTree` constructor assertions run at class-load — launching the client validates index/mask invariants for free.
3. Manual save-compat smoke (the critical one): before the refactor, create a test world with a Crusher that has several nodes unlocked including `CELL_BYPASS`, `DENSE_BATCHING`, and `PRECISION_JAW_MOUNT`; record the stats panel values. After the refactor, load the same world and confirm: identical stat panel, battery slot still blocked, dense parallel still active, jaw-mount head restriction still enforced, mastery tab renders identically, locked/unlockable/unlocked tooltip states unchanged, and breaking and re-placing the machine (data-component round trip) preserves unlocks.
4. Optional, recommended: add a plain JUnit source set with tests for `PassiveTree.canUnlock`/`statTotal`/`hasUnlockedFlag` against a small fake `PassiveNode` enum and a record-backed `PassiveProgressionView` — these types have no Minecraft-registry dependencies, so tests need no bootstrap.

## Acceptance Criteria

- A world saved before the refactor loads with identical Crusher stats, unlocked nodes, and special behaviors.
- `CrusherPassiveNode` constant order and mask semantics are unchanged (asserted at startup by `PassiveTree`).
- Server and menu unlock validation share one predicate; the empty-parents semantic is the server's (documented).
- Battery-slot blocking, Crush Head stage matching, Dense Parallel, and gear-conflict checks remain in Crusher classes with unchanged behavior.
- `PassiveNodeFlag` and `PassiveStatType` queries exist and are exercised by at least the tooltip path, even before any node uses them.
- No generic mastery screen is introduced.
- `./gradlew quickCheck` passes.
- `mkdocs build` passes after docs are updated.

## Assumptions

- `index()` ≡ `ordinal()` permanently for `CrusherPassiveNode`; new nodes are appended only; 128 is an acceptable hard cap (90 used).
- Unifying on the server's empty-parents semantic is acceptable (no live behavior change; every current node has links).
- The `Identity`/`Special`/magnitude effect-generation tables stay Crusher-internal for now.
- `buttonId = 100 + index` remains the menu wire protocol for all future trees (each machine has its own menu, so IDs cannot collide across machines).
- Mute Sound is a per-machine unlock read client-side from the synced progression data component (the current sync path already delivers it, as the mastery tab renders from it).
- The moddex web editor's generated Java targets `CrusherPassiveTreeLayout`-style constants, which this plan does not change, so the tool keeps working unmodified.

## Open Questions

- Should the optional JUnit source set be added as part of this PRD or deferred?
- Should `PassiveNodeFlag` tooltip lines use one shared lang key pattern (`rngtech.mastery.flag.<name>`) from the start?
