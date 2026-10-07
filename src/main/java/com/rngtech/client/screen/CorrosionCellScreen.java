package com.rngtech.client.screen;

import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CorrosionCellScreen extends AbstractContainerScreen<CorrosionCellMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF4F9F91;
    private static final int PROGRESS = 0xFF6B8F4A;
    private static final int STAT_ACCENT = 0xFF4F9F91;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int PLATE_SLOT_X = 16;
    private static final int PLATE_SLOT_Y = 32;
    private static final int ELECTROLYTE_SLOT_X = 16;
    private static final int ELECTROLYTE_SLOT_Y = 70;
    private static final int RESIDUE_SLOT_X = 175;
    private static final int RESIDUE_SLOT_Y = 70;
    private static final int ELECTROLYTE_METER_X = 42;
    private static final int ELECTROLYTE_METER_Y = 36;
    private static final int ELECTROLYTE_METER_WIDTH = 12;
    private static final int ELECTROLYTE_METER_HEIGHT = 52;
    private static final int ENERGY_METER_X = 158;
    private static final int ENERGY_METER_Y = 36;
    private static final int ENERGY_METER_WIDTH = 12;
    private static final int ENERGY_METER_HEIGHT = 52;
    private static final int PROGRESS_BAR_X = 72;
    private static final int PROGRESS_BAR_Y = 71;
    private static final int PROGRESS_BAR_WIDTH = 64;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 72;
    private static final int STATUS_ICON_Y = 53;
    private static final int GENERATION_ICON_X = 88;
    private static final int OUTPUT_ICON_X = 104;
    private static final int ICON_SIZE = 12;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.fluid_transfer",
            "rngtech.stat.efficiency",
            "rngtech.stat.processing_speed",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            CorrosionCellMenu.energyGenerationDataIndex(),
            CorrosionCellMenu.energyCapacityDataIndex(),
            CorrosionCellMenu.fluidTransferDataIndex(),
            CorrosionCellMenu.efficiencyDataIndex(),
            CorrosionCellMenu.processingSpeedDataIndex(),
            CorrosionCellMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.FLUID_TRANSFER,
            MachineStat.EFFICIENCY,
            MachineStat.PROCESSING_SPEED,
            MachineStat.STABILITY
    };

    public CorrosionCellScreen(CorrosionCellMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != CorrosionCellMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_GEAR) {
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 62, 35);
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.fluid_pump.short"), 104, 35);
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.cathode.short"), 146, 35);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, STAT_ACCENT);
        } else {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int tab = 0; tab < 4; tab++) {
                if (isOverTab(mouseX, mouseY, tab)) {
                    menu.selectTab(tab);
                    return true;
                }
            }
            if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == CorrosionCellMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == CorrosionCellMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, PLATE_SLOT_X, PLATE_SLOT_Y);
            renderSlotFrame(guiGraphics, ELECTROLYTE_SLOT_X, ELECTROLYTE_SLOT_Y);
            renderSlotFrame(guiGraphics, RESIDUE_SLOT_X, RESIDUE_SLOT_Y);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 52, 47);
            renderSlotFrame(guiGraphics, 94, 47);
            renderSlotFrame(guiGraphics, 136, 47);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != CorrosionCellMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + ELECTROLYTE_METER_X,
                y + ELECTROLYTE_METER_Y,
                x + ELECTROLYTE_METER_X + ELECTROLYTE_METER_WIDTH,
                y + ELECTROLYTE_METER_Y + ELECTROLYTE_METER_HEIGHT,
                0xFF5F5F5F
        );
        int electrolyteHeight = Math.round(48 * menu.electrolyteFluidProgress());
        int electrolyteBottom = y + ELECTROLYTE_METER_Y + ELECTROLYTE_METER_HEIGHT - 2;
        guiGraphics.fill(
                x + ELECTROLYTE_METER_X + 2,
                electrolyteBottom - electrolyteHeight,
                x + ELECTROLYTE_METER_X + ELECTROLYTE_METER_WIDTH - 2,
                electrolyteBottom,
                0xFF5C86C7
        );
        guiGraphics.fill(
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH,
                y + ENERGY_METER_Y + ENERGY_METER_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        int energyBottom = y + ENERGY_METER_Y + ENERGY_METER_HEIGHT - 2;
        guiGraphics.fill(
                x + ENERGY_METER_X + 2,
                energyBottom - energyHeight,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH - 2,
                energyBottom,
                ENERGY
        );

        guiGraphics.fill(
                x + PROGRESS_BAR_X,
                y + PROGRESS_BAR_Y,
                x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH,
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT,
                0xFF5F5F5F
        );
        guiGraphics.fill(
                x + PROGRESS_BAR_X + 1,
                y + PROGRESS_BAR_Y + 1,
                x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1,
                PROGRESS
        );
        renderProcessingIcons(guiGraphics);
        renderPurgeButtons(guiGraphics);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines().length, STAT_ACCENT);
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.corrosion_cell.plate"), 18, 22, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.corrosion_cell.electrolyte.short"), 18, 60, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.corrosion_cell.residue"), 175, 56, TEXT_MUTED, false);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case CorrosionCellBlockEntity.STATUS_NO_PLATE -> Component.translatable("rngtech.corrosion_cell.status.no_plate");
            case CorrosionCellBlockEntity.STATUS_NO_ELECTROLYTE -> Component.translatable("rngtech.corrosion_cell.status.no_electrolyte");
            case CorrosionCellBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.corrosion_cell.status.invalid_recipe");
            case CorrosionCellBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.corrosion_cell.status.blocked_stage", menu.minimumStage());
            case CorrosionCellBlockEntity.STATUS_BLOCKED_CATHODE -> Component.translatable("rngtech.corrosion_cell.status.blocked_cathode", menu.minimumStage());
            case CorrosionCellBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.corrosion_cell.status.output_full");
            case CorrosionCellBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.corrosion_cell.status.energy_full");
            case CorrosionCellBlockEntity.STATUS_REDSTONE_DISABLED -> Component.translatable("rngtech.corrosion_cell.status.redstone");
            default -> Component.translatable("rngtech.corrosion_cell.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case CorrosionCellBlockEntity.STATUS_READY -> ENERGY;
            case CorrosionCellBlockEntity.STATUS_NO_PLATE, CorrosionCellBlockEntity.STATUS_NO_ELECTROLYTE -> PANEL_DARK;
            case CorrosionCellBlockEntity.STATUS_ENERGY_FULL, CorrosionCellBlockEntity.STATUS_REDSTONE_DISABLED -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != CorrosionCellMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ELECTROLYTE_METER_X,
                ELECTROLYTE_METER_Y,
                ELECTROLYTE_METER_WIDTH,
                ELECTROLYTE_METER_HEIGHT,
                electrolyteTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_METER_X,
                ENERGY_METER_Y,
                ENERGY_METER_WIDTH,
                ENERGY_METER_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PROGRESS_BAR_X,
                PROGRESS_BAR_Y,
                PROGRESS_BAR_WIDTH,
                PROGRESS_BAR_HEIGHT,
                progressTooltip()
        );
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STATUS_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.status", statusComponent()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, GENERATION_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.output", CompactValueText.energyRate(menu.maxOutput())));
    }

    private Component electrolyteTooltip() {
        return FluidMeterTooltips.amount(
                menu.electrolyteFluidName(),
                menu.electrolyteFluid(),
                menu.electrolyteFluidCapacity(),
                "transfer " + menu.fluidTransfer() + " mB/t"
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable("rngtech.corrosion_cell.tooltip.progress.empty", CompactValueText.energyAmount(menu.recipeEnergy()));
        }
        return Component.translatable("rngtech.corrosion_cell.tooltip.progress", menu.progress(), ticks, CompactValueText.energyAmount(menu.recipeEnergy()));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines());
        }
    }

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor());

        renderIconBox(guiGraphics, GENERATION_ICON_X, STATUS_ICON_Y);
        int sparkX = leftPos + GENERATION_ICON_X;
        int sparkY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, 0xFFD3A33A);

        renderIconBox(guiGraphics, OUTPUT_ICON_X, STATUS_ICON_Y);
        int arrowX = leftPos + OUTPUT_ICON_X;
        int arrowY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(arrowX + 3, arrowY + 5, arrowX + 8, arrowY + 7, ENERGY);
        guiGraphics.fill(arrowX + 8, arrowY + 4, arrowX + 10, arrowY + 8, ENERGY);
        guiGraphics.fill(arrowX + 10, arrowY + 5, arrowX + 11, arrowY + 7, ENERGY);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                ELECTROLYTE_METER_X + 3,
                ELECTROLYTE_METER_Y + 2,
                menu.electrolyteFluid() > 0
        );
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ELECTROLYTE_METER_X + 3,
                ELECTROLYTE_METER_Y + 2,
                CorrosionCellBlockEntity.PURGE_ELECTROLYTE_TANK
        );
    }

    private boolean renderPurgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        return FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ELECTROLYTE_METER_X + 3,
                ELECTROLYTE_METER_Y + 2,
                Component.translatable("rngtech.purge.target.electrolyte_tank"),
                menu.electrolyteFluid() > 0 ? menu.electrolyteFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.electrolyteFluid(),
                menu.electrolyteFluidCapacity()
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
                    isIntegralStat(STAT_DATA_INDICES[index]),
                    !isIntegralStat(STAT_DATA_INDICES[index]) && value > 1.001,
                    MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), STAT_TYPES[index], value)
            );
        }
        return MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(CorrosionCellMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(CorrosionCellMenu.flatEnergyGenerationDataIndex())
        );
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == CorrosionCellMenu.energyCapacityDataIndex()
                || dataIndex == CorrosionCellMenu.fluidTransferDataIndex();
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

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
