package com.rngtech.client.screen;

import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SolidFuelBurnerScreen extends AbstractContainerScreen<SolidFuelBurnerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FUEL = 0xFFD3942F;
    private static final int STAT_ACCENT = 0xFFE3B75A;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_GAUGE_X = 184;
    private static final int ENERGY_GAUGE_Y = 30;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int FUEL_BAR_X = 110;
    private static final int FUEL_BAR_Y = 48;
    private static final int FUEL_BAR_WIDTH = 58;
    private static final int FUEL_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 117;
    private static final int GENERATION_ICON_X = 133;
    private static final int OUTPUT_ICON_X = 149;
    private static final int STATUS_ICON_Y = 60;
    private static final int TIER_ICON_X = 79;
    private static final int FUEL_FORM_ICON_X = 95;
    private static final int GEAR_INFO_ICON_Y = 78;
    private static final int ICON_SIZE = 12;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.efficiency",
            "rngtech.stat.fuel_efficiency",
            "rngtech.stat.fuel_duration",
            "rngtech.stat.heat_isolation",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            SolidFuelBurnerMenu.energyGenerationDataIndex(),
            SolidFuelBurnerMenu.efficiencyDataIndex(),
            SolidFuelBurnerMenu.fuelEfficiencyDataIndex(),
            SolidFuelBurnerMenu.fuelDurationDataIndex(),
            SolidFuelBurnerMenu.heatIsolationDataIndex(),
            SolidFuelBurnerMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.EFFICIENCY,
            MachineStat.FUEL_EFFICIENCY,
            MachineStat.FUEL_DURATION,
            MachineStat.HEAT_ISOLATION,
            MachineStat.STABILITY
    };

    public SolidFuelBurnerScreen(SolidFuelBurnerMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != SolidFuelBurnerMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(SolidFuelBurnerMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(SolidFuelBurnerMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(SolidFuelBurnerMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(SolidFuelBurnerMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_REFINEMENT
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == SolidFuelBurnerMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == SolidFuelBurnerMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == SolidFuelBurnerMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_PROCESSING) {
            for (int slot = 0; slot < menu.activeFuelSlots(); slot++) {
                renderSlotFrame(guiGraphics, 50 + slot * 22, 54);
            }
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 43, 47);
            renderSlotFrame(guiGraphics, 79, 47);
            renderSlotFrame(guiGraphics, 115, 47);
        } else if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_STATS) {
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
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );

        guiGraphics.fill(x + FUEL_BAR_X, y + FUEL_BAR_Y, x + FUEL_BAR_X + FUEL_BAR_WIDTH, y + FUEL_BAR_Y + FUEL_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + FUEL_BAR_X + 1,
                y + FUEL_BAR_Y + 1,
                x + FUEL_BAR_X + 1 + Math.round((FUEL_BAR_WIDTH - 2) * menu.fuelProgress()),
                y + FUEL_BAR_Y + FUEL_BAR_HEIGHT - 1,
                FUEL
        );
        renderProcessingIcons(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, TIER_ICON_X, GEAR_INFO_ICON_Y);
        int tierX = leftPos + TIER_ICON_X;
        int tierY = topPos + GEAR_INFO_ICON_Y;
        guiGraphics.fill(tierX + 3, tierY + 8, tierX + 9, tierY + 10, FUEL);
        guiGraphics.fill(tierX + 4, tierY + 5, tierX + 8, tierY + 7, FUEL);
        guiGraphics.fill(tierX + 5, tierY + 2, tierX + 7, tierY + 4, FUEL);

        renderIconBox(guiGraphics, FUEL_FORM_ICON_X, GEAR_INFO_ICON_Y);
        int formX = leftPos + FUEL_FORM_ICON_X;
        int formY = topPos + GEAR_INFO_ICON_Y;
        guiGraphics.fill(formX + 3, formY + 3, formX + 9, formY + 5, FUEL);
        guiGraphics.fill(formX + 3, formY + 5, formX + 5, formY + 9, FUEL);
        guiGraphics.fill(formX + 7, formY + 5, formX + 9, formY + 9, FUEL);
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
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.fuel"), 51, 43, TEXT_MUTED, false);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 52);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 88);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.fuel_box.short"), 124);
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
            case SolidFuelBurnerBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.burner.status.missing_heat_core");
            case SolidFuelBurnerBlockEntity.STATUS_MISSING_BATTERY_CELL -> Component.translatable("rngtech.burner.status.missing_battery_cell");
            case SolidFuelBurnerBlockEntity.STATUS_MISSING_FUEL_BOX -> Component.translatable("rngtech.burner.status.missing_fuel_box");
            case SolidFuelBurnerBlockEntity.STATUS_NO_FUEL -> Component.translatable("rngtech.burner.status.no_fuel");
            case SolidFuelBurnerBlockEntity.STATUS_BLOCKED_TIER -> Component.translatable("rngtech.burner.status.blocked_tier");
            case SolidFuelBurnerBlockEntity.STATUS_BLOCKED_FORM -> Component.translatable("rngtech.burner.status.blocked_form");
            case SolidFuelBurnerBlockEntity.STATUS_FULL_GOVERNED -> Component.translatable("rngtech.burner.status.full_governed");
            case SolidFuelBurnerBlockEntity.STATUS_NOT_BURNABLE -> Component.translatable("rngtech.burner.status.not_burnable");
            default -> Component.translatable("rngtech.burner.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case SolidFuelBurnerBlockEntity.STATUS_NO_FUEL,
                    SolidFuelBurnerBlockEntity.STATUS_MISSING_HEAT_CORE,
                    SolidFuelBurnerBlockEntity.STATUS_MISSING_BATTERY_CELL,
                    SolidFuelBurnerBlockEntity.STATUS_MISSING_FUEL_BOX -> PANEL_DARK;
            case SolidFuelBurnerBlockEntity.STATUS_FULL_GOVERNED -> 0xFFD3A33A;
            case SolidFuelBurnerBlockEntity.STATUS_BLOCKED_TIER,
                    SolidFuelBurnerBlockEntity.STATUS_BLOCKED_FORM,
                    SolidFuelBurnerBlockEntity.STATUS_NOT_BURNABLE -> 0xFFB45B4A;
            default -> FUEL;
        };
    }

    private Component fuelFormsComponent() {
        return switch (menu.fuelFormsCode()) {
            case 1 -> Component.translatable("rngtech.fuel_form.items");
            case 2 -> Component.translatable("rngtech.fuel_form.blocks");
            case 3 -> Component.translatable("rngtech.fuel_form.items_blocks");
            default -> Component.literal("--");
        };
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    TIER_ICON_X,
                    GEAR_INFO_ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE,
                    Component.translatable("rngtech.burner.tooltip.max_fuel_tier", menu.maxFuelTier())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    FUEL_FORM_ICON_X,
                    GEAR_INFO_ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE,
                    Component.translatable("rngtech.burner.tooltip.fuel_forms", fuelFormsComponent())
            );
            return;
        }
        if (menu.selectedTab() != SolidFuelBurnerMenu.TAB_PROCESSING) {
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
                Component.translatable("rngtech.burner.tooltip.energy", exactEnergyText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FUEL_BAR_X,
                FUEL_BAR_Y,
                FUEL_BAR_WIDTH,
                FUEL_BAR_HEIGHT,
                fuelTooltip()
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
                Component.translatable("rngtech.burner.tooltip.status", statusComponent())
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
                Component.translatable("rngtech.burner.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick()))
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
                outputTooltip()
        );
    }

    private Component fuelTooltip() {
        int total = menu.totalBurnTime();
        if (total <= 0) {
            return Component.translatable("rngtech.burner.tooltip.fuel.empty");
        }
        return Component.translatable("rngtech.burner.tooltip.fuel", menu.burnTime(), total);
    }

    private Component outputTooltip() {
        int lastOutput = menu.lastOutput();
        int connectorCap = menu.connectorOutputCap();
        if (connectorCap > 0 && menu.energyPerTick() > connectorCap) {
            return Component.translatable(
                    "rngtech.burner.tooltip.output_connector_limited",
                    CompactValueText.energyRate(lastOutput),
                    CompactValueText.energyRate(connectorCap)
            );
        }
        if (connectorCap > 0) {
            return Component.translatable(
                    "rngtech.burner.tooltip.output_connector",
                    CompactValueText.energyRate(lastOutput),
                    CompactValueText.energyRate(connectorCap)
            );
        }
        return Component.translatable("rngtech.burner.tooltip.output", CompactValueText.energyRate(lastOutput));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != SolidFuelBurnerMenu.TAB_STATS) {
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
                    statTooltip(index, value)
            ).withStat(STAT_TYPES[index]);
        }
        return statLines;
    }

    private Component statTooltip(int index, double value) {
        MachineStat stat = STAT_TYPES[index];
        return MachineScreenStyle.statLayerTooltip(
                menu.getSlot(menu.refinementTargetSlot()).getItem(),
                stat,
                value
        );
    }

    private boolean isIntegralStat(MachineStat stat) {
        return stat == MachineStat.INPUT_SLOTS
                || stat == MachineStat.ENERGY_GENERATION;
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        return value > 1.001;
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
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, FUEL);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, FUEL);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, FUEL);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, FUEL);

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

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 34, TEXT_MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
