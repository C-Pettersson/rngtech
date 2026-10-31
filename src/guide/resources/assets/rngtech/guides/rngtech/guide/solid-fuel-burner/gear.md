---
navigation:
  title: "Gear"
  parent: solid-fuel-burner.md
  position: 4
  icon: rngtech:crude_solid_fuel_burner
---

# Gear

The Gear tab has three required slots:

- **Heat Core**: sets base FE/t and the highest fuel tier. The core's stage cannot exceed the chassis stage.
- **Fuel Box**: sets the number of fuel slots, which fuel forms are accepted, and fuel behaviors.
- **Battery Cell**: stores the generated FE. Stage 0–4 cells fit, up to the chassis stage. In a burner, the cell only adds storage and does not cap output.

| Heat Core | Base output | Fuel efficiency | Heat isolation | Max fuel tier |
|---|---:|---:|---:|---:|
| <ItemLink id="rngtech:iron_heat_core" /> | 24 FE/t | 0.85x | 0.85x | 1 |
| <ItemLink id="rngtech:copper_heat_core" /> | 40 FE/t | 0.80x | 0.75x | 2 |
| <ItemLink id="rngtech:bronze_heat_core" /> | 64 FE/t | 1.00x | 1.00x | 3 |
| <ItemLink id="rngtech:steel_heat_core" /> | 96 FE/t | 1.15x | 1.20x | 4 |

Heat isolation below 1.00x leaks heat: part of each fuel item's burn time is lost while it burns, so Iron and Copper Heat Cores waste some fuel. At 1.00x or above, nothing leaks.

| Fuel Box | Fuel slots | Fuel efficiency | Forms | Behaviors |
|---|---:|---:|---|---|
| <ItemLink id="rngtech:iron_fuel_box" /> | 1 | 0.95x | Item fuels | None |
| <ItemLink id="rngtech:copper_fuel_box" /> | 1 | 0.90x | Item fuels | Fuel Governor, Quick Feed |
| <ItemLink id="rngtech:bronze_fuel_box" /> | 2 | 1.00x | Item fuels | Fuel Governor, Fuel Reserve |
| <ItemLink id="rngtech:steel_fuel_box" /> | 2 | 1.10x | Item and block fuels | Fuel Governor, Block Feed |

The Heat Core's and Fuel Box's fuel efficiency multiply together.

What the behaviors do:

- **Fuel Governor**: when the Battery Cell is full, the burner pauses instead of burning fuel for nothing. The current fuel item stops burning and loses no burn time to heat leakage, the burner keeps exporting FE, and it picks up where it left off once the cell has room. An Iron Fuel Box has no governor, so it keeps burning and wastes fuel while the cell is full. See [Fuel Governor](rngtech:rarity-and-affixes.md#fuel-governor).
- **Quick Feed**: the burner turns its stoke down as the Battery Cell fills past half, so each fuel item gives more FE while you do not need the power. See [Stoke](rngtech:solid-fuel-burner/stoke.md). Comes from the Copper burner, Copper Heat Core, and Copper Fuel Box. Any one of them is enough.
- **Fuel Reserve**: the burner leaves the last fuel item in its slot untouched, so an automated feed line never runs completely dry. Comes from the Alloy burner and Bronze Fuel Box.
- **Block Feed**: lets the burner accept compact block fuels such as Blocks of Coal.
