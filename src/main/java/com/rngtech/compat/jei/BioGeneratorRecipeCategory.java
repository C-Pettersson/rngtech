package com.rngtech.compat.jei;

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

public final class BioGeneratorRecipeCategory implements IRecipeCategory<BioGeneratorJeiFuel> {
    private static final int HEIGHT = 68;
    private static final int FUEL_X = 4;
    private static final int FUEL_Y = 20;
    private static final int DETAIL_X = 28;
    private static final int DETAIL_WIDTH = JeiCategoryUi.WIDTH - DETAIL_X - 4;

    private final IDrawable background;
    private final IDrawable icon;

    BioGeneratorRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIO_GENERATOR.get()));
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<BioGeneratorJeiFuel> getRecipeType() {
        return JeiRecipeTypes.BIO_GENERATOR;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.bio_generator");
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
    public void setRecipe(IRecipeLayoutBuilder builder, BioGeneratorJeiFuel recipe, IFocusGroup focuses) {
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FUEL_X, FUEL_Y)
                .addItemStacks(recipe.fuels())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.processing.fuel"));
                    tooltip.add(recipe.family());
                    tooltip.add(Component.translatable("rngtech.jei.bio_generator.base_energy", recipe.baseEnergy()));
                });
    }

    @Override
    public void draw(
            BioGeneratorJeiFuel recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.fuel", FUEL_X + 9, 9);
        JeiCategoryUi.drawClipped(guiGraphics, font, recipe.family(), DETAIL_X, 20, DETAIL_WIDTH, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.bio_generator.base_energy", recipe.baseEnergy()),
                DETAIL_X,
                32,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawClipped(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.bio_generator.base_burn_time", recipe.baseBurnTicks()),
                DETAIL_X,
                43,
                DETAIL_WIDTH,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.bio_generator.modifiers"),
                58,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
