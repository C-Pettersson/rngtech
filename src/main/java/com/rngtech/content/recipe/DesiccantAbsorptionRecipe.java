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

public record DesiccantAbsorptionRecipe(
        Ingredient dryInput,
        ItemStack saturatedOutput,
        FluidStack waterOutput,
        int processingTicks,
        int minimumStage
)
        implements Recipe<DesiccantAbsorptionRecipeInput> {
    public DesiccantAbsorptionRecipe {
        saturatedOutput = saturatedOutput.copy();
        waterOutput = waterOutput.copy();
    }

    public ItemStack outputStack() {
        return saturatedOutput.copy();
    }

    public FluidStack outputFluid() {
        return waterOutput.copy();
    }

    @Override
    public boolean matches(DesiccantAbsorptionRecipeInput input, Level level) {
        return dryInput.test(input.dryInput());
    }

    @Override
    public ItemStack assemble(DesiccantAbsorptionRecipeInput input, HolderLookup.Provider registries) {
        return saturatedOutput.copy();
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
        return saturatedOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(dryInput);
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.SILICA_GEL_DEHUMIDIFIER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DESICCANT_ABSORPTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.DESICCANT_ABSORPTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<DesiccantAbsorptionRecipe> {
        private static final MapCodec<DesiccantAbsorptionRecipe> CODEC =
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                                Ingredient.CODEC_NONEMPTY.fieldOf("dry_input").forGetter(DesiccantAbsorptionRecipe::dryInput),
                                ItemStack.STRICT_CODEC.fieldOf("saturated_output").forGetter(DesiccantAbsorptionRecipe::saturatedOutput),
                                FluidStack.CODEC.fieldOf("water_output").forGetter(DesiccantAbsorptionRecipe::waterOutput),
                                Codec.intRange(1, Integer.MAX_VALUE)
                                        .fieldOf("processing_ticks")
                                        .forGetter(DesiccantAbsorptionRecipe::processingTicks),
                                Codec.intRange(0, Integer.MAX_VALUE)
                                        .fieldOf("minimum_stage")
                                        .orElse(0)
                                        .forGetter(DesiccantAbsorptionRecipe::minimumStage)
                        )
                        .apply(instance, DesiccantAbsorptionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, DesiccantAbsorptionRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public DesiccantAbsorptionRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new DesiccantAbsorptionRecipe(
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, DesiccantAbsorptionRecipe recipe) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.dryInput);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.saturatedOutput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.waterOutput);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumStage);
                    }
                };

        @Override
        public MapCodec<DesiccantAbsorptionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DesiccantAbsorptionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
