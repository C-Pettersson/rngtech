# Quest Stage Coverage

Status: Prototype

The optional quest extra contains several chapters under `extras/ftbquests/config/ftbquests/quests/chapters/`, with text in the adjacent `lang/en_us.snbt` file. It is a pack-author guide, not a requirement of standalone RNGTech.

## Check coverage

Run `npm run moddex:check`, then use the ModDex Stages and recipe views to compare quest targets with current recipes and component stages. Read all chapters, including the separate stage, logistics, and refinement chapters. Item mentions are not the same as distinct quests or required progression gates.

Recompute counts when reviewing a pack; do not use an old snapshot as proof of survival reachability. ModDex models selected code-driven processes, but an analyzer result still needs in-game validation.

## Authoring rules

- Gate mandatory quests with available recipes and machine Gear, including temperature and calibration requirements.
- Teach a machine process before requiring an output from it. Check press and Circuit Mold requirements before electric-furnace gates.
- Use optional branches for sidegrades, storage variants, and repeated material tiers.
- Verify late-game quest lines against [Component Stages](component-stages.md), including the Stage 8 capstone.
- Keep loot-only items, compatibility-only IDs, and planned machines out of mandatory crafting quests.
- Test rewards, unlock dependencies, and completion in a new quest save. Pack recipe changes can invalidate the default progression.

The quest files ship as an optional extra. Pack authors decide whether to include them and should review rewards before distribution.
