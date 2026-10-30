---
navigation:
  title: "When a machine underperforms"
  parent: getting-started.md
  position: 9
  icon: rngtech:wooden_crusher_chassis
---

# When a machine underperforms

- **Check its Gear first.** A missing Battery Cell, a Crush Head below the recipe's hardness, or a Heat Core that cannot reach the recipe's temperature explains most slow or stalled machines.
- **Check the recipe.** Recipe tables list minimum temperature, temperature stability, and hardness. Hover the status squares on the Process tab for what is missing.
- **Check power.** From Stage 4, a power drop in the middle of a job can cause a failure. Servos and Battery Cells help.
- **Check connectors.** When power or fluid does not move, check each connector's mode and channel. A red warning on the Energy tab means the machine has no FE on that connector's Attach As side.
- **Read it with a comparator.** A Redstone Comparator next to a machine outputs 15 when the machine is blocked (it wants to work but cannot, for example with no power, a full output, or a jammed jaw), 8 while it runs, 4 while paused by redstone, and 1 while idle with nothing to do. Use it to light a lamp or sound a note block when a line stalls. A machine with the [Telemetry](rngtech:rarity-and-affixes/glow-discreet-and-telemetry.md) affix sends a gauge instead, such as its FE buffer fill.
- **Check the roll.** Compare your Gear's affixes with what the recipe needs, and refine or recycle the part instead of rebuilding the machine.
