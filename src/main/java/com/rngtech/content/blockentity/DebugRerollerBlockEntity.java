package com.rngtech.content.blockentity;

import com.rngtech.RNGTech;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.DebugRerollerMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DebugRerollerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_COUNT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_TARGET && canReroll(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public DebugRerollerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEBUG_REROLLER.get(), pos, blockState);
    }

    public static boolean canReroll(ItemStack stack) {
        return RNGTech.isDebugContentEnabled()
                && RefinementTargets.canRefine(stack)
                && RefinementTargets.traits(stack).rarity() != Rarity.UNIQUE;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean rerollTarget(RandomSource random) {
        ItemStack target = inventory.getStackInSlot(SLOT_TARGET);
        if (!canReroll(target)) {
            return false;
        }

        ItemStack rerolled = target.copy();
        RefinementTargets.setTraits(rerolled, rerolledTraits(target, random));
        inventory.setStackInSlot(SLOT_TARGET, rerolled);
        setChanged();
        return true;
    }

    public String failureMessageKey() {
        ItemStack target = inventory.getStackInSlot(SLOT_TARGET);
        if (RefinementTargets.canRefine(target) && RefinementTargets.traits(target).rarity() == Rarity.UNIQUE) {
            return "rngtech.debug_reroller.failure.unique";
        }
        return "rngtech.debug_reroller.failure.invalid_target";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.debug_reroller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DebugRerollerMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        ItemStack stack = inventory.getStackInSlot(SLOT_TARGET);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(SLOT_TARGET, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    private static MachineTraits rerolledTraits(ItemStack stack, RandomSource random) {
        MachineTraits rolled = MachineTraitRoller.roll(
                RefinementTargets.eligibilityProfile(stack),
                RefinementTargets.modifierRollComponentStage(stack),
                random
        );
        if (!(stack.getItem() instanceof MachinePartItem)) {
            return rolled;
        }
        MachinePartItem part = (MachinePartItem) stack.getItem();
        return new MachineTraits(
                rolled.rarity(),
                part.modifierSet().refinementPotential() + rolled.refinementPotential(),
                rolled.modifiers(),
                rolled.behaviors()
        );
    }
}
