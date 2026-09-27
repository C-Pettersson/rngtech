package com.rngtech.client.screen;

import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.menu.ConfiguratorActionMenu;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ConfiguratorActionScreen extends AbstractContainerScreen<ConfiguratorActionMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF4F6F73;
    private static final int BUTTON_DARK = 0xFF283D40;
    private static final int BUTTON_LIGHT = 0xFF88AEB2;
    private static final int BUTTON_X = 18;
    private static final int BUTTON_Y = 36;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 6;

    private static final int[] BUTTONS = {
            ConfiguratorActionMenu.BUTTON_COPY_ALL,
            ConfiguratorActionMenu.BUTTON_COPY_FLUID,
            ConfiguratorActionMenu.BUTTON_COPY_ITEM,
            ConfiguratorActionMenu.BUTTON_COPY_ENERGY,
            ConfiguratorActionMenu.BUTTON_PASTE
    };

    public ConfiguratorActionScreen(ConfiguratorActionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 184;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 210;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        for (int index = 0; index < BUTTONS.length; index++) {
            renderButton(guiGraphics, BUTTON_X, BUTTON_Y + index * (BUTTON_HEIGHT + BUTTON_GAP), BUTTON_WIDTH, BUTTON_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(
                font,
                Component.translatable(menu.target().type().translationKey()),
                8,
                22,
                TEXT_MUTED,
                false
        );
        for (int index = 0; index < BUTTONS.length; index++) {
            drawCentered(
                    guiGraphics,
                    Component.translatable("rngtech.configurator.action." + BUTTONS[index]),
                    BUTTON_X,
                    BUTTON_Y + index * (BUTTON_HEIGHT + BUTTON_GAP) + 5,
                    BUTTON_WIDTH,
                0xFFFFFFFF
            );
        }
        renderCopiedSummary(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int index = 0; index < BUTTONS.length; index++) {
                int y = BUTTON_Y + index * (BUTTON_HEIGHT + BUTTON_GAP);
                if (inBounds(mouseX, mouseY, BUTTON_X, y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    sendButton(BUTTONS[index]);
                    return true;
                }
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

    private void renderCopiedSummary(GuiGraphics guiGraphics) {
        ConfiguratorPreset preset = currentPreset();
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.configurator.copied_settings"),
                8,
                156,
                TEXT_MUTED,
                false
        );
        Component summary = copiedSummary(preset);
        drawClipped(guiGraphics, summary, 8, 168, imageWidth - 16, preset.hasAnySettings() ? TEXT : TEXT_MUTED);
    }

    private ConfiguratorPreset currentPreset() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ConfiguratorPreset.EMPTY;
        }
        ItemStack stack = player.getItemInHand(menu.hand());
        return stack.getOrDefault(ModDataComponents.CONFIGURATOR_PRESET.get(), ConfiguratorPreset.EMPTY);
    }

    private Component copiedSummary(ConfiguratorPreset preset) {
        if (!preset.hasAnySettings()) {
            return Component.translatable("rngtech.configurator.message.empty_preset");
        }
        StringBuilder summary = new StringBuilder();
        if (preset.energy().present()) {
            summary.append("Energy");
        }
        if (hasPresent(preset.fluidModules())) {
            appendSummary(summary, "Fluid");
        }
        if (hasPresent(preset.itemModules())) {
            appendSummary(summary, "Item");
        }
        return Component.literal(summary.toString());
    }

    private static void appendSummary(StringBuilder summary, String value) {
        if (!summary.isEmpty()) {
            summary.append(", ");
        }
        summary.append(value);
    }

    private static boolean hasPresent(java.util.List<com.rngtech.content.configurator.ConnectorSetting> settings) {
        for (com.rngtech.content.configurator.ConnectorSetting setting : settings) {
            if (setting.present()) {
                return true;
            }
        }
        return false;
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

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, BUTTON_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, BUTTON);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, BUTTON_LIGHT);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private void drawClipped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        String value = text.getString();
        if (font.width(value) <= width) {
            guiGraphics.drawString(font, value, x, y, color, false);
            return;
        }
        while (font.width(value + "...") > width && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }
        guiGraphics.drawString(font, value + "...", x, y, color, false);
    }
}
