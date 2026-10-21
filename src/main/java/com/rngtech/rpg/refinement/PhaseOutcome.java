package com.rngtech.rpg.refinement;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** The weaker and the bonus results a Phase Array recipe names for its Partial and Overhold outcomes. */
public final class PhaseOutcome {
    private PhaseOutcome() {
    }

    /** What happens when the pulse slips one or two Coils short. */
    public enum Partial implements StringRepresentable {
        NONE,
        /** Reforge: rerolls the affixes but keeps rarity and RP. */
        REROLL_AFFIXES,
        /** Attune: rerolls one random affix's value. */
        REROLL_ONE_VALUE,
        /** Resonate, Exalt: rerolls the selected affix's value within its tier. */
        REROLL_SELECTED_VALUE,
        /** Excise: removes a random affix instead of the selected one. */
        REMOVE_RANDOM,
        /** Imprint: adds a random affix that shares a lens family with the chosen one. */
        ADD_RANDOM_OF_FAMILY,
        /** Transmute: swaps the selected affix for a random affix of the same slot type. */
        SWAP_RANDOM_SAME_SLOT,
        /** Temper: halves Strain, rounding down. */
        HALVE_STRAIN,
        /** Graft: the affix arrives one tier lower. */
        DONOR_TIER_DOWN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The bonus when every Coil holds on a line longer than the operation needs. */
    public enum Overhold implements StringRepresentable {
        NONE,
        /** Reforge: rolls rarity twice and keeps the better. */
        BEST_OF_TWO_RARITY,
        /** Attune: rolls each value twice and keeps the better. */
        BEST_OF_TWO_VALUES,
        /** Resonate: raises the affix two tiers. */
        TWO_TIERS,
        /** Excise: also removes one Strain. */
        REMOVE_STRAIN,
        /** Imprint: rolls the tier twice and keeps the higher. */
        BEST_OF_TWO_TIERS,
        /** Transmute: keeps the roll's position within its tier range. */
        KEEP_VALUE_QUALITY,
        /** Graft: the donor survives, stripped of that affix. */
        KEEP_DONOR;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
