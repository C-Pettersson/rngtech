# Corrosion Cell

Status: Prototype

The Corrosion Cell is a Stage 4 recipe-backed FE generator registered as `rngtech:corrosion_cell`. It consumes one metal plate or `rngtech:scrap` plus either one existing `rngtech:electrolyte` item or `125 mB` of `rngtech:electrolyte_solution`, generates FE over a timed work cycle, and emits deterministic `rngtech:corrosion_residue`.

## Behavior

| Surface | Current implementation |
|---|---|
| Recipe type | `rngtech:corrosion_cell` |
| Process inputs | One plate/scrap slot, one electrolyte item slot, and an internal Electrolyte Solution tank |
| Process output | One residue item slot |
| FE output | Side block energy capability |
| Fluid input | Side fluid fill for Electrolyte Solution when a Fluid Pump is installed |
| Automation | Top inserts plates, sides insert plates or electrolyte items, sides fill Electrolyte Solution with a Fluid Pump installed, sides extract FE, bottom extracts residue |
| Gear | Optional Battery Cell slot and optional Fluid Pump slot |
| Refinement | Item stack and placed-machine Refinement tab |

The machine pauses before consuming another plate and electrolyte when residue cannot fit or its internal buffer plus installed Battery Cell are full. Redstone power pauses active generation.

The tank accepts fluids tagged as `rngtech:electrolytes`; the current default fluid is Electrolyte Solution. A Fluid Pump is not required for item-backed electrolyte recipes, but it enables direct side filling into the tank. The Process tab includes a purge control for the electrolyte tank. Corrosion waste remains item-backed so it can feed later recycling or chemistry recipes without adding a sludge-fluid slice.

Placed block art follows [Machine Visual Design](../reference/machine-visual-design.md). The Corrosion Cell uses owned `textures/block/corrosion_cell/<face>/corrosion_cell` face textures with `64x64` frames, paired plate sockets, an electrolyte channel, a residue tray, insulated bus bars, and a localized active front glow.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Battery Cell | No | Existing Battery Cell items up to Stage 5 | Adds portable FE capacity and participates in FE extraction |
| Fluid Pump | No | Staged Fluid Pump items | Enables side fluid filling and contributes `FLUID_TRANSFER` to the electrolyte fill rate |

Gear slots are UI equipment and are not exposed through the Corrosion Cell's sided item automation.

## Step-By-Step Guide

1. Craft the Corrosion Cell once Steel Casings, Electrolyte, Bus Bar, and a Reinforced Machine Frame are available.
2. Place the block and optionally install a compatible Battery Cell in the Gear tab.
3. Put Iron Plate, Copper Plate, or Scrap in the plate input. Top automation inserts valid plate/scrap inputs; side automation can also route them when using a side connector.
4. Put `rngtech:electrolyte` in the electrolyte input, or install a Fluid Pump and pump Electrolyte Solution into the side fluid capability.
5. Extract FE from the sides and Corrosion Residue from the bottom.
6. Clear the residue slot or attached output before the next cycle; the machine will not consume inputs when residue cannot fit.

## Recipes And JEI

The first recipes use Iron Plate, Copper Plate, or Scrap with `rngtech:electrolyte` and output Corrosion Residue. Stored Electrolyte Solution is treated as the fluid equivalent of the recipe's dry electrolyte item at `125 mB` per cycle. With JEI installed, the Corrosion Cell category shows the plate input, electrolyte input, total FE, processing ticks, minimum material stage, and residue output. In the machine screen, clicking the Process tab recipe line opens the Corrosion Cell recipe category.

## Planned Extensions

- Stronger electrolyte families.
- Reinforced Corrosion Cell or higher-stage recipe gates.
- Electrode Rack Gear if recipes alone do not give enough tuning pressure.
- Corrosion sludge as a fluid in a later fluid-handling slice.
