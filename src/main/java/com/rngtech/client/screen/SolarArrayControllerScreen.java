package com.rngtech.client.screen;

import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.menu.SolarArrayControllerMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SolarArrayControllerScreen extends AbstractContainerScreen<SolarArrayControllerMenu> {
    private static final int PANEL = 0xFFC7D0C0;
    private static final int PANEL_DARK = 0xFF7B8672;
    private static final int PANEL_LIGHT = 0xFFEAF1DF;
    private static final int TEXT = 0xFF364033;
    private static final int TEXT_MUTED = 0xFF5F6B5A;
    private static final int ENERGY = 0xFF3D8E6B;
    private static final int SOLAR = 0xFFD6A83A;
    private static final int STAT_ACCENT = 0xFFD0A039;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_GAUGE_X = 42;
    private static final int ENERGY_GAUGE_Y = 30;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int PANEL_BAR_X = 126;
    private static final int ACTIVE_PANEL_BAR_Y = 48;
    private static final int BLOCKED_PANEL_BAR_Y = 64;
    private static final int PANEL_BAR_WIDTH = 80;
    private static final int PANEL_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 126;
    private static final int GENERATION_ICON_X = 142;
    private static final int OUTPUT_ICON_X = 158;
    private static final int PREVIEW_ICON_X = 174;
    private static final int STATUS_ICON_Y = 30;
    private static final int ICON_SIZE = 12;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.solar_panel_limit",
            "rngtech.stat.moonlight_conversion",
            "rngtech.stat.weather_recovery",
            "rngtech.stat.solar_panel_synchronization",
            "rngtech.stat.clear_sky_amplification",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            SolarArrayControllerMenu.energyGenerationDataIndex(),
            SolarArrayControllerMenu.panelLimitDataIndex(),
            SolarArrayControllerMenu.moonlightConversionDataIndex(),
            SolarArrayControllerMenu.weatherRecoveryDataIndex(),
            SolarArrayControllerMenu.panelSynchronizationDataIndex(),
            SolarArrayControllerMenu.clearSkyAmplificationDataIndex(),
            SolarArrayControllerMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.SOLAR_PANEL_LIMIT,
            MachineStat.MOONLIGHT_CONVERSION,
            MachineStat.WEATHER_RECOVERY,
            MachineStat.SOLAR_PANEL_SYNCHRONIZATION,
            MachineStat.CLEAR_SKY_AMPLIFICATION,
            MachineStat.STABILITY
    };

    public SolarArrayControllerScreen(SolarArrayControllerMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == SolarArrayControllerMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == SolarArrayControllerMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == SolarArrayControllerMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != SolarArrayControllerMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == SolarArrayControllerMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == SolarArrayControllerMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else if (menu.selectedTab() == SolarArrayControllerMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(SolarArrayControllerMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(SolarArrayControllerMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(SolarArrayControllerMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(SolarArrayControllerMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == SolarArrayControllerMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == SolarArrayControllerMenu.TAB_PROCESSING
                    && isOverPreviewButton(mouseX, mouseY)) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null && minecraft.gameMode != null
                        && menu.clickMenuButton(minecraft.player, SolarArrayControllerMenu.BUTTON_TOGGLE_PREVIEW_RANGE)) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            SolarArrayControllerMenu.BUTTON_TOGGLE_PREVIEW_RANGE
                    );
                }
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == SolarArrayControllerMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == SolarArrayControllerMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == SolarArrayControllerMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == SolarArrayControllerMenu.TAB_REFINEMENT);
    }

    private void renderTab(GuiGraphics guiGraphics, int index, Component label, boolean selected) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        int color = selected ? PANEL : 0xFF9DA894;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, color);
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 1, PANEL_LIGHT);
        guiGraphics.fill(x, y, x + 1, y + 21, PANEL_LIGHT);
        guiGraphics.fill(x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + 21, PANEL_DARK);
        if (!selected) {
            guiGraphics.fill(x, y + 20, x + TAB_WIDTH, y + 21, PANEL_DARK);
        }
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, selected ? TEXT : 0xFF2F382D, false);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == SolarArrayControllerMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 56, 47);
            renderSlotFrame(guiGraphics, 92, 47);
        } else if (menu.selectedTab() == SolarArrayControllerMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == SolarArrayControllerMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + ENERGY_GAUGE_X,
                y + ENERGY_GAUGE_Y,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT,
                0xFF566052
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );

        renderPanelBar(guiGraphics, ACTIVE_PANEL_BAR_Y, menu.activePanels(), SOLAR);
        renderPanelBar(guiGraphics, BLOCKED_PANEL_BAR_Y, menu.blockedPanels(), 0xFF9B6E5A);
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

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.gear.solar_array_extender.short"), 52, 35, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.gear.battery_cell.short"), 90, 35, TEXT_MUTED, false);
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
            case SolarArrayControllerBlockEntity.STATUS_WEATHER -> Component.translatable("rngtech.solar.status.weather");
            case SolarArrayControllerBlockEntity.STATUS_NIGHT -> Component.translatable("rngtech.solar.status.night");
            case SolarArrayControllerBlockEntity.STATUS_BAD_DIMENSION -> Component.translatable("rngtech.solar.status.dimension");
            case SolarArrayControllerBlockEntity.STATUS_NO_PANELS -> Component.translatable("rngtech.solar.status.no_panels");
            case SolarArrayControllerBlockEntity.STATUS_FULL -> Component.translatable("rngtech.solar.status.full");
            case SolarArrayControllerBlockEntity.STATUS_BLOCKED -> Component.translatable("rngtech.solar.status.blocked");
            default -> Component.translatable("rngtech.solar.status.clear");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case SolarArrayControllerBlockEntity.STATUS_CLEAR -> SOLAR;
            case SolarArrayControllerBlockEntity.STATUS_WEATHER, SolarArrayControllerBlockEntity.STATUS_FULL -> 0xFFD3A33A;
            case SolarArrayControllerBlockEntity.STATUS_NIGHT, SolarArrayControllerBlockEntity.STATUS_NO_PANELS -> PANEL_DARK;
            default -> 0xFFB45B4A;
        };
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != SolarArrayControllerMenu.TAB_PROCESSING) {
            return;
        }
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
                Component.translatable("rngtech.solar.tooltip.energy", exactEnergyText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PANEL_BAR_X,
                ACTIVE_PANEL_BAR_Y,
                PANEL_BAR_WIDTH,
                PANEL_BAR_HEIGHT,
                Component.translatable("rngtech.solar.tooltip.active_panels", menu.activePanels(), menu.panelLimit())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PANEL_BAR_X,
                BLOCKED_PANEL_BAR_Y,
                PANEL_BAR_WIDTH,
                PANEL_BAR_HEIGHT,
                Component.translatable("rngtech.solar.tooltip.blocked_panels", menu.blockedPanels(), menu.panelLimit())
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
                Component.translatable("rngtech.solar.tooltip.status", statusComponent())
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
                Component.translatable("rngtech.solar.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick()))
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
                Component.translatable(
                        "rngtech.solar.tooltip.output",
                        CompactValueText.energyRate(menu.maxOutput())
                )
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PREVIEW_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable(menu.previewRange()
                        ? "rngtech.solar.tooltip.preview_range.on"
                        : "rngtech.solar.tooltip.preview_range.off")
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != SolarArrayControllerMenu.TAB_STATS) {
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
                    isIntegralStat(STAT_TYPES[index]),
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            ).withStat(STAT_TYPES[index]);
        }
        MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(SolarArrayControllerMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(SolarArrayControllerMenu.flatEnergyGenerationDataIndex())
        );
        return MachineScreenStyle.withoutInactiveModifierStats(STAT_TYPES, statLines);
    }

    private boolean isIntegralStat(MachineStat stat) {
        return stat == MachineStat.SOLAR_PANEL_LIMIT
                || stat == MachineStat.SOLAR_PANEL_ARBITRATION;
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == SolarArrayControllerMenu.panelLimitDataIndex()) {
            return value > 1.001;
        }
        return value > 1.001;
    }

    private void renderPanelBar(GuiGraphics guiGraphics, int y, int count, int fillColor) {
        int left = leftPos + PANEL_BAR_X;
        int top = topPos + y;
        int innerWidth = PANEL_BAR_WIDTH - 2;
        int panelLimit = Math.max(1, menu.panelLimit());
        int fill = Math.min(innerWidth, count * innerWidth / panelLimit);
        guiGraphics.fill(left, top, left + PANEL_BAR_WIDTH, top + PANEL_BAR_HEIGHT, 0xFF566052);
        guiGraphics.fill(left + 1, top + 1, left + 1 + fill, top + PANEL_BAR_HEIGHT - 1, fillColor);
        for (int pip = 1; pip < panelLimit; pip++) {
            int pipX = left + pip * PANEL_BAR_WIDTH / panelLimit;
            guiGraphics.fill(pipX, top + 1, pipX + 1, top + PANEL_BAR_HEIGHT - 1, PANEL_DARK);
        }
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

        renderIconBox(guiGraphics, GENERATION_ICON_X, STATUS_ICON_Y);
        int sparkX = leftPos + GENERATION_ICON_X;
        int sparkY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, SOLAR);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, SOLAR);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, SOLAR);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, SOLAR);

        renderIconBox(guiGraphics, OUTPUT_ICON_X, STATUS_ICON_Y);
        int arrowX = leftPos + OUTPUT_ICON_X;
        int arrowY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(arrowX + 3, arrowY + 5, arrowX + 8, arrowY + 7, ENERGY);
        guiGraphics.fill(arrowX + 8, arrowY + 4, arrowX + 10, arrowY + 8, ENERGY);
        guiGraphics.fill(arrowX + 10, arrowY + 5, arrowX + 11, arrowY + 7, ENERGY);

        renderIconBox(guiGraphics, PREVIEW_ICON_X, STATUS_ICON_Y);
        int previewX = leftPos + PREVIEW_ICON_X;
        int previewY = topPos + STATUS_ICON_Y;
        int previewColor = menu.previewRange() ? SOLAR : PANEL_DARK;
        guiGraphics.fill(previewX + 3, previewY + 3, previewX + 9, previewY + 4, previewColor);
        guiGraphics.fill(previewX + 3, previewY + 8, previewX + 9, previewY + 9, previewColor);
        guiGraphics.fill(previewX + 3, previewY + 3, previewX + 4, previewY + 9, previewColor);
        guiGraphics.fill(previewX + 8, previewY + 3, previewX + 9, previewY + 9, previewColor);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF566052);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F382D);
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

    private boolean isOverPreviewButton(double mouseX, double mouseY) {
        int x = leftPos + PREVIEW_ICON_X;
        int y = topPos + STATUS_ICON_Y;
        return mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
    }
}
