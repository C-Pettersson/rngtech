---
navigation:
  title: "Stages"
  parent: battery-chassis.md
  position: 1
  icon: rngtech:iron_battery_chassis
---

# Stages

| Stage | Chassis | Cell slots | Notes |
|---:|---|---:|---|
| 0 | <ItemLink id="rngtech:wooden_battery_chassis" /> | 1 | Novelty holder. Poor efficiency and stability, and it leaks. |
| 1 | <ItemLink id="rngtech:iron_battery_chassis" /> | 1 | Simple and reliable, with no leakage. Strong per-cell bonuses. |
| 2 | <ItemLink id="rngtech:copper_battery_chassis" /> | 2 | Better transfer rating with light leakage. |
| 3 | <ItemLink id="rngtech:gold_battery_chassis" /> | 3 | Large burst identity, but less stable and leakier. |
| 4 | <ItemLink id="rngtech:steel_battery_chassis" /> | 4 | Stable, no chassis leakage, and cuts each installed cell's own leakage by 0.02%/min. |
| 5 | <ItemLink id="rngtech:sparksteel_battery_chassis" /> | 5 | Charge Balancer. |
| 6 | <ItemLink id="rngtech:arclite_battery_chassis" /> | 6 | High transfer rating with Charge Balancer. |
| 7 | <ItemLink id="rngtech:nullite_battery_chassis" /> | 8 | Late-game bank with Charge Balancer. |
| 7 | <ItemLink id="rngtech:aethergold_battery_chassis" /> | 6 | Charge Balancer. Seals installed cells so they do not leak at all. Strongest burst identity. |
| 8 | <ItemLink id="rngtech:exotic_battery_chassis" /> | 10 | Charge Balancer. Largest, most stable bank, with weaker per-cell bonuses. |

More slots is not always better. Low-slot chassis apply their affixes more strongly to each cell, so a focused Iron or Copper chassis can still be worth keeping.

**Charge Balancer** spreads charging, discharging, and leakage evenly across all installed cells instead of filling or draining one at a time. Chassis without it can roll the **Balance Mode** prefix for the same effect. See [Balance Mode](rngtech:rarity-and-affixes.md#balance-mode). Balancing still respects each cell's own rates, stored FE, and free space: when one cell cannot take or give its share, the others cover it. It does not change capacity or transfer rate.

The Copper and Gold chassis, and every chassis from Sparksteel up, carry a burst identity. It does not raise output above what the cells can deliver.
