package com.rngtech.rpg.workorder;

/**
 * Finished work a machine reports: the verb, the recipe's stage, the machine setting it ran at, the input item's id, and
 * how many items it completed. Fields a machine does not track stay 0 or empty.
 */
public record WorkReport(WorkObjectiveType type, int stage, int setting, String item, long amount) {
    public WorkReport {
        item = item == null ? "" : item;
    }
}
