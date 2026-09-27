# Materials List

Status: Prototype

This page defines the material families and recipe forms used by advanced RNGTech machines, machine parts, battery systems, and refinement items.

The material catalog, ore generation, alloying, and survival recipe chains are implemented prototypes. Broader component-upgrade and breakthrough systems remain planned. See [Current Implementation](current-implementation.md) for the feature inventory.

[Component Stages](component-stages.md) defines how material stages work. This page names the material families and crafting inputs used by those stages.

## Code-Backed Catalog

Status: Prototype

The current implementation registers texture-backed RNGTech item ids for staged material family forms that RNGTech owns, plus generic recipe forms, RNGTech-owned non-stage materials, and RNGTech-owned ore block items. Vanilla-owned forms listed in the catalog's `vanilla_form_mappings` use Minecraft item ids in tags and recipes instead of duplicate RNGTech items. The material catalog source is `src/main/resources/data/rngtech/materials/catalog.json`; ore block and worldgen profiles are sourced from `src/main/resources/data/rngtech/materials/ore_catalog.json`.

Code-backed catalog behavior includes:

- Item registration, models, language entries, creative-tab placement, and item tags.
- Stone and deepslate ore block registration, blockstates, block models, block loot tables, and block tags.
- Vanilla form mappings for Iron, Copper, Gold, and Netherite ingots, ores, nuggets, and storage blocks where Minecraft already provides the item.
- Server config enablement under `materials.<material>`, with disabled RNGTech-owned family forms still registered for save compatibility.
- Ore worldgen config under `worldgen.ores.enabled` and `worldgen.ores.<material>.enabled`, with stable NeoForge biome modifier ids such as `rngtech:ore_tin`.
- Optional JEI ore-generation entries for configured source ore profiles, showing ore blocks, raw drop, Y range, attempts per chunk, vein size, air-exposure discard, ore hardness, default generation state, current config state, and the stable biome modifier id.
- Aggregate material-family group tags, currently including `rngtech:materials/family/alloys` for alloy-backed material families.
- Shapeless manual Forming Hammer plate recipes for iron, copper, bronze, and gold at four ingots per two plates, with one full hammer lasting 60 plates.
- Nugget-diagonal rod recipes for Iron, Copper, Gold, Steel, Aluminum, Sparksteel, Titanium, Nullite, Netherite, and Naquadah; Tungstensteel Rod from a Steel Rod core and Tungstensteel Plates; vanilla-backed recipes for iron, copper, and gold gears, coils, and casings; a manual Bronze Gear recipe; alloy-family coils from matching ingots, matching plates, and redstone; Titanium coils from matching ingots and redstone; plus netherite coil and the current netherite plate recipe.
- Naquadah plate, rod, coil, and casing recipes for the default Exotic-stage crafting pressure.
- Nugget conversion recipes for the catalog material outputs needed by nugget-based manual casing and bus bar recipes.
- Metal Press plate recipes for all registered material plate forms, using one matching ingot, a Plate Mold, and the material's heat gate.
- Metal Press casing recipes for the downstream-used casing forms, using four matching ingots, a Casing Mold, and the material's heat gate.
- Metal Press gear recipes for every registered material gear form, using three matching ingots, a Gear Mold, and the material's heat gate.
- Manual crushed-to-dust recipes for iron, copper, tin, and gold.
- `rngtech:crusher` crushed-to-dust recipes for non-alloy crushed materials. This second Crusher pass is faster and cheaper overall than direct crushed smelting.
- Direct `rngtech:furnace` crushed-to-ingot recipes for non-alloy crushed materials. These are hopper-friendly fallback recipes at `600` ticks and `7,200 FE`, making them much slower and more expensive than the Crusher-to-dust route.
- Crusher ingot-to-dust recipes for Iron, Copper, and Tin, supporting ordinary non-alloy dust use and pack compatibility without making alloy dusts part of default progression.
- Crusher coal-to-dust conversion for `rngtech:coal_dust`, used by the Carbon Exhaust Bucket bootstrap recipe.
- Early Bronze ingot bootstrap from `3x c:ingots/copper`, `1x c:ingots/tin`, and `1x minecraft:charcoal`, producing `2` Bronze Ingots before Alloy Furnace infrastructure exists.
- `rngtech:furnace` dust-to-ingot recipes for non-alloy staged material families, plus tag-backed raw-to-ingot and ore-to-ingot recipes for natural source ore families. Alloy families such as Bronze do not have default dust, crushed, raw, or ore smelting paths.
- Generic Casing, tiered Machine Frame, and Bus Bar crafting for machine-body and battery-chassis recipes.
- Refractory Casing crafting from Generic Casing, brick, and obsidian for higher Alloy Furnace chassis and Reactor Chamber component chains. The Bronze Alloy Furnace setup uses Bronze Casing.
- `rngtech:machine_frame` remains the base frame for Stage 0-3 machines, early support recipes, and the Steel Metal Press body. `rngtech:reinforced_machine_frame` is the Steel-stage structural gate for most other Stage 4 Steel and Lead bodies, `rngtech:advanced_machine_frame` gates Stage 5-6 bodies, and `rngtech:exotic_machine_frame` gates Stage 7-8 bodies through `rngtech:lubricated_frame_coupling`, which is assembled from Tungstensteel Casing and Lubricant in the Component Assembler.
- Energy Coil crafting from Copper Coil, Copper Energy Connector, and redstone.
- Press-based Electric circuit tier recipes, Circuit Blank forms, and `c:circuits` tags. The circuit chain uses mineral insulation and metal/alloy conductive parts rather than organic wire, resin, rubber, or lubricant inputs.

The generated coverage report is [Material Catalog Coverage](material-catalog-coverage.md).

RNGTech-owned ore ids such as `rngtech:tin_ore` are block items, with deepslate variants such as `rngtech:deepslate_tin_ore`. Default RNGTech worldgen places Tin, Zinc, Nickel, Lead, Silver, Aluminum, Osmium, Titanium, Tungsten, Platinum, and Naquadah in new Overworld chunks. Normal mining drops the matching `raw_<material>` item with Minecraft-style Fortune scaling, while Silk Touch drops the ore block. Alloy families exclude ore and raw forms, so Bronze, Steel, Invar, Sparksteel, Arclite, Tungstensteel, Nullite, and Aethergold are reached through ingredient chains and Alloy Furnace routes rather than direct ore smelting. Vanilla-backed ores, raw materials, and storage blocks use Minecraft's existing ids.

Large-deposit packs can disable RNGTech placement by config or by overriding `data/rngtech/neoforge/biome_modifier/ore_<material>.json` to `neoforge:none`. The `c:ores/<material>` block and item tags remain the compatibility surface for external ore generators.

## Current Recipe Baseline

Status: Prototype

The current code-backed crafting recipes use a small vanilla material set. Furnace dust-to-ingot recipes exist for non-alloy staged material families and are gated by material enablement, component stage, and heat requirements. Non-alloy crushed materials can smelt directly to ingots at `600` ticks and `7,200 FE` so early Crusher-to-Furnace hopper automation works. Running crushed material through the Crusher again produces dust quickly and cheaply, making the Crusher-to-Crusher-to-Furnace route the better energy and throughput path at `260` ticks and `6,000 FE` before machine modifiers. Ore-to-ingot and crushed-to-ingot recipes are not the alloy path.

| Material or Ingredient | Current Use | Notes |
|---|---|---|
| Wood planks and sticks | Wooden Crusher Chassis, Flint Crush Head | Stage 0 primitive structure and handles. |
| Flint | Flint Crush Head, Wooden Crusher Chassis | Stage 0 crusher tooling and primitive body bracing. |
| Iron ingot or nugget | Machine Frame, Crusher, Affix Forge, Iron Crush Head, Crude Energy Connector, Iron Battery Cell, Iron Battery Chassis, Forming Hammer, generic Casing, manual Iron Plate | Main early structural material. The base Machine Frame carries Stage 0-3 bodies and the Steel Metal Press body, while most other Steel-stage bodies move to the reinforced frame sink. |
| Copper ingot, nugget, or block | Crusher, Furnace, Copper Energy Connector, Bus Bar, Energy Coil, Affix Forge, Affix Injector, Nullifier Coil, Copper Battery Cell, Copper Battery Chassis, manual Copper Plate | Main early energy and conductivity material. |
| Bronze ingot tag | Bronze machine parts, manual Bronze Plate, manual Bronze Gear, Crude Press bootstrap, and Basic Circuit blanks | The early bootstrap crafts `3x c:ingots/copper`, `1x c:ingots/tin`, and `1x minecraft:charcoal` into `2` Bronze Ingots. The Alloy Furnace is the full-yield route, producing `4` Bronze Ingots from the same `3:1` Copper/Tin ratio plus Charcoal flux once Bronze Alloy Furnace gear is built. `c:ingots/bronze` remains the compatibility input where recipes need an ingot tag. |
| Redstone | Most machine and energy recipes, including Bus Bar and Energy Coil | Current control and energy-routing ingredient. |
| Smooth stone and vanilla Furnace | Machine Frame, Furnace, Crusher, Crusher Machine Chassis, generic Casing, solid fuel-burning generator chassis | Current machine body and vanilla bridge. Reinforced, Advanced, and Exotic Machine Frames build upward from later structural requirements instead of replacing early recipes. |
| Coal | Solid fuel-burning generator chassis and Coal Dust | Coal can be crushed into `rngtech:coal_dust`, which combines with a water bucket to produce Carbon Exhaust Bucket for the Algae Photobioreactor. |
| Lapis lazuli | Nullifier Coil | Current modifier-removal ingredient. |
| Diamond | Matrix, Stabilization Catalyst, Affix Resonance Matrix, modifier lenses, broad refinement crystals, and Stabilizer Matrix recipes through Titanium | Current precision, lens, matrix, and stability pressure. |
| Emerald | Ascension Catalyst, Ascension Matrix, Conservation Crystal, Transmutation Crystal, Expansion Crystal, and Calibrated Diamond Crystal calibration catalyst | Current rarity, value, and tuning pressure. |
| Calibrated Diamond Crystal | Tungstensteel and Nullite Stabilizer Matrix recipes | Stage 6 logic-family calibration output made from Diamond with an Emerald catalyst; used as the T7+ crystal gate. |
| Gold ingot, diamond, and emerald | Ascension Matrix | Current rarity-ascension ingredients. |
| Electric circuit tiers | Shared recipe components and machine costs | Prototype tier ladder from Basic through Ultimate; blanks are crafted from mineral insulation, alloy/conductive parts, redstone logic, and calibrated logic, then pressed with the Circuit Mold. Exposed through modern `c:circuits` tags. |
| Logs, saplings, flowers, bone meal, and charcoal | Plant Reagent and Resin | Current vanilla-accessible organic path for organic reagents, Rubber, and lubricant tag inputs. The Electric Circuit chain does not depend on these materials. |

The family ids below are registered. Their advanced behavior and most non-vanilla recipes remain design targets unless code or data recipes are added for them.

## Core Material Families

Material families are ore or ingot families. They can set component stage, base stat ranges, and recipe pressure. Recipe forms such as plates, gears, rods, coils, dusts, and casings may consume these families, but those forms should not define stage by themselves.

| Stage | Stage Role | Primary Families | Optional Compatible Families | Crafting Role |
|---:|---|---|---|---|
| 0 | Primitive | None | Stone, clay, glass, coal, charcoal, wood, plant reagents | Tutorial recipes, manual tools, disposable catalysts. |
| 1 | Iron | Iron | None | First frames, crush heads, crude cells, crude energy routing, simple casings. |
| 2 | Copper | Copper | None | Stronger powered connector, coils, heat transfer, basic energy storage. |
| 3 | Bronze | Bronze, tin, zinc, gold | Pack-defined early alloys | First alloy pressure, conductive upgrades, stronger mechanical parts. |
| 4 | Steel | Steel, invar, nickel, lead, silver | Pack-defined midgame metals | Stable midgame casings, heat cores, shielding, safer storage. |
| 5 | Aluminum | Aluminum, sparksteel, osmium | Pack-defined control metals | Cleaner control, efficient transfer, improved automation behavior. |
| 6 | Titanium | Titanium, arclite | Pack bridge alloys | High heat, high throughput, advanced control. |
| 7 | Tungstensteel | Tungstensteel, nullite, aethergold, platinum | Tungsten or pack-defined late metals | Late-game processing, transfer, storage, and affix ceilings. |
| 8 | Exotic | Pack-defined exotic ingots, defaulting to Naquadah | Netherite as a secondary Stage 8 catalyst or circuit material | Optional endgame extension and unique crafting pressure. |

Implemented transfer recipes now split the physical cable from transfer endpoints. `rngtech:cable` is a cheap bulk cable body made from wooden slabs and redstone. The Crude Energy Connector is a direct Cable, Iron Coil, and redstone craft for `64 FE/t` early routing. Basic and higher Energy Connectors are fixed-stat endpoint modules pressed with `rngtech:connector_mold` from staged coils: Iron for Basic, Copper, Gold, Sparksteel, Arclite, and eight Nullite Coils for Exotic. Fluid Connectors are pressed from matching staged casings. Item Connectors are pressed from matching staged gears. The dev-only Debug connector variants are not survival recipes.

Use shared tags where possible:

| Material Form | Tag Shape |
|---|---|
| Ore | `c:ores/<material>` block and item tags |
| Raw material | `c:raw_materials/<material>` |
| Ingot | `c:ingots/<material>` |
| Storage block | `c:storage_blocks/<material>` |
| Dust | `c:dusts/<material>` |
| Nugget | `c:nuggets/<material>` |
| Circuit | `c:circuits/<tier>` |
| Material family | `rngtech:materials/family/<material>` |
| Material family group | `rngtech:materials/family/<group>` |
| Exotic-stage recipe forms | `rngtech:materials/exotic/ingots`, `/plates`, `/gears`, `/rods`, and `/casings` |

Ore and ingot tags should be the stage authority. Other forms are recipe ingredients.

The current aggregate family group is `rngtech:materials/family/alloys`, covering Bronze, Steel, Invar, Sparksteel, Arclite, Tungstensteel, Nullite, and Aethergold forms. Alloy families intentionally exclude default alloy-dust forms from the public catalog surface; they are reached through ingot-output Alloy Furnace routes instead. Exotic-stage machine and tool recipes use `rngtech:materials/exotic/*` tags so packs can replace the default Naquadah forms without rewriting every Exotic recipe.

## Non-Stage Materials

These materials are useful for advanced recipes but should not define component stage.

| Material | Suggested Role |
|---|---|
| Redstone | Control circuits, routing, transfer logic, activation costs. |
| Lapis lazuli | Modifier removal, filtering, memory, calibration, precision catalysts. |
| Amethyst | Optional resonance or refinement flavor for pack overrides; no default survival recipe currently depends on it. |
| Quartz | Measurement, insulation, balancing, stable control boards. |
| Diamond | Ascension, high-pressure crafting, durable lenses, rare catalysts. |
| Emerald | Trading, tuning, rarity-weighted catalysts, optional pack economy hooks. |
| Ender pearl or Eye of Ender | Routing, remote transfer, phase behavior, late storage utility. |
| Glowstone | Energy visibility, high-output transfer, signal amplification. |
| Obsidian | Heat shielding, blast resistance, stable advanced casings. |
| Blaze rod or powder | Heat recipes, ignition, high-temperature catalysts. |
| Slime, honey, resin, or rubber | Current default Melter lubricant bases, plus optional gaskets, loss reduction, item durability, or non-circuit insulation in future recipes. |
| Organic reagents | Optional non-stage catalyst and recipe-risk input outside the Electric Circuit chain. |

## Recipe Forms

RNGTech can add recipe forms without turning every form into a separate material stage.

| Form | Stage Setter? | Use |
|---|---|---|
| Machine frame or casing | Yes, through its ore or ingot family | Main structural ingredient for machines and battery chassis. Base frames cover Stage 0-3 and the Steel Metal Press body; Reinforced frames cover most other Stage 4 Steel and Lead bodies; Advanced frames cover Stage 5-6; Exotic frames cover Stage 7-8 and require Lubricated Frame Coupling as a fluid-backed assembly gate. |
| Mechanism | Yes, through its ore or ingot family | Mechanical recipe core for crushers, furnaces, and moving parts. |
| Heat core | Yes, through its ore or ingot family | Furnace temperature, heat transfer, and alloy gates. |
| Crush head | Yes, through its ore or ingot family | Crusher processing speed, output amount, and processing level. |
| Energy coil | Yes, through its ore or ingot family | Transfer, generation, storage scaling, and energy loss rules. |
| Battery cell | Yes, through its ore or ingot family | Portable or chassis-installed FE capacity and per-cell transfer. |
| Control board | Usually no | Redstone, quartz, lapis, diamond, emerald, and circuit-style behavior. |
| Electric circuit | No | Shared recipe component. Tiers use common tags such as `c:circuits/basic` and should get sharply more expensive as the tier rises. |
| Insulator | No | Leakage reduction, stability, side safety, and heat shielding. |
| Lens, matrix, or catalyst | No | Refinement, rarity ascension, modifier targeting, or recipe risk. |
| Scrap, fragment, or spent catalyst | No | Recycling outputs and upgrade-failure recovery. |

## Advanced Machine Needs

Use this table as a checklist when adding advanced machines or upgrade recipes.

| Content Area | Required Material Support | Notes |
|---|---|---|
| Crusher upgrades | Crush heads, mechanisms, abrasive compounds, output-bin materials | Gate higher ores through `PROCESSING_LEVEL` and better crush heads. |
| Furnace and heat processing | Heat cores, refractory casings, insulation, ignition catalysts | Gate alloys through `MAX_TEMPERATURE`, `PROCESSING_LEVEL`, and `TEMPERATURE_STABILITY`. |
| Energy generation | Coils, heat shields, fuel chambers, control boards | Keep generator personality separate from fuel item value. |
| Energy transfer | Cables, coils, connectors, insulators, and circuits | Use material personality for transfer, loss, and stability. |
| Battery cells | Cell shells, electrolytes, conductive plates, stabilization catalysts | Cells provide most stored FE and their own input and output rates. |
| Battery chassis | Frames, bus bars, balancing boards, shielding, redstone controls | Chassis material controls slot count and global behavior, not large hidden capacity. |
| Affix and refinement systems | Lapis, gold, diamond, emerald, calibrated diamond crystals, matrices, nullifier coils, spent catalysts | Keep refinement recipes expensive enough that modifier rolls matter. |
| Component upgrades | Target-stage ingot family, risk catalyst, recycling byproducts | Upgrades preserve investment but do not grant fresh Refinement Potential. |

## Recommended Material Sets

### Standalone Vanilla Baseline

RNGTech should remain playable with vanilla ingredients until optional compatibility recipes are added.

| Role | Materials |
|---|---|
| Structure | Stone, smooth stone, iron, copper, obsidian, netherite. |
| Energy and control | Redstone, copper, gold, quartz, glowstone. |
| Refinement | Lapis, gold, diamond, emerald, calibrated diamond crystals. |
| Heat and fuel | Coal, charcoal, blaze powder, lava bucket. |
| Storage and insulation | Copper, iron, gold, quartz, slime, honey, leather. |

### Common Modded Compatibility

Advanced recipes should prefer optional `c:` tags for common modded metals instead of hard dependencies.

| Role | Suggested Tags |
|---|---|
| Early alloys | `c:ingots/tin`, `c:ingots/bronze`, `c:ingots/zinc` |
| Midgame structure | `c:ingots/steel`, `c:ingots/invar`, `c:ingots/nickel`, `c:ingots/lead`, `c:ingots/silver` |
| Energy and transfer | `c:ingots/sparksteel`, `c:ingots/arclite`, `c:ingots/nullite`, `c:ingots/aethergold` |
| Advanced machine bodies | `c:ingots/aluminum`, `c:ingots/osmium`, `c:ingots/titanium` |
| Late-game pressure | `c:ingots/tungsten`, `c:ingots/tungstensteel`, `c:ingots/platinum` |

When a tag may not exist, the recipe should have a vanilla fallback, a datapack override path, or remain disabled until the pack supplies the material.

## Implementation Notes

- The staged material ladder is registered as stable RNGTech content for ids, tags, recipes, and pack integration, with catalog exclusions for forms that have no current downstream use.
- RNGTech-owned ore ids are block items, while non-ore material forms remain item-only unless a content page says otherwise.
- Prefer tag ingredients for metals and direct item ids for distinctive vanilla catalysts.
- Manual early plate forming uses shapeless `rngtech:forming_hammer` recipes for Iron, Copper, Bronze, and Gold plates at four ingots per two plates, with one full hammer lasting 60 plates. Manual shaped Bronze Gear uses four Bronze Ingots. Manual shaped material casing recipes currently exist for Iron, Copper, Bronze, Gold, and Naquadah and use five matching ingots plus three matching nuggets; powered Metal Press plate recipes use one matching ingot with `rngtech:plate_mold` for every registered material plate form. Powered Metal Press casing recipes use four matching ingots with `rngtech:casing_mold` only for casing forms that feed downstream recipes. Powered Metal Press gear recipes use three matching ingots with `rngtech:gear_mold` for every registered material gear form.
- Manual shaped rod crafting uses three diagonal nuggets for Iron, Copper, Gold, Steel, Aluminum, Sparksteel, Titanium, Nullite, Netherite, and Naquadah instead of the broader ingot-based rod shortcut.
- Keep stage identity on ore and ingot families, not on plates, gears, rods, wires, coils, or dusts.
- Keep alloy dusts out of the default survival progression unless a pack explicitly adds and owns that route.
- Use recipe forms to make advanced crafting readable, but keep forms data-driven and replaceable by packs.
- Keep the [Current Implementation Matrix](current-implementation.md) authoritative for what is actually registered.
- Advanced material stages do not imply a shared storage system or arbitrary world-block modifiers.

## Related

- [Current Implementation Matrix](current-implementation.md)
- [Material Catalog Coverage](material-catalog-coverage.md)
- [Component Stages](component-stages.md)
- [Crafting and Upgrades](../systems/crafting.md)
- [Battery Chassis](../content/battery-chassis.md)
- [Battery Cells](../content/battery-cells.md)
