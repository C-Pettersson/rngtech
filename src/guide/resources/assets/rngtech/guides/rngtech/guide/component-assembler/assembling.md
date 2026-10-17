---
navigation:
  title: "Assembling"
  parent: component-assembler.md
  position: 0
  icon: rngtech:battery_assembler
---

# Assembling

Put the item ingredients in the four input slots on the Process tab and fill the assembly tank with the fluid the recipe needs. The machine then runs on FE and places the result in its four-slot output rack. Ingredients go in slot order: the recipe's first ingredient in the first slot, the second in the next, and so on. Leave unused input slots empty. Some recipes take more than one of an item from a single input slot, such as four Redstone for a Servo.

The tank holds 4,000 mB of one assembly fluid at a time:

- **Electrolyte Solution** for Primed Cell Cores, Anodes, and the Primed Seal Core.
- **Lubricant** for Servos, the Lubricated Seal Core, and the <ItemLink id="rngtech:lubricated_frame_coupling" />.
- **Ender Slurry** for Void Catalyst.

You can fill the tank three ways:

- Pipe the fluid in from any side.
- Put a filled bucket or other fluid container in the fluid slot.
- Put dry **Electrolyte** in the fluid slot. Each one dissolves into 125 mB of Electrolyte Solution, so you can make batteries before you reach the [Melter](rngtech:melter.md). Dry electrolyte cannot make Lubricant.

To switch fluids, press the purge button on the Process tab or right-click the machine with a **Purge Bucket**. Each purge voids up to 1,000 mB and resets the current job.
