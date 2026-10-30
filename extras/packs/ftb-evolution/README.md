# RNGTech in FTB Evolution

This folder adds RNGTech to [FTB Evolution](https://www.feed-the-beast.com/modpacks/125-ftb-evolution). It is
optional pack-maker content and is not part of RNGTech's runtime.

The kit was tested with FTB Evolution 1.45.1 (Minecraft 1.21.1, NeoForge 21.1.250) and RNGTech 2.0.0-alpha.3 on a
dedicated server. The pack loaded with no KubeJS errors, and the checks below were run in game. RNGTech builds after
alpha.3 fix the ore hardness, circuit, and alloy conflicts that test found, so the kit no longer needs KubeJS
scripts. This version of the kit has not been tested in the pack again.

## Install

Stop the client or server first. FTB Quests saves its quest book on shutdown and overwrites a merge made while it
runs.

1. Put `rngtech-<version>.jar` in the pack's `mods` folder.
2. Copy `config/rngtech-common.toml` into the pack's `config` folder. If the pack already has one, set the same keys.
3. Merge the RNGTech quest chapters into the pack's quest book:

    ```sh
    python merge_ftbquests.py ../../ftbquests/config/ftbquests/quests <pack>/config/ftbquests/quests
    ```

    The script stops without writing anything if a chapter file, object id, or lang key already exists. It works
    for any FTB Quests pack.

4. Optional: add the keys in `optional/rngtech-common-energy.toml` to `config/rngtech-common.toml` (see
   [Energy](#energy)).
5. Optional: show RNGTech's alloys, plates, gears, and rods in JEI (see [AlmostUnified](#almostunified)).

If the pack already ran RNGTech 2.0.0-alpha.3, remove the kit's old `kubejs/server_scripts/rngtech` folder. A
config file written by alpha.3 also keeps its old tool policy, so set `vanillaToolBridgePolicy = "TIER_LADDER"`
under `[worldgen.ores]`.

## What each file changes

### `config/rngtech-common.toml`

Turns off RNGTech's Tin, Zinc, Nickel, Lead, Silver, Aluminum, Osmium, Tungsten, and Platinum ores. FTB Materials,
GeOre, and Create Propulsion already place those, and AlmostUnified turns RNGTech's raw drops into FTB Materials
items anyway. Titanium and Naquadah stay on because nothing else in the pack places them in the Overworld. NeoForge
fills in every other key with its default.

### `optional/rngtech-common-energy.toml`

Multiplies the FE of RNGTech's Stage 5-8 generators: Stage 5 `1.5x`, Stage 6 `2x`, and Stages 7 and 8 `3x`. That
gives a Syngas Combustor about `360 FE/t`, a Fuel Cell on Ammonia about `1,200 FE/t`, and a Vacuum Collapse
Generator with Tungstensteel parts about `3,000 FE/t`. Solar Panels and the other generators of those stages scale
too. This is not playtested.

### `merge_ftbquests.py`

Copies the RNGTech chapters and reward tables into an existing quest book, appends the RNGTech chapter group, and
merges the `en_us` lang entries. It does not change the pack's own chapters.

## What RNGTech handles itself

Evolution shares many `c:` tags with RNGTech. RNGTech's defaults keep them from breaking its progression:

- Ore hardness applies to RNGTech's own ores and the vanilla ores it lists. Other mods' ores get it only when
  `worldgen.ores.commonOreTagHardness` is on, which is off by default. With it on in Evolution, 49 ore blocks from
  FTB Materials, Mekanism, Modern Industrialization, XyCraft, EnderIO, Occultism, and Create Propulsion would drop
  nothing for 108 of the pack's 119 pickaxes.
- Pickaxes that are not Modular tools reach up to hardness 6 by their tier, and the Netherite Pickaxe reaches every
  hardness. Diamond-tier pickaxes from other mods mine RNGTech Titanium, Tungsten, and Platinum ore.
- RNGTech recipes take `rngtech:circuits/<tier>`, which holds only RNGTech circuits. Mekanism's control circuits
  are in `c:circuits/*` in Evolution and would skip the Arclite, Titanium, Nullite, Naquadah, and Tungstensteel in
  48 RNGTech recipes. RNGTech circuits stay in the `c:` tags, so other mods still accept them.
- RNGTech recipes take alloy ingots and nuggets through `rngtech:ingots/<alloy>` and `rngtech:nuggets/<alloy>`, and
  plates, gears, and rods through `rngtech:plates/<metal>`, `rngtech:gears/<metal>`, and `rngtech:rods/<metal>`.
  These tags hold only RNGTech items, so FTB Materials, Mekanism, Immersive Engineering, and Modern
  Industrialization steel, bronze, and plates do not skip the Alloy Furnace or the Metal Press. The items stay in
  the `c:` tags, and 196 other-mod recipe inputs in Evolution take an RNGTech Steel Plate.
- RNGTech's AlmostUnified plugin keeps those items in RNGTech recipe outputs. AlmostUnified still turns RNGTech's
  mined-metal outputs, such as Tin and Titanium ingots, into FTB Materials items.

### AlmostUnified

AlmostUnified hides RNGTech's alloys, plates, gears, and rods in JEI, because FTB Materials items are its targets.
To show them, add these patterns to `ignored_items` in `config/almostunified/unification/materials.json`:

```json
"rngtech:(bronze|steel|invar|sparksteel|arclite|tungstensteel|nullite|aethergold)_(ingot|nugget)",
"rngtech:[a-z]+_(plate|gear|rod)"
```

## Energy

Values are base stats from Evolution's configs and RNGTech 2.0.0-alpha.3's recipes. Mekanism's `2.5 J` per FE is
converted.

| Generator | FE/t | FE per Coal | Build cost (approximate) |
|---|---:|---:|---|
| RNGTech Crude Solid Fuel Burner + Iron Heat Core | 24 | 16,000 | 12 iron, 2 copper, 28 redstone |
| Powah Starter Furnator | 20 | 48,000 | 6 iron, 2 redstone |
| Mekanism Heat Generator | 80 | about 128,000 | 3 iron, 2 copper, 1 osmium |
| RNGTech Steel Solid Fuel Burner + Steel Heat Core | 96 | 16,000 | 18 steel, 12 iron, 32 redstone, 2 blaze rods |
| Powah Hardened Furnator | 200 | 48,000 | 28 iron, 4.5 gold, 11 redstone |
| RNGTech Coal Gasifier + Syngas Combustor | 240 | about 40,000 | 15 aluminum, 15 iron, 2 steel, 53 redstone, plus a Stage 5 Heat Core |
| Powah Blazing Furnator | 800 | 48,000 | 35 iron, 9 blaze rods, 4.5 gold |
| RNGTech Fuel Cell chain | 600 | not coal | Titanium machines plus Nitrogen and Hydrogen chains |
| RNGTech Vacuum Collapse Generator, Tungstensteel parts | about 1,050 | not coal | 51 Tungstensteel, 20 Ender Pearls, a Void Catalyst every 30 seconds |
| Powah Niotic Furnator | 2,000 | 48,000 | 43 iron, 13 diamonds, 9 blaze rods |

Without the optional config, Powah's Furnators out-produce every RNGTech generator for less material. RNGTech
machines use about as much energy as Mekanism's. Raw ore to ingots costs about `7,200 FE` per ingot in the Stage 1
Crusher and Furnace and about `6,000 FE` in Mekanism's Enrichment Chamber and Energized Smelter. Stage 5-8 recipes
cost more by design. Any FE source in the pack powers RNGTech machines through Mekanism, Flux, or other cables.

`energy.generationMultiplier` scales every stage by one factor, if you prefer that to the per-stage keys.

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

- Mined metals stay on `c:` tags. Modern Industrialization Titanium, Tungsten, and Platinum ingots work in RNGTech
  recipes, so its processing skips RNGTech's Furnace heat for those metals. RNGTech's own gate for them is ore
  hardness, which reaches other mods' ores only when `commonOreTagHardness` is on.
- KubeJS has no schema for RNGTech's recipe types, so `replaceInput` misses them. To edit an RNGTech recipe, rewrite
  its JSON and register it again under the same id. The `id`, `type`, and `output` filters work.
