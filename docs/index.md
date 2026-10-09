# RNGTech Documentation

RNGTech is a Minecraft 1.21.1 NeoForge technology mod with staged machines, installable Gear, rarity, affixes, and refinement.

## Start here

- [Player wiki](https://c-pettersson.github.io/rngtech/): how to play, every machine, and the player guides. Player-facing behavior lives only there, and shipped implementation detail lives only in the code; these docs keep design intent, system design, and proposals.
- [Wiki Authoring](reference/wiki-authoring.md): how the published player wiki in `wiki/` is built and what belongs in it.
- [Current Implementation](reference/current-implementation.md): which features exist, their automation surfaces, and refinement support.
- [Component Stages](reference/component-stages.md): the material progression ladder.
- [Machine Guidelines](reference/machine-guidelines.md): machine rules, balance, and per-machine design notes.
- [Machine Mastery](systems/machine-mastery.md) and [Affix Generation](systems/affix-generation.md): system design.

## Read feature status

**Prototype** features exist but may change. **Planned** features are design proposals. A release version does not mean that every design page describes shipped gameplay. The [current implementation reference](reference/current-implementation.md) records status; the wiki explains behavior.

Shipped behavior is documented on the [player wiki](https://c-pettersson.github.io/rngtech/); design intent and balance rules are in [Machine Guidelines](reference/machine-guidelines.md). [Design proposals](design-proposals.md) describe requirements beyond the implemented feature set.

## Maintain the project

Use [documentation conventions](conventions.md), [machine guidelines](reference/machine-guidelines.md), and [release verification](releasing.md). Define each concept in one place and link to it from related pages.
