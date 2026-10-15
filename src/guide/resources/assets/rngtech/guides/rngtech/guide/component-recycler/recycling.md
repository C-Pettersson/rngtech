---
navigation:
  title: "Recycling"
  parent: component-recycler.md
  position: 0
  icon: rngtech:iron_component_recycler
---

# Recycling

Put the item to recycle in the input slot. When it finishes, its returns go to the three output slots. Each recyclable item has its own fixed return list; the Recycler never reverses arbitrary crafting recipes, and no return is ever larger than what the original craft used. Common returns:

| Item recycled | Guaranteed return | With a Recovery Filter |
|---|---|---|
| Tool Head | Flint or one material gear | Scrap or one material plate |
| Tool Rod | Sticks or one material rod | A button or one material plate |
| Machine chassis | One frame or chassis ingredient plus one material or functional ingredient | Another ingredient, such as a circuit, coil, or bus bar |
| Machine part | One gear, coil, core, plate, casing, or other functional part | Another ingredient |
| Calibrated component | Its main raw ingredient, such as the Iron Gear from a **Calibrated Kinetic Component** | Nothing extra |
| Battery Cell | The cell core or shell plus one material ingredient | One plate or stabilizing ingredient |
| Resonance Coil, Control Board, or Stabilizer Matrix | Its core part (Energy Coil, Control Board, or Matrix) plus one matching ingot | Redstone, or a Stabilization Catalyst for a Matrix |
| Alloy Crucible | The previous crucible, or a Bronze Casing for the Bronze one, plus two ingots of its metal | Two more ingots |
| **Cavitation Rotor Hub** | Its Steel Casing, at Stage 5 | Nothing extra |
| **Pitted Cavitation Rotor** | Recycling Byproduct only, at Stage 5 | Nothing extra |

Calibrated components return their raw ingredient, so a low-stability roll from the [Resonance Calibrator](rngtech:resonance-calibrator.md) is not wasted: recycle it and try again. Patterns, catalysts, and stabilizers used in the calibration are not returned. Machines and parts that were crafted from calibrated components return the uncalibrated ingredient instead. Calibrated Conductive Components always return a Copper Coil, even ones calibrated from a Sparksteel Coil.
