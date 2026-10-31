---
navigation:
  title: "Screen"
  parent: universal-cable.md
  position: 7
  icon: rngtech:cable
---

# Screen

A Universal Connector's screen has five tabs:

- **Energy**: three Energy Connector rows with mode, channel, Attach, and Distribution each. A red Attach button means the machine has no FE on that side. Hover a row to see its transfer cap, the FE it moved in and out last tick, and the warning.
- **Fluid**: three Fluid Connector rows with mode, channel, Attach, and two filter slots each.
- **Item**: three Item Connector rows with mode, channel, Attach, and two filter slots each.
- **Bridge**: the network connector slot, bridge channel, and link status.
- **Network**: a read-only overview of the whole cable network: cables, connectors, modules, and channels. The FE meter shows power actually delivered on the channel of the first Energy row that has a connector, as In and Out averaged over 1, 5, or 15 minutes, plus Max: the most the channel can move with its current connectors. It does not count demand that went unmet. Averages cover only the time measured so far, so a new network shows its real rate at once, and the history is kept when the network is rebuilt, extended, or merged. Hover In or Out for the live rate. **Clear network cache** forces a rebuild.
