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
        int maxStability,
        int minRefinementPotential,
        int consumeRefinementPotential
) {
    public static final int MAX_STABILITY = 100;

    public static final Codec<CalibrationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CalibrationFamily.CODEC.fieldOf("family").forGetter(CalibrationRequirement::family),
            Codec.intRange(0, 8).fieldOf("min_stage").orElse(0).forGetter(CalibrationRequirement::minStage),
            Codec.intRange(0, 100)
                    .fieldOf("min_stability")
                    .orElse(0)
                    .forGetter(CalibrationRequirement::minStability),
            Codec.intRange(0, MAX_STABILITY)
                    .fieldOf("max_stability")
                    .orElse(MAX_STABILITY)
                    .forGetter(CalibrationRequirement::maxStability),
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
                    CalibrationRequirement::maxStability,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::minRefinementPotential,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRequirement::consumeRefinementPotential,
                    CalibrationRequirement::new
            );

    public CalibrationRequirement {
        minStage = Math.max(0, minStage);
        minStability = Math.max(0, minStability);
        maxStability = Math.max(minStability, Math.min(MAX_STABILITY, maxStability));
        minRefinementPotential = Math.max(minRefinementPotential, consumeRefinementPotential);
        consumeRefinementPotential = Math.max(0, consumeRefinementPotential);
    }

    public boolean test(ItemStack stack) {
        return accepts(stack.get(ModDataComponents.CALIBRATION_STATE.get()));
    }

    public boolean accepts(CalibrationState state) {
        return state != null
                && state.meets(family, minStage, minStability, minRefinementPotential)
                && state.stability() <= maxStability;
    }

    /** Whether the requirement caps stability, so a well-tuned part overshoots it. */
    public boolean hasStabilityCeiling() {
        return maxStability < MAX_STABILITY;
    }
}
