---
wiki:
  category: Processing
  icon: rngtech:wooden_composter
  ids:
    - rngtech:wooden_composter
---

# Wooden Composter

{{ infobox(
    fields={
        "Type": "Passive processing machine",
        "Stages": "1",
        "Power": "None",
        "Gear": "None",
        "Mastery": "No",
    },
) }}

The **Wooden Composter** slowly turns low-value plant matter into {{ item('rngtech:composted_biomass') }}, a dense early fuel for the [Bio Generator](bio-generator.md). It needs no power, so you can fill it with seeds, saplings, leaves, and crop scraps and leave it to work. Adding water makes it twice as fast.

Unlike most RNGTech machines, the Wooden Composter has no rarity, affixes, Gear, or Mastery.

## Obtaining

### Crafting

{{ crafting() }}

### Breaking

Mine a placed Wooden Composter with a pickaxe. It drops itself, and the items inside it drop next to it.

## Usage

### Composting

Put compostable items in the 24 input slots. Every 8 compost units make one Composted Biomass. Most items count as 1 unit; prepared inputs such as {{ item('rngtech:compost_feedstock') }} count as 2.

| Condition | Time per Composted Biomass |
|---|---:|
| Dry | 120 s (2,400 ticks) |
| Wet | 60 s (1,200 ticks), using 10 mB of water per batch |

Two bonuses each add 10% composting speed:

- **Bulk**: at least 200 compost units waiting in the input slots.
- **Variety**: at least 5 different compostable items in the input slots.

The water tank holds 4,000 mB. Fill it by putting water buckets or other water containers in the water slot, or pipe water in. If you need to empty it, use the purge button on the Process tab or right-click the Composter with a {{ item('rngtech:purge_bucket') }}. Each purge voids up to 1,000 mB.

### Rich Biomass

Put a catalyst in the catalyst slot, next to the output slot, to make {{ item('rngtech:rich_biomass') }} directly. The batch uses the same compost units, time, and water.

| Catalyst | Batches per item |
|---|---:|
| Bone Meal, Gunpowder, or Organic Reagent | 1 |
| [Urea](ammonia-synthesizer.md#using-urea) | 4 |

A [Crusher](crusher.md) turns one Bone into 4 Bone Meal, one more than a crafting grid gives.

The Composter takes the Urea when a batch finishes, and the Urea pays for that batch and the next three. If you break the Composter, it loses any batches that the Urea has not made yet. The first status square lights when the next batch makes Rich Biomass.

The output slot holds one kind of item:

- **Composted Biomass in the output slot:** the Composter continues to make Composted Biomass and keeps the catalyst. Empty the slot to change to Rich Biomass.
- **Rich Biomass in the output slot and no catalyst left:** the Composter stops. Empty the slot or add a catalyst.

### Inputs

The Composter accepts everything in the inputs tag below, plus any item that the vanilla Composter accepts.

{{ tag("rngtech:wooden_composter/inputs") }}

These prepared inputs count as 2 compost units each:

{{ tag("rngtech:wooden_composter/prepared_inputs") }}

### Crushing seeds first

Crushing seeds in a [Crusher](crusher.md) turns each seed into one Compost Feedstock, which is worth twice as much in the Composter. Eight seeds composted directly make one Composted Biomass; eight seeds crushed first make two. Crushing eight seeds costs 640 FE, and the extra Composted Biomass burns for 1,600 FE, so you come out 960 FE ahead.

{{ processing("crusher", output="rngtech:compost_feedstock") }}

### Using the output

Burn {{ item('rngtech:composted_biomass') }} in the [Bio Generator](bio-generator.md) for 1,600 FE each before modifiers, or craft it with Bone Meal, Organic Reagent, or Gunpowder into {{ item('rngtech:rich_biomass') }}, a denser fuel. One Urea from the [Ammonia Synthesizer](ammonia-synthesizer.md#using-urea) turns four Composted Biomass into four Rich Biomass. The grid recipes cost the same as [the catalyst slot](#rich-biomass). Composted Biomass is not a furnace fuel and does not burn in the [Fuel Burner](fuel-burner.md).

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Insert compostable items, catalysts, and water containers, and fill the water tank. |
| Bottom | Extracts Composted Biomass, Rich Biomass, and empty containers. |

The Wooden Composter has no FE input or output.

## Screen

The Wooden Composter has one working tab, **Process**. It shows the 24 input slots, the catalyst slot, the output slot, the water tank with its container slots and purge button, progress, and status squares for the next batch and the bulk and variety bonuses. Hover for exact values.

## Data values

{{ data_values() }}

## See also

- [Bio Generator](bio-generator.md)
- [Crusher](crusher.md)

{{ navbox() }}
