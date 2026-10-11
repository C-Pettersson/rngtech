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

The **Syngas Combustor** is the Stage 5 gas-power generator. It burns Syngas from the [Coal Gasifier](coal-gasifier.md) for 240 FE/t before rolls, and turns the leftovers into Carbon Exhaust that you can feed to an [Algae Photobioreactor](algae-photobioreactor.md). It also burns Carbon Monoxide as a weak way to get rid of it. It is built like a small gas turbine: fuel gas feeds a combustion can, the hot gas turns a turbine, and the turbine's shaft drives an alternator at the back.

Like every RNGTech machine, each Syngas Combustor you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Syngas Combustor with a pickaxe. It drops itself and keeps its rolled traits.

## Usage

### Generating power

Pipe Syngas or Carbon Monoxide into the gas tank. The combustor needs no Gear to run. Each cycle burns a batch of gas, generates FE over the cycle, and fills the Carbon Exhaust tank. Both tanks hold 8,000 mB. Facing the front, the sight glass at the front right shows the fuel gas, and the one on the left side, beside the exhaust stack, shows the Carbon Exhaust.

While the combustor burns, the flame shows through the two view ports on the front: orange on Syngas, blue on Carbon Monoxide. Smoke rises from the stack, the status lamp lights, and the shaft between the turbine and the alternator spins up. The tachometer on the front shows the shaft speed, and the shaft coasts down for a few seconds after the flame goes out.

The times and amounts below are for the rated mixture.

| Fuel | Per cycle | Time | FE/t (base) | Exhaust |
|---|---:|---:|---:|---:|
| Syngas | 1,000 mB for 57,600 FE | 12 s | 240 | 500 mB |
| Carbon Monoxide | 500 mB for 4,000 FE | 8 s | 25 | 125 mB |

Carbon Monoxide is a by-product of the [Steam Methane Reformer](steam-methane-reformer.md). Burning it here is mainly a way to keep the reformer from backing up, not a power source.

The combustor stops when its FE storage is full or when the Carbon Exhaust tank cannot fit the next cycle's exhaust. Drain both to keep it running. It pushes FE into neighbors on its four sides, and does not limit its own output, so the receiver or connector tier decides how much FE moves each tick.

### Mixture

The mixture sets how much fuel the combustor feeds its flame. Set it from 70% to 140% by clicking or scrolling on the mixture gauge on the Process tab (hold Shift to step by 5). A rich mixture burns fuel faster than the air can burn it cleanly: more FE/t, but less FE from each mB of fuel and much more Carbon Exhaust. A lean mixture is the reverse.

| Mixture | Fuel use | FE/t | FE per mB | Carbon Exhaust per mB |
|---|---:|---:|---:|---:|
| 70% (lean) | ×0.70 | ×0.77 | ×1.09 | ×0.49 |
| 85% | ×0.85 | ×0.89 | ×1.04 | ×0.72 |
| 100% (rated, default) | ×1.00 | ×1.00 | ×1.00 | ×1.00 |
| 120% | ×1.20 | ×1.15 | ×0.96 | ×1.44 |
| 140% (rich) | ×1.40 | ×1.29 | ×0.92 | ×1.96 |

On Syngas, lean runs at 184 FE/t and gets 62,972 FE and 245 mB of Carbon Exhaust from each 1,000 mB; rich runs at 309 FE/t and gets 52,953 FE and 980 mB of Carbon Exhaust. Run lean when Syngas is scarce, and rich for peak power or to feed Carbon Exhaust to [Algae Photobioreactors](algae-photobioreactor.md). Exhaust is counted as the fuel burns, so switching mixture partway through a cycle charges each part at its own setting. Hover the gauge to see the exhaust one cycle leaves at the current setting.

You can see the mixture on the machine: a lean flame burns bluer and the stack gives off only thin wisps, while a rich flame burns yellow and the stack pours out heavy black smoke. The shaft and tachometer follow the power.

### Gear

The Gear tab has two optional slots:

- **Battery Cell**: adds FE storage. Only Stage 5 and higher cells fit.
- **Servo**: any Servo from {{ item('rngtech:steel_servo') }} up. It speeds up cycles. A {{ item('rngtech:nullite_servo') }} also voids Carbon Exhaust that would overflow a tank already holding Carbon Exhaust, so a full exhaust tank no longer stops the combustor. It runs at half speed, though.

Installed Gear shows on the machine. The Servo mounts on the right side of the turbine as the fuel governor, its arm trimming back and forth while the combustor burns; a faster Servo spins the shaft faster. The Battery Cell sits in a cradle on the left side next to the alternator, with a window that shows its charge.

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Output FE, for example into a [Universal Cable](universal-cable.md). |
| Any | Fills the gas tank with Syngas or Carbon Monoxide, and drains Carbon Exhaust. |

Gear slots are not reachable by automation. The back of the alternator has a port plate sized for a Universal Connector, the tidiest place to attach one.

### Gas combustion recipes

The Syngas Combustor uses its own recipe type, `rngtech:gas_combustion`.

{{ processing("gas_combustion") }}

## Screen

The Syngas Combustor screen has four tabs:

- **Process**: the gas and Carbon Exhaust tanks with their purge buttons, FE, cycle progress, and the mixture gauge. Hover the gauge for what the setting does.
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
