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

## Shaped Models

`Prototype`: every generator and the gas and ammonia chain behind them have shaped, non-cube models, part of moving placed machines toward visible working parts: the Solid Fuel Burners, Bio Generator, Algae Photobioreactor, Solar Panels and Solar Array Controller, Corrosion Cell, Coal Gasifier, Syngas Combustor, Steam Methane Reformer, Cavitation Generator, Ammonia Synthesizer, Ammonia Fuel Cell, Potential Reactor, and Vacuum Collapse Generator, plus the Battery Chassis and the Fluid and Compressor Tanks. The Tool Bench is a Stage 0 exception: a plain wooden table in its recipe's materials, without the slate casing and teal bands, whose cable port appears only while an Energy Connector is installed.

- Show the working mechanism itself (culture columns, a rotor behind a porthole, a fluid column in a sight glass, heat fins) instead of painting it on a cube face.
- Keep the RNGTech palette: dark slate casing, teal accent bands and trim, and steel pipework. Put teal only on deliberate band elements; textures mapped by element position must not carry accent stripes, which land at arbitrary spots.
- Use 64x64 part textures, mapped by element position, for a constant 4 texels per model pixel. Small dials, lamps, and framed glass may map a fixed region of a texture to one face.
- Frame glass that shows a level (sight glasses, rotameters) with visible rails, so a part-full tank still reads as a tube.
- Storage that cables meet from any side, such as the Battery Chassis, keeps a generic housing and shows its tier in the wiring metal. Each of its six sides shows a connector port where a cable or Universal Connector faces it, a solid, smooth contact wall against any other FE neighbour, and nothing otherwise, so every side looks connected without hiding the cells on open sides. Its installed parts are laid out for the active slot count and scaled to fill the housing. The Wooden chassis, like the Tool Bench, is a plain crate in its recipe's materials.
- Fluid storage that pipes meet from any side shows its fluid on every side and a connector port only where a cable or Universal Connector faces it. A plain Fluid Tank follows its recipe: a glass tank framed in its tier metal. A Compressor Tank is built around what makes it different: its pressure vessel dominates the block, its reinforcing hoops grow with the compression ratio, its compressed fill shows in an armoured window with a gauge, and its Servos are visible pumps that stroke while it compresses. The loose fluid moves to glass feed tubes in the corners.
- Give the machine one dedicated cable face: a flush port plate on the face centre, sized to the Universal Connector ring (4 to 12 on both axes) and backed by solid structure. Other faces may stay open.
- Keep the collision shape a full block, and mark the block `noOcclusion` so neighbouring faces still render through the gaps. A block placed by the field, such as a Solar Panel, may use a low collision shape matching its walking surface.
- A block placed in fields must tile: shared edges carry half a frame bar each, so neighbours form one continuous surface, and its cable port goes on the underside of a full-footprint ground pad so a buried cable is covered.
- An open model must not change sky access: when the old cube blocked sky light, keep blocking straight-down sky light (`propagatesSkylightDown` false, light block 1). Recheck light once on load for blocks saved as solid cubes.
- Draw moving or state-driven parts (fluid and culture levels, bubbles, glow, the installed part's material) in a block entity renderer from a small synced visual state. Keep blockstate model swaps for simple on/off cues such as a lamp. For blocks placed by the hundred, draw only the moving part from a pre-baked model, derive its motion on the client from world time, and offer a baked fallback.
- Generate the model and textures from a script under `tools/blockmodels/`, and keep the renderer's positions in step with it.

## Modular Tool Items

`Implemented`: an assembled Modular Field Tool draws its installed Tool Rod and Tool Head as one item.

- Give every head family its own silhouette, readable at 16x16 in any material: a classic double-pointed Pick, a sledge Hammer with flared faces, a rounded Shovel spade, a broad flat-fronted Digger scoop, a single-bit Axe, and a double-bit Treefeller. Material changes only the palette.
- Draw heads as flat item sprites in the vanilla tool style: a full one-pixel outline, darkest on the bottom and right, a bright line inside the top and left edges, and no faked depth. Keep each head mirror-symmetric about the handle, except the Axe's single blade, so it never looks crooked.
- Draw each head in the assembled tool's frame around the shared rod tip, and centre the loose head item by a whole-pixel shift that the renderer undoes. Keep every part transform at scale 1 so texels stay on the pixel grid.
- Every Tool Rod texture uses the same rod shape, so one head position fits every rod.
- Generate the head textures and the renderer's part transforms with `tools/itemmodels/tool_heads.py`. Check the result with `tools/itemmodels/modular_tool_preview.py --check`, which renders every head on every rod outside the game.

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
