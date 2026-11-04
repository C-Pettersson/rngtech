package com.rngtech.content.affixsorter;

import com.rngtech.RNGTechConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Balance numbers for the Identifier and the Affix Sorter. Every number is an {@code identifier.*} or
 * {@code affixSorter.*} config key whose default matches the PRD.
 */
public final class AffixSorterCosts {
    public static final int DEFAULT_IDENTIFIER_FE_PER_ITEM = 16_000;
    public static final int DEFAULT_IDENTIFIER_FLUID_PER_ITEM = 100;
    public static final int DEFAULT_IDENTIFIER_TICKS_PER_ITEM = 100;
    public static final int DEFAULT_SORTER_BASE_FE = 4_000;
    public static final int DEFAULT_SORTER_TICKS_PER_ITEM = 20;
    public static final int DEFAULT_BASE_RULE_CAPACITY = 8;
    public static final double RULE_EXPONENT = 1.5D;
    public static final int MAX_STAGE = 8;

    private static <T> T configured(ModConfigSpec.ConfigValue<T> value, T fallback) {
        return RNGTechConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    /** A Stage 8 item pays the full cost; a Stage 0 item pays one ninth. */
    public static double stageShare(int stage) {
        return (Math.max(0, Math.min(MAX_STAGE, stage)) + 1) / (double) (MAX_STAGE + 1);
    }

    public static long identifierFePerItem(int stage) {
        return Math.max(1L, Math.round(configured(RNGTechConfig.IDENTIFIER_FE_PER_ITEM, DEFAULT_IDENTIFIER_FE_PER_ITEM) * stageShare(stage)));
    }

    public static int identifierFluidPerItem(int stage) {
        return Math.max(1, (int) Math.round(configured(RNGTechConfig.IDENTIFIER_FLUID_PER_ITEM, DEFAULT_IDENTIFIER_FLUID_PER_ITEM) * stageShare(stage)));
    }

    public static int identifierTicksPerItem() {
        return configured(RNGTechConfig.IDENTIFIER_TICKS_PER_ITEM, DEFAULT_IDENTIFIER_TICKS_PER_ITEM);
    }

    /** PRD, Energy: {@code FE per item = E0 x n^1.5} over enabled rules, with at least one rule. */
    public static long sorterFePerItem(int enabledRules) {
        double rules = Math.max(1, enabledRules);
        return Math.round(configured(RNGTechConfig.AFFIX_SORTER_BASE_FE, DEFAULT_SORTER_BASE_FE) * Math.pow(rules, RULE_EXPONENT));
    }

    public static int sorterTicksPerItem() {
        return configured(RNGTechConfig.AFFIX_SORTER_TICKS_PER_ITEM, DEFAULT_SORTER_TICKS_PER_ITEM);
    }

    public static int baseRuleCapacity() {
        return configured(RNGTechConfig.AFFIX_SORTER_BASE_RULE_CAPACITY, DEFAULT_BASE_RULE_CAPACITY);
    }

    private AffixSorterCosts() {
    }
}
