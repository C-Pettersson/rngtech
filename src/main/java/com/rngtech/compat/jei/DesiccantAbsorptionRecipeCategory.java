package com.rngtech.compat.jei;

import com.rngtech.content.recipe.DesiccantAbsorptionRecipe;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class DesiccantAbsorptionRecipeCategory implements IRecipeCategory<RecipeHolder<DesiccantAbsorptionRecipe>> {
    private static final int HEIGHT = 88;
    private static final int INPUT_X = 24;
    private static final int WATER_X = 106;
    private static final int OUTPUT_X = 138;
    private static final int SLOT_Y = 22;
    private static final int ARROW_X = 66;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    DesiccantAbsorptionRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.SILICA_GEL_DEHUMIDIFIER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<DesiccantAbsorptionRecipe>> getRecipeType() {
        return JeiRecipeTypes.DESICCANT_ABSORPTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.desiccant_absorption");
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
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            RecipeHolder<DesiccantAbsorptionRecipe> recipeHolder,
            IFocusGroup focuses
    ) {
        DesiccantAbsorptionRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.dryInput())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.desiccant_absorption.dry_input")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, WATER_X, SLOT_Y)
                .setFluidRenderer(recipe.waterOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.outputFluid())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.desiccant_absorption.water_output",
                        recipe.waterOutput().getAmount()
                )));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.outputStack())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.desiccant_absorption.saturated_output")));
    }

    @Override
    public void draw(
            RecipeHolder<DesiccantAbsorptionRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        DesiccantAbsorptionRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 9);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.silica_gel_dehumidifier.water", WATER_X + 8, 9);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 9);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.processing_ticks", recipe.processingTicks()), 54, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.stageGate(recipe.minimumStage()), 66, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.silica_gel_dehumidifier.no_fe"),
                78,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
