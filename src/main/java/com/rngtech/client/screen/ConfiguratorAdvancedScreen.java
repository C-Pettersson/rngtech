package com.rngtech.client.screen;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.configurator.ConfiguratorPresetCategory;
import com.rngtech.content.configurator.ConnectorSetting;
import com.rngtech.content.configurator.RelativeDirection;
import com.rngtech.content.menu.ConfiguratorAdvancedMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ConfiguratorAdvancedScreen extends AbstractContainerScreen<ConfiguratorAdvancedMenu> {
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

    public ConfiguratorAdvancedScreen(ConfiguratorAdvancedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 286;
        imageHeight = 252;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 274;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        RelativeDirection attach = menu.representativeAttach();
        renderButton(guiGraphics, 16, 32, 96, 18, false);
        renderButton(guiGraphics, 126, 32, 48, 18, menu.preset().pasteModules());
        renderButton(guiGraphics, 178, 32, 48, 18, menu.preset().gearHelper());
        renderButton(guiGraphics, 16, 62, 22, 18, false);
        renderButton(guiGraphics, 76, 62, 22, 18, false);
        renderButton(guiGraphics, 112, 62, 74, 18, false);
        renderButton(guiGraphics, 16, 92, 74, 18, attach.isDefault());
        renderButton(guiGraphics, 96, 92, 56, 18, attach.isNone());
        Direction[] directions = Direction.values();
        for (int index = 0; index < directions.length; index++) {
            int row = index / 3;
            int column = index % 3;
            boolean selected = !attach.isDefault()
                    && !attach.isNone()
                    && attach.directionOrdinal() == directions[index].ordinal();
            renderButton(guiGraphics, 16 + column * 48, 122 + row * 22, 42, 18, selected);
        }
        renderButton(guiGraphics, 166, 92, 58, 18, false);
        renderButton(guiGraphics, 166, 122, 58, 18, false);
        renderButton(guiGraphics, 166, 144, 58, 18, false);
        guiGraphics.fill(leftPos + 16, topPos + 168, leftPos + imageWidth - 16, topPos + 169, PANEL_DARK);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.configurator.category"), 16, 22, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.translatable(menu.selectedCategory().translationKey()), 16, 37, 96, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.configurator.modules", menu.preset().pasteModules()), 126, 37, 48, 0xFFFFFFFF);
        drawCentered(guiGraphics, toggleText("rngtech.configurator.gear", menu.preset().gearHelper()), 178, 37, 48, 0xFFFFFFFF);

        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.channel"), 16, 54, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.literal("-"), 16, 67, 22, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.literal(Integer.toString(menu.representativeChannel())), 42, 67, 30, TEXT);
        drawCentered(guiGraphics, Component.literal("+"), 76, 67, 22, 0xFFFFFFFF);
        drawCentered(guiGraphics, modeName(), 112, 67, 74, 0xFFFFFFFF);

        guiGraphics.drawString(font, Component.translatable("rngtech.cable_connector.attach_as"), 16, 84, TEXT_MUTED, false);
        drawCentered(guiGraphics, Component.translatable("rngtech.configurator.attach.default"), 16, 97, 74, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.translatable("rngtech.universal_connector.attach.none"), 96, 97, 56, 0xFFFFFFFF);
        Direction[] directions = Direction.values();
        for (int index = 0; index < directions.length; index++) {
            int row = index / 3;
            int column = index % 3;
            drawCentered(
                    guiGraphics,
                    Component.translatable("rngtech.direction." + directions[index].getSerializedName()),
                    16 + column * 48,
                    127 + row * 22,
                    42,
                    0xFFFFFFFF
            );
        }
        drawCentered(guiGraphics, Component.translatable("rngtech.configurator.flip"), 166, 97, 58, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.translatable("rngtech.configurator.reset"), 166, 127, 58, 0xFFFFFFFF);
        drawCentered(guiGraphics, Component.translatable("rngtech.configurator.reset_all"), 166, 149, 58, 0xFFFFFFFF);
        renderPresetSummary(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inBounds(mouseX, mouseY, 16, 32, 96, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_CATEGORY);
                return true;
            }
            if (inBounds(mouseX, mouseY, 126, 32, 48, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_TOGGLE_MODULES);
                return true;
            }
            if (inBounds(mouseX, mouseY, 178, 32, 48, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_TOGGLE_GEAR_HELPER);
                return true;
            }
            if (inBounds(mouseX, mouseY, 16, 62, 22, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_CHANNEL_DOWN);
                return true;
            }
            if (inBounds(mouseX, mouseY, 76, 62, 22, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_CHANNEL_UP);
                return true;
            }
            if (inBounds(mouseX, mouseY, 112, 62, 74, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_CYCLE_MODE);
                return true;
            }
            if (inBounds(mouseX, mouseY, 16, 92, 74, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_ATTACH_DEFAULT);
                return true;
            }
            if (inBounds(mouseX, mouseY, 96, 92, 56, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_ATTACH_NONE);
                return true;
            }
            Direction[] directions = Direction.values();
            for (int index = 0; index < directions.length; index++) {
                int row = index / 3;
                int column = index % 3;
                if (inBounds(mouseX, mouseY, 16 + column * 48, 122 + row * 22, 42, 18)) {
                    sendButton(ConfiguratorAdvancedMenu.BUTTON_ATTACH_BASE + directions[index].ordinal());
                    return true;
                }
            }
            if (inBounds(mouseX, mouseY, 166, 92, 58, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_FLIP_MODES);
                return true;
            }
            if (inBounds(mouseX, mouseY, 166, 122, 58, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_RESET_SELECTED);
                return true;
            }
            if (inBounds(mouseX, mouseY, 166, 144, 58, 18)) {
                sendButton(ConfiguratorAdvancedMenu.BUTTON_RESET_ALL);
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

    private Component modeName() {
        ConfiguratorPresetCategory category = menu.representativeModeCategory();
        int ordinal = menu.representativeMode();
        if (category == ConfiguratorPresetCategory.FLUID) {
            FluidConnectorMode[] values = FluidConnectorMode.values();
            FluidConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : FluidConnectorMode.INPUT;
            return Component.translatable(mode.translationKey());
        }
        if (category == ConfiguratorPresetCategory.ITEM) {
            ItemConnectorMode[] values = ItemConnectorMode.values();
            ItemConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ItemConnectorMode.INPUT;
            return Component.translatable(mode.translationKey());
        }
        CableConnectorMode[] values = CableConnectorMode.values();
        CableConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
        return Component.translatable(mode.translationKey());
    }

    private void renderPresetSummary(GuiGraphics guiGraphics) {
        ConfiguratorPreset preset = menu.preset();
        int y = 176;
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.configurator.copied_settings"),
                16,
                y,
                TEXT_MUTED,
                false
        );
        y += 12;
        if (!preset.hasAnySettings()) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("rngtech.configurator.message.empty_preset"),
                    16,
                    y,
                    TEXT_MUTED,
                    false
            );
            return;
        }

        if (preset.energy().present()) {
            drawSummaryLine(guiGraphics, y, Component.literal("E"), preset.energy(), ConfiguratorPresetCategory.ENERGY);
            y += 9;
        }
        for (int index = 0; index < preset.fluidModules().size(); index++) {
            ConnectorSetting setting = preset.fluidModules().get(index);
            if (setting.present()) {
                drawSummaryLine(guiGraphics, y, Component.literal("F" + (index + 1)), setting, ConfiguratorPresetCategory.FLUID);
                y += 9;
            }
        }
        for (int index = 0; index < preset.itemModules().size(); index++) {
            ConnectorSetting setting = preset.itemModules().get(index);
            if (setting.present()) {
                drawSummaryLine(guiGraphics, y, Component.literal("I" + (index + 1)), setting, ConfiguratorPresetCategory.ITEM);
                y += 9;
            }
        }
    }

    private void drawSummaryLine(
            GuiGraphics guiGraphics,
            int y,
            Component label,
            ConnectorSetting setting,
            ConfiguratorPresetCategory category
    ) {
        Component text = Component.literal("")
                .append(label)
                .append(Component.literal(": C" + setting.channel() + " "))
                .append(modeName(category, setting.modeOrdinal()))
                .append(Component.literal(" "))
                .append(attachName(setting.attachAs()));
        if (category == ConfiguratorPresetCategory.ENERGY) {
            text = Component.literal("")
                    .append(label)
                    .append(Component.literal(": C" + setting.channel() + " "))
                    .append(modeName(category, setting.modeOrdinal()))
                    .append(Component.literal(" "))
                    .append(energyDistributionName(setting.distributionOrdinal()))
                    .append(Component.literal(" "))
                    .append(attachName(setting.attachAs()));
        }
        ItemStack module = setting.module();
        if (!module.isEmpty()) {
            text = Component.literal("").append(text).append(Component.literal(" ")).append(module.getHoverName());
        }
        drawClipped(guiGraphics, text, 16, y, imageWidth - 32, TEXT);
    }

    private Component modeName(ConfiguratorPresetCategory category, int ordinal) {
        if (category == ConfiguratorPresetCategory.FLUID) {
            FluidConnectorMode[] values = FluidConnectorMode.values();
            FluidConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : FluidConnectorMode.INPUT;
            return Component.translatable(mode.translationKey());
        }
        if (category == ConfiguratorPresetCategory.ITEM) {
            ItemConnectorMode[] values = ItemConnectorMode.values();
            ItemConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : ItemConnectorMode.INPUT;
            return Component.translatable(mode.translationKey());
        }
        CableConnectorMode[] values = CableConnectorMode.values();
        CableConnectorMode mode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
        return Component.translatable(mode.translationKey());
    }

    private Component energyDistributionName(int ordinal) {
        EnergyDistributionMode[] values = EnergyDistributionMode.values();
        EnergyDistributionMode mode = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : EnergyDistributionMode.ROUND_ROBIN;
        return Component.translatable(mode.translationKey());
    }

    private Component attachName(RelativeDirection attach) {
        if (attach.isNone()) {
            return Component.translatable("rngtech.universal_connector.attach.none");
        }
        if (attach.isDefault()) {
            return Component.translatable("rngtech.configurator.attach.default");
        }
        Direction[] directions = Direction.values();
        int ordinal = attach.directionOrdinal();
        Direction direction = ordinal >= 0 && ordinal < directions.length ? directions[ordinal] : Direction.NORTH;
        return Component.translatable("rngtech.direction." + direction.getSerializedName());
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
        value = value + "...";
        guiGraphics.drawString(font, value, x, y, color, false);
    }

    private Component toggleText(String key, boolean enabled) {
        return Component.translatable(key + (enabled ? ".on" : ".off"));
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
}
