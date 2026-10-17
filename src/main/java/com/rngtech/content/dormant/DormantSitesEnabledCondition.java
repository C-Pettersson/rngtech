package com.rngtech.content.dormant;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Gates the Dormant Machine structure sets on {@code dormantMachines.enabled}. */
public record DormantSitesEnabledCondition() implements ICondition {
    public static final DormantSitesEnabledCondition INSTANCE = new DormantSitesEnabledCondition();
    public static final MapCodec<DormantSitesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return RNGTechConfig.DORMANT_SITES_ENABLED.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.DORMANT_SITES_ENABLED.get();
    }
}
