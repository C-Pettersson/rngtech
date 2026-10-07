# Unique Item Ideas

Status: Planned

This page collects candidate Unique Gear and Battery Cells. Only the Voltaic Potato Battery Cell (`rngtech:unique_potato_battery_cell`) is implemented. The [Unique Items PRD](../prds/uniques.md) defines the catalog, sources, and launch set. Entries here are not current gameplay until that PRD or a later task promotes them.

A strong Unique comes from one exact place in the world, solves one odd build problem, and carries one real drawback. It is a sidegrade or a build-around piece, not a higher stage.

## Design Rules

- Uniques use `UNIQUE` rarity and 0 Refinement Potential. Their stat lines are fixed or ranged, and each copy rolls its ranged lines when identified, so copies can be well or poorly rolled. Uniques cannot be crafted, refined, or recycled. [Corruption](../prds/corruption.md) is the only way to change one.
- Each Unique targets one Gear slot type and names a slot stage, which drives stage gates such as Crush Head ≤ chassis stage and the Metal Press and Melter Heat Core cap of 6.
- Every stat on a Unique, including its drawback, must be read by at least one host it fits. Check [Machine Stats](machine-stats.md) and the host's block entity before using a stat. For example, `STABILITY` is not read by the Solid Fuel Burner, Battery Chassis, or Component Recycler, and `OUTPUT_AMOUNT` is read only by the Crusher and Cavitation Generator.
- A Unique should have one signature: a stat no normal part of its type provides, a behavior normal parts get only at a later stage, or a new `MachineBehavior`.
- A Unique may grant an ascendancy stat as a build-around hook. Its tooltip names the ascendancy it needs.
- No yield stats in 2.0. A Unique that adds Output Amount, Super Output, salvage, Fluid Yield, Ledger Rate, or residue needs loop-audit coverage first (see the [ascendancy loop rules](../prds/machine-ascendancies.md#loop-prevention)).
- Uniques must not skip the stage ladder. At their best roll, recipe-gating stats such as Processing Level or input-slot count may reach at most one stage past the slot stage.
- A drawback must still be a penalty at its best roll, and a signature must still be a benefit at its worst roll.

## Launch Set

These eight are specified, with their ranges, in the [Unique Items PRD](../prds/uniques.md#launch-uniques).

| Unique | Host | Slot stage | Source | Signature | Drawback |
|---|---|---:|---|---|---|
| Fortress Heater Element | Heat Core | 4 | Nether fortress chest | Titanium-class Maximum Temperature, 75% less Warmup Time, Overdrive Margin hook | More Energy Usage, less Temperature Stability, poor Solid Fuel Burner fuel efficiency |
| Igloo Basement Thermostat | Heat Core | 2 | Igloo basement chest | Top Temperature Stability and Overheat Tolerance, early `POWER_GRACE`, Heat Window hook | Bronze-class Maximum Temperature cap, less Heat Transfer |
| Mineshaft Worn Pick-Jaw | Crush Head | 2 | Mineshaft minecart chest | +1 Processing Level over slot stage, +1 to +3 Batch Size, Jam Recovery hook | Jam Chance on every cycle, less Output Amount |
| Crying Crucible | Alloy Crucible | 3 | Ruined portal chest | Blend Speed and Blend Heat Reduction without Blendwright | Less Stability and Temperature Stability |
| Trial Vault Escapement | Servo | 6 | Ominous trial vault | `ESCAPEMENT` first-cycle speed, Mold Swap Time hook | Slower sustained cycles, no `POWER_GRACE` |
| Witch-Bottle Reflux Pump | Fluid Pump | 5 | Witch drop | Doubled host Fluid Capacity, `REFLUX` cheaper Sprinkler | Much less Fluid Transfer |
| Ancient Echo Control Board | Control Board | 6 | Ancient city chest | Nullite-class Calibration Precision, `ECHO_STREAK` | Less Processing Speed, no Refinement Potential Bonus |
| Bastion Coin-Stack Capacitor | Battery Cell | 4 | Bastion treasure chest | Doubled Burst Transfer and Duration in burst-capable chassis | High Idle Loss, weak steady output |

## Backlog

Redesigned 2026-10-04 against current stats and Gear slots. Each entry lists what it needs beyond the Unique catalog.

| Unique | Host | Slot stage | Source | Signature | Drawback | Needs |
|---|---|---:|---|---|---|---|
| Blaze Cage Igniter | Fuel Box | 3 | Nether fortress chest | Burns blaze rods and blaze powder at very high Fuel Efficiency and Energy Generation | Rejects every other fuel, one fuel slot | A per-box fuel whitelist in `SolidFuelBurnerFuelRules`. A check that no recipe turns FE into blaze fuel. |
| Shipwreck Salt-Zinc Anode | Battery Cell | 1 | Shipwreck supply chest | Lead-class capacity at Stage 1 | High Idle Loss, low Efficiency, low output rate | A `BatteryCellMaterial` entry only. |
| End City Phase-Latched Cell | Battery Cell | 7 | End city treasure | Exotic-class capacity and input transfer | Output rate stays low unless the host has `BURST_RELEASE` | A new `PHASE_LATCH` behavior in Battery Chassis output. |
| Jungle Tripwire Governor | Servo | 4 | Jungle temple chest | `POWER_GRACE` and high Overheat Tolerance two stages early | Less Processing Speed, and it disables the host's `BULK_SPEED` | A behavior that suppresses `BULK_SPEED`. |
| Buried Anchor Coil | Resonance Coil | 4 | Buried treasure chest | More Calibration Quality on Structural and Storage patterns, Coil Reach hook (Mass Tuner) | Less Calibration Quality on Conductive and Kinetic patterns | A `PATTERN_AFFINITY` behavior keyed to Calibration Pattern family. |
| Trail-Sherd Calibration Shim | Stabilizer Matrix | 2 | Trail ruins rare archaeology | High Catalyst Efficiency and Calibration Quality on Stage 4 or lower outputs | Less Calibration Quality on Stage 5+ outputs | An output-stage-aware matrix behavior. The old "failed upgrade recovery" was dropped because upgrade outcomes do not exist. |
| Stronghold Marginalia Board | Control Board | 6 | Stronghold library chest | Refinement Potential Bonus on Stage 6+ outputs | Low Stability and Calibration Precision on lower outputs | A review of the Refinement Potential economy before any Unique raises RP. |
| Prismarine Quench Lining | Containment Lining | 3 | Elder guardian drop | Very high Potential Reactor Stability | Less Energy Generation | An entity loot modifier. "Residue safety" was dropped, because `RESIDUE_SCREEN` belongs to Recovery Filters. |
| Trial Vault Tempered Pawl | Disassembly Head | 3 | Trial chambers normal vault | Stage 5 Processing Level reach on the Component Recycler | More Energy Usage, less Processing Speed | Nothing new. The old `OUTPUT_AMOUNT` drawback was dropped, because the recycler does not read it. |
| Dungeon Ash Filter | Recovery Filter | 1 | Dungeon chest | Strong residue recovery from low-stage salvage | Slower reactor processing | Loop-audit and recycling-return coverage, because residue is yield. |
| Mansion Heartwood Splitter | Axe or Treefeller head | 5 | Woodland mansion chest | Very high Tree Fell Limit for Timber Baron carts | Low Durability, high FE Usage | Unique support in `ToolBaseStatCatalog` and the Tool Bench. |

## Retired and Reworked

| Old candidate | Change | Reason |
|---|---|---|
| Mineshaft Worn Commutator (Servo) | Became the Mineshaft Worn Pick-Jaw (Crush Head) | Servos start at Stage 4, so no early Servo can exist. Mining also suits a Crush Head. |
| Mansion Escapement Servo | Became the Trial Vault Escapement | Trial chambers fit a clockwork part. The mansion now hosts the Heartwood Splitter. |
| Witch-Bottle Reflux Pump (lubricant handling) | Became a capacity and Sprinkler pump | Lubricant handling does not exist, and the Metal Press has no pump slot. |
| Ruined Portal Crying Regulator (Stabilizer Matrix) | Became the Crying Crucible | Calibration has no heat, so overheat protection had nothing to protect. |
| Blaze Cage Igniter `STABILITY` drawback | Replaced with a fuel whitelist and one fuel slot | The Solid Fuel Burner does not read `STABILITY`. |
| Ancient Echo Control Board "no catalyst preservation" | Replaced with no Refinement Potential Bonus | Catalyst preservation is `CATALYST_EFFICIENCY`, which belongs to the Stabilizer Matrix. |

## Source Guidelines

Use sources that make the item's identity obvious:

| Source type | Good fit | Avoid |
|---|---|---|
| Structure chest | Nether fortress heat parts, stronghold control boards, shipwreck corroded cells | Generic "any dungeon chest" for a top-tier item |
| Archaeology | Ceramic shims, old calibration gauges, damaged prototype parts | Broad material-stage replacements |
| Boss or entity drop | Monument containment, witch brews, endgame phase storage | Mandatory progression drops for ordinary machines |
| Trial or vault reward | Stop-start servos, burst parts, risky high-value sidegrades | Required crafting ingredients for baseline chassis |

Vanilla sources are the default. Every default Unique loot table carries the `rngtech:unique_loot_enabled` condition, so pack makers can switch any Unique off and move it to an RNGTech [challenge loot table](../prds/uniques.md#challenge-loot), a quest reward, or their own loot. See [Pack configuration](../prds/uniques.md#pack-configuration).

## Promotion Checklist

Before moving an idea into the Unique catalog, define:

- Final name, resource id, host type, and slot stage.
- Base profile, fixed lines, and ranged lines with worst and best rolls, with each stat confirmed as read by at least one host.
- The signature, and any new `MachineBehavior` with its host code.
- The drawback, confirmed as read by the hosts.
- Any ascendancy hook and the ascendancy it needs.
- Exact loot table, chance, and loot modifier.
- Tooltip identity, signature, drawback, and source text.
- Corruption pool coverage for its host type.
- Manual tests for installation, rejection from incompatible slots, stat display, refinement rejection, and corruption.

## Related Pages

- [Unique Items PRD](../prds/uniques.md)
- [Corruption PRD](../prds/corruption.md)
- [Rarity](rarity.md)
- [Modifier Eligibility](modifier-eligibility.md)
- [Machine Parts](../content/machine-parts.md)
- [Battery Cells](../content/battery-cells.md)
- [Crafting and Upgrades](../systems/crafting.md#unique-components)
