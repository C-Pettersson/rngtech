---
navigation:
  title: "Circuits"
  parent: metal-press.md
  position: 2
  icon: rngtech:metal_press
---

# Circuits

Electric Circuits are a main reason to build a press. Every tier works the same way: craft four circuit blanks at a crafting table, then press each blank into a circuit with the **Circuit Mold**. Each tier's blank needs the circuit from the tier below, so you climb the chain in order:

1. **Insulators.** Craft <ItemLink id="rngtech:insulator" /> from glass, quartz, clay, and smooth stone. Every blank uses them.
2. **Basic.** A <ItemLink id="rngtech:basic_circuit_blank" /> needs Insulators, a Bronze Plate, a Copper Coil, redstone, and quartz. A Bronze Heat Core is enough to press it into a <ItemLink id="rngtech:basic_electric_circuit" />.
3. **Calibrate.** Run a Basic Electric Circuit through a [Resonance Calibrator](rngtech:resonance-calibrator.md) to get a **Calibrated Logic Component**. Its stability roll decides which higher blanks it can go into.
4. **Advanced.** An <ItemLink id="rngtech:advanced_circuit_blank" /> needs a Basic circuit, Gold Coils, a Steel Plate, Insulators, and a Calibrated Logic Component with **55** or more stability. A well-equipped Crude Metal Press can press it.
5. **Elite.** An <ItemLink id="rngtech:elite_circuit_blank" /> needs an Advanced circuit, Titanium Plates, a Titanium Control Board, a Titanium Resonance Coil, an Arclite Coil, Insulators, and a Calibrated Logic Component with **80** or more stability.
6. **Ultimate.** An <ItemLink id="rngtech:ultimate_circuit_blank" /> needs an Elite circuit, Tungstensteel Plates, Naquadah Coils, a Nullite Resonance Coil, Insulators, and a Stage 6 **Calibrated Diamond Crystal** with **85** or more stability.

Elite and Ultimate circuits expect the Steel press or a strongly upgraded Crude press. The Notes column below shows the stage and stability each calibrated ingredient needs. The Circuits table under [Metal Press recipes](rngtech:metal-press/metal-press-recipes.md) lists the heat, stability, and FE for pressing each blank.

<RecipesFor id="rngtech:insulator" />

<RecipesFor id="rngtech:basic_circuit_blank" />

<RecipesFor id="rngtech:advanced_circuit_blank" />

<RecipesFor id="rngtech:elite_circuit_blank" />

<RecipesFor id="rngtech:ultimate_circuit_blank" />

- Requires the Bronze material to be enabled.
- Requires the Copper material to be enabled.
- **Calibrated Logic Component** must be a Logic calibration, Stage 2+, stability 55+.
- Requires the Steel material to be enabled.
- **Calibrated Logic Component** must be a Logic calibration, Stage 2+, stability 80+.
- Requires the Titanium material to be enabled.
- Requires the Arclite material to be enabled.
- **Calibrated Diamond Crystal** must be a Logic calibration, Stage 6+, stability 85+.
- Requires the Nullite material to be enabled.
- Requires the Tungstensteel material to be enabled.
- Requires the Naquadah material to be enabled.

| Input | Output | Time | Energy | Min. stage |
|---|---|---|---|---|
| <ItemLink id="rngtech:basic_electric_circuit" /><br />**Logic Calibration Pattern** *(Pattern)*<br /><ItemLink id="minecraft:quartz" /> *(Catalyst)* | **Calibrated Logic Component** *(Stage 2, stability 35–75, refinement potential 0–2)* | 10 s | 1,600 FE | 1 |
| <ItemLink id="rngtech:basic_electric_circuit" /><br />**Logic Calibration Pattern** *(Pattern)*<br />**Matrix** *(Catalyst)*<br />**Stabilization Catalyst** *(Stabilizer)* | **Calibrated Logic Component** *(Stage 2, stability 65–95, refinement potential 1–3)* | 18 s | 5,600 FE | 4 |
