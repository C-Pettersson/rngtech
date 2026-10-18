---
navigation:
  title: "Melting"
  parent: melter.md
  position: 0
  icon: rngtech:melter
---

# Melting

Fill the input tank with the recipe's fluid, put the two recipe items in the input slots, and supply FE. The two items can go in either slot, and a recipe that needs two of the same item takes both from one slot. Each melt uses 1,000 mB of fluid and puts 1,000 mB of the product into the output tank, except Ender Slurry from Collapse Residue, which makes 2,000 mB. Ender Slurry dissolves its items in [Ammonia](rngtech:ammonia-synthesizer.md); every other default recipe uses water.

Some ingredients leave an item behind, such as the Glass Bottle from a <ItemLink id="minecraft:honey_bottle" />. A leftover the Melter cannot use goes into an empty output container slot if it can hold the product, and otherwise into the inventory directly below the Melter, such as a hopper or chest, or onto the ground if there is none. It never blocks the input slots; see [Automation](rngtech:automation/ingredient-leftovers.md).

The crucible heats up like the other heated machines and melts once it reaches the recipe's temperature; see [Heat](rngtech:heat.md). Every default recipe needs at least 1,200 heat (methane needs 1,400, and Ender Slurry from Collapse Residue needs 1,600) and hardness level 6, so you need a hot enough Heat Core and a Titanium or better Crush Head. Unlike the [Crusher](rngtech:crusher.md), the Melter does not run recipes above its hardness at all.

Methane takes plain **Algae Biomass**. **Dense Algae Biomass** does not work in the Melter; burn it in a [Bio Generator](rngtech:bio-generator.md) instead.

You can fill the input tank with buckets or other fluid containers in the Process tab, or pipe fluid in from the sides. To take fluid out, fill an empty container in the output container slot, or pull from the sides once a Fluid Pump is installed. If you put the wrong fluid in, use the purge buttons on the Process tab, or right-click the Melter with a **Purge Bucket** to void up to 1,000 mB (sneak to purge the output tank). Purging the input tank resets the current melt.

Both tanks hold 4,000 mB to start. When the output tank is full, the Melter stops. A Nullite Servo instead voids the overflow so it keeps running.
