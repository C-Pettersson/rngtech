package com.rngtech.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The heat bar's markers and hover lines shared by the Furnace, Alloy Furnace, Metal Press, and Melter screens. */
final class HeatReadout {
    private static final int TARGET = 0xFFF2F2F2;
    private static final int SETPOINT = 0xFFFFD27F;
    private static final int WOBBLE = 0x55FFFFFF;

    private HeatReadout() {
    }

    /** Marks the target and the setpoint on a vertical heat bar, and shades the wobble band around the setpoint. */
    static void renderMarkers(
            GuiGraphics guiGraphics,
            int barX,
            int barY,
            int barWidth,
            int barHeight,
            int fillHeight,
            int scale,
            int target,
            int setpoint,
            int wobble
    ) {
        if (scale <= 0 || target <= 0) {
            return;
        }
        int held = Math.max(target, setpoint);
        if (wobble > 0) {
            int top = level(barY, barHeight, fillHeight, scale, held + wobble);
            int bottom = level(barY, barHeight, fillHeight, scale, held - wobble);
            guiGraphics.fill(barX + 1, top, barX + barWidth - 1, Math.max(top + 1, bottom), WOBBLE);
        }
        int targetY = level(barY, barHeight, fillHeight, scale, target);
        guiGraphics.fill(barX, targetY, barX + barWidth, targetY + 1, TARGET);
        if (setpoint > target) {
            int setpointY = level(barY, barHeight, fillHeight, scale, setpoint);
            guiGraphics.fill(barX, setpointY, barX + barWidth, setpointY + 1, SETPOINT);
        }
    }

    /** The heat bar's hover: the temperatures, then the setpoint, heat factor, wobble, and where the FE goes. */
    static List<Component> lines(
            Component heat,
            int slider,
            int target,
            int setpoint,
            int heatFactor,
            int wobble,
            int heating,
            int holding,
            boolean electric
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(heat);
        if (setpoint > target) {
            lines.add(Component.translatable("rngtech.heat.tooltip.setpoint", setpoint, slider));
        }
        if (heatFactor > 0) {
            lines.add(Component.translatable("rngtech.heat.tooltip.heat_factor", String.format(Locale.ROOT, "%.2f", heatFactor / 100.0)));
        }
        if (wobble > 0) {
            lines.add(Component.translatable("rngtech.heat.tooltip.wobble", wobble));
        }
        if (electric && heating + holding > 0) {
            lines.add(Component.translatable("rngtech.heat.tooltip.energy", heating, holding));
        }
        return lines;
    }

    private static int level(int barY, int barHeight, int fillHeight, int scale, int temperature) {
        double share = Math.max(0.0, Math.min(1.0, temperature / (double) scale));
        return barY + barHeight - 1 - (int) Math.round(fillHeight * share);
    }
}
