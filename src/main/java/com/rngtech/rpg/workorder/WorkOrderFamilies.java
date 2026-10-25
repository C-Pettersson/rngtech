package com.rngtech.rpg.workorder;

import com.rngtech.rpg.progression.MachineMasteryFamily;

import java.util.Locale;

/**
 * The machine kinds Work Orders name, by id. A Mastery machine's id is its Mastery family's. Machines without Mastery
 * join here when they get objectives.
 */
public final class WorkOrderFamilies {
    private WorkOrderFamilies() { }

    public static String of(MachineMasteryFamily family) {
        return family.name().toLowerCase(Locale.ROOT);
    }

    /** The highest stage any machine of {@code id} reaches, or 0 for an id no machine uses. */
    public static int topStage(String id) {
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            if (of(family).equals(id)) { return family.topStage(); }
        }
        return 0;
    }

    public static boolean known(String id) {
        return topStage(id) > 0;
    }
}
