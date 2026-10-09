package com.rngtech.client.screen;

import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.menu.BatteryAssemblerMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BatteryAssemblerScreen extends AbstractContainerScreen<BatteryAssemblerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FLUID = 0xFF55AFC0;
    private static final int PROGRESS = 0xFF667F51;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STATUS_WARN = 0xFFAA7A31;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int STAT_ACCENT = 0xFF4D92A3;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_BAR_X = 8;
    private static final int FLUID_BAR_X = 24;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int FLUID_BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int PROGRESS_X = 147;
    private static final int PROGRESS_Y = 50;
    private static final int PROGRESS_WIDTH = 34;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_X = 147;
    private static final int TRANSFER_X = 163;
    private static final int STATUS_Y = 66;
    private static final int STATUS_SIZE = 12;
    private static final int[] INPUT_SLOT_X = {56, 78, 100, 122};
    private static final int INPUT_SLOT_Y = 28;
    private static final int FLUID_SLOT_X = 56;
    private static final int FLUID_SLOT_Y = 74;
    private static final int[] OUTPUT_SLOT_X = {188, 210, 188, 210};
    private static final int[] OUTPUT_SLOT_Y = {28, 28, 50, 50};
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.fluid_transfer"
    };
    private static final int[] STAT_DATA_INDICES = {
            BatteryAssemblerMenu.processingSpeedDataIndex(),
            BatteryAssemblerMenu.energyUsageDataIndex(),
            BatteryAssemblerMenu.energyCapacityStatDataIndex(),
            BatteryAssemblerMenu.fluidTransferDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.FLUID_TRANSFER
    };

    public BatteryAssemblerScreen(BatteryAssemblerMenu menu, Inventory playerInventory, Component title) {
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

        if (menu.selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != BatteryAssemblerMenu.TAB_CONFIGURATION) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(BatteryAssemblerMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(BatteryAssemblerMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(BatteryAssemblerMenu.TAB_CONFIGURATION);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(BatteryAssemblerMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == BatteryAssemblerMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == BatteryAssemblerMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == BatteryAssemblerMenu.TAB_CONFIGURATION);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == BatteryAssemblerMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == BatteryAssemblerMenu.TAB_PROCESSING) {
            for (int x : INPUT_SLOT_X) {
                renderSlotFrame(guiGraphics, x, INPUT_SLOT_Y);
            }
            renderSlotFrame(guiGraphics, FLUID_SLOT_X, FLUID_SLOT_Y);
            for (int index = 0; index < OUTPUT_SLOT_X.length; index++) {
                renderSlotFrame(guiGraphics, OUTPUT_SLOT_X[index], OUTPUT_SLOT_Y[index]);
            }
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 111, 47);
        } else if (menu.selectedTab() == BatteryAssemblerMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != BatteryAssemblerMenu.TAB_CONFIGURATION) {
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

        guiGraphics.fill(x + FLUID_BAR_X, y + BAR_Y, x + FLUID_BAR_X + FLUID_BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int fluidHeight = Math.round(BAR_FILL_HEIGHT * menu.fluidProgress());
        guiGraphics.fill(
                x + FLUID_BAR_X + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - fluidHeight,
                x + FLUID_BAR_X + FLUID_BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                FLUID
        );

        renderIconBox(guiGraphics, STATUS_X, STATUS_Y);
        guiGraphics.fill(x + STATUS_X + 3, y + STATUS_Y + 3, x + STATUS_X + 9, y + STATUS_Y + 9, statusColor());
        renderIconBox(guiGraphics, TRANSFER_X, STATUS_Y);
        guiGraphics.fill(x + TRANSFER_X + 3, y + STATUS_Y + 3, x + TRANSFER_X + 9, y + STATUS_Y + 9, transferColor());
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, FLUID_BAR_X + 3, BAR_Y + 2, menu.fluid() > 0);
    }

    private void renderConfiguration(GuiGraphics guiGraphics) {
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
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.inputs"), 99, 18);
        drawCentered(guiGraphics, Component.translatable("rngtech.jei.fluid.short"), FLUID_BAR_X + FLUID_BAR_WIDTH / 2, 18);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.outputs"), 209, 18);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawCentered(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 120, 35);
    }

    private void drawConfigurationLabels(GuiGraphics guiGraphics) {
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

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BatteryAssemblerMenu.TAB_PROCESSING) {
            return;
        }
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FLUID_BAR_X + 3,
                BAR_Y + 2,
                Component.translatable("rngtech.purge.target.assembly_fluid"),
                menu.fluid() > 0 ? menu.fluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.fluid(),
                menu.fluidCapacity()
        )) {
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
                FLUID_BAR_X,
                BAR_Y,
                FLUID_BAR_WIDTH,
                BAR_HEIGHT,
                fluidTooltip()
        );
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
                STATUS_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                statusText()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                TRANSFER_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable("rngtech.battery_assembler.fluid_input_rate", menu.fluidTransfer())
        );
    }

    private Component fluidTooltip() {
        return FluidMeterTooltips.amount(menu.fluidName(), menu.fluid(), menu.fluidCapacity());
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FLUID_BAR_X + 3,
                BAR_Y + 2,
                BatteryAssemblerBlockEntity.PURGE_ASSEMBLY_FLUID
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        return ticks <= 0
                ? Component.literal("Progress: -- / --")
                : Component.literal("Progress: " + menu.progress() + " / " + ticks);
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BatteryAssemblerMenu.TAB_CONFIGURATION) {
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
            case BatteryAssemblerBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.battery_assembler.status.no_input");
            case BatteryAssemblerBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.battery_assembler.status.invalid_recipe");
            case BatteryAssemblerBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.battery_assembler.status.no_fluid");
            case BatteryAssemblerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.battery_assembler.status.output_full");
            case BatteryAssemblerBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.battery_assembler.status.no_power");
            default -> Component.translatable("rngtech.battery_assembler.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case BatteryAssemblerBlockEntity.STATUS_READY -> STATUS_READY;
            case BatteryAssemblerBlockEntity.STATUS_NO_POWER,
                    BatteryAssemblerBlockEntity.STATUS_NO_FLUID,
                    BatteryAssemblerBlockEntity.STATUS_NO_INPUT -> STATUS_WARN;
            default -> STATUS_ERROR;
        };
    }

    private int transferColor() {
        return menu.fluidTransfer() > 0 ? STATUS_READY : STATUS_WARN;
    }

    private String statValue(int index) {
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
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

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == BatteryAssemblerMenu.energyCapacityStatDataIndex()
                || dataIndex == BatteryAssemblerMenu.fluidTransferDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == BatteryAssemblerMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == BatteryAssemblerMenu.energyCapacityStatDataIndex()
                || dataIndex == BatteryAssemblerMenu.fluidTransferDataIndex()) {
            return false;
        }
        return value > 1.001;
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + STATUS_SIZE, topPos + y + STATUS_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + STATUS_SIZE - 1, topPos + y + STATUS_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + STATUS_SIZE - 2, topPos + y + STATUS_SIZE - 2, PANEL_DARK);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
