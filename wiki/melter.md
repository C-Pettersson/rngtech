---
wiki:
  category: Processing
  icon: rngtech:melter
  ids:
    - rngtech:melter
---

# Melter

{{ infobox(
    fields={
        "Type": "Processing machine",
        "Stages": "6",
        "Power": "FE",
        "Gear": "{{ item('rngtech:titanium_heat_core', 'Heat Core') }}, {{ item('rngtech:titanium_crush_head', 'Crush Head') }}, {{ item('rngtech:osmium_fluid_pump', 'Fluid Pump') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }}",
        "Mastery": "Yes",
    },
) }}

The **Melter** is a Stage 6 machine that turns two items and water into a fluid. Its main job is making Lubricant, which later Servo and frame recipes need. It also makes Electrolyte Solution in bulk for the [Component Assembler](component-assembler.md), methane from algae for gas chemistry, and lava.

The Melter runs on FE and needs a Heat Core, a Crush Head, and a Fluid Pump installed. Like every RNGTech machine, each Melter you craft rolls its own [rarity and affixes](rarity-and-affixes.md), and it can level up through [Machine Mastery](machine-mastery.md).

## Obtaining

### Crafting

The Melter's recipe needs a calibrated conductive component of Stage 5 or higher with at least 70 stability. You make one by calibrating a Sparksteel Coil in the [Resonance Calibrator](resonance-calibrator.md).

{{ crafting() }}

### Breaking

Mine a placed Melter with a pickaxe. It drops itself and keeps its rolled traits and Mastery progress. Items inside it, including installed Gear, drop next to it.

## Usage

### Melting

Fill the input tank with water, put the two recipe items in the input slots, and supply FE. The two items can go in either slot. Each melt uses 1,000 mB of water and puts 1,000 mB of the product into the output tank.

Every default recipe needs at least 1,200 heat (methane needs 1,400) and hardness level 6, so you need a hot enough Heat Core and a Titanium or better Crush Head. Unlike the [Crusher](crusher.md), the Melter does not run recipes above its hardness at all.

Methane takes plain {{ item('rngtech:algae_biomass') }}. {{ item('rngtech:dense_algae_biomass') }} does not work in the Melter; burn it in a [Bio Generator](bio-generator.md) instead.

You can fill the input tank with water buckets or other fluid containers in the Process tab, or pipe water in from the sides. To take fluid out, fill an empty container in the output container slot, or pull from the sides once a Fluid Pump is installed. If you put the wrong fluid in, use the purge buttons on the Process tab, or right-click the Melter with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB (sneak to purge the output tank). Purging the input tank resets the current melt.

When the output tank is full, the Melter stops. A Nullite Servo instead voids the overflow so it keeps running.

### Gear

The Gear tab has five slots:

- **Heat Core** (required): sets the Melter's temperature. Iron through Titanium Heat Cores fit.
- **Crush Head** (required): sets the Melter's hardness level.
- **Fluid Pump** (required): lets the Melter output fluid, and sets how fast it fills containers and pushes fluid out of the sides. Rolled Fluid Capacity affixes on the pump enlarge both tanks.
- **Battery Cell** (optional): adds portable FE storage. Without a cell, the Melter keeps only a small working buffer and runs more slowly.
- **Servo** (optional): adds speed and control.

| Fluid Pump | Stage | Base transfer |
|---|---:|---:|
| {{ item('rngtech:osmium_fluid_pump') }} | 5 | 125 mB/t |
| {{ item('rngtech:titanium_fluid_pump') }} | 6 | 250 mB/t |
| {{ item('rngtech:tungstensteel_fluid_pump') }} | 7 | 500 mB/t |
| {{ item('rngtech:exotic_fluid_pump') }} | 8 | 1,000 mB/t |

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts recipe items. |
| Bottom | Extracts filled output containers. |
| Sides | Insert fluid containers and fill the input tank. With a Fluid Pump installed, sides also drain the output tank. |
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

Gear slots and the Refinement slot cannot be filled by automation.

### Mastery and Ascendancies

Each completed melt earns Melter Mastery XP, and you spend the points on the shared [Machine Mastery](machine-mastery.md) tree. The lava recipe earns none, and nothing is earned while the Melter is missing Gear, too cold, blocked by a full tank, or out of power. Melters choose between the **Pressure Vessel** and **Twin Crucible** [ascendancies](machine-mastery.md#ascendancies).

### Melter recipes

The Melter uses its own recipe type, `rngtech:melter`. The Lubricant recipe accepts several base and additive items; hover a slot to see them cycle.

{{ processing("melter") }}

## Screen

The Melter screen has five tabs:

- **Process**: item inputs, fluid container slots, the input and output tanks with purge buttons, progress, energy, and heat. Status squares show the recipe state and fluid transfer. Hover for exact values.
- **Gear**: Heat Core, Crush Head, Fluid Pump, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits, Gear, and Mastery.
- **Refinement**: refine the placed Melter's traits with a catalyst.
- **Mastery**: machine XP, level, and the passive tree.

## Data values

{{ data_values() }}

## See also

- [Component Assembler](component-assembler.md): uses Electrolyte Solution and Lubricant.
- [Steam Methane Reformer](steam-methane-reformer.md): uses methane.
- [Gear](gear.md)

{{ navbox() }}
