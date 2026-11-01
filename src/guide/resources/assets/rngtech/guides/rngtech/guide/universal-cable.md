---
navigation:
  title: "Universal Cable"
  parent: logistics.md
  icon: rngtech:cable
categories:
  - "Logistics"
item_ids:
  - rngtech:cable
  - rngtech:universal_connector
  - rngtech:crude_energy_connector
  - rngtech:basic_energy_connector
  - rngtech:copper_energy_connector
  - rngtech:gold_energy_connector
  - rngtech:sparksteel_energy_connector
  - rngtech:arclite_energy_connector
  - rngtech:exotic_energy_connector
  - rngtech:basic_fluid_connector
  - rngtech:copper_fluid_connector
  - rngtech:gold_fluid_connector
  - rngtech:sparksteel_fluid_connector
  - rngtech:arclite_fluid_connector
  - rngtech:crude_item_connector
  - rngtech:basic_item_connector
  - rngtech:copper_item_connector
  - rngtech:gold_item_connector
  - rngtech:sparksteel_item_connector
  - rngtech:arclite_item_connector
  - rngtech:advanced_item_filter
  - rngtech:ae2_network_connector
  - rngtech:refined_storage_network_connector
  - rngtech:wrench
  - rngtech:configurator
---

# Universal Cable

<BlockImage id="rngtech:cable" scale="6" />

<Row>
  <ItemImage id="rngtech:cable" scale="2" />
  <ItemImage id="rngtech:universal_connector" scale="2" />
</Row>

- **Type:** Cable network
- **Connector stages:** 1–8
- **Power:** None (carries FE)
- **Modules:** <ItemLink id="rngtech:basic_energy_connector" />, <ItemLink id="rngtech:basic_fluid_connector" />, <ItemLink id="rngtech:basic_item_connector" />, <ItemLink id="rngtech:ae2_network_connector" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

**Universal Cable** is RNGTech's one cable for everything. The same cable carries FE, fluids, and items between machines, and can extend an AE2 or Refined Storage network. The cable itself stores nothing. A **Universal Connector** on the cable next to each machine decides what moves, which way, and how fast, through the connector modules you install in it.

Cables, connectors, and modules do not roll rarity or affixes, and cannot be refined.

## Usage

- [Building a network](rngtech:universal-cable/building-a-network.md)
- [Dyed cables](rngtech:universal-cable/dyed-cables.md)
- [Connector settings](rngtech:universal-cable/connector-settings.md)
- [Energy](rngtech:universal-cable/energy.md)
- [Fluids](rngtech:universal-cable/fluids.md)

### Items

The Item tab works the same way as the Fluid tab, with three Item Connector rows. Put an item in a row's filter slot to allow only that item. See [Automation](rngtech:automation.md) for how machines sort the items you send them into their input slots.

| Connector | Stage | Shipment | Every | Jam chance, pause |
|---|---:|---:|---:|---:|
| <ItemLink id="rngtech:crude_item_connector" /> | 1 | 1 item | 12 s | 12%, 24 s |
| <ItemLink id="rngtech:basic_item_connector" /> | 1 | 1 item | 4 s | 12%, 8 s |
| <ItemLink id="rngtech:copper_item_connector" /> | 2 | 4 items | 2 s | 8%, 6 s |
| <ItemLink id="rngtech:gold_item_connector" /> | 3 | 16 items | 1 s | 5%, 4 s |
| <ItemLink id="rngtech:sparksteel_item_connector" /> | 5 | 32 items | 0.5 s | 2%, 3 s |
| <ItemLink id="rngtech:arclite_item_connector" /> | 6 | 64 items | 0.2 s | 1%, 2 s |

### Advanced Item Filter

An <ItemLink id="rngtech:advanced_item_filter" /> is a reusable filter you configure once and drop into an item filter slot. Right-click it to set it to **allow** or **deny**, and match by sample items, item tags, mod, component stage, Stability, identified state, or rarity. Deny filters are checked first, then allow filters. A row with only deny filters passes everything not denied.

The <ItemLink id="rngtech:miners_companion" /> accepts the same filter.

### Network bridges

The Bridge tab holds one <ItemLink id="rngtech:ae2_network_connector" /> or <ItemLink id="rngtech:refined_storage_network_connector" />. Two bridge connectors of the same kind, on the same bridge channel, join their AE2 or Refined Storage networks through your RNGTech cables. AE2 never links to Refined Storage, and RNGTech does not read or store anything in those networks. Without the matching mod installed, the bridge shows its status but does nothing.

- [Wrench](rngtech:universal-cable/wrench.md)
- [Configurator](rngtech:universal-cable/configurator.md)

## Obtaining

- [Crafting](rngtech:universal-cable/crafting.md)
- [Metal Press](rngtech:universal-cable/metal-press.md)

### Breaking

Mine Cables and Universal Connectors with a pickaxe. Aiming at a connector on a cable removes only that connector and leaves the cable. Mining the cable core breaks the cable and drops every connector on it, with their installed modules.

## More

- [Screen](rngtech:universal-cable/screen.md)
