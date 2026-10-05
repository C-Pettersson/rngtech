# Battery Cells

Status: Prototype


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

Battery cells are chargeable item components installed into machines and storage blocks, including the [Crusher](crusher.md), [Battery Chassis](battery-chassis.md), and solid fuel-burning generators. Potato, Iron, Copper, and Lead cells have direct crafting-table recipes that consume dry `rngtech:electrolyte`; standard material cells can also be assembled in the [Component Assembler](battery-assembler.md) from Primed Cell Cores.

Cells provide stored FE, input rate, output rate, and cell-level energy traits. Battery Chassis uses the summed cell rates as storage throughput. Solid fuel-burning generators use installed cells as capacity buffers only; generator FE/t and export are not capped by the cell input/output rate or cell efficiency.

## Behavior

Battery cells expose the standard NeoForge item energy capability so portable use, Battery Chassis, machine Gear slots, and compatible modded energy systems can charge or discharge them. Inside solid fuel-burning generators, generated FE is written directly to the installed cell as a capacity buffer.

Current runtime surface:

- Java item type: `BatteryCellItem`.
- Each material variant stacks to one and stores FE in the `rngtech:battery_cell_energy` data component.
- Standard cells roll `MachineType.BATTERY_CELL` traits when crafted, assembled, or otherwise created through the item production hook and can be refined through the Affix Forge.
- `rngtech:unique_potato_battery_cell` has fixed `UNIQUE` rarity, renders with item glint, cannot be crafted or refined, carries the Voltaic Spud identity, and can appear as rare village chest loot.
- Tooltips show material, stored FE, effective capacity, input, output, efficiency, idle loss, authored base stats, rarity, Refinement Potential, fixed identity, and rolled modifiers.
- The item durability-style bar shows stored charge.
- Assembled final cells start at zero FE. Their final traits are rolled once on completion, which is why final cells remain unstackable.
- The Primed Cell Core stacks, stores no FE, exposes no item energy capability, carries no RPG traits, and is not a refinement target.

Cells control:

- Stored FE capacity.
- Maximum input rate.
- Maximum output rate.
- Cell efficiency.
- Cell idle loss or leakage.
- Cell-level modifier rolls.

Cells do not have burst. `BURST_TRANSFER` and `BURST_DURATION` belong to the [Battery Chassis](battery-chassis.md), and even chassis burst output is still capped by the combined output rate of inserted cells.

## Base Cell Values

These values use a staged rechargeable battery ladder as the unmodified baseline. `Capacity` is the base FE stored by one cell before prefixes. `Input` and `Output` are per-cell rate limits, with normal cells accepting twice their output rate. `Efficiency` uses `1.0` as neutral. `Idle Loss` is passive leakage from that cell's stored FE.

| Cell | Stage | Capacity | Input | Output | Efficiency | Idle Loss | Base identity |
|---|---:|---:|---:|---:|---:|---:|---|
| Potato Cell | 0 | `1,000 FE` | `8 FE/t` | `4 FE/t` | `0.25` | `12%/min` | `Spud Cell`: novelty storage with aggressive leakage. |
| Voltaic Potato Cell | 0 | `10,000 FE` | `128 FE/t` | `64 FE/t` | `0.60` | `6%/min` | `Voltaic Spud`: Unique novelty storage with fixed `UNIQUE` rarity and no refinement. |
| Iron Cell | 1 | `10,000 FE` | `128 FE/t` | `64 FE/t` | `1.00` | `0%/min` | `Iron Cell`: early practical storage with no special behavior. |
| Copper Cell | 2 | `12,000 FE` | `256 FE/t` | `128 FE/t` | `0.98` | `0.01%/min` | `Conductive Cell`: acid-battery capacity with light leakage. |
| Lead Cell | 3 | `32,000 FE` | `256 FE/t` | `128 FE/t` | `1.00` | `0%/min` | `Dense Cell`: higher capacity with practical transfer. |
| Invar Cell | 4 | `50,000 FE` | `512 FE/t` | `256 FE/t` | `1.02` | `0%/min` | `Insulated Cell`: high-density storage and good durability. |
| Sparksteel Cell | 5 | `100,000 FE` | `1024 FE/t` | `512 FE/t` | `1.04` | `0.01%/min` | `Balanced Cell`: strong transfer and efficiency. |
| Arclite Cell | 6 | `400,000 FE` | `2048 FE/t` | `1024 FE/t` | `0.99` | `0.02%/min` | `Signal Cell`: advanced throughput with mild leakage. |
| Nullite Cell | 7 | `1,600,000 FE` | `4096 FE/t` | `2048 FE/t` | `1.02` | `0.02%/min` | `Phase Cell`: late-game storage and high-rate output. |
| Aethergold Cell | 7 | `1,200,000 FE` | `6144 FE/t` | `3072 FE/t` | `1.03` | `0%/min` | `Aetherburst Cell`: high-output transfer cell with no idle loss and lower capacity than Nullite. |
| Exotic Cell | 8 | `10,000,000 FE` | `8192 FE/t` | `4096 FE/t` | `1.06` | `0%/min` | `Harmonic Cell`: pack-endgame capacity, high efficiency, no idle loss. |

## Effective Chassis Rate

Cells limit the actual input and output rate of a chassis.

Example:

```text
Steel Battery Chassis ENERGY_TRANSFER = 256 FE/t
4 inserted Iron Cells output = 4 * 64 FE/t = 256 FE/t
effective output = 256 FE/t
```

If the same chassis only has one Iron Cell:

```text
Steel Battery Chassis ENERGY_TRANSFER = 256 FE/t
1 inserted Iron Cell output = 64 FE/t
effective output = 64 FE/t
```

The chassis sets the ceiling. The cells must supply the rate.

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

Example:

```text
Copper Cell base capacity = 12,000 FE
Flat capacity prefix = +4,000 FE
Increased capacity prefix = +25%

effective capacity = (12,000 + 4,000) * 1.25 = 20,000 FE
```

Capacity prefixes affect the cell item. A battery chassis should use the resulting effective capacities when summing inserted cell storage.

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
