# Machine GUI Design

This page defines the baseline visual design rules for RNGTech single-block machine screens. It is written for designers, implementers, and GPT-5.5 xhigh agents generating or reviewing Minecraft-style machine GUI layouts.

The goal is a consistent, readable process UI where a player can identify inputs, processing, outputs, stored resources, and machine status without reading a manual.

## Scope

This page applies to:

- Machine screen layout, especially the `Process` tab.
- Slot, tank, energy, progress, output, and status placement.
- Process-flow readability for item, energy, fluid, gas, heat, pressure, charge, residue, exhaust, and waste machines.
- Tab structure for `Process`, `Gear`, `Stats`, and `Refine`.
- Player inventory placement inside machine screens.
- Tooltip naming and compact quantity formatting for resource meters.
- UI review prompts and acceptance checks for generated screens.

This page does not define:

- Placed machine block textures.
- Chassis, front-face, top, side, bottom, or active block-state art.
- JEI category pages, unless they reuse the same process grammar.
- Cable, connector, multiblock, or world-overlay UI.

## Core Rule

A machine screen must read as a left-to-right process diagram.

```text
Inputs / required resources  ->  progress / transformation  ->  outputs / emitted resources / status
```

A player should understand four things within two seconds:

| Question                               | UI answer                                                                                                                                               |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| What does the machine consume?         | Left input lane: item slots, energy input, fluid tanks, gas tanks, catalysts, plates, or fuel.                                                         |
| What is happening right now?           | Center process lane: progress bar, reaction bar, conversion bar, or timed work indicator.                                                               |
| What does the machine produce or emit? | Right output lane: item outputs, residue slots, energy output, fluid/gas outputs, waste, exhaust, or stored product.                                    |
| Why is it not running?                 | A progress-adjacent status rail attached to the work indicator. Use a vertical progress/status stack when same-row placement would crowd output meters. |

The center lane is reserved for processing and status directly attached to processing. Do not place standalone red/yellow/green status squares in the center lane. A status rail may sit directly beside, above, or below the center progress bar only when it is visually grouped with that progress bar and does not read as a second progress element.

## Non-Negotiable Placement Rules

These rules override individual template preferences:

1. **Timed machines:** if a machine has a horizontal progress bar, the status rail must be progress-adjacent (prefer above). Progress-adjacent means one of: a stacked row directly below/above the progress bar, a same-row rail immediately to the right of the progress bar, or a compact side column directly touching the progress module.
2. **Meter symmetry outranks same-row status:** do not place status icons horizontally to the right of the progress bar if doing so consumes right-lane space, forces output meters to shrink, prevents left/right meters from sharing the same height, or pushes output meters out of alignment.
3. **Meter-heavy default:** when the screen has a left input meter and one or more right output meters, use a **vertical progress/status stack** by default: progress bar on one row, status icons directly below it or above it, with both rows kept in the center lane.
4. **Same-row status is conditional:** a same-row status rail to the right of the progress bar is acceptable only for one or two icons, or when it demonstrably preserves output spacing and equal-height meter geometry.
5. **Detached status rows:** do not place status squares as a detached top-right row when there is usable progress-adjacent space. Detached top-right status is only acceptable for passive/no-progress machines or screens with no usable progress-adjacent space.
6. **Progress labels:** do not add permanent process acronyms or process names such as `Synthesis`, `Reaction`, `Conversion`, or `Electrolysis` above, inside, or below a progress bar. Put that information in the progress tooltip or machine title instead.
7. **Resource meter height:** vertical energy, fluid, gas, heat, pressure, charge, waste, and exhaust meters on the same screen should use the same maximum height and baseline unless a machine-specific exception is documented.
8. **Resource tooltip names:** fluid/gas/waste/exhaust meter tooltips must start with the actual resource currently in the meter, not the meter role. Use `Syngas`, not `Input gas`; use `Carbon Exhaust`, not `Carbon input`.
9. **Compact resource amounts:** large fluid and gas amounts must use the compact RNGTech unit tiers in this document instead of long raw `mB` numbers.

## GPT-5.5 xhigh Agent Execution Rules

When designing or reviewing a machine GUI, follow this order:

1. Classify the machine as one of: passive generator/controller, recipe processor, chemical/gas synthesizer, electrochemical generator, direct resource-to-energy generator or field converter, storage/buffer, refinement utility, parallel processor, or special-case machine.
2. List every input, stored resource, catalyst, progress state, output, residue, exhaust, emitted energy, and status condition before placing UI elements.
3. Select the matching layout template from this document.
4. Place all inputs in the left lane, all progress in the center lane, and all outputs in the right lane.
5. Reserve the vertical input/output meter columns before placing status icons. Decide the common meter height and baseline first, then fit progress and status around that geometry.
6. For timed machines, attach the status rail to the progress bar. Use a vertical progress/status stack when a same-row rail would crowd the right lane, compromise meter equality, or create excessive horizontal spread.
7. Use a same-row rail to the right of progress only when it preserves left/right meter size, meter baseline, and right-lane output readability.
8. Only fall back to a right-output cluster or top-right cluster if the screen has no progress bar or no usable progress-adjacent space.
9. Anchor the player inventory at the bottom and never mix machine process components into the inventory area.
10. Add labels only where they remove ambiguity; keep labels short and consistent with the Minecraft pixel font.
11. Use actual resource names in meter tooltips and compact amount formatting for large values.
12. Reject any layout where a player could confuse a status indicator for a progress indicator, an input for an output, or a storage bar for a recipe-progress bar.

When returning a design, include a short component map:

```yaml
machine_id:
title:
process_type:
left_lane:
center_lane:
right_lane:
status_rail:
status_rail_orientation:
meter_height_standard:
inventory:
tooltip_examples:
notes:
```

The `status_rail_orientation` field must use one of these values:

| Value                   | Meaning                                                                                                                                        |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `progress_status_stack` | Preferred for meter-heavy timed machines. The progress bar and status rail are stacked vertically in the center lane.                          |
| `same_row_right`        | Status icons sit to the right of the progress bar. Only use when this does not crowd output meters.                                            |
| `side_icon_column`      | Status icons form a compact column directly beside the progress module. Use only when it preserves meter symmetry better than a same-row rail. |
| `right_output_fallback` | Status appears near outputs because there is no progress-adjacent room.                                                                        |
| `passive_top_right`     | Passive/no-progress machine status cluster.                                                                                                    |

The `notes` field must explicitly state whether the design avoided detached status rows, avoided permanent progress acronyms, preserved equal-height meters, used actual-resource tooltip labels, and selected a status rail orientation for a layout reason.

## Global Screen Structure

Every machine screen uses the same high-level structure:

```text
┌──────────────────────────────────────────────┐
│ Tabs: Process | Gear | Stats | Refine         │
├──────────────────────────────────────────────┤
│ Machine title                                │
│                                              │
│ [Left inputs]   [Center progress + status]   [Right outputs/meters]
│                                              │
│ Inventory                                    │
│ [9 columns x 3 player inventory rows]        │
│ [9 columns x 1 hotbar row]                   │
└──────────────────────────────────────────────┘
```

### Required zones

| Zone                   | Required behavior                                                                                                                                                               |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Tabs                   | Top of the screen. Keep tab order stable: `Process`, `Gear`, `Stats`, `Refine`.                                                                                                 |
| Title                  | Upper-left of the page body, directly under tabs. Use the machine display name.                                                                                                 |
| Left lane              | Inputs and required resources. This includes consumed energy, gas, fluid, items, catalysts, fuel, or plates.                                                                      |
| Center lane            | Process state and progress-attached status only. Use horizontal left-to-right progress unless the machine has no timed process. Do not add permanent process acronyms.          |
| Progress/status module | For timed machines, status icons must be visually attached to the progress bar. Use a stacked status row under/over progress when same-row placement would crowd output meters. |
| Right lane             | Outputs, produced resources, residue, emitted energy, waste, exhaust, and resource meters.                                                                                      |
| Inventory              | Bottom area only. Keep the 9-column player inventory and hotbar visually separate from the machine process area.                                                                |

The layout should feel stable across machines even when a machine has fewer controls. Empty space is acceptable when the machine has no relevant component for a zone.

## Pixel and Spacing Rules

Use the implementation's actual GUI texture size, but keep these relationships consistent.

| Element                 | Recommended size or spacing                                                                                                                                                                                                       |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Item slot               | Standard Minecraft slot size. Use the same slot frame everywhere in the UI set.                                                                                                                                                   |
| Player inventory        | 9 columns x 3 rows plus a 9-slot hotbar row. Keep row spacing identical to vanilla inventory.                                                                                                                                     |
| Vertical resource meter | Tall enough to read fill state at a glance. Energy, fluid, gas, heat, pressure, charge, waste, and exhaust meters should fill bottom-to-top and should share the same height on a screen.                                         |
| Horizontal progress bar | Wide enough to show partial progress clearly. Fill left-to-right. Do not place permanent text labels on the bar.                                                                                                                  |
| Status indicator        | Small square or icon attached to the progress/status module. On timed machines, choose `progress_status_stack` before `same_row_right` when meter symmetry is at risk. Must have tooltip text and should not rely on color alone. |
| Group spacing           | Leave visible empty pixels between input, progress/status, output, and inventory groups.                                                                                                                                          |
| Labels                  | Place above or beside the group they name. Do not place labels inside bars or over slot borders.                                                                                                                                  |

All UI art must be pixel-perfect. Do not anti-alias borders, scale sprites with blur, or use gradients that clash with the Minecraft UI style.

### Meter height standard

Use one vertical meter height per screen whenever possible.

```text
Good:  [Gas meter height 64] [Energy output meter height 64] [Heat meter height 64]
Bad:   [Gas meter height 92] [Energy output meter height 55] [Heat meter height 70]
```

Rules:

- Align comparable vertical meters to the same top and bottom pixel positions.
- Use the same empty frame, fill direction, and border thickness for all resource meters in the same screen family.
- Reserve the right lane for output meters before spending horizontal space on status icons. If status icons would force unequal meter heights or crowd right-side meters, stack the status rail under or above the progress bar in the center lane.
- If a machine needs a deliberately smaller auxiliary meter, make the difference meaningful: label it, frame it differently, or move it to `Stats`. Do not mix random meter heights for visual decoration.
- Adjacent output meters on the right should feel like a coordinated output cluster, not unrelated widgets.
- Meter height and baseline are reserved before status icon layout. If a status icon row would force unequal meters, change the status rail orientation instead of changing the meters.

## Tabs

### Process

The default tab. It shows live machine operation, inputs, progress, outputs, resource buffers, and status.

### Gear

Use for upgrades, installed modules, side configuration, filters, and automation settings. Do not place primary recipe progress here.

### Stats

Use for read-only numbers: energy capacity, input/output rate, efficiency, heat, cycle time, maintenance, parallel lanes, stored resource totals, error history, or detailed resource quantities.

### Refine

Use only for machines with refinement behavior: recipe tuning, catalyst selection, quality filters, tiered outputs, or secondary processing modes. If the machine has no refine behavior, the tab may remain disabled or reserved, depending on the existing UI convention.

Tab order must not change per machine. Tab availability may change, but the player's muscle memory should remain intact.

## Component Grammar

### Item slots

Place consumed item slots in the left lane. Place produced item slots in the right lane. Do not use only the slot position to explain meaning when two slots are visually similar; add a short label such as `Plate`, `Elect.`, `Residue`, `Input`, or `Output`.

### Energy bars

Use vertical bars for stored FE or energy buffers.

| Energy role                                             | Placement                                                                                                    |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Consumed energy                                         | Left lane, near other inputs.                                                                                |
| Stored energy buffer for a passive generator/controller | Left lane unless the right lane is specifically showing emitted output.                                      |
| Produced or emitted energy                              | Right lane, near outputs/status.                                                                             |
| Internal charge state                                   | Place next to the component it affects, but avoid the center progress lane unless it is the progress itself. |

Energy bars should include a lightning/FE icon or tooltip. Do not let a plain energy bar be confused with a gas tank. Energy meters should follow the same height standard as nearby fluid/gas/heat meters.

### Fluid, gas, and waste tanks

Use vertical tanks/meters for fluids, gases, and waste. Inputs belong on the left. Outputs, products, exhaust, and waste belong on the right. When two or more gas tanks appear together, label them with short, font-safe text such as `N2`, `H2`, and `NH3`; avoid subscript characters unless the game font supports them clearly.

Rules:

- Keep all comparable vertical resource meters the same height on a screen.
- Use distinct fill colors/textures or icons so energy, gas, fluid, and exhaust do not read as the same resource.
- Label small visible groups with compact names, but use the tooltip for exact resource name, direction, and amount.
- A resource meter tooltip title must be the actual current resource name when known.

### Progress bars

Progress bars are central and horizontal by default. They show recipe progress, reaction progress, charging progress, conversion progress, or timed work. They must not double as a status/error indicator.

Use left-to-right fill direction unless a specific machine family has an established alternate convention.

Do not add permanent text above, below, or inside a progress bar just to name the process. The machine title and progress tooltip already provide context. For example, a chemical machine should not have `Synthesis` printed above the progress bar; the progress tooltip can say `Synthesis progress: 48%`.

A label may appear near progress only when it distinguishes multiple parallel lanes, such as `Lane 1`, `Lane 2`, or recipe icons. Do not use long process nouns as static decoration.

### Status indicators and progress/status stack

Status indicators explain whether the machine is running, blocked, idle, missing resources, disabled, obstructed, over capacity, unable to output, or outside environmental requirements.

For timed machines, status indicators form a **status rail** attached to the progress bar. The status rail should be placed according to this priority:

1. `progress_status_stack`: progress bar and status rail stacked vertically in the center lane.
2. `same_row_right`: status rail immediately to the right of progress, only when meter geometry is unaffected.
3. `side_icon_column`: compact column beside the progress module, only when it preserves meter geometry better than a same-row layout.
4. `right_output_fallback`: status near outputs only when there is no progress-adjacent room.
5. `passive_top_right`: top-right/right status cluster for passive or no-progress machines.

Preferred meter-heavy layout:

```text
[Left input meter]      [horizontal progress bar       ]      [Right output meters]
                        [status icons row              ]      [Right output meters]
```

This is the default for machines with left input meters and right output meters. It keeps the status rail attached to progress while leaving horizontal room for equal-height output meters.

Same-row layout, only when it preserves meter symmetry:

```text
[Left inputs]      [horizontal progress bar] [status icons]      [right output meters]
```

Fallback layout when there is no progress-adjacent space:

```text
[Left inputs]      [horizontal progress bar]      [right output meters]
                                           [status icons near outputs]
```

Passive/no-progress layout:

```text
[Left storage or generation meter]                  [top-right/right status cluster]
```

Rules:

- Choose status placement after reserving left and right meter space. Equal-height comparable meters outrank same-row status placement.
- Use a vertical progress/status stack when a same-row rail would crowd the right output lane or force unequal input/output meter sizing.
- In a vertical progress/status stack, put status icons directly below the progress bar by default; place them above only if the lower row conflicts with slots, labels, or inventory.
- Align the stacked status row to the progress bar width or right edge so it reads as part of the progress module, not as a floating warning row.
- A lone red/yellow/green square below the progress bar is forbidden. A grouped status rail directly below the progress bar is allowed when it is aligned and visually attached.
- Use a same-row rail to the right of the progress bar only when it preserves meter symmetry and output spacing.
- Detached top-right status rows are forbidden for timed machines if the status rail could fit in a progress-adjacent stack or beside the progress bar.
- Keep status icons outside the fill area of the progress bar.
- Status icons must have tooltips and must not rely on color alone.

### Coordinate implementation rules for progress/status rails

Use these rules when translating the design into GUI constants or screen-rendering code. They are intended to prevent the common failure where status icons are placed as a long horizontal row after the progress bar even though a vertical stack would preserve meter symmetry.

Rules:

- `progress_status_stack` is mandatory by default for meter-heavy timed machines with all three of these traits: at least one left vertical input meter, at least one right vertical output meter, and three or more status/output-condition icons.
- Reserve `RESOURCE_METER_Y`, `RESOURCE_METER_HEIGHT`, left meter `x`, and right meter `x` before calculating progress or status coordinates. Status icons must adapt to the reserved meter geometry, not the other way around.
- A same-row horizontal status rail is considered active when status icons share the progress bar's vertical band and extend rightward from the progress bar. This is invalid when it pushes into the output-meter lane, shortens output meters, or leaves less than one clean meter column on the right.
- For a stacked status rail below progress, derive icon coordinates from the progress bar:
    - `STATUS_ICON_Y = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT + 4` is the preferred first pass.
    - `STATUS_ICON_X` should align within the progress module: left-aligned, centered, or right-aligned under the progress bar width.
    - The final icon in the row must remain inside the center progress/status module, not in the right output-meter lane.
- For a stacked status rail above progress, use `STATUS_ICON_Y = PROGRESS_BAR_Y - ICON_SIZE - 4` only when the lower row collides with slots, labels, or inventory.
- Do not implement meter-heavy layouts as `STATUS_ICON_X = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH + gap` with all status icons using the same y-band as the progress bar unless the design has explicitly selected `same_row_right` and documented why meter geometry is unaffected.
- If the status rail has four icons, use `progress_status_stack` unless there is a machine-specific exception. Four icons almost always consume enough horizontal space to crowd right-side meters.

Preferred status order, left-to-right for stacked/same-row rails or top-to-bottom for columns:

```text
[running/paused] [missing input] [blocked output] [disabled/config] [environment]
```

Status color guidance:

| State                     | Visual treatment                                           |
| ------------------------- | ---------------------------------------------------------- |
| Running                   | Green or lit icon plus tooltip.                            |
| Idle / waiting            | Gray or muted icon plus tooltip.                           |
| Missing input             | Yellow/orange icon plus tooltip.                           |
| Blocked output            | Red icon plus tooltip.                                     |
| Disabled by player/config | Gray/red icon plus tooltip.                                |
| Environmental block       | Context icon, such as sun/cloud/obstruction, plus tooltip. |

Never rely on color alone. Use an icon shape, symbol, or tooltip so colorblind players and small-screen players can still understand the state.

## Layout Templates

### Template A: Passive generator or controller

Use for machines like the Solar Array Controller.

```text
Left lane:    stored/generated energy buffer
Center lane:  empty, output-rate meter, or generation meter only if meaningful
Right lane:   status cluster: sunlight, obstruction, output enabled, network state
Bottom:       player inventory
```

Rules:

- Do not invent a recipe progress bar for passive generation.
- Put energy storage where it is easy to monitor, usually left.
- Put environmental and output-state indicators on the right.
- Top-right status clusters are acceptable here because the machine has no central timed progress bar.
- If an energy output bar exists separately from storage, place emitted/output energy in the right lane.

Accepted pattern from the example: Solar Array Controller uses energy on the left and operational status on the right.

### Template B: Single recipe processor

Use for item/fluid processors, corrosion cells, furnaces, crushers, presses, melters, and similar machines.

```text
Left lane:    item/fluid/energy inputs
Center lane:  horizontal progress bar + progress-attached status rail
Right lane:   output item/fluid/energy, residue, exhaust, or waste
Bottom:       player inventory
```

Rules:

- Keep item inputs stacked or grouped on the left.
- Keep the progress bar centered between inputs and outputs.
- Prefer `progress_status_stack` when the machine has left/right meters or when there are three or more status icons.
- Use `same_row_right` only when it does not crowd outputs or force unequal meter sizing.
- If the machine produces residue, place residue on the right and label it.
- If the machine emits energy, place the energy output bar on the right.
- Keep all right-side resource meters the same height when multiple vertical meters appear.

Melter special case:

- Treat the Melter as a fluid-heavy single recipe processor: item inputs, heat,
  FE, and the consumed fluid stay in the left lane; recipe progress and status
  stay in the center; the produced fluid tank stays in the right lane.
- The consumed-fluid and product-fluid meters may be wider than energy and heat
  meters, but they must keep the same vertical meter height, baseline, fill
  direction, and container-slot alignment.
- Melter fluid tooltips should start with the current tank fluid when the menu
  can identify it. Use `Water` for an empty input fallback and `Melter Product`
  for an empty output fallback.

Accepted pattern from the example: Corrosion Cell uses `Plate` and `Elect.` inputs on the left, reaction progress in the middle, and residue plus output energy on the right.

### Template C: Multi-input chemical or gas synthesizer

Use for machines like the Ammonia Synthesizer.

```text
Left lane:    energy input + gas/fluid input tanks
Center lane:  synthesis/reaction progress bar + progress-attached status rail
Right lane:   product/output resource meters
Bottom:       player inventory
```

Rules:

- Group gas inputs together on the left and label each tank.
- Place consumed FE on the left with the inputs.
- Place the synthesis progress in the center.
- Place product/output resource on the right.
- Use `progress_status_stack` when product meters need the right lane or when the status rail has multiple icons.
- Use `same_row_right` only when the input and output meters still match height and baseline.
- Do not place a lone status/error indicator below the center progress bar. A grouped status rail directly below the progress bar is allowed when it is aligned and visually attached.
- Keep input and output meters visually coordinated, with matching meter heights when the layout allows.

Required correction from the example: the red status square in the Ammonia Synthesizer must move out of the loose center-lane position and into the progress-attached status rail or right fallback status cluster.

### Template D: Electrochemical generator or fuel cell

Use for corrosion generation, ammonia fuel cells, membrane-stack machines, and no-combustion generators.

```text
Left lane:    fuel, electrolyte, plates, membrane inputs, or required energy starter
Center lane:  reaction, membrane, or electrochemical progress + progress-attached status rail
Right lane:   energy output, residue output, charge/output status
Bottom:       player inventory
```

Rules:

- Show electrochemical activity as progress, not flames.
- Put emitted energy on the right.
- Put residue/drain outputs on the right, usually below or beside energy output.
- Put blocked-output and missing-fuel status in a progress-attached status rail. Prefer `progress_status_stack` when emitted-energy or residue meters need the right lane.
- Use actual resource names in fluid/gas/exhaust tooltip titles.

### Template E: Storage or battery machine

Use for machines whose main purpose is storing, routing, or buffering energy/items/fluids.

```text
Left lane:    input or accepted resource
Center lane:  storage visualization only if it is the primary purpose
Right lane:   output/export state, lock state, transfer status
Bottom:       player inventory
```

Rules:

- Do not use a progress bar unless the machine performs timed conversion.
- The main stored resource may be visually central if storage is the entire purpose.
- Keep import/export directions explicit.
- Use consistent meter heights for all storage/export meters on the screen.
- A standalone fluid tank may use one wide vertical meter for the stored fluid,
  but it must keep the standard resource-meter height, bottom-to-top fill, and
  top/bottom baseline instead of becoming a squat horizontal reservoir.

### Template F: Parallel or multi-lane processor

Use when a machine runs multiple recipes in parallel.

```text
Left lane:    aligned input columns per lane
Center lane:  stacked progress bars, one per lane, with lane status attached to each row if possible
Right lane:   aligned output columns per lane
Bottom:       player inventory
```

Rules:

- Use one horizontal row per lane.
- Do not compress multiple lanes into one ambiguous progress bar.
- Align each input, progress bar, status indicator, and output horizontally so the lane is readable.
- If each lane has independent status, place each status icon beside or directly under its lane progress bar, whichever preserves lane alignment and output spacing.
- If status is machine-wide, place one shared status rail below the group of progress bars by default, or beside the group only if meter and output spacing remain clean.

### Template G: Direct resource-to-energy generator or field converter

Use for machines that convert a resource directly into power, field strength, charge, or another output without a conventional item output. Examples include turbine-like field converters, gas combustors, and resource-to-FE generators.

```text
Left lane:    input resource meter or fuel/resource slot
Center lane:  conversion progress bar + progress-attached status rail
Right lane:   emitted energy/output meters, waste/exhaust meter if present
Bottom:       player inventory
```

Rules:

- Put the consumed resource meter on the left, such as syngas, steam, coolant, or hot exhaust.
- Put the conversion progress bar in the center with no permanent acronym label.
- Use `progress_status_stack` by default for meter-heavy generators and converters: progress bar on one row, status icons directly below it.
- Use `same_row_right` only when the left input meter and right output meters can still use the same height and clean baselines.
- Put output energy/resource meters on the right.
- Keep all vertical resource meters the same height.
- Use actual resource names in tooltip titles, such as `Syngas`, `Carbon Exhaust`, or `Steam`, not `Input gas` or `Output fluid`.

## Example-Specific Direction

### Solar Array Controller

Good pattern:

- Energy is placed on the left, where players naturally scan for stored/generated power.
- Status indicators are placed on the right, which correctly separates machine state from resource storage.
- The center is not overloaded with fake progress.

Optimized direction:

```yaml
machine_id: solar_array_controller
process_type: passive_generator_controller
left_lane:
    - vertical_energy_buffer: stored FE or generated buffer
center_lane:
    - empty_or_generation_rate_meter: only if useful
right_lane:
    - daylight_status
    - obstruction_status
    - output_enabled_status
    - optional_output_rate_indicator
status_rail: top-right or right of output-rate indicator, because there is no timed progress bar
status_rail_orientation: passive_top_right
meter_height_standard: use the screen's standard vertical meter height for energy/output meters
inventory: bottom, unchanged
```

### Corrosion Cell

Good pattern:

- Inputs are on the left: plate and electrolyte.
- Reaction progress is in the middle.
- Residue and energy output are on the right.
- Labels clarify the non-obvious slot roles.

Optimized direction:

```yaml
machine_id: corrosion_cell
process_type: electrochemical_generator
left_lane:
    - plate_slot_labelled_Plate
    - electrolyte_slot_or_tank_labelled_Elect
center_lane:
    - horizontal_reaction_progress
    - status_rail_attached_to_progress
right_lane:
    - vertical_energy_output_bar
    - residue_output_slot_labelled_Residue
status_rail:
    - progress_status_stack_when_it_preserves_output_space
    - same_row_right_only_if_meter_symmetry_is_preserved
    - right_output_fallback_only_if_no_progress_adjacent_space
status_rail_orientation: progress_status_stack or same_row_right, chosen by meter geometry
meter_height_standard: energy and electrolyte/resource meters use consistent height where both are visible
inventory: bottom, unchanged
```

### Ammonia Synthesizer

Good pattern:

- The machine uses multiple inputs on the left.
- The synthesis progress bar is centered.
- The output/resource bar is on the right.

Required correction:

- Move the red status square out of the loose center-lane position. It competes with the progress bar and reads like a second process element.
- Put status in a grouped progress-attached status rail. Prefer a stacked rail below the progress bar when product/resource meters need the right lane.

Optimized direction:

```yaml
machine_id: ammonia_synthesizer
process_type: chemical_gas_synthesizer
left_lane:
    - vertical_energy_input_bar
    - vertical_nitrogen_input_tank_labelled_N2
    - vertical_hydrogen_input_tank_labelled_H2
center_lane:
    - horizontal_synthesis_progress
    - status_rail_attached_to_progress
right_lane:
    - vertical_output_resource_or_energy_bar
status_rail:
    - progress_status_stack_when_product_meters_need_right_lane_space
    - same_row_right_only_if_meter_symmetry_is_preserved
    - running_or_blocked_icon
    - missing_N2_icon_if_needed
    - missing_H2_icon_if_needed
    - output_blocked_icon_if_needed
status_rail_orientation: progress_status_stack by default for multi-icon status
meter_height_standard: FE, N2, H2, and output meters use the same height when presented as vertical meters
tooltip_examples:
    - "Nitrogen: 8000 / 8000 mB"
    - "Hydrogen: 8000 / 8000 mB"
    - "Ammonia: 12.3 / 16B"
inventory: bottom, unchanged
```

## Labeling Rules

Use labels when the component role is not self-evident.

| Resource or slot | Preferred visible label                                               |
| ---------------- | --------------------------------------------------------------------- |
| Plate input      | `Plate`                                                               |
| Electrolyte      | `Elect.`                                                              |
| Residue output   | `Residue`                                                             |
| Nitrogen         | `N2`                                                                  |
| Hydrogen         | `H2`                                                                  |
| Ammonia          | `NH3`                                                                 |
| Energy           | `FE` or lightning icon with tooltip                                   |
| Syngas           | `Syngas` if space allows, otherwise icon/short gas mark plus tooltip  |
| Carbon Exhaust   | `Exhaust` or `C-Exh.` if space is constrained                         |
| Input            | `Input` only when a specific material/resource label is not available |
| Output           | `Output` only when a specific product/resource label is not available |

Avoid labels longer than the available pixel space. Prefer a short visible label plus tooltip over cramped text.

Visible labels may describe a slot role when needed, but tooltips for resource meters must start with the actual content name when known.

## Interaction and Tooltip Rules

Every interactive or stateful component must expose a tooltip.

| Component                   | Tooltip content                                                                                                                                           |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Item slot                   | Accepted item type, current stack, automation direction if relevant.                                                                                      |
| Energy bar                  | Current FE, capacity, input/output rate, whether it is input, output, or storage.                                                                         |
| Fluid/gas/waste tank        | Actual resource name, amount, capacity, direction/role, and whether it is required, produced, stored, exhausted, or blocked.                              |
| Progress bar                | Current recipe/process, percent/cycle progress, time remaining if available.                                                                              |
| Status icon                 | Exact reason: running, idle, missing input, blocked output, disabled, obstructed, insufficient energy, invalid structure, tank full, or output slot full. |
| Tab                         | Tab name and short purpose if not obvious.                                                                                                                |

A red status icon without a tooltip is not acceptable.

### Resource meter tooltip titles

Fluid, gas, exhaust, and waste meter tooltips must begin with the actual content of the meter.

Use this title priority:

1. Current tank contents.
2. Configured tank resource.
3. Active recipe resource.
4. Machine-specific empty label.
5. Generic fallback only if nothing else is known.

Correct examples:

```text
Syngas: 8000 / 8000 mB
Carbon Exhaust: 4000 / 4000 mB
Water: 8000 / 8000 mB
Steam: 10 / 16MB
Empty Syngas Tank
Empty Carbon Exhaust Tank
```

Incorrect examples:

```text
Input gas: 8000 / 8000 mB
Output fluid: 8000 / 8000 mB
Carbon input: 4000 / 4000 mB
Tank: 10MB / 16MB
```

Role and direction can appear on later tooltip lines:

```text
Syngas: 8000 / 8000 mB
Role: Input fuel gas
Automation: Top insert, side extract disabled
```

```text
Carbon Exhaust: 4000 / 4000 mB
Role: Produced exhaust
Automation: Side extraction
```

### Fluid and gas quantity formatting

Internal fluid and gas values may be stored in `mB`, but UI text should compact large values using RNGTech display tiers.

|                Stored amount | Display rule                                        | Example                      |
| ---------------------------: | --------------------------------------------------- | ---------------------------- |
|                `< 10 000 mB` | Show raw `mB`.                                      | `1500 mB -> 1500 mB`         |
| `10 000` to `< 1 000 000 mB` | Divide by `1000`, suffix `B`.                       | `12 300 mB -> 12.3B`         |
|            `>= 1 000 000 mB` | Divide by `1 000 000`, suffix `MB`.                 | `10 000 000 mB -> 10MB`      |
|        `>= 1 000 000 000 mB` | Divide by `1 000 000 000`, suffix `GB` when needed. | `1 250 000 000 mB -> 1.25GB` |

Rounding rules:

- Prefer up to three significant figures for compact values.
- Use one decimal when it improves readability, such as `12.3B`.
- Strip trailing `.0`, such as `10MB` instead of `10.0MB`.
- Do not round a non-zero amount down to `0`.
- Use no space inside compact values: `12.3B`, `10MB`, `1.25GB`.
- For tooltip fractions, choose the display tier from the larger of current amount and capacity so both sides use the same unit.
- When both sides share the same unit, the unit may appear once at the end: `Syngas: 8000 / 8000 mB`, `Steam: 10 / 16MB`.

Do not display large values such as `10000000 / 16000000 mB` in normal machine tooltips unless the user is in a debug/developer mode.

## Visual Style

Use the existing Minecraft machine UI language:

- Light or medium gray panel base.
- Darker borders for slots, bars, meters, and group frames.
- Pixel-perfect square corners.
- Minimal decoration; controls should look functional, not ornamental.
- Muted backgrounds with brighter colors only for active resources, warnings, and fills.
- Consistent tab shape and selected-tab treatment.
- High contrast between filled and empty portions of bars.
- Consistent height and baseline for comparable vertical resource meters.

Do not overuse saturated colors. The player should notice errors and active resources immediately because they are rare, not because the entire UI is brightly colored.

## Forbidden Patterns

Do not use these patterns:

- Loose, standalone status indicators placed under or inside the center progress lane.
- Horizontal progress-plus-status rows that crowd the right output lane or force unequal vertical meter sizes.
- Same-row status placement chosen even though a vertical progress/status stack would preserve equal-height left/right meters.
- Detached top-right status rows on timed machines when status icons could fit in a progress-adjacent stack or beside the progress bar.
- Red/yellow/green squares with no tooltip or icon meaning.
- Output slots on the left unless the machine has a documented right-to-left flow, which should be avoided for RNGTech machines.
- Input tanks on the right and output tanks on the left.
- Progress bars that fill in an unexpected direction without explicit reason.
- Permanent progress-bar acronyms or process labels such as `Synthesis`, `Reaction`, `Conversion`, or `Electrolysis`.
- Decorative bars that look like progress but have no gameplay meaning.
- Inventory slots mixed with machine input/output slots.
- Labels overlapping slot frames, bars, meters, or tab borders.
- Resource bars without role distinction between input, storage, and output.
- Inconsistent heights for comparable vertical resource meters on the same screen.
- Fluid/gas/exhaust tooltips titled by role when the actual content is known, such as `Input gas` instead of `Syngas`.
- Large raw `mB` values where compact `B`, `MB`, or `GB` display should be used.
- Different tab order per machine.

## Conflict Resolution

When a machine has too many components for the available space:

1. Preserve the left-center-right flow before preserving decoration.
2. Keep the player inventory fixed at the bottom.
3. Reserve enough left and right lane space for comparable vertical meters to share the same height and baseline.
4. Use a vertical progress/status stack before moving status to a detached output or top-right cluster.
5. Use a same-row status rail only after confirming it does not crowd output meters or break meter symmetry.
6. Reduce label length before moving components into the wrong zone.
7. Remove permanent progress labels before moving status away from the progress module.
8. Group related inputs vertically on the left.
9. Group related outputs vertically on the right.
10. Keep comparable meter heights consistent; if the screen cannot fit equal-height meters, move secondary diagnostics to `Stats`.
11. Move secondary diagnostics to `Stats` rather than crowding `Process`.
12. Move configuration controls to `Gear` rather than crowding `Process`.
13. Use tooltips for exact values instead of forcing long text into the panel.

When a machine has no timed process, leave the center lane empty or use a meaningful rate/storage visualization. Do not invent progress.

## Review Checklist

Before accepting a new or revised machine GUI, verify:

- The tab row is present and the tab order is stable.
- The machine title is visible at the top-left of the page body.
- Inputs and consumed resources are on the left.
- The center lane contains only progress, timed work, progress-attached status, or an intentional empty space.
- Outputs, emitted resources, residue, exhaust, waste, and output meters are on the right.
- If the machine has a progress bar, status icons are progress-adjacent: stacked directly under/over progress or placed beside progress without crowding meters.
- Same-row progress/status placement was not used when a vertical stack would preserve equal-height left/right meters.
- No detached top-right status row was used on a timed machine unless there was no usable progress-adjacent space.
- Status icons are not confused with progress bars.
- Every status icon has a tooltip and does not rely on color alone.
- No permanent progress acronym or process-name label was added, such as `Synthesis`.
- Energy bars are clearly input, output, or storage.
- Fluid/gas/waste tanks are labeled, icon-coded, or tooltip-identified.
- Comparable vertical meters on the same screen use the same height and baseline.
- No status rail caused a left input meter and right output meter to become different sizes when they should match.
- Resource meter tooltip titles use actual content names, such as `Syngas` or `Carbon Exhaust`, when known.
- Fluid and gas quantities use compact formatting for large values.
- Player inventory is aligned at the bottom and visually separate from machine controls.
- Slot borders, bar borders, meter borders, and text are pixel-perfect.
- Labels are short, readable, and not overlapping UI components.
- The UI remains readable at normal Minecraft GUI scale.
- The layout still works when bars/meters are empty, full, active, blocked, and disabled.

## Quick Scoring Rubric

Use this rubric for automated or GPT-assisted review.

| Score | Meaning                                                                                                                                                                                              |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 5     | Layout is immediately readable, follows left-center-right flow, uses the correct progress/status orientation, preserves equal-height meters, and uses actual resource names with compact quantities. |
| 4     | Minor spacing or label issues, but all gameplay meaning is clear and no major placement rule is violated.                                                                                            |
| 3     | Usable, but one role is ambiguous or a component is slightly misplaced.                                                                                                                              |
| 2     | Multiple ambiguous controls, weak process/status separation, horizontal status crowding, inconsistent meters, or poor tooltip naming.                                                                |
| 1     | Inputs, progress, outputs, or status cannot be reliably identified.                                                                                                                                  |
| 0     | Layout violates the core flow and should be redesigned.                                                                                                                                              |

A player-facing UI must score at least 4 before implementation.

## New Machine GUI Prompt Template

Use this prompt when asking a GPT-5.5 xhigh agent to design or review a machine GUI:

```text
Design or review the Process tab for <machine_name>.
Use the RNGTech Machine GUI Design rules.

Machine behavior:
- Inputs:
- Consumed energy:
- Fluids/gases/waste/exhaust:
- Progress/timed process:
- Outputs:
- Residue/byproducts:
- Status conditions:
- Tabs needed:

Return:
1. Machine classification.
2. Component map using left_lane, center_lane, right_lane, status_rail, status_rail_orientation, meter_height_standard, inventory.
3. Notes on labels, icons, and tooltips.
4. Tooltip examples that use actual resource names and compact quantities.
5. Confirmation that status is progress-adjacent, and whether it uses a vertical progress/status stack, same-row rail, side icon column, or fallback placement.
6. Confirmation that same-row status placement was not chosen when it would crowd right-side meters.
7. Confirmation that comparable vertical meters use the same height.
8. Confirmation that permanent progress acronyms/process labels were avoided.
9. Any violations of the left-to-right process grammar.
10. Final acceptance checklist.
```

## Final Acceptance Statement

A correct RNGTech machine GUI is a compact process diagram. The player reads it left to right: resources enter, work happens, status is visible as part of the work module, and outputs appear on the right. The design fails if status competes with progress, if a horizontal status row steals space from right-side output meters, if outputs look like inputs, if resource bars lack role clarity, if comparable meters use inconsistent heights, if tooltips hide the actual resource name behind generic roles, if large resource quantities are not compacted, or if inventory space is mixed with machine controls.
