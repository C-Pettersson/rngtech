---
navigation:
  title: "Stage 7–8: Void Catalyst"
  parent: power-chains.md
  position: 5
  icon: rngtech:syngas_combustor
---

# Stage 7–8: Void Catalyst

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:ammonia_synthesizer" scale="2" />
  Ammonia Synthesizer
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="minecraft:ender_pearl" scale="2" />
  Ender Pearl
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:melter" scale="2" />
  Melter
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:titanium_alloy_furnace_chassis" scale="2" />
  Alloy Furnace
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:metal_press" scale="2" />
  Metal Press
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:battery_assembler" scale="2" />
  Component Assembler
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:vacuum_collapse_generator" scale="2" />
  Vacuum Collapse Generator
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:ammonia_synthesizer" /> | Ammonia · 1,000 mB · 50 → 83.3 mB/s | <ItemLink id="rngtech:melter" /> |
| *you supply* | Ender Pearl · 2 · 10/min | <ItemLink id="rngtech:melter" /> |
| <ItemLink id="rngtech:melter" /> | Ender Slurry · 1,000 mB · 83.3 → 66.7 mB/s | <ItemLink id="rngtech:battery_assembler" /> |
| <ItemLink id="rngtech:titanium_alloy_furnace_chassis" /> | Nullite Ingot · 2 → 1 · 2 → 4/min | <ItemLink id="rngtech:battery_assembler" /> |
| <ItemLink id="rngtech:metal_press" /> | Tungstensteel Plate · 1 · 6 → 4/min | <ItemLink id="rngtech:battery_assembler" /> |
| <ItemLink id="rngtech:battery_assembler" /> | Void Catalyst · 1 · 4 → 0.5/min | <ItemLink id="rngtech:vacuum_collapse_generator" /> |
| <ItemLink id="rngtech:vacuum_collapse_generator" /> | Collapse Residue · 1 · 0.5 → 3.33/min | ↺ back to <ItemLink id="rngtech:melter" /> |

**What to build.** A [Melter](rngtech:melter.md) that dissolves Ender Pearls in Ammonia into Ender Slurry, a [Component Assembler](rngtech:component-assembler.md) that seals the slurry around a Nullite Ingot in a Tungstensteel Plate, and the [Vacuum Collapse Generator](rngtech:vacuum-collapse-generator.md) that collapses each Void Catalyst into a large batch of FE. Nullite comes from the [Alloy Furnace](rngtech:alloy-furnace.md) and Tungstensteel Plates from the [Metal Press](rngtech:metal-press.md). The Ammonia comes from the Stage 6 [Ammonia](rngtech:power-chains/stage-6-ammonia.md) chain, so keep it running.

**Bottleneck.** Ender Pearls: every catalyst needs them twice, in its slurry and in its Nullite Ingot. With Max Temperature affixes, a Melter can use the generator's Collapse Residue in place of one pearl and make twice the slurry, so each catalyst needs fewer pearls and half the Ammonia. See [Void Catalyst](rngtech:vacuum-collapse-generator/void-catalyst.md) for the details. The generator's output also peaks at the end of each collapse, so your cables and storage must take the peak; see [the collapse curve](rngtech:vacuum-collapse-generator/the-collapse-curve.md).

**Next upgrade.** Better Gear in the generator. Nullite and Exotic Void Chambers, Collapse Nozzles, and Dimensional Stabilizers raise the FE from every catalyst.
