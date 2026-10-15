---
navigation:
  title: "Fuel tiers"
  parent: corrosion-cell.md
  position: 2
  icon: rngtech:corrosion_cell
---

# Fuel tiers

Plain plates burn on their own. From Stage 5, the main fuel is an **Anode**: two plates of one metal assembled with 250 mB of Electrolyte Solution and 4,800 FE in the [Component Assembler](rngtech:component-assembler.md). An Anode burns for twice as long as a plate and produces far more FE, but it only burns when the installed Cathode is at least the Anode's stage. A higher-tier plate still burns without a Cathode, at roughly 40% of its Anode's FE/t.

| Fuel | Stage | FE/t (base, rated load) | Cathode needed |
|---|---:|---:|---|
| **Scrap** | 4 | 30 | None |
| **Iron Plate** | 4 | 80 | None |
| **Copper Plate** | 4 | 100 | None |
| **Aluminum Anode** | 5 | 200 | <ItemLink id="rngtech:aluminum_cathode" /> or better |
| **Titanium Anode** | 6 | 480 | <ItemLink id="rngtech:titanium_cathode" /> or better |
| **Tungstensteel Anode** | 7 | 900 | <ItemLink id="rngtech:tungstensteel_cathode" /> or better |
| **Naquadah Anode** | 8 | 1,800 | <ItemLink id="rngtech:naquadah_cathode" /> |

Values are before machine and Cathode rolls. Every Anode tier stays below that stage's main generator once you count its assembly cost.
