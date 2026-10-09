package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.menu.AlloyFurnaceMenu;
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
import java.util.Map;

public class AlloyFurnaceScreen extends AbstractContainerScreen<AlloyFurnaceMenu> {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int HEAT = 0xFFE0712F;
    private static final int FAILURE = 0xFFB45B4A;
    private static final int PROGRESS = 0xFFB87832;
    private static final int BLEND_LEDGER = 0xFFD3A33A;
    private static final int FLUX_LEDGER = 0xFF8FB4D8;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STATUS_WARN = 0xFFAA7A31;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int STAT_ACCENT = 0xFFB87832;
    private static final int MASTERY_START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF6D4A24;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_SPACING = 44;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_BAR_X = 20;
    private static final int HEAT_BAR_X = 34;
    private static final int FAILURE_BAR_X = 188;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int PROGRESS_X = 98;
    private static final int PROGRESS_Y = 51;
    private static final int PROGRESS_WIDTH = 86;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_ICON_X = 127;
    private static final int HEAT_ICON_X = 143;
    private static final int STATUS_ICON_Y = 63;
    private static final int STATUS_ICON_SIZE = 12;
    private static final int OUTPUT_SLOT_X = 204;
    private static final int OUTPUT_SLOT_Y = 42;
    private static final int[] INPUT_X = {50, 70, 50, 70};
    private static final int[] INPUT_Y = {29, 29, 49, 49};
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.max_temperature",
            "rngtech.stat.warmup_time",
            "rngtech.stat.cooling_rate",
            "rngtech.stat.temperature_stability",
            "rngtech.stat.overheat_tolerance",
            "rngtech.stat.stability",
            "rngtech.stat.input_slots"
    };
    private static final int[] STAT_DATA_INDICES = {
            AlloyFurnaceMenu.processingSpeedDataIndex(),
            AlloyFurnaceMenu.energyUsageDataIndex(),
            AlloyFurnaceMenu.energyCapacityStatDataIndex(),
            AlloyFurnaceMenu.heatTransferDataIndex(),
            AlloyFurnaceMenu.maxTemperatureDataIndex(),
            AlloyFurnaceMenu.warmupTimeDataIndex(),
            AlloyFurnaceMenu.coolingRateDataIndex(),
            AlloyFurnaceMenu.temperatureStabilityDataIndex(),
            AlloyFurnaceMenu.overheatToleranceDataIndex(),
            AlloyFurnaceMenu.stabilityDataIndex(),
            AlloyFurnaceMenu.inputSlotsDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.HEAT_TRANSFER,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.WARMUP_TIME,
            MachineStat.COOLING_RATE,
            MachineStat.TEMPERATURE_STABILITY,
            MachineStat.OVERHEAT_TOLERANCE,
            MachineStat.STABILITY,
            MachineStat.INPUT_SLOTS
    };
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;

    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                MegaPassiveTree.TREE,
                MegaPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.ALLOY_FURNACE.startNodeId()),
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
                        MASTERY_START_NODE,
                        MASTERY_RING
                )
        );
    }

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY) {
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY) {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS && menu.selectedTab() != AlloyFurnaceMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(AlloyFurnaceMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(AlloyFurnaceMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(AlloyFurnaceMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(AlloyFurnaceMenu.TAB_REFINEMENT);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 4)) {
                selectTab(AlloyFurnaceMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
        }
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) {
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY && masterySupport.charTyped(character)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY && masterySupport.expanded()) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == AlloyFurnaceMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT);
        renderTab(guiGraphics, 4, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == AlloyFurnaceMenu.TAB_MASTERY);
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
        if (menu.selectedTab() == AlloyFurnaceMenu.TAB_PROCESSING) {
            for (int slot = 0; slot < menu.activeInputSlots(); slot++) {
                renderSlotFrame(guiGraphics, INPUT_X[slot] - 1, INPUT_Y[slot] - 1);
            }
            renderSlotFrame(guiGraphics, OUTPUT_SLOT_X - 1, OUTPUT_SLOT_Y - 1);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 43, 47);
            renderSlotFrame(guiGraphics, 91, 47);
            renderSlotFrame(guiGraphics, 139, 47);
            renderSlotFrame(guiGraphics, 187, 47);
        } else if (menu.selectedTab() == AlloyFurnaceMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS && menu.selectedTab() != AlloyFurnaceMenu.TAB_MASTERY) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_WIDTH, y + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + PROGRESS_X + 1,
                y + PROGRESS_Y + 1,
                x + PROGRESS_X + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_Y + PROGRESS_HEIGHT - 1,
                PROGRESS
        );
        float ledger = menu.ledgerProgress();
        if (ledger >= 0.0F) {
            // The flux or blend ledger fills a thin line along the progress bar's bottom edge.
            int ledgerY = y + PROGRESS_Y + PROGRESS_HEIGHT - 1;
            guiGraphics.fill(x + PROGRESS_X + 1, ledgerY, x + PROGRESS_X + PROGRESS_WIDTH - 1, ledgerY + 1, 0xFF3A3326);
            guiGraphics.fill(x + PROGRESS_X + 1, ledgerY, x + PROGRESS_X + 1 + Math.round((PROGRESS_WIDTH - 2) * ledger), ledgerY + 1,
                    menu.ledgerIsBlend() ? BLEND_LEDGER : FLUX_LEDGER);
        }

        guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(BAR_FILL_HEIGHT * menu.energyProgress());
        guiGraphics.fill(x + ENERGY_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - energyHeight, x + ENERGY_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, ENERGY);

        guiGraphics.fill(x + HEAT_BAR_X, y + BAR_Y, x + HEAT_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int heatHeight = Math.round(BAR_FILL_HEIGHT * menu.heatProgress());
        guiGraphics.fill(x + HEAT_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - heatHeight, x + HEAT_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, HEAT);

        if (menu.hasFailureRecipe()) {
            guiGraphics.fill(x + FAILURE_BAR_X, y + BAR_Y, x + FAILURE_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
            int failureHeight = Math.round(BAR_FILL_HEIGHT * menu.failureProgress());
            guiGraphics.fill(
                    x + FAILURE_BAR_X + 1,
                    y + BAR_Y + BAR_HEIGHT - 1 - failureHeight,
                    x + FAILURE_BAR_X + BAR_WIDTH - 1,
                    y + BAR_Y + BAR_HEIGHT - 1,
                    FAILURE
            );
        }

        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + STATUS_ICON_Y + 3, x + STATUS_ICON_X + 9, y + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, HEAT_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + HEAT_ICON_X + 3, y + STATUS_ICON_Y + 3, x + HEAT_ICON_X + 9, y + STATUS_ICON_Y + 9, heatGateColor());
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
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.inputs"), 68, 78);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output"), OUTPUT_SLOT_X + 8, 28);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 52);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.crucible.short"), 100);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 148);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 196);
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

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_PROCESSING) {
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
        if (menu.hasFailureRecipe()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    FAILURE_BAR_X,
                    BAR_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    failureTooltip()
            );
        }
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
                Component.translatable("rngtech.alloy_furnace.tooltip.status", statusText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                HEAT_ICON_X,
                STATUS_ICON_Y,
                STATUS_ICON_SIZE,
                STATUS_ICON_SIZE,
                heatTooltip()
        );
    }

    private Component heatTooltip() {
        return Component.translatable(
                "rngtech.alloy_furnace.tooltip.heat",
                menu.heat(),
                menu.minimumTemperature(),
                menu.targetTemperature(),
                menu.safeMaximumTemperature(),
                menu.overheatTemperature()
        );
    }

    private Component failureTooltip() {
        Component base = Component.translatable("rngtech.alloy_furnace.tooltip.failure", menu.failureStrain());
        return menu.powerSensitiveActive()
                ? base.copy().append(Component.literal(" ")).append(Component.translatable("rngtech.alloy_furnace.tooltip.power_drop"))
                : base;
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        Component progress = ticks <= 0
                ? Component.translatable("rngtech.alloy_furnace.tooltip.progress.empty")
                : Component.translatable("rngtech.alloy_furnace.tooltip.progress", menu.progress(), ticks, CompactValueText.energyRate(menu.energyPerTick()));
        float ledger = menu.ledgerProgress();
        return ledger < 0.0F ? progress : progress.copy().append(" ").append(Component.translatable(
                menu.ledgerIsBlend() ? "rngtech.alloy_furnace.tooltip.blend_ledger" : "rngtech.alloy_furnace.tooltip.flux", Math.round(ledger * 100)));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlloyFurnaceMenu.TAB_STATS) {
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
            case AlloyFurnaceBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.alloy_furnace.status.missing_heat_core");
            case AlloyFurnaceBlockEntity.STATUS_MISSING_CRUCIBLE -> Component.translatable("rngtech.alloy_furnace.status.missing_crucible");
            case AlloyFurnaceBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.alloy_furnace.status.no_input");
            case AlloyFurnaceBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.alloy_furnace.status.invalid_recipe");
            case AlloyFurnaceBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.alloy_furnace.status.blocked_stage");
            case AlloyFurnaceBlockEntity.STATUS_ROUTE_DISABLED -> Component.translatable("rngtech.alloy_furnace.status.route_disabled");
            case AlloyFurnaceBlockEntity.STATUS_HEAT_LOW -> Component.translatable("rngtech.alloy_furnace.status.heat_low", menu.minimumTemperature());
            case AlloyFurnaceBlockEntity.STATUS_STABILITY_LOW -> Component.translatable("rngtech.alloy_furnace.status.stability_low");
            case AlloyFurnaceBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.alloy_furnace.status.output_full");
            case AlloyFurnaceBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.alloy_furnace.status.no_power");
            case AlloyFurnaceBlockEntity.STATUS_WARMING -> Component.translatable("rngtech.alloy_furnace.status.warming", menu.targetTemperature());
            case AlloyFurnaceBlockEntity.STATUS_POWER_DROP -> Component.translatable("rngtech.alloy_furnace.status.power_drop");
            case AlloyFurnaceBlockEntity.STATUS_FAILURE_RISK -> Component.translatable("rngtech.alloy_furnace.status.failure_risk", menu.failureStrain());
            default -> Component.translatable("rngtech.alloy_furnace.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case AlloyFurnaceBlockEntity.STATUS_READY -> STATUS_READY;
            case AlloyFurnaceBlockEntity.STATUS_NO_POWER,
                    AlloyFurnaceBlockEntity.STATUS_NO_INPUT,
                    AlloyFurnaceBlockEntity.STATUS_MISSING_HEAT_CORE,
                    AlloyFurnaceBlockEntity.STATUS_MISSING_CRUCIBLE,
                    AlloyFurnaceBlockEntity.STATUS_HEAT_LOW,
                    AlloyFurnaceBlockEntity.STATUS_WARMING,
                    AlloyFurnaceBlockEntity.STATUS_POWER_DROP,
                    AlloyFurnaceBlockEntity.STATUS_FAILURE_RISK -> STATUS_WARN;
            default -> STATUS_ERROR;
        };
    }

    private int heatGateColor() {
        return menu.targetTemperature() <= 0
                        || menu.heat() >= menu.targetTemperature()
                ? STATUS_READY
                : STATUS_WARN;
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
            );
        }
        return MachineScreenStyle.fitStatLines(
                MachineScreenStyle.withAscendancyStats(statLines, menu.ascendancyStats()),
                MachineScreenStyle.maxStatRows(STAT_PANEL_Y, BASE_IMAGE_HEIGHT)
        );
    }

    private String statValue(int index) {
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == AlloyFurnaceMenu.energyCapacityStatDataIndex()
                || dataIndex == AlloyFurnaceMenu.maxTemperatureDataIndex()
                || dataIndex == AlloyFurnaceMenu.inputSlotsDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == AlloyFurnaceMenu.energyUsageDataIndex()
                || dataIndex == AlloyFurnaceMenu.warmupTimeDataIndex()
                || dataIndex == AlloyFurnaceMenu.coolingRateDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == AlloyFurnaceMenu.energyCapacityStatDataIndex()
                || dataIndex == AlloyFurnaceMenu.maxTemperatureDataIndex()
                || dataIndex == AlloyFurnaceMenu.inputSlotsDataIndex()) {
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

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + STATUS_ICON_SIZE, topPos + y + STATUS_ICON_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + STATUS_ICON_SIZE - 1, topPos + y + STATUS_ICON_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + STATUS_ICON_SIZE - 2, topPos + y + STATUS_ICON_SIZE - 2, PANEL_DARK);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 48, TEXT_MUTED);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
