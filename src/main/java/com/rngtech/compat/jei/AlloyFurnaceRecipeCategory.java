package com.rngtech.compat.jei;

import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.recipe.AlloyFurnaceRecipe;
import com.rngtech.content.recipe.CountedIngredient;
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

public final class AlloyFurnaceRecipeCategory implements IRecipeCategory<RecipeHolder<AlloyFurnaceRecipe>> {
    private static final int HEIGHT = 126;
    private static final int[] INPUT_X = {4, 26, 48, 26};
    private static final int[] INPUT_Y = {24, 6, 24, 42};
    private static final int OUTPUT_X = 142;
    private static final int SLOT_Y = 24;
    private static final int ARROW_X = 88;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    AlloyFurnaceRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(
                new ItemStack(ModItems.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get())
        );
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<AlloyFurnaceRecipe>> getRecipeType() {
        return JeiRecipeTypes.ALLOY_FURNACE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.alloy_furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyFurnaceRecipe> recipeHolder, IFocusGroup focuses) {
        AlloyFurnaceRecipe recipe = recipeHolder.value();
        for (int index = 0; index < recipe.ingredients().size() && index < INPUT_X.length; index++) {
            CountedIngredient ingredient = recipe.ingredients().get(index);
            JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X[index], INPUT_Y[index])
                    .addIngredients(ingredient.ingredient())
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("rngtech.processing.input"));
                        tooltip.add(Component.translatable("rngtech.jei.ingredient_count", ingredient.count()));
                    });
        }
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.outputStack())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.output")));
    }

    @Override
    public void draw(
            RecipeHolder<AlloyFurnaceRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        AlloyFurnaceRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", 30, 66);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 13);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.alloy_mode", recipe.mode()), 76, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()),
                86,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.stageGate(recipe.minimumComponentStage()), 96, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.heatGate(recipe.minimumTemperature()), 106, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.stabilityGate(recipe.requiredTemperatureStability()),
                116,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
