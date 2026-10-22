# Balance World Datapack

`build.py` writes the `rngbalance` datapack: one function per balance-test setup, each a running machine chain wired with pre-configured Universal Connectors. Infinite Debug Chests and Debug Tanks supply the inputs and Debug Trashcans take the overflow. Debug blocks only work in dev runs (`runClient`, `runServer`).

```sh
python tools/balance-world/build.py --install "run/client/saves/<world>"
```

Then `/reload` in game and `/function rngbalance:help`.

## Using it

Each setup builds from your feet toward east (+X): a cable trunk two blocks ahead, machines on its north side, and a sign at the west end. Wooden Dehumidifiers are the full-size hollow 3×3×3 frame cube with the controller in the middle of the side facing the trunk. A setup refuses to build unless its footprint is empty air, and its function header lists the footprint.

| Function | What it does |
|---|---|
| `rngbalance:<setup>` | Builds the setup if the footprint is empty. |
| `rngbalance:force/<setup>` | Clears the footprint, then builds. |
| `rngbalance:rebuild/<setup>` | Clears and rebuilds the setup where it was last built, with fresh machines and Mastery. Run it after a balance change. |
| `rngbalance:rebuild_all` | Rebuilds every setup that has been built. |
| `rngbalance:world_rules` | Fixed noon, clear weather, no mob spawns, for steady solar, algae, and dehumidifier rates. |

Setups: the power chains from the [Power Chains](../../wiki/power-chains.md) guide (`coal`, `syngas`, `carbon_loop`, `ammonia`, `void_catalyst`, `water`, `side_power`), `nitrogen_atmosphere` (Blast Draft heated machines with and without a Nitrogen feed, from the [Heat](../../wiki/heat.md#nitrogen-atmosphere) guide), `hydrogen_chemistry` (water-gas shift, a Hydrogen-fed Fuel Cell, Urea, and Hydrogen Quench and Bright Anneal presses), `phase_array` (a nine-Coil [Phase Array](../../wiki/phase-array.md) with every position inscribed, a Debug Battery and charged cells on the Cradle, and Debug Chests of Glyphs and Stage 8 parts; identify a part from the chest in a crafting grid before firing at it), and one Crusher-to-Furnace ore line per stage (`ore_s1` to `ore_s8`), read from [`processing-setups.json`](../balance/processing-setups.json).

Generator chains end in an Exotic Battery Chassis whose first two cells start charged; watch its total to see the chain's net FE. Chains without a generator run on a Debug Battery. Read machine rates from Jade or each machine's screen.

## Keeping it accurate

The layouts use the machines' saved NBT (inventory sizes, Gear slots, tank keys) and their sided item handlers: input on top, output on the bottom. When a block entity's save format or Gear slots change, rebuild the pack, spawn the affected setup, and check that it runs.
