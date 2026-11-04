---
navigation:
  title: "Fluid fuels"
  parent: fuel-burner.md
  position: 3
  icon: rngtech:crude_solid_fuel_burner
---

# Fluid fuels

A **Fuel Tank** burns liquids. A **Gas Jet** burns gases. Install one in the Fuel Feed slot in place of a Fuel Box.

| Fuel | Tier | FE per 1,000 mB | Part | Requires |
|---|---:|---:|---|---|
| Lava | 3 | 100,000 | Fuel Tank | Bronze Heat Core or better |
| Methane | 4 | 15,000 | Gas Jet | Steel Heat Core |

These FE values are before Gear. Fuel efficiency, fuel duration, and stoke change them as they do for item fuels.

Before you have a [Melter](rngtech:melter.md), lava is a manual fuel. Bring Lava Buckets from the world and fill a Fuel Tank by hand. One bucket is worth 6.25 Coal, but you must collect each one. Later, the Melter makes lava and Methane. It makes 250 mB of lava per melt, so a lava line needs a steady supply of blaze powder and several burners. This is a use for your old burners.

The burner uses fluid in lots of up to 100 mB. Each lot burns like one fuel item. Stoke, Quick Feed, and Fuel Governor work as usual. The last lot can be smaller, and it gives FE for its size.

The choice of part is a trade. A burner with a tank or jet cannot burn items, and a burner with a Fuel Box cannot burn fluid. Syngas, Carbon Monoxide, Hydrogen, and Ammonia do not burn here. They have their own generators. Modpacks can add fluid fuels.

The fluid stays in the part. Remove a tank and the item keeps its fluid. Install it again and the fluid returns. A Component Recycler refuses a part that holds fluid, so purge the part first.

Each part shows only its own slots. When you install a Fuel Tank or Gas Jet, the items in the fuel slots drop from the burner. When you install a Fuel Box, the container in the container slot drops from the burner.
