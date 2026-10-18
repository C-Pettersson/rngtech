---
navigation:
  title: "Metal Press"
  parent: processing.md
  icon: rngtech:metal_press
categories:
  - "Processing"
item_ids:
  - rngtech:crude_metal_press
  - rngtech:metal_press
  - rngtech:insulator
  - rngtech:basic_circuit_blank
  - rngtech:advanced_circuit_blank
  - rngtech:elite_circuit_blank
  - rngtech:ultimate_circuit_blank
  - rngtech:basic_electric_circuit
  - rngtech:advanced_electric_circuit
  - rngtech:elite_electric_circuit
  - rngtech:ultimate_electric_circuit
---

# Metal Press

<BlockImage id="rngtech:metal_press" scale="6" />

<Row>
  <ItemImage id="rngtech:crude_metal_press" scale="2" />
  <ItemImage id="rngtech:metal_press" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 3 (Crude), 4 (Steel)
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:bronze_heat_core" />, <ItemLink id="rngtech:steel_servo" />, **Mold**, <ItemLink id="rngtech:iron_battery_cell" />
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Metal Press** forms ingots into plates, gears, and casings, presses circuit blanks into Electric Circuits, and presses coils, casings, and gears into Universal Cable connectors. The installed Mold decides what it makes. There are two presses: the **Crude Metal Press** is the early bootstrap press, and the Stage 4 **Metal Press** (the Steel press) is faster and more controlled but can fail.

Both presses run on FE and heat up before pressing. Like every RNGTech machine, each press you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md), and it can level up through [Machine Mastery](rngtech:machine-mastery.md).

You can read a press from the outside. The selected Mold sits on the bed as the die with the input lying on it, and the other stored Molds stand in the rack on the back cabinet. While the press works, the ram comes down as the cycle runs and strikes when it finishes, and pressed parts collect in the tray at the front of the bed. The bed and the die glow from dull red to yellow-white with the heat. The bar on the front of the bed reads like the [heat bar](rngtech:heat/the-overheat-slider.md) on the Process tab: it fills with the heat against the Heat Core's Max Temperature, a white mark shows the recipe's target and a yellow mark the setpoint, and the fill turns red while the press overheats. Facing the front, the Heat Core is the finned radiator on the right and the Battery Cell stands in the cradle on the left with its charge meter. The gas cylinder behind the cradle is the [atmosphere tank](rngtech:heat/nitrogen-atmosphere.md): its window shows how full it is, pale blue for Nitrogen and near white for Hydrogen, and its valve lights up while the press uses Nitrogen. The cable port is on the back.

The Steel press is a servo press: the Servo lies on the crown and drives the ram, and the meter next to the heat bar fills from amber to red with failure strain and blinks when a failure is near. The Crude press is a bronze fly press: copper coils heat its firebrick bed, and the screw and its fly bar turn as the ram comes down. A Servo installed in a Crude press sits behind the crosshead.

## Usage

- [Pressing](rngtech:metal-press/pressing.md)
- [Molds](rngtech:metal-press/molds.md)
- [Circuits](rngtech:metal-press/circuits.md)

### Stages

| Stage | Press | Notes |
|---:|---|---|
| 3 | <ItemLink id="rngtech:crude_metal_press" /> | Bootstrap press. Servo optional. Pauses instead of failing. |
| 4 | <ItemLink id="rngtech:metal_press" /> | Servo required. Uses failure strain and failure outputs. |

- [Gear](rngtech:metal-press/gear.md)
- [Automation](rngtech:metal-press/automation.md)
- [Mastery and Ascendancies](rngtech:metal-press/mastery-and-ascendancies.md)
- [Annealing](rngtech:metal-press/annealing.md)
- [Metal Press recipes](rngtech:metal-press/metal-press-recipes.md)

## Obtaining

- [Crafting](rngtech:metal-press/crafting.md)

### Breaking

Mine a placed press with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear and stored Molds, drop next to it.

## More

- [Screen](rngtech:metal-press/screen.md)
