---
wiki:
  category: Refinement
  icon: rngtech:affix_sorter
  ids:
    - rngtech:affix_sorter
    - rngtech:exotic_rule_core
    - rngtech:rule_card
---

# Affix Sorter

{{ infobox(
    variants=[["Affix Sorter", "rngtech:affix_sorter"]],
    fields={
        "Type": "Logistics machine",
        "Stages": "8",
        "Power": "FE",
        "Gear": "{{ item('rngtech:exotic_rule_core', 'Rule Core') }}, {{ item('rngtech:exotic_servo', 'Servo') }}, {{ item('rngtech:exotic_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Affix Sorter** reads the rolls of identified items and sends each item to one of two routes, **Pass** or **Reject**. You write the rules. Use it after an [Identifier](identifier.md) to keep the Gear you want from an automated crafting line, and send the rest to a [Potential Reactor](potential-reactor.md) or a [Component Recycler](component-recycler.md).

The sorter never changes an item. It only reads it.

## Obtaining

### Crafting

The Affix Sorter and the Rule Core come out Unidentified and roll as Stage 8 items. The Rule Card does not roll.

{{ crafting() }}

### Breaking

Mine the Affix Sorter with a pickaxe. It keeps its traits and its rules. The items in its slots drop.

## Usage

### Rules

Open the **Rules** tab to write rules. A rule has a route, a set of conditions, and an on/off switch. An item matches a rule when it meets every condition that the rule sets. A condition that you do not set matches every item.

The sorter checks the rules from the top. The first enabled rule that matches decides the route. An item that matches no rule takes the default route. The default route is Reject. Click the default route under the list to change it.

| Condition | Matches |
|---|---|
| Items | Up to six items. Click **+** to add the item in the input slot. Click an item to remove it. |
| Tag | One item tag. Click to cycle through the tags of the rule's items. |
| Stage | A range of item stages. |
| Rarity | Any of the selected rarities. |
| Affixes | Up to six affixes, each at or above a tier. Set how many of them the item must have. |
| Quality | The item's Quality, the sum of its affix tiers, at or above a value. |
| RP | Refinement Potential at or above a value. |
| Roll % | A Unique's roll quality at or above a value. |
| Corrupted, Mirrored | Either state, only items in the state, or only items outside it. |

Put a Reject rule above a Pass rule to make an exception. For example, reject Corrupted items first, then pass Rare items.

Turn on **Pulse** to make a rule send a short redstone pulse when it decides an item. The pulse comes out of every face that is not a Pass or Reject face. Use it to stop your crafting line when you get the item you want.

### Rule Capacity

The installed Rule Core lets the sorter run 8 enabled rules. Rule Capacity rolls on the sorter and on the Rule Core can add up to 12 more. The list can hold 32 rules. You can keep extra rules switched off and turn them on later. The sorter does not let you enable a rule past its Rule Capacity.

### Energy

Each item costs FE. The cost grows faster than the number of enabled rules: 4 kFE times the number of enabled rules to the power of 1.5.

| Enabled rules | FE per item |
|---:|---:|
| 1 | 4 kFE |
| 5 | 44.7 kFE |
| 10 | 126 kFE |
| 20 | 358 kFE |

Disabled rules cost nothing. The bottom of the Rules tab shows the cost per item now. Hover it to see the cost with one more rule. Each item takes 1 second at base speed.

### Routes and faces

Each face of the sorter is an **input**, a **Pass** face, or a **Reject** face. Click a face on the Process tab to change it. By default, the left face is Pass and the right face is Reject, as seen from the front.

The collar around the port on each face shows its setting when a cable connector, a hopper, or a container touches that face. A green collar is a Pass face. A red collar is a Reject face. A bare collar is an input face or a face with nothing attached. The collars of a route flash when an item goes into its buffer.

- Cables and hoppers insert items through input faces.
- Cables and hoppers extract items through Pass and Reject faces. The sorter never pushes items out.
- Each route has three buffer slots. When the buffer for an item is full, the sorter stops. It never reroutes or voids an item.
- The sorter accepts only identified items.

### Rule Card

A {{ item('rngtech:rule_card') }} copies a rule list from one sorter to another.

- Sneak-use the card on a sorter to copy its rules. A use with an empty card also copies.
- Use the card on another sorter to write the rules. This replaces the whole list.
- Rules past the sorter's Rule Capacity arrive switched off.

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Rule Core | Yes | The {{ item('rngtech:exotic_rule_core') }}. It sets the base Rule Capacity. |
| Servo | No | Any Servo, for speed. |
| Battery Cell | No | Any Battery Cell. Without one, the sorter runs 20% slower. |

## Screen

The Affix Sorter has five tabs:

- **Process**: the input slot, the Pass and Reject buffers, the face settings, the energy bar, and progress. The route stripe beside each buffer lights for the last route.
- **Gear**: the Battery Cell, Rule Core, and Servo slots.
- **Stats**: Processing Speed, Energy Use, Energy Capacity, and Rule Capacity.
- **Refinement**: refine the placed sorter with a catalyst.
- **Rules**: the rule list on the left and the selected rule on the right. A mark at the end of a row shows the rule that decided the last item.

## Data values

{{ data_values() }}

## See also

- [Identifier](identifier.md)
- [Rarity and Affixes](rarity-and-affixes.md)
- {{ item('rngtech:advanced_item_filter') }}, the cable filter for identity, rarity, stage, and Stability

{{ navbox() }}
