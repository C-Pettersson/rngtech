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
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** The Ammonia Synthesizer's second recipe: Ammonia and Carbon Exhaust into an item, such as Urea. */
public record UreaSynthesisRecipe(
        String group,
        Ingredient catalyst,
        SizedFluidIngredient ammoniaInput,
        SizedFluidIngredient carbonInput,
        ItemStack result,
        int energy,
        int processingTicks,
        int minimumCatalystStage
)
        implements Recipe<UreaSynthesisRecipeInput> {
    public ItemStack resultStack() {
        return result.copy();
    }

    @Override
    public boolean matches(UreaSynthesisRecipeInput input, Level level) {
        return catalyst.test(input.catalyst())
                && ammoniaInput.test(input.ammonia())
                && carbonInput.test(input.carbon());
    }

    @Override
    public ItemStack assemble(UreaSynthesisRecipeInput input, HolderLookup.Provider registries) {
        return resultStack();
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
        return resultStack();
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
        return new ItemStack(ModItems.AMMONIA_SYNTHESIZER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.UREA_SYNTHESIS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.UREA_SYNTHESIS_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<UreaSynthesisRecipe> {
        private static final MapCodec<UreaSynthesisRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(UreaSynthesisRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(UreaSynthesisRecipe::catalyst),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("ammonia_input").forGetter(UreaSynthesisRecipe::ammoniaInput),
                        SizedFluidIngredient.NESTED_CODEC.fieldOf("carbon_input").forGetter(UreaSynthesisRecipe::carbonInput),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(UreaSynthesisRecipe::result),
                        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("energy").forGetter(UreaSynthesisRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(200)
                                .forGetter(UreaSynthesisRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_catalyst_stage")
                                .orElse(6)
                                .forGetter(UreaSynthesisRecipe::minimumCatalystStage)
                )
                .apply(instance, UreaSynthesisRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, UreaSynthesisRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public UreaSynthesisRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new UreaSynthesisRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                SizedFluidIngredient.STREAM_CODEC.decode(buffer),
                                ItemStack.STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, UreaSynthesisRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.ammoniaInput);
                        SizedFluidIngredient.STREAM_CODEC.encode(buffer, recipe.carbonInput);
                        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumCatalystStage);
                    }
                };

        @Override
        public MapCodec<UreaSynthesisRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UreaSynthesisRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
