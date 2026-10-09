# Algae Photobioreactor

Status: Prototype

Player guide: [Algae Photobioreactor](https://c-pettersson.github.io/rngtech/algae-photobioreactor/)

The Algae Photobioreactor (`rngtech:algae_photobioreactor`) is a Stage 3-4 no-FE support processor that turns water, a carbon-bearing gas, and light into Algae Biomass.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:algae_photobioreactor` | `AlgaePhotobioreactorBlockEntity` | `AlgaePhotobioreactorMenu` / `AlgaePhotobioreactorScreen` | Fluid input on every face except the bottom, top and side filled-container input, bottom item output | No |

| Tank | Capacity | Accepted fluid |
|---|---:|---|
| Water | `4,000 mB` | `minecraft:water` |
| Carbon | `4,000 mB` | `rngtech:carbon_exhaust`, `rngtech:syngas`, or `rngtech:carbon_monoxide` |

Both tanks are input-only; fluid automation never extracts stored inputs. Side item automation may insert `rngtech:algae_biomass` into the output slot for pre-stacking, and the slot still behaves as an output for extraction. The machine has no FE capability, Battery Cell slot, generator stats, or Refinement tab. Purging either input tank resets active growth progress.

## Recipes

Algae growth uses the `rngtech:algae_growth` recipe type. Recipes declare water input, carbon input, item output, processing ticks, and minimum light level. JEI shows both fluid inputs, the output, processing time, the light gate, and the Bio Chamber note for output scaling.

The starter recipe consumes `250 mB` water and `100 mB` Carbon Exhaust over `1,200 ticks` at light level `12` or more, producing one `rngtech:algae_biomass`. Syngas is a faster intermediate carbon input at `80 mB` over `900 ticks`. Carbon Monoxide is the high-throughput bulk sink at `500 mB` over `600 ticks`.

`rngtech:carbon_exhaust_bucket` has a bootstrap crafting recipe from one water bucket and one Coal Dust.

## Gear

| Slot | Accepted items | Runtime role |
|---|---|---|
| Bio Chamber | `rngtech:bio_chamber` | Adds the chamber's stored `FUEL_EFFICIENCY` contribution as Bio Conversion, which deterministically scales algae item output and banks fractional bonus progress between cycles. |

The Bio Chamber slot is manual equipment and is not exposed to sided item automation.

## Fuel Chain

| Fuel | Tag | Base FE |
|---|---|---:|
| Algae Biomass | `rngtech:bio_generator/algae_biomass` | `400 FE` |
| Dense Algae Biomass | `rngtech:bio_generator/dense_algae_biomass` | `2,400 FE` |

Dense Algae Biomass is crafted from four Algae Biomass, so dense fuel beats burning the same algae directly. Neither algae item is tagged as vanilla furnace fuel or solid fuel-burning generator fuel.

## Related Pages

- [Bio Generator](bio-generator.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
