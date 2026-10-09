# Bio Generator

Status: Prototype

Player guide: [Bio Generator](https://c-pettersson.github.io/rngtech/bio-generator/)

The Bio Generator is an early organic-fuel FE generator registered as `rngtech:bio_generator`. It burns crop and prepared biomass fuels and uses a Bio Chamber Gear part for ingredient-aware power modifiers.

## Current Runtime Surface

| Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|
| `rngtech:bio_generator` | `BioGeneratorBlockEntity` | `BioGeneratorMenu` / `BioGeneratorScreen` | Top item input, side FE extraction | Item stack and placed machine |
| `rngtech:wooden_composter` | `WoodenComposterBlockEntity` | `WoodenComposterMenu` / `WoodenComposterScreen` | Top or side compostable item input, side water input, bottom item output | No |

The Bio Generator block has one Process fuel slot and two Gear slots:

| Slot | Accepted items | Runtime role |
|---|---|---|
| Battery Cell | Stage 0-3 `BatteryCellItem` stacks, including the Voltaic Potato Battery Cell | Adds portable FE capacity through the item energy capability. |
| Bio Chamber | `rngtech:bio_chamber` | Adds rolled Bio Generator part modifiers, including ingredient power prefixes and prepared-fuel suffixes. |

## Fuel Values

Fuel acceptance is tag-backed and server-authoritative.

| Fuel family | Tag | Base FE |
|---|---|---:|
| Potatoes | `rngtech:bio_generator/potato_fuels` | `120 FE` |
| Poisonous Potato | `minecraft:poisonous_potato` inside potato fuels | `200 FE` |
| Carrots | `rngtech:bio_generator/carrot_fuels` | `120 FE` |
| Bread | `rngtech:bio_generator/bread_fuels` | `240 FE` |
| Saplings | `rngtech:bio_generator/sapling_biomass` | `80 FE` |
| Seeds | `rngtech:bio_generator/seed_biomass` | `40 FE` |
| Other plant biomass | `rngtech:bio_generator/plant_biomass` | `80 FE` |
| Organic Reagent | `rngtech:bio_generator/organic_reagents` | `546 FE` |
| Composted Biomass | `rngtech:bio_generator/composted_biomass` | `1,600 FE` |
| Algae Biomass | `rngtech:bio_generator/algae_biomass` | `400 FE` |
| Dense Algae Biomass | `rngtech:bio_generator/dense_algae_biomass` | `2,400 FE` |
| Rich Biomass | `rngtech:bio_generator/rich_biomass` | `3,200 FE` |

Golden foods are excluded through `rngtech:bio_generator/excluded_foods`. JEI exposes the fuels as a recipe-like Bio Generator Fuels category.

## Wooden Composter

Player guide: [Wooden Composter](https://c-pettersson.github.io/rngtech/wooden-composter/)

The passive Wooden Composter is the Stage 1-2 Bio Generator support path. It has no FE input or output.

Runtime behavior:

| Surface | Current behavior |
|---|---|
| Block id | `rngtech:wooden_composter` |
| Stage | Stage 1 support, with no FE input or output |
| Inventory | `27` compostable input slots, one Composted Biomass output slot, one water-container input slot, and one container-remainder output slot |
| Water | `4,000 mB` input-only water tank; filled by water containers or side fluid automation |
| Batch | `8` compost units produce `1` Composted Biomass; normal compostables count as `1` unit and prepared inputs count as `2` units |
| Dry cycle | `2,400 ticks` |
| Wet cycle | `1,200 ticks` and consumes `10 mB` water at batch start |
| Bulk bonus | At least `200` compost units in the input buffer adds `+10%` composting speed |
| Variety bonus | At least `5` different compostable item types in the input buffer adds `+10%` composting speed |
| Output role | Prepared Bio Generator fuel, not vanilla furnace or solid fuel-burning generator fuel |

The Composter accepts the `rngtech:wooden_composter/inputs` tag plus items registered as compostable through the vanilla/NeoForge compostable data map. Items tagged `rngtech:wooden_composter/prepared_inputs` count as `2` compost units each. JEI exposes a recipe-like Wooden Composting category.

Prepared biomass fuels use Bio Generator-only tags and explicit values:

| Fuel | Stage | Source | Base FE | Notes |
|---|---:|---|---:|---|
| Composted Biomass | 1 | Wooden Composter output | `1,600 FE` | Stable early fuel for battery-buffered Bio Generator setups. |
| Algae Biomass | 3 | Algae Photobioreactor output | `400 FE` | Lower-value direct fuel and Melter feedstock for methane chemistry. |
| Dense Algae Biomass | 4 | Shapeless craft from four Algae Biomass | `2,400 FE` | More efficient algae fuel for cleaner Bio Generator logistics. |
| Rich Biomass | 2 | Shapeless craft from Composted Biomass plus `rngtech:bio_generator/rich_biomass_catalysts` | `3,200 FE` | Denser Stage 2 fuel that rewards basic farm and mob-drop infrastructure. |

The default Rich Biomass catalyst tag includes Bone Meal, Organic Reagent, and Gunpowder. Composted Biomass and Rich Biomass are not tagged as vanilla furnace fuel, solid fuel-burning generator fuel, or broad modded fuel.

## Algae Photobioreactor

The Stage 3-4 greenhouse support path is the [Algae Photobioreactor](algae-photobioreactor.md), which grows Algae Biomass for direct fuel, Dense Algae Biomass crafting, and Melter methane chemistry.

## Crusher Biomass

Stage 0 Crusher recipes convert potatoes, carrots, and crushable non-seed plant biomass into `rngtech:organic_reagent`. Each Organic Reagent conversion is authored as `120 ticks` and `600 FE`.

Seeds have a separate Stage 0 Crusher recipe into `rngtech:compost_feedstock`. One seed produces one Compost Feedstock over `16 ticks` for `80 FE`; Compost Feedstock is a Wooden Composter prepared input worth `2` compost units. That makes crushing seeds first energy-positive once the resulting Composted Biomass is burned:

| Seed compost chain | Result |
|---|---:|
| `8` seeds directly composted | `1` Composted Biomass = `1,600 FE` |
| `8` seeds crushed first | `8` Compost Feedstock = `16` compost units = `2` Composted Biomass |
| Crusher cost | `640 FE` |
| Crushed-seed value after Crusher cost | `2,560 FE` |
| Gain over direct composting | `960 FE` |

At base energy stats, converting a potato or carrot into Organic Reagent is an energy-negative processing step before Bio Generator modifiers:

| Step | Value |
|---|---:|
| Direct potato burn | `120 FE` |
| Crusher recipe time | `120 ticks` |
| Crusher recipe energy | `600 FE` |
| Organic Reagent burn | `546 FE` |
| Net after crushing | `-54 FE` |

Compared with burning the potato directly, the conversion path is `174 FE` behind before any Bio Chamber or Bio Generator modifiers are applied.

Processing speed does not change the recipe's base total FE cost. Faster machines finish in fewer ticks and draw proportionally more FE per active tick; slower machines draw less FE per tick over more ticks. Crusher `ENERGY_USAGE` can still change the effective total FE per craft.

| Setup | Effective time | FE/t | FE per craft | Net after crushing |
|---|---:|---:|---:|---:|
| Base `1.0x` speed and energy stats | `120 ticks` | `5 FE/t` | `600 FE` | `-54 FE` |
| `2.0x` processing speed and base energy stats | `60 ticks` | `10 FE/t` | `600 FE` | `-54 FE` |

If a setup consumes a flat FE/t value for every adjusted tick and changes total FE only because the machine is slower or faster, it is not following the current processing-machine energy model.

## Ingredient Modifiers

The Bio Generator profile can roll general generator stats such as `ENERGY_GENERATION`, `ENERGY_CAPACITY`, `FUEL_EFFICIENCY`, and `EFFICIENCY`. On Bio Generators, `ENERGY_GENERATION` changes FE/t power, while fuel value and `FUEL_DURATION` control burn length. The Bio Generator has no machine-side export cap, so it does not roll `ENERGY_TRANSFER`; the attached receiver or [Universal Connector tier](basic-wire.md#energy-transfer-limits) decides the export rate, and the Output readout shows the last tick's actual FE output.

The Bio Chamber profile can roll generic suffix modifiers:

- `ENERGY_GENERATION`
- `FUEL_DURATION`

`ENERGY_GENERATION` increases Bio Generator FE/t power. `FUEL_DURATION` increases burn ticks without increasing FE/t.

It can also roll ingredient-aware crop-family prefix modifiers:

- `POTATO_POWER`
- `CARROT_POWER`
- `BREAD_POWER`
- `SAPLING_POWER`
- `SEED_POWER`

It can also roll prepared-fuel suffix modifiers:

- `PLANT_POWER`
- `ORGANIC_REAGENT_POWER`
- `COMPOSTED_BIOMASS_POWER`
- `ALGAE_POWER`
- `RICH_BIOMASS_POWER`

These multipliers apply only when the active fuel belongs to the matching tag family. Specific families win over broader tags, so saplings and seeds use their own crop-family stats instead of generic `PLANT_POWER`; Dense Algae Biomass shares `ALGAE_POWER` with normal Algae Biomass.

## Automation

| Block | Item surface | Fluid surface | Energy surface |
|---|---|---|---|
| Bio Generator | Top inserts accepted fuels; no item extraction | None | Sides extract generated FE; no external FE input |
| Wooden Composter | Top or sides insert `rngtech:wooden_composter/inputs` items and filled water containers; bottom extracts Composted Biomass and container remainders | Sides fill the input-only water tank | None |

Gear slots are player-managed through the UI.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
- [Battery Cells](battery-cells.md)
- [Machine Stats](../reference/machine-stats.md)
