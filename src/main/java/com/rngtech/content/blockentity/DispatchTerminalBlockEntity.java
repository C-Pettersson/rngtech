package com.rngtech.content.blockentity;

import com.rngtech.content.block.DispatchTerminalBlock;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.DispatchTerminalMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkObjectiveCatalog;
import com.rngtech.content.workorder.WorkOrderJobs;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Holds a Dispatch Terminal's six offered Work Orders. A new board fills its offer the first time it is opened; after that,
 * a taken order leaves its slot empty until a Reroll replaces the whole offer. The offer, empty slots included, stays on
 * the dropped board so breaking and placing it again never refills it.
 */
public class DispatchTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OFFERS = 6;

    private final ItemStackHandler offers = new ItemStackHandler(OFFERS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateNotes();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private boolean filled;

    public DispatchTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DISPATCH_TERMINAL.get(), pos, blockState);
    }

    public ItemStackHandler offers() {
        return offers;
    }

    public boolean filled() {
        return filled;
    }

    /** Fills a board that has never had an offer. Returns false when nothing could be rolled. */
    public boolean ensureFilled() {
        return filled || reroll(new SplittableRandom(level == null ? 0L : level.getRandom().nextLong()));
    }

    /** Replaces every offer, taken or not, with a new roll. Returns false when no objective is loaded. */
    public boolean reroll(RandomGenerator random) {
        ItemStack[] rolled = new ItemStack[OFFERS];
        for (int slot = 0; slot < OFFERS; slot++) {
            WorkOrder order = level == null ? WorkOrderRules.roll(WorkObjectiveCatalog.all(), random)
                    : WorkOrderJobs.roll(level, worldPosition, 0, random);
            if (order == null) {
                return false;
            }
            rolled[slot] = JobChipItem.of(ModItems.JOB_CHIP.get(), order, MachineTraits.EMPTY);
        }
        for (int slot = 0; slot < OFFERS; slot++) {
            offers.setStackInSlot(slot, rolled[slot]);
        }
        filled = true;
        setChanged();
        return true;
    }

    /** Keeps the blockstate's listing count, which the model draws as lit status lamps, in step with the jobs left. */
    private void updateNotes() {
        if (level == null || level.isClientSide) {
            return;
        }
        int left = 0;
        for (int slot = 0; slot < OFFERS; slot++) {
            left += offers.getStackInSlot(slot).isEmpty() ? 0 : 1;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(DispatchTerminalBlock.LISTINGS) && state.getValue(DispatchTerminalBlock.LISTINGS) != left) {
            level.setBlock(worldPosition, state.setValue(DispatchTerminalBlock.LISTINGS, left), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.dispatch_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ensureFilled();
        return new DispatchTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Offers", offers.serializeNBT(registries));
        tag.putBoolean("Filled", filled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        offers.deserializeNBT(registries, tag.getCompound("Offers"));
        filled = tag.getBoolean("Filled");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        ItemContainerContents contents = componentInput.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> stacks = NonNullList.withSize(OFFERS, ItemStack.EMPTY);
            contents.copyInto(stacks);
            for (int slot = 0; slot < OFFERS; slot++) {
                offers.setStackInSlot(slot, stacks.get(slot));
            }
            filled = true;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (filled) {
            NonNullList<ItemStack> stacks = NonNullList.withSize(OFFERS, ItemStack.EMPTY);
            for (int slot = 0; slot < OFFERS; slot++) {
                stacks.set(slot, offers.getStackInSlot(slot).copy());
            }
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Offers");
        tag.remove("Filled");
    }
}
