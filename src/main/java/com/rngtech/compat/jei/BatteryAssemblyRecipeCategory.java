package com.rngtech.compat.jei;

import com.rngtech.content.recipe.BatteryAssemblyRecipe;
import com.rngtech.content.recipe.CountedIngredient;
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

public final class BatteryAssemblyRecipeCategory implements IRecipeCategory<RecipeHolder<BatteryAssemblyRecipe>> {
    private static final int HEIGHT = 106;
    private static final int[] INPUT_X = {4, 28, 52, 76};
    private static final int INPUT_Y = 14;
    private static final int FLUID_X = 16;
    private static final int FLUID_Y = 42;
    private static final int OUTPUT_X = 142;
    private static final int OUTPUT_Y = 28;
    private static final int ARROW_X = 106;
    private static final int ARROW_Y = 32;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    BatteryAssemblyRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BATTERY_ASSEMBLER.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<BatteryAssemblyRecipe>> getRecipeType() {
        return JeiRecipeTypes.BATTERY_ASSEMBLY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.battery_assembly");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BatteryAssemblyRecipe> recipeHolder, IFocusGroup focuses) {
        BatteryAssemblyRecipe recipe = recipeHolder.value();
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            CountedIngredient ingredient = recipe.ingredients().get(index);
            JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, INPUT_X[index], INPUT_Y)
                    .addItemStacks(countedIngredientStacks(ingredient))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("rngtech.processing.input"));
                        if (ingredient.count() > 1) {
                            tooltip.add(Component.translatable("rngtech.jei.ingredient_count", ingredient.count()));
                        }
                    });
        }
        recipe.fluidInput().ifPresent(fluid -> {
            List<FluidStack> fluids = Arrays.stream(fluid.getFluids()).map(FluidStack::copy).toList();
            JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FLUID_X, FLUID_Y)
                    .setFluidRenderer(fluid.amount(), false, 16, 16)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, fluids)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                            "rngtech.jei.fluid_input",
                            fluid.amount()
                    )));
        });
        JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.outputStack())
                .addRichTooltipCallback((view, tooltip) -> {
                    if (recipe.rollResultTraits()) {
                        tooltip.add(Component.translatable("rngtech.jei.battery_assembly.rolls_traits"));
                    }
                });
    }

    @Override
    public void draw(
            RecipeHolder<BatteryAssemblyRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        BatteryAssemblyRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.inputs", 42, 3);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 15);
        if (recipe.fluidAmount() > 0) {
            JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.fluid.short", FLUID_X + 8, 31);
        }
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                JeiCategoryUi.energyCost(recipe.energy(), recipe.processingTicks()),
                66,
                JeiCategoryUi.TEXT_COLOR
        );
        if (recipe.rollResultTraits()) {
            JeiCategoryUi.drawLine(
                    guiGraphics,
                    font,
                    Component.translatable("rngtech.jei.battery_assembly.rolls_traits"),
                    76,
                    JeiCategoryUi.MUTED_TEXT_COLOR
            );
        }
    }

    private static List<ItemStack> countedIngredientStacks(CountedIngredient ingredient) {
        return Arrays.stream(ingredient.ingredient().getItems())
                .map(stack -> stack.copyWithCount(ingredient.count()))
                .toList();
    }
}
