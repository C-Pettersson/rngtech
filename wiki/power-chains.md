---
wiki:
  category: Guides
  icon: rngtech:syngas_combustor
---

# Power Chains

Each generator page explains one machine. This page shows how they connect: which machine feeds which, what flows between them, and what usually runs short. RNGTech power comes in steps. Coal carries you to Stage 4, gas chemistry takes over at Stages 5 and 6, and Void Catalyst powers Stages 7 and 8. Each step is a chain of several machines, and the slowest link sets how much FE you get.

For each generator's output per stage, see the table under [Power basics](getting-started.md#power-basics). For how to run a single machine, follow the links in each diagram.

## Reading the diagrams {#reading}

Each box is a machine, and each arrow is an item or fluid moving from one machine to the next. Click a box to open its page. The label on an arrow shows the resource, how much one run makes or uses, and the rate at base speed.

- One number, such as **1,000 mB**, means the two machines make and use the same amount.
- Two numbers, such as **500 mB → 100 mB**, mean the first machine makes 500 mB per run and the second uses 100 mB per run. Compare the rates to see how many of each machine you need.
- A dashed box is something you supply yourself, such as mined coal, or a product that goes on to another chain.
- A dashed arrow loops back to an earlier machine.

The rates are the recipes' base times, before machine stats, Gear, Mastery, and settings such as mixture, aeration, or throttle. Hover a label for the exact amounts and times.

Most gas machines also use water. To keep the diagrams readable, water appears only under [Water supply](#water).

## Stage 1–4: coal {#coal}

{{ chain(edges=[
    [None, "minecraft:coal", "rngtech:crude_solid_fuel_burner"],
    ["rngtech:furnace", "minecraft:charcoal", "rngtech:crude_solid_fuel_burner"],
]) }}

**What to build.** A [Solid Fuel Burner](solid-fuel-burner.md) next to your first machines, fed with mined Coal or with Charcoal smelted from logs in the [Furnace](furnace.md). See [Fuel](solid-fuel-burner.md#fuel) for everything else it burns. Coal Dust from the [Crusher](crusher.md) does not burn in the burner; save it for Bronze Blend and the Stage 5 Coal Gasifier.

**Bottleneck.** Rate, not fuel. A burner's FE/t is set by its Heat Core, and a Steel Heat Core is as fast as a burner gets. More coal only makes a burner last longer. At the same time, Stage 5 and later recipes cost several times the early FE, so each new machine needs more burners.

**Next upgrade.** [Syngas](#syngas). The same coal, crushed and gasified, runs a Syngas Combustor at more than twice a burner's best FE/t.

## Stage 5: Syngas {#syngas}

{{ chain(edges=[
    ["rngtech:iron_crusher_chassis", "rngtech:coal_dust", "rngtech:coal_gasifier"],
    ["rngtech:coal_gasifier", "rngtech:syngas", "rngtech:syngas_combustor"],
    ["rngtech:syngas_combustor", "rngtech:carbon_exhaust", "rngtech:algae_photobioreactor"],
    ["rngtech:coal_gasifier", "rngtech:gasification_residue", "rngtech:wooden_dehumidifier"],
    ["rngtech:wooden_dehumidifier", "rngtech:carbon_exhaust", "rngtech:algae_photobioreactor"],
    ["rngtech:algae_photobioreactor", "rngtech:algae_biomass", "rngtech:bio_generator"],
]) }}

**What to build.** A [Crusher](crusher.md) grinding coal into Coal Dust, a [Coal Gasifier](coal-gasifier.md) that turns the dust and water into Syngas, and a [Syngas Combustor](syngas-combustor.md) that burns it. One Gasifier keeps one Combustor running at the rated mixture. The Gasifier also needs water; see [Water supply](#water).

**Bottleneck.** The by-products. The Combustor stops when its Carbon Exhaust tank is full, and the Gasifier stops when its residue slot is full. Pipe Carbon Exhaust to an [Algae Photobioreactor](algae-photobioreactor.md), and put Gasification Residue in a [Wooden Dehumidifier](wooden-dehumidifier.md), which turns it into more Carbon Exhaust. One Combustor makes far more exhaust than one Photobioreactor uses, so build several Photobioreactors or void the extra with the Combustor's purge button. A leaner mixture makes less exhaust per mB of Syngas. The algae can go straight into a [Bio Generator](bio-generator.md), but its best use comes at Stage 6.

**Next upgrade.** The Stage 6 [carbon loop](#carbon-loop), which turns the algae into Hydrogen for [Ammonia](#ammonia).

## Stage 6: the carbon loop {#carbon-loop}

{{ chain(edges=[
    ["rngtech:algae_photobioreactor", "rngtech:algae_biomass", "rngtech:melter"],
    ["rngtech:iron_crusher_chassis", "rngtech:organic_reagent", "rngtech:melter"],
    ["rngtech:melter", "rngtech:methane", "rngtech:steam_methane_reformer"],
    ["rngtech:steam_methane_reformer", "rngtech:carbon_monoxide", "rngtech:algae_photobioreactor"],
    ["rngtech:steam_methane_reformer", "rngtech:carbon_monoxide", "rngtech:syngas_combustor"],
    ["rngtech:steam_methane_reformer", "rngtech:hydrogen", None],
]) }}

**What to build.** A [Melter](melter.md) that melts Algae Biomass, Organic Reagent, and water into Methane, and a [Steam Methane Reformer](steam-methane-reformer.md) that reacts the Methane with water into Hydrogen and Carbon Monoxide. Organic Reagent comes from crushing potatoes, carrots, or plant biomass in the [Crusher](crusher.md). The Reformer also accepts Syngas instead of Methane, but makes much less Hydrogen from it, so use Syngas only until your Methane runs.

**Bottleneck.** The Reformer stops when its Carbon Monoxide tank is full. Send the Carbon Monoxide back to the [Algae Photobioreactor](algae-photobioreactor.md), where it is the fastest carbon input. That closes the loop: each reform makes as much Carbon Monoxide as one Algae Biomass needs at rated aeration, and that biomass melts into the next batch of Methane. A Photobioreactor grows biomass more slowly than a Reformer uses Methane, so run two or more per Reformer, and keep them in light. The [Syngas Combustor](syngas-combustor.md) can burn spare Carbon Monoxide, but it gets little FE from it. What the loop cannot make for itself is Organic Reagent, so keep a crop farm feeding the Crusher.

**Shifting for more Hydrogen.** A second Reformer can run the [water-gas shift](steam-methane-reformer.md#water-gas-shift) on the Carbon Monoxide instead, which adds about a third more Hydrogen per reform. The shift turns the Carbon Monoxide into a little Carbon Exhaust, and algae grow half as fast on Carbon Exhaust, so a shift loop needs about twice the Photobioreactors. Shift when Hydrogen is what runs short and you have the room and light for more Photobioreactors.

{{ chain(edges=[
    ["rngtech:melter", "rngtech:methane", "rngtech:steam_methane_reformer"],
    ["rngtech:steam_methane_reformer", "rngtech:carbon_monoxide", "rngtech:steam_methane_reformer#2"],
    ["rngtech:steam_methane_reformer#2", "rngtech:carbon_exhaust", "rngtech:algae_photobioreactor"],
    ["rngtech:algae_photobioreactor", "rngtech:algae_biomass", "rngtech:melter"],
    ["rngtech:steam_methane_reformer", "rngtech:hydrogen", None],
    ["rngtech:steam_methane_reformer#2", "rngtech:hydrogen", None],
]) }}

**Next upgrade.** Send the Hydrogen on to [Ammonia](#ammonia).

## Stage 6: Ammonia {#ammonia}

{{ chain(edges=[
    [None, "rngtech:methane", "rngtech:steam_methane_reformer"],
    ["rngtech:steam_methane_reformer", "rngtech:hydrogen", "rngtech:ammonia_synthesizer"],
    ["rngtech:cavitation_generator", "rngtech:nitrogen", "rngtech:ammonia_synthesizer"],
    ["rngtech:ammonia_synthesizer", "rngtech:ammonia", "rngtech:ammonia_fuel_cell"],
]) }}

**What to build.** A [Cavitation Generator](cavitation-generator.md) fitted with the Nitrogen Extraction Rotor and Nitrogen Separation Nozzle, which splits water into Nitrogen. An [Ammonia Synthesizer](ammonia-synthesizer.md) that combines the Nitrogen with Hydrogen from the [carbon loop](#carbon-loop), and a [Fuel Cell](ammonia-fuel-cell.md) that burns the Ammonia. The Fuel Cell makes more FE than the Synthesizer costs.

**Bottleneck.** Hydrogen. One Reformer keeps about one Synthesizer busy, and one Synthesizer about one Fuel Cell, while one Cavitation Generator makes Nitrogen for two or three Synthesizers. Every extra Fuel Cell therefore needs another Reformer and the Melter and Photobioreactors behind it. The Cavitation Generator is also the hungriest water user in the chain; see [Water supply](#water).

**Spare Nitrogen.** Nitrogen has a second use. With the **Blast Draft** keystone, Nitrogen piped into a Furnace, Alloy Furnace, or Metal Press lets it run hotter, and so faster, before it builds failure strain; see [Nitrogen atmosphere](heat.md#nitrogen-atmosphere). A hot lane uses 1 mB a tick, a bit less than a Synthesizer, so every lane you run that way is Nitrogen your Ammonia chain does not get.

**Burning Hydrogen directly.** The [Fuel Cell](ammonia-fuel-cell.md#ammonia-or-hydrogen) also burns Hydrogen, with no Nitrogen or Synthesizer, but gets less than half the FE per mB of Hydrogen that the Ammonia route does. Use it while you build the rest of the chain, or for Hydrogen you have to spare.

**Urea.** An Ammonia Synthesizer with Carbon Exhaust piped in turns some of its Ammonia into [Urea](ammonia-synthesizer.md#making-urea), a fertilizer for the Forestry Companion and the Algae Photobioreactor. It makes Urea before Ammonia whenever it can, and every Urea is Ammonia your Fuel Cells do not get, so feed it only as much Carbon Exhaust as you need Urea.

**Next upgrade.** [Void Catalyst](#void-catalyst). In the Vacuum Collapse Generator, Ammonia is worth far more FE than in a Fuel Cell.

## Stage 7–8: Void Catalyst {#void-catalyst}

{{ chain(edges=[
    ["rngtech:ammonia_synthesizer", "rngtech:ammonia", "rngtech:melter", {"to": "melter/ender_slurry"}],
    [None, "minecraft:ender_pearl", "rngtech:melter", {"to": "melter/ender_slurry"}],
    ["rngtech:melter", "rngtech:ender_slurry", "rngtech:battery_assembler", {"from": "melter/ender_slurry"}],
    ["rngtech:titanium_alloy_furnace_chassis", "rngtech:nullite_ingot", "rngtech:battery_assembler"],
    ["rngtech:metal_press", "rngtech:tungstensteel_plate", "rngtech:battery_assembler"],
    ["rngtech:battery_assembler", "rngtech:void_catalyst", "rngtech:vacuum_collapse_generator"],
    ["rngtech:vacuum_collapse_generator", "rngtech:collapse_residue", "rngtech:melter"],
]) }}

**What to build.** A [Melter](melter.md) that dissolves Ender Pearls in Ammonia into Ender Slurry, a [Component Assembler](component-assembler.md) that seals the slurry around a Nullite Ingot in a Tungstensteel Plate, and the [Vacuum Collapse Generator](vacuum-collapse-generator.md) that collapses each Void Catalyst into a large batch of FE. Nullite comes from the [Alloy Furnace](alloy-furnace.md) and Tungstensteel Plates from the [Metal Press](metal-press.md). The Ammonia comes from the Stage 6 [Ammonia](#ammonia) chain, so keep it running.

**Bottleneck.** Ender Pearls: every catalyst needs them twice, in its slurry and in its Nullite Ingot. With Max Temperature affixes, a Melter can use the generator's Collapse Residue in place of one pearl and make twice the slurry, so each catalyst needs fewer pearls and half the Ammonia. See [Void Catalyst](vacuum-collapse-generator.md#void-catalyst) for the details. The generator's output also peaks at the end of each collapse, so your cables and storage must take the peak; see [the collapse curve](vacuum-collapse-generator.md#the-collapse-curve).

**Next upgrade.** Better Gear in the generator. Nullite and Exotic Void Chambers, Collapse Nozzles, and Dimensional Stabilizers raise the FE from every catalyst.

## Water supply {#water}

{{ chain(edges=[
    ["rngtech:wooden_dehumidifier", "minecraft:water", "rngtech:coal_gasifier"],
    ["rngtech:silica_gel_dehumidifier", "minecraft:water", "rngtech:cavitation_generator"],
    ["rngtech:silica_gel_dehumidifier", "rngtech:saturated_silica_gel_beads", "rngtech:furnace"],
    ["rngtech:furnace", "rngtech:silica_gel_beads", "rngtech:silica_gel_dehumidifier"],
]) }}

Every gas chain runs on water: the Coal Gasifier, Algae Photobioreactor, Melter, Steam Methane Reformer, and Cavitation Generator all use it.

**What to build.** A [Wooden Dehumidifier](wooden-dehumidifier.md) first. It needs no power and fills itself from the air while it can see the sky, faster in wet biomes and in rain. From Stage 5, a [Silica Gel Dehumidifier](silica-gel-dehumidifier.md) gives steady water anywhere, including underground and in the Nether. Its saturated beads go back through a [Furnace](furnace.md) to dry, so the beads loop between the two machines and only the Furnace's FE is spent.

**Bottleneck.** Water runs short sooner than you expect. Even a Wooden Dehumidifier in the rain makes less water than one Coal Gasifier uses, and the Melter and the Cavitation Generator each use more than the Gasifier. Build several Wooden Dehumidifiers early, and move the thirsty machines onto Silica Gel Dehumidifiers.

**Next upgrade.** Supercharged beads, made in the [Alloy Furnace](alloy-furnace.md), hold more than three times as much water per bead.

## Side power {#side-power}

These generators support the main chains. None of them replaces the chain for your stage.

### Solar Panels {#solar}

[Solar Panels](solar-panel.md) need no fuel and no chain at all, only daylight and open sky. They suit remote or low-power machines, and a Solar Array Controller gathers a field of them into one output. They make little or nothing at night, so pair them with a [Battery Chassis](battery-chassis.md).

### Bio Generator {#bio}

{{ chain(edges=[
    ["rngtech:wooden_composter", "rngtech:composted_biomass", "rngtech:bio_generator"],
    ["rngtech:iron_crusher_chassis", "rngtech:organic_reagent", "rngtech:bio_generator"],
    ["rngtech:algae_photobioreactor", "rngtech:algae_biomass", "rngtech:bio_generator"],
]) }}

The [Bio Generator](bio-generator.md) burns crops and prepared biomass. Prepared fuels from the [Wooden Composter](wooden-composter.md) and [Algae Photobioreactor](algae-photobioreactor.md) are worth much more per item than raw crops. See [Fuel chains](bio-generator.md#fuel-chains) for which preparation pays off.

### Corrosion Cell {#corrosion}

{{ chain(edges=[
    ["rngtech:metal_press", "rngtech:iron_plate", "rngtech:corrosion_cell"],
    ["rngtech:metal_press", "rngtech:aluminum_plate", "rngtech:battery_assembler"],
    ["rngtech:melter", "rngtech:electrolyte_solution", "rngtech:battery_assembler"],
    ["rngtech:battery_assembler", "rngtech:aluminum_anode", "rngtech:corrosion_cell"],
    ["rngtech:melter", "rngtech:electrolyte_solution", "rngtech:corrosion_cell"],
]) }}

The [Corrosion Cell](corrosion-cell.md) dissolves metal in electrolyte. It burns plain plates from Stage 4, and from Stage 5 it burns Anodes assembled from two plates and Electrolyte Solution in the [Component Assembler](component-assembler.md). Once you count the cost of assembling Anodes, it makes less than the main chain of the same stage, so use it to turn spare plates into power or as a backup.

### Potential Reactor {#potential-reactor}

The [Potential Reactor](potential-reactor.md) recoups FE from retired Gear and recycling byproducts. It pays for what an item rolled, then hands the stripped item on to the [Component Recycler](component-recycler.md) for its materials. It only runs while you have old items to burn, so treat it as a refund on Gear you replace, never as base power.

## See also

- [Getting Started](getting-started.md#power-basics), for generator output by stage.
- [Stages](stages.md)
- [Battery Chassis](battery-chassis.md), for storing power between generation and use.
- [Universal Cable](universal-cable.md), for moving FE, fluids, and items between the machines in a chain.

{{ navbox() }}
