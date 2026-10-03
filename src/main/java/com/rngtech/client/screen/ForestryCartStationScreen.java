package com.rngtech.client.screen;

import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.menu.ForestryCartStationMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class ForestryCartStationScreen extends AbstractContainerScreen<ForestryCartStationMenu> {
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
    private static final int ENERGY_GAUGE_X = 212;
    private static final int ENERGY_GAUGE_Y = 30;
    private static final int ENERGY_GAUGE_WIDTH = 12;
    private static final int ENERGY_GAUGE_HEIGHT = 52;
    private static final int WATER_GAUGE_X = 198;
    private static final int WATER_GAUGE_WIDTH = 8;
    private static final int WATER = 0xFF3F76E4;
    private static final int GROUP_LABEL_Y = 21;
    private static final int STATUS_ICON_X = 20;
    private static final int STATUS_ICON_Y = 72;
    private static final int ACTION_ICON_X = 36;
    private static final int ACTION_ICON_Y = 72;
    private static final int HOLD_BUTTON_X = 56;
    private static final int HOLD_BUTTON_Y = 72;
    private static final int CONTROL_BUTTON_WIDTH = 30;
    private static final int CONTROL_BUTTON_HEIGHT = 12;
    private static final int ICON_SIZE = 12;
    private static final int FERTILIZER_BAR_X = ForestryCartStationMenu.FERTILIZER_X;
    private static final int FERTILIZER_BAR_Y = ForestryCartStationMenu.SUPPLY_ROW_Y + 19;
    private static final int FERTILIZER_BAR_WIDTH = 16;
    private static final int GHOST_OVERLAY = 0x998B8B8B;
    private static final int FERTILIZER = 0xFFE3DCC4;

    public ForestryCartStationScreen(ForestryCartStationMenu menu, Inventory playerInventory, Component title) {
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
        renderEmptySlotHints(guiGraphics, mouseX, mouseY);
        GearSlotTooltips.render(this, guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(guiGraphics);
        renderTabs(guiGraphics);
        renderSlotFrames(guiGraphics);
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            renderProcessing(guiGraphics);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_SUPPLY) {
            renderSupply(guiGraphics);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            renderGear(guiGraphics);
        } else {
            renderStats(guiGraphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        if (menu.selectedTab() != ForestryCartStationMenu.TAB_STATS) {
            guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        }
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            drawGroupLabel(guiGraphics, "rngtech.forestry_station.output", ForestryCartStationMenu.OUTPUT_X, 3);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_SUPPLY) {
            drawGroupLabel(guiGraphics, "rngtech.forestry_station.group.plantables", ForestryCartStationMenu.PLANTABLES_X, ForestryCartStationMenu.PLANTABLES_COLUMNS);
            drawGroupLabel(guiGraphics, "rngtech.forestry_station.group.refills", ForestryCartStationMenu.FERTILIZER_X, 3);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.station_gear"), 119, 22, 120, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.station_cell.short"), 80, 40, 22, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.energy_port.short"), 104, 40, 22, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.item_port.short"), 128, 40, 22, TEXT_MUTED);
            MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable("rngtech.forestry_station.fluid_port.short"), 152, 40, 22, TEXT_MUTED);
        } else {
            drawStatsLabels(guiGraphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOverTab(mouseX, mouseY, 0)) {
                menu.selectTab(ForestryCartStationMenu.TAB_PROCESSING);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 1)) {
                menu.selectTab(ForestryCartStationMenu.TAB_SUPPLY);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 2)) {
                menu.selectTab(ForestryCartStationMenu.TAB_GEAR);
                return true;
            }
            if (isOverTab(mouseX, mouseY, 3)) {
                menu.selectTab(ForestryCartStationMenu.TAB_STATS);
                return true;
            }
            if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING
                    && inBounds(mouseX, mouseY, HOLD_BUTTON_X, HOLD_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT)) {
                sendButton(ForestryCartStationMenu.BUTTON_TOGGLE_HOLD);
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
        renderTab(guiGraphics, 0, Component.translatable("rngtech.tab.station"), menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING);
        renderTab(guiGraphics, 1, Component.translatable("rngtech.tab.supply"), menu.selectedTab() == ForestryCartStationMenu.TAB_SUPPLY);
        renderTab(guiGraphics, 2, Component.translatable("rngtech.tab.gear"), menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR);
        renderTab(guiGraphics, 3, Component.translatable("rngtech.tab.stats"), menu.selectedTab() == ForestryCartStationMenu.TAB_STATS);
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

    /** Short group labels sit centered above their slots, clipped to the group's width so they never overlap. */
    private void drawGroupLabel(GuiGraphics guiGraphics, String key, int firstSlotX, int columns) {
        int width = columns * 18 + (columns > 2 ? 0 : 2);
        MachineScreenStyle.drawClippedCentered(guiGraphics, font, Component.translatable(key), firstSlotX - 1 + width / 2, GROUP_LABEL_Y, width, TEXT_MUTED);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        int row = ForestryCartStationMenu.SUPPLY_ROW_Y - 1;
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            for (int slot = 0; slot < ForestryCartStationBlockEntity.OUTPUT_SLOT_COUNT; slot++) {
                renderSlotFrame(guiGraphics, ForestryCartStationMenu.OUTPUT_X - 1 + (slot % 3) * 18, row + (slot / 3) * 18);
            }
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_SUPPLY) {
            int columns = ForestryCartStationMenu.PLANTABLES_COLUMNS;
            for (int slot = 0; slot <= ForestryCartStationBlockEntity.PLANTABLES_EXTRA_COUNT; slot++) {
                renderSlotFrame(guiGraphics, ForestryCartStationMenu.PLANTABLES_X - 1 + (slot % columns) * 18, row + (slot / columns) * 18);
            }
            renderSlotFrame(guiGraphics, ForestryCartStationMenu.FERTILIZER_X - 1, row);
            renderSlotFrame(guiGraphics, ForestryCartStationMenu.TOOL_X - 1, row);
            renderSlotFrame(guiGraphics, ForestryCartStationMenu.SHEARS_X - 1, row);
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_GEAR) {
            renderSlotFrame(guiGraphics, 70, 51);
            renderSlotFrame(guiGraphics, 94, 51);
            renderSlotFrame(guiGraphics, 118, 51);
            renderSlotFrame(guiGraphics, 142, 51);
        }

        if (menu.selectedTab() == ForestryCartStationMenu.TAB_STATS) {
            return;
        }
        renderPlayerInventoryFrames(guiGraphics);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x + ENERGY_GAUGE_X, y + ENERGY_GAUGE_Y, x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH, y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT, 0xFF5F5F5F);
        int energyHeight = Math.round(48 * menu.energyProgress());
        guiGraphics.fill(
                x + ENERGY_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - energyHeight,
                x + ENERGY_GAUGE_X + ENERGY_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                ENERGY
        );
        guiGraphics.fill(x + WATER_GAUGE_X, y + ENERGY_GAUGE_Y, x + WATER_GAUGE_X + WATER_GAUGE_WIDTH, y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT, 0xFF5F5F5F);
        int waterHeight = Math.round((ENERGY_GAUGE_HEIGHT - 4) * (float) menu.water() / ForestryCartStationBlockEntity.WATER_CAPACITY);
        guiGraphics.fill(
                x + WATER_GAUGE_X + 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2 - waterHeight,
                x + WATER_GAUGE_X + WATER_GAUGE_WIDTH - 2,
                y + ENERGY_GAUGE_Y + ENERGY_GAUGE_HEIGHT - 2,
                WATER
        );
        renderStatusIcons(guiGraphics);
        renderControlButtons(guiGraphics);
    }

    private void renderSupply(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        int filled = Math.round(FERTILIZER_BAR_WIDTH * (float) menu.fertilizer() / ForestryCartEntity.FERTILIZER_CAPACITY);
        guiGraphics.fill(x + FERTILIZER_BAR_X, y + FERTILIZER_BAR_Y, x + FERTILIZER_BAR_X + FERTILIZER_BAR_WIDTH, y + FERTILIZER_BAR_Y + 2, 0xFF5F5F5F);
        guiGraphics.fill(x + FERTILIZER_BAR_X, y + FERTILIZER_BAR_Y, x + FERTILIZER_BAR_X + filled, y + FERTILIZER_BAR_Y + 2, FERTILIZER);
        renderGhostItems(guiGraphics);
    }

    private void renderGear(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, 113, 77);
        int x = leftPos + 113;
        int y = topPos + 77;
        guiGraphics.fill(x + 3, y + 8, x + 9, y + 10, FORESTRY);
        guiGraphics.fill(x + 4, y + 5, x + 8, y + 7, FORESTRY);
        guiGraphics.fill(x + 5, y + 2, x + 7, y + 4, FORESTRY);
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
        List<MachineScreenStyle.StatLine> lines = new java.util.ArrayList<>(List.of(
                stat("rngtech.forestry_station.stat.energy_transfer", CompactValueText.energyAmount(menu.energyTransfer())),
                stat("rngtech.forestry_station.stat.item_transfer", Integer.toString(menu.itemTransfer())),
                stat("rngtech.forestry_station.stat.fluid_transfer", menu.fluidTransfer() + " mB"),
                stat("rngtech.forestry_station.stat.water", menu.water() + " / " + ForestryCartStationBlockEntity.WATER_CAPACITY + " mB"),
                stat("rngtech.forestry_station.stat.docked_cart", menu.cartDocked()
                        ? Component.translatable("rngtech.forestry_station.cart_status.docked").getString()
                        : Component.translatable("rngtech.forestry_station.cart_status.none").getString())
        ));
        if (menu.cartDocked()) {
            lines.add(stat("rngtech.forestry_station.stat.cart_energy", menu.cartEnergyPercent() + "%"));
            lines.add(stat("rngtech.forestry_station.stat.cart_work_range", Integer.toString(menu.cartWorkRange())));
            if (menu.cartWaitingCells() > 0) {
                lines.add(stat("rngtech.forestry_station.stat.cart_waiting_cells", Integer.toString(menu.cartWaitingCells())));
            }
            if (menu.cartTendsCrops()) {
                lines.add(stat("rngtech.forestry_station.stat.cart_water", menu.cartWater() + " mB"));
            } else {
                lines.add(stat("rngtech.forestry_station.stat.cart_tool", toolConditionText(menu.cartToolCondition())));
            }
        }
        return lines.toArray(MachineScreenStyle.StatLine[]::new);
    }

    private static String toolConditionText(int condition) {
        return Component.translatable(switch (condition) {
            case ForestryCartEntity.GEAR_CONDITION_READY -> "rngtech.forestry_cart.gear_condition.ready";
            case ForestryCartEntity.GEAR_CONDITION_BROKEN -> "rngtech.forestry_cart.gear_condition.broken";
            case ForestryCartEntity.GEAR_CONDITION_INVALID -> "rngtech.forestry_cart.gear_condition.invalid";
            default -> "rngtech.forestry_cart.gear_condition.missing";
        }).getString();
    }

    /** Faded examples in empty supply slots show what each slot takes. */
    private void renderGhostItems(GuiGraphics guiGraphics) {
        for (Slot slot : menu.slots) {
            ItemStack ghost = ghostItem(slot);
            if (ghost.isEmpty() || slot.hasItem() || !slot.isActive()) {
                continue;
            }
            int left = leftPos + slot.x;
            int top = topPos + slot.y;
            guiGraphics.renderFakeItem(ghost, left, top);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
            guiGraphics.fill(left, top, left + 16, top + 16, GHOST_OVERLAY);
            guiGraphics.pose().popPose();
        }
    }

    private ItemStack ghostItem(Slot slot) {
        if (!(slot instanceof net.neoforged.neoforge.items.SlotItemHandler handlerSlot)
                || handlerSlot.getItemHandler() != menu.processInventory()) {
            return ItemStack.EMPTY;
        }
        return switch (slot.getSlotIndex()) {
            case ForestryCartStationBlockEntity.SLOT_SAPLING -> new ItemStack(menu.cartTendsCrops() ? Items.WHEAT_SEEDS : Items.OAK_SAPLING);
            case ForestryCartStationBlockEntity.SLOT_PLANTABLES_EXTRA_START -> ItemStack.EMPTY;
            case ForestryCartStationBlockEntity.SLOT_FERTILIZER_INPUT -> new ItemStack(Items.BONE_MEAL);
            case ForestryCartStationBlockEntity.SLOT_TOOL_INPUT -> new ItemStack(Items.IRON_AXE);
            case ForestryCartStationBlockEntity.SLOT_SHEARS_INPUT -> new ItemStack(Items.SHEARS);
            default -> ItemStack.EMPTY;
        };
    }

    /** Hovering an empty supply slot names it and says what it does. */
    private void renderEmptySlotHints(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = hoveredSlot;
        if (slot == null || slot.hasItem() || menu.selectedTab() != ForestryCartStationMenu.TAB_SUPPLY
                || !(slot instanceof net.neoforged.neoforge.items.SlotItemHandler handlerSlot) || handlerSlot.getItemHandler() != menu.processInventory()) {
            return;
        }
        String key = ForestryCartStationBlockEntity.isPlantablesSlot(slot.getSlotIndex()) ? "rngtech.forestry_station.slot.plantables" : switch (slot.getSlotIndex()) {
            case ForestryCartStationBlockEntity.SLOT_FERTILIZER_INPUT -> "rngtech.forestry_station.slot.bone_meal";
            case ForestryCartStationBlockEntity.SLOT_TOOL_INPUT -> "rngtech.forestry_station.slot.tool";
            default -> "rngtech.forestry_station.slot.shears";
        };
        guiGraphics.renderComponentTooltip(font, List.of(
                Component.translatable(key).withStyle(ChatFormatting.YELLOW),
                Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY)
        ), mouseX, mouseY);
    }

    private MachineScreenStyle.StatLine stat(String labelKey, String value) {
        return MachineScreenStyle.statLine(Component.translatable(labelKey), value, 0.0D, false, false, Component.translatable(labelKey));
    }

    private Component statusComponent() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_MISSING_RAIL -> Component.translatable("rngtech.forestry_station.status.missing_rail");
            case ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> Component.translatable("rngtech.forestry_station.status.holding_cart");
            default -> Component.translatable("rngtech.forestry_station.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.statusCode()) {
            case ForestryCartStationBlockEntity.STATUS_READY -> FORESTRY;
            case ForestryCartStationBlockEntity.STATUS_HOLDING_CART -> PANEL_DARK;
            default -> WARNING;
        };
    }

    private Component actionComponent() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER -> Component.translatable("rngtech.forestry_station.action.dock_transfer");
            case ForestryCartStationBlockEntity.ACTION_HOLDING -> Component.translatable("rngtech.forestry_station.action.holding");
            case ForestryCartStationBlockEntity.ACTION_DOCKED_READY -> Component.translatable("rngtech.forestry_station.action.docked_ready");
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> Component.translatable("rngtech.forestry_station.action.setup_blocked");
            default -> Component.translatable("rngtech.forestry_station.action.idle");
        };
    }

    private int actionColor() {
        return switch (menu.currentAction()) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER,
                    ForestryCartStationBlockEntity.ACTION_DOCKED_READY -> 0xFF5F7DA8;
            case ForestryCartStationBlockEntity.ACTION_HOLDING -> PANEL_DARK;
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED -> WARNING;
            default -> PANEL_DARK;
        };
    }

    private void renderValueTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
            CompactValueText.renderTooltipIfHovered(
                    guiGraphics,
                    font,
                    leftPos,
                    topPos,
                    mouseX,
                    mouseY,
                    WATER_GAUGE_X,
                    ENERGY_GAUGE_Y,
                    WATER_GAUGE_WIDTH,
                    ENERGY_GAUGE_HEIGHT,
                    Component.translatable("rngtech.forestry_station.tooltip.water", menu.water(), ForestryCartStationBlockEntity.WATER_CAPACITY)
            );
        }
        if (menu.selectedTab() == ForestryCartStationMenu.TAB_PROCESSING) {
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
                    Component.translatable("rngtech.forestry_station.tooltip.energy", CompactValueText.exactEnergyAmountPair(menu.energy(), menu.energyCapacity()))
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
                    HOLD_BUTTON_X,
                    HOLD_BUTTON_Y,
                    CONTROL_BUTTON_WIDTH,
                    CONTROL_BUTTON_HEIGHT,
                    Component.translatable(menu.holdCartAtStation()
                            ? "rngtech.forestry_station.tooltip.hold_enabled"
                            : "rngtech.forestry_station.tooltip.hold_disabled")
            );
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_SUPPLY) {
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
        } else if (menu.selectedTab() == ForestryCartStationMenu.TAB_STATS) {
            MachineScreenStyle.renderStatPanelTooltip(guiGraphics, font, leftPos, topPos, mouseX, mouseY, 8, 18, 224, statLines());
        }
    }

    private void renderStatusIcons(GuiGraphics guiGraphics) {
        renderIconBox(guiGraphics, STATUS_ICON_X, STATUS_ICON_Y);
        guiGraphics.fill(leftPos + STATUS_ICON_X + 3, topPos + STATUS_ICON_Y + 3, leftPos + STATUS_ICON_X + 9, topPos + STATUS_ICON_Y + 9, statusColor());
        renderIconBox(guiGraphics, ACTION_ICON_X, ACTION_ICON_Y);
        guiGraphics.fill(leftPos + ACTION_ICON_X + 3, topPos + ACTION_ICON_Y + 3, leftPos + ACTION_ICON_X + 9, topPos + ACTION_ICON_Y + 9, actionColor());
    }

    private void renderControlButtons(GuiGraphics guiGraphics) {
        renderButton(guiGraphics, HOLD_BUTTON_X, HOLD_BUTTON_Y, CONTROL_BUTTON_WIDTH, CONTROL_BUTTON_HEIGHT, menu.holdCartAtStation());
        MachineScreenStyle.drawClippedCentered(
                guiGraphics,
                font,
                Component.translatable(menu.holdCartAtStation()
                        ? "rngtech.forestry_station.control.hold_on"
                        : "rngtech.forestry_station.control.hold_off"),
                leftPos + HOLD_BUTTON_X + CONTROL_BUTTON_WIDTH / 2,
                topPos + HOLD_BUTTON_Y + 3,
                CONTROL_BUTTON_WIDTH - 4,
                menu.holdCartAtStation() ? TEXT : TEXT_MUTED
        );
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
}
