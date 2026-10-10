---
wiki:
  category: Guides
  icon: rngtech:fortress_heater_element
  ids:
    - rngtech:unique_potato_battery_cell
    - rngtech:fortress_heater_element
    - rngtech:igloo_basement_thermostat
    - rngtech:mineshaft_worn_pick_jaw
    - rngtech:crying_crucible
    - rngtech:trial_vault_escapement
    - rngtech:witch_bottle_reflux_pump
    - rngtech:ancient_echo_control_board
    - rngtech:bastion_coin_stack_capacitor
---

# Uniques

**Uniques** are [Gear](gear.md) and [Battery Cells](battery-cells.md) you find, never craft. Each has an orange-brown name, the unique colour from Path of Exile, a **signature** that no normal part of its type has, and a **drawback** you can see on the item. Some also carry an **ascendancy bonus** that works only on a machine that chose that ascendancy (see [Machine Mastery](machine-mastery.md#ascendancies)).

A Unique is a sidegrade, not a higher stage. It lets a machine do something different, and it costs you something for it.

Every copy of a Unique has the same stat lines, but each copy rolls its own values inside the ranges listed below. You can find a well-rolled copy or a poorly rolled one.

## Finding Uniques

Each Unique comes from one kind of place.

| Unique | Gear | Counts as | Found in |
|---|---|---:|---|
| {{ item('rngtech:unique_potato_battery_cell') }} | Battery Cell | Stage 0 | Village chests, 5% per chest |
| {{ item('rngtech:igloo_basement_thermostat') }} | Heat Core | Stage 2 | Igloo basement chests, 10% |
| {{ item('rngtech:mineshaft_worn_pick_jaw') }} | Crush Head | Stage 2 | Mineshaft minecart chests, 4% |
| {{ item('rngtech:crying_crucible') }} | Alloy Crucible | Stage 3 | Ruined portal chests, 4% |
| {{ item('rngtech:fortress_heater_element') }} | Heat Core | Stage 4 | Nether fortress chests, 8% |
| {{ item('rngtech:bastion_coin_stack_capacitor') }} | Battery Cell | Stage 4 | Bastion treasure chests, 10% |
| {{ item('rngtech:witch_bottle_reflux_pump') }} | Fluid Pump | Stage 5 | Witches killed by a player, 1%, plus 0.5% per Looting level |
| {{ item('rngtech:trial_vault_escapement') }} | Servo | Stage 6 | Ominous trial vaults, 3% per vault |
| {{ item('rngtech:ancient_echo_control_board') }} | Control Board | Stage 6 | Ancient city chests, 4% |

Your modpack may move Uniques to other sources, such as quest rewards or machine challenges.

## Identifying a Unique

A Unique usually drops **Unidentified**. Its tooltip shows the range of every stat line. To roll its values, put it **alone** in a crafting grid and take the result, the same way you [identify crafted items](rarity-and-affixes.md#identification). The values were fixed when it dropped, so identifying it again elsewhere does not change them.

The {{ item('rngtech:unique_potato_battery_cell') }} has no ranges, so it never needs identifying.

Once identified, the tooltip shows each rolled value. Hold **Shift** to see each line's range and its **roll quality**: where the roll sits in its range, from 0% for the worst roll to 100% for the best. For a drawback, the smallest penalty is the best roll. The tooltip also shows the Unique's overall roll quality, the average of its ranged lines.

## Using a Unique

- A Unique fits the same Gear slots as a normal part of its type, and follows the same stage rules using the stage it counts as. The {{ item('rngtech:mineshaft_worn_pick_jaw') }} counts as Stage 2, so a Copper Crusher accepts it, even though it crushes at hardness 3.
- A machine can hold a Unique in every slot that accepts one.
- Inside a machine's screen, a Unique's tooltip greys out any stat or behavior that machine does not use, marked "not used here".
- An ascendancy bonus does nothing until the machine has chosen that ascendancy; you don't need any of its nodes. The tooltip says which ascendancy it needs, and inside a machine that hasn't chosen it the line is greyed out. It adds to the same stat the ascendancy's own nodes give.
- Stats a Unique gives a machine, such as Blend Speed or Jam Chance per Cycle, appear on that machine's Stats tab while the Unique is installed. See [Machine Stats](machine-stats.md#unique-stats).
- Uniques keep their rolls when you break the machine they are in.

Uniques have no Refinement Potential and cannot be refined or rerolled: the [Affix Forge](affix-forge.md) and [Exotic Affix Forge](exotic-affix-forge.md) refuse them. The [Component Recycler](component-recycler.md) and [Potential Reactor](potential-reactor.md) will not take them either.

## The Uniques

Ranges are listed worst roll to best. Flat lines with whole-number ranges, such as Jam Recovery, roll whole numbers.

### Fortress Heater Element

A Heat Core with Sparksteel-class maximum temperature in a Stage 4 slot, so a Steel-stage [Furnace](furnace.md), [Alloy Furnace](alloy-furnace.md), [Metal Press](metal-press.md), or [Melter](melter.md) reaches the next stage's heat early. A Steel Furnace with it reaches 1600 heat, enough for Titanium recipes. Its other base stats, including Temperature Stability, are a Sparksteel Heat Core's. In a [Solid Fuel Burner](solid-fuel-burner.md) it burns fuels up to tier 4.

- **Signature:** (60–80)% less Warmup Time.
- **Crucible Keeper:** +(20–40) °C Overdrive Margin.
- **Drawbacks:** (60–40)% increased Energy Use, (60–40)% less Overheat Tolerance, which adds failure strain when heat overshoots a recipe's safe maximum, and 50% less Fuel Efficiency.

### Igloo Basement Thermostat

A cold-running Heat Core for presses and furnaces that punish unstable heat. Its base stats, including maximum temperature, are a Bronze Heat Core's, so it is no use for hot alloys.

- **Signature:** (40–80)% more Temperature Stability, (40–80)% more Overheat Tolerance, and Power Grace, which normal parts only have on Stage 6 and higher Servos.
- **Drop Forge:** +(5–15)% Heat Window.
- **Drawback:** (35–20)% less Heat Transfer, which slows heating and processing, including in the Melter.

### Mineshaft Worn Pick-Jaw

A Stage 2 Crush Head that crushes at hardness 3, one level above its stage. Its other base stats are a Copper Crush Head's. It also works in the [Melter](melter.md).

- **Signature:** hardness 3 and +(1–2) Batch Size, so even a Stage 2 Crusher crushes in batches.
- **Rockbreaker:** +(10–30)% Jam Recovery.
- **Drawbacks:** +(8–3)% Jam Chance per Cycle, on every craft and not only under-level ones, and (30–15)% increased Energy Use.

### Crying Crucible

A Bronze-class Alloy Crucible with the same three input slots, tuned for blend recipes.

- **Signature:** +(20–40)% Blend Speed and +(25–50) °C Blend Heat Reduction on blend recipes. These are the Blendwright's stats, and they stack with that ascendancy.
- **Drawbacks:** (40–20)% increased Energy Use, and (40–20)% less Stability, which raises failure strain on recipes that can fail.

### Trial Vault Escapement

A stop-start Servo for small batches. Its other base stats are a Titanium Servo's.

- **Signature:** Escapement. The first cycle after the machine idles, or after its recipe or mold changes, is faster by its Escapement Speed of +(50–100)%. FE per craft is unchanged. It works in the [Metal Press](metal-press.md), [Alloy Furnace](alloy-furnace.md), and [Melter](melter.md).
- **Die Keeper:** −(10–25) ticks Mold Swap Time.
- **Drawbacks:** (15–5)% less Processing Speed, and no Power Grace, which every other Stage 6 and higher Servo has.

### Witch-Bottle Reflux Pump

A Fluid Pump that holds rather than moves. Its other base stats are an Osmium Fluid Pump's, and it outputs fluid from the side like other pumps.

- **Signature:** (100–150)% increased Fluid Capacity for the machine it is in, such as the [Melter](melter.md) tanks and the Forestry Companion's water tank.
- **Field Hand:** Reflux. The Field Hand Sprinkler spends half as much water, on top of Irrigation.
- **Drawback:** (50–30)% less Fluid Transfer, which slows Melter container filling, side output, and cart refills.

### Ancient Echo Control Board

A Control Board with Nullite-class Calibration Precision in a Stage 6 slot, for the [Resonance Calibrator](resonance-calibrator.md).

- **Signature:** Echo Streak. The calibration streak survives one calibration of another family: that calibration builds on nothing, and the streak carries on when its family returns. It works once per streak.
- **Harmonist:** +(1–3) Streak Cap.
- **Drawbacks:** (25–10)% less Processing Speed, and no Refinement Potential Bonus, which every other Control Board gives.

### Bastion Coin-Stack Capacitor

A burst Battery Cell. It holds 40,000 FE, takes 512 FE/t, and gives a steady 128 FE/t, half an Invar cell's output.

- **Signature:** (75–125)% increased Burst Transfer and (75–125)% increased Burst Duration for the [Battery Chassis](battery-chassis.md) it is in. Only a chassis that can burst uses them.
- **Drawback:** (150–100)% increased Idle Loss.

### Voltaic Potato Battery Cell

A potato wired to hold far more than it should. It stores 10,000 FE with 128 FE/t input, 64 FE/t output, and 6%/min idle loss. It has no ranges to roll. See [Battery Cells](battery-cells.md#unique-cells).

## Data values

{{ data_values() }}

## See also

- [Rarity and Affixes](rarity-and-affixes.md)
- [Gear](gear.md)
- [Battery Cells](battery-cells.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
