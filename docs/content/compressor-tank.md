# Fluid and Compressor Tanks

Status: Prototype

Player guide: [Fluid and Compressor Tanks](https://c-pettersson.github.io/rngtech/compressor-tank/)

Fluid and Compressor Tanks are staged single-fluid storage blocks. Iron, Copper, and Bronze are plain Fluid Tanks; Steel and later Compressor Tanks add an FE-driven compressed store that needs one compatible [Battery Cell](battery-cells.md) and at least one Servo.

## Implementation Contract

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

The early Fluid Tank ids keep the original `*_compressor_tank` ids for save and recipe compatibility.

The placed machine uses `CompressorTankBlockEntity`, `CompressorTankMenu`, and `CompressorTankScreen`. Plain Fluid Tanks support Process, Stats, and Refinement tabs; Steel+ tanks add a Gear tab. No custom recipe type or JEI category is used in v1.

Tank crafting uses non-rollable `rngtech:tank_frame` and `rngtech:pressure_tank_frame` intermediates instead of consuming earlier tank stacks, so rolled or refined tanks stay usable after a player crafts later tiers.

| Side | Behavior |
|---|---|
| Sides | Insert fluid-container items. Steel+ tanks also receive FE. Fluid capability accepts ordinary fluid into the loose tank and drains loose storage first. |
| Bottom | Extracts filled fluid-container items. |
| Gear and Refinement slots | Player-managed UI slots only. |

Internal loose and compressed fluid contents are not portable in v1; breaking the block drops the machine item, saved Gear, and Refinement inventory.

## Storage Model

Plain Fluid Tanks have one loose fluid store. Steel+ Compressor Tanks have two:

| Store | Meaning | External capability |
|---|---|---|
| Loose tank | Normal fluid volume. Insertions and unpowered drains use this store. | Exposed as ordinary fluid. |
| Compressed store | Stored as decompressed-equivalent mB. UI also displays physical compressed volume as `equivalent / ratio`. | Drains as ordinary fluid only when compression Gear and FE are available. |

The block never creates or exposes a custom compressed-fluid type; external automation sees ordinary NeoForge fluid handlers. Different fluids are rejected while either store contains another fluid. Missing power never leaks, deletes, or voids stored fluid. Compressed purging deletes stored decompressed-equivalent mB directly and does not require FE.

## Gear

| Gear Slot | Count | Runtime role |
|---|---:|---|
| Battery Cell | 1 | Required for compression and decompression. Provides extra stored FE; the block still exposes FE input only. |
| Servo | 4 | At least one is required for Steel+ compression. More Servos increase the compression rate. |

Installed Gear must be at or below the tank chassis stage. Implemented Servos start at Stage 4 Steel, so compression starts with the Steel Compressor Tank.

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

Iron, Copper, and Bronze Fluid Tanks expose no FE capability. Steel+ Compressor Tanks expose FE input only; they never expose FE extraction and are not battery blocks.

FE received through the block energy capability fills the internal buffer first, then charges an installed Battery Cell when possible. Compression and decompression spend internal buffer FE first, then draw any remaining tick cost from the installed cell. Idle compressed storage costs `0 FE/t`.

## Modifier Eligibility

Iron, Copper, and Bronze Fluid Tank machine stacks and placed machines use the `FLUID_TANK` modifier eligibility profile and do not roll affixes in v1. Steel+ Compressor Tank machine stacks and placed machines use the `COMPRESSOR_TANK` profile, which can roll FE storage/input, fluid transfer, processing speed, energy usage, energy transfer, and stability modifiers.

`FLUID_CAPACITY` and `COMPRESSION_RATIO` are authored/display stats in v1. They do not roll as normal affixes.

## Related Pages

- [Machine Parts](machine-parts.md)
- [Battery Cells](battery-cells.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
