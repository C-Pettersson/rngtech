# Shared Affixes

Status: Planned

Proposed affixes that several machine families can share, to widen pools where a Rare almost always rolls the same lines. They come from a pool study of `main` at 672b9577 (2026-10-28). Roll mechanics, weights, and tier tables are defined in [Affix Generation](../systems/affix-generation.md); capability flags in [Modifier Eligibility](../reference/modifier-eligibility.md).

Six lines from the study have shipped: Fluid Capacity on fluid processors and generators (Deep Tanks), Twinned, Studious, Glow, Discreet, and Telemetry. The cheapest dilution knob shipped with them: generic processing speed rolls at weight `75` wherever a named speed exists. See [Affix Generation](../systems/affix-generation.md#glow-discreet-and-telemetry) and the [Rarity and Affixes](https://c-pettersson.github.io/rngtech/rarity-and-affixes/) wiki page.

## Why the pools need widening

A Rare rolls three prefixes and three suffixes, each from the groups not already on the item. A slot with three or fewer groups is therefore fixed on every Rare; only the tiers vary. At 672b9577:

- 23 slot pools had three or fewer groups. They include every generator suffix pool (Bio Generator, Solar Panel, Potential Reactor, Vacuum Collapse Generator) and the parts with one prefix: Void Chamber, Collapse Nozzle, Dimensional Stabilizer, both Catalyst Beds, and the Solar Array Extender. Cathode, Fuel Cell Membrane, and the Component Assembler had two.
- Eight generators shared the same four prefixes: flat and increased energy capacity, flat energy generation, and efficiency.
- The `processing_speed` group (generic speed `100`, named speed `75`, Overclocked `25`, Bulk Speed `25`) landed on 53-100% of processing Rares. Lowering generic speed to `75` where a named speed exists is the cheapest dilution knob.
- Some wide pools are one stat under several names: all eight Containment Lining groups are Stability, and eight of the Fluid Pump's thirteen are Fluid Transfer.

Shared lines alone cannot fix a part with one prefix: three additions still leave the same set on most Rares. Those parts also need prefixes authored from stats they already read.

## Rules every proposal follows

- Part affixes stay local, except the machine-wide Mastery stats (attributes and Mastery XP gain).
- No hidden costs. A cost sits on the affix tooltip as a visible condition or trade.
- Quality-of-life lines need no downside.
- Status cues are diegetic: the machine itself shows the effect.
- No autocrafting and no roll-until-X automation.
- No line feeds the refinement economy directly: no Refinement Potential over time, no currency drops, no extra recycler returns.

## Filler lines

Weak, visible, mildly useful in one niche, in the spirit of Path of Exile's Light Radius.

Glow, Discreet, and Telemetry from this tier have shipped.

### Hearth

| Field | Value |
|---|---|
| Slot and group | Prefix, `ambient_warmth` |
| Rolls on | Furnace, electric Furnace, Alloy Furnace, Metal Press, Melter, Coal Gasifier, Steam Methane Reformer, Solid Fuel Burner, Syngas Combustor |
| Tiers | Radius 2, 3, 5, 7 |
| Weight | `60` |
| Hook | New. A throttled server check every 40 ticks removes snow layers and powder snow in the cube and resets freeze ticks on players inside it. Never touches ice, so it cannot flood a base. |

### Tutelage

| Field | Value |
|---|---|
| Slot and group | Suffix, `vanilla_experience` |
| Rolls on | Item-output processors: Crusher, Furnace, electric Furnace, Alloy Furnace, Metal Press, Component Recycler, Component Assembler, Coal Gasifier, Resonance Calibrator |
| Tiers | 0.05, 0.1, 0.15, 0.2, 0.3 vanilla experience per craft (a vanilla iron smelt gives 0.7) |
| Weight | `60` |
| Hook | New. The mod spawns no experience orbs today. Store a float on the block entity and pay it out when a player takes the output, as vanilla furnaces do. Count only crafts with authored `machine_xp` or recipe experience, so loops earn nothing. |

### Warded

| Field | Value |
|---|---|
| Slot and group | Prefix, `blight_ward` |
| Rolls on | Every Corruption host |
| Tiers | 15%, 30%, 45% reduced Blighted outcome weight |
| Weight | `55` |
| Hook | Partial. `CorruptionCatalog` holds per-outcome weights; scale Blighted by the item's stat when the outcome is rolled. |

It matters once, at corruption, and stays well under the Stabilization Crystal, which removes Blighted entirely. It is the only proposed line that reaches the one-prefix parts.

## Mid lines

### Surge

| Field | Value |
|---|---|
| Slot and group | Prefix, `low_buffer_surge` |
| Rolls on | Generators with a buffer: Bio Generator, Solar Panel, Solar Array Controller, Potential Reactor, Corrosion Cell, Vacuum Collapse Generator, Syngas Combustor, Cavitation Generator, Fuel Cell |
| Tiers | 10-15%, 16-25%, 26-40%, 45-60% increased Energy Generation while the buffer is under 25% full |
| Weight | `75` |
| Hook | New. Each generator computes generation on its own; add one shared helper that applies the bonus from buffer fill. |

It works against Reserve Bank and increased Energy Capacity, so a player picks a hungry small-buffer generator or a deep one. Together with the shipped Discreet prefix it drops each shared generator prefix from 75% to about 54% of Rares.

### Backlog

| Field | Value |
|---|---|
| Slot and group | Prefix, `output_backlog` |
| Rolls on | Item-output processors except the Crusher: Furnace, electric Furnace, Alloy Furnace, Metal Press, Component Recycler, Component Assembler, Coal Gasifier, Resonance Calibrator |
| Tiers | Holds 1, 2, 4, 8 finished crafts while the output is full |
| Weight | `65` |
| Hook | Partial. `OutputLedger` already banks fractional output until the slot has room on the Furnace, Alloy Furnace, and Metal Press; bank whole crafts the same way. The Crusher keeps Crusher Frame and Output Guard. |

### Discerning Feed

| Field | Value |
|---|---|
| Slot and group | Prefix, `feed_control` |
| Rolls on | Item processors except the Crusher: Furnace, electric Furnace, Alloy Furnace, Metal Press, Melter, Component Recycler, Component Assembler, Resonance Calibrator |
| Tiers | 1: refuse automation inserts when the output cannot take one result. 2: also refuse inputs that would conflict with banked bonus output. |
| Weight | `60` |
| Hook | Partial. The predicate exists for the Crusher (`CRUSHER_INPUT_FILTER`); move it to a shared helper. Check first which machines already refuse non-recipe items. |

## Chase lines

### Anointed

| Field | Value |
|---|---|
| Slot and group | Suffix, `anointment` |
| Rolls on | Mastery machines and their Gear |
| Value | Untiered: one Notable from the host family's tree, chosen at roll time. Never a Keystone or an ascendancy node. |
| Weight | `8` |
| Hook | New. The tree resolves allocated node ids into effects; add a second source of node ids from installed modifiers, with no point cost or adjacency. |

Path of Exile's "Allocates" anoint. Notables carry no penalties, so it adds none to the tree. Needs its own design pass before work starts.

## Rejected

| Idea | Reason |
|---|---|
| Scrap Value (more Component Recycler returns) | Too dangerous: it feeds refinement supply directly. |
| Seasoned (Refinement Potential earned per craft) | Too risky to balance. |
| Prospector (chance of refinement currency per craft, using the unread `MAGIC_FIND` stat) | Too risky to balance; Work Orders are the designed currency source. |
| Fluid Capacity on Fluid and Compressor Tanks | Storage capacity is authored identity ([Machine Guidelines](../reference/machine-guidelines.md#fluid-and-compressor-tanks)). |
| Reduced stage requirement on Gear | Skips the stage gate. Corruption already bars `UPGRADE_LIMIT` from 2.0 pools for this reason. |
| Idle drain | Machines draw no FE while idle, so a reduction has nothing to reduce, and adding drain is a hidden cost. |
| Spawn-suppression radius | The shipped Glow line already blocks spawns where it lights, and shows why. |
| Redstone on/off as an affix | Basic control behind a roll reads as a tax. Better as a default. |
| Silence on Mastery machines | The Silent Operation keystone owns it. |

## Related cleanup

Found during the study; not shared affixes, but they decide how wide a pool really is:

- About 60 named `runtime()` affixes (flag `NON_TARGETABLE`) have mechanic-style group names, but nothing reads their id or group; their only effect is the generic stat, and their `.extra` tooltip lines describe behavior that does not exist. Implementing those names (Check Valve, Vapor-Lock Breaker, Cinder Return, Exhaust Port, Pressure Curtain) is the cheapest source of per-family niche lines. Otherwise merge same-stat siblings into one group.
- Exact duplicates that stack: High Head and Fluid Transfer, Leak Quencher and Idle Loss, Low Drag and Processing Speed (outside the `processing_speed` group), Charge Backplane and Energy Transfer.
- Heat Soak is reduced `COOLING_RATE`, which `HeatEngine` reads only for active overheat shedding, so it may slow overheat recovery instead of keeping heat in.
- The `QUICK_FEED` and `THERMAL_BUFFER` implicit behaviors are granted but never read.
- The Forestry Companion has no prefixes, and the Miner's Companion's Rare equals its Magic.
