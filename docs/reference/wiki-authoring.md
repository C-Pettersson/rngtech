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
| `navbox()` | `{{Navbox blocks}}` | Navigation box of all wiki pages by category. End every machine page with it. |
| `gallery()` | — | Icon card grid of all wiki pages; used on the main page. |

Hover a slot to see its name, fluid amount, tag, role, or requirement. A yellow dot marks a slot with a role or requirement in its tooltip, such as a catalyst, mold, or calibration gate. Tag ingredients cycle through their members every two seconds.

## Examples

```md
```


```md
```


## Icons

Icons are rendered by `tools/rngwiki/icons.py` from item and block models: block models render as inventory-angle cubes, and `item/generated` models render flat. Vanilla icons, names, and tags come from the Minecraft 1.21.1 client jar. The hook finds it through the `RNGTECH_MC_CLIENT_JAR` environment variable, `build/wiki/`, or the NeoGradle cache. If it is missing, run:

```sh
python tools/rngwiki/vanilla.py --fetch
```

That downloads the jar from Mojang and checks its SHA-1 against Mojang's manifest. Without it, vanilla items render as `?` placeholders.

## Content rules

The wiki is for players. It describes the game as it is today.

- Document implemented behavior only. Leave out anything Planned, Deferred, or Out of scope, design proposals, and PRD or state-file content. Don't promise future changes ("not yet", "will", "reserved for").
- Link only to other wiki pages. The wiki cannot link into `docs/`, because those pages are not published. Explain a concept on its wiki guide page (Rarity and Affixes, Machine Mastery, Machine Stats, Gear, Battery Cells, Stages) instead.
- Use player voice: no Java class names, recipe JSON fields, config keys, or internal status labels.
- When behavior changes in code, update the wiki page in the same change as the matching `docs/content/` page.

## Writing a page

Copy `docs/page-templates/wiki.md` into `wiki/`, add it to the nav in `mkdocs.wiki.yml`, and follow `wiki/furnace.md`. Preview with `mkdocs serve -f mkdocs.wiki.yml`, and check with `mkdocs build --strict -f mkdocs.wiki.yml`.
