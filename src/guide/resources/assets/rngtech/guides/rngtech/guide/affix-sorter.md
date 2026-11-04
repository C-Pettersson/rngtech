---
navigation:
  title: "Affix Sorter"
  parent: refinement.md
  icon: rngtech:affix_sorter
categories:
  - "Refinement"
item_ids:
  - rngtech:affix_sorter
  - rngtech:exotic_rule_core
  - rngtech:rule_card
---

# Affix Sorter

<BlockImage id="rngtech:affix_sorter" scale="6" />

- **Type:** Logistics machine
- **Stages:** 8
- **Power:** FE
- **Gear:** <ItemLink id="rngtech:exotic_rule_core" />, <ItemLink id="rngtech:exotic_servo" />, <ItemLink id="rngtech:exotic_battery_cell" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

The **Affix Sorter** reads the rolls of identified items and sends each item to one of two routes, **Pass** or **Reject**. You write the rules. Use it after an [Identifier](rngtech:identifier.md) to keep the Gear you want from an automated crafting line, and send the rest to a [Potential Reactor](rngtech:potential-reactor.md) or a [Component Recycler](rngtech:component-recycler.md).

The sorter never changes an item. It only reads it.

## Usage

- [Rules](rngtech:affix-sorter/rules.md)

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

- [Routes and faces](rngtech:affix-sorter/routes-and-faces.md)

### Rule Card

A <ItemLink id="rngtech:rule_card" /> copies a rule list from one sorter to another.

- Sneak-use the card on a sorter to copy its rules. A use with an empty card also copies.
- Use the card on another sorter to write the rules. This replaces the whole list.
- Rules past the sorter's Rule Capacity arrive switched off.

### Gear

| Slot | Required | Accepts |
|---|---|---|
| Rule Core | Yes | The <ItemLink id="rngtech:exotic_rule_core" />. It sets the base Rule Capacity. |
| Servo | No | Any Servo, for speed. |
| Battery Cell | No | Any Battery Cell. Without one, the sorter runs 20% slower. |

## Screen

The Affix Sorter has five tabs:

- **Process**: the input slot, the Pass and Reject buffers, the face settings, the energy bar, and progress. The route stripe beside each buffer lights for the last route.
- **Gear**: the Battery Cell, Rule Core, and Servo slots.
- **Stats**: Processing Speed, Energy Use, Energy Capacity, and Rule Capacity.
- **Refinement**: refine the placed sorter with a catalyst.
- **Rules**: the rule list on the left and the selected rule on the right. A mark at the end of a row shows the rule that decided the last item.

## Obtaining

- [Crafting](rngtech:affix-sorter/crafting.md)

### Breaking

Mine the Affix Sorter with a pickaxe. It keeps its traits and its rules. The items in its slots drop.
