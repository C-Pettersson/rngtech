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
    - **Uniques:** you can move them to quests and add your own. See [Uniques](#uniques).
    - **Ore progression:** ore hardness covers other mods' ores, and you can weaken or disable vanilla tools. See [Strict preset](#strict-preset).
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
| 6 | [Fuel Cell](ammonia-fuel-cell.md) | 600 | 160–512 FE/t |
| 7 | [Vacuum Collapse Generator](vacuum-collapse-generator.md) | about 1,057 | 512–3,072 FE/t |
| 8 | Vacuum Collapse Generator, Exotic parts | about 3,000 | 2,048–8,192 FE/t |

Vacuum Collapse figures are averages at the default rated depth. Each collapse climbs to twice its average at the end of the cycle, and players can trade Void Catalyst for a harder peak with the collapse depth setting.

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
| FE output of a fuel generator | Override its recipe and change `energy`: `gas_combustion` (Syngas), `ammonia_power_cycle` (Ammonia and Hydrogen in the Fuel Cell), `vacuum_collapse`, `cavitation`, `corrosion_cell`, `potential_reactor`. |
| Solid Fuel Burner FE per item | Each fuel gives 10 FE per burn tick (Coal: 16,000 FE). Change the burn time in NeoForge's `furnace_fuels` data map. This also changes vanilla furnaces. |
| Other mods' fuels in the Solid Fuel Burner | Add the item to a tier tag `rngtech:solid_fuel/tier_1` to `tier_4`, plus `rngtech:solid_fuel/item_fuels` or `block_fuels`. |
| Bio Generator fuels | Add the item to a `rngtech:bio_generator/*` tag. It gives that category's fixed FE, such as `potato_fuels` (120 FE). |
| Surcharge on the hardest ores | `crusher.highHardnessEnergyMultiplier` (default 2.0). Heated recipes carry their own `heat` in heat units instead; a datapack recipe that still sets `energy` in FE loads with 0.6 heat per FE. |
| Resonance Calibrator cost | `calibration.energyMultiplier` and `calibration.timeMultiplier`. |
| Solar in other dimensions | `solar.dimensions.overworld`, `nether`, `end`, `other`. The Overworld is the default. |

You cannot change these without code: connector caps, Battery Cell sizes, Heat Core FE/t, Solar Panel FE/t, and Bio Generator FE per category.

!!! warning "Recipe gotchas"
    - If `energy` holds an invalid value, such as `0` or a negative number, most recipe types fall back to their default without an error. RNGTech ignores misspelled fields. Check the value in JEI after `/reload`.
    - A craft in progress keeps its old FE cost until it finishes.
    - Melter and Alloy Furnace recipes are shapeless: slot order does not matter, and a Melter recipe whose `primary_ingredient` and `secondary_ingredient` both match one item takes two of it from one slot. Component Assembler `ingredients` are ordered, one input slot per entry.

## Water

!!! danger "Plan for no infinite water"
    RNGTech's water numbers assume finite water, and water costs players real effort from Stage 5 on. RNGTech machines accept water from buckets and from other mods' pipes, so you turn infinite water off yourself:

    - Set the vanilla gamerule `waterSourceConversion` to `false`. A 2×2 pool then stops making new sources.
    - Remove or gate other mods' infinite-water blocks, such as aqueous accumulators and pumps that treat large lakes as endless.

### Making water

| Source | Stage | Water | Cost |
|---|---:|---:|---|
| [Wooden Dehumidifier](wooden-dehumidifier.md) | 2–3 | Up to 1.25 mB/t, less in dry biomes | No FE; needs sky access and room for its 3×3×3 frame cube |
| [Silica Gel Dehumidifier](silica-gel-dehumidifier.md), Silica Gel Beads | 5 | 2.5 mB/t (3 lanes) | About 30 FE per mB to recharge beads |
| Silica Gel Dehumidifier, Supercharged Beads | 6 | 8.3 mB/t (3 lanes) | About 18 FE per mB to recharge beads |

RNGTech has no other water source.

### Using water

Each generator below needs this much water to keep running, counting its whole fuel chain:

| Generator | Stage | Water |
|---|---:|---:|
| [Syngas Combustor](syngas-combustor.md), fed by a [Coal Gasifier](coal-gasifier.md) | 5 | about 2.1 mB/t |
| [Cavitation Generator](cavitation-generator.md), water rotor | 5 | about 4.2 mB/t |
| [Fuel Cell](ammonia-fuel-cell.md), whole Ammonia chain | 6 | about 6.3 mB/t |

A full Wooden Dehumidifier (1.25 mB/t) falls short of one Syngas Combustor. A Fuel Cell uses most of a Supercharged Silica Gel Dehumidifier.

Other machines use less:

| Machine | Water |
|---|---|
| [Melter](melter.md) (Lubricant, Electrolyte, Methane, Lava) | 1,000 mB per melt, about 3–4.5 mB/t per melt running |
| [Steam Methane Reformer](steam-methane-reformer.md) | 500 mB per craft, about 1.4–2.1 mB/t |
| [Algae Photobioreactor](algae-photobioreactor.md) | 250 mB per Algae Biomass, about 0.2–0.4 mB/t |
| [Forestry Cart](forestry-cart-station.md) sprinkler | up to about 8 mB/t for a full field |
| [Wooden Composter](wooden-composter.md) | 10 mB per batch (optional, doubles speed) |

## Uniques

By default, Uniques drop from vanilla structures and mobs. The [Uniques](uniques.md) page lists where.

- **Turn off default drops:** `uniques.loot.enabled`, or one Unique with `uniques.loot.<id>.enabled`.
- **Give one:** `/rngtech unique give <players> <id>`, or use the item as a quest reward. Each player rolls their own copy when they identify it.
- **Give a Glyph:** `/rngtech glyph give <players> <tier>` gives a Glyph of tier 1 to 5. Machines do not drop loot on their own events, so quests are the way to reward machine play.
- **Add your own Uniques:** put a JSON file in `config/rngtech/uniques/`.

The [Uniques guide for pack makers](https://github.com/c-pettersson/rngtech/blob/main/extras/uniques/README.md) covers the file format and rules.

## Dormant Machines

[Dormant Machines](dormant-machines.md) hold a Mastery soul for players to wake and take. RNGTech generates six kinds of site; turn that off with `dormantMachines.enabled`. You can also place them in your own structures or with a command:

```
/setblock ~ ~ ~ rngtech:dormant_machine{Site:"rngtech:end_relay"}
```

- **Sites:** each site is a JSON file in `data/<namespace>/dormant_machine/`. It sets the machine families with weights, the soul level range, the wake FE range and curve, the FE input per tick, and optionally `release_stage`, the lowest stage a soul may be released into (none by default; it never goes above the highest stage the family reaches). Override the six default files in a datapack, or add your own. A machine with no `Site` uses `rngtech:collapsed_workshop`.
- **How often sites appear:** override `data/rngtech/worldgen/structure_set/<site>.json` in a datapack. `spacing` is the size, in chunks, of the region each site gets one try in; `separation` is the minimum gap between two of them. Each try can still fail on biome or terrain, so the surface and ocean sites turn up less often than their spacing suggests, while the underground ones almost always succeed.
- **Builds:** a site may list builds per family as target nodes; souls follow the path through them. Without a list, each soul walks to nearby notables its family can use.
- **Failed absorbs:** `dormantMachines.failedAbsorb` (below).
- **Reroll souls:** `/rngtech dormant reset` gives untouched machines a new random soul, for example after you change a site file. `reset force` also rerolls machines players have started charging, and empties their charge.
- **Bring sites back:** `/rngtech dormant restore same` puts every site back with its last soul, husks included, asleep and empty. `restore random` does the same with new souls. A site whose husk was mined comes back only on empty ground; the command lists the ones it skipped. Sites in unloaded chunks change the next time their chunk loads.

## Other settings

These live in `config/rngtech-common.toml`.

| Setting | Default | What it does |
|---|---|---|
| `materials.<material>` | on | Turn off to stop the material's recipes loading. Its items show as disabled. |
| `worldgen.ores.enabled` | on | RNGTech ore generation in new chunks. |
| `worldgen.ores.<material>.enabled` | on | One ore's generation. |
| `worldgen.ores.vanillaToolBridgePolicy` | `EARLY_BRIDGE` | How far tools other than Modular tools reach on ore hardness. See [Tool reach](#tool-reach). |
| `restrictedTools.mode` | `NORMAL` | `WEAKENED` or `USELESS` vanilla tools. See [Restricted tools](#restricted-tools). |
| `restrictedTools.weakenedSpeedMultiplier` | 0.25 | Mining speed of restricted tools in `WEAKENED` mode. |
| `restrictedTools.vanillaRecipesEnabled` | on | Turn off to remove the stone to netherite pickaxe, axe, and shovel recipes. |
| `ascendancy.sealRecipesEnabled` | on | Turn off to hand out Ascendancy Seals by quest or loot instead. |
| `soulTransfer.alwaysSucceeds` | off | Turn on so every [Soul Transfer](machine-mastery.md#soul-transfer) absorb succeeds, whatever its calibration. A Soul Transfer from a quest, loot, or `/give` has no calibration and always succeeds anyway. |
| `soulTransfer.failureXpLossPercent` | 0 | Percent of a machine's Mastery XP lost when a Soul Transfer absorb fails. Late levels need far more XP than early ones, so a high-level machine loses only a few levels unless this is near 100. |
| `soulTransfer.failureLevelLoss` | 0 | Mastery levels lost when a Soul Transfer absorb fails, applied after any XP loss. |
| `dormantMachines.enabled` | on | Generate Dormant Machine sites in new chunks. Sites already generated, and machines you place, keep working. |
| `dormantMachines.failedAbsorb` | `NOTHING` | What a failed Soul Transfer absorb does to a [Dormant Machine](dormant-machines.md). `NOTHING` keeps the soul (the `soulTransfer` loss settings still apply to it), `HUSK` destroys it, and `EXPLODE` destroys it in a TNT-strength explosion. The Soul Transfer is used up either way. |
| `corruption.volatileCatalystRecipesEnabled` | on | Turn off to hand out Volatile Catalysts by quest or loot instead. |

When a failed Soul Transfer costs levels, the machine refunds its latest-allocated nodes first. It skips any node the rest of the tree or the installed Gear still needs, so the tree stays connected and the Gear stays legal. If no node can safely go, the machine keeps the level its tree needs. The old build becomes the machine's followed build, so it fills back in as the machine earns XP again.

Each player also has `config/rngtech-client.toml`. Its `solar.animatedPanels` (default on) turns [Solar Panel](solar-panel.md) decks smoothly with the sun; turn it off for panels that tilt in steps, which is lighter on very large fields. It takes effect after a resource reload (F3 + T).

## Ore progression

Players move through RNGTech's stages by mining harder ores with better [Modular tools](modular-tools.md). You can make that the only way through, including for other mods' ores and tools.

### Ore hardness

Each metal has an ore hardness from 1 to 8: 1 for Iron and Copper, 2 for Gold, Tin, and Zinc, and up to 8 for Naquadah. The [Ore hardness](modular-tools.md#ore-hardness) table lists which tool heads reach each level.

- **Other mods' ores** of the same metal get the same hardness. The block tags `rngtech:ore_hardness/level_1` to `level_8` include each metal's common tag, such as `c:ores/tin`. This also gates Nether Gold Ore, which is in `c:ores/gold`.
- **Add an ore** RNGTech doesn't know, such as Diamond Ore, by adding it to a level tag.
- **Raise an ore** by adding it to a higher level tag. An ore in several level tags uses the highest one.
- **Lower an ore** by overriding its level tag with `"replace": true`, leaving the ore out, then adding it to the lower tag.
- **The Crusher** takes any mod's ore of a metal by its common tag. An ore in `c:ores_in_ground/deepslate` uses the slower deepslate recipe.

Hardness applies when a player breaks the ore, including the fake players many mods' machines use. Explosions, and machines that break blocks without a player, still get the drops. Gate such machines in a strict pack.

### Tool reach

Modular tools reach their head's hardness. Every other item reaches as far as `worldgen.ores.vanillaToolBridgePolicy` allows for its vanilla tier. RNGTech reads the tier from which vanilla blocks the item can harvest, so other mods' pickaxes and drills follow the same policy as vanilla pickaxes.

| Policy | Stone tier | Iron tier | Diamond tier | Netherite Pickaxe |
|---|---:|---:|---:|---:|
| `MODULAR_ONLY` | none | none | none | none |
| `BOOTSTRAP_ONLY` | 1 | 1 | 1 | 1 |
| `EARLY_BRIDGE` (default) | 1 | 2 | 2 | 2 |
| `VANILLA_TIER_MAPPING` | 1 | 2 | 4 | 5 |

Wooden and golden pickaxes, axes, and shovels reach no ore hardness. Only the Netherite Pickaxe counts as netherite tier, because vanilla has no block that needs one. Give another mod's tool its own reach in the `rngtech:ore_reach` item data map, at `data/rngtech/data_maps/item/ore_reach.json` in your datapack. A listed item ignores the policy:

```json
{
  "values": {
    "examplemod:mining_drill": { "level": 5 },
    "#examplemod:starter_picks": { "level": 1 }
  }
}
```

### Restricted tools

The item tag `rngtech:restricted_tools` lists the stone to netherite pickaxes, axes, and shovels. Wooden tools stay usable in every mode: they reach no ore hardness, so they bypass nothing. Add other mods' tools to it, or remove ones you want to keep. `restrictedTools.mode` sets what happens to them:

| Mode | Restricted tools |
|---|---|
| `NORMAL` (default) | Work as usual. |
| `WEAKENED` | Mine at `restrictedTools.weakenedSpeedMultiplier` of their speed (default 0.25). |
| `USELESS` | Mine and harvest like an empty hand, reach no ore hardness, and can't strip logs or make paths. They still work as weapons. |

In the last two modes, restricted tools say so in their tooltip. `restrictedTools.vanillaRecipesEnabled` (default on) removes the vanilla crafting and smithing recipes for those 15 tools when you turn it off; wooden tools, hoes, and swords keep theirs. Tools from chests, villagers, and mobs still turn up, which is why the mode exists.

Players don't need restricted tools to start: the [Tool Bench](tool-bench.md) needs only oak slabs, a crafting table, and gravel, and a Flint Pick mines stone, coal, and hardness 1 ores.

### Strict preset

For a pack where RNGTech's Modular tools are the only way to mine, put this in `config/rngtech-common.toml`:

```toml
[worldgen.ores]
    vanillaToolBridgePolicy = "MODULAR_ONLY"

[restrictedTools]
    mode = "USELESS"
    vanillaRecipesEnabled = false
```

Then add other mods' mining tools to `rngtech:restricted_tools`, or give them a level in `rngtech:ore_reach`, and gate other mods' block-breaking machines.

## Phase Array

The [Phase Array](phase-array.md)'s operations are recipes of type `rngtech:phase_array`, one file per operation under `data/rngtech/recipe/phase_array/`. Each names its `action` (`reforge`, `attune`, `resonate`, `excise`, `imprint`, `transmute`, `temper`, `exalt`, or `graft`), its `coils` (3 to 9), its `glyph_tier` (0 for none, else 1 to 5), its `base_fe`, its `refinement_potential_cost`, and its `partial` and `overhold` outcomes. Override a file to retune an operation, replace it with an empty recipe to remove it, or add a file with another action to offer the same action at a second price. A recipe condition such as `rngtech:material_enabled` hides an operation the way it hides a crafting recipe.

The line's own numbers live under `phaseArray.*` in `config/rngtech-common.toml`:

| Setting | Default | What it does |
|---|---|---|
| `phaseArray.coilHoldChance` | 92 | Percent chance a Coil holds the pulse, before part affixes. |
| `phaseArray.coilOverholdChance` | 8 | Percent chance a surplus Coil's hold triggers Overhold, before part affixes. |
| `phaseArray.partialWindow` | 2 | Coils short of an operation's count that still count as Partial. |
| `phaseArray.fracturePerStrainPercent` | 1 | Percent Fracture chance per point of Strain past the line's Tolerance. |
| `phaseArray.qualityBase`, `phaseArray.qualityDivisor` | 1.5, 3.5 | A pulse's FE multiplies by the base for every divisor points of Quality, the sum of the item's affix tiers. |
| `phaseArray.stageFactor.stage0to2` to `stage8` | 0.1, 0.25, 0.5, 1, 1.5 | The FE factor for the item's stage. |
| `phaseArray.emitterChargeCapacity`, `phaseArray.emitterChargeRate` | 16,000,000 FE, 8,000 FE/t | What the Emitter adds to the line's store and rate, before affixes. |
| `phaseArray.coilChargeCapacity`, `phaseArray.coilChargeRate` | 4,000,000 FE, 2,000 FE/t | What each Coil adds, before affixes. |
| `phaseArray.glyphFloors` | `[1, 1, 1, 2, 2, 3, 3, 4, 5]` | The lowest Glyph tier each position needs, nine entries from the position next to the Emitter outward. |

Refinement Potential is shared with the Affix Forge: a Stage 8 part rolls 22 to 32 at craft, `calibration.refinementPotentialBonus` shifts every roll, and each operation's `refinement_potential_cost` is in its recipe.

Glyphs (`rngtech:glyph`) have no recipe. Their tier is the `rngtech:glyph_tier` component, 1 to 5; a stack without it counts as Tier I, so `/give @p rngtech:glyph[rngtech:glyph_tier=3]` or a quest reward works. By default only woken Dormant Machines drop them, and Tier V has no default source. Hand Glyphs out through quests to change that.

## Compatibility

- **Other mods' metals:** RNGTech recipes use common tags such as `c:ingots/tin` and `c:ores/tin`, so other mods' tin works in them. Their ores share RNGTech's [ore hardness](#ore-hardness).
- **AE2 and Refined Storage:** Universal Cables can link their networks.
- **JEI and Jade** show RNGTech recipes and machine state.
- **FTB Quests:** the repository has a ready-made quest line in [extras/ftbquests](https://github.com/c-pettersson/rngtech/tree/main/extras/ftbquests). Copy its `config/ftbquests` into your pack.

## See also

- [Getting Started](getting-started.md)
- [Stages](stages.md)
- [Uniques](uniques.md)
- [Universal Cable](universal-cable.md)

{{ navbox() }}
