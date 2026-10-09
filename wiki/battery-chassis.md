---
wiki:
  category: Energy storage
  icon: rngtech:iron_battery_chassis
  ids:
    - rngtech:wooden_battery_chassis
    - rngtech:iron_battery_chassis
    - rngtech:copper_battery_chassis
    - rngtech:gold_battery_chassis
    - rngtech:steel_battery_chassis
    - rngtech:sparksteel_battery_chassis
    - rngtech:arclite_battery_chassis
    - rngtech:nullite_battery_chassis
    - rngtech:aethergold_battery_chassis
    - rngtech:exotic_battery_chassis
---

# Battery Chassis

{{ infobox(
    variants=[
        ["Wooden", "rngtech:wooden_battery_chassis"],
        ["Iron", "rngtech:iron_battery_chassis"],
        ["Copper", "rngtech:copper_battery_chassis"],
        ["Gold", "rngtech:gold_battery_chassis"],
        ["Steel", "rngtech:steel_battery_chassis"],
        ["Sparksteel", "rngtech:sparksteel_battery_chassis"],
        ["Arclite", "rngtech:arclite_battery_chassis"],
        ["Nullite", "rngtech:nullite_battery_chassis"],
        ["Aethergold", "rngtech:aethergold_battery_chassis"],
        ["Exotic", "rngtech:exotic_battery_chassis"],
    ],
    fields={
        "Type": "Energy storage",
        "Stages": "0–8",
        "Power": "Stores FE in installed cells",
        "Gear": "{{ item('rngtech:iron_battery_cell', 'Battery Cells') }}",
        "Mastery": "No",
    },
) }}

The **Battery Chassis** is a placed energy bank that holds [Battery Cells](battery-cells.md). The cells provide the storage and decide how fast FE goes in and out. The chassis decides how many cells fit and adds its own behavior on top, such as balancing charge across cells or sealing in leakage.

Each chassis you craft rolls its own [rarity and affixes](rarity-and-affixes.md). Battery Chassis do not have Machine Mastery.

## Obtaining

### Crafting

Each stage has its own recipe built around a Machine Frame. You do not need the previous chassis to craft the next one.

{{ crafting() }}

### Breaking

Mine a placed chassis with a pickaxe. It drops itself and keeps its rolled traits. Installed cells drop as separate items and keep their own FE, rarity, and affixes, but lose any bonus capacity the chassis was giving them.

## Usage

### Storing power

Put Battery Cells in the chassis, then connect it to generators and machines directly or with a [Universal Cable](universal-cable.md). The chassis accepts FE from any side and pushes stored FE out to adjacent blocks that accept it. It does not push FE back into a side that fed it on the same tick.

- **Capacity** is the total of the installed cells, multiplied by the chassis's capacity affixes. That bonus only applies while the cells stay in this chassis.
- **Input and output speed** come from the installed cells. Their combined charge and discharge rates are the limit, not the chassis, so a big chassis full of weak cells is still slow. These rates are per tick and shared by every side and connector, so adding sides or connectors does not raise them. An attached Universal Cable connector can cap it lower; while it runs at its cap and the cells could move more, the readout shows **Attached connector limits input energy** or **Attached connector limits output energy**.
- **Idle loss** slowly drains stored FE. Cells leak on their own, and some chassis add or remove leakage.

Two Battery Chassis placed directly next to each other slowly even out their fill levels. Banks on the same cable network with connectors on Both also even out their charge and then stop, instead of draining each other, so daisy-chained banks pass charge along. See [storage balancing](universal-cable.md#energy).

### Stages

| Stage | Chassis | Cell slots | Notes |
|---:|---|---:|---|
| 0 | {{ item('rngtech:wooden_battery_chassis') }} | 1 | Novelty holder. Poor efficiency and stability, and it leaks. |
| 1 | {{ item('rngtech:iron_battery_chassis') }} | 1 | Simple and reliable, with no leakage. Strong per-cell bonuses. |
| 2 | {{ item('rngtech:copper_battery_chassis') }} | 2 | Better transfer rating with light leakage. |
| 3 | {{ item('rngtech:gold_battery_chassis') }} | 3 | Large burst identity, but less stable and leakier. |
| 4 | {{ item('rngtech:steel_battery_chassis') }} | 4 | Stable, no chassis leakage, and reduces installed cells' own leakage. |
| 5 | {{ item('rngtech:sparksteel_battery_chassis') }} | 5 | Charge Balancer. |
| 6 | {{ item('rngtech:arclite_battery_chassis') }} | 6 | High transfer rating with Charge Balancer. |
| 7 | {{ item('rngtech:nullite_battery_chassis') }} | 8 | Late-game bank with Charge Balancer. |
| 7 | {{ item('rngtech:aethergold_battery_chassis') }} | 6 | Seals installed cells so they do not leak at all. Strongest burst identity. |
| 8 | {{ item('rngtech:exotic_battery_chassis') }} | 10 | Largest, most stable bank, with weaker per-cell bonuses. |

More slots is not always better. Low-slot chassis apply their affixes more strongly to each cell, so a focused Iron or Copper chassis can still be worth keeping.

**Charge Balancer** spreads charging, discharging, and leakage evenly across all installed cells instead of filling or draining one at a time. Chassis without it can roll the **Balance Mode** prefix for the same effect. See [Balance Mode](rarity-and-affixes.md#balance-mode).

The Copper and Gold chassis, and every chassis from Sparksteel up, carry a burst identity. It does not raise output above what the cells can deliver.

### Gear

The Gear tab holds the Battery Cells. Any Battery Cell fits, one per slot. The **Additional Battery Slots** prefix can unlock up to four more slots, and the **Charged Storage** prefix doubles capacity after the chassis has held energy continuously for 10 minutes.

### Automation

| Side | Behavior |
|---|---|
| Any | Accepts and exports FE. |
| Any | Inserts and extracts Battery Cells, for example with a hopper or item connector. |

The chassis is not general item storage: it only holds Battery Cells.

## Screen

The Battery Chassis screen has four tabs:

- **Process**: stored FE, the chassis transfer rating, the cells' combined input and output rates, any attached connector cap, and last-tick transfer. Hover for exact values.
- **Gear**: the Battery Cell slots.
- **Stats**: the chassis's current stats, including traits.
- **Refinement**: refine the placed chassis's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Battery Cells](battery-cells.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
