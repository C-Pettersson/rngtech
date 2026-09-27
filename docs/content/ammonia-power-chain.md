# Ammonia Power Chain

Status: Prototype

The ammonia chain has two registered machines: `rngtech:ammonia_synthesizer` and `rngtech:ammonia_fuel_cell`. The synthesizer is a powered support processor that converts nitrogen and hydrogen into ammonia. The fuel cell is the generator that consumes ammonia for FE.

## Runtime Surface

| Content | Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|---|
| Ammonia Synthesizer | `rngtech:ammonia_synthesizer` | `AmmoniaSynthesizerBlockEntity` | `AmmoniaSynthesizerMenu` / `AmmoniaSynthesizerScreen` | Side FE input, fluid input, ammonia fluid output | Item stack and placed machine |
| Ammonia Fuel Cell | `rngtech:ammonia_fuel_cell` | `AmmoniaFuelCellBlockEntity` | `AmmoniaFuelCellMenu` / `AmmoniaFuelCellScreen` | Ammonia fluid input, top/side FE extraction, bottom residue extraction | Item stack and placed machine |

Ammonia synthesis uses the `rngtech:ammonia_synthesis` recipe type. Ammonia generation uses the `rngtech:ammonia_power_cycle` recipe type. JEI exposes both categories. In the Ammonia Fuel Cell screen, clicking the Process tab recipe line opens the Ammonia Power Cycle recipe category.

## Gear

| Machine | Slot | Required | Accepted item | Role |
|---|---|---:|---|---|
| Ammonia Synthesizer | Catalyst Bed | Yes | `rngtech:ammonia_catalyst_bed` | Gates synthesis recipes and contributes processing, efficiency, stability, and fluid transfer |
| Ammonia Fuel Cell | Fuel Cell Membrane | Yes | `rngtech:fuel_cell_membrane` | Gates ammonia power recipes and contributes generation, transfer, speed, efficiency, and stability |
| Ammonia Fuel Cell | Battery Cell | No | Stage 6+ Battery Cells | Adds portable FE capacity and output storage |

The ammonia synthesizer owns nitrogen, hydrogen, and ammonia tanks. The fuel cell owns one ammonia input tank and one residue output slot. The default code-backed Nitrogen route is the Cavitation Generator's Stage 6 water separation recipe with `rngtech:nitrogen_extraction_rotor` and `rngtech:nitrogen_separation_nozzle`; the default Hydrogen route is Steam Methane Reforming.

The Process tabs have purge buttons beside the visible gas tanks. The craftable `rngtech:purge_bucket` can also right-click these placed machines to void up to `1000 mB`; normal use prefers input tanks, while sneak-use on the Synthesizer prefers the Ammonia output tank. Purging Synthesizer Nitrogen/Hydrogen or Fuel Cell Ammonia resets that machine's active work state before draining.

Gas Chemistry machines follow the same purge rules. Coal Gasifier exposes Water and Syngas purge targets, Syngas Combustor exposes gas input and Carbon Exhaust purge targets, and Steam Methane Reformer exposes Water, gas input, Hydrogen output, and Carbon Monoxide output purge targets. The Syngas Combustor burns Syngas for its main Stage 5 gas-power route and also accepts Carbon Monoxide as a weaker disposal fuel. Nullite Servo Auto Purge applies to Gas Chemistry output tanks when produced fluid overflows matching stored fluid, including the Syngas Combustor's Carbon Exhaust tank.

## Step-By-Step Guide

1. Craft an Ammonia Catalyst Bed.
2. Craft and place the Ammonia Synthesizer, then install the Catalyst Bed in its Gear tab.
3. Feed Nitrogen from Cavitation and Hydrogen from Steam Methane Reforming into the Synthesizer fluid handler, then feed FE from a side. The default recipe consumes `500 mB` Nitrogen, `1,500 mB` Hydrogen, and `12,000 FE` to produce `1,000 mB` Ammonia.
4. Craft a Fuel Cell Membrane.
5. Craft and place the Ammonia Fuel Cell, then install the membrane in Gear.
6. Optionally install a Stage 6+ Battery Cell in the Fuel Cell for output buffering.
7. Move Ammonia from the Synthesizer to the Fuel Cell.
8. Extract FE from Fuel Cell top or sides and residue from the bottom.

## Automation

- The synthesizer receives FE from non-bottom sides, fills nitrogen or hydrogen through the fluid capability, and drains ammonia from its output tank.
- The synthesizer exposes its Catalyst Bed slot only from the top; normal item automation has no process item slots.
- The fuel cell accepts ammonia through the fluid capability, extracts FE from the top or sides, and extracts residue from the bottom.
- Gear slots are manual UI equipment except the synthesizer Catalyst Bed top handler.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
