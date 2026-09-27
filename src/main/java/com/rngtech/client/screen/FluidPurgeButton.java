package com.rngtech.client.screen;

import com.rngtech.content.purge.FluidPurgeSupport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

final class FluidPurgeButton {
    static final int SIZE = 10;
    private static final int FRAME = 0xFF3C4546;
    private static final int BODY = 0xFF7D9293;
    private static final int BODY_DISABLED = 0xFF6F7373;
    private static final int LIGHT = 0xFFB7D1D2;
    private static final int DRAIN = 0xFF263131;

    private FluidPurgeButton() {
    }

    static void render(GuiGraphics guiGraphics, int leftPos, int topPos, int x, int y, boolean active) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + SIZE, top + SIZE, FRAME);
        guiGraphics.fill(left + 1, top + 1, left + SIZE - 1, top + SIZE - 1, active ? BODY : BODY_DISABLED);
        guiGraphics.fill(left + 1, top + 1, left + SIZE - 1, top + 2, LIGHT);
        guiGraphics.fill(left + 4, top + 2, left + 6, top + 6, DRAIN);
        guiGraphics.fill(left + 3, top + 6, left + 7, top + 7, DRAIN);
        guiGraphics.fill(left + 2, top + 7, left + 8, top + 8, DRAIN);
    }

    static boolean handleClick(
            AbstractContainerMenu menu,
            int leftPos,
            int topPos,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int targetId
    ) {
        if (!inBounds(mouseX, mouseY, leftPos + x, topPos + y, SIZE, SIZE)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int buttonId = FluidPurgeSupport.buttonId(targetId);
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
        return true;
    }

    static boolean renderTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            Component tankName,
            Component fluidName,
            int amount,
            int capacity
    ) {
        if (!inBounds(mouseX, mouseY, leftPos + x, topPos + y, SIZE, SIZE)) {
            return false;
        }
        guiGraphics.renderComponentTooltip(
                font,
                List.of(
                        Component.translatable("rngtech.purge.tooltip.title", tankName),
                        Component.translatable("rngtech.purge.tooltip.fluid", fluidName),
                        Component.translatable("rngtech.purge.tooltip.amount", CompactValueText.fluidAmountPair(amount, capacity)),
                        Component.translatable("rngtech.purge.tooltip.action")
                ),
                mouseX,
                mouseY
        );
        return true;
    }

    private static boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
