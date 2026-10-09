package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.rpg.progression.ForestryCompanionPassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ForestryCartScreen extends AbstractContainerScreen<ForestryCartMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int PANEL_LIGHT = 0xFFE9E9E9;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF6B6B6B;
    private static final int ENERGY = 0xFFB43B28;
    private static final int FORESTRY = 0xFF4E8B54;
    private static final int WARNING = 0xFFB45B4A;
    private static final int TAB_WIDTH = 54;
    private static final int TAB_SPACING = 56;
    private static final int ENERGY_GAUGE_X = 214;
    private static final int ENERGY_GAUGE_Y = 42;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int STATUS_ICON_X = 134;
    private static final int STATUS_ICON_Y = 52;
    private static final int ACTION_ICON_X = 150;
    private static final int ACTION_ICON_Y = 52;
    private static final int RANGE_ICON_X = 134;
    private static final int RANGE_ICON_Y = 38;
    private static final int IDLE_ICON_X = 150;
    private static final int IDLE_ICON_Y = 38;
    private static final int WATER_GAUGE_X = 202;
    private static final int WATER_GAUGE_Y = 42;
    private static final int WATER_GAUGE_WIDTH = 8;
    private static final int WATER = 0xFF3F76E4;
    private static final int SCAN_DEBUG_BUTTON_X = 166;
    private static final int SCAN_DEBUG_BUTTON_Y = 52;
    private static final int MANUAL_SPEED_BUTTON_X = 166;
    private static final int MANUAL_SPEED_BUTTON_Y = 66;
    private static final int RESET_CELLS_BUTTON_X = 166;
    private static final int RESET_CELLS_BUTTON_Y = 38;
    private static final int CONTROL_BUTTON_WIDTH = 30;
    private static final int CONTROL_BUTTON_HEIGHT = 12;
    private static final int ICON_SIZE = 12;
    private static final int FERTILIZER_BAR_X = 42;
    private static final int FERTILIZER_BAR_Y = 100;
    private static final int FERTILIZER_BAR_WIDTH = 36;
    private static final int FERTILIZER = 0xFFE3DCC4;
    private static final int BASE_IMAGE_WIDTH = 240;
    private static final int BASE_IMAGE_HEIGHT = 200;
    private static final Map<MegaPassiveNode, ResourceLocation> MASTERY_ICON_TEXTURES = createMasteryIconTextures();

    private final MasteryScreenSupport<MegaPassiveNode> masterySupport;

    private static Map<MegaPassiveNode, ResourceLocation> createMasteryIconTextures() {
        Map<MegaPassiveNode, ResourceLocation> textures = new java.util.HashMap<>();
        for (MegaPassiveNode node : MegaPassiveTree.TREE.nodes()) {
            textures.put(node, RNGTech.id("textures/gui/mastery/" + node.masteryIconKey() + ".png"));
        }
        return Map.copyOf(textures);
    }

    public ForestryCartScreen(ForestryCartMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BASE_IMAGE_WIDTH;
        imageHeight = BASE_IMAGE_HEIGHT;
        inventoryLabelX = 39;
        inventoryLabelY = 104;
        masterySupport = new MasteryScreenSupport<>(
                ForestryCompanionPassiveTree.TREE,
                ForestryCompanionPassiveTree.TREE.nodes(),
                MegaPassiveTree.node(MachineMasteryFamily.FORESTRY.startNodeId()),
                MASTERY_ICON_TEXTURES,
                menu,
                new MasteryScreenSupport.Callbacks<>() {
                    @Override
                    public boolean gearAllowsUnlock(MegaPassiveNode node) {
                        return true;
                    }

                    @Override
                    public void appendSpecialTooltip(MegaPassiveNode node, List<Component> tooltip) {
                        if (node.behaviors().contains("MAGNET_MODE")) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.forestry_companion.magnet_mode")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                        if (node.behaviors().contains("SERRATED_LEAF_PROTOCOL")) {
                            tooltip.add(Component.translatable(
                                    "rngtech.mastery.tooltip.forestry_companion.serrated_leaf_protocol",
                                    menu.unshearedLeafFe()
                            ).withStyle(ChatFormatting.GOLD));
                        }
                        if (node.behaviors().contains("MANUAL_THROTTLE")) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.forestry_companion.manual_throttle")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                        if (node.behaviors().contains("COASTING_CLUTCH")) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.forestry_companion.coasting_clutch")
                                    .withStyle(ChatFormatting.GOLD));
                        }
                        if (node.behaviors().contains("SEEDLING_MAGNET")) {
                            tooltip.add(Component.translatable("rngtech.mastery.tooltip.forestry_companion.seedling_magnet")
                                    .withStyle(ChatFormatting.GOLD));
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
                        FORESTRY,
                        ENERGY,
                        0xFF38D857,
                        0xFF4E5F34
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
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
        renderSupplySlotHint(guiGraphics, mouseX, mouseY);
        renderValueTooltips(guiGraphics, mouseX, mouseY);
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY) {
            masterySupport.renderTooltips(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART) {
            renderCart(guiGraphics);
        } else if (menu.selectedTab() == ForestryCartMenu.TAB_STATS) {
            renderStats(guiGraphics);
        } else {
            masterySupport.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.battery_cell.short"), 51, 39, 30, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.modular_tool.short"), 81, 39, 30, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.shears.short"), 111, 39, 30, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.gear.fluid_pump.short"), 21, 39, 26, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.saplings"), 60, 72, 50, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.output"), 145, 72, 110, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.literal(Integer.toString(Math.max(1, menu.workRange()))),
                    RANGE_ICON_X + ICON_SIZE / 2, RANGE_ICON_Y + 2, ICON_SIZE - 2, 0xFFE0E0E0);
        } else if (menu.selectedTab() == ForestryCartMenu.TAB_STATS) {
            drawStatsLabels(guiGraphics);
        } else {
            masterySupport.drawLabels(guiGraphics, font, imageWidth);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                selectTab(ForestryCartMenu.TAB_CART);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                selectTab(ForestryCartMenu.TAB_STATS);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                selectTab(ForestryCartMenu.TAB_MASTERY);
                return true;
            }
            if (menu.selectedTab() == ForestryCartMenu.TAB_CART
                    && Screen.hasControlDown()
                    && inBounds(mouseX, mouseY, ACTION_ICON_X, ACTION_ICON_Y, ICON_SIZE, ICON_SIZE)) {
                sendButton(ForestryCartMenu.BUTTON_RELEASE_MANAGEMENT_VIEW);
                return true;
            }
            if (menu.selectedTab() == ForestryCartMenu.TAB_CART
                    && inBounds(mouseX, mouseY, SCAN_DEBUG_BUTTON_X, SCAN_DEBUG_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT)) {
                sendButton(ForestryCartMenu.BUTTON_TOGGLE_SCAN_DEBUG);
                return true;
            }
            if (menu.selectedTab() == ForestryCartMenu.TAB_CART
                    && inBounds(mouseX, mouseY, MANUAL_SPEED_BUTTON_X, MANUAL_SPEED_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT)) {
                sendButton(ForestryCartMenu.BUTTON_TOGGLE_MANUAL_SPEED);
                return true;
            }
            if (menu.selectedTab() == ForestryCartMenu.TAB_CART
                    && inBounds(mouseX, mouseY, RESET_CELLS_BUTTON_X, RESET_CELLS_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT)) {
                if (Screen.hasShiftDown()) {
                    sendButton(ForestryCartMenu.BUTTON_RESET_MANAGED_CELLS);
                }
                return true;
            }
        }
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY
                && masterySupport.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY
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
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY
                && masterySupport.mouseScrolled(mouseX, mouseY, scrollX, scrollY, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.cart"), menu.selectedTab() == ForestryCartMenu.TAB_CART);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ForestryCartMenu.TAB_STATS);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.mastery.short"), menu.selectedTab() == ForestryCartMenu.TAB_MASTERY);
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
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, label, x + TAB_WIDTH / 2, y + 7, TAB_WIDTH - 8, selected ? TEXT : 0xFF2F2F2F);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        if (menu.selectedTab() != ForestryCartMenu.TAB_CART) {
            return;
        }
        renderSlotFrame(guiGraphics, 42, 51);
        renderSlotFrame(guiGraphics, 72, 51);
        renderSlotFrame(guiGraphics, 102, 51);
        renderSlotFrame(guiGraphics, 12, 51);
        renderSlotFrame(guiGraphics, 42, 81);
        renderSlotFrame(guiGraphics, 60, 81);
        for (int slot = 0; slot < ForestryCartEntity.OUTPUT_SLOT_COUNT; slot++) {
            renderSlotFrame(guiGraphics, 91 + slot * 18, 81);
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderCart(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        if (menu.fertilizerCapacity() > 0 || menu.fertilizer() > 0) {
            int capacity = Math.max(menu.fertilizer(), menu.fertilizerCapacity());
            int filled = Math.round(FERTILIZER_BAR_WIDTH * (float) menu.fertilizer() / capacity);
            guiGraphics.fill(x + FERTILIZER_BAR_X, y + FERTILIZER_BAR_Y, x + FERTILIZER_BAR_X + FERTILIZER_BAR_WIDTH, y + FERTILIZER_BAR_Y + 2, 0xFF5F5F5F);
            guiGraphics.fill(x + FERTILIZER_BAR_X, y + FERTILIZER_BAR_Y, x + FERTILIZER_BAR_X + filled, y + FERTILIZER_BAR_Y + 2, FERTILIZER);
        }
        guiGraphics.fill(
                x + ENERGY_GAUGE_X,
                y + ENERGY_GAUGE_Y,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT,
                0xFF5F5F5F
        );
        int energyHeight = Math.round(48 * menu.cartEnergyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );
        if (menu.waterCapacity() > 0 || menu.water() > 0) {
            int capacity = Math.max(menu.water(), Math.max(1, menu.waterCapacity()));
            int waterHeight = Math.round((ENERGY_GAUGE_HEIGHT - 4) * (float) menu.water() / capacity);
            guiGraphics.fill(x + WATER_GAUGE_X, y + WATER_GAUGE_Y, x + WATER_GAUGE_X + WATER_GAUGE_WIDTH, y + WATER_GAUGE_Y + ENERGY_GAUGE_HEIGHT, 0xFF5F5F5F);
            guiGraphics.fill(
                    x + WATER_GAUGE_X + 2,
                    y + WATER_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - waterHeight,
                    x + WATER_GAUGE_X + WATER_GAUGE_WIDTH - 2,
                    y + WATER_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                    WATER
            );
        }
        renderIconBox(guiGraphics, RANGE_ICON_X, RANGE_ICON_Y);
        renderIconBox(guiGraphics, IDLE_ICON_X, IDLE_ICON_Y);
        guiGraphics.fill(x + IDLE_ICON_X + 3, y + IDLE_ICON_Y + 3, x + IDLE_ICON_X + 9, y + IDLE_ICON_Y + 9,
                menu.idleSpeedActive() ? 0xFF5F7DA8 : PANEL_DARK);
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(x + STATUS_ICON_X + 3, y + STATUS_ICON_Y + 3, x + STATUS_ICON_X + 9, y + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, ACTION_ICON_X, ACTION_ICON_Y);
        guiGraphics.fill(x + ACTION_ICON_X + 3, y + ACTION_ICON_Y + 3, x + ACTION_ICON_X + 9, y + ACTION_ICON_Y + 9, actionColor());
        renderButton(guiGraphics, SCAN_DEBUG_BUTTON_X, SCAN_DEBUG_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, menu.scanDebugVisible());
        MachineScreenStyle.drawClippedCentered(
                guiGraphics,
                font,
                Component.translatable("rngtech.forestry_station.control.scan_debug"),
                leftPos + SCAN_DEBUG_BUTTON_X + CONTROL_BUTTON_WIDTH / 2,
                topPos + SCAN_DEBUG_BUTTON_Y + 3,
                CONTROL_BUTTON_WIDTH - 4,
                menu.scanDebugVisible() ? TEXT : TEXT_MUTED
        );
        renderButton(guiGraphics, MANUAL_SPEED_BUTTON_X, MANUAL_SPEED_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, menu.manualSpeedEnabled());
        MachineScreenStyle.drawClippedCentered(
                guiGraphics,
                font,
                Component.translatable("rngtech.forestry_station.control.manual_speed"),
                leftPos + MANUAL_SPEED_BUTTON_X + CONTROL_BUTTON_WIDTH / 2,
                topPos + MANUAL_SPEED_BUTTON_Y + 3,
                CONTROL_BUTTON_WIDTH - 4,
                menu.manualSpeedUnlocked() ? menu.manualSpeedEnabled() ? TEXT : TEXT_MUTED : 0xFF707070
        );
        renderButton(guiGraphics, RESET_CELLS_BUTTON_X, RESET_CELLS_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, false);
        MachineScreenStyle.drawClippedCentered(
                guiGraphics,
                font,
                Component.translatable("rngtech.forestry_station.control.reset_cells"),
                leftPos + RESET_CELLS_BUTTON_X + CONTROL_BUTTON_WIDTH / 2,
                topPos + RESET_CELLS_BUTTON_Y + 3,
                CONTROL_BUTTON_WIDTH - 4,
                menu.managedCells() > 0 ? WARNING : 0xFF707070
        );
    }

    private void renderStats(GuiGraphics guiGraphics) {
        MachineScreenStyle.renderStatPanel(guiGraphics, leftPos, topPos, 8, 18, 224, statLines().length, FORESTRY);
    }

    private void drawStatsLabels(GuiGraphics guiGraphics) {
        MachineScreenStyle.drawStatPanelLabels(
                guiGraphics,
                font,
                Component.translatable("rngtech.tab.stats"),
                statLines(),
                8,
                18,
                224,
                FORESTRY
        );
    }

    private MachineScreenStyle.StatLine[] statLines() {
        return MachineScreenStyle.fitStatLines(
                MachineScreenStyle.withAscendancyStats(cartStatLines(), menu.ascendancyStats()),
                MachineScreenStyle.maxStatRows(18, BASE_IMAGE_HEIGHT)
        );
    }

    private MachineScreenStyle.StatLine[] cartStatLines() {
        return new MachineScreenStyle.StatLine[] {
                stat(
                        "rngtech.forestry_cart.stat.cart_energy",
                        CompactValueText.energyAmountPair(menu.cartEnergy(), menu.cartEnergyCapacity()),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.cart_energy",
                                CompactValueText.exactEnergyAmountPair(menu.cartEnergy(), menu.cartEnergyCapacity())
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.core_stats",
                        coreStatsValue(),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.core_stats",
                                menu.coreControl(),
                                menu.coreDrive(),
                                menu.coreReserve()
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.managed_cells",
                        "%s / %s / %s".formatted(menu.activeCells(), menu.managedCells(), menu.maxManagedCells()),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.managed_cells",
                                menu.activeCells(),
                                menu.managedCells(),
                                menu.maxManagedCells()
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.sapling_cargo",
                        countPair(menu.saplingCargo(), menu.saplingCargoCapacity()),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.sapling_cargo",
                                menu.saplingCargo(),
                                menu.saplingCargoCapacity()
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.output_cargo",
                        countPair(menu.outputCargo(), menu.outputCargoCapacity()),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.output_cargo",
                                menu.outputCargo(),
                                menu.outputCargoCapacity()
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.tree_log_limit",
                        Integer.toString(menu.maxConnectedLogs()),
                        Component.translatable("rngtech.forestry_cart.stat_tooltip.tree_log_limit")
                ),
                stat(
                        "rngtech.forestry_cart.stat.logs_per_action",
                        Integer.toString(menu.logsPerAction()),
                        Component.translatable("rngtech.forestry_cart.stat_tooltip.logs_per_action")
                ),
                stat(
                        "rngtech.forestry_cart.stat.height_limit",
                        Integer.toString(menu.maxTreeHeight()),
                        Component.translatable("rngtech.forestry_cart.stat_tooltip.height_limit")
                ),
                stat(
                        "rngtech.forestry_cart.stat.reserved_plantables",
                        Integer.toString(menu.reservedPlantables()),
                        Component.translatable("rngtech.forestry_cart.stat_tooltip.reserved_plantables", menu.reservedPlantables())
                ),
                stat(
                        "rngtech.forestry_cart.stat.action_interval",
                        "%st / %st".formatted(menu.workInterval(), menu.shearInterval()),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.action_interval",
                                menu.workInterval(),
                                menu.shearInterval(),
                                menu.workCooldown()
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.travel_speed",
                        "%s / %s / %s".formatted(
                                speedValue(menu.poweredSpeedMilli()),
                                speedValue(menu.transferSeekingSpeedMilli()),
                                speedValue(menu.unpoweredSpeedMilli())
                        ),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.travel_speed",
                                speedValue(menu.poweredSpeedMilli()),
                                speedValue(menu.transferSeekingSpeedMilli()),
                                speedValue(menu.unpoweredSpeedMilli())
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.energy_costs",
                        "%s/t %s/%s/%s".formatted(
                                menu.movementFe(),
                                menu.scanFe(),
                                menu.plantFe(),
                                CompactValueText.energyAmount(menu.cutFe())
                        ),
                        Component.translatable(
                                "rngtech.forestry_cart.stat_tooltip.energy_costs",
                                CompactValueText.energyRate(menu.movementFe()),
                                CompactValueText.energyAmount(menu.scanFe()),
                                CompactValueText.energyAmount(menu.plantFe()),
                                CompactValueText.energyAmount(menu.cutFe())
                        )
                ),
                stat(
                        "rngtech.forestry_cart.stat.tool_condition",
                        toolConditionValue(),
                        toolConditionTooltip()
                ),
                stat(
                        "rngtech.forestry_cart.stat.leaf_collection",
                        leafCollectionValue(),
                        leafCollectionTooltip()
                )
        };
    }

    private MachineScreenStyle.StatLine stat(String labelKey, String value, Component tooltip) {
        return MachineScreenStyle.statLine(Component.translatable(labelKey), value, 0.0D, false, false, tooltip);
    }

    private String countPair(int count, int capacity) {
        return "%s / %s".formatted(count, capacity);
    }

    private String coreStatsValue() {
        return "C%s D%s R%s".formatted(menu.coreControl(), menu.coreDrive(), menu.coreReserve());
    }

    private String speedValue(int milliBlocksPerTick) {
        return String.format(Locale.ROOT, "%.3f", milliBlocksPerTick / 1000.0D);
    }

    private String toolConditionValue() {
        return switch (menu.toolCondition()) {
            case ForestryCartEntity.GEAR_CONDITION_READY -> countPair(menu.toolDurability(), menu.toolMaxDurability());
            case ForestryCartEntity.GEAR_CONDITION_BROKEN -> Component.translatable("rngtech.forestry_cart.gear_condition.broken").getString();
            case ForestryCartEntity.GEAR_CONDITION_INVALID -> Component.translatable("rngtech.forestry_cart.gear_condition.invalid").getString();
            default -> Component.translatable("rngtech.forestry_cart.gear_condition.missing").getString();
        };
    }

    private String leafCollectionValue() {
        if (!menu.leafCollectionEnabled()) {
            return Component.translatable("rngtech.forestry_cart.stat.disabled").getString();
        }
        if (menu.shearsCondition() != ForestryCartEntity.GEAR_CONDITION_READY) {
            return Component.translatable("rngtech.forestry_cart.stat.enabled_no_shears").getString();
        }
        return Component.translatable("rngtech.forestry_cart.stat.enabled_short", menu.shearsLeafBudget()).getString();
    }

    private Component toolConditionTooltip() {
        if (menu.toolMaxDurability() <= 0) {
            return Component.translatable(
                    "rngtech.forestry_cart.stat_tooltip.tool_condition_status",
                    gearConditionText(menu.toolCondition())
            );
        }
        return Component.translatable(
                "rngtech.forestry_cart.stat_tooltip.tool_condition",
                gearConditionText(menu.toolCondition()),
                menu.toolDurability(),
                menu.toolMaxDurability()
        );
    }

    private Component leafCollectionTooltip() {
        if (!menu.leafCollectionEnabled()) {
            return Component.translatable(
                    "rngtech.forestry_cart.stat_tooltip.leaf_collection_disabled",
                    gearConditionText(menu.shearsCondition())
            );
        }
        if (menu.shearsCondition() != ForestryCartEntity.GEAR_CONDITION_READY) {
            return Component.translatable(
                    "rngtech.forestry_cart.stat_tooltip.leaf_collection_mastery",
                    CompactValueText.energyAmount(menu.unshearedLeafFe())
            );
        }
        if (menu.shearsMaxDurability() <= 0) {
            return Component.translatable(
                    "rngtech.forestry_cart.stat_tooltip.leaf_collection_unbreakable",
                    menu.shearsLeafBudget()
            );
        }
        return Component.translatable(
                "rngtech.forestry_cart.stat_tooltip.leaf_collection",
                menu.shearsDurability(),
                menu.shearsMaxDurability(),
                menu.shearsLeafBudget()
        );
    }

    private Component gearConditionText(int condition) {
        return switch (condition) {
            case ForestryCartEntity.GEAR_CONDITION_READY -> Component.translatable("rngtech.forestry_cart.gear_condition.ready");
            case ForestryCartEntity.GEAR_CONDITION_BROKEN -> Component.translatable("rngtech.forestry_cart.gear_condition.broken");
            case ForestryCartEntity.GEAR_CONDITION_INVALID -> Component.translatable("rngtech.forestry_cart.gear_condition.invalid");
            default -> Component.translatable("rngtech.forestry_cart.gear_condition.missing");
        };
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_MISSING_BATTERY_CELL -> Component.translatable("rngtech.forestry_station.status.missing_battery_cell");
            case ForestryCartStationBlockEntity.STATUS_MISSING_TOOL -> Component.translatable("rngtech.forestry_station.status.missing_tool");
            case ForestryCartStationBlockEntity.STATUS_INVALID_TOOL -> Component.translatable("rngtech.forestry_station.status.invalid_tool");
            case ForestryCartStationBlockEntity.STATUS_BROKEN_TOOL -> Component.translatable("rngtech.forestry_station.status.broken_tool");
            case ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS -> Component.translatable("rngtech.forestry_station.status.no_saplings");
            case ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("rngtech.forestry_station.status.output_full");
            case ForestryCartStationBlockEntity.STATUS_NO_POWER -> Component.translatable("rngtech.forestry_station.status.no_power");
            case ForestryCartStationBlockEntity.STATUS_ROUTE_BLOCKED -> Component.translatable("rngtech.forestry_station.status.route_blocked");
            case ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED -> Component.translatable("rngtech.forestry_station.status.path_blocked");
            case ForestryCartStationBlockEntity.STATUS_CART_OFF_RAIL -> Component.translatable("rngtech.forestry_station.status.cart_off_rail");
            case ForestryCartStationBlockEntity.STATUS_TREE_TOO_LARGE -> Component.translatable("rngtech.forestry_station.status.tree_too_large");
            case ForestryCartStationBlockEntity.STATUS_INVALID_SOIL -> Component.translatable("rngtech.forestry_station.status.invalid_soil");
            case ForestryCartStationBlockEntity.STATUS_PLANTING_BLOCKED -> Component.translatable("rngtech.forestry_station.status.planting_blocked");
            case ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED -> Component.translatable("rngtech.forestry_station.status.harvest_blocked");
            case ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> Component.translatable("rngtech.forestry_station.status.holding_cart");
            default -> Component.translatable("rngtech.forestry_station.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_READY -> FORESTRY;
            case ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS,
                    ForestryCartStationBlockEntity.STATUS_MISSING_BATTERY_CELL,
                    ForestryCartStationBlockEntity.STATUS_MISSING_TOOL,
                    ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> PANEL_DARK;
            default -> WARNING;
        };
    }

    private Component actionComponent() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER -> Component.translatable("rngtech.forestry_station.action.dock_transfer");
            case ForestryCartStationBlockEntity.ACTION_HOLDING -> Component.translatable("rngtech.forestry_station.action.holding");
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> Component.translatable("rngtech.forestry_station.action.setup_blocked");
            case ForestryCartStationBlockEntity.ACTION_WAITING_COOLDOWN -> Component.translatable("rngtech.forestry_station.action.waiting_cooldown");
            case ForestryCartStationBlockEntity.ACTION_SNAPSHOT_OUT_OF_RANGE -> Component.translatable("rngtech.forestry_station.action.snapshot_out_of_range");
            case ForestryCartStationBlockEntity.ACTION_SCANNING_LOG_BASES -> Component.translatable("rngtech.forestry_station.action.scanning_log_bases");
            case ForestryCartStationBlockEntity.ACTION_CREATING_SNAPSHOT -> Component.translatable("rngtech.forestry_station.action.creating_snapshot");
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_LEAVES -> Component.translatable("rngtech.forestry_station.action.harvesting_leaves");
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_CROP -> Component.translatable("rngtech.forestry_station.action.harvesting_crop");
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_LOG -> Component.translatable("rngtech.forestry_station.action.harvesting_log");
            case ForestryCartStationBlockEntity.ACTION_TREEFELLER_BATCH -> Component.translatable("rngtech.forestry_station.action.treefeller_batch");
            case ForestryCartStationBlockEntity.ACTION_PLANTING -> Component.translatable("rngtech.forestry_station.action.planting");
            case ForestryCartStationBlockEntity.ACTION_NO_SAPLINGS -> Component.translatable("rngtech.forestry_station.action.no_saplings");
            case ForestryCartStationBlockEntity.ACTION_MOVING -> Component.translatable("rngtech.forestry_station.action.moving");
            case ForestryCartStationBlockEntity.ACTION_NO_POWER -> Component.translatable("rngtech.forestry_station.action.no_power");
            case ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL -> Component.translatable("rngtech.forestry_station.action.output_full");
            case ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED -> Component.translatable("rngtech.forestry_station.action.path_blocked");
            case ForestryCartStationBlockEntity.ACTION_PLAYER_BLOCKING_PATH -> Component.translatable("rngtech.forestry_station.action.player_blocking_path");
            case ForestryCartStationBlockEntity.ACTION_CLEARING_PATH -> Component.translatable("rngtech.forestry_station.action.clearing_path");
            case ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED -> Component.translatable("rngtech.forestry_station.action.harvest_blocked");
            case ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED -> Component.translatable("rngtech.forestry_station.action.planting_blocked");
            case ForestryCartStationBlockEntity.ACTION_MANAGED -> Component.translatable("rngtech.forestry_cart.action.managed");
            case ForestryCartStationBlockEntity.ACTION_SEEKING_TRANSFER -> Component.translatable("rngtech.forestry_cart.action.seeking_transfer");
            default -> Component.translatable("rngtech.forestry_station.action.idle");
        };
    }

    private int actionColor() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_HARVESTING_LEAVES,
                    ForestryCartStationBlockEntity.ACTION_HARVESTING_CROP,
                    ForestryCartStationBlockEntity.ACTION_HARVESTING_LOG,
                    ForestryCartStationBlockEntity.ACTION_TREEFELLER_BATCH,
                    ForestryCartStationBlockEntity.ACTION_PLANTING,
                    ForestryCartStationBlockEntity.ACTION_CLEARING_PATH,
                    ForestryCartStationBlockEntity.ACTION_CREATING_SNAPSHOT,
                    ForestryCartStationBlockEntity.ACTION_SCANNING_LOG_BASES -> FORESTRY;
            case ForestryCartStationBlockEntity.ACTION_WAITING_COOLDOWN,
                    ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER,
                    ForestryCartStationBlockEntity.ACTION_MOVING,
                    ForestryCartStationBlockEntity.ACTION_MANAGED,
                    ForestryCartStationBlockEntity.ACTION_SEEKING_TRANSFER -> 0xFF5F7DA8;
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_NO_SAPLINGS,
                    ForestryCartStationBlockEntity.ACTION_NO_POWER,
                    ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL,
                    ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PLAYER_BLOCKING_PATH,
                    ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED -> WARNING;
            default -> PANEL_DARK;
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART && (menu.fertilizerCapacity() > 0 || menu.fertilizer() > 0)) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    FERTILIZER_BAR_X,
                    FERTILIZER_BAR_Y - 1,
                    FERTILIZER_BAR_WIDTH,
                    4,
                    Component.translatable("rngtech.forestry_station.tooltip.fertilizer", menu.fertilizer(), ForestryCartEntity.FERTILIZER_CAPACITY)
            );
        }
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART) {
            CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, RANGE_ICON_X, RANGE_ICON_Y, ICON_SIZE, ICON_SIZE,
                    Component.translatable("rngtech.forestry_cart.tooltip.work_range", Math.max(1, menu.workRange())));
            CompactValueText.renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, IDLE_ICON_X, IDLE_ICON_Y, ICON_SIZE, ICON_SIZE,
                    Component.translatable(menu.idleSpeedActive() ? "rngtech.forestry_cart.tooltip.idle_speed_on" : "rngtech.forestry_cart.tooltip.idle_speed_off"));
        }
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART && (menu.waterCapacity() > 0 || menu.water() > 0)) {
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
                    ENERGY_GAUGE_HEIGHT,
                    Component.translatable("rngtech.forestry_cart.tooltip.water", menu.water(), Math.max(menu.water(), menu.waterCapacity()))
            );
        }
        if (menu.selectedTab() == ForestryCartMenu.TAB_CART) {
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
                    Component.translatable(
                            "rngtech.forestry_station.tooltip.cart_energy",
                            CompactValueText.exactEnergyAmountPair(menu.cartEnergy(), menu.cartEnergyCapacity())
                    )
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
                    Component.translatable("rngtech.forestry_station.tooltip.status", statusComponent())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    ACTION_ICON_X,
                    ACTION_ICON_Y,
                    ICON_SIZE,
                    ICON_SIZE,
                    Component.translatable("rngtech.forestry_station.tooltip.action", actionComponent())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    SCAN_DEBUG_BUTTON_X,
                    SCAN_DEBUG_BUTTON_Y,
                    CONTROL_BUTTON_WIDTH,
                    CONTROL_BUTTON_HEIGHT,
                    Component.translatable(menu.scanDebugVisible()
                            ? "rngtech.forestry_station.tooltip.scan_debug_enabled"
                            : "rngtech.forestry_station.tooltip.scan_debug_disabled")
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    MANUAL_SPEED_BUTTON_X,
                    MANUAL_SPEED_BUTTON_Y,
                    CONTROL_BUTTON_WIDTH,
                    CONTROL_BUTTON_HEIGHT,
                    Component.translatable(manualSpeedTooltipKey())
            );
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    RESET_CELLS_BUTTON_X,
                    RESET_CELLS_BUTTON_Y,
                    CONTROL_BUTTON_WIDTH,
                    CONTROL_BUTTON_HEIGHT,
                    menu.managedCells() > 0
                            ? Component.translatable("rngtech.forestry_station.tooltip.reset_cells", menu.managedCells())
                            : Component.translatable("rngtech.forestry_station.tooltip.reset_cells_empty")
            );
        } else if (menu.selectedTab() == ForestryCartMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 8, 18, 224, statLines());
        }
    }

    private String manualSpeedTooltipKey() {
        if (!menu.manualSpeedUnlocked()) {
            return "rngtech.forestry_station.tooltip.manual_speed_locked";
        }
        return menu.manualSpeedEnabled()
                ? "rngtech.forestry_station.tooltip.manual_speed_enabled"
                : "rngtech.forestry_station.tooltip.manual_speed_disabled";
    }

    private void selectTab(int tab) {
        masterySupport.resetDragging();
        menu.selectTab(tab);
        updateImageSizeForSelectedTab();
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY) {
            masterySupport.clampAfterGeometryChange(imageWidth, imageHeight);
        }
    }

    private void updateImageSizeForSelectedTab() {
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY && masterySupport.expanded()) {
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

    private void renderIconBox(GuiGraphics guiGraphics, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + ICON_SIZE - 1, top + ICON_SIZE - 1, 0xFF5F5F5F);
        guiGraphics.fill(left + 2, top + 2, left + ICON_SIZE - 2, top + ICON_SIZE - 2, 0xFF2F2F2F);
    }

    private void renderButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean selected) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + width, top + height, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, selected ? 0xFF8CBF8F : 0xFFB8B8B8);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + 2, selected ? 0xFFA9D3AC : PANEL_LIGHT);
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

    private boolean isOverTab(double mouseX, double mouseY, int index) {
        int x = leftPos + 8 + index * TAB_SPACING;
        int y = topPos - 20;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + 21;
    }

    private boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        int left = leftPos + x;
        int top = topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void sendButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY && masterySupport.keyPressed(key, scan, modifiers, imageWidth, imageHeight)) { return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean charTyped(char character, int modifiers) {
        if (menu.selectedTab() == ForestryCartMenu.TAB_MASTERY && masterySupport.charTyped(character)) { return true; }
        return super.charTyped(character, modifiers);
    }


    /** Hovering an empty supply slot says what this cart plants. */
    private void renderSupplySlotHint(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = hoveredSlot;
        if (menu.selectedTab() != ForestryCartMenu.TAB_CART || slot == null || slot.hasItem() || !(slot instanceof SlotItemHandler)
                || menu.isGearSlot(slot) || slot.getSlotIndex() >= ForestryCartEntity.OUTPUT_SLOT_START) {
            return;
        }
        guiGraphics.renderComponentTooltip(font, List.of(
                Component.translatable("rngtech.forestry_cart.slot.supply").withStyle(ChatFormatting.YELLOW),
                Component.translatable("rngtech.forestry_cart.slot.supply.hint").withStyle(ChatFormatting.GRAY)
        ), mouseX, mouseY);
    }

}
