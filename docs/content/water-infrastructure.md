# Water Infrastructure

Status: Prototype

RNGTech currently has two atmospheric water sources: the low-tech Wooden Dehumidifier and the Stage 5 Silica Gel Dehumidifier. The Wooden Dehumidifier is registered as `rngtech:wooden_dehumidifier` with expandable `rngtech:wooden_dehumidifier_frame` blocks and a `rngtech:waterproofed_planks` crafting ingredient. The Silica Gel Dehumidifier is registered as `rngtech:silica_gel_dehumidifier` with a fixed two-block `rngtech:silica_gel_column_casing` column and consumable bead cycles.

## Wooden Dehumidifier

The Wooden Dehumidifier is a no-FE Stage 2-3 support machine. It passively condenses water from biome humidity and can convert Coal Dust or Gasification Residue plus stored water into Carbon Exhaust for the early algae chain.

| Surface | Current behavior |
|---|---|
| Controller | `rngtech:wooden_dehumidifier` |
| Frame block | `rngtech:wooden_dehumidifier_frame` |
| Block entity | `WoodenDehumidifierBlockEntity` |
| Menu / screen | `WoodenDehumidifierMenu` / `WoodenDehumidifierScreen` |
| Recipe type | `rngtech:wooden_dehumidifier_conversion` |
| RPG traits / refinement | No |
| FE | No input, output, or storage |

The controller scans a bounded cluster around itself and detects the full face-connected frame shell. A valid barrel needs at least `4` detected frames. All detected frames switch to their formed visual state, while tank capacity and passive water production use a capped effective frame count of `25`.

| Structure size | Water tank | Converted-output tank | Base water rate |
|---:|---:|---:|---:|
| `4` frames | `4,000 mB` | `4,000 mB` | `240 mB/min` before humidity |
| `25` frames | `25,000 mB` | `25,000 mB` | `1,500 mB/min` before humidity |

Water output requires solar access from the controller or at least one formed frame. If the entire formed structure is covered, passive water production is `0`. Water output is capped at `1,500 mB/min` after humidity and rain bonuses.

## Humidity

The machine uses RNGTech biome tags under `data/rngtech/tags/worldgen/biome/dehumidifier_humidity/`:

| Band | Multiplier |
|---|---:|
| Wet | `1.50x` |
| Normal | `1.00x` |
| Dry | `0.35x` |
| Blocked | `0x` |

Rain adds `+0.50x` after the humidity band when the structure has solar access, but the final rate still respects the `1,500 mB/min` cap. Nether and End dimensions are always blocked.

## Conversion

Default conversion is data-driven:

| Input | Water | Output | Time |
|---|---:|---:|---:|
| `#c:dusts/coal` | `1,000 mB minecraft:water` | `1,000 mB rngtech:carbon_exhaust` | `6,000 ticks` |
| `rngtech:gasification_residue` | `1,000 mB minecraft:water` | `1,000 mB rngtech:carbon_exhaust` | `6,000 ticks` |

Conversion progresses only while the structure is valid, the item matches a loaded recipe, enough water is stored, and the converted-output tank can accept the result. The batch consumes its item and water only at completion.

The Process tab has separate purge buttons for the water tank and converted-output tank. The craftable `rngtech:purge_bucket` can also right-click the placed controller to void up to `1000 mB`; normal use prefers stored water, while sneak-use prefers the converted output. Purging the water tank resets active conversion progress.

## Automation

- Top item automation inserts valid conversion ingredients and empty fluid containers.
- Bottom item automation extracts filled containers.
- Sides and bottom expose drain-only fluid automation for the selected output.
- Formed frame blocks proxy conversion-item and empty-container insertion plus drain-only selected-fluid output to their owning controller.
- The selected-output square cycles between Water and unique fluids produced by loaded Wooden Dehumidifier conversion recipes.
- Fluid insertion is not accepted from any side.
- Frame blocks expose no energy capability and unformed frames expose no item or fluid capability.

With JEI installed, Wooden Dehumidifier conversion recipes appear in the Wooden Dehumidifier Conversion category with the dehumidifier as catalyst.

## Silica Gel Dehumidifier

The Silica Gel Dehumidifier is a no-FE Stage 5 support machine that produces water by cycling dry desiccant beads into saturated beads. It does not read sky access, biome humidity, rain, dimension, or ambient air; bead consumption is the balancing cost.

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

The only valid structure is the controller with two `rngtech:silica_gel_column_casing` blocks directly above it. The machine has three independent dry-bead input lanes, three saturated-bead output lanes, and one shared `10,000 mB` water tank. Invalid structure pauses all lanes without deleting beads or stored water.

| Dry input | Saturated output | Water | Time | Full three-lane rate |
|---|---|---:|---:|---:|
| `rngtech:silica_gel_beads` | `rngtech:saturated_silica_gel_beads` | `945 mB minecraft:water` | `1,134 ticks` | `3 buckets/min` |
| `rngtech:supercharged_silica_gel_beads` | `rngtech:saturated_supercharged_silica_gel_beads` | `3,150 mB minecraft:water` | `1,134 ticks` | `10 buckets/min` |

Each lane progresses only while the structure is valid, the dry input matches a loaded `rngtech:desiccant_absorption` recipe, the matching saturated output slot can accept the result, and the shared tank can accept the recipe water output. The batch consumes one dry bead and produces one saturated bead only at completion. At full three-lane throughput, one stack of dry beads lasts about `20` minutes.

Saturated beads recharge through explicit `rngtech:furnace` recipes. Normal saturated beads recharge in `1,200 ticks` for `28,800 FE`; supercharged saturated beads recharge in `2,400 ticks` for `57,600 FE`. Both recharge recipes set `bonus_output: false`: absorbing and recharging returns the same beads, so Super Output would duplicate them. Supercharged dry beads are made through `rngtech:alloy_furnace` mode `desiccant` from `4x rngtech:silica_gel_beads`, `1x #c:dusts/titanium`, and `1x minecraft:redstone` at Stage 6, `1,450 C`, `6,000 FE`, and `240 ticks`.

The Process tab has a purge button for the shared water tank. The craftable `rngtech:purge_bucket` can also right-click the placed controller to void up to `1000 mB` of stored water.

## Silica Gel Automation

- Controller top item automation inserts dry beads into the three lane input slots.
- Controller bottom item automation extracts saturated beads from the three lane output slots.
- Controller horizontal sides expose drain-only fluid automation for the shared water tank.
- Valid column casing horizontal sides proxy drain-only water extraction to the owning controller.
- Casings expose no item automation, and the machine exposes no fluid insertion, energy capability, bucket slots, or container slots.

## Related Pages

- [Algae Photobioreactor](algae-photobioreactor.md)
- [Bio Generator](bio-generator.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
