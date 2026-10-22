---
navigation:
  title: "Miner's Companion"
  parent: tools.md
  icon: rngtech:miners_companion
categories:
  - "Tools"
item_ids:
  - rngtech:miners_companion
  - rngtech:miners_companion_magnet
  - rngtech:mining_lamp
---

# Miner's Companion

<ItemImage id="rngtech:miners_companion" scale="4" />

<Row>
  <ItemImage id="rngtech:miners_companion" scale="2" />
  <ItemImage id="rngtech:miners_companion_magnet" scale="2" />
  <ItemImage id="rngtech:mining_lamp" scale="2" />
</Row>

- **Type:** Portable mining utility
- **Power:** FE (installed Battery Cell)
- **Gear:** <ItemLink id="rngtech:iron_crush_head" />, <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:iron_recovery_filter" />, <ItemLink id="rngtech:miners_companion_magnet" />, <ItemLink id="rngtech:mining_lamp" />
- **Mastery:** No

The **Miner's Companion** is a battery-powered gadget you carry while mining. It has three utilities, each with its own on/off toggle:

- **Block Chew** deletes unwanted block drops, such as cobblestone or dirt, the moment you break the block, so they never reach your inventory.
- **Magnet** pulls nearby dropped items to you.
- **Mining Lamp** lights up the area around you like a held torch.

It works from anywhere in your inventory, hotbar, or offhand, and pays for everything from the Battery Cell installed in it. Each Miner's Companion you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md).

## Usage

- [Opening and toggling](rngtech:miners-companion/opening-and-toggling.md)
- [Gear](rngtech:miners-companion/gear.md)

### Charging

The Miner's Companion stores no FE of its own; it uses the installed Battery Cell. To charge it, put it in the Tool slot of a [Tool Bench](rngtech:tool-bench.md) that has an Energy Connector installed. You can also take the cell out on the Gear tab, charge it like any other [Battery Cell](rngtech:battery-cells.md), and put it back.

- [Block Chew](rngtech:miners-companion/block-chew.md)
- [Filters](rngtech:miners-companion/filters.md)

### Magnet

While the Magnet is on, it pulls dropped items within **6 blocks** towards you, and you pick them up by the normal rules. It does not store anything. It skips items reserved for another player.

The Magnet costs **1 FE per tick** while switched on, even with nothing to pull, plus **2 FE** for each item stack it pulls that tick. When the cell runs dry, it stops pulling until you recharge it.

### Mining Lamp

While the Mining Lamp is on, a light of level **14** follows your head, like holding a torch. It costs **1 FE per tick**. The light is only visual and only on your own screen: other players do not see it, and it does not stop mobs from spawning or help crops grow. It turns off when the cell is empty.

- [Trace recovery](rngtech:miners-companion/trace-recovery.md)

### Automation

The Miner's Companion is an item, not a placed block, so it has no automation sides. Everything is set through its screen.

## Rarity, affixes, and refinement

Each Miner's Companion rolls its rarity, affixes, and Refinement Potential when you identify it. It can roll:

- **Indexing** (prefix): extra active filter slots.
- **Economy** (suffix): less FE per chewed item. It does not change the Magnet or Mining Lamp costs.

Because it is an item and has no Refinement tab, refine it in the [Affix Forge](rngtech:affix-forge.md) or the [Phase Array](rngtech:phase-array.md) like any other refinable item.

## Obtaining

- [Crafting](rngtech:miners-companion/crafting.md)

## More

- [Screen](rngtech:miners-companion/screen.md)
