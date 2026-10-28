# RNGTech in FTB Evolution

This folder adds RNGTech to [FTB Evolution](https://www.feed-the-beast.com/modpacks/125-ftb-evolution). It is
optional pack-maker content and is not part of RNGTech's runtime.

Tested with FTB Evolution 1.45.1 (Minecraft 1.21.1, NeoForge 21.1.250) and RNGTech 2.0.0-alpha.3 on a dedicated
server: the pack loads with no KubeJS errors, and the checks below were run in game.

## Install

Stop the client or server first. FTB Quests saves its quest book on shutdown and overwrites a merge made while it
runs.

1. Put `rngtech-<version>.jar` in the pack's `mods` folder.
2. Copy `config/rngtech-common.toml` into the pack's `config` folder. If the pack already has one, set the same keys.
3. Copy `kubejs/server_scripts/rngtech` into the pack's `kubejs/server_scripts`.
4. Merge the RNGTech quest chapters into the pack's quest book:

    ```sh
    python merge_ftbquests.py ../../ftbquests/config/ftbquests/quests <pack>/config/ftbquests/quests
    ```

    The script stops without writing anything if a chapter file, object id, or lang key already exists.

5. Optional: copy `optional/rngtech_energy_parity.js` into `kubejs/server_scripts/rngtech` (see
   [Energy](#energy)).

### RNGTech after 2.0.0-alpha.3

Newer RNGTech builds fix the ore and circuit problems themselves, so skip step 3:

- Ore hardness applies to other mods' ores only when `worldgen.ores.commonOreTagHardness` is on. It is off by
  default.
- RNGTech recipes take `rngtech:circuits/<tier>`, which holds only RNGTech circuits.
- Pickaxes that are not Modular tools reach up to hardness 6 by their tier, and the Netherite Pickaxe reaches every
  hardness. Diamond-tier pickaxes from other mods mine RNGTech Titanium, Tungsten, and Platinum ore. A config file
  written by an older RNGTech keeps its old value, so set `vanillaToolBridgePolicy = "TIER_LADDER"` under
  `[worldgen.ores]` when you upgrade.

For energy, set the stage multipliers in `config/rngtech-common.toml` instead of using the optional script.
`energy.stageGenerationMultiplier.stage5 = 1.5`, `stage6 = 2.0`, and `stage7 = 3.0` match the script's Syngas,
Fuel Cell, and Vacuum Collapse factors. They also scale every other generator of those stages, Solar Panels
included, which the script cannot change. `energy.generationMultiplier` scales every stage by one factor.

## What each file changes

### `config/rngtech-common.toml`

Turns off RNGTech's Tin, Zinc, Nickel, Lead, Silver, Aluminum, Osmium, Tungsten, and Platinum ores. FTB Materials,
GeOre, and Create Propulsion already place those, and AlmostUnified turns RNGTech's raw drops into FTB Materials
items anyway. Titanium and Naquadah stay on because nothing else in the pack places them in the Overworld. NeoForge
fills in every other key with its default.

### `kubejs/server_scripts/rngtech/ore_hardness.js`

RNGTech's `rngtech:ore_hardness/level_*` tags include the shared `#c:ores/*` tags. In Evolution that puts 49 ore
blocks from FTB Materials, Mekanism, Modern Industrialization, XyCraft, EnderIO, Occultism, and Create Propulsion
behind RNGTech ore hardness. Only RNGTech Modular tools reach past hardness 2, so 108 of the pack's 119 pickaxes,
including Netherite, Draconic, and every paxel and AIOT, break those ores without drops. The script keeps hardness
on RNGTech's own ores and the vanilla ores RNGTech lists.

### `kubejs/server_scripts/rngtech/circuits.js`

RNGTech recipes ask for `#c:circuits/basic`, `advanced`, `elite`, and `ultimate`. In Evolution those tags also hold
Mekanism's control circuits, which a Metallurgic Infuser makes from osmium, redstone, diamond, and refined
obsidian. That skips the Arclite, Titanium, Nullite, Naquadah, and Tungstensteel in RNGTech's Elite and Ultimate
Electric Circuits for 48 recipes. The script rewrites RNGTech's recipes to take RNGTech circuits. Other mods still
accept RNGTech circuits.

KubeJS has no schema for RNGTech's recipe types, so `replaceInput` misses them. The script rewrites the recipe
JSON and registers it again under the same id; use the same approach for other RNGTech recipe edits.

### `optional/rngtech_energy_parity.js`

Multiplies the FE of RNGTech's recipe-driven generators: Syngas `1.5x` (`360 FE/t`), Ammonia and Hydrogen Fuel Cell
`2x` (`1,200 FE/t` on Ammonia), and Vacuum Collapse `3x` (`3,000 FE/t`). This is not playtested. Without it,
Powah's Furnators out-produce every RNGTech generator for less material. The Solid Fuel Burner and Solar Panels
have no recipe energy, so a script cannot change them.

## Energy

Values are base stats from Evolution's configs and RNGTech's recipes. Mekanism's `2.5 J` per FE is converted.

| Generator | FE/t | FE per Coal | Build cost (approximate) |
|---|---:|---:|---|
| RNGTech Crude Solid Fuel Burner + Iron Heat Core | 24 | 16,000 | 12 iron, 2 copper, 28 redstone |
| Powah Starter Furnator | 20 | 48,000 | 6 iron, 2 redstone |
| Mekanism Heat Generator | 80 | about 128,000 | 3 iron, 2 copper, 1 osmium |
| RNGTech Steel Solid Fuel Burner + Steel Heat Core | 96 | 16,000 | 18 steel, 12 iron, 32 redstone, 2 blaze rods |
| Powah Hardened Furnator | 200 | 48,000 | 28 iron, 4.5 gold, 11 redstone |
| RNGTech Coal Gasifier + Syngas Combustor | 240 | about 40,000 | 15 aluminum, 15 iron, 2 steel, 53 redstone, plus a Stage 5 Heat Core |
| Powah Blazing Furnator | 800 | 48,000 | 35 iron, 9 blaze rods, 4.5 gold |
| RNGTech Ammonia Fuel Cell chain | 600 | not coal | Titanium machines plus Nitrogen and Hydrogen chains |
| RNGTech Vacuum Collapse Generator, Tungstensteel parts | about 1,050 | not coal | 51 Tungstensteel, 20 Ender Pearls, a Void Catalyst every 30 seconds |
| Powah Niotic Furnator | 2,000 | 48,000 | 43 iron, 13 diamonds, 9 blaze rods |

RNGTech machines use about as much energy as Mekanism's. Raw ore to ingots costs about `7,200 FE` per ingot in
the Stage 1 Crusher and Furnace and about `6,000 FE` in Mekanism's Enrichment Chamber and Energized Smelter. Stage
5-8 recipes cost more by design. Any FE source in the pack powers RNGTech machines through Mekanism, Flux, or other
cables.

## Ore availability

Ore blocks per Overworld chunk, counted in 338 freshly generated chunks before the config change.

| Material | Per chunk | From RNGTech | RNGTech hardness |
|---|---:|---:|---:|
| Lead | 167 | 15% | 3 |
| Nickel | 133 | 18% | 3 |
| Tin | 117 | 28% | 2 |
| Zinc | 93 | 32% | 2 |
| Silver | 71 | 28% | 3 |
| Osmium | 57 | 29% | 4 |
| Aluminum | 54 | 43% | 4 |
| Platinum | 34 | 16% | 6 |
| Tungsten | 22 | 26% | 6 |
| Titanium | 10 | 100% | 5 |
| Naquadah | 0.4 | 100% | 8 |

With the config, a new survey of 169 chunks found only RNGTech Titanium and Naquadah ore. RNGTech's raw Titanium
drops become FTB Materials raw Titanium, so RNGTech also adds an Overworld Titanium source for Modern
Industrialization, behind an RNGTech Stage 5 Modular pickaxe.

## Known gaps

- RNGTech's recipes ask for `#c:ingots/steel`, `bronze`, `invar`, `titanium`, and the other shared metals, so
  Mekanism, Immersive Engineering, and Modern Industrialization alloys skip RNGTech's Alloy Furnace steps. This kit
  leaves that as normal pack interop.
- RNGTech does not tag its plates, gears, or rods under `c:`, so other mods cannot use RNGTech Metal Press output.
  This keeps AlmostUnified from replacing RNGTech plates in RNGTech recipes.
- In 2.0.0-alpha.3, `rngtech:trait_shaped` recipes cannot be encoded with `Recipe.CODEC`, so recipe dump tools
  skip them. Newer builds encode them.
