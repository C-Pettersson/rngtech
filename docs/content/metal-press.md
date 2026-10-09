# Metal Press

Status: Prototype

Player guide: [Metal Press](https://c-pettersson.github.io/rngtech/metal-press/)

Resource ids: `rngtech:crude_metal_press`, `rngtech:metal_press`

## Summary

The press line is the powered forming path for material plates, gears, selected casings, Electric Circuits, and Energy Connectors. The Crude Metal Press is the pre-Steel bootstrap press that pauses instead of failing; the Steel Metal Press is the Stage 4 press with mandatory Servo control and failure outputs.

## Gear Requirements

Both presses share one menu and slot layout. Heat Core and an active mold from the five-slot internal mold storage are required on both; the Servo is required on the Steel press and optional on the Crude press; the Battery Cell is optional, and a press without one keeps only its small working buffer at lower speed and stability.

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Crude Metal Press block | `rngtech:crude_metal_press` |
| Metal Press block | `rngtech:metal_press` |
| Recipe type | `rngtech:metal_press` |
| Steel Servo | `rngtech:steel_servo` |
| Plate Mold | `rngtech:plate_mold` |
| Casing Mold | `rngtech:casing_mold` |
| Gear Mold | `rngtech:gear_mold` |
| Circuit Mold | `rngtech:circuit_mold` |
| Connector Mold | `rngtech:connector_mold` |
| Malformed Ingot | `rngtech:malformed_ingot` |
| Broken Circuit | `rngtech:broken_circuit` |

Plate recipes consume one matching `c:ingots/<material>` input, require the Plate Mold to be installed, and output one registered material plate. They cover Iron, Copper, Bronze, Tin, Zinc, Gold, Steel, Invar, Nickel, Lead, Silver, Aluminum, Sparksteel, Osmium, Titanium, Arclite, Tungsten, Tungstensteel, Nullite, Aethergold, Platinum, Netherite, and Naquadah. Each plate recipe takes `200` ticks and `2400 FE` before machine and component stats adjust timing and cost. Plate heat gates follow the material stage bands already used by press forming recipes: `800-1100` for Iron, `900-1200` for Copper, `1000-1300` for Stage 3 materials, `1100-1400` for Stage 4 materials, `1300-1600` for Stage 5 materials, `1450-1750` for Stage 6 materials, `1750-2050` for Stage 7 materials, and `2000-2300` for Stage 8 materials.

Casing recipes consume four matching ingots, require the Casing Mold to be installed, and currently exist only for downstream-used casing forms: Iron, Copper, Bronze, Gold, Steel, Aluminum, Titanium, Tungstensteel, Sparksteel, Arclite, and Naquadah. Gear recipes consume three matching ingots, require the Gear Mold to be installed, and cover every registered material gear form. Electric Circuit recipes consume one matching circuit blank, require the Circuit Mold, and output one finished circuit per press cycle. Basic through Arclite Energy Connector recipes consume one staged coil, while Exotic consumes eight Nullite Coils; all require the Connector Mold and output fixed-stat Energy Connector endpoints for the [Universal Cable](basic-wire.md). The Crude Energy Connector is the direct-crafted early exception. Circuit failures on the Steel Metal Press output `rngtech:broken_circuit`.

## Circuit Flow

`rngtech:circuit_mold` is tagged `rngtech:metal_press_molds`. The player guide walks the blank chain and its calibration gates. Balance intent: a Bronze Heat Core meets the Basic heat and stability gates, Advanced is reachable on a well-controlled Crude Press, and Elite and Ultimate are balanced for the Steel Press or a strongly upgraded Crude Press. Higher blanks are `rngtech:calibrated_shaped` recipes whose calibration gates (`logic` family, stage, and `min_stability`) live in the recipe JSON.

## Failure Behavior

Metal Press recipes support `target_temperature` and `power_sensitive` in addition to the existing `minimum_temperature`, `safe_maximum_temperature`, `required_temperature_stability`, and failure-output fields. `target_temperature` defaults to `minimum_temperature`; `power_sensitive` defaults to true for existing press recipes. `bonus_output` defaults to `true`; set it to `false` to turn off Super Output for a recipe that could otherwise form a loop. Pressing starts only after live stored heat reaches the target temperature. Warmup consumes the recipe's normal FE/t, while no FE during warmup pauses the machine and lets it cool.

The Crude Metal Press hard-gates unsafe recipes: insufficient heat capacity, stored heat above the safe maximum after overheat tolerance, low temperature stability, and FE shortfalls all pause the machine with inputs untouched.

The Steel Metal Press uses the failure-strain model. Strain sources are heat outside the minimum/safe-maximum window (after overheat tolerance), temperature stability below the recipe requirement, low effective Servo stability, and FE shortfalls after progress starts on power-sensitive recipes. `POWER_GRACE` halves press power-drop strain. A failed cycle consumes the input and outputs the recipe's failure output: material-marked `rngtech:malformed_ingot` for plates and casings, read by Furnace malformed-ingot recovery, and `rngtech:broken_circuit` for circuits.

## Mastery

Both presses enter the [shared Machine Mastery tree](../systems/machine-mastery.md) at the Control start, which they share with the Resonance Calibrator. Each placed press keeps its own `rngtech:machine_progression`; drops and pick-block preserve it. A press starts with 20 Control and no Drive or Reserve. In addition to the shared attribute conversions, each Control point grants 0.05% increased Temperature Stability.

Heat, processing speed, Energy Usage, Energy Capacity, Stability, Instant Process, and Super Output effects apply. Output-amount, energy-transfer, fuel, and fluid effects are marked inactive. [Batch Size](../reference/machine-stats.md) presses several plate, gear, or casing input sets per cycle; circuits never batch. As a heated machine, the press gains the [tagged](../systems/machine-mastery.md#tagged-payoffs) speed bonuses of Low Heat Specialist and Flash Annealing along with their temperature caps. Single Pass removes its Super Output chance in exchange for 30% more Processing Speed. Stability effects are marked inactive on the Crude Metal Press because it has no failure-strain model. Allocation changes reset the active cycle and failure strain but keep inventory, the selected mold, and current heat.

Press recipes accept optional `machine_xp` and `machine_xp_band` fields. A press grants `machine_xp` once per successful normal output, including Instant Process completions. Failure outputs, Crude Press safety pauses, missing Gear, output-blocked waits, and FE shortfalls grant none. An omitted `machine_xp_band` derives from `target_temperature` with the Furnace heat ladder described in [Furnace Recipe Use](furnace.md#recipe-use).

Shipped plate, gear, casing, and Electric Circuit recipes grant XP; connector recipes grant none. Plates grant `1` for Iron and Copper, then `2`, `3`, `4`, `5`, `6`, and `8` for Stage 3 through Stage 8 materials. Gears grant 1.5 times the plate value, rounded, and casings grant twice the plate value. Basic, Advanced, Elite, and Ultimate Electric Circuits grant `3`, `5`, `10`, and `27`.

## Ascendancies

Status: Prototype

Metal Press machines choose between Die Keeper and Drop Forge when they use their first Ascendancy Seal. [Machine Mastery](../systems/machine-mastery.md#ascendancies) defines Seals, points, refunds, and switching, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines the new stats. The tables below are generated from the ascendancy catalog.

- Recipes are classed by their mold tag: `rngtech:plate_molds`, `rngtech:gear_molds`, `rngtech:casing_molds`, and `rngtech:circuit_molds`.
- Die Keeper’s Mold Rack switches to the first installed mold that fits the input and pauses for the Mold Swap Time. Quick Change keeps heat during the swap.
- Drop Forge’s Drop Hammer, Anvil Mass, and Forge Line raise Batch Size. A batch presses that many plate, gear, or casing input sets at that many times the FE, takes Batch Overhead longer per extra set, and rolls Super Output for each job. Circuits never batch; Forge Line disables them on its press. Heat Window moves both edges of each recipe’s safe window toward its target.
- Split Failure fails one job’s input and keeps the rest for the next cycle, because a failure output and pressed outputs cannot share the output slot.

<!-- ascendancy-trees:start -->

### Die Keeper

| Node | Type | After | Effect |
|---|---|---|---|
| **Mold Rack** | Root | — | +40 ticks Mold Swap Time. Switches to the stored mold that fits the input after the Mold Swap Time. |
| Swap Drill | Small | Mold Rack | −5 ticks Mold Swap Time. |
| **Quick Change** | Notable | Swap Drill | −25 ticks Mold Swap Time. The press keeps its heat while it swaps molds. |
| Rack Drill | Small | Quick Change | −5 ticks Mold Swap Time. |
| **Production Die** | Deep notable | Rack Drill | Gears and casings press 40% faster; circuits 25% slower. |
| Steady Bed | Small | Mold Rack | 8% increased Temperature Stability. |
| **Tolerance Map** | Notable | Steady Bed | 40% less heat-related failure strain. |
| True Bed | Small | Tolerance Map | 8% increased Temperature Stability. |
| **Master Die** | Deep notable | True Bed | 20% less Processing Speed. No failures while heat stays in the window and power holds. |
| Fine Servos | Small | Mold Rack | 8% increased Stability. |
| **Circuit Discipline** | Notable | Fine Servos | Circuit recipes gain 20% increased Stability and use 15% less FE. |
| Heat Margin | Small | Mold Rack | 10% increased Overheat Tolerance. |
| **Servo Sync** | Notable | Heat Margin | Power-drop grace is doubled, halving power-drop strain again. |

### Drop Forge

| Node | Type | After | Effect |
|---|---|---|---|
| **Drop Hammer** | Root | — | +4 Batch Size; −25% Heat Window. Plate, gear, and casing recipes batch; circuits never do. |
| Heavy Blows | Small | Drop Hammer | 8% increased Processing Speed. |
| **Anvil Mass** | Notable | Heavy Blows | +2 Batch Size. |
| Fast Line | Small | Anvil Mass | 8% increased Processing Speed. |
| **Forge Line** | Deep notable | Fast Line | +4 Batch Size. Circuit recipes are disabled. |
| Wide Bed | Small | Drop Hammer | +5% Heat Window. |
| **Split Failure** | Notable | Wide Bed | A failed batch ruins one job’s input instead of all of them; the rest waits for the next cycle. |
| Broad Bed | Small | Split Failure | +5% Heat Window. |
| **Batch Ledger** | Deep notable | Broad Bed | +5% Ledger Rate. |
| Lean Strikes | Small | Drop Hammer | 5% reduced Energy Use. |
| **Shock Absorbers** | Notable | Lean Strikes | Power-drop grace is doubled while batching. |
| Hot Platen | Small | Drop Hammer | 8% increased Heat Transfer. |
| **Hot Stamping** | Notable | Hot Platen | Plates press 20% faster above their target temperature. |

<!-- ascendancy-trees:end -->

## Automation

| Side | Behavior |
|---|---|
| Top | Insert recipe input. |
| Bottom | Extract output. |
| Sides | Receive FE through the block energy capability. |

Gear slots and the Refinement catalyst slot are not exposed through sided item automation. FE received through the block energy capability fills the internal buffer first, then charges an installed Battery Cell when possible. Processing draws spend internal buffer FE first and then draw any remaining tick cost from the installed cell.

## Modifier Eligibility

Crude and Steel Metal Press machine stacks and placed machines use the `METAL_PRESS` modifier eligibility profile. The profile can roll FE storage/input, processing, heat, warmup, cooling, power-grace, and stability modifiers that match the shared press implementation. Although the press bodies remain Stage 3 and Stage 4 progression content, they use Stage 8 modifier roll weighting and Refinement Potential ranges because Metal Press bodies are not part of a staged chassis upgrade ladder that preserves investment.

The Steel Servo is a `SERVO` machine part for `MachineType.METAL_PRESS`. It has authored base stats for press speed, stability, temperature stability, and overheat tolerance, and it can roll processing, energy usage, heat-control, stability, temperature stability, overheat tolerance, and `POWER_GRACE` modifiers.

## Related Pages

- [Machine Parts](machine-parts.md)
- [Universal Cable](basic-wire.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
