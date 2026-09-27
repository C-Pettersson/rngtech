package com.rngtech.client.screen;

import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int HEAT = 0xFFE0712F;
    private static final int FAILURE = 0xFFB45B4A;
    private static final int PROGRESS = 0xFFB87832;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STATUS_WARN = 0xFFAA7A31;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int STAT_ACCENT = 0xFFB87832;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_BAR_X = 20;
    private static final int HEAT_BAR_X = 34;
    private static final int FAILURE_BAR_X = 188;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int PROGRESS_X = 98;
    private static final int PROGRESS_Y = 51;
    private static final int PROGRESS_WIDTH = 88;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_ICON_X = 129;
    private static final int HEAT_ICON_X = 145;
    private static final int STATUS_ICON_Y = 63;
    private static final int STATUS_ICON_SIZE = 12;
    private static final int OUTPUT_SLOT_X = 204;
    private static final int OUTPUT_SLOT_Y = 42;
    private static final int[] INPUT_X = {50, 70, 50, 70};
    private static final int[] INPUT_Y = {31, 31, 51, 51};
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.max_temperature",
            "rngtech.stat.warmup_time",
            "rngtech.stat.cooling_rate",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.overheat_tolerance",
            "rngtech.stat.stability",
            "rngtech.stat.input_slots"
    };
    private static final int[] STAT_DATA_INDICES = {
            AlloyFurnaceMenu.processingSpeedDataIndex(),
            AlloyFurnaceMenu.energyUsageDataIndex(),
            AlloyFurnaceMenu.energyCapacityStatDataIndex(),
            AlloyFurnaceMenu.heatTransferDataIndex(),
            AlloyFurnaceMenu.maxTemperatureDataIndex(),
            AlloyFurnaceMenu.warmupTimeDataIndex(),
            AlloyFurnaceMenu.coolingRateDataIndex(),
            AlloyFurnaceMenu.temperatureStabilityDataIndex(),
            AlloyFurnaceMenu.overheatToleranceDataIndex(),
            AlloyFurnaceMenu.stabilityDataIndex(),
            AlloyFurnaceMenu.inputSlotsDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.HEAT_TRANSFER,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.WARMUP_TIME,
            MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OVERHEAT_TOLERANCE,
            MachineStat.STABILITY,
            MachineStat.INPUT_SLOTS
    };

    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(AlloyFurnaceMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(AlloyFurnaceMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(AlloyFurnaceMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(AlloyFurnaceMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            for (int slot = 0; slot < menu.activeInputSlots(); slot++) {
                renderSlotFrame(guiGraphics, INPUT_X[slot] - 1, INPUT_Y[slot] - 1);
            }
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X - 1, OUTPUT_SLOT_Y - 1);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 43, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 139, 47);
            renderSlotFrame(guiGraphics, 187, 47);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_WIDTH, y + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + PROGRESS_X + 1,
                y + PROGRESS_Y + 1,
                x + PROGRESS_X + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_Y + PROGRESS_HEIGHT - 1,
                PROGRESS
        );

        guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(BAR_FILL_HEIGHT * menu.energyProgress());
        guiGraphics.fill(x + ENERGY_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - energyHeight, x + ENERGY_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, ENERGY);

        guiGraphics.fill(x + HEAT_BAR_X, y + BAR_Y, x + HEAT_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int heatHeight = Math.round(BAR_FILL_HEIGHT * menu.heatProgress());
        guiGraphics.fill(x + HEAT_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - heatHeight, x + HEAT_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, HEAT);

        if (menu.hasFailureRecipe()) {
            guiGraphics.fill(x + FAILURE_BAR_X, y + BAR_Y, x + FAILURE_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
            int failureHeight = Math.round(BAR_FILL_HEIGHT * menu.failureProgress());
            guiGraphics.fill(
                    x + FAILURE_BAR_X + 1,
                    y + BAR_Y + BAR_HEIGHT - 1 - failureHeight,
                    x + FAILURE_BAR_X + BAR_WIDTH - 1,
                    y + BAR_Y + BAR_HEIGHT - 1,
                    FAILURE
            );
        }

        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + STATUS_ICON_Y + 3, x + STATUS_ICON_X + 9, y + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, HEAT_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + HEAT_ICON_X + 3, y + STATUS_ICON_Y + 3, x + HEAT_ICON_X + 9, y + STATUS_ICON_Y + 9, heatGateColor());
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

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.inputs"), 69, 78);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output"), OUTPUT_SLOT_X + 8, 28);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 53);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.crucible.short"), 101);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 149);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 197);
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

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_PROCESSING) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_BAR_X,
                BAR_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                HEAT_BAR_X,
                BAR_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                heatTooltip()
        );
        if (menu.hasFailureRecipe()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    FAILURE_BAR_X,
                    BAR_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    failureTooltip()
            );
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PROGRESS_X,
                PROGRESS_Y,
                PROGRESS_WIDTH,
                PROGRESS_HEIGHT,
                progressTooltip()
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
                STATUS_ICON_SIZE,
                STATUS_ICON_SIZE,
                Component.translatable("rngtech.alloy_furnace.tooltip.status", statusText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                HEAT_ICON_X,
                STATUS_ICON_Y,
                STATUS_ICON_SIZE,
                STATUS_ICON_SIZE,
                heatTooltip()
        );
    }

    private Component heatTooltip() {
        return Component.translatable(
                "rngtech.alloy_furnace.tooltip.heat",
                menu.heat(),
                menu.minimumTemperature(),
                menu.targetTemperature(),
                menu.safeMaximumTemperature(),
                menu.overheatTemperature()
        );
    }

    private Component failureTooltip() {
        Component base = Component.translatable("rngtech.alloy_furnace.tooltip.failure", menu.failureStrain());
        return menu.powerSensitiveActive()
                ? base.copy().append(Component.literal(" ")).append(Component.translatable("rngtech.alloy_furnace.tooltip.power_drop"))
                : base;
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        return ticks <= 0
                ? Component.translatable("rngtech.alloy_furnace.tooltip.progress.empty")
                : Component.translatable("rngtech.alloy_furnace.tooltip.progress", menu.progress(), ticks, CompactValueText.energyRate(menu.energyPerTick()));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS) {
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

    private Component statusText() {
        return switch (menu.status()) {
            case AlloyFurnaceBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.alloy_furnace.status.missing_heat_core");
            case AlloyFurnaceBlockEntity.STATUS_MISSING_CRUCIBLE -> Component.translatable("rngtech.alloy_furnace.status.missing_crucible");
            case AlloyFurnaceBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.alloy_furnace.status.no_input");
            case AlloyFurnaceBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.alloy_furnace.status.invalid_recipe");
            case AlloyFurnaceBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.alloy_furnace.status.blocked_stage");
            case AlloyFurnaceBlockEntity.STATUS_HEAT_LOW -> Component.translatable("rngtech.alloy_furnace.status.heat_low", menu.minimumTemperature());
            case AlloyFurnaceBlockEntity.STATUS_STABILITY_LOW -> Component.translatable("rngtech.alloy_furnace.status.stability_low");
            case AlloyFurnaceBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.alloy_furnace.status.output_full");
            case AlloyFurnaceBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.alloy_furnace.status.no_power");
            case AlloyFurnaceBlockEntity.STATUS_WARMING -> Component.translatable("rngtech.alloy_furnace.status.warming", menu.targetTemperature());
            case AlloyFurnaceBlockEntity.STATUS_POWER_DROP -> Component.translatable("rngtech.alloy_furnace.status.power_drop");
            case AlloyFurnaceBlockEntity.STATUS_FAILURE_RISK -> Component.translatable("rngtech.alloy_furnace.status.failure_risk", menu.failureStrain());
            default -> Component.translatable("rngtech.alloy_furnace.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case AlloyFurnaceBlockEntity.STATUS_READY -> STATUS_READY;
            case AlloyFurnaceBlockEntity.STATUS_NO_POWER,
                    AlloyFurnaceBlockEntity.STATUS_NO_INPUT,
                    AlloyFurnaceBlockEntity.STATUS_MISSING_HEAT_CORE,
                    AlloyFurnaceBlockEntity.STATUS_MISSING_CRUCIBLE,
                    AlloyFurnaceBlockEntity.STATUS_HEAT_LOW,
                    AlloyFurnaceBlockEntity.STATUS_WARMING,
                    AlloyFurnaceBlockEntity.STATUS_POWER_DROP,
                    AlloyFurnaceBlockEntity.STATUS_FAILURE_RISK -> STATUS_WARN;
            default -> STATUS_ERROR;
        };
    }

    private int heatGateColor() {
        return menu.targetTemperature() <= 0
                        || menu.heat() >= menu.targetTemperature()
                ? STATUS_READY
                : STATUS_WARN;
    }

    private MachineScreenStyle.StatLine[] statLines() {
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[STAT_LABEL_KEYS.length];
        for (int index = 0; index < STAT_LABEL_KEYS.length; index++) {
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    statValue(index),
                    value,
                    isIntegralStat(STAT_DATA_INDICES[index]),
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            );
        }
        return statLines;
    }

    private String statValue(int index) {
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == AlloyFurnaceMenu.energyCapacityStatDataIndex()
                || dataIndex == AlloyFurnaceMenu.maxTemperatureDataIndex()
                || dataIndex == AlloyFurnaceMenu.inputSlotsDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == AlloyFurnaceMenu.energyUsageDataIndex()
                || dataIndex == AlloyFurnaceMenu.warmupTimeDataIndex()
                || dataIndex == AlloyFurnaceMenu.coolingRateDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == AlloyFurnaceMenu.energyCapacityStatDataIndex()
                || dataIndex == AlloyFurnaceMenu.maxTemperatureDataIndex()
                || dataIndex == AlloyFurnaceMenu.inputSlotsDataIndex()) {
            return false;
        }
        return value > 1.001;
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

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + STATUS_ICON_SIZE, topPos + y + STATUS_ICON_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + STATUS_ICON_SIZE - 1, topPos + y + STATUS_ICON_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + STATUS_ICON_SIZE - 2, topPos + y + STATUS_ICON_SIZE - 2, PANEL_DARK);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 48, TEXT_MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
