---
wiki:
  category: Fluids and gases
  icon: rngtech:steel_compressor_tank
  ids:
    - rngtech:iron_compressor_tank
    - rngtech:copper_compressor_tank
    - rngtech:bronze_compressor_tank
    - rngtech:steel_compressor_tank
    - rngtech:aluminum_compressor_tank
    - rngtech:titanium_compressor_tank
    - rngtech:tungstensteel_compressor_tank
    - rngtech:exotic_compressor_tank
    - rngtech:tank_frame
    - rngtech:pressure_tank_frame
---

# Fluid and Compressor Tanks

{{ infobox(
    variants=[
        ["Iron", "rngtech:iron_compressor_tank"],
        ["Copper", "rngtech:copper_compressor_tank"],
        ["Bronze", "rngtech:bronze_compressor_tank"],
        ["Steel", "rngtech:steel_compressor_tank"],
        ["Aluminum", "rngtech:aluminum_compressor_tank"],
        ["Titanium", "rngtech:titanium_compressor_tank"],
        ["Tungstensteel", "rngtech:tungstensteel_compressor_tank"],
        ["Exotic", "rngtech:exotic_compressor_tank"],
    ],
    fields={
        "Type": "Fluid storage",
        "Stages": "1–8",
        "Power": "None (Fluid Tanks), FE for compression (Compressor Tanks)",
        "Gear": "{{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }} (Compressor Tanks only)",
        "Mastery": "No",
    },
) }}

**Fluid Tanks** and **Compressor Tanks** are RNGTech's staged fluid storage blocks. Each holds one fluid at a time.

- **Fluid Tanks** (Iron, Copper, Bronze) are plain tanks. They need no power or Gear.
- **Compressor Tanks** (Steel and later) also have a second, compressed store. With a Battery Cell, at least one Servo, and FE, they squeeze fluid into it, so a small block holds far more fluid.

Each tank you craft rolls its own [rarity and affixes](rarity-and-affixes.md) where its tier allows it. Fluid Tanks do not roll affixes. Compressor Tank affixes improve FE storage, FE use, speed, fluid transfer, and stability. Tank capacity and compression ratio never roll.

## Obtaining

### Crafting

Each stage has its own recipe built on a {{ item('rngtech:tank_frame') }} (Fluid Tanks) or a {{ item('rngtech:pressure_tank_frame') }} (Compressor Tanks). You do not need the previous tank to craft the next one, so your existing tanks and their rolls stay usable.

{{ crafting() }}

### Breaking

Mine a tank with a pickaxe. It drops itself with its rolled traits, plus any installed Gear and Refinement items. **The fluid inside is lost**, so empty the tank before you move it.

## Usage

### Storing fluid

Pipe fluid into any side, or use the container slots on the Process tab to fill and empty buckets. A tank holds only one kind of fluid. It rejects a different fluid while either store still holds the old one.

### Compression

On a Compressor Tank, fluid first goes into the **loose** tank. With a valid Battery Cell, at least one Servo, and FE, the tank moves loose fluid into the **compressed** store. Compressed fluid still counts as the same amount of ordinary fluid. The Process tab also shows the smaller physical volume it takes up, so a Steel tank holding 100 buckets compressed shows about 10 buckets.

When you drain the tank, it empties loose fluid first. Draining compressed fluid needs the same Gear and FE, and costs a quarter of the FE it took to compress it. It runs at the same rate limit as compression. Without power, compressed fluid simply stays put: it never leaks, vanishes, or gets voided.

Idle compressed storage costs no FE.

FE you feed the tank fills its small internal buffer first, then charges the Battery Cell. Compression spends the buffer first, then draws from the cell. The tank sets no intake limit of its own, so only the cell, your cable, and the source limit charging.

Compression speed is the base rate times the Servo bonus, raised further by speed and fluid transfer affixes.

### Stages

Values are base values before rolls and Gear.

| Stage | Tank | Loose tank | Compressed store | Ratio | Compression rate | Compression cost |
|---:|---|---:|---:|---:|---:|---:|
| 1 | {{ item('rngtech:iron_compressor_tank') }} | 16 B | — | — | — | — |
| 2 | {{ item('rngtech:copper_compressor_tank') }} | 24 B | — | — | — | — |
| 3 | {{ item('rngtech:bronze_compressor_tank') }} | 32 B | — | — | — | — |
| 4 | {{ item('rngtech:steel_compressor_tank') }} | 48 B | 480 B | 10× | 150 mB/t | 200 FE/B |
| 5 | {{ item('rngtech:aluminum_compressor_tank') }} | 64 B | 1,024 B | 16× | 250 mB/t | 320 FE/B |
| 6 | {{ item('rngtech:titanium_compressor_tank') }} | 96 B | 2,304 B | 24× | 400 mB/t | 480 FE/B |
| 7 | {{ item('rngtech:tungstensteel_compressor_tank') }} | 128 B | 4,096 B | 32× | 700 mB/t | 640 FE/B |
| 8 | {{ item('rngtech:exotic_compressor_tank') }} | 192 B | 9,216 B | 48× | 1,000 mB/t | 960 FE/B |

The compressed store column is the amount of fluid it holds, not its physical size. 1 B is one bucket (1,000 mB).

### Gear

Fluid Tanks have no Gear. Compressor Tanks have a Gear tab with:

- one **Battery Cell** slot. A cell is required for compression and adds FE storage.
- four **Servo** slots. At least one is required. More Servos compress faster: 1.75× with two, 2.5× with three, and 3.5× with four. Better Servos also help, based on their average speed rolls.

Installed Gear cannot be from a higher stage than the tank. Servos start at Stage 4, which is why compression starts with the Steel tank.

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert fluid containers. |
| Bottom | Extracts filled containers. |
| Any | Fills and drains ordinary fluid, loose fluid first. Compressor Tanks also accept FE, for example from a [Universal Cable](universal-cable.md). |

Compressor Tanks only take FE in. They never give FE out, so they cannot act as batteries. Gear and Refinement slots are filled by hand.

To throw fluid away, use the purge buttons on the Process tab, or right-click the tank with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB. A normal right-click empties loose fluid first; sneaking empties compressed fluid first, which needs no FE.

## Screen

The tank screen has these tabs:

- **Process**: the loose and compressed stores with purge buttons, the fluid container slots, FE, and status.
- **Gear**: Battery Cell and Servo slots (Compressor Tanks only).
- **Stats**: the tank's current stats, including fluid capacity and compression ratio.
- **Refinement**: refine the placed tank's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Battery Cells](battery-cells.md)
- [Gear](gear.md)
- [Universal Cable](universal-cable.md)

{{ navbox() }}
