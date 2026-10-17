---
navigation:
  title: "Generating power"
  parent: vacuum-collapse-generator.md
  position: 0
  icon: rngtech:vacuum_collapse_generator
---

# Generating power

Install a Void Chamber, a Collapse Nozzle, and a Dimensional Stabilizer in the Gear tab, then put Void Catalyst in the input slot. Each catalyst starts one collapse, which generates its FE over the cycle and then drops any residue into the output slot.

A collapse starts only when:

- the Void Chamber's stage meets the recipe's minimum (Stage 7 for Void Catalyst),
- the machine's stability is at least the recipe's minimum (1.0 for Void Catalyst), and
- the residue slot has room.

During a collapse, a glowing core inside the chamber shrinks and darkens to black as the cycle runs, and the chamber's cap rims pulse faster toward the end. The stabilizer lenses glow and draw motes into the core.

Once a collapse starts it cannot pause for full storage. FE that the 20,000 FE internal buffer cannot hold is lost, so connect enough cables and storage to take each catalyst's output. The generator does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. A redstone signal pauses it, even in the middle of a collapse, which is the way to hold it while storage catches up.
