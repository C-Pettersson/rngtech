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

public record GasCombustionRecipe(
        String group,
        SizedFluidIngredient gasInput,
        long energy,
        int processingTicks,
        FluidStack exhaustOutput
)
        implements Recipe<GasCombustionRecipeInput> {
    public FluidStack exhaustFluid() {
        return exhaustOutput.copy();
    }

    @Override
    public boolean matches(GasCombustionRecipeInput input, Level level) {
        return gasInput.test(input.gas());
    }

    @Override
    public ItemStack assemble(GasCombustionRecipeInput input, HolderLookup.Provider registries) {
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
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.SYNGAS_COMBUSTOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GAS_COMBUSTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.GAS_COMBUSTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GasCombustionRecipe> {
        private static final MapCodec<GasCombustionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(GasCombustionRecipe::group),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("gas_input").forGetter(GasCombustionRecipe::gasInput),
                        Codec.LONG.fieldOf("energy").forGetter(GasCombustionRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(GasCombustionRecipe::processingTicks),
                        FluidStack.CODEC.fieldOf("exhaust_output").forGetter(GasCombustionRecipe::exhaustOutput)
                )
                .apply(instance, GasCombustionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GasCombustionRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public GasCombustionRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new GasCombustionRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_LONG.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, GasCombustionRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.gasInput);
                        ByteBufCodecs.VAR_LONG.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.exhaustOutput);
                    }
                };

        @Override
        public MapCodec<GasCombustionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GasCombustionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
