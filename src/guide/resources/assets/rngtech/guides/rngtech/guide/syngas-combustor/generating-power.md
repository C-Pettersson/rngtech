---
navigation:
  title: "Generating power"
  parent: syngas-combustor.md
  position: 0
  icon: rngtech:syngas_combustor
---

# Generating power

Pipe Syngas or Carbon Monoxide into the gas tank. The combustor needs no Gear to run. Each cycle burns a batch of gas, generates FE over the cycle, and fills the Carbon Exhaust tank. Both tanks hold 8,000 mB. Facing the front, the sight glass at the front right shows the fuel gas, and the one on the left side, beside the exhaust stack, shows the Carbon Exhaust.

While the combustor burns, the flame shows through the two view ports on the front: orange on Syngas, blue on Carbon Monoxide. Smoke rises from the stack, the status lamp lights, and the shaft between the turbine and the alternator spins up. The tachometer on the front shows the shaft speed, and the shaft coasts down for a few seconds after the flame goes out.

The times and amounts below are for the rated mixture.

| Fuel | Per cycle | Time | FE/t (base) | Exhaust |
|---|---:|---:|---:|---:|
| Syngas | 1,000 mB for 57,600 FE | 12 s | 240 | 500 mB |
| Carbon Monoxide | 500 mB for 4,000 FE | 8 s | 25 | 125 mB |

Carbon Monoxide is a by-product of the [Steam Methane Reformer](rngtech:steam-methane-reformer.md). Burning it here is mainly a way to keep the reformer from backing up, not a power source.

The combustor stops when its FE storage is full or when the Carbon Exhaust tank cannot fit the next cycle's exhaust. Drain both to keep it running. It pushes FE into neighbors on its four sides, and does not limit its own output, so the receiver or connector tier decides how much FE moves each tick.
