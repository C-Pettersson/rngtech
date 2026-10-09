package com.rngtech.client.screen;

import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.menu.CompressorTankMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CompressorTankScreen extends AbstractContainerScreen<CompressorTankMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int LOOSE_FLUID = 0xFF4C83BE;
    private static final int COMPRESSED_FLUID = 0xFF3FB8C8;
    private static final int EQUIVALENT_FLUID = 0xFF236C98;
    private static final int STATUS_READY = 0xFF5F8A45;
    private static final int STATUS_WARN = 0xFFAA7A31;
    private static final int STAT_ACCENT = 0xFF2F7F90;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_BAR_X = 22;
    private static final int LOOSE_FLUID_BAR_X = 88;
    private static final int COMPRESSED_FLUID_BAR_X = 104;
    private static final int EQUIVALENT_FLUID_BAR_X = 120;
    private static final int BAR_Y = 28;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_FILL_HEIGHT = 48;
    private static final int STATUS_X = 146;
    private static final int STATUS_Y = 51;
    private static final int STATUS_SIZE = 12;
    private static final int COMPRESSION_INDICATOR_X = 138;
    private static final int COMPRESSION_INDICATOR_Y = 38;
    private static final int COMPRESSION_INDICATOR_WIDTH = 28;
    private static final int COMPRESSION_INDICATOR_HEIGHT = 7;
    private static final int PLAIN_TANK_X = 76;
    private static final int PLAIN_TANK_Y = BAR_Y;
    private static final int PLAIN_TANK_WIDTH = 72;
    private static final int PLAIN_TANK_HEIGHT = BAR_HEIGHT;
    private static final int PLAIN_TANK_FILL_WIDTH = PLAIN_TANK_WIDTH - 2;
    private static final int PLAIN_TANK_FILL_HEIGHT = PLAIN_TANK_HEIGHT - 2;
    private static final int FLUID_CONTAINER_SLOT_FRAME_Y = BAR_Y + BAR_HEIGHT + 2;
    private static final int FLUID_INPUT_CONTAINER_SLOT_FRAME_X = LOOSE_FLUID_BAR_X + BAR_WIDTH / 2 - 9;
    private static final int FLUID_OUTPUT_CONTAINER_SLOT_FRAME_X = EQUIVALENT_FLUID_BAR_X + BAR_WIDTH / 2 - 9;
    private static final String[] COMPRESSOR_STAT_LABEL_KEYS = {
            "rngtech.stat.fluid_capacity",
            "rngtech.stat.compression_ratio",
            "rngtech.stat.fluid_transfer",
            "rngtech.stat.processing_speed",
            "rngtech.stat.energy_usage",
            "rngtech.stat.energy_capacity"
    };
    private static final int[] COMPRESSOR_STAT_DATA_INDICES = {
            CompressorTankMenu.fluidCapacityStatDataIndex(),
            CompressorTankMenu.compressionRatioDataIndex(),
            CompressorTankMenu.fluidTransferDataIndex(),
            CompressorTankMenu.processingSpeedDataIndex(),
            CompressorTankMenu.energyUsageDataIndex(),
            CompressorTankMenu.energyCapacityStatDataIndex()
    };
    private static final MachineStat[] COMPRESSOR_STAT_TYPES = {
            MachineStat.FLUID_CAPACITY,
            MachineStat.COMPRESSION_RATIO,
            MachineStat.FLUID_TRANSFER,
            MachineStat.PROCESSING_SPEED,
            MachineStat.ENERGY_USAGE,
            MachineStat.ENERGY_CAPACITY
    };
    private static final String[] PLAIN_STAT_LABEL_KEYS = {"rngtech.stat.fluid_capacity"};
    private static final int[] PLAIN_STAT_DATA_INDICES = {CompressorTankMenu.fluidCapacityStatDataIndex()};
    private static final MachineStat[] PLAIN_STAT_TYPES = {MachineStat.FLUID_CAPACITY};

    public CompressorTankScreen(CompressorTankMenu menu, Inventory playerInventory, Component title) {
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
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);

        if (menu.selectedTab() == CompressorTankMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != CompressorTankMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }

        if (menu.selectedTab() == CompressorTankMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_STATS) {
            drawStatLabels(guiGraphics);
        } else {
            drawRefinementLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(CompressorTankMenu.TAB_PROCESSING);
                return true;
            }
            if (menu.supportsCompression() && isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(CompressorTankMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(CompressorTankMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(CompressorTankMenu.TAB_REFINEMENT);
                return true;
            }
            if (menu.selectedTab() == CompressorTankMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CompressorTankMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"), menu.selectedTab() == CompressorTankMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == CompressorTankMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == CompressorTankMenu.TAB_STATS);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"), menu.selectedTab() == CompressorTankMenu.TAB_REFINEMENT);
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
        if (menu.selectedTab() == CompressorTankMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, FLUID_INPUT_CONTAINER_SLOT_FRAME_X, FLUID_CONTAINER_SLOT_FRAME_Y);
            renderSlotFrame(guiGraphics, FLUID_OUTPUT_CONTAINER_SLOT_FRAME_X, FLUID_CONTAINER_SLOT_FRAME_Y);
            renderSlotFrame(
                    guiGraphics,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_X - 1,
                    RefinementScreenStyle.PROCESSING_TARGET_SLOT_Y - 1
            );
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_GEAR && menu.supportsCompression()) {
            renderSlotFrame(guiGraphics, 31, 47);
            renderSlotFrame(guiGraphics, 67, 47);
            renderSlotFrame(guiGraphics, 103, 47);
            renderSlotFrame(guiGraphics, 139, 47);
            renderSlotFrame(guiGraphics, 175, 47);
        } else if (menu.selectedTab() == CompressorTankMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != CompressorTankMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;

        if (!menu.supportsCompression()) {
            renderPlainTank(guiGraphics);
            return;
        }

        if (menu.supportsCompression()) {
            guiGraphics.fill(x + ENERGY_BAR_X, y + BAR_Y, x + ENERGY_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
            int energyHeight = Math.round(BAR_FILL_HEIGHT * menu.energyProgress());
            guiGraphics.fill(
                    x + ENERGY_BAR_X + 1,
                    y + BAR_Y + BAR_HEIGHT - 1 - energyHeight,
                    x + ENERGY_BAR_X + BAR_WIDTH - 1,
                    y + BAR_Y + BAR_HEIGHT - 1,
                    ENERGY
            );
        }

        guiGraphics.fill(x + LOOSE_FLUID_BAR_X, y + BAR_Y, x + LOOSE_FLUID_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
        int looseHeight = Math.round(BAR_FILL_HEIGHT * menu.looseFluidProgress());
        FluidBarRenderer.fill(
                guiGraphics,
                menu.looseFluidType(),
                x + LOOSE_FLUID_BAR_X + 1,
                y + BAR_Y + BAR_HEIGHT - 1 - looseHeight,
                x + LOOSE_FLUID_BAR_X + BAR_WIDTH - 1,
                y + BAR_Y + BAR_HEIGHT - 1,
                LOOSE_FLUID
        );

        if (menu.supportsCompression()) {
            guiGraphics.fill(x + COMPRESSED_FLUID_BAR_X, y + BAR_Y, x + COMPRESSED_FLUID_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
            int compressedHeight = Math.round(BAR_FILL_HEIGHT * menu.compressedFluidProgress());
            FluidBarRenderer.fill(
                    guiGraphics,
                    menu.compressedFluidType(),
                    x + COMPRESSED_FLUID_BAR_X + 1,
                    y + BAR_Y + BAR_HEIGHT - 1 - compressedHeight,
                    x + COMPRESSED_FLUID_BAR_X + BAR_WIDTH - 1,
                    y + BAR_Y + BAR_HEIGHT - 1,
                    COMPRESSED_FLUID
            );

            guiGraphics.fill(x + EQUIVALENT_FLUID_BAR_X, y + BAR_Y, x + EQUIVALENT_FLUID_BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, 0xFF5F5F5F);
            int equivalentHeight = Math.round(BAR_FILL_HEIGHT * compressedEquivalentProgress());
            FluidBarRenderer.fill(
                    guiGraphics,
                    menu.compressedFluidType(),
                    x + EQUIVALENT_FLUID_BAR_X + 1,
                    y + BAR_Y + BAR_HEIGHT - 1 - equivalentHeight,
                    x + EQUIVALENT_FLUID_BAR_X + BAR_WIDTH - 1,
                    y + BAR_Y + BAR_HEIGHT - 1,
                    EQUIVALENT_FLUID
            );

            guiGraphics.fill(
                    x + COMPRESSION_INDICATOR_X,
                    y + COMPRESSION_INDICATOR_Y,
                    x + COMPRESSION_INDICATOR_X + COMPRESSION_INDICATOR_WIDTH,
                    y + COMPRESSION_INDICATOR_Y + COMPRESSION_INDICATOR_HEIGHT,
                    0xFF5F5F5F
            );
            guiGraphics.fill(
                    x + COMPRESSION_INDICATOR_X + 1,
                    y + COMPRESSION_INDICATOR_Y + 1,
                    x + COMPRESSION_INDICATOR_X + 1 + Math.round((COMPRESSION_INDICATOR_WIDTH - 2) * compressedEquivalentProgress()),
                    y + COMPRESSION_INDICATOR_Y + COMPRESSION_INDICATOR_HEIGHT - 1,
                    COMPRESSED_FLUID
            );
        }
        renderPurgeButtons(guiGraphics);

        int statusColor = menu.status() == CompressorTankBlockEntity.STATUS_READY
                        || menu.status() == CompressorTankBlockEntity.STATUS_PLAIN_TANK
                ? STATUS_READY
                : STATUS_WARN;
        guiGraphics.fill(x + STATUS_X, y + STATUS_Y, x + STATUS_X + STATUS_SIZE, y + STATUS_Y + STATUS_SIZE, 0xFF5F5F5F);
        guiGraphics.fill(x + STATUS_X + 2, y + STATUS_Y + 2, x + STATUS_X + STATUS_SIZE - 2, y + STATUS_Y + STATUS_SIZE - 2, statusColor);
    }

    private void renderPlainTank(GuiGraphics guiGraphics) {
        int x = leftPos + PLAIN_TANK_X;
        int y = topPos + PLAIN_TANK_Y;
        guiGraphics.fill(x, y, x + PLAIN_TANK_WIDTH, y + PLAIN_TANK_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(x + 1, y + 1, x + PLAIN_TANK_WIDTH - 1, y + PLAIN_TANK_HEIGHT - 1, 0xFF2F4050);
        int fillHeight = Math.round(PLAIN_TANK_FILL_HEIGHT * menu.looseFluidProgress());
        FluidBarRenderer.fill(
                guiGraphics,
                menu.looseFluidType(),
                x + 1,
                y + PLAIN_TANK_HEIGHT - 1 - fillHeight,
                x + 1 + PLAIN_TANK_FILL_WIDTH,
                y + PLAIN_TANK_HEIGHT - 1,
                LOOSE_FLUID
        );
        guiGraphics.fill(x + 3, y + 3, x + PLAIN_TANK_WIDTH - 3, y + 5, 0x559AC0E8);
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                PLAIN_TANK_X + PLAIN_TANK_WIDTH / 2 - FluidPurgeButton.SIZE / 2,
                PLAIN_TANK_Y + 2,
                menu.looseFluid() > 0
        );
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
        if (!menu.supportsCompression()) {
            drawCentered(guiGraphics, Component.translatable("rngtech.processing.tank"), PLAIN_TANK_X + PLAIN_TANK_WIDTH / 2, 20);
            drawProcessingTargetLabel(guiGraphics);
            return;
        }
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.input"), LOOSE_FLUID_BAR_X + BAR_WIDTH / 2, 18);
        drawCentered(guiGraphics, Component.translatable("rngtech.processing.output"), EQUIVALENT_FLUID_BAR_X + BAR_WIDTH / 2, 18);
        drawProcessingTargetLabel(guiGraphics);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        if (!menu.supportsCompression()) {
            return;
        }
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 41);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 77);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 113);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 149);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.servo.short"), 185);
    }

    private void drawStatLabels(GuiGraphics guiGraphics) {
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

    private float compressedEquivalentProgress() {
        int capacity = menu.compressedEquivalentCapacity();
        return capacity <= 0 ? 0.0F : Math.min(1.0F, menu.compressedEquivalent() / (float) capacity);
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
        if (menu.selectedTab() != CompressorTankMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }

        if (menu.supportsCompression()) {
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
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                menu.supportsCompression() ? LOOSE_FLUID_BAR_X : PLAIN_TANK_X,
                menu.supportsCompression() ? BAR_Y : PLAIN_TANK_Y,
                menu.supportsCompression() ? BAR_WIDTH : PLAIN_TANK_WIDTH,
                menu.supportsCompression() ? BAR_HEIGHT : PLAIN_TANK_HEIGHT,
                FluidMeterTooltips.amount(menu.looseFluidName(), menu.looseFluid(), menu.looseFluidCapacity())
        );
        if (menu.supportsCompression()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    COMPRESSED_FLUID_BAR_X,
                    BAR_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    compressedTooltip()
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    EQUIVALENT_FLUID_BAR_X,
                    BAR_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    equivalentTooltip()
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    COMPRESSION_INDICATOR_X,
                    COMPRESSION_INDICATOR_Y,
                    COMPRESSION_INDICATOR_WIDTH,
                    COMPRESSION_INDICATOR_HEIGHT,
                    statusText()
            );
        }
        if (menu.supportsCompression()) {
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
        }
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, LOOSE_FLUID_BAR_X, BAR_Y + 2, menu.looseFluid() > 0);
        if (menu.supportsCompression()) {
            FluidPurgeButton.render(
                    guiGraphics,
                    leftPos,
                    topPos,
                    COMPRESSED_FLUID_BAR_X,
                    BAR_Y + 2,
                    menu.compressedEquivalent() > 0
            );
        }
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        int looseX = menu.supportsCompression()
                ? LOOSE_FLUID_BAR_X
                : PLAIN_TANK_X + PLAIN_TANK_WIDTH / 2 - FluidPurgeButton.SIZE / 2;
        int looseY = menu.supportsCompression() ? BAR_Y + 2 : PLAIN_TANK_Y + 2;
        if (FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                looseX,
                looseY,
                CompressorTankBlockEntity.PURGE_LOOSE_TANK
        )) {
            return true;
        }
        return menu.supportsCompression()
                && FluidPurgeButton.handleClick(
                        menu,
                        leftPos,
                        topPos,
                        mouseX,
                        mouseY,
                        COMPRESSED_FLUID_BAR_X,
                        BAR_Y + 2,
                        CompressorTankBlockEntity.PURGE_COMPRESSED_TANK
                );
    }

    private boolean renderPurgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int looseX = menu.supportsCompression()
                ? LOOSE_FLUID_BAR_X
                : PLAIN_TANK_X + PLAIN_TANK_WIDTH / 2 - FluidPurgeButton.SIZE / 2;
        int looseY = menu.supportsCompression() ? BAR_Y + 2 : PLAIN_TANK_Y + 2;
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                looseX,
                looseY,
                Component.translatable("rngtech.purge.target.loose_tank"),
                menu.looseFluid() > 0 ? menu.looseFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.looseFluid(),
                menu.looseFluidCapacity()
        )) {
            return true;
        }
        if (menu.supportsCompression()) {
            return FluidPurgeButton.renderTooltip(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    COMPRESSED_FLUID_BAR_X,
                    BAR_Y + 2,
                    Component.translatable("rngtech.purge.target.compressed_tank"),
                    menu.compressedEquivalent() > 0 ? menu.compressedFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                    menu.compressedEquivalent(),
                    menu.compressedEquivalentCapacity()
            );
        }
        return false;
    }

    private Component compressedTooltip() {
        return FluidMeterTooltips.amount(menu.compressedFluidName(), menu.compressedPhysical(), menu.compressedPhysicalCapacity(), "physical compressed");
    }

    private Component equivalentTooltip() {
        return FluidMeterTooltips.amount(
                menu.compressedFluidName(),
                menu.compressedEquivalent(),
                menu.compressedEquivalentCapacity(),
                "equivalent, " + menu.compressionRate() + " mB/t, " + menu.compressFePerBucket() + " FE/B in, "
                        + menu.decompressFePerBucket() + " FE/B out"
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != CompressorTankMenu.TAB_STATS) {
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
            case CompressorTankBlockEntity.STATUS_PLAIN_TANK -> Component.translatable("rngtech.compressor_tank.status.plain_tank");
            case CompressorTankBlockEntity.STATUS_NO_FLUID -> Component.translatable("rngtech.compressor_tank.status.no_fluid");
            case CompressorTankBlockEntity.STATUS_COMPRESSED_FULL -> Component.translatable("rngtech.compressor_tank.status.compressed_full");
            case CompressorTankBlockEntity.STATUS_MISSING_BATTERY_CELL -> Component.translatable("rngtech.compressor_tank.status.missing_battery_cell");
            case CompressorTankBlockEntity.STATUS_MISSING_SERVO -> Component.translatable("rngtech.compressor_tank.status.missing_servo");
            case CompressorTankBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.compressor_tank.status.no_power");
            default -> Component.translatable("rngtech.compressor_tank.status.ready");
        };
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
        return menu.supportsCompression() ? COMPRESSOR_STAT_LABEL_KEYS : PLAIN_STAT_LABEL_KEYS;
    }

    private int[] statDataIndices() {
        return menu.supportsCompression() ? COMPRESSOR_STAT_DATA_INDICES : PLAIN_STAT_DATA_INDICES;
    }

    private MachineStat[] statTypes() {
        return menu.supportsCompression() ? COMPRESSOR_STAT_TYPES : PLAIN_STAT_TYPES;
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == CompressorTankMenu.fluidCapacityStatDataIndex()
                || dataIndex == CompressorTankMenu.compressionRatioDataIndex()
                || dataIndex == CompressorTankMenu.fluidTransferDataIndex()
                || dataIndex == CompressorTankMenu.energyCapacityStatDataIndex()
                || dataIndex == CompressorTankMenu.energyTransferDataIndex();
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        if (dataIndex == CompressorTankMenu.energyUsageDataIndex()) {
            return value < 0.999;
        }
        if (dataIndex == CompressorTankMenu.fluidCapacityStatDataIndex()
                || dataIndex == CompressorTankMenu.compressionRatioDataIndex()
                || dataIndex == CompressorTankMenu.energyCapacityStatDataIndex()
                || dataIndex == CompressorTankMenu.energyTransferDataIndex()
                || dataIndex == CompressorTankMenu.fluidTransferDataIndex()) {
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

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 34, TEXT_MUTED);
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
