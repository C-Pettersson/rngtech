# Releasing

## Version and downloads

The source version is `mod_version` in `gradle.properties`. Release builds take their version from the tag or the manual publishing workflow input. Published artifacts and their notes live on [GitHub Releases](https://github.com/c-pettersson/rngtech/releases) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/rngtech).

The in-game update checker is disabled. There is no maintained `update.json` feed. Use the release pages to check available versions; a source version bump does not establish that a release has been published.

## Verification

Install Java 21, Node.js 22.22.2 or newer in the 22.x line, and the Python documentation dependencies. See `package.json` for other supported Node versions. Use the checked-in Gradle wrapper (`.\gradlew.bat` on Windows).

```sh
npm ci
npm run repo:check
npm run moddex:check
./gradlew ciCheck
python -m pip install -r requirements-docs.txt
mkdocs build --strict
```

The Gradle build checks formatting, compiles Java, processes resources, and packages the mod. There is no Java test source tree. ModDex checks selected source/data invariants and passive-tree geometry. The repository check checks local documentation links and common private-data patterns. None of these commands certifies all gameplay behavior or replaces a full secret scan of Git history.

Before publishing, record results from a disposable world:

- Start a standalone client and a dedicated server with the release JAR. Check that optional integrations are optional.
- Craft, identify, place, equip, run, refine, break, and reload representative machines; check that stored Gear and traits survive.
- Transfer items into nearly full inventories. Verify accepted counts and that partial remainders return or drop once.
- Check connector channels, directions, disabled links, and fluid filters. Verify that matching network bridges require their optional mod.
- Fill machine outputs and confirm inputs remain until results fit. Check reversible recipes with output bonuses.
- Check protected Gear automation boundaries and intentional exceptions.
- Confirm production builds disable debug sources, sinks, and connector modules.
- Exercise Forestry Companion docking, cargo unloading, charging, harvesting, and mastery persistence.

These are release checks to perform, not claims that the current revision has passed them. Include tested versions and integrations in the release notes.

## Publish

The release tooling uses `release-it`. Preview a version change with:

```sh
npm run release:dry-run -- patch
```

To publish a prepared release:

```sh
npm run release -- patch
```

This updates `mod_version`, runs `quickCheck`, commits the version change, creates a `vX.Y.Z` tag, and pushes it. Run the full verification above before this command. The tag workflow builds the JAR, packages the optional quest ZIP, creates a GitHub Release, and publishes to GitHub Packages. A hyphenated version becomes a GitHub prerelease.

Normal changes go through pull requests with passing CI and resolved review conversations. The repository administrator can bypass the pull-request rules for the release command's version commit. Keep that bypass limited to reviewed releases or recovery; it is not a substitute for the verification above. The separate rules against deleting or force-pushing `main` apply to administrators too.

CurseForge publication uses the manual **Publish CurseForge Release** workflow. Configure `CURSEFORGE_API_TOKEN` as a repository secret and `CURSEFORGE_PROJECT_ID` as a repository variable. Select the target version and release type. The workflow can upload the optional quests as a child file. It does not run automatically from a Git tag.

Branch and pull-request Actions artifacts expire and are intended for testing. Use release JARs for packs. Keep tokens in secret storage, and review the author name/email used by release commits before pushing.
