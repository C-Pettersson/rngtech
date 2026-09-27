package com.rngtech.compat.jei;

import com.rngtech.content.calibration.CalibrationRecipeResult;
import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.registry.ModItems;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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

public final class ResonanceCalibrationRecipeCategory implements IRecipeCategory<RecipeHolder<CalibrationRecipe>> {
    private static final int WIDTH = 166;
    private static final int HEIGHT = 84;
    private static final int INPUT_X = 4;
    private static final int PATTERN_X = 30;
    private static final int CATALYST_X = 56;
    private static final int STABILIZER_X = 82;
    private static final int SLOT_Y = 18;
    private static final int OUTPUT_X = 142;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int MUTED_TEXT_COLOR = 0xFF686868;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    ResonanceCalibrationRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.resonanceCalibrator(
                com.rngtech.content.calibration.ResonanceCalibratorChassis.IRON
        ).get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<RecipeHolder<CalibrationRecipe>> getRecipeType() {
        return JeiRecipeTypes.CALIBRATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("rngtech.jei.category.calibration");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return WIDTH;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CalibrationRecipe> recipeHolder, IFocusGroup focuses) {
        CalibrationRecipe recipe = recipeHolder.value();
        slot(builder, RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(recipe.ingredient())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.processing.input")));
        slot(builder, RecipeIngredientRole.INPUT, PATTERN_X, SLOT_Y)
                .addIngredients(recipe.pattern())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.calibration.pattern")));
        slot(builder, RecipeIngredientRole.INPUT, CATALYST_X, SLOT_Y)
                .addIngredients(recipe.catalyst())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.calibration.catalyst")));
        recipe.stabilizer().ifPresent(stabilizer -> slot(builder, RecipeIngredientRole.INPUT, STABILIZER_X, SLOT_Y)
                .addIngredients(stabilizer)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("rngtech.jei.calibration.stabilizer"))));

        slot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, representativeOutput(recipe))
                .addRichTooltipCallback((view, tooltip) -> {
                    CalibrationRecipeResult result = recipe.result();
                    tooltip.add(Component.translatable("rngtech.jei.calibration.output"));
                    tooltip.add(Component.translatable("rngtech.jei.calibration.family", Component.translatable(recipe.family().translationKey())));
                    tooltip.add(Component.translatable("rngtech.jei.calibration.stage", result.stage()));
                    tooltip.add(Component.translatable("rngtech.jei.calibration.minimum_stage", recipe.minimumStage()));
                    tooltip.add(Component.translatable(
                            "rngtech.jei.calibration.stability",
                            result.stability().min(),
                            result.stability().max()
                    ));
                    tooltip.add(Component.translatable(
                            "rngtech.jei.calibration.rp",
                            result.refinementPotential().min(),
                            result.refinementPotential().max()
                    ));
                });
    }

    @Override
    public void draw(
            RecipeHolder<CalibrationRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        CalibrationRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;
        arrow.draw(guiGraphics, 112, SLOT_Y + 1);
        drawLabel(guiGraphics, font, "rngtech.processing.input.tiny", INPUT_X + 8, 7);
        drawLabel(guiGraphics, font, "rngtech.calibration.pattern.tiny", PATTERN_X + 8, 7);
        drawLabel(guiGraphics, font, "rngtech.calibration.catalyst.tiny", CATALYST_X + 8, 7);
        drawLabel(guiGraphics, font, "rngtech.calibration.stabilizer.tiny", STABILIZER_X + 8, 7);
        drawLabel(guiGraphics, font, "rngtech.processing.output.tiny", OUTPUT_X + 8, 7);

        CalibrationRecipeResult result = recipe.result();
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.jei.calibration.family", Component.translatable(recipe.family().translationKey())),
                4,
                42,
                TEXT_COLOR,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.jei.calibration.stability", result.stability().min(), result.stability().max()),
                4,
                52,
                MUTED_TEXT_COLOR,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.jei.calibration.rp", result.refinementPotential().min(), result.refinementPotential().max()),
                4,
                62,
                MUTED_TEXT_COLOR,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.jei.calibration.cost", recipe.energy(), recipe.processingTicks()),
                4,
                72,
                MUTED_TEXT_COLOR,
                false
        );
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setStandardSlotBackground();
    }

    private static ItemStack representativeOutput(CalibrationRecipe recipe) {
        return recipe.result().stack().copy();
    }

    private static void drawLabel(GuiGraphics guiGraphics, Font font, String translationKey, int centerX, int y) {
        Component label = Component.translatable(translationKey);
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, MUTED_TEXT_COLOR, false);
    }
}
