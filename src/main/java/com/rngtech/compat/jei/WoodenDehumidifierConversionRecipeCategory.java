package com.rngtech.compat.jei;

import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipe;
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

public final class WoodenDehumidifierConversionRecipeCategory
        implements IRecipeCategory<RecipeHolder<WoodenDehumidifierConversionRecipe>> {
    private static final int HEIGHT = 86;
    private static final int INPUT_X = 18;
    private static final int WATER_X = 48;
    private static final int OUTPUT_X = 132;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 88;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    WoodenDehumidifierConversionRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.WOODEN_DEHUMIDIFIER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<WoodenDehumidifierConversionRecipe>> getRecipeType() {
        return JeiRecipeTypes.WOODEN_DEHUMIDIFIER_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.wooden_dehumidifier_conversion");
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
            RecipeHolder<WoodenDehumidifierConversionRecipe> recipeHolder,
            IFocusGroup focuses
    ) {
        WoodenDehumidifierConversionRecipe recipe = recipeHolder.value();
        List<FluidStack> waterInputs = Arrays.stream(recipe.waterInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.ingredient())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.wooden_dehumidifier.ingredient")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, WATER_X, SLOT_Y)
                .setFluidRenderer(recipe.waterInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, waterInputs)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.wooden_dehumidifier.water",
                        recipe.waterInput().amount()
                )));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .setFluidRenderer(recipe.fluidOutput().getAmount(), false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, recipe.outputFluid())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                        "rngtech.jei.wooden_dehumidifier.fluid_output",
                        recipe.fluidOutput().getAmount()
                )));
    }

    @Override
    public void draw(
            RecipeHolder<WoodenDehumidifierConversionRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        WoodenDehumidifierConversionRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 8);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.wooden_dehumidifier.water", WATER_X + 8, 8);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 8);
        JeiCategoryUi.drawLine(guiGraphics, font, Component.translatable("rngtech.jei.processing_ticks", recipe.processingTicks()), 52, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.wooden_dehumidifier.no_fe"),
                64,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
