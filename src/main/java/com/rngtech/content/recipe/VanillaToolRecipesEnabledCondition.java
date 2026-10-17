package com.rngtech.content.recipe;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Loads RNGTech's copies of the vanilla tool recipes only while {@code restrictedTools.vanillaRecipesEnabled} is on. */
public final class VanillaToolRecipesEnabledCondition implements ICondition {
    public static final VanillaToolRecipesEnabledCondition INSTANCE = new VanillaToolRecipesEnabledCondition();
    public static final MapCodec<VanillaToolRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private VanillaToolRecipesEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return RNGTechConfig.VANILLA_TOOL_RECIPES_ENABLED.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.VANILLA_TOOL_RECIPES_ENABLED.get();
    }
}
