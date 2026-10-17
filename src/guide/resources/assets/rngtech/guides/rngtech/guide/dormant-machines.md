---
navigation:
  title: "Dormant Machines"
  parent: guides.md
  icon: rngtech:dormant_machine
categories:
  - "Guides"
item_ids:
  - rngtech:dormant_machine
  - rngtech:derelict_cart
---

# Dormant Machines

<BlockImage id="rngtech:dormant_machine" scale="6" />

- **Type:** Derelict machine
- **Power:** FE
- **Holds:** One Mastery soul
- **Breakable:** Only once spent
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

A **Dormant Machine** is an old RNGTech machine with a Mastery soul still inside it. Wake it with power, take the soul with a <ItemLink id="rngtech:soul_transfer" />, and release the soul into a machine of the same kind. Your machine then starts at the soul's level instead of level 1.

You never need one. A soul only saves you the time it would take to level a machine yourself.

## Waking it

A Dormant Machine starts asleep. Fill its wake charge with FE:

- **Carry cells.** Put up to nine charged [Battery Cells](rngtech:battery-cells.md) in its sockets. It drains them all at once, each at its own output rate. The early sites are small enough for a handful of Iron or Lead Cells.
- **Bring power.** It takes FE from any side, from a generator or a cable.

A higher soul costs more to wake, and the cost climbs faster near the top of a site's level range. The charge never leaks away, even if you leave and come back. When it is full, the machine wakes and stays awake.

## Other players

Dormant Machines have no owner. Anyone can add power to one, and whoever absorbs it first takes the soul. If you leave a machine half-awake, someone else can finish waking it and take the soul, so guard it, finish it in one trip, or bring enough cells.

## Spent husks

A spent husk breaks with a pickaxe and drops **Scrap**. A spent derelict cart breaks with a hit and drops Scrap too. Cells left in their sockets drop with them.

## Automation

| Side | Behavior |
|---|---|
| Top, sides | Battery Cells go in. FE goes in on every side. |
| Bottom | Empty Battery Cells come out. Charged cells stay in. |

## Screen

The screen shows the cell sockets on the left, with a ring on each socket that dims as its cell empties. The FE gauge above them shows the power coming in. In the middle, the wake conduit fills as the charge rises, with the soul's level below it. On the right, the soul glows in its chamber, and the glyphs around it light up one by one as the machine wakes. The pips under the chamber show the stage the soul needs. Hover any part for exact values.

## More

- [Finding one](rngtech:dormant-machines/finding-one.md)
- [Taking the soul](rngtech:dormant-machines/taking-the-soul.md)
