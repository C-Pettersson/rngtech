# Dormant Machines

Status: Planned

Planned resource ids: `rngtech:dormant_machine`, `rngtech:derelict_husk`

## Summary

Dormant Machines are derelict RNGTech machines placed by world generation. Each one holds a Mastery soul. A player who finds one powers it awake and absorbs its soul with a [Soul Transfer](https://c-pettersson.github.io/rngtech/machine-mastery/#soul-transfer), then releases the soul into a machine of the same family.

## Design Role

Nothing in RNGTech needs the player to leave base today. Dormant Machines reward exploration through Machine Mastery, which already exists, so they add no new currency.

A dormant soul saves Mastery leveling time. No recipe, stage, or Seal needs one, so a player who never explores only levels slower.

Finding the site is the exploration step. Taking the soul is RNGTech work: the player carries or builds power to wake the machine, then absorbs the soul with a well-calibrated Soul Transfer. No vanilla item or structure loot is an ingredient.

The block and the Soul Transfer show the wake cost, success chance, and release requirements before the player commits.

Pack makers can turn generation off, edit the soul tables, choose what a failed absorb does, or place Dormant Machines in their own structures. Server operators can restore spent sites.

## Behavior

### Finding a site

Dormant Machines generate in small RNGTech structures, one machine per site. Each site type sets the families it can roll, a level band, and the lowest machine stage the soul can be released into.

Starting site set, subject to balance testing:

| Site | Where | Families | Soul level | Wake FE | Wake input | Release needs |
|---|---|---|---:|---:|---:|---|
| Collapsed Workshop | Overworld surface: plains, forest, taiga, savanna | Crusher, Furnace, Metal Press | 8-20 | 35,000-100,000 | 2,000 FE/t | Stage 1 |
| Abandoned Cart Siding | Branch of a mineshaft rail line | Forestry Companion | 10-25 | 35,000-100,000 | 2,000 FE/t | Any |
| Buried Calibration Vault | Overworld below Y 0 | Resonance Calibrator | 20-40 | 750,000-3,000,000 | 25,000 FE/t | Stage 4 |
| Flooded Foundry | Ocean floor near ruins | Alloy Furnace, Melter | 25-45 | 1,000,000-4,000,000 | 25,000 FE/t | Stage 4 (Melter Stage 6) |
| Nether Smeltworks | Nether Wastes, Basalt Deltas | Furnace, Alloy Furnace, Melter | 35-55 | 2,500,000-7,500,000 | 50,000 FE/t | Stage 5 |
| End Relay | End outer islands | Crusher, Resonance Calibrator, Melter | 40-70 | 10,000,000-60,000,000 | 250,000 FE/t | Stage 7 |

The Abandoned Cart Siding holds a derelict Forestry Companion entity instead of a block. It follows the same wake and absorb rules.

A site's highest soul level must stay at or below the level that the best work at its release stage can train, under the band rules in [Machine Mastery](../systems/machine-mastery.md#xp-and-chassis-ownership). A soul saves the machine time but never lifts it past what its own work could reach.

### The soul

The soul rolls once, from the world seed and the block position, the first time the chunk loads. Only the [operator commands](#operator-commands) roll it again. It holds:

- XP for a level in the site's band.
- An archetype build for its family, taken from the site's soul table. Nodes are allocated in build order up to the level's points, and the full archetype becomes the followed build.
- One free Full Reset, so the player can clear the archetype without spending Mastery Refunds.
- No ascendancy and no Seal tiers. Seals stay the ascendancy gate.

The block shows the family, level, and release stage before the player commits any power.

### Waking

A Dormant Machine starts asleep. It accepts FE on every face and has nine Battery Cell slots, so the player can either run power to the site or carry charged cells. It drains every installed cell at once, each at its own output rate, the same way a [Battery Chassis](https://c-pettersson.github.io/rngtech/battery-chassis/) adds up its cells. Empty cells stay in their slots until a player or automation takes them out.

Each site type has its own wake FE range. Where a machine falls in that range depends on how high its soul rolled within the site's level band, and the cost climbs faster toward the top of the band:

```text
t       = (soul level - lowest level) / (highest level - lowest level)
wake FE = lowest FE + (highest FE - lowest FE) * t^curve
```

`curve` defaults to `2`. For the End Relay, a level 40 soul needs `10,000,000 FE`, a level 55 soul `22,500,000 FE`, and a level 70 soul `60,000,000 FE`. The top half of the band costs three times as much FE as the bottom half.

Early sites are sized for carried Battery Cells. A Collapsed Workshop or Abandoned Cart Siding takes a handful of Iron or Lead Cells, so a Stage 1 player can wake one without building a generator on site. Later sites cost more than a player can comfortably carry at that stage, so they need a generator at the site or a large bank of high-stage cells.

The wake input rate is set high on purpose, so the machine almost never limits how fast a player can wake it. At its input rate, every site's most expensive soul would wake in under 15 seconds; in practice the attached Energy Connector tier, the generators, or the installed cells' combined output rate set the pace.

Stored wake charge does not decay and survives chunk unloads. The machine wakes when the charge is full and stays awake.

### Absorbing and releasing

An awake Dormant Machine accepts a Soul Transfer like any Mastery machine:

- The absorb success chance is the Soul Transfer's recorded calibration stability, as today.
- A failed absorb always uses up the Soul Transfer. What happens to the machine depends on the pack's `dormantMachines.failedAbsorb` setting, below.
- A successful absorb empties the block, which turns into a Derelict Husk.

| `dormantMachines.failedAbsorb` | What a failed absorb does to the machine |
|---|---|
| `nothing` (default) | The machine stays awake with its soul. The existing `soulTransfer.failureXpLossPercent` and `soulTransfer.failureLevelLoss` settings still apply to the soul; both default to 0. |
| `husk` | The soul is lost and the machine turns into a Derelict Husk. |
| `explode` | The machine explodes with the force of TNT, hurting nearby players and breaking blocks. The soul is lost and a Derelict Husk is left in place. |

The block's screen and Jade line warn about `husk` and `explode` before a player tries, and the Soul Transfer's tooltip shows the risk next to its success chance.

The charged Soul Transfer records the site's release stage. It refuses to release into a machine below that stage and says why, just as a soul with an ascendancy refuses a machine below Stage 4. The other release rules, same family and overwrite confirmation, stay as they are.

### Derelict Husk

While a Dormant Machine holds a soul it cannot be broken in Survival, so the soul can only leave through a Soul Transfer. The husk left behind breaks with a pickaxe and drops Scrap. It marks the site as spent and is not meant as loot.

## Screen

A Dormant Machine is not a machine the player built, so its screen breaks from the gray panels and plain slot frames in [Machine GUI Design](../reference/machine-gui-design.md#visual-style). It should look like old, heavy equipment with something not quite mechanical still alive inside it.

### What it keeps from Machine GUI Design

- The left-to-right flow: power in on the left, the wake meter in the center, the soul on the right.
- The player inventory and hotbar at the bottom in the standard positions, so shift-click and inventory mods work.
- Exact values in tooltips, compact FE units, and no permanent text labels on the meters.
- Status icons attached to the wake meter, with shapes as well as colors so no state relies on color alone.

### What it changes

- **Panel:** dark gunmetal plate with a riveted bezel, worn edges, and faint circuit traces etched into it. No gray fill and no tab bar, since the block has no Gear, Stats, or Refine.
- **Title:** the family icon and site name stamped on a small engraved plate in the top-left corner.
- **Cell bank (left):** the nine Battery Cell slots are a 3 × 3 bank of recessed sockets instead of slot frames. Each socket has a thin ring that shows that cell's charge and dims when the cell is empty. An FE input jack above the bank shows live FE/t with a small needle gauge.
- **Wake meter (center):** a horizontal glass conduit that fills left to right with flowing light as the charge rises. The soul's level sits under it in glowing segmented digits. The status lights for no power, input capped by a connector, and awake sit directly above it.
- **Soul chamber (right):** a glass containment vessel holding the soul as a slow, pale wisp. Hovering it shows the family, level, archetype build, release stage, and remaining wake FE. The release stage shows as a row of stage pips under the vessel.
- **Failed-absorb risk:** when the pack sets `husk` or `explode`, a hazard plate is riveted beside the soul chamber, with the outcome in its tooltip.
- **Inventory area:** the same dark sockets as the cell bank, in the vanilla layout.

### The magic touch

The magic stays in the details so the screen still reads as tech:

- A ring of faint glyphs is etched around the soul chamber. They light one by one as the charge rises, and the ring closes when the machine wakes.
- The wisp is dim and nearly still while the machine sleeps. It drifts faster and brightens as charge builds, and pulses steadily once awake.
- On waking, the glyph ring flares once, a relay clunks, and a soft chime plays.
- Soul light is pale cyan-white. It never uses purple or magenta, which stay reserved for a future Legendary rarity.

### States

| State | Look |
|---|---|
| Asleep, no charge | Panel cold and dim. Conduit empty, glyphs dark, wisp barely visible. |
| Waking | Circuit traces near the cell bank light up, the conduit flows, and glyphs light in step with the charge. |
| Awake | Conduit full and pulsing, glyph ring closed, wisp bright and steady. |

A Derelict Husk has no screen.

### Sketch

```text
┌───────────────────────────────────────────────┐
│ [icon] COLLAPSED WORKSHOP            ⚠ (pack)  │
│                                               │
│  ⚡ FE/t ◔      ▲ ■ ●  (status)      ╭───────╮  │
│  ◯ ◯ ◯                              │ ·:˙·  │  │
│  ◯ ◯ ◯     ══════▶════════          │ wisp  │  │
│  ◯ ◯ ◯        ▯▯ level              ╰───────╯  │
│  cell bank      wake conduit       ▪▪▪▫▫ stage │
│                                               │
│  ◯◯◯◯◯◯◯◯◯  player inventory                  │
│  ◯◯◯◯◯◯◯◯◯                                    │
│  ◯◯◯◯◯◯◯◯◯                                    │
│  ◯◯◯◯◯◯◯◯◯  hotbar                            │
└───────────────────────────────────────────────┘
```

## Multiplayer

Dormant Machines have no owner and no claim. On a server, any player can add power to any machine and any player can absorb an awake soul.

- The wake charge belongs to the machine, not to a player. FE from every cable, cell, and player adds to the same charge.
- Whoever absorbs first takes the soul. If one player wakes a machine from 0% to 70% and leaves, a second player can add the last 30% and absorb it. Stealing a half-woken machine is part of the game, and protecting one is up to the players: guard it, finish it in one trip, or bring enough charged cells.
- Every machine has one soul for the whole server. An absorb turns the block into a Derelict Husk for everyone. Souls are not rolled per player.
- A charged Soul Transfer is an ordinary item. Players can trade or give it away, and anyone holding it can release it under the normal release rules.
- The derelict Forestry Companion at an Abandoned Cart Siding follows the same rules.

Server operators can reroll souls or bring spent sites back with the [operator commands](#operator-commands).

### Operator commands

Both commands act on every Dormant Machine site in the world and need permission level 2.

| Command | Sites it touches | Result |
|---|---|---|
| `/rngtech dormant reset` | Untouched machines: still holding a soul, never charged | A new random soul from the site's soul table. |
| `/rngtech dormant reset force` | Every machine still holding a soul, charged or not | A new random soul, and the stored charge drops to 0. |
| `/rngtech dormant restore same` | Every site, husks included | The soul the site last rolled, before any failed absorb reduced it. |
| `/rngtech dormant restore random` | Every site, husks included | A new random soul. |

`reset` is for refreshing souls after a soul table change without touching anything players have started on. It never brings back a husk. `restore` puts spent sites back in play.

A restored site starts asleep with no charge, including machines that were partly awake. Sites in unloaded chunks reset or restore the next time their chunk loads. Sites in unloaded chunks reset the next time their chunk loads. A site whose husk was mined out is restored only when its position is empty; if a player has built there, the command leaves it alone and lists it as skipped.

## Implementation Contract

Planned runtime surface:

- Block: `rngtech:dormant_machine` with a `family` block state for the model; `rngtech:derelict_husk` once spent. No block items in creative tabs outside dev; pack makers place them by command or structure.
- Block entity: `DormantMachineBlockEntity` owning the soul table id, the lazily rolled `MachineProgressionState`, family, release stage, wake charge, nine Battery Cell slots, and the site generation it last synced to.
- Site index: world saved data listing every generated site with its dimension, position, site type, state (untouched, charged, or spent), soul salt, and a generation counter. The block entity updates the state when it first takes charge and when it is spent, so the commands can pick sites in unloaded chunks. The soul seed is world seed, position, and salt.
- Commands: `reset` and `restore random` change the salt of each site they touch; `restore same` keeps it. All of them bump the site's generation. A block entity whose generation is behind its index entry rerolls on load, and a chunk-load listener restores mined-out sites in that chunk when their position is air.
- Command output: how many sites changed now, how many wait on chunk load, and which were skipped.
- Failed absorb: `dormantMachines.failedAbsorb` read by the absorb path; `explode` uses a block-sourced TNT-strength explosion and then sets the husk.
- Menu and screen: `DormantMachineMenu` and `DormantMachineScreen`, drawn from a dedicated `textures/gui/dormant_machine.png` sheet as described in [Screen](#screen). Animations run client-side from the synced charge fraction, awake flag, and failed-absorb mode.
- Capabilities: block FE input on every face until awake; Battery Cell insertion from the top and sides; extraction of empty cells only, from the bottom.
- Jade: the block implements `MachineInfoProvider` for family, level, release stage, and wake progress.
- Soul Transfer: `SoulTransferItem` absorbs from an awake Dormant Machine. `SoulTransfer.Charge` gains an optional minimum release stage, defaulting to none so existing charged Soul Transfers keep working.
- Forestry: a derelict Forestry Companion entity variant with a wake charge instead of a block.
- Data: soul tables at `data/rngtech/dormant_machine/<site>.json` (family weights, level range, wake FE range, wake curve exponent, wake input rate, release stage, weighted archetype build codes). Structures, structure sets, and template pools under `data/rngtech/worldgen/`, with templates under `data/rngtech/structure/`. These are the mod's first generated structures.
- Config: `dormantMachines.enabled` (default on) gates the structure sets through a load condition, like `OreWorldgenEnabledCondition`. `dormantMachines.failedAbsorb` (`nothing`, `husk`, or `explode`; default `nothing`) sets the failed-absorb outcome.

Implementation checks:

- GameTests: wake by FE, by one Battery Cell, and by several cells draining together at their summed output rate, empty cells extractable from below and charged cells not, absorb refused while asleep, absorb success empties the block into a husk, failure applies the configured loss, release refused below the recorded stage, the soul rolls the same from the same seed and position.
- Failed-absorb GameTests: one per `failedAbsorb` mode, checking the soul, the block left behind, and that `explode` leaves a husk.
- Multiplayer GameTests: two players adding charge to one machine, and a player who added no charge absorbing it.
- Reset GameTests: `reset` rerolls an untouched machine and leaves a charged machine and a husk alone; `reset force` rerolls the charged machine and clears its charge.
- Restore GameTests: `same` returns the last rolled soul after a failed absorb reduced it, `random` changes it, partly woken machines reset to no charge, an unloaded site resets on chunk load, and an occupied mined-out position is skipped.
- `masteryCheck`: every archetype build code parses, stays legal for its family, and fits the 99 level points.
- An audit that each site's top level stays within the training reach of its release stage, and that each wake FE range is ordered with a curve exponent of at least `1`.
- `npm run moddex:check` covers the soul tables if the data shape needs validation.

## Modifier Eligibility

Available modifiers: none. Dormant Machines carry no rarity, affixes, or Gear, and reject refinement.

| Modifier Source | Eligible? | Notes |
|---|---|---|
| Implicit | No | Family identity comes from the block state. |
| Prefix | No | None. |
| Suffix | No | None. |
| Enchant | Out of scope | None. |

## Base Stats

Status: Planned

Starting values, subject to balance testing.

| Stat or value | Base | Notes |
|---|---:|---|
| Wake charge | Per site, see [Finding a site](#finding-a-site) | Placed in the site's range by soul level on the [wake curve](#waking), exponent `2`. |
| Wake input rate | Per site, see [Finding a site](#finding-a-site) | Total across all faces and the cell slots. Limited further by the attached connector tier. |
| Battery Cell slots | `9` | Every installed cell drains at once. |
| Battery Cell drain | Sum of cell output rates | Each cell drains at its own output rate; the total is capped by the wake input rate. |
| Souls per site | `1` | One Dormant Machine per structure. |

## Open Questions

- Whether the End Relay or Nether Smeltworks should carry a Seal I tier with an ascendancy already chosen. The default keeps Seals as the only ascendancy source.
- Whether waking should draw hostile mobs, such as a wave once the charge passes half. That would turn the expedition into a fight, but it is an automatic cost the player cannot shape with Gear or Mastery.

## Related Pages

- [Machine Mastery](../systems/machine-mastery.md)
- [Soul Transfer](https://c-pettersson.github.io/rngtech/machine-mastery/#soul-transfer)
- [Resonance Calibrator](https://c-pettersson.github.io/rngtech/resonance-calibrator/)
- [Modpack Creators](https://c-pettersson.github.io/rngtech/modpack-creators/)
- [Current Implementation Matrix](../reference/current-implementation.md)
