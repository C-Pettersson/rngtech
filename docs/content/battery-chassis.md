# Battery Chassis

Status: Prototype


Gameplay resource ids:

- `rngtech:wooden_battery_chassis`
- `rngtech:iron_battery_chassis`
- `rngtech:copper_battery_chassis`
- `rngtech:gold_battery_chassis`
- `rngtech:steel_battery_chassis`
- `rngtech:sparksteel_battery_chassis`
- `rngtech:arclite_battery_chassis`
- `rngtech:nullite_battery_chassis`
- `rngtech:aethergold_battery_chassis`
- `rngtech:exotic_battery_chassis`

The first implementation uses material-specific block ids so each variant can have a normal block model, item model, recipe, and loot table.

## Summary

The battery chassis is a stationary energy storage block that holds [Battery Cells](battery-cells.md).

Inserted [Battery Cells](battery-cells.md) provide most or all stored FE and must support the actual charge or discharge rate. The chassis defines the maximum rate ceiling, charge balancing, burst release behavior, leakage damping, automation exposure, and global behavior.

## Behavior

The chassis has `1-10` material-defined battery slots, with rollable chassis prefixes able to activate up to `4` additional slots. It exposes the standard NeoForge block energy capability so adjacent machines, wires, generators, and modded automation can interact with it as an energy block.

Current runtime surface:

- Java block type: `BatteryChassisBlock`.
- Java block entity: `BatteryChassisBlockEntity`.
- Java block item: `BatteryChassisBlockItem`, backed by `MachineBlockItem` trait and refinement behavior.
- Menu and screen: `BatteryChassisMenu` and `BatteryChassisScreen`.
- Capabilities: block energy storage and block item handler for Battery Cell slots.
- Chassis blocks roll `MachineType.BATTERY_CHASSIS` traits when crafted or placed through normal block-item paths, then apply authored material base stats from `MachineBaseStatCatalog` and fixed behavior identity from `MachineImplicitCatalog`.
- Battery Chassis recipes consume the shared Machine Frame structural ladder instead of the lower-level generic casing form. Wooden through Gold chassis use `rngtech:machine_frame`, Steel uses `rngtech:reinforced_machine_frame`, Sparksteel and Arclite use `rngtech:advanced_machine_frame`, and Nullite, Aethergold, and Exotic use `rngtech:exotic_machine_frame`.
- Placed chassis blocks can be refined from their screen Refinement tab.
- Installed cells are stored as item stacks inside the block entity, so FE, rarity, Refinement Potential, and modifiers stay on the cell when it is moved or dropped.

Total storage is based on the inserted cells, then scaled by chassis capacity prefixes:

```text
usable capacity = sum(installed cell capacities) * chassis ENERGY_CAPACITY multipliers
```

The chassis does not create portable cell capacity. Capacity above a cell's own item limit is only available while that cell remains installed in the boosted chassis; removing or dropping the cell clamps it back to its intrinsic capacity.

Chassis material controls characteristics such as:

- Battery slot count and rollable additional cell slots.
- Transfer rating; actual input and output throughput are decided by installed cell rates, stored FE, and available cell space.
- Charge and discharge efficiency.
- Burst identity and stability behavior.
- Idle loss or insulation.
- Stability under high transfer or overclocked use.
- Whether cells charge and drain evenly.
- Burst release and leakage damping behavior for specific material identities.
- Redstone control and side configuration.
- Global modifier strength across inserted cells.
- The rollable Balance Mode prefix, which enables even charge and discharge on chassis that do not already have the behavior.

Removed cells keep their own stored FE, rarity, Refinement Potential, and item modifiers. They do not keep temporary effects from the chassis that held them.

Directly touching Battery Chassis blocks slowly equalize by fill ratio. A fuller chassis feeds an adjacent lower-fill chassis through the normal cell-led transfer path, but each move is capped so the pair does not overshoot and bounce energy back on the next tick.

## Gear Tab

The battery chassis screen includes a Gear tab for installed cells. Future non-cell chassis components are still planned.

The Gear tab should keep component installation separate from energy status, stats, and refinement controls. Battery cell slots should still use standard item capability rules for automation where appropriate, while non-cell chassis components can stay player-managed if they should not be exposed to hoppers or item pipes.

Placed machine Gear tabs are already prototyped for Crusher and Furnace. The solid fuel-burning generator category is currently a chassis/category target only. Current Gear-tab status is tracked in [Machine Chassis](machine-chassis.md#gear-tabs).

## Chassis Roles

More slots should not always mean a better chassis. A low-slot chassis can apply stronger global effects to each inserted cell, while a high-slot chassis is better for broad base storage.

| Role | Typical Slots | Gameplay Character |
|---|---:|---|
| Starter holder | `1` | First stationary use for a battery cell. Low transfer, simple behavior. |
| Early buffer | `2-3` | Small base buffer with readable material traits. |
| Controlled bank | `3-5` | Better redstone behavior, reserve modes, and safer transfer. |
| Burst bank | `3-6` | High output for demanding machines, with stability or efficiency tradeoffs. |
| Bulk storage | `6-10` | Large storage from many inserted cells, usually weaker per-cell global scaling. |

## Material Direction

These are design targets for material personality, not final balance values.

| Material Direction | Slots | Characteristic Focus |
|---|---:|---|
| Crude or Iron | `1-2` | Simple, reliable, low transfer. |
| Copper | `2` | Better early transfer, light loss or low stability. |
| Bronze or Gold | `3` | Conductive burst behavior, worse stability. |
| Lead or Steel | `4-5` | Shielding, low idle loss, stable discharge. |
| Redstone or Sparksteel | `4-5` | Control, reserve thresholds, better transfer. |
| Quartz or Aluminum | `5` | Efficient charging and precise balancing. |
| Arclite or Titanium | `6` | High transfer, burst output, advanced control. |
| Nullite or Tungstensteel | `7-8` | Late-game throughput and special charge-balancing behavior. |
| Aethergold | `6` | Focused no-leak burst bank with strong short-window surge identity. |
| Pack-defined Exotic, Naquadah by default | `8-10` | Endgame stability, high heat tolerance, or pack-defined behavior. |

## Base Chassis Values

These values are first-pass balance targets. Inserted cells still define base storage capacity. Chassis values define how the bank behaves, and Battery Chassis capacity prefixes can scale installed-cell capacity while the cells remain in the chassis.

`Transfer` is the chassis' material bus rating shown in status and stat readouts. Actual input and output throughput are based on the installed cells' combined charge/discharge rates, stored FE, and free capacity. `Burst` and `Duration` describe chassis surge identity for later risk tuning; current steady transfer remains cell-led. `Efficiency`, `Stability`, and `Global` use `1.0` as neutral. `Idle Loss` is passive leakage from the stored FE in inserted cells.

| Chassis | Stage | Slots | Transfer | Burst | Duration | Efficiency | Stability | Idle Loss | Global | Characteristic Implicit Modifier |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Wooden Battery Chassis | 0 | `1` | `16 FE/t` | `1.0x` | `0 ticks` | `0.80` | `0.25` | `0.10%/min` | `1.50` | `Splintered Rack`: novelty holder with awful stability. |
| Iron Battery Chassis | 1 | `1` | `128 FE/t` | `1.0x` | `0 ticks` | `1.00` | `1.00` | `0%/min` | `1.30` | `Iron Rack`: no special behavior, strong per-cell modifier focus. |
| Copper Battery Chassis | 2 | `2` | `192 FE/t` | `1.1x` | `20 ticks` | `0.96` | `0.95` | `0.02%/min` | `1.20` | `Conductive Bus`: higher transfer with light leakage and Burst Release. |
| Gold Battery Chassis | 3 | `3` | `384 FE/t` | `2.5x` | `40 ticks` | `0.92` | `0.80` | `0.05%/min` | `1.10` | `Flash Bus`: large Burst Release output with overload risk. |
| Steel Battery Chassis | 4 | `4` | `256 FE/t` | `1.0x` | `0 ticks` | `1.00` | `1.15` | `0%/min` | `1.00` | `Reinforced Rack`: stable discharge, overload resistance, and Cell Leakage Damping. |
| Sparksteel Battery Chassis | 5 | `5` | `512 FE/t` | `1.4x` | `60 ticks` | `1.04` | `1.00` | `0.01%/min` | `0.95` | `Balanced Bus`: Charge Balancer plus Burst Release. |
| Arclite Battery Chassis | 6 | `6` | `1024 FE/t` | `2.0x` | `60 ticks` | `0.98` | `0.90` | `0.03%/min` | `0.90` | `Signal Bus`: high transfer with Charge Balancer and Burst Release. |
| Nullite Battery Chassis | 7 | `8` | `2048 FE/t` | `2.0x` | `80 ticks` | `1.02` | `0.95` | `0.02%/min` | `0.80` | `Phase Bus`: late charge-balancing behavior with Charge Balancer, Burst Release, and mild instability. |
| Aethergold Battery Chassis | 7 | `6` | `3072 FE/t` | `4.0x` | `100 ticks` | `1.03` | `1.05` | `0%/min` | `0.90` | `Aetherburst Bus`: strongest current Burst Release identity with Charge Balancer and sealed installed-cell leakage. |
| Exotic Battery Chassis | 8 | `10` | `4096 FE/t` | `1.5x` | `80 ticks` | `1.05` | `1.20` | `0%/min` | `0.70` | `Harmonic Bank`: broad storage bank with Charge Balancer, Burst Release, and weaker per-cell global scaling. |

Low-slot chassis intentionally have higher `GLOBAL_MODIFIER_STRENGTH`. This lets a focused chassis remain useful even after larger chassis become available.

Steel applies a flat `0.02` Cell Leakage Damping reduction to installed cell idle loss before chassis idle loss is added. Aethergold seals installed-cell idle loss entirely while also carrying no chassis idle loss. Sparksteel, Arclite, Nullite, Aethergold, and Exotic have fixed Charge Balancer identity. The rollable Balance Mode prefix enables the same charge-balancing behavior on any Battery Chassis: charge, discharge, and idle-loss draws are spread across eligible installed cells while respecting each cell's input, output, stored FE, and remaining capacity. Copper, Gold, Sparksteel, Arclite, Nullite, Aethergold, and Exotic keep Burst Release identity for surge tuning, but current steady transfer remains cell-led.

## Effective Transfer

Chassis `ENERGY_TRANSFER` is a material bus rating, not a hard intake or output cap. Input and output are intentionally cell-led: installed Battery Cells decide how much FE the bank can accept or deliver, bounded by remaining space, stored FE, and any upstream connector, downstream connector, source, or sink limit. The summed cell input and output rates are per-tick budgets shared by every side and connector, so adding more connectors or sides does not raise the bank's throughput.

The Status tab exposes the pieces separately: chassis transfer rating, summed cell input rate, summed cell output rate, effective input/output, attached connector cap, and last tick transfer. This is intentionally different from generators and processing machines: Battery Chassis is storage gameplay, so cell rate limits remain visible and meaningful.

Suggested steady output calculation:

```text
cell output ceiling = sum(inserted cell output rates)
effective output = min(cell output ceiling, machine demand, available stored FE)
```

Suggested steady input calculation:

```text
cell input ceiling = sum(inserted cell input rates capped by free cell space)
effective input = min(cell input ceiling, offered FE, available cell space)
```

This keeps high-stage chassis from making weak cells behave like high-output cells while allowing a broad bank of strong cells to charge and discharge at their combined rate.

When an adjacent Universal Connector is present, the connector tier is the wiring cap. A low-tier connector can limit transfer below the cell sums; the UI and Jade readout report this as `Attached connector limits input energy` or `Attached connector limits output energy`. Two banks on one network with default `Both` connectors do not charge each other; see the [storage loop guard](basic-wire.md#energy-transfer-limits) for how to move FE between banks on purpose.

## Burst Transfer

`BURST_TRANSFER` is retained as chassis surge identity for later risk and overload tuning. Current Battery Chassis transfer is cell-led, so burst values do not raise actual throughput above the installed cells' combined output rate.

Example:

```text
Gold Battery Chassis
ENERGY_TRANSFER = 384 FE/t
BURST_TRANSFER = 2.5x

burst rating = 384 * 2.5 = 960 FE/t
```

The gold chassis has a `384 FE/t` transfer rating and a `960 FE/t` burst rating. Actual current output still comes from the installed cells' combined output rate and stored FE.

Core burst rules:

- Burst does not create energy.
- Burst does not increase storage capacity.
- Burst is output-only by default.
- Burst does not override installed-cell output rates.
- Burst should later feed duration, cooldown, budget, stability risk, or some combination of those rules.
- Low-stability chassis are intended to overload, waste FE, or temporarily lose burst output in a later risk pass.
- High-stability chassis are intended to burst more safely, even if their burst multiplier is lower.

Suggested calculation:

```text
cell output ceiling = sum(inserted cell output rates)
actual output = min(machine demand, cell output ceiling, available stored FE)
```

A chassis can also define a burst budget:

```text
burst budget = ENERGY_TRANSFER * BURST_DURATION
```

For example, a gold chassis carries a `40`-tick burst window for later startup-spike or overclock risk rules.

Burst should reinforce chassis personality:

| Chassis Direction | Burst Meaning |
|---|---|
| Iron | No burst. Predictable steady output. |
| Copper | Tiny burst. Mostly early transfer help. |
| Gold | Huge burst, poor stability, and energy waste risk. |
| Steel | Little or no burst, but very safe sustained output. |
| Sparksteel | Moderate controlled burst with smarter balancing. |
| Arclite | Strong burst for advanced machine demand spikes. |
| Nullite | Strong burst plus unusual charge-balancing behavior. |
| Aethergold | Highest current burst multiplier and longest burst window, with no leakage. |
| Exotic | Lower burst multiplier than Arclite, but very stable. |

## Global Modifier Surface

Chassis modifiers apply to the chassis behavior and can affect every inserted cell while installed.

Global effects usually focus on behavior, with capacity handled by the dedicated Battery Chassis capacity prefixes:

| Effect Area | Examples |
|---|---|
| Transfer shape | Higher steady transfer, burst transfer, throttled output, charge-first behavior. |
| Efficiency | Less energy lost while charging or discharging. |
| Stability | Lower overload risk, safer burst behavior, less strain on inserted cells. |
| Balancing | Evenly charge or drain all cells instead of filling or emptying one at a time. |
| Reservation | Keep one slot or a percentage of stored FE unavailable except under configured conditions. |
| Insulation | Reduce idle loss or environmental penalties. |
| Matching bonuses | Reward inserted cells that share material, rarity, or modifier themes. |
| Storage | Increased installed-cell capacity, charged storage after uptime, or additional active cell slots. |

Global modifier power should have a budget. A chassis with many slots can spread an effect across more cells, but the per-cell impact may be weaker than on a focused low-slot chassis.

## Automation

The chassis should use standard capabilities:

- Energy capability for charging and discharging the bank.
- Item capability for inserting and extracting compatible battery cells.

Hoppers and modded automation should be able to interact with the battery slots through normal item rules. Battery banks use these capability handlers, including when connected through RNGTech Item Connectors. They do not provide a shared storage inventory.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Implicit | Chassis material identity defines baseline stats through `MachineBaseStatCatalog` and fixed behavior flags through `MachineImplicitCatalog`. |
| Prefix | `ENERGY_CAPACITY`, Charged Storage, Balance Mode, Additional Battery Slots, and `EFFICIENCY`. |
| Suffix | `ENERGY_TRANSFER`, `BURST_TRANSFER`, `BURST_DURATION`, `STABILITY`, `IDLE_LOSS`, `GLOBAL_MODIFIER_STRENGTH`. |
| Enchant | See [Modifier Eligibility](../reference/modifier-eligibility.md). |

Battery Chassis blocks use the `BATTERY_CHASSIS` modifier eligibility profile. Inserted cells keep their own `BATTERY_CELL` profile modifiers. See [Modifier Eligibility](../reference/modifier-eligibility.md#current-code-profiles).

Charged Storage is an untiered Battery Chassis prefix. It grants `100% more ENERGY_CAPACITY` only after the placed chassis has held energy continuously for `10` minutes. If the bank empties, the uptime counter resets.

Balance Mode is a single-tier Battery Chassis prefix. It enables even fill and unload behavior for installed cells, without changing capacity or transfer stats.

Additional Battery Slots is a Battery Chassis prefix that adds active cell slots:

| Tier | Added slots |
|---:|---:|
| 1 | `+1` |
| 2 | `+1` |
| 3 | `+2` |
| 4 | `+2` |
| 5 | `+2` |
| 6 | `+3` |
| 7 | `+4` |

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Battery Cells](battery-cells.md)
- [Machine Chassis](machine-chassis.md)
- [Component Stages](../reference/component-stages.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
