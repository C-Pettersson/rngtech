---
navigation:
  title: "Methane take-off"
  parent: bio-generator.md
  position: 4
  icon: rngtech:bio_generator
---

# Methane take-off

Instead of burning its gas, the Bio Generator can send it out as Methane for the [Steam Methane Reformer](rngtech:steam-methane-reformer.md). Install a Fluid Pump in one of the two **Gas Line** slots on the Gear tab. The engine stops, and the generator separates the gas in its holder. Biogas is 60% methane and 40% CO₂:

| Gas | Amount in the gas |
|---|---|
| Methane | 1 mB for each 20 FE of gas |
| CO₂ | 1 mB of Carbon Exhaust for each 30 FE of gas |

A Rich Biomass (3,200 FE) holds 160 mB of Methane and 107 mB of CO₂. Retention, Fuel Efficiency, and the Bio Chamber change the gas per item as they change FE. Energy Generation does not add gas in take-off mode: it only makes the digester work through each item faster, so a well-rolled generator makes Methane faster but needs just as much feed for it. Some Methane always escapes with the CO₂, and the Gas Line slots decide how much:

| Gas Line slots | Methane out | CO₂ |
|---|---|---|
| A Fluid Pump, or two | 70% | Vented from the stack |
| A Fluid Pump and a Biogas Membrane | 85–98%, set by the membrane | Captured as Carbon Exhaust |
| A Biogas Membrane alone | None; the engine runs as normal | Burned with the gas |

Methane and Carbon Exhaust collect in two 1,000 mB tanks, shown on the right of the Process tab. Pipes pull both from any face. When the Methane tank is full, the gas holder fills and the digester waits, so no fuel is lost. When the Carbon Exhaust tank is full, the extra CO₂ is vented. Rolled Fluid Capacity on a pump enlarges both tanks, and a second pump adds its own. To empty the tanks, sneak and use a **Purge Bucket** on the generator; it voids up to 1,000 mB, Methane first.

Through the Reformer and the [Ammonia](rngtech:power-chains/stage-6-ammonia.md) chain, the Methane is worth about ten times the FE the engine makes from the same gas. A [Melter](rngtech:melter.md) gets far more Methane from each item, so send Algae Biomass and Organic Reagent there, and give the take-off crops and compost from a large farm. Several old Bio Generators together feed one Reformer.

## Biogas Membrane

A <ItemLink id="rngtech:biogas_membrane" /> is assembled in the [Component Assembler](rngtech:component-assembler.md) from a Calibrated Storage Component, two Rubber, and a Titanium Plate. Its Methane recovery is fixed when it is made: 85%, plus up to 13% more from the calibration stability of the component, so a stability 80 component gives about 95%. The tooltip shows the recovery. Membranes do not roll affixes.

| Input | Output | Time | Energy | Rolls traits | Record calibration |
|---|---|---|---|---|---|
| **Calibrated Storage Component**<br />2× **Rubber**<br />**Titanium Plate** | <ItemLink id="rngtech:biogas_membrane" /> | 12 s | 6,000 FE | No | Yes |
