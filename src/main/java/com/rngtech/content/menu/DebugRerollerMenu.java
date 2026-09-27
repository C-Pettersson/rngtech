package com.rngtech.content.menu;

import com.rngtech.content.blockentity.DebugRerollerBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DebugRerollerMenu extends AbstractContainerMenu {
    public static final int BUTTON_REROLL = 0;

    private static final int MACHINE_SLOT_COUNT = DebugRerollerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final DebugRerollerBlockEntity reroller;

    public DebugRerollerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()));
    }

    public DebugRerollerMenu(int containerId, Inventory playerInventory, DebugRerollerBlockEntity reroller) {
        super(ModMenus.DEBUG_REROLLER.get(), containerId);
        this.reroller = reroller;
        access = ContainerLevelAccess.create(reroller.getLevel(), reroller.getBlockPos());

        ItemStackHandler inventory = reroller.getInventory();
        addSlot(new SlotItemHandler(inventory, DebugRerollerBlockEntity.SLOT_TARGET, 48, 42));
        addPlayerInventory(playerInventory);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_REROLL) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        if (!reroller.rerollTarget(player.level().random)) {
            player.displayClientMessage(Component.translatable(reroller.failureMessageKey()), true);
            return false;
        }

        player.displayClientMessage(Component.translatable("rngtech.debug_reroller.success"), true);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DEBUG_REROLLER.get());
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
        } else if (!moveItemStackTo(
                stack,
                DebugRerollerBlockEntity.SLOT_TARGET,
                DebugRerollerBlockEntity.SLOT_TARGET + 1,
                false
        )) {
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 39 + column * 18, 174));
        }
    }

    private static DebugRerollerBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof DebugRerollerBlockEntity reroller) {
            return reroller;
        }
        throw new IllegalStateException("Expected debug reroller block entity at " + pos);
    }
}
