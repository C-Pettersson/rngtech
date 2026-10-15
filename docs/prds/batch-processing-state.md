# Batch Processing Implementation State

Source: [Batch Processing PRD](batch-processing.md).

Status: **Prototype** for the 2.0 release. Slice 1 is merged and passed an in-game check on 2026-10-06. Slice 2 passed an in-game check on 2026-10-15 and merged as #164. Slices 3 and 4 pass the automated checks and ship with the [Heat Model](heat-model.md) prototype for one playtest.

## Scope checklist

- [x] Survey current parallel behavior across the Lead Furnace, Crusher, Metal Press, Melter, and Resonance Calibrator.
- [x] Draft the PRD and this state page.
- [ ] Resolve the PRD's open questions.
- [x] Slice 1: Batch Size, Batch Overhead, shared batch helper, `DENSE_PARALLEL` removal, Single Charge, Calibrator lanes to Batch Size.
- [x] Slice 2: Lead Furnace shared Heat Core, Furnace and Alloy Furnace batching.
- [x] Slice 3: Dense Batching, Batch Overhead path, and ascendancy values.
- [x] Slice 4: stat display, hover text, Jade, language, and canonical docs. Progress pips are not done.
- [ ] In-game playtest.

## Decisions

Recorded 2026-10-05:

- Parallel work follows the hybrid model: lanes come from chassis identity, and Batch Size works per lane. Throughput is lanes × batch.
- Gear is shared across lanes. The Lead Furnace no longer needs four Heat Cores.
- Parallel Jobs is renamed Batch Size and counts total items per cycle, base 1.
- Batching costs time through a new Batch Overhead stat, so extra items lengthen the cycle.
- Batch Size and Batch Overhead are active on the Furnace and Alloy Furnace.
- Crucible Heart and Shared Hearth move to a separate Furnace heat model PRD. Heat batching with 40% less Max Temperature was dropped: with real recipe and chassis temperatures it rarely batches more than 1–2.
- The Resonance Calibrator's lanes are a batch size on one input slot, not lanes, and become chassis Batch Size.

Recorded 2026-10-06, during slice 1:

- Ascendancy values keep today's real totals. Today's code counted them as `1 + Parallel Jobs` on a base of 1, one more than their text said. Drop Hammer becomes +4 and Second Crucible +2; Anvil Mass, Forge Line, and Triple Crucible keep their values.
- Resonance Array becomes 100% increased Batch Size, doubling the chassis lanes as its text said. Today's code counted lanes twice, so Lead and Tungstensteel calibrators with Resonance Array drop from 8 and 15 copies to 4 and 6.
- Fused Crucibles drops from 30% to 20% Processing Speed per point of Batch Size, which keeps today's 60% with Second Crucible alone.
- The Resonance Calibrator now pays FE for every copy in a batch. Before, a multi-lane batch paid one craft's FE.
- Single Charge is a parameter of the shared batch helper, not a new behavior. Refiner’s Oath and Fused Crucibles pass it.
- Machines that recomputed their batch every tick (Metal Press, Melter, Resonance Calibrator) now lock it at cycle start, like the Crusher. A locked batch shrinks only if its input has gone.
- `DENSE_PARALLEL` stays as an enum constant so stored traits decode; nothing grants or reads it. The Crusher Throughput affix keeps its `dense_parallel` mod group ID.

Recorded 2026-10-15, during slice 2:

- Shared Hearth gets an interim rule until the [Heat Model](heat-model.md) replaces it: a Lead Furnace lane warming up starts at the hottest working lane's temperature, up to its own target. With one shared core, its old 90%-of-the-hottest-core rule had nothing left to share.
- Every Furnace has one Gear slot. The Heat Core's stats apply to the whole Furnace, so the per-lane and "strongest core" stat paths are gone. Gear stats now come before attribute conversions on the Furnace, as on the other machines.
- Heating a lane, and Overdrive's extra heat ticks, cost one item's FE rate however large the batch. Working ticks pay per item.
- The Stage 0 Furnace burns one item's burn time per batched item, spread over the longer cycle in thousandths of a tick. Fuel per item does not change.
- A failed batch ruins every item in it, as on the Metal Press. Crucible Heart keeps its rule: one extra item after the batch when the lane runs at twice the target.
- A batch keeps output room for its base items, so Super Output or a ledger payout never strands a paid-for item.
- The Corruption `batching_machines` pool now includes the Furnace, Electric Furnace, and Alloy Furnace.

Recorded 2026-10-15, during slice 3:

- Dense Batching's wait applies on every batching machine through a `DENSE_BATCHING` behavior. A full batch is the batch the machine may lock, capped by what its input slots (and the Melter's tank) can hold, so a recipe that cannot batch, such as a Metal Press circuit, or a batching opt-out never waits.
- A machine waiting for its batch shows **Filling Batch**; one with enough input but not enough output room for the batch shows its usual output-full status.
- The Batch Overhead path is an `arc` extra pocket beside Dense Batching. Existing node IDs and positions are unchanged; the catalog grows from 1,348 to 1,351 nodes.

## Progress log

- 2026-10-05: Surveyed current behavior and wrote the PRD and this state page.
- 2026-10-06, slice 1:
    - `PARALLEL_JOBS` is renamed `BATCH_SIZE`; the stat codec and authored-data parser accept the old name. `BATCH_OVERHEAD` is a new stat with base 25 and a floor of 5.
    - `BatchProcessing` holds the shared rules: Batch Size clamp, opt-out, Batch Overhead time scaling, and the largest-fitting batch search.
    - Crusher, Metal Press, Melter, and Resonance Calibrator use it. Each locks its batch at cycle start, scales cycle time by Batch Overhead, and pays FE per item.
    - `DENSE_PARALLEL` is gone from chassis implicits, the Crusher Throughput affix, the Dense Batching keystone, and every gate.
    - Canonical docs, generated ascendancy tables, and Moddex data are updated.
    - The Resonance Calibrator, Metal Press, and Melter Stats tabs show Batch Size. The Press and Melter never showed Parallel Jobs, so batches from Drop Forge, Twin Crucible, or Dense Batching were invisible there. The Calibrator chassis tooltip names its base Batch Size.
    - In-game check passed: Crusher, Metal Press, Melter, and Resonance Calibrator batching, plus the new Stats tab rows.
    - Still open for slice 4: Lane Sync, Shared Field, and Wide Tolerance still say "lanes".
- 2026-10-15, slice 2:
    - Lead Furnace has one Heat Core slot for all four lanes. Cores saved in the three retired slots move to an adjacent inventory, or drop once a player is near.
    - Furnace lanes and the Alloy Furnace lock a batch at cycle start, scale cycle time by Batch Overhead, and pay FE and grant XP per item. The Alloy Furnace batches whole recipe sets.
    - Furnace and Alloy Furnace families read Batch Size, so Dense Batching, Uniques, and Corruption reach them. Both Stats tabs show Batch Size.
    - GameTests cover one core running four Lead Furnace lanes, a Furnace batch, an Alloy Furnace recipe-set batch, and a four-core Lead Furnace moving its extra cores out.
    - Wiki, Machine Stats, Current Implementation, and the generated ascendancy tables are updated.
    - In-game check passed: Lead Furnace with one core, a batched Furnace and Alloy Furnace, and a world saved with a four-core Lead Furnace.
- 2026-10-15, slice 3:
    - Dense Batching is +3 Batch Size and a full-batch wait, with its speed and Energy Usage costs removed.
    - The Batch Overhead path adds two Batching nodes (8% reduced Batch Overhead each) and Even Loading (15% reduced Batch Overhead, +1 Batch Size).
    - Drop Hammer's Heat Window drawback is −15%; Second Crucible no longer costs 15% more Energy Use.
    - A GameTest covers the full-batch wait on the Furnace.
- 2026-10-15, slice 4:
    - Jade shows each batching machine's locked batch.
    - Lane Sync, Shared Field, and Wide Tolerance no longer say "lanes".
    - Under the heat model a batch charges each item's heat and takes a larger load dip, replacing slice 2's one-item warmup rate.
    - Batch progress pips are not done; the progress bar covers the whole batch.
