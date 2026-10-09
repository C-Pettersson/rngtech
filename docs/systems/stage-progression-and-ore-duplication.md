# Stage Progression and Ore Duplication

Status: Prototype

Player guide: [Stages](https://c-pettersson.github.io/rngtech/stages/)

The component upgrade loop below is planned and not registered gameplay; the stage ladder and Crusher duplication path are implemented.

This page is a schema for how [Component Stages](../reference/component-stages.md), [Crafting and Upgrades](crafting.md), and the [Crusher](../content/crusher.md) connect. It does not replace those canonical rule pages.

## Progression Schema

```mermaid
flowchart TD
    subgraph stages["Component stage ladder (implemented)"]
        S0["0 Primitive Stage\nNo ore or ingot family\nNatural tier target 1"]
        S1["1 Iron Stage\nFirst mechanical components\nNatural tier target 2"]
        S2["2 Copper Stage\nFirst energy and heat transfer\nNatural tier target 2"]
        S3["3 Bronze Stage\nFirst alloy pressure\nNatural tier target 3"]
        S4["4 Steel Stage\nStable midgame machines\nNatural tier target 3"]
        S5["5 Aluminum Stage\nBetter control and transfer\nNatural tier target 3"]
        S6["6 Titanium Stage\nHigh heat and throughput\nNatural tier target 3"]
        S7["7 Tungstensteel Stage\nLate-game material pressure\nNatural tier target 4"]
        S8["8 Exotic Stage\nOptional pack endgame\nNatural tier target 4"]

        S0 --> S1 --> S2 --> S3 --> S4 --> S5 --> S6 --> S7 --> S8
    end

    subgraph upgrades["Component upgrade loop (planned)"]
        Crafted["Craft component from ore or ingot family"]
        Roll["Roll rarity, modifiers, and Refinement Potential"]
        Refine["Refine within remaining Potential"]
        Upgrade["Upgrade recipe targets a higher material stage"]
        Outcome{"Upgrade outcome"}
        Failed["Failed\nNo stage increase"]
        Fractured["Fractured\nStage increases, modifier damage"]
        Strained["Strained\nStage increases, strain added"]
        Clean["Clean\nStage increases, modifiers preserved"]
        Breakthrough["Breakthrough\nTarget stage +1 or family rating +1"]

        Crafted --> Roll --> Refine --> Upgrade --> Outcome
        Outcome --> Failed
        Outcome --> Fractured
        Outcome --> Strained
        Outcome --> Clean
        Outcome --> Breakthrough
        Failed --> Refine
        Fractured --> Refine
        Strained --> Refine
        Clean --> Refine
        Breakthrough --> Refine
    end

    S1 --> Crafted
    S2 --> Crafted
    S3 --> Crafted
    S4 --> Crafted
    S5 --> Crafted
    S6 --> Crafted
    S7 --> Crafted
    S8 --> Crafted
```

## Ore Duplication Schema

```mermaid
flowchart TD
    subgraph inputs["Crusher inputs (implemented)"]
        Raw["Raw material drops"]
        Ore["Iron, copper, gold ore"]
        Deepslate["Deepslate iron, copper, gold ore"]
        RngtechOre["RNGTech source ores\nStone, deepslate, and dimension-native variants"]
    end

    subgraph recipes["rngtech:crusher recipes (implemented)"]
        RawRecipe["Raw material recipe\nHardness: matches source ore\nBase output: 2 crushed\nBase time: 120 ticks"]
        OreRecipe["Stone ore recipe\nHardness: 2\nBase output: 3 crushed\nBase time: 160 ticks"]
        DeepslateRecipe["Deepslate ore recipe\nHardness: 2\nBase output: 3 crushed\nBase time: 200 ticks"]
        RngtechRecipe["Source ore recipes\nOre hardness: world harvest gate\nCrusher level: soft recipe pressure\nBase output: 3 crushed"]
    end

    subgraph stats["Effective Crusher stats (prototype)"]
        MachineAffixes["Crusher machine affixes"]
        CrushHead["Required installed Crush Head\nFlint level 1, Iron level 2"]
        Hardness["PROCESSING_LEVEL\nBelow recipe hardness costs more, slows, and can jam"]
        BatteryCell["Installed Battery Cell\nNo cell applies output penalty"]
        OutputAmount["OUTPUT_AMOUNT\nScales recipe output count"]
        ProcessingSpeed["PROCESSING_SPEED\nReduces processing ticks"]
        EnergyUsage["ENERGY_USAGE\nSets total FE cost"]
    end

    subgraph output["Output count resolution (implemented)"]
        BaseCount["Recipe base count"]
        HardnessGate["Recipe hardness pressure"]
        CycleTime["Cycle time uses recipe ticks and PROCESSING_SPEED"]
        FeCost["FE per craft uses recipe energy, ENERGY_USAGE,\nand under-hardness penalty"]
        ScaledCount["base count x effective OUTPUT_AMOUNT"]
        Guaranteed["Guaranteed output is floor(scaled count), minimum 1"]
        Fractional["Fractional remainder banks toward +1 output"]
        Crushed["Crushed iron, crushed copper, or crushed gold"]
    end

    Raw --> RawRecipe
    Ore --> OreRecipe
    Deepslate --> DeepslateRecipe
    RngtechOre --> RngtechRecipe

    RawRecipe --> BaseCount
    OreRecipe --> BaseCount
    DeepslateRecipe --> BaseCount
    RngtechRecipe --> BaseCount

    MachineAffixes --> OutputAmount
    CrushHead --> Hardness
    CrushHead --> OutputAmount
    CrushHead --> ProcessingSpeed
    BatteryCell --> OutputAmount
    MachineAffixes --> ProcessingSpeed
    MachineAffixes --> EnergyUsage

    Hardness --> HardnessGate
    BaseCount --> HardnessGate
    HardnessGate --> ScaledCount
    OutputAmount --> ScaledCount
    BaseCount --> ScaledCount --> Guaranteed --> Fractional --> Crushed
    ProcessingSpeed --> CycleTime
    EnergyUsage --> FeCost
```

## Read This As

- Component stages set base stat expectations, Refinement Potential expectations, and natural modifier tier weighting.
- Crusher duplication is recipe based: raw materials produce `2` crushed items and Silk-Touched ore blocks produce `3`. Alloy families have no RNGTech ore or raw forms.
- Source-ore hardness hard-gates world harvesting with Modular Picks and Hammers. Crusher `required_processing_level` is separate machine recipe pressure: an under-level Crush Head still runs the recipe with extra time, extra total FE, jam risk, and no bonus output. Under-level recipes suppress positive `OUTPUT_AMOUNT` above base and do not roll Super Output, Instant Process, or Crusher Salvage.
- `OUTPUT_AMOUNT` multiplies the recipe's base count, and fractional results fill the output-bonus bank. `SUPER_OUTPUT_CHANCE` adds one extra base output copy without multiplying or draining that bank.
- A Crusher without an installed Battery Cell applies a production penalty to `OUTPUT_AMOUNT`; the tiny internal buffer is an emergency working buffer, not the normal production path.
- `PROCESSING_SPEED` affects throughput without lowering the recipe's base FE cost. `ENERGY_USAGE` affects FE cost, not the base duplication ratio.
- Balance anchors for Stage 1-4 materials: smelting crushed material directly costs `600` ticks and `7,200 FE`; the Crusher-to-Crusher-to-Furnace dust chain costs `260` ticks and `6,000 FE`, trading a second processing step for lower cost. Stage 5-8 recipes use authored `2.0x`, `3.0x`, `4.0x`, and `6.0x` FE ramps before runtime high-heat rules.
- Default data defines manual crushed-to-dust recipes for iron, copper, tin, and gold, Crusher crushed-to-dust recipes for non-alloy crushed materials, source-ore Crusher recipes, and `rngtech:furnace` dust-to-ingot recipes for non-alloy staged materials. Alloy dusts are not part of the default public progression. Bronze has an early blend chain: three Copper Dust, one Tin Dust, and Coal Dust make two Bronze Blend; the Bronze Alloy Furnace makes three Bronze Blend from the same dust ratio plus Charcoal at `900` heat, and the direct-ingot recipe makes four Bronze Ingots from `3:1` Copper/Tin ingots plus Charcoal at `1100` heat.

## Related

- [Component Stages](../reference/component-stages.md)
- [Crafting and Upgrades](crafting.md)
- [Crusher](../content/crusher.md)
- [Machine Stats](../reference/machine-stats.md)
