package com.rngtech.client.screen;

import com.rngtech.content.menu.HeatSliderSupport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Process tab's overheat slider: a short track from the recipe target (left) to Max Temperature (right). Clicking or
 * dragging sets it in steps of 5%.
 */
final class HeatSlider {
    private static final int HEIGHT = 6;
    private static final int STEP = 5;
    private static final int TRACK = 0xFF5F5F5F;
    private static final int FILL = 0xFFE0712F;
    private static final int KNOB = 0xFFF2F2F2;

    private final int x;
    private final int y;
    private final int width;
    private boolean dragging;
    private int pending = -1;

    HeatSlider(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
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

    List<Component> tooltip(int percent, int setpoint) {
        Component value = setpoint > 0
                ? Component.translatable("rngtech.heat_slider.tooltip.setpoint", percent, setpoint)
                : Component.translatable("rngtech.heat_slider.tooltip.idle", percent);
        return List.of(Component.translatable("rngtech.heat_slider.tooltip.title"), value,
                Component.translatable("rngtech.heat_slider.tooltip.hint"));
    }

    private void send(double mouseX, int left, int containerId) {
        double fraction = (mouseX - (left + x + 1)) / Math.max(1, width - 2);
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * 100.0 / STEP) * STEP;
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
