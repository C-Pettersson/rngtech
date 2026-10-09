---
wiki:
  category: Fluids and gases
  icon: rngtech:coal_gasifier
  ids:
    - rngtech:coal_gasifier
---

# Coal Gasifier

{{ infobox(
    fields={
        "Type": "Gas chemistry machine",
        "Stages": "5",
        "Power": "FE consumer",
        "Gear": "{{ item('rngtech:aluminum_heat_core', 'Heat Core') }}, {{ item('rngtech:sparksteel_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }}",
        "Mastery": "No",
    },
) }}

The **Coal Gasifier** turns Coal Dust and water into **Syngas**, the Stage 5 step up from burning coal directly. Burn the Syngas in a [Syngas Combustor](syngas-combustor.md) for power, or reform it in a [Steam Methane Reformer](steam-methane-reformer.md). Each run also leaves a {{ item('rngtech:gasification_residue') }} behind.

Like other RNGTech machines, each Coal Gasifier you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

The recipe uses an {{ item('rngtech:aluminum_heat_core') }} and a {{ item('rngtech:steel_servo') }} as ingredients. They are used up, so you need a second Heat Core for the Gear slot.

{{ crafting() }}

### Breaking

Mine a placed Coal Gasifier with a pickaxe. It drops itself with its rolled traits, plus any Coal Dust, residue, and Gear inside it. Fluid in its tanks is lost.

## Usage

### Gasifying coal

1. Install a Stage 5 or higher Heat Core in the Gear tab. The Gasifier will not run without one.
2. Put Coal Dust in the input slot. Grind coal in a [Crusher](crusher.md) to get it.
3. Fill the water tank, for example by pumping water in with a Fluid Connector on a [Universal Cable](universal-cable.md).
4. Supply FE.

Each run uses one Coal Dust, 500 mB of water, and 6,000 FE over 12 seconds, and makes 1,000 mB of Syngas plus one Gasification Residue. The water and Syngas tanks hold 8,000 mB each. The Gasifier stops when the Syngas tank or the residue slot is full, so pipe both away.

The screen's status line tells you what is missing: Heat Core, input, fluid, power, or output space. You can empty the water or Syngas tank with the purge buttons on the Process tab, or by right-clicking the Gasifier with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Where the products go

- **Syngas** fuels the [Syngas Combustor](syngas-combustor.md), RNGTech's Stage 5 gas generator. The [Steam Methane Reformer](steam-methane-reformer.md) can also reform it into Hydrogen, at a lower yield than Methane.
- **Gasification Residue** can be turned into Carbon Exhaust in a [Wooden Dehumidifier](wooden-dehumidifier.md), which feeds algae in the [Algae Photobioreactor](algae-photobioreactor.md).

### Gear

The Gear tab has three slots:

- **Heat Core** (required): Stage 5 or higher, such as {{ item('rngtech:aluminum_heat_core') }}, {{ item('rngtech:sparksteel_heat_core') }}, {{ item('rngtech:titanium_heat_core') }}, or {{ item('rngtech:exotic_heat_core') }}.
- **Battery Cell** (optional): Stage 5 or higher, such as a {{ item('rngtech:sparksteel_battery_cell') }}. It adds energy storage on top of the small internal buffer.
- **Servo** (optional): {{ item('rngtech:steel_servo') }} or better. A {{ item('rngtech:nullite_servo') }} also auto-purges: when the Syngas tank is full, it voids the extra gas instead of stopping the machine.

The stats and rolled affixes of installed Gear add to the Gasifier's own.

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts Coal Dust into the input slot. Accepts FE. |
| Bottom | Extracts Gasification Residue. Does not accept FE. |
| Sides | Accept FE. |
| Any | Fill water and drain Syngas through any face. |

Gear slots are manual only.

### Coal gasification recipes

The Coal Gasifier uses the `rngtech:coal_gasification` recipe type.

{{ processing("coal_gasification") }}

## Screen

The Coal Gasifier screen has four tabs:

- **Process**: the Coal Dust and residue slots, water and Syngas tanks with purge buttons, progress, energy, and status. Hover for exact amounts.
- **Gear**: Heat Core, Battery Cell, and Servo slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed Gasifier's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Syngas Combustor](syngas-combustor.md) and [Steam Methane Reformer](steam-methane-reformer.md), the rest of the gas chemistry chain.
- [Stages](stages.md): where Syngas fits in Stage 5 power.
- [Ammonia Fuel Cell](ammonia-fuel-cell.md): gas purge rules and the downstream Hydrogen chain.

{{ navbox() }}
