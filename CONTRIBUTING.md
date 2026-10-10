# Contributing

RNGTech targets Minecraft 1.21.1, NeoForge, and Java 21. Use the checked-in Gradle wrapper; on Windows use `.\gradlew.bat` in place of `./gradlew`.

## Propose a change

Check [open issues](https://github.com/C-Pettersson/rngtech/issues) before reporting a bug or proposing a feature. Discuss large gameplay or progression changes before implementing them. See [Support](SUPPORT.md) for help and [Security](SECURITY.md) for private vulnerability reports. Follow the [Code of Conduct](CODE_OF_CONDUCT.md) in project discussions.

Fork the repository, create a branch, and open a pull request against `main`. Use a Conventional Commits title, such as `fix: preserve battery charge when breaking a machine` or `docs: clarify generator fuel requirements`. Maintainers squash pull requests using the PR title and description as the commit message.

Pull requests must pass the build, repository, and dependency review checks and have their review conversations resolved. No second-person approval is required. Maintainers review gameplay and compatibility changes before merging; passing CI does not verify gameplay.

## Build from source

Install Java 21 and run `./gradlew build`. The mod JAR appears at `build/libs/rngtech-<version>.jar`. Gradle downloads the development dependencies on the first run. The development client includes optional mods for integration testing; they are not required dependencies of the published mod.

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

Use Node.js 22.22.2 or newer in the 22.x line for the Node tooling; CI uses Node 22. See `package.json` for other supported Node versions. `quickCheck` checks formatting, compiles Java, processes resources, and runs the domain checks in `src/masteryTest`. `ciCheck` runs the Gradle build. ModDex checks source/data consistency, recipe sentinels, and passive-tree layouts. These commands do not establish gameplay correctness.

To work on the documentation, run `mkdocs serve` for the design docs or `mkdocs serve -f mkdocs.wiki.yml` for the player wiki, or run `docker compose -f compose.docs.yml up` and open <http://localhost:8000>. See the [ModDex README](tools/moddex/README.md) for the source/data explorer.

Use `./gradlew runClient` for in-game checks, `./gradlew runServer` for a development server, and `./gradlew spotlessApply` for formatting. The development runs include optional integration mods. Report which integrations and client/server configurations you tested.

## Scope and documentation

Follow [AGENTS.md](AGENTS.md), the [current implementation reference](docs/reference/current-implementation.md), and [machine guidelines](docs/reference/machine-guidelines.md). Describe the problem, resulting behavior, and validation in a pull request. Keep unrelated refactors separate.

Update the relevant content page when behavior changes. Follow [documentation conventions](docs/conventions.md); mark unimplemented designs as Planned and keep descriptions aligned with current behavior. Recipe IDs, stages, and Gear requirements should agree with the registries and recipe data.

Do not commit credentials, personal paths, local editor state, game saves, logs, or generated build output. Review your Git author name/email before making public commits. The repository check catches common patterns; it is not a full secret scanner.

By contributing, you agree that your contribution is licensed under the MIT License.
