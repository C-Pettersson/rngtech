---
navigation:
  title: "Assembled cells"
  parent: battery-cells.md
  position: 6
  icon: rngtech:iron_battery_cell
---

# Assembled cells

Every standard cell from Iron to Exotic can be made in the [Component Assembler](rngtech:component-assembler.md) in two steps:

1. Assemble a <ItemLink id="rngtech:battery_cell" /> from a Cell Shell, a Conductive Plate, and Electrolyte Solution. Primed Cell Cores stack and hold no power.
2. Assemble the final cell from one Primed Cell Core, the material's plate or ingot, and Redstone. Sparksteel and higher cells also need a Stabilization Catalyst.

The finished cell starts empty and rolls its traits when assembly completes. The [Component Assembler](rngtech:component-assembler.md) page lists every assembly recipe.

| Input | Output | Time | Energy | Rolls traits |
|---|---|---|---|---|
| <ItemLink id="rngtech:battery_cell" /><br />**Sparksteel Plate**<br /><ItemLink id="minecraft:redstone" /><br />**Stabilization Catalyst** | <ItemLink id="rngtech:sparksteel_battery_cell" /> | 11 s | 3,200 FE | Yes |

Unwanted standard cells can be broken down in a [Component Recycler](rngtech:component-recycler.md).
