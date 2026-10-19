---
navigation:
  title: "Generating power"
  parent: solar-panel.md
  position: 0
  icon: rngtech:crude_solar_panel
---

# Generating power

A panel generates FE only when all of these are true:

- It is daytime.
- The block above the panel can see the sky. Transparent blocks that keep sky visibility are fine.
- You are in the Overworld. Other dimensions are disabled by default in the common config.

Rain, snow, and thunderstorms drop a panel to its lower weather output. At night or under a roof it makes nothing. Anything above a panel blocks it, including another panel, even with a gap between them.

A standalone panel stores FE in a small internal buffer and pushes it out to adjacent blocks that accept FE, such as a [Universal Cable](rngtech:universal-cable.md) or a [Battery Chassis](rngtech:battery-chassis.md). A panel does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. Panels never accept FE from outside.

The cable port sits under the foot of the post. A cable directly below plugs in, so you can run cables one block down under a field or raise panels on a column of cable. A cable beside a panel connects too.

A standalone panel with a full buffer has nowhere to send its FE, so it stops and lies flat until something takes the FE.
