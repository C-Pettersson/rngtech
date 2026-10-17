---
navigation:
  title: "Fluid and Compressor Tanks"
  parent: fluids-and-gases.md
  icon: rngtech:steel_compressor_tank
categories:
  - "Fluids and gases"
item_ids:
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

<BlockImage id="rngtech:steel_compressor_tank" scale="6" />

<Row>
  <ItemImage id="rngtech:iron_compressor_tank" scale="2" />
  <ItemImage id="rngtech:copper_compressor_tank" scale="2" />
  <ItemImage id="rngtech:bronze_compressor_tank" scale="2" />
  <ItemImage id="rngtech:steel_compressor_tank" scale="2" />
  <ItemImage id="rngtech:aluminum_compressor_tank" scale="2" />
  <ItemImage id="rngtech:titanium_compressor_tank" scale="2" />
  <ItemImage id="rngtech:tungstensteel_compressor_tank" scale="2" />
  <ItemImage id="rngtech:exotic_compressor_tank" scale="2" />
</Row>

- **Type:** Fluid storage
- **Stages:** 1–8
- **Power:** None (Fluid Tanks), FE for compression (Compressor Tanks)
- **Gear:** <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:steel_servo" /> (Compressor Tanks only)
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

**Fluid Tanks** and **Compressor Tanks** are RNGTech's staged fluid storage blocks. Each holds one fluid at a time.

- **Fluid Tanks** (Iron, Copper, Bronze) are plain tanks. They need no power or Gear.
- **Compressor Tanks** (Steel and later) also have a second, compressed store. With a Battery Cell, at least one Servo, and FE, they squeeze fluid into it, so a small block holds far more fluid.

Each tank you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md) where its tier allows it. Fluid Tanks do not roll affixes. Compressor Tank affixes improve FE storage, FE use, speed, fluid transfer, and stability. Tank capacity and compression ratio never roll.

## Usage

### Storing fluid

Pipe fluid into any side, or use the container slots on the Process tab to fill and empty buckets. A tank holds only one kind of fluid. It rejects a different fluid while either store still holds the old one.

- [Compression](rngtech:compressor-tank/compression.md)
- [Stages](rngtech:compressor-tank/stages.md)

### Gear

Fluid Tanks have no Gear. Compressor Tanks have a Gear tab with:

- one **Battery Cell** slot. A cell is required for compression and adds FE storage.
- four **Servo** slots. At least one is required. More Servos compress faster: 1.75× with two, 2.5× with three, and 3.5× with four. Better Servos also help, based on their average speed rolls.

Installed Gear cannot be from a higher stage than the tank. Servos start at Stage 4, which is why compression starts with the Steel tank.

- [Automation](rngtech:compressor-tank/automation.md)

## Screen

The tank screen has these tabs:

- **Process**: the loose and compressed stores with purge buttons, the fluid container slots, FE, and status.
- **Gear**: Battery Cell and Servo slots (Compressor Tanks only).
- **Stats**: the tank's current stats, including fluid capacity and compression ratio.
- **Refinement**: refine the placed tank's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:compressor-tank/crafting.md)

### Breaking

Mine a tank with a pickaxe. It drops itself with its rolled traits, plus any installed Gear and Refinement items. **The fluid inside is lost**, so empty the tank before you move it.
