# PRD: Phase Array

> Design requirements, not a release status report. See [Current Implementation](../reference/current-implementation.md) and the [Exotic Affix Forge](https://c-pettersson.github.io/rngtech/exotic-affix-forge/) wiki page for current behavior.

PRD status: Accepted

Implementation status: Planned

Release: Not scheduled

Last updated: 2026-10-21

Model: `./gradlew phaseArraySim`, described in `tools/balance/README.md`.

## Summary

The Phase Array is the endgame refinement multiblock. It replaces the Exotic Affix Forge. A player builds a straight line: an Emitter, three to nine Phase Coils, and a Cradle holding the item. They pick an operation, the line charges FE, and they fire it. A pulse runs down the line, coil to coil. Each coil either holds the pulse or lets it slip. If the pulse reaches the Cradle, the operation happens. If it dies on the way, it does not.

The Array is RNGTech's crafting bench, in the Path of Exile sense: the player chooses what happens to the item and pays for the choice. It takes any refinable item of any stage. Its operations are datapack recipes, so a pack maker can change, remove, or add them. Each operation spends a little Refinement Potential, so RP stays the ceiling it is everywhere else, but the Array's real limits sit below that ceiling: FE that grows with how good the item already is, Strain that grows with every pulse and raises the odds of a Fracture, and Glyphs, which no recipe makes. Glyphs ship with this PRD, with default sources in the existing challenge loot tables and in Dormant Machines, so the Array works on its own. [Work Orders](work-orders.md) will add Glyphs to their rewards when they land. Most items are done when the next pulse costs more than the player wants to pay, before their RP runs out.

The Array is also a build of its own. Its Emitter and Coils roll rarity and affixes like machine parts, Glyphs inscribed into Coils unlock deeper operations, and a Coil can be pulled from the line and refined in the Array like any other part. A player who wants nothing but a longer, steadier line has a reason to run it.

## Why the Exotic Affix Forge is replaced

The Exotic Affix Forge got three things right: the player chooses the affix, it works at Stage 8, and it costs FE instead of catalysts alone. Its problem is the FE. Its operations cost 6 to 16 MFE, and endgame generators make 1 to 3 kFE/t each, so one operation is a 30 to 90 minute wait on one generator, and the only thing happening during that wait is a progress bar. Every change since has been a patch on that wait, not a better design:

- Rising per-item costs made the wait longer on exactly the items the player cares about.
- Nitrogen Cooling let the forge finish as fast as FE arrives, which exposed that FE cannot arrive fast enough.
- Ten Battery Cell slots and Discharge Coils made FE arrive faster.
- Spool-up added a failure gamble so the fast path had tension.
- Forge affixes and a Spool dial tuned the gamble.

Each step followed from the one before it. None of them made the forge more fun to use, and none of them gave the player a reason to stop other than running out of RP. The Array keeps what was right and drops the rest.

## References

- [Work Orders PRD](work-orders.md): the source of Glyphs.
- [Crafting and Upgrades](../systems/crafting.md): Strain was planned there as crafting wear.
- [Affix Generation](../systems/affix-generation.md)
- [Corruption PRD](corruption.md): Corrupted items stay final.
- [Unique Items PRD](uniques.md): Uniques stay out.
- [Machine Guidelines](../reference/machine-guidelines.md), especially the refinement and visible trade-off rules.
- [Dormant Machines](https://c-pettersson.github.io/rngtech/dormant-machines/): a second Glyph source.

Key existing code:

- `src/main/java/com/rngtech/rpg/refinement/RefinementEngine.java`, `RefinementTargets.java`: every operation goes through this library.
- `src/main/java/com/rngtech/content/blockentity/ExoticAffixForgeBlockEntity.java`: the block that becomes the Cradle.
- `src/main/java/com/rngtech/content/block/WoodenDehumidifierBlock.java`: the one shaped multiblock, a hollow 3x3x3 frame, for the formation pattern.
- `src/main/java/com/rngtech/content/loot/ChallengeLoot.java`, `ChallengeContext.java`: Glyph rewards.
- `src/main/java/com/rngtech/rpg/ModifierEligibilityProfiles.java`: Emitter and Coil affix pools.
- `src/main/java/com/rngtech/client/screen/AffixForgeScreen.java`: the outcome table the Volatile Catalyst already shows, reused for the pulse preview.

## Goals

- A crafting bench for every refinable item at every stage: the player chooses the affix, the slot, or the swap, and pays for the choice.
- Every cost is visible before the pulse: FE, RP, Strain, Fracture odds, Glyph, and the full outcome table.
- The soft limit is price, not a counter. FE grows with the item's quality, Strain grows with each pulse, and Glyphs are scarce. RP is spent sparingly, so a player decides an item is done before RP decides for them.
- Every operation is a datapack recipe. Pack makers change, remove, or add operations without code.
- A pulse is a short, watchable, audible sequence with a payoff at the end. It reads as a machine holding a signal together, not as a spell.
- The Array scales by length: more inscribed Coils unlock deeper operations and add charge, better Coils hold the pulse more often, a better Emitter charges faster.
- The Array is a target for itself. The Emitter and Coils are refinable parts, and Glyphs spent on the line compete with Glyphs spent on items.
- Glyphs have no recipe. They come from RNGTech machine challenges, and pack makers can hand them out from any source.
- Everything the Array needs ships with it: the parts and their recipes, the operations, the Glyph item and its default sources, and a balance-world setup that spawns a formed line. No later PRD is required for it to be playable.
- Emitter and Coil affix pools are deep enough that a Rare roll is a roll. Each part has at least six prefixes and six suffixes, every one read by the Cradle.

## Non-Goals

- No new RP source and no RP refund. Reforge stays the only way to a fresh RP roll.
- No Tier 8 or new affix tiers. The Array reaches Tier 7 on Exotic items, where the Affix Forge stops at 6.
- No refinement of Corrupted items or Uniques. Corruption stays final, and Uniques stay sidegrades.
- No item automation and no redstone trigger. A pulse is a player click, so there is no roll-until-X loop.
- No operation that raises yield, Refinement Potential, or Gear stage acceptance beyond what affixes already allow.
- No Mastery family for the Array, and no Work Orders on the Array.
- No Nitrogen, cooling, spool, or power-failure mechanic. Speed is charge time, and charge time is supply.
- No floating icons. Status is the coil lights, the line's hum, the item in the Cradle, Jade, and the Cradle screen.

## Vocabulary

| Term | Meaning |
|---|---|
| Phase Array | The formed multiblock: an Emitter, three to nine Phase Coils, and a Cradle in a straight horizontal line. |
| Cradle | `rngtech:exotic_affix_forge`, renamed. The controller block at the end of the line. It holds the target, the Glyph slot, the output, and the screen. |
| Emitter | `rngtech:phase_emitter`. The block at the start of the line. It stores and delivers most of the charge. Rolls rarity and affixes. |
| Phase Coil | `rngtech:phase_coil`. A line block between the Emitter and the Cradle. Rolls rarity and affixes. Blank until a Glyph is inscribed. |
| Position | A Coil's place in the line, counted from the Emitter: 1 is next to the Emitter, 9 is the farthest allowed. |
| Glyph | `rngtech:glyph`, with a tier I to V in a data component. No recipe. Consumed by deep operations, or inscribed into a Coil for good. |
| Operation | What a pulse does. A recipe of type `rngtech:phase_array`, under `data/<namespace>/recipe/`. |
| Pulse | One attempt: charge, then the hold sequence, then the outcome. |
| Hold sequence | The pulse crosses the Coils in position order. Each one holds or slips. The sequence stops at the first slip. |
| Quality | The sum of an item's affix tiers. A Rare item with six Tier 7 affixes has Quality 42. |
| Strain | A whole number on the item, 0 when crafted. Each pulse adds 1. Each point above the line's Strain Tolerance adds Fracture chance. |
| Strain Tolerance | A line stat, the sum of its parts' Tolerance. Strain up to this number adds no Fracture chance. |
| Fracture | A pulse outcome where one random affix loses a tier. A Tier 1 affix is removed. |
| Overhold | A bonus outcome, possible only when the line has more Coils than the operation needs. |

## Structure

The Array is a straight line on one horizontal axis, 5 to 11 blocks long: the Emitter, then 3 to 9 Phase Coils, then the Cradle. The Cradle faces away from the line, so the item is reached from the end. Nothing is required above, below, or beside the line.

The Cradle forms the Array on its own, the same way the Wooden Dehumidifier forms its cube: it scans every horizontal side on a timer and whenever a member block is placed, changed, or broken, and lights the members while the line is complete. A line with fewer than three inscribed Coils forms but can fire nothing, and the Cradle screen says which positions are blank. Breaking any line block unforms it. The Emitter and Coils keep their rolled traits when mined.

Draft base stats:

| Part | Stage | Base stats |
|---|---:|---|
| Emitter | 8 | Charge Capacity 16 MFE, Charge Rate 8 kFE/t |
| Phase Coil | 8 | Hold Chance 92%, Charge Capacity 4 MFE, Charge Rate 2 kFE/t |

A nine-Coil line stores 52 MFE and charges at 26 kFE/t from its own parts. The Emitter, the Cradle, and every Coil accept FE on any side. The item sits visible in the Cradle while the line is charging or firing.

The Cradle also has nine Battery Cell sockets, like a Dormant Machine. Cells in the sockets drain into the charge, each at its own output rate, so a player can carry a pulse's worth of FE to a line that has no cable. Empty cells come out of the bottom. The sockets are the Array's only Gear.

The Emitter and Coils are Stage 8 parts. Their recipes use Exotic Machine Frames, Nullite, and a Calibrated Conductive Component, and both sit behind the Nullite material condition. A Blank Coil takes a Glyph in the Cradle's Inscribe tab, which fixes the Coil's tier and uses the Glyph up. The position sets the lowest Glyph tier it accepts:

| Position | Lowest Glyph tier |
|---:|---:|
| 1 to 3 | I |
| 4 to 5 | II |
| 6 to 7 | III |
| 8 | IV |
| 9 | V |

A Coil inscribed with a higher tier than its position needs is legal. An inscribed Coil can move to any position that accepts its tier, so a player can extend a line by inserting a new Coil near the Emitter and shifting the rest outward.

## Player Flow

1. The player puts an identified, non-Corrupted, non-Unique item in the Cradle's target slot. Machine items, machine parts, Battery Cells, Tool Heads, Tool Rods, companions, Emitters, and Coils all qualify, at any stage.
2. They pick an operation. Operations that need a selection ask for an affix, an empty slot, or a donor item.
3. The screen shows the pulse preview: FE cost, charge time at the current supply, Strain after the pulse, the Glyph it consumes, each Coil's Hold Chance, and the outcome table: Success, Overhold, Partial, Miss, and Fracture with their odds.
4. They click **Charge**. The line fills from its own parts and from supply. Charging pauses when supply dips and resumes when it returns. Nothing is spent.
5. When the charge is full, the button becomes **Fire**. Clicking it runs the hold sequence: the pulse leaves the Emitter and crosses each Coil in order, with a rising tone and a light per Coil. The sequence stops at the first slip.
6. The outcome applies, the FE and Glyph are spent, Strain goes up by one, and the item moves to the output slot. The outcome, and the position that slipped, stay on the screen until the next target goes in.

Charge is kept if the player closes the screen or changes the operation to one with the same or a lower cost. Changing to a costlier operation keeps the charge and asks for more.

## Operations

Operations are recipes of type `rngtech:phase_array`. Each names its action, selection, Coil count, Glyph tier, base FE, RP cost, and the Partial and Overhold outcomes, so a pack can retune, delete, or add one, and JEI lists them. Draft default set, by the Coils it needs:

| Operation | Effect on Success | Selection | Coils | Glyph | RP | Partial | Overhold |
|---|---|---|---:|---|---:|---|---|
| Reforge | Rerolls rarity, affixes, and RP as if newly crafted. Strain stays. | None | 3 | None | 0 | Rerolls affixes only | Rolls rarity twice, keeps the better |
| Attune | Rerolls every affix's value within its tier. | None | 4 | None | 1 | Rerolls one random affix's value | Rolls each value twice, keeps the better |
| Resonate | Raises the selected affix one tier, up to Tier 6. | One affix | 5 | None | 1 | Rerolls its value within its tier | Raises it two tiers |
| Excise | Removes the selected affix. | One affix | 5 | None | 1 | Removes a random affix | Also removes one Strain |
| Imprint | Adds the chosen affix to the chosen empty slot. The tier rolls with the item's normal weights. | One empty slot and one legal affix | 6 | II or higher | 2 | Adds a random affix of the chosen affix's family | Rolls the tier twice, keeps the higher |
| Transmute | Swaps the selected affix for another legal affix in the same prefix or suffix slot, keeping its tier. | One affix | 7 | III or higher | 1 | Swaps it for a random affix of the same slot type | Keeps the roll's value quality too |
| Temper | Sets Strain to 0. | None | 7 | III or higher | 0 | Halves Strain, rounding down | None |
| Exalt | Raises the selected Tier 6 affix to Tier 7. Exotic items only. | One Tier 6 affix | 8 | IV or higher | 2 | Rerolls its value within Tier 6 | None |
| Graft | Moves the selected affix from a donor item onto the target's chosen empty slot, keeping tier and value. The donor is destroyed. | One donor affix and one empty slot | 9 | V | 2 | The affix arrives one tier lower | The donor survives, stripped of that affix |

RP is spent on Success, Overhold, and Partial, never on a Miss or a Fracture, like a failed catalyst at the Affix Forge. The costs are fixed, lower than the Affix Forge's rolled ranges for the same work, and the item needs the full cost to start. The Array is a bench, not a second RP budget. The model set Resonate at 1 RP: at 2 RP, RP was the only limit a Rare item ever met, and Strain and FE never got to matter.

Rules:

- Legality comes from the shared refinement library. An affix that cannot roll on the item cannot be Imprinted or Grafted onto it. Fixed identity traits and implicits are never selectable.
- Imprint lists every affix the item's eligibility profile allows in the chosen slot. The player chooses the affix; the tier still rolls.
- Reforge works on any legal target, including one at 0 RP. It is the only operation that renews RP, and it does so only as a side effect of rerolling the item.
- Tier limits follow the item's normal rules. Tier 7 exists only on Stage 8 Exotic items, so Exalt is only legal there, and Imprint on a Stage 2 part rolls Stage 2 tier weights.
- Graft needs the donor and the target to share an eligibility profile for that affix. The donor may be at 0 RP or Corrupted; Corrupted donors give the affix, not their implicit.
- Pack makers can edit every operation's cost, Coil count, Glyph tier, RP, and outcome weights in its recipe, remove one by overriding it with an empty recipe, and add operations that compose the refinement library's existing actions. A recipe condition can hide an operation behind a config key, as the Seal and Volatile Catalyst recipes do.

## The Hold Sequence

A pulse resolves in two steps, both shown in the preview:

1. **Fracture check.** Before the pulse leaves the Emitter, the item fractures with a chance of 1% per point of Strain above the line's Strain Tolerance. A fractured pulse spends FE and the Glyph, and the sequence does not run. Strain still rises by one. At 0 Strain, or within Tolerance, this step cannot happen.
2. **Hold sequence.** The pulse crosses the Coils in position order, from 1 up to the last inscribed Coil. Each holds with its own Hold Chance, or slips. The first slip ends the sequence. With `R` the Coils the operation needs and `L` the Coils that held:
    - `L` equals the number of inscribed Coils: **Success**, and the Overhold roll runs if the line has more Coils than `R`.
    - `L` is at least `R`: **Success**, no Overhold.
    - `L` is `R - 1` or `R - 2`: **Partial**.
    - Otherwise: **Miss**. Nothing changes on the item.

Overhold rolls once per Coil beyond `R` that held, at 8% each. One Overhold is enough; extra Coils only add chances.

Odds on a line of seven unmodified Coils, Resonate (`R` = 5), 0 Strain, from the model's closed form:

| Outcome | Chance |
|---|---:|
| Success | 66% |
| of which Overhold | 9% |
| Partial | 12% |
| Miss | 22% |
| Fracture | 0% |

At 10 Strain the same pulse fractures 10% of the time first. At 25 Strain it fractures a quarter of the time, and a player who reads the preview stops there: the model's players all do. Temper resets that, at a Glyph and a large FE cost, so the player decides between tempering and living with the risk. The model started Fracture at 2% per point; that made a fresh Rare item unworkable after 13 pulses and cost five Glyphs of Temper to reach the same place one Glyph reaches at 1%.

Nine Coils against seven buy Overhold, not Success: 13% against 9% on Resonate. Hold Chance 92% is kept. At 95% the ladder barely moves, and at 88% a Rare item fractures twice as often before it is done.

## Costs

FE is the main cost. It grows with the item, not with the item's history, so a bad item is cheap to fix and a near-perfect item is expensive to polish:

```text
FE = base(operation) x stage(item) x 1.5 ^ (Quality / 3.5)
```

`base` is 1 MFE for Attune and Excise, 2 MFE for Resonate, Reforge, and Imprint, 4 MFE for Transmute, Temper, and Exalt, and 8 MFE for Graft. `stage` scales the price to the item, so a lower-stage item is cheap to work on and the Array is worth building before Stage 8:

| Item stage | `stage` factor |
|---:|---:|
| 0 to 2 | 0.1 |
| 3 to 4 | 0.25 |
| 5 to 6 | 0.5 |
| 7 | 1 |
| 8 | 1.5 |

Charge time for one Resonate on a Stage 8 item at a reference supply of 20 kFE/t, from the model:

| Item | Quality | FE | Resonate charge time |
|---|---:|---:|---|
| Fresh Rare Exotic part | 12 | 12 MFE | 30 seconds |
| Mid-refined, all Tier 4 | 24 | 48 MFE | 2 minutes |
| Six Tier 6 affixes | 36 | 194 MFE | 8 minutes |
| Six Tier 7 affixes | 42 | 389 MFE | 16 minutes |

A Stage 5 item costs a third of that at the same Quality, and a Stage 2 item a fifteenth. The last pulses on an item cost thirty times the first. A player with a long line and a bank of Battery Chassis can still finish a perfect item; they pay for it with everything they stored.

Strain adds 1 per pulse, on every outcome. Nothing but Temper and Excise's Overhold removes it. A Reforge keeps it, so a reforged item is a fresh roll on a worn body.

Glyphs are consumed per pulse by Imprint, Transmute, Temper, Exalt, and Graft. The tier is a floor: a Tier V Glyph pays for an Imprint. The Glyph is spent whatever the outcome.

RP is the ceiling. A fresh Stage 5 part with 18 RP can take eighteen Resonates in the Array, against three to six at the Affix Forge. The Array does not replace the Affix Forge for early play: the Emitter and Coils are Stage 8 parts, and Glyphs come from Stage 6 calibrations and Mastery milestones at the earliest. It replaces the question of what to do with a good item once catalysts stop being the limit.

### What the model shows

`./gradlew phaseArraySim` plays these numbers out with players who read the preview and stop firing at 25% Fracture. On a seven-Coil line at 20 kFE/t, medians over 2,000 trials:

| Player | Item | Pulses | Time | Ends at | Stopped by |
|---|---|---:|---:|---|---|
| Resonate the lowest affix, never Temper | Fresh Rare Stage 8 | 25 | 30 min | Quality 25 of 36, 10 RP left | Strain |
| The same, Tempering at 10% Fracture | Fresh Rare Stage 8 | 39 | 100 min, 5 Glyphs | Quality 34; half reach all Tier 6 | RP |
| Resonate the lowest affix | Fresh Magic Stage 8 | 12 | 4 min | Both affixes Tier 6 | Done |
| Resonate the lowest affix | Fresh Rare Stage 5 | 25 | 8 min | Quality 24; 40% run out of RP first | Strain |
| Exalt six Tier 6 affixes to Tier 7, nine Coils | All-Tier-6 Rare Stage 8 | 12 | 4.5 h, 12 Glyphs | 94% reach six Tier 7 | Done |
| Reforge until Rare with Quality 12 or more | Any Stage 8 | 1 to 25 | Under 5 min | 83% get one before Strain stops them | Done |

The three limits bind in the order the PRD wants. Strain is the first wall and Glyphs buy the way past it, RP ends the Resonate stretch around all Tier 6, and FE is what a perfect item costs: a Tier 7 pulse at the end is 16 to 30 minutes of the reference supply. A Reforge is cheap on a bad item and useless on a good one, since a Rare with Quality 12 is 10% of rolls and the item's Strain runs out before the player's luck does.

## Glyphs and Work Orders

Glyphs have no recipe. This PRD ships the item and its default sources. All six challenge loot tables are empty today; these are their first default entries, and each one is an RNGTech machine challenge, never vanilla loot. Draft:

| Source | Condition | Glyph |
|---|---|---|
| `rngtech:challenges/mastery_level` | Level 20, 40, 60, 80, and 100 | Tier I at 20 and 40, Tier II at 60 and 80, Tier III at 100 |
| `rngtech:challenges/calibration` | Stage 6 or higher, 90% stability or higher | 15% chance of Tier II; Stage 8 at 95% gives Tier III |
| `rngtech:challenges/forestry_harvest` | A Stage 7 or higher cutting head | 2% chance of Tier II |
| `rngtech:challenges/vacuum_collapse` | Instability 60 or higher | 10% chance of Tier III; 90 or higher gives Tier IV |
| `rngtech:challenges/crusher_jam` | Processing Level 7 or higher | 5% chance of Tier I |
| `rngtech:challenges/heat_failure` | Never | Failing on purpose should not pay |
| Dormant Machine, woken | By site | Collapsed Workshop and Cart Siding Tier I, Calibration Vault and Flooded Foundry Tier II, Nether Smeltworks Tier III, End Relay Tier IV |
| Pack makers | Any | `/give`, quests, other mods' loot, or a recipe they add. A plain `rngtech:glyph` stack with no tier reads as Tier I. |

Tier V has no default source until Work Orders land, and this PRD adds none. A line with eight positions open runs everything but Graft. A command, `/rngtech glyph give <players> <tier>`, exists for testing and for packs.

When the [Work Orders PRD](work-orders.md) ships, its reward table pays Glyphs of the order's tier, including Tier V, and becomes the main supply. Nothing in this PRD waits for it.

A Glyph has two uses, and they compete:

- **Consume** it on a deep pulse.
- **Inscribe** it into a Blank Coil for good, to open a position.

Opening position 9 costs a Tier V Glyph, which is also what a Graft costs. The player who inscribes it gives up one Graft to make every later Graft possible.

## The Array as a Build

The Emitter and Coils roll rarity and affixes when crafted, like other Stage 8 parts, from their own eligibility profiles. A Rare rolls three prefixes and three suffixes, so each part's pool has at least six of each, and the profile loader rejects a Phase Array profile with fewer. The line reads eleven stats, and every affix moves one of them:

| Stat | Read by | Meaning |
|---|---|---|
| Charge Capacity | Line sum | FE the line can hold |
| Charge Rate | Line sum | FE/t the line takes from supply and cells |
| Hold Chance | Each Coil | The chance this Coil holds the pulse |
| Second Hold | Each Coil | The chance a slip in this Coil is rolled again |
| Overhold Chance | Each Coil, plus the Emitter | The chance a surplus hold in this Coil triggers Overhold |
| Partial Window | Emitter | How many Coils short of `R` still count as Partial, 2 at base |
| Strain Tolerance | Line sum | Strain points that add no Fracture chance |
| FE Thrift | Line sum | Percent off every pulse's FE |
| FE Recovery | Line sum | Percent of the pulse's FE returned to the charge on a slip |
| Glyph Salvage | Line sum | Chance the Glyph returns on a Miss |
| Strain Mercy | Line sum | Chance a pulse adds no Strain |

Draft pools:

| Part | Prefixes | Suffixes |
|---|---|---|
| Emitter | Dense (Charge Capacity), Conductive (Charge Rate), Focused (Overhold Chance), Lenient (Partial Window +1), Tempered (Strain Tolerance), Primed (Charge Rate while cells are socketed) | of Thrift (FE Thrift), of Recovery (FE Recovery), of Salvage (Glyph Salvage), of Mercy (Strain Mercy), of Banking (Charge Capacity), of Patience (Strain Tolerance) |
| Coil | Steady (Hold Chance), Persistent (Second Hold), Resonant (Overhold Chance), Charged (Charge Capacity), Conductive (Charge Rate), Tempered (Strain Tolerance) | of Thrift (FE Thrift), of Recovery (FE Recovery), of Salvage (Glyph Salvage), of Mercy (Strain Mercy), of Poise (Hold Chance, smaller range than Steady), of Focus (Overhold Chance, smaller range than Resonant) |

Each affix must change a number the Cradle shows. No affix raises yield, RP, or stage acceptance. Emitter and Coil affixes are local to the part, and every Coil contributes to the line's totals. Two affixes that move the same stat from different slots, such as Steady and of Poise, share a modifier group so a part cannot stack both from one slot type twice over; the suffix is the weaker roll.

A Coil or Emitter is a legal Array target. Pulling position 7 out to Exalt it drops the line to six Coils, so the player cannot fire Exalt on it in this line. They can in a friend's line, or on a second one. The intended loop is: open positions, refine Coils at the Affix Forge while RP lasts, then refine them in the Array when it does not.

The Array has no Mastery. Its progression is length, Glyph tiers, and part affixes.

## Storage and Saves

- Strain is a new optional field on the item's traits, next to the Exotic Forge history it replaces. Missing means 0, so existing items load unchanged.
- The Cradle block entity stores the formed state, the line's direction and length, the charge, the chosen operation and selection, and the last pulse's outcome.
- The Emitter and Coils store their traits as machine parts do, and copy them to the item on drop.
- An inscribed Coil stores its Glyph tier in `rngtech:coil_tier`.
- The Exotic Forge per-item history, the Nitrogen tank, and the cooling and spool state are removed. A world with an Exotic Affix Forge placed keeps the block as an unformed Cradle.

## UI

The Cradle screen does not follow the Process, Gear, and Stats tab layout of the processing machines. It follows the Dormant Machine: one graphical screen that draws the line as it stands, with hover for every exact value and no text labels.

- **The line:** the Emitter, each Coil, and the Cradle drawn left to right as they are built, each Coil a node that shows its position, its tier mark, and whether it is blank. Hover a node for its Hold Chance, Second Hold, Overhold Chance, and affixes. During a pulse the nodes light in order and the slipped one flashes.
- **The chamber:** the target item at the right end, with its affixes listed under it. Selecting an affix, an empty slot, or a donor affix is a click on that list. Strain shows as marks on the chamber's rim.
- **The charge conduit:** fills along the line as FE arrives, from the Emitter toward the chamber. Hover for FE held, FE needed, and charge time at the current rate.
- **The cell sockets:** nine sockets under the Emitter, each with a ring that dims as its cell empties, exactly as on a Dormant Machine.
- **The operation selector:** a column of operation marks beside the chamber. A mark that cannot run is dim, and its hover names the missing position, Glyph, selection, or RP. Hover the selected mark for the full outcome table: Success, Overhold, Partial, Miss, and Fracture with their odds, plus FE, RP, Glyph, and Strain after the pulse.
- **Inscribing:** drop a Glyph on a blank node. The node shows the lowest tier its position accepts.
- **One button:** Charge, then Fire. It reads the current state and nothing else is needed.
- **Tooltips:** Strain on every item that has it, with the Fracture chance it adds. Glyph tier. Emitter and Coil affixes.
- **JEI:** every `rngtech:phase_array` recipe in a Phase Array category, with its Coil count, Glyph, FE at Quality 0, and RP, and information pages for Glyphs, the Emitter, and Coils.
- **Jade:** formed or the first missing block, charge, and the current operation.
- **In world:** the item sits in the Cradle while the line works. Each Coil lights as the pulse holds in it, in order from the Emitter, with a rising tone. On Success the Cradle flashes and the item drops into its output. On a slip the lit Coils go dark from the slipped one back to the Emitter. Sound and light are the status; there is no overlay.

## Delivery Phases

1. **PRD and state.** This document and [its state page](phase-array-state.md).
2. **Simulation.** A seeded model of Hold Chance, Strain, Overhold, and FE by Quality, with the draft numbers tuned to the time targets.
3. **Parts and formation.** Emitter, Coil, Glyph, their traits and profiles, the line pattern, formation and unformation, and the Cradle rename.
4. **Operations, no sequence.** The operation loader, the Fire tab, charge, and every operation resolving through the refinement library with an instant outcome.
5. **The hold sequence.** Timed Coil holds, sounds, lights, and the stored last outcome.
6. **Glyph sources.** Work Order reward entries, the Dormant Machine drop, and the plain-stack rule.
7. **Removal.** Exotic Forge recipes, history, cooling, cells, Coils, and their lang, JEI, and wiki entries.
8. **Balance-world setup.** An `rngbalance:phase_array` setup in `tools/balance-world/build.py`: a formed nine-Coil line with every position inscribed, a Debug Battery on the Emitter, a Debug Chest of Glyphs of every tier, and a chest of Rare Stage 8 parts and a Stage 5 part to fire at. The Cradle starts with a target in place.
9. **Documentation.** The Phase Array wiki page, Rarity and Affixes, Modpack Creators, the implementation matrix, the GuideME guide, Machine Design Notes, and quests.

## Test Plan

Domain checks (`masteryCheck`):

- Items with and without Strain round-trip, and existing items load at 0 Strain.
- Every operation refuses an illegal target, selection, Glyph, Coil count, or short RP and spends nothing.
- RP is spent on Success, Overhold, and Partial only.
- A datapack can override a default operation with a changed cost, and an empty recipe removes it.
- The outcome ladder over a seeded sample matches the preview's odds within tolerance.
- The Fracture check is 0% at 0 Strain and 1% per point after.
- The outcome ladder the Cradle previews matches `PhaseArraySimulation`'s closed form for every operation and line size.
- FE by Quality matches the formula for every operation and stage.
- No Emitter or Coil affix touches yield, RP, or stage acceptance.
- Every Phase Array profile has at least six prefixes and six suffixes, and every affix in it is read by the Cradle.
- Each default challenge table entry has a condition, and no entry sits on `heat_failure`.
- Graft destroys the donor on Success and keeps it on Overhold.

ModDex checks:

- Glyphs have no recipe, and Emitter and Coil recipes carry the Nullite condition.
- No recycling or reactor recipe accepts a Glyph.
- Every Emitter and Coil affix is read by the Cradle.

GameTests:

- Form a line, charge from a generator, fire Attune on a Stage 8 part, and check that the output has new values and Strain 1.
- Break a Coil and check that the line unforms and the charge is kept.

In-game checks:

- Run `rngbalance:phase_array` in the Full Test environment world, fire every operation, and check that every preview matches what happens.
- Craft and roll ten Rare Coils and check that no two share the same six affixes.
- Fire an item to a Fracture and read the outcome on the screen and in the world.
- Inscribe position 9 and Graft between two parts.
- Pull a Coil, refine it in a second line, and put it back.

## Acceptance Criteria

- Any refinable item of any stage can be refined in the Array, the player chooses the affix for Imprint, Transmute, and Graft, and every operation's cost and odds show before the pulse.
- Every operation is a `rngtech:phase_array` recipe a datapack can change, remove, or add.
- No Array part affix raises yield, RP, or stage acceptance.
- Glyphs have no recipe, the challenge tables and Dormant Machines yield them by default, and `/give` and `/rngtech glyph give` Glyphs work.
- A player on `main` with no other PRD, datapack, or pack content can build a line, earn Glyphs, open positions, and fire every operation but Graft.
- `rngbalance:phase_array` spawns a formed, inscribed, powered line in the balance test world.
- The Exotic Affix Forge's operations, history, cooling, cells, Discharge Coils, and forge affixes are gone, and an existing placed forge loads as an unformed Cradle.
- `./gradlew ciCheck`, `./gradlew runGameTestServer`, `npm run moddex:check`, `npm run repo:check`, `mkdocs build --strict`, and `mkdocs build --strict -f mkdocs.wiki.yml` pass.

## Effect on Open Work

- The Exotic Affix Forge FE/t branch (Nitrogen Cooling, Spool-up, ten cells, Discharge Coils, forge affixes) is superseded. Two pieces survive on their own: the `CompactValueText` SI fix, and the rule that a readout claims only what the machine measures. Extract those; close the rest.
- The Affix Forge 3D models branch keeps its Affix Forge work. Its Exotic Affix Forge model becomes the Cradle, the end block of a line, so its silhouette should read as a holder that the line feeds into, with the item visible. Hold that part until the form is accepted.
- The Work Orders PRD changes in two places: Glyphs replace the Exotic Affix Catalyst reward, and the Phase Array, not the Exotic Affix Forge, rejects Work Orders as targets. Work Orders build on this PRD's items; this PRD does not wait for Work Orders.

## Form Alternatives

The hold sequence needs an ordered set of parts that each hold or slip, a charge store, and a place for the item. Three forms were weighed:

| Form | Scales by | For | Against |
|---|---|---|---|
| Straight line, drafted | Adding Coils, 5 to 11 blocks long | Grows one block at a time, lies along a wall, the pulse's travel is the sequence, looks like a machine | Less of a landmark |
| Upright 9x9 ring with nine sockets | Filling sockets only | A landmark, a clear centre for the item | Fixed size, nine tall, reads as a prop from somewhere else |
| Floor grid around a pedestal | Adding rings of parts | Compact footprint | The order of parts is not visible, so the sequence is harder to read |

The line is drafted because it is the only one that grows. The ring is kept here as the alternative if a landmark matters more than growth.

## Decisions

Resolved 2026-10-21:

- The Array replaces the Exotic Affix Forge. Two Stage 8 refinement stations would split one audience.
- The Cradle keeps the `rngtech:exotic_affix_forge` id so alpha worlds load, with a new display name.
- The form is the straight line, for the reasons in Form Alternatives.
- Reforge keeps Strain. A reforge is not a cheap Temper.
- Every stage is accepted, and operations spend a little RP, so RP stays the ceiling.
- Operations are `rngtech:phase_array` recipes.
- Default Glyph sources are the existing challenge loot tables and Dormant Machines. Tier V comes from Work Orders only.
- The Cradle screen follows the Dormant Machine's graphical panel.

## Open Questions

- Whether the pulse always runs from the Emitter to the Cradle, or whether the player can choose which Coils take part, which would let a strong Coil sit where it matters.
- Whether the Fracture check should run before the sequence, as drafted, or replace the first slip, so a fracture is something the player watches happen.
- Whether the line may also be built vertically, as a column under or above the Cradle.
- Whether the challenge table chances are high enough that a player building a line sees Glyphs arrive at a usable rate, or whether `mastery_level` should pay at every ten levels.
- Whether the RP costs should be fixed, as drafted, or rolled in a narrow range like the Affix Forge's.
- Whether a Graft donor must be the same part type as the target, or only share the affix's eligibility.
- Whether the time targets should assume one line charging from supply, or a line plus full Battery Chassis, which changes the `base` values by about four times.
