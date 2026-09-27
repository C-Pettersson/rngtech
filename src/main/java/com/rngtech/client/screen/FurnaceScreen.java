package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.progression.FurnacePassiveNode;
import com.rngtech.rpg.progression.FurnacePassiveTree;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FurnaceScreen extends AbstractContainerScreen<FurnaceMenu> {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int PROGRESS = 0xFFB86B28;
    private static final int FUEL = 0xFFD3942F;
    private static final int HEAT = 0xFFE0712F;
    private static final int FAILURE = 0xFFB45B4A;
    private static final int STAT_ACCENT = 0xFFD18A3C;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_SPACING = 44;
    private static final int STATUS_ICON_X = 114;
    private static final int STATUS_ICON_Y = 28;
    private static final int ICON_SIZE = 12;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int[] PROCESS_INPUT_X = {56, 38, 56, 38};
    private static final int[] PROCESS_INPUT_Y = {42, 42, 60, 60};
    private static final int[] PROCESS_OUTPUT_X = {164, 182, 164, 182};
    private static final int[] PROCESS_OUTPUT_Y = {42, 42, 60, 60};
    private static final int[] GEAR_X = {92, 116, 140, 164};
    private static final int[] GEAR_Y = {66, 66, 66, 66};
    private static final int[] MULTI_PROGRESS_Y = {43, 50, 57, 64};
    private static final String[] FUEL_STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.efficiency",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.max_temperature",
            "rngtech.stat.warmup_time",
            "rngtech.stat.cooling_rate",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.overheat_tolerance",
            "rngtech.stat.input_slots",
            "rngtech.stat.fuel_efficiency"
    };
    private static final int[] FUEL_STAT_DATA_INDICES = {
            FurnaceMenu.processingSpeedDataIndex(),
            FurnaceMenu.efficiencyDataIndex(),
            FurnaceMenu.heatTransferDataIndex(),
            FurnaceMenu.maxTemperatureDataIndex(),
            FurnaceMenu.warmupTimeDataIndex(),
            FurnaceMenu.coolingRateDataIndex(),
            FurnaceMenu.temperatureStabilityDataIndex(),
            FurnaceMenu.overheatToleranceDataIndex(),
            FurnaceMenu.inputSlotsDataIndex(),
            FurnaceMenu.fuelEfficiencyDataIndex()
    };
    private static final MachineStat[] FUEL_STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.EFFICIENCY,
            MachineStat.HEAT_TRANSFER,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.WARMUP_TIME,
            MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OVERHEAT_TOLERANCE,
            MachineStat.INPUT_SLOTS,
            MachineStat.FUEL_EFFICIENCY
    };
    private static final String[] ELECTRIC_STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.max_temperature",
            "rngtech.stat.warmup_time",
            "rngtech.stat.cooling_rate",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.overheat_tolerance",
            "rngtech.stat.input_slots",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity"
    };
    private static final int[] ELECTRIC_STAT_DATA_INDICES = {
            FurnaceMenu.processingSpeedDataIndex(),
            FurnaceMenu.heatTransferDataIndex(),
            FurnaceMenu.maxTemperatureDataIndex(),
            FurnaceMenu.warmupTimeDataIndex(),
            FurnaceMenu.coolingRateDataIndex(),
            FurnaceMenu.temperatureStabilityDataIndex(),
            FurnaceMenu.overheatToleranceDataIndex(),
            FurnaceMenu.inputSlotsDataIndex(),
            FurnaceMenu.energyUsageDataIndex(),
            FurnaceMenu.energyCapacityStatDataIndex()
    };
    private static final MachineStat[] ELECTRIC_STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.HEAT_TRANSFER,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.WARMUP_TIME,
            MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OVERHEAT_TOLERANCE,
            MachineStat.INPUT_SLOTS,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY
    };
    private static final Map<FurnacePassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<FurnacePassiveNode> masterySupport;

    private static Map<FurnacePassiveNode, ResourceLocation> createMasteryIconTextures() {
        EnumMap<FurnacePassiveNode, ResourceLocation> textures = new EnumMap<>(FurnacePassiveNode.class);
        for (FurnacePassiveNode node : FurnacePassiveNode.values()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public FurnaceScreen(FurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                FurnacePassiveTree.TREE,
                FurnacePassiveTree.TREE.nodes(),
                FurnacePassiveNode.STARTER,
                MASTERY_ICON_TEXTURES,
                menu,
                new MasteryScreenSupport.Callbacks<>() {
                    @Override
                    public boolean gearAllowsUnlock(FurnacePassiveNode node) {
                        return true;
                    }

                    @Override
                    public boolean unlock(FurnacePassiveNode node) {
                        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
                            return false;
                        }
                        if (!menu.canUnlockPassiveNode(node) || !menu.clickMenuButton(minecraft.player, node.buttonId())) {
                            return false;
                        }
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, node.buttonId());
                        return true;
                    }

                    @Override
                    public void appendSpecialTooltip(FurnacePassiveNode node, List<Component> tooltip) {
                        if (node == FurnacePassiveNode.QUENCH_PROTOCOL) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.quench_protocol")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                        if (node == FurnacePassiveNode.CLOSED_LOOP_RECUPERATOR) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.closed_loop_recuperator")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                    }

                    @Override
                    public void geometryChanged() {
                        updateImageSizeForSelectedTab();
                    }
                },
                new MasteryScreenSupport.Palette(
                        PANEL_DARK,
                        PANEL_LIGHT,
                        TEXT,
                        STAT_ACCENT,
                        FUEL,
                        0xFF38D857,
                        0xFF6D4A24
                )
        );
    }

    @Override
    protected void init() {
        updateImageSizeForSelectedTab();
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY) {
            masterySupport.renderTooltips(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits());
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);

        if (menu.selectedTab() == FurnaceMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != FurnaceMenu.TAB_CONFIGURATION && menu.selectedTab() != FurnaceMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == FurnaceMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else if (menu.selectedTab() == FurnaceMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(FurnaceMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(FurnaceMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(FurnaceMenu.TAB_CONFIGURATION);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(FurnaceMenu.TAB_REFINEMENT);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 4)) {
                selectTab(FurnaceMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == FurnaceMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY
                    && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY
                && masterySupport.mouseDragged(mouseX, mouseY, button, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (masterySupport.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == FurnaceMenu.TAB_MASTERY && masterySupport.expanded()) {
            imageWidth = masterySupport.imageWidth(BASE_IMAGE_WIDTH, width);
            imageHeight = masterySupport.imageHeight(BASE_IMAGE_HEIGHT, height);
        } else {
            imageWidth = BASE_IMAGE_WIDTH;
            imageHeight = BASE_IMAGE_HEIGHT;
        }
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        if (masterySupport != null) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        Component input = Component.translatable(menu.activeProcessingSlots() > 1 ? "rngtech.processing.inputs" : "rngtech.processing.input");
        guiGraphics.drawString(font, input, 56 - font.width(input) / 2, 28, TEXT_MUTED, false);
        if (!menu.isElectric()) {
            guiGraphics.drawString(font, Component.translatable("rngtech.processing.fuel"), 52, 76, TEXT_MUTED, false);
        }
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.output"), 164, 28, TEXT_MUTED, false);
        drawProcessingTargetLabel(guiGraphics);
    }

    private void drawConfigurationLabels(GuiGraphics guiGraphics) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == FurnaceMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == FurnaceMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == FurnaceMenu.TAB_CONFIGURATION);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == FurnaceMenu.TAB_REFINEMENT);
        renderTab(guiGraphics, 4, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == FurnaceMenu.TAB_MASTERY);
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
        if (menu.selectedTab() == FurnaceMenu.TAB_PROCESSING) {
            for (int slot = 0; slot < menu.activeProcessingSlots(); slot++) {
                renderSlotFrame(guiGraphics, PROCESS_INPUT_X[slot] - 1, PROCESS_INPUT_Y[slot] - 1);
                renderSlotFrame(guiGraphics, PROCESS_OUTPUT_X[slot] - 1, PROCESS_OUTPUT_Y[slot] - 1);
            }
            if (!menu.isElectric()) {
                renderSlotFrame(guiGraphics, 55, 65);
            }
            renderSlotFrame(
                    guiGraphics,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_X - 1,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_Y - 1
            );
        } else if (menu.selectedTab() == FurnaceMenu.TAB_GEAR) {
            for (int slot = 0; slot < menu.activeGearSlots(); slot++) {
                renderSlotFrame(guiGraphics, GEAR_X[slot] - 1, GEAR_Y[slot] - 1);
            }
            if (menu.isElectric()) {
                renderSlotFrame(guiGraphics, 55, 65);
            }
        } else if (menu.selectedTab() == FurnaceMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != FurnaceMenu.TAB_CONFIGURATION && menu.selectedTab() != FurnaceMenu.TAB_MASTERY) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        if (menu.activeProcessingSlots() == 1) {
            guiGraphics.fill(x + 86, y + 51, x + 155, y + 59, 0xFF5F5F5F);
            guiGraphics.fill(x + 87, y + 52, x + 87 + Math.round(67 * menu.processingProgress()), y + 58, PROGRESS);
        } else {
            for (int lane = 0; lane < menu.activeProcessingSlots(); lane++) {
                int barY = MULTI_PROGRESS_Y[lane];
                guiGraphics.fill(x + 86, y + barY, x + 155, y + barY + 5, 0xFF5F5F5F);
                guiGraphics.fill(
                        x + 87,
                        y + barY + 1,
                        x + 87 + Math.round(67 * menu.processingProgress(lane)),
                        y + barY + 4,
                        PROGRESS
                );
            }
        }

        if (menu.isElectric()) {
            renderVerticalBar(guiGraphics, 14, menu.energyProgress(), ENERGY);
            renderVerticalBar(guiGraphics, 26, menu.heatProgress(), HEAT);
            if (menu.hasFailureRecipe()) {
                renderVerticalBar(guiGraphics, 38, menu.failureProgress(), FAILURE);
            }
        } else {
            renderVerticalBar(guiGraphics, 14, menu.heatProgress(), HEAT);
            if (menu.hasFailureRecipe()) {
                renderVerticalBar(guiGraphics, 26, menu.failureProgress(), FAILURE);
            }
            guiGraphics.fill(x + 86, y + 69, x + 155, y + 73, 0xFF5F5F5F);
            guiGraphics.fill(x + 87, y + 70, x + 87 + Math.round(67 * menu.fuelProgress()), y + 72, FUEL);
        }
        renderStatusIcon(guiGraphics);
    }

    private void renderVerticalBar(GuiGraphics guiGraphics, int barX, float progress, int color) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + barX, y + BAR_Y, x + barX + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int fillHeight = Math.round(BAR_FILL_HEIGHT * progress);
        guiGraphics.fill(
                x + barX + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - fillHeight,
                x + barX + BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                color
        );
    }

    private void renderStatusIcon(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(
                leftPos + STATUS_ICON_X + 3,
                topPos + STATUS_ICON_Y + 3,
                leftPos + STATUS_ICON_X + 9,
                topPos + STATUS_ICON_Y + 9,
                statusColor()
        );
    }

    private void renderConfiguration(GuiGraphics guiGraphics) {
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
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawRefinementLabels(GuiGraphics guiGraphics) {
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        Component label = Component.translatable(menu.usesHeatCores() ? "rngtech.gear.heat_core.short" : "rngtech.gear.component");
        int gearGroupCenter = GEAR_X[0] + 9;
        if (menu.activeGearSlots() > 1) {
            gearGroupCenter = (GEAR_X[0] + GEAR_X[menu.activeGearSlots() - 1]) / 2 + 9;
        }
        guiGraphics.drawString(font, label, gearGroupCenter - font.width(label) / 2, 54, TEXT_MUTED, false);
        if (menu.isElectric()) {
            Component batteryCell = Component.translatable("rngtech.gear.battery_cell.short");
            guiGraphics.drawString(font, batteryCell, 65 - font.width(batteryCell) / 2, 54, TEXT_MUTED, false);
        }
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

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != FurnaceMenu.TAB_PROCESSING) {
            return;
        }

        if (menu.isElectric()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    14,
                    28,
                    10,
                    50,
                    exactEnergyText()
            );
        }
        int heatBarX = menu.isElectric() ? 26 : 14;
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                heatBarX,
                BAR_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                heatTooltip()
        );
        if (menu.hasFailureRecipe()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    menu.isElectric() ? 38 : 26,
                    BAR_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    failureTooltip()
            );
        }
        if (menu.activeProcessingSlots() == 1) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    86,
                    51,
                    69,
                    8,
                    progressTooltip(0)
            );
        } else {
            for (int lane = 0; lane < menu.activeProcessingSlots(); lane++) {
                CompactValueText.renderTooltipIfHovered(
                        guiGraphics,
                        font,
                        leftPos,
                        topPos,
                        mouseX,
                        mouseY,
                        86,
                        MULTI_PROGRESS_Y[lane],
                        69,
                        5,
                        progressTooltip(lane)
                );
            }
        }
        if (!menu.isElectric()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    86,
                    69,
                    69,
                    4,
                    fuelTooltip()
            );
        }
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
                Component.translatable("rngtech.furnace.tooltip.status", statusComponent())
        );
    }

    private Component progressTooltip(int lane) {
        int ticks = menu.processingTicks(lane);
        if (menu.activeProcessingSlots() > 1) {
            if (menu.isElectric() && ticks > 0) {
                return Component.translatable(
                        "rngtech.furnace.tooltip.progress.lane_energy",
                        lane + 1,
                        menu.progress(lane),
                        ticks,
                        CompactValueText.energyRate(menu.energyPerTick(lane)),
                        CompactValueText.energyAmount(menu.energyPerCraft(lane)),
                        CompactValueText.energyRate(menu.totalEnergyPerTick())
                );
            }
            return ticks <= 0
                    ? Component.translatable("rngtech.furnace.tooltip.progress.lane.empty", lane + 1)
                    : Component.translatable("rngtech.furnace.tooltip.progress.lane", lane + 1, menu.progress(lane), ticks);
        }
        if (menu.isElectric() && ticks > 0) {
            return Component.translatable(
                    "rngtech.furnace.tooltip.progress.energy",
                    menu.progress(lane),
                    ticks,
                    CompactValueText.energyRate(menu.energyPerTick(lane)),
                    CompactValueText.energyAmount(menu.energyPerCraft(lane))
            );
        }
        return ticks <= 0
                ? Component.translatable("rngtech.furnace.tooltip.progress.empty")
                : Component.translatable("rngtech.furnace.tooltip.progress", menu.progress(lane), ticks);
    }

    private Component fuelTooltip() {
        int totalBurnTime = menu.totalBurnTime();
        return totalBurnTime <= 0
                ? Component.translatable("rngtech.furnace.tooltip.fuel.empty")
                : Component.translatable("rngtech.furnace.tooltip.fuel", menu.burnTime(), totalBurnTime);
    }

    private Component heatTooltip() {
        return Component.translatable(
                "rngtech.furnace.tooltip.heat",
                menu.heat(),
                menu.minimumTemperature(),
                menu.targetTemperature(),
                menu.safeMaximumTemperature(),
                menu.overheatTemperature()
        );
    }

    private Component failureTooltip() {
        Component base = Component.translatable("rngtech.furnace.tooltip.failure", menu.failureStrain());
        return menu.powerSensitiveActive()
                ? base.copy().append(Component.literal(" ")).append(Component.translatable("rngtech.furnace.tooltip.power_drop"))
                : base;
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case FurnaceBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.furnace.status.no_input");
            case FurnaceBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.furnace.status.invalid_recipe");
            case FurnaceBlockEntity.STATUS_HEAT_LOW -> Component.translatable("rngtech.furnace.status.heat_low", menu.minimumTemperature());
            case FurnaceBlockEntity.STATUS_STABILITY_LOW -> Component.translatable("rngtech.furnace.status.stability_low");
            case FurnaceBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.furnace.status.output_full");
            case FurnaceBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.furnace.status.no_power");
            case FurnaceBlockEntity.STATUS_POWER_LIMITED -> Component.translatable("rngtech.furnace.status.power_limited");
            case FurnaceBlockEntity.STATUS_NO_FUEL -> Component.translatable("rngtech.furnace.status.no_fuel");
            case FurnaceBlockEntity.STATUS_WARMING -> Component.translatable("rngtech.furnace.status.warming", menu.targetTemperature());
            case FurnaceBlockEntity.STATUS_POWER_DROP -> Component.translatable("rngtech.furnace.status.power_drop");
            case FurnaceBlockEntity.STATUS_FAILURE_RISK -> Component.translatable("rngtech.furnace.status.failure_risk", menu.failureStrain());
            default -> Component.translatable("rngtech.furnace.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case FurnaceBlockEntity.STATUS_READY -> PROGRESS;
            case FurnaceBlockEntity.STATUS_NO_INPUT -> PANEL_DARK;
            case FurnaceBlockEntity.STATUS_NO_POWER,
                    FurnaceBlockEntity.STATUS_POWER_LIMITED,
                    FurnaceBlockEntity.STATUS_NO_FUEL,
                    FurnaceBlockEntity.STATUS_WARMING,
                    FurnaceBlockEntity.STATUS_POWER_DROP,
                    FurnaceBlockEntity.STATUS_FAILURE_RISK -> FUEL;
            default -> STATUS_ERROR;
        };
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != FurnaceMenu.TAB_CONFIGURATION) {
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
        int dataIndex = statDataIndices()[index];
        double value = menu.statValue(dataIndex);
        return MachineScreenStyle.statValue(statTypes()[index], value);
    }

    private MachineScreenStyle.StatLine[] statLines() {
        String[] labelKeys = statLabelKeys();
        int[] dataIndices = statDataIndices();
        MachineStat[] statTypes = statTypes();
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[labelKeys.length];
        for (int index = 0; index < labelKeys.length; index++) {
            double value = menu.statValue(dataIndices[index]);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(labelKeys[index]),
                    statValue(index),
                    value,
                    isIntegralStat(dataIndices[index]),
                    isEnhancedStat(dataIndices[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            statTypes[index],
                            value
                    )
            );
        }
        return statLines;
    }

    private String[] statLabelKeys() {
        return menu.isElectric() ? ELECTRIC_STAT_LABEL_KEYS : FUEL_STAT_LABEL_KEYS;
    }

    private int[] statDataIndices() {
        return menu.isElectric() ? ELECTRIC_STAT_DATA_INDICES : FUEL_STAT_DATA_INDICES;
    }

    private MachineStat[] statTypes() {
        return menu.isElectric() ? ELECTRIC_STAT_TYPES : FUEL_STAT_TYPES;
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == FurnaceMenu.energyCapacityStatDataIndex()
                || dataIndex == FurnaceMenu.maxTemperatureDataIndex()
                || dataIndex == FurnaceMenu.inputSlotsDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == FurnaceMenu.energyUsageDataIndex()
                || dataIndex == FurnaceMenu.warmupTimeDataIndex()
                || dataIndex == FurnaceMenu.coolingRateDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == FurnaceMenu.energyCapacityStatDataIndex()) {
            return false;
        }
        if (dataIndex == FurnaceMenu.maxTemperatureDataIndex()) {
            double baseTemperature = menu.isElectric()
                    ? MachineStatAccumulator.FURNACE_BASE_MAX_TEMPERATURE
                    : MachineStatAccumulator.PRIMITIVE_FURNACE_MAX_TEMPERATURE;
            return value > baseTemperature;
        }
        return value > 1.001;
    }

    private void drawProcessingTargetLabel(GuiGraphics guiGraphics) {
        Component target = Component.translatable("rngtech.processing.machine");
        int x = RefinementScreenStyle.PROCESSING_TARGET_SLOT_X + 8 - font.width(target) / 2;
        guiGraphics.drawString(font, target, x, 64, TEXT_MUTED, false);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, PANEL_DARK);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
