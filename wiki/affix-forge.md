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
    - rngtech:volatile_catalyst
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

The **Affix Forge** is RNGTech's early refinement station. Put in a machine item, machine part, Battery Cell, Tool Head, Tool Rod, or companion item, add a catalyst, and the forge adds, upgrades, rerolls, or removes that item's rolled affixes. It needs no power. Three reusable upgrades unlock stronger catalysts and the optional focus slot.

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

A failed or illegal operation costs nothing: no catalyst and no RP. Identify an item before you refine it (see [Getting Started](getting-started.md)). Unidentified items, recycled (stripped) items, and [Corrupted](rarity-and-affixes.md#corruption) items cannot be refined. Unique items accept only a Volatile Catalyst.

Affix and tier odds are weighted, not guaranteed. Tier 7 affixes can appear only on Stage 8 Exotic items, and only when an affix is added or rerolled. Upgrades and the Ascension catalysts stop at Tier 6. See [Rarity and Affixes](rarity-and-affixes.md#affix-tiers) for tiers and weights.

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
| {{ item('rngtech:affix_modifier') }} | Upgrades one random affix by one tier. Fails if every affix is already at its top tier. | 2–6 | — |
| {{ item('rngtech:nullifier_coil') }} | Removes one random rolled affix. | 1–4 | — |
| {{ item('rngtech:affix_upgrade') }} | Upgrades one random affix. If every affix is already at its top tier, it retunes one instead. | 6–10 | Resonance Matrix |
| {{ item('rngtech:ascension_catalyst') }} | Makes a Magic item Rare and adds one affix. The item needs at least 5 RP. | 4 | Resonance Matrix |
| {{ item('rngtech:ascension_matrix') }} | Makes a Magic item with at least one affix Rare, adds 1–4 affixes (usually 1), and upgrades or retunes one existing affix. | All remaining | Resonance Matrix |
| {{ item('rngtech:chaos_crystal') }} | Rerolls all of the item's current affixes. Rarity and the number of prefixes and suffixes stay the same. | 1–4 | Resonance Matrix |
| {{ item('rngtech:expansion_crystal') }} | Adds affixes until every open slot is full or the RP runs out. A Normal item becomes Magic. | 1–18 per affix | Resonance Matrix |
| {{ item('rngtech:null_crystal') }} | Removes one random rolled affix. | 1–4 | Resonance Matrix |
| {{ item('rngtech:volatile_catalyst') }} | [Corrupts](rarity-and-affixes.md#corruption) a machine part or Battery Cell, Uniques included. The item can never be refined again. | None | Resonance Matrix |

Stronger affixes cost more RP. Each tier above Tier 1 raises the lowest possible cost by 3 RP, up to the catalyst's maximum. For example, an Affix Injector that adds a Tier 3 affix costs 7–8 RP. The final cost is capped by what the item has left, so a low-RP item can still gamble, but may spend everything. The Affix Upgrader needs at least 2 RP to start, and the Greater Affix Upgrader at least 6.

Removing or rerolling is cheap. The Nullifier Coil, Chaos Crystal, and Null Crystal cost 1 RP half the time, 2 RP 30% of the time, 3 RP 15% of the time, and 4 RP 5% of the time.

A placed machine's Refinement tab works the same way, with one catalyst slot and no focus slot. It needs no forge upgrades: every catalyst works there.

Removing affixes never touches an item's fixed identity traits or base stats. Only rolled affixes can be added or removed.

### Upgrades

The forge has three upgrade slots. Upgrades are reusable: they stay installed and are never used up. Each one also changes how the placed forge looks.

| Upgrade | Unlocks |
|---|---|
| {{ item('rngtech:affix_lens_array') }} | Modifier lenses in the focus slot. |
| {{ item('rngtech:affix_modifier_socket') }} | Modifier crystals in the focus slot. |
| {{ item('rngtech:affix_resonance_matrix') }} | Greater Affix Upgrader, Ascension Catalyst, Ascension Matrix, Chaos Crystal, Expansion Crystal, Null Crystal, and Volatile Catalyst. |

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
| {{ item('rngtech:conservation_crystal') }} | Affix Injector, Affix Upgrader, Greater Affix Upgrader | 25% chance to keep the catalyst. The crystal is used up. |
| {{ item('rngtech:frugality_crystal') }} | Affix Upgrader | 25% chance the upgrade costs only 1 RP. The crystal is used up. |
| {{ item('rngtech:transmutation_crystal') }} | Greater Affix Upgrader | Swaps the upgraded affix for a different one in the same prefix or suffix slot. |
| {{ item('rngtech:resonance_crystal') }} | Greater Affix Upgrader | Keeps the upgraded affix's roll quality in its new tier. |
| {{ item('rngtech:destabilization_crystal') }} | Greater Affix Upgrader | Rerolls the item's other affixes, at their current tiers, after the upgrade. |
| {{ item('rngtech:stabilization_crystal') }} | Volatile Catalyst | Removes the Blighted outcome; its chance goes to Untouched. |

Every crystal is used up on success. A crystal paired with a catalyst it does not work with blocks Reforge, and nothing is used.

### Corrupting with a Volatile Catalyst

With a Resonance Matrix installed, a {{ item('rngtech:volatile_catalyst') }} in the catalyst slot turns the outcome panel into the [corruption](rarity-and-affixes.md#corruption) table for the item in the target slot: the chance of each outcome, with any that cannot happen folded into Untouched. Hover Blessed or Blighted to list every implicit the item can gain, with its odds. A Stabilization Crystal in the focus slot shows Blighted as removed.

Click **Reforge** to roll. The catalyst is always used up, and so is a Stabilization Crystal. No RP is spent, and the RP readout shows Corrupted from then on.

A placed machine's Refinement tab also accepts the catalyst, without a focus slot. Hover its **Apply** button with a catalyst inserted to see the same outcome table.

### Automation

The Affix Forge has no item or energy automation. Everything goes through its screen.

### Crafting the consumables

Catalysts:

{{ crafting("rngtech:affix_injector", "rngtech:affix_modifier", "rngtech:affix_upgrade", "rngtech:ascension_catalyst", "rngtech:ascension_matrix", "rngtech:nullifier_coil", "rngtech:chaos_crystal", "rngtech:expansion_crystal", "rngtech:null_crystal") }}

The Volatile Catalyst asks you to make three machines go wrong on purpose:

- a Stage 5 Calibrated Kinetic Component with stability between 20% and 35%. A well-built [Resonance Calibrator](resonance-calibrator.md) overshoots that band, so mistune it: a weaker Control Board, no Stabilizer Matrix, or no Battery Cell;
- two Malformed Ingots of a Stage 5 or higher material, from failed [Metal Press](metal-press.md) or [Alloy Furnace](alloy-furnace.md) cycles;
- two {{ item('rngtech:jam_debris') }} from [Crusher](crusher.md#jam-debris) jams.

{{ crafting("rngtech:volatile_catalyst") }}

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
