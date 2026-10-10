package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetalPressScreen extends AbstractContainerScreen<MetalPressMenu> {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int HEAT = 0xFFE0712F;
    private static final int PROGRESS = 0xFF667F51;
    private static final int RISK = 0xFFC14332;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STATUS_WARN = 0xFFAA7A31;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int STAT_ACCENT = 0xFFB86B28;
    private static final int START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF4A5C3B;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_SPACING = 44;
    private static final int ENERGY_BAR_X = 22;
    private static final int HEAT_BAR_X = 36;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int RISK_BAR_X = 86;
    private static final int RISK_BAR_Y = 41;
    private static final int RISK_BAR_WIDTH = 69;
    private static final int RISK_BAR_HEIGHT = 5;
    private static final int PROGRESS_BAR_X = 86;
    private static final int PROGRESS_BAR_Y = 51;
    private static final int PROGRESS_BAR_WIDTH = 69;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    private static final int STATUS_ICON_X = 107;
    private static final int RISK_ICON_X = 123;
    private static final int STATUS_ICON_Y = 63;
    private static final int STATUS_ICON_SIZE = 12;
    private static final int GEAR_HEAT_CORE_X = 29;
    private static final int GEAR_SERVO_X = 77;
    private static final int GEAR_BATTERY_X = 125;
    private static final int GEAR_TOP_ROW_Y = 47;
    private static final int GEAR_MOLD_X = 23;
    private static final int GEAR_MOLD_Y = 75;
    private static final int GEAR_MOLD_SPACING = 44;
    private static final int MOLD_SELECTOR_Y = 98;
    private static final int MOLD_SELECTOR_WIDTH = 12;
    private static final int MOLD_SELECTOR_HEIGHT = 5;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.batch_size",
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.max_temperature",
            "rngtech.stat.warmup_time",
            "rngtech.stat.cooling_rate",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.overheat_tolerance",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            MetalPressMenu.batchSizeDataIndex(),
            MetalPressMenu.processingSpeedDataIndex(),
            MetalPressMenu.energyUsageDataIndex(),
            MetalPressMenu.energyCapacityStatDataIndex(),
            MetalPressMenu.heatTransferDataIndex(),
            MetalPressMenu.maxTemperatureDataIndex(),
            MetalPressMenu.warmupTimeDataIndex(),
            MetalPressMenu.coolingRateDataIndex(),
            MetalPressMenu.temperatureStabilityDataIndex(),
            MetalPressMenu.overheatToleranceDataIndex(),
            MetalPressMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.BATCH_SIZE,
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.HEAT_TRANSFER,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.WARMUP_TIME,
            MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OVERHEAT_TOLERANCE,
            MachineStat.STABILITY
    };
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public MetalPressScreen(MetalPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                MegaPassiveTree.TREE,
                MegaPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.METAL_PRESS.startNodeId()),
                MASTERY_ICON_TEXTURES,
                menu,
                new MasteryScreenSupport.Callbacks<>() {
                    @Override
                    public boolean gearAllowsUnlock(MegaPassiveNode node) {
                        return true;
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
                        HEAT,
                        START_NODE,
                        MASTERY_RING
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
        renderMoldSelectorTooltips(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY) {
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

        if (menu.selectedTab() == MetalPressMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != MetalPressMenu.TAB_CONFIGURATION && menu.selectedTab() != MetalPressMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == MetalPressMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else if (menu.selectedTab() == MetalPressMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(MetalPressMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(MetalPressMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(MetalPressMenu.TAB_CONFIGURATION);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(MetalPressMenu.TAB_REFINEMENT);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 4)) {
                selectTab(MetalPressMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == MetalPressMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == MetalPressMenu.TAB_GEAR) {
                int moldIndex = moldSelectorAt(mouseX, mouseY);
                if (moldIndex >= 0) {
                    return sendButton(MetalPressMenu.BUTTON_SELECT_MOLD_BASE + moldIndex);
                }
            }
        }
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY
                && masterySupport.mouseDragged(mouseX, mouseY, button, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (masterySupport.mouseReleased(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY
                && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) {
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY && masterySupport.charTyped(character)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == MetalPressMenu.TAB_MASTERY && masterySupport.expanded()) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == MetalPressMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == MetalPressMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == MetalPressMenu.TAB_CONFIGURATION);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == MetalPressMenu.TAB_REFINEMENT);
        renderTab(guiGraphics, 4, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == MetalPressMenu.TAB_MASTERY);
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
        if (menu.selectedTab() == MetalPressMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, 55, 41);
            renderSlotFrame(guiGraphics, 169, 51);
            renderSlotFrame(
                    guiGraphics,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_X - 1,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_Y - 1
            );
        } else if (menu.selectedTab() == MetalPressMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, GEAR_HEAT_CORE_X, GEAR_TOP_ROW_Y);
            renderSlotFrame(guiGraphics, GEAR_SERVO_X, GEAR_TOP_ROW_Y);
            renderSlotFrame(guiGraphics, GEAR_BATTERY_X, GEAR_TOP_ROW_Y);
            for (int index = 0; index < MetalPressBlockEntity.MOLD_SLOT_COUNT; index++) {
                renderSlotFrame(guiGraphics, GEAR_MOLD_X + index * GEAR_MOLD_SPACING, GEAR_MOLD_Y);
            }
        } else if (menu.selectedTab() == MetalPressMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != MetalPressMenu.TAB_CONFIGURATION && menu.selectedTab() != MetalPressMenu.TAB_MASTERY) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + RISK_BAR_X, y + RISK_BAR_Y, x + RISK_BAR_X + RISK_BAR_WIDTH, y + RISK_BAR_Y + RISK_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + RISK_BAR_X + 1,
                y + RISK_BAR_Y + 1,
                x + RISK_BAR_X + 1 + Math.round((RISK_BAR_WIDTH - 2) * menu.failureRiskProgress()),
                y + RISK_BAR_Y + RISK_BAR_HEIGHT - 1,
                RISK
        );

        guiGraphics.fill(x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y, x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + PROGRESS_BAR_X + 1,
                y + PROGRESS_BAR_Y + 1,
                x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1,
                PROGRESS
        );
        float ledger = menu.ledgerProgress();
        if (ledger >= 0.0F) {
            // The Batch Ledger fills a thin line along the progress bar's bottom edge.
            int ledgerY = y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1;
            guiGraphics.fill(x + PROGRESS_BAR_X + 1, ledgerY, x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH - 1, ledgerY + 1, 0xFF3A3326);
            guiGraphics.fill(x + PROGRESS_BAR_X + 1, ledgerY, x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * ledger), ledgerY + 1, 0xFFD3A33A);
        }

        guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(BAR_FILL_HEIGHT * menu.energyProgress());
        guiGraphics.fill(x + ENERGY_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - energyHeight, x + ENERGY_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, ENERGY);

        guiGraphics.fill(x + HEAT_BAR_X, y + BAR_Y, x + HEAT_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int heatHeight = Math.round(BAR_FILL_HEIGHT * menu.heatProgress());
        guiGraphics.fill(x + HEAT_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - heatHeight, x + HEAT_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, HEAT);

        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + STATUS_ICON_Y + 3, x + STATUS_ICON_X + 9, y + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, RISK_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + RISK_ICON_X + 3, y + STATUS_ICON_Y + 3, x + RISK_ICON_X + 9, y + STATUS_ICON_Y + 9, riskColor());
    }

    private void renderGear(GuiGraphics guiGraphics) {
        for (int index = 0; index < MetalPressBlockEntity.MOLD_SLOT_COUNT; index++) {
            renderMoldSelector(guiGraphics, index);
        }
    }

    private void renderMoldSelector(GuiGraphics guiGraphics, int moldIndex) {
        int x = leftPos + GEAR_MOLD_X + moldIndex * GEAR_MOLD_SPACING + 3;
        int y = topPos + MOLD_SELECTOR_Y;
        boolean selected = menu.selectedMold() == moldIndex;
        guiGraphics.fill(x, y, x + MOLD_SELECTOR_WIDTH, y + MOLD_SELECTOR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + MOLD_SELECTOR_WIDTH - 1,
                y + MOLD_SELECTOR_HEIGHT - 1,
                selected ? STAT_ACCENT : PANEL_LIGHT
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

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.input"), 52, 28, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.output"), 164, 40, TEXT_MUTED, false);
        drawProcessingTargetLabel(guiGraphics);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 38, 35, 44);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 86, 35, 44);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 134, 35, 44);
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

    private void drawRefinementLabels(GuiGraphics guiGraphics) {
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
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

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != MetalPressMenu.TAB_PROCESSING) {
            return;
        }

        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_BAR_X,
                BAR_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                HEAT_BAR_X,
                BAR_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                heatTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                RISK_BAR_X,
                RISK_BAR_Y,
                RISK_BAR_WIDTH,
                RISK_BAR_HEIGHT,
                riskTooltip()
        );
        renderProgressTooltipIfHovered(guiGraphics, mouseX, mouseY);
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STATUS_ICON_X,
                STATUS_ICON_Y,
                STATUS_ICON_SIZE,
                STATUS_ICON_SIZE,
                Component.translatable("rngtech.press.tooltip.status", statusText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                RISK_ICON_X,
                STATUS_ICON_Y,
                STATUS_ICON_SIZE,
                STATUS_ICON_SIZE,
                riskTooltip()
        );
    }

    private void renderMoldSelectorTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != MetalPressMenu.TAB_GEAR) {
            return;
        }
        int moldIndex = moldSelectorAt(mouseX, mouseY);
        if (moldIndex < 0) {
            return;
        }
        Component tooltip = moldIndex == menu.selectedMold()
                ? Component.translatable("rngtech.press.tooltip.active_mold")
                : Component.translatable("rngtech.press.tooltip.select_mold");
        guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private Component heatTooltip() {
        return Component.translatable(
                "rngtech.press.tooltip.heat",
                menu.heat(),
                menu.minimumTemperature(),
                menu.targetTemperature(),
                menu.safeMaximumTemperature(),
                menu.overheatTemperature()
        );
    }

    private Component riskTooltip() {
        return Component.translatable("rngtech.press.tooltip.failure_risk", menu.failureRisk());
    }

    private List<Component> progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return List.of(
                    Component.translatable("rngtech.press.tooltip.progress.empty"),
                    heatTooltip(),
                    riskTooltip(),
                    Component.translatable("rngtech.press.tooltip.status", statusText())
            );
        }
        int progress = menu.progress();
        int remaining = Math.max(0, ticks - progress);
        float ledger = menu.ledgerProgress();
        return List.of(
                ledger < 0.0F
                        ? Component.translatable("rngtech.press.tooltip.progress", progress, ticks, remaining)
                        : Component.translatable("rngtech.press.tooltip.progress", progress, ticks, remaining).append(" ")
                                .append(Component.translatable("rngtech.press.tooltip.batch_ledger", Math.round(ledger * 100))),
                Component.translatable(
                        "rngtech.press.tooltip.progress.energy",
                        CompactValueText.energyRate(menu.energyPerTick()),
                        CompactValueText.energyAmount(menu.energyPerCraft())
                ),
                heatTooltip(),
                riskTooltip(),
                Component.translatable("rngtech.press.tooltip.status", statusText())
        );
    }

    private void renderProgressTooltipIfHovered(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = leftPos + PROGRESS_BAR_X;
        int top = topPos + PROGRESS_BAR_Y;
        if (mouseX >= left
                && mouseX < left + PROGRESS_BAR_WIDTH
                && mouseY >= top
                && mouseY < top + PROGRESS_BAR_HEIGHT) {
            guiGraphics.renderComponentTooltip(font, progressTooltip(), mouseX, mouseY);
        }
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != MetalPressMenu.TAB_CONFIGURATION) {
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
        return switch (menu.status()) {
            case MetalPressBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.press.status.missing_heat_core");
            case MetalPressBlockEntity.STATUS_MISSING_SERVO -> Component.translatable("rngtech.press.status.missing_servo");
            case MetalPressBlockEntity.STATUS_MISSING_MOLD -> Component.translatable("rngtech.press.status.missing_mold");
            case MetalPressBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.press.status.no_input");
            case MetalPressBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.press.status.invalid_recipe");
            case MetalPressBlockEntity.STATUS_HEAT_LOW -> Component.translatable("rngtech.press.status.heat_low", menu.minimumTemperature());
            case MetalPressBlockEntity.STATUS_HEAT_HIGH -> Component.translatable("rngtech.press.status.heat_high");
            case MetalPressBlockEntity.STATUS_STABILITY_LOW -> Component.translatable("rngtech.press.status.stability_low");
            case MetalPressBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.press.status.output_full");
            case MetalPressBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.press.status.no_power");
            case MetalPressBlockEntity.STATUS_POWER_DROP -> Component.translatable("rngtech.press.status.power_drop");
            case MetalPressBlockEntity.STATUS_WARMING -> Component.translatable("rngtech.press.status.warming", menu.targetTemperature());
            case MetalPressBlockEntity.STATUS_ROUTE_DISABLED -> Component.translatable("rngtech.press.status.route_disabled");
            case MetalPressBlockEntity.STATUS_SWAPPING_MOLD -> Component.translatable("rngtech.press.status.swapping_mold");
            default -> Component.translatable("rngtech.press.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case MetalPressBlockEntity.STATUS_READY -> STATUS_READY;
            case MetalPressBlockEntity.STATUS_NO_POWER,
                    MetalPressBlockEntity.STATUS_NO_INPUT,
                    MetalPressBlockEntity.STATUS_MISSING_HEAT_CORE,
                    MetalPressBlockEntity.STATUS_MISSING_SERVO,
                    MetalPressBlockEntity.STATUS_MISSING_MOLD,
                    MetalPressBlockEntity.STATUS_HEAT_LOW,
                    MetalPressBlockEntity.STATUS_SWAPPING_MOLD,
                    MetalPressBlockEntity.STATUS_WARMING -> STATUS_WARN;
            default -> STATUS_ERROR;
        };
    }

    private int riskColor() {
        if (menu.failureRisk() <= 0) {
            return STATUS_READY;
        }
        return menu.failureRisk() < 50 ? STATUS_WARN : STATUS_ERROR;
    }

    private String statValue(int index) {
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
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
                    isIntegralStat(STAT_DATA_INDICES[index]),
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            ).withStat(STAT_TYPES[index]);
        }
        return MachineScreenStyle.fitStatLines(
                MachineScreenStyle.withAscendancyStats(statLines, menu.ascendancyStats()),
                MachineScreenStyle.maxStatRows(STAT_PANEL_Y, BASE_IMAGE_HEIGHT)
        );
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == MetalPressMenu.energyCapacityStatDataIndex()
                || dataIndex == MetalPressMenu.maxTemperatureDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == MetalPressMenu.energyUsageDataIndex()
                || dataIndex == MetalPressMenu.warmupTimeDataIndex()
                || dataIndex == MetalPressMenu.coolingRateDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == MetalPressMenu.energyCapacityStatDataIndex()
                || dataIndex == MetalPressMenu.maxTemperatureDataIndex()) {
            return false;
        }
        return value > 1.001;
    }

    private void drawProcessingTargetLabel(GuiGraphics guiGraphics) {
        Component target = Component.translatable("rngtech.processing.machine");
        int x = RefinementScreenStyle.PROCESSING_TARGET_SLOT_X + 8 - font.width(target) / 2;
        guiGraphics.drawString(font, target, x, 28, TEXT_MUTED, false);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int width) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, y, width, TEXT_MUTED);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + STATUS_ICON_SIZE, topPos + y + STATUS_ICON_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + STATUS_ICON_SIZE - 1, topPos + y + STATUS_ICON_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + STATUS_ICON_SIZE - 2, topPos + y + STATUS_ICON_SIZE - 2, PANEL_DARK);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }

    private int moldSelectorAt(double mouseX, double mouseY) {
        for (int index = 0; index < MetalPressBlockEntity.MOLD_SLOT_COUNT; index++) {
            int x = leftPos + GEAR_MOLD_X + index * GEAR_MOLD_SPACING + 3;
            int y = topPos + MOLD_SELECTOR_Y;
            if (mouseX >= x
                    && mouseX < x + MOLD_SELECTOR_WIDTH
                    && mouseY >= y
                    && mouseY < y + MOLD_SELECTOR_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    private boolean sendButton(int id) {
        if (minecraft == null
                || minecraft.player == null
                || minecraft.gameMode == null
                || !menu.clickMenuButton(minecraft.player, id)) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        return true;
    }
}
