---
wiki:
  category: Guides
  icon: rngtech:affix_injector
---

# Rarity and Affixes

RNGTech treats machines like loot. Every machine, part, and Battery Cell you craft rolls its own **rarity**, a set of random **affixes**, and a budget of **Refinement Potential** (RP). Two Crushers from the same recipe can come out quite different: one might be faster, another might use less power, and a lucky one might do both.

This page explains what those rolls mean, how many affixes each rarity gets, how strong they can be, and how to improve them afterwards.

## What rolls traits

These items roll rarity, affixes, and RP when you craft them:

| Item | Examples |
|---|---|
| Machines | {{ item('rngtech:iron_furnace_chassis', 'Furnace') }}, {{ item('rngtech:iron_crusher_chassis', 'Crusher') }}, [Battery Chassis](battery-chassis.md), generators, [Fluid and Compressor Tanks](compressor-tank.md) |
| Machine parts ([Gear](gear.md)) | {{ item('rngtech:iron_heat_core') }}, {{ item('rngtech:iron_crush_head') }}, Servos, Fluid Pumps, Reactor Chambers |
| [Battery Cells](battery-cells.md) | {{ item('rngtech:iron_battery_cell') }} and every other material cell |
| Modular tool parts | Tool Heads and Tool Rods for the [Tool Bench](tool-bench.md) |
| Companions | The Forestry Companion cart from the [Forestry Cart Station](forestry-cart-station.md), and the {{ item('rngtech:miners_companion') }} |

A few blocks never roll traits: the [Affix Forge](affix-forge.md) and [Exotic Affix Forge](exotic-affix-forge.md), the Crude Component Recycler, the wooden and silica-gel machines such as the [Wooden Composter](wooden-composter.md), the [Algae Photobioreactor](algae-photobioreactor.md), and cables and connectors. Assembled modular tools carry no traits of their own. Their Tool Head and Tool Rod do.

## Identification

A freshly crafted item comes out **Unidentified**, for example "Unidentified Iron Furnace Chassis". Its rarity, affixes, and RP are hidden, and the crafting preview never shows them, so you cannot reroll a recipe by peeking at the result.

To identify an item, put it **alone** in a crafting grid and take the result. The preview still says Unidentified; the traits appear when you take the item out. Other ways to identify:

- **Placing** an unidentified machine rolls its traits as it goes down.
- Battery Cells made in a [Component Assembler](component-assembler.md) come out already identified.

You cannot refine an unidentified item. Identify it first.

## Rarity

Rarity sets how many affixes an item can hold.

| Rarity | Prefixes | Suffixes | Notes |
|---|---:|---:|---|
| **Normal** | 0 | 0 | Base stats only. |
| **Magic** | 1 | 1 | Named after its affixes, for example "Hellish Iron Furnace Chassis of Smelting". |
| **Rare** | up to 3 | up to 3 | Rolls three of each when crafted, if enough different affixes fit the item. The name uses its first prefix and first suffix. |
| **Unique** | — | — | Found, not crafted. Rolled stat lines, no RP, and only a [Volatile Catalyst](#corruption) can change it. |

[Uniques](uniques.md) have their own page: where to find them, how their ranged stats roll, and what each one does.

### Rarity odds

The odds depend on the item's [stage](stages.md):

| Item stage | Normal | Magic | Rare |
|---|---:|---:|---:|
| 0 | 35% | 50% | 15% |
| 1–4 | — | 70% | 30% |
| 5 and higher | 50% | 40% | 10% |

Stage 1–4 items are never Normal, so the gear you build early always shows some affixes. From Stage 5 up, a Normal roll is common, and the [Affix Forge](affix-forge.md) does more of the work.

The {{ item('rngtech:miners_companion') }} and the Forestry Companion roll as Stage 2 items. The Crude and Steel [Metal Press](metal-press.md) roll as Stage 8 items, with Stage 8 tiers and RP, because they have no upgrade ladder to carry your investment forward.

## Affixes

An affix is a random bonus, such as faster processing, a bigger energy buffer, or a chance for extra output.

- **Prefixes** come before the item name and lean towards capacity, heat, efficiency, and machine-specific tricks.
- **Suffixes** come after the name and lean towards speed, energy use, generation, output, and slots.

An item can only roll affixes that its machine actually uses, so a Solar Panel never rolls Crusher bonuses. An item also never holds two affixes from the same group. For example, a Crusher can have only one processing-speed suffix: plain speed, a named one like "of Crushing", Overclocked, or Bulk Speed. Narrow items may have fewer legal affixes than the rarity allows.

### Base stats and fixed traits

Rolled affixes sit on top of the item's own identity, which never changes:

- **Base stats** come from the item's material and stage. A Titanium Heat Core is always hotter than an Iron one.
- **Fixed traits** are built-in behaviors listed under Behaviors in the tooltip. For example, the Bronze Furnace speeds up alloy blends, and Copper, Bronze, and Steel Fuel Boxes have [Fuel Governor](#fuel-governor).

Base stats and fixed traits do not use affix slots, and refinement can never remove them.

### Reading the tooltip

Hover an identified item to see its rarity, RP, and rolled affixes. A [Corrupted](#corruption) item shows a red Corrupted line instead of its RP, and any corruption implicit in red. Hold **Left Shift** for material and affix details, and **Left Alt** for each affix's tier and roll range. To see how affixes, Gear, and Mastery combine into a machine's final numbers, hold **Shift** over a row on its Stats tab. See [Machine Stats](machine-stats.md#how-stats-are-built).

Affixes use four words:

- **increased** and **reduced** add together. Two "20% increased Processing Speed" affixes give 40%. Energy Use is the exception: its reductions add together and then divide the cost, so 100% reduced Energy Use halves it instead of removing it.
- **more** and **less** multiply separately, after that, so they are worth more when stacked with increases.

### Affix tiers

Most affixes have a **tier** from 1 to 7. The tier sets the range, and the affix rolls one value inside it. Most percentage affixes use this table:

| Tier | Range |
|---:|---:|
| 1 | 2–5% |
| 2 | 4–10% |
| 3 | 11–30% |
| 4 | 40–55% |
| 5 | 60–75% |
| 6 | 80–100% |
| 7 | 110–140% |

Crusher yield affixes use smaller tables, because they all feed the Crusher's [Output Amount](crusher.md#output-amount) total, which already has diminishing returns.

- **Crusher Jaws** and **Crush Head Pulverizing** (prefixes) give the most Bonus Output, but also roll reduced Processing Speed. The speed penalty rolls on its own, wider range, so a light penalty is a lucky roll.
- The **Output Amount** suffix on Crushers and Crush Heads is small and has no penalty. It can sit beside Jaws or Pulverizing.

| Tier | Jaws and Pulverizing: Bonus Output | Jaws and Pulverizing: reduced Processing Speed | Output Amount suffix |
|---:|---:|---:|---:|
| 1 | 2–3% | 1–4% | 1% |
| 2 | 4–6% | 3–8% | 1–2% |
| 3 | 7–10% | 5–12% | 2–3% |
| 4 | 11–14% | 8–18% | 3% |
| 5 | 15–17% | 11–22% | 4% |
| 6 | 18–20% | 14–26% | 4–5% |
| 7 | 22–25% | 17–31% | 6% |

Older Crushers and Crush Heads can carry yield values from earlier, larger tables, without the speed penalty. They keep those values until you upgrade or reroll the affix, which then uses these tables.

Some affixes use their own tables, such as flat FE, extra slots, or durability. A few are untiered and always roll the same value.

Low tiers are much more common than high ones. The item's stage sets a **natural tier**, and every tier above it is far less likely:

| Item stage | Natural tier |
|---:|---:|
| 0 | 1 |
| 1–2 | 2 |
| 3–6 | 3 |
| 7–8 | 4 |

One tier above natural rolls at half the usual weight, two above at 15%, and three or more above at 5%. Stage 3–4 items are the exception: they roll one tier above natural at full weight, so about one in four Stage 3–4 items shows a Tier 4 or better affix. Tool Heads and Tool Rods are slightly luckier with high tiers.

Random rolls reach Tier 6 at most. **Tier 7** appears only on Stage 8 Exotic items.

### Machine and part affixes

Where an affix sits changes what it affects:

- **Affixes on the machine** apply to the whole machine.
- **Affixes on an installed part are local.** They improve that part's own contribution first, and then the part's total is added to the machine. For example, a percentage Max Temperature affix on a Heat Core raises that core's heat, not the chassis's.
- **Crush Head yield is the exception.** The head's own Output Amount and its yield affixes add straight to the Crusher's increased Output Amount total.

Hold Left Shift on a part to see which of its affixes are marked **(Local)**. On generators, a part's generation affix scales the machine's base or recipe output once; it does not scale flat FE/t from other parts.

Machine affixes, together with [Machine Mastery](machine-mastery.md) passives, are what scale the whole machine.

## Notable affixes

Most affixes simply raise or lower a [stat](machine-stats.md). A few change how the machine behaves.

### Bulk Speed

A suffix for processing machines. Each completed process adds 1% Processing Speed, up to 100%. Powered machines keep the same FE per craft, so their FE/t rises as they speed up. The bonus resets when the chain is interrupted, for example by invalid input, missing Gear, or a different recipe. Topping up the input with more of the same item keeps it.

Bulk Speed works on the Crusher, electric Furnace, Alloy Furnace, Component Assembler, Component Recycler, Metal Press, Melter, Resonance Calibrator, Potential Reactor, and Corrosion Cell. On the two generators it can double FE/t at its cap. It can also roll on the Coal Gasifier, Steam Methane Reformer, and Ammonia Synthesizer, where it has no effect.

Bulk Speed takes the processing-speed group, so it never appears alongside another speed suffix.

### Balance Mode

A Battery Chassis prefix, shown as **Balanced**. The chassis fills and drains all installed Battery Cells evenly instead of one at a time. It gives chassis without a built-in Charge Balancer the same behavior. See [Battery Chassis](battery-chassis.md).

### Fuel Governor

A fixed trait of the Copper, Bronze, and Steel Fuel Boxes in a [Solid Fuel Burner](solid-fuel-burner.md). While the burner's storage is full, it pauses the fuel burn instead of wasting it, and keeps exporting stored FE. It is not a rolled affix.

### Power Grace

A suffix that halves the failure strain a heat machine takes when its power drops mid-cycle. It can roll on electric Furnaces, Alloy Furnaces, Heat Cores, Alloy Crucibles, and Servos, and Stage 6 and higher Servos have it built in. See [Furnace](furnace.md) for how failure strain works.

### Other named affixes

| Affix | Effect |
|---|---|
| Overclocked | More Processing Speed, but also more Energy Use. |
| Instant Process | A small chance for a process to finish the moment it starts. Powered machines need enough FE for the whole craft up front. |
| Super Output | A small chance to add one extra copy of the recipe's base output. |
| Charged Storage | Battery Chassis only. 100% more installed-cell capacity after the chassis has held energy for 10 minutes straight. |
| Additional Battery Slots | Battery Chassis only. Unlocks up to four extra cell slots. |
| Vein Miner | Rare Pick Head prefix. The Pick breaks connected ore of the same type, at an extra FE cost per block. |
| Self Repair | Tool Head and Tool Rod prefix. Restores durability every 7.5 seconds while the tool sits idle in your hotbar or offhand. |

## Refinement Potential

**Refinement Potential** (RP) is each item's budget for changing its affixes. Every successful refinement spends some. When RP runs out, the item is final, so spend it where it matters. The only way back is a Reforge in the [Exotic Affix Forge](exotic-affix-forge.md), which rerolls the whole item, RP included. A [Corrupted](#corruption) item keeps its RP but can never spend it, so its RP is hidden.

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

## Refining

Refinement changes an item's rolled affixes with a consumable **catalyst**. You can:

- **add** an affix ({{ item('rngtech:affix_injector') }}). A Normal item becomes Magic, a Magic item fills its missing prefix or suffix, and a Rare item keeps adding until full.
- **upgrade** a random affix to a higher tier, up to Tier 6.
- **promote** a Magic item to Rare with an Ascension Catalyst or Ascension Matrix.
- **remove** or **reroll** affixes.
- **corrupt** a finished item with a {{ item('rngtech:volatile_catalyst') }}, a one-way gamble described in [Corruption](#corruption).

You choose the catalyst, but the game picks which affix it adds or upgrades. In the Affix Forge, modifier lenses can tilt the odds towards a family you want.

Where to refine:

- **Items** in the [Affix Forge](affix-forge.md). Its [catalyst table](affix-forge.md#catalysts) lists every operation and its RP cost.
- **Placed machines** in the Refinement tab of the machine's own screen.
- **Tool Heads and Tool Rods**, even inside an assembled tool, in the [Tool Bench](tool-bench.md) Refine tab.
- **Late-game powered operations** in the [Exotic Affix Forge](exotic-affix-forge.md).

## Corruption

A {{ item('rngtech:volatile_catalyst') }} corrupts a finished machine part or Battery Cell in the [Affix Forge](affix-forge.md#corrupting-with-a-volatile-catalyst), or a placed machine in its Refinement tab. It costs no RP, and it is the only thing that can change a Unique. Machine and chassis items, Tool Heads, Tool Rods, companions, and unidentified or stripped items cannot be corrupted as items; place a machine first and corrupt it from its Refinement tab.

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

A **Corrupted** item can never be refined again: the Affix Forge, Exotic Affix Forge, Tool Bench, Refinement tabs, and a second Volatile Catalyst all refuse it, and nothing is used. Corruption cannot be removed. Nothing is ever destroyed or loses rarity; the worst result is a Blighted implicit. A placed machine keeps its corruption when you break and place it again.

### Corruption implicits

A corruption implicit is one fixed value, with no tier or range. It sits outside the prefix and suffix limits, and applies after the item's affixes. Each item draws one from its own pool, weighted:

| Item | Blessed | Blighted |
|---|---|---|
| Heat Core | 10% increased Max Temperature, 15% increased Temperature Stability, 15% increased Overheat Tolerance, 10% increased Heat Transfer, 10% increased Fuel Efficiency, 20% reduced Warmup Time, or Power Grace | 20% less Temperature Stability, or 25% increased Warmup Time |
| Crush Head | +1 Processing Level (rare), +25% Jam Recovery, 10% increased Processing Speed, or 10% more Processing Speed | 25% more Jam Chance, or 15% less Processing Speed |
| Servo | Power Grace, Auto Purge, 15% increased Overheat Tolerance, 10% increased Processing Speed, 15% increased Stability, 15% reduced Energy Use, or 20% increased Fluid Transfer | 15% less Processing Speed, or 20% less Stability |
| Alloy Crucible | +15% Blend Speed, 15% increased Temperature Stability, 10% increased Processing Speed, 15% increased Overheat Tolerance, or 20% reduced Warmup Time | 20% less Stability |
| Fluid Pump | 50% increased Fluid Capacity, or 30% increased Fluid Transfer | 30% less Fluid Transfer |
| Battery Cell | 20% increased Energy Transfer, 30% less Idle Loss, 15% increased Energy Capacity, or 10% increased Efficiency | Doubled Idle Loss |
| Resonance Coil, Control Board, Stabilizer Matrix | +1 Coil Reach, 10% increased Catalyst Efficiency, 10% increased Calibration Precision, or 10% increased Processing Speed | 20% less Processing Speed, or 10% less Calibration Precision |
| Bio Chamber, Fuel Box | 15% increased Fuel Efficiency, or 15% increased Fuel Duration; the Bio Chamber can also roll 10% increased Energy Generation, and the Fuel Box 15% increased Stability | 10% less Energy Generation on the Bio Chamber; 15% less Stability on the Fuel Box |
| Other generator parts | 10% increased Energy Generation; most also roll 15% increased Stability or 10% increased Processing Speed | 10% less Energy Generation |
| Other parts | 15% increased Stability; most also roll 10% increased Processing Speed or 10% increased Efficiency | 15% less Stability |
| Placed processing machine | 10% increased Processing Speed, 20% reduced Energy Use, or 15% increased Stability where the machine has it; +1 Batch Size (rare) on the Crusher, Melter, Metal Press, and Resonance Calibrator; 15% increased Fuel Efficiency or 10% increased Max Temperature on the Furnace | 25% increased Energy Use; 20% less Fuel Efficiency on the Furnace |
| Placed generator | 10% increased Energy Generation, 15% increased Efficiency, and where the machine has them 15% increased Stability or 25% increased Energy Capacity | 20% less Efficiency |
| Battery Chassis | +1 Battery Slot, 15% increased Energy Capacity, or 20% increased Energy Transfer | 15% increased Idle Loss |

+1 Processing Level on a Crush Head lets it crush one level harder ore without jamming; it does not change which Gear the Crusher accepts. With a catalyst in the Affix Forge, hover Blessed or Blighted in the outcome panel to see the exact pool and odds for the item in the target slot. Corruption implicits never add yield or RP.

## See also

- [Machine Stats](machine-stats.md): what each stat does.
- [Affix Forge](affix-forge.md): catalysts, lenses, and modifier crystals.
- [Gear](gear.md): the parts you install in machines.
- [Machine Mastery](machine-mastery.md): levelling machines up.

{{ navbox() }}
