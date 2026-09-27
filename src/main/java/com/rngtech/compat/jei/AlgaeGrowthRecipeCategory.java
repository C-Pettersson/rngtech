package com.rngtech.compat.jei;

import com.rngtech.content.recipe.AlgaeGrowthRecipe;
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

public final class AlgaeGrowthRecipeCategory implements IRecipeCategory<RecipeHolder<AlgaeGrowthRecipe>> {
    private static final int HEIGHT = 96;
    private static final int WATER_X = 12;
    private static final int CARBON_X = 42;
    private static final int FLUID_Y = 20;
    private static final int OUTPUT_X = 138;
    private static final int ARROW_X = 82;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    AlgaeGrowthRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ALGAE_PHOTOBIOREACTOR.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<AlgaeGrowthRecipe>> getRecipeType() {
        return JeiRecipeTypes.ALGAE_GROWTH;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.algae_growth");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlgaeGrowthRecipe> recipeHolder, IFocusGroup focuses) {
        AlgaeGrowthRecipe recipe = recipeHolder.value();
        List<FluidStack> waterInputs = Arrays.stream(recipe.waterInput().getFluids()).map(FluidStack::copy).toList();
        List<FluidStack> carbonInputs = Arrays.stream(recipe.carbonInput().getFluids()).map(FluidStack::copy).toList();

        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, WATER_X, FLUID_Y)
                .setFluidRenderer(recipe.waterInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, waterInputs)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.algae_growth.water",
                        recipe.waterInput().amount()
                )));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, CARBON_X, FLUID_Y)
                .setFluidRenderer(recipe.carbonInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, carbonInputs)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.algae_growth.carbon",
                        recipe.carbonInput().amount()
                )));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, FLUID_Y).addItemStack(recipe.output());
    }

    @Override
    public void draw(
            RecipeHolder<AlgaeGrowthRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        AlgaeGrowthRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, FLUID_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.algae_photobioreactor.water", WATER_X + 8, 8);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.algae_photobioreactor.carbon", CARBON_X + 8, 8);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.processing_ticks", recipe.processingTicks()), 58, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.algae_growth.light", recipe.minimumLight()),
                68,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.algae_growth.modifiers"),
                80,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
