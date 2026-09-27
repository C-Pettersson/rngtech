package com.rngtech.content.recipe;

import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModTags;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CarbonExhaustBucketRecipe implements CraftingRecipe {
    private static final NonNullList<Ingredient> INGREDIENTS = NonNullList.of(
            Ingredient.EMPTY,
            Ingredient.of(Items.WATER_BUCKET),
            Ingredient.of(ModTags.Items.COMMON_COAL_DUSTS)
    );

    private final CraftingBookCategory category;

    public CarbonExhaustBucketRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasWater = false;
        boolean hasCoalDust = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.WATER_BUCKET) && !hasWater) {
                hasWater = true;
            } else if (stack.is(ModTags.Items.COMMON_COAL_DUSTS) && !hasCoalDust) {
                hasCoalDust = true;
            } else {
                return false;
            }
        }
        return hasWater && hasCoalDust;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.CARBON_EXHAUST_BUCKET.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.CARBON_EXHAUST_BUCKET.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return INGREDIENTS;
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
        return new ItemStack(ModItems.CARBON_EXHAUST_BUCKET.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CARBON_EXHAUST_BUCKET_SERIALIZER.get();
    }

    @Override
    public boolean isIncomplete() {
        return INGREDIENTS.stream().anyMatch(Ingredient::hasNoItems);
    }

    public static final class Serializer implements RecipeSerializer<CarbonExhaustBucketRecipe> {
        private static final MapCodec<CarbonExhaustBucketRecipe> CODEC = CraftingBookCategory.CODEC
                .fieldOf("category")
                .orElse(CraftingBookCategory.MISC)
                .xmap(CarbonExhaustBucketRecipe::new, CarbonExhaustBucketRecipe::category);

        private static final StreamCodec<RegistryFriendlyByteBuf, CarbonExhaustBucketRecipe> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public CarbonExhaustBucketRecipe decode(RegistryFriendlyByteBuf buffer) {
                        return new CarbonExhaustBucketRecipe(CraftingBookCategory.STREAM_CODEC.decode(buffer));
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CarbonExhaustBucketRecipe recipe) {
                        CraftingBookCategory.STREAM_CODEC.encode(buffer, recipe.category);
                    }
                };

        @Override
        public MapCodec<CarbonExhaustBucketRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CarbonExhaustBucketRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
