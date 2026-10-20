---
wiki:
  category: Power generation
  icon: rngtech:cavitation_generator
  ids:
    - rngtech:cavitation_generator
---

# Cavitation Generator

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "5",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:steel_cavitation_rotor', 'Cavitation Rotor') }}, {{ item('rngtech:steel_collapse_nozzle', 'Collapse Nozzle') }}, {{ item('rngtech:titanium_servo', 'Servo') }}",
        "Mastery": "No",
    },
) }}

The **Cavitation Generator** is a Stage 5 generator that spins a rotor through Water to make FE. You set the rotor's throttle: run it at rated speed, ease off to make the rotor last, or push it into supercavitation for more FE at the cost of wear and heat. With the Stage 6 nitrogen Gear installed, it also splits Water into Nitrogen for the [Ammonia Synthesizer](ammonia-synthesizer.md). See [Power Chains](power-chains.md#ammonia) for the ammonia chain, and [Water supply](power-chains.md#water) for keeping it fed.

The installed rotor spins behind the front porthole at its real speed, and the chamber glows blue while it cavitates. The side sight glasses show the Water and Nitrogen levels, and the rear fins glow orange as heat strain builds.

Like every RNGTech machine, each Cavitation Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Cavitation Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Cavitation Rotor and a Collapse Nozzle in the Gear tab, then fill the input tank with Water. The tank holds 4,000 mB and takes fluid from pipes or from filled buckets and containers. Each cycle uses 1,000 mB of Water and generates FE while it runs; how long a cycle lasts depends on the rotor's speed (see below). The rotor's stage must meet the recipe's minimum; the starter Water recipe needs a Stage 5 rotor.

### Rotor speed and throttle

The rotor has to spin up before it makes power. While a cycle runs, it speeds up toward the throttle setting, taking about 3.5 seconds to reach rated speed (2,100 RPM). Below 900 RPM the Water does not cavitate: the generator makes no FE and the cycle does not advance, so spinning up costs time but no Water. When the generator stops, whether for lack of Water, a redstone signal, or a missing part, the rotor coasts down slowly, and it has to spin up again from wherever it slowed to. A steady Water supply keeps it at speed. Faster Gear (a higher Processing Speed) also spins the rotor up faster.

Set the throttle on the rotor speed gauge in the Process tab. Click a point on the gauge to jump there, scroll over it to move 1%, or hold Shift while scrolling to move 5%. The throttle runs from 40% to 100% in steps of 1% (30 RPM), and new generators start at the rated 70%. The table shows every tenth step.

| Throttle | Rotor speed | FE/t | Rotor wear | Heat strain | Water |
|---|---|---|---|---|---|
| 40% | 1,200 RPM | x0.25 | x0.19 | x0.01 | x0.57 |
| 50% | 1,500 RPM | x0.50 | x0.36 | x0.07 | x0.71 |
| 60% | 1,800 RPM | x0.75 | x0.63 | x0.29 | x0.86 |
| 70% (rated) | 2,100 RPM | x1.00 | x1.00 | x1.00 | x1.00 |
| 80% | 2,400 RPM | x1.12 | x1.49 | x2.91 | x1.14 |
| 90% | 2,700 RPM | x1.23 | x2.13 | x7.47 | x1.29 |
| 100% | 3,000 RPM | x1.35 | x2.92 | x17.35 | x1.43 |

The rotor pushes Water through the chamber in proportion to its speed, so each 1,000 mB cycle finishes faster at a higher throttle and slower at a lower one. At rated speed a cycle takes 12 seconds, which is 5 buckets per minute. Rated speed gets the most FE out of each bucket; every other throttle gets a little less, and low throttles much less.

Below the rated speed, each rotor lasts longer and makes more FE over its life, but makes less FE each tick and less FE per bucket, while using less Water per minute. Above it, the generator supercavitates: more FE per tick, but the rotor wears out much faster and heat climbs steeply. A generator cools 1 heat strain per tick, plus half its Heat Core's stage (a Titanium Heat Core cools 4 per tick). When the selected throttle makes more heat than that, strain builds until the strain penalty eats the extra FE. Your Heat Core and Temperature Stability therefore set the highest throttle you can hold: without a Heat Core a Steel setup holds 80%, a Steel Heat Core lets it hold 90%, and full throttle needs an Exotic Heat Core or a Titanium one backed by extra Temperature Stability. Faster rotors and Servos run shorter cycles, so they heat up faster too.

Hovering over the gauge shows the current speed, how fast the rotor is wearing right now (including resonance), the FE and heat multipliers for the selected throttle, and its heat strain per tick against your cooling, in red when the generator cannot hold that throttle. The wear column in the table is the off-resonance rate.

### Rotor resonance

Every Cavitation Rotor has its own resonance speed, somewhere between 55% and 90% throttle. Run it at exactly that throttle and the rotor wears about a third as fast. FE and heat do not change, so you keep everything else the throttle gives you.

The resonance is not shown on the rotor. Find it by tuning:

- Watch **Rotor wear now** in the gauge tooltip while you scroll 1% at a time. It drops as you approach the resonance and turns aqua close to it.
- Each step closer helps. Three steps off still saves a quarter of the wear, one step off saves most of it, and only the exact throttle gives the full bonus.
- In the world, the chamber glow flickers hard off resonance and settles into a steady glow on it.

A rotor's resonance never changes, so once you have found it you can leave the generator running. The rotor rolls its resonance when crafted. A more precise [Cavitation Rotor Hub](#crafting-rotors) makes a higher resonance more likely, which puts the rotor's low-wear speed at more FE/t, though a resonance above 70% also brings supercavitation heat with it. Rotors from creative or loot roll a resonance when first installed.

Two things build up as it runs:

- **Heat strain** builds while the rotor cavitates and cools off over time. The hotter the machine, the less FE each cycle makes, losing up to half at the strain cap of 1,000. At the cap it waits to cool before starting another cycle. Hover the strain bar to see the FE lost now and at the cap, and your cooling per tick.
- **Rotor wear** is stored on the rotor itself, so pulling it out and putting it back does not reset it. At rated speed off resonance, a Steel rotor lasts about an hour, Titanium 71 minutes, Tungstensteel 86 minutes, and Nullite 100 minutes; tuned to resonance, each lasts about three times as long. The Aethergold burst rotor lasts minutes. When wear reaches the rotor's durability, the rotor is removed and a {{ item('rngtech:pitted_cavitation_rotor') }} drops into the output slot. Keep that slot empty, or the generator stops. Pitted rotors can be salvaged in a Stage 5 [Component Recycler](component-recycler.md) for byproduct only.

The generator never pauses because its storage is full. FE that its 4,000 FE buffer and Battery Cell cannot hold is lost, so connect it to cables and storage that keep up. It does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it.

### Nitrogen

Install a {{ item('rngtech:nitrogen_extraction_rotor') }} and a {{ item('rngtech:nitrogen_separation_nozzle') }}, then run Water as usual. Each cycle adds Nitrogen to an 8,000 mB output tank and makes no FE. Energy Generation still counts, at 25% of its value: the generator works out the extra FE your Energy Generation bonuses, flat FE/t included, would make over the same Gear unrolled, and adds a quarter of that share to the Nitrogen. +40% Energy Generation adds 10% Nitrogen per cycle, and a net penalty takes Nitrogen away the same way. Recipes that need specific Gear take priority over the plain Water recipe. When the output tank is full, the generator stops with a Nitrogen Full status unless a Nullite Servo is installed and the tank already holds Nitrogen, in which case the excess is voided. Nitrogen feeds the [Ammonia Synthesizer](ammonia-synthesizer.md) and the [Nitrogen atmosphere](heat.md#nitrogen-atmosphere) of heated machines. While it makes Nitrogen, the generation icon on the Process tab shows the Nitrogen rate per minute and the Energy Generation bonus, the Stats tab's Energy Generation row shows the Nitrogen it adds, and Jade shows the Nitrogen rate and progress, with the energy lines only in its detailed view.

### Burst rotor

The {{ item('rngtech:aethergold_cavitation_rotor') }} is a Stage 7 sidegrade. It runs cycles very fast and raises FE sharply, but wears out in a short time. Attach the generator through a Universal Connector with a Sparksteel or better Energy Connector, or most of the burst is vented.

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Cavitation Rotor | Yes | Steel, Titanium, Tungstensteel, Aethergold, and Nullite Cavitation Rotors, or the Nitrogen Extraction Rotor. Sets the stage, generation, cycle speed, and durability. |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles, or the Nitrogen Separation Nozzle. Tunes FE, heat strain, and Nitrogen output. |
| Heat Core | No | Makes heat strain cool faster: half its stage per tick, so an Iron Heat Core adds nothing. Its Heat Transfer rolls speed that cooling up, Strain Recovery adds flat cooling, Overheat Tolerance raises the strain cap, Max Temperature lowers the FE lost at the cap, and Temperature Stability cuts the heat each cycle makes. |
| Battery Cell | No | Adds FE storage. |
| Servo | No | Stage 6+ Servos only ({{ item('rngtech:titanium_servo') }} and up). Speeds cycles and reduces wear and heat strain. The Nullite Servo instead halves cycle speed, in exchange for voiding excess Nitrogen. |

Cavitation Rotors can roll flat and percent Durability affixes.

**Bonus Output** from the generator, rotor, or nozzle gets the same FE from less Water: each cycle draws 1,000 mB divided by Bonus Output, so +55% draws 646 mB. With Nitrogen Gear it adds Nitrogen instead, on top of the Energy Generation bonus. **Fluid Transfer** only matters with Nitrogen Gear, where it limits how fast Nitrogen drains out, so the Stats tab only shows it then.

On the Gear tab, the tooltip of an installed Heat Core or Servo dims the lines this generator does not use, such as Cooling Rate, Heat Isolation, Thermal Mass, and the Heat Core's own Energy Generation.

### Crafting rotors

Every Cavitation Rotor needs a {{ item('rngtech:cavitation_rotor_hub') }}: a Steel Casing calibrated in the [Resonance Calibrator](resonance-calibrator.md) with the Kinetic pattern. Better rotors need a better-calibrated hub:

| Rotor | Minimum hub stability |
|---|---|
| Steel | 20 |
| Titanium, Nitrogen Extraction | 35 |
| Tungstensteel, Aethergold | 50 |
| Nullite | 65 |

A hub's stability also skews the rotor's resonance upward, so a precise hub is worth keeping for the rotor you care about most. A hub that rolls too low for what you need recycles back into its Steel Casing in a Stage 5 [Component Recycler](component-recycler.md).

### Automation

| Side | Behavior |
|---|---|
| Bottom | Extracts Pitted Cavitation Rotors. |
| Other sides | Insert filled fluid containers. |
| Any side | Fills the Water tank and drains Nitrogen through pipes. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation.

### Cavitation recipes

The Cavitation Generator uses its own recipe type, `rngtech:cavitation`. Each recipe lists its pressure at rated speed. In the Process tab the pressure readout follows the rotor: 0 at rest, the recipe's pressure at rated speed, and higher in supercavitation. Pressure is a readout only; there is no pressure network.

{{ processing("cavitation") }}

## Screen

The Cavitation Generator screen has four tabs:

- **Process**: the input tank and Nitrogen tank with purge buttons, the container slot, the pitted-rotor slot, meters for FE, cycle progress, heat strain, and rotor wear, and the rotor speed gauge with its throttle pointer. Click the recipe line to open its JEI category.
- **Gear**: rotor, nozzle, Heat Core, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

A {{ item('rngtech:purge_bucket') }} used on the generator voids up to 1,000 mB from the input tank, or from the Nitrogen tank while sneaking. Purging the input tank also cancels the cycle in progress, and its Water is lost.

## Data values

{{ data_values() }}

## See also

- [Vacuum Collapse Generator](vacuum-collapse-generator.md), which shares the Collapse Nozzle.
- [Fuel Cell](ammonia-fuel-cell.md), the end of the Nitrogen chain.
- [Gear](gear.md)

{{ navbox() }}
