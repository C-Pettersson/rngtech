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

The **Steam Methane Reformer** reacts a gas with water to make **Hydrogen** and **Carbon Monoxide**. It is the Stage 6 Hydrogen source for the [Ammonia Synthesizer](ammonia-synthesizer.md). Methane is its main feed. Syngas from the [Coal Gasifier](coal-gasifier.md) also works, at a lower Hydrogen yield. Fed Carbon Monoxide instead, it runs the water-gas shift, which turns the Carbon Monoxide into more Hydrogen and a little Carbon Exhaust.

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
2. Fill the water tank, and fill the gas tank with Methane, Syngas, or Carbon Monoxide. Make Methane from algae biomass in the [Melter](melter.md).
3. Supply FE.

Each reform uses 1,000 mB of gas and 500 mB of water. Methane makes 1,500 mB of Hydrogen and Syngas only 500 mB, so treat Syngas as a stopgap until your Methane supply is running. Methane and Syngas both also make 500 mB of Carbon Monoxide. See the [recipe table](#gas-reforming-recipes) for time and FE costs.

All four tanks hold 8,000 mB. The Reformer stops when either output tank is full, so drain both.

You can read the Reformer from the outside. Facing the front:

- The window shows four reformer tubes. An installed Catalyst Bed shows as pellets packed inside them, and the tubes glow red-orange while the Reformer runs.
- The installed Heat Core sits in the burner bay under the firebox, in its material's colour, and blue burner flames light under the tubes while it runs.
- The three glasses on the right show the feed gas, Hydrogen, and second output levels. The glass on the steam drum shows the water level, and the gauge beside it rises while the Reformer runs.
- The Servo works the feed valve above the feed glass, and the Battery Cell sits at the front left with a charge window.
- Smoke rises from the flue stack at the back while it runs, and the lamp at the front lights.

The screen's status line tells you what is missing: Heat Core, catalyst, fluid, power, or output space. You can empty any tank with the purge buttons on the Process tab, or by right-clicking the Reformer with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Water-gas shift

The shift uses 500 mB of Carbon Monoxide and 500 mB of water, and makes 500 mB of Hydrogen and 125 mB of Carbon Exhaust. It needs the same Heat Core and Catalyst Bed as a reform.

The gas tank and the second output tank each hold one fluid at a time. The second output tank holds Carbon Monoxide after a reform and Carbon Exhaust after a shift, so a Reformer cannot start a shift while that tank still holds Carbon Monoxide; drain or purge it first. In practice, run the shift in its own Reformer and pipe the first Reformer's Carbon Monoxide into it.

Shifting the Carbon Monoxide from one Methane reform adds 500 mB of Hydrogen, about a third more. The cost is in the algae: the [Algae Photobioreactor](algae-photobioreactor.md) grows one Algae Biomass in 600 ticks on Carbon Monoxide but needs 1,200 ticks on Carbon Exhaust, so a shift loop needs about twice the Photobioreactors. See [the carbon loop](power-chains.md#carbon-loop).

### Where the products go

- **Hydrogen** feeds the [Ammonia Synthesizer](ammonia-synthesizer.md), which makes Ammonia for the [Fuel Cell](ammonia-fuel-cell.md). The Fuel Cell can also [burn Hydrogen directly](ammonia-fuel-cell.md#ammonia-or-hydrogen), for less FE.
- **Carbon Monoxide** is the best carbon input for the [Algae Photobioreactor](algae-photobioreactor.md). A second Reformer can [shift](#water-gas-shift) it into more Hydrogen, and the [Syngas Combustor](syngas-combustor.md) can burn it as a weaker fuel to get rid of it.
- **Carbon Exhaust** from a shift is a slower carbon input for the Algae Photobioreactor. The [Ammonia Synthesizer](ammonia-synthesizer.md) also takes it to make Urea.

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
| Any | Fill water and Methane, Syngas, or Carbon Monoxide, and drain Hydrogen and the second output, through any face. |

The Reformer has no item automation. Gear slots are manual only. The back cabinet has a port plate sized for a Universal Connector, the tidiest place to attach one.

### Gas reforming recipes

The Steam Methane Reformer uses the `rngtech:gas_reforming` recipe type. The Catalyst Bed in each recipe is the Gear slot requirement, and it is not used up.

{{ processing("gas_reforming") }}

## Screen

The Steam Methane Reformer screen has four tabs:

- **Process**: water, gas, Hydrogen, and second output tanks with purge buttons, progress, energy, and status. Hover for exact amounts.
- **Gear**: Heat Core, Battery Cell, Servo, and Catalyst Bed slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Reformer's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Coal Gasifier](coal-gasifier.md) and [Syngas Combustor](syngas-combustor.md), the rest of the gas chemistry chain.
- [Fuel Cell](ammonia-fuel-cell.md): how Hydrogen becomes power, as Ammonia or burned directly.
- [Stages](stages.md): where the Reformer fits in Stage 6.

{{ navbox() }}
