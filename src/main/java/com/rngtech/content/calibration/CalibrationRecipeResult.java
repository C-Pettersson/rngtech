package com.rngtech.content.calibration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record CalibrationRecipeResult(
        ItemStack stack,
        int stage,
        CalibrationValueRange stability,
        CalibrationValueRange refinementPotential
) {
    public static final Codec<CalibrationRecipeResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("stack").forGetter(CalibrationRecipeResult::stack),
            Codec.intRange(0, 8).fieldOf("stage").forGetter(CalibrationRecipeResult::stage),
            CalibrationValueRange.CODEC.fieldOf("stability").forGetter(CalibrationRecipeResult::stability),
            CalibrationValueRange.CODEC
                    .optionalFieldOf("refinement_potential", new CalibrationValueRange(0, 0))
                    .forGetter(CalibrationRecipeResult::refinementPotential)
    ).apply(instance, CalibrationRecipeResult::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalibrationRecipeResult> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC,
                    CalibrationRecipeResult::stack,
                    ByteBufCodecs.VAR_INT,
                    CalibrationRecipeResult::stage,
                    CalibrationValueRange.STREAM_CODEC,
                    CalibrationRecipeResult::stability,
                    CalibrationValueRange.STREAM_CODEC,
                    CalibrationRecipeResult::refinementPotential,
                    CalibrationRecipeResult::new
            );

    public CalibrationRecipeResult {
        stack = stack.copy();
        stage = Math.max(0, Math.min(8, stage));
    }

    public ItemStack stackWithState(CalibrationFamily family, int stabilityValue, int refinementPotentialValue) {
        ItemStack result = stack.copy();
        result.set(
                com.rngtech.content.registry.ModDataComponents.CALIBRATION_STATE.get(),
                new CalibrationState(family, stage, stabilityValue, refinementPotentialValue)
        );
        return result;
    }
}
