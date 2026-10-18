---
navigation:
  title: "Automation"
  parent: furnace.md
  position: 4
  icon: rngtech:furnace
---

# Automation

| Side | Behavior |
|---|---|
| Top | Inserts into the input lane(s). Accepts only items with a Furnace recipe. |
| Bottom | Extracts finished output. |
| Sides | Fuel on Stage 0, Battery Cells on electric stages. |
| Any | Electric stages accept FE, for example from a [Universal Cable](rngtech:universal-cable.md), and Nitrogen for the [Nitrogen atmosphere](rngtech:heat/nitrogen-atmosphere.md). |

Automation fills the input lanes in order: lane 1 takes a full stack before lane 2 gets anything, so a slow feed into a Lead Furnace keeps one lane busy while the others sit idle. The **Even Feed** keystone on the [Machine Mastery](rngtech:machine-mastery.md) tree changes that. With it, each item that arrives through automation goes to the emptiest lane that can hold it, so a hopper trickle or a full stack from a cable spreads across every lane. It does not change which items the Furnace accepts, and a lane never holds two kinds of item. Items you place by hand still go where you put them. Even Feed sits right on a road, 15 points from the Furnace start, and has no downside beyond the points it costs.
