---
wiki:
  category: Power generation
  icon: rngtech:syngas_combustor
  ids:
    - rngtech:syngas_combustor
---

# Syngas Combustor

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "5",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:sparksteel_battery_cell', 'Battery Cell') }}, {{ item('rngtech:steel_servo', 'Servo') }}",
        "Mastery": "No",
    },
) }}

The **Syngas Combustor** is the Stage 5 gas-power generator. It burns Syngas from the [Coal Gasifier](coal-gasifier.md) for 240 FE/t before rolls, and turns the leftovers into Carbon Exhaust that you can feed to an [Algae Photobioreactor](algae-photobioreactor.md). It also burns Carbon Monoxide as a weak way to get rid of it.

Like every RNGTech machine, each Syngas Combustor you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Syngas Combustor with a pickaxe. It drops itself and keeps its rolled traits.

## Usage

### Generating power

Pipe Syngas or Carbon Monoxide into the gas tank. The combustor needs no Gear to run. Each cycle burns a batch of gas, generates FE over the cycle, and fills the Carbon Exhaust tank. Both tanks hold 8,000 mB.

| Fuel | Per cycle | Time | FE/t (base) | Exhaust |
|---|---:|---:|---:|---:|
| Syngas | 1,000 mB for 57,600 FE | 12 s | 240 | 500 mB |
| Carbon Monoxide | 500 mB for 4,000 FE | 8 s | 25 | 125 mB |

Carbon Monoxide is a by-product of the [Steam Methane Reformer](steam-methane-reformer.md). Burning it here is mainly a way to keep the reformer from backing up, not a power source.

The combustor stops when its FE storage is full or when the Carbon Exhaust tank cannot fit the next cycle's exhaust. Drain both to keep it running. It pushes FE into neighbors on its four sides, and does not limit its own output, so the receiver or connector tier decides how much FE moves each tick.

### Gear

The Gear tab has two optional slots:

- **Battery Cell**: adds FE storage. Only Stage 5 and higher cells fit.
- **Servo**: any Servo from {{ item('rngtech:steel_servo') }} up. It speeds up cycles. A {{ item('rngtech:nullite_servo') }} also voids Carbon Exhaust that would overflow a tank already holding Carbon Exhaust, so a full exhaust tank no longer stops the combustor. It runs at half speed, though.

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Output FE, for example into a [Universal Cable](universal-cable.md). |
| Any | Fills the gas tank with Syngas or Carbon Monoxide, and drains Carbon Exhaust. |

Gear slots are not reachable by automation.

### Gas combustion recipes

The Syngas Combustor uses its own recipe type, `rngtech:gas_combustion`.

{{ processing("gas_combustion") }}

## Screen

The Syngas Combustor screen has four tabs:

- **Process**: the gas and Carbon Exhaust tanks with their purge buttons, FE, and cycle progress.
- **Gear**: Battery Cell and Servo slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed combustor's traits with a catalyst.

A {{ item('rngtech:purge_bucket') }} used on the combustor voids up to 1,000 mB from one of its tanks.

## Data values

{{ data_values() }}

## See also

- [Coal Gasifier](coal-gasifier.md), which makes Syngas.
- [Steam Methane Reformer](steam-methane-reformer.md)
- [Algae Photobioreactor](algae-photobioreactor.md), which consumes Carbon Exhaust.
- [Stages](stages.md), for the Stage 5 power band.

{{ navbox() }}
