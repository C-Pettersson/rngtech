# Machine Guidelines

This page defines the baseline rules for adding or changing RNGTech machines. Use the [Current Implementation Matrix](current-implementation.md) to confirm what exists in code, use the [Crusher](../content/crusher.md) as the reference implementation for processing-machine behavior, and use [Machine Visual Design](machine-visual-design.md) for block art, texture resolution, active-state, and family readability rules.

## Balance Position

Base machines should lean hard-tech: expensive enough to require infrastructure, staged parts, and deliberate power planning. RNGTech should start closer to a GTNH-style baseline than a lightweight kitchen-sink baseline.

Good modifiers, refined parts, and stronger cells are the path toward a lighter feel. A well-rolled or refined machine may approach the comfort of lighter packs such as SkyBlock-style or FTB-style play, but the unmodified base machine should not begin there.

For processing machines:

- Recipe time, FE cost, input tier, output amount, and gear requirements should all matter.
- Recipe hardness pressure should use `PROCESSING_LEVEL`. For Crushers, the installed Crush Head supplies the active level and recipes declare `required_processing_level`; under-level work is allowed but takes longer, costs more total FE, and can jam instead of hard-blocking the recipe.
- If a machine family has staged chassis, the placed chassis should be the machine body. Do not model it as a separate item-only ingredient unless the machine itself is not staged.
- Chassis stage may gate installed Gear stage, but chassis material should also change base stats or fixed behavior identity so same-stage choices can matter.
- Processing speed should increase throughput, not silently make each craft cheaper.
- Reduced energy usage and output amount are the main ways to cheaper effective processing-machine production.
- Config keys may tune baselines, but renaming a key is appropriate when an old local config would preserve an obsolete balance target.

## Energy Model

Processing machines should not hide large built-in FE storage unless they are explicitly energy-storage machines.

- A machine's built-in buffer should be just large enough for work-cycle smoothing.
- Meaningful portable or replaceable storage should come from Battery Cells in Gear slots.
- A newly placed processing machine without a Battery Cell should show a small capacity.
- A processing machine without an installed Battery Cell should have a production penalty. For the Crusher reference implementation this is a `0.75x OUTPUT_AMOUNT` multiplier.
- Speed modifiers should preserve base FE per craft by increasing FE/t as processing time falls.
- `ENERGY_USAGE` may change total FE per craft. `EFFICIENCY` is reserved for generators, fuel duration, batteries, storage loss, and other non-recipe efficiency behavior.
- `ENERGY_GENERATION` belongs only on machines that actually generate FE and have the `HAS_FE_GENERATION` capability.
- Processing machines should expose energy demand, not a machine-side external intake cap. External FE intake is limited by free buffer space and the source, normally a Universal Connector.
- Generators should expose energy supply, not a machine-side export cap. Generator-installed Battery Cells are capacity buffers and should not cap generated FE/t or export FE/t.
- Universal Connector tier is the primary player-facing RNGTech wiring cap. UI read models should show connector cap, last FE in/out, and bottleneck warnings when the connector limits transfer.
- Connector tiers are per-tick caps in both directions, shared across every source or sink using that connector. See [Universal Cable](../content/basic-wire.md#energy-transfer-limits).
- Any machine-side FE cap that remains, such as a connector-controlled generator's export or a Battery Chassis cell rate, must also be a per-tick budget shared by every side and connector, never a per-call limit.
- FE transfer stats should remain meaningful for storage-like blocks, Battery Cells, Battery Chassis, and connector-controlled outputs such as Solar Array Controller, Cavitation Generator, and Vacuum Collapse Generator. Do not show `ENERGY_TRANSFER` on ordinary processors or generators where it no longer controls the real player-facing limit.

## Output Amount

Fractional output is deterministic and should use a per-input bonus bank.

Example: `1.25x OUTPUT_AMOUNT` gives the normal output each craft and adds `0.25` to the bonus bank. When the bank reaches `1.0`, the craft pays one extra output and keeps any remainder.

`SUPER_OUTPUT_CHANCE` is separate from the deterministic bank. When it succeeds, it adds one extra copy of the base stackable output only after the machine has resolved its ordinary output amount.

Rules:

- Show a bonus output progress bar when a machine can produce fractional extra output.
- Place the bonus bar above the normal processing progress bar.
- The bonus bar shows banked, already-earned fractional progress only. Do not fill it with the in-flight craft's pending increment; put next-craft payout details in hover text.
- Changing the input item or its components resets the bonus bank.
- The output slot preview should include the next bonus payout when the current bank would pay one.
- No set of recipes may loop, and no recipe cycle may return more of an item than it consumed.
- Reversible conversion recipes must opt out of bonus output. For Crusher and Furnace recipes, set `bonus_output: false` when the output can smelt or process back into the original input, such as ingot-to-dust and matching dust-to-ingot conversions. Alloy Furnace, Metal Press, Melter, and calibration recipes use the same field, which defaults to `true`; a calibration sets it to `false` when its calibrated component recycles back into the calibration input. Machines apply Output Amount, Super Output, salvage, and other yield effects only through the shared check on that field.
- Component Recyclers have no bonus output. Every recycling recipe returns ingredients of the craft that made its input, so a return must not exceed what any recipe making that input consumed of the same item.
- `npm run moddex:check` runs two recipe checks:
    - `tools/moddex/check-recipe-loops.mjs` audits every recipe type, with vanilla tags and wood, chest, and furnace crafting included. It fails when some mix of recipes returns every consumed input, other than water, cobblestone, and calibration catalysts or stabilizers, while gaining items or producing net FE. Bonus-eligible outputs count at their worst-case bound from `tools/moddex/recipe-loop-bounds.json`, and Potential Reactor stripping of recyclable machines and parts counts at a conservative payout. Each failure names a minimal loop and its likely fix.
    - `tools/moddex/check-recycling-returns.mjs` compares every recycling recipe with every recipe that makes its input, resolving ingredient tags and alternatives. It fails when a return exceeds the craft's consumption, or when calibration Super Output could make a return match or exceed it. Add `--report --recycler-super-output` to list the recipes that recycler Super Output would break.
- Machine-owned progression recipes should author `machine_xp` only on progression sources. For Furnace recipes, use positive `machine_xp` on ores, raw metals, crushed inputs, and alloy blends; leave reversible dust-to-ingot loops, malformed recovery, food, decorative blocks, and ordinary utility smelts at the default `0`. Omitted Furnace `machine_xp_band` values derive from `target_temperature`, or `minimum_temperature` when no target is authored.
- Do not use random chance for routine fractional output. Randomness belongs in explicit special outcome systems.

## Machine Tabs

Use these tab responsibilities consistently.

| Tab | Responsibility |
|---|---|
| Process | Active recipe input, output, work progress, and work-state indicators. |
| Gear | Installed machine parts, Battery Cells, component slots, and other equipment-like slots. |
| Stats | Effective stats that apply to this machine's actual capabilities. |
| Refinement | Shared placed-machine refinement UI for compatible machines. |
| Mastery | Machine-owned XP, level, unspent passive points, and passive-tree nodes for machines that implement progression. |

Battery Cells are Gear, not Process. A processing machine may expose a side-insertable Battery Cell slot for automation, but the UI should still present it as equipment.

Fuel slots belong on Process only when the machine burns fuel as part of its active work loop. Battery Cells are not fuel slots.

## Machine Progression

Machine XP and passive-tree state belongs to the machine chassis, not to a player or world position. When a machine supports progression, store XP, level, spent points, and unlocked nodes on a machine-owned item/block data component and copy that component anywhere the machine's identity is expected to survive, including block drops and pick-block.

Processing XP should be granted only after work actually completes and output is produced. Batched processing should grant XP per completed item. Failed starts, jams, output-blocked waits, invalid recipes, and no-power pauses should not grant XP.

Passive nodes must go through the same effective-stat aggregation used by chassis, Gear, rarity, and modifiers. Slot-changing nodes must also participate in server-side slot validation and unlock validation; unlocking a node that blocks or limits an occupied Gear slot should fail unless the current gear is already compatible.

All functional base-machine families should use the [shared Machine Mastery graph](../systems/machine-mastery.md); integrate new families through a start and capability adapter. Gear and cables are excluded. Attribute totals, inherent conversions, and explicit scaling are separate. Increased/reduced modifiers share an additive bucket; more/less factors multiply independently. Absolute ceilings resolve last.

The Mastery graph deliberately includes irrelevant effects. Keep them visible and selectable, label applicability per effect, and apply supported penalties even when a benefit is inactive. Capability filtering still applies to the Stats tab. Refunds and copied targets must enforce connectivity, earned points, matching starts, and current Gear legality on the server. Old per-machine mask limits do not apply to the shared stable-ID graph.

## Stats Tab

Stats must be capability-driven. Do not show stats that cannot apply to the machine.

- Do not show energy generation on a Crusher or any non-generator.
- Do not show fuel efficiency or fuel slots on electric-only machines.
- Do not show heat stats unless heat affects behavior.
- Use [Modifier Eligibility](modifier-eligibility.md) capability flags as the source for which stat families are relevant.
- Prefer concise stat rows. Avoid explanatory text inside the machine screen.
- Keep visible text to the minimum needed to identify the tab, slot, or stat.

## Process UI

Process screens should stay compact and readable.

- Do not show FE or progress as always-visible text when the bar already communicates the state.
- Use bars, icons, slot position, color, and fill state as the primary way to represent machine state.
- Hover text should carry exact FE, raw progress, bonus-bank values, penalties, and other precise details.
- Prefer graphical state plus hover detail over permanent labels or explanatory copy.
- Use long labels only when an icon, slot position, or short stat name would be ambiguous.
- For fluid-capable Process screens, bucket or other fluid-container slots should sit directly below their corresponding fluid meter or bar, centered on that meter when space allows.
- Fluid tanks that can trap a wrong fluid should expose a small purge control beside the visible tank meter. Purging voids up to `1000 mB`, never returns fluid, and should identify the tank and current fluid in hover text.
- Keep inappropriate machine concepts out of the view. A Crusher does not display generation, burn time, or fuel text.

## Fluid Purging

RNGTech fluid machines use purging as the standard survival-safe tank cleanup affordance. The craftable `rngtech:purge_bucket` right-clicks supported RNGTech machine tanks and voids up to `1000 mB`; normal use prefers input or storage tanks, while sneak-use prefers output, byproduct, or compressed tanks. Process-screen purge buttons target the selected visible tank exactly.

Purging is destructive cleanup only. It must not recover buckets, items, energy, or byproducts, and it must not touch third-party fluid handlers, debug sinks/sources, Universal Cable transfer state, or connector network state. Empty and unsupported targets should leave storage unchanged and report actionbar feedback.

If a purged input tank can affect an active recipe, the machine must reset its active progress, recipe snapshot, or equivalent local work state before or during the drain. Output-tank purges should not reset progress unless that machine's invariants require it. Compressor Tanks expose loose and compressed targets separately; compressed purging voids stored decompressed-equivalent mB directly and does not require FE.

## Gear Slots

Gear slots are equipment for the placed machine.

- Machine parts should contribute their per-stack modifiers only while installed.
- Required processing parts must be hard requirements, not optional stat bonuses. For example, a Crusher must not process without a valid Crush Head.
- Required processing parts may also provide recipe-pressure stats. For example, Crush Heads provide Crusher hardness through `PROCESSING_LEVEL`; lower-than-recipe hardness slows Crusher work, raises total FE cost, and can jam.
- Gear slots should save with the block entity and drop with the machine inventory.
- Standard machine-part Gear slots should not be exposed through ordinary sided item automation unless the machine family explicitly supports that.
- Battery Cell Gear slots may be side-insertable when the machine needs automation-friendly power swapping.
- Slot validation must reject unrelated items, even if the UI slot is visually close to other machine slots.

## Automation

Use standard NeoForge capabilities for item and energy automation.

- Existing Universal Cables support item, fluid, and energy transfer. Do not expand them into a shared storage system or storage-network replacement unless project scope changes.
- Keep automation surfaces predictable and machine-local.
- Sided handlers should expose only the slots that make sense for ordinary automation.
- Gear automation is opt-in per slot type, not automatic for every Gear slot.

## Documentation

When adding or changing a machine:

- Update the machine content page.
- Follow [Machine Visual Design](machine-visual-design.md) when adding or revising placed machine block assets.
- Update [Current Implementation Matrix](current-implementation.md) when the code-backed surface changes.
- Update [Modifier Eligibility](modifier-eligibility.md) if capabilities or rollable stats change.
- Update [Machine Stats](machine-stats.md) only when stat semantics change.
- Keep planned behavior labeled as `Planned`; do not present it as code-backed behavior.
