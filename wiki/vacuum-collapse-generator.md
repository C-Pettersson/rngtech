---
wiki:
  category: Power generation
  icon: rngtech:vacuum_collapse_generator
  ids:
    - rngtech:vacuum_collapse_generator
---

# Vacuum Collapse Generator

{{ infobox(
    fields={
        "Type": "Generator",
        "Stages": "7",
        "Power": "FE generator",
        "Gear": "{{ item('rngtech:tungstensteel_void_chamber', 'Void Chamber') }}, {{ item('rngtech:tungstensteel_collapse_nozzle', 'Collapse Nozzle') }}, {{ item('rngtech:tungstensteel_dimensional_stabilizer', 'Dimensional Stabilizer') }}, {{ item('rngtech:crude_energy_connector', 'Energy Connector') }}",
        "Mastery": "No",
    },
) }}

The **Vacuum Collapse Generator** is RNGTech's Stage 7 primary generator. It collapses one {{ item('rngtech:void_catalyst') }} at a time into a huge batch of FE, about 1,000 FE/t before Gear, and can leave {{ item('rngtech:collapse_residue') }} behind. Its Gear decides how much it makes and how stable each collapse is.

Like every RNGTech machine, each Vacuum Collapse Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md).

## Obtaining

### Crafting

The recipe itself uses a full set of Tungstensteel Gear, so you will need to craft a second set to run it.

{{ crafting() }}

### Breaking

Mine a placed Vacuum Collapse Generator with a pickaxe. It drops itself. Installed Gear and stored items drop separately.

## Usage

### Generating power

Install a Void Chamber, a Collapse Nozzle, and a Dimensional Stabilizer in the Gear tab, then put Void Catalyst in the input slot. Each catalyst starts one collapse, which generates its FE over the cycle and then drops any residue into the output slot.

A collapse starts only when:

- the Void Chamber's stage meets the recipe's minimum (Stage 7 for Void Catalyst),
- the machine's stability is at least the recipe's minimum (1.0 for Void Catalyst), and
- the residue slot has room.

Once a collapse starts it cannot pause for full storage. FE that the 20,000 FE internal buffer cannot hold is lost, so connect enough cables and storage to take each catalyst's output. A redstone signal pauses it, even in the middle of a collapse, which is the way to hold it while storage catches up.

### Instability

Every recipe adds instability, and higher stability reduces it. Instability makes each collapse slower and yield less FE, and every full point of it adds an extra Collapse Residue. If the installed Collapse Nozzle's stage is lower than the recipe's minimum chamber stage, each missing stage adds a full point of instability.

### Gear

| Slot | Accepts | Role |
|---|---|---|
| Void Chamber | Tungstensteel, Nullite, or Exotic Void Chambers | Required. Sets the stage, generation, and stability. |
| Collapse Nozzle | Steel through Exotic Collapse Nozzles. The Nitrogen Separation Nozzle is rejected. | Required. Tunes generation, transfer, speed, and stability. |
| Dimensional Stabilizer | Tungstensteel, Nullite, or Exotic Dimensional Stabilizers | Raises stability and efficiency. Void Catalyst only leaves residue while one is installed. |
| Energy Connector | Any Energy Connector | Optional. Sets how fast FE leaves the sides. Without one, the Gear's own transfer rate is used. Either way, the cap is per tick and shared by all sides. |

Chamber and nozzle generation multiply each other. Unmodified Tungstensteel Gear reaches about 1,057 FE/t, and unmodified Exotic Gear about 3,000 FE/t. Rolled and refined parts push beyond that. See [Stages](stages.md) for the Stage 7 power band.

The Collapse Nozzle is shared with the [Cavitation Generator](cavitation-generator.md).

### Automation

| Side | Behavior |
|---|---|
| Top | Inserts catalysts. |
| Bottom | Extracts Collapse Residue. |
| Sides | Output FE, for example into a [Universal Cable](universal-cable.md). |

Gear slots are not reachable by automation.

### Void Catalyst

{{ crafting('rngtech:void_catalyst') }}

### Vacuum Collapse recipes

The Vacuum Collapse Generator uses its own recipe type, `rngtech:vacuum_collapse`.

{{ processing("vacuum_collapse", hide=["residue_requires_stabilizer"]) }}

## Screen

The Vacuum Collapse Generator screen has four tabs:

- **Process**: the catalyst and residue slots, FE, cycle progress, and instability. The generation icon shows generated FE/t, and the output icon shows the side export cap. Click the recipe line to open its JEI category.
- **Gear**: Void Chamber, Collapse Nozzle, Dimensional Stabilizer, and Energy Connector slots.
- **Stats**: the machine's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Gear](gear.md)
- [Potential Reactor](potential-reactor.md), whose Containment Linings are separate from the Dimensional Stabilizer.

{{ navbox() }}
