# Universal Cable

Status: Prototype

Player guide: [Universal Cable](https://c-pettersson.github.io/rngtech/universal-cable/)

Resource ids: `rngtech:cable`, `rngtech:universal_connector`, `rngtech:crude_energy_connector`, `rngtech:basic_energy_connector`, `rngtech:copper_energy_connector`, `rngtech:gold_energy_connector`, `rngtech:sparksteel_energy_connector`, `rngtech:arclite_energy_connector`, `rngtech:exotic_energy_connector`, dev-only debug id `rngtech:debug_energy_connector`, `rngtech:basic_fluid_connector`, `rngtech:copper_fluid_connector`, `rngtech:gold_fluid_connector`, `rngtech:sparksteel_fluid_connector`, `rngtech:arclite_fluid_connector`, dev-only debug id `rngtech:debug_fluid_connector`, `rngtech:basic_item_connector`, `rngtech:copper_item_connector`, `rngtech:gold_item_connector`, `rngtech:sparksteel_item_connector`, `rngtech:arclite_item_connector`, dev-only debug id `rngtech:debug_item_connector`, `rngtech:ae2_network_connector`, `rngtech:refined_storage_network_connector`, `rngtech:connector_mold`, tool id `rngtech:wrench`, tool id `rngtech:configurator`, and filter item id `rngtech:advanced_item_filter`

Universal Cable is the pass-through cable body for endpoint-based FE, fluid, and item transfer and optional AE2/Refined Storage bridging. Cables store nothing and expose no extraction; Universal Connector endpoints and their installed modules own all transfer.

## Connector Tiers

Connector tier is the main RNGTech wiring limit: machines may have their own demand or supply, but the connector is the visible cap for cable transfer.

| Energy Connector | Stage | Transfer |
|---|---:|---:|
| Crude Energy Connector | 1 | `64 FE/t` |
| Basic Energy Connector | 1 | `128 FE/t` |
| Copper Energy Connector | 2 | `512 FE/t` |
| Gold Energy Connector | 3 | `2,048 FE/t` |
| Sparksteel Energy Connector | 5 | `8,192 FE/t` |
| Arclite Energy Connector | 6 | `32,768 FE/t` |
| Exotic Energy Connector | 8 | `1,000,000 FE/t` |
| Debug Energy Connector | Dev-only debug | `50,000,000 FE/t` |

| Fluid Connector | Stage | Shipment | Wait | Jam |
|---|---:|---:|---:|---:|
| Basic Fluid Connector | 1 | `100 mB` | `80 ticks` | `12%`, `8s` |
| Copper Fluid Connector | 2 | `250 mB` | `40 ticks` | `8%`, `6s` |
| Gold Fluid Connector | 3 | `500 mB` | `20 ticks` | `5%`, `4s` |
| Sparksteel Fluid Connector | 5 | `1,000 mB` | `10 ticks` | `2%`, `3s` |
| Arclite Fluid Connector | 6 | `4,000 mB` | `4 ticks` | `1%`, `2s` |
| Debug Fluid Connector | Dev-only debug | `16,000 mB` | `1 tick` | none |

| Item Connector | Stage | Shipment | Wait | Jam |
|---|---:|---:|---:|---:|
| Basic Item Connector | 1 | `1 item` | `80 ticks` | `12%`, `8s` |
| Copper Item Connector | 2 | `4 items` | `40 ticks` | `8%`, `6s` |
| Gold Item Connector | 3 | `16 items` | `20 ticks` | `5%`, `4s` |
| Sparksteel Item Connector | 5 | `32 items` | `10 ticks` | `2%`, `3s` |
| Arclite Item Connector | 6 | `64 items` | `4 ticks` | `1%`, `2s` |
| Debug Item Connector | Dev-only debug | `64 items` | `1 tick` | none |

## Connector Settings

Defaults, Attach behavior, and the Network tab are described on the player guide. Routing details it leaves out:

- Bridge channels are independent from Energy, Fluid, and Item channels.
- Distribution: `Even` splits each transfer across available outputs, then sends leftovers to outputs that can still accept. `First Available` uses stable endpoint order without cursor rotation.
- Fluid and Item rows: Attach `None` disables a row without removing its module. Source rows are polled round-robin so a slowly filled source does not starve later rows or channels. `OUT` tier caps per-delivery insertion.
- Installed modules drop with the block inventory.

## Cable Management

Placement, wrench, and Configurator use are on the player guide. Implementation notes:

- Using a Cable on a standalone Universal Connector space exports each plate's connector data by face and reinstalls it as cable-side Universal Connector data.
- The wrench hologram is a server snapshot (`WrenchOverlayServerHandler`) built only for standalone and cable-side Universal Connectors.
- Configurator presets store Attach as a `RelativeDirection` (`NONE`, `DEFAULT`, or an absolute side); copying captures `DEFAULT` when the source Attach equals its default side.
- Configurator Gear helper mode inserts through the machine's own Gear slot validation and does not expose Gear slots to item automation.

## Recipes

Crafting and Metal Press recipes are generated on the player guide from recipe data. Notes the recipes do not show:

- `rngtech:connector_mold` is Metal Press tooling tagged `rngtech:metal_press_molds`.
- `rngtech:crude_energy_connector` is the only connector module crafted without the Connector Mold.
- `rngtech:advanced_item_filter` is a reusable configuration item and is not a Component Recycler target.
- Debug connectors are dev-only. They have no survival recipe, are omitted from ordinary connector tags, are hidden from production creative tabs, and are rejected by connector slots or direct cable installation when debug content is disabled.
- `rngtech:ae2_network_connector` and `rngtech:refined_storage_network_connector` recipes load only when AE2 or Refined Storage is installed.

## Implementation Contract

Current runtime surface:

- Block and item ids: listed above.
- Block entities: `CableBlockEntity` and `UniversalConnectorBlockEntity`; side-mounted Universal Connector state is stored per face in the cable block entity.
- Menus and screens: `UniversalConnectorMenu` / `UniversalConnectorScreen`; cable-face connector configuration remains available through `CableConnectorMenu` / `CableConnectorScreen`; configurator actions use `ConfiguratorActionMenu` / `ConfiguratorActionScreen` and the advanced preset uses `ConfiguratorAdvancedMenu` / `ConfiguratorAdvancedScreen`.
- Wrench hologram: read-only world-space Universal Connector settings view with Summary, Energy, Item, and Fluid pages while the wrench is held.
- Slots: Universal Connector has Energy, Fluid, Item, Bridge, and Network tabs. The Energy tab has one functional Energy Connector slot, the Fluid tab has three functional Fluid Connector slots plus two ghost whitelist filter slots per row, the Item tab has three functional Item Connector slots plus two ghost item filter slots per row, Bridge has one AE2/Refined Storage Network Connector slot plus bridge channel controls and player inventory, and Network is inventory-free diagnostics with aggregate network snapshot metrics, selected-channel FE throughput, bridge diagnostics, and a Clear network cache action.
- Energy capability: Universal Connector exposes energy capability only when an Energy Connector module is installed. Cable faces expose energy capability through either a direct Energy Connector or a side-mounted Universal Connector energy module. Energy input endpoints accept pushed FE and actively extract FE from output-capable attached targets, capped by the installed Energy Connector tier. Energy Distribution is source-side and supports Round Robin, Even, and First Available routing across same-channel outputs.
- Fluid transfer: Universal Connector fluid modules actively move fluid shipments through connected cables to same-channel fluid modules. Cable blocks do not store fluid, but side-mounted Universal Connectors can expose a transient fluid capability on their attached face for same-channel insertion into the cable network. Fluid filters apply to active pulls, transient capability fills, and incoming cable shipments for the filtered row.
- Item transfer: Universal Connector item modules actively move item shipments through connected cables to same-channel item modules. Cable blocks do not store items, but side-mounted Universal Connectors can expose a transient item capability on their attached face for same-channel insertion into the cable network. Item filters apply to active pulls, transient capability inserts, and incoming cable shipments for the filtered row. Advanced Item Filter deny entries are checked before any allow entries on the same row.
- Network bridge capability: Universal Connector Bridge modules expose AE2 or Refined Storage network capabilities only when the matching optional mod is loaded. Matching same-mod Bridge endpoints on the same bridge channel are connected through the cable graph. AE2-to-Refined-Storage conversion and external storage exposure are not supported.
- Transfer rate: fixed per connector tier.
- Cable links are automatic blockstate connections between adjacent `rngtech:cable` blocks unless disabled by wrench; standalone Universal Connector blocks do not create cable links, and connector side modules render local cable body arms but do not create cable network links themselves.
- Universal Connector endpoint state is stored in either the placed connector block entity or per face in the cable block entity. Direct cable-face Energy Connector endpoint state is also stored per face in the cable block entity.
- RPG traits and refinement are out of scope for cables and Energy Connectors.

Implementation checks:

- Placing cables adjacent to each other should create cable links.
- Wrench-disabled cable links should persist across reload.
- Universal Connectors without installed Energy Connectors should expose no FE capability.
- FE inserted into a Universal Connector should route only to Universal Connectors on the same channel.
- Connector modes should control whether a connector accepts network input, sends network output, or both.
- Energy Distribution should control how a source connector selects same-channel output endpoints without adding cable storage.
- Attach As should change the side used for the target block capability lookup.
- Module install, replacement, configuration, and block-break drops should preserve installed connector items.
- Mining a side-mounted connector face should remove only that connector; mining the cable core should remove the Cable block and drop its installed connectors.
- Fluid module mode, channel, and Attach settings should be independent per row, multiple rows may target the same channel and side, and source polling should rotate across rows to avoid fixed-order starvation.
- Fluid `IN` shipments should respect tier shipment size, wait time, and jam chance; jams pause that source module without consuming source fluid. Fluid `OUT` modules should cap per-delivery insertion by tier without adding an extra wait timer.
- Fluid filters should be row-local optional whitelists: no filter means unrestricted, one or two filled filter slots restrict movement to matching fluids.
- Item module mode, channel, and Attach settings should be independent per row, multiple rows may target the same channel and side, and source polling should rotate across rows to avoid fixed-order starvation.
- Item `IN` shipments should respect tier shipment size, wait time, and jam chance; jams pause that source module without consuming source items. Item `OUT` modules should cap per-delivery insertion by tier without adding an extra wait timer.
- Item filters should be row-local optional filters: no filter means unrestricted, item-template filters are allow entries, Advanced Item Filters can be allow or deny entries, and deny entries are evaluated before allow entries.
- Bridge modules should install only in the Bridge tab, bridge channels should be independent from Energy/Fluid/Item channels, same-mod same-channel endpoints should connect, different channels should stay isolated, and AE2 modules should never connect to Refined Storage modules.
- Without AE2 or Refined Storage installed, the game should still load, optional recipes should be hidden, and installed Bridge modules should report unloaded-mod status without exposing third-party capabilities.
- Cables should not store FE, fluids, or items, or expose extraction.
- Configurator settings-only paste should not change installed connector modules. Fluid and item ghost filters copy and paste as connector settings. Module paste should consume player-held modules atomically and leave the target unchanged when required modules or inventory space are missing.
- Configurator Gear helper mode should insert only into compatible empty Gear/component slots and should not change machine item automation capabilities.

## Future Work

Heat controls are planned to use the same endpoint model. Future wireless connector variants should share connector configuration logic, using connector codes or equivalent pairing data instead of direct cable adjacency.

## Modifier Eligibility

Universal Cable blocks, Energy Connectors, Fluid Connectors, Item Connectors, and Network Connectors do not roll modifiers and do not consume modifier rules. Transfer rate, shipment size, wait time, jam behavior, and bridge type are fixed by endpoint item, and channel, mode, Distribution, Attach, disabled links, and installed connector faces are placement/configuration state only.

Available modifiers: none.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Metal Press](metal-press.md)
- [Battery Chassis](battery-chassis.md)
- [Solid Fuel Burning](solid-fuel-burner.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
