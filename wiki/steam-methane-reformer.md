---
wiki:
  category: Fluids and gases
  icon: rngtech:steam_methane_reformer
  ids:
    - rngtech:steam_methane_reformer
    - rngtech:reforming_catalyst_bed
---

# Steam Methane Reformer

{{ infobox(
    variants=["rngtech:steam_methane_reformer"],
    fields={
        "Type": "Gas chemistry machine",
        "Stages": "6",
        "Power": "FE consumer",
        "Gear": "{{ item('rngtech:titanium_heat_core', 'Heat Core') }}, {{ item('rngtech:reforming_catalyst_bed', 'Reforming Catalyst Bed') }}, {{ item('rngtech:arclite_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }}",
        "Mastery": "No",
    },
) }}

The **Steam Methane Reformer** reacts a gas with water to make **Hydrogen** and **Carbon Monoxide**. It is the Stage 6 Hydrogen source for the [Ammonia Synthesizer](ammonia-synthesizer.md). Methane is its main feed. Syngas from the [Coal Gasifier](coal-gasifier.md) also works, at a lower Hydrogen yield.

It is built as a fired tubular reformer: burners under a firebox heat a row of catalyst-filled tubes, and a steam drum on top feeds them water.

Like other RNGTech machines, each Steam Methane Reformer you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

The Reformer's recipe uses up one {{ item('rngtech:reforming_catalyst_bed') }}, so craft two: one for the recipe and one for the Gear slot.

{{ crafting() }}

### Breaking

Mine a placed Steam Methane Reformer with a pickaxe. It drops itself with its rolled traits, plus any installed Gear. Fluid in its tanks is lost.

## Usage

### Reforming gas

1. Install a Stage 6 or higher Heat Core and a Reforming Catalyst Bed in the Gear tab. The Reformer needs both to run.
2. Fill the water tank and the gas tank with Methane or Syngas. Make Methane from algae biomass in the [Melter](melter.md).
3. Supply FE.

Each run uses 1,000 mB of gas and 500 mB of water. Methane makes 1,500 mB of Hydrogen and Syngas only 500 mB, so treat Syngas as a stopgap until your Methane supply is running. Both feeds also make 500 mB of Carbon Monoxide. See the [recipe table](#gas-reforming-recipes) for time and FE costs.

All four tanks hold 8,000 mB. The Reformer stops when either output tank is full, so drain both.

You can read the Reformer from the outside. Facing the front:

- The window shows four reformer tubes. An installed Catalyst Bed shows as pellets packed inside them, and the tubes glow red-orange while the Reformer runs.
- The installed Heat Core sits in the burner bay under the firebox, in its material's colour, and blue burner flames light under the tubes while it runs.
- The three glasses on the right show the feed gas, Hydrogen, and Carbon Monoxide levels. The glass on the steam drum shows the water level, and the gauge beside it rises while the Reformer runs.
- The Servo works the feed valve above the feed glass, and the Battery Cell sits at the front left with a charge window.
- Smoke rises from the flue stack at the back while it runs, and the lamp at the front lights.

The screen's status line tells you what is missing: Heat Core, catalyst, fluid, power, or output space. You can empty any tank with the purge buttons on the Process tab, or by right-clicking the Reformer with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Where the products go

- **Hydrogen** feeds the [Ammonia Synthesizer](ammonia-synthesizer.md), which makes Ammonia for the [Ammonia Fuel Cell](ammonia-fuel-cell.md).
- **Carbon Monoxide** is the best carbon input for the [Algae Photobioreactor](algae-photobioreactor.md). The [Syngas Combustor](syngas-combustor.md) can also burn it as a weaker fuel to get rid of it.

### Gear

The Gear tab has four slots:

- **Heat Core** (required): Stage 6 or higher, so a {{ item('rngtech:titanium_heat_core') }} or an {{ item('rngtech:exotic_heat_core') }}.
- **Catalyst Bed** (required): a {{ item('rngtech:reforming_catalyst_bed') }}. Like other machine parts, it rolls its own affixes, and they apply to the Reformer.
- **Battery Cell** (optional): Stage 6 or higher, such as an {{ item('rngtech:arclite_battery_cell') }}. It adds energy storage on top of the small internal buffer.
- **Servo** (optional): {{ item('rngtech:steel_servo') }} or better. A {{ item('rngtech:nullite_servo') }} also auto-purges: when an output tank is full, it voids the extra gas instead of stopping the machine.

### Automation

| Side | Behavior |
|---|---|
| Top | Accepts FE. |
| Bottom | Does not accept FE. |
| Sides | Accept FE. |
| Any | Fill water and Methane or Syngas, and drain Hydrogen and Carbon Monoxide, through any face. |

The Reformer has no item automation. Gear slots are manual only. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

### Gas reforming recipes

The Steam Methane Reformer uses the `rngtech:gas_reforming` recipe type. The Catalyst Bed in each recipe is the Gear slot requirement, and it is not used up.

{{ processing("gas_reforming") }}

## Screen

The Steam Methane Reformer screen has four tabs:

- **Process**: water, gas, Hydrogen, and Carbon Monoxide tanks with purge buttons, progress, energy, and status. Hover for exact amounts.
- **Gear**: Heat Core, Battery Cell, Servo, and Catalyst Bed slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Reformer's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Coal Gasifier](coal-gasifier.md) and [Syngas Combustor](syngas-combustor.md), the rest of the gas chemistry chain.
- [Ammonia Fuel Cell](ammonia-fuel-cell.md): how Hydrogen becomes Ammonia power.
- [Stages](stages.md): where the Reformer fits in Stage 6.

{{ navbox() }}
