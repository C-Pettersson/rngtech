package com.rngtech.content.loot;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.registry.ModLoot;
import com.rngtech.rpg.jewel.JewelColour;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Optional;

/** Passes while {@code jewels.loot.enabled} and, when a colour is named, its own {@code jewels.loot.<colour>.enabled} key are on. */
public record JewelLootEnabledCondition(Optional<JewelColour> colour) implements LootItemCondition {
    public static final MapCodec<JewelLootEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            JewelColour.CODEC.optionalFieldOf("colour").forGetter(JewelLootEnabledCondition::colour)
    ).apply(instance, JewelLootEnabledCondition::new));

    @Override
    public LootItemConditionType getType() {
        return ModLoot.JEWEL_LOOT_ENABLED.get();
    }

    @Override
    public boolean test(LootContext context) {
        return RNGTechConfig.jewelLootEnabled(colour.orElse(null));
    }
}
