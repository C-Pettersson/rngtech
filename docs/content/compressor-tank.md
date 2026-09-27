# Fluid and Compressor Tanks

Status: Prototype


Gameplay resource ids:

- `rngtech:iron_compressor_tank`
- `rngtech:copper_compressor_tank`
- `rngtech:bronze_compressor_tank`
- `rngtech:steel_compressor_tank`
- `rngtech:aluminum_compressor_tank`
- `rngtech:titanium_compressor_tank`
- `rngtech:tungstensteel_compressor_tank`
- `rngtech:exotic_compressor_tank`

## Summary

Fluid and Compressor Tanks are staged fluid storage blocks. Iron, Copper, and Bronze display as plain Fluid Tanks and only provide ordinary fluid storage. Steel and later variants display as Compressor Tanks and become powered compressor tanks when fitted with one compatible [Battery Cell](battery-cells.md), at least one compatible Servo, and FE.

The early Fluid Tank resource ids still use the original `*_compressor_tank` ids for save and recipe compatibility.

Tank crafting uses non-rollable `rngtech:tank_frame` and `rngtech:pressure_tank_frame` intermediates instead of consuming earlier Fluid or Compressor Tank stacks. This keeps existing rolled or refined tanks usable after a player crafts later storage tiers.

Compressed storage preserves the fluid amount. A Steel tank that compresses `100 B` still holds `100 B` of ordinary fluid, but the Process tab displays its compressed physical volume as about `10 B` because Steel uses a `10x` compression ratio.

## Current Runtime Surface

Registered content:

| Content | Resource id |
|---|---|
| Iron Fluid Tank | `rngtech:iron_compressor_tank` |
| Copper Fluid Tank | `rngtech:copper_compressor_tank` |
| Bronze Fluid Tank | `rngtech:bronze_compressor_tank` |
| Steel Compressor Tank | `rngtech:steel_compressor_tank` |
| Aluminum Compressor Tank | `rngtech:aluminum_compressor_tank` |
| Titanium Compressor Tank | `rngtech:titanium_compressor_tank` |
| Tungstensteel Compressor Tank | `rngtech:tungstensteel_compressor_tank` |
| Exotic Compressor Tank | `rngtech:exotic_compressor_tank` |

The placed machine uses `CompressorTankBlockEntity`, `CompressorTankMenu`, and `CompressorTankScreen`. Plain Fluid Tanks support Process, Stats, and Refinement tabs. Steel+ Compressor Tanks also expose a Gear tab. No custom recipe type or JEI category is used in v1.

## Storage Model

Plain Fluid Tanks have one loose fluid store. Steel+ Compressor Tanks have two internal fluid stores:

| Store | Meaning | External capability |
|---|---|---|
| Loose tank | Normal fluid volume. Insertions and unpowered drains use this store. | Exposed as ordinary fluid. |
| Compressed store | Stored as decompressed-equivalent mB. UI also displays physical compressed volume as `equivalent / ratio`. | Drains as ordinary fluid only when compression Gear and FE are available. |

The block never creates or exposes a custom compressed-fluid type. External automation sees ordinary NeoForge fluid handlers.

Different fluids are rejected while either the loose tank or compressed store contains another fluid. Missing power never leaks, deletes, or voids stored fluid.

The Process tab has purge controls for loose fluid and, on Steel+ Compressor Tanks, compressed fluid. The craftable `rngtech:purge_bucket` can also right-click the placed tank to void up to `1000 mB`; normal use prefers loose storage, while sneak-use prefers compressed storage when present. Compressed purging deletes stored decompressed-equivalent mB directly and does not require FE.

## Gear Tab

Iron, Copper, and Bronze Fluid Tanks have no active Gear tab and do not accept Battery Cells or Servos. Steel+ Compressor Tanks expose:

| Gear Slot | Count | Runtime role |
|---|---:|---|
| Battery Cell | 1 | Required for compression and decompression. Provides extra stored FE but the Steel+ block still exposes FE input only. |
| Servo | 4 | At least one is required for Steel+ compression. More Servos increase the compression rate. |

Installed Gear must be at or below the tank chassis stage. The currently implemented Servos start at Stage 4 Steel, so compression starts with the Steel Compressor Tank.

Servo count multipliers:

| Servos | Rate multiplier |
|---:|---:|
| 1 | `1.0x` |
| 2 | `1.75x` |
| 3 | `2.5x` |
| 4 | `3.5x` |

Servo quality affects rate through the average installed Servo processing-speed contribution. Four full Servo affix sets are not multiplied together.

## Base Values

| Stage | Tank | Loose tank | Compressed physical | Ratio | Compressed effective | Base rate | Compress cost |
|---:|---|---:|---:|---:|---:|---:|---:|
| 1 | Iron | `16 B` | `0 B` | `1x` | `0 B` | `0 mB/t` | `0 FE/B` |
| 2 | Copper | `24 B` | `0 B` | `1x` | `0 B` | `0 mB/t` | `0 FE/B` |
| 3 | Bronze | `32 B` | `0 B` | `1x` | `0 B` | `0 mB/t` | `0 FE/B` |
| 4 | Steel | `48 B` | `48 B` | `10x` | `480 B` | `150 mB/t` | `200 FE/B` |
| 5 | Aluminum | `64 B` | `64 B` | `16x` | `1024 B` | `250 mB/t` | `320 FE/B` |
| 6 | Titanium | `96 B` | `96 B` | `24x` | `2304 B` | `400 mB/t` | `480 FE/B` |
| 7 | Tungstensteel | `128 B` | `128 B` | `32x` | `4096 B` | `700 mB/t` | `640 FE/B` |
| 8 | Exotic | `192 B` | `192 B` | `48x` | `9216 B` | `1000 mB/t` | `960 FE/B` |

Decompression costs `25%` of the compression FE per bucket and uses the same rate cap.

## Energy Behavior

Iron, Copper, and Bronze Fluid Tanks expose no FE capability. Steel+ Compressor Tanks expose FE input only. They never expose FE extraction and are not battery blocks.

For Steel+ tanks, FE received through the block energy capability fills the internal buffer first, then charges an installed Battery Cell when possible. During compression and decompression, work draws spend internal buffer FE first and then draw any remaining tick cost from the installed cell. Idle compressed storage costs `0 FE/t`.

## Automation

| Side | Behavior |
|---|---|
| Sides | Insert fluid-container items. Steel+ tanks also receive FE. Fluid capability accepts ordinary fluid into the loose tank and drains ordinary fluid from loose storage first. |
| Bottom | Extract fluid-container items after they are filled. |
| Gear and Refinement slots | Steel+ Gear slots and all Refinement slots are player-managed UI slots only. |

The Process tab has manual fluid-container slots for filling and emptying compatible fluid-handler items. Loose fluid remains drainable without power. Draining compressed storage requires a valid Battery Cell, at least one valid Servo, and enough FE.

## Drops

Breaking the block drops the machine item, any saved Gear inventory, and Refinement inventory. Internal loose and compressed fluid contents are not portable in v1.

## Modifier Eligibility

Iron, Copper, and Bronze Fluid Tank machine stacks and placed machines use the `FLUID_TANK` modifier eligibility profile and do not roll affixes in v1. Steel+ Compressor Tank machine stacks and placed machines use the `COMPRESSOR_TANK` modifier eligibility profile. It can roll FE storage/input, fluid transfer, processing speed, energy usage, energy transfer, and stability modifiers.

`FLUID_CAPACITY` and `COMPRESSION_RATIO` are authored/display stats in v1. They do not roll as normal affixes.

## Related Pages

- [Machine Parts](machine-parts.md)
- [Battery Cells](battery-cells.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
