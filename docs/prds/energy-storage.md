# PRD: Energy Storage Ladder

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the player wiki's [Battery Cells](https://c-pettersson.github.io/rngtech/battery-cells/) and [Battery Chassis](https://c-pettersson.github.io/rngtech/battery-chassis/) pages for current behavior.

PRD status: Accepted

Implementation status: Planned

Release: 2.0

Last updated: 2026-10-31

## Summary

Battery Cells store too little FE for the generators they sit beside. A Copper Battery Chassis with three Copper cells holds 36 kFE. A Copper Solid Fuel Burner fills it in 45 seconds, and the full bank runs one Crusher for 37 seconds. From Stage 1 to Stage 6, a full bank holds between half a minute and two and a half minutes of its own stage's generator. Then the ladder jumps to 10–30 minutes at Stages 7–8.

Most tech mods size their first storage block at several minutes to half an hour of their first generator. RNGTech sits 5–30 times below that through mid-game. A bank feels like a capacitor, solar banks cannot carry a machine through the night, and a pack that raises `energy.generationMultiplier` makes the gap wider, because there is no matching storage setting.

This PRD covers:

- a design target that ties cell capacity to the stage's main generator;
- new base capacities for the standard and Unique cells;
- the Lead cell as long-term storage: far more capacity than its stage, at slow transfer rates;
- a doubled flat capacity affix table for cells;
- a pack-level `energy.storageMultiplier` config value.

Chassis stats, internal machine buffers, and the Exotic cell stay as they are. Lead is the only cell whose transfer rates change.

## References

- [Machine Guidelines](../reference/machine-guidelines.md), Battery Cells and Battery Chassis
- [Affix Generation](../systems/affix-generation.md), natural modifier tiers
- [Machine Stats](../reference/machine-stats.md)

Key existing code:

- `src/main/java/com/rngtech/content/energy/BatteryCellMaterial.java`: base capacity, rates, efficiency, idle loss
- `src/main/java/com/rngtech/content/energy/BatteryChassisMaterial.java`: slots, transfer, burst, Global Modifier Strength
- `src/main/java/com/rngtech/rpg/ModifierEligibilityProfiles.java`: `BATTERY_CELL_ENERGY_CAPACITY_ADD_RANGES`
- `src/main/java/com/rngtech/rpg/ComponentBaseStatCatalog.java`: `batteryCell(...)` profile
- `src/main/java/com/rngtech/content/item/BatteryCellItem.java`: stored FE and capacity clamp
- `src/main/java/com/rngtech/RNGTechConfig.java`: `energy.generationMultiplier` and the per-stage multipliers

## Goals

- A same-stage rate cell holds about 80–120 seconds of its stage's main generator, at every stage from 1 to 7.
- A base-slot chassis filled with same-stage cells holds a little more time at each stage, from about 1.5 minutes at Stage 1 to about 30 minutes at Stage 8, with no cliff between Stages 6 and 7.
- Lead stays the bulk storage cell from Stage 3 through Stage 6. It wins on capacity per slot and loses on transfer, so players choose between a deep bank and a fast one.
- Early storage lands near IC2 and Immersive Engineering, and late storage approaches Mekanism. RNGTech stays the tighter mod. Players still build banks on purpose rather than by accident.
- Packs can scale storage with the same ease as generation.
- Existing saves keep every stored FE value. No migration code.

## Non-goals

- No change to cell input or output rates, except Lead's. Bank throughput stays cell-led and visible, as the guidelines require.
- No change to chassis slots, transfer, burst, Efficiency, Stability, or Global Modifier Strength.
- No change to internal machine buffers. A machine without a cell still shows a small capacity and keeps its no-cell penalty.
- No change to generator FE/t, fuel FE, or recipe FE costs.
- No new storage block or multiblock.

## Problem

### Current ladder

Each stage's best chassis at its base slot count, holding that stage's best cell, against that stage's typical main generator before rolls:

| Stage | Bank | Capacity | Generator | Fill time | One 48 FE/t Crusher |
|---|---|---|---|---|---|
| 1 | Iron × 1 Iron | 10 kFE | Iron Heat Core, 24 FE/t | 21 s | 10 s |
| 2 | Copper × 2 Copper | 24 kFE | Copper Heat Core, 40 FE/t | 30 s | 25 s |
| 3 | Gold × 3 Lead | 96 kFE | Bronze Heat Core, 64 FE/t | 75 s | 100 s |
| 4 | Steel × 4 Invar | 200 kFE | Steel Heat Core, 96 FE/t | 1.7 min | 3.5 min |
| 5 | Sparksteel × 5 Sparksteel | 500 kFE | Syngas Combustor, 240 FE/t | 1.7 min | — |
| 6 | Arclite × 6 Arclite | 2.4 MFE | Fuel Cell, 600 FE/t | 3.3 min | — |
| 7 | Nullite × 8 Nullite | 12.8 MFE | Vacuum Collapse, about 1,050 FE/t | 10 min | — |
| 8 | Exotic × 10 Exotic | 100 MFE | Vacuum Collapse, about 3,000 FE/t | 28 min | — |

Generator figures come from the main-generator table on the wiki's [Modpack Creators](https://c-pettersson.github.io/rngtech/modpack-creators/) page. The Crusher column uses iron ore crushing, 9,600 FE over 200 ticks. Stage 5–8 recipes cost 2–6 times more FE, so the column stops at Stage 4.

The playtest case: a Copper chassis with the Additional Battery Slots prefix and three Copper cells holds 36 kFE, which is 2¼ Coal.

### Other mods

Approximate defaults from community sources. Versions and packs differ, so these numbers show scale, not exact parity.

| Mod | First storage block | Starter generator | Time to fill |
|---|---|---|---|
| IndustrialCraft 2 | BatBox, 40,000 EU | Generator, 10 EU/t | 3.3 min |
| Immersive Engineering | LV Capacitor, 100,000 FE | Measured at 40 FE/t | 2.1 min |
| Mekanism | Basic Energy Cube, 4 MJ (1.6 MFE) | Heat Generator, about 45 FE/t | about 30 min |
| RNGTech today, Stage 2 | Copper chassis, 2 Copper cells, 24 kFE | Copper burner, 40 FE/t | 30 s |

### Where it hurts

- **Solar.** The Solar Panel page tells players to store daytime power for the night in a Battery Chassis. A night lasts about 10 minutes (12,000 ticks). One Crusher through one night needs about 576 kFE. No bank before Stage 5 holds that.
- **Pack scaling.** `energy.generationMultiplier` raises FE so RNGTech can match another energy mod. Storage does not follow, so at 4× a Copper bank fills in about 11 seconds.
- **Machine buffers.** Every machine uses its one cell as its reserve. A Copper cell in a Crusher holds 1¼ iron ore crushes.
- **Feel.** A battery block that empties in seconds reads as a capacitor.

Burners are the least affected. The Fuel Governor pauses a burner when its cell is full, so no fuel is lost. The fuel waits in the hopper instead of in the bank.

## Design target

One same-stage cell holds about 1,600–2,300 ticks (80–115 seconds) of its stage's main generator before rolls. Chassis slots then multiply that, so time per bank rises with stage on its own, because slot counts rise with stage.

Stage 8 stays above the target. The Exotic cell already holds 10 MFE, and the endgame bank should feel large.

Lead is outside the target on purpose. It is the density cell, described under [Lead as long-term storage](#lead-as-long-term-storage).

## Cell capacities

| Cell | Stage | Now | Proposed | Factor | One cell holds |
|---|---|---|---|---|---|
| Potato | 0 | 1 kFE | 1 kFE | 1× | unchanged novelty cell |
| Iron | 1 | 10 kFE | 40 kFE | 4× | 1,667 ticks at 24 FE/t |
| Copper | 2 | 12 kFE | 64 kFE | 5.3× | 1,600 ticks at 40 FE/t |
| Lead | 3 | 32 kFE | 1.5 MFE | 47× | density cell, slow transfer |
| Invar | 4 | 50 kFE | 200 kFE | 4× | 2,083 ticks at 96 FE/t |
| Sparksteel | 5 | 100 kFE | 500 kFE | 5× | 2,083 ticks at 240 FE/t |
| Arclite | 6 | 400 kFE | 1.2 MFE | 3× | 2,000 ticks at 600 FE/t |
| Nullite | 7 | 1.6 MFE | 2.4 MFE | 1.5× | 2,286 ticks at 1,050 FE/t |
| Aethergold | 7 | 1.2 MFE | 1.8 MFE | 1.5× | 1,714 ticks at 1,050 FE/t |
| Exotic | 8 | 10 MFE | 10 MFE | 1× | 3,333 ticks at 3,000 FE/t |

Copper gets the largest step of the rate cells because today it barely beats Iron (12 kFE against 10 kFE). Copper stays the fast early cell, and Lead becomes the slow, deep one.

The Potato cell stays a novelty. Its 12%/min leak makes extra capacity pointless.

### Resulting banks

| Stage | Bank | Capacity | Fill time at the stage's generator |
|---|---|---|---|
| 1 | Iron × 1 Iron | 40 kFE | 1.4 min |
| 2 | Copper × 2 Copper | 128 kFE | 2.7 min |
| 3 | Gold × 3 Copper | 192 kFE | 2.5 min |
| 3 | Gold × 3 Lead | 4.5 MFE | 59 min, at 192 FE/t out |
| 4 | Steel × 4 Invar | 800 kFE | 6.9 min |
| 5 | Sparksteel × 5 Sparksteel | 2.5 MFE | 8.7 min |
| 6 | Arclite × 6 Arclite | 7.2 MFE | 10 min |
| 7 | Nullite × 8 Nullite | 19.2 MFE | 15 min |
| 8 | Exotic × 10 Exotic | 100 MFE | 28 min |

The playtest bank, a Copper chassis with three Copper cells, would hold 192 kFE: 4 minutes of a Copper burner, 12 Coal, and 3.3 minutes of one Crusher.

Stage 3 gets two rows because it is the first stage with a real choice. Copper cells give a small, fast bank. Lead cells give a deep, slow one.

At Stage 3, a Lead bank carries a solar array through a night at up to 192 FE/t, about four Crushers. The cells' output rate is the limit, not the stored FE. A Stage 5 rate bank covers about 200 FE/t. That keeps night power a real build goal without making it free.

## Lead as long-term storage

Lead is the density cell. It holds far more than its stage and moves FE slowly, so it stays the bulk-storage pick long after Stage 3. It also keeps its zero idle loss, so stored FE stays put.

| Stat | Now | Proposed |
|---|---|---|
| Capacity | 32 kFE | 1.5 MFE |
| Input | 256 FE/t | 128 FE/t |
| Output | 128 FE/t | 64 FE/t |
| Idle loss | 0 | 0 |

The rates match the Iron cell's and keep the rule that cells charge twice as fast as they discharge. Filling one empty Lead cell at its full input rate takes about 10 minutes. Draining it at its full output rate takes about 20 minutes.

How long Lead stays relevant, per slot, against each stage's best rate cell:

| Stage | Rate cell | Lead capacity | Lead transfer |
|---|---|---|---|
| 3 | Copper, 64 kFE, 128 FE/t out | 23× | 0.5× |
| 4 | Invar, 200 kFE, 256 FE/t out | 7.5× | 0.25× |
| 5 | Sparksteel, 500 kFE, 512 FE/t out | 3× | 0.125× |
| 6 | Arclite, 1.2 MFE, 1,024 FE/t out | 1.25× | 0.0625× |
| 7 | Nullite, 2.4 MFE, 2,048 FE/t out | 0.63× | 0.03× |

Lead wins on capacity per slot through Stage 6 and loses on both counts from Stage 7. After that it is only a cheap filler.

### Sizing Lead against a factory

A Lead bank has two limits: its output rate (64 FE/t per cell) and its stored FE (1.5 MFE per cell). A cell running at full output empties in 1,500,000 ÷ 64 = 23,438 ticks, about 19.5 minutes. That is 98% of one Minecraft day (24,000 ticks).

So a bank with enough Lead cells to meet a factory's draw also holds that factory for about one day. For any outage shorter than a day, including a night, the output rate sets the cell count, not the capacity. Capacity only sets the count for outages longer than a day.

Lead cells per running machine, from the typical machine draw on the wiki's Modpack Creators page:

| Stage | Typical machine draw | Midpoint | Lead cells per machine |
|---|---|---|---|
| 1 | 24–48 FE/t | 36 FE/t | 0.6 |
| 2 | 24–64 FE/t | 44 FE/t | 0.7 |
| 3 | 48–96 FE/t | 72 FE/t | 1.1 |
| 4 | about 96 FE/t | 96 FE/t | 1.5 |
| 5 | 128–384 FE/t | 256 FE/t | 4 |
| 6 | 160–512 FE/t | 336 FE/t | 5.3 |
| 7 | 512–3,072 FE/t | 1,792 FE/t | 28 |
| 8 | 2,048–8,192 FE/t | 5,120 FE/t | 80 |

A reference factory, with an assumed machine count that grows with stage. All machines run at once, which is the worst case:

| Stage | Machines | Draw | Main generators | Lead cells | Base-slot chassis |
|---|---|---|---|---|---|
| 1 | 3 | 108 FE/t | 4.5 | 2 | 2 Iron |
| 2 | 4 | 176 FE/t | 4.4 | 3 | 2 Copper |
| 3 | 6 | 432 FE/t | 6.8 | 7 | 3 Gold |
| 4 | 8 | 768 FE/t | 8 | 12 | 3 Steel |
| 5 | 10 | 2,560 FE/t | 10.7 | 40 | 8 Sparksteel |
| 6 | 12 | 4,032 FE/t | 6.7 | 63 | 11 Arclite |
| 7 | 14 | 25,088 FE/t | 24 | 392 | 49 Nullite |
| 8 | 16 | 81,920 FE/t | 27 | 1,280 | 128 Exotic |

Each of those banks carries its whole factory for about one day.

What this shows:

- **Stages 1–4:** Lead can carry a whole factory. At most 12 cells, in three chassis, is a reasonable build.
- **Stages 5–6:** carrying the whole factory takes 40–63 cells, which is too many to be practical. Lead becomes the reserve for a base load: a few always-on machines, or the gap between the generators and peak draw. The fast cells cover the peak.
- **Stages 7–8:** Lead cannot keep up. This matches its exit from the ladder.
- **Rolls:** Energy Transfer affixes on the chassis raise each cell's rate, and Gold's burst covers short peaks. A well-rolled chassis cuts these counts by about a third.

What this means for the numbers:

- **The output rate is what really matters.** Doubling Lead's capacity does not change any cell count above for outages up to a day. Halving its output to 32 FE/t doubles every count, to 24 cells at Stage 4 and 80 at Stage 5. Lead would then carry a whole factory only through Stage 3.
- **Capacity sets the endurance.** At 768 kFE a rate-sized bank lasts one night. At 1.5 MFE it lasts one day. At 3 MFE it lasts two days.
- **1.5 MFE is the one-day anchor.** A Lead bank sized for its rate lasts about a full day. A day is long enough to count as long-term storage. It is also short enough that Lead does not beat Nullite's capacity at Stage 7. The round 1.5 MFE is kept over the exact one-day value of 64 × 24,000 = 1,536,000 FE.

The cost is visible and the player chooses it:

- **Mixed banks.** A Sparksteel chassis with three Sparksteel cells and two Lead cells holds 4.5 MFE. While the Sparksteel cells hold charge, the bank delivers up to 1,664 FE/t. Once they are empty, only the Lead cells' 128 FE/t remains. The chassis already shows cell rate limits, so the drop is visible. Charge Balancer and Balance Mode spread the load but do not raise Lead's rate.
- **Lead in machines.** A machine with a Lead cell gets a deep reserve that feeds only 64 FE/t. A machine that draws more shows **Cell Rate Limited** once its internal buffer is empty. That is the trade, not a fault.
- **Gold chassis.** Gold's 2.5× Burst Transfer lifts a Lead bank to 480 FE/t for short peaks. Gold's 0.80 Stability wastes part of that burst FE, so Lead pairs well with Gold but pays for it.
- **Charged Storage.** A Lead bank rarely runs empty, so it keeps the Charged Storage doubling more easily than a busy rate bank. A Gold chassis with this prefix and three Lead cells holds 9 MFE at Stage 3. Its output stays at 192 FE/t, so the extra depth adds endurance, not power.

Lead must not become a power source. A bank stores only what generators made, and its slow output keeps a coal-era Lead bank from running a late-stage load.

### Unique cells

Each Unique keeps its place relative to the standard cell it is described against.

| Unique | Stage | Now | Proposed | Anchor |
|---|---|---|---|---|
| Unique Potato Battery Cell | 0 | 10 kFE | 40 kFE | matches the Iron cell |
| Shipwreck Salt-Zinc Anode | 1 | 24 kFE | 96 kFE | 2.4× an Iron cell, slow, leaky |
| Bastion Coin Stack Capacitor | 4 | 40 kFE | 160 kFE | stays 80% of an Invar cell |

Their rates, leaks, and Unique lines do not change. Rolled Unique values keep the same range shape around the new base. The Salt-Zinc Anode is described against the Iron cell only, so the Lead change does not touch its text.

## Flat capacity affix

Cells roll a flat `energy_capacity_flat` prefix. Its natural tier at Stage 1–2 adds 4–12 kFE, which is 33–100% of a Copper cell today. At the new bases the same roll would add 6–19%.

Double the cell table so the early flat affix stays a strong roll without dominating the cell:

| Tier | Now | Proposed | Share of its natural-stage cell |
|---|---|---|---|
| 1 | 1,000–3,000 | 2,000–6,000 | Iron: 5–15% |
| 2 | 4,000–12,000 | 8,000–24,000 | Copper: 12–38% |
| 3 | 16,000–50,000 | 32,000–100,000 | Invar: 16–50%; Arclite: 3–8%; Lead: 2–7% |
| 4 | 75,000–100,000 | 150,000–200,000 | Nullite: 6–8% |
| 5 | 150,000–250,000 | 300,000–500,000 | overflow roll |
| 6 | 350,000–600,000 | 700,000–1,200,000 | overflow roll |
| 7 | 800,000–1,000,000 | 1,600,000–2,000,000 | Exotic: 16–20% |

A flat affix fades on higher-stage cells by nature. The percent capacity prefix covers those. The machine-side flat table (`MACHINE_ENERGY_CAPACITY_ADD_RANGES`) does not change.

Existing cells keep their rolled values. Only new rolls, rerolls, and Affix Forge operations use the new table.

## Pack config

Add `energy.storageMultiplier` next to `energy.generationMultiplier`:

- range 0.01–100, default 1.0;
- applies to a Battery Cell's final capacity, after base, flat, and percent affixes, in every host: machines, burners, Battery Chassis, solar controllers, and tools;
- does not change transfer rates, idle loss percentages, or internal machine buffers;
- the [Modpack Creators](https://c-pettersson.github.io/rngtech/modpack-creators/) page tells packs to raise it together with the generation multiplier.

Lowering it clamps stored FE the next time a cell is read, so the config comment says that FE above the new capacity is lost.

## What stays the same

- Cell input and output rates, except Lead's, plus efficiency and idle loss percentages.
- Chassis material stats, including Chassis Capacity, Energy Transfer, Charged Storage, Burst, and Balance Mode.
- Internal machine buffers and the no-cell penalties.
- Generator output, fuel FE, and recipe FE costs.
- The Exotic and Potato cells.
- Cell recipes and assembly costs. Cells already cost little next to the FE they store, and this PRD does not try to fix that.

## Side effects to check

- **Machine reserves.** Every machine's buffer is its internal buffer plus the cell, so all machines with a cell hold 3–5 times more FE. Check that work-state lights, Jade readouts, and the Glow affix thresholds still read well.
- **Solid Fuel Burner.** Quick Feed eases the stoke above half of the burner's own cell. A larger cell keeps a burner at full stoke longer before it eases. The Fuel Governor pause point moves with the cell. Confirm both still feel right with a Copper cell.
- **Modular tools.** Tools carry a cell for their FE features. A larger cell means more blocks per charge. Check the Hammer and Digger area modes, the most FE-hungry tool features.
- **Burst consumers.** A larger bank lets slow generation feed a short, high-draw job for longer. Bank output is still capped by cell output rates, so a coal-era bank cannot run a late-stage FE/t load. Check the Phase Array's supply rule and the Resonance Calibrator against a large low-stage bank.
- **Cable balancing.** `EnergyAllocator` balances storage by fill ratio, so larger capacities need no change. Confirm in the balance test world.
- **Integer limits.** Cell capacity is an `int`, saturated in the chassis. The largest new value is the Nullite cell at 2.4 MFE before affixes, Charged Storage, Global Modifier Strength, and the storage multiplier. Check that a fully rolled Exotic cell at the 100× config ceiling saturates cleanly instead of wrapping.
- **Existing Lead cells.** Lead cells already in machines and banks halve their transfer rates. A Furnace or Crusher that leaned on a Lead cell may now show Cell Rate Limited. The changelog should say so.
- **Existing saves.** Cells store FE as an absolute amount and clamp to capacity on read. Raising capacity leaves existing cells partly full and loses nothing.
- **GameTests and checks.** Tests that read `BatteryCellMaterial.capacity()` follow the change. Tests with hardcoded FE values need new numbers.

## Alternatives considered

- **More chassis slots.** Slots are budgeted against Global Modifier Strength, and a bigger slot count does not help a machine's one cell. Rejected.
- **Config only.** Leaves the default ladder too small and moves the fix to every pack. Kept as an addition, not the fix.
- **Uniform 4× on every cell.** Simpler, but it widens the Stage 7–8 jump, keeps Copper close to Iron, and pushes Exotic to 40 MFE per cell. Rejected for the stage-tuned table.
- **A dedicated large storage block.** New content with its own UI, art, and recipes. Out of scope for a balance pass.

## Rollout

One slice:

1. Update `BatteryCellMaterial` capacities, Lead's rates, and the three Unique bases.
2. Double `BATTERY_CELL_ENERGY_CAPACITY_ADD_RANGES`.
3. Add `energy.storageMultiplier` and apply it where cell capacity is computed.
4. Update the wiki: the Battery Cells table and Unique lines, the Battery Cells line that says each tier stores more and moves FE faster (Lead now breaks that rule), a short note on Lead as long-term storage, the Battery Chassis examples, the affix example on Battery Cells (today it uses a 12,000 FE Copper cell), and the Modpack Creators config list. Regenerate the GuideME guide.
5. Run `quickCheck`, `runGameTestServer`, `npm run moddex:check`, both strict MkDocs builds, and the guide check.
6. Playtest in the balance test world: a Copper bank on a Copper burner, a Gold Lead bank carrying a Stage 3 solar array through one night, a mixed Sparksteel and Lead bank running dry, and one Crusher on a Copper cell.

## Decisions

Recorded 2026-10-31:

- Lead is the long-term storage cell: 1.5 MFE, 128 FE/t in, 64 FE/t out, no idle loss. A bank sized for its rate lasts about one day. The round value is kept over the exact 1,536,000 FE. Lead does not go slower than 64 FE/t out, because 32 FE/t would double every factory cell count.
- The Shipwreck Salt-Zinc Anode is described against the Iron cell only. Its tooltip and wiki text no longer mention Lead.
- About 100 seconds of the stage's main generator per same-stage rate cell is the target feel.
- `energy.storageMultiplier` scales Battery Cells only. If packs need to scale internal machine buffers, that gets its own multiplier later.
- No material's stats are set as a share of another material's. Each cell is sized from its own stage target and role. Aethergold's 1.8 MFE stands on its own: 1,714 ticks of its stage's generator, inside the target band.
- The reference factory's machine counts are accepted as the sizing basis. They are hard to verify, so playtests may revise the Stage 5–6 conclusion.

## Open questions

None.