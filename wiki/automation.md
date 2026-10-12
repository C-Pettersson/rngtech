---
wiki:
  category: Guides
  icon: rngtech:basic_item_connector
---

# Automation

Autocrafting in RNGTech means factory chains. Hoppers, [Universal Cables](universal-cable.md), or another mod's pipes feed each machine, and each machine's output feeds the next.

## Faces

Inputs usually go in the top and outputs come out the bottom. Each machine page lists its faces under **Automation**. Automation cannot reach installed Gear, except the Battery Cell slot many machines open on a side.

Recipe machines reject items none of their recipes use.

Some ingredients leave an item behind, such as the Glass Bottle from a {{ item('minecraft:honey_bottle') }}. The [Melter](melter.md#melting) sends leftovers it cannot use into the inventory below it, or drops them if there is none, so they never block its input slots.

## Recipes with several ingredients

An arriving ingredient never takes a slot another ingredient of the recipe still needs. The machine decides from what its slots hold, so arrival order does not matter, and all the ingredients can share one cable or hopper.

### Any order

The [Melter](melter.md) and [Alloy Furnace](alloy-furnace.md) ignore slot order. A recipe that needs two of an item takes both from one slot. An item gets a second slot only if every recipe it could still make keeps an empty slot for each missing ingredient.

- A Melter fed only {{ item('minecraft:sugar') }} fills one slot and keeps the other for the {{ item('minecraft:redstone') }} in Electrolyte Solution.
- A Melter fed only {{ item('minecraft:ender_pearl') }} keeps one slot for {{ item('rngtech:collapse_residue') }}. Ender Slurry from pearls alone takes both pearls from that one slot.
- A three-slot Alloy Furnace fed only {{ item('minecraft:copper_ingot') }} keeps two slots for Bronze's tin and charcoal.
- Two Lubricant bases cannot fill both Melter slots, because the additive needs one.

An ingredient stacks in its own slot up to a full stack. To keep ingredients within one craft of each other, take the Alloy Furnace's **Recipe Lock** in the [Metallurgist](alloy-furnace.md#metallurgist) ascendancy.

### Fixed order

The [Component Assembler](component-assembler.md) needs each ingredient in its own position. A slot accepts an item only if a recipe that matches the other filled slots needs it there. With Redstone in the first slot for a Servo, the second slot takes the Servo's ingots and refuses a Battery Cell's plate.

When a recipe needs one item in two positions, such as an Anode's two plates, automation fills both slots evenly, so the first craft starts after two plates.

### One job per slot

The [Resonance Calibrator](resonance-calibrator.md) and [Corrosion Cell](corrosion-cell.md) give each slot a single role. The [Furnace](furnace.md), [Crusher](crusher.md), and [Metal Press](metal-press.md) run one-item recipes. Nothing on these machines competes for a slot.

## Smelting lanes

A Furnace with several smelting lanes, such as the Lead Furnace's four, fills them in order: lane 1 takes a full stack before lane 2 gets anything. A slow feed keeps one lane busy while the rest sit idle. The **Even Feed** keystone on the [Machine Mastery](machine-mastery.md) tree sends each arriving item to the emptiest lane that can hold it instead, so a trickle or a full stack from a cable spreads across every lane. See [Furnace](furnace.md#automation).

## Building a chain

- Give each recipe its own machine. When an item fits several recipes, the next matching ingredient to arrive picks the recipe. Use Item Connector filter rows or an {{ item('rngtech:advanced_item_filter') }} to keep other recipes' items out.
- Pull outputs. A full output slot or tank pauses the machine.
- Size connectors to the machine. An Item Connector's tier sets how many items it moves and how often; see [Universal Cable](universal-cable.md#items).
- Pipe fluids with Fluid Connectors. They fill input tanks directly.

Example: a [Metal Press](metal-press.md) with a {{ item('rngtech:plate_mold') }} presses ingots into plates. A cable carries the plates to a Component Assembler, and a Melter pipes in Electrolyte Solution. The Assembler makes Anodes for a [Corrosion Cell](corrosion-cell.md).

## Shift-click and hand placement

Shift-click into a Melter, Alloy Furnace, or Component Assembler follows the automation rules. Hand placement does not. An item placed by hand that the recipe does not use stops the machine until you take it out.

## See also

- [Universal Cable](universal-cable.md)
- [Getting Started](getting-started.md#logistics-basics)

{{ navbox() }}
