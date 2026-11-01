package com.rngtech.client.screen;

import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.energy.CavitationRotorDynamics;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

public class CavitationGeneratorScreen extends AbstractContainerScreen<CavitationGeneratorMenu> implements MachineTabs.Host {
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
    private static final int STATUS_ICON_Y = 34;
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
    private static final int SPEED_GAUGE_Y = 85;
    private static final int SPEED_GAUGE_HEIGHT = 8;
    private static final int SPEED_POINTER_HEIGHT = 3;
    private static final int SPEED_DEAD_ZONE = 0xFF3A3A3A;
    private static final int SPEED_RATED_ZONE = 0xFF2E5A66;
    private static final int SPEED_SUPER_ZONE = 0xFF6A3A2E;
    private static final int SPEED_FILL = 0xFF5FD0E8;
    private static final int SPEED_SUPER_FILL = 0xFFF08A4B;
    private static final int SPEED_POINTER = 0xFFF2F2F2;
    private static final int THROTTLE_COARSE_STEP = 5;
    private static final int THROTTLE_NOTCH_STEP = 10;
    private static final double RESONANCE_HIGHLIGHT = 0.6;
    private static final int PITTED_LABEL_X = 192;
    private static final int PITTED_LABEL_Y = 47;
    private static final int PITTED_SLOT_X = 192;
    private static final int PITTED_SLOT_Y = 61;
    private static final int PROCESSING_LEVEL_ICON_X = 148;
    private static final int PROCESSING_LEVEL_ICON_Y = STATUS_ICON_Y;
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

    private final MachineTabs tabs;

    public CavitationGeneratorScreen(CavitationGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(CavitationGeneratorMenu.TAB_PROCESSING, ModBlocks.CAVITATION_GENERATOR.get())
                .gear(CavitationGeneratorMenu.TAB_GEAR)
                .stats(CavitationGeneratorMenu.TAB_STATS)
                .refinement(CavitationGeneratorMenu.TAB_REFINEMENT)
                .jewels(CavitationGeneratorMenu.TAB_JEWELS)
                .build();
    }

    @Override
    public MachineTabs machineTabs() {
        return tabs;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        tabs.renderTooltip(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_JEWELS) {
            List<Component> jewelTooltip = JewelsTab.tooltip(this);
            if (!jewelTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, jewelTooltip, mouseX, mouseY);
            }
        }
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_JEWELS) {
            JewelsTab.render(guiGraphics, font, menu, leftPos, topPos, TEXT, TEXT_MUTED, (x, y) -> renderSlotFrame(guiGraphics, x, y));
        }
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines().length, ENERGY);
        } else if (menu.selectedTab() == CavitationGeneratorMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), ENERGY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
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
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                menu.selectTab(tab);
                return true;
            }
            if (menu.selectedTab() == CavitationGeneratorMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING && isOverSpeedGauge(mouseX, mouseY)) {
                double fraction = (mouseX - (leftPos + PROGRESS_BAR_X + 1)) / (PROCESS_BAR_WIDTH - 2);
                sendThrottle((int) Math.round(fraction * 100.0));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == CavitationGeneratorMenu.TAB_PROCESSING && isOverSpeedGauge(mouseX, mouseY) && scrollY != 0.0) {
            int step = hasShiftDown() ? THROTTLE_COARSE_STEP : 1;
            sendThrottle(menu.throttle() + (scrollY > 0.0 ? step : -step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendThrottle(int throttle) {
        int clamped = CavitationRotorDynamics.clampThrottle(throttle);
        if (clamped == menu.throttle() || minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = CavitationGeneratorMenu.throttleButtonId(clamped);
        if (menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private boolean isOverSpeedGauge(double mouseX, double mouseY) {
        int left = leftPos + PROGRESS_BAR_X;
        int top = topPos + SPEED_GAUGE_Y;
        return mouseX >= left && mouseX < left + PROCESS_BAR_WIDTH && mouseY >= top && mouseY < top + SPEED_GAUGE_HEIGHT + SPEED_POINTER_HEIGHT;
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
        renderSpeedGauge(guiGraphics);
        renderProcessingIcons(guiGraphics);
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
        renderPurgeButtons(guiGraphics);
    }

    private void renderHorizontalMeter(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, left + 1 + Math.round((width - 2) * progress), top + height - 1, color);
    }

    /** Rotor speed over its regimes: no cavitation, up to rated, and supercavitation. The pointer marks the throttle. */
    private void renderSpeedGauge(GuiGraphics guiGraphics) {
        int left = leftPos + PROGRESS_BAR_X;
        int top = topPos + SPEED_GAUGE_Y;
        int inner = PROCESS_BAR_WIDTH - 2;
        int onset = left + 1 + speedOffset(CavitationRotorDynamics.ONSET_SPEED, inner);
        int rated = left + 1 + speedOffset(CavitationRotorDynamics.RATED_SPEED, inner);
        int right = left + 1 + inner;
        int bottom = top + SPEED_GAUGE_HEIGHT - 1;
        guiGraphics.fill(left, top, left + PROCESS_BAR_WIDTH, top + SPEED_GAUGE_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, onset, bottom, SPEED_DEAD_ZONE);
        guiGraphics.fill(onset, top + 1, rated, bottom, SPEED_RATED_ZONE);
        guiGraphics.fill(rated, top + 1, right, bottom, SPEED_SUPER_ZONE);

        double speed = menu.rotorRpm() / (double) CavitationRotorDynamics.MAX_RPM;
        int fill = left + 1 + speedOffset(speed, inner);
        guiGraphics.fill(left + 1, top + 3, Math.min(fill, rated), bottom - 2, SPEED_FILL);
        if (fill > rated) {
            guiGraphics.fill(rated, top + 3, fill, bottom - 2, SPEED_SUPER_FILL);
        }

        for (int notch = CavitationRotorDynamics.MIN_THROTTLE; notch <= CavitationRotorDynamics.MAX_THROTTLE; notch += THROTTLE_NOTCH_STEP) {
            int x = left + 1 + speedOffset(CavitationRotorDynamics.speedOf(notch), inner);
            guiGraphics.fill(Math.min(x, right - 1), top + 1, Math.min(x, right - 1) + 1, top + 2, 0xFF8B8B8B);
        }
        int pointer = Math.min(left + 1 + speedOffset(CavitationRotorDynamics.targetSpeed(menu.throttle()), inner), right - 1);
        int pointerTop = top + SPEED_GAUGE_HEIGHT;
        guiGraphics.fill(pointer, pointerTop - 1, pointer + 1, pointerTop + SPEED_POINTER_HEIGHT, SPEED_POINTER);
        guiGraphics.fill(pointer - 1, pointerTop + 1, pointer + 2, pointerTop + SPEED_POINTER_HEIGHT, SPEED_POINTER);
    }

    /** Strain against its cap, the FE it costs now and at the cap, and how fast it cools. */
    private List<Component> strainTooltip() {
        double maxPenalty = menu.maxStrainPenalty();
        double penalty = menu.maxHeatStrain() <= 0 ? 0.0 : maxPenalty * Math.min(1.0, menu.heatStrain() / (double) menu.maxHeatStrain());
        return List.of(
                Component.translatable("rngtech.cavitation.tooltip.strain", menu.heatStrain(), menu.maxHeatStrain(), menu.recipeStrain()),
                Component.translatable("rngtech.cavitation.tooltip.strain_penalty", percent(penalty), percent(maxPenalty)).withStyle(ChatFormatting.GRAY),
                Component.translatable("rngtech.cavitation.tooltip.strain_cooling", multiplier(menu.coolingRate())).withStyle(ChatFormatting.GRAY)
        );
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f", fraction * 100.0);
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static int speedOffset(double speed, int width) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, speed)) * width);
    }

    private List<Component> speedGaugeTooltip() {
        int throttle = menu.throttle();
        double target = CavitationRotorDynamics.targetSpeed(throttle);
        String regime = throttle < CavitationRotorDynamics.RATED_THROTTLE
                ? "below_rated"
                : throttle == CavitationRotorDynamics.RATED_THROTTLE ? "rated" : "supercavitation";
        return List.of(
                Component.translatable("rngtech.cavitation.tooltip.rotor_speed", menu.rotorRpm(), CavitationRotorDynamics.rpmOf(throttle)),
                Component.translatable("rngtech.cavitation.tooltip.throttle", throttle, Component.translatable("rngtech.cavitation.throttle." + regime)),
                Component.translatable("rngtech.cavitation.tooltip.live_wear", multiplier(menu.liveWear()))
                        .withStyle(menu.liveResonance() < RESONANCE_HIGHLIGHT ? ChatFormatting.AQUA : ChatFormatting.WHITE),
                Component.translatable(
                        "rngtech.cavitation.tooltip.throttle_effects",
                        multiplier(CavitationRotorDynamics.generation(target)),
                        multiplier(CavitationRotorDynamics.heat(target)),
                        multiplier(CavitationRotorDynamics.flow(target))
                ).withStyle(ChatFormatting.GRAY),
                Component.translatable("rngtech.cavitation.tooltip.heat_balance", multiplier(menu.throttleHeat()), multiplier(menu.coolingRate()))
                        .withStyle(menu.throttleHeat() > menu.coolingRate() ? ChatFormatting.RED : ChatFormatting.GRAY),
                Component.translatable("rngtech.cavitation.tooltip.onset", CavitationRotorDynamics.rpm(CavitationRotorDynamics.ONSET_SPEED)).withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    private static String multiplier(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
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
            case CavitationGeneratorBlockEntity.STATUS_READY, CavitationGeneratorBlockEntity.STATUS_SPOOLING -> FLUID;
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
            case CavitationGeneratorBlockEntity.STATUS_SPOOLING -> Component.translatable("rngtech.cavitation.status.spooling");
            default -> Component.translatable("rngtech.cavitation.status.ready");
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != CavitationGeneratorMenu.TAB_PROCESSING) {
            return;
        }
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
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        if (isOverSpeedGauge(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, speedGaugeTooltip(), mouseX, mouseY);
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, FLUID_METER_X, FLUID_METER_Y, FLUID_METER_WIDTH, FLUID_METER_HEIGHT,
                FluidMeterTooltips.amount(menu.inputFluidName(), menu.inputFluid(), menu.inputFluidCapacity(), "-" + menu.waterPerCycle() + " mB"));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_METER_X, OUTPUT_METER_Y, OUTPUT_METER_WIDTH, OUTPUT_METER_HEIGHT,
                FluidMeterTooltips.amount(menu.outputFluidName(), menu.outputFluid(), menu.outputFluidCapacity(), "+" + menu.recipeFluidOutput() + " mB"));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, ENERGY_METER_X, ENERGY_METER_Y, ENERGY_METER_WIDTH, ENERGY_METER_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PROGRESS_BAR_X, PROGRESS_BAR_Y, PROCESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                Component.translatable("rngtech.cavitation.tooltip.progress", menu.progress(), menu.processingTicks(), cycleYield()));
        if (isOver(mouseX, mouseY, PROGRESS_BAR_X, STRAIN_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, strainTooltip(), mouseX, mouseY);
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PROGRESS_BAR_X, WEAR_BAR_Y, PROCESS_BAR_WIDTH, STRAIN_WEAR_BAR_HEIGHT,
                Component.translatable("rngtech.cavitation.tooltip.wear", menu.rotorWear(), menu.rotorWearLimit(), String.format(Locale.ROOT, "%.1f", menu.recipeWear()), menu.ratedPressure()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STATUS_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.status", statusComponent()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, GENERATION_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                generationTooltip());
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.output", CompactValueText.energyRate(menu.maxOutput())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, PRESSURE_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE,
                Component.translatable("rngtech.cavitation.tooltip.pressure", menu.pressureRating(), menu.ratedPressure()));
    }

    /** What one cycle makes: its fluid on a fluid-output cycle, which makes no FE, otherwise its FE. */
    private Component cycleYield() {
        if (menu.recipeFluidOutput() > 0) {
            return Component.translatable("rngtech.cavitation.tooltip.cycle_fluid", menu.recipeFluidOutput(), menu.outputFluidName());
        }
        return Component.literal(CompactValueText.energyAmount(menu.recipeEnergy()));
    }

    /** A cycle with a fluid output makes no FE, so it shows the fluid rate and what Energy Generation adds to it. */
    private List<Component> generationTooltip() {
        if (menu.recipeFluidOutput() <= 0) {
            return List.of(Component.translatable("rngtech.cavitation.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick())));
        }
        return List.of(
                Component.translatable("rngtech.cavitation.tooltip.fluid_product", menu.outputFluidName(), CompactValueText.fluidRate(menu.fluidOutputRate())),
                generationFluidLine().withStyle(ChatFormatting.GRAY)
        );
    }

    private MutableComponent generationFluidLine() {
        return Component.translatable(
                "rngtech.cavitation.tooltip.generation_to_fluid",
                percent(CavitationGeneratorBlockEntity.GENERATION_TO_FLUID),
                signedPercent(menu.generationFluidBonus()),
                menu.outputFluidName()
        );
    }

    private static String signedPercent(double fraction) {
        return (fraction < 0.0 ? "" : "+") + String.format(Locale.ROOT, "%.1f", fraction * 100.0).replaceAll("\\.0$", "") + "%";
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

    /** Fluid Transfer only drains a fluid output, so it shows only while the recipe or the output tank has one. */
    private MachineScreenStyle.StatLine[] statLines() {
        boolean fluidOutput = menu.recipeFluidOutput() > 0 || menu.outputFluid() > 0;
        List<MachineScreenStyle.StatLine> lines = new java.util.ArrayList<>();
        List<MachineStat> shown = new java.util.ArrayList<>();
        for (int index = 0; index < STAT_LABEL_KEYS.length; index++) {
            if (STAT_TYPES[index] == MachineStat.FLUID_TRANSFER && !fluidOutput) {
                continue;
            }
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            shown.add(STAT_TYPES[index]);
            lines.add(MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    statValue(index),
                    value,
                    STAT_TYPES[index] == MachineStat.PROCESSING_LEVEL,
                    value > 1.001,
                    MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), STAT_TYPES[index], value)
            ).withStat(STAT_TYPES[index]));
        }
        if (menu.recipeFluidOutput() > 0) {
            return MachineScreenStyle.withGenerationAsFluid(
                    shown.toArray(MachineStat[]::new),
                    lines.toArray(MachineScreenStyle.StatLine[]::new),
                    signedPercent(menu.generationFluidBonus()) + " mB",
                    List.of(
                            Component.translatable("rngtech.cavitation.tooltip.no_fe", menu.outputFluidName()),
                            generationFluidLine().withStyle(ChatFormatting.GRAY)
                    )
            );
        }
        return MachineScreenStyle.withGenerationBreakdown(
                shown.toArray(MachineStat[]::new),
                lines.toArray(MachineScreenStyle.StatLine[]::new),
                menu.energyPerTick(),
                menu.statValue(CavitationGeneratorMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(CavitationGeneratorMenu.flatEnergyGenerationDataIndex()),
                menu.packGenerationMultiplier()
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
}
