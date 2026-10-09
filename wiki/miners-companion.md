---
wiki:
  category: Tools
  icon: rngtech:miners_companion
  ids:
    - rngtech:miners_companion
    - rngtech:miners_companion_magnet
    - rngtech:mining_lamp
---

# Miner's Companion

{{ infobox(
    variants=[
        ["Companion", "rngtech:miners_companion"],
        ["Magnet", "rngtech:miners_companion_magnet"],
        ["Lamp", "rngtech:mining_lamp"],
    ],
    fields={
        "Type": "Portable mining utility",
        "Power": "FE (installed Battery Cell)",
        "Gear": "{{ item('rngtech:iron_crush_head', 'Crush Head') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:iron_recovery_filter', 'Recovery Filter') }}, {{ item('rngtech:miners_companion_magnet', 'Magnet') }}, {{ item('rngtech:mining_lamp', 'Mining Lamp') }}",
        "Mastery": "No",
    },
) }}

The **Miner's Companion** is a battery-powered gadget you carry while mining. It has three utilities, each with its own on/off toggle:

- **Block Chew** deletes unwanted block drops, such as cobblestone or dirt, the moment you break the block, so they never reach your inventory.
- **Magnet** pulls nearby dropped items to you.
- **Mining Lamp** lights up the area around you like a held torch.

It works from anywhere in your inventory, hotbar, or offhand, and pays for everything from the Battery Cell installed in it. Each Miner's Companion you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting("rngtech:miners_companion") }}

The crafted Miner's Companion comes out Unidentified, like other trait items, and rolls as a Stage 2 item. It does not stack. See [Rarity and Affixes](rarity-and-affixes.md) for how to identify it.

## Usage

### Opening and toggling

- **Right-click** with the Miner's Companion in your main hand or offhand to open its screen. It opens even when you are aiming at a block.
- **Sneak + right-click** turns Block Chew on or off without opening the screen. This needs a Crush Head and a Battery Cell installed; otherwise the action bar tells you to install them.
- The Magnet and Mining Lamp toggles are on the Process tab of the screen.

Once set up, it does not need to be held. Every Miner's Companion in your main inventory, hotbar, or offhand works at the same time; armor slots and containers do not count. The item tooltip shows whether each utility is on, the active filter count, and the stored FE. The bar under the icon shows the installed cell's charge.

### Gear

Gear is installed on the Gear tab and stays on the item, so it travels with the Miner's Companion. Each slot holds one item.

| Slot | Required? | Accepts | Purpose |
|---|---|---|---|
| Head | Yes, for Block Chew | Any {{ item('rngtech:iron_crush_head', 'Crush Head') }} | Enables Block Chew. Its processing level also gates recovery recipes (see below). |
| Cell | Yes, for everything | Any {{ item('rngtech:iron_battery_cell', 'Battery Cell') }} | Stores the FE every utility spends. |
| Filter | No | Any {{ item('rngtech:iron_recovery_filter', 'Recovery Filter') }} | Enables trace recovery rolls. |
| Magnet | No | {{ item('rngtech:miners_companion_magnet') }} | Enables the Magnet toggle. |
| Lamp | No | {{ item('rngtech:mining_lamp') }} | Enables the Mining Lamp toggle. |

Hover an empty slot to see what it accepts and whether it is required; hold Shift for example items. Shift-clicking a valid item from your inventory moves it into its slot.

A toggle stays locked until its Gear and a Battery Cell are installed. Removing that Gear, or the Battery Cell, switches the utility off, so you have to turn it on again after reinstalling.

The Magnet and the Mining Lamp are fixed upgrades: they never roll rarity or affixes.

{{ crafting("rngtech:miners_companion_magnet", "rngtech:mining_lamp") }}

The Magnet needs a calibrated conductive component with at least 40% Stability, made on the [Resonance Calibrator](resonance-calibrator.md). The Mining Lamp is the earlier upgrade: it takes plain copper parts, a Bronze Plate, an Energy Coil, and either Glowstone Dust or Glow Berries.

### Charging

The Miner's Companion stores no FE of its own; it uses the installed Battery Cell. To charge it, put it in the Tool slot of a [Tool Bench](tool-bench.md) that has an Energy Connector installed. You can also take the cell out on the Gear tab, charge it like any other [Battery Cell](battery-cells.md), and put it back.

### Block Chew

When Block Chew is on and you break a block, the Miner's Companion checks each drop against its filters. Each matching item costs **16 FE** to remove; the Economy affix lowers that cost. If the cell cannot pay for a whole stack, the items it can pay for are removed and the rest drops normally. With more than one Miner's Companion on you, the next one with a matching filter takes what the first could not pay for.

Block Chew only acts on drops from blocks you break yourself. It never cleans out items already in your inventory, and it ignores mob drops, chest contents, and machine output.

The Process tab has a status square for Block Chew:

| Status | Meaning |
|---|---|
| Ready | Chewing works. |
| Disabled | Block Chew is switched off. |
| Needs Head | No Crush Head is installed. |
| Needs Cell | No Battery Cell is installed. |
| No Filters | None of the active filter slots has a filter in it. |
| No Power | The cell holds less FE than one chew costs. |

### Filters

The Process tab has ten **ghost filter slots** in two rows of five. A ghost slot holds a copy and never takes your item:

- Click a slot while holding a block item to set it as a filter. The item stays on your cursor.
- Click a slot with an empty cursor, or right-click it, to clear it.

Only block items and {{ item('rngtech:advanced_item_filter', 'Advanced Item Filters') }} are accepted.

A Miner's Companion starts with **3 active filter slots**. The others are greyed out and show "Locked by current roll". The **Indexing** prefix ("Indexed", "Cataloged", and so on) adds more, up to all ten. The active count is the Block Filter Slots stat in [Machine Stats](machine-stats.md).

A block-item filter matches in two ways: when a drop is that item, or when the block you broke is that block. A Stone filter therefore removes the cobblestone that stone drops. Take care with ore blocks: a Diamond Ore filter removes the diamonds that the ore drops.

An Advanced Item Filter can be set to **allow** or **deny**, and match by sample items, item tags, mod, component stage, Stability, identified state, or rarity; see [Advanced Item Filter](universal-cable.md#advanced-item-filter) for how to configure one. It is checked against both the drop and the broken block. Deny filters are checked first, so a drop matching any deny filter is kept. Then the drop is chewed if it matches any allow filter, or plain block-item filter. If every active filter is a deny filter, everything that is not denied gets chewed.

### Magnet

While the Magnet is on, it pulls dropped items within **6 blocks** towards you, and you pick them up by the normal rules. It does not store anything. It skips items reserved for another player.

The Magnet costs **1 FE per tick** while switched on, even with nothing to pull, plus **2 FE** for each item stack it pulls that tick. When the cell runs dry, it stops pulling until you recharge it.

### Mining Lamp

While the Mining Lamp is on, a light of level **14** follows your head, like holding a torch. It costs **1 FE per tick**. The light is only visual and only on your own screen: other players do not see it, and it does not stop mobs from spawning or help crops grow. It turns off when the cell is empty.

### Trace recovery

With a Recovery Filter installed, each chewed item gets a small chance, **1 in 5,000** before the filter's Efficiency, to give back an item from a recovery recipe. A recovery recipe can also require a minimum Recovery Filter stage and a minimum Crush Head processing level. A recovered item goes into your inventory, or drops at the broken block if your inventory is full.

RNGTech ships no recovery recipes of its own; modpacks and datapacks add them. With JEI installed, its **Miner's Companion Recovery** category lists the recovery recipes that are loaded, with the chewed input, the result, the chance, and the filter and head requirements.

### Automation

The Miner's Companion is an item, not a placed block, so it has no automation sides. Everything is set through its screen.

## Rarity, affixes, and refinement

Each Miner's Companion rolls its rarity, affixes, and Refinement Potential when you identify it. It can roll:

- **Indexing** (prefix): extra active filter slots.
- **Economy** (suffix): less FE per chewed item. It does not change the Magnet or Mining Lamp costs.

Because it is an item and has no Refinement tab, refine it in the [Affix Forge](affix-forge.md) or the [Exotic Affix Forge](exotic-affix-forge.md) like any other refinable item.

## Screen

The Miner's Companion screen has three tabs:

- **Process**: the FE gauge, the ghost filter slots, the Block Chew status square, and the **Chew**, **Mag**, and **Lamp** toggles. Hover a toggle to see its state and FE cost, or why it is locked.
- **Gear**: the Head, Cell, Filter, Magnet, and Lamp slots.
- **Stats**: active filters, stored and maximum FE, FE per chewed item, recovery chance (1/5000 with a Recovery Filter installed), and the Magnet and Lamp costs.

The screen stays open only while you hold the Miner's Companion in the hand you opened it with.

## Data values

{{ data_values() }}

## See also

- [Gear](gear.md), for Crush Heads and Recovery Filters.
- [Tool Bench](tool-bench.md), for charging.
- [Universal Cable](universal-cable.md), which uses the same Advanced Item Filter.

{{ navbox() }}
