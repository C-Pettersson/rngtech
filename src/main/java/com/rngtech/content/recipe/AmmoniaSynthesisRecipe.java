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

public record AmmoniaSynthesisRecipe(
        String group,
        Ingredient catalyst,
        SizedFluidIngredient nitrogenInput,
        SizedFluidIngredient hydrogenInput,
        FluidStack output,
        int energy,
        int processingTicks,
        int minimumCatalystStage
)
        implements Recipe<AmmoniaSynthesisRecipeInput> {
    public FluidStack outputFluid() {
        return output.copy();
    }

    @Override
    public boolean matches(AmmoniaSynthesisRecipeInput input, Level level) {
        return catalyst.test(input.catalyst())
                && nitrogenInput.test(input.nitrogen())
                && hydrogenInput.test(input.hydrogen());
    }

    @Override
    public ItemStack assemble(AmmoniaSynthesisRecipeInput input, HolderLookup.Provider registries) {
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
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.AMMONIA_SYNTHESIZER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.AMMONIA_SYNTHESIS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.AMMONIA_SYNTHESIS_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AmmoniaSynthesisRecipe> {
        private static final MapCodec<AmmoniaSynthesisRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(AmmoniaSynthesisRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(AmmoniaSynthesisRecipe::catalyst),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("nitrogen_input").forGetter(AmmoniaSynthesisRecipe::nitrogenInput),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("hydrogen_input").forGetter(AmmoniaSynthesisRecipe::hydrogenInput),
                        FluidStack.CODEC.fieldOf("output").forGetter(AmmoniaSynthesisRecipe::output),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(AmmoniaSynthesisRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(400)
                                .forGetter(AmmoniaSynthesisRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_catalyst_stage")
                                .orElse(6)
                                .forGetter(AmmoniaSynthesisRecipe::minimumCatalystStage)
                )
                .apply(instance, AmmoniaSynthesisRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AmmoniaSynthesisRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public AmmoniaSynthesisRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new AmmoniaSynthesisRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                FluidStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, AmmoniaSynthesisRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.nitrogenInput);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.hydrogenInput);
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.output);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumCatalystStage);
                    }
                };

        @Override
        public MapCodec<AmmoniaSynthesisRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AmmoniaSynthesisRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
