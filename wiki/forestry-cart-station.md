---
wiki:
  category: Tools
  icon: rngtech:forestry_cart
  ids:
    - rngtech:forestry_cart_station
    - rngtech:forestry_cart
    - rngtech:iron_pruning_shears
    - rngtech:copper_pruning_shears
    - rngtech:bronze_pruning_shears
    - rngtech:steel_pruning_shears
    - rngtech:aluminum_pruning_shears
    - rngtech:titanium_pruning_shears
    - rngtech:tungstensteel_pruning_shears
    - rngtech:nullite_pruning_shears
    - rngtech:exotic_pruning_shears
---

# Forestry Cart Station

{{ infobox(
    variants=[
        ["Station", "rngtech:forestry_cart_station"],
        ["Companion", "rngtech:forestry_cart"],
        ["Shears", "rngtech:iron_pruning_shears"],
    ],
    fields={
        "Type": "Tree farm (rail cart and dock)",
        "Power": "FE (cart Battery Cell, charged at the station)",
        "Gear": "Cart: {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:modular_axe', 'Axe or Treefeller') }}, {{ item('rngtech:iron_pruning_shears', 'Shears') }}. Station: Battery Cell, {{ item('rngtech:basic_energy_connector', 'Connectors') }}",
        "Mastery": "Yes (Forestry Companion)",
    },
) }}

RNGTech's tree farm is a cart that runs on vanilla rails. The **Forestry Companion** cart drives along a track, plants saplings beside it, and cuts down the trees that grow there. The **Companion Station** is an optional dock beside the track. It unloads the cart's harvest, refills its saplings, and charges its battery.

The cart does all the work and works without a station for as long as its battery and cargo last. Each Forestry Companion rolls its own [rarity and affixes](rarity-and-affixes.md) and levels up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

The Station needs a [Tool Bench](tool-bench.md) and a composter. The Companion is built around a minecart.

{{ crafting("rngtech:forestry_cart_station", "rngtech:forestry_cart") }}

### Breaking

Mine a placed Companion Station with a pickaxe. It drops itself, the items in its slots, its connectors and Battery Cell, and its stored bone meal. Water in its tank is lost.

Break a placed Forestry Companion like a minecart. It drops as an item that keeps its traits and Mastery progress. Pick-block on a placed cart also keeps them.

## Usage

### Setting up

1. Lay a vanilla rail line through the area you want to farm. Dead ends are fine; the cart reverses at them.
2. Right-click a rail with the Forestry Companion to place it.
3. Right-click the cart to open it. Install a charged Battery Cell and a [modular](tool-bench.md) Axe or Treefeller, then load saplings into its supply cargo. Add shears if you want leaf drops.
4. Optionally, place a Companion Station right next to the track so the cart passes it.

The cart will not start without a Battery Cell and an unbroken cutting tool. While anyone has the cart's screen open, it stops and pauses work. Any player can open and manage a cart. The cart breaks blocks as the player who placed it, so protection and claim mods treat its harvest as that player's.

The cart stops working while it is off the rails, and resumes once it is back on a rail. If something blocks the track ahead, including the space above a slope, the cart stops and shows a blocked path. A player standing in the way also stops it. Leaves in the way are cut; any other block keeps it waiting until you clear it.

### Planting and harvesting

As the cart moves, it scans the row of blocks beside the rail on both sides. It plants saplings in empty spots and remembers each spot as a managed cell. A cart manages up to 96 cells to start with; Mastery can raise the cell cap and the number of rows, from 1 up to 7 on each side. It plants the nearest rows first. Only the first row reports **Invalid Soil** or **Planting Blocked**; outer-row spots over water, paths, or fences are skipped silently. Running out of saplings only stops planting: the cart still drives and harvests.

The cap fills sooner on wider farms. With one row, each rail block has 2 cells, so 96 cells cover 48 rail blocks; with three rows, each rail block has 6 cells and the cap fills after 16. When the cart scans a rail block, it forgets cells there that lie outside its current rows and no longer hold a sapling, crop, or log, so shortening the range or moving the track frees up cells.

To start over, **shift-click Reset** on the cart's Cart tab. The cart forgets every managed cell, returns any Seed Library reserves to its cargo, and reboots, standing still with a blue screen for 3 seconds. Plants stay in the world, and the cart takes back the ones it can tend as it passes them.

When it finds a grown tree beside the track, it stops, clears the leaves first, then cuts the logs from the top down. An Axe cuts one log at a time. A Treefeller cuts a batch of logs at once, which is faster but costs FE for its full batch size; if the battery cannot cover a whole batch, the cart goes to recharge first. Trees planted close together, or with merged canopies, are cut one tree at a time, and leaves touching another tree's logs wait for that tree. Saplings that drop go back into the cart's sapling cargo first, and everything else goes into its output cargo.

A tree can be too big to take. The cart works a tree of up to 512 logs, cutting it down over as many actions as it needs. A larger tree, or a connected mass of more than 2,048 logs, is skipped: the cart shows **Tree Too Large**, marks that cell blocked, and drives on to work other cells. It checks the tree again on later passes.

Without shears, leaves are removed but drop nothing, so the cart cannot replant from its own harvest. The Serrated Leaf Protocol Mastery node lets the cart collect normal leaf drops without shears, but each of those leaves costs 12 times the normal cut FE. With shears installed, leaves drop normally. Vanilla shears lose 1 durability per 8 leaves. {{ item('rngtech:iron_pruning_shears') }} and the other Pruning Shears last longer, and higher-stage shears last longer still. Broken shears are used up.

Cutting takes time. Logs crack like when you mine them, taking hardness × 60 ÷ (tool speed × Processing Speed) ticks, at least 6. An iron-head Axe cuts an ordinary log in about 24 ticks, and Efficiency on the tool speeds it up. After each cut the cart waits 4 ticks. A Treefeller cracks its whole batch at once, in the slowest log's time × (1 + 0.35 × log₂ of the batch size): a 16-log batch takes 2.4 times as long as one log. Planting takes 30 ticks divided by Processing Speed, whatever the tool. A cut in progress starts over if the cart is reloaded.

The cart pays FE from its Battery Cell for moving, scanning, planting, and cutting:

| Action | Base FE |
|---|---:|
| Moving | 1 per tick (4 with Manual Throttle on) |
| Scanning a rail block | 2 per row worked |
| Planting | 40 |
| Cutting a log or leaf | 80 (Treefeller: 80 × Tree Fell Limit per batch) |

Energy Use affixes and Mastery change these costs, and Processing Speed affixes shorten cutting and planting. The cart only charges its own Battery Cell; it never charges the cutting tool's cell. When its cargo is full or its battery is low, it stops working and heads along the track looking for a station. If it runs out of FE, it keeps rolling slowly so it can still reach one.

### Speed

| Travel | Speed (blocks per tick) | With Manual Throttle |
|---|---:|---:|
| Working | 0.055 | 0.095 |
| Looking for a station | 0.08 | 0.12 |
| Out of FE | 0.03 (0.06 with Coasting Clutch) | 0.03 (0.06 with Coasting Clutch) |

Cart Speed from affixes and Mastery multiplies the powered speeds, up to 0.34 blocks per tick. The **Manual Throttle** Mastery node adds a **Spd** toggle on the Cart tab. While it is on, the cart travels at the faster speeds above, but every movement tick costs 4 times as much FE.

### Station

When the cart stops beside a station, the station:

- unloads the cart's output into its six output slots.
- loads saplings from its supply into the cart. A Seed Library or Seed Ledger cart first gets the species its empty cells are waiting for, and the station makes room by moving supply nobody is waiting for into its plantables buffer. While the station still stocks a species the cells want, it tops up only species the cart already carries.
- installs spare shears into an empty cart shears slot, and a spare Axe or Treefeller when the cart's tool is missing or broken. The broken tool goes to the station's output slots, so no tool is swapped while those are full.
- charges the cart's Battery Cell, at up to its Energy Connector tier or its own Battery Cell's output rate, whichever is higher. The cart waits at the dock until its cell is full.
- holds a cart with empty sapling cargo while it loads saplings, then lets it go once the station has had nothing more to load for 20 ticks.

The station's hold toggle keeps any docked cart parked until you release it. The station does not need to be linked to a cart; any cart that passes it is served.

### Gear

The **Forestry Companion** carries its own Gear, managed from its screen:

- a **Battery Cell** (required), which powers everything the cart does.
- an **Axe** or **Treefeller** modular tool (required for tree work).
- **shears** (optional): vanilla shears or any Pruning Shears.
- a **Fluid Pump** (optional), used by the Field Hand's Sprinkler.

The **Companion Station** Gear tab has optional ports:

- a **Battery Cell** for extra FE storage.
- an **Energy Connector**, needed for the station to accept FE.
- an **Item Connector**, needed for item automation.
- a **Fluid Connector**, needed to fill its 16,000 mB water tank.

### Automation

The station is the only automation surface; the cart's cargo cannot be piped.

| Side | Behavior |
|---|---|
| Top | With an Item Connector: inserts saplings, bone meal, shears, and spare Axes or Treefellers into their slots. |
| Bottom | With an Item Connector: extracts from the six output slots. |
| Any | With an Energy Connector: accepts FE, for example from a [Universal Cable](universal-cable.md). With a Fluid Connector: accepts water. |

Each connector's tier caps how fast it transfers.

### Pruning Shears

{{ crafting("rngtech:iron_pruning_shears", "rngtech:copper_pruning_shears", "rngtech:bronze_pruning_shears", "rngtech:steel_pruning_shears", "rngtech:aluminum_pruning_shears", "rngtech:titanium_pruning_shears", "rngtech:tungstensteel_pruning_shears", "rngtech:nullite_pruning_shears", "rngtech:exotic_pruning_shears") }}

### Mastery and Ascendancies

Successful planting, leaf cleanup, and log cutting earn Mastery XP for the cart. The Forestry Companion starts from the Control / Drive start of the shared [Machine Mastery](machine-mastery.md) tree, with nodes for cart speed, work range, managed cells, and behaviors such as Magnet Mode, which picks up dropped items within 3 blocks into output cargo. With Seedling Magnet too, picked-up saplings go to sapling cargo first.

Its first Ascendancy Seal chooses one of three paths:

- **Timber Baron**: a logger that banks extra logs and harvests while it keeps rolling.
- **Grove Warden**: spends bone meal to grow managed saplings faster, and can plant 2×2 giant trees.
- **Field Hand**: a farmer that plants and harvests wheat, carrots, potatoes, and beetroot on farmland instead of trees, and can water its own fields.

A few rules the node tables below do not spell out:

- **Rolling Harvest** and **Rolling Reap**: the cart halts only when a tree or ripe crop it is working, or still has to work, would leave its reach at the next rail block. Cells it rolled past stay on its work list while in reach. Empty cells never hold it up. After planting, it moves at most one rail block per planting interval, so faster Processing Speed means a faster stride. With nothing to plant it rolls at full speed. Each sapling still costs its usual FE.
- **Growth Pulse**: growth costs bone meal, not FE alone. A Grove Warden cart stores up to 256 bone meal, loaded through its sapling slots or from a docked station. The station keeps its own 256 store, filled through its bone meal slot or its sapling slot. Each managed sapling takes at most one pulse per 100 ticks. If a ready sapling still does not grow, for lack of room or because its species only grows as a 2×2, that cell waits 1,200 ticks before the next pulse.
- **Seed Library** and **Seed Ledger**: a cell remembers whatever grows in it when the cart scans it, including plants you placed yourself, and the cart adopts those plants while its cell cap allows. When the cart harvests a cell, it sets aside one matching sapling or crop item from that cell's drops (four for an Ancient Grove plot) as that cell's replant. Planting uses that reserve before supply cargo, so a common species cannot crowd out a rare one. The Stats tab shows reserves as Reserved. They return to cargo when a cell is forgotten or changes species, move to output if the cart loses the node, and drop if the cart is broken.
- **Ancient Grove** needs four matching dark oak, jungle, or spruce saplings in cargo. It plants a clear 2×2 plot away from the rail on any row, and the far squares may sit one row past Work Range. It costs four plantings of FE and counts as one managed cell.
- **Field Hand** supply slots take only crop items. Seeds, carrots, and potatoes from a harvest go back to supply; wheat and beetroot go to output. Each crop harvest costs 20 FE and gives 2 machine XP. If the cart leaves Field Hand, crop items left in supply move to output.
- **Sprinkler**: the cart's water tank holds 8,000 mB, raised by Fluid Capacity. A docked station fills it as fast as the installed Fluid Pump's transfer rate allows. Sprinkled farmland counts as next to water for 60 seconds (90 with Irrigation), and the cart refreshes it once half that time has passed. Watering works like a nearby water source and adds no extra growth.

See [Forestry Companion ascendancies](machine-mastery.md#ascendancies) for the node tables.

## Ascendancy trees

Every node in this machine's ascendancies. See [Machine Mastery](machine-mastery.md#ascendancies) for how Seals and points work.

<!-- ascendancy-trees:start -->

### Timber Baron

| Node | Type | After | Effect |
|---|---|---|---|
| **Log Ledger** | Root | — | +10% Ledger Rate. Harvested logs bank Ledger Rate of themselves and pay whole logs into cargo; leaf cleanup is 25% slower. |
| Long Reach | Small | Log Ledger | +2 Tree Fell Limit. |
| **Sawyer’s Eye** | Notable | Long Reach | +5 Tree Fell Limit. |
| Broad Reach | Small | Sawyer’s Eye | +2 Tree Fell Limit. |
| **Clearcut Charter** | Deep notable | Broad Reach | +15% Ledger Rate. The cart stops planting and sends saplings to output cargo. |
| Lean Cut | Small | Log Ledger | 5% reduced Energy Use. |
| **Clean Fell** | Notable | Lean Cut | Treefeller batches cost FE only for the logs cut, not the full Tree Fell Limit. |
| Greased Axles | Small | Log Ledger | 15% increased Cart Speed. |
| **Dock Sprint** | Notable | Greased Axles | +50% Cart Speed while heading to a station. |
| Loose Brakes | Small | Dock Sprint | 15% increased Cart Speed. |
| **Rolling Harvest** | Deep notable | Loose Brakes | The cart keeps rolling while it plants and waits out work, and cuts trees within Work Range + 1 blocks of the rail. It plants every empty cell in reach in one action, then slows to one rail block per planting. |
| Tally Board | Small | Log Ledger | +3% Ledger Rate. |
| **Heartwood** | Notable | Tally Board | Logs cut in one Treefeller batch feed the Log Ledger twice. |

### Grove Warden

| Node | Type | After | Effect |
|---|---|---|---|
| **Growth Pulse** | Root | — | +1 Growth Pulse. As the cart passes a managed sapling, a growth pulse spends 1 bone meal and 30 FE. Load bone meal through the sapling slots of the cart or its station. Movement costs 25% more FE. |
| Spare Plots | Small | Growth Pulse | +16 managed cells. |
| **Nursery** | Notable | Spare Plots | +2 rows Work Range. |
| Old Rows | Small | Nursery | +16 managed cells. |
| **Ancient Grove** | Deep notable | Old Rows | With four matching saplings in cargo, plants a 2×2 giant dark oak, jungle, or spruce away from the rail. |
| Loam | Small | Growth Pulse | 10% increased Growth Pulse. |
| **Rich Soil** | Notable | Loam | 50% increased Growth Pulse. |
| Deep Roots | Small | Rich Soil | 10% increased Growth Pulse. |
| **Verdant Surge** | Deep notable | Deep Roots | Growth pulses reach every managed sapling within Work Range + 1 blocks of the rail. |
| Seed Drawers | Small | Growth Pulse | 5% reduced Energy Use. |
| **Seed Library** | Notable | Seed Drawers | Each managed cell replants the species it last grew, and waits until that sapling is in cargo. |
| Light Touch | Small | Growth Pulse | 5% increased Processing Speed. |
| **Canopy Care** | Notable | Light Touch | Shears wear half as fast. |

### Field Hand

| Node | Type | After | Effect |
|---|---|---|---|
| **Field Rotation** | Root | — | +1 row Work Range. Plants seeds, carrots, and potatoes from supply cargo on farmland in Work Range, harvests fully grown crops, and replants them. The cart never plants saplings or cuts trees, and needs no cutting tool. |
| Furrow Cells | Small | Field Rotation | +16 managed cells. |
| **Wide Furrows** | Notable | Furrow Cells | +1 row Work Range. |
| Long Furrows | Small | Wide Furrows | +16 managed cells. |
| **Open Fields** | Deep notable | Long Furrows | +1 row Work Range; +32 managed cells. |
| Hose Fittings | Small | Field Rotation | 25% increased Fluid Capacity. |
| **Sprinkler** | Notable | Hose Fittings | With a Fluid Pump installed, spends 50 mB of tank water to till dirt or grass under an empty crop cell or to wet farmland, which then stays watered for 60 seconds. Passing again refreshes it. |
| Wide Nozzles | Small | Sprinkler | 25% increased Fluid Capacity. |
| **Irrigation** | Deep notable | Wide Nozzles | The sprinkler reaches one more row, spends half the water, and keeps farmland watered 50% longer. |
| Sharp Sickle | Small | Field Rotation | 5% increased Processing Speed. |
| **Reap and Sow** | Notable | Sharp Sickle | Replants a harvested crop cell in the same action. |
| Quick Wheels | Small | Reap and Sow | 10% increased Cart Speed. |
| **Rolling Reap** | Deep notable | Quick Wheels | The cart keeps rolling while it harvests crops, plants, and waits out work, within Work Range + 1 blocks of the rail. |
| Seed Bins | Small | Field Rotation | +16 managed cells. |
| **Seed Ledger** | Notable | Seed Bins | Each managed cell replants the species it last grew, and waits until that sapling is in cargo. |

<!-- ascendancy-trees:end -->

## Screen

The **Forestry Companion** screen opens when you right-click the cart and has three tabs:

- **Cart**: the cart's cargo and Gear slots and its current action.
- **Stats**: the cart's energy, cargo, managed cells, tool condition, travel speed, and energy costs.
- **Mastery**: machine XP, level, and the passive tree.

The **Companion Station** screen has four tabs:

- **Station**: the six output slots, water and FE gauges, status, and the hold toggle.
- **Supply**: a 3×3 plantables buffer and a refills row for bone meal, a spare tool, and spare shears.
- **Gear**: the Battery Cell and connector ports.
- **Stats**: the station's water store and, while a cart is docked, the cart's energy, Work Range, and tool condition.

## Data values

{{ data_values() }}

## See also

- [Tool Bench](tool-bench.md), for building the cart's Axe or Treefeller.
- [Bio Generator](bio-generator.md)

{{ navbox() }}
