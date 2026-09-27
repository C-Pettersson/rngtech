package com.rngtech.content.menu;

import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
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

public class WoodenComposterMenu extends AbstractContainerMenu {
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_AVAILABLE_ITEMS = 2;
    private static final int DATA_WATER = 3;
    private static final int DATA_WATER_CAPACITY = 4;
    private static final int DATA_STATUS = 5;
    private static final int DATA_WET_BATCH = 6;
    private static final int DATA_VARIETY_COUNT = 7;
    private static final int DATA_SPEED_BONUS_PERCENT = 8;
    private static final int DATA_COUNT = 9;

    private static final int PLAYER_INVENTORY_START = WoodenComposterBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final WoodenComposterBlockEntity composter;

    public WoodenComposterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public WoodenComposterMenu(
            int containerId,
            Inventory playerInventory,
            WoodenComposterBlockEntity composter,
            ContainerData data
    ) {
        super(ModMenus.WOODEN_COMPOSTER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(composter.getLevel(), composter.getBlockPos());
        this.data = data;
        this.composter = composter;
        addDataSlots(data);

        ItemStackHandler inventory = composter.getInventory();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(inventory, column + row * 9, 8 + column * 18, 30 + row * 18));
            }
        }
        addSlot(new SlotItemHandler(inventory, WoodenComposterBlockEntity.SLOT_OUTPUT, 194, 48));
        addSlot(new SlotItemHandler(inventory, WoodenComposterBlockEntity.SLOT_WATER_INPUT_CONTAINER, 184, 76));
        addSlot(new SlotItemHandler(inventory, WoodenComposterBlockEntity.SLOT_WATER_OUTPUT_CONTAINER, 206, 76));

        addPlayerInventory(playerInventory);
    }

    public float compostProgress() {
        int required = requiredTicks();
        return required <= 0 ? 0.0F : Mth.clamp(progressTicks() / (float) required, 0.0F, 1.0F);
    }

    public float waterProgress() {
        int capacity = waterCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp(waterAmount() / (float) capacity, 0.0F, 1.0F);
    }

    public int progressTicks() {
        return data.get(DATA_PROGRESS);
    }

    public int requiredTicks() {
        return data.get(DATA_REQUIRED_TICKS);
    }

    public int availableItems() {
        return data.get(DATA_AVAILABLE_ITEMS);
    }

    public int waterAmount() {
        return data.get(DATA_WATER);
    }

    public int waterCapacity() {
        return data.get(DATA_WATER_CAPACITY);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public boolean wateredBatch() {
        return data.get(DATA_WET_BATCH) != 0;
    }

    public int varietyCount() {
        return data.get(DATA_VARIETY_COUNT);
    }

    public int speedBonusPercent() {
        return data.get(DATA_SPEED_BONUS_PERCENT);
    }

    public boolean bulkBonusActive() {
        return availableItems() >= WoodenComposterBlockEntity.BULK_BONUS_UNIT_THRESHOLD;
    }

    public boolean varietyBonusActive() {
        return varietyCount() >= WoodenComposterBlockEntity.VARIETY_BONUS_ITEM_THRESHOLD;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return FluidPurgeSupport.handleMenuButton(player, composter, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WOODEN_COMPOSTER.get());
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
        } else if (WoodenComposterBlockEntity.isCompostable(stack)) {
            if (!moveItemStackTo(stack, 0, WoodenComposterBlockEntity.INPUT_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (WoodenComposterBlockEntity.isFluidInputContainer(stack)) {
            if (!moveItemStackTo(
                    stack,
                    WoodenComposterBlockEntity.SLOT_WATER_INPUT_CONTAINER,
                    WoodenComposterBlockEntity.SLOT_WATER_INPUT_CONTAINER + 1,
                    false
            )) {
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 35 + column * 18, 120 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 35 + column * 18, 178));
        }
    }

    private static WoodenComposterBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof WoodenComposterBlockEntity composter) {
            return composter;
        }
        throw new IllegalStateException("Expected wooden composter block entity at " + pos);
    }
}
