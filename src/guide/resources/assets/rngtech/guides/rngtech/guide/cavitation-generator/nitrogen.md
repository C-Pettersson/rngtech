---
navigation:
  title: "Nitrogen"
  parent: cavitation-generator.md
  position: 2
  icon: rngtech:cavitation_generator
---

# Nitrogen

Install a <ItemLink id="rngtech:nitrogen_extraction_rotor" /> and a <ItemLink id="rngtech:nitrogen_separation_nozzle" />, then run Water as usual. Each cycle adds Nitrogen to an 8,000 mB output tank and makes no FE. Energy Generation still counts, at 25% of its value: the generator works out the extra FE your Energy Generation bonuses, flat FE/t included, would make over the same Gear unrolled, and adds a quarter of that share to the Nitrogen. +40% Energy Generation adds 10% Nitrogen per cycle, and a net penalty takes Nitrogen away the same way. Recipes that need specific Gear take priority over the plain Water recipe. When the output tank is full, the generator stops with a Nitrogen Full status unless a Nullite Servo is installed and the tank already holds Nitrogen, in which case the excess is voided. Nitrogen feeds the [Ammonia Synthesizer](rngtech:ammonia-synthesizer.md) and the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md) of heated machines. While it makes Nitrogen, the generation icon on the Process tab shows the Nitrogen rate per minute and the Energy Generation bonus, the Stats tab's Energy Generation row shows the Nitrogen it adds, and Jade shows the Nitrogen rate and progress, with the energy lines only in its detailed view.
