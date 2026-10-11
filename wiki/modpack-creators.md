---
wiki:
  category: Guides
  icon: rngtech:gold_energy_connector
---

# Modpack Creators

Use this page to fit RNGTech into your modpack. It lists the energy and water numbers behind RNGTech's balance, and the settings you can change without code.

!!! summary "At a glance"
    - **Energy** is plain FE. At each stage, one main generator powers about one typical machine.
    - **Ore to ingots** costs about 9,000 FE per ingot at Stage 1 and about 70,000 at Stage 8.
    - **Water is finite** in RNGTech's balance. The mod still accepts infinite water, so turn it off in your pack.
    - **Uniques:** you can move them to quests or machine challenges and add your own. See [Uniques](#uniques).
    - **Settings** live in `config/rngtech-common.toml`. You change recipes with an ordinary datapack.

## Energy

### How power works

- RNGTech uses standard FE, so its generators and machines connect to other mods' FE devices.
- A recipe sets the **total FE for one craft**. Speed upgrades raise a machine's FE/t and keep the FE per craft the same. The Energy Usage stat changes FE per craft.
- The Energy Connector in a [Universal Cable](universal-cable.md) connector caps how much FE moves. Machines and generators take and send as much as that connector allows.

| Energy Connector | FE/t |
|---|---:|
| {{ item('rngtech:crude_energy_connector') }} | 64 |
| {{ item('rngtech:basic_energy_connector') }} | 128 |
| {{ item('rngtech:copper_energy_connector') }} | 512 |
| {{ item('rngtech:gold_energy_connector') }} | 2,048 |
| {{ item('rngtech:sparksteel_energy_connector') }} | 8,192 |
| {{ item('rngtech:arclite_energy_connector') }} | 32,768 |
| {{ item('rngtech:exotic_energy_connector') }} | 1,000,000 |

### Power per stage

These numbers use freshly crafted parts and no Mastery. Rolled affixes and Mastery raise or lower them.

| Stage | Main generator | Generator FE/t | Typical machine draw |
|---:|---|---:|---:|
| 1 | [Solid Fuel Burner](solid-fuel-burner.md), Iron Heat Core | 24 | 24–48 FE/t |
| 2 | Solid Fuel Burner, Copper Heat Core | 40 | 24–64 FE/t |
| 3 | Solid Fuel Burner, Bronze Heat Core | 64 | 48–96 FE/t |
| 4 | Solid Fuel Burner, Steel Heat Core | 96 | about 96 FE/t |
| 5 | [Syngas Combustor](syngas-combustor.md) | 240 | 128–384 FE/t |
| 6 | [Ammonia Fuel Cell](ammonia-fuel-cell.md) | 600 | 160–512 FE/t |
| 7 | [Vacuum Collapse Generator](vacuum-collapse-generator.md) | about 1,057 | 512–3,072 FE/t |
| 8 | Vacuum Collapse Generator, Exotic parts | about 3,000 | 2,048–8,192 FE/t |

[Solar Panels](solar-panel.md), the [Bio Generator](bio-generator.md), the [Corrosion Cell](corrosion-cell.md), and the [Cavitation Generator](cavitation-generator.md) add power on the side. The [Potential Reactor](potential-reactor.md) turns retired Gear into FE; count it as a bonus.

### Energy per ingot

Each row crushes ore down to dust in two steps and smelts the dust, with a Crusher and Furnace from that stage.

| Stage | Ore | Ingots per ore | FE per ingot | Ingots/min one generator powers |
|---:|---|---:|---:|---:|
| 1 | Iron | 3 | 9,280 | 3 |
| 2 | Copper | 3 | 8,612 | 6 |
| 3 | Tin | 3.6 | 8,300 | 9 |
| 4 | Lead | 3.4 | 7,532 | 15 |
| 5 | Aluminum | 3 | 15,559 | 19 |
| 6 | Titanium | 3.8 | 22,533 | 32 |
| 7 | Tungsten | 4.6 | 44,090 | 29 |
| 8 | Naquadah | 4.3 | 70,608 | 51 |

- Smelting raw ore skips the Crusher. It costs less FE per ingot and gives 1 ingot per ore.
- A machine built for Energy Usage at Mastery 60 spends about **a quarter** of this FE.
- Stage 5–8 ore recipes cost 2x, 3x, 4x, and 6x the early FE. The hardest ores and hottest smelts cost double on top; you can change that surcharge in the config.

### Scaling energy

RNGTech has no global FE multiplier. To scale the whole mod, multiply generator and machine recipes by the same factor so the stages stay in step.

| You want to change | How |
|---|---|
| FE cost of a machine recipe | Override the recipe in a datapack and change `energy` (total FE per craft) or `processing_ticks`. |
| FE output of a fuel generator | Override its recipe and change `energy`: `gas_combustion` (Syngas), `ammonia_power_cycle`, `vacuum_collapse`, `cavitation`, `corrosion_cell`, `potential_reactor`. |
| Solid Fuel Burner FE per item | Each fuel gives 10 FE per burn tick (Coal: 16,000 FE). Change the burn time in NeoForge's `furnace_fuels` data map. This also changes vanilla furnaces. |
| Other mods' fuels in the Solid Fuel Burner | Add the item to a tier tag `rngtech:solid_fuel/tier_1` to `tier_4`, plus `rngtech:solid_fuel/item_fuels` or `block_fuels`. |
| Bio Generator fuels | Add the item to a `rngtech:bio_generator/*` tag. It gives that category's fixed FE, such as `potato_fuels` (120 FE). |
| Surcharge on the hardest ores and hottest smelts | `crusher.highHardnessEnergyMultiplier` and `furnace.highHeatEnergyMultiplier` (default 2.0). |
| Resonance Calibrator cost | `calibration.energyMultiplier` and `calibration.timeMultiplier`. |
| Exotic Affix Forge cost | `exoticAffixForge.energyMultiplier` and `exoticAffixForge.timeMultiplier`. |
| Solar in other dimensions | `solar.dimensions.overworld`, `nether`, `end`, `other`. The Overworld is the default. |

You cannot change these without code: connector caps, Battery Cell sizes, Heat Core FE/t, Solar Panel FE/t, and Bio Generator FE per category.

!!! warning "Recipe gotchas"
    - If `energy` holds an invalid value, such as `0` or a negative number, most recipe types fall back to their default without an error. RNGTech ignores misspelled fields. Check the value in JEI after `/reload`.
    - A craft in progress keeps its old FE cost until it finishes.

## Water

!!! danger "Plan for no infinite water"
    RNGTech's water numbers assume finite water, and water costs players real effort from Stage 5 on. RNGTech machines accept water from buckets and from other mods' pipes, so you turn infinite water off yourself:

    - Set the vanilla gamerule `waterSourceConversion` to `false`. A 2×2 pool then stops making new sources.
    - Remove or gate other mods' infinite-water blocks, such as aqueous accumulators and pumps that treat large lakes as endless.

### Making water

| Source | Stage | Water | Cost |
|---|---:|---:|---|
| [Wooden Dehumidifier](wooden-dehumidifier.md) | 2–3 | Up to 1.25 mB/t (25 frames), less in dry biomes | No FE; needs sky access and room for its frames |
| [Silica Gel Dehumidifier](silica-gel-dehumidifier.md), Silica Gel Beads | 5 | 2.5 mB/t (3 lanes) | About 30 FE per mB to recharge beads |
| Silica Gel Dehumidifier, Supercharged Beads | 6 | 8.3 mB/t (3 lanes) | About 18 FE per mB to recharge beads |

RNGTech has no other water source.

### Using water

Each generator below needs this much water to keep running, counting its whole fuel chain:

| Generator | Stage | Water |
|---|---:|---:|
| [Syngas Combustor](syngas-combustor.md), fed by a [Coal Gasifier](coal-gasifier.md) | 5 | about 2.1 mB/t |
| [Cavitation Generator](cavitation-generator.md), water rotor | 5 | about 4.2 mB/t |
| [Ammonia Fuel Cell](ammonia-fuel-cell.md), whole Ammonia chain | 6 | about 6.3 mB/t |

A full Wooden Dehumidifier (1.25 mB/t) falls short of one Syngas Combustor. An Ammonia Fuel Cell uses most of a Supercharged Silica Gel Dehumidifier.

Other machines use less:

| Machine | Water |
|---|---|
| [Melter](melter.md) (Lubricant, Electrolyte, Methane, Lava) | 1,000 mB per melt, about 3–4.5 mB/t per melt running |
| [Steam Methane Reformer](steam-methane-reformer.md) | 500 mB per craft, about 1.4–1.7 mB/t |
| [Algae Photobioreactor](algae-photobioreactor.md) | 250 mB per Algae Biomass, about 0.2–0.4 mB/t |
| [Forestry Cart](forestry-cart-station.md) sprinkler | up to about 8 mB/t for a full field |
| [Wooden Composter](wooden-composter.md) | 10 mB per batch (optional, doubles speed) |

## Uniques

By default, Uniques drop from vanilla structures and mobs. The [Uniques](uniques.md) page lists where.

- **Turn off default drops:** `uniques.loot.enabled`, or one Unique with `uniques.loot.<id>.enabled`.
- **Give one:** `/rngtech unique give <players> <id>`, or use the item as a quest reward. Each player rolls their own copy when they identify it.
- **Machine challenges:** RNGTech rolls a loot table on machine events, such as a Crusher jam or a finished calibration. The tables ship empty; fill them to reward players for running the machines.
- **Add your own Uniques:** put a JSON file in `config/rngtech/uniques/`.

The [Uniques guide for pack makers](https://github.com/c-pettersson/rngtech/blob/main/extras/uniques/README.md) covers the file format and rules.

## Other settings

These live in `config/rngtech-common.toml`.

| Setting | Default | What it does |
|---|---|---|
| `materials.<material>` | on | Turn off to stop the material's recipes loading. Its items show as disabled. |
| `worldgen.ores.enabled` | on | RNGTech ore generation in new chunks. |
| `worldgen.ores.<material>.enabled` | on | One ore's generation. |
| `worldgen.ores.vanillaToolBridgePolicy` | `EARLY_BRIDGE` | How far vanilla pickaxes reach on RNGTech ores (see below). |
| `ascendancy.sealRecipesEnabled` | on | Turn off to hand out Ascendancy Seals by quest or loot instead. |
| `corruption.volatileCatalystRecipesEnabled` | on | Turn off to hand out Volatile Catalysts by quest or loot instead. |

**Vanilla pickaxes and ore hardness.** Each metal ore has an ore hardness: 1 for Iron and Copper, 2 for Gold, Tin, and Zinc, and up to 8 for Naquadah. The [Modular Pick](tool-bench.md) reaches every level. The policy sets how far vanilla pickaxes reach:

| Policy | Vanilla pickaxes can mine |
|---|---|
| `MODULAR_ONLY` | None, including Iron and Copper ore. |
| `BOOTSTRAP_ONLY` | Iron and Copper ore. |
| `EARLY_BRIDGE` (default) | Stone pickaxe: Iron and Copper. Iron pickaxe or better: also Gold, Tin, and Zinc. |
| `VANILLA_TIER_MAPPING` | Same as `EARLY_BRIDGE`, but Diamond reaches Aluminum and Osmium, and Netherite reaches Titanium. |

Wooden and golden pickaxes can't mine metal ores.

## Compatibility

- **Other mods' metals:** RNGTech recipes use common tags such as `c:ingots/tin`, so other mods' tin works in them.
- **AE2 and Refined Storage:** Universal Cables can link their networks.
- **JEI and Jade** show RNGTech recipes and machine state.
- **FTB Quests:** the repository has a ready-made quest line in [extras/ftbquests](https://github.com/c-pettersson/rngtech/tree/main/extras/ftbquests). Copy its `config/ftbquests` into your pack.

## See also

- [Getting Started](getting-started.md)
- [Stages](stages.md)
- [Uniques](uniques.md)
- [Universal Cable](universal-cable.md)

{{ navbox() }}
