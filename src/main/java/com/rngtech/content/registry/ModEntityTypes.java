package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.entity.ForestryCartEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, RNGTech.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ForestryCartEntity>> FORESTRY_CART =
            ENTITY_TYPES.register(
                    "forestry_cart",
                    () -> EntityType.Builder.<ForestryCartEntity>of(ForestryCartEntity::new, MobCategory.MISC)
                            .sized(0.98F, 0.7F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("forestry_cart")
            );

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    private ModEntityTypes() {
    }
}
