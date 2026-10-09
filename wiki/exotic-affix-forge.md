---
wiki:
  category: Refinement
  icon: rngtech:exotic_affix_forge
  ids:
    - rngtech:exotic_affix_forge
    - rngtech:exotic_affix_catalyst
---

# Exotic Affix Forge

{{ infobox(
    variants=["rngtech:exotic_affix_forge"],
    fields={
        "Type": "Refinement station",
        "Stages": "8",
        "Power": "FE consumer",
        "Gear": "{{ item('rngtech:exotic_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Exotic Affix Forge** is the endgame refinement station. Where the [Affix Forge](affix-forge.md) rolls dice for you, the Exotic Forge lets you choose: pick an affix to refine or remove, pick an empty slot to fill, or reforge the whole item. Each operation costs millions of FE and several {{ item('rngtech:exotic_affix_catalyst', 'Exotic Affix Catalysts') }}, and every operation on the same item makes the next one more expensive.

The Exotic Forge refines items only. Placed machines still use their own Refinement tab.

## Obtaining

### Crafting

{{ crafting() }}

Neither the forge nor the catalyst rolls rarity or affixes. Both recipes need the Nullite material to be enabled.

### Breaking

Mine a placed Exotic Affix Forge with a pickaxe. It drops itself and everything in its slots, including the Battery Cell.

## Usage

### Running an operation

1. Supply FE to any side of the forge.
2. Put a refinable item in the **target** slot and Exotic Affix Catalysts in the **catalyst** slot. The forge accepts the same items as the [Affix Forge](affix-forge.md#how-refinement-works): machine items, machine parts, Battery Cells, and other identified, non-Unique gear.
3. Choose an operation. Operations that work on one affix or one empty slot also need you to select it.
4. Click **Apply**. The forge does not start on its own.

While the operation runs, the operation and selection controls are locked. When it finishes, the forge uses up the target and catalysts, spends the item's [Refinement Potential](rarity-and-affixes.md#refinement-potential), and puts the refined item in the **output** slot. Empty the output slot before the next operation.

### Power failure

The forge draws FE every tick while it works. If it cannot draw enough, the power-failure meter fills. When the meter is full, the operation fails with an audible cue, progress resets, and the button changes to **Retry**.

A failure keeps your item, catalysts, and RP. Only the FE already spent is lost. Make sure your supply, plus the internal buffer and Battery Cell, can carry the whole operation.

### Operations

| Operation | Effect | Selection |
|---|---|---|
| Refine All | Rerolls the value of every affix within its current tier. | None |
| Upgrade Random | Upgrades one random affix, like an Affix Upgrader. | None |
| Refine Selected | Rerolls the value of one affix within its current tier. | One affix |
| Remove Selected | Removes one rolled affix. Fixed identity traits cannot be removed. | One affix |
| Add Modifier | Adds a legal affix to an empty prefix or suffix slot. | One empty slot |
| Reforge | Rerolls rarity, Refinement Potential, and affixes as if the item were newly crafted. Stored energy and the item's Exotic Forge history are kept. | None |

The forge refuses to start, and uses nothing, if the item cannot be refined, has too little RP, lacks a selection, or the operation is not legal for it.

### Rising costs

The forge records how many times it has worked on each item, both in total and per operation. Every successful operation raises the FE and catalyst cost of the next one on that item, up to 16 catalysts per operation. A fresh item costs the base amounts shown in the table below, unless the server config scales FE or time.

### Gear

The Gear tab has one optional **Battery Cell** slot. Any Battery Cell fits. The forge also has a 2,000,000 FE internal buffer and accepts up to 65,536 FE/t. A large cell lets a long operation keep running when your supply cannot match the draw.

### Automation

| Side | Behavior |
|---|---|
| Any | Accepts FE, for example from a [Universal Cable](universal-cable.md). |

The forge has no item automation.

### Exotic affix forge recipes

The forge uses the `rngtech:exotic_affix_forge` recipe type. Each recipe is one operation, listed with its base catalyst count, FE, time, and fixed RP cost. Upgrade Random and Add Modifier also roll the normal Affix Upgrader and Affix Injector RP costs.

{{ processing("exotic_affix_forge", columns=["action", "catalyst_count", "energy", "processing_ticks", "refinement_potential_cost"]) }}

## Screen

The Exotic Affix Forge screen has three working tabs:

- **Process**: target, catalyst, and output slots, the affix and empty-slot selection panel, operation buttons, Apply/Retry, and energy, power-failure, progress, and cost readouts. With JEI installed, click the recipe area to open the Exotic Affix Forging category.
- **Gear**: the Battery Cell slot.
- **Stats**: the forge's current stats.

## Data values

{{ data_values() }}

## See also

- [Affix Forge](affix-forge.md): the early catalyst-based station and how refinement works.
- [Rarity and Affixes](rarity-and-affixes.md)

{{ navbox() }}
