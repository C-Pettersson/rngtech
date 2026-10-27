package com.rngtech.rpg.refinement;

import java.util.Locale;

public enum RefinementModifier {
    NONE,
    /** Conservation Crystal: 25% chance to keep the catalyst. Works with every catalyst but the Volatile and Mirror Catalysts. */
    CONSERVE_CATALYST,
    /** Frugality Crystal: 25% chance a rolled RP cost becomes 1 RP. */
    REDUCE_POTENTIAL_COST,
    /** Transmutation Crystal: the upgraded affix becomes a different affix in the same slot. */
    TRANSMUTE_UPGRADE,
    /** Resonance Crystal: the upgraded affix keeps its roll quality in its new tier. */
    PRESERVE_ROLL,
    /** Destabilization Crystal: rerolls the other affixes after the upgrade. */
    DESTABILIZE_OTHERS,
    /** Stabilization Crystal: removes the Blighted outcome from a Volatile Catalyst and is consumed. */
    CORRUPTION_WARD;

    /** The failure key when this crystal does not work with {@code operation}, or null when it does. */
    public String failureKey(RefinementOperation operation) {
        RefinementAction action = operation.action();
        if (this != NONE && action == RefinementAction.MIRROR) {
            return "rngtech.refinement.failure.focus_rejects_mirror";
        }
        return switch (this) {
            case NONE -> null;
            case CONSERVE_CATALYST -> action == RefinementAction.CORRUPT
                    ? "rngtech.refinement.failure.modifier_rejects_corrupt"
                    : null;
            case REDUCE_POTENTIAL_COST -> action == RefinementAction.CORRUPT || action == RefinementAction.ASCEND_RARITY
                    ? "rngtech.refinement.failure.modifier_requires_rp_cost"
                    : null;
            case TRANSMUTE_UPGRADE, PRESERVE_ROLL, DESTABILIZE_OTHERS ->
                    action == RefinementAction.RANDOM_UPGRADE || action == RefinementAction.SELECTED_UPGRADE
                            ? null
                            : "rngtech.refinement.failure.modifier_requires_upgrade";
            case CORRUPTION_WARD -> action == RefinementAction.CORRUPT
                    ? null
                    : "rngtech.refinement.failure.modifier_requires_corrupt";
        };
    }

    public String tooltipKey() {
        return "rngtech.tooltip.refinement_modifier." + name().toLowerCase(Locale.ROOT);
    }
}
