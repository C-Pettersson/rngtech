package com.rngtech.rpg.jewel;

import com.rngtech.rpg.MachineStat;

import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.Set;

/**
 * The jewel pool's ids and groups, the attribute thresholds its chase lines wait for, and how a cut jewel's
 * Refinement Potential follows its calibration stability. The definitions themselves live in
 * {@code ModifierEligibilityProfiles}, where every affix family is declared.
 */
public final class JewelAffixes {
    public static final String CAPACITIVE_GROUP = "jewel_capacitive";
    public static final String FRUGAL_GROUP = "jewel_frugal";
    public static final String STEADY_GROUP = "jewel_steady";
    public static final String DEEP_GROUP = "jewel_deep";
    public static final String INSULATED_GROUP = "jewel_insulated";
    public static final String CONDUCTIVE_GROUP = "jewel_conductive";
    public static final String THRIFTY_GROUP = "jewel_thrifty";
    public static final String YIELDING_GROUP = "jewel_yielding";
    public static final String FORTUNATE_GROUP = "jewel_fortunate";
    public static final String PACE_GROUP = "jewel_pace";
    public static final String OUTPUT_GROUP = "jewel_output";
    public static final String CONTROL_GROUP = "jewel_control";
    public static final String DRIVE_GROUP = "jewel_drive";
    public static final String RESERVE_GROUP = "jewel_reserve";
    public static final String STUDY_GROUP = "jewel_study";
    public static final String FLASHES_GROUP = "jewel_flashes";
    public static final String HEADROOM_GROUP = "jewel_headroom";
    public static final String MOMENTUM_GROUP = "jewel_momentum";
    public static final String POISE_GROUP = "jewel_poise";
    public static final String DEPTH_GROUP = "jewel_depth";

    public static final String MOMENTUM_ID = "jewel_momentum";
    public static final String POISE_ID = "jewel_poise";
    public static final String DEPTH_ID = "jewel_depth";

    /** Lines that keep their own weight on every colour instead of the home and away weights. */
    public static final Set<String> CHASE_GROUPS = Set.of(HEADROOM_GROUP, MOMENTUM_GROUP, POISE_GROUP, DEPTH_GROUP);

    /** The attribute total a threshold line waits for, read after Gear and every jewel's flat attribute lines. */
    public static final int ATTRIBUTE_THRESHOLD = 100;

    private static final Map<String, JewelColour> THRESHOLD_COLOURS = Map.of(
            MOMENTUM_ID, JewelColour.DRIVE,
            POISE_ID, JewelColour.CONTROL,
            DEPTH_ID, JewelColour.RESERVE
    );

    /** The least and most Refinement Potential a jewel can be cut with. */
    public static final int MIN_REFINEMENT_POTENTIAL = 2;
    public static final int MAX_REFINEMENT_POTENTIAL = 4;

    private JewelAffixes() {
    }

    /** The colour whose threshold line {@code affixId} is, or null for every other affix. */
    public static JewelColour thresholdColour(String affixId) {
        return THRESHOLD_COLOURS.get(affixId);
    }

    /** The attribute a threshold line reads, or null for every other affix. */
    public static MachineStat thresholdAttribute(String affixId) {
        JewelColour colour = thresholdColour(affixId);
        return colour == null ? null : colour.attribute();
    }

    /** Refinement Potential from the cut's stability: 2 below 50, 3 below 80, 4 from 80 up. */
    public static int refinementPotential(int stability) {
        if (stability >= 80) {
            return MAX_REFINEMENT_POTENTIAL;
        }
        return stability >= 50 ? 3 : MIN_REFINEMENT_POTENTIAL;
    }

    /** Refinement Potential for a jewel that was never cut, such as one from a chest: any value a cut can give. */
    public static int uncutRefinementPotential(RandomSource random) {
        return MIN_REFINEMENT_POTENTIAL + random.nextInt(MAX_REFINEMENT_POTENTIAL - MIN_REFINEMENT_POTENTIAL + 1);
    }
}
