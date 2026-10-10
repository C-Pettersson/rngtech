# PRD: Batch Processing

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the machine content pages for current behavior.

PRD status: Draft

Implementation status: Planned

Release: 2.0

Last updated: 2026-10-05

## Summary

Machines handle "parallel" work in four different ways today:

| Machine | Today | Gear | Stat meaning |
|---|---|---|---|
| Lead Furnace | Four independent lanes from `INPUT_SLOTS` | One Heat Core per lane | Does not read `PARALLEL_JOBS` |
| Crusher | N jobs from one input stack, gated by `DENSE_PARALLEL` | One Crush Head | `PARALLEL_JOBS` is the total job count |
| Metal Press (Drop Forge) | N jobs from one input stack | One | `1 + PARALLEL_JOBS` |
| Melter (Twin Crucible) | N melts per cycle | One | `1 + PARALLEL_JOBS` |
| Resonance Calibrator | Chassis "lanes" × (1 + `PARALLEL_JOBS`) operations from one input slot | One | `1 + PARALLEL_JOBS` |

The Lead Furnace and the Lead and Tungstensteel Calibrators carry `DENSE_PARALLEL`, but only the Crusher reads it. Extra jobs also cost no extra time, so batching feels like free multiplication.

This PRD replaces all of that with one model:

- A **lane** is a physical input/output pair. Lanes come only from chassis identity.
- A **batch** is several items from one lane's input stack, worked together in one cycle.
- **Batch Size** replaces Parallel Jobs. It counts total items per cycle and is 1 by default.
- **Batch Overhead** makes every extra item lengthen the cycle, so batching pays off less with each extra item.
- Throughput is lanes × batch, shown the same way on every machine.

The work lands in four slices. Each slice passes every check on its own.

1. Batch Size, Batch Overhead, and a shared batch helper; Crusher, Metal Press, Melter, and Calibrator move onto it.
2. Lead Furnace shared Heat Core, Furnace and Alloy Furnace batching.
3. Passive tree and ascendancy rework.
4. UI, Jade, documentation, and language.

Progress, decisions, and verification are tracked on [Batch Processing State](batch-processing-state.md).

## References

- [Machine Guidelines](../reference/machine-guidelines.md)
- [Machine Stats](../reference/machine-stats.md)
- [Machine GUI Design](../reference/machine-gui-design.md)
- [Shared Machine Mega Passive Tree PRD](machine-mega-passive-tree.md)
- [Machine Ascendancies PRD](machine-ascendancies.md)
- [Passive Tree Design Rules](../reference/passive-tree-design-rules.md)

Key existing code:

- `src/main/java/com/rngtech/rpg/MachineStat.java` and `MachineStatDisplay.java`
- `src/main/java/com/rngtech/rpg/MachineImplicitCatalog.java` and `MachineBehavior.java`
- `src/main/java/com/rngtech/rpg/progression/MachineMasteryFamily.java` and `AscendancyFormulas.java`
- `src/main/java/com/rngtech/content/blockentity/CrusherBlockEntity.java` (`parallelJobs`, `availableJobs`)
- `src/main/java/com/rngtech/content/blockentity/FurnaceBlockEntity.java` (lanes, `applyLaneGearStats`, Crucible Heart in `process`)
- `src/main/java/com/rngtech/content/blockentity/MetalPressBlockEntity.java` (`batchJobs`)
- `src/main/java/com/rngtech/content/blockentity/MelterBlockEntity.java` (`parallelMelts`)
- `src/main/java/com/rngtech/content/blockentity/ResonanceCalibratorBlockEntity.java` (`operationsFor`)
- `src/main/java/com/rngtech/content/calibration/ResonanceCalibratorChassis.java`
- `src/main/resources/data/rngtech/mastery/machine_tree.json` and `ascendancies/`
- `tools/moddex/author-mega-tree.mjs`

## Goals

- One batching rule on every recipe machine, with one stat name and one way to read it.
- Batching feels physical: a bigger load takes longer, costs FE per item, and needs the input and output room to match.
- Batch Size can be changed safely by affixes, refinement, and Mastery, because it never adds or removes slots.
- No machine needs more than one set of Gear to reach full throughput.
- Batch Overhead gives the passive tree a second batching axis besides `+N` nodes.

## Non-goals

- Adding lanes to machines that do not have them today.
- Rolled, refined, or passive lane counts.
- Recipe changes.
- Renaming registry IDs or affix IDs.
- A shared item-storage system.

## Terms

| Term | Definition |
|---|---|
| Lane | An input/output pair that runs its own recipe and progress. Lane count is fixed by the chassis. Only the Lead Furnace has more than one. |
| Batch | The items one lane takes from its input stack and works together in one cycle. |
| Batch Size | The most items one lane batches per cycle. |
| Batch Overhead | How much longer a cycle takes for each item beyond the first. |

The canonical definitions move into [Machine Guidelines](../reference/machine-guidelines.md) and [Machine Stats](../reference/machine-stats.md) when slice 4 lands. Other pages link to them.

## Batch Size

`BATCH_SIZE` replaces `PARALLEL_JOBS`.

- Total items per lane per cycle. The base is 1 on every machine. Nodes, affixes, and chassis use `ADD`.
- Whole number, clamped to 1–16.
- There is no gate behavior. A Batch Size above 1 always batches, unless a batching opt-out (see below) applies.
- The enum constant is renamed. The stat codec still accepts the legacy name `parallel_jobs`, so stored affixes, item data, and datapacks keep loading.

At the start of each cycle a lane locks its batch:

```text
batch = min(Batch Size,
            input items / recipe input count,
            fluid or catalyst sets available,
            largest batch whose whole output fits)
```

- A batch locked at cycle start does not change during the cycle.
- If fewer items are loaded than Batch Size, the lane runs what is loaded. Dense Batching changes this (see the passive tree section).
- Output Amount, Super Output, and Instant Process roll per item.
- Machine XP is granted per completed item, as [Machine Guidelines](../reference/machine-guidelines.md) already require.
- Recipe-level limits stay. The Metal Press still never batches circuit recipes.

## Batch Overhead

`BATCH_OVERHEAD` is a percentage. The base is 25 on every machine.

```text
cycle time   = base cycle time × (1 + Batch Overhead × (batch − 1))
FE per cycle = per-item FE × batch
FE/t         = FE per cycle / cycle time
```

- FE per item does not change. FE/t rises with batch but less than linearly.
- "Reduced Batch Overhead" multiplies the value, the same way as other reduced stats. The floor is 5.
- Throughput at a batch of `n` is `n / (1 + Batch Overhead × (n − 1))`. It approaches `1 / Batch Overhead`, so Batch Overhead sets the long-run ceiling and Batch Size sets how close a machine gets to it.

| Batch | Overhead 25% | Overhead 15% | Overhead 5% |
|---|---|---|---|
| 1 | 1.00× | 1.00× | 1.00× |
| 2 | 1.60× | 1.74× | 1.90× |
| 3 | 2.00× | 2.31× | 2.73× |
| 4 | 2.29× | 2.76× | 3.48× |
| 6 | 2.67× | 3.43× | 4.80× |
| 9 | 3.00× | 4.09× | 6.43× |

Chassis that are built around bulk work can author a lower base Batch Overhead as part of their identity. Values are settled in the balance pass.

## Lanes

- Lanes come only from chassis identity (`INPUT_SLOTS` on the Lead Furnace). Affixes, refinement, and Mastery never change lane count.
- Each lane runs its own recipe and locks its own batch.
- Gear is shared. One Heat Core drives every Lead Furnace lane, so one core is enough for full throughput.
- The Lead Furnace's three extra Heat Core slots are removed. On load, cores in the removed slots drop at the block, as they would if it were broken.
- Total throughput is lanes × the batch each lane runs.

The Resonance Calibrator's "lanes" all draw from one input slot, so they are a batch size rather than lanes. Lead and Tungstensteel chassis author a base Batch Size of 2 and 3 instead of a lane count. Lane-worded nodes and text (Lane Sync, Wide Tolerance, Shared Field) are reworded to batch terms with the same thresholds.

## Removed: `DENSE_PARALLEL`

- Crusher, Furnace, and Calibrator chassis implicits drop the behavior. Fixed behavior is re-derived from the block, so saved machines need no migration.
- Crusher Throughput prefixes keep their affix ID and grant `+1 / +2 / +3 / +5` Batch Size without enabling anything.
- The behavior constant stays decodable and is ignored, so old stored data does not fail to load.
- Tungstensteel and Exotic Crusher chassis author base Batch Size 4 and 9, as today.

## Batching opt-out

Refiner’s Oath and Fused Crucibles both turn batching off for a payoff. They share one rule, **Single Charge**, applied through the shared batch helper rather than a new behavior:

- The machine's batch is always 1. Batch Overhead never applies.
- The payoff scales with the machine's Batch Size stat, base included, which is how both nodes count today.
- Refiner’s Oath: a flat 20% increased Output Amount that joins the Crusher's yield bucket. It no longer scales with Batch Size.
- Fused Crucibles: 20% more Processing Speed per point of Batch Size. Second Crucible now adds 2 instead of 1 (see below), so the rate drops from 30% to keep today's 60% with Second Crucible alone.
- Tooltips show the Batch Size given up and the resulting payoff.

## Families

Batch Size and Batch Overhead are active on the Crusher, Furnace, Alloy Furnace, Metal Press, Melter, and Resonance Calibrator. They stay inactive on Forestry.

The Alloy Furnace batches whole recipe sets: a batch of `n` needs `n` × every ingredient count.

## Passive tree

### Shared tree

- **Dense Batching** (keystone) becomes: +3 Batch Size. A cycle starts only with a full batch. Its 25% less Processing Speed and 25% more Energy Usage are removed, because Batch Overhead now supplies the time cost.
- A new Batch Overhead path leads to Dense Batching:
    - two small nodes, 8% reduced Batch Overhead each;
    - one notable, **Even Loading**: 15% reduced Batch Overhead and +1 Batch Size.
- New nodes go in a new extra pocket through `author-mega-tree.mjs`, so existing node IDs stay stable.
- All values are placeholders for the balance pass.

### Ascendancies

Today's code counted these nodes as `1 + Parallel Jobs` on top of a base of 1, so real totals were one higher than the node text said. The new values keep today's real totals. Batch Overhead now lowers their real throughput, so their drawbacks are softened in slice 3 rather than their numbers raised.

| Node | Change |
|---|---|
| Drop Hammer (Drop Forge) | +4 Batch Size, a batch of 5 as today. Heat Window drawback goes from −25% to −15% in slice 3. No longer gates batching. |
| Anvil Mass, Forge Line (Drop Forge) | +2 and +4 Batch Size, for batches of 7 and 11 as today. Forge Line keeps disabling circuits. |
| Second Crucible (Twin Crucible) | +2 Batch Size, three melts as today. The 15% more Energy Use is removed in slice 3, since FE already scales per item. No longer gates batching. |
| Triple Crucible (Twin Crucible) | +1 Batch Size, four melts as today. |
| Shared Heat (Twin Crucible) | Unchanged: items after the first in a batch use 20% less FE. |
| Fused Crucibles (Twin Crucible) | Uses Single Charge. |
| Resonance Array (Mass Tuner) | 100% increased Batch Size, so it doubles the chassis lanes as its text always said: 2, 4, and 6 copies on one-, two-, and three-lane chassis. Today's code counted lanes twice and gave 3, 8, and 15. Its stability ceiling drop stays. |
| Refiner’s Oath (Assayer) | Uses Single Charge. |

### Furnace heat nodes

Crucible Heart and Shared Hearth depend on how Furnace heat works, so they move to a separate Furnace heat model PRD. Until that lands:

- Crucible Heart keeps its current rule.
- Shared Hearth does nothing on a Lead Furnace with one shared Heat Core. Its new effect is decided in the heat model PRD.

## UI

Follows [Machine GUI Design](../reference/machine-gui-design.md).

- The stat is shown as **Batch Size** and formatted as `×4`. Batch Overhead is shown as a percentage.
- Hover text names the batch in machine terms, with one stat name underneath:
    - Furnace: charge;
    - Crusher: bed width;
    - Metal Press: die cavities;
    - Melter: crucible load;
    - Resonance Calibrator: tuning bed.
- Progress bars show the locked batch as pips or `×N` while a cycle runs.
- Hover text shows the locked batch, the cycle-time multiplier from Batch Overhead, per-item FE, and total FE/t.
- Jade overlays show the locked batch per lane.
- The Lead Furnace Gear tab shows one Heat Core slot.

## Migration

- `parallel_jobs` decodes as `BATCH_SIZE` in the stat codec. Datapacks and stored modifiers keep working.
- `DENSE_PARALLEL` decodes and is ignored.
- Lead Furnace cores in removed Gear slots drop at the block on load.
- Mastery allocations stay. Node IDs do not change. New nodes use new IDs in a new pocket.

## Slices

1. **Core model.** `BATCH_SIZE` with legacy codec name, `BATCH_OVERHEAD`, and one shared batch helper that locks a batch and returns time and FE scaling. The Crusher, Metal Press, Melter, and Calibrator move onto it. Remove `DENSE_PARALLEL` and the behavior gates. Single Charge. Calibrator lanes become chassis Batch Size.
2. **Furnaces.** One shared Heat Core on the Lead Furnace, batching per lane, Alloy Furnace recipe-set batching, and both families active.
3. **Passive tree.** Dense Batching, the Batch Overhead path, and ascendancy values.
4. **Presentation.** Stat display, hover text, progress pips, Jade, language entries, and the canonical docs: Machine Guidelines, Machine Stats, the six machine content pages, Current Implementation, and the Mastery pages.

## Verification

- `masteryCheck` covers:
    - the batch lock: input-limited, output-limited, fluid-limited, and the full-batch rule;
    - the Batch Overhead time and FE formulas and the 5% floor;
    - Single Charge payoffs;
    - `parallel_jobs` and `DENSE_PARALLEL` decoding.
- `quickCheck` after each slice; `ciCheck`, `npm run moddex:check`, `npm run repo:check`, and `mkdocs build --strict` before handoff.
- In-game checks:
    - a Lead Furnace with one Heat Core runs all four lanes;
    - a Tungstensteel Crusher batches without any flag;
    - Drop Forge, Twin Crucible, and Mass Tuner batches;
    - a world saved before the change loads with its machines, affixes, and allocations intact.

## Open questions

- **Batch Overhead by chassis:** whether the Tungstensteel and Exotic Crusher author a lower base, and how much. With the base 25%, an Exotic Crusher at batch 9 drops from 9× to 3× throughput.
- **Dense Batching:** "only full batches start" is the proposed drawback. The alternative is "Batch Overhead is doubled".
