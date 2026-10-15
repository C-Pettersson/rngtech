---
navigation:
  title: "Load"
  parent: corrosion-cell.md
  position: 1
  icon: rngtech:corrosion_cell
---

# Load

The load is the share of its rated current the cell draws. Each fuel holds a fixed amount of charge, so the load sets how fast the fuel dissolves. A lighter load also runs the cell at a higher voltage, so each fuel is worth more FE. Rated load (100%) makes the most FE/t and gives exactly the recipe's FE and cycle time. New cells start there.

Lower loads trade FE/t for FE per fuel. Two cells at 50% together make 1.125 times the FE/t of one cell at rated load, and they get 12.5% more FE out of every fuel. That pays off when fuel, such as Anodes, runs short before cells do. Each fuel still uses one Electrolyte and leaves one Corrosion Residue, so a lighter load also gets more FE from each Electrolyte.

| Load | FE/t | FE per fuel | Cycle length |
|---|---|---|---|
| 40% | x0.46 | x1.15 | x2.50 |
| 50% | x0.56 | x1.125 | x2.00 |
| 60% | x0.66 | x1.10 | x1.67 |
| 70% | x0.75 | x1.075 | x1.43 |
| 80% | x0.84 | x1.05 | x1.25 |
| 90% | x0.92 | x1.025 | x1.11 |
| 100% (rated) | x1.00 | x1.00 | x1.00 |

Set the load on the gauge below the cycle bar in the Process tab. Click a point on it, or scroll over it to move 5%. The load runs from 40% to 100%; the table shows every 10%. The gauge draws the trade: the teal profile is the FE/t at each load, lit up to the current the cell draws right now, and the amber line is the FE per fuel. The white pointer marks your setting. Hover the gauge for the exact numbers and the cell voltage. A load change applies to the rest of the fuel already in the bath.
