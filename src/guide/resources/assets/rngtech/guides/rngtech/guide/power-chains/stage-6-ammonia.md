---
navigation:
  title: "Stage 6: Ammonia"
  parent: power-chains.md
  position: 4
  icon: rngtech:syngas_combustor
---

# Stage 6: Ammonia

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:methane_bucket" scale="2" />
  Methane
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:steam_methane_reformer" scale="2" />
  Steam Methane Reformer
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:cavitation_generator" scale="2" />
  Cavitation Generator
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:syngas_combustor" scale="2" />
  Syngas Combustor
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:ammonia_synthesizer" scale="2" />
  Ammonia Synthesizer
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:ammonia_fuel_cell" scale="2" />
  Fuel Cell
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:algae_photobioreactor" scale="2" />
  Algae Photobioreactor
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| *you supply* | Methane · 1,000 mB · 55.6 mB/s | <ItemLink id="rngtech:steam_methane_reformer" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Hydrogen · 1,500 mB · 83.3 → 75 mB/s | <ItemLink id="rngtech:ammonia_synthesizer" /> |
| <ItemLink id="rngtech:cavitation_generator" /> | Nitrogen · 1,000 → 500 mB · 66.7 → 25 mB/s | <ItemLink id="rngtech:ammonia_synthesizer" /> |
| <ItemLink id="rngtech:ammonia_synthesizer" /> | Ammonia · 1,000 mB · 50 → 55.6 mB/s | <ItemLink id="rngtech:ammonia_fuel_cell" /> |
| <ItemLink id="rngtech:syngas_combustor" /> | Carbon Exhaust · 500 → 250 mB · 41.7 → 41.7 mB/s | <ItemLink id="rngtech:ammonia_synthesizer" /> |
| <ItemLink id="rngtech:ammonia_synthesizer" /> | Urea · 1 · 10/min | <ItemLink id="rngtech:algae_photobioreactor" /> |

**What to build.** A [Cavitation Generator](rngtech:cavitation-generator.md) fitted with the Nitrogen Extraction Rotor and Nitrogen Separation Nozzle, which splits water into Nitrogen. An [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md) that combines the Nitrogen with Hydrogen from the [carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md), and a [Fuel Cell](rngtech:ammonia-fuel-cell.md) that burns the Ammonia. The Fuel Cell makes more FE than the Synthesizer costs.

**Bottleneck.** Hydrogen. One Reformer keeps about one Synthesizer busy, and one Synthesizer about one Fuel Cell, while one Cavitation Generator makes Nitrogen for two or three Synthesizers. Every extra Fuel Cell therefore needs another Reformer and the Melter and Photobioreactors behind it. The Cavitation Generator is also the hungriest water user in the chain; see [Water supply](rngtech:power-chains/water-supply.md).

**Spare Nitrogen.** Nitrogen has a second use. With the **Blast Draft** keystone, Nitrogen piped into a Furnace, Alloy Furnace, or Metal Press lets it run hotter, and so faster, before it builds failure strain; see [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). A hot lane uses 1 mB a tick, a bit less than a Synthesizer, so every lane you run that way is Nitrogen your Ammonia chain does not get.

**Burning Hydrogen directly.** The [Fuel Cell](rngtech:ammonia-fuel-cell/ammonia-or-hydrogen.md) also burns Hydrogen, with no Nitrogen or Synthesizer, but gets less than half the FE per mB of Hydrogen that the Ammonia route does. Use it while you build the rest of the chain, or for Hydrogen you have to spare.

**Hydrogen for annealing.** A [Metal Press](rngtech:metal-press/annealing.md) with the Bright Anneal or Hydrogen Quench notable takes Hydrogen in its atmosphere tank and anneals what it presses, for 250 mB of Hydrogen per item. Annealed circuits calibrate with lucky stability, and Gear crafted only from Annealed plates rolls lucky Refinement Potential. Each Annealed item costs a sixth of the Hydrogen in one Ammonia cycle, so anneal only the circuits and plates that go into Gear.

**Urea.** An Ammonia Synthesizer with Carbon Exhaust piped in turns some of its Ammonia into [Urea](rngtech:ammonia-synthesizer.md). In the Photobioreactors of your [carbon loop](rngtech:power-chains/stage-6-the-carbon-loop.md), Urea makes Algae Biomass grow twice as fast. It also works as bone meal and feeds the Forestry Companion. The Synthesizer makes Urea before Ammonia whenever it can, and every Urea is Ammonia your Fuel Cells do not get, so feed it only as much Carbon Exhaust as you need Urea. The Carbon Exhaust can come from a Syngas Combustor, a Wooden Dehumidifier, or a Reformer that runs the shift.

**Next upgrade.** [Void Catalyst](rngtech:power-chains/stage-7-8-void-catalyst.md). In the Vacuum Collapse Generator, Ammonia is worth far more FE than in a Fuel Cell.
