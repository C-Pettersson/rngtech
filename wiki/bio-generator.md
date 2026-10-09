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
        "Power": "Burns crops and biomass, generates FE",
        "Gear": "{{ item('rngtech:bio_chamber', 'Bio Chamber') }}, {{ item('rngtech:iron_battery_cell', 'Battery Cell') }}",
        "Mastery": "No",
    },
) }}

The **Bio Generator** is an early generator that burns food and plant matter instead of coal. Potatoes, carrots, bread, saplings, and seeds all work straight from the farm, and prepared biomass from the [Wooden Composter](wooden-composter.md) and [Algae Photobioreactor](algae-photobioreactor.md) burns for much longer.

Each Bio Generator you craft rolls its own [rarity and affixes](rarity-and-affixes.md). It does not have Machine Mastery.

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine it with a pickaxe to get it back. Fuel and installed Gear drop as separate items.

## Usage

### Generating power

Put fuel in the fuel slot on the Process tab. The generator burns one item at a time at about 8 FE/t before stats, so a fuel's FE value decides how long it burns. A Composted Biomass worth 1,600 FE burns for 200 ticks (10 seconds).

Generated FE goes into a small internal buffer, plus any installed Battery Cell, and the generator pushes it out to adjacent blocks on its four sides, for example a [Universal Cable](universal-cable.md). The generator does not limit its own output, so the receiver or connector tier decides how much FE moves each tick. When the buffer is full, the generator pauses and does not waste fuel. It never accepts FE from outside.

Golden Apples, Enchanted Golden Apples, and Golden Carrots are refused.

### Fuel

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
- **Rich Biomass** is crafted from Composted Biomass plus Bone Meal, Gunpowder, or Organic Reagent, and is worth 3,200 FE.
- **Algae Biomass** grows in the [Algae Photobioreactor](algae-photobioreactor.md). Craft four into Dense Algae Biomass for a better fuel.
- **Organic Reagent** comes from crushing potatoes, carrots, and plant biomass. Crushing a potato costs 600 FE and the reagent burns for 546 FE, so this path loses a little power unless your Bio Chamber boosts reagent fuel.

Composted Biomass and Rich Biomass burn only in the Bio Generator. They are not furnace fuel and do not burn in the [Solid Fuel Burner](solid-fuel-burner.md).

{{ crafting("rngtech:rich_biomass", "rngtech:dense_algae_biomass") }}

### Gear

The Gear tab has two optional slots:

- **Battery Cell**: adds FE storage on top of the internal buffer. Stage 0–3 cells fit, including the Voltaic Potato Battery Cell.
- **Bio Chamber**: a {{ item('rngtech:bio_chamber') }} adds its rolled power modifiers. Besides generic FE/t and burn-duration bonuses, it can roll bonuses for one fuel family, such as potatoes, seeds, Composted Biomass, or algae. These apply only while that family is burning, and a specific family like saplings uses its own bonus rather than the general plant bonus.



### Automation

| Side | Behavior |
|---|---|
| Top | Inserts fuel. |
| Sides | Export FE to adjacent receivers. Does not accept FE. |
| Bottom | Nothing. |

Automation cannot extract items, and the Gear slots are installed by hand.

## Screen

The Bio Generator screen has four tabs:

- **Process**: fuel slot, burn progress, stored FE, and FE/t. Hover for exact values.
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
