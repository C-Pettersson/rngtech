package com.rngtech.client.screen;

import com.rngtech.content.blockentity.WoodenDehumidifierBlockEntity;
import com.rngtech.content.menu.WoodenDehumidifierMenu;
import com.rngtech.content.registry.ModFluids;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class WoodenDehumidifierScreen extends AbstractContainerScreen<WoodenDehumidifierMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int WATER = 0xFF2E82D8;
    private static final int EXHAUST = 0xFF6A7778;
    private static final int WOOD = 0xFF9B6A3A;
    private static final int DRY = 0xFFD3A33A;
    private static final int BLOCKED = 0xFFB45B4A;
    private static final int BAR_BACKING = 0xFF5F5F5F;
    private static final int WATER_GAUGE_X = 12;
    private static final int WATER_GAUGE_Y = 24;
    private static final int OUTPUT_GAUGE_X = 176;
    private static final int OUTPUT_GAUGE_Y = 24;
    private static final int GAUGE_WIDTH = 36;
    private static final int GAUGE_HEIGHT = 52;
    private static final int CONVERSION_SLOT_X = 60;
    private static final int CONVERSION_SLOT_Y = 54;
    private static final int EMPTY_CONTAINER_X = 176;
    private static final int FILLED_CONTAINER_X = 198;
    private static final int CONTAINER_Y = 78;
    private static final int SELECTOR_X = 156;
    private static final int SELECTOR_Y = 80;
    private static final int SELECTOR_SIZE = 14;
    private static final int PROGRESS_X = 88;
    private static final int PROGRESS_Y = 59;
    private static final int PROGRESS_WIDTH = 66;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_X = 88;
    private static final int STRUCTURE_X = 104;
    private static final int HUMIDITY_X = 120;
    private static final int STATUS_Y = 38;
    private static final int STATUS_SIZE = 12;

    public WoodenDehumidifierScreen(WoodenDehumidifierMenu menu, Inventory playerInventory, Component title) {
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
        renderGauge(guiGraphics, WATER_GAUGE_X, WATER_GAUGE_Y, menu.waterProgress(), WATER);
        renderGauge(guiGraphics, OUTPUT_GAUGE_X, OUTPUT_GAUGE_Y, menu.convertedOutputProgress(), EXHAUST);
        renderPurgeButtons(guiGraphics);
        renderProgress(guiGraphics);
        renderStatusSquare(guiGraphics, STATUS_X, statusColor());
        renderStatusSquare(guiGraphics, STRUCTURE_X, menu.structureValid() ? WOOD : BLOCKED);
        renderStatusSquare(guiGraphics, HUMIDITY_X, humidityColor());
        renderSelector(guiGraphics);
        renderContainerArrow(guiGraphics, EMPTY_CONTAINER_X + 18, CONTAINER_Y + 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePurgeButtonClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && inBounds(mouseX, mouseY, SELECTOR_X, SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            sendButton(WoodenDehumidifierMenu.BUTTON_CYCLE_OUTPUT);
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
        renderSlotFrame(guiGraphics, CONVERSION_SLOT_X, CONVERSION_SLOT_Y);
        renderSlotFrame(guiGraphics, EMPTY_CONTAINER_X, CONTAINER_Y);
        renderSlotFrame(guiGraphics, FILLED_CONTAINER_X, CONTAINER_Y);
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderGauge(GuiGraphics guiGraphics, int gaugeX, int gaugeY, float fill, int color) {
        int x = leftPos + gaugeX;
        int y = topPos + gaugeY;
        guiGraphics.fill(x, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, BAR_BACKING);
        int filledHeight = Math.round((GAUGE_HEIGHT - 2) * fill);
        int bottom = y + GAUGE_HEIGHT - 1;
        guiGraphics.fill(x + 1, bottom - filledHeight, x + GAUGE_WIDTH - 1, bottom, color);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        guiGraphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, BAR_BACKING);
        guiGraphics.fill(
                x + 1,
                y + 1,
                x + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.conversionProgress()),
                y + PROGRESS_HEIGHT - 1,
                WOOD
        );
    }

    private void renderStatusSquare(GuiGraphics guiGraphics, int squareX, int color) {
        int x = leftPos + squareX;
        int y = topPos + STATUS_Y;
        guiGraphics.fill(x, y, x + STATUS_SIZE, y + STATUS_SIZE, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + STATUS_SIZE - 2, y + STATUS_SIZE - 2, color);
    }

    private void renderSelector(GuiGraphics guiGraphics) {
        int x = leftPos + SELECTOR_X;
        int y = topPos + SELECTOR_Y;
        guiGraphics.fill(x, y, x + SELECTOR_SIZE, y + SELECTOR_SIZE, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + SELECTOR_SIZE - 2, y + SELECTOR_SIZE - 2, fluidColor(menu.selectedOutput()));
    }

    private void renderContainerArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        int x = leftPos + arrowX;
        int y = topPos + arrowY;
        guiGraphics.fill(x, y, x + 3, y + 2, TEXT);
        guiGraphics.fill(x + 2, y - 1, x + 4, y + 3, TEXT);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case WoodenDehumidifierBlockEntity.STATUS_INVALID_STRUCTURE,
                    WoodenDehumidifierBlockEntity.STATUS_OUTPUT_FULL -> BLOCKED;
            case WoodenDehumidifierBlockEntity.STATUS_NO_WATER -> WATER;
            case WoodenDehumidifierBlockEntity.STATUS_NO_RECIPE -> PANEL_DARK;
            default -> WOOD;
        };
    }

    private int humidityColor() {
        if (!menu.solarAccess()) {
            return BLOCKED;
        }
        return switch (menu.humidityBand()) {
            case WoodenDehumidifierBlockEntity.HUMIDITY_WET -> WATER;
            case WoodenDehumidifierBlockEntity.HUMIDITY_DRY -> DRY;
            case WoodenDehumidifierBlockEntity.HUMIDITY_BLOCKED -> BLOCKED;
            default -> WOOD;
        };
    }

    private int fluidColor(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return WATER;
        }
        if (fluid == ModFluids.CARBON_EXHAUST_SOURCE.get()) {
            return EXHAUST;
        }
        return WOOD;
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case WoodenDehumidifierBlockEntity.STATUS_INVALID_STRUCTURE ->
                    Component.translatable("rngtech.wooden_dehumidifier.status.invalid_structure");
            case WoodenDehumidifierBlockEntity.STATUS_NO_RECIPE -> Component.translatable("rngtech.wooden_dehumidifier.status.no_recipe");
            case WoodenDehumidifierBlockEntity.STATUS_NO_WATER -> Component.translatable("rngtech.wooden_dehumidifier.status.no_water");
            case WoodenDehumidifierBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.wooden_dehumidifier.status.output_full");
            default -> Component.translatable("rngtech.wooden_dehumidifier.status.ready");
        };
    }

    private Component humidityComponent() {
        return switch (menu.humidityBand()) {
            case WoodenDehumidifierBlockEntity.HUMIDITY_WET -> Component.translatable("rngtech.wooden_dehumidifier.humidity.wet");
            case WoodenDehumidifierBlockEntity.HUMIDITY_DRY -> Component.translatable("rngtech.wooden_dehumidifier.humidity.dry");
            case WoodenDehumidifierBlockEntity.HUMIDITY_BLOCKED -> Component.translatable("rngtech.wooden_dehumidifier.humidity.blocked");
            default -> Component.translatable("rngtech.wooden_dehumidifier.humidity.normal");
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
                WATER_GAUGE_X,
                WATER_GAUGE_Y,
                GAUGE_WIDTH,
                GAUGE_HEIGHT,
                FluidMeterTooltips.amount(Component.translatable("block.minecraft.water"), menu.waterAmount(), menu.waterCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_GAUGE_X,
                OUTPUT_GAUGE_Y,
                GAUGE_WIDTH,
                GAUGE_HEIGHT,
                FluidMeterTooltips.amount(fluidName(menu.convertedOutputFluid()), menu.convertedOutputAmount(), menu.convertedOutputCapacity())
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
                Component.translatable(
                        "rngtech.wooden_dehumidifier.tooltip.progress",
                        menu.progressTicks(),
                        menu.requiredTicks()
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
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable("rngtech.wooden_dehumidifier.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                STRUCTURE_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable(
                        "rngtech.wooden_dehumidifier.tooltip.structure",
                        menu.frameCount(),
                        menu.structureValid() ? statusValid() : statusInvalid()
                )
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                HUMIDITY_X,
                STATUS_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable(
                        "rngtech.wooden_dehumidifier.tooltip.humidity",
                        humidityComponent(),
                        menu.solarAccess()
                                ? Component.translatable("rngtech.wooden_dehumidifier.solar_access")
                                : Component.translatable("rngtech.wooden_dehumidifier.no_solar_access"),
                        menu.rainBonusActive() ? Component.translatable("rngtech.wooden_dehumidifier.rain_bonus") : Component.translatable("rngtech.wooden_dehumidifier.no_rain_bonus"),
                        menu.productionRatePerMinute()
                )
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                SELECTOR_X,
                SELECTOR_Y,
                SELECTOR_SIZE,
                SELECTOR_SIZE,
                Component.translatable("rngtech.wooden_dehumidifier.tooltip.selected_output", fluidName(menu.selectedOutput()))
        );
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                WATER_GAUGE_X + 13,
                WATER_GAUGE_Y + 2,
                menu.waterAmount() > 0
        );
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                OUTPUT_GAUGE_X + 13,
                OUTPUT_GAUGE_Y + 2,
                menu.convertedOutputAmount() > 0
        );
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X + 13,
                WATER_GAUGE_Y + 2,
                WoodenDehumidifierBlockEntity.PURGE_WATER_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_GAUGE_X + 13,
                OUTPUT_GAUGE_Y + 2,
                WoodenDehumidifierBlockEntity.PURGE_CONVERTED_OUTPUT_TANK
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
                WATER_GAUGE_X + 13,
                WATER_GAUGE_Y + 2,
                Component.translatable("rngtech.purge.target.water_tank"),
                menu.waterAmount() > 0 ? Component.translatable("block.minecraft.water") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.waterAmount(),
                menu.waterCapacity()
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
                OUTPUT_GAUGE_X + 13,
                OUTPUT_GAUGE_Y + 2,
                Component.translatable("rngtech.purge.target.converted_output_tank"),
                menu.convertedOutputAmount() > 0 ? fluidName(menu.convertedOutputFluid()) : Component.translatable("rngtech.purge.empty_fluid"),
                menu.convertedOutputAmount(),
                menu.convertedOutputCapacity()
        );
    }

    private Component fluidName(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return Component.translatable("rngtech.wooden_dehumidifier.converted_output");
        }
        return new FluidStack(fluid, 1).getHoverName();
    }

    private Component statusValid() {
        return Component.translatable("rngtech.wooden_dehumidifier.structure.valid");
    }

    private Component statusInvalid() {
        return Component.translatable("rngtech.wooden_dehumidifier.structure.invalid");
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
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
