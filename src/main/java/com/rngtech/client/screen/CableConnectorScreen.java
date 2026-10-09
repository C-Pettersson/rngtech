package com.rngtech.client.screen;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.menu.CableConnectorMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CableConnectorScreen extends AbstractContainerScreen<CableConnectorMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF4F6F73;
    private static final int BUTTON_DARK = 0xFF283D40;
    private static final int BUTTON_LIGHT = 0xFF88AEB2;
    private static final int SELECTED = 0xFF6C7658;
    private static final int SELECTED_LIGHT = 0xFFA5B17E;
    private static final int SMALL_BUTTON = 18;
    private static final int ROW_Y = 58;
    private static final int MODE_X = 16;
    private static final int MODE_WIDTH = 50;
    private static final int CHANNEL_DOWN_X = 76;
    private static final int CHANNEL_VALUE_X = 97;
    private static final int CHANNEL_UP_X = 118;
    private static final int DISTRIBUTION_X = 146;
    private static final int DISTRIBUTION_WIDTH = 56;
    private static final int ATTACH_X = 212;
    private static final int ATTACH_WIDTH = 48;
    private static final int TRANSFER_Y = 100;
    private static final int WARNING_Y = 110;
    private static final int WARNING = 0xFFA14D4D;

    public CableConnectorScreen(CableConnectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 276;
        imageHeight = 122;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 200;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderControlTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderButton(guiGraphics, MODE_X, ROW_Y, MODE_WIDTH, SMALL_BUTTON, false);
        renderButton(guiGraphics, CHANNEL_DOWN_X, ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        renderButton(guiGraphics, CHANNEL_UP_X, ROW_Y, SMALL_BUTTON, SMALL_BUTTON, false);
        renderButton(guiGraphics, DISTRIBUTION_X, ROW_Y, DISTRIBUTION_WIDTH, SMALL_BUTTON, false);
        renderButton(guiGraphics, ATTACH_X, ROW_Y, ATTACH_WIDTH, SMALL_BUTTON, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.cable_connector.face", directionName(menu.face())),
                8,
                22,
                TEXT_MUTED,
                false
        );
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.mode"), 20, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.channel"), 78, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.distribution.short"), 146, 48, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.universal_connector.attach"), 217, 48, TEXT_MUTED, false);
        drawCentered(guiGraphics, modeName(), MODE_X, ROW_Y + 5, MODE_WIDTH, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("-"), CHANNEL_DOWN_X, ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal("+"), CHANNEL_UP_X, ROW_Y + 5, SMALL_BUTTON, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(Integer.toString(menu.channel())), CHANNEL_VALUE_X, ROW_Y + 5, 18, TEXT);
        drawCentered(guiGraphics, distributionName(), DISTRIBUTION_X, ROW_Y + 5, DISTRIBUTION_WIDTH, 0xFFFFFFFF);
        drawCentered(guiGraphics, directionName(menu.attachAs()), ATTACH_X, ROW_Y + 5, ATTACH_WIDTH, 0xFFFFFFFF);
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.cable_connector.transfer", menu.transferRate()),
                8,
                TRANSFER_Y,
                TEXT_MUTED,
                false
        );
        if (!menu.targetHasEnergyAccess()) {
            String warning = Component.translatable("rngtech.cable_connector.no_energy_access").getString();
            guiGraphics.drawString(font, font.plainSubstrByWidth(warning, imageWidth - 16), 8, WARNING_Y, WARNING, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inBounds(mouseX, mouseY, CHANNEL_DOWN_X, ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                sendButton(CableConnectorMenu.BUTTON_CHANNEL_DOWN);
                return true;
            }
            if (inBounds(mouseX, mouseY, CHANNEL_UP_X, ROW_Y, SMALL_BUTTON, SMALL_BUTTON)) {
                sendButton(CableConnectorMenu.BUTTON_CHANNEL_UP);
                return true;
            }
            if (inBounds(mouseX, mouseY, MODE_X, ROW_Y, MODE_WIDTH, SMALL_BUTTON)) {
                sendButton(CableConnectorMenu.BUTTON_MODE);
                return true;
            }
            if (inBounds(mouseX, mouseY, DISTRIBUTION_X, ROW_Y, DISTRIBUTION_WIDTH, SMALL_BUTTON)) {
                sendButton(CableConnectorMenu.BUTTON_DISTRIBUTION);
                return true;
            }
            if (inBounds(mouseX, mouseY, ATTACH_X, ROW_Y, ATTACH_WIDTH, SMALL_BUTTON)) {
                sendButton(CableConnectorMenu.BUTTON_ATTACH_AS_BASE + nextDirection(menu.attachAs()).ordinal());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
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

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, BUTTON_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, selected ? SELECTED : BUTTON);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, selected ? SELECTED_LIGHT : BUTTON_LIGHT);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private Component modeName() {
        return Component.translatable(connectorMode().translationKey());
    }

    private CableConnectorMode connectorMode() {
        CableConnectorMode[] values = CableConnectorMode.values();
        int ordinal = menu.modeOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
    }

    private EnergyDistributionMode distributionMode() {
        EnergyDistributionMode[] values = EnergyDistributionMode.values();
        int ordinal = menu.distributionOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EnergyDistributionMode.ROUND_ROBIN;
    }

    private Component distributionName() {
        return Component.translatable(distributionMode().translationKey());
    }

    private void renderControlTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (inBounds(mouseX, mouseY, MODE_X, ROW_Y, MODE_WIDTH, SMALL_BUTTON)) {
            CableConnectorMode mode = connectorMode();
            guiGraphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable(mode.translationKey()), Component.translatable(mode.descriptionKey())),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (!inBounds(mouseX, mouseY, DISTRIBUTION_X, ROW_Y, DISTRIBUTION_WIDTH, SMALL_BUTTON)) {
            return;
        }
        EnergyDistributionMode mode = distributionMode();
        guiGraphics.renderComponentTooltip(
                font,
                List.of(Component.translatable(mode.translationKey()), Component.translatable(mode.descriptionKey())),
                mouseX,
                mouseY
        );
    }

    private static Component directionName(Direction direction) {
        return Component.translatable("rngtech.direction." + direction.getSerializedName());
    }

    private static Direction nextDirection(Direction direction) {
        Direction[] values = Direction.values();
        int ordinal = direction == null ? 0 : direction.ordinal() + 1;
        return values[Math.floorMod(ordinal, values.length)];
    }
}
