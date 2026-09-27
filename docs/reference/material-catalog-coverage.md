# Material Catalog Coverage

Status: Implemented

Generated from `src/main/resources/data/rngtech/materials/catalog.json` and `src/main/resources/data/rngtech/materials/ore_catalog.json`.

## Staged Material Families

| Material | Stage | Default | Reason |
|---|---:|---|---|
| `iron` | 1 | Enabled | Vanilla-backed early structural material. |
| `copper` | 2 | Enabled | Vanilla-backed early conductive material. |
| `bronze` | 3 | Enabled | Common early alloy compatibility material. |
| `tin` | 3 | Enabled | Common early alloy ingredient. |
| `zinc` | 3 | Enabled | Common early alloy ingredient. |
| `gold` | 3 | Enabled | Vanilla-backed conductive and refinement material. |
| `steel` | 4 | Enabled | Common midgame structural compatibility material. |
| `invar` | 4 | Enabled | Common midgame thermal compatibility material. |
| `nickel` | 4 | Enabled | Common midgame metal compatibility material. |
| `lead` | 4 | Enabled | Common midgame shielding compatibility material. |
| `silver` | 4 | Enabled | Common midgame conductive compatibility material. |
| `aluminum` | 5 | Enabled | Common efficient-control compatibility material. |
| `sparksteel` | 5 | Enabled | RNGTech-owned energy-transfer alloy. |
| `osmium` | 5 | Enabled | Common advanced machine compatibility material. |
| `titanium` | 6 | Enabled | Advanced high-throughput compatibility material. |
| `arclite` | 6 | Enabled | RNGTech-owned advanced control and transfer alloy. |
| `tungsten` | 7 | Enabled | Late-game pressure compatibility material. |
| `tungstensteel` | 7 | Enabled | Late-game machine-body compatibility material. |
| `nullite` | 7 | Enabled | RNGTech-owned late-game energy and phase alloy. |
| `aethergold` | 7 | Enabled | RNGTech-owned late-game visibility and transfer alloy. |
| `platinum` | 7 | Enabled | Late-game catalyst compatibility material. |
| `netherite` | 8 | Enabled | Vanilla-backed exotic extension material. |
| `naquadah` | 8 | Enabled | RNGTech-fictional exotic extension material. |

## Material Family Groups

| Group | Families | Reason |
|---|---|---|
| `rngtech:materials/family/alloys` | `bronze`, `steel`, `invar`, `sparksteel`, `arclite`, `tungstensteel`, `nullite`, `aethergold` | Shared recipe and datapack hook for alloy-backed material families. |

## Registered Item Counts

- Registered metal form items: 223
- Vanilla-mapped metal forms: 17
- Generic form items: 51
- Registered non-stage items: 5
- Registered RNGTech ore blocks: 22
- Total generated material item models: 279

## Ore Block Catalog

| Material | Hardness | Default worldgen | Biomes | Variants |
|---|---:|---|---|---|
| `tin` | 2 | Enabled | `#minecraft:is_overworld` | `rngtech:tin_ore`, `rngtech:deepslate_tin_ore` |
| `zinc` | 2 | Enabled | `#minecraft:is_overworld` | `rngtech:zinc_ore`, `rngtech:deepslate_zinc_ore` |
| `nickel` | 3 | Enabled | `#minecraft:is_overworld` | `rngtech:nickel_ore`, `rngtech:deepslate_nickel_ore` |
| `lead` | 3 | Enabled | `#minecraft:is_overworld` | `rngtech:lead_ore`, `rngtech:deepslate_lead_ore` |
| `silver` | 3 | Enabled | `#minecraft:is_overworld` | `rngtech:silver_ore`, `rngtech:deepslate_silver_ore` |
| `aluminum` | 4 | Enabled | `#minecraft:is_overworld` | `rngtech:aluminum_ore`, `rngtech:deepslate_aluminum_ore` |
| `osmium` | 4 | Enabled | `#minecraft:is_overworld` | `rngtech:osmium_ore`, `rngtech:deepslate_osmium_ore` |
| `titanium` | 5 | Enabled | `#minecraft:is_overworld` | `rngtech:titanium_ore`, `rngtech:deepslate_titanium_ore` |
| `tungsten` | 6 | Enabled | `#minecraft:is_overworld` | `rngtech:tungsten_ore`, `rngtech:deepslate_tungsten_ore` |
| `platinum` | 6 | Enabled | `#minecraft:is_overworld` | `rngtech:platinum_ore`, `rngtech:deepslate_platinum_ore` |
| `naquadah` | 8 | Enabled | `#minecraft:is_overworld` | `rngtech:naquadah_ore`, `rngtech:deepslate_naquadah_ore` |

Default ore generation can be disabled globally with `worldgen.ores.enabled`, per family with `worldgen.ores.<material>.enabled`, or by overriding the stable `data/rngtech/neoforge/biome_modifier/ore_<material>.json` file.

## Vanilla-Mapped Metal Forms

| Material | Form | Uses |
|---|---|---|
| `iron` | `ingot` | `minecraft:iron_ingot` |
| `iron` | `nugget` | `minecraft:iron_nugget` |
| `iron` | `storage_block` | `minecraft:iron_block` |
| `iron` | `ore` | `minecraft:iron_ore`, `minecraft:deepslate_iron_ore` |
| `iron` | `raw` | `minecraft:raw_iron` |
| `copper` | `ingot` | `minecraft:copper_ingot` |
| `copper` | `storage_block` | `minecraft:copper_block` |
| `copper` | `ore` | `minecraft:copper_ore`, `minecraft:deepslate_copper_ore` |
| `copper` | `raw` | `minecraft:raw_copper` |
| `gold` | `ingot` | `minecraft:gold_ingot` |
| `gold` | `nugget` | `minecraft:gold_nugget` |
| `gold` | `storage_block` | `minecraft:gold_block` |
| `gold` | `ore` | `minecraft:gold_ore`, `minecraft:deepslate_gold_ore`, `minecraft:nether_gold_ore` |
| `gold` | `raw` | `minecraft:raw_gold` |
| `netherite` | `ingot` | `minecraft:netherite_ingot` |
| `netherite` | `storage_block` | `minecraft:netherite_block` |
| `netherite` | `ore` | `minecraft:ancient_debris` |

## Non-Stage Material Handling

| Material | Handling | Maps to | Reason |
|---|---|---|---|
| `stone` | map | `minecraft:stone` | Vanilla block item covers this ingredient. |
| `clay` | map | `minecraft:clay_ball` | Vanilla item covers this ingredient. |
| `glass` | map | `minecraft:glass` | Vanilla block item covers this ingredient. |
| `coal` | map | `minecraft:coal` | Vanilla item covers this ingredient. |
| `charcoal` | map | `minecraft:charcoal` | Vanilla item covers this ingredient. |
| `coal_dust` | register | `` | RNGTech-owned crushed carbon input for gas-bucket bootstrap recipes. |
| `wood` | map | `minecraft:oak_log` | Vanilla logs and tags cover this ingredient family. |
| `plant_reagent` | register | `` | RNGTech-owned planned reagent with no vanilla equivalent. |
| `redstone` | map | `minecraft:redstone` | Vanilla item covers this ingredient. |
| `lapis_lazuli` | map | `minecraft:lapis_lazuli` | Vanilla item covers this ingredient. |
| `amethyst` | map | `minecraft:amethyst_shard` | Vanilla item covers this ingredient. |
| `quartz` | map | `minecraft:quartz` | Vanilla item covers this ingredient. |
| `diamond` | map | `minecraft:diamond` | Vanilla item covers this ingredient. |
| `emerald` | map | `minecraft:emerald` | Vanilla item covers this ingredient. |
| `ender_pearl` | map | `minecraft:ender_pearl` | Vanilla item covers this ingredient. |
| `eye_of_ender` | map | `minecraft:ender_eye` | Vanilla item covers this ingredient. |
| `glowstone` | map | `minecraft:glowstone_dust` | Vanilla item covers this ingredient. |
| `obsidian` | map | `minecraft:obsidian` | Vanilla block item covers this ingredient. |
| `blaze_rod` | map | `minecraft:blaze_rod` | Vanilla item covers this ingredient. |
| `blaze_powder` | map | `minecraft:blaze_powder` | Vanilla item covers this ingredient. |
| `slime` | map | `minecraft:slime_ball` | Vanilla item covers this ingredient. |
| `honey` | map | `minecraft:honey_bottle` | Vanilla item covers this ingredient. |
| `resin` | register | `` | RNGTech-owned planned reagent with no vanilla equivalent. |
| `rubber` | register | `` | RNGTech-owned planned insulator with no vanilla equivalent. |
| `organic_reagent` | register | `` | RNGTech-owned planned organic input with no vanilla equivalent. |
| `lava_bucket` | map | `minecraft:lava_bucket` | Vanilla item covers this ingredient. |
| `leather` | map | `minecraft:leather` | Vanilla item covers this ingredient. |

Mapped non-stage materials use vanilla or external ingredients in recipes instead of registering RNGTech item ids.

Registered non-stage materials: `coal_dust`, `plant_reagent`, `resin`, `rubber`, `organic_reagent`.
Mapped non-stage materials: 22.
