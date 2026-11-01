package com.rngtech.client.screen;

import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.CombustionMixture;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GasChemistryScreen extends AbstractContainerScreen<GasChemistryMenu> implements MachineTabs.Host {
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
    private static final int MIXTURE_X = 72;
    private static final int MIXTURE_Y = 66;
    private static final int MIXTURE_WIDTH = 72;
    private static final int MIXTURE_HEIGHT = 8;
    private static final int MIXTURE_POINTER_HEIGHT = 3;
    private static final int MIXTURE_LEAN_ZONE = 0xFF2E4A5E;
    private static final int MIXTURE_RICH_ZONE = 0xFF6A4A2E;
    private static final int MIXTURE_FILL = 0xFFF0A860;
    private static final int MIXTURE_POINTER = 0xFFF2F2F2;
    private static final int MIXTURE_NOTCH = 0xFF3A3C3E;
    private static final int MIXTURE_RATED_MARK = 0xFFE6E6E0;
    private static final int MIXTURE_COARSE_STEP = 5;
    private static final int MIXTURE_NOTCH_STEP = 10;

    private final MachineTabs tabs;

    public GasChemistryScreen(GasChemistryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(GasChemistryMenu.TAB_PROCESSING, processingBlock(menu.machine()))
                .gear(GasChemistryMenu.TAB_GEAR)
                .stats(GasChemistryMenu.TAB_STATS)
                .refinement(GasChemistryMenu.TAB_REFINEMENT)
                .jewels(GasChemistryMenu.TAB_JEWELS)
                .style(new MachineTabs.Style(PANEL, 0xFFE9E9E9, 0xFF8B8B8B, 0xFF9A9A9A, 0xFFB0B0B0, 0xFF3F3F3F, 0xFF2F2F2F))
                .build();
    }

    private static Block processingBlock(GasChemistryMachine machine) {
        return switch (machine) {
            case COAL_GASIFIER -> ModBlocks.COAL_GASIFIER.get();
            case SYNGAS_COMBUSTOR -> ModBlocks.SYNGAS_COMBUSTOR.get();
            case STEAM_METHANE_REFORMER -> ModBlocks.STEAM_METHANE_REFORMER.get();
        };
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
        if (menu.selectedTab() == GasChemistryMenu.TAB_JEWELS) {
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        if (menu.selectedTab() == GasChemistryMenu.TAB_JEWELS) {
            JewelsTab.render(guiGraphics, font, menu, leftPos, topPos, TEXT, MUTED, (x, y) -> slot(guiGraphics, x, y));
        }
        if (menu.selectedTab() == GasChemistryMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, STAT_ACCENT);
        } else if (menu.selectedTab() == GasChemistryMenu.TAB_REFINEMENT) {
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
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, MUTED);
        if (menu.selectedTab() != GasChemistryMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == GasChemistryMenu.TAB_GEAR) {
            if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
                drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 54);
            }
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 90);
            drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 126);
            if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
                drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.catalyst_bed.short"), 162);
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
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                menu.selectTab(tab);
                return true;
            }
            if (menu.selectedTab() == GasChemistryMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == GasChemistryMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
            if (isOverMixtureGauge(mouseX, mouseY)) {
                double fraction = (mouseX - (leftPos + MIXTURE_X + 1)) / (MIXTURE_WIDTH - 2);
                int span = CombustionMixture.MAX_MIXTURE - CombustionMixture.MIN_MIXTURE;
                sendMixture(CombustionMixture.MIN_MIXTURE + (int) Math.round(fraction * span));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverMixtureGauge(mouseX, mouseY) && scrollY != 0.0) {
            int step = hasShiftDown() ? MIXTURE_COARSE_STEP : 1;
            sendMixture(menu.mixture() + (scrollY > 0.0 ? step : -step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendMixture(int mixture) {
        int clamped = CombustionMixture.clamp(mixture);
        if (clamped == menu.mixture() || minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = GasChemistryMenu.mixtureButtonId(clamped);
        if (menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private boolean isOverMixtureGauge(double mouseX, double mouseY) {
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR || menu.selectedTab() != GasChemistryMenu.TAB_PROCESSING) {
            return false;
        }
        int left = leftPos + MIXTURE_X;
        int top = topPos + MIXTURE_Y;
        return mouseX >= left && mouseX < left + MIXTURE_WIDTH && mouseY >= top && mouseY < top + MIXTURE_HEIGHT + MIXTURE_POINTER_HEIGHT;
    }

    /** The fuel mixture from lean to rich, with the rated mixture marked. The pointer marks the selected mixture. */
    private void renderMixtureGauge(GuiGraphics guiGraphics) {
        int left = leftPos + MIXTURE_X;
        int top = topPos + MIXTURE_Y;
        int inner = MIXTURE_WIDTH - 2;
        int rated = left + 1 + mixtureOffset(CombustionMixture.RATED_MIXTURE, inner);
        int right = left + 1 + inner;
        int bottom = top + MIXTURE_HEIGHT - 1;
        guiGraphics.fill(left, top, left + MIXTURE_WIDTH, top + MIXTURE_HEIGHT, DARK);
        guiGraphics.fill(left + 1, top + 1, rated, bottom, MIXTURE_LEAN_ZONE);
        guiGraphics.fill(rated, top + 1, right, bottom, MIXTURE_RICH_ZONE);
        int pointer = Math.min(left + 1 + mixtureOffset(menu.mixture(), inner), right - 1);
        guiGraphics.fill(left + 1, top + 3, Math.max(left + 1, pointer), bottom - 2, MIXTURE_FILL);
        for (int notch = CombustionMixture.MIN_MIXTURE; notch <= CombustionMixture.MAX_MIXTURE; notch += MIXTURE_NOTCH_STEP) {
            int x = Math.min(left + 1 + mixtureOffset(notch, inner), right - 1);
            guiGraphics.fill(x, top + 1, x + 1, top + 2, MIXTURE_NOTCH);
        }
        guiGraphics.fill(rated, top + 1, rated + 1, bottom, MIXTURE_RATED_MARK);
        int pointerTop = top + MIXTURE_HEIGHT;
        guiGraphics.fill(pointer, pointerTop - 1, pointer + 1, pointerTop + MIXTURE_POINTER_HEIGHT, MIXTURE_POINTER);
        guiGraphics.fill(pointer - 1, pointerTop + 1, pointer + 2, pointerTop + MIXTURE_POINTER_HEIGHT, MIXTURE_POINTER);
    }

    private static int mixtureOffset(int mixture, int width) {
        int span = CombustionMixture.MAX_MIXTURE - CombustionMixture.MIN_MIXTURE;
        int clamped = CombustionMixture.clamp(mixture) - CombustionMixture.MIN_MIXTURE;
        return (int) Math.round(clamped / (double) span * width);
    }

    private List<Component> mixtureTooltip() {
        int mixture = menu.mixture();
        String regime = mixture < CombustionMixture.RATED_MIXTURE
                ? "lean"
                : mixture == CombustionMixture.RATED_MIXTURE ? "rated" : "rich";
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "rngtech.syngas_combustor.tooltip.mixture",
                mixture,
                Component.translatable("rngtech.syngas_combustor.mixture." + regime)
        ));
        lines.add(Component.translatable(
                "rngtech.syngas_combustor.tooltip.mixture_effects",
                multiplier(CombustionMixture.fuelRate(mixture)),
                multiplier(CombustionMixture.power(mixture)),
                multiplier(CombustionMixture.energyPerFuel(mixture))
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "rngtech.syngas_combustor.tooltip.mixture_exhaust",
                multiplier(CombustionMixture.exhaustPerFuel(mixture))
        ).withStyle(ChatFormatting.GRAY));
        if (menu.exhaustPerCycle() > 0) {
            lines.add(Component.translatable("rngtech.syngas_combustor.tooltip.exhaust_per_cycle", menu.exhaustPerCycle())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("rngtech.syngas_combustor.tooltip.mixture_hint").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static String multiplier(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int energyX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_ENERGY_BAR_X : ENERGY_BAR_X;
        bar(guiGraphics, energyX, 28, menu.energyFill(), Fluids.EMPTY, ENERGY);
        if (menu.machine() != GasChemistryMachine.SYNGAS_COMBUSTOR) {
            bar(guiGraphics, WATER_BAR_X, 28, menu.waterFill(), menu.waterFluidType(), WATER);
        }
        if (menu.machine() != GasChemistryMachine.COAL_GASIFIER) {
            int inputX = menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR ? COMBUSTOR_INPUT_BAR_X : INPUT_BAR_X;
            bar(guiGraphics, inputX, 28, menu.inputFill(), menu.inputFluidType(), GAS);
        }
        int outputX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_OUTPUT_BAR_X : STANDARD_OUTPUT_BAR_X;
        bar(guiGraphics, outputX, 28, menu.outputFill(), menu.outputFluidType(), OUTPUT);
        if (menu.machine() == GasChemistryMachine.STEAM_METHANE_REFORMER) {
            bar(guiGraphics, SECONDARY_OUTPUT_BAR_X, 28, menu.secondaryOutputFill(), menu.secondaryOutputFluidType(), SECONDARY);
        }
        if (menu.machine() == GasChemistryMachine.COAL_GASIFIER) {
            slot(guiGraphics, GASIFIER_INPUT_SLOT_X, 49);
            slot(guiGraphics, GASIFIER_OUTPUT_SLOT_X, 49);
        }
        int progressX = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_X : STANDARD_PROGRESS_X;
        int progressWidth = menu.machine() == GasChemistryMachine.COAL_GASIFIER ? GASIFIER_PROGRESS_WIDTH : STANDARD_PROGRESS_WIDTH;
        guiGraphics.fill(leftPos + progressX, topPos + 54, leftPos + progressX + progressWidth, topPos + 62, DARK);
        guiGraphics.fill(leftPos + progressX + 1, topPos + 55, leftPos + progressX + 1 + Math.round((progressWidth - 2) * menu.progressFill()), topPos + 61,
                menu.instantFlash() ? MachineScreenStyle.INSTANT_FLASH : PROGRESS);
        statusIcon(guiGraphics, progressX, 39);
        renderPurgeButtons(guiGraphics);
        if (menu.machine() == GasChemistryMachine.SYNGAS_COMBUSTOR) {
            renderMixtureGauge(guiGraphics);
        }
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
                            menu.statValue(GasChemistryMenu.flatEnergyGenerationDataIndex()),
                            menu.packGenerationMultiplier()
                    ),
                    stat("rngtech.stat.efficiency", menu.statValue(GasChemistryMenu.efficiencyDataIndex()), MachineStat.EFFICIENCY),
                    stat("rngtech.stat.processing_speed", menu.statValue(GasChemistryMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED)
            };
        }
        return new MachineScreenStyle.StatLine[] {
                stat("rngtech.stat.energy_usage", menu.statValue(GasChemistryMenu.energyUsageDataIndex()), MachineStat.ENERGY_USAGE),
                stat("rngtech.stat.processing_speed", menu.statValue(GasChemistryMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED)
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

    private void bar(GuiGraphics guiGraphics, int x, int y, float fill, Fluid fluid, int color) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 10, topPos + y + 50, DARK);
        int height = Math.round(48 * fill);
        FluidBarRenderer.fill(guiGraphics, fluid, leftPos + x + 1, topPos + y + 49 - height, leftPos + x + 9, topPos + y + 49, color);
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
        if (isOverMixtureGauge(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, mixtureTooltip(), mouseX, mouseY);
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
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, progressX, 39, 12, 12,
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
}
