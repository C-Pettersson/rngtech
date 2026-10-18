---
navigation:
  title: "Gear"
  parent: cavitation-generator.md
  position: 3
  icon: rngtech:cavitation_generator
---

# Gear

| Slot | Required | Accepts |
|---|---|---|
| Cavitation Rotor | Yes | Steel, Titanium, Tungstensteel, Aethergold, and Nullite Cavitation Rotors, or the Nitrogen Extraction Rotor. Sets the stage, generation, cycle speed, and durability. |
| Collapse Nozzle | Yes | Steel through Exotic Collapse Nozzles, or the Nitrogen Separation Nozzle. Tunes FE, heat strain, and Nitrogen output. |
| Heat Core | No | Makes heat strain cool faster: half its stage per tick, so an Iron Heat Core adds nothing. Its Heat Transfer rolls speed that cooling up, Strain Recovery adds flat cooling, Overheat Tolerance raises the strain cap, Max Temperature lowers the FE lost at the cap, and Temperature Stability cuts the heat each cycle makes. |
| Battery Cell | No | Adds FE storage. |
| Servo | No | Stage 6+ Servos only (<ItemLink id="rngtech:titanium_servo" /> and up). Speeds cycles and reduces wear and heat strain. The Nullite Servo instead halves cycle speed, in exchange for voiding excess Nitrogen. |

Cavitation Rotors can roll flat and percent Durability affixes.

**Bonus Output** from the generator, rotor, or nozzle gets the same FE from less Water: each cycle draws 1,000 mB divided by Bonus Output, so +55% draws 646 mB. With Nitrogen Gear it adds Nitrogen instead. **Fluid Transfer** only matters with Nitrogen Gear, where it limits how fast Nitrogen drains out, so the Stats tab only shows it then.

On the Gear tab, the tooltip of an installed Heat Core or Servo dims the lines this generator does not use, such as Cooling Rate, Heat Isolation, Thermal Mass, and the Heat Core's own Energy Generation.
