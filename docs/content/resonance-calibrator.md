# Resonance Calibrator

Status: Prototype

The Resonance Calibrator turns ordinary material forms into RNGTech-calibrated components. External or vanilla machines can still make plates, coils, casings, circuits, and other raw ingredients, but RNGTech recipes can require calibrated state before those ingredients count as internal machine components.

Calibration is separate from refinement. Calibration creates component validity and quality state; refinement mutates rolled RPG traits on eligible machines and parts.

## Implemented Chassis

| Chassis | Stage | Identity | Runtime role |
|---|---:|---|---|
| Iron Resonance Calibrator Chassis | 1 | Stable frame | First calibrator body with one lane, broad output quality, and early Steel bootstrap reach. |
| Copper Resonance Calibrator Chassis | 2 | Conductive tuning frame | Optional early acceleration sidegrade with better transfer and weaker stability control. |
| Steel Resonance Calibrator Chassis | 4 | Reinforced stabilizer | Slower but more stable and output-guarded. |
| Titanium Resonance Calibrator Chassis | 6 | Precision frame | Better speed, precision, stability, and RP outcomes. |
| Lead Resonance Calibrator Chassis | 4 | Dense calibration bed | Two-lane bulk sidegrade with lower precision. |
| Tungstensteel Resonance Calibrator Chassis | 7 | Dense resonance array | Three-lane endgame bulk branch with `DENSE_PARALLEL`. |
| Nullite Resonance Calibrator Chassis | 7 | Phase precision frame | One-lane endgame precision branch with stronger quality and RP control. |

All chassis are block items, roll machine RPG traits, support placed-machine refinement, expose FE input, and share the same Process, Gear, Stats, and Refinement screen pattern.

Placed chassis art follows [Machine Visual Design](../reference/machine-visual-design.md#resonance-calibrator-chassis). Resonance Calibrator blocks should use owned `textures/block/resonance_calibrator_chassis/<face>/<id>` face textures, keep `64x64` block-face frames, and show a front tuning cue such as a lens, coil ring, waveform meter, or alignment target. Bulk sidegrades should visibly communicate multiple calibration lanes, while Nullite should read as the precision branch.

## Gear

The Gear tab uses:

| Gear slot | Required | Runtime role |
|---|---|---|
| Battery Cell | No | Adds portable FE storage. Without a cell, the machine keeps only its internal buffer and applies a speed and calibration-quality penalty. |
| Resonance Coil | Yes | Sets calibration stage reach, improves quality, and contributes FE transfer or speed through its component base profile. |
| Control Board | Yes | Improves calibration precision, stability, and useful Refinement Potential outcomes through its source-local base profile plus stored affixes. |
| Stabilizer Matrix | No | Improves stability floor, quality consistency, and catalyst efficiency through its source-local base profile plus stored affixes. |
| Calibration Patterns | Yes | Stores all six reusable family patterns internally; one stored pattern is selected as the active recipe pattern from the Gear tab. |

Implemented Gear materials are Iron, Copper, Steel, Titanium, Tungstensteel, and Nullite.

## Calibration Flow

1. Make ordinary material forms through vanilla, RNGTech, or external machines.
2. Craft a Resonance Calibrator, a reusable family pattern, and the required Resonance Coil and Control Board.
3. Install the Resonance Coil, Control Board, and reusable family patterns in the Gear tab, then select the active pattern.
4. Put the raw input, catalyst, and optional recipe stabilizer in the Process tab.
5. Supply FE directly or through the optional Battery Cell.
6. The output becomes a broad calibrated component item with `rngtech:calibration_state`.

Stored calibration state contains:

- Family: Structural, Kinetic, Thermal, Conductive, Storage, or Logic.
- Stage: component-stage identity for downstream gates.
- Stability: `0-100` quality value.
- Refinement Potential: a consumable quality budget for downstream gated recipes.

Patterns are reusable, stored internally in the Gear tab, and are not consumed. Catalysts are consumed unless catalyst efficiency preserves them. Recipe stabilizers are consumed when a calibration recipe declares one.

Low-stability calibrated outputs are not dead ends. Current calibrated components have Component Recycler recipes that recover the main raw input, such as Iron Gear from a Calibrated Kinetic Component, so a failed stability roll can be recouped for another attempt. The recycler does not return the reusable pattern, consumed catalyst, consumed recipe stabilizer, calibration state, or Refinement Potential.

## Recipe Surface

The `rngtech:calibration` recipe type is data-driven. Recipes can declare the minimum calibrator/coil stage separately from the output component stage. This lets an early recipe use Iron calibrator reach while still producing a Stage 2 component when the input material already carries that identity. Stability remains the quality gate for whether that specific output is good enough for downstream crafting. The default surface includes every calibration family, with Structural using multiple catalyst tiers so machine-frame gates can climb without changing the raw plate input:

| Family | Input example | Catalyst or stabilizer | Required reach | Output stage | Output |
|---|---|---|---:|---:|---|
| Structural | Iron Plate | Lapis Lazuli | 1 | 1 | Calibrated Structural Component |
| Structural | Iron Plate | Stabilization Catalyst | 2 | 2 | Calibrated Structural Component |
| Structural | Iron Plate | Matrix plus Stabilization Catalyst stabilizer | 4 | 3 | Calibrated Structural Component |
| Conductive | Copper Coil | Redstone | 2 | 2 | Calibrated Conductive Component |
| Conductive | Copper Coil | Matrix plus Stabilization Catalyst stabilizer | 4 | 2 | Calibrated Conductive Component |
| Conductive | Sparksteel Coil | Matrix plus Stabilization Catalyst stabilizer | 4 | 5 | Calibrated Conductive Component |
| Kinetic | Iron Gear | Redstone | 1 | 1 | Calibrated Kinetic Component |
| Kinetic | Iron Gear | Matrix plus Stabilization Catalyst stabilizer | 4 | 1 | Calibrated Kinetic Component |
| Thermal | Copper Casing | Glowstone Dust | 1 | 2 | Calibrated Thermal Component |
| Thermal | Copper Casing | Matrix plus Stabilization Catalyst stabilizer | 4 | 2 | Calibrated Thermal Component |
| Storage | Cell Shell | Redstone plus Stabilization Catalyst stabilizer | 2 | 2 | Calibrated Storage Component |
| Storage | Cell Shell | Matrix plus Stabilization Catalyst stabilizer | 4 | 2 | Calibrated Storage Component |
| Logic | Basic Electric Circuit | Quartz | 1 | 2 | Calibrated Logic Component |
| Logic | Basic Electric Circuit | Matrix plus Stabilization Catalyst stabilizer | 4 | 2 | Calibrated Logic Component |
| Logic | Diamond | Emerald | 6 | 6 | Calibrated Diamond Crystal |

The Basic Electric Circuit logic recipe and the basic Copper Casing thermal recipe require Stage 1 calibrator reach while storing Stage 2 calibration state. This lets Iron Resonance Calibrators produce the first Steel progression gates. Copper Resonance Calibrators remain an optional speed and transfer sidegrade instead of a mandatory step.

The `rngtech:calibrated_shaped` recipe serializer can require calibrated components by family, minimum stage, minimum stability, minimum Refinement Potential, and declared Refinement Potential consumption. Stage 3+ chassis recipes use those gates for calibrated component family, stage, and stability. Selected Resonance Calibrator branch recipes also require Refinement Potential.

The Stage 6 Melter body uses the Sparksteel Coil conductive calibration path. It requires a conductive Calibrated Component at Stage `5+` and `70+` stability, so the earlier Copper Coil Stage 2 conductive recipes do not satisfy that gate even when they roll high stability.

Resonance Calibrator chassis recipes do not consume the previous Resonance Calibrator generation. This keeps refined and well-rolled earlier chassis useful after an upgrade and lets Copper, Lead, Tungstensteel, and Nullite remain real sidegrade or branch choices instead of mandatory stepping stones.

| Chassis | Structural body | Current recipe pressure | Reason |
|---|---|---|---|
| Iron | Furnace Machine Chassis | Energy Coil, Basic Electric Circuits, Iron, and Redstone. | First calibrator body, reached from ordinary early machine crafting. |
| Copper | Machine Frame | Copper Resonance Coil, Basic Electric Circuits, and Copper. | Easy Stage 2 sidegrade that improves transfer and speed without consuming the Iron chassis. |
| Steel | Reinforced Machine Frame | Steel Control Board, Steel Resonance Coil, Stage 1 structural component at `55` stability, and Stage 2 logic component at `55` stability. | Easy Stage 4 stability body that keeps the Iron chassis available for early calibration work. |
| Lead | Reinforced Machine Frame | Iron Stabilizer Matrices, Stage 1 structural component at `45` stability, and Stage 2 storage component at `45` stability. | Easy Stage 4 bulk sidegrade that does not consume the optional Copper chassis. |
| Titanium | Advanced Machine Frame | Titanium Control Board, Titanium Resonance Coil, Stage 2 structural component at `70` stability with `1` RP consumed, and Stage 2 conductive component at `70` stability. | Easy Stage 6 precision body that uses advanced-frame pressure instead of consuming the Steel chassis. |
| Tungstensteel | Exotic Machine Frame | Tungstensteel Stabilizer Matrix, Tungstensteel Resonance Coil, Stage 3 structural component at `85` stability with `1` RP consumed, and Stage 6 logic Calibrated Diamond Crystal at `85` stability. | Hard Stage 7 dense branch; the crystal gate replaces previous-chassis dependency with late calibration pressure. |
| Nullite | Exotic Machine Frame | Nullite Control Board, Nullite Resonance Coil, Stage 2 conductive component at `85` stability with `1` RP consumed, and Stage 6 logic Calibrated Diamond Crystal at `85` stability. | Hard Stage 7 precision branch; the crystal gate makes Nullite expensive without consuming Titanium. |

The Calibrated Diamond Crystal is a Stage 6 logic-family calibration output used by late Stabilizer Matrix recipes and Stage 7 Resonance Calibrator chassis recipes. Tungstensteel and Nullite Stabilizer Matrix crafting requires a logic-calibrated crystal with at least Stage 6 and `75` stability, while Tungstensteel and Nullite Resonance Calibrator chassis require `85` stability, so the chassis gate sits after Titanium calibration and asks for a stronger crystal roll.

When JEI is installed, `rngtech:calibration` recipes appear under a Resonance Calibration category. Resonance Calibrator chassis are registered as catalysts, and the recipe view shows input, reusable pattern, catalyst, optional stabilizer, output family, stability range, Refinement Potential range, FE cost, and processing time. Recipe transfer sends patterns to the selected, matching, or first empty Gear-tab pattern slot.

## Progression

The player first uses normal processing to make raw forms, then calibrates the forms needed for RNGTech machine bodies and advanced components. Stage 3+ chassis now require calibrated components and frame-tier bodies, so external machines can speed up raw preprocessing but cannot bypass RNGTech's internal component gate.

The late-game choice is intentionally split:

- Tungstensteel favors dense multi-process throughput.
- Nullite favors single-output speed, precision, stability, and RP outcomes.

Future Exotic content should preserve that branch choice instead of merging both into one automatic best chassis.
