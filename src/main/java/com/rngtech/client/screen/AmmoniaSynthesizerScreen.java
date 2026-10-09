package com.rngtech.client.screen;

import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.menu.AmmoniaSynthesizerMenu;
import com.rngtech.rpg.MachineStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AmmoniaSynthesizerScreen extends AbstractContainerScreen<AmmoniaSynthesizerMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF5F5F5F;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF5D8DBA;
    private static final int NITROGEN = 0xFF8BA4C9;
    private static final int HYDROGEN = 0xFFE3D2A2;
    private static final int AMMONIA = 0xFFBFD48B;
    private static final int PROGRESS = 0xFF71A67B;
    private static final int STAT_ACCENT = 0xFF74A7A0;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;

    public AmmoniaSynthesizerScreen(AmmoniaSynthesizerMenu menu, Inventory playerInventory, Component title) {
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        renderTabs(guiGraphics);
        if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_PROCESSING) {
            bar(guiGraphics, 28, 28, menu.energyFill(), ENERGY);
            bar(guiGraphics, 48, 28, menu.nitrogenFill(), NITROGEN);
            bar(guiGraphics, 68, 28, menu.hydrogenFill(), HYDROGEN);
            bar(guiGraphics, 154, 28, menu.ammoniaFill(), AMMONIA);
            renderPurgeButtons(guiGraphics);
            guiGraphics.fill(leftPos + 92, topPos + 54, leftPos + 140, topPos + 62, DARK);
            guiGraphics.fill(leftPos + 93, topPos + 55, leftPos + 93 + Math.round(46 * menu.progressFill()), topPos + 61, PROGRESS);
            statusIcon(guiGraphics, 110, 66);
        } else if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_GEAR) {
            slot(guiGraphics, 79, 47);
        } else if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, STAT_ACCENT);
        } else {
            slot(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            slot(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
        }
        if (menu.selectedTab() != AmmoniaSynthesizerMenu.TAB_STATS) {
            playerSlots(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != AmmoniaSynthesizerMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_GEAR) {
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.catalyst_bed.short"), 88, 35, 54, MUTED);
        } else if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), 8, 18, 224, STAT_ACCENT);
        } else if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_REFINEMENT) {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int index = 0; index < 4; index++) {
                if (isOverTab(mouseX, mouseY, index)) {
                    menu.selectTab(index);
                    return true;
                }
            }
            if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private MachineScreenStyle.StatLine[] statLines() {
        return new MachineScreenStyle.StatLine[] {
                stat("rngtech.stat.energy_usage", menu.statValue(AmmoniaSynthesizerMenu.energyUsageDataIndex()), MachineStat.ENERGY_USAGE),
                stat("rngtech.stat.processing_speed", menu.statValue(AmmoniaSynthesizerMenu.processingSpeedDataIndex()), MachineStat.PROCESSING_SPEED)
        };
    }

    private MachineScreenStyle.StatLine stat(String key, double value, MachineStat stat) {
        return MachineScreenStyle.statLine(
                Component.translatable(key),
                MachineScreenStyle.statValue(stat, value),
                value,
                false,
                value > 1.001,
                MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), stat, value)
        );
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        tab(guiGraphics, 0, Component.translatable("rngtech.tab.processing.short"));
        tab(guiGraphics, 1, Component.translatable("rngtech.tab.gear"));
        tab(guiGraphics, 2, Component.translatable("rngtech.tab.stats"));
        tab(guiGraphics, 3, Component.translatable("rngtech.tab.refinement.short"));
    }

    private void tab(GuiGraphics guiGraphics, int index, Component label) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        guiGraphics.fill(x, y, x + TAB_WIDTH, y + 21, menu.selectedTab() == index ? PANEL : 0xFF9A9A9A);
        guiGraphics.drawString(font, label, x + (TAB_WIDTH - font.width(label)) / 2, y + 7, TEXT, false);
    }

    private void bar(GuiGraphics guiGraphics, int x, int y, float fill, int color) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 10, topPos + y + 50, DARK);
        int height = Math.round(48 * fill);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 49 - height, leftPos + x + 9, topPos + y + 49, color);
    }

    private void statusIcon(GuiGraphics guiGraphics, int x, int y) {
        int color = menu.statusCode() == AmmoniaSynthesizerBlockEntity.STATUS_READY ? PROGRESS : 0xFFB45B4A;
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 12, topPos + y + 12, DARK);
        guiGraphics.fill(leftPos + x + 3, topPos + y + 3, leftPos + x + 9, topPos + y + 9, color);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case AmmoniaSynthesizerBlockEntity.STATUS_MISSING_CATALYST -> Component.translatable("rngtech.ammonia_synthesizer.status.no_catalyst");
            case AmmoniaSynthesizerBlockEntity.STATUS_NO_RECIPE -> Component.translatable("rngtech.ammonia_synthesizer.status.no_recipe");
            case AmmoniaSynthesizerBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.ammonia_synthesizer.status.no_power");
            case AmmoniaSynthesizerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.ammonia_synthesizer.status.output_full");
            case AmmoniaSynthesizerBlockEntity.STATUS_NO_FLUID -> menu.nitrogen() <= 0
                    ? Component.translatable("rngtech.ammonia_synthesizer.status.no_nitrogen")
                    : Component.translatable("rngtech.ammonia_synthesizer.status.no_hydrogen");
            default -> Component.translatable("rngtech.ammonia_synthesizer.status.ready");
        };
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable("rngtech.ammonia_synthesizer.tooltip.progress.empty");
        }
        return Component.translatable("rngtech.ammonia_synthesizer.tooltip.progress", menu.progress(), ticks);
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != AmmoniaSynthesizerMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 28, 28, 10, 50,
                Component.translatable("rngtech.ammonia_synthesizer.tooltip.energy", CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 48, 28, 10, 50,
                FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.nitrogen"), menu.nitrogen(), menu.tankCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 68, 28, 10, 50,
                FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.hydrogen"), menu.hydrogen(), menu.tankCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 154, 28, 10, 50,
                FluidMeterTooltips.amount(Component.translatable("fluid.rngtech.ammonia"), menu.ammonia(), menu.tankCapacity()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 92, 54, 48, 8, progressTooltip());
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 110, 66, 12, 12,
                Component.translatable("rngtech.ammonia_synthesizer.tooltip.status", statusComponent()));
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, 48, 30, menu.nitrogen() > 0);
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, 68, 30, menu.hydrogen() > 0);
        FluidPurgeButton.render(guiGraphics, leftPos, topPos, 154, 30, menu.ammonia() > 0);
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                48,
                30,
                AmmoniaSynthesizerBlockEntity.PURGE_NITROGEN_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                68,
                30,
                AmmoniaSynthesizerBlockEntity.PURGE_HYDROGEN_TANK
        ) || FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                154,
                30,
                AmmoniaSynthesizerBlockEntity.PURGE_OUTPUT_TANK
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
                48,
                30,
                Component.translatable("rngtech.purge.target.nitrogen_tank"),
                menu.nitrogen() > 0 ? Component.translatable("fluid.rngtech.nitrogen") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.nitrogen(),
                menu.tankCapacity()
        )) {
            return true;
        }
        if (FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                68,
                30,
                Component.translatable("rngtech.purge.target.hydrogen_tank"),
                menu.hydrogen() > 0 ? Component.translatable("fluid.rngtech.hydrogen") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.hydrogen(),
                menu.tankCapacity()
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
                154,
                30,
                Component.translatable("rngtech.purge.target.output_tank"),
                menu.ammonia() > 0 ? Component.translatable("fluid.rngtech.ammonia") : Component.translatable("rngtech.purge.empty_fluid"),
                menu.ammonia(),
                menu.tankCapacity()
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == AmmoniaSynthesizerMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 8, 18, 224, statLines());
        }
    }

    private void playerSlots(GuiGraphics guiGraphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(guiGraphics, 38 + column * 18, 115 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(guiGraphics, 38 + column * 18, 173);
        }
    }

    private void slot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, 0xFF373737);
        guiGraphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, 0xFFE0E0E0);
        guiGraphics.fill(leftPos + x + 2, topPos + y + 2, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }
}
