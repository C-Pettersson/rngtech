package com.rngtech.content.recipe;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Loads the default Ascendancy Seal recipes only while {@code ascendancy.sealRecipesEnabled} is on. */
public final class AscendancySealRecipesEnabledCondition implements ICondition {
    public static final AscendancySealRecipesEnabledCondition INSTANCE = new AscendancySealRecipesEnabledCondition();
    public static final MapCodec<AscendancySealRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private AscendancySealRecipesEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return RNGTechConfig.ASCENDANCY_SEAL_RECIPES_ENABLED.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.ASCENDANCY_SEAL_RECIPES_ENABLED.get();
    }
}
