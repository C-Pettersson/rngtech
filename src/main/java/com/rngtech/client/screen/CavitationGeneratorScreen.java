package com.rngtech.client.screen;

import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CavitationGeneratorScreen extends AbstractContainerScreen<CavitationGeneratorMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF5BA850;
    private static final int FLUID = 0xFF4A8FB8;
    private static final int NITROGEN = 0xFF8BA4C9;
    private static final int PROGRESS = 0xFF8B6F4A;
    private static final int STRAIN = 0xFFD38A35;
    private static final int WEAR = 0xFFB45B4A;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int FLUID_METER_X = 8;
    private static final int FLUID_METER_Y = 27;
    private static final int FLUID_METER_WIDTH = 16;
    private static final int FLUID_METER_HEIGHT = 52;
    private static final int FLUID_SLOT_X = 28;
    private static final int FLUID_SLOT_Y = 61;
    private static final int OUTPUT_METER_X = 52;
    private static final int OUTPUT_METER_Y = 27;
    private static final int OUTPUT_METER_WIDTH = 16;
    private static final int OUTPUT_METER_HEIGHT = 52;
    private static final int ENERGY_METER_X = 176;
    private static final int ENERGY_METER_Y = 27;
    private static final int ENERGY_METER_WIDTH = 12;
    private static final int ENERGY_METER_HEIGHT = 52;
    private static final int STATUS_ICON_X = 84;
    private static final int STATUS_ICON_Y = 30;
    private static final int GENERATION_ICON_X = 100;
    private static final int OUTPUT_ICON_X = 116;
    private static final int PRESSURE_ICON_X = 132;
    private static final int ICON_SIZE = 12;
    private static final int PROGRESS_BAR_X = 84;
    private static final int PROGRESS_BAR_Y = 49;
    private static final int PROCESS_BAR_WIDTH = 72;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STRAIN_BAR_Y = 61;
    private static final int WEAR_BAR_Y = 73;
    private static final int STRAIN_WEAR_BAR_HEIGHT = 6;
    private static final int PITTED_LABEL_X = 192;
    private static final int PITTED_LABEL_Y = 47;
    private static final int PITTED_SLOT_X = 192;
    private static final int PITTED_SLOT_Y = 61;
    private static final int PROCESSING_LEVEL_ICON_X = 184;
    private static final int PROCESSING_LEVEL_ICON_Y = 78;
    private static final int PROCESSING_LEVEL_MAX = 4;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.efficiency",
            "rngtech.stat.processing_speed",
            "rngtech.stat.processing_level",
            "rngtech.stat.stability",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.output_amount",
            "rngtech.stat.fluid_transfer"
    };
    private static final int[] STAT_DATA_INDICES = {
            CavitationGeneratorMenu.energyGenerationDataIndex(),
            CavitationGeneratorMenu.energyCapacityDataIndex(),
            CavitationGeneratorMenu.efficiencyDataIndex(),
            CavitationGeneratorMenu.processingSpeedDataIndex(),
            CavitationGeneratorMenu.processingLevelDataIndex(),
            CavitationGeneratorMenu.stabilityDataIndex(),
            CavitationGeneratorMenu.temperatureStabilityDataIndex(),
            CavitationGeneratorMenu.outputAmountDataIndex(),
            CavitationGeneratorMenu.fluidTransferDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.EFFICIENCY,
            MachineStat.PROCESSING_SPEED,
            MachineStat.PROCESSING_LEVEL,
            MachineStat.STABILITY,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OUTPUT_AMOUNT,
            MachineStat.FLUID_TRANSFER
    };

    public CavitationGeneratorScreen(CavitationGeneratorMenu menu, Inventory playerInventory, Component title) {
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
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines().length, ENERGY);
        } else {
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), ENERGY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != CavitationGeneratorMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING) {
            guiGraphics.drawString(font, Component.translatable("rngtech.cavitation.pitted"), PITTED_LABEL_X, PITTED_LABEL_Y, TEXT_MUTED, false);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_GEAR) {
            guiGraphics.drawString(font, Component.translatable("rngtech.gear.rotor.short"), 13, 35, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.gear.nozzle.short"), 48, 35, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.gear.heat_core.short"), 84, 35, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.gear.battery_cell.short"), 118, 35, TEXT_MUTED, false);
            guiGraphics.drawString(font, Component.translatable("rngtech.gear.servo.short"), 150, 35, TEXT_MUTED, false);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, ENERGY);
        } else {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), ENERGY);
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
            if (menu.selectedTab() == CavitationGeneratorMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == CavitationGeneratorMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == CavitationGeneratorMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == CavitationGeneratorMenu.TAB_REFINEMENT);
    }

    private void renderTab(GuiGraphics guiGraphics, int index, Component label, boolean selected) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, selected ? PANEL : 0xFF9A9A9A);
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, selected ? TEXT : 0xFF2F2F2F, false);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, FLUID_SLOT_X, FLUID_SLOT_Y);
            renderSlotFrame(guiGraphics, PITTED_SLOT_X, PITTED_SLOT_Y);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 18, 47);
            renderSlotFrame(guiGraphics, 52, 47);
            renderSlotFrame(guiGraphics, 86, 47);
            renderSlotFrame(guiGraphics, 120, 47);
            renderSlotFrame(guiGraphics, 154, 47);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }
        if (menu.selectedTab() != CavitationGeneratorMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + FLUID_METER_X,
                y + FLUID_METER_Y,
                x + FLUID_METER_X + FLUID_METER_WIDTH,
                y + FLUID_METER_Y + FLUID_METER_HEIGHT,
                0xFF5F5F5F
        );
        int fluidHeight = Math.round((FLUID_METER_HEIGHT - 4) * menu.fluidProgress());
        int fluidBottom = y + FLUID_METER_Y + FLUID_METER_HEIGHT - 2;
        FluidBarRenderer.fill(guiGraphics, menu.inputFluidType(), x + FLUID_METER_X + 2, fluidBottom - fluidHeight, x + FLUID_METER_X + FLUID_METER_WIDTH - 2, fluidBottom, FLUID);

        guiGraphics.fill(
                x + OUTPUT_METER_X,
                y + OUTPUT_METER_Y,
                x + OUTPUT_METER_X + OUTPUT_METER_WIDTH,
                y + OUTPUT_METER_Y + OUTPUT_METER_HEIGHT,
                0xFF5F5F5F
        );
        int outputHeight = Math.round((OUTPUT_METER_HEIGHT - 4) * menu.outputFluidProgress());
        int outputBottom = y + OUTPUT_METER_Y + OUTPUT_METER_HEIGHT - 2;
        FluidBarRenderer.fill(guiGraphics, menu.outputFluidType(), x + OUTPUT_METER_X + 2, outputBottom - outputHeight, x + OUTPUT_METER_X + OUTPUT_METER_WIDTH - 2, outputBottom, NITROGEN);

        guiGraphics.fill(
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH,
                y + ENERGY_METER_Y + ENERGY_METER_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round((ENERGY_METER_HEIGHT - 4) * menu.energyProgress());
        int energyBottom = y + ENERGY_METER_Y + ENERGY_METER_HEIGHT - 2;
        guiGraphics.fill(x + ENERGY_METER_X + 2, energyBottom - energyHeight, x + ENERGY_METER_X + ENERGY_METER_WIDTH - 2, energyBottom, ENERGY);

        renderHorizontalMeter(guiGraphics, PROGRESS_BAR_X, PROGRESS_BAR_Y, PROCESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, menu.processingProgress(), PROGRESS);
        renderHorizontalMeter(guiGraphics, PROGRESS_BAR_X, STRAIN_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT, menu.strainProgress(), STRAIN);
        renderHorizontalMeter(guiGraphics, PROGRESS_BAR_X, WEAR_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT, menu.wearProgress(), WEAR);
        renderProcessingIcons(guiGraphics);
        renderPurgeButtons(guiGraphics);
    }

    private void renderHorizontalMeter(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, left + 1 + Math.round((width - 2) * progress), top + height - 1, color);
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
                ENERGY
        );
    }

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor());

        renderIconBox(guiGraphics, GENERATION_ICON_X, STATUS_ICON_Y);
        int sparkX = leftPos + GENERATION_ICON_X;
        int sparkY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, ENERGY);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, ENERGY);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, ENERGY);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, ENERGY);

        renderIconBox(guiGraphics, OUTPUT_ICON_X, STATUS_ICON_Y);
        int arrowX = leftPos + OUTPUT_ICON_X;
        int arrowY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(arrowX + 3, arrowY + 5, arrowX + 8, arrowY + 7, FLUID);
        guiGraphics.fill(arrowX + 8, arrowY + 4, arrowX + 10, arrowY + 8, FLUID);
        guiGraphics.fill(arrowX + 10, arrowY + 5, arrowX + 11, arrowY + 7, FLUID);

        renderIconBox(guiGraphics, PRESSURE_ICON_X, STATUS_ICON_Y);
        int pressureX = leftPos + PRESSURE_ICON_X;
        int pressureY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(pressureX + 3, pressureY + 7, pressureX + 10, pressureY + 9, PROGRESS);
        guiGraphics.fill(pressureX + 5, pressureY + 4, pressureX + 8, pressureY + 7, PROGRESS);
        guiGraphics.fill(pressureX + 6, pressureY + 2, pressureX + 7, pressureY + 4, PROGRESS);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case CavitationGeneratorBlockEntity.STATUS_READY -> FLUID;
            case CavitationGeneratorBlockEntity.STATUS_MISSING_ROTOR,
                    CavitationGeneratorBlockEntity.STATUS_MISSING_NOZZLE,
                    CavitationGeneratorBlockEntity.STATUS_NO_FLUID -> PANEL_DARK;
            case CavitationGeneratorBlockEntity.STATUS_ENERGY_FULL,
                    CavitationGeneratorBlockEntity.STATUS_FLUID_OUTPUT_FULL,
                    CavitationGeneratorBlockEntity.STATUS_REDSTONE_DISABLED -> 0xFFD3A33A;
            default -> WEAR;
        };
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case CavitationGeneratorBlockEntity.STATUS_MISSING_ROTOR -> Component.translatable("rngtech.cavitation.status.no_rotor");
            case CavitationGeneratorBlockEntity.STATUS_MISSING_NOZZLE -> Component.translatable("rngtech.cavitation.status.no_nozzle");
            case CavitationGeneratorBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.cavitation.status.no_fluid");
            case CavitationGeneratorBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.cavitation.status.invalid_recipe");
            case CavitationGeneratorBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.cavitation.status.blocked_stage");
            case CavitationGeneratorBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.cavitation.status.energy_full");
            case CavitationGeneratorBlockEntity.STATUS_STRAIN_HIGH -> Component.translatable("rngtech.cavitation.status.strain_high");
            case CavitationGeneratorBlockEntity.STATUS_ROTOR_WORN -> Component.translatable("rngtech.cavitation.status.rotor_worn");
            case CavitationGeneratorBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.cavitation.status.output_full");
            case CavitationGeneratorBlockEntity.STATUS_FLUID_OUTPUT_FULL -> Component.translatable("rngtech.cavitation.status.fluid_output_full");
            case CavitationGeneratorBlockEntity.STATUS_REDSTONE_DISABLED -> Component.translatable("rngtech.cavitation.status.redstone");
            default -> Component.translatable("rngtech.cavitation.status.ready");
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_GEAR) {
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
        if (menu.selectedTab() != CavitationGeneratorMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, FLUID_METER_X, FLUID_METER_Y, FLUID_METER_WIDTH, FLUID_METER_HEIGHT,
                FluidMeterTooltips.amount(menu.inputFluidName(), menu.inputFluid(), menu.inputFluidCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_METER_X, OUTPUT_METER_Y, OUTPUT_METER_WIDTH, OUTPUT_METER_HEIGHT,
                FluidMeterTooltips.amount(menu.outputFluidName(), menu.outputFluid(), menu.outputFluidCapacity(), "+" + menu.recipeFluidOutput() + " mB"));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, ENERGY_METER_X, ENERGY_METER_Y, ENERGY_METER_WIDTH, ENERGY_METER_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PROGRESS_BAR_X, PROGRESS_BAR_Y, PROCESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                Component.translatable("rngtech.cavitation.tooltip.progress", menu.progress(), menu.processingTicks(), CompactValueText.energyAmount(menu.recipeEnergy())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PROGRESS_BAR_X, STRAIN_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT,
                Component.translatable("rngtech.cavitation.tooltip.strain", menu.heatStrain(), menu.maxHeatStrain(), menu.recipeStrain()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PROGRESS_BAR_X, WEAR_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT,
                Component.translatable("rngtech.cavitation.tooltip.wear", menu.rotorWear(), menu.rotorWearLimit(), menu.recipeWear(), menu.pressureRating()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STATUS_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.status", statusComponent()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, GENERATION_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.output", CompactValueText.energyRate(menu.maxOutput())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PRESSURE_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.pressure", menu.pressureRating()));
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, FLUID_METER_X + 3, FLUID_METER_Y + 2, menu.inputFluid() > 0);
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, OUTPUT_METER_X + 3, OUTPUT_METER_Y + 2, menu.outputFluid() > 0);
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FLUID_METER_X + 3,
                FLUID_METER_Y + 2,
                CavitationGeneratorBlockEntity.PURGE_INPUT_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_METER_X + 3,
                OUTPUT_METER_Y + 2,
                CavitationGeneratorBlockEntity.PURGE_OUTPUT_TANK
        );
    }

    private boolean renderPurgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FLUID_METER_X + 3,
                FLUID_METER_Y + 2,
                Component.translatable("rngtech.purge.target.input_tank"),
                menu.inputFluidName(),
                menu.inputFluid(),
                menu.inputFluidCapacity()
        )) {
            return true;
        }
        return FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_METER_X + 3,
                OUTPUT_METER_Y + 2,
                Component.translatable("rngtech.purge.target.output_tank"),
                menu.outputFluidName(),
                menu.outputFluid(),
                menu.outputFluidCapacity()
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines());
        }
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
                    value > 1.001,
                    MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), STAT_TYPES[index], value)
            ).withStat(STAT_TYPES[index]);
        }
        return MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(CavitationGeneratorMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(CavitationGeneratorMenu.flatEnergyGenerationDataIndex())
        );
    }

    private String statValue(int index) {
        double value = menu.statValue(STAT_DATA_INDICES[index]);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
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
}
