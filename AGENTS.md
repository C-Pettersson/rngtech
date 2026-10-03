# AGENTS.md

Guidance for agents working in this repository.

## Project Overview

RNGTech targets Minecraft 1.21.1, NeoForge, and Java 21. It combines staged machines, installable Gear, rarity, affixes, and refinement.

Use [Current Implementation Matrix](docs/reference/current-implementation.md) for the code-backed feature inventory. Check the registries and recipes before changing a feature's status. Keep the feature inventory in that reference rather than duplicating it here.

Machine implementation and balance work must follow [Machine Guidelines](docs/reference/machine-guidelines.md). Universal Cables already transfer energy, fluids, and items and bridge optional AE2/Refined Storage networks. A shared storage system or storage-network replacement remains out of scope.

## Repository Layout

- `src/main/java/com/rngtech/` contains the mod source.
    - `content/registry/` contains deferred registers for blocks, items, block entities, menus, capabilities, and creative tabs.
    - `content/block/`, `content/blockentity/`, `content/menu/`, and `client/screen/` contain machine behavior and UI.
    - `rpg/` contains rarity, modifier, stat, and machine-part domain types.
    - `util/` contains shared implementation helpers.
- `src/main/resources/` contains assets, recipes, loot tables, language entries, and `META-INF/neoforge.mods.toml`.
- `docs/` contains MkDocs design and reference documentation.
- `mkdocs.yml` configures the documentation site.
- `build.gradle`, `settings.gradle`, and `gradle.properties` define the Gradle/NeoForge setup.

## Requirements

- Java 21.
- Node.js 22.22.2 or newer in the 22.x line for Node tooling; see `package.json` for other supported versions.
- Use the checked-in Gradle wrapper.
- For documentation work, install Python docs requirements from `requirements-docs.txt`.

On Windows, prefer `.\gradlew.bat`. On Unix-like shells, use `./gradlew`.

## Common Commands

Run fast local verification:

```sh
./gradlew quickCheck
```

Run full CI-equivalent verification:

```sh
./gradlew ciCheck
```

Build the mod:

```sh
./gradlew build
```

Apply formatting:

```sh
./gradlew spotlessApply
```

Launch the client:

```sh
./gradlew runClient
```

In a linked git worktree, every run uses the main checkout's `run/` directory, so worktrees share its settings and worlds.

Generate data resources:

```sh
./gradlew runData
```

Preview docs locally:

```sh
mkdocs serve
```

Build docs:

```sh
mkdocs build --strict
```

## Verification Expectations

- Run `quickCheck` after Java or resource changes when feasible.
- Run `ciCheck` before larger handoffs or changes that affect build configuration, resources, or shared behavior.
- `check` depends on `spotlessCheck`, so formatting failures are build failures.
- Domain checks live in `src/masteryTest` and run through `masteryCheck`, included in `quickCheck` and `ciCheck`. Run `npm run moddex:check` for the source/data smoke checks and passive-tree validation. Run `npm run repo:check` and `mkdocs build --strict` for public repository and documentation changes. Gameplay still needs focused in-game checks.

## Formatting and Style

- Follow `.editorconfig`: UTF-8, LF line endings for most files, final newline, four-space indentation for Java, Gradle, JSON, TOML, Markdown, YAML, and properties files.
- Spotless manages Java imports, unused imports, trailing whitespace, and final newlines.
- Java import order is `com.rngtech`, blank, `javax`, then `java`.
- Keep code in the existing package structure. Add new registry entries through the appropriate `Mod*` registry class.
- Keep comments sparse and useful. Prefer clear names and small methods over explanatory comments.
- Avoid unrelated refactors while implementing feature work.

## NeoForge and Gameplay Guidelines

- Follow `docs/reference/machine-guidelines.md` for machine balance, tabs, stats, energy behavior, output amount handling, Gear slots, automation surfaces, and low-text graphical machine UI with hover details.
- Target Minecraft `1.21.1`, NeoForge `21.1.228`, and Java 21 unless the project version properties are intentionally updated.
- Use `RNGTech.MOD_ID` for the mod id in code.
- Register blocks, items, block entities, menus, capabilities, and creative tabs through the existing deferred-register pattern.
- Keep runtime-only optional mod dependencies on `localRuntime`, not `runtimeOnly`, so they are not published as hard dependencies.
- Generated data belongs under `src/generated/resources`; hand-authored resources belong under `src/main/resources`.
- Do not add a shared item-storage system or storage-network replacement unless scope changes. Preserve existing connector item/fluid/energy transfer.

## Documentation Guidelines

- Documentation lives in `docs/` and should stay consistent with `mkdocs.yml` navigation.
- Follow `docs/conventions.md`.
- Use status labels consistently: `Implemented`, `Prototype`, `Planned`, `Deferred`, and `Out of scope`.
- Define each game concept once, then link to that canonical definition from other pages.
- Do not promote example-only concepts into canonical docs until they become real RNGTech design.
- Use `docs/page-templates/modifier.md` for modifier pages and `docs/page-templates/content.md` for content pages.

## CI

GitHub Actions runs:

```sh
./gradlew ciCheck
```

with Temurin Java 21 on Ubuntu. CI also runs `npm run repo:check`, `npm run moddex:check`, and `mkdocs build --strict`. Local changes should pass the relevant checks before handoff.
