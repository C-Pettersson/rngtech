package com.rngtech.content.calibration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record CalibrationState(
        CalibrationFamily family,
        int stage,
        int stability,
        int refinementPotential
) {
    public static final Codec<CalibrationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CalibrationFamily.CODEC.fieldOf("family").forGetter(CalibrationState::family),
            Codec.intRange(0, 8).fieldOf("stage").forGetter(CalibrationState::stage),
            Codec.intRange(0, 100).fieldOf("stability").forGetter(CalibrationState::stability),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .fieldOf("refinement_potential")
                    .forGetter(CalibrationState::refinementPotential)
    ).apply(instance, CalibrationState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalibrationState> STREAM_CODEC = StreamCodec.composite(
            CalibrationFamily.STREAM_CODEC,
            CalibrationState::family,
            ByteBufCodecs.VAR_INT,
            CalibrationState::stage,
            ByteBufCodecs.VAR_INT,
            CalibrationState::stability,
            ByteBufCodecs.VAR_INT,
            CalibrationState::refinementPotential,
            CalibrationState::new
    );

    public CalibrationState {
        stage = Mth.clamp(stage, 0, 8);
        stability = Mth.clamp(stability, 0, 100);
        refinementPotential = Math.max(0, refinementPotential);
    }

    public boolean meets(CalibrationFamily requiredFamily, int minimumStage, int minimumStability, int minimumRefinementPotential) {
        return family == requiredFamily
                && stage >= minimumStage
                && stability >= minimumStability
                && refinementPotential >= minimumRefinementPotential;
    }
}
