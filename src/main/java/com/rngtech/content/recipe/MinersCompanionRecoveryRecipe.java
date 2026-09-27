package com.rngtech.content.recipe;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record MinersCompanionRecoveryRecipe(
        String group,
        Ingredient input,
        ItemStack result,
        int weight,
        int minFilterStage,
        int minProcessingLevel
) implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
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
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.MINERS_COMPANION.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MINERS_COMPANION_RECOVERY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MINERS_COMPANION_RECOVERY_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MinersCompanionRecoveryRecipe> {
        private static final MapCodec<MinersCompanionRecoveryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(MinersCompanionRecoveryRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(MinersCompanionRecoveryRecipe::input),
                ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(MinersCompanionRecoveryRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("weight", 1).forGetter(MinersCompanionRecoveryRecipe::weight),
                Codec.intRange(1, Integer.MAX_VALUE)
                        .optionalFieldOf("min_filter_stage", 1)
                        .forGetter(MinersCompanionRecoveryRecipe::minFilterStage),
                Codec.intRange(1, Integer.MAX_VALUE)
                        .optionalFieldOf("min_processing_level", 1)
                        .forGetter(MinersCompanionRecoveryRecipe::minProcessingLevel)
        ).apply(instance, MinersCompanionRecoveryRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MinersCompanionRecoveryRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        MinersCompanionRecoveryRecipe::group,
                        Ingredient.CONTENTS_STREAM_CODEC,
                        MinersCompanionRecoveryRecipe::input,
                        ItemStack.OPTIONAL_STREAM_CODEC,
                        MinersCompanionRecoveryRecipe::result,
                        ByteBufCodecs.VAR_INT,
                        MinersCompanionRecoveryRecipe::weight,
                        ByteBufCodecs.VAR_INT,
                        MinersCompanionRecoveryRecipe::minFilterStage,
                        ByteBufCodecs.VAR_INT,
                        MinersCompanionRecoveryRecipe::minProcessingLevel,
                        MinersCompanionRecoveryRecipe::new
                );

        @Override
        public MapCodec<MinersCompanionRecoveryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MinersCompanionRecoveryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
