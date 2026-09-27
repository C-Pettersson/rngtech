# Ore Slurry Turbine

Status: Planned


Planned resource id: `rngtech:ore_slurry_turbine`

## Summary

The Ore Slurry Turbine is a generator that extracts FE from ore-processing slurry, wash residue, or concentrated mineral runoff.

It ties energy production to the ore chain. A player who builds a better processing line can recover some power from byproducts, but the turbine should not replace the main cost of crushing, washing, or refining ore.

## Design Role

The turbine should make ore rooms feel more connected.

It gives the Crusher and future ore-processing machines a reason to produce secondary byproducts. It also gives midgame players a low-maintenance generator that depends on throughput instead of fuel stockpiles.

Primary design goals:

- Recover energy from ore-processing waste.
- Reward full processing chains without making ore duplication free.
- Give material stage and recipe quality a visible effect on power output.
- Use standard fluid or item capabilities instead of adding an item-transfer network.

## Input Contract

RNGTech does not currently define a fluid system for ore slurry. The first implementation can choose either a fluid-backed or item-backed model.

Allowed implementation paths:

| Path | Input Surface | Notes |
|---|---|---|
| Fluid-backed slurry | NeoForge fluid capability | Best fit if RNGTech adds tanks, pipes, or washer output fluids. |
| Item-backed concentrate | Item capability | Good first pass if slurry is represented as buckets, cells, filters, or residue stacks. |

The turbine should accept processed byproducts, not raw ores. Direct raw ore fuel would compete with the Crusher and break the ore-processing loop.

Planned input examples:

- Dirty ore slurry from a future washer.
- Mineral concentrate from crushed ore cleanup.
- Tailings from recipes that trade yield for recovery.
- Stage-specific slurry variants for high-tier ores.

## Power Model

The turbine should generate steady FE/t while it has valid slurry and output space.

Suggested recipe fields:

| Field | Purpose |
|---|---|
| Input slurry or concentrate | Defines the consumed byproduct. |
| FE per unit | Defines total recoverable energy. |
| Flow rate | Defines how quickly input is consumed. |
| Processing time | Defines duration for item-backed recipes. |
| Residue output | Optional inert residue or reusable container. |
| Material stage | Optional progression gate. |

Suggested calculation shape:

```text
effective FE/t = base recipe FE/t * ENERGY_GENERATION * EFFICIENCY
effective input use = base input use / EFFICIENCY
```

`ENERGY_GENERATION` should raise output rate. `EFFICIENCY` should reduce consumed slurry per FE or increase total FE per input. `STABILITY` can reduce surging, clogs, or residue loss.

## Balance Rules

The turbine should remain a recovery machine.

Core balance rules:

- Slurry output should come from ore-processing recipes with a real input cost.
- Turbine output should refund part of the processing chain's power cost, not exceed it by default.
- High-yield ore recipes can produce weaker slurry.
- Low-yield cleanup recipes can produce stronger slurry.
- Materials from higher stages can have better slurry energy, gated by `PROCESSING_LEVEL`.

This keeps the turbine useful in a working factory without creating passive infinite energy from a single machine loop.

## Failure and Maintenance

The turbine can use maintenance pressure instead of fuel scarcity.

Possible failure hooks:

- Low `STABILITY` can cause clog buildup.
- Dirty slurry can create more residue than clean concentrate.
- Overclocked flow can reduce `EFFICIENCY`.
- High-stage slurry can require better turbine parts.

Clogs should slow generation or pause the machine. They should not destroy stored items or fluids without a strong UI warning.

## Gear

The Ore Slurry Turbine should use Gear slots that control flow, clogging, and residue recovery. The first implementation should expose the Rotor, Nozzle, and Filter Mesh slots. Settling Chamber can come with multi-output residue recipes.

| Gear Slot | Required? | Role | Stat and Behavior Hooks |
|---|---|---|---|
| Rotor | Yes | Converts slurry flow into FE and sets the turbine's practical output ceiling. | `ENERGY_GENERATION`, `ENERGY_TRANSFER`, `STABILITY` |
| Nozzle | No | Controls slurry flow rate and pressure handling. | Flow rate, input use, overclock penalty |
| Filter Mesh | No | Reduces clogs and separates useful residue from dirty slurry. | `EFFICIENCY`, clog resistance, residue chance |
| Settling Chamber | No | Holds heavier mineral residue and improves cleanup recipes. | `BUFFER_SIZE`, residue quality, mixed-stage handling |

The Rotor should define the material stage of the turbine. Higher-stage slurry should require a Rotor that can survive the flow pressure, while Filter Mesh and Settling Chamber parts should tune recovery instead of unlocking the recipe by themselves.

## Automation

The Ore Slurry Turbine should use standard capabilities:

- Fluid capability if slurry is implemented as a fluid.
- Item capability if slurry or residue is item-backed.
- Energy capability for FE output.

Suggested sided behavior:

| Side | Behavior |
|---|---|
| Top | Insert slurry items or fluid input. |
| Bottom | Extract residue or empty containers. |
| Sides | Extract FE. |

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine implicit | Identity can carry flow, rotor, or mineral-recovery behavior. |
| Machine prefix | Can affect buffer size, processing level, input handling, or stability. |
| Machine suffix | Can affect `ENERGY_GENERATION`, `EFFICIENCY`, flow rate, and clog resistance. |
| Machine enchant | Reserved for special slurry behavior such as preserving residue or handling mixed-stage input. |
| Machine part modifiers | Rotors, filters, nozzles, and settling chambers can modify flow and clog behavior. |

## Implementation Notes

No Ore Slurry Turbine block, block entity, recipe type, menu, screen, loot table, slurry fluid, or slurry item is currently registered.

A future implementation should decide the slurry representation before adding recipes. If RNGTech adds fluids, the turbine should use NeoForge fluid capabilities. If the first pass uses items, the item model should remain compatible with a later fluid-backed migration.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Crusher](crusher.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Parts](machine-parts.md)
- [Machine Stats](../reference/machine-stats.md)
- [Stage Progression and Ore Duplication](../systems/stage-progression-and-ore-duplication.md)
