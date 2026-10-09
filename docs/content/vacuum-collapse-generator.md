# Vacuum Collapse Generator

Status: Prototype

Player guide: [Vacuum Collapse Generator](https://c-pettersson.github.io/rngtech/vacuum-collapse-generator/)

The Vacuum Collapse Generator (`rngtech:vacuum_collapse_generator`) is the Stage 7 item-fed FE generator. It uses local instability and a dedicated Dimensional Stabilizer instead of sharing Potential Reactor Containment Linings. A running collapse never pauses for a full buffer: FE that its `20,000 FE` internal buffer cannot hold is vented.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:vacuum_collapse_generator` | `VacuumCollapseGeneratorBlockEntity` | `VacuumCollapseGeneratorMenu` / `VacuumCollapseGeneratorScreen` | Top catalyst input, side FE extraction, bottom residue output | Item stack and placed machine |

The recipe type is `rngtech:vacuum_collapse`. Recipes declare catalyst input, minimum Void Chamber stage, minimum stability, generated FE, processing ticks, instability, optional residue, and whether residue requires the installed stabilizer.

Side FE export is capped by the installed Energy Connector tier, or by effective `ENERGY_TRANSFER` when no connector is installed. Gear slots are not exposed to automation. Redstone power pauses generation.

## Gear

| Slot | Required | Accepted item | Role |
|---|---:|---|---|
| Void Chamber | Yes | Tungstensteel, Nullite, or Exotic Void Chambers | Sets `PROCESSING_LEVEL`, generation, and stability |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles | Tunes generation, transfer, speed, and stability. `rngtech:nitrogen_separation_nozzle` is rejected. |
| Dimensional Stabilizer | Yes | Tungstensteel, Nullite, or Exotic Dimensional Stabilizers | Improves stability and efficiency |
| Energy Connector | No | Existing Energy Connector items | Sets side FE export when installed; without one, export uses the machine's effective `ENERGY_TRANSFER`. The cap is per tick, shared between the generator's own push and connector pulls |

The starter catalyst is `rngtech:void_catalyst`; default recipes can produce `rngtech:collapse_residue`. Void Chamber stage is the hard recipe gate. If the installed Collapse Nozzle stage is below a recipe's minimum chamber stage, each missing stage adds `+1.0` local instability pressure.

## Balance

The Vacuum Collapse Generator is the Stage 7 primary generator. Its target band is listed in [Component Stages](../reference/component-stages.md).

| Setup | Generated FE/t |
|---|---:|
| Void Catalyst recipe before part multipliers | `1,000 FE/t` (`600,000 FE / 600 ticks`) |
| Unmodified Tungstensteel Void Chamber, Collapse Nozzle, and Dimensional Stabilizer | about `1,057 FE/t` |
| Unmodified Exotic Void Chamber, Collapse Nozzle, and Dimensional Stabilizer | about `3,000 FE/t` |

Void Chamber and Collapse Nozzle generation multiply each other, the nozzle stage raises cycle speed, and the stabilizer stage raises efficiency. Rolled and refined parts carry the machine above these baselines.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
