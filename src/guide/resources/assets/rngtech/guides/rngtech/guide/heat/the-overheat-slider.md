---
navigation:
  title: "The overheat slider"
  parent: heat.md
  position: 1
  icon: rngtech:steel_heat_core
---

# The overheat slider

The slider under the progress bars on the Process tab sets the setpoint: how far past each recipe's target the lanes run. Fully left is the target, the default. On its own, fully right is the recipe's safe maximum. The **Blast Draft** keystone on the [Machine Mastery](rngtech:machine-mastery.md) tree lets it reach the machine's Max Temperature; a recipe that cannot fail still stops at its overheat temperature. The setpoint is written beside the slider in °C, and the hover shows the range for the current recipe. The slider is a position on a track from the recipe's target to the machine's Max Temperature, so one setting works for every recipe; the knob stops where the locked part begins, and taking Blast Draft later never moves your setpoint. All lanes of a Lead Furnace share it. The Stage 0 fuel Furnace has no slider and always holds the recipe target.

- Higher is faster and costs more FE per craft.
- Running hot is your risk. Every metal smelt can fail: when wobble spikes past the overheat temperature, failure strain builds, and a full strain bar turns the batch into **Malformed Ingot**s of that metal. Smelt them again to get two nuggets back. Strain carries over from one cycle to the next and only drains while the lane works inside its safe band, and it builds faster the hotter the lane runs, so short cycles cannot outrun it. A failed batch needs room in the output slot, so keep it emptied.
- Pushing past the target makes the temperature **wobble**. The heat bar shades the wobble band around the setpoint. Temperature Stability and Thermal Mass narrow it.
- Wobble that dips below the recipe's minimum pauses the work for that moment. On recipes that can fail, wobble that spikes past the overheat temperature builds failure strain.
- At 0% only a shortfall in Temperature Stability makes a lane wobble.

The heat bar runs from 0 to the machine's Max Temperature, so a temperature always sits at the same height. A strip on its inner edge, and the tint of its empty part, show the recipe's bands: grey below the minimum, green for the working band up to the safe maximum, amber up to the overheat temperature, and red past it. The fill turns red once the lane is overheating. A white line marks the recipe target, a yellow line the setpoint, and a pale band the wobble. Hover it for the setpoint, heat factor, wobble, and where the FE goes: into the input, warming the chamber, or lost through the walls.

The slider's track uses the same colors and runs from the recipe target to the machine's Max Temperature. The part you cannot reach yet, without **Blast Draft**, is dark and hatched. The setpoint is written beside it in °C.
