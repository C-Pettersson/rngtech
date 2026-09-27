package com.rngtech.client.screen;

import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;
import com.rngtech.content.menu.SilicaGelDehumidifierMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SilicaGelDehumidifierScreen extends AbstractContainerScreen<SilicaGelDehumidifierMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int WATER = 0xFF2E82D8;
    private static final int SILICA = 0xFFD7E3E6;
    private static final int SATURATED = 0xFF5EA3C8;
    private static final int READY = 0xFF7DA665;
    private static final int BLOCKED = 0xFFB45B4A;
    private static final int BAR_BACKING = 0xFF5F5F5F;
    private static final int LANE_INPUT_X = 40;
    private static final int LANE_OUTPUT_X = 150;
    private static final int LANE_Y = 28;
    private static final int LANE_SPACING = 24;
    private static final int PROGRESS_X = 72;
    private static final int PROGRESS_WIDTH = 58;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_X = 134;
    private static final int STATUS_SIZE = 12;
    private static final int WATER_GAUGE_X = 190;
    private static final int WATER_GAUGE_Y = 24;
    private static final int GAUGE_WIDTH = 28;
    private static final int GAUGE_HEIGHT = 68;
    private static final int STRUCTURE_X = 174;
    private static final int STRUCTURE_Y = 82;

    public SilicaGelDehumidifierScreen(SilicaGelDehumidifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 232;
        imageHeight = 198;
        inventoryLabelX = 35;
        inventoryLabelY = 102;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderSlotFrames(guiGraphics);
        for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
            renderLane(guiGraphics, lane);
        }
        renderGauge(guiGraphics, menu.waterProgress());
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, WATER_GAUGE_X + 9, WATER_GAUGE_Y + 2, menu.waterAmount() > 0);
        renderStatusSquare(guiGraphics, STRUCTURE_X, STRUCTURE_Y, menu.structureValid() ? READY : BLOCKED);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X + 9,
                WATER_GAUGE_Y + 2,
                SilicaGelDehumidifierBlockEntity.PURGE_WATER_TANK
        )) {
            return true;
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

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
            int y = LANE_Y + lane * LANE_SPACING;
            renderSlotFrame(guiGraphics, LANE_INPUT_X, y);
            renderSlotFrame(guiGraphics, LANE_OUTPUT_X, y);
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderLane(GuiGraphics guiGraphics, int lane) {
        int y = LANE_Y + lane * LANE_SPACING + 5;
        int x = leftPos + PROGRESS_X;
        int top = topPos + y;
        guiGraphics.fill(x, top, x + PROGRESS_WIDTH, top + PROGRESS_HEIGHT, BAR_BACKING);
        guiGraphics.fill(
                x + 1,
                top + 1,
                x + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.laneProgress(lane)),
                top + PROGRESS_HEIGHT - 1,
                lane == 0 ? SILICA : lane == 1 ? SATURATED : WATER
        );
        renderStatusSquare(guiGraphics, STATUS_X, y - 2, statusColor(menu.laneStatus(lane)));
    }

    private void renderGauge(GuiGraphics guiGraphics, float fill) {
        int x = leftPos + WATER_GAUGE_X;
        int y = topPos + WATER_GAUGE_Y;
        guiGraphics.fill(x, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, BAR_BACKING);
        int filledHeight = Math.round((GAUGE_HEIGHT - 2) * fill);
        int bottom = y + GAUGE_HEIGHT - 1;
        guiGraphics.fill(x + 1, bottom - filledHeight, x + GAUGE_WIDTH - 1, bottom, WATER);
    }

    private void renderStatusSquare(GuiGraphics guiGraphics, int squareX, int squareY, int color) {
        int x = leftPos + squareX;
        int y = topPos + squareY;
        guiGraphics.fill(x, y, x + STATUS_SIZE, y + STATUS_SIZE, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + STATUS_SIZE - 2, y + STATUS_SIZE - 2, color);
    }

    private int statusColor(int status) {
        return switch (status) {
            case SilicaGelDehumidifierBlockEntity.STATUS_INVALID_STRUCTURE,
                    SilicaGelDehumidifierBlockEntity.STATUS_WATER_FULL,
                    SilicaGelDehumidifierBlockEntity.STATUS_OUTPUT_FULL -> BLOCKED;
            case SilicaGelDehumidifierBlockEntity.STATUS_NO_INPUT,
                    SilicaGelDehumidifierBlockEntity.STATUS_NO_RECIPE -> PANEL_DARK;
            default -> READY;
        };
    }

    private Component statusComponent(int status) {
        return switch (status) {
            case SilicaGelDehumidifierBlockEntity.STATUS_INVALID_STRUCTURE ->
                    Component.translatable("rngtech.silica_gel_dehumidifier.status.invalid_structure");
            case SilicaGelDehumidifierBlockEntity.STATUS_NO_INPUT ->
                    Component.translatable("rngtech.silica_gel_dehumidifier.status.no_input");
            case SilicaGelDehumidifierBlockEntity.STATUS_NO_RECIPE ->
                    Component.translatable("rngtech.silica_gel_dehumidifier.status.no_recipe");
            case SilicaGelDehumidifierBlockEntity.STATUS_WATER_FULL ->
                    Component.translatable("rngtech.silica_gel_dehumidifier.status.water_full");
            case SilicaGelDehumidifierBlockEntity.STATUS_OUTPUT_FULL ->
                    Component.translatable("rngtech.silica_gel_dehumidifier.status.output_full");
            default -> Component.translatable("rngtech.silica_gel_dehumidifier.status.ready");
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X + 9,
                WATER_GAUGE_Y + 2,
                Component.translatable("rngtech.purge.target.water_tank"),
                menu.waterAmount() > 0 ? Component.translatable("block.minecraft.water") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.waterAmount(),
                menu.waterCapacity()
        )) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X,
                WATER_GAUGE_Y,
                GAUGE_WIDTH,
                GAUGE_HEIGHT,
                FluidMeterTooltips.amount(Component.translatable("block.minecraft.water"), menu.waterAmount(), menu.waterCapacity())
        );
        for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
            int progressY = LANE_Y + lane * LANE_SPACING + 5;
            int statusY = progressY - 2;
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    PROGRESS_X,
                    progressY,
                    PROGRESS_WIDTH,
                    PROGRESS_HEIGHT,
                    Component.translatable(
                            "rngtech.silica_gel_dehumidifier.tooltip.lane_progress",
                            lane + 1,
                            menu.laneProgressTicks(lane),
                            menu.laneRequiredTicks(lane),
                            menu.laneWaterOutput(lane)
                    )
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    STATUS_X,
                    statusY,
                    STATUS_SIZE,
                    STATUS_SIZE,
                    Component.translatable(
                            "rngtech.silica_gel_dehumidifier.tooltip.lane_status",
                            lane + 1,
                            statusComponent(menu.laneStatus(lane))
                    )
            );
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STRUCTURE_X,
                STRUCTURE_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable(
                        "rngtech.silica_gel_dehumidifier.tooltip.structure",
                        menu.structureValid()
                        ? Component.translatable("rngtech.silica_gel_dehumidifier.structure.valid")
                        : Component.translatable("rngtech.silica_gel_dehumidifier.structure.invalid")
                )
        );
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 34 + column * 18, 113 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 34 + column * 18, 171);
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
