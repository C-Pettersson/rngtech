package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.material.MaterialEnabledCondition;
import com.rngtech.content.material.OreWorldgenEnabledCondition;
import com.rngtech.content.recipe.AscendancySealRecipesEnabledCondition;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModConditions {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RNGTech.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<MaterialEnabledCondition>> MATERIAL_ENABLED =
            CONDITIONS.register("material_enabled", () -> MaterialEnabledCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<OreWorldgenEnabledCondition>> ORE_WORLDGEN_ENABLED =
            CONDITIONS.register("ore_worldgen_enabled", () -> OreWorldgenEnabledCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AscendancySealRecipesEnabledCondition>> ASCENDANCY_SEAL_RECIPES_ENABLED =
            CONDITIONS.register("ascendancy_seal_recipes_enabled", () -> AscendancySealRecipesEnabledCondition.CODEC);

    public static void register(IEventBus bus) {
        CONDITIONS.register(bus);
    }

    private ModConditions() {
    }
}
