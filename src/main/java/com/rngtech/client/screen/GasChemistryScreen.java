package com.rngtech.client.screen;

import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GasChemistryScreen extends AbstractContainerScreen<GasChemistryMenu> {
    private static final int PANEL = 0xFFC7C8C2;
    private static final int DARK = 0xFF565A5C;
    private static final int TEXT = 0xFF333333;
    private static final int MUTED = 0xFF686A68;
    private static final int ENERGY = 0xFF5D8DBA;
    private static final int WATER = 0xFF548BD4;
    private static final int GAS = 0xFFB69D54;
    private static final int OUTPUT = 0xFF75A882;
    private static final int SECONDARY = 0xFF8B8D90;
    private static final int PROGRESS = 0xFF5F9D69;
    private static final int STAT_ACCENT = 0xFF6B9085;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int GASIFIER_INPUT_SLOT_X = 70;
    private static final int GASIFIER_OUTPUT_SLOT_X = 142;
    private static final int GASIFIER_OUTPUT_BAR_X = 174;
    private static final int GASIFIER_PROGRESS_X = 96;
    private static final int GASIFIER_PROGRESS_WIDTH = 38;
    private static final int ENERGY_BAR_X = 28;
    private static final int WATER_BAR_X = 48;
    private static final int INPUT_BAR_X = 68;
    private static final int COMBUSTOR_INPUT_BAR_X = 48;
    private static final int COMBUSTOR_ENERGY_BAR_X = 174;
    private static final int STANDARD_OUTPUT_BAR_X = 154;
    private static final int SECONDARY_OUTPUT_BAR_X = 174;
    private static final int STANDARD_PROGRESS_X = 92;
    private static final int STANDARD_PROGRESS_WIDTH = 48;

    public GasChemistryScreen(GasChemistryMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == GasChemistryMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, STAT_ACCENT);
        } else {
            slot(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            slot(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
        }
        if (menu.selectedTab() != GasChemistryMenu.TAB_STATS) {
            playerSlots(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != GasChemistryMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == GasChemistryMenu.TAB_GEAR) {
            if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
                drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 55);
            }
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 91);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 127);
            if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
                drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.catalyst_bed.short"), 163);
            }
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), 8, 18, 224, STAT_ACCENT);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_REFINEMENT) {
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
            if (menu.selectedTab() == GasChemistryMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == GasChemistryMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int energyX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_ENERGY_BAR_X : ENERGY_BAR_X;
        bar(guiGraphics, energyX, 28, menu.energyFill(), ENERGY);
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            bar(guiGraphics, WATER_BAR_X, 28, menu.waterFill(), WATER);
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER) {
            int inputX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_INPUT_BAR_X : INPUT_BAR_X;
            bar(guiGraphics, inputX, 28, menu.inputFill(), GAS);
        }
        int outputX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_OUTPUT_BAR_X : STANDARD_OUTPUT_BAR_X;
        bar(guiGraphics, outputX, 28, menu.outputFill(), OUTPUT);
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            bar(guiGraphics, SECONDARY_OUTPUT_BAR_X, 28, menu.secondaryOutputFill(), SECONDARY);
        }
        if (menu.machine() == GasChemistryMachine.COAL_GASIFIER) {
            slot(guiGraphics, GASIFIER_INPUT_SLOT_X, 49);
            slot(guiGraphics, GASIFIER_OUTPUT_SLOT_X, 49);
        }
        int progressX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_X : STANDARD_PROGRESS_X;
        int progressWidth = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_WIDTH : STANDARD_PROGRESS_WIDTH;
        guiGraphics.fill(leftPos + progressX, topPos + 54, leftPos + progressX + progressWidth, topPos + 62, DARK);
        guiGraphics.fill(leftPos + progressX + 1, topPos + 55, leftPos + progressX + 1 + Math.round((progressWidth - 2) * menu.progressFill()), topPos + 61, PROGRESS);
        statusIcon(guiGraphics, 116, 70);
        renderPurgeButtons(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            slot(guiGraphics, 45, 47);
        }
        slot(guiGraphics, 81, 47);
        slot(guiGraphics, 117, 47);
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            slot(guiGraphics, 153, 47);
        }
    }

    private MachineScreenStyle.StatLine[] statLines() {
        if (menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR) {
            return new MachineScreenStyle.StatLine[] {
                    MachineScreenStyle.generationBreakdownLine(
                            stat("rngtech.stat.energy_generation", menu.statValue(GasChemistryMenu.energyGenerationDataIndex()), MachineStat.ENERGY_GENERATION),
                            menu.energyDelta(),
                            menu.statValue(GasChemistryMenu.baseEnergyGenerationDataIndex()),
                            menu.statValue(GasChemistryMenu.flatEnergyGenerationDataIndex())
                    ),
                    stat("rngtech.stat.efficiency", menu.statValue(GasChemistryMenu.efficiencyDataIndex()), MachineStat.EFFICIENCY),
                    stat("rngtech.stat.processing_speed", menu.statValue(GasChemistryMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED)
            };
        }
        return new MachineScreenStyle.StatLine[] {
                stat("rngtech.stat.energy_usage", menu.statValue(GasChemistryMenu.energyUsageDataIndex()), MachineStat.ENERGY_USAGE),
                stat("rngtech.stat.processing_speed", menu.statValue(GasChemistryMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED),
                stat("rngtech.stat.max_temperature", menu.statValue(GasChemistryMenu.maxTemperatureDataIndex()), MachineStat.MAX_TEMPERATURE)
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
        );
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
        int color = menu.statusCode() == GasChemistryBlockEntity.STATUS_READY ? PROGRESS : 0xFFB45B4A;
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 12, topPos + y + 12, DARK);
        guiGraphics.fill(leftPos + x + 3, topPos + y + 3, leftPos + x + 9, topPos + y + 9, color);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case GasChemistryBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.gas_chemistry.status.no_heat_core");
            case GasChemistryBlockEntity.STATUS_MISSING_CATALYST -> Component.translatable("rngtech.gas_chemistry.status.no_catalyst");
            case GasChemistryBlockEntity.STATUS_NO_RECIPE -> Component.translatable("rngtech.gas_chemistry.status.no_recipe");
            case GasChemistryBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.gas_chemistry.status.no_power");
            case GasChemistryBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.gas_chemistry.status.energy_full");
            case GasChemistryBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.gas_chemistry.status.output_full");
            case GasChemistryBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.gas_chemistry.status.no_fluid");
            case GasChemistryBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.gas_chemistry.status.no_input");
            default -> Component.translatable("rngtech.gas_chemistry.status.ready");
        };
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable("rngtech.gas_chemistry.tooltip.progress.empty", CompactValueText.energyRate(menu.energyDelta()));
        }
        return Component.translatable("rngtech.gas_chemistry.tooltip.progress", menu.progress(), ticks, CompactValueText.energyRate(menu.energyDelta()));
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != GasChemistryMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        int energyX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_ENERGY_BAR_X : ENERGY_BAR_X;
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, energyX, 28, 10, 50,
                Component.translatable("rngtech.gas_chemistry.tooltip.energy", CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())));
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, WATER_BAR_X, 28, 10, 50,
                    FluidMeterTooltips.amount(menu.waterFluidName(), menu.water(), menu.tankCapacity()));
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER) {
            int inputX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_INPUT_BAR_X : INPUT_BAR_X;
            CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, inputX, 28, 10, 50,
                    FluidMeterTooltips.amount(menu.inputFluidName(), menu.input(), menu.tankCapacity()));
        }
        int outputX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_OUTPUT_BAR_X : STANDARD_OUTPUT_BAR_X;
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, outputX, 28, 10, 50,
                FluidMeterTooltips.amount(menu.outputFluidName(), menu.output(), menu.tankCapacity()));
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, SECONDARY_OUTPUT_BAR_X, 28, 10, 50,
                    FluidMeterTooltips.amount(menu.secondaryOutputFluidName(), menu.secondaryOutput(), menu.tankCapacity()));
        }
        int progressX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_X : STANDARD_PROGRESS_X;
        int progressWidth = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_WIDTH : STANDARD_PROGRESS_WIDTH;
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, progressX, 54, progressWidth, 8, progressTooltip());
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 116, 70, 12, 12,
                Component.translatable("rngtech.gas_chemistry.tooltip.status", statusComponent()));
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            FluidPurgeButton.render(guiGraphics, leftPos, topPos, WATER_BAR_X, 30, menu.water() > 0);
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER) {
            FluidPurgeButton.render(guiGraphics, leftPos, topPos, inputBarX(), 30, menu.input() > 0);
        }
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, outputBarX(), 30, menu.output() > 0);
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            FluidPurgeButton.render(guiGraphics, leftPos, topPos, SECONDARY_OUTPUT_BAR_X, 30, menu.secondaryOutput() > 0);
        }
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR
                && FluidPurgeButton.handleClick(
                        menu,
                        leftPos,
                        topPos,
                        mouseX,
                        mouseY,
                        WATER_BAR_X,
                        30,
                        GasChemistryBlockEntity.PURGE_WATER_TANK
                )) {
            return true;
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER
                && FluidPurgeButton.handleClick(
                        menu,
                        leftPos,
                        topPos,
                        mouseX,
                        mouseY,
                        inputBarX(),
                        30,
                        GasChemistryBlockEntity.PURGE_INPUT_TANK
                )) {
            return true;
        }
        if (FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                outputBarX(),
                30,
                GasChemistryBlockEntity.PURGE_OUTPUT_TANK
        )) {
            return true;
        }
        return menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER
                && FluidPurgeButton.handleClick(
                        menu,
                        leftPos,
                        topPos,
                        mouseX,
                        mouseY,
                        SECONDARY_OUTPUT_BAR_X,
                        30,
                        GasChemistryBlockEntity.PURGE_SECONDARY_OUTPUT_TANK
                );
    }

    private boolean renderPurgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            if (FluidPurgeButton.renderTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    WATER_BAR_X,
                    30,
                    Component.translatable("rngtech.purge.target.water_tank"),
                    menu.water() > 0 ? menu.waterFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                    menu.water(),
                    menu.tankCapacity()
            )) {
                return true;
            }
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER) {
            if (FluidPurgeButton.renderTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    inputBarX(),
                    30,
                    Component.translatable("rngtech.purge.target.input_tank"),
                    menu.input() > 0 ? menu.inputFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                    menu.input(),
                    menu.tankCapacity()
            )) {
                return true;
            }
        }
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                outputBarX(),
                30,
                Component.translatable("rngtech.purge.target.output_tank"),
                menu.output() > 0 ? menu.outputFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.output(),
                menu.tankCapacity()
        )) {
            return true;
        }
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            return FluidPurgeButton.renderTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    SECONDARY_OUTPUT_BAR_X,
                    30,
                    Component.translatable("rngtech.purge.target.secondary_output_tank"),
                    menu.secondaryOutput() > 0 ? menu.secondaryOutputFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                    menu.secondaryOutput(),
                    menu.tankCapacity()
            );
        }
        return false;
    }

    private int inputBarX() {
        return menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_INPUT_BAR_X : INPUT_BAR_X;
    }

    private int outputBarX() {
        return menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_OUTPUT_BAR_X : STANDARD_OUTPUT_BAR_X;
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == GasChemistryMenu.TAB_STATS) {
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
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 34, MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
