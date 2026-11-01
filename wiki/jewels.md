---
wiki:
  category: Guides
  icon: rngtech:drive_jewel
  ids:
    - rngtech:jewel_blank
    - rngtech:control_jewel
    - rngtech:drive_jewel
    - rngtech:reserve_jewel
---

# Jewels

{{ infobox(
    variants=[["Control", "rngtech:control_jewel"], ["Drive", "rngtech:drive_jewel"], ["Reserve", "rngtech:reserve_jewel"], ["Blank", "rngtech:jewel_blank"]],
    fields={
        "Type": "Rolled item",
        "Stage": "3",
        "Rarity": "Magic or Rare",
        "Sockets": "[Machine Mastery](machine-mastery.md) tree, machine bodies",
    },
) }}

A **jewel** is a small rolled item that you socket into a machine. It carries up to four affixes from one pool that every machine can read. A jewel line works where the machine reads that stat and does nothing where it does not, like a node on the [Machine Mastery](machine-mastery.md) tree. Jewels fill the gaps in a build, and a Rare jewel with four good lines is worth chasing.

## Obtaining

### Crafting

Craft a **Jewel Blank** in a crafting grid.

{{ crafting("rngtech:jewel_blank") }}

### Cutting

A Stage 3 or higher [Resonance Calibrator](resonance-calibrator.md) cuts a Jewel Blank into a jewel. Use the Structural pattern. The catalyst picks the colour:

| Catalyst | Jewel |
|---|---|
| {{ item('minecraft:prismarine_crystals') }} | {{ item('rngtech:control_jewel') }} |
| {{ item('minecraft:gold_ingot') }} | {{ item('rngtech:drive_jewel') }} |
| {{ item('minecraft:redstone_block') }} | {{ item('rngtech:reserve_jewel') }} |

The cut's stability sets the jewel's Refinement Potential: 2 RP below 50%, 3 RP from 50%, and 4 RP from 80%. A jewel comes out of the Calibrator **Unidentified**. Put it alone in a crafting grid to identify it, like any other [rolled item](rarity-and-affixes.md#identification).

## Rarity and affixes

A jewel holds up to two prefixes and two suffixes. A Magic jewel rolls one or two lines. A Rare jewel rolls three or four. Every jewel line has one tier, so refinement changes which lines a jewel has and how high they rolled, never a tier.

All three colours roll the same pool. A jewel's colour makes its own attribute's lines come up more often, and only a colour's own threshold line can roll on it.

| Line | Slot | Roll | Colour |
|---|---|---|---|
| Capacitive | Prefix | 10-20% increased Energy Capacity | Reserve |
| Frugal | Prefix | 4-8% reduced Energy Use | Control |
| Steady | Prefix | 5-10% increased Stability | Control |
| Deep | Prefix | 10-20% increased Fluid Capacity | Reserve |
| Insulated | Prefix | 6-12% increased Heat Isolation | Control |
| Conductive | Prefix | 6-12% increased Heat Transfer | Drive |
| Thrifty | Prefix | 4-8% increased Fuel Efficiency | Reserve |
| Yielding | Prefix | 4-8% increased Bonus Output | Drive |
| Fortunate | Prefix | +1-2% Super Output | Drive |
| of Pace | Suffix | 6-10% increased Processing Speed | Drive |
| of Output | Suffix | 6-10% increased Energy Generation | Reserve |
| of Control, of Drive, of Reserve | Suffix | +6-10 of the attribute | Its own |
| of Study | Suffix | 10-20% increased Mastery XP gain | Control |
| of Flashes | Suffix | +1-2% Instant Process | Drive |
| of Headroom | Suffix | +1-2% [Maximum Bonus Output](machine-stats.md#processing) | Any, rare |
| of Momentum | Suffix | 12% increased Processing Speed with at least 100 Drive | Drive only, rare |
| of Poise | Suffix | 8% reduced Energy Use with at least 100 Control | Control only, rare |
| of Depth | Suffix | 25% increased Energy Capacity with at least 100 Reserve | Reserve only, rare |

The threshold lines read the machine's final attribute, after Gear and every jewel's own flat attribute lines.

## Sockets

### Tree sockets

The [Machine Mastery](machine-mastery.md#node-types) tree has nine **Jewel Sockets**. A socket grants nothing itself. Each socket you allocate opens one jewel slot on the machine's **Jewels** tab. Hover a socket on the Mastery tab to see the jewel it holds and which of its lines this machine reads.

Refunding a socket, or clearing the tree, leaves the jewel in its slot. The jewel does nothing until a socket is allocated again. A Soul Transfer carries the tree's jewels with the Mastery.

### Body sockets

A machine body can have up to four sockets of its own:

- The **Bedazzling** suffix gives 1 socket at Tier 1 to 3, 2 at Tier 4 to 6, and 3 at Tier 7. Every machine with a screen can roll it.
- The Gold [Crusher](crusher.md#stages), Gold [Furnace](furnace.md#stages), and Gold [Battery Chassis](battery-chassis.md) each have one built in.

Body sockets work on machines without Machine Mastery too. Body jewels stay with the body when you break it.

### The Jewels tab

Every machine screen has a **Jewels** tab. The top row is the tree sockets, one slot per allocated socket. The row under it is the body sockets. Put an identified jewel in a slot, or shift-click it from your inventory. Take it out the same way. A jewel never gets used up.

## Refinement

A jewel is a refinement target like any part. The [Affix Forge](affix-forge.md) and the [Phase Array](phase-array.md) can add, remove, reroll, swap, and corrupt its lines. Jewel lines have one tier, so upgrade catalysts, Resonate, and Exalt have nothing to raise on them.

## Data values

{{ data_values() }}

## See also

- [Rarity and Affixes](rarity-and-affixes.md)
- [Machine Mastery](machine-mastery.md)
- [Resonance Calibrator](resonance-calibrator.md)

{{ navbox() }}
