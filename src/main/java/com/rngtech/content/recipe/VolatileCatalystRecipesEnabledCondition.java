package com.rngtech.content.recipe;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Loads the default Volatile Catalyst recipe only while {@code corruption.volatileCatalystRecipesEnabled} is on. */
public final class VolatileCatalystRecipesEnabledCondition implements ICondition {
    public static final VolatileCatalystRecipesEnabledCondition INSTANCE = new VolatileCatalystRecipesEnabledCondition();
    public static final MapCodec<VolatileCatalystRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private VolatileCatalystRecipesEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return RNGTechConfig.VOLATILE_CATALYST_RECIPES_ENABLED.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.VOLATILE_CATALYST_RECIPES_ENABLED.get();
    }
}
