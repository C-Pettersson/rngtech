package com.rngtech.content.recipe;

import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.registry.ModDataComponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record CalibrationRequirement(
        CalibrationFamily family,
        int minStage,
        int minStability,
        int minRefinementPotential,
        int consumeRefinementPotential
) {
    public static final Codec<CalibrationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CalibrationFamily.CODEC.fieldOf("family").forGetter(CalibrationRequirement::family),
            Codec.intRange(0, 8).fieldOf("min_stage").orElse(0).forGetter(CalibrationRequirement::minStage),
            Codec.intRange(0, 100)
                    .fieldOf("min_stability")
                    .orElse(0)
                    .forGetter(CalibrationRequirement::minStability),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .fieldOf("min_refinement_potential")
                    .orElse(0)
                    .forGetter(CalibrationRequirement::minRefinementPotential),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .fieldOf("consume_refinement_potential")
                    .orElse(0)
                    .forGetter(CalibrationRequirement::consumeRefinementPotential)
    ).apply(instance, CalibrationRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalibrationRequirement> STREAM_CODEC =
            StreamCodec.composite(
                    CalibrationFamily.STREAM_CODEC,
                    CalibrationRequirement::family,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::minStage,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::minStability,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::minRefinementPotential,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::consumeRefinementPotential,
                    CalibrationRequirement::new
            );

    public CalibrationRequirement {
        minStage = Math.max(0, minStage);
        minStability = Math.max(0, minStability);
        minRefinementPotential = Math.max(minRefinementPotential, consumeRefinementPotential);
        consumeRefinementPotential = Math.max(0, consumeRefinementPotential);
    }

    public boolean test(ItemStack stack) {
        CalibrationState state = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        return state != null && state.meets(family, minStage, minStability, minRefinementPotential);
    }
}
