---
navigation:
  title: "Forestry Cart Station"
  parent: tools.md
  icon: rngtech:forestry_cart
categories:
  - "Tools"
item_ids:
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

<ItemImage id="rngtech:forestry_cart" scale="4" />

<Row>
  <ItemImage id="rngtech:forestry_cart_station" scale="2" />
  <ItemImage id="rngtech:forestry_cart" scale="2" />
  <ItemImage id="rngtech:iron_pruning_shears" scale="2" />
</Row>

- **Type:** Tree farm (rail cart and dock)
- **Power:** FE (cart Battery Cell, charged at the station)
- **Gear:** Cart: <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:modular_axe" />, <ItemLink id="rngtech:iron_pruning_shears" />. Station: Battery Cell, <ItemLink id="rngtech:basic_energy_connector" />
- **Mastery:** Yes (Forestry Companion)
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

RNGTech's tree farm is a cart that runs on vanilla rails. The **Forestry Companion** cart drives along a track, plants saplings beside it, and cuts down the trees that grow there. The **Companion Station** is an optional dock beside the track. It unloads the cart's harvest, refills its saplings, and charges its battery.

The cart does all the work and works without a station for as long as its battery and cargo last. Each Forestry Companion rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md) and levels up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Setting up](rngtech:forestry-cart-station/setting-up.md)
- [Planting and harvesting](rngtech:forestry-cart-station/planting-and-harvesting.md)

### Speed

| Travel | Speed (blocks per tick) | With Manual Throttle |
|---|---:|---:|
| Working | 0.055 | 0.095 |
| Looking for a station | 0.08 | 0.12 |
| Out of FE | 0.03 (0.06 with Coasting Clutch) | 0.03 (0.06 with Coasting Clutch) |

Cart Speed from affixes and Mastery multiplies the powered speeds, up to 0.34 blocks per tick. The **Manual Throttle** Mastery node adds a **Spd** toggle on the Cart tab. While it is on, the cart travels at the faster speeds above, but every movement tick costs 4 times as much FE.

- [Station](rngtech:forestry-cart-station/station.md)
- [Gear](rngtech:forestry-cart-station/gear.md)

### Automation

The station is the only automation surface; the cart's cargo cannot be piped.

| Side | Behavior |
|---|---|
| Top | With an Item Connector: inserts saplings, bone meal, shears, and spare Axes or Treefellers into their slots. |
| Bottom | With an Item Connector: extracts from the six output slots. |
| Any | With an Energy Connector: accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). With a Fluid Connector: accepts water. |

Each connector's tier caps how fast it transfers.

- [Pruning Shears](rngtech:forestry-cart-station/pruning-shears.md)
- [Mastery and Ascendancies](rngtech:forestry-cart-station/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:forestry-cart-station/crafting.md)

### Breaking

Mine a placed Companion Station with a pickaxe. It drops itself, the items in its slots, its connectors and Battery Cell, and its stored bone meal. Water in its tank is lost.

Break a placed Forestry Companion like a minecart. It drops as an item that keeps its traits and Mastery progress. Pick-block on a placed cart also keeps them.

## More

- [Screen](rngtech:forestry-cart-station/screen.md)
