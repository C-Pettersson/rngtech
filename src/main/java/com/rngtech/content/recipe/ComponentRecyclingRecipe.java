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

import java.util.List;

public record ComponentRecyclingRecipe(
        String group,
        Ingredient ingredient,
        int minimumProcessingLevel,
        int processingTicks,
        int energy,
        List<ComponentRecyclingOutput> outputs,
        boolean allowsBonusOutput
)
        implements Recipe<SingleRecipeInput> {
    public List<ItemStack> outputStacks(boolean hasRecoveryFilter) {
        return outputs.stream()
                .filter(output -> !output.requiresFilter() || hasRecoveryFilter)
                .map(ComponentRecyclingOutput::copyStack)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copyStack();
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
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().stack();
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
        return new ItemStack(ModItems.componentRecycler(com.rngtech.content.recycling.ComponentRecyclerChassis.IRON).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPONENT_RECYCLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COMPONENT_RECYCLING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ComponentRecyclingRecipe> {
        private static final MapCodec<ComponentRecyclingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ComponentRecyclingRecipe::group),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ComponentRecyclingRecipe::ingredient),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("minimum_processing_level")
                                .orElse(1)
                                .forGetter(ComponentRecyclingRecipe::minimumProcessingLevel),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("processing_ticks")
                                .orElse(120)
                                .forGetter(ComponentRecyclingRecipe::processingTicks),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("energy")
                                .orElse(1200)
                                .forGetter(ComponentRecyclingRecipe::energy),
                        ComponentRecyclingOutput.CODEC.listOf()
                                .fieldOf("outputs")
                                .forGetter(ComponentRecyclingRecipe::outputs),
                        Codec.BOOL.fieldOf("bonus_output").orElse(true).forGetter(ComponentRecyclingRecipe::allowsBonusOutput)
                )
                .apply(instance, ComponentRecyclingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ComponentRecyclingRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public ComponentRecyclingRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new ComponentRecyclingRecipe(
                                ByteBufCodecs.STRING_UTF8.decode(buffer),
                                Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ByteBufCodecs.VAR_INT.decode(buffer),
                                ComponentRecyclingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer),
                                ByteBufCodecs.BOOL.decode(buffer)
                        );
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, ComponentRecyclingRecipe recipe) {
                        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.minimumProcessingLevel);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTicks);
                        ByteBufCodecs.VAR_INT.encode(buffer, recipe.energy);
                        ComponentRecyclingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, recipe.outputs);
                        ByteBufCodecs.BOOL.encode(buffer, recipe.allowsBonusOutput);
                    }
                };

        @Override
        public MapCodec<ComponentRecyclingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ComponentRecyclingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
