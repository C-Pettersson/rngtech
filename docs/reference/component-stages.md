# Component Stages

Status: Prototype

Component stages define the material progression for machine components. A stage uses ore and ingot families, such as Iron Stage, Copper Stage, and Steel Stage. Modifier tiers remain separate and only describe affix quality.

RNGTech targets a medium-large modpack progression curve. The first release should support more stages than a simple vanilla tech mod, but it should not require GTNH-scale material depth.

## Vocabulary

| Term | Meaning |
|---|---|
| Stage | Material progression band for components and recipes. |
| Modifier tier | Affix quality range. Example: tier 1 speed modifier, tier 3 output modifier. |
| Family rating | Component-family performance rank, such as Heat Rating 1 or Heat Rating 2. |

Use stage for material progression. Use tier for modifier quality. Use rating when a specific component family needs its own performance rank.

## Stage Rules

- Component stage controls natural modifier tier weighting and Refinement Potential expectations, but ordinary affix tiers are not hard-capped by stage.
- Upgrading a component can raise its component stage.
- Upgrading a component does not grant new [Refinement Potential](../systems/progression.md#refinement-potential).
- Breakthrough upgrades can raise a component one stage above the recipe target or raise a family rating.
- Pack makers can disable stages, remap stages, or add higher stages through datapacks and configs.
- Stages 0-3 teach processing through gating, pausing, speed penalties, or low output. They should not normally consume inputs into processing failures.
- Stage 4 is the first default failure-bearing component stage. Machines at this stage can turn active work into recoverable failure outputs when control conditions are bad.
- Stage 5+ components should improve failure mitigation through better control, stability, and recovery rather than simply increasing raw throughput.

## Compatibility Policy

Use item tags for material compatibility. In modern NeoForge, these tags replace the old OreDictionary pattern.

Component stage materials should use shared `c:` ore and ingot tags:

| Material Form | Tag Shape |
|---|---|
| Ore | `c:ores/<material>` |
| Ingot | `c:ingots/<material>` |

Do not define component progression through plates, gears, rods, wires, coils, dusts, gems, or storage blocks. A recipe can still craft an item or block called a coil, cell, or battery chassis, but its stage comes from the ore or ingot family used to make it.

Recipes should use tag ingredients where practical. Material-specific recipes can still use direct item ids when a unique item matters. Non-metal catalysts, plant reagents, and utility items can appear in recipes, but they do not set the component stage.

If a material comes from another mod, RNGTech should reference its tag as optional compatibility. RNGTech should not require that mod unless the feature depends on it.

The advanced material checklist and recipe-form vocabulary live in [Materials List](materials.md).

## Survival Stage List

RNGTech starts with one primitive stage, seven core survival component stages, and one optional extension stage. Stage 8 Exotic is the top current code-backed scope; higher endgame ideas are outside this cut unless a future design reintroduces them as new content.

| Stage | Name | Ore or Ingot Families | Natural Tier Target | Role |
|---:|---|---|---:|---|
| 0 | Primitive Stage | No ore or ingot stage | 1 if rollable | Manual or tutorial crafting. No serious component investment. Includes wooden machine bodies and flint crusher tooling. |
| 1 | Iron Stage | Iron | 1 | First mechanical components, crude energy transfer, and early machine or battery chassis. |
| 2 | Copper Stage | Copper | 1 | Stronger energy transfer, heat transfer, coils, and battery cells. |
| 3 | Bronze Stage | Bronze, tin, zinc, gold | 2 | First alloy pressure and stronger early components. |
| 4 | Steel Stage | Steel, invar, nickel, lead, silver | 2 | Stable midgame machines, heat processing, and the first recoverable processing failures. |
| 5 | Aluminum Stage | Aluminum, sparksteel, osmium | 3 | Better control, higher transfer, cleaner recipes, and stronger failure mitigation. |
| 6 | Titanium Stage | Titanium, arclite | 3 | High heat, high throughput, and pack bridge stage. |
| 7 | Tungstensteel Stage | Tungstensteel, nullite, aethergold, platinum | 4 | Late-game material pressure and strong affix ceilings. |
| 8 | Exotic Stage | Pack-defined exotic ingots or ores, with Naquadah as the default RNGTech implementation; Netherite remains a Stage 8 secondary material | 4 | Optional endgame extension for large packs and the highest current RNGTech component stage. |

Packs can swap the order of Iron Stage and Copper Stage if their resource curve wants copper before iron. RNGTech uses separate stage names so packs can move one stage without renaming the whole ladder.

## Power Budget By Stage

Power progression should be visible at the component-stage level. This is not a strict voltage ladder: RNGTech still uses FE, machine stats, Battery Cells, Battery Chassis, and connector limits rather than a mandatory one-cable-per-tier system. The budget table gives each stage a default expectation for base, unmodified setups so generator output, storage, transfer, and machine recipe costs can be compared during implementation.

Use these rules when checking a stage:

- A same-stage primary generator should either sustain one normal same-stage processing lane or make the shortfall obvious enough that the player is expected to build multiples, add storage, or refine parts.
- Passive generation can sit below the active-machine target, but it must scale cleanly through arrays or storage.
- A processing machine's FE/t is a derived value: `effective FE/t = adjusted recipe FE / adjusted ticks`. Speed improvements raise FE/t unless `ENERGY_USAGE` or output amount changes the economics.
- Battery Cells and Battery Chassis are expected to buffer intermittent early production and machine demand spikes. They should not hide a stage where generation is so low that ordinary processing mostly means waiting.

| Stage | Baseline Production Target | Current Code-Backed Production | Machine Consumption Expectation | Current Code-Backed Consumers | Gap / Balance Read |
|---:|---:|---|---:|---|---|
| 0 | `0-8 FE/t` | No true Stage 0 grid generator. Bio Generator burns at `8 FE/t` but is crafted with Copper-stage ingredients. | `0-8 FE/t` tutorial work. | Stage 0 Crusher biomass recipes use `5 FE/t`: `80 FE / 16 ticks` for seed feedstock and `600 FE / 120 ticks` for Organic Reagent. Stage 0 Furnace is fuel-only. | Primitive power is deliberately weak. If Stage 0 power becomes a real path, keep it tutorial-scale. |
| 1 | `16-32 FE/t` | Crude Solid Fuel Burner with Iron Heat Core: `24 FE/t`; Crude Solar Panel: `4 FE/t` clear day. | `24-48 FE/t` for first electric machines. | Furnace smelting commonly sits near `24 FE/t`; Crusher ore recipes are `48 FE/t`; Component Recycler recipes are usually below `13 FE/t`. | One Stage 1 burner now sustains ordinary Furnace work and can buffer toward Crusher work. Wooden Composter improves Bio Generator fuel density with Composted Biomass, but it does not directly raise FE/t. |
| 2 | `32-64 FE/t` | Copper Solid Fuel Burner with Copper Heat Core: `40 FE/t`; Copper Solar Panel: `8 FE/t`; Bio Generator: `8 FE/t`; Potential Reactor starter fuels generate `40-75 FE/t` after setup. | `24-64 FE/t`. | Crusher ore remains `48 FE/t`; Furnace metals are usually `24-36 FE/t`; Alloy Furnace recipes are `10-19 FE/t`; Resonance Calibration recipes are `7-8 FE/t`. | Copper nearly sustains one Crusher ore lane before storage and modifiers. Rich Biomass makes Bio Generator fueling less painful, but active Bio Generator output remains low. |
| 3 | `48-96 FE/t` | Alloy Solid Fuel Burner with Bronze Heat Core: `64 FE/t`; Gold Solar Panel: `12 FE/t`. | `48-96 FE/t` for sustained ore processing plus first alloy/circuit work. | Crusher ore is `48 FE/t`; Alloy Furnace remains below `20 FE/t`; early Metal Press connector/circuit recipes are `10-20 FE/t`. | Bronze now comfortably sustains one normal Crusher lane and leaves headroom for lower-cost support machines. |
| 4 | `96-192 FE/t` | Steel Solid Fuel Burner with Steel Heat Core: `96 FE/t`; Solar Array Controller can aggregate up to eight Gold panels for `96 FE/t` clear day; Potential Reactor remains `24-75 FE/t` before modifiers. | `96 FE/t` for bulk lanes, with `48 FE/t` still the normal single Crusher lane. | Lead Furnace can run four `24 FE/t` lanes for about `96 FE/t`; Metal Press recipes are `10-20 FE/t`; Component Recycler stays mostly below `13 FE/t`. | Stage 4 solid fuel and clear-day solar now both hit the bulk-lane target; weather and night still create storage pressure. |
| 5 | `192-384 FE/t` | Sparksteel Solar Panel: `24 FE/t`; Solar Array with eight Sparksteel panels reaches `192 FE/t` clear day and `64 FE/t` in weather. Syngas Combustor burns `57,600 FE` per `1,000 mB` Coal Gasifier Syngas over `240` ticks: `240 FE/t` before Servo and rolls. | `128-384 FE/t` for faster controlled machines and higher transfer expectations. | Aluminum Crusher/Furnace improve speed or efficiency; Coal Gasifier consumes Coal Dust and water for Syngas; Syngas Combustor converts Syngas to FE plus Carbon Exhaust. | Stage 5 now has an active gas-power identity instead of only passive solar scaling. Syngas is the step up from coal: faster than any Stage 4 burner and about 2.5x the net FE per Coal of a Steel burner after gasification. |
| 6 | `512-1,024 FE/t` | Steam Methane Reformer produces Hydrogen and Carbon Monoxide from Methane plus water, with Syngas as a lower-yield transitional feed. Cavitation nitrogen Gear produces Nitrogen from water as a utility source for the Ammonia Fuel Cell chain. The Ammonia Fuel Cell burns `216,000 FE` per `1,000 mB` Ammonia over `360` ticks: `600 FE/t` before membrane rolls, about `510 FE/t` net of the synthesis chain. Ordinary Collapse Nozzles are shared with Vacuum Collapse, while Nitrogen Separation Nozzle remains Cavitation-only. | `160-512 FE/t` per serious machine or multi-lane setup before overclocking. | Titanium Alloy Furnace accepts `192 FE/t`; Melter lubricant recipes are `4,800 FE / 240 ticks = 20 FE/t`; methane-from-algae Melter chemistry feeds SMR; Arclite Battery Chassis transfers `1,024 FE/t`. | Stage 6 is now chemistry scale-up: SMR is the intended Hydrogen source, while Cavitation with dedicated nitrogen rotor/nozzle supplies Nitrogen without making water cavitation a strong power source. |
| 7 | `1,024-4,096 FE/t` | Vacuum Collapse Generator: Void Catalyst authors `600,000 FE / 600 ticks = 1,000 FE/t`; an unmodified Tungstensteel Void Chamber, Collapse Nozzle, and Dimensional Stabilizer setup generates about `1,430 FE/t`, and an all-Exotic part setup about `3,000 FE/t`. | `512-3,072 FE/t` for dense, bursty, or high-throughput late-game lines. | Tungstensteel Crusher batches up to four items; Nullite Battery Cell outputs `2,048 FE/t`; Aethergold Battery Cell outputs `3,072 FE/t` with no idle loss; Nullite Battery Chassis transfers `2,048 FE/t`; Aethergold Battery Chassis transfers `3,072 FE/t` with the strongest current burst identity. | Vacuum Collapse is the Stage 7 primary generator. Unmodified parts sit at the low end of the band so rolled and refined parts carry the climb; one catalyst per 30 seconds keeps catalyst supply a real logistics cost. |
| 8 | `4,096-16,384 FE/t` | No Stage 8 primary generator. | `2,048-8,192 FE/t` for optional pack-endgame machines and ordinary high-end transfer; Exotic Energy Connector is an expensive `1,000,000 FE/t` endpoint outlier. | Exotic Battery Cell outputs `4,096 FE/t`; Exotic Battery Chassis transfers `4,096 FE/t`; Exotic Energy Connector transfers `1,000,000 FE/t`. | Exotic storage and control exist, but production has no matching survival source. Stage 8 is the top current code-backed scope. |

### Current FE/t Anchors

These anchors should be rechecked when changing recipe energy, processing time, generation, or stage unlock order.

| Surface | Code-backed baseline |
|---|---:|
| Stage 0 Crusher biomass recipes | `5 FE/t` |
| Crusher raw, stone, and deepslate ore recipes | Stage 1-4: `48 FE/t`; Stage 5-8 material-form outputs author `96`, `144`, `192`, and `288 FE/t` before high-hardness surcharges |
| Furnace recipes | Early/common recipes: `15-36 FE/t`; Stage 5-8 material smelting authors higher FE, and Stage 7+ heat-band recipes can reach roughly `96-288 FE/t` before machine `ENERGY_USAGE` |
| Alloy Furnace recipes | `10-19 FE/t` |
| Metal Press recipes | `10-20 FE/t` |
| Melter lubricant recipes | `20 FE/t` |
| Melter lava recipe | about `27 FE/t` |
| Resonance Calibration recipes | `7-8 FE/t` |
| Component Recycler recipes | roughly `5-13 FE/t` |
| Potential Reactor starter fuels | `24-75 FE/t` generated before modifiers |
| Solid Fuel Burner Heat Cores | Stage 1-4: `24`, `40`, `64`, `96 FE/t`; fuel holds `10 FE` per effective burn tick (Coal `16,000 FE` before fuel stats) |
| Solar Panels | Stage 1, 2, 3, and 5: `4`, `8`, `12`, `24 FE/t` clear day |
| Syngas Combustor | Syngas: `57,600 FE / 240 ticks = 240 FE/t` |
| Ammonia Fuel Cell | Ammonia: `216,000 FE / 360 ticks = 600 FE/t` |
| Vacuum Collapse Generator | Void Catalyst: `600,000 FE / 600 ticks = 1,000 FE/t` before Void Chamber, Collapse Nozzle, and Dimensional Stabilizer multipliers |

## Failure Introduction

Stage 4 is where normal machine processing first gains a real failure state. Earlier component stages should block invalid work, pause on missing power, run slowly, or produce modest output, but they should avoid consuming a player's input into a bad result during ordinary processing.

Steel Stage machines can ask the player to manage stable FE, controlled heat, and precision parts. The current reference example is the [Metal Press](../content/metal-press.md): the pre-Steel Crude Press hard-blocks unsafe work without consuming inputs, while the Stage 4 Steel Press can produce recoverable failure outputs such as `rngtech:malformed_ingot` or `rngtech:broken_circuit` when heat overshoots the safe band, temperature stability is poor, power drops after work has started, or Servo control is weak.

Failure outputs should preserve recovery value. They should represent lost work or damaged material, not silent deletion. When a failure output needs source identity, prefer one generic failure item with a data component, such as `rngtech:malformed_ingot` with `rngtech:material = "steel"`, instead of per-material failure item ids.

Failure risk must be visible and mechanically tied to installed components. Heat Cores should define the reachable temperature band, Servos should provide precision and failure resistance, Battery Cells or machine buffers can smooth FE drops, and later-stage parts should make the same process safer before they make it faster.

## Material Inspiration

The stage list borrows common material roles from established modded Minecraft ecosystems:

| Source Pattern | Useful Lesson for RNGTech |
|---|---|
| Thermal-style upgrades | A compact ladder can use Basic, Hardened, Reinforced, Arclite, and Resonant roles. |
| Mekanism-style upgrades | Basic, Advanced, Elite, and Ultimate roles give clear machine and energy storage jumps. |
| Immersive Engineering | Copper, sparksteel, and steel work well as readable low, mid, and high electrical materials. |
| Tinkers-style materials | Many materials can coexist when each one has a clear role or trait. |
| GregTech / GTNH | Deep packs need stat gates and material stages, but RNGTech should not require ten-plus mandatory stages in its first release. |

## Component Family Tracks

Different component families can use different materials in the same stage. The stage number matters more than the exact material.

### Heat Cores

Heat cores control furnace temperature, heat transfer, temperature stability, and heat-related recipe gates.

| Stage | Example Heat Core | Expected Tags | Approximate Temperature Band |
|---:|---|---|---:|
| 1 | Iron Heat Core | `c:ingots/iron`, `c:ores/iron` | `600-850` |
| 2 | Copper Heat Core | `c:ingots/copper`, `c:ores/copper` | `700-950` |
| 3 | Bronze Heat Core | `c:ingots/bronze` | `850-1150` |
| 4 | Steel Heat Core | `c:ingots/steel`, `c:ingots/invar` | `1050-1400` |
| 5 | Aluminum Heat Core | `c:ingots/aluminum` | Fast low-heat sidegrade below the Furnace `1000` band |
| 5 | Sparksteel Heat Core | `c:ingots/sparksteel` | `1250-1650` |
| 6 | Titanium Heat Core | `c:ingots/titanium`, `c:ingots/arclite` | `1500-2000` |
| 7 | Tungstensteel Heat Core | `c:ingots/tungstensteel`, `c:ingots/nullite`, `c:ingots/aethergold` | `1800-2450` |
| 8 | Exotic Heat Core | `rngtech:materials/exotic/ingots`, pack-defined exotic ore or ingot tags | `2200-3200` |

Recipe packs can gate alloys by `MAX_TEMPERATURE`, `PROCESSING_LEVEL`, and `TEMPERATURE_STABILITY`.

### Servos

Servos control precision machine motion, power-drop tolerance, and failure resistance for machines that need controlled forming or positioning. They are a Stage 4+ component family because Stage 4 is where processing failures become part of normal machine design.

| Stage | Example Servo | Expected Tags | Role |
|---:|---|---|---|
| 4 | Steel Servo | `c:ingots/steel`, `c:ingots/invar` | First powered forming control component, used by the Metal Press to reduce malformed-output risk. |
| 5 | Aluminum Servo | `c:ingots/aluminum`, `c:ingots/sparksteel` | Faster control with better power-drop recovery and failure mitigation. |
| 6 | Titanium Servo | `c:ingots/titanium`, `c:ingots/arclite` | Stable high-throughput control. |
| 7 | Tungstensteel Servo | `c:ingots/tungstensteel` | Late-game precision control. |
| 7 | Nullite Servo | `c:ingots/nullite` | Fluid-output safety sidegrade with lower throughput. |
| 8 | Exotic Servo | Pack-defined exotic ore or ingot tags | Optional endgame control behavior. |

The current prototype implements `rngtech:steel_servo`, plus Stage 6+ `rngtech:titanium_servo`, `rngtech:tungstensteel_servo`, `rngtech:nullite_servo`, and `rngtech:exotic_servo`. Stage 6+ Servo recipes require lubricant: crafting uses `rngtech:lubricant_bucket`, and the Component Assembler can consume `1000 mB` stored Lubricant through the compatibility `rngtech:battery_assembly` recipe type. The Steel Servo recipe intentionally remains lubricant-free to avoid a Stage 4 progression loop. Nullite Servo is a Stage 7 sidegrade with fixed Auto Purge behavior and `0.5x PROCESSING_SPEED`. Cavitation Generator accepts only Stage 6+ Servos for nitrogen utility control, so Steel Servo remains a Metal Press and early-control part rather than a chemistry component.

### Fluid Pumps

Fluid Pumps control fluid transfer for machines with fluid input or output. They are a Stage 5+ component family introduced with the Melter and lubricant.

| Stage | Example Fluid Pump | Expected Tags | Runtime transfer |
|---:|---|---|---:|
| 5 | Osmium Fluid Pump | `c:ingots/osmium` | `125 mB/t` |
| 6 | Titanium Fluid Pump | `c:ingots/titanium`, `c:ingots/arclite` | `250 mB/t` |
| 7 | Tungstensteel Fluid Pump | `c:ingots/tungstensteel`, `c:ingots/nullite` | `500 mB/t` |
| 8 | Exotic Fluid Pump | Pack-defined exotic ore or ingot tags | `1000 mB/t` |

The current prototype implements all three listed pumps as Melter Gear parts. No RNGTech fluid pipes are implemented; machines expose standard NeoForge fluid capabilities when the installed pump allows it.

### Crush Heads

Crush heads control processing speed, output amount, output slots, processing level, and special ore-processing effects. On Crushers, `PROCESSING_LEVEL` is the crush hardness level compared against recipe `required_processing_level`; under-level heads can still run the recipe, but more slowly, with higher total FE cost, and with jam risk.

| Stage | Example Crush Head | Expected Level | Expected Tags | Role |
|---:|---|---:|---|---|
| 0 | Flint Crush Head | `1` | `minecraft:flint` | Primitive raw-ore crushing only. Slow and not worth long-term refinement. |
| 1 | Iron Crush Head | `2` | `c:ingots/iron`, `c:ores/iron` | First ore processing, including vanilla stone and deepslate ore blocks. |
| 2 | Copper Crush Head | `2` | `c:ingots/copper`, `c:ores/copper` | Better early throughput and conductive upgrades. |
| 3 | Bronze Crush Head | `3` | `c:ingots/bronze` | Better throughput and early output rolls for source-ore processing. |
| 4 | Steel Crush Head | `4` | `c:ingots/steel`, `c:ingots/invar` | Reliable midgame processing. |
| 5 | Aluminum Crush Head | `5` | `c:ingots/aluminum`, `c:ingots/sparksteel` | Faster processing and cleaner automation. |
| 6 | Titanium Crush Head | `6` | `c:ingots/titanium`, `c:ingots/osmium` | Higher processing level and advanced ores. |
| 7 | Tungstensteel Crush Head | `7` | `c:ingots/tungstensteel`, `c:ingots/nullite` | Late-game processing pressure. |
| 8 | Exotic Crush Head | `8+` | Pack-defined exotic ore or ingot tags | Optional endgame and unique outcomes. |

### Machine Chassis

Machine chassis items define the machine body or class. Chassis stage should describe the body material, not the installed parts.

| Stage | Example Chassis | Expected Tags | Role |
|---:|---|---|---|
| 0 | Wooden Crusher Chassis | `minecraft:planks`, `minecraft:stick` | Primitive Crusher body used as the first assembly step. |
| 1 | Iron Crusher Chassis | `c:ingots/iron`, `c:ingots/copper` | First powered Crusher body. |
| 2 | Copper Crusher Chassis | `c:ingots/copper` | Conductive early chassis with better FE transfer. |
| 3 | Bronze Crusher Chassis | `c:ingots/bronze` | Alloy chassis with better output behavior. |
| 4 | Steel Crusher Chassis | `c:ingots/steel`, `c:ingots/invar` | Durable midgame chassis with efficiency and buffer strength. |
| 5 | Aluminum Crusher Chassis | `c:ingots/aluminum`, `c:ingots/sparksteel` | Faster controlled chassis with cleaner energy behavior. |
| 6 | Titanium Crusher Chassis | `c:ingots/titanium`, `c:ingots/arclite` | High-throughput chassis for advanced processing. |
| 7 | Tungstensteel Crusher Chassis | `c:ingots/tungstensteel`, `c:ingots/nullite` | Heavy late-game output chassis. |
| 8 | Exotic Crusher Chassis | `rngtech:materials/exotic/ingots` | Optional endgame chassis. |

Crusher chassis are placed machine blocks in the current prototype. They gate the maximum installed Crush Head stage, provide authored base stat profiles, and carry fixed machine behavior identity.

### Component Recycler Chassis and Heads

Component Recyclers use a manual Stage 2 bootstrap block plus the powered Stage 1-8 ladder. The Crude Recycler has no Disassembly Head and runs only tagged manual recyclables up to Stage 2 when a Hand Crank is placed above it and right-clicked through a recipe-scaled manual cycle. Powered chassis gate max accepted component recipe stage and max installed Disassembly Head stage. Disassembly Heads provide the `PROCESSING_LEVEL` gate used by `rngtech:component_recycling` recipes.

| Stage | Component Recycler chassis | Disassembly Head |
|---:|---|---|
| 2 manual | Crude Recycler | None; Hand Crank block above |
| 1 | Iron Component Recycler | Iron Disassembly Head |
| 2 | Copper Component Recycler | Copper Disassembly Head |
| 3 | Bronze Component Recycler | Bronze Disassembly Head |
| 4 | Steel Component Recycler | Steel Disassembly Head |
| 5 | Aluminum Component Recycler | Aluminum Disassembly Head |
| 6 | Titanium Component Recycler | Titanium Disassembly Head |
| 7 | Tungstensteel Component Recycler | Tungstensteel Disassembly Head |
| 8 | Exotic Component Recycler | Exotic Disassembly Head |

### Resonance Calibrator Chassis

Resonance Calibrators intentionally do not use every material stage. They are a compact family where Iron can advance directly into Steel, Copper is an optional early sidegrade, Lead is a dense sidegrade, and Stage 7 splits into branch choices.

| Stage | Chassis | Role |
|---:|---|---|
| 1 | Iron Resonance Calibrator Chassis | Stable baseline, one lane, broad early quality range. |
| 2 | Copper Resonance Calibrator Chassis | Optional conductive early frame with faster work and better FE intake. |
| 4 | Steel Resonance Calibrator Chassis | Reinforced stability body with output guard behavior. |
| 4 | Lead Resonance Calibrator Chassis | Dense sidegrade with two slower bulk lanes. |
| 6 | Titanium Resonance Calibrator Chassis | Precision frame with better control and RP outcomes. |
| 7 | Tungstensteel Resonance Calibrator Chassis | Dense multi-process endgame branch. |
| 7 | Nullite Resonance Calibrator Chassis | Single-fast precision endgame branch. |

Future Exotic calibrator content should preserve the Tungstensteel dense branch and Nullite precision branch instead of merging them into one automatic best chassis.

### Resonance Calibration Gear

Resonance Calibrator Gear uses material stage for recipe reach, natural modifier tier weighting, and Refinement Potential expectations.

| Stage | Gear material | Implemented Gear |
|---:|---|---|
| 1 | Iron | Resonance Coil, Control Board, Stabilizer Matrix |
| 2 | Copper | Resonance Coil, Control Board, Stabilizer Matrix |
| 4 | Steel | Resonance Coil, Control Board, Stabilizer Matrix |
| 6 | Titanium | Resonance Coil, Control Board, Stabilizer Matrix |
| 7 | Tungstensteel | Resonance Coil, Control Board, Stabilizer Matrix |
| 7 | Nullite | Resonance Coil, Control Board, Stabilizer Matrix |

Resonance Coils set calibration recipe stage reach. A calibration recipe can declare a lower required reach than its stored output component stage, as the Basic Electric Circuit logic recipe does for the Iron-to-Steel path. Control Boards and Stabilizer Matrices improve precision, stability, catalyst efficiency, and RP outcomes; they do not replace the required coil stage gate.

### Energy Coils

Energy coils control energy transfer, generation, storage scaling, and loss rules.

| Stage | Example Coil | Expected Tags | Role |
|---:|---|---|---|
| 1 | Iron Coil | `c:ingots/iron`, `c:ores/iron` | Primitive powered machines and low transfer. |
| 2 | Copper Coil | `c:ingots/copper`, `c:ores/copper` | Basic FE movement. |
| 3 | Gold Coil | `c:ingots/gold`, `c:ores/gold` | Better conductivity, higher cost. |
| 4 | Sparksteel Coil | `c:ingots/sparksteel` | Midgame transfer jump. |
| 5 | Aluminum Coil | `c:ingots/aluminum`, `c:ingots/sparksteel` | Stable transfer and lower loss. |
| 6 | Arclite Coil | `c:ingots/arclite`, `c:ingots/osmium` | High transfer and machine control. |
| 7 | Nullite Coil | `c:ingots/nullite`, `c:ingots/platinum` | Late-game transfer. |
| 8 | Exotic Coil | Pack-defined exotic ore or ingot tags | Optional pack-endgame transfer. |

### Battery Cells

Battery cells control item energy capacity, charge rate, discharge rate, and energy efficiency.

Block storage should use [Battery Chassis](../content/battery-chassis.md) behavior around inserted [Battery Cells](../content/battery-cells.md) instead of treating the block material as the main capacity source.

| Stage | Example Cell | Expected Tags | Role |
|---:|---|---|---|
| 0 | Potato Cell | `minecraft:potato` plus simple metal contacts | Novelty storage with aggressive leakage. |
| 0 | Voltaic Potato Cell | Drop/find only; no default recipe | Unique novelty storage outside the normal material ladder. |
| 1 | Iron Cell | `c:ingots/iron`, `c:ores/iron` | Crude chargeable storage. |
| 2 | Copper Cell | `c:ingots/copper`, `c:ores/copper` | First practical battery cell. |
| 3 | Lead Cell | `c:ingots/lead`, `c:ingots/tin` | Higher capacity, common modded battery material. |
| 4 | Invar Cell | `c:ingots/invar`, `c:ingots/steel` | Durable midgame storage. |
| 5 | Sparksteel Cell | `c:ingots/sparksteel` | Better transfer and efficiency. |
| 6 | Arclite Cell | `c:ingots/arclite`, `c:ingots/osmium` | Advanced throughput. |
| 7 | Nullite Cell | `c:ingots/nullite`, `c:ingots/platinum` | High-end storage. |
| 7 | Aethergold Cell | `c:ingots/aethergold` | No-leakage high-output storage for burst banks. |
| 8 | Exotic Cell | `rngtech:materials/exotic/ingots` | Optional endgame battery scaling. |

### Battery Chassis

Battery chassis components control stationary battery slot count, energy movement behavior, stability, efficiency, balancing, redstone control, and global modifier strength.

Inserted battery cells provide most or all stored FE. Chassis material should change how the bank behaves more than how much hidden capacity it has.

Default slot counts are design targets. Packs can remap materials or slot counts for their progression curve.

| Stage | Example Chassis | Expected Tags | Target Slots | Role |
|---:|---|---|---:|---|
| 0 | Wooden Battery Chassis | `minecraft:planks` plus simple contacts | Novelty holder with poor transfer and stability. |
| 1 | Iron Battery Chassis | `c:ingots/iron`, `c:ores/iron` | `1-2` | First stationary holder for battery cells. |
| 2 | Copper Battery Chassis | `c:ingots/copper`, `c:ores/copper` | `2` | Early energy transfer and basic buffering. |
| 3 | Gold Battery Chassis | `c:ingots/gold`, `c:ores/gold`, `c:ingots/bronze` | `3` | Conductive burst behavior with stability tradeoffs. |
| 4 | Steel Battery Chassis | `c:ingots/steel`, `c:ingots/lead`, `c:ingots/invar` | `4-5` | Stable midgame storage, shielding, and lower idle loss. |
| 5 | Sparksteel Battery Chassis | `c:ingots/sparksteel`, `c:ingots/aluminum` | `4-5` | Better transfer, efficiency, and redstone control. |
| 6 | Arclite Battery Chassis | `c:ingots/arclite`, `c:ingots/titanium`, `c:ingots/osmium` | `6` | High transfer, burst output, and advanced control. |
| 7 | Nullite Battery Chassis | `c:ingots/nullite`, `c:ingots/tungstensteel`, `c:ingots/platinum` | `7-8` | Late-game throughput and special charge-balancing behavior. |
| 7 | Aethergold Battery Chassis | `c:ingots/aethergold` | `6` | Focused burst bank with no chassis or installed-cell leakage. |
| 8 | Exotic Battery Chassis | `rngtech:materials/exotic/ingots` | `8-10` | Optional endgame storage banks and pack-defined behavior. |

High-slot chassis should not obsolete low-slot chassis. A focused low-slot chassis can have stronger global modifier impact per inserted cell, while a large chassis spreads its behavior across more cells.

## Family Ratings

Some component families need a rating in addition to material stage. A rating measures the component's functional breakthrough for that family.

Example heat ratings:

| Rating | Meaning |
|---:|---|
| Heat Rating 1 | Normal heat core for its stage. |
| Heat Rating 2 | Better temperature band or better recipe-gate reach. |
| Heat Rating 3 | Late or unique-grade heat behavior. |

A rare upgrade outcome can turn a Heat Rating 1 component into Heat Rating 2. This can happen without changing the component's material stage. For example, a Copper Stage Heat Core can become a Copper Stage Heat Core with Heat Rating 2.

Pack makers can decide whether breakthrough upgrades raise material stage, family rating, or both.

## Recipe Gate Examples

Pack makers should gate recipes by machine and component stats. Item names can act as a fallback, but stat gates preserve the ARPG item model.

| Gate | Suggested Requirement |
|---|---|
| First alloys | `MAX_TEMPERATURE >= 800` |
| Steel Stage | `MAX_TEMPERATURE >= 1100`, `PROCESSING_LEVEL >= 2` |
| First failure-bearing forming | Stage 4 Steel Press, valid Heat Core, valid Servo, valid mold, visible recoverable failure output |
| Titanium Stage | `MAX_TEMPERATURE >= 1450`, `PROCESSING_LEVEL >= 3` |
| Tungstensteel Stage | `MAX_TEMPERATURE >= 1750`, `PROCESSING_LEVEL >= 4` |
| Exotic Stage | Pack-defined stats and components |

Lucky components can cross one stage boundary. Natural tier weighting, no free Refinement Potential on upgrade, and upgrade failure outcomes stop one early item from solving the whole playthrough.

## Implementation Notes

- Use `c:ores/<material>` and `c:ingots/<material>` tags in recipes for stage materials.
- Add `rngtech:` tags for behavior categories, such as valid heat-core casing materials or valid crush-head materials.
- Use optional ore or ingot tag ingredients for modded metals that may not exist in a given pack.
- Prefer machine-specific stats such as heat, processing level, pressure, or stability for recipe reach instead of generic component-stage gates.
- Keep material stage, family rating, and modifier tier separate in data.
- Keep battery cell capacity separate from battery chassis behavior.
- Let battery chassis materials define slot count and global behavior, not large hidden storage.
- Treat Stage 4 as the first normal machine-processing failure band; earlier processing should prefer blocking, pausing, or slowing instead of consuming inputs into failed outputs.
- Keep Stage 4 failure outputs recoverable and data-driven when source material matters.
- Let datapacks remap material tags into component stages.
- Keep Unique components outside the normal material ladder unless a pack defines a specific unique upgrade chain.

## Related

- [Crafting and Upgrades](../systems/crafting.md)
- [Materials List](materials.md)
- [Battery Chassis](../content/battery-chassis.md)
- [Battery Cells](../content/battery-cells.md)
- [Machine Stats](machine-stats.md)
- [Modifier Eligibility](modifier-eligibility.md)
- [Rarity](rarity.md)
