package com.rngtech.client.screen;

import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.menu.AlgaePhotobioreactorMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AlgaePhotobioreactorScreen extends AbstractContainerScreen<AlgaePhotobioreactorMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int WATER = 0xFF2E82D8;
    private static final int CARBON = 0xFF6A7778;
    private static final int ALGAE = 0xFF5AA449;
    private static final int LIGHT = 0xFFD7B64A;
    private static final int BAR_BACKING = 0xFF5F5F5F;
    private static final int WATER_GAUGE_X = 8;
    private static final int WATER_GAUGE_Y = 21;
    private static final int CARBON_GAUGE_X = 54;
    private static final int CARBON_GAUGE_Y = 21;
    private static final int GAUGE_WIDTH = 40;
    private static final int GAUGE_HEIGHT = 40;
    private static final int PROGRESS_X = 98;
    private static final int PROGRESS_Y = 53;
    private static final int PROGRESS_WIDTH = 58;
    private static final int PROGRESS_HEIGHT = 8;
    private static final int STATUS_X = 98;
    private static final int STATUS_Y = 37;
    private static final int STATUS_SIZE = 12;
    private static final int LIGHT_X = 114;
    private static final int LIGHT_Y = 37;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int OUTPUT_X = 138;
    private static final int OUTPUT_Y = 65;
    private static final int WATER_INPUT_X = 8;
    private static final int WATER_OUTPUT_X = 30;
    private static final int WATER_CONTAINER_Y = 65;
    private static final int CARBON_INPUT_X = 54;
    private static final int CARBON_OUTPUT_X = 76;
    private static final int CARBON_CONTAINER_Y = 65;
    private static final int BIO_CHAMBER_X = 109;
    private static final int BIO_CHAMBER_Y = 47;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final String[] STAT_LABEL_KEYS = {"rngtech.stat.bio_conversion"};
    private static final int[] STAT_DATA_INDICES = {AlgaePhotobioreactorMenu.bioConversionDataIndex()};

    public AlgaePhotobioreactorScreen(AlgaePhotobioreactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 186;
        inventoryLabelX = 8;
        inventoryLabelY = 90;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_STATS) {
            renderStats(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != AlgaePhotobioreactorMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(AlgaePhotobioreactorMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(AlgaePhotobioreactorMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(AlgaePhotobioreactorMenu.TAB_STATS);
                return true;
            }
            if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
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
        renderTab(
                guiGraphics,
                0,
                Component.translatable("rngtech.tab.processing.short"),
                menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_PROCESSING
        );
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_GEAR);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_STATS);
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
        if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, OUTPUT_X, OUTPUT_Y);
            renderSlotFrame(guiGraphics, WATER_INPUT_X, WATER_CONTAINER_Y);
            renderSlotFrame(guiGraphics, WATER_OUTPUT_X, WATER_CONTAINER_Y);
            renderSlotFrame(guiGraphics, CARBON_INPUT_X, CARBON_CONTAINER_Y);
            renderSlotFrame(guiGraphics, CARBON_OUTPUT_X, CARBON_CONTAINER_Y);
            renderContainerArrow(guiGraphics, WATER_INPUT_X + 18, WATER_CONTAINER_Y + 8);
            renderContainerArrow(guiGraphics, CARBON_INPUT_X + 18, CARBON_CONTAINER_Y + 8);
        } else if (menu.selectedTab() == AlgaePhotobioreactorMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, BIO_CHAMBER_X, BIO_CHAMBER_Y);
        }

        if (menu.selectedTab() != AlgaePhotobioreactorMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        renderGauge(guiGraphics, WATER_GAUGE_X, WATER_GAUGE_Y, menu.waterProgress(), WATER);
        renderGauge(guiGraphics, CARBON_GAUGE_X, CARBON_GAUGE_Y, menu.carbonProgress(), CARBON);
        renderProgress(guiGraphics);
        renderStatusSquare(guiGraphics, STATUS_X, STATUS_Y, statusColor());
        renderStatusSquare(guiGraphics, LIGHT_X, LIGHT_Y, menu.lightLevel() >= menu.minimumLight() ? LIGHT : PANEL_DARK);
        renderPurgeButtons(guiGraphics);
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
                ALGAE
        );
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.bio_chamber.short"), 118, 35, 54, TEXT_MUTED);
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
                ALGAE
        );
    }

    private void renderGauge(GuiGraphics guiGraphics, int gaugeX, int gaugeY, float fill, int color) {
        int x = leftPos + gaugeX;
        int y = topPos + gaugeY;
        guiGraphics.fill(x, y, x + GAUGE_WIDTH, y + GAUGE_HEIGHT, BAR_BACKING);
        int filledHeight = Math.round((GAUGE_HEIGHT - 2) * fill);
        int bottom = y + GAUGE_HEIGHT - 1;
        guiGraphics.fill(x + 1, bottom - filledHeight, x + GAUGE_WIDTH - 1, bottom, color);
    }

    private void renderContainerArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        int x = leftPos + arrowX;
        int y = topPos + arrowY;
        guiGraphics.fill(x, y, x + 3, y + 2, TEXT_MUTED);
        guiGraphics.fill(x + 2, y - 1, x + 4, y + 3, TEXT_MUTED);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        guiGraphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, BAR_BACKING);
        guiGraphics.fill(x + 1, y + 1, x + 1 + Math.round((PROGRESS_WIDTH - 2) * menu.growthProgress()), y + PROGRESS_HEIGHT - 1, ALGAE);
    }

    private void renderStatusSquare(GuiGraphics guiGraphics, int squareX, int squareY, int color) {
        int x = leftPos + squareX;
        int y = topPos + squareY;
        guiGraphics.fill(x, y, x + STATUS_SIZE, y + STATUS_SIZE, 0xFF373737);
        guiGraphics.fill(x + 2, y + 2, x + STATUS_SIZE - 2, y + STATUS_SIZE - 2, color);
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case AlgaePhotobioreactorBlockEntity.STATUS_READY -> ALGAE;
            case AlgaePhotobioreactorBlockEntity.STATUS_LOW_LIGHT -> LIGHT;
            case AlgaePhotobioreactorBlockEntity.STATUS_OUTPUT_FULL -> 0xFFB45B4A;
            case AlgaePhotobioreactorBlockEntity.STATUS_NO_WATER -> WATER;
            case AlgaePhotobioreactorBlockEntity.STATUS_NO_CARBON -> CARBON;
            default -> PANEL_DARK;
        };
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case AlgaePhotobioreactorBlockEntity.STATUS_READY -> Component.translatable("rngtech.algae_photobioreactor.status.ready");
            case AlgaePhotobioreactorBlockEntity.STATUS_LOW_LIGHT -> Component.translatable("rngtech.algae_photobioreactor.status.low_light");
            case AlgaePhotobioreactorBlockEntity.STATUS_OUTPUT_FULL ->
                    Component.translatable("rngtech.algae_photobioreactor.status.output_full");
            case AlgaePhotobioreactorBlockEntity.STATUS_NO_WATER -> Component.translatable("rngtech.algae_photobioreactor.status.no_water");
            case AlgaePhotobioreactorBlockEntity.STATUS_NO_CARBON -> Component.translatable("rngtech.algae_photobioreactor.status.no_carbon");
            default -> Component.translatable("rngtech.algae_photobioreactor.status.no_recipe");
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlgaePhotobioreactorMenu.TAB_PROCESSING) {
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
                WATER_GAUGE_X,
                WATER_GAUGE_Y,
                GAUGE_WIDTH,
                GAUGE_HEIGHT,
                FluidMeterTooltips.amount(menu.waterFluidName(), menu.waterAmount(), menu.waterCapacity())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                CARBON_GAUGE_X,
                CARBON_GAUGE_Y,
                GAUGE_WIDTH,
                GAUGE_HEIGHT,
                FluidMeterTooltips.amount(menu.carbonFluidName(), menu.carbonAmount(), menu.carbonCapacity())
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
                Component.translatable("rngtech.algae_photobioreactor.tooltip.progress", menu.progressTicks(), menu.requiredTicks())
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
                Component.translatable("rngtech.algae_photobioreactor.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                LIGHT_X,
                LIGHT_Y,
                STATUS_SIZE,
                STATUS_SIZE,
                Component.translatable("rngtech.algae_photobioreactor.tooltip.light", menu.lightLevel(), menu.minimumLight())
        );
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                WATER_GAUGE_X + 15,
                WATER_GAUGE_Y + 2,
                menu.waterAmount() > 0
        );
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                CARBON_GAUGE_X + 15,
                CARBON_GAUGE_Y + 2,
                menu.carbonAmount() > 0
        );
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                WATER_GAUGE_X + 15,
                WATER_GAUGE_Y + 2,
                AlgaePhotobioreactorBlockEntity.PURGE_WATER_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                CARBON_GAUGE_X + 15,
                CARBON_GAUGE_Y + 2,
                AlgaePhotobioreactorBlockEntity.PURGE_CARBON_TANK
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
                WATER_GAUGE_X + 15,
                WATER_GAUGE_Y + 2,
                Component.translatable("rngtech.purge.target.water_tank"),
                menu.waterAmount() > 0 ? menu.waterFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
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
                CARBON_GAUGE_X + 15,
                CARBON_GAUGE_Y + 2,
                Component.translatable("rngtech.purge.target.carbon_tank"),
                menu.carbonAmount() > 0 ? menu.carbonFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.carbonAmount(),
                menu.carbonCapacity()
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AlgaePhotobioreactorMenu.TAB_STATS) {
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
        return MachineScreenStyle.statValue(MachineStat.FUEL_EFFICIENCY, menu.statValue(STAT_DATA_INDICES[index]));
    }

    private MachineScreenStyle.StatLine[] statLines() {
        MachineScreenStyle.StatLine[] statLines = new MachineScreenStyle.StatLine[STAT_LABEL_KEYS.length];
        for (int index = 0; index < STAT_LABEL_KEYS.length; index++) {
            double value = menu.statValue(STAT_DATA_INDICES[index]);
            statLines[index] = MachineScreenStyle.statLine(
                    Component.translatable(STAT_LABEL_KEYS[index]),
                    statValue(index),
                    value,
                    false,
                    value > 1.001,
                    Component.translatable(
                            "rngtech.algae_photobioreactor.tooltip.bio_conversion",
                            statValue(index),
                            Math.round(menu.outputBonusProgress() * 100.0)
                    )
            );
        }
        return statLines;
    }

    private void renderPlayerInventoryFrames(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(guiGraphics, 7 + column * 18, 101 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(guiGraphics, 7 + column * 18, 159);
        }
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
