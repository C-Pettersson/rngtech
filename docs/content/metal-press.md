# Metal Press

Status: Prototype


Resource ids: `rngtech:crude_metal_press`, `rngtech:metal_press`

## Summary

The press line is the powered forming path for material plates, material gears, selected material casings, and Electric Circuit tiers. Both placed blocks use the `rngtech:metal_press` recipe type, the same Process/Gear/Stats/Refinement/Mastery screen, and the same top input, bottom output, and side FE capability layout.

The Crude Metal Press is the pre-Steel bootstrap press. It can make early circuits and material plates when its Heat Core, Mold, and optional control parts satisfy the recipe gates. The Steel Metal Press is the standalone Stage 4 press with mandatory Servo control and failure-output behavior; its body recipe uses the regular Machine Frame rather than consuming the Crude Metal Press.

## Gear Requirements

| Gear Slot | Crude Press | Steel Press | Runtime role |
|---|---|---|---|
| Heat Core | Required | Required | Provides effective forming temperature and heat stability. |
| Servo | Optional | Required | Adds speed, stability, temperature stability, overheat tolerance, and power-drop grace behavior. |
| Mold storage | Required | Required | Stores up to five molds internally. The Gear tab selector chooses the active mold used by recipes. Current molds are `rngtech:plate_mold`, `rngtech:casing_mold`, `rngtech:gear_mold`, `rngtech:circuit_mold`, and `rngtech:connector_mold`. |
| Battery Cell | Optional | Optional | Adds portable FE storage. Without a cell, the press keeps only its small working buffer and has lower speed and stability. |

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

1. Craft `rngtech:insulator` from mineral inputs: glass, quartz, clay, and smooth stone.
2. Craft `rngtech:circuit_mold`; it is tagged as `rngtech:metal_press_molds`.
3. Craft `rngtech:basic_circuit_blank` from Bronze/Copper conductive parts, mineral insulation, redstone, and quartz.
4. Press Basic blanks with the Circuit Mold. A Bronze Heat Core is enough for the Basic heat and stability gates.
5. Use Basic circuits to build Resonance Calibrator progression and produce `rngtech:calibrated_logic_component`.
6. Craft higher blanks with alloy plates/coils, redstone logic inputs, mineral insulation, and calibrated logic gates: `55+` stability for Advanced, `80+` for Elite, and a Stage 6 calibrated diamond crystal at `85+` stability for Ultimate.
7. Press each blank tier with the Circuit Mold. Advanced can be reached before the Steel Metal Press if the Crude Press is fitted with enough control. Elite and Ultimate expect the Steel Press or a strongly upgraded Crude Press.

## Failure Behavior

Metal Press recipes support `target_temperature` and `power_sensitive` in addition to the existing `minimum_temperature`, `safe_maximum_temperature`, `required_temperature_stability`, and failure-output fields. `target_temperature` defaults to `minimum_temperature`; `power_sensitive` defaults to true for existing press recipes. `bonus_output` defaults to `true`; set it to `false` to turn off Super Output for a recipe that could otherwise form a loop. Pressing starts only after live stored heat reaches the target temperature. Warmup consumes the recipe's normal FE/t, while no FE during warmup pauses the machine and lets it cool.

The Crude Metal Press hard-gates unsafe recipes. If its effective heat capacity is below the target, if live stored heat is above the safe maximum after overheat tolerance, or if temperature stability is below the recipe requirement, the machine pauses and leaves inputs untouched. A press that is too hot for the current recipe cools down before continuing. Power shortfalls also pause without converting inputs into failure outputs.

The Steel Metal Press uses the failure-strain model. Strain can come from live heat below the recipe minimum, heat above the safe maximum after overheat tolerance, temperature stability below the recipe requirement, low effective Servo stability, or FE shortfalls after progress has already started on power-sensitive recipes. `POWER_GRACE` halves press power-drop strain.

When a Steel Press cycle fails, the input is consumed and the output slot receives the recipe failure output. Plate and casing failures produce `rngtech:malformed_ingot`; Furnace malformed-ingot recovery reads the stored material component and returns two matching nuggets through slow low-FE recipes. Circuit failures produce `rngtech:broken_circuit`. The Potential Reactor and Component Recycler can recover generic scrap from broken circuits.

## Mastery

Both presses enter the [shared Machine Mastery tree](../systems/machine-mastery.md) at the Control start, which they share with the Resonance Calibrator. Each placed press keeps its own `rngtech:machine_progression`; drops and pick-block preserve it. A press starts with 20 Control and no Drive or Reserve. In addition to the shared attribute conversions, each Control point grants 0.05% increased Temperature Stability.

Heat, processing speed, Energy Usage, Energy Capacity, Stability, Instant Process, and Super Output effects apply. Output-amount, parallel-job, energy-transfer, fuel, and fluid effects are marked inactive. As a heated machine, the press gains the [tagged](../systems/machine-mastery.md#tagged-payoffs) speed bonuses of Low Heat Specialist and Flash Annealing along with their temperature caps. Single Pass removes its Super Output chance in exchange for 30% more Processing Speed. Stability effects are marked inactive on the Crude Metal Press because it has no failure-strain model. Allocation changes reset the active cycle and failure strain but keep inventory, the selected mold, and current heat.

Press recipes accept optional `machine_xp` and `machine_xp_band` fields. A press grants `machine_xp` once per successful normal output, including Instant Process completions. Failure outputs, Crude Press safety pauses, missing Gear, output-blocked waits, and FE shortfalls grant none. An omitted `machine_xp_band` derives from `target_temperature` with the Furnace heat ladder described in [Furnace Recipe Use](furnace.md#recipe-use).

Shipped plate, gear, casing, and Electric Circuit recipes grant XP; connector recipes grant none. Plates grant `1` for Iron and Copper, then `2`, `3`, `4`, `5`, `6`, and `8` for Stage 3 through Stage 8 materials. Gears grant 1.5 times the plate value, rounded, and casings grant twice the plate value. Basic, Advanced, Elite, and Ultimate Electric Circuits grant `3`, `5`, `10`, and `27`.

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
