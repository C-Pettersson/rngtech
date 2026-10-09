# Component Recycler

Status: Prototype

Player guide: [Component Recycler](https://c-pettersson.github.io/rngtech/component-recycler/)

Resource ids: `rngtech:crude_recycler`, `rngtech:hand_crank`, `rngtech:<material>_component_recycler`

## Summary

Component Recyclers disassemble compatible RNGTech machine blocks, machine parts, modular tool heads and rods, calibrated components, and Battery Cells into recipe-defined physical returns. They never recover rarity, rolled modifiers, or Refinement Potential.

The intended chain is:

```text
Chest -> Potential Reactor / Energy Recycler compatibility -> Component Recycler -> chest or storage
```

A modified item that skips the energy step is still accepted when a `rngtech:component_recycling` recipe exists; its remaining RPG data is destroyed on consumption.

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

Placed powered chassis preserve their RPG traits and support placed-machine refinement. The Crude Recycler has one input, three outputs, no FE path, no Gear or Refinement tab, does not roll or refine RPG traits, and advances only through right-clicks on a `rngtech:hand_crank` placed directly above it.

## Gear

Powered chassis require a Disassembly Head (Iron through Exotic), which provides base-profile `PROCESSING_LEVEL`, speed, stability, and item-stage reach; stored Disassembly Head affixes scale the head contribution before host rolls apply. The optional Recovery Filter enables recipe outputs marked `requires_filter` and contributes its own base-profile efficiency or speed; current filters are shared with Potential Reactor parts and cover Stages 1-4. The Battery Cell slot is optional.

## Recipes and Returns

Component recycling is explicit recipe data under `rngtech:component_recycling`. The machine does not reverse-craft arbitrary crafting recipes. For source-linked machine and part targets, outputs should be a lossy subset of that target's authored recipe ingredients. If the source recipe uses an ingredient tag, the recycler recipe may name a concrete item accepted by that tag. Explicit fallback recipes without a normal source craft, such as calibrated components, broken circuits, and manual tool-head salvage, remain authored exceptions.

Component Recyclers have no Super Output, and recycler chassis and Disassembly Heads do not roll it. Every return is part of the craft that made the input, so a duplicated return would feed a loop. Recycler Gear that already rolled Super Output keeps the line, but it has no effect. Returns also follow the [recipe loop rule](../reference/machine-guidelines.md#output-amount): no return may exceed what the craft consumed of the same item, and `npm run moddex:check` checks every recycling recipe against every recipe that makes its input.

Current machine-block coverage includes staged Crusher, Furnace, Alloy Furnace, Battery Chassis, Component Recycler, solid-fuel burner, Solar Panel, Resonance Calibrator, Compressor Tank, and later utility/generator machine stacks. Current modular tool-part coverage includes all Tool Rods and all Tool Heads from Flint through Exotic. Loose Resonance Calibrator Gear covers Resonance Coils, Control Boards, and Stabilizer Matrices; these recycle to their core ingredient plus one matching material ingot, with redstone or a Stabilization Catalyst as the Recovery Filter bump. Alloy Crucibles also recycle through explicit recipes, returning their source crucible or casing and part of their material input. Damaged `rngtech:pitted_cavitation_rotor` outputs recycle through a Stage 5 recipe into `rngtech:recycling_byproduct` only, because the pitted item no longer records the source rotor material.

With JEI installed, Component Recycler recipes are exposed with input, up to three outputs, processing ticks, FE cost, minimum processing level, and Recovery Filter requirements for gated outputs. Crude Recycler compatibility is narrower than the full recipe category: it accepts `rngtech:manual_recycler_inputs`, including explicit Stage 1-2 early-machine recipes and Stage 1 iron component recipes, ignores FE cost, ignores Recovery Filter outputs, and uses a recipe-scaled manual cycle. Simple recipes complete in at least `80` ticks; longer recipes take twice their authored processing ticks.

First-pass returns are intentionally punishing and source-linked: a small guaranteed subset of the source recipe, plus one more source ingredient or material plate behind the Recovery Filter. Calibrated components have no filter bump.

Calibrated component recycling is the fallback for stability misses. It returns a deterministic raw-material share worth roughly `30-50%` of the calibration attempt and never more than one raw input per component, because the matching calibration recipes opt out of Super Output and the recycler has none. Reusable calibration patterns were never consumed, and catalysts or recipe stabilizers are intentionally lost.

Machine and part stacks do not remember the stability or stage of calibrated ingredients consumed during crafting. When those crafted targets recycle a calibrated-component slot, the return is the uncalibrated precursor item for that calibration family rather than a calibrated component with invented state.

Rarity, modifier tier, modifier count, and Refinement Potential never improve Component Recycler output. Those values belong to the energy-recycling step.

## Automation

| Side | Behavior |
|---|---|
| Top | Insert valid Component Recycler recipe inputs. |
| Bottom | Extract up to three output slots. |
| Sides | Receive FE. |

Gear slots and Refinement catalyst slots are not exposed through normal sided automation. The powered Component Recycler has no machine-side FE intake cap; external intake is limited by free buffer or installed-cell space plus the source or attached Universal Connector. The Crude Recycler has no FE capability; its top is occupied by the Hand Crank during normal use.

## Related Pages

- [Potential Reactor](potential-reactor.md)
- [Machine Parts](machine-parts.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
