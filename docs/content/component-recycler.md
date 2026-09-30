# Component Recycler

Resource ids: `rngtech:crude_recycler`, `rngtech:<material>_component_recycler`

Status: Prototype

## Summary

Component Recyclers disassemble compatible RNGTech machine blocks, machine parts, modular tool heads and rods, calibrated components, and Battery Cells into recipe-defined physical returns. They do not recover rarity, rolled modifiers, or Refinement Potential.

`rngtech:crude_recycler` is the manual bootstrap variant. It has one input, three outputs, no FE path, no Gear or Refinement tab, and only runs from player crank interaction. Put the input in its screen, close the screen, then right-click a `rngtech:hand_crank` placed directly above it until the recipe-scaled manual cycle completes. Each accepted handle turn plays a grinding sound and rotates the crank; completion plays a separate sound cue.

The intended chain is:

```text
Chest -> Potential Reactor / Energy Recycler compatibility -> Component Recycler -> chest or storage
```

If a modified item skips the energy step and goes straight into a Component Recycler, the input is still accepted when a `rngtech:component_recycling` recipe exists. Any remaining RPG data is destroyed when the item is consumed, and the machine returns only physical recipe parts.

## Implemented Chassis

| Stage | Block id |
|---:|---|
| 2 manual | `rngtech:crude_recycler` |
| 1 | `rngtech:iron_component_recycler` |
| 2 | `rngtech:copper_component_recycler` |
| 3 | `rngtech:bronze_component_recycler` |
| 4 | `rngtech:steel_component_recycler` |
| 5 | `rngtech:aluminum_component_recycler` |
| 6 | `rngtech:titanium_component_recycler` |
| 7 | `rngtech:tungstensteel_component_recycler` |
| 8 | `rngtech:exotic_component_recycler` |

Powered chassis have Process, Gear, Stats, and Refinement tabs. Placed powered chassis preserve their RPG traits and can be refined through the machine Refinement tab. The Crude Recycler has only the manual Process surface and does not roll or refine RPG traits.

## Gear

| Gear slot | Required? | Runtime role |
|---|---:|---|
| Battery Cell | No | Portable FE buffer. |
| Disassembly Head | Yes | Provides base-profile `PROCESSING_LEVEL`, speed, stability, and item-stage reach. Stored Disassembly Head affixes scale the head contribution before host rolls apply. |
| Recovery Filter | No | Enables recipe outputs marked `requires_filter` and contributes its own base-profile efficiency or speed. |

Disassembly Heads are implemented from Iron through Exotic. Current Recovery Filters are shared with Potential Reactor parts and cover stages 1-4. The Crude Recycler does not use these slots; its Hand Crank is a placed block above the recycler and must be right-clicked to advance manual progress.

## Recipes and Returns

Component recycling is explicit recipe data under `rngtech:component_recycling`. The machine does not reverse-craft arbitrary crafting recipes. For source-linked machine and part targets, outputs should be a lossy subset of that target's authored recipe ingredients. If the source recipe uses an ingredient tag, the recycler recipe may name a concrete item accepted by that tag. Explicit fallback recipes without a normal source craft, such as calibrated components, broken circuits, and manual tool-head salvage, remain authored exceptions.

A successful Super Output roll adds one extra copy of every stackable return. Recipes can set `bonus_output: false`, which defaults to `true`, to always return exactly their authored outputs. Following the [reversible-conversion rule](../reference/machine-guidelines.md), the calibrated component recipes opt out because they return the calibration input. Recipes whose returns cover what the recycled item cost also opt out, because Super Output would duplicate those inputs: the metal tool rods, the Alloy Crucibles, the Wooden, Iron, and Copper Crusher Chassis, the Crude Recycler, the Flint Crush Head, and the Invar Battery Cell. The recipe loop audit in `npm run moddex:check` names any further recipe that needs to opt out.

Current machine-block coverage includes staged Crusher, Furnace, Alloy Furnace, Battery Chassis, Component Recycler, solid-fuel burner, Solar Panel, Resonance Calibrator, Compressor Tank, and later utility/generator machine stacks. Current modular tool-part coverage includes all Tool Rods and all Tool Heads from Flint through Exotic. Loose Resonance Calibrator Gear covers Resonance Coils, Control Boards, and Stabilizer Matrices; these recycle to their core ingredient plus one matching material ingot, with redstone or a Stabilization Catalyst as the Recovery Filter bump. Alloy Crucibles also recycle through explicit recipes, returning their source crucible or casing and part of their material input. Damaged `rngtech:pitted_cavitation_rotor` outputs recycle through a Stage 5 recipe into `rngtech:recycling_byproduct` only, because the pitted item no longer records the source rotor material.

With JEI installed, Component Recycler recipes are exposed with input, up to three outputs, processing ticks, FE cost, minimum processing level, Recovery Filter requirements for gated outputs, and an output-tooltip note on recipes with output bonuses off. Clicking the Process-tab progress bar opens the Component Recycler recipe category. Crude Recycler compatibility is narrower than the full recipe category: it accepts `rngtech:manual_recycler_inputs`, including explicit Stage 1-2 early-machine recipes and Stage 1 iron component recipes, ignores FE cost, ignores Recovery Filter outputs, and uses a recipe-scaled manual cycle advanced by right-clicking the Hand Crank. Simple recipes complete in at least `80` ticks; longer recipes take twice their authored processing ticks.

Current first-pass returns are intentionally punishing and source-linked:

| Target category | Typical guaranteed return | Recovery Filter bump |
|---|---|---|
| Modular tool heads | Flint or one material gear. | Scrap or one material plate. |
| Tool Rods | Sticks or one material rod. | One button or one material plate. |
| Machine chassis | One source frame or chassis ingredient plus one material or functional ingredient. | Another source recipe ingredient such as a circuit, raw calibrated-component precursor, coil, or bus bar. |
| Component Recycler chassis | One matching source machine-frame ingredient plus two casings. | The source Disassembly Head. |
| Machine parts | One source gear, coil, core, plate, casing, or functional part. | Another source recipe ingredient. |
| Calibrated components | One raw component ingredient, such as the Iron Gear from a Calibrated Kinetic Component. | None. |
| Battery Cells | Source cell core or shell ingredients plus one material ingredient. | One source material plate or stabilizing ingredient. |

Calibrated component recycling is the fallback for stability misses. If a component rolls below the stability needed by a downstream craft, it can be recycled for a deterministic raw-material return worth roughly `30-50%` of the calibration attempt. The return is never more than one raw input per component, because neither the calibration recipe nor the recycling recipe allows Super Output. Reusable calibration patterns are not part of the return because they were never consumed, and catalysts or recipe stabilizers are intentionally lost.

Machine and part stacks do not remember the stability or stage of calibrated ingredients consumed during crafting. When those crafted targets recycle a calibrated-component slot, the return is the uncalibrated precursor item for that calibration family rather than a calibrated component with invented state.

Rarity, modifier tier, modifier count, and Refinement Potential never improve Component Recycler output. Those values belong to the energy-recycling step.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert valid Component Recycler recipe inputs. |
| Bottom | Extract up to three output slots. |
| Sides | Receive FE. |

Gear slots and Refinement catalyst slots are not exposed through normal sided automation. The Crude Recycler has no FE capability; its top is occupied by the Hand Crank during normal use, so it is primarily a manual bootstrap block.

## Related Pages

- [Potential Reactor](potential-reactor.md)
- [Machine Parts](machine-parts.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
