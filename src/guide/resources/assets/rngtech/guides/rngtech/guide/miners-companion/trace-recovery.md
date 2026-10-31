---
navigation:
  title: "Trace recovery"
  parent: miners-companion.md
  position: 4
  icon: rngtech:miners_companion
---

# Trace recovery

With a Recovery Filter installed, each chewed item gets a small chance, **1 in 5,000** before the filter's Efficiency, to give back an item from a recovery recipe. A recovery recipe can also require a minimum Recovery Filter stage and a minimum Crush Head processing level. A recovered item goes into your inventory, or drops at the broken block if your inventory is full.

RNGTech recovers one raw metal at a time from chewed rock. Each rock has its own list of metals:

- **Stone**: cobblestone, mossy cobblestone, stone, andesite, diorite, and granite.
- **Deepslate**: cobbled deepslate, deepslate, and tuff.
- **Nether**: netherrack, basalt, and blackstone.
- **Sediment**: gravel and sand.

A metal needs a Recovery Filter and a Crush Head of its stage. For example, Raw Tin is a Stage 3 metal. It needs a Bronze Filter and a head with processing level 3 or more. The Steel Filter is the highest filter, so Stage 5 to 8 metals need only a higher Crush Head. When more than one metal is possible, the weight sets how often each one comes back. Dirt, end stone, and other blocks give nothing.

Modpacks and datapacks can add more recovery recipes, or add blocks to the rock tags. With JEI installed, its **Miner's Companion Recovery** category lists the recovery recipes that are loaded, with the chewed input, the result, the chance, and the filter and head requirements.
