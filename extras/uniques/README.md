# RNGTech Uniques for Pack Makers

Uniques are find-only Gear with ranged stat lines, a signature, and a drawback. Players see them on the wiki's Uniques page. This guide covers what a modpack can change without code: where Uniques come from, and new Uniques of your own.

## Moving where Uniques come from

### Turn off default loot

Every built-in Unique drops from one vanilla source through its own loot table, `rngtech:uniques/<id>`. Two keys in `config/rngtech-common.toml` gate those tables:

```toml
[uniques.loot]
    # Every default Unique loot table.
    enabled = true

    [uniques.loot.fortress_heater_element]
        # Only this Unique's default table.
        enabled = true
```

The keys gate only the default tables. Uniques you add to other loot, quests, or commands are unaffected.

### Challenge loot

RNGTech rolls a loot table of type `rngtech:challenge` when a machine event happens. All six ship empty, so default play is unchanged.

| Table | Rolled when | Results go to |
|---|---|---|
| `rngtech:challenges/calibration` | A Resonance Calibrator calibration completes | Output slot, else dropped on top |
| `rngtech:challenges/crusher_jam` | A Crusher jams | Output slot, else dropped on top |
| `rngtech:challenges/heat_failure` | A Furnace, Alloy Furnace, or Metal Press cycle fails | Output slot, else dropped on top |
| `rngtech:challenges/vacuum_collapse` | A Vacuum Collapse Generator cycle completes | Residue slot, else dropped on top |
| `rngtech:challenges/forestry_harvest` | A Forestry Companion harvests a tree or crop | The cart's cargo |
| `rngtech:challenges/mastery_level` | A machine or cart gains a Mastery level | Dropped on top, or at the cart |

Conditions that read the event:

| Condition | Field | Value |
|---|---|---|
| `rngtech:machine_family` | `family` | `crusher`, `furnace`, `alloy_furnace`, `metal_press`, `melter`, `resonance_calibrator`, `forestry`, or `vacuum_collapse_generator` |
| `rngtech:ascendancy` | `ascendancy` | An ascendancy id, such as `rockbreaker` |
| `rngtech:machine_stage` | `min`, `max` | Chassis stage, or the cutting tool head's stage for a Forestry harvest |
| `rngtech:recipe_stage` | `min`, `max` | The recipe's XP band; the Alloy Furnace's minimum component stage; the calibration minimum stage; a jam's recipe Processing Level |
| `rngtech:calibration_stability` | `min`, `max` | The calibrated result's stability |
| `rngtech:instability` | `min`, `max` | Vacuum Collapse instability times 100 |
| `rngtech:mastery_level` | `min`, `max` | The machine's Mastery level |

`min` and `max` are inclusive and optional. A condition never passes outside a challenge roll. This datapack file, `data/rngtech/loot_table/challenges/heat_failure.json`, gives a 2% chance of a Fortress Heater Element on a Stage 4+ heat failure:

```json
{
    "type": "rngtech:challenge",
    "pools": [
        {
            "rolls": 1,
            "conditions": [
                { "condition": "rngtech:machine_stage", "min": 4 },
                { "condition": "minecraft:random_chance", "chance": 0.02 }
            ],
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "rngtech:fortress_heater_element",
                    "functions": [{ "function": "rngtech:unidentified_unique" }]
                }
            ]
        }
    ]
}
```

The tables can hold anything, not only Uniques. You own the balance and loop safety of what you add.

### Quests and commands

A plain Unique item, such as an FTB Quests item reward, counts as unidentified, so each player rolls their own copy when they identify it. `/rngtech unique give <players> <id>` gives an unidentified copy and needs permission level 2.

### Your own loot

Replace or extend any loot table or loot modifier with an ordinary datapack. Put `rngtech:unidentified_unique` on Unique entries: it fixes the copy's roll when it drops, so players cannot reroll it by identifying elsewhere. `rngtech:unique_loot_enabled`, with an optional `unique` field, checks the config keys above if you want your tables to follow them.

## Adding your own Uniques

A pack Unique is a JSON file in `config/rngtech/uniques/`. RNGTech reads every `*.json` there at startup, after its own catalog, and registers each one as an item. **The server and every client need the same files**, or NeoForge refuses the connection over mismatched items.

A file that breaks a rule is skipped, and the game log says why with `Skipped pack Unique`. The game still starts.

### The file

`config/rngtech/uniques/mypack_ember_core.json`:

```json
{
    "version": 1,
    "id": "rngtech:mypack_ember_core",
    "host": "heat_core",
    "slot_stage": 3,
    "base_profile": "rngtech:bronze_heat_core",
    "stats": {
        "rngtech:warmup_time": { "operation": "more", "min": -0.3, "max": -0.5, "role": "signature" },
        "rngtech:overdrive_margin": { "operation": "flat", "min": 10, "max": 20, "role": "hook", "ascendancy": "crucible_keeper" },
        "rngtech:energy_usage": { "operation": "increased", "min": 0.3, "max": 0.15, "role": "drawback" }
    },
    "behaviors": [],
    "source": "rngtech:unique_source.mypack_volcano"
}
```

| Field | Meaning |
|---|---|
| `version` | Catalog format version. This RNGTech reads `1`. Optional; a newer number is refused. |
| `id` | The item id. Pack Uniques register under `rngtech`, so use a bare id or `rngtech:<id>`. Prefix it with your pack's name so a later RNGTech item cannot take it. An id an RNGTech item already uses is skipped. |
| `host` | The Gear type: `crush_head`, `heat_core`, `alloy_crucible`, `servo`, `fluid_pump`, or `control_board`. Battery Cells cannot be pack Uniques. |
| `slot_stage` | The stage it counts as for every Gear stage rule. It must lie in the host's range below. |
| `base_profile` | Optional. A normal part whose base stats it starts from, such as `rngtech:bronze_heat_core`. Without one, list every stat the part should have. |
| `stats` | The stat lines. The key is a stat id, such as `rngtech:warmup_time`. |
| `behaviors` | Optional behavior ids, such as `rngtech:power_grace`. |
| `source` | A language key for the "Found in" tooltip line. `rngtech:unique_source.x` becomes `rngtech.unique_source.x`. |

Each stat line has:

- `operation`: `flat` adds the value; `increased` adds a fraction to the increased bucket (`0.3` is 30% increased); `more` multiplies by one plus the value (`-0.5` is 50% less).
- Either `value` for a fixed line, or `min` and `max` for a ranged one. `min` is the worst roll and `max` the best, so a drawback range runs downward. Flat lines with whole-number bounds roll whole numbers.
- `role`: `identity`, `signature`, `hook`, or `drawback`. A `hook` also names its `ascendancy`, and only applies on a machine that chose it.

### Rules

The catalog refuses a file that:

- uses an unknown host, stat, behavior, or ascendancy, or a slot stage outside the host's range;
- has a stat or behavior that no machine taking that host reads;
- has a `signature` or `hook` that is not a benefit at its worst roll, or a `drawback` that is not a penalty at its best roll;
- puts a non-whole bound on an integer stat, such as Batch Size;
- reaches more than one stage past its slot stage at its best roll on a recipe gate: Crush Head Processing Level, Heat Core Maximum Temperature, or Alloy Crucible input slots;
- raises yield, such as Output Amount, Super Output, salvage, Fluid Yield, or Ledger Rate. Lowering yield is allowed;
- hooks an ascendancy whose machine does not read the stat.

| Host | Stages | Base profiles | Machines |
|---|---|---|---|
| `crush_head` | 0–8 | flint, iron, copper, bronze, steel, aluminum, titanium, tungstensteel, exotic | Crusher, Melter, Miner's Companion |
| `heat_core` | 1–8 | iron, copper, bronze, steel, aluminum, sparksteel, titanium, exotic | Furnace, Alloy Furnace, Metal Press, Melter, Solid Fuel Burner, Cavitation Generator, gas chemistry machines |
| `alloy_crucible` | 3–6 | bronze, steel, titanium | Alloy Furnace |
| `servo` | 4–8 | steel, titanium, tungstensteel, nullite, exotic | Metal Press, Alloy Furnace, Melter, Cavitation Generator, gas chemistry machines, Compressor Tank |
| `fluid_pump` | 5–8 | osmium, titanium, tungstensteel, exotic | Melter, Corrosion Cell, Forestry Companion |
| `control_board` | 1–7 | iron, copper, steel, titanium, tungstensteel, nullite | Resonance Calibrator |

Base profiles are written as `rngtech:<material>_<host>`, such as `rngtech:titanium_servo`.

Behaviors a Unique can carry: `power_grace`, `side_fluid_output`, `auto_purge`, `escapement` (with the `escapement_speed` stat), `reflux`, and `echo_streak`. Ascendancies: `rockbreaker` and `assayer` (Crusher), `crucible_keeper` and `bloomer` (Furnace), `blendwright` and `metallurgist` (Alloy Furnace), `die_keeper` and `drop_forge` (Metal Press), `pressure_vessel` and `twin_crucible` (Melter), `harmonist` and `mass_tuner` (Resonance Calibrator), and `field_hand`, `grove_warden`, and `timber_baron` (Forestry Companion).

A Unique built on a base profile changes when RNGTech retunes that part, so retest pack Uniques after updates.

### Name, texture, and model

Ship these in a resource pack. Without them the item shows its raw name and the missing-texture pattern.

`assets/rngtech/lang/en_us.json`:

```json
{
    "item.rngtech.mypack_ember_core": "Ember Core",
    "rngtech.unique.mypack_ember_core.description": "A core that wakes up hot.",
    "rngtech.unique_source.mypack_volcano": "Volcano vaults"
}
```

`assets/rngtech/models/item/mypack_ember_core.json`, with the texture at `assets/rngtech/textures/item/mypack_ember_core.png`:

```json
{
    "parent": "minecraft:item/generated",
    "textures": { "layer0": "rngtech:item/mypack_ember_core" }
}
```

### Loot

Pack Uniques have no default loot. Add them to challenge tables, quests, or your own loot tables as above, always with `rngtech:unidentified_unique`. A new pack Unique also gets its own `uniques.loot.<id>.enabled` config key, which your tables can check through `rngtech:unique_loot_enabled`. This datapack loot modifier, `data/mypack/loot_modifiers/ember_core.json`, adds a table to Nether fortress chests:

```json
{
    "type": "neoforge:add_table",
    "conditions": [{ "condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/nether_bridge" }],
    "table": "mypack:uniques/ember_core"
}
```

List it in `data/neoforge/loot_modifiers/global_loot_modifiers.json` with `"replace": false`, and write the table at `data/mypack/loot_table/uniques/ember_core.json` like the challenge example, with `"type": "minecraft:chest"`.
