package com.rngtech.content.recipe;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class UnidentifiedTraitIdentifyRecipe implements CraftingRecipe {
    private final CraftingBookCategory category;

    public UnidentifiedTraitIdentifyRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack target = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!target.isEmpty() || !CraftedTraitOutputs.isUnidentified(stack)) {
                return false;
            }
            target = stack;
        }
        return !target.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (CraftedTraitOutputs.isUnidentified(stack)) {
                return CraftedTraitOutputs.prepareIdentification(stack.copyWithCount(1));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
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
        return "";
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.AFFIX_INJECTOR.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.UNIDENTIFIED_TRAIT_IDENTIFY_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    public static final class Serializer implements RecipeSerializer<UnidentifiedTraitIdentifyRecipe> {
        private static final MapCodec<UnidentifiedTraitIdentifyRecipe> CODEC = CraftingBookCategory.CODEC
                .fieldOf("category")
                .orElse(CraftingBookCategory.MISC)
                .xmap(UnidentifiedTraitIdentifyRecipe::new, UnidentifiedTraitIdentifyRecipe::category);

        private static final StreamCodec<RegistryFriendlyByteBuf, UnidentifiedTraitIdentifyRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public UnidentifiedTraitIdentifyRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new UnidentifiedTraitIdentifyRecipe(CraftingBookCategory.STREAM_CODEC.decode(buffer));
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, UnidentifiedTraitIdentifyRecipe recipe) {
                        CraftingBookCategory.STREAM_CODEC.encode(buffer, recipe.category);
                    }
                };

        @Override
        public MapCodec<UnidentifiedTraitIdentifyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UnidentifiedTraitIdentifyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
