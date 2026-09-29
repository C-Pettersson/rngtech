package com.rngtech.compat.jei;

import com.rngtech.content.recipe.ComponentRecyclingOutput;
import com.rngtech.content.recipe.ComponentRecyclingRecipe;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
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

import java.util.List;

public final class ComponentRecyclingRecipeCategory implements IRecipeCategory<RecipeHolder<ComponentRecyclingRecipe>> {
    private static final int HEIGHT = 92;
    private static final int INPUT_X = 4;
    private static final int SLOT_Y = 20;
    private static final int[] OUTPUT_X = {112, 132, 148};
    private static final int OUTPUT_Y = 20;
    private static final int ARROW_X = 76;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    ComponentRecyclingRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.componentRecycler(ComponentRecyclerChassis.IRON).get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<ComponentRecyclingRecipe>> getRecipeType() {
        return JeiRecipeTypes.COMPONENT_RECYCLING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.component_recycling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ComponentRecyclingRecipe> recipeHolder, IFocusGroup focuses) {
        ComponentRecyclingRecipe recipe = recipeHolder.value();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.ingredient())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.input")));
        List<ComponentRecyclingOutput> outputs = recipe.outputs();
        for (int index = 0; index < Math.min(outputs.size(), OUTPUT_X.length); index++) {
            ComponentRecyclingOutput output = outputs.get(index);
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X[index], OUTPUT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, output.copyStack())
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("rngtech.processing.output"));
                        if (output.requiresFilter()) {
                            tooltip.add(Component.translatable("rngtech.jei.requires_recovery_filter"));
                        }
                    });
        }
    }

    @Override
    public void draw(
            RecipeHolder<ComponentRecyclingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        ComponentRecyclingRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 8);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", 136, 8);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()),
                46,
                JeiCategoryUi.TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.minimum_processing_level", recipe.minimumProcessingLevel()),
                56,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.output_slots", recipe.outputs().size()),
                66,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        if (recipe.outputs().stream().anyMatch(ComponentRecyclingOutput::requiresFilter)) {
            JeiCategoryUi.drawLine(
                    guiGraphics,
                    font,
                    Component.translatable("rngtech.jei.requires_recovery_filter"),
                    76,
                    JeiCategoryUi.MUTED_TEXT_COLOR
            );
        }
    }
}
