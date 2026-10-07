# Corrosion Cell

Status: Prototype

The Corrosion Cell is a Stage 4 recipe-backed FE generator registered as `rngtech:corrosion_cell`. It consumes one metal plate, assembled Anode, or `rngtech:scrap` plus either one existing `rngtech:electrolyte` item or `125 mB` of `rngtech:electrolyte_solution`, generates FE over a timed work cycle, and emits deterministic `rngtech:corrosion_residue`.

## Behavior

| Surface | Current implementation |
|---|---|
| Recipe type | `rngtech:corrosion_cell` |
| Process inputs | One plate/anode/scrap slot, one electrolyte item slot, and an internal Electrolyte Solution tank |
| Process output | One residue item slot |
| FE output | Side block energy capability |
| Fluid input | Side fluid fill for Electrolyte Solution when a Fluid Pump is installed |
| Automation | Top inserts plates, sides insert plates or electrolyte items, sides fill Electrolyte Solution with a Fluid Pump installed, sides extract FE, bottom extracts residue |
| Gear | Optional Battery Cell, Fluid Pump, and Cathode slots |
| Refinement | Item stack and placed-machine Refinement tab |

The machine pauses before consuming another plate and electrolyte when residue cannot fit or its internal buffer plus installed Battery Cell are full. Redstone power pauses active generation.

The tank accepts fluids tagged as `rngtech:electrolytes`; the current default fluid is Electrolyte Solution. A Fluid Pump is not required for item-backed electrolyte recipes, but it enables direct side filling into the tank. The tank holds `4,000 mB`; a pump's Fluid Capacity affixes (flat Brimming, percent Depth) enlarge it. The Process tab includes a purge control for the electrolyte tank. Corrosion waste remains item-backed so it can feed later recycling or chemistry recipes without adding a sludge-fluid slice.

Placed block art follows [Machine Visual Design](../reference/machine-visual-design.md). The Corrosion Cell uses owned `textures/block/corrosion_cell/<face>/corrosion_cell` face textures with `64x64` frames, paired plate sockets, an electrolyte channel, a residue tray, insulated bus bars, and a localized active front glow.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Battery Cell | No | Existing Battery Cell items up to Stage 5 | Adds portable FE capacity and participates in FE extraction |
| Fluid Pump | No | Staged Fluid Pump items | Enables side fluid filling and contributes `FLUID_TRANSFER` to the electrolyte fill rate |
| Cathode | No | Aluminum, Titanium, Tungstensteel, and Naquadah Cathodes (Stage 5-8) | Sets the highest Anode stage the cell can burn. Rolls Energy Generation, Efficiency, Processing Speed, and Stability affixes, which apply to the cell, and adds `+2%` base Efficiency per stage above Aluminum |

Gear slots are UI equipment and are not exposed through the Corrosion Cell's sided item automation.

## Fuel Tiers

Plates burn directly. From Stage 5 the main fuel is an Anode: two plates of one metal assembled with `250 mB` Electrolyte Solution and `4,800 FE` in the [Component Assembler](battery-assembler.md). Burning an Anode needs a Cathode of at least its stage. A higher-tier plate still burns without a Cathode, at about 40% of its Anode's FE/t.

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

## Step-By-Step Guide

1. Craft the Corrosion Cell once Steel Casings, Electrolyte, Bus Bar, and a Reinforced Machine Frame are available.
2. Place the block and optionally install a compatible Battery Cell in the Gear tab.
3. Put a plate, Anode, or Scrap in the plate input. Anodes need a Cathode of at least their stage in the Gear tab. Top automation inserts valid plate/scrap inputs; side automation can also insert them when using a side connector.
4. Put `rngtech:electrolyte` in the electrolyte input, or install a Fluid Pump and pump Electrolyte Solution into the side fluid capability.
5. Extract FE from the sides and Corrosion Residue from the bottom.
6. Clear the residue slot or attached output before the next cycle; the machine will not consume inputs when residue cannot fit.

## Recipes And JEI

Recipes use Scrap, Iron, Copper, Aluminum, Titanium, Tungstensteel, or Naquadah plates, or Aluminum to Naquadah Anodes, with `rngtech:electrolyte`, and output Corrosion Residue. Stored Electrolyte Solution is treated as the fluid equivalent of the recipe's dry electrolyte item at `125 mB` per cycle. With JEI installed, the Corrosion Cell category shows the plate input, electrolyte input, total FE, processing ticks, minimum material stage, required Cathode stage, and residue output. In the machine screen, clicking the Process tab recipe line opens the Corrosion Cell recipe category.

## Planned Extensions

- Stronger electrolyte families.
- Reinforced Corrosion Cell body.
- Corrosion sludge as a fluid in a later fluid-handling slice.
