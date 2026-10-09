# Solid Fuel Burning

Status: Prototype

Player guide: [Solid Fuel Burner](https://c-pettersson.github.io/rngtech/solid-fuel-burner/)

Solid fuel burning is a machine category with concrete Stage 1-4 generator chassis. The category itself is still not registered as `rngtech:solid_fuel_burner`.

## Current Runtime Surface

Registered placed generators:

| Chassis | Resource id | Stage | Max part stage | Component slots |
|---|---|---:|---:|---|
| Crude Solid Fuel Burner | `rngtech:crude_solid_fuel_burner` | 1 | 1 | Heat Core, Battery Cell, Fuel Box |
| Copper Solid Fuel Burner | `rngtech:copper_solid_fuel_burner` | 2 | 2 | Heat Core, Battery Cell, Fuel Box |
| Alloy Solid Fuel Burner | `rngtech:alloy_solid_fuel_burner` | 3 | 3 | Heat Core, Battery Cell, Fuel Box |
| Steel Solid Fuel Burner | `rngtech:steel_solid_fuel_burner` | 4 | 4 | Heat Core, Battery Cell, Fuel Box |

The shared runtime uses `SolidFuelBurnerBlockEntity`, `SolidFuelBurnerMenu`, and `SolidFuelBurnerScreen` under the `solid_fuel_burner_chassis` registry ids. The public placed blocks use only the four concrete chassis ids above.

There is still no `rngtech:solid_fuel_burner` block, item, or block entity type.

## Components

Burners require all three Gear-tab components before they burn fuel:

| Slot | Accepted items | Runtime role |
|---|---|---|
| Heat Core | `rngtech:iron_heat_core`, `rngtech:copper_heat_core`, `rngtech:bronze_heat_core`, `rngtech:steel_heat_core` | Sets base FE/t, fuel efficiency, heat isolation, and max accepted fuel tier. |
| Battery Cell | Existing gameplay Battery Cells from Stage 0-4 | Stores generated FE on the item stack. In generators, the cell is capacity-only and does not cap generated FE/t, export FE/t, or apply cell efficiency loss. |
| Fuel Box | `rngtech:iron_fuel_box`, `rngtech:copper_fuel_box`, `rngtech:bronze_fuel_box`, `rngtech:steel_fuel_box` | Sets fuel slot count, accepted fuel forms, fuel efficiency, stability, and behavior such as Quick Feed, Fuel Reserve, Fuel Governor, and compact block fuel support. |

Chassis reject components above their max part stage. They accept lower-stage components so mixed setups can trade speed, efficiency, fuel queue size, and output behavior.

The generic material-form `rngtech:battery_cell` is not a valid burner buffer. Burners use material-specific `BatteryCellItem` stacks such as `rngtech:potato_battery_cell`, `rngtech:unique_potato_battery_cell`, `rngtech:iron_battery_cell`, `rngtech:copper_battery_cell`, `rngtech:lead_battery_cell`, and `rngtech:invar_battery_cell`.

## Fuel Rules

Fuel insertion must pass two tag-backed gates:

- Fuel form: `rngtech:solid_fuel/item_fuels` or `rngtech:solid_fuel/block_fuels`.
- Fuel tier: `rngtech:solid_fuel/tier_1` through `rngtech:solid_fuel/tier_4`.

Tagged fuels are the authored balance surface. In addition, ordinary vanilla burnable items that are not compact block fuels and do not leave a crafting remainder are accepted as Tier 1 item fuels, so fallback fuels such as extra wooden burnables work without listing every item. Logs are accepted but deliberately inefficient: burners use one quarter of their vanilla furnace burn time.

Heat Cores gate fuel tier and Fuel Boxes gate fuel form; see the part tables under [Base Stats](#base-stats). Compact block fuels include items tagged `c:storage_blocks/charcoal` when a pack provides charcoal blocks.

Burners use the furnace fuel burn-time source with burner-specific adjustments such as the log penalty. Each fuel item holds `10 FE` per effective burn tick: `FUEL_DURATION`, `FUEL_EFFICIENCY`, and `EFFICIENCY` increase the effective burn ticks and so the FE in each item. The Heat Core and `ENERGY_GENERATION` set FE/t, so a stronger burner spends the same fuel faster rather than getting more FE from it. One Coal (`1,600` burn ticks) holds `16,000 FE` before fuel stats; a Steel Heat Core in a Steel Fuel Box spends about `20,000 FE` from it in roughly 10 seconds. Higher FE/t comes from Syngas at Stage 5, not from better burners. Generated FE is written directly into the installed Battery Cell stack, then exported to adjacent energy receivers through the block energy capability. The generator has no RNGTech machine-side export cap; adjacent receivers, Battery Chassis input limits, and Universal Connector tier caps decide how much FE actually moves each tick.

JEI shows a Solid Fuel Burning recipe-like category grouped by fuel tier and form.

## Base Stats

The chassis supplies stability and identity behavior. The Heat Core and Fuel Box provide most generator behavior through installed component modifiers. Universal Connector tier is the intended player-facing wiring limit.

| Chassis | Resource id | Stability | Notes |
|---|---|---:|---|
| Crude Solid Fuel Burner | `rngtech:crude_solid_fuel_burner` | `0.75` | Stage 1 chassis; accepts Stage 1 parts. |
| Copper Solid Fuel Burner | `rngtech:copper_solid_fuel_burner` | `0.90` | Stage 2 chassis; accepts parts up to Stage 2 and carries Quick Feed. |
| Alloy Solid Fuel Burner | `rngtech:alloy_solid_fuel_burner` | `1.00` | Stage 3 chassis; accepts parts up to Stage 3 and carries Fuel Reserve. |
| Steel Solid Fuel Burner | `rngtech:steel_solid_fuel_burner` | `1.15` | Stage 4 chassis; accepts parts up to Stage 4. |

Heat Core base stat contributors:

| Part | Stage | Energy generation | Fuel efficiency | Heat isolation | Max fuel tier | Behavior |
|---|---:|---:|---:|---:|---:|---|
| Iron Heat Core | 1 | `24 FE/t` | `0.85x` | `0.85x` | 1 | None |
| Copper Heat Core | 2 | `40 FE/t` | `0.80x` | `0.75x` | 2 | Quick Feed |
| Bronze Heat Core | 3 | `64 FE/t` | `1.00x` | `1.00x` | 3 | None |
| Steel Heat Core | 4 | `96 FE/t` | `1.15x` | `1.20x` | 4 | Thermal Buffer |

Fuel Box base stat contributors:

| Part | Stage | Fuel slots | Fuel efficiency | Stability | Behavior |
|---|---:|---:|---:|---:|---|
| Iron Fuel Box | 1 | `1` | `0.95x` | `0.85x` | None |
| Copper Fuel Box | 2 | `1` | `0.90x` | `0.90x` | Fuel Governor and Quick Feed |
| Bronze Fuel Box | 3 | `2` | `1.00x` | `1.00x` | Fuel Governor and Fuel Reserve |
| Steel Fuel Box | 4 | `2` | `1.10x` | `1.15x` | Fuel Governor and Block Feed |

## Fuel Behaviors

- Fuel Governor: while the installed Battery Cell is full, the burner does not start a new fuel item and does not decrement active burn time.
- Quick Feed: identity flag only; Fuel Governor still controls full-buffer pausing.
- Fuel Reserve: keeps the final accepted fuel item untouched so automation loops do not drain dry.
- Block Feed: enables compact block fuels when the fuel tier tags allow them.

## Automation

- The item capability exposes only the Fuel Box fuel slots; Gear components are not exposed.
- The block energy capability is extract-only and has no internal output-rate cap.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Chassis implicit | Chassis base stats are resolved from `MachineBaseStatCatalog`, and fixed behavior flags are resolved from `MachineImplicitCatalog`; no random implicit modifiers currently roll. |
| Chassis prefix | `EFFICIENCY`, `FUEL_EFFICIENCY`. |
| Chassis affixes | Prefix flat `ENERGY_GENERATION`; suffix `INPUT_SLOTS`, percent `ENERGY_GENERATION`, `FUEL_DURATION`, and `STABILITY`. Existing old stacks with `ENERGY_TRANSFER` are harmless legacy rolls because burner export is no longer machine-transfer capped. |
| Heat Core base profile | Authored component base stats provide `ENERGY_GENERATION`, `FUEL_EFFICIENCY`, `HEAT_ISOLATION`, heat-machine stats, and hard fuel-tier identity. |
| Heat Core rolled modifiers | Prefix flat `ENERGY_GENERATION`, `FUEL_EFFICIENCY`, `HEAT_ISOLATION`, `HEAT_TRANSFER`, and `MAX_TEMPERATURE`; suffix percent `ENERGY_GENERATION` and `TEMPERATURE_STABILITY`. |
| Fuel Box base profile | Authored component base stats provide fuel slot count, `FUEL_EFFICIENCY`, and `STABILITY`; fuel forms are fixed identity. |
| Fuel Box rolled modifiers | Prefix `FUEL_EFFICIENCY`; suffix `STABILITY`. Normal Fuel Box modifiers do not raise fuel slot count or fuel-tier support. |
| Fuel behavior | Quick Feed, Fuel Reserve, Fuel Governor, and Block Feed are fixed identity behaviors. They are not rolled named modifiers yet. |

Solid Fuel Burner chassis use the `SOLID_FUEL_BURNER` modifier eligibility profile. Heat Cores and Fuel Boxes use their own part profiles. See [Modifier Eligibility](../reference/modifier-eligibility.md#current-code-profiles).

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Fuel Governor](../modifiers/fuel-governor.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Parts](machine-parts.md)
- [Battery Cells](battery-cells.md)
- [Machine Stats](../reference/machine-stats.md)
