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
        "Gear": "{{ item('rngtech:steel_cavitation_rotor', 'Cavitation Rotor') }}, {{ item('rngtech:steel_collapse_nozzle', 'Collapse Nozzle') }}, {{ item('rngtech:titanium_servo', 'Servo') }}, {{ item('rngtech:crude_energy_connector', 'Energy Connector') }}",
        "Mastery": "No",
    },
) }}

The **Cavitation Generator** is a Stage 5 generator that spins a rotor through Water to make FE. Every cycle heats the machine and wears down the installed rotor, so you manage two meters: heat strain and rotor wear. With the Stage 6 nitrogen Gear installed, it also splits Water into Nitrogen for the [Ammonia Synthesizer](ammonia-synthesizer.md).

Like every RNGTech machine, each Cavitation Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Cavitation Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Cavitation Rotor and a Collapse Nozzle in the Gear tab, then fill the input tank with Water. The tank holds 4,000 mB and takes fluid from pipes or from filled buckets and containers. Each cycle uses 1,000 mB of Water and generates FE over the cycle. The rotor's stage must meet the recipe's minimum; the starter Water recipe needs a Stage 5 rotor.

Two things build up as it runs:

- **Heat strain** rises after each cycle and cools off over time. The hotter the machine, the less FE each cycle makes, down to half at full strain. At full strain it waits to cool before starting another cycle.
- **Rotor wear** is stored on the rotor itself, so pulling it out and putting it back does not reset it. When wear reaches the rotor's durability, the rotor is removed and a {{ item('rngtech:pitted_cavitation_rotor') }} drops into the output slot. Keep that slot empty, or the generator stops. Pitted rotors can be salvaged in a Stage 5 [Component Recycler](component-recycler.md) for byproduct only.

The generator never pauses because its storage is full. FE that its 4,000 FE buffer and Battery Cell cannot hold is lost, so connect it to cables and storage that keep up. A redstone signal pauses it.

### Nitrogen

Install a {{ item('rngtech:nitrogen_extraction_rotor') }} and a {{ item('rngtech:nitrogen_separation_nozzle') }}, then run Water as usual. Each cycle adds Nitrogen to an 8,000 mB output tank and makes much less FE than the plain Water recipe. Recipes that need specific Gear take priority over the plain Water recipe. When the output tank is full, the generator stops with a Nitrogen Full status unless a Nullite Servo is installed and the tank already holds Nitrogen, in which case the excess is voided.

### Burst rotor

The {{ item('rngtech:aethergold_cavitation_rotor') }} is a Stage 7 sidegrade. It runs cycles very fast and raises FE sharply, but wears out in a short time. It does not raise the export rate without a connector, so pair it with a Sparksteel or better Energy Connector or most of the burst is vented.

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Cavitation Rotor | Yes | Steel, Titanium, Tungstensteel, Aethergold, and Nullite Cavitation Rotors, or the Nitrogen Extraction Rotor. Sets the stage, generation, cycle speed, and durability. |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles, or the Nitrogen Separation Nozzle. Tunes FE, heat strain, and Nitrogen output. |
| Heat Core | No | Makes heat strain cool faster. Higher-stage cores cool faster; an Iron Heat Core adds nothing. |
| Battery Cell | No | Adds FE storage. It does not raise the export rate. |
| Servo | No | Stage 6+ Servos only ({{ item('rngtech:titanium_servo') }} and up). Speeds cycles and reduces wear and heat strain. |
| Energy Connector | No | Sets how fast FE leaves the sides. Without one, the rotor and nozzle set a lower fallback rate. |

Cavitation Rotors can roll flat and percent Durability affixes.

### Automation

| Side | Behavior |
|---|---|
| Bottom | Extracts Pitted Cavitation Rotors. |
| Other sides | Insert filled fluid containers. |
| Any side | Fills the Water tank and drains Nitrogen through pipes. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation.

### Cavitation recipes

The Cavitation Generator uses its own recipe type, `rngtech:cavitation`. Pressure is shown for reference only; there is no pressure network.

{{ processing("cavitation") }}

## Screen

The Cavitation Generator screen has four tabs:

- **Process**: the input tank and Nitrogen tank with purge buttons, the container slot, the pitted-rotor slot, and meters for FE, cycle progress, heat strain, and rotor wear. Click the recipe line to open its JEI category.
- **Gear**: rotor, nozzle, Heat Core, Battery Cell, Servo, and Energy Connector slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

A {{ item('rngtech:purge_bucket') }} used on the generator voids up to 1,000 mB from the input tank, or from the Nitrogen tank while sneaking.

## Data values

{{ data_values() }}

## See also

- [Vacuum Collapse Generator](vacuum-collapse-generator.md), which shares the Collapse Nozzle.
- [Ammonia Fuel Cell](ammonia-fuel-cell.md), the end of the Nitrogen chain.
- [Gear](gear.md)

{{ navbox() }}
