package com.rngtech.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

public record CalibratedIngredient(Ingredient ingredient, Optional<CalibrationRequirement> calibration) {
    public CalibratedIngredient {
        ingredient = UnidentifiedTraitIngredient.wrapIfNeeded(ingredient);
    }

    public static final Codec<CalibratedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CalibratedIngredient::ingredient),
            CalibrationRequirement.CODEC.optionalFieldOf("calibration").forGetter(CalibratedIngredient::calibration)
    ).apply(instance, CalibratedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CalibratedIngredient> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CalibratedIngredient decode(RegistryFriendlyByteBuf buffer) {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                    Optional<CalibrationRequirement> calibration = ByteBufCodecs.BOOL.decode(buffer)
                            ? Optional.of(CalibrationRequirement.STREAM_CODEC.decode(buffer))
                            : Optional.empty();
                    return new CalibratedIngredient(ingredient, calibration);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, CalibratedIngredient value) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, value.ingredient);
                    ByteBufCodecs.BOOL.encode(buffer, value.calibration.isPresent());
                    value.calibration.ifPresent(requirement -> CalibrationRequirement.STREAM_CODEC.encode(buffer, requirement));
                }
            };

    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && calibration.map(requirement -> requirement.test(stack)).orElse(true);
    }
}
