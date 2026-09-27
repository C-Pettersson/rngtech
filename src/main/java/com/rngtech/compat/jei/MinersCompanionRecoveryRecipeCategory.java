package com.rngtech.compat.jei;

import com.rngtech.content.recipe.MinersCompanionRecoveryRecipe;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class MinersCompanionRecoveryRecipeCategory implements IRecipeCategory<RecipeHolder<MinersCompanionRecoveryRecipe>> {
    private static final int HEIGHT = 82;
    private static final int INPUT_X = 8;
    private static final int OUTPUT_X = 142;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    MinersCompanionRecoveryRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.MINERS_COMPANION.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<MinersCompanionRecoveryRecipe>> getRecipeType() {
        return JeiRecipeTypes.MINERS_COMPANION_RECOVERY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.miners_companion_recovery");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return JeiCategoryUi.WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MinersCompanionRecoveryRecipe> recipeHolder, IFocusGroup focuses) {
        MinersCompanionRecoveryRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.input())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.miners_companion.chewed")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.result().copy())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.miners_companion.recovered")));
    }

    @Override
    public void draw(
            RecipeHolder<MinersCompanionRecoveryRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        MinersCompanionRecoveryRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.miners_companion.chewed", INPUT_X + 4, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.miners_companion.recovered", OUTPUT_X, 7);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.miners_companion.base_chance", "1/5000"),
                44,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.miners_companion.minimum_filter_stage", recipe.minFilterStage()),
                54,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.miners_companion.minimum_processing_level", recipe.minProcessingLevel()),
                64,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.miners_companion.weight", recipe.weight()),
                74,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
