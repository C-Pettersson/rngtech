package com.rngtech.content.recipe;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModTags;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public final class TraitShapedRecipe extends ShapedRecipe {
    public TraitShapedRecipe(
            String group,
            CraftingBookCategory category,
            ShapedRecipePattern pattern,
            ItemStack result,
            boolean showNotification
    ) {
        super(group, category, UnidentifiedTraitIngredient.wrapPattern(pattern), result, showNotification);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = CraftedTraitOutputs.seedIfMissing(super.assemble(input, registries), RandomSource.create());
        if (CraftedTraitOutputs.isUnidentified(result) && allPlatesAnnealed(input)) {
            result.set(ModDataComponents.ANNEALED.get(), true);
        }
        return result;
    }

    /** At least one plate, and every plate pressed under Hydrogen. */
    private static boolean allPlatesAnnealed(CraftingInput input) {
        boolean anyPlate = false;
        for (ItemStack stack : input.items()) {
            if (stack.is(ModTags.Items.PLATES)) {
                if (!CraftedTraitOutputs.isAnnealed(stack)) {
                    return false;
                }
                anyPlate = true;
            }
        }
        return anyPlate;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return CraftedTraitOutputs.seedIfMissing(super.getResultItem(registries).copy(), RandomSource.create());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TRAIT_SHAPED_SERIALIZER.get();
    }

    private static TraitShapedRecipe fromShaped(ShapedRecipe recipe) {
        return new TraitShapedRecipe(
                recipe.getGroup(),
                recipe.category(),
                recipe.pattern,
                recipe.getResultItem(null).copy(),
                recipe.showNotification()
        );
    }

    public static final class Serializer implements RecipeSerializer<TraitShapedRecipe> {
        private static final MapCodec<TraitShapedRecipe> CODEC =
                ShapedRecipe.Serializer.CODEC.xmap(TraitShapedRecipe::fromShaped, recipe -> recipe);

        private static final StreamCodec<RegistryFriendlyByteBuf, TraitShapedRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static TraitShapedRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            boolean showNotification = buffer.readBoolean();
            return new TraitShapedRecipe(group, category, pattern, result, showNotification);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, TraitShapedRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
            buffer.writeBoolean(recipe.showNotification());
        }

        @Override
        public MapCodec<TraitShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TraitShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
