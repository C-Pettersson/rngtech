# RNGTech

[![Build](https://github.com/C-Pettersson/rngtech/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/C-Pettersson/rngtech/actions/workflows/build.yml)

RNGTech is a Minecraft **1.21.1 / NeoForge** technology mod with staged machines, installable parts, and action-RPG item rolls inspired by **Path of Exile** and **Last Epoch**. Machines and parts can have rarity, affixes, and a limited Refinement Potential budget for further changes.

Build an ore-processing line, generate and store energy, then improve the machines and their Gear. The mod includes crushing, smelting, alloying, metal forming, recycling, calibration, fluid and gas processing, modular tools, and a rail-based Forestry Companion. Universal Cables transfer energy, fluids, and items; optional connectors bridge AE2 or Refined Storage networks.

## Influences

RNGTech brings action-RPG loot and build planning to a tech mod. The ideas come mainly from Path of Exile and Last Epoch, but they apply to machines instead of characters:

- **Rarity and affixes** (Path of Exile, Last Epoch). Machines, parts, and Battery Cells roll Normal, Magic, Rare, or Unique rarity, with tiered prefix and suffix modifiers. See [Rarity](docs/reference/rarity.md) and [Affix generation](docs/systems/affix-generation.md).
- **A limited crafting budget** (Last Epoch). Refinement Potential works like Forging Potential: each refinement spends a rolled amount, so every item can be improved only so far. See [Affixes and refinement](docs/systems/progression.md).
- **Single-purpose crafting currency** (Path of Exile). Crystals, catalysts, and coils each add, upgrade, reroll, or remove affixes, or promote rarity, much like orbs.
- **A shared passive tree** (Path of Exile). Machine Mastery gives each machine chassis its own points to spend on one large passive tree, with attribute roads, notables, and keystones that trade a cost for a strong payoff. See [Machine Mastery](docs/systems/machine-mastery.md).
- **Ascendancies** (Path of Exile). Each Mastery family has two ascendancies: small specialization trees that a machine unlocks with Ascendancy Seals.

The machines, progression, and resource chains remain grounded in Minecraft tech mods. RNGTech uses its own names, mechanics, and artwork, and is not affiliated with or endorsed by Grinding Gear Games or Eleventh Hour Games.

## Install and play

- Use Minecraft **1.21.1**, **NeoForge 21.1.228 or newer for 1.21.1**, and **Java 21**. Fabric and Forge are not supported loaders.
- Download the mod JAR from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/rngtech) or [GitHub Releases](https://github.com/c-pettersson/rngtech/releases), or build it from source. Put the JAR in the instance's `mods` directory on the client and server.
- JEI supplies recipe and Gear information; Jade supplies machine overlays. These integrations are optional. AE2 and Refined Storage are needed only for their respective network bridges.
- FTB Quests is optional. Pack authors can install the [quest extra](extras/ftbquests/README.md); the mod JAR does not install a quest book.

See [Getting Started](docs/getting-started.md) for the first tools and machines, and [Current Implementation](docs/reference/current-implementation.md) for feature status. Features marked **Prototype** exist but may change; **Planned** pages describe future work. Back up worlds before updating. The project has automated source/data checks, but no Java unit-test suite or exhaustive gameplay test suite.

## Build from source

Install Java 21 and use the checked-in Gradle wrapper:

```sh
./gradlew build
```

The mod JAR appears at `build/libs/rngtech-<version>.jar`. Use `.\gradlew.bat` on Windows. Gradle downloads the development dependencies on the first run. The development client includes optional mods for integration testing; these are not required dependencies of the published mod.

For contributions and verification commands, see [CONTRIBUTING.md](CONTRIBUTING.md). For release tooling and the manual gameplay checklist, see [Releasing](docs/releasing.md).

## Documentation and tools

- [Documentation index](docs/index.md)
- [Machine and part stages](docs/reference/component-stages.md)
- [Affixes and refinement](docs/systems/progression.md)
- [ModDex source/data explorer](tools/moddex/README.md)
- [Security reporting](SECURITY.md)
- [Support and bug reports](SUPPORT.md)

To build the documentation, install `requirements-docs.txt` and run `mkdocs build --strict`. Run `mkdocs serve` for a local preview, or `docker compose -f compose.docs.yml up` and open <http://localhost:8000>.

## License

RNGTech uses the [MIT License](LICENSE). Minecraft, NeoForge, and optional mod dependencies retain their own licenses; they are not included in this grant.
