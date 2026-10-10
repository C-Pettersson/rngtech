package com.rngtech.content.loot;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModLoot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Optional;

/** Passes while {@code uniques.loot.enabled} and the named Unique's own {@code uniques.loot.<id>.enabled} key are on. */
public record UniqueLootEnabledCondition(Optional<ResourceLocation> unique) implements LootItemCondition {
    public static final MapCodec<UniqueLootEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("unique").forGetter(UniqueLootEnabledCondition::unique)
    ).apply(instance, UniqueLootEnabledCondition::new));

    @Override
    public LootItemConditionType getType() {
        return ModLoot.UNIQUE_LOOT_ENABLED.get();
    }

    @Override
    public boolean test(LootContext context) {
        return RNGTechConfig.uniqueLootEnabled(unique.map(ResourceLocation::getPath).orElse(""));
    }
}
