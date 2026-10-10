package com.rngtech.rpg.refinement;

import java.util.Locale;

public enum RefinementModifier {
    NONE(false, false, false),
    CONSERVE_CATALYST(false, false, false),
    REDUCE_POTENTIAL_COST(false, true, false),
    TRANSMUTE_UPGRADE(true, false, false),
    PRESERVE_ROLL(true, false, false),
    DESTABILIZE_OTHERS(true, false, false),
    /** Stabilization Crystal: removes the Blighted outcome from a Volatile Catalyst and is consumed. */
    CORRUPTION_WARD(false, false, true);

    private final boolean requiresSelectedUpgrade;
    private final boolean requiresRandomUpgrade;
    private final boolean requiresCorrupt;

    RefinementModifier(boolean requiresSelectedUpgrade, boolean requiresRandomUpgrade, boolean requiresCorrupt) {
        this.requiresSelectedUpgrade = requiresSelectedUpgrade;
        this.requiresRandomUpgrade = requiresRandomUpgrade;
        this.requiresCorrupt = requiresCorrupt;
    }

    public boolean requiresCorrupt() {
        return requiresCorrupt;
    }

    public boolean requiresSelectedUpgrade() {
        return requiresSelectedUpgrade;
    }

    public boolean requiresRandomUpgrade() {
        return requiresRandomUpgrade;
    }

    public String tooltipKey() {
        return "rngtech.tooltip.refinement_modifier." + name().toLowerCase(Locale.ROOT);
    }
}
