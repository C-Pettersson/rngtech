package com.rngtech.client.screen;

import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BatteryChassisScreen extends AbstractContainerScreen<BatteryChassisMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF27BFAE;
    private static final int ENERGY_DARK = 0xFF4F5E5D;
    private static final int TRANSFER = 0xFFB45A28;
    private static final int STAT_ACCENT = 0xFF7F9ED7;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.battery_slots",
            "rngtech.stat.energy_transfer",
            "rngtech.stat.burst_transfer",
            "rngtech.stat.burst_duration",
            "rngtech.stat.efficiency",
            "rngtech.stat.stability",
            "rngtech.stat.idle_loss",
            "rngtech.stat.global_modifier_strength"
    };
    private static final int[] STAT_DATA_INDICES = {
            BatteryChassisMenu.cellSlotsDataIndex(),
            BatteryChassisMenu.energyTransferDataIndex(),
            BatteryChassisMenu.burstTransferDataIndex(),
            BatteryChassisMenu.burstDurationDataIndex(),
            BatteryChassisMenu.efficiencyDataIndex(),
            BatteryChassisMenu.stabilityDataIndex(),
            BatteryChassisMenu.idleLossDataIndex(),
            BatteryChassisMenu.globalModifierStrengthDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.BATTERY_SLOTS,
            MachineStat.ENERGY_TRANSFER,
            MachineStat.BURST_TRANSFER,
            MachineStat.BURST_DURATION,
            MachineStat.EFFICIENCY,
            MachineStat.STABILITY,
            MachineStat.IDLE_LOSS,
            MachineStat.GLOBAL_MODIFIER_STRENGTH
    };

    public BatteryChassisScreen(BatteryChassisMenu menu, Inventory playerInventory, Component title) {
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
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits());
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);

        if (menu.selectedTab() == BatteryChassisMenu.TAB_STATUS) {
            renderStatus(guiGraphics);
        } else if (menu.selectedTab() == BatteryChassisMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == BatteryChassisMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != BatteryChassisMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == BatteryChassisMenu.TAB_STATUS) {
            drawStatusLabels(guiGraphics);
        } else if (menu.selectedTab() == BatteryChassisMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == BatteryChassisMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(BatteryChassisMenu.TAB_STATUS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(BatteryChassisMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(BatteryChassisMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(BatteryChassisMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == BatteryChassisMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == BatteryChassisMenu.TAB_STATUS);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == BatteryChassisMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == BatteryChassisMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == BatteryChassisMenu.TAB_REFINEMENT);
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

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == BatteryChassisMenu.TAB_GEAR) {
            for (int slot = 0; slot < menu.visibleCellSlotCount(); slot++) {
                if (menu.isCellSlotVisible(slot)) {
                    renderSlotFrame(guiGraphics, BatteryChassisMenu.cellSlotX(slot) - 1, BatteryChassisMenu.cellSlotY(slot) - 1);
                }
            }
        } else if (menu.selectedTab() == BatteryChassisMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == BatteryChassisMenu.TAB_STATS) {
            return;
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void renderStatus(GuiGraphics guiGraphics) {
        renderEnergyGauge(guiGraphics);
        int x = leftPos + 52;
        int y = topPos + 26;
        guiGraphics.fill(x, y, x + 150, y + 70, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 149, y + 69, PANEL_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + 148, y + 68, 0xFF9B9B9B);
        for (int index = 0; index < 5; index++) {
            int rowY = y + 7 + index * 12;
            guiGraphics.fill(x + 7, rowY, x + 142, rowY + 9, index % 2 == 0 ? 0xFFB4B4B4 : 0xFFA9A9A9);
            guiGraphics.fill(x + 7, rowY, x + 9, rowY + 9, index < 2 ? ENERGY : TRANSFER);
        }
    }

    private void renderEnergyGauge(GuiGraphics guiGraphics) {
        int x = leftPos + 20;
        int y = topPos + 26;
        guiGraphics.fill(x, y, x + 20, y + 70, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 19, y + 69, 0xFFE0E0E0);
        guiGraphics.fill(x + 2, y + 2, x + 18, y + 68, ENERGY_DARK);

        int energyHeight = Math.round(64 * menu.energyProgress());
        guiGraphics.fill(x + 4, y + 66 - energyHeight, x + 16, y + 66, ENERGY);
        guiGraphics.fill(x + 5, y + 66 - energyHeight, x + 15, y + 68 - energyHeight, 0xFF79F4E8);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(
                guiGraphics,
                leftPos,
                topPos,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                statLines().length,
                STAT_ACCENT
        );
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawStatusLabels(GuiGraphics guiGraphics) {
        drawRow(guiGraphics, 61, 34, Component.translatable("rngtech.battery.energy"), energyText(), ENERGY);
        drawRow(guiGraphics, 61, 46, Component.translatable("rngtech.battery.fill"), fillText(), ENERGY);
        drawRow(guiGraphics, 61, 58, Component.translatable("rngtech.battery.input"), inputText(), TRANSFER);
        drawRow(guiGraphics, 61, 70, Component.translatable("rngtech.battery.output"), outputText(), TRANSFER);
        drawRow(guiGraphics, 61, 82, Component.translatable("rngtech.battery_chassis.burst"), burstText(), TRANSFER);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        Component label = Component.translatable("rngtech.gear.battery_cell.short");
        guiGraphics.drawString(font, label, 120 - font.width(label) / 2, 20, TEXT_MUTED, false);
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawStatPanelLabels(
                guiGraphics,
                font,
                Component.translatable("rngtech.tab.stats"),
                statLines(),
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                STAT_ACCENT
        );
    }

    private void drawRefinementLabels(GuiGraphics guiGraphics) {
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawRow(GuiGraphics guiGraphics, int x, int y, Component label, String value, int accent) {
        guiGraphics.drawString(font, label, x, y, TEXT_MUTED, false);
        guiGraphics.drawString(font, value, 194 - font.width(value), y, accent == ENERGY ? TEXT : 0xFF4A3426, false);
    }

    private String energyText() {
        return CompactValueText.energyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private String fillText() {
        return Math.round(menu.energyProgress() * 100.0F) + "%";
    }

    private String inputText() {
        return CompactValueText.energyRate(menu.maxInput());
    }

    private String outputText() {
        return CompactValueText.energyRate(menu.maxOutput());
    }

    private String burstText() {
        return menu.burstOutput() > 0 ? CompactValueText.energyRate(menu.burstOutput()) : "--";
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BatteryChassisMenu.TAB_STATUS) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                20,
                26,
                20,
                70,
                exactEnergyText()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                52,
                56,
                150,
                12,
                inputTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                52,
                68,
                150,
                12,
                outputTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                52,
                80,
                150,
                12,
                burstTooltip()
        );
    }

    private Component inputTooltip() {
        return Component.translatable(
                inputLimitedByCells()
                        ? "rngtech.battery_chassis.tooltip.input_cell_limited"
                        : "rngtech.battery_chassis.tooltip.input",
                CompactValueText.energyRate(menu.chassisCeiling()),
                CompactValueText.energyRate(menu.cellInputSum()),
                CompactValueText.energyRate(menu.maxInput()),
                connectorCapText(menu.inputConnectorCap()),
                CompactValueText.energyRate(menu.lastInput())
        );
    }

    private Component outputTooltip() {
        return Component.translatable(
                outputLimitedByCells()
                        ? "rngtech.battery_chassis.tooltip.output_cell_limited"
                        : "rngtech.battery_chassis.tooltip.output",
                CompactValueText.energyRate(menu.chassisCeiling()),
                CompactValueText.energyRate(menu.cellOutputSum()),
                CompactValueText.energyRate(menu.maxOutput()),
                connectorCapText(menu.outputConnectorCap()),
                CompactValueText.energyRate(menu.lastOutput())
        );
    }

    private Component burstTooltip() {
        return Component.translatable(
                connectorLimitsOutput()
                        ? "rngtech.battery_chassis.tooltip.burst_connector_limited"
                        : "rngtech.battery_chassis.tooltip.burst",
                CompactValueText.energyRate(menu.burstOutput()),
                connectorCapText(menu.outputConnectorCap()),
                CompactValueText.energyRate(menu.lastOutput())
        );
    }

    private boolean inputLimitedByCells() {
        return menu.cellInputSum() < menu.chassisCeiling();
    }

    private boolean outputLimitedByCells() {
        return menu.cellOutputSum() < menu.chassisCeiling();
    }

    private boolean connectorLimitsOutput() {
        return menu.outputConnectorCap() > 0 && menu.burstOutput() > menu.outputConnectorCap();
    }

    private String connectorCapText(int connectorCap) {
        return connectorCap > 0 ? CompactValueText.energyRate(connectorCap) : "--";
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BatteryChassisMenu.TAB_STATS) {
            return;
        }
        MachineScreenStyle.renderStatPanelTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                statLines()
        );
    }

    private String statValue(int index) {
        double value = menu.statValue(STAT_DATA_INDICES[index]);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
    }

    private MachineScreenStyle.StatLine[] statLines() {
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[STAT_LABEL_KEYS.length];
        for (int index = 0; index < STAT_LABEL_KEYS.length; index++) {
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    statValue(index),
                    value,
                    index == 0 || index == 3,
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            ).withStat(STAT_TYPES[index]);
        }
        return statLines;
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == BatteryChassisMenu.idleLossDataIndex()) {
            return false;
        }
        return value > 1.001;
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
}
