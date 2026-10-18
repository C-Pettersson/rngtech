package com.rngtech.client.screen;

import com.rngtech.content.menu.HeatSliderSupport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Process tab's overheat slider: a track from the recipe target (left) to Max Temperature (right), colored by heat
 * band. The knob moves over the part the lane may reach; the rest shows as locked. Clicking or dragging sets it in steps of
 * 5% of the reachable band, and the setpoint is written beside the track.
 */
final class HeatSlider {
    private static final int HEIGHT = 6;
    private static final int STEP = 5;
    private static final int TRACK = 0xFF5F5F5F;
    private static final int FILL = 0xFFE0712F;
    private static final int KNOB = 0xFFF2F2F2;
    private static final int LOCKED = 0xFF3C3C3C;
    private static final int LOCKED_HATCH = 0xFF4A4A4A;
    private static final int LABEL = 0xFF3F3F3F;

    private final int x;
    private final int y;
    private final int width;
    private final boolean labelLeft;
    private boolean dragging;
    private int pending = -1;
    private HeatReadout.Bands bands;

    HeatSlider(int x, int y, int width) {
        this(x, y, width, false);
    }

    HeatSlider(int x, int y, int width, boolean labelLeft) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.labelLeft = labelLeft;
    }

    /** Draws the track by band, the locked part past the reach, the knob, and the setpoint label. */
    void render(GuiGraphics guiGraphics, Font font, int left, int top, int percent, HeatReadout.Bands bands) {
        this.bands = bands;
        if (!banded(bands)) {
            render(guiGraphics, left, top, percent);
            return;
        }
        int shown = pending >= 0 ? pending : percent;
        int x0 = left + x;
        int y0 = top + y;
        guiGraphics.fill(x0, y0, x0 + width, y0 + HEIGHT, TRACK);
        int trackTop = Math.max(bands.maximum(), bands.reach());
        int setpoint = Math.min(bands.reach(), bands.target() + Math.round((trackTop - bands.target()) * shown / 100.0F));
        // The reachable band, dim, then the part up to the setpoint, bright.
        segment(guiGraphics, x0, y0, bands.target(), bands.reach(), bands, true);
        segment(guiGraphics, x0, y0, bands.target(), setpoint, bands, false);
        int reachX = position(bands, bands.reach());
        int end = width - 1;
        for (int px = reachX; px < end; px++) {
            guiGraphics.fill(x0 + px, y0 + 1, x0 + px + 1, y0 + HEIGHT - 1, (px + y0) % 3 == 0 ? LOCKED_HATCH : LOCKED);
        }
        int knob = x0 + Math.min(width - 3, position(bands, setpoint));
        guiGraphics.fill(knob, y0 - 1, knob + 2, y0 + HEIGHT + 1, KNOB);
        String label = String.format(java.util.Locale.ROOT, "%,d°C", setpoint);
        int labelX = labelLeft ? x0 - 3 - font.width(label) : x0 + width + 3;
        guiGraphics.drawString(font, label, labelX, y0 - 1, LABEL, false);
    }

    private static boolean banded(HeatReadout.Bands bands) {
        return bands != null && bands.hasRecipe() && bands.reach() > bands.target();
    }

    /** The pixel offset of a temperature on the track, which spans the target to Max Temperature. */
    private int position(HeatReadout.Bands bands, int temperature) {
        int top = Math.max(bands.maximum(), bands.reach());
        double share = (temperature - bands.target()) / (double) Math.max(1, top - bands.target());
        return 1 + (int) Math.round(Math.max(0.0, Math.min(1.0, share)) * (width - 2));
    }

    private void segment(GuiGraphics guiGraphics, int x0, int y0, int from, int to, HeatReadout.Bands bands, boolean dim) {
        int[][] zones = {
                {from, Math.min(to, bands.safe()), dim ? HeatReadout.TINT_WORK : HeatReadout.ZONE_WORK},
                {Math.max(from, bands.safe()), Math.min(to, bands.overheat()), dim ? HeatReadout.TINT_HOT : HeatReadout.ZONE_HOT},
                {Math.max(from, bands.overheat()), to, dim ? HeatReadout.TINT_OVER : HeatReadout.ZONE_OVER}};
        for (int[] zone : zones) {
            if (zone[1] > zone[0]) {
                guiGraphics.fill(x0 + position(bands, zone[0]), y0 + 1, x0 + position(bands, zone[1]), y0 + HEIGHT - 1, zone[2]);
            }
        }
    }

    void render(GuiGraphics guiGraphics, int left, int top, int percent) {
        int shown = pending >= 0 ? pending : percent;
        guiGraphics.fill(left + x, top + y, left + x + width, top + y + HEIGHT, TRACK);
        int inner = width - 2;
        int filled = Math.round(inner * shown / 100.0F);
        guiGraphics.fill(left + x + 1, top + y + 1, left + x + 1 + filled, top + y + HEIGHT - 1, FILL);
        int knob = left + x + 1 + Math.min(inner - 2, filled);
        guiGraphics.fill(knob, top + y - 1, knob + 2, top + y + HEIGHT + 1, KNOB);
    }

    boolean isOver(double mouseX, double mouseY, int left, int top) {
        return mouseX >= left + x - 1 && mouseX < left + x + width + 1 && mouseY >= top + y - 2 && mouseY < top + y + HEIGHT + 2;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int left, int top, int containerId) {
        if (button != 0 || !isOver(mouseX, mouseY, left, top)) {
            return false;
        }
        dragging = true;
        send(mouseX, left, containerId);
        return true;
    }

    boolean mouseDragged(double mouseX, int button, int left, int containerId) {
        if (!dragging || button != 0) {
            return false;
        }
        send(mouseX, left, containerId);
        return true;
    }

    void mouseReleased() {
        dragging = false;
    }

    /** Forgets the locally shown value once the server reports it. */
    void sync(int percent) {
        if (pending == percent) {
            pending = -1;
        }
    }

    /** The setpoint in degrees and how far past the target it is, the band the slider covers, and what is still locked. */
    List<Component> tooltip(int percent, int setpoint) {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("rngtech.heat_slider.tooltip.title"));
        if (!banded(bands)) {
            lines.add(Component.translatable("rngtech.heat_slider.tooltip.idle"));
        } else {
            lines.add(Component.translatable("rngtech.heat_slider.tooltip.setpoint", degrees(setpoint), degrees(setpoint - bands.target())));
            lines.add(Component.translatable("rngtech.heat_slider.tooltip.range", degrees(bands.target()), degrees(bands.reach())));
            if (bands.reach() < bands.maximum()) {
                lines.add(Component.translatable("rngtech.heat_slider.tooltip.locked", degrees(bands.reach())));
            }
        }
        lines.add(Component.translatable("rngtech.heat_slider.tooltip.hint"));
        return lines;
    }

    private static String degrees(int temperature) {
        return String.format(java.util.Locale.ROOT, "%,d", temperature);
    }

    private void send(double mouseX, int left, int containerId) {
        double fraction = (mouseX - (left + x + 1)) / Math.max(1, width - 2);
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * 100.0 / STEP) * STEP;
        if (banded(bands)) {
            // The knob stops where the locked part starts, so unlocking more reach later never moves the setpoint.
            int top = Math.max(bands.maximum(), bands.reach());
            int reachPercent = (int) Math.floor(100.0 * (bands.reach() - bands.target()) / Math.max(1, top - bands.target()));
            percent = Math.min(percent, reachPercent);
        }
        if (percent == pending) {
            return;
        }
        pending = percent;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(containerId, HeatSliderSupport.buttonId(percent));
        }
    }
}
