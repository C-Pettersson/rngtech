---
wiki:
  category: Refinement
  icon: rngtech:exotic_affix_forge
  ids:
    - rngtech:exotic_affix_forge
    - rngtech:phase_emitter
    - rngtech:phase_coil
    - rngtech:glyph
---

# Phase Array

{{ infobox(
    variants=["rngtech:exotic_affix_forge", "rngtech:phase_emitter", "rngtech:phase_coil"],
    fields={
        "Type": "Refinement multiblock",
        "Stages": "8",
        "Power": "FE consumer",
        "Gear": "9 × {{ item('rngtech:exotic_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Phase Array** is the endgame refinement bench. Where the [Affix Forge](affix-forge.md) rolls dice for you, the Array lets you choose: raise this affix, remove that one, add the affix you want, swap one for another, or move an affix from a donor item. Every operation is a **pulse**: the line charges, you fire it, and the Coils hold the pulse or let it slip. A pulse costs FE that grows with how good the item already is, adds **Strain** to the item, and the deeper operations spend a **Glyph**. The Array spends only a little [Refinement Potential](rarity-and-affixes.md#refinement-potential), so an item is usually done when you decide it is, not when a counter runs out.

The Array works on any refinable item of any stage: machine items, machine parts, Battery Cells, Tool Heads, Tool Rods, companions, and its own Emitter and Coils. Unique and [Corrupted](rarity-and-affixes.md#corruption) items are refused.

## Obtaining

### Crafting

{{ crafting("rngtech:exotic_affix_forge", "rngtech:phase_emitter", "rngtech:phase_coil") }}

The Emitter and Coils roll rarity and affixes when crafted, like other Stage 8 parts, and keep them when mined. The Cradle does not roll. All three recipes need the Nullite material to be enabled.

### Glyphs

{{ item('rngtech:glyph', 'Glyphs') }} have no recipe. They come in tiers I to V and are earned by running machines hard:

| Source | Glyph |
|---|---|
| A machine reaching Mastery level 20 or 40 | Tier I |
| A machine reaching Mastery level 60 or 80 | Tier II |
| A machine reaching Mastery level 100 | Tier III |
| A Stage 6 or higher [calibration](resonance-calibrator.md) at 90% stability or more | 15% chance of Tier II; Stage 8 at 95% gives Tier III |
| A [Forestry Companion](forestry-cart-station.md) harvest with a Stage 7 or higher head | 2% chance of Tier II |
| A [Vacuum Collapse](vacuum-collapse-generator.md) cycle at instability 60 or more | 10% chance of Tier III; 90 or more gives Tier IV |
| A [Crusher](crusher.md) jam on a Processing Level 7 or higher recipe | 5% chance of Tier I |
| Waking a [Dormant Machine](dormant-machines.md) | Tier I to IV by site |

Tier V has no source yet. A Glyph with no tier counts as Tier I, so one from a quest or a command works.

### Breaking

Mine any part with a pickaxe. The Cradle drops itself and everything in its slots, including the Battery Cells. A Coil keeps the Glyph inscribed into it.

## Usage

### Building the line

Place the Cradle, then three to nine Coils in a straight horizontal row from any side of it, and an Emitter at the far end. The Cradle finds the line on its own and lights the parts while it is complete; break any part and the line goes dark. Nothing else is needed above, below, or beside the row.

Each Coil has a **position**, counted from the Emitter. A Coil is blank until a Glyph is inscribed into it, and a blank Coil ends the usable line. The position sets the lowest Glyph tier it takes:

| Position | Lowest Glyph tier |
|---:|---:|
| 1 to 3 | I |
| 4 to 5 | II |
| 6 to 7 | III |
| 8 | IV |
| 9 | V |

To inscribe, right-click a placed Coil with a Glyph. The Glyph is used up and the Coil keeps its tier for good. The Cradle's screen does the same: with a Glyph in its Glyph slot, click the Coil's node on the line. An inscribed Coil can move to any position that accepts its tier, so a line grows by inserting a new Coil next to the Emitter and shifting the rest outward.

### Running a pulse

1. Put the item in the Cradle's **target** slot. For a Graft, put the donor in the **donor** slot too.
2. Pick an operation from the row of marks. Operations that work on one affix or one empty slot ask you to click it in the list; Imprint then lists the affixes it can add, and Graft lists the donor's affixes.
3. Hover the mark for the cost, the Glyph, the RP, and the odds of each outcome for this item on this line.
4. Click **Charge**. The line fills from FE on any side of the Cradle, the Emitter, or a Coil, and from the Battery Cells in the Cradle's sockets, up to the line's Charge Rate. A dip in supply pauses the charge; it never fails. Charge stays if you close the screen or switch to a cheaper operation.
5. Click **Fire**. The Coils engage from the Emitter inward, one every 0.4 seconds with a tone that climbs the line. The first Coil that slips stops the pulse: it flashes, the lit Coils go dark one by one, and the outcome applies.

The item sits locked in the Cradle while the pulse runs and moves to the **output** slot when it ends. Take it out before the next pulse.

### Outcomes

Each Coil holds the pulse with its own **Hold Chance**, 92% before affixes. With `R` the Coils the operation needs:

| Outcome | When | Effect |
|---|---|---|
| Success | At least `R` Coils hold | The operation does what it says. |
| Overhold | Every Coil holds on a line longer than `R` | Success with a bonus, named on the operation. Each surplus Coil has an 8% chance to trigger it. |
| Partial | The pulse slips one or two Coils short of `R` | A weaker version, named on the operation. |
| Miss | The pulse slips earlier | Nothing changes. Only the FE and the Glyph are spent. |
| Fracture | Rolled before the Coils engage, 1% per point of Strain above the line's Tolerance | One random affix drops a tier. A Tier 1 affix is removed. |

RP is spent on Success, Overhold, and Partial, never on a Miss or a Fracture.

### Operations

{{ processing("phase_array", columns=["action", "coils", "glyph_tier", "base_fe", "refinement_potential_cost", "partial", "overhold"]) }}

- **Reforge** rerolls rarity, affixes, and RP as if the item were newly crafted. Strain stays.
- **Attune** rerolls every affix's value within its tier.
- **Resonate** raises the selected affix one tier, up to Tier 6.
- **Excise** removes the selected affix.
- **Imprint** adds the affix you choose to the empty slot you choose. The tier rolls with the item's normal weights.
- **Transmute** swaps the selected affix for another legal affix of the same slot at the same tier.
- **Temper** sets Strain to 0.
- **Exalt** raises the selected Tier 6 affix to Tier 7. Exotic items only.
- **Graft** moves the selected donor affix onto the empty slot you choose, keeping its tier and value. The donor is destroyed.

### Costs

A pulse's FE is the operation's base, times a factor for the item's stage, times `1.5` for every `3.5` points of the item's **Quality**, the sum of its affix tiers. A fresh Rare Stage 8 part pays about four times the base; six Tier 7 affixes pay about a hundred and thirty times. Stage 0 to 2 items pay a tenth of a Stage 8 item at the same Quality. The line's FE Thrift takes a percentage off.

Every pulse adds one **Strain** to the item, on every outcome, unless the line's Strain Mercy rolls. Strain shows on the item's tooltip and as marks around the chamber. Only Temper removes it, and Excise's Overhold takes one off.

### Gear

The Cradle has nine Battery Cell sockets. Any Battery Cell fits. While the line charges, each cell drains into the charge at its own output rate, so a full set carries a pulse to a line with no cable.

### Line stats

The Emitter and Coils roll their own affixes, and the line reads them. Hover a Coil's node for its Hold Chance, Second Hold, Overhold Chance, and affixes, and the Emitter's for the line's totals and its affixes.

| Stat | From | What it does |
|---|---|---|
| Charge Capacity, Charge Rate | Every part, summed | How much FE the line holds and how fast it takes it |
| Primed Charge Rate | Emitter | Extra Charge Rate, paid only while a Battery Cell sits in the Cradle's sockets |
| Hold Chance, Second Hold | Each Coil | Whether that Coil holds the pulse, and whether a slip there is rolled again |
| Overhold Chance | Each Coil, plus the Emitter | The chance a surplus hold triggers Overhold |
| Partial Window | The Emitter | How many Coils short of `R` still count as Partial |
| Strain Tolerance | Every part, summed | Strain that adds no Fracture chance |
| FE Thrift, FE Recovery | Every part, summed | A percentage off every pulse, and a percentage of a slipped pulse's FE returned to the charge |
| Glyph Salvage, Strain Mercy | Every part, summed | The chance a Glyph returns on a Miss, and the chance a pulse adds no Strain |

A Coil or the Emitter is itself a target: pull it out, refine it in the Cradle, and put it back.

### Automation

| Side | Behavior |
|---|---|
| Any side of the Cradle, the Emitter, or a Coil | Accepts FE while the line charges, for example from a [Universal Cable](universal-cable.md). |
| Bottom of the Cradle | Hands out Battery Cells once they are drained, for example to a hopper. Nothing goes in this way. |

Nothing else moves by automation: the target, the Glyph, the donor, and the output are placed and taken by hand, and every pulse is a click. The target shows in the Cradle's bore while it sits in the chamber.

## Screen

The Cradle has one screen. The line is drawn across the top as it stands, with the Emitter at the left and the target's chamber at the right; the conduit under it fills with the charge. The cell sockets sit on the left, the operation marks and the affix list in the middle, and the one button under the list reads Charge, Stop, Fire, or Pulse. Three lights at the top left show blocked, waiting for input, and live. Hover anything for its exact values.

With JEI installed, the operations are listed under the Phase Array category.

## Data values

{{ data_values() }}

## See also

- [Affix Forge](affix-forge.md): the early catalyst-based station and how refinement works.
- [Rarity and Affixes](rarity-and-affixes.md)
- [Dormant Machines](dormant-machines.md)

{{ navbox() }}
