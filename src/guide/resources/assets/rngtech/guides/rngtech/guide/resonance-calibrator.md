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

You can read a Calibrator from the outside. The installed Resonance Coil is the wound ring at the front, in the coil's metal; with no coil you see only the empty saddle. The input waits on the pin in the middle of the ring. While the Calibrator works, the input turns, the ring glows teal, and the lens behind it shines a beam onto the input. The ring flashes when a calibration finishes, and the component drops into the tray at the front. A multi-lane chassis has one smaller ring per lane, and each pin holds an input when the cycle has enough to fill that lane. The monitor on top shows the selected pattern behind glass on the left. The scope on the right draws a waveform that sweeps across as the cycle runs, steadier the higher the stability range. The bar beside it shows the range, and a white mark shows the last roll. With the Harmonist's Resonant Streak, gold pips under the scope fill as your streak builds. The Control Board stands in a bay on the right side of the cabinet as you face it, with LEDs that blink while it works. The Stabilizer Matrix floats between two prongs on the right of the front, and the Battery Cell stands on the left with its charge meter. The cable port is on the back. The Nullite chassis adds a fine vernier ring with alignment ticks around its coil.

## Usage

- [Calibrating](rngtech:resonance-calibrator/calibrating.md)
- [Annealed circuits](rngtech:resonance-calibrator/annealed-circuits.md)
- [Calibration families](rngtech:resonance-calibrator/calibration-families.md)
- [Stages](rngtech:resonance-calibrator/stages.md)
- [Gear](rngtech:resonance-calibrator/gear.md)
- [Automation](rngtech:resonance-calibrator/automation.md)
- [Mastery and Ascendancies](rngtech:resonance-calibrator/mastery-and-ascendancies.md)

## Obtaining

- [Crafting](rngtech:resonance-calibrator/crafting.md)

### Breaking

Mine a placed Resonance Calibrator with a pickaxe. It drops itself and keeps its rolled traits, Mastery progress, and ascendancy.

## More

- [Screen](rngtech:resonance-calibrator/screen.md)
