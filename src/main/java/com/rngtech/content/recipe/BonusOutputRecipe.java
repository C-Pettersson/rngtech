package com.rngtech.content.recipe;

/**
 * A recipe that can opt out of bonus output with {@code "bonus_output": false}. Every yield effect, such as Output
 * Amount, Super Output, and salvage, checks it, so a recipe that could form a loop never pays a bonus.
 */
public interface BonusOutputRecipe {
    boolean allowsBonusOutput();
}
