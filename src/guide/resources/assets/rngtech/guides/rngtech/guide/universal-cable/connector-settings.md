---
navigation:
  title: "Connector settings"
  parent: universal-cable.md
  position: 2
  icon: rngtech:cable
---

# Connector settings

| Setting | What it does |
|---|---|
| Channel | 0–15. Connectors only exchange with the same channel. Energy, Fluid, Item, and Bridge channels are separate. |
| Mode | Energy rows: In takes FE from the machine into the cable, Out sends FE from the cable into the machine, and Both does both. Hover a mode button for its description. Fluid and Item rows: In pulls from the machine into the cable, Out pushes from the cable into the machine. |
| Attach As | Which side of the machine the connector acts as. For example, a connector under a machine can insert as if it were on top. Set a Fluid or Item row to None to switch it off. |
| Distribution | Energy rows only. How this row splits the FE it gives each tick: Round Robin rotates between outputs, Even splits it evenly and then sends leftovers to outputs that can still take more, First Available fills outputs in order. |
| Filters | Fluid and Item rows each have two ghost filter slots. Filters are copies and are never used up. |

A new connector starts on channel 0. Every Energy row starts on Both with Distribution Round Robin, and every Fluid and Item row starts on In. Attach As starts as the machine face the connector actually touches. Each Energy, Fluid, and Item row has its own mode, channel, and Attach, and several rows can share a channel and side.
