---
navigation:
  title: "Fuel"
  parent: solid-fuel-burner.md
  position: 2
  icon: rngtech:crude_solid_fuel_burner
---

# Fuel

A fuel has to pass two checks before the burner accepts it:

- **Fuel tier.** The Heat Core sets the highest tier you can burn.
- **Fuel form.** The Fuel Box decides whether you can burn item fuels, compact block fuels, or both.

| Tier | Fuels | Requires |
|---:|---|---|
| 1 | <ItemLink id="minecraft:coal" /><ItemLink id="minecraft:charcoal" /><ItemLink id="minecraft:stick" /><ItemLink id="minecraft:oak_planks" /><ItemLink id="minecraft:oak_log" /> and every other log and plank | Any Heat Core |
| 2 | <ItemLink id="minecraft:coal_block" />, <ItemLink id="minecraft:dried_kelp_block" /> | Copper Heat Core or better, and a Steel Fuel Box because these are block fuels |
| 3 | <ItemLink id="minecraft:blaze_rod" /> | Bronze Heat Core or better |

Any other vanilla item that works in a furnace and leaves nothing behind (so not a Lava Bucket) also counts as a Tier 1 item fuel. Tier 4 has no fuels by default. Modpacks can add fuels to any tier, and a pack that adds charcoal blocks gets them as Tier 2 block fuels automatically.

Item fuels:

<ItemLink id="minecraft:dark_oak_log" />, <ItemLink id="minecraft:dark_oak_wood" />, <ItemLink id="minecraft:stripped_dark_oak_log" /> and 52 more

Compact block fuels, which only a Steel Fuel Box accepts:

<ItemLink id="minecraft:coal_block" />, <ItemLink id="minecraft:dried_kelp_block" />

With JEI installed, click the fuel line on the Process tab to open the Solid Fuel Burning category, which groups fuels by tier and form.
