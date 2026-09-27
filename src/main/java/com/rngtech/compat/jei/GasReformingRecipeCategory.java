package com.rngtech.compat.jei;

import com.rngtech.content.recipe.GasReformingRecipe;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

public final class GasReformingRecipeCategory implements IRecipeCategory<RecipeHolder<GasReformingRecipe>> {
    private static final int HEIGHT = 96;
    private static final int CATALYST_X = 4;
    private static final int GAS_X = 32;
    private static final int WATER_X = 58;
    private static final int HYDROGEN_X = 124;
    private static final int CARBON_MONOXIDE_X = 146;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 84;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    GasReformingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.STEAM_METHANE_REFORMER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<GasReformingRecipe>> getRecipeType() {
        return JeiRecipeTypes.GAS_REFORMING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.gas_reforming");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GasReformingRecipe> recipeHolder, IFocusGroup focuses) {
        GasReformingRecipe recipe = recipeHolder.value();
        List<FluidStack> gases = Arrays.stream(recipe.gasInput().getFluids()).map(FluidStack::copy).toList();
        List<FluidStack> water = Arrays.stream(recipe.waterInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y).addIngredients(recipe.catalyst());
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, GAS_X, SLOT_Y)
                .setFluidRenderer(recipe.gasInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, gases);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, WATER_X, SLOT_Y)
                .setFluidRenderer(recipe.waterInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, water);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, HYDROGEN_X, SLOT_Y)
                .setFluidRenderer(recipe.hydrogenOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.hydrogenFluid());
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, CARBON_MONOXIDE_X, SLOT_Y)
                .setFluidRenderer(recipe.carbonMonoxideOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.carbonMonoxideFluid());
    }

    @Override
    public void draw(
            RecipeHolder<GasReformingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        GasReformingRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()), 50, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.catalyst_stage", recipe.minimumCatalystStage()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.hydrogen_and_carbon_monoxide"), 74, JeiCategoryUi.MUTED_TEXT_COLOR);
    }
}
