---
navigation:
  title: "Gear"
  parent: miners-companion.md
  position: 1
  icon: rngtech:miners_companion
---

# Gear

Gear is installed on the Gear tab and stays on the item, so it travels with the Miner's Companion. Each slot holds one item.

| Slot | Required? | Accepts | Purpose |
|---|---|---|---|
| Head | Yes, for Block Chew | Any <ItemLink id="rngtech:iron_crush_head" /> | Enables Block Chew. Its processing level also gates recovery recipes (see below). |
| Cell | Yes, for everything | Any <ItemLink id="rngtech:iron_battery_cell" /> | Stores the FE every utility spends. |
| Filter | No | Any <ItemLink id="rngtech:iron_recovery_filter" /> | Enables trace recovery rolls. |
| Magnet | No | <ItemLink id="rngtech:miners_companion_magnet" /> | Enables the Magnet toggle. |
| Lamp | No | <ItemLink id="rngtech:mining_lamp" /> | Enables the Mining Lamp toggle. |

Hover an empty slot to see what it accepts and whether it is required; hold Shift for example items. Shift-clicking a valid item from your inventory moves it into its slot.

A toggle stays locked until its Gear and a Battery Cell are installed. Removing that Gear, or the Battery Cell, switches the utility off, so you have to turn it on again after reinstalling.

The Magnet and the Mining Lamp are fixed upgrades: they never roll rarity or affixes.

<RecipesFor id="rngtech:miners_companion_magnet" />

<RecipesFor id="rngtech:mining_lamp" />

- **Calibrated Conductive Component** must be a Conductive calibration, Stage 2+, stability 40+.
- Requires the Iron material to be enabled.
- Requires the Copper material to be enabled.
- Requires the Bronze material to be enabled.

The Magnet needs a calibrated conductive component with at least 40% Stability, made on the [Resonance Calibrator](rngtech:resonance-calibrator.md). The Mining Lamp is the earlier upgrade: it takes plain copper parts, a Bronze Plate, an Energy Coil, and either Glowstone Dust or Glow Berries.
