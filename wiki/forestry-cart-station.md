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

Mine a placed Companion Station with a pickaxe. It drops itself.

Break a placed Forestry Companion like a minecart. It drops as an item that keeps its traits and Mastery progress. Pick-block on a placed cart also keeps them.

## Usage

### Setting up

1. Lay a vanilla rail line through the area you want to farm. Dead ends are fine; the cart reverses at them.
2. Right-click a rail with the Forestry Companion to place it.
3. Right-click the cart to open it. Install a charged Battery Cell and a [modular](tool-bench.md) Axe or Treefeller, then load saplings into its supply cargo. Add shears if you want leaf drops.
4. Optionally, place a Companion Station right next to the track so the cart passes it.

The cart will not start without a Battery Cell and an unbroken cutting tool. While anyone has the cart's screen open, it stops and pauses work.

### Planting and harvesting

As the cart moves, it scans the row of blocks beside the rail on both sides. It plants saplings in empty spots and remembers each spot as a managed cell. A cart manages up to 96 cells to start with; Mastery can raise both the number of rows and the cell cap.

The cap fills sooner on wider farms. With one row, each rail block has 2 cells, so 96 cells cover 48 rail blocks; with three rows, each rail block has 6 cells and the cap fills after 16. When the cart scans a rail block, it forgets cells there that lie outside its current rows and no longer hold a sapling, crop, or log, so shortening the range or moving the track frees up cells.

To start over, **shift-click Reset** on the cart's Cart tab. The cart forgets every managed cell, returns any Seed Library reserves to its cargo, and reboots, standing still with a blue screen for 3 seconds. Plants stay in the world, and the cart takes back the ones it can tend as it passes them.

When it finds a grown tree beside the track, it stops, clears the leaves first, then cuts the logs from the top down. An Axe cuts one log at a time. A Treefeller cuts a batch of logs at once, which is faster but costs FE for its full batch size. Saplings that drop go back into the cart's sapling cargo first, and everything else goes into its output cargo.

A tree can be too big to take. The cart works a tree of up to 512 logs, cutting it down over as many actions as it needs. A larger tree, or a connected mass of more than 2,048 logs, is skipped: the cart shows **Tree Too Large**, marks that cell blocked, and drives on to work other cells. It checks the tree again on later passes.

Without shears, leaves are removed but drop nothing, so the cart cannot replant from its own harvest. The Serrated Leaf Protocol Mastery node lets the cart collect normal leaf drops without shears, but each of those leaves costs 12 times the normal cut FE. With shears installed, leaves drop normally. Vanilla shears lose 1 durability per 8 leaves. {{ item('rngtech:iron_pruning_shears') }} and the other Pruning Shears last longer, and higher-stage shears last longer still.

The cart pays FE from its Battery Cell for moving, scanning, planting, and cutting. When its cargo is full or its battery is low, it stops working and heads along the track looking for a station. If it runs out of FE, it keeps rolling slowly so it can still reach one.

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
- charges the cart's Battery Cell. The cart waits at the dock until its cell is full.

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

Successful planting, leaf cleanup, and log cutting earn Mastery XP for the cart. The Forestry Companion starts from the Control / Drive start of the shared [Machine Mastery](machine-mastery.md) tree, with nodes for cart speed, work range, managed cells, and behaviors such as Magnet Mode, which picks up nearby dropped items.

Its first Ascendancy Seal chooses one of three paths:

- **Timber Baron**: a logger that banks extra logs and harvests while it keeps rolling.
- **Grove Warden**: spends bone meal to grow managed saplings faster, and can plant 2×2 giant trees.
- **Field Hand**: a farmer that plants and harvests wheat, carrots, potatoes, and beetroot on farmland instead of trees, and can water its own fields.

See [Forestry Companion ascendancies](machine-mastery.md#ascendancies) for the node tables.

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
