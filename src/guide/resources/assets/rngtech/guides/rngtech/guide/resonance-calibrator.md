---
navigation:
  title: "Resonance Calibrator"
  parent: processing.md
  icon: rngtech:iron_resonance_calibrator_chassis
categories:
  - "Processing"
item_ids:
  - rngtech:iron_resonance_calibrator_chassis
  - rngtech:copper_resonance_calibrator_chassis
  - rngtech:steel_resonance_calibrator_chassis
  - rngtech:titanium_resonance_calibrator_chassis
  - rngtech:lead_resonance_calibrator_chassis
  - rngtech:tungstensteel_resonance_calibrator_chassis
  - rngtech:nullite_resonance_calibrator_chassis
---

# Resonance Calibrator

<BlockImage id="rngtech:iron_resonance_calibrator_chassis" scale="6" />

<Row>
  <ItemImage id="rngtech:iron_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:copper_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:steel_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:lead_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:titanium_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:tungstensteel_resonance_calibrator_chassis" scale="2" />
  <ItemImage id="rngtech:nullite_resonance_calibrator_chassis" scale="2" />
</Row>

- **Type:** Processing machine
- **Stages:** 1, 2, 4, 6, 7
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:iron_resonance_coil" />, <ItemLink id="rngtech:iron_control_board" />, <ItemLink id="rngtech:iron_stabilizer_matrix" />, <ItemLink id="rngtech:iron_battery_cell" />, **Calibration Patterns**
- **Mastery:** Yes
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Resonance Calibrator** turns ordinary plates, gears, coils, casings, and circuits into calibrated components. Stage 3 and higher RNGTech machines need calibrated components in their crafting recipes, so the Calibrator is the gate between early and mid-game progression. Any machine, including ones from other mods, can make the raw parts, but only the Calibrator can calibrate them.

Each calibrated component records a family, a stage, a stability score from 0 to 100, and sometimes Refinement Potential. Recipes that use it can demand a minimum of each. Like every RNGTech machine, each Calibrator rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md) and levels up through [Machine Mastery](rngtech:machine-mastery.md).

## Usage

- [Calibrating](rngtech:resonance-calibrator/calibrating.md)

### Annealed circuits

An Annealed <ItemLink id="rngtech:basic_electric_circuit" />, pressed under Hydrogen in a [Metal Press](rngtech:metal-press/annealing.md) with Bright Anneal, calibrates with lucky stability. The Calibrator rolls stability twice within the recipe's range, after your Gear's adjustments, and keeps the higher roll. The Harmonist's Second Pass, if you have it, applies after that. On the plain Basic circuit recipe (35–75 stability), the chance of reaching 55 or more for an Advanced blank goes from about 51% to about 76%.

Annealed and plain circuits do not stack, so a multi-lane cycle is either all Annealed or all plain.

- [Calibration families](rngtech:resonance-calibrator/calibration-families.md)
- [Stages](rngtech:resonance-calibrator/stages.md)
- [Gear](rngtech:resonance-calibrator/gear.md)

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert into the input, catalyst, and stabilizer slots. Each slot accepts only items a calibration recipe uses there. |
| Bottom | Extracts finished components. |
| Any | Accepts FE, for example from a [Universal Cable](rngtech:universal-cable.md). |

Gear and pattern slots are not reachable by automation.

With JEI installed, recipes are listed under Resonance Calibration. Recipe transfer puts the pattern into the selected pattern slot if it is empty or already holds that pattern, otherwise into a slot holding it or the first empty one.

- [Mastery and Ascendancies](rngtech:resonance-calibrator/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:resonance-calibrator/crafting.md)

### Breaking

Mine a placed Resonance Calibrator with a pickaxe. It drops itself and keeps its rolled traits, Mastery progress, and ascendancy.

## More

- [Screen](rngtech:resonance-calibrator/screen.md)
