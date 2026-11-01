# PRD: Jewels and the Bonus Output Cap

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the player wiki's [Machine Mastery](https://c-pettersson.github.io/rngtech/machine-mastery/) and [Rarity and Affixes](https://c-pettersson.github.io/rngtech/rarity-and-affixes/) pages for current behavior.

PRD status: Accepted

Implementation status: In progress on `feature/jewels-bonus-output-7b4ab9`. The Bonus Output cap (phase 2) is implemented; the jewel phases follow on the same branch.

Release: 2.0

Last updated: 2026-11-01

## Summary

Jewels are small rolled items, modeled on Path of Exile's jewels, that a player sockets into a machine. A socket is either a **Jewel Socket node** on the shared Mastery tree, allocated with a point like any node, or a **body socket** on the machine itself, which comes from a rolled suffix or from a Gold body that has one built in. A jewel carries up to four affixes drawn from one machine-agnostic pool, each with a single tier, so a jewel is read the same way in a Crusher, a Furnace, a Melter, or a generator: every line applies where the machine reads the stat, and the tooltip dims the rest, exactly as tree nodes already behave.

Jewels fill two roles at once. A Magic jewel with one useful line is a gap filler: a Furnace build that lacks Energy Usage, a Crusher short of Reserve for its keystone. A Rare jewel with four wanted lines at high rolls is a chase item, the thing a player keeps cutting jewels for after the machine's own affixes are settled.

The same change reworks **Bonus Output**. Today the Crusher's Output Amount bucket has diminishing returns that never reach +100%. It becomes a hard cap at **75%**, like Path of Exile's maximum resistances, and a new stat, **Maximum Bonus Output**, raises the cap. The tree offers +5% across four nodes and jewels roll +1-2%, so pushing past 75% is a build decision rather than a side effect of stacking more yield.

## References

- [Machine Mastery](../systems/machine-mastery.md): the shared tree, attributes, tagged payoffs, and chassis ownership.
- [Affix Generation](../systems/affix-generation.md): rarity, affix counts, tiers, groups, and weights.
- [Passive Tree Design Rules](../reference/passive-tree-design-rules.md): node kinds, degree caps, gates, and the `EXTRAS` authoring path.
- [Crusher Yield](../reference/machine-stats.md#crusher-yield): the soft cap this PRD replaces.
- [Corruption](corruption.md) and [Unique Items](uniques.md): the pool and catalog patterns jewels reuse.
- [Shared Affixes](../modifiers/shared-affixes.md): the rules every new affix follows.

Key existing code:

- `src/main/java/com/rngtech/rpg/MachineStatAccumulator.java`: `CRUSHER_YIELD_SOFT_CAP`, `withCrusherYieldRules`, `softCapped`, `effectiveIncreasedPercent`, the `CEILING` merge.
- `src/main/java/com/rngtech/rpg/StatBreakdown.java` and `StatBreakdownText.java`: the `SOFT_CAP` term and its "Diminishing returns" line.
- `src/main/java/com/rngtech/content/blockentity/CrusherBlockEntity.java`: `outputAmountFor`, At-Level Output, the no-cell penalty.
- `src/main/java/com/rngtech/content/blockentity/CavitationGeneratorBlockEntity.java` and `CavitationHeat.java`: the other `OUTPUT_AMOUNT` reader.
- `src/main/java/com/rngtech/rpg/progression/MegaPassiveTree.java`, `MegaPassiveNode.java`, `MachineProgressionState.java`, `MasteryEffectSource.java`: node kinds, effect sources, and the per-machine state that a tree-socketed jewel joins.
- `src/main/java/com/rngtech/rpg/ModifierEligibilityProfiles.java`, `ModifierDefinition.java`, and `MachineTraitRoller.java` (`RollTuning`, `MAGIC_SLOT_LIMIT`, `RARE_SLOT_LIMIT`, `rareAffixCount`): profiles, families, tier tables, slot limits, and the `additional_battery_slots` family that the socket suffix is shaped after.
- `src/main/java/com/rngtech/content/energy/BatteryChassisMaterial.java` (`GOLD`), `CrusherChassisMaterial.java`, `FurnaceChassisMaterial.java`: the body ladders that gain a Gold member.
- `src/main/java/com/rngtech/rpg/progression/CrusherPassiveNode.java` and `data/rngtech/mastery/`: the yield nodes and `declarations.json`.
- `tools/moddex/author-mega-tree.mjs` (`EXTRAS`) and `layout-mega-tree.mjs`: where new tree pockets are authored.

## Goals

- One jewel item family, three colours, with a machine-agnostic affix pool that reads on every machine family. Each jewel affix has one tier.
- Jewel Socket nodes on the shared tree, allocated with points and filled per machine, stored with the machine's Mastery so they survive drops and Soul Transfer.
- Body sockets from a rolled suffix (1 to 3 sockets, the third only at Tier 7) and from Gold bodies with one built in.
- Jewels are refinement targets like any rolled item, minus tier operations.
- Jewels come from the Resonance Calibrator, Work Orders, and, behind a config key, vanilla chests.
- Bonus Output caps at 75% and Maximum Bonus Output raises the cap, from four tree nodes and a jewel suffix.
- Every number in this PRD is visible: the cap, the over-cap amount, the socket count, and which jewel lines a machine reads.

## Non-Goals

- No radius or threshold jewels that change other tree nodes, and no Unique jewels in this slice. Both fit the Unique catalog later; see [Deferred](#deferred).
- No jewel sockets on Gear, Battery Cells, tool parts, Job Chips, or companions' bodies. A Forestry Companion uses tree sockets only.
- No automation surface for sockets. Jewels are player-managed, like a Battery Chassis's Gear.
- No new tier tables for jewel affixes: one tier each, by design.
- No change to Super Output, Crusher Salvage, Single Pass, the no-Battery-Cell penalty, or under-level suppression.
- No shared storage, no autocrafting, and no roll-until-X helper for jewels.

## Vocabulary

| Term | Meaning |
|---|---|
| Jewel | `rngtech:control_jewel`, `rngtech:drive_jewel`, `rngtech:reserve_jewel`. A rolled item with up to two prefixes and two suffixes from the jewel pool. |
| Jewel colour | Which of the three attributes the jewel is cut for. Colour weights the pool toward that attribute's themes; it never excludes a line. |
| Jewel Socket | A node kind on the shared tree, `JEWEL_SOCKET`. Allocating it costs one point and opens one socket on that machine. |
| Body socket | A socket on the machine itself, from the Bedazzling suffix or a Gold body's implicit. |
| Bedazzling | The suffix family `jewel_sockets`, "of Bedazzling", that grants body sockets. |
| Gold body | A Stage 3 machine body whose identity implicit is one body socket. |
| Maximum Bonus Output | New stat `MAX_OUTPUT_AMOUNT`. The highest increased Bonus Output a machine pays, 75% before modifiers. |
| Over cap | Increased Bonus Output above the maximum. It is shown, and it pays nothing. |

## Player Flow

1. At Stage 3 the player presses Gold Plates and cuts a **Jewel Blank** in the Resonance Calibrator. The jewel comes out Unidentified; identifying it rolls rarity, affixes, and Refinement Potential like any part.
2. On the Mastery screen they path to a Jewel Socket node, allocate it, and drop the jewel in. The node shows the jewel; its tooltip shows which lines this machine reads.
3. A Gold Crusher body has one empty socket in its Gear tab from the start. A later Rare roll, or an Affix Injector, adds "of Bedazzling" for one more.
4. The Stats tab's Bonus Output line reads "75% (86% rolled, 11% over cap)". The player takes the Headroom pocket on the tree and a jewel with "+2% Maximum Bonus Output", and the line reads 79%.
5. The jewel that fills the gap is refined at the Affix Forge: an Injector fills its last slot, a Chaos Crystal rerolls a bad one, a Volatile Catalyst gambles a finished one.

## Jewels

### Items and rolls

Three items, one per attribute colour: Control (teal), Drive (gold), Reserve (red), matching the start emblems' colours. A jewel is a rolled item with the normal trait data (`rngtech:machine_traits`), so identification, naming, tooltips, Jade, JEI, and every refinement station already know how to handle it.

| Rule | Jewel | Why |
|---|---|---|
| Affix slots | Up to 2 prefixes and 2 suffixes | Path of Exile's jewel limit. Four lines keep a Rare jewel readable and make the perfect four a chase. |
| Rarity | Magic rolls 1 or 2 affixes; Rare rolls 3 or 4 at `50/50` | Rare must be common enough that jewels matter, but the fourth line stays a roll. |
| Stage | 3, the Calibrator cut's stage | Jewels enter when the Gold body does. Stage 1-4 rarity odds (35% Magic, 65% Rare) mean most cut jewels show three lines. |
| Tiers | One per affix family | The chase is which lines and how high within the one range, never a tier climb. |
| Refinement Potential | `2-4`, from the cut's calibration stability | Enough to fill a Magic jewel or finish a three-line Rare; never enough to rebuild one. |
| Mod groups | One group per family, checked within the jewel | A jewel cannot hold two Processing Speed lines. Groups are per item, so a machine may read Processing Speed from its body, its Gear, and several jewels. |

The roller's `MAGIC_SLOT_LIMIT` and `RARE_SLOT_LIMIT` are constants today, and the Rare count table is fixed at `35/35/20/10`. `RollTuning`, which already gives tool parts their own Refinement Potential and overflow bonuses per profile, grows a per-side slot limit and a Rare count table. Every existing profile keeps 3/3 and `35/35/20/10`.

### The jewel pool

One pool, machine-agnostic, every family read by at least one machine family. Values sit around the lower half of the machine Tier 3 table, so one jewel line is worth about one and a half ordinary tree nodes and a four-line Rare about the same as a Magic Gear part. Weights are per colour: a colour's **home** lines roll at `150`, **away** lines at `60`, and chase lines at the weight shown on every colour. The three profiles list the same twenty definitions; `JewelColour` holds the home groups and reweights the pool at roll time, so ModDex shows the family weights, not the colour's.

Prefixes:

| Family | Group | Roll | Home colour | Read by |
|---|---|---|---|---|
| Capacitive | `energy_capacity` | 10-20% increased Energy Capacity | Reserve | Every machine with an FE buffer |
| Frugal | `energy_usage` | 4-8% reduced Energy Usage | Control | Every FE-using machine |
| Steady | `stability` | 5-10% increased Stability | Control | Processing machines, generators |
| Deep | `fluid_capacity` | 10-20% increased Fluid Capacity | Reserve | Fluid machines and tanks |
| Insulated | `heat_isolation` | 6-12% increased Heat Isolation | Control | Heated machines |
| Conductive | `heat_transfer` | 6-12% increased Heat Transfer | Drive | Heated machines, Cavitation Generator |
| Thrifty | `fuel_efficiency` | 4-8% increased Fuel Efficiency | Reserve | Fuel burners, Stage 0 Furnace |
| Yielding | `output_amount` | 4-8% increased Bonus Output | Drive | Crusher, Cavitation Generator |
| Fortunate | `super_output` | +1-2% Super Output | Drive | Item processors |

Suffixes:

| Family | Group | Roll | Home colour | Read by |
|---|---|---|---|---|
| of Pace | `processing_speed` | 6-10% increased Processing Speed | Drive | Processing machines |
| of Output | `energy_generation` | 6-10% increased Energy Generation | Reserve | Generators |
| of Control / of Drive / of Reserve | `control`, `drive`, `reserve` | +6-10 of the attribute | Its own colour | Mastery machines |
| of Study | `mastery_xp` | 10-20% increased Mastery XP gain | Control | Mastery machines |
| of Flashes | `instant_process` | +1-2% Instant Process chance | Drive | Processing machines with Instant Process |
| of Headroom | `max_output_amount` | +1-2% Maximum Bonus Output | Chase, weight `25` on every colour | Crusher, Cavitation Generator |
| of Momentum | `drive_threshold` | 12% increased Processing Speed with at least 100 Drive | Chase, weight `20`, Drive only | Mastery processing machines |
| of Poise | `control_threshold` | 8% reduced Energy Usage with at least 100 Control | Chase, weight `20`, Control only | Mastery FE-using machines |
| of Depth | `reserve_threshold` | 25% increased Energy Capacity with at least 100 Reserve | Chase, weight `20`, Reserve only | Mastery machines with an FE buffer |

Rules for the pool, checked by a domain test:

- Every family has exactly one tier range.
- Every family's stat is read by at least one Mastery family or generator, using the same reader table as the Unique catalog, so no jewel line is dead everywhere.
- Jewel lines are machine-wide. A jewel never has a local scaling layer, so a jewel in a Furnace's tree socket and the same jewel in a Furnace's body socket give the same numbers.
- Threshold lines read the machine's final attribute totals after Gear, every jewel's flat attribute lines, and attribute modifiers, in one pass. A threshold line never counts toward its own threshold.
- No jewel line feeds the refinement economy or drops loot, per the shared affix rules.

Nineteen groups is wide enough that two Rare jewels rarely match, and narrow enough that each colour's home set reads as a theme: Control jewels lean to efficiency and stability, Drive to speed and yield, Reserve to buffers and generation.

### Jewel Socket nodes on the tree

A new node kind, `JEWEL_SOCKET`, joins the five existing kinds. It has no stat effects of its own. When allocated, the machine may hold one jewel in it, and that jewel's lines join the machine's effect sources beside the tree's nodes.

| Rule | Value |
|---|---|
| Count | 9 sockets |
| Home sockets | One per start, 4 to 8 points out, at the end of a two-node arm: one ordinary reward node in the region's theme, then the socket |
| Outer sockets | Three, attached directly to a travel node on an outer star hub or the perimeter road, like a keystone, each in a different third of the tree |
| Degree cap | Exactly 1, a leaf |
| Distance | At least 4 allocations from every start |
| Gate separation | At least two travel edges from any other reward gate, as today |
| Cost | Home sockets 2 points from the road; outer sockets 1 point plus the travel to reach them |

The sockets are added through `EXTRAS` in `author-mega-tree.mjs`, so every existing node keeps its ID, position, and links and the catalog version does not change. The kind tables are hard-coded to five kinds in `check-mega-tree.mjs` (kind sets, diameters, `CATALOG_NODES = 1368`), `MasteryChecks` (the 1368 assertion), `MegaPassiveNode.size()`, and the Mastery screen's `nodeFill` and `drawNode`; each gains `JEWEL_SOCKET`, drawn as an empty ring at the NOTABLE diameter, and the node count moves to the new total (about 20 new nodes with the Headroom extras below). `masteryCheck` gains the socket rules above and counts a socket as live, and `moddex:check` accepts the kind with its degree cap.

Behaviour rules:

- Socketing and removing a jewel are server actions from the Mastery screen, on an allocated socket only. Removing returns the jewel to the player.
- Refunding a socket, a Clear, or a Full Reset leaves the jewel in place and inactive, with its node dimmed, so a respec never drops items. Target builds and build codes carry the socket node but never the jewel.
- A socket's jewel is one more `MasteryEffectSource`. Its effects apply with the tree's nodes in `applyNodeStats`, before Gear and `applyAttributeStats`, so jewel attributes feed the conversions and thresholds like Gear attributes do. Each line applies where `MachineMasteryFamily.supports` says the family reads the stat, the same gate every node passes. A stat breakdown labels the line "Jewel: <jewel name>".
- The Mastery tab syncs through `ContainerData` ints, which cannot carry an item. The socket contents reach the client through menu slots over the socket inventory, one per socket, so the screen draws the real stacks and the existing click handling moves them.
- Single Pass still removes every yield line a jewel carries, because yield is declared by stat, not by source.
- Foreign-start rules are unchanged: a socket is reached through the machine's own start like any node.

### Body sockets

**Bedazzling** is a suffix family, `jewel_sockets`, shaped after `additional_battery_slots`:

| Tier | Body sockets | Where it can appear |
|---:|---:|---|
| 1-3 | +1 | Any stage, by the normal tier weights |
| 4-6 | +2 | Natural at Stage 7-8, a lucky overflow before |
| 7 | +3 | Stage 8 Exotic bodies only, from a roll, an Imprint, or an Exalt from Tier 6 |

Roll weight `40`. It rolls on every placed machine body profile through a new capability, `HAS_JEWEL_SOCKETS`: processing machines, generators, the Battery Chassis, the Fluid and Compressor Tanks, and the Dispatch Terminal if it ever rolls traits. It never rolls on Gear, cells, chips, tool parts, or companions, so a machine can only have one Bedazzling line and sockets never nest.

A **Gold body** carries one body socket as a fixed identity implicit, the way Steel Crushers carry Output Guard. Gold is a Stage 3 material that today feeds refinement, connectors, Solar Panels, and one body: the Gold Battery Chassis (`BatteryChassisMaterial.GOLD`, Stage 3). That makes it the natural "socket base", as the Stygian Vise is in Path of Exile. The first slice gives the existing Gold Battery Chassis the implicit, and adds a Gold Crusher Chassis and a Gold Furnace Chassis: the two Stage 0-3 ladders every player climbs, and the two Mastery families with the most yield and heat lines to fill. A new Gold body's other stats sit at its stage's Copper body: the socket is the upgrade, not the numbers. Art follows the stage and the recipe: a Copper-era body with gold trim and a visible empty setting on its front.

Shared rules:

- Sockets stack: implicit plus suffix, capped at **4** per machine. A Gold body with Tier 7 Bedazzling has four.
- Every socketed jewel lives in one `rngtech:jewels` component on the block entity: body sockets as a list of up to four stacks, tree sockets as a map from socket node ID to stack. It is kept on the dropped item like traits and Job Chips, and Soul Transfer copies the tree-keyed entries with the Mastery.
- They show as a socket row in the Gear tab, or in the Stats tab on a machine without Gear, as empty settings that fill with the jewel's icon. Hover shows the jewel's tooltip with this machine's inactive lines dimmed.
- A body-socket jewel is the same effect source as a tree-socket jewel; only the storage differs.
- A refinement operation that would remove occupied sockets (a Chaos Crystal, Nullifier Coil, Excise, Transmute, or Reforge on the Bedazzling line) is rejected until the jewels are taken out. The station names the reason, as a Gear conflict does on the Mastery screen. No operation ever deletes or drops a jewel.
- Soul Transfer moves tree-socket jewels with the Mastery; body-socket jewels stay with the body, because the sockets do.

### Refinement and corruption

A jewel is a refinement target on every station. The tier operations are illegal on it and the station says so: Upgrade catalysts, Resonate, and Exalt have nothing to climb. Everything else works as on a part: Injector, Chaos Crystal, Nullifier Coil, lenses and crystals, Attune, Imprint, Transmute, Graft, and the Mirror Catalyst, which can copy a perfect jewel under its existing rules.

The Volatile Catalyst corrupts a jewel with the same five outcomes. Draft jewel pools, following the [corruption pool rules](corruption.md#corruption-implicits) (one fixed value each, no yield, no Refinement Potential):

| Blessed | Blighted |
|---|---|
| +5 Control, +5 Drive, or +5 Reserve | 10% increased Energy Usage |
| 10% increased Mastery XP gain | 5% less Processing Speed |
| One prefix or suffix slot more than the 2/2 limit, empty | 10% less Stability |

Maximum Bonus Output stays out of the corruption pools, because it unlocks yield.

### Sources

- **Cut at the Resonance Calibrator.** A Jewel Blank is a crafting-grid item: four Gold Nuggets around an Amethyst Shard. A Stage 3 Structural calibration cuts it into a jewel, and the catalyst picks the colour: Prismarine Crystals for Control, a Gold Ingot for Drive, a Block of Redstone for Reserve. The cut's stability (30-90 before Gear) sets the jewel's Refinement Potential (2 below 50, 3 below 80, 4 from 80), so a well-tuned Calibrator cuts better jewels. The jewel comes out Unidentified, like a crafted part. This is the mod-challenge gate, as the Volatile Catalyst and the Seals are gated.
- **Work Orders.** Tier III and higher jobs add a jewel kind to `rngtech:challenges/work_order`, roughly one jewel per fifteen listed jobs, Tier V jobs guaranteeing Rare through a loot function. `check-work-orders.mjs` holds the rate.
- **Vanilla chests, configurable.** An unidentified jewel of a random colour sits in the same structure tables the Uniques use, through `neoforge:add_table` loot modifiers, at a draft 4% per common chest and 8% per uncommon chest, behind the common config keys `jewels.loot.enabled` and `jewels.loot.<colour>.enabled`. The Calibrator stays the reliable source; chests are the early surprise.

## Bonus Output

### The cap

Bonus Output is the `OUTPUT_AMOUNT` increased bucket: Crush Head base yield (Bronze 10, Steel 5, Titanium 15, Tungstensteel 30, Exotic 25), yield affixes, tree and ascendancy nodes, and At-Level Output. Today it pays `B * 100 / (B + 100)` on the Crusher and the raw bucket on the Cavitation Generator. The tree alone holds 62 points of it: thirty Recovery smalls at 0.5%, eight notables at 1.5%, and three keystones at 10, 10, and 15. Both readers change to:

```text
paid increased percent = min(B, Maximum Bonus Output)
Output Amount = authored base * (1 + paid / 100) * more and less factors
```

- `MAX_OUTPUT_AMOUNT` is a new stat, 75 before modifiers, `ADD` only. The constant replaces `CRUSHER_YIELD_SOFT_CAP`, and `withCrusherYieldRules` becomes an output-cap rule applied to every machine that reads `OUTPUT_AMOUNT`. `declarations.json` lists the Crusher as its family, and `CrusherBlockEntity` reads it directly so the `claimedStatsAreRead` audit sees a real read.
- Maximum Bonus Output has its own ceiling of **90%**. It is a `CEILING` term and shows in the breakdown like Super Output's 25% does. The old curve paid 75% at a 300-point bucket and 90% at 900; the ceiling keeps the top end where the game already is, while sockets keep the number of +1-2% sources open-ended.
- Reductions subtract before the cap. Less multipliers and the no-Battery-Cell penalty apply after it, as today.
- Single Pass's ceiling of `1.0` on Output Amount still wins: a `CEILING` merges by minimum.
- Under-level recipes and `bonus_output: false` recipes are unchanged.
- The Cavitation Generator reads the same capped bucket for its Water saving and Nitrogen yield; its rotor and nozzle `more` multipliers sit outside the cap. Its generator, rotor, and nozzle rolled Output Amount from the general percent table, whose Tier 5 and above exceed 75% on one line. The shared `OUTPUT_AMOUNT` definition moves to the Jaws yield table (2-25%) without the speed penalty, so three top rolls meet the cap exactly and no single roll is born over it.
- Work Order output goals (`crusher_goal_output.json`) reach `1.74`, inside the 75% cap. `WorkOrderRuleChecks` gains a check that no Output Amount goal exceeds `1 + ceiling / 100`.

Effect on existing builds: a bucket below 75 pays more than before (a 100-point bucket paid 50%, now 75%), a bucket above 300 pays the same or a little less, and nothing in a save changes shape. A Stage 7 Crusher with a Tungstensteel head, Tier 5 Jaws and Pulverizing, and Refiner's Oath holds an 86-point bucket, so it reaches the cap from its own affixes, and the cap becomes the thing to raise. That is the intended shape: the cap is easy to hit and hard to move.

The Crusher yield affix tables stay as they are; they were shrunk for the soft cap and they now fill a 75-point budget instead. Playtest confirms whether Jaws and Pulverizing still earn their speed penalty when a plain Output Amount suffix and the head already reach the cap.

### Sources of Maximum Bonus Output

| Source | Amount | Notes |
|---|---:|---|
| Headroom pocket (tree, Drive region's outer field, among the Crusher's Recovery pockets) | +5% | A diamond off one road gate: three +1% smalls, then the notable **Overflow Gate**: +2% Maximum Bonus Output and 1.5% increased Bonus Output, the Recovery notable's scale. Four reward points plus travel. |
| of Headroom (jewel suffix) | +1-2% | Weight `25` on every colour. |

The pocket is an `EXTRAS` entry (`headroom` theme), placed after the original fill. Its nodes are live by the design rules because the Crusher reads the stat; a Furnace allocating one gains nothing and its tooltip says so, as with any irrelevant benefit.

Budget: the tree gives +5%. Reaching the 90% ceiling then needs five +2% jewels, or more at lower rolls, each of which spends one of four lines. A player who gets there has given most of their sockets to yield.

### UI

- The Stats tab's Bonus Output line shows the paid value. Its hover replaces the "Diminishing returns" line with `Capped: 86% rolled, 75% maximum, 11% over cap`, then the Maximum Bonus Output sources.
- Maximum Bonus Output appears in the Stats tab whenever any source changes it from 75%.
- Recipe previews, the bonus bar hover, and the Jade line use the paid value, as they use the soft-capped value today.
- The Crusher and Cavitation Generator wiki pages, Machine Stats, and Rarity and Affixes replace the diminishing-returns text with the cap.

## Storage and Saves

- A new data component, `rngtech:jewels`, holds every socketed jewel: `body`, a list of up to four stacks, and `tree`, a map from socket node ID to stack. It sits beside `rngtech:machine_traits` and `rngtech:machine_progression` in the block entity's components, is kept on the dropped item, and uses a lenient codec so a jewel that no longer decodes is dropped without touching the machine's other data. `MachineProgressionState` is unchanged, so build codes, the 120-slot sync, and the Forestry cart's NBT keep their shape; the cart carries the component on its entity.
- Soul Transfer copies the `tree` entries with the Mastery and leaves `body` with the body. A tree entry whose node is not allocated in the new machine stays stored and inactive.
- No catalog version bump: sockets and Headroom nodes are appended extras.
- Existing Crushers recompute on load; no yield data is stored.

## Validation

- `masteryCheck`: 9 sockets, degree 1, distance and gate rules, each socket live; the Headroom and Fine Screens extras follow the notable investment rule; the node-count assertions move to the new total.
- `WorkOrderRuleChecks`: no Output Amount goal above the ceiling.
- Domain tests: every jewel family has one tier and a reader; the three jewel profiles share one family list; Bedazzling rolls only on `HAS_JEWEL_SOCKETS` profiles; sockets never exceed 4; the cap formula and ceiling; Single Pass still zeroes jewel yield.
- `moddex:check`: the profile parser accepts the new profile knobs in literal `profile(...)` form; the smoke group counts are updated for the three jewel profiles; the tree validator accepts `JEWEL_SOCKET`.
- `check-work-orders.mjs`: the jewel reward rate.
- GameTests: socket a jewel through the Mastery action and read its stat on the machine; break and replace a machine with tree and body jewels; reject a Chaos Crystal on an occupied Bedazzling line; a Crusher at an 86-point bucket pays 75%, and 77% with a +2% jewel.

## Delivery Phases

1. **PRD and state.** This document and a state page on acceptance.
2. **Bonus Output cap.** `MAX_OUTPUT_AMOUNT`, the cap and ceiling in the accumulator, both readers, breakdown text, the Headroom and Fine Screens extras, wiki and docs. Ships on its own; it has no jewel dependency.
3. **Jewel items and pool.** The three items, the Jewel Blank and its calibration recipe, the jewel profiles with the 2/2 cap and Rare count table, the pool, threshold effects, readers, tooltips, JEI.
4. **Tree sockets.** `JEWEL_SOCKET`, the nine extras, the `tree` half of `rngtech:jewels`, the socket menu slots, the Mastery screen actions and rendering, the effect source, the `masteryCheck` and `check-mega-tree.mjs` rules.
5. **Body sockets.** `HAS_JEWEL_SOCKETS`, Bedazzling, the Gold Battery Chassis implicit, the Gold Crusher and Furnace bodies with theirs, models and textures, the `body` half of `rngtech:jewels`, the Gear tab row, the refinement rejection.
6. **Refinement and sources.** Tier-operation rejection, jewel corruption pools, the Work Order reward entry.
7. **Documentation and guide.** Wiki pages for Jewels, Gold bodies, and the cap; Machine Mastery and Rarity updates; the regenerated in-game guide; the implementation matrix.

## Test Plan

- Cut a Jewel Blank of each colour at a Stage 3 Calibrator and identify the result. Rarity, affix count, and Refinement Potential follow the tables, and the colour's home lines appear more often across fifty cuts.
- Socket a Drive jewel with "of Pace" in a Furnace's tree socket. The Stats tab shows the speed line labelled "Jewel", the hover dims the jewel's Bonus Output line, and breaking and replacing the Furnace keeps the jewel.
- Refund the socket. The jewel stays, dimmed, and the stat is gone; re-allocating brings it back without a server round trip of the item.
- Roll Bedazzling on a Stage 8 Exotic Crusher until Tier 7, add a Gold body's implicit in a second test, and confirm the Gear tab caps at four sockets.
- Put a jewel in a Bedazzling socket and apply a Chaos Crystal: the forge rejects it and names the reason; remove the jewel and the reroll works.
- Crusher yield: a bucket of 50 pays 50%, 86 pays 75%, 86 with the Overflow Gate pays 77%, and a machine stacked to 110% of maximum shows the 90% ceiling in its breakdown. Single Pass still pays 1x. A no-cell Crusher at the cap pays 75% of its bonus above 1x.
- Cavitation Generator: Bonus Output over 75% draws no less Water than at 75%.
- Soul Transfer a Crusher with tree jewels into a new body: the jewels arrive; body-socket jewels stay in the old body's sockets.
- Benchmark: `runTickBenchmark` within noise before and after; jewels add no per-tick work beyond the stat cache rebuild.

## Acceptance Criteria

- A jewel socketed on the tree or in a body changes the machine's stats exactly as its tooltip's active lines say, and nowhere else.
- No action deletes or drops a jewel without the player removing it.
- The Crusher never pays more than its Maximum Bonus Output, and the Stats tab shows the rolled, paid, and over-cap values.
- Every jewel family is read by at least one machine family, and the three profiles expose every family.
- `quickCheck`, `ciCheck`, `moddex:check`, `repo:check`, both MkDocs builds, the guide check, and `runGameTestServer` pass.

## Deferred

- **Radius and threshold jewels** that change travel nodes near their socket, and **Unique jewels** with fixed behaviours. Both belong in the Unique catalog with its ranged lines, once plain jewels have been played.
- **Gold bodies for other ladders** (Alloy Furnace, Metal Press, Battery Chassis) once the two first bodies prove the socket base is worth building.
- **Body sockets on the Forestry Companion**, which needs a cart UI row.
- **A Bonus Summary section** for jewels, alongside the deferred Work Order section.

## Decisions

Resolved 2026-11-01:

- Maximum Bonus Output has a 90% ceiling. It is visible, it has precedent, and sockets make the number of +1-2% sources unbounded otherwise.
- Nine tree sockets. That is one per 11 points, below Path of Exile's density, and a 99-point build will reach three to five.
- Gold is the socket base: the existing Gold Battery Chassis plus new Gold Crusher and Furnace bodies. Every machine body can roll Bedazzling, Mastery or not.
- The cap reaches the Cavitation Generator, with its Output Amount lines moved to the yield table without the Jaws penalty.
- The +5% of tree Maximum Bonus Output sits in one Headroom diamond rather than split across the tree, so it reads as one pocket and its notable has the three reward allocations the rules require.
- A jewel's Refinement Potential follows the cut's calibration stability.
- Jewels also appear in vanilla chests by default, behind a config key, as Uniques do.

Open:

- The suffix name. "of Bedazzling" is the working name; the family id `jewel_sockets` does not depend on it.
