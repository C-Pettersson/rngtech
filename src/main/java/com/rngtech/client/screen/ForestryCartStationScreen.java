package com.rngtech.client.screen;

import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.menu.ForestryCartStationMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ForestryCartStationScreen extends AbstractContainerScreen<ForestryCartStationMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FORESTRY = 0xFF4E8B54;
    private static final int WARNING = 0xFFB45B4A;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_GAUGE_X = 184;
    private static final int ENERGY_GAUGE_Y = 30;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int STATUS_ICON_X = 111;
    private static final int STATUS_ICON_Y = 69;
    private static final int ACTION_ICON_X = 128;
    private static final int ACTION_ICON_Y = 69;
    private static final int HOLD_BUTTON_X = 145;
    private static final int HOLD_BUTTON_Y = 66;
    private static final int CONTROL_BUTTON_WIDTH = 30;
    private static final int CONTROL_BUTTON_HEIGHT = 12;
    private static final int ICON_SIZE = 12;

    public ForestryCartStationScreen(ForestryCartStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else {
            renderStats(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != ForestryCartStationMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            guiGraphics.drawString(font, Component.translatable("rngtech.forestry_station.saplings"), 39, 28, TEXT_MUTED, false);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.shears.short"), 83, 28, 28, TEXT_MUTED);
            guiGraphics.drawString(font, Component.translatable("rngtech.forestry_station.output"), 101, 19, TEXT_MUTED, false);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.station_gear"), 119, 30, 120, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.station_cell.short"), 80, 39, 24, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.energy_connector.short"), 104, 39, 24, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.item_port.short"), 128, 39, 24, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.fluid_port.short"), 152, 39, 28, TEXT_MUTED);
        } else {
            drawStatsLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(ForestryCartStationMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(ForestryCartStationMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(ForestryCartStationMenu.TAB_STATS);
                return true;
            }
            if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING
                    && inBounds(mouseX, mouseY, HOLD_BUTTON_X, HOLD_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT)) {
                sendButton(ForestryCartStationMenu.BUTTON_TOGGLE_HOLD);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.station"), menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ForestryCartStationMenu.TAB_STATS);
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
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, x + TAB_WIDTH / 2, y + 7, TAB_WIDTH - 8, selected ? TEXT : 0xFF2F2F2F);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, 46, 38);
            renderSlotFrame(guiGraphics, 73, 38);
            for (int slot = 0; slot < ForestryCartStationBlockEntity.OUTPUT_SLOT_COUNT; slot++) {
                renderSlotFrame(guiGraphics, 100 + (slot % 3) * 18, 29 + (slot / 3) * 18);
            }
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 70, 51);
            renderSlotFrame(guiGraphics, 94, 51);
            renderSlotFrame(guiGraphics, 118, 51);
            renderSlotFrame(guiGraphics, 142, 51);
        }

        if (menu.selectedTab() == ForestryCartStationMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + ENERGY_GAUGE_X, y + ENERGY_GAUGE_Y, x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH, y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );
        renderStatusIcons(guiGraphics);
        renderControlButtons(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, 91, 77);
        int x = leftPos + 91;
        int y = topPos + 77;
        guiGraphics.fill(x + 3, y + 8, x + 9, y + 10, FORESTRY);
        guiGraphics.fill(x + 4, y + 5, x + 8, y + 7, FORESTRY);
        guiGraphics.fill(x + 5, y + 2, x + 7, y + 4, FORESTRY);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, FORESTRY);
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawStatPanelLabels(
                guiGraphics,
                font,
                Component.translatable("rngtech.tab.stats"),
                statLines(),
                8,
                18,
                224,
                FORESTRY
        );
    }

    private MachineScreenStyle.StatLine[] statLines() {
        return new MachineScreenStyle.StatLine[] {
                stat("rngtech.forestry_station.stat.energy_transfer", CompactValueText.energyAmount(menu.energyTransfer())),
                stat("rngtech.forestry_station.stat.item_transfer", Integer.toString(menu.itemTransfer())),
                stat("rngtech.forestry_station.stat.fluid_transfer", menu.fluidTransfer() + " mB"),
                stat("rngtech.forestry_station.stat.docked_cart", menu.cartDocked()
                        ? Component.translatable("rngtech.forestry_station.cart_status.docked").getString()
                        : Component.translatable("rngtech.forestry_station.cart_status.none").getString())
        };
    }

    private MachineScreenStyle.StatLine stat(String labelKey, String value) {
        return MachineScreenStyle.statLine(Component.translatable(labelKey), value, 0.0D, false, false, Component.translatable(labelKey));
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_MISSING_RAIL -> Component.translatable("rngtech.forestry_station.status.missing_rail");
            case ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> Component.translatable("rngtech.forestry_station.status.holding_cart");
            default -> Component.translatable("rngtech.forestry_station.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_READY -> FORESTRY;
            case ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> PANEL_DARK;
            default -> WARNING;
        };
    }

    private Component actionComponent() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER -> Component.translatable("rngtech.forestry_station.action.dock_transfer");
            case ForestryCartStationBlockEntity.ACTION_HOLDING -> Component.translatable("rngtech.forestry_station.action.holding");
            case ForestryCartStationBlockEntity.ACTION_DOCKED_READY -> Component.translatable("rngtech.forestry_station.action.docked_ready");
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> Component.translatable("rngtech.forestry_station.action.setup_blocked");
            default -> Component.translatable("rngtech.forestry_station.action.idle");
        };
    }

    private int actionColor() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER,
                    ForestryCartStationBlockEntity.ACTION_DOCKED_READY -> 0xFF5F7DA8;
            case ForestryCartStationBlockEntity.ACTION_HOLDING -> PANEL_DARK;
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> WARNING;
            default -> PANEL_DARK;
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    ENERGY_GAUGE_X,
                    ENERGY_GAUGE_Y,
                    ENERGY_GAUGE_WIDTH,
                    ENERGY_GAUGE_HEIGHT,
                    Component.translatable("rngtech.forestry_station.tooltip.energy", CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()))
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    STATUS_ICON_X,
                    STATUS_ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE,
                    Component.translatable("rngtech.forestry_station.tooltip.status", statusComponent())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    ACTION_ICON_X,
                    ACTION_ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE,
                    Component.translatable("rngtech.forestry_station.tooltip.action", actionComponent())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    HOLD_BUTTON_X,
                    HOLD_BUTTON_Y,
                    CONTROL_BUTTON_WIDTH,
                    CONTROL_BUTTON_HEIGHT,
                    Component.translatable(menu.holdCartAtStation()
                            ? "rngtech.forestry_station.tooltip.hold_enabled"
                            : "rngtech.forestry_station.tooltip.hold_disabled")
            );
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 8, 18, 224, statLines());
        }
    }

    private void renderStatusIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, ACTION_ICON_X, ACTION_ICON_Y);
        guiGraphics.fill(leftPos + ACTION_ICON_X + 3, topPos + ACTION_ICON_Y + 3, leftPos + ACTION_ICON_X + 9, topPos + ACTION_ICON_Y + 9, actionColor());
    }

    private void renderControlButtons(GuiGraphics guiGraphics) {
        renderButton(guiGraphics, HOLD_BUTTON_X, HOLD_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, menu.holdCartAtStation());
        MachineScreenStyle.drawClippedCentered(
                guiGraphics,
                font,
                Component.translatable(menu.holdCartAtStation()
                        ? "rngtech.forestry_station.control.hold_on"
                        : "rngtech.forestry_station.control.hold_off"),
                leftPos + HOLD_BUTTON_X + CONTROL_BUTTON_WIDTH / 2,
                topPos + HOLD_BUTTON_Y + 3,
                CONTROL_BUTTON_WIDTH - 4,
                menu.holdCartAtStation() ? TEXT : TEXT_MUTED
        );
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, selected ? 0xFF8CBF8F : 0xFFB8B8B8);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, selected ? 0xFFA9D3AC : PANEL_LIGHT);
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
