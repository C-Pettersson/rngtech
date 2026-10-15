---
wiki:
  category: Guides
  icon: rngtech:steel_heat_core
---

# Heat

The [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md), and [Melter](melter.md) all heat their work the same way. Each lane has a live temperature. A heater pushes heat in, the walls leak it out, and the input soaks it up as it cooks. You pay FE, or fuel on the Stage 0 Furnace, for the heat the heater puts in, including the heat the walls lose.

## How a lane heats

- **Warming up.** A cold lane heats toward its setpoint. Heat Transfer sets how hard the heater pushes, and Thermal Mass sets how much heat the chamber has to take before its temperature moves.
- **Starting.** A cycle starts once the lane reaches the recipe's minimum temperature. Most recipes have no separate minimum, so they start at the target. A cold batch takes some heat from the chamber when it goes in. The lane never drops below the minimum because of it, and a bigger batch takes a bigger dip.
- **Working.** Below the target the input takes heat more slowly; above it, faster. The heat bar's hover shows this as the **heat factor**: ×1 at the target, ×1.3 at the default safe maximum. Thermal Conductivity and Processing Speed multiply it.
- **Idle.** With nothing to work on, the heater turns off and the walls slowly leak the heat away. Heat Insulation and Thermal Mass keep it longer.
- **Too hot.** A lane hotter than its next recipe allows, after a hotter recipe or when you lower the slider, stops and cools first. The status reads **Too Hot**. Cooling Rate makes that wait shorter.

Heat loss grows quickly with temperature, so running hotter is faster but costs more FE per craft. Every craft pays at least its recipe's heat; JEI and the recipe tables list it in heat units (HU). On an electric machine each HU costs 1 FE before Energy Usage.

## The overheat slider

The slider under the progress bar on the Process tab sets how far past each recipe's target the lanes run, from 0% (the target, the default) to 100% (the machine's Max Temperature). One setting works for every recipe, because it is a share of each recipe's own range. All lanes of a Lead Furnace share it.

- Higher is faster and costs more FE per craft.
- Pushing past the target makes the temperature **wobble**. The heat bar shades the wobble band around the setpoint. Temperature Stability and Thermal Mass narrow it.
- Wobble that dips below the recipe's minimum pauses the work for that moment. On recipes that can fail, wobble that spikes past the overheat temperature builds failure strain.
- At 0% only a shortfall in Temperature Stability makes a lane wobble.

The heat bar marks the recipe target with a white line and the setpoint with a yellow one. Hover it for the setpoint, heat factor, wobble, and how much of the FE goes into heating and how much the walls lose.

## Heat stats

| Stat | Helps | Costs |
|---|---|---|
| Max Temperature | Unlocks hotter recipes and lets the slider go further. A heater far below its maximum heats faster. | — |
| Heat Transfer | Fast warmup and fast recovery after a load dip. | Higher FE/t while heating, so the Battery Cell and cables must keep up. |
| Thermal Conductivity | Faster crafting. | More heat leaks through the walls, so FE/t rises; FE per craft stays about the same. |
| Heat Insulation | Less heat lost, so cheaper crafts. | — |
| Thermal Mass | Smaller load dips, less wobble, holds heat while idle. | Slower first warmup and recovery. |
| Temperature Stability | Less wobble, so the slider can run hotter safely. Some recipes need a minimum. | — |
| Cooling Rate | Shorter **Too Hot** waits when switching to a cooler recipe or lowering the slider. | — |
| Overheat Tolerance | More room above the safe maximum before spikes build strain. | — |

Heat Cores lean different ways. The {{ item('rngtech:steel_heat_core') }} is heavy and well insulated: slower but cheap to run. The {{ item('rngtech:aluminum_heat_core') }} conducts fast but leaks and wobbles. Sparksteel and Titanium cores heat up quickly.

## See also

- [Machine Stats](machine-stats.md#heat) for every stat.
- [Machine Mastery](machine-mastery.md) for heat nodes and keystones.

{{ navbox() }}
