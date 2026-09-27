# Getting Started

## Install

Use Minecraft **1.21.1**, **NeoForge 21.1.228 or newer for 1.21.1**, and **Java 21**. Download RNGTech from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/rngtech) or [GitHub Releases](https://github.com/c-pettersson/rngtech/releases), and place the mod JAR in the instance's `mods` directory. Install it on both the client and server when playing multiplayer.

JEI provides recipe and Gear lookups. Jade provides machine overlays. Both are optional, as are the AE2 and Refined Storage bridge integrations. The development client includes more optional mods than a standalone installation needs.

The optional FTB Quests extra is for pack authors. Copy its `config/ftbquests` directory into the instance root and install FTB Quests with its dependencies. Review rewards and progression before including it in a pack.

## First tools and machines

1. Craft a Tool Bench, a Flint Pick Head, and a Wooden Tool Rod. Assemble the parts in the Tool Bench. Picks, shovels, and axes do not require a Tiny Anvil; the Hammer, Digger, and Treefeller families do.
2. Identify rolled equipment before using or refining it. Craft an unidentified stack by itself and take the result to reveal its traits. Crafting previews deliberately keep the roll hidden.
3. Use [modular tools](content/modular-field-tools.md) to collect early materials. Repair a tool with its matching head material at the Tool Bench; a tool at zero durability stays as a broken stack.
4. Build a [Crusher](content/crusher.md) chassis and install a compatible Crush Head in its Gear tab. The head supplies processing hardness. An under-level head incurs time, energy, and jam penalties. A missing Battery Cell reduces output.
5. Add a power source and connect it with [Universal Cable and Energy Connectors](content/basic-wire.md). A solid-fuel burner requires Heat Core, Battery Cell, and Fuel Box Gear before it can operate. Check connector direction and channel when power does not move.
6. Follow recipe requirements into [metal forming](content/metal-press.md), [alloying](content/alloy-furnace.md), and [calibration](content/resonance-calibrator.md). A material stage alone does not guarantee that a machine has the required Gear or temperature.

Use JEI for exact ingredients and alternate recipes. Datapacks and configuration can change them. [Component Stages](reference/component-stages.md) explains the stage ladder; [Current Implementation](reference/current-implementation.md) lists machine-specific requirements.

## Read the machine screen

The Process tab shows work and stored resources. The Gear tab holds installed components. Hover status icons, meters, and slots for missing requirements or blocked output. Stats shows effective values; Refinement changes eligible machine affixes within the remaining Refinement Potential budget. Available tabs vary by machine.

Normal automation generally accesses process inventories, not protected Gear. Battery Cells and other exceptions depend on the machine. See the machine's content page before designing automated loading.

## Scope and updates

Many features are prototypes and may change between releases. Planned designs are not available merely because they have documentation pages. Back up worlds before updating, and check the release notes for the version you install. Automated checks cover compilation, resources, and selected source/data rules; they do not prove that every multiplayer or mod interaction works.
