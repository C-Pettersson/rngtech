package com.rngtech.compat.jei;

import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.registry.ModItems;

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

public final class SolidFuelBurnerRecipeCategory implements IRecipeCategory<SolidFuelBurnerJeiFuel> {
    private static final int HEIGHT = 64;
    private static final int FUEL_X = 4;
    private static final int FUEL_Y = 18;
    private static final int DETAIL_X = 28;
    private static final int DETAIL_WIDTH = JeiCategoryUi.WIDTH - DETAIL_X - 4;

    private final IDrawable background;
    private final IDrawable icon;

    SolidFuelBurnerRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.solidFuelBurner(SolidFuelBurnerChassis.STEEL).get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<SolidFuelBurnerJeiFuel> getRecipeType() {
        return JeiRecipeTypes.SOLID_FUEL_BURNING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.solid_fuel_burning");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SolidFuelBurnerJeiFuel recipe, IFocusGroup focuses) {
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FUEL_X, FUEL_Y)
                .addItemStacks(recipe.fuels())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.fuel")));
    }

    @Override
    public void draw(
            SolidFuelBurnerJeiFuel recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.fuel", FUEL_X + 4, 7);
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.solid_fuel_burning.tier", recipe.tier()),
                DETAIL_X,
                20,
                DETAIL_WIDTH,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable(
                        recipe.blockFuel()
                                ? "rngtech.jei.solid_fuel_burning.block_fuels"
                                : "rngtech.jei.solid_fuel_burning.item_fuels"
                ),
                DETAIL_X,
                30,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.solid_fuel_burning.burn_time"),
                44,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.solid_fuel_burning.output"),
                54,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
