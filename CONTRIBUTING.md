# Contributing

RNGTech targets Minecraft 1.21.1, NeoForge, and Java 21. Use the checked-in Gradle wrapper; on Windows use `.\gradlew.bat` in place of `./gradlew`.

## Set up and verify

```sh
./gradlew quickCheck
./gradlew ciCheck
npm ci
npm run repo:check
npm run moddex:check
python -m pip install -r requirements-docs.txt
mkdocs build --strict
```

Node.js 20.19 or newer is required for ModDex and release tooling. `quickCheck` checks formatting, compiles Java, and processes resources. `ciCheck` runs the Gradle build. ModDex checks source/data consistency, recipe sentinels, and passive-tree layouts. There is no Java test source tree, so these commands do not establish gameplay correctness.

Use `./gradlew runClient` for in-game checks, `./gradlew runServer` for a development server, and `./gradlew spotlessApply` for formatting. The development runs include optional integration mods. Report which integrations and client/server configurations you tested.

## Scope and documentation

Follow [AGENTS.md](AGENTS.md), the [current implementation reference](docs/reference/current-implementation.md), and [machine guidelines](docs/reference/machine-guidelines.md). Describe the problem, resulting behavior, and validation in a pull request. Keep unrelated refactors separate.

Update the relevant content page when behavior changes. Follow [documentation conventions](docs/conventions.md); mark unimplemented designs as Planned and keep descriptions aligned with current behavior. Recipe IDs, stages, and Gear requirements should agree with the registries and recipe data.

Do not commit credentials, personal paths, local editor state, game saves, logs, or generated build output. Review your Git author name/email before making public commits. The repository check catches common patterns; it is not a full secret scanner.

By contributing, you agree that your contribution is licensed under the MIT License.
