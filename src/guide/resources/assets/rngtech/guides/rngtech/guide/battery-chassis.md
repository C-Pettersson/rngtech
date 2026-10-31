---
navigation:
  title: "Battery Chassis"
  parent: energy-storage.md
  icon: rngtech:iron_battery_chassis
categories:
  - "Energy storage"
item_ids:
  - rngtech:wooden_battery_chassis
  - rngtech:iron_battery_chassis
  - rngtech:copper_battery_chassis
  - rngtech:gold_battery_chassis
  - rngtech:steel_battery_chassis
  - rngtech:sparksteel_battery_chassis
  - rngtech:arclite_battery_chassis
  - rngtech:nullite_battery_chassis
  - rngtech:aethergold_battery_chassis
  - rngtech:exotic_battery_chassis
---

# Battery Chassis

<BlockImage id="rngtech:iron_battery_chassis" scale="6" />

<Row>
  <ItemImage id="rngtech:wooden_battery_chassis" scale="2" />
  <ItemImage id="rngtech:iron_battery_chassis" scale="2" />
  <ItemImage id="rngtech:copper_battery_chassis" scale="2" />
  <ItemImage id="rngtech:gold_battery_chassis" scale="2" />
  <ItemImage id="rngtech:steel_battery_chassis" scale="2" />
  <ItemImage id="rngtech:sparksteel_battery_chassis" scale="2" />
  <ItemImage id="rngtech:arclite_battery_chassis" scale="2" />
  <ItemImage id="rngtech:nullite_battery_chassis" scale="2" />
  <ItemImage id="rngtech:aethergold_battery_chassis" scale="2" />
  <ItemImage id="rngtech:exotic_battery_chassis" scale="2" />
</Row>

- **Type:** Energy storage
- **Stages:** 0–8
- **Power:** Stores FE in installed cells
- **Gear:** <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Battery Chassis** is a placed energy bank that holds [Battery Cells](rngtech:battery-cells.md). The cells provide the storage and decide how fast FE goes in and out. The chassis decides how many cells fit and adds its own behavior on top, such as balancing charge across cells or sealing in leakage.

Each chassis you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md). Battery Chassis do not have Machine Mastery.

You can read a chassis from the outside. Each active cell slot is a socket on the floor of the rack, and every installed cell stands in its socket, so you can see which cells are in and which slots are empty. The window on each cell fills up with that cell's charge. The lights on the corner posts show the whole bank's charge, the bottom two warning orange and red. While the bank charges, the light above the charge blinks and the lamps on the lid glow green; while it discharges, the top light pulses and the lamps glow amber. Teal lamps mean FE passes through in about equal amounts, and dark lamps mean nothing is moving. The bus rails under the lid and the straps over the cells are in the chassis's metal.

Each side, top and bottom included, shows what it touches. A side facing a cable connector or a [Universal Connector](rngtech:universal-cable.md) shows a connector port. A side against another FE block, such as a machine or a chassis it links to, is closed with a solid, smooth contact wall. A side with nothing attached stays open, or plain on the lid and base. The <ItemLink id="rngtech:wooden_battery_chassis" /> is a plain plank crate instead: its cells sit in the crate, wired to a copper bar, and its redstone lamp glows while FE flows in or out. Slow leakage alone does not light it.

## Usage

- [Storing power](rngtech:battery-chassis/storing-power.md)
- [Stages](rngtech:battery-chassis/stages.md)
- [Burst](rngtech:battery-chassis/burst.md)
- [Dyeing](rngtech:battery-chassis/dyeing.md)

### Gear

The Gear tab holds the Battery Cells. Any Battery Cell fits, one per slot. The **Additional Battery Slots** prefix can unlock up to four more slots, and the **Charged Storage** prefix doubles capacity after the chassis has held energy continuously for 10 minutes. If the bank runs empty, that timer starts over.

### Automation

| Side | Behavior |
|---|---|
| Any | Accepts and exports FE. |
| Any | Inserts and extracts Battery Cells, for example with a hopper or item connector. |

The chassis is not general item storage: it only holds Battery Cells.

## Screen

The Battery Chassis screen has four tabs:

- **Process**: stored FE, the chassis transfer rating, the cells' combined input and output rates, any attached connector cap, and last-tick transfer. Hover for exact values.
- **Gear**: the Battery Cell slots.
- **Stats**: the chassis's current stats, including traits.
- **Refinement**: refine the placed chassis's traits with a catalyst.

## Obtaining

- [Crafting](rngtech:battery-chassis/crafting.md)

### Breaking

Mine a placed chassis with a pickaxe. It drops itself and keeps its rolled traits and its dye color. Installed cells drop as separate items and keep their own FE, rarity, and affixes, but lose any bonus capacity the chassis was giving them.
