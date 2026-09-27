package com.rngtech.compat.jei;

import com.rngtech.content.recipe.AmmoniaSynthesisRecipe;
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

public final class AmmoniaSynthesisRecipeCategory implements IRecipeCategory<RecipeHolder<AmmoniaSynthesisRecipe>> {
    private static final int HEIGHT = 92;
    private static final int CATALYST_X = 8;
    private static final int NITROGEN_X = 34;
    private static final int HYDROGEN_X = 60;
    private static final int OUTPUT_X = 138;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 92;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    AmmoniaSynthesisRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.AMMONIA_SYNTHESIZER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<AmmoniaSynthesisRecipe>> getRecipeType() {
        return JeiRecipeTypes.AMMONIA_SYNTHESIS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.ammonia_synthesis");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AmmoniaSynthesisRecipe> recipeHolder, IFocusGroup focuses) {
        AmmoniaSynthesisRecipe recipe = recipeHolder.value();
        List<FluidStack> nitrogen = Arrays.stream(recipe.nitrogenInput().getFluids()).map(FluidStack::copy).toList();
        List<FluidStack> hydrogen = Arrays.stream(recipe.hydrogenInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y).addIngredients(recipe.catalyst());
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, NITROGEN_X, SLOT_Y)
                .setFluidRenderer(recipe.nitrogenInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, nitrogen);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, HYDROGEN_X, SLOT_Y)
                .setFluidRenderer(recipe.hydrogenInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, hydrogen);
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .setFluidRenderer(recipe.output().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.outputFluid());
    }

    @Override
    public void draw(
            RecipeHolder<AmmoniaSynthesisRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        AmmoniaSynthesisRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()), 50, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.ammonia.catalyst_stage", recipe.minimumCatalystStage()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
