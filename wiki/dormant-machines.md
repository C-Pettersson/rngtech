---
wiki:
  category: Guides
  icon: rngtech:dormant_machine
  ids:
    - rngtech:dormant_machine
    - rngtech:derelict_cart
---

# Dormant Machines

{{ infobox(
    fields={
        "Type": "Derelict machine",
        "Power": "FE",
        "Holds": "One Mastery soul",
        "Breakable": "Only once spent",
    },
) }}

A **Dormant Machine** is an old RNGTech machine with a Mastery soul still inside it. Wake it with power, take the soul with a {{ item('rngtech:soul_transfer') }}, and release the soul into a machine of the same kind. Your machine then starts at the soul's level instead of level 1.

You never need one. A soul only saves you the time it would take to level a machine yourself.

## Finding one

Each Dormant Machine sits in a small ruin, one machine per site. Sites are occasional finds: you come across a Collapsed Workshop now and then, and an End Relay less often than an End city. Each kind of site holds souls of a few machine families and a range of levels:

| Site | Where | Souls | Level | Wake FE |
|---|---|---|---:|---:|
| Collapsed Workshop | A roofless shed on dry land in plains, forests, taigas, savannas, and meadows | Crusher, Furnace, Metal Press | 8-20 | 35K-100K |
| Abandoned Cart Siding | A dead-end rail tunnel underground in the Overworld | Forestry Companion | 10-25 | 35K-100K |
| Buried Calibration Vault | A sealed deepslate room deep underground | Resonance Calibrator | 20-40 | 750K-3M |
| Flooded Foundry | A brick ruin on the ocean floor; the machine waits in a glass bell of air | Alloy Furnace, Melter | 25-45 | 1M-4M |
| Nether Smeltworks | A blackstone platform in Nether Wastes and Basalt Deltas | Furnace, Alloy Furnace, Melter | 35-55 | 2.5M-7.5M |
| End Relay | A purpur pylon on the End's outer islands | Crusher, Resonance Calibrator, Melter | 40-70 | 10M-60M |

You can't break a Dormant Machine while it still holds a soul.

The Abandoned Cart Siding holds a **derelict Forestry Companion** on the end of its rails instead of a block. It works the same way, except that it takes power only from the cells on its screen, not from cables.

## Waking it

A Dormant Machine starts asleep. Fill its wake charge with FE:

- **Carry cells.** Put up to nine charged [Battery Cells](battery-cells.md) in its sockets. It drains them all at once, each at its own output rate. The early sites are small enough for a handful of Iron or Lead Cells.
- **Bring power.** It takes FE from any side, from a generator or a cable.

A higher soul costs more to wake, and the cost climbs faster near the top of a site's level range. The charge never leaks away, even if you leave and come back. When it is full, the machine wakes and stays awake.

## Taking the soul

Use a {{ item('rngtech:soul_transfer') }} on the awake machine. The absorb has the same success chance as on any machine (see [Soul Transfer](machine-mastery.md#soul-transfer)). On success the Soul Transfer holds the soul, and the machine turns into a spent husk.

The soul carries:

- XP for its level, with every point already spent along a build that suits its machine family.
- One free Full Reset, so you can clear that build without spending Mastery Refunds.
- No ascendancy and no Seal tiers.

A soul goes into any machine of its own family, even an early one. Your modpack may require a minimum stage; when it does, the Soul Transfer's tooltip shows it.

A failed absorb always uses up the Soul Transfer. By default the machine keeps its soul and stays awake, so you can try again. Your modpack may make a failure destroy the soul, or blow the machine apart. When it does, a striped warning plate shows on the machine's screen.

## Other players

Dormant Machines have no owner. Anyone can add power to one, and whoever absorbs it first takes the soul. If you leave a machine half-awake, someone else can finish waking it and take the soul, so guard it, finish it in one trip, or bring enough cells.

## Spent husks

A spent husk breaks with a pickaxe and drops {{ item('rngtech:scrap') }}. A spent derelict cart breaks with a hit and drops Scrap too. Cells left in their sockets drop with them.

## Automation

| Side | Behavior |
|---|---|
| Top, sides | Battery Cells go in. FE goes in on every side. |
| Bottom | Empty Battery Cells come out. Charged cells stay in. |

## Screen

The screen shows the cell sockets on the left, with a ring on each socket that dims as its cell empties. The FE gauge above them shows the power coming in. In the middle, the wake conduit fills as the charge rises, with the soul's level below it. On the right, the soul glows in its chamber, and the glyphs around it light up one by one as the machine wakes. The pips under the chamber show the stage the soul needs. Hover any part for exact values.

## Data values

{{ data_values() }}

## See also

- [Machine Mastery](machine-mastery.md)
- [Battery Cells](battery-cells.md)

{{ navbox() }}
