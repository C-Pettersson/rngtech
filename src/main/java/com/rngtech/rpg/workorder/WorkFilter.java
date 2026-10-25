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
    STAT;

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
