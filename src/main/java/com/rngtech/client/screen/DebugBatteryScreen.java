package com.rngtech.client.screen;

import com.rngtech.content.menu.DebugBatteryMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DebugBatteryScreen extends AbstractContainerScreen<DebugBatteryMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF27BFAE;
    private static final int BUTTON = 0xFF4F5E5D;
    private static final int BUTTON_DARK = 0xFF273433;
    private static final int BUTTON_LIGHT = 0xFF79F4E8;
    private static final int RATE_PANEL_X = 18;
    private static final int RATE_PANEL_Y = 24;
    private static final int RATE_PANEL_WIDTH = 140;
    private static final int RATE_PANEL_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 16;

    private static final ButtonSpec[] BUTTONS = {
            new ButtonSpec(DebugBatteryMenu.BUTTON_ZERO, 18, 52, "0"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_MAX, 122, 52, "MAX"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_DEC_1K, 18, 74, "-1K"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_DEC_10K, 58, 74, "-10K"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_INC_10K, 98, 74, "+10K"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_INC_1K, 138, 74, "+1K"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_DEC_100K, 18, 96, "-100K"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_DEC_1M, 58, 96, "-1M"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_INC_1M, 98, 96, "+1M"),
            new ButtonSpec(DebugBatteryMenu.BUTTON_INC_100K, 138, 96, "+100K")
    };

    public DebugBatteryScreen(DebugBatteryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 124;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                RATE_PANEL_X,
                RATE_PANEL_Y,
                RATE_PANEL_WIDTH,
                RATE_PANEL_HEIGHT,
                CompactValueText.exactEnergyRate(menu.outputRate())
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderRatePanel(guiGraphics);
        for (ButtonSpec button : BUTTONS) {
            renderButton(guiGraphics, button);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        Component output = Component.translatable("rngtech.debug_battery.output");
        guiGraphics.drawString(font, output, 18, 14, TEXT_MUTED, false);
        String value = CompactValueText.energyRate(menu.outputRate());
        guiGraphics.drawString(
                font,
                value,
                RATE_PANEL_X + RATE_PANEL_WIDTH - 7 - font.width(value),
                RATE_PANEL_Y + 6,
                TEXT,
                false
        );
        for (ButtonSpec button : BUTTONS) {
            Component label = Component.literal(button.label());
            guiGraphics.drawString(
                    font,
                    label,
                    button.x() + (BUTTON_WIDTH - font.width(label)) / 2,
                    button.y() + 5,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (ButtonSpec spec : BUTTONS) {
                if (isOverButton(mouseX, mouseY, spec)) {
                    clickButton(spec.id());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
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

    private void renderRatePanel(GuiGraphics guiGraphics) {
        int x = leftPos + RATE_PANEL_X;
        int y = topPos + RATE_PANEL_Y;
        guiGraphics.fill(x, y, x + RATE_PANEL_WIDTH, y + RATE_PANEL_HEIGHT, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + RATE_PANEL_WIDTH - 1, y + RATE_PANEL_HEIGHT - 1, PANEL_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + 7, y + RATE_PANEL_HEIGHT - 2, ENERGY);
    }

    private void renderButton(GuiGraphics guiGraphics, ButtonSpec button) {
        int x = leftPos + button.x();
        int y = topPos + button.y();
        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, BUTTON_DARK);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, BUTTON);
        guiGraphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + 2, BUTTON_LIGHT);
    }

    private boolean isOverButton(double mouseX, double mouseY, ButtonSpec button) {
        int x = leftPos + button.x();
        int y = topPos + button.y();
        return mouseX >= x && mouseX < x + BUTTON_WIDTH && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
    }

    private record ButtonSpec(int id, int x, int y, String label) {
    }
}
