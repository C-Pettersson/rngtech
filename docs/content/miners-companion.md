# Miner's Companion

Resource ids: `rngtech:miners_companion`, `rngtech:advanced_item_filter`

Status: Prototype

## Summary

The Miner's Companion is a portable FE-backed inventory utility for mining sessions. Its current utility channels are Block Chew, Magnet, and Mining Lamp.

The item works while it is installed and enabled anywhere in the player's main inventory, hotbar, or offhand. Right-click opens the Miner's Companion GUI. Sneak-right-click toggles Block Chew on or off without opening the GUI. Magnet and Mining Lamp have separate Process-tab toggles.

## Utility Direction

The current code-backed utilities are:

- Block Chew: filtered block drops are removed at block-break time in exchange for FE.
- Magnet: nearby dropped item entities are pulled toward the player.
- Mining Lamp: the client renders a player-following auxiliary light similar to holding a torch.

Magnet and Mining Lamp are fixed upgrade items. They do not roll affixes and do not add new modifier families. The Magnet recipe consumes a conductive calibrated component from copper-coil calibration with at least `40%` stability. The Mining Lamp recipe is earlier and uses uncalibrated copper parts, one Bronze Plate, one Energy Coil, and either Glowstone Dust or Glow Berries.

## Gear

| Gear slot | Required? | Runtime role |
|---|---:|---|
| Crush Head | Yes | Enables chewing and supplies `PROCESSING_LEVEL` for recovery recipe gates. |
| Battery Cell | Yes | Provides the FE store used by the Miner's Companion and exposes item energy through the Miner's Companion stack. |
| Recovery Filter | No | Unlocks datapack-authored trace recovery rolls and supplies the filter stage and `EFFICIENCY` multiplier. |
| Miner's Companion Magnet | No | Enables the Magnet toggle and pulls dropped item entities toward the player. |
| Mining Lamp | No | Enables the Mining Lamp toggle and creates client-side visual light while powered. |

The Miner's Companion stores its installed Gear on the item stack. It is refined as an item-stack target through the Affix Forge rather than through a placed-machine Refinement tab.

Empty Gear slots show hover hints for the accepted component type, whether the slot is required, and Shift-expanded valid item examples.

The installed Battery Cell can be charged through the Miner's Companion stack's item energy capability. A Tool Bench with an installed Energy Connector can charge a Miner's Companion placed in the Tool slot.

## Filters

The Process tab has up to ten ghost filter slots. Filter entries accept block items or Advanced Item Filters, store one non-consumed copy, and match either the item form of block drops or the item form of the block that was mined. Normal block-item filters are allow entries. Advanced Item Filters can be configured as allow or deny filters using samples, item tags, item mod ids, component stage, Stability, identified/unidentified state, and rarity. Deny filters veto first; allow filters then whitelist. If only deny filters are active, anything not denied can be chewed. The active slot count starts at `3` from the base profile and can be raised by Miner's Companion filter-slot prefixes, capped at ten.

The Process tab Block Chew status square reports whether chewing is disabled, missing required Gear, missing active filters, out of FE for one chew, or ready.

When an enabled Miner's Companion sees matching drops from a player block break, it removes as many matching item drops as it can pay for. The base cost is `16 FE` per chewed item before the stack's effective `ENERGY_USAGE` multiplier. If the installed Battery Cell cannot pay for a full drop stack, the paid portion is removed and the remaining drop stays in the world.

Existing inventory contents are not cleaned up. The Miner's Companion only acts on newly generated block drops.

## Utility Toggles

The Process tab exposes independent toggles for Block Chew, Magnet, and Mining Lamp. A toggle is locked until its matching Gear and a Battery Cell are installed, and removing that Gear clears the stored toggle state. Each utility also needs enough stored FE for that tick or action.

Magnet costs `1 FE/t` while enabled and installed, then `2 FE` for each dropped item entity it pulls during that tick. It scans within `6` blocks, respects item pickup ownership targets, pulls matching dropped item entities toward the player, and lets vanilla pickup rules handle the final pickup. It adds no item storage.

Mining Lamp costs `1 FE/t` while enabled and installed. Its light is client-side visual light at level `14`, follows the player's eye position, and does not create server block light for mob spawning or crop growth.

## Recovery

Optional recovery is driven by `rngtech:miners_companion_recovery` datapack recipes. No default recovery recipes are shipped in the first implementation.

Each chewed item has a baseline `1/5000` recovery roll. The installed Recovery Filter's effective `EFFICIENCY` scales that chance. A recipe can require both a minimum Recovery Filter stage and a minimum Crush Head processing level. Successful recovery attempts insert the result into the player's inventory; if the inventory is full, the result drops at the broken block position.

JEI exposes loaded Miner's Companion Recovery recipes with the chewed input, recovered output, base chance, recipe weight, minimum filter stage, and minimum head level. The Miner's Companion is the recipe catalyst.

## Current Limits

The Miner's Companion has no sided automation and no placed block form. Block Chew only listens to player block-drop events, so it does not affect machine outputs, chest contents, mob drops, or unrelated loot systems. Magnet affects existing dropped item entities only and does not add storage or item-transfer network behavior.
