package com.rngtech.content.menu;

import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class ForestryCartStationMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_SUPPLY = 1;
    public static final int TAB_GEAR = 2;
    public static final int TAB_STATS = 3;
    public static final int BUTTON_TOGGLE_HOLD = 0;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_STATUS = 2;
    private static final int DATA_HOLD_CART = 3;
    private static final int DATA_CART_DOCKED = 4;
    private static final int DATA_ENERGY_TRANSFER = 5;
    private static final int DATA_ITEM_TRANSFER = 6;
    private static final int DATA_FLUID_TRANSFER = 7;
    private static final int DATA_CURRENT_ACTION = 8;
    private static final int DATA_FERTILIZER = 9;
    private static final int DATA_WATER = 10;
    private static final int DATA_CART_TOOL_CONDITION = 11;
    private static final int DATA_CART_ENERGY_PERCENT = 12;
    private static final int DATA_CART_WORK_RANGE = 13;
    private static final int DATA_CART_WATER = 14;
    private static final int DATA_CART_CROPS = 15;
    private static final int DATA_CART_WAITING_CELLS = 16;
    private static final int DATA_COUNT = 17;

    public static final int PLANTABLES_X = 20;
    public static final int FERTILIZER_X = 100;
    public static final int TOOL_X = 118;
    public static final int SHEARS_X = 136;
    public static final int OUTPUT_X = 20;
    public static final int SUPPLY_ROW_Y = 32;
    public static final int PLANTABLES_COLUMNS = 3;

    private static final int SAPLING_SLOT = 0;
    private static final int PLANTABLES_SLOT_END = SAPLING_SLOT + 1 + ForestryCartStationBlockEntity.PLANTABLES_EXTRA_COUNT;
    private static final int OUTPUT_SLOT_START = PLANTABLES_SLOT_END;
    private static final int SHEARS_INPUT_SLOT = OUTPUT_SLOT_START + ForestryCartStationBlockEntity.OUTPUT_SLOT_COUNT;
    private static final int FERTILIZER_INPUT_SLOT = SHEARS_INPUT_SLOT + 1;
    private static final int TOOL_INPUT_SLOT = FERTILIZER_INPUT_SLOT + 1;
    private static final int STATION_BATTERY_SLOT = TOOL_INPUT_SLOT + 1;
    private static final int ENERGY_CONNECTOR_SLOT = STATION_BATTERY_SLOT + 1;
    private static final int ITEM_CONNECTOR_SLOT = ENERGY_CONNECTOR_SLOT + 1;
    private static final int FLUID_CONNECTOR_SLOT = ITEM_CONNECTOR_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = FLUID_CONNECTOR_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ForestryCartStationBlockEntity station;
    private int selectedTab = TAB_PROCESSING;

    public ForestryCartStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public ForestryCartStationMenu(
            int containerId,
            Inventory playerInventory,
            ForestryCartStationBlockEntity station,
            ContainerData data
    ) {
        super(ModMenus.FORESTRY_CART_STATION.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());
        this.data = data;
        this.station = station;

        ItemStackHandler processInventory = station.getProcessInventory();
        BooleanSupplier processing = () -> selectedTab == TAB_PROCESSING;
        BooleanSupplier supply = () -> selectedTab == TAB_SUPPLY;
        for (int index = 0; index <= ForestryCartStationBlockEntity.PLANTABLES_EXTRA_COUNT; index++) {
            int handlerSlot = index == 0 ? ForestryCartStationBlockEntity.SLOT_SAPLING : ForestryCartStationBlockEntity.SLOT_PLANTABLES_EXTRA_START + index - 1;
            addSlot(new TabbedSlot(
                    processInventory,
                    handlerSlot,
                    PLANTABLES_X + (index % PLANTABLES_COLUMNS) * 18,
                    SUPPLY_ROW_Y + (index / PLANTABLES_COLUMNS) * 18,
                    supply
            ));
        }
        for (int slot = 0; slot < ForestryCartStationBlockEntity.OUTPUT_SLOT_COUNT; slot++) {
            addSlot(new TabbedSlot(
                    processInventory,
                    ForestryCartStationBlockEntity.SLOT_OUTPUT_START + slot,
                    OUTPUT_X + (slot % 3) * 18,
                    SUPPLY_ROW_Y + (slot / 3) * 18,
                    processing
            ));
        }
        addSlot(new TabbedSlot(processInventory, ForestryCartStationBlockEntity.SLOT_SHEARS_INPUT, SHEARS_X, SUPPLY_ROW_Y, supply));
        addSlot(new TabbedSlot(processInventory, ForestryCartStationBlockEntity.SLOT_FERTILIZER_INPUT, FERTILIZER_X, SUPPLY_ROW_Y, supply));
        addSlot(new TabbedSlot(processInventory, ForestryCartStationBlockEntity.SLOT_TOOL_INPUT, TOOL_X, SUPPLY_ROW_Y, supply));

        ItemStackHandler connectorInventory = station.getConnectorInventory();
        addSlot(new TabbedSlot(connectorInventory, ForestryCartStationBlockEntity.SLOT_STATION_BATTERY_CELL, 71, 52, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(connectorInventory, ForestryCartStationBlockEntity.SLOT_ENERGY_CONNECTOR, 95, 52, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(connectorInventory, ForestryCartStationBlockEntity.SLOT_ITEM_CONNECTOR, 119, 52, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(connectorInventory, ForestryCartStationBlockEntity.SLOT_FLUID_CONNECTOR, 143, 52, () -> selectedTab == TAB_GEAR));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_SUPPLY -> TAB_SUPPLY;
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public boolean holdCartAtStation() {
        return data.get(DATA_HOLD_CART) != 0;
    }

    public boolean cartDocked() {
        return data.get(DATA_CART_DOCKED) != 0;
    }

    public int currentAction() {
        return data.get(DATA_CURRENT_ACTION);
    }

    public int energyTransfer() {
        return data.get(DATA_ENERGY_TRANSFER);
    }

    public int itemTransfer() {
        return data.get(DATA_ITEM_TRANSFER);
    }

    public int fluidTransfer() {
        return data.get(DATA_FLUID_TRANSFER);
    }

    public int fertilizer() {
        return data.get(DATA_FERTILIZER);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    public ItemStackHandler processInventory() {
        return station.getProcessInventory();
    }

    public int cartToolCondition() {
        return data.get(DATA_CART_TOOL_CONDITION);
    }

    public int cartEnergyPercent() {
        return data.get(DATA_CART_ENERGY_PERCENT);
    }

    public int cartWorkRange() {
        return data.get(DATA_CART_WORK_RANGE);
    }

    public int cartWater() {
        return data.get(DATA_CART_WATER);
    }

    public boolean cartTendsCrops() {
        return data.get(DATA_CART_CROPS) != 0;
    }

    public int cartWaitingCells() {
        return data.get(DATA_CART_WAITING_CELLS);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return id == BUTTON_TOGGLE_HOLD;
        }
        return id == BUTTON_TOGGLE_HOLD && station.toggleHoldCartAtStation();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.FORESTRY_CART_STATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (station.isSaplingStack(stack)) {
            if (!moveItemStackTo(stack, SAPLING_SLOT, PLANTABLES_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isFertilizerStack(stack)) {
            if (!moveItemStackTo(stack, FERTILIZER_INPUT_SLOT, FERTILIZER_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isToolCandidate(stack)) {
            if (!moveItemStackTo(stack, TOOL_INPUT_SLOT, TOOL_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (station.isShearsStack(stack)) {
            if (!moveItemStackTo(stack, SHEARS_INPUT_SLOT, SHEARS_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (station.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, STATION_BATTERY_SLOT, STATION_BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof EnergyConnectorItem) {
            if (!moveItemStackTo(stack, ENERGY_CONNECTOR_SLOT, ENERGY_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemConnectorItem) {
            if (!moveItemStackTo(stack, ITEM_CONNECTOR_SLOT, ITEM_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidConnectorItem) {
            if (!moveItemStackTo(stack, FLUID_CONNECTOR_SLOT, FLUID_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        39 + column * 18,
                        116 + row * 18,
                        () -> selectedTab != TAB_STATS
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_STATS
            ));
        }
    }

    private static ForestryCartStationBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ForestryCartStationBlockEntity station) {
            return station;
        }
        throw new IllegalStateException("Expected forestry cart station block entity at " + pos);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(
                net.neoforged.neoforge.items.IItemHandler itemHandler,
                int index,
                int xPosition,
                int yPosition,
                BooleanSupplier activeSupplier
        ) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier activeSupplier;

        private TabbedInventorySlot(Inventory inventory, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(inventory, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }
}
