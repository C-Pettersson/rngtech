package com.rngtech.content.material;

import com.rngtech.content.registry.ModConditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public record MaterialEnabledCondition(String material) implements ICondition {
    public static final MapCodec<MaterialEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.STRING.fieldOf("material").forGetter(MaterialEnabledCondition::material))
            .apply(instance, MaterialEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return MaterialEnablement.isEnabled(material);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ModConditions.MATERIAL_ENABLED.get();
    }
}
