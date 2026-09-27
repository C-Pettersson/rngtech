package com.rngtech.compat.jei;

import com.rngtech.content.recipe.VacuumCollapseRecipe;
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

public final class VacuumCollapseRecipeCategory implements IRecipeCategory<RecipeHolder<VacuumCollapseRecipe>> {
    private static final int HEIGHT = 86;
    private static final int INPUT_X = 20;
    private static final int OUTPUT_X = 138;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 82;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    VacuumCollapseRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.VACUUM_COLLAPSE_GENERATOR.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<VacuumCollapseRecipe>> getRecipeType() {
        return JeiRecipeTypes.VACUUM_COLLAPSE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.vacuum_collapse");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<VacuumCollapseRecipe> recipeHolder, IFocusGroup focuses) {
        VacuumCollapseRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y).addIngredients(recipe.ingredient());
        if (!recipe.residue().isEmpty()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.residue().copy());
        }
    }

    @Override
    public void draw(
            RecipeHolder<VacuumCollapseRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        VacuumCollapseRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyGenerated(recipe.energy(), recipe.processingTicks()), 48, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.vacuum.gates", recipe.minimumChamberStage(), JeiCategoryUi.percent(recipe.minimumStability())),
                58,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.vacuum.instability", JeiCategoryUi.percent(recipe.instability())),
                68,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
