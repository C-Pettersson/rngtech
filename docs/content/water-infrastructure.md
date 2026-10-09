# Water Infrastructure

Status: Prototype

Player guides: [Wooden Dehumidifier](https://c-pettersson.github.io/rngtech/wooden-dehumidifier/), [Silica Gel Dehumidifier](https://c-pettersson.github.io/rngtech/silica-gel-dehumidifier/)

RNGTech has two no-FE atmospheric water sources: the Stage 2-3 Wooden Dehumidifier multiblock and the Stage 5 Silica Gel Dehumidifier bead column. Neither has RPG traits, Refinement, or FE capabilities.

## Wooden Dehumidifier

| Surface | Current behavior |
|---|---|
| Controller | `rngtech:wooden_dehumidifier` |
| Frame block | `rngtech:wooden_dehumidifier_frame` |
| Ingredient | `rngtech:waterproofed_planks` |
| Block entity | `WoodenDehumidifierBlockEntity` |
| Menu / screen | `WoodenDehumidifierMenu` / `WoodenDehumidifierScreen` |
| Recipe type | `rngtech:wooden_dehumidifier_conversion` |
| RPG traits / refinement | No |
| FE | No input, output, or storage |
| Item automation | Top inserts conversion items and empty containers; bottom extracts filled containers |
| Fluid automation | Sides and bottom drain the selected output only; no fluid insertion from any side |
| Frames | Formed frames proxy item insertion and drain-only selected-fluid output to the controller; frames expose no energy, and unformed frames expose no item or fluid capability |

The controller scans a bounded cluster around itself and detects the full face-connected frame shell. A valid structure needs at least `4` detected frames. All detected frames switch to their formed visual state, while tank capacity and passive water production use a capped effective frame count of `25`: `1,000 mB` per tank and `60 mB/min` base water per effective frame. Output is capped at `1,500 mB/min` after humidity and rain bonuses. Water production requires solar access from the controller or at least one formed frame.

The selected output cycles between Water and the unique fluids produced by loaded conversion recipes. Purging the water tank resets active conversion progress. Conversion consumes its item and water only at batch completion; the default recipes turn `#c:dusts/coal` or `rngtech:gasification_residue` plus `1,000 mB` water into `1,000 mB` `rngtech:carbon_exhaust` over `6,000 ticks`.

### Humidity

Humidity bands come from RNGTech biome tags under `data/rngtech/tags/worldgen/biome/dehumidifier_humidity/`:

| Band | Multiplier |
|---|---:|
| Wet | `1.50x` |
| Normal | `1.00x` |
| Dry | `0.35x` |
| Blocked | `0x` |

Untagged biomes count as Normal. Rain adds `+0.50x` after the humidity band when the structure has solar access. Nether and End dimensions are always blocked.

## Silica Gel Dehumidifier

| Surface | Current behavior |
|---|---|
| Controller | `rngtech:silica_gel_dehumidifier` |
| Column block | `rngtech:silica_gel_column_casing` |
| Beads | `rngtech:silica_gel_beads`, `rngtech:saturated_silica_gel_beads`, `rngtech:supercharged_silica_gel_beads`, `rngtech:saturated_supercharged_silica_gel_beads` |
| Block entity | `SilicaGelDehumidifierBlockEntity` |
| Menu / screen | `SilicaGelDehumidifierMenu` / `SilicaGelDehumidifierScreen` |
| Recipe type | `rngtech:desiccant_absorption` |
| RPG traits / refinement | No |
| FE | No input, output, or storage |
| Item automation | Controller top inserts dry beads into the three lane inputs; controller bottom extracts saturated beads |
| Fluid automation | Controller horizontal sides and valid casing horizontal sides drain the shared water tank; no fluid insertion |
| Casings | No item automation, bucket slots, or container slots |

The only valid structure is the controller with two casings directly above it. It has three independent dry-bead lanes and one shared `10,000 mB` water tank. An invalid structure pauses all lanes without deleting beads or stored water. The machine does not read sky access, biome humidity, rain, dimension, or ambient air; bead consumption is the balancing cost.

### Bead Balance

| Dry input | Water | Time | Full three-lane rate |
|---|---:|---:|---:|
| `rngtech:silica_gel_beads` | `945 mB` | `1,134 ticks` | `3 buckets/min` |
| `rngtech:supercharged_silica_gel_beads` | `3,150 mB` | `1,134 ticks` | `10 buckets/min` |

At full three-lane throughput, one stack of dry beads lasts about `20` minutes.

Saturated beads recharge through explicit `rngtech:furnace` recipes: normal beads in `1,200 ticks` for `28,800 FE`, supercharged beads in `2,400 ticks` for `57,600 FE`. Both recharge recipes set `bonus_output: false`, because absorbing and recharging returns the same beads and Super Output would duplicate them. Supercharged dry beads come from `rngtech:alloy_furnace` mode `desiccant`: `4x rngtech:silica_gel_beads`, `1x #c:dusts/titanium`, and `1x minecraft:redstone` at Stage 6, `1,450 C`, `6,000 FE`, and `240 ticks`.

## Related Pages

- [Algae Photobioreactor](algae-photobioreactor.md)
- [Bio Generator](bio-generator.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
