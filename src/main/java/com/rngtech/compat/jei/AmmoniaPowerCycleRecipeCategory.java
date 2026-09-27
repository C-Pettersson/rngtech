package com.rngtech.compat.jei;

import com.rngtech.content.recipe.AmmoniaPowerCycleRecipe;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.constants.VanillaTypes;
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

public final class AmmoniaPowerCycleRecipeCategory implements IRecipeCategory<RecipeHolder<AmmoniaPowerCycleRecipe>> {
    private static final int HEIGHT = 92;
    private static final int MEMBRANE_X = 18;
    private static final int FLUID_X = 48;
    private static final int OUTPUT_X = 138;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    AmmoniaPowerCycleRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.AMMONIA_FUEL_CELL.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<AmmoniaPowerCycleRecipe>> getRecipeType() {
        return JeiRecipeTypes.AMMONIA_POWER_CYCLE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.ammonia_power_cycle");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AmmoniaPowerCycleRecipe> recipeHolder, IFocusGroup focuses) {
        AmmoniaPowerCycleRecipe recipe = recipeHolder.value();
        List<FluidStack> ammonia = Arrays.stream(recipe.ammoniaInput().getFluids()).map(FluidStack::copy).toList();
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, MEMBRANE_X, SLOT_Y).addIngredients(recipe.membrane());
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FLUID_X, SLOT_Y)
                .setFluidRenderer(recipe.ammoniaInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, ammonia);
        if (!recipe.residue().isEmpty()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.residue().copy());
        }
    }

    @Override
    public void draw(
            RecipeHolder<AmmoniaPowerCycleRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        AmmoniaPowerCycleRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyGenerated(recipe.energy(), recipe.processingTicks()), 50, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.ammonia.membrane_stage", recipe.minimumMembraneStage()),
                62,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
