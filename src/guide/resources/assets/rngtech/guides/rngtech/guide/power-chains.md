---
navigation:
  title: "Power Chains"
  parent: guides.md
  icon: rngtech:syngas_combustor
categories:
  - "Guides"
---

# Power Chains

Each generator page explains one machine. This page shows how they connect: which machine feeds which, what flows between them, and what usually runs short. RNGTech power comes in steps. Coal carries you to Stage 4, gas chemistry takes over at Stages 5 and 6, and Void Catalyst powers Stages 7 and 8. Each step is a chain of several machines, and the slowest link sets how much FE you get.

For each generator's output per stage, see the table under [Power basics](rngtech:getting-started/power-basics.md). For how to run a single machine, follow the links in each diagram.

## Side power

These generators support the main chains. None of them replaces the chain for your stage.

### Solar Panels

[Solar Panels](rngtech:solar-panel.md) need no fuel and no chain at all, only daylight and open sky. They suit remote or low-power machines, and a Solar Array Controller gathers a field of them into one output. They make little or nothing at night, so pair them with a [Battery Chassis](rngtech:battery-chassis.md).

### Bio Generator

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:wooden_composter" scale="2" />
  Wooden Composter
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:iron_crusher_chassis" scale="2" />
  Crusher
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:algae_photobioreactor" scale="2" />
  Algae Photobioreactor
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:bio_generator" scale="2" />
  Bio Generator
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:wooden_composter" /> | Composted Biomass | <ItemLink id="rngtech:bio_generator" /> |
| <ItemLink id="rngtech:iron_crusher_chassis" /> | Organic Reagent · 1 · 10/min | <ItemLink id="rngtech:bio_generator" /> |
| <ItemLink id="rngtech:algae_photobioreactor" /> | Algae Biomass · 1 · 1/min | <ItemLink id="rngtech:bio_generator" /> |

The [Bio Generator](rngtech:bio-generator.md) burns crops and prepared biomass. Prepared fuels from the [Wooden Composter](rngtech:wooden-composter.md) and [Algae Photobioreactor](rngtech:algae-photobioreactor.md) are worth much more per item than raw crops. See [Fuel chains](rngtech:bio-generator/fuel-chains.md) for which preparation pays off. At Stage 6 the same generators can export Methane for the [carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md) instead.

- [Corrosion Cell](rngtech:power-chains/corrosion-cell.md)

### Potential Reactor

The [Potential Reactor](rngtech:potential-reactor.md) recoups FE from retired Gear and recycling byproducts. It pays for what an item rolled, then hands the stripped item on to the [Component Recycler](rngtech:component-recycler.md) for its materials. It only runs while you have old items to burn, so treat it as a refund on Gear you replace, never as base power.

## More

- [Reading the diagrams](rngtech:power-chains/reading-the-diagrams.md)
- [Stage 1–4: coal](rngtech:power-chains/stage-1-4-coal.md)
- [Stage 5: Syngas](rngtech:power-chains/stage-5-syngas.md)
- [Stage 6: the carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md)
- [Stage 6: Ammonia](rngtech:power-chains/stage-6-ammonia.md)
- [Stage 7–8: Void Catalyst](rngtech:power-chains/stage-7-8-void-catalyst.md)
- [Water supply](rngtech:power-chains/water-supply.md)
