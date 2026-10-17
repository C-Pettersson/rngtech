---
navigation:
  title: "Stage 5: Syngas"
  parent: power-chains.md
  position: 2
  icon: rngtech:syngas_combustor
---

# Stage 5: Syngas

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:iron_crusher_chassis" scale="2" />
  Crusher
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:coal_gasifier" scale="2" />
  Coal Gasifier
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:syngas_combustor" scale="2" />
  Syngas Combustor
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:wooden_dehumidifier" scale="2" />
  Wooden Dehumidifier
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
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
| <ItemLink id="rngtech:iron_crusher_chassis" /> | Coal Dust · 1 · 20 → 5/min | <ItemLink id="rngtech:coal_gasifier" /> |
| <ItemLink id="rngtech:coal_gasifier" /> | Syngas · 1,000 mB · 83.3 mB/s | <ItemLink id="rngtech:syngas_combustor" /> |
| <ItemLink id="rngtech:syngas_combustor" /> | Carbon Exhaust · 500 → 100 mB · 41.7 → 1.67 mB/s | <ItemLink id="rngtech:algae_photobioreactor" /> |
| <ItemLink id="rngtech:coal_gasifier" /> | Gasification Residue · 1 · 5 → 0.2/min | <ItemLink id="rngtech:wooden_dehumidifier" /> |
| <ItemLink id="rngtech:wooden_dehumidifier" /> | Carbon Exhaust · 1,000 → 100 mB · 3.33 → 1.67 mB/s | <ItemLink id="rngtech:algae_photobioreactor" /> |
| <ItemLink id="rngtech:algae_photobioreactor" /> | Algae Biomass · 1 · 1/min | <ItemLink id="rngtech:bio_generator" /> |

**What to build.** A [Crusher](rngtech:crusher.md) grinding coal into Coal Dust, a [Coal Gasifier](rngtech:coal-gasifier.md) that turns the dust and water into Syngas, and a [Syngas Combustor](rngtech:syngas-combustor.md) that burns it. One Gasifier keeps one Combustor running at the rated mixture. The Gasifier also needs water; see [Water supply](rngtech:power-chains/water-supply.md).

**Bottleneck.** The by-products. The Combustor stops when its Carbon Exhaust tank is full, and the Gasifier stops when its residue slot is full. Pipe Carbon Exhaust to an [Algae Photobioreactor](rngtech:algae-photobioreactor.md), and put Gasification Residue in a [Wooden Dehumidifier](rngtech:wooden-dehumidifier.md), which turns it into more Carbon Exhaust. One Combustor makes far more exhaust than one Photobioreactor uses, so build several Photobioreactors or void the extra with the Combustor's purge button. A leaner mixture makes less exhaust per mB of Syngas. The algae can go straight into a [Bio Generator](rngtech:bio-generator.md), but its best use comes at Stage 6.

**Next upgrade.** The Stage 6 [carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md), which turns the algae into Hydrogen for [Ammonia](rngtech:power-chains/stage-6-ammonia.md).
