# Corrosion Cell

Status: Prototype

Player guide: [Corrosion Cell](https://c-pettersson.github.io/rngtech/corrosion-cell/)

The Corrosion Cell (`rngtech:corrosion_cell`) is a Stage 4 recipe-backed FE generator that consumes a plate, Anode, or `rngtech:scrap` plus electrolyte and emits `rngtech:corrosion_residue`.

## Implementation Contract

| Surface | Current implementation |
|---|---|
| Recipe type | `rngtech:corrosion_cell` |
| Process inputs | One plate/anode/scrap slot, one electrolyte item slot, and an internal Electrolyte Solution tank |
| Process output | One residue item slot |
| FE output | Side block energy capability with no machine-side export cap; the attached receiver or [Universal Connector tier](basic-wire.md#energy-transfer-limits) decides the rate, and the Output readout shows the last tick's actual FE output |
| Fluid input | Side fluid fill for Electrolyte Solution when a Fluid Pump is installed |
| Automation | Top inserts plates, sides insert plates or electrolyte items, sides fill Electrolyte Solution with a Fluid Pump installed, sides extract FE, bottom extracts residue |
| Gear | Optional Battery Cell, Fluid Pump, and Cathode slots; not exposed through sided item automation |
| Refinement | Item stack and placed-machine Refinement tab |

The tank accepts fluids tagged `rngtech:electrolytes`; Electrolyte Solution is the current default. Stored solution substitutes for the recipe's dry electrolyte item at `125 mB` per cycle. The tank holds `4,000 mB`, enlarged by the pump's Fluid Capacity affixes (flat Brimming, percent Depth). Corrosion waste stays item-backed so later recycling or chemistry recipes can consume it without a sludge-fluid slice.

Placed block art follows [Machine Visual Design](../reference/machine-visual-design.md). The Corrosion Cell uses owned `textures/block/corrosion_cell/<face>/corrosion_cell` face textures with `64x64` frames, paired plate sockets, an electrolyte channel, a residue tray, insulated bus bars, and a localized active front glow.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Battery Cell | No | Existing Battery Cell items up to Stage 5 | Adds portable FE capacity and participates in FE extraction |
| Fluid Pump | No | Staged Fluid Pump items | Enables side fluid filling and contributes `FLUID_TRANSFER` to the electrolyte fill rate |
| Cathode | No | Aluminum, Titanium, Tungstensteel, and Naquadah Cathodes (Stage 5-8) | Sets the highest Anode stage the cell can burn. Rolls Energy Generation, Efficiency, Processing Speed, and Stability affixes, which apply to the cell, and adds `+2%` base Efficiency per stage above Aluminum |

## Fuel Balance

An Anode is two plates of one metal assembled with `250 mB` Electrolyte Solution and `4,800 FE` in the [Component Assembler](battery-assembler.md).

| Fuel | Stage | FE per cycle | Ticks | FE/t | Cathode |
|---|---:|---:|---:|---:|---|
| Scrap | 4 | `4,800` | `160` | `30` | None |
| Iron Plate | 4 | `12,800` | `160` | `80` | None |
| Copper Plate | 4 | `16,000` | `160` | `100` | None |
| Aluminum Plate | 5 | `12,800` | `160` | `80` | None |
| Aluminum Anode | 5 | `64,000` | `320` | `200` | Stage 5+ |
| Titanium Plate | 6 | `30,720` | `160` | `192` | None |
| Titanium Anode | 6 | `153,600` | `320` | `480` | Stage 6+ |
| Tungstensteel Plate | 7 | `57,600` | `160` | `360` | None |
| Tungstensteel Anode | 7 | `288,000` | `320` | `900` | Stage 7+ |
| Naquadah Plate | 8 | `115,200` | `160` | `720` | None |
| Naquadah Anode | 8 | `576,000` | `320` | `1,800` | Stage 8 |

Values are before machine and Cathode rolls. Each Anode stays below that stage's main generator after its assembly cost: about `107`, `357`, `597`, and `1,587 FE/t` net for Stages 5-8.

## Planned Extensions

- Stronger electrolyte families.
- Reinforced Corrosion Cell body.
- Corrosion sludge as a fluid in a later fluid-handling slice.
