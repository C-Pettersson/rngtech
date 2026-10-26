package com.rngtech.rpg.workorder;

/**
 * Finished work a machine reports: the verb, the recipe's stage, the machine setting it ran at, the item's id, how many
 * items it completed, and its group, the kind of work such as a Metal Press mold or a calibration family. Fields a machine
 * does not track stay 0 or empty.
 */
public record WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount, String group) {
    public WorkReport {
        item = item == null ? "" : item;
        group = group == null ? "" : group;
    }

    public WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount) {
        this(type, stage, setting, item, amount, "");
    }
}
