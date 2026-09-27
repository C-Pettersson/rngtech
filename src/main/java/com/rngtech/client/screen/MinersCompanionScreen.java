package com.rngtech.client.screen;

import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.minerscompanion.MinersCompanionState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MinersCompanionScreen extends AbstractContainerScreen<MinersCompanionMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int BUTTON = 0xFF9A9A9A;
    private static final int BUTTON_DARK = 0xFF5F5F5F;
    private static final int BUTTON_LIGHT = 0xFFE9E9E9;
    private static final int SELECTED = 0xFF7F7F7F;
    private static final int SELECTED_LIGHT = 0xFFCFCFCF;
    private static final int ENERGY = 0xFFE0E0E0;
    private static final int ENERGY_EMPTY = 0xFF4F4F4F;
    private static final int STATUS_READY = 0xFF65B26A;
    private static final int STATUS_WARN = 0xFFD3A33A;
    private static final int STATUS_BLOCKED = 0xFFB45B4A;
    private static final int SLOT = 0xFFB8B8B8;
    private static final int SLOT_DARK = 0xFF5F5F5F;
    private static final int SLOT_DISABLED = 0xFF777777;
    private static final int TAB_WIDTH = 52;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_X = 18;
    private static final int ENERGY_Y = 34;
    private static final int ENERGY_WIDTH = 8;
    private static final int ENERGY_HEIGHT = 55;
    private static final int FILTER_X = 40;
    private static final int FILTER_Y = 34;
    private static final int TOGGLE_X = 132;
    private static final int BLOCK_CHEW_TOGGLE_Y = 34;
    private static final int MAGNET_TOGGLE_Y = 52;
    private static final int LAMP_TOGGLE_Y = 70;
    private static final int TOGGLE_WIDTH = 34;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int STATUS_X = 143;
    private static final int STATUS_Y = 89;
    private static final int STATUS_SIZE = 12;
    private static final int STAT_PANEL_X = 18;
    private static final int STAT_PANEL_Y = 32;
    private static final int STAT_PANEL_WIDTH = 140;
    private static final int STAT_PANEL_HEIGHT = 68;
    private static final int STAT_ROW_START_Y = 36;
    private static final int STAT_ROW_HEIGHT = 10;
    private static final int STAT_LABEL_X = 26;
    private static final int STAT_VALUE_RIGHT = 150;

    public MinersCompanionScreen(MinersCompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 200;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 104;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderCustomTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        switch (menu.selectedTab()) {
            case MinersCompanionMenu.TAB_GEAR -> renderGearBg(guiGraphics);
            case MinersCompanionMenu.TAB_STATS -> renderStatsBg(guiGraphics);
            default -> renderProcessBg(guiGraphics);
        }
        renderPlayerInventorySlots(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        switch (menu.selectedTab()) {
            case MinersCompanionMenu.TAB_GEAR -> renderGearLabels(guiGraphics);
            case MinersCompanionMenu.TAB_STATS -> renderStatsLabels(guiGraphics);
            default -> renderProcessLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                sendButton(MinersCompanionMenu.BUTTON_TAB_BASE + MinersCompanionMenu.TAB_PROCESS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                sendButton(MinersCompanionMenu.BUTTON_TAB_BASE + MinersCompanionMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                sendButton(MinersCompanionMenu.BUTTON_TAB_BASE + MinersCompanionMenu.TAB_STATS);
                return true;
            }
            if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                    && menu.canToggleBlockChew()
                    && inBounds(mouseX, mouseY, TOGGLE_X, BLOCK_CHEW_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
                sendButton(MinersCompanionMenu.BUTTON_TOGGLE_BLOCK_CHEW);
                return true;
            }
            if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                    && menu.canToggleMagnet()
                    && inBounds(mouseX, mouseY, TOGGLE_X, MAGNET_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
                sendButton(MinersCompanionMenu.BUTTON_TOGGLE_MAGNET);
                return true;
            }
            if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                    && menu.canToggleMiningLamp()
                    && inBounds(mouseX, mouseY, TOGGLE_X, LAMP_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
                sendButton(MinersCompanionMenu.BUTTON_TOGGLE_MINING_LAMP);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderProcessBg(GuiGraphics guiGraphics) {
        renderEnergyMeter(guiGraphics);
        for (int index = 0; index < MinersCompanionState.FILTER_SLOT_COUNT; index++) {
            int row = index / 5;
            int column = index % 5;
            renderSlot(guiGraphics, FILTER_X + column * 18, FILTER_Y + row * 18, index >= menu.activeFilterSlots());
        }
        renderStatusSquare(guiGraphics);
        renderButton(guiGraphics, TOGGLE_X, BLOCK_CHEW_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, menu.blockChewEnabled(), menu.canToggleBlockChew());
        renderButton(guiGraphics, TOGGLE_X, MAGNET_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, menu.magnetEnabled(), menu.canToggleMagnet());
        renderButton(guiGraphics, TOGGLE_X, LAMP_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT, menu.miningLampEnabled(), menu.canToggleMiningLamp());
    }

    private void renderGearBg(GuiGraphics guiGraphics) {
        renderSlot(guiGraphics, 7, 47, false);
        renderSlot(guiGraphics, 41, 47, false);
        renderSlot(guiGraphics, 75, 47, false);
        renderSlot(guiGraphics, 109, 47, false);
        renderSlot(guiGraphics, 143, 47, false);
    }

    private void renderStatsBg(GuiGraphics guiGraphics) {
        renderInset(guiGraphics, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, STAT_PANEL_HEIGHT);
        for (int index = 0; index < 6; index++) {
            int rowY = topPos + STAT_ROW_START_Y + index * STAT_ROW_HEIGHT;
            int color = index % 2 == 0 ? 0xFFBDBDBD : 0xFFCFCFCF;
            guiGraphics.fill(
                    leftPos + STAT_PANEL_X + 6,
                    rowY,
                    leftPos + STAT_PANEL_X + STAT_PANEL_WIDTH - 6,
                    rowY + 10,
                    color
            );
        }
    }

    private void renderProcessLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.miners_companion.filters"), FILTER_X, 24, TEXT_MUTED, false);
        drawCentered(
                guiGraphics,
                Component.translatable("rngtech.miners_companion.toggle.chew"),
                TOGGLE_X,
                BLOCK_CHEW_TOGGLE_Y + 3,
                TOGGLE_WIDTH,
                menu.canToggleBlockChew() ? 0xFFFFFFFF : TEXT_MUTED
        );
        drawCentered(
                guiGraphics,
                Component.translatable("rngtech.miners_companion.toggle.magnet"),
                TOGGLE_X,
                MAGNET_TOGGLE_Y + 3,
                TOGGLE_WIDTH,
                menu.canToggleMagnet() ? 0xFFFFFFFF : TEXT_MUTED
        );
        drawCentered(
                guiGraphics,
                Component.translatable("rngtech.miners_companion.toggle.lamp"),
                TOGGLE_X,
                LAMP_TOGGLE_Y + 3,
                TOGGLE_WIDTH,
                menu.canToggleMiningLamp() ? 0xFFFFFFFF : TEXT_MUTED
        );
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.miners_companion.filter_count", menu.activeFilterSlots(), MinersCompanionItem.MAX_FILTER_SLOTS),
                FILTER_X,
                76,
                TEXT,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.miners_companion.energy_cost", menu.energyCostPerItem()),
                FILTER_X,
                87,
                TEXT_MUTED,
                false
        );
    }

    private void renderGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.miners_companion.gear.head"), 17);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.miners_companion.gear.cell"), 51);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.miners_companion.gear.filter"), 85);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.miners_companion.gear.magnet"), 119);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.miners_companion.gear.lamp"), 153);
    }

    private void renderStatsLabels(GuiGraphics guiGraphics) {
        int y = STAT_ROW_START_Y + 1;
        drawStat(guiGraphics, y, "rngtech.miners_companion.stat.filters", Integer.toString(menu.activeFilterSlots()));
        y += STAT_ROW_HEIGHT;
        drawStat(
                guiGraphics,
                y,
                "rngtech.miners_companion.stat.energy",
                CompactValueText.energyAmountPair(menu.storedEnergy(), menu.energyCapacity())
        );
        y += STAT_ROW_HEIGHT;
        drawStat(guiGraphics, y, "rngtech.miners_companion.stat.cost", CompactValueText.energyAmount(menu.energyCostPerItem()));
        y += STAT_ROW_HEIGHT;
        drawStat(guiGraphics, y, "rngtech.miners_companion.stat.recovery", recoveryText());
        y += STAT_ROW_HEIGHT;
        drawStat(guiGraphics, y, "rngtech.miners_companion.stat.magnet", magnetText());
        y += STAT_ROW_HEIGHT;
        drawStat(guiGraphics, y, "rngtech.miners_companion.stat.lamp", CompactValueText.energyAmount(menu.miningLampCost()) + "/t");
    }

    private void drawStat(GuiGraphics guiGraphics, int y, String labelKey, String value) {
        String clippedValue = font.plainSubstrByWidth(value, 70);
        int valueX = STAT_VALUE_RIGHT - font.width(clippedValue);
        int labelWidth = Math.max(0, valueX - STAT_LABEL_X - 4);
        String label = font.plainSubstrByWidth(Component.translatable(labelKey).getString(), labelWidth);
        guiGraphics.drawString(font, label, STAT_LABEL_X, y, TEXT_MUTED, false);
        guiGraphics.drawString(font, clippedValue, valueX, y, TEXT, false);
    }

    private String recoveryText() {
        return menu.state().recoveryFilter().isEmpty() ? "--" : "1/5000";
    }

    private String magnetText() {
        return CompactValueText.energyAmount(menu.magnetPassiveCost()) + "/t+"
                + CompactValueText.energyAmount(menu.magnetItemCost()) + "/drop";
    }

    private void renderEnergyMeter(GuiGraphics guiGraphics) {
        int left = leftPos + ENERGY_X;
        int top = topPos + ENERGY_Y;
        guiGraphics.fill(left, top, left + ENERGY_WIDTH, top + ENERGY_HEIGHT, SLOT_DARK);
        guiGraphics.fill(left + 1, top + 1, left + ENERGY_WIDTH - 1, top + ENERGY_HEIGHT - 1, ENERGY_EMPTY);
        int capacity = menu.energyCapacity();
        if (capacity <= 0) {
            return;
        }
        int filled = Math.round((ENERGY_HEIGHT - 2) * menu.storedEnergy() / (float) capacity);
        guiGraphics.fill(left + 1, top + ENERGY_HEIGHT - 1 - filled, left + ENERGY_WIDTH - 1, top + ENERGY_HEIGHT - 1, ENERGY);
    }

    private void renderStatusSquare(GuiGraphics guiGraphics) {
        int left = leftPos + STATUS_X;
        int top = topPos + STATUS_Y;
        guiGraphics.fill(left, top, left + STATUS_SIZE, top + STATUS_SIZE, 0xFF373737);
        guiGraphics.fill(left + 2, top + 2, left + STATUS_SIZE - 2, top + STATUS_SIZE - 2, statusColor());
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case MinersCompanionMenu.STATUS_READY -> STATUS_READY;
            case MinersCompanionMenu.STATUS_NO_POWER -> STATUS_BLOCKED;
            case MinersCompanionMenu.STATUS_MISSING_HEAD, MinersCompanionMenu.STATUS_MISSING_CELL -> STATUS_WARN;
            default -> PANEL_DARK;
        };
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case MinersCompanionMenu.STATUS_READY -> Component.translatable("rngtech.miners_companion.status.ready");
            case MinersCompanionMenu.STATUS_MISSING_HEAD -> Component.translatable("rngtech.miners_companion.status.missing_head");
            case MinersCompanionMenu.STATUS_MISSING_CELL -> Component.translatable("rngtech.miners_companion.status.missing_cell");
            case MinersCompanionMenu.STATUS_NO_FILTERS -> Component.translatable("rngtech.miners_companion.status.no_filters");
            case MinersCompanionMenu.STATUS_NO_POWER -> Component.translatable("rngtech.miners_companion.status.no_power");
            default -> Component.translatable("rngtech.miners_companion.status.disabled");
        };
    }

    private void renderPlayerInventorySlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlot(guiGraphics, 7 + column * 18, 115 + row * 18, false);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlot(guiGraphics, 7 + column * 18, 173, false);
        }
    }

    private void renderCustomTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                && inBounds(mouseX, mouseY, STATUS_X, STATUS_Y, STATUS_SIZE, STATUS_SIZE)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.miners_companion.tooltip.status", statusComponent()),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                && inBounds(mouseX, mouseY, ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable("rngtech.miners_companion.tooltip.energy", menu.storedEnergy(), menu.energyCapacity()),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                && inBounds(mouseX, mouseY, TOGGLE_X, BLOCK_CHEW_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            if (!menu.canToggleBlockChew()) {
                guiGraphics.renderTooltip(
                        font,
                        Component.translatable("rngtech.miners_companion.tooltip.block_chew_toggle_locked"),
                        mouseX,
                        mouseY
                );
                return;
            }
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "rngtech.miners_companion.tooltip.block_chew_toggle",
                            toggleState(menu.blockChewEnabled()),
                            menu.energyCostPerItem()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                && inBounds(mouseX, mouseY, TOGGLE_X, MAGNET_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            if (!menu.canToggleMagnet()) {
                guiGraphics.renderTooltip(
                        font,
                        Component.translatable("rngtech.miners_companion.tooltip.magnet_toggle_locked"),
                        mouseX,
                        mouseY
                );
                return;
            }
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "rngtech.miners_companion.tooltip.magnet_toggle",
                            toggleState(menu.magnetEnabled()),
                            menu.magnetPassiveCost(),
                            menu.magnetItemCost()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS
                && inBounds(mouseX, mouseY, TOGGLE_X, LAMP_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            if (!menu.canToggleMiningLamp()) {
                guiGraphics.renderTooltip(
                        font,
                        Component.translatable("rngtech.miners_companion.tooltip.lamp_toggle_locked"),
                        mouseX,
                        mouseY
                );
                return;
            }
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(
                            "rngtech.miners_companion.tooltip.lamp_toggle",
                            toggleState(menu.miningLampEnabled()),
                            menu.miningLampCost()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS) {
            for (int index = 0; index < MinersCompanionState.FILTER_SLOT_COUNT; index++) {
                int row = index / 5;
                int column = index % 5;
                if (inBounds(mouseX, mouseY, FILTER_X + column * 18, FILTER_Y + row * 18, 18, 18)
                        && index >= menu.activeFilterSlots()) {
                    guiGraphics.renderComponentTooltip(
                            font,
                            List.of(Component.translatable("rngtech.miners_companion.tooltip.locked_filter")),
                            mouseX,
                            mouseY
                    );
                    return;
                }
            }
        }
    }

    private Component toggleState(boolean enabled) {
        return Component.translatable(enabled ? "rngtech.miners_companion.on" : "rngtech.miners_companion.off");
    }

    private void sendButton(int id) {
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

    private void renderTabs(GuiGraphics guiGraphics) {
        renderTab(guiGraphics, 0, Component.translatable("rngtech.miners_companion.tab.process"), menu.selectedTab() == MinersCompanionMenu.TAB_PROCESS);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.miners_companion.tab.gear"), menu.selectedTab() == MinersCompanionMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.miners_companion.tab.stats"), menu.selectedTab() == MinersCompanionMenu.TAB_STATS);
    }

    private void renderTab(GuiGraphics guiGraphics, int index, Component label, boolean selected) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        int color = selected ? PANEL : 0xFF9A9A9A;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, color);
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + 21, PANEL_LIGHT);
        guiGraphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + 21, PANEL_DARK);
        if (!selected) {
            guiGraphics.fill(x, y + 20, x + TAB_WIDTH, y + 21, PANEL_DARK);
        }
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, selected ? TEXT : 0xFF2F2F2F, false);
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected, boolean active) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, BUTTON_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, active ? selected ? SELECTED : BUTTON : SLOT_DISABLED);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, active ? selected ? SELECTED_LIGHT : BUTTON_LIGHT : PANEL_DARK);
    }

    private void renderInset(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFFBDBDBD);
        guiGraphics.fill(left, top, left + width, top + 1, PANEL_LIGHT);
        guiGraphics.fill(left, top, left + 1, top + height, PANEL_LIGHT);
        guiGraphics.fill(left + width - 1, top, left + width, top + height, PANEL_DARK);
        guiGraphics.fill(left, top + height - 1, left + width, top + height, PANEL_DARK);
    }

    private void renderSlot(GuiGraphics guiGraphics, int x, int y, boolean disabled) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, SLOT_DARK);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, disabled ? SLOT_DISABLED : SLOT);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 2, PANEL_LIGHT);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 68, 32, TEXT_MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int left = leftPos + 8 + index * TAB_SPACING;
        int top = topPos - 20;
        return mouseX >= left && mouseX < left + TAB_WIDTH && mouseY >= top && mouseY < top + 21;
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }
}
