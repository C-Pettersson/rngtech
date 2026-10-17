package com.rngtech.client.screen;

import com.rngtech.content.blockentity.InertAtmosphere;
import com.rngtech.content.blockentity.MetalPressBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The heated machines' Nitrogen tank: a thin vertical gauge with the inert atmosphere's effect on hover. */
final class NitrogenGauge {
    static final int WIDTH = 10;
    static final int HEIGHT = 50;
    private static final int TRACK = 0xFF5F5F5F;
    private static final int NITROGEN = 0xFF8BA4C9;
    private static final int ACTIVE_RIM = 0xFFB9D3F2;
    private static final int HYDROGEN = 0xFFD8E6F2;

    private final int x;
    private final int y;

    NitrogenGauge(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void render(GuiGraphics guiGraphics, int leftPos, int topPos, int amount, int use) {
        render(guiGraphics, leftPos, topPos, amount, use, false);
    }

    void render(GuiGraphics guiGraphics, int leftPos, int topPos, int amount, int use, boolean hydrogen) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + WIDTH, top + HEIGHT, use > 0 ? ACTIVE_RIM : TRACK);
        guiGraphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, TRACK);
        int fill = Math.round((HEIGHT - 2) * Math.min(1.0F, amount / (float) InertAtmosphere.CAPACITY));
        guiGraphics.fill(left + 1, top + HEIGHT - 1 - fill, left + WIDTH - 1, top + HEIGHT - 1, hydrogen ? HYDROGEN : NITROGEN);
    }

    boolean isOver(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + WIDTH && mouseY >= topPos + y && mouseY < topPos + y + HEIGHT;
    }

    /** Renders the tooltip when hovered; returns whether it did. */
    boolean renderTooltip(GuiGraphics guiGraphics, Font font, int leftPos, int topPos, int mouseX, int mouseY, int amount, int use, int bonusPercent) {
        if (!isOver(mouseX, mouseY, leftPos, topPos)) {
            return false;
        }
        guiGraphics.renderComponentTooltip(font, lines(amount, use, bonusPercent), mouseX, mouseY);
        return true;
    }

    /** A Metal Press with an annealing node: Hydrogen anneals what its node names; Nitrogen works as on any heated machine. */
    boolean renderTooltip(GuiGraphics guiGraphics, Font font, int leftPos, int topPos, int mouseX, int mouseY,
            int amount, int use, int bonusPercent, boolean hydrogen, int annealMode) {
        if (!isOver(mouseX, mouseY, leftPos, topPos)) {
            return false;
        }
        if (annealMode == MetalPressBlockEntity.ANNEAL_NONE) {
            guiGraphics.renderComponentTooltip(font, lines(amount, use, bonusPercent), mouseX, mouseY);
            return true;
        }
        String target = annealMode == MetalPressBlockEntity.ANNEAL_CIRCUITS ? "circuits" : "plates";
        List<Component> lines = new ArrayList<>();
        if (hydrogen) {
            lines.add(FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.hydrogen"), amount, InertAtmosphere.CAPACITY));
            lines.add(Component.translatable("rngtech.atmosphere.anneals." + target).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("rngtech.atmosphere.anneal_cost", InertAtmosphere.HYDROGEN_PER_ANNEAL).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.addAll(lines(amount, use, bonusPercent));
            lines.add(Component.translatable("rngtech.atmosphere.or_hydrogen." + target, InertAtmosphere.HYDROGEN_PER_ANNEAL)
                    .withStyle(ChatFormatting.GRAY));
        }
        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    static List<Component> lines(int amount, int use, int bonusPercent) {
        List<Component> lines = new ArrayList<>();
        lines.add(FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.nitrogen"), amount, InertAtmosphere.CAPACITY));
        if (bonusPercent > 0) {
            lines.add(Component.translatable("rngtech.atmosphere.active", bonusPercent).withStyle(ChatFormatting.AQUA));
        } else if (amount > 0) {
            lines.add(Component.translatable("rngtech.atmosphere.needs_blast_draft").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("rngtech.atmosphere.empty", Math.round(InertAtmosphere.OVERHEAT_BONUS * 100))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(use > 0
                ? Component.translatable("rngtech.atmosphere.using", use).withStyle(ChatFormatting.AQUA)
                : Component.translatable("rngtech.atmosphere.use_rule", InertAtmosphere.USE_PER_LANE_TICK).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }
}
