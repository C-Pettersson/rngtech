---
navigation:
  title: "Reading the diagrams"
  parent: power-chains.md
  position: 0
  icon: rngtech:syngas_combustor
---

# Reading the diagrams

Each chain shows its machines from the first step down to the last. The table under it lists every flow: the machine
that makes the resource, how much one run makes or uses, the rate at base speed, and the machine that takes it.
Click a linked name to open that machine's page.

- One number, such as **1,000 mB**, means both machines make and use the same amount.
- Two numbers, such as **500 → 100 mB**, mean the first machine makes 500 mB per run and the second uses 100 mB per
  run. Compare the rates to see how many of each machine you need.
- *You supply* marks something you bring yourself, such as mined coal. *Leaves the chain* marks a product that goes on
  to another chain.
- ↺ marks a flow that loops back to an earlier machine.

The rates are the recipes' base times, before machine stats, Gear, Mastery, and settings such as mixture, aeration,
or throttle.

Most gas machines also use water. To keep the chains readable, water appears only under [Water supply](rngtech:power-chains/water-supply.md).
