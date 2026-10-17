---
navigation:
  title: "Mixture"
  parent: syngas-combustor.md
  position: 1
  icon: rngtech:syngas_combustor
---

# Mixture

The mixture sets how much fuel the combustor feeds its flame. Set it from 70% to 140% by clicking or scrolling on the mixture gauge on the Process tab (hold Shift to step by 5). A rich mixture burns fuel faster than the air can burn it cleanly: more FE/t, but less FE from each mB of fuel and much more Carbon Exhaust. A lean mixture is the reverse.

| Mixture | Fuel use | FE/t | FE per mB | Carbon Exhaust per mB |
|---|---:|---:|---:|---:|
| 70% (lean) | ×0.70 | ×0.77 | ×1.09 | ×0.49 |
| 85% | ×0.85 | ×0.89 | ×1.04 | ×0.72 |
| 100% (rated, default) | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| 120% | ×1.20 | ×1.15 | ×0.96 | ×1.44 |
| 140% (rich) | ×1.40 | ×1.29 | ×0.92 | ×1.96 |

On Syngas, lean runs at 184 FE/t and gets 62,972 FE and 245 mB of Carbon Exhaust from each 1,000 mB; rich runs at 309 FE/t and gets 52,953 FE and 980 mB of Carbon Exhaust. Run lean when Syngas is scarce, and rich for peak power or to feed Carbon Exhaust to [Algae Photobioreactors](rngtech:algae-photobioreactor.md). Exhaust is counted as the fuel burns, so switching mixture partway through a cycle charges each part at its own setting. Hover the gauge to see the exhaust one cycle leaves at the current setting.

You can see the mixture on the machine: a lean flame burns bluer and the stack gives off only thin wisps, while a rich flame burns yellow and the stack pours out heavy black smoke. The shaft and tachometer follow the power.
