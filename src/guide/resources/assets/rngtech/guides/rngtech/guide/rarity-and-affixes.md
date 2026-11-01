---
navigation:
  title: "Rarity and Affixes"
  parent: guides.md
  icon: rngtech:affix_injector
categories:
  - "Guides"
---

# Rarity and Affixes

RNGTech treats machines like loot. Every machine, part, and Battery Cell you craft rolls its own **rarity**, a set of random **affixes**, and a budget of **Refinement Potential** (RP). Two Crushers from the same recipe can come out quite different: one might be faster, another might use less power, and a lucky one might do both.

This page explains what those rolls mean, how many affixes each rarity gets, how strong they can be, and how to improve them afterwards.

## Rarity

Rarity sets how many affixes an item can hold.

| Rarity | Prefixes | Suffixes | Notes |
|---|---:|---:|---|
| **Normal** | 0 | 0 | Base stats only. |
| **Magic** | up to 1 | up to 1 | Rolls one or two affixes when crafted. Named after its affixes, for example "Hellish Iron Furnace Chassis of Smelting". |
| **Rare** | up to 3 | up to 3 | Rolls three to six affixes when crafted. The name uses its first prefix and first suffix. |
| **Unique** | — | — | Found, not crafted. Rolled stat lines, no RP, and only a [Volatile Catalyst](rngtech:rarity-and-affixes.md#corruption) can change it. |

Each affix goes to a random side that still has room. A Rare rolls 3 affixes 35% of the time, 4 affixes 35% of the time, 5 affixes 20% of the time, and 6 affixes 10% of the time. Most crafted items therefore have open slots. Use an [Affix Injector](rngtech:affix-forge/catalysts.md) to fill them with your RP. An item rolls fewer affixes if not enough different affixes fit it.

[Uniques](rngtech:uniques.md) have their own page: where to find them, how their ranged stats roll, and what each one does.

- [Rarity odds](rngtech:rarity-and-affixes/rarity-odds.md)

## Affixes

An affix is a random bonus, such as faster processing, a bigger energy buffer, or a chance for extra output.

- **Prefixes** come before the item name and lean towards capacity, heat, efficiency, and machine-specific tricks.
- **Suffixes** come after the name and lean towards speed, energy use, generation, output, and slots.

An item can only roll affixes that its machine actually uses, so a Solar Panel never rolls Crusher bonuses. An item also never holds two affixes from the same group. For example, a Crusher can have only one processing-speed suffix: plain speed, a named one like "of Crushing", Overclocked, or Bulk Speed. Narrow items may have fewer legal affixes than the rarity allows.

- [Base stats and fixed traits](rngtech:rarity-and-affixes/base-stats-and-fixed-traits.md)
- [Reading the tooltip](rngtech:rarity-and-affixes/reading-the-tooltip.md)
- [Affix tiers](rngtech:rarity-and-affixes/affix-tiers.md)
- [Machine and part affixes](rngtech:rarity-and-affixes/machine-and-part-affixes.md)
- [Attribute suffixes](rngtech:rarity-and-affixes/attribute-suffixes.md)

## Notable affixes

Most affixes simply raise or lower a [stat](rngtech:machine-stats.md). A few change how the machine behaves.

- [Bulk Speed](rngtech:rarity-and-affixes/bulk-speed.md)

### Balance Mode

A Battery Chassis prefix, shown as **Balanced**. The chassis fills and drains all installed Battery Cells evenly instead of one at a time. It gives chassis without a built-in Charge Balancer the same behavior. See [Battery Chassis](rngtech:battery-chassis.md).

### Fuel Governor

A fixed trait of the Copper, Bronze, and Steel Fuel Boxes in a [Solid Fuel Burner](rngtech:solid-fuel-burner.md). While the burner's storage is full, it pauses the fuel burn instead of wasting it, and keeps exporting stored FE. It is not a rolled affix.

### Power Grace

A suffix that halves the failure strain a heat machine takes when its power drops mid-cycle. It can roll on electric Furnaces, Alloy Furnaces, Heat Cores, Alloy Crucibles, and Servos, and Stage 6 and higher Servos have it built in. See [Furnace](rngtech:furnace.md) for how failure strain works.

- [Glow, Discreet, and Telemetry](rngtech:rarity-and-affixes/glow-discreet-and-telemetry.md)
- [Other named affixes](rngtech:rarity-and-affixes/other-named-affixes.md)

## Refinement Potential

**Refinement Potential** (RP) is each item's budget for changing its affixes. Every successful refinement spends some. When RP runs out, the item is final, so spend it where it matters. The only way back is a Reforge in the [Phase Array](rngtech:phase-array.md), which rerolls the whole item, RP included. A [Corrupted](rngtech:rarity-and-affixes.md#corruption) item keeps its RP but can never spend it, so its RP is hidden.

RP is rolled separately from rarity, and the starting affixes cost nothing. A Rare item keeps its full budget, just like a Normal one. Higher stages get bigger budgets:

| Item stage | Starting RP |
|---:|---:|
| 0 | 1–3 |
| 1–2 | 6–10 |
| 3–4 | 8–14 |
| 5–6 | 14–22 |
| 7–8 | 22–32 |

Tool Heads and Tool Rods get 2 extra RP, and some machine parts add a small bonus of their own. The tooltip of an unidentified part shows the range it can roll.

An illegal or failed refinement costs nothing: no catalyst and no RP. If an add or upgrade rolls a cost higher than the RP left, it still succeeds, and the item drops to 0 RP.

- [Strain and Quality](rngtech:rarity-and-affixes/strain-and-quality.md)
- [Annealed items](rngtech:rarity-and-affixes/annealed-items.md)

## Corruption

A <ItemLink id="rngtech:volatile_catalyst" /> corrupts a finished machine part or Battery Cell in the [Affix Forge](rngtech:affix-forge/corrupting-with-a-volatile-catalyst.md), or a placed machine in its Refinement tab. It costs no RP, and it is the only thing that can change a Unique. Machine and chassis items, Tool Heads, Tool Rods, companions, and unidentified or stripped items cannot be corrupted as items; place a machine first and corrupt it from its Refinement tab. You can corrupt a [Job Chip](rngtech:work-orders/refining-and-corrupting-chips.md) in the Affix Forge. There, the outcomes change the job's reward instead of a stat.

The catalyst rolls one outcome, and every outcome leaves the item **Corrupted**:

| Outcome | Chance | Effect |
|---|---:|---|
| **Untouched** | 20% | Nothing else changes. |
| **Blessed** | 20% | Adds one positive corruption implicit. |
| **Reforged** | 20% | Rerolls every rolled affix from the item's legal pools, like a Chaos Crystal. Rarity, RP, base stats, and fixed traits stay. |
| **Warped** | 20% | Each rolled affix moves by its own random amount from −15% to +15% of its value, past its tier's range. 100% increased Processing Speed can become anything from 85% to 115%. Tiers stay, and yield and RP affixes are not changed. |
| **Blighted** | 20% | Adds one negative corruption implicit. |

On a Unique, Reforged rerolls every ranged stat line inside its range, and Blessed or Blighted draw from the pool of the Unique's part. An outcome with nothing to do becomes Untouched: Reforged or Warped on an item without affixes it can change, Reforged on a Unique with no ranged lines, Warped on any Unique, and Blessed or Blighted on an item with no implicit to draw. A Stabilization Crystal in the Affix Forge's focus slot removes Blighted, adds its chance to Untouched, and is used up. Pack makers can change these chances and the Warped range.

A Warped affix uses one roll for all of its effects, so a drawback on a two-stat affix grows or shrinks with its benefit. Hold Left Alt to compare a Warped affix with its tier's normal range.

A **Corrupted** item can never be refined again: the Affix Forge, Phase Array, Tool Bench, Refinement tabs, and a second Volatile Catalyst all refuse it, and nothing is used. Corruption cannot be removed. Nothing is ever destroyed or loses rarity; the worst result is a Blighted implicit. A placed machine keeps its corruption when you break and place it again.

- [Corruption implicits](rngtech:rarity-and-affixes/corruption-implicits.md)

## More

- [What rolls traits](rngtech:rarity-and-affixes/what-rolls-traits.md)
- [Identification](rngtech:rarity-and-affixes/identification.md)
- [Refining](rngtech:rarity-and-affixes/refining.md)
- [Mirrored items](rngtech:rarity-and-affixes/mirrored-items.md)
