# Machine Guidelines

This page defines the baseline rules for adding or changing RNGTech machines. Use the [Current Implementation Matrix](current-implementation.md) to confirm what exists in code, use the Crusher (`CrusherBlockEntity`; player guide: [Crusher](https://c-pettersson.github.io/rngtech/crusher/)) as the reference implementation for processing-machine behavior, and use [Machine Visual Design](machine-visual-design.md) for block art, texture resolution, active-state, and family readability rules.

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
- Connector tiers are per-tick caps in both directions, shared across every source or sink using that connector. See [Universal Cable](https://c-pettersson.github.io/rngtech/universal-cable/#energy).
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
- Holding Shift over a stat row shows the server-recorded math behind it, with one line per source. New stat sources must apply inside a labelled `MachineStatAccumulator.source(...)` scope, or use `apply(source, modifier)`, so the breakdown names them. Affixes, Gear parts, and Mastery nodes are already labelled. Anything left unlabelled shows as "Machine".
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

## Machine Design Notes

Design intent and balance rules behind shipped machines that the code does not state. Slots, sides, numbers, and recipe data live in code and data, and the [player wiki](https://c-pettersson.github.io/rngtech/) describes current behavior. Planned additions are in [Design Proposals](../design-proposals.md#planned-extensions-to-shipped-machines).

### Cross-Machine Rules

- Staged machine bodies, Gear, and tool parts craft from plain materials, Machine Frames, or non-rollable intermediates such as `rngtech:tank_frame`, `rngtech:pressure_tank_frame`, and `rngtech:solar_panel_frame`. They never consume the previous rolled stack, so a well-rolled or refined older piece stays worth keeping beside its upgrade. The one exception is the Nullite Servo, which consumes a Titanium Servo.
- Stage 0 Gear, such as the Flint Crush Head, is disposable starter tooling. Gear above Stage 0 is a long-term investment.
- Machine Frame tier is a recipe cost only. It adds no stats, traits, or refinement behavior.
- Stage 5-8 material-form recipes on the Crusher and Furnace author `2x`, `3x`, `4x`, and `6x` the early FE baseline. The Crusher high-hardness and Furnace high-heat surcharges stack on top of that ramp.
- Cables and connector modules never roll modifiers. Transfer is fixed by the endpoint item.

### Crusher

- Under-level work multiplies processing time and total FE by the same factor, so FE/t stays at the optimal-hardness rate. The penalty is a slower, costlier craft, not a power spike.
- Crush Head hardness applies only inside Crusher recipes. World ore harvesting uses a separate ore-hardness gate on the Modular Pick and Hammer.
- Author an explicit `machine_xp_band` when a recipe's training value should follow the processed material rather than its hardness gate.

### Furnace

- Vanilla ore and raw-metal smelts are mirrored as `rngtech:furnace` recipes with the same heat gates, so vanilla inputs cannot bypass metal heat progression.
- Crushed material smelts directly for early hopper lines, but at `3x` the time and `1.5x` the FE of dust, so the dust step stays worthwhile.
- Steel comes from the Alloy Furnace, as Steel Blend or direct ingots. Do not add a Furnace Steel Dust smelt.
- The Stage 0 solid-fuel Furnace is deliberately slow (`0.54x` base speed).
- Lead is a Stage 4 four-lane sidegrade at half per-lane speed. The Aluminum Heat Core is a low-heat (`850`), fast-transfer sidegrade: flat Max Temperature rolls can lift it over early heat bands, but its identity stays low-heat and fast.

### Alloy Furnace

- Early alloy paths are deliberately slow, lossy, and additive-heavy. Stronger chassis and crucibles unlock cleaner direct-ingot recipes.
- Sparksteel and Arclite are slow direct-ingot recipes, so the first coils, circuits, and Titanium infrastructure need no alloy-dust chain. Invar Blend and Sparksteel Blend stay registered for compatibility with no survival recipe.
- Late alloys (Aethergold, Nullite, Tungstensteel) stay slower and lossier than ordinary metal processing.

### Metal Press

- The Crude Metal Press is the pre-Steel bootstrap: unsafe conditions pause it with inputs untouched. Failure outputs start with the Stage 4 Steel Metal Press, matching the failure rule in [Component Stages](component-stages.md#stage-rules).
- Circuit gates: a Bronze Heat Core meets the Basic heat and stability gates, Advanced needs a well-controlled Crude Press, and Elite and Ultimate target the Steel Press or a heavily upgraded Crude Press.
- Press bodies roll modifiers with Stage 8 weighting and Refinement Potential, because they are not a staged chassis ladder that carries investment forward.
- The Crude Energy Connector is the only connector crafted without the Connector Mold, so early wiring does not wait for a press.

### Melter

- The default `1200` heat minimum makes Stage 6 Titanium heat the first working path.
- Melter Electrolyte Solution is a bulk supply. Dry electrolyte in the Component Assembler keeps early battery production available before the Melter.
- Dense Algae Biomass stays a Bio Generator fuel. No Melter recipe accepts it.

### Resonance Calibrator

- Calibration creates component validity and quality. It is separate from refinement, which changes rolled traits.
- Stage 3+ chassis recipes require calibrated components and frame-tier bodies, so other mods can speed up raw preprocessing but cannot bypass RNGTech's internal component gate.
- Early recipes may need less calibrator reach than the stage they store (Basic Electric Circuit and Copper Casing: reach `1`, Stage `2`). Iron calibrators therefore produce the first Steel gates, and Copper stays an optional sidegrade.
- The late branch split is deliberate: Tungstensteel for dense multi-lane throughput, Nullite for single-output speed, precision, stability, and RP. Future Exotic content should keep the choice instead of merging both into one best chassis.
- The Melter's conductive gate (Stage `5+`, `70+` stability) is set so the Stage 2 Copper Coil calibrations cannot satisfy it, however well they roll.

### Fluid and Compressor Tanks

- External automation only ever sees ordinary fluid. There is no custom compressed-fluid type.
- Missing power never leaks, deletes, or voids stored fluid. Only purging destroys fluid.
- Servo count sets the rate on a fixed curve and Servo quality is averaged, so four rolled Servos never multiply together.
- Fluid Capacity and Compression Ratio are authored identity, not rollable affixes.

### Battery Cells and Battery Chassis

- Final material cells roll traits once on assembly, which is why they do not stack. The Primed Cell Core is the stackable, trait-free intermediate.
- Bank throughput is cell-led: a high-stage chassis cannot make weak cells move FE fast. The chassis transfer rating and burst rating are identity readouts, not caps or boosts.
- More slots is not strictly better. Low-slot chassis carry higher Global Modifier Strength so a focused bank stays useful after larger ones unlock, and global modifier power is budgeted across slots.
- Battery Chassis is storage gameplay, so cell rate limits stay visible and meaningful. Generators and processors instead leave the connector as the visible limit.
- A chassis never creates portable capacity. A removed cell drops back to its own capacity.

### Bio Generator and Composting

- Crushing a potato or carrot into Organic Reagent is deliberately energy-negative (`546 FE` burned minus `600 FE` crushing, against `120 FE` for the raw crop). Organic Reagent is a convenience fuel, not a power gain.
- Crushing seeds into Compost Feedstock before composting is deliberately energy-positive, to reward the Crusher step.
- Composted, Rich, and algae biomass are Bio Generator-only fuels. Do not tag them as vanilla furnace, Solid Fuel Burner, or broad modded fuel.
- Specific fuel families beat broader tags for Bio Chamber power bonuses: saplings and seeds use their own stats, not Plant Power.

### Solid Fuel Burner

- Each fuel item holds a fixed FE (`10 FE` per effective burn tick). Better Heat Cores and Energy Generation spend fuel faster rather than extracting more FE from it. Higher FE/t comes from Syngas at Stage 5, not from better burners.
- Logs burn for a quarter of their vanilla burn time on purpose.
- Chassis accept lower-stage Gear so mixed setups can trade speed, efficiency, fuel queue size, and behavior.
- Fuel Governor pauses only when the buffer cannot hold more generated FE. It never blocks export, and it never changes fuel cost, duration, or generation while there is room.

### Potential Reactor

- Payout scales with what the item was (stage, Machine Mastery, Ascendancy Seals), so the reactor recoups retired gear instead of paying for cheap crafts. Repeat burns of the same item lose value.
- Authored base stats and fixed behavior pay nothing. Only rarity, rolled affixes, and Refinement Potential pay FE.
- `rngtech:scrap` is residue-only with no reactor fuel recipe, so recovered residue cannot loop back into FE.

### Component Recycler

- The intended chain is the Potential Reactor first (RPG value into FE), then the Component Recycler (physical returns). Rarity, affixes, and Refinement Potential never improve recycler output.
- Returns are an explicit, lossy subset of the source recipe. The recycler never reverse-crafts arbitrary recipes, and first-pass returns are deliberately punishing.
- Calibrated-component recycling is the fallback for stability misses: roughly `30-50%` of the attempt and never more than one raw input. Patterns are reusable and catalysts are lost.
- Crafted machines do not remember calibrated ingredient state, so their calibrated slots return the uncalibrated precursor.

### Corrosion Cell

- Anode FE/t stays below the same stage's main generator: the Syngas Combustor at Stage 5, the Ammonia Fuel Cell at Stage 6, and the Vacuum Collapse Generator at Stage 7 (and its all-Exotic setup at Stage 8).
- Corrosion waste stays item-backed so recycling or chemistry recipes can consume it without a fluid slice.

### Cavitation Generator

- Pressure is a recipe display and balance field only. There is no pressure network.
- The Aethergold rotor is a short-lived burst sidegrade: faster cycles and more FE for much more wear. It does not raise the fallback export rate, so its burst needs a Sparksteel or better Energy Connector.
- Worn rotors all become one generic pitted item, so recycling them returns only generic byproduct, never the source rotor material.

### Vacuum Collapse Generator

- It is the Stage 7 primary generator. It uses its own Dimensional Stabilizer instead of sharing Potential Reactor Containment Linings.
- The Void Chamber stage is the hard recipe gate. An under-stage Collapse Nozzle adds instability pressure instead of blocking the recipe.

### Ammonia Chain

- The Ammonia Catalyst Bed and Fuel Cell Membrane have neutral base stats. An unrolled part only gates recipes.

### Algae and Water Support

- The Algae Photobioreactor and both Dehumidifiers are no-FE support machines with no RPG traits.
- Dense Algae Biomass beats burning the same algae directly, which rewards the crafting step.
- The Silica Gel Dehumidifier ignores sky, biome, and weather. Bead consumption is its balancing cost.

### Modular Field Tools

- Assembled tools are blank carriers. All rolls live on the installed head and rod, and the assembled tool is never a refinement target.
- Crafting-grid repair is disabled so installed part data is never discarded.

### Miner's Companion

- The Magnet and Mining Lamp are fixed upgrades that never roll affixes.
- Block Chew acts only on new drops from player block breaks, never on existing inventory, machine outputs, chests, or mob drops.
- The Mining Lamp light is client-side only, so it never affects mob spawning or crop growth.

### Refinement

- Rarity controls open affix slots, not affix tier eligibility.
- Add and upgrade operations have no separate success chance. The rolled cost is capped by remaining Refinement Potential.
- Every refinement entry point must use the shared `com.rngtech.rpg.refinement` library so legality checks and results stay consistent.
- The Exotic Affix Forge writes per-item history so repeat work on the same item gets more expensive.

## Documentation

When adding or changing a machine:

- Update the machine's player wiki page under `wiki/`, and add any design intent or balance rule to [Machine Design Notes](#machine-design-notes).
- Follow [Machine Visual Design](machine-visual-design.md) when adding or revising placed machine block assets.
- Update [Current Implementation Matrix](current-implementation.md) when the code-backed surface changes.
- Update [Modifier Eligibility](modifier-eligibility.md) if capabilities or rollable stats change.
- Update [Machine Stats](machine-stats.md) only when stat semantics change.
- Keep planned behavior labeled as `Planned`; do not present it as code-backed behavior.
