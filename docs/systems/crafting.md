# Crafting and Upgrades

Status: Planned

RNGTech treats machines like action RPG characters. A machine is the character. Components are its gear. Refinement changes the quality of that gear, and upgrades let strong components stay useful beyond the stage where the player first made them.

## Design Goals

- A player can craft a good result for the current stage and use it through one or two later stages.
- A player can hit a rare upgrade outcome that pushes a component above the expected result, such as Heat Rating 1 becoming Heat Rating 2.
- A player should not feel punished for investing in a strong component.
- Bad or outdated components should feed the recycling economy instead of becoming dead items.
- Pack makers can gate progression through machine stats, such as a heat core needing enough temperature to melt the next ingot stage.

## Crafting Layers

RNGTech crafting has four layers.

| Layer | Role | Example |
|---|---|---|
| Assembly | Creates the base object. | Craft a Crusher, Heat Core, or Crush Head. |
| Rolling | Assigns rarity, Refinement Potential, base ranges, and legal modifiers. | A crafted Heat Core rolls high `MAX_TEMPERATURE`. |
| Refinement | Mutates a modifier-bearing stack or placed machine. | Use an Affix Injector or Nullifier Coil in the Affix Forge. |
| Recycling | Converts bad, obsolete, or failed items into useful material. | Break down an old component into scrap and catalyst inputs. |

Assembly decides what the item is. Rolling and refinement decide how good that copy is.

Component material stages are based on ore and ingot families. Non-stage reagents can appear in recipes, but they do not set component stage.

Machine Frame tiers are shared recipe ingredients, not RPG trait owners. `rngtech:machine_frame` covers Stage 0-3 machine bodies, early support recipes, and the Steel Metal Press body; `rngtech:reinforced_machine_frame` gates most other Stage 4 Steel and Lead bodies, `rngtech:advanced_machine_frame` covers Stage 5-6, and `rngtech:exotic_machine_frame` covers Stage 7-8 with a Lubricant Bucket fluid gate. The frame tier helps structure assembly recipes; placed machine identity still comes from the machine or chassis item that is crafted.

Current prototype rolling already treats starting affixes and Refinement Potential as separate outcomes. Rarity determines how many starting affixes can appear, while component stage determines the RP range and natural modifier tier weighting.

## Machine as Character

A machine build is made from several long-lived pieces.

| Piece | RPG equivalent | Notes |
|---|---|---|
| Machine chassis | Character class | Defines behavior, screen, slots, and machine type. |
| Component | Gear item | Holds stats and modifiers that contribute to the machine. |
| Modifier | Item affix | Changes stats or behavior. |
| Rarity | Item rarity | Controls affix count and naming rules. |
| Refinement Potential | Crafting budget | Limits how much more the item can be changed. |
| Strain | Crafting wear | Raises future risk or cost after rough upgrades. |

The player should care about the specific component they made, not only the block it sits inside.

The current prototype has registered [Machine Chassis](https://c-pettersson.github.io/rngtech/stages/#machine-frames) subtype items and Gear tabs on placed Crusher and Furnace machines. The Stage 0 Furnace recipe still crafts `rngtech:furnace` directly, while electric Furnace chassis use staged placed blocks. Current item-only chassis items do not carry RPG traits, so no chassis item data is preserved into a placed machine unless the result itself is a rollable machine block item.

The solid fuel-burning generator subtype exists as a chassis/category target only. It is not a placed machine block.

## Component Stages

Each component has a material stage. That stage controls base stat expectations, Refinement Potential budget, and the natural modifier tier weighting used by rolls.

| Component Stage | Example Components | Natural Tier Target |
|---|---|---:|
| Primitive Stage | Wooden Crusher Chassis, Flint Crush Head | 1 if rollable |
| Iron Stage | Iron Crush Head, Iron Heat Core | 1 |
| Copper Stage | Copper Coil, Copper Heat Core | 1 |
| Steel Stage | Steel Crush Head, Steel Heat Core | 2 |
| Titanium Stage | Titanium Heat Core, Arclite Coil | 3 |
| Tungstensteel Stage | Tungstensteel Crush Head, Nullite Cell | 4 |

A great Iron Stage component can carry the player into Copper Stage or early Steel Stage content, but tier 4 affixes remain rare and expensive at low stage. This keeps lucky early rolls valuable without letting one item solve the whole pack.

The canonical material and component ladder is documented in [Component Stages](../reference/component-stages.md).

The advanced material checklist and recipe-form vocabulary are documented in [Materials List](../reference/materials.md).

## Upgrade Paths

Upgrade recipes raise a component into a higher material stage.

Example:

```text
Iron Crush Head + Steel Alloy + Grinding Compound -> Steel Crush Head
```

An upgrade can:

- Raise the component stage.
- Reroll or improve base stat ranges into the new stage.
- Preserve compatible modifiers.
- Preserve rarity when the recipe allows it.
- Add strain.
- Remove or downgrade a rolled modifier on bad outcomes.

An upgrade should not:

- Add new Refinement Potential.
- Upgrade existing modifier tiers for free.
- Remove implicit identity rules unless the target component replaces them.
- Destroy the component on ordinary failure.

The core rule is: upgrading preserves investment, but does not renew the item's crafting life.

## Refinement Potential Across Upgrades

Upgraded components keep their remaining Refinement Potential. They do not gain a fresh budget from the new stage.

For newly generated stacks, starting affixes do not spend Refinement Potential. RP is only reduced by later player-driven refinement operations.

Example:

| Step | Result |
|---|---|
| Player crafts an Iron Heat Core with `12` Refinement Potential. | Strong Iron Stage crafting base. |
| Player spends `9` potential improving it. | `3` potential remains. |
| Player upgrades it into a Steel Heat Core. | It still has `3` potential. |

This lets good components move forward while preserving a real cost for heavy early investment.

## Upgrade Outcomes

Upgrade recipes use weighted outcomes instead of a single pass or fail result.

| Outcome | Default Role | Result |
|---|---|---|
| Failed | Risk floor | No stage increase. Some materials may be consumed. The component survives. |
| Fractured | Bad success | Stage increases, but one rolled modifier is removed or downgraded. |
| Strained | Normal rough success | Stage increases, modifiers are preserved, and strain increases. |
| Clean | Good success | Stage increases and modifiers are preserved. |
| Breakthrough | Rare lucky success | Stage or family rating increases beyond the recipe target. |

Example weights for a normal upgrade:

| Outcome | Example Chance |
|---|---:|
| Failed | 10% |
| Fractured | 15% |
| Strained | 35% |
| Clean | 39% |
| Breakthrough | 1% |

These numbers are design targets, not final balance. Configs or datapack rules should let pack makers make upgrades harsher or safer.

## Breakthrough Upgrades

Breakthrough is the rare lucky upgrade outcome.

Example:

```text
Copper Heat Core, Heat Rating 1 -> expected Steel Heat Core, Heat Rating 1
Breakthrough -> Steel Heat Core, Heat Rating 2
```

Breakthrough rules:

- It can only happen during component stage-up crafting.
- It can raise the component one stage above the recipe target.
- It can raise a component family rating, such as Heat Rating 1 to Heat Rating 2.
- It grants no new Refinement Potential.
- It does not upgrade existing modifier tiers.
- It cannot exceed the configured maximum stage or rating.
- It can be disabled or reweighted for hardcore packs.

Breakthrough gives the player a memorable hit without removing future progression. The component still needs enough stats, potential, and modifier tiers to keep scaling.

## Strain

Status: Planned

Strain tracks damage from rough upgrades.

Proposed behavior:

- Strain increases future upgrade cost or failure chance.
- Strain can raise the chance of Fractured outcomes.
- Strain can make refinement consume more Refinement Potential.
- Special maintenance recipes can reduce strain at a material cost.

Strain gives upgrades a long-term price without deleting invested components.

## Stat Gates for Packs

Recipes should gate progression by stats instead of item names whenever possible.

Example furnace gates:

| Recipe Stage | Example Requirement |
|---|---|
| Bronze | `MAX_TEMPERATURE >= 750` |
| Steel | `MAX_TEMPERATURE >= 1100`, `PROCESSING_LEVEL >= 2` |
| Titanium | `MAX_TEMPERATURE >= 1600`, `PROCESSING_LEVEL >= 3` |

Example data shape:

```json
{
  "requirements": {
    "machine_type": "rngtech:furnace",
    "min_stats": {
      "rngtech:max_temperature": 1100,
      "rngtech:processing_level": 2,
      "rngtech:temperature_stability": 0.95
    }
  }
}
```

A lucky lower-stage component can reach forward into the next stage. Natural tier weighting, no free Refinement Potential, and upgrade risk stop that component from carrying the whole playthrough.

## Unique Components

Status: Prototype

Unique components are build-defining items. Each has authored, ranged stat lines, one signature mechanic, and one visible drawback, and each copy rolls its own values when identified. The [Unique Items PRD](../prds/uniques.md) defines the catalog and the launch set, and the [Unique Item Ideas](../reference/unique-item-ideas.md) page holds the backlog.

Unique components are drop or find only. They should not replace the normal stage system; they should create a different build choice.

Pack makers control where Uniques come from without code:

- The `uniques.loot.enabled` common config key turns off every default Unique loot table, and `uniques.loot.<id>.enabled` turns off one Unique's table. These keys gate only the default tables.
- The six `rngtech:challenges/*` loot tables roll when machine events happen, such as a Crusher jam, a heat failure, or a Mastery level. They ship empty; a datapack can fill them with Uniques or anything else, using the context conditions listed in the [Current Implementation Matrix](../reference/current-implementation.md#challenge-loot-tables).
- A plain Unique stack, such as an FTB Quests item reward, counts as unidentified, so each player rolls their own copy. `/rngtech unique give <players> <id>` gives one the same way.
- Any default loot table or loot modifier can be replaced or extended by datapack. Use the `rngtech:unidentified_unique` loot function on Unique entries so they drop unidentified with a fixed roll.
- Packs can add their own Uniques as `config/rngtech/uniques/*.json`, with names and textures from a resource pack and loot from a datapack.

The pack-maker guide, `extras/uniques/README.md`, covers all of this with examples.

## Recycling Principles

Status: Planned

Recycling should make unwanted rolls useful.

Rules:

- Recycling returns value based on component stage, rarity, modifier count, and modifier tier.
- Recycling can return machine scrap, metal fragments, spent catalysts, or stage-specific fragments.
- Failed upgrades may return partial materials.
- Bad rare items should recycle into better material than bad normal items.
- Unique components are never recycled.

Recycling supports experimentation. A player can chase a better part without filling storage with failed attempts.

## Related

- [Progression](https://c-pettersson.github.io/rngtech/rarity-and-affixes/)
- [Affix Generation](affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
- [Machine Parts](https://c-pettersson.github.io/rngtech/gear/)
- [Materials List](../reference/materials.md)
- [Component Stages](../reference/component-stages.md)
- [Machine Stats](../reference/machine-stats.md)
- [Rarity](https://c-pettersson.github.io/rngtech/rarity-and-affixes/)
