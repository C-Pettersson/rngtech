package com.rngtech.rpg.workorder;

import java.util.Locale;

/** Which finished work a job part counts. */
public enum WorkFilter {
    /** Any work of the part's verb. */
    ANY,
    /** Work whose recipe stage is at or above the part's minimum: Processing Level for a Crusher, material stage for a Furnace. */
    STAGE,
    /** Work done with a machine setting at or above the part's minimum, such as the Furnace's heat slider in percent. */
    SETTING,
    /** Work on one named input item. */
    ITEM,
    /**
     * A stat goal: the machine's effective stat named by the part's item, at or above its target, which is in hundredths
     * of the stat's value. It counts no work; the machine checks the stat each time it completes a job.
     */
    STAT,
    /**
     * Work of the part's verb in a row, with its setting at or above the part's minimum (0 takes any): qualifying work
     * counts, and both other work of the verb and a failure on the machine, such as a Metal Press heat failure, set an
     * unfinished streak back to 0. The chip keeps its best streak.
     */
    STREAK,
    /** Work of one group the part's item names, such as Metal Press plates or Logic calibrations. */
    GROUP;

    /** Whether parts of this filter name what they count, an item or a group, from the entry's options. */
    public boolean named() {
        return this == ITEM || this == GROUP;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The filter with {@code id}, or null for a name this version does not know. */
    public static WorkFilter byId(String id) {
        for (WorkFilter filter : values()) {
            if (filter.id().equals(id)) { return filter; }
        }
        return null;
    }
}
