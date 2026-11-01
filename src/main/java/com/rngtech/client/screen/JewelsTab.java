package com.rngtech.client.screen;

import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.menu.JewelMenuView;
import com.rngtech.content.menu.JewelSocketSlot;
import com.rngtech.rpg.jewel.MachineJewels;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Jewels tab shared by the machine screens: a row of tree jewel slots over the inventory grid, one per allocated
 * Jewel Socket, and a row of body slots under it once the body has sockets. The slots are the menu's; this draws the
 * frames, headings, and the hints for empty ones.
 */
public final class JewelsTab {
    private static final int HEADING_Y = 20;
    private static final int BODY_HEADING_Y = 56;
    private static final int HINT_Y = 90;
    private static final int TEXT_WIDTH = 170;

    private JewelsTab() {
    }

    public static void render(GuiGraphics guiGraphics, Font font, JewelMenuView view, int leftPos, int topPos, int text, int muted,
            BiConsumer<Integer, Integer> slotFrame) {
        int tree = view.jewelTreeCapacity();
        for (int slot = 0; slot < tree; slot++) {
            slotFrame.accept(JewelSocketSlot.X - 1 + slot * JewelSocketSlot.STEP, JewelSocketSlot.TREE_Y - 1);
        }
        int body = view.jewelBodyCapacity();
        for (int slot = 0; slot < body; slot++) {
            slotFrame.accept(JewelSocketSlot.X - 1 + slot * JewelSocketSlot.STEP, JewelSocketSlot.BODY_Y - 1);
        }
        int x = leftPos + JewelSocketSlot.X;
        if (tree > 0) {
            guiGraphics.drawString(font, Component.translatable("rngtech.jewels.tree_sockets", tree, MachineJewels.TREE_SLOTS), x, topPos + HEADING_Y, text, false);
        } else {
            drawWrapped(guiGraphics, font, Component.translatable("rngtech.jewels.tree_sockets.none"), x, topPos + HEADING_Y, muted);
        }
        if (body > 0) {
            guiGraphics.drawString(font, Component.translatable("rngtech.jewels.body_sockets", body), x, topPos + BODY_HEADING_Y, text, false);
        }
        drawWrapped(guiGraphics, font, Component.translatable("rngtech.jewels.hint"), x, topPos + HINT_Y, muted);
    }

    private static void drawWrapped(GuiGraphics guiGraphics, Font font, Component component, int x, int y, int colour) {
        int line = 0;
        for (FormattedCharSequence sequence : font.split(component, TEXT_WIDTH)) {
            guiGraphics.drawString(font, sequence, x, y + line++ * font.lineHeight, colour, false);
        }
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
        return List.of(line.copy().withStyle(ChatFormatting.GRAY));
    }
}
