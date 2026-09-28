package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineStat;

/**
 * Which Mastery effects a machine uses. Family rules are the default; a machine menu may narrow them for a specific
 * chassis, such as a fuel-burning Furnace that has no FE buffer.
 */
public interface MasteryApplicability {
    MachineMasteryFamily masteryFamily();

    default boolean masterySupports(MachineStat stat) { return masteryFamily().supports(stat); }

    default boolean masterySupportsAbsolute(MachineStat stat) { return masterySupports(stat) && masteryFamily().supportsAbsolute(stat); }

    default boolean masterySupportsBehavior(String behavior) { return masteryFamily().supportsBehavior(behavior); }

    default boolean masterySupports(MegaPassiveNode.TaggedEffect tagged) {
        return masteryFamily().has(tagged.tag()) && masterySupports(tagged.effect().stat());
    }

    static MasteryApplicability of(MachineMasteryFamily family) {
        return () -> family;
    }
}
