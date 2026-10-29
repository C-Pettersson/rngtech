package com.rngtech.compat.jei;

import com.rngtech.content.recipe.ToolDamageShapelessRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class ToolDamageCraftingCategoryExtension implements ICraftingCategoryExtension<ToolDamageShapelessRecipe> {
    @Override
    public void setRecipe(
            RecipeHolder<ToolDamageShapelessRecipe> recipeHolder,
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses
    ) {
        ToolDamageShapelessRecipe recipe = recipeHolder.value();
        CraftingTransferVariants variants = CraftingTransferVariants.forOpenMenu();
        List<List<ItemStack>> inputs = recipe.getIngredients().stream()
                .map(ingredient -> variants.stacks(ingredient, ingredient))
                .toList();
        craftingGridHelper.createAndSetOutputs(builder, List.of(recipe.getResultItem(null)));
        craftingGridHelper.createAndSetInputs(builder, inputs, 0, 0);
    }
}
