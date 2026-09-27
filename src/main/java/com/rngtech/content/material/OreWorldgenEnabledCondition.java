package com.rngtech.content.material;

import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public record OreWorldgenEnabledCondition(String material) implements ICondition {
    public static final MapCodec<OreWorldgenEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.STRING.fieldOf("material").forGetter(OreWorldgenEnabledCondition::material))
            .apply(instance, OreWorldgenEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return OreWorldgenEnablement.isEnabled(material);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.ORE_WORLDGEN_ENABLED.get();
    }
}
