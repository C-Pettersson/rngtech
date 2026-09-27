package com.rngtech.compat.jei;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recipe.CrusherRecipe;
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

import java.util.Arrays;
import java.util.List;

public final class CrusherRecipeCategory implements IRecipeCategory<RecipeHolder<CrusherRecipe>> {
    private static final int HEIGHT = 94;
    private static final int INPUT_X = 4;
    private static final int OUTPUT_X = 142;
    private static final int SLOT_Y = 18;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    CrusherRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.crusherChassis(CrusherChassisMaterial.WOODEN).get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CrusherRecipe>> getRecipeType() {
        return JeiRecipeTypes.CRUSHER;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.crusher");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrusherRecipe> recipeHolder, IFocusGroup focuses) {
        CrusherRecipe recipe = recipeHolder.value();
        var inputSlot = JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y);
        if (recipe.inputCount() == 1 && recipe.malformedMaterial().isBlank()) {
            inputSlot.addIngredients(recipe.ingredient());
        } else {
            inputSlot.addItemStacks(countedIngredientStacks(recipe));
        }
        inputSlot.addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("rngtech.processing.input"));
            if (recipe.inputCount() > 1) {
                tooltip.add(Component.translatable("rngtech.jei.ingredient_count", recipe.inputCount()));
            }
        });
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.outputStack(recipe.baseOutputCount()))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("rngtech.processing.output"));
                    tooltip.add(Component.translatable("rngtech.jei.output_count", recipe.baseOutputCount()));
                    if (!recipe.allowsBonusOutput()) {
                        tooltip.add(Component.translatable("rngtech.jei.output_bonuses_disabled"));
                    }
                });
    }

    @Override
    public void draw(
            RecipeHolder<CrusherRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        CrusherRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 7);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.ticks(recipe.processingTicks()), 42, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(
                        ProcessingEnergyScaling.crusherEnergy(recipe),
                        recipe.processingTicks(),
                        ProcessingEnergyScaling.crusherEnergyMultiplier(recipe)
                ),
                52,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.required_processing_level", recipe.requiredProcessingLevel()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.output_count", recipe.baseOutputCount()),
                72,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        if (!recipe.allowsBonusOutput()) {
            JeiCategoryUi.drawLine(
                    guiGraphics,
                    font,
                    Component.translatable("rngtech.jei.output_bonuses_disabled"),
                    82,
                    JeiCategoryUi.MUTED_TEXT_COLOR
            );
        }
    }

    private static List<ItemStack> countedIngredientStacks(CrusherRecipe recipe) {
        if (!recipe.malformedMaterial().isBlank()) {
            return List.of(MalformedIngotItem.create(recipe.malformedMaterial()).copyWithCount(recipe.inputCount()));
        }
        return Arrays.stream(recipe.ingredient().getItems())
                .map(stack -> stack.copyWithCount(recipe.inputCount()))
                .toList();
    }
}
