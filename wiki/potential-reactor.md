---
wiki:
  category: Power generation
  icon: rngtech:potential_reactor
  ids:
    - rngtech:potential_reactor
---

# Potential Reactor

{{ infobox(
    fields={
        "Type": "Generator",
        "Power": "Burns salvage and retired gear, generates FE",
        "Gear": "{{ item('rngtech:iron_reactor_chamber', 'Reactor Chamber') }}, {{ item('rngtech:iron_recovery_filter', 'Recovery Filter') }}, {{ item('rngtech:iron_containment_lining', 'Containment Lining') }}",
        "Mastery": "No",
    },
) }}

The **Potential Reactor** turns salvage and old gear into FE. Feed it recycling byproducts, or any rolled machine, part, or Battery Cell you no longer need, and it pays out power for the item's rarity, affixes, and remaining Refinement Potential. The stripped item that comes out goes on to the [Component Recycler](component-recycler.md) for its materials.

Each Potential Reactor you craft rolls its own [rarity and affixes](rarity-and-affixes.md). It does not have Machine Mastery.

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine it with a pickaxe to get it back. Items in its slots and installed Gear drop as separate items.

## Usage

### Generating power

Install a Reactor Chamber on the Gear tab, then put fuel in the input slot. The reactor processes one item at a time, adds the FE to its internal buffer, and pushes it out to adjacent blocks on its four sides, for example a [Universal Cable](universal-cable.md). When the buffer is full, the reactor pauses so expensive fuel is never burned for nothing.

A redstone signal pauses the reactor.

It accepts two kinds of fuel:

- **Salvage fuel**: recycling byproducts listed in the recipe table below. Each has a fixed FE value and needs a minimum Reactor Chamber stage.
- **Retired gear**: any machine, machine part, or Battery Cell that still has something to pay for: rarity above Normal, at least one rolled prefix or suffix, or unspent Refinement Potential. Unique items are refused, and so are unidentified or already stripped items.

### Recycling gear

Burning a rolled item pays FE for its rarity, affix tiers, and Refinement Potential. Plain base stats pay nothing on their own. The payout then scales with what the item was:

- **Stage**: Stage 0 junk pays a quarter, Stage 4 gear pays 1.25×, and Stage 8 gear pays 2.25×.
- **Mastery**: +1% for each Mastery level above 1.
- **Ascendancy**: +200% for each Seal tier used on the machine, so a fully ascended machine is worth 7×.

Repeats lose value. The reactor remembers the item types of its last 15 gear burns, and each earlier burn of the same type cuts the next payout by a quarter, down to 10%. Rotate through 16 or more different items to avoid the penalty. Salvage fuel recipes are not affected.

The output is a single **stripped** copy of the item you burned. A stripped item cannot be placed, installed, refined, or charged, but the [Component Recycler](component-recycler.md) still recognizes it. A typical chain is:

```text
Chest → Potential Reactor → Component Recycler → storage
```

### Gear

| Slot | Required | What it does |
|---|---|---|
| Reactor Chamber | Yes | Sets the reactor's stage, which gates salvage fuel, and adds FE generation and stability. |
| Recovery Filter | No | Returns residue from salvage fuel. Without one, residue is lost. Can also roll efficiency and speed. |
| Containment Lining | No | Raises stability, which increases the FE you recover. |

| Stage | Reactor Chamber | Recovery Filter | Containment Lining |
|---:|---|---|---|
| 1 | {{ item('rngtech:iron_reactor_chamber') }} | {{ item('rngtech:iron_recovery_filter') }} | {{ item('rngtech:iron_containment_lining') }} |
| 2 | {{ item('rngtech:copper_reactor_chamber') }} | {{ item('rngtech:copper_recovery_filter') }} | {{ item('rngtech:copper_containment_lining') }} |
| 3 | {{ item('rngtech:bronze_reactor_chamber') }} | {{ item('rngtech:bronze_recovery_filter') }} | {{ item('rngtech:bronze_containment_lining') }} |
| 4 | {{ item('rngtech:steel_reactor_chamber') }} | {{ item('rngtech:steel_recovery_filter') }} | {{ item('rngtech:steel_containment_lining') }} |

Recovered residue is {{ item('rngtech:scrap') }}, which cannot be fed back in for more FE. The reactor also accepts {{ item('rngtech:malformed_ingot') }} from failed smelts and presses. With a Recovery Filter, a malformed ingot of a known metal returns two matching nuggets, and an unknown one returns two Scrap.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts salvage fuel or gear to recycle. |
| Bottom | Extracts residue and stripped items. |
| Sides | Export FE to adjacent receivers. Do not accept FE. |

Gear slots are installed by hand.

### Potential Reactor recipes

Salvage fuel uses its own recipe type, `rngtech:potential_reactor`. Residue appears only with a Recovery Filter installed. Retired gear is not listed here because its value is calculated from each item.

{{ processing("potential_reactor") }}

With JEI installed, click the recipe line on the Process tab to open the Potential Reactor category.

## Screen

The Potential Reactor screen has four tabs:

- **Process**: input and residue slots, the FE buffer, processing progress, and status. Hover for exact FE, fuel value, and rates.
- **Gear**: Reactor Chamber, Recovery Filter, and Containment Lining slots, plus the chamber's stage.
- **Stats**: the reactor's current stats, including traits and Gear.
- **Refinement**: refine the placed reactor's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Component Recycler](component-recycler.md), which breaks stripped items down into materials.
- [Machine Stats](machine-stats.md)

{{ navbox() }}
