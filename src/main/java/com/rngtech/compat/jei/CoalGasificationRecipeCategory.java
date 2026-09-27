package com.rngtech.compat.jei;

import com.rngtech.content.recipe.CoalGasificationRecipe;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.constants.VanillaTypes;
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

public final class CoalGasificationRecipeCategory implements IRecipeCategory<RecipeHolder<CoalGasificationRecipe>> {
    private static final int HEIGHT = 92;
    private static final int CARBON_X = 8;
    private static final int WATER_X = 36;
    private static final int GAS_X = 124;
    private static final int RESIDUE_X = 146;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 84;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    CoalGasificationRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.COAL_GASIFIER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CoalGasificationRecipe>> getRecipeType() {
        return JeiRecipeTypes.COAL_GASIFICATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.coal_gasification");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CoalGasificationRecipe> recipeHolder, IFocusGroup focuses) {
        CoalGasificationRecipe recipe = recipeHolder.value();
        List<FluidStack> water = Arrays.stream(recipe.waterInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, CARBON_X, SLOT_Y).addIngredients(recipe.carbonInput());
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, WATER_X, SLOT_Y)
                .setFluidRenderer(recipe.waterInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, water);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, GAS_X, SLOT_Y)
                .setFluidRenderer(recipe.output().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.outputFluid());
        if (!recipe.residue().isEmpty()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, RESIDUE_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.residue().copy());
        }
    }

    @Override
    public void draw(
            RecipeHolder<CoalGasificationRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        CoalGasificationRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()), 50, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.heat_core_stage", recipe.minimumHeatCoreStage()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
