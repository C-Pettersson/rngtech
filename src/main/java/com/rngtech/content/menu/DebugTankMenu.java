package com.rngtech.content.menu;

import com.rngtech.content.blockentity.DebugTankBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

public class DebugTankMenu extends AbstractContainerMenu {
    public static final int BUTTON_CLEAR = 0;

    private static final int MACHINE_SLOT_COUNT = DebugTankBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int DATA_HAS_FLUID = 0;
    private static final int DATA_COUNT = 1;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final DebugTankBlockEntity tank;

    public DebugTankMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    public DebugTankMenu(
            int containerId,
            Inventory playerInventory,
            DebugTankBlockEntity tank,
            ContainerData data
    ) {
        super(ModMenus.DEBUG_TANK.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(tank.getLevel(), tank.getBlockPos());
        this.data = data;
        this.tank = tank;
        addDataSlots(data);

        ItemStackHandler inventory = tank.getContainerInventory();
        addSlot(new SlotItemHandler(inventory, DebugTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER, 62, 35));
        addSlot(new SlotItemHandler(inventory, DebugTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER, 98, 35));

        addPlayerInventory(playerInventory);
    }

    public boolean hasFluid() {
        return data.get(DATA_HAS_FLUID) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_CLEAR) {
            return false;
        }
        if (!player.level().isClientSide) {
            tank.clearTemplateFluid();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DEBUG_TANK.get());
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DebugTankBlockEntity.isFluidInputContainer(stack)) {
            if (!moveItemStackTo(stack, DebugTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER, DebugTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DebugTankBlockEntity.isFluidOutputContainer(stack)) {
            if (!moveItemStackTo(stack, DebugTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER, DebugTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER + 1, false)) {
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private static DebugTankBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof DebugTankBlockEntity tank) {
            return tank;
        }
        throw new IllegalStateException("Expected debug tank block entity at " + pos);
    }
}
