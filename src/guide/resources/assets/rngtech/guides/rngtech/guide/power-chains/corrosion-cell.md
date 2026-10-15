---
navigation:
  title: "Corrosion Cell"
  parent: power-chains.md
  position: 7
  icon: rngtech:syngas_combustor
---

# Corrosion Cell

<Column alignItems="center" fullWidth={true}>
<Row gap="16" alignItems="end">
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:metal_press" scale="2" />
  Metal Press
</Column>
<Column alignItems="center" gap="2">
  <ItemImage id="rngtech:melter" scale="2" />
  Melter
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
  <ItemImage id="rngtech:corrosion_cell" scale="2" />
  Corrosion Cell
</Column>
</Row>
</Column>

| From | Flow | To |
|---|---|---|
| <ItemLink id="rngtech:metal_press" /> | Iron Plate · 1 · 6 → 7.5/min | <ItemLink id="rngtech:corrosion_cell" /> |
| <ItemLink id="rngtech:metal_press" /> | Aluminum Plate · 1 → 2 · 6 → 12/min | <ItemLink id="rngtech:battery_assembler" /> |
| <ItemLink id="rngtech:melter" /> | Electrolyte Solution · 1,000 → 250 mB · 90.9 → 25 mB/s | <ItemLink id="rngtech:battery_assembler" /> |
| <ItemLink id="rngtech:battery_assembler" /> | Aluminum Anode · 1 · 6 → 3.75/min | <ItemLink id="rngtech:corrosion_cell" /> |
| <ItemLink id="rngtech:melter" /> | Electrolyte Solution · 1,000 mB · 90.9 mB/s | <ItemLink id="rngtech:corrosion_cell" /> |

The [Corrosion Cell](rngtech:corrosion-cell.md) dissolves metal in electrolyte. It burns plain plates from Stage 4, and from Stage 5 it burns Anodes assembled from two plates and Electrolyte Solution in the [Component Assembler](rngtech:component-assembler.md). Once you count the cost of assembling Anodes, it makes less than the main chain of the same stage, so use it to turn spare plates into power or as a backup.
