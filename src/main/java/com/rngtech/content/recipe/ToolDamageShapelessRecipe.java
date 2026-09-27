package com.rngtech.content.recipe;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record ToolDamageShapelessRecipe(
        String group,
        CraftingBookCategory category,
        ItemStack result,
        NonNullList<Ingredient> ingredients,
        Ingredient tool,
        int toolDamage
)
        implements CraftingRecipe {
    private static final int MAX_CRAFTING_GRID_INGREDIENTS = 9;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        List<ItemStack> nonEmptyItems = new ArrayList<>(input.ingredientCount());
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                nonEmptyItems.add(stack);
            }
        }
        List<Predicate<ItemStack>> ingredientTests = ingredients.stream()
                .<Predicate<ItemStack>>map(ingredient ->
                        stack -> ToolDamageRecipeSupport.ingredientMatchesIgnoringToolDamage(ingredient, tool, stack))
                .toList();
        return RecipeMatcher.findMatches(nonEmptyItems, ingredientTests) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
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

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
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
        return ModRecipes.TOOL_DAMAGE_SHAPELESS_SERIALIZER.get();
    }

    @Override
    public boolean isIncomplete() {
        return ingredients.isEmpty() || ingredients.stream().anyMatch(Ingredient::hasNoItems);
    }

    public static class Serializer implements RecipeSerializer<ToolDamageShapelessRecipe> {
        private static final MapCodec<ToolDamageShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ToolDamageShapelessRecipe::group),
                        CraftingBookCategory.CODEC
                                .fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(ToolDamageShapelessRecipe::category),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ToolDamageShapelessRecipe::result),
                        Ingredient.CODEC_NONEMPTY
                                .listOf()
                                .fieldOf("ingredients")
                                .flatXmap(Serializer::toNonNullList, DataResult::success)
                                .forGetter(ToolDamageShapelessRecipe::ingredients),
                        Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(ToolDamageShapelessRecipe::tool),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("tool_damage")
                                .orElse(1)
                                .forGetter(ToolDamageShapelessRecipe::toolDamage)
                )
                .apply(instance, ToolDamageShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<ToolDamageShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolDamageShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static DataResult<NonNullList<Ingredient>> toNonNullList(List<Ingredient> ingredients) {
            Ingredient[] ingredientArray = ingredients.toArray(Ingredient[]::new);
            if (ingredientArray.length == 0) {
                return DataResult.error(() -> "No ingredients for shapeless tool-damage recipe");
            }
            if (ingredientArray.length > MAX_CRAFTING_GRID_INGREDIENTS) {
                return DataResult.error(() -> "Too many ingredients for shapeless tool-damage recipe. The maximum is: %s"
                        .formatted(MAX_CRAFTING_GRID_INGREDIENTS));
            }
            return DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredientArray));
        }

        private static ToolDamageShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
            CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int ingredientCount = ByteBufCodecs.VAR_INT.decode(buffer);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            Ingredient tool = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int toolDamage = ByteBufCodecs.VAR_INT.decode(buffer);
            return new ToolDamageShapelessRecipe(group, category, result, ingredients, tool, toolDamage);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, ToolDamageShapelessRecipe recipe) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.group);
            CraftingBookCategory.STREAM_CODEC.encode(buffer, recipe.category);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            ByteBufCodecs.VAR_INT.encode(buffer, recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.tool);
            ByteBufCodecs.VAR_INT.encode(buffer, recipe.toolDamage);
        }
    }
}
