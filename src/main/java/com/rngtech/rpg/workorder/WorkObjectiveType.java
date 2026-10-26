package com.rngtech.rpg.workorder;

import java.util.Locale;

/** The work a machine reports toward a Work Order: the verb a job part counts. */
public enum WorkObjectiveType {
    /** Completed Crusher jobs. */
    CRUSH,
    /** Completed Furnace smelts. */
    SMELT,
    /** Completed Component Assembler assemblies. */
    ASSEMBLE,
    /** Completed Alloy Furnace recipe sets. */
    ALLOY,
    /** Completed Metal Press jobs. */
    PRESS,
    /** Fluid a Melter makes, in mB. */
    MELT,
    /** Components a Resonance Calibrator calibrates. */
    CALIBRATE,
    /** Logs a Forestry Companion fells and crops it harvests. */
    HARVEST;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The type with {@code id}, or null for a name this version does not know. */
    public static WorkObjectiveType byId(String id) {
        for (WorkObjectiveType type : values()) {
            if (type.id().equals(id)) { return type; }
        }
        return null;
    }
}
