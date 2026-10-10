package com.rngtech.content.loot;

import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;

import java.util.Locale;

/**
 * What happened when a challenge loot table rolls. Fields an event does not have stay at their defaults: an empty
 * family or ascendancy, and zero for numbers.
 */
public record ChallengeContext(
        String family,
        int machineStage,
        int recipeStage,
        int stability,
        int instability,
        int masteryLevel,
        String ascendancy
) {
    public ChallengeContext {
        family = family == null ? "" : family;
        ascendancy = ascendancy == null ? "" : ascendancy;
    }

    /** A Mastery host's family, stage, level, and chosen ascendancy. */
    public static ChallengeContext of(MachineMasteryHost host) {
        MachineProgressionState state = host.masteryState();
        return new ChallengeContext(host.masteryFamily().name().toLowerCase(Locale.ROOT), host.ascendancyEntryStage(), 0, 0, 0,
                state.level(), state.ascendancy());
    }

    public ChallengeContext withRecipeStage(int value) {
        return new ChallengeContext(family, machineStage, value, stability, instability, masteryLevel, ascendancy);
    }

    public ChallengeContext withStability(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, value, instability, masteryLevel, ascendancy);
    }

    public ChallengeContext withInstability(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, stability, value, masteryLevel, ascendancy);
    }

    public ChallengeContext withMasteryLevel(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, stability, instability, value, ascendancy);
    }
}
