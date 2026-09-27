package com.rngtech.content.menu;

import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementSelection;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AffixForgeMenu extends AbstractContainerMenu {
    private static final int FORGE_SLOT_COUNT = AffixForgeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FORGE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int BUTTON_REFORGE_NONE = 100;
    private static final int PLAYER_INVENTORY_X = 132;
    private static final int PLAYER_INVENTORY_Y = 143;
    private static final int HOTBAR_Y = 201;
    private static final int UPGRADE_SLOT_Y = 143;

    private final ContainerLevelAccess access;
    private final AffixForgeBlockEntity affixForge;

    public AffixForgeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()));
    }

    public AffixForgeMenu(int containerId, Inventory playerInventory, AffixForgeBlockEntity affixForge) {
        super(ModMenus.AFFIX_FORGE.get(), containerId);
        this.affixForge = affixForge;
        access = ContainerLevelAccess.create(affixForge.getLevel(), affixForge.getBlockPos());

        ItemStackHandler inventory = affixForge.getInventory();
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_TARGET, 14, 32));
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_CONSUMABLE, 43, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return affixForge.isRefinementConsumable(stack);
            }
        });
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_MODIFIER, 72, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return affixForge.isFocusItem(stack);
            }
        });
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_LENS_ARRAY, 14, UPGRADE_SLOT_Y));
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_MODIFIER_SOCKET, 43, UPGRADE_SLOT_Y));
        addSlot(new SlotItemHandler(inventory, AffixForgeBlockEntity.SLOT_RESONANCE_MATRIX, 72, UPGRADE_SLOT_Y));
        addPlayerInventory(playerInventory);
    }

    public boolean hasLensArray() {
        return affixForge.hasLensArray();
    }

    public boolean hasModifierSocket() {
        return affixForge.hasModifierSocket();
    }

    public boolean hasResonanceMatrix() {
        return affixForge.hasResonanceMatrix();
    }

    public String lockedMessage(RefinementOperation operation) {
        return affixForge.lockedMessage(operation);
    }

    public int reforgeButtonId() {
        return BUTTON_REFORGE_NONE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!isReforgeButton(id)) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return affixForge.applyRefinement(player, RefinementSelection.none());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AFFIX_FORGE.get());
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

        if (index < FORGE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AffixForgeBlockEntity.upgradeSlot(stack) >= 0) {
            int upgradeSlot = AffixForgeBlockEntity.upgradeSlot(stack);
            if (!moveItemStackTo(stack, upgradeSlot, upgradeSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (affixForge.isRefinementConsumable(stack)) {
            if (!moveItemStackTo(stack, AffixForgeBlockEntity.SLOT_CONSUMABLE, AffixForgeBlockEntity.SLOT_CONSUMABLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (affixForge.isFocusItem(stack)) {
            if (!moveItemStackTo(stack, AffixForgeBlockEntity.SLOT_MODIFIER, AffixForgeBlockEntity.SLOT_MODIFIER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, AffixForgeBlockEntity.SLOT_TARGET, AffixForgeBlockEntity.SLOT_TARGET + 1, false)) {
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
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    private static boolean isReforgeButton(int id) {
        return id == BUTTON_REFORGE_NONE;
    }

    private static AffixForgeBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AffixForgeBlockEntity affixForge) {
            return affixForge;
        }
        throw new IllegalStateException("Expected affix forge block entity at " + pos);
    }
}
