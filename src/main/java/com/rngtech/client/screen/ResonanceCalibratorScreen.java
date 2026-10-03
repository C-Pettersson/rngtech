package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
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

public class ResonanceCalibratorScreen extends AbstractContainerScreen<ResonanceCalibratorMenu> {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int PROGRESS = 0xFF667F51;
    private static final int QUALITY = 0xFF4F7A85;
    private static final int STAT_ACCENT = 0xFF4F7A85;
    private static final int MASTERY_HIGHLIGHT = 0xFFD3A33A;
    private static final int MASTERY_START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF2F4A51;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TITLE_RIGHT_PADDING = 8;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_SPACING = 44;
    private static final int STABILITY_BAR_X = 92;
    private static final int STABILITY_BAR_Y = 66;
    private static final int STABILITY_BAR_WIDTH = 76;
    private static final int STABILITY_BAR_HEIGHT = 5;
    private static final int PROGRESS_BAR_X = 92;
    private static final int PROGRESS_BAR_Y = 74;
    private static final int PROGRESS_BAR_WIDTH = 76;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    private static final int STATUS_ICON_X = 124;
    private static final int STATUS_ICON_Y = 86;
    private static final int STATUS_ICON_SIZE = 12;
    private static final int PROCESS_INPUT_FRAME_X = 37;
    private static final int PROCESS_CATALYST_FRAME_X = 79;
    private static final int PROCESS_STABILIZER_FRAME_X = 107;
    private static final int PROCESS_OUTPUT_FRAME_X = 175;
    private static final int PROCESS_SLOT_FRAME_Y = 41;
    private static final int GEAR_BATTERY_X = 29;
    private static final int GEAR_RESONANCE_COIL_X = 69;
    private static final int GEAR_CONTROL_BOARD_X = 109;
    private static final int GEAR_STABILIZER_MATRIX_X = 149;
    private static final int GEAR_TOP_ROW_Y = 47;
    private static final int GEAR_PATTERN_X = 23;
    private static final int GEAR_PATTERN_Y = 75;
    private static final int GEAR_PATTERN_SPACING = 36;
    private static final int PATTERN_SELECTOR_Y = 98;
    private static final int PATTERN_SELECTOR_WIDTH = 12;
    private static final int PATTERN_SELECTOR_HEIGHT = 5;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.stability",
            "rngtech.stat.calibration_quality",
            "rngtech.stat.calibration_precision",
            "rngtech.stat.catalyst_efficiency",
            "rngtech.stat.refinement_potential_bonus"
    };
    private static final int[] STAT_DATA_INDICES = {
            ResonanceCalibratorMenu.processingSpeedDataIndex(),
            ResonanceCalibratorMenu.energyUsageDataIndex(),
            ResonanceCalibratorMenu.energyCapacityStatDataIndex(),
            ResonanceCalibratorMenu.stabilityDataIndex(),
            ResonanceCalibratorMenu.calibrationQualityDataIndex(),
            ResonanceCalibratorMenu.calibrationPrecisionDataIndex(),
            ResonanceCalibratorMenu.catalystEfficiencyDataIndex(),
            ResonanceCalibratorMenu.refinementPotentialBonusDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.STABILITY,
            MachineStat.CALIBRATION_QUALITY,
            MachineStat.CALIBRATION_PRECISION,
            MachineStat.CATALYST_EFFICIENCY,
            MachineStat.REFINEMENT_POTENTIAL_BONUS
    };
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;

    public ResonanceCalibratorScreen(ResonanceCalibratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                MegaPassiveTree.TREE,
                MegaPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.RESONANCE_CALIBRATOR.startNodeId()),
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
                        MASTERY_HIGHLIGHT,
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
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY) {
            masterySupport.renderTooltips(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits());
        renderLabelTooltips(guiGraphics, mouseX, mouseY);
        renderPatternSelectorTooltips(guiGraphics, mouseX, mouseY);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);

        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawTitle(guiGraphics);
        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_CONFIGURATION
                && menu.selectedTab() != ResonanceCalibratorMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        guiGraphics.drawString(
                font,
                clipped(title, imageWidth - titleLabelX - TITLE_RIGHT_PADDING),
                titleLabelX,
                titleLabelY,
                TEXT,
                false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(ResonanceCalibratorMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(ResonanceCalibratorMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(ResonanceCalibratorMenu.TAB_CONFIGURATION);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(ResonanceCalibratorMenu.TAB_REFINEMENT);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 4)) {
                selectTab(ResonanceCalibratorMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR) {
                int patternIndex = patternSelectorAt(mouseX, mouseY);
                if (patternIndex >= 0) {
                    return sendButton(ResonanceCalibratorMenu.BUTTON_SELECT_PATTERN_BASE + patternIndex);
                }
            }
        }
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY
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
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY
                && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) {
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY && masterySupport.charTyped(character)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY && masterySupport.expanded()) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == ResonanceCalibratorMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ResonanceCalibratorMenu.TAB_CONFIGURATION);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == ResonanceCalibratorMenu.TAB_REFINEMENT);
        renderTab(guiGraphics, 4, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == ResonanceCalibratorMenu.TAB_MASTERY);
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
        if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, PROCESS_INPUT_FRAME_X, PROCESS_SLOT_FRAME_Y);
            renderSlotFrame(guiGraphics, PROCESS_CATALYST_FRAME_X, PROCESS_SLOT_FRAME_Y);
            renderSlotFrame(guiGraphics, PROCESS_STABILIZER_FRAME_X, PROCESS_SLOT_FRAME_Y);
            renderSlotFrame(guiGraphics, PROCESS_OUTPUT_FRAME_X, PROCESS_SLOT_FRAME_Y);
            renderSlotFrame(
                    guiGraphics,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_X - 1,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_Y - 1
            );
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, GEAR_BATTERY_X, GEAR_TOP_ROW_Y);
            renderSlotFrame(guiGraphics, GEAR_RESONANCE_COIL_X, GEAR_TOP_ROW_Y);
            renderSlotFrame(guiGraphics, GEAR_CONTROL_BOARD_X, GEAR_TOP_ROW_Y);
            renderSlotFrame(guiGraphics, GEAR_STABILIZER_MATRIX_X, GEAR_TOP_ROW_Y);
            for (int index = 0; index < ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT; index++) {
                renderSlotFrame(guiGraphics, GEAR_PATTERN_X + index * GEAR_PATTERN_SPACING, GEAR_PATTERN_Y);
            }
        } else if (menu.selectedTab() == ResonanceCalibratorMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_CONFIGURATION
                && menu.selectedTab() != ResonanceCalibratorMenu.TAB_MASTERY) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + STABILITY_BAR_X,
                y + STABILITY_BAR_Y,
                x + STABILITY_BAR_X + STABILITY_BAR_WIDTH,
                y + STABILITY_BAR_Y + STABILITY_BAR_HEIGHT,
                0xFF5F5F5F
        );
        int min = Math.round((STABILITY_BAR_WIDTH - 2) * menu.stabilityMin() / 100.0F);
        int max = Math.round((STABILITY_BAR_WIDTH - 2) * menu.stabilityMax() / 100.0F);
        guiGraphics.fill(x + STABILITY_BAR_X + 1, y + STABILITY_BAR_Y + 1, x + STABILITY_BAR_X + 1 + Math.max(1, min), y + STABILITY_BAR_Y + STABILITY_BAR_HEIGHT - 1, 0xFF787878);
        guiGraphics.fill(
                x + STABILITY_BAR_X + 1 + min,
                y + STABILITY_BAR_Y + 1,
                x + STABILITY_BAR_X + 1 + Math.max(min + 1, max),
                y + STABILITY_BAR_Y + STABILITY_BAR_HEIGHT - 1,
                QUALITY
        );

        guiGraphics.fill(
                x + PROGRESS_BAR_X,
                y + PROGRESS_BAR_Y,
                x + PROGRESS_BAR_X + PROGRESS_BAR_WIDTH,
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT,
                0xFF5F5F5F
        );
        guiGraphics.fill(
                x + PROGRESS_BAR_X + 1,
                y + PROGRESS_BAR_Y + 1,
                x + PROGRESS_BAR_X + 1 + Math.round((PROGRESS_BAR_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT - 1,
                PROGRESS
        );

        guiGraphics.fill(
                x + STATUS_ICON_X,
                y + STATUS_ICON_Y,
                x + STATUS_ICON_X + STATUS_ICON_SIZE,
                y + STATUS_ICON_Y + STATUS_ICON_SIZE,
                0xFF5F5F5F
        );
        guiGraphics.fill(
                x + STATUS_ICON_X + 2,
                y + STATUS_ICON_Y + 2,
                x + STATUS_ICON_X + STATUS_ICON_SIZE - 2,
                y + STATUS_ICON_Y + STATUS_ICON_SIZE - 2,
                statusColor()
        );

        guiGraphics.fill(x + 22, y + 28, x + 32, y + 78, 0xFF5F5F5F);
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(x + 23, y + 77 - energyHeight, x + 31, y + 77, ENERGY);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        for (int index = 0; index < ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT; index++) {
            renderPatternSelector(guiGraphics, index);
        }
    }

    private void renderPatternSelector(GuiGraphics guiGraphics, int patternIndex) {
        int x = leftPos + GEAR_PATTERN_X + patternIndex * GEAR_PATTERN_SPACING + 3;
        int y = topPos + PATTERN_SELECTOR_Y;
        boolean selected = menu.selectedPattern() == patternIndex;
        guiGraphics.fill(x, y, x + PATTERN_SELECTOR_WIDTH, y + PATTERN_SELECTOR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + PATTERN_SELECTOR_WIDTH - 1,
                y + PATTERN_SELECTOR_HEIGHT - 1,
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
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.input.tiny"), 47, 28);
        drawCentered(guiGraphics, Component.translatable("rngtech.calibration.catalyst.tiny"), 89, 28);
        drawCentered(guiGraphics, Component.translatable("rngtech.calibration.stabilizer.tiny"), 117, 28);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output.tiny"), 184, 28);
        drawProcessingTargetLabel(guiGraphics);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 39, 35, 38);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.resonance_coil.short"), 79, 35, 38);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.control_board.short"), 119, 35, 38);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.stabilizer_matrix.tiny"), 159, 35, 38);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.calibration.pattern.tiny"), 123, 66, 128);
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
        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_PROCESSING) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                22,
                28,
                10,
                50,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STABILITY_BAR_X,
                STABILITY_BAR_Y,
                STABILITY_BAR_WIDTH,
                STABILITY_BAR_HEIGHT,
                stabilityTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PROGRESS_BAR_X,
                PROGRESS_BAR_Y,
                PROGRESS_BAR_WIDTH,
                PROGRESS_BAR_HEIGHT,
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
                Component.literal("Status: ").append(statusText())
        );
    }

    private void renderLabelTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTabTooltip(guiGraphics, mouseX, mouseY, 0, Component.translatable("rngtech.tab.processing"));
        renderTabTooltip(guiGraphics, mouseX, mouseY, 1, Component.translatable("rngtech.tab.gear"));
        renderTabTooltip(guiGraphics, mouseX, mouseY, 2, Component.translatable("rngtech.tab.stats"));
        renderTabTooltip(guiGraphics, mouseX, mouseY, 3, Component.translatable("rngtech.tab.refinement"));
        renderTabTooltip(guiGraphics, mouseX, mouseY, 4, Component.translatable("rngtech.tab.mastery"));

        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_PROCESSING) {
            return;
        }

        renderCenteredLabelTooltip(guiGraphics, mouseX, mouseY, 47, 28, 24, Component.translatable("rngtech.processing.input"));
        renderCenteredLabelTooltip(guiGraphics, mouseX, mouseY, 89, 28, 24, Component.translatable("rngtech.calibration.catalyst"));
        renderCenteredLabelTooltip(guiGraphics, mouseX, mouseY, 117, 28, 24, Component.translatable("rngtech.calibration.stabilizer"));
        renderCenteredLabelTooltip(guiGraphics, mouseX, mouseY, 184, 28, 24, Component.translatable("rngtech.processing.output"));
        renderCenteredLabelTooltip(
                guiGraphics,
                mouseX,
                mouseY,
                RefinementScreenStyle.PROCESSING_TARGET_SLOT_X + 8,
                28,
                32,
                Component.translatable("rngtech.processing.machine")
        );
    }

    private void renderPatternSelectorTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_GEAR) {
            return;
        }
        int patternIndex = patternSelectorAt(mouseX, mouseY);
        if (patternIndex < 0) {
            return;
        }
        Component tooltip = patternIndex == menu.selectedPattern()
                ? Component.translatable("rngtech.calibrator.tooltip.active_pattern")
                : Component.translatable("rngtech.calibrator.tooltip.select_pattern");
        guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private void renderTabTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int index, Component tooltip) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderCenteredLabelTooltip(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            int centerX,
            int y,
            int width,
            Component tooltip
    ) {
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                centerX - width / 2,
                y,
                width,
                font.lineHeight,
                tooltip
        );
    }

    private Component stabilityTooltip() {
        CalibrationFamily family = menu.family();
        Component familyName = family == null
                ? Component.translatable("rngtech.calibration.no_family")
                : Component.translatable(family.translationKey());
        return Component.translatable("rngtech.calibration.stability_range", familyName, menu.stabilityMin(), menu.stabilityMax());
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        Component progress = ticks <= 0
                ? Component.literal("Progress: -- / --")
                : Component.literal("Progress: " + menu.progress() + " / " + ticks);
        return menu.streakFloor() <= 0 ? progress
                : progress.copy().append(" ").append(Component.translatable("rngtech.calibration.tooltip.streak", menu.streakFloor()));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != ResonanceCalibratorMenu.TAB_CONFIGURATION) {
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
            case ResonanceCalibratorBlockEntity.STATUS_MISSING_RESONANCE_COIL ->
                    Component.translatable("rngtech.calibrator.status.missing_resonance_coil");
            case ResonanceCalibratorBlockEntity.STATUS_MISSING_CONTROL_BOARD ->
                    Component.translatable("rngtech.calibrator.status.missing_control_board");
            case ResonanceCalibratorBlockEntity.STATUS_MISSING_PATTERN ->
                    Component.translatable("rngtech.calibrator.status.missing_pattern");
            case ResonanceCalibratorBlockEntity.STATUS_MISSING_CATALYST ->
                    Component.translatable("rngtech.calibrator.status.missing_catalyst");
            case ResonanceCalibratorBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.calibrator.status.no_input");
            case ResonanceCalibratorBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.calibrator.status.invalid_recipe");
            case ResonanceCalibratorBlockEntity.STATUS_INSUFFICIENT_STAGE ->
                    Component.translatable("rngtech.calibrator.status.insufficient_stage");
            case ResonanceCalibratorBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.calibrator.status.output_full");
            case ResonanceCalibratorBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.calibrator.status.no_power");
            default -> Component.translatable("rngtech.calibrator.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case ResonanceCalibratorBlockEntity.STATUS_READY -> PROGRESS;
            case ResonanceCalibratorBlockEntity.STATUS_NO_POWER -> 0xFFD3A33A;
            case ResonanceCalibratorBlockEntity.STATUS_NO_INPUT,
                    ResonanceCalibratorBlockEntity.STATUS_MISSING_PATTERN,
                    ResonanceCalibratorBlockEntity.STATUS_MISSING_CATALYST,
                    ResonanceCalibratorBlockEntity.STATUS_MISSING_RESONANCE_COIL,
                    ResonanceCalibratorBlockEntity.STATUS_MISSING_CONTROL_BOARD -> PANEL_DARK;
            default -> 0xFFB45B4A;
        };
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
            );
        }
        return MachineScreenStyle.fitStatLines(
                MachineScreenStyle.withAscendancyStats(MachineScreenStyle.withoutInactiveModifierStats(STAT_TYPES, statLines), menu.ascendancyStats()),
                MachineScreenStyle.maxStatRows(STAT_PANEL_Y, BASE_IMAGE_HEIGHT)
        );
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == ResonanceCalibratorMenu.energyCapacityStatDataIndex()
                || dataIndex == ResonanceCalibratorMenu.refinementPotentialBonusDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == ResonanceCalibratorMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == ResonanceCalibratorMenu.energyCapacityStatDataIndex()) {
            return false;
        }
        return value > 1.001;
    }

    private void drawProcessingTargetLabel(GuiGraphics guiGraphics) {
        Component target = Component.translatable("rngtech.processing.machine.tiny");
        int x = RefinementScreenStyle.PROCESSING_TARGET_SLOT_X + 8 - font.width(target) / 2;
        guiGraphics.drawString(font, target, x, 28, TEXT_MUTED, false);
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX, int y, int width) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, y, width, TEXT_MUTED);
    }

    private Component clipped(Component component, int width) {
        String text = component.getString();
        if (font.width(text) <= width) {
            return component;
        }

        String suffix = "...";
        int availableWidth = width - font.width(suffix);
        if (availableWidth <= 0) {
            return Component.empty();
        }
        while (!text.isEmpty() && font.width(text) > availableWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + suffix);
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

    private int patternSelectorAt(double mouseX, double mouseY) {
        for (int index = 0; index < ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT; index++) {
            int x = leftPos + GEAR_PATTERN_X + index * GEAR_PATTERN_SPACING + 3;
            int y = topPos + PATTERN_SELECTOR_Y;
            if (mouseX >= x
                    && mouseX < x + PATTERN_SELECTOR_WIDTH
                    && mouseY >= y
                    && mouseY < y + PATTERN_SELECTOR_HEIGHT) {
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
