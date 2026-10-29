package com.rngtech.client.screen;

import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.energy.BiogasDigestion;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BioGeneratorScreen extends AbstractContainerScreen<BioGeneratorMenu> implements MachineTabs.Host {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFF4C9C70;
    private static final int FUEL = 0xFF7DA84B;
    private static final int STAT_ACCENT = 0xFF82B366;
    private static final int STAT_PANEL_X = 8;
    private static final int STAT_PANEL_Y = 18;
    private static final int STAT_PANEL_WIDTH = 224;
    private static final int ENERGY_GAUGE_X = 184;
    private static final int ENERGY_GAUGE_Y = 30;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int FUEL_BAR_X = 96;
    private static final int FUEL_BAR_Y = 52;
    private static final int FUEL_BAR_WIDTH = 72;
    private static final int FUEL_BAR_HEIGHT = 7;
    private static final int STATUS_ICON_X = 96;
    private static final int GENERATION_ICON_X = 112;
    private static final int OUTPUT_ICON_X = 128;
    private static final int FUEL_VALUE_ICON_X = 144;
    private static final int STATUS_ICON_Y = 37;
    private static final int ICON_SIZE = 12;
    private static final int RETENTION_DIAL_X = 96;
    private static final int RETENTION_DIAL_Y = 65;
    private static final int RETENTION_DIAL_WIDTH = 72;
    private static final int RETENTION_DIAL_HEIGHT = 7;
    private static final int RETENTION_POINTER_HEIGHT = 3;
    private static final int RETENTION_NOTCH_STEP = 5;
    private static final int RETENTION_COARSE_STEP = 5;
    private static final int RETENTION_SHORT_ZONE = 0xFFA8824A;
    private static final int RETENTION_RATED_NOTCH = 0xFFE9E9E9;
    private static final int RETENTION_LONG_ZONE = 0xFF5E8A6A;
    private static final int RETENTION_POINTER = 0xFF2F2F2F;
    private static final int GAS_GAUGE_X = 170;
    private static final int GAS_GAUGE_Y = 30;
    private static final int GAS_GAUGE_WIDTH = 10;
    private static final int GAS_GAUGE_HEIGHT = 52;
    private static final int GAS = 0xFFC2C76A;
    private static final int GAS_START_MARK = 0xFF2F2F2F;
    private static final int METHANE_GAUGE_X = 202;
    private static final int EXHAUST_GAUGE_X = 216;
    private static final int TAKE_OFF_GAUGE_Y = 30;
    private static final int TAKE_OFF_GAUGE_WIDTH = 10;
    private static final int TAKE_OFF_GAUGE_HEIGHT = 52;
    private static final int METHANE = 0xFF8FB8D8;
    private static final int EXHAUST = 0xFF7A7A72;
    private static final int TAKE_OFF_IDLE = 0xFF4A4A4A;
    private static final int WARNING = 0xFFD3A33A;
    private static final MachineScreenStyle.AffixLine[] EMPTY_AFFIX_LINES = new MachineScreenStyle.AffixLine[0];
    private static final String[] STAT_LABEL_KEYS = {
            "rngtech.stat.energy_generation",
            "rngtech.stat.energy_capacity",
            "rngtech.stat.efficiency",
            "rngtech.stat.fuel_efficiency",
            "rngtech.stat.potato_power",
            "rngtech.stat.carrot_power",
            "rngtech.stat.bread_power",
            "rngtech.stat.sapling_power",
            "rngtech.stat.seed_power",
            "rngtech.stat.plant_power",
            "rngtech.stat.organic_reagent_power",
            "rngtech.stat.composted_biomass_power",
            "rngtech.stat.algae_power",
            "rngtech.stat.rich_biomass_power",
            "rngtech.stat.fuel_duration"
    };
    private static final int[] STAT_DATA_INDICES = {
            BioGeneratorMenu.energyGenerationDataIndex(),
            BioGeneratorMenu.energyCapacityStatDataIndex(),
            BioGeneratorMenu.efficiencyDataIndex(),
            BioGeneratorMenu.fuelEfficiencyDataIndex(),
            BioGeneratorMenu.potatoPowerDataIndex(),
            BioGeneratorMenu.carrotPowerDataIndex(),
            BioGeneratorMenu.breadPowerDataIndex(),
            BioGeneratorMenu.saplingPowerDataIndex(),
            BioGeneratorMenu.seedPowerDataIndex(),
            BioGeneratorMenu.plantPowerDataIndex(),
            BioGeneratorMenu.organicReagentPowerDataIndex(),
            BioGeneratorMenu.compostedBiomassPowerDataIndex(),
            BioGeneratorMenu.algaePowerDataIndex(),
            BioGeneratorMenu.richBiomassPowerDataIndex(),
            BioGeneratorMenu.fuelDurationDataIndex()
    };
    private static final MachineStat[] STAT_TYPES = {
            MachineStat.ENERGY_GENERATION,
            MachineStat.ENERGY_CAPACITY,
            MachineStat.EFFICIENCY,
            MachineStat.FUEL_EFFICIENCY,
            MachineStat.POTATO_POWER,
            MachineStat.CARROT_POWER,
            MachineStat.BREAD_POWER,
            MachineStat.SAPLING_POWER,
            MachineStat.SEED_POWER,
            MachineStat.PLANT_POWER,
            MachineStat.ORGANIC_REAGENT_POWER,
            MachineStat.COMPOSTED_BIOMASS_POWER,
            MachineStat.ALGAE_POWER,
            MachineStat.RICH_BIOMASS_POWER,
            MachineStat.FUEL_DURATION
    };

    private final MachineTabs tabs;

    public BioGeneratorScreen(BioGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 200;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        tabs = MachineTabs.builder(this)
                .processing(BioGeneratorMenu.TAB_PROCESSING, ModBlocks.BIO_GENERATOR.get())
                .gear(BioGeneratorMenu.TAB_GEAR)
                .stats(BioGeneratorMenu.TAB_STATS)
                .refinement(BioGeneratorMenu.TAB_REFINEMENT)
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
        renderStatTooltips(guiGraphics, mouseX, mouseY);
        RefinementScreenStyle.renderTooltips(guiGraphics, font, leftPos, topPos, mouseX, mouseY, menu.selectedTab() == RefinementScreenStyle.REFINEMENT_TAB_INDEX, menu.machineTraits(), menu);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        tabs.render(guiGraphics, mouseX, mouseY, menu.selectedTab());
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == BioGeneratorMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_REFINEMENT) {
            renderRefinement(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        tabs.renderSelectedLabel(guiGraphics, menu.selectedTab(), titleLabelX, titleLabelY, TEXT_MUTED);
        if (menu.selectedTab() != BioGeneratorMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == BioGeneratorMenu.TAB_PROCESSING) {
            drawProcessingLabels(guiGraphics);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_GEAR) {
            drawGearLabels(guiGraphics);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_REFINEMENT) {
            drawRefinementLabels(guiGraphics);
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
            if (menu.selectedTab() == BioGeneratorMenu.TAB_REFINEMENT
                    && RefinementScreenStyle.handleApplyClick(this, menu, mouseX, mouseY)) {
                return true;
            }
            if (menu.selectedTab() == BioGeneratorMenu.TAB_PROCESSING && isOverRetentionDial(mouseX, mouseY)) {
                double fraction = (mouseX - (leftPos + RETENTION_DIAL_X + 1)) / (RETENTION_DIAL_WIDTH - 2);
                sendRetention(BiogasDigestion.MIN_RETENTION_DAYS + (int) Math.round(fraction * retentionSpan()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.selectedTab() == BioGeneratorMenu.TAB_PROCESSING && isOverRetentionDial(mouseX, mouseY) && scrollY != 0.0) {
            int step = hasShiftDown() ? RETENTION_COARSE_STEP : 1;
            sendRetention(menu.retentionDays() + (scrollY > 0.0 ? step : -step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendRetention(int days) {
        int clamped = BiogasDigestion.clampRetention(days);
        if (clamped == menu.retentionDays() || minecraft == null || minecraft.player == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = BioGeneratorMenu.retentionButtonId(clamped);
        if (menu.clickMenuButton(minecraft.player, buttonId)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private boolean isOverRetentionDial(double mouseX, double mouseY) {
        int left = leftPos + RETENTION_DIAL_X;
        int top = topPos + RETENTION_DIAL_Y;
        return mouseX >= left && mouseX < left + RETENTION_DIAL_WIDTH
                && mouseY >= top && mouseY < top + RETENTION_DIAL_HEIGHT + RETENTION_POINTER_HEIGHT;
    }

    private static int retentionSpan() {
        return BiogasDigestion.MAX_RETENTION_DAYS - BiogasDigestion.MIN_RETENTION_DAYS;
    }

    private static int retentionOffset(int days, int width) {
        return Math.round((BiogasDigestion.clampRetention(days) - BiogasDigestion.MIN_RETENTION_DAYS) * width / (float) retentionSpan());
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
        if (menu.selectedTab() == BioGeneratorMenu.TAB_PROCESSING) {
            renderSlotFrame(guiGraphics, 52, 54);
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_GEAR) {
            for (int x : BioGeneratorMenu.GEAR_SLOT_X) {
                renderSlotFrame(guiGraphics, x - 1, BioGeneratorMenu.GEAR_SLOT_Y - 1);
            }
        } else if (menu.selectedTab() == BioGeneratorMenu.TAB_REFINEMENT) {
            renderSlotFrame(guiGraphics, RefinementScreenStyle.TARGET_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
            renderSlotFrame(guiGraphics, RefinementScreenStyle.CONSUMABLE_SLOT_X - 1, RefinementScreenStyle.SLOT_Y - 1);
        }

        if (menu.selectedTab() == BioGeneratorMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(
                x + ENERGY_GAUGE_X,
                y + ENERGY_GAUGE_Y,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );

        guiGraphics.fill(x + FUEL_BAR_X, y + FUEL_BAR_Y, x + FUEL_BAR_X + FUEL_BAR_WIDTH, y + FUEL_BAR_Y + FUEL_BAR_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(
                x + FUEL_BAR_X + 1,
                y + FUEL_BAR_Y + 1,
                x + FUEL_BAR_X + 1 + Math.round((FUEL_BAR_WIDTH - 2) * menu.burnProgress()),
                y + FUEL_BAR_Y + FUEL_BAR_HEIGHT - 1,
                FUEL
        );
        renderGasHolder(guiGraphics);
        renderTakeOffTank(guiGraphics, METHANE_GAUGE_X, menu.methane(), METHANE);
        renderTakeOffTank(guiGraphics, EXHAUST_GAUGE_X, menu.exhaust(), EXHAUST);
        renderRetentionDial(guiGraphics);
        renderProcessingIcons(guiGraphics);
    }

    /** Gas holder fill, with a mark at the pressure the engine needs to start. */
    private void renderGasHolder(GuiGraphics guiGraphics) {
        int left = leftPos + GAS_GAUGE_X;
        int top = topPos + GAS_GAUGE_Y;
        int inner = GAS_GAUGE_HEIGHT - 4;
        int bottom = top + GAS_GAUGE_HEIGHT - 2;
        guiGraphics.fill(left, top, left + GAS_GAUGE_WIDTH, top + GAS_GAUGE_HEIGHT, 0xFF5F5F5F);
        int gasHeight = Math.round(inner * menu.gasProgress());
        guiGraphics.fill(left + 2, bottom - gasHeight, left + GAS_GAUGE_WIDTH - 2, bottom, GAS);
        int capacity = Math.max(1, menu.gasCapacity());
        int mark = bottom - Math.round(inner * BiogasDigestion.ENGINE_START_GAS / (float) capacity);
        guiGraphics.fill(left + 1, mark, left + GAS_GAUGE_WIDTH - 1, mark + 1, GAS_START_MARK);
    }

    /** A take-off output tank; its frame darkens while no pump sends gas to it. */
    private void renderTakeOffTank(GuiGraphics guiGraphics, int x, int amount, int color) {
        int left = leftPos + x;
        int top = topPos + TAKE_OFF_GAUGE_Y;
        int inner = TAKE_OFF_GAUGE_HEIGHT - 4;
        int bottom = top + TAKE_OFF_GAUGE_HEIGHT - 2;
        guiGraphics.fill(left, top, left + TAKE_OFF_GAUGE_WIDTH, top + TAKE_OFF_GAUGE_HEIGHT, menu.takeOff() ? 0xFF5F5F5F : TAKE_OFF_IDLE);
        int capacity = Math.max(1, menu.takeOffCapacity());
        int height = Math.round(inner * Math.min(1.0F, amount / (float) capacity));
        guiGraphics.fill(left + 2, bottom - height, left + TAKE_OFF_GAUGE_WIDTH - 2, bottom, color);
    }

    /** Retention from short to long: the rated notch keeps the listed fuel values, and the pointer marks the setting. */
    private void renderRetentionDial(GuiGraphics guiGraphics) {
        int left = leftPos + RETENTION_DIAL_X;
        int top = topPos + RETENTION_DIAL_Y;
        int inner = RETENTION_DIAL_WIDTH - 2;
        int rated = left + 1 + retentionOffset(BiogasDigestion.RATED_RETENTION_DAYS, inner);
        int right = left + 1 + inner;
        int bottom = top + RETENTION_DIAL_HEIGHT - 1;
        guiGraphics.fill(left, top, left + RETENTION_DIAL_WIDTH, top + RETENTION_DIAL_HEIGHT, 0xFF5F5F5F);
        guiGraphics.fill(left + 1, top + 1, rated, bottom, RETENTION_SHORT_ZONE);
        guiGraphics.fill(rated + 1, top + 1, right, bottom, RETENTION_LONG_ZONE);
        for (int days = BiogasDigestion.MIN_RETENTION_DAYS; days <= BiogasDigestion.MAX_RETENTION_DAYS; days += RETENTION_NOTCH_STEP) {
            int x = Math.min(left + 1 + retentionOffset(days, inner), right - 1);
            guiGraphics.fill(x, top + 1, x + 1, top + 2, 0xFF8B8B8B);
        }
        guiGraphics.fill(rated, top + 1, rated + 1, bottom, RETENTION_RATED_NOTCH);
        int pointer = Math.min(left + 1 + retentionOffset(menu.retentionDays(), inner), right - 1);
        int pointerTop = top + RETENTION_DIAL_HEIGHT;
        guiGraphics.fill(pointer, pointerTop - 1, pointer + 1, pointerTop + RETENTION_POINTER_HEIGHT, RETENTION_POINTER);
        guiGraphics.fill(pointer - 1, pointerTop + 1, pointer + 2, pointerTop + RETENTION_POINTER_HEIGHT, RETENTION_POINTER);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderCompactStatsPanel(
                guiGraphics,
                leftPos,
                topPos,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                statLines(),
                EMPTY_AFFIX_LINES,
                STAT_ACCENT
        );
    }

    private void renderRefinement(GuiGraphics guiGraphics) {
        RefinementScreenStyle.render(guiGraphics, leftPos, topPos, menu.machineTraits(), STAT_ACCENT);
    }

    private void drawProcessingLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("rngtech.processing.biomass"), 46, 43, TEXT_MUTED, false);
    }

    private void drawGearLabels(GuiGraphics guiGraphics) {
        int[] x = BioGeneratorMenu.GEAR_SLOT_X;
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.battery_cell.short"), x[0] + 8);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.bio_chamber.short"), x[1] + 8);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.gas_line.short"), x[2] + 8);
        drawGearLabel(guiGraphics, Component.translatable("rngtech.gear.gas_line.short"), x[3] + 8);
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawCompactStatsPanelLabels(
                guiGraphics,
                font,
                Component.translatable("rngtech.tab.stats"),
                Component.empty(),
                statLines(),
                EMPTY_AFFIX_LINES,
                STAT_PANEL_X,
                STAT_PANEL_Y,
                STAT_PANEL_WIDTH,
                STAT_ACCENT
        );
    }

    private void drawRefinementLabels(GuiGraphics guiGraphics) {
        RefinementScreenStyle.drawLabels(guiGraphics, font, menu.machineTraits(), STAT_ACCENT);
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case BioGeneratorBlockEntity.STATUS_NO_FUEL -> Component.translatable("rngtech.bio_generator.status.no_fuel");
            case BioGeneratorBlockEntity.STATUS_INVALID_FUEL -> Component.translatable("rngtech.bio_generator.status.invalid_fuel");
            case BioGeneratorBlockEntity.STATUS_ENERGY_FULL -> Component.translatable("rngtech.bio_generator.status.energy_full");
            case BioGeneratorBlockEntity.STATUS_PRIMING -> Component.translatable("rngtech.bio_generator.status.priming");
            case BioGeneratorBlockEntity.STATUS_TAKE_OFF -> Component.translatable("rngtech.bio_generator.status.take_off");
            case BioGeneratorBlockEntity.STATUS_METHANE_FULL -> Component.translatable("rngtech.bio_generator.status.methane_full");
            case BioGeneratorBlockEntity.STATUS_NEEDS_PUMP -> Component.translatable("rngtech.bio_generator.status.needs_pump");
            default -> Component.translatable("rngtech.bio_generator.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case BioGeneratorBlockEntity.STATUS_NO_FUEL -> PANEL_DARK;
            case BioGeneratorBlockEntity.STATUS_ENERGY_FULL -> 0xFFD3A33A;
            case BioGeneratorBlockEntity.STATUS_INVALID_FUEL -> 0xFFB45B4A;
            case BioGeneratorBlockEntity.STATUS_PRIMING -> GAS;
            case BioGeneratorBlockEntity.STATUS_TAKE_OFF -> METHANE;
            case BioGeneratorBlockEntity.STATUS_METHANE_FULL, BioGeneratorBlockEntity.STATUS_NEEDS_PUMP -> WARNING;
            default -> FUEL;
        };
    }

    private List<Component> retentionTooltip() {
        int days = menu.retentionDays();
        String regime = days < BiogasDigestion.RATED_RETENTION_DAYS
                ? "short"
                : days == BiogasDigestion.RATED_RETENTION_DAYS ? "rated" : "long";
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "rngtech.bio_generator.tooltip.retention",
                days,
                Component.translatable("rngtech.bio_generator.retention." + regime)
        ));
        lines.add(Component.translatable(
                "rngtech.bio_generator.tooltip.retention_effects",
                multiplier(BiogasDigestion.digestionRate(days)),
                multiplier(BiogasDigestion.gasYield(days))
        ).withStyle(ChatFormatting.GRAY));
        if (menu.feedEnergy() > 0) {
            lines.add(Component.translatable(
                    "rngtech.bio_generator.tooltip.retention_item",
                    CompactValueText.energyRate(menu.engineRate()),
                    CompactValueText.energyAmount((int) Math.round(menu.feedEnergy() * BiogasDigestion.gasYield(days)))
            ).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(
                "rngtech.bio_generator.tooltip.retention_digested",
                percent(BiogasDigestion.digestedShare(days))
        ).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("rngtech.bio_generator.tooltip.retention_hint", BiogasDigestion.RATED_RETENTION_DAYS)
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private List<Component> gasTooltip() {
        Component gas = Component.translatable(
                "rngtech.bio_generator.tooltip.gas",
                CompactValueText.energyAmount(menu.gas()),
                CompactValueText.energyAmount(menu.gasCapacity())
        );
        if (menu.takeOff()) {
            return List.of(gas, Component.translatable("rngtech.bio_generator.tooltip.gas_take_off").withStyle(ChatFormatting.GRAY));
        }
        return List.of(
                gas,
                (menu.engineRunning()
                        ? Component.translatable("rngtech.bio_generator.tooltip.engine_running", CompactValueText.energyRate(menu.engineRate()))
                        : Component.translatable("rngtech.bio_generator.tooltip.engine_start", CompactValueText.energyAmount(BiogasDigestion.ENGINE_START_GAS)))
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private List<Component> methaneTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("rngtech.bio_generator.tooltip.methane", menu.methane(), menu.takeOffCapacity()));
        if (menu.takeOff()) {
            lines.add(Component.translatable("rngtech.bio_generator.tooltip.methane_rate", rate(menu.methaneRate()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("rngtech.bio_generator.tooltip.recovery", decimal(menu.recovery() * 100.0))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("rngtech.bio_generator.tooltip.take_off_needs_pump").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> exhaustTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("rngtech.bio_generator.tooltip.exhaust", menu.exhaust(), menu.takeOffCapacity()));
        if (!menu.takeOff()) {
            lines.add(Component.translatable("rngtech.bio_generator.tooltip.take_off_needs_pump").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        String key = menu.capturesCarbon() ? "rngtech.bio_generator.tooltip.exhaust_captured" : "rngtech.bio_generator.tooltip.exhaust_vented";
        lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (menu.ventRate() > 0.0) {
            lines.add(Component.translatable("rngtech.bio_generator.tooltip.vent_rate", rate(menu.ventRate()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static String rate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String multiplier(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.0f", fraction * 100.0);
    }

    private Component exactEnergyText() {
        return CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity());
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BioGeneratorMenu.TAB_PROCESSING) {
            return;
        }
        if (isOverRetentionDial(mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, retentionTooltip(), mouseX, mouseY);
            return;
        }
        if (isOver(mouseX, mouseY, GAS_GAUGE_X, GAS_GAUGE_Y, GAS_GAUGE_WIDTH, GAS_GAUGE_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, gasTooltip(), mouseX, mouseY);
            return;
        }
        if (isOver(mouseX, mouseY, METHANE_GAUGE_X, TAKE_OFF_GAUGE_Y, TAKE_OFF_GAUGE_WIDTH, TAKE_OFF_GAUGE_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, methaneTooltip(), mouseX, mouseY);
            return;
        }
        if (isOver(mouseX, mouseY, EXHAUST_GAUGE_X, TAKE_OFF_GAUGE_Y, TAKE_OFF_GAUGE_WIDTH, TAKE_OFF_GAUGE_HEIGHT)) {
            guiGraphics.renderComponentTooltip(font, exhaustTooltip(), mouseX, mouseY);
            return;
        }
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                ENERGY_GAUGE_X,
                ENERGY_GAUGE_Y,
                ENERGY_GAUGE_WIDTH,
                ENERGY_GAUGE_HEIGHT,
                Component.translatable("rngtech.bio_generator.tooltip.energy", exactEnergyText())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FUEL_BAR_X,
                FUEL_BAR_Y,
                FUEL_BAR_WIDTH,
                FUEL_BAR_HEIGHT,
                fuelTooltip()
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
                Component.translatable("rngtech.bio_generator.tooltip.status", statusComponent())
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                GENERATION_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                menu.takeOff()
                        ? Component.translatable("rngtech.bio_generator.tooltip.methane_rate", rate(menu.methaneRate()))
                        : Component.translatable("rngtech.bio_generator.tooltip.generation", CompactValueText.energyRate(menu.energyPerTick()))
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                OUTPUT_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable("rngtech.bio_generator.tooltip.output", CompactValueText.energyRate(menu.maxOutput()))
        );
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                FUEL_VALUE_ICON_X,
                STATUS_ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                Component.translatable("rngtech.bio_generator.tooltip.fuel_value", CompactValueText.energyAmount(menu.recipeEnergy()))
        );
    }

    private Component fuelTooltip() {
        int total = menu.totalBurnTime();
        if (total <= 0) {
            return Component.translatable("rngtech.bio_generator.tooltip.fuel.empty", CompactValueText.energyAmount(menu.recipeEnergy()));
        }
        return Component.translatable(
                "rngtech.bio_generator.tooltip.fuel",
                menu.burnTime(),
                total,
                CompactValueText.energyAmount(menu.recipeEnergy())
        );
    }

    private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() != BioGeneratorMenu.TAB_STATS) {
            return;
        }
        MachineScreenStyle.renderCompactStatPanelTooltip(
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
                    false,
                    isEnhancedStat(STAT_DATA_INDICES[index], value),
                    MachineScreenStyle.statLayerTooltip(
                            menu.getSlot(menu.refinementTargetSlot()).getItem(),
                            STAT_TYPES[index],
                            value
                    )
            ).withStat(STAT_TYPES[index]);
        }
        return MachineScreenStyle.withGenerationBreakdown(
                STAT_TYPES,
                statLines,
                menu.energyPerTick(),
                menu.statValue(BioGeneratorMenu.baseEnergyGenerationDataIndex()),
                menu.statValue(BioGeneratorMenu.flatEnergyGenerationDataIndex()),
                menu.packGenerationMultiplier()
        );
    }

    private boolean isEnhancedStat(int dataIndex, double value) {
        return value > 1.001;
    }

    private void renderProcessingIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(
                leftPos + STATUS_ICON_X + 3,
                topPos + STATUS_ICON_Y + 3,
                leftPos + STATUS_ICON_X + 9,
                topPos + STATUS_ICON_Y + 9,
                statusColor()
        );

        renderIconBox(guiGraphics, GENERATION_ICON_X, STATUS_ICON_Y);
        int sparkX = leftPos + GENERATION_ICON_X;
        int sparkY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(sparkX + 6, sparkY + 2, sparkX + 9, sparkY + 3, FUEL);
        guiGraphics.fill(sparkX + 5, sparkY + 3, sparkX + 8, sparkY + 6, FUEL);
        guiGraphics.fill(sparkX + 4, sparkY + 6, sparkX + 7, sparkY + 7, FUEL);
        guiGraphics.fill(sparkX + 3, sparkY + 7, sparkX + 6, sparkY + 10, FUEL);

        renderIconBox(guiGraphics, OUTPUT_ICON_X, STATUS_ICON_Y);
        int arrowX = leftPos + OUTPUT_ICON_X;
        int arrowY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(arrowX + 3, arrowY + 5, arrowX + 8, arrowY + 7, ENERGY);
        guiGraphics.fill(arrowX + 8, arrowY + 4, arrowX + 10, arrowY + 8, ENERGY);
        guiGraphics.fill(arrowX + 10, arrowY + 5, arrowX + 11, arrowY + 7, ENERGY);

        renderIconBox(guiGraphics, FUEL_VALUE_ICON_X, STATUS_ICON_Y);
        int fuelX = leftPos + FUEL_VALUE_ICON_X;
        int fuelY = topPos + STATUS_ICON_Y;
        guiGraphics.fill(fuelX + 5, fuelY + 2, fuelX + 8, fuelY + 3, FUEL);
        guiGraphics.fill(fuelX + 4, fuelY + 3, fuelX + 9, fuelY + 6, FUEL);
        guiGraphics.fill(fuelX + 3, fuelY + 6, fuelX + 7, fuelY + 9, FUEL);
        guiGraphics.fill(fuelX + 7, fuelY + 7, fuelX + 9, fuelY + 10, FUEL);
    }

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
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

    private void drawGearLabel(GuiGraphics guiGraphics, Component label, int centerX) {
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, centerX, 35, 48, TEXT_MUTED);
    }

    private void renderSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFFE0E0E0);
        guiGraphics.fill(left + 2, top + 2, left + 16, top + 16, 0xFF8B8B8B);
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
