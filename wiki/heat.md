---
wiki:
  category: Guides
  icon: rngtech:steel_heat_core
---

# Heat

The [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md), and [Melter](melter.md) all heat their work the same way. Each lane has a live temperature. A heater pushes heat in, the walls leak it out, and the input soaks it up as it cooks. You pay FE, or fuel on the Stage 0 Furnace, for the heat the heater puts in, including the heat the walls lose.

## How a lane heats

- **Warming up.** A cold lane heats toward its setpoint. Heat Transfer sets how hard the heater pushes, and Thermal Mass sets how much heat the chamber has to take before its temperature moves. Unrolled gear climbs about 100 °C a second, so an Iron Furnace takes about 10 seconds to reach 800 °C; the fastest Heat Cores still take a few seconds.
- **Starting.** A cycle starts once the lane reaches the recipe's minimum temperature. Most recipes have no separate minimum, so they start at the target. A cold batch takes some heat from the chamber when it goes in. The lane never drops below the minimum because of it, and a bigger batch takes a bigger dip.
- **Working.** Below the target the input takes heat more slowly; above it, faster. The heat bar's hover shows this as the **heat factor**: ×1 at the target, ×1.3 at the default safe maximum. Thermal Conductivity and Processing Speed multiply it.
- **Short of power.** When FE or fuel cannot keep up, the input takes only the heat left after the walls, so crafting slows down and the status reads **Underpowered**. The lane keeps its temperature. Hot recipes need a lot of heat, so a weak supply crawls on them.
- **Output full.** A lane whose output has no room, for example after a failed batch, holds its setpoint and pays the wall loss, so it resumes at once when you clear the output.
- **Idle.** With nothing to work on, the heater turns off and the walls slowly leak the heat away. Heat Insulation and Thermal Mass keep it longer.
- **Too hot.** A lane hotter than its next recipe allows, after a hotter recipe or when you lower the slider, stops and cools first. The status reads **Too Hot**. Cooling Rate makes that wait shorter. With the **Cool Headed** keystone, every new input waits until the lane has cooled to the recipe target, so a cycle never starts on leftover or pushed heat.

Recipes with no temperature requirement, such as food, work at any heat at their normal speed, and a lane still hot from an earlier recipe pays for them with that leftover heat.

Heat loss grows quickly with temperature, so running hotter is faster but costs more FE per craft. Every craft pays at least its recipe's heat; JEI and the recipe tables list it in heat units (HU). Hotter recipes need more heat for the same input, as real metal does: a 1,450 °C smelt takes about 1.6 times the heat of an 800 °C one, and a 2,000 °C smelt about 2.1 times. On an electric machine each HU costs 1 FE before Energy Usage.

## The overheat slider

The slider under the progress bars on the Process tab sets the setpoint: how far past each recipe's target the lanes run. Fully left is the target, the default. On its own, fully right is the recipe's safe maximum. The **Blast Draft** keystone on the [Machine Mastery](machine-mastery.md) tree lets it reach the machine's Max Temperature; a recipe that cannot fail still stops at its overheat temperature. The setpoint is written beside the slider in °C, and the hover shows the range for the current recipe. The slider is a position on a track from the recipe's target to the machine's Max Temperature, so one setting works for every recipe; the knob stops where the locked part begins, and taking Blast Draft later never moves your setpoint. All lanes of a Lead Furnace share it. The Stage 0 fuel Furnace has no slider and always holds the recipe target.

- Higher is faster and costs more FE per craft.
- Running hot is your risk. Every metal smelt can fail: when wobble spikes past the overheat temperature, failure strain builds, and a full strain bar turns the batch into {{ item('rngtech:malformed_ingot') }}s of that metal. Smelt them again to get two nuggets back. Strain carries over from one cycle to the next and only drains while the lane works inside its safe band, and it builds faster the hotter the lane runs, so short cycles cannot outrun it. A failed batch needs room in the output slot, so keep it emptied.
- Pushing past the target makes the temperature **wobble**. The heat bar shades the wobble band around the setpoint. Temperature Stability and Thermal Mass narrow it.
- Wobble that dips below the recipe's minimum pauses the work for that moment. On recipes that can fail, wobble that spikes past the overheat temperature builds failure strain.
- At 0% only a shortfall in Temperature Stability makes a lane wobble.

The heat bar runs from 0 to the machine's Max Temperature, so a temperature always sits at the same height. A strip on its inner edge, and the tint of its empty part, show the recipe's bands: grey below the minimum, green for the working band up to the safe maximum, amber up to the overheat temperature, and red past it. The fill turns red once the lane is overheating. A white line marks the recipe target, a yellow line the setpoint, and a pale band the wobble. Hover it for the setpoint, heat factor, wobble, and where the FE goes: into the input, warming the chamber, or lost through the walls.

The slider's track uses the same colors and runs from the recipe target to the machine's Max Temperature. The part you cannot reach yet, without **Blast Draft**, is dark and hatched. The setpoint is written beside it in °C.

## Nitrogen atmosphere {#nitrogen-atmosphere}

The electric [Furnace](furnace.md), the [Alloy Furnace](alloy-furnace.md), and the [Metal Press](metal-press.md) each have a Nitrogen tank that holds 4,000 mB. Pipe in {{ item('rngtech:nitrogen_bucket') }} Nitrogen from any side, for example from a [Cavitation Generator](cavitation-generator.md) running the Nitrogen rotor and nozzle. The gauge sits at the right edge of the Process tab.

- **With Blast Draft**, Nitrogen in the tank gives 15% increased Overheat Tolerance, so the overheat temperature sits further above the safe maximum. For a 1,000 °C tin smelt with a 1,150 °C safe maximum, the line moves from 1,150 °C to 1,323 °C. That is more room before wobble spikes build failure strain, and a recipe that cannot fail lets the slider go further, so it crafts faster.
- **Cost.** A lane uses 1 mB of Nitrogen per tick, but only while it works hotter than its overheat temperature would be without Nitrogen. A lane at or below that line uses none, and a 4-lane Lead Furnace running hot uses up to 4 mB a tick.
- **Without Blast Draft** the Nitrogen does nothing and stays in the tank. A cable can pump the tank back out.
- When the tank runs dry, the overheat temperature drops back at once. A smelt that can fail keeps its setpoint and starts building strain, so keep the tank fed or lower the slider. A recipe that cannot fail stops and cools to its old limit first, the same as lowering the slider.

The gauge's rim lights up while a lane is using Nitrogen, and its hover shows the amount, whether the atmosphere is active, and the Nitrogen used per tick. Every mB here is Nitrogen your [Ammonia](power-chains.md#ammonia) chain does not get.

A Metal Press with Bright Anneal or Hydrogen Quench may hold Hydrogen in this tank instead, one gas at a time, to anneal what it presses; see [Annealing](metal-press.md#annealing).

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
