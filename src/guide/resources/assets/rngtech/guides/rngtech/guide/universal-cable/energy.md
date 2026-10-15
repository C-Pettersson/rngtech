---
navigation:
  title: "Energy"
  parent: universal-cable.md
  position: 3
  icon: rngtech:cable
---

# Energy

The Energy tab has one Energy Connector slot. The connector pulls FE from machines that output it, carries it through the cable, and delivers it to Output connectors on the same channel. The installed connector's tier is the cap on how much FE that endpoint moves.

| Connector | Stage | Transfer |
|---|---:|---:|
| <ItemLink id="rngtech:crude_energy_connector" /> | 1 | 64 FE/t |
| <ItemLink id="rngtech:basic_energy_connector" /> | 1 | 128 FE/t |
| <ItemLink id="rngtech:copper_energy_connector" /> | 2 | 512 FE/t |
| <ItemLink id="rngtech:gold_energy_connector" /> | 3 | 2,048 FE/t |
| <ItemLink id="rngtech:sparksteel_energy_connector" /> | 5 | 8,192 FE/t |
| <ItemLink id="rngtech:arclite_energy_connector" /> | 6 | 32,768 FE/t |
| <ItemLink id="rngtech:exotic_energy_connector" /> | 8 | 1,000,000 FE/t |

The tier is a per-tick cap in both directions. A connector takes at most its tier in FE/t from its machine, and delivers at most its tier in FE/t into its machine, however many generators feed the network. Most machines do not limit how fast they take or give FE, so the connector tier is usually what sets the rate.

**Sharing the load.** Each tick, every energy channel moves its FE in one step, after all machines have run. Every Input or Both connector offers what its machine can give, every Output or Both connector asks for what its machine can take, and then the FE is shared out in this order:

1. Generators feed machines. When generators offer more than the machines need, each generator gives the same share of what it offers, so they share the load instead of one doing all the work.
2. Spare FE from generators charges storage blocks.
3. Storage blocks cover what the machines still need.
4. Storage blocks even out with each other (see below).

Each connector's Distribution setting decides how its own share is split between the receiving connectors. The result does not depend on the order you placed things in. Machines that push FE into a connector on their own deliver it right away, by the same rules.

**Storage balancing.** A storage block is any block that can both take and give FE at that moment, such as a [Battery Chassis](rngtech:battery-chassis.md). Between two storage blocks whose connectors are both on Both, FE flows only from the fuller block to the emptier one, by fill percentage, and stops once they are equally full. Gaps under 1% move nothing, so idle banks do not trade FE back and forth and lose it to charge and discharge losses. Daisy-chained banks still pass charge along: a generator charges the first bank, and the first bank shares with the next. Generators still charge banks, and banks still feed machines. A completely full bank passes its overflow on freely, and a completely empty bank takes FE freely. To fill one bank from another regardless of fill, set the source bank's connector to Input or the destination bank's connector to Output.

**No FE on this side.** You can put a connector on any face, but many machines take or give FE only on some sides. If the machine has no FE on the connector's Attach As side, the Energy tab and the Wrench hologram show a red warning: `No FE on this side of the target. Change Attach As or move the connector.`

**Machine readouts.** A machine's Jade tooltip and info panel count only connectors that actually move FE for it: the mode must match the direction, and the Attach As side must be one where the machine has FE. When those connectors carry less than the machine needs, it shows **Attached connector limits input energy**, or **Attached connector limits output energy** on a generator.
