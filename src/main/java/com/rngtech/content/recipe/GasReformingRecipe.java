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

public record GasReformingRecipe(
        String group,
        Ingredient catalyst,
        SizedFluidIngredient gasInput,
        SizedFluidIngredient waterInput,
        FluidStack hydrogenOutput,
        FluidStack carbonMonoxideOutput,
        int energy,
        int processingTicks,
        int minimumCatalystStage
)
        implements Recipe<GasReformingRecipeInput> {
    public FluidStack hydrogenFluid() {
        return hydrogenOutput.copy();
    }

    public FluidStack carbonMonoxideFluid() {
        return carbonMonoxideOutput.copy();
    }

    @Override
    public boolean matches(GasReformingRecipeInput input, Level level) {
        return catalyst.test(input.catalyst()) && gasInput.test(input.feedGas()) && waterInput.test(input.water());
    }

    @Override
    public ItemStack assemble(GasReformingRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.add(catalyst);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.STEAM_METHANE_REFORMER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GAS_REFORMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GAS_REFORMING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GasReformingRecipe> {
        private static final MapCodec<GasReformingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(GasReformingRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(GasReformingRecipe::catalyst),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("gas_input").forGetter(GasReformingRecipe::gasInput),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("water_input").forGetter(GasReformingRecipe::waterInput),
                        FluidStack.CODEC.fieldOf("hydrogen_output").forGetter(GasReformingRecipe::hydrogenOutput),
                        FluidStack.CODEC.fieldOf("carbon_monoxide_output").forGetter(GasReformingRecipe::carbonMonoxideOutput),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(GasReformingRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(400)
                                .forGetter(GasReformingRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_catalyst_stage")
                                .orElse(6)
                                .forGetter(GasReformingRecipe::minimumCatalystStage)
                )
                .apply(instance, GasReformingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GasReformingRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public GasReformingRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new GasReformingRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, GasReformingRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.gasInput);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.waterInput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.hydrogenOutput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.carbonMonoxideOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumCatalystStage);
                    }
                };

        @Override
        public MapCodec<GasReformingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GasReformingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
