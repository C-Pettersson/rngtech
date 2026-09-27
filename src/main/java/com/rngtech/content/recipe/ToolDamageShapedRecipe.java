package com.rngtech.content.recipe;

import com.rngtech.content.item.ModularToolItem;
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
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

public record ToolDamageShapedRecipe(
        String group,
        CraftingBookCategory category,
        ShapedRecipePattern pattern,
        ItemStack result,
        Ingredient tool,
        int toolDamage
)
        implements CraftingRecipe {
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.ingredientCount() == ingredientCount()
                && input.width() == pattern.width()
                && input.height() == pattern.height()
                && (matchesPattern(input, false) || matchesPattern(input, true));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < remaining.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (ToolDamageRecipeSupport.toolMatchesIgnoringDamage(tool, stack)) {
                remaining.set(slot, damagedTool(stack));
            } else if (stack.getItem().hasCraftingRemainingItem()) {
                remaining.set(slot, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            }
        }
        return remaining;
    }

    private ItemStack damagedTool(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return stack.copyWithCount(1);
        }
        ItemStack remainder = stack.copyWithCount(1);
        if (ModularToolItem.isModularTool(remainder)) {
            ModularToolItem.damageWithoutBreaking(remainder, toolDamage);
            return remainder;
        }
        int nextDamage = remainder.getDamageValue() + toolDamage;
        if (nextDamage >= remainder.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        remainder.setDamageValue(nextDamage);
        return remainder;
    }

    private int ingredientCount() {
        int count = 0;
        for (Ingredient ingredient : pattern.ingredients()) {
            if (!ingredient.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesPattern(CraftingInput input, boolean mirrored) {
        for (int row = 0; row < pattern.height(); row++) {
            for (int column = 0; column < pattern.width(); column++) {
                int ingredientColumn = mirrored ? pattern.width() - column - 1 : column;
                Ingredient ingredient = pattern.ingredients().get(ingredientColumn + row * pattern.width());
                if (!ToolDamageRecipeSupport.ingredientMatchesIgnoringToolDamage(
                        ingredient, tool, input.getItem(column, row))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return pattern.ingredients();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.FORMING_HAMMER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TOOL_DAMAGE_SHAPED_SERIALIZER.get();
    }

    @Override
    public boolean isIncomplete() {
        NonNullList<Ingredient> ingredients = getIngredients();
        return ingredients.isEmpty() || ingredients.stream().anyMatch(Ingredient::hasNoItems);
    }

    public static class Serializer implements RecipeSerializer<ToolDamageShapedRecipe> {
        private static final MapCodec<ToolDamageShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ToolDamageShapedRecipe::group),
                        CraftingBookCategory.CODEC
                                .fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(ToolDamageShapedRecipe::category),
                        ShapedRecipePattern.MAP_CODEC.forGetter(ToolDamageShapedRecipe::pattern),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ToolDamageShapedRecipe::result),
                        Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(ToolDamageShapedRecipe::tool),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("tool_damage")
                                .orElse(1)
                                .forGetter(ToolDamageShapedRecipe::toolDamage)
                )
                .apply(instance, ToolDamageShapedRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapedRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        ToolDamageShapedRecipe::group,
                        CraftingBookCategory.STREAM_CODEC,
                        ToolDamageShapedRecipe::category,
                        ShapedRecipePattern.STREAM_CODEC,
                        ToolDamageShapedRecipe::pattern,
                        ItemStack.STREAM_CODEC,
                        ToolDamageShapedRecipe::result,
                        Ingredient.CONTENTS_STREAM_CODEC,
                        ToolDamageShapedRecipe::tool,
                        ByteBufCodecs.VAR_INT,
                        ToolDamageShapedRecipe::toolDamage,
                        ToolDamageShapedRecipe::new
                );

        @Override
        public MapCodec<ToolDamageShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapedRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
