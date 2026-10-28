# RNGTech in Techopolis 3

This folder adds RNGTech to [Techopolis 3](https://www.curseforge.com/minecraft/modpacks/techopolis-3). It is
optional pack-maker content and is not part of RNGTech's runtime.

Tested with Techopolis 3 13.7 (Minecraft 1.21.1, NeoForge 21.1.265) and RNGTech built from main after
2.0.0-alpha.3 on a dedicated server, Normal world type. The pack loads with no KubeJS errors, and the checks below
were run in game. It is not playtested.

## Install

Stop the client or server first. FTB Quests saves its quest book on shutdown and overwrites a merge made while it
runs.

1. Put `rngtech-<version>.jar` in the pack's `mods` folder.
2. Copy `config/rngtech-common.toml` into the pack's `config` folder. If the pack already has one, set the same keys.
3. Copy `kubejs/server_scripts/rngtech` into the pack's `kubejs/server_scripts`, and `kubejs/data/rngtech` into
   the pack's `kubejs/data`.
4. Merge the RNGTech quest chapters into the pack's quest book with the Evolution kit's script, which works for
   any FTB Quests pack:

    ```sh
    python ../ftb-evolution/merge_ftbquests.py ../../ftbquests/config/ftbquests/quests <pack>/config/ftbquests/quests
    ```

    The RNGTech chapters arrive as their own chapter group after Techopolis' Ender tier.

5. Optional: add the keys in `optional/rngtech-common-energy.toml` to `config/rngtech-common.toml` (see
   [Energy](#energy)).

## How Techopolis differs from other packs

Techopolis builds its own resource economy, and RNGTech's defaults step around it.

- **Ore blocks drop themselves.** Techopolis removes every recipe that smelts or crushes an ore block. Ingots come
  from Strainer ore pieces, compacted into raw metal. Mined ores go into the Dimensional Infuser, 32 ores for one
  Dimensional Ore, which a Mekanism chain turns into 10 dust.
- **The Nether opens late.** Obsidian portals do not work. The portal frame needs Ultimate Technium, so Blaze
  Rods, Blaze Powder, and Netherite come in the Hellish tier, the fifth of seven.
- **Materials set the tier.** Iron, Tin, Aluminum, and Redstone arrive in the Advanced tier. Gold, Diamond,
  and Nether Quartz come from Elite tier machines, and Ender Pearls and Emeralds from the Ultimate tier.
- **Recipes, not quests, gate mods.** Techopolis puts Technium machine frames and research blueprints into other
  mods' recipes. Its quests guide players but do not block any crafting.

## What each file changes

### `config/rngtech-common.toml`

Turns off RNGTech's Tin, Zinc, Nickel, Lead, Silver, Aluminum, Osmium, and Platinum ores. AllTheOres, Mekanism,
Immersive Engineering, and Mekanism More Machine already place those, and AlmostUnified turns RNGTech's ingots
and dusts into AllTheOres items anyway. Titanium, Tungsten, and Naquadah stay on, because nothing else in the
pack places them.

### `kubejs/server_scripts/rngtech/ore_blocks.js`

Removes the 40 RNGTech Crusher and Furnace recipes that take an ore block. Without it, a Stage 1 Crusher turns
one ore block into 3 ingots, about 10 times what Techopolis gives for an ore. The Stage 0 Furnace also smelts
Diamond, Emerald, Redstone, Lapis, and Quartz ore 1:1. Techopolis gives those only through its own machines,
Diamond and Quartz not before Elite. RNGTech keeps its
Titanium, Tungsten, and Naquadah ore recipes, and it still crushes raw metal: 2 Crushed per raw metal, against
1.67 dust from the Immersive Engineering Crusher.

### `kubejs/server_scripts/rngtech/rngtech_only_tags.js`

Immersive Engineering ships Tungsten and Titanium recipes that stay empty until a mod fills `#c:ores/tungsten`
and `#c:ores/titanium`. RNGTech is the only mod in the pack that does. With it, the Engineer's Hammer crushes
Tungsten Ore in the crafting grid, the Arc Furnace skips RNGTech's Stage 7 smelting, and the Excavator digs both
ores with no ore hardness. The script removes those five recipes and the two Excavator entries, so the pack
behaves as it did before RNGTech.

### `kubejs/server_scripts/rngtech/nether_materials.js`

RNGTech's Steel, Aluminum, Sparksteel, and Titanium Heat Cores need 2 Blaze Rods each, and the Stage 6 Ammonia
and Reforming Catalyst Beds need Blaze Powder. Without a change, RNGTech's fuel power stops at the Bronze Heat
Core and its gas power waits for the Nether. The script lets those Heat Cores take RNGTech's Ignition Catalyst,
which the Bronze Heat Core already accepts, and lets the Catalyst Beds take Iron Dust and Nickel Dust. The Exotic
Heat Core keeps its Blaze Rods for Stage 8. The Ignition Catalyst needs a Torchflower, so this works only in the
Normal and Hexlands world types, where Sniffers spawn.

### `kubejs/data/rngtech/data_maps/item/ore_reach.json`

Only the vanilla Netherite Pickaxe reaches ore hardness 8. The pack's Netherite AIOT, Paxel, and Hammer reach 6
and cannot mine Naquadah. This `rngtech:ore_reach` data map sets those three to 8.

### Editing RNGTech recipes

KubeJS has no schema for RNGTech's recipe types. Its `input` filter finds no RNGTech recipe, and `replaceInput`
misses most of them, so the scripts rewrite the recipe JSON and register it again under the same id. The `id`,
`type`, and `output` filters work.

## Where RNGTech sits

| Techopolis tier | Materials it adds | RNGTech stage | RNGTech power |
|---|---|---:|---|
| Basic | Cobblestone, Coal, Copper | 0 | None. The Basic quests have no FE generator either. |
| Advanced | Iron, Tin, Aluminum, Redstone, Lapis | 1-4 | Solid Fuel Burner, 24-96 FE/t |
| Elite | Gold, Diamond, Nether Quartz | 5-6 | Syngas Combustor 240 FE/t, Fuel Cell 600 FE/t |
| Ultimate | Ender Pearls, Emeralds, Uranium reactors | 7 | Vacuum Collapse Generator, about 1,050 FE/t |
| Hellish | Blaze, Netherite | 8 | Exotic Vacuum Collapse, about 3,000 FE/t |

RNGTech stages follow the materials they use, so the pack's own economy holds RNGTech near these tiers. The
quest book cannot: like Techopolis' own chapters, the RNGTech chapters block no crafting. Basic Electric Circuits
need Nether Quartz, so Stage 3 circuits and Stage 1 Solar Panels wait for Elite even though Bronze comes earlier.

## Energy

Values are base stats from the pack's scripts and configs and from RNGTech's recipes. Build costs are approximate,
from a bill of materials over the 25,894 recipes loaded at runtime.

| Generator | Tier | FE/t | FE per fuel | Build cost (approximate) |
|---|---|---:|---:|---|
| RNGTech Crude Solid Fuel Burner + Iron Heat Core | Advanced | 24 | 16,000 per Coal | 12 iron, 2 copper, 28 redstone |
| Immersive Engineering Kinetic Dynamo + Water Wheel | Advanced | not measured | free | 5 copper, 3 iron, 1 steel, 20 treated wood |
| Actually Additions Oil Generator | Advanced | 40-120 | 4,000-48,000 per 50 mB oil | 9 lead, 3 nickel, 6 Basic Technium Nuggets |
| RNGTech Steel Solid Fuel Burner + Steel Heat Core | Advanced | 96 | 16,000 per Coal | 18 steel, 12 iron, 32 redstone, an Ignition Catalyst |
| Actually Additions Coal Generator | Elite | 20 | 32,000 per Coal | 12 Elite Technium Nuggets |
| RNGTech Coal Gasifier + Syngas Combustor | Elite | 240 | about 40,000 per Coal | 15 aluminum, 15 iron, 54 redstone, quartz, a Stage 5 Heat Core |
| Techopolis Bio Generator | Elite | 640-1,280 | 512,000 per Bio Fuel | 6 Bio Fuel, 2 Infused Alloy, an Advanced Technium frame |
| RNGTech Fuel Cell chain | Elite | 600 | not coal | Titanium machines plus Nitrogen and Hydrogen chains |
| RNGTech Vacuum Collapse Generator | Ultimate | about 1,050 | not coal | 37 Tungstensteel, 16 Ender Pearls, a Void Catalyst every 30 seconds |
| Techopolis Nuclear Reactor | Ultimate | 2,500-100,000 | 3 million per Uranium Ingot | 6 Elite Technium Nuggets, 2 Unstable Matter, an Extreme Reactors multiblock |
| Immersive Engineering Diesel Generator | Ultimate | 4,096 | not coal | Ultimate research |

The Basic quests use no FE, and the Advanced generators are about as strong as RNGTech's Stage 1-4
burners. From Elite up, the Bio Generator outruns the Syngas Combustor and Fuel Cell on output, and it gets more than ten
times the FE per fuel. At Ultimate, Extreme Reactors and the Nuclear Reactor pass every RNGTech generator. Players
will run RNGTech machines on Techopolis power from Elite on. That works, because RNGTech uses plain FE.

`optional/rngtech-common-energy.toml` doubles the FE of Stage 5-8 generators and leaves Stages 1-4 alone, where
the burners already match the pack. That gives a Syngas Combustor about 480 FE/t and a Fuel Cell about
1,200 FE/t, near the Bio Generator, and a Vacuum Collapse Generator about 2,100 FE/t, near the Nuclear Reactor on
Uranium. This is not playtested.

RNGTech machines use about as much energy as the pack's. Raw metal to ingots costs about `7,200 FE` per ingot in
the Stage 1 Crusher and Furnace, and about `7,600 FE` in the Immersive Engineering Crusher and Mekanism Energized
Smelter. The Dimensional Infuser alone costs `64,000 FE` per Dimensional Ore, or about `6,400 FE` per ingot.

## Ore availability

Ore blocks per Overworld chunk, counted in 338 freshly generated chunks before the config change. Every ore in the
pack drops itself.

| Material | Per chunk | From RNGTech | RNGTech hardness |
|---|---:|---:|---:|
| Silver | 213 | 9% | 3 |
| Lead | 202 | 14% | 3 |
| Osmium | 177 | 9% | 4 |
| Tin | 176 | 20% | 2 |
| Nickel | 165 | 17% | 3 |
| Zinc | 117 | 28% | 2 |
| Aluminum | 63 | 40% | 4 |
| Platinum | 45 | 11% | 6 |
| Titanium | 9.1 | 100% | 5 |
| Tungsten | 4.9 | 100% | 6 |
| Naquadah | 0.4 | 100% | 8 |

With the config, a new survey of 338 chunks found only RNGTech Titanium, Tungsten, and Naquadah ore. RNGTech's
ore hardness stays off other mods' ores. Iron, Bronze, and Osmium pickaxes reach hardness 3. Diamond, Steel,
Refined Obsidian, and the Meka-Tool reach 6. Only the Netherite Pickaxe reaches Naquadah's 8, and with the kit also the
Netherite AIOT, Paxel, and Hammer. Netherite comes in the
Hellish tier, the same tier as RNGTech's Stage 8.

## Known gaps

- Only the Normal and Hexlands world types are covered. In Skyblock and Superflat, RNGTech's Titanium, Tungsten, and
  Naquadah have no source, and the Torchflower for the Ignition Catalyst is hard to reach.
- Replication gives Titanium and Tungsten Ingots matter values, so a late Replicator can copy them. This kit
  leaves that as Techopolis' late-game design.
- RNGTech recipes take alloys, plates, gears, and rods through RNGTech-only tags, so AllTheOres, Immersive
  Engineering, Mekanism, and Stellaris steel and plates do not skip RNGTech's Alloy Furnace and Metal Press.
  RNGTech's AlmostUnified plugin keeps those items as RNGTech items in RNGTech recipe outputs.
- AlmostUnified hides RNGTech's alloys, plates, gears, and rods in JEI, because AllTheOres items are its targets.
  To show them, add these patterns to `ignored_items` in `config/almostunified/unification/materials.json`:

    ```json
    "rngtech:(bronze|steel|invar|sparksteel|arclite|tungstensteel|nullite|aethergold)_(ingot|nugget)",
    "rngtech:[a-z]+_(plate|gear|rod)"
    ```
