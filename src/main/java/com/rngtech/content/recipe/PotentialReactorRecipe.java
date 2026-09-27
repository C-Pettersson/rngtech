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

public record PotentialReactorRecipe(
        String group,
        Ingredient ingredient,
        int energy,
        int processingTicks,
        ItemStack residue,
        int minimumMaterialStage
)
        implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
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
        ingredients.add(ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.POTENTIAL_REACTOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.POTENTIAL_REACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.POTENTIAL_REACTOR_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<PotentialReactorRecipe> {
        private static final MapCodec<PotentialReactorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(PotentialReactorRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PotentialReactorRecipe::ingredient),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(PotentialReactorRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(120)
                                .forGetter(PotentialReactorRecipe::processingTicks),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("residue", ItemStack.EMPTY)
                                .forGetter(PotentialReactorRecipe::residue),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_material_stage")
                                .orElse(1)
                                .forGetter(PotentialReactorRecipe::minimumMaterialStage)
                )
                .apply(instance, PotentialReactorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PotentialReactorRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                PotentialReactorRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                PotentialReactorRecipe::ingredient,
                ByteBufCodecs.VAR_INT,
                PotentialReactorRecipe::energy,
                ByteBufCodecs.VAR_INT,
                PotentialReactorRecipe::processingTicks,
                ItemStack.OPTIONAL_STREAM_CODEC,
                PotentialReactorRecipe::residue,
                ByteBufCodecs.VAR_INT,
                PotentialReactorRecipe::minimumMaterialStage,
                PotentialReactorRecipe::new
        );

        @Override
        public MapCodec<PotentialReactorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PotentialReactorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
