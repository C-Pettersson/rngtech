# Universal Cable

Resource ids: `rngtech:cable`, `rngtech:universal_connector`, `rngtech:crude_energy_connector`, `rngtech:basic_energy_connector`, `rngtech:copper_energy_connector`, `rngtech:gold_energy_connector`, `rngtech:sparksteel_energy_connector`, `rngtech:arclite_energy_connector`, `rngtech:exotic_energy_connector`, dev-only debug id `rngtech:debug_energy_connector`, `rngtech:basic_fluid_connector`, `rngtech:copper_fluid_connector`, `rngtech:gold_fluid_connector`, `rngtech:sparksteel_fluid_connector`, `rngtech:arclite_fluid_connector`, dev-only debug id `rngtech:debug_fluid_connector`, `rngtech:basic_item_connector`, `rngtech:copper_item_connector`, `rngtech:gold_item_connector`, `rngtech:sparksteel_item_connector`, `rngtech:arclite_item_connector`, dev-only debug id `rngtech:debug_item_connector`, `rngtech:ae2_network_connector`, `rngtech:refined_storage_network_connector`, `rngtech:connector_mold`, tool id `rngtech:wrench`, tool id `rngtech:configurator`, and filter item id `rngtech:advanced_item_filter`

Status: Prototype

## Behavior

The Universal Cable is the physical cable body for endpoint-based transfer. The cable block is pass-through only: it stores no FE and does not expose energy extraction from the cable itself.

Cables automatically link to adjacent cables and Universal Connector blocks unless a wrench-disabled link blocks that face.

`rngtech:universal_connector` is the machine-facing endpoint. It can be installed as a side module on a Cable block face, sharing the cable blockspace, or placed as the older standalone compact endpoint block. Install it against a machine, open its machine-style screen, and install connector modules in the matching tab. The screen has five tabs: Energy, Fluid, Item, Bridge, and Network. The Energy tab has one Energy Connector slot with mode, channel, Distribution, and Attach settings. The Fluid and Item tabs each have three connector slots, each with its own mode, channel, and Attach setting. Fluid and Item Attach can be set to `None` to disable that row without removing the module. The Bridge tab has one Network Connector slot, its own bridge channel controls, a compact module/mod/peer status panel, and the player inventory. The Network tab is diagnostics-only and shows a current network snapshot with aggregate cable nodes, Universal Connector endpoints, Energy/Fluid/Item/Bridge module counts, active channels, transfer caps, cache freshness, selected-channel FE throughput, bridge endpoint counts, bridge channels, and a Clear network cache action for forcing the connector network graph snapshot to rebuild.

The first implemented module family is Energy Connectors:

| Connector | Stage | Transfer |
|---|---:|---:|
| Crude Energy Connector | 1 | `64 FE/t` |
| Basic Energy Connector | 1 | `128 FE/t` |
| Copper Energy Connector | 2 | `512 FE/t` |
| Gold Energy Connector | 3 | `2,048 FE/t` |
| Sparksteel Energy Connector | 5 | `8,192 FE/t` |
| Arclite Energy Connector | 6 | `32,768 FE/t` |
| Exotic Energy Connector | 8 | `1,000,000 FE/t` |
| Debug Energy Connector | Dev-only debug | `50,000,000 FE/t` |

Universal Connectors without installed Energy Connectors expose no energy capability. A Universal Connector with an installed Energy Connector can accept pushed FE from its target machine side, actively extract FE from output-capable target sides, push that FE into adjacent connected cables, and receive FE from connected cables for insertion into its target block. FE routing traverses connected cable blocks and only targets same-channel output endpoints. The source connector's Distribution setting controls same-channel output ordering: `Round Robin` rotates output priority after each transfer, `Even` splits each transfer across available outputs and then sends leftovers to outputs that can still accept, and `First Available` fills earlier outputs before later outputs.

The Energy tab, wrench hologram, and Jade connector readout show the installed connector cap plus last-tick FE into the cable network and last-tick FE out to the attached target. The Network tab also shows actual FE throughput for the opened connector's selected energy channel, with Input, Output, and Sum over a toggleable `1 min`, `5 min`, or `15 min` average window. Output is actual FE delivered to machines or storage on that channel, not theoretical unmet demand. This makes Universal Connector tier the main user-facing RNGTech wiring limit. Machines and generators may have their own demand or supply, but the connector is the visible cap for cable transfer.

Fluid Connectors are the second implemented module family:

| Connector | Stage | Shipment | Wait | Jam |
|---|---:|---:|---:|---:|
| Basic Fluid Connector | 1 | `100 mB` | `80 ticks` | `12%`, `8s` |
| Copper Fluid Connector | 2 | `250 mB` | `40 ticks` | `8%`, `6s` |
| Gold Fluid Connector | 3 | `500 mB` | `20 ticks` | `5%`, `4s` |
| Sparksteel Fluid Connector | 5 | `1,000 mB` | `10 ticks` | `2%`, `3s` |
| Arclite Fluid Connector | 6 | `4,000 mB` | `4 ticks` | `1%`, `2s` |
| Debug Fluid Connector | Dev-only debug | `16,000 mB` | `1 tick` | none |

Fluid Connectors use per-row mode. `IN` modules actively drain fluid from the attached target block through the configured Attach side and route through adjacent connected cables. `OUT` modules passively receive same-channel fluid shipments from connected cables and fill the attached target block. `IN` module wait and jam timers are independent, so multiple `IN` rows on the same channel and Attach side increase throughput. Source rows are polled round-robin so a slowly-filled tank does not permanently starve later rows or other channels. `OUT` module tier caps how much one incoming delivery can insert. Multiple fluid modules may use the same channel and Attach side. Each row has two ghost whitelist filter slots that copy filled fluid containers such as buckets without consuming or storing them. If either filter slot is filled, that row only moves matching fluids; empty filters leave the row unfiltered.

Item Connectors are the third implemented module family:

| Connector | Stage | Shipment | Wait | Jam |
|---|---:|---:|---:|---:|
| Basic Item Connector | 1 | `1 item` | `80 ticks` | `12%`, `8s` |
| Copper Item Connector | 2 | `4 items` | `40 ticks` | `8%`, `6s` |
| Gold Item Connector | 3 | `16 items` | `20 ticks` | `5%`, `4s` |
| Sparksteel Item Connector | 5 | `32 items` | `10 ticks` | `2%`, `3s` |
| Arclite Item Connector | 6 | `64 items` | `4 ticks` | `1%`, `2s` |
| Debug Item Connector | Dev-only debug | `64 items` | `1 tick` | none |

Item Connectors use per-row mode. `IN` modules actively pull from the attached target block through the configured Attach side and route through adjacent connected cables. `OUT` modules passively receive same-channel shipments from connected cables and insert into the attached target block. `IN` module wait and jam timers are independent, so multiple `IN` rows on the same channel and Attach side increase throughput. Source rows are polled round-robin so a slowly-fed inventory does not permanently starve later rows or other channels. `OUT` module tier caps how many items one incoming delivery can insert. Multiple item modules may use the same channel and Attach side. Each row has two ghost filter slots that copy item templates or configured Advanced Item Filters without consuming or storing them. Normal item templates whitelist matching item stacks. Advanced Item Filters can be configured as allow or deny filters using samples, item tags, item mod ids, component stage, Stability, identified/unidentified state, and rarity. Deny filters veto first; allow filters then whitelist. A row with only deny filters allows anything not denied, and an empty row remains unfiltered.

Network Bridge Connectors are optional compat modules:

| Connector | Required mod | Behavior |
|---|---|---|
| AE2 Network Connector | Applied Energistics 2 | Exposes AE2 in-world grid nodes and bridges matching AE2 endpoints through the RNGTech cable graph. |
| Refined Storage Network Connector | Refined Storage | Exposes Refined Storage network node containers and bridges matching Refined Storage endpoints through the RNGTech cable graph. |

Bridge modules are bidirectional same-mod network extenders. They only connect to matching Bridge modules of the same type on the same RNGTech bridge channel. AE2 modules never connect to Refined Storage modules, different bridge channels stay isolated, and the bridge does not convert between network mods or expose external storage to RNGTech. If the required optional mod is not loaded, the installed module remains visible in the Bridge tab but does not expose the third-party network capability.

## Connector Settings

Each Universal Connector stores:

| Setting | Current behavior |
|---|---|
| Channel | `0-15`, defaults to `0`. Only matching channels route together. |
| Attach As | Capability side used when interacting with the adjacent target block. Energy defaults to the physical opposite side. Fluid and Item rows can also use `None`, which disables that row. |
| Mode | `Both`, `Input`, or `Output`, defaults to `Both`. Input accepts FE into the network; Output sends FE from the network to the adjacent target. |
| Distribution | Energy-only output selection, defaults to `Round Robin`. `Even` splits each transfer across same-channel outputs before a leftover pass. `First Available` uses stable endpoint order without cursor rotation. |
| Fluid mode | Per Fluid Connector row, either `IN` to drain from the target into the cable network or `OUT` to fill the target from cable shipments. |
| Item mode | Per Item Connector row, either `IN` to pull from the target into the cable network or `OUT` to insert cable shipments into the target. |
| Bridge channel | `0-15`, defaults to `0`. Bridge channels are independent from Energy, Fluid, and Item channels. Only matching same-mod Bridge modules on the same bridge channel connect. |
| Fluid filter | Two optional per-row ghost whitelist slots. Filled fluid containers define the allowed fluid for that row without being consumed or stored. |
| Item filter | Two optional per-row ghost filter slots. Item stacks whitelist matching stacks, and Advanced Item Filters can allow or deny by configured item criteria without being consumed or stored. |
| Transfer | Fixed by the installed Energy Connector tier for FE, and by each installed Fluid or Item Connector tier for shipments. |
| Installed module | One Energy Connector item in the Energy tab slot, three Fluid Connector items in the Fluid tab, three Item Connector items in the Item tab, and one AE2 or Refined Storage Network Connector item in the Bridge tab. Installed modules drop with the block inventory. |

Attach As lets a Universal Connector act as another side for target capability lookup. For example, a connector physically touching the bottom of a target can be configured to insert as that target's top side.

## Cable Management

Use `rngtech:universal_connector` on a Cable face beside the machine side that should be connected. It can also be used on a machine face when the adjacent placement position is already occupied by a Cable; in that case it installs onto the matching Cable side instead of trying to place a separate block. If a standalone Universal Connector was placed first, using another Universal Connector on a different valid face adds another standalone connector plate to the same blockspace without creating a Cable. Using a Cable on that connector blockspace or on the machine face behind it explicitly converts the blockspace into a Cable and preserves each connector's module inventory and configuration as Cable side modules. Open the connector and place an Energy Connector item in the Energy tab slot, Fluid Connector items in the Fluid tab slots, Item Connector items in the Item tab slots, or an AE2/Refined Storage Network Connector item in the Bridge tab slot. Standalone Universal Connector blocks remain supported for older layouts, render as connector plates without a local cable body, and do not join a cable network until a Cable is merged into the same blockspace.

One Cable block can hold side-mounted Universal Connectors on multiple different faces, up to one side module on each of the six faces. A face can hold either one direct Energy Connector or one Universal Connector, not both. Cable links and side connector modules are separate state: cable link booleans create network links and render cable arms, while connector booleans on Cable blocks render both the local cable arm and the connector ring for that face.

Use `rngtech:wrench` on placed cables:

- Right-click a cable-to-cable face to toggle that cable link on or off.
- Right-click a cable-to-Universal Connector face to toggle that endpoint link on or off.
- Right-click a cable face with a side-mounted Universal Connector to open its Universal Connector screen.

Mining a side-mounted Universal Connector or direct Energy Connector removes only the targeted connector face and leaves the Cable block in place. Mining the cable core breaks the Cable block and drops all installed connector items and Universal Connector module inventories.

Hold `rngtech:wrench` while looking at a Universal Connector to show a read-only holographic settings view in the world. The hologram supports standalone Universal Connectors and cable-side Universal Connectors. It does not show direct cable-face Energy Connectors. The Summary page includes a compact Bridge line such as `Bridge: AE2 ch 3` or `Bridge: none`. Cycle the hologram between Summary, Energy, Item, and Fluid pages with Shift + right-click, or bind and use the `Cycle Wrench View` key.

Use `rngtech:configurator` on a standalone Universal Connector, cable-side Universal Connector, or direct cable-face Energy Connector to copy or paste connector settings. The action menu can copy all settings or only Energy, Fluid, or Item settings. Paste applies copied channels, modes, Energy Distribution, and Attach As values. The stored preset keeps target-default Attach settings relative, so a preset copied from one connector orientation can paste onto another connector orientation. The advanced configurator key opens the stored preset, where module installation on paste can be enabled, modes can be flipped in bulk, channels and Attach settings can be bulk-edited, and the preset can be reset.

By default, paste does not change installed connector modules. When module paste is enabled in the advanced preset menu, the configurator consumes matching connector modules from the player's inventory and replaces mismatched target modules only when all required modules and inventory space for replaced modules are available. The configurator can also enable a manual Gear helper mode. In that mode, using the tool on a compatible machine attempts to fill empty Gear/component slots from the player's inventory through the machine's existing Gear slot validation. This does not expose Gear slots to ordinary item automation.

Disabled cable links persist through neighbor updates and world reloads.

## Recipes

`rngtech:cable` has a cheap bulk crafting recipe from wooden slabs and redstone. `rngtech:universal_connector` is crafted from Cable, Iron ingots, and redstone. `rngtech:configurator` is crafted shapelessly from Cable, redstone, and an Iron ingot. `rngtech:advanced_item_filter` is crafted from a Basic Item Connector, a Basic Circuit, redstone, and a Glass Pane. It is a reusable configuration item and is not a Component Recycler target.

`rngtech:connector_mold` is crafted as Metal Press tooling and is tagged as `rngtech:metal_press_molds`.

`rngtech:crude_energy_connector` is crafted directly from Cable, an Iron Coil, and redstone. It does not require the Connector Mold or Metal Press.

Basic and higher Energy Connectors, plus all survival Fluid Connectors and Item Connectors, are produced by `rngtech:metal_press` recipes using the Connector Mold.

| Connector | Recipe input | Tooling |
|---|---|---|
| Crude Energy Connector | Direct crafting from `rngtech:cable`, `rngtech:iron_coil`, and `minecraft:redstone` | none |
| Basic Energy Connector | `rngtech:iron_coil` | `rngtech:connector_mold` |
| Copper Energy Connector | `rngtech:copper_coil` | `rngtech:connector_mold` |
| Gold Energy Connector | `rngtech:gold_coil` | `rngtech:connector_mold` |
| Sparksteel Energy Connector | `rngtech:sparksteel_coil` | `rngtech:connector_mold` |
| Arclite Energy Connector | `rngtech:arclite_coil` | `rngtech:connector_mold` |
| Exotic Energy Connector | `8x rngtech:nullite_coil` | `rngtech:connector_mold` |

| Fluid Connector | Press ingredient | Mold |
|---|---|---|
| Basic Fluid Connector | `rngtech:iron_casing` | `rngtech:connector_mold` |
| Copper Fluid Connector | `rngtech:copper_casing` | `rngtech:connector_mold` |
| Gold Fluid Connector | `rngtech:gold_casing` | `rngtech:connector_mold` |
| Sparksteel Fluid Connector | `rngtech:sparksteel_casing` | `rngtech:connector_mold` |
| Arclite Fluid Connector | `rngtech:arclite_casing` | `rngtech:connector_mold` |

| Item Connector | Press ingredient | Mold |
|---|---|---|
| Basic Item Connector | `rngtech:iron_gear` | `rngtech:connector_mold` |
| Copper Item Connector | `rngtech:copper_gear` | `rngtech:connector_mold` |
| Gold Item Connector | `rngtech:gold_gear` | `rngtech:connector_mold` |
| Sparksteel Item Connector | `rngtech:sparksteel_gear` | `rngtech:connector_mold` |
| Arclite Item Connector | `rngtech:arclite_gear` | `rngtech:connector_mold` |

The debug connectors are dev-only. They have no survival recipe, are omitted from ordinary connector tags, are hidden from production creative tabs, and are rejected by connector slots or direct cable installation when debug content is disabled.

`rngtech:ae2_network_connector` has an optional shaped recipe that is loaded only when AE2 is installed. `rngtech:refined_storage_network_connector` has an optional shaped recipe that is loaded only when Refined Storage is installed.

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
