---
navigation:
  title: "Water supply"
  parent: power-chains.md
  position: 6
  icon: rngtech:syngas_combustor
---

# Water supply

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:wooden_dehumidifier" scale="2" />
  Wooden Dehumidifier
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:silica_gel_dehumidifier" scale="2" />
  Silica Gel Dehumidifier
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:coal_gasifier" scale="2" />
  Coal Gasifier
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:cavitation_generator" scale="2" />
  Cavitation Generator
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:furnace" scale="2" />
  Furnace
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:wooden_dehumidifier" /> | Water · 500 mB · 41.7 mB/s | <ItemLink id="rngtech:coal_gasifier" /> |
| <ItemLink id="rngtech:silica_gel_dehumidifier" /> | Water · 945 → 1,000 mB · 16.7 → 83.3 mB/s | <ItemLink id="rngtech:cavitation_generator" /> |
| <ItemLink id="rngtech:silica_gel_dehumidifier" /> | Saturated Silica Gel Beads · 1 · 1.06 → 1/min | <ItemLink id="rngtech:furnace" /> |
| <ItemLink id="rngtech:furnace" /> | Silica Gel Beads · 1 · 1 → 1.06/min | ↺ back to <ItemLink id="rngtech:silica_gel_dehumidifier" /> |

Every gas chain runs on water: the Coal Gasifier, Algae Photobioreactor, Melter, Steam Methane Reformer, and Cavitation Generator all use it.

**What to build.** A [Wooden Dehumidifier](rngtech:wooden-dehumidifier.md) first. It needs no power and fills itself from the air while it can see the sky, faster in wet biomes and in rain. From Stage 5, a [Silica Gel Dehumidifier](rngtech:silica-gel-dehumidifier.md) gives steady water anywhere, including underground and in the Nether. Its saturated beads go back through a [Furnace](rngtech:furnace.md) to dry, so the beads loop between the two machines and only the Furnace's FE is spent.

**Bottleneck.** Water runs short sooner than you expect. Even a Wooden Dehumidifier in the rain makes less water than one Coal Gasifier uses, and the Melter and the Cavitation Generator each use more than the Gasifier. Build several Wooden Dehumidifiers early, and move the thirsty machines onto Silica Gel Dehumidifiers.

**Next upgrade.** Supercharged beads, made in the [Alloy Furnace](rngtech:alloy-furnace.md), hold more than three times as much water per bead.
