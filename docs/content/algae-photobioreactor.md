# Algae Photobioreactor

Status: Prototype

The Algae Photobioreactor is a Stage 3-4 support processor registered as `rngtech:algae_photobioreactor`. It does not produce FE. It turns water, carbon-bearing gas inputs, and light into Algae Biomass for direct Bio Generator fuel, methane chemistry, or Dense Algae Biomass crafting.

## Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:algae_photobioreactor` | `AlgaePhotobioreactorBlockEntity` | `AlgaePhotobioreactorMenu` / `AlgaePhotobioreactorScreen` | Side fluid input, side filled-container input, bottom item output | No |

The machine owns two input-only tanks:

| Tank | Capacity | Accepted fluid |
|---|---:|---|
| Water | `4,000 mB` | `minecraft:water` |
| Carbon | `4,000 mB` | `rngtech:carbon_exhaust`, `rngtech:syngas`, or `rngtech:carbon_monoxide` |

The Process tab has separate purge buttons for the Water and Carbon tanks. The craftable `rngtech:purge_bucket` can also right-click the placed machine to void up to `1000 mB` from the first non-empty input tank. Purging either input tank resets active algae growth progress.

## Recipes

Algae growth uses the `rngtech:algae_growth` recipe type. Recipes declare water input, carbon input, item output, processing ticks, and minimum light level.

The starter recipe consumes `250 mB` water and `100 mB` Carbon Exhaust over `1,200 ticks` with at least light level `12`, producing one `rngtech:algae_biomass`. Syngas is a faster intermediate carbon input at `80 mB` over `900 ticks`. Carbon Monoxide is the high-throughput bulk sink at `500 mB` over `600 ticks`.

JEI exposes algae growth recipes with both fluid inputs, the algae output, processing time, the light gate, and the Bio Chamber note for output scaling.

Players and side item automation can place `rngtech:algae_biomass` directly in the output slot to pre-stack or consolidate output. The slot still behaves as an output slot for extraction.

`rngtech:carbon_exhaust_bucket` has a bootstrap crafting recipe from one water bucket and one Coal Dust. Coal Dust is produced by crushing coal in the Crusher.

## Gear

The Algae Photobioreactor has one Gear slot:

| Slot | Accepted items | Runtime role |
|---|---|---|
| Bio Chamber | `rngtech:bio_chamber` | Adds the chamber's stored `FUEL_EFFICIENCY` contribution as Bio Conversion, which deterministically scales algae item output and banks fractional bonus progress between cycles. |

The Bio Chamber slot is manual equipment and is not exposed to sided item automation.

## Fuel Chain

`rngtech:algae_biomass` is direct Bio Generator fuel and methane chemistry feedstock, while `rngtech:dense_algae_biomass` is the more efficient Bio Generator algae fuel:

| Fuel | Tag | Base FE |
|---|---|---:|
| Algae Biomass | `rngtech:bio_generator/algae_biomass` | `400 FE` |
| Dense Algae Biomass | `rngtech:bio_generator/dense_algae_biomass` | `2,400 FE` |

Dense Algae Biomass is crafted from four Algae Biomass, making dense fuel more effective than burning the same algae directly. Plain Algae Biomass also feeds the Melter methane recipe with Organic Reagent and water. Neither algae item is tagged as vanilla furnace fuel or solid fuel-burning generator fuel.

## Automation

- Sides fill water, Carbon Exhaust, Syngas, or Carbon Monoxide through the block fluid capability.
- Sides insert filled water or supported carbon-gas containers, and may insert Algae Biomass into the output slot for pre-stacking.
- Bottom extracts algae output and empty container remainders.
- Fluid automation can fill water or supported carbon gases only. The machine never exposes stored input fluids for fluid extraction.
- The machine has no FE capability, no Battery Cell slot, no generator stats, and no Refinement tab.

## Related Pages

- [Bio Generator](bio-generator.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
