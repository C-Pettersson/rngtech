# Wiki Authoring

Status: Implemented

The player wiki is a separate MkDocs site: pages live in `wiki/`, the config is `mkdocs.wiki.yml`, and the GitHub Pages workflow publishes it. These design docs are not published. Wiki pages can call templates that render infoboxes, crafting grids, recipe tables, and navigation from the mod's own resources. They play the same role as the [Minecraft Wiki](https://minecraft.wiki/w/Furnace) templates. The MkDocs hook in `tools/rngwiki/` expands them at build time, reading `src/main/resources` for recipes, tags, models, textures, and `en_us.json` names.

A template call is `{{` + a Python-style function call + `}}`. Arguments must be literals: strings, numbers, lists, or dicts. Templates are expanded in every wiki page, never inside code blocks.

## Page front matter

Every machine page declares its navbox category, icon, and the resource ids it documents:

```yaml
---
wiki:
  category: Processing
  icon: rngtech:furnace
  ids:
    - rngtech:furnace
    - rngtech:iron_furnace_chassis
---
```

`ids` drive the default arguments of `infobox`, `crafting`, `crafting_usage`, and `data_values`. Any slot or item link showing one of these ids links to the page. Vanilla items link to the Minecraft Wiki. Categories, in navbox order, are: Guides, Processing, Power generation, Energy storage, Fluids and gases, Refinement, Logistics, and Tools.

## Templates

| Template | Minecraft Wiki equivalent | Renders |
|---|---|---|
| `infobox(variants=None, fields=None, title=None, caption=None)` | `{{Infobox block}}` | Floating box with a render per variant, variant tabs, author fields, and an automatic Tool row from the `mineable` block tags. `variants` is a list of ids or `[label, id]` pairs, and defaults to the page `ids`. `fields` is an ordered dict, and values may contain other templates. |
| `crafting(*ids, notes=True)` | `{{Crafting}}` | Table of every crafting-table recipe that outputs the ids (default: page `ids`), with calibration, tool-damage, and material-condition notes. `notes=False` drops the Notes column. Whether the crafted item rolls traits depends on its item class, not the recipe, so state that in the page text. |
| `processing(type, input=None, output=None, columns=None, hide=None, limit=None)` | `{{Smelting}}` / usage tables | Table of every recipe of a machine recipe type, such as `"furnace"` or `"rngtech:crusher"`. Inputs, outputs, fluids, and scalar fields are detected automatically. `input`/`output` filter by item id. `columns` picks and orders scalar fields; `hide` drops them. Tables with more than 12 rows get a filter box. |
| `crafting_usage(*ids)` | `{{crafting usage}}` | Crafting recipes that use the ids as an ingredient. |
| `data_values(*ids)` | `{{ID table}}` | Name, resource location, block or item form, and translation key. |
| `item(id, text=None, link=True)` | `{{BlockLink}}` / `{{ItemLink}}` | Inline 16 px icon and name, linked to its wiki page. |
| `slot(id, count=1)` | `{{Slot}}` | One inventory slot. |
| `tag(tag, cycle=False)` | — | One slot per item in an item tag, e.g. a fuel tag. `cycle=True` shows a single slot that cycles through the members instead, for large tags. |
| `chain(*steps, edges=None, caption=None)` | — | Flow diagram of machines linked by the items and fluids they pass on, with every link checked against the recipes. See [Chains](#chains). |
| `navbox()` | `{{Navbox blocks}}` | Navigation box of all wiki pages by category. End every machine page with it. |
| `gallery()` | — | Icon card grid of all wiki pages; used on the main page. |

Hover a slot to see its name, fluid amount, tag, role, or requirement. A yellow dot marks a slot with a role or requirement in its tooltip, such as a catalyst, mold, or calibration gate. Tag ingredients cycle through their members every two seconds.

## Examples

```md
```


```md
```


## Chains

`chain()` draws a production chain top to bottom: machines as boxes, the resources they pass on as arrows. `wiki/power-chains.md` uses it.

```md
{{ chain("rngtech:iron_crusher_chassis", "rngtech:coal_dust", "rngtech:coal_gasifier", "rngtech:syngas", "rngtech:syngas_combustor") }}

{{ chain(edges=[
    [None, "minecraft:coal", "rngtech:crude_solid_fuel_burner"],
    ["rngtech:melter", "rngtech:ender_slurry", "rngtech:battery_assembler", {"from": "melter/ender_slurry"}],
    ["rngtech:vacuum_collapse_generator", "rngtech:collapse_residue", "rngtech:melter"],
]) }}
```

- Positional `steps` alternate machine, resource, machine, and so on, for a straight line. `edges` takes `[from, resource, to]` triples for branches and loops; both can be combined.
- A machine is any id listed on a machine's wiki page. Append `#2` to an id to draw a second, separate box for the same machine.
- `None` as `from` draws a dashed box for something the player supplies; `None` as `to` draws one for a product that leaves the chain.
- An edge that points back upstream is drawn as a dashed loop on the right.
- Every edge is checked: the `from` machine's recipe types must output the resource, and the `to` machine's recipe types must take it. A failed check is a build warning, so `mkdocs build --strict` fails and the diagram cannot drift from the recipes.
- Labels show the amount per run and the rate at base speed, from the recipe's amounts and `processing_ticks`. When a machine has several matching recipes, the one that matches most of that machine's other edges in the chain wins. A fourth element `{"from": recipe, "to": recipe}` pins either side to one recipe id.

`MACHINES` in `tools/rngwiki/chains.py` maps each machine to its recipe types. It also lists what a machine's code handles without recipe JSON: fuel tags (Solid Fuel Burner, Bio Generator, Wooden Composter), inputs (Electrolyte Solution in the Corrosion Cell), and outputs (water from the Wooden Dehumidifier, Composted Biomass). Add a machine there before using it in a chain, and update it when a machine's code starts taking or making something new.

The chain model in `chains.py` (nodes, edges, checked flows, and the layered layout) holds no HTML, so another exporter can draw the same chains.

## Icons

Icons are rendered by `tools/rngwiki/icons.py` from item and block models: block models render at the inventory angle, including element rotation, so shaped models such as the Cavitation Generator draw as they look in game, and `item/generated` models render flat. Vanilla icons, names, and tags come from the Minecraft 1.21.1 client jar. The hook finds it through the `RNGTECH_MC_CLIENT_JAR` environment variable, `build/wiki/`, or the NeoGradle cache. If it is missing, run:

```sh
python tools/rngwiki/vanilla.py --fetch
```

That downloads the jar from Mojang and checks its SHA-1 against Mojang's manifest. Without it, vanilla items render as `?` placeholders.

## In-game guide

Status: Implemented

The wiki also ships in the mod jar as an in-game guide for [GuideME](https://www.curseforge.com/minecraft/mc-mods/guideme), an optional dependency. Without GuideME the guide files are never read. `tools/rngwiki/guideme.py` renders every wiki page except the front page and `modpack-creators.md` into GuideME Markdown under `src/guide/resources`, which Gradle adds to the jar's resources. Those files are generated: never edit them, and commit them with the wiki change that produced them.

```sh
python tools/rngwiki/guideme.py
```

CI runs `python tools/rngwiki/guideme.py --check` and fails when the committed guide no longer matches the wiki. The exporter needs the client jar (see [Icons](#icons)), because vanilla names and tags would otherwise change its output.

The exporter reshapes pages the way the AE2 guide reads, so a wiki page needs no guide-specific markup:

- Machine pages open with a 3D render of the page icon and a row of their variants.
- Sections the game already shows are left out: full `processing()` tables (JEI), ascendancy trees (the Mastery tab), `data_values()`, and See also. Filtered `processing()` tables stay.
- A section heavier than about 1,000 characters of text, or a run of crafting grids, moves to a child page, and a page made of many mid-sized sections splits its heaviest ones until it fits. Long child pages split again.
- Each wiki category becomes an index page; the guide's front page lists them.
- `chain()` draws its machines layer by layer with a table of every flow, because GuideME cannot draw arrows.
- Wiki links follow sections to their child pages. Links to pages outside the guide become plain text.

A section that describes the web page itself, such as how to read the diagrams, gets guide wording from `_GUIDE_SECTIONS` in the exporter, keyed by page and heading id.

The client run reads the pages straight from `src/guide/resources`, so re-running the exporter updates an open guide. `./gradlew runClient -PguideStartup` opens the guide on the title screen and logs every page error, without loading a world.

## Content rules

The wiki is for players. It describes the game as it is today.

- Document implemented behavior only. Leave out anything Planned, Deferred, or Out of scope, design proposals, and PRD or state-file content. Don't promise future changes ("not yet", "will", "reserved for").
- Link only to other wiki pages. The wiki cannot link into `docs/`, because those pages are not published. Explain a concept on its wiki guide page (Rarity and Affixes, Machine Mastery, Machine Stats, Gear, Battery Cells, Stages) instead.
- Use player voice: no Java class names, recipe JSON fields, config keys, or internal status labels.
- The one exception is `wiki/modpack-creators.md`, written for pack makers. It may name config keys, tags, recipe fields, commands, and gamerules, and it may link to the pack-maker guides under `extras/` on GitHub. It still never names Java classes or links into `docs/`. When a config key, connector cap, energy recipe, or water rate changes, update that page too.
- When behavior changes in code, update the wiki page in the same change, and the Machine Design Notes in `docs/reference/machine-guidelines.md` if a design rule changed.

## Writing a page

Copy `docs/page-templates/wiki.md` into `wiki/`, add it to the nav in `mkdocs.wiki.yml`, and follow `wiki/furnace.md`. Preview with `mkdocs serve -f mkdocs.wiki.yml`, and check with `mkdocs build --strict -f mkdocs.wiki.yml`. Then regenerate the [in-game guide](#in-game-guide).
