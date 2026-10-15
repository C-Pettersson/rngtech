---
navigation:
  title: "Automation"
  parent: guides.md
  icon: rngtech:basic_item_connector
categories:
  - "Guides"
---

# Automation

Autocrafting in RNGTech means factory chains. Hoppers, [Universal Cables](rngtech:universal-cable.md), or another mod's pipes feed each machine, and each machine's output feeds the next.

## Faces

Inputs usually go in the top and outputs come out the bottom. Each machine page lists its faces under **Automation**. Automation cannot reach installed Gear, except the Battery Cell slot many machines open on a side.

Recipe machines reject items none of their recipes use.

## Recipes with several ingredients

An arriving ingredient never takes a slot another ingredient of the recipe still needs. The machine decides from what its slots hold, so arrival order does not matter, and all the ingredients can share one cable or hopper.

- [Any order](rngtech:automation/any-order.md)

### Fixed order

The [Component Assembler](rngtech:component-assembler.md) needs each ingredient in its own position. A slot accepts an item only if a recipe that matches the other filled slots needs it there. With Redstone in the first slot for a Servo, the second slot takes the Servo's ingots and refuses a Battery Cell's plate.

When a recipe needs one item in two positions, such as an Anode's two plates, automation fills both slots evenly, so the first craft starts after two plates.

### One job per slot

The [Resonance Calibrator](rngtech:resonance-calibrator.md) and [Corrosion Cell](rngtech:corrosion-cell.md) give each slot a single role. The [Furnace](rngtech:furnace.md), [Crusher](rngtech:crusher.md), and [Metal Press](rngtech:metal-press.md) run one-item recipes. Nothing on these machines competes for a slot.

## Smelting lanes

A Furnace with several smelting lanes, such as the Lead Furnace's four, fills them in order: lane 1 takes a full stack before lane 2 gets anything. A slow feed keeps one lane busy while the rest sit idle. The **Even Feed** keystone on the [Machine Mastery](rngtech:machine-mastery.md) tree sends each arriving item to the emptiest lane that can hold it instead, so a trickle or a full stack from a cable spreads across every lane. See [Furnace](rngtech:furnace/automation.md).

## Shift-click and hand placement

Shift-click into a Melter, Alloy Furnace, or Component Assembler follows the automation rules. Hand placement does not. An item placed by hand that the recipe does not use stops the machine until you take it out.

## More

- [Ingredient leftovers](rngtech:automation/ingredient-leftovers.md)
- [Building a chain](rngtech:automation/building-a-chain.md)
