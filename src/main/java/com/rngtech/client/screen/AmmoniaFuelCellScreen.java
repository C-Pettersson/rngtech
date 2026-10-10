package com.rngtech.client.screen;

import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.menu.AmmoniaFuelCellMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AmmoniaFuelCellScreen extends AbstractContainerScreen<AmmoniaFuelCellMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF5F5F5F;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF74A857;
    private static final int AMMONIA = 0xFFBFD48B;
    private static final int PROGRESS = 0xFF71A67B;
    private static final int STAT_ACCENT = 0xFF8EA553;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int AMMONIA_BAR_X = 40;
    private static final int ENERGY_BAR_X = 174;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int PROGRESS_X = 90;
    private static final int PROGRESS_Y = 48;
    private static final int PROGRESS_WIDTH = 66;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int RESIDUE_SLOT_X = 194;
    private static final int RESIDUE_SLOT_Y = 60;
    private static final int STATUS_X = 117;
    private static final int STATUS_Y = 60;
    private static final int STATUS_SIZE = 12;

    public AmmoniaFuelCellScreen(AmmoniaFuelCellMenu menu, Inventory playerInventory, Component title) {
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        renderTabs(guiGraphics);
        if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_PROCESSING) {
            bar(guiGraphics, AMMONIA_BAR_X, BAR_Y, menu.ammoniaFill(), AMMONIA);
            FluidPurgeButton.render(guiGraphics, leftPos, topPos, AMMONIA_BAR_X, BAR_Y + 2, menu.ammonia() > 0);
            bar(guiGraphics, ENERGY_BAR_X, BAR_Y, menu.energyFill(), ENERGY);
            guiGraphics.fill(
                    leftPos + PROGRESS_X,
                    topPos + PROGRESS_Y,
                    leftPos + PROGRESS_X + PROGRESS_WIDTH,
                    topPos + PROGRESS_Y + PROGRESS_HEIGHT,
                    DARK
            );
            guiGraphics.fill(
                    leftPos + PROGRESS_X + 1,
                    topPos + PROGRESS_Y + 1,
                    leftPos + PROGRESS_X + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.progressFill()),
                    topPos + PROGRESS_Y + PROGRESS_HEIGHT - 1,
                    PROGRESS
            );
            slot(guiGraphics, RESIDUE_SLOT_X, RESIDUE_SLOT_Y);
            statusIcon(guiGraphics, STATUS_X, STATUS_Y);
        } else if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_GEAR) {
            slot(guiGraphics, 65, 47);
            slot(guiGraphics, 125, 47);
        } else if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, STAT_ACCENT);
        } else {
            slot(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            slot(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
        }
        if (menu.selectedTab() != AmmoniaFuelCellMenu.TAB_STATS) {
            playerSlots(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != AmmoniaFuelCellMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_GEAR) {
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.fuel_cell_membrane.short"), 74);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 134);
        } else if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), 8, 18, 224, STAT_ACCENT);
        } else if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int index = 0; index < 4; index++) {
                if (isOverTab(mouseX, mouseY, index)) {
                    menu.selectTab(index);
                    return true;
                }
            }
            if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_PROCESSING
                    && FluidPurgeButton.handleClick(
                            menu,
                            leftPos,
                            topPos,
                            mouseX,
                            mouseY,
                            AMMONIA_BAR_X,
                            BAR_Y + 2,
                            AmmoniaFuelCellBlockEntity.PURGE_AMMONIA_TANK
                    )) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private MachineScreenStyle.StatLine[] statLines() {
        return new MachineScreenStyle.StatLine[] {
                MachineScreenStyle.generationBreakdownLine(
                        stat("rngtech.stat.energy_generation", menu.statValue(AmmoniaFuelCellMenu.energyGenerationDataIndex()), MachineStat.ENERGY_GENERATION),
                        menu.energyPerTick(),
                        menu.statValue(AmmoniaFuelCellMenu.baseEnergyGenerationDataIndex()),
                        menu.statValue(AmmoniaFuelCellMenu.flatEnergyGenerationDataIndex())
                ),
                stat("rngtech.stat.efficiency", menu.statValue(AmmoniaFuelCellMenu.efficiencyDataIndex()), MachineStat.EFFICIENCY),
                stat("rngtech.stat.processing_speed", menu.statValue(AmmoniaFuelCellMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED)
        };
    }

    private MachineScreenStyle.StatLine stat(String key, double value, MachineStat stat) {
        return MachineScreenStyle.statLine(
                Component.translatable(key),
                MachineScreenStyle.statValue(stat, value),
                value,
                false,
                value > 1.001,
                MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), stat, value)
        ).withStat(stat);
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        tab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"));
        tab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"));
        tab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"));
        tab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"));
    }

    private void tab(GuiGraphics guiGraphics, int index, Component label) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, menu.selectedTab() == index ? PANEL : 0xFF9A9A9A);
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, TEXT, false);
    }

    private void bar(GuiGraphics guiGraphics, int x, int y, float fill, int color) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 10, topPos + y + 50, DARK);
        int height = Math.round(48 * fill);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 49 - height, leftPos + x + 9, topPos + y + 49, color);
    }

    private void statusIcon(GuiGraphics guiGraphics, int x, int y) {
        int color = menu.statusCode() == AmmoniaFuelCellBlockEntity.STATUS_READY ? PROGRESS : 0xFFB45B4A;
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 12, topPos + y + 12, DARK);
        guiGraphics.fill(leftPos + x + 3, topPos + y + 3, leftPos + x + 9, topPos + y + 9, color);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case AmmoniaFuelCellBlockEntity.STATUS_MISSING_MEMBRANE -> Component.translatable("rngtech.ammonia_fuel_cell.status.no_membrane");
            case AmmoniaFuelCellBlockEntity.STATUS_NO_RECIPE -> Component.translatable("rngtech.ammonia_fuel_cell.status.no_recipe");
            case AmmoniaFuelCellBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.ammonia_fuel_cell.status.energy_full");
            case AmmoniaFuelCellBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.ammonia_fuel_cell.status.output_full");
            case AmmoniaFuelCellBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.ammonia_fuel_cell.status.no_ammonia");
            default -> Component.translatable("rngtech.ammonia_fuel_cell.status.ready");
        };
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable("rngtech.ammonia_fuel_cell.tooltip.progress.empty", CompactValueText.energyRate(menu.energyPerTick()));
        }
        return Component.translatable("rngtech.ammonia_fuel_cell.tooltip.progress", menu.progress(), ticks, CompactValueText.energyRate(menu.energyPerTick()));
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AmmoniaFuelCellMenu.TAB_PROCESSING) {
            return;
        }
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                AMMONIA_BAR_X,
                BAR_Y + 2,
                Component.translatable("rngtech.purge.target.ammonia_tank"),
                menu.ammonia() > 0 ? Component.translatable("fluid.rngtech.ammonia") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.ammonia(),
                menu.tankCapacity()
        )) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, AMMONIA_BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT,
                FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.ammonia"), menu.ammonia(), menu.tankCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, ENERGY_BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT,
                Component.translatable("rngtech.ammonia_fuel_cell.tooltip.energy", CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())));
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
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STATUS_X, STATUS_Y, STATUS_SIZE, STATUS_SIZE,
                Component.translatable("rngtech.ammonia_fuel_cell.tooltip.status", statusComponent()));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == AmmoniaFuelCellMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 8, 18, 224, statLines());
        }
    }

    private void playerSlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void slot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, 0xFF373737);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, 0xFFE0E0E0);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 54, MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
