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

**Next upgrade.** Send the Hydrogen on to [Ammonia](rngtech:power-chains/stage-6-ammonia.md).
