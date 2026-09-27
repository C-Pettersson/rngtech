package com.rngtech.rpg.refinement;

import java.util.Locale;

public enum RefinementModifier {
    NONE(false, false),
    CONSERVE_CATALYST(false, false),
    REDUCE_POTENTIAL_COST(false, true),
    TRANSMUTE_UPGRADE(true, false),
    PRESERVE_ROLL(true, false),
    DESTABILIZE_OTHERS(true, false),
    CORRUPTION_WARD(false, false);

    private final boolean requiresSelectedUpgrade;
    private final boolean requiresRandomUpgrade;

    RefinementModifier(boolean requiresSelectedUpgrade, boolean requiresRandomUpgrade) {
        this.requiresSelectedUpgrade = requiresSelectedUpgrade;
        this.requiresRandomUpgrade = requiresRandomUpgrade;
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
