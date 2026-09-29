# Getting Started

RNGTech is a Minecraft 1.21.1 NeoForge technology mod that treats machines like RPG characters. A machine chassis sets base stats and limits, installed Gear supplies the real capability, and machines, parts, tools, and Battery Cells can roll rarity and affixes. Progress moves through [component stages](reference/component-stages.md) from flint and iron to steel, fluids, gas chemistry, and exotic materials.

This page follows the same order as the optional quest book that ships with the mod: a Start chapter for tools, four stage chapters, and side chapters for logistics and the Affix Forge. If your pack includes the quest extra, the book and this page agree on the route. Exact ingredients, temperatures, and amounts belong to JEI, because datapacks and configuration can change them.

## Install

Use Minecraft **1.21.1**, **NeoForge 21.1.228 or newer for 1.21.1**, and **Java 21**. Download RNGTech from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/rngtech) or [GitHub Releases](https://github.com/c-pettersson/rngtech/releases), and place the mod JAR in the instance's `mods` directory. Install it on both the client and server when playing multiplayer.

JEI provides recipe and Gear lookups. Jade provides machine overlays. Both are optional, as are the AE2 and Refined Storage bridge integrations. The development client includes more optional mods than a standalone installation needs.

The optional FTB Quests extra is for pack authors. Copy its `config/ftbquests` directory into the instance root and install FTB Quests with its dependencies. The mod JAR does not install a quest book on its own. Review rewards and progression before including the extra in a pack; [Quest Stage Coverage](reference/quest-stage-coverage.md) explains how to check it.

## How RNGTech works

Machines are built in layers. Each layer is a separate item with its own roll, so a weak machine is usually a Gear problem rather than a chassis problem.

- **Chassis** set base stats and cap how advanced the installed components can be. A Gear part's stage may not exceed the chassis stage, but lower-stage parts always fit, so mixed sets are normal.
- **Battery Cells** decide how much energy a machine can receive, store, and send. Most machines run worse without one. See [Battery Cells](content/battery-cells.md).
- **Processing Gear** is the machine's weapon: Crush Heads set the hardness a Crusher handles, Heat Cores set the temperature a furnace or press reaches, Molds pick what a press forms.
- **Control Gear** such as Servos, Fluid Pumps, and calibrated components adds stability, transfer, and extra behavior in later stages. See [Machine Parts](content/machine-parts.md).

Crafted machines, parts, tools, and cells come out **unidentified**. Craft the stack by itself, or place an unidentified machine block, to reveal its rarity and affixes. Placing identifies only the placed block; the rest of the stack stays unidentified. Crafting previews deliberately keep the roll hidden.

Each rolled item also carries [Refinement Potential](systems/progression.md#refinement-potential), the budget spent when you add, upgrade, reroll, or remove affixes. Upgrading a component to a later stage keeps its remaining budget and does not grant a new one, so good early rolls stay valuable. Machines additionally earn their own [Machine Mastery](systems/machine-mastery.md) XP from successful work and spend passive points in the Mastery tab.

The stage ladder groups into four bands. Each band adds a new rule to respect.

| Band | Stages | Goal | New pressure |
|---|---|---|---|
| Start and basic power | 0-2 | Modular tools, a Crusher, a solid-fuel burner, a stored-energy path | Install the Gear before blaming the machine |
| Steel factory | 3-4 | Alloys, pressed circuits, electric smelting, calibration, a steel machine park | Temperature targets, steady FE/t, and the first recoverable processing failures |
| Fluids and chemistry | 5-6 | Lubricant, gases, syngas and ammonia power, aluminum and titanium sets | Fluid handling and stricter recipe control |
| Advanced power | 7-8 | Tungstensteel and exotic lines, high-tier cells, large FE generators | Affix breakpoints on stability and heat, permanent infrastructure |

## Start: modular tools

You gather the materials, so the first chapter is about tools. Modular tools are assembled from parts, and every part keeps its own roll. Full details are on [Modular Field Tools](content/modular-field-tools.md).

1. Craft a **Tool Bench**. Its Build tab assembles a Tool Head and Tool Rod into a tool, swaps parts, repairs with the matching head material, and disassembles a tool without losing installed parts.
2. Craft a **Flint Pick Head** and a **Wooden Tool Rod**, then assemble a Modular Pick. Compare the head, rod, and tool tooltips before you replace anything.
3. Add Flint Shovel and Axe heads when you want them. Swap heads onto an existing rod instead of rebuilding the whole tool.
4. Install a **Tiny Anvil** in the Tool Bench Gear tab. It is reusable and unlocks the heavy families: Hammers break a 3x3 area, Treefellers clear connected logs, and Diggers cover bulk soft blocks.
5. Craft a **Forming Hammer** and hammer iron and copper ingots into plates. Plates and gears open iron and copper heads, which are harder and faster than flint. Copper heads are quicker than iron heads but wear out sooner.
6. Set up a **Crude Recycler** with a **Hand Crank** on top. It is the manual way to get value back from spare parts and bad rolls before powered recycling exists.

The optional **Miner's Companion** chews unwanted blocks, holds a lamp, and pulls in drops. Every action draws FE from its installed Battery Cell, and chewing also needs a Crush Head. See [Miner's Companion](content/miners-companion.md).

RNGTech ores use an ore-hardness gate separate from vanilla mining tiers. A Modular Pick or Hammer needs enough reach for the ore band, and a Hammer skips over-hard blocks instead of breaking them.

| Hardness | Heads that reach it | Ores in the band |
|---:|---|---|
| 1 | Flint, Iron | Iron, Copper |
| 2 | Copper, Bronze | Tin, Zinc, Gold |
| 3 | Steel | Nickel, Lead, Silver |
| 4 | Steel with a Diamond Tip | Aluminum, Osmium |
| 5 | Aluminum | Titanium |
| 6 | Titanium | Tungsten, Platinum |
| 7 | Tungstensteel, Nullite | No default ore |
| 8 | Exotic | Naquadah |

Aluminum is the first material after the Diamond Tip and acts as the Stage 5 foothold. Packs can change the vanilla pickaxe bridge and the ore list.

## Stage 1-2: basic power

The first factory goal is ore multiplication. Build in this order and power moves without cables.

1. Craft a **Machine Frame** and an **Iron Battery Cell**. Cells are Gear, install them in the Gear tab.
2. Build a **Crude Solid Fuel Burner** and install an Iron Fuel Box, an Iron Heat Core, and a Battery Cell. It burns coal or charcoal and exports FE from its sides. See [Solid Fuel Burning](content/solid-fuel-burner.md).
3. Build an **Iron Crusher Chassis** and install an Iron Crush Head and a Battery Cell. Place it next to the burner. A Crusher always accepts harder ore than its head, but each missing level adds time, energy, and jam risk and suppresses bonus output. See [Crusher](content/crusher.md).
4. Crush raw ore before smelting. Output Amount is deterministic and banks fractions until they pay out an extra item; Super Output is a separate chance for one more. Crushing crushed ore again into dust lowers the smelting cost.
5. Build the **RNGTech Furnace**. It is the solid-fuel furnace that reaches the heat crushed ore and dust need. Electric furnaces arrive after circuits in the next band. See [Furnace](content/furnace.md).
6. Pick one **Battery Chassis** path for stored energy. Iron is steady and simple; Copper moves more FE/t but leaks while idle. A chassis outputs only what its installed cells allow, so one full cell next to empty cells is still limited. See [Battery Chassis](content/battery-chassis.md).

Optional early power comes from farms. The **Bio Generator** burns crops, seeds, saplings, and prepared biomass, and a Bio Chamber improves it. The **Wooden Composter** batches eight plant inputs into Composted Biomass, and Rich Biomass is the denser follow-up fuel. See [Bio Generator](content/bio-generator.md). Wooden chassis and the Potato Battery Cell exist for organic-only processing and are not a foundation for a factory.

## Stage 3-4: the steel factory

Stage 3-4 changes the rules. Recipes start to care about temperature, machines need steady FE/t, and Stage 4 introduces recoverable processing failures when control is poor. The goal is a steel machine park you keep.

1. Make **Bronze**. Crush tin and copper into dust, combine copper dust, tin dust, and coal dust into Bronze Blend, and smelt it in the RNGTech Furnace. Tin is an alloy ingredient here, not a machine stage.
2. Visit the **Nether** for Nether Quartz. Circuits need it.
3. Build a **Crude Metal Press** with a Circuit Mold, a Bronze Heat Core, and a Battery Cell, then press a **Basic Electric Circuit**. See [Metal Press](content/metal-press.md).
4. Learn the heat vocabulary before pressing harder recipes. Work waits below Min Temperature, output can fail above Max Temperature, Heat Stability keeps the machine near target, and Overheat tolerance widens the safe band. Heat Cores raise heat; Servos and stability make it safe.
5. Build one **electric furnace**. Iron is the plain baseline; Copper heats faster with a thinner margin and more power draw; Bronze runs hotter and gets a bonus on alloy blends. Any one of them completes the smelting lesson.
6. Build a **Bronze Alloy Furnace Chassis** with a Bronze Heat Core and make **Steel** from iron and carbon. Two direct steel recipes exist with different heat targets. Start with the lower one. See [Alloy Furnace](content/alloy-furnace.md).
7. Build the **Steel Metal Press** and install a Steel Servo. It handles demanding plates, casings, circuits, and connector modules. Keep the crude press for slow plate work. A Steel Servo also helps a crude press reach steel plates.
8. Build an **Iron Resonance Calibrator** with an Iron Resonance Coil, an Iron Control Board, and a reusable pattern, then calibrate kinetic, logic, and thermal components. Calibration stability is rolled when the component is made. Quality raises the whole roll, Stability raises the floor, Precision narrows the range, and a Battery Cell avoids the no-cell penalty. See [Resonance Calibrator](content/resonance-calibrator.md).
9. Press a **Reinforced Machine Frame**, calibrate a logic component for the **Advanced Electric Circuit**, and alloy **Invar** from iron and nickel dust.
10. Upgrade to a **Steel Crusher Chassis**, **Steel Furnace Chassis**, **Steel Battery Chassis**, and a **Steel Crush Head**. Steel heads also reach hardness 3, and a Diamond Tip pushes them to 4.

Two recovery machines become worthwhile here. The **Potential Reactor** turns leftover affixes and Refinement Potential into FE, and the **Component Recycler** returns crafting material from parts and machines while ignoring affixes entirely. Run a stack through the reactor first, then recycle the stripped item. See [Potential Reactor](content/potential-reactor.md) and [Component Recycler](content/component-recycler.md).

Power options widen as well. The [Corrosion Cell](content/corrosion-cell.md) eats iron or copper plates and Electrolyte. [Solar Panels](content/solar-panel-and-array.md) need circuits and open sky. The [Algae Photobioreactor](content/algae-photobioreactor.md) grows Bio Generator fuel from water, carbon, and light, and the [Wooden Dehumidifier](content/water-infrastructure.md) multiblock supplies the water. Copper, alloy, and steel solid-fuel burners keep the fuel route alive.

## Stage 5-6: fluids and chemistry

Stage 5-6 is about fluids. Most of its quests are optional, but every route ends in more power, and the next band needs that power. Machine sets now come in Aluminum with Invar Battery Cells and Titanium with Sparksteel Battery Cells. Lower-stage components still mix in freely.

1. Craft an **Advanced Machine Frame**.
2. Build the **Melter** with a Heat Core, a Crush Head, and a Fluid Pump. It melts plant material and resin into **Lubricant**, which later machine parts require. Bucket lubricant early and pump it once lines need steady flow. See [Melter](content/melter.md).
3. Build the **Component Assembler** to bulk-produce cell cores and make batteries cheaper. See [Component Assembler](content/battery-assembler.md).
4. Treat **gases as fluids**. They use the same tanks, buckets, and fluid connectors, and JEI shows gasification, combustion, reforming, and algae recipes.
5. Gasify coal dust and water into **Syngas** in the Coal Gasifier, then burn it in the **Syngas Combustor**. Capture its Carbon Exhaust for algae.
6. Make **Methane** from algae biomass and organic reagent in the Melter, then reform it with steam in the **Steam Methane Reformer** for Hydrogen and Carbon Monoxide. Carbon Monoxide is the best algae carbon input.
7. Build the **Cavitation Generator** with a rotor and nozzle. Fed water, it makes FE and wears its rotor; fitted with nitrogen Gear, it separates Nitrogen instead. See [Cavitation Generator](content/cavitation-generator.md).
8. Combine Nitrogen, Hydrogen, and FE in the **Ammonia Synthesizer**, then burn the Ammonia in the **Ammonia Fuel Cell**. Only the fuel cell generates. See [Ammonia Power Chain](content/ammonia-power-chain.md).

The **Silica Gel Dehumidifier** with two column casings replaces the wooden barrel: it makes about twice the water and works underground. Steel and Titanium Resonance sets calibrate the next component stage.

## Stage 7-8: advanced power

Stage 7+ builds permanent infrastructure for high FE/t systems and unlocks the highest affix tiers. Recipes here can gate on affix breakpoints, so watch stability and maximum heat on your Gear.

1. Press an **Elite Electric Circuit**.
2. Build a **Titanium Alloy Furnace Chassis** with a Titanium Heat Core, a Sparksteel Battery Cell, and a Titanium Alloy Crucible. Alloy **Tungstensteel**, **Nullite**, and **Aethergold**.
3. Craft an **Exotic Machine Frame**, then upgrade a Crusher and Furnace to **Tungstensteel** with a Tungstensteel Crush Head. Place this line where power and logistics can scale around it.
4. Build the **Exotic Affix Forge** for powered, recipe-driven refinement. See [Exotic Affix Forge](content/exotic-affix-forge.md).
5. Make **Nullite Battery Cells** and a Nullite Battery Chassis. Nullite cells allow higher affix tiers; Aethergold cells have the highest burst output.
6. Craft a **Tungstensteel Fluid Pump** and **Tungstensteel Servo**, then press an **Ultimate Electric Circuit**.
7. Build the **Vacuum Collapse Generator** with a Void Chamber, Collapse Nozzle, and Dimensional Stabilizer. Feed it Void Catalyst, keep stability high before spending catalysts, and pull residue from the bottom. Redstone pauses it. See [Vacuum Collapse Generator](content/vacuum-collapse-generator.md).

Exotic Crusher and Furnace sets with Exotic Battery Cells are the top machine stage. Tungstensteel and Nullite Resonance sets cover late calibration.

## Logistics: cables, connectors, and tanks

Early machines share power by touching. Once you have the RNGTech Furnace, build **Cable** and **Universal Connectors**. One cable carries energy, fluids, and items; the connector module on each endpoint decides throughput, so a hungry machine gets a better connector without re-laying cable. See [Universal Cable](content/basic-wire.md).

- The Crude Energy Connector is craftable at Stage 1. Basic Energy, Fluid, and Item Connectors need a Connector Mold in a Metal Press, and higher tiers follow the stage ladder.
- Each interface supports up to 15 channels, transfers many-to-many, prioritizes by mode, and can cap a specific machine's rate. The Network tab shows live statistics.
- A **Wrench** controls cable links and connector overlays. The **Configurator** copies one Universal Connector's settings, and optionally its modules, onto others.
- A **Tank Frame** builds plain Iron, Copper, and Bronze tanks. A Pressure Tank Frame builds the Steel, Titanium, and Tungstensteel **Compressor Tanks**, which need a Battery Cell, at least one Servo, and FE only while compressing. See [Compressor Tank](content/compressor-tank.md).

Normal automation reaches process inventories, not protected Gear. Battery Cells and other exceptions depend on the machine, so read the content page before designing automated loading.

## Refinement: the Affix Forge

The **Affix Forge** applies refinement catalysts to machine items, parts, and Battery Cells. Tool Heads and Rods refine in the Tool Bench Refine tab, and placed machines refine from their own Refinement tab. Every operation spends Refinement Potential. See [Affix Forge](content/affix-forge.md) and [Refinement Operations](systems/progression.md#refinement-operations).

- **Base forge**: Affix Injector adds an affix, Affix Modifier upgrades a random affix, Nullifier Coil removes a random one. Magic items hold one prefix and one suffix; Rare items hold up to three of each.
- **Lens Array upgrade**: Power, Speed, Yield, Stability, Control, Kinetic, and Efficiency lenses weight the roll toward matching affixes. This is the first deterministic tool; not every affix is reachable through it.
- **Modifier Socket upgrade**: Conservation, Stabilization, and Destabilization crystals modify an operation's cost or side effects.
- **Resonance Matrix upgrade**: the Greater Affix Upgrader, Resonance and Transmutation crystals, Null, Chaos, and Expansion crystals, and the Ascension Catalyst and Ascension Matrix that turn a Magic item into Rare.

Spend Refinement Potential deliberately. A Magic part with two good affixes is a strong ascension target, and stripped items can still feed the Potential Reactor.

## Read the machine screen

The Process tab shows work, stored resources, and status squares. The Gear tab holds installed components. Hover status icons, meters, and slots for missing requirements, temperature problems, or blocked output. Stats shows effective values with hover explanations, Refinement changes the placed machine's affixes within its remaining budget, and Mastery shows the chassis' level and passive tree. Available tabs vary by machine.

## When a machine underperforms

- Check its Gear first. A missing Battery Cell, an under-level Crush Head, or a cold Heat Core explains most slow or stalled machines.
- Check the recipe gates in JEI: minimum temperature, temperature stability, component stage, processing level, and calibration stability.
- Check power. Stage 4 and later work can halt or fail when FE/t drops mid-job, and Servos add grace.
- Check connector direction, mode, and channel when power or fluid does not move.
- Check the roll. Compare the affixes on the part with what the recipe needs, and refine or recycle rather than rebuilding the block.

## Scope and updates

Many features are prototypes and may change between releases. Planned designs are not available merely because they have documentation pages; [Current Implementation](reference/current-implementation.md) records what exists. Back up worlds before updating, and check the release notes for the version you install. Automated checks cover compilation, resources, and selected source/data rules; they do not prove that every multiplayer or mod interaction works.
