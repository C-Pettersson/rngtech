package com.rngtech.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class JeiCategoryUi {
    static final int WIDTH = 166;
    static final int TEXT_COLOR = 0xFF404040;
    static final int MUTED_TEXT_COLOR = 0xFF686868;

    private JeiCategoryUi() {
    }

    static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setStandardSlotBackground();
    }

    static void drawLabel(GuiGraphics guiGraphics, Font font, String translationKey, int centerX, int y) {
        Component label = Component.translatable(translationKey);
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, MUTED_TEXT_COLOR, false);
    }

    static void drawLine(GuiGraphics guiGraphics, Font font, Component component, int y, int color) {
        drawClipped(guiGraphics, font, component, 4, y, WIDTH - 8, color);
    }

    static void drawClipped(
            GuiGraphics guiGraphics,
            Font font,
            Component component,
            int x,
            int y,
            int width,
            int color
    ) {
        guiGraphics.drawString(font, Component.literal(font.plainSubstrByWidth(component.getString(), width)), x, y, color, false);
    }

    static Component ticks(int ticks) {
        return Component.translatable("rngtech.jei.ticks", ticks);
    }

    static Component energyCost(int energy, int ticks) {
        return Component.translatable("rngtech.jei.energy_cost_time", energy, ticks);
    }

    static Component energyCost(int energy, int ticks, double multiplier) {
        return multiplier <= 1.000001D
                ? energyCost(energy, ticks)
                : Component.translatable("rngtech.jei.energy_cost_time_multiplier", energy, ticks, compactDouble(multiplier));
    }

    static Component energyGenerated(int energy, int ticks) {
        return Component.translatable("rngtech.jei.energy_generated_time", energy, ticks);
    }

    static Component energyGenerated(long energy, int ticks) {
        return Component.translatable("rngtech.jei.energy_generated_time", energy, ticks);
    }

    static Component stageGate(int stage) {
        return stage <= 0
                ? Component.translatable("rngtech.jei.no_stage_gate")
                : Component.translatable("rngtech.jei.minimum_stage", stage);
    }

    static Component heatGate(int temperature) {
        return temperature <= 0
                ? Component.translatable("rngtech.jei.no_heat_gate")
                : Component.translatable("rngtech.jei.minimum_heat", temperature);
    }

    static Component stabilityGate(double stability) {
        return stability <= 0.0
                ? Component.translatable("rngtech.jei.no_stability_gate")
                : Component.translatable("rngtech.jei.temperature_stability", percent(stability));
    }

    static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    static String compactDouble(double value) {
        return value == Math.rint(value) ? Integer.toString((int) Math.round(value)) : Double.toString(value);
    }
}
