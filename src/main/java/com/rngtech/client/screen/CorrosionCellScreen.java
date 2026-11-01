package com.rngtech.client.screen;

import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.energy.CorrosionCellLoad;
import com.rngtech.content.item.CathodeItem;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class CorrosionCellScreen extends AbstractContainerScreen<CorrosionCellMenu> implements MachineTabs.Host {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF4F9F91;
    private static final int PROGRESS = 0xFF6B8F4A;
    private static final int STAT_ACCENT = 0xFF4F9F91;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int PLATE_SLOT_X = 16;
    private static final int PLATE_SLOT_Y = 32;
    private static final int ELECTROLYTE_SLOT_X = 39;
    private static final int ELECTROLYTE_SLOT_Y = 78;
    private static final int CATHODE_BADGE_X = 19;
    private static final int CATHODE_BADGE_Y = 55;
    private static final int CATHODE_BADGE_SIZE = 12;
    private static final int RESIDUE_SLOT_X = 175;
    private static final int RESIDUE_SLOT_Y = 70;
    private static final int ELECTROLYTE_METER_X = 42;
    private static final int ELECTROLYTE_METER_Y = 36;
    private static final int ELECTROLYTE_METER_WIDTH = 12;
    private static final int ELECTROLYTE_METER_HEIGHT = 38;
    private static final int PURGE_BUTTON_X = ELECTROLYTE_METER_X + (ELECTROLYTE_METER_WIDTH - FluidPurgeButton.SIZE) / 2;
    private static final int ENERGY_METER_X = 158;
    private static final int ENERGY_METER_Y = 36;
    private static final int ENERGY_METER_WIDTH = 12;
    private static final int ENERGY_METER_HEIGHT = 52;
    private static final int PROGRESS_BAR_X = 72;
    private static final int PROGRESS_BAR_Y = 70;
    private static final int PROGRESS_BAR_WIDTH = 64;
    private static final int PROGRESS_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 72;
    private static final int STATUS_ICON_Y = 55;
    private static final int GENERATION_ICON_X = 88;
    private static final int OUTPUT_ICON_X = 104;
    private static final int ICON_SIZE = 12;
    private static final int LOAD_GAUGE_X = 72;
    private static final int LOAD_GAUGE_Y = 84;
    private static final int LOAD_GAUGE_WIDTH = 64;
    private static final int LOAD_GAUGE_HEIGHT = 8;
    private static final int LOAD_POINTER_HEIGHT = 3;
    private static final int LOAD_TRACK = 0xFF2F2F2F;
    private static final int LOAD_DIM = 0xFF2E5A66;
    private static final int LOAD_FUEL_LINE = 0xFFD3A33A;
    private static final int LOAD_POINTER = 0xFFF2F2F2;
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.fluid_transfer",
            "rngtech.stat.efficiency",
            "rngtech.stat.processing_speed",
            "rngtech.stat.stability"
    };
    private static final int[] STAT_DATA_INDICES = {
            CorrosionCellMenu.energyGenerationDataIndex(),
            CorrosionCellMenu.energyCapacityDataIndex(),
            CorrosionCellMenu.fluidTransferDataIndex(),
            CorrosionCellMenu.efficiencyDataIndex(),
            CorrosionCellMenu.processingSpeedDataIndex(),
            CorrosionCellMenu.stabilityDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.FLUID_TRANSFER,
            MachineStat.EFFICIENCY,
            MachineStat.PROCESSING_SPEED,
            MachineStat.STABILITY
    };

    private final MachineTabs tabs;

    public CorrosionCellScreen(CorrosionCellMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(CorrosionCellMenu.TAB_PROCESSING, ModBlocks.CORROSION_CELL.get())
                .gear(CorrosionCellMenu.TAB_GEAR)
                .stats(CorrosionCellMenu.TAB_STATS)
                .refinement(CorrosionCellMenu.TAB_REFINEMENT)
                .jewels(CorrosionCellMenu.TAB_JEWELS, menu::jewelsShown)
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
        if (menu.selectedTab() == CorrosionCellMenu.TAB_JEWELS) {
            List<Component> jewelTooltip = JewelsTab.tooltip(this);
            if (!jewelTooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(font, jewelTooltip, mouseX, mouseY);
            }
        }
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        if (menu.selectedTab() == CorrosionCellMenu.TAB_JEWELS) {
            JewelsTab.render(guiGraphics, font, menu, leftPos, topPos, TEXT, TEXT_MUTED, (x, y) -> renderSlotFrame(guiGraphics, x, y));
        }
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
        if (menu.selectedTab() != CorrosionCellMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_GEAR) {
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), 61, 35);
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.fluid_pump.short"), 103, 35);
            drawCentered(guiGraphics, Component.translatable("rngtech.gear.cathode.short"), 145, 35);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            MachineScreenStyle.drawStatPanelLabels(guiGraphics, font, Component.translatable("rngtech.tab.stats"), statLines(), STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, STAT_ACCENT);
        } else {
            RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = tabs.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                menu.selectTab(tab);
                return true;
            }
            if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING && handlePurgeButtonClick(mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING && isOverLoadGauge(mouseX, mouseY)) {
                double fraction = (mouseX - (leftPos + LOAD_GAUGE_X + 1)) / (LOAD_GAUGE_WIDTH - 3);
                sendLoad((int) Math.round(CorrosionCellLoad.MIN_LOAD + fraction * (CorrosionCellLoad.MAX_LOAD - CorrosionCellLoad.MIN_LOAD)));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING && isOverLoadGauge(mouseX, mouseY) && scrollY != 0.0) {
            sendLoad(menu.load() + (scrollY > 0.0 ? CorrosionCellLoad.STEP : -CorrosionCellLoad.STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendLoad(int load) {
        int snapped = CorrosionCellLoad.snap(load);
        if (snapped == menu.load() || minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = CorrosionCellMenu.loadButtonId(snapped);
        if (menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private boolean isOverLoadGauge(double mouseX, double mouseY) {
        int left = leftPos + LOAD_GAUGE_X;
        int top = topPos + LOAD_GAUGE_Y;
        return mouseX >= left && mouseX < left + LOAD_GAUGE_WIDTH && mouseY >= top && mouseY < top + LOAD_GAUGE_HEIGHT + LOAD_POINTER_HEIGHT;
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
        if (menu.selectedTab() == CorrosionCellMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, PLATE_SLOT_X, PLATE_SLOT_Y);
            renderSlotFrame(guiGraphics, ELECTROLYTE_SLOT_X, ELECTROLYTE_SLOT_Y);
            renderSlotFrame(guiGraphics, RESIDUE_SLOT_X, RESIDUE_SLOT_Y);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 52, 47);
            renderSlotFrame(guiGraphics, 94, 47);
            renderSlotFrame(guiGraphics, 136, 47);
        } else if (menu.selectedTab() == CorrosionCellMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() != CorrosionCellMenu.TAB_STATS) {
            renderPlayerInventoryFrames(guiGraphics);
        }
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + ELECTROLYTE_METER_X,
                y + ELECTROLYTE_METER_Y,
                x + ELECTROLYTE_METER_X + ELECTROLYTE_METER_WIDTH,
                y + ELECTROLYTE_METER_Y + ELECTROLYTE_METER_HEIGHT,
                0xFF5F5F5F
        );
        int electrolyteHeight = Math.round((ELECTROLYTE_METER_HEIGHT - 4) * menu.electrolyteFluidProgress());
        int electrolyteBottom = y + ELECTROLYTE_METER_Y + ELECTROLYTE_METER_HEIGHT - 2;
        FluidBarRenderer.fill(
                guiGraphics,
                menu.electrolyteFluidType(),
                x + ELECTROLYTE_METER_X + 2,
                electrolyteBottom - electrolyteHeight,
                x + ELECTROLYTE_METER_X + ELECTROLYTE_METER_WIDTH - 2,
                electrolyteBottom,
                0xFF5C86C7
        );
        renderCathodeBadge(guiGraphics);
        guiGraphics.fill(
                x + ENERGY_METER_X,
                y + ENERGY_METER_Y,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH,
                y + ENERGY_METER_Y + ENERGY_METER_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        int energyBottom = y + ENERGY_METER_Y + ENERGY_METER_HEIGHT - 2;
        guiGraphics.fill(
                x + ENERGY_METER_X + 2,
                energyBottom - energyHeight,
                x + ENERGY_METER_X + ENERGY_METER_WIDTH - 2,
                energyBottom,
                ENERGY
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
        renderLoadGauge(guiGraphics);
        renderProcessingIcons(guiGraphics);
        renderPurgeButtons(guiGraphics);
    }

    /**
     * The load gauge draws the trade itself: the teal profile is FE/t at each load, lit up to the current the cell draws
     * now, and the amber line is FE per fuel, highest at the lightest load. The pointer marks the selected load.
     */
    private void renderLoadGauge(GuiGraphics guiGraphics) {
        int left = leftPos + LOAD_GAUGE_X;
        int top = topPos + LOAD_GAUGE_Y;
        int inner = LOAD_GAUGE_WIDTH - 2;
        int innerHeight = LOAD_GAUGE_HEIGHT - 2;
        int bottom = top + LOAD_GAUGE_HEIGHT - 1;
        guiGraphics.fill(left, top, left + LOAD_GAUGE_WIDTH, top + LOAD_GAUGE_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, left + 1 + inner, bottom, LOAD_TRACK);
        double maxBonus = CorrosionCellLoad.fePerFuel(CorrosionCellLoad.MIN_LOAD) - 1.0;
        int live = menu.liveLoad();
        for (int column = 0; column < inner; column++) {
            int load = loadAtColumn(column, inner);
            int x = left + 1 + column;
            int height = Math.max(1, (int) Math.round(CorrosionCellLoad.fePerTick(load) * innerHeight));
            guiGraphics.fill(x, bottom - height, x + 1, bottom, live > 0 && load <= live ? ENERGY : LOAD_DIM);
            double bonus = maxBonus <= 0.0 ? 0.0 : (CorrosionCellLoad.fePerFuel(load) - 1.0) / maxBonus;
            int lineY = top + 1 + (int) Math.round((1.0 - bonus) * (innerHeight - 1));
            guiGraphics.fill(x, lineY, x + 1, lineY + 1, LOAD_FUEL_LINE);
        }
        int pointer = left + 1 + columnOfLoad(menu.load(), inner);
        int pointerTop = top + LOAD_GAUGE_HEIGHT;
        guiGraphics.fill(pointer, pointerTop - 1, pointer + 1, pointerTop + LOAD_POINTER_HEIGHT, LOAD_POINTER);
        guiGraphics.fill(pointer - 1, pointerTop + 1, pointer + 2, pointerTop + LOAD_POINTER_HEIGHT, LOAD_POINTER);
    }

    private static int loadAtColumn(int column, int columns) {
        double fraction = columns <= 1 ? 1.0 : column / (double) (columns - 1);
        return (int) Math.round(CorrosionCellLoad.MIN_LOAD + fraction * (CorrosionCellLoad.MAX_LOAD - CorrosionCellLoad.MIN_LOAD));
    }

    private static int columnOfLoad(int load, int columns) {
        double fraction = (CorrosionCellLoad.clamp(load) - CorrosionCellLoad.MIN_LOAD) / (double) (CorrosionCellLoad.MAX_LOAD - CorrosionCellLoad.MIN_LOAD);
        return (int) Math.round(fraction * (columns - 1));
    }

    private List<Component> loadTooltip() {
        int load = menu.load();
        String regime = load >= CorrosionCellLoad.RATED_LOAD ? "rated" : "light";
        int live = menu.liveLoad();
        return List.of(
                Component.translatable("rngtech.corrosion_cell.tooltip.load", load, Component.translatable("rngtech.corrosion_cell.load." + regime)),
                Component.translatable(
                        "rngtech.corrosion_cell.tooltip.load_effects",
                        multiplier(CorrosionCellLoad.fePerTick(load)),
                        multiplier(CorrosionCellLoad.fePerFuel(load)),
                        multiplier(CorrosionCellLoad.cycleLength(load))
                ).withStyle(ChatFormatting.GRAY),
                Component.translatable(
                        "rngtech.corrosion_cell.tooltip.load_voltage",
                        String.format(Locale.ROOT, "%.2f", CorrosionCellLoad.volts(load)),
                        String.format(Locale.ROOT, "%.2f", CorrosionCellLoad.RATED_VOLTS)
                ).withStyle(ChatFormatting.GRAY),
                live > 0
                        ? Component.translatable("rngtech.corrosion_cell.tooltip.load_live", live).withStyle(ChatFormatting.GRAY)
                        : Component.translatable("rngtech.corrosion_cell.tooltip.load_idle").withStyle(ChatFormatting.DARK_GRAY),
                Component.translatable("rngtech.corrosion_cell.tooltip.load_hint").withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    private static String multiplier(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines().length, STAT_ACCENT);
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.corrosion_cell.plate"), 18, 22, TEXT_MUTED, false);
        guiGraphics.drawString(font, Component.translatable("rngtech.corrosion_cell.residue"), 175, 56, TEXT_MUTED, false);
    }

    /** The installed Cathode, shown under the Fuel slot so the Anode gate is visible without opening the Gear tab. */
    private void renderCathodeBadge(GuiGraphics guiGraphics) {
        int left = leftPos + CATHODE_BADGE_X;
        int top = topPos + CATHODE_BADGE_Y;
        ItemStack cathode = menu.cathodeStack();
        if (cathode.isEmpty()) {
            guiGraphics.fill(left, top, left + CATHODE_BADGE_SIZE, top + CATHODE_BADGE_SIZE, 0xFF8B8B8B);
            guiGraphics.fill(left + 1, top + 1, left + CATHODE_BADGE_SIZE - 1, top + CATHODE_BADGE_SIZE - 1, PANEL);
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0);
        guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiGraphics.renderItem(cathode, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderInputTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredEmptyProcessSlot(CorrosionCellBlockEntity.SLOT_PLATE)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("rngtech.corrosion_cell.plate").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("rngtech.corrosion_cell.slot.fuel").withStyle(ChatFormatting.GRAY),
                    Component.translatable("rngtech.corrosion_cell.slot.fuel.anode").withStyle(ChatFormatting.DARK_GRAY)
            ), mouseX, mouseY);
            return;
        }
        if (hoveredEmptyProcessSlot(CorrosionCellBlockEntity.SLOT_ELECTROLYTE)) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("rngtech.corrosion_cell.electrolyte").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("rngtech.corrosion_cell.slot.electrolyte").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return;
        }
        int left = leftPos + CATHODE_BADGE_X;
        int top = topPos + CATHODE_BADGE_Y;
        if (mouseX < left || mouseX >= left + CATHODE_BADGE_SIZE || mouseY < top || mouseY >= top + CATHODE_BADGE_SIZE) {
            return;
        }
        ItemStack cathode = menu.cathodeStack();
        List<Component> lines = cathode.getItem() instanceof CathodeItem item
                ? List.of(
                        Component.translatable("rngtech.corrosion_cell.cathode.installed", cathode.getHoverName()).withStyle(ChatFormatting.YELLOW),
                        Component.translatable("rngtech.corrosion_cell.cathode.runs", item.stage()).withStyle(ChatFormatting.GRAY)
                )
                : List.of(
                        Component.translatable("rngtech.corrosion_cell.cathode.none").withStyle(ChatFormatting.YELLOW),
                        Component.translatable("rngtech.corrosion_cell.cathode.install").withStyle(ChatFormatting.GRAY)
                );
        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private boolean hoveredEmptyProcessSlot(int processSlot) {
        Slot slot = hoveredSlot;
        return slot != null && slot.isActive() && !slot.hasItem() && slot.index == processSlot;
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case CorrosionCellBlockEntity.STATUS_NO_PLATE -> Component.translatable("rngtech.corrosion_cell.status.no_plate");
            case CorrosionCellBlockEntity.STATUS_NO_ELECTROLYTE -> Component.translatable("rngtech.corrosion_cell.status.no_electrolyte");
            case CorrosionCellBlockEntity.STATUS_INVALID_RECIPE -> Component.translatable("rngtech.corrosion_cell.status.invalid_recipe");
            case CorrosionCellBlockEntity.STATUS_BLOCKED_STAGE -> Component.translatable("rngtech.corrosion_cell.status.blocked_stage", menu.minimumStage());
            case CorrosionCellBlockEntity.STATUS_BLOCKED_CATHODE -> Component.translatable("rngtech.corrosion_cell.status.blocked_cathode", menu.minimumStage());
            case CorrosionCellBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.corrosion_cell.status.output_full");
            case CorrosionCellBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.corrosion_cell.status.energy_full");
            case CorrosionCellBlockEntity.STATUS_REDSTONE_DISABLED -> Component.translatable("rngtech.corrosion_cell.status.redstone");
            default -> Component.translatable("rngtech.corrosion_cell.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case CorrosionCellBlockEntity.STATUS_READY -> ENERGY;
            case CorrosionCellBlockEntity.STATUS_NO_PLATE, CorrosionCellBlockEntity.STATUS_NO_ELECTROLYTE -> PANEL_DARK;
            case CorrosionCellBlockEntity.STATUS_ENERGY_FULL, CorrosionCellBlockEntity.STATUS_REDSTONE_DISABLED -> 0xFFD3A33A;
            default -> 0xFFB45B4A;
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != CorrosionCellMenu.TAB_PROCESSING) {
            return;
        }
        if (renderPurgeTooltips(guiGraphics, mouseX, mouseY)) {
            return;
        }
        if (isOverLoadGauge(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, loadTooltip(), mouseX, mouseY);
            return;
        }
        renderInputTooltips(guiGraphics, mouseX, mouseY);
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ELECTROLYTE_METER_X,
                ELECTROLYTE_METER_Y,
                ELECTROLYTE_METER_WIDTH,
                ELECTROLYTE_METER_HEIGHT,
                electrolyteTooltip()
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_METER_X,
                ENERGY_METER_Y,
                ENERGY_METER_WIDTH,
                ENERGY_METER_HEIGHT,
                CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity())
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
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STATUS_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.status", statusComponent()));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, GENERATION_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick())));
        CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, OUTPUT_ICON_X, STATUS_ICON_Y, ICON_SIZE, ICON_SIZE, Component.translatable("rngtech.corrosion_cell.tooltip.output", CompactValueText.energyRate(menu.maxOutput())));
    }

    private Component electrolyteTooltip() {
        return FluidMeterTooltips.amount(
                menu.electrolyteFluidName(),
                menu.electrolyteFluid(),
                menu.electrolyteFluidCapacity(),
                "transfer " + menu.fluidTransfer() + " mB/t"
        );
    }

    private Component progressTooltip() {
        int ticks = menu.processingTicks();
        if (ticks <= 0) {
            return Component.translatable("rngtech.corrosion_cell.tooltip.progress.empty", CompactValueText.energyAmount(menu.recipeEnergy()));
        }
        return Component.translatable("rngtech.corrosion_cell.tooltip.progress", menu.progress(), ticks, CompactValueText.energyAmount(menu.recipeEnergy()));
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == CorrosionCellMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, STAT_PANEL_X, STAT_PANEL_Y, STAT_PANEL_WIDTH, statLines());
        }
    }

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor());

        renderIconBox(guiGraphics, GENERATION_ICON_X, STATUS_ICON_Y);
        int sparkX = leftPos + GENERATION_ICON_X;
        int sparkY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, 0xFFD3A33A);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, 0xFFD3A33A);

        renderIconBox(guiGraphics, OUTPUT_ICON_X, STATUS_ICON_Y);
        int arrowX = leftPos + OUTPUT_ICON_X;
        int arrowY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(arrowX + 3, arrowY + 5, arrowX + 8, arrowY + 7, ENERGY);
        guiGraphics.fill(arrowX + 8, arrowY + 4, arrowX + 10, arrowY + 8, ENERGY);
        guiGraphics.fill(arrowX + 10, arrowY + 5, arrowX + 11, arrowY + 7, ENERGY);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderPurgeButtons(GuiGraphics guiGraphics) {
        FluidPurgeButton.render(
                guiGraphics,
                leftPos,
                topPos,
                PURGE_BUTTON_X,
                ELECTROLYTE_METER_Y + 2,
                menu.electrolyteFluid() > 0
        );
    }

    private boolean handlePurgeButtonClick(double mouseX, double mouseY) {
        return FluidPurgeButton.handleClick(
                menu,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PURGE_BUTTON_X,
                ELECTROLYTE_METER_Y + 2,
                CorrosionCellBlockEntity.PURGE_ELECTROLYTE_TANK
        );
    }

    private boolean renderPurgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        return FluidPurgeButton.renderTooltip(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                PURGE_BUTTON_X,
                ELECTROLYTE_METER_Y + 2,
                Component.translatable("rngtech.purge.target.electrolyte_tank"),
                menu.electrolyteFluid() > 0 ? menu.electrolyteFluidName() : Component.translatable("rngtech.purge.empty_fluid"),
                menu.electrolyteFluid(),
                menu.electrolyteFluidCapacity()
        );
    }

    private String statValue(int index) {
        double value = menu.statValue(STAT_DATA_INDICES[index]);
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
                    !isIntegralStat(STAT_DATA_INDICES[index]) && value > 1.001,
                    MachineScreenStyle.statLayerTooltip(menu.getSlot(menu.refinementTargetSlot()).getItem(), STAT_TYPES[index], value)
            ).withStat(STAT_TYPES[index]);
        }
        return MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(CorrosionCellMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(CorrosionCellMenu.flatEnergyGenerationDataIndex()),
                menu.packGenerationMultiplier()
        );
    }

    private boolean isIntegralStat(int dataIndex) {
        return dataIndex == CorrosionCellMenu.energyCapacityDataIndex()
                || dataIndex == CorrosionCellMenu.fluidTransferDataIndex();
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

    private void drawCentered(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(font, label, centerX - font.width(label) / 2, y, TEXT_MUTED, false);
    }
}
