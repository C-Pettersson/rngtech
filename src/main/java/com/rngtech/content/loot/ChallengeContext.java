package com.rngtech.content.loot;

import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.workorder.FulfilledWorkOrder;

import java.util.Locale;

/**
 * What happened when a challenge loot table rolls. Fields an event does not have stay at their defaults: an empty
 * family or ascendancy, and zero for numbers. A Work Order reward also carries the order's tier, Bounty, Prestige, and
 * which of its reward rolls this is, counting from 0.
 */
public record ChallengeContext(
        String family,
        int machineStage,
        int recipeStage,
        int stability,
        int instability,
        int masteryLevel,
        String ascendancy,
        int orderTier,
        int bounty,
        int prestige,
        int orderRoll
) {
    public ChallengeContext {
        family = family == null ? "" : family;
        ascendancy = ascendancy == null ? "" : ascendancy;
    }

    public ChallengeContext(String family, int machineStage, int recipeStage, int stability, int instability, int masteryLevel, String ascendancy) {
        this(family, machineStage, recipeStage, stability, instability, masteryLevel, ascendancy, 0, 0, 0, 0);
    }

    /** A Mastery host's family, stage, level, and chosen ascendancy. */
    public static ChallengeContext of(MachineMasteryHost host) {
        MachineProgressionState state = host.masteryState();
        return new ChallengeContext(host.masteryFamily().name().toLowerCase(Locale.ROOT), host.ascendancyEntryStage(), 0, 0, 0,
                state.level(), state.ascendancy());
    }

    /** Reward roll {@code roll} of a Fulfilled Work Order. */
    public static ChallengeContext of(FulfilledWorkOrder order, int roll) {
        return new ChallengeContext(order.family(), 0, 0, 0, 0, 0, order.ascendancy(), order.tier(), order.bounty(), order.prestige(), roll);
    }

    public ChallengeContext withRecipeStage(int value) {
        return new ChallengeContext(family, machineStage, value, stability, instability, masteryLevel, ascendancy, orderTier, bounty, prestige, orderRoll);
    }

    public ChallengeContext withStability(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, value, instability, masteryLevel, ascendancy, orderTier, bounty, prestige, orderRoll);
    }

    public ChallengeContext withInstability(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, stability, value, masteryLevel, ascendancy, orderTier, bounty, prestige, orderRoll);
    }

    public ChallengeContext withMasteryLevel(int value) {
        return new ChallengeContext(family, machineStage, recipeStage, stability, instability, value, ascendancy, orderTier, bounty, prestige, orderRoll);
    }
}
