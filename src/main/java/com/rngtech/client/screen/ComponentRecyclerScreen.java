package com.rngtech.client.screen;

import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ComponentRecyclerScreen extends AbstractContainerScreen<ComponentRecyclerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int PROGRESS = 0xFF4F7A85;
    private static final int STAT_ACCENT = 0xFF4F7A85;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int INPUT_SLOT_X = 44;
    private static final int INPUT_SLOT_Y = 51;
    private static final int OUTPUT_SLOT_X = 176;
    private static final int OUTPUT_PRIMARY_Y = 29;
    private static final int OUTPUT_SECONDARY_Y = 51;
    private static final int OUTPUT_TERTIARY_Y = 73;
    private static final int ENERGY_BAR_X = 28;
    private static final int ENERGY_BAR_Y = 30;
    private static final int ENERGY_BAR_WIDTH = 12;
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int PROGRESS_BAR_X = 84;
    private static final int PROGRESS_BAR_Y = 56;
    private static final int PROGRESS_BAR_WIDTH = 76;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 108;
    private static final int STATUS_ICON_Y = 67;
    private static final int ENERGY_ICON_X = 124;
    private static final int PROCESSING_LEVEL_ICON_X = 184;
    private static final int PROCESSING_LEVEL_ICON_Y = 78;
    private static final int PROCESSING_LEVEL_MAX = 8;
    private static final int ICON_SIZE = 12;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.processing_level",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            ComponentRecyclerMenu.processingSpeedDataIndex(),
            ComponentRecyclerMenu.energyUsageDataIndex(),
            ComponentRecyclerMenu.energyCapacityStatDataIndex(),
            ComponentRecyclerMenu.processingLevelDataIndex(),
            ComponentRecyclerMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.PROCESSING_LEVEL,
            MachineStat.STABILITY
    };

    public ComponentRecyclerScreen(ComponentRecyclerMenu menu, Inventory playerInventory, Component title) {
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
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == ComponentRecyclerMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != ComponentRecyclerMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == ComponentRecyclerMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.isManual()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(ComponentRecyclerMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(ComponentRecyclerMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(ComponentRecyclerMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(ComponentRecyclerMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == ComponentRecyclerMenu.TAB_REFINEMENT
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
        if (menu.isManual()) {
            renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), true);
            return;
        }
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == ComponentRecyclerMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ComponentRecyclerMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ComponentRecyclerMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == ComponentRecyclerMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == ComponentRecyclerMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, INPUT_SLOT_X, INPUT_SLOT_Y);
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X, OUTPUT_PRIMARY_Y);
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X, OUTPUT_SECONDARY_Y);
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X, OUTPUT_TERTIARY_Y);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 55, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 127, 47);
        } else if (menu.selectedTab() == ComponentRecyclerMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == ComponentRecyclerMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
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

        if (menu.isManual()) {
            renderProcessingIcons(guiGraphics);
            return;
        }
        guiGraphics.fill(
                x + ENERGY_BAR_X,
                y + ENERGY_BAR_Y,
                x + ENERGY_BAR_X + ENERGY_BAR_WIDTH,
                y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        int energyBottom = y + ENERGY_BAR_Y + ENERGY_BAR_HEIGHT - 2;
        guiGraphics.fill(x + ENERGY_BAR_X + 2, energyBottom - energyHeight, x + ENERGY_BAR_X + 10, energyBottom, ENERGY);
        renderProcessingIcons(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        if (menu.isManual()) {
            return;
        }
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

    private void renderStats(GuiGraphics guiGraphics) {
        if (menu.isManual()) {
            return;
        }
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
        if (menu.isManual()) {
            return;
        }
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.input.tiny"), INPUT_SLOT_X + 9, 39);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output.tiny"), OUTPUT_SLOT_X + 9, 18);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        if (menu.isManual()) {
            return;
        }
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 64);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.disassembly_head.short"), 100);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.recovery_filter.short"), 136);
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        if (menu.isManual()) {
            return;
        }
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
        if (menu.isManual()) {
            return;
        }
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
    }

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(
                leftPos + STATUS_ICON_X + 3,
                topPos + STATUS_ICON_Y + 3,
                leftPos + STATUS_ICON_X + 9,
                topPos + STATUS_ICON_Y + 9,
                statusColor()
        );

        if (menu.isManual()) {
            renderManualCrankIcon(guiGraphics);
            return;
        }
        renderIconBox(guiGraphics, ENERGY_ICON_X, STATUS_ICON_Y);
        int boltX = leftPos + ENERGY_ICON_X;
        int boltY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(boltX + 6, boltY + 2, boltX + 9, boltY + 3, ENERGY);
        guiGraphics.fill(boltX + 5, boltY + 3, boltX + 8, boltY + 6, ENERGY);
        guiGraphics.fill(boltX + 4, boltY + 6, boltX + 7, boltY + 7, ENERGY);
        guiGraphics.fill(boltX + 3, boltY + 7, boltX + 6, boltY + 10, ENERGY);
    }

    private void renderManualCrankIcon(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, ENERGY_ICON_X, STATUS_ICON_Y);
        int crankX = leftPos + ENERGY_ICON_X;
        int crankY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(crankX + 5, crankY + 2, crankX + 7, crankY + 10, 0xFFB8925A);
        guiGraphics.fill(crankX + 3, crankY + 3, crankX + 9, crankY + 5, 0xFFB8925A);
        guiGraphics.fill(crankX + 2, crankY + 2, crankX + 4, crankY + 6, 0xFFD0B078);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == ComponentRecyclerMenu.TAB_GEAR) {
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
        if (menu.selectedTab() != ComponentRecyclerMenu.TAB_PROCESSING) {
            return;
        }
        if (!menu.isManual()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    ENERGY_BAR_X,
                    ENERGY_BAR_Y,
                    ENERGY_BAR_WIDTH,
                    ENERGY_BAR_HEIGHT,
                    CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
            );
        }
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
                Component.translatable("rngtech.recycler.tooltip.status", statusText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                menu.isManual()
                        ? Component.translatable("rngtech.recycler.tooltip.crank")
                        : Component.translatable("rngtech.recycler.tooltip.energy", CompactValueText.energyRate(menu.energyPerTick()))
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        return ticks <= 0
                ? Component.translatable("rngtech.recycler.tooltip.progress.empty")
                : Component.translatable("rngtech.recycler.tooltip.progress", menu.progress(), ticks);
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.isManual() || menu.selectedTab() != ComponentRecyclerMenu.TAB_STATS) {
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
        return switch (menu.statusCode()) {
            case ComponentRecyclerBlockEntity.STATUS_MISSING_HEAD -> Component.translatable("rngtech.recycler.status.missing_head");
            case ComponentRecyclerBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.recycler.status.no_input");
            case ComponentRecyclerBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.recycler.status.invalid_recipe");
            case ComponentRecyclerBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.recycler.status.blocked_stage");
            case ComponentRecyclerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.recycler.status.output_full");
            case ComponentRecyclerBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.recycler.status.no_power");
            case ComponentRecyclerBlockEntity.STATUS_MISSING_CRANK -> Component.translatable("rngtech.recycler.status.missing_crank");
            default -> Component.translatable("rngtech.recycler.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case ComponentRecyclerBlockEntity.STATUS_READY -> STAT_ACCENT;
            case ComponentRecyclerBlockEntity.STATUS_NO_INPUT, ComponentRecyclerBlockEntity.STATUS_MISSING_HEAD -> PANEL_DARK;
            case ComponentRecyclerBlockEntity.STATUS_MISSING_CRANK -> PANEL_DARK;
            case ComponentRecyclerBlockEntity.STATUS_NO_POWER -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
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
            ).withStat(STAT_TYPES[index]);
        }
        return statLines;
    }

    private String statValue(int index) {
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == ComponentRecyclerMenu.energyCapacityStatDataIndex()
                || dataIndex == ComponentRecyclerMenu.processingLevelDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == ComponentRecyclerMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == ComponentRecyclerMenu.energyCapacityStatDataIndex()) {
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

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 34, TEXT_MUTED);
    }

    private Component clipped(Component component, int width) {
        String text = component.getString();
        if (font.width(text) <= width) {
            return component;
        }
        String suffix = "...";
        int availableWidth = width - font.width(suffix);
        if (availableWidth <= 0) {
            return Component.empty();
        }
        while (!text.isEmpty() && font.width(text) > availableWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + suffix);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
