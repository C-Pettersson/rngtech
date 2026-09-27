package com.rngtech.compat.jei;

import com.rngtech.content.recipe.CavitationRecipe;
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

public final class CavitationRecipeCategory implements IRecipeCategory<RecipeHolder<CavitationRecipe>> {
    private static final int HEIGHT = 86;
    private static final int ROTOR_X = 4;
    private static final int NOZZLE_X = 24;
    private static final int FLUID_X = 52;
    private static final int OUTPUT_X = 124;
    private static final int SLOT_Y = 20;
    private static final int ARROW_X = 86;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    CavitationRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(JeiCategoryUi.WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.CAVITATION_GENERATOR.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CavitationRecipe>> getRecipeType() {
        return JeiRecipeTypes.CAVITATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.cavitation");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CavitationRecipe> recipeHolder, IFocusGroup focuses) {
        CavitationRecipe recipe = recipeHolder.value();
        List<FluidStack> inputs = Arrays.stream(recipe.fluidInput().getFluids()).map(FluidStack::copy).toList();
        recipe.rotor().ifPresent(rotor -> JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, ROTOR_X, SLOT_Y)
                .addIngredients(rotor)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.cavitation.rotor"))));
        recipe.nozzle().ifPresent(nozzle -> JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, NOZZLE_X, SLOT_Y)
                .addIngredients(nozzle)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.cavitation.nozzle"))));
        JeiCategoryUi.slot(builder, RecipeIngredientRole.INPUT, FLUID_X, SLOT_Y)
                .setFluidRenderer(recipe.fluidInput().amount(), false, 16, 16)
                .addIngredients(NeoForgeTypes.FLUID_STACK, inputs)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.cavitation.fluid")));
        FluidStack output = recipe.fluidOutputStack();
        if (!output.isEmpty()) {
            JeiCategoryUi.slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                    .setFluidRenderer(output.getAmount(), false, 16, 16)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, output)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.cavitation.fluid_output")));
        }
    }

    @Override
    public void draw(
            RecipeHolder<CavitationRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        CavitationRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, ARROW_X, SLOT_Y + 1);
        JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.cavitation.fluid", FLUID_X + 8, 8);
        if (!recipe.fluidOutputStack().isEmpty()) {
            JeiCategoryUi.drawLabel(guiGraphics, font, "rngtech.jei.cavitation.fluid_output", OUTPUT_X + 8, 8);
        }
        JeiCategoryUi.drawLine(guiGraphics, font, JeiCategoryUi.energyGenerated(recipe.energy(), recipe.processingTicks()), 48, JeiCategoryUi.TEXT_COLOR);
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.cavitation.gates", recipe.minimumRotorStage(), recipe.pressureRating()),
                58,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
        JeiCategoryUi.drawLine(
                guiGraphics,
                font,
                Component.translatable("rngtech.jei.cavitation.strain_wear", recipe.heatStrain(), recipe.wear()),
                68,
                JeiCategoryUi.MUTED_TEXT_COLOR
        );
    }
}
