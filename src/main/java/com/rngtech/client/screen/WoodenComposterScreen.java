package com.rngtech.client.screen;

import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.menu.WoodenComposterMenu;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WoodenComposterScreen extends AbstractContainerScreen<WoodenComposterMenu> implements MachineTabs.Host {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int COMPOST = 0xFF7DA84B;
    private static final int WATER = 0xFF2E82D8;
    private static final int BAR_BACKING = 0xFF5F5F5F;
    private static final int INPUT_X = 8;
    private static final int INPUT_Y = 30;
    private static final int INPUT_COLUMNS = WoodenComposterBlockEntity.INPUT_SLOT_COUNT / 3;
    // The right side is two columns; the Output row lines up with the middle row of inputs.
    private static final int OUTPUT_X = 161;
    private static final int OUTPUT_Y = 49;
    private static final int CATALYST_X = 202;
    private static final int CATALYST_Y = 49;
    private static final int WATER_INPUT_X = 161;
    private static final int WATER_INPUT_Y = 77;
    private static final int WATER_OUTPUT_X = 202;
    private static final int WATER_OUTPUT_Y = 77;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 91;
    private static final int PROGRESS_WIDTH = 144;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int WATER_GAUGE_X = 160;
    private static final int WATER_GAUGE_Y = 30;
    private static final int WATER_GAUGE_WIDTH = 47;
    private static final int WATER_GAUGE_HEIGHT = 8;
    private static final int WATER_PURGE_X = 209;
    private static final int WATER_PURGE_Y = 29;
    private static final int TAB_PROCESSING = 0;
    private static final int STATUS_X = 108;
    private static final int STATUS_Y = 101;
    private static final int STATUS_SIZE = 12;
    private static final int RICH_STATUS_X = 92;
    private static final int BULK_STATUS_X = 124;
    private static final int VARIETY_STATUS_X = 140;
    private static final int BULK_BONUS = 0xFF7DA84B;
    private static final int VARIETY_BONUS = 0xFFD3A33A;
    private static final int RICH_BATCH = 0xFF4F7D2E;

    private final MachineTabs tabs;

    public WoodenComposterScreen(WoodenComposterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 232;
        imageHeight = 204;
        inventoryLabelX = 35;
        inventoryLabelY = 108;
        tabs = MachineTabs.builder(this)
                .processing(TAB_PROCESSING, ModBlocks.WOODEN_COMPOSTER.get())
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
        renderValueTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, TAB_PROCESSING);
        renderMachineSlots(guiGraphics);
        renderPlayerInventoryFrames(guiGraphics);
        renderProgress(guiGraphics);
        renderWaterGauge(guiGraphics);
        renderStatusIcon(guiGraphics);
        renderBonusStatusSquares(guiGraphics);
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, WATER_PURGE_X, WATER_PURGE_Y, menu.waterAmount() > 0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, TAB_PROCESSING, titleLabelX, titleLabelY, TEXT_MUTED);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.wooden_composter.inputs"), 8, 18, TEXT_MUTED, false);
        renderCenteredLabel(guiGraphics, "rngtech.wooden_composter.output", OUTPUT_X + 8, 39);
        renderCenteredLabel(guiGraphics, "rngtech.wooden_composter.catalyst", CATALYST_X + 8, 39);
        renderCenteredLabel(guiGraphics, "rngtech.wooden_composter.water", (WATER_INPUT_X + WATER_OUTPUT_X + 16) / 2, 67);
    }

    private void renderCenteredLabel(GuiGraphics guiGraphics, String key, int centerX, int y) {
        Component label = Component.translatable(key);
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_PURGE_X,
                WATER_PURGE_Y,
                WoodenComposterBlockEntity.PURGE_WATER_TANK
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

    private void renderMachineSlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < INPUT_COLUMNS; column++) {
                renderSlotFrame(guiGraphics, INPUT_X + column * 18, INPUT_Y + row * 18);
            }
        }
        renderSlotFrame(guiGraphics, OUTPUT_X - 1, OUTPUT_Y - 1);
        renderSlotFrame(guiGraphics, CATALYST_X - 1, CATALYST_Y - 1);
        renderSlotFrame(guiGraphics, WATER_INPUT_X - 1, WATER_INPUT_Y - 1);
        renderSlotFrame(guiGraphics, WATER_OUTPUT_X - 1, WATER_OUTPUT_Y - 1);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        guiGraphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, BAR_BACKING);
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.compostProgress()),
                y + PROGRESS_HEIGHT - 1,
                COMPOST
        );
    }

    private void renderWaterGauge(GuiGraphics guiGraphics) {
        int x = leftPos + WATER_GAUGE_X;
        int y = topPos + WATER_GAUGE_Y;
        guiGraphics.fill(x, y, x + WATER_GAUGE_WIDTH, y + WATER_GAUGE_HEIGHT, BAR_BACKING);
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + 1 + Math.round((WATER_GAUGE_WIDTH - 2) * menu.waterProgress()),
                y + WATER_GAUGE_HEIGHT - 1,
                WATER
        );
    }

    private void renderStatusIcon(GuiGraphics guiGraphics) {
        renderStatusSquare(guiGraphics, STATUS_X, statusColor());
    }

    private void renderBonusStatusSquares(GuiGraphics guiGraphics) {
        renderStatusSquare(guiGraphics, RICH_STATUS_X, menu.nextBatchRich() ? RICH_BATCH : PANEL_DARK);
        renderStatusSquare(guiGraphics, BULK_STATUS_X, menu.bulkBonusActive() ? BULK_BONUS : PANEL_DARK);
        renderStatusSquare(guiGraphics, VARIETY_STATUS_X, menu.varietyBonusActive() ? VARIETY_BONUS : PANEL_DARK);
    }

    private void renderStatusSquare(GuiGraphics guiGraphics, int squareX, int color) {
        int x = leftPos + squareX;
        int y = topPos + STATUS_Y;
        guiGraphics.fill(x, y, x + STATUS_SIZE, y + STATUS_SIZE, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + STATUS_SIZE - 2, y + STATUS_SIZE - 2, color);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case WoodenComposterBlockEntity.STATUS_OUTPUT_FULL, WoodenComposterBlockEntity.STATUS_OUTPUT_MIXED -> 0xFFB45B4A;
            case WoodenComposterBlockEntity.STATUS_NO_INPUT -> PANEL_DARK;
            case WoodenComposterBlockEntity.STATUS_COMPOSTING -> menu.wateredBatch() ? WATER : COMPOST;
            default -> COMPOST;
        };
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case WoodenComposterBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.wooden_composter.status.output_full");
            case WoodenComposterBlockEntity.STATUS_OUTPUT_MIXED -> Component.translatable("rngtech.wooden_composter.status.output_mixed");
            case WoodenComposterBlockEntity.STATUS_NO_INPUT -> Component.translatable("rngtech.wooden_composter.status.no_input");
            case WoodenComposterBlockEntity.STATUS_COMPOSTING -> Component.translatable("rngtech.wooden_composter.status.composting");
            default -> Component.translatable("rngtech.wooden_composter.status.ready");
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
                WATER_PURGE_X,
                WATER_PURGE_Y,
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
                PROGRESS_X,
                PROGRESS_Y,
                PROGRESS_WIDTH,
                PROGRESS_HEIGHT,
                Component.translatable(
                        "rngtech.wooden_composter.tooltip.progress",
                        menu.progressTicks(),
                        menu.requiredTicks(),
                        menu.availableItems(),
                        menu.speedBonusPercent()
                )
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X,
                WATER_GAUGE_Y,
                WATER_GAUGE_WIDTH,
                WATER_GAUGE_HEIGHT,
                FluidMeterTooltips.amount(Component.translatable("block.minecraft.water"), menu.waterAmount(), menu.waterCapacity())
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
                Component.translatable("rngtech.wooden_composter.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                RICH_STATUS_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                menu.nextBatchRich()
                        ? Component.translatable("rngtech.wooden_composter.tooltip.rich_batch", menu.catalystCharges())
                        : Component.translatable("rngtech.wooden_composter.tooltip.composted_batch")
        );
        if (menu.getSlot(WoodenComposterBlockEntity.SLOT_CATALYST).getItem().isEmpty()) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    CATALYST_X,
                    CATALYST_Y,
                    16,
                    16,
                    Component.translatable("rngtech.wooden_composter.tooltip.catalyst")
            );
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                BULK_STATUS_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable(
                        "rngtech.wooden_composter.tooltip.bulk_bonus",
                        menu.availableItems(),
                        WoodenComposterBlockEntity.BULK_BONUS_UNIT_THRESHOLD,
                        10
                )
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                VARIETY_STATUS_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable(
                        "rngtech.wooden_composter.tooltip.variety_bonus",
                        menu.varietyCount(),
                        WoodenComposterBlockEntity.VARIETY_BONUS_ITEM_THRESHOLD,
                        10
                )
        );
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 34 + column * 18, 119 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 34 + column * 18, 177);
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
