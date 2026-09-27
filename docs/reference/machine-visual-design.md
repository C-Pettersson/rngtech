# Machine Visual Design

This page defines the baseline visual rules for placed RNGTech machine blocks. Use it with [Machine Guidelines](machine-guidelines.md) for behavior and [Machine GUI Design](machine-gui-design.md) for screens.

## Scope

These rules apply to single-block machine block models, face textures, active-state textures, and material-stage readability.

They do not define process-screen layout, JEI category layout, or cable-network overlays.

## Baseline Rules

- Use owned RNGTech textures for public-facing machine blocks.
- Use `64x64` block-face textures for placed machine faces unless a local asset family deliberately documents another size.
- Keep each machine family visually distinct by silhouette, face layout, front working feature, and service-port placement.
- Make the front face immediately readable as the working face.
- Keep top, side, and bottom faces functional: top faces should read as feed, access, heat, pressure, or tank surfaces when the machine uses those concepts.
- Active-state art should localize the motion or glow to the working feature. Do not wash the whole block face with a generic bright overlay.
- Stage and material changes should be visible at normal play distance through frame material, accent color, panel density, or tier-specific components.
- Bulk sidegrades should communicate extra lanes, extra sockets, larger chambers, or reinforced output handling instead of only changing color.

## Texture Organization

Prefer a predictable texture folder per machine family:

```text
textures/block/<machine_family>/<face>/<id>.png
```

Examples:

- `textures/block/crusher_chassis/front/steel.png`
- `textures/block/resonance_calibrator_chassis/side/nullite.png`

When a family already has a local folder convention, follow that convention instead of moving assets.

## Face Roles

| Face | Expected read |
|---|---|
| Front | Main work cue, active animation target, status window, jaw, lens, nozzle, aperture, or reaction chamber. |
| Side | Service panels, power bus, tanks, vents, item/fluid ports, reinforcement, or family identity marks. |
| Top | Input hatch, heat cap, fluid access, pressure collar, light collector, or safe flat casing when no top behavior exists. |
| Bottom | Output chute, drain, residue tray, feet, or reinforced base. |

## Family Readability

Machine families should remain recognizable across materials:

- Crushers use jaw, roller, feed, and output language.
- Furnaces use heat bands, chamber doors, insulation, and venting.
- Alloy Furnaces use crucible, mixer, or multi-input metalworking cues.
- Generators use fuel, catalyst, rotor, chamber, aperture, or output-bus cues tied to their generation method.
- Storage blocks use cell bays, charge indicators, bus bars, and containment frames.
- Fluid and gas processors use visible tanks, gauges, valves, compressor forms, or sealed pipework.

## Resonance Calibrator Chassis

Resonance Calibrator blocks should use owned `textures/block/resonance_calibrator_chassis/<face>/<id>` face textures, keep `64x64` block-face frames, and show a front tuning cue such as a lens, coil ring, waveform meter, or alignment target.

Bulk sidegrades should visibly communicate multiple calibration lanes. Nullite should read as the precision branch rather than a generic recolor.

## Active States

Active textures should answer what changed:

- Processing machines can move, glow, heat, pulse, or show work-progress equipment only on the front or relevant chamber face.
- Fluid machines can show fill, flow, bubbles, or pressure movement near the tank or valve.
- Generators can show localized output, burn, plasma, rotor, aperture, or reaction effects near the generation element.
- Debug-only machines may use simpler utility cues, but they should not be reused as public survival-machine identity.

Avoid active states that make every face equally bright, hide the material stage, or make the inactive block unreadable by contrast.

## Acceptance Checks

Before treating a placed machine asset as release-ready:

- The machine family is identifiable without reading the block name.
- The material or stage is distinguishable from adjacent stages.
- The front face is clear in inventory preview and in-world placement.
- Active and inactive states differ where runtime state exists.
- The texture does not rely on another mod's art identity.
- Public survival blocks do not use temporary debug-only art unless the release notes explicitly call it out as temporary.
