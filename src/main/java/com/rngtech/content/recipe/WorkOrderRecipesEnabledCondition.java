package com.rngtech.content.recipe;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Loads the default Tier I Work Order recipe only while {@code workOrders.recipesEnabled} is on. */
public final class WorkOrderRecipesEnabledCondition implements ICondition {
    public static final WorkOrderRecipesEnabledCondition INSTANCE = new WorkOrderRecipesEnabledCondition();
    public static final MapCodec<WorkOrderRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private WorkOrderRecipesEnabledCondition() {
    }

    @Override
    public boolean test(IContext context) {
        return RNGTechConfig.WORK_ORDER_RECIPES_ENABLED.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.WORK_ORDER_RECIPES_ENABLED.get();
    }
}
