package com.rngtech.client.screen;

import com.rngtech.content.menu.DebugTankMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DebugTankScreen extends AbstractContainerScreen<DebugTankMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int FLUID = 0xFF2E82D8;
    private static final int EMPTY = 0xFF4A4A4A;
    private static final int BUTTON = 0xFF4F5E5D;
    private static final int BUTTON_DARK = 0xFF273433;
    private static final int BUTTON_LIGHT = 0xFF87C8FF;
    private static final int STATUS_X = 18;
    private static final int STATUS_Y = 24;
    private static final int STATUS_WIDTH = 28;
    private static final int STATUS_HEIGHT = 32;
    private static final int BUTTON_X = 122;
    private static final int BUTTON_Y = 34;
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 16;

    public DebugTankScreen(DebugTankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelX = 8;
        inventoryLabelY = 74;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderStatusTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderStatus(guiGraphics);
        renderSlotFrame(guiGraphics, 61, 34);
        renderSlotFrame(guiGraphics, 97, 34);
        renderSlotFrames(guiGraphics, 7, 83, 3);
        renderSlotFrames(guiGraphics, 7, 141, 1);
        renderButton(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.debug_tank.input"), 57, 22, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.debug_tank.output"), 91, 22, TEXT_MUTED, false);

        Component clear = Component.translatable("rngtech.debug_tank.clear");
        guiGraphics.drawString(
                font,
                clear,
                BUTTON_X + (BUTTON_WIDTH - font.width(clear)) / 2,
                BUTTON_Y + 5,
                0xFFFFFFFF,
                false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverButton(mouseX, mouseY)) {
            clickButton();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickButton() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, DebugTankMenu.BUTTON_CLEAR)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DebugTankMenu.BUTTON_CLEAR);
        }
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, PANEL_LIGHT);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_DARK);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_DARK);
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        int x = leftPos + STATUS_X;
        int y = topPos + STATUS_Y;
        guiGraphics.fill(x, y, x + STATUS_WIDTH, y + STATUS_HEIGHT, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + STATUS_WIDTH - 1, y + STATUS_HEIGHT - 1, PANEL_LIGHT);
        int fill = menu.hasFluid() ? FLUID : EMPTY;
        guiGraphics.fill(x + 4, y + 4, x + STATUS_WIDTH - 4, y + STATUS_HEIGHT - 4, fill);
        guiGraphics.fill(x + 7, y + 7, x + STATUS_WIDTH - 7, y + 10, 0x66FFFFFF);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics, int x, int y, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, x + column * 18, y + row * 18);
            }
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderButton(GuiGraphics guiGraphics) {
        int x = leftPos + BUTTON_X;
        int y = topPos + BUTTON_Y;
        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private void renderStatusTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + STATUS_X;
        int y = topPos + STATUS_Y;
        if (mouseX < x || mouseX >= x + STATUS_WIDTH || mouseY < y || mouseY >= y + STATUS_HEIGHT) {
            return;
        }
        Component status = menu.hasFluid()
                ? Component.translatable("rngtech.debug_tank.status.configured")
                : Component.translatable("rngtech.debug_tank.status.empty");
        guiGraphics.renderComponentTooltip(font, List.of(status), mouseX, mouseY);
    }

    private boolean isOverButton(double mouseX, double mouseY) {
        int x = leftPos + BUTTON_X;
        int y = topPos + BUTTON_Y;
        return mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
    }
}
