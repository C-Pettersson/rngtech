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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public record WoodenDehumidifierConversionRecipe(
        String group,
        Ingredient ingredient,
        SizedFluidIngredient waterInput,
        FluidStack fluidOutput,
        int processingTicks
)
        implements Recipe<WoodenDehumidifierConversionRecipeInput> {
    public FluidStack outputFluid() {
        return fluidOutput.copy();
    }

    @Override
    public boolean matches(WoodenDehumidifierConversionRecipeInput input, Level level) {
        return ingredient.test(input.ingredient()) && waterInput.test(input.water());
    }

    @Override
    public ItemStack assemble(WoodenDehumidifierConversionRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
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
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.WOODEN_DEHUMIDIFIER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<WoodenDehumidifierConversionRecipe> {
        private static final MapCodec<WoodenDehumidifierConversionRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                                Codec.STRING.optionalFieldOf("group", "").forGetter(WoodenDehumidifierConversionRecipe::group),
                                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(WoodenDehumidifierConversionRecipe::ingredient),
                                SizedFluidIngredient.NESTED_CODEC.fieldOf("water_input").forGetter(WoodenDehumidifierConversionRecipe::waterInput),
                                FluidStack.CODEC.fieldOf("fluid_output").forGetter(WoodenDehumidifierConversionRecipe::fluidOutput),
                                Codec.intRange(1, Integer.MAX_VALUE)
                                        .fieldOf("processing_ticks")
                                        .orElse(6000)
                                        .forGetter(WoodenDehumidifierConversionRecipe::processingTicks)
                        )
                        .apply(instance, WoodenDehumidifierConversionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WoodenDehumidifierConversionRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public WoodenDehumidifierConversionRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new WoodenDehumidifierConversionRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, WoodenDehumidifierConversionRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.waterInput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.fluidOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                    }
                };

        @Override
        public MapCodec<WoodenDehumidifierConversionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WoodenDehumidifierConversionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
