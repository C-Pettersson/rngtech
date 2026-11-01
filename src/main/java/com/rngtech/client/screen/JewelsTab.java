package com.rngtech.client.screen;

import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.menu.JewelMenuView;
import com.rngtech.content.menu.JewelSocketSlot;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Jewels tab shared by the machine screens: a row of tree jewel slots over the inventory grid, one per allocated
 * Jewel Socket, and a row of body slots under it. The slots are the menu's; this draws their frames and a one-word label
 * for each row that exists. Everything else is said by the slot tooltips, so the tab never becomes a wall of text.
 */
public final class JewelsTab {
    private static final int LABEL_X = 10;
    private static final int LABEL_OFFSET = 5;

    private JewelsTab() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, JewelMenuView view, int leftPos, int topPos, int text, int muted,
            BiConsumer<Integer, Integer> slotFrame) {
        row(guiGraphics, font, view.jewelTreeCapacity(), JewelSocketSlot.TREE_Y, "rngtech.jewels.row.tree", leftPos, topPos, muted, slotFrame);
        row(guiGraphics, font, view.jewelBodyCapacity(), JewelSocketSlot.BODY_Y, "rngtech.jewels.row.body", leftPos, topPos, muted, slotFrame);
    }

    private static void row(GuiGraphics guiGraphics, Font font, int slots, int y, String labelKey, int leftPos, int topPos, int colour,
            BiConsumer<Integer, Integer> slotFrame) {
        if (slots <= 0) {
            return;
        }
        for (int slot = 0; slot < slots; slot++) {
            slotFrame.accept(JewelSocketSlot.X - 1 + slot * JewelSocketSlot.STEP, y - 1);
        }
        guiGraphics.drawString(font, Component.translatable(labelKey), leftPos + LABEL_X, topPos + y + LABEL_OFFSET, colour, false);
    }

    /** The hint for an empty jewel slot under the cursor, or nothing. */
    public static List<Component> tooltip(AbstractContainerScreen<?> screen) {
        Slot hovered = screen.getSlotUnderMouse();
        if (!(hovered instanceof JewelSocketSlot socket) || socket.hasItem()) {
            return List.of();
        }
        int index = socket.getSlotIndex();
        Component line = index < JewelSockets.BODY_START
                ? Component.translatable("rngtech.jewels.slot.tree", index + 1)
                : Component.translatable("rngtech.jewels.slot.body", index - JewelSockets.BODY_START + 1);
        return List.of(line.copy().withStyle(ChatFormatting.GRAY), Component.translatable("rngtech.jewels.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
