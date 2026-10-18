---
wiki:
  category: Power generation
  icon: rngtech:bio_generator
  ids:
    - rngtech:bio_generator
---

# Bio Generator

{{ infobox(
    fields={
        "Type": "Generator",
        "Power": "Digests crops and biomass into biogas, burns it for FE",
        "Gear": "{{ item('rngtech:bio_chamber', 'Bio Chamber') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Bio Generator** is an early generator built like a small farm biogas plant. A feed hopper drops food and plant matter into a digester tank, where it rots into biogas. The gas collects under a membrane dome, and a small gas engine burns it for FE. Potatoes, carrots, bread, saplings, and seeds all work straight from the farm, and prepared biomass from the [Wooden Composter](wooden-composter.md) and [Algae Photobioreactor](algae-photobioreactor.md) lasts much longer. See [Power Chains](power-chains.md#bio) for how it fits beside the main generators.

Each Bio Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md). It does not have Machine Mastery.

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine it with a pickaxe to get it back. Fuel and installed Gear drop as separate items.

## Usage

### Generating power

Put fuel in the fuel slot on the Process tab. Power comes in three steps, and you can watch each one on the block:

1. **Digester.** The digester takes one item at a time from the hopper and turns it into biogas at about 8 FE/t before stats. A fuel's FE value decides how long it digests: a Composted Biomass worth 1,600 FE takes 200 ticks (10 seconds). The sight glass on the tank shows the current item's slurry level falling, with bubbles while gas forms.
2. **Gas holder.** Biogas collects under the dome on top of the tank, which swells as it fills. It holds 1,600 FE of gas.
3. **Engine.** The gas engine starts once the holder has 400 FE of gas, then runs at the digester's pace until the holder is empty. Its flywheel spins and the stack puffs exhaust while it runs, and the lamp on the hopper lights. The gauge on the engine shows the holder's pressure, with the amber band below the start pressure.

A freshly fed generator therefore waits a few seconds before the first FE while the gas pressure builds. The last of the gas still burns when the hopper runs dry, so no fuel is lost.

Generated FE goes into a small internal buffer, plus any installed Battery Cell, and the generator pushes it out to adjacent blocks on every side, for example a [Universal Cable](universal-cable.md). The generator does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. When the buffer is full, the engine idles and the digester keeps filling the gas holder. Once the holder is full too, the digester waits. Nothing is wasted. The generator never accepts FE from outside.

Golden Apples, Enchanted Golden Apples, and Golden Carrots are refused.

### Retention time

The dial above the digestion bar on the Process tab sets the **retention time**: how many days the feed stays in the digester. Click or scroll it. Real digesters work the same way: a short stay pushes feed through faster but leaves gas undigested, and a long stay digests more of each load but makes gas slowly.

| Retention | FE/t | FE per item | Unrolled generator | Rich Biomass |
|---|---:|---:|---:|---:|
| 10 days | 1.60x | 0.80x | 13 FE/t | 2,560 FE |
| 15 days | 1.23x | 0.92x | 10 FE/t | 2,954 FE |
| **20 days (rated)** | **1.00x** | **1.00x** | **8 FE/t** | **3,200 FE** |
| 30 days | 0.73x | 1.09x | 6 FE/t | 3,491 FE |
| 40 days | 0.57x | 1.14x | 5 FE/t | 3,657 FE |

The rated 20 days gives every fuel exactly its listed FE at the listed rate. FE/t rounds up like the generator's normal rate. A new retention applies at once, including to the item already digesting. Hover the dial to see the numbers for the item in the hopper.

### Fuel

Base FE is at the rated 20-day retention.

| Fuel | Items | Base FE |
|---|---|---:|
| Potatoes | {{ tag("rngtech:bio_generator/potato_fuels") }} | 120 (Poisonous Potato: 200) |
| Carrots | {{ tag("rngtech:bio_generator/carrot_fuels") }} | 120 |
| Bread | {{ tag("rngtech:bio_generator/bread_fuels") }} | 240 |
| Saplings | {{ tag("rngtech:bio_generator/sapling_biomass") }} | 80 |
| Seeds | {{ tag("rngtech:bio_generator/seed_biomass") }} | 40 |
| Other plant biomass | {{ slot('minecraft:kelp') }}{{ slot('minecraft:dried_kelp') }}{{ slot('rngtech:plant_reagent') }} | 80 |
| Organic Reagent | {{ tag("rngtech:bio_generator/organic_reagents") }} | 546 |
| Algae Biomass | {{ tag("rngtech:bio_generator/algae_biomass") }} | 400 |
| Composted Biomass | {{ tag("rngtech:bio_generator/composted_biomass") }} | 1,600 |
| Dense Algae Biomass | {{ tag("rngtech:bio_generator/dense_algae_biomass") }} | 2,400 |
| Rich Biomass | {{ tag("rngtech:bio_generator/rich_biomass") }} | 3,200 |

With JEI installed, all fuels and their values appear in the Bio Generator Fuels category.

### Fuel chains

Burning crops directly is the simplest setup, but prepared fuels are worth much more per item:

- **Composted Biomass** comes from the [Wooden Composter](wooden-composter.md), which turns eight units of cheap organic scraps into one item worth 1,600 FE. Crushing seeds into Compost Feedstock in the [Crusher](crusher.md) first doubles their compost value and comes out well ahead even after the Crusher's power cost.
- **Rich Biomass** is crafted from Composted Biomass plus Bone Meal, Gunpowder, or Organic Reagent, and is worth 3,200 FE. One [Urea](ammonia-synthesizer.md#using-urea) turns four Composted Biomass into four Rich Biomass.
- **Algae Biomass** grows in the [Algae Photobioreactor](algae-photobioreactor.md). Craft four into Dense Algae Biomass for a better fuel.
- **Organic Reagent** comes from crushing potatoes, carrots, and plant biomass. Crushing a potato costs 600 FE and the reagent gives 546 FE, so this path loses a little power unless your Bio Chamber boosts reagent fuel.

Composted Biomass and Rich Biomass are fuel only for the Bio Generator. They are not furnace fuel and do not burn in the [Solid Fuel Burner](solid-fuel-burner.md).

{{ crafting("rngtech:rich_biomass", "rngtech:rich_biomass_from_urea", "rngtech:dense_algae_biomass") }}

### Gear

The Gear tab has two optional slots:

- **Battery Cell**: adds FE storage on top of the internal buffer. Stage 0–3 cells fit, including the Voltaic Potato Battery Cell.
- **Bio Chamber**: a {{ item('rngtech:bio_chamber') }} adds its rolled power modifiers, and shows as a green culture canister in front of the sight glass. Besides generic FE/t and fuel-duration bonuses, it can roll bonuses for one fuel family, such as potatoes, seeds, Composted Biomass, or algae. These apply only while that family is digesting, and a specific family like saplings uses its own bonus rather than the general plant bonus.

### Automation

Connectors work on any side: every face inserts fuel and exports FE to adjacent receivers, and no face accepts FE. The **automation port** on the back, the plant's grid-connection cabinet with a copper-pinned socket in its door and a copper conduit from the engine, marks the spot where a [Universal Cable](universal-cable.md) connector seats flush against the machine.

Automation cannot extract items, and the Gear slots are installed by hand.

## Screen

The Bio Generator screen has four tabs:

- **Process**: fuel slot, the retention dial, digestion progress, the gas holder gauge with its engine start mark, stored FE, and status icons. Hover for exact values.
- **Gear**: Battery Cell and Bio Chamber slots.
- **Stats**: the generator's current stats, including traits and Gear.
- **Refinement**: refine the placed generator's traits with a catalyst.

## Data values

{{ data_values() }}

## See also

- [Wooden Composter](wooden-composter.md) and [Algae Photobioreactor](algae-photobioreactor.md), which make prepared fuel.
- [Solid Fuel Burner](solid-fuel-burner.md)
- [Machine Stats](machine-stats.md)

{{ navbox() }}
