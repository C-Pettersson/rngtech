package com.rngtech.content.menu;

import net.minecraft.world.entity.player.Player;

import java.util.function.IntConsumer;

/** Menu buttons that set a heated machine's overheat slider, one button per percent. */
public final class HeatSliderSupport {
    public static final int BUTTON_BASE = 8000;
    public static final int MAX_PERCENT = 100;

    private HeatSliderSupport() {
    }

    public static int buttonId(int percent) {
        return BUTTON_BASE + Math.max(0, Math.min(MAX_PERCENT, percent));
    }

    /** Sets the slider on the server when {@code id} is a slider button; the client only acknowledges it. */
    public static boolean handle(Player player, int id, IntConsumer setSlider) {
        if (id < BUTTON_BASE || id > BUTTON_BASE + MAX_PERCENT) {
            return false;
        }
        if (!player.level().isClientSide) {
            setSlider.accept(id - BUTTON_BASE);
        }
        return true;
    }
}
