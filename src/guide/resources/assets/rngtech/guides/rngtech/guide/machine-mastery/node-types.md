---
navigation:
  title: "Node types"
  parent: machine-mastery.md
  position: 4
  icon: rngtech:ascendancy_seal_1
---

# Node types

- **Travel nodes** sit along the roads and grant attribute points.
- **Ordinary nodes** and **notables** branch off the roads into reward clusters. Notables are the stronger nodes at the end or center of a cluster, at least three points in from the road.
- **Jewel Sockets** hold one [jewel](rngtech:jewels.md) each. A socket grants nothing itself. Allocate it to open a jewel slot on the machine's Jewels tab. There are nine: one at the end of a short arm in each home region, and three on the outer roads.
- **Keystones** are big commitments, at least 10 points from every start. Most are trade-offs: **Steady State** gives 50% more Stability but 15% less Processing Speed, **Single Pass** turns off bonus output for 30% more Processing Speed, and **Dense Batching** adds 3 Batch Size but makes a machine wait for a full batch before it starts a cycle. The path leading to Dense Batching lowers Batch Overhead, and its notable **Even Loading** adds 1 Batch Size. A few only change how the machine works, and cost nothing but the points to reach them: **Silent Operation** mutes the machine, **Even Feed** makes automation fill a [Furnace](rngtech:furnace/automation.md)'s input lanes evenly, and two keystones on the same node 10 points from the Drive/Reserve start set how a heated machine runs its heat: **Blast Draft** lets the [overheat slider](rngtech:heat/the-overheat-slider.md) go past the safe maximum, and **Cool Headed** cools the chamber to the recipe target before every new input.

Every node is visible to every family. A node can have no effect on your machine: a Crusher can take a heat node and gain nothing from it. Any penalty on that node still applies, and the tooltip marks which effects are inactive. Some keystone payoffs only work on the machines that actually pay their cost; the tooltip lists which machines those are. For example, **Flash Annealing** caps Max Temperature, so its payoff works only on the [Furnace](rngtech:furnace/mastery-and-ascendancies.md), [Alloy Furnace](rngtech:alloy-furnace/mastery-and-ascendancies.md), and [Metal Press](rngtech:metal-press/mastery-and-ascendancies.md). **Low Heat Specialist**'s cap is below most alloy and press recipes, so its payoff works only on the Furnace. Use **Highlight relevant** in the **Menu** to highlight nodes that matter for your machine.

Stat terms like "increased" and "more" are explained in [Rarity and Affixes](rngtech:rarity-and-affixes.md), and each stat is defined in [Machine Stats](rngtech:machine-stats.md).
