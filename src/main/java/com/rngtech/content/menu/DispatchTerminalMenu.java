package com.rngtech.content.menu;

import com.rngtech.content.blockentity.DispatchTerminalBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.SplittableRandom;

/**
 * A Dispatch Terminal's screen: six listed jobs as rows on the display. The listing slots only show and sync the Job
 * Chips; a row's Download button moves its chip into the player's inventory, and Refresh spends one Requisition. Creative
 * players need a Requisition but do not use it up, as with a Seal.
 */
public class DispatchTerminalMenu extends AbstractContainerMenu {
    public static final int BUTTON_REROLL = 0;
    /** Download buttons are {@code BUTTON_TAKE + slot}. */
    public static final int BUTTON_TAKE = 1;
    public static final int ROW_LEFT = 10;
    public static final int ROW_TOP = 22;
    public static final int ROW_WIDTH = 234;
    public static final int ROW_HEIGHT = 28;
    public static final int ROW_STEP = 30;
    public static final int ICON_X = 5;
    public static final int ICON_Y = 6;
    private static final int OFFERS = DispatchTerminalBlockEntity.OFFERS;

    private final DispatchTerminalBlockEntity board;
    private final ContainerLevelAccess access;

    public DispatchTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()));
    }

    public DispatchTerminalMenu(int containerId, Inventory playerInventory, DispatchTerminalBlockEntity board) {
        super(ModMenus.DISPATCH_TERMINAL.get(), containerId);
        this.board = board;
        access = ContainerLevelAccess.create(board.getLevel(), board.getBlockPos());
        for (int slot = 0; slot < OFFERS; slot++) {
            addSlot(new SlotItemHandler(board.offers(), slot, ROW_LEFT + ICON_X, rowTop(slot) + ICON_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    /** A listing row's top edge in the screen. */
    public static int rowTop(int slot) {
        return ROW_TOP + slot * ROW_STEP;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_TAKE && id < BUTTON_TAKE + OFFERS) {
            return player.level().isClientSide || take(player, id - BUTTON_TAKE);
        }
        if (id != BUTTON_REROLL) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        int requisition = requisitionSlot(player.getInventory());
        if (requisition < 0) {
            player.displayClientMessage(Component.translatable("rngtech.dispatch_terminal.message.no_requisition"), true);
            return false;
        }
        if (!board.reroll(new SplittableRandom(player.getRandom().nextLong()))) {
            player.displayClientMessage(Component.translatable("rngtech.dispatch_terminal.message.no_objectives"), true);
            return false;
        }
        if (!player.isCreative()) {
            player.getInventory().getItem(requisition).shrink(1);
            player.getInventory().setChanged();
        }
        broadcastChanges();
        return true;
    }

    /** Moves the order in {@code slot} into the player's inventory, or drops it at their feet when the inventory is full. */
    private boolean take(Player player, int slot) {
        ItemStack order = board.offers().extractItem(slot, 1, false);
        if (order.isEmpty()) {
            return false;
        }
        if (!player.getInventory().add(order)) {
            player.drop(order, false);
        }
        broadcastChanges();
        return true;
    }

    /** The first inventory slot holding a Requisition, or -1. */
    public static int requisitionSlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.REQUISITION.get())) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DISPATCH_TERMINAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static DispatchTerminalBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof DispatchTerminalBlockEntity board) {
            return board;
        }
        throw new IllegalStateException("Expected a Dispatch Terminal block entity at " + pos);
    }
}
