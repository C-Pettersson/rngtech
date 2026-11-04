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
| Lava | 3 | 40,000 | Fuel Tank | Bronze Heat Core or better |
| Methane | 4 | 40,000 | Gas Jet | Steel Heat Core |

These FE values are before Gear. Fuel efficiency, fuel duration, and stoke change them as they do for item fuels.

The [Melter](rngtech:melter.md) makes lava and Methane. You can also fill a Fuel Tank with a Lava Bucket from the world.

The burner uses fluid in lots of up to 100 mB. Each lot burns like one fuel item. Stoke, Quick Feed, and Fuel Governor work as usual. The last lot can be smaller, and it gives FE for its size.

The choice of part is a trade. A burner with a tank or jet cannot burn items, and a burner with a Fuel Box cannot burn fluid. Syngas, Carbon Monoxide, Hydrogen, and Ammonia do not burn here. They have their own generators. Modpacks can add fluid fuels.

The fluid stays in the part. Remove a tank and the item keeps its fluid. Install it again and the fluid returns. Items in the fuel slots stay there and burn again when a Fuel Box returns.
