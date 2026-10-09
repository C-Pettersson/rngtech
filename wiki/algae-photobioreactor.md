---
wiki:
  category: Fluids and gases
  icon: rngtech:algae_photobioreactor
  ids:
    - rngtech:algae_photobioreactor
---

# Algae Photobioreactor

{{ infobox(
    fields={
        "Type": "Support processor",
        "Stage": "3–4",
        "Power": "None",
        "Gear": "{{ item('rngtech:bio_chamber', 'Bio Chamber') }} (optional)",
        "Mastery": "No",
    },
) }}

The **Algae Photobioreactor** grows {{ item('rngtech:algae_biomass') }} from water, a carbon-bearing gas, and light. It uses no power. Algae Biomass is fuel for the [Bio Generator](bio-generator.md) and a feedstock for methane in the [Melter](melter.md).

It is a Stage 3–4 support machine that turns early gas byproducts into fuel. It has no rarity, affixes, or Refinement of its own; an installed Bio Chamber is what improves it.

## Obtaining

### Crafting

The recipe uses up a {{ item('rngtech:bio_chamber') }}. You need a second Bio Chamber if you want one in the Gear slot.

{{ crafting() }}

### Breaking

Mine the Photobioreactor with a pickaxe. It drops itself plus the items in its slots.

## Usage

### Growing algae

The Photobioreactor has two input tanks of 4,000 mB each:

- a **Water** tank.
- a **Carbon** tank that takes Carbon Exhaust, Syngas, or Carbon Monoxide.

Fill them with buckets in the container slots, or pipe fluid into the sides. Each cycle uses a little of both and makes one Algae Biomass.

Algae needs light. The block directly above the Photobioreactor must have a light level of 12 or more, either from open sky or from a light source such as a torch or Glowstone. If it is too dark, the machine shows a low-light status and waits.

The carbon gas you choose sets the speed:

| Carbon input | Per Algae Biomass | Time | Role |
|---|---:|---:|---|
| Carbon Exhaust | 100 mB | 1,200 ticks | Starter input. |
| Syngas | 80 mB | 900 ticks | Faster intermediate input. |
| Carbon Monoxide | 500 mB | 600 ticks | Fastest, and burns through a lot of gas. Good for getting rid of spare Carbon Monoxide. |

Every recipe also uses 250 mB of water. Algae growth uses the `rngtech:algae_growth` recipe type.

{{ processing("algae_growth") }}

### Getting carbon gas

- **Carbon Exhaust**: make it in a [Wooden Dehumidifier](wooden-dehumidifier.md) from Coal Dust or Gasification Residue, or burn Syngas in a [Syngas Combustor](syngas-combustor.md). To bootstrap, craft a Carbon Exhaust Bucket from a Water Bucket and one {{ item('rngtech:coal_dust') }}. You get Coal Dust by crushing coal in a [Crusher](crusher.md).
- **Syngas**: from the [Coal Gasifier](coal-gasifier.md).
- **Carbon Monoxide**: a byproduct of the [Steam Methane Reformer](steam-methane-reformer.md).

### Gear

The Gear tab has one optional **Bio Chamber** slot. The chamber's fuel-efficiency rolls become *Bio Conversion*, which raises how much algae each cycle produces. Partial bonus algae carries over between cycles, so even a small bonus pays off over time. Without a chamber, each cycle makes exactly one Algae Biomass.

### Using Algae Biomass

- Burn it in the [Bio Generator](bio-generator.md) for 400 FE base.
- Craft four into one {{ item('rngtech:dense_algae_biomass') }}, worth 2,400 FE base in the Bio Generator instead of 1,600 FE for the four pieces burned separately.
- Melt it with Organic Reagent and water in the [Melter](melter.md) to make methane.

Neither algae item burns in a vanilla furnace or a [Solid Fuel Burner](solid-fuel-burner.md).

{{ crafting('rngtech:dense_algae_biomass') }}

You can also put Algae Biomass into the output slot yourself, by hand or from the sides, to top up a partial stack.

### Automation

| Side | Behavior |
|---|---|
| Top and sides | Fill the water and carbon tanks with fluid. Insert filled water or carbon-gas containers. Insert Algae Biomass into the output slot. |
| Bottom | Extracts Algae Biomass and empty containers. |

Pipes cannot drain fluid back out of the tanks. The Photobioreactor has no FE connection, and the Bio Chamber slot is not reachable by automation.

To throw fluid away, use the purge buttons beside each tank, or right-click the machine with a {{ item('rngtech:purge_bucket') }} to void up to 1,000 mB. Purging either tank resets the current growth cycle.

## Screen

The Algae Photobioreactor screen has three tabs:

- **Process**: water and carbon tanks with purge buttons, the container slots, the output slot, progress, and the light level. Hover for amounts and the light requirement.
- **Gear**: the Bio Chamber slot.
- **Stats**: the machine's current stats.

## Data values

{{ data_values() }}

## See also

- [Bio Generator](bio-generator.md)
- [Wooden Dehumidifier](wooden-dehumidifier.md): water and Carbon Exhaust.

{{ navbox() }}
