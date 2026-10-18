---
navigation:
  title: "Stage 6: the carbon loop"
  parent: power-chains.md
  position: 3
  icon: rngtech:syngas_combustor
---

# Stage 6: the carbon loop

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:algae_photobioreactor" scale="2" />
  Algae Photobioreactor
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:iron_crusher_chassis" scale="2" />
  Crusher
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:melter" scale="2" />
  Melter
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:steam_methane_reformer" scale="2" />
  Steam Methane Reformer
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:syngas_combustor" scale="2" />
  Syngas Combustor
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:hydrogen_bucket" scale="2" />
  Hydrogen
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:algae_photobioreactor" /> | Algae Biomass · 1 · 2 → 3.75/min | <ItemLink id="rngtech:melter" /> |
| <ItemLink id="rngtech:iron_crusher_chassis" /> | Organic Reagent · 1 · 10 → 3.75/min | <ItemLink id="rngtech:melter" /> |
| <ItemLink id="rngtech:melter" /> | Methane · 1,000 mB · 62.5 → 55.6 mB/s | <ItemLink id="rngtech:steam_methane_reformer" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Carbon Monoxide · 500 mB · 27.8 → 16.7 mB/s | ↺ back to <ItemLink id="rngtech:algae_photobioreactor" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Carbon Monoxide · 500 mB · 27.8 → 62.5 mB/s | <ItemLink id="rngtech:syngas_combustor" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Hydrogen · 1,500 mB · 83.3 mB/s | *leaves the chain* |

**What to build.** A [Melter](rngtech:melter.md) that melts Algae Biomass, Organic Reagent, and water into Methane, and a [Steam Methane Reformer](rngtech:steam-methane-reformer.md) that reacts the Methane with water into Hydrogen and Carbon Monoxide. Organic Reagent comes from crushing potatoes, carrots, or plant biomass in the [Crusher](rngtech:crusher.md). The Reformer also accepts Syngas instead of Methane, but makes much less Hydrogen from it, so use Syngas only until your Methane runs.

**Bottleneck.** The Reformer stops when its Carbon Monoxide tank is full. Send the Carbon Monoxide back to the [Algae Photobioreactor](rngtech:algae-photobioreactor.md), where it is the fastest carbon input. That closes the loop: each reform makes as much Carbon Monoxide as one Algae Biomass needs at rated aeration, and that biomass melts into the next batch of Methane. A Photobioreactor grows biomass more slowly than a Reformer uses Methane, so run two or more per Reformer, and keep them in light. The [Syngas Combustor](rngtech:syngas-combustor.md) can burn spare Carbon Monoxide, but it gets little FE from it. What the loop cannot make for itself is Organic Reagent, so keep a crop farm feeding the Crusher.

**Urea instead of more Photobioreactors.** When the [Ammonia](rngtech:power-chains/stage-6-ammonia.md) chain runs, put [Urea](rngtech:algae-photobioreactor.md#urea) from the Ammonia Synthesizer in the Photobioreactors. Each Urea makes the next 4 Algae Biomass grow twice as fast, so the same Photobioreactors keep up with more Reformers. The water and carbon gas per biomass do not change. One Urea costs 250 mB of Ammonia, a small part of the Ammonia that you get from those 4 biomass.

**Shifting for more Hydrogen.** A second Reformer can run the [water-gas shift](rngtech:steam-methane-reformer/water-gas-shift.md) on the Carbon Monoxide instead, which adds about a third more Hydrogen per reform. The shift turns the Carbon Monoxide into a little Carbon Exhaust, and algae grow half as fast on Carbon Exhaust, so a shift loop needs about twice the Photobioreactors. Shift when Hydrogen is what runs short and you have the room and light for more Photobioreactors.

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:melter" scale="2" />
  Melter
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:steam_methane_reformer" scale="2" />
  Steam Methane Reformer
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:steam_methane_reformer" scale="2" />
  Steam Methane Reformer
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:hydrogen_bucket" scale="2" />
  Hydrogen
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:algae_photobioreactor" scale="2" />
  Algae Photobioreactor
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:hydrogen_bucket" scale="2" />
  Hydrogen
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:melter" /> | Methane · 1,000 mB · 62.5 → 55.6 mB/s | <ItemLink id="rngtech:steam_methane_reformer" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Carbon Monoxide · 500 mB · 27.8 → 41.7 mB/s | <ItemLink id="rngtech:steam_methane_reformer" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Carbon Exhaust · 125 → 100 mB · 10.4 → 1.67 mB/s | <ItemLink id="rngtech:algae_photobioreactor" /> |
| <ItemLink id="rngtech:algae_photobioreactor" /> | Algae Biomass · 1 · 1 → 3.75/min | ↺ back to <ItemLink id="rngtech:melter" /> |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Hydrogen · 1,500 mB · 83.3 mB/s | *leaves the chain* |
| <ItemLink id="rngtech:steam_methane_reformer" /> | Hydrogen · 500 mB · 41.7 mB/s | *leaves the chain* |

**Next upgrade.** Send the Hydrogen on to [Ammonia](rngtech:power-chains/stage-6-ammonia.md).
