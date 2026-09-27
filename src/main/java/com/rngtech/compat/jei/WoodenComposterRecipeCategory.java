package com.rngtech.compat.jei;

import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
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

public final class WoodenComposterRecipeCategory implements IRecipeCategory<WoodenComposterJeiRecipe> {
    private static final int HEIGHT = 76;
    private static final int INPUT_X = 18;
    private static final int OUTPUT_X = 132;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 72;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    WoodenComposterRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.WOODEN_COMPOSTER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<WoodenComposterJeiRecipe> getRecipeType() {
        return JeiRecipeTypes.WOODEN_COMPOSTER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.wooden_composter");
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
    public void setRecipe(IRecipeLayoutBuilder builder, WoodenComposterJeiRecipe recipe, IFocusGroup focuses) {
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addItemStacks(recipe.inputs())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.jei.wooden_composter.inputs"));
                    tooltip.add(Component.translatable(
                            "rngtech.jei.wooden_composter.consumes",
                            WoodenComposterBlockEntity.COMPOST_UNITS_PER_BATCH
                    ));
                    tooltip.add(Component.translatable(
                            "rngtech.jei.wooden_composter.prepared_inputs",
                            WoodenComposterBlockEntity.PREPARED_INPUT_COMPOST_UNITS
                    ));
                });
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.output())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.output")));
    }

    @Override
    public void draw(
            WoodenComposterJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 8);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 8);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.jei.wooden_composter.consumes",
                        WoodenComposterBlockEntity.COMPOST_UNITS_PER_BATCH
                ),
                48,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.wooden_composter.dry_time", WoodenComposterBlockEntity.DRY_COMPOST_TICKS),
                58,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable(
                        "rngtech.jei.wooden_composter.wet_time",
                        WoodenComposterBlockEntity.WET_COMPOST_TICKS,
                        WoodenComposterBlockEntity.WATER_PER_BATCH
                ),
                68,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
