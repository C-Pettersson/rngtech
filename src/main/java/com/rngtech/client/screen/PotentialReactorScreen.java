package com.rngtech.client.screen;

import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.menu.PotentialReactorMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PotentialReactorScreen extends AbstractContainerScreen<PotentialReactorMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF5BA850;
    private static final int PROGRESS = 0xFF8B6F4A;
    private static final int STAT_ACCENT = 0xFF75B56A;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int INPUT_SLOT_X = 42;
    private static final int INPUT_SLOT_Y = 54;
    private static final int RESIDUE_SLOT_X = 194;
    private static final int RESIDUE_SLOT_Y = 54;
    private static final int ENERGY_METER_X = 174;
    private static final int ENERGY_METER_Y = 30;
    private static final int ENERGY_METER_WIDTH = 12;
    private static final int ENERGY_METER_HEIGHT = 52;
    private static final int PROGRESS_BAR_X = 90;
    private static final int PROGRESS_BAR_Y = 48;
    private static final int PROGRESS_BAR_WIDTH = 66;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int ICON_SIZE = 12;
    private static final int STATUS_ICON_SPACING = 16;
    private static final int STATUS_ICON_ROW_WIDTH = ICON_SIZE + STATUS_ICON_SPACING * 2;
    private static final int STATUS_ICON_X = PROGRESS_BAR_X + (PROGRESS_BAR_WIDTH - STATUS_ICON_ROW_WIDTH) / 2;
    private static final int STATUS_ICON_Y = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT + 5;
    private static final int GENERATION_ICON_X = STATUS_ICON_X + STATUS_ICON_SPACING;
    private static final int OUTPUT_ICON_X = GENERATION_ICON_X + STATUS_ICON_SPACING;
    private static final int PROCESSING_LEVEL_ICON_X = 184;
    private static final int PROCESSING_LEVEL_ICON_Y = 78;
    private static final int PROCESSING_LEVEL_MAX = 4;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.efficiency",
            "rngtech.stat.processing_speed",
            "rngtech.stat.processing_level",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            PotentialReactorMenu.energyGenerationDataIndex(),
            PotentialReactorMenu.energyCapacityDataIndex(),
            PotentialReactorMenu.efficiencyDataIndex(),
            PotentialReactorMenu.processingSpeedDataIndex(),
            PotentialReactorMenu.processingLevelDataIndex(),
            PotentialReactorMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.EFFICIENCY,
            MachineStat.PROCESSING_SPEED,
            MachineStat.PROCESSING_LEVEL,
            MachineStat.STABILITY
    };

    public PotentialReactorScreen(PotentialReactorMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == PotentialReactorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != PotentialReactorMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == PotentialReactorMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(PotentialReactorMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(PotentialReactorMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(PotentialReactorMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(PotentialReactorMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == PotentialReactorMenu.TAB_REFINEMENT
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == PotentialReactorMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == PotentialReactorMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == PotentialReactorMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == PotentialReactorMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == PotentialReactorMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, INPUT_SLOT_X, INPUT_SLOT_Y);
            renderSlotFrame(guiGraphics, RESIDUE_SLOT_X, RESIDUE_SLOT_Y);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 43, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 139, 47);
        } else if (menu.selectedTab() == PotentialReactorMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == PotentialReactorMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH,
                y + ENERGY_METER_Y + ENERGY_METER_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round((ENERGY_METER_HEIGHT - 4) * menu.energyProgress());
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

    private void renderGear(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderProcessingLevelSquare(
                guiGraphics,
                leftPos,
                topPos,
                PROCESSING_LEVEL_ICON_X,
                PROCESSING_LEVEL_ICON_Y,
                menu.processingLevel(),
                PROCESSING_LEVEL_MAX,
                STAT_ACCENT
        );
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.input"), INPUT_SLOT_X, 43, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.reactor.residue"), RESIDUE_SLOT_X - 6, 43, TEXT_MUTED, false);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.reactor_chamber.short"), 52);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.recovery_filter.short"), 100);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.containment_lining.short"), 148);
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

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case PotentialReactorBlockEntity.STATUS_MISSING_CHAMBER -> Component.translatable("rngtech.reactor.status.missing_chamber");
            case PotentialReactorBlockEntity.STATUS_NO_FUEL -> Component.translatable("rngtech.reactor.status.no_fuel");
            case PotentialReactorBlockEntity.STATUS_INVALID_FUEL -> Component.translatable("rngtech.reactor.status.invalid_fuel");
            case PotentialReactorBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.reactor.status.blocked_stage");
            case PotentialReactorBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.reactor.status.output_full");
            case PotentialReactorBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.reactor.status.energy_full");
            case PotentialReactorBlockEntity.STATUS_REDSTONE_DISABLED -> Component.translatable("rngtech.reactor.status.redstone");
            default -> Component.translatable("rngtech.reactor.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case PotentialReactorBlockEntity.STATUS_READY -> ENERGY;
            case PotentialReactorBlockEntity.STATUS_NO_FUEL, PotentialReactorBlockEntity.STATUS_MISSING_CHAMBER -> PANEL_DARK;
            case PotentialReactorBlockEntity.STATUS_ENERGY_FULL, PotentialReactorBlockEntity.STATUS_REDSTONE_DISABLED -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == PotentialReactorMenu.TAB_GEAR) {
            MachineScreenStyle.renderStatusSquareTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    PROCESSING_LEVEL_ICON_X,
                    PROCESSING_LEVEL_ICON_Y,
                    Component.translatable("rngtech.gear.tooltip.processing_level", menu.processingLevel())
            );
            return;
        }
        if (menu.selectedTab() != PotentialReactorMenu.TAB_PROCESSING) {
            return;
        }
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
                Component.literal("Energy: ").append(exactEnergyText())
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
                Component.translatable("rngtech.reactor.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                GENERATION_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable("rngtech.reactor.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick()))
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable("rngtech.reactor.tooltip.output", CompactValueText.energyRate(menu.maxOutput()))
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable(
                    "rngtech.reactor.tooltip.progress.empty",
                    CompactValueText.energyAmount(menu.recipeEnergy())
            );
        }
        return Component.translatable(
                "rngtech.reactor.tooltip.progress",
                menu.progress(),
                ticks,
                CompactValueText.energyAmount(menu.recipeEnergy())
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != PotentialReactorMenu.TAB_STATS) {
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

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        int statusColor = statusColor();
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor);

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
                    STAT_TYPES[index] == MachineStat.PROCESSING_LEVEL,
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            );
        }
        return MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(PotentialReactorMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(PotentialReactorMenu.flatEnergyGenerationDataIndex())
        );
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == PotentialReactorMenu.energyCapacityDataIndex()
                || dataIndex == PotentialReactorMenu.processingLevelDataIndex()) {
            return value > 1.001;
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

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 46, TEXT_MUTED);
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
