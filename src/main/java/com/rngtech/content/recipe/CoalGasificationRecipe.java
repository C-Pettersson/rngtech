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

public record CoalGasificationRecipe(
        String group,
        Ingredient carbonInput,
        SizedFluidIngredient waterInput,
        FluidStack output,
        ItemStack residue,
        int energy,
        int processingTicks,
        int minimumHeatCoreStage
)
        implements Recipe<CoalGasificationRecipeInput> {
    public FluidStack outputFluid() {
        return output.copy();
    }

    @Override
    public boolean matches(CoalGasificationRecipeInput input, Level level) {
        return carbonInput.test(input.carbonInput()) && waterInput.test(input.water());
    }

    @Override
    public ItemStack assemble(CoalGasificationRecipeInput input, HolderLookup.Provider registries) {
        return residue.copy();
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
        return residue;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(carbonInput);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.COAL_GASIFIER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COAL_GASIFICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COAL_GASIFICATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CoalGasificationRecipe> {
        private static final MapCodec<CoalGasificationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(CoalGasificationRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("carbon_input").forGetter(CoalGasificationRecipe::carbonInput),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("water_input").forGetter(CoalGasificationRecipe::waterInput),
                        FluidStack.CODEC.fieldOf("output").forGetter(CoalGasificationRecipe::output),
                        ItemStack.STRICT_CODEC.fieldOf("residue").forGetter(CoalGasificationRecipe::residue),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(CoalGasificationRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(240)
                                .forGetter(CoalGasificationRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_heat_core_stage")
                                .orElse(5)
                                .forGetter(CoalGasificationRecipe::minimumHeatCoreStage)
                )
                .apply(instance, CoalGasificationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CoalGasificationRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CoalGasificationRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new CoalGasificationRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CoalGasificationRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.carbonInput);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.waterInput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.output);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.residue);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumHeatCoreStage);
                    }
                };

        @Override
        public MapCodec<CoalGasificationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CoalGasificationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
