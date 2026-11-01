package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MelterScreen extends AbstractContainerScreen<MelterMenu> implements MachineTabs.Host {
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
    private static final int INPUT_FLUID = 0xFF5C86C7;
    private static final int OUTPUT_FLUID = 0xFFB9A33F;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STAT_ACCENT = 0xFFB86B28;
    private static final int START_NODE = 0xFF38D857;
    private static final int MASTERY_RING = 0xFF4A5A3A;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_BAR_X = 8;
    private static final int HEAT_BAR_X = 22;
    private static final int INPUT_FLUID_BAR_X = 38;
    private static final int OUTPUT_FLUID_BAR_X = 176;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int FLUID_BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int PROGRESS_X = 104;
    private static final int PROGRESS_Y = 57;
    private static final int PROGRESS_WIDTH = 58;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_X = 104;
    private static final int TRANSFER_X = 120;
    private static final int STATUS_Y = 42;
    private static final int STATUS_SIZE = 12;
    private static final int PROCESSING_LEVEL_ICON_X = 136;
    private static final int PROCESSING_LEVEL_ICON_Y = STATUS_Y;
    private static final int PROCESSING_LEVEL_MAX = 8;
    private static final int INPUT_SLOT_FRAME_X = 57;
    private static final int SECONDARY_INPUT_SLOT_FRAME_X = 81;
    private static final int INPUT_SLOT_TOP_FRAME_Y = BAR_Y;
    private static final int FLUID_CONTAINER_SLOT_FRAME_Y = BAR_Y + BAR_HEIGHT + 2;
    private static final int INPUT_FLUID_CONTAINER_SLOT_FRAME_X = INPUT_FLUID_BAR_X + FLUID_BAR_WIDTH / 2 - 9;
    private static final int OUTPUT_FLUID_CONTAINER_SLOT_FRAME_X = OUTPUT_FLUID_BAR_X + FLUID_BAR_WIDTH / 2 - 9;
    private static final int INPUT_GROUP_CENTER_X = 69;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.batch_size",
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.heat_transfer",
            "rngtech.stat.thermal_conductivity",
            "rngtech.stat.max_temperature",
            "rngtech.stat.fluid_transfer"
    };
    private static final int[] STAT_DATA_INDICES = {
            MelterMenu.batchSizeDataIndex(),
            MelterMenu.processingSpeedDataIndex(),
            MelterMenu.energyUsageDataIndex(),
            MelterMenu.energyCapacityStatDataIndex(),
            MelterMenu.heatTransferDataIndex(),
            MelterMenu.thermalConductivityDataIndex(),
            MelterMenu.maxTemperatureDataIndex(),
            MelterMenu.fluidTransferDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.BATCH_SIZE,
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.HEAT_TRANSFER,
            MachineStat.THERMAL_CONDUCTIVITY,
            MachineStat.MAX_TEMPERATURE,
            MachineStat.FLUID_TRANSFER
    };
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;
    private final MachineTabs tabs;
    private final WorkOrdersTab workOrdersTab;
    private final HeatSlider heatSlider = new HeatSlider(104, 81, 58, true);

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public MelterScreen(MelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(MelterMenu.TAB_PROCESSING, ModBlocks.MELTER.get())
                .gear(MelterMenu.TAB_GEAR)
                .stats(MelterMenu.TAB_CONFIGURATION)
                .refinement(MelterMenu.TAB_REFINEMENT)
                .mastery(MelterMenu.TAB_MASTERY)
                .jewels(MelterMenu.TAB_JEWELS)
                .orders(MelterMenu.TAB_ORDERS, menu::workOrdersShown)
                .build();
        workOrdersTab = new WorkOrdersTab(menu, STAT_ACCENT);
        masterySupport = new MasteryScreenSupport<>(
                MegaPassiveTree.TREE,
                MegaPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.MELTER.startNodeId()),
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

                    @Override
                    public ItemStack socketedJewel(int index) {
                        return menu.jewel(index);
                    }
                },
                new MasteryScreenSupport.Palette(
                        PANEL_DARK,
                        PANEL_LIGHT,
                        TEXT,
                        STAT_ACCENT,
                        OUTPUT_FLUID,
                        START_NODE,
                        MASTERY_RING
                )
        );
    }

    @Override
    public MachineTabs machineTabs() {
        return tabs;
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
        tabs.renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY) {
            masterySupport.renderTooltips(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
        if (menu.selectedTab() == MelterMenu.TAB_ORDERS) {
            List<Component> orderTooltip = workOrdersTab.tooltip(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
            if (!orderTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, orderTooltip, mouseX, mouseY);
            }
        }
        if (menu.selectedTab() == MelterMenu.TAB_JEWELS) {
            List<Component> jewelTooltip = JewelsTab.tooltip(this);
            if (!jewelTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, jewelTooltip, mouseX, mouseY);
            }
        }
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        renderSlotFrames(guiGraphics);

        if (menu.selectedTab() == MelterMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_CONFIGURATION) {
            renderConfiguration(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_MASTERY) {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else if (menu.selectedTab() == MelterMenu.TAB_ORDERS) {
            workOrdersTab.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        } else if (menu.selectedTab() == MelterMenu.TAB_JEWELS) {
            JewelsTab.render(guiGraphics, font, menu, leftPos, topPos, TEXT, TEXT_MUTED, (x, y) -> renderSlotFrame(guiGraphics, x, y));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
        if (menu.selectedTab() != MelterMenu.TAB_CONFIGURATION && menu.selectedTab() != MelterMenu.TAB_MASTERY) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == MelterMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_CONFIGURATION) {
            drawConfigurationLabels(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
        } else if (menu.selectedTab() == MelterMenu.TAB_MASTERY) {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.selectedTab() == MelterMenu.TAB_PROCESSING
                && heatSlider.mouseClicked(mouseX, mouseY, button, leftPos, topPos, menu.containerId)) {
            return true;
        }
        if (button == 0) {
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                selectTab(tab);
                return true;
            }
            if (menu.selectedTab() == MelterMenu.TAB_ORDERS
                    && workOrdersTab.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
                return true;
            }
            if (menu.selectedTab() == MelterMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == MelterMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
        }
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (heatSlider.mouseDragged(mouseX, button, leftPos, menu.containerId)) {
            return true;
        }
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY
                && masterySupport.mouseDragged(mouseX, mouseY, button, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        heatSlider.mouseReleased();
        if (masterySupport.mouseReleased(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) {
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY && masterySupport.charTyped(character)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WorkOrderMenuSupport.BUTTON_SELECT_TAB + menu.selectedTab());
        }
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == MelterMenu.TAB_MASTERY && masterySupport.expanded()) {
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

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() == MelterMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, INPUT_SLOT_FRAME_X, INPUT_SLOT_TOP_FRAME_Y);
            renderSlotFrame(guiGraphics, SECONDARY_INPUT_SLOT_FRAME_X, INPUT_SLOT_TOP_FRAME_Y);
            renderSlotFrame(guiGraphics, INPUT_FLUID_CONTAINER_SLOT_FRAME_X, FLUID_CONTAINER_SLOT_FRAME_Y);
            renderSlotFrame(guiGraphics, OUTPUT_FLUID_CONTAINER_SLOT_FRAME_X, FLUID_CONTAINER_SLOT_FRAME_Y);
        } else if (menu.selectedTab() == MelterMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 19, 47);
            renderSlotFrame(guiGraphics, 67, 47);
            renderSlotFrame(guiGraphics, 115, 47);
            renderSlotFrame(guiGraphics, 163, 47);
            renderSlotFrame(guiGraphics, 211, 47);
        } else if (menu.selectedTab() == MelterMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != MelterMenu.TAB_CONFIGURATION && menu.selectedTab() != MelterMenu.TAB_MASTERY) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        heatSlider.sync(menu.heatSlider());
        heatSlider.render(guiGraphics, font, leftPos, topPos, menu.heatSlider(), heatBands());
        int x = leftPos;
        int y = topPos;

        guiGraphics.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_WIDTH, y + PROGRESS_Y + PROGRESS_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + PROGRESS_X + 1,
                y + PROGRESS_Y + 1,
                x + PROGRESS_X + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.processingProgress()),
                y + PROGRESS_Y + PROGRESS_HEIGHT - 1,
                (menu.instantFlash() ? MachineScreenStyle.INSTANT_FLASH : PROGRESS)
        );

        guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(BAR_FILL_HEIGHT * menu.energyProgress());
        guiGraphics.fill(x + ENERGY_BAR_X + 1, y + BAR_Y + BAR_HEIGHT - 1 - energyHeight, x + ENERGY_BAR_X + BAR_WIDTH - 1, y + BAR_Y + BAR_HEIGHT - 1, ENERGY);

        HeatReadout.renderBar(guiGraphics, leftPos + HEAT_BAR_X, topPos + BAR_Y, BAR_WIDTH, BAR_HEIGHT, BAR_FILL_HEIGHT, heatBands());

        int outputHeight = Math.round(BAR_FILL_HEIGHT * menu.outputFluidProgress());
        guiGraphics.fill(x + OUTPUT_FLUID_BAR_X, y + BAR_Y, x + OUTPUT_FLUID_BAR_X + FLUID_BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        FluidBarRenderer.fill(
                guiGraphics,
                menu.outputFluidType(),
                x + OUTPUT_FLUID_BAR_X + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - outputHeight,
                x + OUTPUT_FLUID_BAR_X + FLUID_BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                OUTPUT_FLUID
        );

        guiGraphics.fill(x + INPUT_FLUID_BAR_X, y + BAR_Y, x + INPUT_FLUID_BAR_X + FLUID_BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int inputHeight = Math.round(BAR_FILL_HEIGHT * menu.inputFluidProgress());
        FluidBarRenderer.fill(
                guiGraphics,
                menu.inputFluidType(),
                x + INPUT_FLUID_BAR_X + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - inputHeight,
                x + INPUT_FLUID_BAR_X + FLUID_BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                INPUT_FLUID
        );

        renderIconBox(guiGraphics, STATUS_X, STATUS_Y);
        MachineScreenStyle.renderStatusGlyph(guiGraphics, leftPos + STATUS_X, topPos + STATUS_Y, statusState());
        renderIconBox(guiGraphics, TRANSFER_X, STATUS_Y);
        renderTransferGlyph(guiGraphics, TRANSFER_X, STATUS_Y);
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
        renderPurgeButtons(guiGraphics);
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
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.inputs"), INPUT_GROUP_CENTER_X, 18);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output"), OUTPUT_FLUID_BAR_X + FLUID_BAR_WIDTH / 2, 18);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.heat_core.short"), 28);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.crush_head.short"), 76);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 124);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 172);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.fluid_pump.short"), 220);
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
        if (menu.selectedTab() != MelterMenu.TAB_PROCESSING) {
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
                processingLevelTooltip()
        );
        if (heatSlider.isOver(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, heatSlider.tooltip(menu.heatSlider(), menu.setpoint()), mouseX, mouseY);
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
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
                heatTooltipLines()
        );
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
                progressTooltipLines()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                INPUT_FLUID_BAR_X,
                BAR_Y,
                FLUID_BAR_WIDTH,
                BAR_HEIGHT,
                inputFluidTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_FLUID_BAR_X,
                BAR_Y,
                FLUID_BAR_WIDTH,
                BAR_HEIGHT,
                outputFluidTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STATUS_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                statusText()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                TRANSFER_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                transferTooltip()
        );
    }

    /** The Melter's melting point is its target; its safe maximum is 15% above. */
    private HeatReadout.Bands heatBands() {
        int melt = menu.minimumTemperature();
        return new HeatReadout.Bands(menu.heat(), melt, melt, Math.round(melt * 1.15F), menu.overheatTemperature(), menu.setpoint(),
                menu.heatReach(), menu.heatMaximum(), menu.wobble());
    }

    private List<Component> heatTooltipLines() {
        return HeatReadout.lines(heatTooltip(), menu.heatSlider(), menu.minimumTemperature(), menu.setpoint(), menu.heatFactor(), menu.wobble(),
                menu.inputEnergy(), menu.heatingEnergy(), menu.holdingEnergy(), true);
    }

    private Component heatTooltip() {
        return Component.translatable("rngtech.melter.tooltip.heat", menu.heat(), menu.minimumTemperature());
    }

    private List<Component> progressTooltipLines() {
        return MachineScreenStyle.withOutputRate(
                progressTooltip(),
                menu.progress(),
                menu.processingTicks(),
                menu.outputPerMinuteTenths(),
                true
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        return ticks <= 0
                ? Component.translatable("rngtech.melter.tooltip.progress.empty")
                : Component.translatable("rngtech.melter.tooltip.progress", menu.progress(), ticks);
    }

    private Component inputFluidTooltip() {
        return FluidMeterTooltips.amount(menu.inputFluidName(), menu.inputFluid(), menu.inputFluidCapacity());
    }

    private Component outputFluidTooltip() {
        return FluidMeterTooltips.amount(menu.outputFluidName(), menu.outputFluid(), menu.outputFluidCapacity(), Component.translatable("rngtech.melter.tooltip.transfer_detail", menu.fluidTransfer()));
    }

    private Component transferTooltip() {
        return menu.fluidTransfer() > 0
                ? Component.translatable("rngtech.melter.tooltip.transfer", menu.fluidTransfer())
                : Component.translatable("rngtech.melter.tooltip.transfer.none");
    }

    private Component processingLevelTooltip() {
        int required = menu.requiredProcessingLevel();
        if (required > 0) {
            return Component.translatable("rngtech.gear.tooltip.processing_level_required", menu.processingLevel(), required);
        }
        return Component.translatable("rngtech.gear.tooltip.processing_level", menu.processingLevel());
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                INPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                menu.inputFluid() > 0
        );
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                OUTPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                menu.outputFluid() > 0
        );
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                INPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                MelterBlockEntity.PURGE_INPUT_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                MelterBlockEntity.PURGE_OUTPUT_TANK
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
                INPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                Component.translatable("rngtech.purge.target.input_tank"),
                menu.inputFluid() > 0 ? menu.inputFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
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
                OUTPUT_FLUID_BAR_X + 3,
                BAR_Y + 2,
                Component.translatable("rngtech.purge.target.output_tank"),
                menu.outputFluid() > 0 ? menu.outputFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.outputFluid(),
                menu.outputFluidCapacity()
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != MelterMenu.TAB_CONFIGURATION) {
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
            case MelterBlockEntity.STATUS_MISSING_HEAT_CORE -> Component.translatable("rngtech.melter.status.missing_heat_core");
            case MelterBlockEntity.STATUS_MISSING_CRUSH_HEAD -> Component.translatable("rngtech.melter.status.missing_crush_head");
            case MelterBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.melter.status.no_input");
            case MelterBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.melter.status.invalid_recipe");
            case MelterBlockEntity.STATUS_HEAT_LOW -> Component.translatable("rngtech.melter.status.heat_low", menu.minimumTemperature());
            case MelterBlockEntity.STATUS_LEVEL_LOW -> Component.translatable("rngtech.melter.status.level_low");
            case MelterBlockEntity.STATUS_OUTPUT_TANK_FULL -> Component.translatable("rngtech.melter.status.output_full");
            case MelterBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.melter.status.no_power");
            case MelterBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.melter.status.no_fluid");
            case MelterBlockEntity.STATUS_FILLING_BATCH -> Component.translatable("rngtech.melter.status.filling_batch");
            case MelterBlockEntity.STATUS_WARMING -> Component.translatable("rngtech.melter.status.warming", menu.minimumTemperature());
            case MelterBlockEntity.STATUS_HEAT_HIGH -> Component.translatable("rngtech.melter.status.heat_high");
            default -> Component.translatable("rngtech.melter.status.ready");
        };
    }

    private MachineScreenStyle.StatusState statusState() {
        return switch (menu.status()) {
            case MelterBlockEntity.STATUS_READY -> MachineScreenStyle.StatusState.RUNNING;
            case MelterBlockEntity.STATUS_NO_POWER,
                    MelterBlockEntity.STATUS_NO_FLUID,
                    MelterBlockEntity.STATUS_NO_INPUT,
                    MelterBlockEntity.STATUS_FILLING_BATCH,
                    MelterBlockEntity.STATUS_WARMING,
                    MelterBlockEntity.STATUS_HEAT_HIGH,
                    MelterBlockEntity.STATUS_MISSING_HEAT_CORE,
                    MelterBlockEntity.STATUS_MISSING_CRUSH_HEAD -> MachineScreenStyle.StatusState.WAITING;
            case MelterBlockEntity.STATUS_INVALID_RECIPE -> MachineScreenStyle.StatusState.ERROR;
            default -> MachineScreenStyle.StatusState.BLOCKED;
        };
    }

    private int transferColor() {
        return menu.fluidTransfer() > 0 ? STATUS_READY : TEXT_MUTED;
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
        return dataIndex == MelterMenu.energyCapacityStatDataIndex()
                || dataIndex == MelterMenu.maxTemperatureDataIndex()
                || dataIndex == MelterMenu.fluidTransferDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == MelterMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == MelterMenu.energyCapacityStatDataIndex()
                || dataIndex == MelterMenu.maxTemperatureDataIndex()
                || dataIndex == MelterMenu.fluidTransferDataIndex()) {
            return false;
        }
        return value > 1.001;
    }

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 44, TEXT_MUTED);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + STATUS_SIZE, topPos + y + STATUS_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + STATUS_SIZE - 1, topPos + y + STATUS_SIZE - 1, PANEL_LIGHT);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + STATUS_SIZE - 2, topPos + y + STATUS_SIZE - 2, PANEL_DARK);
    }

    private void renderTransferGlyph(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        int color = transferColor();
        guiGraphics.fill(left + 3, top + 5, left + 8, top + 7, color);
        guiGraphics.fill(left + 8, top + 4, left + 10, top + 8, color);
        if (menu.fluidTransfer() <= 0) {
            guiGraphics.fill(left + 4, top + 3, left + 9, top + 4, color);
            guiGraphics.fill(left + 3, top + 4, left + 8, top + 5, color);
            guiGraphics.fill(left + 2, top + 5, left + 7, top + 6, color);
            guiGraphics.fill(left + 2, top + 6, left + 6, top + 7, color);
            guiGraphics.fill(left + 2, top + 7, left + 5, top + 8, color);
            guiGraphics.fill(left + 2, top + 8, left + 4, top + 9, color);
        }
    }
}
