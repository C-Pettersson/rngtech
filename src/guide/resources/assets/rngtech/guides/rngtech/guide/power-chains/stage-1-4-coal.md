---
navigation:
  title: "Stage 1–4: coal"
  parent: power-chains.md
  position: 1
  icon: rngtech:syngas_combustor
---

# Stage 1–4: coal

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="minecraft:coal" scale="2" />
  Coal
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:furnace" scale="2" />
  Furnace
</Column>
</Row>

↓

<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:crude_solid_fuel_burner" scale="2" />
  Solid Fuel Burner
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| *you supply* | Coal | <ItemLink id="rngtech:crude_solid_fuel_burner" /> |
| <ItemLink id="rngtech:furnace" /> | Charcoal · 1 · 6/min | <ItemLink id="rngtech:crude_solid_fuel_burner" /> |

**What to build.** A [Solid Fuel Burner](rngtech:solid-fuel-burner.md) next to your first machines, fed with mined Coal or with Charcoal smelted from logs in the [Furnace](rngtech:furnace.md). See [Fuel](rngtech:solid-fuel-burner/fuel.md) for everything else it burns. Coal Dust from the [Crusher](rngtech:crusher.md) does not burn in the burner; save it for Bronze Blend and the Stage 5 Coal Gasifier.

**Bottleneck.** Rate, not fuel. A burner's FE/t is set by its Heat Core, and a Steel Heat Core is as fast as a burner gets. More coal only makes a burner last longer. At the same time, Stage 5 and later recipes cost several times the early FE, so each new machine needs more burners.

**Next upgrade.** [Syngas](rngtech:power-chains/stage-5-syngas.md). The same coal, crushed and gasified, runs a Syngas Combustor at more than twice a burner's best FE/t.
