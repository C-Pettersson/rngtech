package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.CrusherPassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int PROGRESS = 0xFF6F7F35;
    private static final int OUTPUT_BONUS = 0xFFD3A33A;
    private static final int STAT_ACCENT = 0xFF7F9ED7;
    private static final int STATUS_ERROR = 0xFFB45B4A;
    private static final int START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF53591D;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TAB_WIDTH = 42;
    private static final int TAB_SPACING = 44;
    private static final int STATUS_ICON_X = 96;
    private static final int CELL_STATUS_ICON_X = 112;
    private static final int STATUS_ICON_Y = 28;
    private static final int ICON_SIZE = 12;
    private static final int OUTPUT_BONUS_BAR_WIDTH = 67;
    private static final int PROCESSING_LEVEL_ICON_X = 184;
    private static final int PROCESSING_LEVEL_ICON_Y = 78;
    private static final int PROCESSING_LEVEL_MAX = 8;
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.input_slots",
            "rngtech.stat.crush_hardness",
            "rngtech.stat.batch_size",
            "rngtech.stat.output_amount",
            "rngtech.stat.crusher_salvage_chance",
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.output_guard_grace",
            "rngtech.stat.no_battery_output_retention",
            "rngtech.stat.high_hardness_energy_mitigation",
            "rngtech.stat.crusher_input_filter"
    };
    private static final int[] STAT_DATA_INDICES = {
            CrusherMenu.inputSlotsDataIndex(),
            CrusherMenu.processingLevelDataIndex(),
            CrusherMenu.batchSizeDataIndex(),
            CrusherMenu.outputAmountDataIndex(),
            CrusherMenu.crusherSalvageChanceDataIndex(),
            CrusherMenu.processingSpeedDataIndex(),
            CrusherMenu.energyUsageDataIndex(),
            CrusherMenu.energyCapacityStatDataIndex(),
            CrusherMenu.outputGuardGraceDataIndex(),
            CrusherMenu.noBatteryOutputRetentionDataIndex(),
            CrusherMenu.highHardnessEnergyMitigationDataIndex(),
            CrusherMenu.crusherInputFilterDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.INPUT_SLOTS,
            MachineStat.PROCESSING_LEVEL,
            MachineStat.BATCH_SIZE,
            MachineStat.OUTPUT_AMOUNT,
            MachineStat.CRUSHER_SALVAGE_CHANCE,
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.OUTPUT_GUARD_GRACE,
            MachineStat.NO_BATTERY_OUTPUT_RETENTION,
            MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
            MachineStat.CRUSHER_INPUT_FILTER
    };
    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new java.util.HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                CrusherPassiveTree.TREE,
                CrusherPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.CRUSHER.startNodeId()),
                MASTERY_ICON_TEXTURES,
                menu,
                new MasteryScreenSupport.Callbacks<>() {
                    @Override
                    public boolean gearAllowsUnlock(MegaPassiveNode node) {
                        return masteryNodeGearAllowsUnlock(node);
                    }

                    @Override
                    public void appendSpecialTooltip(MegaPassiveNode node, List<Component> tooltip) {
                        if (node.blocksBatteryCell()) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.blocks_battery").withStyle(ChatFormatting.GOLD));
                        }
                        if (node.requiresMatchingCrushHeadStage()) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.matching_head").withStyle(ChatFormatting.GOLD));
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
                        OUTPUT_BONUS,
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
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY) {
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

        if (menu.selectedTab() == CrusherMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != CrusherMenu.TAB_CONFIGURATION && menu.selectedTab() != CrusherMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == CrusherMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else if (menu.selectedTab() == CrusherMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(CrusherMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(CrusherMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(CrusherMenu.TAB_CONFIGURATION);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                selectTab(CrusherMenu.TAB_REFINEMENT);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 4)) {
                selectTab(CrusherMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == CrusherMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
        }
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY
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
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY && masterySupport.expanded()) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == CrusherMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == CrusherMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == CrusherMenu.TAB_CONFIGURATION);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == CrusherMenu.TAB_REFINEMENT);
        renderTab(guiGraphics, 4, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == CrusherMenu.TAB_MASTERY);
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
        if (menu.selectedTab() == CrusherMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, 55, 41);
            renderSlotFrame(guiGraphics, 169, 53);
            renderSlotFrame(
                    guiGraphics,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_X - 1,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_Y - 1
            );
        } else if (menu.selectedTab() == CrusherMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 43, 47);
            renderSlotFrame(guiGraphics, 79, 47);
        } else if (menu.selectedTab() == CrusherMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == CrusherMenu.TAB_CONFIGURATION || menu.selectedTab() == CrusherMenu.TAB_MASTERY) {
            return;
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + 86, y + 43, x + 155, y + 48, 0xFF5F5F5F);
        guiGraphics.fill(
                x + 87,
                y + 44,
                x + 87 + Math.round(OUTPUT_BONUS_BAR_WIDTH * menu.outputBonusProgress()),
                y + 47,
                OUTPUT_BONUS
        );
        if (menu.outputBonusPayoutsNextCraft() > 0) {
            guiGraphics.fill(x + 153, y + 44, x + 154, y + 47, 0xFFFFE2A2);
        }

        guiGraphics.fill(x + 86, y + 51, x + 155, y + 59, 0xFF5F5F5F);
        guiGraphics.fill(x + 87, y + 52, x + 87 + Math.round(67 * menu.processingProgress()), y + 58, PROGRESS);

        guiGraphics.fill(x + 22, y + 28, x + 32, y + 78, 0xFF5F5F5F);
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(x + 23, y + 77 - energyHeight, x + 31, y + 77, ENERGY);

        renderStatusIcons(guiGraphics);
    }

    private void renderStatusIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(
                leftPos + STATUS_ICON_X + 3,
                topPos + STATUS_ICON_Y + 3,
                leftPos + STATUS_ICON_X + 9,
                topPos + STATUS_ICON_Y + 9,
                statusColor()
        );

        renderIconBox(guiGraphics, CELL_STATUS_ICON_X, STATUS_ICON_Y);
        int cellColor = menu.batterySlotBlocked() ? STATUS_ERROR : menu.hasBatteryCell() ? STAT_ACCENT : OUTPUT_BONUS;
        guiGraphics.fill(
                leftPos + CELL_STATUS_ICON_X + 3,
                topPos + STATUS_ICON_Y + 3,
                leftPos + CELL_STATUS_ICON_X + 9,
                topPos + STATUS_ICON_Y + 9,
                cellColor
        );
        if (!menu.hasBatteryCell()) {
            int left = leftPos + CELL_STATUS_ICON_X;
            int top = topPos + STATUS_ICON_Y;
            guiGraphics.fill(left + 5, top + 3, left + 7, top + 7, 0xFF302414);
            guiGraphics.fill(left + 5, top + 8, left + 7, top + 10, 0xFF302414);
        }
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
        MachineScreenStyle.renderProcessingLevelSquare(
                guiGraphics,
                leftPos,
                topPos,
                PROCESSING_LEVEL_ICON_X,
                PROCESSING_LEVEL_ICON_Y,
                menu.processingLevel(),
                PROCESSING_LEVEL_MAX,
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

    private void drawGearLabels(GuiGraphics guiGraphics) {
        Component crushHead = Component.translatable("rngtech.gear.crush_head.short");
        Component batteryCell = Component.translatable("rngtech.gear.battery_cell.short");
        guiGraphics.drawString(font, crushHead, 53 - font.width(crushHead) / 2, 35, TEXT_MUTED, false);
        guiGraphics.drawString(font, batteryCell, 89 - font.width(batteryCell) / 2, 35, TEXT_MUTED, false);
    }

    private void drawRefinementLabels(GuiGraphics guiGraphics) {
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == CrusherMenu.TAB_GEAR) {
            MachineScreenStyle.renderStatusSquareTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    PROCESSING_LEVEL_ICON_X,
                    PROCESSING_LEVEL_ICON_Y,
                    processingLevelTooltip()
            );
            return;
        }
        if (menu.selectedTab() != CrusherMenu.TAB_PROCESSING) {
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
                exactEnergyText()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                86,
                43,
                69,
                5,
                outputBonusTooltip()
        );
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
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable("rngtech.crusher.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                CELL_STATUS_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                cellStatusTooltip()
        );
    }

    private Component progressTooltip() {
        if (menu.jamTicks() > 0) {
            return Component.translatable("rngtech.crusher.tooltip.jammed", menu.jamTicks());
        }
        int ticks = menu.processingTicks();
        int jobs = Math.max(1, menu.activeJobs());
        int perJobEnergy = Math.max(1, (int) Math.ceil(menu.energyPerTick() / (double) jobs));
        String underLevelText = underLevelTooltipText();
        return ticks <= 0
                ? Component.literal("Progress: -- / --")
                : Component.literal(
                        "Progress: "
                                + menu.progress()
                                + " / "
                                + ticks
                                + ", "
                                + CompactValueText.energyRate(perJobEnergy)
                                + " per job"
                                + (jobs > 1
                                        ? ", " + CompactValueText.energyRate(menu.energyPerTick()) + " total"
                                        : "")
                                + ", "
                                + CompactValueText.energyAmount(menu.energyPerCraft())
                                + " per craft"
                                + underLevelText
                );
    }

    private String underLevelTooltipText() {
        if (menu.hardnessDeficit() <= 0) {
            return "";
        }
        return ", hardness "
                + menu.processingLevel()
                + " / "
                + menu.requiredProcessingLevel()
                + ", "
                + formatMultiplier(menu.underLevelPenaltyMultiplier())
                + " time/FE, "
                + "bonuses off, "
                + formatPerThousandPercent(menu.jamChancePerThousand())
                + " jam";
    }

    private Component outputBonusTooltip() {
        int progress = menu.outputBonusProgressRaw();
        int increment = menu.outputBonusIncrementRaw();
        int scale = menu.outputBonusScale();
        String banked = "Bonus bank: " + formatBonusPercent(progress, scale);
        if (increment <= 0) {
            return Component.literal(banked);
        }

        int payouts = menu.outputBonusPayoutsNextCraft();
        if (payouts > 0) {
            String items = payouts == 1 ? " item" : " items";
            return Component.literal(
                    banked
                            + "; next craft +"
                            + payouts
                            + items
                            + ", then "
                            + formatBonusPercent(menu.outputBonusProgressAfterNextCraftRaw(), scale)
            );
        }
        return Component.literal(banked + "; next craft adds " + formatBonusPercent(increment, scale));
    }

    private String formatBonusPercent(int value, int scale) {
        if (scale <= 0) {
            return "--";
        }
        double percent = value * 100.0 / scale;
        String pattern = percent == Math.rint(percent) ? "%.0f%%" : "%.1f%%";
        return String.format(Locale.ROOT, pattern, percent);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case CrusherBlockEntity.STATUS_MISSING_CRUSH_HEAD -> Component.translatable("rngtech.crusher.status.missing_crush_head");
            case CrusherBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.crusher.status.no_input");
            case CrusherBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.crusher.status.invalid_recipe");
            case CrusherBlockEntity.STATUS_BLOCKED_LEVEL -> Component.translatable("rngtech.crusher.status.blocked_level");
            case CrusherBlockEntity.STATUS_INSUFFICIENT_INPUT -> Component.translatable("rngtech.crusher.status.insufficient_input");
            case CrusherBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.crusher.status.output_full");
            case CrusherBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.crusher.status.no_power");
            case CrusherBlockEntity.STATUS_JAMMED -> Component.translatable("rngtech.crusher.status.jammed");
            case CrusherBlockEntity.STATUS_UNDER_LEVEL_PENALTY -> Component.translatable("rngtech.crusher.status.under_level_penalty");
            default -> Component.translatable("rngtech.crusher.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case CrusherBlockEntity.STATUS_READY -> PROGRESS;
            case CrusherBlockEntity.STATUS_NO_INPUT, CrusherBlockEntity.STATUS_MISSING_CRUSH_HEAD -> PANEL_DARK;
            case CrusherBlockEntity.STATUS_NO_POWER,
                    CrusherBlockEntity.STATUS_JAMMED,
                    CrusherBlockEntity.STATUS_UNDER_LEVEL_PENALTY -> OUTPUT_BONUS;
            default -> STATUS_ERROR;
        };
    }

    private Component cellStatusTooltip() {
        if (menu.batterySlotBlocked()) {
            return Component.translatable("rngtech.crusher.tooltip.battery_slot_blocked");
        }
        if (menu.hasBatteryCell()) {
            return Component.translatable("rngtech.crusher.tooltip.cell", Component.translatable("rngtech.crusher.status.battery_cell"));
        }
        return Component.translatable("rngtech.crusher.tooltip.no_battery_cell", outputAmountMultiplier());
    }

    private Component processingLevelTooltip() {
        int required = menu.requiredProcessingLevel();
        if (required > 0) {
            return Component.translatable("rngtech.gear.tooltip.processing_level_required", menu.processingLevel(), required);
        }
        return Component.translatable("rngtech.gear.tooltip.processing_level", menu.processingLevel());
    }

    private boolean masteryNodeGearAllowsUnlock(MegaPassiveNode node) {
        if (node.blocksBatteryCell() && CrusherBlockEntity.isBatteryCell(menu.getSlot(CrusherBlockEntity.SLOT_FUEL).getItem())) {
            return false;
        }
        if (!node.requiresMatchingCrushHeadStage()) {
            return true;
        }
        return menu.getSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD).getItem().isEmpty()
                || menu.getSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD).getItem().getItem() instanceof CrushHeadItem head
                        && head.material().stage() == menu.chassisStage();
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != CrusherMenu.TAB_CONFIGURATION) {
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
        int dataIndex = STAT_DATA_INDICES[index];
        double value = menu.statValue(dataIndex);
        return MachineScreenStyle.statValue(STAT_TYPES[index], value);
    }

    private String outputAmountMultiplier() {
        return formatMultiplier(menu.statValue(CrusherMenu.outputAmountDataIndex()));
    }

    private String formatMultiplier(double value) {
        return String.format(Locale.ROOT, "%.2fx", value);
    }

    private String formatPerThousandPercent(int value) {
        double percent = value / 10.0D;
        String pattern = percent == Math.rint(percent) ? "%.0f%%" : "%.1f%%";
        return String.format(Locale.ROOT, pattern, percent);
    }

    private MachineScreenStyle.StatLine[] statLines() {
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[STAT_LABEL_KEYS.length];
        for (int index = 0; index < STAT_LABEL_KEYS.length; index++) {
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            Component tooltip = statTooltip(index, value);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    statValue(index),
                    value,
                    isIntegralStat(STAT_DATA_INDICES[index]),
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    tooltip
            );
        }
        return MachineScreenStyle.fitStatLines(
                MachineScreenStyle.withAscendancyStats(MachineScreenStyle.withoutInactiveModifierStats(STAT_TYPES, statLines), menu.ascendancyStats()),
                MachineScreenStyle.maxStatRows(STAT_PANEL_Y, BASE_IMAGE_HEIGHT)
        );
    }

    private Component statTooltip(int index, double value) {
        if (STAT_DATA_INDICES[index] == CrusherMenu.outputAmountDataIndex() && !menu.hasBatteryCell()) {
            return Component.translatable("rngtech.crusher.tooltip.output_amount_no_battery", formatMultiplier(value));
        }
        return MachineScreenStyle.statLayerTooltip(
                menu.getSlot(menu.refinementTargetSlot()).getItem(),
                STAT_TYPES[index],
                value
        );
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == CrusherMenu.inputSlotsDataIndex()
                || dataIndex == CrusherMenu.energyCapacityStatDataIndex()
                || dataIndex == CrusherMenu.processingLevelDataIndex()
                || dataIndex == CrusherMenu.batchSizeDataIndex()
                || dataIndex == CrusherMenu.outputGuardGraceDataIndex()
                || dataIndex == CrusherMenu.noBatteryOutputRetentionDataIndex()
                || dataIndex == CrusherMenu.highHardnessEnergyMitigationDataIndex()
                || dataIndex == CrusherMenu.crusherInputFilterDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == CrusherMenu.outputGuardGraceDataIndex()
                || dataIndex == CrusherMenu.noBatteryOutputRetentionDataIndex()
                || dataIndex == CrusherMenu.highHardnessEnergyMitigationDataIndex()
                || dataIndex == CrusherMenu.crusherInputFilterDataIndex()
                || dataIndex == CrusherMenu.crusherSalvageChanceDataIndex()) {
            return value > 0.001;
        }
        if (dataIndex == CrusherMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == CrusherMenu.processingLevelDataIndex()) {
            return value > 0.001;
        }
        if (dataIndex == CrusherMenu.batchSizeDataIndex()) {
            return value > 1.001;
        }
        return value > 1.001;
    }

    private void drawProcessingTargetLabel(GuiGraphics guiGraphics) {
        Component target = Component.translatable("rngtech.processing.machine");
        int x = RefinementScreenStyle.PROCESSING_TARGET_SLOT_X + 8 - font.width(target) / 2;
        guiGraphics.drawString(font, target, x, 28, TEXT_MUTED, false);
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

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) { return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == CrusherMenu.TAB_MASTERY && masterySupport.charTyped(character)) { return true; }
        return super.charTyped(character, modifiers);
    }

}
