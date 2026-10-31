---
navigation:
  title: "Fluids"
  parent: universal-cable.md
  position: 4
  icon: rngtech:cable
---

# Fluids

The Fluid tab has three rows, each with a Fluid Connector slot, its own mode, channel, and Attach setting, and two filter slots. An In row drains a shipment from the machine, waits, and repeats. An Out row fills its machine from shipments arriving on its channel, up to its tier's shipment size. To filter a row, put a filled bucket or other fluid container in a filter slot.

Each shipment can **jam**: the row pauses for a few seconds without losing any fluid. Several In rows on the same machine side and channel run in parallel, so stacking rows raises throughput.

An In row that finds nothing it can send, because its machine is empty, no Out row on its channel can take the shipment, or its filter matches nothing, waits before it looks again. It waits its tier's time between shipments, but never more than one second, and it looks again at once when cables, connectors, or their settings change. Item rows work the same way.

A connector with an In row also accepts fluid or items that its machine pushes out on its own, and sends them to that row's channel. Pushed shipments follow the same size, wait, jam, and filter rules.

**Splitting shipments.** Each fluid or item shipment is split evenly between every connector with an Out row on its channel that can take it, however far away each one is. If a machine is full or its filter rejects the shipment, its share goes to the others. When a shipment does not divide evenly, the extra goes to a different machine each time, so a Basic Item Connector feeding three Furnaces sends one item to each in turn.

| Connector | Stage | Shipment | Every | Jam chance, pause |
|---|---:|---:|---:|---:|
| <ItemLink id="rngtech:basic_fluid_connector" /> | 1 | 100 mB | 4 s | 12%, 8 s |
| <ItemLink id="rngtech:copper_fluid_connector" /> | 2 | 250 mB | 2 s | 8%, 6 s |
| <ItemLink id="rngtech:gold_fluid_connector" /> | 3 | 500 mB | 1 s | 5%, 4 s |
| <ItemLink id="rngtech:sparksteel_fluid_connector" /> | 5 | 1,000 mB | 0.5 s | 2%, 3 s |
| <ItemLink id="rngtech:arclite_fluid_connector" /> | 6 | 4,000 mB | 0.2 s | 1%, 2 s |
