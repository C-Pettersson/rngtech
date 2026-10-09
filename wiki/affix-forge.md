---
wiki:
  category: Refinement
  icon: rngtech:affix_forge
  ids:
    - rngtech:affix_forge
    - rngtech:affix_lens_array
    - rngtech:affix_modifier_socket
    - rngtech:affix_resonance_matrix
    - rngtech:affix_injector
    - rngtech:affix_modifier
    - rngtech:affix_upgrade
    - rngtech:ascension_catalyst
    - rngtech:ascension_matrix
    - rngtech:nullifier_coil
    - rngtech:chaos_crystal
    - rngtech:expansion_crystal
    - rngtech:null_crystal
    - rngtech:power_modifier_lens
    - rngtech:speed_modifier_lens
    - rngtech:yield_modifier_lens
    - rngtech:stability_modifier_lens
    - rngtech:control_modifier_lens
    - rngtech:kinetic_modifier_lens
    - rngtech:efficiency_modifier_lens
    - rngtech:conservation_crystal
    - rngtech:frugality_crystal
    - rngtech:transmutation_crystal
    - rngtech:resonance_crystal
    - rngtech:destabilization_crystal
    - rngtech:stabilization_crystal
---

# Affix Forge

{{ infobox(
    variants=["rngtech:affix_forge"],
    fields={
        "Type": "Refinement station",
        "Power": "None",
        "Gear": "{{ item('rngtech:affix_lens_array') }}, {{ item('rngtech:affix_modifier_socket') }}, {{ item('rngtech:affix_resonance_matrix') }}",
        "Mastery": "No",
    },
) }}

The **Affix Forge** is RNGTech's early refinement station. Put in a machine item, machine part, or Battery Cell, add a catalyst, and the forge adds, upgrades, rerolls, or removes that item's rolled affixes. It needs no power. Three reusable upgrades unlock stronger catalysts and the optional focus slot.

The forge refines items, not placed machines. To refine a machine that is already placed, use the Refinement tab in that machine's own screen. For late-game powered refinement, see the [Exotic Affix Forge](exotic-affix-forge.md).

## Obtaining

### Crafting

{{ crafting("rngtech:affix_forge") }}

The forge does not roll rarity or affixes itself. Its recipe needs the Copper material to be enabled.

### Breaking

Mine a placed Affix Forge with a pickaxe. It drops itself and everything inside it, including installed upgrades.

## Usage

### How refinement works

Every refinable item has a [rarity](rarity-and-affixes.md) and a budget of [Refinement Potential](rarity-and-affixes.md#refinement-potential) (RP):

- **Rarity** sets how many affixes the item can hold. Normal items have none, Magic items hold one prefix and one suffix, and Rare items hold up to three of each.
- **RP** is spent by every successful operation. When an item runs out, you cannot refine it further, so plan where you spend it.

A failed or illegal operation costs nothing: no catalyst and no RP. Identify an item before you refine it (see [Getting Started](getting-started.md)). Unique items, unidentified items, and recycled (stripped) items cannot be refined.

Affix and tier odds are weighted, not guaranteed. Tier 7 affixes can appear only on Stage 8 Exotic items. See [Rarity and Affixes](rarity-and-affixes.md#affix-tiers) for tiers and weights.

### Using the forge

1. Put the item you want to refine in the **target** slot. The RP readout on the left shows its remaining budget.
2. Put a catalyst in the **catalyst** slot. The outcome panel previews what it will do and its RP cost range.
3. Optionally, put a lens or modifier crystal in the **focus** slot.
4. Click **Reforge**. On success, the item changes in place and the catalyst is used up.

The modifier list shows the item's current affixes. You cannot pick which affix to add or upgrade; the forge rolls it for you. Shift-click moves items into the right slot.

### Catalysts

| Catalyst | Effect | RP cost | Needs |
|---|---|---|---|
| {{ item('rngtech:affix_injector') }} | Adds one affix. Normal items become Magic. Magic items can only fill their missing prefix or suffix. Rare items can keep adding until full. | 1–8 | — |
| {{ item('rngtech:affix_modifier') }} | Upgrades one random affix. | 2–6 | — |
| {{ item('rngtech:nullifier_coil') }} | Removes one random rolled affix. | 1–4 | — |
| {{ item('rngtech:affix_upgrade') }} | Upgrades one random affix, or retunes it if it is already at the top tier. | 6–10 | Resonance Matrix |
| {{ item('rngtech:ascension_catalyst') }} | Makes a Magic item Rare and adds one affix. The item needs at least 5 RP. | 4 | Resonance Matrix |
| {{ item('rngtech:ascension_matrix') }} | Makes a Magic item with at least one affix Rare, adds 1–4 affixes, and upgrades one existing affix. | All remaining | Resonance Matrix |
| {{ item('rngtech:chaos_crystal') }} | Rerolls all of the item's current affixes. | 1–4 | Resonance Matrix |
| {{ item('rngtech:expansion_crystal') }} | Tries to fill every open affix slot. | 1–18 per affix | Resonance Matrix |
| {{ item('rngtech:null_crystal') }} | Removes one random rolled affix. | 1–4 | Resonance Matrix |

Stronger affixes tend to cost more RP. The final cost is capped by what the item has left, so a low-RP item can still gamble, but may spend everything. A placed machine's Refinement tab works the same way, with one catalyst slot and no focus slot.

Removing affixes never touches an item's fixed identity traits or base stats. Only rolled affixes can be added or removed.

### Upgrades

The forge has three upgrade slots. Upgrades are reusable: they stay installed and are never used up. Each one also changes how the placed forge looks.

| Upgrade | Unlocks |
|---|---|
| {{ item('rngtech:affix_lens_array') }} | Modifier lenses in the focus slot. |
| {{ item('rngtech:affix_modifier_socket') }} | Modifier crystals in the focus slot. |
| {{ item('rngtech:affix_resonance_matrix') }} | Greater Affix Upgrader, Ascension Catalyst, Ascension Matrix, Chaos Crystal, Expansion Crystal, and Null Crystal. |

Without an upgrade, the forge still runs the Affix Injector, Affix Upgrader, and Nullifier Coil. If a catalyst or focus needs a missing upgrade, the outcome panel names it and Reforge refuses without using anything.

### Modifier lenses

With an Affix Lens Array installed, a lens in the focus slot makes affixes from its family three times as likely to be picked. Lenses work with the Affix Injector, Affix Upgrader, and Greater Affix Upgrader.

A lens is a nudge, not a filter: other affixes can still roll. If nothing on the item can match the lens, Reforge is blocked and nothing is used. On success, both the catalyst and the lens are used up. Hold Shift on a lens to see which affixes it favors.

| Lens | Favors |
|---|---|
| {{ item('rngtech:power_modifier_lens') }} | Power affixes |
| {{ item('rngtech:speed_modifier_lens') }} | Speed affixes |
| {{ item('rngtech:yield_modifier_lens') }} | Yield affixes |
| {{ item('rngtech:stability_modifier_lens') }} | Stability affixes |
| {{ item('rngtech:control_modifier_lens') }} | Control affixes |
| {{ item('rngtech:kinetic_modifier_lens') }} | Speed and motion affixes |
| {{ item('rngtech:efficiency_modifier_lens') }} | Efficiency affixes |

### Modifier crystals

With an Affix Modifier Socket installed, a crystal in the focus slot changes how certain catalysts behave. You cannot use a lens and a crystal together.

| Crystal | Works with | Effect |
|---|---|---|
| {{ item('rngtech:conservation_crystal') }} | Adds and upgrades | 25% chance to keep the catalyst. The crystal is used up. |
| {{ item('rngtech:frugality_crystal') }} | Affix Upgrader | 25% chance the upgrade costs only 1 RP. The crystal is used up. |
| {{ item('rngtech:transmutation_crystal') }} | Greater Affix Upgrader | Swaps the upgraded affix for a different one in the same prefix or suffix slot. |
| {{ item('rngtech:resonance_crystal') }} | Greater Affix Upgrader | Keeps the upgraded affix's roll quality in its new tier. |
| {{ item('rngtech:destabilization_crystal') }} | Greater Affix Upgrader | Rerolls the item's other affixes after the upgrade. |
| {{ item('rngtech:stabilization_crystal') }} | — | Has no effect and is not used up. |

### Automation

The Affix Forge has no item or energy automation. Everything goes through its screen.

### Crafting the consumables

Catalysts:

{{ crafting("rngtech:affix_injector", "rngtech:affix_modifier", "rngtech:affix_upgrade", "rngtech:ascension_catalyst", "rngtech:ascension_matrix", "rngtech:nullifier_coil", "rngtech:chaos_crystal", "rngtech:expansion_crystal", "rngtech:null_crystal") }}

Upgrades. The Resonance Matrix needs calibrated components from a [Resonance Calibrator](resonance-calibrator.md):

{{ crafting("rngtech:affix_lens_array", "rngtech:affix_modifier_socket") }}

{{ crafting("rngtech:affix_resonance_matrix") }}

Lenses:

{{ crafting("rngtech:power_modifier_lens", "rngtech:speed_modifier_lens", "rngtech:yield_modifier_lens", "rngtech:stability_modifier_lens", "rngtech:control_modifier_lens", "rngtech:kinetic_modifier_lens", "rngtech:efficiency_modifier_lens") }}

Modifier crystals:

{{ crafting("rngtech:conservation_crystal", "rngtech:frugality_crystal", "rngtech:transmutation_crystal", "rngtech:resonance_crystal", "rngtech:destabilization_crystal", "rngtech:stabilization_crystal") }}

## Screen

The Affix Forge has a single screen with the target, catalyst, and focus slots, the three upgrade slots, the RP readout, the current-affix list, the outcome preview, and the Reforge button. Hover the RP readout for the exact value, and a lens preview for the matching candidates.

With JEI installed, the catalysts are listed under the Affix Forging category.

## Data values

{{ data_values() }}

## See also

- [Rarity and Affixes](rarity-and-affixes.md): rarity tiers, affixes, and Refinement Potential.
- [Exotic Affix Forge](exotic-affix-forge.md)

{{ navbox() }}
