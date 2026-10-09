# Battery Cells

Status: Prototype

Player guide: [Battery Cells](https://c-pettersson.github.io/rngtech/battery-cells/)

Gameplay resource ids:

- `rngtech:potato_battery_cell`
- `rngtech:unique_potato_battery_cell`
- `rngtech:iron_battery_cell`
- `rngtech:copper_battery_cell`
- `rngtech:lead_battery_cell`
- `rngtech:invar_battery_cell`
- `rngtech:sparksteel_battery_cell`
- `rngtech:arclite_battery_cell`
- `rngtech:nullite_battery_cell`
- `rngtech:aethergold_battery_cell`
- `rngtech:exotic_battery_cell`

Stackable inert core id:

- `rngtech:battery_cell` displayed as Primed Cell Core

The generic `rngtech:battery_cell` id is the stackable non-energy intermediate used by the [Component Assembler](battery-assembler.md). It is not a chargeable gameplay cell.

## Summary

Battery cells are chargeable item components installed into machine Gear slots and [Battery Chassis](battery-chassis.md). Cells provide stored FE, input rate, output rate, and cell-level energy traits. Solid fuel-burning generators use installed cells as capacity buffers only; generator FE/t and export are not capped by the cell rates or cell efficiency.

## Behavior

Battery cells expose the standard NeoForge item energy capability so portable use, Battery Chassis, machine Gear slots, and compatible modded energy systems can charge or discharge them. Inside solid fuel-burning generators, generated FE is written directly to the installed cell as a capacity buffer.

Current runtime surface:

- Java item type: `BatteryCellItem`.
- Each material variant stacks to one and stores FE in the `rngtech:battery_cell_energy` data component.
- Standard cells roll `MachineType.BATTERY_CELL` traits when crafted, assembled, or otherwise created through the item production hook and can be refined through the Affix Forge.
- `rngtech:unique_potato_battery_cell` has fixed `UNIQUE` rarity, renders with item glint, cannot be crafted or refined, and can appear as rare village chest loot.
- Assembled final cells start at zero FE. Their final traits are rolled once on completion, which is why final cells remain unstackable.
- The Primed Cell Core stacks, stores no FE, exposes no item energy capability, carries no RPG traits, and is not a refinement target.

Cells do not have burst. `BURST_TRANSFER` and `BURST_DURATION` belong to the [Battery Chassis](battery-chassis.md), and even chassis burst output is still capped by the combined output rate of inserted cells.

## Base Cell Values

These values use a staged rechargeable battery ladder as the unmodified baseline. `Capacity` is the base FE stored by one cell before prefixes. `Input` and `Output` are per-cell rate limits, with normal cells accepting twice their output rate. `Efficiency` uses `1.0` as neutral. `Idle Loss` is passive leakage from that cell's stored FE. Cell efficiency is shown in the cell tooltip only; no transfer path reads it.

| Cell | Stage | Capacity | Input | Output | Efficiency | Idle Loss |
|---|---:|---:|---:|---:|---:|---:|
| Potato Cell | 0 | `1,000 FE` | `8 FE/t` | `4 FE/t` | `0.25` | `12%/min` |
| Voltaic Potato Cell | 0 | `10,000 FE` | `128 FE/t` | `64 FE/t` | `0.60` | `6%/min` |
| Iron Cell | 1 | `10,000 FE` | `128 FE/t` | `64 FE/t` | `1.00` | `0%/min` |
| Copper Cell | 2 | `12,000 FE` | `256 FE/t` | `128 FE/t` | `0.98` | `0.01%/min` |
| Lead Cell | 3 | `32,000 FE` | `256 FE/t` | `128 FE/t` | `1.00` | `0%/min` |
| Invar Cell | 4 | `50,000 FE` | `512 FE/t` | `256 FE/t` | `1.02` | `0%/min` |
| Sparksteel Cell | 5 | `100,000 FE` | `1024 FE/t` | `512 FE/t` | `1.04` | `0.01%/min` |
| Arclite Cell | 6 | `400,000 FE` | `2048 FE/t` | `1024 FE/t` | `0.99` | `0.02%/min` |
| Nullite Cell | 7 | `1,600,000 FE` | `4096 FE/t` | `2048 FE/t` | `1.02` | `0.02%/min` |
| Aethergold Cell | 7 | `1,200,000 FE` | `6144 FE/t` | `3072 FE/t` | `1.03` | `0%/min` |
| Exotic Cell | 8 | `10,000,000 FE` | `8192 FE/t` | `4096 FE/t` | `1.06` | `0%/min` |

## Effective Chassis Rate

Cells limit the actual input and output rate of a chassis. See [Battery Chassis](battery-chassis.md#effective-transfer).

## Capacity Prefixes

Battery cells can roll both flat capacity and increased capacity prefixes.

Both of these modifier families are prefixes:

| Prefix Type | Operation | Example |
|---|---|---|
| Flat capacity | `ADD ENERGY_CAPACITY_FLAT` | `+4,000 ENERGY_CAPACITY_FLAT` |
| Increased capacity | `INCREASED_PERCENT ENERGY_CAPACITY` | `+20% increased ENERGY_CAPACITY` |

Capacity, transfer, efficiency, and idle loss are authored as a Battery Cell base profile, then stored Battery Cell affixes are evaluated source-locally on that cell. Capacity calculation applies flat capacity before increased capacity:

```text
effective cell capacity = (base capacity + flat capacity prefixes) * (1 + increased capacity prefixes)
```

Capacity prefixes affect the cell item. A battery chassis uses the resulting effective capacities when summing inserted cell storage.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Base profile | Cell material identity defines capacity, input transfer, output transfer, efficiency, and idle loss through `ComponentBaseStatCatalog`. These values are not removable affixes. |
| Prefix | Flat capacity, increased capacity, and efficiency can roll. |
| Suffix | Transfer and leakage modifiers can roll. |
| Enchant | See [Modifier Eligibility](../reference/modifier-eligibility.md). |

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Component Assembler](battery-assembler.md)
- [Battery Chassis](battery-chassis.md)
- [Component Stages](../reference/component-stages.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
