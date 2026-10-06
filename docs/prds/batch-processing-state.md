# Batch Processing Implementation State

Source: [Batch Processing PRD](batch-processing.md).

Status: **Prototype** for the 2.0 release. Slice 1 is in on `feature/batch-processing` and passes the automated checks; it still needs an in-game playtest.

## Scope checklist

- [x] Survey current parallel behavior across the Lead Furnace, Crusher, Metal Press, Melter, and Resonance Calibrator.
- [x] Draft the PRD and this state page.
- [ ] Resolve the PRD's open questions.
- [x] Slice 1: Batch Size, Batch Overhead, shared batch helper, `DENSE_PARALLEL` removal, Single Charge, Calibrator lanes to Batch Size.
- [ ] Slice 2: Lead Furnace shared Heat Core, Furnace and Alloy Furnace batching.
- [ ] Slice 3: Dense Batching, Batch Overhead path, and ascendancy values.
- [ ] Slice 4: stat display, hover text, progress pips, Jade, language, and canonical docs.
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

## Progress log

- 2026-10-05: Surveyed current behavior and wrote the PRD and this state page.
- 2026-10-06, slice 1:
    - `PARALLEL_JOBS` is renamed `BATCH_SIZE`; the stat codec and authored-data parser accept the old name. `BATCH_OVERHEAD` is a new stat with base 25 and a floor of 5.
    - `BatchProcessing` holds the shared rules: Batch Size clamp, opt-out, Batch Overhead time scaling, and the largest-fitting batch search.
    - Crusher, Metal Press, Melter, and Resonance Calibrator use it. Each locks its batch at cycle start, scales cycle time by Batch Overhead, and pays FE per item.
    - `DENSE_PARALLEL` is gone from chassis implicits, the Crusher Throughput affix, the Dense Batching keystone, and every gate.
    - Canonical docs, generated ascendancy tables, and Moddex data are updated.
    - The Calibrator Stats tab shows Batch Size, and the chassis tooltip names its base Batch Size.
    - Still open for slice 4: Lane Sync, Shared Field, and Wide Tolerance still say "lanes".
