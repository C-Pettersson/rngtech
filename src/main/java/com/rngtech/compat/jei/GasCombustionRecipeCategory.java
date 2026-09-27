package com.rngtech.compat.jei;

import com.rngtech.content.recipe.GasCombustionRecipe;
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

public final class GasCombustionRecipeCategory implements IRecipeCategory<RecipeHolder<GasCombustionRecipe>> {
    private static final int HEIGHT = 82;
    private static final int GAS_X = 28;
    private static final int EXHAUST_X = 138;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 84;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    GasCombustionRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.SYNGAS_COMBUSTOR.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<GasCombustionRecipe>> getRecipeType() {
        return JeiRecipeTypes.GAS_COMBUSTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.gas_combustion");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GasCombustionRecipe> recipeHolder, IFocusGroup focuses) {
        GasCombustionRecipe recipe = recipeHolder.value();
        List<FluidStack> gases = Arrays.stream(recipe.gasInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, GAS_X, SLOT_Y)
                .setFluidRenderer(recipe.gasInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, gases);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, EXHAUST_X, SLOT_Y)
                .setFluidRenderer(recipe.exhaustOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.exhaustFluid());
    }

    @Override
    public void draw(
            RecipeHolder<GasCombustionRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        GasCombustionRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyGenerated(recipe.energy(), recipe.processingTicks()), 48, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.exhaust_output"), 60, JeiCategoryUi.MUTED_TEXT_COLOR);
    }
}
