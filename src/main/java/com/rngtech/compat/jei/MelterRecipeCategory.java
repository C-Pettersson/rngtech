package com.rngtech.compat.jei;

import com.rngtech.content.recipe.MelterRecipe;
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

public final class MelterRecipeCategory implements IRecipeCategory<RecipeHolder<MelterRecipe>> {
    private static final int HEIGHT = 106;
    private static final int PRIMARY_X = 4;
    private static final int SECONDARY_X = 28;
    private static final int ITEM_Y = 14;
    private static final int FLUID_INPUT_X = 4;
    private static final int FLUID_OUTPUT_X = 142;
    private static final int FLUID_Y = 42;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    MelterRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.MELTER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<MelterRecipe>> getRecipeType() {
        return JeiRecipeTypes.MELTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.melter");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MelterRecipe> recipeHolder, IFocusGroup focuses) {
        MelterRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, PRIMARY_X, ITEM_Y)
                .addIngredients(recipe.primaryIngredient())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.primary_input")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, SECONDARY_X, ITEM_Y)
                .addIngredients(recipe.secondaryIngredient())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.secondary_input")));

        List<FluidStack> fluidInputs = Arrays.stream(recipe.fluidInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FLUID_INPUT_X, FLUID_Y)
                .setFluidRenderer(recipe.fluidInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, fluidInputs)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.fluid_input",
                        recipe.fluidInput().amount()
                )));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, FLUID_OUTPUT_X, FLUID_Y)
                .setFluidRenderer(recipe.fluidOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.outputFluid())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.fluid_output",
                        recipe.fluidOutput().getAmount()
                )));
    }

    @Override
    public void draw(
            RecipeHolder<MelterRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        MelterRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, FLUID_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.inputs", 24, 3);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.fluid.short", FLUID_INPUT_X + 8, 31);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.fluid.short", FLUID_OUTPUT_X + 8, 31);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()),
                66,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.heatGate(recipe.minimumTemperature()), 76, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.required_processing_level", recipe.requiredProcessingLevel()),
                86,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
