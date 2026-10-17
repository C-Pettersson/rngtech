---
navigation:
  title: "Void Catalyst"
  parent: vacuum-collapse-generator.md
  position: 4
  icon: rngtech:vacuum_collapse_generator
---

# Void Catalyst

A Void Catalyst is Ender Slurry sealed around a Nullite core in a Tungstensteel casing. It takes two machines:

1. **Ender Slurry**: a [Melter](rngtech:melter.md) dissolves 2 Ender Pearls in 1,000 mB of [Ammonia](rngtech:ammonia-synthesizer.md) to make 1,000 mB of **Ender Slurry**. A Melter hot enough to reach 1,600 heat can melt 1 Ender Pearl with 1 **Collapse Residue** instead, which makes 2,000 mB from the same Ammonia. An unrolled Melter tops out at 1,200 heat, so this needs Max Temperature affixes on the Melter or its Heat Core.
2. **Assembly**: a [Component Assembler](rngtech:component-assembler.md) pours 1,000 mB of Ender Slurry over a Nullite Ingot (first slot) and a Tungstensteel Plate (second slot) to make one Void Catalyst.

Each collapse with a Dimensional Stabilizer installed leaves at least one Collapse Residue, and one Residue makes slurry for two catalysts, so a running generator with a hot Melter has Residue to spare and needs only half the Ammonia: 500 mB per catalyst instead of 1,000.

A Void Catalyst lasts one collapse of 2,400 ticks (two minutes) at rated depth, so a generator running nonstop uses about 30 an hour, and 15,000 to 30,000 mB of Ammonia an hour for their slurry, a sixth of one Ammonia Synthesizer's output at most. Burned in an [Ammonia Fuel Cell](rngtech:ammonia-fuel-cell.md) instead, 1,000 mB of Ammonia makes about 216,000 FE; in a Void Catalyst it helps make about 2.4M FE.

| Input | Output | Time | Heat | Min. temperature | Processing level | Mastery XP |
|---|---|---|---|---|---|---|
| <ItemLink id="minecraft:ender_pearl" /> *(Primary ingredient)*<br /><ItemLink id="minecraft:ender_pearl" /> *(Secondary ingredient)*<br />Ammonia (1,000 mB) *(Fluid input)* | Ender Slurry (1,000 mB) *(Fluid output)* | 12 s | 3,984 HU | 1,200 | 6 | 5 |
| <ItemLink id="minecraft:ender_pearl" /> *(Primary ingredient)*<br />**Collapse Residue** *(Secondary ingredient)*<br />Ammonia (1,000 mB) *(Fluid input)* | Ender Slurry (2,000 mB) *(Fluid output)* | 18 s | 10,029 HU | 1,600 | 6 | 8 |

| Input | Output | Time | Energy | Rolls traits |
|---|---|---|---|---|
| <ItemLink id="rngtech:nullite_ingot" /><br />**Tungstensteel Plate**<br />Ender Slurry (1,000 mB) *(Fluid input)* | **Void Catalyst** | 15 s | 7,200 FE | No |
