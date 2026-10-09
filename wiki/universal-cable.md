---
wiki:
  category: Logistics
  icon: rngtech:cable
  ids:
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

{{ infobox(
    variants=[
        ["Cable", "rngtech:cable"],
        ["Connector", "rngtech:universal_connector"],
    ],
    fields={
        "Type": "Cable network",
        "Connector stages": "1–8",
        "Power": "None (carries FE)",
        "Modules": "{{ item('rngtech:basic_energy_connector', 'Energy') }}, {{ item('rngtech:basic_fluid_connector', 'Fluid') }}, {{ item('rngtech:basic_item_connector', 'Item') }}, {{ item('rngtech:ae2_network_connector', 'Network') }}",
        "Mastery": "No",
    },
) }}

**Universal Cable** is RNGTech's one cable for everything. The same cable carries FE, fluids, and items between machines, and can extend an AE2 or Refined Storage network. The cable itself stores nothing. A **Universal Connector** on the cable next to each machine decides what moves, which way, and how fast, through the connector modules you install in it.

Cables, connectors, and modules do not roll rarity or affixes, and cannot be refined.

## Obtaining

### Crafting

One craft makes 12 Cables or 2 Universal Connectors. The {{ item('rngtech:crude_energy_connector') }} is the only connector module you can craft by hand.

{{ crafting("rngtech:cable", "rngtech:universal_connector", "rngtech:crude_energy_connector", "rngtech:advanced_item_filter") }}

The network connectors have recipes only when their mod is installed:

- {{ item('rngtech:ae2_network_connector') }}: a Universal Connector, two Basic Energy Connectors, an AE2 Engineering Processor, Fluix Glass Cables, and redstone.
- {{ item('rngtech:refined_storage_network_connector') }}: a Universal Connector, four Basic Item Connectors, Refined Storage Cables, and a Construction Core and Destruction Core.

### Metal Press

Every other Energy, Fluid, and Item Connector is pressed in a [Metal Press](metal-press.md) with a Connector Mold. Energy Connectors start from a coil, Fluid Connectors from a casing, and Item Connectors from a gear.

{{ processing("metal_press", input="rngtech:connector_mold", columns=["processing_ticks", "energy", "minimum_temperature"]) }}

### Breaking

Mine Cables and Universal Connectors with a pickaxe. Aiming at a connector on a cable removes only that connector and leaves the cable. Mining the cable core breaks the cable and drops every connector on it, with their installed modules.

## Usage

### Building a network

1. Place Cables. Touching Cables link up on their own.
2. Use a Universal Connector on the cable face beside a machine. You can also use it on the machine itself when a Cable is next to it. The connector mounts on that face of the cable.
3. Right-click the connector with an empty hand to open it, and install modules: an Energy Connector for FE, Fluid Connectors for fluids, Item Connectors for items.
4. Set each module's channel and mode. Only connectors on the same channel exchange anything.

One Cable can hold a connector on each of its six faces.

You can also install an Energy Connector directly on a cable face, without a Universal Connector, for simple power links. Each face holds either one direct Energy Connector or one Universal Connector, never both.

#### Standalone plates

A Universal Connector placed on its own, with no Cable, works as a standalone plate. Use another Universal Connector on a different face of the same space to add a second plate there. Plates join a network only once they sit on a Cable: use a Cable on the plate's space, or on the machine face behind it, and the space turns into a Cable. Every plate in it keeps its modules and settings.

### Connector settings

| Setting | What it does |
|---|---|
| Channel | 0–15. Connectors only exchange with the same channel. Energy, Fluid, Item, and Bridge channels are separate. |
| Mode | Energy: Both, Input, or Output. Fluid and Item rows: IN pulls from the machine into the cable, OUT pushes from the cable into the machine. |
| Attach As | Which side of the machine the connector acts as. For example, a connector under a machine can insert as if it were on top. Set a Fluid or Item row to None to switch it off. |
| Distribution | Energy only. Round Robin rotates between outputs, Even splits each transfer, First Available fills outputs in order. |
| Filters | Fluid and Item rows each have two ghost filter slots. Filters are copies and are never used up. |

A new connector starts on channel 0, with Energy mode Both, Distribution Round Robin, and every Fluid and Item row set to IN. Attach As starts as the machine face the connector actually touches. Each Fluid and Item row has its own mode, channel, and Attach, and several rows can share a channel and side.

### Energy

The Energy tab has one Energy Connector slot. The connector pulls FE from machines that output it, carries it through the cable, and delivers it to Output connectors on the same channel. The installed connector's tier is the cap on how much FE that endpoint moves.

| Connector | Stage | Transfer |
|---|---:|---:|
| {{ item('rngtech:crude_energy_connector') }} | 1 | 64 FE/t |
| {{ item('rngtech:basic_energy_connector') }} | 1 | 128 FE/t |
| {{ item('rngtech:copper_energy_connector') }} | 2 | 512 FE/t |
| {{ item('rngtech:gold_energy_connector') }} | 3 | 2,048 FE/t |
| {{ item('rngtech:sparksteel_energy_connector') }} | 5 | 8,192 FE/t |
| {{ item('rngtech:arclite_energy_connector') }} | 6 | 32,768 FE/t |
| {{ item('rngtech:exotic_energy_connector') }} | 8 | 1,000,000 FE/t |

### Fluids

The Fluid tab has three rows, each with a Fluid Connector slot, its own mode, channel, and Attach setting, and two filter slots. An IN row drains a shipment from the machine, waits, and repeats. An OUT row fills its machine from shipments arriving on its channel, up to its tier's shipment size. To filter a row, put a filled bucket or other fluid container in a filter slot.

Each shipment can **jam**: the row pauses for a few seconds without losing any fluid. Several IN rows on the same machine side and channel run in parallel, so stacking rows raises throughput.

| Connector | Stage | Shipment | Every | Jam chance, pause |
|---|---:|---:|---:|---:|
| {{ item('rngtech:basic_fluid_connector') }} | 1 | 100 mB | 4 s | 12%, 8 s |
| {{ item('rngtech:copper_fluid_connector') }} | 2 | 250 mB | 2 s | 8%, 6 s |
| {{ item('rngtech:gold_fluid_connector') }} | 3 | 500 mB | 1 s | 5%, 4 s |
| {{ item('rngtech:sparksteel_fluid_connector') }} | 5 | 1,000 mB | 0.5 s | 2%, 3 s |
| {{ item('rngtech:arclite_fluid_connector') }} | 6 | 4,000 mB | 0.2 s | 1%, 2 s |

### Items

The Item tab works the same way as the Fluid tab, with three Item Connector rows. Put an item in a row's filter slot to allow only that item.

| Connector | Stage | Shipment | Every | Jam chance, pause |
|---|---:|---:|---:|---:|
| {{ item('rngtech:basic_item_connector') }} | 1 | 1 item | 4 s | 12%, 8 s |
| {{ item('rngtech:copper_item_connector') }} | 2 | 4 items | 2 s | 8%, 6 s |
| {{ item('rngtech:gold_item_connector') }} | 3 | 16 items | 1 s | 5%, 4 s |
| {{ item('rngtech:sparksteel_item_connector') }} | 5 | 32 items | 0.5 s | 2%, 3 s |
| {{ item('rngtech:arclite_item_connector') }} | 6 | 64 items | 0.2 s | 1%, 2 s |

### Advanced Item Filter

An {{ item('rngtech:advanced_item_filter') }} is a reusable filter you configure once and drop into an item filter slot. Right-click it to set it to **allow** or **deny**, and match by sample items, item tags, mod, component stage, Stability, identified state, or rarity. Deny filters are checked first, then allow filters. A row with only deny filters passes everything not denied.

The Miner's Companion accepts the same filter.

### Network bridges

The Bridge tab holds one {{ item('rngtech:ae2_network_connector') }} or {{ item('rngtech:refined_storage_network_connector') }}. Two bridge connectors of the same kind, on the same bridge channel, join their AE2 or Refined Storage networks through your RNGTech cables. AE2 never links to Refined Storage, and RNGTech does not read or store anything in those networks. Without the matching mod installed, the bridge shows its status but does nothing.

### Wrench

The {{ item('rngtech:wrench') }} manages links and shows connector settings.

- Right-click a cable face that points at another Cable to switch that link off or on. Disabled links stay off after reloads.
- Right-click a connector on a cable face to open its screen.
- Hold the Wrench while looking at a Universal Connector, standalone or on a cable, to see a hologram of its settings. Direct Energy Connectors on a cable face show no hologram.
- The hologram has Summary, Energy, Item, and Fluid pages. The Summary page also shows the bridge, such as `Bridge: AE2 ch 3`. Shift + right-click cycles the pages, and so does the **Cycle Wrench View** key. That key has no default binding; set it under Controls.

### Configurator

The {{ item('rngtech:configurator') }} copies connector settings and pastes them onto other connectors. It works on standalone and cable-side Universal Connectors and on direct Energy Connectors on a cable face.

Right-click a connector to open the Configurator menu:

- **Copy All**, **Copy Energy**, **Copy Fluid**, or **Copy Item** stores those settings on the Configurator. A partial copy replaces only that part of the stored preset.
- **Paste** applies the stored channels, modes, Distribution, Attach As, and ghost filters. It never changes installed modules unless module paste is on.

An Attach setting left on its starting side is stored as **Default**, so a preset pastes correctly onto connectors facing other directions.

To edit the stored preset, hold the Configurator and press the **Open Configurator Preset** key. It has no default binding; set it under Controls. The preset screen can:

- Pick a category (All, Energy, Fluid, or Item), then set its channel, cycle its mode, or set its Attach to Default, None, or a fixed side, for every row at once.
- **Flip** every mode in the preset: Input and Output swap, and so do IN and OUT.
- **Reset** the selected category, or **Reset All**.
- Turn **module paste** on (Mod On). Paste then also installs the matching modules from your inventory and hands back any modules it replaces. If a module is missing or you have no room for the replaced ones, nothing changes at all.
- Turn the **Gear helper** on (Gear On). Right-click a machine to fill its empty Gear slots from your inventory, with the same rules as placing Gear by hand. It does not let pipes or hoppers reach Gear slots.

The Configurator can also copy Machine Mastery builds. See [Machine Mastery](machine-mastery.md).

## Screen

A Universal Connector's screen has five tabs:

- **Energy**: the Energy Connector slot with mode, channel, Distribution, and Attach settings, plus FE in and out last tick.
- **Fluid**: three Fluid Connector rows with mode, channel, Attach, and two filter slots each.
- **Item**: three Item Connector rows with mode, channel, Attach, and two filter slots each.
- **Bridge**: the network connector slot, bridge channel, and link status.
- **Network**: a read-only overview of the whole cable network: cables, connectors, modules, and channels. The FE meter shows power actually delivered on this connector's energy channel, as In, Out, and Sum, averaged over 1, 5, or 15 minutes. It does not count demand that went unmet. Hover a value for the live rate. **Clear network cache** forces a rebuild.

## Data values

{{ data_values() }}

## See also

- [Metal Press](metal-press.md): presses Basic and higher connectors.
- [Battery Chassis](battery-chassis.md): store FE on the network.

{{ navbox() }}
