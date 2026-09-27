package com.rngtech.compat.jei;

import com.rngtech.content.recipe.MetalPressRecipe;
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

import java.util.Arrays;
import java.util.List;

public final class MetalPressRecipeCategory implements IRecipeCategory<RecipeHolder<MetalPressRecipe>> {
    private static final int HEIGHT = 110;
    private static final int INPUT_X = 4;
    private static final int MOLD_X = 34;
    private static final int OUTPUT_X = 142;
    private static final int FAILURE_X = 112;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 82;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    MetalPressRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.METAL_PRESS.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<MetalPressRecipe>> getRecipeType() {
        return JeiRecipeTypes.METAL_PRESS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.metal_press");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MetalPressRecipe> recipeHolder, IFocusGroup focuses) {
        MetalPressRecipe recipe = recipeHolder.value();
        var inputSlot = JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y);
        if (recipe.inputCount() == 1) {
            inputSlot.addIngredients(recipe.ingredient());
        } else {
            inputSlot.addItemStacks(countedIngredientStacks(recipe));
        }
        inputSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.input")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, MOLD_X, SLOT_Y)
                .addIngredients(recipe.mold())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.mold")));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.outputStack())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.output")));
        if (!recipe.failureOutput().isEmpty()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, FAILURE_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.failureStack())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.failure_output")));
        }
    }

    @Override
    public void draw(
            RecipeHolder<MetalPressRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        MetalPressRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.mold.short", MOLD_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.failure.short", FAILURE_X + 8, 7);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()),
                42,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.heatGate(recipe.minimumTemperature()), 52, JeiCategoryUi.MUTED_TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.target_heat", recipe.targetTemperature()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.safe_heat", recipe.safeMaximumTemperature()),
                72,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.stabilityGate(recipe.requiredTemperatureStability()),
                82,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.failure_output"),
                92,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }

    private static List<ItemStack> countedIngredientStacks(MetalPressRecipe recipe) {
        return Arrays.stream(recipe.ingredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.inputCount()))
                .toList();
    }
}
