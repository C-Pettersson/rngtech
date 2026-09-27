package com.rngtech.content.blockentity;

import com.rngtech.content.menu.DebugChestMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DebugChestBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 27;

    private final ItemStackHandler templateInventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler infiniteItemHandler = new InfiniteItemHandler();

    public DebugChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEBUG_CHEST.get(), pos, blockState);
    }

    public ItemStackHandler getTemplateInventory() {
        return templateInventory;
    }

    public IItemHandler getItemHandler(Direction side) {
        return infiniteItemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.debug_chest");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DebugChestMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < templateInventory.getSlots(); slot++) {
            ItemStack stack = templateInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                templateInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Templates", templateInventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        templateInventory.deserializeNBT(registries, tag.getCompound("Templates"));
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < templateInventory.getSlots();
    }

    private final class InfiniteItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return templateInventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!isValidSlot(slot)) {
                return ItemStack.EMPTY;
            }
            ItemStack template = templateInventory.getStackInSlot(slot);
            return template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount(template.getMaxStackSize());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isValidSlot(slot) || stack.isEmpty()) {
                return stack;
            }

            ItemStack template = templateInventory.getStackInSlot(slot);
            if (!template.isEmpty() && !ItemStack.isSameItemSameComponents(template, stack)) {
                return stack;
            }
            if (!simulate && template.isEmpty()) {
                templateInventory.setStackInSlot(slot, stack.copyWithCount(1));
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isValidSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack template = templateInventory.getStackInSlot(slot);
            if (template.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return template.copyWithCount(Math.min(amount, template.getMaxStackSize()));
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!isValidSlot(slot) || stack.isEmpty()) {
                return false;
            }
            ItemStack template = templateInventory.getStackInSlot(slot);
            return template.isEmpty() || ItemStack.isSameItemSameComponents(template, stack);
        }
    }
}
