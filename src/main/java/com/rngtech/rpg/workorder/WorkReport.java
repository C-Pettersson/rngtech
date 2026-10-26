package com.rngtech.rpg.workorder;

/**
 * Finished work a machine reports: the verb, the recipe's stage, the machine setting it ran at, the item's id, how many
 * items it completed, its group, the kind of work such as a Metal Press mold or a calibration family, and the recipe's
 * base time per item in ticks, which weighs the work for any-work, stage, and setting jobs. Fields a machine does not
 * track stay 0 or empty.
 */
public record WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount, String group, int ticks) {
    public WorkReport {
        item = item == null ? "" : item;
        group = group == null ? "" : group;
        ticks = Math.max(0, ticks);
    }

    public WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount, String group) {
        this(type, stage, setting, item, amount, group, 0);
    }

    public WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount) {
        this(type, stage, setting, item, amount, "", 0);
    }

    /** This report with the recipe's base time per item. */
    public WorkReport withTicks(int value) {
        return new WorkReport(type, stage, setting, item, amount, group, value);
    }
}
