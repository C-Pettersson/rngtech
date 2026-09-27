package com.rngtech.compat.jei;

import com.rngtech.content.recipe.CorrosionCellRecipe;
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

public final class CorrosionCellRecipeCategory implements IRecipeCategory<RecipeHolder<CorrosionCellRecipe>> {
    private static final int HEIGHT = 84;
    private static final int PLATE_X = 4;
    private static final int ELECTROLYTE_X = 28;
    private static final int OUTPUT_X = 142;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    CorrosionCellRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.CORROSION_CELL.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CorrosionCellRecipe>> getRecipeType() {
        return JeiRecipeTypes.CORROSION_CELL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.corrosion_cell");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CorrosionCellRecipe> recipeHolder, IFocusGroup focuses) {
        CorrosionCellRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, PLATE_X, SLOT_Y)
                .addIngredients(recipe.plate())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.corrosion_cell.plate")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, ELECTROLYTE_X, SLOT_Y)
                .addIngredients(recipe.electrolyte())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.corrosion_cell.electrolyte")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.residue().copy())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.residue")));
    }

    @Override
    public void draw(
            RecipeHolder<CorrosionCellRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        CorrosionCellRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.corrosion_cell.plate", PLATE_X + 1, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.corrosion_cell.electrolyte.short", ELECTROLYTE_X + 4, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyGenerated(recipe.energy(), recipe.processingTicks()),
                42,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.minimum_material_stage", recipe.minimumMaterialStage()),
                52,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.residue"),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
