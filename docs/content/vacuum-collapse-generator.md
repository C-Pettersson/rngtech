# Vacuum Collapse Generator

Status: Prototype

The Vacuum Collapse Generator is a late-stage item-fed FE generator registered as `rngtech:vacuum_collapse_generator`. It consumes recipe-authored catalysts, generates large FE batches, and can produce residue. It uses local instability and a dedicated Dimensional Stabilizer instead of sharing Potential Reactor Containment Linings. High-output setups can install an Energy Connector to raise side FE export beyond the machine's effective `ENERGY_TRANSFER` fallback. A running collapse never pauses for a full buffer: FE that its `20,000 FE` internal buffer and Battery Cell cannot hold is vented, so storage and wiring must keep up with each catalyst.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:vacuum_collapse_generator` | `VacuumCollapseGeneratorBlockEntity` | `VacuumCollapseGeneratorMenu` / `VacuumCollapseGeneratorScreen` | Top catalyst input, side FE extraction, bottom residue output | Item stack and placed machine |

The recipe type is `rngtech:vacuum_collapse`. Recipes declare catalyst input, minimum Void Chamber stage, minimum stability, generated FE, processing ticks, instability, optional residue, and whether residue requires the installed stabilizer. With JEI installed, clicking the Process tab recipe line opens the Vacuum Collapse recipe category.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Void Chamber | Yes | Tungstensteel, Nullite, or Exotic Void Chambers | Sets `PROCESSING_LEVEL`, generation, and stability |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles | Tunes generation, transfer, speed, and stability. `rngtech:nitrogen_separation_nozzle` is rejected. |
| Dimensional Stabilizer | Yes | Tungstensteel, Nullite, or Exotic Dimensional Stabilizers | Improves stability and efficiency |
| Energy Connector | No | Existing Energy Connector items | Sets side FE export when installed; without one, export uses the machine's effective `ENERGY_TRANSFER` |

The current starter catalyst item is `rngtech:void_catalyst`; default recipes can produce `rngtech:collapse_residue`. Void Chamber stage remains the hard recipe gate. If the installed Collapse Nozzle stage is below a recipe's minimum chamber stage, each missing stage adds `+1.0` local instability pressure.

## Balance

The Vacuum Collapse Generator is the Stage 7 primary generator. Its target band is listed in [Component Stages](../reference/component-stages.md).

| Setup | Generated FE/t |
|---|---:|
| Void Catalyst recipe before part multipliers | `1,000 FE/t` (`600,000 FE / 600 ticks`) |
| Unmodified Tungstensteel Void Chamber, Collapse Nozzle, and Dimensional Stabilizer | about `1,057 FE/t` |
| Unmodified Exotic Void Chamber, Collapse Nozzle, and Dimensional Stabilizer | about `3,000 FE/t` |

Void Chamber and Collapse Nozzle generation multiply each other, the nozzle stage raises cycle speed, and the stabilizer stage raises efficiency. Rolled and refined parts carry the machine above these baselines. The Process tab output icon shows the side FE export cap, which is the installed Energy Connector tier, not generated FE/t; the generation icon shows generated FE/t.

## Step-By-Step Guide

1. Craft a Tungstensteel Void Chamber, Tungstensteel Collapse Nozzle, and Tungstensteel Dimensional Stabilizer.
2. Craft the Vacuum Collapse Generator and install all three required parts in the Gear tab.
3. Optionally install an Energy Connector for higher side FE export.
4. Craft Void Catalyst from Ender Pearl, Stabilization Crystal, and Tungstensteel Plate. Echo Shard is also accepted as an alternate catalyst ingredient.
5. Insert Void Catalyst from the top.
6. Keep effective stability at or above the recipe requirement. The default recipe requires `1.0` stability and a Stage 7 Void Chamber.
7. Extract FE from the sides and Collapse Residue from the bottom.
8. Use redstone power to pause generation when downstream storage is full or maintenance is needed. While running, FE the buffer cannot hold is vented.

## Automation

- Top inserts valid catalysts.
- Sides extract FE, capped by the installed Energy Connector tier or by effective `ENERGY_TRANSFER` when no connector is installed.
- Bottom extracts residue.
- Gear slots are manual UI equipment.
- Redstone power pauses generation.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
