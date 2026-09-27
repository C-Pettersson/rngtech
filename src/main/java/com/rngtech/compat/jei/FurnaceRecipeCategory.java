package com.rngtech.compat.jei;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.recipe.ProcessingEnergyScaling;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.world.item.crafting.RecipeHolder;

public final class FurnaceRecipeCategory implements IRecipeCategory<RecipeHolder<FurnaceRecipe>> {
    private static final int HEIGHT = 116;
    private static final int INPUT_X = 4;
    private static final int OUTPUT_X = 142;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    FurnaceRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.FURNACE.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<FurnaceRecipe>> getRecipeType() {
        return JeiRecipeTypes.FURNACE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<FurnaceRecipe> recipeHolder, IFocusGroup focuses) {
        FurnaceRecipe recipe = recipeHolder.value();
        var inputSlot = JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y);
        if (recipe.malformedMaterial().isBlank()) {
            inputSlot.addIngredients(recipe.ingredient());
        } else {
            inputSlot.addIngredient(VanillaTypes.ITEM_STACK, MalformedIngotItem.create(recipe.malformedMaterial()));
        }
        inputSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.input")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.outputStack())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.processing.output"));
                    if (!recipe.allowsBonusOutput()) {
                        tooltip.add(Component.translatable("rngtech.jei.output_bonuses_disabled"));
                    }
                });
    }

    @Override
    public void draw(
            RecipeHolder<FurnaceRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        FurnaceRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.ticks(recipe.processingTicks()), 42, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(
                        ProcessingEnergyScaling.furnaceEnergy(recipe),
                        recipe.processingTicks(),
                        ProcessingEnergyScaling.furnaceEnergyMultiplier(recipe)
                ),
                52,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.experience", JeiCategoryUi.compactDouble(recipe.experience())),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.heatGate(recipe.minimumTemperature()), 72, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.stabilityGate(recipe.requiredTemperatureStability()),
                82,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        int nextLineY = 92;
        if (recipe.machineXp() > 0) {
            JeiCategoryUi.drawLine(
                    guiGraphics,
                    font,
                    Component.translatable("rngtech.jei.machine_xp", recipe.machineXp(), recipe.machineXpBand()),
                    nextLineY,
                    JeiCategoryUi.MUTED_TEXT_COLOR
            );
            nextLineY += 10;
        }
        if (!recipe.allowsBonusOutput()) {
            JeiCategoryUi.drawLine(
                    guiGraphics,
                    font,
                    Component.translatable("rngtech.jei.output_bonuses_disabled"),
                    nextLineY,
                    JeiCategoryUi.MUTED_TEXT_COLOR
            );
        }
    }
}
