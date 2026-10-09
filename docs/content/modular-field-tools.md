# Modular Field Tools

Status: Prototype

Player guide: [Tool Bench](https://c-pettersson.github.io/rngtech/tool-bench/)

Modular Field Tools are assembled from a Tool Head, Tool Rod, and optional Battery Cell. Assembled tools are blank carriers: they store installed part stacks, damage, and optional Battery Cell data, then derive effective stats from the current installed parts.

## Implemented Content

| Content | Resource ids | Notes |
|---|---|---|
| Tool Bench | `rngtech:tool_bench` | Placed station with Build, Gear, Stats, and Refine tabs. Build has tool, head, rod, Battery Cell, and repair material slots and validates Battery Support; removed components keep their stored data. Gear has a Tiny Anvil slot and one Energy Connector slot that enables block FE input for the Tool slot. Refine has Tool, Head, Rod, and catalyst slots. |
| Tiny Anvil | `rngtech:tiny_anvil` | Reusable Tool Bench Gear item that gates Hammer, Digger, and Treefeller assembly and head swaps. |
| Diamond Tip | `rngtech:diamond_tip` | Consumable Tool Bench upgrade for untipped Steel Pick Heads and Steel Hammer Heads. The upgrade is stored on the head stack and raises ore-hardness reach by `+1`. |
| Assembled tools | `rngtech:modular_pick`, `rngtech:modular_hammer`, `rngtech:modular_shovel`, `rngtech:modular_digger`, `rngtech:modular_axe`, `rngtech:modular_treefeller` | Dedicated item ids per action family. Installed part stacks and Battery Cell data are stored on the assembled stack. The installed Tool Head family must match the assembled tool family. |
| Tool Heads | `rngtech:<material>_<family>_head` | Families are Pick, Hammer, Shovel, Digger, Axe, and Treefeller. Materials are Flint, Iron, Copper, Bronze, Steel, Aluminum, Titanium, Tungstensteel, Nullite, and Exotic. |
| Tool Rods | `rngtech:<material>_tool_rod` variants | Rods define durability, mining-speed adjustment, attack-speed adjustment, FE transfer, control, and Battery Support. Battery Support only gates installed and inventory Battery Cell stage. |

## Runtime Behavior

Hammer and Digger `3x3` areas include extra targets only when they are valid pick-minable or shovel-minable blocks. Treefellers break connected logs up to the effective `TREE_FELL_LIMIT`. Picks with the rare Vein Miner Pick Head prefix break connected same-type ore blocks up to `VEIN_MINE_LIMIT` unless the player is sneaking. Assembled tools expose a main-hand attack-speed attribute derived from the installed Tool Head family plus the installed Tool Rod's swing adjustment.

Durability and FE are charged per block that is actually broken. Vein Miner also requires `VEIN_MINE_FE_USAGE` extra FE for each connected block after the targeted block; when that surcharge cannot be paid, remaining connected blocks are skipped. Stability above `1.0` gives a per-block chance to skip both durability and FE cost equal to the amount above `1.0`; for example, `1.10` Stability gives a `10%` cost-skip chance. If Stability does not skip the cost, Control above `1.0` deterministically moves part of the durability cost onto the installed Battery Cell as family-based shield FE. The Control FE share is `control - 1.0`, capped at `100%`; for example, `1.60` Control pays `60%` of each durability point as FE and leaves `40%` as accumulated fractional durability wear. Full shield FE costs are Pick `16 FE`, Shovel `12 FE`, Axe `16 FE`, Hammer `32 FE`, Digger `28 FE`, and Treefeller `28 FE`; the current Control share scales that cost and rounds it to whole FE. If full shield FE cannot be paid from an accepted installed Battery Cell, tools continue with durability-only work for that block. When durability reaches `0`, the tool remains in the stack but stops acting as a modular tool until repaired. Installed Battery Cells expose an item energy capability through the tool stack, and compatible inventory Battery Cells can recharge the installed cell in deterministic inventory order when their stage is within the tool's effective Battery Support. A Tool Bench with an installed Energy Connector exposes FE input and forwards received FE into the inserted modular tool, capped by the connector tier and the tool's effective FE transfer. The same Tool slot charging path accepts Miner's Companions and charges their installed Battery Cells through the Miner's Companion item energy capability.

Tool Heads and Tool Rods can roll the Regenerative Self Repair prefix (`ADD SELF_REPAIR`, fixed `3`, `4`, `6`, `8` by tier). The assembled tool sums the installed parts' values and repairs every `SELF_REPAIR_INTERVAL_TICKS = 150` while in a hotbar slot or the offhand and not in active use.

Generic two-tool crafting repair is intentionally disabled for modular tools so the installed component data is not discarded.

Ore Burst applies only to Pick and Hammer tools whose head or rod material has the `oreBurst` flag in `ToolHeadMaterial` or `ToolRodMaterial`, and only adds speed when the parts roll `ORE_BURST_SPEED` (the Ore Burst suffix; base `0`). It increases break speed only on blocks tagged `rngtech:ore_burst_targets` while the tool can pay `ORE_BURST_FE_USAGE`, and charges that extra FE only for ore blocks that are actually broken.

RNGTech ores add an ore-hardness gate separate from block destroy speed, vanilla tier tags, component stage, and Crusher `required_processing_level`. Blocks tagged `rngtech:ore_hardness/level_<n>` require a Modular Pick or Hammer with effective `MINING_LEVEL >= n`, except for the configured vanilla bridge. The default bridge lets stone-or-better vanilla pickaxes harvest hardness `1` and iron-or-better vanilla pickaxes harvest hardness `2`; hardness `3+` requires modular tool reach unless the pack changes `worldgen.ores.vanillaToolBridgePolicy`. Each head's reach is `ToolHeadMaterial.miningLevel()` plus `1` when Diamond-tipped; the player guide lists every material's reach. Hammers evaluate this per target and skip over-hard ore blocks without breaking them or charging durability/FE for those skipped blocks.

Luck is tool-local. For each drop stack from an ore-burst target or log broken by a modular tool without Silk Touch, it rolls `min(LUCK, 5)` independent `15%` chances and adds one copy per success; for RNGTech ores this happens after the ore-hardness gate passes and the ore loot table has produced its normal raw material drops. It does not affect machines, recipes, chests, mobs, or unrelated loot tables.

## Refinement

Tool Heads and Tool Rods are normal Affix Forge targets. The Tool Bench Refine tab uses the Affix Forge-style outcome preview without upgrade slots and accepts only Affix Injector, Ascension Catalyst, and Ascension Matrix. Lenses, removals, rerolls, Expansion Crystal, and modifier crystals belong to the Affix Forge until the Tool Bench gets its own focus-slot pass.

Assembled modular tools are not refinement or debug-reroll targets, and they do not roll or keep their own affixes. Swapping parts returns removed components with their stored rolls intact, while the assembled tool immediately reflects the base stats and affixes of the currently installed head and rod.

Tool Head and Tool Rod rolls use field-tool tuning: their Refinement Potential range is the normal component-stage range plus `2`, and their affix tier rolls have a small high-tier bias compared with generic components. Pick Heads use a pick-specific profile that can roll the rare Vein Miner prefix; non-pick heads continue to use the general Tool Head profile.

## Current Limits

The first slice uses simple shaped recipes for heads and rods. Resonance Calibrator rod stabilization gates are still a design target until calibrated tool-part recipes are added. There is no mode UI for Hammer or Digger size or target filtering yet; the implemented area behavior uses the authored area stats directly.
