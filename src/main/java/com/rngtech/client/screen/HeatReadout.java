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
    private static final int TRACK = 0xFF5F5F5F;
    private static final int FILL = 0xFFE0712F;
    private static final int FILL_OVERHEAT = 0xFFE8432F;
    static final int ZONE_COLD = 0xFF8A8A8A;
    static final int ZONE_WORK = 0xFF5FB65A;
    static final int ZONE_HOT = 0xFFE0A93A;
    static final int ZONE_OVER = 0xFFD8473A;
    static final int TINT_WORK = 0xFF4E6A4B;
    static final int TINT_HOT = 0xFF75633A;
    static final int TINT_OVER = 0xFF7A3E38;

    private HeatReadout() {
    }

    /**
     * A heated machine's temperatures for its heat bar and slider. {@code reach} is the slider's 100% and {@code maximum}
     * the machine's Max Temperature; a zero target means no recipe.
     */
    record Bands(int heat, int minimum, int target, int safe, int overheat, int setpoint, int reach, int maximum, int wobble) {
        boolean hasRecipe() {
            return target > 0;
        }

        /** The heat bar's top: the hottest the lane can be pushed, with a little headroom, or the current heat. */
        int scale() {
            if (!hasRecipe()) {
                return Math.max(heat, maximum);
            }
            int top = Math.max(Math.max(reach, overheat), setpoint + wobble);
            return Math.max(heat, (int) Math.round(top * 1.08));
        }

        /** The band a temperature falls in: below the minimum, working, past the safe maximum, past overheating. */
        int zone(int temperature) {
            if (temperature < minimum) {
                return ZONE_COLD;
            }
            return temperature <= safe ? ZONE_WORK : temperature <= overheat ? ZONE_HOT : ZONE_OVER;
        }
    }

    /**
     * A vertical heat bar: the empty part tinted by band, the fill turning red past the overheat temperature, a band strip
     * on the inner edge that stays visible under the fill, then the target, setpoint, and wobble markers.
     */
    static void renderBar(GuiGraphics guiGraphics, int barX, int barY, int barWidth, int barHeight, int fillHeight, Bands bands) {
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, TRACK);
        int scale = bands.scale();
        if (scale <= 0) {
            return;
        }
        int left = barX + 1;
        int right = barX + barWidth - 1;
        int stripLeft = right - 2;
        if (bands.hasRecipe()) {
            band(guiGraphics, left, stripLeft, barY, barHeight, fillHeight, scale, bands.minimum(), bands.safe(), TINT_WORK);
            band(guiGraphics, left, stripLeft, barY, barHeight, fillHeight, scale, bands.safe(), bands.overheat(), TINT_HOT);
            band(guiGraphics, left, stripLeft, barY, barHeight, fillHeight, scale, bands.overheat(), Integer.MAX_VALUE, TINT_OVER);
        }
        int heatTop = level(barY, barHeight, fillHeight, scale, bands.heat());
        int bottom = barY + barHeight - 1;
        if (bands.heat() > 0) {
            guiGraphics.fill(left, heatTop, stripLeft, bottom, FILL);
            if (bands.hasRecipe() && bands.heat() > bands.overheat()) {
                guiGraphics.fill(left, heatTop, stripLeft, level(barY, barHeight, fillHeight, scale, bands.overheat()), FILL_OVERHEAT);
            }
        }
        if (bands.hasRecipe()) {
            band(guiGraphics, stripLeft, right, barY, barHeight, fillHeight, scale, 0, bands.minimum(), ZONE_COLD);
            band(guiGraphics, stripLeft, right, barY, barHeight, fillHeight, scale, bands.minimum(), bands.safe(), ZONE_WORK);
            band(guiGraphics, stripLeft, right, barY, barHeight, fillHeight, scale, bands.safe(), bands.overheat(), ZONE_HOT);
            band(guiGraphics, stripLeft, right, barY, barHeight, fillHeight, scale, bands.overheat(), Integer.MAX_VALUE, ZONE_OVER);
        }
        renderMarkers(guiGraphics, barX, barY, barWidth, barHeight, fillHeight, scale, bands.target(), bands.setpoint(), bands.wobble());
    }

    private static void band(GuiGraphics guiGraphics, int left, int right, int barY, int barHeight, int fillHeight, int scale, int from, int to,
            int color) {
        if (to <= from) {
            return;
        }
        int bottom = level(barY, barHeight, fillHeight, scale, from);
        int top = level(barY, barHeight, fillHeight, scale, to);
        if (bottom > top) {
            guiGraphics.fill(left, top, right, bottom, color);
        }
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
