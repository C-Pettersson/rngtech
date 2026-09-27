package com.rngtech.content.recipe;

import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationRecipeResult;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record CalibrationRecipe(
        String group,
        CalibrationFamily family,
        Ingredient ingredient,
        Ingredient pattern,
        Ingredient catalyst,
        Optional<Ingredient> stabilizer,
        CalibrationRecipeResult result,
        int minimumStage,
        int processingTicks,
        int energy
) implements Recipe<CalibrationRecipeInput> {
    public CalibrationRecipe {
        minimumStage = minimumStage < 0 ? result.stage() : Math.max(0, Math.min(8, minimumStage));
    }

    public ItemStack outputStack() {
        return result.stack().copy();
    }

    @Override
    public boolean matches(CalibrationRecipeInput input, Level level) {
        return ingredient.test(input.input())
                && pattern.test(input.pattern())
                && catalyst.test(input.catalyst())
                && stabilizer.map(stabilizerIngredient -> stabilizerIngredient.test(input.stabilizer())).orElse(true);
    }

    @Override
    public ItemStack assemble(CalibrationRecipeInput input, HolderLookup.Provider registries) {
        return result.stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        ingredients.add(pattern);
        ingredients.add(catalyst);
        stabilizer.ifPresent(ingredients::add);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.resonanceCalibrator(com.rngtech.content.calibration.ResonanceCalibratorChassis.IRON).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CALIBRATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CALIBRATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CalibrationRecipe> {
        private static final MapCodec<CalibrationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CalibrationRecipe::group),
                        CalibrationFamily.CODEC.fieldOf("family").forGetter(CalibrationRecipe::family),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CalibrationRecipe::ingredient),
                        Ingredient.CODEC_NONEMPTY.fieldOf("pattern").forGetter(CalibrationRecipe::pattern),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(CalibrationRecipe::catalyst),
                        Ingredient.CODEC_NONEMPTY.optionalFieldOf("stabilizer").forGetter(CalibrationRecipe::stabilizer),
                        CalibrationRecipeResult.CODEC.fieldOf("result").forGetter(CalibrationRecipe::result),
                        Codec.INT.optionalFieldOf("minimum_stage", -1).forGetter(CalibrationRecipe::minimumStage),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(160)
                                .forGetter(CalibrationRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").orElse(1200).forGetter(CalibrationRecipe::energy)
                )
                .apply(instance, CalibrationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CalibrationRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CalibrationRecipe decode(RegistryFriendlyByteBuf buffer) {
                        String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
                        CalibrationFamily family = CalibrationFamily.STREAM_CODEC.decode(buffer);
                        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                        Ingredient pattern = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                        Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                        Optional<Ingredient> stabilizer = ByteBufCodecs.BOOL.decode(buffer)
                                ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer))
                                : Optional.empty();
                        CalibrationRecipeResult result = CalibrationRecipeResult.STREAM_CODEC.decode(buffer);
                        int minimumStage = ByteBufCodecs.VAR_INT.decode(buffer);
                        int processingTicks = ByteBufCodecs.VAR_INT.decode(buffer);
                        int energy = ByteBufCodecs.VAR_INT.decode(buffer);
                        return new CalibrationRecipe(
                                group,
                                family,
                                ingredient,
                                pattern,
                                catalyst,
                                stabilizer,
                                result,
                                minimumStage,
                                processingTicks,
                                energy
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CalibrationRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        CalibrationFamily.STREAM_CODEC.encode(buffer, recipe.family);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.pattern);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.stabilizer.isPresent());
                        recipe.stabilizer.ifPresent(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
                        CalibrationRecipeResult.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumStage);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                    }
                };

        @Override
        public MapCodec<CalibrationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CalibrationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
